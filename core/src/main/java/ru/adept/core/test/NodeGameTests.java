package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.node.NodeGrid;
import ru.adept.core.node.NodeRecord;
import ru.adept.core.node.NodeRegistry;
import ru.adept.core.node.ResonatorItem;

import java.util.Optional;

/** Автотесты узлов: уровень по расстоянию, сетка (детерминизм, один на биом), реестр, резонатор. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NodeGameTests {
    private NodeGameTests() {
    }

    @GameTest(template = "empty")
    public static void levelGrowsWithDistance(GameTestHelper helper) {
        int step = BalanceConfig.NODE_BLOCKS_PER_LEVEL.get();
        helper.assertTrue(NodeGrid.levelForDistance(0) == 1, "У точки появления уровень не 1");
        helper.assertTrue(NodeGrid.levelForDistance(step + 1) == 2, "Уровень не растёт с расстоянием");
        helper.assertTrue(NodeGrid.levelForDistance(step * 1000.0) == BalanceConfig.NODE_MAX_LEVEL.get(), "Уровень не ограничен сверху");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void gridIsDeterministicAndOnePerBiome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int base = 50; // далеко от точки появления
        int valid = 0;
        for (int x = base; x < base + 6; x++) {
            for (int z = base; z < base + 6; z++) {
                Optional<NodeGrid.Candidate> a = NodeGrid.candidate(level, x, z);
                Optional<NodeGrid.Candidate> b = NodeGrid.candidate(level, x, z);
                helper.assertTrue(a.map(NodeGrid.Candidate::column).equals(b.map(NodeGrid.Candidate::column)),
                        "Кандидат ячейки не детерминирован");
                if (a.isEmpty()) {
                    continue;
                }
                valid++;
                // Соседняя ячейка с тем же биомом не может тоже держать узел.
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        Optional<NodeGrid.Candidate> n = NodeGrid.candidate(level, x + dx, z + dz);
                        helper.assertTrue(n.isEmpty() || !n.get().biome().is(a.get().biome()),
                                "Два узла в соседних ячейках одного биома");
                    }
                }
            }
        }
        helper.assertTrue(valid > 0, "В области 6×6 ячеек нет ни одного узла");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registryStoresNodes(GameTestHelper helper) {
        NodeRegistry registry = NodeRegistry.get(helper.getLevel().getServer());
        BlockPos pos = new BlockPos(123456, 70, 654321);
        registry.add(new NodeRecord(pos, 2, "minecraft:plains", NodeRecord.State.DORMANT), 999_999L);
        helper.assertTrue(registry.at(pos).map(NodeRecord::level).orElse(0) == 2, "Узел не найден по позиции");
        helper.assertTrue(registry.isCellPlaced(999_999L), "Ячейка не отмечена");
        registry.update(registry.at(pos).get().withState(NodeRecord.State.CLEANSED));
        helper.assertTrue(registry.at(pos).get().state() == NodeRecord.State.CLEANSED, "Состояние не обновилось");
        registry.remove(pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void resonatorDirections(GameTestHelper helper) {
        helper.assertTrue("east".equals(ResonatorItem.direction(10, 0)), "Восток определён неверно");
        helper.assertTrue("south".equals(ResonatorItem.direction(0, 10)), "Юг определён неверно");
        helper.assertTrue("north".equals(ResonatorItem.direction(0, -10)), "Север определён неверно");
        helper.assertTrue("west".equals(ResonatorItem.direction(-10, 0)), "Запад определён неверно");
        helper.assertTrue("northeast".equals(ResonatorItem.direction(10, -10)), "Северо-восток определён неверно");
        helper.succeed();
    }
}
