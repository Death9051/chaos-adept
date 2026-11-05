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
            HeroSquads.get(level.getServer()).onMemberDied(level, hero);
        }
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
