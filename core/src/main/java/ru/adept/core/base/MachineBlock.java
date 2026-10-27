package ru.adept.core.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.adept.core.registry.ModBlockEntities;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Блок машины базы. Подклассы задают свою блок-сущность. */
public abstract class MachineBlock extends BaseEntityBlock {
    private final Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory;

    protected MachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type,
                           BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties);
        this.type = type;
        this.factory = factory;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != type.get()) {
            return null;
        }
        return (l, p, s, be) -> ((MachineBlockEntity) be).serverTick();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            player.openMenu(machine);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            Containers.dropContents(level, pos, machine.items());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static final class Generator extends MachineBlock {
        public static final MapCodec<Generator> CODEC = simpleCodec(Generator::new);

        public Generator(Properties properties) {
            super(properties, ModBlockEntities.GENERATOR, GeneratorBlockEntity::new);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    public static final class Crusher extends MachineBlock {
        public static final MapCodec<Crusher> CODEC = simpleCodec(Crusher::new);

        public Crusher(Properties properties) {
            super(properties, ModBlockEntities.CRUSHER, CrusherBlockEntity::new);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    public static final class Smelter extends MachineBlock {
        public static final MapCodec<Smelter> CODEC = simpleCodec(Smelter::new);

        public Smelter(Properties properties) {
            super(properties, ModBlockEntities.SMELTER, SmelterBlockEntity::new);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }
}
