package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@Mod(
    modid = "recipethreadclientqa",
    name = "Recipe Thread Client QA",
    version = "1",
    dependencies = "after:recipethreadqa")
public final class RecipeThreadClientChecks {

    private volatile int requested;
    private volatile int captured;
    private volatile boolean finished;
    private boolean started;
    private int ticks;
    private int frames;
    private int scene;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.thread.client.qa")) FMLCommonHandler.instance()
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
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        if (!started && mc.currentScreen != null && mc.theWorld == null) {
            started = true;
            mc.launchIntegratedServer(
                "recipe-thread-" + System.currentTimeMillis(),
                "Thread QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (requested != 3 || captured != 2 || scene != 0) return;
        try {
            var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(new RecipeThreadChecks.Fixture())
                .toolTipFinisher();
            List<String> lines = new ArrayList<>(Arrays.asList(tooltip.getInformation()));
            List<String> waila = new ArrayList<>();
            IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IWailaDataAccessor.class },
                (proxy, method, args) -> {
                    if (method.getName()
                        .equals("getNBTData")) return RecipeThreadChecks.wailaSnapshot;
                    return null;
                });
            IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IWailaConfigHandler.class },
                (proxy, method, args) -> true);
            BaseMetaTileEntity controllerTile = (BaseMetaTileEntity) mc.theWorld.getTileEntity(0, 10, 0);
            controllerTile.getMetaTileEntity()
                .getWailaBody(null, waila, accessor, config);
            if (waila.stream()
                .noneMatch(line -> line.contains("2") && line.contains("/"))) {
                throw new AssertionError("Waila thread line is missing");
            }
            lines.add(" ");
            lines.addAll(
                waila.stream()
                    .filter(line -> line.contains("/") && line.contains("2") && !line.contains("waila.bar"))
                    .toList());
            mc.displayGuiScreen(new Preview("Automatic tooltip + native Waila body", lines, true));
            scene = 1;
            frames = 0;
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
            if (++ticks > 1800) throw new AssertionError("module GUI timeout");
            if (ticks == 20) {
                new RecipeThreadChecks().verify(player.worldObj);
                player.playerNetServerHandler.setPlayerLocation(4, 10, -2, 0, 0);
            }
            if (ticks == 50) {
                ParameterModuleHatch speed = (ParameterModuleHatch) ((BaseMetaTileEntity) player.worldObj
                    .getTileEntity(4, 10, 0)).getMetaTileEntity();
                speed.onRightclick(speed.getBaseMetaTileEntity(), player);
                requested = 1;
            }
            if (captured == 1 && requested == 1) {
                player.closeScreen();
                player.worldObj.setBlock(9, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
                BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(9, 10, 0);
                tile.setInitialValuesAsNBT(
                    null,
                    (short) GTNRItemList.DynamicParallelControllerT2.get(1)
                        .getItemDamage());
                ParameterModuleHatch parallel = (ParameterModuleHatch) tile.getMetaTileEntity();
                parallel.setParameter(128);
                player.playerNetServerHandler.setPlayerLocation(9, 10, -2, 0, 0);
                requested = -2;
                ticks = 55;
            }
            if (requested == -2 && ticks == 65) {
                ParameterModuleHatch parallel = (ParameterModuleHatch) ((BaseMetaTileEntity) player.worldObj
                    .getTileEntity(9, 10, 0)).getMetaTileEntity();
                parallel.onRightclick(parallel.getBaseMetaTileEntity(), player);
                requested = 2;
            }
            if (captured == 2 && requested == 2) {
                player.closeScreen();
                requested = 3;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (scene == 0 && (!(mc.currentScreen instanceof GuiContainerWrapper) || requested <= captured)) return;
        if (scene != 0 && !(mc.currentScreen instanceof Preview)) return;
        if (++frames < 20) return;
        try {
            if (scene == 0) {
                capture(requested == 1 ? "speed-module-settings.png" : "parallel-module-settings.png");
                captured = requested;
                frames = 0;
            } else if (scene == 1) {
                capture("modules-tooltip-waila.png");
                var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(new RecipeThreadChecks.Fixture())
                    .toolTipFinisher();
                mc.displayGuiScreen(
                    new Preview(
                        "Automatic structure tooltip / Shift",
                        Arrays.asList(tooltip.getStructureInformation()),
                        false));
                scene = 2;
                frames = 0;
            } else {
                capture("automatic-structure-tooltip.png");
                Files.write(new File(output(), "client-result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println(
                    "RECIPE_THREAD_CLIENT_QA PASS: integrated server checks + two live module GUIs + tooltips + Waila body");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.thread.qa.output"));
        output.mkdirs();
        return output;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(
                new File(output(), "client-result.txt").toPath(),
                ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            failure.addSuppressed(writeFailure);
        }
        finished = true;
    }

    private static final class Preview extends GuiScreen {

        private final String title;
        private final List<String> lines;
        private final boolean showModules;
        private final RenderItem items = new RenderItem();

        private Preview(String title, List<String> lines, boolean showModules) {
            this.title = title;
            this.lines = lines;
            this.showModules = showModules;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawString(fontRendererObj, title, 12, 12, 0xffffff);
            if (showModules) {
                RenderHelper.enableGUIStandardItemLighting();
                int row = 0;
                for (GTNRItemList[] group : List.of(
                    MachineLoader.STATIC_PARALLEL,
                    MachineLoader.DYNAMIC_PARALLEL,
                    MachineLoader.STATIC_SPEED,
                    MachineLoader.DYNAMIC_SPEED,
                    MachineLoader.POWER,
                    MachineLoader.OVERCLOCK)) {
                    for (int column = 0; column < group.length; column++) {
                        items.renderItemAndEffectIntoGUI(
                            fontRendererObj,
                            mc.getTextureManager(),
                            group[column].get(1),
                            20 + column * 24,
                            38 + row * 23);
                    }
                    row++;
                }
                RenderHelper.disableStandardItemLighting();
            }
            drawHoveringText(lines, 16, showModules ? 200 : 44, fontRendererObj);
        }
    }
}
