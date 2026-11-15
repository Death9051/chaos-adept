package ru.adept.core.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.HeroEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.config.HeroConfig;
import ru.adept.core.mob.MobArcs;
import ru.adept.core.registry.ModBlocks;

import java.util.UUID;

/**
 * Герой — местный защитник, которого жители направляют на игрока между узлами.
 * Облик — модель игрока со скином (пока заглушка скина). Размер 0,6 × 1,8.
 * Ученик владеет первым приёмом класса, ветеран — двумя, мастер — тремя и носит лучшее снаряжение:
 * воин — щит спереди / таран щитом / боевой клич; лучник — держит дистанцию / залп / стрела с цепью;
 * следопыт — метка / капканы / ручной зверь; командир — аура строя / «окружить» / подкрепление;
 * маг — снаряд стихии края / барьер на союзника / корни; целитель — лечение / снятие эффектов / подъём павшего.
 */
public class HeroEntity extends Monster implements RangedAttackMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CLASS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(HeroEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** Ранг для клиента: знак ранга на облике героя (HeroRenderer). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> RANK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(HeroEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private HeroClass heroClass = HeroClass.WARRIOR;
    private HeroRank rank = HeroRank.APPRENTICE;
    private UUID squad;
    private boolean configured;
    private boolean spotted;
    private boolean wounded;
    private boolean lostFired;
    private int lostTicks;
    private int techniqueCooldown;
    /** Перезарядка третьего приёма (мастер) и первого у целителя и мага (лечение, барьер). */
    private int thirdCooldown;
    private int supportCooldown;
    /** Приём «раз за бой» уже был: зверь следопыта, подкрепление командира, подъём павшего у целителя. */
    private boolean onceUsed;
    private int auraTimer;
    private boolean retreating;
    private int retreatTicks;
    private String villageName = "";
    private long villageId;

    public HeroEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLASS, HeroClass.WARRIOR.ordinal());
        builder.define(RANK, HeroRank.APPRENTICE.ordinal());
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // ---- настройка ----

    /** Задать класс, ранг и отряд: снаряжение, здоровье, урон и приёмы. */
    public void setup(HeroClass heroClass, HeroRank rank, UUID squad) {
        this.heroClass = heroClass;
        this.rank = rank;
        this.squad = squad;
        entityData.set(CLASS, heroClass.ordinal());
        entityData.set(RANK, rank.ordinal());
        double health = switch (heroClass) {
            case WARRIOR -> BalanceConfig.HERO_WARRIOR_HEALTH.get();
            case ARCHER -> BalanceConfig.HERO_ARCHER_HEALTH.get();
            case TRACKER -> BalanceConfig.HERO_TRACKER_HEALTH.get();
            case COMMANDER -> BalanceConfig.HERO_COMMANDER_HEALTH.get();
            case MAGE -> HeroConfig.MAGE_HEALTH.get();
            case HEALER -> HeroConfig.HEALER_HEALTH.get();
        } * rankMultiplier(rank);
        double damage = switch (heroClass) {
            case WARRIOR -> BalanceConfig.HERO_WARRIOR_DAMAGE.get();
            case ARCHER -> BalanceConfig.HERO_ARCHER_DAMAGE.get();
            case TRACKER -> BalanceConfig.HERO_TRACKER_DAMAGE.get();
            case COMMANDER -> BalanceConfig.HERO_COMMANDER_DAMAGE.get();
            case MAGE -> HeroConfig.MAGE_DAMAGE.get();
            case HEALER -> HeroConfig.HEALER_DAMAGE.get();
        } * rankMultiplier(rank);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        setHealth(getMaxHealth());
        equip();
        configureGoals();
    }

    /** Множитель здоровья и урона по рангу. */
    public static double rankMultiplier(HeroRank rank) {
        return switch (rank) {
            case APPRENTICE -> 1.0;
            case VETERAN -> BalanceConfig.HERO_VETERAN_MULTIPLIER.get();
            case MASTER -> HeroConfig.MASTER_MULTIPLIER.get();
        };
    }

    /** Снаряжение по классу и рангу; мастер носит лучшее (алмаз и железо вместо кольчуги и кожи). */
    private void equip() {
        boolean master = rank == HeroRank.MASTER;
        switch (heroClass) {
            case WARRIOR -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(master ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(master ? Items.DIAMOND_CHESTPLATE
                        : rank == HeroRank.VETERAN ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
                if (master) {
                    setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
            }
            case ARCHER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(master ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE));
                if (master) {
                    setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                }
            }
            case TRACKER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(master ? Items.DIAMOND_AXE : Items.IRON_AXE));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                if (master) {
                    setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                }
            }
            case COMMANDER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(master ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(master ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(master ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
            }
            case MAGE -> {
                // Посох — стержень ифрита; облик держит скин, брони мало.
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
                if (rank != HeroRank.APPRENTICE) {
                    setItemSlot(EquipmentSlot.HEAD, new ItemStack(master ? Items.GOLDEN_HELMET : Items.LEATHER_HELMET));
                }
                if (master) {
                    setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                }
            }
            case HEALER -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BREEZE_ROD));
                if (rank != HeroRank.APPRENTICE) {
                    setItemSlot(EquipmentSlot.CHEST, new ItemStack(master ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE));
                }
                if (master) {
                    setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                }
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, BalanceConfig.HERO_GEAR_DROP_CHANCE.get().floatValue());
        }
    }

    private void configureGoals() {
        configured = true;
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        if (heroClass == HeroClass.ARCHER) {
            goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0, 30, 18.0f));
        } else if (heroClass == HeroClass.MAGE) {
            int cast = HeroConfig.MAGE_CAST_TICKS.get();
            goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RangedAttackGoal(this, 1.0, cast, cast, 16.0f));
        } else {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        }
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, HeroEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
    }

    public HeroClass heroClass() {
        return level().isClientSide ? HeroClass.values()[Math.floorMod(entityData.get(CLASS), HeroClass.values().length)] : heroClass;
    }

    /** Ранг в синхронизированных данных (то, что видит клиент). */
    public int syncedRank() {
        return entityData.get(RANK);
    }

    public HeroRank rank() {
        return level().isClientSide ? HeroRank.values()[Math.floorMod(entityData.get(RANK), HeroRank.values().length)] : rank;
    }

    public UUID squad() {
        return squad;
    }

    public boolean isRetreating() {
        return retreating;
    }

    /** Приём № {@code n} (1 или 2) доступен по рангу. */
    public boolean has(int n) {
        return rank.techniques >= n;
    }

    // ---- события ----

    private Player targetPlayer() {
        return getTarget() instanceof Player p ? p : null;
    }

    void post(java.util.function.Function<HeroEntity, HeroEvent> make) {
        if (getServer() != null) {
            AdeptEvents.post(make.apply(this));
        }
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (target instanceof Player player && !spotted && !level().isClientSide && !retreating) {
            spotted = true;
            post(h -> new HeroEvent.Spotted(h, heroClass.id(), rank.id(), squad, player));
            if (heroClass == HeroClass.TRACKER) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, BalanceConfig.HERO_MARK_TICKS.get(), 0), this);
            }
            HeroSquads.get(getServer()).onSpotted(this, player);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (heroClass == HeroClass.WARRIOR && getOffhandItem().is(Items.SHIELD)
                && MobArcs.isFromFront(this, source, BalanceConfig.HERO_SHIELD_ARC.get())) {
            playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !wounded && getHealth() < getMaxHealth() / 2 && isAlive() && !level().isClientSide) {
            wounded = true;
            Player p = source.getEntity() instanceof Player sp ? sp : targetPlayer();
            post(h -> new HeroEvent.Wounded(h, heroClass.id(), rank.id(), squad, p));
        }
        return hurt;
    }

    // ---- поведение ----

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (retreating) {
            retreatStep(server);
            return;
        }
        if (techniqueCooldown > 0) {
            techniqueCooldown--;
        }
        if (thirdCooldown > 0) {
            thirdCooldown--;
        }
        if (supportCooldown > 0) {
            supportCooldown--;
        }
        Player player = targetPlayer();
        if (player == null) {
            return;
        }
        // Игрок скрылся: не виден дольше 5 секунд.
        if (getSensing().hasLineOfSight(player)) {
            lostTicks = 0;
            lostFired = false;
        } else if (++lostTicks >= BalanceConfig.HERO_LOST_TICKS.get() && !lostFired) {
            lostFired = true;
            post(h -> new HeroEvent.PlayerLost(h, heroClass.id(), rank.id(), squad, player));
        }
        switch (heroClass) {
            case WARRIOR -> warriorStep(player);
            case ARCHER -> archerStep(server, player);
            case TRACKER -> trackerStep(server, player);
            case COMMANDER -> commanderStep(server, player);
            case MAGE -> mageStep(server, player);
            case HEALER -> healerStep(server, player);
        }
        if (has(3) && thirdCooldown <= 0) {
            masterStep(server, player);
        }
    }

    // ---- маг и целитель (срез 3) ----

    /** Маг держит дистанцию; ветеран — барьер (поглощение) на самого раненого союзника рядом. */
    private void mageStep(ServerLevel server, Player player) {
        double keep = HeroConfig.MAGE_KEEP_DISTANCE.get();
        if (distanceToSqr(player) < keep * keep) {
            Vec3 away = position().subtract(player.position()).normalize().scale(6.0);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
        }
        if (has(2) && supportCooldown <= 0) {
            HeroEntity ally = weakestAlly(server, false);
            if (ally != null && !ally.hasEffect(MobEffects.ABSORPTION)) {
                supportCooldown = HeroConfig.BARRIER_COOLDOWN.get();
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, HeroConfig.BARRIER_TICKS.get(), HeroConfig.BARRIER_LEVEL.get()), this);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, ally.getX(), ally.getY() + 1, ally.getZ(), 30, 0.4, 0.8, 0.4, 0.5);
                playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.3f);
            }
        }
    }

    /** Целитель лечит самого раненого союзника рядом; ветеран снимает с союзников негативные эффекты. */
    private void healerStep(ServerLevel server, Player player) {
        if (supportCooldown <= 0) {
            HeroEntity ally = weakestAlly(server, true);
            if (ally != null) {
                supportCooldown = HeroConfig.HEAL_COOLDOWN.get();
                heal(server, ally, player);
            }
        }
        if (has(2) && techniqueCooldown <= 0) {
            double r = HeroConfig.SUPPORT_RADIUS.get();
            boolean cleansed = false;
            for (HeroEntity ally : server.getEntitiesOfClass(HeroEntity.class, getBoundingBox().inflate(r), HeroEntity::isAlive)) {
                for (MobEffectInstance e : java.util.List.copyOf(ally.getActiveEffects())) {
                    if (e.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                        ally.removeEffect(e.getEffect());
                        ally.clearFire();
                        cleansed = true;
                    }
                }
            }
            if (cleansed) {
                techniqueCooldown = HeroConfig.CLEANSE_COOLDOWN.get();
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 20, r / 3, 0.5, r / 3, 0.02);
            }
        }
    }

    /** Лечение союзника: здоровье, частицы и событие «лечение союзника». */
    public void heal(ServerLevel server, HeroEntity ally, Player player) {
        ally.heal(HeroConfig.HEAL_AMOUNT.get().floatValue());
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, ally.getX(), ally.getY() + 1.8, ally.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        post(h -> new HeroEvent.AllyHealed(h, heroClass.id(), rank.id(), squad, player, ally));
    }

    /**
     * Самый раненый союзник-герой в радиусе поддержки. {@code needHeal} — только те, кто ниже порога лечения;
     * иначе любой раненый (для барьера мага). Себя целитель не лечит.
     */
    private HeroEntity weakestAlly(ServerLevel server, boolean needHeal) {
        double r = HeroConfig.SUPPORT_RADIUS.get();
        HeroEntity best = null;
        float bestRatio = needHeal ? HeroConfig.HEAL_BELOW.get().floatValue() : 1.0f;
        for (HeroEntity ally : server.getEntitiesOfClass(HeroEntity.class, getBoundingBox().inflate(r), h -> h != this && h.isAlive() && !h.retreating)) {
            float ratio = ally.getHealth() / ally.getMaxHealth();
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = ally;
            }
        }
        return best;
    }

    /**
     * Третьи приёмы мастера: воин — боевой клич (сила отряду рядом); лучник — стрела с цепью (обездвиживает);
     * следопыт — ручной зверь края (раз за бой); командир — подкрепление (раз за бой, когда отряд понёс потери
     * или сам он ранен); маг — корни на игрока. Целитель-мастер поднимает павшего — в {@link HeroEvents}.
     */
    private void masterStep(ServerLevel server, Player player) {
        switch (heroClass) {
            case WARRIOR -> {
                if (distanceToSqr(player) < 10 * 10) {
                    thirdCooldown = HeroConfig.WAR_CRY_COOLDOWN.get();
                    double r = HeroConfig.WAR_CRY_RADIUS.get();
                    for (HeroEntity ally : server.getEntitiesOfClass(HeroEntity.class, getBoundingBox().inflate(r), HeroEntity::isAlive)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, HeroConfig.WAR_CRY_TICKS.get(), 0), this);
                    }
                    playSound(SoundEvents.RAVAGER_ROAR, 1.0f, 1.4f);
                }
            }
            case ARCHER -> {
                if (getSensing().hasLineOfSight(player) && distanceToSqr(player) < 24 * 24) {
                    thirdCooldown = HeroConfig.CHAIN_COOLDOWN.get();
                    Arrow chain = shootArrow(player, 0.0f);
                    chain.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HeroConfig.CHAIN_TICKS.get(), 9));
                    playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.8f);
                }
            }
            case TRACKER -> {
                if (!onceUsed) {
                    onceUsed = true;
                    thirdCooldown = 20;
                    net.minecraft.world.entity.animal.Wolf wolf = EntityType.WOLF.create(server);
                    if (wolf != null) {
                        wolf.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
                        EventHooksBridge.finalize(server, wolf);
                        server.addFreshEntity(wolf);
                        wolf.setPersistentAngerTarget(player.getUUID());
                        wolf.startPersistentAngerTimer();
                        wolf.setTarget(player);
                    }
                }
            }
            case COMMANDER -> {
                if (!onceUsed && squad != null) {
                    var s = HeroSquads.get(server.getServer()).byId(squad);
                    if (s.isPresent() && (s.get().fallen > 0 || getHealth() < getMaxHealth() / 2)) {
                        onceUsed = true;
                        thirdCooldown = 20;
                        for (int i = 0; i < HeroConfig.REINFORCEMENTS.get(); i++) {
                            HeroClass c = i % 2 == 0 ? HeroClass.WARRIOR : HeroClass.ARCHER;
                            HeroEntity add = HeroSpawner.spawnHero(server, blockPosition().offset(i * 2 - 1, 0, 2), c, HeroRank.APPRENTICE, s.get());
                            if (add != null) {
                                add.setVillage(villageId, villageName);
                                add.setTarget(player);
                            }
                        }
                        playSound(SoundEvents.RAID_HORN.value(), 2.0f, 1.0f);
                    }
                }
            }
            case MAGE -> {
                if (getSensing().hasLineOfSight(player) && distanceToSqr(player) < 16 * 16) {
                    thirdCooldown = HeroConfig.ROOTS_COOLDOWN.get();
                    int ticks = HeroConfig.ROOTS_TICKS.get();
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9), this);
                    server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState()), player.getX(), player.getY() + 0.3, player.getZ(), 40, 0.4, 0.3, 0.4, 0.1);
                    playSound(SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.6f);
                }
            }
            case HEALER -> {
            }
        }
    }

    /** Целитель-мастер ещё может поднять павшего (раз за бой). */
    public boolean canRevive() {
        return heroClass == HeroClass.HEALER && has(3) && !onceUsed && isAlive() && !retreating;
    }

    public void markReviveUsed() {
        onceUsed = true;
    }

    /** Мост к финализации появления моба (ванильные варианты волка по биому). */
    private static final class EventHooksBridge {
        static void finalize(ServerLevel level, net.minecraft.world.entity.Mob mob) {
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    net.minecraft.world.entity.MobSpawnType.EVENT, null);
        }
    }

    /** Таран щитом (ветеран): рывок к игроку вплотную, оглушение без отброса. */
    private void warriorStep(Player player) {
        if (!has(2) || techniqueCooldown > 0 || distanceToSqr(player) > 9.0) {
            return;
        }
        techniqueCooldown = BalanceConfig.HERO_BASH_COOLDOWN_TICKS.get();
        player.hurt(damageSources().mobAttack(this), BalanceConfig.HERO_BASH_DAMAGE.get().floatValue());
        int stun = BalanceConfig.HERO_BASH_STUN_TICKS.get();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 4), this);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1), this);
        playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.6f);
    }

    /** Лучник держит дистанцию: слишком близко — отходит; ветеран раз в несколько секунд даёт залп из трёх стрел. */
    private void archerStep(ServerLevel server, Player player) {
        double keep = BalanceConfig.HERO_ARCHER_KEEP_DISTANCE.get();
        if (distanceToSqr(player) < keep * keep) {
            Vec3 away = position().subtract(player.position()).normalize().scale(6.0);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
        }
        if (has(2) && techniqueCooldown <= 0 && getSensing().hasLineOfSight(player) && distanceToSqr(player) < 24 * 24) {
            techniqueCooldown = BalanceConfig.HERO_VOLLEY_COOLDOWN_TICKS.get();
            for (int i = -1; i <= 1; i++) {
                shootArrow(player, i * BalanceConfig.HERO_VOLLEY_SPREAD.get().floatValue());
            }
        }
    }

    /** Следопыт-ветеран ставит капкан на пути игрока. */
    private void trackerStep(ServerLevel server, Player player) {
        if (!has(2) || techniqueCooldown > 0 || distanceToSqr(player) > 16 * 16) {
            return;
        }
        Vec3 dir = player.getDeltaMovement().multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0e-3) {
            dir = player.position().subtract(position()).multiply(1, 0, 1);
        }
        dir = dir.normalize().scale(2.5);
        BlockPos at = BlockPos.containing(player.getX() + dir.x, player.getY(), player.getZ() + dir.z);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos p = at.above(dy);
            if (server.getBlockState(p).isAir() && server.getBlockState(p.below()).isFaceSturdy(server, p.below(), net.minecraft.core.Direction.UP)) {
                server.setBlock(p, ModBlocks.HERO_TRAP.get().defaultBlockState(), 3);
                techniqueCooldown = BalanceConfig.HERO_TRAP_COOLDOWN_TICKS.get();
                return;
            }
        }
    }

    /** Командир: аура строя (сопротивление урону рядом), ветеран — приказ «окружить». */
    private void commanderStep(ServerLevel server, Player player) {
        if (++auraTimer >= 20) {
            auraTimer = 0;
            double r = BalanceConfig.HERO_AURA_RADIUS.get();
            for (HeroEntity ally : server.getEntitiesOfClass(HeroEntity.class, getBoundingBox().inflate(r), h -> h != this && h.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0), this);
            }
        }
        if (has(2) && techniqueCooldown <= 0) {
            techniqueCooldown = BalanceConfig.HERO_SURROUND_COOLDOWN_TICKS.get();
            var allies = server.getEntitiesOfClass(HeroEntity.class, getBoundingBox().inflate(24), h -> h != this && h.isAlive()
                    && h.squad != null && h.squad.equals(squad) && h.heroClass != HeroClass.ARCHER);
            for (int i = 0; i < allies.size(); i++) {
                double angle = Math.PI * 2 * i / Math.max(1, allies.size());
                allies.get(i).getNavigation().moveTo(player.getX() + Math.cos(angle) * 4, player.getY(), player.getZ() + Math.sin(angle) * 4, 1.3);
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (heroClass == HeroClass.MAGE) {
            castSpell(target);
            return;
        }
        shootArrow(target, 0.0f);
    }

    /** Снаряд стихии края, где стоит маг. */
    public HeroSpellEntity castSpell(LivingEntity target) {
        HeroSpellEntity spell = new HeroSpellEntity(level(), this, HeroSpellEntity.Element.of(level().getBiome(blockPosition())));
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - spell.getY();
        double dz = target.getZ() - getZ();
        spell.shoot(dx, dy, dz, 1.4f, 2.0f);
        playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.4f);
        level().addFreshEntity(spell);
        return spell;
    }

    private Arrow shootArrow(LivingEntity target, float yawOffset) {
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), getMainHandItem());
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) / 2.0);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double cos = Math.cos(Math.toRadians(yawOffset));
        double sin = Math.sin(Math.toRadians(yawOffset));
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        double dy = target.getY(0.33) - arrow.getY();
        double dist = Math.sqrt(rx * rx + rz * rz);
        arrow.shoot(rx, dy + dist * 0.2, rz, 1.6f, 8.0f);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
        return arrow;
    }

    // ---- награды ----

    /** Трофеи для Альянса: эмблема (шанс по рангу), знамя и изредка приказ — с командира. Снаряжение — шансом выпадения. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (!(source.getEntity() instanceof Player)) {
            return;
        }
        double emblem = switch (rank) {
            case APPRENTICE -> BalanceConfig.HERO_EMBLEM_CHANCE.get();
            case VETERAN -> BalanceConfig.HERO_EMBLEM_CHANCE_VETERAN.get();
            case MASTER -> HeroConfig.EMBLEM_CHANCE_MASTER.get();
        };
        if (random.nextFloat() < emblem) {
            spawnAtLocation(new ItemStack(ru.adept.core.registry.ModItems.HERO_EMBLEM.get()));
        }
        if (heroClass == HeroClass.COMMANDER) {
            spawnAtLocation(new ItemStack(ru.adept.core.registry.ModItems.HERO_BANNER.get()));
            if (random.nextFloat() < BalanceConfig.HERO_ORDER_CHANCE.get()) {
                BlockPos at = ru.adept.core.village.VillageRegistry.get(level.getServer()).byId(villageId)
                        .map(v -> v.center()).orElse(blockPosition());
                String name = getCustomName() == null ? "" : getCustomName().getString();
                spawnAtLocation(CommanderOrderItem.create(ru.adept.core.registry.ModItems.COMMANDER_ORDER.get(),
                        name, villageName, villageId, at.getX(), at.getZ()));
            }
        }
    }

    // ---- отступление ----

    /** Отряд отступает: цель сброшена, герой уходит от игрока и пропадает вдали. */
    public void startRetreat() {
        retreating = true;
        retreatTicks = 0;
        setTarget(null);
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    private void retreatStep(ServerLevel server) {
        retreatTicks++;
        Player near = server.getNearestPlayer(this, 64);
        if (near != null && getNavigation().isDone()) {
            Vec3 away = position().subtract(near.position()).multiply(1, 0, 1).normalize().scale(20);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.3);
        }
        if (retreatTicks > BalanceConfig.HERO_RETREAT_DESPAWN_TICKS.get() && (near == null || distanceToSqr(near) > 32 * 32)) {
            HeroSquads.get(server.getServer()).onLeft(this);
            discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!configured && !level().isClientSide) {
            configureGoals();
        }
    }

    // ---- сохранение ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("adept_class", heroClass.id());
        tag.putString("adept_rank", rank.id());
        if (squad != null) {
            tag.putUUID("adept_squad", squad);
        }
        tag.putBoolean("adept_spotted", spotted);
        tag.putBoolean("adept_wounded", wounded);
        tag.putBoolean("adept_retreating", retreating);
        tag.putBoolean("adept_once", onceUsed);
        tag.putLong("adept_village", villageId);
        tag.putString("adept_village_name", villageName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        heroClass = HeroClass.byId(tag.getString("adept_class"));
        entityData.set(CLASS, heroClass.ordinal());
        rank = HeroRank.byId(tag.getString("adept_rank"));
        entityData.set(RANK, rank.ordinal());
        squad = tag.hasUUID("adept_squad") ? tag.getUUID("adept_squad") : null;
        spotted = tag.getBoolean("adept_spotted");
        wounded = tag.getBoolean("adept_wounded");
        villageId = tag.getLong("adept_village");
        villageName = tag.getString("adept_village_name");
        onceUsed = tag.getBoolean("adept_once");
        configureGoals();
        if (tag.getBoolean("adept_retreating")) {
            startRetreat();
        }
    }

    /** Деревня-отправитель (для приказа командира). */
    public void setVillage(long id, String name) {
        this.villageId = id;
        this.villageName = name == null ? "" : name;
    }

    /** Имя для командира — над головой. */
    public void setHeroName(String name) {
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    static float clampChance(double v) {
        return (float) Mth.clamp(v, 0.0, 1.0);
    }
}
