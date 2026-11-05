package ru.adept.core.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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

import java.util.EnumSet;

/**
 * Кабан-вожак (танк): с дистанции 6–16 блоков роет землю (подготовка) и бросается по прямой.
 * Попал — бьёт тараном с усиленным уроном (без отброса игрока). Влетел в стену — оглушён.
 * Размер заглушки = размер будущей модели: 1,4 × 1,2.
 */
public class BoarLeaderEntity extends Monster {
    private enum Phase { NONE, WINDUP, CHARGE, STUNNED }

    private Phase phase = Phase.NONE;
    private int phaseTicks;
    private int cooldown;
    private Vec3 chargeDir = Vec3.ZERO;

    public BoarLeaderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isCharging() {
        return phase == Phase.CHARGE;
    }

    public boolean isStunned() {
        return phase == Phase.STUNNED;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (cooldown > 0) {
            cooldown--;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("adept_charge_cd", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("adept_charge_cd");
    }

    /** Разгон и таран. Пока цель цела и в зоне — фаза за фазой: подготовка → бросок → (оглушение). */
    private final class ChargeGoal extends Goal {
        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || cooldown > 0 || !onGround()) {
                return false;
            }
            double d = distanceTo(target);
            return d >= BalanceConfig.BOAR_CHARGE_MIN_RANGE.get() && d <= BalanceConfig.BOAR_CHARGE_MAX_RANGE.get() && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return phase != Phase.NONE;
        }

        @Override
        public void start() {
            phase = Phase.WINDUP;
            phaseTicks = BalanceConfig.BOAR_WINDUP_TICKS.get();
            getNavigation().stop();
        }

        @Override
        public void stop() {
            phase = Phase.NONE;
            cooldown = BalanceConfig.BOAR_CHARGE_COOLDOWN_TICKS.get();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            phaseTicks--;
            switch (phase) {
                case WINDUP -> {
                    if (target != null) {
                        getLookControl().setLookAt(target, 30.0f, 30.0f);
                    }
                    if (level() instanceof ServerLevel server && phaseTicks % 4 == 0) {
                        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 3, 0.4, 0.05, 0.4, 0.02);
                    }
                    if (phaseTicks <= 0) {
                        Vec3 to = target != null ? target.position().subtract(position()) : getLookAngle();
                        chargeDir = new Vec3(to.x, 0, to.z).normalize();
                        phase = Phase.CHARGE;
                        phaseTicks = BalanceConfig.BOAR_CHARGE_TICKS.get();
                    }
                }
                case CHARGE -> {
                    double speed = BalanceConfig.BOAR_CHARGE_SPEED.get();
                    setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
                    setYRot((float) (Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z))));
                    yBodyRot = getYRot();
                    for (LivingEntity hit : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3),
                            e -> e != BoarLeaderEntity.this && e.isAlive() && !(e instanceof BoarLeaderEntity))) {
                        float damage = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * BalanceConfig.BOAR_RAM_MULTIPLIER.get());
                        hit.hurt(damageSources().mobAttack(BoarLeaderEntity.this), damage);
                        phase = Phase.NONE;
                        return;
                    }
                    if (horizontalCollision) {
                        phase = Phase.STUNNED;
                        phaseTicks = BalanceConfig.BOAR_STUN_TICKS.get();
                        setDeltaMovement(Vec3.ZERO);
                    } else if (phaseTicks <= 0) {
                        phase = Phase.NONE;
                    }
                }
                case STUNNED -> {
                    setDeltaMovement(0, getDeltaMovement().y, 0);
                    if (level() instanceof ServerLevel server && phaseTicks % 5 == 0) {
                        server.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight(), getZ(), 2, 0.3, 0.1, 0.3, 0.0);
                    }
                    if (phaseTicks <= 0) {
                        phase = Phase.NONE;
                    }
                }
                default -> {
                }
            }
        }
    }
}
