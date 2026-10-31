package com.xyp.gtneverreset.qa;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineBuffer;
import com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineInput;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.container.ContainerNull;
import appeng.tile.storage.TileDrive;
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
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMaps;

@Mod(modid = "overloadedgtqa", name = "Overloaded GT crafting QA", version = "1", dependencies = "after:gtneverreset")
public final class OverloadedGtCraftingChecks {

    private boolean started, finished, sawProcessing;
    private int ticks, stage;
    private volatile int screenshot;
    private TileEntityOverloadedProvider provider;
    private BaseMetaTileEntity bender;
    private final BaseMetaTileEntity[] benders = new BaseMetaTileEntity[3];
    private final FailingFluidTile[] fluidTargets = new FailingFluidTile[2];
    private final boolean[] processed = new boolean[3];
    private Future<ICraftingJob> job;
    private ICraftingLink link;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(FailingFluidTile.class, "gtnrFluidCommitQa");
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.ingameGUI.getChatGUI()
            .clearChatMessages();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "overloaded-gt-" + System.currentTimeMillis(),
                "Overloaded GT crafting QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            if (++ticks == 30) setup(world, player);
            if (bender == null) return;
            // Test power supply only: machine recipe execution and inventories remain untouched.
            for (int i = 0; i < benders.length; i++) {
                benders[i].setStoredEU(benders[i].getEUCapacity());
                processed[i] |= benders[i].isActive();
            }
            if (bender.isActive() && !sawProcessing) {
                sawProcessing = true;
                log("GT bender is processing inputs delivered by the provider");
                screenshot = 1;
            }
            if (ticks < 120) return;
            if (ticks > 1800) throw new AssertionError(
                "timeout stage=" + stage
                    + " active="
                    + provider.getProxy()
                        .isActive()
                    + " stone="
                    + stored(Blocks.stone)
                    + " cobble="
                    + stored(Blocks.cobblestone));
            if (stage == 0) {
                verifyBatchDistribution();
                verifyFluidCommit(world);
                require(
                    provider.getProxy()
                        .isActive(),
                    "provider has powered AE channel");
                require(
                    !provider.getProxy()
                        .getCrafting()
                        .getCpus()
                        .isEmpty(),
                    "real crafting CPU is online");
                require(
                    provider.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .injectItems(
                            AEItemStack.create(new ItemStack(Blocks.cobblestone, 4)),
                            Actionable.MODULATE,
                            new MachineSource(provider))
                        == null,
                    "four cobblestone stored in ME disk");
                require(stored(Blocks.stone) == 0, "no stone exists before order");
                job = provider.getProxy()
                    .getCrafting()
                    .beginCraftingJob(
                        world,
                        provider.getProxy()
                            .getGrid(),
                        new MachineSource(provider),
                        AEItemStack.create(new ItemStack(Blocks.stone, 4)),
                        null);
                stage = 1;
            } else if (stage == 1 && job.isDone()) {
                require(
                    !job.get()
                        .isSimulation(),
                    "AE calculated processing job with available ingredients");
                link = provider.getProxy()
                    .getCrafting()
                    .submitJob(job.get(), null, null, false, new MachineSource(provider));
                require(link != null, "AE CPU accepted order for four stone");
                stage = 2;
            } else if (stage == 2 && stored(Blocks.stone) == 4
                && provider.getProxy()
                    .getCrafting()
                    .getCpus()
                    .stream()
                    .noneMatch(cpu -> cpu.isBusy())) {
                        require(!link.isCanceled(), "AE CPU finished the standalone order without cancellation");
                        require(sawProcessing, "GT machine actually ran");
                        for (boolean ran : processed) require(ran, "each connected machine processed the AE order");
                        require(
                            stored(Blocks.stone) == 4 && stored(Blocks.cobblestone) == 0,
                            "all four products returned to ME and all four inputs consumed");
                        for (int slot = 0; slot < bender.getSizeInventory(); slot++) require(
                            bender.getStackInSlot(slot) == null,
                            "machine slot " + slot + " empty after recovery");
                        for (int slot = TileEntityOverloadedProvider.PATTERNS; slot
                            < provider.getSizeInventory(); slot++)
                            require(provider.getStackInSlot(slot) == null, "return buffer " + slot + " empty");
                        stage = 3;
                        screenshot = 2;
                    } else
                if (stage == 3 && screenshot == 0) finish(null);
            if (ticks % 100 == 0)
                log("stage=" + stage + " active=" + bender.isActive() + " stone=" + stored(Blocks.stone));
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        ItemStack circuit = gregtech.api.util.GTUtility.getIntegratedCircuit(1);
        circuit.stackSize = 0;
        require(
            !GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Blocks.cobblestone), circuit)
                .itemOutputs(new ItemStack(Blocks.stone))
                .duration(20)
                .eut(8)
                .addTo(RecipeMaps.benderRecipes)
                .isEmpty(),
            "register deterministic batch test recipe");
        var defs = AEApi.instance()
            .definitions();
        world.setWorldTime(6000);
        for (int x = 0; x < 4; x++) {
            world.setBlock(
                x,
                10,
                1,
                defs.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                defs.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
        }
        world.setBlock(
            0,
            10,
            0,
            net.minecraft.block.Block.getBlockFromItem(
                GTNRItemList.OverloadedWirelessPatternProvider.get(1)
                    .getItem()));
        provider = (TileEntityOverloadedProvider) world.getTileEntity(0, 10, 0);
        provider.setOwnerName(player.getCommandSenderName());
        world.setBlock(
            1,
            10,
            0,
            defs.blocks()
                .drive()
                .maybeBlock()
                .get());
        ((TileDrive) world.getTileEntity(1, 10, 0)).getInternalInventory()
            .setInventorySlotContents(
                0,
                defs.items()
                    .cell1k()
                    .maybeStack(1)
                    .get());
        world.setBlock(
            2,
            10,
            0,
            defs.blocks()
                .craftingStorage1k()
                .maybeBlock()
                .get());
        world.setBlock(
            3,
            10,
            0,
            defs.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(6, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        bender = (BaseMetaTileEntity) world.getTileEntity(6, 10, 0);
        bender.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_LV_Bender.get(1)
                .getItemDamage());
        bender.setFrontFacing(ForgeDirection.NORTH);
        bender.setOwnerName(player.getCommandSenderName());
        bender.enableWorking();
        require(
            provider.bind(new PackagedTarget(world.provider.dimensionId, 6, 10, 0, ForgeDirection.UP.ordinal())),
            "wireless target is GT LV bender top face");
        benders[0] = bender;
        for (int i = 1; i < benders.length; i++) {
            world.setBlock(6 + i, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity target = (BaseMetaTileEntity) world.getTileEntity(6 + i, 10, 0);
            target.setInitialValuesAsNBT(
                null,
                (short) ItemList.Machine_LV_Bender.get(1)
                    .getItemDamage());
            target.setFrontFacing(ForgeDirection.NORTH);
            target.setOwnerName(player.getCommandSenderName());
            target.enableWorking();
            benders[i] = target;
            require(
                provider
                    .bind(new PackagedTarget(world.provider.dimensionId, 6 + i, 10, 0, ForgeDirection.UP.ordinal())),
                "bind extra bender " + i);
        }
        for (int i = 0; i < fluidTargets.length; i++) {
            world.setBlock(12 + i, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            fluidTargets[i] = new FailingFluidTile();
            world.setTileEntity(12 + i, 10, 0, fluidTargets[i]);
            fluidTargets[i].setInitialValuesAsNBT(
                null,
                (short) ItemList.Machine_LV_ChemicalBath.get(1)
                    .getItemDamage());
            fluidTargets[i].setFrontFacing(ForgeDirection.NORTH);
            fluidTargets[i].enableWorking();
        }
        ItemStack pattern = defs.items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList input = new NBTTagList(), output = new NBTTagList();
        input.appendTag(new ItemStack(Blocks.cobblestone).writeToNBT(new NBTTagCompound()));
        output.appendTag(new ItemStack(Blocks.stone).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", input);
        tag.setTag("out", output);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        provider.setInventorySlotContents(0, pattern);
        player.capabilities.isFlying = true;
        player.inventory.setInventorySlotContents(0, null);
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(3, 13, -6, 0, 25);
    }

    private long stored(net.minecraft.block.Block block) throws Exception {
        var stack = provider.getProxy()
            .getStorage()
            .getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(new ItemStack(block)));
        return stack == null ? 0 : stack.getStackSize();
    }

    @SuppressWarnings("unchecked")
    private void verifyBatchDistribution() throws Exception {
        var field = TileEntityOverloadedProvider.class.getDeclaredField("patterns");
        field.setAccessible(true);
        ICraftingPatternDetails details = ((Set<ICraftingPatternDetails>) field.get(provider)).iterator()
            .next();
        ItemStack encoded = provider.getStackInSlot(0)
            .copy();
        MEInventoryCrafting table = batch(10, 10);
        require(provider.pushPattern(details, table), "round robin accepts ten crafts");
        int[] first = deliveryCounts();
        requireCounts(first, 3, 3, 4);
        require(
            table.getAEStackInSlot(0)
                .getStackSize() == 10,
            "dispatch preserves the CPU delivery table");
        clearDeliveries(encoded);
        require(provider.pushPattern(details, batch(10, 10)), "next uneven batch accepted");
        int[] second = deliveryCounts();
        requireCounts(second, 3, 3, 4);
        require(!Arrays.equals(first, second), "remainder rotates between machines");
        clearDeliveries(encoded);
        require(provider.pushPattern(details, batch(2, 2)), "batch smaller than machine count accepted");
        requireCounts(deliveryCounts(), 0, 1, 1);
        clearDeliveries(encoded);
        benders[1].disableWorking();
        require(provider.pushPattern(details, batch(10, 10)), "disabled machine is excluded");
        requireCounts(deliveryCounts(), 0, 5, 5);
        require(deliveryCounts()[1] == 0, "disabled machine receives no materials");
        clearDeliveries(encoded);
        for (BaseMetaTileEntity target : benders) target.disableWorking();
        require(!provider.pushPattern(details, batch(10, 10)), "all unavailable rejects the entire batch");
        requireCounts(deliveryCounts(), 0, 0, 0);
        clearDeliveries(encoded);
        require(!provider.pushPattern(details, batch(7, 10)), "fractional craft inputs are rejected");
        requireCounts(deliveryCounts(), 0, 0, 0);
        require(provider.pushPattern(details, batch(200, 200)), "large batch is distributed with overflow");
        requireCounts(deliveryCounts(), 66, 67, 67);
        NBTTagCompound saved = new NBTTagCompound();
        provider.writeToNBT(saved);
        TileEntityOverloadedProvider restored = new TileEntityOverloadedProvider();
        restored.readFromNBT(saved);
        NBTTagCompound reloaded = new NBTTagCompound();
        restored.writeToNBT(reloaded);
        require(
            saved.getTagList("Targets", 10)
                .equals(reloaded.getTagList("Targets", 10)),
            "distributed overflow and return tracking survive reload");
        clearDeliveries(encoded);
        provider.toggleDistribution();
        require(provider.pushPattern(details, batch(200, 200)), "fill-first still accepts a complete batch");
        requireCounts(deliveryCounts(), 0, 0, 200);
        clearDeliveries(encoded);
        provider.toggleDistribution();
    }

    private static MEInventoryCrafting batch(int items, long crafts) {
        MEInventoryCrafting table = new MEInventoryCrafting(new ContainerNull(), 1, 1);
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(Blocks.cobblestone, items)));
        ((LargeInventoryCrafting) table).setAssemblerSize(crafts);
        return table;
    }

    private void verifyFluidCommit(World world) {
        require(
            !GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Items.nether_star))
                .fluidInputs(new FluidStack(FluidRegistry.WATER, 100))
                .itemOutputs(new ItemStack(Items.diamond))
                .duration(20)
                .eut(8)
                .addTo(RecipeMaps.chemicalBathRecipes)
                .isEmpty(),
            "register deterministic fluid test recipe");
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList(), outputs = new NBTTagList();
        NBTTagCompound itemTag = new NBTTagCompound(), fluidTag = new NBTTagCompound(),
            outputTag = new NBTTagCompound();
        AEItemStack.create(new ItemStack(Items.nether_star))
            .writeToNBTGeneric(itemTag);
        AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 100))
            .writeToNBTGeneric(fluidTag);
        AEItemStack.create(new ItemStack(Items.diamond))
            .writeToNBTGeneric(outputTag);
        inputs.appendTag(itemTag);
        inputs.appendTag(fluidTag);
        outputs.appendTag(outputTag);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        var details = CircuitPatternCodec.runtime(encoded, world);
        MEInventoryCrafting table = new MEInventoryCrafting(new ContainerNull(), 2, 1);
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(Items.nether_star)));
        table.setInventorySlotContents(1, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 100)));
        FailingFluidTile[] targets = fluidTargets;
        OverloadedMachineBuffer[] buffers = { new OverloadedMachineBuffer(), new OverloadedMachineBuffer() };
        var prepared = new java.util.ArrayList<OverloadedMachineInput.PreparedInput>();
        for (int i = 0; i < targets.length; i++) {
            var input = OverloadedMachineInput.prepare(targets[i], details, table, ForgeDirection.UP, buffers[i]);
            require(input != null, "fluid delivery prepares without committing: " + i);
            prepared.add(input);
        }
        targets[1].rejectFill = true;
        require(!OverloadedMachineInput.commit(prepared), "late fluid rejection rejects the entire batch");
        for (int i = 0; i < targets.length; i++) {
            MTEBasicMachine machine = (MTEBasicMachine) targets[i].getMetaTileEntity();
            require(
                machine.getFillableStack() == null && buffers[i].isEmpty()
                    && Arrays.stream(machine.mInventory)
                        .allMatch(java.util.Objects::isNull),
                "late rejection restores previous machines, inputs and buffers: " + i);
        }
        targets[1].rejectFill = false;
        require(OverloadedMachineInput.commit(prepared), "restored fluid batch retries successfully");
        for (FailingFluidTile target : targets) {
            MTEBasicMachine machine = (MTEBasicMachine) target.getMetaTileEntity();
            require(
                machine.getFillableStack().amount == 100
                    && machine.getStackInSlot(machine.getInputSlot()).stackSize == 1,
                "retry commits exact fluid and item quantities once");
        }
    }

    private int[] deliveryCounts() {
        int[] counts = new int[benders.length];
        NBTTagCompound saved = new NBTTagCompound();
        provider.writeToNBT(saved);
        NBTTagList targets = saved.getTagList("Targets", 10);
        for (int i = 0; i < counts.length; i++) {
            MTEBasicMachine machine = (MTEBasicMachine) benders[i].getMetaTileEntity();
            for (int slot = machine.getInputSlot(); slot < machine.getInputSlot() + machine.mInputSlotCount; slot++) {
                ItemStack input = machine.getStackInSlot(slot);
                if (input != null) counts[i] += input.stackSize;
            }
            NBTTagList pending = targets.getCompoundTagAt(i)
                .getCompoundTag("OverloadedInputBuffer")
                .getTagList("Items", 10);
            for (int item = 0; item < pending.tagCount(); item++) counts[i] += pending.getCompoundTagAt(item)
                .getInteger("Quantity");
        }
        return counts;
    }

    private static void requireCounts(int[] counts, int... expected) {
        counts = counts.clone();
        Arrays.sort(counts);
        require(Arrays.equals(counts, expected), "exact distributed quantities " + Arrays.toString(expected));
    }

    private void clearDeliveries(ItemStack encoded) {
        provider.takeDrops();
        provider.setInventorySlotContents(0, encoded.copy());
        for (BaseMetaTileEntity target : benders) {
            MTEBasicMachine machine = (MTEBasicMachine) target.getMetaTileEntity();
            Arrays.fill(machine.mInventory, null);
            target.enableWorking();
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screenshot == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            new File("."),
            "overloaded-gt-" + screenshot + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        screenshot = 0;
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        finished = true;
        log(failure == null ? "PASS" : "FAIL");
        Minecraft.getMinecraft()
            .func_152344_a(
                () -> Minecraft.getMinecraft()
                    .shutdown());
    }

    private static void log(String message) {
        System.out.println("OVERLOADED_GT_QA: " + message);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        log(message);
    }

    public static final class FailingFluidTile extends BaseMetaTileEntity {

        private boolean rejectFill;

        @Override
        public int fill(ForgeDirection side, FluidStack fluid, boolean doFill) {
            return doFill && rejectFill ? 0 : super.fill(side, fluid, doFill);
        }
    }
}
