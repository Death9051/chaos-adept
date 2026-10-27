package ru.adept.core.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/** Область биома поглощена (границы найдены, чанки превращаются в пустошь по мере загрузки). */
public final class BiomeAbsorbedEvent extends AdeptEvent {
    private final BlockPos node;
    private final int level;
    private final String biome;
    private final int chunks;

    public BiomeAbsorbedEvent(MinecraftServer server, BlockPos node, int level, String biome, int chunks) {
        super(server);
        this.node = node;
        this.level = level;
        this.biome = biome;
        this.chunks = chunks;
    }

    public BlockPos getNode() {
        return node;
    }

    /** Уровень узла (хранится вместе с областью и переживает перезапуск мира). */
    public int getLevel() {
        return level;
    }

    /** Идентификатор исходного биома, например {@code minecraft:forest}. */
    public String getBiome() {
        return biome;
    }

    public int getChunks() {
        return chunks;
    }
}
