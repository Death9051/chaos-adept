package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.GaussLook.Segment;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.table.CostList;
import ru.adept.core.table.GaussUpgrades;
import ru.adept.core.table.GaussUpgrades.Node;
import ru.adept.core.table.TableActions;

/** Автотесты прокачки гаусс-пушки по сегментам: облик из прогресса, видимость костей, порядок ступеней, цена, вершина. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GaussUpgradeGameTests {
    private GaussUpgradeGameTests() {
    }

    @GameTest(template = "empty")
    public static void gaussLookFollowsProgressAndHidesBones(GameTestHelper helper) {
        ArtifactProgress p = ArtifactProgress.EMPTY
                .withStep(Segment.BARREL.key(0), 3)
                .withStep(Segment.BARREL.key(1), 1)
                .withStep(Segment.BARREL.apexKey(), 1)
                .withStep(Segment.SCOPE.key(1), 2);
        int look = GaussLook.pack(p);
        helper.assertTrue(GaussLook.base(look, Segment.BARREL) == 3 && GaussLook.add(look, Segment.BARREL) == 1
                && GaussLook.apex(look, Segment.BARREL), "Облик ствола не совпал с прогрессом");
        helper.assertTrue(GaussLook.base(look, Segment.SCOPE) == 0 && GaussLook.add(look, Segment.SCOPE) == 2
                && !GaussLook.apex(look, Segment.SCOPE), "Облик прицела не совпал с прогрессом");
        helper.assertTrue(GaussLook.visible(look, "frame") && GaussLook.visible(look, "barrel"), "Рама и кость сегмента видны всегда");
        helper.assertTrue(GaussLook.visible(look, "barrel_base3") && !GaussLook.visible(look, "barrel_base0"), "Основа ствола: виден один вариант");
        helper.assertTrue(GaussLook.visible(look, "barrel_add1") && !GaussLook.visible(look, "barrel_add2"), "Детали ствола: до текущей ступени");
        helper.assertTrue(GaussLook.visible(look, "barrel_apex") && !GaussLook.visible(look, "coils_apex"), "Вершина видна только открытая");
        helper.assertTrue(GaussLook.visible(look, "coils_base0") && !GaussLook.visible(look, "coils_add1"), "Нетронутый сегмент — прототип без деталей");
        helper.assertTrue(GaussLook.visible(look, "scope_add2") && !GaussLook.visible(look, "scope_add3"), "Детали прицела");
        helper.assertTrue(GaussLook.uniform(3, 3, true) == GaussLook.pack(full()), "Полная прокачка и uniform(3,3,да) расходятся");
        ItemStack gun = new ItemStack(ModItems.GAUSS_GUN.get());
        GaussGunItem.setCharge(gun, 100);
        GaussGunItem.setLook(gun, look);
        helper.assertTrue(GaussGunItem.look(gun) == look && GaussGunItem.charge(gun) == 100, "Облик в данных предмета");
        helper.succeed();
    }

    private static ArtifactProgress full() {
        ArtifactProgress p = ArtifactProgress.EMPTY;
        for (Segment s : GaussLook.SEGMENTS) {
            p = p.withStep(s.key(0), 3).withStep(s.key(1), 3).withStep(s.apexKey(), 1);
        }
        return p;
    }

    @GameTest(template = "empty")
    public static void gaussButtonsCoverAllNodes(GameTestHelper helper) {
        int count = GaussLook.SEGMENTS.length * GaussUpgrades.NODES_PER_SEGMENT;
        for (int id = GaussUpgrades.BTN_FIRST; id < GaussUpgrades.BTN_FIRST + count; id++) {
            helper.assertTrue(GaussUpgrades.isButton(id) && GaussUpgrades.node(id).button() == id, "Кнопка " + id + " не возвращается в себя");
            helper.assertTrue(!GaussUpgrades.cost(GaussUpgrades.node(id)).items().isEmpty(), "У узла " + id + " пустая цена");
        }
        helper.assertTrue(!GaussUpgrades.isButton(GaussUpgrades.BTN_FIRST - 1) && !GaussUpgrades.isButton(GaussUpgrades.BTN_FIRST + count),
                "Границы кнопок");
        helper.assertTrue(new Node(Segment.BARREL, 1, 1).key().equals(TableActions.GAUSS_TEMPO), "Ключ «Темпа» разошёлся со старым");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gaussStepsGoInOrderAndCostComponents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Node first = new Node(Segment.COILS, 0, 1);
        Node second = new Node(Segment.COILS, 0, 2);
        helper.assertTrue(GaussUpgrades.unlock(player, first) == TableActions.Result.NOT_CHOSEN, "Ступень без собранной пушки");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(TableActions.GAUSS_BUILT, 1));
        helper.assertTrue(GaussUpgrades.unlock(player, second) == TableActions.Result.NOT_AVAILABLE, "Ступень II раньше I");
        helper.assertTrue(GaussUpgrades.unlock(player, first) == TableActions.Result.NOT_ENOUGH, "Ступень без компонентов");
        CostList cost = GaussUpgrades.cost(first);
        int total = 0;
        for (ItemStack stack : cost.items()) {
            total += stack.getCount();
            player.getInventory().add(stack.copy());
        }
        helper.assertTrue(total == 4, "Ступень I стоит " + total + " компонентов вместо 4");
        helper.assertTrue(GaussUpgrades.unlock(player, first) == TableActions.Result.OK, "Ступень I с компонентами");
        helper.assertTrue(TableActions.progress(player).step(Segment.COILS.key(0)) == 1, "Ступень не записана");
        helper.assertTrue(player.getInventory().isEmpty(), "Компоненты не списаны");
        helper.assertTrue(GaussUpgrades.unlock(player, first) == TableActions.Result.ALREADY_DONE, "Ступень открыта дважды");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gaussApexNeedsOneFullPath(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Node apex = GaussUpgrades.apex(Segment.SCOPE);
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player)
                .withStep(TableActions.GAUSS_BUILT, 1).withStep(Segment.SCOPE.key(0), 2).withStep(Segment.SCOPE.key(1), 2));
        helper.assertTrue(GaussUpgrades.unlock(player, apex) == TableActions.Result.NOT_AVAILABLE, "Вершина без закрытого пути");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(Segment.SCOPE.key(1), 3));
        boolean cluster = false;
        for (ItemStack stack : GaussUpgrades.cost(apex).items()) {
            cluster |= stack.is(ModItems.cluster(3));
            player.getInventory().add(stack.copy());
        }
        helper.assertTrue(cluster, "В цене вершины нет сгустка узла");
        helper.assertTrue(GaussUpgrades.unlock(player, apex) == TableActions.Result.OK, "Вершина с закрытым путём деталей");
        helper.assertTrue(GaussLook.apex(GaussLook.pack(TableActions.progress(player)), Segment.SCOPE), "Вершина не попала в облик");
        helper.succeed();
    }
}
