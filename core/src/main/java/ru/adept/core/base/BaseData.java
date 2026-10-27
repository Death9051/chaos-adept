package ru.adept.core.base;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import ru.adept.core.AdeptCore;
import ru.adept.core.config.BalanceConfig;

/** Общий запас энергии базы (электричество и мана раздельно) и признак, что база построена. */
public final class BaseData extends SavedData {
    private static final String DATA_NAME = AdeptCore.MOD_ID + "_base";
    private static final SavedData.Factory<BaseData> FACTORY =
            new SavedData.Factory<>(BaseData::new, BaseData::load, null);

    private int electricity;
    private int mana;
    private boolean built;

    private BaseData() {
    }

    public static BaseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static int capacity() {
        return BalanceConfig.BASE_ENERGY_CAPACITY.get();
    }

    public int electricity() {
        return electricity;
    }

    public int mana() {
        return mana;
    }

    public boolean isFull() {
        return electricity >= capacity() && mana >= capacity();
    }

    public void add(int electricityAmount, int manaAmount) {
        int e = Math.min(capacity(), electricity + Math.max(0, electricityAmount));
        int m = Math.min(capacity(), mana + Math.max(0, manaAmount));
        if (e != electricity || m != mana) {
            electricity = e;
            mana = m;
            setDirty();
        }
    }

    public boolean useElectricity(int amount) {
        if (electricity < amount) {
            return false;
        }
        electricity -= amount;
        setDirty();
        return true;
    }

    public boolean useMana(int amount) {
        if (mana < amount) {
            return false;
        }
        mana -= amount;
        setDirty();
        return true;
    }

    public void set(int electricityAmount, int manaAmount) {
        electricity = Math.max(0, Math.min(capacity(), electricityAmount));
        mana = Math.max(0, Math.min(capacity(), manaAmount));
        setDirty();
    }

    public boolean isBuilt() {
        return built;
    }

    void markBuilt() {
        built = true;
        setDirty();
    }

    private static BaseData load(CompoundTag tag, HolderLookup.Provider registries) {
        BaseData data = new BaseData();
        data.electricity = tag.getInt("electricity");
        data.mana = tag.getInt("mana");
        data.built = tag.getBoolean("built");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("electricity", electricity);
        tag.putInt("mana", mana);
        tag.putBoolean("built", built);
        return tag;
    }
}
