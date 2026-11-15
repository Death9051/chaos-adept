package ru.adept.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Цена ступеней прокачки по сегментам («Артефакты», «Прокачка: числа», «Каркас»; «Материалы и расходники», «Компоненты»).
 * Лежит в том же файле {@code config/adept_core-common.toml}, раздел определяется из статического блока {@link BalanceConfig}.
 * Сами компоненты ступени задаёт сегмент: два семейства в пропорции 3:1 (путь основы) и 1:3 (путь деталей).
 */
public final class UpgradeConfig {
    public static ModConfigSpec.ConfigValue<String> STEP_COMPONENTS;
    public static ModConfigSpec.ConfigValue<String> STEP_MAGIC_SHARE;
    public static ModConfigSpec.IntValue APEX_PER_FAMILY;
    public static ModConfigSpec.IntValue APEX_CLUSTERS;
    public static ModConfigSpec.IntValue APEX_CLUSTER_LEVEL;

    private UpgradeConfig() {
    }

    static void define(ModConfigSpec.Builder b) {
        b.comment("Цена ступеней прокачки по сегментам (пока — гаусс-пушка)").push("upgrades");
        STEP_COMPONENTS = b.comment("Компонентов на ступени I, II, III (ранг компонента равен ступени), через запятую")
                .define("stepComponents", "4,6,8");
        STEP_MAGIC_SHARE = b.comment("Доля рунных (магических) компонентов на ступенях I, II, III: подпорка магией падает")
                .define("stepMagicShare", "0.6,0.4,0.2");
        APEX_PER_FAMILY = b.comment("Вершина: компонентов III ранга каждого из двух семейств сегмента")
                .defineInRange("apexPerFamily", 2, 0, 64);
        APEX_CLUSTERS = b.comment("Вершина: сгустков узла").defineInRange("apexClusters", 1, 0, 64);
        APEX_CLUSTER_LEVEL = b.comment("Вершина: уровень сгустка узла").defineInRange("apexClusterLevel", 3, 1, 5);
        b.pop();
    }

    /** Число из списка «a,b,c» по ступени 1–3; при ошибке записи — запасное значение. */
    public static double at(String list, int step, double fallback) {
        String[] parts = list.split(",");
        if (step < 1 || step > parts.length) {
            return fallback;
        }
        try {
            return Double.parseDouble(parts[step - 1].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
