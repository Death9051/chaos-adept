package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Числа среза 3, часть 3: высекатель рун, руны и слова (снаряды и обереги брони), рельса и болты.
 * Файл тот же — {@code config/adept_core-common.toml}; разделы определяются из статического блока {@link BalanceConfig}.
 */
public final class RuneConfig {
    // Высекатель рун
    public static ModConfigSpec.IntValue CARVER_ELECTRICITY;
    public static ModConfigSpec.IntValue CARVER_TICKS;
    public static ModConfigSpec.IntValue CARVER_MANA;
    public static ModConfigSpec.IntValue CARVER_NEEDLES_PER_MATERIAL;

    // Материалы гравировки
    public static ModConfigSpec.ConfigValue<List<? extends String>> RUNE_MATERIALS;
    public static ModConfigSpec.DoubleValue MOD_CHAIN_CHANCE;
    public static ModConfigSpec.IntValue MOD_ECHO_MANA;
    public static ModConfigSpec.DoubleValue MOD_RESONANCE;
    public static ModConfigSpec.DoubleValue MOD_DURATION;
    public static ModConfigSpec.DoubleValue MOD_ZONE;
    public static ModConfigSpec.DoubleValue MOD_LUCK;

    // Руны на снарядах
    public static ModConfigSpec.DoubleValue RUNE_ARC_DAMAGE;
    public static ModConfigSpec.DoubleValue RUNE_RADIUS;
    public static ModConfigSpec.IntValue RUNE_FIRE_SECONDS;
    public static ModConfigSpec.IntValue RUNE_LINK_SECONDS;
    public static ModConfigSpec.DoubleValue RUNE_LINK_SHARE;
    public static ModConfigSpec.IntValue RUNE_ECHO_DELAY;
    public static ModConfigSpec.DoubleValue RUNE_ECHO_SHARE;
    public static ModConfigSpec.IntValue RUNE_ZONE_SECONDS;
    public static ModConfigSpec.DoubleValue RUNE_ZONE_RADIUS;
    public static ModConfigSpec.DoubleValue RUNE_ZONE_DAMAGE;
    public static ModConfigSpec.DoubleValue RUNE_PULL;
    public static ModConfigSpec.IntValue RUNE_FROST_SECONDS;
    public static ModConfigSpec.DoubleValue RUNE_SHATTER_DAMAGE;
    public static ModConfigSpec.IntValue RUNE_VOID_SECONDS;
    public static ModConfigSpec.DoubleValue RUNE_VOID_ARMOR;
    public static ModConfigSpec.DoubleValue RUNE_FACET_SHARE;
    public static ModConfigSpec.DoubleValue WORD_DAMAGE;
    public static ModConfigSpec.IntValue WORD_SECONDS;
    public static ModConfigSpec.DoubleValue WORD_RADIUS;

    // Обереги брони
    public static ModConfigSpec.IntValue WARD_STUN_TICKS;
    public static ModConfigSpec.DoubleValue WARD_SHARE;
    public static ModConfigSpec.DoubleValue WARD_ECHO_RESTORE;
    public static ModConfigSpec.DoubleValue WARD_REDUCE;
    public static ModConfigSpec.IntValue WARD_SLOW_TICKS;
    public static ModConfigSpec.IntValue WARD_COOLDOWN;
    public static ModConfigSpec.IntValue WARD_RARE_COOLDOWN;
    public static ModConfigSpec.DoubleValue WARD_STRONG_HIT;
    public static ModConfigSpec.DoubleValue WARD_ROOT_REDUCE;
    public static ModConfigSpec.DoubleValue WARD_DISTORT_CHANCE;

    // Рельса
    public static ModConfigSpec.IntValue RAIL_CAPACITY;
    public static ModConfigSpec.IntValue RAIL_SHOT_COST;
    public static ModConfigSpec.IntValue RAIL_CHARGE_TICKS;
    public static ModConfigSpec.DoubleValue RAIL_RANGE;
    public static ModConfigSpec.IntValue RAIL_PIERCE;
    public static ModConfigSpec.DoubleValue RAIL_FALLOFF;
    public static ModConfigSpec.ConfigValue<List<? extends String>> BOLTS;
    public static ModConfigSpec.DoubleValue PIERCE_DAMAGE;
    public static ModConfigSpec.IntValue PIERCE_TARGETS;
    public static ModConfigSpec.DoubleValue PIERCE_ARMOR;
    public static ModConfigSpec.IntValue CAST_BOLT_MB;

    // Стол
    public static ModConfigSpec.ConfigValue<String> TABLE_RAIL_PROTOTYPE;
    public static ModConfigSpec.ConfigValue<String> TABLE_RAIL_PIERCE_1;
    public static ModConfigSpec.ConfigValue<String> TABLE_CARVER_PRECISION_1;

    private RuneConfig() {
    }

    static void define(ModConfigSpec.Builder b) {
        b.comment("Высекатель рун (нулевой уровень): гравировка литых изделий одним материалом").push("carver");
        CARVER_ELECTRICITY = b.comment("Электричество базы за тик работы").defineInRange("electricityPerTick", 3, 0, 1000);
        CARVER_TICKS = b.comment("Гравировка одной партии, тики").defineInRange("engraveTicks", 80, 1, 72000);
        CARVER_MANA = b.comment("Мана базы на одно изделие (руна запитывается)").defineInRange("manaPerItem", 2, 0, 10000);
        CARVER_NEEDLES_PER_MATERIAL = b.comment("Игл на единицу материала гравировки (болт и пластина — по единице)")
                .defineInRange("needlesPerMaterial", 8, 1, 64);
        b.pop();

        b.comment("Материалы гравировки: «id|предмет|множитель|шанс не стереться». Модификаторы по id:",
                "copper — «Цепь», gold — «Отклик», red_gold — «Созвучие», netherite — «Неугасимость», diamond — «Ясность»,",
                "emerald — «Удача», lapis — «Экономия», redstone — «Мгновенность»; iron, steel — без модификатора").push("runeMaterials");
        RUNE_MATERIALS = b.defineListAllowEmpty("materials", List.of(
                "copper|minecraft:copper_ingot|0.8|0.4",
                "iron|minecraft:iron_ingot|1.0|0.5",
                "steel|adept_core:steel_ingot|1.1|0.75",
                "gold|minecraft:gold_ingot|1.2|0.3",
                "red_gold|adept_core:red_gold_ingot|1.3|0.5",
                "netherite|minecraft:netherite_ingot|1.5|0.9",
                "diamond|minecraft:diamond|1.4|0.85",
                "emerald|minecraft:emerald|1.1|0.6",
                "lapis|minecraft:lapis_lazuli|0.9|0.4",
                "redstone|minecraft:redstone|0.9|0.4"), () -> "", o -> o instanceof String s && s.split("\\|").length == 4);
        MOD_CHAIN_CHANCE = b.comment("«Цепь»: шанс, что эффект перекинется на ближайшую цель").defineInRange("chainChance", 0.3, 0.0, 1.0);
        MOD_ECHO_MANA = b.comment("«Отклик»: маны в накопитель за срабатывание").defineInRange("echoMana", 2, 0, 1000);
        MOD_RESONANCE = b.comment("«Созвучие»: прибавка к множителю, если обе руны одинаковые").defineInRange("resonance", 0.3, 0.0, 10.0);
        MOD_DURATION = b.comment("«Неугасимость»: длительные эффекты дольше на эту долю").defineInRange("duration", 0.5, 0.0, 10.0);
        MOD_ZONE = b.comment("«Ясность»: зоны рун больше и дольше на эту долю").defineInRange("zone", 0.3, 0.0, 10.0);
        MOD_LUCK = b.comment("«Удача»: шанс, что слово сработает дважды").defineInRange("luck", 0.15, 0.0, 1.0);
        b.pop();

        b.comment("Руны на иглах и болтах: основа (первая руна), грань (вторая), особые слова из двух рун.",
                "Урон и длительности умножаются на множитель материала гравировки").push("runes");
        RUNE_ARC_DAMAGE = b.comment("Ток: урон разряда по каждой из двух соседних целей").defineInRange("arcDamage", 3.0, 0.0, 1000.0);
        RUNE_RADIUS = b.comment("Радиус поиска соседних целей, блоки").defineInRange("radius", 4.0, 0.5, 32.0);
        RUNE_FIRE_SECONDS = b.comment("Пламя: поджог, секунды").defineInRange("fireSeconds", 4, 0, 600);
        RUNE_LINK_SECONDS = b.comment("Узы: связь, секунды").defineInRange("linkSeconds", 5, 0, 600);
        RUNE_LINK_SHARE = b.comment("Узы: доля урона, которая дублируется на связанного врага").defineInRange("linkShare", 0.5, 0.0, 10.0);
        RUNE_ECHO_DELAY = b.comment("Эхо: задержка повтора, тики").defineInRange("echoDelay", 20, 0, 1200);
        RUNE_ECHO_SHARE = b.comment("Эхо: доля урона повтора").defineInRange("echoShare", 0.5, 0.0, 10.0);
        RUNE_ZONE_SECONDS = b.comment("Разлом и зоны слов: длительность, секунды").defineInRange("zoneSeconds", 3, 0, 600);
        RUNE_ZONE_RADIUS = b.comment("Разлом: радиус трещины").defineInRange("zoneRadius", 2.0, 0.5, 32.0);
        RUNE_ZONE_DAMAGE = b.comment("Разлом: урон в трещине раз в 0,5 с").defineInRange("zoneDamage", 1.0, 0.0, 1000.0);
        RUNE_PULL = b.comment("Тяга: сила притяжения").defineInRange("pull", 0.6, 0.0, 10.0);
        RUNE_FROST_SECONDS = b.comment("Иней: заморозка, секунды").defineInRange("frostSeconds", 3, 0, 600);
        RUNE_SHATTER_DAMAGE = b.comment("Иней: урон соседям, если замёрзшая цель погибла").defineInRange("shatterDamage", 3.0, 0.0, 1000.0);
        RUNE_VOID_SECONDS = b.comment("Пустота: цель без защиты, секунды").defineInRange("voidSeconds", 5, 0, 600);
        RUNE_VOID_ARMOR = b.comment("Пустота: доля брони, которую теряет цель").defineInRange("voidArmor", 0.5, 0.0, 1.0);
        RUNE_FACET_SHARE = b.comment("Сила грани и второго оберега относительно основы").defineInRange("facetShare", 0.5, 0.0, 10.0);
        WORD_DAMAGE = b.comment("Особые слова: базовый урон").defineInRange("wordDamage", 4.0, 0.0, 1000.0);
        WORD_SECONDS = b.comment("Особые слова: длительность облаков, полей и воронок, секунды").defineInRange("wordSeconds", 3, 0, 600);
        WORD_RADIUS = b.comment("Особые слова: радиус").defineInRange("wordRadius", 3.0, 0.5, 32.0);
        b.pop();

        b.comment("Руны на пластинах тяжёлой брони — обереги и особые слова брони").push("wards");
        WARD_STUN_TICKS = b.comment("Ток: оглушение ударившего, тики").defineInRange("stunTicks", 20, 0, 1200);
        WARD_SHARE = b.comment("Узы: доля урона пластины, которая уходит на соседнюю").defineInRange("share", 0.25, 0.0, 1.0);
        WARD_ECHO_RESTORE = b.comment("Эхо: разбитая пластина один раз восстанавливается на эту долю").defineInRange("echoRestore", 0.333, 0.0, 1.0);
        WARD_REDUCE = b.comment("Тяга и Пустота: на эту долю меньше урона от снарядов и магии").defineInRange("reduce", 0.2, 0.0, 1.0);
        WARD_SLOW_TICKS = b.comment("Иней и Разлом: замедление, тики").defineInRange("slowTicks", 40, 0, 1200);
        WARD_COOLDOWN = b.comment("Перезарядка слов брони, тики (Огненный круг, Отражение, Ледяная стена)").defineInRange("cooldown", 160, 0, 72000);
        WARD_RARE_COOLDOWN = b.comment("Перезарядка редких слов (Ледяной кокон), тики").defineInRange("rareCooldown", 6000, 0, 720000);
        WARD_STRONG_HIT = b.comment("Сильный удар для слов брони, урон").defineInRange("strongHit", 6.0, 0.0, 1000.0);
        WARD_ROOT_REDUCE = b.comment("Укоренение: насколько меньше урон, если стоять 2 с").defineInRange("rootReduce", 0.4, 0.0, 1.0);
        WARD_DISTORT_CHANCE = b.comment("Искажение: шанс, что снаряд пройдёт мимо").defineInRange("distortChance", 0.5, 0.0, 1.0);
        b.pop();

        b.comment("Рельса: тяжёлые болты, заряд конденсатора перед выстрелом, питание электричеством").push("rail");
        RAIL_CAPACITY = b.comment("Запас электричества").defineInRange("capacity", 400, 1, 100000);
        RAIL_SHOT_COST = b.comment("Электричества на выстрел").defineInRange("shotCost", 25, 0, 100000);
        RAIL_CHARGE_TICKS = b.comment("Заряд конденсатора перед выстрелом, тики").defineInRange("chargeTicks", 20, 1, 1200);
        RAIL_RANGE = b.defineInRange("range", 64.0, 1.0, 256.0);
        RAIL_PIERCE = b.comment("Сколько целей пробивает болт").defineInRange("pierce", 2, 1, 64);
        RAIL_FALLOFF = b.comment("Каждая следующая цель получает на эту долю меньше").defineInRange("falloff", 0.25, 0.0, 1.0);
        BOLTS = b.comment("Болты по материалу: «материал|урон|шанс поломки». Сталь пробивает на одну цель больше,",
                        "медь бьёт сильнее по целям без брони и слабее по бронированным")
                .defineListAllowEmpty("bolts", List.of("iron|16|0.3", "copper|13|0.6", "gold|13|0.6", "steel|19|0.15", "red_gold|16|0.35"),
                        () -> "", o -> o instanceof String s && s.split("\\|").length == 3);
        PIERCE_DAMAGE = b.comment("«Пробой»: прибавка урона за ступень").defineInRange("pierceDamage", 0.15, 0.0, 10.0);
        PIERCE_TARGETS = b.comment("«Пробой»: целей за ступень I").defineInRange("pierceTargets", 2, 0, 64);
        PIERCE_ARMOR = b.comment("«Пробой»: доля урона, которая идёт мимо брони").defineInRange("pierceArmor", 0.4, 0.0, 1.0);
        CAST_BOLT_MB = b.comment("Литейная: расплава на один болт").defineInRange("castBoltMb", 200, 1, 10000);
        b.pop();

        b.comment("Стол и высекатель: рельса, «Пробой» I, «Точность» I. Формат «id*кол-во;…»").push("tableRunes");
        TABLE_RAIL_PROTOTYPE = b.comment("Прототип рельсы")
                .define("railPrototype", "adept_core:crude_coil*4;adept_core:crude_wiring*2;adept_core:crude_emitter;minecraft:iron_ingot*6");
        TABLE_RAIL_PIERCE_1 = b.comment("Рельса, ствол, путь «Пробой», ступень I. Ствол — катушка и каркас; технический сегмент,",
                        "подпорка — магические катушки (60%)")
                .define("railPierce1", "adept_core:coil_1_magic*3;adept_core:coil_1_tech*2;adept_core:frame_1*2;minecraft:iron_ingot*4");
        TABLE_CARVER_PRECISION_1 = b.comment("Высекатель рун, путь «Точность», ступень I (вторая руна на литом изделии).",
                        "Семейства — эмиттер и плата управления; машина-гибрид, вариации поровну")
                .define("carverPrecision1", "adept_core:emitter_1_tech*2;adept_core:emitter_1_magic*2;adept_core:control_board_1_tech;adept_core:control_board_1_magic");
        b.pop();
    }
}
