package ru.adept.core.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import ru.adept.core.base.Molten;
import ru.adept.core.config.BalanceConfig;

import java.util.List;

/**
 * Тяжёлая силовая броня, прототип. Шлем — пластина головы (15% урона), нагрудник — пластины корпуса (40%) и рук (20%),
 * ноги — пластина ног (25%). Пластина гасит часть своей доли урона (70%, «Бастион» больше), остальное проходит;
 * доля разбитой или пустой пластины проходит целиком. Пластины ставятся из второй руки: броня в основной, ПКМ.
 * Сервоприводы питаются электричеством нагрудника. Не ломается и не зачаровывается.
 */
public class HeavyArmorItem extends ArmorItem {
    /** Части тела и их доли урона. Порядок в нагруднике: корпус, руки. */
    public enum Part { HEAD, TORSO, ARMS, LEGS }

    public HeavyArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public List<Part> parts() {
        return switch (getType()) {
            case HELMET -> List.of(Part.HEAD);
            case CHESTPLATE -> List.of(Part.TORSO, Part.ARMS);
            case LEGGINGS -> List.of(Part.LEGS);
            default -> List.of();
        };
    }

    public static double share(Part part) {
        return switch (part) {
            case HEAD -> BalanceConfig.HEAVY_SHARE_HEAD.get();
            case TORSO -> BalanceConfig.HEAVY_SHARE_TORSO.get();
            case ARMS -> BalanceConfig.HEAVY_SHARE_ARMS.get();
            case LEGS -> BalanceConfig.HEAVY_SHARE_LEGS.get();
        };
    }

    /** Прочность новой пластины по материалу. */
    public static double plateDurability(Molten m) {
        return switch (m) {
            case IRON -> BalanceConfig.PLATE_IRON.get();
            case COPPER -> BalanceConfig.PLATE_COPPER.get();
            case GOLD -> BalanceConfig.PLATE_GOLD.get();
            case STEEL -> BalanceConfig.PLATE_STEEL.get();
            case RED_GOLD -> BalanceConfig.PLATE_RED_GOLD.get();
        };
    }

    // ---- данные в предмете: plates [{material, durability}] по частям, charge (нагрудник) ----

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Пластина части {@code index} (по порядку {@link #parts()}): материал и прочность, пусто — нет. */
    public static CompoundTag plate(ItemStack stack, int index) {
        ListTag plates = data(stack).getList("plates", Tag.TAG_COMPOUND);
        return index < plates.size() ? plates.getCompound(index) : new CompoundTag();
    }

    public static void setPlate(ItemStack stack, int index, CompoundTag plate) {
        CompoundTag tag = data(stack);
        ListTag plates = tag.getList("plates", Tag.TAG_COMPOUND);
        while (plates.size() <= index) {
            plates.add(new CompoundTag());
        }
        plates.set(index, plate);
        tag.put("plates", plates);
        save(stack, tag);
    }

    public static boolean intact(CompoundTag plate) {
        return plate.contains("material") && plate.getDouble("durability") > 0;
    }

    /** Поставить пластину в первое пустое или разбитое гнездо. */
    public boolean installPlate(ItemStack armor, ItemStack plateItem) {
        var material = Molten.ofPart(plateItem).filter(m -> plateItem.is(m.plate()));
        if (material.isEmpty()) {
            return false;
        }
        for (int i = 0; i < parts().size(); i++) {
            if (!intact(plate(armor, i))) {
                CompoundTag p = new CompoundTag();
                p.putString("material", material.get().id());
                p.putDouble("durability", plateDurability(material.get()));
                setPlate(armor, i, p);
                plateItem.shrink(1);
                return true;
            }
        }
        return false;
    }

    public static int charge(ItemStack chest) {
        return data(chest).getInt("charge");
    }

    public static void setCharge(ItemStack chest, int value) {
        CompoundTag tag = data(chest);
        tag.putInt("charge", Math.max(0, Math.min(value, BalanceConfig.HEAVY_CHARGE_CAPACITY.get())));
        save(chest, tag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack armor = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (hand == InteractionHand.MAIN_HAND && Molten.ofPart(other).isPresent()) {
            if (!level.isClientSide && installPlate(armor, other)) {
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), player.getSoundSource(), 1.0f, 0.8f);
            }
            return InteractionResultHolder.sidedSuccess(armor, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<Part> parts = parts();
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag p = plate(stack, i);
            Component part = Component.translatable("adept_core.heavy.part." + parts.get(i).name().toLowerCase(java.util.Locale.ROOT));
            if (intact(p)) {
                tooltip.add(Component.translatable("adept_core.heavy.plate", part,
                        Component.translatable("adept_core.molten." + p.getString("material")),
                        StepTexts.format(Math.ceil(p.getDouble("durability")))).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("adept_core.heavy.no_plate", part).withStyle(ChatFormatting.DARK_RED));
            }
        }
        if (getType() == Type.CHESTPLATE) {
            tooltip.add(Component.translatable("adept_core.heavy.charge", charge(stack), BalanceConfig.HEAVY_CHARGE_CAPACITY.get())
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("adept_core.heavy.install").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    static Item.Properties props() {
        return new Item.Properties().stacksTo(1);
    }
}
