package io.github.yutoutcourt.itfollows.entity.ai;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal <b>unique</b> de la traqueuse (refonte « ESM-style »). Remplace l'ancien trio
 * {@code StalkerHuntGoal + StalkerPathfinder + StalkerPath} (un A* voxel 3D maison qui se battait
 * contre le moteur et produisait jitter, envols et blocages).
 *
 * <p>Philosophie reprise du mod <i>NightmareESM</i> : on s'appuie sur la <b>navigation vanilla</b>
 * (robuste, lissée) pour la traque au sol, et on superpose deux <b>behaviors réactifs</b> déclenchés
 * par des signaux simples — le <b>vol</b> (qui remplace la pose de blocs / pont d'ESM) et la
 * <b>casse</b> (dernier recours, façon {@code MobDig}). À chaque tick on choisit <b>un seul mode</b> :
 *
 * <ul>
 *   <li><b>VOL</b> : la cible est nettement au-dessus, <i>ou</i> la nav vanilla ne peut pas l'atteindre
 *       ({@code !path.canReach()}, équivalent de {@code CannotReachPlayer}) / on stagne, <i>et</i> il y
 *       a de l'air pour monter. On vise la position réelle de la cible (Y plafonné à {@code target.y+1}
 *       → plus d'« envol à 200 blocs »), on atterrit dès qu'on peut de nouveau marcher jusqu'à elle.</li>
 *   <li><b>CASSE</b> : on est réellement bloqué, voler ne résout pas (cible scellée, pas d'air au-dessus)
 *       et un bloc cassable barre la route. On casse <b>uniquement</b> ce bloc (raycast cardinal vers la
 *       cible), progressivement — jamais à distance, jamais en gruyère.</li>
 *   <li><b>SOL</b> (défaut) : {@code navigation.moveTo(target)}. La {@code GroundPathNavigation} gère
 *       marche, montée d'1 bloc ({@code maxUpStep 1.1}), flottaison, etc.</li>
 * </ul>
 */
public final class StalkerChaseGoal extends Goal {

    private enum Mode { GROUND, FLY, DIG }

    /** Intervalle (ticks) entre deux recalculs de chemin / sondes d'atteignabilité (garde-fou perf). */
    private static final int REPLAN_INTERVAL = 10;
    /** Ticks sans progression horizontale au-delà desquels on considère l'entité « coincée ». */
    private static final int STUCK_TICKS = 16;
    /** Progression horizontale² minimale (blocs²) pour réarmer le compteur anti-blocage. */
    private static final double MOVE_EPSILON_SQR = 0.0064; // 0.08 bloc

    private final StalkerEntity stalker;

    private Mode mode = Mode.GROUND;
    private int groundRepathCooldown;
    /** Ticks écoulés depuis le décollage (hystérésis anti-clignotement décollage/atterrissage). */
    private int flyTicks;
    /** Ticks consécutifs où la cible est restée perchée hors de portée au sol (délai avant l'envol). */
    private int aboveTicks;
    /** Vrai tant qu'une session de creuse est en cours : on tunnelle sans relâche jusqu'à percer. */
    private boolean digLatched;

    /** Suivi anti-blocage : dernière position horizontale et tick de la dernière progression. */
    private double lastX;
    private double lastZ;
    private int lastProgressTick;

    /** Interception : suivi de la position de la cible pour estimer sa vélocité. */
    private Vec3 lastTargetPos;
    private int lastTargetTick;

    /** État de la casse progressive du bloc courant. */
    private BlockPos digBlock;
    private float digProgress;
    private int digStage = -1;

    public StalkerChaseGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return stalker.resolveTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return stalker.resolveTarget() != null;
    }

    @Override
    public void tick() {
        ServerPlayer target = stalker.resolveTarget();
        if (target == null) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        stalker.getLookControl().setLookAt(target, 30.0f, 30.0f);

        double dy = target.getY() - stalker.getY();
        double distXZ = horizontalDistanceTo(target);

        // --- 0) FLUIDE (priorité absolue) : nage droit vers la cible en 3D ----------------------
        // La nav vanilla « flotte » mal et peine à ressortir. On reprend la main : on vise directement
        // la cible (Y compris), ce qui traverse l'eau ET grimpe sur la berge (le yya de steer3D soulève).
        if (stalker.isInWater() || (stalker.isInLava() && config.stalkerCanEnterLava)) {
            swimMode(target, config);
            return;
        }

        boolean stuck = updateStuck();

        // --- 1) POURSUITE VERTICALE (vol + minage), façon MobBuildUp mais en volant ------------
        // La cible est-elle perchée et proche horizontalement ? On accumule un délai d'observation
        // avant de décoller (tempo « It Follows »).
        boolean targetHigh = dy > config.stalkerFlyTriggerHeight
                && distXZ <= config.stalkerFlyApproachRange;
        aboveTicks = targetHigh ? aboveTicks + 1 : 0;

        if (config.stalkerCanFly) {
            if (stalker.isFlying()) {
                // En vol : on continue tant qu'on n'a pas rejoint le niveau de la cible (hystérésis).
                if (flyTicks < config.stalkerMinFlightTicks || dy > 1.0) {
                    verticalMode(target, config);
                    return;
                }
            } else if (targetHigh) {
                if (aboveTicks < config.stalkerFlyDelayTicks) {
                    // Phase d'observation : on s'approche au sol et on regarde, sans décoller ni miner
                    // vers le haut (l'aspect « il t'observe puis déploie ses ailes »).
                    groundMode(target, config);
                    return;
                }
                verticalMode(target, config);
                return;
            }
        }

        // --- 2) POURSUITE VERTICALE VERS LE BAS + CASSE LATCHÉE --------------------------------
        // Symétrique du vol : si la cible est nettement EN DESSOUS et proche horizontalement, on creuse
        // vers elle SANS attendre d'être « coincé ». Sinon (cible à niveau / au-dessus mais vol off), la
        // casse s'amorce dès qu'on stagne. Une fois engagée, on tunnelle SANS relâche (digLatched)
        // jusqu'à ce qu'aucun bloc ne barre plus la route — « il a miné jusqu'à toi ».
        boolean targetLow = dy < -config.stalkerFlyTriggerHeight
                && distXZ <= config.stalkerFlyApproachRange;
        if ((targetLow || stuck || digLatched) && config.stalkerCanBreakBlocks) {
            BlockPos digTarget = blockingBlockToward(target, config);
            if (digTarget != null) {
                digLatched = true;
                digMode(target, digTarget, config);
                return;
            }
            // Rien de cassable juste en dessous (puits déjà ouvert) : on laisse la nav/le filet ramper
            // vers la cible. On ne ferme la session de creuse que si elle n'est pas réamorcée par targetLow.
            if (!targetLow) {
                digLatched = false;
            }
        }

        // --- 3) SOL (défaut) : navigation vanilla ----------------------------------------------
        groundMode(target, config);
    }

    // --- MODE 1 : POURSUITE VERTICALE (vol, + minage du plafond en vol stationnaire) ---

    private void verticalMode(ServerPlayer target, ItFollowsConfig config) {
        if (mode != Mode.FLY) {
            mode = Mode.FLY;
            stalker.getNavigation().stop(); // la nav au sol ne doit pas se battre avec le pilotage 3D
            flyTicks = 0;
        }
        stalker.setFlyingMode(true);
        flyTicks++;

        // La montée est-elle bouchée par un bloc cassable juste au-dessus de la tête ? On le mine en vol
        // stationnaire (sans pousser dans le solide), puis on remonte au tick suivant. C'est le « vol +
        // minage simultané » : plus de piège dans une grotte refermée au-dessus.
        BlockPos above = stalker.blockPosition().above(2);
        if (config.stalkerCanBreakBlocks && dy(target) > 0.0 && isBreakableSolid(above, config)) {
            // Vol stationnaire : viser sa propre position annule zza/yya (steer3D), on ne dérive pas.
            stalker.getMoveControl().setWantedPosition(stalker.getX(), stalker.getY(), stalker.getZ(), 1.0);
            progressBreak(above, config);
            return;
        }
        clearDig();

        Vec3 aim = aimPoint(target, config);
        // On vise TOUJOURS +1 au-dessus de la cible : en sortant d'une grotte / d'un tunnel vertical,
        // ce bloc de marge permet de franchir la lèvre de l'ouverture (sinon l'entité bute sur le
        // dernier bloc du rebord et redescend). Anti-emballement préservé : la cible borne la montée
        // (jamais plus de target.y+1) — fini le « bug envol à 200 blocs ».
        double wantedY = target.getY() + 1.0;
        stalker.getMoveControl().setWantedPosition(aim.x, wantedY, aim.z, 1.0);
    }

    // --- MODE FLUIDE : nage 3D droit vers la cible (traverse + ressort sur la berge) ---

    private void swimMode(ServerPlayer target, ItFollowsConfig config) {
        if (mode != Mode.GROUND) {
            // On laisse le mode logique à GROUND : à la sortie de l'eau, la traque au sol reprend seule.
            mode = Mode.GROUND;
        }
        stalker.setFlyingMode(false);
        clearDig();
        stalker.getNavigation().stop();
        // Vise la cible en 3D (+1 pour franchir la lèvre de la berge en sortant). steer3D nage vers elle.
        double wantedY = target.getY() + 1.0;
        // Si un rebord solide barre la sortie à hauteur des pieds, on vise franchement vers le haut :
        // l'entité grimpe le long de la berge avant d'avancer, au lieu de presser dans le mur et stagner.
        Direction dir = horizontalDirToward(target);
        BlockPos ahead = stalker.blockPosition().relative(dir);
        if (!stalker.level().getBlockState(ahead).getCollisionShape(stalker.level(), ahead).isEmpty()) {
            wantedY = Math.max(wantedY, stalker.getY() + 2.0);
        }
        stalker.getMoveControl().setWantedPosition(target.getX(), wantedY, target.getZ(), 1.0);
    }

    private double dy(ServerPlayer target) {
        return target.getY() - stalker.getY();
    }

    // --- MODE 2 : CASSE (dernier recours, façon MobDig réactif) ---

    private void digMode(ServerPlayer target, BlockPos toBreak, ItFollowsConfig config) {
        if (mode != Mode.DIG) {
            mode = Mode.DIG;
            stalker.getNavigation().stop();
        }
        stalker.setFlyingMode(false);
        // On avance vers la cible TOUT EN cassant : dès que le bloc cède, le corps progresse dans la
        // trouée et enchaîne le bloc suivant — tunnel continu jusqu'à percer, sans pause entre les blocs.
        // (Presser droit vers la cible n'est pas du jitter : c'est l'A* maison qui en produisait avant.)
        stalker.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        progressBreak(toBreak, config);
    }

    // --- MODE 3 : SOL (navigation vanilla) ---

    private void groundMode(ServerPlayer target, ItFollowsConfig config) {
        boolean entering = mode != Mode.GROUND;
        mode = Mode.GROUND;
        digLatched = false; // on marche librement : la session de creuse est close
        stalker.setFlyingMode(false);
        clearDig();
        openDoorsInFront(target);
        // Recalcul du chemin throttlé (comme MeleeAttackGoal vanilla) ; entre deux, la nav suit seule.
        if (entering || --groundRepathCooldown <= 0) {
            groundRepathCooldown = REPLAN_INTERVAL;
            stalker.getNavigation().moveTo(target, 1.0);
        }
        // Filet : si la nav n'a aucun chemin (cible momentanément injoignable au sol), on pousse quand
        // même le corps droit vers la cible. L'entité « rampe » toujours dans la bonne direction au lieu
        // de rester plantée — c'est un mob inéluctable, jamais inerte.
        if (stalker.getNavigation().isDone()) {
            stalker.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        }
    }

    // --- Anti-blocage ---

    /**
     * Met à jour le suivi anti-blocage (progression horizontale uniquement : monter/sauter sur place ne
     * compte pas) et renvoie {@code true} si l'entité n'a pas progressé depuis {@link #STUCK_TICKS} ticks.
     */
    private boolean updateStuck() {
        double dx = stalker.getX() - lastX;
        double dz = stalker.getZ() - lastZ;
        if (dx * dx + dz * dz > MOVE_EPSILON_SQR) {
            lastX = stalker.getX();
            lastZ = stalker.getZ();
            lastProgressTick = stalker.tickCount;
        }
        return (stalker.tickCount - lastProgressTick) >= STUCK_TICKS;
    }

    private double horizontalDistanceTo(ServerPlayer target) {
        double dx = target.getX() - stalker.getX();
        double dz = target.getZ() - stalker.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Direction cardinale dominante vers la cible (axe X ou Z selon le plus grand écart). */
    private Direction horizontalDirToward(ServerPlayer target) {
        double dx = target.getX() - stalker.getX();
        double dz = target.getZ() - stalker.getZ();
        return Math.abs(dx) >= Math.abs(dz)
                ? (dx >= 0 ? Direction.EAST : Direction.WEST)
                : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
    }

    // --- Interception : vise où la cible va (vélocité mesurée × stalkerLeadTicks) ---

    private Vec3 aimPoint(ServerPlayer target, ItFollowsConfig config) {
        Vec3 current = target.position();
        int lead = config.stalkerLeadTicks;
        if (lead <= 0 || lastTargetPos == null) {
            lastTargetPos = current;
            lastTargetTick = stalker.tickCount;
            return current;
        }
        int dt = Math.max(1, stalker.tickCount - lastTargetTick);
        Vec3 velocity = current.subtract(lastTargetPos).scale(1.0 / dt);
        lastTargetPos = current;
        lastTargetTick = stalker.tickCount;
        return current.add(velocity.scale(lead));
    }

    // --- Casse : un seul bloc à la fois, dans la direction de la cible ---

    /**
     * Bloc cassable qui barre la route vers la cible (raycast cardinal façon {@code GetBlockingBlock}
     * d'ESM : pieds + tête, et plafond/sol selon que la cible est au-dessus/en-dessous). Renvoie
     * {@code null} si rien de cassable ne barre, ou si l'entité est en fluide.
     */
    private BlockPos blockingBlockToward(ServerPlayer target, ItFollowsConfig config) {
        // if (stalker.isInWater() || stalker.isInLava()) {
        //     return null;
        // }
        BlockPos feet = stalker.blockPosition();
        // STICKINESS : tant qu'un bloc est en cours de cassage, qu'il reste cassable et à portée, on
        // s'acharne dessus jusqu'à le percer. Sans ça, les micro-déplacements du corps (digMode pousse
        // vers la cible) recalculent les candidats à chaque tick, désignant parfois un autre bloc → la
        // progression repart de zéro et l'entité « commence à casser puis s'arrête » sans jamais finir.
        if (digBlock != null && digStage >= 0 && isBreakableSolid(digBlock, config)
                && feet.distSqr(digBlock) <= 4.5) {
            return digBlock;
        }

        double dx = target.getX() - stalker.getX();
        double dz = target.getZ() - stalker.getZ();
        Direction dir = horizontalDirToward(target);

        double dyTarget = target.getY() - stalker.getY();
        boolean offset = (dx * dx + dz * dz) > 2.25; // cible décalée de > 1,5 bloc à l'horizontale
        BlockPos[] cands;
        if (dyTarget > 1.5) {
            // Cible au-dessus (vol désactivé) : perce le plafond pour tunneler vers le haut.
            cands = new BlockPos[]{feet.above(2), feet.relative(dir), feet.above().relative(dir)};
        } else if (dyTarget < -1.5) {
            // Cible en dessous : escalier descendant si elle est décalée (avant-bas + tête), sinon puits
            // vertical droit si elle est juste sous l'entité — pour la rejoindre coûte que coûte.
            cands = offset
                    ? new BlockPos[]{feet.relative(dir).below(), feet.relative(dir), feet.above().relative(dir)}
                    : new BlockPos[]{feet.below()};
        } else {
            cands = new BlockPos[]{feet.relative(dir), feet.above().relative(dir)};
        }
        for (BlockPos p : cands) {
            if (isBreakableSolid(p, config)) {
                return p;
            }
        }
        return null;
    }

    /** Casse progressive d'un bloc (fissures visibles, vitesse ∝ 1/dureté). */
    private void progressBreak(BlockPos toBreak, ItFollowsConfig config) {
        Level level = stalker.level();
        if (digBlock == null || !digBlock.equals(toBreak)) {
            resetDigProgress();
            digBlock = toBreak;
        }
        BlockState state = level.getBlockState(toBreak);
        float hardness = Math.max(state.getDestroySpeed(level, toBreak), 0.05f);
        digProgress += config.stalkerBreakSpeed / hardness;

        int stage = Mth.clamp((int) (digProgress * 10.0f), 0, 9);
        if (stage != digStage) {
            level.destroyBlockProgress(stalker.getId(), toBreak, stage);
            digStage = stage;
        }
        if (digProgress >= 1.0f) {
            level.destroyBlock(toBreak, config.stalkerBreakDropItems, stalker);
            level.destroyBlockProgress(stalker.getId(), toBreak, -1);
            resetDigProgress();
        }
    }

    private void clearDig() {
        if (digBlock != null && digStage >= 0) {
            stalker.level().destroyBlockProgress(stalker.getId(), digBlock, -1);
        }
        resetDigProgress();
    }

    private void resetDigProgress() {
        digBlock = null;
        digProgress = 0.0f;
        digStage = -1;
    }

    private boolean isBreakableSolid(BlockPos pos, ItFollowsConfig config) {
        Level level = stalker.level();
        BlockState state = level.getBlockState(pos);
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        if (isOpenableWood(state)) {
            return false; // une porte s'ouvre, ne se casse pas
        }
        if (!config.stalkerCanBreakBlocks) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f) {
            return false; // incassable (bedrock, barrière…)
        }
        float cap = config.stalkerBreakMaxHardness;
        return cap < 0.0f || hardness <= cap;
    }

    // --- Portes : ouverture instantanée du bois devant l'entité ---

    private void openDoorsInFront(ServerPlayer target) {
        if (!ItFollowsConfig.get().stalkerOpenDoors) {
            return;
        }
        BlockPos feet = stalker.blockPosition().relative(horizontalDirToward(target));
        tryOpen(feet);
        tryOpen(feet.above());
    }

    private void tryOpen(BlockPos pos) {
        Level level = stalker.level();
        BlockState state = level.getBlockState(pos);
        if (!isOpenableWood(state)) {
            return;
        }
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(stalker, level, state, pos, true);
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /** Porte / trappe / portillon en bois <b>fermé</b> (ouvrable instantanément). */
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

    @Override
    public void stop() {
        clearDig();
        lastTargetPos = null;
        mode = Mode.GROUND;
        PathNavigation nav = stalker.getNavigation();
        if (nav != null) {
            nav.stop();
        }
    }
}
