package ru.adept.core.component;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Компонент для ступеней прокачки артефактов: семейство, ранг I–III и вариация.
 * В срезе 4 рецептов нет — выдаётся командой {@code /adept component} и лежит во вкладке творческого режима.
 */
public class ComponentItem extends Item {
    private static final String[] ROMAN = {"I", "II", "III"};

    private final ComponentFamily family;
    private final int rank;
    private final ComponentVariant variant;

    public ComponentItem(ComponentFamily family, int rank, ComponentVariant variant, Properties properties) {
        super(properties);
        this.family = family;
        this.rank = rank;
        this.variant = variant;
    }

    public ComponentFamily family() {
        return family;
    }

    public int rank() {
        return rank;
    }

    public ComponentVariant variant() {
        return variant;
    }

    public static String roman(int rank) {
        return ROMAN[Math.max(1, Math.min(rank, ROMAN.length)) - 1];
    }

    @Override
    public Component getName(ItemStack stack) {
        Component familyName = Component.translatable("adept_core.component." + family.id());
        String key = family.nature() == ComponentFamily.Nature.BOTH
                ? "adept_core.component.name." + variant.id()
                : "adept_core.component.name";
        return Component.translatable(key, familyName, roman(rank));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("adept_core.component.rank", roman(rank),
                Component.translatable("adept_core.component.material." + rank)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("adept_core.component.variant." + variant.id())
                .withStyle(variant == ComponentVariant.MAGIC ? ChatFormatting.BLUE : ChatFormatting.RED));
        tooltip.add(Component.translatable("adept_core.component.no_recipe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
