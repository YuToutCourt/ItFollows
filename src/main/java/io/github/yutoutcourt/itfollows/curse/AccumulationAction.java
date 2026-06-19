package io.github.yutoutcourt.itfollows.curse;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.ToIntFunction;

/**
 * Base des actions à <b>accumulation</b> : la progression monte tant qu'une condition est tenue,
 * et alimente l'indicateur % affiché au-dessus de la victime.
 *
 * <ul>
 *   <li>{@code PARTIAL} : la jauge <i>redescend</i> quand la condition n'est plus tenue (« il la
 *       regarde, 10 %… 15 %… puis ça baisse s'il détourne le regard »).</li>
 *   <li>{@code GRACE} : tolère un court décrochage ({@code curseBreakGraceTicks}) puis <i>remet à
 *       zéro</i> si la condition reste rompue trop longtemps.</li>
 * </ul>
 */
public class AccumulationAction implements CurseAction {

    public enum Mode {PARTIAL, GRACE}

    /** Condition vérifiée à chaque échantillon. */
    public interface Cond {
        boolean test(ServerPlayer curser, ServerPlayer target, ItFollowsConfig config);
    }

    /** Texte d'objectif. */
    public interface Describer {
        String describe(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config);
    }

    private final String id;
    private final String name;
    private final Mode mode;
    private final ToIntFunction<ItFollowsConfig> required;
    private final Cond cond;
    private final Describer describer;
    private final Cond candidate;

    public AccumulationAction(String id, String name, Mode mode, ToIntFunction<ItFollowsConfig> required,
                              Cond cond, Describer describer, Cond candidate) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.required = required;
        this.cond = cond;
        this.describer = describer;
        this.candidate = candidate;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String describe(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config) {
        return describer.describe(curser, target, p, config);
    }

    @Override
    public boolean isCandidate(ServerPlayer curser, ServerPlayer target, ItFollowsConfig config) {
        return candidate == null || candidate.test(curser, target, config);
    }

    @Override
    public boolean onTick(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config) {
        long now = curser.serverLevel().getGameTime();
        int interval = Math.max(1, config.curseSampleIntervalTicks);
        int req = Math.max(1, required.applyAsInt(config));
        boolean ok = cond.test(curser, target, config);

        if (ok) {
            p.count += interval;
            p.lastSatisfiedTime = now;
        } else if (mode == Mode.PARTIAL) {
            p.count = Math.max(0, p.count - interval);
        } else if (now - p.lastSatisfiedTime > config.curseBreakGraceTicks) {
            p.count = 0;
        }

        p.count = Math.min(p.count, req);
        p.fraction = (float) p.count / req;
        return p.count >= req;
    }
}
