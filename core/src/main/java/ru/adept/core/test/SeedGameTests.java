package ru.adept.core.test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.SeedEvent;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;
import ru.adept.core.seed.Seeds;

import java.util.concurrent.atomic.AtomicInteger;

/** Автотесты стартового зачатка: выдача один раз, сохранность после смерти, рецепт стола артефактов. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeedGameTests {
    private SeedGameTests() {
    }

    @GameTest(template = "empty")
    public static void seedGrantedOncePerPlayer(GameTestHelper helper) {
        AtomicInteger received = new AtomicInteger();
        Object listener = new Object() {
            @SubscribeEvent
            public void got(SeedEvent.Received e) {
                if (e.isFirstJoin()) {
                    received.incrementAndGet();
                }
            }
        };
        NeoForge.EVENT_BUS.register(listener);
        try {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().clearContent();
            helper.assertTrue(Seeds.grantFirst(player), "Зачаток не выдан при первом входе");
            helper.assertTrue(!Seeds.grantFirst(player), "Зачаток выдан второй раз");
            helper.assertTrue(player.getInventory().countItem(ModItems.SEED.get()) == 1, "Зачатков не один");
            helper.assertTrue(received.get() == 1, "Событие получения: " + received.get());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seedKeptAfterDeathAndProtected(GameTestHelper helper) {
        Player before = helper.makeMockPlayer(GameType.SURVIVAL);
        before.getInventory().clearContent();
        before.getInventory().add(Seeds.stack());
        before.getInventory().add(new ItemStack(Items.DIRT, 5));
        helper.assertTrue(Seeds.stash(before) == 1 && !Seeds.has(before), "Зачаток не убран перед выпадением вещей");
        helper.assertTrue(before.getInventory().countItem(Items.DIRT) == 5, "Убрано лишнее");
        Player after = helper.makeMockPlayer(GameType.SURVIVAL);
        after.getInventory().clearContent();
        helper.assertTrue(Seeds.restore(before, after) == 1 && Seeds.has(after), "Зачаток не вернулся после смерти");
        helper.assertTrue(Seeds.restore(before, after) == 0, "Зачаток вернулся дважды");
        ItemStack seed = Seeds.stack();
        helper.assertTrue(seed.has(DataComponents.FIRE_RESISTANT), "Зачаток горит");
        helper.assertTrue(seed.getEntityLifespan(helper.getLevel()) == Integer.MAX_VALUE, "Зачаток исчезает на земле");
        ItemStack table = new ItemStack(ModBlocks.ARTIFACT_TABLE.get());
        helper.assertTrue(table.has(DataComponents.FIRE_RESISTANT) && table.getEntityLifespan(helper.getLevel()) == Integer.MAX_VALUE,
                "Стол на земле не защищён");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tableRecipeNeedsSeed(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "artifact_table")).orElseThrow();
        var recipe = holder.value();
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(ModBlocks.ARTIFACT_TABLE.get().asItem()),
                "Рецепт даёт не стол");
        int seeds = 0;
        int gold = 0;
        boolean workbench = false;
        for (Ingredient in : recipe.getIngredients()) {
            if (in.test(Seeds.stack())) {
                seeds++;
            }
            if (in.test(new ItemStack(Items.GOLD_INGOT))) {
                gold++;
            }
            if (in.test(new ItemStack(Items.CRAFTING_TABLE))) {
                workbench = true;
            }
        }
        helper.assertTrue(seeds == 1 && gold == 4 && !workbench, "Рецепт стола: зачатков " + seeds + ", золота " + gold + ", верстак " + workbench);
        helper.succeed();
    }
}
