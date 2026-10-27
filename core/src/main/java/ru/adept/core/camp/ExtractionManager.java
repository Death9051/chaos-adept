package ru.adept.core.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.event.AdeptEvents;
import ru.adept.core.api.event.NodeEvent;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.mob.Possession;
import ru.adept.core.node.NodeRecord;
import ru.adept.core.node.NodeRegistry;
import ru.adept.core.registry.ModBlocks;
import ru.adept.core.registry.ModItems;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Лагерь и выкачка: развёртывание, прогресс, волны по порогам, смерть игрока, установка
 * выкачки (прочность, ремонт), сворачивание. Прогресс показывается полосой босса.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class ExtractionManager {
    private static ServerBossEvent bar;

    private ExtractionManager() {
    }

    public static Optional<ExtractionSession> session(MinecraftServer server) {
        return ExtractionData.get(server).session();
    }

    // ---- развёртывание и сворачивание ----

    public static boolean deploy(ServerPlayer player, BlockPos clicked, Direction facing) {
        MinecraftServer server = player.getServer();
        ServerLevel level = player.serverLevel();
        if (server == null || level != server.overworld()) {
            player.displayClientMessage(Component.translatable("adept_core.camp.wrong_world"), true);
            return false;
        }
        ExtractionData data = ExtractionData.get(server);
        if (data.session().isPresent()) {
            player.displayClientMessage(Component.translatable("adept_core.camp.already"), true);
            return false;
        }
        int radius = BalanceConfig.CAMP_RADIUS.get();
        Optional<NodeRecord> node = NodeRegistry.get(server).all().stream()
                .filter(n -> n.state() == NodeRecord.State.DORMANT)
                .filter(n -> n.pos().distSqr(clicked) <= (double) radius * radius)
                .min(Comparator.comparingDouble(n -> n.pos().distSqr(clicked)));
        if (node.isEmpty()) {
            player.displayClientMessage(Component.translatable("adept_core.camp.no_node", radius), true);
            return false;
        }
        BlockPos extractor = clicked.above();
        BlockPos transition = extractor.relative(facing.getClockWise(), 2);
        if (!level.getBlockState(transition).canBeReplaced()) {
            transition = extractor.relative(facing.getCounterClockWise(), 2);
        }
        if (!level.getBlockState(extractor).canBeReplaced() || !level.getBlockState(transition).canBeReplaced()) {
            player.displayClientMessage(Component.translatable("adept_core.camp.no_space"), true);
            return false;
        }
        level.setBlockAndUpdate(extractor, ModBlocks.EXTRACTOR.get().defaultBlockState());
        level.setBlockAndUpdate(transition, ModBlocks.TRANSITION.get().defaultBlockState());

        NodeRecord n = node.get();
        ExtractionSession s = new ExtractionSession();
        s.node = n.pos();
        s.extractor = extractor;
        s.transition = transition;
        s.player = player.getUUID();
        s.level = n.level();
        s.total = BalanceConfig.extractionTicks(n.level());
        s.waves = BalanceConfig.waveCount(n.level());
        s.extractorHealth = BalanceConfig.EXTRACTOR_HEALTH.get();
        data.set(s);
        NodeRegistry.get(server).update(n.withState(NodeRecord.State.EXTRACTING));
        player.displayClientMessage(Component.translatable("adept_core.camp.deployed", n.level(), s.waves), false);
        AdeptEvents.post(new NodeEvent.ExtractionStarted(server, s.node, s.level));
        return true;
    }

    /** Свернуть лагерь. До зачистки узла прогресс теряется, узел снова спит. */
    public static void fold(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session();
        if (opt.isEmpty()) {
            return;
        }
        ExtractionSession s = opt.get();
        ServerLevel level = server.overworld();
        clearWave(level, s);
        if (level.getBlockState(s.extractor).is(ModBlocks.EXTRACTOR.get())) {
            level.removeBlock(s.extractor, false);
        }
        if (level.getBlockState(s.transition).is(ModBlocks.TRANSITION.get())) {
            level.removeBlock(s.transition, false);
        }
        if (!s.done) {
            NodeRegistry registry = NodeRegistry.get(server);
            registry.at(s.node).ifPresent(n -> registry.update(n.withState(NodeRecord.State.DORMANT)));
            player.displayClientMessage(Component.translatable("adept_core.camp.folded_early"), false);
        } else {
            player.displayClientMessage(Component.translatable("adept_core.camp.folded"), true);
        }
        ItemStack camp = new ItemStack(ModItems.CAMP.get());
        if (!player.getInventory().add(camp)) {
            player.drop(camp, false);
        }
        data.set(null);
        hideBar();
    }

    public static void showStatus(ServerPlayer player) {
        session(player.getServer()).ifPresent(s -> player.displayClientMessage(Component.translatable(
                "adept_core.camp.status", s.level, s.percent(), s.wavesCleared, s.waves, Math.max(0, s.extractorHealth),
                BalanceConfig.EXTRACTOR_HEALTH.get()), true));
    }

    // ---- установка выкачки ----

    public static boolean isRepairItem(ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(BalanceConfig.EXTRACTOR_REPAIR_ITEM.get());
        return id != null && stack.is(BuiltInRegistries.ITEM.get(id));
    }

    public static void repair(ServerPlayer player, ItemStack stack) {
        ExtractionData data = ExtractionData.get(player.getServer());
        data.session().ifPresent(s -> {
            int max = BalanceConfig.EXTRACTOR_HEALTH.get();
            if (s.extractorHealth >= max) {
                return;
            }
            s.extractorHealth = Math.min(max, s.extractorHealth + BalanceConfig.EXTRACTOR_REPAIR_AMOUNT.get());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            data.touch();
            showStatus(player);
        });
    }

    public static void damageExtractor(MinecraftServer server, BlockPos pos, int amount) {
        ExtractionData data = ExtractionData.get(server);
        data.session().filter(s -> s.extractor.equals(pos) && !s.done).ifPresent(s -> {
            boolean wasWorking = s.extractorHealth > 0;
            s.extractorHealth = Math.max(0, s.extractorHealth - amount);
            data.touch();
            if (wasWorking && s.extractorHealth == 0) {
                ServerPlayer player = server.getPlayerList().getPlayer(s.player);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("adept_core.camp.broken"), false);
                }
                AdeptEvents.post(new NodeEvent.ExtractorBroken(server, s.node, s.level));
            }
        });
    }

    // ---- волны ----

    /** Начать следующую волну (по порогу или отладочной командой). */
    public static boolean startNextWave(MinecraftServer server) {
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session();
        if (opt.isEmpty() || opt.get().waveActive || opt.get().done || opt.get().wavesCleared >= opt.get().waves) {
            return false;
        }
        ExtractionSession s = opt.get();
        ServerLevel level = server.overworld();
        ServerPlayer player = server.getPlayerList().getPlayer(s.player);
        int wave = s.wavesCleared + 1;
        s.waveMobs.clear();
        s.waveMobs.addAll(WaveSpawner.spawn(level, s, wave, player));
        s.waveActive = true;
        data.touch();
        if (player != null) {
            player.displayClientMessage(Component.translatable(wave >= s.waves ? "adept_core.wave.final" : "adept_core.wave.start",
                    wave, s.waves), true);
        }
        AdeptEvents.post(new NodeEvent.WaveStarted(server, s.node, s.level, wave, s.waves));
        return true;
    }

    /** Убрать мобов текущей волны (без засчитывания). */
    public static void clearWave(ServerLevel level, ExtractionSession s) {
        for (UUID id : s.waveMobs) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        s.waveMobs.clear();
        s.waveActive = false;
    }

    /** Отладка: засчитать текущую волну отбитой. */
    public static boolean forceClearWave(MinecraftServer server) {
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session().filter(s -> s.waveActive);
        if (opt.isEmpty()) {
            return false;
        }
        ExtractionSession s = opt.get();
        for (UUID id : s.waveMobs) {
            Entity e = server.overworld().getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        s.waveMobs.clear();
        data.touch();
        return true;
    }

    /** Отладка: задать прогресс в процентах. */
    public static boolean setProgress(MinecraftServer server, int percent) {
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session().filter(s -> !s.done);
        if (opt.isEmpty()) {
            return false;
        }
        ExtractionSession s = opt.get();
        s.progress = s.total * Math.max(0, Math.min(100, percent)) / 100.0;
        data.touch();
        return true;
    }

    private static void waveCleared(MinecraftServer server, ExtractionData data, ExtractionSession s) {
        s.waveActive = false;
        s.wavesCleared++;
        data.touch();
        ServerPlayer player = server.getPlayerList().getPlayer(s.player);
        if (player != null && s.wavesCleared < s.waves) {
            player.displayClientMessage(Component.translatable("adept_core.wave.cleared", s.wavesCleared, s.waves), true);
        }
        AdeptEvents.post(new NodeEvent.WaveCleared(server, s.node, s.level, s.wavesCleared, s.waves));
        if (s.wavesCleared >= s.waves) {
            finish(server, data, s);
        }
    }

    private static void finish(MinecraftServer server, ExtractionData data, ExtractionSession s) {
        s.done = true;
        s.progress = s.total;
        data.touch();
        NodeRegistry registry = NodeRegistry.get(server);
        registry.at(s.node).ifPresent(n -> registry.update(n.withState(NodeRecord.State.CLEANSED)));
        ServerPlayer player = server.getPlayerList().getPlayer(s.player);
        if (player != null) {
            player.displayClientMessage(Component.translatable("adept_core.camp.cleansed"), false);
        }
        hideBar();
        AdeptEvents.post(new NodeEvent.Cleansed(server, s.node, s.level));
    }

    // ---- тик ----

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ExtractionData data = ExtractionData.get(server);
        Optional<ExtractionSession> opt = data.session();
        if (opt.isEmpty() || opt.get().done) {
            return;
        }
        ExtractionSession s = opt.get();
        ServerLevel level = server.overworld();
        if (!level.isLoaded(s.node)) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(s.player);
        int pause = BalanceConfig.EXTRACTION_PAUSE_DISTANCE.get();
        boolean near = player != null && player.level() == level && player.isAlive()
                && player.distanceToSqr(s.node.getCenter()) <= (double) pause * pause;

        if (near && s.waveActive) {
            s.waveMobs.removeIf(id -> {
                Entity e = level.getEntity(id);
                return e == null || !e.isAlive();
            });
            if (server.getTickCount() % 10 == 0) {
                for (UUID id : s.waveMobs) {
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        Possession.aura(level, e);
                    }
                }
            }
            if (s.waveMobs.isEmpty()) {
                waveCleared(server, data, s);
                if (s.done) {
                    return;
                }
            }
        }

        boolean working = near && s.extractorHealth > 0;
        if (working) {
            double speed = s.waveActive ? BalanceConfig.EXTRACTION_WAVE_SPEED.get() : 1.0;
            s.progress = Math.min(s.total, s.progress + speed);
            if (!s.waveActive && s.wavesCleared < s.waves
                    && s.progress >= ExtractionMath.threshold(s.total, s.wavesCleared + 1, s.waves)) {
                startNextWave(server);
            }
            if (server.getTickCount() % 100 == 0) {
                data.touch();
            }
        }
        if (server.getTickCount() % 10 == 0) {
            updateBar(server, s, near ? player : null, working);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        ExtractionData data = ExtractionData.get(server);
        data.session().filter(s -> !s.done && s.player.equals(player.getUUID())).ifPresent(s -> {
            int percent = BalanceConfig.EXTRACTION_DEATH_ROLLBACK_PERCENT.get();
            if (player.distanceToSqr(s.node.getCenter()) > Math.pow(BalanceConfig.EXTRACTION_PAUSE_DISTANCE.get(), 2)) {
                return;
            }
            clearWave(server.overworld(), s);
            s.progress = ExtractionMath.rollback(s.progress, s.total, percent);
            data.touch();
            AdeptEvents.post(new NodeEvent.PlayerDied(server, s.node, s.level, percent));
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        bar = null;
    }

    private static void updateBar(MinecraftServer server, ExtractionSession s, ServerPlayer near, boolean working) {
        if (bar == null) {
            bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        }
        String key = s.extractorHealth <= 0 ? "adept_core.bar.broken" : working ? "adept_core.bar.extracting" : "adept_core.bar.paused";
        bar.setName(Component.translatable(key, s.level, s.percent(), s.currentWave(), s.waves));
        bar.setProgress((float) (s.total <= 0 ? 0 : s.progress / s.total));
        for (ServerPlayer p : bar.getPlayers().stream().toList()) {
            if (p != near) {
                bar.removePlayer(p);
            }
        }
        if (near != null) {
            bar.addPlayer(near);
        }
    }

    private static void hideBar() {
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    /** Отладка: снять блоки лагеря без возврата предмета (используется тестами). */
    static void removeBlocks(ServerLevel level, ExtractionSession s) {
        level.setBlockAndUpdate(s.extractor, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(s.transition, Blocks.AIR.defaultBlockState());
    }
}
