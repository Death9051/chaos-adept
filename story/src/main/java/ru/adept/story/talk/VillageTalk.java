package ru.adept.story.talk;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import ru.adept.core.api.event.VillageEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.StoryConfig;
import ru.adept.story.journal.Journal;
import ru.adept.story.net.StoryNetwork;

import java.util.List;
import java.util.Map;

/**
 * Реплики жителей и старейшин, когда NPC замечает игрока: при низкой угрозе — мирные, при высокой — страх
 * и враждебность. Ядро шлёт событие не чаще раза в 30 секунд от одного NPC; здесь — общая пауза между
 * репликами всех жителей. Ключи: {@code adept_story.village.<роль>.<low|high>.<номер>}.
 * Попутно событие уходит в журнал как {@code npc_met} (первая встреча с беженцем и т. п.).
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class VillageTalk {
    public static final String KEY = AdeptStory.MOD_ID + ".village.";
    public static final String SPEAKER = AdeptStory.MOD_ID + ".speaker.";

    /** Сколько строк у роли при низкой и высокой угрозе. Ополченцы и беженцы — со своими событиями позже. */
    public static final Map<String, Integer> LINES = Map.of(
            "villager.low", 4, "villager.high", 5,
            "elder.low", 2, "elder.high", 2);

    private static long until;

    private VillageTalk() {
    }

    @SubscribeEvent
    public static void onNoticed(VillageEvent.NpcNoticedPlayer event) {
        MinecraftServer server = event.getServer();
        Journal.fire(server, "npc_met", Map.of("npc", event.getRole()));
        String key = line(server, event.getRole(), event.getThreat());
        if (key != null && event.getPlayer() instanceof ServerPlayer player) {
            StoryNetwork.sendSubtitle(player, SPEAKER + event.getRole(), List.of(key));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }

    public static void reset() {
        until = 0;
    }

    /** Строка жителя с этой ролью при этой угрозе; ставит общую паузу. {@code null} — молчит. */
    public static String line(MinecraftServer server, String role, int threat) {
        String group = role + "." + mood(threat);
        int count = LINES.getOrDefault(group, 0);
        long now = server.getTickCount();
        if (count == 0 || now < until) {
            return null;
        }
        until = now + StoryConfig.VILLAGER_COOLDOWN.get() * 20L;
        return KEY + group + "." + (1 + server.overworld().getRandom().nextInt(count));
    }

    public static String mood(int threat) {
        return threat >= StoryConfig.HIGH_THREAT.get() ? "high" : "low";
    }
}
