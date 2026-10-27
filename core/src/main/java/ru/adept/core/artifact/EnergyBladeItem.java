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
public class EnergyBladeItem extends Item {
    public EnergyBladeItem(Properties properties) {
        super(properties);
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
