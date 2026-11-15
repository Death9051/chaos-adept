package ru.adept.story.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.adept.story.AdeptStory;
import ru.adept.story.journal.JournalDef;
import ru.adept.story.net.EntryView;
import ru.adept.story.net.StoryNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Журнал героя: потёртая бумага, записи каллиграфическим почерком, пометки на полях другой рукой и красными
 * чернилами. Записи — не квесты: без целей и галочек. Вкладки «Записи» и «Торговец». Всё рисуется через
 * {@link GuiGraphics}, Iris экраны не трогает (core/RENDERING.md).
 */
public final class JournalScreen extends Screen {
    private static final ResourceLocation PAPER = ResourceLocation.fromNamespaceAndPath(AdeptStory.MOD_ID, "journal/paper");
    /** Почерк записей (Bad Script) и пометок (Caveat), запасной — ванильный шрифт. */
    public static final ResourceLocation HAND = ResourceLocation.fromNamespaceAndPath(AdeptStory.MOD_ID, "journal");
    public static final ResourceLocation NOTE_HAND = ResourceLocation.fromNamespaceAndPath(AdeptStory.MOD_ID, "journal_note");

    // Чернила на бумаге #E8D9B5: контраст основного текста ~13:1, дня ~6:1, пометок ~6.5:1.
    private static final int INK = 0xFF2B1D12;
    private static final int INK_DAY = 0xFF6B3A1E;
    private static final int INK_NOTE = 0xFF8A2B1A;
    private static final int INK_FADED = 0xFF6E5640;
    private static final int INK_RULE = 0x408A2B1A;
    private static final int INK_SCROLL = 0x806B3A1E;

    private static final int PAD = 22;
    private static final int LINE = 11;
    private static final int NOTE_LINE = 10;
    private static final int MARGIN = 86;
    private static final int TAB_Y = 14;
    private static final int CONTENT_Y = 36;

    private enum Tab { ENTRIES, TRADER }

    /** Строка на бумаге: смещение от начала содержимого в пикселях. */
    private record Row(FormattedCharSequence text, int x, int y, int color) {
    }

    private Tab tab = Tab.ENTRIES;
    private final int seenAtOpen;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int marginWidth;
    private int scroll;
    private int contentHeight;
    private List<Row> rows = List.of();

    public JournalScreen() {
        super(Component.translatable("adept_story.journal.title"));
        seenAtOpen = JournalClient.seen();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(380, width - 16);
        panelHeight = height - 24;
        left = (width - panelWidth) / 2;
        top = 12;
        // Поля под пометки — когда окно достаточно широкое; иначе пометка идёт под записью.
        marginWidth = panelWidth >= 300 ? MARGIN : 0;
        rebuild();
        scroll = maxScroll();
    }

    private static MutableComponent hand(Component text) {
        return text.copy().withStyle(s -> s.withFont(HAND));
    }

    private static MutableComponent noteHand(Component text) {
        return text.copy().withStyle(s -> s.withFont(NOTE_HAND));
    }

    private int textWidth() {
        return panelWidth - PAD * 2 - marginWidth;
    }

    private void rebuild() {
        List<Row> out = new ArrayList<>();
        int wrap = textWidth();
        int noteX = PAD + wrap + 8;
        int noteWrap = marginWidth - 10;
        int y = 0;
        int notesBottom = 0;
        List<EntryView> entries = JournalClient.entries();
        for (int i = 0; i < entries.size(); i++) {
            EntryView entry = entries.get(i);
            boolean traderPage = entry.page().equals(JournalDef.PAGE_TRADER);
            if (traderPage != (tab == Tab.TRADER)) {
                continue;
            }
            int entryTop = y;
            MutableComponent header = Component.translatable("adept_story.journal.day", entry.day());
            if (i >= seenAtOpen) {
                // «Новое» — словом, а не только цветом.
                header = header.append(Component.literal(" · ")).append(Component.translatable("adept_story.journal.new"));
            }
            for (FormattedCharSequence row : font.split(hand(header), wrap)) {
                out.add(new Row(row, PAD, y, INK_DAY));
                y += LINE;
            }
            for (String key : entry.keys()) {
                for (FormattedCharSequence row : font.split(hand(Component.translatable(key)), wrap)) {
                    out.add(new Row(row, PAD, y, INK));
                    y += LINE;
                }
                y += 3;
            }
            if (entry.hasNote()) {
                Component note = noteHand(Component.translatable(entry.note()));
                if (marginWidth > 0) {
                    int noteY = Math.max(entryTop + 2, notesBottom);
                    for (FormattedCharSequence row : font.split(note, noteWrap)) {
                        out.add(new Row(row, noteX, noteY, INK_NOTE));
                        noteY += NOTE_LINE;
                    }
                    notesBottom = noteY + 4;
                } else {
                    for (FormattedCharSequence row : font.split(note, wrap - 16)) {
                        out.add(new Row(row, PAD + 16, y, INK_NOTE));
                        y += NOTE_LINE;
                    }
                }
            }
            y += 8;
        }
        if (out.isEmpty()) {
            out.add(new Row(hand(Component.translatable("adept_story.journal.empty")).getVisualOrderText(), PAD, 0, INK_FADED));
            y = LINE;
        }
        rows = out;
        contentHeight = Math.max(y, notesBottom);
    }

    private int viewHeight() {
        return panelHeight - CONTENT_Y - 18;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - viewHeight());
    }

    // --- Вкладки: надпись почерком, активная темнее и подчёркнута, наведение — подчёркивание бледнее ---

    private Component tabLabel(Tab t) {
        return hand(Component.translatable(t == Tab.ENTRIES ? "adept_story.journal.tab.entries" : "adept_story.journal.tab.trader"));
    }

    private int tabX(Tab t) {
        return t == Tab.ENTRIES ? left + PAD : left + PAD + font.width(tabLabel(Tab.ENTRIES)) + 18;
    }

    private boolean overTab(Tab t, double mouseX, double mouseY) {
        int x = tabX(t);
        int y = top + TAB_Y;
        return mouseX >= x - 3 && mouseX <= x + font.width(tabLabel(t)) + 3 && mouseY >= y - 3 && mouseY <= y + 12;
    }

    private void renderTab(GuiGraphics graphics, Tab t, int mouseX, int mouseY) {
        int x = tabX(t);
        int y = top + TAB_Y;
        boolean active = tab == t;
        boolean hover = !active && overTab(t, mouseX, mouseY);
        int w = font.width(tabLabel(t));
        graphics.drawString(font, tabLabel(t), x, y, active ? INK : hover ? INK_DAY : INK_FADED, false);
        if (active) {
            graphics.fill(x, y + 11, x + w, y + 12, INK);
        } else if (hover) {
            graphics.fill(x, y + 11, x + w, y + 12, INK_SCROLL);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(PAPER, left, top, panelWidth, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTab(graphics, Tab.ENTRIES, mouseX, mouseY);
        renderTab(graphics, Tab.TRADER, mouseX, mouseY);

        int viewTop = top + CONTENT_Y;
        int viewBottom = viewTop + viewHeight();
        // Линейка под вкладками и красная линия полей, как в тетради.
        graphics.fill(left + PAD, viewTop - 6, left + panelWidth - PAD, viewTop - 5, INK_RULE);
        if (marginWidth > 0) {
            int x = left + panelWidth - PAD - marginWidth + 3;
            graphics.fill(x, viewTop - 5, x + 1, viewBottom + 4, INK_RULE);
        }

        graphics.enableScissor(left, viewTop, left + panelWidth, viewBottom);
        for (Row row : rows) {
            int y = viewTop + row.y() - scroll;
            if (y + LINE < viewTop || y > viewBottom) {
                continue;
            }
            graphics.drawString(font, row.text(), left + row.x(), y, row.color(), false);
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            // Прокрутка: тонкий чернильный штрих у правого края.
            int track = viewHeight();
            int thumb = Math.max(12, track * track / contentHeight);
            int thumbY = viewTop + (track - thumb) * scroll / maxScroll();
            int x = left + panelWidth - PAD / 2 - 1;
            graphics.fill(x, thumbY, x + 2, thumbY + thumb, INK_SCROLL);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Tab t : Tab.values()) {
                if (t != tab && overTab(t, mouseX, mouseY)) {
                    tab = t;
                    rebuild();
                    scroll = maxScroll();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * LINE * 3, 0, maxScroll());
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
