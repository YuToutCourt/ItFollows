package io.github.yutoutcourt.itfollows.curse;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.mixin.ItemEntityThrowerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Banque des actions de malédiction (Phase 3) + tirage pondéré anti-répétition.
 * Voir le cahier des charges et le plan : 27 actions « pas trop faciles », observables de l'extérieur.
 */
public final class CurseActionRegistry {

    private static final List<CurseAction> ALL = new ArrayList<>();
    private static final Map<String, CurseAction> BY_ID = new LinkedHashMap<>();

    // Items « rares » (Le quêteur) et nourritures (Le sommelier).
    private static final List<Item> RARES = List.of(
            Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT, Items.QUARTZ, Items.NETHER_STAR);
    private static final List<Item> FOODS = List.of(
            Items.BREAD, Items.COOKED_BEEF, Items.GOLDEN_APPLE, Items.COOKED_CHICKEN, Items.APPLE);

    // Blocs « signature » difficiles à obtenir (Le maçon) et sources de feu (Le pyromane).
    private static final Set<Block> DIFFICULT_BLOCKS = Set.of(
            Blocks.PLAYER_HEAD, Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.CREEPER_HEAD,
            Blocks.DRAGON_HEAD, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN, Blocks.BEEHIVE,
            Blocks.SOUL_LANTERN, Blocks.SOUL_TORCH);
    private static final Set<Block> FIRE_BLOCKS = Set.of(
            Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);

    static {
        register();
    }

    private CurseActionRegistry() {
    }

    public static List<CurseAction> all() {
        return ALL;
    }

    public static CurseAction byId(String id) {
        return BY_ID.get(id);
    }

    /** Tirage pondéré : exclut les candidates non pertinentes et les dernières utilisées. */
    public static CurseAction draw(ServerPlayer curser, ServerPlayer target, CurseState state,
                                   ItFollowsConfig config, RandomSource random) {
        List<CurseAction> pool = new ArrayList<>();
        for (CurseAction a : ALL) {
            if (a.isCandidate(curser, target, config) && !state.wasRecent(a.id(), config.curseRecentMemory)) {
                pool.add(a);
            }
        }
        if (pool.isEmpty()) {
            for (CurseAction a : ALL) {
                if (a.isCandidate(curser, target, config)) {
                    pool.add(a);
                }
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int total = pool.stream().mapToInt(CurseAction::weight).sum();
        int r = random.nextInt(Math.max(1, total));
        for (CurseAction a : pool) {
            r -= a.weight();
            if (r < 0) {
                return a;
            }
        }
        return pool.get(0);
    }

    private static void add(CurseAction a) {
        ALL.add(a);
        BY_ID.put(a.id(), a);
    }

    // === Helpers ============================================================

    private static int secs(int ticks) {
        return Math.max(1, ticks / 20);
    }

    private static String tn(ServerPlayer target) {
        return target.getName().getString();
    }

    private static boolean hasItem(ServerPlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCategory(ServerPlayer p, ItemCategory cat) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (cat.matches(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static Item firstMissingRare(ServerPlayer target) {
        for (Item item : RARES) {
            if (!hasItem(target, item)) {
                return item;
            }
        }
        return null;
    }

    private static ItemCategory firstMissingCategory(ServerPlayer target) {
        for (ItemCategory cat : ItemCategory.values()) {
            if (!hasCategory(target, cat)) {
                return cat;
            }
        }
        return null;
    }

    private static String label(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static boolean within(ServerPlayer a, ServerPlayer b, double range) {
        return CurseUtil.within(a, b, range);
    }

    /** UUID du joueur qui a jeté cet item ({@code null} si miné/spawné). Voir {@link ItemEntityThrowerAccessor}. */
    private static UUID throwerOf(ItemEntity ie) {
        return ((ItemEntityThrowerAccessor) (Object) ie).itfollows$getThrower();
    }

    /** Vrai si l'item a été jeté par le maudit (causalité des actions « objets au sol/lave »). */
    private static boolean thrownBy(ItemEntity ie, ServerPlayer curser) {
        return curser.getUUID().equals(throwerOf(ie));
    }

    /** Distance 3D du centre d'un bloc aux pieds de la cible. */
    private static double distToTarget(BlockPos pos, ServerPlayer target) {
        double dx = pos.getX() + 0.5 - target.getX();
        double dy = pos.getY() + 0.5 - target.getY();
        double dz = pos.getZ() + 0.5 - target.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // === La banque ==========================================================

    private static void register() {
        ALL.clear();
        BY_ID.clear();

        // --- A. Inventaire / objets (déduction) ---

        add(new CurseAction() {
            public String id() { return "quete"; }
            public String name() { return "Le quêteur"; }
            public boolean isCandidate(ServerPlayer c, ServerPlayer t, ItFollowsConfig cfg) {
                return firstMissingRare(t) != null;
            }
            public void onAssign(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                Item item = firstMissingRare(t);
                p.requiredItem = item != null ? item : Items.DIAMOND;
                p.requiredLabel = label(p.requiredItem);
            }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Fais ramasser à " + tn(t) + " un objet rare qu'il/elle n'a pas : " + p.requiredLabel + ".";
            }
            public boolean onVictimPickup(ServerPlayer c, ServerPlayer t, ItemStack s, CurseProgress p, ItFollowsConfig cfg) {
                return s.getItem() == p.requiredItem;
            }
        });

        add(new CurseAction() {
            public String id() { return "denuement"; }
            public String name() { return "Le dénuement"; }
            public boolean isCandidate(ServerPlayer c, ServerPlayer t, ItFollowsConfig cfg) {
                return firstMissingCategory(t) != null;
            }
            public void onAssign(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                ItemCategory cat = firstMissingCategory(t);
                p.requiredCategory = cat != null ? cat : ItemCategory.NOURRITURE;
                p.requiredLabel = p.requiredCategory.label();
            }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Donne à " + tn(t) + " " + p.requiredLabel + " (il/elle n'en a pas du tout).";
            }
            public boolean onVictimPickup(ServerPlayer c, ServerPlayer t, ItemStack s, CurseProgress p, ItFollowsConfig cfg) {
                return p.requiredCategory != null && p.requiredCategory.matches(s);
            }
        });

        add(new CurseAction() {
            public String id() { return "diamantaire"; }
            public String name() { return "Le diamantaire"; }
            public void onAssign(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                p.requiredItem = Items.DIAMOND;
                p.requiredLabel = label(Items.DIAMOND);
            }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Fais ramasser un " + p.requiredLabel + " à " + tn(t) + ".";
            }
            public boolean onVictimPickup(ServerPlayer c, ServerPlayer t, ItemStack s, CurseProgress p, ItFollowsConfig cfg) {
                return s.getItem() == Items.DIAMOND;
            }
        });

        add(new CurseAction() {
            public String id() { return "sommelier"; }
            public String name() { return "Le sommelier"; }
            public void onAssign(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                Item food = FOODS.get(c.getRandom().nextInt(FOODS.size()));
                p.requiredItem = food;
                p.requiredLabel = label(food);
            }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Donne « " + p.requiredLabel + " » à " + tn(t) + " et attends qu'il/elle le mange.";
            }
            public boolean onVictimPickup(ServerPlayer c, ServerPlayer t, ItemStack s, CurseProgress p, ItFollowsConfig cfg) {
                // Ramassage déjà filtré « jeté par le maudit » : on mémorise que la victime a reçu l'aliment.
                if (s.getItem() == p.requiredItem) {
                    p.victimReceivedRequired = true;
                }
                return false; // la complétion, c'est le repas, pas le ramassage
            }
            public boolean onVictimConsume(ServerPlayer c, ServerPlayer t, ItemStack s, CurseProgress p, ItFollowsConfig cfg) {
                // Il faut qu'elle mange bien l'aliment QUE LE MAUDIT lui a donné.
                return p.victimReceivedRequired && s.getItem() == p.requiredItem;
            }
        });

        add(new CurseAction() {
            public String id() { return "sacrifice"; }
            public String name() { return "Le sacrifice"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Détruis " + cfg.curseSacrificeCount + " objets dans la lave (ou le vide) sous les yeux de " + tn(t) + ".";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                if (!within(c, t, cfg.curseProximityRange + 6)) {
                    return false;
                }
                ServerLevel lvl = c.serverLevel();
                AABB box = t.getBoundingBox().inflate(cfg.curseProximityRange + 6);
                for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, box)) {
                    long key = ie.getId();
                    if (p.marks.contains(key) || !thrownBy(ie, c)) {
                        continue; // seuls les objets JETÉS PAR LE MAUDIT comptent
                    }
                    if (ie.isInLava() || ie.getY() < lvl.getMinBuildHeight() + 1) {
                        p.marks.add(key);
                        p.count++;
                    }
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseSacrificeCount);
                return p.count >= cfg.curseSacrificeCount;
            }
        });

        add(new CurseAction() {
            public String id() { return "autel"; }
            public String name() { return "L'autel"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Dépose " + cfg.curseAltarCount + " objets au sol aux pieds de " + tn(t) + ".";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                ServerLevel lvl = t.serverLevel();
                AABB box = t.getBoundingBox().inflate(3.0);
                int onGround = 0;
                for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, box)) {
                    if (ie.onGround() && thrownBy(ie, c)) { // déposés par le maudit uniquement
                        onGround++;
                    }
                }
                p.count = onGround;
                p.fraction = Math.min(1.0f, (float) onGround / cfg.curseAltarCount);
                return onGround >= cfg.curseAltarCount;
            }
        });

        // --- B. Gestuelle / proximité (accumulation) ---

        add(new AccumulationAction("regard", "Le regard", AccumulationAction.Mode.PARTIAL,
                cfg -> cfg.curseStareTicks,
                (c, t, cfg) -> within(c, t, cfg.curseProximityRange) && CurseUtil.looksAt(c, t, cfg.curseLookConeDegrees),
                (c, t, p, cfg) -> "Fixe " + tn(t) + " pendant " + secs(cfg.curseStareTicks) + " s sans le/la lâcher des yeux.",
                null));

        add(new AccumulationAction("ombre", "L'ombre", AccumulationAction.Mode.GRACE,
                cfg -> cfg.curseFollowTicks,
                (c, t, cfg) -> within(c, t, 4.0),
                (c, t, p, cfg) -> "Suis " + tn(t) + " de près (≤ 4 blocs) pendant " + secs(cfg.curseFollowTicks) + " s.",
                null));

        add(new AccumulationAction("plante", "Le planté", AccumulationAction.Mode.GRACE,
                cfg -> cfg.cursePlanteTicks,
                (c, t, cfg) -> c.level() == t.level()
                        && c.distanceToSqr(t) >= 8 * 8 && c.distanceToSqr(t) <= 16 * 16
                        && c.getDeltaMovement().horizontalDistanceSqr() < 0.0025
                        && CurseUtil.looksAt(c, t, cfg.curseLookConeDegrees),
                (c, t, p, cfg) -> "Reste parfaitement immobile en fixant " + tn(t) + " (8-16 blocs) pendant " + secs(cfg.cursePlanteTicks) + " s.",
                null));

        add(new AccumulationAction("mime", "Le mime", AccumulationAction.Mode.PARTIAL,
                cfg -> cfg.curseMimicTicks,
                (c, t, cfg) -> within(c, t, cfg.curseProximityRange) && c.isShiftKeyDown() == t.isShiftKeyDown(),
                (c, t, p, cfg) -> "Imite la posture de " + tn(t) + " (accroupi/debout en miroir) pendant " + secs(cfg.curseMimicTicks) + " s.",
                null));

        add(new AccumulationAction("imitateur", "L'imitateur", AccumulationAction.Mode.GRACE,
                cfg -> cfg.curseMimicTicks,
                (c, t, cfg) -> within(c, t, cfg.curseProximityRange)
                        && (c.getDeltaMovement().horizontalDistanceSqr() > 0.0025)
                            == (t.getDeltaMovement().horizontalDistanceSqr() > 0.0025),
                (c, t, p, cfg) -> "Copie les déplacements de " + tn(t) + " (avance/arrête-toi en même temps) pendant " + secs(cfg.curseMimicTicks) + " s.",
                null));

        // Sneak rituel (compteur de bascules dans son champ de vision)
        add(new CurseAction() {
            public String id() { return "sneak"; }
            public String name() { return "Le sneak rituel"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Accroupis-toi " + cfg.curseSneakCount + " fois à moins de 3 blocs de " + tn(t) + ", dans son champ de vision.";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                boolean sneak = c.isShiftKeyDown();
                if (sneak && !p.prevBool && within(c, t, 3.0) && CurseUtil.inViewOf(t, c, 50.0)) {
                    p.count++;
                }
                p.prevBool = sneak;
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseSneakCount);
                return p.count >= cfg.curseSneakCount;
            }
        });

        // Yo-yo (cycles accroupi/relevé rapides, proches)
        add(new CurseAction() {
            public String id() { return "yoyo"; }
            public String name() { return "Le yo-yo"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Accroupis-toi et relève-toi rapidement " + cfg.curseYoyoCount + " fois près de " + tn(t) + ".";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                boolean sneak = c.isShiftKeyDown();
                if (sneak && !p.prevBool && within(c, t, 4.0)) {
                    p.count++;
                }
                p.prevBool = sneak;
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseYoyoCount);
                return p.count >= cfg.curseYoyoCount;
            }
        });

        // Métronome (sauts près de la cible)
        add(new CurseAction() {
            public String id() { return "metronome"; }
            public String name() { return "Le métronome"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Saute " + cfg.curseJumpCount + " fois collé(e) à " + tn(t) + ".";
            }
            public float indicator(CurseProgress p) { return p.fraction; }
            public boolean onCurserJump(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                if (within(c, t, 2.5)) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseJumpCount);
                return p.count >= cfg.curseJumpCount;
            }
        });

        // --- C. Contact / physique ---

        add(new CurseAction() {
            public String id() { return "bapteme"; }
            public String name() { return "Le baptême"; }
            public void onAssign(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                // Mémorise l'état initial : si la cible est DÉJÀ dans l'eau, on n'auto-valide pas ;
                // il faudra une vraie nouvelle entrée dans l'eau.
                p.prevBool = t.isInWater();
            }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Pousse " + tn(t) + " dans l'eau.";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                boolean inWater = t.isInWater();
                boolean justEntered = inWater && !p.prevBool; // transition sec → eau, pas « déjà dedans »
                p.prevBool = inWater;
                // Complète seulement si le maudit est collé à la cible À L'INSTANT de l'entrée :
                // signe qu'il l'a poussée, et non qu'elle est entrée seule.
                return justEntered && within(c, t, 1.8);
            }
        });

        add(new CurseAction() {
            public String id() { return "chute"; }
            public String name() { return "La chute"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Fais chuter " + tn(t) + " d'une petite hauteur (sans le/la tuer).";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                long now = c.serverLevel().getGameTime();
                if (within(c, t, 4.0)) {
                    p.lastNearTime = now;
                }
                boolean landed = t.onGround() && p.aux >= 3.0f;
                boolean complete = landed && (now - p.lastNearTime) <= 60;
                if (!t.onGround()) {
                    p.aux = t.fallDistance;
                }
                if (complete) {
                    p.aux = 0.0f;
                }
                return complete;
            }
        });

        // --- D. Environnement (placements difficiles) ---

        add(new CurseAction() {
            public String id() { return "macon"; }
            public String name() { return "Le maçon"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Pose un bloc rare (tête, citrouille sculptée, ruche, lanterne d'âmes…) juste à côté de " + tn(t) + ".";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                return DIFFICULT_BLOCKS.contains(st.getBlock()) && distToTarget(pos, t) <= 2.0;
            }
        });

        add(new CurseAction() {
            public String id() { return "fossoyeur"; }
            public String name() { return "Le fossoyeur"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Creuse un trou de " + cfg.curseGraveDepth + " blocs aux pieds de " + tn(t) + ".";
            }
            public boolean onCurserBreakBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                if (distToTarget(pos, t) <= 2.0 && pos.getY() <= t.getBlockY() && p.marks.add(pos.asLong())) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseGraveDepth);
                return p.count >= cfg.curseGraveDepth;
            }
        });

        add(new CurseAction() {
            public String id() { return "tunnelier"; }
            public String name() { return "Le tunnelier"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Creuse un tunnel descendant d'au moins " + cfg.curseTunnelLength + " blocs depuis " + tn(t) + ".";
            }
            public boolean onCurserBreakBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                if (distToTarget(pos, t) <= 4.0 && pos.getY() < t.getBlockY() + 1 && p.marks.add(pos.asLong())) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseTunnelLength);
                return p.count >= cfg.curseTunnelLength;
            }
        });

        add(new CurseAction() {
            public String id() { return "geolier"; }
            public String name() { return "Le geôlier"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Encage " + tn(t) + " : bouche " + cfg.curseCageSides + " côtés autour d'elle/lui.";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                int dx = pos.getX() - t.getBlockX();
                int dz = pos.getZ() - t.getBlockZ();
                int dy = pos.getY() - t.getBlockY();
                boolean adjacentSide = (dy == 0 || dy == 1) && Math.abs(dx) + Math.abs(dz) == 1;
                if (adjacentSide) {
                    long side = (dx & 0xFFL) << 8 | (dz & 0xFFL); // identifiant de côté (indépendant de la hauteur)
                    if (p.marks.add(side)) {
                        p.count++;
                    }
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseCageSides);
                return p.count >= cfg.curseCageSides;
            }
        });

        add(new CurseAction() {
            public String id() { return "batisseur"; }
            public String name() { return "Le bâtisseur"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Érige un pilier de " + cfg.curseTowerHeight + " blocs collé à " + tn(t) + ".";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                int dx = pos.getX() - t.getBlockX();
                int dz = pos.getZ() - t.getBlockZ();
                boolean adjacentColumn = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (Math.abs(dx) + Math.abs(dz) >= 1);
                if (adjacentColumn && p.marks.add((long) pos.getY())) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseTowerHeight);
                return p.count >= cfg.curseTowerHeight;
            }
        });

        add(new CurseAction() {
            public String id() { return "pyromane"; }
            public String name() { return "Le pyromane"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Allume " + cfg.curseFireCount + " feux ou feux de camp près de " + tn(t) + ".";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                if (FIRE_BLOCKS.contains(st.getBlock()) && distToTarget(pos, t) <= 5.0 && p.marks.add(pos.asLong())) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseFireCount);
                return p.count >= cfg.curseFireCount;
            }
        });

        add(new CurseAction() {
            public String id() { return "encerclement"; }
            public String name() { return "L'encerclement"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Pose " + cfg.curseRingCount + " blocs en cercle tout autour de " + tn(t) + ".";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                double d = distToTarget(pos, t);
                if (d >= 1.2 && d <= 3.0 && p.marks.add(pos.asLong())) {
                    p.count++;
                }
                p.fraction = Math.min(1.0f, (float) p.count / cfg.curseRingCount);
                return p.count >= cfg.curseRingCount;
            }
        });

        add(new CurseAction() {
            public String id() { return "facteur"; }
            public String name() { return "Le facteur"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Pose un panneau à moins de 4 blocs de " + tn(t) + ".";
            }
            public boolean onCurserPlaceBlock(ServerPlayer c, ServerPlayer t, BlockPos pos, BlockState st, CurseProgress p, ItFollowsConfig cfg) {
                Block b = st.getBlock();
                return (b instanceof SignBlock || b instanceof WallSignBlock) && distToTarget(pos, t) <= 4.0;
            }
        });

        // --- E. Monde / lieux ---

        add(new AccumulationAction("veilleur", "Le veilleur nocturne", AccumulationAction.Mode.GRACE,
                cfg -> cfg.curseDarkTicks,
                (c, t, cfg) -> within(c, t, cfg.curseProximityRange)
                        && c.serverLevel().getMaxLocalRawBrightness(c.blockPosition()) == 0,
                (c, t, p, cfg) -> "Tiens-toi dans le noir complet à moins de 6 blocs de " + tn(t) + " pendant " + secs(cfg.curseDarkTicks) + " s.",
                null));

        add(new CurseAction() {
            public String id() { return "pelerin"; }
            public String name() { return "Le pèlerin"; }
            public String describe(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                return "Emmène " + tn(t) + " dans le Nether ou l'End (soyez-y tous les deux).";
            }
            public boolean onTick(ServerPlayer c, ServerPlayer t, CurseProgress p, ItFollowsConfig cfg) {
                ServerLevel lvl = c.serverLevel();
                boolean otherworld = lvl.dimension() == net.minecraft.world.level.Level.NETHER
                        || lvl.dimension() == net.minecraft.world.level.Level.END;
                return otherworld && t.level() == lvl;
            }
        });

        add(new AccumulationAction("veilleurdesommeil", "Le veilleur de sommeil", AccumulationAction.Mode.GRACE,
                cfg -> cfg.curseSleepWatchTicks,
                (c, t, cfg) -> within(c, t, cfg.curseProximityRange) && t.isSleeping()
                        && CurseUtil.looksAt(c, t, cfg.curseLookConeDegrees * 2),
                (c, t, p, cfg) -> "Regarde " + tn(t) + " dormir pendant " + secs(cfg.curseSleepWatchTicks) + " s.",
                null));
    }
}
