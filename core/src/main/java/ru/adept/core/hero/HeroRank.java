package ru.adept.core.hero;

import java.util.Locale;

/** Ранги героев: ученик владеет первым приёмом класса, ветеран — двумя, мастер — тремя и носит лучшее снаряжение. */
public enum HeroRank {
    APPRENTICE(1), VETERAN(2), MASTER(3);

    public final int techniques;

    HeroRank(int techniques) {
        this.techniques = techniques;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HeroRank byId(String id) {
        for (HeroRank r : values()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return APPRENTICE;
    }
}
