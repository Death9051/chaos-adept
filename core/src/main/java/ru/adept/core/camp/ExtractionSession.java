package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Состояние развёрнутого лагеря и выкачки одного узла. */
public final class ExtractionSession {
    public BlockPos node;
    public BlockPos extractor;
    public BlockPos transition;
    public UUID player;
    public int level;
    public double progress;
    public double total;
    public int waves;
    public int wavesCleared;
    public boolean waveActive;
    public int extractorHealth;
    public boolean done;
    public boolean traderCalled;
    public final List<UUID> waveMobs = new ArrayList<>();

    public int currentWave() {
        return waveActive ? wavesCleared + 1 : wavesCleared;
    }

    public int percent() {
        return total <= 0 ? 0 : (int) Math.min(100, Math.floor(progress * 100.0 / total));
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("node", NbtUtils.writeBlockPos(node));
        tag.put("extractor", NbtUtils.writeBlockPos(extractor));
        tag.put("transition", NbtUtils.writeBlockPos(transition));
        tag.putUUID("player", player);
        tag.putInt("level", level);
        tag.putDouble("progress", progress);
        tag.putDouble("total", total);
        tag.putInt("waves", waves);
        tag.putInt("wavesCleared", wavesCleared);
        tag.putBoolean("waveActive", waveActive);
        tag.putInt("extractorHealth", extractorHealth);
        tag.putBoolean("done", done);
        tag.putBoolean("traderCalled", traderCalled);
        ListTag mobs = new ListTag();
        waveMobs.forEach(id -> mobs.add(StringTag.valueOf(id.toString())));
        tag.put("waveMobs", mobs);
        return tag;
    }

    static ExtractionSession load(CompoundTag tag) {
        ExtractionSession s = new ExtractionSession();
        s.node = NbtUtils.readBlockPos(tag, "node").orElse(BlockPos.ZERO);
        s.extractor = NbtUtils.readBlockPos(tag, "extractor").orElse(BlockPos.ZERO);
        s.transition = NbtUtils.readBlockPos(tag, "transition").orElse(BlockPos.ZERO);
        s.player = tag.getUUID("player");
        s.level = tag.getInt("level");
        s.progress = tag.getDouble("progress");
        s.total = tag.getDouble("total");
        s.waves = tag.getInt("waves");
        s.wavesCleared = tag.getInt("wavesCleared");
        s.waveActive = tag.getBoolean("waveActive");
        s.extractorHealth = tag.getInt("extractorHealth");
        s.done = tag.getBoolean("done");
        s.traderCalled = tag.getBoolean("traderCalled");
        ListTag mobs = tag.getList("waveMobs", Tag.TAG_STRING);
        for (int i = 0; i < mobs.size(); i++) {
            try {
                s.waveMobs.add(UUID.fromString(mobs.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // повреждённая запись — пропускаем
            }
        }
        return s;
    }
}
