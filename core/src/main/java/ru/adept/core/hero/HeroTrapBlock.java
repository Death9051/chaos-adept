package ru.adept.core.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.adept.core.config.BalanceConfig;

/** Капкан следопыта: наступивший игрок обездвижен на секунду и ранен, капкан ломается. Без отброса. */
public class HeroTrapBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);

    public HeroTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceConfig.HERO_TRAP_HOLD_TICKS.get(), 9));
        player.hurt(level.damageSources().generic(), BalanceConfig.HERO_TRAP_DAMAGE.get().floatValue());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
