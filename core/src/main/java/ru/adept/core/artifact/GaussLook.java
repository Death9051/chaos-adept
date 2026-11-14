package ru.adept.core.artifact;

import ru.adept.core.component.ComponentFamily;
import ru.adept.core.data.ArtifactProgress;

/**
 * Облик гаусс-пушки по прокачке («Артефакты», «Прокачка по сегментам»): у сегмента один путь меняет основу
 * (ступень 0–3 выбирает вариант основы), второй добавляет навесные детали (по одной на ступень), вершина — отдельная деталь.
 *
 * Облик упакован в одно число (по 5 бит на сегмент: 2 — основа, 2 — детали, 1 — вершина) и лежит в данных
 * предмета под ключом {@code look}: так рендер видит его и в руке, и в рамке, и у других игроков.
 * Кости модели: {@code <сегмент>_base0..3}, {@code <сегмент>_add1..3}, {@code <сегмент>_apex}
 * (генератор {@code tools/models/GaussModel.java}).
 */
public final class GaussLook {
    /** Сегмент: id кости и ключа прогресса, путь основы, путь деталей, два семейства компонентов (основное и второе). */
    public enum Segment {
        BARREL("barrel", "boost", "tempo", ComponentFamily.COIL, ComponentFamily.FRAME),
        COILS("coils", "stability", "economy", ComponentFamily.COIL, ComponentFamily.WIRING),
        COOLER("cooler", "heatsink", "purge", ComponentFamily.RADIATOR, ComponentFamily.NOZZLE),
        BATTERY("battery", "capacity", "recuperation", ComponentFamily.CAPACITOR, ComponentFamily.WIRING),
        MAGAZINE("magazine", "volume", "pattern", ComponentFamily.ACTUATOR, ComponentFamily.SPRING),
        SCOPE("scope", "lens", "scanner", ComponentFamily.LENS, ComponentFamily.SENSOR);

        private final String id;
        private final String basePath;
        private final String addPath;
        private final ComponentFamily main;
        private final ComponentFamily second;

        Segment(String id, String basePath, String addPath, ComponentFamily main, ComponentFamily second) {
            this.id = id;
            this.basePath = basePath;
            this.addPath = addPath;
            this.main = main;
            this.second = second;
        }

        public String id() {
            return id;
        }

        /** Id пути: 0 — путь основы, 1 — путь деталей. */
        public String path(int path) {
            return path == 0 ? basePath : addPath;
        }

        /** Ключ прогресса пути: {@code gauss/<сегмент>/<путь>}, значение — ступень 0–3. */
        public String key(int path) {
            return "gauss/" + id + "/" + path(path);
        }

        /** Ключ вершины: значение 1 — открыта. */
        public String apexKey() {
            return "gauss/" + id + "/apex";
        }

        public ComponentFamily main() {
            return main;
        }

        public ComponentFamily second() {
            return second;
        }
    }

    public static final Segment[] SEGMENTS = Segment.values();
    public static final String TAG = "look";

    private GaussLook() {
    }

    public static int pack(ArtifactProgress progress) {
        int look = 0;
        for (Segment s : SEGMENTS) {
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

    /** Сегмент, которому принадлежит кость, или {@code null} для рамы. */
    public static Segment segmentOf(String bone) {
        for (Segment s : SEGMENTS) {
            if (bone.equals(s.id) || bone.startsWith(s.id + "_")) {
                return s;
            }
        }
        return null;
    }

    /** Видна ли кость при таком облике. Рама и кости сегментов видны всегда, части — по ступеням. */
    public static boolean visible(int look, String bone) {
        Segment s = segmentOf(bone);
        if (s == null || bone.length() <= s.id.length() + 1) {
            return true;
        }
        String part = bone.substring(s.id.length() + 1);
        if (part.startsWith("base") && part.length() == 5) {
            return part.charAt(4) - '0' == base(look, s);
        }
        if (part.startsWith("add") && part.length() == 4) {
            return part.charAt(3) - '0' <= add(look, s);
        }
        if (part.equals("apex")) {
            return apex(look, s);
        }
        return true;
    }

    /** Доля прокачки облика 0–1 (все ступени и вершины): окно стола по ней уходит от магии к технике. */
    public static double progress(int look) {
        int sum = 0;
        for (Segment s : SEGMENTS) {
            sum += base(look, s) + add(look, s) + (apex(look, s) ? 1 : 0);
        }
        return sum / (double) (SEGMENTS.length * 7);
    }
}
