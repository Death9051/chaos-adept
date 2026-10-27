package ru.adept.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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

import java.util.List;

/**
 * Клиентский автотест (CI, clienttest). Включается только флагом -Dadept.clienttest=true
 * (запуск runClientTest), в обычной игре ничего не делает.
 *
 * Порядок: дождаться главного меню → создать новый мир → показать сцены для скриншотов
 * → провести в мире оставшиеся тики → записать в лог маркер {@link #MARKER} → закрыть игру.
 * CI считает тест пройденным, если маркер есть в логе.
 *
 * Сцены: перед каждой в лог пишется строка {@code ADEPT_SCENE <имя>}, после неё кадр держится
 * {@link #SCENE_HOLD} тиков — скрипт CI снимает скриншот по этой строке. Сцена строится командами
 * на встроенном сервере рядом с игроком: мобы без ИИ, интерфейс скрыт.
 * Длительность пребывания в мире — свойство {@code -Dadept.clienttest.ticks} (по умолчанию 200),
 * отсчёт после сцен.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    public static final String MARKER = "ADEPT_CLIENTTEST_OK";
    public static final String SCENE_PREFIX = "ADEPT_SCENE ";
    private static final boolean ENABLED = Boolean.getBoolean("adept.clienttest");
    private static final int TICKS_IN_WORLD = Integer.getInteger("adept.clienttest.ticks", 200);
    /** Тиков на подготовку сцены до строки в логе (догрузка чанков и рендера). */
    private static final int SCENE_SETUP = 30;
    /** Тиков, которые сцена держится после строки в логе. */
    private static final int SCENE_HOLD = 60;
    private static final long SEED = 20261027L;

    private enum Stage { WAIT_TITLE, CREATING, BUILD, SCENES, IN_WORLD, DONE }

    /** Сцена: имя, позиция камеры относительно площадки, поворот, показывать ли интерфейс. */
    private record Scene(String name, double x, double y, double z, float yaw, float pitch, boolean gui) {
    }

    private static final List<Scene> SCENES = List.of(
            new Scene("table", 0.5, 1.6, 1.5, 0, 25, false),
            new Scene("node_camp", 6.5, 2.0, 1.0, 0, 20, false),
            new Scene("machines", -5.5, 2.0, 1.0, 0, 20, false),
            new Scene("mobs", 0.5, 2.5, 3.0, 0, 12, false),
            new Scene("blade_hand", 0.5, 1.0, 1.5, 0, 0, true));

    private static Stage stage = Stage.WAIT_TITLE;
    private static int ticks;
    private static int sceneIndex;
    private static BlockPos origin;

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
                    stage = Stage.BUILD;
                }
            }
            case BUILD -> {
                // Дать миру прогрузиться, затем построить площадку со сценами.
                if (++ticks >= 40) {
                    origin = mc.player.blockPosition().offset(0, 20, 0);
                    build(mc.getSingleplayerServer(), origin);
                    ticks = 0;
                    sceneIndex = 0;
                    stage = Stage.SCENES;
                }
            }
            case SCENES -> {
                Scene scene = SCENES.get(sceneIndex);
                if (ticks == 0) {
                    mc.options.hideGui = !scene.gui();
                    run(mc.getSingleplayerServer(), String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f %.1f %.1f",
                            origin.getX() + scene.x(), origin.getY() + scene.y(), origin.getZ() + scene.z(), scene.yaw(), scene.pitch()));
                }
                ticks++;
                if (ticks == SCENE_SETUP) {
                    AdeptCore.LOG.info("[clienttest] {}{}", SCENE_PREFIX, scene.name());
                }
                if (ticks >= SCENE_SETUP + SCENE_HOLD) {
                    ticks = 0;
                    if (++sceneIndex >= SCENES.size()) {
                        mc.options.hideGui = false;
                        stage = Stage.IN_WORLD;
                    }
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

    /** Площадка в воздухе над игроком: стол, узел с лагерем, машины, ряд мобов среза. */
    private static void build(MinecraftServer server, BlockPos o) {
        if (server == null) {
            return;
        }
        int x = o.getX();
        int y = o.getY();
        int z = o.getZ();
        String[] commands = {
                "time set noon",
                "weather clear",
                "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false",
                "gamerule doMobSpawning false",
                "gamerule sendCommandFeedback false",
                fill(x - 10, y - 1, z - 2, x + 10, y - 1, z + 14, "minecraft:smooth_stone"),
                fill(x - 10, y, z - 2, x + 10, y + 6, z + 14, "minecraft:air"),
                set(x, y, z + 4, "adept_core:artifact_table"),
                set(x + 6, y, z + 5, "adept_core:node_crystal"),
                set(x + 4, y, z + 4, "adept_core:extractor"),
                set(x + 8, y, z + 4, "adept_core:transition"),
                set(x - 7, y, z + 4, "adept_core:generator"),
                set(x - 5, y, z + 4, "adept_core:crusher"),
                set(x - 3, y, z + 4, "adept_core:smelter"),
                set(x + 6, y - 1, z + 7, "adept_core:ash"),
                summon("adept_core:hornet", x - 4, y + 1, z + 10),
                summon("adept_core:wisp", x - 2, y + 1, z + 10),
                summon("adept_core:ember_spirit", x, y, z + 10),
                summon("adept_core:horned_leader", x + 3, y, z + 11),
                summon("adept_core:trader", x + 6, y, z + 10),
                summon("minecraft:wolf", x - 6, y, z + 10),
                "item replace entity @p hotbar.0 with adept_core:energy_blade",
        };
        for (String command : commands) {
            run(server, command);
        }
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block;
    }

    private static String set(int x, int y, int z, String block) {
        return "setblock " + x + " " + y + " " + z + " " + block;
    }

    private static String summon(String type, int x, int y, int z) {
        return "summon " + type + " " + x + " " + y + " " + z + " {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}";
    }

    private static void run(MinecraftServer server, String command) {
        if (server == null) {
            return;
        }
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command));
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
