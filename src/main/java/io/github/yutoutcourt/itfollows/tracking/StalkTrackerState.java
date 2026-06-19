package io.github.yutoutcourt.itfollows.tracking;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * État persistant du système de traque (Phase 2), attaché à l'overworld comme
 * {@code FatigueState}. Connaît la cible courante, l'entité spawnée et la phase d'escalade.
 *
 * <p>L'entité elle-même n'est pas sauvegardée (cf. {@code StalkerEntity#shouldBeSaved}) ;
 * on ne persiste ici que la <i>logique</i> de traque, robuste aux déco/reco et redémarrages.
 */
public class StalkTrackerState extends SavedData {

    private static final String DATA_NAME = "itfollows_stalker";

    private UUID targetPlayer;
    private UUID stalkerEntityId;
    private int phase;
    /** Dernière cible morte sous la traque : exclue de la prochaine sélection (anti « maudit à vie »). */
    private UUID lastVictim;

    /**
     * Position « logique » de la traqueuse quand elle n'est <i>pas</i> matérialisée (modèle C).
     * Elle avance vers la cible hors-chunks (simple arithmétique, aucun chunk chargé) ; l'entité
     * réelle n'apparaît qu'à proximité. {@code null} tant qu'aucune traque n'est amorcée.
     */
    private Vec3 virtualPos;
    /** Dimension associée à {@link #virtualPos}. */
    private ResourceKey<Level> virtualDim;

    public static StalkTrackerState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                StalkTrackerState::load,
                StalkTrackerState::new,
                DATA_NAME);
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
        setDirty();
    }

    public UUID getStalkerEntityId() {
        return stalkerEntityId;
    }

    public void setStalkerEntityId(UUID stalkerEntityId) {
        this.stalkerEntityId = stalkerEntityId;
        setDirty();
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
        setDirty();
    }

    public UUID getLastVictim() {
        return lastVictim;
    }

    public void setLastVictim(UUID lastVictim) {
        this.lastVictim = lastVictim;
        setDirty();
    }

    public Vec3 getVirtualPos() {
        return virtualPos;
    }

    public ResourceKey<Level> getVirtualDim() {
        return virtualDim;
    }

    public void setVirtual(Vec3 pos, ResourceKey<Level> dim) {
        this.virtualPos = pos;
        this.virtualDim = dim;
        setDirty();
    }

    public void clearVirtual() {
        this.virtualPos = null;
        this.virtualDim = null;
        setDirty();
    }

    public static StalkTrackerState load(CompoundTag tag) {
        StalkTrackerState state = new StalkTrackerState();
        if (tag.hasUUID("target")) {
            state.targetPlayer = tag.getUUID("target");
        }
        if (tag.hasUUID("entity")) {
            state.stalkerEntityId = tag.getUUID("entity");
        }
        if (tag.hasUUID("lastVictim")) {
            state.lastVictim = tag.getUUID("lastVictim");
        }
        if (tag.contains("virtualDim") && tag.contains("vx")) {
            state.virtualPos = new Vec3(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
            state.virtualDim = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(tag.getString("virtualDim")));
        }
        state.phase = tag.getInt("phase");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (targetPlayer != null) {
            tag.putUUID("target", targetPlayer);
        }
        if (stalkerEntityId != null) {
            tag.putUUID("entity", stalkerEntityId);
        }
        if (lastVictim != null) {
            tag.putUUID("lastVictim", lastVictim);
        }
        if (virtualPos != null && virtualDim != null) {
            tag.putDouble("vx", virtualPos.x);
            tag.putDouble("vy", virtualPos.y);
            tag.putDouble("vz", virtualPos.z);
            tag.putString("virtualDim", virtualDim.location().toString());
        }
        tag.putInt("phase", phase);
        return tag;
    }
}
