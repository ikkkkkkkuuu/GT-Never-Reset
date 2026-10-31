package com.xyp.gtnrlib.gui.theme;

import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/** Nine-slice controls using a caller-owned modern sprite sheet set. */
public final class ModernGuiSkin {

    public final UITexture panel;
    public final UITexture button;
    public final UITexture hover;
    public final UITexture disabled;
    public final UITexture slot;
    public final UITexture tab;
    public final UITexture selectedTab;
    public final UITexture input;
    public final UITexture inputFocused;

    private final String namespace;
    private final String directory;

    /** The directory contains the background, button, slot, tabs_left and text_field sprite sheets. */
    public ModernGuiSkin(String namespace, String directory) {
        this.namespace = java.util.Objects.requireNonNull(namespace);
        this.directory = java.util.Objects.requireNonNull(directory);
        this.panel = texture("background", 256, 256, 2);
        this.button = texture("button", 200, 20, 3);
        this.hover = texture("button_highlighted", 200, 20, 3);
        this.disabled = texture("button_disabled", 200, 20, 3);
        this.slot = texture("slot", 18, 18, 1);
        this.tab = sprite("tabs_left", 64, 84, 0, 28, 32, 28, 3);
        this.selectedTab = sprite("tabs_left", 64, 84, 32, 28, 32, 28, 3);
        this.input = sprite("text_field", 128, 128, 0, 0, 128, 12, 1);
        this.inputFocused = sprite("text_field", 128, 128, 0, 24, 128, 12, 1);
    }

    public ButtonWidget<?> button() {
        ButtonWidget<?> widget = new ButtonWidget<>();
        return widget.background(new DynamicDrawable(() -> widget.isEnabled() ? button : disabled))
            .hoverBackground(hover);
    }

    public TextFieldWidget textField() {
        TextFieldWidget field = new TextFieldWidget();
        return field.background(new DynamicDrawable(() -> field.isFocused() ? inputFocused : input))
            .padding(3, 0)
            .setTextAlignment(Alignment.CenterLeft)
            .setTextColor(0xFFFFFFFF);
    }

    private UITexture texture(String name, int width, int height, int border) {
        return sprite(name, width, height, 0, 0, width, height, border);
    }

    private UITexture sprite(String name, int imageWidth, int imageHeight, int x, int y, int width, int height,
        int border) {
        return UITexture.builder()
            .location(namespace, directory + name)
            .imageSize(imageWidth, imageHeight)
            .nonOpaque()
            .subAreaXYWH(x, y, width, height)
            .adaptable(border)
            .build()
            .withColorOverride(0xFFFFFFFF);
    }
}
