package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Числа среза 3 «мана и руны»: накопитель, котёл душ, души и ядра душ, команды при создании мира.
 * Лежат в том же файле {@code config/adept_core-common.toml}, что и {@link BalanceConfig}:
 * разделы определяются из его статического блока, отдельный класс — чтобы не раздувать BalanceConfig.
 */
public final class ManaConfig {
    // Накопитель («Артефакты», «Прокачка: числа», «Накопитель»)
    public static ModConfigSpec.IntValue ACCUMULATOR_CAPACITY;
    public static ModConfigSpec.ConfigValue<String> ACCUMULATOR_CAPACITY_STEPS;
    public static ModConfigSpec.IntValue ACCUMULATOR_INTAKE;
    public static ModConfigSpec.IntValue ACCUMULATOR_OUTPUT;

    // Котёл душ
    public static ModConfigSpec.IntValue SOUL_MANA_NORMAL;
    public static ModConfigSpec.IntValue SOUL_MANA_STRONG;
    public static ModConfigSpec.IntValue SOUL_MANA_GUARDIAN;
    public static ModConfigSpec.DoubleValue SOUL_STRONG_HEALTH;
    public static ModConfigSpec.ConfigValue<List<? extends String>> SOUL_GUARDIANS;
    public static ModConfigSpec.IntValue SOUL_BURN_TICKS;
    public static ModConfigSpec.IntValue SOUL_LIFETIME_TICKS;
    public static ModConfigSpec.DoubleValue SOUL_PICKUP_RADIUS;
    public static ModConfigSpec.DoubleValue SOUL_MERGE_RADIUS;
    public static ModConfigSpec.DoubleValue SOUL_CORE_CHANCE_NORMAL;
    public static ModConfigSpec.DoubleValue SOUL_CORE_CHANCE_STRONG;
    public static ModConfigSpec.DoubleValue SOUL_CORE_CHANCE_GUARDIAN;
    public static ModConfigSpec.IntValue CAULDRON_FURNACE_SOULS;
    public static ModConfigSpec.IntValue CAULDRON_BUFFER;
    public static ModConfigSpec.IntValue CAULDRON_TRANSFER;
    public static ModConfigSpec.DoubleValue CAULDRON_TRANSFER_LOSS;
    public static ModConfigSpec.DoubleValue CAULDRON_YIELD_PER_STEP;
    public static ModConfigSpec.IntValue VESSEL_GLASS;
    public static ModConfigSpec.IntValue VESSEL_QUARTZ;

    // Ядра душ у торговца
    public static ModConfigSpec.IntValue SOUL_CORES_PER_CLUSTER;

    // Стол: накопитель и котёл
    public static ModConfigSpec.ConfigValue<String> TABLE_ACCUMULATOR;
    public static ModConfigSpec.ConfigValue<String> TABLE_ACCUMULATOR_CAPACITY_1;
    public static ModConfigSpec.ConfigValue<String> TABLE_CAULDRON;
    public static ModConfigSpec.ConfigValue<String> TABLE_CAULDRON_YIELD_1;

    // Мир
    public static ModConfigSpec.ConfigValue<List<? extends String>> WORLD_CREATE_COMMANDS;

    private ManaConfig() {
    }

    static void define(ModConfigSpec.Builder b) {
        b.comment("Накопитель («Артефакты», «Накопитель»): два раздельных запаса, мана и электричество").push("accumulator");
        ACCUMULATOR_CAPACITY = b.comment("Каждый запас без прокачки").defineInRange("capacity", 1000, 1, 1_000_000);
        ACCUMULATOR_CAPACITY_STEPS = b.comment("«Ёмкость» I–III: каждый запас по ступеням, через запятую")
                .define("capacitySteps", "2000,3000,4350");
        ACCUMULATOR_INTAKE = b.comment("Приём на запас в секунду (от запаса базы на базе, от котла)")
                .defineInRange("intakePerSecond", 20, 0, 100_000);
        ACCUMULATOR_OUTPUT = b.comment("Отдача одному артефакту в секунду, без потерь")
                .defineInRange("outputPerSecond", 10, 0, 100_000);
        b.pop();

        b.comment("Котёл душ («Артефакты», «Котёл душ»)").push("soulCauldron");
        SOUL_MANA_NORMAL = b.comment("Мана с обычной души").defineInRange("manaNormal", 10, 0, 100_000);
        SOUL_MANA_STRONG = b.comment("Мана с сильной души (герои, элита или моб от strongHealth здоровья)")
                .defineInRange("manaStrong", 40, 0, 100_000);
        SOUL_MANA_GUARDIAN = b.comment("Мана с души стража узла").defineInRange("manaGuardian", 200, 0, 100_000);
        SOUL_STRONG_HEALTH = b.comment("С какого запаса здоровья моб даёт сильную душу")
                .defineInRange("strongHealth", 40.0, 1.0, 100_000.0);
        SOUL_GUARDIANS = b.comment("Мобы, дающие душу стража (id сущностей)")
                .defineListAllowEmpty("guardians", List.of("adept_core:horned_leader"), () -> "", o -> o instanceof String);
        SOUL_BURN_TICKS = b.comment("Горнило сжигает одну душу за столько тиков (40 = 2 с)")
                .defineInRange("burnTicks", 40, 1, 72_000);
        SOUL_LIFETIME_TICKS = b.comment("Душа на земле исчезает через столько тиков (1200 = 60 с)")
                .defineInRange("soulLifetimeTicks", 1200, 20, 72_000);
        SOUL_PICKUP_RADIUS = b.comment("Радиус сбора душ котлом, блоков").defineInRange("pickupRadius", 4.0, 0.5, 64.0);
        SOUL_MERGE_RADIUS = b.comment("Души одной силы ближе этого расстояния сливаются в одну")
                .defineInRange("mergeRadius", 1.5, 0.0, 16.0);
        SOUL_CORE_CHANCE_NORMAL = b.comment("Шанс ядра души при сжигании обычной души").defineInRange("coreChanceNormal", 0.02, 0.0, 1.0);
        SOUL_CORE_CHANCE_STRONG = b.comment("сильной").defineInRange("coreChanceStrong", 0.15, 0.0, 1.0);
        SOUL_CORE_CHANCE_GUARDIAN = b.comment("стража").defineInRange("coreChanceGuardian", 1.0, 0.0, 1.0);
        CAULDRON_FURNACE_SOULS = b.comment("Сколько душ ждёт в горниле; лишние уходят в сосуд")
                .defineInRange("furnaceSouls", 4, 1, 1000);
        CAULDRON_BUFFER = b.comment("Запас маны в самом котле до передачи в накопитель")
                .defineInRange("manaBuffer", 400, 1, 100_000);
        CAULDRON_TRANSFER = b.comment("Отдача маны в накопитель в секунду").defineInRange("transferPerSecond", 10, 0, 100_000);
        CAULDRON_TRANSFER_LOSS = b.comment("Потери при передаче (0,2 = 20%)").defineInRange("transferLoss", 0.2, 0.0, 1.0);
        CAULDRON_YIELD_PER_STEP = b.comment("«Выход»: прибавка маны с души за ступень (0,15 = +15%)")
                .defineInRange("yieldPerStep", 0.15, 0.0, 10.0);
        VESSEL_GLASS = b.comment("Объём сосуда в душах: стекло").defineInRange("vesselGlass", 16, 1, 10_000);
        VESSEL_QUARTZ = b.comment("кварц").defineInRange("vesselQuartz", 32, 1, 10_000);
        b.pop();

        b.comment("Ядра душ — валюта торговца наравне со сгустками").push("soulCores");
        SOUL_CORES_PER_CLUSTER = b.comment("Сколько ядер душ стоит один сгусток узла I в ценах торговца")
                .defineInRange("coresPerCluster", 6, 1, 64);
        b.pop();

        b.comment("Стол: накопитель и котёл душ. Ступени — по «Компонентам» («Материалы и расходники»):",
                        "ранг по ступени, доля подпорки I 60% / II 40% / III 20%, пути 3:1 и 1:3 по семействам сегмента")
                .push("tableSlice3");
        TABLE_ACCUMULATOR = b.comment("Накопитель (базовая версия, грубые компоненты)")
                .define("accumulator", "adept_core:crude_coil*2;adept_core:crude_mana_circuit*2;minecraft:glass*4;minecraft:copper_ingot*4");
        TABLE_ACCUMULATOR_CAPACITY_1 = b.comment("Накопитель, ядро (конденсатор + решётка), путь «Ёмкость», ступень I.",
                        "Гибрид: подпорки поровну — 2 магических и 2 технических")
                .define("accumulatorCapacity1", "adept_core:capacitor_1_tech*2;adept_core:capacitor_1_magic;adept_core:matrix_1");
        TABLE_CAULDRON = b.comment("Котёл душ (базовая версия)")
                .define("cauldron", "adept_core:crude_mana_circuit*2;adept_core:crude_wiring*2;minecraft:iron_ingot*6;minecraft:soul_sand*4");
        TABLE_CAULDRON_YIELD_1 = b.comment("Котёл душ, горнило (решётка вместо колбы душ + радиатор), путь «Выход», ступень I.",
                        "Магический сегмент: подпорка — техника, её закрывает радиатор")
                .define("cauldronYield1", "adept_core:matrix_1*3;adept_core:radiator_1");
        b.pop();

        b.comment("Команды при создании мира (один раз на мир). Формат «мод|команда»: команда выполняется,",
                        "только если мод загружен; без «мод|» — всегда. Born in Chaos: выключить хэллоуинские тыквы").push("worldInit");
        WORLD_CREATE_COMMANDS = b.defineListAllowEmpty("createCommands",
                List.of("born_in_chaos_v1|gamerule seasonalEvents false"), () -> "", o -> o instanceof String);
        b.pop();
    }

    /** Ёмкость запаса накопителя на ступени «Ёмкости» 0–3. */
    public static int accumulatorCapacity(int step) {
        if (step <= 0) {
            return ACCUMULATOR_CAPACITY.get();
        }
        String[] parts = ACCUMULATOR_CAPACITY_STEPS.get().split(",");
        String raw = parts[Math.min(step, parts.length) - 1].trim();
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return ACCUMULATOR_CAPACITY.get();
        }
    }
}
