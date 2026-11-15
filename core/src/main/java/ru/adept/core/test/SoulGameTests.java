package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.config.ManaConfig;
import ru.adept.core.mana.AccumulatorItem;
import ru.adept.core.mana.ArtifactPower;
import ru.adept.core.mana.ManaSystems;
import ru.adept.core.mana.SoulCauldronItem;
import ru.adept.core.mana.SoulCounts;
import ru.adept.core.mana.SoulEntity;
import ru.adept.core.mana.SoulStrength;
import ru.adept.core.progress.WorldInit;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.table.CostList;
import ru.adept.core.table.TableActions;
import ru.adept.core.trader.TraderOffers;
import ru.adept.core.util.TextWrap;

import java.util.List;

/** Автотесты среза 3, часть 1: накопитель, котёл душ, души, ядра душ у торговца, команды мира, перенос текста. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulGameTests {
    private SoulGameTests() {
    }

    @GameTest(template = "empty")
    public static void accumulatorFeedsArtifactsWithoutLoss(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack acc = new ItemStack(ModItems.ACCUMULATOR.get());
        AccumulatorItem.setStored(acc, ArtifactPower.Resource.ELECTRICITY, 100);
        AccumulatorItem.setStored(acc, ArtifactPower.Resource.MANA, 50);
        player.getInventory().add(acc);
        player.getInventory().add(new ItemStack(ModItems.GAUSS_GUN.get()));
        ItemStack held = AccumulatorItem.find(player);
        ManaSystems.distribute(player, held, 20);
        ItemStack gun = player.getInventory().items.stream().filter(s -> s.is(ModItems.GAUSS_GUN.get())).findFirst().orElseThrow();
        int out = ManaConfig.ACCUMULATOR_OUTPUT.get();
        helper.assertTrue(GaussGunItem.charge(gun) == out, "Пушка получила " + GaussGunItem.charge(gun) + " вместо " + out);
        helper.assertTrue(AccumulatorItem.stored(held, ArtifactPower.Resource.ELECTRICITY) == 100 - out, "Потери при отдаче");
        helper.assertTrue(AccumulatorItem.stored(held, ArtifactPower.Resource.MANA) == 50, "Мана ушла в электричество");
        helper.assertTrue(AccumulatorItem.capacity(player) == ManaConfig.ACCUMULATOR_CAPACITY.get(), "Ёмкость без ступени");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(TableActions.ACCUMULATOR_CAPACITY, 1));
        helper.assertTrue(AccumulatorItem.capacity(player) == 2000, "«Ёмкость» I не 2000");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cauldronBurnsSoulsIntoAccumulator(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(ModItems.ACCUMULATOR.get()));
        player.getInventory().add(new ItemStack(ModItems.SOUL_CAULDRON.get()));
        ItemStack acc = AccumulatorItem.find(player);
        ItemStack cauldron = SoulCauldronItem.find(player);
        int furnaceCap = ManaConfig.CAULDRON_FURNACE_SOULS.get();
        int taken = SoulCauldronItem.collect(cauldron, SoulStrength.NORMAL, furnaceCap + 2);
        helper.assertTrue(taken == furnaceCap, "Без сосуда принято " + taken);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.GLASS_SOUL_VESSEL.get()));
        helper.assertTrue(SoulCauldronItem.swapVessel(player, cauldron), "Сосуд не встал в котёл");
        helper.assertTrue(SoulCauldronItem.collect(cauldron, SoulStrength.STRONG, 2) == 2, "Лишние души не ушли в сосуд");
        helper.assertTrue(SoulCounts.total(SoulCauldronItem.vesselSouls(cauldron)) == 2, "В сосуде не 2 души");

        SoulCauldronItem.process(player, cauldron, acc, ManaConfig.SOUL_BURN_TICKS.get());
        int mana = ManaConfig.SOUL_MANA_NORMAL.get();
        int arrived = (int) Math.floor(Math.min(mana, ManaConfig.CAULDRON_TRANSFER.get() * ManaConfig.SOUL_BURN_TICKS.get() / 20)
                * (1.0 - ManaConfig.CAULDRON_TRANSFER_LOSS.get()));
        helper.assertTrue(SoulCounts.total(SoulCauldronItem.furnace(cauldron)) == furnaceCap - 1, "Горнило не сожгло душу");
        helper.assertTrue(AccumulatorItem.stored(acc, ArtifactPower.Resource.MANA) == arrived,
                "В накопитель пришло " + AccumulatorItem.stored(acc, ArtifactPower.Resource.MANA) + " вместо " + arrived);
        helper.assertTrue(TableActions.progress(player).step(SoulCauldronItem.FIRST_SOUL) == 1, "Первая душа не отмечена");

        helper.assertTrue(SoulCauldronItem.feedFromVessel(cauldron) == 1, "Ручная подача из сосуда");
        helper.assertTrue(SoulCauldronItem.manaFor(player, SoulStrength.NORMAL) == mana, "Мана без «Выхода»");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(TableActions.CAULDRON_YIELD, 1));
        helper.assertTrue(SoulCauldronItem.manaFor(player, SoulStrength.STRONG) == 46, "«Выход» I: сильная душа не 46");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulStrengthByMob(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(SoulStrength.of(EntityType.ZOMBIE.create(level)) == SoulStrength.NORMAL, "Зомби не обычная душа");
        helper.assertTrue(SoulStrength.of(ModEntities.HORNED_LEADER.get().create(level)) == SoulStrength.GUARDIAN, "Страж не душа стража");
        helper.assertTrue(SoulStrength.of(ModEntities.HERO.get().create(level)) == SoulStrength.STRONG, "Герой не сильная душа");
        helper.assertTrue(SoulStrength.of(EntityType.IRON_GOLEM.create(level)) == SoulStrength.STRONG, "Голем (100 здоровья) не сильная душа");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void soulsOfOneStrengthMerge(GameTestHelper helper) {
        Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        var level = helper.getLevel();
        level.addFreshEntity(SoulEntity.create(level, pos, SoulStrength.NORMAL, 1, null));
        level.addFreshEntity(SoulEntity.create(level, pos.add(0.3, 0, 0), SoulStrength.NORMAL, 2, null));
        level.addFreshEntity(SoulEntity.create(level, pos.add(0, 0, 0.3), SoulStrength.STRONG, 1, null));
        helper.succeedWhen(() -> {
            List<SoulEntity> souls = level.getEntitiesOfClass(SoulEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(3));
            long normal = souls.stream().filter(s -> s.strength() == SoulStrength.NORMAL).count();
            helper.assertTrue(normal == 1, "Обычные души не слились: " + normal);
            helper.assertTrue(souls.stream().anyMatch(s -> s.strength() == SoulStrength.NORMAL && s.count() == 3), "Сумма не 3");
            helper.assertTrue(souls.stream().anyMatch(s -> s.strength() == SoulStrength.STRONG), "Сильная душа слилась с обычной");
        });
    }

    @GameTest(template = "empty")
    public static void traderTakesSoulCores(GameTestHelper helper) {
        var offers = TraderOffers.roll(RandomSource.create(7));
        MerchantOffer cluster = offers.stream().filter(o -> o.getItemCostA().item().value() != ModItems.SOUL_CORE.get())
                .findFirst().orElseThrow();
        MerchantOffer cores = TraderOffers.coreOffer(cluster);
        helper.assertTrue(cores != null, "Нет варианта за ядра");
        int expected = cluster.getItemCostA().count() * ManaConfig.SOUL_CORES_PER_CLUSTER.get();
        int got = cores.getItemCostA().count() + cores.getItemCostB().map(c -> c.count()).orElse(0);
        helper.assertTrue(got == expected, "Цена в ядрах " + got + " вместо " + expected);
        helper.assertTrue(TraderOffers.currency(cores).equals("soul_core"), "Валюта сделки не ядра душ");
        helper.assertTrue(offers.stream().anyMatch(o -> o.getItemCostA().item().value() == ModItems.SOUL_CORE.get()),
                "В ассортименте нет позиций за ядра");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableBuildsSinglesAndOpensSteps(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        helper.assertTrue(TableActions.upgradeAccumulatorCapacity(player) == TableActions.Result.NOT_CHOSEN, "Ступень без накопителя");
        helper.assertTrue(TableActions.craftAccumulator(player) == TableActions.Result.OK, "Накопитель не собран");
        helper.assertTrue(TableActions.craftCauldron(player) == TableActions.Result.OK, "Котёл не собран");
        helper.assertTrue(!AccumulatorItem.find(player).isEmpty() && !SoulCauldronItem.find(player).isEmpty(), "Предметы не выданы");
        helper.assertTrue(TableActions.upgradeAccumulatorCapacity(player) == TableActions.Result.OK, "«Ёмкость» I");
        helper.assertTrue(TableActions.upgradeCauldronYield(player) == TableActions.Result.OK, "«Выход» I");
        helper.assertTrue(TableActions.upgradeCauldronYield(player) == TableActions.Result.ALREADY_DONE, "«Выход» I дважды");
        for (String price : List.of(ManaConfig.TABLE_ACCUMULATOR.get(), ManaConfig.TABLE_ACCUMULATOR_CAPACITY_1.get(),
                ManaConfig.TABLE_CAULDRON.get(), ManaConfig.TABLE_CAULDRON_YIELD_1.get())) {
            helper.assertTrue(CostList.parse(price).items().size() == price.split(";").length, "Неизвестный предмет в цене: " + price);
        }
        helper.assertTrue(StepTexts.find(TableActions.ACCUMULATOR_CAPACITY, 1).isPresent()
                && StepTexts.find(TableActions.CAULDRON_YIELD, 1).isPresent(), "Нет описаний ступеней");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldCommandsAndTooltipWrap(GameTestHelper helper) {
        List<String> commands = WorldInit.commandsFor(List.of("born_in_chaos_v1|gamerule seasonalEvents false",
                "missing_mod|say no", "say always", " "), id -> id.equals("born_in_chaos_v1"));
        helper.assertTrue(commands.equals(List.of("gamerule seasonalEvents false", "say always")), "Команды мира: " + commands);
        String text = "Командир Ортвин Седой приказывает собрать отряд у старой мельницы и выступить к деревне Нижние Броды до заката";
        List<String> lines = TextWrap.wrap(text, 40);
        helper.assertTrue(lines.size() >= 3 && lines.stream().allMatch(l -> l.length() <= 40), "Перенос: " + lines);
        helper.assertTrue(String.join(" ", lines).equals(text), "Перенос потерял слова");
        helper.assertTrue(TextWrap.wrap("первая\nвторая", 40).size() == 2, "Перенос по \\n");
        helper.succeed();
    }
}
