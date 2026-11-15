package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Числа среза 3, часть 2: ограночная машина, фокусы (огранка, износ, трещина, поломка, починка)
 * и лёгкий экзокостюм (щит из маны, фокус нагрудника, «Ёмкость»). Файл тот же —
 * {@code config/adept_core-common.toml}; разделы определяются из статического блока {@link BalanceConfig}.
 */
public final class FocusConfig {
    // Ограночная машина (нулевой уровень)
    public static ModConfigSpec.IntValue CUTTER_ELECTRICITY;
    public static ModConfigSpec.IntValue CUTTER_CUT_TICKS;
    public static ModConfigSpec.IntValue CUTTER_CRYSTALS;
    public static ModConfigSpec.IntValue CUTTER_REPAIR_TICKS;
    public static ModConfigSpec.IntValue CUTTER_REPAIR_MANA;
    public static ModConfigSpec.DoubleValue CUTTER_REPAIR_RESTORE;

    // Фокусы
    public static ModConfigSpec.ConfigValue<List<? extends String>> FOCUS_CRYSTALS;
    public static ModConfigSpec.DoubleValue FOCUS_CRACK_CHANCE;
    public static ModConfigSpec.DoubleValue FOCUS_LUCK_CHANCE;
    public static ModConfigSpec.IntValue FOCUS_GROWTH_SECONDS;
    public static ModConfigSpec.DoubleValue FOCUS_ECONOMY;

    // Ответ щита (фокус нагрудника лёгкой брони)
    public static ModConfigSpec.IntValue RESPONSE_COOLDOWN;
    public static ModConfigSpec.IntValue RESPONSE_MANA;
    public static ModConfigSpec.DoubleValue RESPONSE_RADIUS;
    public static ModConfigSpec.DoubleValue RESPONSE_KNOCKBACK;
    public static ModConfigSpec.IntValue RESPONSE_FLASH_TICKS;
    public static ModConfigSpec.DoubleValue RESPONSE_RECHARGE;
    public static ModConfigSpec.IntValue RESPONSE_SLOW_TICKS;
    public static ModConfigSpec.IntValue RESPONSE_SLOW_LEVEL;

    // Лёгкий экзокостюм
    public static ModConfigSpec.IntValue LIGHT_SHIELD;
    public static ModConfigSpec.ConfigValue<String> LIGHT_SHIELD_STEPS;
    public static ModConfigSpec.IntValue LIGHT_REGEN_DELAY;
    public static ModConfigSpec.DoubleValue LIGHT_REGEN_PER_SECOND;
    public static ModConfigSpec.IntValue LIGHT_MANA_PER_UNIT;
    public static ModConfigSpec.IntValue LIGHT_MANA_CAPACITY;

    // Стол: лёгкий экзокостюм
    public static ModConfigSpec.ConfigValue<String> TABLE_LIGHT_PROTOTYPE;
    public static ModConfigSpec.ConfigValue<String> TABLE_LIGHT_CAPACITY_1;

    private FocusConfig() {
    }

    static void define(ModConfigSpec.Builder b) {
        b.comment("Ограночная машина (нулевой уровень): огранка кристаллов в фокусы и починка фокусов").push("cutter");
        CUTTER_ELECTRICITY = b.comment("Электричество базы за тик работы").defineInRange("electricityPerTick", 2, 0, 1000);
        CUTTER_CUT_TICKS = b.comment("Огранка одного фокуса, тики").defineInRange("cutTicks", 100, 1, 72000);
        CUTTER_CRYSTALS = b.comment("Кристаллов на один фокус").defineInRange("crystalsPerFocus", 2, 1, 64);
        CUTTER_REPAIR_TICKS = b.comment("Починка фокуса (изношенного или треснувшего), тики").defineInRange("repairTicks", 60, 1, 72000);
        CUTTER_REPAIR_MANA = b.comment("Мана базы за починку одного фокуса").defineInRange("repairMana", 40, 0, 100000);
        CUTTER_REPAIR_RESTORE = b.comment("Доля полной прочности, которую возвращает починка (полностью — путь «Реставрация», позже); трещина снимается всегда")
                .defineInRange("repairRestore", 0.5, 0.0, 1.0);
        b.pop();

        b.comment("Фокусы: огранка задаёт тип, кристалл — силу, прочность и модификатор («Материалы», «Фокусы»)").push("focus");
        FOCUS_CRYSTALS = b.comment("Кристаллы фокусов: «предмет|id|сила|прочность». Прочность — срабатываний до износа.",
                        "Модификаторы по id: quartz, diamond — нет; lapis — «Экономия»; amethyst — «Рост»; emerald — «Удача»;",
                        "node — «Созвучие» (резонансы, позже)")
                .defineListAllowEmpty("crystals", List.of(
                        "minecraft:quartz|quartz|0.8|50",
                        "minecraft:lapis_lazuli|lapis|0.9|20",
                        "minecraft:amethyst_shard|amethyst|1.0|35",
                        "minecraft:emerald|emerald|1.1|35",
                        "minecraft:diamond|diamond|1.3|80",
                        "adept_core:node_shard|node|1.5|35"), () -> "", o -> o instanceof String s && s.split("\\|").length == 4);
        FOCUS_CRACK_CHANCE = b.comment("Шанс, что изношенный фокус треснет (гаснет, чинится), иначе ломается и пропадает")
                .defineInRange("crackChance", 0.5, 0.0, 1.0);
        FOCUS_LUCK_CHANCE = b.comment("«Удача» (изумруд): шанс сработать без износа").defineInRange("luckChance", 0.25, 0.0, 1.0);
        FOCUS_GROWTH_SECONDS = b.comment("«Рост» (аметист): секунд на восстановление одной единицы прочности").defineInRange("growthSeconds", 60, 1, 86400);
        FOCUS_ECONOMY = b.comment("«Экономия» (лазурит): доля маны за срабатывание").defineInRange("economy", 0.5, 0.0, 1.0);
        b.pop();

        b.comment("Ответ щита — фокус нагрудника лёгкой брони. Срабатывает, когда щит принял удар.",
                "Сила кристалла умножает силу отталкивания, длительность эффектов и число единиц перезаряда").push("shieldResponse");
        RESPONSE_COOLDOWN = b.comment("Не чаще раза в столько тиков").defineInRange("cooldownTicks", 40, 0, 12000);
        RESPONSE_MANA = b.comment("Маны костюма за срабатывание").defineInRange("mana", 10, 0, 10000);
        RESPONSE_RADIUS = b.comment("Радиус отталкивания, вспышки и замедления, блоки").defineInRange("radius", 4.0, 0.5, 32.0);
        RESPONSE_KNOCKBACK = b.comment("Отталкивание: сила").defineInRange("knockback", 1.2, 0.0, 10.0);
        RESPONSE_FLASH_TICKS = b.comment("Ослепляющая вспышка: слепота, тики").defineInRange("flashTicks", 60, 0, 12000);
        RESPONSE_RECHARGE = b.comment("Быстрый перезаряд: единиц щита сразу").defineInRange("rechargeUnits", 4.0, 0.0, 1000.0);
        RESPONSE_SLOW_TICKS = b.comment("Замедление вокруг: длительность, тики").defineInRange("slowTicks", 60, 0, 12000);
        RESPONSE_SLOW_LEVEL = b.comment("Замедление вокруг: уровень эффекта (0 — I)").defineInRange("slowLevel", 1, 0, 9);
        b.pop();

        b.comment("Лёгкий экзокостюм: щит из маны принимает урон раньше здоровья; без маны щита нет, броня 6").push("lightArmor");
        LIGHT_SHIELD = b.comment("Щит без ступеней, единиц (1 единица гасит 1 урон)").defineInRange("shield", 10, 0, 10000);
        LIGHT_SHIELD_STEPS = b.comment("Щит по ступеням «Ёмкости» I–III, через запятую").define("shieldSteps", "15,20,25");
        LIGHT_REGEN_DELAY = b.comment("Восстановление щита начинается через столько тиков без урона").defineInRange("regenDelayTicks", 80, 0, 12000);
        LIGHT_REGEN_PER_SECOND = b.comment("Восстановление, единиц в секунду").defineInRange("regenPerSecond", 2.0, 0.0, 1000.0);
        LIGHT_MANA_PER_UNIT = b.comment("Маны костюма на единицу щита").defineInRange("manaPerUnit", 5, 0, 1000);
        LIGHT_MANA_CAPACITY = b.comment("Запас маны в костюме (подпитка — от накопителя)").defineInRange("manaCapacity", 500, 0, 100000);
        b.pop();

        b.comment("Стол: лёгкий экзокостюм. Формат «id*кол-во;…»").push("tableLight");
        TABLE_LIGHT_PROTOTYPE = b.comment("Прототип лёгкого экзокостюма (нагрудник — генератор щита)")
                .define("lightPrototype", "adept_core:crude_mana_circuit*3;adept_core:crude_emitter*2;minecraft:iron_ingot*6;minecraft:amethyst_shard*4");
        TABLE_LIGHT_CAPACITY_1 = b.comment("Лёгкий экзокостюм, нагрудник, путь «Ёмкость», ступень I.",
                        "Семейства нагрудника — эмиттер и контур маны; магический сегмент, подпорка — технические эмиттеры (60%)")
                .define("lightCapacity1", "adept_core:emitter_1_tech*3;adept_core:emitter_1_magic*2;adept_core:mana_circuit_1*2;minecraft:amethyst_shard*2");
        b.pop();
    }

    /** Щит костюма при {@code step} открытых ступенях «Ёмкости» (0 — без ступеней). */
    public static int shieldCapacity(int step) {
        if (step <= 0) {
            return LIGHT_SHIELD.get();
        }
        String[] parts = LIGHT_SHIELD_STEPS.get().split(",");
        try {
            return Integer.parseInt(parts[Math.min(step, parts.length) - 1].trim());
        } catch (NumberFormatException e) {
            return LIGHT_SHIELD.get();
        }
    }
}
