package ru.adept.core.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.adept.core.config.BalanceConfig;

/** Ключ к базе: из мира — на базу (запоминая место), с базы — обратно в точку последнего выхода. */
public class KeyItem extends Item {
    public KeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            if (BaseTravel.isBase(level)) {
                BaseTravel.leave(server);
            } else {
                BaseTravel.enter(server, server.position(), server.getYRot());
            }
            player.getCooldowns().addCooldown(this, BalanceConfig.KEY_COOLDOWN_TICKS.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
