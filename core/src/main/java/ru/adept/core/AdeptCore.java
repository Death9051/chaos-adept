package ru.adept.core;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * «Ядро»: все механики сборки. Другие моды (прежде всего «Сюжет») работают
 * с ядром через пакет {@code ru.adept.core.api} и события ядра.
 */
@Mod(AdeptCore.MOD_ID)
public final class AdeptCore {
    public static final String MOD_ID = "adept_core";
    public static final Logger LOG = LogUtils.getLogger();

    public AdeptCore(IEventBus modBus, ModContainer container) {
        LOG.info("Ядро загружено, версия {}", container.getModInfo().getVersion());
    }
}
