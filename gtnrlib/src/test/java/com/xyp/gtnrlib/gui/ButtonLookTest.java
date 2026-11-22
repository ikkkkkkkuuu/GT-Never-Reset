package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.drawable.UITexture;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

public class ButtonLookTest {

    @Test
    public void shadowRule() {
        assertEquals(3, AE2ButtonDrawable.shadow(State.NORMAL, 20), "a 20-pixel button has AE2's 3-pixel shadow");
        assertEquals(3, AE2ButtonDrawable.shadow(State.NORMAL, 16), "the full shadow holds down to 16 pixels");
        assertEquals(2, AE2ButtonDrawable.shadow(State.NORMAL, 15), "a 15-pixel button keeps 2 pixels of shadow");
        assertEquals(1, AE2ButtonDrawable.shadow(State.NORMAL, 14), "a 14-pixel button keeps 1 pixel of shadow");
        assertEquals(1, AE2ButtonDrawable.shadow(State.NORMAL, 8), "the shadow never drops below 1 pixel");
        assertEquals(2, AE2ButtonDrawable.shadow(State.HOVER, 20), "a hovered button's shadow is 1 pixel thinner");
        assertEquals(1, AE2ButtonDrawable.shadow(State.DISABLED, 20), "a disabled button's shadow is 2 pixels thinner");
        assertEquals(0, AE2ButtonDrawable.shadow(State.DISABLED, 14), "a thinner shadow never goes below zero");
    }

    @Test
    public void labelSinks() {
        assertEquals(5, AE2ButtonDrawable.textY(State.NORMAL, 20), "a 20-pixel button's label starts 5 pixels down");
        for (int height = 8; height <= 32; height++) for (State state : State.values()) {
            int lost = AE2ButtonDrawable.shadow(State.NORMAL, height) - AE2ButtonDrawable.shadow(state, height);
            assertEquals(
                AE2ButtonDrawable.textY(State.NORMAL, height) + lost,
                AE2ButtonDrawable.textY(state, height),
                "a " + state.name + " label at height " + height + " sinks by the pixels the shadow lost");
        }
    }

    @Test
    public void stateChoice() {
        AE2Button button = new AE2Button(() -> "Apply", () -> {});
        assertEquals(
            State.DISABLED,
            State.of(button, false, true),
            "an unavailable button is disabled, selected or not");
        assertEquals(State.HOVER, State.of(button, true, true), "a selected button uses AE2's highlighted button");
        assertEquals(State.NORMAL, State.of(button, true, false), "an idle button is normal");
        for (State state : State.values())
            assertEquals(state, State.byName(state.name), "theme JSON finds the " + state.name + " state by name");
        assertEquals(State.NORMAL, State.byName("missing"), "an unknown name in theme JSON falls back to normal");
        AE2ButtonDrawable saved = new AE2ButtonDrawable(State.HOVER, Variant.DARK.folder);
        JsonObject json = new JsonObject();
        saved.saveToJson(json);
        AE2ButtonDrawable loaded = new AE2ButtonDrawable(State.NORMAL, Variant.LIGHT.folder);
        loaded.loadFromJson(json);
        assertEquals(saved, loaded, "a button background saved in theme JSON loads back with its state and skin");
    }

    @Test
    public void stateTextures() {
        Path assets = Paths.get("src/main/resources/assets/gtnrlib");
        Set<String> files = new HashSet<>();
        for (State state : State.values()) {
            assertTrue(files.add(state.file), "each button state has its own texture, " + state.file + " twice");
            for (Variant variant : Variant.values()) {
                Path texture = assets.resolve(
                    AE2Theme.texture(variant.folder, state.file)
                        .getResourcePath());
                assertTrue(Files.isRegularFile(texture), variant.folder + " has the " + state.name + " button");
            }
        }
    }

    @Test
    public void checkboxSprites() {
        for (Variant variant : Variant.values()) {
            Set<String> checkboxes = new HashSet<>(), radios = new HashSet<>();
            for (boolean on : new boolean[] { false, true }) for (boolean hovered : new boolean[] { false, true }) {
                checkboxes.add(region(variant.checkboxSprite(on, hovered)));
                radios.add(region(variant.radioSprite(on, hovered)));
            }
            assertEquals(4, checkboxes.size(), variant.folder + " draws each check box state from its own sprite");
            assertEquals(4, radios.size(), variant.folder + " draws each radio state from its own sprite");
        }
    }

    private static String region(UITexture texture) {
        return texture.location + "@" + texture.u0 + "," + texture.v0;
    }
}
