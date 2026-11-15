package ru.adept.core.hero;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.HeroEvent;

/** Смерть героев (отряд) и промахи стрел (событие «промах»). */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class HeroEvents {
    private HeroEvents() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof HeroEntity hero && hero.level() instanceof ServerLevel level) {
            if (tryRevive(level, hero)) {
                event.setCanceled(true);
                return;
            }
            HeroSquads.get(level.getServer()).onMemberDied(level, hero);
        }
    }

    /**
     * Целитель-мастер того же отряда рядом поднимает павшего (раз за бой): смерть отменяется,
     * боец встаёт с долей здоровья {@code reviveHealth}. Вернёт true, если подъём случился.
     */
    public static boolean tryRevive(ServerLevel level, HeroEntity fallen) {
        if (fallen.squad() == null) {
            return false;
        }
        double r = ru.adept.core.config.HeroConfig.REVIVE_RADIUS.get();
        for (HeroEntity healer : level.getEntitiesOfClass(HeroEntity.class, fallen.getBoundingBox().inflate(r),
                h -> h != fallen && h.canRevive() && fallen.squad().equals(h.squad()))) {
            healer.markReviveUsed();
            fallen.setHealth(Math.max(1.0f, fallen.getMaxHealth() * ru.adept.core.config.HeroConfig.REVIVE_HEALTH.get().floatValue()));
            fallen.removeAllEffects();
            fallen.clearFire();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, fallen.getX(), fallen.getY() + 1, fallen.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
            fallen.playSound(net.minecraft.sounds.SoundEvents.TOTEM_USE, 0.8f, 1.2f);
            Player target = fallen.getTarget() instanceof Player p ? p : null;
            healer.post(h -> new HeroEvent.AllyRevived(h, h.heroClass().id(), h.rank().id(), h.squad(), target, fallen));
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(projectile.getOwner() instanceof HeroEntity hero)
                || !(hero.getTarget() instanceof Player target)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        boolean hitPlayer = hit instanceof EntityHitResult e && e.getEntity() == target;
        if (!hitPlayer && hit.getType() != HitResult.Type.MISS) {
            hero.post(h -> new HeroEvent.Missed(h, h.heroClass().id(), h.rank().id(), h.squad(), target));
        }
    }
}
