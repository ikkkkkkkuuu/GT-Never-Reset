package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.relauncher.Side;
import gtPlusPlus.core.material.Material;

@Mod(
    modid = "wildcardserverqa",
    name = "Wildcard dedicated server QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class WildcardServerChecks {

    @Mod.EventHandler
    public void ready(FMLServerStartedEvent event) throws Exception {
        Path result = Path.of(System.getProperty("gtnr.wildcard.server.result"));
        try {
            require(
                FMLCommonHandler.instance()
                    .getSide() == Side.SERVER,
                "physical dedicated server");
            FakePlayer player = FakePlayerFactory.getMinecraft(
                MinecraftServer.getServer()
                    .worldServerForDimension(0));
            checkPanel(player, null);
            checkPanel(player, "Iron");
            checkPanel(player, "WildcardQaMissingMaterial");
            String gtpp = Material.mMaterialsByName.values()
                .stream()
                .map(Material::getInternalName)
                .filter(name -> WildcardMaterials.findOreMaterial(name) instanceof Material)
                .findFirst()
                .orElseThrow(() -> new AssertionError("GT++ material required"));
            checkPanel(player, gtpp);
            Files.writeString(result, "PASS\n");
        } catch (Throwable error) {
            Files.writeString(result, "FAIL " + error + "\n");
            throw new AssertionError("wildcard dedicated-server regression", error);
        } finally {
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    private static void checkPanel(FakePlayer player, String example) {
        PropertyFilterComponent property = PropertyFilterComponent.empty();
        SubTagFilterComponent tag = SubTagFilterComponent.empty();
        property.setExampleName(example);
        tag.setExampleName(example);
        ItemStack stack = GTNRItemList.WildcardPattern.get(1);
        WildcardPatternGenerator.markAsWildcard(stack);
        WildcardModelState.setFilters(stack, List.of(property, tag));
        player.inventory.setInventorySlotContents(0, stack);
        NBTTagCompound before = (NBTTagCompound) stack.getTagCompound()
            .copy();
        for (int attempt = 0; attempt < 2; attempt++) {
            PlayerInventoryGuiData data = new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, 0);
            ModularSyncManager manager = new ModularSyncManager(false);
            PanelSyncManager sync = new PanelSyncManager(manager, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            PlayerInventoryGuiFactory factory = PlayerInventoryGuiFactory.INSTANCE;
            settings.defaultCanInteractWith(factory, data);
            ModularPanel panel = factory.createPanel(data, sync, settings);
            List<ScrollingDropdown> selectors = new ArrayList<>();
            collect(panel, selectors);
            require(selectors.size() == 2, "both filter selectors built: " + example);
            if (example != null && WildcardMaterials.propertiesOf(example)
                .isEmpty()) {
                require(
                    selectors.get(0)
                        .getCandidates()
                        .contains(StatCollector.translateToLocal("gtnr.gui.wildcardpattern.no_property")),
                    "no-property fallback");
            }
            if (example != null && WildcardMaterials.subTagsOf(example)
                .isEmpty()) {
                require(
                    selectors.get(1)
                        .getCandidates()
                        .equals(List.of(StatCollector.translateToLocal("gtnr.gui.wildcardpattern.no_flag"))),
                    "no-tag fallback");
            }
            require(
                selectors.get(0)
                    .getCandidates()
                    .contains(WildcardMaterials.Property.GTPlusPlus.displayName()),
                "GT++ option retained");
            WidgetTree.collectSyncValues(sync, panel);
            ModularContainer container = factory.createContainer();
            container.construct(player, manager, settings, panel.getName(), data);
            require(before.equals(stack.getTagCompound()), "opening preserves stored filters: " + example);
            container.onContainerClosed(player);
        }
        System.out.println("[wildcard-server-qa] PASS build, sync, container and reopen: " + example);
    }

    private static void collect(IWidget widget, List<ScrollingDropdown> selectors) {
        if (widget instanceof ScrollingDropdown dropdown) selectors.add(dropdown);
        for (IWidget child : widget.getChildren()) collect(child, selectors);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
