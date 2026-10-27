package ru.adept.core.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import ru.adept.core.config.BalanceConfig;

/** Блуждающий огонёк (рой): взрывается рядом с игроком, блоки не ломает. */
public class WispEntity extends FlyingNodeMob {
    public WispEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected double flySpeed() {
        return 0.06;
    }

    @Override
    protected double reach() {
        return 2.2;
    }

    @Override
    protected int onReach(LivingEntity target) {
        level().explode(this, getX(), getY(), getZ(), BalanceConfig.WISP_EXPLOSION.get().floatValue(), Level.ExplosionInteraction.NONE);
        discard();
        return 0;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
