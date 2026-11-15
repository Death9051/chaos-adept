package ru.adept.core.api.event;

import net.minecraft.world.entity.player.Player;

/** События стартового предмета «Зачаток» (единственный предмет на старте, из него собирается стол артефактов). */
public abstract class SeedEvent extends AdeptEvent {
    public static final String FIRST_JOIN = "first_join";
    public static final String COMMAND = "command";

    private final Player player;

    protected SeedEvent(Player player) {
        super(player.getServer());
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Игрок получил зачаток. {@code source} — {@link #FIRST_JOIN} (первый вход в мир, один раз на игрока —
     * для записи журнала) или {@link #COMMAND} (возвращён командой {@code /adept seed}).
     */
    public static final class Received extends SeedEvent {
        private final String source;

        public Received(Player player, String source) {
            super(player);
            this.source = source;
        }

        public String getSource() {
            return source;
        }

        public boolean isFirstJoin() {
            return FIRST_JOIN.equals(source);
        }
    }
}
