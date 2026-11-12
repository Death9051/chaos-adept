package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/**
 * События котла душ (срез 3). Сила души — {@code normal}, {@code strong} (герои, элита) или {@code guardian} (страж узла).
 */
public abstract class SoulEvent extends AdeptEvent {
    private final Player player;
    private final String strength;

    protected SoulEvent(Player player, String strength) {
        super(player.getServer());
        this.player = player;
        this.strength = strength;
    }

    public Player getPlayer() {
        return player;
    }

    public String getStrength() {
        return strength;
    }

    /**
     * Горнило сожгло душу и получило ману. {@code first} — первая сожжённая душа этого игрока
     * (запись журнала «котёл душ и первая сожжённая душа»).
     */
    public static final class Burned extends SoulEvent {
        private final int mana;
        private final boolean first;

        public Burned(Player player, String strength, int mana, boolean first) {
            super(player, strength);
            this.mana = mana;
            this.first = first;
        }

        public int getMana() {
            return mana;
        }

        public boolean isFirst() {
            return first;
        }
    }

    /** Горнило выдало ядро души. {@code first} — первое ядро этого игрока. */
    public static final class CoreObtained extends SoulEvent {
        private final boolean first;

        public CoreObtained(Player player, String strength, boolean first) {
            super(player, strength);
            this.first = first;
        }

        public boolean isFirst() {
            return first;
        }
    }
}
