package com.xyp.gtnrlib.client.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import com.xyp.gtnrlib.client.text.EffectTextLayout.Glyph;
import com.xyp.gtnrlib.text.SlotOrder;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * What rolling numbers show at one moment. Digits joined only by a separator ({@code , . _ '}) form one number, and
 * every number in a run counts up at the same time. Each digit is an odometer wheel: the ones wheel turns with the
 * value, and a higher wheel only turns while every wheel below it rolls over from 9 to 0. Wheels the value has not
 * reached take no room, so a number grows from the left and pushes what follows right as a wheel turns in; a number
 * written with leading zeros keeps all its wheels, and a decimal keeps its decimals and the digit before the point, so
 * {@code 0.85} rolls as {@code 0.xx}. Values are exact at any length. With a {@link SlotOrder} other than
 * {@code NONE} every wheel shows from the start and spins on its own instead, stopping on its digit one after another.
 */
public class NumberRoll {

    public static final double JUMP_STEP_SECONDS = 0.05;
    private static final double JUMP_EASE_POWER = 1.6;
    private static final double SLIDE_EASE_POWER = 3;
    private static final String SEPARATORS = ",._'";
    private static final int THOUSANDS_GROUP = 3;
    private static final double SLOT_DIGITS_PER_SECOND = 14;
    private static final double SLOT_BRAKE_SECONDS = 0.3;
    private static final double SLOT_SETTLE_SECONDS = 0.18;
    private static final double SLOT_FIRST_STOP = 0.35;
    private static final double SLOT_BOUNCE = 0.22;
    private static final int SLOT_SPARE_TURNS = 40;
    private static final float SLOT_BLUR_SPREAD = 0.45f;
    private static final float SLOT_MIN_BLUR = 0.1f;
    private static final float[] SLOT_GHOST_SHIFTS = { -1, -0.5f, 0.5f, 1 };
    private static final float[] SLOT_GHOST_ALPHAS = { 0.3f, 0.55f, 0.55f, 0.3f };

    private NumberRoll() {}

    public static boolean isDigit(String text) {
        return text.length() == 1 && text.charAt(0) >= '0' && text.charAt(0) <= '9';
    }

    /**
     * {@code progress} runs from 0 to 1 over {@code seconds}. A jumping number only changes every
     * {@link #JUMP_STEP_SECONDS} and eases out more gently, so its first steps are not one big leap. A spinning slot
     * wheel smears into faint copies above and below it, {@code blur} percent as strong as by default; 0 turns it off.
     */
    public static Frame frame(List<Glyph> glyphs, float progress, float seconds, boolean jump, SlotOrder slot,
        int blur) {
        Wheels wheels = new Wheels(glyphs.size());
        if (slot != SlotOrder.NONE) {
            for (IntList digits : numbers(glyphs))
                wheels.spin(glyphs, digits, progress * seconds, seconds, jump, blur / 100f, slot);
        } else {
            double shown = progress;
            if (jump && seconds > 0) {
                double step = JUMP_STEP_SECONDS / seconds;
                shown = Math.min(1, Math.floor(progress / step) * step);
            }
            BigDecimal eased = BigDecimal.valueOf(1 - Math.pow(1 - shown, jump ? JUMP_EASE_POWER : SLIDE_EASE_POWER));
            for (IntList digits : numbers(glyphs)) wheels.turn(glyphs, digits, eased, jump);
        }
        float[] presence = new float[glyphs.size()];
        float leftPresence = 1;
        float hidden = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            if (isDigit(
                glyphs.get(i)
                    .text()))
                leftPresence = wheels.shown[i] ? 1 : wheels.turn[i];
            presence[i] = leftPresence;
            hidden += glyphs.get(i)
                .width() * (1 - leftPresence);
        }
        List<Layer> layers = new ObjectArrayList<>();
        StringBuilder text = new StringBuilder();
        float x = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            if (isDigit(glyph.text())) {
                int digit = wheels.digit[i];
                float turn = wheels.turn[i];
                if (wheels.shown[i]) {
                    layers.add(new Layer(String.valueOf(digit), x, -turn, turn > 0, 1));
                    text.append(digit);
                }
                if (turn > 0) layers.add(new Layer(String.valueOf((digit + 1) % 10), x, 1 - turn, true, 1));
                if (wheels.blur[i] > SLOT_MIN_BLUR) smear(layers, digit, turn, wheels.blur[i], x);
            } else if (glyph.width() > 0 && presence[i] > 0.5f) {
                layers.add(new Layer(glyph.text(), x, 0, false, 1));
                if (presence[i] >= 1) text.append(glyph.text());
            }
            x += glyph.width() * presence[i];
        }
        return new Frame(List.copyOf(layers), text.toString(), hidden);
    }

    private static void smear(List<Layer> layers, int digit, float turn, float blur, float x) {
        for (int ghost = 0; ghost < SLOT_GHOST_SHIFTS.length; ghost++) {
            for (int next = 0; next < 2; next++) {
                float shift = next - turn + SLOT_GHOST_SHIFTS[ghost] * SLOT_BLUR_SPREAD * blur;
                if (shift != 0) layers.add(
                    new Layer(
                        String.valueOf((digit + next) % 10),
                        x,
                        shift,
                        true,
                        Math.min(1, SLOT_GHOST_ALPHAS[ghost] * blur)));
            }
        }
    }

    private static List<IntList> numbers(List<Glyph> glyphs) {
        List<IntList> numbers = new ObjectArrayList<>();
        IntList current = null;
        boolean separated = false;
        for (int i = 0; i < glyphs.size(); i++) {
            String text = glyphs.get(i)
                .text();
            if (isDigit(text)) {
                if (current == null) {
                    current = new IntArrayList();
                    numbers.add(current);
                }
                current.add(i);
                separated = false;
            } else if (current != null && !text.isEmpty()) {
                if (!separated && text.length() == 1 && SEPARATORS.indexOf(text.charAt(0)) >= 0) separated = true;
                else current = null;
            }
        }
        return numbers;
    }

    // Leading zeros keep every wheel; a decimal keeps its decimals and the digit before the point. A last separator
    // with exactly three digits after it reads as a thousands separator unless an earlier one is a different mark.
    private static int alwaysShown(List<Glyph> glyphs, IntList digits) {
        int count = digits.size();
        if (count > 1 && glyphs.get(digits.getInt(0))
            .text()
            .equals("0")) return count;
        char last = 0;
        int after = 0;
        boolean mixed = false;
        for (int position = 1; position < count; position++) {
            char mark = separatorBefore(glyphs, digits.getInt(position - 1), digits.getInt(position));
            if (mark == 0) continue;
            if (last != 0 && mark != last) mixed = true;
            last = mark;
            after = count - position;
        }
        boolean decimal = last != 0 && (after != THOUSANDS_GROUP || mixed);
        return decimal ? after + 1 : 1;
    }

    private static char separatorBefore(List<Glyph> glyphs, int previousDigit, int digit) {
        for (int i = digit - 1; i > previousDigit; i--) {
            String text = glyphs.get(i)
                .text();
            if (!text.isEmpty()) return text.charAt(0);
        }
        return 0;
    }

    private static class Wheels {

        private final int[] digit;
        private final float[] turn;
        private final boolean[] shown;
        private final float[] blur;

        private Wheels(int size) {
            digit = new int[size];
            turn = new float[size];
            shown = new boolean[size];
            blur = new float[size];
        }

        private void turn(List<Glyph> glyphs, IntList digits, BigDecimal eased, boolean jump) {
            StringBuilder written = new StringBuilder();
            for (int i = 0; i < digits.size(); i++) written.append(
                glyphs.get(digits.getInt(i))
                    .text());
            BigDecimal value = new BigDecimal(written.toString()).multiply(eased);
            int always = alwaysShown(glyphs, digits);
            int lowerDigit = 9;
            float lowerTurn = 0;
            for (int place = 0; place < digits.size(); place++) {
                int index = digits.getInt(digits.size() - 1 - place);
                BigDecimal position = value.movePointLeft(place);
                BigInteger whole = position.toBigInteger();
                int wheelDigit = whole.mod(BigInteger.TEN)
                    .intValue();
                float wheelTurn = place == 0 ? position.subtract(new BigDecimal(whole))
                    .floatValue() : lowerDigit == 9 ? lowerTurn : 0;
                digit[index] = wheelDigit;
                turn[index] = jump ? 0 : wheelTurn;
                shown[index] = place < always || whole.signum() > 0;
                lowerDigit = wheelDigit;
                lowerTurn = wheelTurn;
            }
        }

        // Every wheel spins from the start and brakes onto its digit, the first stopping a third of the way in and the
        // last as the roll ends; a small bounce marks each stop.
        private void spin(List<Glyph> glyphs, IntList digits, double time, double total, boolean jump, float blurShare,
            SlotOrder order) {
            int last = Math.max(1, digits.size() - 1);
            double lastStop = Math.max(SLOT_BRAKE_SECONDS, total - SLOT_SETTLE_SECONDS);
            for (int place = 0; place < digits.size(); place++) {
                int index = digits.getInt(digits.size() - 1 - place);
                int turnOrder = order == SlotOrder.RIGHT_FIRST ? place : digits.size() - 1 - place;
                double stop = lastStop * (SLOT_FIRST_STOP + (1 - SLOT_FIRST_STOP) * turnOrder / last);
                double position = slotPosition(
                    Integer.parseInt(
                        glyphs.get(index)
                            .text()),
                    stop - time);
                double whole = Math.floor(position);
                digit[index] = (int) whole % 10;
                turn[index] = jump ? 0 : (float) (position - whole);
                shown[index] = true;
                double untilStop = stop - time;
                double pace = untilStop >= SLOT_BRAKE_SECONDS ? 1 : untilStop > 0 ? untilStop / SLOT_BRAKE_SECONDS : 0;
                blur[index] = jump ? 0 : (float) pace * blurShare;
            }
        }

        private static double slotPosition(int target, double untilStop) {
            double landing = target + SLOT_SPARE_TURNS * 10;
            if (untilStop >= SLOT_BRAKE_SECONDS)
                return landing - SLOT_DIGITS_PER_SECOND * (SLOT_BRAKE_SECONDS / 2 + untilStop - SLOT_BRAKE_SECONDS);
            if (untilStop > 0)
                return landing - SLOT_DIGITS_PER_SECOND * untilStop * untilStop / (2 * SLOT_BRAKE_SECONDS);
            double settled = Math.min(1, -untilStop / SLOT_SETTLE_SECONDS);
            return landing + SLOT_BOUNCE * Math.sin(Math.PI * settled) * (1 - settled);
        }
    }

    /**
     * {@code text} is the glyph drawn at {@code x} (relative to the run, after hidden wheels made room) and moved down
     * by {@code shift} line heights at {@code alpha} opacity; {@code moving} marks a digit sliding past the line's
     * edge.
     */
    public record Layer(String text, float x, float shift, boolean moving, float alpha) {}

    /** {@code jumpText} is the whole number as it reads now, used when the number jumps instead of sliding. */
    public record Frame(List<Layer> layers, String jumpText, float hidden) {}
}
