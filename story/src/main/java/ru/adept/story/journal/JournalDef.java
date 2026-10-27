package ru.adept.story.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Описание записи журнала из {@code data/adept_story/journal/**.json}.
 *
 * <pre>
 * {
 *   "trigger": { "type": "node_absorbed", "level": 2, "first": "level" },
 *   "page": "entries" | "trader" | "epilogue",   (по умолчанию entries)
 *   "repeat": "once" | "pool",                    (по умолчанию once)
 *   "popup": true,                                (всплывающее сообщение, по умолчанию нет)
 *   "text": { "public": [ключи], "streamer": [ключи] } или { "common": [ключи] }
 * }
 * </pre>
 * once — ключи являются абзацами одной записи; pool — вариантами, из которых берётся один неиспользованный.
 */
public record JournalDef(String id, String type, Map<String, String> params, String page, boolean pool,
                         boolean popup, Map<String, List<String>> text) {

    public static final String PAGE_ENTRIES = "entries";
    public static final String PAGE_TRADER = "trader";
    public static final String PAGE_EPILOGUE = "epilogue";

    public static JournalDef parse(String id, JsonObject json) {
        JsonObject trigger = json.getAsJsonObject("trigger");
        if (trigger == null || !trigger.has("type")) {
            throw new IllegalArgumentException("нет trigger.type");
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : trigger.entrySet()) {
            if (!e.getKey().equals("type")) {
                params.put(e.getKey(), e.getValue().getAsString());
            }
        }
        String page = json.has("page") ? json.get("page").getAsString() : PAGE_ENTRIES;
        boolean pool = json.has("repeat") && json.get("repeat").getAsString().equals("pool");
        boolean popup = json.has("popup") && json.get("popup").getAsBoolean();

        JsonObject textJson = json.getAsJsonObject("text");
        if (textJson == null || textJson.isEmpty()) {
            throw new IllegalArgumentException("нет text");
        }
        Map<String, List<String>> text = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : textJson.entrySet()) {
            List<String> keys = new ArrayList<>();
            e.getValue().getAsJsonArray().forEach(k -> keys.add(k.getAsString()));
            text.put(e.getKey(), List.copyOf(keys));
        }
        return new JournalDef(id, trigger.get("type").getAsString(), Map.copyOf(params), page, pool, popup, Map.copyOf(text));
    }

    /** Подходит ли запись к событию: тип совпадает и все параметры записи совпадают с атрибутами события. */
    public boolean matches(String eventType, Map<String, String> attrs) {
        if (!type.equals(eventType)) {
            return false;
        }
        for (Map.Entry<String, String> p : params.entrySet()) {
            if (!p.getValue().equals(attrs.get(p.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** Ключи для ветки: своя ветка → common → public. */
    public List<String> keysFor(String branch) {
        List<String> keys = text.get(branch);
        if (keys == null) {
            keys = text.get("common");
        }
        if (keys == null) {
            keys = text.getOrDefault("public", List.of());
        }
        return keys;
    }
}
