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
 * Генератор модели тяжёлой силовой брони для GeckoLib (броня на игроке). Общие инструменты — в {@link GaussModel}.
 *
 * Запуск (JDK 22+, файлы лежат рядом): {@code java tools/models/ArmorModel.java <assets> [preview]}.
 *
 * Единица модели — 1/16 блока, как у модели игрока; кости {@code armorHead}, {@code armorBody}, {@code armorRightArm},
 * {@code armorLeftArm}, {@code armorRightLeg}, {@code armorLeftLeg} — те, что ждёт рендер брони GeckoLib (правая
 * сторона игрока — отрицательный X). Части сегмента лежат прямо в кости своей части тела; у рук и ног каждая часть
 * есть в двух костях с окончаниями {@code _l} и {@code _r}. Зоны: шлем — голова; нагрудник — грудь и спина выше пояса;
 * рунный узел — пояс; реактор — ранец на спине; руки и ноги — конечности.
 */
public class ArmorModel extends GaussModel {
    static void on(String bone, String part) {
        Bone p = BONES.get(bone);
        bone(part, bone, p.px(), p.py(), p.pz());
    }

    /** Часть на обеих конечностях: строится для левой (X > 0) и зеркалится на правую. */
    static void sides(String part, String limb, Runnable left) {
        on("armorLeft" + limb, part + "_l");
        int from = CUBES.size();
        left.run();
        int to = CUBES.size();
        on("armorRight" + limb, part + "_r");
        for (int i = from; i < to; i++) {
            Cube q = CUBES.get(i);
            box(q.mat, -q.x - q.w, q.y, q.z, q.w, q.h, q.d).inflate(q.inflate);
        }
    }

    static void buildArmor() {
        bone("armorHead", null, 0, 24, 0);
        bone("armorBody", null, 0, 24, 0);
        bone("armorRightArm", null, -5, 22, 0);
        bone("armorLeftArm", null, 5, 22, 0);
        bone("armorRightLeg", null, -2, 12, 0);
        bone("armorLeftLeg", null, 2, 12, 0);
        helmet();
        chest();
        arms();
        legs();
        reactor();
        rune();
    }

    static void helmet() {
        // Прототип: отдельные пластины на рунных скобах, голова видна в просветах.
        on("armorHead", "helmet_base0");
        box(Mat.STEEL, -4.5, 32.2, -4.5, 9, 1, 9);
        box(Mat.STEEL, -4, 28, -5.2, 8, 3, 1);
        both(Mat.STEEL, 4.2, 25, -3, 1, 6, 6);
        box(Mat.STEEL, -3, 25, 4.2, 6, 6, 1);
        both(Mat.RUNE, 3.6, 31.2, -4.8, 1, 1, 1);
        both(Mat.RUNE, 3.6, 31.2, 3.8, 1, 1, 1);
        // Переход I: закрытый шлем, рунная полоса над забралом.
        on("armorHead", "helmet_base1");
        box(Mat.GUNMETAL, -4, 24, -4, 8, 8, 8).inflate(0.7);
        box(Mat.ARC, -3, 27.5, -5.0, 6, 1, 1);
        box(Mat.RUNE, -3, 30, -4.9, 6, 1, 1);
        // Переход II: подбородник и гребень, руны только у висков.
        on("armorHead", "helmet_base2");
        box(Mat.GUNMETAL, -4, 24, -4, 8, 8, 8).inflate(0.7);
        box(Mat.ARC, -3, 27.5, -5.0, 6, 1, 1);
        box(Mat.STEEL, -3, 24, -5.4, 6, 2, 1);
        box(Mat.STEEL, -1, 32.8, -4, 2, 1, 8);
        both(Mat.RUNE, 4.8, 29, -1, 1, 1, 2);
        // Финал: цельный шлем с латунным гребнем и ушными блоками.
        on("armorHead", "helmet_base3");
        box(Mat.GUNMETAL, -4, 24, -4, 8, 8, 8).inflate(0.9);
        box(Mat.ARC, -3, 27.5, -5.2, 6, 1, 1);
        box(Mat.GUNMETAL, -3.5, 23.6, -5.6, 7, 3, 1);
        box(Mat.BRASS, -1, 33, -4.5, 2, 1, 9);
        both(Mat.STEEL, 5, 26, -1.5, 1, 3, 3);
        // «Очистка»: фильтры по бокам и блок на затылке.
        on("armorHead", "helmet_add1");
        box(Mat.BRASS, 5.3, 24.4, -4, 1, 2, 2);
        on("armorHead", "helmet_add2");
        box(Mat.BRASS, -6.3, 24.4, -4, 1, 2, 2);
        on("armorHead", "helmet_add3");
        box(Mat.STEEL, -1, 24.5, 5.3, 2, 2, 1);
        box(Mat.LAMP, -0.5, 26.5, 5.3, 1, 1, 1);
        // Вершина «Аварийный режим»: красный маяк на макушке.
        on("armorHead", "helmet_apex");
        box(Mat.BRASS, -1, 34, 0, 2, 1, 2);
        box(Mat.LAMP, -0.5, 35, 0.5, 1, 1, 1);
    }

    static void chest() {
        // Прототип: нагрудная и спинная пластины на ремнях, руны по углам.
        on("armorBody", "chest_base0");
        box(Mat.STEEL, -3.5, 16, -3.2, 7, 7, 1);
        box(Mat.STEEL, -3.5, 16, 2.2, 7, 7, 1);
        both(Mat.LEATHER, 2, 23.3, -3, 2, 1, 6);
        both(Mat.RUNE, 2.6, 22.2, -3.5, 1, 1, 1);
        both(Mat.RUNE, 2.6, 16, -3.5, 1, 1, 1);
        // Переход I: кираса, рунный шов по центру.
        on("armorBody", "chest_base1");
        box(Mat.GUNMETAL, -4, 15, -2, 8, 9, 4).inflate(0.6);
        box(Mat.RUNE, -0.5, 19, -2.9, 1, 4, 1);
        // Переход II: воротник, гидравлика по бокам, руны у ключиц.
        on("armorBody", "chest_base2");
        box(Mat.GUNMETAL, -4, 15, -2, 8, 9, 4).inflate(0.6);
        box(Mat.STEEL, -4, 23, -3.4, 8, 2, 1);
        box(Mat.STEEL, -4, 23, 2.4, 8, 2, 1);
        both(Mat.STEEL, 4.7, 15.5, -0.5, 1, 6, 1);
        both(Mat.RUNE, 2.5, 21, -2.9, 1, 1, 1);
        // Финал: цельная грудь с плитами, руны ушли на сами пластины.
        on("armorBody", "chest_base3");
        box(Mat.GUNMETAL, -4, 15, -2, 8, 9, 4).inflate(0.8);
        both(Mat.GUNMETAL, 0.3, 19, -3.7, 3, 4, 1);
        box(Mat.STEEL, -2.5, 15.2, -3.4, 5, 3, 1);
        box(Mat.GUNMETAL, -4, 23.2, -3.6, 8, 2, 1);
        box(Mat.GUNMETAL, -4, 23.2, 2.6, 8, 2, 1);
        // «Ремонт»: два картриджа у живота и трубка маны к ним.
        on("armorBody", "chest_add1");
        box(Mat.BRASS, 2.9, 15.5, -3.8, 1, 2, 1);
        on("armorBody", "chest_add2");
        box(Mat.BRASS, -3.9, 15.5, -3.8, 1, 2, 1);
        on("armorBody", "chest_add3");
        box(Mat.MANA_TUBE, 3.7, 17.6, -3.6, 1, 4, 1);
        // Вершина «Перераспределение»: шина через грудь с разрядниками на концах.
        on("armorBody", "chest_apex");
        box(Mat.BRASS, -4.5, 23.4, -4.7, 9, 1, 1);
        both(Mat.ARC, 3.5, 22.4, -4.7, 1, 1, 1);
    }

    static void arms() {
        // Прототип: наплечная пластина, щитки плеча и предплечья на ремне, руна.
        sides("arms_base0", "Arm", () -> {
            box(Mat.STEEL, 3.8, 23.2, -2.5, 5, 1, 5);
            box(Mat.STEEL, 8.2, 17, -2, 1, 5, 4);
            box(Mat.STEEL, 8.2, 12.5, -1.5, 1, 3, 3);
            box(Mat.RUNE, 8.3, 22.2, -0.5, 1, 1, 1);
            box(Mat.LEATHER, 4, 16, -2, 4, 1, 4).inflate(0.3);
        });
        // Переход I: наплечник и перчатка, рунная полоса на плече.
        sides("arms_base1", "Arm", () -> {
            box(Mat.GUNMETAL, 4, 21, -2, 5, 3, 4).inflate(0.6);
            box(Mat.GUNMETAL, 4, 12, -2, 4, 5, 4).inflate(0.5);
            box(Mat.RUNE, 9.7, 21.5, -1.5, 1, 1, 3);
        });
        // Переход II: обод наплечника и сервопривод локтя.
        sides("arms_base2", "Arm", () -> {
            box(Mat.GUNMETAL, 4, 21, -2, 5, 3, 4).inflate(0.6);
            box(Mat.GUNMETAL, 4, 12, -2, 4, 5, 4).inflate(0.5);
            box(Mat.STEEL, 3.6, 20, -3, 6, 1, 6);
            box(Mat.STEEL, 8.2, 17.5, -1, 1, 2, 2);
        });
        // Финал: цельный рукав и большой наплечник с латунной накладкой.
        sides("arms_base3", "Arm", () -> {
            box(Mat.GUNMETAL, 4, 12, -2, 4, 12, 4).inflate(0.6);
            box(Mat.GUNMETAL, 3.6, 21.4, -3, 6, 3, 6);
            box(Mat.BRASS, 9.7, 22, -1.5, 1, 1, 3);
        });
        // «Перехват»: щиток на предплечье, второй слой, излучатель.
        sides("arms_add1", "Arm", () -> box(Mat.BRASS, 9.2, 13, -1.5, 1, 4, 3));
        sides("arms_add2", "Arm", () -> box(Mat.STEEL, 10.2, 14, -1, 1, 2, 2));
        sides("arms_add3", "Arm", () -> box(Mat.ARC, 11.2, 14.5, -0.5, 1, 1, 1));
        // Вершина «Таран»: шип на наплечнике.
        sides("arms_apex", "Arm", () -> {
            box(Mat.STEEL, 6, 24.5, -1, 2, 2, 2);
            box(Mat.BRASS, 6.5, 26.5, -0.5, 1, 1, 1);
        });
    }

    static void legs() {
        // Прототип: щитки бедра, колена и голени, руна у пояса.
        sides("legs_base0", "Leg", () -> {
            box(Mat.STEEL, 0.5, 7, -3.1, 3, 4, 1);
            box(Mat.STEEL, 1, 5, -3.4, 2, 2, 1);
            box(Mat.STEEL, 0.5, 1, -3.1, 3, 4, 1);
            box(Mat.STEEL, 4.2, 6, -1.5, 1, 5, 3);
            box(Mat.RUNE, 1.5, 11, -3.3, 1, 1, 1);
        });
        // Переход I: набедренник и понож, руна на колене.
        sides("legs_base1", "Leg", () -> {
            box(Mat.GUNMETAL, 0.2, 0, -2, 4, 6, 4).inflate(0.4);
            box(Mat.GUNMETAL, 0.2, 7, -2, 4, 5, 4).inflate(0.3);
            box(Mat.RUNE, 1.7, 6, -2.9, 1, 1, 1);
        });
        // Переход II: наколенник и поршень икры.
        sides("legs_base2", "Leg", () -> {
            box(Mat.GUNMETAL, 0.2, 0, -2, 4, 6, 4).inflate(0.4);
            box(Mat.GUNMETAL, 0.2, 7, -2, 4, 5, 4).inflate(0.3);
            box(Mat.STEEL, 1.2, 5.5, -3.3, 2, 2, 1);
            box(Mat.STEEL, 1.7, 2, 2.5, 1, 5, 1);
        });
        // Финал: цельная нога, латунный наколенник, носок ботинка.
        sides("legs_base3", "Leg", () -> {
            box(Mat.GUNMETAL, 0.3, 0, -2, 4, 12, 4).inflate(0.5);
            box(Mat.BRASS, 1.3, 5.5, -3.4, 2, 2, 1);
            box(Mat.GUNMETAL, 0.3, 0, -3.5, 4, 2, 1);
        });
        // «Мобильность»: приводы на голени и бедре, лампа готовности.
        sides("legs_add1", "Leg", () -> box(Mat.STEEL, 4.9, 2, -1, 1, 4, 2));
        sides("legs_add2", "Leg", () -> box(Mat.BRASS, 4.9, 7, -1, 1, 3, 2));
        sides("legs_add3", "Leg", () -> box(Mat.LAMP, 5.9, 3.5, -0.5, 1, 1, 1));
        // Вершина «Реактивный прыжок»: ускорители за икрами.
        sides("legs_apex", "Leg", () -> {
            box(Mat.STEEL, 0.8, 0.5, 3.7, 3, 3, 1);
            box(Mat.ARC, 1.8, -0.5, 3.7, 1, 1, 1);
        });
    }

    static void reactor() {
        // Прототип: ранец с сырой намоткой и рунной полосой.
        on("armorBody", "reactor_base0");
        box(Mat.STEEL, -2.5, 16, 3.3, 5, 6, 2);
        box(Mat.COIL_RAW, -1.5, 17, 5.3, 3, 4, 1);
        box(Mat.RUNE, -2.5, 22.2, 3.5, 5, 1, 1);
        // Переход I: корпус, две ровные катушки.
        on("armorBody", "reactor_base1");
        box(Mat.GUNMETAL, -3, 15.5, 3.2, 6, 7, 3);
        both(Mat.COIL, 0.7, 16.5, 6.2, 2, 5, 1);
        box(Mat.RUNE, -3, 22.7, 3.4, 6, 1, 1);
        // Переход II: шире, решётка охлаждения, трубы по бокам.
        on("armorBody", "reactor_base2");
        box(Mat.GUNMETAL, -3.5, 15, 3.2, 7, 8, 3);
        box(Mat.VENT, -2.5, 16, 6.2, 5, 5, 1);
        both(Mat.STEEL, 3.6, 17, 3.5, 1, 5, 1);
        // Финал: цельный реактор с красными лампами и выхлопами.
        on("armorBody", "reactor_base3");
        box(Mat.GUNMETAL, -3.5, 15, 3.2, 7, 8, 4);
        box(Mat.LAMPS, -2.5, 18, 7.2, 5, 3, 1);
        both(Mat.STEEL, 1.5, 23, 4, 2, 2, 2);
        // «Рекуперация»: шины по бокам ранца и лампы возврата.
        on("armorBody", "reactor_add1");
        box(Mat.BRASS, 4.7, 19.5, 4.5, 1, 3, 2);
        on("armorBody", "reactor_add2");
        box(Mat.BRASS, -5.7, 19.5, 4.5, 1, 3, 2);
        on("armorBody", "reactor_add3");
        both(Mat.LAMP, 4.7, 22.7, 5, 1, 1, 1);
        // Вершина «Резервные катушки»: две запасные катушки снизу по бокам.
        on("armorBody", "reactor_apex");
        both(Mat.COIL, 3.8, 15.2, 5, 2, 3, 2);
        both(Mat.BRASS, 3.8, 18.2, 5, 2, 1, 2);
    }

    static void rune() {
        // Прототип: кожаный пояс с большой рунной пластиной.
        on("armorBody", "rune_base0");
        box(Mat.LEATHER, -4, 12.5, -2, 8, 2, 4).inflate(0.4);
        box(Mat.RUNE, -2, 12, -3.4, 4, 3, 1);
        // Переход I: стальной пояс, узел в латунной оправе, руны по бокам.
        on("armorBody", "rune_base1");
        box(Mat.STEEL, -4, 12.5, -2, 8, 2, 4).inflate(0.5);
        box(Mat.BRASS, -1.5, 12.2, -3.5, 3, 3, 1);
        box(Mat.CRYSTAL, -0.5, 13.2, -3.9, 1, 1, 1);
        both(Mat.RUNE, 3, 13, -2.9, 1, 1, 1);
        // Переход II: тёмная оправа, латунные клеммы.
        on("armorBody", "rune_base2");
        box(Mat.STEEL, -4, 12.5, -2, 8, 2, 4).inflate(0.5);
        box(Mat.GUNMETAL, -1.5, 12.2, -3.5, 3, 3, 1);
        box(Mat.CRYSTAL, -0.5, 13.2, -3.9, 1, 1, 1);
        both(Mat.BRASS, 1.6, 13.2, -3.3, 1, 1, 1);
        // Финал: компактный узел, кристалл в центре.
        on("armorBody", "rune_base3");
        box(Mat.GUNMETAL, -4, 12.5, -2, 8, 2, 4).inflate(0.6);
        box(Mat.GUNMETAL, -1, 12.6, -3.7, 2, 2, 1);
        box(Mat.CRYSTAL, -0.5, 13.1, -4.1, 1, 1, 1);
        // «Насыщение»: трубки маны вдоль пояса.
        on("armorBody", "rune_add1");
        box(Mat.MANA_TUBE, 2, 13.4, -3.1, 2, 1, 1);
        on("armorBody", "rune_add2");
        box(Mat.MANA_TUBE, -4, 13.4, -3.1, 2, 1, 1);
        on("armorBody", "rune_add3");
        box(Mat.MANA_TUBE, -2, 13.4, 2.1, 4, 1, 1);
        // Вершина «Антимагическое поле»: излучатели на бёдрах.
        on("armorBody", "rune_apex");
        both(Mat.BRASS, 4.9, 11.5, -0.5, 1, 3, 1);
        both(Mat.CRYSTAL, 4.9, 14.5, -0.5, 1, 1, 1);
    }

    /** Манекен игрока для предпросмотра; добавляется после записи модели и в файлы не попадает. */
    static void mannequin() {
        bone("dummy", null, 0, 0, 0);
        box(Mat.DUMMY, -4, 24, -4, 8, 8, 8);
        box(Mat.DUMMY, -4, 12, -2, 8, 12, 4);
        both(Mat.DUMMY, 4, 12, -2, 4, 12, 4);
        both(Mat.DUMMY, 0, 0, -2, 4, 12, 4);
    }

    /** Лист: столбцы — состояния, верхний ряд — спереди, нижний — сзади. */
    static BufferedImage armorSheet(String title, List<Shot> shots) {
        int cw = 250, ch = 400, head = 34;
        BufferedImage out = new BufferedImage(cw * shots.size(), head + ch * 2 + 40, BufferedImage.TYPE_INT_RGB);
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
            g.drawImage(render(s.look(), 205, 14, 9.6, cw - 2, ch - 2), i * cw, head, null);
            g.drawImage(render(s.look(), 25, 14, 9.6, cw - 2, ch - 2), i * cw, head + ch, null);
            g.setColor(new Color(0xB08D57));
            g.drawRect(i * cw, head, cw - 2, ch - 2);
            g.drawRect(i * cw, head + ch, cw - 2, ch - 2);
            g.setColor(new Color(0xE8E0D0));
            int line = 0;
            for (String part : s.label().split("\n")) {
                g.drawString(part, i * cw + 8, head + ch * 2 + 14 + line++ * 15);
            }
        }
        g.dispose();
        return out;
    }

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "core/src/main/resources/assets/adept_core");
        Path preview = args.length > 1 ? Path.of(args[1]) : null;
        NAME = "heavy_armor";
        SEGMENTS = new String[]{"helmet", "chest", "arms", "legs", "reactor", "rune"};
        CENTER = new double[]{0, 17, 0};
        buildArmor();
        pack();
        paintTextures();
        checkCoplanar();
        Files.createDirectories(assets.resolve("geo/armor"));
        Files.createDirectories(assets.resolve("textures/artifact"));
        Files.writeString(assets.resolve("geo/armor/heavy_armor.geo.json"), geoJson(), StandardCharsets.UTF_8);
        ImageIO.write(texture, "png", assets.resolve("textures/artifact/heavy_armor.png").toFile());
        ImageIO.write(glowmask, "png", assets.resolve("textures/artifact/heavy_armor_glowmask.png").toFile());
        System.out.println("Кубов: " + CUBES.size() + ", костей: " + BONES.size() + ", текстура " + texW + "×" + texH);
        if (preview != null) {
            Files.createDirectories(preview);
            mannequin();
            Look proto = Look.of(0, 0, false);
            ImageIO.write(armorSheet("Тяжёлая силовая броня: облик по ступеням (все сегменты разом)", List.of(
                    new Shot("Прототип", proto),
                    new Shot("Оба пути I", Look.of(1, 1, false)),
                    new Shot("Оба пути II", Look.of(2, 2, false)),
                    new Shot("Оба пути III и вершины", Look.of(3, 3, true)))),
                    "png", preview.resolve("armor-stages.png").toFile());
            ImageIO.write(armorSheet("Тяжёлая силовая броня: пути по отдельности", List.of(
                    new Shot("Только основа III", Look.of(3, 0, false)),
                    new Shot("Только детали III\n(основа прототипа)", Look.of(0, 3, false)),
                    new Shot("Прототип с вершинами", Look.of(0, 0, true)),
                    new Shot("Частично: шлем III/I,\nнагрудник I/III, ноги II/II", proto.with(0, 3, 1, true).with(1, 1, 3, false).with(3, 2, 2, false)))),
                    "png", preview.resolve("armor-mix.png").toFile());
        }
    }
}
