package ru.adept.story.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Надписи поверх игры: субтитры с именем говорящего внизу экрана (по очереди),
 * всплывающее сообщение вехи сверху и короткое уведомление о новой записи в журнале.
 */
public final class StoryOverlay {
    /** Хаос и всё, что от Альянса, — бледно-оранжевый (общая палитра сборки). */
    public static final int CHAOS_COLOR = 0xF2B98A;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int BACKGROUND = 0x99000000;

    private record Line(Component speaker, Component text, int ticks) {
    }

    private static final Deque<Line> SUBTITLES = new ArrayDeque<>();
    private static int subtitleLeft;
    private static List<Component> popup = List.of();
    private static int popupLeft;
    private static int noticeLeft;

    private StoryOverlay() {
    }

    public static void subtitle(Component speaker, Component text) {
        // Время на строку — по длине текста: от 3 до 10 секунд.
        int length = text.getString().length();
        int ticks = Math.max(60, Math.min(200, 40 + length * 2));
        SUBTITLES.add(new Line(speaker, text, ticks));
        if (SUBTITLES.size() == 1) {
            subtitleLeft = ticks;
        }
    }

    public static void popup(List<String> keys) {
        List<Component> lines = new ArrayList<>();
        keys.forEach(k -> lines.add(Component.translatable(k)));
        popup = lines;
        popupLeft = 200;
    }

    public static void notice() {
        noticeLeft = 80;
    }

    public static void clear() {
        SUBTITLES.clear();
        popup = List.of();
        popupLeft = 0;
        noticeLeft = 0;
    }

    public static void tick() {
        if (!SUBTITLES.isEmpty() && --subtitleLeft <= 0) {
            SUBTITLES.poll();
            Line next = SUBTITLES.peek();
            subtitleLeft = next == null ? 0 : next.ticks();
        }
        if (popupLeft > 0) {
            popupLeft--;
        }
        if (noticeLeft > 0) {
            noticeLeft--;
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        Font font = mc.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        Line line = SUBTITLES.peek();
        if (line != null) {
            MutableComponent text = line.speaker().copy().withColor(CHAOS_COLOR)
                    .append(Component.literal(": ").withColor(CHAOS_COLOR))
                    .append(line.text().copy().withColor(TEXT_COLOR));
            List<FormattedCharSequence> rows = font.split(text, Math.min(360, width - 40));
            int bottom = height - 72;
            drawBlock(graphics, font, rows, width / 2, bottom - rows.size() * 10);
        }

        if (popupLeft > 0 && !popup.isEmpty()) {
            List<FormattedCharSequence> rows = new ArrayList<>();
            int wrap = Math.min(300, width - 40);
            for (Component c : popup) {
                rows.addAll(font.split(c, wrap));
            }
            drawBlock(graphics, font, rows, width / 2, height / 5);
        }

        if (noticeLeft > 0) {
            Component notice = Component.translatable("adept_story.journal.notice")
                    .append(" [").append(StoryClient.JOURNAL_KEY.getTranslatedKeyMessage()).append("]");
            int w = font.width(notice);
            int x = width - w - 8;
            graphics.fill(x - 4, 6, x + w + 4, 20, BACKGROUND);
            graphics.drawString(font, notice, x, 9, CHAOS_COLOR, false);
        }
    }

    private static void drawBlock(GuiGraphics graphics, Font font, List<FormattedCharSequence> rows, int centerX, int top) {
        int w = 0;
        for (FormattedCharSequence row : rows) {
            w = Math.max(w, font.width(row));
        }
        graphics.fill(centerX - w / 2 - 4, top - 3, centerX + w / 2 + 4, top + rows.size() * 10 + 1, BACKGROUND);
        int y = top;
        for (FormattedCharSequence row : rows) {
            graphics.drawString(font, row, centerX - font.width(row) / 2, y, TEXT_COLOR, false);
            y += 10;
        }
    }
}
