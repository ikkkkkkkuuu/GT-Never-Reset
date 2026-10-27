package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.client.render.FluxConnectorRender;
import com.xyp.gtneverreset.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = "fluxclientqa",
    name = "Flux Client QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NEVER_RESET)
public final class FluxClientChecks {

    private final FluxChecks checks = new FluxChecks();
    private volatile boolean finished;
    private volatile int stage;
    private boolean started;
    private boolean geometryChecked;
    private int clientTicks;
    private int serverTicks;
    private int frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.flux.client.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (finished) {
            FMLCommonHandler.instance()
                .exitJava(0, false);
            return;
        }
        try {
            require(++clientTicks < 3000, "Flux client timeout at stage " + stage);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            if (!started && mc.currentScreen != null && mc.theWorld == null) {
                started = true;
                mc.launchIntegratedServer(
                    "flux-qa-" + System.currentTimeMillis(),
                    "Flux QA",
                    new WorldSettings(742L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            serverTicks++;
            if (serverTicks == 20) {
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                checks.setup(player);
                player.worldObj.getGameRules()
                    .setOrCreateGameRule("doDaylightCycle", "false");
                player.worldObj.getGameRules()
                    .setOrCreateGameRule("doMobSpawning", "false");
                player.worldObj.setWorldTime(6000);
                require(
                    GameRegistry.findBlock(ModList.GTNeverReset.getID(), "flux_logistics_plug") == null,
                    "logistics connector is excluded from registration");
            }
            if (serverTicks == 80) {
                checks.verifyNative(player);
                TileEntityGuiFactory.INSTANCE.open(player, 2, 10, 0);
                stage = 1;
            }
            if (stage == 7) {
                var point = checks.point();
                require(
                    point.customName()
                        .equals("Flux QA") && point.priority() == 123,
                    "name and priority editors synchronize to the authoritative server");
                require(
                    point.voltage() == 128 && point.amperage() == 3 && point.redstoneRequired(),
                    "voltage/current editors and redstone switch synchronize to the authoritative server");
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(1.5, 11.5, -4.5, 0, 18);
                stage = 8;
            }
            if (stage == 9) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 10, -2.5, 0, 15);
                TileEntityGuiFactory.INSTANCE.open(player, 0, 10, 0);
                stage = 10;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0 || stage == 7 || stage == 9) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 8) {
                if (mc.currentScreen != null || ++frames < 100) return;
                capture("flux-energy-world.png");
                frames = 0;
                stage = 9;
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return;
            ModularPanel panel = gui.getScreen()
                .getMainPanel();
            require(
                panel.getName()
                    .equals("flux_connector"),
                "real connector GUI is open");
            if (++frames < 35) return;
            frames = 0;
            if (!geometryChecked) {
                verifyGeometry();
                verifyTextures();
                geometryChecked = true;
            }
            PagedWidget<?> pages = collect(panel, PagedWidget.class).get(0);
            List<ButtonWidget> tabs = new ArrayList<>();
            for (IWidget child : panel.getChildren()) if (child instanceof ButtonWidget tab) tabs.add(tab);
            require(tabs.size() == 5, "five original navigation buttons are present");
            if (stage == 1) {
                capture("flux-point-home.png");
                List<TextFieldWidget> fields = collect(pages.getCurrentPage(), TextFieldWidget.class);
                require(fields.size() == 2, "home provides name and priority fields");
                edit(fields.get(0), "Flux QA", panel);
                edit(fields.get(1), "123", panel);
                tabs.get(1)
                    .onMousePressed(0);
                require(pages.getCurrentPageIndex() == 1, "network tab activates its page");
                stage = 2;
            } else if (stage == 2) {
                capture("flux-point-account.png");
                tabs.get(2)
                    .onMousePressed(0);
                stage = 3;
            } else if (stage == 3) {
                capture("flux-point-connections.png");
                tabs.get(3)
                    .onMousePressed(0);
                stage = 4;
            } else if (stage == 4) {
                capture("flux-point-statistics.png");
                tabs.get(4)
                    .onMousePressed(0);
                stage = 5;
            } else if (stage == 5) {
                List<TextFieldWidget> fields = collect(pages.getCurrentPage(), TextFieldWidget.class);
                require(fields.size() == 2, "settings provides voltage and amperage fields");
                edit(fields.get(0), "128", panel);
                edit(fields.get(1), "3", panel);
                ((InteractionSyncHandler) gui.getScreen()
                    .getSyncManager()
                    .getMainPSM()
                    .findSyncHandlerNullable("redstone", 0)).onMousePressed(0);
                stage = 6;
            } else if (stage == 6) {
                capture("flux-point-settings.png");
                stage = 7;
            } else if (stage == 10) {
                capture("flux-plug-home.png");
                Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS");
                System.out.println(
                    "FLUX_CLIENT_QA PASS: native GT transfer, EU conservation, NBT, recipes, geometry, "
                        + "five GUI pages, owner edits, server synchronization and both block models");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void verifyGeometry() throws Exception {
        Method vertices = FluxConnectorRender.class
            .getDeclaredMethod("vertices", ForgeDirection.class, double[].class, double[].class);
        vertices.setAccessible(true);
        Method rotation = FluxConnectorRender.class
            .getDeclaredMethod("rotate", double.class, double.class, double.class, int.class);
        rotation.setAccessible(true);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            double[] foot = rotate(rotation, new double[] { 0, -0.5, 0 }, side.ordinal());
            require(
                Math.abs(foot[0] - side.offsetX * 0.5) < 0.000001 && Math.abs(foot[1] - side.offsetY * 0.5) < 0.000001
                    && Math.abs(foot[2] - side.offsetZ * 0.5) < 0.000001,
                "model connection reaches " + side);
        }
        for (int side = -1; side < 6; side++) {
            for (ForgeDirection face : ForgeDirection.VALID_DIRECTIONS) {
                double[][] quad = (double[][]) vertices
                    .invoke(null, face, new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 });
                double[] a = rotate(rotation, quad[3], side), b = rotate(rotation, quad[2], side),
                    c = rotate(rotation, quad[1], side);
                double[] n = rotate(rotation, new double[] { face.offsetX, face.offsetY, face.offsetZ }, side);
                double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
                double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
                require(
                    (uy * vz - uz * vy) * n[0] + (uz * vx - ux * vz) * n[1] + (ux * vy - uy * vx) * n[2] > 0,
                    "quad winding points outward for " + face + " rotation " + side);
            }
        }
    }

    private static double[] rotate(Method method, double[] point, int side) throws Exception {
        return (double[]) method.invoke(null, point[0], point[1], point[2], side);
    }

    private static void verifyTextures() {
        Minecraft mc = Minecraft.getMinecraft();
        for (String type : new String[] { "plug", "point" }) {
            for (String state : new String[] { "on", "off", "colour" }) {
                String name = ModList.GTNeverReset.getResourcePath("flux/flux_" + type + "_" + state);
                TextureAtlasSprite sprite = mc.getTextureMapBlocks()
                    .getAtlasSprite(name);
                require(
                    sprite.getIconName()
                        .equals(name) && sprite.getIconWidth() > 0,
                    "atlas contains " + name);
            }
        }
    }

    private static void edit(TextFieldWidget field, String value, ModularPanel panel) {
        field.setText(value);
        field.onRemoveFocus(panel.getContext());
    }

    private static <T> List<T> collect(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) matches.add(type.cast(root));
        for (IWidget child : root.getChildren()) matches.addAll(collect(child, type));
        return matches;
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.flux.client.qa.output"));
        output.mkdirs();
        return output;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fail(Throwable failure) {
        if (finished) return;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(new File(output(), "client-result.txt").toPath(), "FAIL\n" + trace);
        } catch (Exception ignored) {}
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
