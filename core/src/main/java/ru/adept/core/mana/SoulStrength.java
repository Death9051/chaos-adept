package ru.adept.core.mana;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import ru.adept.core.config.ManaConfig;
import ru.adept.core.hero.HeroEntity;

/** Сила души: задаёт ману с души и шанс ядра. Порядок — от слабой к сильной (индекс в массивах счётчиков). */
public enum SoulStrength {
    NORMAL("normal"),
    STRONG("strong"),
    GUARDIAN("guardian");

    private final String id;

    SoulStrength(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public int mana() {
        return switch (this) {
            case NORMAL -> ManaConfig.SOUL_MANA_NORMAL.get();
            case STRONG -> ManaConfig.SOUL_MANA_STRONG.get();
            case GUARDIAN -> ManaConfig.SOUL_MANA_GUARDIAN.get();
        };
    }

    public double coreChance() {
        return switch (this) {
            case NORMAL -> ManaConfig.SOUL_CORE_CHANCE_NORMAL.get();
            case STRONG -> ManaConfig.SOUL_CORE_CHANCE_STRONG.get();
            case GUARDIAN -> ManaConfig.SOUL_CORE_CHANCE_GUARDIAN.get();
        };
    }

    /** Страж узла — по списку в конфиге; герои и мобы от {@code strongHealth} здоровья — сильные; остальные — обычные. */
    public static SoulStrength of(LivingEntity entity) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (ManaConfig.SOUL_GUARDIANS.get().contains(type)) {
            return GUARDIAN;
        }
        if (entity instanceof HeroEntity || entity.getMaxHealth() >= ManaConfig.SOUL_STRONG_HEALTH.get()) {
            return STRONG;
        }
        return NORMAL;
    }

    public static SoulStrength byIndex(int index) {
        SoulStrength[] all = values();
        return all[Math.max(0, Math.min(index, all.length - 1))];
    }

    public static SoulStrength byId(String id) {
        for (SoulStrength s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }
}
