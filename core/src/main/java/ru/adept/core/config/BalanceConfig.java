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
    public static final ModConfigSpec.IntValue NODE_CELL_SIZE;
    public static final ModConfigSpec.IntValue NODE_MIN_SPAWN_DISTANCE;
    public static final ModConfigSpec.IntValue RESONATOR_RANGE;
    public static final ModConfigSpec.IntValue RESONATOR_COOLDOWN_TICKS;

    // Выкачка
    public static final ModConfigSpec.IntValue EXTRACTION_MINUTES_MIN;
    public static final ModConfigSpec.IntValue EXTRACTION_MINUTES_MAX;
    public static final ModConfigSpec.IntValue EXTRACTION_DEATH_ROLLBACK_PERCENT;
    public static final ModConfigSpec.IntValue EXTRACTION_PAUSE_DISTANCE;

    public static final ModConfigSpec.IntValue CAMP_RADIUS;
    public static final ModConfigSpec.DoubleValue EXTRACTION_WAVE_SPEED;
    public static final ModConfigSpec.IntValue EXTRACTOR_HEALTH;
    public static final ModConfigSpec.ConfigValue<String> EXTRACTOR_REPAIR_ITEM;
    public static final ModConfigSpec.IntValue EXTRACTOR_REPAIR_AMOUNT;

    // Поглощение биома
    public static final ModConfigSpec.IntValue ABSORB_MAX_RADIUS;
    public static final ModConfigSpec.IntValue ABSORB_MAX_CELLS;
    public static final ModConfigSpec.IntValue ABSORB_SAMPLES_PER_TICK;
    public static final ModConfigSpec.IntValue ABSORB_CHUNKS_PER_TICK;

    // Волны
    public static final ModConfigSpec.IntValue WAVE_POSSESSED_BASE;
    public static final ModConfigSpec.IntValue WAVE_POSSESSED_PER_WAVE;
    public static final ModConfigSpec.IntValue WAVE_HORNETS_FROM;
    public static final ModConfigSpec.IntValue WAVE_HORNETS_COUNT;
    public static final ModConfigSpec.IntValue WAVE_WISPS_FROM;
    public static final ModConfigSpec.IntValue WAVE_WISPS_COUNT;
    public static final ModConfigSpec.IntValue WAVE_SPIRITS_FROM;
    public static final ModConfigSpec.IntValue WAVE_SPIRITS_COUNT;
    public static final ModConfigSpec.IntValue WAVE_SPAWN_MIN_RADIUS;
    public static final ModConfigSpec.IntValue WAVE_SPAWN_MAX_RADIUS;

    // Мобы
    public static final ModConfigSpec.DoubleValue POSSESSED_HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue POSSESSED_DAMAGE;
    public static final ModConfigSpec.DoubleValue POSSESSED_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HORNET_HEALTH;
    public static final ModConfigSpec.DoubleValue HORNET_DAMAGE;
    public static final ModConfigSpec.IntValue HORNET_POISON_TICKS;
    public static final ModConfigSpec.DoubleValue WISP_HEALTH;
    public static final ModConfigSpec.DoubleValue WISP_EXPLOSION;
    public static final ModConfigSpec.DoubleValue SPIRIT_HEALTH;
    public static final ModConfigSpec.IntValue SPIRIT_EXTRACTOR_DAMAGE;
    public static final ModConfigSpec.DoubleValue GUARDIAN_HEALTH;
    public static final ModConfigSpec.DoubleValue GUARDIAN_DAMAGE;
    public static final ModConfigSpec.IntValue GUARDIAN_VINES_INTERVAL;
    public static final ModConfigSpec.IntValue GUARDIAN_CALL_INTERVAL;
    public static final ModConfigSpec.IntValue GUARDIAN_CALL_COUNT;
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

    // Стол артефактов: рецепты в формате «id*кол-во;id*кол-во»
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_WIRING;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_COIL;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_EMITTER;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_MANA_CIRCUIT;
    public static final ModConfigSpec.ConfigValue<String> TABLE_RESONATOR;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CAMP;
    public static final ModConfigSpec.ConfigValue<String> TABLE_BLADE_PROTOTYPE;
    public static final ModConfigSpec.ConfigValue<String> TABLE_BLADE_EDGE_1;

    // Энергоклинок
    public static final ModConfigSpec.DoubleValue BLADE_BASE_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLADE_ATTACK_SPEED;
    public static final ModConfigSpec.DoubleValue BLADE_EDGE_DAMAGE_PER_STEP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Узлы концентрации магии").push("node");
        NODE_MAX_LEVEL = b.comment("Максимальный уровень узлов (число актов)")
                .defineInRange("maxLevel", 5, 1, 10);
        NODE_BLOCKS_PER_LEVEL = b.comment("Через сколько блоков от точки появления уровень узлов растёт на 1")
                .defineInRange("blocksPerLevel", 1500, 100, 100_000);
        NODE_CELL_SIZE = b.comment("Размер ячейки сетки узлов, блоки (кратно 16). В ячейке не больше одного узла;",
                        "соседние ячейки с тем же биомом уступают одной — так узлов не больше одного на биом")
                .defineInRange("cellSize", 384, 64, 4096);
        NODE_MIN_SPAWN_DISTANCE = b.comment("Ближе этого к точке появления узлов нет, блоки")
                .defineInRange("minSpawnDistance", 160, 0, 10_000);
        RESONATOR_RANGE = b.comment("Дальность поиска резонатора, блоки")
                .defineInRange("resonatorRange", 1200, 64, 10_000);
        RESONATOR_COOLDOWN_TICKS = b.comment("Перезарядка резонатора, тики")
                .defineInRange("resonatorCooldownTicks", 40, 0, 6000);
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
        CAMP_RADIUS = b.comment("Лагерь разворачивается не дальше стольких блоков от кристалла узла")
                .defineInRange("campRadius", 12, 2, 64);
        EXTRACTION_WAVE_SPEED = b.comment("Скорость выкачки, пока волна не отбита (1.0 — полная)")
                .defineInRange("waveSpeed", 0.25, 0.0, 1.0);
        EXTRACTOR_HEALTH = b.comment("Прочность установки выкачки")
                .defineInRange("extractorHealth", 100, 1, 10_000);
        EXTRACTOR_REPAIR_ITEM = b.comment("Предмет для ремонта установки (ПКМ по установке)")
                .define("extractorRepairItem", "minecraft:iron_ingot");
        EXTRACTOR_REPAIR_AMOUNT = b.comment("Сколько прочности чинит один предмет")
                .defineInRange("extractorRepairAmount", 25, 1, 10_000);
        b.pop();

        b.comment("Поглощение биома после зачистки узла").push("absorb");
        ABSORB_MAX_RADIUS = b.comment("Дальше стольких блоков от узла биом не поглощается (защита от огромных биомов)")
                .defineInRange("maxRadius", 768, 32, 4096);
        ABSORB_MAX_CELLS = b.comment("Предел ячеек 8×8 в одной области поглощения")
                .defineInRange("maxCells", 40_000, 100, 1_000_000);
        ABSORB_SAMPLES_PER_TICK = b.comment("Сколько ячеек проверять за тик при поиске границ биома")
                .defineInRange("samplesPerTick", 1500, 10, 100_000);
        ABSORB_CHUNKS_PER_TICK = b.comment("Сколько чанков превращать в пустошь за тик")
                .defineInRange("chunksPerTick", 2, 1, 64);
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
        WAVE_POSSESSED_BASE = b.comment("Одержимых зверей в первой волне")
                .defineInRange("possessedBase", 4, 0, 50);
        WAVE_POSSESSED_PER_WAVE = b.comment("Прибавка одержимых с каждой следующей волной")
                .defineInRange("possessedPerWave", 1, 0, 20);
        WAVE_HORNETS_FROM = b.comment("С какой волны приходят шершни (номер с 1)")
                .defineInRange("hornetsFromWave", 1, 1, 20);
        WAVE_HORNETS_COUNT = b.comment("Шершней в стае")
                .defineInRange("hornetsCount", 3, 0, 30);
        WAVE_WISPS_FROM = b.comment("С какой волны приходят блуждающие огоньки")
                .defineInRange("wispsFromWave", 2, 1, 20);
        WAVE_WISPS_COUNT = b.comment("Блуждающих огоньков в волне")
                .defineInRange("wispsCount", 2, 0, 30);
        WAVE_SPIRITS_FROM = b.comment("С какой волны приходят тлеющие духи (диверсанты)")
                .defineInRange("spiritsFromWave", 2, 1, 20);
        WAVE_SPIRITS_COUNT = b.comment("Тлеющих духов в волне")
                .defineInRange("spiritsCount", 1, 0, 30);
        WAVE_SPAWN_MIN_RADIUS = b.comment("Волна появляется не ближе стольких блоков от узла")
                .defineInRange("spawnMinRadius", 16, 4, 128);
        WAVE_SPAWN_MAX_RADIUS = b.comment("и не дальше стольких")
                .defineInRange("spawnMaxRadius", 24, 4, 128);
        b.pop();

        b.comment("Противники на узле").push("mobs");
        POSSESSED_HEALTH_MULTIPLIER = b.comment("Множитель здоровья одержимых зверей")
                .defineInRange("possessedHealthMultiplier", 1.5, 0.1, 20.0);
        POSSESSED_DAMAGE = b.comment("Урон одержимого зверя")
                .defineInRange("possessedDamage", 3.0, 0.0, 100.0);
        POSSESSED_SPEED_MULTIPLIER = b.comment("Множитель скорости одержимых зверей")
                .defineInRange("possessedSpeedMultiplier", 1.25, 0.1, 5.0);
        HORNET_HEALTH = b.defineInRange("hornetHealth", 6.0, 1.0, 1000.0);
        HORNET_DAMAGE = b.defineInRange("hornetDamage", 2.0, 0.0, 100.0);
        HORNET_POISON_TICKS = b.comment("Длительность яда от шершня, тики")
                .defineInRange("hornetPoisonTicks", 60, 0, 1200);
        WISP_HEALTH = b.defineInRange("wispHealth", 8.0, 1.0, 1000.0);
        WISP_EXPLOSION = b.comment("Сила взрыва огонька (блоки не ломает)")
                .defineInRange("wispExplosion", 1.5, 0.1, 10.0);
        SPIRIT_HEALTH = b.defineInRange("spiritHealth", 12.0, 1.0, 1000.0);
        SPIRIT_EXTRACTOR_DAMAGE = b.comment("Урон тлеющего духа по установке выкачки")
                .defineInRange("spiritExtractorDamage", 25, 0, 10_000);
        GUARDIAN_HEALTH = b.comment("Рогатый вожак: здоровье")
                .defineInRange("guardianHealth", 120.0, 1.0, 10_000.0);
        GUARDIAN_DAMAGE = b.comment("Рогатый вожак: урон")
                .defineInRange("guardianDamage", 8.0, 0.0, 1000.0);
        GUARDIAN_VINES_INTERVAL = b.comment("Рогатый вожак: лозы (замедление рядом), раз в столько тиков")
                .defineInRange("guardianVinesInterval", 200, 20, 6000);
        GUARDIAN_CALL_INTERVAL = b.comment("Рогатый вожак: зов зверей, раз в столько тиков")
                .defineInRange("guardianCallInterval", 400, 20, 6000);
        GUARDIAN_CALL_COUNT = b.comment("Рогатый вожак: сколько зверей приходит на зов")
                .defineInRange("guardianCallCount", 2, 0, 20);
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

        b.comment("Стол артефактов. Рецепт: список «id*кол-во» через точку с запятой, без *кол-во — 1 штука")
                .push("table");
        TABLE_CRUDE_WIRING = b.comment("Грубая проводка")
                .define("crudeWiring", "minecraft:copper_ingot*2");
        TABLE_CRUDE_COIL = b.comment("Грубая катушка")
                .define("crudeCoil", "minecraft:copper_ingot*3;minecraft:iron_ingot");
        TABLE_CRUDE_EMITTER = b.comment("Грубый эмиттер")
                .define("crudeEmitter", "minecraft:copper_ingot*2;minecraft:iron_ingot;minecraft:redstone*2");
        TABLE_CRUDE_MANA_CIRCUIT = b.comment("Грубый контур маны")
                .define("crudeManaCircuit", "minecraft:gold_ingot*2;minecraft:lapis_lazuli*3");
        TABLE_RESONATOR = b.comment("Резонатор")
                .define("resonator", "adept_core:crude_coil*2;adept_core:crude_wiring;minecraft:compass");
        TABLE_CAMP = b.comment("Лагерь (установка выкачки и стационарный переход)")
                .define("camp", "adept_core:crude_coil*2;adept_core:crude_emitter;adept_core:crude_mana_circuit;minecraft:iron_block");
        TABLE_BLADE_PROTOTYPE = b.comment("Прототип энергоклинка")
                .define("bladePrototype", "adept_core:crude_emitter;adept_core:crude_coil;adept_core:crude_mana_circuit;adept_core:crude_wiring*2");
        TABLE_BLADE_EDGE_1 = b.comment("Клинок, эмиттер, путь «Кромка», ступень I")
                .define("bladeEdge1", "adept_core:crude_emitter*2;adept_core:crude_coil");
        b.pop();

        b.comment("Энергоклинок").push("blade");
        BLADE_BASE_DAMAGE = b.comment("Урон прототипа (как у меча: полный урон удара)")
                .defineInRange("baseDamage", 6.0, 1.0, 100.0);
        BLADE_ATTACK_SPEED = b.comment("Скорость атаки (ударов в секунду)")
                .defineInRange("attackSpeed", 1.8, 0.1, 10.0);
        BLADE_EDGE_DAMAGE_PER_STEP = b.comment("Прибавка урона за каждую ступень пути «Кромка»")
                .defineInRange("edgeDamagePerStep", 1.5, 0.0, 50.0);
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
