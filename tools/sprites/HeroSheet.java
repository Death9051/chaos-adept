import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Лист скинов героев (вид спереди, с верхним слоем) для просмотра.
 * Запуск: java tools/sprites/HeroSheet.java core/src/main/resources/assets/adept_core/textures/entity/hero tools/sprites/heroes-sheet.png
 * Столбцы: воин, лучник, следопыт, командир, маг, целитель; строки — варианты 1–3.
 */
public class HeroSheet {
    static void part(Graphics2D g, BufferedImage s, int sx, int sy, int w, int h, int dx, int dy, int k) {
        g.drawImage(s, dx, dy, dx + w * k, dy + h * k, sx, sy, sx + w, sy + h, null);
    }
    public static void main(String[] a) throws Exception {
        String[] cls = {"warrior", "archer", "tracker", "commander", "mage", "healer"};
        int k = 6, cw = 16 * k + 24, chh = 32 * k + 24;
        BufferedImage out = new BufferedImage(cw * 6, chh * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0x6E8F5A)); g.fillRect(0, 0, out.getWidth(), out.getHeight());
        for (int c = 0; c < 6; c++) for (int n = 1; n <= 3; n++) {
            BufferedImage s = ImageIO.read(new File(a[0], cls[c] + "_" + n + ".png"));
            int ox = c * cw + 12, oy = (n - 1) * chh + 12;
            // base then overlay
            for (int layer = 0; layer < 2; layer++) {
                int o = layer;
                part(g, s, 8 + 32 * o, 8, 8, 8, ox + 4 * k, oy, k);                      // head / hat
                part(g, s, 20, 20 + 16 * o, 8, 12, ox + 4 * k, oy + 8 * k, k);           // body
                part(g, s, 44, 20 + 16 * o, 4, 12, ox, oy + 8 * k, k);                   // right arm
                part(g, s, o == 0 ? 36 : 52, 52, 4, 12, ox + 12 * k, oy + 8 * k, k);     // left arm
                part(g, s, 4, 20 + 16 * o, 4, 12, ox + 4 * k, oy + 20 * k, k);           // right leg
                part(g, s, o == 0 ? 20 : 4, 52, 4, 12, ox + 8 * k, oy + 20 * k, k);      // left leg
            }
        }
        ImageIO.write(out, "png", new File(a[1]));
    }
}
