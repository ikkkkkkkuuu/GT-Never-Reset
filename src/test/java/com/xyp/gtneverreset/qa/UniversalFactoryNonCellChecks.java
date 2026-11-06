package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;

@Mod(
    modid = "universalfactorynoncellqa",
    name = "Universal Factory NonCell Regression QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class UniversalFactoryNonCellChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, opened, finished;
    private volatile boolean ready;
    private volatile Throwable failure;
    private int ticks, frames, serverTicks;
    private BaseMetaTileEntity tile;
    private UniversalJointFactory factory;

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
        try {
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            if (!checked) {
                checked = true;
                World world = player.worldObj;
                tile = place(world, 0, GTNRMachineID.UniversalJointFactory.id);
                tile.setOwnerName(player.getCommandSenderName());
                tile.setOwnerUuid(player.getUniqueID());
                tile.disableWorking();
                factory = (UniversalJointFactory) tile.getMetaTileEntity();
                factory.fixAllIssues();
                var hatch = (CrossRecipeMEPatternBuffer) place(world, 1, GTNRMachineID.CrossRecipeMEPatternBuffer.id)
                    .getMetaTileEntity();
                verifySaltElectrolysis(factory, hatch);
                factory.setMachineMode(15);
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(0.5, 80, -3, 0, 0);
                ready = true;
            }
            if (ready && !opened && ++serverTicks >= 60) {
                require(factory.onRightclick(tile, player), "factory GUI opened after regression checks");
                opened = true;
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
            mc.gameSettings.pauseOnLostFocus = false;
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "factory-noncell-" + System.currentTimeMillis(),
                    "Factory NonCell QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server regression checks", failure);
            if (launched && ++ticks > 1800) throw new AssertionError("QA timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper) || ++frames < 40) return;
        try {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "factory-noncell.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verifySaltElectrolysis(UniversalJointFactory factory, CrossRecipeMEPatternBuffer hatch) {
        ItemStack salt = Materials.Salt.getDust(2);
        ItemStack sodium = Materials.Sodium.getDust(1);
        FluidStack chlorine = Materials.Chlorine.getGas(1000);
        for (boolean usePattern : new boolean[] { false, true }) {
            factory.setMachineMode(usePattern ? 18 : 15);
            UniversalJointFactoryProcessingLogic logic = new UniversalJointFactoryProcessingLogic(factory);
            logic.setMachine(factory)
                .setRecipeMapSupplier(factory::getRecipeMap)
                .setAvailableVoltage(32)
                .setAvailableAmperage(512)
                .setMaxParallel(1)
                .setVoidProtection(false, false);
            ItemStack material = salt.copy();
            logic.prepareRecipe(16384);
            if (usePattern) {
                hatch.onPatternChange(0, pattern(salt, sodium, chlorine));
                require(
                    logic.tryCachePossibleRecipesFromPattern(
                        hatch.inventories()
                            .next()),
                    "cross-recipe hatch accepts the industrial electrolyzer recipe map");
            }
            logic.setInputItems(material)
                .setInputFluids();
            String mode = usePattern ? "cross-map pattern with extractor selected" : "selected electrolyzer mode";
            require(
                logic.process()
                    .wasSuccessful(),
                mode + " electrolyzes salt without cells");
            require(
                logic.getOutputFluids().length == 1 && logic.getOutputFluids()[0].isFluidEqual(chlorine)
                    && logic.getOutputFluids()[0].amount == chlorine.amount,
                mode + " produces 1000 L chlorine as fluid");
            require(
                logic.getOutputItems().length == 1 && logic.getOutputItems()[0].isItemEqual(sodium)
                    && logic.getOutputItems()[0].stackSize == sodium.stackSize,
                mode + " produces sodium without filled cells");
            require(material.stackSize == 2, mode + " keeps inputs until commit");
            require(logic.commitInputs() && material.stackSize == 0, mode + " consumes two salt dust");
            require(factory.getMachineMode() == (usePattern ? 18 : 15), mode + " preserves selected mode");
        }
    }

    private static ItemStack pattern(ItemStack input, ItemStack output, FluidStack fluidOutput) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        in.appendTag(
            AEItemStack.create(input)
                .toNBTGeneric());
        out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        out.appendTag(
            AEFluidStack.create(fluidOutput)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        encoded.setTagCompound(tag);
        PatternRecipeMap.write(encoded, RecipeMaps.electrolyzerNonCellRecipes.unlocalizedName);
        return encoded;
    }

    private static BaseMetaTileEntity place(World world, int x, int id) {
        world.setBlock(x, 80, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 80, 0);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[factory-noncell-qa] PASS " + message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "factory-noncell-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

}
