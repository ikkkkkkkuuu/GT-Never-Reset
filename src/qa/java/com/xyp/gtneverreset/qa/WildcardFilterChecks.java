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
import com.glodblock.github.common.item.ItemFluidDrop;
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

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.state.MaterialState;

@Mod(modid = "wildcardfilterqa", name = "Wildcard Filter QA", version = "1", dependencies = "after:gtneverreset")
public final class WildcardFilterChecks {

    private boolean launched, initialized, opened, finished;
    private int ticks, serverTicks, stage, frames;
    private volatile int request, acknowledged;
    private volatile Throwable failure;
    private ItemStack sample;
    private String sampleName;
    private ItemStack bwSample;
    private String bwName;
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
                checkNativeMaterials();
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
            if (request == 2 && acknowledged == 1 || request == 4 && acknowledged == 3) {
                open(player);
                acknowledged = request;
            }
            if (request == 3 && acknowledged == 2) {
                var filters = WildcardModelState.getFilters(player.getHeldItem());
                var property = (PropertyFilterComponent) filters.get(0);
                var subTag = (SubTagFilterComponent) filters.get(1);
                if (property.getProperty() == Property.BlastFurnace && subTag.getSubTag() == SubTag.METAL) {
                    require(
                        property.getExampleName()
                            .isEmpty()
                            && subTag.getExampleName()
                                .isEmpty(),
                        "cleared examples stay cleared on server");
                    require(
                        property.test("TungstenSteel") == Materials.TungstenSteel.mBlastFurnaceRequired,
                        "native property reaches server");
                    require(
                        subTag.test("Iron") && !subTag.test(sampleName),
                        "native tag reaches server without GT++ fake tags");
                    acknowledged = 3;
                }
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

    private void checkNativeMaterials() {
        require(
            WildcardMaterials.findOreMaterial("Iron") == Materials.Iron,
            "GT identity has priority over same-name addon entries");
        require(
            WildcardMaterials.findOreMaterial(sampleName) instanceof Material,
            "GT++ resolves through IOreMaterial");
        require(
            Property.BlastFurnace.test("TungstenSteel") == Materials.TungstenSteel.mBlastFurnaceRequired,
            "GT blast-furnace metadata is used");
        require(
            Property.Radioactive.test("Uranium235") && !Property.Radioactive.test("Iron"),
            "GT radioactivity follows native enchantments");
        require(
            WildcardMaterials.findProperty("METAL") == Property.Metal
                && WildcardMaterials.findProperty("INGOT") == Property.Ingot,
            "legacy property IDs do not change through display aliases");
        for (Property property : WildcardMaterials.propertyChoices()
            .values()) {
            require(
                WildcardMaterials.findProperty(property.displayName()) == property,
                "localized property round trip: " + property);
            var filter = new PropertyFilterComponent(property, null, false);
            var loaded = PropertyFilterComponent.readData(filter.writeData());
            require(loaded.getProperty() == property && !loaded.isWhitelist(), "stable property NBT: " + property);
        }
        int gtppCount = 0, gtppGases = 0;
        for (Material material : Material.mMaterialsByName.values()) {
            String name = material.getInternalName();
            if (WildcardMaterials.findOreMaterial(name) != material) continue;
            if (Property.BlastFurnace.test(name) != material.requiresBlastFurnace()
                || Property.Radioactive.test(name) != material.isRadioactive)
                throw new AssertionError("GT++ native stats mismatch: " + name);
            if (!WildcardMaterials.subTagsOf(name)
                .isEmpty()) throw new AssertionError("invented GT++ tags: " + name);
            if ((material.getState() == MaterialState.GAS || material.getState() == MaterialState.PURE_GAS)
                && material.getFluid() != null) {
                require(Property.Gas.test(name), "GT++ native gas property: " + name);
                ItemStack drop = WildcardMaterials.makeFluidStack(FluidState.GAS, name, 1);
                require(
                    drop != null && ItemFluidDrop.getFluidStack(drop)
                        .getFluid() == material.getFluid(),
                    "GT++ gas uses registered fluid identity: " + name);
                gtppGases++;
            }
            gtppCount++;
        }
        require(gtppCount > 0 && gtppGases > 0, "GT++ registry metadata checked: " + gtppCount);
        int bwCount = 0, tagged = 0, toxic = 0, electrolytic = 0, centrifuged = 0;
        for (Werkstoff material : Werkstoff.werkstoffHashSet) {
            String name = material.getVarName();
            if (WildcardMaterials.findOreMaterial(name) != material) continue;
            if (Property.BlastFurnace.test(name) != material.getStats()
                .isBlastFurnace() || Property.Radioactive.test(name)
                    != material.getStats()
                        .isRadioactive()
                || Property.Toxic.test(name) != material.getStats()
                    .isToxic()
                || Property.Electrolysis.test(name) != material.getStats()
                    .isElektrolysis()
                || Property.Centrifuge.test(name) != material.getStats()
                    .isCentrifuge())
                throw new AssertionError("BW native stats mismatch: " + name);
            Property type = Property.valueOf("BW" + switch (material.getType()) {
                case MATERIAL -> "Material";
                case COMPOUND -> "Compound";
                case MIXTURE -> "Mixture";
                case BIOLOGICAL -> "Biological";
                case ELEMENT -> "Element";
                case ISOTOPE -> "Isotope";
                case UNDEFINED -> "Undefined";
            });
            if (!type.test(name)) throw new AssertionError("BW type mismatch: " + name);
            for (SubTag tag : WildcardMaterials.subTagChoices()) {
                if (material.contains(tag)) {
                    if (!WildcardMaterials.hasSubTag(name, tag))
                        throw new AssertionError("lost native BW tag: " + name + "/" + tag.mName);
                    var filter = SubTagFilterComponent.readData(new SubTagFilterComponent(tag, null, true).writeData());
                    if (!filter.test(name)) throw new AssertionError("BW tag NBT/filter mismatch: " + name);
                    tagged++;
                }
            }
            if (material.getStats()
                .isToxic()) toxic++;
            if (material.getStats()
                .isElektrolysis()) electrolytic++;
            if (material.getStats()
                .isCentrifuge()) centrifuged++;
            if (bwSample == null && !WildcardMaterials.subTagsOf(name)
                .isEmpty()) {
                ItemStack dust = WildcardMaterials.makePrefixStack(OrePrefixes.dust, name, 1);
                if (dust != null) {
                    bwSample = dust;
                    bwName = name;
                }
            }
            bwCount++;
        }
        require(
            bwCount > 0 && tagged > 0 && toxic > 0 && electrolytic > 0 && centrifuged > 0 && bwSample != null,
            "BW native registry checked: " + bwCount
                + ", tags "
                + tagged
                + ", toxic "
                + toxic
                + ", electrolysis "
                + electrolytic
                + ", centrifuge "
                + centrifuged);
        require(
            WildcardMaterials.subTagChoices()
                .contains(WerkstoffLoader.NOBLE_GAS),
            "BW tags are in native global choices");
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
                require(
                    property.getCandidates()
                        .containsAll(
                            List.of(
                                Property.BlastFurnace.displayName(),
                                Property.Radioactive.displayName(),
                                Property.BWCompound.displayName(),
                                Property.Toxic.displayName(),
                                Property.Electrolysis.displayName())),
                    "native properties available without an example");
                require(
                    selectors.get(1)
                        .getCandidates()
                        .containsAll(
                            List.of(SubTag.METAL.mName, SubTag.NO_SMELTING.mName, WerkstoffLoader.NOBLE_GAS.mName)),
                    "GT and BW tags available without an example");
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
                var drops = find(panel, DropWidget.class);
                for (DropWidget drop : drops) drop.handleDragAndDrop(bwSample.copy(), 0);
                require(
                    property.getCandidates()
                        .containsAll(
                            WildcardMaterials.propertiesOf(bwName)
                                .stream()
                                .map(Property::displayName)
                                .toList()),
                    "BW drag offers native properties");
                require(
                    selectors.get(1)
                        .getCandidates()
                        .equals(
                            WildcardMaterials.subTagsOf(bwName)
                                .stream()
                                .map(tag -> tag.mName)
                                .toList()),
                    "BW drag offers native tags");
                stage = 5;
                frames = 0;
            } else if (stage == 5 && ++frames > 8) {
                shot("bartworks-native-filter.png");
                mc.thePlayer.inventory.setItemStack(null);
                for (DropWidget drop : find(panel, DropWidget.class)) drop.onMousePressed(0);
                require(
                    property.getCandidates()
                        .contains(Property.BlastFurnace.displayName())
                        && selectors.get(1)
                            .getCandidates()
                            .contains(SubTag.METAL.mName),
                    "clearing examples restores global choices");
                property.onMousePressed(0);
                stage = 6;
                frames = 0;
            } else if (stage == 6 && ++frames > 8) {
                choose(property, Property.BlastFurnace.displayName());
                selectors.get(1)
                    .onMousePressed(0);
                stage = 7;
                frames = 0;
            } else if (stage == 7 && ++frames > 8) {
                choose(selectors.get(1), SubTag.METAL.mName);
                List<ButtonWidget> saves = new ArrayList<>();
                for (ButtonWidget button : find(panel, ButtonWidget.class)) {
                    if (button.getArea().width == 30 && button.getArea().height == 20) saves.add(button);
                }
                saves.get(saves.size() - 1)
                    .onMousePressed(0);
                request = 3;
                stage = 8;
            } else if (stage == 8 && acknowledged == 3) {
                shot("native-filter-saved.png");
                previousScreen = screen;
                request = 4;
                stage = 9;
                frames = 0;
            } else if (stage == 9 && acknowledged == 4 && screen != previousScreen && ++frames > 10) {
                find(panel, PageButton.class).get(3)
                    .onMousePressed(0);
                require(
                    property.getSelectedValue()
                        .equals(Property.BlastFurnace.displayName())
                        && selectors.get(1)
                            .getSelectedValue()
                            .equals(SubTag.METAL.mName),
                    "native property and tag survive reopening");
                stage = 10;
                frames = 0;
            } else if (stage == 10 && ++frames > 8) {
                shot("native-filter-reopened.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void choose(ScrollingDropdown selector, String value) throws Exception {
        Method menu = AbstractMenuButton.class.getDeclaredMethod("getMenu");
        menu.setAccessible(true);
        var buttons = find((IWidget) menu.invoke(selector), ButtonWidget.class);
        buttons.get(
            selector.getCandidates()
                .indexOf(value))
            .onMousePressed(0);
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
