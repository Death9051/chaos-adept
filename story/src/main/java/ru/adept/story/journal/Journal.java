package ru.adept.story.journal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.adept.story.AdeptStory;
import ru.adept.story.StoryBranch;
import ru.adept.story.net.EntryView;
import ru.adept.story.net.StoryNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Журнал героя на сервере: события превращаются в записи по описаниям из датапака. */
public final class Journal {
    private Journal() {
    }

    /**
     * Событие для журнала. Срабатывают все подходящие описания: «один раз» — если ещё не получены,
     * набор — если в нём остался неиспользованный вариант. Возвращает добавленные записи.
     */
    public static List<EntryView> fire(MinecraftServer server, String type, Map<String, String> attrs) {
        JournalData data = JournalData.get(server);
        String branch = branch(server);
        List<EntryView> added = new ArrayList<>();
        for (JournalDef def : JournalDefs.all()) {
            if (!def.matches(type, attrs)) {
                continue;
            }
            EntryView entry = def.pool() ? fromPool(server, data, def, branch) : once(server, data, def, branch);
            if (entry != null) {
                added.add(entry);
            }
        }
        return added;
    }

    public static List<EntryView> fire(MinecraftServer server, String type) {
        return fire(server, type, Map.of());
    }

    /** Поглощение узла: определяет «первое за игру / первое на уровне / повторное». */
    public static List<EntryView> nodeAbsorbed(MinecraftServer server, int level) {
        JournalData data = JournalData.get(server);
        boolean firstGame = data.marks.add("node_absorbed:any");
        boolean firstLevel = data.marks.add("node_absorbed:" + level);
        data.setDirty();
        String first = firstGame ? "game" : firstLevel ? "level" : "none";
        return fire(server, "node_absorbed", Map.of("level", Integer.toString(level), "first", first));
    }

    /** Добавить запись в обход условий (отладочная команда). */
    public static EntryView force(MinecraftServer server, JournalDef def) {
        String branch = branch(server);
        List<String> keys = def.keysFor(branch);
        if (def.pool() && !keys.isEmpty()) {
            keys = List.of(keys.get(server.overworld().getRandom().nextInt(keys.size())));
        }
        return record(server, JournalData.get(server), def, keys);
    }

    /** Ключ отметки «впервые» (true, если отметка поставлена сейчас). */
    public static boolean markFirst(MinecraftServer server, String mark) {
        JournalData data = JournalData.get(server);
        boolean first = data.marks.add(mark);
        if (first) {
            data.setDirty();
        }
        return first;
    }

    public static String branch(MinecraftServer server) {
        return StoryBranch.of(server).name().toLowerCase(Locale.ROOT);
    }

    /** Текущий игровой день, с единицы. */
    public static int day(MinecraftServer server) {
        return (int) (server.overworld().getDayTime() / 24000L) + 1;
    }

    private static EntryView once(MinecraftServer server, JournalData data, JournalDef def, String branch) {
        if (data.done.contains(def.id())) {
            return null;
        }
        data.done.add(def.id());
        return record(server, data, def, def.keysFor(branch));
    }

    private static EntryView fromPool(MinecraftServer server, JournalData data, JournalDef def, String branch) {
        List<String> free = new ArrayList<>();
        for (String key : def.keysFor(branch)) {
            if (!data.usedKeys.contains(key)) {
                free.add(key);
            }
        }
        if (free.isEmpty()) {
            return null;
        }
        String key = free.get(server.overworld().getRandom().nextInt(free.size()));
        data.usedKeys.add(key);
        return record(server, data, def, List.of(key));
    }

    private static EntryView record(MinecraftServer server, JournalData data, JournalDef def, List<String> keys) {
        if (keys.isEmpty()) {
            AdeptStory.LOG.warn("Запись журнала {} без текста для ветки {}", def.id(), branch(server));
            return null;
        }
        EntryView entry = new EntryView(def.id(), day(server), def.page(), List.copyOf(keys), def.popup());
        data.entries.add(entry);
        data.setDirty();
        AdeptStory.LOG.info("Журнал: запись {}", def.id());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StoryNetwork.sendEntry(player, entry);
        }
        return entry;
    }
}
