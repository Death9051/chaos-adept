package ru.adept.core.mob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import ru.adept.core.config.BalanceConfig;

/** Шершень (рой): стая, жалит и травит ядом. */
public class HornetEntity extends FlyingNodeMob {
    public HornetEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected double flySpeed() {
        return 0.09;
    }

    @Override
    protected int onReach(LivingEntity target) {
        if (target.hurt(damageSources().mobAttack(this), BalanceConfig.HORNET_DAMAGE.get().floatValue())) {
            int poison = BalanceConfig.HORNET_POISON_TICKS.get();
            if (poison > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poison), this);
            }
        }
        return 20;
    }
}
