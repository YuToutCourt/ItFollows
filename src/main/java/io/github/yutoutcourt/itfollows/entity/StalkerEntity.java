package io.github.yutoutcourt.itfollows.entity;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.ai.StalkerChaseGoal;
import io.github.yutoutcourt.itfollows.entity.ai.StalkerMoveControl;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Entité traqueuse (Phase 2). Réelle côté serveur mais visible du seul joueur traqué
 * (cf. {@code ChunkMapTrackedEntityMixin}). Invincible, lente, traverse les joueurs, inflige des
 * dégâts <b>uniquement à sa cible</b> au contact. Spawn/despawn et choix de la cible sont pilotés
 * par {@code HauntController} ; l'entité ne fait que suivre l'UUID qu'on lui donne.
 *
 * <p><b>IA (refonte ESM-style)</b> : un <b>unique</b> {@link StalkerChaseGoal} s'appuie sur la
 * <b>navigation vanilla</b> pour la traque au sol et superpose deux behaviors réactifs — le
 * <b>vol</b> (poursuite verticale / franchissement, qui remplace la pose de blocs) et la <b>casse</b>
 * (dernier recours, cible scellée). L'entité expose les primitives physiques que le goal pilote :
 * le <b>mode vol</b> ({@link #setFlyingMode}) et le <b>saut chargé</b> ({@link #tryJump}). Le reste
 * (nage en eau/lave) est géré par {@link #travel} selon le milieu réel. Fini l'A* voxel maison qui
 * se battait contre le moteur (jitter, envols, blocages).
 *
 * <p>Elle n'est pas écrite sur le disque ({@link #shouldBeSaved()} = {@code false}) : elle n'existe
 * qu'à proximité de la cible et est recréée par le controller après un déchargement.
 */
public class StalkerEntity extends PathfinderMob implements GeoEntity {

    // --- Animations GeckoLib (assets/itfollows/animations/toww_geckolib.animation.json) ---
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.TOWWGeckolib.pose1");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.TOWWGeckolib.walk");
    private static final RawAnimation CHASE = RawAnimation.begin().thenLoop("animation.TOWWGeckolib.chase");
    /**
     * Poses statiques (étape 3 « silhouette » + révélation) : l'entité-leurre prend l'une de ces postures
     * figées en fixant le joueur. Indexées par {@link #POSE} (0..5).
     */
    private static final RawAnimation[] POSES = {
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose1"),
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose2"),
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose3"),
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose4"),
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose5"),
            RawAnimation.begin().thenPlayAndHold("animation.TOWWGeckolib.pose6"),
    };

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Synchronisé au client pour piloter l'animation de vol (ailes). */
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Synchronisé au client : index de la pose figée jouée en mode leurre (silhouette / révélation),
     * dans {@code [0, POSES.length)}. {@code -1} = pas de pose imposée (entité normale : idle/marche/poursuite).
     */
    private static final EntityDataAccessor<Integer> POSE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);

    /** Distance² (blocs²) sous laquelle l'entité frappe la cible. 4 = 2 blocs. */
    private static final double ATTACK_RANGE_SQR = 4.0;
    /** Délai (ticks) entre deux coups. */
    private static final int ATTACK_COOLDOWN = 20;

    private UUID targetUuid;
    private int attackCooldown;
    /** Anti-spam : ticks restants avant de pouvoir réarmer un saut. */
    private int jumpCooldown;
    /**
     * Mode « leurre » (Phase 2) : l'entité est <b>immobile</b> et inoffensive — elle ne traque ni ne
     * frappe. Sert au flash de silhouette (étape 3) et à la révélation « LOOK BEHIND YOU » avant la
     * traque réelle. Implémenté via {@code setNoAi(true)} (le goal ne tourne pas) + garde dans {@code tick()}.
     */
    private boolean decoy;

    public StalkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Pas de despawn par éloignement : c'est le controller qui décide.
        this.setPersistenceRequired();
        // Monte un bloc plein sans sauter (et franchit les rebords / sort de l'eau plus facilement).
        this.setMaxUpStep(1.1f);
        // Pilotage de déplacement maison (nage / vol 3D), cf. StalkerMoveControl.
        this.moveControl = new StalkerMoveControl(this);
        // L'eau ne pénalise pas le pathfinding (le planificateur la traverse de toute façon).
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
        // Idem la lave : ignifugée, elle doit pouvoir y entrer.
        if (ItFollowsConfig.get().stalkerCanEnterLava) {
            this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0f);
            this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0f);
            this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                // FOLLOW_RANGE pilote maxVisitedNodes de la nav vanilla (≈ range×16). On le garde bas :
                // la traque à courte portée suffit, la longue distance est couverte par HauntController.
                .add(Attributes.FOLLOW_RANGE, 1000.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
        this.entityData.define(POSE, -1);
    }

    /**
     * Navigation au sol vanilla : c'est elle qui route la traque au sol (marche / portes / flottaison).
     * Le {@link StalkerChaseGoal} l'engage via {@code moveTo}, et la prend de court par le vol ou la
     * casse quand elle ne peut pas atteindre la cible.
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanOpenDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StalkerChaseGoal(this));
    }

    // --- Cible ---

    public void setTargetUuid(UUID uuid) {
        this.targetUuid = uuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    // --- Mode leurre (immobile, inoffensif) ---

    public boolean isDecoy() {
        return decoy;
    }

    /**
     * Bascule le mode leurre. En leurre : {@code NoAi} (le {@code StalkerChaseGoal} ne tourne pas, donc
     * aucun déplacement) et aucune attaque (cf. {@code tick()}). On le pose au sol, il reste planté à
     * regarder où on l'a tourné. Sortir du mode leurre lui rend sa traque normale.
     */
    public void setDecoy(boolean decoy) {
        this.decoy = decoy;
        this.setNoAi(decoy);
        // En leurre : on tire une pose figée aléatoire (chaque flash de silhouette / révélation diffère).
        // Hors leurre : pas de pose imposée, le contrôleur d'anim reprend idle/marche/poursuite.
        this.entityData.set(POSE, decoy ? this.random.nextInt(POSES.length) : -1);
    }

    /** Résout la cible si elle est en ligne et dans la même dimension que l'entité, sinon {@code null}. */
    public ServerPlayer resolveTarget() {
        if (targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(targetUuid);
        if (player != null && player.level() == this.level()) {
            return player;
        }
        return null;
    }

    // --- Mode vol (piloté par le goal selon le pas FLY du chemin) ---

    /** Vrai si l'entité est en mode vol (lu par le client pour l'animation d'ailes). */
    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    /**
     * Active/désactive le mode vol. Au décollage : coupe la gravité. À l'atterrissage : rétablit la
     * gravité et purge l'élan vertical résiduel pour ne pas « rebondir ». Idempotent.
     */
    public void setFlyingMode(boolean fly) {
        if (this.isFlying() == fly) {
            return;
        }
        this.entityData.set(FLYING, fly);
        this.setNoGravity(fly);
        if (!fly) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
    }

    /**
     * Saut « boosté » pour franchir un rebord 1..{@code stalkerJumpMaxHeight} blocs plus haut.
     * Applique une impulsion verticale calibrée sur la hauteur ; l'élan horizontal vient du
     * {@link StalkerMoveControl} (qui pousse déjà vers la case du pas). Anti-spam par cooldown.
     */
    public void tryJump(double height) {
        if (jumpCooldown > 0 || !this.onGround()) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        double h = Mth.clamp(height, 1.0, config.stalkerJumpMaxHeight);
        // v ≈ 0.42·√(h/1.25) reproduit le saut vanilla (0.42 → 1.25 bloc) mis à l'échelle, + marge.
        double vy = 0.42 * Math.sqrt((h + 0.5) / 1.25);
        Vec3 dm = this.getDeltaMovement();
        this.setDeltaMovement(dm.x, vy, dm.z);
        this.hasImpulse = true;
        jumpCooldown = config.stalkerJumpCooldownTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        // Jamais blessée par l'environnement : on nettoie le feu par sécurité visuelle.
        this.clearFire();
        // Sécurité : jamais en vol dans un fluide (la nage prime), au cas où on y tomberait en volant.
        if (this.isFlying() && (this.isInWater() || this.isInLava())) {
            this.setFlyingMode(false);
        }
        // Anti-catapulte : borne la vitesse verticale ASCENDANTE. Le saut a besoin de ~0.79 ; au-delà
        // c'est forcément un emballement (courant + saut/vol) → on plafonne. La descente reste libre
        // (entité invincible, pas de dégâts de chute).
        Vec3 dm = this.getDeltaMovement();
        if (dm.y > 0.85) {
            this.setDeltaMovement(dm.x, 0.85, dm.z);
        }

        // Un leurre ne frappe jamais : il n'est là que pour être vu/visé (silhouette, « LOOK BEHIND YOU »).
        // Même planté (NoAi), il pivote chaque tick pour fixer en permanence le joueur traqué.
        if (this.decoy) {
            faceTarget();
            return;
        }

        ServerPlayer target = resolveTarget();
        if (target != null) {
            double distSqr = this.distanceToSqr(target);
            if (attackCooldown == 0 && distSqr < ATTACK_RANGE_SQR) {
                this.doHurtTarget(target);
                attackCooldown = ATTACK_COOLDOWN;
                // Rarement, un éclat de panique dans sa voix, audible de la seule cible (depuis sa position).
                if (this.random.nextFloat() < 0.3f) {
                    ItFollowsNetworking.playSoundTo(target, ModSounds.ENTITY_PANIC,
                            this.getX(), this.getEyeY(), this.getZ(), 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * Oriente le leurre (corps + tête) vers le joueur traqué, à l'horizontale. Appelé chaque tick en mode
     * leurre pour qu'il « fixe » la cible où qu'elle aille, même immobile (silhouette + révélation).
     */
    private void faceTarget() {
        ServerPlayer target = resolveTarget();
        if (target == null) {
            return;
        }
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        // yRotO/yBodyRotO évitent l'interpolation visible d'un grand pivot entre deux ticks.
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }

    // --- Physique de déplacement : eau/lave (pattern Axolotl), vol 3D, sinon marche vanilla ---

    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi() && (this.isInWater() || (this.isInLava() && canEnterLava()))) {
            // Avant de nager : on annule le courant pour ne pas être repoussée (cf. note ci-dessous).
            float resist = ItFollowsConfig.get().stalkerCurrentResistance;
            if (resist > 0.0f) {
                Vec3 flow = this.level().getFluidState(this.blockPosition())
                        .getFlow(this.level(), this.blockPosition());
                if (flow.lengthSqr() > 1.0e-6) {
                    double pushScale = this.isInWater() ? 0.014 : 0.007;
                    this.setDeltaMovement(this.getDeltaMovement()
                            .subtract(flow.normalize().scale(pushScale * resist)));
                }
            }
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 dm = this.getDeltaMovement().scale(0.9);
            // Borne la vitesse verticale en fluide : assez pour s'extraire sur la berge (0.5), sans le
            // « catapultage » d'antan. Le plafond global de tick() couvre le reste.
            if (dm.y > 0.5) {
                dm = new Vec3(dm.x, 0.5, dm.z);
            }
            this.setDeltaMovement(dm);
        } else if (this.isFlying()) {
            // Vol : poussée 3D (l'input.y porté par yya monte/descend), sans gravité, friction air.
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
        } else {
            super.travel(input);
        }
    }

    /** Vrai si la lave est traversable (ignifugée + activée en config). */
    private boolean canEnterLava() {
        return ItFollowsConfig.get().stalkerCanEnterLava;
    }

    /** Pose de nage pour l'animation, sans bascule de navigation (l'IA route elle-même). */
    @Override
    public void updateSwimming() {
        if (this.level().isClientSide()) {
            super.updateSwimming();
            return;
        }
        this.setSwimming(this.isEffectiveAi() && (this.isInWater() || this.isInLava()));
    }

    // --- Invincibilité ---

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    // --- Fantôme vis-à-vis des joueurs : aucune poussée, pas poussable ---

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // No-op : l'entité ne pousse personne pour ne pas trahir sa position à un non-traqué.
    }

    // --- Cycle de vie : pilotée par le controller, jamais sauvegardée ---

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    // --- GeckoLib : pilotage des animations du modèle Blockbench ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animateMain));
    }

    private PlayState animateMain(AnimationState<StalkerEntity> state) {
        // Mode leurre : pose figée imposée (silhouette / révélation), prioritaire sur tout le reste.
        int pose = this.entityData.get(POSE);
        if (pose >= 0 && pose < POSES.length) {
            state.setAnimation(POSES[pose]);
        } else if (this.isFlying()) {
            // Pas d'animation de vol dédiée dans le nouveau modèle : on réutilise la poursuite.
            state.setAnimation(CHASE);
        } else if (state.isMoving()) {
            state.setAnimation(WALK);
        } else {
            state.setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
