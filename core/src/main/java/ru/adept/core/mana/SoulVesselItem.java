package ru.adept.core.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.config.ManaConfig;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * Сосуд для душ. Объём задаёт материал (стекло, кварц; дальше — аметист, алмаз и сплавы кристаллов).
 * Души хранятся числом по силе в данных предмета ({@code souls}). Сосуд ставится в слот котла душ.
 */
public class SoulVesselItem extends Item {
    private final String material;
    private final IntSupplier capacity;

    public SoulVesselItem(String material, IntSupplier capacity, Properties properties) {
        super(properties);
        this.material = material;
        this.capacity = capacity;
    }

    public String material() {
        return material;
    }

    public int capacity() {
        return capacity.getAsInt();
    }

    /** Объём сосуда по материалу (для сосуда, вставленного в котёл). */
    public static int capacityOf(String material) {
        return switch (material) {
            case "glass" -> ManaConfig.VESSEL_GLASS.get();
            case "quartz" -> ManaConfig.VESSEL_QUARTZ.get();
            default -> 0;
        };
    }

    public static int[] souls(ItemStack stack) {
        return SoulCounts.read(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(), "souls");
    }

    public static void setSouls(ItemStack stack, int[] counts) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        SoulCounts.write(tag, "souls", counts);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int[] souls = souls(stack);
        tooltip.add(Component.translatable("adept_core.vessel.souls", SoulCounts.total(souls), capacity()).withStyle(ChatFormatting.AQUA));
        if (souls[1] + souls[2] > 0) {
            tooltip.add(Component.translatable("adept_core.vessel.strong", souls[1], souls[2]).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("adept_core.vessel.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
