package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look.Marker;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Scrollbars;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;

public class AE2LookTest {

    @Test
    public void scrollWidthFollowsLook() {
        HandleScrollData large = new HandleScrollData(), small = new HandleScrollData(true);
        int vanilla = AE2Scrollbars.scrollbarWidth(null);
        assertEquals(vanilla, large.getThickness(), "a new list scroll bar starts plain");
        assertEquals(vanilla, small.getThickness(), "the small handle is an AE2 handle; plain looks use vanilla");
        large.look(AE2Look.of(Variant.DARK));
        small.look(AE2Look.of(Variant.DARK));
        assertEquals(AE2Theme.BIG_SCROLLER_WIDTH, large.getThickness(), "an AE2 panel gives the large AE2 handle");
        assertEquals(AE2Theme.SCROLLBAR_WIDTH, small.getThickness(), "an AE2 panel gives the small AE2 handle");
        small.look(AE2Look.plain());
        assertEquals(vanilla, small.getThickness(), "a plain panel takes the vanilla width back");
    }

    @Test
    public void skinsByFolder() {
        for (Variant variant : Variant.values())
            assertSame(variant, AE2Look.skin(variant.folder), "the folder name finds its skin: " + variant.folder);
        assertNull(AE2Look.skin(""), "an empty name is the plain look");
        assertNull(AE2Look.skin("metal"), "a folder that is not an AE2 skin is the plain look");
        assertNull(AE2Look.skin(null), "no name is the plain look");
    }

    @Test
    public void sharedLooks() {
        assertSame(AE2Look.plain(), AE2Look.of((Variant) null), "no skin gives the shared plain look");
        assertFalse(
            AE2Look.plain()
                .isAE2(),
            "the plain look is not an AE2 skin");
        for (Variant variant : Variant.values()) {
            AE2Look look = AE2Look.of(variant);
            assertSame(look, AE2Look.of(variant), "each skin has one shared look: " + variant);
            assertSame(variant, look.variant(), "the look keeps its skin: " + variant);
            assertTrue(look.isAE2(), "a skin's look is an AE2 look: " + variant);
        }
    }

    @Test
    public void markerInherits() {
        Marker parent = new Marker(Variant.DARK.folder);
        JsonObject named = new JsonObject();
        named.addProperty(Marker.SKIN, Variant.LIGHT.folder);
        assertEquals(Variant.LIGHT.folder, new Marker(parent, named, null).skin(), "a theme that names a skin uses it");
        assertEquals(
            Variant.DARK.folder,
            new Marker(parent, new JsonObject(), null).skin(),
            "a theme that names no skin keeps its parent's");
    }
}
