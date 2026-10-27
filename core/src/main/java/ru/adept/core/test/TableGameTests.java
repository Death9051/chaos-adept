package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;
import ru.adept.core.table.CostList;
import ru.adept.core.table.TableActions;
import ru.adept.core.table.TableActions.Result;

/** Автотесты стола артефактов: выбор пары, грубые компоненты, прототип клинка, ступень. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TableGameTests {
    private TableGameTests() {
    }

    @GameTest(template = "empty")
    public static void pairChoiceIsExclusive(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(TableActions.choosePair(player, TableActions.HAMMER) == Result.OK, "Первый выбор не прошёл");
        helper.assertTrue(TableActions.choosePair(player, TableActions.BLADE) == Result.ALREADY_CHOSEN, "Пару можно выбрать дважды");
        helper.assertTrue(TableActions.craftBladePrototype(player) == Result.NOT_CHOSEN, "Клинок собирается при выборе молота");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crudeComponentConsumesCost(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(TableActions.craftCrude(player, 0) == Result.NOT_ENOUGH, "Компонент собрался без материалов");
        give(player, CostList.parse(BalanceConfig.TABLE_CRUDE_WIRING.get()));
        helper.assertTrue(TableActions.craftCrude(player, 0) == Result.OK, "Грубая проводка не собралась");
        helper.assertTrue(player.getInventory().countItem(ModItems.CRUDE_WIRING.get()) == 1, "Проводка не выдана");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 0, "Цена не списана");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bladePrototypeAndEdge(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TableActions.choosePair(player, TableActions.BLADE);
        helper.assertTrue(TableActions.upgradeBladeEdge(player) == Result.NOT_CHOSEN, "Ступень открылась без прототипа");
        give(player, CostList.parse(BalanceConfig.TABLE_BLADE_PROTOTYPE.get()));
        helper.assertTrue(TableActions.craftBladePrototype(player) == Result.OK, "Прототип не собрался");
        helper.assertTrue(player.getInventory().countItem(ModItems.ENERGY_BLADE.get()) == 1, "Клинок не выдан");
        helper.assertTrue(TableActions.upgradeBladeEdge(player) == Result.NOT_ENOUGH, "Ступень открылась без цены");
        give(player, CostList.parse(BalanceConfig.TABLE_BLADE_EDGE_1.get()));
        helper.assertTrue(TableActions.upgradeBladeEdge(player) == Result.OK, "Ступень не открылась");
        helper.assertTrue(TableActions.progress(player).step(TableActions.BLADE_EDGE) == 1, "Ступень не записана");
        helper.assertTrue(TableActions.upgradeBladeEdge(player) == Result.ALREADY_DONE, "Ступень открылась дважды");
        helper.succeed();
    }

    private static void give(Player player, CostList cost) {
        for (ItemStack stack : cost.items()) {
            player.getInventory().add(stack.copy());
        }
    }
}
