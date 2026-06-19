package io.github.yutoutcourt.itfollows.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Store persistant des malédictions (Phase 3), attaché à l'overworld comme {@code FatigueState} /
 * {@code StalkTrackerState}. Connaît les joueurs maudits et garde une mémoire glissante des
 * dernières actions tirées (anti-répétition).
 */
public class CurseState extends SavedData {

    private static final String DATA_NAME = "itfollows_curse";

    private final Map<UUID, CurseData> curses = new HashMap<>();
    /** Ids des dernières actions assignées (la plus récente en tête). */
    private final Deque<String> recentActions = new ArrayDeque<>();

    public static CurseState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                CurseState::load,
                CurseState::new,
                DATA_NAME);
    }

    public boolean isCursed(UUID playerId) {
        return curses.containsKey(playerId);
    }

    public CurseData getCurse(UUID playerId) {
        return curses.get(playerId);
    }

    /** Pose/écrase une malédiction sur un joueur. */
    public CurseData setCurse(UUID playerId, UUID source, long now) {
        CurseData data = new CurseData();
        data.curseSource = source;
        data.curseStartTime = now;
        curses.put(playerId, data);
        setDirty();
        return data;
    }

    public void removeCurse(UUID playerId) {
        if (curses.remove(playerId) != null) {
            setDirty();
        }
    }

    public List<UUID> cursedPlayers() {
        return new ArrayList<>(curses.keySet());
    }

    public void markDirty() {
        setDirty();
    }

    // --- Anti-répétition ---

    public boolean wasRecent(String actionId, int memory) {
        int i = 0;
        for (String id : recentActions) {
            if (i++ >= memory) {
                break;
            }
            if (id.equals(actionId)) {
                return true;
            }
        }
        return false;
    }

    public void pushRecent(String actionId, int memory) {
        recentActions.addFirst(actionId);
        while (recentActions.size() > Math.max(1, memory) * 2) {
            recentActions.removeLast();
        }
        setDirty();
    }

    // --- Persistance ---

    public static CurseState load(CompoundTag tag) {
        CurseState state = new CurseState();
        CompoundTag players = tag.getCompound("curses");
        for (String key : players.getAllKeys()) {
            try {
                state.curses.put(UUID.fromString(key), CurseData.load(players.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // clé UUID corrompue → ignorée
            }
        }
        ListTag recent = tag.getList("recent", Tag.TAG_STRING);
        for (int i = 0; i < recent.size(); i++) {
            state.recentActions.addLast(recent.getString(i));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, CurseData> e : curses.entrySet()) {
            players.put(e.getKey().toString(), e.getValue().save());
        }
        tag.put("curses", players);
        ListTag recent = new ListTag();
        for (String id : recentActions) {
            recent.add(StringTag.valueOf(id));
        }
        tag.put("recent", recent);
        return tag;
    }
}
