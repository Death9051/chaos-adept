package ru.adept.core.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ru.adept.core.camp.WaveSpawner;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;

/**
 * Споровик (рой): гриб, который при смерти лопается облаком спор и выпускает маленьких споровиков.
 * Маленькие (отдельный тип) уже не делятся. Размеры: 0,8 × 1,1 и 0,4 × 0,55.
 */
public class SporelingEntity extends Monster {
    public SporelingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static AttributeSupplier.Builder smallAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public boolean isSmall() {
        return getType() == ModEntities.SPORELING_SMALL.get();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isSmall() || !(level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.5, getZ(), 30, 0.6, 0.4, 0.6, 0.02);
        int count = BalanceConfig.SPORELING_SPLIT_COUNT.get();
        for (int i = 0; i < count; i++) {
            SporelingEntity child = ModEntities.SPORELING_SMALL.get().create(server);
            if (child == null) {
                continue;
            }
            child.moveTo(getX() + (random.nextDouble() - 0.5), getY(), getZ() + (random.nextDouble() - 0.5), random.nextFloat() * 360f, 0f);
            child.setPersistenceRequired();
            child.setTarget(getTarget());
            server.addFreshEntity(child);
            WaveSpawner.adoptIntoWave(server, this, child);
        }
    }
}
