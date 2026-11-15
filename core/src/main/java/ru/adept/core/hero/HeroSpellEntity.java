package ru.adept.core.hero;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ru.adept.core.config.HeroConfig;
import ru.adept.core.registry.ModEntities;

import java.util.Locale;

/**
 * Снаряд стихии мага. Стихия — по краю, где стоит маг: огонь в жарких биомах (поджигает), лёд в холодных
 * (замедляет и морозит), природа в остальных (травит). Летит почти прямо, рисуется ванильным рендером
 * брошенного предмета (огненный заряд, снежок, слизь).
 */
public class HeroSpellEntity extends ThrowableItemProjectile {
    public enum Element {
        FIRE(Items.FIRE_CHARGE), FROST(Items.SNOWBALL), NATURE(Items.SLIME_BALL);

        public final Item item;

        Element(Item item) {
            this.item = item;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Стихия края по биому: жаркий — огонь, холодный — лёд, остальное — природа. */
        public static Element of(Holder<Biome> biome) {
            float t = biome.value().getBaseTemperature();
            if (t >= 1.0f) {
                return FIRE;
            }
            if (t < 0.3f) {
                return FROST;
            }
            return NATURE;
        }

        public static Element ofItem(ItemStack stack) {
            for (Element e : values()) {
                if (stack.is(e.item)) {
                    return e;
                }
            }
            return NATURE;
        }
    }

    public HeroSpellEntity(EntityType<? extends HeroSpellEntity> type, Level level) {
        super(type, level);
    }

    public HeroSpellEntity(Level level, LivingEntity owner, Element element) {
        super(ModEntities.HERO_SPELL.get(), owner, level);
        setItem(new ItemStack(element.item));
    }

    public Element element() {
        return Element.ofItem(getItem());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.005;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && tickCount % 2 == 0) {
            server.sendParticles(switch (element()) {
                case FIRE -> ParticleTypes.FLAME;
                case FROST -> ParticleTypes.SNOWFLAKE;
                case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            }, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }
        if (tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target) || target == getOwner() || target instanceof HeroEntity) {
            return;
        }
        LivingEntity owner = getOwner() instanceof LivingEntity l ? l : null;
        target.hurt(damageSources().indirectMagic(this, owner), HeroConfig.MAGE_BOLT_DAMAGE.get().floatValue());
        int ticks = HeroConfig.MAGE_ELEMENT_TICKS.get();
        switch (element()) {
            case FIRE -> target.igniteForTicks(ticks);
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1), owner);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
            }
            case NATURE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0), owner);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
