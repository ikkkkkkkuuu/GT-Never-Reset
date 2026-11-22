package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.ITheme;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Colors;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look.Marker;
import com.xyp.gtnrlib.gui.theme.ae2.PlainLook;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;
import com.xyp.gtnrlib.gui.widget.IconRowList;

public class AE2LookTest {

    @Test
    public void scrollWidthFollowsLook() {
        HandleScrollData large = new HandleScrollData(), small = new HandleScrollData(true);
        int vanilla = AE2Look.plain()
            .scrollbarWidth();
        assertEquals(vanilla, large.getThickness(), "a new list scroll bar starts plain");
        assertEquals(
            AE2Theme.SMALL_SCROLLER_WIDTH,
            small.getThickness(),
            "a small bar is as narrow in the plain look as AE2's");
        large.look(AE2Look.of(Variant.DARK));
        small.look(AE2Look.of(Variant.DARK));
        assertEquals(AE2Theme.BIG_SCROLLER_WIDTH, large.getThickness(), "an AE2 panel gives the large AE2 handle");
        assertEquals(AE2Theme.SMALL_SCROLLER_WIDTH, small.getThickness(), "an AE2 panel gives the small AE2 handle");
        large.look(AE2Look.plain());
        assertEquals(vanilla, large.getThickness(), "a plain panel takes the vanilla width back");
    }

    @Test
    public void toolbarButtons() {
        assertEquals(
            AE2Theme.ICON_BUTTON_HEIGHT,
            AE2Look.of(Variant.LIGHT)
                .iconButtonHeight(),
            "AE2 toolbar buttons keep their shadow below an 18-pixel face");
        assertEquals(
            AE2Theme.ICON_BUTTON_WIDTH,
            AE2Look.plain()
                .iconButtonHeight(),
            "plain toolbar buttons are square, as Minecraft's button has no shadow");
    }

    @Test
    public void panelLooks() {
        ITheme screen = theme(AE2Theme.DARK_ID), dialog = theme("gtnrlib:test_plain"),
            other = theme("gtnrlib:test_other");
        assertSame(AE2Look.of(Variant.DARK), AE2Look.of(screen), "an AE2 theme gives its skin's shared look");
        AE2Look plain = AE2Look.of(dialog);
        assertFalse(plain.isAE2(), "a panel with another theme is plain, whatever the screen's theme is");
        assertTrue(
            plain.plainLook()
                .reads(dialog),
            "a plain look reads the text and button styles of its own panel's theme");
        assertSame(plain, AE2Look.of(dialog), "asking again for the same theme reuses the look");
        assertFalse(
            AE2Look.of(other)
                .plainLook()
                .reads(dialog),
            "another plain theme gets a plain look of its own");
        assertSame(AE2Look.plain(), AE2Look.of((ITheme) null), "no theme gives the plain look without a theme");
        assertSame(
            AE2Look.plain(),
            AE2Look.of(new AE2Button(() -> "", () -> {})),
            "a widget that is not in a panel yet is plain");
        assertNull(
            AE2Look.of(screen)
                .plainLook(),
            "an AE2 look has no plain rules");
    }

    @Test
    public void lookSizes() {
        AE2Look plain = AE2Look.plain(), dark = AE2Look.of(Variant.DARK);
        assertEquals(PlainLook.CHECKBOX_SIZE, plain.checkboxWidth(), "the plain check box is MUI2's square box");
        assertEquals(AE2Theme.CHECKBOX_WIDTH, dark.checkboxWidth(), "AE2's check box is its 22-pixel sprite");
        assertEquals(PlainLook.SCROLLBAR_WIDTH, plain.scrollbarWidth(), "the plain scroll bar is the creative one");
        assertEquals(AE2Theme.BIG_SCROLLER_WIDTH, dark.scrollbarWidth(), "AE2's scroll bar is its handle's width");
        assertEquals(PlainLook.INPUT_ERROR, plain.error(), "plain invalid input is AE2 1.21's text field error red");
        assertEquals(Variant.DARK.colors.error(), dark.error(), "AE2 invalid input uses the skin's error color");
        assertEquals(IconRowList.GOOD, plain.good(), "the plain good color is the one rows name");
    }

    private static ITheme theme(String id) {
        return (ITheme) Proxy.newProxyInstance(
            ITheme.class.getClassLoader(),
            new Class<?>[] { ITheme.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getId", "toString" -> id;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
            });
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

    @Test
    public void listColors() {
        for (Variant variant : Variant.values()) {
            AE2Look look = AE2Look.of(variant);
            assertEquals(variant.colors.text(), look.listText(), "AE2 rows use the skin's text color: " + variant);
            assertEquals(variant.colors.bad(), look.bad(), "AE2 rows use the skin's bad color: " + variant);
            assertEquals(look.hover(), look.nested(), "AE2 skins shade nested rows like hovered ones: " + variant);
            assertFalse(look.listShadow(), "AE2 list text has no shadow: " + variant);
        }
        assertNotEquals(
            AE2Look.of(Variant.LIGHT)
                .hover(),
            AE2Look.of(Variant.DARK)
                .hover(),
            "hover darkens a light skin and lightens a dark one");
        AE2Look plain = AE2Look.plain();
        assertTrue(plain.listShadow(), "plain list text has Minecraft's shadow on the grey slot");
        assertNotEquals(plain.listText(), plain.listMuted(), "plain headers stand apart from row text");
    }

    @Test
    public void colorsByName() {
        Colors colors = Colors.builder()
            .text(1)
            .muted(2)
            .secondary(3)
            .buttonText(4)
            .hoverText(5)
            .disabledText(6)
            .fieldText(7)
            .placeholder(8)
            .line(9)
            .edge(10)
            .panel(11)
            .well(12)
            .outline(13)
            .raised(14)
            .good(15)
            .bad(16)
            .info(17)
            .warning(18)
            .error(19)
            .build();
        assertEquals(
            new Colors(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19),
            colors,
            "each named setter fills its own component");
    }

    @Test
    public void purpleTextures() throws IOException {
        Path assets = Paths.get("src/main/resources/assets/gtnrlib"), skins = assets.resolve("textures/gui/skin");
        assertEquals(
            AE2Theme.PURPLE_TEXTURES,
            names(skins.resolve(AE2Theme.PURPLE_FOLDER)),
            "the purple folder holds exactly the textures it recolours");
        for (String name : names(skins.resolve(AE2Theme.DARK_FOLDER))) {
            Path resolved = assets.resolve(
                AE2Theme.texture(AE2Theme.PURPLE_FOLDER, name)
                    .getResourcePath());
            assertTrue(Files.isRegularFile(resolved), "the purple skin finds " + name + " at " + resolved);
        }
    }

    private static Set<String> names(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(
                file -> file.getFileName()
                    .toString()
                    .replace(".png", ""))
                .collect(Collectors.toSet());
        }
    }
}
