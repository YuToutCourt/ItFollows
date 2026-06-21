package io.github.yutoutcourt.itfollows.presence;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import io.github.yutoutcourt.itfollows.tracking.HauntController;
import io.github.yutoutcourt.itfollows.tracking.StalkTrackerState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Effets de présence (Phase 4a — couche audio + sync), cf. cahier §5/§6. Deux pistes indépendantes :
 *
 * <ol>
 *   <li><b>Piste fatigue</b> (TOUS les joueurs, maudits ou non) : paliers 20 / 10 / 5 %. Le serveur joue
 *       battement de cœur, sons zombie/creeper rares, pas derrière soi, murmures. Les <i>visuels</i> de
 *       cette piste sont gérés côté client à partir de la fatigue déjà synchronisée.</li>
 *   <li><b>Piste entité</b> (la cible traquée seulement) : pilotée par la <b>distance</b> à l'entité,
 *       par bandes (30 / 15 / 10 / 5 / &lt;3 blocs). Le serveur envoie la distance au client (overlays) et
 *       joue l'ambiance évolutive : cri lointain → parasite radio → murmures stéréo → heartbeat synchronisé
 *       → coupure. Les sons sont <b>positionnés</b> autour du joueur pour donner une direction (panning L/R).</li>
 * </ol>
 *
 * <p>Quand la cible est dans la portée de l'entité, la piste entité <b>prime</b> sur la piste fatigue
 * pour l'audio (on ne double pas les sons).
 */
public final class PresenceManager {

    /** Pas (sons de marche) joués « derrière » le joueur — effet « quelqu'un me suit » (piste fatigue ≤ 10 %). */
    private static final SoundEvent[] STEP_SOUNDS = {
            SoundEvents.GRAVEL_STEP,
            SoundEvents.WOOD_STEP,
            SoundEvents.STONE_STEP,
            SoundEvents.SAND_STEP,
    };

    /** Sons de mobs d'ambiance joués rarement quand le joueur est fatigué (≤ palier léger). */
    private static final SoundEvent[] MOB_SOUNDS = {
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.SKELETON_AMBIENT,
    };

    /** Valeur de distance signifiant « pas d'entité applicable » (piste 2 inactive). */
    public static final float NO_ENTITY = -1.0f;

    private static int tickCounter;
    /** Dernière distance envoyée par joueur (pour n'émettre que sur changement notable). */
    private static final Map<UUID, Float> lastSentDistance = new HashMap<>();
    /** Heure-monde du prochain son de présence autorisé, par joueur. */
    private static final Map<UUID, Long> nextSoundTick = new HashMap<>();

    private PresenceManager() {
    }

    public static void tick(MinecraftServer server) {
        ItFollowsConfig config = ItFollowsConfig.get();
        if (!config.presenceEnabled) {
            return;
        }
        if (++tickCounter < config.presenceSampleIntervalTicks) {
            return;
        }
        tickCounter = 0;

        StalkTrackerState state = StalkTrackerState.get(server);
        ServerPlayer target = HauntController.currentTarget(server);
        StalkerEntity entity = HauntController.resolveEntity(server, state);
        boolean materialized = entity != null && !entity.isRemoved() && !entity.isDecoy();
        UUID targetId = target == null ? null : target.getUUID();

        long now = server.overworld().getGameTime();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();

            // Distance entité → cible (piste 2), ou NO_ENTITY si non applicable / hors portée.
            float distance = NO_ENTITY;
            if (materialized && id.equals(targetId) && player.level() == entity.level()) {
                float d = (float) Math.sqrt(player.distanceToSqr(entity));
                if (d <= config.presenceEntityMaxDistance) {
                    distance = d;
                }
            }
            syncDistance(player, distance);

            // Audio (les deux pistes), planifié par un timer par joueur.
            if (now >= nextSoundTick.getOrDefault(id, 0L)) {
                long next = playPresenceAudio(player, distance, FatigueManager.getFatigue(player),
                        player.getRandom(), config);
                nextSoundTick.put(id, now + Math.max(1L, next));
            }
        }
    }

    /** Envoie la distance au client seulement sur changement notable (ou bascule vers/depuis NO_ENTITY). */
    private static void syncDistance(ServerPlayer player, float distance) {
        UUID id = player.getUUID();
        Float prev = lastSentDistance.get(id);
        boolean changed = prev == null
                || (prev < 0) != (distance < 0)
                || Math.abs(prev - distance) >= 0.5f;
        if (changed) {
            ItFollowsNetworking.sendPresence(player, distance);
            lastSentDistance.put(id, distance);
        }
    }

    // --- Audio : choisit et joue le(s) son(s), renvoie le délai (ticks) avant le prochain ---------

    private static long playPresenceAudio(ServerPlayer player, float distance, float fatigue,
                                          RandomSource random, ItFollowsConfig config) {
        // Piste entité (prioritaire si dans la portée).
        if (distance >= 0.0f) {
            if (distance <= config.presenceBandClose) {
                // Heartbeat battant SANS ARRÊT dès ≤ 5 b, y compris au contact (≤ 3 b) : plus proche =
                // plus fort et plus rapide. Les murmures + parasite radio « constants » sont des BOUCLES
                // côté client ({@code ClientPresenceAmbience}) pilotées par la distance reçue.
                float closeness = 1.0f - distance / config.presenceBandClose; // 0 → 1
                heartbeat(player, Mth.lerp(closeness, 0.5f, config.heartbeatMaxVolume));
                return (long) Mth.lerp(closeness, 16.0f, 6.0f);
            }
            // 5 < distance ≤ 30 : cri de hunting rare (l'ambiance continue = boucles client whisper/radio).
            Vec3 pos = around(player, random, 6.0, 12.0);
            ItFollowsNetworking.playSoundTo(player, ModSounds.HUNTING_CRY,
                    pos.x, pos.y, pos.z, config.presenceHuntingCryVolume, 1.0f);
            return 200L + random.nextInt(2000);
        }

        // Piste fatigue (tous les joueurs).
        if (fatigue <= config.presenceFatigueLight) {
            // Battement de cœur : faible à 20 %, plus fort/rapide vers 0 %.
            float t = Mth.clamp(1.0f - fatigue / Math.max(1.0f, config.presenceFatigueLight), 0.0f, 1.0f);
            heartbeat(player, Mth.lerp(t, 0.3f, config.heartbeatMaxVolume));

            // Sons zombie/creeper aléatoires très rares.
            if (random.nextFloat() < config.presenceMobSoundChance) {
                Vec3 pos = around(player, random, 4.0, 10.0);
                SoundEvent mob = MOB_SOUNDS[random.nextInt(MOB_SOUNDS.length)];
                ItFollowsNetworking.playSoundTo(player, mob,
                        pos.x, pos.y, pos.z, config.presenceMobVolume, 0.8f + random.nextFloat() * 0.3f);
            }
            // Palier 10 % : pas « derrière soi ».
            if (fatigue <= config.presenceFatigueStrong && random.nextFloat() < 0.5f) {
                Vec3 behind = HauntController.behindTarget(player, 2.0 + random.nextDouble() * 2.0);
                SoundEvent step = STEP_SOUNDS[random.nextInt(STEP_SOUNDS.length)];
                ItFollowsNetworking.playSoundTo(player, step,
                        behind.x, player.getY(), behind.z, config.presenceFootstepVolume, 0.9f + random.nextFloat() * 0.2f);
            }
            // Palier 5 % : murmures.
            if (fatigue <= config.presenceFatigueWhisper && random.nextFloat() < 0.35f) {
                playPanned(player, ModSounds.WHISPER, random.nextBoolean(), 3.0,
                        config.presenceWhisperVolume, 1.0f, random);
            }
            // Cadence du cœur : ~1,5 s à 20 %, ~0,6 s vers 0 %.
            return (long) Mth.lerp(t, 30.0f, 12.0f);
        }

        // Joueur reposé et hors portée : rien, on repolle bientôt.
        return 40L;
    }

    /** Battement de cœur non directionnel, à la position du joueur. */
    private static void heartbeat(ServerPlayer player, float volume) {
        if (volume <= 0.04f) {
            return;
        }
        ItFollowsNetworking.playSoundTo(player, SoundEvents.WARDEN_HEARTBEAT,
                player.getX(), player.getEyeY(), player.getZ(), volume, 1.0f);
    }

    /** Joue un son décalé à gauche/droite du joueur (panning) : direction = vecteur droite du regard. */
    private static void playPanned(ServerPlayer player, SoundEvent sound, boolean right, double offset,
                                   float volume, float pitch, RandomSource random) {
        double yaw = Math.toRadians(player.getYRot());
        // Direction du regard (horizontale) : (-sin yaw, cos yaw) ; vecteur droite = (cos yaw, sin yaw).
        double rx = Math.cos(yaw);
        double rz = Math.sin(yaw);
        double sign = right ? 1.0 : -1.0;
        double x = player.getX() + rx * offset * sign;
        double z = player.getZ() + rz * offset * sign;
        double y = player.getEyeY();
        ItFollowsNetworking.playSoundTo(player, sound, x, y, z, volume, pitch);
    }

    /** Position autour du joueur : angle horizontal aléatoire, distance dans [min, max], léger décalage vertical. */
    private static Vec3 around(ServerPlayer player, RandomSource random, double min, double max) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = min + random.nextDouble() * (max - min);
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        double y = player.getEyeY() + (random.nextDouble() - 0.5);
        return new Vec3(x, y, z);
    }
}
