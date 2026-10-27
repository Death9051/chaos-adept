package ru.adept.core.mob;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import ru.adept.core.config.BalanceConfig;

/**
 * Рогатый вожак — страж узла лиственного леса (в срезе 1 — страж любого узла).
 * Оплетает лозами (замедление игроков рядом) и зовёт одержимых зверей.
 */
public class HornedLeaderEntity extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private int vinesTimer;
    private int callTimer;

    public HornedLeaderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel server) || getTarget() == null) {
            return;
        }
        if (++vinesTimer >= BalanceConfig.GUARDIAN_VINES_INTERVAL.get()) {
            vinesTimer = 0;
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6.0))) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3), this);
            }
        }
        if (++callTimer >= BalanceConfig.GUARDIAN_CALL_INTERVAL.get()) {
            callTimer = 0;
            for (int i = 0; i < BalanceConfig.GUARDIAN_CALL_COUNT.get(); i++) {
                Possession.spawnPossessed(server, EntityType.WOLF, blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), getTarget());
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }
}
