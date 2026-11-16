package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/** События щита из маны лёгкого экзокостюма (срез 3). */
public abstract class ShieldEvent extends AdeptEvent {
    private final Player player;

    protected ShieldEvent(Player player) {
        super(player.getServer());
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Удар пробил щит до нуля. {@code damage} — урон удара, {@code passed} — сколько прошло в здоровье,
     * {@code first} — впервые у этого игрока (запись журнала «щит из маны»).
     */
    public static final class Broken extends ShieldEvent {
        private final float damage;
        private final float passed;
        private final boolean first;

        public Broken(Player player, float damage, float passed, boolean first) {
            super(player);
            this.damage = damage;
            this.passed = passed;
            this.first = first;
        }

        public float getDamage() {
            return damage;
        }

        public float getPassed() {
            return passed;
        }

        public boolean isFirst() {
            return first;
        }
    }
}
