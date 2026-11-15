package ru.adept.core.rune;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.RuneConfig;
import ru.adept.core.mana.AccumulatorItem;
import ru.adept.core.mana.ArtifactPower;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Руны на снарядах (иглы гаусс-пушки, болты рельсы): срабатывают при попадании. Основа — эффект первой руны,
 * грань — как её меняет вторая; особое слово заменяет обе. Урон и длительности умножаются на множитель материала,
 * модификатор материала добавляет своё («Цепь», «Отклик», «Неугасимость», «Ясность», «Удача», «Мгновенность»).
 * Все эффекты — из ванильных средств: урон, эффекты зелий, толчки, частицы.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class RuneEffects {
    public static final ResourceLocation VOID_ID = ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "rune_void");
    private static final String FIRE = "adept_rune_fire_until";
    private static final String FROST = "adept_rune_frost_until";
    private static final String LINK = "adept_rune_link";
    private static final String LINK_UNTIL = "adept_rune_link_until";
    private static final String MULT = "adept_rune_mult";
    private static boolean linking;

    /** Всё, что нужно эффектам одного попадания. */
    private static final class Ctx {
        ServerLevel level;
        Player shooter;
        RuneSet set;
        double mult;
        double time;
        double zone;
        Vec3 dir;
        double damage;
        boolean pierceArmor;
    }

    private RuneEffects() {
    }

    /**
     * Попадание гравированного снаряда. {@code target} — цель (может быть null при попадании в блок),
     * {@code point} — точка попадания, {@code dir} — направление выстрела, {@code hitDamage} — урон самого снаряда.
     */
    public static void onHit(ServerLevel level, Player shooter, LivingEntity target, Vec3 point, Vec3 dir, RuneSet set, double hitDamage) {
        if (set.isEmpty() || shooter == null) {
            return;
        }
        Ctx c = new Ctx();
        c.level = level;
        c.shooter = shooter;
        c.set = set;
        c.mult = set.multiplier();
        c.time = set.modifier(RuneMaterial.NETHERITE) ? 1.0 + RuneConfig.MOD_DURATION.get() : 1.0;
        c.zone = set.modifier(RuneMaterial.DIAMOND) ? 1.0 + RuneConfig.MOD_ZONE.get() : 1.0;
        c.dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        c.damage = hitDamage;
        c.pierceArmor = set.word() == null && set.second() == Rune.VOID;
        int times = set.word() != null && set.modifier(RuneMaterial.EMERALD) && level.random.nextDouble() < RuneConfig.MOD_LUCK.get() ? 2 : 1;
        for (int i = 0; i < times; i++) {
            once(c, target, point);
        }
        if (set.modifier(RuneMaterial.GOLD)) {
            var acc = AccumulatorItem.find(shooter);
            if (!acc.isEmpty()) {
                AccumulatorItem.receive(acc, ArtifactPower.Resource.MANA, RuneConfig.MOD_ECHO_MANA.get(), AccumulatorItem.capacity(shooter));
            }
        }
        if (set.modifier(RuneMaterial.COPPER) && set.word() == null && level.random.nextDouble() < RuneConfig.MOD_CHAIN_CHANCE.get()) {
            LivingEntity next = nearest(c, point, target);
            if (next != null) {
                base(c, set.first(), next, next.position(), RuneConfig.RUNE_FACET_SHARE.get());
            }
        }
    }

    private static void once(Ctx c, LivingEntity target, Vec3 point) {
        RuneWord word = c.set.word();
        if (word != null) {
            word(c, word, target, point);
            return;
        }
        Rune second = c.set.second();
        if (second == Rune.PULL) {
            pull(c, point, RuneConfig.RUNE_FACET_SHARE.get());
        }
        base(c, c.set.first(), target, point, 1.0);
        if (second != null) {
            facet(c, second, target, point);
        }
    }

    // ---- основы ----

    private static void base(Ctx c, Rune rune, LivingEntity target, Vec3 point, double scale) {
        double s = c.mult * scale;
        switch (rune) {
            case CURRENT -> {
                List<LivingEntity> near = enemies(c, point, RuneConfig.RUNE_RADIUS.get(), target);
                for (int i = 0; i < Math.min(2, near.size()); i++) {
                    LivingEntity e = near.get(i);
                    line(c.level, point, e.position().add(0, e.getBbHeight() / 2, 0), ParticleTypes.ELECTRIC_SPARK);
                    hurt(c, e, RuneConfig.RUNE_ARC_DAMAGE.get() * s);
                }
            }
            case FLAME -> {
                if (target != null) {
                    int ticks = (int) Math.round(RuneConfig.RUNE_FIRE_SECONDS.get() * 20 * c.time * scale);
                    target.igniteForTicks(ticks);
                    mark(target, FIRE, ticks, s);
                }
            }
            case BOND -> {
                if (target != null) {
                    LivingEntity partner = nearest(c, target.position(), target);
                    if (partner != null) {
                        int ticks = (int) Math.round(RuneConfig.RUNE_LINK_SECONDS.get() * 20 * c.time * scale);
                        link(target, partner, ticks, s);
                        link(partner, target, ticks, s);
                        line(c.level, target.position().add(0, 1, 0), partner.position().add(0, 1, 0), ParticleTypes.ENCHANT);
                    }
                }
            }
            case ECHO -> {
                if (target != null) {
                    int delay = c.set.modifier(RuneMaterial.REDSTONE) ? 0 : RuneConfig.RUNE_ECHO_DELAY.get();
                    double dmg = c.damage * RuneConfig.RUNE_ECHO_SHARE.get() * s;
                    RuneScheduler.later(c.level, delay, () -> {
                        if (target.isAlive()) {
                            hurt(c, target, dmg);
                            c.level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                        }
                    });
                }
            }
            case RIFT -> {
                double radius = RuneConfig.RUNE_ZONE_RADIUS.get() * c.zone;
                int pulses = (int) Math.round(RuneConfig.RUNE_ZONE_SECONDS.get() * 2 * c.zone * c.time);
                RuneScheduler.repeat(c.level, 10, pulses, () -> {
                    c.level.sendParticles(ParticleTypes.CRIT, point.x, point.y + 0.1, point.z, 12, radius / 2, 0.05, radius / 2, 0.0);
                    for (LivingEntity e : enemies(c, point, radius, null)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1), c.shooter);
                        hurt(c, e, RuneConfig.RUNE_ZONE_DAMAGE.get() * s);
                    }
                });
            }
            case PULL -> pull(c, point, scale);
            case FROST -> {
                if (target != null) {
                    freeze(c, target, scale);
                }
            }
            case VOID -> {
                if (target != null) {
                    AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
                    if (armor != null) {
                        armor.removeModifier(VOID_ID);
                        armor.addTransientModifier(new AttributeModifier(VOID_ID, -Math.min(1.0, RuneConfig.RUNE_VOID_ARMOR.get() * scale),
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        RuneScheduler.later(c.level, (int) Math.round(RuneConfig.RUNE_VOID_SECONDS.get() * 20 * c.time * scale),
                                () -> armor.removeModifier(VOID_ID));
                    }
                    c.level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }
    }

    // ---- грани ----

    private static void facet(Ctx c, Rune rune, LivingEntity target, Vec3 point) {
        double s = RuneConfig.RUNE_FACET_SHARE.get();
        Rune first = c.set.first();
        switch (rune) {
            case CURRENT, BOND -> {
                LivingEntity next = nearest(c, point, target);
                if (next != null) {
                    line(c.level, point, next.position().add(0, 1, 0), rune == Rune.CURRENT ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANT);
                    base(c, first, next, next.position(), s);
                }
            }
            case FLAME -> {
                double radius = 1.5 * c.zone;
                int pulses = (int) Math.round(RuneConfig.RUNE_ZONE_SECONDS.get() * 2 * c.zone * c.time);
                RuneScheduler.repeat(c.level, 10, pulses, () -> {
                    c.level.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.1, point.z, 6, radius / 2, 0.05, radius / 2, 0.01);
                    for (LivingEntity e : enemies(c, point, radius, null)) {
                        e.igniteForTicks(40);
                    }
                });
            }
            case ECHO -> {
                int delay = c.set.modifier(RuneMaterial.REDSTONE) ? 0 : RuneConfig.RUNE_ECHO_DELAY.get();
                RuneScheduler.later(c.level, delay, () -> base(c, first, target != null && target.isAlive() ? target : null, point, s));
            }
            case RIFT -> {
                if (first == Rune.RIFT) {
                    base(c, Rune.RIFT, target, point, s);
                    return;
                }
                double radius = RuneConfig.RUNE_ZONE_RADIUS.get() * c.zone;
                int pulses = (int) Math.round(RuneConfig.RUNE_ZONE_SECONDS.get() * c.zone * c.time);
                RuneScheduler.repeat(c.level, 20, pulses, () -> {
                    c.level.sendParticles(ParticleTypes.CRIT, point.x, point.y + 0.1, point.z, 8, radius / 2, 0.05, radius / 2, 0.0);
                    List<LivingEntity> in = enemies(c, point, radius, null);
                    for (int i = 0; i < Math.min(3, in.size()); i++) {
                        base(c, first, in.get(i), in.get(i).position(), s);
                    }
                });
            }
            case PULL, VOID -> {
                // Тяга стягивает врагов до основы, Пустота делает урон основы сквозь броню — учтено в once() и hurt().
            }
            case FROST -> {
                if (target != null) {
                    freeze(c, target, s);
                }
            }
        }
    }

    // ---- особые слова ----

    private static void word(Ctx c, RuneWord word, LivingEntity target, Vec3 point) {
        double dmg = RuneConfig.WORD_DAMAGE.get() * c.mult;
        double radius = RuneConfig.WORD_RADIUS.get() * c.zone;
        int seconds = (int) Math.max(1, Math.round(RuneConfig.WORD_SECONDS.get() * c.time * c.zone));
        switch (word) {
            case STORM -> RuneScheduler.repeat(c.level, 20, seconds + 1, () -> {
                c.level.sendParticles(ParticleTypes.CLOUD, point.x, point.y + 3, point.z, 10, radius / 2, 0.2, radius / 2, 0.0);
                List<LivingEntity> in = enemies(c, point, radius, null);
                if (!in.isEmpty()) {
                    LivingEntity e = in.get(c.level.random.nextInt(in.size()));
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(c.level);
                    if (bolt != null) {
                        bolt.moveTo(e.getX(), e.getY(), e.getZ());
                        bolt.setVisualOnly(true);
                        c.level.addFreshEntity(bolt);
                    }
                    hurt(c, e, dmg);
                }
            });
            case ERUPTION -> RuneScheduler.repeat(c.level, 10, seconds * 2, () -> {
                c.level.sendParticles(ParticleTypes.LAVA, point.x, point.y + 0.2, point.z, 4, 0.3, 0.1, 0.3, 0.0);
                for (LivingEntity e : enemies(c, point, radius, null)) {
                    e.igniteForTicks(40);
                    hurt(c, e, dmg * 0.25);
                }
            });
            case ARCANE -> {
                List<LivingEntity> pulled = enemies(c, point, radius * 2, null);
                List<LivingEntity> group = new java.util.ArrayList<>();
                if (target != null) {
                    group.add(target);
                }
                for (LivingEntity e : pulled) {
                    if (group.size() >= 3) {
                        break;
                    }
                    if (e != target) {
                        group.add(e);
                    }
                }
                for (LivingEntity e : group) {
                    push(e, point.subtract(e.position()).normalize().scale(1.2));
                }
                RuneScheduler.later(c.level, 8, () -> {
                    for (LivingEntity e : group) {
                        if (e.isAlive()) {
                            stun(c, e, 40);
                            hurt(c, e, dmg * 0.5);
                        }
                    }
                });
            }
            case DOUBLE -> {
                Vec3 end = point.add(c.dir.scale(16));
                for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, new AABB(point, end).inflate(1.0),
                        e -> enemy(c, e) && e != target && e.getBoundingBox().inflate(0.3).clip(point, end).isPresent())) {
                    hurt(c, e, c.damage * 0.5 * c.mult);
                }
                line(c.level, point, end, ParticleTypes.END_ROD);
            }
            case GLACIER -> RuneScheduler.repeat(c.level, 10, seconds * 2, () -> {
                c.level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y + 0.2, point.z, 10, radius / 2, 0.1, radius / 2, 0.0);
                for (LivingEntity e : enemies(c, point, radius, null)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 9), c.shooter);
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 20));
                }
            });
            case BLACK_HOLE -> RuneScheduler.repeat(c.level, 5, seconds * 4, () -> {
                c.level.sendParticles(ParticleTypes.PORTAL, point.x, point.y + 0.5, point.z, 10, radius / 3, radius / 3, radius / 3, 0.2);
                for (LivingEntity e : enemies(c, point, radius * 1.5, null)) {
                    push(e, point.subtract(e.position()).normalize().scale(0.35));
                }
                for (Projectile p : c.level.getEntitiesOfClass(Projectile.class, new AABB(point, point).inflate(radius),
                        p -> p.getOwner() != c.shooter)) {
                    p.setDeltaMovement(point.subtract(p.position()).normalize().scale(0.2));
                }
            });
            case SUPERCONDUCTOR -> {
                for (LivingEntity e : enemies(c, point, radius * 2, null)) {
                    line(c.level, point, e.position().add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK);
                    hurt(c, e, dmg * 0.75);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3), c.shooter);
                }
            }
            case ASH -> {
                if (target != null) {
                    int ticks = (int) Math.round(5 * 20 * c.time);
                    target.igniteForTicks(ticks);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1), c.shooter);
                }
            }
            case PIT -> {
                c.level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y + 0.2, point.z, 1, 0, 0, 0, 0);
                for (LivingEntity e : enemies(c, point, radius, null)) {
                    Vec3 v = e.getDeltaMovement();
                    e.setDeltaMovement(v.x, 1.1, v.z);
                    e.hurtMarked = true;
                }
            }
            case SHARDS -> {
                if (target != null) {
                    freeze(c, target, 1.0);
                    RuneScheduler.later(c.level, 20, () -> {
                        Vec3 at = target.position();
                        c.level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y + 1, at.z, 30, radius / 2, 0.5, radius / 2, 0.2);
                        for (LivingEntity e : enemies(c, at, radius, null)) {
                            hurt(c, e, dmg);
                        }
                    });
                }
            }
        }
    }

    // ---- помощники ----

    private static void pull(Ctx c, Vec3 point, double scale) {
        for (LivingEntity e : enemies(c, point, RuneConfig.RUNE_RADIUS.get() * 1.25, null)) {
            push(e, point.subtract(e.position()).normalize().scale(RuneConfig.RUNE_PULL.get() * scale));
        }
        c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y + 0.5, point.z, 15, 0.5, 0.5, 0.5, 0.1);
    }

    private static void freeze(Ctx c, LivingEntity target, double scale) {
        int ticks = (int) Math.round(RuneConfig.RUNE_FROST_SECONDS.get() * 20 * c.time * scale);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3), c.shooter);
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        mark(target, FROST, ticks, c.mult * scale);
        c.level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
    }

    private static void stun(Ctx c, LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9), c.shooter);
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4), c.shooter);
    }

    private static void push(Entity e, Vec3 v) {
        e.setDeltaMovement(e.getDeltaMovement().add(v.x, Math.max(v.y, 0.1), v.z));
        e.hurtMarked = true;
    }

    private static void hurt(Ctx c, LivingEntity e, double amount) {
        if (amount <= 0 || !e.isAlive()) {
            return;
        }
        DamageSource source = c.pierceArmor
                ? c.level.damageSources().indirectMagic(c.shooter, c.shooter)
                : c.level.damageSources().playerAttack(c.shooter);
        e.invulnerableTime = 0;
        e.hurt(source, (float) amount);
    }

    static boolean enemy(Ctx c, LivingEntity e) {
        return e.isAlive() && e != c.shooter && !(e instanceof Player) && !(e instanceof ArmorStand)
                && !(e instanceof TamableAnimal t && t.isTame());
    }

    private static List<LivingEntity> enemies(Ctx c, Vec3 point, double radius, LivingEntity except) {
        List<LivingEntity> out = new java.util.ArrayList<>(c.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(point, point).inflate(radius), e -> enemy(c, e) && e != except && e.distanceToSqr(point) <= radius * radius));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(point)));
        return out;
    }

    private static LivingEntity nearest(Ctx c, Vec3 point, LivingEntity except) {
        List<LivingEntity> near = enemies(c, point, RuneConfig.RUNE_RADIUS.get() * 1.5, except);
        return near.isEmpty() ? null : near.get(0);
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        int steps = (int) Math.max(2, from.distanceTo(to) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void mark(LivingEntity e, String key, int ticks, double mult) {
        CompoundTag d = e.getPersistentData();
        d.putLong(key, e.level().getGameTime() + ticks);
        d.putDouble(MULT, mult);
    }

    private static void link(LivingEntity from, LivingEntity to, int ticks, double mult) {
        CompoundTag d = from.getPersistentData();
        d.putUUID(LINK, to.getUUID());
        d.putLong(LINK_UNTIL, from.level().getGameTime() + ticks);
        d.putDouble(MULT, mult);
    }

    /** Узы: урон по связанной цели частью уходит связанному врагу. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity e = event.getEntity();
        if (linking || !(e.level() instanceof ServerLevel level) || event.getNewDamage() <= 0) {
            return;
        }
        CompoundTag d = e.getPersistentData();
        if (!d.hasUUID(LINK) || d.getLong(LINK_UNTIL) < level.getGameTime()) {
            return;
        }
        UUID id = d.getUUID(LINK);
        if (level.getEntity(id) instanceof LivingEntity partner && partner.isAlive()) {
            linking = true;
            try {
                partner.invulnerableTime = 0;
                partner.hurt(level.damageSources().magic(), (float) (event.getNewDamage() * RuneConfig.RUNE_LINK_SHARE.get() * d.getDouble(MULT)));
            } finally {
                linking = false;
            }
        }
    }

    /** Пламя — огонь переходит на соседей при смерти цели; Иней — разбитая замёрзшая цель ранит соседей. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag d = e.getPersistentData();
        long now = level.getGameTime();
        double radius = RuneConfig.RUNE_RADIUS.get();
        boolean fire = d.getLong(FIRE) > now;
        boolean frost = d.getLong(FROST) > now;
        if (!fire && !frost) {
            return;
        }
        for (LivingEntity n : level.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(radius),
                n -> n != e && n.isAlive() && !(n instanceof Player) && !(n instanceof ArmorStand))) {
            if (fire) {
                n.igniteForTicks(RuneConfig.RUNE_FIRE_SECONDS.get() * 20);
            }
            if (frost) {
                n.invulnerableTime = 0;
                n.hurt(level.damageSources().freeze(), (float) (RuneConfig.RUNE_SHATTER_DAMAGE.get() * Math.max(0.1, d.getDouble(MULT))));
            }
        }
        if (frost) {
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, e.getX(), e.getY() + 1, e.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
        }
    }
}
