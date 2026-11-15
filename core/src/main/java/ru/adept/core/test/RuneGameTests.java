package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.HeavyArmorItem;
import ru.adept.core.artifact.HeavyArmorSystem;
import ru.adept.core.artifact.RailGunItem;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.CarverBlockEntity;
import ru.adept.core.base.CasterBlockEntity;
import ru.adept.core.base.Molten;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.config.RuneConfig;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.rune.Rune;
import ru.adept.core.rune.RuneMaterial;
import ru.adept.core.rune.RuneSet;
import ru.adept.core.rune.RuneWards;
import ru.adept.core.rune.RuneWord;
import ru.adept.core.rune.Runes;
import ru.adept.core.table.CostList;
import ru.adept.core.table.TableActions;

import java.util.List;

/** Автотесты среза 3, часть 3: руны и слова, высекатель, стирание при подборе, обереги пластин, рельса и болты. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RuneGameTests {
    private RuneGameTests() {
    }

    @GameTest(template = "empty")
    public static void runeWordsMaterialsAndEncoding(GameTestHelper helper) {
        helper.assertTrue(RuneMaterial.all().size() == 10, "Материалов гравировки не 10: " + RuneMaterial.all().size());
        for (RuneWord w : RuneWord.values()) {
            helper.assertTrue(new RuneSet(List.of(w.first, w.second), "iron").word() == w, "Слово " + w + " не находится");
            helper.assertTrue(RuneWord.of(w.second, w.first) == null || RuneWord.of(w.second, w.first) != w, "Порядок рун не важен для " + w);
        }
        RuneSet set = new RuneSet(List.of(Rune.CURRENT, Rune.ECHO), "gold");
        helper.assertTrue(RuneSet.decode(set.encode()).equals(set), "Кодирование гравировки: " + set.encode());
        RuneSet same = new RuneSet(List.of(Rune.FROST, Rune.FROST), RuneMaterial.RED_GOLD);
        double redGold = RuneMaterial.byId(RuneMaterial.RED_GOLD).orElseThrow().multiplier();
        helper.assertTrue(Math.abs(same.multiplier() - (redGold + RuneConfig.MOD_RESONANCE.get())) < 1e-9, "«Созвучие» красного золота");
        ItemStack needles = new ItemStack(Molten.IRON.needle(), 8);
        helper.assertTrue(Runes.engravable(needles) && Runes.kind(needles).equals("needle"), "Иглы не принимают руны");
        set.applyTo(needles);
        helper.assertTrue(!Runes.engravable(needles) && RuneSet.of(needles).equals(set), "Гравировка не записалась");
        helper.assertTrue(!Runes.engravable(new ItemStack(Items.IRON_INGOT)), "Руны на слитке");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void carverEngravesOneRuneThenTwoWithPrecision(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CARVER.get());
        CarverBlockEntity carver = (CarverBlockEntity) helper.getBlockEntity(pos);
        BaseData base = BaseData.get(helper.getLevel().getServer());
        base.set(BaseData.capacity(), BaseData.capacity());
        base.setMachineStep(CarverBlockEntity.PRECISION, 0);
        carver.setRunes(Rune.CURRENT, Rune.ECHO);
        carver.items().setItem(CarverBlockEntity.SLOT_INPUT, new ItemStack(Molten.IRON.needle(), 16));
        int perMaterial = RuneConfig.CARVER_NEEDLES_PER_MATERIAL.get();
        int need = (int) Math.ceil(16.0 / perMaterial);
        carver.items().setItem(CarverBlockEntity.SLOT_MATERIAL, new ItemStack(Items.GOLD_INGOT, need + 1));
        int mana = base.mana();
        helper.assertTrue(carver.finish(), "Гравировка не прошла");
        ItemStack out = carver.items().getItem(CarverBlockEntity.SLOT_OUTPUT);
        RuneSet first = RuneSet.of(out);
        helper.assertTrue(out.getCount() == 16 && first.runes().equals(List.of(Rune.CURRENT)) && first.material().equals("gold"),
                "Без «Точности» должна быть одна руна: " + first);
        helper.assertTrue(carver.items().getItem(CarverBlockEntity.SLOT_MATERIAL).getCount() == 1, "Материал не по числу игл");
        helper.assertTrue(base.mana() == mana - RuneConfig.CARVER_MANA.get() * 16, "Мана базы за гравировку");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Runes.onTaken(player, out);
        helper.assertTrue(!Runes.fresh(out) && player.getData(ModRegistries.ARTIFACT_PROGRESS).step(Runes.FIRST_ENGRAVE) == 1,
                "Первая гравировка не отмечена");
        carver.items().setItem(CarverBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        base.setMachineStep(CarverBlockEntity.PRECISION, 1);
        carver.items().setItem(CarverBlockEntity.SLOT_INPUT, new ItemStack(Molten.IRON.bolt(), 2));
        carver.items().setItem(CarverBlockEntity.SLOT_MATERIAL, new ItemStack(Items.IRON_INGOT, 2));
        helper.assertTrue(carver.finish(), "Гравировка болтов не прошла");
        RuneSet two = RuneSet.of(carver.items().getItem(CarverBlockEntity.SLOT_OUTPUT));
        helper.assertTrue(two.runes().size() == 2 && two.word() == RuneWord.STORM, "С «Точностью» I две руны и слово «Гроза»: " + two);
        helper.assertTrue(!carver.finish(), "Гравировка без изделий");
        base.setMachineStep(CarverBlockEntity.PRECISION, 0);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boltEngravingErasesOnPickupByChance(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        double keep = RuneMaterial.byId("iron").orElseThrow().keep();
        for (long seed = 0; seed < 8; seed++) {
            ItemStack bolt = Runes.engraved(new ItemStack(Molten.IRON.bolt()), new RuneSet(List.of(Rune.FLAME), "iron"));
            RailGunItem.drop(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)), bolt).discard();
            var tag = RailGunItem.data(bolt);
            tag.putBoolean(RailGunItem.LANDED, true);
            bolt.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            boolean kept = RandomSource.create(seed).nextDouble() < keep;
            boolean erased = Runes.pickUpLanded(player, bolt, RandomSource.create(seed));
            helper.assertTrue(erased == !kept, "Стирание не по шансу материала");
            helper.assertTrue(!RailGunItem.data(bolt).getBoolean(RailGunItem.LANDED), "Отметка «упал» осталась");
            helper.assertTrue(RuneSet.of(bolt).isEmpty() == erased, "Гравировка после броска");
        }
        ItemStack plain = new ItemStack(Molten.IRON.bolt());
        helper.assertTrue(!Runes.pickUpLanded(player, plain, RandomSource.create(1)), "Бросок у обычного болта");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plateWardsAndArmorWords(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chest = new ItemStack(ModItems.HEAVY_CHESTPLATE.get());
        HeavyArmorItem armor = ModItems.HEAVY_CHESTPLATE.get();
        armor.installPlate(chest, Runes.engraved(new ItemStack(Molten.IRON.plate()), new RuneSet(List.of(Rune.ECHO), "iron")));
        armor.installPlate(chest, Runes.engraved(new ItemStack(Molten.IRON.plate()), new RuneSet(List.of(Rune.FLAME, Rune.VOID), "iron")));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        helper.assertTrue(RuneWards.words(player).contains(RuneWord.ASH), "Слово «Закалка» на пластине не видно");
        helper.assertTrue(RuneWards.strength(player, Rune.ECHO) > 0, "Оберег Эхо не виден");
        HeavyArmorSystem.absorbDamage(player, 200f);
        var torso = HeavyArmorItem.plate(chest, 0);
        double expected = BalanceConfig.PLATE_IRON.get() * RuneConfig.WARD_ECHO_RESTORE.get();
        helper.assertTrue(torso.getBoolean("echoed") && Math.abs(torso.getDouble("durability") - expected) < 1e-3,
                "Эхо не подняло пластину: " + torso.getDouble("durability"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void railFiresBoltsAndPierceHurts(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModItems.RAIL_GUN.get());
        RailGunItem.setCharge(gun, RailGunItem.capacity());
        player.getInventory().add(new ItemStack(Molten.IRON.bolt(), 3));
        helper.assertTrue(RailGunItem.fire(helper.getLevel(), player, gun), "Выстрел не прошёл");
        helper.assertTrue(RailGunItem.charge(gun) == RailGunItem.capacity() - RuneConfig.RAIL_SHOT_COST.get(), "Расход заряда");
        helper.assertTrue(player.getInventory().countItem(Molten.IRON.bolt()) == 2, "Болт не ушёл");
        RailGunItem.setCharge(gun, 0);
        helper.assertTrue(!RailGunItem.fire(helper.getLevel(), player, gun), "Выстрел без электричества");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        float before = zombie.getHealth();
        RailGunItem.hurt(helper.getLevel(), player, zombie, 6.0, RuneConfig.PIERCE_ARMOR.get());
        helper.assertTrue(zombie.getHealth() < before, "Урон рельсы не прошёл");
        helper.assertTrue(RailGunItem.targets(player, Molten.IRON) == RuneConfig.RAIL_PIERCE.get(), "Целей без «Пробоя»");
        helper.assertTrue(RailGunItem.targets(player, Molten.STEEL) == RuneConfig.RAIL_PIERCE.get() + 1, "Сталь пробивает на одну больше");
        helper.setBlock(new BlockPos(1, 1, 3), ModBlocks.CASTER.get());
        CasterBlockEntity caster = (CasterBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 3));
        caster.setForm(CasterBlockEntity.Form.BOLT.ordinal());
        helper.assertTrue(CasterBlockEntity.product(CasterBlockEntity.Form.BOLT, Molten.STEEL).is(Molten.STEEL.bolt()), "Литейная не льёт болты");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableBuildsRailPierceAndCarverPrecision(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        helper.assertTrue(TableActions.craftRailPrototype(player) == TableActions.Result.NOT_CHOSEN, "Рельса без выбора пары");
        TableActions.choosePair(player, TableActions.RANGED, TableActions.RAIL);
        helper.assertTrue(TableActions.craftRailPrototype(player) == TableActions.Result.OK, "Прототип рельсы не собран");
        helper.assertTrue(TableActions.upgradeRailPierce(player) == TableActions.Result.OK, "«Пробой» I");
        helper.assertTrue(RailGunItem.targets(player, Molten.IRON) == RuneConfig.RAIL_PIERCE.get() + RuneConfig.PIERCE_TARGETS.get(),
                "Целей после «Пробоя» I");
        BaseData base = BaseData.get(helper.getLevel().getServer());
        base.setMachineStep(CarverBlockEntity.PRECISION, 0);
        helper.assertTrue(TableActions.upgradeCarverPrecision(player) == TableActions.Result.OK, "«Точность» I");
        helper.assertTrue(base.machineStep(CarverBlockEntity.PRECISION) == 1
                && TableActions.upgradeCarverPrecision(player) == TableActions.Result.ALREADY_DONE, "«Точность» I дважды");
        base.setMachineStep(CarverBlockEntity.PRECISION, 0);
        for (String price : List.of(RuneConfig.TABLE_RAIL_PROTOTYPE.get(), RuneConfig.TABLE_RAIL_PIERCE_1.get(),
                RuneConfig.TABLE_CARVER_PRECISION_1.get())) {
            helper.assertTrue(CostList.parse(price).items().size() == price.split(";").length, "Неизвестный предмет в цене: " + price);
        }
        helper.assertTrue(StepTexts.find(TableActions.RAIL_PIERCE, 1).isPresent()
                && StepTexts.find(TableActions.CARVER_PRECISION, 1).isPresent(), "Нет описаний ступеней");
        helper.succeed();
    }
}
