package ru.adept.core.focus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.registry.ModItems;

import java.util.List;

/**
 * Фокус — огранённый кристалл. Данные в предмете: огранка ({@code cut}), кристалл ({@code crystal}),
 * прочность ({@code durability} из {@code max}), трещина ({@code cracked}), {@code fresh} — только что огранён
 * (снимается, когда игрок забирает фокус из машины). Износ виден полосой прочности под значком.
 */
public class FocusItem extends Item {
    public FocusItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(FocusCut cut, FocusCrystal crystal) {
        ItemStack stack = new ItemStack(ModItems.FOCUS.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("cut", cut.id());
        tag.putString("crystal", crystal.id());
        tag.putInt("max", crystal.durability());
        tag.putInt("durability", crystal.durability());
        save(stack, tag);
        return stack;
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Фокус из сохранённых данных (гнездо артефакта хранит данные фокуса, а не предмет). */
    public static ItemStack fromData(CompoundTag tag) {
        if (tag.isEmpty() || !tag.contains("cut")) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(ModItems.FOCUS.get());
        save(stack, tag.copy());
        return stack;
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof FocusItem && data(stack).contains("cut");
    }

    public static FocusCut cut(ItemStack stack) {
        FocusCut cut = FocusCut.byId(data(stack).getString("cut"));
        return cut == null ? FocusCut.KNOCKBACK : cut;
    }

    public static String crystalId(ItemStack stack) {
        return data(stack).getString("crystal");
    }

    /** Сила кристалла; если кристалла уже нет в конфиге — 1. */
    public static double power(ItemStack stack) {
        return FocusCrystal.byId(crystalId(stack)).map(FocusCrystal::power).orElse(1.0);
    }

    public static int durability(ItemStack stack) {
        return data(stack).getInt("durability");
    }

    public static int max(ItemStack stack) {
        return Math.max(1, data(stack).getInt("max"));
    }

    public static boolean cracked(ItemStack stack) {
        return data(stack).getBoolean("cracked");
    }

    /** Нужна ли починка: треснул или прочность не полная. */
    public static boolean worn(ItemStack stack) {
        return cracked(stack) || durability(stack) < max(stack);
    }

    public static void setDurability(ItemStack stack, int value) {
        CompoundTag tag = data(stack);
        tag.putInt("durability", Mth.clamp(value, 0, Math.max(1, tag.getInt("max"))));
        save(stack, tag);
    }

    public static void setCracked(ItemStack stack, boolean cracked) {
        CompoundTag tag = data(stack);
        tag.putBoolean("cracked", cracked);
        if (cracked) {
            tag.putInt("durability", 0);
        }
        save(stack, tag);
    }

    /** Полная починка: прочность полная, трещины нет. */
    public static void repair(ItemStack stack) {
        repair(stack, 1.0);
    }

    /** Починка: трещина снимается, прочность растёт на долю {@code share} полной (не выше полной). */
    public static void repair(ItemStack stack, double share) {
        CompoundTag tag = data(stack);
        int max = Math.max(1, tag.getInt("max"));
        tag.putBoolean("cracked", false);
        tag.putInt("durability", Math.min(max, Math.max(1, tag.getInt("durability") + (int) Math.ceil(max * share))));
        tag.remove("growth");
        save(stack, tag);
    }

    public static boolean fresh(ItemStack stack) {
        return data(stack).getBoolean("fresh");
    }

    public static void setFresh(ItemStack stack, boolean fresh) {
        CompoundTag tag = data(stack);
        if (fresh) {
            tag.putBoolean("fresh", true);
        } else {
            tag.remove("fresh");
        }
        save(stack, tag);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!is(stack)) {
            return super.getName(stack);
        }
        Component cut = Component.translatable("adept_core.focus.cut." + cut(stack).id());
        return Component.translatable(cracked(stack) ? "adept_core.focus.name.cracked" : "adept_core.focus.name", cut);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!is(stack)) {
            tooltip.add(Component.translatable("adept_core.focus.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        FocusCut cut = cut(stack);
        String crystal = crystalId(stack);
        tooltip.add(Component.translatable("adept_core.focus.crystal",
                Component.translatable("adept_core.focus.crystal." + crystal), StepTexts.format(power(stack))).withStyle(ChatFormatting.AQUA));
        wrapped(tooltip, Component.translatable("adept_core.focus.effect." + cut.id(), effectArgs(cut, power(stack))), ChatFormatting.GRAY);
        wrapped(tooltip, Component.translatable("adept_core.focus.trigger",
                StepTexts.format(FocusConfig.RESPONSE_COOLDOWN.get() / 20.0),
                StepTexts.format(Math.round(FocusConfig.RESPONSE_MANA.get() * Focuses.manaFactor(stack)))), ChatFormatting.GRAY);
        wrapped(tooltip, Component.translatable("adept_core.focus.modifier." + crystal,
                FocusConfig.FOCUS_GROWTH_SECONDS.get(), StepTexts.format(100 * FocusConfig.FOCUS_LUCK_CHANCE.get()),
                StepTexts.format(100 * FocusConfig.FOCUS_ECONOMY.get())), ChatFormatting.DARK_AQUA);
        if (cracked(stack)) {
            wrapped(tooltip, Component.translatable("adept_core.focus.cracked"), ChatFormatting.RED);
        } else {
            tooltip.add(Component.translatable("adept_core.focus.durability", durability(stack), max(stack)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("adept_core.focus.segment." + cut.segment().replace('/', '.')).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Длинная строка подсказки — по словам, подсказки сами не переносятся. */
    public static void wrapped(List<Component> tooltip, Component text, ChatFormatting style) {
        for (String line : ru.adept.core.util.TextWrap.wrap(text.getString(), ru.adept.core.util.TextWrap.TOOLTIP_CHARS)) {
            tooltip.add(Component.literal(line).withStyle(style));
        }
    }

    /** Числа эффекта для описания: уже с силой кристалла. */
    public static Object[] effectArgs(FocusCut cut, double power) {
        String radius = StepTexts.format(FocusConfig.RESPONSE_RADIUS.get());
        return switch (cut) {
            case KNOCKBACK -> new Object[]{radius, StepTexts.format(FocusConfig.RESPONSE_KNOCKBACK.get() * power)};
            case FLASH -> new Object[]{radius, StepTexts.format(FocusConfig.RESPONSE_FLASH_TICKS.get() * power / 20.0)};
            case RECHARGE -> new Object[]{StepTexts.format(FocusConfig.RESPONSE_RECHARGE.get() * power)};
            case SLOW -> new Object[]{radius, StepTexts.format(FocusConfig.RESPONSE_SLOW_TICKS.get() * power / 20.0),
                    FocusConfig.RESPONSE_SLOW_LEVEL.get() + 1};
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return is(stack) && worn(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * durability(stack) / max(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = (float) durability(stack) / max(stack);
        return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
    }
}
