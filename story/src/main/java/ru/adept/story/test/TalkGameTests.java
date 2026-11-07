package ru.adept.story.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.api.event.RaidEvent;
import ru.adept.core.api.event.VillageEvent;
import ru.adept.core.api.event.WorldProgressEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.journal.JournalData;
import ru.adept.story.journal.JournalDefs;
import ru.adept.story.journal.StoryTriggers;
import ru.adept.story.talk.HeroTalk;
import ru.adept.story.talk.VillageTalk;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Автотесты среза 2 (CI, servertest): реплики героев и жителей, записи журнала на события ядра.
 * Каждый тест сбрасывает откаты и журнал и проверяет всё за один тик.
 */
@GameTestHolder(AdeptStory.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TalkGameTests {
    private TalkGameTests() {
    }

    private static MinecraftServer reset(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        JournalData.get(server).reset();
        HeroTalk.reset();
        VillageTalk.reset();
        return server;
    }

    /** Все ключи реплик из таблиц кода есть в lang-файле. */
    @GameTest(template = "empty")
    public static void talkKeysInLang(GameTestHelper helper) {
        JsonObject lang;
        try (InputStream in = AdeptStory.class.getResourceAsStream("/assets/adept_story/lang/ru_ru.json")) {
            helper.assertTrue(in != null, "Нет lang-файла сюжета");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        HeroTalk.LINES.forEach((group, count) -> {
            for (int i = 1; i <= count; i++) {
                String key = HeroTalk.KEY + group + "." + i;
                helper.assertTrue(lang.has(key), "Нет строки " + key);
            }
            helper.assertTrue(!lang.has(HeroTalk.KEY + group + "." + (count + 1)), "В lang больше строк, чем в коде: " + group);
        });
        VillageTalk.LINES.forEach((group, count) -> {
            for (int i = 1; i <= count; i++) {
                helper.assertTrue(lang.has(VillageTalk.KEY + group + "." + i), "Нет строки " + VillageTalk.KEY + group + "." + i);
            }
            helper.assertTrue(!lang.has(VillageTalk.KEY + group + "." + (count + 1)), "В lang больше строк, чем в коде: " + group);
        });
        for (String c : List.of("tracker", "warrior", "archer", "commander")) {
            helper.assertTrue(lang.has(HeroTalk.SPEAKER + c), "Нет имени говорящего для класса " + c);
            for (String e : List.of("spotted", "wounded", "missed", "player_lost", "ally_fell", "squad_retreating")) {
                helper.assertTrue(!HeroTalk.pool(e, c, "veteran").isEmpty(), "Нет реплик: " + c + " " + e);
            }
        }
        helper.assertTrue(lang.has("adept_core.commander_order.text"), "Нет шаблона приказа командира");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heroPoolByClassAndRank(GameTestHelper helper) {
        List<String> apprentice = HeroTalk.pool("wounded", "warrior", "apprentice");
        List<String> veteran = HeroTalk.pool("wounded", "warrior", "veteran");
        helper.assertTrue(apprentice.contains(HeroTalk.KEY + "warrior.wounded.1") && apprentice.contains(HeroTalk.KEY + "common.wounded.1"),
                "У ученика-воина нет строк класса или общих: " + apprentice);
        helper.assertTrue(apprentice.stream().anyMatch(k -> k.contains(".apprentice.")), "У ученика нет строк о страхе");
        helper.assertTrue(veteran.stream().noneMatch(k -> k.contains(".apprentice.")), "У ветерана строки ученика");
        helper.assertTrue(HeroTalk.pool("missed", "archer", "veteran").contains(HeroTalk.KEY + "archer.missed.1"), "Нет промаха лучника");
        helper.assertTrue(HeroTalk.pool("ally_healed", "warrior", "veteran").isEmpty(), "Лечение — только у целителя");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heroChanceAndCooldowns(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        UUID squad = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        // Ветеран при высоком броске молчит, ученик при том же броске говорит.
        helper.assertTrue(HeroTalk.line(server, a, squad, "spotted", "warrior", "veteran", 0.5) == null, "Ветеран должен промолчать");
        String first = HeroTalk.line(server, b, squad, "spotted", "archer", "apprentice", 0.5);
        helper.assertTrue(first != null && HeroTalk.pool("spotted", "archer", "apprentice").contains(first), "Ученик должен сказать: " + first);
        // Сразу после реплики — общая пауза: молчат все.
        helper.assertTrue(HeroTalk.line(server, a, UUID.randomUUID(), "wounded", "warrior", "apprentice", 0) == null, "Нет общей паузы");
        // Отступление звучит при любом броске, но и оно ждёт откатов.
        HeroTalk.reset();
        helper.assertTrue(HeroTalk.line(server, a, squad, "squad_retreating", "commander", "veteran", 0.99) != null,
                "Приказ отступать должен звучать всегда");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void villagerMoodByThreat(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        String calm = VillageTalk.line(server, "villager", 10);
        helper.assertTrue(calm != null && calm.startsWith(VillageTalk.KEY + "villager.low."), "Мирная реплика: " + calm);
        helper.assertTrue(VillageTalk.line(server, "villager", 10) == null, "Нет паузы между репликами жителей");
        VillageTalk.reset();
        String scared = VillageTalk.line(server, "elder", 80);
        helper.assertTrue(scared != null && scared.startsWith(VillageTalk.KEY + "elder.high."), "Старейшина при высокой угрозе: " + scared);
        VillageTalk.reset();
        helper.assertTrue(VillageTalk.line(server, "militia", 80) == null, "У ополченцев реплик пока нет");
        helper.succeed();
    }

    /** События ядра доходят до журнала через шину NeoForge. */
    @GameTest(template = "empty")
    public static void slice2JournalEntries(GameTestHelper helper) {
        MinecraftServer server = reset(helper);
        for (String id : List.of("firsts/first_threat", "nodes/tier_2", "firsts/first_heroes", "firsts/first_ruins")) {
            helper.assertTrue(JournalDefs.get(id) != null, "Нет записи " + id);
        }
        NeoForge.EVENT_BUS.post(new WorldProgressEvent.ThreatLevelChanged(server, 1, 0));
        expectOnly(helper, server, List.of());
        NeoForge.EVENT_BUS.post(new WorldProgressEvent.ThreatLevelChanged(server, 0, 1));
        NeoForge.EVENT_BUS.post(new WorldProgressEvent.ThreatLevelChanged(server, 1, 2));
        expectOnly(helper, server, List.of("firsts/first_threat"));

        // Напрямую, без шины: ядро на это событие меняет сложность Improved Mobs в мире тестов.
        StoryTriggers.onNodeTier(new WorldProgressEvent.NodeTierChanged(server, 1, 2));
        NeoForge.EVENT_BUS.post(new RaidEvent.Ended(server, UUID.randomUUID(), 1, "Ольховка", false));
        expectOnly(helper, server, List.of("firsts/first_threat", "nodes/tier_2"));
        NeoForge.EVENT_BUS.post(new RaidEvent.Ended(server, UUID.randomUUID(), 1, "Ольховка", true));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        NeoForge.EVENT_BUS.post(new VillageEvent.RuinsEntered(player, 1L, "Ольховка"));
        NeoForge.EVENT_BUS.post(new VillageEvent.RuinsEntered(player, 1L, "Ольховка"));
        expectOnly(helper, server, List.of("firsts/first_threat", "nodes/tier_2", "firsts/first_heroes", "firsts/first_ruins"));
        helper.assertTrue(JournalData.get(server).entries.stream().filter(e -> e.id().equals("nodes/tier_2")).allMatch(e -> e.popup()),
                "Переход на второй уровень должен всплывать");
        helper.succeed();
    }

    private static void expectOnly(GameTestHelper helper, MinecraftServer server, List<String> ids) {
        List<String> got = JournalData.get(server).entries.stream().map(e -> e.id()).toList();
        helper.assertTrue(got.equals(ids), "Ожидались записи " + ids + ", получены " + got);
    }
}
