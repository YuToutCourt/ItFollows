package io.github.yutoutcourt.itfollows.curse;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Données persistées d'un joueur maudit : qui l'a maudit, depuis quand, l'action assignée,
 * la victime désignée, et l'anti-ping-pong (interdiction temporaire de re-maudire une personne).
 */
public final class CurseData {

    /** Qui a transmis la malédiction (null = malédiction initiale, posée par l'entité). */
    public UUID curseSource;
    /** Heure-monde de l'attribution de la malédiction. */
    public long curseStartTime;

    /** Id de l'action en cours (null tant que l'objectif n'est pas livré). */
    public String activeActionId;
    /** Victime désignée par l'action (futur maudit). */
    public UUID designatedVictim;
    /** L'objectif a-t-il déjà été livré (anti-double-envoi) ? */
    public boolean objectiveSent;

    /** Personne que ce maudit ne peut PAS désigner comme victime (anti aller-retour). */
    public UUID antiPingPongTarget;
    /** Heure-monde jusqu'à laquelle l'interdiction tient. */
    public long antiPingPongUntil;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (curseSource != null) {
            tag.putUUID("source", curseSource);
        }
        tag.putLong("start", curseStartTime);
        if (activeActionId != null) {
            tag.putString("action", activeActionId);
        }
        if (designatedVictim != null) {
            tag.putUUID("victim", designatedVictim);
        }
        tag.putBoolean("sent", objectiveSent);
        if (antiPingPongTarget != null) {
            tag.putUUID("ppTarget", antiPingPongTarget);
            tag.putLong("ppUntil", antiPingPongUntil);
        }
        return tag;
    }

    public static CurseData load(CompoundTag tag) {
        CurseData data = new CurseData();
        if (tag.hasUUID("source")) {
            data.curseSource = tag.getUUID("source");
        }
        data.curseStartTime = tag.getLong("start");
        if (tag.contains("action")) {
            data.activeActionId = tag.getString("action");
        }
        if (tag.hasUUID("victim")) {
            data.designatedVictim = tag.getUUID("victim");
        }
        data.objectiveSent = tag.getBoolean("sent");
        if (tag.hasUUID("ppTarget")) {
            data.antiPingPongTarget = tag.getUUID("ppTarget");
            data.antiPingPongUntil = tag.getLong("ppUntil");
        }
        return data;
    }
}
