package io.github.yutoutcourt.itfollows.curse;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.tracking.HauntController;
import io.github.yutoutcourt.itfollows.tracking.HauntPhase;
import io.github.yutoutcourt.itfollows.tracking.StalkTrackerState;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pilote la Phase 3 côté serveur : marque le maudit (= cible de l'entité en HUNTING), livre son
 * objectif <b>10 min après le début de la chasse</b>, détecte l'accomplissement de l'action, puis
 * transfère la malédiction et re-cible l'entité sur la victime.
 *
 * <p>État d'avancement transitoire (jauge) gardé en mémoire ; la vérité persistante (qui est
 * maudit, action assignée, victime désignée, anti-ping-pong) vit dans {@link CurseState}.
 */
public final class CurseManager {

    private static int sampleCounter;
    /** Progression transitoire de l'action en cours, par maudit. */
    private static final Map<UUID, CurseProgress> progressByPlayer = new HashMap<>();

    private CurseManager() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            resyncOnJoin(server, handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                progressByPlayer.remove(handler.player.getUUID()));

        // Casse de bloc par le maudit (fossoyeur, tunnelier).
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer sp) {
                onCurserBreakBlock(sp, pos, state);
            }
        });
    }

    // === Boucle serveur =====================================================

    public static void tick(MinecraftServer server) {
        ItFollowsConfig config = ItFollowsConfig.get();
        if (!config.curseEnabled) {
            return;
        }

        StalkTrackerState tracker = StalkTrackerState.get(server);
        CurseState state = CurseState.get(server);
        long now = server.overworld().getGameTime();
        HauntPhase phase = tracker.getHauntPhase();
        UUID targetId = tracker.getTargetPlayer();

        // Le maudit, c'est la cible de l'entité une fois la chasse active.
        if (phase == HauntPhase.HUNTING && targetId != null && !state.isCursed(targetId)) {
            state.setCurse(targetId, null, now);
            ServerPlayer tp = server.getPlayerList().getPlayer(targetId);
            if (tp != null) {
                ItFollowsNetworking.sendCurse(tp, true);
            }
        }

        if (++sampleCounter < config.curseSampleIntervalTicks) {
            return;
        }
        sampleCounter = 0;

        for (UUID id : state.cursedPlayers()) {
            ServerPlayer curser = server.getPlayerList().getPlayer(id);
            CurseData data = state.getCurse(id);
            if (curser == null || data == null) {
                continue;
            }

            if (!data.objectiveSent) {
                // Livraison 10 min après le début de HUNTING, pour le maudit traqué.
                boolean isHuntedTarget = id.equals(targetId) && phase == HauntPhase.HUNTING;
                if (isHuntedTarget && now - tracker.getPhaseStartTime() >= config.curseObjectiveDelayTicks) {
                    assignObjective(server, state, curser, data, config, now);
                }
            } else {
                runActiveAction(server, state, curser, data, config, now);
            }
        }
    }

    private static void assignObjective(MinecraftServer server, CurseState state, ServerPlayer curser,
                                        CurseData data, ItFollowsConfig config, long now) {
        ServerPlayer victim = pickVictim(server, curser, data, config, now);
        if (victim == null) {
            return; // aucun autre joueur disponible : on réessaiera au prochain échantillon
        }
        CurseAction action = CurseActionRegistry.draw(curser, victim, state, config, curser.getRandom());
        if (action == null) {
            return;
        }
        CurseProgress p = new CurseProgress();
        action.onAssign(curser, victim, p, config);
        progressByPlayer.put(curser.getUUID(), p);

        data.activeActionId = action.id();
        data.designatedVictim = victim.getUUID();
        data.objectiveSent = true;
        state.markDirty();

        ItFollowsNetworking.sendCurse(curser, true);
        ItFollowsNetworking.sendCurseObjective(curser, action.describe(curser, victim, p, config));
    }

    private static void runActiveAction(MinecraftServer server, CurseState state, ServerPlayer curser,
                                        CurseData data, ItFollowsConfig config, long now) {
        CurseAction action = CurseActionRegistry.byId(data.activeActionId);
        if (action == null) {
            return;
        }
        ServerPlayer victim = data.designatedVictim == null
                ? null : server.getPlayerList().getPlayer(data.designatedVictim);
        CurseProgress p = restoreProgress(curser, victim, data, action, config);
        if (victim == null) {
            return; // victime hors ligne : on met l'action en pause
        }

        boolean done = action.onTick(curser, victim, p, config);
        pushIndicator(curser, victim, action, p);
        if (done) {
            transfer(server, state, curser, victim, data, config, now);
        }
    }

    // === Hooks d'événements (appelés par les mixins / events) ===============

    public static boolean curseEnabled() {
        return ItFollowsConfig.get().curseEnabled;
    }

    public static void onCurserBreakBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        dispatchCurser(player, (action, target, p, server, cfg) ->
                action.onCurserBreakBlock(player, target, pos, state, p, cfg));
    }

    public static void onCurserPlaceBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        dispatchCurser(player, (action, target, p, server, cfg) ->
                action.onCurserPlaceBlock(player, target, pos, state, p, cfg));
    }

    public static void onCurserJump(ServerPlayer player) {
        dispatchCurser(player, (action, target, p, server, cfg) ->
                action.onCurserJump(player, target, p, cfg));
    }

    public static void onVictimPickup(ServerPlayer player, ItemStack stack, UUID thrower) {
        dispatchVictim(player, (action, curser, p, server, cfg) -> {
            // Causalité : l'objet doit avoir été jeté PAR LE MAUDIT (un item miné/ramassé seul a
            // un thrower null ou un autre joueur). Sinon la victime validerait l'action toute seule.
            if (thrower == null || !thrower.equals(curser.getUUID())) {
                return false;
            }
            return action.onVictimPickup(curser, player, stack, p, cfg);
        });
    }

    public static void onVictimConsume(ServerPlayer player, ItemStack stack) {
        dispatchVictim(player, (action, curser, p, server, cfg) ->
                action.onVictimConsume(curser, player, stack, p, cfg));
    }

    public static void onCurserSign(ServerPlayer player, BlockPos pos, String text) {
        dispatchCurser(player, (action, target, p, server, cfg) ->
                action.onCurserSign(player, target, pos, text, p, cfg));
    }

    public static void onCurserItemBurned(ServerPlayer thrower, net.minecraft.world.entity.item.ItemEntity item) {
        dispatchCurser(thrower, (action, target, p, server, cfg) ->
                action.onCurserItemBurned(thrower, target, item, p, cfg));
    }

    // === Routage commun =====================================================

    private interface CurserHook {
        boolean run(CurseAction action, ServerPlayer target, CurseProgress p, MinecraftServer server, ItFollowsConfig cfg);
    }

    private interface VictimHook {
        boolean run(CurseAction action, ServerPlayer curser, CurseProgress p, MinecraftServer server, ItFollowsConfig cfg);
    }

    /** Dispatch un événement déclenché <b>par le maudit</b> (casse/pose/saut). */
    private static void dispatchCurser(ServerPlayer player, CurserHook hook) {
        MinecraftServer server = player.getServer();
        if (server == null || !curseEnabled()) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        CurseState state = CurseState.get(server);
        CurseData data = state.getCurse(player.getUUID());
        if (data == null || !data.objectiveSent || data.activeActionId == null) {
            return;
        }
        CurseAction action = CurseActionRegistry.byId(data.activeActionId);
        ServerPlayer victim = data.designatedVictim == null
                ? null : server.getPlayerList().getPlayer(data.designatedVictim);
        if (action == null || victim == null) {
            return;
        }
        CurseProgress p = restoreProgress(player, victim, data, action, config);
        boolean done = hook.run(action, victim, p, server, config);
        pushIndicator(player, victim, action, p);
        if (done) {
            transfer(server, state, player, victim, data, config, server.overworld().getGameTime());
        }
    }

    /** Dispatch un événement déclenché <b>par la victime</b> (ramassage/consommation). */
    private static void dispatchVictim(ServerPlayer victim, VictimHook hook) {
        MinecraftServer server = victim.getServer();
        if (server == null || !curseEnabled()) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        CurseState state = CurseState.get(server);
        for (UUID id : state.cursedPlayers()) {
            CurseData data = state.getCurse(id);
            if (data == null || !data.objectiveSent || !victim.getUUID().equals(data.designatedVictim)) {
                continue;
            }
            ServerPlayer curser = server.getPlayerList().getPlayer(id);
            CurseAction action = data.activeActionId == null ? null : CurseActionRegistry.byId(data.activeActionId);
            if (curser == null || action == null) {
                continue;
            }
            CurseProgress p = restoreProgress(curser, victim, data, action, config);
            if (hook.run(action, curser, p, server, config)) {
                transfer(server, state, curser, victim, data, config, server.overworld().getGameTime());
            }
            return;
        }
    }

    // === Transfert ==========================================================

    private static void transfer(MinecraftServer server, CurseState state, ServerPlayer curser,
                                 ServerPlayer victim, CurseData data, ItFollowsConfig config, long now) {
        state.pushRecent(data.activeActionId, config.curseRecentMemory);
        state.removeCurse(curser.getUUID());
        progressByPlayer.remove(curser.getUUID());

        CurseData vd = state.setCurse(victim.getUUID(), curser.getUUID(), now);
        vd.antiPingPongTarget = curser.getUUID();
        vd.antiPingPongUntil = now + config.curseAntiPingPongTicks;
        state.markDirty();

        // Retour de réussite : la jauge atteint 100 %, puis la bannière/son « action accomplie ».
        ItFollowsNetworking.sendCurseProgress(curser, victim.getUUID(), 100);
        ItFollowsNetworking.sendCurseResult(curser, victim.getName().getString());

        // Ancien maudit libéré.
        ItFollowsNetworking.sendCurse(curser, false);
        ItFollowsNetworking.sendCurseObjective(curser, "");

        // Nouvelle cible maudite + entité re-ciblée. Son objectif viendra 10 min après son HUNTING.
        ItFollowsNetworking.sendCurse(victim, true);
        HauntController.transferTarget(server, victim);
    }

    // === Helpers ============================================================

    private static CurseProgress restoreProgress(ServerPlayer curser, ServerPlayer victim, CurseData data,
                                                 CurseAction action, ItFollowsConfig config) {
        CurseProgress p = progressByPlayer.get(curser.getUUID());
        if (p == null) {
            // Après un redémarrage, l'objectif persiste mais la jauge non : on recrée et on rejoue
            // l'assignation (pour restaurer l'item/catégorie déduits).
            p = new CurseProgress();
            if (victim != null) {
                action.onAssign(curser, victim, p, config);
            }
            progressByPlayer.put(curser.getUUID(), p);
        }
        return p;
    }

    private static void pushIndicator(ServerPlayer curser, ServerPlayer victim, CurseAction action, CurseProgress p) {
        float ind = action.indicator(p);
        if (ind >= 0.0f && victim != null) {
            ItFollowsNetworking.sendCurseProgress(curser, victim.getUUID(), Math.round(ind * 100));
        }
    }

    private static ServerPlayer pickVictim(MinecraftServer server, ServerPlayer curser, CurseData data,
                                           ItFollowsConfig config, long now) {
        List<ServerPlayer> candidates = new ArrayList<>();
        boolean ppActive = data.antiPingPongTarget != null && now < data.antiPingPongUntil;
        CurseState state = CurseState.get(server);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == curser || other.isCreative() || other.isSpectator()) {
                continue;
            }
            if (state.isCursed(other.getUUID())) {
                continue;
            }
            if (ppActive && other.getUUID().equals(data.antiPingPongTarget)) {
                continue;
            }
            candidates.add(other);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(curser.getRandom().nextInt(candidates.size()));
    }

    /**
     * Sélection de victime pour les commandes debug : journalise pourquoi chaque joueur en ligne est
     * retenu ou exclu, et reste <b>permissive</b> (créatif/spectateur autorisés, contrairement au jeu
     * normal) pour ne pas bloquer les tests. Le maudisseur lui-même n'est jamais victime. Les joueurs
     * déjà maudits ou sous anti-ping-pong ne sont pas des victimes <i>idéales</i> mais servent de
     * <b>dernier recours</b> (ex. serveur à 2 joueurs où l'autre est déjà maudit) — jamais d'auto-victime.
     */
    private static ServerPlayer pickVictimDebug(MinecraftServer server, ServerPlayer curser, CurseData data,
                                                ItFollowsConfig config, long now) {
        List<ServerPlayer> candidates = new ArrayList<>();
        List<ServerPlayer> fallback = new ArrayList<>(); // autres joueurs non idéaux (déjà maudits / anti-ping-pong)
        boolean ppActive = data.antiPingPongTarget != null && now < data.antiPingPongUntil;
        CurseState state = CurseState.get(server);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            String name = other.getName().getString();
            if (other == curser) {
                Itfollows.LOGGER.info("[Curse][pickVictim] {} exclu : c'est le maudisseur lui-même.", name);
                continue;
            }
            if (state.isCursed(other.getUUID())) {
                Itfollows.LOGGER.info("[Curse][pickVictim] {} non idéal : déjà maudit → gardé en dernier recours.", name);
                fallback.add(other);
                continue;
            }
            if (ppActive && other.getUUID().equals(data.antiPingPongTarget)) {
                Itfollows.LOGGER.info("[Curse][pickVictim] {} non idéal : anti-ping-pong actif (encore {} ticks) "
                        + "→ gardé en dernier recours.", name, data.antiPingPongUntil - now);
                fallback.add(other);
                continue;
            }
            if (other.isCreative() || other.isSpectator()) {
                // En jeu normal ces joueurs seraient exclus ; en debug on les garde (tests).
                Itfollows.LOGGER.info("[Curse][pickVictim] {} retenu malgré mode créatif/spectateur (debug).", name);
            } else {
                Itfollows.LOGGER.info("[Curse][pickVictim] {} retenu comme victime potentielle.", name);
            }
            candidates.add(other);
        }
        if (!candidates.isEmpty()) {
            return candidates.get(curser.getRandom().nextInt(candidates.size()));
        }
        if (!fallback.isEmpty()) {
            ServerPlayer v = fallback.get(curser.getRandom().nextInt(fallback.size()));
            Itfollows.LOGGER.warn("[Curse][pickVictim] Aucune victime idéale → dernier recours sur {} "
                    + "(déjà maudit(e) ou anti-ping-pong).", v.getName().getString());
            return v;
        }
        Itfollows.LOGGER.warn("[Curse][pickVictim] Aucun autre joueur que le maudisseur parmi les {} en ligne.",
                server.getPlayerList().getPlayers().size());
        return null;
    }

    private static void resyncOnJoin(MinecraftServer server, ServerPlayer player) {
        CurseState state = CurseState.get(server);
        CurseData data = state.getCurse(player.getUUID());
        boolean cursed = data != null;
        ItFollowsNetworking.sendCurse(player, cursed);
        if (cursed && data.objectiveSent && data.activeActionId != null) {
            CurseAction action = CurseActionRegistry.byId(data.activeActionId);
            ServerPlayer victim = data.designatedVictim == null
                    ? null : server.getPlayerList().getPlayer(data.designatedVictim);
            if (action != null && victim != null) {
                CurseProgress p = restoreProgress(player, victim, data, action, ItFollowsConfig.get());
                ItFollowsNetworking.sendCurseObjective(player, action.describe(player, victim, p, ItFollowsConfig.get()));
            }
        }
    }

    // === API debug (commande) ==============================================

    /** Maudit un joueur tout de suite (source = null), sans attendre la chasse. */
    public static void debugCurse(MinecraftServer server, ServerPlayer player) {
        CurseState state = CurseState.get(server);
        long now = server.overworld().getGameTime();
        state.setCurse(player.getUUID(), null, now);
        ItFollowsNetworking.sendCurse(player, true);
    }

    /** Lève la malédiction d'un joueur (debug). */
    public static void debugRemove(MinecraftServer server, ServerPlayer player) {
        CurseState.get(server).removeCurse(player.getUUID());
        progressByPlayer.remove(player.getUUID());
        ItFollowsNetworking.sendCurse(player, false);
        ItFollowsNetworking.sendCurseObjective(player, "");
    }

    /**
     * Force l'assignation d'une action précise (debug détection). Journalise chaque étape et la
     * raison d'un éventuel échec. Pour permettre les tests en solo, si aucun autre joueur n'est
     * disponible comme victime, on retombe sur le joueur lui-même (auto-victime).
     */
    public static boolean debugAssign(MinecraftServer server, ServerPlayer player, String actionId) {
        String who = player.getName().getString();
        CurseAction action = CurseActionRegistry.byId(actionId);
        if (action == null) {
            Itfollows.LOGGER.warn("[Curse][debugAssign] Échec : action « {} » inconnue. Ids valides : {}",
                    actionId, actionIds());
            return false;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        CurseState state = CurseState.get(server);
        long now = server.overworld().getGameTime();
        CurseData data = state.getCurse(player.getUUID());
        if (data == null) {
            data = state.setCurse(player.getUUID(), null, now);
            Itfollows.LOGGER.info("[Curse][debugAssign] {} n'était pas maudit : malédiction posée (source=null).", who);
        }

        ServerPlayer victim = pickVictimDebug(server, player, data, config, now);
        if (victim == null) {
            Itfollows.LOGGER.warn(
                    "[Curse][debugAssign] Échec : aucune victime éligible (autre joueur) pour {}. "
                            + "Il faut un autre joueur en ligne, non déjà maudit. Joueurs en ligne : {}. "
                            + "Voir les lignes [Curse][pickVictim] ci-dessus pour le détail des exclusions.",
                    who, server.getPlayerList().getPlayers().size());
            return false;
        }
        Itfollows.LOGGER.info("[Curse][debugAssign] Victime désignée pour {} : {}.",
                who, victim.getName().getString());

        if (!action.isCandidate(player, victim, config)) {
            Itfollows.LOGGER.warn(
                    "[Curse][debugAssign] L'action « {} » n'est pas candidate pour {} → {} "
                            + "(condition isCandidate non remplie : inventaire/contexte). Assignée quand même (debug).",
                    actionId, who, victim.getName().getString());
        }

        CurseProgress p = new CurseProgress();
        action.onAssign(player, victim, p, config);
        progressByPlayer.put(player.getUUID(), p);
        data.activeActionId = action.id();
        data.designatedVictim = victim.getUUID();
        data.objectiveSent = true;
        state.markDirty();
        ItFollowsNetworking.sendCurse(player, true);
        String objective = action.describe(player, victim, p, config);
        ItFollowsNetworking.sendCurseObjective(player, objective);
        Itfollows.LOGGER.info("[Curse][debugAssign] OK : « {} » assignée à {} (victime {}). Objectif : {}",
                actionId, who, victim.getName().getString(), objective);
        return true;
    }

    /** Renvoie une description du statut courant de malédiction (debug). */
    public static String describeStatus(MinecraftServer server, ServerPlayer player) {
        CurseState state = CurseState.get(server);
        CurseData data = state.getCurse(player.getUUID());
        if (data == null) {
            return player.getName().getString() + " : non maudit.";
        }
        CurseProgress p = progressByPlayer.get(player.getUUID());
        String action = data.activeActionId == null ? "aucune (objectif non livré)" : data.activeActionId;
        String victim = "—";
        if (data.designatedVictim != null) {
            ServerPlayer v = server.getPlayerList().getPlayer(data.designatedVictim);
            victim = v != null ? v.getName().getString() : data.designatedVictim.toString();
        }
        int pct = p != null && p.fraction >= 0 ? Math.round(p.fraction * 100) : 0;
        return String.format("%s : MAUDIT | action : %s | victime : %s | avancement : %d%%",
                player.getName().getString(), action, victim, pct);
    }

    /** Ids de toutes les actions (pour l'auto-complétion de la commande). */
    public static List<String> actionIds() {
        List<String> ids = new ArrayList<>();
        CurseActionRegistry.all().forEach(a -> ids.add(a.id()));
        return Collections.unmodifiableList(ids);
    }
}
