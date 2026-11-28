package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;
import com.xyp.gtnrlib.client.input.KeyBuffer;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.IconButton;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

@Mod(
    modid = "gtnrprojectorpinnedgridqa",
    name = "Pinned projector grid checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorPinnedGridChecks {

    private enum Stage {
        SETUP,
        OPEN,
        BATCH_TAB,
        SEED_BATCH_GRID,
        CHECK_TOOLBAR,
        PAINT_EDITOR,
        EDIT,
        VERIFY_EDITOR,
        VERIFY_HUD,
        OPEN_FULL,
        REOPEN,
        VERIFY_FULL_GUI,
        VERIFY_UNPIN_GUI,
        TRY_ALT_AFTER_UNPIN,
        VERIFY_UNPIN
    }

    private static final String PINNED = "projector_pinned_";
    private static final String HUD_SCREENSHOT = "projector-pinned-hud.png";
    private static final String EDITOR_SCREENSHOT = "projector-pinned-editor.png";
    private static final String FULL_SCREENSHOT = "projector-full-batch.png";
    private static final int[] OTHER_PAGES = { ProjectorGui.MACHINES, ProjectorGui.PARAMETERS, ProjectorGui.HATCHES,
        ProjectorGui.GENERATION, ProjectorGui.MATERIALS };
    private static final ForgeDirection[] AXES = { ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH };

    private volatile Stage stage = Stage.SETUP;
    private volatile boolean serverSynced;
    private boolean launched, done, holdingEditorKey, hudCaptured, editorCaptured, fullCaptured;
    private int ticks, captureAfterTick, toolbarPage, verifyUnpinAfterTick, machine, x, z;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (stage == Stage.SETUP) {
            setup(player);
            stage = Stage.OPEN;
        } else if (stage == Stage.VERIFY_HUD && !serverSynced) {
            serverSynced = hasEditedGrid(ProjectorPlan.read(player.getHeldItem()));
        }
    }

    private void setup(EntityPlayerMP player) {
        machine = ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1));
        var plan = new ProjectorPlan();
        plan.selectMachine(machine);
        plan.state = ProjectorPlan.State.LOCKED;
        plan.dimension = player.dimension;
        plan.x = x = (int) player.posX;
        plan.y = 12;
        plan.z = z = (int) player.posZ;
        plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
        plan.batch.faceOutward = false;
        plan.display = ProjectorPlan.Display.BLOCKS;
        plan.buildStyle = StructureDeploymentStyle.SINGULARITY;
        var item = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
        plan.write(item);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, item);
        player.inventoryContainer.detectAndSendChanges();
        player.worldObj.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        player.worldObj.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        player.worldObj.setWorldTime(1400);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.rotationYaw = 28;
        player.rotationPitch = 21;
        player.setPositionAndUpdate(x + 18, 26, z - 24);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (holdingEditorKey) KeyBuffer.set(Keyboard.KEY_LMENU, true);
            return;
        }
        var mc = Minecraft.getMinecraft();
        if (done) {
            mc.shutdown();
            return;
        }
        try {
            require(++ticks <= 1600, "Timed out at " + stage);
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getSimpleName()
                    .equals("GSGuiMainMenu"))) {
                launched = true;
                mc.gameSettings.clouds = false;
                mc.launchIntegratedServer(
                    "projector-pinned-grid-" + System.currentTimeMillis(),
                    "Pinned Projector Grid QA",
                    new WorldSettings(4L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            switch (stage) {
                case OPEN -> {
                    var plan = ProjectorClient.plan;
                    if (plan == null || plan.machine != machine || plan.x != x || plan.z != z) return;
                    ProjectorClient.open(0);
                    stage = Stage.BATCH_TAB;
                }
                case BATCH_TAB -> {
                    var gui = fullGui();
                    if (gui == null) return;
                    selectPage(gui, ProjectorGui.BATCH);
                    stage = Stage.SEED_BATCH_GRID;
                }
                case SEED_BATCH_GRID -> {
                    var gui = fullGui();
                    if (gui == null || gui.structure() == null
                        || gui.structure()
                            .isEmpty())
                        return;
                    pinButton(gui);
                    for (var axis : AXES) {
                        setNumber(gui, "batch_count_" + axis.ordinal(), axis == ForgeDirection.EAST ? 0 : 1);
                        setNumber(gui, "batch_gap_" + axis.ordinal(), 2);
                        require(gui.working.batch.gap(axis) == 2, "Full GUI gap edit did not apply for " + axis);
                    }
                    require(gui.working.batch.machines() == 4, "Full GUI directional edits did not apply");
                    stage = Stage.CHECK_TOOLBAR;
                }
                case CHECK_TOOLBAR -> {
                    var gui = fullGui();
                    require(gui != null, "Projector GUI closed while visiting tabs");
                    var button = pinButton(gui);
                    if (toolbarPage < OTHER_PAGES.length) {
                        selectPage(gui, OTHER_PAGES[toolbarPage++]);
                        return;
                    }
                    require(!gui.working.batchProjection, "Fixture did not leave the batch projection page");
                    button.onMousePressed(0);
                    stage = Stage.PAINT_EDITOR;
                }
                case PAINT_EDITOR -> {
                    if (mc.currentScreen != null) return;
                    require(
                        ProjectorClient.plan != null && ProjectorClient.plan.batchProjection,
                        "Batch draft lost on pin");
                    holdEditorKey(true);
                    stage = Stage.EDIT;
                }
                case EDIT -> {
                    var editor = pinnedEditor();
                    if (editor == null) return;
                    require(!mc.currentScreen.doesGuiPauseGame(), "Pinned editor pauses the game");
                    setNumber(editor, PINNED + "position_x", x + 1);
                    setNumber(editor, PINNED + "copies_x", 3);
                    setNumber(editor, PINNED + "copies_z", -2);
                    setNumber(editor, PINNED + "gap_x", 3);
                    setNumber(editor, PINNED + "gap_y", 4);
                    require(
                        ProjectorClient.plan.batch.copyHatches,
                        "Projector plan without a mapping flag did not default to on");
                    var mapping = WidgetTree.findFirstWithName(editor, PINNED + "copy_hatches", AE2Button.class);
                    require(mapping != null && mapping.areAncestorsEnabled(), "Missing pinned hatch mapping switch");
                    mapping.onMousePressed(0);
                    require(!ProjectorClient.plan.batch.copyHatches, "Pinned hatch mapping switch did not turn off");
                    require(
                        hasEditedGrid(ProjectorClient.plan),
                        "HUD grid edits did not apply: " + describeGrid(ProjectorClient.plan));
                    mc.ingameGUI.getChatGUI()
                        .clearChatMessages();
                    captureAfterTick = ticks + 12;
                    stage = Stage.VERIFY_EDITOR;
                }
                case VERIFY_EDITOR -> {
                    if (!editorCaptured) return;
                    var editor = pinnedEditor();
                    require(editor != null, "Pinned editor closed before the array rendered");
                    holdEditorKey(false);
                    stage = Stage.VERIFY_HUD;
                }
                case VERIFY_HUD -> {
                    if (!hudCaptured || !serverSynced) return;
                    holdEditorKey(true);
                    stage = Stage.OPEN_FULL;
                }
                case OPEN_FULL -> {
                    var editor = pinnedEditor();
                    if (editor == null) return;
                    require(
                        !ProjectorClient.plan.batch.copyHatches,
                        "Hatch mapping reset after reopening the pinned editor");
                    var full = WidgetTree.findFirstWithName(editor, PINNED + "full_gui", AE2Button.class);
                    require(full != null && full.areAncestorsEnabled(), "Missing full GUI action");
                    full.onMousePressed(0);
                    holdEditorKey(false);
                    stage = Stage.REOPEN;
                }
                case REOPEN -> {
                    var gui = fullGui();
                    if (gui == null) return;
                    require(hasEditedGrid(gui.working), "Reopened full GUI did not retain HUD grid edits");
                    selectPage(gui, ProjectorGui.BATCH);
                    captureAfterTick = ticks + 12;
                    stage = Stage.VERIFY_FULL_GUI;
                }
                case VERIFY_FULL_GUI -> {
                    if (!fullCaptured) return;
                    var gui = fullGui();
                    require(gui != null, "Full projector GUI closed before unpinning");
                    pinButton(gui).onMousePressed(0);
                    verifyUnpinAfterTick = ticks + 3;
                    stage = Stage.VERIFY_UNPIN_GUI;
                }
                case VERIFY_UNPIN_GUI -> {
                    var gui = fullGui();
                    require(gui != null, "Unpin closed the full projector GUI");
                    if (ticks < verifyUnpinAfterTick) return;
                    gui.getScreen()
                        .close();
                    stage = Stage.TRY_ALT_AFTER_UNPIN;
                }
                case TRY_ALT_AFTER_UNPIN -> {
                    if (mc.currentScreen != null) return;
                    holdEditorKey(true);
                    verifyUnpinAfterTick = ticks + 5;
                    stage = Stage.VERIFY_UNPIN;
                }
                case VERIFY_UNPIN -> {
                    require(mc.currentScreen == null, "Unpin left the HUD editor active");
                    if (ticks < verifyUnpinAfterTick) return;
                    holdEditorKey(false);
                    finish(null);
                }
                default -> {}
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void capture(TickEvent.RenderTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        try {
            if (stage == Stage.VERIFY_EDITOR && !editorCaptured && ticks >= captureAfterTick && previewMatchesGrid()) {
                screenshot(mc, EDITOR_SCREENSHOT);
                editorCaptured = true;
            } else if (stage == Stage.VERIFY_HUD && !hudCaptured && mc.currentScreen == null && serverSynced) {
                if (!previewMatchesGrid()) return;
                screenshot(mc, HUD_SCREENSHOT);
                hudCaptured = true;
            } else if (stage == Stage.VERIFY_FULL_GUI && !fullCaptured && ticks >= captureAfterTick) {
                var batch = WidgetTree.findFirstWithName(fullGui(), "generate_batch", AE2Button.class);
                require(batch != null && batch.areAncestorsEnabled(), "Batch page did not become visible");
                screenshot(mc, FULL_SCREENSHOT);
                fullCaptured = true;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void setNumber(ModularPanel gui, String name, int value) {
        var input = WidgetTree.findFirstWithName(gui, name, NumberEntryWidget.class);
        require(input != null && input.areAncestorsEnabled(), "Missing visible number entry " + name);
        var field = (TextFieldWidget) input.getChildren()
            .stream()
            .filter(TextFieldWidget.class::isInstance)
            .findFirst()
            .orElseThrow();
        field.setText(Integer.toString(value));
        field.onRemoveFocus(
            gui.getScreen()
                .getContext());
    }

    private static void selectPage(ProjectorGui gui, int page) {
        var tabs = (PageTabs) gui.getChildren()
            .stream()
            .filter(PageTabs.class::isInstance)
            .findFirst()
            .orElseThrow();
        ((ThemedButton<?>) tabs.getChildren()
            .get(page)).onMousePressed(0);
    }

    private static IconButton pinButton(ProjectorGui gui) {
        var button = WidgetTree.findFirstWithName(gui, "pin_controls", IconButton.class);
        require(
            button != null && button.areAncestorsEnabled() && button.getParent() instanceof VerticalButtonBar,
            "Missing pin button on the common projector toolbar");
        return button;
    }

    private static ProjectorGui fullGui() {
        var screen = ModularScreen.getCurrent();
        return screen != null && screen.getMainPanel() instanceof ProjectorGui gui ? gui : null;
    }

    private void holdEditorKey(boolean held) throws ReflectiveOperationException {
        holdingEditorKey = held;
        int key = Keyboard.KEY_LMENU;
        KeyBuffer.set(key, held);
        // Feed the isolated client's key queue without posting Windows input.
        var keyboard = Class.forName("org.lwjglx.input.Keyboard");
        var eventClass = Class.forName("org.lwjglx.input.Keyboard$KeyEvent");
        var stateClass = Class.forName("org.lwjglx.input.Keyboard$KeyState");
        var state = stateClass.getField(held ? "PRESS" : "RELEASE")
            .get(null);
        var event = eventClass.getConstructor(int.class, int.class, int.class, stateClass, long.class)
            .newInstance(key, key, 0, state, System.nanoTime());
        keyboard.getMethod("addRawKeyEvent", eventClass)
            .invoke(null, event);
    }

    private static ModularPanel pinnedEditor() {
        var screen = ModularScreen.getCurrent();
        if (screen == null) return null;
        var panel = screen.getMainPanel();
        return WidgetTree.findFirstWithName(panel, PINNED + "copies_x", NumberEntryWidget.class) != null ? panel : null;
    }

    private static String describeGrid(ProjectorPlan plan) {
        if (plan == null) return "no plan";
        var description = new StringBuilder().append("x=")
            .append(plan.x)
            .append(", copies=")
            .append(plan.batch.machines())
            .append(", copy hatches=")
            .append(plan.batch.copyHatches)
            .append(", grid");
        for (var axis : AXES) {
            description.append(' ')
                .append(axis)
                .append(' ')
                .append(plan.batch.additional(axis.getOpposite()))
                .append('/')
                .append(plan.batch.additional(axis))
                .append(" gap ")
                .append(plan.batch.gap(axis));
        }
        return description.toString();
    }

    private boolean hasEditedGrid(ProjectorPlan plan) {
        return plan != null && plan.batchProjection
            && !plan.batch.copyHatches
            && plan.buildStyle == StructureDeploymentStyle.SINGULARITY
            && plan.x == x + 1
            && plan.batch.machines() == 12
            && plan.batch.additional(ForgeDirection.EAST) == 2
            && plan.batch.additional(ForgeDirection.WEST) == 0
            && plan.batch.additional(ForgeDirection.SOUTH) == 0
            && plan.batch.additional(ForgeDirection.NORTH) == 1
            && plan.batch.gap(ForgeDirection.EAST) == 3
            && plan.batch.gap(ForgeDirection.UP) == 4
            && plan.batch.gap(ForgeDirection.SOUTH) == 2;
    }

    private boolean previewMatchesGrid() throws ReflectiveOperationException {
        if (!ProjectorClient.visible || ProjectorClient.structure == null || ProjectorClient.x != x + 1) return false;
        var field = ProjectorClient.class.getDeclaredField("projection");
        field.setAccessible(true);
        if (!(field.get(null) instanceof ProjectorStructure array) || array == ProjectorClient.structure) return false;
        var outlines = array.getClass()
            .getDeclaredField("copyBounds");
        outlines.setAccessible(true);
        if (((int[][]) outlines.get(array)).length != ProjectorClient.plan.batch.machines()) return false;
        var facing = ProjectorClient.facing;
        int[] one = ProjectorClient.structure.worldBounds(facing);
        int[] all = array.worldBounds(facing);
        var plan = ProjectorClient.plan;
        for (int i = 0; i < AXES.length; i++) {
            var axis = AXES[i];
            int span = one[i + 3] - one[i] + 1;
            int step = span + plan.batch.gap(axis);
            if (all[i] != one[i] - plan.batch.additional(axis.getOpposite()) * step
                || all[i + 3] != one[i + 3] + plan.batch.additional(axis) * step) return false;
        }
        return true;
    }

    private static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        require(Files.isRegularFile(Path.of("screenshots", name)), "Missing screenshot " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Throwable failure) {
        try {
            var trace = new StringWriter();
            if (failure != null) failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(Path.of("result.txt"), failure == null ? "PASS" : "FAIL " + trace);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        holdingEditorKey = false;
        KeyBuffer.set(Keyboard.KEY_LMENU, false);
        done = true;
    }
}
