package com.xyp.gtnrlib.qa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.GuiViewportStack;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
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
                    player.worldObj.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                    player.worldObj.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                    player.worldObj.setWorldTime(6000);
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
        if (finished) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (event.phase == TickEvent.Phase.START) {
            // A profile skin may arrive mid-test. Use the same game skin for both idle screenshots.
            mc.thePlayer.func_152121_a(Type.SKIN, AbstractClientPlayer.locationStevePng);
            return;
        }
        if (stage != 2 && ticks - entered < 20) return;
        try {
            var held = mc.thePlayer.getHeldItem();
            switch (stage) {
                case 1 -> {
                    if (held == null || mc.currentScreen != null) return;
                    require(
                        "gtneverreset:multiblock_projector".equals(held.getIconIndex().getIconName()),
                        "projector uses the original item sprite");
                    require(
                        MinecraftForgeClient.getItemRenderer(held, ItemRenderType.INVENTORY) == null
                            && MinecraftForgeClient.getItemRenderer(held, ItemRenderType.EQUIPPED_FIRST_PERSON) == null
                            && MinecraftForgeClient.getItemRenderer(held, ItemRenderType.EQUIPPED) == null,
                        "projector uses standard inventory and held rendering");
                    capture(mc, "projector-first-person");
                    held.getItem().onItemRightClick(held, mc.theWorld, mc.thePlayer);
                    advance();
                }
                case 2 -> {
                    if (!ModularScreen.isActive("gtneverreset", "multiblock_projector")) {
                        if (!raisedBeforeGui && ticks - entered >= 6 && mc.currentScreen == null) {
                            capture(mc, "projector-raising-held");
                            raisedBeforeGui = true;
                        }
                        return;
                    }
                    require(raisedBeforeGui, "raised-arm presentation begins before the GUI covers the view");
                    if (ticks - entered < 12) return;
                    require(
                        changedArmPixels("projector-first-person", "projector-raising-held") >= 200,
                        "opening moves the hand before showing the GUI");
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
                    require(rightArmPitch(mc) > -.6f, "held terminal uses the normal third-person resting arm");
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
                    // Use the vanilla idle pose for both captures so HMI breathing does not change the arm pixels.
                    GTNRLibConfig.enableFirstPersonAnimations = false;
                    mc.gameSettings.thirdPersonView = 0;
                    advance();
                }
                case 8 -> {
                    require(held == null && ProjectorArmAnimation.wearsProjector(mc.thePlayer), "projector worn at idle");
                    capture(mc, "projector-worn-idle-first-person");
                    int slot = projectorSlot(BaublesCompat.getBaubles(mc.thePlayer));
                    require(slot >= 0, "worn terminal slot found");
                    GTNRLibConfig.enableFirstPersonAnimations = true;
                    ProjectorClient.open(-slot - 1);
                    advance();
                }
                case 9 -> {
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
                case 10 -> {
                    require(
                        ModularScreen.isActive("gtneverreset", "multiblock_projector"),
                        "projector interface stayed open");
                    require(rightArmPitch(mc) < -.9f, "third-person GUI raises the right arm");
                    capture(mc, "projector-gui-third-person");
                    clickProjectedTab(ModularScreen.getCurrent());
                    mc.thePlayer.closeScreen();
                    advance();
                }
                case 11 -> {
                    require(
                        !ModularScreen.isActive("gtneverreset", "multiblock_projector"),
                        "projector interface closed");
                    require(rightArmPitch(mc) > -.6f, "right arm returned to idle");
                    capture(mc, "projector-after-close");
                    showObserverView(mc);
                    advance();
                }
                case 12 -> {
                    capture(mc, "projector-observer");
                    mc.theWorld.removeEntityFromWorld(-147);
                    GTNRLibConfig.enableFirstPersonAnimations = false;
                    mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
                    String name = mc.thePlayer.getCommandSenderName();
                    serverTask.set(() -> {
                        var player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
                        var baubles = BaublesCompat.getBaubles(player);
                        int slot = projectorSlot(baubles);
                        require(slot >= 0, "server found worn terminal");
                        baubles.setInventorySlotContents(slot, null);
                        baubles.markDirty();
                        player.inventoryContainer.detectAndSendChanges();
                    });
                    advance();
                }
                case 13 -> {
                    if (ProjectorArmAnimation.wearsProjector(mc.thePlayer)) return;
                    capture(mc, "projector-unequipped-idle-first-person");
                    require(
                        changedArmPixels(
                            "projector-worn-idle-first-person",
                            "projector-unequipped-idle-first-person") < 500,
                        "equipped idle projector leaves the first-person arm unchanged");
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
        require(new File(output, "screenshots/" + name + ".png").isFile(), "screenshot written: " + name);
    }

    private int changedArmPixels(String first, String second) throws IOException {
        File screenshots = new File(output, "screenshots");
        var a = ImageIO.read(new File(screenshots, first + ".png"));
        var b = ImageIO.read(new File(screenshots, second + ".png"));
        int width = a.getWidth(), height = a.getHeight(), differences = 0;
        require(width == b.getWidth() && height == b.getHeight(), "same camera dimensions");
        for (int y = height / 2; y < height; y++) {
            for (int x = width / 2; x < width; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                if (Math.abs((p >> 16 & 255) - (q >> 16 & 255)) > 24
                    || Math.abs((p >> 8 & 255) - (q >> 8 & 255)) > 24
                    || Math.abs((p & 255) - (q & 255)) > 24) differences++;
            }
        }
        System.out.println("[projector-arm-qa] " + first + " vs " + second + " changed pixels: " + differences);
        return differences;
    }

    private static int projectorSlot(IInventory baubles) {
        if (baubles == null) return -1;
        var projector = GameRegistry.findItem("gtneverreset", "multiblock_projector");
        for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
            var stack = baubles.getStackInSlot(slot);
            if (stack != null && stack.getItem() == projector) return slot;
        }
        return -1;
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
}
