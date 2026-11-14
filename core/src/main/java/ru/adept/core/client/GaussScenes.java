package ru.adept.core.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.GaussLook.Segment;
import ru.adept.core.table.GaussUpgrades;

import java.util.Locale;

/**
 * Сцены гаусс-пушки для клиентского автотеста ({@link ClientSmokeTest}): модель в трёх состояниях прокачки,
 * вид от первого и третьего лица со шкалами на экране игрока, экран прокачки на столе.
 * Своя площадка в стороне от остальных сцен, тёмная стена позади — чтобы модель читалась на снимке.
 */
final class GaussScenes {
    /** Частичная прокачка: ствол в финале с одной деталью и вершиной, катушки и прицел в переходе, остальное — прототип. */
    static final int PARTIAL = GaussLook.with(GaussLook.with(GaussLook.with(GaussLook.with(0,
            Segment.BARREL, 3, 1, true), Segment.COILS, 1, 3, false), Segment.SCOPE, 2, 2, false), Segment.MAGAZINE, 1, 0, false);
    static final int FINAL = GaussLook.uniform(3, 3, true);
    /** Почти всё открыто, вершины прицела нет: на экране стола она доступна. */
    static final int ALMOST = GaussLook.with(FINAL, Segment.SCOPE, 3, 2, false);

    private GaussScenes() {
    }

    /** Площадка: пол, тёмная стена позади модели. Координаты сцен в {@link ClientSmokeTest} — относительно той же точки. */
    static void build(MinecraftServer server, BlockPos o) {
        int x = o.getX();
        int y = o.getY();
        int z = o.getZ();
        run(server, "fill " + (x + 16) + " " + (y - 1) + " " + (z - 1) + " " + (x + 29) + " " + (y - 1) + " " + (z + 9) + " minecraft:smooth_stone");
        run(server, "fill " + (x + 16) + " " + y + " " + (z - 1) + " " + (x + 29) + " " + (y + 7) + " " + (z + 9) + " minecraft:air");
        run(server, "fill " + (x + 16) + " " + y + " " + (z + 9) + " " + (x + 29) + " " + (y + 7) + " " + (z + 9) + " minecraft:black_concrete");
    }

    /** Подготовка сцены; вызывается в её первый тик. */
    static void start(Minecraft mc, String scene, BlockPos o) {
        MinecraftServer server = mc.getSingleplayerServer();
        switch (scene) {
            case "gauss_proto" -> show(server, o, 0);
            case "gauss_partial" -> show(server, o, PARTIAL);
            case "gauss_final" -> show(server, o, FINAL);
            case "gauss_hand" -> {
                run(server, "kill @e[tag=gauss_show]");
                hold(mc, server, 2, 2, 0, 0.62);
            }
            case "gauss_third" -> {
                hold(mc, server, 3, 3, 1, 0.2);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case "gauss_table" -> {
                // Выживание: видно, каких компонентов хватает, а каких нет.
                run(server, "gamemode survival @p");
                run(server, "give @p adept_core:coil_2_tech 2");
                run(server, "give @p adept_core:frame_2 3");
                run(server, "item replace entity @p hotbar.8 with adept_core:gauss_gun"
                        + "[minecraft:custom_data={charge:260,magazine:[\"iron\",\"iron\",\"steel\",\"steel\",\"iron\"]}]");
                mc.setScreen(new GaussUpgradeScreen(null, () -> PARTIAL, id -> {
                }).select(Segment.BARREL, 1, 2).tryOn());
            }
            case "gauss_table_apex" -> {
                run(server, "give @p adept_core:lens_3_magic 1");
                run(server, "give @p adept_core:lens_3_tech 1");
                run(server, "give @p adept_core:sensor_3_magic 1");
                run(server, "give @p adept_core:sensor_3_tech 1");
                run(server, "give @p adept_core:node_cluster_3 1");
                mc.setScreen(new GaussUpgradeScreen(null, () -> ALMOST, id -> {
                }).select(Segment.SCOPE, GaussUpgrades.APEX, 1));
            }
            default -> {
            }
        }
    }

    /** Уборка после сцены. */
    static void end(Minecraft mc, String scene) {
        MinecraftServer server = mc.getSingleplayerServer();
        switch (scene) {
            case "gauss_third" -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (mc.player != null) {
                    mc.player.getInventory().selected = 0;
                }
                run(server, "item replace entity @p hotbar.1 with minecraft:air");
            }
            case "gauss_table" -> mc.setScreen(null);
            case "gauss_table_apex" -> {
                mc.setScreen(null);
                run(server, "clear @p");
                run(server, "gamemode creative @p");
                run(server, "item replace entity @p hotbar.0 with adept_core:energy_blade");
            }
            default -> {
            }
        }
    }

    /** Модель на площадке: предмет-витрина в полный размер, облик задан данными предмета. */
    private static void show(MinecraftServer server, BlockPos o, int look) {
        run(server, "kill @e[tag=gauss_show]");
        run(server, String.format(Locale.ROOT,
                "summon minecraft:item_display %.1f %.1f %.1f {item:{id:\"adept_core:gauss_gun\",count:1,"
                        + "components:{\"minecraft:custom_data\":{look:%d}}},item_display:\"none\",Rotation:[110f,0f],"
                        + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],"
                        + "scale:[0.8f,0.8f,0.8f]},brightness:{sky:15,block:15},Tags:[\"gauss_show\"]}",
                o.getX() + 22.5, o.getY() + 1.8, o.getZ() + 6.0, look));
    }

    /** Пушка в руке игрока: прогресс задаёт облик, заряд и нагрев — данные предмета (нагрев не остывает). */
    private static void hold(Minecraft mc, MinecraftServer server, int base, int add, int apex, double heat) {
        run(server, "adept artifact gauss @p " + base + " " + add + " " + apex);
        run(server, String.format(Locale.ROOT, "item replace entity @p hotbar.1 with adept_core:gauss_gun"
                + "[minecraft:custom_data={charge:260,heat:%.2fd,heat_time:9000000000L,magazine:[\"iron\",\"iron\",\"steel\"]}]", heat));
        if (mc.player != null) {
            mc.player.getInventory().selected = 1;
        }
    }

    private static void run(MinecraftServer server, String command) {
        if (server == null) {
            return;
        }
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command));
    }
}
