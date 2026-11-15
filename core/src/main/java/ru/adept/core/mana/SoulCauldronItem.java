package ru.adept.core.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.SoulEvent;
import ru.adept.core.config.ManaConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.table.TableActions;

import java.util.List;

/**
 * Котёл душ: одиночный артефакт, носится в инвентаре. Собранные души ждут в горниле ({@code furnace}),
 * лишние уходят в сосуд в слоте котла ({@code vessel} — материал, {@code vessel_souls} — души).
 * Горнило сжигает по одной душе за {@code burnTicks}, мана копится в котле ({@code buffer})
 * и уходит в накопитель с потерями. С небольшим шансом горнило выдаёт ядро души.
 * ПКМ — отправить души из сосуда в горнило; Shift + ПКМ — поменять сосуд с левой рукой.
 */
public class SoulCauldronItem extends Item {
    public static final String FIRST_SOUL = "cauldron/first_soul";
    public static final String FIRST_CORE = "cauldron/first_core";

    public SoulCauldronItem(Properties properties) {
        super(properties);
    }

    // ---- данные ----

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int[] furnace(ItemStack stack) {
        return SoulCounts.read(data(stack), "furnace");
    }

    public static int[] vesselSouls(ItemStack stack) {
        return SoulCounts.read(data(stack), "vessel_souls");
    }

    public static String vessel(ItemStack stack) {
        return data(stack).getString("vessel");
    }

    public static int buffer(ItemStack stack) {
        return data(stack).getInt("buffer");
    }

    public static void setBuffer(ItemStack stack, int value) {
        CompoundTag tag = data(stack);
        tag.putInt("buffer", Math.max(0, value));
        save(stack, tag);
    }

    /** Первый котёл в инвентаре игрока или пустой стек. */
    public static ItemStack find(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SoulCauldronItem) {
                return stack;
            }
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof SoulCauldronItem ? off : ItemStack.EMPTY;
    }

    /** Мана с души с учётом «Выхода». */
    public static int manaFor(Player player, SoulStrength strength) {
        int step = TableActions.progress(player).step(TableActions.CAULDRON_YIELD);
        return (int) Math.round(strength.mana() * (1.0 + step * ManaConfig.CAULDRON_YIELD_PER_STEP.get()));
    }

    // ---- сбор ----

    /**
     * Принять души: сначала в горнило (до {@code furnaceSouls}), остальное — в сосуд, если он есть и не полон.
     * Возвращает, сколько принято; непринятые остаются на земле.
     */
    public static int collect(ItemStack cauldron, SoulStrength strength, int count) {
        CompoundTag tag = data(cauldron);
        int[] furnace = SoulCounts.read(tag, "furnace");
        int toFurnace = Math.max(0, Math.min(count, ManaConfig.CAULDRON_FURNACE_SOULS.get() - SoulCounts.total(furnace)));
        furnace[strength.ordinal()] += toFurnace;
        int left = count - toFurnace;
        int toVessel = 0;
        String vessel = tag.getString("vessel");
        if (left > 0 && !vessel.isEmpty()) {
            int[] stored = SoulCounts.read(tag, "vessel_souls");
            toVessel = Math.max(0, Math.min(left, SoulVesselItem.capacityOf(vessel) - SoulCounts.total(stored)));
            stored[strength.ordinal()] += toVessel;
            SoulCounts.write(tag, "vessel_souls", stored);
        }
        SoulCounts.write(tag, "furnace", furnace);
        save(cauldron, tag);
        return toFurnace + toVessel;
    }

    /** Души из сосуда в горнило (ручная подача), сильные первыми. Возвращает, сколько перенесено. */
    public static int feedFromVessel(ItemStack cauldron) {
        CompoundTag tag = data(cauldron);
        int[] furnace = SoulCounts.read(tag, "furnace");
        int[] stored = SoulCounts.read(tag, "vessel_souls");
        int space = ManaConfig.CAULDRON_FURNACE_SOULS.get() - SoulCounts.total(furnace);
        int moved = 0;
        for (int i = stored.length - 1; i >= 0 && space > 0; i--) {
            int n = Math.min(space, stored[i]);
            stored[i] -= n;
            furnace[i] += n;
            space -= n;
            moved += n;
        }
        SoulCounts.write(tag, "furnace", furnace);
        SoulCounts.write(tag, "vessel_souls", stored);
        save(cauldron, tag);
        return moved;
    }

    // ---- горнило и передача ----

    /**
     * Секунда работы котла: горнило жжёт души (не впустую — только если мана влезает в котёл),
     * затем мана уходит в накопитель. {@code accumulator} может быть пустым стеком.
     */
    public static void process(Player player, ItemStack cauldron, ItemStack accumulator, int ticks) {
        CompoundTag tag = data(cauldron);
        int[] furnace = SoulCounts.read(tag, "furnace");
        int burnTicks = ManaConfig.SOUL_BURN_TICKS.get();
        int burn = tag.getInt("burn") + ticks;
        int buffer = tag.getInt("buffer");
        int bufferCap = ManaConfig.CAULDRON_BUFFER.get();
        RandomSource random = player.getRandom();
        while (burn >= burnTicks) {
            SoulStrength soul = SoulCounts.strongest(furnace);
            if (soul == null) {
                burn = Math.min(burn, burnTicks);
                break;
            }
            int mana = manaFor(player, soul);
            if (buffer > 0 && buffer + mana > bufferCap) {
                burn = Math.min(burn, burnTicks);
                break;
            }
            furnace[soul.ordinal()]--;
            buffer += mana;
            burn -= burnTicks;
            onBurned(player, soul, mana, random);
        }
        tag.putInt("burn", burn);
        SoulCounts.write(tag, "furnace", furnace);
        if (!accumulator.isEmpty() && buffer > 0) {
            int capacity = AccumulatorItem.capacity(player);
            int space = capacity - AccumulatorItem.stored(accumulator, ArtifactPower.Resource.MANA);
            double keep = 1.0 - ManaConfig.CAULDRON_TRANSFER_LOSS.get();
            int rate = ManaConfig.CAULDRON_TRANSFER.get() * ticks / 20;
            int send = Math.min(buffer, rate);
            if (keep > 0) {
                send = Math.min(send, (int) Math.ceil(space / keep));
            }
            int arrives = Math.min(space, (int) Math.floor(send * keep));
            if (send > 0) {
                AccumulatorItem.receive(accumulator, ArtifactPower.Resource.MANA, arrives, capacity);
                buffer -= send;
            }
        }
        tag.putInt("buffer", buffer);
        save(cauldron, tag);
    }

    private static void onBurned(Player player, SoulStrength soul, int mana, RandomSource random) {
        ArtifactProgress p = TableActions.progress(player);
        boolean first = p.step(FIRST_SOUL) == 0;
        if (first) {
            p = p.withStep(FIRST_SOUL, 1);
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p);
        }
        post(player, new SoulEvent.Burned(player, soul.id(), mana, first));
        if (random.nextDouble() < soul.coreChance()) {
            ItemStack core = new ItemStack(ModItems.SOUL_CORE.get());
            if (!player.getInventory().add(core)) {
                player.drop(core, false);
            }
            boolean firstCore = p.step(FIRST_CORE) == 0;
            if (firstCore) {
                player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(FIRST_CORE, 1));
            }
            post(player, new SoulEvent.CoreObtained(player, soul.id(), firstCore));
        }
    }

    private static void post(Player player, SoulEvent event) {
        if (player.getServer() != null) {
            AdeptEvents.post(event);
        }
    }

    // ---- сосуд в слоте ----

    /** Поменять сосуд котла с предметом в левой руке: пустая рука — вынуть сосуд. Возвращает, что изменилось. */
    public static boolean swapVessel(Player player, ItemStack cauldron) {
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && !(off.getItem() instanceof SoulVesselItem)) {
            return false;
        }
        CompoundTag tag = data(cauldron);
        String current = tag.getString("vessel");
        if (off.isEmpty() && current.isEmpty()) {
            return false;
        }
        ItemStack out = ItemStack.EMPTY;
        if (!current.isEmpty()) {
            out = new ItemStack(current.equals("quartz") ? ModItems.QUARTZ_SOUL_VESSEL.get() : ModItems.GLASS_SOUL_VESSEL.get());
            SoulVesselItem.setSouls(out, SoulCounts.read(tag, "vessel_souls"));
        }
        if (!off.isEmpty()) {
            ItemStack in = off.split(1);
            tag.putString("vessel", ((SoulVesselItem) in.getItem()).material());
            SoulCounts.write(tag, "vessel_souls", SoulVesselItem.souls(in));
        } else {
            tag.putString("vessel", "");
            SoulCounts.write(tag, "vessel_souls", new int[SoulStrength.values().length]);
        }
        save(cauldron, tag);
        if (!out.isEmpty()) {
            if (player.getOffhandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.OFF_HAND, out);
            } else if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer) {
            if (player.isShiftKeyDown()) {
                if (swapVessel(player, stack)) {
                    level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6f, 1.0f);
                }
            } else if (feedFromVessel(stack) > 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.SOUL_SAND_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("adept_core.cauldron.furnace", SoulCounts.total(furnace(stack)),
                ManaConfig.CAULDRON_FURNACE_SOULS.get()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("adept_core.cauldron.buffer", buffer(stack), ManaConfig.CAULDRON_BUFFER.get())
                .withStyle(ChatFormatting.BLUE));
        String vessel = vessel(stack);
        if (vessel.isEmpty()) {
            tooltip.add(Component.translatable("adept_core.cauldron.no_vessel").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("adept_core.cauldron.vessel",
                    Component.translatable("adept_core.vessel.material." + vessel),
                    SoulCounts.total(vesselSouls(stack)), SoulVesselItem.capacityOf(vessel)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("adept_core.cauldron.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
