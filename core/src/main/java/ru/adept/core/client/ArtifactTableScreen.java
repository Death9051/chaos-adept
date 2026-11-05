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
import ru.adept.core.artifact.StepTexts;
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

    /** Вкладки: 0 — ближний бой, 1 — броня, 2 — дальний бой. */
    private int tab;
    private int shownState = -1;
    /** Выбранный, но ещё не подтверждённый вариант пары: -1 — нет. */
    private int pendingChoice = -1;
    /** Перестроить кнопки на следующем тике (не внутри обработчика нажатия). */
    private boolean dirty;

    public ArtifactTableScreen(ArtifactTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 180;
    }

    /** Описание вкладки: выбор, собран ли прототип, ступень, кнопки и ключи текстов. */
    private record Tab(String first, String second, int choiceFirst, int choiceSecond, int prototype, int step,
                       String stepKey, String prototypeCost, String stepCost, String later) {
    }

    private Tab tabInfo() {
        return switch (tab) {
            case 1 -> new Tab("light", "heavy", ArtifactTableMenu.BTN_CHOOSE_LIGHT, ArtifactTableMenu.BTN_CHOOSE_HEAVY,
                    ArtifactTableMenu.BTN_HEAVY_PROTOTYPE, ArtifactTableMenu.BTN_HEAVY_BASTION, TableActions.HEAVY_BASTION,
                    BalanceConfig.TABLE_HEAVY_PROTOTYPE.get(), BalanceConfig.TABLE_HEAVY_BASTION_1.get(), "light");
            case 2 -> new Tab("gauss", "rail", ArtifactTableMenu.BTN_CHOOSE_GAUSS, ArtifactTableMenu.BTN_CHOOSE_RAIL,
                    ArtifactTableMenu.BTN_GAUSS_PROTOTYPE, ArtifactTableMenu.BTN_GAUSS_TEMPO, TableActions.GAUSS_TEMPO,
                    BalanceConfig.TABLE_GAUSS_PROTOTYPE.get(), BalanceConfig.TABLE_GAUSS_TEMPO_1.get(), "rail");
            default -> new Tab("blade", "hammer", ArtifactTableMenu.BTN_CHOOSE_BLADE, ArtifactTableMenu.BTN_CHOOSE_HAMMER,
                    ArtifactTableMenu.BTN_BLADE_PROTOTYPE, ArtifactTableMenu.BTN_BLADE_EDGE, TableActions.BLADE_EDGE,
                    BalanceConfig.TABLE_BLADE_PROTOTYPE.get(), BalanceConfig.TABLE_BLADE_EDGE_1.get(), "hammer");
        };
    }

    /** Выбор вкладки: 0 — нет, 1 — первый вариант (с прототипом в срезе), 2 — второй (позже). */
    private int choice() {
        return switch (tab) {
            case 1 -> menu.armorChoice.get() == 2 ? 1 : menu.armorChoice.get() == 1 ? 2 : 0;
            case 2 -> menu.rangedChoice.get();
            default -> menu.choice.get();
        };
    }

    private int built() {
        return switch (tab) {
            case 1 -> menu.heavyBuilt.get();
            case 2 -> menu.gaussBuilt.get();
            default -> menu.bladeBuilt.get();
        };
    }

    private int stepDone() {
        return switch (tab) {
            case 1 -> menu.heavyBastion.get();
            case 2 -> menu.gaussTempo.get();
            default -> menu.bladeEdge.get();
        };
    }

    private int state() {
        return tab * 1000 + choice() * 100 + built() * 10 + stepDone();
    }

    @Override
    protected void init() {
        super.init();
        shownState = state();
        String[] tabs = {"melee", "armor", "ranged"};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            Button t = Button.builder(Component.translatable("adept_core.table.tab." + tabs[i]), b -> {
                tab = index;
                pendingChoice = -1;
                dirty = true;
            }).bounds(leftPos + 96 + i * 46, topPos + 4, 44, 12).build();
            t.active = tab != i;
            addRenderableWidget(t);
        }
        Tab info = tabInfo();
        int x = leftPos + 10;
        int y = topPos + 26;
        // Броня: первый вариант на экране — тяжёлая (в срезе есть прототип), второй — лёгкая.
        String a = tab == 1 ? info.second() : info.first();
        String bKey = tab == 1 ? info.first() : info.second();
        int aBtn = tab == 1 ? info.choiceSecond() : info.choiceFirst();
        int bBtn = tab == 1 ? info.choiceFirst() : info.choiceSecond();
        if (choice() == 0) {
            addRenderableWidget(Button.builder(Component.translatable("adept_core.table." + a), b -> { pendingChoice = aBtn; dirty = true; })
                    .bounds(x, y + 40, 105, 20)
                    .tooltip(Tooltip.create(Component.translatable("adept_core.table." + a + ".desc"))).build());
            addRenderableWidget(Button.builder(Component.translatable("adept_core.table." + bKey), b -> { pendingChoice = bBtn; dirty = true; })
                    .bounds(x + 115, y + 40, 105, 20)
                    .tooltip(Tooltip.create(Component.translatable("adept_core.table." + bKey + ".desc"))).build());
            if (pendingChoice >= 0) {
                addRenderableWidget(Button.builder(Component.translatable("adept_core.table.confirm").withStyle(ChatFormatting.GOLD),
                        b -> {
                            press(pendingChoice);
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
        addRenderableWidget(Button.builder(Component.translatable("item.adept_core.resonator"),
                        b -> press(ArtifactTableMenu.BTN_RESONATOR))
                .bounds(x, y + 56, 72, 18)
                .tooltip(Tooltip.create(costText(CostList.parse(BalanceConfig.TABLE_RESONATOR.get())))).build());
        addRenderableWidget(Button.builder(Component.translatable("item.adept_core.key"),
                        b -> press(ArtifactTableMenu.BTN_KEY))
                .bounds(x + 148, y + 56, 72, 18)
                .tooltip(Tooltip.create(costText(CostList.parse(BalanceConfig.TABLE_KEY.get())))).build());
        addRenderableWidget(Button.builder(Component.translatable("item.adept_core.camp"),
                        b -> press(ArtifactTableMenu.BTN_CAMP))
                .bounds(x + 74, y + 56, 72, 18)
                .tooltip(Tooltip.create(costText(CostList.parse(BalanceConfig.TABLE_CAMP.get())))).build());
        if (choice() == 1) {
            String variant = tab == 1 ? "heavy" : info.first();
            addRenderableWidget(Button.builder(Component.translatable(built() > 0
                            ? "adept_core.table." + variant + ".rebuild" : "adept_core.table." + variant + ".build"),
                            b -> press(info.prototype()))
                    .bounds(x, y + 78, 220, 20)
                    .tooltip(Tooltip.create(costText(CostList.parse(info.prototypeCost())))).build());
            Button step = Button.builder(Component.translatable("adept_core.table.open",
                            StepTexts.find(info.stepKey(), 1).map(StepTexts.Step::name).orElse(Component.empty())),
                            b -> press(info.step()))
                    .bounds(x, y + 120, 220, 20)
                    .tooltip(Tooltip.create(StepTexts.find(info.stepKey(), 1)
                            .map(st -> StepTexts.tooltip(st).append(Component.literal("\n\n")))
                            .orElse(Component.empty())
                            .append(costText(CostList.parse(info.stepCost()))))).build();
            step.active = built() > 0 && stepDone() == 0;
            addRenderableWidget(step);
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
        if (dirty || state() != shownState) {
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
        Tab info = tabInfo();
        if (choice() == 0) {
            g.drawWordWrap(font, Component.translatable("adept_core.table.choose.hint." + tab), 10, 26, 220, MUTED);
            if (pendingChoice >= 0) {
                g.drawWordWrap(font, Component.translatable("adept_core.table.choose.warning")
                        .withStyle(ChatFormatting.RED), 10, 92, 220, 0xFFFF6060);
            }
        } else {
            g.drawString(font, Component.translatable("adept_core.table.components"), 10, 26, MUTED, false);
            if (choice() == 1) {
                MutableComponent label = Component.translatable("adept_core.table.segment." + tab);
                StepTexts.find(info.stepKey(), 1).ifPresent(step -> label.append(": ").append(step.name()));
                g.drawString(font, label, 10, 136, MUTED, false);
                if (stepDone() > 0) {
                    g.drawString(font, Component.translatable("adept_core.table.done"), 170, 136, 0xFF60D060, false);
                }
            } else {
                g.drawWordWrap(font, Component.translatable("adept_core.table." + info.later() + ".later"), 10, 106, 220, MUTED);
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
