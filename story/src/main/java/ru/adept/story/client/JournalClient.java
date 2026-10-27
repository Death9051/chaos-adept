package ru.adept.story.client;

import ru.adept.story.net.EntryView;

import java.util.ArrayList;
import java.util.List;

/** Копия журнала на клиенте. Обновляется пакетами с сервера. */
public final class JournalClient {
    private static final List<EntryView> ENTRIES = new ArrayList<>();
    private static int seen;

    private JournalClient() {
    }

    public static void set(List<EntryView> entries, int seenCount) {
        ENTRIES.clear();
        ENTRIES.addAll(entries);
        seen = seenCount;
    }

    public static void add(EntryView entry) {
        ENTRIES.add(entry);
    }

    public static List<EntryView> entries() {
        return List.copyOf(ENTRIES);
    }

    public static int seen() {
        return seen;
    }

    public static int unread() {
        return Math.max(0, ENTRIES.size() - seen);
    }

    public static void markSeen() {
        seen = ENTRIES.size();
    }

    public static void clear() {
        ENTRIES.clear();
        seen = 0;
    }
}
