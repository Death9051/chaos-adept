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
 * Модель строится в двойном разрешении (единица — 1/32 блока, игрок: голова 16, корпус 16×24×8) и пишется в файл
 * с масштабом 0,5 и UV по граням: так хватает точности на грани. Формы гранёные, не кубические: шлем, рукава,
 * поножи и реактор — восьмигранники, у корпуса и пояса срезаны углы, плиты груди стоят с наклоном.
 * Повороты вокруг Y — только у симметричных брусков (восьмигранники, квадратные бруски под 45°).
 *
 * Кости {@code armorHead}, {@code armorBody}, {@code armorRightArm}, {@code armorLeftArm}, {@code armorRightLeg},
 * {@code armorLeftLeg} — те, что ждёт рендер брони GeckoLib (правая сторона игрока — отрицательный X). Части сегмента
 * лежат прямо в кости своей части тела; у рук и ног каждая часть есть в двух костях с окончаниями {@code _l} и
 * {@code _r}. Зоны: шлем — голова; нагрудник — грудь и спина выше пояса; рунный узел — пояс; реактор — ранец на
 * спине; руки и ноги — конечности.
 */
public class ArmorModel extends GaussModel {
    static void on(String bone, String part) {
        Bone p = BONES.get(bone);
        bone(part, bone, p.px(), p.py(), p.pz());
    }

    /** Зеркальная копия куба относительно плоскости X = 0 в текущей кости. */
    static void mirror(Cube q) {
        Cube m = box(q.mat, -q.x - q.w, q.y, q.z, q.w, q.h, q.d).inflate(q.inflate);
        m.round = q.round;
        m.capX = q.capX;
        m.capY = q.capY;
        m.axis = q.axis;
        m.deg = q.axis == 1 ? q.deg : -q.deg;
    }

    /** Куб и его зеркальная копия. */
    static void sym(Cube q) {
        mirror(q);
    }

    /** Часть на обеих конечностях: строится для левой (X > 0) и зеркалится на правую. */
    static void sides(String part, String limb, Runnable left) {
        on("armorLeft" + limb, part + "_l");
        int from = CUBES.size();
        left.run();
        int to = CUBES.size();
        on("armorRight" + limb, part + "_r");
        for (int i = from; i < to; i++) {
            mirror(CUBES.get(i));
        }
    }

    /**
     * Корпус со срезанными вертикальными углами: два бруса крестом и четыре квадратных бруска под 45° в углах.
     * X и Z — полуразмеры, срез — 2 единицы.
     */
    static void chamfered(Mat m, double y, int h, int X, int Z) {
        c(m, 2 * X, y, h, -(Z - 2), 2 * (Z - 2));
        c(m, 2 * (X - 2), y + 0.01, h, -Z, 2 * Z);
        double off = 0.7071 * 1.5;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double cx = sx * (X - 1 - off), cz = sz * (Z - 1 - off);
                box(m, cx - 1.5, y + 0.02, cz - 1.5, 3, h, 3).rotY(45);
            }
        }
    }

    static void buildArmor() {
        bone("armorHead", null, 0, 48, 0);
        bone("armorBody", null, 0, 48, 0);
        bone("armorRightArm", null, -10, 44, 0);
        bone("armorLeftArm", null, 10, 44, 0);
        bone("armorRightLeg", null, -4, 24, 0);
        bone("armorLeftLeg", null, 4, 24, 0);
        helmet();
        chest();
        arms();
        legs();
        reactor();
        rune();
    }

    /**
     * Полоса-ламель вокруг части тела: два бруса крестом, углы получаются с вырезом, а не прямыми.
     * cx, cz — центр; X, Z — полуразмеры; h — высота полосы.
     */
    static void ring(Mat m, double cx, double cz, double y, int h, double X, double Z) {
        box(m, cx - X, y, cz - (Z - 1), (int) Math.round(2 * X), h, (int) Math.round(2 * (Z - 1)));
        box(m, cx - (X - 1), y + 0.01, cz - Z, (int) Math.round(2 * (X - 1)), h, (int) Math.round(2 * Z));
    }

    /** Несколько полос одна над другой; материалы чередуются. */
    static void rings(double cx, double cz, double X, double Z, double[] ys, Mat... mats) {
        for (int i = 0; i < ys.length; i++) {
            ring(mats[i % mats.length], cx, cz, ys[i], 2, X, Z);
        }
    }

    /**
     * Скруглённый шлем: плотная оболочка по голове, накладки на гранях и, если {@code edges}, скосы по вертикальным
     * и верхним рёбрам — квадратные бруски под 45°. Лицевую накладку ставит сам вариант шлема.
     */
    static void dome(Mat m, boolean edges) {
        box(Mat.GUNMETAL, -8, 48, -8, 16, 16, 16).inflate(0.4);
        c(m, 14, 49, 14, -10, 2);
        c(m, 14, 49, 14, 8, 2);
        sym(box(m, 8, 49, -7, 2, 14, 14));
        c(m, 14, 64, 2, -7, 14);
        if (!edges) {
            return;
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                box(m, sx * 7.09 - 2, 49.02, sz * 7.09 - 2, 4, 15, 4).rotY(45);
            }
        }
        c(m, 14, 61.09, 4, -9.09, 4).rotX(45);
        c(m, 14, 61.09, 4, 5.09, 4).rotX(45);
        sym(box(m, 5.09, 61.09, -7, 4, 4, 14).rotZ(45));
    }

    static void helmet() {
        // Прототип: отдельные пластины на рунных скобах, голова видна в просветах.
        on("armorHead", "helmet_base0");
        c(Mat.STEEL, 12, 64.4, 1, -6, 12);
        c(Mat.STEEL, 12, 56, 5, -9.4, 1).rotX(-8);
        sym(box(Mat.STEEL, 8.6, 51, -5, 1, 9, 10));
        c(Mat.STEEL, 10, 52, 9, 8.6, 1);
        sym(box(Mat.RUNE, 6.5, 61.4, -9, 1, 3, 1));
        sym(box(Mat.RUNE, 6.5, 61.4, 8, 1, 3, 1));
        // Переход I: шлем с накладками на гранях, смотровая щель, рунная полоса на лбу.
        on("armorHead", "helmet_base1");
        dome(Mat.STEEL, false);
        c(Mat.ARC, 10, 54.2, 2, -10.5, 1);
        c(Mat.RUNE, 12, 59, 1, -10.5, 1);
        // Переход II: рёбра скошены, дыхательный узел, руны только у висков.
        on("armorHead", "helmet_base2");
        dome(Mat.STEEL, true);
        c(Mat.ARC, 10, 54.2, 2, -10.5, 1);
        oct(Mat.GUNMETAL, 7, 3, 0, 51, -12, 2);
        sym(box(Mat.RUNE, 9.5, 55, -2, 1, 2, 4));
        // Финал: скошенные рёбра, темя с латунным гребнем, две линзы в оправах, переносица,
        // дыхательный узел с решёткой, диски на ушах, слоёный затыльник.
        on("armorHead", "helmet_base3");
        dome(Mat.STEEL, true);
        octY(Mat.GUNMETAL, 10, 4, 0, 66, 0, 1);
        c(Mat.BRASS, 2, 67, 1, -5, 10);
        for (int sx = -1; sx <= 1; sx += 2) {
            oct(Mat.BRASS, 5, 2, sx * 3.5, 55, -10.6, 1);
            oct(Mat.ARC, 3, 1, sx * 3.5, 55, -11.2, 1);
        }
        c(Mat.GUNMETAL, 2, 53.6, 4, -10.7, 1);
        oct(Mat.GUNMETAL, 8, 3, 0, 51, -12.6, 3);
        oct(Mat.STEEL, 5, 2, 0, 51, -13.2, 1);
        octX(Mat.GUNMETAL, 7, 3, 10, 55, 0, 1);
        octX(Mat.GUNMETAL, 7, 3, -11, 55, 0, 1);
        c(Mat.STEEL, 12, 47.4, 2, 9.6, 1).rotX(-20);
        c(Mat.GUNMETAL, 14, 45.8, 2, 10.2, 1).rotX(-20);
        // «Очистка»: фильтры по бокам дыхательного узла и блок на затылке.
        on("armorHead", "helmet_add1");
        oct(Mat.BRASS, 3, 1, 6, 50.5, -13.6, 3);
        on("armorHead", "helmet_add2");
        oct(Mat.BRASS, 3, 1, -6, 50.5, -13.6, 3);
        on("armorHead", "helmet_add3");
        c(Mat.GUNMETAL, 6, 53, 4, 10.2, 2);
        box(Mat.LAMP, -1, 57.2, 10.6, 2, 1, 1);
        // Вершина «Аварийный режим»: красный маяк на макушке.
        on("armorHead", "helmet_apex");
        octY(Mat.BRASS, 5, 2, 0, 68.1, 0, 1);
        box(Mat.LAMP, -1, 69.1, -1, 2, 2, 2);
    }

    /** Грудные плиты: две половины с наклоном вперёд и скосом к плечу. d — толщина, w — ширина половины. */
    static void pecs(Mat m, int w, int d, double y, int h) {
        sym(box(m, 0.3, y, -5.4 - d, w, h, d).rotX(-12));
        sym(box(m, w - 1.6, y + 0.3, -6.6 - d / 2.0, 3, h - 1, 3).rotY(45));
    }

    static void chest() {
        // Прототип: нагрудник и спинная пластина на ремнях, две полосы на животе, руны.
        on("armorBody", "chest_base0");
        c(Mat.STEEL, 12, 38, 8, -6, 1).rotX(-10);
        c(Mat.STEEL, 10, 31, 2, -5.4, 1);
        c(Mat.STEEL, 10, 34.4, 2, -5.4, 1);
        c(Mat.STEEL, 12, 36, 10, 4.6, 1);
        sym(box(Mat.LEATHER, 3, 47, -5, 4, 1, 10));
        sym(box(Mat.RUNE, 6.2, 44.4, -6.6, 1, 2, 1));
        sym(box(Mat.RUNE, 5.2, 31, -5.6, 1, 2, 1));
        // Переход I: поддоспешник, цельный нагрудник со скосами к плечам, две полосы на животе, рунный шов.
        on("armorBody", "chest_base1");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        pecs(Mat.STEEL, 9, 2, 38.5, 8);
        rings(0, 0, 9, 5.2, new double[]{31, 34.6}, Mat.STEEL);
        c(Mat.RUNE, 2, 39, 6, -8.4, 1).rotX(-12);
        // Переход II: покатые плечи, горжет, лопатки, три полосы на животе, руны у ключиц.
        on("armorBody", "chest_base2");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        pecs(Mat.STEEL, 9, 2, 38.5, 8);
        rings(0, 0, 9, 5.2, new double[]{30.4, 33, 35.6}, Mat.STEEL, Mat.GUNMETAL);
        sym(box(Mat.STEEL, 2.6, 46.6, -4, 6, 2, 8).rotZ(12));
        c(Mat.GUNMETAL, 10, 46, 3, -7.4, 2).rotX(-25);
        sym(box(Mat.STEEL, 1, 39.5, 4.6, 7, 7, 1).rotX(8));
        sym(box(Mat.RUNE, 4, 44.6, -8.4, 2, 1, 1));
        // Финал: толстые грудные плиты с латунной грудиной, покатые плечи, горжет спереди и сзади,
        // лопатки и хребет, плотные полосы на животе.
        on("armorBody", "chest_base3");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        pecs(Mat.STEEL, 10, 3, 38.5, 8);
        c(Mat.BRASS, 2, 38, 9, -9.4, 1).rotX(-12);
        rings(0, 0, 9, 5.2, new double[]{30.2, 32.6, 35, 37.4}, Mat.STEEL, Mat.GUNMETAL);
        sym(box(Mat.STEEL, 2.6, 46.8, -4.5, 7, 2, 9).rotZ(12));
        c(Mat.GUNMETAL, 10, 46.2, 3, -8, 2).rotX(-25);
        c(Mat.GUNMETAL, 10, 46.2, 3, 5.4, 2).rotX(25);
        sym(box(Mat.STEEL, 1, 39.5, 4.8, 8, 7, 2).rotX(8));
        c(Mat.GUNMETAL, 2, 31, 16, 5.4, 1);
        // «Ремонт»: два картриджа у живота и трубка маны к ним.
        on("armorBody", "chest_add1");
        oct(Mat.BRASS, 3, 1, 5.5, 32, -7.4, 2);
        on("armorBody", "chest_add2");
        oct(Mat.BRASS, 3, 1, -5.5, 32, -7.4, 2);
        on("armorBody", "chest_add3");
        box(Mat.MANA_TUBE, 7.6, 33.4, -6.2, 1, 5, 1);
        // Вершина «Перераспределение»: шина через грудь с разрядниками на концах.
        on("armorBody", "chest_apex");
        c(Mat.BRASS, 18, 42, 1, -11.4, 1);
        sym(box(Mat.ARC, 8, 41, -11.4, 1, 3, 1));
    }

    static void arms() {
        // Прототип: наклонная наплечная пластина и три щитка снаружи руки, руна.
        sides("arms_base0", "Arm", () -> {
            box(Mat.STEEL, 9, 46.6, -5, 9, 1, 10).rotZ(15);
            for (double y : new double[]{27, 33.5, 40}) {
                box(Mat.STEEL, 16.2, y, -3, 1, 4, 6);
            }
            box(Mat.RUNE, 16.4, 44.4, -1, 1, 2, 2);
        });
        // Переход I: рукав, округлый наплечник, по две полосы на плече и предплечье, рунное кольцо.
        sides("arms_base1", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.STEEL, 12, 5, 12.6, 43, 0, 3);
            octY(Mat.STEEL, 8, 3, 12.6, 46, 0, 1);
            rings(12, 0, 5, 5, new double[]{26.4, 29.4, 36.4, 39.4}, Mat.STEEL);
            octY(Mat.RUNE, 14, 6, 12.6, 43.6, 0, 1);
        });
        // Переход II: наплечник в два яруса, наплечье и наруч цельные, локоть и запястье открыты, диск локтя.
        sides("arms_base2", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.STEEL, 14, 6, 12.6, 42.4, 0, 3);
            octY(Mat.STEEL, 12, 5, 12.6, 45.4, 0, 2);
            octY(Mat.GUNMETAL, 12, 5, 12, 35.6, 0, 6);
            octY(Mat.GUNMETAL, 12, 5, 12, 26.4, 0, 6);
            octX(Mat.STEEL, 5, 2, 16.4, 33.5, 0, 1);
        });
        // Финал: наплечник в три яруса с латунной кромкой, наплечье, наруч с раструбом к локтю,
        // диск локтя, щиток кисти.
        sides("arms_base3", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.BRASS, 15, 6, 12.6, 41.6, 0, 1);
            octY(Mat.STEEL, 14, 6, 12.6, 42.6, 0, 3);
            octY(Mat.STEEL, 12, 5, 12.6, 45.6, 0, 2);
            octY(Mat.GUNMETAL, 8, 3, 12.6, 47.6, 0, 1);
            octY(Mat.GUNMETAL, 12, 5, 12, 35.6, 0, 6);
            octY(Mat.GUNMETAL, 12, 5, 12, 26.4, 0, 5);
            octY(Mat.STEEL, 14, 6, 12, 30.4, 0, 2);
            octX(Mat.STEEL, 5, 2, 17.2, 33.8, 0, 1);
            box(Mat.STEEL, 16.3, 24.1, -3, 1, 2, 6);
        });
        // «Перехват»: щиток на наруче, второй слой, излучатель.
        sides("arms_add1", "Arm", () -> box(Mat.BRASS, 18.3, 26.5, -3, 1, 4, 6));
        sides("arms_add2", "Arm", () -> octX(Mat.STEEL, 5, 2, 19.3, 28.5, 0, 1));
        sides("arms_add3", "Arm", () -> box(Mat.ARC, 20.3, 27.5, -1, 1, 2, 2));
        // Вершина «Таран»: ступенчатый шип на наплечнике.
        sides("arms_apex", "Arm", () -> {
            octY(Mat.STEEL, 5, 2, 14.6, 48.6, 0, 2);
            octY(Mat.BRASS, 3, 1, 14.6, 50.6, 0, 2);
        });
    }

    static void legs() {
        // Прототип: щитки бедра и голени спереди, диск колена, боковая пластина, руна у пояса.
        sides("legs_base0", "Leg", () -> {
            box(Mat.STEEL, 1, 14, -5.2, 6, 7, 1);
            box(Mat.STEEL, 1, 3, -5.2, 6, 6, 1);
            oct(Mat.STEEL, 5, 2, 4, 11, -5.8, 1);
            box(Mat.STEEL, 8.2, 13, -3, 1, 8, 6);
            box(Mat.RUNE, 3, 22, -5.4, 2, 1, 1);
        });
        // Переход I: штанина, по две полосы на бедре и голени, руна на колене.
        sides("legs_base1", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            rings(4, 0, 5, 5, new double[]{4, 7, 15, 18}, Mat.STEEL);
            oct(Mat.RUNE, 5, 2, 4, 11.5, -4.8, 1);
        });
        // Переход II: набедренник и понож цельные, колено и щиколотка открыты, диск колена, носок.
        sides("legs_base2", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 14, 0, 8);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 3.6, 0, 6);
            oct(Mat.STEEL, 8, 3, 4.3, 11.5, -6.2, 2);
            box(Mat.STEEL, 0.6, 0, -7, 7, 3, 3);
        });
        // Финал: набедренник с передней плитой, диск колена со ступицей, понож с ребром и объёмной икрой,
        // полоса на щиколотке, ботинок со скошенным носком и пяткой.
        sides("legs_base3", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 14, 0, 8);
            box(Mat.STEEL, 1.6, 15, -7.2, 6, 6, 1).rotX(-6);
            oct(Mat.STEEL, 8, 3, 4.3, 11.5, -6.2, 2);
            oct(Mat.BRASS, 5, 2, 4.3, 11.5, -6.8, 1);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 3.6, 0, 6);
            box(Mat.STEEL, 3.6, 4, -7.4, 2, 5, 2).rotY(45);
            box(Mat.STEEL, 2.6, 4.6, 5.6, 4, 4, 2).rotX(8);
            ring(Mat.STEEL, 4, 0, 0.6, 2, 5, 5);
            box(Mat.GUNMETAL, 0.6, 0, -7.6, 7, 3, 3);
            box(Mat.GUNMETAL, 0.7, 1.4, -7.4, 7, 3, 3).rotX(45);
            box(Mat.GUNMETAL, 1.6, 0, 4.4, 5, 3, 1);
        });
        // «Мобильность»: приводы на голени и бедре, лампа готовности.
        sides("legs_add1", "Leg", () -> box(Mat.STEEL, 10.8, 4, -2, 1, 5, 4));
        sides("legs_add2", "Leg", () -> octX(Mat.BRASS, 5, 2, 10.8, 18, 0, 1));
        sides("legs_add3", "Leg", () -> box(Mat.LAMP, 11.8, 5.5, -1, 1, 2, 2));
        // Вершина «Реактивный прыжок»: сопла за икрами.
        sides("legs_apex", "Leg", () -> {
            octY(Mat.STEEL, 5, 2, 4.6, 3.6, 10, 6);
            octY(Mat.ARC, 3, 1, 4.6, 2.6, 10, 1);
        });
    }

    /** Сдвиг ранца назад от спины: под ним лопатки. */
    static final double RZ = 1.4;

    static void reactor() {
        // Прототип: короб с диском сырой намотки и рунной полосой.
        on("armorBody", "reactor_base0");
        c(Mat.STEEL, 10, 32, 12, 6.2 + RZ, 4);
        oct(Mat.COIL_RAW, 7, 3, 0, 38, 10.2 + RZ, 2);
        c(Mat.RUNE, 10, 44.6, 3, 7 + RZ, 3);
        // Переход I: корпус, две вертикальные катушки в латунных торцах, руны на бортах.
        on("armorBody", "reactor_base1");
        c(Mat.GUNMETAL, 12, 31, 14, 6.2 + RZ, 5);
        for (int sx = -1; sx <= 1; sx += 2) {
            octY(Mat.COPPER, 5, 2, sx * 3, 33.5, 12.6 + RZ, 8);
            octY(Mat.BRASS, 5, 2, sx * 3, 32.5, 12.6 + RZ, 1);
            octY(Mat.BRASS, 5, 2, sx * 3, 41.5, 12.6 + RZ, 1);
        }
        sym(box(Mat.RUNE, 6.1, 36, 7.5 + RZ, 1, 5, 3));
        // Переход II: восьмигранный барабан с решёткой, трубы по бокам.
        on("armorBody", "reactor_base2");
        oct(Mat.GUNMETAL, 14, 6, 0, 38.5, 6.2 + RZ, 5);
        oct(Mat.STEEL, 10, 4, 0, 38.5, 11.2 + RZ, 1);
        sym(box(Mat.STEEL, 7.2, 33.5, 7 + RZ, 1, 10, 2));
        // Финал: большой барабан, латунное кольцо, красный глаз реактора, выхлопы сверху.
        on("armorBody", "reactor_base3");
        oct(Mat.GUNMETAL, 17, 7, 0, 38.5, 6.2 + RZ, 6);
        oct(Mat.BRASS, 12, 5, 0, 38.5, 12.2 + RZ, 1);
        oct(Mat.LAMP, 7, 3, 0, 38.5, 12.5 + RZ, 1);
        octY(Mat.STEEL, 5, 2, 5.5, 46.6, 9 + RZ, 3);
        octY(Mat.STEEL, 5, 2, -5.5, 46.6, 9 + RZ, 3);
        // «Рекуперация»: шины по бокам ранца и лампы возврата.
        on("armorBody", "reactor_add1");
        box(Mat.BRASS, 8.8, 35, 7.5 + RZ, 1, 7, 3);
        on("armorBody", "reactor_add2");
        box(Mat.BRASS, -9.8, 35, 7.5 + RZ, 1, 7, 3);
        on("armorBody", "reactor_add3");
        sym(box(Mat.LAMP, 8.8, 43, 8.5 + RZ, 1, 1, 1));
        // Вершина «Резервные катушки»: две запасные катушки под барабаном.
        on("armorBody", "reactor_apex");
        for (int sx = -1; sx <= 1; sx += 2) {
            octY(Mat.COPPER, 5, 2, sx * 3.5, 25.6, 10.5 + RZ, 4);
            octY(Mat.BRASS, 7, 3, sx * 3.5, 24.6, 10.5 + RZ, 1);
        }
    }

    /** Набедренные щитки: спереди и по бокам, низ отведён от тела. */
    static void tassets(Mat m) {
        c(m, 8, 19.6, 5, -6, 1).rotX(-10);
        sym(box(m, 8.7, 19.6, -3, 1, 5, 6).rotZ(-10));
    }

    static void rune() {
        // Прототип: кожаный пояс с большой восьмигранной рунной пряжкой.
        on("armorBody", "rune_base0");
        ring(Mat.LEATHER, 0, 0, 25, 4, 9, 5.2);
        oct(Mat.RUNE, 10, 4, 0, 27, -6.3, 1);
        // Переход I: стальной пояс, узел в латунной оправе, руны по бокам, набедренные щитки.
        on("armorBody", "rune_base1");
        ring(Mat.STEEL, 0, 0, 25, 4, 9, 5.2);
        oct(Mat.BRASS, 7, 3, 0, 27, -6.2, 1);
        box(Mat.CRYSTAL, -1, 26, -6.8, 2, 2, 1);
        sym(box(Mat.RUNE, 5.5, 26, -5.6, 2, 2, 1));
        tassets(Mat.STEEL);
        // Переход II: тёмная оправа, латунные клеммы.
        on("armorBody", "rune_base2");
        ring(Mat.STEEL, 0, 0, 25, 4, 9, 5.2);
        oct(Mat.GUNMETAL, 7, 3, 0, 27, -6.2, 1);
        box(Mat.CRYSTAL, -1, 26, -6.8, 2, 2, 1);
        sym(box(Mat.BRASS, 4.5, 26.5, -5.6, 1, 1, 1));
        tassets(Mat.STEEL);
        // Финал: пояс из двух полос, компактный узел, кристалл в центре, набедренные щитки в два слоя.
        on("armorBody", "rune_base3");
        ring(Mat.GUNMETAL, 0, 0, 24.6, 2, 9, 5.2);
        ring(Mat.STEEL, 0, 0, 27, 2, 9.5, 5.5);
        oct(Mat.GUNMETAL, 5, 2, 0, 27, -6.5, 1);
        box(Mat.CRYSTAL, -1, 26, -7.1, 2, 2, 1);
        tassets(Mat.STEEL);
        c(Mat.GUNMETAL, 10, 17.4, 3, -6.6, 1).rotX(-10);
        // «Насыщение»: трубки маны вдоль пояса.
        on("armorBody", "rune_add1");
        box(Mat.MANA_TUBE, 4, 25.4, -6.3, 4, 1, 1);
        on("armorBody", "rune_add2");
        box(Mat.MANA_TUBE, -8, 25.4, -6.3, 4, 1, 1);
        on("armorBody", "rune_add3");
        box(Mat.MANA_TUBE, -4, 25.6, 5.4, 8, 1, 1);
        // Вершина «Антимагическое поле»: излучатели на бёдрах.
        on("armorBody", "rune_apex");
        sym(box(Mat.BRASS, 10.6, 23, -1, 1, 6, 2));
        sym(box(Mat.CRYSTAL, 10.6, 29, -1, 1, 2, 2));
    }

    /** Манекен игрока для предпросмотра; добавляется после записи модели и в файлы не попадает. */
    static void mannequin() {
        bone("dummy", null, 0, 0, 0);
        box(Mat.DUMMY, -8, 48, -8, 16, 16, 16);
        box(Mat.DUMMY, -8, 24, -4, 16, 24, 8);
        both(Mat.DUMMY, 8, 24, -4, 8, 24, 8);
        both(Mat.DUMMY, 0, 0, -4, 8, 24, 8);
    }

    /** Лист: столбцы — состояния, верхний ряд — спереди, нижний — сзади. */
    static BufferedImage armorSheet(String title, List<Shot> shots) {
        return armorSheet(title, shots, 4.7);
    }

    static BufferedImage armorSheet(String title, List<Shot> shots, double scale) {
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
            g.drawImage(render(s.look(), 205, 14, scale, cw - 2, ch - 2), i * cw, head, null);
            g.drawImage(render(s.look(), 25, 14, scale, cw - 2, ch - 2), i * cw, head + ch, null);
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
        CENTER = new double[]{0, 35, 0};
        GEO_SCALE = 0.5;
        FRONT_TONE = 2;
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
            CENTER = new double[]{0, 58, 0};
            ImageIO.write(armorSheet("Тяжёлая силовая броня: шлем крупно", List.of(
                    new Shot("Прототип", proto),
                    new Shot("Основа I", Look.of(1, 0, false)),
                    new Shot("Основа II", Look.of(2, 0, false)),
                    new Shot("Основа III, детали III, вершина", Look.of(3, 3, true))), 8.5),
                    "png", preview.resolve("armor-helmet.png").toFile());
        }
    }
}
