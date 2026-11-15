package ru.adept.core.rune;

/**
 * Особые слова из двух рун: при точном порядке рун системный результат (основа + грань) заменяется своим эффектом.
 * У каждой пары два слова: для снарядов (иглы, болты) и для брони (пластины).
 */
public enum RuneWord {
    STORM(Rune.CURRENT, Rune.ECHO, "storm", "lightning_rod"),          // Гроза / Громоотвод
    ERUPTION(Rune.FLAME, Rune.RIFT, "eruption", "fire_ring"),          // Извержение / Огненный круг
    ARCANE(Rune.BOND, Rune.PULL, "arcane", "anchor"),                  // Аркан / Якорь
    DOUBLE(Rune.ECHO, Rune.BOND, "double", "reflection"),              // Двойник / Отражение
    GLACIER(Rune.FROST, Rune.RIFT, "glacier", "ice_wall"),             // Ледник / Ледяная стена
    BLACK_HOLE(Rune.VOID, Rune.PULL, "black_hole", "distortion"),      // Чёрная дыра / Искажение
    SUPERCONDUCTOR(Rune.CURRENT, Rune.FROST, "superconductor", "static_shell"), // Сверхпроводник / Статический панцирь
    ASH(Rune.FLAME, Rune.VOID, "ash", "tempering"),                    // Пепел / Закалка
    PIT(Rune.RIFT, Rune.PULL, "pit", "rooting"),                       // Провал / Укоренение
    SHARDS(Rune.FROST, Rune.ECHO, "shards", "ice_cocoon");             // Осколки / Ледяной кокон

    public final Rune first;
    public final Rune second;
    public final String shotId;
    public final String armorId;

    RuneWord(Rune first, Rune second, String shotId, String armorId) {
        this.first = first;
        this.second = second;
        this.shotId = shotId;
        this.armorId = armorId;
    }

    /** Слово по точному порядку рун или null. */
    public static RuneWord of(Rune first, Rune second) {
        for (RuneWord w : values()) {
            if (w.first == first && w.second == second) {
                return w;
            }
        }
        return null;
    }
}
