package ru.adept.core.rune;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.RuneEvent;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.base.Molten;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModItems;
import ru.adept.core.registry.ModRegistries;

import java.util.List;

/**
 * Гравировка изделий: какие изделия принимают руны, нанесение, событие при выдаче из высекателя,
 * стирание при подборе (один бросок на всю гравировку) и описание рун в подсказке изделия.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class Runes {
    public static final String FIRST_ENGRAVE = "rune/first_engrave";
    /** Отметка партии, только что вышедшей из высекателя (снимается, когда игрок её забирает). */
    public static final String KEY_FRESH = "rune_fresh";

    private Runes() {
    }

    /** Вид изделия для рун: {@code needle}, {@code bolt}, {@code plate} или пусто — руны не ставятся. */
    public static String kind(ItemStack stack) {
        for (Molten m : Molten.values()) {
            if (stack.is(m.needle())) {
                return "needle";
            }
            if (stack.is(m.bolt())) {
                return "bolt";
            }
            if (stack.is(m.plate())) {
                return "plate";
            }
        }
        return "";
    }

    public static boolean engravable(ItemStack stack) {
        return !kind(stack).isEmpty() && RuneSet.of(stack).isEmpty();
    }

    public static boolean fresh(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(KEY_FRESH);
    }

    public static void setFresh(ItemStack stack, boolean fresh) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (fresh) {
            tag.putBoolean(KEY_FRESH, true);
        } else {
            tag.remove(KEY_FRESH);
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** Игрок забрал партию из высекателя: событие гравировки (первая — с отметкой). */
    public static void onTaken(Player player, ItemStack stack) {
        if (player.level().isClientSide || !fresh(stack)) {
            return;
        }
        setFresh(stack, false);
        RuneSet set = RuneSet.of(stack);
        if (set.isEmpty()) {
            return;
        }
        ArtifactProgress p = player.getData(ModRegistries.ARTIFACT_PROGRESS);
        boolean first = p.step(FIRST_ENGRAVE) == 0;
        if (first) {
            player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(FIRST_ENGRAVE, 1));
        }
        RuneWord word = set.word();
        String kind = kind(stack);
        String wordId = word == null ? "" : "plate".equals(kind) ? word.armorId : word.shotId;
        post(player, new RuneEvent.Engraved(player, ids(set), set.material(), kind, stack.getCount(), wordId, first));
    }

    /**
     * Бросок стирания при подборе: с шансом «не стереться» материала гравировка остаётся, иначе пропадает целиком.
     * Вернёт true, если стёрлась.
     */
    public static boolean rollErase(Player player, ItemStack stack, RandomSource random) {
        RuneSet set = RuneSet.of(stack);
        if (set.isEmpty()) {
            return false;
        }
        if (random.nextDouble() < set.materialData().keep()) {
            return false;
        }
        RuneSet.EMPTY.applyTo(stack);
        if (player != null) {
            post(player, new RuneEvent.Erased(player, ids(set), set.material(), kind(stack)));
        }
        return true;
    }

    public static List<String> ids(RuneSet set) {
        return set.runes().stream().map(Rune::id).toList();
    }

    /** Название гравировки: «Ток + Эхо» или особое слово. */
    public static MutableComponent title(RuneSet set, boolean armor) {
        RuneWord word = set.word();
        if (word != null) {
            return Component.translatable("adept_core.rune.word." + (armor ? word.armorId : word.shotId));
        }
        MutableComponent out = Component.translatable("adept_core.rune." + set.runes().get(0).id());
        for (int i = 1; i < set.runes().size(); i++) {
            out.append(" + ").append(Component.translatable("adept_core.rune." + set.runes().get(i).id()));
        }
        return out;
    }

    /** Строки описания гравировки для подсказки изделия. */
    public static void describe(RuneSet set, boolean armor, List<Component> tooltip) {
        RuneMaterial m = set.materialData();
        tooltip.add(Component.translatable("adept_core.rune.engraving", title(set, armor),
                Component.translatable("adept_core.rune.material." + m.id())).withStyle(ChatFormatting.LIGHT_PURPLE));
        RuneWord word = set.word();
        String prefix = armor ? "adept_core.rune.ward." : "adept_core.rune.base.";
        if (word != null) {
            wrapped(tooltip, Component.translatable("adept_core.rune.word." + (armor ? word.armorId : word.shotId) + ".desc", args(set)), ChatFormatting.GRAY);
        } else {
            wrapped(tooltip, Component.translatable(prefix + set.first().id(), args(set)), ChatFormatting.GRAY);
            if (set.second() != null) {
                wrapped(tooltip, Component.translatable(armor ? "adept_core.rune.ward_second" : "adept_core.rune.facet_line",
                        Component.translatable((armor ? "adept_core.rune.ward." : "adept_core.rune.facet.") + set.second().id(), args(set))), ChatFormatting.GRAY);
            }
        }
        wrapped(tooltip, Component.translatable("adept_core.rune.material_line", StepTexts.format(set.multiplier()),
                StepTexts.format(100 * m.keep()), Component.translatable("adept_core.rune.modifier." + m.id())), ChatFormatting.DARK_AQUA);
    }

    /**
     * Числа для текстов рун (позиции %N$s): урон — с множителем материала, длительности — с «Неугасимостью».
     * 1 разряд, 2 поджог с, 3 связь с, 4 доля Уз %, 5 задержка Эха с, 6 доля Эха %, 7 зона с, 8 радиус трещины,
     * 9 радиус соседей, 10 заморозка с, 11 урон осколков, 12 Пустота с, 13 брони теряет %, 14 урон слова, 15 слово с,
     * 16 радиус слова, 17 сила грани %, 18 оглушение с, 19 доля Уз брони %, 20 Эхо брони %, 21 Тяга и Пустота брони %,
     * 22 замедление с, 23 перезарядка с, 24 редкая перезарядка с, 25 сильный удар, 26 Укоренение %, 27 Искажение %.
     */
    public static Object[] args(RuneSet set) {
        double m = set.isEmpty() ? 1.0 : set.multiplier();
        double t = set.modifier(RuneMaterial.NETHERITE) ? 1.0 + ru.adept.core.config.RuneConfig.MOD_DURATION.get() : 1.0;
        var c = new Object[27];
        c[0] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_ARC_DAMAGE.get() * m);
        c[1] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_FIRE_SECONDS.get() * t);
        c[2] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_LINK_SECONDS.get() * t);
        c[3] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.RUNE_LINK_SHARE.get() * m);
        c[4] = StepTexts.format(set.modifier(RuneMaterial.REDSTONE) ? 0 : ru.adept.core.config.RuneConfig.RUNE_ECHO_DELAY.get() / 20.0);
        c[5] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.RUNE_ECHO_SHARE.get() * m);
        c[6] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_ZONE_SECONDS.get() * t);
        c[7] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_ZONE_RADIUS.get());
        c[8] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_RADIUS.get());
        c[9] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_FROST_SECONDS.get() * t);
        c[10] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_SHATTER_DAMAGE.get() * m);
        c[11] = StepTexts.format(ru.adept.core.config.RuneConfig.RUNE_VOID_SECONDS.get() * t);
        c[12] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.RUNE_VOID_ARMOR.get());
        c[13] = StepTexts.format(ru.adept.core.config.RuneConfig.WORD_DAMAGE.get() * m);
        c[14] = StepTexts.format(ru.adept.core.config.RuneConfig.WORD_SECONDS.get() * t);
        c[15] = StepTexts.format(ru.adept.core.config.RuneConfig.WORD_RADIUS.get());
        c[16] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.RUNE_FACET_SHARE.get());
        c[17] = StepTexts.format(ru.adept.core.config.RuneConfig.WARD_STUN_TICKS.get() * m / 20.0);
        c[18] = StepTexts.format(100 * Math.min(1, ru.adept.core.config.RuneConfig.WARD_SHARE.get() * m));
        c[19] = StepTexts.format(100 * Math.min(1, ru.adept.core.config.RuneConfig.WARD_ECHO_RESTORE.get() * m));
        c[20] = StepTexts.format(100 * Math.min(1, ru.adept.core.config.RuneConfig.WARD_REDUCE.get() * m));
        c[21] = StepTexts.format(ru.adept.core.config.RuneConfig.WARD_SLOW_TICKS.get() * m / 20.0);
        c[22] = StepTexts.format(ru.adept.core.config.RuneConfig.WARD_COOLDOWN.get() / 20.0);
        c[23] = StepTexts.format(ru.adept.core.config.RuneConfig.WARD_RARE_COOLDOWN.get() / 20.0);
        c[24] = StepTexts.format(ru.adept.core.config.RuneConfig.WARD_STRONG_HIT.get());
        c[25] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.WARD_ROOT_REDUCE.get());
        c[26] = StepTexts.format(100 * ru.adept.core.config.RuneConfig.WARD_DISTORT_CHANCE.get());
        return c;
    }

    private static void wrapped(List<Component> tooltip, Component text, ChatFormatting style) {
        for (String line : ru.adept.core.util.TextWrap.wrap(text.getString(), ru.adept.core.util.TextWrap.TOOLTIP_CHARS)) {
            tooltip.add(Component.literal(line).withStyle(style));
        }
    }

    /** Подсказка гравированных игл, болтов и пластин (обычные предметы, без своего класса). */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String kind = kind(stack);
        if (kind.isEmpty()) {
            return;
        }
        RuneSet set = RuneSet.of(stack);
        if (set.isEmpty()) {
            if ("bolt".equals(kind)) {
                event.getToolTip().add(Component.translatable("adept_core.bolt.hint").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        describe(set, "plate".equals(kind), event.getToolTip());
    }

    /**
     * Подбор упавшего после выстрела болта: снимается отметка «упал» и бросается стирание рун.
     * Возвращает true, если гравировка стёрлась.
     */
    public static boolean pickUpLanded(Player player, ItemStack stack, RandomSource random) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean(ru.adept.core.artifact.RailGunItem.LANDED)) {
            return false;
        }
        tag.remove(ru.adept.core.artifact.RailGunItem.LANDED);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return rollErase(player, stack, random);
    }

    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        if (!"bolt".equals(kind(stack))) {
            return;
        }
        // Меняем сам предмет на земле (на месте): игра берёт в инвентарь именно его.
        pickUpLanded(player, stack, player.getRandom());
    }

    static void post(Player player, RuneEvent event) {
        if (player.getServer() != null) {
            AdeptEvents.post(event);
        }
    }

    /** Литое изделие с гравировкой (для команд и тестов). */
    public static ItemStack engraved(ItemStack stack, RuneSet set) {
        ItemStack out = stack.copy();
        set.applyTo(out);
        return out;
    }
}
