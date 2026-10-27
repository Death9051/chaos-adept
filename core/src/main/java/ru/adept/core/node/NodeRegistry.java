package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Реестр поставленных узлов верхнего мира и ячеек сетки, где узел уже поставлен. */
public final class NodeRegistry extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_nodes";
    private static final SavedData.Factory<NodeRegistry> FACTORY =
            new SavedData.Factory<>(NodeRegistry::new, NodeRegistry::load, null);

    private final Map<BlockPos, NodeRecord> nodes = new LinkedHashMap<>();
    private final Set<Long> placedCells = new HashSet<>();

    private NodeRegistry() {
    }

    public static NodeRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Collection<NodeRecord> all() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Optional<NodeRecord> at(BlockPos pos) {
        return Optional.ofNullable(nodes.get(pos));
    }

    public Optional<NodeRecord> nearest(BlockPos from) {
        return nodes.values().stream().min(Comparator.comparingDouble(n -> n.pos().distSqr(from)));
    }

    public boolean isCellPlaced(long cellKey) {
        return placedCells.contains(cellKey);
    }

    /** Добавить узел. {@code cellKey} — ячейка сетки или {@code null} для узлов из отладочной команды. */
    public void add(NodeRecord node, Long cellKey) {
        nodes.put(node.pos(), node);
        if (cellKey != null) {
            placedCells.add(cellKey);
        }
        setDirty();
    }

    public void update(NodeRecord node) {
        if (nodes.containsKey(node.pos())) {
            nodes.put(node.pos(), node);
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (nodes.remove(pos) != null) {
            setDirty();
        }
    }

    private static NodeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        NodeRegistry data = new NodeRegistry();
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            NodeRecord node = NodeRecord.load(list.getCompound(i));
            data.nodes.put(node.pos(), node);
        }
        for (long key : tag.getLongArray("cells")) {
            data.placedCells.add(key);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        nodes.values().forEach(n -> list.add(n.save()));
        tag.put("nodes", list);
        tag.put("cells", new LongArrayTag(placedCells.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }
}
