package io.github.yutoutcourt.itfollows.tracking;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Escalade de la traque (Phase 2b/2c, cf. cahier §3.2). Pilote, une fois la cible choisie, les trois
 * étapes d'avertissement progressif puis la révélation « LOOK BEHIND YOU », avant de céder la main à
 * {@link HauntController} pour la traque active :
 *
 * <ol>
 *   <li><b>WARNING_DISTANT</b> : bruits lointains ciblés (le seul traqué les entend), spatialisés.</li>
 *   <li><b>WARNING_PHYSICAL</b> : portes qui s'ouvrent / blocs cassés <i>réels</i> à proximité (perçus par tous).</li>
 *   <li><b>WARNING_SILHOUETTE</b> : flashs d'un leurre immobile dans le champ de vision de la cible (~1 s).</li>
 *   <li><b>REVEAL</b> : leurre immobile à {@code revealDistance} blocs <i>derrière</i> la cible + voix
 *       « LOOK BEHIND YOU » ; dès que la cible <i>vise</i> le leurre (raycast de regard), il disparaît et la
 *       vraie entité traqueuse apparaît à sa place → {@link HauntPhase#HUNTING}.</li>
 * </ol>
 *
 * <p>Le minutage des étapes s'appuie sur l'heure-monde ({@code getGameTime}, persistée dans
 * {@link StalkTrackerState#getPhaseStartTime()}), robuste aux déco/reco. Les minuteurs intra-étape
 * (prochain bruit, flash en cours, maintien du regard) sont en mémoire de session — réinitialisés à
 * chaque changement de phase via {@link #resetTimers()}.
 */
public final class HauntPhaseController {

    /** Heure-monde du prochain événement d'étape (bruit lointain / casse / flash de silhouette). */
    private static long nextEventTime;
    /** Heure-monde à laquelle retirer le flash de silhouette en cours, ou {@code 0} si aucun. */
    private static long silhouetteRemoveAt;
    /** Le leurre de révélation a-t-il déjà été matérialisé pour la phase REVEAL courante ? */
    private static boolean revealSpawned;
    /** Ticks consécutifs où la cible vise le leurre de révélation (anti faux positif). */
    private static int lookHoldTicks;

    private HauntPhaseController() {
    }

    /** Remet à zéro les minuteurs de session (appelé à chaque changement de phase). */
    static void resetTimers() {
        nextEventTime = 0L;
        silhouetteRemoveAt = 0L;
        revealSpawned = false;
        lookHoldTicks = 0;
    }

    static void tick(MinecraftServer server, StalkTrackerState state, ServerPlayer target, ItFollowsConfig config) {
        long now = server.overworld().getGameTime();
        long elapsed = now - state.getPhaseStartTime();
        RandomSource random = target.getRandom();

        switch (state.getHauntPhase()) {
            case WARNING_DISTANT -> {
                distantStage(target, config, now, random);
                if (elapsed >= config.hauntDistantStageTicks) {
                    advance(server, state, HauntPhase.WARNING_PHYSICAL);
                }
            }
            case WARNING_PHYSICAL -> {
                physicalStage(target, config, now, random);
                if (elapsed >= config.hauntPhysicalStageTicks) {
                    advance(server, state, HauntPhase.WARNING_SILHOUETTE);
                }
            }
            case WARNING_SILHOUETTE -> {
                silhouetteStage(server, state, target, config, now, random);
                if (elapsed >= config.hauntSilhouetteStageTicks) {
                    clearSilhouette(server, state);
                    advance(server, state, HauntPhase.REVEAL);
                }
            }
            case REVEAL -> revealStage(server, state, target, config, now);
            default -> {
            }
        }
    }

    /** Bascule de phase d'escalade et horodate (heure-monde), en réinitialisant les minuteurs de session. */
    private static void advance(MinecraftServer server, StalkTrackerState state, HauntPhase phase) {
        state.setHauntPhase(phase);
        state.setPhaseStartTime(server.overworld().getGameTime());
        resetTimers();
    }

    // --- Étape 1 : bruits lointains ciblés ------------------------------------------------------

    /**
     * Pool de sons vanilla effrayants joués aléatoirement pendant l'étape lointaine.
     * Mélange de sons de caverne, pas, portes, mobs hostiles et ambiances sinistres.
     */
    private static final SoundEvent[] SCARY_SOUNDS = {
            // Sons d'ambiance de caverne (les plus emblématiques pour faire peur)
            SoundEvents.AMBIENT_CAVE.value(),
            // Pas sur différentes surfaces (quelqu'un marche dans le noir…)
            SoundEvents.GRAVEL_STEP,
            SoundEvents.WOOD_STEP,
            SoundEvents.STONE_STEP,
            SoundEvents.SAND_STEP,
            // Portes et trappes (quelqu'un manipule des portes…)
            SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.WOODEN_DOOR_CLOSE,
            SoundEvents.WOODEN_TRAPDOOR_OPEN,
            SoundEvents.WOODEN_TRAPDOOR_CLOSE,
            SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_DOOR_CLOSE,
            // Coffre (quelqu'un fouille…)
            SoundEvents.CHEST_OPEN,
            SoundEvents.CHEST_CLOSE,
            // Mobs hostiles (sons éloignés, inquiétants)
            SoundEvents.WARDEN_HEARTBEAT,
            // Blocs et mécanismes (bruits industriels / surnaturels)
            SoundEvents.CHAIN_STEP,
            SoundEvents.SOUL_SAND_STEP,
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
    };

    private static void distantStage(ServerPlayer target, ItFollowsConfig config, long now, RandomSource random) {
        if (now < nextEventTime) {
            return;
        }
        // Position spatialisée : direction horizontale aléatoire, distance « lointaine ».
        double distance = lerp(random, config.hauntDistantSoundMinDistance, config.hauntDistantSoundMaxDistance);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double x = target.getX() + Math.cos(angle) * distance;
        double z = target.getZ() + Math.sin(angle) * distance;
        double y = target.getEyeY();
        float pitch = 0.85f + random.nextFloat() * 0.3f;
        // Choix aléatoire parmi le pool de sons vanilla effrayants.
        SoundEvent chosen = SCARY_SOUNDS[random.nextInt(SCARY_SOUNDS.length)];
        ItFollowsNetworking.playSoundTo(target, chosen, x, y, z, config.hauntDistantSoundVolume, pitch);
        nextEventTime = now + interval(random,
                config.hauntDistantSoundMinIntervalTicks, config.hauntDistantSoundMaxIntervalTicks);
    }

    // --- Étape 2 : événements physiques réels (portes / blocs) ----------------------------------

    private static void physicalStage(ServerPlayer target, ItFollowsConfig config, long now, RandomSource random) {
        if (now < nextEventTime) {
            return;
        }
        physicalEvent((ServerLevel) target.level(), target, config, random);
        nextEventTime = now + interval(random,
                config.hauntPhysicalEventMinIntervalTicks, config.hauntPhysicalEventMaxIntervalTicks);
    }

    /** Ouvre une porte en bois proche (prioritaire, évocateur) ; à défaut, casse un bloc réel à proximité. */
    private static void physicalEvent(ServerLevel level, ServerPlayer target, ItFollowsConfig config, RandomSource random) {
        int r = config.hauntPhysicalEventRadius;
        BlockPos center = target.blockPosition();

        BlockPos door = sampleNearby(level, center, r, random, pos -> isOpenableWood(level.getBlockState(pos)));
        if (door != null) {
            openWood(level, door);
            return;
        }
        BlockPos block = sampleNearby(level, center, r, random, pos ->
                !pos.equals(center.below()) && isBreakable(level, pos, config));
        if (block != null) {
            // destroyBlock émet particules + son vanilla, donc audibles/visibles par tous (« conséquences »).
            level.destroyBlock(block, config.stalkerBreakDropItems);
        }
    }

    // --- Étape 3 : flashs de silhouette (leurre immobile, ~1 s) ----------------------------------

    private static void silhouetteStage(MinecraftServer server, StalkTrackerState state, ServerPlayer target,
                                        ItFollowsConfig config, long now, RandomSource random) {
        if (silhouetteRemoveAt > 0L) {
            // Flash en cours : on le retire à échéance, puis on programme le suivant.
            if (now >= silhouetteRemoveAt) {
                clearSilhouette(server, state);
                silhouetteRemoveAt = 0L;
                nextEventTime = now + interval(random,
                        config.hauntSilhouetteMinIntervalTicks, config.hauntSilhouetteMaxIntervalTicks) + interval(random, 800, 1200);
            }
            return;
        }
        if (now < nextEventTime) {
            return;
        }
        // Apparition d'un leurre immobile dans le champ de vision (périphérique) de la cible.
        Vec3 pos = inViewCone(target, config, random);
        StalkerEntity decoy = HauntController.spawn(server, state, target, pos, true, config);
        if (decoy != null) {
            silhouetteRemoveAt = now + config.hauntSilhouetteDurationTicks;
        } else {
            // Échec de spawn (chunk non chargé ?) : on retente bientôt.
            nextEventTime = now + 20L;
        }
    }

    private static void clearSilhouette(MinecraftServer server, StalkTrackerState state) {
        HauntController.discardAllStalkers(server);
        state.setStalkerEntityId(null);
    }

    // --- Révélation : « LOOK BEHIND YOU » --------------------------------------------------------

    private static void revealStage(MinecraftServer server, StalkTrackerState state, ServerPlayer target,
                                    ItFollowsConfig config, long now) {
        if (!revealSpawned) {
            Vec3 behind = HauntController.behindTarget(target, config.revealDistance);
            StalkerEntity decoy = HauntController.spawn(server, state, target, behind, true, config);
            if (decoy == null) {
                return; // on retentera au tick suivant
            }
            // Voix jouée DERRIÈRE la cible (à la position du leurre) : « regarde derrière toi ».
            ItFollowsNetworking.playSoundTo(target, ModSounds.LOOK_BEHIND_YOU,
                    decoy.getX(), decoy.getEyeY(), decoy.getZ(), config.lookBehindVolume, 1.0f);
            revealSpawned = true;
            lookHoldTicks = 0;
            return;
        }

        StalkerEntity decoy = HauntController.resolveEntity(server, state);
        if (decoy == null || decoy.isRemoved()) {
            // Leurre perdu (déchargement…) : on le refera apparaître.
            revealSpawned = false;
            return;
        }

        if (isLookingAt(target, decoy, config)) {
            if (++lookHoldTicks >= config.revealLookHoldTicks) {
                // La cible a « vu » le leurre : il disparaît, et la vraie traqueuse réapparaît le plus
                // loin possible derrière elle (pas « à côté » du leurre) pour relancer une vraie traque.
                StalkerEntity hunter = HauntController.spawnFarHunter(server, state, target, config);
                if (hunter != null) {
                    // Grondement lointain (audible de la seule cible) : la traque commence.
                    ItFollowsNetworking.playSoundTo(target, ModSounds.HAUNT_DISTANT,
                            hunter.getX(), hunter.getEyeY(), hunter.getZ(), config.hauntDistantSoundVolume, 1.0f);
                }
                state.setHauntPhase(HauntPhase.HUNTING);
                state.setPhaseStartTime(now);
                resetTimers();
            }
        } else {
            lookHoldTicks = 0;
        }
    }

    /** La cible vise-t-elle le leurre ? Raycast du regard contre la boîte englobante (légèrement gonflée). */
    private static boolean isLookingAt(ServerPlayer target, StalkerEntity decoy, ItFollowsConfig config) {
        Vec3 eye = target.getEyePosition();
        Vec3 end = eye.add(target.getViewVector(1.0f).scale(config.revealLookDetectRange));
        AABB box = decoy.getBoundingBox().inflate(0.3);
        return box.clip(eye, end).isPresent();
    }

    // --- Helpers ---------------------------------------------------------------------------------

    /** Position dans un cône frontal de la cible (vision périphérique), à distance « silhouette ». */
    private static Vec3 inViewCone(ServerPlayer target, ItFollowsConfig config, RandomSource random) {
        float spread = config.hauntSilhouetteConeDegrees;
        float yawDeg = target.getYRot() + (random.nextFloat() * 2.0f - 1.0f) * spread;
        double yaw = Math.toRadians(yawDeg);
        // Convention Minecraft : yaw 0 = +Z, +90 = -X.
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        double distance = lerp(random, config.hauntSilhouetteMinDistance, config.hauntSilhouetteMaxDistance);
        return new Vec3(target.getX() + dx * distance, target.getY(), target.getZ() + dz * distance);
    }

    @FunctionalInterface
    private interface PosPredicate {
        boolean test(BlockPos pos);
    }

    /** Tire jusqu'à 40 positions aléatoires dans la boîte (±r horizontal, ±3 vertical) et renvoie la 1re valide. */
    private static BlockPos sampleNearby(ServerLevel level, BlockPos center, int r, RandomSource random, PosPredicate ok) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 40; i++) {
            int x = center.getX() + random.nextInt(2 * r + 1) - r;
            int z = center.getZ() + random.nextInt(2 * r + 1) - r;
            int y = center.getY() + random.nextInt(7) - 3;
            pos.set(x, y, z);
            if (level.isLoaded(pos) && ok.test(pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Bloc cassable réel : a une collision, dureté ≥ 0 (pas bedrock), sous le plafond de dureté config. */
    private static boolean isBreakable(ServerLevel level, BlockPos pos, ItFollowsConfig config) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isOpenableWood(state)) {
            return false;
        }
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f) {
            return false; // incassable (bedrock, barrière…)
        }
        float cap = config.stalkerBreakMaxHardness;
        return cap < 0.0f || hardness <= cap;
    }

    /** Porte / trappe / portillon en bois <b>fermé</b> (ouvrable). */
    private static boolean isOpenableWood(BlockState state) {
        if (state.is(BlockTags.WOODEN_DOORS)) {
            return !state.getValue(DoorBlock.OPEN);
        }
        if (state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return !state.getValue(TrapDoorBlock.OPEN);
        }
        if (state.is(BlockTags.FENCE_GATES)) {
            return !state.getValue(FenceGateBlock.OPEN);
        }
        return false;
    }

    private static void openWood(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(null, level, state, pos, true);
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /** Tirage entier dans [min, max] (ticks). */
    private static long interval(RandomSource random, int min, int max) {
        if (max <= min) {
            return Math.max(1, min);
        }
        return min + random.nextInt(max - min + 1);
    }

    private static double lerp(RandomSource random, float min, float max) {
        return min + random.nextDouble() * Math.max(0.0f, max - min);
    }
}
