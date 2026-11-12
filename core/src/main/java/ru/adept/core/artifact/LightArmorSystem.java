package ru.adept.core.artifact;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.ShieldEvent;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.focus.FocusCut;
import ru.adept.core.focus.FocusItem;
import ru.adept.core.focus.Focuses;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.table.TableActions;

/**
 * Работа лёгкого экзокостюма: щит из маны гасит урон раньше здоровья, восстанавливается через паузу без урона
 * за ману костюма; без маны щит гаснет. Когда щит принял удар, фокус нагрудника отвечает (не чаще перезарядки)
 * и изнашивается. Пробитие щита до нуля — событие для сюжета.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class LightArmorSystem {
    public static final String FIRST_BREAK = "light/first_shield_break";

    private LightArmorSystem() {
    }

    public static ItemStack chest(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof LightArmorItem ? chest : ItemStack.EMPTY;
    }

    /** Предел щита по ступеням «Ёмкости». */
    public static int capacity(Player player) {
        return FocusConfig.shieldCapacity(TableActions.progress(player).step(TableActions.LIGHT_CAPACITY));
    }

    /** Урон, который щит не принимает: голод, удушье, падение в пустоту и всё, что проходит неуязвимость. */
    public static boolean ignores(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL);
    }

    /**
     * Щит гасит урон {@code amount}: вернуть, сколько проходит в здоровье. Удар по щиту запускает ответ фокуса;
     * щит, пробитый до нуля, — событие.
     */
    public static float absorb(Player player, ItemStack chest, float amount, LivingEntity attacker) {
        double shield = LightArmorItem.shield(chest);
        long now = player.level().getGameTime();
        var tag = LightArmorItem.data(chest);
        tag.putLong("lastHit", now);
        LightArmorItem.save(chest, tag);
        if (shield <= 0 || amount <= 0) {
            return amount;
        }
        double absorbed = Math.min(shield, amount);
        double left = shield - absorbed;
        LightArmorItem.setShield(chest, left, capacity(player));
        float passed = (float) (amount - absorbed);
        respond(player, chest, attacker);
        if (left <= 1.0e-6 && LightArmorItem.shield(chest) <= 1.0e-6) {
            post(player, new ShieldEvent.Broken(player, amount, passed, markFirst(player)));
        }
        return passed;
    }

    /** Ответ щита: эффект фокуса нагрудника. Вернёт true, если фокус сработал. */
    public static boolean respond(Player player, ItemStack chest, LivingEntity attacker) {
        ItemStack focus = LightArmorItem.focus(chest);
        if (!Focuses.active(focus)) {
            return false;
        }
        long now = player.level().getGameTime();
        var tag = LightArmorItem.data(chest);
        if (now < tag.getLong("nextResponse")) {
            return false;
        }
        int cost = (int) Math.round(FocusConfig.RESPONSE_MANA.get() * Focuses.manaFactor(focus));
        if (LightArmorItem.mana(chest) < cost) {
            return false;
        }
        tag.putLong("nextResponse", now + FocusConfig.RESPONSE_COOLDOWN.get());
        tag.putInt("mana", LightArmorItem.mana(chest) - cost);
        LightArmorItem.save(chest, tag);
        apply(player, chest, FocusItem.cut(focus), FocusItem.power(focus));
        Focuses.Wear wear = Focuses.wear(player, focus, player.getRandom());
        LightArmorItem.setFocus(chest, wear == Focuses.Wear.BROKEN ? ItemStack.EMPTY : focus);
        if (wear == Focuses.Wear.BROKEN || wear == Focuses.Wear.CRACKED) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, player.getSoundSource(), 1.0f, 0.8f);
        }
        return true;
    }

    private static void apply(Player player, ItemStack chest, FocusCut cut, double power) {
        double radius = FocusConfig.RESPONSE_RADIUS.get();
        var others = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Player p && p.isCreative()));
        switch (cut) {
            case KNOCKBACK -> {
                for (LivingEntity e : others) {
                    Vec3 d = e.position().subtract(player.position());
                    e.knockback(FocusConfig.RESPONSE_KNOCKBACK.get() * power, -d.x, -d.z);
                    e.hurtMarked = true;
                }
            }
            case FLASH -> {
                int ticks = (int) Math.round(FocusConfig.RESPONSE_FLASH_TICKS.get() * power);
                for (LivingEntity e : others) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks), player);
                }
            }
            case RECHARGE -> {
                int cap = capacity(player);
                LightArmorItem.setShield(chest, LightArmorItem.shield(chest) + FocusConfig.RESPONSE_RECHARGE.get() * power, cap);
            }
            case SLOW -> {
                int ticks = (int) Math.round(FocusConfig.RESPONSE_SLOW_TICKS.get() * power);
                for (LivingEntity e : others) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, FocusConfig.RESPONSE_SLOW_LEVEL.get()), player);
                }
            }
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(cut == FocusCut.FLASH ? ParticleTypes.FLASH : ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(), cut == FocusCut.FLASH ? 1 : 20, radius / 3, 0.5, radius / 3, 0.1);
        }
    }

    /**
     * Секунда работы костюма (вынесено для тестов): без маны щит гаснет; через паузу без урона щит
     * восстанавливается за ману; аметист в гнезде понемногу чинится сам.
     */
    public static void tick(Player player, ItemStack chest, int ticks) {
        int cap = capacity(player);
        double shield = Math.min(LightArmorItem.shield(chest), cap);
        int mana = LightArmorItem.mana(chest);
        if (mana <= 0) {
            shield = 0;
        } else if (shield < cap && player.level().getGameTime() - LightArmorItem.data(chest).getLong("lastHit") >= FocusConfig.LIGHT_REGEN_DELAY.get()) {
            int perUnit = FocusConfig.LIGHT_MANA_PER_UNIT.get();
            double units = Math.min(FocusConfig.LIGHT_REGEN_PER_SECOND.get() * ticks / 20.0, cap - shield);
            if (perUnit > 0) {
                units = Math.min(units, (double) mana / perUnit);
            }
            shield += units;
            LightArmorItem.setMana(chest, mana - (int) Math.ceil(units * perUnit - 1.0e-6));
        }
        LightArmorItem.setShield(chest, shield, cap);
        ItemStack focus = LightArmorItem.focus(chest);
        if (!focus.isEmpty() && Focuses.grow(focus, ticks / 20)) {
            LightArmorItem.setFocus(chest, focus);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || ignores(event.getSource())
                || event.getNewDamage() <= 0) {
            return;
        }
        ItemStack chest = chest(player);
        if (chest.isEmpty()) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        event.setNewDamage(absorb(player, chest, event.getNewDamage(), attacker));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ItemStack chest = chest(player);
        if (!chest.isEmpty()) {
            tick(player, chest, 20);
        }
    }

    private static boolean markFirst(Player player) {
        ArtifactProgress p = player.getData(ModRegistries.ARTIFACT_PROGRESS);
        if (p.step(FIRST_BREAK) > 0) {
            return false;
        }
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(FIRST_BREAK, 1));
        return true;
    }

    private static void post(Player player, ShieldEvent event) {
        if (player.getServer() != null) {
            AdeptEvents.post(event);
        }
    }
}
