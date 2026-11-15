package ru.adept.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Перенос длинного текста подсказки по словам: строки в подсказках предмета не переносятся сами.
 * Разрывы «\n» в тексте сохраняются; слово длиннее строки идёт отдельной строкой целиком.
 */
public final class TextWrap {
    /** Ширина строки подсказки в знаках — примерно 200 пикселей шрифта игры. */
    public static final int TOOLTIP_CHARS = 40;

    private TextWrap() {
    }

    public static List<String> wrap(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\\\\n|\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                if (line.length() > 0 && line.length() + 1 + word.length() > maxChars) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() > 0) {
                    line.append(' ');
                }
                line.append(word);
            }
            out.add(line.toString());
        }
        return out;
    }
}
