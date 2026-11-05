package ru.adept.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Набеги героев на игрока между узлами. {@code threatLevel} — ступень угрозы (1 — разведка, 2 — отряды, 3 — крупные отряды).
 * {@code villageName} — деревня-отправитель (пусто, если деревень рядом нет).
 */
public abstract class RaidEvent extends AdeptEvent {
    private final UUID player;
    private final int threatLevel;
    private final String villageName;

    protected RaidEvent(MinecraftServer server, UUID player, int threatLevel, String villageName) {
        super(server);
        this.player = player;
        this.threatLevel = threatLevel;
        this.villageName = villageName;
    }

    public UUID getPlayerId() {
        return player;
    }

    public Player getPlayer() {
        return getServer().getPlayerList().getPlayer(player);
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public String getVillageName() {
        return villageName;
    }

    /** Звук рога: отряд будет через минуту, следопыт уже пометил игрока. */
    public static final class Horn extends RaidEvent {
        public Horn(MinecraftServer server, UUID player, int threatLevel, String villageName) {
            super(server, player, threatLevel, villageName);
        }
    }

    /** Отряд появился. {@code waiting} — игрок на базе, отряд ждёт у точки выхода. */
    public static final class Started extends RaidEvent {
        private final UUID squad;
        private final int size;
        private final boolean waiting;

        public Started(MinecraftServer server, UUID player, int threatLevel, String villageName, UUID squad, int size, boolean waiting) {
            super(server, player, threatLevel, villageName);
            this.squad = squad;
            this.size = size;
            this.waiting = waiting;
        }

        public UUID getSquad() {
            return squad;
        }

        public int getSize() {
            return size;
        }

        public boolean isWaiting() {
            return waiting;
        }
    }

    /** Набег закончился: {@code defeated} — отряд разгромлен (погиб или отступил), иначе — ушёл, не дождавшись. */
    public static final class Ended extends RaidEvent {
        private final boolean defeated;

        public Ended(MinecraftServer server, UUID player, int threatLevel, String villageName, boolean defeated) {
            super(server, player, threatLevel, villageName);
            this.defeated = defeated;
        }

        public boolean isDefeated() {
            return defeated;
        }
    }
}
