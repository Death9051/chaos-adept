package ru.adept.core.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import ru.adept.core.config.BalanceConfig;

import java.util.Optional;

/**
 * Детерминированная сетка узлов. Мир делится на ячейки; в каждой ячейке есть одна точка-кандидат,
 * вычисляемая из сида. Кандидат годится, если он дальше минимума от точки появления, стоит в
 * подходящем биоме (не океан, не река, не пляж) и среди соседних ячеек с тем же биомом у него
 * наименьший хеш — так в одном биоме оказывается один узел (приближение «не больше одного на биом»).
 *
 * Биом берётся из источника биомов без генерации чанков, поэтому резонатор видит узлы
 * и в ещё не загруженных местах; сам кристалл ставится, когда загрузится его чанк.
 */
public final class NodeGrid {
    public record Candidate(long cellKey, BlockPos column, int level, Holder<Biome> biome) {
        public String biomeId() {
            return biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        }
    }

    private NodeGrid() {
    }

    public static int cellSize() {
        return Math.max(16, BalanceConfig.NODE_CELL_SIZE.get() / 16 * 16);
    }

    public static int cellOf(int block) {
        return Math.floorDiv(block, cellSize());
    }

    public static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    /** Уровень узла по расстоянию до точки появления. */
    public static int levelForDistance(double distance) {
        int level = 1 + (int) (distance / BalanceConfig.NODE_BLOCKS_PER_LEVEL.get());
        return Mth.clamp(level, 1, BalanceConfig.NODE_MAX_LEVEL.get());
    }

    public static Optional<Candidate> candidate(ServerLevel level, int cellX, int cellZ) {
        Optional<Candidate> raw = raw(level, cellX, cellZ);
        if (raw.isEmpty()) {
            return raw;
        }
        long hash = hash(level.getSeed(), cellX, cellZ);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Optional<Candidate> other = raw(level, cellX + dx, cellZ + dz);
                if (other.isPresent() && other.get().biome().is(raw.get().biome())
                        && Long.compareUnsigned(hash(level.getSeed(), cellX + dx, cellZ + dz), hash) < 0) {
                    return Optional.empty();
                }
            }
        }
        return raw;
    }

    /** Колонка кандидата ячейки (без проверки биома и расстояния) — дёшево, для фильтра по чанку. */
    public static BlockPos column(long seed, int cellX, int cellZ) {
        int cell = cellSize();
        int margin = cell / 8;
        long h = hash(seed, cellX, cellZ);
        int x = cellX * cell + margin + (int) Long.remainderUnsigned(h, cell - 2L * margin);
        int z = cellZ * cell + margin + (int) Long.remainderUnsigned(h >>> 24, cell - 2L * margin);
        return new BlockPos(x, 0, z);
    }

    private static Optional<Candidate> raw(ServerLevel level, int cellX, int cellZ) {
        BlockPos column = column(level.getSeed(), cellX, cellZ);
        int x = column.getX();
        int z = column.getZ();
        BlockPos spawn = level.getSharedSpawnPos();
        double distance = Math.sqrt(Mth.square((double) x - spawn.getX()) + Mth.square((double) z - spawn.getZ()));
        if (distance < BalanceConfig.NODE_MIN_SPAWN_DISTANCE.get()) {
            return Optional.empty();
        }
        Holder<Biome> biome = biomeAt(level, x, z);
        if (!eligible(biome)) {
            return Optional.empty();
        }
        return Optional.of(new Candidate(cellKey(cellX, cellZ), new BlockPos(x, 0, z), levelForDistance(distance), biome));
    }

    public static Holder<Biome> biomeAt(ServerLevel level, int x, int z) {
        int y = level.getSeaLevel() + 4;
        return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                level.getChunkSource().randomState().sampler());
    }

    public static boolean eligible(Holder<Biome> biome) {
        return !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_DEEP_OCEAN)
                && !biome.is(BiomeTags.IS_RIVER) && !biome.is(BiomeTags.IS_BEACH);
    }

    static long hash(long seed, int cellX, int cellZ) {
        long h = seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL) ^ 0x4164657074L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
