package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.LightArmorItem;
import ru.adept.core.artifact.LightArmorSystem;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.CutterBlockEntity;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.focus.FocusCrystal;
import ru.adept.core.focus.FocusCut;
import ru.adept.core.focus.FocusItem;
import ru.adept.core.focus.Focuses;
import ru.adept.core.mana.ArtifactPower;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;
import ru.adept.core.table.CostList;
import ru.adept.core.table.TableActions;

import java.util.List;

/** Автотесты среза 3, часть 2: фокусы (износ, трещина, поломка), ограночная машина, щит лёгкой брони, стол. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FocusGameTests {
    private FocusGameTests() {
    }

    private static FocusCrystal crystal(String id) {
        return FocusCrystal.byId(id).orElseThrow();
    }

    private static ItemStack wearChest(Player player) {
        ItemStack chest = new ItemStack(ModItems.LIGHT_CHESTPLATE.get());
        LightArmorItem.setMana(chest, FocusConfig.LIGHT_MANA_CAPACITY.get());
        LightArmorItem.setShield(chest, FocusConfig.LIGHT_SHIELD.get(), FocusConfig.LIGHT_SHIELD.get());
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        return LightArmorSystem.chest(player);
    }

    @GameTest(template = "empty")
    public static void focusWearsThenCracksOrBreaks(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack focus = FocusItem.create(FocusCut.KNOCKBACK, crystal(FocusCrystal.QUARTZ));
        helper.assertTrue(FocusItem.durability(focus) == crystal(FocusCrystal.QUARTZ).durability(), "Прочность нового фокуса не по кристаллу");
        helper.assertTrue(Focuses.wear(player, focus, RandomSource.create(1)) == Focuses.Wear.WORN
                && FocusItem.durability(focus) == FocusItem.max(focus) - 1, "Срабатывание не изнашивает фокус");
        for (long seed = 0; seed < 8; seed++) {
            ItemStack f = FocusItem.create(FocusCut.SLOW, crystal(FocusCrystal.QUARTZ));
            FocusItem.setDurability(f, 1);
            boolean crack = RandomSource.create(seed).nextDouble() < FocusConfig.FOCUS_CRACK_CHANCE.get();
            Focuses.Wear wear = Focuses.wear(player, f, RandomSource.create(seed));
            helper.assertTrue(wear == (crack ? Focuses.Wear.CRACKED : Focuses.Wear.BROKEN), "Исход износа не по шансу: " + wear);
            helper.assertTrue(!Focuses.active(f), "Изношенный фокус ещё работает");
            if (crack) {
                helper.assertTrue(FocusItem.cracked(f), "Нет трещины");
            }
        }
        ItemStack amethyst = FocusItem.create(FocusCut.FLASH, crystal(FocusCrystal.AMETHYST));
        FocusItem.setDurability(amethyst, 1);
        Focuses.grow(amethyst, FocusConfig.FOCUS_GROWTH_SECONDS.get() * 2);
        helper.assertTrue(FocusItem.durability(amethyst) == 3, "«Рост» аметиста: " + FocusItem.durability(amethyst));
        helper.assertTrue(Focuses.manaFactor(FocusItem.create(FocusCut.FLASH, crystal(FocusCrystal.LAPIS))) == FocusConfig.FOCUS_ECONOMY.get(),
                "«Экономия» лазурита");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cutterCutsAndRepairs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CUTTER.get());
        CutterBlockEntity cutter = (CutterBlockEntity) helper.getBlockEntity(pos);
        BaseData base = BaseData.get(helper.getLevel().getServer());
        base.set(BaseData.capacity(), BaseData.capacity());
        cutter.setCut(FocusCut.RECHARGE.ordinal());
        cutter.items().setItem(0, new ItemStack(Items.DIAMOND, FocusConfig.CUTTER_CRYSTALS.get() - 1));
        helper.assertTrue(cutter.job() == CutterBlockEntity.Job.NONE && !cutter.finish(), "Огранка без нужного числа кристаллов");
        cutter.items().setItem(0, new ItemStack(Items.DIAMOND, FocusConfig.CUTTER_CRYSTALS.get()));
        helper.assertTrue(cutter.finish(), "Фокус не огранён");
        ItemStack focus = cutter.items().getItem(1);
        helper.assertTrue(FocusItem.is(focus) && FocusItem.cut(focus) == FocusCut.RECHARGE
                && FocusCrystal.DIAMOND.equals(FocusItem.crystalId(focus)) && FocusItem.fresh(focus), "Не тот фокус");
        helper.assertTrue(cutter.items().getItem(0).isEmpty(), "Кристаллы не потрачены");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Focuses.onTaken(player, focus);
        helper.assertTrue(!FocusItem.fresh(focus) && player.getData(ru.adept.core.registry.ModRegistries.ARTIFACT_PROGRESS)
                .step(Focuses.FIRST_CUT) == 1, "Первый фокус не отмечен");
        cutter.items().setItem(1, ItemStack.EMPTY);
        FocusItem.setCracked(focus, true);
        cutter.items().setItem(0, focus);
        int mana = base.mana();
        helper.assertTrue(cutter.job() == CutterBlockEntity.Job.REPAIR && cutter.finish(), "Треснувший фокус не починен");
        ItemStack repaired = cutter.items().getItem(1);
        int restored = Math.min(FocusItem.max(repaired), Math.max(1, (int) Math.ceil(FocusItem.max(repaired) * FocusConfig.CUTTER_REPAIR_RESTORE.get())));
        helper.assertTrue(!FocusItem.cracked(repaired) && FocusItem.durability(repaired) == restored, "Починка: " + FocusItem.durability(repaired));
        helper.assertTrue(base.mana() == mana - FocusConfig.CUTTER_REPAIR_MANA.get(), "Починка не взяла ману базы");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightShieldTakesDamageFirst(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chest = wearChest(player);
        int cap = FocusConfig.LIGHT_SHIELD.get();
        helper.assertTrue(LightArmorSystem.absorb(player, chest, 4, null) == 0f
                && Math.abs(LightArmorItem.shield(chest) - (cap - 4)) < 1e-6, "Щит не принял урон");
        float passed = LightArmorSystem.absorb(player, chest, cap, null);
        helper.assertTrue(Math.abs(passed - 4) < 1e-4 && LightArmorItem.shield(chest) == 0, "Лишний урон не прошёл в здоровье: " + passed);
        LightArmorSystem.tick(player, chest, 20);
        helper.assertTrue(LightArmorItem.shield(chest) == 0, "Щит восстановился без паузы");
        var tag = LightArmorItem.data(chest);
        tag.putLong("lastHit", helper.getLevel().getGameTime() - FocusConfig.LIGHT_REGEN_DELAY.get() - 1);
        LightArmorItem.save(chest, tag);
        int mana = LightArmorItem.mana(chest);
        LightArmorSystem.tick(player, chest, 20);
        double regen = FocusConfig.LIGHT_REGEN_PER_SECOND.get();
        helper.assertTrue(Math.abs(LightArmorItem.shield(chest) - regen) < 1e-6, "Восстановление: " + LightArmorItem.shield(chest));
        helper.assertTrue(LightArmorItem.mana(chest) == mana - (int) Math.ceil(regen * FocusConfig.LIGHT_MANA_PER_UNIT.get()), "Мана за щит");
        LightArmorItem.setMana(chest, 0);
        LightArmorSystem.tick(player, chest, 20);
        helper.assertTrue(LightArmorItem.shield(chest) == 0, "Без маны щит держится");
        helper.assertTrue(ArtifactPower.consumers(player).stream().anyMatch(c -> c.resource() == ArtifactPower.Resource.MANA
                && c.capacity() == FocusConfig.LIGHT_MANA_CAPACITY.get()), "Костюм не берёт ману из накопителя");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shieldFocusRespondsAndWears(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chest = wearChest(player);
        ItemStack hand = FocusItem.create(FocusCut.RECHARGE, crystal(FocusCrystal.QUARTZ));
        helper.assertTrue(LightArmorItem.installFocus(player, chest, hand) && hand.isEmpty(), "Фокус не встал в нагрудник");
        int mana = LightArmorItem.mana(chest);
        LightArmorSystem.absorb(player, chest, 3, null);
        ItemStack focus = LightArmorItem.focus(chest);
        double expected = Math.min(FocusConfig.LIGHT_SHIELD.get(),
                FocusConfig.LIGHT_SHIELD.get() - 3 + FocusConfig.RESPONSE_RECHARGE.get() * crystal(FocusCrystal.QUARTZ).power());
        helper.assertTrue(Math.abs(LightArmorItem.shield(chest) - expected) < 1e-6, "Перезаряд щита: " + LightArmorItem.shield(chest));
        helper.assertTrue(FocusItem.durability(focus) == FocusItem.max(focus) - 1, "Фокус не изношен");
        helper.assertTrue(LightArmorItem.mana(chest) == mana - FocusConfig.RESPONSE_MANA.get(), "Ответ не взял ману");
        LightArmorSystem.absorb(player, chest, 1, null);
        helper.assertTrue(FocusItem.durability(LightArmorItem.focus(chest)) == FocusItem.max(focus) - 1, "Ответ без перезарядки");
        FocusItem.setCracked(focus, true);
        LightArmorItem.setFocus(chest, focus);
        helper.assertTrue(!LightArmorSystem.respond(player, chest, null), "Треснувший фокус сработал");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableBuildsLightSuitAndCapacity(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        helper.assertTrue(TableActions.craftLightPrototype(player) == TableActions.Result.NOT_CHOSEN, "Прототип без выбора пары");
        TableActions.choosePair(player, TableActions.ARMOR, TableActions.LIGHT);
        helper.assertTrue(TableActions.craftHeavyPrototype(player) == TableActions.Result.NOT_CHOSEN, "Тяжёлая броня при лёгкой");
        helper.assertTrue(TableActions.craftLightPrototype(player) == TableActions.Result.OK, "Прототип не собран");
        helper.assertTrue(player.getInventory().countItem(ModItems.LIGHT_CHESTPLATE.get()) == 1, "Нагрудник не выдан");
        helper.assertTrue(TableActions.upgradeLightCapacity(player) == TableActions.Result.OK, "«Ёмкость» I");
        helper.assertTrue(LightArmorSystem.capacity(player) == FocusConfig.shieldCapacity(1)
                && FocusConfig.shieldCapacity(1) > FocusConfig.shieldCapacity(0), "Щит после «Ёмкости» I");
        for (String price : List.of(FocusConfig.TABLE_LIGHT_PROTOTYPE.get(), FocusConfig.TABLE_LIGHT_CAPACITY_1.get())) {
            helper.assertTrue(CostList.parse(price).items().size() == price.split(";").length, "Неизвестный предмет в цене: " + price);
        }
        helper.assertTrue(StepTexts.find(TableActions.LIGHT_CAPACITY, 1).isPresent(), "Нет описания «Ёмкости» I");
        helper.assertTrue(FocusCrystal.all().size() == 6, "Кристаллы фокусов в конфиге");
        helper.succeed();
    }
}
