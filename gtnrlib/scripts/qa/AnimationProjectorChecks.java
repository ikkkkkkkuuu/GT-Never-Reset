package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.GuiViewportStack;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.render.animation.ProjectorArmModel;
import com.xyp.gtnrlib.client.render.animation.ProjectorHologramRenderer;
import com.xyp.gtnrlib.compat.gtneverreset.ProjectorArmAnimation;
import com.xyp.gtnrlib.config.GTNRLibConfig;
import com.xyp.gtnrlib.gui.widget.PageTabs;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

@Mod(
    modid = "projectorarmqa",
    name = "Forearm projector QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class AnimationProjectorChecks {

    private final File output = new File(System.getProperty("gtnrlib.projector.arm.qa.output", "."));
    private final AtomicReference<Runnable> serverTask = new AtomicReference<>();
    private volatile Throwable failure;
    private boolean launched, finished, raisedBeforeGui;
    private int ticks, stage, entered;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.projector.arm.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (++ticks > 2200) throw new AssertionError("timeout at projector QA stage " + stage);
            if (failure != null) throw new AssertionError("server setup failed", failure);
            if (!launched
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen != null && mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "projector-arm-" + System.currentTimeMillis(),
                    "Forearm Projector QA",
                    new WorldSettings(4, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 25) return;
            if (stage == 0) {
                mc.gameSettings.thirdPersonView = 0;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
                String name = mc.thePlayer.getCommandSenderName();
                serverTask.set(() -> {
                    var player = MinecraftServer.getServer()
                        .getConfigurationManager()
                        .func_152612_a(name);
                    var stack = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                    var plan = new ProjectorPlan();
                    plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
                    plan.write(stack);
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
                    player.inventoryContainer.detectAndSendChanges();
                });
                advance();
            }
        } catch (Throwable error) {
            finish(mc, error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || stage != 2 && ticks - entered < 20) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        try {
            var held = mc.thePlayer.getHeldItem();
            switch (stage) {
                case 1 -> {
                    if (held == null || mc.currentScreen != null) return;
                    require(
                        MinecraftForgeClient
                            .getItemRenderer(held, ItemRenderType.INVENTORY) instanceof ProjectorArmAnimation,
                        "forearm model registered in inventory");
                    require(
                        HmiFirstPerson.INSTANCE.handheldTool() instanceof ProjectorArmAnimation,
                        "held projector uses the animated arm rig");
                    capture(mc, "projector-first-person");
                    ProjectorClient.open(0);
                    advance();
                }
                case 2 -> {
                    if (!ModularScreen.isActive("gtneverreset", "multiblock_projector")) {
                        if (!raisedBeforeGui && ticks - entered >= 7 && mc.currentScreen == null) {
                            capture(mc, "projector-raising-held");
                            raisedBeforeGui = true;
                        }
                        return;
                    }
                    require(raisedBeforeGui, "raised-arm presentation begins before the GUI covers the view");
                    if (ticks - entered < 12) return;
                    capture(mc, "projector-held-gui-first-person");
                    mc.thePlayer.closeScreen();
                    advance();
                }
                case 3 -> {
                    require(mc.currentScreen == null, "held-terminal GUI closed");
                    capture(mc, "projector-reset-held");
                    GTNRLibConfig.enableFirstPersonAnimations = false;
                    advance();
                }
                case 4 -> {
                    capture(mc, "projector-vanilla-first-person");
                    GTNRLibConfig.enableFirstPersonAnimations = true;
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                    advance();
                }
                case 5 -> {
                    require(mc.currentScreen instanceof GuiInventory, "player inventory was displayed");
                    capture(mc, "projector-inventory");
                    mc.thePlayer.closeScreen();
                    mc.gameSettings.thirdPersonView = 2;
                    advance();
                }
                case 6 -> {
                    require(rightArmPitch(mc) > -.24f, "held terminal uses the normal third-person resting arm");
                    capture(mc, "projector-held-third-person");
                    String name = mc.thePlayer.getCommandSenderName();
                    serverTask.set(() -> {
                        var player = MinecraftServer.getServer()
                            .getConfigurationManager()
                            .func_152612_a(name);
                        int hotbar = player.inventory.currentItem;
                        var stack = player.inventory.getStackInSlot(hotbar);
                        var baubles = BaublesCompat.getBaubles(player);
                        require(baubles != null, "baubles inventory available");
                        for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                            if (baubles.getStackInSlot(slot) != null || !baubles.isItemValidForSlot(slot, stack))
                                continue;
                            baubles.setInventorySlotContents(slot, stack);
                            player.inventory.setInventorySlotContents(hotbar, null);
                            baubles.markDirty();
                            player.inventoryContainer.detectAndSendChanges();
                            return;
                        }
                        throw new AssertionError("no usable universal bauble slot");
                    });
                    advance();
                }
                case 7 -> {
                    if (held != null || !ProjectorArmAnimation.wearsProjector(mc.thePlayer)) return;
                    capture(mc, "projector-worn-third-person");
                    mc.gameSettings.thirdPersonView = 0;
                    var baubles = BaublesCompat.getBaubles(mc.thePlayer);
                    for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                        var stack = baubles.getStackInSlot(slot);
                        if (stack != null
                            && stack.getItem() == GameRegistry.findItem("gtneverreset", "multiblock_projector")) {
                            ProjectorClient.open(-slot - 1);
                            break;
                        }
                    }
                    advance();
                }
                case 8 -> {
                    if (!ModularScreen.isActive("gtneverreset", "multiblock_projector")) {
                        require(ticks - entered < 60, "worn terminal GUI did not open");
                        return;
                    }
                    require(
                        ModularScreen.getCurrent()
                            .shouldDrawDarkBackground(),
                        "original GUI backdrop preserved");
                    var lens = ProjectorHologramRenderer.class.getDeclaredField("lensVisible");
                    lens.setAccessible(true);
                    if (!lens.getBoolean(null)) {
                        if (ticks - entered >= 60) capture(mc, "projector-worn-qa-debug");
                        require(ticks - entered < 60, "worn terminal GUI opened without a visible lens sample");
                        return;
                    }
                    capture(mc, "projector-gui-first-person");
                    mc.gameSettings.thirdPersonView = 2;
                    advance();
                }
                case 9 -> {
                    require(
                        ModularScreen.isActive("gtneverreset", "multiblock_projector"),
                        "projector interface stayed open");
                    require(rightArmPitch(mc) < -.9f, "third-person GUI raises the right arm");
                    capture(mc, "projector-gui-third-person");
                    clickProjectedTab(ModularScreen.getCurrent());
                    mc.thePlayer.closeScreen();
                    advance();
                }
                case 10 -> {
                    require(
                        !ModularScreen.isActive("gtneverreset", "multiblock_projector"),
                        "projector interface closed");
                    require(rightArmPitch(mc) > -.6f, "right arm returned to idle");
                    capture(mc, "projector-after-close");
                    showObserverView(mc);
                    advance();
                }
                case 11 -> {
                    capture(mc, "projector-observer");
                    mc.theWorld.removeEntityFromWorld(-147);
                    mc.displayGuiScreen(new ModelGallery());
                    advance();
                }
                case 12 -> {
                    capture(mc, "projector-model-angles");
                    finish(mc, null);
                }
                default -> {}
            }
        } catch (Throwable error) {
            finish(mc, error);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var work = serverTask.getAndSet(null);
        if (work == null) return;
        try {
            work.run();
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void advance() {
        stage++;
        entered = ticks;
    }

    private void capture(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Minecraft mc, Throwable error) {
        try {
            Files.writeString(
                new File(output, "result.txt").toPath(),
                error == null ? "PASS\n" : "FAIL " + error + "\n");
        } catch (Exception writeError) {
            throw new IllegalStateException(writeError);
        } finally {
            finished = true;
            mc.shutdown();
        }
    }

    private static void showObserverView(Minecraft mc) {
        mc.gameSettings.thirdPersonView = 0;
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 10;
        var actor = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.randomUUID(), "Projection"));
        actor.setPositionAndRotation(
            mc.thePlayer.posX + .25,
            mc.thePlayer.boundingBox.minY,
            mc.thePlayer.posZ + 3.6,
            166,
            0);
        actor.prevPosX = actor.posX;
        actor.prevPosY = actor.posY;
        actor.prevPosZ = actor.posZ;
        actor.prevRotationYaw = actor.rotationYaw;
        actor.rotationYawHead = actor.prevRotationYawHead = 166;
        actor.renderYawOffset = actor.prevRenderYawOffset = 166;
        actor.setCurrentItemOrArmor(0, new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector")));
        mc.theWorld.addEntityToWorld(-147, actor);
        ProjectorArmAnimation.receivePose(mc, -147, mc.thePlayer.dimension, true);
    }

    private static void clickProjectedTab(ModularScreen screen) throws ReflectiveOperationException {
        var panel = (ProjectorGui) screen.getMainPanel();
        var tabs = (PageTabs) panel.getChildren()
            .stream()
            .filter(PageTabs.class::isInstance)
            .findFirst()
            .orElseThrow();
        var tab = tabs.getChildren()
            .get(ProjectorGui.MACHINES);
        var viewport = new GuiViewportStack();
        viewport.pushViewport(null, screen.getScreenArea());
        viewport.pushViewport(panel, panel.getArea());
        panel.transform(viewport);
        int x = tab.getArea().x - panel.getArea().x + tab.getArea().width / 2;
        int y = tab.getArea().y - panel.getArea().y + tab.getArea().height / 2;
        screen.getContext()
            .updateState(viewport.transformX(x, y), viewport.transformY(x, y), .5f);
        screen.onFrameUpdate();
        require(screen.onMousePressed(0), "projected tab receives mouse input");
        screen.onMouseRelease(0);
        var page = ProjectorGui.class.getDeclaredField("page");
        page.setAccessible(true);
        require(page.getInt(panel) == ProjectorGui.MACHINES, "projected tab uses existing page navigation");
    }

    private static float rightArmPitch(Minecraft mc) {
        var renderer = (RenderPlayer) RenderManager.instance.getEntityRenderObject(mc.thePlayer);
        return renderer.modelBipedMain.bipedRightArm.rotateAngleX;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ModelGallery extends GuiScreen {

        private static final String[] LABELS = { "Optics and controls", "Forearm opening", "Fasteners and cooling" };
        private static final float[][] ANGLES = { { 5, 158, -18 }, { 68, 142, 7 }, { -17, -25, 12 } };

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawGradientRect(0, 0, width, height, 0xff40535a, 0xff1f313c);
            drawCenteredString(
                fontRendererObj,
                "WRIST-MOUNTED HOLOGRAPHIC TERMINAL",
                width / 2,
                height / 8,
                0xffdcf2f4);
            for (int i = 0; i < ANGLES.length; i++) {
                int center = width * (2 * i + 1) / 6;
                drawCenteredString(fontRendererObj, LABELS[i], center, height * 4 / 5, 0xff91b7c0);
                float[] rotation = ANGLES[i];
                GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
                GL11.glPushMatrix();
                float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
                try {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    RenderHelper.enableGUIStandardItemLighting();
                    GL11.glTranslatef(center, height * .47f, 80);
                    float size = Math.min(width / 3f, height / 2f) * 1.18f;
                    GL11.glScalef(size, size, size);
                    GL11.glRotatef(rotation[0], 1, 0, 0);
                    GL11.glRotatef(rotation[1], 0, 1, 0);
                    GL11.glRotatef(rotation[2], 0, 0, 1);
                    ProjectorArmModel.render();
                } finally {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
                    GL11.glPopMatrix();
                    GL11.glPopAttrib();
                }
            }
        }
    }
}
