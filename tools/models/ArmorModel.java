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

    static void helmet() {
        // Прототип: отдельные пластины на рунных скобах, голова видна в просветах.
        on("armorHead", "helmet_base0");
        octY(Mat.STEEL, 17, 7, 0, 64.4, 0, 2);
        c(Mat.STEEL, 14, 55, 6, -10, 1).rotX(-8);
        sym(box(Mat.STEEL, 9, 49, -7, 1, 12, 13));
        c(Mat.STEEL, 12, 50, 11, 9, 1);
        sym(box(Mat.RUNE, 7, 61, -9.5, 1, 3, 1));
        sym(box(Mat.RUNE, 7, 61, 8.5, 1, 3, 1));
        // Переход I: закрытый шлем со срезанными углами, рунная полоса над забралом.
        on("armorHead", "helmet_base1");
        chamfered(Mat.GUNMETAL, 48.4, 16, 10, 9);
        c(Mat.ARC, 10, 54, 2, -9.6, 1);
        c(Mat.RUNE, 12, 59, 1, -9.7, 1);
        sym(box(Mat.RUNE, 9.7, 59, -5, 1, 1, 10));
        // Переход II: купол, выступающая лицевая часть, руны только у висков.
        on("armorHead", "helmet_base2");
        chamfered(Mat.GUNMETAL, 48.4, 16, 10, 9);
        octY(Mat.STEEL, 17, 7, 0, 64.4, 0, 2);
        c(Mat.ARC, 10, 54.6, 2, -9.6, 1);
        c(Mat.STEEL, 8, 48.6, 5, -11.2, 3).rotX(12);
        sym(box(Mat.RUNE, 9.7, 57, -2, 1, 2, 4));
        // Финал: двухъярусный купол с гребнем, козырёк, лицевая часть со скулами, диски на ушах.
        on("armorHead", "helmet_base3");
        chamfered(Mat.GUNMETAL, 48.4, 16, 10, 9);
        octY(Mat.GUNMETAL, 17, 7, 0, 64.4, 0, 2);
        octY(Mat.STEEL, 12, 5, 0, 66.4, 0, 1);
        c(Mat.ARC, 12, 54.6, 2, -9.7, 1);
        c(Mat.STEEL, 14, 56.8, 2, -10.8, 2).rotX(-25);
        c(Mat.GUNMETAL, 8, 48.6, 5, -11.6, 3).rotX(12);
        sym(box(Mat.STEEL, 4.5, 49, -10.4, 4, 5, 1));
        octX(Mat.STEEL, 7, 3, 10, 55, 0, 1);
        octX(Mat.STEEL, 7, 3, -11, 55, 0, 1);
        c(Mat.BRASS, 2, 67.4, 1, -6, 12);
        // «Очистка»: фильтры у подбородка и блок на затылке.
        on("armorHead", "helmet_add1");
        oct(Mat.BRASS, 3, 1, 7, 50.5, -13.6, 3);
        on("armorHead", "helmet_add2");
        oct(Mat.BRASS, 3, 1, -7, 50.5, -13.6, 3);
        on("armorHead", "helmet_add3");
        c(Mat.STEEL, 6, 50, 4, 9.2, 2);
        box(Mat.LAMP, -1, 54.2, 9.6, 2, 1, 1);
        // Вершина «Аварийный режим»: красный маяк на макушке.
        on("armorHead", "helmet_apex");
        octY(Mat.BRASS, 5, 2, 0, 68.5, 0, 1);
        box(Mat.LAMP, -1, 69.5, -1, 2, 2, 2);
    }

    static void chest() {
        // Прототип: нагрудная пластина с наклонным верхом и спинная пластина на ремнях, руны по углам.
        on("armorBody", "chest_base0");
        c(Mat.STEEL, 12, 32, 9, -6.4, 1);
        c(Mat.STEEL, 10, 41, 5, -7, 1).rotX(-22);
        c(Mat.STEEL, 12, 33, 12, 5.4, 1);
        sym(box(Mat.LEATHER, 3, 47, -6, 4, 1, 12));
        sym(box(Mat.RUNE, 5, 44, -7.2, 2, 2, 1));
        sym(box(Mat.RUNE, 5, 32, -6.9, 2, 2, 1));
        // Переход I: округлая грудь — восьмигранная призма поперёк плеч, рунный шов по центру. Талия открыта.
        on("armorBody", "chest_base1");
        octX(Mat.GUNMETAL, 17, 7, -9, 39.5, 0, 18);
        c(Mat.RUNE, 3, 36, 7, -9, 1);
        // Переход II: грудная плита, воротник по верхней грани, руны у ключиц.
        on("armorBody", "chest_base2");
        octX(Mat.GUNMETAL, 17, 7, -9, 39.5, 0, 18);
        c(Mat.STEEL, 12, 36.5, 6, -9.3, 1);
        c(Mat.STEEL, 12, 45.4, 2, -7.6, 2);
        sym(box(Mat.RUNE, 4, 43.2, -8.2, 2, 2, 1).rotX(45));
        // Финал: грудь шире, плита с ребром, пластины по нижней грани, высокий воротник.
        on("armorBody", "chest_base3");
        octX(Mat.GUNMETAL, 19, 8, -10, 38.5, 0, 20);
        c(Mat.STEEL, 14, 35.5, 6, -10.3, 1);
        c(Mat.STEEL, 2, 35, 7, -10.9, 1);
        sym(box(Mat.STEEL, 1.5, 30.2, -8.6, 6, 3, 1).rotX(-45));
        c(Mat.GUNMETAL, 14, 46.4, 3, -8.2, 2);
        c(Mat.GUNMETAL, 14, 46.4, 3, 6.2, 2);
        // «Ремонт»: два картриджа под грудью и трубка маны к ним.
        on("armorBody", "chest_add1");
        oct(Mat.BRASS, 3, 1, 6, 31, -10.4, 2);
        on("armorBody", "chest_add2");
        oct(Mat.BRASS, 3, 1, -6, 31, -10.4, 2);
        on("armorBody", "chest_add3");
        box(Mat.MANA_TUBE, 7.6, 34, -10.4, 1, 7, 1);
        // Вершина «Перераспределение»: шина через грудь с разрядниками на концах.
        on("armorBody", "chest_apex");
        c(Mat.BRASS, 18, 42.4, 1, -10.6, 1);
        sym(box(Mat.ARC, 8, 41.4, -10.6, 1, 3, 1));
    }

    static void arms() {
        // Прототип: наклонная наплечная пластина, щитки плеча и предплечья на ремне, руна.
        sides("arms_base0", "Arm", () -> {
            box(Mat.STEEL, 9, 46.4, -5, 9, 1, 10).rotZ(15);
            box(Mat.STEEL, 16.3, 34, -3, 1, 9, 6);
            box(Mat.STEEL, 16.3, 25, -2.5, 1, 6, 5);
            box(Mat.RUNE, 16.5, 43, -1, 1, 2, 2);
            box(Mat.LEATHER, 8, 32, -4, 8, 1, 8).inflate(0.4);
        });
        // Переход I: наклонный наплечник, плечо и наруч раздельно (локоть открыт), рунное кольцо на плече.
        sides("arms_base1", "Arm", () -> {
            box(Mat.GUNMETAL, 8.5, 44.6, -6, 11, 3, 12).rotZ(20);
            octY(Mat.GUNMETAL, 12, 5, 12, 35, 0, 8);
            octY(Mat.RUNE, 14, 6, 12, 38, 0, 1);
            octY(Mat.GUNMETAL, 12, 5, 12, 24, 0, 8);
        });
        // Переход II: второй слой наплечника, наруч шире плеча, диск сервопривода на локте.
        sides("arms_base2", "Arm", () -> {
            box(Mat.GUNMETAL, 8.5, 44.6, -6, 11, 3, 12).rotZ(20);
            box(Mat.STEEL, 10, 47.2, -5, 8, 2, 10).rotZ(20);
            octY(Mat.GUNMETAL, 12, 5, 12, 35, 0, 8);
            octY(Mat.GUNMETAL, 14, 6, 12, 24, 0, 7);
            octX(Mat.STEEL, 5, 2, 16.2, 33.5, 0, 1);
        });
        // Финал: большой двухслойный наплечник, плечо, широкий наруч с манжетой, диск локтя.
        sides("arms_base3", "Arm", () -> {
            box(Mat.GUNMETAL, 8, 44, -7, 13, 4, 14).rotZ(22);
            box(Mat.STEEL, 9.5, 47.6, -5.5, 9, 2, 11).rotZ(22);
            box(Mat.BRASS, 20.2, 41.6, -2, 1, 2, 4);
            octY(Mat.GUNMETAL, 12, 5, 12, 35, 0, 8);
            octY(Mat.GUNMETAL, 14, 6, 12, 24, 0, 8);
            octY(Mat.STEEL, 15, 6, 12, 24.5, 0, 2);
            octX(Mat.STEEL, 5, 2, 17.2, 33.5, 0, 1);
        });
        // «Перехват»: щиток на наруче, второй слой, излучатель.
        sides("arms_add1", "Arm", () -> box(Mat.BRASS, 19.6, 26, -3, 1, 6, 6));
        sides("arms_add2", "Arm", () -> octX(Mat.STEEL, 5, 2, 20.6, 29, 0, 1));
        sides("arms_add3", "Arm", () -> box(Mat.ARC, 21.6, 28, -1, 1, 2, 2));
        // Вершина «Таран»: ступенчатый шип на наплечнике.
        sides("arms_apex", "Arm", () -> {
            octY(Mat.STEEL, 5, 2, 16, 47, 0, 3);
            octY(Mat.BRASS, 3, 1, 16, 50, 0, 2);
        });
    }

    static void legs() {
        // Прототип: щитки бедра и голени, диск колена, боковая пластина, руна у пояса.
        sides("legs_base0", "Leg", () -> {
            box(Mat.STEEL, 1, 14, -6.3, 6, 8, 1);
            oct(Mat.STEEL, 5, 2, 4, 11, -7, 1);
            box(Mat.STEEL, 1, 2, -6.3, 6, 7, 1);
            box(Mat.STEEL, 8.4, 12, -3, 1, 10, 6);
            box(Mat.RUNE, 3, 22.4, -6.6, 2, 1, 1);
        });
        // Переход I: набедренник и понож раздельно (колено открыто), руна на колене.
        sides("legs_base1", "Leg", () -> {
            octY(Mat.GUNMETAL, 12, 5, 5, 14, 0, 9);
            octY(Mat.GUNMETAL, 12, 5, 5, 1, 0, 9);
            oct(Mat.RUNE, 5, 2, 4.5, 11.5, -5.6, 1);
        });
        // Переход II: диск наколенника, расширение у стопы, поршень икры.
        sides("legs_base2", "Leg", () -> {
            octY(Mat.GUNMETAL, 12, 5, 5, 14, 0, 9);
            octY(Mat.GUNMETAL, 12, 5, 5, 3, 0, 7);
            octY(Mat.GUNMETAL, 14, 6, 6, 0, 0, 4);
            oct(Mat.STEEL, 8, 3, 4.5, 11.5, -7, 2);
            box(Mat.STEEL, 4, 4.5, 6.1, 2, 7, 1);
        });
        // Финал: набедренник, большой диск колена со ступицей, понож с ребром, ботинок со скошенным носком.
        sides("legs_base3", "Leg", () -> {
            octY(Mat.GUNMETAL, 12, 5, 5, 14, 0, 10);
            oct(Mat.STEEL, 10, 4, 4.5, 11.5, -7.4, 2);
            oct(Mat.BRASS, 5, 2, 4.5, 11.5, -8, 1);
            octY(Mat.GUNMETAL, 12, 5, 5, 4, 0, 9);
            box(Mat.STEEL, 3.5, 4.2, -7.4, 2, 5, 2).rotY(45);
            octY(Mat.GUNMETAL, 14, 6, 6, 0, 0, 5);
            box(Mat.GUNMETAL, 2.5, 0, -9.6, 7, 3, 3);
            box(Mat.GUNMETAL, 2.6, 1.4, -9.4, 7, 3, 3).rotX(45);
        });
        // «Мобильность»: приводы на голени и бедре, лампа готовности.
        sides("legs_add1", "Leg", () -> box(Mat.STEEL, 11.2, 5.5, -2, 1, 6, 4));
        sides("legs_add2", "Leg", () -> octX(Mat.BRASS, 5, 2, 11.2, 18, 0, 1));
        sides("legs_add3", "Leg", () -> box(Mat.LAMP, 12.2, 7.5, -1, 1, 2, 2));
        // Вершина «Реактивный прыжок»: сопла за икрами.
        sides("legs_apex", "Leg", () -> {
            octY(Mat.STEEL, 5, 2, 5, 5.6, 9.7, 6);
            octY(Mat.ARC, 3, 1, 5, 4.6, 9.7, 1);
        });
    }

    /** Сдвиг ранца назад: спина нагрудника — восьмигранная призма. */
    static final double RZ = 3.4;

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
        chamfered(Mat.LEATHER, 25, 4, 9, 6);
        oct(Mat.RUNE, 10, 4, 0, 27, -7.4, 1);
        // Переход I: стальной пояс, узел в латунной оправе, руны по бокам.
        on("armorBody", "rune_base1");
        chamfered(Mat.STEEL, 25, 4, 9, 6);
        oct(Mat.BRASS, 7, 3, 0, 27, -7.3, 1);
        c(Mat.STEEL, 8, 19.6, 5, -6.9, 1).rotX(-10);
        box(Mat.CRYSTAL, -1, 26, -7.9, 2, 2, 1);
        sym(box(Mat.RUNE, 5.5, 26, -6.7, 2, 2, 1));
        // Переход II: тёмная оправа, латунные клеммы.
        on("armorBody", "rune_base2");
        chamfered(Mat.STEEL, 25, 4, 9, 6);
        oct(Mat.GUNMETAL, 7, 3, 0, 27, -7.3, 1);
        c(Mat.STEEL, 8, 19.6, 5, -6.9, 1).rotX(-10);
        box(Mat.CRYSTAL, -1, 26, -7.9, 2, 2, 1);
        sym(box(Mat.BRASS, 4.5, 26.5, -6.7, 1, 1, 1));
        // Финал: широкий пояс, компактный узел, кристалл в центре.
        on("armorBody", "rune_base3");
        chamfered(Mat.GUNMETAL, 24.6, 5, 10, 6);
        oct(Mat.GUNMETAL, 5, 2, 0, 27, -7.3, 1);
        c(Mat.GUNMETAL, 10, 18.6, 6, -7, 1).rotX(-10);
        box(Mat.CRYSTAL, -1, 26, -7.9, 2, 2, 1);
        // «Насыщение»: трубки маны вдоль пояса.
        on("armorBody", "rune_add1");
        box(Mat.MANA_TUBE, 4, 26.6, -6.9, 4, 1, 1);
        on("armorBody", "rune_add2");
        box(Mat.MANA_TUBE, -8, 26.6, -6.9, 4, 1, 1);
        on("armorBody", "rune_add3");
        box(Mat.MANA_TUBE, -4, 26.8, 6.2, 8, 1, 1);
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
            g.drawImage(render(s.look(), 205, 14, 4.7, cw - 2, ch - 2), i * cw, head, null);
            g.drawImage(render(s.look(), 25, 14, 4.7, cw - 2, ch - 2), i * cw, head + ch, null);
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
