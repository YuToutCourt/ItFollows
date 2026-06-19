package io.github.yutoutcourt.itfollows.fatigue;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sieste.SiesteManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrateur du système de fatigue côté serveur.
 *
 * <p>Approche événementielle + échantillonnage (cf. cahier) : les coûts d'actions sont
 * appliqués à la volée (sprint/marche échantillonnés au tick, saut/attaque/minage sur event),
 * tandis que décroissance, régén, effets serveur et sync client ne s'exécutent qu'une fois
 * tous les {@code sampleIntervalTicks}.
 */
public final class FatigueManager {

    /** Seuil de déplacement horizontal (en blocs/tick) au-delà duquel on considère le joueur en marche. */
    private static final double MOVE_EPSILON = 0.01;

    private static int tickCounter = 0;
    /** UUID des joueurs ayant effectué une action coûteuse durant l'intervalle courant (pas de régén pour eux). */
    private static final Set<UUID> costlyThisInterval = new HashSet<>();
    /** Dernière position connue par joueur, pour détecter la marche. */
    private static final Map<UUID, Vec3> lastPos = new HashMap<>();

    private FatigueManager() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            // Sync initiale + nettoyage de l'état transitoire de mouvement.
            lastPos.remove(player.getUUID());
            syncAndApply(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            lastPos.remove(id);
            costlyThisInterval.remove(id);
            FatigueEffects.clear(id);
        });

        // À la mort, on repart frais : fatigue remise au maximum sur le nouveau pantin.
        // alive == false ⇒ respawn consécutif à une mort (par opposition au retour de l'End).
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                lastPos.remove(newPlayer.getUUID());
                costlyThisInterval.remove(newPlayer.getUUID());
                setFatigue(newPlayer, FatigueRules.MAX);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && !isSuspended(serverPlayer)) {
                applyCostInternal(serverPlayer, ItFollowsConfig.get().mineCost);
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer && !isSuspended(serverPlayer)) {
                applyCostInternal(serverPlayer, ItFollowsConfig.get().attackCost);
            }
            return InteractionResult.PASS;
        });
    }

    /** Appelé par le mixin de saut. */
    public static void onJump(ServerPlayer player) {
        if (!isSuspended(player)) {
            applyCostInternal(player, ItFollowsConfig.get().jumpCost);
        }
    }

    // --- API publique (commande debug + futurs systèmes) ---

    public static float getFatigue(ServerPlayer player) {
        return FatigueState.get(player.server).getFatigue(player.getUUID());
    }

    public static void setFatigue(ServerPlayer player, float value) {
        FatigueState.get(player.server).setFatigue(player.getUUID(), value);
        syncAndApply(player);
    }

    public static void addFatigue(ServerPlayer player, float delta) {
        setFatigue(player, getFatigue(player) + delta);
    }

    // --- Boucle serveur ---

    public static void onServerTick(MinecraftServer server) {
        ItFollowsConfig config = ItFollowsConfig.get();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isSuspended(player)) {
                continue;
            }
            sampleMovement(player, config);
        }

        if (++tickCounter >= config.sampleIntervalTicks) {
            tickCounter = 0;
            runSample(server, config);
        }
    }

    private static void sampleMovement(ServerPlayer player, ItFollowsConfig config) {
        UUID id = player.getUUID();
        Vec3 current = player.position();
        Vec3 previous = lastPos.put(id, current);
        double horizontal = previous == null
                ? 0.0
                : Math.hypot(current.x - previous.x, current.z - previous.z);

        // Classification du mode de déplacement, par priorité décroissante.
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Boat boat) {
            // En bateau : uniquement si le bateau avance réellement et que ce joueur le contrôle (celui qui rame).
            if (horizontal > MOVE_EPSILON && boat.getControllingPassenger() == player) {
                applyCostInternal(player, config.boatCostPerTick);
            }
        } else if (player.isSwimming() || (player.isInWater() && horizontal > MOVE_EPSILON)) {
            // Nage (pose de nage active, ou simplement se déplacer dans l'eau).
            applyCostInternal(player, config.swimCostPerTick);
        } else if (player.isSprinting()) {
            applyCostInternal(player, config.sprintCostPerTick);
        } else if (horizontal > MOVE_EPSILON) {
            applyCostInternal(player, config.walkCostPerTick);
        }
    }

    private static void runSample(MinecraftServer server, ItFollowsConfig config) {
        FatigueState state = FatigueState.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isSuspended(player)) {
                continue;
            }
            UUID id = player.getUUID();
            float value = state.getFatigue(id);

            if (SiesteManager.isNapping(id)) {
                // En sieste : récup rapide une fois le délai passé, sinon rien (ni régén passive).
                if (SiesteManager.isRecovering(id, server, config)) {
                    value += config.siesteRegenPerSample;
                }
            } else if (!costlyThisInterval.contains(id)) {
                value += config.passiveRegenPerSample;
            }

            state.setFatigue(id, value);
            applyEffectsAndSync(player, state.getFatigue(id), config);
        }
        costlyThisInterval.clear();
    }

    // --- Helpers internes ---

    private static void applyCostInternal(ServerPlayer player, float cost) {
        FatigueState state = FatigueState.get(player.server);
        float newValue = FatigueRules.applyCost(state.getFatigue(player.getUUID()), cost);
        state.setFatigue(player.getUUID(), newValue);
        costlyThisInterval.add(player.getUUID());
        // Épuisement total → évanouissement.
        if (newValue <= FatigueRules.MIN) {
            FaintManager.faint(player);
        }
    }

    /** Recalcule palier, applique effets serveur et sync au client. */
    private static void syncAndApply(ServerPlayer player) {
        applyEffectsAndSync(player, getFatigue(player), ItFollowsConfig.get());
    }

    private static void applyEffectsAndSync(ServerPlayer player, float value, ItFollowsConfig config) {
        FatigueEffects.apply(player, value, config);
        ItFollowsNetworking.sendFatigue(player, value);
    }

    /** Les modes créatif et spectateur ne subissent pas la fatigue. */
    private static boolean isExempt(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    /** Joueur à ignorer par la boucle de fatigue : exempté, ou évanoui (géré par {@link FaintManager}). */
    private static boolean isSuspended(ServerPlayer player) {
        return isExempt(player) || FaintManager.isFainted(player.getUUID());
    }
}
