package ru.adept.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.adept.core.camp.TransitionMenu;

/** Экран стационарного перехода: на базу или вызов торговца Альянса. */
public class TransitionScreen extends AbstractContainerScreen<TransitionMenu> {
    private Button call;

    public TransitionScreen(TransitionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 180;
        imageHeight = 84;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("adept_core.transition.base"), b -> press(TransitionMenu.BTN_BASE))
                .bounds(leftPos + 10, topPos + 24, 160, 20).build());
        call = addRenderableWidget(Button.builder(Component.translatable("adept_core.transition.call"), b -> press(TransitionMenu.BTN_CALL))
                .bounds(leftPos + 10, topPos + 50, 160, 20).build());
        call.active = menu.canCall.get() == 1;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (call != null) {
            call.active = menu.canCall.get() == 1;
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF01A1720);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFB08D57);
        g.fill(leftPos + 10, topPos + 18, leftPos + imageWidth - 10, topPos + 19, 0xFFF0AA6E);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 7, 0xFFE8E0D0, false);
    }
}
