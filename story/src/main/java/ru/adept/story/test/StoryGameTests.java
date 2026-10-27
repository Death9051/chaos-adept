package ru.adept.story.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.api.WorldFlags;
import ru.adept.story.AdeptStory;
import ru.adept.story.StoryBranch;
import ru.adept.story.StoryConfig;

/** Серверные автотесты «Сюжета» (CI, servertest). Проверяют и связку с ядром. */
@GameTestHolder(AdeptStory.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoryGameTests {
    private StoryGameTests() {
    }

    @GameTest(template = "empty")
    public static void branchFixedInWorld(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(WorldFlags.get(server).has(StoryBranch.FLAG),
                "Ветка сюжета не записана в мир при запуске");
        // В CI конфиг не трогаем, поэтому ветка должна быть публичной.
        helper.assertTrue(StoryBranch.of(server) == StoryConfig.Branch.PUBLIC,
                "По умолчанию ожидалась публичная ветка");
        helper.succeed();
    }
}
