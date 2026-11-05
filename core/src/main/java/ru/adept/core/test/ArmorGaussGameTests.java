package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.HeavyArmorItem;
import ru.adept.core.artifact.HeavyArmorSystem;
import ru.adept.core.base.Molten;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.table.TableActions;

/** Автотесты тяжёлой брони и гаусс-пушки: пластины и «Бастион», магазин, заряд, нагрев, «Темп», выбор пар. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmorGaussGameTests {
    private ArmorGaussGameTests() {
    }

    private static ItemStack plated(HeavyArmorItem item) {
        ItemStack stack = new ItemStack(item);
        for (int i = 0; i < item.parts().size(); i++) {
            item.installPlate(stack, new ItemStack(Molten.IRON.plate()));
        }
        return stack;
    }

    @GameTest(template = "empty")
    public static void platesAbsorbShareAndBastionAddsMore(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, plated(ModItems.HEAVY_HELMET.get()));
        ItemStack chest = plated(ModItems.HEAVY_CHESTPLATE.get());
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ItemStack legs = plated(ModItems.HEAVY_LEGGINGS.get());
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        float passed = HeavyArmorSystem.absorbDamage(player, 10.0f);
        helper.assertTrue(Math.abs(passed - 3.0f) < 1e-3, "Через целые пластины прошло " + passed + " вместо 3");
        double torso = HeavyArmorItem.plate(chest, 0).getDouble("durability");
        helper.assertTrue(Math.abs(torso - (BalanceConfig.PLATE_IRON.get() - 2.8)) < 1e-3, "Износ пластины корпуса: " + torso);
        HeavyArmorItem.setPlate(legs, 0, new CompoundTag());
        passed = HeavyArmorSystem.absorbDamage(player, 10.0f);
        helper.assertTrue(Math.abs(passed - 4.75f) < 1e-3, "Без пластины ног прошло " + passed + " вместо 4,75");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(TableActions.HEAVY_BASTION, 1));
        helper.assertTrue(Math.abs(HeavyArmorSystem.absorb(player) - 0.75) < 1e-6, "«Бастион» I не 75%");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gaussMagazineChargeHeatAndTempo(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModItems.GAUSS_GUN.get());
        GaussGunItem.setCharge(gun, GaussGunItem.capacity());
        player.getInventory().add(new ItemStack(Molten.STEEL.needle(), 8));
        player.getInventory().add(new ItemStack(Molten.IRON.needle(), 40));
        GaussGunItem.finishReload(player, gun);
        helper.assertTrue(GaussGunItem.magazine(gun).size() == BalanceConfig.GAUSS_MAGAZINE.get(), "Магазин не полный");
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(GaussGunItem.fire(helper.getLevel(), player, gun, now), "Первый выстрел не прошёл");
        helper.assertTrue(!GaussGunItem.fire(helper.getLevel(), player, gun, now), "Второй выстрел в тот же тик");
        helper.assertTrue(GaussGunItem.charge(gun) == GaussGunItem.capacity() - BalanceConfig.GAUSS_SHOT_COST.get(), "Расход заряда");
        helper.assertTrue(Math.abs(GaussGunItem.heat(GaussGunItem.data(gun), now) - BalanceConfig.GAUSS_HEAT_PER_SHOT.get()) < 1e-6, "Нагрев");
        helper.assertTrue(GaussGunItem.magazine(gun).size() == BalanceConfig.GAUSS_MAGAZINE.get() - 1, "Игла не ушла из магазина");
        helper.assertTrue(Math.abs(GaussGunItem.fireRate(player) - 4.0) < 1e-6, "Скорострельность без «Темпа»");
        player.setData(ModRegistries.ARTIFACT_PROGRESS, TableActions.progress(player).withStep(TableActions.GAUSS_TEMPO, 1));
        helper.assertTrue(Math.abs(GaussGunItem.fireRate(player) - 4.8) < 1e-6, "«Темп» I не 4,8 в секунду");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorAndRangedPairsAreExclusive(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(TableActions.choosePair(player, TableActions.ARMOR, TableActions.HEAVY) == TableActions.Result.OK, "Выбор брони");
        helper.assertTrue(TableActions.choosePair(player, TableActions.ARMOR, TableActions.LIGHT) == TableActions.Result.ALREADY_CHOSEN,
                "Броню выбрали второй раз");
        helper.assertTrue(TableActions.choosePair(player, TableActions.RANGED, TableActions.GAUSS) == TableActions.Result.OK, "Выбор дальнего боя");
        helper.assertTrue(TableActions.craftHeavyPrototype(player) == TableActions.Result.NOT_ENOUGH, "Прототип брони без материалов");
        helper.assertTrue(TableActions.upgradeGaussTempo(player) == TableActions.Result.NOT_CHOSEN, "«Темп» без прототипа");
        helper.succeed();
    }
}
