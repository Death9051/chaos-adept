package ru.adept.story;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ru.adept.core.api.WorldFlags;

import java.util.Locale;

/**
 * Фиксация ветки сюжета в мире. При первом запуске мира ветка берётся из конфига и
 * записывается во флаги мира; дальше читается только из мира, смена конфига на него не влияет.
 */
@EventBusSubscriber(modid = AdeptStory.MOD_ID)
public final class StoryBranch {
    public static final String FLAG = AdeptStory.MOD_ID + ":branch";

    private StoryBranch() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        WorldFlags flags = WorldFlags.get(event.getServer());
        if (!flags.has(FLAG)) {
            StoryConfig.Branch branch = StoryConfig.BRANCH.get();
            flags.set(FLAG, branch.name().toLowerCase(Locale.ROOT));
            AdeptStory.LOG.info("Ветка сюжета для этого мира: {}", branch);
        }
    }

    /** Ветка текущего мира. Если флага почему-то нет, считаем ветку публичной. */
    public static StoryConfig.Branch of(MinecraftServer server) {
        return WorldFlags.get(server).get(FLAG)
                .map(value -> StoryConfig.Branch.valueOf(value.toUpperCase(Locale.ROOT)))
                .orElse(StoryConfig.Branch.PUBLIC);
    }
}
