package com.xyp.gtneverreset.utils.structure;

import java.util.ArrayList;
import java.util.List;

final class TooltipLineWrapper {

    private static final int LINE_WIDTH = 76;

    static String[] wrap(String[] lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String remaining = line;
            String indent = line.startsWith("   ") ? "     " : "  ";
            int split;
            while ((split = splitAt(remaining)) < remaining.length()) {
                String head = remaining.substring(0, split)
                    .stripTrailing();
                result.add(head);
                remaining = indent + formatting(head)
                    + remaining.substring(split)
                        .stripLeading();
            }
            result.add(remaining);
        }
        return result.toArray(String[]::new);
    }

    private static int splitAt(String text) {
        int width = 0, space = -1;
        for (int index = 0; index < text.length();) {
            if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) {
                index += 2;
                continue;
            }
            int point = text.codePointAt(index);
            int next = index + Character.charCount(point);
            if (point == ' ' && width > 5) space = index;
            int advance = switch (Character.UnicodeScript.of(point)) {
                case HAN, HANGUL, HIRAGANA, KATAKANA -> 2;
                default -> 1;
            };
            if (width + advance > LINE_WIDTH) return space > 5 ? space : index;
            width += advance;
            index = next;
        }
        return text.length();
    }

    private static String formatting(String text) {
        String color = "";
        StringBuilder styles = new StringBuilder();
        for (int index = 0; index + 1 < text.length(); index++) {
            if (text.charAt(index) != '\u00a7') continue;
            char code = Character.toLowerCase(text.charAt(++index));
            if ("0123456789abcdefr".indexOf(code) >= 0) {
                color = "\u00a7" + code;
                styles.setLength(0);
            } else if ("klmno".indexOf(code) >= 0 && styles.indexOf("\u00a7" + code) < 0) {
                styles.append('\u00a7')
                    .append(code);
            }
        }
        return color + styles;
    }
}
