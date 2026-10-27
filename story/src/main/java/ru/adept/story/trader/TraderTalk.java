package ru.adept.story.trader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.api.event.TraderEvent;
import ru.adept.story.AdeptStory;
import ru.adept.story.journal.Journal;
import ru.adept.story.journal.JournalData;
import ru.adept.story.net.StoryNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Реплики торговца Альянса субтитрами. Первый ритуал — свои реплики и запись после ухода;
 * первый ритуал на новом уровне узлов — веха (условия в журнал, на странице «Торговец»);
 * иначе случайное приветствие. Ветки различаются только там, где это есть в текстах.
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class TraderTalk {
    public static final String SPEAKER = AdeptStory.MOD_ID + ".speaker.trader";
    private static final String KEY = AdeptStory.MOD_ID + ".trader.";
    private static final int GREETINGS = 5;

    private TraderTalk() {
    }

    @SubscribeEvent
    public static void onArrived(TraderEvent.Arrived event) {
        say(event.getPlayer(), arrivalLines(event.getServer(), event.isFirstRitual()));
    }

    @SubscribeEvent
    public static void onAccountOpened(TraderEvent.AccountOpened event) {
        say(event.getPlayer(), List.of(KEY + "account"));
        Journal.fire(event.getServer(), "trader_account_opened");
    }

    @SubscribeEvent
    public static void onDeal(TraderEvent.Deal event) {
        switch (event.getCategory()) {
            case "trophy" -> say(event.getPlayer(), List.of(KEY + "trophy"));
            case "pillar_material" -> say(event.getPlayer(), List.of(KEY + "pillar_material"));
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onLeft(TraderEvent.Left event) {
        MinecraftServer server = event.getServer();
        say(event.getPlayer(), List.of(KEY + "farewell"));
        JournalData data = JournalData.get(server);
        if (data.ritualPending) {
            data.ritualPending = false;
            data.setDirty();
            Journal.fire(server, "ritual_done", Map.of("first", "game"));
        }
    }

    /**
     * Что торговец говорит при появлении. Состояние вехи и первого ритуала меняется здесь,
     * поэтому метод вызывается ровно один раз на появление.
     */
    public static List<String> arrivalLines(MinecraftServer server, boolean firstRitual) {
        JournalData data = JournalData.get(server);
        String branch = Journal.branch(server);
        int tier = WorldProgress.get(server).nodeTier();
        List<String> lines = new ArrayList<>();

        if (firstRitual) {
            int count = branch.equals("streamer") ? 2 : 3;
            for (int i = 1; i <= count; i++) {
                lines.add(KEY + "first_ritual." + branch + "." + i);
            }
            data.ritualPending = true;
            // Веха текущего уровня уже не нужна: первый ритуал её заменяет.
            data.traderTier = Math.max(data.traderTier, tier);
            data.setDirty();
            return lines;
        }

        if (tier > data.traderTier) {
            data.traderTier = tier;
            data.setDirty();
            lines.add(KEY + "level_" + tier);
            if (branch.equals("streamer") && (tier == 3 || tier == 5)) {
                lines.add(KEY + "level_" + tier + ".lord");
            }
            Journal.fire(server, "node_level_reached", Map.of("level", Integer.toString(tier)));
            return lines;
        }

        lines.add(KEY + "greet." + (1 + server.overworld().getRandom().nextInt(GREETINGS)));
        return lines;
    }

    private static void say(Player player, List<String> lines) {
        if (player instanceof ServerPlayer serverPlayer) {
            StoryNetwork.sendSubtitle(serverPlayer, SPEAKER, lines);
        }
    }
}
