package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftConfirmPacket;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftingAvailabilityPacket;
import com.xyp.gtneverreset.common.packet.quantum.RecursivePatternResultPacket;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingJob;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.machine.ECraftingCPUCluster;
import com.xyp.gtneverreset.utils.structure.StructureTooltipReader;

import appeng.api.util.WorldCoord;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

@Mod(
    modid = "compactaeserverqa",
    name = "Compact AE Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NEVER_RESET)
public final class CompactAEServerChecks {

    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtnr.compactAe.server.qa")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.worldServerForDimension(0) == null) return;
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verifyMachines();
            verifyCommonTypes();
            finish(server, "PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(server, "FAIL");
        }
    }

    private static void verifyMachines() {
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.QuantumComputer.id] instanceof QuantumComputer,
            "quantum computer registration");
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.AssemblerMatrix.id] instanceof AssemblerMatrix,
            "assembler matrix registration");
        require(
            GTNRItemList.QuantumComputer.get(1)
                .getItemDamage() == GTNRMachineID.QuantumComputer.id,
            "quantum computer item ID");
        require(
            GTNRItemList.AssemblerMatrix.get(1)
                .getItemDamage() == GTNRMachineID.AssemblerMatrix.id,
            "assembler matrix item ID");

        QuantumComputer computer = new QuantumComputer("compactAeServerQaComputer");
        AssemblerMatrix matrix = new AssemblerMatrix("compactAeServerQaMatrix");
        require(computer.getMaximumStorage() == Long.MAX_VALUE, "quantum storage capacity");
        require(
            matrix.getInventory()
                .getSizeInventory() == AssemblerMatrix.PATTERN_CAPACITY,
            "matrix pattern inventory");
        verifyStructure(computer);
        verifyStructure(matrix);
        require(
            computer.createTooltip()
                .getInformation().length > 1,
            "quantum information tooltip");
        require(
            computer.createTooltip()
                .getStructureInformation().length > 1,
            "quantum structure tooltip");
        require(
            matrix.createTooltip()
                .getInformation().length > 1,
            "matrix information tooltip");
        require(
            matrix.createTooltip()
                .getStructureInformation().length > 1,
            "matrix structure tooltip");
    }

    private static void verifyStructure(MTEEnhancedMultiBlockBase<?> machine) {
        require(
            machine.getStructureDefinition() != null,
            machine.getClass()
                .getSimpleName() + " definition");
        var structure = StructureTooltipReader.readStructure(machine);
        var size = structure.size();
        require(
            size.width() == 3 && size.height() == 2 && size.depth() == 1,
            machine.getClass()
                .getSimpleName() + " compact dimensions: "
                + size);
        require(
            !structure.blocks()
                .isEmpty(),
            machine.getClass()
                .getSimpleName() + " structure casings");
    }

    private static void verifyCommonTypes() {
        for (Class<?> type : List.of(
            QuantumComputer.class,
            AssemblerMatrix.class,
            ECraftingCPUCluster.class,
            QuantumCraftingJob.class,
            QuantumCraftingAvailabilityPacket.class,
            QuantumCraftConfirmPacket.class,
            RecursivePatternResultPacket.class)) {
            type.getDeclaredConstructors();
            type.getDeclaredMethods();
        }
        WorldCoord position = new WorldCoord(0, 64, 0);
        ECraftingCPUCluster cpu = new ECraftingCPUCluster(position, position);
        cpu.setAvailableStorage(1024);
        cpu.setAccelerators(16);
        require(cpu.getAvailableStorage() == 1024, "virtual CPU construction");

        new QuantumCraftingAvailabilityPacket.Handler().onMessage(new QuantumCraftingAvailabilityPacket(true), null);
        new QuantumCraftConfirmPacket.Handler().onMessage(new QuantumCraftConfirmPacket(true), null);
        new RecursivePatternResultPacket.Handler().onMessage(
            new RecursivePatternResultPacket(AEItemStack.create(new ItemStack(Items.stick)), List.of(), false),
            null);
    }

    private void finish(MinecraftServer server, String result) {
        finished = true;
        try {
            File directory = new File(System.getProperty("gtnr.compactAe.server.qa.output"));
            directory.mkdirs();
            Files.write(new File(directory, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            System.out.println("COMPACT_AE_SERVER_QA " + result);
        } catch (Exception failure) {
            failure.printStackTrace();
        } finally {
            server.initiateShutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
