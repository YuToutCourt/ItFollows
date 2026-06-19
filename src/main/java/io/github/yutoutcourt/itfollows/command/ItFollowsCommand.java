package io.github.yutoutcourt.itfollows.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.curse.CurseManager;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import io.github.yutoutcourt.itfollows.tracking.HauntController;
import io.github.yutoutcourt.itfollows.tracking.HauntPhase;
import io.github.yutoutcourt.itfollows.tracking.StalkTrackerState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Commande debug {@code /itfollows fatigue ...} (op niveau 2) pour inspecter et forcer
 * la fatigue en jeu. Sert de principal outil de vérification manuelle.
 */
public final class ItFollowsCommand {

    /** Auto-complétion des noms de phase (minuscules) pour {@code /itfollows stalker phase <phase>}. */
    private static final SuggestionProvider<CommandSourceStack> PHASE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(HauntPhase.values()).map(p -> p.name().toLowerCase(Locale.ROOT)),
                    builder);

    /** Auto-complétion des ids d'actions de malédiction pour {@code /itfollows curse action ...}. */
    private static final SuggestionProvider<CommandSourceStack> CURSE_ACTION_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(CurseManager.actionIds(), builder);

    private ItFollowsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itfollows")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("fatigue")
                        .then(Commands.literal("get")
                                .executes(ctx -> getFatigue(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> getFatigue(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                                                .executes(ctx -> setFatigue(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        FloatArgumentType.getFloat(ctx, "value"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("delta", FloatArgumentType.floatArg())
                                                .executes(ctx -> addFatigue(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        FloatArgumentType.getFloat(ctx, "delta")))))))
                .then(Commands.literal("stalker")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> forceTarget(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> forceTarget(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("target")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> forceTarget(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("escalate")
                                .executes(ctx -> forceEscalation(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> forceEscalation(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("phase")
                                .then(Commands.argument("phase", StringArgumentType.word())
                                        .suggests(PHASE_SUGGESTIONS)
                                        .executes(ctx -> forcePhase(ctx, StringArgumentType.getString(ctx, "phase"), null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> forcePhase(ctx,
                                                        StringArgumentType.getString(ctx, "phase"),
                                                        EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("despawn")
                                .executes(ItFollowsCommand::despawnStalker))
                        .then(Commands.literal("status")
                                .executes(ItFollowsCommand::stalkerStatus)))
                .then(Commands.literal("curse")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> curseSet(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> curseRemove(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("action")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(CURSE_ACTION_SUGGESTIONS)
                                                .executes(ctx -> curseAction(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("status")
                                .executes(ctx -> curseStatus(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> curseStatus(ctx, EntityArgument.getPlayer(ctx, "player")))))));
    }

    // --- Phase 3 : malédiction ---

    private static int curseSet(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CurseManager.debugCurse(ctx.getSource().getServer(), player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString() + " est maintenant maudit(e)."), true);
        return 1;
    }

    private static int curseRemove(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CurseManager.debugRemove(ctx.getSource().getServer(), player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Malédiction levée pour " + player.getName().getString() + "."), true);
        return 1;
    }

    private static int curseAction(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id) {
        boolean ok = CurseManager.debugAssign(ctx.getSource().getServer(), player, id);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal(
                    "Échec : action « " + id + " » inconnue, ou aucune victime éligible (autre joueur requis). "
                            + "Voir les logs serveur pour le détail."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Action « " + id + " » assignée à " + player.getName().getString() + "."), true);
        return 1;
    }

    private static int curseStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String status = CurseManager.describeStatus(ctx.getSource().getServer(), player);
        ctx.getSource().sendSuccess(() -> Component.literal(status), false);
        return 1;
    }

    private static int getFatigue(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        float value = FatigueManager.getFatigue(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("Fatigue de %s : %.1f / 100", player.getName().getString(), value)), false);
        return (int) value;
    }

    private static int setFatigue(CommandContext<CommandSourceStack> ctx, ServerPlayer player, float value) {
        FatigueManager.setFatigue(player, value);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("Fatigue de %s réglée à %.1f", player.getName().getString(), value)), true);
        return (int) value;
    }

    private static int addFatigue(CommandContext<CommandSourceStack> ctx, ServerPlayer player, float delta) {
        FatigueManager.addFatigue(player, delta);
        float result = FatigueManager.getFatigue(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("Fatigue de %s : %.1f (%+.1f)", player.getName().getString(), result, delta)), true);
        return (int) result;
    }

    // --- Phase 2 : entité traqueuse ---

    private static int forceTarget(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MinecraftServer server = ctx.getSource().getServer();
        HauntController.forceTarget(server, player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Entité forcée sur " + player.getName().getString() + " (grâce ignorée)."), true);
        return 1;
    }

    private static int forceEscalation(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MinecraftServer server = ctx.getSource().getServer();
        HauntController.forceEscalation(server, player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Escalade démarrée sur " + player.getName().getString() + " (étape 1 : bruits lointains)."), true);
        return 1;
    }

    private static int forcePhase(CommandContext<CommandSourceStack> ctx, String phaseName, ServerPlayer player) {
        MinecraftServer server = ctx.getSource().getServer();
        HauntPhase phase;
        try {
            phase = HauntPhase.valueOf(phaseName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Phase inconnue : " + phaseName));
            return 0;
        }
        boolean ok = HauntController.forcePhase(server, player, phase);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal(
                    "Aucune cible : précise un joueur (/itfollows stalker phase " + phaseName + " <joueur>)."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Phase forcée : " + phase.name()), true);
        return 1;
    }

    private static int despawnStalker(CommandContext<CommandSourceStack> ctx) {
        HauntController.stop(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Entité retirée, traque arrêtée."), true);
        return 1;
    }

    private static int stalkerStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        StalkTrackerState state = StalkTrackerState.get(server);
        UUID targetId = state.getTargetPlayer();
        ServerPlayer target = targetId == null ? null : server.getPlayerList().getPlayer(targetId);
        String targetName = target != null
                ? target.getName().getString()
                : (targetId != null ? targetId + " (hors ligne)" : "aucune");
        StalkerEntity entity = HauntController.resolveEntity(server, state);
        long grace = HauntController.graceRemainingTicks(server, ItFollowsConfig.get());

        final String position;
        if (entity != null) {
            String dim = entity.level().dimension().location().toString();
            String dist = target != null && target.level() == entity.level()
                    ? String.format(" | dist cible : %.1f bloc(s)", Math.sqrt(entity.distanceToSqr(target)))
                    : "";
            position = String.format("présente @ %.1f %.1f %.1f (%s)%s",
                    entity.getX(), entity.getY(), entity.getZ(), dim, dist);
        } else if (state.getVirtualPos() != null) {
            Vec3 v = state.getVirtualPos();
            String dim = state.getVirtualDim() != null ? state.getVirtualDim().location().toString() : "?";
            String dist = target != null && state.getVirtualDim() != null
                    && target.level().dimension().equals(state.getVirtualDim())
                    ? String.format(" | dist cible : %.1f bloc(s)",
                        target.position().distanceTo(v))
                    : "";
            position = String.format("virtuelle @ %.1f %.1f %.1f (%s)%s",
                    v.x, v.y, v.z, dim, dist);
        } else {
            position = "absente";
        }

        String range = HauntController.describeRange(server, ItFollowsConfig.get());
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Traque — cible : %s | entité : %s | seuils : %s | phase : %s | forcée : %s | grâce restante : %ds",
                targetName,
                position,
                range,
                state.getHauntPhase().name(),
                HauntController.isForced() ? "oui" : "non",
                grace / 20)), false);
        return 1;
    }
}
