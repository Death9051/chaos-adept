package ru.adept.story.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.api.WorldProgress;
import ru.adept.story.AdeptStory;
import ru.adept.story.journal.Journal;
import ru.adept.story.journal.JournalData;
import ru.adept.story.journal.JournalDef;
import ru.adept.story.journal.JournalDefs;
import ru.adept.story.net.EntryView;
import ru.adept.story.trader.TraderTalk;

import java.util.List;
import java.util.Map;

/**
 * Автотесты журнала и реплик торговца (CI, servertest). Каждый тест сбрасывает журнал и
 * проверяет всё за один тик, поэтому общее состояние мира не мешает соседним тестам.
 */
@GameTestHolder(AdeptStory.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JournalGameTests {
    private JournalGameTests() {
    }

    private static MinecraftServer reset(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        JournalData.get(server).reset();
        return server;
    }

    @GameTest(template = "empty")
    public static void definitionsLoaded(GameTestHelper helper) {
        helper.assertTrue(JournalDefs.all().size() >= 42, "Записей журнала меньше 42: " + JournalDefs.all().size());
        for (String id : List.of("prologue/first_night", "nodes/first", "nodes/level_1_repeat", "ritual/first", "trader/level_3")) {
            helper.assertTrue(JournalDefs.get(id) != null, "Нет записи " + id);
        }
        for (JournalDef def : JournalDefs.all()) {
            helper.assertTrue(!def.keysFor("public").isEmpty() && !def.keysFor("streamer").isEmpty(),
                    "У записи " + def.id() + " нет текста для одной из веток");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onceEntryOnlyOnce(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        List<EntryView> first = Journal.fire(server, "artifact_table_built");
        List<EntryView> second = Journal.fire(server, "artifact_table_built");
        helper.assertTrue(first.size() == 1 && second.isEmpty(), "Запись «стол собран» должна появиться один раз");
        helper.assertTrue(first.get(0).keys().equals(List.of("adept_story.journal.prologue.artifact_table.public")),
                "Ожидался текст публичной ветки: " + first.get(0).keys());
        helper.assertTrue(JournalData.get(server).entries.size() == 1, "В журнале должна быть одна запись");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nodeAbsorptionOrder(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        expectIds(helper, Journal.nodeAbsorbed(server, 1), "nodes/first");
        expectIds(helper, Journal.nodeAbsorbed(server, 1), "nodes/level_1_repeat");
        expectIds(helper, Journal.nodeAbsorbed(server, 2), "nodes/level_2_first");
        expectIds(helper, Journal.nodeAbsorbed(server, 2), "nodes/level_2_repeat");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poolWithoutRepeats(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        Journal.nodeAbsorbed(server, 1);
        int pool = JournalDefs.get("nodes/level_1_repeat").keysFor("public").size();
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (int i = 0; i < pool; i++) {
            List<EntryView> added = Journal.nodeAbsorbed(server, 1);
            helper.assertTrue(added.size() == 1, "Набор кончился раньше времени");
            keys.add(added.get(0).keys().get(0));
        }
        helper.assertTrue(keys.size() == pool, "Варианты набора повторились");
        helper.assertTrue(Journal.nodeAbsorbed(server, 1).isEmpty(), "После исчерпания набора записей быть не должно");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void traderRitualAndMilestone(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        WorldProgress progress = WorldProgress.get(server);
        int oldTier = progress.nodeTier();
        try {
            progress.setNodeTier(server, 1);
            List<String> ritual = TraderTalk.arrivalLines(server, true);
            helper.assertTrue(ritual.size() == 3 && ritual.get(0).endsWith("first_ritual.public.1"),
                    "Реплики первого ритуала: " + ritual);
            helper.assertTrue(JournalData.get(server).ritualPending, "Запись о ритуале должна ждать ухода торговца");
            expectIds(helper, Journal.fire(server, "ritual_done", Map.of("first", "game")), "ritual/first");

            List<String> greet = TraderTalk.arrivalLines(server, false);
            helper.assertTrue(greet.size() == 1 && greet.get(0).contains(".greet."), "Ожидалось приветствие: " + greet);

            progress.setNodeTier(server, 3);
            List<String> milestone = TraderTalk.arrivalLines(server, false);
            helper.assertTrue(milestone.equals(List.of(AdeptStory.MOD_ID + ".trader.level_3")), "Веха 3: " + milestone);
            helper.assertTrue(JournalData.get(server).entries.stream().anyMatch(e -> e.id().equals("trader/level_3")
                    && e.page().equals(JournalDef.PAGE_TRADER)), "Условия вехи 3 не попали на страницу торговца");
            helper.assertTrue(TraderTalk.arrivalLines(server, false).get(0).contains(".greet."), "Веха не должна повторяться");
        } finally {
            progress.setNodeTier(server, oldTier);
        }
        helper.succeed();
    }

    private static void expectIds(GameTestHelper helper, List<EntryView> added, String... ids) {
        List<String> got = added.stream().map(EntryView::id).toList();
        helper.assertTrue(got.equals(List.of(ids)), "Ожидались записи " + List.of(ids) + ", получены " + got);
    }
}
