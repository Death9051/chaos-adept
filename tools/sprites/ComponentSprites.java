import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Генератор 2D-спрайтов компонентов (16×16) и моделей предметов.
 * Форма — по семейству, металл — по рангу (I медь, II сталь, III титан),
 * магическая вариация добавляет светящиеся руны и кристалл, техническая — болты и красный индикатор.
 *
 * Запуск из корня репозитория (нужна Java 17+):
 *   java tools/sprites/ComponentSprites.java core/src/main/resources/assets/adept_core
 *
 * Список семейств и их природа должны совпадать с ru.adept.core.component.ComponentFamily.
 */
public class ComponentSprites {
    static final int N = 16;

    // Обозначения пикселей в холсте
    static final char EMPTY = '.', METAL = 'm', GLASS = 'g', GLASS_HI = 'G', DARK = 'k', BOARD = 'p',
            CRYSTAL = 'v', CRYSTAL_HI = 'V', MANA = 'B', MANA_HI = 'b', RUNE = 'r', RED = 'e', BOLT = 'x';

    record Family(String id, String nature) {}

    static final List<Family> FAMILIES = List.of(
            new Family("wiring", "tech"), new Family("coil", "both"), new Family("capacitor", "both"),
            new Family("frame", "tech"), new Family("radiator", "tech"), new Family("nozzle", "tech"),
            new Family("actuator", "both"), new Family("spring", "both"), new Family("lens", "both"),
            new Family("sensor", "both"), new Family("emitter", "both"), new Family("mana_circuit", "magic"),
            new Family("matrix", "magic"), new Family("control_board", "both"));

    // Металл по рангу: блик, основа, тень
    static final int[][] RANK_METAL = {
            {0xF0A070, 0xC0703F, 0x7E4023},  // I — медь
            {0xD2D7DE, 0x969CA6, 0x585E68},  // II — сталь
            {0xF2F5FB, 0xBCC6D8, 0x76819A},  // III — титан
    };
    static final int OUTLINE = 0x241E1C;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Укажите папку assets/adept_core");
            System.exit(1);
        }
        Path assets = Path.of(args[0]);
        Path tex = assets.resolve("textures/item/component");
        Path models = assets.resolve("models/item");
        Files.createDirectories(tex);
        Files.createDirectories(models);
        int count = 0;
        for (Family f : FAMILIES) {
            List<String> variants = switch (f.nature) {
                case "magic" -> List.of("magic");
                case "tech" -> List.of("tech");
                default -> List.of("magic", "tech");
            };
            for (String variant : variants) {
                for (int rank = 1; rank <= 3; rank++) {
                    String id = f.nature.equals("both") ? f.id + "_" + rank + "_" + variant : f.id + "_" + rank;
                    char[][] c = shape(f.id);
                    overlay(c, f.id, variant);
                    ImageIO.write(render(c, rank), "png", tex.resolve(id + ".png").toFile());
                    Files.writeString(models.resolve(id + ".json"), """
                            {
                              "parent": "minecraft:item/generated",
                              "textures": {
                                "layer0": "adept_core:item/component/%s"
                              }
                            }
                            """.formatted(id), StandardCharsets.UTF_8);
                    count++;
                }
            }
        }
        System.out.println("Спрайтов и моделей: " + count);
    }

    // ---------- Формы по семействам ----------

    static char[][] blank() {
        char[][] c = new char[N][N];
        for (char[] row : c) {
            java.util.Arrays.fill(row, EMPTY);
        }
        return c;
    }

    static void set(char[][] c, int x, int y, char v) {
        if (x >= 0 && y >= 0 && x < N && y < N) {
            c[y][x] = v;
        }
    }

    static void rect(char[][] c, int x0, int y0, int x1, int y1, char v) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(c, x, y, v);
            }
        }
    }

    static void line(char[][] c, int x0, int y0, int x1, int y1, char v) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            set(c, x0, y0, v);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    static void disk(char[][] c, double cx, double cy, double r, char v) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                if (dx * dx + dy * dy <= r * r) {
                    set(c, x, y, v);
                }
            }
        }
    }

    static void ring(char[][] c, double cx, double cy, double rOut, double rIn, char v) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = dx * dx + dy * dy;
                if (d <= rOut * rOut && d > rIn * rIn) {
                    set(c, x, y, v);
                }
            }
        }
    }

    static char[][] shape(String id) {
        char[][] c = blank();
        switch (id) {
            case "wiring" -> { // пучок из трёх проводов со стяжкой
                line(c, 1, 11, 11, 1, METAL);
                line(c, 2, 13, 13, 2, METAL);
                line(c, 4, 14, 14, 4, METAL);
                line(c, 5, 6, 9, 10, DARK);
                line(c, 6, 5, 10, 9, DARK);
            }
            case "coil" -> { // катушка: щёчки и намотка
                rect(c, 2, 2, 13, 3, METAL);
                rect(c, 2, 12, 13, 13, METAL);
                rect(c, 4, 4, 11, 11, METAL);
                for (int y = 5; y <= 11; y += 2) {
                    rect(c, 4, y, 11, y, DARK);
                }
            }
            case "capacitor" -> { // банка с выводами
                rect(c, 4, 2, 11, 11, METAL);
                rect(c, 4, 2, 11, 3, DARK);
                rect(c, 6, 12, 6, 14, METAL);
                rect(c, 9, 12, 9, 14, METAL);
            }
            case "frame" -> { // рама с раскосом
                rect(c, 2, 2, 13, 13, METAL);
                rect(c, 5, 5, 10, 10, EMPTY);
                line(c, 5, 10, 10, 5, METAL);
            }
            case "radiator" -> { // рёбра на основании
                rect(c, 2, 10, 13, 13, METAL);
                for (int x = 2; x <= 13; x += 2) {
                    rect(c, x, 2, x, 9, METAL);
                }
            }
            case "nozzle" -> { // раструб
                rect(c, 5, 1, 10, 3, DARK);
                for (int y = 4; y <= 14; y++) {
                    int half = 2 + (y - 4) / 2;
                    rect(c, 8 - half, y, 7 + half, y, METAL);
                }
                rect(c, 6, 12, 9, 14, DARK);
            }
            case "actuator" -> { // шестерня
                disk(c, 8, 8, 5, METAL);
                int[][] teeth = {{7, 1}, {7, 13}, {1, 7}, {13, 7}, {3, 3}, {11, 3}, {3, 11}, {11, 11}};
                for (int[] t : teeth) {
                    rect(c, t[0], t[1], t[0] + 1, t[1] + 1, METAL);
                }
                disk(c, 8, 8, 1.6, DARK);
            }
            case "spring" -> { // зигзаг между двух шайб
                rect(c, 4, 1, 11, 2, METAL);
                rect(c, 4, 13, 11, 14, METAL);
                rect(c, 5, 2, 10, 2, DARK);
                rect(c, 5, 13, 10, 13, DARK);
                for (int i = 0; i < 4; i++) {
                    int y = 3 + i * 2 + i / 2;
                    line(c, 4, y, 11, y + 2, METAL);
                    line(c, 11, y + 2, 4, Math.min(y + 4, 12), METAL);
                }
            }
            case "lens" -> { // оправа и стекло
                ring(c, 8, 8, 6.5, 4.5, METAL);
                disk(c, 8, 8, 4.5, GLASS);
                set(c, 6, 6, GLASS_HI);
                set(c, 7, 6, GLASS_HI);
                set(c, 6, 7, GLASS_HI);
            }
            case "sensor" -> { // корпус с глазком и антенной
                rect(c, 3, 5, 12, 13, METAL);
                disk(c, 7.5, 9.5, 2.6, DARK);
                disk(c, 7.5, 9.5, 1.2, GLASS);
                rect(c, 11, 1, 11, 4, METAL);
                set(c, 11, 1, RED);
            }
            case "emitter" -> { // рукоять с кристаллом на конце
                rect(c, 6, 6, 9, 14, METAL);
                for (int y = 8; y <= 13; y += 2) {
                    rect(c, 6, y, 9, y, DARK);
                }
                rect(c, 5, 5, 10, 6, METAL);
                for (int y = 1; y <= 4; y++) {
                    int half = y <= 2 ? y : 5 - y;
                    rect(c, 8 - half, y, 7 + half, y, CRYSTAL);
                }
                set(c, 7, 2, CRYSTAL_HI);
            }
            case "mana_circuit" -> { // пластина с синими жилами
                rect(c, 2, 3, 13, 12, BOARD);
                line(c, 3, 5, 8, 5, MANA);
                line(c, 8, 5, 8, 10, MANA);
                line(c, 8, 10, 12, 10, MANA);
                line(c, 4, 9, 6, 7, MANA);
                set(c, 3, 5, MANA_HI);
                set(c, 12, 10, MANA_HI);
                set(c, 6, 7, MANA_HI);
                for (int x = 3; x <= 12; x += 2) {
                    set(c, x, 13, METAL);
                }
            }
            case "matrix" -> { // кристаллическая решётка в оправе
                for (int y = 1; y <= 14; y++) {
                    for (int x = 1; x <= 14; x++) {
                        if (Math.abs(x + 0.5 - 8) + Math.abs(y + 0.5 - 8) <= 6.5) {
                            set(c, x, y, CRYSTAL);
                        }
                    }
                }
                line(c, 8, 2, 8, 13, CRYSTAL_HI);
                line(c, 2, 8, 13, 8, CRYSTAL_HI);
                int[][] corners = {{1, 1}, {13, 1}, {1, 13}, {13, 13}};
                for (int[] k : corners) {
                    rect(c, k[0], k[1], k[0] + 1, k[1] + 1, METAL);
                }
            }
            case "control_board" -> { // плата с чипом и дорожками
                rect(c, 2, 2, 13, 13, BOARD);
                rect(c, 6, 6, 9, 9, DARK);
                line(c, 3, 4, 6, 4, METAL);
                line(c, 9, 11, 12, 11, METAL);
                line(c, 4, 7, 5, 7, METAL);
                line(c, 10, 8, 12, 8, METAL);
                line(c, 7, 3, 7, 5, METAL);
                line(c, 8, 10, 8, 12, METAL);
            }
            default -> throw new IllegalArgumentException("Нет формы для " + id);
        }
        return c;
    }

    // ---------- Вариации ----------

    /** Точки рун (магия) и индикатора с болтами (техника) для каждого семейства: x,y. */
    static int[] magicAnchor(String id) {
        return switch (id) {
            case "coil" -> new int[]{6, 6};
            case "capacitor" -> new int[]{6, 6};
            case "actuator" -> new int[]{3, 7};
            case "spring" -> new int[]{6, 1};
            case "lens" -> new int[]{3, 2};
            case "sensor" -> new int[]{4, 5};
            case "emitter" -> new int[]{6, 9};
            case "control_board" -> new int[]{3, 9};
            default -> null;
        };
    }

    static void overlay(char[][] c, String id, String variant) {
        if (variant.equals("magic") && magicAnchor(id) != null) {
            // тёмные детали светятся маной, стекло линзы — кристальное
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    if (c[y][x] == DARK) {
                        c[y][x] = MANA;
                    } else if (c[y][x] == GLASS) {
                        c[y][x] = CRYSTAL;
                    } else if (c[y][x] == GLASS_HI) {
                        c[y][x] = CRYSTAL_HI;
                    }
                }
            }
        }
        if (variant.equals("magic")) {
            int[] a = magicAnchor(id);
            if (a != null) { // руна-«галочка» из трёх точек и кристаллик рядом
                set(c, a[0], a[1], RUNE);
                set(c, a[0] + 1, a[1] + 1, RUNE);
                set(c, a[0] + 2, a[1], RUNE);
                set(c, a[0] + 3, a[1] + 1, CRYSTAL);
            }
        } else if (magicAnchor(id) != null) { // у семейств с вариациями — болты и индикатор
            int[] a = magicAnchor(id);
            set(c, a[0], a[1], BOLT);
            set(c, a[0] + 2, a[1], BOLT);
            set(c, a[0] + 1, a[1] + 1, RED);
        }
    }

    // ---------- Отрисовка ----------

    static BufferedImage render(char[][] c, int rank) {
        int[] metal = RANK_METAL[rank - 1];
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                char v = c[y][x];
                int rgb;
                if (v == EMPTY) {
                    // контур вокруг заполненных пикселей
                    if (filled(c, x - 1, y) || filled(c, x + 1, y) || filled(c, x, y - 1) || filled(c, x, y + 1)) {
                        img.setRGB(x, y, 0xFF000000 | OUTLINE);
                    }
                    continue;
                }
                rgb = switch (v) {
                    case METAL -> !filled(c, x, y - 1) || !filled(c, x - 1, y) ? metal[0]
                            : !filled(c, x, y + 1) || !filled(c, x + 1, y) ? metal[2] : metal[1];
                    case GLASS -> 0x9ED8E8;
                    case GLASS_HI -> 0xE8FAFF;
                    case DARK -> 0x3A3A42;
                    case BOARD -> 0x2E4636;
                    case CRYSTAL -> 0xA874E0;
                    case CRYSTAL_HI -> 0xE2CCFF;
                    case MANA -> 0x3F8FE8;
                    case MANA_HI -> 0xA8DCFF;
                    case RUNE -> 0x6FD4FF;
                    case RED -> 0xE8443A;
                    case BOLT -> 0x30343A;
                    default -> 0xFF00FF;
                };
                img.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return img;
    }

    static boolean filled(char[][] c, int x, int y) {
        return x >= 0 && y >= 0 && x < N && y < N && c[y][x] != EMPTY;
    }
}
