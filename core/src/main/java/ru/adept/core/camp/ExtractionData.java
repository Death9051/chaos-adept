package ru.adept.core.camp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;

import java.util.Optional;

/** Хранение текущей выкачки в сохранении. Одиночная игра: одна выкачка за раз. */
public final class ExtractionData extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_extraction";
    private static final SavedData.Factory<ExtractionData> FACTORY =
            new SavedData.Factory<>(ExtractionData::new, ExtractionData::load, null);

    private ExtractionSession session;

    private ExtractionData() {
    }

    public static ExtractionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<ExtractionSession> session() {
        return Optional.ofNullable(session);
    }

    public void set(ExtractionSession value) {
        session = value;
        setDirty();
    }

    /** Отметить изменение полей текущей сессии. */
    public void touch() {
        setDirty();
    }

    private static ExtractionData load(CompoundTag tag, HolderLookup.Provider registries) {
        ExtractionData data = new ExtractionData();
        if (tag.contains("session")) {
            data.session = ExtractionSession.load(tag.getCompound("session"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (session != null) {
            tag.put("session", session.save());
        }
        return tag;
    }
}
