package ru.adept.core.artifact;

import ru.adept.core.component.ComponentFamily;
import ru.adept.core.data.ArtifactProgress;

/**
 * Облик артефакта по прокачке («Артефакты», «Прокачка по сегментам») — гаусс-пушки, энергоклинка, тяжёлой силовой
 * брони: у сегмента один путь меняет основу (ступень 0–3 выбирает вариант основы), второй добавляет навесные детали
 * (по одной на ступень), вершина — отдельная деталь.
 *
 * Облик упакован в одно число (по 5 бит на сегмент: 2 — основа, 2 — детали, 1 — вершина) и лежит в данных
 * предмета под ключом {@code look}: так рендер видит его и в руке, и в рамке, и у других игроков.
 * Кости модели: {@code <сегмент>_base0..3}, {@code <сегмент>_add1..3}, {@code <сегмент>_apex}
 * (генераторы в {@code tools/models}).
 */
public final class GaussLook {
    /**
     * Сегмент артефакта: id кости и ключа прогресса, путь основы, путь деталей, два семейства компонентов
     * (основное и второе). Семейства — «Материалы», «Компоненты по артефактам»; бронепанели в коде пока нет,
     * у брони её заменяет каркас.
     */
    public static final class Segment {
        public static final Segment BARREL = new Segment(0, 0, "barrel", "boost", "tempo", ComponentFamily.COIL, ComponentFamily.FRAME);
        public static final Segment COILS = new Segment(0, 1, "coils", "stability", "economy", ComponentFamily.COIL, ComponentFamily.WIRING);
        public static final Segment COOLER = new Segment(0, 2, "cooler", "heatsink", "purge", ComponentFamily.RADIATOR, ComponentFamily.NOZZLE);
        public static final Segment BATTERY = new Segment(0, 3, "battery", "capacity", "recuperation", ComponentFamily.CAPACITOR, ComponentFamily.WIRING);
        public static final Segment MAGAZINE = new Segment(0, 4, "magazine", "volume", "pattern", ComponentFamily.ACTUATOR, ComponentFamily.SPRING);
        public static final Segment SCOPE = new Segment(0, 5, "scope", "lens", "scanner", ComponentFamily.LENS, ComponentFamily.SENSOR);

        public static final Segment BLADE_EMITTER = new Segment(1, 0, "emitter", "form", "edge", ComponentFamily.EMITTER, ComponentFamily.COIL);
        public static final Segment BLADE_DRIVE = new Segment(1, 1, "drive", "dance", "dash", ComponentFamily.ACTUATOR, ComponentFamily.SPRING);
        public static final Segment BLADE_BATTERY = new Segment(1, 2, "battery", "capacity", "recuperation", ComponentFamily.CAPACITOR, ComponentFamily.WIRING);
        public static final Segment BLADE_FOCUS = new Segment(1, 3, "focus", "slots", "economy", ComponentFamily.MANA_CIRCUIT, ComponentFamily.LENS);
        public static final Segment BLADE_CONTOUR = new Segment(1, 4, "contour", "reserve", "absorb", ComponentFamily.MANA_CIRCUIT, ComponentFamily.MATRIX);
        public static final Segment BLADE_SYNC = new Segment(1, 5, "sync", "threshold", "feedback", ComponentFamily.CONTROL_BOARD, ComponentFamily.MANA_CIRCUIT);

        public static final Segment HEAVY_HELMET = new Segment(2, 0, "helmet", "damping", "cleanse", ComponentFamily.FRAME, ComponentFamily.SENSOR);
        public static final Segment HEAVY_CHEST = new Segment(2, 1, "chest", "bastion", "repair", ComponentFamily.FRAME, ComponentFamily.FRAME);
        public static final Segment HEAVY_ARMS = new Segment(2, 2, "arms", "repel", "intercept", ComponentFamily.ACTUATOR, ComponentFamily.FRAME);
        public static final Segment HEAVY_LEGS = new Segment(2, 3, "legs", "stability", "mobility", ComponentFamily.ACTUATOR, ComponentFamily.SPRING);
        public static final Segment HEAVY_REACTOR = new Segment(2, 4, "reactor", "capacity", "recuperation", ComponentFamily.CAPACITOR, ComponentFamily.RADIATOR);
        public static final Segment HEAVY_RUNE = new Segment(2, 5, "rune", "absorb", "saturation", ComponentFamily.MANA_CIRCUIT, ComponentFamily.CONTROL_BOARD);

        private final int tree;
        private final int index;
        private final String id;
        private final String basePath;
        private final String addPath;
        private final ComponentFamily main;
        private final ComponentFamily second;

        private Segment(int tree, int index, String id, String basePath, String addPath, ComponentFamily main, ComponentFamily second) {
            this.tree = tree;
            this.index = index;
            this.id = id;
            this.basePath = basePath;
            this.addPath = addPath;
            this.main = main;
            this.second = second;
        }

        /** Номер артефакта: 0 — гаусс, 1 — клинок, 2 — тяжёлая броня ({@link GaussLook#ARTIFACTS}). */
        public int tree() {
            return tree;
        }

        /** Id артефакта: первая часть ключей прогресса. */
        public String artifact() {
            return ARTIFACTS[tree];
        }

        /** Номер сегмента в своём артефакте (0–5). */
        public int ordinal() {
            return index;
        }

        public String id() {
            return id;
        }

        /** Id пути: 0 — путь основы, 1 — путь деталей. */
        public String path(int path) {
            return path == 0 ? basePath : addPath;
        }

        /** Ключ прогресса пути: {@code <артефакт>/<сегмент>/<путь>}, значение — ступень 0–3. */
        public String key(int path) {
            return artifact() + "/" + id + "/" + path(path);
        }

        /** Ключ вершины: значение 1 — открыта. */
        public String apexKey() {
            return artifact() + "/" + id + "/apex";
        }

        public ComponentFamily main() {
            return main;
        }

        public ComponentFamily second() {
            return second;
        }

        @Override
        public String toString() {
            return artifact() + "/" + id;
        }
    }

    public static final String[] ARTIFACTS = {"gauss", "blade", "heavy"};
    public static final Segment[] SEGMENTS = {Segment.BARREL, Segment.COILS, Segment.COOLER, Segment.BATTERY, Segment.MAGAZINE, Segment.SCOPE};
    public static final Segment[] BLADE = {Segment.BLADE_EMITTER, Segment.BLADE_DRIVE, Segment.BLADE_BATTERY, Segment.BLADE_FOCUS,
            Segment.BLADE_CONTOUR, Segment.BLADE_SYNC};
    public static final Segment[] HEAVY = {Segment.HEAVY_HELMET, Segment.HEAVY_CHEST, Segment.HEAVY_ARMS, Segment.HEAVY_LEGS,
            Segment.HEAVY_REACTOR, Segment.HEAVY_RUNE};
    /** Сегменты по номеру артефакта. */
    public static final Segment[][] TREES = {SEGMENTS, BLADE, HEAVY};
    public static final String TAG = "look";

    private GaussLook() {
    }

    public static int pack(ArtifactProgress progress) {
        return pack(SEGMENTS, progress);
    }

    public static int pack(Segment[] segments, ArtifactProgress progress) {
        int look = 0;
        for (Segment s : segments) {
            look = with(look, s, Math.min(3, progress.step(s.key(0))), Math.min(3, progress.step(s.key(1))),
                    progress.step(s.apexKey()) > 0);
        }
        return look;
    }

    /** Один и тот же облик у всех сегментов (для показа и команд). */
    public static int uniform(int base, int add, boolean apex) {
        int look = 0;
        for (Segment s : SEGMENTS) {
            look = with(look, s, base, add, apex);
        }
        return look;
    }

    public static int with(int look, Segment s, int base, int add, boolean apex) {
        int shift = s.ordinal() * 5;
        int bits = (base & 3) | ((add & 3) << 2) | (apex ? 16 : 0);
        return (look & ~(31 << shift)) | (bits << shift);
    }

    public static int base(int look, Segment s) {
        return (look >> (s.ordinal() * 5)) & 3;
    }

    public static int add(int look, Segment s) {
        return (look >> (s.ordinal() * 5 + 2)) & 3;
    }

    public static boolean apex(int look, Segment s) {
        return ((look >> (s.ordinal() * 5 + 4)) & 1) != 0;
    }

    /** Ступень пути: 0 — путь основы, 1 — путь деталей. */
    public static int step(int look, Segment s, int path) {
        return path == 0 ? base(look, s) : add(look, s);
    }

    /** Сегмент гаусс-пушки, которому принадлежит кость, или {@code null} для рамы. */
    public static Segment segmentOf(String bone) {
        return segmentOf(SEGMENTS, bone);
    }

    /** Сегмент, которому принадлежит кость, или {@code null} для рамы. */
    public static Segment segmentOf(Segment[] segments, String bone) {
        for (Segment s : segments) {
            if (bone.equals(s.id) || bone.startsWith(s.id + "_")) {
                return s;
            }
        }
        return null;
    }

    /** Видна ли кость гаусс-пушки при таком облике. */
    public static boolean visible(int look, String bone) {
        return visible(SEGMENTS, look, bone);
    }

    /**
     * Видна ли кость артефакта при таком облике. Рама и кости сегментов видны всегда, части — по ступеням.
     * Имя части может продолжаться после номера: {@code arms_base1_l} (левая рука брони),
     * {@code emitter_base2_field} (поле лезвия).
     */
    public static boolean visible(Segment[] segments, int look, String bone) {
        Segment s = segmentOf(segments, bone);
        if (s == null || bone.length() <= s.id.length() + 1) {
            return true;
        }
        String part = bone.substring(s.id.length() + 1);
        if (part.startsWith("base") && part.length() >= 5 && Character.isDigit(part.charAt(4))) {
            return part.charAt(4) - '0' == base(look, s);
        }
        if (part.startsWith("add") && part.length() >= 4 && Character.isDigit(part.charAt(3))) {
            return part.charAt(3) - '0' <= add(look, s);
        }
        if (part.startsWith("apex")) {
            return apex(look, s);
        }
        return true;
    }

    /** Доля прокачки облика 0–1 (все ступени и вершины). */
    public static double progress(int look) {
        int sum = 0;
        for (Segment s : SEGMENTS) {
            sum += base(look, s) + add(look, s) + (apex(look, s) ? 1 : 0);
        }
        return sum / (double) (SEGMENTS.length * 7);
    }
}
