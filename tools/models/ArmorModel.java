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
     * Гранёный корпус: два бруса крестом и четыре плиты под 45° в углах. X и Z — полуразмеры, c — срез угла.
     * При X = Z и c ≈ 0,45·X получается правильный восьмигранник.
     */
    static void faceted(Mat m, double y, int h, int X, int Z, double c) {
        c(m, 2 * X, y, h, -(Z - c), (int) Math.round(2 * (Z - c)));
        c(m, (int) Math.round(2 * (X - c)), y + 0.01, h, -Z, 2 * Z);
        int len = (int) Math.floor(c * 1.4142);
        double off = 0.7071 * 1.5;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double cx = sx * (X - c / 2 - off), cz = sz * (Z - c / 2 - off);
                box(m, cx - len / 2.0, y + 0.02, cz - 1.5, len, h, 3).rotY(45 * sx * sz);
            }
        }
    }

    /** Плотная оболочка шлема по голове и плоское темя. */
    static void skull(Mat top) {
        box(Mat.GUNMETAL, -8, 48, -8, 16, 16, 16).inflate(0.4);
        c(top, 12, 64.5, 2, -6, 12);
    }

    /**
     * Скаты шлема: четыре плиты от темени вниз и наружу под 30°, как поля. start — отступ от края темени
     * вдоль ската, len — длина плиты.
     */
    static void slopes(Mat m, double start, int len) {
        double along = start + len / 2.0;
        double r = 6 + along * 0.866;
        double y = 64.6 - along * 0.5 - 0.6;
        c(m, 14, y, 2, -r - len / 2.0, len).rotX(30);
        c(m, 14, y, 2, r - len / 2.0, len).rotX(-30);
        sym(box(m, r - len / 2.0, y, -7, len, 2, 14).rotZ(30));
    }

    /** Лицевая маска клином: две плиты сходятся к середине лица, внешние края уходят назад. */
    static void beak(Mat m, double y, int h) {
        sym(box(m, 0.2, y, -11.4, 7, h, 1).rotY(-32));
    }

    static void helmet() {
        // Прототип: лобовая и затылочная пластины с наклоном, щитки на щеках, рунные скобы; темя открыто.
        on("armorHead", "helmet_base0");
        c(Mat.STEEL, 14, 57, 6, -9.6, 1).rotX(-12);
        c(Mat.STEEL, 12, 52, 10, 8.8, 1).rotX(10);
        sym(box(Mat.STEEL, 8.6, 49.5, -5, 1, 7, 9).rotZ(-8));
        sym(box(Mat.RUNE, 6.5, 62.4, -9.4, 1, 2, 1));
        sym(box(Mat.RUNE, 8.6, 57.5, -1, 1, 2, 2));
        // Переход I: закрытый шлем со скатами, смотровая щель, рунная полоса на лбу.
        on("armorHead", "helmet_base1");
        skull(Mat.STEEL);
        slopes(Mat.STEEL, 0, 5);
        c(Mat.ARC, 10, 54.4, 2, -8.9, 1);
        c(Mat.RUNE, 10, 58.4, 1, -8.9, 1);
        // Переход II: лицевая маска клином и наклонные щёки, руны только у висков.
        on("armorHead", "helmet_base2");
        skull(Mat.STEEL);
        slopes(Mat.STEEL, 0, 5);
        c(Mat.ARC, 12, 54.4, 2, -8.9, 1);
        beak(Mat.STEEL, 48.4, 6);
        sym(box(Mat.STEEL, 8.5, 48.6, -6, 1, 6, 9).rotZ(-10));
        sym(box(Mat.RUNE, 8.6, 57.5, -2, 1, 2, 4));
        // Финал: длинные скаты с латунной кромкой, высокая маска с переносицей, наклонные щёки,
        // диски на ушах, гребень, затыльник.
        on("armorHead", "helmet_base3");
        skull(Mat.GUNMETAL);
        slopes(Mat.STEEL, 0, 6);
        slopes(Mat.BRASS, 6.1, 2);
        c(Mat.ARC, 12, 54.6, 2, -8.9, 1);
        beak(Mat.STEEL, 48.2, 7);
        c(Mat.GUNMETAL, 2, 48.4, 9, -12.2, 2).rotX(-8);
        sym(box(Mat.STEEL, 8.5, 48.6, -6, 2, 7, 10).rotZ(-10));
        octX(Mat.GUNMETAL, 5, 2, 9.6, 56, 1, 1);
        octX(Mat.GUNMETAL, 5, 2, -10.6, 56, 1, 1);
        c(Mat.BRASS, 2, 66.5, 1, -5, 10);
        c(Mat.STEEL, 12, 47.4, 3, 8.8, 1).rotX(-22);
        // «Очистка»: фильтры по бокам маски и блок на затылке.
        on("armorHead", "helmet_add1");
        oct(Mat.BRASS, 3, 1, 7.4, 50, -11, 3);
        on("armorHead", "helmet_add2");
        oct(Mat.BRASS, 3, 1, -7.4, 50, -11, 3);
        on("armorHead", "helmet_add3");
        c(Mat.GUNMETAL, 6, 53, 4, 9.4, 2);
        box(Mat.LAMP, -1, 57.2, 9.8, 2, 1, 1);
        // Вершина «Аварийный режим»: красный маяк на макушке.
        on("armorHead", "helmet_apex");
        octY(Mat.BRASS, 5, 2, 0, 67.6, 0, 1);
        box(Mat.LAMP, -1, 68.6, -1, 2, 2, 2);
    }

    /** Грудные плиты: две половины с наклоном вперёд и скосом к плечу. d — толщина, w — ширина половины. */
    static void pecs(Mat m, int w, int d, double y, int h) {
        sym(box(m, 0.3, y, -6.2 - d, w, h, d).rotX(-12));
        sym(box(m, w - 1.6, y + 0.3, -7.4 - d / 2.0, 3, h - 1, 3).rotY(45));
    }

    /** Поддоспешник корпуса: в плане шестигранник — прямая грудь и спина, скошенные бока. */
    static void torso() {
        faceted(Mat.GUNMETAL, 30, 18, 10, 6, 3.5);
    }

    /** Полосы на животе: гранёные, как корпус, чуть шире его. */
    static void belly(double[] ys, Mat... mats) {
        for (int i = 0; i < ys.length; i++) {
            faceted(mats[i % mats.length], ys[i], 2, 11, 7, 4);
        }
    }

    /** Пластины живота уголком: две половины сходятся книзу к середине. */
    static void chevron(Mat m, double y) {
        sym(box(m, 0.2, y, -8.4, 7, 3, 1).rotZ(-18));
    }

    /** Воротник-воронка: плиты вокруг шеи, верх отведён наружу. {@code full} — со всех сторон, иначе только спереди. */
    static void collar(Mat m, boolean full) {
        c(m, 16, 44.6, 5, -9.4, 2).rotX(28);
        if (full) {
            c(m, 16, 44.6, 5, 7.4, 2).rotX(-28);
            sym(box(m, 8.6, 44.6, -5, 2, 5, 10).rotZ(28));
        }
    }

    static void chest() {
        // Прототип: тонкие грудные плиты и лопатки на ремнях, один уголок на животе, руны.
        on("armorBody", "chest_base0");
        pecs(Mat.STEEL, 8, 1, 39, 7);
        chevron(Mat.STEEL, 33);
        sym(box(Mat.STEEL, 1, 39.5, 4.6, 6, 7, 1).rotX(8));
        sym(box(Mat.LEATHER, 3, 47, -5, 4, 1, 10));
        sym(box(Mat.RUNE, 6.2, 45.6, -7.8, 1, 2, 1));
        sym(box(Mat.RUNE, 3, 30.6, -6.4, 2, 2, 1));
        // Переход I: гранёный поддоспешник, нагрудник со скосами к плечам, уголок и полоса на животе, рунный шов.
        on("armorBody", "chest_base1");
        torso();
        pecs(Mat.STEEL, 9, 2, 38.5, 8);
        belly(new double[]{30.6}, Mat.STEEL);
        chevron(Mat.STEEL, 34);
        c(Mat.RUNE, 2, 39, 6, -9.2, 1).rotX(-12);
        // Переход II: покатые плечи, воротник спереди, лопатки, два уголка на животе, руны у ключиц.
        on("armorBody", "chest_base2");
        torso();
        pecs(Mat.STEEL, 9, 2, 38.5, 8);
        belly(new double[]{30.4}, Mat.GUNMETAL);
        chevron(Mat.STEEL, 32.6);
        chevron(Mat.STEEL, 35.4);
        sym(box(Mat.STEEL, 2.6, 46.6, -4, 6, 2, 8).rotZ(12));
        collar(Mat.GUNMETAL, false);
        sym(box(Mat.STEEL, 1, 39.5, 6.2, 7, 7, 1).rotX(8));
        sym(box(Mat.RUNE, 5, 43, -9.4, 2, 1, 1));
        // Финал: толстые грудные плиты с латунной грудиной, воротник-воронка со всех сторон, покатые плечи,
        // лопатки и хребет, три уголка на животе.
        on("armorBody", "chest_base3");
        torso();
        pecs(Mat.STEEL, 10, 3, 38.5, 8);
        c(Mat.BRASS, 2, 38, 9, -10.2, 1).rotX(-12);
        belly(new double[]{30.2}, Mat.GUNMETAL);
        chevron(Mat.STEEL, 31.6);
        chevron(Mat.GUNMETAL, 34);
        chevron(Mat.STEEL, 36.4);
        sym(box(Mat.STEEL, 2.6, 46.8, -4.5, 7, 2, 9).rotZ(12));
        collar(Mat.STEEL, true);
        sym(box(Mat.STEEL, 1, 39.5, 6.4, 8, 7, 2).rotX(8));
        c(Mat.GUNMETAL, 2, 31, 9, 6.4, 1);
        // «Ремонт»: два картриджа у живота и трубка маны к ним.
        on("armorBody", "chest_add1");
        oct(Mat.BRASS, 3, 1, 7.4, 32, -8.4, 2);
        on("armorBody", "chest_add2");
        oct(Mat.BRASS, 3, 1, -7.4, 32, -8.4, 2);
        on("armorBody", "chest_add3");
        box(Mat.MANA_TUBE, 8.6, 33.6, -6.6, 1, 5, 1);
        // Вершина «Перераспределение»: шина через грудь с разрядниками на концах.
        on("armorBody", "chest_apex");
        c(Mat.BRASS, 18, 42, 1, -12.2, 1);
        sym(box(Mat.ARC, 8, 41, -12.2, 1, 3, 1));
    }

    static void arms() {
        // Прототип: наклонная наплечная пластина и два гранёных браслета на руке, ремень на локте, руна.
        sides("arms_base0", "Arm", () -> {
            box(Mat.STEEL, 9, 45.4, -5, 8, 2, 10).rotZ(18);
            octY(Mat.STEEL, 12, 5, 12, 37, 0, 4);
            octY(Mat.STEEL, 12, 5, 12, 27, 0, 4);
            box(Mat.RUNE, 17.6, 38, -1, 1, 2, 2);
            box(Mat.LEATHER, 8, 33, -4, 8, 1, 8).inflate(0.3);
        });
        // Переход I: рукав, наклонный наплечник, по две полосы на плече и предплечье, рунное кольцо.
        sides("arms_base1", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            box(Mat.STEEL, 9, 42.4, -5, 8, 4, 10).rotZ(20);
            octY(Mat.GUNMETAL, 12, 5, 12, 36, 0, 5);
            octY(Mat.GUNMETAL, 12, 5, 12, 26.4, 0, 5);
            box(Mat.RUNE, 17.4, 39.6, -2, 1, 2, 4);
        });
        // Переход II: наклонный наплечник с боковой плитой, наплечье и наруч цельные, локоть и запястье открыты, диск локтя.
        sides("arms_base2", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            box(Mat.STEEL, 9, 42.4, -5.5, 8, 4, 11).rotZ(20);
            box(Mat.GUNMETAL, 16.4, 37.4, -4.5, 2, 6, 9).rotZ(-12);
            octY(Mat.GUNMETAL, 12, 5, 12, 35.6, 0, 6);
            octY(Mat.GUNMETAL, 12, 5, 12, 26.4, 0, 6);
            octX(Mat.STEEL, 5, 2, 16.4, 33.5, 0, 1);
        });
        // Финал: толстый наклонный наплечник в два слоя, боковая плита с латунной кромкой, наплечье, наруч с раструбом к локтю,
        // диск локтя, щиток кисти.
        sides("arms_base3", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 24, -4, 8, 24, 8).inflate(0.3);
            box(Mat.STEEL, 8.6, 42.2, -6, 9, 5, 12).rotZ(20);
            box(Mat.GUNMETAL, 10, 46.8, -4, 6, 2, 8).rotZ(20);
            box(Mat.STEEL, 16.8, 37, -5, 2, 7, 10).rotZ(-12);
            box(Mat.BRASS, 18.6, 36.4, -5.5, 1, 2, 11).rotZ(-12);
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
        // Прототип: два гранёных браслета на ноге, диск колена, руна.
        sides("legs_base0", "Leg", () -> {
            octY(Mat.STEEL, 12, 5, 4.6, 15, 0, 5);
            octY(Mat.STEEL, 12, 5, 4.6, 4, 0, 5);
            oct(Mat.STEEL, 5, 2, 4.3, 11.5, -5.4, 1);
            box(Mat.RUNE, 3.6, 17, -6.6, 2, 1, 1);
        });
        // Переход I: штанина, по две полосы на бедре и голени, руна на колене.
        sides("legs_base1", "Leg", () -> {
            box(Mat.GUNMETAL, 0, 0, -4, 8, 24, 8).inflate(0.3);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 14.6, 0, 7);
            octY(Mat.GUNMETAL, 12, 5, 4.6, 4, 0, 5);
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
    static final double RZ = 2.6;

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
        c(m, 8, 19.6, 5, -6.8, 1).rotX(-10);
        sym(box(m, 10.2, 19.6, -2, 1, 5, 4).rotZ(-10));
    }

    static void rune() {
        // Прототип: кожаный пояс с большой восьмигранной рунной пряжкой.
        on("armorBody", "rune_base0");
        faceted(Mat.LEATHER, 25, 4, 10, 6, 3.5);
        oct(Mat.RUNE, 10, 4, 0, 27, -7, 1);
        // Переход I: стальной пояс, узел в латунной оправе, руны по бокам, набедренные щитки.
        on("armorBody", "rune_base1");
        faceted(Mat.STEEL, 25, 4, 10, 6, 3.5);
        oct(Mat.BRASS, 7, 3, 0, 27, -6.9, 1);
        box(Mat.CRYSTAL, -1, 26, -7.5, 2, 2, 1);
        sym(box(Mat.RUNE, 4.2, 26, -6.4, 2, 2, 1));
        tassets(Mat.STEEL);
        // Переход II: тёмная оправа, латунные клеммы.
        on("armorBody", "rune_base2");
        faceted(Mat.STEEL, 25, 4, 10, 6, 3.5);
        oct(Mat.GUNMETAL, 7, 3, 0, 27, -6.9, 1);
        box(Mat.CRYSTAL, -1, 26, -7.5, 2, 2, 1);
        sym(box(Mat.BRASS, 4.5, 26.5, -6.4, 1, 1, 1));
        tassets(Mat.STEEL);
        // Финал: пояс из двух полос, компактный узел, кристалл в центре, набедренные щитки в два слоя.
        on("armorBody", "rune_base3");
        faceted(Mat.GUNMETAL, 24.6, 2, 10, 6, 3.5);
        faceted(Mat.STEEL, 27, 2, 11, 7, 4);
        oct(Mat.GUNMETAL, 5, 2, 0, 27, -7.9, 1);
        box(Mat.CRYSTAL, -1, 26, -8.5, 2, 2, 1);
        tassets(Mat.STEEL);
        c(Mat.GUNMETAL, 10, 17.4, 3, -7.4, 1).rotX(-10);
        // «Насыщение»: трубки маны вдоль пояса.
        on("armorBody", "rune_add1");
        box(Mat.MANA_TUBE, 2.4, 25.2, -8.2, 4, 1, 1);
        on("armorBody", "rune_add2");
        box(Mat.MANA_TUBE, -6.4, 25.2, -8.2, 4, 1, 1);
        on("armorBody", "rune_add3");
        box(Mat.MANA_TUBE, -4, 25.6, 7.2, 8, 1, 1);
        // Вершина «Антимагическое поле»: излучатели на бёдрах.
        on("armorBody", "rune_apex");
        sym(box(Mat.BRASS, 11.6, 23, -1, 1, 6, 2));
        sym(box(Mat.CRYSTAL, 11.6, 29, -1, 1, 2, 2));
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
