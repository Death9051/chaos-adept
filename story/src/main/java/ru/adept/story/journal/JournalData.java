package ru.adept.story.journal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.story.AdeptStory;
import ru.adept.story.net.EntryView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Состояние сюжета в мире (игра одиночная, поэтому одно на мир): полученные записи,
 * использованные варианты наборов, отметки «впервые», сколько записей игрок уже видел.
 */
public final class JournalData extends SavedData {
    private static final String DATA_NAME = AdeptStory.MOD_ID + "_journal";
    private static final SavedData.Factory<JournalData> FACTORY =
            new SavedData.Factory<>(JournalData::new, JournalData::load, null);

    /** Записи в порядке получения. */
    public final List<EntryView> entries = new ArrayList<>();
    /** id записей «один раз», которые уже получены. */
    public final Set<String> done = new HashSet<>();
    /** Использованные ключи из наборов (pool). */
    public final Set<String> usedKeys = new HashSet<>();
    /** Отметки «впервые» для событий: node_absorbed:any, node_absorbed:2 и т. п. */
    public final Set<String> marks = new HashSet<>();
    /** Сколько записей игрок уже видел в журнале. */
    public int seen;
    /** Уровень узлов, о котором торговец уже объявил (вехи). */
    public int traderTier = 1;
    /** Первый ритуал прошёл, запись о нём ждёт ухода торговца. */
    public boolean ritualPending;

    private JournalData() {
    }

    public static JournalData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /** Всё сбросить (отладочная команда и автотесты). */
    public void reset() {
        entries.clear();
        done.clear();
        usedKeys.clear();
        marks.clear();
        seen = 0;
        traderTier = 1;
        ritualPending = false;
        setDirty();
    }

    private static JournalData load(CompoundTag tag, HolderLookup.Provider registries) {
        JournalData data = new JournalData();
        for (Tag t : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            List<String> keys = new ArrayList<>();
            for (Tag k : e.getList("keys", Tag.TAG_STRING)) {
                keys.add(k.getAsString());
            }
            data.entries.add(new EntryView(e.getString("id"), e.getInt("day"), e.getString("page"), keys, e.getBoolean("popup")));
        }
        readSet(tag, "done", data.done);
        readSet(tag, "used", data.usedKeys);
        readSet(tag, "marks", data.marks);
        data.seen = tag.getInt("seen");
        data.traderTier = Math.max(1, tag.getInt("trader_tier"));
        data.ritualPending = tag.getBoolean("ritual_pending");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (EntryView e : entries) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.id());
            c.putInt("day", e.day());
            c.putString("page", e.page());
            ListTag keys = new ListTag();
            e.keys().forEach(k -> keys.add(StringTag.valueOf(k)));
            c.put("keys", keys);
            c.putBoolean("popup", e.popup());
            list.add(c);
        }
        tag.put("entries", list);
        writeSet(tag, "done", done);
        writeSet(tag, "used", usedKeys);
        writeSet(tag, "marks", marks);
        tag.putInt("seen", seen);
        tag.putInt("trader_tier", traderTier);
        tag.putBoolean("ritual_pending", ritualPending);
        return tag;
    }

    private static void readSet(CompoundTag tag, String name, Set<String> into) {
        for (Tag t : tag.getList(name, Tag.TAG_STRING)) {
            into.add(t.getAsString());
        }
    }

    private static void writeSet(CompoundTag tag, String name, Set<String> from) {
        ListTag list = new ListTag();
        from.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put(name, list);
    }
}
