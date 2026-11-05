package ru.adept.core.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Деревни мира. Деревня = ванильная структура с тегом {@code #minecraft:village}; номер — чанк её начала.
 * Запись создаётся, когда в деревне появляется житель или когда её чанк поглощается, — даже если игрок там не был.
 */
public final class VillageRegistry extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_villages";
    private static final SavedData.Factory<VillageRegistry> FACTORY =
            new SavedData.Factory<>(VillageRegistry::new, VillageRegistry::load, null);

    private final Map<Long, VillageRecord> villages = new HashMap<>();

    private VillageRegistry() {
    }

    public static VillageRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Collection<VillageRecord> all() {
        return villages.values();
    }

    public Optional<VillageRecord> byId(long id) {
        return Optional.ofNullable(villages.get(id));
    }

    public Optional<VillageRecord> byResident(java.util.UUID id) {
        return villages.values().stream().filter(v -> v.residents.contains(id)).findFirst();
    }

    /** Ближайшая записанная деревня, в границах которой (с запасом) лежит точка. */
    public Optional<VillageRecord> at(double x, double z, int margin) {
        return villages.values().stream().filter(v -> v.containsXZ(x, z, margin))
                .min(java.util.Comparator.comparingDouble(v -> v.center().distToCenterSqr(x, v.center().getY(), z)));
    }

    /** Найти или записать деревню по её структуре. */
    public VillageRecord register(ServerLevel level, StructureStart start) {
        long id = start.getChunkPos().toLong();
        VillageRecord existing = villages.get(id);
        if (existing != null) {
            return existing;
        }
        Set<String> taken = villages.values().stream().map(v -> v.name).collect(Collectors.toSet());
        VillageRecord record = new VillageRecord(id, NamePools.pick(NamePools.villages(), taken, level.random), start.getBoundingBox());
        villages.put(id, record);
        setDirty();
        AdeptCore.LOG.debug("Деревня «{}» записана: {}", record.name, start.getBoundingBox());
        return record;
    }

    /** Отладка и тесты: записать деревню с произвольными границами. */
    public VillageRecord registerManual(long id, String name, net.minecraft.world.level.levelgen.structure.BoundingBox box) {
        VillageRecord record = new VillageRecord(id, name, box);
        villages.put(id, record);
        setDirty();
        return record;
    }

    public void remove(long id) {
        villages.remove(id);
        setDirty();
    }

    /** Деревня, в структуру которой входит точка (по ванильным данным структур). */
    public Optional<VillageRecord> findOrRegisterAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (start.isValid()) {
            return Optional.of(register(level, start));
        }
        return at(pos.getX(), pos.getZ(), 16);
    }

    /** Все деревни, части которых лежат в чанке (по ссылкам структур чанка). */
    public java.util.List<VillageRecord> registerInChunk(ServerLevel level, ChunkAccess chunk) {
        java.util.List<VillageRecord> out = new java.util.ArrayList<>();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> e : chunk.getAllReferences().entrySet()) {
            if (!registry.wrapAsHolder(e.getKey()).is(StructureTags.VILLAGE)) {
                continue;
            }
            for (long startKey : e.getValue()) {
                ChunkPos sp = new ChunkPos(startKey);
                StructureStart start = level.structureManager().getStartForStructure(SectionPos.of(sp, 0), e.getKey(),
                        level.getChunk(sp.x, sp.z, ChunkStatus.STRUCTURE_STARTS));
                if (start != null && start.isValid()) {
                    out.add(register(level, start));
                }
            }
        }
        return out;
    }

    public void changed() {
        setDirty();
    }

    private static VillageRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageRegistry data = new VillageRegistry();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            VillageRecord.load((CompoundTag) t).ifPresent(r -> data.villages.put(r.id, r));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        villages.values().forEach(v -> list.add(v.save()));
        tag.put("villages", list);
        return tag;
    }
}
