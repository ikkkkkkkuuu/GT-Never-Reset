package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.tileentities.machines.multi.MTEIndustrialPackager;

/** Exercises the real synchronized hatch GUI on independent client/server hatch instances. */
@Mod(
    modid = "crossrecipeselectorqa",
    name = "Cross Recipe Selector QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class CrossRecipeSelectorChecks {

    private static final String LONG_NAME = "超级跨配方类型控制器的超长配方池名称 / Extended Industrial Recipe Processing Category";
    private SimpleGuiFactory factory;
    private CrossRecipeMEPatternBuffer serverHatch;
    private MTEMultiBlockBase controller;
    private RecipeMap<?> longMap;
    private boolean launched, initialized, opened, finished;
    private int serverTicks, ticks, stage, frames;
    private volatile int request;
    private volatile int acknowledged;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        factory = GuiFactories.createSimple("gtnr:cross_recipe_selector_qa", (data, sync, settings) -> {
            var hatch = data.getWorld().isRemote ? hatch(data.getWorld()) : serverHatch;
            return hatch.buildUI(new PosGuiData(data.getPlayer(), 0, 64, 0), sync, settings);
        });
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        try {
            if (!initialized) {
                initialized = true;
                setup(player);
            }
            if (!opened && ++serverTicks >= 20) {
                opened = true;
                factory.open(player);
            }
            if (request == 1 && acknowledged < 1
                && serverHatch.getTerminalRecipeMap()
                    .equals(RecipeMaps.packagerRecipes.unlocalizedName)) {
                require(
                    serverHatch.getRawName()
                        .equals(RecipeMaps.packagerRecipes.getDefaultRecipeCategory().unlocalizedName),
                    "client selection changes Interface Terminal raw name");
                require(controller.getMachineMode() == 0, "display-name selection does not change controller mode");
                acknowledged = 1;
            } else if (request == 2 && acknowledged < 2) {
                serverHatch.removeWatcher(controller);
                controller.mDualInputHatches.clear();
                controller = new LongNameController(longMap);
                BaseMetaTileEntity tile = new BaseMetaTileEntity();
                tile.setWorldObj(player.worldObj);
                tile.setMetaTileEntity(controller);
                controller.setBaseMetaTileEntity(tile);
                attach(serverHatch, controller);
                require(
                    serverHatch.getControllerRecipeMaps()
                        .size() == 3,
                    "replacement controller exposes exactly three recipe types");
                acknowledged = 2;
            } else if (request == 3 && acknowledged < 3
                && serverHatch.getTerminalRecipeMap()
                    .equals(LONG_NAME)) {
                        require(
                            serverHatch.getRawName()
                                .equals(LONG_NAME),
                            "long recipe name reaches the terminal naming contract");
                        acknowledged = 3;
                    } else
                if (request == 4 && acknowledged < 4) {
                    controller.mDualInputHatches.clear();
                    require(
                        serverHatch.getControllerRecipeMaps()
                            .isEmpty(),
                        "stale watcher without membership supplies no options");
                    acknowledged = 4;
                } else if (request >= 5 && acknowledged < request) {
                    if (!controller.mDualInputHatches.contains(serverHatch))
                        controller.mDualInputHatches.add(serverHatch);
                    require(
                        serverHatch.getTerminalRecipeMap()
                            .equals(LONG_NAME),
                        "reconnecting restores saved selection");
                    factory.open(player);
                    acknowledged = request;
                }
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void setup(EntityPlayerMP player) {
        serverHatch = hatch(player.worldObj);
        for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
            if (GregTechAPI.METATILEENTITIES[id] == null
                || GregTechAPI.METATILEENTITIES[id].getClass() != MTEIndustrialPackager.class) continue;
            BaseMetaTileEntity tile = new BaseMetaTileEntity();
            tile.setWorldObj(player.worldObj);
            tile.setInitialValuesAsNBT(null, (short) id);
            controller = (MTEMultiBlockBase) tile.getMetaTileEntity();
            break;
        }
        require(controller != null, "native packager exists");
        attach(serverHatch, controller);
        require(
            serverHatch.getControllerRecipeMaps()
                .size() == 2,
            "native controller supplies exactly two entries");
        serverHatch.setCustomName("old custom name");
        serverHatch.setTerminalRecipeMap(RecipeMaps.unpackagerRecipes.unlocalizedName);
        require(!serverHatch.hasCustomName(), "explicit name selection replaces manual terminal name");
        require(
            serverHatch.getRawName()
                .equals(RecipeMaps.unpackagerRecipes.getDefaultRecipeCategory().unlocalizedName),
            "terminal uses selected category key");
        serverHatch.setTerminalRecipeMap(RecipeMaps.assemblerRecipes.unlocalizedName);
        require(
            serverHatch.getTerminalRecipeMap()
                .equals(RecipeMaps.unpackagerRecipes.unlocalizedName),
            "server rejects unsupported recipe name");
        NBTTagCompound saved = new NBTTagCompound();
        serverHatch.saveNBTData(saved);
        var restored = hatch(player.worldObj);
        restored.loadNBTData(saved);
        attach(restored, controller);
        require(
            restored.getTerminalRecipeMap()
                .equals(RecipeMaps.unpackagerRecipes.unlocalizedName),
            "selection survives NBT reload");
        controller.mDualInputHatches.remove(restored);
        longMap = RecipeMapBuilder.of(LONG_NAME)
            .maxIO(1, 1, 0, 0)
            .build();
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "selector-" + System.currentTimeMillis(),
                    "Cross Recipe Selector QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server check failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("Selector QA timeout at stage " + stage);
            ModularScreen screen = ModularScreen.getCurrent();
            if (screen == null) return;
            ScrollingDropdown selector = findSelector(screen.getMainPanel());
            if (selector == null) return;
            if (stage == 0 && selector.getCandidates()
                .size() == 2 && ++frames > 10) {
                require(
                    selector.getArea().width == 162 && selector.getArea().height == 18,
                    "selector has fixed 162 by 18 dimensions");
                require(
                    selector.getSelectedValue()
                        .equals(
                            selector.getCandidates()
                                .get(1)),
                    "initial selection synchronized to independent client hatch");
                shot("selector-closed.png");
                selector.onMousePressed(0);
                next();
            } else if (stage == 1 && ++frames > 10) {
                require(selector.isOpen(), "native dropdown mouse event opens menu");
                shot("selector-open.png");
                buttons(selector).get(0)
                    .onMousePressed(0);
                request = 1;
                next();
            } else if (stage == 2 && acknowledged == 1
                && selector.getSelectedValue()
                    .equals(
                        selector.getCandidates()
                            .get(0))) {
                                require(!selector.isOpen(), "selecting an entry closes dropdown");
                                shot("selector-selected.png");
                                request = 2;
                                next();
                            } else
                if (stage == 3 && acknowledged == 2
                    && selector.getCandidates()
                        .size() == 3) {
                            selector.onMousePressed(0);
                            next();
                        } else
                    if (stage == 4 && ++frames > 10) {
                        require(
                            buttons(selector).size() == 3,
                            "updated dropdown has exactly the controller recipe count");
                        buttons(selector).get(1)
                            .onMousePressed(0);
                        request = 3;
                        next();
                    } else if (stage == 5 && acknowledged == 3
                        && selector.getSelectedValue()
                            .equals(LONG_NAME)) {
                                require(
                                    mc.fontRenderer.getStringWidth(LONG_NAME) > 158,
                                    "long name exceeds fixed text viewport");
                                shot("selector-long-start.png");
                                next();
                            } else
                        if (stage == 6 && ++frames > 50) {
                            require(selector.getArea().width == 162, "long name does not expand selector");
                            shot("selector-long-scroll.png");
                            selector.onMousePressed(0);
                            next();
                        } else if (stage == 7 && ++frames > 35) {
                            shot("selector-long-menu.png");
                            selector.closeMenu(false);
                            request = 4;
                            next();
                        } else if (stage == 8 && acknowledged == 4
                            && selector.getCandidates()
                                .isEmpty()) {
                                    selector.onMousePressed(0);
                                    require(!selector.isOpen(), "unbound selector cannot open stale options");
                                    shot("selector-unbound.png");
                                    mc.thePlayer.closeScreen();
                                    request = 5;
                                    next();
                                } else
                            if (stage == 9 && acknowledged == 5
                                && selector.getCandidates()
                                    .size() == 3
                                && ++frames > 10) {
                                    require(
                                        selector.getSelectedValue()
                                            .equals(LONG_NAME),
                                        "reopened GUI restores selection");
                                    shot("selector-reopened.png");
                                    mc.thePlayer.closeScreen();
                                    mc.gameSettings.guiScale = 1;
                                    request = 6;
                                    next();
                                } else if (stage == 10 && acknowledged == 6
                                    && selector.getCandidates()
                                        .size() == 3
                                    && ++frames > 10) {
                                        checkBounds(screen, selector);
                                        shot("selector-scale-1.png");
                                        mc.thePlayer.closeScreen();
                                        mc.gameSettings.guiScale = 3;
                                        request = 7;
                                        next();
                                    } else if (stage == 11 && acknowledged == 7
                                        && selector.getCandidates()
                                            .size() == 3
                                        && ++frames > 10) {
                                            checkBounds(screen, selector);
                                            shot("selector-scale-3.png");
                                            finish(null);
                                        }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void next() {
        stage++;
        frames = 0;
    }

    private static void checkBounds(ModularScreen screen, ScrollingDropdown selector) {
        var panel = screen.getMainPanel()
            .getArea();
        var area = selector.getArea();
        require(
            area.width == 162 && area.height == 18
                && area.x >= panel.x
                && area.y >= panel.y
                && area.x + area.width <= panel.x + panel.width
                && area.y + area.height <= panel.y + panel.height,
            "fixed selector remains inside panel at GUI scale " + Minecraft.getMinecraft().gameSettings.guiScale);
    }

    private static void attach(CrossRecipeMEPatternBuffer hatch, MTEMultiBlockBase controller) {
        controller.mDualInputHatches.add(hatch);
        hatch.addWatcher(controller);
    }

    private static CrossRecipeMEPatternBuffer hatch(World world) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CrossRecipeMEPatternBuffer.id);
        return (CrossRecipeMEPatternBuffer) tile.getMetaTileEntity();
    }

    private static ScrollingDropdown findSelector(IWidget widget) {
        if (widget instanceof ScrollingDropdown dropdown && "terminal_recipe_map".equals(widget.getName()))
            return dropdown;
        for (IWidget child : widget.getChildren()) {
            ScrollingDropdown found = findSelector(child);
            if (found != null) return found;
        }
        return null;
    }

    private static List<ButtonWidget<?>> buttons(ScrollingDropdown selector) throws Exception {
        Method menu = AbstractMenuButton.class.getDeclaredMethod("getMenu");
        menu.setAccessible(true);
        List<ButtonWidget<?>> result = new ArrayList<>();
        collectButtons((IWidget) menu.invoke(selector), result);
        return result;
    }

    private static void collectButtons(IWidget parent, List<ButtonWidget<?>> result) {
        if (parent instanceof ButtonWidget<?>button) result.add(button);
        for (IWidget child : parent.getChildren()) collectButtons(child, result);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("[cross-recipe-selector-qa] PASS " + message);
    }

    private void finish(Throwable error) {
        if (finished) return;
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception io) {
            io.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "cross-recipe-selector-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class LongNameController extends MTEIndustrialPackager {

        private final RecipeMap<?> longMap;

        private LongNameController(RecipeMap<?> longMap) {
            super("qa.long_names");
            this.longMap = longMap;
        }

        @Override
        public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
            return List.of(RecipeMaps.packagerRecipes, longMap, RecipeMaps.unpackagerRecipes);
        }
    }
}
