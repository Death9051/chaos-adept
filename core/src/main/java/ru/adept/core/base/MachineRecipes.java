package ru.adept.core.base;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.adept.core.registry.ModItems;

import java.util.Map;

/**
 * Рецепты машин среза 1. Переработка: сырой кусок или блок руды → рудная пыль (1:1, пути «Выход» добавят больше).
 * Плавильня: пыль → расплав → слиток.
 */
public final class MachineRecipes {
    private MachineRecipes() {
    }

    private static Map<Item, Item> crusher() {
        return Map.ofEntries(
                Map.entry(Items.RAW_IRON, ModItems.IRON_DUST.get()),
                Map.entry(Items.IRON_ORE, ModItems.IRON_DUST.get()),
                Map.entry(Items.DEEPSLATE_IRON_ORE, ModItems.IRON_DUST.get()),
                Map.entry(Items.RAW_COPPER, ModItems.COPPER_DUST.get()),
                Map.entry(Items.COPPER_ORE, ModItems.COPPER_DUST.get()),
                Map.entry(Items.DEEPSLATE_COPPER_ORE, ModItems.COPPER_DUST.get()),
                Map.entry(Items.RAW_GOLD, ModItems.GOLD_DUST.get()),
                Map.entry(Items.GOLD_ORE, ModItems.GOLD_DUST.get()),
                Map.entry(Items.DEEPSLATE_GOLD_ORE, ModItems.GOLD_DUST.get()));
    }

    private static Map<Item, Item> smelter() {
        return Map.of(
                ModItems.IRON_DUST.get(), Items.IRON_INGOT,
                ModItems.COPPER_DUST.get(), Items.COPPER_INGOT,
                ModItems.GOLD_DUST.get(), Items.GOLD_INGOT);
    }

    public static ItemStack crush(ItemStack input) {
        Item out = crusher().get(input.getItem());
        return out == null ? ItemStack.EMPTY : new ItemStack(out);
    }

    /** Слиток, в который плавится пыль; пусто, если не плавится. */
    public static ItemStack ingotFor(ItemStack dust) {
        Item out = smelter().get(dust.getItem());
        return out == null ? ItemStack.EMPTY : new ItemStack(out);
    }
}
