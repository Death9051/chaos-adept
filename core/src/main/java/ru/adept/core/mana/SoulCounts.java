package ru.adept.core.mana;

import net.minecraft.nbt.CompoundTag;

/** Счётчики душ по силе (обычные, сильные, стражи) в теге предмета: котёл (горнило) и сосуды. */
public final class SoulCounts {
    private SoulCounts() {
    }

    public static int[] read(CompoundTag tag, String key) {
        int[] raw = tag.getIntArray(key);
        int[] out = new int[SoulStrength.values().length];
        System.arraycopy(raw, 0, out, 0, Math.min(raw.length, out.length));
        return out;
    }

    public static void write(CompoundTag tag, String key, int[] counts) {
        tag.putIntArray(key, counts.clone());
    }

    public static int total(int[] counts) {
        int sum = 0;
        for (int c : counts) {
            sum += c;
        }
        return sum;
    }

    /** Самая сильная душа в счётчиках; {@code null}, если пусто. */
    public static SoulStrength strongest(int[] counts) {
        for (int i = counts.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                return SoulStrength.byIndex(i);
            }
        }
        return null;
    }
}
