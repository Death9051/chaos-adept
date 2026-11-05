package ru.adept.core.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Деревня в данных мира: имя, границы, жители, старейшина и состояние. */
public final class VillageRecord {
    public final long id;
    public final String name;
    public final BoundingBox box;
    public final Set<UUID> residents = new HashSet<>();
    public UUID elder;
    public boolean ruined;
    public boolean destroyed;
    /** До какого игрового времени набег в этой местности отложен (после смерти старейшины). */
    public long raidDelayUntil;

    public VillageRecord(long id, String name, BoundingBox box) {
        this.id = id;
        this.name = name;
        this.box = box;
    }

    public BlockPos center() {
        return box.getCenter();
    }

    /** Точка внутри границ деревни по горизонтали (с запасом {@code margin}). */
    public boolean containsXZ(double x, double z, int margin) {
        return x >= box.minX() - margin && x <= box.maxX() + 1 + margin && z >= box.minZ() - margin && z <= box.maxZ() + 1 + margin;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id);
        tag.putString("name", name);
        tag.putIntArray("box", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
        ListTag list = new ListTag();
        residents.forEach(u -> list.add(NbtUtils.createUUID(u)));
        tag.put("residents", list);
        if (elder != null) {
            tag.putUUID("elder", elder);
        }
        tag.putBoolean("ruined", ruined);
        tag.putBoolean("destroyed", destroyed);
        tag.putLong("raid_delay", raidDelayUntil);
        return tag;
    }

    public static Optional<VillageRecord> load(CompoundTag tag) {
        int[] b = tag.getIntArray("box");
        if (b.length != 6) {
            return Optional.empty();
        }
        VillageRecord r = new VillageRecord(tag.getLong("id"), tag.getString("name"), new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        for (Tag t : tag.getList("residents", Tag.TAG_INT_ARRAY)) {
            r.residents.add(NbtUtils.loadUUID(t));
        }
        if (tag.hasUUID("elder")) {
            r.elder = tag.getUUID("elder");
        }
        r.ruined = tag.getBoolean("ruined");
        r.destroyed = tag.getBoolean("destroyed");
        r.raidDelayUntil = tag.getLong("raid_delay");
        return Optional.of(r);
    }
}
