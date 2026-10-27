package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.hatch.VaultPortHatch;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mod(modid = "vaultserverqa", name = "Vault Server QA", version = "1", dependencies = "after:" + GTNeverReset.MODID)
public final class VaultServerChecks {

    private boolean finished;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.vault.server.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verify(server.worldServerForDimension(0));
            System.out.println("VAULT_SERVER_QA PASS: " + checks + " checks");
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        } finally {
            try {
                File directory = new File(System.getProperty("gtnr.vault.server.qa.output"));
                directory.mkdirs();
                Files.writeString(new File(directory, "result.txt").toPath(), result);
            } catch (Exception failure) {
                failure.printStackTrace();
            }
            server.initiateShutdown();
        }
    }

    private void verify(World world) {
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.SingularityDataHub.id] instanceof SingularityDataHub,
            "vault controller registered on dedicated server");
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.VaultPortHatch.id] instanceof VaultPortHatch,
            "vault port registered on dedicated server");
        require(
            GTNRItemList.EddingtonWallCasing.hasBeenSet() && BlockLoader.eddingtonWallCasing != null,
            "Eddington casing registered on dedicated server");
        var casingTexture = BlockLoader.eddingtonWallCasing.getCasingTexture();
        require(
            casingTexture != null && !casingTexture.getClass()
                .getName()
                .contains("VaultConnectedTextureRender"),
            "common casing texture path avoids client connected renderer");
        BaseMetaTileEntity base = place(world, new int[] { 0, 70, 0 }, GTNRMachineID.SingularityDataHub.id);
        SingularityDataHub machine = (SingularityDataHub) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.stick), false);
        List<int[]> candidates = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int y = 70; y <= 72; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.getBlock(x, y, z) == BlockLoader.eddingtonWallCasing)
                        candidates.add(new int[] { x, y, z });
                }
            }
        }
        require(candidates.size() == 11, "original 3x2x2 construction has eleven casing candidates");
        int[] portPosition = candidates.get(0);
        VaultPortHatch port = (VaultPortHatch) place(world, portPosition, GTNRMachineID.VaultPortHatch.id)
            .getMetaTileEntity();
        require(
            machine.checkStructure(true, base) && machine.mCountCasing == 10 && port.controller == machine,
            "one port and ten Eddington walls form a bound vault on dedicated server");
        require(
            port.getCasingTexture()
                .getClass() == casingTexture.getClass(),
            "port casing also uses the common texture implementation");
        int[] secondPortPosition = candidates.get(1);
        place(world, secondPortPosition, GTNRMachineID.VaultPortHatch.id);
        require(!machine.checkStructure(true, base), "second vault port is rejected");
        world.setBlock(
            secondPortPosition[0],
            secondPortPosition[1],
            secondPortPosition[2],
            BlockLoader.eddingtonWallCasing,
            0,
            3);
        require(
            machine.checkStructure(true, base) && port.controller == machine,
            "single-port structure restores and rebinds");
        machine.locked = false;
        ItemStack iron = new ItemStack(Items.iron_ingot, 32);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 4096);
        require(
            machine.injectItems(iron, false) == 32 && machine.injectFluids(water, false) == 4096
                && machine.getUsedStorageBytes() == 0,
            "item and fluid simulation leaves server inventory unchanged");
        require(
            machine.injectItems(iron, true) == 32 && machine.injectFluids(water, true) == 4096,
            "server commits item and fluid insertion");
        require(
            machine.extractItems(AEItemStack.create(new ItemStack(Items.iron_ingot, 8)), true) == 8
                && machine.extractFluids(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 512)), true) == 512,
            "server commits item and fluid extraction");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        require(
            machine.getStoredItem(new ItemStack(Items.iron_ingot))
                .getStackSize() == 24
                && machine.getStoredFluid(new FluidStack(FluidRegistry.WATER, 1))
                    .getStackSize() == 3584
                && machine.getUsedStorageBytes() == 21,
            "server NBT roundtrip retains items, fluids and shared byte accounting");
        require(
            machine.getActualEnergyUsage() == 0 && port.isInfinite(),
            "native zero-EU vault and infinite ME power remain available");
    }

    private static BaseMetaTileEntity place(World world, int[] position, int id) {
        world.setBlockToAir(position[0], position[1], position[2]);
        world.setBlock(position[0], position[1], position[2], GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(position[0], position[1], position[2]);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName("vault-server-qa");
        tile.setOwnerUuid(new UUID(1, 2));
        return tile;
    }

    private void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
