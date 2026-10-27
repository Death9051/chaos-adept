package ru.adept.core.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.BaseTravel;
import ru.adept.core.base.CrusherBlockEntity;
import ru.adept.core.base.MachineRecipes;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;

/** Автотесты базы: измерение, запас энергии, рецепты, работа переработки руды. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseGameTests {
    private BaseGameTests() {
    }

    @GameTest(template = "empty")
    public static void baseDimensionTypeRegistered(GameTestHelper helper) {
        // Сервер геймтестов строит миры по своему пресету и измерения из датапаков не поднимает,
        // поэтому здесь проверяется только тип измерения; само измерение проверяет клиентский тест (лог).
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE)
                .containsKey(BaseTravel.BASE.location()), "Тип измерения базы не зарегистрирован");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseEnergyClamps(GameTestHelper helper) {
        BaseData data = BaseData.get(helper.getLevel().getServer());
        int e = data.electricity();
        int m = data.mana();
        data.set(0, 0);
        data.add(BaseData.capacity() * 2, 5);
        helper.assertTrue(data.electricity() == BaseData.capacity(), "Запас электричества не ограничен");
        helper.assertTrue(!data.useMana(6) && data.useMana(5) && data.mana() == 0, "Расход маны посчитан неверно");
        data.set(e, m);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineRecipes(GameTestHelper helper) {
        helper.assertTrue(MachineRecipes.crush(new ItemStack(Items.RAW_IRON)).is(ModItems.IRON_DUST.get()), "Сырое железо не дробится");
        helper.assertTrue(MachineRecipes.ingotFor(new ItemStack(ModItems.COPPER_DUST.get())).is(Items.COPPER_INGOT), "Медная пыль не плавится");
        helper.assertTrue(MachineRecipes.crush(new ItemStack(Items.STONE)).isEmpty(), "Камень дробится как руда");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void crusherProducesDust(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        BaseData data = BaseData.get(server);
        data.set(BaseData.capacity(), BaseData.capacity());
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        crusher.items().setItem(0, new ItemStack(Items.RAW_IRON, 1));
        helper.succeedWhen(() -> helper.assertTrue(crusher.items().getItem(1).is(ModItems.IRON_DUST.get()),
                "Пыль ещё не готова"));
    }
}
