package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/** События торговца Альянса. */
public abstract class TraderEvent extends AdeptEvent {
    private final Player player;

    protected TraderEvent(Player player) {
        super(player.getServer());
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    /** Игрок начал вызов: в арке перехода раскрывается разрыв. */
    public static final class CallStarted extends TraderEvent {
        public CallStarted(Player player) {
            super(player);
        }
    }

    /** Торговец вышел из разрыва. {@code firstRitual} — первый ритуал за игру (выдача чертежа врат). */
    public static final class Arrived extends TraderEvent {
        private final boolean firstRitual;

        public Arrived(Player player, boolean firstRitual) {
            super(player);
            this.firstRitual = firstRitual;
        }

        public boolean isFirstRitual() {
            return firstRitual;
        }
    }

    /**
     * Сделка: предмет, категория ({@code purchase} — покупка у торговца; позже {@code trophy}, {@code pillar_material},
     * {@code trinket}, {@code semi_finished}, {@code other} — продажа ему) и количество.
     */
    public static final class Deal extends TraderEvent {
        private final String item;
        private final String category;
        private final int count;

        public Deal(Player player, String item, String category, int count) {
            super(player);
            this.item = item;
            this.category = category;
            this.count = count;
        }

        public String getItem() {
            return item;
        }

        public String getCategory() {
            return category;
        }

        public int getCount() {
            return count;
        }
    }

    /** У торговца открылся счёт. */
    public static final class AccountOpened extends TraderEvent {
        public AccountOpened(Player player) {
            super(player);
        }
    }

    /** Торговец ушёл в разрыв. */
    public static final class Left extends TraderEvent {
        public Left(Player player) {
            super(player);
        }
    }
}
