package ru.adept.core.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.config.ManaConfig;
import ru.adept.core.table.TableActions;

import java.util.List;

/**
 * Накопитель: одиночный артефакт, носится в инвентаре. Два раздельных запаса — мана и электричество,
 * друг в друга не переводятся; приём и отдача без потерь. Ёмкость растёт «Ёмкостью» (прогресс игрока),
 * в данных предмета хранится последняя известная ёмкость — для подсказки.
 */
public class AccumulatorItem extends Item {
    public AccumulatorItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static int stored(ItemStack stack, ArtifactPower.Resource resource) {
        return data(stack).getInt(resource.id());
    }

    public static void setStored(ItemStack stack, ArtifactPower.Resource resource, int value) {
        CompoundTag tag = data(stack);
        tag.putInt(resource.id(), Math.max(0, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Ёмкость каждого запаса для игрока (по «Ёмкости»). */
    public static int capacity(Player player) {
        return ManaConfig.accumulatorCapacity(TableActions.progress(player).step(TableActions.ACCUMULATOR_CAPACITY));
    }

    /** Запомнить ёмкость в предмете (для подсказки) и срезать запасы сверх неё. */
    public static void syncCapacity(ItemStack stack, int capacity) {
        CompoundTag tag = data(stack);
        if (tag.getInt("capacity") == capacity) {
            return;
        }
        tag.putInt("capacity", capacity);
        for (ArtifactPower.Resource r : ArtifactPower.Resource.values()) {
            tag.putInt(r.id(), Math.min(tag.getInt(r.id()), capacity));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Принять ресурс в запас; возвращает, сколько принято. */
    public static int receive(ItemStack stack, ArtifactPower.Resource resource, int amount, int capacity) {
        int have = stored(stack, resource);
        int take = Math.max(0, Math.min(amount, capacity - have));
        if (take > 0) {
            setStored(stack, resource, have + take);
        }
        return take;
    }

    /** Первый накопитель в инвентаре игрока или пустой стек. */
    public static ItemStack find(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AccumulatorItem) {
                return stack;
            }
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof AccumulatorItem ? off : ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int cap = data(stack).getInt("capacity");
        if (cap <= 0) {
            cap = ManaConfig.ACCUMULATOR_CAPACITY.get();
        }
        tooltip.add(Component.translatable("adept_core.accumulator.mana", stored(stack, ArtifactPower.Resource.MANA), cap)
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("adept_core.accumulator.electricity", stored(stack, ArtifactPower.Resource.ELECTRICITY), cap)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("adept_core.accumulator.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
