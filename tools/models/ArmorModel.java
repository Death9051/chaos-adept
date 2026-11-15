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

    static void helmet() {
        // Прототип: отдельные пластины на рунных скобах, голова видна в просветах.
        on("armorHead", "helmet_base0");
        c(Mat.STEEL, 14, 64.4, 1, -7, 14);
        c(Mat.STEEL, 14, 56, 5, -9.4, 1).rotX(-8);
        sym(box(Mat.STEEL, 8.6, 50, -6, 1, 10, 12));
        c(Mat.STEEL, 12, 51, 10, 8.6, 1);
        sym(box(Mat.RUNE, 6.5, 61.4, -9, 1, 3, 1));
        sym(box(Mat.RUNE, 6.5, 61.4, 8, 1, 3, 1));
        // Переход I: облегающий шлем, три полосы, смотровая щель, рунная полоса на лбу.
        on("armorHead", "helmet_base1");
        box(Mat.GUNMETAL, -8, 48, -8, 16, 16, 16).inflate(0.5);
        rings(0, 0, 9.5, 9.5, new double[]{49.5, 58, 62}, Mat.STEEL);
        c(Mat.STEEL, 14, 64.6, 1, -7, 14);
        c(Mat.ARC, 10, 54, 2, -8.9, 1);
        c(Mat.RUNE, 12, 60.3, 1, -8.9, 1);
        // Переход II: полосы чаще, двухъярусный верх, дыхательный узел, руны только у висков.
        on("armorHead", "helmet_base2");
        box(Mat.GUNMETAL, -8, 48, -8, 16, 16, 16).inflate(0.5);
        rings(0, 0, 9.5, 9.5, new double[]{48.8, 51.4, 57.4, 60, 62.6}, Mat.STEEL, Mat.GUNMETAL);
        c(Mat.STEEL, 14, 64.7, 1, -7, 14);
        c(Mat.STEEL, 10, 65.7, 1, -5, 10);
        c(Mat.ARC, 10, 54.2, 2, -8.9, 1);
        oct(Mat.STEEL, 7, 3, 0, 51.5, -11, 2);
        sym(box(Mat.RUNE, 9.2, 54, -2, 1, 2, 4));
        // Финал: плотные полосы, верх с латунным гребнем, две линзы в оправах, переносица,
        // дыхательный узел с решёткой, диски на ушах, слоёный затыльник.
        on("armorHead", "helmet_base3");
        box(Mat.GUNMETAL, -8, 48, -8, 16, 16, 16).inflate(0.5);
        rings(0, 0, 9.5, 9.5, new double[]{48.6, 51, 57.6, 60, 62.4}, Mat.STEEL, Mat.GUNMETAL);
        c(Mat.STEEL, 14, 64.6, 1, -7, 14);
        c(Mat.GUNMETAL, 10, 65.6, 1, -5, 10);
        c(Mat.BRASS, 2, 66.6, 1, -5, 10);
        for (int sx = -1; sx <= 1; sx += 2) {
            oct(Mat.BRASS, 5, 2, sx * 3.5, 55, -9.3, 1);
            oct(Mat.ARC, 3, 1, sx * 3.5, 55, -9.9, 1);
        }
        c(Mat.STEEL, 2, 53.6, 4, -9.4, 1);
        oct(Mat.GUNMETAL, 8, 3, 0, 51, -11.6, 3);
        oct(Mat.STEEL, 5, 2, 0, 51, -12.2, 1);
        octX(Mat.STEEL, 7, 3, 9.6, 55, 0, 1);
        octX(Mat.STEEL, 7, 3, -10.6, 55, 0, 1);
        c(Mat.STEEL, 12, 47.4, 2, 8.8, 1).rotX(-20);
        c(Mat.GUNMETAL, 14, 45.8, 2, 9.4, 1).rotX(-20);
        // «Очистка»: фильтры по бокам дыхательного узла и блок на затылке.
        on("armorHead", "helmet_add1");
        oct(Mat.BRASS, 3, 1, 6, 50.5, -12.6, 3);
        on("armorHead", "helmet_add2");
        oct(Mat.BRASS, 3, 1, -6, 50.5, -12.6, 3);
        on("armorHead", "helmet_add3");
        c(Mat.STEEL, 6, 53, 4, 9.6, 2);
        box(Mat.LAMP, -1, 57.2, 10, 2, 1, 1);
        // Вершина «Аварийный режим»: красный маяк на макушке.
        on("armorHead", "helmet_apex");
        octY(Mat.BRASS, 5, 2, 0, 67.7, 0, 1);
        box(Mat.LAMP, -1, 68.7, -1, 2, 2, 2);
    }

    static void chest() {
        // Прототип: три нагрудные полосы и наклонная верхняя пластина на ремнях, две полосы на спине, руны.
        on("armorBody", "chest_base0");
        for (double y : new double[]{32, 36, 40}) {
            c(Mat.STEEL, 12, y, 3, -5.6, 1);
        }
        c(Mat.STEEL, 10, 43.6, 4, -6, 1).rotX(-20);
        c(Mat.STEEL, 12, 34, 4, 4.6, 1);
        c(Mat.STEEL, 12, 40, 4, 4.6, 1);
        sym(box(Mat.LEATHER, 3, 47, -5, 4, 1, 10));
        sym(box(Mat.RUNE, 6.2, 44, -5.4, 1, 2, 1));
        sym(box(Mat.RUNE, 6.2, 32, -5.4, 1, 2, 1));
        // Переход I: поддоспешник и четыре полосы вокруг корпуса, рунный шов по центру.
        on("armorBody", "chest_base1");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        rings(0, 0, 9.5, 5.5, new double[]{31, 35, 39, 43}, Mat.STEEL);
        c(Mat.RUNE, 2, 32, 12, -6.2, 1);
        // Переход II: шесть полос, наклонный воротник, руны у ключиц.
        on("armorBody", "chest_base2");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        rings(0, 0, 9.5, 5.5, new double[]{30.4, 33.2, 36, 38.8, 41.6, 44.4}, Mat.STEEL, Mat.GUNMETAL);
        c(Mat.STEEL, 10, 45.6, 3, -6.6, 1).rotX(-18);
        sym(box(Mat.RUNE, 4, 42, -6.2, 2, 1, 1));
        // Финал: плотные полосы, латунная грудина, две наклонные верхние пластины, воротник спереди и сзади.
        on("armorBody", "chest_base3");
        box(Mat.GUNMETAL, -8, 30, -4, 16, 18, 8).inflate(0.4);
        rings(0, 0, 9.5, 5.5, new double[]{30.2, 32.6, 35, 37.4, 39.8, 42.2, 44.6}, Mat.STEEL, Mat.GUNMETAL);
        c(Mat.BRASS, 4, 33, 9, -6.3, 1);
        sym(box(Mat.STEEL, 2.4, 42.6, -7, 6, 4, 1).rotX(-18));
        c(Mat.GUNMETAL, 12, 46.2, 3, -6.9, 1).rotX(-18);
        c(Mat.GUNMETAL, 12, 46.2, 3, 5.9, 1).rotX(18);
        // «Ремонт»: два картриджа у живота и трубка маны к ним.
        on("armorBody", "chest_add1");
        oct(Mat.BRASS, 3, 1, 5.5, 31.5, -7.8, 2);
        on("armorBody", "chest_add2");
        oct(Mat.BRASS, 3, 1, -5.5, 31.5, -7.8, 2);
        on("armorBody", "chest_add3");
        box(Mat.MANA_TUBE, 7.4, 33.4, -6.5, 1, 7, 1);
        // Вершина «Перераспределение»: шина через грудь с разрядниками на концах.
        on("armorBody", "chest_apex");
        c(Mat.BRASS, 18, 40.2, 1, -7.4, 1);
        sym(box(Mat.ARC, 8, 39.2, -7.4, 1, 3, 1));
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
        // Переход I: рукав, четыре полосы, наплечник из двух наклонных ламелей, рунная полоса.
        sides("arms_base1", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            rings(12, 0, 5, 5, new double[]{26, 31, 38, 42.4}, Mat.STEEL);
            box(Mat.STEEL, 9, 46.4, -5, 9, 1, 10).rotZ(18);
            box(Mat.GUNMETAL, 10, 44.8, -5.5, 9, 1, 11).rotZ(18);
            box(Mat.RUNE, 17.2, 34.4, -2, 1, 2, 4);
        });
        // Переход II: полосы чаще, наплечник из трёх ламелей, диск сервопривода на локте.
        sides("arms_base2", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            rings(12, 0, 5, 5, new double[]{24.6, 27.4, 30.2, 37, 39.8, 42.6}, Mat.STEEL, Mat.GUNMETAL);
            box(Mat.STEEL, 8.6, 46.8, -5, 9, 1, 10).rotZ(18);
            box(Mat.GUNMETAL, 9.8, 45.2, -5.5, 9, 1, 11).rotZ(18);
            box(Mat.STEEL, 11, 43.6, -6, 9, 1, 12).rotZ(18);
            octX(Mat.STEEL, 5, 2, 17.1, 34, 0, 1);
        });
        // Финал: плотные полосы, наплечник из четырёх ламелей с латунной кромкой, диск локтя, щиток кисти.
        sides("arms_base3", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            rings(12, 0, 5, 5, new double[]{24.4, 26.8, 29.2, 31.6, 36.8, 39.2, 41.6}, Mat.STEEL, Mat.GUNMETAL);
            box(Mat.STEEL, 8.2, 47.4, -5, 9, 1, 10).rotZ(20);
            box(Mat.GUNMETAL, 9.4, 45.8, -5.5, 9, 1, 11).rotZ(20);
            box(Mat.STEEL, 10.6, 44.2, -6, 9, 1, 12).rotZ(20);
            box(Mat.BRASS, 11.8, 42.6, -6.5, 9, 1, 13).rotZ(20);
            octX(Mat.STEEL, 5, 2, 17.1, 34.2, 0, 1);
            box(Mat.STEEL, 17.2, 24.2, -3, 1, 4, 6);
        });
        // «Перехват»: щиток на предплечье, второй слой, излучатель.
        sides("arms_add1", "Arm", () -> box(Mat.BRASS, 18.3, 26, -3, 1, 6, 6));
        sides("arms_add2", "Arm", () -> octX(Mat.STEEL, 5, 2, 19.3, 29, 0, 1));
        sides("arms_add3", "Arm", () -> box(Mat.ARC, 20.3, 28, -1, 1, 2, 2));
        // Вершина «Таран»: ступенчатый шип на наплечнике.
        sides("arms_apex", "Arm", () -> {
            octY(Mat.STEEL, 5, 2, 15, 48, 0, 3);
            octY(Mat.BRASS, 3, 1, 15, 51, 0, 2);
        });
    }

    static void legs() {
        // Прототип: щитки бедра и голени спереди, диск колена, боковая пластина, руна у пояса.
        sides("legs_base0", "Leg", () -> {
            for (double y : new double[]{2, 6, 14, 18}) {
                box(Mat.STEEL, 1, y, -5.2, 6, 3, 1);
            }
            oct(Mat.STEEL, 5, 2, 4, 11, -5.8, 1);
            box(Mat.STEEL, 8.2, 13, -3, 1, 8, 6);
            box(Mat.RUNE, 3, 22, -5.4, 2, 1, 1);
        });
        // Переход I: штанина и четыре полосы, руна на колене.
        sides("legs_base1", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            rings(4, 0, 5, 5, new double[]{2, 6.6, 15, 19.6}, Mat.STEEL);
            oct(Mat.RUNE, 5, 2, 4, 11.5, -4.8, 1);
        });
        // Переход II: полосы чаще, диск наколенника, поршень икры.
        sides("legs_base2", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            rings(4, 0, 5, 5, new double[]{0.6, 3.4, 6.2, 14.4, 17.2, 20}, Mat.STEEL, Mat.GUNMETAL);
            oct(Mat.STEEL, 8, 3, 4, 11.5, -5.8, 2);
            box(Mat.STEEL, 3, 2, 5.1, 2, 7, 1);
        });
        // Финал: плотные полосы, большой диск колена со ступицей, скошенный носок.
        sides("legs_base3", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            rings(4, 0, 5, 5, new double[]{0.4, 2.8, 5.2, 7.6, 14, 16.4, 18.8, 21.2}, Mat.STEEL, Mat.GUNMETAL);
            oct(Mat.STEEL, 10, 4, 4, 11.5, -6.2, 2);
            oct(Mat.BRASS, 5, 2, 4, 11.5, -6.8, 1);
            box(Mat.GUNMETAL, 0.5, 0, -7.6, 7, 3, 3);
            box(Mat.GUNMETAL, 0.6, 1.4, -7.4, 7, 3, 3).rotX(45);
        });
        // «Мобильность»: приводы на голени и бедре, лампа готовности.
        sides("legs_add1", "Leg", () -> box(Mat.STEEL, 9.2, 2.6, -2, 1, 6, 4));
        sides("legs_add2", "Leg", () -> octX(Mat.BRASS, 5, 2, 9.2, 17.6, 0, 1));
        sides("legs_add3", "Leg", () -> box(Mat.LAMP, 10.2, 4.6, -1, 1, 2, 2));
        // Вершина «Реактивный прыжок»: сопла за икрами.
        sides("legs_apex", "Leg", () -> {
            octY(Mat.STEEL, 5, 2, 4, 2, 8.2, 6);
            octY(Mat.ARC, 3, 1, 4, 1, 8.2, 1);
        });
    }

    /** Сдвиг ранца назад от спины. */
    static final double RZ = 0.2;

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

    static void rune() {
        // Прототип: кожаный пояс с большой восьмигранной рунной пряжкой.
        on("armorBody", "rune_base0");
        ring(Mat.LEATHER, 0, 0, 25, 4, 9.5, 5.5);
        oct(Mat.RUNE, 10, 4, 0, 27, -6.6, 1);
        // Переход I: стальной пояс, узел в латунной оправе, руны по бокам, набедренная пластина.
        on("armorBody", "rune_base1");
        ring(Mat.STEEL, 0, 0, 25, 4, 9.5, 5.5);
        oct(Mat.BRASS, 7, 3, 0, 27, -6.5, 1);
        box(Mat.CRYSTAL, -1, 26, -7.1, 2, 2, 1);
        sym(box(Mat.RUNE, 5.5, 26, -5.9, 2, 2, 1));
        c(Mat.STEEL, 8, 19.6, 5, -6, 1).rotX(-10);
        // Переход II: тёмная оправа, латунные клеммы.
        on("armorBody", "rune_base2");
        ring(Mat.STEEL, 0, 0, 25, 4, 9.5, 5.5);
        oct(Mat.GUNMETAL, 7, 3, 0, 27, -6.5, 1);
        box(Mat.CRYSTAL, -1, 26, -7.1, 2, 2, 1);
        sym(box(Mat.BRASS, 4.5, 26.5, -5.9, 1, 1, 1));
        c(Mat.STEEL, 8, 19.6, 5, -6, 1).rotX(-10);
        // Финал: широкий пояс из двух полос, компактный узел, кристалл в центре, две набедренные ламели.
        on("armorBody", "rune_base3");
        ring(Mat.GUNMETAL, 0, 0, 24.6, 2, 10, 6);
        ring(Mat.STEEL, 0, 0, 27, 2, 10, 6);
        oct(Mat.GUNMETAL, 5, 2, 0, 27, -7, 1);
        box(Mat.CRYSTAL, -1, 26, -7.6, 2, 2, 1);
        c(Mat.STEEL, 8, 21, 3, -6.4, 1).rotX(-10);
        c(Mat.GUNMETAL, 10, 18.4, 3, -6.8, 1).rotX(-10);
        // «Насыщение»: трубки маны вдоль пояса.
        on("armorBody", "rune_add1");
        box(Mat.MANA_TUBE, 4, 25.4, -6.6, 4, 1, 1);
        on("armorBody", "rune_add2");
        box(Mat.MANA_TUBE, -8, 25.4, -6.6, 4, 1, 1);
        on("armorBody", "rune_add3");
        box(Mat.MANA_TUBE, -4, 25.6, 5.7, 8, 1, 1);
        // Вершина «Антимагическое поле»: излучатели на бёдрах.
        on("armorBody", "rune_apex");
        sym(box(Mat.BRASS, 10.3, 23, -1, 1, 6, 2));
        sym(box(Mat.CRYSTAL, 10.3, 29, -1, 1, 2, 2));
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
