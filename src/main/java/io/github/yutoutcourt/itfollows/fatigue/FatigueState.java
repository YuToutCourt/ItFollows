package io.github.yutoutcourt.itfollows.fatigue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Store persistant de la fatigue par joueur, indexé par UUID.
 *
 * <p>1.20.1 n'a pas l'API Data Attachment ; on utilise donc {@link SavedData}
 * (= PersistentState) attaché à l'overworld. Survit déconnexion/reconnexion et
 * redémarrage serveur, indépendamment de la NBT du joueur.
 */
public class    FatigueState extends SavedData {

    private static final String DATA_NAME = "itfollows_fatigue";
    private static final float DEFAULT_FATIGUE = FatigueRules.MAX;

    private final Map<UUID, Float> fatigueByPlayer = new HashMap<>();

    public static FatigueState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                FatigueState::load,
                FatigueState::new,
                DATA_NAME);
    }

    public float getFatigue(UUID playerId) {
        return fatigueByPlayer.getOrDefault(playerId, DEFAULT_FATIGUE);
    }

    public void setFatigue(UUID playerId, float value) {
        fatigueByPlayer.put(playerId, FatigueRules.clamp(value));
        setDirty();
    }

    public static FatigueState load(CompoundTag tag) {
        FatigueState state = new FatigueState();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                state.fatigueByPlayer.put(UUID.fromString(key), players.getFloat(key));
            } catch (IllegalArgumentException ignored) {
                // clé UUID invalide → on ignore l'entrée corrompue
            }
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Float> entry : fatigueByPlayer.entrySet()) {
            players.putFloat(entry.getKey().toString(), entry.getValue());
        }
        tag.put("players", players);
        return tag;
    }
}
