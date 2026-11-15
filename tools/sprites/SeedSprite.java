import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Спрайт «Зачатка» 16×16 (docs/context/models.md, «Спрайты предметов»): бронзовое семя-капсула Альянса
 * с окном, где светится ядро маны, стальной пояс посередине и росток сверху. Силуэт — округлое семя
 * с ростком, ни у одного компонента такого нет. Палитры — из ComponentSprites (bronze, сталь GUN, MANA).
 * Свет сверху-слева, контур в тон материала, без одиночных пикселей.
 *
 * Запуск из корня репозитория (Java 17+):
 *   java tools/sprites/SeedSprite.java core/src/main/resources/assets/adept_core [лист.png]
 * Второй аргумент — увеличенный просмотр ×8 и полоса ×2 на фоне слота.
 */
public class SeedSprite {
    static final int N = 16;
    static final int[] BRONZE = {0xF6D488, 0xDCA850, 0xB47C36, 0x875828, 0x5E3B1E, 0x392313};
    static final int[] GUN = {0x7C818D, 0x555964, 0x353740, 0x1E1F26};
    static final int[] MANA = {0xE6F6FF, 0x7CC4FF, 0x2F8CFF, 0x153F9E};

    /** Карта: . пусто, 1–5 бронза (блик → тень), O контур бронзы, K k j o сталь, W M m n мана. */
    static final String[] MAP = {
            "................",
            "..........MW....",
            ".........Mmn....",
            "........k.......",
            "......OOkOO.....",
            ".....O1122O.....",
            "....O112233O....",
            "....O12nWM3O....",
            "....O1nMWmn4O...",
            "....oKKkkjjjo...",
            "....oKkkjjjjo...",
            "....O2nmmmn4O...",
            "....O233344O....",
            ".....O3444O.....",
            "......OOOO......",
            "................",
    };

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "core/src/main/resources/assets/adept_core");
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            String row = MAP[y];
            if (row.length() != N) {
                throw new IllegalStateException("Строка " + y + " не 16 знаков: " + row.length());
            }
            for (int x = 0; x < N; x++) {
                int c = color(row.charAt(x));
                img.setRGB(x, y, c);
            }
        }
        Path out = assets.resolve("textures/item/seed.png");
        Files.createDirectories(out.getParent());
        ImageIO.write(img, "png", out.toFile());
        System.out.println("Записан " + out);
        if (args.length > 1) {
            int s = 8;
            BufferedImage sheet = new BufferedImage(N * s + 8 + 36 + 8, N * s, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < N * s; y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    sheet.setRGB(x, y, 0xFF2B2630);
                }
            }
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    int c = img.getRGB(x, y);
                    if ((c >>> 24) == 0) {
                        continue;
                    }
                    for (int dy = 0; dy < s; dy++) {
                        for (int dx = 0; dx < s; dx++) {
                            sheet.setRGB(x * s + dx, y * s + dy, c);
                        }
                    }
                }
            }
            // ×2 на фоне ванильного слота (серый 0x8B8B8B), рядом с крупным видом
            int ox = N * s + 8;
            for (int y = 0; y < 36; y++) {
                for (int x = 0; x < 36; x++) {
                    sheet.setRGB(ox + x, y, 0xFF8B8B8B);
                }
            }
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    int c = img.getRGB(x, y);
                    if ((c >>> 24) == 0) {
                        continue;
                    }
                    for (int d = 0; d < 4; d++) {
                        sheet.setRGB(ox + 2 + x * 2 + (d & 1), 2 + y * 2 + (d >> 1), c);
                    }
                }
            }
            ImageIO.write(sheet, "png", Path.of(args[1]).toFile());
            System.out.println("Лист: " + args[1]);
        }
    }

    static int color(char ch) {
        int rgb = switch (ch) {
            case '1', '2', '3', '4', '5' -> BRONZE[ch - '1'];
            case 'O' -> BRONZE[5];
            case 'K' -> GUN[0];
            case 'k' -> GUN[1];
            case 'j' -> GUN[2];
            case 'o' -> GUN[3];
            case 'W' -> MANA[0];
            case 'M' -> MANA[1];
            case 'm' -> MANA[2];
            case 'n' -> MANA[3];
            default -> -1;
        };
        return rgb < 0 ? 0 : 0xFF000000 | rgb;
    }
}
