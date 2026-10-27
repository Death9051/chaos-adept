package ru.adept.core.absorb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Что ещё не превратилось в пустошь: чанки с маской ячеек 8×8 (4 бита на чанк)
 * и узлы, для которых ещё ищутся границы биома.
 */
public final class AbsorptionData extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_absorption";
    private static final SavedData.Factory<AbsorptionData> FACTORY =
            new SavedData.Factory<>(AbsorptionData::new, AbsorptionData::load, null);

    final Map<Long, Integer> pendingChunks = new HashMap<>();
    final List<BlockPos> pendingRegions = new ArrayList<>();
    /** Уровень узла каждой области (0 — неизвестен, для старых сохранений). */
    final Map<BlockPos, Integer> regionLevels = new HashMap<>();

    private AbsorptionData() {
    }

    public static AbsorptionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public int pendingChunkCount() {
        return pendingChunks.size();
    }

    public int pendingRegionCount() {
        return pendingRegions.size();
    }

    void addCells(Map<Long, Integer> chunks) {
        chunks.forEach((key, mask) -> pendingChunks.merge(key, mask, (a, b) -> a | b));
        setDirty();
    }

    private static AbsorptionData load(CompoundTag tag, HolderLookup.Provider registries) {
        AbsorptionData data = new AbsorptionData();
        long[] keys = tag.getLongArray("chunks");
        int[] masks = tag.getIntArray("masks");
        for (int i = 0; i < Math.min(keys.length, masks.length); i++) {
            data.pendingChunks.put(keys[i], masks[i]);
        }
        ListTag regions = tag.getList("regionsV2", Tag.TAG_COMPOUND);
        for (int i = 0; i < regions.size(); i++) {
            CompoundTag region = regions.getCompound(i);
            int level = region.getInt("level");
            NbtUtils.readBlockPos(region, "pos").ifPresent(pos -> {
                data.pendingRegions.add(pos);
                data.regionLevels.put(pos, level);
            });
        }
        // Старый формат (до уровня узла): только позиции.
        ListTag old = tag.getList("regions", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < old.size(); i++) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("p", old.get(i));
            NbtUtils.readBlockPos(wrapper, "p").ifPresent(data.pendingRegions::add);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] keys = new long[pendingChunks.size()];
        int[] masks = new int[pendingChunks.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : pendingChunks.entrySet()) {
            keys[i] = e.getKey();
            masks[i++] = e.getValue();
        }
        tag.putLongArray("chunks", keys);
        tag.putIntArray("masks", masks);
        ListTag regions = new ListTag();
        for (BlockPos p : pendingRegions) {
            CompoundTag region = new CompoundTag();
            region.put("pos", NbtUtils.writeBlockPos(p));
            region.putInt("level", regionLevels.getOrDefault(p, 0));
            regions.add(region);
        }
        tag.put("regionsV2", regions);
        return tag;
    }
}
