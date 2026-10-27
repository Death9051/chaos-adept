package ru.adept.story;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * «Сюжет»: история, тексты, катсцены. Механик своих не держит — слушает события ядра
 * и пишет свои флаги в мир через {@link ru.adept.core.api.WorldFlags}.
 */
@Mod(AdeptStory.MOD_ID)
public final class AdeptStory {
    public static final String MOD_ID = "adept_story";
    public static final Logger LOG = LogUtils.getLogger();

    public AdeptStory(IEventBus modBus, ModContainer container) {
        // Конфиг лежит в config/adept_story-common.toml. Экрана настроек нет намеренно:
        // ветку сюжета нельзя переключить из меню.
        container.registerConfig(ModConfig.Type.COMMON, StoryConfig.SPEC);
        LOG.info("Сюжет загружен, версия {}", container.getModInfo().getVersion());
    }
}
