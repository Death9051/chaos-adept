package ru.adept.core.seed;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ru.adept.core.focus.FocusItem;

import java.util.List;

/**
 * «Зачаток» — единственный предмет на старте («Свод решений», «Старт»). Сам ничего не делает: из него собирается
 * стол артефактов. Не горит (свойство предмета), не исчезает на земле, при смерти остаётся у игрока ({@link Seeds}).
 */
public class SeedItem extends Item {
    public SeedItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    /** Лежит на земле бессрочно. */
    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FocusItem.wrapped(tooltip, Component.translatable("adept_core.seed.desc"), ChatFormatting.GRAY);
        FocusItem.wrapped(tooltip, Component.translatable("adept_core.seed.safe"), ChatFormatting.DARK_GRAY);
    }
}
