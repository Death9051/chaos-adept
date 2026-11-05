package ru.adept.core.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.RaidEvent;
import ru.adept.core.base.ReturnPoint;
import ru.adept.core.camp.ExtractionManager;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.hero.HeroEntity;
import ru.adept.core.hero.HeroSpawner;
import ru.adept.core.hero.HeroSquads;
import ru.adept.core.registry.ModRegistries;
import ru.adept.core.threat.ThreatLevel;
import ru.adept.core.threat.ThreatManager;
import ru.adept.core.village.VillageRecord;
import ru.adept.core.village.VillageRegistry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Набеги («Мобы», «Герои и набеги»): проверка при уходе с узла и раз в 10 минут в пути, шанс по ступени угрозы,
 * не чаще раза в 15–20 минут, не во время волн, не на базе и не у лагеря. За минуту — рог и метка следопыта.
 * Если игрок ушёл на базу, отряд ждёт у точки выхода около 5 минут, потом уходит, а угроза растёт.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class RaidManager {
    /** Состояние набегов одного игрока. */
    public static final class State {
        public long nextAllowed;
        public long nextCheck;
        public boolean wasNearCamp;
        /** Рог прозвучал: в этот момент появится отряд. 0 — набега нет. */
        public long spawnAt;
        public ThreatLevel pendingLevel = ThreatLevel.CALM;
        public UUID squad;
        public long waitUntil;
        public String villageName = "";
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    static {
        HeroSquads.onSquadEnd(RaidManager::onSquadEnd);
    }

    private RaidManager() {
    }

    public static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    /** Почему набег сейчас невозможен; пусто — возможен. */
    public static Optional<String> blocker(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        State st = state(player);
        long now = server.overworld().getGameTime();
        if (ThreatManager.level(server) == ThreatLevel.CALM) {
            return Optional.of("угроза ниже порога");
        }
        if (st.spawnAt > 0 || st.squad != null) {
            return Optional.of("набег уже идёт");
        }
        if (now < st.nextAllowed) {
            return Optional.of("рано, до следующего набега " + (st.nextAllowed - now) / 20 + " с");
        }
        if (player.level() != server.overworld()) {
            return Optional.of("игрок не в обычном мире");
        }
        if (nearCamp(player)) {
            return Optional.of("игрок у лагеря");
        }
        if (ExtractionManager.session(server).map(s -> s.waveActive).orElse(false)) {
            return Optional.of("идёт волна");
        }
        BlockPos at = player.blockPosition();
        int area = BalanceConfig.RAID_ELDER_DELAY_RADIUS.get();
        for (VillageRecord v : VillageRegistry.get(server).all()) {
            if (v.raidDelayUntil > now && v.center().distSqr(at) < (double) area * area) {
                return Optional.of("старейшина «" + v.name + "» убит, набег отложен");
            }
        }
        return Optional.empty();
    }

    private static boolean nearCamp(ServerPlayer player) {
        int r = BalanceConfig.EXTRACTION_PAUSE_DISTANCE.get();
        return player.level() == player.getServer().overworld() && ExtractionManager.session(player.getServer())
                .map(s -> player.distanceToSqr(s.node.getCenter()) <= (double) r * r)
                .orElse(false);
    }

    /** Шанс набега при проверке для ступени угрозы. */
    public static double chance(ThreatLevel level) {
        return switch (level) {
            case CALM -> 0.0;
            case SCOUTS -> BalanceConfig.RAID_CHANCE_SCOUTS.get();
            case SQUADS -> BalanceConfig.RAID_CHANCE_SQUADS.get();
            case WARBANDS -> BalanceConfig.RAID_CHANCE_WARBANDS.get();
        };
    }

    /** Проверка набега: если можно и выпал шанс — рог. Возвращает, прозвучал ли рог. */
    public static boolean check(ServerPlayer player, boolean force) {
        if (blocker(player).isPresent()) {
            return false;
        }
        ThreatLevel level = ThreatManager.level(player.getServer());
        if (!force && player.getRandom().nextDouble() >= chance(level)) {
            return false;
        }
        horn(player, level);
        return true;
    }

    /** Рог: через минуту появится отряд; следопыт заранее метит игрока. */
    public static void horn(ServerPlayer player, ThreatLevel level) {
        State st = state(player);
        MinecraftServer server = player.getServer();
        st.spawnAt = server.overworld().getGameTime() + BalanceConfig.RAID_HORN_LEAD_TICKS.get();
        st.pendingLevel = level;
        st.villageName = senderVillage(server, player.blockPosition()).map(v -> v.name).orElse("");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN, SoundSource.HOSTILE, 64.0f, 1.0f);
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, BalanceConfig.RAID_HORN_LEAD_TICKS.get() + 200, 0));
        AdeptEvents.post(new RaidEvent.Horn(server, player.getUUID(), level.ordinal(), st.villageName));
    }

    /** Деревня-отправитель: ближайшая живая деревня со старейшиной, иначе ближайшая живая. */
    public static Optional<VillageRecord> senderVillage(MinecraftServer server, BlockPos at) {
        var all = VillageRegistry.get(server).all().stream().filter(v -> !v.ruined && !v.destroyed).toList();
        Comparator<VillageRecord> byDist = Comparator.comparingDouble(v -> v.center().distSqr(at));
        return all.stream().filter(v -> v.elder != null).min(byDist).or(() -> all.stream().min(byDist));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State st = state(player);
            if (st.spawnAt > 0 && now >= st.spawnAt) {
                spawn(player, st);
                continue;
            }
            if (st.squad != null && st.waitUntil > 0) {
                waitStep(player, st, now);
                continue;
            }
            boolean near = nearCamp(player);
            boolean leftNode = st.wasNearCamp && !near;
            st.wasNearCamp = near;
            if (leftNode) {
                check(player, false);
            } else if (now >= st.nextCheck) {
                st.nextCheck = now + BalanceConfig.RAID_CHECK_TICKS.get();
                check(player, false);
            }
        }
    }

    private static void spawn(ServerPlayer player, State st) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.overworld();
        st.spawnAt = 0;
        long now = overworld.getGameTime();
        int min = BalanceConfig.RAID_COOLDOWN_MIN_TICKS.get();
        int max = Math.max(min, BalanceConfig.RAID_COOLDOWN_MAX_TICKS.get());
        st.nextAllowed = now + min + player.getRandom().nextInt(max - min + 1);
        boolean atBase = player.level() != overworld;
        BlockPos around = player.blockPosition();
        if (atBase) {
            ReturnPoint rp = player.getData(ModRegistries.RETURN_POINT);
            around = rp.isSet() ? BlockPos.containing(rp.x(), rp.y(), rp.z()) : overworld.getSharedSpawnPos();
        }
        VillageRecord village = senderVillage(server, around).orElse(null);
        HeroSquads.Squad squad = HeroSpawner.spawnSquad(overworld, around, atBase ? null : player, st.pendingLevel, village);
        squad.target = player.getUUID();
        st.squad = squad.id;
        st.waitUntil = atBase ? now + BalanceConfig.RAID_WAIT_TICKS.get() : 0;
        AdeptEvents.post(new RaidEvent.Started(server, player.getUUID(), st.pendingLevel.ordinal(), st.villageName,
                squad.id, squad.members.size(), atBase));
    }

    /** Отряд ждёт у точки выхода с базы: игрок вернулся — бой, время вышло — отряд уходит, угроза растёт. */
    private static void waitStep(ServerPlayer player, State st, long now) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.overworld();
        HeroSquads squads = HeroSquads.get(server);
        Optional<HeroSquads.Squad> squad = squads.byId(st.squad);
        if (squad.isEmpty()) {
            st.squad = null;
            st.waitUntil = 0;
            return;
        }
        if (player.level() == overworld) {
            st.waitUntil = 0;
            for (UUID id : squad.get().members) {
                if (overworld.getEntity(id) instanceof HeroEntity hero) {
                    hero.setTarget(player);
                }
            }
            return;
        }
        if (now >= st.waitUntil) {
            ThreatManager.apply(server, ThreatManager.Reason.RAID_WAITED);
            String village = st.villageName;
            int level = st.pendingLevel.ordinal();
            st.squad = null;
            st.waitUntil = 0;
            squads.disband(overworld, squad.get());
            AdeptEvents.post(new RaidEvent.Ended(server, player.getUUID(), level, village, false));
        }
    }

    /** Отряд кончился (погиб, отступил или распущен): набег игрока закончен. */
    private static void onSquadEnd(HeroSquads.Squad squad, boolean defeated) {
        for (Map.Entry<UUID, State> e : STATES.entrySet()) {
            State st = e.getValue();
            if (squad.id.equals(st.squad)) {
                st.squad = null;
                st.waitUntil = 0;
                if (defeated) {
                    var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        AdeptEvents.post(new RaidEvent.Ended(server, e.getKey(), st.pendingLevel.ordinal(), st.villageName, true));
                    }
                }
            }
        }
    }
}
