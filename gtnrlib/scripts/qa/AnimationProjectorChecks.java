package com.xyp.gtnrlib.qa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

@Mod(
    modid = "projectoropenqa",
    name = "Projector GUI Opening QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class AnimationProjectorChecks {

    // The old gesture waited eight client ticks before sending the open request.
    private static final int MAX_OPEN_TICKS = 7;
    private final File output = new File(System.getProperty("gtnrlib.projector.open.qa.output", "."));
    private final AtomicReference<Consumer<EntityPlayerMP>> serverTask = new AtomicReference<>();
    private volatile Throwable failure;
    private Stage stage = Stage.GIVE_ITEM;
    private boolean launched, finished;
    private int ticks, requestTick;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.projector.open.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        try {
            require(++ticks < 2200, "Timeout at " + stage);
            if (failure != null) throw new AssertionError("Server setup failed", failure);
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "projector-open-" + System.currentTimeMillis(),
                    "Projector GUI Opening QA",
                    new WorldSettings(4, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
            }
            var player = mc.thePlayer;
            if (player == null || player.ticksExisted < 25) return;
            switch (stage) {
                case GIVE_ITEM -> {
                    GTNRLibConfig.enableFirstPersonAnimations = true;
                    mc.gameSettings.thirdPersonView = 0;
                    player.rotationPitch = player.prevRotationPitch = 0;
                    player.rotationYaw = player.prevRotationYaw = 0;
                    serverTask.set(AnimationProjectorChecks::giveProjector);
                    stage = Stage.OPEN_HELD;
                }
                case WAIT_HELD_GUI -> checkGui("Held projector", Stage.CAPTURE_HELD_GUI);
                case OPEN_WORN -> {
                    if (player.getHeldItem() != null || mc.currentScreen != null) return;
                    IInventory baubles = BaublesCompat.getBaubles(player);
                    if (baubles == null) return;
                    for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                        if (!isProjector(baubles.getStackInSlot(slot))) continue;
                        requestTick = ticks;
                        ProjectorClient.open(-slot - 1);
                        stage = Stage.WAIT_WORN_GUI;
                        return;
                    }
                }
                case WAIT_WORN_GUI -> checkGui("Worn projector", Stage.CAPTURE_WORN_GUI);
                default -> {}
            }
        } catch (Throwable error) {
            finish(mc, error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        try {
            switch (stage) {
                case OPEN_HELD -> {
                    ItemStack held = mc.thePlayer.getHeldItem();
                    if (!isProjector(held) || mc.currentScreen != null) return;
                    require(
                        "gtneverreset:multiblock_projector".equals(held.getIconIndex().getIconName()),
                        "Projector uses its original item sprite");
                    for (ItemRenderType renderType : new ItemRenderType[] { ItemRenderType.INVENTORY,
                        ItemRenderType.EQUIPPED_FIRST_PERSON, ItemRenderType.EQUIPPED }) {
                        require(
                            MinecraftForgeClient.getItemRenderer(held, renderType) == null,
                            "Unexpected projector renderer for " + renderType);
                    }
                    capture(mc, "projector-held");
                    requestTick = ticks;
                    held.getItem().onItemRightClick(held, mc.theWorld, mc.thePlayer);
                    stage = Stage.WAIT_HELD_GUI;
                }
                case CAPTURE_HELD_GUI -> {
                    capture(mc, "projector-held-gui");
                    mc.thePlayer.closeScreen();
                    serverTask.set(AnimationProjectorChecks::wearProjector);
                    stage = Stage.OPEN_WORN;
                }
                case CAPTURE_WORN_GUI -> {
                    capture(mc, "projector-worn-gui");
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
            var players = FMLCommonHandler.instance().getMinecraftServerInstance()
                .getConfigurationManager().playerEntityList;
            work.accept((EntityPlayerMP) players.get(0));
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void checkGui(String description, Stage next) {
        int elapsed = ticks - requestTick;
        require(elapsed <= MAX_OPEN_TICKS, description + " GUI did not open within " + MAX_OPEN_TICKS + " ticks");
        if (ModularScreen.isActive("gtneverreset", "multiblock_projector")) {
            System.out.println("[projector-open-qa] " + description + " GUI opened in " + elapsed + " client ticks");
            stage = next;
        }
    }

    private static void giveProjector(EntityPlayerMP player) {
        var stack = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        var plan = new ProjectorPlan();
        plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
        plan.write(stack);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
        player.inventoryContainer.detectAndSendChanges();
    }

    private static void wearProjector(EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        require(isProjector(held), "Server still has the held projector");
        IInventory baubles = BaublesCompat.getBaubles(player);
        require(baubles != null, "Baubles inventory available");
        for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
            if (baubles.getStackInSlot(slot) != null || !baubles.isItemValidForSlot(slot, held)) continue;
            baubles.setInventorySlotContents(slot, held);
            baubles.markDirty();
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            player.inventoryContainer.detectAndSendChanges();
            return;
        }
        throw new AssertionError("No available bauble slot for the projector");
    }

    private static boolean isProjector(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MultiblockProjector;
    }

    private void capture(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        require(new File(output, "screenshots/" + name + ".png").isFile(), "Screenshot written: " + name);
    }

    private void finish(Minecraft mc, Throwable error) {
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(new File(output, "result.txt").toPath(), error == null ? "PASS\n" : "FAIL " + error + "\n");
        } catch (IOException writeError) {
            throw new IllegalStateException(writeError);
        } finally {
            finished = true;
            mc.shutdown();
        }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private enum Stage {
        GIVE_ITEM,
        OPEN_HELD,
        WAIT_HELD_GUI,
        CAPTURE_HELD_GUI,
        OPEN_WORN,
        WAIT_WORN_GUI,
        CAPTURE_WORN_GUI
    }
}
