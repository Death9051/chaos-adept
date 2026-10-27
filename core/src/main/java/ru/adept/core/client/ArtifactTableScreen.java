package ru.adept.core.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import ru.adept.core.config.BalanceConfig;
import ru.adept.core.table.ArtifactTableMenu;
import ru.adept.core.table.CostList;
import ru.adept.core.table.TableActions;

/**
 * Экран стола артефактов (срез 1, без 3D-превью): выбор пары, грубые компоненты,
 * прототип клинка и ступень «Кромка I». Цвета — палитра интерфейсов из «Артефактов».
 */
public class ArtifactTableScreen extends AbstractContainerScreen<ArtifactTableMenu> {
    private static final int BG = 0xF01A1720;
    private static final int FRAME = 0xFFB08D57;      // латунь
    private static final int MANA = 0xFF3D6BFF;       // мана — синий
    private static final int TEXT = 0xFFE8E0D0;
    private static final int MUTED = 0xFF9A9080;

    private int shownChoice = -1;
    private int shownBuilt = -1;
    private int shownEdge = -1;
    /** Выбранный, но ещё не подтверждённый вариант пары: -1 — нет. */
    private int pendingChoice = -1;
    /** Перестроить кнопки на следующем тике (не внутри обработчика нажатия). */
    private boolean dirty;

    public ArtifactTableScreen(ArtifactTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        shownChoice = menu.choice.get();
        shownBuilt = menu.bladeBuilt.get();
        shownEdge = menu.bladeEdge.get();
        int x = leftPos + 10;
        int y = topPos + 26;
        if (shownChoice == 0) {
            addRenderableWidget(Button.builder(Component.translatable("adept_core.table.blade"), b -> { pendingChoice = 1; dirty = true; })
                    .bounds(x, y + 40, 105, 20)
                    .tooltip(Tooltip.create(Component.translatable("adept_core.table.blade.desc"))).build());
            addRenderableWidget(Button.builder(Component.translatable("adept_core.table.hammer"), b -> { pendingChoice = 2; dirty = true; })
                    .bounds(x + 115, y + 40, 105, 20)
                    .tooltip(Tooltip.create(Component.translatable("adept_core.table.hammer.desc"))).build());
            if (pendingChoice > 0) {
                addRenderableWidget(Button.builder(Component.translatable("adept_core.table.confirm").withStyle(ChatFormatting.GOLD),
                        b -> {
                            press(pendingChoice == 1 ? ArtifactTableMenu.BTN_CHOOSE_BLADE : ArtifactTableMenu.BTN_CHOOSE_HAMMER);
                            pendingChoice = -1;
                            dirty = true;
                        }).bounds(x + 30, y + 100, 160, 20).build());
            }
            return;
        }
        // Грубые компоненты — всегда доступны после выбора пары.
        for (int i = 0; i < TableActions.CRUDE.size(); i++) {
            TableActions.Craft craft = TableActions.CRUDE.get(i);
            int id = ArtifactTableMenu.BTN_CRUDE_FIRST + i;
            Button button = Button.builder(new ItemStack(craft.output().get()).getHoverName(), b -> press(id))
                    .bounds(x + (i % 2) * 112, y + 12 + (i / 2) * 22, 108, 20)
                    .tooltip(Tooltip.create(costText(craft.costList()))).build();
            addRenderableWidget(button);
        }
        if (shownChoice == 1) {
            addRenderableWidget(Button.builder(Component.translatable(shownBuilt > 0
                            ? "adept_core.table.blade.rebuild" : "adept_core.table.blade.build"),
                            b -> press(ArtifactTableMenu.BTN_BLADE_PROTOTYPE))
                    .bounds(x, y + 76, 220, 20)
                    .tooltip(Tooltip.create(costText(CostList.parse(BalanceConfig.TABLE_BLADE_PROTOTYPE.get())))).build());
            Button edge = Button.builder(Component.translatable("adept_core.table.blade.edge1"),
                            b -> press(ArtifactTableMenu.BTN_BLADE_EDGE))
                    .bounds(x, y + 120, 220, 20)
                    .tooltip(Tooltip.create(costText(CostList.parse(BalanceConfig.TABLE_BLADE_EDGE_1.get())))).build();
            edge.active = shownBuilt > 0 && shownEdge == 0;
            addRenderableWidget(edge);
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private static Component costText(CostList cost) {
        MutableComponent text = Component.translatable("adept_core.table.cost").withStyle(ChatFormatting.GOLD);
        for (ItemStack stack : cost.items()) {
            text.append(Component.literal("\n" + stack.getCount() + " × ").withStyle(ChatFormatting.GRAY))
                    .append(stack.getHoverName());
        }
        return text;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (dirty || menu.choice.get() != shownChoice || menu.bladeBuilt.get() != shownBuilt || menu.bladeEdge.get() != shownEdge) {
            dirty = false;
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, FRAME);
        g.renderOutline(leftPos + 2, topPos + 2, imageWidth - 4, imageHeight - 4, 0x80B08D57);
        g.fill(leftPos + 10, topPos + 18, leftPos + imageWidth - 10, topPos + 19, MANA);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 7, TEXT, false);
        int choice = menu.choice.get();
        if (choice == 0) {
            g.drawWordWrap(font, Component.translatable("adept_core.table.choose.hint"), 10, 26, 220, MUTED);
            if (pendingChoice > 0) {
                g.drawWordWrap(font, Component.translatable("adept_core.table.choose.warning")
                        .withStyle(ChatFormatting.RED), 10, 92, 220, 0xFFFF6060);
            }
        } else {
            g.drawString(font, Component.translatable("adept_core.table.components"), 10, 26, MUTED, false);
            if (choice == 1) {
                g.drawString(font, Component.translatable("adept_core.table.segment.emitter"), 10, 136, MUTED, false);
                if (menu.bladeEdge.get() > 0) {
                    g.drawString(font, Component.translatable("adept_core.table.done"), 170, 136, 0xFF60D060, false);
                }
            } else {
                g.drawWordWrap(font, Component.translatable("adept_core.table.hammer.later"), 10, 106, 220, MUTED);
            }
        }
        int result = menu.lastResult.get();
        if (result > 1) {
            TableActions.Result r = TableActions.Result.values()[result - 1];
            g.drawString(font, Component.translatable("adept_core.table.result." + r.name().toLowerCase()), 10,
                    imageHeight - 12, 0xFFFF8060, false);
        }
    }
}
