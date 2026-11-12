package ru.adept.core.mana;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.config.ManaConfig;

import java.util.UUID;

/**
 * Душа на земле: лёгкая сущность по образцу сферы опыта. Хранит силу, число душ (слившиеся рядом души одной силы
 * складываются), убийцу и возраст. Исчезает через {@code soulLifetimeTicks}. Подбирает только котёл убийцы:
 * в радиусе сбора душа летит к игроку и уходит в горнило или сосуд. Вид — сгусток ванильных частиц душ
 * (заглушка 0,4 × 0,4; модель — по желанию «Моделей» позже).
 */
public class SoulEntity extends Entity {
    private static final EntityDataAccessor<Integer> STRENGTH = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COUNT = SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.INT);

    private UUID owner;
    private int age;

    public SoulEntity(EntityType<? extends SoulEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public static SoulEntity create(Level level, Vec3 pos, SoulStrength strength, int count, UUID owner) {
        SoulEntity soul = new SoulEntity(ru.adept.core.registry.ModEntities.SOUL.get(), level);
        soul.setPos(pos);
        soul.setStrength(strength);
        soul.setCount(count);
        soul.owner = owner;
        return soul;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STRENGTH, 0);
        builder.define(COUNT, 1);
    }

    public SoulStrength strength() {
        return SoulStrength.byIndex(entityData.get(STRENGTH));
    }

    public void setStrength(SoulStrength strength) {
        entityData.set(STRENGTH, strength.ordinal());
    }

    public int count() {
        return entityData.get(COUNT);
    }

    public void setCount(int count) {
        entityData.set(COUNT, Math.max(0, count));
    }

    public UUID owner() {
        return owner;
    }

    public int age() {
        return age;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (++age >= ManaConfig.SOUL_LIFETIME_TICKS.get() || count() <= 0) {
            discard();
            return;
        }
        if (age % 10 == 0) {
            mergeNearby();
        }
        Player player = owner == null ? null : level().getPlayerByUUID(owner);
        Vec3 motion = Vec3.ZERO;
        if (player != null && player.isAlive() && !player.isSpectator()) {
            ItemStack cauldron = SoulCauldronItem.find(player);
            double radius = ManaConfig.SOUL_PICKUP_RADIUS.get();
            Vec3 target = player.position().add(0, player.getBbHeight() * 0.5, 0);
            double dist = target.distanceTo(position());
            if (!cauldron.isEmpty() && dist <= radius) {
                if (dist < 1.0) {
                    int taken = SoulCauldronItem.collect(cauldron, strength(), count());
                    setCount(count() - taken);
                    if (count() <= 0) {
                        discard();
                        return;
                    }
                } else {
                    motion = target.subtract(position()).normalize().scale(0.35);
                }
            }
        }
        if (motion == Vec3.ZERO) {
            // Лёгкое парение на месте.
            motion = new Vec3(0, Math.sin((age + getId()) * 0.1) * 0.01, 0);
        }
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    /** Души одной силы и одного убийцы рядом сливаются в эту. */
    private void mergeNearby() {
        double r = ManaConfig.SOUL_MERGE_RADIUS.get();
        if (r <= 0 || !(level() instanceof ServerLevel)) {
            return;
        }
        for (SoulEntity other : level().getEntitiesOfClass(SoulEntity.class, getBoundingBox().inflate(r))) {
            if (other != this && other.isAlive() && other.strength() == strength()
                    && java.util.Objects.equals(other.owner, owner) && other.getId() > getId()) {
                setCount(count() + other.count());
                age = Math.min(age, other.age);
                other.discard();
            }
        }
    }

    private void clientParticles() {
        if (tickCount % 3 != 0) {
            return;
        }
        double spread = 0.15 + 0.05 * strength().ordinal();
        Level level = level();
        level.addParticle(strength() == SoulStrength.NORMAL ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL,
                getX() + (random.nextDouble() - 0.5) * spread, getY() + 0.2 + (random.nextDouble() - 0.5) * spread,
                getZ() + (random.nextDouble() - 0.5) * spread, 0, 0.01, 0);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStrength(SoulStrength.byIndex(tag.getInt("strength")));
        setCount(tag.getInt("count"));
        age = tag.getInt("age");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("strength", strength().ordinal());
        tag.putInt("count", count());
        tag.putInt("age", age);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }
}
