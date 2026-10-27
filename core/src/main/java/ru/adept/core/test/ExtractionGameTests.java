package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.camp.ExtractionMath;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.mob.Possession;
import ru.adept.core.registry.ModEntities;

/** Автотесты выкачки: пороги волн, откат при смерти, состав волн, мобы узла, одержимость. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtractionGameTests {
    private ExtractionGameTests() {
    }

    @GameTest(template = "empty")
    public static void waveThresholdsAndRollback(GameTestHelper helper) {
        double total = BalanceConfig.extractionTicks(1);
        int waves = BalanceConfig.waveCount(1);
        helper.assertTrue(waves == 4, "На первом уровне не 4 волны: " + waves);
        helper.assertTrue(ExtractionMath.threshold(total, waves, waves) == total, "Последняя волна не на 100%");
        helper.assertTrue(ExtractionMath.threshold(total, 1, waves) == total / 4, "Первая волна не на 25%");
        helper.assertTrue(ExtractionMath.rollback(total * 0.5, total, 10) == total * 0.4, "Откат не 10%");
        helper.assertTrue(ExtractionMath.rollback(total * 0.05, total, 10) == 0.0, "Откат уходит ниже нуля");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waveCompositionGrows(GameTestHelper helper) {
        ExtractionMath.Composition first = ExtractionMath.composition(1, 4, 4, 1, 1, 3, 2, 2, 2, 1);
        ExtractionMath.Composition last = ExtractionMath.composition(4, 4, 4, 1, 1, 3, 2, 2, 2, 1);
        helper.assertTrue(first.possessed() == 4 && first.hornets() == 3 && first.wisps() == 0 && !first.guardian(), "Первая волна собрана неверно");
        helper.assertTrue(last.possessed() == 7 && last.guardian() && last.spirits() == 0, "Финальная волна собрана неверно");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nodeMobsSpawn(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        for (EntityType<? extends Mob> type : java.util.List.of(ModEntities.HORNET.get(), ModEntities.WISP.get(),
                ModEntities.EMBER_SPIRIT.get(), ModEntities.HORNED_LEADER.get())) {
            Mob mob = helper.spawn(type, pos);
            helper.assertTrue(mob.isAlive() && mob.getMaxHealth() > 0, "Моб не появился: " + type);
            mob.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void possessionBuffsAnimal(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(0, 2, 0));
        float before = cow.getMaxHealth();
        Possession.apply(cow);
        helper.assertTrue(Possession.isPossessed(cow), "Метка одержимости не поставлена");
        helper.assertTrue(cow.getMaxHealth() > before, "Здоровье одержимого не выросло");
        cow.discard();
        helper.succeed();
    }
}
