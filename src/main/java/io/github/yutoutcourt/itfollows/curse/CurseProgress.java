package io.github.yutoutcourt.itfollows.curse;

import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

/**
 * État <b>transitoire</b> de progression d'une action de malédiction en cours, gardé en mémoire
 * par {@link CurseManager} (clé = UUID du maudit). Non persisté : si le serveur redémarre, l'action
 * active repart de zéro, ce qui est acceptable (l'objectif lui-même reste, lui, persistant).
 *
 * <p>C'est un simple sac de champs génériques réutilisés selon l'action (compteur, fraction
 * d'avancement pour l'indicateur, mémoires diverses), pour éviter une sous-classe d'état par action.
 */
public final class CurseProgress {

    /** Avancement 0..1 affiché au-dessus de la victime (indicateur). -1 = action sans indicateur. */
    public float fraction = -1.0f;
    /** Compteur générique (ticks cumulés, bascules, blocs posés/cassés, items…). */
    public int count;
    /** Heure-monde de la dernière fois où la condition était satisfaite (tolérance de décrochage). */
    public long lastSatisfiedTime = Long.MIN_VALUE;
    /** Booléen précédent générique (état d'accroupissement, posture, dans-l'eau…). */
    public boolean prevBool;
    /** Second booléen précédent (mouvement de la cible pour l'imitateur…). */
    public boolean prevBool2;
    /** Champ flottant auxiliaire (ex. fallDistance précédente de la cible). */
    public float aux;
    /** Item requis déduit à l'assignation (quêteur, sommelier…). */
    public Item requiredItem;
    /** Étiquette lisible de la cible d'objet/catégorie, pour le message. */
    public String requiredLabel = "";
    /** Catégorie requise (dénuement). */
    public ItemCategory requiredCategory;
    /** La victime a bien ramassé l'objet requis donné par le maudit (sommelier : pré-condition au repas). */
    public boolean victimReceivedRequired;
    /** Positions (packées via BlockPos.asLong) déjà comptées, pour ne pas compter deux fois. */
    public final Set<Long> marks = new HashSet<>();
}
