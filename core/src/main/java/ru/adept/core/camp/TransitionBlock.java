package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Стационарный переход лагеря. Переход на базу и вызов торговца подключаются в следующих частях среза. */
public class TransitionBlock extends Block {
    public TransitionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("adept_core.transition.idle"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
