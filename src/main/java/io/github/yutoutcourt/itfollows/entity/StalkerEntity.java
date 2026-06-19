package io.github.yutoutcourt.itfollows.entity;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.ai.StalkerChaseGoal;
import io.github.yutoutcourt.itfollows.entity.ai.StalkerMoveControl;
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

import java.util.Objects;
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

    // --- Animations GeckoLib (assets/itfollows/animations/stalker.animation.json) ---
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.monstre.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.monstre.walk");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.monstre.hover");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.monstre.fly_chase");
    private static final RawAnimation DEPLOY = RawAnimation.begin().thenPlay("animation.monstre.wing_deploy");
    /** Vitesse horizontale² au-delà de laquelle le vol passe en poursuite (sinon vol stationnaire). */
    private static final double FLY_CHASE_SPEED_SQR = 0.02;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Synchronisé au client pour piloter l'animation de vol (ailes). */
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);

    /** Distance² (blocs²) sous laquelle l'entité frappe la cible. 4 = 2 blocs. */
    private static final double ATTACK_RANGE_SQR = 4.0;
    /** Délai (ticks) entre deux coups. */
    private static final int ATTACK_COOLDOWN = 20;

    private UUID targetUuid;
    private int attackCooldown;
    /** Anti-spam : ticks restants avant de pouvoir réarmer un saut. */
    private int jumpCooldown;

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
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                // FOLLOW_RANGE pilote maxVisitedNodes de la nav vanilla (≈ range×16). On le garde bas :
                // la traque à courte portée suffit, la longue distance est couverte par HauntController.
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
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
     * Active/désactive le mode vol. Au décollage : coupe la gravité et joue le déploiement des ailes
     * (synchronisé vers le client traqueur). À l'atterrissage : rétablit la gravité et purge l'élan
     * vertical résiduel pour ne pas « rebondir ». Idempotent.
     */
    public void setFlyingMode(boolean fly) {
        if (this.isFlying() == fly) {
            return;
        }
        this.entityData.set(FLYING, fly);
        this.setNoGravity(fly);
        if (fly) {
            this.triggerAnim("main", "deploy");
        } else {
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

        ServerPlayer target = resolveTarget();
        if (target != null) {
            double distSqr = this.distanceToSqr(target);
            if (attackCooldown == 0 && distSqr < ATTACK_RANGE_SQR) {
                this.doHurtTarget(target);
                attackCooldown = ATTACK_COOLDOWN;
            }
        }
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
        controllers.add(new AnimationController<>(this, "main", 5, this::animateMain)
                .triggerableAnim("deploy", DEPLOY));
    }

    private PlayState animateMain(AnimationState<StalkerEntity> state) {
        if (this.isFlying()) {
            boolean chasing = this.getDeltaMovement().horizontalDistanceSqr() > FLY_CHASE_SPEED_SQR;
            state.setAnimation(chasing ? FLY : HOVER);
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
