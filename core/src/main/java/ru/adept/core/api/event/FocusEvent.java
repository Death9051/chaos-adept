package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/**
 * События фокусов (срез 3). Огранка — тип фокуса ({@code knockback}, {@code flash}, {@code recharge}, {@code slow}
 * у нагрудника лёгкой брони), кристалл — {@code quartz}, {@code lapis}, {@code amethyst}, {@code emerald},
 * {@code diamond}, {@code node}. Сегмент — где стоит фокус ({@code light/chest}).
 */
public abstract class FocusEvent extends AdeptEvent {
    private final Player player;
    private final String cut;
    private final String crystal;

    protected FocusEvent(Player player, String cut, String crystal) {
        super(player.getServer());
        this.player = player;
        this.cut = cut;
        this.crystal = crystal;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCut() {
        return cut;
    }

    public String getCrystal() {
        return crystal;
    }

    /**
     * Игрок забрал новый фокус из ограночной машины. {@code first} — первый фокус этого игрока
     * (запись журнала «первый фокус»).
     */
    public static final class Cut extends FocusEvent {
        private final boolean first;

        public Cut(Player player, String cut, String crystal, boolean first) {
            super(player, cut, crystal);
            this.first = first;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /**
     * Прочность кончилась, фокус треснул: погас, остался в гнезде, его можно починить на ограночной машине.
     * {@code first} — первый треснувший фокус игрока (запись журнала).
     */
    public static final class Cracked extends FocusEvent {
        private final String segment;
        private final boolean first;

        public Cracked(Player player, String cut, String crystal, String segment, boolean first) {
            super(player, cut, crystal);
            this.segment = segment;
            this.first = first;
        }

        public String getSegment() {
            return segment;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /** Прочность кончилась, фокус сломался и пропал из гнезда. */
    public static final class Broken extends FocusEvent {
        private final String segment;

        public Broken(Player player, String cut, String crystal, String segment) {
            super(player, cut, crystal);
            this.segment = segment;
        }

        public String getSegment() {
            return segment;
        }
    }
}
