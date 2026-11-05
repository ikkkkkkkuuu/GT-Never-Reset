package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer.CircuitSlot;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.PatternRecipeRouting;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineBuffer;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineInput;
import com.xyp.gtneverreset.common.me.overloaded.OverloadedMachineMolds;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternCache;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.SimpleIOComponent;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerNull;
import appeng.util.PatternMultiplierHelper;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.multi.MTEIndustrialPackager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Runs native AE encoding and GT matching in an isolated, hidden client with real item registries. */
@Mod(modid = "circuitpatternqa", name = "Circuit Pattern QA", version = "1", dependencies = "after:gtneverreset")
public final class CircuitPatternChecks {

    private boolean launched, checked, finished;
    private int ticks, frames;
    private volatile ItemStack preview;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || checked || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        checked = true;
        try {
            preview = verify((EntityPlayerMP) players.get(0));
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "patterns-" + System.currentTimeMillis(),
                    "Circuit Pattern QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks >= 1800) throw new AssertionError("QA timeout");
            if (preview == null) return;
            if (!(mc.currentScreen instanceof PreviewScreen)) {
                List<String> tooltip = preview.getTooltip(mc.thePlayer, false);
                require(
                    tooltip.stream()
                        .anyMatch(line -> line.contains("内置编程电路：4")),
                    "circuit tooltip");
                require(
                    tooltip.stream()
                        .anyMatch(line -> line.contains("内置不消耗工具")),
                    "mold tooltip");
                mc.displayGuiScreen(new PreviewScreen(tooltip));
            }
            if (++frames < 12) return;
            if (frames == 12) {
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "circuit-pattern.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                var mirror = GTNRItemList.CrossRecipeMEPatternMirror.get(1);
                require(
                    mirror.getDisplayName()
                        .contains("超级跨配方类型样板输入镜像"),
                    "dedicated mirror localized name");
                List<String> tooltip = mirror.getTooltip(mc.thePlayer, false);
                require(
                    tooltip.stream()
                        .anyMatch(line -> line.contains("仅可连接超级跨配方类型")),
                    "dedicated mirror tooltip explains compatible master");
                require(
                    tooltip.stream()
                        .anyMatch(line -> line.contains("同一多方块仅可安装")),
                    "dedicated mirror tooltip explains family restriction");
                mc.displayGuiScreen(new PreviewScreen(tooltip));
                return;
            }
            if (frames < 24) return;
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "cross-recipe-mirror.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static ItemStack verify(EntityPlayerMP player) throws Exception {
        RecipeMap<RecipeMapBackend> map = RecipeMapBuilder.of("gtnr.qa.circuit_pattern")
            .maxIO(3, 1, 1, 0)
            .minInputs(1, 0)
            .build();
        ItemStack material = new ItemStack(Items.clay_ball, 2);
        ItemStack output = new ItemStack(Items.paper, 3);
        ItemStack mold = ItemList.Shape_Mold_Plate.get(0);
        ItemStack circuit = GTUtility.getIntegratedCircuit(4);
        FluidStack fluid = new FluidStack(FluidRegistry.WATER, 100);
        GTValues.RA.stdBuilder()
            .itemInputs(material, mold, circuit)
            .itemOutputs(output)
            .fluidInputs(fluid)
            .duration(20)
            .eut(8)
            .addTo(map);

        ItemStack displayedMold = mold.copy();
        displayedMold.stackSize = 1;
        ItemStack displayedCircuit = circuit.copy();
        displayedCircuit.stackSize = 1;
        IAEStack<?>[] inputs = { AEItemStack.create(material), AEItemStack.create(displayedMold),
            AEItemStack.create(displayedCircuit), AEFluidStack.create(fluid) };
        ItemStack legacy = pattern(inputs, output);
        ICraftingPatternDetails runtime = CircuitPatternCodec.runtime(legacy, player.worldObj);
        require(runtime != null, "legacy pattern recognized");
        checkTools(runtime);
        require(
            CircuitPatternCodec.decode(legacy, player.worldObj)
                .getAEInputs().length == 4,
            "legacy physical pattern unchanged");

        ItemStack terminalStack = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        terminalStack.setTagCompound(new NBTTagCompound());
        player.inventory.setInventorySlotContents(0, terminalStack);
        DualTerminalGuiObject terminal = new DualTerminalGuiObject(
            (IWirelessTermHandler) terminalStack.getItem(),
            terminalStack,
            player,
            player.worldObj,
            0);
        terminal.setInventorySize(256, 256);
        terminal.readInventory();
        terminal.setCraftingRecipe(false);
        require(terminal.shouldKeepNonConsumables(), "new terminal defaults to metadata encoding");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(8)
                    .get());
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, legacy.copy());
        terminal.setRecipeMapName(map.unlocalizedName);
        require(terminal.encode(null, null, null, "QA", player.worldObj), "native terminal encoding");
        ItemStack encoded = terminal.getInventoryByName("pattern")
            .getStackInSlot(1)
            .copy();
        checkTools(CircuitPatternCodec.decode(encoded, player.worldObj));
        require(map.unlocalizedName.equals(PatternRecipeMap.name(encoded)), "terminal encodes recipe map metadata");
        require(
            encoded.getTagCompound()
                .getTagList("in", 10)
                .tagCount() == 2,
            "only material and fluid in AE input list");
        ItemStack saved = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        checkTools(CircuitPatternCodec.runtime(saved, player.worldObj));
        require(
            saved.getTagCompound()
                .equals(
                    CircuitPatternCodec.runtime(saved, player.worldObj)
                        .getPattern()
                        .getTagCompound()),
            "stable runtime identity after reload");
        for (IAEStack<?> input : CircuitPatternCodec.decode(saved, player.worldObj)
            .getAEInputs()) {
            if (input instanceof IAEFluidStack aeFluid) require(
                aeFluid.getFluidStack()
                    .isFluidEqual(fluid) && input.getStackSize() == 100,
                "native fluid input retained");
        }

        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, saved.copy());
        require(
            terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
                .getAEStackInSlot(3) != null,
            "saved tools restored in editor");
        terminal.writeInventory();
        terminal.readInventory();
        require(terminal.encode(null, null, null, "QA", player.worldObj), "encode after terminal reload");
        require(
            map.unlocalizedName.equals(
                PatternRecipeMap.name(
                    terminal.getInventoryByName("pattern")
                        .getStackInSlot(1))),
            "recipe map survives editor reopening and terminal reload");
        checkTools(
            CircuitPatternCodec.decode(
                terminal.getInventoryByName("pattern")
                    .getStackInSlot(1),
                player.worldObj));
        terminal.setKeepNonConsumables(false);
        require(terminal.encode(null, null, null, "QA", player.worldObj), "disabled encoding");
        ItemStack ordinary = terminal.getInventoryByName("pattern")
            .getStackInSlot(1);
        require(
            map.unlocalizedName.equals(PatternRecipeMap.name(ordinary)),
            "recipe map encoding is independent of virtual-tool switch");
        require(
            !ordinary.getTagCompound()
                .hasKey(CircuitPatternCodec.CIRCUIT)
                && !ordinary.getTagCompound()
                    .hasKey(CircuitPatternCodec.MOLDS),
            "disabled switch preserves normal inputs");
        require(
            CircuitPatternCodec.decode(ordinary, player.worldObj)
                .getCondensedAEInputs().length == 4,
            "disabled switch retains physical tools");

        terminal.setKeepNonConsumables(true);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, pattern(new IAEStack<?>[] { AEItemStack.create(material) }, output));
        require(terminal.encode(null, null, null, "QA", player.worldObj), "encode changed recipe");
        require(
            PatternRecipeMap.name(
                terminal.getInventoryByName("pattern")
                    .getStackInSlot(1))
                == null,
            "loading an unannotated recipe clears stale recipe map");
        require(
            CircuitPatternCodec.storedCircuit(
                terminal.getInventoryByName("pattern")
                    .getStackInSlot(1))
                == -1,
            "changed recipe clears circuit");
        require(
            CircuitPatternCodec.storedMolds(
                terminal.getInventoryByName("pattern")
                    .getStackInSlot(1)).length
                == 0,
            "changed recipe clears molds");

        ItemStack doubled = saved.copy();
        PatternMultiplierHelper.applyModification(doubled, 1);
        checkTools(CircuitPatternCodec.runtime(doubled, player.worldObj));
        require(CircuitPatternCodec.storedCircuit(doubled) == 4, "multiplied pattern retains circuit metadata");
        ItemStack malformed = saved.copy();
        malformed.getTagCompound()
            .setInteger(CircuitPatternCodec.CIRCUIT, 25);
        require(CircuitPatternCodec.runtime(malformed, player.worldObj) == null, "invalid circuit rejected");
        malformed = saved.copy();
        malformed.getTagCompound()
            .getTagList(CircuitPatternCodec.MOLDS, 10)
            .getCompoundTagAt(0)
            .setByte("Count", (byte) 1);
        require(CircuitPatternCodec.runtime(malformed, player.worldObj) == null, "positive mold metadata rejected");

        ItemStack consumedOutput = new ItemStack(Items.book, 1);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.stick, 1), displayedMold)
            .itemOutputs(consumedOutput)
            .duration(20)
            .eut(8)
            .addTo(map);
        var consumed = CircuitPatternCodec
            .runtime(
                pattern(
                    new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.stick)),
                        AEItemStack.create(displayedMold) },
                    consumedOutput),
                player.worldObj);
        require(
            consumed != null && CircuitPatternCodec.molds(consumed).length == 0
                && consumed.getCondensedAEInputs().length == 2,
            "consumed mold stays a real AE input");
        verifyWildcard(player, map);
        verifySingleblock(player);
        verifyCrossRecipe(player);
        verifyMirrors(player);
        return saved;
    }

    private static void verifyMirrors(EntityPlayerMP player) throws Exception {
        require(GTNRItemList.CrossRecipeMEPatternMirror.get(1) != null, "cross-recipe mirror registered");
        require(
            RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> java.util.Arrays.stream(recipe.mOutputs)
                        .anyMatch(output -> GTNRItemList.CrossRecipeMEPatternMirror.isStackEqual(output))),
            "cross-recipe mirror recipe registered");
        int[] masterIds = { GTNRMachineID.SuperCraftingInputME.id, GTNRMachineID.CircuitMEPatternBuffer.id,
            GTNRMachineID.CrossRecipeMEPatternBuffer.id };
        int[] mirrorIds = { GTNRMachineID.SuperCraftingInputSlave.id, GTNRMachineID.CircuitMEPatternMirror.id,
            GTNRMachineID.CrossRecipeMEPatternMirror.id };
        var masters = new SuperMTEHatchCraftingInputME[3];
        var mirrors = new SuperMTEHatchCraftingInputSlave[3];
        int originX = (int) player.posX + 3;
        int originZ = (int) player.posZ + 3;
        ItemStack held = player.inventory.getCurrentItem();
        try {
            for (int i = 0; i < 3; i++) {
                masters[i] = (SuperMTEHatchCraftingInputME) placedMachine(player, masterIds[i], originX + i, originZ)
                    .getMetaTileEntity();
                mirrors[i] = (SuperMTEHatchCraftingInputSlave) placedMachine(
                    player,
                    mirrorIds[i],
                    originX + i,
                    originZ + 1).getMetaTileEntity();
            }
            for (int i = 0; i < 3; i++) {
                var mirror = mirrors[i];
                for (int j = 0; j < 3; j++) {
                    boolean match = i == j;
                    String pair = "mirror family " + i + " -> master " + j;
                    var master = masters[j];
                    var base = master.getBaseMetaTileEntity();
                    mirror.loadNBTData(new NBTTagCompound());
                    require(
                        (mirror.trySetMasterFromCoord(base.getXCoord(), base.getYCoord(), base.getZCoord()) == master)
                            == match,
                        pair + " coordinate binding");
                    require((mirror.getMaster() == master) == match, pair + " coordinate access");
                    var saved = new NBTTagCompound();
                    var coordinates = new NBTTagCompound();
                    coordinates.setInteger("x", base.getXCoord());
                    coordinates.setInteger("y", base.getYCoord());
                    coordinates.setInteger("z", base.getZCoord());
                    saved.setTag("master", coordinates);
                    saved.setString("type", SuperMTEHatchCraftingInputSlave.COPIED_DATA_IDENTIFIER);
                    mirror.loadNBTData(new NBTTagCompound());
                    require(mirror.pasteCopiedData(player, saved) == match, pair + " copied binding");
                    require((mirror.getMaster() == master) == match, pair + " copied access");
                    mirror.loadNBTData(saved);
                    mirror.onPostTick(mirror.getBaseMetaTileEntity(), 100);
                    require((mirror.getMaster() == master) == match, pair + " saved binding after tick");
                    mirror.loadNBTData(new NBTTagCompound());
                    var stick = ItemList.Tool_DataStick.get(1);
                    master.saveToDataStick(player, stick);
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, stick);
                    require(mirror.onRightclick(mirror.getBaseMetaTileEntity(), player), pair + " data stick handled");
                    require((mirror.getMaster() == master) == match, pair + " data stick binding");
                    if (!match) {
                        Field cached = SuperMTEHatchCraftingInputSlave.class.getDeclaredField("master");
                        cached.setAccessible(true);
                        cached.set(mirror, master);
                        master.addProxyHatch(mirror);
                        require(
                            mirror.getMaster() == null && !mirror.inventories()
                                .hasNext(),
                            pair + " stale mixed cache rejected");
                        master.getProxyHatches();
                        master.removeProxyHatch(mirror);
                        var ownBase = masters[i].getBaseMetaTileEntity();
                        mirror.trySetMasterFromCoord(ownBase.getXCoord(), ownBase.getYCoord(), ownBase.getZCoord());
                        require(
                            !mirror.pasteCopiedData(player, saved) && mirror.getMaster() == masters[i],
                            pair + " rejected paste preserves valid binding");
                        mirror.onRightclick(mirror.getBaseMetaTileEntity(), player);
                        require(
                            mirror.getMaster() == masters[i],
                            pair + " rejected data stick preserves valid binding");
                    }
                }
            }
            var hatch = (CrossRecipeMEPatternBuffer) masters[2];
            verifyPatternFamilies(player, masters, mirrors);
            var mirror = mirrors[2];
            var base = hatch.getBaseMetaTileEntity();
            require(
                mirror.trySetMasterFromCoord(base.getXCoord(), base.getYCoord(), base.getZCoord()) == hatch,
                "dedicated mirror linked to cross-recipe assembly");
            hatch.onPatternChange(
                0,
                metadata(
                    mapPattern(
                        RecipeMaps.packagerRecipes,
                        new ItemStack(Items.clay_ball, 3),
                        new ItemStack(Items.paper)),
                    4,
                    null));
            hatch.onPatternChange(
                1,
                metadata(
                    mapPattern(
                        RecipeMaps.unpackagerRecipes,
                        new ItemStack(Items.clay_ball, 3),
                        new ItemStack(Items.book)),
                    4,
                    null));
            var slots = new ArrayList<CircuitSlot>();
            mirror.inventories()
                .forEachRemaining(slot -> {
                    require(
                        slot instanceof RecipeMapPatternInventory,
                        "mirror retains per-pattern recipe map contract");
                    slots.add((CircuitSlot) slot);
                    require(
                        slot.insertItemsAndFluids(delivery(slot.getPatternDetails())),
                        "mirrored delivery accepted");
                });
            var packager = controller(player, "gregtech.common.tileentities.machines.multi.MTEIndustrialPackager");
            Field pipeTier = packager.getClass()
                .getDeclaredField("itemPipeTier");
            pipeTier.setAccessible(true);
            pipeTier.setInt(packager, 0);
            packager.setMachineMode(0);
            packager.mDualInputHatches.add(mirror);
            mirror.addWatcher(packager);
            require(
                hatch.getControllerRecipeMaps()
                    .equals(
                        List.of(
                            RecipeMaps.packagerRecipes.unlocalizedName,
                            RecipeMaps.unpackagerRecipes.unlocalizedName)),
                "dropdown discovers controller through dedicated mirror");
            hatch.setTerminalRecipeMap(RecipeMaps.unpackagerRecipes.unlocalizedName);
            require(
                hatch.getTerminalRecipeMap()
                    .equals(RecipeMaps.unpackagerRecipes.unlocalizedName),
                "mirrored controller recipe map selectable for terminal name");
            packager.mDualInputHatches.add(masters[1]);
            var mixed = runController(packager);
            require(
                !mixed.wasSuccessful() && slots.stream()
                    .noneMatch(CircuitSlot::isEmpty),
                "mixed circuit assembly and cross-recipe mirror cannot consume buffered inputs");
            require(
                mixed.getDisplayString()
                    .contains("不可混装"),
                "mixed-input error has localized player feedback");
            packager.mDualInputHatches.remove(masters[1]);
            require(
                runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.paper,
                "native controller consumes packing through mirror only");
            require(
                runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.book,
                "native controller consumes unpacking through same mirror");
            require(
                packager.getMachineMode() == 0 && slots.size() == 2
                    && slots.stream()
                        .allMatch(CircuitSlot::isEmpty),
                "mirrored deliveries consumed once and controller mode restored");
            packager.mDualInputHatches.clear();
            require(
                hatch.getControllerRecipeMaps()
                    .isEmpty(),
                "detached mirrored controller leaves no stale map choices");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
            for (int i = 0; i < 3; i++) {
                player.worldObj.setBlockToAir(originX + i, 100, originZ + 1);
                player.worldObj.setBlockToAir(originX + i, 100, originZ);
            }
        }
    }

    private static BaseMetaTileEntity placedMachine(EntityPlayerMP player, int id, int x, int z) {
        player.worldObj.setBlock(x, 100, z, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 100, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private static void verifyPatternFamilies(EntityPlayerMP player, SuperMTEHatchCraftingInputME[] masters,
        SuperMTEHatchCraftingInputSlave[] mirrors) throws Exception {
        var controller = new PatternStructureController();
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.setMetaTileEntity(controller);
        controller.setBaseMetaTileEntity(tile);
        var inputs = new IDualInputHatch[] { masters[0], masters[1], masters[2], mirrors[0], mirrors[1], mirrors[2] };
        Method ttCheck = MTEMultiBlockBase.class.getDeclaredMethod("checkMachine_TT", IGregTechTileEntity.class);
        ttCheck.setAccessible(true);
        Field errors = MTEMultiBlockBase.class.getDeclaredField("structureErrors");
        errors.setAccessible(true);
        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < inputs.length; j++) {
                controller.inputs = List.of(inputs[i], inputs[j]);
                boolean compatible = i % 3 == j % 3;
                require(
                    controller.checkStructure(true, tile) == compatible,
                    "native structure check respects input families " + i + "/" + j);
                require(
                    ((List<?>) errors.get(controller)).isEmpty() == compatible,
                    "mixed structure exposes native error " + i + "/" + j);
                controller.mDualInputHatches.clear();
                require(
                    (boolean) ttCheck.invoke(controller, tile) == compatible,
                    "TT structure check respects input families " + i + "/" + j);
            }
        }
        for (var mirror : mirrors) mirror.loadNBTData(new NBTTagCompound());
        controller.inputs = List.of(mirrors[1], mirrors[2]);
        require(!controller.checkStructure(true, tile), "unlinked mirrors still retain their family restriction");
    }

    private static void verifyCrossRecipe(EntityPlayerMP player) throws Exception {
        require(GTNRItemList.CrossRecipeMEPatternBuffer.get(1) != null, "cross-recipe assembly registered");
        var packager = controller(player, "gregtech.common.tileentities.machines.multi.MTEIndustrialPackager");
        Field pipeTier = packager.getClass()
            .getDeclaredField("itemPipeTier");
        pipeTier.setAccessible(true);
        pipeTier.setInt(packager, 0);
        packager.setMachineMode(0);
        require(packager.getRecipeMap() == RecipeMaps.packagerRecipes, "native packager starts in packing mode");
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.clay_ball, 3), GTUtility.getIntegratedCircuit(4))
            .itemOutputs(new ItemStack(Items.paper))
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.packagerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.clay_ball, 3))
            .itemOutputs(new ItemStack(Items.book))
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.unpackagerRecipes);
        ItemStack packing = mapPattern(
            RecipeMaps.packagerRecipes,
            new ItemStack(Items.clay_ball, 3),
            new ItemStack(Items.paper));
        ItemStack unpacking = mapPattern(
            RecipeMaps.unpackagerRecipes,
            new ItemStack(Items.clay_ball, 3),
            new ItemStack(Items.book));
        packing = metadata(packing, 4, null);
        unpacking = metadata(unpacking, 4, null);
        var hatch = crossHatch(player);
        hatch.onPatternChange(0, packing);
        hatch.onPatternChange(1, unpacking);
        var slots = new ArrayList<CircuitSlot>();
        hatch.inventories()
            .forEachRemaining(slot -> slots.add((CircuitSlot) slot));
        require(
            slots.size() == 2 && slots.get(0) instanceof RecipeMapPatternInventory,
            "new hatch exposes isolated recipe-map inventories");
        for (var slot : slots) require(
            slot.insertItemsAndFluids(delivery(slot.getPatternDetails())),
            "cross-recipe buffer accepts actual AE inputs");
        packager.mDualInputHatches.add(hatch);
        require(
            runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.paper,
            "native multiblock executes packing pattern");
        require(
            runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.book,
            "same multiblock executes unpacking pattern without manual mode switch");
        require(
            packager.getMachineMode() == 0 && slots.stream()
                .allMatch(CircuitSlot::isEmpty),
            "native mode restored and both isolated deliveries consumed exactly once");

        var fallback = pattern(
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.clay_ball, 3)) },
            new ItemStack(Items.paper));
        fallback = metadata(fallback, 4, null);
        checkMapDelivery(
            player,
            packager,
            fallback,
            true,
            Items.paper,
            "unannotated new-hatch pattern uses current mode");
        checkMapDelivery(
            player,
            packager,
            mapPattern(RecipeMaps.assemblerRecipes, new ItemStack(Items.clay_ball, 3), new ItemStack(Items.book)),
            false,
            null,
            "unsupported map cannot run in packager or consume inputs");
        var invalid = unpacking.copy();
        PatternRecipeMap.write(invalid, "gtnr.qa.removed_map");
        checkMapDelivery(player, packager, invalid, false, null, "unknown map stays buffered");
        invalid = unpacking.copy();
        invalid.getTagCompound()
            .setInteger(PatternRecipeMap.KEY, 1);
        checkMapDelivery(player, packager, invalid, false, null, "malformed map is not treated as unannotated");

        var ordinaryTile = new BaseMetaTileEntity();
        ordinaryTile.setWorldObj(player.worldObj);
        ordinaryTile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CircuitMEPatternBuffer.id);
        var ordinary = (CircuitMEPatternBuffer) ordinaryTile.getMetaTileEntity();
        ordinary.onPatternChange(0, unpacking);
        var ordinarySlot = (CircuitSlot) ordinary.inventories()
            .next();
        ordinarySlot.insertItemsAndFluids(delivery(ordinarySlot.getPatternDetails()));
        packager.mDualInputHatches.clear();
        packager.mDualInputHatches.add(ordinary);
        require(
            runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.paper,
            "ordinary circuit assembly retains original machine-mode behavior");

        var reloadedHatch = crossHatch(player);
        reloadedHatch.onPatternChange(0, unpacking);
        var active = (CircuitSlot) reloadedHatch.inventories()
            .next();
        active.insertItemsAndFluids(delivery(active.getPatternDetails()));
        NBTTagCompound saved = new NBTTagCompound();
        reloadedHatch.saveNBTData(saved);
        var restored = crossHatch(player);
        restored.loadNBTData(saved);
        var restoredSlot = (CircuitSlot) restored.inventories()
            .next();
        require(
            RecipeMaps.unpackagerRecipes.unlocalizedName
                .equals(((RecipeMapPatternInventory) restoredSlot).getRecipeMapName()),
            "recipe-map selection and active buffer survive hatch NBT reload");
        packager.mDualInputHatches.clear();
        packager.mDualInputHatches.add(restored);
        require(
            runController(packager).wasSuccessful() && logic(packager).getOutputItems()[0].getItem() == Items.book,
            "reloaded cross-recipe assembly resumes correct recipe pool");

        var precise = controller(player, "goodgenerator.blocks.tileEntity.MTEPreciseAssembler");
        if (precise.getRecipeMap() != RecipeMaps.assemblerRecipes) precise.setMachineMode(precise.nextMachineMode());
        int originalMode = precise.getMachineMode();
        RecipeMap<?> preciseMap = precise.getAvailableRecipeMaps()
            .stream()
            .filter(map -> map != RecipeMaps.assemblerRecipes)
            .findFirst()
            .get();
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.cookie))
            .itemOutputs(new ItemStack(Items.diamond))
            .duration(20)
            .eut(8)
            .specialValue(100)
            .addTo(preciseMap);
        var tierResult = checkMapDelivery(
            player,
            precise,
            mapPattern(preciseMap, new ItemStack(Items.cookie), new ItemStack(Items.diamond)),
            false,
            null,
            "precise assembler still enforces target mode casing tier");
        require(
            tierResult.getID()
                .equals("insufficient_machine_tier"),
            "target-mode validation returns its native tier error");
        require(
            precise.getMachineMode() == originalMode,
            "failed target-mode validation restores precise assembler mode");
        try (var selected = PatternRecipeRouting.select(precise, preciseMap)) {
            require(
                selected != null && precise.getRecipeMap() == preciseMap && precise.getMaxParallelRecipes() == 1,
                "native target-mode parallel limit is honored");
        }
        packager.setRecipeLocking(true);
        checkMapDelivery(player, packager, unpacking, false, null, "recipe locking prevents an implicit mode change");
        packager.setRecipeLocking(false);
        verifyModeParameters(player);
        verifyModeCoverage(player);
        var assemblyLine = controller(player, "bartworks.common.tileentities.multis.MTECircuitAssemblyLine");
        RecipeMap<?> lineMap = assemblyLine.getRecipeMap();
        assemblyLine.setMachineMode(assemblyLine.nextMachineMode());
        checkMapDelivery(
            player,
            assemblyLine,
            mapPattern(lineMap, new ItemStack(Items.clay_ball, 3), new ItemStack(Items.book)),
            false,
            null,
            "circuit assembly line missing imprint keeps inputs without crashing");

        var payload = new RecipeTransferPayload(
            false,
            true,
            4,
            false,
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.clay_ball, 3)) },
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.book)) },
            null,
            RecipeMaps.unpackagerRecipes.unlocalizedName);
        Method write = RecipeTransferPayload.class
            .getDeclaredMethod("write", ByteBuf.class, RecipeTransferPayload.class);
        Method read = RecipeTransferPayload.class.getDeclaredMethod("read", ByteBuf.class);
        write.setAccessible(true);
        read.setAccessible(true);
        ByteBuf bytes = Unpooled.buffer();
        try {
            write.invoke(null, bytes, payload);
            var roundTrip = (RecipeTransferPayload) read.invoke(null, bytes);
            require(
                roundTrip.getRecipeMapName()
                    .equals(payload.getRecipeMapName()) && roundTrip.shouldEncode(),
                "NEI transfer packet atomically preserves recipe map and encode request");
        } finally {
            bytes.release();
        }
        verifyMapWildcards(player);
    }

    private static void verifyMapWildcards(EntityPlayerMP player) throws Exception {
        HashSet<ICraftingPatternDetails> options = new HashSet<>();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.packagerRecipes, RecipeMaps.unpackagerRecipes }) {
            ItemStack source = GTNRItemList.WildcardPattern.get(1);
            WildcardPatternGenerator.markAsWildcard(source);
            WildcardModelState.setInputs(source, List.of(new PrefixIOComponent(OrePrefixes.ingot, 1)));
            WildcardModelState.setOutputs(source, List.of(new PrefixIOComponent(OrePrefixes.plate, 1)));
            WildcardModelState.setFilters(source, List.of(new StringFilterComponent("^(Iron|Copper)$", true)));
            PatternRecipeMap.write(source, map.unlocalizedName);
            for (var detail : WildcardPatternGenerator.generateAllDetails(source, player.worldObj)) {
                require(options.add(detail), "identical wildcard IO with different recipe maps stays distinct");
                require(
                    map.unlocalizedName.equals(PatternRecipeMap.name(detail.getPattern())),
                    "wildcard expansion preserves recipe map");
                var runtime = CircuitPatternCodec.runtime(detail.getPattern(), player.worldObj);
                require(
                    map.unlocalizedName.equals(PatternRecipeMap.name(runtime.getPattern())),
                    "native wildcard runtime preserves recipe map");
            }
        }
        require(options.size() == 4, "all recipe-map wildcard identities verified");
    }

    private static CheckRecipeResult checkMapDelivery(EntityPlayerMP player, MTEMultiBlockBase controller,
        ItemStack pattern, boolean expected, net.minecraft.item.Item output, String message) throws Exception {
        var hatch = crossHatch(player);
        hatch.onPatternChange(0, pattern);
        var slot = (CircuitSlot) hatch.inventories()
            .next();
        require(slot.insertItemsAndFluids(delivery(slot.getPatternDetails())), "routed test buffer populated");
        int mode = controller.getMachineMode();
        controller.mDualInputHatches.clear();
        controller.mDualInputHatches.add(hatch);
        var result = runController(controller);
        require(
            result.wasSuccessful() == expected
                && (expected ? logic(controller).getOutputItems()[0].getItem() == output && slot.isEmpty()
                    : !slot.isEmpty()),
            message);
        require(controller.getMachineMode() == mode, "controller mode does not leak to subsequent inventories");
        return result;
    }

    private static CheckRecipeResult runController(MTEMultiBlockBase controller) throws Exception {
        ProcessingLogic logic = logic(controller);
        ((com.xyp.gtneverreset.mixins.late.gregtech.MultiblockProcessingAccessor) controller)
            .gtnr$setupProcessingLogic(logic);
        logic.setMaxParallelSupplier(() -> 1)
            .setVoidProtection(false, false);
        Method check = MTEMultiBlockBase.class.getDeclaredMethod("doCheckRecipe");
        check.setAccessible(true);
        var result = (CheckRecipeResult) check.invoke(controller);
        System.out.println(
            "[cross-recipe-qa] controller=" + controller.getClass()
                .getSimpleName()
                + " result="
                + result.getID()
                + " detail="
                + result.getDisplayString()
                + " output="
                + java.util.Arrays.toString(logic.getOutputItems()));
        return result;
    }

    private static void verifyModeParameters(EntityPlayerMP player) throws Exception {
        var tower = controller(
            player,
            "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.advanced.MTEAdvDistillationTower");
        Field height = tower.getClass()
            .getDeclaredField("mHeight");
        height.setAccessible(true);
        height.setInt(tower, 4);
        var fluid = new net.minecraftforge.fluids.Fluid("gtnr.qa.cross_recipe");
        FluidRegistry.registerFluid(fluid);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.egg))
            .fluidInputs(new FluidStack(fluid, 13))
            .itemOutputs(new ItemStack(Items.apple))
            .fluidOutputs(new FluidStack(FluidRegistry.WATER, 1))
            .duration(200)
            .eut(32)
            .addTo(RecipeMaps.distilleryRecipes);
        ItemStack encoded = pattern(
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.egg)),
                AEFluidStack.create(new FluidStack(fluid, 13)) },
            new ItemStack(Items.apple));
        PatternRecipeMap.write(encoded, RecipeMaps.distilleryRecipes.unlocalizedName);
        if (tower.getRecipeMap() != RecipeMaps.distilleryRecipes) tower.setMachineMode(tower.nextMachineMode());
        checkMapDelivery(
            player,
            tower,
            encoded,
            true,
            Items.apple,
            "advanced distillation tower native distillery baseline");
        long eut = logic(tower).getCalculatedEut();
        int duration = logic(tower).getDuration();
        tower.setMachineMode(tower.nextMachineMode());
        checkMapDelivery(
            player,
            tower,
            encoded,
            true,
            Items.apple,
            "advanced distillation tower automatically selects distillery mode");
        require(
            logic(tower).getCalculatedEut() == eut && logic(tower).getDuration() == duration,
            "automatic mode matches native speed and power settings");
        Field speed = ProcessingLogic.class.getDeclaredField("speedBoost");
        speed.setAccessible(true);
        require(
            Math.abs(speed.getDouble(logic(tower)) - 1D / 3D) < 1E-9,
            "original processing settings restored after routed recipe");
    }

    private static void verifyModeCoverage(EntityPlayerMP player) {
        int machines = 0, maps = 0;
        var types = new HashSet<Class<?>>();
        for (var prototype : GregTechAPI.METATILEENTITIES) {
            if (!(prototype instanceof MTEMultiBlockBase multi) || !types.add(multi.getClass())
                || multi.getAvailableRecipeMaps()
                    .size() < 2)
                continue;
            if (!multi.supportsMachineModeSwitch()) {
                System.out.println(
                    "[cross-recipe-qa] non-mode controller=" + multi.getClass()
                        .getName());
                continue;
            }
            var target = controller(
                player,
                multi.getClass()
                    .getName());
            int mode = target.getMachineMode();
            for (RecipeMap<?> map : target.getAvailableRecipeMaps()) {
                try (var selected = PatternRecipeRouting.select(target, map)) {
                    if (selected == null) {
                        System.out.println(
                            "[cross-recipe-qa] unavailable native route=" + target.getClass()
                                .getName() + " / " + map.unlocalizedName);
                        require(target.getMachineMode() == mode, "unavailable native route restores controller");
                        continue;
                    }
                    require(
                        selected != null && target.getRecipeMap() == map,
                        "native map selectable: " + target.getClass()
                            .getSimpleName() + " / " + map.unlocalizedName);
                }
                require(target.getMachineMode() == mode, "native mode enumeration restores controller");
                maps++;
            }
            machines++;
        }
        require(
            machines >= 3 && maps >= 6,
            "native multi-mode coverage: " + machines + " controller types, " + maps + " recipe map routes");
    }

    private static ProcessingLogic logic(MTEMultiBlockBase controller) throws Exception {
        Field field = MTEMultiBlockBase.class.getDeclaredField("processingLogic");
        field.setAccessible(true);
        return (ProcessingLogic) field.get(controller);
    }

    private static MTEMultiBlockBase controller(EntityPlayerMP player, String name) {
        for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
            var prototype = GregTechAPI.METATILEENTITIES[id];
            if (prototype == null || !prototype.getClass()
                .getName()
                .equals(name)) continue;
            BaseMetaTileEntity tile = new BaseMetaTileEntity();
            tile.setWorldObj(player.worldObj);
            tile.setInitialValuesAsNBT(null, (short) id);
            var controller = (MTEMultiBlockBase) tile.getMetaTileEntity();
            BaseMetaTileEntity energy = new BaseMetaTileEntity();
            energy.setWorldObj(player.worldObj);
            energy.setInitialValuesAsNBT(
                null,
                (short) ItemList.Hatch_Energy_LV.get(1)
                    .getItemDamage());
            controller.mEnergyHatches
                .add((gregtech.api.metatileentity.implementations.MTEHatchEnergy) energy.getMetaTileEntity());
            controller.setVoidingMode(gregtech.api.enums.VoidingMode.VOID_ALL);
            return controller;
        }
        throw new AssertionError("Missing test controller: " + name);
    }

    private static CrossRecipeMEPatternBuffer crossHatch(EntityPlayerMP player) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CrossRecipeMEPatternBuffer.id);
        return (CrossRecipeMEPatternBuffer) tile.getMetaTileEntity();
    }

    private static ItemStack mapPattern(RecipeMap<?> map, ItemStack input, ItemStack output) {
        ItemStack result = pattern(new IAEStack<?>[] { AEItemStack.create(input) }, output);
        PatternRecipeMap.write(result, map.unlocalizedName);
        return result;
    }

    private static void verifyWildcard(EntityPlayerMP player, RecipeMap<?> map) throws Exception {
        int cases = 0;
        HashSet<String> identities = new HashSet<>();
        for (boolean withFluid : new boolean[] { false, true }) {
            for (int circuit : new int[] { 4, 9 }) {
                ItemStack mold = (circuit == 4 ? ItemList.Shape_Mold_Plate : ItemList.Shape_Mold_Ingot).get(0);
                for (Materials material : new Materials[] { Materials.Iron, Materials.Copper }) {
                    GTValues.RA.stdBuilder()
                        .itemInputs(
                            GTOreDictUnificator.get(OrePrefixes.ingot, material, 2),
                            mold,
                            GTUtility.getIntegratedCircuit(circuit))
                        .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, material, 3))
                        .fluidInputs(
                            withFluid ? new FluidStack[] { new FluidStack(FluidRegistry.WATER, 100) }
                                : new FluidStack[0])
                        .duration(20)
                        .eut(8)
                        .addTo(map);
                }
                ItemStack wildcard = GTNRItemList.WildcardPattern.get(1);
                WildcardPatternGenerator.markAsWildcard(wildcard);
                var inputs = new ArrayList<com.xyp.gtneverreset.common.me.wildcard.model.IWildcardIOComponent>();
                inputs.add(new PrefixIOComponent(OrePrefixes.ingot, 2));
                inputs.add(new SimpleIOComponent(GTUtility.getIntegratedCircuit(circuit), 1));
                inputs.add(new SimpleIOComponent(mold, 1));
                if (withFluid) inputs
                    .add(new SimpleIOComponent(ItemFluidDrop.newStack(new FluidStack(FluidRegistry.WATER, 100)), 100));
                WildcardModelState.setInputs(wildcard, inputs);
                WildcardModelState.setOutputs(wildcard, List.of(new PrefixIOComponent(OrePrefixes.plate, 3)));
                WildcardModelState.setFilters(wildcard, List.of(new StringFilterComponent("^(Iron|Copper)$", true)));
                var cache = new WildcardPatternCache<List<ICraftingPatternDetails>>();
                var expanded = cache.get(
                    wildcard,
                    player.worldObj,
                    () -> WildcardPatternGenerator.generateAllDetails(wildcard, player.worldObj));
                require(expanded.size() == 2, "wildcard expands exactly iron and copper");
                require(
                    cache.get(
                        wildcard,
                        player.worldObj,
                        () -> { throw new AssertionError("Unchanged wildcard rebuilt"); }) == expanded,
                    "wildcard expansion cache reused");
                for (ICraftingPatternDetails detail : expanded) {
                    var runtime = checkWildcardDetail(player, map, detail, circuit, mold, withFluid, 1);
                    require(
                        identities.add(WildcardPatternGenerator.getGeneratedPatternId(runtime.getPattern())),
                        "wildcard material/circuit/mold/fluid identity is distinct");
                    cases++;
                }
                verifyWildcardConsumers(player, wildcard, circuit, mold);
                ItemStack restored = ItemStack.loadItemStackFromNBT(wildcard.writeToNBT(new NBTTagCompound()));
                var reload = WildcardPatternGenerator.generateAllDetails(restored, player.worldObj);
                require(reload.size() == 2, "wildcard NBT reload retains expansion");
                for (int i = 0; i < reload.size(); i++) require(
                    WildcardPatternGenerator.getGeneratedPatternId(
                        reload.get(i)
                            .getPattern())
                        .equals(
                            WildcardPatternGenerator.getGeneratedPatternId(
                                expanded.get(i)
                                    .getPattern())),
                    "wildcard NBT reload retains recipe identity");
                PatternMultiplierHelper.applyModification(wildcard, 1);
                var doubled = cache.get(
                    wildcard,
                    player.worldObj,
                    () -> WildcardPatternGenerator.generateAllDetails(wildcard, player.worldObj));
                require(doubled != expanded && doubled.size() == 2, "wildcard multiplier invalidates expansion cache");
                for (var detail : doubled) {
                    checkWildcardDetail(player, map, detail, circuit, mold, withFluid, 2);
                    cases++;
                }
                WildcardModelState.setFilters(wildcard, List.of(new StringFilterComponent("Iron", true, true)));
                require(
                    cache
                        .get(
                            wildcard,
                            player.worldObj,
                            () -> WildcardPatternGenerator.generateAllDetails(wildcard, player.worldObj))
                        .size() == 1,
                    "wildcard filter change invalidates cache");
            }
        }
        require(cases == 16, "all 16 wildcard material/configuration/quantity cases passed");
    }

    private static ICraftingPatternDetails checkWildcardDetail(EntityPlayerMP player, RecipeMap<?> map,
        ICraftingPatternDetails detail, int circuit, ItemStack mold, boolean fluid, int multiplier) {
        var runtime = CircuitPatternCodec.runtime(detail.getPattern(), player.worldObj);
        require(
            runtime != null && !WildcardPatternGenerator.isWildcardPattern(runtime.getPattern()),
            "wildcard becomes native AE runtime pattern");
        require(CircuitPatternCodec.circuit(runtime) == circuit, "wildcard preserves selected circuit");
        ItemStack[] molds = CircuitPatternCodec.molds(runtime);
        require(
            molds.length == 1 && GTUtility.areStacksEqual(molds[0], mold) && molds[0].stackSize == 0,
            "wildcard preserves non-consumed mold");
        for (var input : runtime.getAEInputs()) {
            require(
                !CircuitPatternCodec.isCircuit(input) && !CircuitPatternCodec.isMold(input, molds),
                "wildcard AE demand excludes circuit and mold");
            require(
                input.getStackSize() == multiplier * (input instanceof IAEFluidStack ? 100 : 2),
                "wildcard consumption multiplier");
        }
        require(runtime.getAEInputs().length == (fluid ? 2 : 1), "wildcard native item/fluid channels");
        ItemStack saved = ItemStack.loadItemStackFromNBT(
            runtime.getPattern()
                .writeToNBT(new NBTTagCompound()));
        var reloaded = CircuitPatternCodec.runtime(saved, player.worldObj);
        require(
            reloaded != null && runtime.equals(reloaded)
                && CircuitPatternCodec.circuit(reloaded) == circuit
                && CircuitPatternCodec.molds(reloaded).length == 1,
            "wildcard runtime survives CPU-style NBT reload");
        return runtime;
    }

    private static void verifyWildcardConsumers(EntityPlayerMP player, ItemStack wildcard, int circuit, ItemStack mold)
        throws Exception {
        TileEntityOverloadedProvider provider = new TileEntityOverloadedProvider();
        provider.setWorldObj(player.worldObj);
        Method decode = TileEntityOverloadedProvider.class.getDeclaredMethod("decodePatterns", ItemStack.class);
        decode.setAccessible(true);
        var decoded = (List<?>) decode.invoke(provider, wildcard.copy());
        require(decoded.size() == 2, "provider decoder expands wildcard");
        for (Object option : decoded) {
            var runtime = (ICraftingPatternDetails) option;
            require(
                runtime != null && CircuitPatternCodec.storedCircuit(runtime.getPattern()) == -1
                    && CircuitPatternCodec.storedMolds(runtime.getPattern()).length == 0
                    && runtime.getAEInputs().length >= 3,
                "provider keeps unannotated wildcard inputs without inferring tools");
        }

        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.xCoord = (int) player.posX;
        tile.yCoord = 100;
        tile.zCoord = (int) player.posZ;
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CircuitMEPatternBuffer.id);
        CircuitMEPatternBuffer hatch = (CircuitMEPatternBuffer) tile.getMetaTileEntity();
        Method create = CircuitMEPatternBuffer.class
            .getDeclaredMethod("createPatternSlot", ItemStack.class, NBTTagCompound.class);
        create.setAccessible(true);
        CircuitSlot slot = (CircuitSlot) create.invoke(hatch, wildcard.copy(), null);
        Method recipes = CircuitSlot.class.getDeclaredMethod("recipes");
        recipes.setAccessible(true);
        var options = (List<?>) recipes.invoke(slot);
        require(
            options.size() == 2 && slot.getItemInputs().length == 0,
            "hatch expands wildcard but empty buffer supplies no phantom inputs");
        Method insert = CircuitSlot.class.getDeclaredMethod(
            "insert",
            options.get(0)
                .getClass(),
            MEInventoryCrafting.class);
        insert.setAccessible(true);
        var first = (ICraftingPatternDetails) field(options.get(0), "details");
        require((boolean) insert.invoke(slot, options.get(0), delivery(first)), "hatch accepts wildcard consumables");
        require(
            !(boolean) insert
                .invoke(slot, options.get(1), delivery((ICraftingPatternDetails) field(options.get(1), "details"))),
            "hatch rejects mixing another wildcard material into occupied buffer");
        checkHatchInputs(slot, circuit, mold);
        NBTTagCompound saved = slot.writeToNBT(new NBTTagCompound());
        CircuitSlot restored = (CircuitSlot) create.invoke(hatch, wildcard.copy(), saved);
        require(first.equals(restored.getPatternDetails()), "hatch reload restores active wildcard recipe");
        checkHatchInputs(restored, circuit, mold);
        require(
            restored.getStoredItems()
                .size() == 1,
            "hatch stores no physical circuit or mold");
    }

    private static void verifySingleblock(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity tile = testMachine(player, new BaseMetaTileEntity());
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        OverloadedMachineConfiguration configuration = (OverloadedMachineConfiguration) machine;
        ItemStack manual = GTUtility.getIntegratedCircuit(17);
        manual.stackSize = 1;
        tile.setInventorySlotContents(machine.getCircuitSlot(), manual);
        manual = machine.getStackInSlot(machine.getCircuitSlot());
        OverloadedMachineMolds.set(machine, ItemList.Shape_Mold_Ingot.get(0));
        ItemStack plain = pattern(
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.clay_ball)) },
            new ItemStack(Items.diamond, 64));
        var details = CircuitPatternCodec.decode(plain, player.worldObj);
        var buffer = new OverloadedMachineBuffer();
        require(
            OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer),
            "singleblock accepts ordinary delivery without recipe/output/voltage/ratio matching");
        require(
            machine.getStackInSlot(machine.getCircuitSlot()) == manual
                && GTUtility.areStacksEqual(OverloadedMachineMolds.get(machine), ItemList.Shape_Mold_Ingot.get(0)),
            "ordinary delivery preserves manual circuit and mold");
        machine.mMaxProgresstime = 100;
        tile.disableWorking();
        require(
            OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer)
                && machine.getStackInSlot(machine.getInputSlot()).stackSize == 2,
            "ordinary delivery merges while running and disabled without same-recipe gate");

        ItemStack configured = metadata(plain, 4, ItemList.Shape_Mold_Plate.get(0));
        details = CircuitPatternCodec.decode(configured, player.worldObj);
        require(
            OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer)
                && machine.getStackInSlot(machine.getCircuitSlot())
                    .getItemDamage() == 4
                && GTUtility.areStacksEqual(OverloadedMachineMolds.get(machine), ItemList.Shape_Mold_Plate.get(0)),
            "metadata changes circuit and mold on occupied running singleblock");
        details = CircuitPatternCodec.decode(metadata(plain, 9, null), player.worldObj);
        require(
            OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer)
                && machine.getStackInSlot(machine.getCircuitSlot())
                    .getItemDamage() == 9
                && GTUtility.areStacksEqual(OverloadedMachineMolds.get(machine), ItemList.Shape_Mold_Plate.get(0)),
            "circuit-only metadata preserves mold");
        details = CircuitPatternCodec.decode(metadata(plain, -1, ItemList.Shape_Mold_Ingot.get(0)), player.worldObj);
        require(
            OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer)
                && machine.getStackInSlot(machine.getCircuitSlot())
                    .getItemDamage() == 9
                && GTUtility.areStacksEqual(OverloadedMachineMolds.get(machine), ItemList.Shape_Mold_Ingot.get(0)),
            "mold-only metadata preserves circuit");
        int before = machine.getStackInSlot(machine.getInputSlot()).stackSize;
        details = CircuitPatternCodec.decode(configured, player.worldObj);
        require(
            !OverloadedMachineInput.push(tile, details, delivery(details), machine.mMainFacing, buffer)
                && machine.getStackInSlot(machine.getInputSlot()).stackSize == before
                && machine.getStackInSlot(machine.getCircuitSlot())
                    .getItemDamage() == 9
                && GTUtility.areStacksEqual(OverloadedMachineMolds.get(machine), ItemList.Shape_Mold_Ingot.get(0)),
            "blocked native insertion side leaves inventory and configuration untouched");
        details = CircuitPatternCodec.decode(metadata(plain, 25, null), player.worldObj);
        require(
            !OverloadedMachineInput.push(tile, details, delivery(details), ForgeDirection.UP, buffer)
                && machine.getStackInSlot(machine.getInputSlot()).stackSize == before
                && machine.getStackInSlot(machine.getCircuitSlot())
                    .getItemDamage() == 9,
            "malformed metadata rejects atomically");

        BaseMetaTileEntity bulkTile = testMachine(player, new BaseMetaTileEntity());
        MTEBasicMachine bulkMachine = (MTEBasicMachine) bulkTile.getMetaTileEntity();
        OverloadedMachineConfiguration bulkConfiguration = (OverloadedMachineConfiguration) bulkMachine;
        details = CircuitPatternCodec.decode(
            pattern(
                new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.clay_ball))
                    .setStackSize(200) },
                new ItemStack(Items.paper)),
            player.worldObj);
        buffer = new OverloadedMachineBuffer();
        require(
            OverloadedMachineInput.push(bulkTile, details, delivery(details), ForgeDirection.UP, buffer)
                && bulkConfiguration.gtnr$hasPending()
                && !buffer.isEmpty(),
            "ordinary large delivery uses persistent overflow");
        require(
            !OverloadedMachineInput
                .push(bulkTile, details, delivery(details), ForgeDirection.UP, new OverloadedMachineBuffer()),
            "pending overflow retains exclusive delivery ownership");
        NBTTagCompound saved = new NBTTagCompound();
        bulkMachine.saveNBTData(saved);
        buffer.writeToNBT(saved);
        require(
            saved.getCompoundTag("OverloadedConfiguration")
                .getBoolean("Pending")
                && !saved.getCompoundTag("OverloadedConfiguration")
                    .getBoolean("Active"),
            "ordinary overflow persists without changing tool configuration");
        var loadedBuffer = new OverloadedMachineBuffer();
        loadedBuffer.readFromNBT(saved);
        var loadedTile = testMachine(player, new BaseMetaTileEntity());
        ((MTEBasicMachine) loadedTile.getMetaTileEntity()).loadNBTData(saved);
        require(
            ((OverloadedMachineConfiguration) loadedTile.getMetaTileEntity()).gtnr$hasPending()
                && loadedBuffer.getItems()
                    .get(0).stackSize
                    == buffer.getItems()
                        .get(0).stackSize,
            "ordinary overflow and ownership survive NBT reload");
        int delivered = 0;
        for (int cycle = 0; cycle < 8; cycle++) {
            for (int slot = bulkMachine.getInputSlot(); slot
                < bulkMachine.getInputSlot() + bulkMachine.mInputSlotCount; slot++) {
                ItemStack stack = bulkMachine.getStackInSlot(slot);
                if (stack != null) delivered += stack.stackSize;
                bulkTile.setInventorySlotContents(slot, null);
            }
            if (buffer.isEmpty()) break;
            OverloadedMachineInput.refill(bulkMachine, buffer);
        }
        require(
            delivered == 200 && buffer.isEmpty() && !bulkConfiguration.gtnr$hasPending(),
            "ordinary overflow refill delivers exactly 200 items with no loss or duplication");

        ItemStack physicalCircuit = GTUtility.getIntegratedCircuit(2);
        physicalCircuit.stackSize = 1;
        details = CircuitPatternCodec.decode(
            pattern(new IAEStack<?>[] { AEItemStack.create(physicalCircuit) }, new ItemStack(Items.book)),
            player.worldObj);
        require(
            OverloadedMachineInput.push(bulkTile, details, delivery(details), ForgeDirection.UP, buffer)
                && bulkMachine.getStackInSlot(bulkMachine.getCircuitSlot()) == null
                && bulkMachine.getStackInSlot(bulkMachine.getInputSlot()).stackSize == 1,
            "unannotated circuit input is delivered as an ordinary physical item");

        var fluidDetails = CircuitPatternCodec.decode(
            metadata(
                pattern(
                    new IAEStack<?>[] { AEFluidStack.create(Materials.Tin.getMolten(100)),
                        AEItemStack.create(new ItemStack(Items.clay_ball)) },
                    new ItemStack(Items.diamond)),
                4,
                ItemList.Shape_Mold_Plate.get(0)),
            player.worldObj);
        BaseMetaTileEntity fluidTile = testMachine(player, new BaseMetaTileEntity());
        FailingFillTile failTile = (FailingFillTile) testMachine(player, new FailingFillTile());
        var first = OverloadedMachineInput
            .prepare(fluidTile, fluidDetails, delivery(fluidDetails), ForgeDirection.UP, new OverloadedMachineBuffer());
        var second = OverloadedMachineInput
            .prepare(failTile, fluidDetails, delivery(fluidDetails), ForgeDirection.UP, new OverloadedMachineBuffer());
        require(first != null && second != null, "fluid and item deliveries prepare without recipe matching");
        failTile.rejectFill = true;
        require(
            !OverloadedMachineInput.commit(List.of(first, second))
                && ((MTEBasicMachine) fluidTile.getMetaTileEntity()).getFillableStack() == null
                && ((MTEBasicMachine) failTile.getMetaTileEntity()).getFillableStack() == null
                && fluidTile.getStackInSlot(((MTEBasicMachine) fluidTile.getMetaTileEntity()).getInputSlot()) == null
                && fluidTile.getStackInSlot(((MTEBasicMachine) fluidTile.getMetaTileEntity()).getCircuitSlot()) == null
                && OverloadedMachineMolds.get((MTEBasicMachine) fluidTile.getMetaTileEntity()) == null,
            "multi-target commit failure rolls back fluids and leaves items and tools untouched");
        failTile.rejectFill = false;
        require(
            OverloadedMachineInput
                .push(fluidTile, fluidDetails, delivery(fluidDetails), ForgeDirection.UP, new OverloadedMachineBuffer())
                && ((MTEBasicMachine) fluidTile.getMetaTileEntity()).getFillableStack().amount == 100,
            "singleblock accepts fluid and item delivery with explicit metadata");
        verifyWildcardMetadata(player);
    }

    private static void verifyWildcardMetadata(EntityPlayerMP player) throws Exception {
        HashSet<ICraftingPatternDetails> options = new HashSet<>();
        TileEntityOverloadedProvider provider = new TileEntityOverloadedProvider();
        provider.setWorldObj(player.worldObj);
        Method decode = TileEntityOverloadedProvider.class.getDeclaredMethod("decodePatterns", ItemStack.class);
        decode.setAccessible(true);
        for (int circuit : new int[] { 4, 9 }) {
            ItemStack wildcard = GTNRItemList.WildcardPattern.get(1);
            WildcardPatternGenerator.markAsWildcard(wildcard);
            WildcardModelState.setInputs(wildcard, List.of(new PrefixIOComponent(OrePrefixes.ingot, 2)));
            WildcardModelState.setOutputs(wildcard, List.of(new PrefixIOComponent(OrePrefixes.plate, 3)));
            WildcardModelState.setFilters(wildcard, List.of(new StringFilterComponent("^(Iron|Copper)$", true)));
            wildcard = metadata(wildcard, circuit, ItemList.Shape_Mold_Plate.get(0));
            var expanded = (List<ICraftingPatternDetails>) decode.invoke(provider, wildcard);
            require(expanded.size() == 2, "metadata wildcard provider expansion");
            for (var detail : expanded) {
                require(options.add(detail), "metadata wildcard configurations remain distinct AE options");
                require(
                    CircuitPatternCodec.storedCircuit(detail.getPattern()) == circuit
                        && detail.getAEInputs().length == 1,
                    "metadata wildcard retains tools outside AE demands");
                var tile = testMachine(player, new BaseMetaTileEntity());
                require(
                    OverloadedMachineInput
                        .push(tile, detail, delivery(detail), ForgeDirection.UP, new OverloadedMachineBuffer())
                        && tile.getStackInSlot(((MTEBasicMachine) tile.getMetaTileEntity()).getCircuitSlot())
                            .getItemDamage() == circuit,
                    "metadata wildcard pushes and configures singleblock");
                var restored = CircuitPatternCodec.decode(
                    ItemStack.loadItemStackFromNBT(
                        detail.getPattern()
                            .writeToNBT(new NBTTagCompound())),
                    player.worldObj);
                require(
                    detail.equals(restored) && detail.hashCode() == restored.hashCode(),
                    "metadata wildcard identity survives CPU-style reload");
            }
        }
        require(options.size() == 4, "all 4 metadata wildcard singleblock cases passed");
    }

    private static BaseMetaTileEntity testMachine(EntityPlayerMP player, BaseMetaTileEntity tile) throws Exception {
        tile.setWorldObj(player.worldObj);
        tile.xCoord = (int) player.posX;
        tile.yCoord = 100;
        tile.zCoord = (int) player.posZ;
        tile.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_LV_Assembler.get(1)
                .getItemDamage());
        for (Class<?> type = BaseMetaTileEntity.class; type != null; type = type.getSuperclass()) {
            try {
                Field timer = type.getDeclaredField("mTickTimer");
                timer.setAccessible(true);
                timer.setLong(tile, 10);
                return tile;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new AssertionError("GT tile startup timer missing");
    }

    private static ItemStack metadata(ItemStack plain, int circuit, ItemStack mold) {
        ItemStack result = plain.copy();
        if (circuit >= 0) result.getTagCompound()
            .setInteger(CircuitPatternCodec.CIRCUIT, circuit);
        if (mold != null) {
            NBTTagList molds = new NBTTagList();
            ItemStack copy = mold.copy();
            copy.stackSize = 0;
            molds.appendTag(copy.writeToNBT(new NBTTagCompound()));
            result.getTagCompound()
                .setTag(CircuitPatternCodec.MOLDS, molds);
        }
        return result;
    }

    private static final class FailingFillTile extends BaseMetaTileEntity {

        private boolean rejectFill;

        @Override
        public int fill(ForgeDirection side, FluidStack fluid, boolean doFill) {
            return rejectFill && doFill ? 0 : super.fill(side, fluid, doFill);
        }
    }

    private static void checkHatchInputs(CircuitSlot slot, int circuit, ItemStack mold) {
        boolean foundCircuit = false, foundMold = false;
        for (ItemStack input : slot.getItemInputs()) {
            if (GTUtility.areStacksEqual(input, GTUtility.getIntegratedCircuit(circuit)))
                foundCircuit = input.stackSize == 0;
            if (GTUtility.areStacksEqual(input, mold)) foundMold = input.stackSize == 0;
        }
        require(foundCircuit && foundMold, "hatch supplies the active wildcard circuit and mold as zero-size tools");
    }

    private static MEInventoryCrafting delivery(ICraftingPatternDetails details) {
        var inputs = details.getAEInputs();
        var delivery = new MEInventoryCrafting(new ContainerNull(), inputs.length, 1);
        for (int i = 0; i < inputs.length; i++) delivery.setInventorySlotContents(i, inputs[i].copy());
        return delivery;
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static ItemStack pattern(IAEStack<?>[] inputs, ItemStack output) {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (IAEStack<?> input : inputs) in.appendTag(input.toNBTGeneric());
        out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        result.setTagCompound(tag);
        return result;
    }

    private static void checkTools(ICraftingPatternDetails details) {
        require(details != null && CircuitPatternCodec.circuit(details) == 4, "stored circuit available to hatch");
        ItemStack[] molds = CircuitPatternCodec.molds(details);
        require(molds.length == 1 && molds[0].stackSize == 0, "stored mold available to hatch");
        for (IAEStack<?> input : details.getAEInputs()) require(
            !CircuitPatternCodec.isCircuit(input) && !CircuitPatternCodec.isMold(input, molds),
            "AE never requests virtual tools");
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("[circuit-pattern-qa] PASS " + message);
    }

    private void finish(Throwable error) {
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
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "circuit-pattern-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class PatternStructureController extends MTEIndustrialPackager {

        private List<IDualInputHatch> inputs = List.of();

        private PatternStructureController() {
            super("qa.pattern_families");
        }

        @Override
        public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
            mDualInputHatches.addAll(inputs);
        }
    }

    private static final class PreviewScreen extends GuiScreen {

        private final List<String> tooltip;

        private PreviewScreen(List<String> tooltip) {
            this.tooltip = new ArrayList<>(tooltip);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Cross-recipe pattern routing: PASS", width / 2, 35, 0x80FF80);
            drawCenteredString(fontRendererObj, "超级跨配方类型样板输入总成 / 通配符与电路模具回归测试", width / 2, 55, 0xFFFFFF);
            drawHoveringText(tooltip, 24, 110, fontRendererObj);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
