import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Генератор 2D-спрайтов компонентов (16×16) и моделей предметов.
 * Каждое семейство нарисовано вручную пиксельной картой в components.txt, свет сверху-слева.
 * Материал ранга красит тона 1–5 (совпадает с названиями из ComponentNames),
 * вариация — акцент: у рунной синий кристалл и руны, у литой красный индикатор и болты.
 *
 * Запуск из корня репозитория (нужна Java 17+):
 *   java tools/sprites/ComponentSprites.java core/src/main/resources/assets/adept_core [лист.png]
 * Второй аргумент — увеличенный лист всех спрайтов для просмотра.
 *
 * Обозначения в картах (components.txt):
 *   .          прозрачно
 *   1 2 3 4 5  материал ранга от блика к тени, O — его контур
 *   K k j      тёмная сталь (светлая, средняя, тёмная), o — нейтральный контур
 *   C c D      акцент вариации: кристалл маны у рунной, красный индикатор у литой
 *   W M m n    мана: блик, свет, середина, контур; R r — руны (светлая, тусклая)
 *   P p        текстолит платы
 *   w          белый блик
 * После карты — строки «magic:» и «tech:» с правками вариации вида «x,y,символ».
 */
public class ComponentSprites {
    static final int N = 16;

    record Family(String id, String nature, String[] mats) {}

    static final String[] CU_AG_EL = {"copper", "silver", "electrum"};
    static final String[] BR_ST_TI = {"bronze", "steel", "titanium"};
    static final String[] BR_DU_TI = {"bronze", "dural", "titanium"};
    static final String[] CU_DU_TI = {"copper", "dural", "titanium"};
    static final String[] GEMS = {"quartz", "amethyst", "diamond"};

    // Список, природа и материалы должны совпадать с ComponentFamily и ComponentNames.
    static final List<Family> FAMILIES = List.of(
            new Family("wiring", "tech", CU_AG_EL), new Family("coil", "both", CU_AG_EL),
            new Family("capacitor", "both", BR_DU_TI), new Family("frame", "tech", BR_ST_TI),
            new Family("radiator", "tech", CU_DU_TI), new Family("nozzle", "tech", BR_ST_TI),
            new Family("actuator", "both", BR_ST_TI), new Family("spring", "both", BR_ST_TI),
            new Family("lens", "both", GEMS), new Family("sensor", "both", BR_DU_TI),
            new Family("emitter", "both", CU_AG_EL), new Family("mana_circuit", "magic", CU_AG_EL),
            new Family("matrix", "magic", GEMS), new Family("control_board", "both", CU_AG_EL));

    // Материалы: тона 1–5 и контур
    static final Map<String, int[]> MATERIALS = Map.of(
            "copper", new int[]{0xFBB08C, 0xE5825A, 0xBF5C3C, 0x91432E, 0x66302A, 0x3E1D1C},
            "bronze", new int[]{0xF6D488, 0xDCA850, 0xB47C36, 0x875828, 0x5E3B1E, 0x392313},
            "steel", new int[]{0xE6EBF0, 0xB6BEC8, 0x87909D, 0x5E6672, 0x41474F, 0x25282E},
            "silver", new int[]{0xFFFFFF, 0xDDE8F0, 0xAFC0CE, 0x8091A3, 0x586575, 0x30394A},
            "dural", new int[]{0xF0F5F2, 0xC5D2CC, 0x99AAA6, 0x6E807E, 0x4A5A5A, 0x2A3536},
            "titanium", new int[]{0xF5F1FF, 0xCFCBEA, 0xA29FC8, 0x73719C, 0x4D4B72, 0x2B2A47},
            "electrum", new int[]{0xFFFACC, 0xF7E47E, 0xDCBC4E, 0xAC8A32, 0x775C24, 0x473515},
            "quartz", new int[]{0xFFFFFF, 0xF4E8E2, 0xDFCAC2, 0xB99F99, 0x8A7074, 0x54434C},
            "amethyst", new int[]{0xF5DAFF, 0xD3A0F4, 0xAA6FDA, 0x7B49AE, 0x52307E, 0x2E1A4D},
            "diamond", new int[]{0xF2FFFF, 0xAAF5F0, 0x62DCDA, 0x32AAB4, 0x1F7284, 0x123F52});

    static final int[] GUN = {0x7C818D, 0x555964, 0x353740, 0x1E1F26};            // K k j o
    static final int[] MANA = {0xF0FDFF, 0x8CE8FF, 0x38AEFF, 0x1B55B8};           // W M m n
    static final int[] RED = {0xFF9A86, 0xF03A2C, 0x8E1418};
    static final int[] BOARD = {0x3C8A5C, 0x275F40};

    static int color(char ch, int[] mat, boolean magic) {
        return switch (ch) {
            case '1', '2', '3', '4', '5' -> mat[ch - '1'];
            case 'O' -> mat[5];
            case 'K' -> GUN[0];
            case 'k' -> GUN[1];
            case 'j' -> GUN[2];
            case 'o' -> GUN[3];
            case 'C' -> magic ? MANA[1] : RED[0];
            case 'c' -> magic ? MANA[2] : RED[1];
            case 'D' -> magic ? MANA[3] : RED[2];
            case 'W' -> MANA[0];
            case 'M', 'R' -> MANA[1];
            case 'm', 'r' -> MANA[2];
            case 'n' -> MANA[3];
            case 'P' -> BOARD[0];
            case 'p' -> BOARD[1];
            case 'w' -> 0xFFFFFF;
            default -> throw new IllegalArgumentException("Неизвестный символ: " + ch);
        };
    }

    /** Карты семейств: id → 16 строк; правки вариаций: «id/вариация» → список «x,y,символ». */
    static final Map<String, List<String>> TEMPLATES = new HashMap<>();
    static final Map<String, String> PATCHES = new HashMap<>();

    static void load(Path file) throws IOException {
        String id = null;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("==")) {
                id = line.substring(2).strip();
                TEMPLATES.put(id, new ArrayList<>());
            } else if (line.startsWith("magic:") || line.startsWith("tech:")) {
                String variant = line.substring(0, line.indexOf(':'));
                PATCHES.merge(id + "/" + variant, line.substring(line.indexOf(':') + 1).strip(), (a, b) -> a + " " + b);
            } else {
                if (line.length() != N) {
                    throw new IllegalStateException(id + ": длина строки " + line.length() + ": " + line);
                }
                TEMPLATES.get(id).add(line);
            }
        }
    }

    static char[][] canvas(String id, String variant) {
        List<String> rows = TEMPLATES.get(id);
        if (rows == null || rows.size() != N) {
            throw new IllegalStateException(id + ": строк " + (rows == null ? 0 : rows.size()));
        }
        char[][] c = new char[N][];
        for (int y = 0; y < N; y++) {
            c[y] = rows.get(y).toCharArray();
        }
        String patch = PATCHES.get(id + "/" + variant);
        if (patch != null && !patch.isBlank()) {
            for (String p : patch.split("\\s+")) {
                String[] t = p.split(",");
                c[Integer.parseInt(t[1])][Integer.parseInt(t[0])] = t[2].charAt(0);
            }
        }
        return c;
    }

    static BufferedImage render(char[][] c, int[] mat, boolean magic) {
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (c[y][x] != '.') {
                    img.setRGB(x, y, 0xFF000000 | color(c[y][x], mat, magic));
                }
            }
        }
        return img;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Укажите папку assets/adept_core и, если нужно, путь листа");
            System.exit(1);
        }
        load(Path.of("tools/sprites/components.txt"));
        Path assets = Path.of(args[0]);
        Path tex = assets.resolve("textures/item/component");
        Path models = assets.resolve("models/item");
        Files.createDirectories(tex);
        Files.createDirectories(models);
        int scale = 6, cell = N * scale + 12, cols = 12;
        BufferedImage sheet = new BufferedImage(cols * cell, 6 * cell + 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                sheet.setRGB(x, y, 0xC6C6C6);
            }
        }
        int count = 0;
        for (Family f : FAMILIES) {
            List<String> variants = switch (f.nature) {
                case "magic" -> List.of("magic");
                case "tech" -> List.of("tech");
                default -> List.of("magic", "tech");
            };
            for (String variant : variants) {
                for (int rank = 1; rank <= 3; rank++) {
                    String id = f.nature.equals("both") ? f.id + "_" + rank + "_" + variant : f.id + "_" + rank;
                    BufferedImage img = render(canvas(f.id, variant), MATERIALS.get(f.mats[rank - 1]), variant.equals("magic"));
                    ImageIO.write(img, "png", tex.resolve(id + ".png").toFile());
                    Files.writeString(models.resolve(id + ".json"), """
                            {
                              "parent": "minecraft:item/generated",
                              "textures": {
                                "layer0": "adept_core:item/component/%s"
                              }
                            }
                            """.formatted(id), StandardCharsets.UTF_8);
                    paste(sheet, img, (count % cols) * cell, (count / cols) * cell, cell, scale);
                    // нижняя полоса листа — в настоящем размере ×2, как в инвентаре
                    paste(sheet, img, count * 36 % (cols * cell), 6 * cell + (count * 36 / (cols * cell)) * 0 + 2, 36, 2);
                    count++;
                }
            }
        }
        if (args.length > 1) {
            ImageIO.write(sheet, "png", Path.of(args[1]).toFile());
        }
        System.out.println("Спрайтов и моделей: " + count);
    }

    /** Ячейка листа: фон как у слота инвентаря, спрайт увеличен без сглаживания. */
    static void paste(BufferedImage sheet, BufferedImage img, int ox, int oy, int cell, int scale) {
        int pad = (cell - N * scale) / 2;
        for (int y = 0; y < cell; y++) {
            for (int x = 0; x < cell; x++) {
                boolean border = x < 1 || y < 1 || x >= cell - 1 || y >= cell - 1;
                if (ox + x < sheet.getWidth() && oy + y < sheet.getHeight()) {
                    sheet.setRGB(ox + x, oy + y, border ? 0xC6C6C6 : 0x8B8B8B);
                }
            }
        }
        for (int y = 0; y < N * scale; y++) {
            for (int x = 0; x < N * scale; x++) {
                int argb = img.getRGB(x / scale, y / scale);
                if ((argb >>> 24) != 0 && ox + pad + x < sheet.getWidth() && oy + pad + y < sheet.getHeight()) {
                    sheet.setRGB(ox + pad + x, oy + pad + y, argb & 0xFFFFFF);
                }
            }
        }
    }
}
