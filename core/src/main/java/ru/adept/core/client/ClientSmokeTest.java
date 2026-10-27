package ru.adept.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ru.adept.core.AdeptCore;

/**
 * Клиентский автотест (CI, clienttest). Включается только флагом -Dadept.clienttest=true
 * (запуск runClientTest), в обычной игре ничего не делает.
 *
 * Порядок: дождаться главного меню → создать новый мир → провести в нём TICKS_IN_WORLD тиков
 * → записать в лог маркер {@link #MARKER} → закрыть игру. CI считает тест пройденным,
 * если маркер есть в логе. Падение, зависание или ошибка загрузки дают провал.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    public static final String MARKER = "ADEPT_CLIENTTEST_OK";
    private static final boolean ENABLED = Boolean.getBoolean("adept.clienttest");
    private static final int TICKS_IN_WORLD = 200;
    private static final long SEED = 20261027L;

    private enum Stage { WAIT_TITLE, CREATING, IN_WORLD, DONE }

    private static Stage stage = Stage.WAIT_TITLE;
    private static int ticks;

    private ClientSmokeTest() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (stage) {
            case WAIT_TITLE -> {
                if (mc.screen instanceof TitleScreen) {
                    AdeptCore.LOG.info("[clienttest] главное меню открыто, создаю мир");
                    createWorld(mc);
                    stage = Stage.CREATING;
                }
            }
            case CREATING -> {
                if (mc.level != null && mc.player != null) {
                    AdeptCore.LOG.info("[clienttest] игрок в мире");
                    ticks = 0;
                    stage = Stage.IN_WORLD;
                }
            }
            case IN_WORLD -> {
                if (++ticks >= TICKS_IN_WORLD) {
                    var server = mc.getSingleplayerServer();
                    AdeptCore.LOG.info("[clienttest] измерение базы: {}", server != null
                            && server.getLevel(ru.adept.core.base.BaseTravel.BASE) != null ? "ADEPT_BASE_DIM_OK" : "нет");
                    AdeptCore.LOG.info("[clienttest] {}", MARKER);
                    stage = Stage.DONE;
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    private static void createWorld(Minecraft mc) {
        String name = "clienttest-" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(SEED, true, false);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, options,
                registries -> WorldPresets.createNormalWorldDimensions(registries), new TitleScreen());
    }
}
