package ru.adept.core.artifact;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import ru.adept.core.config.BalanceConfig;

/**
 * Энергоклинок, прототип. В срезе 1: базовый урон и скорость из конфига,
 * ступень «Кромка I» добавляет урон (см. {@link BladeCombat}). Не ломается, не зачаровывается.
 */
public class EnergyBladeItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public EnergyBladeItem(Properties properties) {
        super(properties);
    }

    /** Модель клинка — GeckoLib; облик по сегментам лежит в данных предмета ({@link GaussLook}). */
    @Override
    public void createGeoRenderer(java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ru.adept.core.client.ArtifactItemRenderer<EnergyBladeItem>(
                            "item/energy_blade", "energy_blade", "energy_blade", GaussLook.BLADE);
                }
                return renderer;
            }
        });
    }

    /** Поле лезвия всё время слегка пульсирует (анимация idle). */
    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public static int look(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).getUnsafe().getInt(GaussLook.TAG);
    }

    public static void setLook(ItemStack stack, int look) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(GaussLook.TAG, look);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /** Сервер держит облик в данных предмета равным прогрессу игрока: так его видят рендер и другие игроки. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int look = GaussLook.pack(GaussLook.BLADE, ru.adept.core.table.TableActions.progress(player));
        if (look(stack) != look) {
            setLook(stack, look);
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Игрок без оружия бьёт на 1 и 4 удара в секунду — модификаторы считаются от этих значений.
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        BalanceConfig.BLADE_BASE_DAMAGE.get() - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        BalanceConfig.BLADE_ATTACK_SPEED.get() - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("adept_core.blade.steps")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }
}
