package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * События рун (срез 3). Руны — id по порядку: {@code current} (Ток), {@code flame} (Пламя), {@code bond} (Узы),
 * {@code echo} (Эхо), {@code rift} (Разлом), {@code pull} (Тяга), {@code frost} (Иней), {@code void} (Пустота).
 * Материал гравировки — {@code copper}, {@code iron}, {@code steel}, {@code gold}, {@code red_gold}, {@code netherite},
 * {@code diamond}, {@code emerald}, {@code lapis}, {@code redstone}. Изделие — {@code needle}, {@code bolt}, {@code plate}.
 */
public abstract class RuneEvent extends AdeptEvent {
    private final Player player;
    private final List<String> runes;
    private final String material;
    private final String item;

    protected RuneEvent(Player player, List<String> runes, String material, String item) {
        super(player.getServer());
        this.player = player;
        this.runes = List.copyOf(runes);
        this.material = material;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public List<String> getRunes() {
        return runes;
    }

    public String getMaterial() {
        return material;
    }

    public String getItem() {
        return item;
    }

    /**
     * Игрок забрал гравированные изделия из высекателя. {@code word} — id особого слова для снарядов или брони
     * (пусто, если слова нет), {@code first} — первая гравировка этого игрока (запись журнала «первая руна»).
     */
    public static final class Engraved extends RuneEvent {
        private final int count;
        private final String word;
        private final boolean first;

        public Engraved(Player player, List<String> runes, String material, String item, int count, String word, boolean first) {
            super(player, runes, material, item);
            this.count = count;
            this.word = word;
            this.first = first;
        }

        public int getCount() {
            return count;
        }

        public String getWord() {
            return word;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /** При подборе болта гравировка стёрлась целиком (один бросок по шансу материала). */
    public static final class Erased extends RuneEvent {
        public Erased(Player player, List<String> runes, String material, String item) {
            super(player, runes, material, item);
        }
    }
}
