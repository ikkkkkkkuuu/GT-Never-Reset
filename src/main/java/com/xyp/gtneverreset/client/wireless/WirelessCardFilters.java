package com.xyp.gtneverreset.client.wireless;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.ButtonText;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class WirelessCardFilters {

    private static final int CHIP_GAP = 3, CHIP_PADDING = 4, OPTION_HEIGHT = 14, OPTION_PADDING = 4;

    private WirelessCardFilters() {}

    static ButtonWidget<?> chip(Supplier<String> label, IntSupplier count, IntSupplier color, BooleanSupplier available,
        BooleanSupplier selected, BooleanSupplier choose) {
        ButtonWidget<?> chip = new ButtonWidget<>();
        IDrawable background = AE2Theme.button(chip, available, selected);
        return chip.background(background)
            .hoverBackground(background)
            .overlay(
                (context, x, y, width, height, theme) -> drawChip(
                    chip,
                    context,
                    x,
                    y,
                    width,
                    height,
                    label.get(),
                    count.getAsInt(),
                    color.getAsInt(),
                    available.getAsBoolean(),
                    selected.getAsBoolean()))
            .onMousePressed(button -> {
                if (button != 0 || !available.getAsBoolean()) return false;
                return choose.getAsBoolean();
            });
    }

    private static void drawChip(Widget<?> chip, GuiContext context, int x, int y, int width, int height, String label,
        int number, int color, boolean available, boolean selected) {
        var fontRendererObj = Minecraft.getMinecraft().fontRenderer;
        AE2ButtonDrawable.State state = AE2ButtonDrawable.State.of(chip, available, selected);
        ButtonText style = AE2Theme.buttonText(context, state, height);
        String count = Integer.toString(number);
        int countWidth = fontRendererObj.getStringWidth(count), textY = y + style.y();
        label = fontRendererObj.trimStringToWidth(label, Math.max(0, width - CHIP_PADDING * 2 - countWidth - 4));
        AE2Theme.text(label, x + CHIP_PADDING, textY, style.color(), style.shadow());
        AE2Theme.text(
            count,
            x + width - CHIP_PADDING - countWidth,
            textY,
            available ? color : style.color(),
            style.shadow());
    }

    static <T> CycleDropdown<T> choice(String name, Class<T> type, Supplier<String> label, Function<T, String> option,
        AE2Theme.Variant variant) {
        CycleDropdown<T> choice = new CycleDropdown<>(name, type);
        IDrawable background = AE2Theme.button(choice, () -> true, () -> false);
        return choice.optionToWidget(
            (value, selected) -> selected ? IDrawable.EMPTY.asWidget()
                .size(0, 0) : optionWidget(option.apply(value), variant))
            .background(background)
            .hoverBackground(background)
            .overlay(
                (context, x, y, width, height, theme) -> drawChoice(label.get(), choice, context, x, y, width, height));
    }

    private static TextWidget<?> optionWidget(String text, AE2Theme.Variant variant) {
        TextWidget<?> option = IKey.str(text)
            .asWidget();
        option.widthRel(1f)
            .height(OPTION_HEIGHT)
            .paddingLeft(OPTION_PADDING);
        if (variant != null)
            option.color(() -> option.isHovering() ? variant.colors.hoverText() : variant.colors.text());
        return option;
    }

    private static void drawChoice(String label, Widget<?> choice, GuiContext context, int x, int y, int width,
        int height) {
        ButtonText style = AE2Theme.buttonText(context, AE2ButtonDrawable.State.of(choice, true, false), height);
        int textY = y + style.y();
        AE2Theme.text(
            Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, Math.max(0, width - 16)),
            x + 4,
            textY,
            style.color(),
            style.shadow());
        int arrowX = x + width - 10, arrowY = textY + 2;
        for (int row = 0; row < 3; row++)
            GuiDraw.drawRect(arrowX + row, arrowY + row, 5 - row * 2, 1, 0xFF000000 | style.color());
    }

    static void layoutChips(List<ButtonWidget<?>> chips, int xSize, IntFunction<String> label, IntUnaryOperator count) {
        int available = xSize - 20 - CHIP_GAP * (chips.size() - 1), total = 0;
        int[] widths = new int[chips.size()];
        var font = Minecraft.getMinecraft().fontRenderer;
        for (int i = 0; i < widths.length; i++) {
            widths[i] = CHIP_PADDING * 2 + font.getStringWidth(label.apply(i))
                + 4
                + font.getStringWidth(Integer.toString(count.applyAsInt(i)));
            total += widths[i];
        }
        int x = 10;
        for (int i = 0; i < widths.length; i++) {
            int width = total <= available ? widths[i] : widths[i] * available / total;
            chips.get(i)
                .pos(x, 47)
                .size(width, 14);
            x += width + CHIP_GAP;
        }
    }
}
