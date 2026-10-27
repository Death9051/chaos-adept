package ru.adept.core.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.WorldProgressEvent;
import ru.adept.core.config.BalanceConfig;

/**
 * Общий прогресс мира: текущий уровень узлов (акт) и угроза.
 * Уровень узлов растёт только вперёд в игре; отладочная команда может задать любой.
 * Изменения публикуются событиями {@link WorldProgressEvent}.
 */
public final class WorldProgress extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_progress";
    private static final SavedData.Factory<WorldProgress> FACTORY =
            new SavedData.Factory<>(WorldProgress::new, WorldProgress::load, null);

    private int nodeTier = 1;
    private int threat;

    private WorldProgress() {
    }

    public static WorldProgress get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /** Текущий уровень узлов, 1…{@code node.maxLevel}. */
    public int nodeTier() {
        return nodeTier;
    }

    /** Угроза мира, 0–100. */
    public int threat() {
        return threat;
    }

    public void setNodeTier(MinecraftServer server, int value) {
        int clamped = Mth.clamp(value, 1, BalanceConfig.NODE_MAX_LEVEL.get());
        if (clamped == nodeTier) {
            return;
        }
        int old = nodeTier;
        nodeTier = clamped;
        setDirty();
        AdeptEvents.post(new WorldProgressEvent.NodeTierChanged(server, old, clamped));
    }

    public void setThreat(MinecraftServer server, int value) {
        int clamped = Mth.clamp(value, 0, 100);
        if (clamped == threat) {
            return;
        }
        int old = threat;
        threat = clamped;
        setDirty();
        AdeptEvents.post(new WorldProgressEvent.ThreatChanged(server, old, clamped));
    }

    private static WorldProgress load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldProgress data = new WorldProgress();
        data.nodeTier = Math.max(1, tag.getInt("node_tier"));
        data.threat = tag.getInt("threat");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("node_tier", nodeTier);
        tag.putInt("threat", threat);
        return tag;
    }
}
