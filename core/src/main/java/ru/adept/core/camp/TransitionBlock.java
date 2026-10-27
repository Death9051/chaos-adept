package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.base.BaseTravel;

/** Стационарный переход лагеря: бесплатный проход на базу, ключ вернёт к переходу. */
public class TransitionBlock extends Block {
    public TransitionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            BaseTravel.enter(server, Vec3.atBottomCenterOf(pos.above()), player.getYRot());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
