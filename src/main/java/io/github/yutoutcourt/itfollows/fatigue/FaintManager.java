package io.github.yutoutcourt.itfollows.fatigue;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Gère l'évanouissement déclenché quand la fatigue atteint 0.
 *
 * <p>Le joueur perd connaissance : écran totalement noir (overlay client via paquet
 * {@code faint_sync}), immobilisation (cécité + lenteur extrême côté serveur, et
 * verrouillage de position chaque tick), pendant {@code faintDurationTicks}. Il se
 * réveille ensuite à {@code faintWakeFatigue} (10 % par défaut).
 *
 * <p>Le verrouillage par téléportation gère le cas où le joueur ne possède plus
 * d'effet (déco/reco), et le réveil force la fatigue au-dessus de 0 même en cas de
 * déconnexion ou de mort pour éviter une boucle de ré-évanouissement.
 */
public final class FaintManager {

    /** État d'un évanouissement en cours : tick de début + position à maintenir. */
    private static final class Faint {
        final int startTick;
        final Vec3 pos;

        Faint(int startTick, Vec3 pos) {
            this.startTick = startTick;
            this.pos = pos;
        }
    }

    /** Amplificateur de lenteur garantissant l'immobilité totale. */
    private static final int IMMOBILIZE_AMPLIFIER = 250;

    private static final Map<UUID, Faint> fainted = new HashMap<>();

    private FaintManager() {
    }

    /** Vrai si le joueur est actuellement évanoui. */
    public static boolean isFainted(UUID playerId) {
        return fainted.containsKey(playerId);
    }

    /** Déclenche l'évanouissement du joueur (sans effet s'il l'est déjà). */
    public static void faint(ServerPlayer player) {
        UUID id = player.getUUID();
        if (fainted.containsKey(id)) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        fainted.put(id, new Faint(player.server.getTickCount(), player.position()));

        // Marge pour couvrir tout l'évanouissement même entre deux échantillonnages.
        int duration = config.faintDurationTicks + 20;
        applyImmobilizingEffect(player, MobEffects.BLINDNESS, duration);
        applyImmobilizingEffect(player, MobEffects.MOVEMENT_SLOWDOWN, duration);
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);

        player.displayClientMessage(
                Component.literal("Vous vous êtes évanoui d'épuisement..."), false);
        ItFollowsNetworking.sendFaint(player, true);
    }

    /** Maintient les évanouissements en cours et réveille les joueurs arrivés à terme. À appeler chaque tick serveur. */
    public static void tick(MinecraftServer server) {
        if (fainted.isEmpty()) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        Iterator<Map.Entry<UUID, Faint>> it = fainted.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Faint> entry = it.next();
            UUID id = entry.getKey();
            Faint faint = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(id);

            if (player == null) {
                // Déconnecté en plein évanouissement : on remonte la fatigue dans l'état persistant
                // pour éviter un ré-évanouissement immédiat à la reconnexion.
                FatigueState.get(server).setFatigue(id, config.faintWakeFatigue);
                it.remove();
                continue;
            }

            if (player.isDeadOrDying()) {
                clearFaintEffects(player);
                ItFollowsNetworking.sendFaint(player, false);
                FatigueState.get(server).setFatigue(id, config.faintWakeFatigue);
                it.remove();
                continue;
            }

            if (server.getTickCount() - faint.startTick >= config.faintDurationTicks) {
                wake(player, config);
                it.remove();
                continue;
            }

            // Maintien immobile : on le renvoie à sa position de départ chaque tick.
            player.setDeltaMovement(Vec3.ZERO);
            player.connection.teleport(faint.pos.x, faint.pos.y, faint.pos.z,
                    player.getYRot(), player.getXRot());
        }
    }

    private static void wake(ServerPlayer player, ItFollowsConfig config) {
        clearFaintEffects(player);
        ItFollowsNetworking.sendFaint(player, false);
        // setFatigue resynchronise la valeur et réapplique les effets de palier adaptés.
        FatigueManager.setFatigue(player, config.faintWakeFatigue);
        player.displayClientMessage(
                Component.literal("Vous reprenez connaissance, encore groggy."), true);
    }

    private static void applyImmobilizingEffect(ServerPlayer player, MobEffect effect, int duration) {
        player.addEffect(new MobEffectInstance(
                effect, duration, IMMOBILIZE_AMPLIFIER, true, false, false));
    }

    private static void clearFaintEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }
}
