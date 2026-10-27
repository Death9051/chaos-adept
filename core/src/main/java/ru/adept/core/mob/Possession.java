package ru.adept.core.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;

import java.util.EnumSet;

/**
 * Одержимость: обычный зверь биома под властью души мира. Получает ауру, усиление здоровья
 * и скорости и собственную атаку (у мирных зверей нет атрибута урона, поэтому атака своя).
 * Метка хранится в данных сущности, цели восстанавливаются после перезагрузки мира.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class Possession {
    public static final String TAG = AdeptCore.MOD_ID + ":possessed";
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "possessed_health");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "possessed_speed");

    private Possession() {
    }

    public static boolean isPossessed(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG);
    }

    /** Сделать зверя одержимым. Вызывать до добавления в мир. */
    public static void apply(PathfinderMob mob) {
        mob.getPersistentData().putBoolean(TAG, true);
        replace(mob.getAttribute(Attributes.MAX_HEALTH), new AttributeModifier(HEALTH_ID,
                BalanceConfig.POSSESSED_HEALTH_MULTIPLIER.get() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        replace(mob.getAttribute(Attributes.MOVEMENT_SPEED), new AttributeModifier(SPEED_ID,
                BalanceConfig.POSSESSED_SPEED_MULTIPLIER.get() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        mob.setHealth(mob.getMaxHealth());
        mob.setPersistenceRequired();
        addGoals(mob);
    }

    /** Создать одержимого зверя у поверхности рядом с {@code near}; null, если тип не подходит. */
    public static PathfinderMob spawnPossessed(ServerLevel level, EntityType<?> type, BlockPos near, LivingEntity target) {
        Entity entity = type.create(level);
        if (!(entity instanceof PathfinderMob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ());
        mob.moveTo(near.getX() + 0.5, y, near.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
        apply(mob);
        if (target != null) {
            mob.setTarget(target);
        }
        level.addFreshEntity(mob);
        return mob;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity() instanceof PathfinderMob mob && isPossessed(mob)) {
            addGoals(mob);
        }
    }

    /** Аура одержимых — вызывается раз в несколько тиков для мобов волны. */
    public static void aura(ServerLevel level, Entity entity) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6,
                entity.getZ(), 2, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.01);
    }

    private static void addGoals(PathfinderMob mob) {
        mob.goalSelector.addGoal(0, new PossessedAttackGoal(mob));
    }

    private static void replace(AttributeInstance attribute, AttributeModifier modifier) {
        if (attribute != null) {
            attribute.removeModifier(modifier.id());
            attribute.addPermanentModifier(modifier);
        }
    }

    /** Атака одержимого: идти к цели (или ближайшему игроку) и бить при сближении. */
    private static final class PossessedAttackGoal extends Goal {
        private final PathfinderMob mob;
        private int cooldown;

        PossessedAttackGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private LivingEntity target() {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive()) {
                return target;
            }
            Player nearest = mob.level().getNearestPlayer(mob, 24.0);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                mob.setTarget(nearest);
                return nearest;
            }
            return null;
        }

        @Override
        public boolean canUse() {
            return target() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = target();
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(target, 1.3);
            }
            if (cooldown > 0) {
                cooldown--;
            }
            double reach = mob.getBbWidth() * 2.0 + target.getBbWidth() + 0.6;
            if (cooldown <= 0 && mob.distanceToSqr(target) <= reach * reach) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                target.hurt(mob.damageSources().mobAttack(mob), BalanceConfig.POSSESSED_DAMAGE.get().floatValue());
                cooldown = 20;
            }
        }
    }
}
