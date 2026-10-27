package ru.adept.core;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import ru.adept.core.command.AdeptCommands;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModRegistries;

/**
 * «Ядро»: все механики сборки. Другие моды (прежде всего «Сюжет») работают
 * с ядром через пакет {@code ru.adept.core.api} и события ядра ({@code ru.adept.core.api.event}).
 */
@Mod(AdeptCore.MOD_ID)
public final class AdeptCore {
    public static final String MOD_ID = "adept_core";
    public static final Logger LOG = LogUtils.getLogger();

    public AdeptCore(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        // Числа баланса: config/adept_core-common.toml.
        container.registerConfig(ModConfig.Type.COMMON, BalanceConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(AdeptCommands::register);
        LOG.info("Ядро загружено, версия {}", container.getModInfo().getVersion());
    }
}
