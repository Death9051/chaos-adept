package ru.adept.core.camp;

/** Чистая арифметика выкачки (без мира) — удобно проверять геймтестами. */
public final class ExtractionMath {
    private ExtractionMath() {
    }

    /** Порог прогресса (в тиках), на котором начинается волна {@code wave} из {@code waves}; последняя — на 100%. */
    public static double threshold(double total, int wave, int waves) {
        return total * wave / Math.max(1, waves);
    }

    /** Прогресс после смерти игрока: откат на {@code percent}% от всей выкачки, не ниже нуля. */
    public static double rollback(double progress, double total, int percent) {
        return Math.max(0.0, progress - total * percent / 100.0);
    }

    /** Состав волны {@code wave} (с 1): одержимые, шершни, огоньки, духи; последняя — со стражем. */
    public record Composition(int possessed, int hornets, int wisps, int spirits, boolean guardian) {
    }

    public static Composition composition(int wave, int waves, int possessedBase, int possessedPerWave,
                                          int hornetsFrom, int hornets, int wispsFrom, int wisps,
                                          int spiritsFrom, int spirits) {
        boolean last = wave >= waves;
        return new Composition(
                possessedBase + possessedPerWave * (wave - 1),
                wave >= hornetsFrom ? hornets : 0,
                wave >= wispsFrom ? wisps : 0,
                wave >= spiritsFrom && !last ? spirits : 0,
                last);
    }
}
