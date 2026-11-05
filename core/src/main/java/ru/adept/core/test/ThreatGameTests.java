package ru.adept.core.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.WorldProgressEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.threat.ThreatLevel;
import ru.adept.core.threat.ThreatManager;

import java.util.concurrent.atomic.AtomicInteger;

/** Автотесты угрозы: ступени, рост и спад по причинам, событие смены ступени. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThreatGameTests {
    private ThreatGameTests() {
    }

    @GameTest(template = "empty")
    public static void threatLevelsAndReasons(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        int old = ThreatManager.threat(server);
        helper.assertTrue(ThreatLevel.of(0) == ThreatLevel.CALM && ThreatLevel.of(24) == ThreatLevel.CALM
                && ThreatLevel.of(25) == ThreatLevel.SCOUTS && ThreatLevel.of(50) == ThreatLevel.SQUADS
                && ThreatLevel.of(100) == ThreatLevel.WARBANDS, "Пороги угрозы не по «Мобам»");
        AtomicInteger levelEvents = new AtomicInteger();
        Object listener = new Object() {
            @SubscribeEvent
            public void on(WorldProgressEvent.ThreatLevelChanged e) {
                levelEvents.incrementAndGet();
            }
        };
        ThreatManager.add(server, -100, ThreatManager.Reason.COMMAND);
        NeoForge.EVENT_BUS.register(listener);
        try {
            ThreatManager.apply(server, ThreatManager.Reason.BIOME_ABSORBED);
            helper.assertTrue(ThreatManager.threat(server) == BalanceConfig.THREAT_BIOME_ABSORBED.get(), "Рост за биом");
            ThreatManager.apply(server, ThreatManager.Reason.ELDER_KILLED);
            helper.assertTrue(ThreatManager.level(server) == ThreatLevel.SCOUTS, "15 + 10 — не разведка");
            ThreatManager.apply(server, ThreatManager.Reason.RAID_DEFEATED);
            helper.assertTrue(ThreatManager.threat(server) == 15, "Спад после набега");
            ThreatManager.add(server, 500, ThreatManager.Reason.COMMAND);
            helper.assertTrue(ThreatManager.threat(server) == 100, "Угроза выше 100");
            helper.assertTrue(levelEvents.get() == 3, "Событий смены ступени: " + levelEvents.get());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            ThreatManager.add(server, old - ThreatManager.threat(server), ThreatManager.Reason.COMMAND);
        }
        helper.succeed();
    }
}
