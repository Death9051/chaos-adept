package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Стационарный переход лагеря: проход на базу и вызов торговца Альянса. */
public class TransitionBlock extends Block {
    private static final Component TITLE = Component.translatable("block.adept_core.transition");

    public TransitionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                    new TransitionMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
