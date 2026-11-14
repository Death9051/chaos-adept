package ru.adept.core.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.adept.core.AdeptCore;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.config.BalanceConfig;

/**
 * Экран игрока, гаусс-пушка в руке («Артефакты», «Интерфейсы»): заряд аккумулятора, нагрев ствола и магазин —
 * справа от панели быстрого доступа. Заряд — красная шкала (электричество) с числом; нагрев — шкала с отметкой
 * предела и процентом, при перегреве мигает надпись и видно, сколько секунд пушка молчит.
 * Смысл продублирован значком и текстом, не только цветом. Рисуется, пока пушка в основной руке и интерфейс не скрыт.
 */
@EventBusSubscriber(modid = AdeptCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GaussHud {
    private static final int BACK = 0xC0101018;
    private static final int FRAME = 0xFFB08D57;
    private static final int EMPTY = 0xFF2A2C34;
    private static final int CHARGE = 0xFFF03A2C;
    private static final int CHARGE_LOW = 0xFF8E1418;
    private static final int HEAT = 0xFFFFA040;
    private static final int HEAT_HIGH = 0xFFFF5030;
    private static final int TEXT = 0xFFE8E0D0;
    private static final int WARN = 0xFFFF7060;
    private static final int W = 78;
    private static final int H = 32;

    private GaussHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(AdeptCore.MOD_ID, "gauss_gun"), GaussHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof GaussGunItem)) {
            return;
        }
        CompoundTag tag = GaussGunItem.data(stack);
        long now = mc.level.getGameTime();
        int capacity = Math.max(1, GaussGunItem.capacity());
        int charge = tag.getInt("charge");
        double heat = GaussGunItem.heat(tag, now);
        long overheat = tag.getLong("overheat_until") - now;
        long reload = tag.getLong("reload_until") - now;
        int magazine = tag.getList("magazine", net.minecraft.nbt.Tag.TAG_STRING).size();

        int x = g.guiWidth() / 2 + 91 + 34;
        int y = g.guiHeight() - H - 2;
        g.fill(x, y, x + W, y + H, BACK);
        g.renderOutline(x, y, W, H, FRAME);

        // Заряд: значок молнии, шкала, число.
        bolt(g, x + 4, y + 4, charge > 0 ? CHARGE : CHARGE_LOW);
        bar(g, x + 12, y + 4, 36, charge / (double) capacity, charge * 4 < capacity ? CHARGE_LOW : CHARGE, -1);
        String chargeText = Integer.toString(charge);
        g.drawString(mc.font, chargeText, x + W - 3 - mc.font.width(chargeText), y + 3, charge > 0 ? TEXT : WARN, true);

        // Нагрев: значок пламени, шкала с отметкой предела, процент.
        flame(g, x + 4, y + 14, heat >= 0.8 ? HEAT_HIGH : HEAT);
        bar(g, x + 12, y + 14, 36, heat, heat >= 0.8 ? HEAT_HIGH : HEAT, 0.8);
        String heatText = Math.round(heat * 100) + "%";
        g.drawString(mc.font, heatText, x + W - 3 - mc.font.width(heatText), y + 13, heat >= 0.8 ? WARN : TEXT, true);

        // Строка состояния: перегрев, перезарядка или магазин.
        Component status;
        int color = TEXT;
        if (overheat > 0) {
            status = Component.translatable("adept_core.gauss.hud.overheat", String.format(java.util.Locale.ROOT, "%.1f", overheat / 20.0));
            color = (now / 5) % 2 == 0 ? WARN : TEXT;
        } else if (reload > 0) {
            status = Component.translatable("adept_core.gauss.hud.reload", String.format(java.util.Locale.ROOT, "%.1f", reload / 20.0));
        } else {
            status = Component.translatable("adept_core.gauss.hud.magazine", magazine, BalanceConfig.GAUSS_MAGAZINE.get());
            color = magazine == 0 ? WARN : TEXT;
        }
        g.drawString(mc.font, status, x + 4, y + 23, color, true);
    }

    /** Шкала 0–1 высотой 6; {@code mark} — отметка предела (доля ширины) или -1. */
    private static void bar(GuiGraphics g, int x, int y, int width, double value, int color, double mark) {
        g.fill(x, y, x + width, y + 6, EMPTY);
        int filled = (int) Math.round(width * Math.max(0.0, Math.min(1.0, value)));
        g.fill(x, y, x + filled, y + 6, color);
        g.fill(x, y, x + filled, y + 1, 0x40FFFFFF);
        if (mark >= 0) {
            int mx = x + (int) Math.round(width * mark);
            g.fill(mx, y - 1, mx + 1, y + 7, TEXT);
        }
    }

    private static void bolt(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 3, y, x + 5, y + 1, color);
        g.fill(x + 2, y + 1, x + 4, y + 2, color);
        g.fill(x + 1, y + 2, x + 5, y + 3, color);
        g.fill(x + 2, y + 3, x + 4, y + 4, color);
        g.fill(x + 1, y + 4, x + 3, y + 5, color);
        g.fill(x, y + 5, x + 2, y + 6, color);
    }

    private static void flame(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 2, y, x + 3, y + 1, color);
        g.fill(x + 1, y + 1, x + 4, y + 2, color);
        g.fill(x + 1, y + 2, x + 5, y + 3, color);
        g.fill(x, y + 3, x + 5, y + 5, color);
        g.fill(x + 1, y + 5, x + 4, y + 6, color);
        g.fill(x + 2, y + 3, x + 3, y + 5, 0xFFFFE080);
    }
}
