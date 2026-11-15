package ru.adept.core.hero;

import java.util.Locale;

/** Классы героев. Порядок значим: номер класса уходит на клиент (облик), новые классы — только в конец. */
public enum HeroClass {
    TRACKER, WARRIOR, ARCHER, COMMANDER, MAGE, HEALER;

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
