package ru.adept.story.journal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ru.adept.story.AdeptStory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Записи журнала из датапака: {@code data/adept_story/journal/<раздел>/<имя>.json}, id записи — путь без .json. */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class JournalDefs extends SimpleJsonResourceReloadListener {
    /** Типы триггеров, которые мод уже умеет вызывать. Остальные ждут событий ядра из следующих срезов. */
    public static final Set<String> SUPPORTED = Set.of(
            "first_dawn", "item_obtained", "artifact_table_built", "biome_entered",
            "artifact_pair_chosen", "artifact_prototype_crafted", "artifact_segment_apex",
            "base_entered", "node_wave_cleared", "node_player_died", "node_cleansed", "node_absorbed",
            "trader_account_opened", "ritual_done", "node_level_reached");

    private static volatile Map<String, JournalDef> defs = Map.of();

    private JournalDefs() {
        super(new Gson(), "journal");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new JournalDefs());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JournalDef> loaded = new TreeMap<>();
        Set<String> waiting = new TreeSet<>();
        files.forEach((rl, json) -> {
            if (!rl.getNamespace().equals(AdeptStory.MOD_ID)) {
                return;
            }
            try {
                JournalDef def = JournalDef.parse(rl.getPath(), json.getAsJsonObject());
                loaded.put(def.id(), def);
                if (!SUPPORTED.contains(def.type())) {
                    waiting.add(def.type());
                }
            } catch (RuntimeException e) {
                AdeptStory.LOG.error("Запись журнала {} не прочитана: {}", rl, e.getMessage());
            }
        });
        defs = Map.copyOf(loaded);
        AdeptStory.LOG.info("Журнал: {} записей; ждут событий следующих срезов: {}", loaded.size(), waiting);
    }

    public static Collection<JournalDef> all() {
        return defs.values();
    }

    public static JournalDef get(String id) {
        return defs.get(id);
    }

    public static Set<String> ids() {
        return new TreeSet<>(defs.keySet());
    }
}
