package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Числа среза 3, часть 4: маг и целитель, ранг мастер и третьи приёмы всех классов («Мобы», «Герои и набеги»).
 * Файл тот же — {@code config/adept_core-common.toml}; раздел определяется из статического блока {@link BalanceConfig}.
 */
public final class HeroConfig {
    // Новые классы
    public static ModConfigSpec.DoubleValue MAGE_HEALTH;
    public static ModConfigSpec.DoubleValue HEALER_HEALTH;
    public static ModConfigSpec.DoubleValue MAGE_DAMAGE;
    public static ModConfigSpec.DoubleValue HEALER_DAMAGE;

    // Мастер
    public static ModConfigSpec.DoubleValue MASTER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue MASTER_PER_TIER;
    public static ModConfigSpec.DoubleValue EMBLEM_CHANCE_MASTER;

    // Маг
    public static ModConfigSpec.IntValue MAGE_CAST_TICKS;
    public static ModConfigSpec.DoubleValue MAGE_BOLT_DAMAGE;
    public static ModConfigSpec.DoubleValue MAGE_KEEP_DISTANCE;
    public static ModConfigSpec.IntValue MAGE_ELEMENT_TICKS;
    public static ModConfigSpec.IntValue BARRIER_COOLDOWN;
    public static ModConfigSpec.IntValue BARRIER_TICKS;
    public static ModConfigSpec.IntValue BARRIER_LEVEL;
    public static ModConfigSpec.IntValue ROOTS_COOLDOWN;
    public static ModConfigSpec.IntValue ROOTS_TICKS;

    // Целитель
    public static ModConfigSpec.IntValue HEAL_COOLDOWN;
    public static ModConfigSpec.DoubleValue HEAL_AMOUNT;
    public static ModConfigSpec.DoubleValue HEAL_BELOW;
    public static ModConfigSpec.DoubleValue SUPPORT_RADIUS;
    public static ModConfigSpec.IntValue CLEANSE_COOLDOWN;
    public static ModConfigSpec.DoubleValue REVIVE_RADIUS;
    public static ModConfigSpec.DoubleValue REVIVE_HEALTH;

    // Третьи приёмы прежних классов
    public static ModConfigSpec.IntValue WAR_CRY_COOLDOWN;
    public static ModConfigSpec.IntValue WAR_CRY_TICKS;
    public static ModConfigSpec.DoubleValue WAR_CRY_RADIUS;
    public static ModConfigSpec.IntValue CHAIN_COOLDOWN;
    public static ModConfigSpec.IntValue CHAIN_TICKS;
    public static ModConfigSpec.IntValue REINFORCEMENTS;

    private HeroConfig() {
    }

    static void define(ModConfigSpec.Builder b) {
        b.comment("Герои, срез 3: маг, целитель, ранг мастер и третьи приёмы").push("heroesSlice3");
        MAGE_HEALTH = b.defineInRange("mageHealth", 20.0, 1.0, 1000.0);
        HEALER_HEALTH = b.defineInRange("healerHealth", 22.0, 1.0, 1000.0);
        MAGE_DAMAGE = b.comment("Удар мага вблизи (посохом)").defineInRange("mageDamage", 3.0, 0.0, 100.0);
        HEALER_DAMAGE = b.defineInRange("healerDamage", 3.0, 0.0, 100.0);
        MASTER_MULTIPLIER = b.comment("Мастер: множитель здоровья и урона").defineInRange("masterMultiplier", 1.8, 1.0, 10.0);
        MASTER_PER_TIER = b.comment("Шанс мастера: прибавка за каждый уровень узлов выше второго (база — угроза выше 75)")
                .defineInRange("masterPerTier", 0.1, 0.0, 1.0);
        EMBLEM_CHANCE_MASTER = b.comment("Эмблема (трофей для Альянса) с мастера").defineInRange("emblemChanceMaster", 0.9, 0.0, 1.0);

        MAGE_CAST_TICKS = b.comment("Маг: снаряд стихии края, тики между бросками").defineInRange("mageCastTicks", 50, 5, 1200);
        MAGE_BOLT_DAMAGE = b.comment("Урон снаряда стихии").defineInRange("mageBoltDamage", 5.0, 0.0, 100.0);
        MAGE_KEEP_DISTANCE = b.comment("Маг отходит, если игрок ближе").defineInRange("mageKeepDistance", 6.0, 0.0, 32.0);
        MAGE_ELEMENT_TICKS = b.comment("Действие стихии (горение, холод, яд), тики").defineInRange("mageElementTicks", 60, 0, 1200);
        BARRIER_COOLDOWN = b.comment("Маг-ветеран: барьер на союзника, перезарядка").defineInRange("barrierCooldownTicks", 240, 1, 6000);
        BARRIER_TICKS = b.comment("Барьер держится, тики").defineInRange("barrierTicks", 200, 1, 6000);
        BARRIER_LEVEL = b.comment("Сила барьера: уровень поглощения (0 — I, 4 единицы)").defineInRange("barrierLevel", 1, 0, 9);
        ROOTS_COOLDOWN = b.comment("Маг-мастер: корни, перезарядка").defineInRange("rootsCooldownTicks", 300, 1, 6000);
        ROOTS_TICKS = b.comment("Корни держат игрока, тики").defineInRange("rootsTicks", 40, 1, 600);

        HEAL_COOLDOWN = b.comment("Целитель: лечение союзника, перезарядка").defineInRange("healCooldownTicks", 80, 1, 6000);
        HEAL_AMOUNT = b.comment("Лечение за раз").defineInRange("healAmount", 6.0, 0.0, 1000.0);
        HEAL_BELOW = b.comment("Лечит союзника ниже этой доли здоровья").defineInRange("healBelow", 0.75, 0.0, 1.0);
        SUPPORT_RADIUS = b.comment("Радиус лечения, снятия эффектов и барьера").defineInRange("supportRadius", 12.0, 1.0, 64.0);
        CLEANSE_COOLDOWN = b.comment("Целитель-ветеран: снятие негативных эффектов, перезарядка").defineInRange("cleanseCooldownTicks", 120, 1, 6000);
        REVIVE_RADIUS = b.comment("Целитель-мастер: поднимает павшего бойца в этом радиусе, раз за бой").defineInRange("reviveRadius", 16.0, 1.0, 64.0);
        REVIVE_HEALTH = b.comment("Поднятый боец встаёт с этой долей здоровья").defineInRange("reviveHealth", 0.5, 0.05, 1.0);

        WAR_CRY_COOLDOWN = b.comment("Воин-мастер: боевой клич, перезарядка").defineInRange("warCryCooldownTicks", 400, 1, 6000);
        WAR_CRY_TICKS = b.comment("Клич: сила отряду, тики").defineInRange("warCryTicks", 160, 1, 6000);
        WAR_CRY_RADIUS = b.defineInRange("warCryRadius", 10.0, 1.0, 64.0);
        CHAIN_COOLDOWN = b.comment("Лучник-мастер: стрела с цепью, перезарядка").defineInRange("chainCooldownTicks", 200, 1, 6000);
        CHAIN_TICKS = b.comment("Стрела с цепью обездвиживает, тики").defineInRange("chainTicks", 20, 1, 600);
        REINFORCEMENTS = b.comment("Командир-мастер: подкрепление раз за бой, героев").defineInRange("reinforcements", 2, 0, 8);
        b.pop();
    }
}
