package io.github.yutoutcourt.itfollows.sieste;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import io.github.yutoutcourt.itfollows.fatigue.FatigueRules;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion de la sieste (Phase 1.5).
 *
 * <p>Le joueur fait un clic droit sur un lit, à n'importe quel moment (jour comme nuit).
 * On le couche via {@code startSleeping} en contournant les vérifications vanilla
 * (nuit obligatoire / monstres à proximité) et on <strong>annule</strong> le sommeil vanilla
 * pour ne jamais avancer l'heure du monde — ça, c'est la Phase 5.
 *
 * <p>Après {@code siesteDelayTicks}, la fatigue remonte de {@code siesteRegenPerSample} par
 * échantillonnage (appliqué dans {@link FatigueManager}, pour centraliser les changements de
 * fatigue). Le joueur se réveille en quittant le lit (bouton « Quitter le lit »), en prenant
 * des dégâts (réveil vanilla), ou automatiquement une fois complètement reposé.
 *
 * <p>L'absence de saut de nuit est garantie par le mixin
 * {@code mixin/PlayerSleepTimerMixin} qui empêche un dormeur en sieste de compter comme
 * « dort depuis assez longtemps » (condition du saut de temps vanilla).
 */
public final class SiesteManager {

    /** Tick serveur auquel chaque joueur a commencé sa sieste. */
    private static final Map<UUID, Integer> napStartTick = new HashMap<>();

    private SiesteManager() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> onUseBlock(player, world, hand, hitResult));
        
        net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.ALLOW_SLEEP_TIME.register((player, sleepingPos, vanillaResult) -> {
            if (player instanceof ServerPlayer serverPlayer && isNapping(serverPlayer.getUUID())) {
                return InteractionResult.SUCCESS; // Autorise la sieste le jour (bypass le check vanilla)
            }
            return InteractionResult.PASS;
        });
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock)) {
            return InteractionResult.PASS;
        }
        
        if (world.isClientSide()) {
            // Le client renvoie SUCCESS pour ne pas déclencher la logique vanilla (qui empêcherait de dormir le jour)
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (isNapping(serverPlayer.getUUID())) {
            return InteractionResult.PASS;
        }

        if (FatigueManager.getFatigue(serverPlayer) >= FatigueRules.MAX) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("Vous n'êtes pas fatigué."), true);
            return InteractionResult.SUCCESS;
        }

        // Couche le joueur via la version basse Player.startSleeping (cf. ServerPlayerNapMixin) :
        // contourne les checks vanilla (jour/monstres/portée). L'override ServerPlayer.startSleeping
        // refuserait en journée ou près d'un monstre, laissant le joueur debout → aucune récup.
        // Pas de saut de nuit : garanti par PlayerSleepTimerMixin.
        ((NapStarter) serverPlayer).itfollows$forceNap(hitResult.getBlockPos());
        napStartTick.put(serverPlayer.getUUID(), serverPlayer.server.getTickCount());
        return InteractionResult.SUCCESS;
    }

    /** Valide l'état des siestes en cours (réveil, déconnexion, repos complet). À appeler chaque tick serveur. */
    public static void tick(MinecraftServer server) {
        if (napStartTick.isEmpty()) {
            return;
        }
        napStartTick.keySet().removeIf(id -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                return true; // déconnecté
            }
            if (!player.isSleeping()) {
                return true; // a quitté le lit (bouton ou réveil vanilla sur dégâts)
            }
            if (FatigueManager.getFatigue(player) >= FatigueRules.MAX) {
                player.stopSleeping(); // complètement reposé → réveil automatique
                return true;
            }
            return false;
        });
    }

    /** Vrai si le joueur est en sieste (couché, en attente ou en récup). */
    public static boolean isNapping(UUID playerId) {
        return napStartTick.containsKey(playerId);
    }

    /** Vrai si la sieste a dépassé le délai initial et récupère donc de la fatigue. */
    public static boolean isRecovering(UUID playerId, MinecraftServer server, ItFollowsConfig config) {
        Integer start = napStartTick.get(playerId);
        return start != null && (server.getTickCount() - start) >= config.siesteDelayTicks;
    }
}
