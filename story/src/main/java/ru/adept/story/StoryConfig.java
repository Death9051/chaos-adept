package ru.adept.story;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StoryConfig {
    public enum Branch { PUBLIC, STREAMER }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Branch> BRANCH;

    // Реплики героев и жителей
    public static final ModConfigSpec.IntValue HERO_RADIUS;
    public static final ModConfigSpec.DoubleValue APPRENTICE_CHANCE;
    public static final ModConfigSpec.DoubleValue VETERAN_CHANCE;
    public static final ModConfigSpec.IntValue HERO_COOLDOWN;
    public static final ModConfigSpec.IntValue SQUAD_EVENT_COOLDOWN;
    public static final ModConfigSpec.IntValue HERO_GLOBAL_COOLDOWN;
    public static final ModConfigSpec.IntValue VILLAGER_COOLDOWN;
    public static final ModConfigSpec.IntValue HIGH_THREAT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        BRANCH = builder
                .comment(
                        "Ветка сюжета для новых миров.",
                        "PUBLIC — обычная ветка для всех игроков.",
                        "STREAMER — ветка для стримов автора сборки. Если вы не автор, не меняйте.",
                        "Ветка записывается в мир при его создании. Уже начатые миры продолжают свою ветку.")
                .defineEnum("branch", Branch.PUBLIC);

        builder.comment("Реплики героев и жителей субтитрами").push("talk");
        HERO_RADIUS = builder.comment("Реплику героя слышат игроки в этом радиусе, блоков")
                .defineInRange("heroRadius", 32, 4, 128);
        APPRENTICE_CHANCE = builder.comment("Шанс, что ученик скажет реплику на событие боя")
                .defineInRange("apprenticeChance", 0.7, 0.0, 1.0);
        VETERAN_CHANCE = builder.comment("Шанс, что ветеран скажет реплику на событие боя (ветераны молчаливее)")
                .defineInRange("veteranChance", 0.35, 0.0, 1.0);
        HERO_COOLDOWN = builder.comment("Откат реплик одного героя, секунд")
                .defineInRange("heroCooldownSeconds", 12, 0, 600);
        SQUAD_EVENT_COOLDOWN = builder.comment("Откат одного и того же события внутри отряда, секунд (на «союзник пал» отвечает один боец)")
                .defineInRange("squadEventCooldownSeconds", 10, 0, 600);
        HERO_GLOBAL_COOLDOWN = builder.comment("Пауза между любыми репликами героев, секунд (чтобы не говорили хором)")
                .defineInRange("heroGlobalCooldownSeconds", 3, 0, 60);
        VILLAGER_COOLDOWN = builder.comment("Пауза между репликами жителей и старейшин, секунд")
                .defineInRange("villagerCooldownSeconds", 5, 0, 600);
        HIGH_THREAT = builder.comment("С этой угрозы жители говорят как с опасным чужаком (в тон порогу ядра, с которого жители не торгуют)")
                .defineInRange("highThreat", 50, 0, 100);
        builder.pop();

        SPEC = builder.build();
    }

    private StoryConfig() {
    }
}
