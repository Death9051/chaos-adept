package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Числа баланса ядра. Файл: {@code config/adept_core-common.toml}.
 * Правится без пересборки; NeoForge перечитывает файл при сохранении, большинство значений
 * берутся в момент использования и применяются сразу.
 *
 * Правило: любое число, которое может понадобиться подкрутить по итогам стримов,
 * живёт здесь, а не в коде. Каждая часть среза добавляет свой раздел.
 */
public final class BalanceConfig {
    public static final ModConfigSpec SPEC;

    // Узлы
    public static final ModConfigSpec.IntValue NODE_MAX_LEVEL;
    public static final ModConfigSpec.IntValue NODE_BLOCKS_PER_LEVEL;

    // Выкачка
    public static final ModConfigSpec.IntValue EXTRACTION_MINUTES_MIN;
    public static final ModConfigSpec.IntValue EXTRACTION_MINUTES_MAX;
    public static final ModConfigSpec.IntValue EXTRACTION_DEATH_ROLLBACK_PERCENT;
    public static final ModConfigSpec.IntValue EXTRACTION_PAUSE_DISTANCE;

    // Волны
    public static final ModConfigSpec.IntValue WAVES_BASE;
    public static final ModConfigSpec.IntValue WAVES_MAX;
    public static final ModConfigSpec.IntValue WAVES_BREAK_MIN_SECONDS;
    public static final ModConfigSpec.IntValue WAVES_BREAK_MAX_SECONDS;

    // Торговец
    public static final ModConfigSpec.DoubleValue TRADER_PRICE_SPREAD;
    public static final ModConfigSpec.DoubleValue TRADER_DEMAND_BONUS;
    public static final ModConfigSpec.IntValue TRADER_OFFERS_MIN;
    public static final ModConfigSpec.IntValue TRADER_OFFERS_MAX;
    public static final ModConfigSpec.IntValue TRADER_STAY_SECONDS;
    public static final ModConfigSpec.IntValue TRADER_ACCOUNT_AFTER_NODES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Узлы концентрации магии").push("node");
        NODE_MAX_LEVEL = b.comment("Максимальный уровень узлов (число актов)")
                .defineInRange("maxLevel", 5, 1, 10);
        NODE_BLOCKS_PER_LEVEL = b.comment("Через сколько блоков от точки появления уровень узлов растёт на 1")
                .defineInRange("blocksPerLevel", 1500, 100, 100_000);
        b.pop();

        b.comment("Выкачка узла").push("extraction");
        EXTRACTION_MINUTES_MIN = b.comment("Длительность выкачки на первом уровне, минуты")
                .defineInRange("minutesAtFirstLevel", 10, 1, 120);
        EXTRACTION_MINUTES_MAX = b.comment("Длительность выкачки на максимальном уровне, минуты")
                .defineInRange("minutesAtMaxLevel", 30, 1, 180);
        EXTRACTION_DEATH_ROLLBACK_PERCENT = b.comment("Откат прогресса при смерти игрока, проценты")
                .defineInRange("deathRollbackPercent", 10, 0, 100);
        EXTRACTION_PAUSE_DISTANCE = b.comment("Дальше этого расстояния от узла выкачка и волны на паузе, блоки")
                .defineInRange("pauseDistance", 48, 8, 512);
        b.pop();

        b.comment("Волны защитников").push("waves");
        WAVES_BASE = b.comment("Число волн = base + уровень узла")
                .defineInRange("base", 3, 0, 20);
        WAVES_MAX = b.comment("Не больше стольких волн на узел")
                .defineInRange("max", 8, 1, 20);
        WAVES_BREAK_MIN_SECONDS = b.comment("Передышка между волнами, минимум, секунды")
                .defineInRange("breakMinSeconds", 60, 0, 600);
        WAVES_BREAK_MAX_SECONDS = b.comment("Передышка между волнами, максимум, секунды")
                .defineInRange("breakMaxSeconds", 120, 0, 600);
        b.pop();

        b.comment("Торговец Альянса").push("trader");
        TRADER_PRICE_SPREAD = b.comment("Разброс цен от вызова к вызову (0.15 = ±15%)")
                .defineInRange("priceSpread", 0.15, 0.0, 1.0);
        TRADER_DEMAND_BONUS = b.comment("Надбавка к цене категории в спросе (0.30 = +30%)")
                .defineInRange("demandBonus", 0.30, 0.0, 5.0);
        TRADER_OFFERS_MIN = b.comment("Случайных позиций в ассортименте, минимум")
                .defineInRange("randomOffersMin", 3, 0, 20);
        TRADER_OFFERS_MAX = b.comment("Случайных позиций в ассортименте, максимум")
                .defineInRange("randomOffersMax", 5, 0, 20);
        TRADER_STAY_SECONDS = b.comment("Сколько торговец ждёт без сделки, секунды")
                .defineInRange("staySeconds", 120, 10, 1200);
        TRADER_ACCOUNT_AFTER_NODES = b.comment("После скольких зачищенных узлов открывается счёт")
                .defineInRange("accountAfterNodes", 3, 1, 50);
        b.pop();

        SPEC = b.build();
    }

    private BalanceConfig() {
    }

    /** Длительность выкачки узла данного уровня в тиках: линейно от минимума к максимуму. */
    public static int extractionTicks(int level) {
        int max = NODE_MAX_LEVEL.get();
        double t = max <= 1 ? 0 : (double) (Math.max(1, Math.min(level, max)) - 1) / (max - 1);
        double minutes = EXTRACTION_MINUTES_MIN.get() + t * (EXTRACTION_MINUTES_MAX.get() - EXTRACTION_MINUTES_MIN.get());
        return (int) Math.round(minutes * 60 * 20);
    }

    /** Число волн на узле данного уровня. */
    public static int waveCount(int level) {
        return Math.min(WAVES_BASE.get() + level, WAVES_MAX.get());
    }
}
