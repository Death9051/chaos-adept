package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.node.NodeGrid;
import ru.adept.core.progress.TierProgression;
import ru.adept.core.table.TableActions;

/** Автотесты среза 2, основа: вехи уровней узлов, сгустки по уровням, описания ступеней. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierGameTests {
    private TierGameTests() {
    }

    @GameTest(template = "empty")
    public static void tierAdvancesAfterCleansedNodes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        int oldTier = progress.nodeTier();
        progress.setNodeTier(server, 2);
        progress.setNodeTier(server, 1);
        int need = BalanceConfig.TIER_NODES_TO_ADVANCE.get();
        for (int i = 0; i < need - 1; i++) {
            progress.recordCleansed(server, 1);
        }
        helper.assertTrue(progress.nodeTier() == 1, "Уровень сменился раньше вехи");
        progress.recordCleansed(server, 1);
        helper.assertTrue(progress.nodeTier() == 2, "Уровень не сменился после вехи");
        helper.assertTrue(progress.cleansedAtTier() == 0, "Счётчик вехи не сброшен");
        progress.recordCleansed(server, 1);
        helper.assertTrue(progress.cleansedAtTier() == 0, "Узел ниже уровня двигает веху");
        helper.assertTrue(NodeGrid.withTierFloor(helper.getLevel(), 1) == (BalanceConfig.TIER_LEVEL_FLOOR.get() ? 2 : 1),
                "Новые узлы ниже уровня мира");
        progress.setNodeTier(server, oldTier);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clustersAndDifficultyByLevel(GameTestHelper helper) {
        helper.assertTrue(BalanceConfig.clustersFor(1) == 1 && BalanceConfig.clustersFor(3) == 2
                && BalanceConfig.clustersFor(5) == 4 && BalanceConfig.clustersFor(9) == 4, "Сгустки по уровням не по конфигу");
        helper.assertTrue(TierProgression.difficultyFor(1) == 0, "Сложность первого уровня не нулевая");
        helper.assertTrue(TierProgression.commandFor(3).contains(Integer.toString(2 * BalanceConfig.IMPROVED_MOBS_PER_TIER.get())),
                "Команда Improved Mobs без сложности");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyStepHasDescription(GameTestHelper helper) {
        helper.assertTrue(StepTexts.find(TableActions.BLADE_EDGE, 1).isPresent(), "Нет описания «Кромки» I");
        for (StepTexts.Step step : StepTexts.all()) {
            String desc = step.description().getString();
            helper.assertTrue(!desc.equals(step.langBase() + ".desc") && !desc.contains("%s") && !desc.contains("$s"),
                    "Описание ступени не заполнено: " + step.langBase());
        }
        helper.assertTrue(StepTexts.format(9.0).equals("9") && StepTexts.format(1.5).equals("1,5"), "Формат чисел");
        helper.succeed();
    }
}
