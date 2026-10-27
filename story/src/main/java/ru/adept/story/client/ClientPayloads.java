package ru.adept.story.client;

import net.minecraft.network.chat.Component;
import ru.adept.story.net.StoryNetwork;

/** Обработка пакетов «Сюжета» на клиенте (вызывается из {@link StoryNetwork} в главном потоке). */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void onSync(StoryNetwork.JournalSync payload) {
        JournalClient.set(payload.entries(), payload.seen());
    }

    public static void onAdded(StoryNetwork.JournalAdded payload) {
        JournalClient.add(payload.entry());
        if (payload.entry().popup()) {
            StoryOverlay.popup(payload.entry().keys());
        } else {
            StoryOverlay.notice();
        }
    }

    public static void onSubtitle(StoryNetwork.Subtitle payload) {
        Component speaker = Component.translatable(payload.speaker());
        for (String line : payload.lines()) {
            StoryOverlay.subtitle(speaker, Component.translatable(line));
        }
    }
}
