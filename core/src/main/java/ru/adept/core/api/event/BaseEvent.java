package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/** События базы-измерения. */
public abstract class BaseEvent extends AdeptEvent {
    private final Player player;

    protected BaseEvent(Player player) {
        super(player.getServer());
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    /** Игрок вошёл на базу; {@code first} — впервые за игру. */
    public static final class Entered extends BaseEvent {
        private final boolean first;

        public Entered(Player player, boolean first) {
            super(player);
            this.first = first;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /** Игрок вернулся с базы в мир. */
    public static final class Left extends BaseEvent {
        public Left(Player player) {
            super(player);
        }
    }
}
