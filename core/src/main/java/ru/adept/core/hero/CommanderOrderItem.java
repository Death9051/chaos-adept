package ru.adept.core.hero;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Приказ командира: предмет ядра с именами командира и деревни-отправителя и меткой деревни.
 * Текст приказа — языковой ключ {@code adept_core.commander_order.text} (%1$s — командир, %2$s — деревня);
 * мод сюжета переопределяет его своим шаблоном.
 */
public class CommanderOrderItem extends Item {
    public CommanderOrderItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, String commander, String village, long villageId, int x, int z) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString("commander", commander);
        tag.putString("village", village);
        tag.putLong("village_id", villageId);
        tag.putInt("x", x);
        tag.putInt("z", z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag.isEmpty()) {
            return;
        }
        // Шаблон сюжета длинный (около 120 знаков) — переносим по словам, «\n» в шаблоне тоже даёт перенос.
        String text = Component.translatable("adept_core.commander_order.text", tag.getString("commander"), tag.getString("village"))
                .getString();
        for (String line : ru.adept.core.util.TextWrap.wrap(text, ru.adept.core.util.TextWrap.TOOLTIP_CHARS)) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("adept_core.commander_order.mark", tag.getInt("x"), tag.getInt("z"))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
