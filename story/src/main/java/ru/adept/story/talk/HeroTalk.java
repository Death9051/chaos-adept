package ru.adept.story.talk;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import ru.adept.core.api.event.HeroEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.StoryConfig;
import ru.adept.story.net.StoryNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Реплики героев по боевым событиям ядра. Набор строк — общие, по классу и по рангу
 * (у учеников свои строки о страхе, у ветеранов — сдержанные). Ученики говорят чаще, ветераны реже;
 * откат у героя, у события в отряде и общая пауза, чтобы герои не говорили хором.
 * Ключи: {@code adept_story.hero.<группа>.<событие>.<номер>}.
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class HeroTalk {
    public static final String KEY = AdeptStory.MOD_ID + ".hero.";
    public static final String SPEAKER = AdeptStory.MOD_ID + ".speaker.hero.";

    /** Сколько строк в каждой группе: «группа.событие» → число. Группа — класс, ранг или common. */
    public static final Map<String, Integer> LINES = Map.ofEntries(
            Map.entry("common.spotted", 2), Map.entry("common.wounded", 1), Map.entry("common.missed", 1),
            Map.entry("common.player_lost", 2), Map.entry("common.ally_fell", 1), Map.entry("common.squad_retreating", 1),
            Map.entry("apprentice.spotted", 1), Map.entry("apprentice.wounded", 2), Map.entry("apprentice.ally_fell", 1),
            Map.entry("apprentice.player_lost", 1), Map.entry("apprentice.squad_retreating", 1),
            Map.entry("veteran.wounded", 1), Map.entry("veteran.ally_fell", 1),
            Map.entry("warrior.spotted", 2), Map.entry("warrior.wounded", 1), Map.entry("warrior.ally_fell", 1),
            Map.entry("warrior.player_lost", 1),
            Map.entry("archer.spotted", 2), Map.entry("archer.missed", 1), Map.entry("archer.ally_fell", 1),
            Map.entry("archer.player_lost", 1),
            Map.entry("tracker.spotted", 2), Map.entry("tracker.player_lost", 1), Map.entry("tracker.squad_retreating", 1),
            Map.entry("commander.spotted", 1), Map.entry("commander.wounded", 1), Map.entry("commander.ally_fell", 1),
            Map.entry("commander.player_lost", 1), Map.entry("commander.squad_retreating", 1),
            // Маг и целитель — заготовка: классов ещё нет в ядре.
            Map.entry("mage.spotted", 2), Map.entry("mage.wounded", 2),
            Map.entry("healer.spotted", 2), Map.entry("healer.ally_healed", 1), Map.entry("healer.ally_fell", 1));

    private static final Map<UUID, Long> HERO_UNTIL = new HashMap<>();
    private static final Map<String, Long> SQUAD_EVENT_UNTIL = new HashMap<>();
    private static long globalUntil;

    private HeroTalk() {
    }

    @SubscribeEvent
    public static void onSpotted(HeroEvent.Spotted event) {
        react(event, "spotted");
    }

    @SubscribeEvent
    public static void onWounded(HeroEvent.Wounded event) {
        react(event, "wounded");
    }

    @SubscribeEvent
    public static void onMissed(HeroEvent.Missed event) {
        react(event, "missed");
    }

    @SubscribeEvent
    public static void onPlayerLost(HeroEvent.PlayerLost event) {
        react(event, "player_lost");
    }

    @SubscribeEvent
    public static void onAllyFell(HeroEvent.AllyFell event) {
        react(event, "ally_fell");
    }

    @SubscribeEvent
    public static void onRetreating(HeroEvent.SquadRetreating event) {
        react(event, "squad_retreating");
    }

    @SubscribeEvent
    public static void onAllyHealed(HeroEvent.AllyHealed event) {
        react(event, "ally_healed");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }

    /** Сбросить откаты (тесты, остановка сервера). */
    public static void reset() {
        HERO_UNTIL.clear();
        SQUAD_EVENT_UNTIL.clear();
        globalUntil = 0;
    }

    private static void react(HeroEvent event, String type) {
        Mob hero = event.getHero();
        if (!(hero.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        String key = line(server, hero.getUUID(), event.getSquad(), type, event.getHeroClass(), event.getRank(),
                hero.getRandom().nextDouble());
        if (key == null) {
            return;
        }
        String speaker = hero.hasCustomName() && hero.getCustomName() != null
                // Неизвестный ключ клиент покажет как есть — так приходит имя командира.
                ? hero.getCustomName().getString()
                : SPEAKER + event.getHeroClass();
        double radius = StoryConfig.HERO_RADIUS.get();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(hero) <= radius * radius) {
                StoryNetwork.sendSubtitle(player, speaker, List.of(key));
            }
        }
    }

    /**
     * Решает, скажет ли герой что-нибудь, и выбирает строку. {@code roll} — случайное число 0..1 для шанса по рангу.
     * Если реплика будет, ставит откаты. Возвращает ключ строки или {@code null}.
     */
    public static String line(MinecraftServer server, UUID hero, UUID squad, String type, String heroClass, String rank,
                              double roll) {
        List<String> pool = pool(type, heroClass, rank);
        if (pool.isEmpty()) {
            return null;
        }
        long now = server.getTickCount();
        String squadKey = (squad != null ? squad : hero) + ":" + type;
        if (now < globalUntil || now < HERO_UNTIL.getOrDefault(hero, 0L) || now < SQUAD_EVENT_UNTIL.getOrDefault(squadKey, 0L)) {
            return null;
        }
        // Приказ отступать звучит всегда, остальное — по шансу ранга.
        if (!type.equals("squad_retreating") && roll >= chance(rank)) {
            return null;
        }
        globalUntil = now + StoryConfig.HERO_GLOBAL_COOLDOWN.get() * 20L;
        HERO_UNTIL.put(hero, now + StoryConfig.HERO_COOLDOWN.get() * 20L);
        SQUAD_EVENT_UNTIL.put(squadKey, now + StoryConfig.SQUAD_EVENT_COOLDOWN.get() * 20L);
        return pool.get(server.overworld().getRandom().nextInt(pool.size()));
    }

    /** Все строки, из которых выбирается реплика героя этого класса и ранга на событие. */
    public static List<String> pool(String type, String heroClass, String rank) {
        List<String> keys = new ArrayList<>();
        add(keys, heroClass, type);
        add(keys, rank, type);
        add(keys, "common", type);
        return keys;
    }

    private static void add(List<String> keys, String group, String type) {
        int count = LINES.getOrDefault(group + "." + type, 0);
        for (int i = 1; i <= count; i++) {
            keys.add(KEY + group + "." + type + "." + i);
        }
    }

    private static double chance(String rank) {
        return "veteran".equals(rank) ? StoryConfig.VETERAN_CHANCE.get() : StoryConfig.APPRENTICE_CHANCE.get();
    }
}
