package ru.adept.core.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import ru.adept.core.registry.ModItems;
import ru.adept.core.table.CostList;
import ru.adept.core.table.GaussUpgrades;
import ru.adept.core.table.GaussUpgrades.Node;
import ru.adept.core.table.GaussUpgrades.State;
import ru.adept.core.table.TableActions;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Прокачка гаусс-пушки на столе артефактов («Артефакты», «Интерфейсы»): слева 3D-превью, которое можно вращать;
 * при выборе сегмента камера наезжает на него, остальные части темнеют; справа два пути рунными линиями
 * и вершина; внизу описание выбранной ступени, цена в компонентах и кнопка.
 *
 * Правила интерфейса (навык ui-ux-pro-max): состояние узла видно не только цветом (галочка, замок, римская цифра);
 * у недоступной кнопки причина написана на самой кнопке; описание и цена — в окне, а не только в подсказке;
 * у всего кликабельного есть отклик на наведение. Палитра — «Интерфейсы»: тёмный металл, латунь, мана синяя,
 * электричество красное, разряды голубые. Рамка меняется с прокачкой: руны по углам сменяются заклёпками.
 *
 * Экран не знает о меню стола: облик и нажатия приходят через {@code look} и {@code press},
 * поэтому тот же экран показывается в сценах автотеста.
 */
public class GaussUpgradeScreen extends Screen {
    private static final int W = 400;
    private static final int H = 240;
    private static final int BG = 0xF01A1720;
    private static final int PANEL = 0xFF100E15;
    private static final int METAL = 0xFF353740;
    private static final int METAL_LIGHT = 0xFF555964;
    private static final int BRASS = 0xFFB08D57;
    private static final int BRASS_LIGHT = 0xFFDCA850;
    private static final int BRASS_DIM = 0x80B08D57;
    private static final int MANA = 0xFF3D6BFF;
    private static final int MANA_LIGHT = 0xFF7CC4FF;
    private static final int LINE_OFF = 0xFF3A3F55;
    private static final int RED = 0xFFF03A2C;
    private static final int GOOD = 0xFF60D060;
    private static final int BAD = 0xFFFF7060;
    private static final int TEXT = 0xFFE8E0D0;
    private static final int MUTED = 0xFF9A9080;
    private static final int GOLD = 0xFFF6D488;

    /** Центр и размер сегмента в модели (единицы модели): на них наезжает камера. Порядок — {@link GaussLook#SEGMENTS}. */
    private static final float[][] FOCUS = {
            {0, 19, -33, 2.3f}, {0, 20, -14, 2.6f}, {0, 11, -14, 2.6f},
            {0, 15, 21, 2.4f}, {0, 9, -1.5f, 2.6f}, {0, 28, 3, 2.4f}};
    private static final float[] WHOLE = {0, 18, -9, 1.0f};
    /** Пикселей интерфейса на блок, когда оружие видно целиком. */
    private static final float BASE_ZOOM = 38f;

    private final Screen parent;
    private final IntSupplier look;
    private final IntConsumer press;
    private final ItemStack preview = new ItemStack(ModItems.GAUSS_GUN.get());

    private int left;
    private int top;
    /** Выбранный сегмент: -1 — оружие целиком. */
    private int segment = -1;
    private Node node;
    private float yaw = 118f;
    private float pitch = 14f;
    private float userZoom = 1f;
    private final float[] camera = WHOLE.clone();
    private long lastFrame;
    private boolean dragging;
    private Button action;

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

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("adept_core.gauss_table.back"), b -> onClose())
                .bounds(left + W - 70, top + 4, 62, 14).build());
        action = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (node != null) {
                press.accept(node.button());
            }
        }).bounds(left + 262, top + 212, 130, 18).build());
        refreshAction();
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

    @Override
    public void tick() {
        refreshAction();
    }

    /** Подпись и доступность кнопки: у недоступной — причина прямо на ней. */
    private void refreshAction() {
        if (action == null) {
            return;
        }
        if (node == null) {
            action.visible = false;
            return;
        }
        action.visible = true;
        State state = GaussUpgrades.state(look.getAsInt(), node);
        boolean canPay = minecraft != null && minecraft.player != null && GaussUpgrades.cost(node).canPay(minecraft.player);
        String key;
        if (state == State.DONE) {
            key = "done";
        } else if (state == State.LOCKED) {
            key = node.isApex() ? "need_path" : "need_previous";
        } else {
            key = canPay ? (node.isApex() ? "open_apex" : "open") : "not_enough";
        }
        action.setMessage(Component.translatable("adept_core.gauss_table.action." + key));
        action.active = state == State.AVAILABLE && canPay;
    }

    // ---- раскладка ----

    private int[] previewRect() {
        return new int[]{left + 8, top + 22, 206, 112};
    }

    private int[] segmentRect(int i) {
        return new int[]{left + 8 + (i % 3) * 69, top + 138 + (i / 3) * 17, 68, 16};
    }

    /** Узел дерева выбранного сегмента: 0–2 — путь основы I–III, 3–5 — путь деталей, 6 — вершина. */
    private int[] nodeRect(int index) {
        int x0 = left + 220;
        int y0 = top + 22;
        if (index == 6) {
            return new int[]{x0 + 76, y0 + 16, 20, 20};
        }
        int column = index / 3;
        int step = index % 3;
        return new int[]{x0 + (column == 0 ? 30 : 122), y0 + 98 - step * 26, 20, 20};
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
        for (int i = 0; i < GaussLook.SEGMENTS.length; i++) {
            if (inside(segmentRect(i), mouseX, mouseY)) {
                segment = segment == i ? -1 : i;
                node = null;
                click();
                refreshAction();
                return true;
            }
        }
        if (segment >= 0) {
            for (int i = 0; i < 7; i++) {
                if (inside(nodeRect(i), mouseX, mouseY)) {
                    node = nodeAt(i);
                    click();
                    refreshAction();
                    return true;
                }
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
        g.fill(0, 0, width, height, 0xA0101014);
        int current = look.getAsInt();
        drawFrame(g, current);
        drawPreview(g);
        drawSegments(g, current, mouseX, mouseY);
        drawTree(g, current, mouseX, mouseY);
        drawDetails(g, current);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (node != null) {
            List<ItemStack> items = GaussUpgrades.cost(node).items();
            for (int i = 0; i < items.size(); i++) {
                if (inside(new int[]{left + 262 + i * 32, top + 178, 30, 30}, mouseX, mouseY)) {
                    g.renderTooltip(font, items.get(i), mouseX, mouseY);
                }
            }
        }
    }

    /** Рамка окна: тёмный металл и латунь; по углам руны (магия) сменяются заклёпками (техника) по мере прокачки. */
    private void drawFrame(GuiGraphics g, int current) {
        g.fill(left, top, left + W, top + H, BG);
        g.renderOutline(left, top, W, H, BRASS);
        g.renderOutline(left + 2, top + 2, W - 4, H - 4, BRASS_DIM);
        double progress = GaussLook.progress(current);
        int[][] corners = {{left + 5, top + 5}, {left + W - 10, top + H - 10}, {left + 5, top + H - 10}};
        for (int i = 0; i < corners.length; i++) {
            int x = corners[i][0];
            int y = corners[i][1];
            if (progress * corners.length > i) {
                g.fill(x, y, x + 5, y + 5, BRASS);
                g.fill(x + 1, y + 1, x + 3, y + 3, BRASS_LIGHT);
            } else {
                g.fill(x + 2, y, x + 3, y + 5, MANA_LIGHT);
                g.fill(x, y + 2, x + 5, y + 3, MANA_LIGHT);
                g.fill(x + 1, y + 1, x + 4, y + 4, MANA);
            }
        }
        g.drawString(font, title, left + 16, top + 7, TEXT, false);
        // Полоса под заголовком: синяя доля — магия, латунная — техника.
        int x1 = left + 8;
        int x2 = left + W - 8;
        int split = x2 - (int) Math.round((x2 - x1) * progress);
        g.fill(x1, top + 19, split, top + 20, MANA);
        g.fill(split, top + 19, x2, top + 20, BRASS);
    }

    private ItemStack previewStack(int current) {
        var tag = GaussGunItem.data(preview);
        if (tag.getInt(GaussLook.TAG) != current || tag.getInt(GaussGunRenderer.FOCUS_TAG) != segment + 1) {
            tag.putInt(GaussLook.TAG, current);
            tag.putInt(GaussGunRenderer.FOCUS_TAG, segment + 1);
            preview.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        return preview;
    }

    private void drawPreview(GuiGraphics g) {
        int[] r = previewRect();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], PANEL);
        // Разметка чертежа: редкая сетка.
        for (int x = r[0] + 13; x < r[0] + r[2]; x += 20) {
            g.fill(x, r[1] + 1, x + 1, r[1] + r[3] - 1, 0x14B08D57);
        }
        for (int y = r[1] + 16; y < r[1] + r[3]; y += 20) {
            g.fill(r[0] + 1, y, r[0] + r[2] - 1, y + 1, 0x14B08D57);
        }
        g.renderOutline(r[0], r[1], r[2], r[3], METAL_LIGHT);

        // Камера плавно идёт к выбранному сегменту.
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 1f : Math.min(1f, (now - lastFrame) / 220f);
        lastFrame = now;
        float[] target = segment >= 0 ? FOCUS[segment] : WHOLE;
        for (int i = 0; i < 4; i++) {
            camera[i] += (target[i] - camera[i]) * dt;
        }
        if (minecraft == null) {
            return;
        }
        float zoom = BASE_ZOOM * camera[3] * userZoom;
        g.enableScissor(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(r[0] + r[2] / 2f, r[1] + r[3] / 2f, 250f);
        pose.scale(zoom, zoom, -zoom);
        pose.mulPose(Axis.ZP.rotationDegrees(180f));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        // Начало модели в предмете — середина блока по X и Z, низ блока по Y.
        pose.translate(-camera[0] / 16f, -camera[1] / 16f - 0.01f, -camera[2] / 16f);
        Lighting.setupForEntityInInventory();
        minecraft.getItemRenderer().renderStatic(previewStack(look.getAsInt()), ItemDisplayContext.NONE, 0xF000F0,
                OverlayTexture.NO_OVERLAY, pose, g.bufferSource(), minecraft.level, 0);
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
        g.disableScissor();
        g.drawString(font, Component.translatable("adept_core.gauss_table.hint"), r[0] + 4, r[1] + r[3] - 11, MUTED, false);
    }

    /** Кнопки сегментов: название и семь отметок прогресса (три ступени основы, три ступени деталей, вершина). */
    private void drawSegments(GuiGraphics g, int current, int mouseX, int mouseY) {
        for (int i = 0; i < GaussLook.SEGMENTS.length; i++) {
            Segment s = GaussLook.SEGMENTS[i];
            int[] r = segmentRect(i);
            boolean selected = segment == i;
            boolean hover = inside(r, mouseX, mouseY);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], selected ? 0xFF3A3020 : hover ? METAL_LIGHT : METAL);
            g.renderOutline(r[0], r[1], r[2], r[3], selected ? BRASS_LIGHT : hover ? BRASS : 0xFF1E1F26);
            if (selected) {
                g.fill(r[0] + 1, r[1] + 1, r[0] + 3, r[1] + r[3] - 1, BRASS_LIGHT);
            }
            g.drawString(font, Component.translatable("adept_core.gauss_table.segment." + s.id()), r[0] + 5, r[1] + 2,
                    selected ? GOLD : TEXT, false);
            int base = GaussLook.base(current, s);
            int add = GaussLook.add(current, s);
            for (int k = 0; k < 7; k++) {
                boolean on = k < 3 ? k < base : k < 6 ? k - 3 < add : GaussLook.apex(current, s);
                int x = r[0] + 5 + k * 5 + (k >= 3 ? 3 : 0) + (k == 6 ? 3 : 0);
                g.fill(x, r[1] + 12, x + 4, r[1] + 14, on ? (k == 6 ? BRASS_LIGHT : MANA_LIGHT) : 0xFF1E1F26);
            }
        }
    }

    /** Дерево выбранного сегмента: два пути снизу вверх рунными линиями сходятся к вершине. */
    private void drawTree(GuiGraphics g, int current, int mouseX, int mouseY) {
        int x0 = left + 220;
        int y0 = top + 22;
        g.fill(x0, y0, x0 + 172, y0 + 148, PANEL);
        g.renderOutline(x0, y0, 172, 148, METAL_LIGHT);
        if (segment < 0) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("adept_core.gauss_table.choose"), 150);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x0 + 11, y0 + 52 + i * 10, MUTED, false);
            }
            return;
        }
        Segment s = GaussLook.SEGMENTS[segment];
        g.drawString(font, Component.translatable("adept_core.gauss_table.segment." + s.id()), x0 + 6, y0 + 5, GOLD, false);
        for (int column = 0; column < 2; column++) {
            int done = GaussLook.step(current, s, column);
            int cx = nodeRect(column * 3)[0] + 9;
            for (int step = 0; step < 2; step++) {
                int[] from = nodeRect(column * 3 + step);
                runeLine(g, cx, from[1] - 6, cx + 2, from[1], done >= step + 2);
            }
            // От ступени III вверх и вбок к вершине.
            int[] third = nodeRect(column * 3 + 2);
            int[] apex = nodeRect(6);
            boolean lit = done >= 3 && GaussLook.apex(current, s);
            runeLine(g, cx, apex[1] + 9, cx + 2, third[1], lit);
            if (column == 0) {
                runeLine(g, cx, apex[1] + 9, apex[0], apex[1] + 11, lit);
            } else {
                runeLine(g, apex[0] + apex[2], apex[1] + 9, cx + 2, apex[1] + 11, lit);
            }
            int[] first = nodeRect(column * 3);
            Component name = Component.translatable("adept_core.gauss_table.path." + s.id() + "." + s.path(column));
            g.drawCenteredString(font, name, first[0] + 10, first[1] + 24, TEXT);
            g.drawCenteredString(font, Component.translatable("adept_core.gauss_table.kind." + (column == 0 ? "base" : "add")),
                    first[0] + 10, first[1] + 35, MUTED);
        }
        for (int i = 0; i < 7; i++) {
            drawNode(g, current, i, mouseX, mouseY);
        }
    }

    /** Рунная линия: полоса с насечками. Горит синим, когда связь открыта. */
    private void runeLine(GuiGraphics g, int x1, int y1, int x2, int y2, boolean lit) {
        g.fill(x1, y1, x2, y2, lit ? MANA : LINE_OFF);
        boolean vertical = y2 - y1 > x2 - x1;
        if (vertical) {
            for (int y = y1 + 2; y < y2 - 1; y += 4) {
                g.fill(x1 - 1, y, x2 + 1, y + 1, lit ? MANA_LIGHT : LINE_OFF);
            }
        } else {
            for (int x = x1 + 2; x < x2 - 1; x += 4) {
                g.fill(x, y1 - 1, x + 1, y2 + 1, lit ? MANA_LIGHT : LINE_OFF);
            }
        }
    }

    private void drawNode(GuiGraphics g, int current, int index, int mouseX, int mouseY) {
        int[] r = nodeRect(index);
        Node n = nodeAt(index);
        State state = GaussUpgrades.state(current, n);
        boolean selected = n.equals(node);
        boolean hover = inside(r, mouseX, mouseY);
        int fill = state == State.DONE ? 0xFF1F3A8A : state == State.AVAILABLE ? 0xFF3A3020 : 0xFF1E1F26;
        int border = state == State.DONE ? MANA_LIGHT : state == State.AVAILABLE ? BRASS_LIGHT : METAL_LIGHT;
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
        g.renderOutline(r[0], r[1], r[2], r[3], border);
        if (state == State.AVAILABLE && (Util.getMillis() / 500) % 2 == 0) {
            g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, BRASS_DIM);
        }
        if (selected || hover) {
            g.renderOutline(r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, selected ? TEXT : MUTED);
        }
        int color = state == State.DONE ? TEXT : state == State.AVAILABLE ? GOLD : MUTED;
        if (n.isApex()) {
            // Вершина — ромб.
            int cx = r[0] + 10;
            int cy = r[1] + 10;
            for (int d = 0; d < 5; d++) {
                g.fill(cx - d, cy - 4 + d, cx + d, cy - 3 + d, color);
                g.fill(cx - d, cy + 4 - d, cx + d, cy + 5 - d, color);
            }
        } else {
            String roman = n.step() == 1 ? "I" : n.step() == 2 ? "II" : "III";
            g.drawString(font, roman, r[0] + 10 - font.width(roman) / 2, r[1] + 6, color, false);
        }
        if (state == State.DONE) {
            // Галочка в углу.
            int x = r[0] + r[2] - 7;
            int y = r[1] + r[3] - 6;
            g.fill(x, y + 2, x + 1, y + 3, GOOD);
            g.fill(x + 1, y + 3, x + 2, y + 4, GOOD);
            g.fill(x + 2, y + 2, x + 3, y + 3, GOOD);
            g.fill(x + 3, y + 1, x + 4, y + 2, GOOD);
            g.fill(x + 4, y, x + 5, y + 1, GOOD);
        } else if (state == State.LOCKED) {
            // Замок в углу.
            int x = r[0] + r[2] - 7;
            int y = r[1] + r[3] - 8;
            g.fill(x + 1, y, x + 4, y + 1, MUTED);
            g.fill(x + 1, y + 1, x + 2, y + 3, MUTED);
            g.fill(x + 3, y + 1, x + 4, y + 3, MUTED);
            g.fill(x, y + 3, x + 5, y + 6, MUTED);
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

    /** Нижняя полоса: название и состояние ступени, описание, цена значками с «есть / нужно». */
    private void drawDetails(GuiGraphics g, int current) {
        int x0 = left + 8;
        int y0 = top + 174;
        g.fill(x0, y0, left + W - 8, top + H - 6, PANEL);
        g.renderOutline(x0, y0, W - 16, H - 6 - 174, METAL_LIGHT);
        if (node == null) {
            g.drawString(font, Component.translatable(segment < 0 ? "adept_core.gauss_table.details.segment"
                    : "adept_core.gauss_table.details.node"), x0 + 6, y0 + 6, MUTED, false);
            return;
        }
        State state = GaussUpgrades.state(current, node);
        g.drawString(font, stepName(node), x0 + 6, y0 + 5, GOLD, false);
        Component status = Component.translatable("adept_core.gauss_table.state." + state.name().toLowerCase(java.util.Locale.ROOT));
        g.drawString(font, status, x0 + 246 - font.width(status), y0 + 5,
                state == State.DONE ? GOOD : state == State.AVAILABLE ? GOLD : MUTED, false);
        List<FormattedCharSequence> lines = font.split(stepDescription(node), 240);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            g.drawString(font, lines.get(i), x0 + 6, y0 + 16 + i * 9, TEXT, false);
        }
        if (!TableActions.GAUSS_TEMPO.equals(node.key()) || node.step() != 1) {
            g.drawString(font, Component.translatable("adept_core.gauss_table.pending"), x0 + 6, top + H - 17, MUTED, false);
        }
        // Цена: значок, под ним «есть / нужно» и галочка или крестик.
        CostList cost = GaussUpgrades.cost(node);
        List<ItemStack> items = cost.items();
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        for (int i = 0; i < items.size(); i++) {
            ItemStack need = items.get(i);
            int x = left + 262 + i * 32;
            int y = top + 178;
            g.fill(x, y, x + 30, y + 30, 0xFF1E1F26);
            g.renderOutline(x, y, 30, 30, METAL_LIGHT);
            g.renderItem(need, x + 7, y + 2);
            int have = minecraft == null || minecraft.player == null ? 0 : minecraft.player.getInventory().countItem(need.getItem());
            boolean enough = creative || have >= need.getCount() || state == State.DONE;
            String text = state == State.DONE ? "×" + need.getCount() : Math.min(have, 99) + "/" + need.getCount();
            g.drawString(font, text, x + 15 - font.width(text) / 2, y + 20, enough ? GOOD : BAD, false);
            if (state != State.DONE && !enough) {
                g.fill(x + 24, y + 2, x + 28, y + 3, RED);
                g.fill(x + 25, y + 3, x + 27, y + 5, RED);
                g.fill(x + 24, y + 5, x + 28, y + 6, RED);
            }
        }
    }
}
