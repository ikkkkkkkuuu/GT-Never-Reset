package com.xyp.gtnrlib.gui.widget;

import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.api.IJsonSerializable;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.BufferBuilder;
import com.cleanroommc.modularui.drawable.DrawableSerialization;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.JsonHelper;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.gui.theme.AE2Theme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SideOnly(Side.CLIENT)
public class AE2ButtonDrawable implements IDrawable, IJsonSerializable {

    public static final String TYPE = "gtnrlib:ae2_button";
    private static final String STATE = "state", SKIN = "skin";
    private static final int TEXTURE_WIDTH = 200, TEXTURE_HEIGHT = 20, EDGE = 2, FRAME_ROWS = 4, TEXT_ROOM = 13,
        MIN_SHADOW = 1, MAX_SHADOW = 3, FACE_TOP = 2, FACE_BOTTOM = 3, LABEL_HEIGHT = 8;
    private static final Map<String, ResourceLocation[]> TEXTURES = new Object2ObjectOpenHashMap<>();

    static {
        DrawableSerialization.registerDrawableType(
            TYPE,
            AE2ButtonDrawable.class,
            json -> new AE2ButtonDrawable(State.NORMAL, AE2Theme.Variant.LIGHT.folder));
    }

    private State state;
    private String skin;

    public AE2ButtonDrawable(State state, String skin) {
        this.state = state;
        this.skin = skin;
    }

    public static int shadow(State state, int height) {
        return Math.max(0, baseShadow(height) - state.sink);
    }

    public static int textY(State state, int height) {
        int shadow = shadow(state, height);
        int top = baseShadow(height) - shadow + FACE_TOP, face = height - FACE_BOTTOM - shadow - top + 1;
        return Math.max(0, top + (face - LABEL_HEIGHT + 1) / 2);
    }

    private static int baseShadow(int height) {
        return Math.max(MIN_SHADOW, Math.min(MAX_SHADOW, height - TEXT_ROOM));
    }

    public static void draw(String skin, State state, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int shadow = shadow(state, height), sunk = baseShadow(height) - shadow;
        int fill = Math.max(0, height - sunk - FRAME_ROWS - shadow);
        Platform.setupDrawTex(texture(skin, state), true);
        GlStateManager.color(1, 1, 1, 1);
        Platform.startDrawing(Platform.DrawMode.QUADS, Platform.VertexFormat.POS_TEX, buffer -> {
            int row = y + sunk;
            row = strip(buffer, state.frame, x, row, width, 1);
            row = strip(buffer, state.light, x, row, width, 1);
            row = strip(buffer, state.fill, x, row, width, fill);
            row = strip(buffer, state.low, x, row, width, 1);
            row = strip(buffer, state.dark, x, row, width, shadow);
            strip(buffer, state.outline, x, row, width, 1);
        });
    }

    private static ResourceLocation texture(String skin, State state) {
        return TEXTURES.computeIfAbsent(skin, folder -> {
            State[] states = State.values();
            ResourceLocation[] textures = new ResourceLocation[states.length];
            for (State each : states) textures[each.ordinal()] = AE2Theme.texture(folder, each.file);
            return textures;
        })[state.ordinal()];
    }

    private static int strip(BufferBuilder buffer, int source, int x, int y, int width, int height) {
        if (height <= 0) return y;
        float v0 = source / (float) TEXTURE_HEIGHT, v1 = (source + 1) / (float) TEXTURE_HEIGHT;
        int edge = Math.min(EDGE, width / 2), right = x + width - edge;
        float uEdge = EDGE / (float) TEXTURE_WIDTH, uRight = (TEXTURE_WIDTH - EDGE) / (float) TEXTURE_WIDTH;
        GuiDraw.drawTexture(buffer, x, y, x + edge, y + height, 0, v0, uEdge, v1, 0);
        GuiDraw.drawTexture(buffer, x + edge, y, right, y + height, uEdge, v0, uRight, v1, 0);
        GuiDraw.drawTexture(buffer, right, y, x + width, y + height, uRight, v0, 1, v1, 0);
        return y + height;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        draw(skin, state, x, y, width, height);
    }

    @Override
    public void loadFromJson(JsonObject json) {
        state = State.byName(JsonHelper.getString(json, state.name, STATE));
        skin = JsonHelper.getString(json, skin, SKIN);
    }

    @Override
    public boolean saveToJson(JsonObject json) {
        json.addProperty(STATE, state.name);
        json.addProperty(SKIN, skin);
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AE2ButtonDrawable drawable && drawable.state == state && drawable.skin.equals(skin);
    }

    @Override
    public int hashCode() {
        return state.hashCode() * 31 + skin.hashCode();
    }

    public enum State {

        NORMAL("normal", "button", 0, 0, 1, 2, 15, 16, 19),
        HOVER("hover", "button_highlighted", 1, 1, 2, 3, 16, 17, 19),
        DISABLED("disabled", "button_disabled", 2, 2, 3, 4, 17, 18, 19);

        public final String name, file;
        public final int sink;
        private final int frame, light, fill, low, dark, outline;

        State(String name, String file, int sink, int frame, int light, int fill, int low, int dark, int outline) {
            this.name = name;
            this.file = file;
            this.sink = sink;
            this.frame = frame;
            this.light = light;
            this.fill = fill;
            this.low = low;
            this.dark = dark;
            this.outline = outline;
        }

        public static State of(Widget<?> widget, boolean available, boolean selected) {
            if (!available) return DISABLED;
            return selected || widget.isHovering() ? HOVER : NORMAL;
        }

        public static State byName(String name) {
            for (State state : values()) if (state.name.equals(name)) return state;
            return NORMAL;
        }
    }
}
