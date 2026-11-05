package ru.adept.core.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import ru.adept.core.absorb.BiomeRegionSearch;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.VillageEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlocks;

import java.util.List;

/**
 * Руины: когда поглощение превращает чанк, деревни в нём (по данным структур, даже непосещённые)
 * записываются и становятся руинами — двери, кровати, стёкла и станции пропадают, дерево частью выгорает,
 * камень зарастает мхом, жители уходят. Границы руин хранятся в {@link VillageRegistry}.
 */
public final class RuinsManager {
    private RuinsManager() {
    }

    /** Вызывается поглощением после превращения чанка по маске ячеек. */
    public static void onChunkConverted(ServerLevel level, LevelChunk chunk, int mask) {
        VillageRegistry registry = VillageRegistry.get(level.getServer());
        List<VillageRecord> villages = registry.registerInChunk(level, chunk);
        ChunkPos cp = chunk.getPos();
        for (VillageRecord village : villages) {
            markRuined(level, village);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = cp.getMinBlockX() + dx;
                    int z = cp.getMinBlockZ() + dz;
                    if ((mask & (1 << BiomeRegionSearch.bit(x, z))) != 0 && village.containsXZ(x, z, 0)) {
                        ruinColumn(level, x, z, village.box);
                    }
                }
            }
            AABB area = new AABB(cp.getMinBlockX(), village.box.minY() - 4, cp.getMinBlockZ(),
                    cp.getMaxBlockX() + 1, village.box.maxY() + 8, cp.getMaxBlockZ() + 1);
            for (ResidentEntity resident : level.getEntitiesOfClass(ResidentEntity.class, area)) {
                village.residents.remove(resident.getUUID());
                resident.discard();
            }
            registry.changed();
        }
    }

    /** Отметить деревню руинами (один раз) и сообщить сюжету. */
    public static void markRuined(ServerLevel level, VillageRecord village) {
        if (village.ruined) {
            return;
        }
        village.ruined = true;
        village.elder = null;
        VillageRegistry.get(level.getServer()).changed();
        AdeptEvents.post(new VillageEvent.Ruined(level.getServer(), village.id, village.name));
    }

    /** Разрушить всю деревню в загруженных чанках (отладочная команда). */
    public static int ruinWhole(ServerLevel level, VillageRecord village) {
        markRuined(level, village);
        int columns = 0;
        BoundingBox b = village.box;
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                if (level.hasChunkAt(new BlockPos(x, b.minY(), z))) {
                    ruinColumn(level, x, z, b);
                    columns++;
                }
            }
        }
        AABB area = new AABB(b.minX(), b.minY() - 4, b.minZ(), b.maxX() + 1, b.maxY() + 8, b.maxZ() + 1);
        for (ResidentEntity resident : level.getEntitiesOfClass(ResidentEntity.class, area)) {
            village.residents.remove(resident.getUUID());
            resident.discard();
        }
        return columns;
    }

    /** Колонка руин в высотах деревни. Случай детерминирован по координатам. */
    public static void ruinColumn(ServerLevel level, int x, int z, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double wood = BalanceConfig.RUIN_WOOD_CHANCE.get();
        double moss = BalanceConfig.RUIN_MOSS_CHANCE.get();
        for (int y = box.maxY() + 4; y >= box.minY(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            double roll = (Mth.getSeed(x, y, z) >>> 16 & 0xFFFF) / 65536.0;
            BlockState next = null;
            if (state.is(BlockTags.DOORS) || state.is(BlockTags.BEDS) || state.is(BlockTags.IMPERMEABLE)
                    || state.is(Blocks.GLASS_PANE) || state.is(BlockTags.WOOL_CARPETS) || state.is(Blocks.TORCH)
                    || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN) || state.is(BlockTags.FLOWER_POTS)
                    || state.is(Blocks.BELL) || isWorkstation(state)) {
                next = Blocks.AIR.defaultBlockState();
            } else if (state.is(Blocks.HAY_BLOCK)) {
                next = ModBlocks.ASH.get().defaultBlockState();
            } else if ((state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS)
                    || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.LOGS)) && roll < wood) {
                next = Blocks.AIR.defaultBlockState();
            } else if (state.is(Blocks.COBBLESTONE) && roll < moss) {
                next = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            } else if (state.is(Blocks.STONE_BRICKS) && roll < moss) {
                next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            }
            if (next != null) {
                level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isWorkstation(BlockState state) {
        return state.is(Blocks.LECTERN) || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.CARTOGRAPHY_TABLE)
                || state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.LOOM) || state.is(Blocks.GRINDSTONE)
                || state.is(Blocks.STONECUTTER) || state.is(Blocks.BREWING_STAND) || state.is(Blocks.COMPOSTER)
                || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER);
    }
}
