package ru.adept.core.village;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.VillageEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModEntities;
import ru.adept.core.threat.ThreatManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Жители и деревни: замена ванильных жителей своими, привязка к деревне и выбор старейшины,
 * угроза за убитых, разрушение деревни, событие входа в руины.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class VillageManager {
    private record Pending(ServerLevel level, CompoundTag tag) {
    }

    private static final Deque<Pending> REPLACE = new ArrayDeque<>();
    private static final Map<UUID, Long> LAST_RUINS = new HashMap<>();

    private VillageManager() {
    }

    /** Ванильный житель в обычном мире заменяется своим на следующем тике (данные и сделки сохраняются). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!BalanceConfig.VILLAGE_REPLACE_VANILLA.get() || event.getEntity().getType() != EntityType.VILLAGER
                || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        Villager villager = (Villager) event.getEntity();
        REPLACE.add(new Pending(level, villager.saveWithoutId(new CompoundTag())));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int budget = 16;
        while (!REPLACE.isEmpty() && budget-- > 0) {
            Pending p = REPLACE.poll();
            ResidentEntity resident = ModEntities.RESIDENT.get().create(p.level());
            if (resident != null) {
                resident.load(p.tag());
                p.level().addFreshEntity(resident);
            }
        }
        if (server.getTickCount() % 20 == 0) {
            checkRuins(server);
        }
    }

    /** Привязать жителя к деревне; первому жителю деревни без старейшины — стать старейшиной. */
    public static void assign(ServerLevel level, ResidentEntity resident) {
        VillageRegistry registry = VillageRegistry.get(level.getServer());
        VillageRecord village = resident.villageId() != 0
                ? registry.byId(resident.villageId()).orElse(null)
                : registry.findOrRegisterAt(level, resident.blockPosition()).orElse(null);
        if (village == null) {
            return;
        }
        resident.setVillageId(village.id);
        if (village.elder == null && !village.ruined && !(resident instanceof ElderEntity)) {
            ElderEntity elder = ModEntities.ELDER.get().create(level);
            if (elder != null) {
                CompoundTag tag = resident.saveWithoutId(new CompoundTag());
                tag.remove("UUID");
                elder.load(tag);
                elder.setUUID(UUID.randomUUID());
                elder.setVillageId(village.id);
                resident.discard();
                level.addFreshEntity(elder);
                village.elder = elder.getUUID();
                village.residents.add(elder.getUUID());
                registry.changed();
                return;
            }
        }
        if (resident instanceof ElderEntity && village.elder == null) {
            village.elder = resident.getUUID();
        }
        village.residents.add(resident.getUUID());
        registry.changed();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ResidentEntity resident) || !(resident.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        VillageRegistry registry = VillageRegistry.get(server);
        boolean byPlayer = event.getSource().getEntity() instanceof Player;
        VillageRecord village = registry.byId(resident.villageId()).orElse(null);
        if (village != null) {
            village.residents.remove(resident.getUUID());
            registry.changed();
        }
        if (!byPlayer) {
            return;
        }
        if (resident instanceof ElderEntity) {
            ThreatManager.apply(server, ThreatManager.Reason.ELDER_KILLED);
            if (village != null) {
                village.elder = null;
                village.raidDelayUntil = level.getGameTime() + BalanceConfig.VILLAGE_ELDER_RAID_DELAY_TICKS.get();
                AdeptEvents.post(new VillageEvent.ElderKilled(server, village.id, village.name));
            }
        } else {
            ThreatManager.apply(server, ThreatManager.Reason.VILLAGER_KILLED);
        }
        if (village != null && village.residents.isEmpty() && !village.destroyed && !village.ruined) {
            village.destroyed = true;
            registry.changed();
            ThreatManager.apply(server, ThreatManager.Reason.VILLAGE_DESTROYED);
            AdeptEvents.post(new VillageEvent.Destroyed(server, village.id, village.name));
        }
    }

    /** Событие входа в руины — при каждом входе в границы разрушенной поглощением деревни. */
    private static void checkRuins(MinecraftServer server) {
        VillageRegistry registry = VillageRegistry.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long inside = 0;
            String name = "";
            if (player.level() == server.overworld()) {
                for (VillageRecord v : registry.all()) {
                    if (v.ruined && v.containsXZ(player.getX(), player.getZ(), 0)) {
                        inside = v.id;
                        name = v.name;
                        break;
                    }
                }
            }
            Long last = LAST_RUINS.put(player.getUUID(), inside);
            if (inside != 0 && (last == null || last != inside)) {
                AdeptEvents.post(new VillageEvent.RuinsEntered(player, inside, name));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        REPLACE.clear();
        LAST_RUINS.clear();
    }
}
