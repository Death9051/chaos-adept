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
import java.util.List;

/**
 * Генератор модели энергоклинка для GeckoLib. Общие инструменты (кубы, раскраска, развёртка, geo, предпросмотр) —
 * в {@link GaussModel}; здесь только форма клинка, его анимации и листы предпросмотра.
 *
 * Запуск (JDK 22+, файлы лежат рядом): {@code java tools/models/BladeModel.java <assets> [preview]}.
 *
 * Клинок стоит вдоль оси Y, лезвием вверх; плоскость лезвия — XY, толщина по Z. Зоны сегментов не пересекаются:
 * аккумулятор — навершие (y 0…6), мана-контур — трубка вдоль рукояти сзади (z от 1,5), синхронизатор — муфта
 * между рукоятью и гардой (y 16…19), привод — гарда (y 19…23), гнездо фокуса — основание лезвия (y 23…29),
 * эмиттер — головка и само лезвие (y от 29). Поле лезвия лежит в костях {@code emitter_baseN_field}:
 * ядро прячет их, когда клинок выключен.
 */
public class BladeModel extends GaussModel {
    /** Куб, центрированный по X и Z. */
    static Cube cz(Mat m, int w, double y, int h, int d) {
        return box(m, -w / 2.0, y, -d / 2.0, w, h, d);
    }

    static void octY(Mat m, int D, int a, double y, int h) {
        octY(m, D, a, 0, y, 0, h);
    }

    /** Кость поля лезвия внутри текущего варианта эмиттера. */
    static void field(int base) {
        bone("emitter_base" + base + "_field", "emitter_base" + base, 0, 32, 0);
    }

    static void buildBlade() {
        bone("blade", null, 0, 12, 0);
        // Рама: кожаная рукоять, сердечник гарды, стержень до эмиттера.
        bone("frame", "blade", 0, 12, 0);
        octY(Mat.LEATHER, 3, 1, 6, 10);
        cz(Mat.GUNMETAL, 4, 19, 4, 3);
        cz(Mat.STEEL, 2, 22.9, 9, 2);

        emitter();
        drive();
        battery();
        focus();
        contour();
        sync();
    }

    static void emitter() {
        bone("emitter", "blade", 0, 30, 0);
        // Прототип: рунные кольца вокруг стержня, поле неровное.
        part("emitter", "base0");
        octY(Mat.RUNE, 5, 2, 29.5, 1);
        octY(Mat.RUNE, 5, 2, 31.3, 1);
        field(0);
        c(Mat.FIELD_RAW, 3, 32.4, 8, -0.5, 1);
        box(Mat.FIELD_RAW, -0.6, 40.4, -0.5, 2, 7, 1);
        box(Mat.FIELD_RAW, -1.8, 47.4, -0.5, 3, 6, 1);
        c(Mat.FIELD_RAW, 1, 53.4, 5, -0.5, 1);
        // Переход I: металлический эмиттер, лезвие ровное.
        part("emitter", "base1");
        cz(Mat.STEEL, 4, 29, 3, 3);
        octY(Mat.RUNE, 7, 3, 30, 1);
        field(1);
        c(Mat.FIELD, 3, 32, 30, -0.5, 1);
        c(Mat.FIELD, 1, 62, 2, -0.5, 1);
        // Переход II: шире эмиттер, латунная губа, лезвие длиннее.
        part("emitter", "base2");
        cz(Mat.GUNMETAL, 6, 29, 3, 3);
        cz(Mat.BRASS, 4, 31.6, 1, 2).inflate(0.1);
        field(2);
        c(Mat.FIELD, 3, 32.6, 33, -0.5, 1);
        c(Mat.FIELD, 1, 65.6, 3, -0.5, 1);
        // Финал: цельная головка со щёчками, кристалл — сердце эмиттера, самое длинное лезвие.
        part("emitter", "base3");
        cz(Mat.GUNMETAL, 6, 29, 3, 4);
        c(Mat.GUNMETAL, 2, 32, 5, -1.6, 1);
        c(Mat.GUNMETAL, 2, 32, 5, 0.6, 1);
        box(Mat.CRYSTAL, -1, 29.5, -2.4, 2, 2, 1);
        field(3);
        c(Mat.FIELD, 3, 32, 38, -0.5, 1);
        c(Mat.FIELD, 1, 70, 3, -0.5, 1);
        // «Кромка»: башмаки и светящиеся кромки вдоль лезвия.
        part("emitter", "add1");
        both(Mat.BRASS, 1.6, 32, -0.5, 1, 2, 1);
        part("emitter", "add2");
        both(Mat.ARC, 1.6, 34, -0.5, 1, 6, 1);
        part("emitter", "add3");
        both(Mat.ARC, 1.6, 40, -0.5, 1, 6, 1);
        both(Mat.ARC, 2.6, 36, -0.5, 1, 1, 1);
        both(Mat.ARC, 2.6, 42, -0.5, 1, 1, 1);
        // Вершина «Волна поля»: рога-излучатели по бокам головки.
        part("emitter", "apex");
        both(Mat.BRASS, 3.2, 29.5, -0.5, 1, 4, 1);
        both(Mat.ARC, 4.2, 31.5, -0.5, 1, 3, 1);
    }

    static void drive() {
        bone("drive", "blade", 0, 21, 0);
        // Прототип: перекладина с рунными пластинами, кожаная обмотка сердечника.
        part("drive", "base0");
        cz(Mat.STEEL, 10, 20, 2, 2);
        both(Mat.RUNE, 5, 19.5, -1.5, 1, 3, 3);
        cz(Mat.LEATHER, 4, 19.5, 1, 3).inflate(0.3);
        // Переход I: длиннее перекладина, латунные торцы, руны ближе к центру.
        part("drive", "base1");
        cz(Mat.STEEL, 12, 20, 2, 2);
        both(Mat.BRASS, 6, 19.5, -1.5, 1, 3, 3);
        both(Mat.RUNE, 3, 19.5, -1.5, 1, 3, 3);
        // Переход II: корпус привода, сервоблоки на концах.
        part("drive", "base2");
        cz(Mat.GUNMETAL, 12, 19.5, 3, 4);
        both(Mat.STEEL, 6, 19, -1.5, 2, 4, 3);
        // Финал: цельная гарда с латунными торцами.
        part("drive", "base3");
        cz(Mat.GUNMETAL, 14, 18.9, 4, 4);
        both(Mat.BRASS, 7, 19.4, -1.5, 1, 3, 3);
        // «Рывок»: ускорители за гардой.
        part("drive", "add1");
        box(Mat.STEEL, 3, 20, 2.2, 2, 2, 2);
        part("drive", "add2");
        box(Mat.STEEL, -5, 20, 2.2, 2, 2, 2);
        part("drive", "add3");
        box(Mat.ARC, 3.5, 19.4, 2.7, 1, 1, 1);
        box(Mat.ARC, -4.5, 19.4, 2.7, 1, 1, 1);
        // Вершина «Сквозной рывок»: загнутые вверх крылья гарды.
        part("drive", "apex");
        both(Mat.BRASS, 8.2, 21, -0.5, 1, 5, 1);
        both(Mat.ARC, 8.2, 26, -0.5, 1, 1, 1);
    }

    static void battery() {
        bone("battery", "blade", 0, 3, 0);
        // Прототип: кристалл в латунной клетке.
        part("battery", "base0");
        cz(Mat.BRASS, 4, 0, 1, 4);
        cz(Mat.BRASS, 4, 5, 1, 4);
        cz(Mat.CRYSTAL, 2, 1, 4, 2);
        both(Mat.STEEL, 1, 1, -2, 1, 4, 1);
        both(Mat.STEEL, 1, 1, 1, 1, 4, 1);
        // Переход I: круглая ячейка с окном.
        part("battery", "base1");
        octY(Mat.BRASS, 5, 2, 0, 1);
        octY(Mat.STEEL, 5, 2, 1, 4);
        octY(Mat.BRASS, 5, 2, 5, 1);
        box(Mat.CRYSTAL, -0.5, 2, -2.8, 1, 2, 1);
        // Переход II: широкие щёчки, окна с двух сторон.
        part("battery", "base2");
        octY(Mat.BRASS, 7, 3, 0, 1);
        octY(Mat.STEEL, 5, 2, 1, 4);
        octY(Mat.BRASS, 7, 3, 5, 1);
        box(Mat.CRYSTAL, -0.5, 2, -2.8, 1, 2, 1);
        box(Mat.CRYSTAL, -0.5, 2, 1.8, 1, 2, 1);
        // Финал: цельное навершие с латунным поясом.
        part("battery", "base3");
        octY(Mat.GUNMETAL, 7, 3, 0, 6);
        octY(Mat.BRASS, 8, 3, 2.5, 1);
        box(Mat.CRYSTAL, -0.5, 4, -3.7, 1, 1, 1);
        // «Рекуперация»: шина, кольцо-уловитель, кристалл преобразователя.
        part("battery", "add1");
        box(Mat.BRASS, 3.8, 1, -0.5, 1, 4, 1);
        part("battery", "add2");
        octY(Mat.COPPER, 8, 3, 0.8, 1);
        part("battery", "add3");
        box(Mat.CRYSTAL, -4.8, 2, -1, 1, 2, 2);
        // Вершина «Перегрузка»: вилка разрядника под навершием.
        part("battery", "apex");
        cz(Mat.BRASS, 2, -1, 1, 2);
        both(Mat.STEEL, 2, -3, -0.5, 1, 3, 1);
        both(Mat.ARC, 2, -4, -0.5, 1, 1, 1);
    }

    /** Гнездо с кристаллом на грани стержня: спереди (dir 0), сзади (1), справа (2), слева (3). */
    static void socket(int dir) {
        switch (dir) {
            case 0 -> {
                box(Mat.CRYSTAL, -1, 25, -2.1, 2, 3, 1);
                box(Mat.BRASS, -1.5, 24, -2.4, 3, 1, 1);
            }
            case 1 -> {
                box(Mat.CRYSTAL, -1, 25, 1.1, 2, 3, 1);
                box(Mat.BRASS, -1.5, 24, 1.4, 3, 1, 1);
            }
            case 2 -> {
                box(Mat.CRYSTAL, 1.1, 25, -1, 1, 3, 2);
                box(Mat.BRASS, 1.4, 24.05, -1.5, 1, 1, 3);
            }
            default -> {
                box(Mat.CRYSTAL, -2.1, 25, -1, 1, 3, 2);
                box(Mat.BRASS, -2.4, 24.05, -1.5, 1, 1, 3);
            }
        }
    }

    static void focus() {
        bone("focus", "blade", 0, 26, 0);
        // Прототип: одно гнездо. Переходы: второе и третье. Финал: четыре гнезда (две цепочки) в общей оправе.
        part("focus", "base0");
        socket(0);
        part("focus", "base1");
        socket(0);
        socket(1);
        part("focus", "base2");
        socket(0);
        socket(1);
        socket(2);
        part("focus", "base3");
        for (int i = 0; i < 4; i++) {
            socket(i);
        }
        octY(Mat.GUNMETAL, 7, 3, 28.1, 1);
        // «Экономия»: стойки-стабилизаторы и верхнее кольцо.
        part("focus", "add1");
        both(Mat.STEEL, 2.8, 24, -0.5, 1, 5, 1);
        part("focus", "add2");
        box(Mat.STEEL, -0.5, 24, 2.8, 1, 5, 1);
        box(Mat.STEEL, -0.5, 24, -3.8, 1, 5, 1);
        part("focus", "add3");
        octY(Mat.BRASS, 8, 3, 23.2, 1);
        // Вершина «Перехлёст»: разрядники между гнёздами.
        part("focus", "apex");
        both(Mat.ARC, 3.9, 25.5, -0.5, 1, 2, 1);
    }

    static void contour() {
        bone("contour", "blade", 0, 11, 2);
        // Прототип: осколок кристалла, привязанный к рукояти.
        part("contour", "base0");
        box(Mat.CRYSTAL, -0.5, 9, 1.6, 1, 4, 1);
        box(Mat.LEATHER, -1, 10.5, 1.3, 2, 1, 1).inflate(0.2);
        // Переход I: трубка маны на латунных хомутах.
        part("contour", "base1");
        box(Mat.MANA_TUBE, -0.5, 6.9, 2, 1, 8, 1);
        box(Mat.BRASS, -1, 7, 1.5, 2, 1, 2);
        box(Mat.BRASS, -1, 14, 1.5, 2, 1, 2);
        // Переход II: трубка длиннее, три стальных хомута.
        part("contour", "base2");
        box(Mat.MANA_TUBE, -0.5, 6.6, 2, 1, 9, 1);
        for (double y : new double[]{6.5, 10.5, 14.5}) {
            box(Mat.STEEL, -1, y, 1.5, 2, 1, 2);
        }
        // Финал: кожух, трубка видна в прорези.
        part("contour", "base3");
        box(Mat.GUNMETAL, -1, 6.5, 1.5, 2, 9, 2);
        box(Mat.MANA_TUBE, -0.5, 7.5, 3.1, 1, 7, 1);
        // «Впитывание»: приёмные шипы по бокам контура.
        part("contour", "add1");
        box(Mat.BRASS, 1.1, 8, 2, 2, 1, 1);
        part("contour", "add2");
        box(Mat.BRASS, -3.1, 11, 2, 2, 1, 1);
        part("contour", "add3");
        box(Mat.CRYSTAL, 3.1, 8, 2, 1, 1, 1);
        box(Mat.CRYSTAL, -4.1, 11, 2, 1, 1, 1);
        // Вершина «Запас прочности»: светящееся кольцо вокруг рукояти.
        part("contour", "apex");
        octY(Mat.PULSE, 7, 3, 12, 1);
    }

    static void sync() {
        bone("sync", "blade", 0, 17.5, 0);
        // Прототип: рунное кольцо на кожаной подмотке.
        part("sync", "base0");
        octY(Mat.RUNE, 5, 2, 17, 1);
        cz(Mat.LEATHER, 2, 16, 1, 2).inflate(0.4);
        // Переход I: стальная муфта, руна поверх.
        part("sync", "base1");
        octY(Mat.STEEL, 5, 2, 16, 3);
        octY(Mat.RUNE, 7, 3, 17, 1);
        // Переход II: два латунных пояса.
        part("sync", "base2");
        octY(Mat.STEEL, 5, 2, 16, 3);
        octY(Mat.BRASS, 7, 3, 16.2, 1);
        octY(Mat.BRASS, 7, 3, 17.8, 1);
        // Финал: цельный узел, два индикатора — электричество спереди, мана сзади.
        part("sync", "base3");
        octY(Mat.GUNMETAL, 7, 3, 16, 3);
        box(Mat.LAMP, -0.5, 17, -3.9, 1, 1, 1);
        box(Mat.CRYSTAL, -0.5, 17, 2.9, 1, 1, 1);
        // «Отдача»: рёбра по бокам и лампы на них.
        part("sync", "add1");
        both(Mat.BRASS, 3.6, 16.5, -0.5, 1, 2, 1);
        part("sync", "add2");
        both(Mat.BRASS, 4.6, 16, -0.5, 1, 3, 1);
        part("sync", "add3");
        both(Mat.LAMP, 5.6, 17, -0.5, 1, 1, 1);
        // Вершина «Перелив»: перемычка от мана-контура к гарде.
        part("sync", "apex");
        box(Mat.MANA_TUBE, -0.5, 15, 4.2, 1, 5, 1);
        box(Mat.ARC, -0.5, 20, 4.2, 1, 1, 1);
    }

    // Анимации: ignite — поле вырастает из эмиттера при включении; idle — лёгкая пульсация поля.
    static final String BLADE_ANIMATIONS = """
            {
              "format_version": "1.8.0",
              "animations": {
                "ignite": {
                  "animation_length": 0.35,
                  "bones": {
                    "emitter_base0_field": {"scale": {"0.0": [1, 0, 1], "0.25": [1.15, 1.05, 1], "0.35": [1, 1, 1]}},
                    "emitter_base1_field": {"scale": {"0.0": [1, 0, 1], "0.25": [1.15, 1.05, 1], "0.35": [1, 1, 1]}},
                    "emitter_base2_field": {"scale": {"0.0": [1, 0, 1], "0.25": [1.15, 1.05, 1], "0.35": [1, 1, 1]}},
                    "emitter_base3_field": {"scale": {"0.0": [1, 0, 1], "0.25": [1.15, 1.05, 1], "0.35": [1, 1, 1]}}
                  }
                },
                "idle": {
                  "loop": true,
                  "animation_length": 1.2,
                  "bones": {
                    "emitter_base0_field": {"scale": {"0.0": [1, 1, 1], "0.15": [1.2, 0.97, 1], "0.3": [0.9, 1.02, 1], "0.6": [1.15, 0.98, 1], "0.9": [0.95, 1.01, 1], "1.2": [1, 1, 1]}},
                    "emitter_base1_field": {"scale": {"0.0": [1, 1, 1], "0.6": [1.06, 1, 1], "1.2": [1, 1, 1]}},
                    "emitter_base2_field": {"scale": {"0.0": [1, 1, 1], "0.6": [1.04, 1, 1], "1.2": [1, 1, 1]}},
                    "emitter_base3_field": {"scale": {"0.0": [1, 1, 1], "0.6": [1.03, 1, 1], "1.2": [1, 1, 1]}}
                  }
                }
              }
            }
            """;

    /** Лист: столбцы — состояния, клинок стоит лезвием вверх. */
    static BufferedImage bladeSheet(String title, List<Shot> shots) {
        int cw = 250, ch = 560, head = 34;
        BufferedImage out = new BufferedImage(cw * shots.size(), head + ch + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1A1720));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(new Color(0xE8E0D0));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString(title, 12, 23);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            g.drawImage(render(s.look(), 205, 12, 6.6, cw - 2, ch - 2), i * cw, head, null);
            g.setColor(new Color(0xB08D57));
            g.drawRect(i * cw, head, cw - 2, ch - 2);
            g.setColor(new Color(0xE8E0D0));
            int line = 0;
            for (String part : s.label().split("\n")) {
                g.drawString(part, i * cw + 8, head + ch + 14 + line++ * 15);
            }
        }
        g.dispose();
        return out;
    }

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "core/src/main/resources/assets/adept_core");
        Path preview = args.length > 1 ? Path.of(args[1]) : null;
        NAME = "energy_blade";
        SEGMENTS = new String[]{"emitter", "drive", "battery", "focus", "contour", "sync"};
        CENTER = new double[]{0, 35, 0};
        buildBlade();
        pack();
        paintTextures();
        checkCoplanar();
        Files.createDirectories(assets.resolve("geo/item"));
        Files.createDirectories(assets.resolve("textures/artifact"));
        Files.createDirectories(assets.resolve("animations/item"));
        Files.writeString(assets.resolve("geo/item/energy_blade.geo.json"), geoJson(), StandardCharsets.UTF_8);
        Files.writeString(assets.resolve("animations/item/energy_blade.animation.json"), BLADE_ANIMATIONS, StandardCharsets.UTF_8);
        ImageIO.write(texture, "png", assets.resolve("textures/artifact/energy_blade.png").toFile());
        ImageIO.write(glowmask, "png", assets.resolve("textures/artifact/energy_blade_glowmask.png").toFile());
        System.out.println("Кубов: " + CUBES.size() + ", костей: " + BONES.size() + ", текстура " + texW + "×" + texH);
        if (preview != null) {
            Files.createDirectories(preview);
            Look proto = Look.of(0, 0, false);
            ImageIO.write(bladeSheet("Энергоклинок: облик по ступеням (все сегменты разом)", List.of(
                    new Shot("Прототип", proto),
                    new Shot("Оба пути I", Look.of(1, 1, false)),
                    new Shot("Оба пути II", Look.of(2, 2, false)),
                    new Shot("Оба пути III и вершины", Look.of(3, 3, true)))),
                    "png", preview.resolve("blade-stages.png").toFile());
            ImageIO.write(bladeSheet("Энергоклинок: пути по отдельности", List.of(
                    new Shot("Только основа III", Look.of(3, 0, false)),
                    new Shot("Только детали III\n(основа прототипа)", Look.of(0, 3, false)),
                    new Shot("Прототип с вершинами", Look.of(0, 0, true)),
                    new Shot("Частично: эмиттер III/I,\nпривод I/III, фокус II/II", proto.with(0, 3, 1, true).with(1, 1, 3, false).with(3, 2, 2, false)))),
                    "png", preview.resolve("blade-mix.png").toFile());
        }
    }
}
