import java.util.ArrayList;
import java.util.List;

/**
 * Печатает строки локализации названий компонентов (ключи item.adept_core.<id>).
 * Ранг передаётся материалом, вариация — словом: «рунная» или «литая» (у линзы — «шлифованная»).
 *
 * Запуск: java tools/sprites/ComponentNames.java ru   (или en)
 * Вывод вставляется в lang/ru_ru.json и lang/en_us.json.
 */
public class ComponentNames {
    // род: m, f, n
    record Family(String id, String nature, char gender, String ru, String en, String[] mats) {}

    static final String[] CU_AG_EL = {"copper", "silver", "electrum"};
    static final String[] BR_ST_TI = {"bronze", "steel", "titanium"};
    static final String[] BR_DU_TI = {"bronze", "dural", "titanium"};
    static final String[] CU_DU_TI = {"copper", "dural", "titanium"};
    static final String[] GEMS = {"quartz", "amethyst", "diamond"};

    static final List<Family> FAMILIES = List.of(
            new Family("wiring", "tech", 'm', "жгут проводов", "Wire Harness", CU_AG_EL),
            new Family("coil", "both", 'f', "катушка", "Coil", CU_AG_EL),
            new Family("capacitor", "both", 'm', "конденсатор", "Capacitor", BR_DU_TI),
            new Family("frame", "tech", 'f', "рама", "Frame", BR_ST_TI),
            new Family("radiator", "tech", 'm', "радиатор", "Radiator", CU_DU_TI),
            new Family("nozzle", "tech", 'n', "сопло", "Nozzle", BR_ST_TI),
            new Family("actuator", "both", 'f', "шестерня", "Gear", BR_ST_TI),
            new Family("spring", "both", 'f', "пружина", "Spring", BR_ST_TI),
            new Family("lens", "both", 'f', "линза", "Lens", GEMS),
            new Family("sensor", "both", 'm', "датчик", "Sensor", BR_DU_TI),
            new Family("emitter", "both", 'm', "излучатель", "Emitter", CU_AG_EL),
            new Family("mana_circuit", "magic", 'f', "мановая жила", "Mana Vein", CU_AG_EL),
            new Family("matrix", "magic", 'f', "решётка", "Lattice", GEMS),
            new Family("control_board", "both", 'f', "плата", "Board", CU_AG_EL));

    /** Прилагательное: основа и тип окончания (ый / ой). */
    static String adj(String stem, boolean stressedEnding, char gender) {
        return stem + switch (gender) {
            case 'f' -> "ая";
            case 'n' -> "ое";
            default -> stressedEnding ? "ой" : "ый";
        };
    }

    static String material(String mat, char g) {
        return switch (mat) {
            case "copper" -> adj("медн", false, g);
            case "bronze" -> adj("бронзов", false, g);
            case "steel" -> adj("стальн", true, g);
            case "silver" -> adj("серебрян", false, g);
            case "dural" -> adj("дюралев", false, g);
            case "titanium" -> adj("титанов", false, g);
            case "electrum" -> adj("электрумов", false, g);
            case "quartz" -> adj("кварцев", false, g);
            case "amethyst" -> adj("аметистов", false, g);
            case "diamond" -> adj("алмазн", false, g);
            default -> throw new IllegalArgumentException(mat);
        };
    }

    static String materialEn(String mat) {
        return mat.equals("dural") ? "Duralumin" : cap(mat);
    }

    static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void main(String[] args) {
        boolean ru = args.length == 0 || args[0].equals("ru");
        List<String> lines = new ArrayList<>();
        for (Family f : FAMILIES) {
            List<String> variants = switch (f.nature) {
                case "magic" -> List.of("magic");
                case "tech" -> List.of("tech");
                default -> List.of("magic", "tech");
            };
            for (String variant : variants) {
                for (int rank = 1; rank <= 3; rank++) {
                    boolean both = f.nature.equals("both");
                    String id = both ? f.id + "_" + rank + "_" + variant : f.id + "_" + rank;
                    String mat = f.mats[rank - 1];
                    String name;
                    if (ru) {
                        String word = !both ? "" : variant.equals("magic") ? adj("рунн", false, f.gender)
                                : f.id.equals("lens") ? "шлифованная" : adj("лит", true, f.gender);
                        name = cap((word + " " + material(mat, f.gender) + " " + f.ru).trim());
                    } else {
                        String word = !both ? "" : variant.equals("magic") ? "Rune"
                                : f.id.equals("lens") ? "Ground" : "Cast";
                        name = (word + " " + materialEn(mat) + " " + f.en).trim();
                    }
                    lines.add("  \"item.adept_core." + id + "\": \"" + name + "\"");
                }
            }
        }
        System.out.println(String.join(",\n", lines));
    }
}
