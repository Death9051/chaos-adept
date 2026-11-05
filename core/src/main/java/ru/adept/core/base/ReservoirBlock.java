package ru.adept.core.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Блок резервуара: ПКМ показывает содержимое; ломается вместе с расплавом (он остаётся в предмете). */
public class ReservoirBlock extends BaseEntityBlock {
    public static final MapCodec<ReservoirBlock> CODEC = simpleCodec(ReservoirBlock::new);

    public ReservoirBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReservoirBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) {
            player.displayClientMessage(r.amount() <= 0
                    ? Component.translatable("adept_core.reservoir.empty", ReservoirBlockEntity.capacity())
                    : Component.translatable("adept_core.reservoir.content",
                    Component.translatable("adept_core.molten." + r.material()), r.amount(), ReservoirBlockEntity.capacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("amount") && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) {
            r.set(tag.getString("material"), tag.getInt("amount"));
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) {
            ItemStack drop = new ItemStack(this);
            if (r.amount() > 0) {
                CompoundTag tag = new CompoundTag();
                tag.putString("material", r.material());
                tag.putInt("amount", r.amount());
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
