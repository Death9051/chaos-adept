package ru.adept.core.focus;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.FocusEvent;
import ru.adept.core.config.FocusConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModRegistries;

/**
 * Прочность фокусов («Материалы», «Прочность фокусов»): каждое срабатывание изнашивает фокус;
 * когда прочность кончается, фокус с шансом трескается (гаснет, чинится на ограночной машине) или ломается (пропадает).
 * Модификаторы кристаллов: «Удача» — шанс сработать без износа, «Рост» — прочность восстанавливается сама,
 * «Экономия» — меньше маны за срабатывание.
 */
public final class Focuses {
    /** Отметки «впервые» в ступенях прогресса игрока. */
    public static final String FIRST_CUT = "focus/first_cut";
    public static final String FIRST_CRACK = "focus/first_crack";

    public enum Wear { WORN, LUCKY, CRACKED, BROKEN }

    private Focuses() {
    }

    /** Фокус работает: огранён, не треснул и прочность есть. */
    public static boolean active(ItemStack focus) {
        return FocusItem.is(focus) && !FocusItem.cracked(focus) && FocusItem.durability(focus) > 0;
    }

    /** Доля маны за срабатывание: у лазурита («Экономия») меньше. */
    public static double manaFactor(ItemStack focus) {
        return FocusCrystal.LAPIS.equals(FocusItem.crystalId(focus)) ? FocusConfig.FOCUS_ECONOMY.get() : 1.0;
    }

    /**
     * Одно срабатывание: износ фокуса (меняется на месте). {@link Wear#BROKEN} — фокус сломался, его надо убрать из гнезда.
     * События трещины и поломки публикуются здесь.
     */
    public static Wear wear(Player player, ItemStack focus, RandomSource random) {
        if (FocusCrystal.EMERALD.equals(FocusItem.crystalId(focus)) && random.nextDouble() < FocusConfig.FOCUS_LUCK_CHANCE.get()) {
            return Wear.LUCKY;
        }
        int left = FocusItem.durability(focus) - 1;
        FocusItem.setDurability(focus, left);
        if (left > 0) {
            return Wear.WORN;
        }
        FocusCut cut = FocusItem.cut(focus);
        String crystal = FocusItem.crystalId(focus);
        if (random.nextDouble() < FocusConfig.FOCUS_CRACK_CHANCE.get()) {
            FocusItem.setCracked(focus, true);
            boolean first = markFirst(player, FIRST_CRACK);
            post(player, new FocusEvent.Cracked(player, cut.id(), crystal, cut.segment(), first));
            return Wear.CRACKED;
        }
        post(player, new FocusEvent.Broken(player, cut.id(), crystal, cut.segment()));
        return Wear.BROKEN;
    }

    /** «Рост» (аметист): за {@code seconds} секунд копится восстановление прочности. Вернёт true, если фокус изменился. */
    public static boolean grow(ItemStack focus, int seconds) {
        if (!FocusCrystal.AMETHYST.equals(FocusItem.crystalId(focus)) || FocusItem.cracked(focus)
                || FocusItem.durability(focus) >= FocusItem.max(focus)) {
            return false;
        }
        var tag = FocusItem.data(focus);
        int growth = tag.getInt("growth") + seconds;
        int per = Math.max(1, FocusConfig.FOCUS_GROWTH_SECONDS.get());
        int add = growth / per;
        tag.putInt("growth", growth % per);
        tag.putInt("durability", Math.min(FocusItem.max(focus), tag.getInt("durability") + add));
        FocusItem.save(focus, tag);
        return true;
    }

    /** Игрок забрал фокус из ограночной машины: новый огранённый фокус даёт событие (первый — с отметкой). */
    public static void onTaken(Player player, ItemStack stack) {
        if (player.level().isClientSide || !FocusItem.is(stack) || !FocusItem.fresh(stack)) {
            return;
        }
        FocusItem.setFresh(stack, false);
        boolean first = markFirst(player, FIRST_CUT);
        post(player, new FocusEvent.Cut(player, FocusItem.cut(stack).id(), FocusItem.crystalId(stack), first));
    }

    private static boolean markFirst(Player player, String key) {
        ArtifactProgress p = player.getData(ModRegistries.ARTIFACT_PROGRESS);
        if (p.step(key) > 0) {
            return false;
        }
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p.withStep(key, 1));
        return true;
    }

    private static void post(Player player, FocusEvent event) {
        if (player.getServer() != null) {
            AdeptEvents.post(event);
        }
    }
}
