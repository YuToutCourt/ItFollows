package io.github.yutoutcourt.itfollows.curse;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Une action de malédiction de la banque (Phase 3). Le <b>maudit</b> ({@code curser}) doit
 * l'accomplir sur la <b>victime désignée</b> ({@code target}) ; une fois validée, la malédiction
 * et la cible de l'entité passent à la victime.
 *
 * <p>La détection passe soit par {@link #onTick} (accumulation/regard/position, échantillonné),
 * soit par les hooks d'événements (casse/pose de bloc par le maudit, ramassage/consommation par
 * la victime). Toutes les méthodes de détection renvoient {@code true} quand l'action est complète.
 */
public interface CurseAction {

    String id();

    /** Nom court lisible (pour le statut debug). */
    String name();

    /** Poids de tirage (plus haut = plus fréquent). */
    default int weight() {
        return 10;
    }

    /** Description envoyée au maudit (peut nommer la victime, le seuil, l'item déduit dans {@code p}). */
    String describe(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config);

    /** L'action est-elle réalisable dans le contexte courant ? (gating anti « trop facile »/fallback). */
    default boolean isCandidate(ServerPlayer curser, ServerPlayer target, ItFollowsConfig config) {
        return true;
    }

    /** Avancement 0..1 pour l'indicateur au-dessus de la victime, ou -1 si l'action n'en a pas. */
    default float indicator(CurseProgress p) {
        return p.fraction;
    }

    /** Préparation à l'assignation (ex. choisir l'item rare manquant). */
    default void onAssign(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config) {
    }

    /** Échantillonnage périodique. */
    default boolean onTick(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config) {
        return false;
    }

    /** Le maudit a cassé un bloc. */
    default boolean onCurserBreakBlock(ServerPlayer curser, ServerPlayer target, BlockPos pos,
                                       BlockState state, CurseProgress p, ItFollowsConfig config) {
        return false;
    }

    /** Le maudit a posé un bloc. */
    default boolean onCurserPlaceBlock(ServerPlayer curser, ServerPlayer target, BlockPos pos,
                                       BlockState state, CurseProgress p, ItFollowsConfig config) {
        return false;
    }

    /** La victime a ramassé un item. */
    default boolean onVictimPickup(ServerPlayer curser, ServerPlayer target, ItemStack stack,
                                   CurseProgress p, ItFollowsConfig config) {
        return false;
    }

    /** La victime a fini de consommer un item. */
    default boolean onVictimConsume(ServerPlayer curser, ServerPlayer target, ItemStack stack,
                                    CurseProgress p, ItFollowsConfig config) {
        return false;
    }

    /** Le maudit a sauté. */
    default boolean onCurserJump(ServerPlayer curser, ServerPlayer target, CurseProgress p, ItFollowsConfig config) {
        return false;
    }
}
