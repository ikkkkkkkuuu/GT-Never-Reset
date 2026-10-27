package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtneverreset.container.ContainerQuickEncodingTerminal;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.inventory.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.machine.ChatComponentInterfaceNameSuffix;
import com.xyp.gtneverreset.utils.machine.CircuitPatternCodec;
import com.xyp.gtneverreset.utils.machine.PatternMEOutput;
import com.xyp.gtneverreset.utils.text.AnimatedText;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.common.gui.modularui.util.PatternSlot;

@Mod(modid = "patterninputqa", name = "Pattern input QA", version = "1", dependencies = "after:gtneverreset")
public final class PatternInputClientChecks {

    private boolean started;
    private volatile boolean finished;
    private volatile int openGui;
    private volatile int capturedGui;
    private int ticks;
    private int frames;
    private int nextOpenTick;
    private SuperMTEHatchCraftingInputME superHatch;
    private CircuitMEPatternBuffer circuitHatch;
    private SuperMTEHatchCraftingInputSlave superMirror;
    private CircuitMEPatternMirror circuitMirror;
    private int controllerId;
    private String previousCredit;
    private long creditSampleTime;
    private boolean creditAnimationVerified;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "pattern-input-" + System.currentTimeMillis(),
                "Pattern input QA",
                new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++ticks > 1800) throw new AssertionError("GUI timeout");
            if (ticks == 1) setup(player);
            if (ticks == 120) {
                checks(player);
                superHatch.onRightclick(superHatch.getBaseMetaTileEntity(), player);
                openGui = 1;
            }
            if (capturedGui == openGui && capturedGui > 0 && capturedGui < 4 && nextOpenTick == 0) {
                player.closeScreen();
                nextOpenTick = ticks + 5;
            }
            if (nextOpenTick != 0 && ticks == nextOpenTick) {
                int nextGui = capturedGui + 1;
                if (nextGui == 2) circuitHatch.onRightclick(circuitHatch.getBaseMetaTileEntity(), player);
                else if (nextGui == 3) superMirror.onRightclick(superMirror.getBaseMetaTileEntity(), player);
                else circuitMirror.onRightclick(circuitMirror.getBaseMetaTileEntity(), player);
                openGui = nextGui;
                nextOpenTick = 0;
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        superHatch = (SuperMTEHatchCraftingInputME) place(
            world,
            player,
            0,
            GTNRItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        circuitHatch = (CircuitMEPatternBuffer) place(world, player, 2, GTNRItemList.CircuitMEPatternBuffer.get(1))
            .getMetaTileEntity();
        superHatch.setConnectsToAllSides(true);
        circuitHatch.setConnectsToAllSides(true);
        IDefinitions definitions = AEApi.instance()
            .definitions();
        for (int x = 0; x <= 2; x++) {
            world.setBlock(
                x,
                10,
                1,
                definitions.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
        }
        world.setBlock(
            1,
            10,
            2,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            0,
            10,
            2,
            definitions.blocks()
                .wireless()
                .maybeBlock()
                .get());
        TileWireless wireless = (TileWireless) world.getTileEntity(0, 10, 2);
        wireless.setOrientation(ForgeDirection.SOUTH, ForgeDirection.UP);
        wireless.onReady();
        for (SuperMTEHatchCraftingInputME hatch : new SuperMTEHatchCraftingInputME[] { superHatch, circuitHatch }) {
            hatch.setInventorySlotContents(0, pattern(1));
            hatch.setInventorySlotContents(539, pattern(24));
        }
        circuitHatch.setInventorySlotContents(1, pattern(2));
        player.playerNetServerHandler.setPlayerLocation(0, 10, -2, 0, 0);
    }

    private void checks(EntityPlayerMP player) throws Exception {
        require(superHatch.isActive() && circuitHatch.isActive(), "both assemblies join an active ME grid");
        for (SuperMTEHatchCraftingInputME hatch : new SuperMTEHatchCraftingInputME[] { superHatch, circuitHatch }) {
            require(
                hatch.getPatternCount() == 540 && hatch.getSizeInventory() == 551,
                "540 patterns and 551 total slots");
            require(
                hatch.getPatterns()
                    .getSizeInventory() == 540,
                "native terminal pattern inventory excludes auxiliary circuit, manual and mold slots");
            require(
                hatch.getCircuitSlot() == 540 && hatch.getManualSlotStart() == 541 && hatch.getMoldSlot() == 550,
                "circuit, nine manual slots and mold follow the 540 pattern slots");
            require(
                hatch.rows() == 60 && hatch.rowSize() == 9 && hatch.numSlots() == 540,
                "interface terminal exposes the populated last pattern row");
            require(
                AEApi.instance()
                    .registries()
                    .interfaceTerminal()
                    .getSupportedClasses()
                    .contains(hatch.getClass()),
                "concrete assembly type is registered in AE interface terminals");
            NBTTagCompound saved = new NBTTagCompound();
            ((BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).writeToNBT(saved);
            BaseMetaTileEntity restored = new BaseMetaTileEntity();
            restored.setWorldObj(player.worldObj);
            restored.readFromNBT(saved);
            SuperMTEHatchCraftingInputME restoredHatch = (SuperMTEHatchCraftingInputME) restored.getMetaTileEntity();
            require(
                restoredHatch.getPatternCount() == 540
                    && ItemStack.areItemStacksEqual(hatch.getStackInSlot(539), restoredHatch.getStackInSlot(539)),
                "tile NBT round trip preserves pattern index 539");
            hatch.setControllerRecipeMap(RecipeMaps.assemblerRecipes);
            require(
                hatch.getRawName()
                    .equals(RecipeMaps.assemblerRecipes.getDefaultRecipeCategory().unlocalizedName),
                "raw terminal name uses the controller recipe category key");
            hatch.setCustomName("QA custom assembly");
            require(
                hatch.getRawName()
                    .equals("QA custom assembly")
                    && hatch.getName()
                        .equals("QA custom assembly"),
                "explicit custom interface names override automatic controller names");
            hatch.setCustomName(null);
            hatch.setInventorySlotContents(hatch.getManualSlotStart(), new ItemStack(Items.dye, 1, 13));
            IChatComponent suffix = IChatComponent.Serializer
                .func_150699_a(IChatComponent.Serializer.func_150696_a(hatch.getNameSuffix()));
            require(
                suffix instanceof ChatComponentInterfaceNameSuffix
                    && ((ChatComponentInterfaceNameSuffix) suffix).getText(true)
                        .contains("13"),
                "assembly suffix survives native chat serialization and keeps manual item metadata");
            hatch.setInventorySlotContents(hatch.getManualSlotStart(), null);
        }
        superHatch.setInventorySlotContents(superHatch.getCircuitSlot(), GTUtility.getIntegratedCircuit(12));
        require(
            ((ChatComponentInterfaceNameSuffix) superHatch.getNameSuffix()).getText(true)
                .contains("12"),
            "own-name policy includes the super assembly's selected ghost circuit");
        superHatch.setInventorySlotContents(superHatch.getCircuitSlot(), null);
        ChatComponentInterfaceNameSuffix policies = new ChatComponentInterfaceNameSuffix(
            new ChatComponentText("[7]"),
            new ChatComponentText("[9]"));
        ChatComponentInterfaceNameSuffix decoded = (ChatComponentInterfaceNameSuffix) IChatComponent.Serializer
            .func_150699_a(IChatComponent.Serializer.func_150696_a(policies));
        require(
            decoded.getText(false)
                .equals("[7]")
                && decoded.getText(true)
                    .equals("[9]"),
            "both client naming policies survive suffix packets independently");
        checkController(player);
        checkCircuitBuffers();
        checkTerminal(player);
        checkMirrors(player);
    }

    private void checkController(EntityPlayerMP player) {
        for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
            if (!(GregTechAPI.METATILEENTITIES[id] instanceof MTEMultiBlockBase)) continue;
            MTEMultiBlockBase candidate = (MTEMultiBlockBase) GregTechAPI.METATILEENTITIES[id];
            if (candidate.getRecipeMap() == null || candidate.getRecipeMap()
                .getDefaultRecipeCategory() == null) continue;
            MTEMultiBlockBase controller = (MTEMultiBlockBase) place(
                player.worldObj,
                player,
                5,
                candidate.getStackForm(1)).getMetaTileEntity();
            controllerId = id;
            require(
                controller.addToMachineList(superHatch.getBaseMetaTileEntity(), 0)
                    && controller.addToMachineList(circuitHatch.getBaseMetaTileEntity(), 0),
                "native multiblock accepts both new assemblies");
            String key = controller.getRecipeMap()
                .getDefaultRecipeCategory().unlocalizedName;
            require(
                superHatch.getRawName()
                    .equals(key)
                    && circuitHatch.getRawName()
                        .equals(key),
                "structure attachment updates both automatic interface names");
            return;
        }
        throw new AssertionError("no registered multiblock with a recipe map");
    }

    private void checkMirrors(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        superMirror = (SuperMTEHatchCraftingInputSlave) place(
            world,
            player,
            6,
            GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1)).getMetaTileEntity();
        circuitMirror = (CircuitMEPatternMirror) place(world, player, 8, GTNRItemList.CircuitMEPatternMirror.get(1))
            .getMetaTileEntity();
        MTEMultiBlockBase unlinkedController = (MTEMultiBlockBase) place(
            world,
            player,
            14,
            GregTechAPI.METATILEENTITIES[controllerId].getStackForm(1)).getMetaTileEntity();
        require(
            superMirror.getMaster() == null && circuitMirror.getMaster() == null,
            "both mirrors begin unlinked before structure validation");
        require(
            unlinkedController.addInputHatchToMachineList(superMirror.getBaseMetaTileEntity(), 0)
                && unlinkedController.addInputHatchToMachineList(circuitMirror.getBaseMetaTileEntity(), 0),
            "input-hatch adder accepts both unlinked mirror types");
        require(
            HatchElement.InputHatch.count(unlinkedController) == 2,
            "input-hatch structure counter counts both unlinked mirrors");
        ItemStack held = player.getHeldItem();
        ItemStack dataStick = ItemList.Tool_DataStick.get(1);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, dataStick);
        try {
            superHatch.onLeftclick(superHatch.getBaseMetaTileEntity(), player);
            require(
                "CraftingInputBuffer".equals(
                    dataStick.getTagCompound()
                        .getString("type"))
                    && dataStick.getTagCompound()
                        .getInteger("x") == 0,
                "super master writes native data-stick link coordinates");
            require(
                superMirror.onRightclick(superMirror.getBaseMetaTileEntity(), player)
                    && superMirror.getMaster() == superHatch,
                "data stick binds the super mirror to its master");
            circuitHatch.onLeftclick(circuitHatch.getBaseMetaTileEntity(), player);
            require(
                circuitMirror.onRightclick(circuitMirror.getBaseMetaTileEntity(), player)
                    && circuitMirror.getMaster() == circuitHatch,
                "data stick binds the dedicated circuit mirror");
            superHatch.onLeftclick(superHatch.getBaseMetaTileEntity(), player);
            require(
                circuitMirror.onRightclick(circuitMirror.getBaseMetaTileEntity(), player)
                    && circuitMirror.getMaster() == circuitHatch,
                "dedicated circuit mirror rejects a super master without losing its valid link");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        }
        SuperMTEHatchCraftingInputME[] masters = { superHatch, circuitHatch };
        SuperMTEHatchCraftingInputSlave[] mirrors = { superMirror, circuitMirror };
        for (int index = 0; index < masters.length; index++) {
            SuperMTEHatchCraftingInputME master = masters[index];
            SuperMTEHatchCraftingInputSlave mirror = mirrors[index];
            require(
                mirror.inventories()
                    .next()
                    == master.inventories()
                        .next(),
                "mirror shares the exact live pattern inventory object");
            master.setInventorySlotContents(master.getManualSlotStart(), new ItemStack(Items.dye, 1, 7 + index));
            require(
                Arrays.equals(mirror.getSharedItems(), master.getSharedItems()),
                "mirror shares manual and virtual tool inputs with its master");
            NBTTagCompound display = new NBTTagCompound();
            mirror.getWailaNBTData(
                player,
                (BaseMetaTileEntity) mirror.getBaseMetaTileEntity(),
                display,
                world,
                mirror.getBaseMetaTileEntity()
                    .getXCoord(),
                10,
                0);
            require(
                display.getString("masterName")
                    .equals(master.getName())
                    && display.getString("masterName")
                        .contains(Integer.toString(7 + index)),
                "mirror name display reflects its master's automatic name and manual-item suffix");
            master.setInventorySlotContents(master.getManualSlotStart(), null);
            NBTTagCompound saved = new NBTTagCompound();
            ((BaseMetaTileEntity) mirror.getBaseMetaTileEntity()).writeToNBT(saved);
            BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
            restoredTile.setWorldObj(world);
            restoredTile.readFromNBT(saved);
            SuperMTEHatchCraftingInputSlave restored = (SuperMTEHatchCraftingInputSlave) restoredTile
                .getMetaTileEntity();
            require(restored.getClass() == mirror.getClass(), "mirror NBT preserves its concrete machine type");
            restored.onPostTick(restoredTile, 100);
            require(
                restored.getMaster() == master && restored.inventories()
                    .next()
                    == master.inventories()
                        .next(),
                "mirror NBT reconnects to the original live pattern reserve");
            master.removeProxyHatch(restored);
        }
        checkMirrorOutputs(player);
        SuperMTEHatchCraftingInputME temporaryMaster = (SuperMTEHatchCraftingInputME) place(
            world,
            player,
            12,
            GTNRItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        SuperMTEHatchCraftingInputSlave temporaryMirror = (SuperMTEHatchCraftingInputSlave) place(
            world,
            player,
            13,
            GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1)).getMetaTileEntity();
        require(
            temporaryMirror.trySetMasterFromCoord(12, 10, 0) == temporaryMaster,
            "disposable mirror links before master removal");
        world.setBlockToAir(12, 10, 0);
        require(
            temporaryMirror.getMaster() == null && !temporaryMirror.inventories()
                .hasNext(),
            "removing a master disconnects its mirror and empties the shared inventory view");
    }

    private void checkMirrorOutputs(EntityPlayerMP player) {
        MTEMultiBlockBase controller = (MTEMultiBlockBase) place(
            player.worldObj,
            player,
            10,
            GregTechAPI.METATILEENTITIES[controllerId].getStackForm(1)).getMetaTileEntity();
        require(
            controller.addToMachineList(circuitMirror.getBaseMetaTileEntity(), 0),
            "controller accepts the dedicated circuit mirror");
        PatternMEOutput output = circuitHatch.getMEOutput();
        require(
            controller.getOutputBusses()
                .size() == 1
                && controller.getOutputBusses()
                    .get(0) == output.itemOutput
                && controller.getOutputHatches()
                    .size() == 1
                && controller.getOutputHatches()
                    .get(0) == output.fluidOutput,
            "mirror exposes its master's exact item and fluid output providers");
        require(
            controller.addToMachineList(circuitHatch.getBaseMetaTileEntity(), 0)
                && controller.addToMachineList(circuitMirror.getBaseMetaTileEntity(), 0),
            "controller accepts master plus repeated mirror aliases");
        require(
            controller.getOutputBusses()
                .size() == 1
                && controller.getOutputHatches()
                    .size() == 1,
            "master and mirror output aliases count each canonical destination once");
        ItemEjectionHelper items = new ItemEjectionHelper(controller.getOutputBusses(), true);
        FluidEjectionHelper fluids = new FluidEjectionHelper(controller.getOutputHatches(), true);
        require(
            items.ejectStack(new ItemStack(Items.clay_ball, 7)) == 7
                && fluids.ejectStack(new FluidStack(FluidRegistry.LAVA, 250)) == 250,
            "native GT ejection routes item and fluid products through a mirror");
        items.commit();
        fluids.commit();
        NBTTagCompound saved = new NBTTagCompound();
        output.save(saved);
        require(
            saved.getTagList("patternMEItemOutput", 10)
                .getCompoundTagAt(0)
                .getLong("amount") == 7
                && saved.getTagList("patternMEFluidOutput", 10)
                    .getCompoundTagAt(0)
                    .getLong("amount") == 250,
            "mirror products persist in the master's independent output caches");
    }

    private void checkCircuitBuffers() {
        List<SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME>> slots = new ArrayList<>();
        circuitHatch.inventories()
            .forEachRemaining(slots::add);
        SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> first = slots.get(0);
        SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> second = slots.get(1);
        ICraftingPatternDetails details = first.getPatternDetails();
        require(
            details.getCondensedAEInputs().length == 2 && !CircuitPatternCodec.isCircuit(details.getAEInputs()[0]),
            "native AE inputs omit encoded virtual circuits");
        require(
            !details.equals(second.getPatternDetails()),
            "different encoded circuits keep distinct AE recipe identities");
        require(
            circuitHatch.pushPattern(details, materials(3))
                && circuitHatch.pushPattern(second.getPatternDetails(), materials(3)),
            "CPU pushes circuit-free item and fluid batches");
        require(
            first.getItemInputs()[1].getItemDamage() == 1 && second.getItemInputs()[1].getItemDamage() == 2,
            "each live recipe buffer exposes its own circuit");
        NBTTagCompound saved = new NBTTagCompound();
        ((BaseMetaTileEntity) circuitHatch.getBaseMetaTileEntity()).writeToNBT(saved);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(
            circuitHatch.getBaseMetaTileEntity()
                .getWorld());
        restored.readFromNBT(saved);
        SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> firstRestored = ((CircuitMEPatternBuffer) restored
            .getMetaTileEntity()).inventories()
                .next();
        require(
            firstRestored.getItemInputs()[0].stackSize == 3 && firstRestored.getFluidInputs()[0].amount == 3
                && firstRestored.getItemInputs()[1].getItemDamage() == 1,
            "NBT reload retains buffered quantities, fluid and virtual circuit");
    }

    private void checkTerminal(EntityPlayerMP player) throws Exception {
        ItemStack terminal = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, terminal);
        DualTerminalGuiObject host = new DualTerminalGuiObject(
            AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(terminal),
            terminal,
            player,
            player.worldObj,
            0);
        TileWireless wireless = (TileWireless) player.worldObj.getTileEntity(0, 10, 2);
        IGrid grid = superHatch.getProxy()
            .getGrid();
        require(
            wireless.getActionableNode()
                .getGrid() == grid,
            "wireless terminal access point shares the assemblies' grid");
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : wireless);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        Field delegateField = ContainerQuickEncodingTerminal.class.getDeclaredField("interfaceDelegate");
        delegateField.setAccessible(true);
        ContainerInterfaceTerminal delegate = (ContainerInterfaceTerminal) delegateField.get(container);
        Field trackedField = ContainerInterfaceTerminal.class.getDeclaredField("tracked");
        trackedField.setAccessible(true);
        Map<?, ?> tracked = (Map<?, ?>) trackedField.get(delegate);
        for (SuperMTEHatchCraftingInputME hatch : new SuperMTEHatchCraftingInputME[] { superHatch, circuitHatch }) {
            Object entry = tracked.get(hatch);
            require(entry != null, "dual interface terminal discovers assembly through native AE registry");
            require(
                (Integer) field(entry, "numSlots") == 540 && field(entry, "name").equals(hatch.getRawName()),
                "dual terminal synchronizes all 540 pattern slots and the automatic recipe category name");
        }
        container.onContainerClosed(player);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || openGui == 0) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            checkClientTooltips(mc);
            if (capturedGui == 4 && creditAnimationVerified) {
                finish("PASS");
                return;
            }
            if (openGui == capturedGui || ++frames < 40) return;
            if (!(mc.currentScreen instanceof GuiContainerWrapper)) return;
            GuiContainerWrapper wrapper = (GuiContainerWrapper) mc.currentScreen;
            require(
                count(
                    wrapper.getScreen()
                        .getMainPanel())
                    == 540,
                "GUI contains exactly 540 pattern widgets");
            String[] screenshots = { "super-pattern-input.png", "circuit-pattern-input.png", "super-pattern-mirror.png",
                "circuit-pattern-mirror.png" };
            ScreenShotHelper.saveScreenshot(
                new File("."),
                screenshots[openGui - 1],
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            frames = 0;
            capturedGui = openGui;
            if (capturedGui == 4 && creditAnimationVerified) finish("PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private void checkClientTooltips(Minecraft mc) {
        if (previousCredit == null) {
            ItemStack[] stacks = { GTNRItemList.SuperMTEHatchCraftingInputME.get(1),
                GTNRItemList.CircuitMEPatternBuffer.get(1), GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1),
                GTNRItemList.CircuitMEPatternMirror.get(1) };
            for (ItemStack stack : stacks) {
                boolean foundCredit = false;
                for (Object entry : stack.getTooltip(mc.thePlayer, false)) {
                    String line = String.valueOf(entry);
                    String plain = EnumChatFormatting.getTextWithoutFormattingCodes(line);
                    if (plain.contains("Added by:") && plain.contains("GT Never Reset")) {
                        foundCredit = !line.equals(plain);
                    }
                }
                require(foundCredit, "rendered machine tooltip contains the formatted Added by GT Never Reset credit");
            }
            previousCredit = AnimatedText.GT_NEVER_RESET.get();
            creditSampleTime = System.nanoTime();
        } else if (!creditAnimationVerified && System.nanoTime() - creditSampleTime >= 100_000_000L) {
            if (!previousCredit.equals(AnimatedText.GT_NEVER_RESET.get())) {
                creditAnimationVerified = true;
                require(true, "GT Never Reset tooltip credit animates between client frames");
            } else if (System.nanoTime() - creditSampleTime > 2_000_000_000L) {
                throw new AssertionError("tooltip credit failed to animate within two seconds");
            }
        }
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static MEInventoryCrafting materials(int amount) {
        IAEStack<?>[] inputs = { AEItemStack.create(new ItemStack(Items.nether_star, amount)),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, amount)) };
        MEInventoryCrafting inventory = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, inputs.length, 1);
        for (int i = 0; i < inputs.length; i++) inventory.setInventorySlotContents(i, inputs[i]);
        return inventory;
    }

    private static ItemStack pattern(int circuit) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagList in = new NBTTagList();
        NBTTagList out = new NBTTagList();
        for (IAEStack<?> input : new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.nether_star)),
            AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)),
            AEItemStack.create(GTUtility.getIntegratedCircuit(circuit)) }) {
            NBTTagCompound entry = new NBTTagCompound();
            input.writeToNBTGeneric(entry);
            in.appendTag(entry);
        }
        NBTTagCompound output = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.apple))
            .writeToNBTGeneric(output);
        out.appendTag(output);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        tag.setBoolean("beSubstitute", true);
        pattern.setTagCompound(tag);
        return pattern;
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static int count(IWidget widget) {
        int result = widget instanceof PatternSlot ? 1 : 0;
        for (IWidget child : widget.getChildren()) result += count(child);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PATTERN_INPUT_QA: " + message);
    }

    private void finish(String result) {
        try {
            Files.write(new File("result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        finished = true;
        Minecraft.getMinecraft()
            .shutdown();
    }
}
