package ru.adept.core.rune;

/**
 * Восемь рун («Материалы», «Руны»). У каждой две роли: основа (руна стоит первой) и грань (стоит второй).
 * Порядок перечисления — порядок выбора на высекателе.
 */
public enum Rune {
    CURRENT("current"),   // Ток
    FLAME("flame"),       // Пламя
    BOND("bond"),         // Узы
    ECHO("echo"),         // Эхо
    RIFT("rift"),         // Разлом
    PULL("pull"),         // Тяга
    FROST("frost"),       // Иней
    VOID("void");         // Пустота

    private final String id;

    Rune(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Rune byId(String id) {
        for (Rune r : values()) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    public static Rune byIndex(int index) {
        Rune[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
