package ru.adept.core.test;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.data.ArtifactProgress;
import ru.adept.core.registry.ModRegistries;

/** Автотесты основы ядра: конфиг баланса, прогресс мира, данные игрока, отладочные команды. */
@GameTestHolder(AdeptCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoundationGameTests {
    private FoundationGameTests() {
    }

    @GameTest(template = "empty")
    public static void balanceConfigLoaded(GameTestHelper helper) {
        // Значения по умолчанию из «Артефактов»: 10 минут на первом уровне, 4 волны на первом уровне.
        helper.assertTrue(BalanceConfig.extractionTicks(1) == BalanceConfig.EXTRACTION_MINUTES_MIN.get() * 60 * 20,
                "Длительность выкачки первого уровня не совпадает с конфигом");
        helper.assertTrue(BalanceConfig.waveCount(1) == Math.min(BalanceConfig.WAVES_BASE.get() + 1, BalanceConfig.WAVES_MAX.get()),
                "Число волн не совпадает с формулой");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldProgressClamps(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        int tier = progress.nodeTier();
        int threat = progress.threat();
        progress.setThreat(server, 150);
        helper.assertTrue(progress.threat() == 100, "Угроза не ограничена сверху");
        progress.setNodeTier(server, 0);
        helper.assertTrue(progress.nodeTier() == 1, "Уровень узлов не ограничен снизу");
        progress.setThreat(server, threat);
        progress.setNodeTier(server, tier);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void artifactProgressOnPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ArtifactProgress p = player.getData(ModRegistries.ARTIFACT_PROGRESS)
                .withChoice("melee", "blade")
                .withStep("blade/emitter/edge", 1);
        player.setData(ModRegistries.ARTIFACT_PROGRESS, p);
        ArtifactProgress read = player.getData(ModRegistries.ARTIFACT_PROGRESS);
        helper.assertTrue(read.choice("melee").filter("blade"::equals).isPresent(), "Выбор пары не сохранился");
        helper.assertTrue(read.step("blade/emitter/edge") == 1, "Ступень не сохранилась");
        helper.assertTrue(read.withStep("blade/emitter/edge", 0).steps().isEmpty(), "Нулевая ступень не удаляется");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        int threat = progress.threat();
        CommandSourceStack source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, "adept threat set 42");
        helper.assertTrue(progress.threat() == 42, "Команда /adept threat set не сработала");
        server.getCommands().performPrefixedCommand(source, "adept flag set adept_core:cmd_probe ok");
        helper.assertTrue(ru.adept.core.api.WorldFlags.get(server).get("adept_core:cmd_probe").filter("ok"::equals).isPresent(),
                "Команда /adept flag set не сработала");
        server.getCommands().performPrefixedCommand(source, "adept flag remove adept_core:cmd_probe");
        progress.setThreat(server, threat);
        helper.succeed();
    }
}
