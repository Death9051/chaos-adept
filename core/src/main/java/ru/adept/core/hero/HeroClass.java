package ru.adept.core.hero;

import java.util.Locale;

/** Классы героев среза 2 (маг и целитель — позже). */
public enum HeroClass {
    TRACKER, WARRIOR, ARCHER, COMMANDER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HeroClass byId(String id) {
        for (HeroClass c : values()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return WARRIOR;
    }
}
