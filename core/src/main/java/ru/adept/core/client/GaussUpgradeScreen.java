package ru.adept.core.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.GaussLook;
import ru.adept.core.artifact.GaussLook.Segment;
import ru.adept.core.artifact.StepTexts;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.registry.ModItems;
import ru.adept.core.table.GaussUpgrades;
import ru.adept.core.table.GaussUpgrades.Node;
import ru.adept.core.table.GaussUpgrades.State;
import ru.adept.core.table.TableActions;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Мастерская артефакта: прокачка гаусс-пушки на столе («Артефакты», «Интерфейсы»). Раскладка по согласованному
 * концепту: шапка с эмблемой и общим прогрессом; слева проекция модели на голографической площадке (вращается,
 * камера наезжает на выбранный сегмент, остальные части темнеют), под ней вкладки сегментов и полоса магазина;
 * справа дерево сегмента из шестигранных узлов (два пути сверху вниз сходятся к вершине) и карточка улучшения:
 * описание, цена в компонентах, кнопки «Примерить» (показать деталь на модели до покупки) и «Вплести».
 *
 * Правила интерфейса (навык ui-ux-pro-max): состояние узла видно не только цветом (заливка, римская цифра, замок);
 * у недоступной кнопки причина написана на ней; описание и цена — в окне, а не в подсказке; у всего кликабельного
 * есть отклик на наведение. Палитра концепта: тёмно-синий фон, бирюзовый акцент, золото для доступного.
 *
 * Экран не знает о меню стола: облик и нажатия приходят через {@code look} и {@code press},
 * поэтому тот же экран показывается в сценах автотеста. Размер подстраивается под окно игры.
 */
public class GaussUpgradeScreen extends Screen {
    private static final int BG = 0xFF0A0F15;
    private static final int PANEL = 0xFF101822;
    private static final int PANEL_DARK = 0xFF0E151E;
    private static final int BORDER = 0xFF1F2D3B;
    private static final int BORDER_LIGHT = 0xFF2B3B4C;
    private static final int ACCENT = 0xFF3FE0D0;
    private static final int ACCENT_BG = 0xFF12232A;
    private static final int GOLD = 0xFFE2B95C;
    private static final int GOLD_BG = 0xFF2A2214;
    private static final int TITLE = 0xFFF3E3B5;
    private static final int LORE = 0xFFC9B98F;
    private static final int TEXT = 0xFFE6EEF5;
    private static final int MUTED = 0xFF93A7BA;
    private static final int DIM = 0xFF5B6F84;
    private static final int LOCK_BG = 0xFF121A24;
    private static final int CARD_BG = 0xFF17140D;
    private static final int CARD_BORDER = 0xFF4A3D20;
    private static final int BAD = 0xFFFF5A6E;
    private static final int INK = 0xFF0A0F15;

    private static final int NODE_W = 18;
    private static final int NODE_H = 14;

    /** Центр и размер сегмента в модели (единицы модели): на них наезжает камера. Порядок — {@link GaussLook#SEGMENTS}. */
    private static final float[][] FOCUS = {
            {0, 19, -33, 2.3f}, {0, 20, -14, 2.6f}, {0, 11, -14, 2.6f},
            {0, 15, 21, 2.4f}, {0, 9, -1.5f, 2.6f}, {0, 28, 3, 2.4f}};
    private static final float[] WHOLE = {0, 18, -9, 1.0f};

    private final Screen parent;
    private final IntSupplier look;
    private final IntConsumer press;
    private final ItemStack preview = new ItemStack(ModItems.GAUSS_GUN.get());

    // Раскладка, считается в init().
    private int w;
    private int h;
    private int left;
    private int top;
    private int bodyY;
    private int bodyH;
    private int lx;
    private int lw;
    private int rx;
    private int rw;
    private int projH;
    private int descLines;
    private boolean showLore;

    /** Выбранный сегмент: -1 — оружие целиком. */
    private int segment = -1;
    private Node node;
    /** Примерка: выбранная ступень показана на модели, хотя ещё не открыта. */
    private boolean tryOn;
    private float yaw = 118f;
    private float pitch = 14f;
    private float userZoom = 1f;
    private final float[] camera = WHOLE.clone();
    private long lastFrame;
    private boolean dragging;

    public GaussUpgradeScreen(Screen parent, IntSupplier look, IntConsumer press) {
        super(Component.translatable("adept_core.gauss_table.title"));
        this.parent = parent;
        this.look = look;
        this.press = press;
    }

    /** Открыть сразу на сегменте и узле (для сцен автотеста). */
    public GaussUpgradeScreen select(Segment s, int path, int step) {
        segment = s.ordinal();
        node = path == GaussUpgrades.APEX ? GaussUpgrades.apex(s) : new Node(s, path, step);
        System.arraycopy(FOCUS[segment], 0, camera, 0, 4);
        return this;
    }

    /** Включить примерку выбранного узла (для сцен автотеста). */
    public GaussUpgradeScreen tryOn() {
        tryOn = true;
        return this;
    }

    @Override
    protected void init() {
        w = Mth.clamp(width - 12, 400, 560);
        h = Mth.clamp(height - 12, 240, 320);
        left = (width - w) / 2;
        top = (height - h) / 2;
        bodyY = top + 36;
        bodyH = h - 44;
        lx = left + 8;
        lw = (w - 22) * 56 / 100;
        rx = lx + lw + 6;
        rw = w - 22 - lw;
        projH = bodyH - 34 - 34 - 8;
        showLore = bodyH >= 250;
        descLines = bodyH >= 250 ? 5 : bodyH >= 205 ? 3 : 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    // ---- состояние ----

    private State state(int current) {
        return GaussUpgrades.state(current, node);
    }

    private boolean canPay() {
        return minecraft != null && minecraft.player != null && GaussUpgrades.cost(node).canPay(minecraft.player);
    }

    /** Облик для проекции: с примеркой — как будто выбранная ступень уже открыта. */
    private int shownLook(int current) {
        if (!tryOn || node == null || state(current) == State.DONE) {
            return current;
        }
        Segment s = node.segment();
        int base = GaussLook.base(current, s);
        int add = GaussLook.add(current, s);
        boolean apex = GaussLook.apex(current, s);
        if (node.isApex()) {
            apex = true;
        } else if (node.path() == 0) {
            base = Math.max(base, node.step());
        } else {
            add = Math.max(add, node.step());
        }
        return GaussLook.with(current, s, base, add, apex);
    }

    private static int done(int current, Segment s) {
        return GaussLook.base(current, s) + GaussLook.add(current, s) + (GaussLook.apex(current, s) ? 1 : 0);
    }

    // ---- раскладка ----

    private int[] backRect() {
        return new int[]{left + w - 8 - 62, top + 10, 62, 16};
    }

    private int[] previewRect() {
        return new int[]{lx, bodyY, lw, projH};
    }

    private int[] segmentRect(int i) {
        int tw = (lw - 4) / 3;
        return new int[]{lx + (i % 3) * (tw + 2), bodyY + projH + 4 + (i / 3) * 17, tw, 16};
    }

    private int[] magazineRect() {
        return new int[]{lx, bodyY + bodyH - 34, lw, 34};
    }

    private int[] cardRect() {
        int ch = 72 + descLines * 9;
        return new int[]{rx + 6, bodyY + bodyH - 6 - ch, rw - 12, ch};
    }

    private int[] tryRect() {
        int[] c = cardRect();
        return new int[]{c[0] + 6, c[1] + c[3] - 21, 62, 16};
    }

    private int[] applyRect() {
        int[] c = cardRect();
        return new int[]{c[0] + 6 + 66, c[1] + c[3] - 21, c[2] - 12 - 66, 16};
    }

    private int treeTop() {
        return bodyY + 20 + (showLore ? 22 : 0);
    }

    private int treeHeader() {
        return cardRect()[1] - 4 - treeTop() >= 100 ? 20 : 10;
    }

    private int treePitch() {
        return Mth.clamp((cardRect()[1] - 4 - treeTop() - treeHeader()) / 4, 16, 24);
    }

    /** Узел дерева выбранного сегмента: 0–2 — путь основы I–III, 3–5 — путь деталей, 6 — вершина. Сверху вниз. */
    private int[] nodeRect(int index) {
        int px = rx + 8;
        int pw = rw - 16;
        int y0 = treeTop() + treeHeader();
        int pitch = treePitch();
        if (index == 6) {
            return new int[]{px + pw / 2 - NODE_W / 2, y0 + 3 * pitch, NODE_W, NODE_H};
        }
        int column = index / 3;
        int cx = px + (column == 0 ? pw / 4 : pw * 3 / 4);
        return new int[]{cx - NODE_W / 2, y0 + (index % 3) * pitch, NODE_W, NODE_H};
    }

    private Node nodeAt(int index) {
        Segment s = GaussLook.SEGMENTS[segment];
        return index == 6 ? GaussUpgrades.apex(s) : new Node(s, index / 3, index % 3 + 1);
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    // ---- ввод ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (inside(backRect(), mouseX, mouseY)) {
            click();
            onClose();
            return true;
        }
        for (int i = 0; i < GaussLook.SEGMENTS.length; i++) {
            if (inside(segmentRect(i), mouseX, mouseY)) {
                segment = segment == i ? -1 : i;
                node = null;
                tryOn = false;
                click();
                return true;
            }
        }
        if (segment >= 0) {
            for (int i = 0; i < 7; i++) {
                if (inside(nodeRect(i), mouseX, mouseY)) {
                    node = nodeAt(i);
                    tryOn = false;
                    click();
                    return true;
                }
            }
        }
        if (node != null) {
            int current = look.getAsInt();
            State st = state(current);
            if (st != State.DONE && inside(tryRect(), mouseX, mouseY)) {
                tryOn = !tryOn;
                click();
                return true;
            }
            if (inside(applyRect(), mouseX, mouseY)) {
                if (st == State.AVAILABLE && canPay()) {
                    press.accept(node.button());
                    tryOn = false;
                    click();
                }
                return true;
            }
        }
        if (inside(previewRect(), mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            yaw += (float) dragX * 1.2f;
            pitch = Mth.clamp(pitch + (float) dragY * 0.8f, -70f, 70f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(previewRect(), mouseX, mouseY)) {
            userZoom = Mth.clamp(userZoom * (scrollY > 0 ? 1.15f : 1 / 1.15f), 0.6f, 2.5f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    // ---- отрисовка ----

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xC0050809);
        int current = look.getAsInt();
        drawFrame(g, current, mouseX, mouseY);
        drawPreview(g, current);
        drawSegments(g, current, mouseX, mouseY);
        drawMagazine(g);
        drawPanel(g, current, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (node != null) {
            List<ItemStack> items = GaussUpgrades.cost(node).items();
            int[] c = cardRect();
            int step = costStep(items.size(), c[2]);
            for (int i = 0; i < items.size(); i++) {
                if (inside(new int[]{c[0] + 6 + i * step, c[1] + c[3] - 42, step - 2, 18}, mouseX, mouseY)) {
                    g.renderTooltip(font, items.get(i), mouseX, mouseY);
                }
            }
        }
    }

    private static String upper(Component c) {
        return c.getString().toUpperCase(Locale.ROOT);
    }

    /** Окно и шапка: эмблема, название артефакта, общий прогресс, кнопка возврата к столу. */
    private void drawFrame(GuiGraphics g, int current, int mouseX, int mouseY) {
        g.fill(left, top, left + w, top + h, BG);
        g.renderOutline(left, top, w, h, BORDER_LIGHT);
        hex(g, left + 9, top + 9, 20, 18, GOLD, BG);
        hex(g, left + 14, top + 13, 10, 10, ACCENT, BG);
        g.fill(left + 18, top + 17, left + 20, top + 19, ACCENT);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(left + 36, top + 7, 0);
        pose.scale(1.5f, 1.5f, 1f);
        g.drawString(font, Component.translatable("adept_core.gauss_table.name"), 0, 0, TITLE, false);
        pose.popPose();
        int total = 0;
        for (Segment s : GaussLook.SEGMENTS) {
            total += done(current, s);
        }
        g.drawString(font, upper(Component.translatable("adept_core.gauss_table.subtitle", total, GaussLook.SEGMENTS.length * 7)),
                left + 36, top + 23, MUTED, false);
        int[] b = backRect();
        boolean hover = inside(b, mouseX, mouseY);
        g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], hover ? ACCENT_BG : PANEL);
        g.renderOutline(b[0], b[1], b[2], b[3], hover ? ACCENT : BORDER);
        g.drawCenteredString(font, Component.translatable("adept_core.gauss_table.back"), b[0] + b[2] / 2, b[1] + 4, hover ? TEXT : MUTED);
    }

    /** Шестигранник с вершинами слева и справа: кайма и заливка. */
    private static void hex(GuiGraphics g, int x, int y, int width, int height, int border, int fill) {
        float mid = (height - 1) / 2f;
        float slope = width / 4f / Math.max(1f, mid);
        for (int j = 0; j < height; j++) {
            int inset = (int) (Math.abs(j - mid) * slope);
            g.fill(x + inset, y + j, x + width - inset, y + j + 1, border);
            if (j > 0 && j < height - 1) {
                g.fill(x + inset + 1, y + j, x + width - inset - 1, y + j + 1, fill);
            }
        }
    }

    private ItemStack previewStack(int shown) {
        var tag = GaussGunItem.data(preview);
        if (tag.getInt(GaussLook.TAG) != shown || tag.getInt(GaussGunRenderer.FOCUS_TAG) != segment + 1) {
            tag.putInt(GaussLook.TAG, shown);
            tag.putInt(GaussGunRenderer.FOCUS_TAG, segment + 1);
            preview.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        return preview;
    }

    /** Проекция артефакта: сетка чертежа, голографическая площадка, модель, плашка фокуса. */
    private void drawPreview(GuiGraphics g, int current) {
        int[] r = previewRect();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], PANEL_DARK);
        for (int x = r[0] + 12; x < r[0] + r[2] - 1; x += 12) {
            g.fill(x, r[1] + 1, x + 1, r[1] + r[3] - 1, 0x103FE0D0);
        }
        for (int y = r[1] + 12; y < r[1] + r[3] - 1; y += 12) {
            g.fill(r[0] + 1, y, r[0] + r[2] - 1, y + 1, 0x103FE0D0);
        }
        g.renderOutline(r[0], r[1], r[2], r[3], BORDER);

        // Камера плавно идёт к выбранному сегменту.
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 1f : Math.min(1f, (now - lastFrame) / 220f);
        lastFrame = now;
        float[] target = segment >= 0 ? FOCUS[segment] : WHOLE;
        for (int i = 0; i < 4; i++) {
            camera[i] += (target[i] - camera[i]) * dt;
        }
        float seconds = (now % 3_600_000L) / 1000f;
        int cx = r[0] + r[2] / 2;
        int cy = r[1] + r[3] / 2 + 4;

        // Площадка: три кольца и золотые метки по кругу; гаснет, когда камера наезжает на сегмент.
        float whole = Mth.clamp(1f - (camera[3] - 1f) / 0.6f, 0f, 1f);
        if (whole > 0.05f) {
            int alpha = (int) (whole * 0x70);
            float a = r[2] * 0.36f;
            int py = r[1] + r[3] * 4 / 5;
            ring(g, cx, py, a, a * 0.2f, (alpha << 24) | 0x3FE0D0, 0f, 140);
            ring(g, cx, py, a * 0.86f, a * 0.172f, ((alpha / 2) << 24) | 0x3FE0D0, 0f, 120);
            ring(g, cx, py, a * 0.62f, a * 0.124f, ((alpha / 2) << 24) | 0x3FE0D0, 0f, 90);
            ring(g, cx, py, a * 0.93f, a * 0.186f, (alpha << 24) | 0xE2B95C, seconds * 0.1f, 16);
        }

        if (minecraft != null) {
            float zoom = r[2] * 0.185f * camera[3] * userZoom;
            float sway = dragging ? 0f : Mth.sin(seconds * 0.35f) * 12f;
            g.enableScissor(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(cx, cy + Mth.sin(seconds * 1.4f) * 1.2f, 250f);
            pose.scale(zoom, zoom, -zoom);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw + sway));
            // Начало модели в предмете — середина блока по X и Z, низ блока по Y.
            pose.translate(-camera[0] / 16f, -camera[1] / 16f - 0.01f, -camera[2] / 16f);
            Lighting.setupForEntityInInventory();
            minecraft.getItemRenderer().renderStatic(previewStack(shownLook(current)), ItemDisplayContext.NONE, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, pose, g.bufferSource(), minecraft.level, 0);
            g.flush();
            Lighting.setupFor3DItems();
            pose.popPose();
            g.disableScissor();
        }

        g.drawString(font, upper(Component.translatable("adept_core.gauss_table.projection")), r[0] + 6, r[1] + 5, MUTED, false);
        Component hint = Component.translatable("adept_core.gauss_table.hint");
        if (r[2] > 150 + font.width(hint)) {
            g.drawString(font, hint, r[0] + r[2] - 6 - font.width(hint), r[1] + 5, DIM, false);
        }
        // Плашка фокуса: что сейчас в кадре; при примерке — золотая.
        boolean trying = tryOn && node != null && state(current) != State.DONE;
        String chip;
        if (trying) {
            chip = upper(Component.translatable("adept_core.gauss_table.chip.try", stepName(node)));
        } else if (segment >= 0) {
            Segment s = GaussLook.SEGMENTS[segment];
            chip = upper(Component.translatable("adept_core.gauss_table.chip.focus",
                    Component.translatable("adept_core.gauss_table.segment." + s.id()), done(current, s), 7));
        } else {
            chip = upper(Component.translatable("adept_core.gauss_table.chip.whole"));
        }
        int color = trying ? GOLD : ACCENT;
        int chipW = font.width(chip) + 17;
        int chipY = r[1] + 17;
        g.fill(r[0] + 6, chipY, r[0] + 6 + chipW, chipY + 13, 0xD00A0F15);
        g.renderOutline(r[0] + 6, chipY, chipW, 13, color);
        g.fill(r[0] + 10, chipY + 5, r[0] + 13, chipY + 8, color);
        g.drawString(font, chip, r[0] + 17, chipY + 3, TEXT, false);
    }

    /** Эллипс точками: {@code count} отметок по кругу, {@code phase} — поворот в радианах. */
    private static void ring(GuiGraphics g, int cx, int cy, float a, float b, int color, float phase, int count) {
        for (int i = 0; i < count; i++) {
            float angle = phase + i * Mth.TWO_PI / count;
            int x = cx + Math.round(Mth.cos(angle) * a);
            int y = cy + Math.round(Mth.sin(angle) * b);
            g.fill(x, y, x + (count <= 16 ? 2 : 1), y + 1, color);
        }
    }

    /** Вкладки сегментов: название и семь отметок прогресса (три ступени основы, три ступени деталей, вершина). */
    private void drawSegments(GuiGraphics g, int current, int mouseX, int mouseY) {
        for (int i = 0; i < GaussLook.SEGMENTS.length; i++) {
            Segment s = GaussLook.SEGMENTS[i];
            int[] r = segmentRect(i);
            boolean selected = segment == i;
            boolean hover = inside(r, mouseX, mouseY);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], selected ? ACCENT_BG : PANEL);
            g.renderOutline(r[0], r[1], r[2], r[3], selected ? ACCENT : hover ? BORDER_LIGHT : BORDER);
            g.drawString(font, Component.translatable("adept_core.gauss_table.segment." + s.id()), r[0] + 5, r[1] + 2,
                    selected ? TEXT : hover ? TEXT : MUTED, false);
            int base = GaussLook.base(current, s);
            int add = GaussLook.add(current, s);
            for (int k = 0; k < 7; k++) {
                boolean on = k < 3 ? k < base : k < 6 ? k - 3 < add : GaussLook.apex(current, s);
                int x = r[0] + 5 + k * 6 + (k >= 3 ? 3 : 0) + (k == 6 ? 3 : 0);
                g.fill(x, r[1] + 12, x + 5, r[1] + 14, on ? (k == 6 ? GOLD : ACCENT) : BORDER_LIGHT);
            }
        }
    }

    /** Магазин пушки из инвентаря игрока: ячейка на иглу, цвет — материал иглы. */
    private void drawMagazine(GuiGraphics g) {
        int[] r = magazineRect();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], PANEL);
        g.renderOutline(r[0], r[1], r[2], r[3], BORDER);
        ItemStack gun = ItemStack.EMPTY;
        if (minecraft != null && minecraft.player != null) {
            var inventory = minecraft.player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).getItem() instanceof GaussGunItem) {
                    gun = inventory.getItem(i);
                    break;
                }
            }
        }
        g.drawString(font, upper(Component.translatable("adept_core.gauss_table.magazine")), r[0] + 6, r[1] + 5, TITLE, false);
        if (gun.isEmpty()) {
            Component none = Component.translatable("adept_core.gauss_table.magazine.none");
            g.drawString(font, none, r[0] + r[2] - 6 - font.width(none), r[1] + 5, DIM, false);
            return;
        }
        List<String> needles = GaussGunItem.magazine(gun);
        int capacity = Math.max(1, BalanceConfig.GAUSS_MAGAZINE.get());
        Component count = Component.translatable("adept_core.gauss_table.magazine.count", needles.size(), capacity);
        g.drawString(font, count, r[0] + r[2] - 6 - font.width(count), r[1] + 5, MUTED, false);
        int cell = Math.max(2, (r[2] - 12) / capacity);
        for (int i = 0; i < capacity; i++) {
            int x = r[0] + 6 + i * cell;
            int y = r[1] + 17;
            if (i < needles.size()) {
                g.fill(x, y, x + cell - 1, y + 12, PANEL_DARK);
                int mid = x + (cell - 1) / 2;
                g.fill(mid, y + 2, mid + Math.max(1, (cell - 1) / 3), y + 10, needleColor(needles.get(i)));
            } else {
                g.fill(x, y + 11, x + cell - 1, y + 12, BORDER_LIGHT);
            }
        }
    }

    private static int needleColor(String material) {
        return switch (material) {
            case "iron" -> 0xFFC9D6E3;
            case "steel" -> 0xFF8FD3FF;
            case "copper" -> 0xFFFF8A3D;
            default -> GOLD;
        };
    }

    /** Правая панель: название сегмента, отметки прогресса, пояснение, дерево, карточка улучшения. */
    private void drawPanel(GuiGraphics g, int current, int mouseX, int mouseY) {
        g.fill(rx, bodyY, rx + rw, bodyY + bodyH, PANEL);
        g.renderOutline(rx, bodyY, rw, bodyH, BORDER);
        int px = rx + 8;
        int pw = rw - 16;
        if (segment < 0) {
            g.drawString(font, upper(Component.translatable("adept_core.gauss_table.chip.whole")), px, bodyY + 7, TITLE, false);
            List<FormattedCharSequence> lines = font.split(Component.translatable("adept_core.gauss_table.choose"), pw);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), px, bodyY + 24 + i * 10, MUTED, false);
            }
            return;
        }
        Segment s = GaussLook.SEGMENTS[segment];
        g.drawString(font, upper(Component.translatable("adept_core.gauss_table.segment." + s.id())), px, bodyY + 7, TITLE, false);
        int progress = done(current, s);
        for (int k = 0; k < 7; k++) {
            int x = px + pw - 7 * 9 + k * 9;
            g.fill(x, bodyY + 9, x + 7, bodyY + 12, k < progress ? ACCENT : BORDER_LIGHT);
        }
        if (showLore) {
            List<FormattedCharSequence> lore = font.split(Component.translatable("adept_core.gauss_table.lore." + s.id()), pw);
            for (int i = 0; i < Math.min(2, lore.size()); i++) {
                g.drawString(font, lore.get(i), px, bodyY + 20 + i * 10, LORE, false);
            }
        }
        drawTree(g, current, s, mouseX, mouseY);
        drawCard(g, current, mouseX, mouseY);
    }

    /** Дерево сегмента: два пути сверху вниз (I → III) сходятся к вершине. */
    private void drawTree(GuiGraphics g, int current, Segment s, int mouseX, int mouseY) {
        int[] apex = nodeRect(6);
        boolean apexDone = GaussLook.apex(current, s);
        for (int column = 0; column < 2; column++) {
            int steps = GaussLook.step(current, s, column);
            int[] first = nodeRect(column * 3);
            int cx = first[0] + NODE_W / 2;
            Component name = Component.translatable("adept_core.gauss_table.path." + s.id() + "." + s.path(column));
            int headerY = treeTop();
            g.drawCenteredString(font, name, cx, headerY, TEXT);
            if (treeHeader() >= 20) {
                g.drawCenteredString(font, Component.translatable("adept_core.gauss_table.kind." + (column == 0 ? "base" : "add")),
                        cx, headerY + 10, MUTED);
            }
            for (int step = 0; step < 2; step++) {
                int[] from = nodeRect(column * 3 + step);
                int[] to = nodeRect(column * 3 + step + 1);
                g.fill(cx - 1, from[1] + NODE_H, cx + 1, to[1], steps >= step + 2 ? ACCENT : BORDER_LIGHT);
            }
            // От ступени III вниз и вбок к вершине.
            int[] third = nodeRect(column * 3 + 2);
            int line = steps >= 3 && apexDone ? ACCENT : steps >= 3 ? GOLD : BORDER_LIGHT;
            int ay = apex[1] + NODE_H / 2;
            g.fill(cx - 1, third[1] + NODE_H, cx + 1, ay + 1, line);
            if (column == 0) {
                g.fill(cx + 1, ay - 1, apex[0], ay + 1, line);
            } else {
                g.fill(apex[0] + NODE_W, ay - 1, cx - 1, ay + 1, line);
            }
        }
        for (int i = 0; i < 7; i++) {
            drawNode(g, current, i, mouseX, mouseY);
        }
    }

    private void drawNode(GuiGraphics g, int current, int index, int mouseX, int mouseY) {
        int[] r = nodeRect(index);
        Node n = nodeAt(index);
        State st = GaussUpgrades.state(current, n);
        boolean selected = n.equals(node);
        boolean hover = inside(r, mouseX, mouseY);
        if (selected || hover) {
            hex(g, r[0] - 3, r[1] - 2, NODE_W + 6, NODE_H + 4, selected ? TEXT : DIM, PANEL);
        }
        boolean pulse = st == State.AVAILABLE && (Util.getMillis() / 500) % 2 == 0;
        int border = st == State.DONE ? ACCENT : st == State.AVAILABLE ? (pulse ? TITLE : GOLD) : BORDER_LIGHT;
        int fill = st == State.DONE ? ACCENT : st == State.AVAILABLE ? GOLD_BG : LOCK_BG;
        hex(g, r[0], r[1], NODE_W, NODE_H, border, fill);
        int cx = r[0] + NODE_W / 2;
        int cy = r[1] + NODE_H / 2;
        if (st == State.LOCKED) {
            // Замок.
            g.fill(cx - 2, cy - 4, cx + 2, cy - 3, DIM);
            g.fill(cx - 2, cy - 3, cx - 1, cy - 1, DIM);
            g.fill(cx + 1, cy - 3, cx + 2, cy - 1, DIM);
            g.fill(cx - 3, cy - 1, cx + 3, cy + 3, DIM);
            return;
        }
        int color = st == State.DONE ? INK : GOLD;
        if (n.isApex()) {
            // Вершина — ромб.
            for (int d = 0; d < 4; d++) {
                g.fill(cx - d - 1, cy - 4 + d, cx + d + 1, cy - 3 + d, color);
                g.fill(cx - d - 1, cy + 3 - d, cx + d + 1, cy + 4 - d, color);
            }
        } else {
            String roman = n.step() == 1 ? "I" : n.step() == 2 ? "II" : "III";
            g.drawString(font, roman, cx - font.width(roman) / 2, r[1] + 3, color, false);
        }
    }

    private static MutableComponent stepName(Node n) {
        return StepTexts.find(n.key(), n.step()).map(StepTexts.Step::name)
                .orElseGet(() -> Component.translatable(langBase(n) + ".name"));
    }

    private static MutableComponent stepDescription(Node n) {
        return StepTexts.find(n.key(), n.step()).map(StepTexts.Step::description)
                .orElseGet(() -> Component.translatable(langBase(n) + ".desc"));
    }

    private static String langBase(Node n) {
        return "adept_core.step." + n.key().replace('/', '.') + "." + n.step();
    }

    private static int costStep(int count, int cardWidth) {
        return Math.min(40, (cardWidth - 12) / Math.max(1, count));
    }

    /** Карточка улучшения: состояние, название, описание, цена «есть / нужно», кнопки «Примерить» и «Вплести». */
    private void drawCard(GuiGraphics g, int current, int mouseX, int mouseY) {
        int[] c = cardRect();
        g.fill(c[0], c[1], c[0] + c[2], c[1] + c[3], CARD_BG);
        g.renderOutline(c[0], c[1], c[2], c[3], CARD_BORDER);
        int x = c[0] + 6;
        int inner = c[2] - 12;
        if (node == null) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("adept_core.gauss_table.details.node"), inner);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x, c[1] + 6 + i * 10, MUTED, false);
            }
            return;
        }
        State st = state(current);
        g.drawString(font, upper(Component.translatable("adept_core.gauss_table.card." + st.name().toLowerCase(Locale.ROOT))),
                x, c[1] + 5, LORE, false);
        if (!TableActions.GAUSS_TEMPO.equals(node.key()) || node.step() != 1) {
            Component pending = Component.translatable("adept_core.gauss_table.pending");
            if (font.width(pending) < inner - 110) {
                g.drawString(font, pending, x + inner - font.width(pending), c[1] + 5, DIM, false);
            }
        }
        g.drawString(font, stepName(node), x, c[1] + 16, TITLE, false);
        List<FormattedCharSequence> lines = font.split(stepDescription(node), inner);
        for (int i = 0; i < Math.min(descLines, lines.size()); i++) {
            g.drawString(font, lines.get(i), x, c[1] + 27 + i * 9, TEXT, false);
        }

        // Цена: значок и «есть / нужно» справа от него.
        List<ItemStack> items = GaussUpgrades.cost(node).items();
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        int step = costStep(items.size(), c[2]);
        int cy = c[1] + c[3] - 42;
        for (int i = 0; i < items.size(); i++) {
            ItemStack need = items.get(i);
            int ix = x + i * step;
            g.renderItem(need, ix, cy + 1);
            int have = minecraft == null || minecraft.player == null ? 0 : minecraft.player.getInventory().countItem(need.getItem());
            boolean enough = creative || st == State.DONE || have >= need.getCount();
            String text = st == State.DONE ? "×" + need.getCount() : Math.min(have, 99) + "/" + need.getCount();
            g.drawString(font, text, ix + 17, cy + 6, enough ? ACCENT : BAD, false);
        }

        // Кнопки.
        int[] a = applyRect();
        if (st == State.DONE) {
            g.fill(x, a[1], x + inner, a[1] + a[3], PANEL);
            g.renderOutline(x, a[1], inner, a[3], BORDER_LIGHT);
            g.drawCenteredString(font, Component.translatable("adept_core.gauss_table.action.done"), x + inner / 2, a[1] + 4, MUTED);
            return;
        }
        int[] t = tryRect();
        boolean tryHover = inside(t, mouseX, mouseY);
        g.fill(t[0], t[1], t[0] + t[2], t[1] + t[3], tryOn ? 0xFF3A2E12 : tryHover ? GOLD_BG : CARD_BG);
        g.renderOutline(t[0], t[1], t[2], t[3], GOLD);
        g.drawCenteredString(font, Component.translatable("adept_core.gauss_table.action." + (tryOn ? "try_off" : "try")),
                t[0] + t[2] / 2, t[1] + 4, TITLE);
        boolean pay = canPay();
        boolean active = st == State.AVAILABLE && pay;
        String key = st == State.LOCKED ? (node.isApex() ? "need_path" : "need_previous")
                : pay ? (node.isApex() ? "open_apex" : "open") : "not_enough";
        Component label = Component.translatable("adept_core.gauss_table.action." + key);
        if (active) {
            g.fill(a[0], a[1], a[0] + a[2], a[1] + a[3], inside(a, mouseX, mouseY) ? TITLE : GOLD);
            g.drawString(font, label, a[0] + a[2] / 2 - font.width(label) / 2, a[1] + 4, 0xFF17140D, false);
        } else {
            g.fill(a[0], a[1], a[0] + a[2], a[1] + a[3], PANEL);
            g.renderOutline(a[0], a[1], a[2], a[3], BORDER_LIGHT);
            g.drawCenteredString(font, label, a[0] + a[2] / 2, a[1] + 4, MUTED);
        }
    }
}
