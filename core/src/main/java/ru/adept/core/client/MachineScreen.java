package ru.adept.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import ru.adept.core.base.MachineMenu;

/**
 * Экран машин базы без текстур: рамка, ячейки слотов, шкалы запаса базы
 * (электричество — красная, мана — синяя), прогресс и бак плавильни.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int BG = 0xF01A1720;
    private static final int FRAME = 0xFFB08D57;
    private static final int SLOT = 0xFF2B2630;
    private static final int ELECTRICITY = 0xFFD8402E;
    private static final int MANA = 0xFF3D6BFF;
    private static final int PROGRESS = 0xFFF0AA6E;
    private static final int MOLTEN = 0xFFFF8A2A;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int cap = Math.max(1, menu.data(2));
        if (inside(mouseX, mouseY, 8, 18, 6, 50)) {
            graphics.renderTooltip(font, Component.translatable("adept_core.machine.electricity", menu.data(0), cap), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 16, 18, 6, 50)) {
            graphics.renderTooltip(font, Component.translatable("adept_core.machine.mana", menu.data(1), cap), mouseX, mouseY);
        } else if (menu.kind == MachineMenu.Kind.SMELTER && inside(mouseX, mouseY, 152, 18, 12, 50)) {
            graphics.renderTooltip(font, Component.translatable("adept_core.machine.tank", menu.data(4), menu.data(5)), mouseX, mouseY);
        }
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, FRAME);
        for (Slot slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT);
        }
        int cap = Math.max(1, menu.data(2));
        bar(g, 8, 18, 6, 50, menu.data(0), cap, ELECTRICITY);
        bar(g, 16, 18, 6, 50, menu.data(1), cap, MANA);
        int progress = menu.data(3);
        if (menu.kind == MachineMenu.Kind.GENERATOR) {
            bar(g, 100, 36, 6, 14, progress, 100, PROGRESS);
        } else {
            g.fill(leftPos + 78, topPos + 40, leftPos + 108, topPos + 44, SLOT);
            g.fill(leftPos + 78, topPos + 40, leftPos + 78 + 30 * progress / 100, topPos + 44, PROGRESS);
        }
        if (menu.kind == MachineMenu.Kind.SMELTER) {
            bar(g, 152, 18, 12, 50, menu.data(4), Math.max(1, menu.data(5)), MOLTEN);
        }
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h, int value, int max, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, SLOT);
        int filled = (int) ((long) h * Math.max(0, Math.min(value, max)) / max);
        g.fill(leftPos + x, topPos + y + h - filled, leftPos + x + w, topPos + y + h, color);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 28, 6, 0xFFE8E0D0, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9A9080, false);
    }
}
