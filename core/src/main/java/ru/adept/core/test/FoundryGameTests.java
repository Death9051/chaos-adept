package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.base.AlloyerBlockEntity;
import ru.adept.core.base.CasterBlockEntity;
import ru.adept.core.base.Molten;
import ru.adept.core.base.ReservoirBlockEntity;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModBlocks;

/** Автотесты литья: резервуар, сплавщик (красное золото и сталь), литейная (иглы и пластины). */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoundryGameTests {
    private FoundryGameTests() {
    }

    private static ReservoirBlockEntity reservoir(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.RESERVOIR.get());
        return (ReservoirBlockEntity) helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void reservoirHoldsOneMaterial(GameTestHelper helper) {
        ReservoirBlockEntity r = reservoir(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(r.insert("iron", 300) == 300, "Резервуар не принял железо");
        helper.assertTrue(r.insert("gold", 100) == 0, "Простой резервуар принял второй материал");
        helper.assertTrue(r.insert("iron", 1_000_000) == ReservoirBlockEntity.capacity() - 300, "Объём не по конфигу");
        helper.assertTrue(r.extract("iron", ReservoirBlockEntity.capacity()) == ReservoirBlockEntity.capacity() && r.material().isEmpty(),
                "Пустой резервуар помнит материал");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void alloyerMakesRedGoldAndSteel(GameTestHelper helper) {
        int mb = BalanceConfig.ALLOY_MB.get();
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.ALLOYER.get());
        AlloyerBlockEntity alloyer = (AlloyerBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        ReservoirBlockEntity gold = reservoir(helper, new BlockPos(1, 1, 2));
        ReservoirBlockEntity copper = reservoir(helper, new BlockPos(3, 1, 2));
        ReservoirBlockEntity out = reservoir(helper, new BlockPos(2, 1, 1));
        gold.insert(Molten.GOLD.id(), mb);
        copper.insert(Molten.COPPER.id(), mb);
        alloyer.setRecipe(0);
        helper.assertTrue(alloyer.finish(), "Красное золото не сплавилось");
        helper.assertTrue(out.material().equals(Molten.RED_GOLD.id()) && out.amount() == mb * 2, "Нет красного золота в резервуаре");
        gold.insert(Molten.IRON.id(), mb);
        alloyer.setRecipe(1);
        helper.assertTrue(!alloyer.finish(), "Сталь без угля");
        alloyer.items().setItem(0, new ItemStack(Items.COAL));
        ReservoirBlockEntity steel = reservoir(helper, new BlockPos(2, 2, 2));
        helper.assertTrue(alloyer.finish(), "Сталь не сплавилась");
        helper.assertTrue(steel.material().equals(Molten.STEEL.id()) && alloyer.items().getItem(0).isEmpty(), "Сталь или уголь не на месте");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void casterMakesNeedlesAndPlates(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.CASTER.get());
        CasterBlockEntity caster = (CasterBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        ReservoirBlockEntity iron = reservoir(helper, new BlockPos(1, 1, 2));
        iron.insert(Molten.IRON.id(), BalanceConfig.CAST_NEEDLE_MB.get() + BalanceConfig.CAST_PLATE_MB.get());
        caster.setForm(CasterBlockEntity.Form.NEEDLE.ordinal());
        helper.assertTrue(caster.cast(), "Иглы не отлились");
        ItemStack needles = caster.items().getItem(1);
        helper.assertTrue(needles.is(Molten.IRON.needle()) && needles.getCount() == BalanceConfig.CAST_NEEDLES_PER_CAST.get(), "Не те иглы");
        caster.items().setItem(1, ItemStack.EMPTY);
        caster.setForm(CasterBlockEntity.Form.PLATE.ordinal());
        helper.assertTrue(caster.cast(), "Пластина не отлилась");
        helper.assertTrue(caster.items().getItem(1).is(Molten.IRON.plate()) && iron.amount() == 0, "Пластина или расход не те");
        helper.assertTrue(!caster.cast(), "Отлилось без расплава");
        helper.succeed();
    }
}
