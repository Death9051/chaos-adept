package ru.adept.core.absorb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import ru.adept.core.node.NodeGrid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Поиск связной области биома узла по ячейкам 8×8 блоков (заливка, порциями по тикам).
 * Биом берётся из источника биомов без генерации чанков. Реки и пляжи, примыкающие
 * к области, поглощаются вместе с ней, но заливка через них не идёт.
 */
public final class BiomeRegionSearch {
    public static final int CELL = 8;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Holder<Biome> biome;
    private final int maxRadius;
    private final int maxCells;
    private final Deque<Long> queue = new ArrayDeque<>();
    private final Set<Long> visited = new HashSet<>();
    private final Map<Long, Integer> chunks = new HashMap<>();
    private int included;

    public BiomeRegionSearch(ServerLevel level, BlockPos origin, int maxRadius, int maxCells) {
        this.level = level;
        this.origin = origin;
        this.biome = NodeGrid.biomeAt(level, origin.getX(), origin.getZ());
        this.maxRadius = maxRadius;
        this.maxCells = maxCells;
        long start = key(Math.floorDiv(origin.getX(), CELL), Math.floorDiv(origin.getZ(), CELL));
        queue.add(start);
        visited.add(start);
    }

    public Holder<Biome> biome() {
        return biome;
    }

    public BlockPos origin() {
        return origin;
    }

    public boolean isDone() {
        return queue.isEmpty() || included >= maxCells;
    }

    /** Результат: чанк → маска ячеек (бит = ячейка 8×8 внутри чанка). */
    public Map<Long, Integer> chunks() {
        return chunks;
    }

    public int includedCells() {
        return included;
    }

    /** Обработать до {@code budget} ячеек. */
    public void step(int budget) {
        while (budget-- > 0 && !isDone()) {
            long cell = queue.poll();
            int cx = (int) (cell >> 32);
            int cz = (int) cell;
            int x = cx * CELL + CELL / 2;
            int z = cz * CELL + CELL / 2;
            if (Math.abs(x - origin.getX()) > maxRadius || Math.abs(z - origin.getZ()) > maxRadius) {
                continue;
            }
            Holder<Biome> here = NodeGrid.biomeAt(level, x, z);
            boolean same = here.is(biome);
            boolean small = !same && (here.is(BiomeTags.IS_RIVER) || here.is(BiomeTags.IS_BEACH));
            if (!same && !small) {
                continue;
            }
            include(cx, cz);
            if (same) {
                enqueue(cx + 1, cz);
                enqueue(cx - 1, cz);
                enqueue(cx, cz + 1);
                enqueue(cx, cz - 1);
            }
        }
    }

    private void include(int cx, int cz) {
        included++;
        int blockX = cx * CELL;
        int blockZ = cz * CELL;
        long chunk = ChunkPos.asLong(blockX >> 4, blockZ >> 4);
        chunks.merge(chunk, 1 << bit(blockX, blockZ), (a, b) -> a | b);
    }

    private void enqueue(int cx, int cz) {
        long k = key(cx, cz);
        if (visited.add(k)) {
            queue.add(k);
        }
    }

    /** Номер ячейки 8×8 внутри чанка для блока (0–3). */
    public static int bit(int blockX, int blockZ) {
        return ((blockX >> 3) & 1) | (((blockZ >> 3) & 1) << 1);
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
