package ru.adept.core.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.focus.FocusCut;
import ru.adept.core.focus.FocusItem;

import java.util.List;

/**
 * Лёгкий экзокостюм, прототип: нагрудник — генератор щита. Щит из маны принимает урон раньше здоровья;
 * без маны щита нет, остаётся броня 6. Мана костюма — от накопителя. В гнезде — фокус нагрудника (ответ щита на удар):
 * фокус ставится из второй руки, костюм в основной, ПКМ; Shift+ПКМ с пустой второй рукой снимает фокус.
 * Данные: {@code mana}, {@code shield}, {@code capacity} (для подсказки), {@code focus} (данные фокуса),
 * {@code lastHit} и {@code nextResponse} — игровое время. Не ломается и не зачаровывается.
 */
public class LightArmorItem extends ArmorItem {
    public static final String SEGMENT = "light/chest";

    public LightArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int mana(ItemStack chest) {
        return data(chest).getInt("mana");
    }

    public static void setMana(ItemStack chest, int value) {
        CompoundTag tag = data(chest);
        tag.putInt("mana", Math.max(0, Math.min(value, FocusConfig.LIGHT_MANA_CAPACITY.get())));
        save(chest, tag);
    }

    public static double shield(ItemStack chest) {
        return data(chest).getDouble("shield");
    }

    public static void setShield(ItemStack chest, double value, int capacity) {
        CompoundTag tag = data(chest);
        tag.putDouble("shield", Math.max(0, Math.min(value, capacity)));
        tag.putInt("capacity", capacity);
        save(chest, tag);
    }

    /** Предел щита, записанный при последнем обновлении (для подсказки и шкалы на экране). */
    public static int shownCapacity(ItemStack chest) {
        CompoundTag tag = data(chest);
        return tag.contains("capacity") ? tag.getInt("capacity") : FocusConfig.LIGHT_SHIELD.get();
    }

    public static ItemStack focus(ItemStack chest) {
        return FocusItem.fromData(data(chest).getCompound("focus"));
    }

    public static void setFocus(ItemStack chest, ItemStack focus) {
        CompoundTag tag = data(chest);
        if (focus.isEmpty()) {
            tag.remove("focus");
        } else {
            tag.put("focus", FocusItem.data(focus));
        }
        save(chest, tag);
    }

    /** Поставить фокус из второй руки (старый возвращается игроку). Только огранки своего сегмента. */
    public static boolean installFocus(Player player, ItemStack chest, ItemStack focus) {
        if (!FocusItem.is(focus) || !SEGMENT.equals(FocusItem.cut(focus).segment())) {
            return false;
        }
        ItemStack old = focus(chest);
        ItemStack copy = focus.copyWithCount(1);
        focus.shrink(1);
        setFocus(chest, copy);
        if (!old.isEmpty() && !player.getInventory().add(old)) {
            player.drop(old, false);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chest = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(InteractionHand.OFF_HAND);
        if (hand == InteractionHand.MAIN_HAND && FocusItem.is(other)) {
            if (!level.isClientSide && installFocus(player, chest, other)) {
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, player.getSoundSource(), 1.0f, 1.2f);
            }
            return InteractionResultHolder.sidedSuccess(chest, level.isClientSide);
        }
        if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && other.isEmpty() && !focus(chest).isEmpty()) {
            if (!level.isClientSide) {
                ItemStack old = focus(chest);
                setFocus(chest, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, old);
            }
            return InteractionResultHolder.sidedSuccess(chest, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("adept_core.light.shield", StepTexts.format(Math.floor(shield(stack))), shownCapacity(stack))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("adept_core.light.mana", mana(stack), FocusConfig.LIGHT_MANA_CAPACITY.get())
                .withStyle(ChatFormatting.BLUE));
        ItemStack focus = focus(stack);
        if (focus.isEmpty()) {
            tooltip.add(Component.translatable("adept_core.light.no_focus").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            FocusCut cut = FocusItem.cut(focus);
            Component name = Component.translatable("adept_core.focus.cut." + cut.id());
            if (FocusItem.cracked(focus)) {
                tooltip.add(Component.translatable("adept_core.light.focus_cracked", name).withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("adept_core.light.focus", name,
                        Component.translatable("adept_core.focus.crystal." + FocusItem.crystalId(focus)),
                        FocusItem.durability(focus), FocusItem.max(focus)).withStyle(ChatFormatting.GRAY));
            }
        }
        FocusItem.wrapped(tooltip, Component.translatable("adept_core.light.install"), ChatFormatting.DARK_GRAY);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }
}
