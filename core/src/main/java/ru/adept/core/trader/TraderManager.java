package ru.adept.core.trader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldFlags;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.TraderEvent;
import ru.adept.core.camp.ExtractionData;
import ru.adept.core.camp.ExtractionSession;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.fx.ChaosParticles;
import ru.adept.core.node.NodeRecord;
import ru.adept.core.node.NodeRegistry;
import ru.adept.core.registry.ModEntities;

import java.util.Optional;
import java.util.UUID;

/**
 * Ритуал вызова: через стационарный переход лагеря после зачистки узла, один раз на узел.
 * В арке раскрывается разрыв, затем выходит торговец.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class TraderManager {
    public static final String FLAG_FIRST_RITUAL = AdeptCore.MOD_ID + ":first_ritual";
    public static final String FLAG_ACCOUNT = AdeptCore.MOD_ID + ":trader_account";

    private static Pending pending;

    private record Pending(BlockPos arch, UUID player, int total, int[] left) {
    }

    private TraderManager() {
    }

    /** Можно ли сейчас вызвать торговца через переход. */
    public static boolean canCall(MinecraftServer server) {
        return ExtractionData.get(server).session().filter(s -> s.done && !s.traderCalled).isPresent() && pending == null;
    }

    public static boolean call(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session();
        if (opt.isEmpty() || !opt.get().done) {
            player.displayClientMessage(Component.translatable("adept_core.trader.not_cleansed"), true);
            return false;
        }
        if (opt.get().traderCalled || pending != null) {
            player.displayClientMessage(Component.translatable("adept_core.trader.already"), true);
            return false;
        }
        opt.get().traderCalled = true;
        data.touch();
        startRitual(player, opt.get().transition);
        return true;
    }

    /** Начать ритуал у арки (без проверок — для отладочной команды). */
    public static void startRitual(ServerPlayer player, BlockPos arch) {
        int ticks = BalanceConfig.TRADER_ARRIVAL_TICKS.get();
        pending = new Pending(arch, player.getUUID(), Math.max(1, ticks), new int[]{ticks});
        AdeptEvents.post(new TraderEvent.CallStarted(player));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (pending == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        Vec3 center = Vec3.atBottomCenterOf(pending.arch().above()).add(0, 1.0, 0);
        int left = --pending.left()[0];
        if (left % 2 == 0) {
            ChaosParticles.rift(level, center, 1.2, 1.0 - (double) Math.max(0, left) / pending.total());
        }
        if (left > 0) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(pending.player());
        BlockPos spot = spawnSpot(level, pending.arch());
        pending = null;
        TraderEntity trader = ModEntities.TRADER.get().create(level);
        if (trader == null || player == null) {
            return;
        }
        trader.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        EventHooks.finalizeMobSpawn(trader, level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
        level.addFreshEntity(trader);
        ChaosParticles.arc(level, center, trader.position().add(0, 1.0, 0));

        WorldFlags flags = WorldFlags.get(server);
        boolean first = !flags.has(FLAG_FIRST_RITUAL);
        if (first) {
            flags.set(FLAG_FIRST_RITUAL, "1");
        }
        AdeptEvents.post(new TraderEvent.Arrived(player, first));
        long cleansed = NodeRegistry.get(server).all().stream().filter(n -> n.state() == NodeRecord.State.CLEANSED).count();
        if (!flags.has(FLAG_ACCOUNT) && cleansed >= BalanceConfig.TRADER_ACCOUNT_AFTER_NODES.get()) {
            flags.set(FLAG_ACCOUNT, "1");
            AdeptEvents.post(new TraderEvent.AccountOpened(player));
        }
    }

    private static BlockPos spawnSpot(ServerLevel level, BlockPos arch) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = arch.relative(d);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                return p;
            }
        }
        return arch.above();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        pending = null;
    }
}
