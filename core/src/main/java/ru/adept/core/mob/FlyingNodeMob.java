package ru.adept.core.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Летучий противник узла: без гравитации, сам подлетает к цели и действует при сближении.
 * Простое управление без навигации — надёжно и дёшево для мелких стайных мобов.
 */
public abstract class FlyingNodeMob extends Monster {
    private int actionCooldown;

    protected FlyingNodeMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Скорость сближения за тик. */
    protected abstract double flySpeed();

    /** Дистанция, на которой срабатывает {@link #onReach}. */
    protected double reach() {
        return 1.6;
    }

    /** Действие у цели. Возвращает перезарядку в тиках. */
    protected abstract int onReach(LivingEntity target);

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (actionCooldown > 0) {
            actionCooldown--;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.8));
            return;
        }
        Vec3 to = target.getEyePosition().subtract(position().add(0, getBbHeight() / 2, 0));
        double distance = to.length();
        if (distance > 0.01) {
            setDeltaMovement(getDeltaMovement().scale(0.7).add(to.normalize().scale(flySpeed())));
        }
        getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (distance < reach() && actionCooldown <= 0) {
            actionCooldown = onReach(target);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
