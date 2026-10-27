package ru.adept.core.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Флаги мира: простое хранилище «ключ → строка», которое живёт вместе с сохранением.
 * Ядро хранит, другие моды записывают (например, «Сюжет» фиксирует ветку при создании мира).
 * Ключи лучше начинать с id мода: {@code adept_story:branch}.
 */
public final class WorldFlags extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_flags";
    private static final SavedData.Factory<WorldFlags> FACTORY =
            new SavedData.Factory<>(WorldFlags::new, WorldFlags::load, null);

    private final Map<String, String> flags = new HashMap<>();

    private WorldFlags() {
    }

    /** Флаги текущего мира. Хранятся в данных верхнего мира, общие для всех измерений. */
    public static WorldFlags get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(flags.get(key));
    }

    public boolean has(String key) {
        return flags.containsKey(key);
    }

    public void set(String key, String value) {
        if (!value.equals(flags.put(key, value))) {
            setDirty();
        }
    }

    public void remove(String key) {
        if (flags.remove(key) != null) {
            setDirty();
        }
    }

    private static WorldFlags load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldFlags data = new WorldFlags();
        CompoundTag stored = tag.getCompound("flags");
        for (String key : stored.getAllKeys()) {
            data.flags.put(key, stored.getString(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stored = new CompoundTag();
        flags.forEach(stored::putString);
        tag.put("flags", stored);
        return tag;
    }
}
