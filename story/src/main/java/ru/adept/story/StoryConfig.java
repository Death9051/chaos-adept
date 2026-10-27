package ru.adept.story;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StoryConfig {
    public enum Branch { PUBLIC, STREAMER }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Branch> BRANCH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        BRANCH = builder
                .comment(
                        "Ветка сюжета для новых миров.",
                        "PUBLIC — обычная ветка для всех игроков.",
                        "STREAMER — ветка для стримов автора сборки. Если вы не автор, не меняйте.",
                        "Ветка записывается в мир при его создании. Уже начатые миры продолжают свою ветку.")
                .defineEnum("branch", Branch.PUBLIC);
        SPEC = builder.build();
    }

    private StoryConfig() {
    }
}
