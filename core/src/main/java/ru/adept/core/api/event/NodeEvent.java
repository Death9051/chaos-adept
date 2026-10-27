package ru.adept.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/** События узла и выкачки. Позиция — кристалл узла. */
public abstract class NodeEvent extends AdeptEvent {
    private final BlockPos node;
    private final int level;

    protected NodeEvent(MinecraftServer server, BlockPos node, int level) {
        super(server);
        this.node = node;
        this.level = level;
    }

    public BlockPos getNode() {
        return node;
    }

    public int getLevel() {
        return level;
    }

    /** Лагерь развёрнут, выкачка началась. */
    public static final class ExtractionStarted extends NodeEvent {
        public ExtractionStarted(MinecraftServer server, BlockPos node, int level) {
            super(server, node, level);
        }
    }

    /** Началась волна {@code wave} из {@code waves}; последняя приводит стража. */
    public static final class WaveStarted extends NodeEvent {
        private final int wave;
        private final int waves;

        public WaveStarted(MinecraftServer server, BlockPos node, int level, int wave, int waves) {
            super(server, node, level);
            this.wave = wave;
            this.waves = waves;
        }

        public int getWave() {
            return wave;
        }

        public int getWaves() {
            return waves;
        }

        public boolean isFinal() {
            return wave >= waves;
        }
    }

    /** Волна отбита. */
    public static final class WaveCleared extends NodeEvent {
        private final int wave;
        private final int waves;

        public WaveCleared(MinecraftServer server, BlockPos node, int level, int wave, int waves) {
            super(server, node, level);
            this.wave = wave;
            this.waves = waves;
        }

        public int getWave() {
            return wave;
        }

        public int getWaves() {
            return waves;
        }
    }

    /** Игрок погиб на узле: волна отступила, прогресс откатился. */
    public static final class PlayerDied extends NodeEvent {
        private final int rollbackPercent;

        public PlayerDied(MinecraftServer server, BlockPos node, int level, int rollbackPercent) {
            super(server, node, level);
            this.rollbackPercent = rollbackPercent;
        }

        public int getRollbackPercent() {
            return rollbackPercent;
        }
    }

    /** Установка выкачки сломана, выкачка стоит до ремонта. */
    public static final class ExtractorBroken extends NodeEvent {
        public ExtractorBroken(MinecraftServer server, BlockPos node, int level) {
            super(server, node, level);
        }
    }

    /** Узел зачищен: финальная волна со стражем отбита. */
    public static final class Cleansed extends NodeEvent {
        public Cleansed(MinecraftServer server, BlockPos node, int level) {
            super(server, node, level);
        }
    }
}
