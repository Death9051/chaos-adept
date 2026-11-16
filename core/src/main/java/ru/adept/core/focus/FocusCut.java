package ru.adept.core.focus;

/**
 * Огранка — тип фокуса. В срезе 3 огранки нагрудника лёгкой брони (ответ щита на удар, «Материалы», «Фокусы»):
 * отталкивание, ослепляющая вспышка, быстрый перезаряд щита, замедление вокруг.
 */
public enum FocusCut {
    KNOCKBACK("knockback", "light/chest"),
    FLASH("flash", "light/chest"),
    RECHARGE("recharge", "light/chest"),
    SLOW("slow", "light/chest");

    private final String id;
    private final String segment;

    FocusCut(String id, String segment) {
        this.id = id;
        this.segment = segment;
    }

    public String id() {
        return id;
    }

    /** Сегмент, в гнездо которого встаёт фокус. */
    public String segment() {
        return segment;
    }

    public static FocusCut byId(String id) {
        for (FocusCut c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static FocusCut byIndex(int index) {
        FocusCut[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
