package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.StorageName;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.api.ItemInfo;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;
import ic2.api.item.ElectricItem;

@Mod(
    modid = "terminalingredientselectionqa",
    name = "Terminal Ingredient Selection QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class TerminalIngredientSelectionChecks {

    private static final int WINDOW_ID = 72;

    private volatile Phase phase = Phase.PREPARE;
    private volatile Throwable failure;
    private TileInterface provider;
    private QuickEncodingTerminalContainer serverContainer;
    private QuickEncodingTerminalGui clientGui;
    private GTNEIDefaultHandler recipe;
    private int recipeIndex;
    private PositionedStack bronzeSlot;
    private ItemStack gregtechBronze, industrialCraftBronze, chosen, plate;
    private ItemStack autoclaveCrystal, renderedEmptyCrystal, renderedChargedCrystal, energyChip;
    private GTNEIDefaultHandler laserRecipe;
    private PositionedStack laserCrystalSlot;
    private int laserRecipeIndex;
    private List<String> screenshot;
    private String screenshotName;
    private Phase afterScreenshot;
    private int networkTicks, clientTicks;
    private boolean launched, finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failure != null || !GregTechAPI.sFullLoadFinished) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            switch (phase) {
                case PREPARE -> prepareServer(player);
                case ATTACH_SERVER -> {
                    player.openContainer = serverContainer;
                    serverContainer.addCraftingToCrafters(player);
                    serverContainer.detectAndSendChanges();
                    phase = Phase.MULTI_READY;
                }
                case MULTI_SENT -> {
                    if (target(serverContainer).shouldKeepNonConsumables()) return;
                    ItemStack imported = importedBronze(serverContainer, chosen);
                    require(
                        imported != null && imported.stackSize == chosen.stackSize,
                        "server imports selected allowed bronze");
                    require(encoded(serverContainer) == null, "multiple visible variants block Shift auto-encoding");
                    report("multi server imported " + stackId(imported) + "; no encoded pattern");
                    phase = Phase.MULTI_VERIFIED;
                }
                case SINGLE_SENT -> {
                    ItemStack encoded = encoded(serverContainer);
                    if (encoded == null) return;
                    ItemStack imported = importedBronze(serverContainer, gregtechBronze);
                    require(
                        imported != null && imported.stackSize == gregtechBronze.stackSize,
                        "server imports exact GT bronze");
                    require(
                        containsEncodedInput(encoded, gregtechBronze, player),
                        "auto-encoded pattern uses exact GT bronze");
                    require(
                        !containsEncodedInput(encoded, industrialCraftBronze, player),
                        "pattern excludes hidden IC2 bronze");
                    require(
                        ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, player.worldObj)
                            .getCondensedAEOutputs()[0].getItemStackForNEI()
                                .isItemEqual(plate),
                        "auto-encoded output is real triple bronze plate");
                    report("single server imported and encoded " + stackId(imported) + " -> " + stackId(plate));
                    phase = Phase.SINGLE_VERIFIED;
                }
                case AUTOCLAVE_SENT -> {
                    ItemStack output = encodedOutput(serverContainer, autoclaveCrystal, player);
                    if (output == null) return;
                    require(output.stackSize == 1, "autoclave pattern produces one exact empty crystal");
                    autoclaveCrystal = output;
                    report("server autoclave encoded output: " + stackId(output));
                    phase = Phase.AUTOCLAVE_VERIFIED;
                }
                case LASER_EMPTY_SENT -> {
                    if (target(serverContainer).shouldKeepNonConsumables()) return;
                    require(
                        importedExactInput(serverContainer, autoclaveCrystal),
                        "laser input matches the autoclave output identity");
                    require(
                        !importedExactInput(serverContainer, renderedEmptyCrystal),
                        "laser input excludes the damage-26 display crystal");
                    require(
                        encodedOutput(serverContainer, energyChip, player) == null,
                        "visible charge alternatives still block Shift auto-encoding");
                    phase = Phase.LASER_EMPTY_IMPORTED;
                }
                case LASER_EMPTY_ENCODED -> {
                    if (encodedOutput(serverContainer, energyChip, player) == null) return;
                    require(
                        encodedExactInput(serverContainer, autoclaveCrystal, player),
                        "encoded laser input has the exact autoclave output identity");
                    report("server laser encoded input: " + stackId(autoclaveCrystal));
                    phase = Phase.LASER_EMPTY_VERIFIED;
                }
                case LASER_CHARGED_ENCODED -> {
                    if (!target(serverContainer).shouldKeepNonConsumables()) return;
                    require(
                        importedExactInput(serverContainer, renderedChargedCrystal),
                        "explicitly selected charged crystal remains in the laser draft");
                    require(
                        encodedExactInput(serverContainer, renderedChargedCrystal, player),
                        "explicitly selected charged crystal retains damage and NBT when encoded");
                    report("server laser charged input: " + stackId(renderedChargedCrystal));
                    phase = Phase.LASER_CHARGED_VERIFIED;
                }
                default -> {}
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getName()
                    .equals("galaxyspace.core.gui.GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "terminal-ingredient-selection-" + System.currentTimeMillis(),
                    "Terminal Ingredient Selection QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server fixture failed", failure);
            if (mc.thePlayer == null) {
                require(clientTicks == 0, "client disconnected at " + phase);
                return;
            }
            require(++clientTicks < 1800, "timeout at " + phase);
            switch (phase) {
                case OPEN_CLIENT -> {
                    ClientConfig.wirelessTerminal.fillSearchFromNeiRecipes = false;
                    clientGui = new QuickEncodingTerminalGui(mc.thePlayer.inventory, terminal(mc.thePlayer));
                    mc.displayGuiScreen(clientGui);
                    clientGui.inventorySlots.windowId = WINDOW_ID;
                    mc.thePlayer.openContainer = clientGui.inventorySlots;
                    phase = Phase.ATTACH_SERVER;
                }
                case MULTI_READY -> {
                    if (!clientContainer().keepNonConsumablesSync.get()) return;
                    findRollingRecipe();
                    testVisibleAlternatives();
                }
                case MULTI_VERIFIED -> {
                    ItemStack imported = importedBronze(clientContainer(), chosen);
                    if (imported == null) return;
                    require(
                        imported.stackSize == chosen.stackSize && encoded(clientContainer()) == null,
                        "client multi-visible input and no encoded result agree with server");
                    report("multi client imported " + stackId(imported) + "; no encoded pattern");
                    testOnlyVisibleBronze();
                }
                case SINGLE_VERIFIED -> {
                    ItemStack imported = importedBronze(clientContainer(), gregtechBronze);
                    ItemStack encoded = encoded(clientContainer());
                    if (imported == null || encoded == null) return;
                    require(
                        imported.stackSize == gregtechBronze.stackSize
                            && containsEncodedInput(encoded, gregtechBronze, mc.thePlayer),
                        "client receives the same exact GT bronze after auto-encoding");
                    report("single client imported " + stackId(imported) + " and receives auto-encoded pattern");
                    var lines = new ArrayList<String>();
                    lines.add("滚压机 · 三重青铜板");
                    lines.add("NEI 只显示 GT 青铜锭时，导入并编码了 3 个 GT 青铜锭");
                    lines.add(stackId(imported));
                    for (Object line : imported.getTooltip(mc.thePlayer, true)) lines.add((String) line);
                    prepareScreenshot(lines, "terminal-rolling-triple-bronze-input.png", Phase.AUTOCLAVE_READY);
                }
                case AUTOCLAVE_READY -> importAutoclave();
                case AUTOCLAVE_VERIFIED -> {
                    if (encodedOutput(clientContainer(), autoclaveCrystal, mc.thePlayer) == null) return;
                    if (!codechicken.nei.ItemList.itemMap.containsKey(autoclaveCrystal.getItem())) return;
                    importLaserWithEmptyCrystal();
                }
                case LASER_EMPTY_IMPORTED -> {
                    if (!importedExactInput(clientContainer(), autoclaveCrystal)) return;
                    phase = Phase.LASER_EMPTY_ENCODED;
                    clientContainer().quickEncodeAction.send();
                }
                case LASER_EMPTY_VERIFIED -> {
                    if (encodedOutput(clientContainer(), energyChip, mc.thePlayer) == null) return;
                    require(
                        encodedExactInput(clientContainer(), autoclaveCrystal, mc.thePlayer),
                        "client receives exact autoclave identity in the encoded laser pattern");
                    prepareScreenshot(
                        List.of("激光蚀刻机 · 雕刻能量芯片", "样板使用高压釜产出的空能量水晶", stackId(autoclaveCrystal)),
                        "terminal-laser-energy-crystal-input.png",
                        Phase.LASER_CHARGED_READY);
                }
                case LASER_CHARGED_READY -> importLaserWithChargedCrystal();
                case LASER_CHARGED_VERIFIED -> {
                    if (!encodedExactInput(clientContainer(), renderedChargedCrystal, mc.thePlayer)) return;
                    phase = Phase.DONE;
                }
                case DONE -> finish(null);
                default -> {}
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void screenshot(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (phase != Phase.SCREENSHOT || screenshot == null || event.gui != clientGui || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            Method draw = GuiScreen.class
                .getDeclaredMethod("drawHoveringText", List.class, int.class, int.class, FontRenderer.class);
            draw.setAccessible(true);
            draw.invoke(clientGui, screenshot, 10, 36, mc.fontRenderer);
            ScreenShotHelper
                .saveScreenshot(new File("."), screenshotName, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            require(Files.exists(Path.of("screenshots", screenshotName)), "recipe screenshot exists");
            phase = afterScreenshot;
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void prepareScreenshot(List<String> lines, String name, Phase nextPhase) {
        screenshot = lines;
        screenshotName = name;
        afterScreenshot = nextPhase;
        phase = Phase.SCREENSHOT;
    }

    private void prepareServer(EntityPlayerMP player) throws Exception {
        if (provider == null) {
            provider = new TileInterface();
            provider.setWorldObj(player.worldObj);
            provider.xCoord = (int) player.posX;
            provider.yCoord = 100;
            provider.zCoord = (int) player.posZ;
            provider.getProxy()
                .onReady();
            TileCreativeEnergyCell power = new TileCreativeEnergyCell();
            power.setWorldObj(player.worldObj);
            power.xCoord = provider.xCoord + 1;
            power.yCoord = provider.yCoord;
            power.zCoord = provider.zCoord;
            power.getProxy()
                .onReady();
            AEApi.instance()
                .createGridConnection(
                    provider.getProxy()
                        .getNode(),
                    power.getProxy()
                        .getNode());
            return;
        }
        if (!provider.getProxy()
            .isActive()) {
            require(++networkTicks < 200, "ME network did not become active");
            return;
        }
        registerLaserFixtureIfMissing();
        var serverTerminal = terminal(player);
        var grid = provider.getProxy()
            .getNode()
            .getGrid();
        field(WirelessTerminalGuiObject.class, "targetGrid").set(serverTerminal, grid);
        field(WirelessTerminalGuiObject.class, "sg").set(serverTerminal, grid.getCache(IStorageGrid.class));
        field(WirelessTerminalGuiObject.class, "infinityRange").setBoolean(serverTerminal, true);
        var accessPoint = Proxy.newProxyInstance(
            IWirelessAccessPoint.class.getClassLoader(),
            new Class<?>[] { IWirelessAccessPoint.class },
            (proxy, method, args) -> method.getName()
                .equals("getGrid") ? grid
                    : provider.getProxy()
                        .getNode());
        field(WirelessTerminalGuiObject.class, "myWap").set(serverTerminal, accessPoint);
        serverContainer = new QuickEncodingTerminalContainer(player.inventory, serverTerminal);
        serverContainer.windowId = WINDOW_ID;
        serverContainer.setPowerSource(
            provider.getProxy()
                .getEnergy());
        phase = Phase.OPEN_CLIENT;
    }

    private void registerLaserFixtureIfMissing() throws Exception {
        energyChip = GameRegistry.findItemStack("dreamcraft", "EngravedEnergyChip", 1);
        require(energyChip != null, "registered engraved energy chip exists");
        if (RecipeMaps.laserEngraverRecipes.getAllRecipes()
            .stream()
            .anyMatch(
                candidate -> Arrays.stream(candidate.mOutputs)
                    .anyMatch(output -> sameExactType(output, energyChip)))) {
            return;
        }
        // The isolated development client can lack unrelated mods required to run NH CoreMod's recipe script.
        ItemStack rubyLens = GTUtility.copyAmount(0, GTOreDictUnificator.get(OrePrefixes.lens, Materials.Ruby, 1L));
        ItemStack wildcardCrystal = GTModHandler.getModItem("IC2", "itemBatCrystal", 1, OreDictionary.WILDCARD_VALUE);
        require(rubyLens != null && wildcardCrystal != null, "laser fixture uses the actual ruby lens and IC2 crystal");
        require(
            !GTValues.RA.stdBuilder()
                .itemInputs(rubyLens, wildcardCrystal)
                .itemOutputs(energyChip.copy())
                .duration(30 * 20)
                .eut(TierEU.RECIPE_IV / 2)
                .addTo(RecipeMaps.laserEngraverRecipes)
                .isEmpty(),
            "the fixture laser recipe registers with GregTech");
        report("registered the pack's ruby-lens laser recipe in the isolated client");
    }

    private void findRollingRecipe() throws Exception {
        plate = GTOreDictUnificator.get(OrePrefixes.plateTriple, Materials.Bronze, 1L);
        require(plate != null, "registered triple bronze plate exists");
        recipe = new GTNEIDefaultHandler(RecipeMaps.benderRecipes.getDefaultRecipeCategory());
        recipe.loadCraftingRecipes(plate);
        for (int index = 0; index < recipe.numRecipes(); index++) {
            if (recipe.getOtherStacks(index)
                .stream()
                .noneMatch(s -> s.item != null && s.item.isItemEqual(plate))) continue;
            for (PositionedStack stack : recipe.getIngredientStacks(index)) {
                List<ItemStack> raw = Arrays.asList(stack.items);
                ItemStack gt = raw.stream()
                    .filter(item -> owner(item).equalsIgnoreCase("gregtech") && item.stackSize == 3)
                    .findFirst()
                    .orElse(null);
                ItemStack ic2 = raw.stream()
                    .filter(item -> owner(item).equalsIgnoreCase("IC2") && item.stackSize == 3)
                    .findFirst()
                    .orElse(null);
                if (gt == null || ic2 == null) continue;
                recipeIndex = index;
                bronzeSlot = stack;
                gregtechBronze = gt;
                industrialCraftBronze = ic2;
                report(
                    "live GT " + recipe.getOverlayIdentifier()
                        + " recipe #"
                        + index
                        + " raw bronze variants="
                        + raw.stream()
                            .map(TerminalIngredientSelectionChecks::stackId)
                            .toList());
                return;
            }
        }
        throw new AssertionError(
            "no registered bender triple-bronze ingot recipe with GT+IC2 variants, matches=" + recipe.numRecipes());
    }

    private void testVisibleAlternatives() throws Exception {
        // This is an isolated client. Establish both variants as visible before adding the narrower filter.
        ItemInfo.hiddenItems.clear();
        ItemInfo.hiddenItemsRules.filters.clear();
        var visible = bronzeSlot.getFilteredPermutations();
        require(
            visible.stream()
                .anyMatch(item -> sameType(item, gregtechBronze))
                && visible.stream()
                    .anyMatch(item -> sameType(item, industrialCraftBronze)),
            "both bronze variants visible");
        chosen = sameType(visible.get(0), industrialCraftBronze) ? gregtechBronze : industrialCraftBronze;
        require(!sameType(chosen, visible.get(0)), "current displayed variant deliberately differs from first visible");
        bronzeSlot.item = chosen.copy();
        report("multi NEI allows both; cached displayed variant=" + stackId(chosen));
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, recipe, recipeIndex, true);
        phase = Phase.MULTI_SENT;
        // Same ordered AE container channel, after transfer. This makes the no-encoding assertion deterministic.
        clientContainer().requestKeepNonConsumables(false);
    }

    private void testOnlyVisibleBronze() throws Exception {
        for (ItemStack alternative : bronzeSlot.items) {
            if (!sameType(alternative, gregtechBronze)) API.hideItem(alternative);
        }
        var visible = bronzeSlot.getFilteredPermutations();
        require(
            !visible.isEmpty() && visible.stream()
                .allMatch(v -> sameType(v, gregtechBronze)),
            "real NEI hiding leaves only the GT bronze type");
        bronzeSlot.item = industrialCraftBronze.copy();
        report(
            "single NEI allowed=" + visible.stream()
                .map(TerminalIngredientSelectionChecks::stackId)
                .toList() + "; cached input deliberately stale=" + stackId(bronzeSlot.item));
        clientContainer().requestKeepNonConsumables(true);
        phase = Phase.SINGLE_SENT;
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, recipe, recipeIndex, true);
    }

    private void importAutoclave() throws Exception {
        autoclaveCrystal = ItemList.IC2_EnergyCrystal.get(1);
        require(
            autoclaveCrystal != null && autoclaveCrystal.getItemDamage() == 0 && !autoclaveCrystal.hasTagCompound(),
            "GT's registered uncharged crystal has damage 0 without NBT");
        var autoclave = new GTNEIDefaultHandler(RecipeMaps.autoclaveRecipes.getDefaultRecipeCategory());
        autoclave.loadCraftingRecipes(autoclaveCrystal);
        for (int index = 0; index < autoclave.numRecipes(); index++) {
            if (autoclave.getOtherStacks(index)
                .stream()
                .noneMatch(output -> sameExactType(output.item, autoclaveCrystal))) continue;
            report("importing live GT autoclave crystal recipe #" + index);
            phase = Phase.AUTOCLAVE_SENT;
            QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, autoclave, index, true);
            return;
        }
        throw new AssertionError("no registered GT autoclave recipe for the empty crystal");
    }

    private void importLaserWithEmptyCrystal() throws Exception {
        laserRecipe = new GTNEIDefaultHandler(RecipeMaps.laserEngraverRecipes.getDefaultRecipeCategory());
        laserRecipe.loadCraftingRecipes(energyChip);
        report("live GT laser recipes for " + stackId(energyChip) + ": " + laserRecipe.numRecipes());
        for (int index = 0; index < laserRecipe.numRecipes() && laserCrystalSlot == null; index++) {
            if (laserRecipe.getOtherStacks(index)
                .stream()
                .noneMatch(output -> sameExactType(output.item, energyChip))) {
                continue;
            }
            for (PositionedStack positioned : laserRecipe.getIngredientStacks(index)) {
                if (!(positioned instanceof GTNEIDefaultHandler.FixedPositionedStack fixed) || !fixed.isInput()) {
                    continue;
                }
                if (positioned.item == null || positioned.item.getItem() != autoclaveCrystal.getItem()) continue;
                report(
                    "live GT laser crystal recipe #" + index
                        + " raw inputs="
                        + Arrays.stream(fixed.recipe.mRecipe.mInputs)
                            .map(TerminalIngredientSelectionChecks::stackIdentity)
                            .toList()
                        + "; NEI permutations="
                        + Arrays.stream(positioned.items)
                            .map(TerminalIngredientSelectionChecks::stackIdentity)
                            .toList());
                if (Arrays.stream(fixed.recipe.mRecipe.mInputs)
                    .noneMatch(
                        input -> input != null && input.getItem() == autoclaveCrystal.getItem()
                            && input.getItemDamage() == OreDictionary.WILDCARD_VALUE))
                    continue;
                List<ItemStack> visibleCrystals = positioned.getFilteredPermutations()
                    .stream()
                    .filter(input -> input.getItem() == autoclaveCrystal.getItem())
                    .toList();
                renderedEmptyCrystal = visibleCrystals.stream()
                    .filter(
                        input -> input.getItemDamage() == 26 && (!input.hasTagCompound() || input.getTagCompound()
                            .hasNoTags()))
                    .findFirst()
                    .orElse(null);
                renderedChargedCrystal = visibleCrystals.stream()
                    .filter(
                        input -> input.hasTagCompound() && input.getTagCompound()
                            .getDouble("charge") > 0)
                    .findFirst()
                    .orElse(null);
                if (renderedEmptyCrystal == null || renderedChargedCrystal == null) continue;
                laserCrystalSlot = positioned;
                laserRecipeIndex = index;
                break;
            }
        }
        require(laserCrystalSlot != null, "live GT laser recipe displays both IC2 crystal states");
        require(
            !sameExactType(renderedEmptyCrystal, autoclaveCrystal),
            "NEI's rendered empty crystal differs from GT output");
        laserCrystalSlot.item = renderedEmptyCrystal.copy();
        laserCrystalSlot.item.setTagCompound(null);
        ElectricItem.manager.getToolTip(laserCrystalSlot.item);
        require(
            laserCrystalSlot.item.hasTagCompound() && laserCrystalSlot.item.getTagCompound()
                .hasNoTags(),
            "IC2's tooltip leaves an empty compound on the display stack");
        report("live GT laser selected NEI display crystal: " + stackIdentity(laserCrystalSlot.item));
        phase = Phase.LASER_EMPTY_SENT;
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, laserRecipe, laserRecipeIndex, true);
        clientContainer().requestKeepNonConsumables(false);
    }

    private void importLaserWithChargedCrystal() {
        laserCrystalSlot.item = renderedChargedCrystal.copy();
        phase = Phase.LASER_CHARGED_ENCODED;
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, laserRecipe, laserRecipeIndex, false);
        clientContainer().quickEncodeAction.send();
        clientContainer().requestKeepNonConsumables(true);
    }

    private QuickEncodingTerminalContainer clientContainer() {
        return (QuickEncodingTerminalContainer) clientGui.inventorySlots;
    }

    private static DualTerminalGuiObject terminal(EntityPlayer player) {
        ItemStack stack = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        stack.setTagCompound(new NBTTagCompound());
        player.inventory.setInventorySlotContents(0, stack);
        var terminal = new DualTerminalGuiObject(
            (IWirelessTermHandler) stack.getItem(),
            stack,
            player,
            player.worldObj,
            0);
        terminal.setInventorySize(DualTerminalGuiObject.PATTERN_SLOT_COUNT, DualTerminalGuiObject.PATTERN_SLOT_COUNT);
        terminal.readInventory();
        terminal.setCraftingRecipe(false);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(64)
                    .get());
        return terminal;
    }

    private static DualTerminalGuiObject target(QuickEncodingTerminalContainer container) {
        return (DualTerminalGuiObject) container.getTarget();
    }

    private static ItemStack encoded(QuickEncodingTerminalContainer container) {
        return target(container).getInventoryByName("pattern")
            .getStackInSlot(1);
    }

    private static ItemStack importedBronze(QuickEncodingTerminalContainer container, ItemStack wanted) {
        var input = target(container).getAEInventoryByName(StorageName.CRAFTING_INPUT);
        for (int slot = 0; slot < input.getSizeInventory(); slot++) {
            var value = input.getAEStackInSlot(slot);
            if (value != null && sameType(value.getItemStackForNEI(), wanted)) return value.getItemStackForNEI();
        }
        return null;
    }

    private static boolean containsEncodedInput(ItemStack pattern, ItemStack wanted, EntityPlayer player) {
        var details = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, player.worldObj);
        return details != null && Arrays.stream(details.getCondensedAEInputs())
            .anyMatch(input -> sameType(input.getItemStackForNEI(), wanted));
    }

    private static boolean importedExactInput(QuickEncodingTerminalContainer container, ItemStack wanted) {
        var input = target(container).getAEInventoryByName(StorageName.CRAFTING_INPUT);
        for (int slot = 0; slot < input.getSizeInventory(); slot++) {
            var value = input.getAEStackInSlot(slot);
            if (value != null && sameExactType(value.getItemStackForNEI(), wanted)) return true;
        }
        return false;
    }

    private static ItemStack encodedOutput(QuickEncodingTerminalContainer container, ItemStack wanted,
        EntityPlayer player) {
        ItemStack pattern = encoded(container);
        if (pattern == null) return null;
        var details = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, player.worldObj);
        if (details == null) return null;
        return Arrays.stream(details.getCondensedAEOutputs())
            .map(stack -> stack.getItemStackForNEI())
            .filter(stack -> sameExactType(stack, wanted))
            .findFirst()
            .orElse(null);
    }

    private static boolean encodedExactInput(QuickEncodingTerminalContainer container, ItemStack wanted,
        EntityPlayer player) {
        ItemStack pattern = encoded(container);
        if (pattern == null) return false;
        var details = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, player.worldObj);
        return details != null && Arrays.stream(details.getCondensedAEInputs())
            .anyMatch(input -> sameExactType(input.getItemStackForNEI(), wanted));
    }

    private static boolean sameExactType(ItemStack first, ItemStack second) {
        return first != null && second != null
            && AEItemStack.create(first)
                .isSameType(AEItemStack.create(second));
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null && NEIServerUtils.areStacksSameType(first, second);
    }

    private static String owner(ItemStack stack) {
        var id = stack != null ? GameRegistry.findUniqueIdentifierFor(stack.getItem()) : null;
        return id == null ? "" : id.modId;
    }

    private static String stackId(ItemStack stack) {
        return GameRegistry.findUniqueIdentifierFor(stack.getItem()) + ":"
            + stack.getItemDamage()
            + " x"
            + stack.stackSize;
    }

    private static String stackIdentity(ItemStack stack) {
        return stack == null ? "null" : stackId(stack) + " nbt=" + stack.getTagCompound();
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void report(String line) throws Exception {
        System.out.println("[terminal-ingredient-selection-qa] " + line);
        Files.writeString(
            Path.of("report.txt"),
            line + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    private static void require(boolean check, String message) {
        if (!check) throw new AssertionError(message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + phase + " " + error);
            field(Minecraft.class, "running").setBoolean(Minecraft.getMinecraft(), false);
        } catch (Exception secondary) {
            secondary.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(1, true);
        }
    }

    private enum Phase {
        PREPARE,
        OPEN_CLIENT,
        ATTACH_SERVER,
        MULTI_READY,
        MULTI_SENT,
        MULTI_VERIFIED,
        SINGLE_SENT,
        SINGLE_VERIFIED,
        SCREENSHOT,
        AUTOCLAVE_READY,
        AUTOCLAVE_SENT,
        AUTOCLAVE_VERIFIED,
        LASER_EMPTY_SENT,
        LASER_EMPTY_IMPORTED,
        LASER_EMPTY_ENCODED,
        LASER_EMPTY_VERIFIED,
        LASER_CHARGED_READY,
        LASER_CHARGED_ENCODED,
        LASER_CHARGED_VERIFIED,
        DONE
    }
}
