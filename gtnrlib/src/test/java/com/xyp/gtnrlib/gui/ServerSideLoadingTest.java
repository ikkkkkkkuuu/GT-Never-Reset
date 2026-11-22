package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ServerSideLoadingTest {

    @Test
    public void ae2WidgetsBuildOnADedicatedServer() throws Exception {
        try (ServerClassLoader loader = new ServerClassLoader()) {
            Class<?> type = loader.loadClass(ServerWidgetScenario.class.getName());
            assertSame(loader, type.getClassLoader(), "the scenario must run inside the server loader");
            Runnable scenario = (Runnable) type.getConstructor()
                .newInstance();
            assertDoesNotThrow(scenario::run, "AE2 widgets must not need client-only classes while a GUI is built");
        }
    }

    @Test
    public void loaderRefusesClientOnlyClasses() throws Exception {
        try (ServerClassLoader loader = new ServerClassLoader()) {
            assertThrows(
                IllegalStateException.class,
                () -> loader.loadClass("com.xyp.gtnrlib.gui.theme.PaletteThemes"),
                "a class marked @SideOnly(Side.CLIENT) must not load in the server loader");
            assertThrows(
                ClassNotFoundException.class,
                () -> loader.loadClass("net.minecraft.client.Minecraft"),
                "client packages must be missing in the server loader");
        }
    }
}
