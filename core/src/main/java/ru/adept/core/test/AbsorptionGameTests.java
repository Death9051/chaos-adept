package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.absorb.AbsorptionManager;
import ru.adept.core.absorb.BiomeRegionSearch;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;

/** Автотесты поглощения: биом пустоши, поиск области, превращение поверхности, сгустки. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AbsorptionGameTests {
    private AbsorptionGameTests() {
    }

    @GameTest(template = "empty")
    public static void wastelandBiomeRegistered(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolder(AbsorptionManager.WASTELAND).isPresent(), "Биом пустоши не зарегистрирован");
        helper.assertTrue(ModItems.cluster(1) != ModItems.cluster(5), "Сгустки уровней не различаются");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void regionSearchIsBounded(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BiomeRegionSearch search = new BiomeRegionSearch(helper.getLevel(), origin, 64, 10_000);
        int guard = 0;
        while (!search.isDone() && guard++ < 1000) {
            search.step(500);
        }
        helper.assertTrue(search.isDone(), "Поиск области не закончился");
        helper.assertTrue(search.includedCells() > 0, "Область пустая");
        // Квадрат ±64 блока — не больше (128/8 + 1)^2 ячеек.
        helper.assertTrue(search.includedCells() <= 17 * 17, "Область вышла за предел радиуса: " + search.includedCells());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void surfaceTurnsToAsh(GameTestHelper helper) {
        BlockPos ground = helper.absolutePos(new BlockPos(0, 1, 0));
        // Колонка вне площадки теста могла бы задеть соседей — берём площадку теста целиком.
        helper.getLevel().setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(ground.above(), Blocks.POPPY.defaultBlockState());
        AbsorptionManager.convertColumn(helper.getLevel(), ground.getX(), ground.getZ());
        helper.assertTrue(helper.getLevel().getBlockState(ground.above()).isAir(), "Цветок не исчез");
        helper.assertTrue(helper.getLevel().getBlockState(ground).is(ModBlocks.ASH.get()), "Трава не стала пеплом");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cellBitsCoverChunk(GameTestHelper helper) {
        helper.assertTrue(BiomeRegionSearch.bit(0, 0) == 0 && BiomeRegionSearch.bit(8, 0) == 1
                && BiomeRegionSearch.bit(0, 8) == 2 && BiomeRegionSearch.bit(15, 15) == 3
                && BiomeRegionSearch.bit(-1, -1) == 3, "Биты ячеек чанка посчитаны неверно");
        helper.succeed();
    }
}
