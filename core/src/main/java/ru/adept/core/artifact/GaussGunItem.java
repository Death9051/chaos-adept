package ru.adept.core.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.adept.core.base.Molten;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.table.TableActions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Гаусс-пушка, прототип. Стреляет иглами из магазина (выстрел — мгновенный луч до 48 блоков с разбросом),
 * материал иглы задаёт урон и бонус. Магазин заряжается по шаблону: выбранный материал или «любые» иглы
 * из инвентаря; при нехватке недостающие пропускаются. Каждый выстрел тратит электричество и греет ствол,
 * при 100% нагрева пушка молчит. Иглы не подбираются. Shift + ПКМ — сменить шаблон и перезарядить.
 */
public class GaussGunItem extends Item {
    public static final String ANY = "";

    public GaussGunItem(Properties properties) {
        super(properties);
    }

    // ---- данные ----

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int capacity() {
        return BalanceConfig.GAUSS_CHARGE_CAPACITY.get();
    }

    public static int charge(ItemStack stack) {
        return data(stack).getInt("charge");
    }

    public static void setCharge(ItemStack stack, int value) {
        CompoundTag tag = data(stack);
        tag.putInt("charge", Mth.clamp(value, 0, capacity()));
        save(stack, tag);
    }

    public static List<String> magazine(ItemStack stack) {
        List<String> out = new ArrayList<>();
        for (Tag t : data(stack).getList("magazine", Tag.TAG_STRING)) {
            out.add(t.getAsString());
        }
        return out;
    }

    private static void putMagazine(CompoundTag tag, List<String> mag) {
        ListTag list = new ListTag();
        mag.forEach(m -> list.add(StringTag.valueOf(m)));
        tag.put("magazine", list);
    }

    public static String template(ItemStack stack) {
        return data(stack).getString("template");
    }

    /** Нагрев 0–1 на момент {@code now} (остывание считается лениво). */
    public static double heat(CompoundTag tag, long now) {
        double cooled = BalanceConfig.GAUSS_COOL_PER_SECOND.get() * Math.max(0, now - tag.getLong("heat_time")) / 20.0;
        return Math.max(0.0, tag.getDouble("heat") - cooled);
    }

    /** Выстрелов в секунду с учётом «Темпа». */
    public static double fireRate(Player player) {
        int tempo = TableActions.progress(player).step(TableActions.GAUSS_TEMPO);
        return BalanceConfig.GAUSS_FIRE_RATE.get() * (1.0 + tempo * BalanceConfig.GAUSS_TEMPO_PER_STEP.get());
    }

    public static double needleDamage(Molten m) {
        return switch (m) {
            case IRON -> BalanceConfig.NEEDLE_DAMAGE_IRON.get();
            case COPPER -> BalanceConfig.NEEDLE_DAMAGE_COPPER.get();
            case GOLD -> BalanceConfig.NEEDLE_DAMAGE_GOLD.get();
            case STEEL -> BalanceConfig.NEEDLE_DAMAGE_STEEL.get();
            case RED_GOLD -> BalanceConfig.NEEDLE_DAMAGE_RED_GOLD.get();
        };
    }

    // ---- использование ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        long now = level.getGameTime();
        if (player.isShiftKeyDown()) {
            cycleTemplate(player, stack);
            startReload(stack, now);
            return InteractionResultHolder.consume(stack);
        }
        fire((ServerLevel) level, player, stack, now);
        return InteractionResultHolder.consume(stack);
    }

    /** Следующий материал шаблона среди игл в инвентаре; «любые» — первым. */
    private static void cycleTemplate(Player player, ItemStack stack) {
        List<String> options = new ArrayList<>();
        options.add(ANY);
        for (Molten m : Molten.values()) {
            if (player.getInventory().countItem(m.needle()) > 0) {
                options.add(m.id());
            }
        }
        CompoundTag tag = data(stack);
        int i = options.indexOf(tag.getString("template"));
        String next = options.get((i + 1) % options.size());
        tag.putString("template", next);
        save(stack, tag);
        player.displayClientMessage(Component.translatable("adept_core.gauss.template",
                next.isEmpty() ? Component.translatable("adept_core.gauss.any") : Component.translatable("adept_core.molten." + next)), true);
    }

    private static void startReload(ItemStack stack, long now) {
        CompoundTag tag = data(stack);
        tag.putLong("reload_until", now + BalanceConfig.GAUSS_RELOAD_TICKS.get());
        save(stack, tag);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        CompoundTag tag = data(stack);
        long until = tag.getLong("reload_until");
        if (until > 0 && level.getGameTime() >= until) {
            finishReload(player, stack);
        }
    }

    /** Заполнить магазин по шаблону из инвентаря. Иглы, которых нет, пропускаются. */
    public static void finishReload(Player player, ItemStack stack) {
        CompoundTag tag = data(stack);
        tag.remove("reload_until");
        List<String> mag = new ArrayList<>(magazine(stack));
        int size = BalanceConfig.GAUSS_MAGAZINE.get();
        String template = tag.getString("template");
        var inv = player.getInventory();
        while (mag.size() < size) {
            Molten found = null;
            for (Molten m : Molten.values()) {
                if ((template.isEmpty() || template.equals(m.id())) && inv.countItem(m.needle()) > 0) {
                    found = m;
                    break;
                }
            }
            if (found == null) {
                break;
            }
            int take = Math.min(size - mag.size(), inv.countItem(found.needle()));
            int left = take;
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(found.needle())) {
                    int n = Math.min(left, s.getCount());
                    s.shrink(n);
                    left -= n;
                }
            }
            for (int i = 0; i < take; i++) {
                mag.add(found.id());
            }
        }
        putMagazine(tag, mag);
        save(stack, tag);
    }

    /** Выстрел; возвращает, был ли он. */
    public static boolean fire(ServerLevel level, Player player, ItemStack stack, long now) {
        CompoundTag tag = data(stack);
        if (tag.getLong("reload_until") > now) {
            return false;
        }
        if (tag.getLong("overheat_until") > now) {
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
            return false;
        }
        if (tag.getDouble("next_shot") > now) {
            return false;
        }
        List<String> mag = new ArrayList<>(magazine(stack));
        if (mag.isEmpty()) {
            startReload(stack, now);
            return false;
        }
        int cost = BalanceConfig.GAUSS_SHOT_COST.get();
        if (!player.isCreative() && tag.getInt("charge") < cost) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.4f);
            return false;
        }
        Molten material = Molten.byId(mag.remove(0)).orElse(Molten.IRON);
        putMagazine(tag, mag);
        if (!player.isCreative()) {
            tag.putInt("charge", tag.getInt("charge") - cost);
        }
        double heat = heat(tag, now) + BalanceConfig.GAUSS_HEAT_PER_SHOT.get();
        if (heat >= 1.0) {
            heat = 1.0;
            tag.putLong("overheat_until", now + BalanceConfig.GAUSS_OVERHEAT_TICKS.get());
        }
        tag.putDouble("heat", heat);
        tag.putLong("heat_time", now);
        tag.putDouble("next_shot", Math.max(tag.getDouble("next_shot"), now) + 20.0 / fireRate(player));
        shoot(level, player, material, tag);
        save(stack, tag);
        return true;
    }

    /** Луч иглы: разброс, урон по материалу, сталь прошивает вторую цель, красное золото копит прибавку по одной цели. */
    private static void shoot(ServerLevel level, Player player, Molten material, CompoundTag tag) {
        double range = BalanceConfig.GAUSS_RANGE.get();
        double spread = Math.toRadians(BalanceConfig.GAUSS_SPREAD.get());
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 dir = look.add(player.getRandom().nextGaussian() * spread * 0.5, player.getRandom().nextGaussian() * spread * 0.5,
                player.getRandom().nextGaussian() * spread * 0.5).normalize();
        Vec3 end = eye.add(dir.scale(range));
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        final Vec3 from = eye;
        final Vec3 rayEnd = end;
        Vec3 to = end;
        List<LivingEntity> hits = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(from, rayEnd).inflate(1.0),
                        e -> e != player && e.isAlive() && e.getBoundingBox().inflate(0.3).clip(from, rayEnd).isPresent()));
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        int targets = material == Molten.STEEL ? 2 : 1;
        for (int i = 0; i < Math.min(targets, hits.size()); i++) {
            LivingEntity target = hits.get(i);
            double damage = needleDamage(material);
            if (material == Molten.COPPER) {
                damage *= target.getArmorValue() > 0 ? 0.7 : 1.3;
            }
            if (material == Molten.RED_GOLD) {
                String id = target.getStringUUID();
                int stacks = id.equals(tag.getString("last_target")) ? Math.min(3, tag.getInt("stacks") + 1) : 0;
                tag.putString("last_target", id);
                tag.putInt("stacks", stacks);
                damage *= 1.0 + 0.15 * stacks;
            }
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), (float) damage);
            if (i == 0) {
                to = target.position().add(0, target.getBbHeight() / 2, 0);
            }
        }
        int steps = (int) Math.max(2, from.distanceTo(to) * 2);
        for (int s = 1; s <= steps; s += 2) {
            Vec3 p = from.lerp(to, s / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String t = template(stack);
        tooltip.add(Component.translatable("adept_core.gauss.magazine", magazine(stack).size(), BalanceConfig.GAUSS_MAGAZINE.get(),
                t.isEmpty() ? Component.translatable("adept_core.gauss.any") : Component.translatable("adept_core.molten." + t))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("adept_core.gauss.charge", charge(stack), capacity()).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("adept_core.gauss.hint").withStyle(ChatFormatting.DARK_GRAY));
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
