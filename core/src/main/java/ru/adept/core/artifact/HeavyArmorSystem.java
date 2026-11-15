package ru.adept.core.artifact;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.base.BaseData;
import ru.adept.core.base.BaseTravel;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.table.TableActions;

/**
 * Работа тяжёлой брони: пластины гасят долю урона после брони, сервоприводы тратят электричество нагрудника
 * при движении и замедляют (без заряда — сильнее), на базе броня и гаусс-пушка подзаряжаются от запаса базы.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class HeavyArmorSystem {
    public static final ResourceLocation SERVO_ID = ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "heavy_servo");

    private HeavyArmorSystem() {
    }

    /** Доля, которую гасит целая пластина: база плюс «Бастион». Никогда не 100%. */
    public static double absorb(Player player) {
        int bastion = TableActions.progress(player).step(TableActions.HEAVY_BASTION);
        return Math.min(0.95, BalanceConfig.HEAVY_PLATE_ABSORB.get() + bastion * BalanceConfig.HEAVY_BASTION_PER_STEP.get());
    }

    public static boolean wearsAny(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            if (player.getItemBySlot(slot).getItem() instanceof HeavyArmorItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Распределить урон {@code amount} (уже после брони) по пластинам: вернуть, сколько проходит в здоровье.
     * Пластины теряют прочность на погашенную часть.
     */
    public static float absorbDamage(Player player, float amount) {
        return absorbDamage(player, amount, null);
    }

    /**
     * То же с источником урона — для оберегов рун: Тяга и Пустота гасят часть урона от снарядов и магии,
     * Узы передают часть урона пластины соседней целой, Эхо один раз поднимает разбитую пластину,
     * Разлом при разрушении пластины замедляет врагов вокруг.
     */
    public static float absorbDamage(Player player, float amount, net.minecraft.world.damagesource.DamageSource source) {
        double absorb = absorb(player);
        double passed = 0;
        double covered = 0;
        boolean projectile = ru.adept.core.rune.RuneWards.projectile(source);
        boolean magic = ru.adept.core.rune.RuneWards.magic(source);
        double reduce = ru.adept.core.config.RuneConfig.WARD_REDUCE.get();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof HeavyArmorItem armor)) {
                continue;
            }
            var parts = armor.parts();
            for (int i = 0; i < parts.size(); i++) {
                double share = amount * HeavyArmorItem.share(parts.get(i));
                covered += HeavyArmorItem.share(parts.get(i));
                CompoundTag plate = HeavyArmorItem.plate(stack, i);
                if (!HeavyArmorItem.intact(plate)) {
                    passed += share;
                    continue;
                }
                var runes = ru.adept.core.rune.RuneSet.read(plate);
                if (projectile) {
                    share *= Math.max(0, 1 - reduce * ru.adept.core.rune.RuneWards.strength(runes, ru.adept.core.rune.Rune.PULL));
                }
                if (magic) {
                    share *= Math.max(0, 1 - reduce * ru.adept.core.rune.RuneWards.strength(runes, ru.adept.core.rune.Rune.VOID));
                }
                double absorbed = share * absorb;
                double bond = ru.adept.core.rune.RuneWards.strength(runes, ru.adept.core.rune.Rune.BOND);
                double moved = bond > 0 ? absorbed * Math.min(1, ru.adept.core.config.RuneConfig.WARD_SHARE.get() * bond) : 0;
                if (moved > 0 && !shiftToNeighbour(player, stack, i, moved)) {
                    moved = 0;
                }
                double before = plate.getDouble("durability");
                plate = HeavyArmorItem.plate(stack, i);
                plate.putDouble("durability", plate.getDouble("durability") - (absorbed - moved));
                if (before > 0 && plate.getDouble("durability") <= 0) {
                    onPlateBroken(player, plate, runes);
                }
                HeavyArmorItem.setPlate(stack, i, plate);
                passed += share - absorbed;
            }
        }
        passed += amount * Math.max(0, 1.0 - covered);
        return (float) passed;
    }

    /** Узы: часть урона уходит на другую целую пластину того же игрока. */
    private static boolean shiftToNeighbour(Player player, ItemStack from, int fromIndex, double amount) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof HeavyArmorItem armor)) {
                continue;
            }
            for (int i = 0; i < armor.parts().size(); i++) {
                if (stack == from && i == fromIndex) {
                    continue;
                }
                CompoundTag p = HeavyArmorItem.plate(stack, i);
                if (HeavyArmorItem.intact(p) && p.getDouble("durability") > amount) {
                    p.putDouble("durability", p.getDouble("durability") - amount);
                    HeavyArmorItem.setPlate(stack, i, p);
                    return true;
                }
            }
        }
        return false;
    }

    /** Разрушение пластины: Эхо один раз поднимает её на треть, Разлом замедляет врагов вокруг. */
    private static void onPlateBroken(Player player, CompoundTag plate, ru.adept.core.rune.RuneSet runes) {
        double echo = ru.adept.core.rune.RuneWards.strength(runes, ru.adept.core.rune.Rune.ECHO);
        if (echo > 0 && !plate.getBoolean("echoed")) {
            ru.adept.core.base.Molten.byId(plate.getString("material")).ifPresent(m -> {
                plate.putDouble("durability", HeavyArmorItem.plateDurability(m) * Math.min(1.0, ru.adept.core.config.RuneConfig.WARD_ECHO_RESTORE.get() * echo));
                plate.putBoolean("echoed", true);
            });
        }
        double rift = ru.adept.core.rune.RuneWards.strength(runes, ru.adept.core.rune.Rune.RIFT);
        if (rift > 0) {
            int ticks = (int) Math.round(ru.adept.core.config.RuneConfig.WARD_SLOW_TICKS.get() * rift);
            for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(4),
                    e -> e != player && e.isAlive() && !(e instanceof Player))) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), player);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !wearsAny(player)
                || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) || event.getNewDamage() <= 0) {
            return;
        }
        event.setNewDamage(absorbDamage(player, event.getNewDamage(), event.getSource()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SERVO_ID);
        if (player.level().dimension() == BaseTravel.BASE) {
            chargeAtBase(player);
        }
        if (!wearsAny(player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChest = chest.getItem() instanceof HeavyArmorItem;
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0e-4;
        boolean powered = hasChest && HeavyArmorItem.charge(chest) > 0;
        if (powered && moving && !player.isCreative()) {
            HeavyArmorItem.setCharge(chest, HeavyArmorItem.charge(chest) - BalanceConfig.HEAVY_SERVO_PER_SECOND.get());
        }
        double slow = powered ? BalanceConfig.HEAVY_SLOW_POWERED.get() : BalanceConfig.HEAVY_SLOW_UNPOWERED.get();
        speed.addTransientModifier(new AttributeModifier(SERVO_ID, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** На базе нагрудник и гаусс-пушки в инвентаре заряжаются от электричества базы. */
    private static void chargeAtBase(ServerPlayer player) {
        BaseData base = BaseData.get(player.getServer());
        int rate = BalanceConfig.ARTIFACT_BASE_CHARGE_PER_SECOND.get();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof HeavyArmorItem) {
            int need = Math.min(rate, BalanceConfig.HEAVY_CHARGE_CAPACITY.get() - HeavyArmorItem.charge(chest));
            if (need > 0 && base.useElectricity(need)) {
                HeavyArmorItem.setCharge(chest, HeavyArmorItem.charge(chest) + need);
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GaussGunItem) {
                int need = Math.min(rate, GaussGunItem.capacity() - GaussGunItem.charge(stack));
                if (need > 0 && base.useElectricity(need)) {
                    GaussGunItem.setCharge(stack, GaussGunItem.charge(stack) + need);
                }
            }
            if (stack.getItem() instanceof RailGunItem) {
                int need = Math.min(rate, RailGunItem.capacity() - RailGunItem.charge(stack));
                if (need > 0 && base.useElectricity(need)) {
                    RailGunItem.setCharge(stack, RailGunItem.charge(stack) + need);
                }
            }
        }
    }
}
