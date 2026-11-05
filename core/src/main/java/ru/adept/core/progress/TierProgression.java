package ru.adept.core.progress;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.adept.core.AdeptCore;
import ru.adept.core.api.WorldProgress;
import ru.adept.core.api.event.NodeEvent;
import ru.adept.core.api.event.WorldProgressEvent;
import ru.adept.core.config.BalanceConfig;

/**
 * Вехи по уровням узлов: зачищенные узлы двигают мир к следующему уровню,
 * а смена уровня поднимает глобальную сложность (Improved Mobs, если загружен).
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID)
public final class TierProgression {
    public static final String IMPROVED_MOBS_ID = "improvedmobs";

    private TierProgression() {
    }

    @SubscribeEvent
    public static void onCleansed(NodeEvent.Cleansed event) {
        WorldProgress.get(event.getServer()).recordCleansed(event.getServer(), event.getLevel());
    }

    @SubscribeEvent
    public static void onTierChanged(WorldProgressEvent.NodeTierChanged event) {
        applyDifficulty(event.getServer(), event.getNewValue());
    }

    /** Сложность Improved Mobs для уровня узлов. */
    public static int difficultyFor(int tier) {
        return Math.max(0, tier - 1) * BalanceConfig.IMPROVED_MOBS_PER_TIER.get();
    }

    /** Команда Improved Mobs для уровня; пустая строка — не вызывать. */
    public static String commandFor(int tier) {
        String template = BalanceConfig.IMPROVED_MOBS_COMMAND.get().trim();
        if (template.isEmpty()) {
            return "";
        }
        return template.replace("%d", Integer.toString(difficultyFor(tier)));
    }

    private static void applyDifficulty(MinecraftServer server, int tier) {
        String command = commandFor(tier);
        if (command.isEmpty() || !ModList.get().isLoaded(IMPROVED_MOBS_ID)) {
            return;
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
        AdeptCore.LOG.info("Уровень узлов {}: сложность Improved Mobs {}", tier, difficultyFor(tier));
    }
}
