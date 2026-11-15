package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.glodblock.github.common.item.ItemFluidDrop;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardExpansion;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardMaterials;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardRecipeTemplate;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.FluidIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.SimpleIOComponent;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.state.MaterialState;

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
            checkSolidifierImport(player);
            Files.writeString(result, "PASS\n");
        } catch (Throwable error) {
            Files.writeString(result, "FAIL " + error + "\n");
            throw new AssertionError("wildcard dedicated-server regression", error);
        } finally {
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    private static void checkSolidifierImport(FakePlayer player) {
        ItemStack mold = ItemList.Shape_Mold_Plate.get(0);
        Map<String, GTRecipe> recipes = new LinkedHashMap<>();
        for (GTRecipe recipe : RecipeMaps.fluidSolidifierRecipes.getAllRecipes()) {
            if (recipe.mInputs.length != 1 || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize != 0
                || !GTUtility.areStacksEqual(mold, recipe.mInputs[0])
                || recipe.mFluidInputs.length != 1
                || recipe.mFluidInputs[0] == null
                || recipe.mFluidInputs[0].amount != 144
                || recipe.mOutputs.length != 1
                || recipe.mOutputs[0] == null
                || recipe.mOutputs[0].stackSize != 1) continue;
            var material = WildcardMaterials.parseItem(recipe.mOutputs[0]);
            if (material.prefix == OrePrefixes.plate
                && WildcardMaterials.Property.GTPlusPlus.test(material.materialName)
                && WildcardMaterials.findOreMaterial(material.materialName) instanceof Material nativeMaterial
                && (nativeMaterial.getState() == MaterialState.SOLID || nativeMaterial.getState() == MaterialState.ORE)
                && !WildcardMaterials.isRealMaterial(WildcardMaterials.findByName(material.materialName)))
                recipes.put(material.materialName, recipe);
        }
        require(recipes.size() > 1, "multiple real GT++ plate solidifier recipes available");
        var source = recipes.entrySet()
            .iterator()
            .next();
        ItemStack fluid = ItemFluidDrop.newStack(source.getValue().mFluidInputs[0].copy());
        var template = WildcardRecipeTemplate.create(List.of(mold, fluid), List.of(source.getValue().mOutputs[0]));
        require(
            template.getInputs()
                .get(0) instanceof SimpleIOComponent,
            "plate mold stays fixed");
        require(
            template.getInputs()
                .get(1) instanceof FluidIOComponent component && component.getState() == FluidState.MOLTEN
                && component.getAmount() == 144,
            "imported GT++ molten fluid becomes a material variable");
        require(
            template.getOutputs()
                .get(0) instanceof PrefixIOComponent,
            "plate output becomes a material variable");
        var filters = List.of(new PropertyFilterComponent(WildcardMaterials.Property.GTPlusPlus, null, true));
        ItemStack pattern = GTNRItemList.WildcardPattern.get(1);
        WildcardPatternGenerator.markAsWildcard(pattern);
        WildcardModelState.setInputs(pattern, template.getInputs());
        WildcardModelState.setOutputs(pattern, template.getOutputs());
        WildcardModelState.setFilters(pattern, new ArrayList<>(filters));
        PatternRecipeMap.write(pattern, RecipeMaps.fluidSolidifierRecipes.unlocalizedName);
        var expanded = WildcardExpansion.expand(
            WildcardModelState.getInputs(pattern),
            WildcardModelState.getOutputs(pattern),
            WildcardModelState.getFilters(pattern));
        int checked = 0;
        for (var entry : expanded) {
            require(WildcardMaterials.Property.GTPlusPlus.test(entry.materialName), "GT++ whitelist retained");
            GTRecipe recipe = recipes.get(entry.materialName);
            if (recipe == null) continue;
            require(GTUtility.areStacksEqual(mold, entry.inputs.get(0)), "same plate mold: " + entry.materialName);
            var actualFluid = ItemFluidDrop.getFluidStack(entry.inputs.get(1));
            require(
                actualFluid != null && actualFluid.isFluidEqual(recipe.mFluidInputs[0])
                    && actualFluid.amount == recipe.mFluidInputs[0].amount,
                "matching molten fluid: " + entry.materialName);
            require(
                GTUtility.areStacksEqual(recipe.mOutputs[0], entry.outputs.get(0)),
                "matching plate output: " + entry.materialName);
            checked++;
        }
        require(
            checked == recipes.size(),
            "all real GT++ plate recipes expand, including other materials; source=" + source.getKey()
                + "; missing="
                + recipes.keySet()
                    .stream()
                    .filter(
                        name -> expanded.stream()
                            .noneMatch(entry -> name.equals(entry.materialName)))
                    .toList());
        int virtualMolds = 0;
        for (var details : WildcardPatternGenerator.generateAllDetails(pattern, player.worldObj)) {
            String name = WildcardMaterials.parseItem(details.getOutputs()[0].getItemStack()).materialName;
            if (!recipes.containsKey(name)) continue;
            ItemStack[] molds = CircuitPatternCodec.molds(details);
            require(
                molds.length == 1 && molds[0].stackSize == 0 && GTUtility.areStacksEqual(mold, molds[0]),
                "non-consumable mold resolved for " + name);
            require(
                RecipeMaps.fluidSolidifierRecipes.unlocalizedName.equals(PatternRecipeMap.name(details.getPattern())),
                "generated pattern retains solidifier recipe map");
            var runtime = CircuitPatternCodec.runtime(details.getPattern(), player.worldObj, details, molds);
            require(runtime != null && runtime.getAEInputs().length == 1, "runtime requests only fluid: " + name);
            virtualMolds++;
        }
        require(virtualMolds == recipes.size(), "molds resolve for every real GT++ plate recipe");
        ItemStack tagged = fluid.copy();
        tagged.getTagCompound()
            .setTag("FluidTag", new NBTTagCompound());
        var taggedTemplate = WildcardRecipeTemplate
            .create(List.of(mold, tagged), List.of(source.getValue().mOutputs[0]));
        require(
            taggedTemplate.getInputs()
                .get(1) instanceof SimpleIOComponent,
            "tagged fluid stays exact");
        var ironTemplate = WildcardRecipeTemplate.create(
            List.of(mold, ItemFluidDrop.newStack(Materials.Iron.getMolten(144))),
            List.of(Materials.Iron.getPlates(1)));
        require(
            ironTemplate.getInputs()
                .get(1) instanceof FluidIOComponent,
            "GT molten import still expands");
        System.out.println(
            "[wildcard-server-qa] PASS GT++ solidifier import, expansion and virtual molds: " + checked
                + " materials, source="
                + source.getKey());
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
