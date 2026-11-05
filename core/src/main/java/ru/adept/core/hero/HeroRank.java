package ru.adept.core.hero;

import java.util.Locale;

/** Ранги героев: ученик владеет первым приёмом класса, ветеран — двумя (мастер — позже). */
public enum HeroRank {
    APPRENTICE(1), VETERAN(2);

    public final int techniques;

    HeroRank(int techniques) {
        this.techniques = techniques;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HeroRank byId(String id) {
        return "veteran".equals(id) ? VETERAN : APPRENTICE;
    }
}
