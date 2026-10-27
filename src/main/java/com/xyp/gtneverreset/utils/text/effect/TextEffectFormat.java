package com.xyp.gtneverreset.utils.text.effect;

/** Native font formatting preserved around the fixed adder span. */
public class TextEffectFormat {

    private TextEffectFormat() {}

    public static String normalizeAmpersand(String token) {
        if (token.length() < 2 || token.charAt(0) != '&') return token;
        char code = Character.toLowerCase(token.charAt(1));
        if (token.length() == 8 && code == '#') return hexToken(token.substring(2));
        if (token.length() == 18 && code == 'g')
            return "\u00a7g" + hexToken(token.substring(4, 10)) + hexToken(token.substring(12));
        if (token.length() == 10 && code == 'u') return "\u00a7u" + hexToken(token.substring(4));
        if (token.length() == 14 && code == 'x') return token.replace('&', '\u00a7');
        if (token.length() == 2 && "0123456789abcdefklmnor".indexOf(code) >= 0) return "\u00a7" + code;
        return token;
    }

    private static String hexToken(String hex) {
        StringBuilder result = new StringBuilder("\u00a7x");
        for (int i = 0; i < hex.length(); i++) result.append('\u00a7')
            .append(hex.charAt(i));
        return result.toString();
    }

    public static int nativeLength(String text, int offset) {
        if (offset + 1 >= text.length() || text.charAt(offset) != '\u00a7') return 0;
        char code = Character.toLowerCase(text.charAt(offset + 1));
        if (code == 'g' && hexColor(text, offset + 2) >= 0 && hexColor(text, offset + 16) >= 0) return 30;
        if (code == 'u' && hexColor(text, offset + 2) >= 0) return 16;
        if (code == 'x' && hexColor(text, offset) >= 0) return 14;
        return 2;
    }

    public static int hexColor(String text, int offset) {
        if (offset + 14 > text.length() || text.charAt(offset) != '\u00a7'
            || Character.toLowerCase(text.charAt(offset + 1)) != 'x') return -1;
        int color = 0;
        for (int i = offset + 2; i < offset + 14; i += 2) {
            int digit = Character.digit(text.charAt(i + 1), 16);
            if (text.charAt(i) != '\u00a7' || digit < 0) return -1;
            color = color << 4 | digit;
        }
        return color;
    }

    public static TextEffectStyle applyColor(TextEffectStyle style, String token) {
        char code = Character.toLowerCase(token.charAt(1));
        if (code == 'r') return null;
        return style;
    }

    public static String applyFormatting(String formatting, String token, boolean hexResetsStyles) {
        char code = Character.toLowerCase(token.charAt(1));
        if (code == 'r') return "";
        if ("0123456789abcdef".indexOf(code) >= 0) return token + retain(formatting, "zvu", true);
        if (token.length() == 14 || token.length() == 30) {
            return token + retain(formatting, hexResetsStyles && code == 'x' ? "zvu" : "klmnozvu", true);
        }
        if (code == 'u' && token.length() == 16) return retain(formatting, "u", false) + token;
        if ("zvu".indexOf(code) >= 0) {
            String remaining = retain(formatting, String.valueOf(code), false);
            return remaining.length() == formatting.length() ? formatting + token : remaining;
        }
        if (code == 'q') return retain(formatting, "gxq", false) + token;
        // Unknown extension codes may toggle registered Angelica effects, so retain every occurrence.
        return "klmno".indexOf(code) >= 0 && formatting.contains(token) ? formatting : formatting + token;
    }

    private static String retain(String formatting, String codes, boolean matching) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < formatting.length();) {
            int length = nativeLength(formatting, i);
            if (length == 0) break;
            boolean match = codes.indexOf(Character.toLowerCase(formatting.charAt(i + 1))) >= 0;
            if (match == matching) result.append(formatting, i, i + length);
            i += length;
        }
        return result.toString();
    }
}
