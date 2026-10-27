package ru.adept.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * Базовое событие ядра. Все события ядра публикуются на {@code NeoForge.EVENT_BUS}
 * на стороне сервера; «Сюжет» и другие моды подписываются на нужные подклассы.
 */
public abstract class AdeptEvent extends Event {
    private final MinecraftServer server;

    protected AdeptEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
