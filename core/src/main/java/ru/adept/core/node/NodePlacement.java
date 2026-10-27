package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.registry.ModBlocks;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Optional;

/**
 * Постановка кристаллов: когда загружается чанк с кандидатом сетки, кандидат встаёт в очередь,
 * а на следующем тике сервера кристалл ставится на поверхность (не внутри события загрузки чанка).
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class NodePlacement {
    private static final Deque<NodeGrid.Candidate> QUEUE = new ConcurrentLinkedDeque<>();

    private NodePlacement() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChunkPos chunk = event.getChunk().getPos();
        int cellX = NodeGrid.cellOf(chunk.getMinBlockX());
        int cellZ = NodeGrid.cellOf(chunk.getMinBlockZ());
        if (!new ChunkPos(NodeGrid.column(level.getSeed(), cellX, cellZ)).equals(chunk)) {
            return;
        }
        NodeRegistry registry = NodeRegistry.get(level.getServer());
        if (registry.isCellPlaced(NodeGrid.cellKey(cellX, cellZ))) {
            return;
        }
        Optional<NodeGrid.Candidate> candidate = NodeGrid.candidate(level, cellX, cellZ);
        candidate.ifPresent(QUEUE::add);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        NodeRegistry registry = NodeRegistry.get(server);
        int budget = 4;
        while (!QUEUE.isEmpty() && budget-- > 0) {
            NodeGrid.Candidate c = QUEUE.poll();
            if (registry.isCellPlaced(c.cellKey()) || !level.hasChunkAt(c.column())) {
                continue;
            }
            BlockPos pos = place(level, c.column());
            registry.add(new NodeRecord(pos, c.level(), c.biomeId(), NodeRecord.State.DORMANT), c.cellKey());
            AdeptCore.LOG.debug("Узел уровня {} поставлен в {} ({})", c.level(), pos, c.biomeId());
        }
    }

    /** Поставить кристалл на поверхность над колонкой; возвращает позицию кристалла. */
    public static BlockPos place(ServerLevel level, BlockPos column) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
        level.setBlock(pos, ModBlocks.NODE_CRYSTAL.get().defaultBlockState(), 3);
        return pos;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
    }
}
