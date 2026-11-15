package ru.adept.core.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.AdeptCore;
import ru.adept.core.base.Molten;
import ru.adept.core.config.RuneConfig;
import ru.adept.core.rune.RuneEffects;
import ru.adept.core.rune.RuneSet;
import ru.adept.core.table.TableActions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Рельса, прототип. Стреляет цельными болтами из литейной: удержание ПКМ заряжает конденсатор (около секунды),
 * отпускание после заряда — выстрел. Болт летит лучом, пробивает несколько целей (каждая следующая получает меньше),
 * материал болта задаёт урон и шанс поломки. Уцелевший болт падает на месте попадания, его можно подобрать
 * (руны при подборе могут стереться). Питание — электричество рельсы (накопитель, база). Shift + ПКМ — выбор болтов.
 */
public class RailGunItem extends Item {
    public static final ResourceKey<DamageType> PIERCE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "rail_pierce"));
    /** Отметка болта, упавшего после выстрела: при подборе бросается стирание рун. */
    public static final String LANDED = "landed";

    public RailGunItem(Properties properties) {
        super(properties);
    }

    /** Болт по материалу: урон и шанс поломки (из конфига). */
    public record Bolt(Molten material, double damage, double breakChance) {
    }

    public static Bolt bolt(Molten m) {
        for (String raw : RuneConfig.BOLTS.get()) {
            String[] p = raw.split("\\|");
            if (p.length == 3 && p[0].trim().equals(m.id())) {
                try {
                    return new Bolt(m, Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()));
                } catch (NumberFormatException ignored) {
                    break;
                }
            }
        }
        return new Bolt(m, 16, 0.3);
    }

    // ---- данные ----

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int capacity() {
        return RuneConfig.RAIL_CAPACITY.get();
    }

    public static int charge(ItemStack stack) {
        return data(stack).getInt("charge");
    }

    public static void setCharge(ItemStack stack, int value) {
        CompoundTag tag = data(stack);
        tag.putInt("charge", Mth.clamp(value, 0, capacity()));
        save(stack, tag);
    }

    public static String template(ItemStack stack) {
        return data(stack).getString("template");
    }

    /** Ступени «Пробоя» игрока. */
    public static int pierceStep(Player player) {
        return TableActions.progress(player).step(TableActions.RAIL_PIERCE);
    }

    public static int targets(Player player, Molten m) {
        return RuneConfig.RAIL_PIERCE.get() + (m == Molten.STEEL ? 1 : 0) + pierceStep(player) * RuneConfig.PIERCE_TARGETS.get();
    }

    public static double damage(Player player, Molten m) {
        return bolt(m).damage() * (1.0 + pierceStep(player) * RuneConfig.PIERCE_DAMAGE.get());
    }

    // ---- использование ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleTemplate(player, stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (findBolt(player, template(stack)) < 0 && !player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("adept_core.rail.no_bolts"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.4f, 1.8f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int held = getUseDuration(stack, entity) - timeLeft;
        if (held < RuneConfig.RAIL_CHARGE_TICKS.get()) {
            return;
        }
        fire(server, player, stack);
    }

    private static void cycleTemplate(Player player, ItemStack stack) {
        List<String> options = new ArrayList<>();
        options.add("");
        for (Molten m : Molten.values()) {
            if (player.getInventory().countItem(m.bolt()) > 0) {
                options.add(m.id());
            }
        }
        CompoundTag tag = data(stack);
        int i = options.indexOf(tag.getString("template"));
        String next = options.get((i + 1) % options.size());
        tag.putString("template", next);
        save(stack, tag);
        player.displayClientMessage(Component.translatable("adept_core.rail.template",
                next.isEmpty() ? Component.translatable("adept_core.gauss.any") : Component.translatable("adept_core.molten." + next)), true);
    }

    /** Слот первого подходящего болта или -1. */
    public static int findBolt(Player player, String template) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            var m = Molten.ofBolt(s);
            if (m.isPresent() && (template.isEmpty() || template.equals(m.get().id()))) {
                return i;
            }
        }
        return -1;
    }

    /** Выстрел (после заряда; вынесено для тестов). Вернёт, был ли выстрел. */
    public static boolean fire(ServerLevel level, Player player, ItemStack gun) {
        int cost = RuneConfig.RAIL_SHOT_COST.get();
        if (!player.isCreative() && charge(gun) < cost) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.2f);
            return false;
        }
        int slot = findBolt(player, template(gun));
        ItemStack bolt;
        if (slot >= 0) {
            bolt = player.getInventory().getItem(slot).split(1);
        } else if (player.isCreative()) {
            bolt = new ItemStack(Molten.IRON.bolt());
        } else {
            return false;
        }
        if (!player.isCreative()) {
            setCharge(gun, charge(gun) - cost);
        }
        shoot(level, player, bolt);
        return true;
    }

    private static void shoot(ServerLevel level, Player player, ItemStack bolt) {
        Molten material = Molten.ofBolt(bolt).orElse(Molten.IRON);
        RuneSet runes = RuneSet.of(bolt);
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = eye.add(dir.scale(RuneConfig.RAIL_RANGE.get()));
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        final Vec3 rayEnd = end;
        List<LivingEntity> hits = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, rayEnd).inflate(1.0),
                e -> e != player && e.isAlive() && e.getBoundingBox().inflate(0.3).clip(eye, rayEnd).isPresent()));
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        int targets = Math.min(targets(player, material), hits.size());
        double base = damage(player, material);
        int step = pierceStep(player);
        Vec3 stop = rayEnd;
        for (int i = 0; i < targets; i++) {
            LivingEntity target = hits.get(i);
            double dmg = base * Math.pow(1.0 - RuneConfig.RAIL_FALLOFF.get(), i);
            if (material == Molten.COPPER) {
                dmg *= target.getArmorValue() > 0 ? 0.7 : 1.3;
            }
            hurt(level, player, target, dmg, step > 0 ? RuneConfig.PIERCE_ARMOR.get() : 0.0);
            Vec3 at = target.position().add(0, target.getBbHeight() / 2, 0);
            if (i == 0) {
                RuneEffects.onHit(level, player, target, at, dir, runes, dmg);
            }
            if (i == targets - 1) {
                stop = at;
            }
        }
        if (targets == 0 && block.getType() != HitResult.Type.MISS) {
            RuneEffects.onHit(level, player, null, rayEnd, dir, runes, base);
        }
        int steps = (int) Math.max(2, eye.distanceTo(stop) * 3);
        for (int s = 1; s <= steps; s++) {
            Vec3 p = eye.lerp(stop, s / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
        if (level.random.nextDouble() >= bolt(material).breakChance()) {
            drop(level, stop.subtract(dir.scale(0.5)), bolt);
        } else {
            level.sendParticles(ParticleTypes.CRIT, stop.x, stop.y, stop.z, 8, 0.2, 0.2, 0.2, 0.1);
        }
    }

    /** Урон болта: часть {@code ignoreArmor} — мимо брони («Пробой»), остальное — обычный удар игрока. */
    public static void hurt(ServerLevel level, Player player, LivingEntity target, double damage, double ignoreArmor) {
        double through = damage * ignoreArmor;
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(player), (float) (damage - through));
        if (through > 0 && target.isAlive()) {
            target.invulnerableTime = 0;
            DamageSource pierce = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(PIERCE_DAMAGE), player);
            target.hurt(pierce, (float) through);
        }
    }

    /** Уцелевший болт ложится на месте попадания. */
    public static ItemEntity drop(ServerLevel level, Vec3 at, ItemStack bolt) {
        ItemStack copy = bolt.copyWithCount(1);
        CompoundTag tag = data(copy);
        tag.putBoolean(LANDED, true);
        save(copy, tag);
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, copy, 0, 0, 0);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
        return item;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String t = template(stack);
        tooltip.add(Component.translatable("adept_core.rail.bolts",
                t.isEmpty() ? Component.translatable("adept_core.gauss.any") : Component.translatable("adept_core.molten." + t)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("adept_core.rail.charge", charge(stack), capacity()).withStyle(ChatFormatting.RED));
        ru.adept.core.focus.FocusItem.wrapped(tooltip, Component.translatable("adept_core.rail.hint",
                StepTexts.format(RuneConfig.RAIL_CHARGE_TICKS.get() / 20.0)), ChatFormatting.DARK_GRAY);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
