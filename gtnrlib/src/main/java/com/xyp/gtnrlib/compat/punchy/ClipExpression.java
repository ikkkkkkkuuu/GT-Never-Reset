package com.xyp.gtnrlib.compat.punchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/** Bounded arithmetic grammar for clip axes. Expressions have no access to the JVM or game. */
final class ClipExpression {

    private final String source;
    private int pos;

    private ClipExpression(String source) {
        this.source = source.toLowerCase(Locale.ROOT);
    }

    static DoubleUnaryOperator compile(String source) {
        if (source.length() > 256) throw new IllegalArgumentException("expression exceeds 256 characters");
        ClipExpression parser = new ClipExpression(source);
        var value = parser.sum();
        parser.space();
        if (parser.pos != source.length()) throw parser.error();
        return t -> {
            double n = value.applyAsDouble(t);
            return Double.isFinite(n) ? n : 0;
        };
    }

    private DoubleUnaryOperator sum() {
        var value = product();
        while (true) {
            var l = value;
            if (take('+')) {
                var r = product();
                value = t -> l.applyAsDouble(t) + r.applyAsDouble(t);
            } else if (take('-')) {
                var r = product();
                value = t -> l.applyAsDouble(t) - r.applyAsDouble(t);
            } else return value;
        }
    }

    private DoubleUnaryOperator product() {
        var value = atom();
        while (true) {
            var l = value;
            if (take('*')) {
                var r = atom();
                value = t -> l.applyAsDouble(t) * r.applyAsDouble(t);
            } else if (take('/')) {
                var r = atom();
                value = t -> {
                    double d = r.applyAsDouble(t);
                    return d == 0 ? 0 : l.applyAsDouble(t) / d;
                };
            } else return value;
        }
    }

    private DoubleUnaryOperator atom() {
        if (take('-')) {
            var v = atom();
            return t -> -v.applyAsDouble(t);
        }
        if (take('(')) {
            var v = sum();
            require(')');
            return v;
        }
        space();
        int start = pos;
        if (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
            while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) pos++;
            double number = Double.parseDouble(source.substring(start, pos));
            return t -> number;
        }
        while (pos < source.length()
            && (Character.isLetterOrDigit(source.charAt(pos)) || "._".indexOf(source.charAt(pos)) >= 0)) pos++;
        if (start == pos) throw error();
        String name = source.substring(start, pos);
        if (!take('(')) return switch (name) {
            case "q.anim_time", "query.anim_time" -> t -> t;
            case "pi" -> t -> Math.PI;
            case "e" -> t -> Math.E;
            default -> t -> 0;
        };
        List<DoubleUnaryOperator> args = new ArrayList<>();
        if (!take(')')) {
            do {
                args.add(sum());
            } while (take(','));
            require(')');
        }
        if (name.startsWith("math.")) name = name.substring(5);
        int count = switch (name) {
            case "sin", "cos", "tan", "abs", "sqrt", "floor", "ceil", "round" -> 1;
            case "min", "max" -> 2;
            case "clamp" -> 3;
            default -> -1;
        };
        if (count == -1) return t -> 0;
        if (args.size() != count) throw error();
        var x = args.get(0);
        return switch (name) {
            case "sin" -> t -> Math.sin(Math.toRadians(x.applyAsDouble(t)));
            case "cos" -> t -> Math.cos(Math.toRadians(x.applyAsDouble(t)));
            case "tan" -> t -> Math.tan(Math.toRadians(x.applyAsDouble(t)));
            case "abs" -> t -> Math.abs(x.applyAsDouble(t));
            case "sqrt" -> t -> Math.sqrt(x.applyAsDouble(t));
            case "floor" -> t -> Math.floor(x.applyAsDouble(t));
            case "ceil" -> t -> Math.ceil(x.applyAsDouble(t));
            case "round" -> t -> Math.round(x.applyAsDouble(t));
            case "min" -> t -> Math.min(
                x.applyAsDouble(t),
                args.get(1)
                    .applyAsDouble(t));
            case "max" -> t -> Math.max(
                x.applyAsDouble(t),
                args.get(1)
                    .applyAsDouble(t));
            default -> t -> Math.max(
                args.get(1)
                    .applyAsDouble(t),
                Math.min(
                    x.applyAsDouble(t),
                    args.get(2)
                        .applyAsDouble(t)));
        };
    }

    private void require(char c) {
        if (!take(c)) throw error();
    }

    private void space() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) pos++;
    }

    private boolean take(char c) {
        space();
        if (pos >= source.length() || source.charAt(pos) != c) return false;
        pos++;
        return true;
    }

    private IllegalArgumentException error() {
        return new IllegalArgumentException("invalid expression at " + pos);
    }
}
