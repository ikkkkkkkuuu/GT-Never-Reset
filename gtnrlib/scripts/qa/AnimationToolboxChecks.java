package com.xyp.gtnrlib.qa;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.ToolboxMotion;
import com.xyp.gtnrlib.client.render.animation.ToolboxModel;
import com.xyp.gtnrlib.compat.gregtech.GregTechToolboxAnimation;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ToolboxSlot;
import gregtech.api.modularui2.ToolboxSelectGuiFactory;
import gregtech.api.net.GTPacketToolboxEvent;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxUtil;

/** Drives the real GT containers and packets in an isolated, invisible client. No desktop input. */
@Mod(
    modid = "toolboxanimationqa",
    name = "Toolbox Animation QA",
    version = "1",
    dependencies = "after:gtnrlib;after:gregtech")
public final class AnimationToolboxChecks {

    private final AnimationClientChecks qa = new AnimationClientChecks();
    private final AtomicReference<Runnable> serverTask = new AtomicReference<>();
    private final File output = new File(System.getProperty("gtnrlib.animation.qa.output", "."));
    private volatile Throwable serverError;
    private UUID actor;
    private boolean launched, finished;
    private int ticks, stage, entered, captures;
    private int[] carrying, searching;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.animation.qa.toolbox")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (++ticks > 2400) throw new AssertionError("toolbox QA timed out at stage " + stage);
            if (!launched && mc.currentScreen != null && mc.theWorld == null) {
                launched = true;
                mc.launchIntegratedServer(
                    System.getProperty("gtnrlib.animation.qa.world"),
                    "Toolbox QA",
                    new WorldSettings(4711, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            finish(mc, failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getMinecraft();
        if (finished || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;
        try {
            if (serverError != null) throw new AssertionError("server operation failed", serverError);
            int age = mc.thePlayer.ticksExisted;
            var stack = mc.thePlayer.getHeldItem();
            switch (stage) {
                case 0 -> {
                    if (age < 30 || mc.currentScreen != null) return;
                    qa.require(!Loader.isModLoaded("gtneverreset"), "standalone GTNRLib with GregTech and no host mod");
                    actor = mc.thePlayer.getUniqueID();
                    mc.gameSettings.thirdPersonView = 0;
                    mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
                    serverTask.set(() -> {
                        var player = serverPlayer();
                        ItemStack box = ItemList.ToolBox.get(1);
                        var inventory = new ToolboxItemStackHandler(box);
                        inventory.setStackInSlot(ToolboxSlot.WRENCH.getSlotID(), tool(IDMetaTool01.WRENCH));
                        inventory.setStackInSlot(ToolboxSlot.SCREWDRIVER.getSlotID(), tool(IDMetaTool01.SCREWDRIVER));
                        inventory.setStackInSlot(ToolboxSlot.WIRE_CUTTER.getSlotID(), tool(IDMetaTool01.WIRECUTTER));
                        ToolboxUtil.saveToolbox(box, inventory);
                        player.inventory.currentItem = 0;
                        player.inventory.setInventorySlotContents(0, box);
                        player.inventory.markDirty();
                    });
                    next(age);
                }
                case 1 -> {
                    if (!(stack != null && stack.getItem() instanceof ItemGTToolbox) || age - entered < 25) return;
                    snapshot(mc, "toolbox-carry-world");
                    carrying = qa.draw(mc, "toolbox-carry", .5f);
                    qa.require(
                        HmiFirstPerson.INSTANCE.handheldTool() instanceof GregTechToolboxAnimation,
                        "unselected GT toolbox binds despite having no Forge renderer");
                    qa.require(
                        MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON) == null,
                        "closed toolbox uses the vanilla item-render path before adaptation");
                    serverTask.set(() -> {
                        var player = serverPlayer();
                        player.theItemInWorldManager.tryUseItem(player, player.worldObj, player.getHeldItem());
                    });
                    next(age);
                }
                case 2 -> {
                    if (!ModularScreen.isActive("gregtech", "Field Engineer's Toolbox")) return;
                    qa.require(true, "normal server right-click opened the original GT inventory");
                    next(age);
                }
                case 3 -> {
                    int elapsed = age - entered;
                    if (captures < 4 && elapsed >= new int[] { 6, 13, 23, 35 }[captures]) {
                        snapshot(mc, "toolbox-inventory-world-" + captures);
                        var pixels = qa.draw(mc, "toolbox-open-" + captures, .5f);
                        qa.require(
                            !Arrays.equals(carrying, pixels),
                            "inventory stage " + captures + " changes the visible pose");
                        if (captures == 3) searching = pixels;
                        captures++;
                    }
                    if (elapsed < 46) return;
                    qa.require(
                        !Arrays.equals(searching, qa.draw(mc, "toolbox-search", .5f)),
                        "searching continues with the inventory open");
                    checkContents(mc, stack);
                    mc.thePlayer.closeScreen();
                    next(age);
                }
                case 4 -> {
                    if (age - entered < 30 || mc.currentScreen != null) return;
                    qa.draw(mc, "toolbox-closed-again", .5f);
                    ToolboxSelectGuiFactory.INSTANCE.open(mc.thePlayer);
                    next(age);
                }
                case 5 -> {
                    if (!ModularScreen.isActive("gregtech", "ToolboxSelectUI")) return;
                    qa.require(true, "original GT radial selector opened");
                    next(age);
                }
                case 6 -> {
                    if (age - entered < 32) return;
                    snapshot(mc, "toolbox-radial-world");
                    qa.draw(mc, "toolbox-radial-search", .5f);
                    qa.require(
                        HmiFirstPerson.INSTANCE.handheldTool() instanceof GregTechToolboxAnimation,
                        "radial selection animates the same toolbox");
                    GTValues.NW.sendToServer(
                        new GTPacketToolboxEvent(
                            GTPacketToolboxEvent.Action.CHANGE_ACTIVE_TOOL,
                            0,
                            ToolboxSlot.WRENCH.getSlotID()));
                    mc.thePlayer.closeScreen();
                    next(age);
                }
                case 7 -> {
                    if (ToolboxUtil.getSelectedToolType(stack)
                        .isEmpty() || age - entered < 15) return;
                    qa.draw(mc, "toolbox-selected-tool", .5f);
                    qa.require(
                        !(HmiFirstPerson.INSTANCE.handheldTool() instanceof GregTechToolboxAnimation),
                        "selected tool returns to GT's native first-person renderer");
                    qa.require(
                        ToolboxUtil.getSelectedTool(stack)
                            .isPresent(),
                        "selection preserved the stored wrench");
                    GTValues.NW.sendToServer(
                        new GTPacketToolboxEvent(
                            GTPacketToolboxEvent.Action.CHANGE_ACTIVE_TOOL,
                            0,
                            ItemGTToolbox.NO_TOOL_SELECTED));
                    next(age);
                }
                case 8 -> {
                    if (age - entered < 20 || ToolboxUtil.getSelectedToolType(stack)
                        .isPresent()) return;
                    GTNRLibConfig.enableFirstPersonAnimations = false;
                    int[] vanilla = qa.draw(mc, "toolbox-disabled", .5f);
                    GTNRLibConfig.enableFirstPersonAnimations = true;
                    int[] enabled = qa.draw(mc, "toolbox-enabled", .5f);
                    qa.require(!Arrays.equals(vanilla, enabled), "animation toggle restores the original flat item");
                    mc.thePlayer.inventory.currentItem = 1;
                    next(age);
                }
                case 9 -> {
                    if (age - entered < 15) return;
                    qa.draw(mc, "toolbox-switched-away", .5f);
                    qa.require(
                        !(HmiFirstPerson.INSTANCE.handheldTool() instanceof GregTechToolboxAnimation),
                        "changing slots releases the toolbox model");
                    finish(mc, null);
                }
                default -> throw new AssertionError("unknown stage " + stage);
            }
        } catch (Throwable failure) {
            finish(mc, failure);
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var task = serverTask.getAndSet(null);
        if (task != null) try {
            task.run();
        } catch (Throwable failure) {
            serverError = failure;
        }
    }

    private static ItemStack tool(IDMetaTool01 type) {
        return MetaGeneratedTool01.INSTANCE.getToolWithStats(type.ID, 1, Materials.Steel, Materials.Wood, null);
    }

    private void checkContents(Minecraft mc, ItemStack stack) throws Exception {
        var pose = new ToolboxMotion();
        for (int frame = 0; frame <= 120; frame++) pose.sample(frame / 60d, true);
        var stored = new ToolboxItemStackHandler(stack);
        var tools = new ItemStack[] { stored.getStackInSlot(ToolboxSlot.WRENCH.getSlotID()),
            stored.getStackInSlot(ToolboxSlot.SCREWDRIVER.getSlotID()),
            stored.getStackInSlot(ToolboxSlot.WIRE_CUTTER.getSlotID()) };
        int[] filled = qa
            .draw(mc, "toolbox-filled-tray", .5f, () -> ToolboxModel.render(pose, stack, tools, left -> {}));
        int[] empty = qa
            .draw(mc, "toolbox-empty-tray", .5f, () -> ToolboxModel.render(pose, stack, new ItemStack[0], left -> {}));
        int changed = 0;
        for (int i = 0; i < filled.length; i++) if (filled[i] != empty[i]) changed++;
        qa.require(changed > 40, "actual stored GT tools produce visible tray pixels: " + changed);
    }

    private EntityPlayerMP serverPlayer() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .worldServerForDimension(0)
            .func_152378_a(actor);
    }

    private void next(int age) {
        entered = age;
        stage++;
    }

    private void snapshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        qa.finish(mc, failure);
    }
}
