import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Генератор модели гаусс-пушки для GeckoLib: геометрия (geo.json), текстура, слой свечения (glowmask)
 * и листы предпросмотра. Blockbench не нужен: модель описана кубами в коде, текстура красится по материалу куба.
 *
 * Запуск из корня репозитория (нужна Java 17+):
 *   java tools/models/GaussModel.java core/src/main/resources/assets/adept_core [папка_предпросмотра]
 *
 * Схема облика (docs/artifacts.md, «Прокачка по сегментам»): у сегмента кость {@code <сегмент>},
 * в ней варианты основы {@code <сегмент>_base0..3} (виден один — по ступени пути, который меняет основу),
 * навесные детали {@code <сегмент>_add1..3} (видны все до ступени второго пути) и вершина {@code <сегмент>_apex}.
 * Ядро прячет лишние кости по прогрессу игрока.
 *
 * Единицы: модель построена в двойном разрешении (1 единица = 1/32 блока, 1 тексель на единицу),
 * в игре уменьшается вдвое преобразованием показа. Ствол смотрит в −Z, верх +Y, модель симметрична по X
 * там, где это возможно. Узоры материалов симметричны, чтобы не зависеть от разворота граней box-UV.
 * Повёрнутых кубов нет: наклоны набраны ступеньками.
 */
public class GaussModel {
    // ---- палитры (tools/sprites/ComponentSprites.java), от блика к тени ----
    static final int[] GUNMETAL = {0x9A9FAB, 0x7C818D, 0x555964, 0x353740, 0x1E1F26, 0x121318};
    static final int[] STEEL = {0xD5DCE4, 0x9EA8B4, 0x6F7986, 0x4C5560, 0x333A43, 0x1C2026};
    static final int[] BRASS = {0xF6D488, 0xDCA850, 0xB47C36, 0x875828, 0x5E3B1E, 0x392313};
    static final int[] COPPER = {0xFBB08C, 0xE5825A, 0xBF5C3C, 0x91432E, 0x66302A, 0x3E1D1C};
    static final int[] LEATHER = {0x91432E, 0x7A3A2A, 0x66302A, 0x522622, 0x3E1D1C, 0x2A1414};
    static final int[] MANA = {0xE6F6FF, 0x7CC4FF, 0x2F8CFF, 0x153F9E};
    static final int[] ARC = {0xF2FFFF, 0xAAF5F0, 0x62DCDA, 0x32AAB4};
    static final int[] RED = {0xFF9A86, 0xF03A2C, 0x8E1418};
    static final int[] BOARD = {0x2F6B4C, 0x1F4A36};

    enum Face { UP, DOWN, EAST, NORTH, WEST, SOUTH }

    /** Материал куба: красит тексель по его месту в кубе. */
    enum Mat {
        GUNMETAL, STEEL, BRASS, COPPER, LEATHER,
        /** Сырая намотка: неровные витки меди. */
        COIL_RAW,
        /** Катушка: ровные витки поперёк оси Z. */
        COIL,
        /** Кристалл маны: светится целиком. */
        CRYSTAL,
        /** Рунное кольцо: латунь со светящимися насечками. */
        RUNE,
        /** Кожух с прорезями. */
        VENT,
        /** Синяя светящаяся трубка контура маны. */
        MANA_TUBE,
        /** Разрядник: голубое свечение. */
        ARC,
        /** Латунь с голубой светящейся полосой. */
        PULSE,
        /** Красная лампа. */
        LAMP,
        /** Тёмный металл с красными лампами в ряд. */
        LAMPS,
        /** Красная краска (рукоятки клапанов), не светится. */
        RED_PAINT,
        /** Ячейка аккумулятора: сталь, латунные торцы, окно с кристаллом. */
        CELL,
        /** Цельный корпус с окном сердечника. */
        CORE,
        /** Конденсатор: сталь с латунными торцами. */
        CAP,
        /** Короб магазина: сталь с окном, в окне иглы. */
        MAG,
        /** Труба прицела: тёмный металл, линзы на торцах. */
        SCOPE,
        /** Плата. */
        PCB
    }

    static final class Cube {
        final String bone;
        final Mat mat;
        final double x, y, z;
        final int w, h, d;
        double inflate;
        int u, v;

        Cube(String bone, Mat mat, double x, double y, double z, int w, int h, int d) {
            this.bone = bone;
            this.mat = mat;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
        }

        Cube inflate(double v) {
            this.inflate = v;
            return this;
        }
    }

    record Bone(String name, String parent, double px, double py, double pz) {
    }

    static final Map<String, Bone> BONES = new LinkedHashMap<>();
    static final List<Cube> CUBES = new ArrayList<>();
    static final String[] SEGMENTS = {"barrel", "coils", "cooler", "battery", "magazine", "scope"};
    static final String[] SEGMENT_NAMES = {"Ствол", "Катушки", "Охладитель", "Аккумулятор", "Магазин", "Прицел"};

    static String cur;

    static void bone(String name, String parent, double px, double py, double pz) {
        BONES.put(name, new Bone(name, parent, px, py, pz));
        cur = name;
    }

    static void part(String segment, String suffix) {
        Bone s = BONES.get(segment);
        bone(segment + "_" + suffix, segment, s.px, s.py, s.pz);
    }

    /** Куб по углу с минимальными координатами. */
    static Cube box(Mat m, double x, double y, double z, int w, int h, int d) {
        Cube c = new Cube(cur, m, x, y, z, w, h, d);
        CUBES.add(c);
        return c;
    }

    /** Куб, центрированный по X. */
    static Cube c(Mat m, int w, double y, int h, double z, int d) {
        return box(m, -w / 2.0, y, z, w, h, d);
    }

    /** Одинаковые кубы слева и справа. */
    static void both(Mat m, double x, double y, double z, int w, int h, int d) {
        box(m, x, y, z, w, h, d);
        box(m, -x - w, y, z, w, h, d);
    }

    // ================= модель =================

    static void build(boolean full) {
        bone("gun", null, 0, 16, 4);
        // Рама: ствольная коробка, рукоять, спуск, хребет приклада. Не меняется.
        bone("frame", "gun", 0, 16, 4);
        c(Mat.GUNMETAL, 6, 15, 8, -6, 20);
        c(Mat.STEEL, 2, 23, 1, -4, 16);
        c(Mat.LEATHER, 4, 9, 6, 9, 4);
        c(Mat.LEATHER, 4, 5, 4, 10, 4);
        c(Mat.STEEL, 2, 11, 1, 3, 6);
        c(Mat.STEEL, 2, 12, 3, 3, 1);
        c(Mat.BRASS, 2, 12, 3, 7, 1);
        c(Mat.GUNMETAL, 4, 17, 3, 14, 12);
        c(Mat.STEEL, 4, 10, 10, 26, 2);
        c(Mat.STEEL, 2, 16, 1, -20, 14);

        barrel(full);
        coils(full);
        cooler(full);
        battery(full);
        magazine(full);
        scope(full);
    }

    static void barrel(boolean full) {
        bone("barrel", "gun", 0, 20, -6);
        // Прототип: тонкий стержень на рунных кольцах, кристаллы сверху.
        part("barrel", "base0");
        c(Mat.STEEL, 2, 19, 2, -32, 26);
        for (int z : new int[]{-31, -27, -23}) {
            c(Mat.RUNE, 4, 18, 4, z, 1);
        }
        c(Mat.CRYSTAL, 2, 21, 3, -30, 2);
        c(Mat.CRYSTAL, 2, 21, 3, -26, 2);
        c(Mat.LEATHER, 2, 19, 2, -22, 1).inflate(0.4);
        // Переход I: настоящий ствол, короткий кожух, руна на стыке.
        part("barrel", "base1");
        c(Mat.STEEL, 4, 18, 4, -33, 27);
        c(Mat.VENT, 6, 17, 6, -28, 7);
        c(Mat.RUNE, 6, 17, 6, -29, 1);
        c(Mat.BRASS, 4, 18, 4, -34, 1).inflate(0.5);
        // Переход II: кожух на всю длину, руна только у дула.
        part("barrel", "base2");
        c(Mat.STEEL, 4, 18, 4, -34, 28);
        c(Mat.VENT, 6, 17, 6, -32, 11);
        c(Mat.RUNE, 6, 17, 6, -33, 1);
        c(Mat.STEEL, 4, 18, 4, -35, 1).inflate(0.5);
        // Финал: цельный ускоритель, длиннее, дульный тормоз.
        part("barrel", "base3");
        c(Mat.STEEL, 4, 18, 4, -37, 31);
        c(Mat.VENT, 6, 17, 6, -34, 13);
        c(Mat.STEEL, 2, 23, 1, -34, 13);
        c(Mat.BRASS, 8, 16, 8, -35, 1);
        c(Mat.GUNMETAL, 6, 17, 6, -38, 2);
        // «Темп»: навесные детали на правом борту коробки.
        part("barrel", "add1");                      // привод подачи
        box(Mat.BRASS, 3, 18, -5, 2, 3, 6);
        box(Mat.STEEL, 3, 19, -8, 1, 1, 3);
        part("barrel", "add2");                      // автоматика спуска
        box(Mat.LAMPS, 3, 18, 2, 1, 3, 5);
        part("barrel", "add3");                      // маховик раскрутки
        box(Mat.BRASS, 3, 17, 8, 1, 5, 5);
        box(Mat.STEEL, 4, 19, 10, 1, 1, 1);
        // Вершина «Разрядная дуга»: вилка разрядника у дула.
        part("barrel", "apex");
        c(Mat.BRASS, 8, 19, 2, -33, 1);
        both(Mat.STEEL, 3, 19, -40, 1, 2, 7);
        both(Mat.ARC, 3, 19, -41, 1, 2, 1);
    }

    static void coils(boolean full) {
        bone("coils", "gun", 0, 20, -13);
        // Прототип: три сырые намотки разного размера, провод поверху.
        part("coils", "base0");
        c(Mat.COIL_RAW, 6, 17, 6, -19, 3);
        c(Mat.COIL_RAW, 5, 17.5, 5, -14, 3);
        c(Mat.COIL_RAW, 6, 17, 6, -9, 3);
        c(Mat.COPPER, 1, 23, 1, -17, 9);
        if (!full) {
            return;
        }
    }

    static void cooler(boolean full) {
        bone("cooler", "gun", 0, 14, -13);
        // Прототип: медная трубка и ледяной кристалл в латунном хомуте.
        part("cooler", "base0");
        c(Mat.COPPER, 2, 14, 2, -20, 14);
        c(Mat.CRYSTAL, 2, 11, 3, -15, 3);
        c(Mat.BRASS, 2, 13, 3, -11, 1).inflate(0.5);
        c(Mat.BRASS, 2, 13, 3, -17, 1).inflate(0.5);
        if (!full) {
            return;
        }
    }

    static void battery(boolean full) {
        bone("battery", "gun", 0, 14, 20);
        // Прототип: кристалл в клетке из латуни под хребтом приклада.
        part("battery", "base0");
        c(Mat.BRASS, 4, 11, 1, 17, 7);
        c(Mat.CRYSTAL, 2, 12, 5, 19, 3);
        both(Mat.STEEL, 1, 12, 17, 1, 5, 1);
        both(Mat.STEEL, 1, 12, 23, 1, 5, 1);
        c(Mat.COPPER, 1, 14, 1, 14, 3);
        if (!full) {
            return;
        }
    }

    static void magazine(boolean full) {
        bone("magazine", "gun", 0, 15, 2);
        // Прототип: короткий короб на 30 игл с кожаной стяжкой.
        part("magazine", "base0");
        c(Mat.MAG, 4, 9, 6, -1, 5);
        c(Mat.LEATHER, 4, 10, 1, -1, 5).inflate(0.4);
        if (!full) {
            return;
        }
    }

    static void scope(boolean full) {
        bone("scope", "gun", 0, 24, 3);
        // Прототип: кольцо-мушка с кристаллом и открытый целик.
        part("scope", "base0");
        c(Mat.STEEL, 4, 24, 1, 9, 1);
        both(Mat.STEEL, 1, 25, 9, 1, 1, 1);
        c(Mat.BRASS, 4, 24, 1, -3, 1);
        c(Mat.BRASS, 4, 27, 1, -3, 1);
        both(Mat.BRASS, 1, 25, -3, 1, 2, 1);
        c(Mat.CRYSTAL, 2, 25, 2, -3, 1);
        if (!full) {
            return;
        }
    }

    // ================= текстура =================

    static int tone(int[] pal, int i) {
        return pal[Math.max(0, Math.min(pal.length - 1, i))];
    }

    /** Цвет текселя; glow[0] — светится ли он. Координаты — место текселя в кубе. */
    static int paint(Cube q, Face f, int x, int y, int z, boolean[] glow) {
        int w = q.w, h = q.h, d = q.d;
        int base = switch (f) {
            case UP -> 1;
            case DOWN -> 4;
            case EAST, WEST -> 2;
            default -> 3;
        };
        // Место на грани: a — по горизонтали, b — по вертикали.
        int a, b, A, B;
        switch (f) {
            case UP, DOWN -> { a = x; b = z; A = w; B = d; }
            case EAST, WEST -> { a = z; b = y; A = d; B = h; }
            default -> { a = x; b = y; A = w; B = h; }
        }
        boolean edge = Math.min(A, B) >= 3 && (a == 0 || b == 0 || a == A - 1 || b == B - 1);
        int t = base + (edge ? 1 : 0);
        boolean side = f != Face.UP && f != Face.DOWN;
        boolean midA = A % 2 == 1 ? a == A / 2 : (a == A / 2 || a == A / 2 - 1);
        boolean midB = B % 2 == 1 ? b == B / 2 : (b == B / 2 || b == B / 2 - 1);
        glow[0] = false;
        switch (q.mat) {
            case GUNMETAL:
                return tone(GUNMETAL, t);
            case STEEL:
                return tone(STEEL, t);
            case BRASS:
                return tone(BRASS, t);
            case COPPER:
                return tone(COPPER, t);
            case LEATHER:
                return tone(LEATHER, base - 1 + (y % 2 == 0 ? 2 : 0));
            case COIL_RAW: {
                boolean tie = z == d / 2 && (x + y) % 4 == 0;
                return tie ? tone(LEATHER, base) : tone(COPPER, base + ((z + (x + y) / 3) % 2) * 2);
            }
            case COIL:
                return tone(COPPER, base - 1 + (z % 2) * 2);
            case CRYSTAL:
                glow[0] = true;
                if (f == Face.UP || (side && b == B - 1 && a == 0)) {
                    return MANA[0];
                }
                return MANA[side && b < B / 2 ? 2 : 1];
            case RUNE:
                if (side && !edge && Math.min(A, B) >= 3 && (a + b) % 2 == 0) {
                    glow[0] = true;
                    return MANA[1];
                }
                if (side && Math.min(A, B) < 3 && midB && a % 2 == 1) {
                    glow[0] = true;
                    return MANA[1];
                }
                return tone(BRASS, t);
            case VENT:
                if (f != Face.DOWN && f != Face.NORTH && f != Face.SOUTH && !edge && z % 2 == 1
                        && (f == Face.UP ? midA : (b >= 2 && b <= B - 3))) {
                    return GUNMETAL[5];
                }
                return tone(GUNMETAL, t);
            case MANA_TUBE:
                glow[0] = true;
                return MANA[f == Face.UP ? 0 : f == Face.DOWN ? 2 : 1];
            case ARC:
                glow[0] = true;
                return ARC[f == Face.UP ? 0 : f == Face.DOWN ? 3 : 1];
            case PULSE:
                if (side && midB) {
                    glow[0] = true;
                    return ARC[1];
                }
                return tone(BRASS, t);
            case LAMP:
                glow[0] = true;
                return RED[f == Face.UP ? 0 : 1];
            case LAMPS:
                if (side && midB && !edge && a % 2 == 1) {
                    glow[0] = true;
                    return RED[1];
                }
                return tone(GUNMETAL, t);
            case RED_PAINT:
                return RED[f == Face.UP ? 1 : 2];
            case CELL:
                if (z == 0 || z == d - 1) {
                    return tone(BRASS, t);
                }
                if ((f == Face.EAST || f == Face.WEST) && midA && midB) {
                    glow[0] = true;
                    return MANA[1];
                }
                return tone(STEEL, t);
            case CORE:
                if ((f == Face.EAST || f == Face.WEST) && Math.abs(2 * a - (A - 1)) <= 3 && Math.abs(2 * b - (B - 1)) <= 3) {
                    glow[0] = true;
                    return Math.abs(2 * a - (A - 1)) <= 1 && Math.abs(2 * b - (B - 1)) <= 1 ? MANA[0] : MANA[2];
                }
                return tone(GUNMETAL, t);
            case CAP:
                return z == 0 || z == d - 1 ? tone(BRASS, base) : tone(STEEL, base + (y == 0 ? 1 : 0));
            case MAG:
                if ((f == Face.EAST || f == Face.WEST) && midA && b >= 1 && b <= B - 2) {
                    return b % 2 == 0 ? BRASS[1] : GUNMETAL[5];
                }
                return tone(STEEL, t + 1);
            case SCOPE:
                if ((f == Face.NORTH || f == Face.SOUTH) && !edge) {
                    glow[0] = true;
                    return ARC[2];
                }
                return tone(GUNMETAL, t);
            case PCB:
                if (side && midB && a % 3 == 1) {
                    return BRASS[1];
                }
                return BOARD[f == Face.UP ? 0 : 1];
            default:
                return 0xFF00FF;
        }
    }

    /** Прямоугольник грани в развёртке box-UV: {u, v, ширина, высота}. */
    static int[] rect(Cube q, Face f) {
        int u = q.u, v = q.v, w = q.w, h = q.h, d = q.d;
        return switch (f) {
            case UP -> new int[]{u + d, v, w, d};
            case DOWN -> new int[]{u + d + w, v, w, d};
            case EAST -> new int[]{u, v + d, d, h};
            case NORTH -> new int[]{u + d, v + d, w, h};
            case WEST -> new int[]{u + d + w, v + d, d, h};
            case SOUTH -> new int[]{u + d + w + d, v + d, w, h};
        };
    }

    /** Место текселя (i, j) грани в кубе: {x, y, z}. */
    static int[] texel(Cube q, Face f, int i, int j) {
        return switch (f) {
            case UP -> new int[]{i, q.h - 1, j};
            case DOWN -> new int[]{i, 0, j};
            case EAST -> new int[]{q.w - 1, q.h - 1 - j, i};
            case WEST -> new int[]{0, q.h - 1 - j, i};
            case NORTH -> new int[]{i, q.h - 1 - j, 0};
            case SOUTH -> new int[]{i, q.h - 1 - j, q.d - 1};
        };
    }

    static int texW, texH;

    /** Раскладка развёрток полками, от высоких к низким. */
    static void pack() {
        List<Cube> order = new ArrayList<>(CUBES);
        order.sort((p, q) -> (q.d + q.h) - (p.d + p.h));
        for (int[] size : new int[][]{{128, 128}, {256, 128}, {256, 256}, {512, 256}, {512, 512}}) {
            int x = 0, y = 0, shelf = 0;
            boolean ok = true;
            for (Cube q : order) {
                int cw = 2 * (q.d + q.w), ch = q.d + q.h;
                if (x + cw > size[0]) {
                    x = 0;
                    y += shelf;
                    shelf = 0;
                }
                if (y + ch > size[1]) {
                    ok = false;
                    break;
                }
                q.u = x;
                q.v = y;
                x += cw;
                shelf = Math.max(shelf, ch);
            }
            if (ok) {
                texW = size[0];
                texH = size[1];
                return;
            }
        }
        throw new IllegalStateException("развёртка не помещается");
    }

    static BufferedImage texture, glowmask;

    static void paintTextures() {
        texture = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        glowmask = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        boolean[] glow = new boolean[1];
        for (Cube q : CUBES) {
            for (Face f : Face.values()) {
                int[] r = rect(q, f);
                for (int j = 0; j < r[3]; j++) {
                    for (int i = 0; i < r[2]; i++) {
                        int[] p = texel(q, f, i, j);
                        int rgb = paint(q, f, p[0], p[1], p[2], glow);
                        texture.setRGB(r[0] + i, r[1] + j, 0xFF000000 | rgb);
                        if (glow[0]) {
                            glowmask.setRGB(r[0] + i, r[1] + j, 0xFF000000 | rgb);
                        }
                    }
                }
            }
        }
    }

    // ================= geo.json =================

    static String num(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "");
    }

    static String geoJson() {
        StringBuilder s = new StringBuilder();
        s.append("{\n  \"format_version\": \"1.12.0\",\n  \"minecraft:geometry\": [\n    {\n");
        s.append("      \"description\": {\n        \"identifier\": \"geometry.gauss_gun\",\n");
        s.append("        \"texture_width\": ").append(texW).append(",\n        \"texture_height\": ").append(texH).append(",\n");
        s.append("        \"visible_bounds_width\": 6,\n        \"visible_bounds_height\": 4,\n        \"visible_bounds_offset\": [0, 1, 0]\n      },\n");
        s.append("      \"bones\": [\n");
        int n = 0;
        for (Bone b : BONES.values()) {
            s.append("        {\"name\": \"").append(b.name).append("\"");
            if (b.parent != null) {
                s.append(", \"parent\": \"").append(b.parent).append("\"");
            }
            s.append(", \"pivot\": [").append(num(b.px)).append(", ").append(num(b.py)).append(", ").append(num(b.pz)).append("]");
            List<Cube> own = CUBES.stream().filter(q -> q.bone.equals(b.name)).toList();
            if (!own.isEmpty()) {
                s.append(", \"cubes\": [\n");
                for (int i = 0; i < own.size(); i++) {
                    Cube q = own.get(i);
                    s.append("          {\"origin\": [").append(num(q.x)).append(", ").append(num(q.y)).append(", ").append(num(q.z))
                            .append("], \"size\": [").append(q.w).append(", ").append(q.h).append(", ").append(q.d).append("]");
                    if (q.inflate != 0) {
                        s.append(", \"inflate\": ").append(num(q.inflate));
                    }
                    s.append(", \"uv\": [").append(q.u).append(", ").append(q.v).append("]}")
                            .append(i < own.size() - 1 ? ",\n" : "\n");
                }
                s.append("        ]");
            }
            s.append("}").append(++n < BONES.size() ? ",\n" : "\n");
        }
        s.append("      ]\n    }\n  ]\n}\n");
        return s.toString();
    }

    // ================= предпросмотр =================

    /** Состояние прокачки: по сегментам — ступень пути основы (0–3), ступень пути деталей (0–3), вершина. */
    record Look(int[] base, int[] add, boolean[] apex) {
        static Look of(int base, int add, boolean apex) {
            int[] b = new int[6], a = new int[6];
            boolean[] x = new boolean[6];
            Arrays.fill(b, base);
            Arrays.fill(a, add);
            Arrays.fill(x, apex);
            return new Look(b, a, x);
        }

        Look with(int segment, int base, int add, boolean apex) {
            Look l = new Look(this.base.clone(), this.add.clone(), this.apex.clone());
            l.base[segment] = base;
            l.add[segment] = add;
            l.apex[segment] = apex;
            return l;
        }

        boolean visible(String bone) {
            for (int i = 0; i < SEGMENTS.length; i++) {
                String p = SEGMENTS[i] + "_";
                if (bone.startsWith(p)) {
                    String part = bone.substring(p.length());
                    if (part.startsWith("base")) {
                        return part.charAt(4) - '0' == base[i];
                    }
                    if (part.startsWith("add")) {
                        return part.charAt(3) - '0' <= add[i];
                    }
                    return apex[i];
                }
            }
            return true;
        }
    }

    /** Ортографический снимок модели: поворот вокруг Y (yaw) и наклон (pitch), градусы. */
    static BufferedImage render(Look look, double yaw, double pitch, double scale, int W, int H) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int g = 0x16 + (y * 0x0C) / H;
                img.setRGB(x, y, (g << 16) | (g << 8) | (g + 6));
            }
        }
        double[] depth = new double[W * H];
        Arrays.fill(depth, Double.NEGATIVE_INFINITY);
        double cy = Math.cos(Math.toRadians(yaw)), sy = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        // Центр кадра — середина модели.
        double mx = 0, my = 17, mz = -5;
        boolean[] glow = new boolean[1];
        for (Cube q : CUBES) {
            if (!look.visible(q.bone)) {
                continue;
            }
            double k = q.inflate;
            for (Face f : Face.values()) {
                double[] nrm = switch (f) {
                    case UP -> new double[]{0, 1, 0};
                    case DOWN -> new double[]{0, -1, 0};
                    case EAST -> new double[]{1, 0, 0};
                    case WEST -> new double[]{-1, 0, 0};
                    case NORTH -> new double[]{0, 0, -1};
                    case SOUTH -> new double[]{0, 0, 1};
                };
                double[] rn = rot(nrm[0], nrm[1], nrm[2], cy, sy, cp, sp);
                if (rn[2] <= 0.001) {
                    continue;
                }
                double light = 0.62 + 0.38 * Math.max(0, rn[0] * -0.35 + rn[1] * 0.6 + rn[2] * 0.72);
                int[] r = rect(q, f);
                for (int j = 0; j < r[3]; j++) {
                    for (int i = 0; i < r[2]; i++) {
                        int[] p = texel(q, f, i, j);
                        int rgb = paint(q, f, p[0], p[1], p[2], glow);
                        if (!glow[0]) {
                            rgb = mul(rgb, light);
                        }
                        // Четыре угла текселя в координатах куба (0..размер), затем в мире с учётом inflate.
                        double[][] corners = new double[4][];
                        for (int cnr = 0; cnr < 4; cnr++) {
                            double da = (cnr == 1 || cnr == 2) ? 1 : 0, db = cnr >= 2 ? 1 : 0;
                            double lx, ly, lz;
                            switch (f) {
                                case UP -> { lx = p[0] + da; ly = q.h; lz = p[2] + db; }
                                case DOWN -> { lx = p[0] + da; ly = 0; lz = p[2] + db; }
                                case EAST -> { lx = q.w; ly = p[1] + db; lz = p[2] + da; }
                                case WEST -> { lx = 0; ly = p[1] + db; lz = p[2] + da; }
                                case NORTH -> { lx = p[0] + da; ly = p[1] + db; lz = 0; }
                                default -> { lx = p[0] + da; ly = p[1] + db; lz = q.d; }
                            }
                            double wx = q.x - k + lx * (q.w + 2 * k) / q.w - mx;
                            double wy = q.y - k + ly * (q.h + 2 * k) / q.h - my;
                            double wz = q.z - k + lz * (q.d + 2 * k) / q.d - mz;
                            double[] v = rot(wx, wy, wz, cy, sy, cp, sp);
                            corners[cnr] = new double[]{W / 2.0 + v[0] * scale, H / 2.0 - v[1] * scale, v[2]};
                        }
                        fill(img, depth, corners, rgb);
                    }
                }
            }
        }
        return img;
    }

    static double[] rot(double x, double y, double z, double cy, double sy, double cp, double sp) {
        double x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
        double y2 = y * cp - z1 * sp, z2 = y * sp + z1 * cp;
        return new double[]{x1, y2, z2};
    }

    static int mul(int rgb, double k) {
        int r = (int) Math.min(255, ((rgb >> 16) & 255) * k), g = (int) Math.min(255, ((rgb >> 8) & 255) * k), b = (int) Math.min(255, (rgb & 255) * k);
        return (r << 16) | (g << 8) | b;
    }

    /** Заливка выпуклого четырёхугольника с буфером глубины (глубина линейна по экрану). */
    static void fill(BufferedImage img, double[] depth, double[][] c, int rgb) {
        int W = img.getWidth(), H = img.getHeight();
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] p : c) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        double ux = c[1][0] - c[0][0], uy = c[1][1] - c[0][1], uz = c[1][2] - c[0][2];
        double vx = c[3][0] - c[0][0], vy = c[3][1] - c[0][1], vz = c[3][2] - c[0][2];
        double det = ux * vy - uy * vx;
        if (Math.abs(det) < 1e-9) {
            return;
        }
        for (int y = (int) Math.floor(minY); y <= (int) Math.ceil(maxY); y++) {
            for (int x = (int) Math.floor(minX); x <= (int) Math.ceil(maxX); x++) {
                if (x < 0 || y < 0 || x >= W || y >= H) {
                    continue;
                }
                double px = x + 0.5 - c[0][0], py = y + 0.5 - c[0][1];
                double s = (px * vy - py * vx) / det, t = (ux * py - uy * px) / det;
                if (s < -0.02 || s > 1.02 || t < -0.02 || t > 1.02) {
                    continue;
                }
                double z = c[0][2] + s * uz + t * vz;
                if (z > depth[y * W + x]) {
                    depth[y * W + x] = z;
                    img.setRGB(x, y, rgb);
                }
            }
        }
    }

    record Shot(String label, Look look) {
    }

    /** Лист: строки — состояния, столбцы — вид в три четверти и сбоку. */
    static BufferedImage sheet(String title, List<Shot> shots) {
        int cw = 520, ch = 250, head = 34;
        BufferedImage out = new BufferedImage(cw * 2, head + ch * shots.size(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1A1720));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(new Color(0xE8E0D0));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString(title, 12, 23);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            int y = head + i * ch;
            g.drawImage(render(s.look, 215, 20, 6.2, cw - 2, ch - 2), 0, y, null);
            g.drawImage(render(s.look, 270, 0, 6.2, cw - 2, ch - 2), cw, y, null);
            g.setColor(new Color(0xB08D57));
            g.drawRect(0, y, cw - 2, ch - 2);
            g.drawRect(cw, y, cw - 2, ch - 2);
            g.setColor(new Color(0xE8E0D0));
            g.drawString(s.label, 10, y + 18);
        }
        g.dispose();
        return out;
    }

    /** Проверка: видимые одновременно кубы не должны иметь совпадающих граней в одной плоскости (мерцание в игре). */
    static void checkCoplanar() {
        int warn = 0;
        for (int i = 0; i < CUBES.size(); i++) {
            for (int j = i + 1; j < CUBES.size(); j++) {
                Cube p = CUBES.get(i), q = CUBES.get(j);
                if (!together(p.bone, q.bone)) {
                    continue;
                }
                double[] a = bounds(p), b = bounds(q);
                for (int axis = 0; axis < 3; axis++) {
                    int o1 = (axis + 1) % 3, o2 = (axis + 2) % 3;
                    double ov1 = Math.min(a[o1 + 3], b[o1 + 3]) - Math.max(a[o1], b[o1]);
                    double ov2 = Math.min(a[o2 + 3], b[o2 + 3]) - Math.max(a[o2], b[o2]);
                    if (ov1 <= 1e-6 || ov2 <= 1e-6) {
                        continue;
                    }
                    for (int side = 0; side < 2; side++) {
                        if (Math.abs(a[axis + side * 3] - b[axis + side * 3]) < 1e-6) {
                            System.out.println("  грани в одной плоскости: " + p.bone + " " + p.mat + " и " + q.bone + " " + q.mat
                                    + " (ось " + "xyz".charAt(axis) + (side == 0 ? "−" : "+") + ")");
                            warn++;
                        }
                    }
                }
            }
        }
        System.out.println("Проверка граней: " + (warn == 0 ? "совпадений нет" : warn + " совпадений"));
    }

    static double[] bounds(Cube q) {
        double k = q.inflate;
        return new double[]{q.x - k, q.y - k, q.z - k, q.x + q.w + k, q.y + q.h + k, q.z + q.d + k};
    }

    /** Могут ли две кости быть видны одновременно: разные варианты основы одного сегмента — нет. */
    static boolean together(String a, String b) {
        for (String s : SEGMENTS) {
            if (a.startsWith(s + "_base") && b.startsWith(s + "_base")) {
                return a.equals(b);
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "core/src/main/resources/assets/adept_core");
        Path preview = args.length > 1 ? Path.of(args[1]) : null;
        build(true);
        pack();
        paintTextures();
        checkCoplanar();
        Files.createDirectories(assets.resolve("geo/item"));
        Files.createDirectories(assets.resolve("textures/artifact"));
        Files.writeString(assets.resolve("geo/item/gauss_gun.geo.json"), geoJson(), StandardCharsets.UTF_8);
        ImageIO.write(texture, "png", assets.resolve("textures/artifact/gauss_gun.png").toFile());
        ImageIO.write(glowmask, "png", assets.resolve("textures/artifact/gauss_gun_glowmask.png").toFile());
        System.out.println("Кубов: " + CUBES.size() + ", костей: " + BONES.size() + ", текстура " + texW + "×" + texH);
        if (preview != null) {
            Files.createDirectories(preview);
            Look proto = Look.of(0, 0, false);
            ImageIO.write(sheet("Гаусс-пушка — прототип (сегменты не тронуты)", List.of(new Shot("Прототип", proto))),
                    "png", preview.resolve("gauss-prototype.png").toFile());
            ImageIO.write(sheet("Ствол: «Разгон» меняет основу", List.of(
                    new Shot("Разгон 0 — прототип", proto),
                    new Shot("Разгон I — переход", proto.with(0, 1, 0, false)),
                    new Shot("Разгон II — переход", proto.with(0, 2, 0, false)),
                    new Shot("Разгон III — финал", proto.with(0, 3, 0, false)))),
                    "png", preview.resolve("gauss-barrel-base.png").toFile());
            ImageIO.write(sheet("Ствол: «Темп» добавляет детали, вершина «Разрядная дуга»", List.of(
                    new Shot("Темп I — привод подачи", proto.with(0, 0, 1, false)),
                    new Shot("Темп III — привод, автоматика, маховик", proto.with(0, 0, 3, false)),
                    new Shot("Разгон III + Темп III + вершина", proto.with(0, 3, 3, true)))),
                    "png", preview.resolve("gauss-barrel-add.png").toFile());
            ImageIO.write(texture, "png", preview.resolve("gauss-texture.png").toFile());
        }
    }
}
