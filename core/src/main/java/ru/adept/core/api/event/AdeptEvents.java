package ru.adept.core.api.event;

import net.neoforged.neoforge.common.NeoForge;

/** Публикация событий ядра. Единая точка, чтобы позже добавить журнал событий для отладки. */
public final class AdeptEvents {
    private AdeptEvents() {
    }

    public static <T extends AdeptEvent> T post(T event) {
        return NeoForge.EVENT_BUS.post(event);
    }
}
