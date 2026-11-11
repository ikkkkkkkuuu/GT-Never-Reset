package com.xyp.gtnrlib.gui.widget;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MathUtils;
import com.cleanroommc.modularui.utils.ParseResult;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SideOnly(Side.CLIENT)
public class NumberEntry extends ParentWidget<NumberEntry> {

    public static final int WIDTH = 138, HEIGHT = 62;
    private static final long[] STEPS = { 1, 10, 100, 1000 }, SHIFT_STEPS = { 1, 16, 32, 64 };
    private static final int[] BUTTON_X = { 0, 28, 62, 100 }, BUTTON_WIDTH = { 22, 28, 32, 38 };
    private static final int MINUS_Y = 42, FIELD_X = 28, FIELD_Y = 25, FIELD_WIDTH = 66, ICON_X = 104, ICON_Y = 27,
        MAX_LENGTH = 64, PLAIN_ERROR = 0xFFFF5555;
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final @Nullable Variant variant;
    private final Field field = new Field();
    private final Warning warning = new Warning();
    private final List<String> messages = new ObjectArrayList<>();
    private String text = "0";
    private long min, max = Long.MAX_VALUE, value;
    private boolean valid = true;
    private LongConsumer listener = ignored -> {};

    public NumberEntry(@Nullable Variant variant) {
        this.variant = variant;
        size(WIDTH, HEIGHT);
        for (int i = 0; i < STEPS.length; i++) {
            child(
                step(i, 1).pos(BUTTON_X[i], 0)
                    .size(BUTTON_WIDTH[i], AE2Theme.BUTTON_HEIGHT));
            child(
                step(i, -1).pos(BUTTON_X[i], MINUS_Y)
                    .size(BUTTON_WIDTH[i], AE2Theme.BUTTON_HEIGHT));
        }
        field.setMaxLength(MAX_LENGTH)
            .setTooltipOverride(true)
            .autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> text, next -> {
                text = next;
                validate(true);
            }))
            .tooltipAutoUpdate(true)
            .tooltipBuilder(tooltip -> messages.forEach(tooltip::addLine));
        child(
            field.pos(FIELD_X, FIELD_Y)
                .size(FIELD_WIDTH, AE2Theme.TEXT_FIELD_HEIGHT));
        child(
            warning.pos(ICON_X, ICON_Y)
                .size(AE2Theme.VALIDATION_ICON_SIZE, AE2Theme.VALIDATION_ICON_SIZE));
        validate(false);
    }

    public NumberEntry bounds(long min, long max) {
        this.min = min;
        this.max = Math.max(min, max);
        validate(false);
        return this;
    }

    public NumberEntry value(long value) {
        setText(Long.toString(value));
        validate(false);
        return this;
    }

    public NumberEntry onChange(LongConsumer listener) {
        this.listener = listener;
        return this;
    }

    public OptionalLong value() {
        return valid ? OptionalLong.of(value) : OptionalLong.empty();
    }

    public boolean isValueValid() {
        return valid;
    }

    public boolean isInputValid() {
        return isValueValid();
    }

    private ButtonWidget<?> step(int index, int sign) {
        ButtonWidget<?> button = new ButtonWidget<>();
        IDrawable background = AE2Theme.button(button, () -> true, () -> false);
        return button.background(background)
            .hoverBackground(background)
            .overlay(AE2Theme.label(button, () -> (sign > 0 ? "+" : "-") + steps()[index], () -> true, () -> false))
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                add(sign * steps()[index]);
                return true;
            });
    }

    private static long[] steps() {
        return GuiScreen.isShiftKeyDown() || GuiScreen.isCtrlKeyDown() ? SHIFT_STEPS : STEPS;
    }

    private void add(long delta) {
        BigDecimal current = valid ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
        BigDecimal next = current.compareTo(BigDecimal.ONE) == 0 && delta > 1 ? BigDecimal.valueOf(delta)
            : current.add(BigDecimal.valueOf(delta));
        setText(
            next.max(BigDecimal.valueOf(min))
                .min(BigDecimal.valueOf(max))
                .toPlainString());
        validate(true);
    }

    private void setText(String next) {
        text = next;
        field.setText(next);
    }

    private void validate(boolean notify) {
        boolean wasValid = valid;
        long previous = value;
        messages.clear();
        BigDecimal number = parse(text);
        if (number == null) {
            // #tr gtnrlib.gui.number_entry.invalid
            // # Not a valid number
            // # zh_CN 不是有效的数字
            messages.add(StatCollector.translateToLocalFormatted("gtnrlib.gui.number_entry.invalid"));
        } else if (number.stripTrailingZeros()
            .scale() > 0) {
                // #tr gtnrlib.gui.number_entry.whole
                // # Only whole numbers are allowed
                // # zh_CN 只能输入整数
                messages.add(StatCollector.translateToLocalFormatted("gtnrlib.gui.number_entry.whole"));
            } else if (number.compareTo(BigDecimal.valueOf(min)) < 0) {
                // #tr gtnrlib.gui.number_entry.min
                // # Minimum is %s
                // # zh_CN 最小为 %s
                messages.add(StatCollector.translateToLocalFormatted("gtnrlib.gui.number_entry.min", min));
            } else if (number.compareTo(BigDecimal.valueOf(max)) > 0) {
                // #tr gtnrlib.gui.number_entry.max
                // # Maximum is %s
                // # zh_CN 最大为 %s
                messages.add(StatCollector.translateToLocalFormatted("gtnrlib.gui.number_entry.max", max));
            }
        valid = messages.isEmpty();
        field.error(valid ? null : variant == null ? PLAIN_ERROR : variant.colors.error());
        warning.setEnabled(!valid);
        if (!valid) return;
        value = number.longValueExact();
        if (!INTEGER.matcher(text.strip())
            .matches()) messages.add("= " + value);
        if (notify && (!wasValid || previous != value)) listener.accept(value);
    }

    private static @Nullable BigDecimal parse(String text) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) return null;
        if (INTEGER.matcher(trimmed)
            .matches()) return new BigDecimal(trimmed);
        try {
            ParseResult result = MathUtils.parseExpression(trimmed, 0);
            if (result.isFailure() || !result.hasValue()
                || !result.getResult()
                    .isNumberValue())
                return null;
            return result.getResult()
                .getNumberValue();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }

    private static class Field extends TextFieldWidget {

        @Override
        public void setText(String text) {
            super.setText(text);
            handler.clampCursors();
            getScrollData().scrollTo(getScrollArea(), 0);
        }

        private void error(@Nullable Integer color) {
            textColor = color;
        }
    }

    private class Warning extends Widget<Warning> {

        private Warning() {
            tooltipAutoUpdate(true).tooltipBuilder(tooltip -> messages.forEach(tooltip::addLine));
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            (variant == null ? Variant.LIGHT : variant).invalid
                .draw(0, 0, AE2Theme.VALIDATION_ICON_SIZE, AE2Theme.VALIDATION_ICON_SIZE);
        }
    }
}
