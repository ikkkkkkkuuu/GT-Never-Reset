package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.compat.gtneverreset.ProjectorArmAnimation;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = "projectorarmqa",
    name = "Forearm projector QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class AnimationProjectorChecks {

    private final File output = new File(System.getProperty("gtnrlib.projector.arm.qa.output", "."));
    private final AtomicReference<Runnable> serverTask = new AtomicReference<>();
    private volatile Throwable failure;
    private boolean launched, finished;
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
        if (finished || event.phase != TickEvent.Phase.END || ticks - entered < 20) return;
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
                    GTNRLibConfig.enableFirstPersonAnimations = false;
                    capture(mc, "projector-first-person");
                    advance();
                }
                case 2 -> {
                    capture(mc, "projector-vanilla-first-person");
                    GTNRLibConfig.enableFirstPersonAnimations = true;
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                    advance();
                }
                case 3 -> {
                    require(mc.currentScreen instanceof GuiInventory, "player inventory was displayed");
                    capture(mc, "projector-inventory");
                    mc.thePlayer.closeScreen();
                    mc.gameSettings.thirdPersonView = 2;
                    advance();
                }
                case 4 -> {
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
                case 5 -> {
                    if (held != null) return;
                    var baubles = BaublesCompat.getBaubles(mc.thePlayer);
                    boolean worn = false;
                    if (baubles != null) for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                        var stack = baubles.getStackInSlot(slot);
                        if (stack != null
                            && stack.getItem() == GameRegistry.findItem("gtneverreset", "multiblock_projector"))
                            worn = true;
                    }
                    if (!worn) return;
                    capture(mc, "projector-worn-third-person");
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
