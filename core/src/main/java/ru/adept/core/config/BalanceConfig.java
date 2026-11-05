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

    // База
    public static final ModConfigSpec.IntValue BASE_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue KEY_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue NODE_SHARDS_PER_LEVEL;
    public static final ModConfigSpec.IntValue GENERATOR_BURN_TICKS;
    public static final ModConfigSpec.IntValue GENERATOR_ELECTRICITY_PER_TICK;
    public static final ModConfigSpec.IntValue GENERATOR_MANA_PER_TICK;
    public static final ModConfigSpec.IntValue CRUSHER_TICKS;
    public static final ModConfigSpec.IntValue CRUSHER_ELECTRICITY_PER_TICK;
    public static final ModConfigSpec.IntValue SMELTER_TICKS;
    public static final ModConfigSpec.IntValue SMELTER_MANA_PER_TICK;
    public static final ModConfigSpec.IntValue SMELTER_TANK_MB;
    public static final ModConfigSpec.IntValue SMELTER_MB_PER_DUST;
    public static final ModConfigSpec.IntValue SMELTER_MB_PER_INGOT;

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

    // Новые противники на узле (срез 2)
    public static final ModConfigSpec.IntValue WAVE_EXTRA_MIN_NODE_LEVEL;
    public static final ModConfigSpec.IntValue WAVE_SPORELINGS_FROM;
    public static final ModConfigSpec.IntValue WAVE_SPORELINGS_COUNT;
    public static final ModConfigSpec.IntValue WAVE_BOARS_FROM;
    public static final ModConfigSpec.IntValue WAVE_BOARS_COUNT;
    public static final ModConfigSpec.IntValue WAVE_ARMADILLOS_FROM;
    public static final ModConfigSpec.IntValue WAVE_ARMADILLOS_COUNT;
    public static final ModConfigSpec.IntValue BOAR_CHARGE_MIN_RANGE;
    public static final ModConfigSpec.IntValue BOAR_CHARGE_MAX_RANGE;
    public static final ModConfigSpec.IntValue BOAR_WINDUP_TICKS;
    public static final ModConfigSpec.IntValue BOAR_CHARGE_TICKS;
    public static final ModConfigSpec.DoubleValue BOAR_CHARGE_SPEED;
    public static final ModConfigSpec.DoubleValue BOAR_RAM_MULTIPLIER;
    public static final ModConfigSpec.IntValue BOAR_STUN_TICKS;
    public static final ModConfigSpec.IntValue BOAR_CHARGE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue ARMADILLO_FRONT_ARC;
    public static final ModConfigSpec.DoubleValue ARMADILLO_CURLED_REDUCTION;
    public static final ModConfigSpec.IntValue ARMADILLO_CURL_TICKS;
    public static final ModConfigSpec.IntValue ARMADILLO_CURL_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue SPORELING_SPLIT_COUNT;

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
    public static final ModConfigSpec.ConfigValue<String> TRADER_FIXED_OFFERS;
    public static final ModConfigSpec.ConfigValue<String> TRADER_OFFER_POOL;
    public static final ModConfigSpec.IntValue TRADER_MAX_USES;
    public static final ModConfigSpec.IntValue TRADER_ARRIVAL_TICKS;

    // Стол артефактов: рецепты в формате «id*кол-во;id*кол-во»
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_WIRING;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_COIL;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_EMITTER;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CRUDE_MANA_CIRCUIT;
    public static final ModConfigSpec.ConfigValue<String> TABLE_RESONATOR;
    public static final ModConfigSpec.ConfigValue<String> TABLE_CAMP;
    public static final ModConfigSpec.ConfigValue<String> TABLE_KEY;
    public static final ModConfigSpec.ConfigValue<String> TABLE_BLADE_PROTOTYPE;
    public static final ModConfigSpec.ConfigValue<String> TABLE_BLADE_EDGE_1;

    // Угроза
    public static final ModConfigSpec.IntValue THREAT_SCOUTS_AT;
    public static final ModConfigSpec.IntValue THREAT_SQUADS_AT;
    public static final ModConfigSpec.IntValue THREAT_WARBANDS_AT;
    public static final ModConfigSpec.IntValue THREAT_BIOME_ABSORBED;
    public static final ModConfigSpec.IntValue THREAT_VILLAGE_DESTROYED;
    public static final ModConfigSpec.IntValue THREAT_ELDER_KILLED;
    public static final ModConfigSpec.IntValue THREAT_VILLAGER_KILLED;
    public static final ModConfigSpec.IntValue THREAT_RAID_DEFEATED;
    public static final ModConfigSpec.IntValue THREAT_RAID_WAITED;
    public static final ModConfigSpec.IntValue THREAT_DECAY_AMOUNT;
    public static final ModConfigSpec.IntValue THREAT_DECAY_TICKS;

    // Деревни
    public static final ModConfigSpec.BooleanValue VILLAGE_REPLACE_VANILLA;
    public static final ModConfigSpec.IntValue VILLAGE_NOTICE_RANGE;
    public static final ModConfigSpec.IntValue VILLAGE_NOTICE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue VILLAGE_HIDE_AT;
    public static final ModConfigSpec.IntValue VILLAGE_TRADE_REFUSE_AT;
    public static final ModConfigSpec.IntValue VILLAGE_ELDER_RAID_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue RUIN_WOOD_CHANCE;
    public static final ModConfigSpec.DoubleValue RUIN_MOSS_CHANCE;

    // Герои
    public static final ModConfigSpec.DoubleValue HERO_WARRIOR_HEALTH;
    public static final ModConfigSpec.DoubleValue HERO_ARCHER_HEALTH;
    public static final ModConfigSpec.DoubleValue HERO_TRACKER_HEALTH;
    public static final ModConfigSpec.DoubleValue HERO_COMMANDER_HEALTH;
    public static final ModConfigSpec.DoubleValue HERO_WARRIOR_DAMAGE;
    public static final ModConfigSpec.DoubleValue HERO_ARCHER_DAMAGE;
    public static final ModConfigSpec.DoubleValue HERO_TRACKER_DAMAGE;
    public static final ModConfigSpec.DoubleValue HERO_COMMANDER_DAMAGE;
    public static final ModConfigSpec.DoubleValue HERO_VETERAN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HERO_VETERAN_PER_TIER;
    public static final ModConfigSpec.IntValue HERO_SHIELD_ARC;
    public static final ModConfigSpec.IntValue HERO_BASH_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue HERO_BASH_DAMAGE;
    public static final ModConfigSpec.IntValue HERO_BASH_STUN_TICKS;
    public static final ModConfigSpec.DoubleValue HERO_ARCHER_KEEP_DISTANCE;
    public static final ModConfigSpec.IntValue HERO_VOLLEY_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue HERO_VOLLEY_SPREAD;
    public static final ModConfigSpec.IntValue HERO_MARK_TICKS;
    public static final ModConfigSpec.IntValue HERO_TRAP_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue HERO_TRAP_HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue HERO_TRAP_DAMAGE;
    public static final ModConfigSpec.DoubleValue HERO_AURA_RADIUS;
    public static final ModConfigSpec.IntValue HERO_SURROUND_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue HERO_LOST_TICKS;
    public static final ModConfigSpec.IntValue HERO_RETREAT_DESPAWN_TICKS;
    public static final ModConfigSpec.DoubleValue HERO_GEAR_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue HERO_EMBLEM_CHANCE;
    public static final ModConfigSpec.DoubleValue HERO_EMBLEM_CHANCE_VETERAN;
    public static final ModConfigSpec.DoubleValue HERO_ORDER_CHANCE;

    // Набеги
    public static final ModConfigSpec.IntValue RAID_CHECK_TICKS;
    public static final ModConfigSpec.IntValue RAID_COOLDOWN_MIN_TICKS;
    public static final ModConfigSpec.IntValue RAID_COOLDOWN_MAX_TICKS;
    public static final ModConfigSpec.DoubleValue RAID_CHANCE_SCOUTS;
    public static final ModConfigSpec.DoubleValue RAID_CHANCE_SQUADS;
    public static final ModConfigSpec.DoubleValue RAID_CHANCE_WARBANDS;
    public static final ModConfigSpec.IntValue RAID_HORN_LEAD_TICKS;
    public static final ModConfigSpec.IntValue RAID_WAIT_TICKS;
    public static final ModConfigSpec.IntValue RAID_ELDER_DELAY_RADIUS;

    // Резервуары, сплавщик, литейная
    public static final ModConfigSpec.IntValue RESERVOIR_CAPACITY_MB;
    public static final ModConfigSpec.IntValue SMELTER_POUR_MB_PER_TICK;
    public static final ModConfigSpec.IntValue ALLOY_MB;
    public static final ModConfigSpec.IntValue ALLOY_TICKS;
    public static final ModConfigSpec.IntValue ALLOY_ELECTRICITY_PER_TICK;
    public static final ModConfigSpec.IntValue CAST_NEEDLE_MB;
    public static final ModConfigSpec.IntValue CAST_NEEDLES_PER_CAST;
    public static final ModConfigSpec.IntValue CAST_PLATE_MB;
    public static final ModConfigSpec.IntValue CAST_TICKS;
    public static final ModConfigSpec.IntValue CAST_ELECTRICITY_PER_TICK;

    // Тяжёлая броня и гаусс-пушка
    public static final ModConfigSpec.DoubleValue HEAVY_SHARE_HEAD;
    public static final ModConfigSpec.DoubleValue HEAVY_SHARE_TORSO;
    public static final ModConfigSpec.DoubleValue HEAVY_SHARE_ARMS;
    public static final ModConfigSpec.DoubleValue HEAVY_SHARE_LEGS;
    public static final ModConfigSpec.DoubleValue HEAVY_PLATE_ABSORB;
    public static final ModConfigSpec.DoubleValue HEAVY_BASTION_PER_STEP;
    public static final ModConfigSpec.DoubleValue PLATE_IRON;
    public static final ModConfigSpec.DoubleValue PLATE_COPPER;
    public static final ModConfigSpec.DoubleValue PLATE_GOLD;
    public static final ModConfigSpec.DoubleValue PLATE_STEEL;
    public static final ModConfigSpec.DoubleValue PLATE_RED_GOLD;
    public static final ModConfigSpec.IntValue HEAVY_CHARGE_CAPACITY;
    public static final ModConfigSpec.IntValue HEAVY_SERVO_PER_SECOND;
    public static final ModConfigSpec.DoubleValue HEAVY_SLOW_POWERED;
    public static final ModConfigSpec.DoubleValue HEAVY_SLOW_UNPOWERED;
    public static final ModConfigSpec.IntValue ARTIFACT_BASE_CHARGE_PER_SECOND;
    public static final ModConfigSpec.IntValue GAUSS_CHARGE_CAPACITY;
    public static final ModConfigSpec.IntValue GAUSS_SHOT_COST;
    public static final ModConfigSpec.DoubleValue GAUSS_FIRE_RATE;
    public static final ModConfigSpec.DoubleValue GAUSS_TEMPO_PER_STEP;
    public static final ModConfigSpec.IntValue GAUSS_MAGAZINE;
    public static final ModConfigSpec.IntValue GAUSS_RELOAD_TICKS;
    public static final ModConfigSpec.DoubleValue GAUSS_HEAT_PER_SHOT;
    public static final ModConfigSpec.DoubleValue GAUSS_COOL_PER_SECOND;
    public static final ModConfigSpec.IntValue GAUSS_OVERHEAT_TICKS;
    public static final ModConfigSpec.DoubleValue GAUSS_SPREAD;
    public static final ModConfigSpec.DoubleValue GAUSS_RANGE;
    public static final ModConfigSpec.DoubleValue NEEDLE_DAMAGE_IRON;
    public static final ModConfigSpec.DoubleValue NEEDLE_DAMAGE_COPPER;
    public static final ModConfigSpec.DoubleValue NEEDLE_DAMAGE_GOLD;
    public static final ModConfigSpec.DoubleValue NEEDLE_DAMAGE_STEEL;
    public static final ModConfigSpec.DoubleValue NEEDLE_DAMAGE_RED_GOLD;
    public static final ModConfigSpec.ConfigValue<String> TABLE_HEAVY_PROTOTYPE;
    public static final ModConfigSpec.ConfigValue<String> TABLE_HEAVY_BASTION_1;
    public static final ModConfigSpec.ConfigValue<String> TABLE_GAUSS_PROTOTYPE;
    public static final ModConfigSpec.ConfigValue<String> TABLE_GAUSS_TEMPO_1;

    // Уровни узлов (вехи)
    public static final ModConfigSpec.IntValue TIER_NODES_TO_ADVANCE;
    public static final ModConfigSpec.BooleanValue TIER_LEVEL_FLOOR;
    public static final ModConfigSpec.ConfigValue<String> CLUSTERS_BY_LEVEL;
    public static final ModConfigSpec.ConfigValue<String> IMPROVED_MOBS_COMMAND;
    public static final ModConfigSpec.IntValue IMPROVED_MOBS_PER_TIER;

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

        b.comment("База, ключ и машины").push("base");
        BASE_ENERGY_CAPACITY = b.comment("Запас базы: электричество и мана, каждого (не больше 30000)")
                .defineInRange("energyCapacity", 20_000, 100, 30_000);
        KEY_COOLDOWN_TICKS = b.comment("Перезарядка ключа, тики")
                .defineInRange("keyCooldownTicks", 100, 0, 12_000);
        NODE_SHARDS_PER_LEVEL = b.comment("Кристаллов узла (топливо генератора) за зачищенный узел, на уровень узла")
                .defineInRange("nodeShardsPerLevel", 8, 0, 640);
        GENERATOR_BURN_TICKS = b.comment("Генератор: сколько тиков горит один кристалл")
                .defineInRange("generatorBurnTicks", 400, 1, 72_000);
        GENERATOR_ELECTRICITY_PER_TICK = b.defineInRange("generatorElectricityPerTick", 10, 0, 10_000);
        GENERATOR_MANA_PER_TICK = b.defineInRange("generatorManaPerTick", 10, 0, 10_000);
        CRUSHER_TICKS = b.comment("Переработка руды: тиков на один кусок")
                .defineInRange("crusherTicks", 100, 1, 12_000);
        CRUSHER_ELECTRICITY_PER_TICK = b.defineInRange("crusherElectricityPerTick", 5, 0, 10_000);
        SMELTER_TICKS = b.comment("Плавильня: тиков на одну пыль")
                .defineInRange("smelterTicks", 80, 1, 12_000);
        SMELTER_MANA_PER_TICK = b.defineInRange("smelterManaPerTick", 5, 0, 10_000);
        SMELTER_TANK_MB = b.comment("Плавильня: объём внутреннего бака, мВ")
                .defineInRange("smelterTankMb", 1000, 100, 30_000);
        SMELTER_MB_PER_DUST = b.defineInRange("smelterMbPerDust", 100, 1, 10_000);
        SMELTER_MB_PER_INGOT = b.comment("Плавильня: расплава на один слиток, мВ")
                .defineInRange("smelterMbPerIngot", 100, 1, 10_000);
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
        TRADER_FIXED_OFFERS = b.comment("Постоянный ассортимент: «товар*кол-во=цена» через «;», цена — в сгустках узла I")
                .define("fixedOffers", "minecraft:netherite_ingot*1=4");
        TRADER_OFFER_POOL = b.comment("Набор случайных позиций (формат тот же); на вызов выбирается randomOffersMin…Max")
                .define("offerPool", "minecraft:diamond*2=2;minecraft:emerald*6=1;minecraft:ender_pearl*4=2;minecraft:blaze_rod*4=2;"
                        + "minecraft:quartz*16=1;minecraft:glowstone_dust*16=1;minecraft:amethyst_shard*8=1;minecraft:experience_bottle*8=2");
        TRADER_MAX_USES = b.comment("Сколько раз можно купить позицию за вызов, максимум (случайно от 1)")
                .defineInRange("maxUses", 3, 1, 64);
        TRADER_ARRIVAL_TICKS = b.comment("Сколько тиков раскрывается разрыв до появления торговца")
                .defineInRange("arrivalTicks", 60, 0, 600);
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
        TABLE_KEY = b.comment("Ключ к базе")
                .define("key", "adept_core:crude_emitter*2;adept_core:crude_mana_circuit*2;minecraft:gold_ingot*2");
        TABLE_BLADE_PROTOTYPE = b.comment("Прототип энергоклинка")
                .define("bladePrototype", "adept_core:crude_emitter;adept_core:crude_coil;adept_core:crude_mana_circuit;adept_core:crude_wiring*2");
        TABLE_BLADE_EDGE_1 = b.comment("Клинок, эмиттер, путь «Кромка», ступень I")
                .define("bladeEdge1", "adept_core:crude_emitter*2;adept_core:crude_coil");
        b.pop();

        b.comment("Энергоклинок").push("blade");
        BLADE_BASE_DAMAGE = b.comment("Урон прототипа (как у меча: полный урон удара)")
                .defineInRange("baseDamage", 7.0, 1.0, 100.0);
        BLADE_ATTACK_SPEED = b.comment("Скорость атаки (ударов в секунду)")
                .defineInRange("attackSpeed", 1.8, 0.1, 10.0);
        BLADE_EDGE_DAMAGE_PER_STEP = b.comment("Прибавка урона за каждую ступень пути «Кромка»")
                .defineInRange("edgeDamagePerStep", 2.0, 0.0, 50.0);
        b.pop();

        b.comment("Новые противники на узле: споровик, кабан-вожак, броненосец").push("nodeMobs");
        WAVE_EXTRA_MIN_NODE_LEVEL = b.comment("С какого уровня узла они приходят в волнах")
                .defineInRange("minNodeLevel", 2, 1, 10);
        WAVE_SPORELINGS_FROM = b.defineInRange("sporelingsFromWave", 1, 1, 20);
        WAVE_SPORELINGS_COUNT = b.defineInRange("sporelingsCount", 2, 0, 30);
        WAVE_BOARS_FROM = b.defineInRange("boarsFromWave", 2, 1, 20);
        WAVE_BOARS_COUNT = b.defineInRange("boarsCount", 1, 0, 10);
        WAVE_ARMADILLOS_FROM = b.defineInRange("armadillosFromWave", 3, 1, 20);
        WAVE_ARMADILLOS_COUNT = b.defineInRange("armadillosCount", 1, 0, 10);
        BOAR_CHARGE_MIN_RANGE = b.comment("Кабан-вожак: разгоняется, если цель дальше стольких блоков")
                .defineInRange("boarChargeMinRange", 6, 1, 64);
        BOAR_CHARGE_MAX_RANGE = b.comment("и ближе стольких").defineInRange("boarChargeMaxRange", 16, 2, 64);
        BOAR_WINDUP_TICKS = b.comment("Подготовка к броску (роет землю), тики").defineInRange("boarWindupTicks", 20, 0, 200);
        BOAR_CHARGE_TICKS = b.comment("Наибольшая длина броска, тики").defineInRange("boarChargeTicks", 30, 1, 200);
        BOAR_CHARGE_SPEED = b.comment("Скорость броска, блоков за тик").defineInRange("boarChargeSpeed", 0.6, 0.1, 3.0);
        BOAR_RAM_MULTIPLIER = b.comment("Урон тарана = урон атаки × это число (отброса игрока нет)")
                .defineInRange("boarRamMultiplier", 1.5, 0.0, 10.0);
        BOAR_STUN_TICKS = b.comment("Оглушение после удара о стену, тики").defineInRange("boarStunTicks", 40, 0, 400);
        BOAR_CHARGE_COOLDOWN_TICKS = b.comment("Перезарядка броска, тики").defineInRange("boarChargeCooldownTicks", 160, 0, 2400);
        ARMADILLO_FRONT_ARC = b.comment("Броненосец: ширина передней дуги, где удары гасятся, градусы")
                .defineInRange("armadilloFrontArc", 120, 0, 360);
        ARMADILLO_CURLED_REDUCTION = b.comment("Доля урона, которую гасит свёрнутый панцирь (со всех сторон)")
                .defineInRange("armadilloCurledReduction", 0.75, 0.0, 1.0);
        ARMADILLO_CURL_TICKS = b.comment("Сколько держится свёрнутым, тики").defineInRange("armadilloCurlTicks", 60, 1, 600);
        ARMADILLO_CURL_COOLDOWN_TICKS = b.comment("Пауза перед следующим сворачиванием, тики")
                .defineInRange("armadilloCurlCooldownTicks", 120, 0, 2400);
        SPORELING_SPLIT_COUNT = b.comment("Споровик: сколько маленьких выпускает при смерти")
                .defineInRange("sporelingSplitCount", 2, 0, 8);
        b.pop();

        b.comment("Угроза мира 0–100 («Мобы», «Герои и набеги»)").push("threat");
        THREAT_SCOUTS_AT = b.comment("С этой угрозы — разведка (одиночные следопыты или пары)").defineInRange("scoutsAt", 25, 0, 100);
        THREAT_SQUADS_AT = b.comment("С этой — отряды из 3–5 героев").defineInRange("squadsAt", 50, 0, 100);
        THREAT_WARBANDS_AT = b.comment("С этой — крупные отряды с командиром").defineInRange("warbandsAt", 75, 0, 100);
        THREAT_BIOME_ABSORBED = b.comment("Рост за поглощённый биом (сильно)").defineInRange("biomeAbsorbed", 15, 0, 100);
        THREAT_VILLAGE_DESTROYED = b.comment("Рост за разрушенную деревню (заметно)").defineInRange("villageDestroyed", 8, 0, 100);
        THREAT_ELDER_KILLED = b.comment("Рост за убитого старейшину (заметно)").defineInRange("elderKilled", 10, 0, 100);
        THREAT_VILLAGER_KILLED = b.comment("Рост за убитого жителя (понемногу)").defineInRange("villagerKilled", 2, 0, 100);
        THREAT_RAID_DEFEATED = b.comment("Спад после разгромленного набега (заметно)").defineInRange("raidDefeated", 10, 0, 100);
        THREAT_RAID_WAITED = b.comment("Рост, если отряд прождал у точки выхода с базы и ушёл").defineInRange("raidWaited", 3, 0, 100);
        THREAT_DECAY_AMOUNT = b.comment("Спад со временем: на сколько").defineInRange("decayAmount", 1, 0, 100);
        THREAT_DECAY_TICKS = b.comment("и раз в сколько тиков (0 — без спада)").defineInRange("decayTicks", 6000, 0, 1_000_000);
        b.pop();

        b.comment("Деревни и руины («Мобы», «Жители деревень»)").push("villages");
        VILLAGE_REPLACE_VANILLA = b.comment("Заменять ванильных жителей своими").define("replaceVanilla", true);
        VILLAGE_NOTICE_RANGE = b.comment("С какого расстояния житель замечает игрока, блоки").defineInRange("noticeRange", 16, 2, 64);
        VILLAGE_NOTICE_COOLDOWN_TICKS = b.comment("Событие «NPC заметил игрока» от одного NPC не чаще, тики")
                .defineInRange("noticeCooldownTicks", 600, 20, 72_000);
        VILLAGE_HIDE_AT = b.comment("С этой угрозы жители при виде игрока прячутся по домам").defineInRange("hideAt", 50, 0, 101);
        VILLAGE_TRADE_REFUSE_AT = b.comment("С этой угрозы жители не торгуют").defineInRange("tradeRefuseAt", 50, 0, 101);
        VILLAGE_ELDER_RAID_DELAY_TICKS = b.comment("Насколько смерть старейшины откладывает набег в этой местности, тики")
                .defineInRange("elderRaidDelayTicks", 24_000, 0, 1_000_000);
        RUIN_WOOD_CHANCE = b.comment("Руины: доля деревянных блоков, которые выгорают").defineInRange("ruinWoodChance", 0.4, 0.0, 1.0);
        RUIN_MOSS_CHANCE = b.comment("Руины: доля камня, который зарастает мхом или трескается").defineInRange("ruinMossChance", 0.25, 0.0, 1.0);
        b.pop();

        b.comment("Герои («Мобы», «Герои и набеги»). Ветеран: здоровье и урон × veteranMultiplier, два приёма").push("heroes");
        HERO_WARRIOR_HEALTH = b.defineInRange("warriorHealth", 32.0, 1.0, 1000.0);
        HERO_ARCHER_HEALTH = b.defineInRange("archerHealth", 22.0, 1.0, 1000.0);
        HERO_TRACKER_HEALTH = b.defineInRange("trackerHealth", 24.0, 1.0, 1000.0);
        HERO_COMMANDER_HEALTH = b.defineInRange("commanderHealth", 50.0, 1.0, 1000.0);
        HERO_WARRIOR_DAMAGE = b.defineInRange("warriorDamage", 6.0, 0.0, 100.0);
        HERO_ARCHER_DAMAGE = b.comment("Урон стрелы лучника = половина этого числа × скорость").defineInRange("archerDamage", 4.0, 0.0, 100.0);
        HERO_TRACKER_DAMAGE = b.defineInRange("trackerDamage", 5.0, 0.0, 100.0);
        HERO_COMMANDER_DAMAGE = b.defineInRange("commanderDamage", 7.0, 0.0, 100.0);
        HERO_VETERAN_MULTIPLIER = b.defineInRange("veteranMultiplier", 1.4, 1.0, 10.0);
        HERO_VETERAN_PER_TIER = b.comment("Прибавка к шансу ветерана за каждый уровень узлов выше первого").defineInRange("veteranPerTier", 0.15, 0.0, 1.0);
        HERO_SHIELD_ARC = b.comment("Воин: ширина дуги щита спереди, градусы").defineInRange("shieldArc", 100, 0, 360);
        HERO_BASH_COOLDOWN_TICKS = b.comment("Воин-ветеран: таран щитом, перезарядка").defineInRange("bashCooldownTicks", 160, 1, 2400);
        HERO_BASH_DAMAGE = b.defineInRange("bashDamage", 4.0, 0.0, 100.0);
        HERO_BASH_STUN_TICKS = b.comment("Оглушение от тарана (вместо сбивания с ног — отброса игрока нет)").defineInRange("bashStunTicks", 30, 0, 200);
        HERO_ARCHER_KEEP_DISTANCE = b.comment("Лучник отходит, если игрок ближе").defineInRange("archerKeepDistance", 7.0, 0.0, 32.0);
        HERO_VOLLEY_COOLDOWN_TICKS = b.comment("Лучник-ветеран: залп из трёх стрел, перезарядка").defineInRange("volleyCooldownTicks", 120, 1, 2400);
        HERO_VOLLEY_SPREAD = b.comment("Разлёт стрел залпа, градусы").defineInRange("volleySpread", 8.0, 0.0, 45.0);
        HERO_MARK_TICKS = b.comment("Следопыт: метка (подсветка игрока), тики").defineInRange("markTicks", 200, 0, 2400);
        HERO_TRAP_COOLDOWN_TICKS = b.comment("Следопыт-ветеран: капкан, перезарядка").defineInRange("trapCooldownTicks", 300, 1, 6000);
        HERO_TRAP_HOLD_TICKS = b.comment("Капкан держит игрока, тики").defineInRange("trapHoldTicks", 20, 0, 200);
        HERO_TRAP_DAMAGE = b.defineInRange("trapDamage", 2.0, 0.0, 100.0);
        HERO_AURA_RADIUS = b.comment("Командир: радиус ауры строя (сопротивление урону союзникам)").defineInRange("auraRadius", 8.0, 1.0, 64.0);
        HERO_SURROUND_COOLDOWN_TICKS = b.comment("Командир-ветеран: приказ «окружить», перезарядка").defineInRange("surroundCooldownTicks", 200, 1, 6000);
        HERO_LOST_TICKS = b.comment("Событие «игрок скрылся» — не виден дольше, тики").defineInRange("lostTicks", 100, 1, 6000);
        HERO_RETREAT_DESPAWN_TICKS = b.comment("Отступивший герой пропадает вдали не раньше, тики").defineInRange("retreatDespawnTicks", 600, 0, 24_000);
        HERO_GEAR_DROP_CHANCE = b.comment("Шанс выпадения каждого предмета снаряжения героя").defineInRange("gearDropChance", 0.25, 0.0, 1.0);
        HERO_EMBLEM_CHANCE = b.comment("Эмблема (трофей для Альянса) с ученика").defineInRange("emblemChance", 0.3, 0.0, 1.0);
        HERO_EMBLEM_CHANCE_VETERAN = b.comment("и с ветерана").defineInRange("emblemChanceVeteran", 0.6, 0.0, 1.0);
        HERO_ORDER_CHANCE = b.comment("Приказ командира с командира (знамя выпадает всегда)").defineInRange("orderChance", 0.35, 0.0, 1.0);
        b.pop();

        b.comment("Набеги героев («Мобы», «Герои и набеги»)").push("raids");
        RAID_CHECK_TICKS = b.comment("Проверка набега в пути, раз в сколько тиков (и при уходе с узла)").defineInRange("checkTicks", 12_000, 200, 1_000_000);
        RAID_COOLDOWN_MIN_TICKS = b.comment("Не чаще одного набега за (от)").defineInRange("cooldownMinTicks", 18_000, 0, 1_000_000);
        RAID_COOLDOWN_MAX_TICKS = b.comment("(до), тики").defineInRange("cooldownMaxTicks", 24_000, 0, 1_000_000);
        RAID_CHANCE_SCOUTS = b.comment("Шанс набега при проверке: разведка").defineInRange("chanceScouts", 0.35, 0.0, 1.0);
        RAID_CHANCE_SQUADS = b.comment("отряды").defineInRange("chanceSquads", 0.5, 0.0, 1.0);
        RAID_CHANCE_WARBANDS = b.comment("крупные отряды").defineInRange("chanceWarbands", 0.65, 0.0, 1.0);
        RAID_HORN_LEAD_TICKS = b.comment("Рог звучит за столько тиков до появления отряда").defineInRange("hornLeadTicks", 1200, 0, 12_000);
        RAID_WAIT_TICKS = b.comment("Сколько отряд ждёт у точки выхода, если игрок на базе").defineInRange("waitTicks", 6000, 0, 72_000);
        RAID_ELDER_DELAY_RADIUS = b.comment("Смерть старейшины откладывает набеги в этом радиусе от деревни, блоки").defineInRange("elderDelayRadius", 256, 0, 10_000);
        b.pop();

        b.comment("Резервуары, сплавщик и литейная («База и машины»)").push("foundry");
        RESERVOIR_CAPACITY_MB = b.comment("Простой резервуар, первый уровень объёма, мВ").defineInRange("reservoirCapacityMb", 4000, 100, 1_000_000);
        SMELTER_POUR_MB_PER_TICK = b.comment("Плавильня сливает в соседний резервуар, мВ за тик").defineInRange("smelterPourMbPerTick", 50, 1, 10_000);
        ALLOY_MB = b.comment("Сплавщик: порция каждого металла, мВ (красное золото: золото + медь → вдвое больше; сталь: железо + уголь)")
                .defineInRange("alloyMb", 100, 1, 10_000);
        ALLOY_TICKS = b.defineInRange("alloyTicks", 100, 1, 12_000);
        ALLOY_ELECTRICITY_PER_TICK = b.defineInRange("alloyElectricityPerTick", 5, 0, 10_000);
        CAST_NEEDLE_MB = b.comment("Литейная: расплава на отливку игл").defineInRange("needleMb", 100, 1, 10_000);
        CAST_NEEDLES_PER_CAST = b.comment("Игл за отливку").defineInRange("needlesPerCast", 8, 1, 64);
        CAST_PLATE_MB = b.comment("Расплава на пластину тяжёлой брони").defineInRange("plateMb", 400, 1, 10_000);
        CAST_TICKS = b.defineInRange("castTicks", 60, 1, 12_000);
        CAST_ELECTRICITY_PER_TICK = b.defineInRange("castElectricityPerTick", 4, 0, 10_000);
        b.pop();

        b.comment("Тяжёлая силовая броня («Артефакты», «Прокачка: числа»)").push("heavyArmor");
        HEAVY_SHARE_HEAD = b.comment("Доли урона по пластинам: шлем").defineInRange("shareHead", 0.15, 0.0, 1.0);
        HEAVY_SHARE_TORSO = b.comment("нагрудник, корпус").defineInRange("shareTorso", 0.40, 0.0, 1.0);
        HEAVY_SHARE_ARMS = b.comment("нагрудник, руки").defineInRange("shareArms", 0.20, 0.0, 1.0);
        HEAVY_SHARE_LEGS = b.comment("ноги").defineInRange("shareLegs", 0.25, 0.0, 1.0);
        HEAVY_PLATE_ABSORB = b.comment("Сколько своей доли гасит целая пластина").defineInRange("plateAbsorb", 0.70, 0.0, 0.95);
        HEAVY_BASTION_PER_STEP = b.comment("«Бастион»: прибавка к гашению за ступень").defineInRange("bastionPerStep", 0.05, 0.0, 0.5);
        PLATE_IRON = b.comment("Прочность пластины по материалу: железо").defineInRange("plateIron", 40.0, 1.0, 10_000.0);
        PLATE_COPPER = b.defineInRange("plateCopper", 25.0, 1.0, 10_000.0);
        PLATE_GOLD = b.defineInRange("plateGold", 25.0, 1.0, 10_000.0);
        PLATE_STEEL = b.defineInRange("plateSteel", 60.0, 1.0, 10_000.0);
        PLATE_RED_GOLD = b.defineInRange("plateRedGold", 40.0, 1.0, 10_000.0);
        HEAVY_CHARGE_CAPACITY = b.comment("Запас электричества сервоприводов (в нагруднике)").defineInRange("chargeCapacity", 600, 1, 100_000);
        HEAVY_SERVO_PER_SECOND = b.comment("Расход при движении, в секунду").defineInRange("servoPerSecond", 5, 0, 1000);
        HEAVY_SLOW_POWERED = b.comment("Замедление с электричеством").defineInRange("slowPowered", 0.15, 0.0, 0.95);
        HEAVY_SLOW_UNPOWERED = b.comment("и без него").defineInRange("slowUnpowered", 0.50, 0.0, 0.95);
        ARTIFACT_BASE_CHARGE_PER_SECOND = b.comment("На базе броня и гаусс заряжаются от запаса базы, в секунду").defineInRange("baseChargePerSecond", 50, 0, 100_000);
        b.pop();

        b.comment("Гаусс-пушка («Артефакты», «Прокачка: числа»)").push("gauss");
        GAUSS_CHARGE_CAPACITY = b.defineInRange("chargeCapacity", 400, 1, 100_000);
        GAUSS_SHOT_COST = b.comment("Электричества на выстрел").defineInRange("shotCost", 4, 0, 1000);
        GAUSS_FIRE_RATE = b.comment("Выстрелов в секунду").defineInRange("fireRate", 4.0, 0.1, 40.0);
        GAUSS_TEMPO_PER_STEP = b.comment("«Темп»: прибавка скорострельности за ступень").defineInRange("tempoPerStep", 0.20, 0.0, 5.0);
        GAUSS_MAGAZINE = b.defineInRange("magazine", 30, 1, 1000);
        GAUSS_RELOAD_TICKS = b.defineInRange("reloadTicks", 40, 0, 1200);
        GAUSS_HEAT_PER_SHOT = b.defineInRange("heatPerShot", 0.05, 0.0, 1.0);
        GAUSS_COOL_PER_SECOND = b.defineInRange("coolPerSecond", 0.15, 0.0, 10.0);
        GAUSS_OVERHEAT_TICKS = b.comment("При 100% нагрева пушка молчит, тики").defineInRange("overheatTicks", 60, 0, 1200);
        GAUSS_SPREAD = b.comment("Разброс, градусы").defineInRange("spread", 2.0, 0.0, 45.0);
        GAUSS_RANGE = b.defineInRange("range", 48.0, 1.0, 256.0);
        NEEDLE_DAMAGE_IRON = b.comment("Урон иглы по материалу: железо (лёгкое отбрасывание — позже)").defineInRange("needleIron", 3.0, 0.0, 100.0);
        NEEDLE_DAMAGE_COPPER = b.comment("медь: +30% по целям без брони, −30% по бронированным").defineInRange("needleCopper", 2.5, 0.0, 100.0);
        NEEDLE_DAMAGE_GOLD = b.comment("золото").defineInRange("needleGold", 2.5, 0.0, 100.0);
        NEEDLE_DAMAGE_STEEL = b.comment("сталь: прошивает одну цель насквозь").defineInRange("needleSteel", 4.0, 0.0, 100.0);
        NEEDLE_DAMAGE_RED_GOLD = b.comment("красное золото: каждое следующее попадание по той же цели +15% (до 3)").defineInRange("needleRedGold", 3.0, 0.0, 100.0);
        b.pop();

        b.comment("Стол: тяжёлая броня и гаусс-пушка").push("tableSlice2");
        TABLE_HEAVY_PROTOTYPE = b.comment("Прототип тяжёлой брони (шлем, нагрудник, ноги без пластин)")
                .define("heavyPrototype", "minecraft:iron_ingot*12;adept_core:crude_wiring*2;adept_core:crude_coil*2");
        TABLE_HEAVY_BASTION_1 = b.comment("Тяжёлая броня, нагрудник, путь «Бастион», ступень I")
                .define("heavyBastion1", "adept_core:crude_coil*2;adept_core:crude_wiring*2;minecraft:iron_ingot*4");
        TABLE_GAUSS_PROTOTYPE = b.comment("Прототип гаусс-пушки")
                .define("gaussPrototype", "adept_core:crude_coil*3;adept_core:crude_emitter;adept_core:crude_wiring*2;minecraft:iron_ingot*4");
        TABLE_GAUSS_TEMPO_1 = b.comment("Гаусс-пушка, ствол, путь «Темп», ступень I")
                .define("gaussTempo1", "adept_core:crude_coil*2;adept_core:crude_wiring");
        b.pop();

        b.comment("Уровни узлов: вехи и рост сложности").push("tiers");
        TIER_NODES_TO_ADVANCE = b.comment("Сколько узлов текущего уровня (или выше) зачистить, чтобы мир перешёл на следующий уровень")
                .defineInRange("nodesToAdvance", 5, 1, 100);
        TIER_LEVEL_FLOOR = b.comment("Новые узлы (в ещё не открытых чанках) не ниже текущего уровня узлов мира")
                .define("levelFloor", true);
        CLUSTERS_BY_LEVEL = b.comment("Сгустков за зачищенный узел по уровням 1–5, через запятую")
                .define("clustersByLevel", "1,1,2,3,4");
        IMPROVED_MOBS_COMMAND = b.comment("Команда Improved Mobs при смене уровня узлов (%d — сложность). Пусто — не вызывать.",
                        "Выполняется, только если Improved Mobs загружен")
                .define("improvedMobsCommand", "improvedmobs difficulty set %d");
        IMPROVED_MOBS_PER_TIER = b.comment("Сложность Improved Mobs = (уровень узлов − 1) × это число")
                .defineInRange("improvedMobsPerTier", 50, 0, 10_000);
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

    /** Сколько сгустков даёт зачищенный узел данного уровня. */
    public static int clustersFor(int level) {
        String[] parts = CLUSTERS_BY_LEVEL.get().split(",");
        int index = Math.max(1, level) - 1;
        String raw = parts.length == 0 ? "1" : parts[Math.min(index, parts.length - 1)].trim();
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Число волн на узле данного уровня. */
    public static int waveCount(int level) {
        return Math.min(WAVES_BASE.get() + level, WAVES_MAX.get());
    }
}
