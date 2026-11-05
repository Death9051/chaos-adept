package ru.adept.core.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.config.BalanceConfig;

/**
 * Броненосец (танк). Спереди неуязвим: удары и снаряды с передней дуги гасятся панцирем.
 * Получив удар в бок или спину, сворачивается: на время стоит на месте и получает меньше урона со всех сторон.
 * Размер заглушки: 1,0 × 0,8, свёрнутый 0,9 × 0,9.
 */
public class ArmadilloEntity extends Monster {
    private static final EntityDataAccessor<Boolean> CURLED = SynchedEntityData.defineId(ArmadilloEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions CURLED_SIZE = EntityDimensions.scalable(0.9f, 0.9f);

    private int curlTicks;
    private int curlCooldown;

    public ArmadilloEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURLED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !isCurled() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isCurled() {
        return entityData.get(CURLED);
    }

    private void setCurled(boolean curled) {
        entityData.set(CURLED, curled);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isCurled() ? CURLED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CURLED.equals(key)) {
            refreshDimensions();
        }
    }

    /** Удар пришёл с передней дуги (угол между взглядом и направлением на источник меньше {@code frontArc}/2). */
    public boolean isFromFront(DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false;
        }
        Vec3 to = from.subtract(position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
        double cos = facing.dot(flat.normalize());
        return cos >= Math.cos(Math.toRadians(BalanceConfig.ARMADILLO_FRONT_ARC.get() / 2.0));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        if (!isCurled() && isFromFront(source)) {
            playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f);
            return false;
        }
        if (isCurled()) {
            amount *= (float) (1.0 - BalanceConfig.ARMADILLO_CURLED_REDUCTION.get());
        } else if (curlCooldown <= 0 && !level().isClientSide) {
            setCurled(true);
            curlTicks = BalanceConfig.ARMADILLO_CURL_TICKS.get();
            getNavigation().stop();
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isCurled() && super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (curlCooldown > 0) {
            curlCooldown--;
        }
        if (isCurled()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            getNavigation().stop();
            if (--curlTicks <= 0) {
                setCurled(false);
                curlCooldown = BalanceConfig.ARMADILLO_CURL_COOLDOWN_TICKS.get();
            }
        }
    }
}
