package ru.adept.core.absorb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.BiomeAbsorbedEvent;
import ru.adept.core.api.event.NodeEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Поглощение биома: после зачистки узла кристалл гаснет и выпадает сгусток узла,
 * ищется область биома, затем чанки лениво превращаются в пустошь — загруженные сразу,
 * остальные при загрузке (порциями, чтобы не было подвисаний).
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class AbsorptionManager {
    public static final ResourceKey<Biome> WASTELAND =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "wasteland"));

    private static BiomeRegionSearch search;
    private static final Deque<Long> CONVERT = new ArrayDeque<>();

    private AbsorptionManager() {
    }

    @SubscribeEvent
    public static void onCleansed(NodeEvent.Cleansed event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        BlockPos node = event.getNode();
        if (level.getBlockState(node).is(ModBlocks.NODE_CRYSTAL.get())) {
            level.removeBlock(node, false);
        }
        ItemStack cluster = new ItemStack(ModItems.cluster(event.getLevel()));
        ItemEntity drop = new ItemEntity(level, node.getX() + 0.5, node.getY() + 0.5, node.getZ() + 0.5, cluster);
        drop.setUnlimitedLifetime();
        level.addFreshEntity(drop);
        start(server, node, event.getLevel());
    }

    /** Поставить поглощение области биома вокруг точки в очередь; {@code nodeLevel} — уровень узла для события. */
    public static void start(MinecraftServer server, BlockPos origin, int nodeLevel) {
        AbsorptionData data = AbsorptionData.get(server);
        BlockPos pos = origin.immutable();
        data.pendingRegions.add(pos);
        data.regionLevels.put(pos, nodeLevel);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        AbsorptionData data = AbsorptionData.get(server);
        if (search == null && !data.pendingRegions.isEmpty()) {
            search = new BiomeRegionSearch(level, data.pendingRegions.get(0),
                    BalanceConfig.ABSORB_MAX_RADIUS.get(), BalanceConfig.ABSORB_MAX_CELLS.get());
        }
        if (search != null) {
            search.step(BalanceConfig.ABSORB_SAMPLES_PER_TICK.get());
            if (search.isDone()) {
                Map<Long, Integer> chunks = search.chunks();
                data.addCells(chunks);
                data.pendingRegions.remove(search.origin());
                int nodeLevel = data.regionLevels.getOrDefault(search.origin(), 0);
                data.regionLevels.remove(search.origin());
                if (nodeLevel <= 0) {
                    nodeLevel = WorldProgress.get(server).nodeTier();
                }
                data.setDirty();
                for (long key : chunks.keySet()) {
                    ChunkPos pos = new ChunkPos(key);
                    if (level.hasChunk(pos.x, pos.z)) {
                        CONVERT.add(key);
                    }
                }
                String biome = search.biome().unwrapKey().map(k -> k.location().toString()).orElse("unknown");
                AdeptEvents.post(new BiomeAbsorbedEvent(server, search.origin(), nodeLevel, biome, chunks.size()));
                AdeptCore.LOG.info("Поглощение биома {}: {} ячеек, {} чанков", biome, search.includedCells(), chunks.size());
                search = null;
            }
        }
        int budget = BalanceConfig.ABSORB_CHUNKS_PER_TICK.get();
        while (budget > 0 && !CONVERT.isEmpty()) {
            long key = CONVERT.poll();
            Integer mask = data.pendingChunks.get(key);
            ChunkPos pos = new ChunkPos(key);
            if (mask == null || !level.hasChunk(pos.x, pos.z)) {
                continue;
            }
            convert(level, level.getChunk(pos.x, pos.z), mask);
            data.pendingChunks.remove(key);
            data.setDirty();
            budget--;
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            long key = event.getChunk().getPos().toLong();
            if (AbsorptionData.get(level.getServer()).pendingChunks.containsKey(key)) {
                CONVERT.add(key);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        search = null;
        CONVERT.clear();
    }

    /** Превратить ячейки чанка по маске в пустошь: биом и поверхность. */
    public static void convert(ServerLevel level, LevelChunk chunk, int mask) {
        Holder<Biome> wasteland = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(WASTELAND);
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            int x = QuartPos.toBlock(qx);
            int z = QuartPos.toBlock(qz);
            return (mask & (1 << BiomeRegionSearch.bit(x, z))) != 0 ? wasteland : chunk.getNoiseBiome(qx, qy, qz);
        }, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                if ((mask & (1 << BiomeRegionSearch.bit(x, z))) != 0) {
                    convertColumn(level, x, z);
                }
            }
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.<ChunkAccess>of(chunk));
    }

    /** Поверхность колонки: растения и листва исчезают, земля и песок становятся пеплом. */
    public static void convertColumn(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockState ash = ModBlocks.ASH.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y > top - 12 && y > level.getMinBuildHeight(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.is(BlockTags.LEAVES) || state.is(Blocks.SNOW)
                    || (state.canBeReplaced() && state.getFluidState().isEmpty())
                    || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.SNOW_BLOCK)
                    || state.is(Blocks.GRAVEL) || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)) {
                level.setBlock(pos, ash, Block.UPDATE_CLIENTS);
            }
            break;
        }
    }
}
