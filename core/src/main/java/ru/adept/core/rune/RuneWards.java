package ru.adept.core.rune;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.HeavyArmorItem;
import ru.adept.core.config.RuneConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Руны на пластинах тяжёлой брони: обереги и особые слова брони. Первая руна пластины — оберег в полную силу,
 * вторая — свой оберег слабее (доля {@code facetShare}); точная пара даёт особое слово брони вместо оберегов.
 * Обереги, которые меняют урон пластины (Узы, Эхо, Разлом, Тяга, Пустота), считаются в {@code HeavyArmorSystem};
 * ответ на удар и слова — здесь.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class RuneWards {
    public static final ResourceLocation ANCHOR_ID = ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "rune_anchor");
    private static final String LAST_MOVE = "adept_ward_last_move";
    private static final String LAST_POS = "adept_ward_last_pos";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};

    private RuneWards() {
    }

    /** Гравировки целых пластин на игроке. */
    public static List<RuneSet> sets(Player player) {
        List<RuneSet> out = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof HeavyArmorItem armor) {
                for (int i = 0; i < armor.parts().size(); i++) {
                    CompoundTag plate = HeavyArmorItem.plate(stack, i);
                    if (HeavyArmorItem.intact(plate)) {
                        RuneSet set = RuneSet.read(plate);
                        if (!set.isEmpty()) {
                            out.add(set);
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Особые слова брони на целых пластинах. */
    public static Set<RuneWord> words(Player player) {
        Set<RuneWord> out = EnumSet.noneOf(RuneWord.class);
        for (RuneSet s : sets(player)) {
            if (s.word() != null) {
                out.add(s.word());
            }
        }
        return out;
    }

    /** Сила оберега руны в гравировке одной пластины: первая — 1, вторая — доля грани, в слове — 0. */
    public static double strength(RuneSet set, Rune rune) {
        if (set.isEmpty() || set.word() != null) {
            return 0;
        }
        if (set.first() == rune) {
            return set.multiplier();
        }
        return set.second() == rune ? set.multiplier() * RuneConfig.RUNE_FACET_SHARE.get() : 0;
    }

    /** Сильнейший оберег руны среди всех целых пластин игрока. */
    public static double strength(Player player, Rune rune) {
        double best = 0;
        for (RuneSet s : sets(player)) {
            best = Math.max(best, strength(s, rune));
        }
        return best;
    }

    public static boolean projectile(DamageSource source) {
        return source != null && source.is(DamageTypeTags.IS_PROJECTILE);
    }

    public static boolean magic(DamageSource source) {
        return source != null && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC));
    }

    private static boolean ready(Player player, String key) {
        return player.level().getGameTime() >= player.getPersistentData().getLong(key);
    }

    private static void cooldown(Player player, String key, int ticks) {
        player.getPersistentData().putLong(key, player.level().getGameTime() + ticks);
    }

    private static List<LivingEntity> enemies(Player player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand));
    }

    private static void stun(Player player, LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9), player);
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4), player);
    }

    /** Слова, которые меняют урон до пластин: Закалка, Искажение, Укоренение, Отражение, Ледяной кокон. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void beforePlates(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || event.getNewDamage() <= 0) {
            return;
        }
        Set<RuneWord> words = words(player);
        if (words.isEmpty()) {
            return;
        }
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        if (words.contains(RuneWord.ASH) && source.is(DamageTypeTags.IS_FIRE)) {
            player.clearFire();
            event.setNewDamage(0);
            return;
        }
        if (words.contains(RuneWord.BLACK_HOLE) && projectile(source)
                && player.getRandom().nextDouble() < RuneConfig.WARD_DISTORT_CHANCE.get()) {
            event.setNewDamage(0);
            return;
        }
        if (words.contains(RuneWord.PIT) && player.level().getGameTime() - player.getPersistentData().getLong(LAST_MOVE) >= 40) {
            damage *= (float) (1.0 - RuneConfig.WARD_ROOT_REDUCE.get());
        }
        if (words.contains(RuneWord.DOUBLE) && ready(player, "adept_ward_reflection")) {
            cooldown(player, "adept_ward_reflection", RuneConfig.WARD_COOLDOWN.get());
            if (Reflection.absorbIntoPlate(player, damage)) {
                event.setNewDamage(0);
                return;
            }
        }
        if (words.contains(RuneWord.SHARDS) && damage >= player.getHealth() && ready(player, "adept_ward_cocoon")) {
            cooldown(player, "adept_ward_cocoon", RuneConfig.WARD_RARE_COOLDOWN.get());
            damage = Math.max(0, player.getHealth() - 1);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1, 0.5, 0.02);
            }
        }
        event.setNewDamage(damage);
    }

    /** Ответ оберегов и слов на удар (после пластин). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void afterPlates(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        List<RuneSet> sets = sets(player);
        if (sets.isEmpty()) {
            return;
        }
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity l && l != player ? l : null;
        boolean melee = attacker != null && source.getDirectEntity() == attacker;
        double current = strength(player, Rune.CURRENT);
        if (attacker != null && current > 0) {
            stun(player, attacker, (int) Math.round(RuneConfig.WARD_STUN_TICKS.get() * current));
        }
        double frost = strength(player, Rune.FROST);
        if (melee && frost > 0) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) Math.round(RuneConfig.WARD_SLOW_TICKS.get() * frost), 1), player);
        }
        if (strength(player, Rune.FLAME) > 0) {
            player.clearFire();
        }
        Set<RuneWord> words = words(player);
        if (words.contains(RuneWord.STORM) && attacker != null) {
            List<LivingEntity> targets = new ArrayList<>(enemies(player, 3));
            if (!targets.contains(attacker)) {
                targets.add(attacker);
            }
            int ticks = RuneConfig.WARD_STUN_TICKS.get();
            targets.forEach(e -> stun(player, e, ticks));
            if (player.level() instanceof ServerLevel level) {
                RuneScheduler.later(level, 20, () -> targets.stream().filter(LivingEntity::isAlive).forEach(e -> stun(player, e, ticks)));
            }
        }
        if (words.contains(RuneWord.ERUPTION) && player.getHealth() - event.getNewDamage() < player.getMaxHealth() / 2
                && ready(player, "adept_ward_fire_ring")) {
            cooldown(player, "adept_ward_fire_ring", RuneConfig.WARD_COOLDOWN.get());
            for (LivingEntity e : enemies(player, 3)) {
                e.igniteForTicks(80);
                Vec3 away = e.position().subtract(player.position()).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.3, away.z));
                e.hurtMarked = true;
            }
            if (player.level() instanceof ServerLevel level) {
                for (int i = 0; i < 24; i++) {
                    double a = Math.PI * 2 * i / 24;
                    level.sendParticles(ParticleTypes.FLAME, player.getX() + Math.cos(a) * 3, player.getY() + 0.2, player.getZ() + Math.sin(a) * 3, 2, 0, 0.1, 0, 0.01);
                }
            }
        }
        if (words.contains(RuneWord.GLACIER) && event.getNewDamage() >= RuneConfig.WARD_STRONG_HIT.get()
                && ready(player, "adept_ward_ice_wall")) {
            cooldown(player, "adept_ward_ice_wall", RuneConfig.WARD_COOLDOWN.get());
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            for (LivingEntity e : enemies(player, 4)) {
                if (e.position().subtract(player.position()).dot(look) > 0) {
                    e.setDeltaMovement(e.getDeltaMovement().add(look.x, 0.2, look.z));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), player);
                }
            }
        }
    }

    /** Раз в секунду: Статический панцирь, Якорь, отметка движения для Укоренения. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag d = player.getPersistentData();
        long pos = player.blockPosition().asLong();
        if (d.getLong(LAST_POS) != pos) {
            d.putLong(LAST_POS, pos);
            d.putLong(LAST_MOVE, player.level().getGameTime());
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        Set<RuneWord> words = words(player);
        if (words.contains(RuneWord.SUPERCONDUCTOR)) {
            for (LivingEntity e : enemies(player, 3)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), player);
            }
        }
        AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(ANCHOR_ID);
            if (words.contains(RuneWord.ARCANE)) {
                kb.addTransientModifier(new AttributeModifier(ANCHOR_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /** Отражение: удар целиком уходит в первую целую пластину. */
    static final class Reflection {
        private Reflection() {
        }

        static boolean absorbIntoPlate(Player player, float damage) {
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.getItem() instanceof HeavyArmorItem armor) {
                    for (int i = 0; i < armor.parts().size(); i++) {
                        CompoundTag plate = HeavyArmorItem.plate(stack, i);
                        if (HeavyArmorItem.intact(plate)) {
                            plate.putDouble("durability", plate.getDouble("durability") - damage);
                            HeavyArmorItem.setPlate(stack, i, plate);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
