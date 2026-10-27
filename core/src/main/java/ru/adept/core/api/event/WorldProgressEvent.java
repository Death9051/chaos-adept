package ru.adept.core.api.event;

import net.minecraft.server.MinecraftServer;

/** События общего прогресса мира. */
public abstract class WorldProgressEvent extends AdeptEvent {
    private final int oldValue;
    private final int newValue;

    protected WorldProgressEvent(MinecraftServer server, int oldValue, int newValue) {
        super(server);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getOldValue() {
        return oldValue;
    }

    public int getNewValue() {
        return newValue;
    }

    /** Переход на новый уровень узлов (веха). */
    public static final class NodeTierChanged extends WorldProgressEvent {
        public NodeTierChanged(MinecraftServer server, int oldTier, int newTier) {
            super(server, oldTier, newTier);
        }
    }

    /** Угроза мира изменилась. */
    public static final class ThreatChanged extends WorldProgressEvent {
        public ThreatChanged(MinecraftServer server, int oldThreat, int newThreat) {
            super(server, oldThreat, newThreat);
        }
    }
}
