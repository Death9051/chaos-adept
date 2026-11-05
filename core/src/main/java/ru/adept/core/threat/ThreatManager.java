package ru.adept.core.threat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.BiomeAbsorbedEvent;
import ru.adept.core.api.event.WorldProgressEvent;
import ru.adept.core.camp.ExtractionManager;
import ru.adept.core.config.BalanceConfig;

/**
 * Угроза мира 0–100 по правилам «Мобов»: растёт за поглощённые биомы, разрушенные деревни,
 * убитых старейшин и жителей; падает со временем и после разгромленного набега.
 * Строка угрозы показывается отдельной полосой у узла с развёрнутым лагерем.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class ThreatManager {
    /** Почему изменилась угроза (для журнала и отладки). */
    public enum Reason { BIOME_ABSORBED, VILLAGE_DESTROYED, ELDER_KILLED, VILLAGER_KILLED, RAID_DEFEATED, RAID_WAITED, DECAY, COMMAND }

    private static ServerBossEvent bar;

    private ThreatManager() {
    }

    public static int threat(MinecraftServer server) {
        return WorldProgress.get(server).threat();
    }

    public static ThreatLevel level(MinecraftServer server) {
        return ThreatLevel.of(threat(server));
    }

    /** Изменить угрозу на {@code delta} (может быть отрицательной), с ограничением 0–100. */
    public static void add(MinecraftServer server, int delta, Reason reason) {
        if (delta == 0) {
            return;
        }
        WorldProgress progress = WorldProgress.get(server);
        int before = progress.threat();
        ThreatLevel oldLevel = ThreatLevel.of(before);
        progress.setThreat(server, before + delta);
        int after = progress.threat();
        ThreatLevel newLevel = ThreatLevel.of(after);
        if (after != before) {
            AdeptCore.LOG.debug("Угроза {} → {} ({})", before, after, reason);
        }
        if (oldLevel != newLevel) {
            AdeptEvents.post(new WorldProgressEvent.ThreatLevelChanged(server, oldLevel.ordinal(), newLevel.ordinal()));
        }
    }

    /** Сколько добавляет событие по конфигу. */
    public static int amountFor(Reason reason) {
        return switch (reason) {
            case BIOME_ABSORBED -> BalanceConfig.THREAT_BIOME_ABSORBED.get();
            case VILLAGE_DESTROYED -> BalanceConfig.THREAT_VILLAGE_DESTROYED.get();
            case ELDER_KILLED -> BalanceConfig.THREAT_ELDER_KILLED.get();
            case VILLAGER_KILLED -> BalanceConfig.THREAT_VILLAGER_KILLED.get();
            case RAID_DEFEATED -> -BalanceConfig.THREAT_RAID_DEFEATED.get();
            case RAID_WAITED -> BalanceConfig.THREAT_RAID_WAITED.get();
            case DECAY -> -BalanceConfig.THREAT_DECAY_AMOUNT.get();
            case COMMAND -> 0;
        };
    }

    public static void apply(MinecraftServer server, Reason reason) {
        add(server, amountFor(reason), reason);
    }

    @SubscribeEvent
    public static void onBiomeAbsorbed(BiomeAbsorbedEvent event) {
        apply(event.getServer(), Reason.BIOME_ABSORBED);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int period = BalanceConfig.THREAT_DECAY_TICKS.get();
        if (period > 0 && server.getTickCount() % period == 0) {
            apply(server, Reason.DECAY);
        }
        if (server.getTickCount() % 20 == 0) {
            updateBar(server);
        }
    }

    /** Полоса угрозы рядом с узлом, где развёрнут лагерь (та же зона, что и полоса выкачки). */
    private static void updateBar(MinecraftServer server) {
        if (bar == null) {
            bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
        }
        int threat = threat(server);
        ThreatLevel level = ThreatLevel.of(threat);
        bar.setName(Component.translatable("adept_core.bar.threat", threat,
                Component.translatable("adept_core.threat." + level.id())));
        bar.setColor(switch (level) {
            case CALM -> BossEvent.BossBarColor.WHITE;
            case SCOUTS -> BossEvent.BossBarColor.YELLOW;
            case SQUADS -> BossEvent.BossBarColor.PINK;
            case WARBANDS -> BossEvent.BossBarColor.RED;
        });
        bar.setProgress(threat / 100.0f);
        ServerPlayer near = ExtractionManager.session(server)
                .map(s -> server.getPlayerList().getPlayer(s.player))
                .filter(p -> p.level() == server.overworld() && ExtractionManager.session(server)
                        .map(s -> p.distanceToSqr(s.node.getCenter()) <= Math.pow(BalanceConfig.EXTRACTION_PAUSE_DISTANCE.get(), 2))
                        .orElse(false))
                .orElse(null);
        for (ServerPlayer p : bar.getPlayers().stream().toList()) {
            if (p != near) {
                bar.removePlayer(p);
            }
        }
        if (near != null) {
            bar.addPlayer(near);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (bar != null) {
            bar.removeAllPlayers();
        }
        bar = null;
    }
}
