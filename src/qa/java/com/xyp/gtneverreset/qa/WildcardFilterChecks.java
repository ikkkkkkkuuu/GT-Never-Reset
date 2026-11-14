package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.xyp.gtneverreset.common.gui.WildcardPatternGui.DropWidget;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardExpansion;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials.Property;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.gui.widget.ItemStackSlot;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gtPlusPlus.core.material.Material;

@Mod(modid = "wildcardfilterqa", name = "Wildcard Filter QA", version = "1", dependencies = "after:gtneverreset")
public final class WildcardFilterChecks {

    private boolean launched, initialized, opened, finished;
    private int ticks, serverTicks, stage, frames;
    private volatile int request, acknowledged;
    private volatile Throwable failure;
    private ItemStack sample;
    private String sampleName;
    private ModularScreen previousScreen;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
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
                checkModel();
                ItemStack stack = GTNRItemList.WildcardPattern.get(1);
                WildcardPatternGenerator.markAsWildcard(stack);
                WildcardModelState
                    .setFilters(stack, List.of(PropertyFilterComponent.empty(), SubTagFilterComponent.empty()));
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, stack);
                player.inventory.markDirty();
            }
            if (!opened && ++serverTicks > 20) {
                opened = true;
                open(player);
            }
            if (request == 1 && acknowledged == 0) {
                var filters = WildcardModelState.getFilters(player.getHeldItem());
                var property = (PropertyFilterComponent) filters.get(0);
                var subTag = (SubTagFilterComponent) filters.get(1);
                if (property.getProperty() == Property.GTPlusPlus && sampleName.equals(property.getExampleName())
                    && sampleName.equals(subTag.getExampleName())) {
                    require(subTag.getSubTag() == null, "GT++ has no invented SubTag");
                    require(property.test(sampleName) && !property.test("Iron"), "server uses GT++ whitelist");
                    acknowledged = 1;
                }
            }
            if (request == 2 && acknowledged == 1) {
                open(player);
                acknowledged = 2;
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void checkModel() {
        for (String name : Material.mMaterialsByName.keySet()
            .stream()
            .sorted()
            .toList()) {
            if (WildcardMaterials.isRealMaterial(WildcardMaterials.findByName(name))) continue;
            ItemStack dust = WildcardMaterials.makePrefixStack(OrePrefixes.dust, name, 1);
            if (dust != null && WildcardMaterials.makePrefixStack(OrePrefixes.ingot, name, 1) != null) {
                sample = dust;
                sampleName = name;
                break;
            }
        }
        require(sample != null, "real GT++ dust/ingot sample found: " + sampleName);
        require(
            sampleName.equals(WildcardMaterials.parseItem(sample).materialName),
            "GT++ ore material name survives parsing");
        require(
            WildcardMaterials.propertiesOf(sampleName)
                .containsAll(List.of(Property.Dust, Property.Ingot, Property.GTPlusPlus)),
            "GT++ offers registered forms and origin");
        var filter = new PropertyFilterComponent(Property.GTPlusPlus, null, true);
        filter.setExampleName(sampleName);
        require(
            filter.test(sampleName) && !filter.test("Iron") && !filter.test("NotAGtPlusPlusMaterial"),
            "origin whitelist rejects GT and unknown names");
        filter.setWhitelist(false);
        require(!filter.test(sampleName) && filter.test("Iron"), "origin blacklist excludes GT++");
        filter = PropertyFilterComponent.readData(filter.writeData());
        require(
            filter.getProperty() == Property.GTPlusPlus && sampleName.equals(filter.getExampleName())
                && !filter.isWhitelist(),
            "NBT preserves origin, sample and blacklist");
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setString("Property", "INGOT");
        legacy.setString("Example", "Iron");
        var old = PropertyFilterComponent.readData(legacy);
        require(old.getExample() == Materials.Iron && old.test("Iron"), "old GT example remains compatible");
        filter.setWhitelist(true);
        var expanded = WildcardExpansion.expand(
            List.of(new PrefixIOComponent(OrePrefixes.dust, 1)),
            List.of(new PrefixIOComponent(OrePrefixes.ingot, 1)),
            List.of(filter));
        require(
            !expanded.isEmpty() && expanded.stream()
                .anyMatch(e -> sampleName.equals(e.materialName)),
            "GT++ material generates a wildcard pattern");
        require(
            expanded.stream()
                .allMatch(e -> Property.GTPlusPlus.test(e.materialName)),
            "expanded whitelist contains only GT++ material axes");
    }

    private static void open(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItem();
        stack.getItem()
            .onItemRightClick(stack, player.worldObj, player);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "wildcard-" + System.currentTimeMillis(),
                    "Wildcard Filter QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server verification failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("timeout at stage " + stage);
            ModularScreen screen = ModularScreen.getCurrent();
            if (screen == null) return;
            var panel = screen.getMainPanel();
            var selectors = find(panel, ScrollingDropdown.class);
            if (selectors.size() != 2) return;
            ScrollingDropdown property = selectors.get(0);
            if (stage == 0 && ++frames > 10) {
                find(panel, PageButton.class).get(3)
                    .onMousePressed(0);
                require(
                    property.getCandidates()
                        .contains(Property.GTPlusPlus.displayName()),
                    "origin entry available without dropping an item");
                var drops = find(panel, DropWidget.class);
                require(drops.size() == 2, "two material filter drop targets");
                for (DropWidget drop : drops)
                    require(drop.handleDragAndDrop(sample.copy(), 0), "NEI ghost-drop callback accepts GT++");
                require(
                    property.getCandidates()
                        .contains("ingot"),
                    "GT++ drag populates form dropdown");
                require(
                    selectors.get(1)
                        .getCandidates()
                        .size() == 1,
                    "GT++ flag dropdown shows no flags");
                Method display = ItemStackSlot.class.getDeclaredMethod("displayStack");
                display.setAccessible(true);
                for (DropWidget drop : drops)
                    require(display.invoke(drop) != null, "GT++ filter example renders an item");
                property.onMousePressed(0);
                stage = 1;
                frames = 0;
            } else if (stage == 1 && ++frames > 8) {
                shot("gtplusplus-dropdown.png");
                Method menu = AbstractMenuButton.class.getDeclaredMethod("getMenu");
                menu.setAccessible(true);
                var buttons = find((IWidget) menu.invoke(property), ButtonWidget.class);
                buttons.get(
                    property.getCandidates()
                        .indexOf(Property.GTPlusPlus.displayName()))
                    .onMousePressed(0);
                require(
                    Property.GTPlusPlus.displayName()
                        .equals(property.getSelectedValue()),
                    "selecting origin updates dropdown");
                List<ButtonWidget> saves = new ArrayList<>();
                for (ButtonWidget button : find(panel, ButtonWidget.class)) {
                    if (button.getArea().width == 30 && button.getArea().height == 20) saves.add(button);
                }
                saves.get(saves.size() - 1)
                    .onMousePressed(0);
                request = 1;
                stage = 2;
            } else if (stage == 2 && acknowledged == 1) {
                shot("gtplusplus-saved.png");
                previousScreen = screen;
                request = 2;
                stage = 3;
                frames = 0;
            } else if (stage == 3 && acknowledged == 2 && screen != previousScreen && ++frames > 10) {
                find(panel, PageButton.class).get(3)
                    .onMousePressed(0);
                require(
                    Property.GTPlusPlus.displayName()
                        .equals(property.getSelectedValue()),
                    "reopened GUI retains GT++ selection");
                require(
                    property.getCandidates()
                        .contains("ingot"),
                    "reopened GUI retains GT++ example");
                stage = 4;
                frames = 0;
            } else if (stage == 4 && ++frames > 8) {
                shot("gtplusplus-reopened.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static <T> List<T> find(IWidget parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(parent)) result.add(type.cast(parent));
        for (IWidget child : parent.getChildren()) result.addAll(find(child, type));
        return result;
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("[wildcard-filter-qa] PASS " + message);
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
        }, "wildcard-filter-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
