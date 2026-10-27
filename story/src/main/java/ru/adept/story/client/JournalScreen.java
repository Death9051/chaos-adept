package ru.adept.story.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.adept.story.journal.JournalDef;
import ru.adept.story.net.EntryView;
import ru.adept.story.net.StoryNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Журнал героя: записи в хронологии с игровым днём и отметкой новых; отдельная вкладка —
 * условия торговца. Внешний вид временный (открытый вопрос «Сюжета»), текст и порядок — окончательные.
 */
public final class JournalScreen extends Screen {
    private static final int PANEL_COLOR = 0xE0161210;
    private static final int BORDER_COLOR = 0xFF5A4636;
    private static final int TEXT_COLOR = 0xE8E2D6;
    private static final int DAY_COLOR = StoryOverlay.CHAOS_COLOR;
    private static final int NEW_COLOR = 0x8FD3FF;
    private static final int LINE = 10;

    private enum Tab { ENTRIES, TRADER }

    private Tab tab = Tab.ENTRIES;
    private final int seenAtOpen;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int scroll;
    private List<FormattedCharSequence> rows = List.of();
    private List<Integer> rowColors = List.of();

    public JournalScreen() {
        super(Component.translatable("adept_story.journal.title"));
        seenAtOpen = JournalClient.seen();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(340, width - 20);
        panelHeight = height - 40;
        left = (width - panelWidth) / 2;
        top = 20;
        int tabWidth = 90;
        addRenderableWidget(Button.builder(Component.translatable("adept_story.journal.tab.entries"), b -> switchTab(Tab.ENTRIES))
                .bounds(left + 8, top + 6, tabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("adept_story.journal.tab.trader"), b -> switchTab(Tab.TRADER))
                .bounds(left + 12 + tabWidth, top + 6, tabWidth, 18).build());
        rebuild();
        scroll = maxScroll();
    }

    private void switchTab(Tab next) {
        tab = next;
        rebuild();
        scroll = maxScroll();
    }

    private void rebuild() {
        List<FormattedCharSequence> out = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int wrap = panelWidth - 24;
        List<EntryView> entries = JournalClient.entries();
        for (int i = 0; i < entries.size(); i++) {
            EntryView entry = entries.get(i);
            boolean traderPage = entry.page().equals(JournalDef.PAGE_TRADER);
            if (traderPage != (tab == Tab.TRADER)) {
                continue;
            }
            MutableComponent header = Component.translatable("adept_story.journal.day", entry.day());
            boolean fresh = i >= seenAtOpen;
            for (FormattedCharSequence row : font.split(header, wrap)) {
                out.add(row);
                colors.add(DAY_COLOR);
            }
            if (fresh) {
                out.add(Component.translatable("adept_story.journal.new").getVisualOrderText());
                colors.add(NEW_COLOR);
            }
            for (String key : entry.keys()) {
                for (FormattedCharSequence row : font.split(Component.translatable(key), wrap)) {
                    out.add(row);
                    colors.add(TEXT_COLOR);
                }
            }
            out.add(FormattedCharSequence.EMPTY);
            colors.add(TEXT_COLOR);
        }
        if (out.isEmpty()) {
            out.add(Component.translatable("adept_story.journal.empty").getVisualOrderText());
            colors.add(TEXT_COLOR);
        }
        rows = out;
        rowColors = colors;
    }

    private int visibleRows() {
        return Math.max(1, (panelHeight - 40) / LINE);
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, BORDER_COLOR);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int y = top + 32;
        int end = Math.min(rows.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            graphics.drawString(font, rows.get(i), left + 12, y, rowColors.get(i), false);
            y += LINE;
        }
        if (maxScroll() > 0) {
            // Полоса прокрутки справа.
            int trackTop = top + 32;
            int trackHeight = visibleRows() * LINE;
            int thumb = Math.max(10, trackHeight * visibleRows() / rows.size());
            int thumbY = trackTop + (trackHeight - thumb) * scroll / maxScroll();
            graphics.fill(left + panelWidth - 6, thumbY, left + panelWidth - 3, thumbY + thumb, BORDER_COLOR);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY) * 3));
        return true;
    }

    /** Та же клавиша, что открывает журнал, его и закрывает. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (StoryClient.JOURNAL_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        JournalClient.markSeen();
        PacketDistributor.sendToServer(new StoryNetwork.JournalSeen(JournalClient.entries().size()));
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
