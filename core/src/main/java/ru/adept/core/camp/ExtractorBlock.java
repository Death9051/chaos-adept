package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Установка выкачки. ПКМ — состояние выкачки; ПКМ предметом ремонта — починка;
 * ПКМ с приседом — свернуть лагерь (лагерь возвращается в инвентарь).
 */
public class ExtractorBlock extends Block {
    public ExtractorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && ExtractionManager.isRepairItem(stack)) {
            if (!level.isClientSide && player instanceof ServerPlayer server) {
                ExtractionManager.repair(server, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                ExtractionManager.fold(server);
            } else {
                ExtractionManager.showStatus(server);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
