package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.loader.MachineLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "modernfluidmachineqa",
    name = "Modern Fluid Machine QA",
    version = "1",
    dependencies = "after:" + GTNeverReset.MODID)
public final class ModernFluidMachineChecks {

    private World world;
    private int nextX;
    private int checks;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.modernFluidMachine.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verify(server.worldServerForDimension(0));
            System.out.println("MODERN_FLUID_MACHINE_QA PASS: " + checks + " assertions");
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        } finally {
            try {
                File directory = new File(System.getProperty("gtnr.modernFluidMachine.qa.output"));
                directory.mkdirs();
                Files.writeString(new File(directory, "result.txt").toPath(), result);
            } catch (Exception failure) {
                failure.printStackTrace();
            }
            server.initiateShutdown();
        }
    }

    private void verify(World testWorld) {
        world = testWorld;
        verifyRegistration();
        verifyNativeRecipes();
        verifyTankAccess();
        verifyProcessingAndPersistence();
        verifyOverclock();
    }

    private void verifyRegistration() {
        Set<Integer> ids = new HashSet<>();
        ItemStack[][] families = { MachineLoader.MODERN_MIXERS, MachineLoader.MODERN_ELECTROLYZERS,
            MachineLoader.MODERN_CHEMICAL_REACTORS };
        int[][] fluidSlots = { { 2, 1 }, { 1, 6 }, { 3, 2 } };
        int[][] capacities = { { 8000, 12000, 16000, 16000, 16000, 16000, 16000, 16000 },
            { 32000, 48000, 64000, 64000, 64000, 64000, 64000, 64000 },
            { 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000 } };
        for (int family = 0; family < families.length; family++) {
            for (int tier = 1; tier <= 8; tier++) {
                ItemStack stack = families[family][tier];
                require(
                    stack != null && ids.add(stack.getItemDamage()),
                    "unique registered machine " + family + "/" + tier);
                require(
                    GregTechAPI.METATILEENTITIES[stack.getItemDamage()] instanceof ModernFluidMachine,
                    "registered modern machine implementation " + family + "/" + tier);
                ModernFluidMachine prototype = (ModernFluidMachine) GregTechAPI.METATILEENTITIES[stack.getItemDamage()];
                require(prototype.mTier == tier, "native voltage tier " + family + "/" + tier);
                require(
                    prototype.getCapacity() == capacities[family][tier - 1],
                    "original GTM tank capacity " + family + "/" + tier);
                require(
                    prototype.getInputTanks().length == fluidSlots[family][0]
                        && prototype.getOutputTanks().length == fluidSlots[family][1],
                    "GTM fluid IO shape " + family + "/" + tier);
                require(
                    RecipeMaps.assemblerRecipes.getAllRecipes()
                        .stream()
                        .anyMatch(
                            recipe -> Arrays.stream(recipe.mOutputs)
                                .anyMatch(output -> GTUtility.areStacksEqual(stack, output))),
                    "survival assembler recipe " + family + "/" + tier);
            }
        }
    }

    private void verifyNativeRecipes() {
        processNative(
            MachineLoader.MODERN_ELECTROLYZERS[8],
            recipe -> Arrays.stream(recipe.mFluidInputs)
                .anyMatch(fluid -> fluid.isFluidEqual(Materials.Water.getFluid(1)))
                && Arrays.stream(recipe.mFluidOutputs)
                    .anyMatch(fluid -> fluid.isFluidEqual(Materials.Hydrogen.getGas(1)))
                && Arrays.stream(recipe.mFluidOutputs)
                    .anyMatch(fluid -> fluid.isFluidEqual(Materials.Oxygen.getGas(1))),
            "native water electrolysis without cells");
        processNative(
            MachineLoader.MODERN_MIXERS[8],
            recipe -> recipe.mFluidInputs.length >= 2,
            "native two-fluid mixing without cells");
        processNative(
            MachineLoader.MODERN_CHEMICAL_REACTORS[8],
            recipe -> recipe.mFluidInputs.length >= 2,
            "native multi-fluid small chemical reaction without cells");
    }

    private void processNative(ItemStack stack, Predicate<GTRecipe> selection, String label) {
        BaseMetaTileEntity tile = place(stack);
        ModernFluidMachine machine = machine(tile);
        GTRecipe recipe = machine.getRecipeMap()
            .getAllRecipes()
            .stream()
            .filter(selection)
            .filter(candidate -> fits(machine, candidate))
            .min(Comparator.comparingInt(candidate -> candidate.mEUt))
            .orElseThrow(() -> new AssertionError(label + ": no suitable real recipe"));
        supply(machine, recipe);
        require(machine.checkRecipe() == 2, label + ": recipe starts");
        require(machine.mEUt > 0 && machine.mMaxProgresstime > 0, label + ": consumes power and takes time");
        complete(tile, label);
        for (FluidStack output : recipe.mFluidOutputs)
            require(amount(machine.getOutputTanks(), output) >= output.amount, label + ": liquid output delivered");
        require(
            Arrays.stream(recipe.mInputs)
                .noneMatch(input -> GTUtility.getFluidForFilledItem(input, true) != null),
            label + ": fluid containers absent from item inputs");
    }

    private static boolean fits(ModernFluidMachine machine, GTRecipe recipe) {
        if (!recipe.mEnabled || recipe.mFakeRecipe
            || recipe.mSpecialValue != 0
            || recipe.mEUt <= 0
            || recipe.mEUt > GTValues.V[machine.mTier]
            || recipe.mInputs.length > machine.mInputSlotCount
            || recipe.mOutputs.length > machine.mOutputItems.length
            || recipe.mFluidInputs.length > machine.getInputTanks().length
            || recipe.mFluidOutputs.length > machine.getOutputTanks().length) return false;
        for (ItemStack input : recipe.mInputs) if (input.stackSize > input.getMaxStackSize()) return false;
        for (FluidStack input : recipe.mFluidInputs) if (input.amount > machine.getCapacity()) return false;
        for (FluidStack output : recipe.mFluidOutputs) if (output.amount > machine.getCapacity()) return false;
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) if (recipe.getFluidOutputChance(i) != 10000) return false;
        return true;
    }

    private void verifyTankAccess() {
        BaseMetaTileEntity tile = place(MachineLoader.MODERN_CHEMICAL_REACTORS[1]);
        ModernFluidMachine machine = machine(tile);
        FluidStack hydrogen = Materials.Hydrogen.getGas(200);
        FluidStack oxygen = Materials.Oxygen.getGas(300);
        require(machine.fill(hydrogen, false) == 200, "first fluid fill simulation accepts fluid");
        require(amount(machine.getInputTanks(), hydrogen) == 0, "fill simulation does not create a fluid stack");
        require(
            machine.fill(hydrogen, true) == 200 && machine.fill(oxygen, true) == 300,
            "two distinct input fluids coexist");
        require(
            amount(machine.getInputTanks(), hydrogen) == 200 && amount(machine.getInputTanks(), oxygen) == 300,
            "distinct input fluid amounts retained");
        require(machine.fill(hydrogen, false) == 200, "existing fluid accepts simulated refill");
        require(amount(machine.getInputTanks(), hydrogen) == 200, "existing fluid simulation leaves amount unchanged");
        setTank(machine.getOutputTanks()[0], Materials.Water.getFluid(500));
        setTank(machine.getOutputTanks()[1], Materials.Oxygen.getGas(600));
        FluidStack simulated = tile.drain(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(125), false);
        require(simulated != null && simulated.amount == 125, "Forge typed drain finds second output tank");
        require(amount(machine.getOutputTanks(), oxygen) == 600, "typed drain simulation preserves second tank");
        FluidStack fast = machine.drain(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(1), 125, false);
        require(fast != null && fast.amount == 125, "fast typed drain respects explicit amount for second tank");
        require(amount(machine.getOutputTanks(), oxygen) == 600, "fast typed drain simulation preserves second tank");
        FluidStack extracted = tile.drain(ForgeDirection.UNKNOWN, Materials.Oxygen.getGas(125), true);
        require(extracted != null && extracted.amount == 125, "Forge typed drain extracts second output tank");
        require(amount(machine.getOutputTanks(), oxygen) == 475, "typed drain modifies only selected fluid");
        require(
            amount(machine.getOutputTanks(), Materials.Water.getFluid(1)) == 500,
            "typed drain preserves first tank");
    }

    private void verifyProcessingAndPersistence() {
        BaseMetaTileEntity tile = place(MachineLoader.MODERN_CHEMICAL_REACTORS[1]);
        ModernFluidMachine machine = machine(tile);
        GTRecipe recipe = fixtureRecipe(
            new FluidStack[] { Materials.Hydrogen.getGas(100), Materials.Oxygen.getGas(50),
                Materials.Nitrogen.getGas(25) },
            new FluidStack[] { Materials.Water.getFluid(75), Materials.Oxygen.getGas(25) },
            16,
            40);
        machine.getRecipeMap()
            .add(recipe);
        supply(machine, recipe);
        int outputAmount = recipe.mOutputs[0].stackSize;
        recipe.mOutputs[0].stackSize = 65;
        require(machine.checkRecipe() != 2, "oversized item output is rejected before native stack truncation");
        require(
            machine.getStackInSlot(machine.getInputSlot()).stackSize == 1,
            "oversized item output preserves item ingredient");
        recipe.mOutputs[0].stackSize = outputAmount;
        for (FluidStackTank tank : machine.getOutputTanks())
            setTank(tank, Materials.Lava.getFluid(machine.getCapacity()));
        require(machine.checkRecipe() != 2, "blocked fluid outputs reject recipe");
        require(
            machine.getStackInSlot(machine.getInputSlot()).stackSize == 1,
            "blocked output preserves item ingredient");
        for (FluidStack input : recipe.mFluidInputs)
            require(amount(machine.getInputTanks(), input) == input.amount, "blocked output preserves input fluid");
        for (FluidStackTank tank : machine.getOutputTanks()) setTank(tank, null);
        require(machine.checkRecipe() == 2, "unblocking starts multi-fluid recipe");
        for (FluidStack input : recipe.mFluidInputs) require(
            amount(machine.getInputTanks(), input) == 0,
            "successful recipe consumes correct input fluid amount");
        require(machine.getStackInSlot(machine.getInputSlot() + 1).stackSize == 1, "non-consumed catalyst retained");

        NBTTagCompound snapshot = new NBTTagCompound();
        machine.saveNBTData(snapshot);
        BaseMetaTileEntity restoredTile = place(MachineLoader.MODERN_CHEMICAL_REACTORS[1]);
        ModernFluidMachine restored = machine(restoredTile);
        restored.loadNBTData(snapshot);
        require(
            restored.mMaxProgresstime == machine.mMaxProgresstime && restored.mEUt == machine.mEUt,
            "NBT retains active process time and energy use");
        complete(restoredTile, "restored active chemical recipe");
        for (FluidStack output : recipe.mFluidOutputs) require(
            amount(restored.getOutputTanks(), output) == output.amount,
            "NBT pending fluid output delivered once");
        require(
            GTUtility
                .areStacksEqual(restored.getStackInSlot(restored.getOutputSlot()), new ItemStack(Items.gold_nugget)),
            "NBT pending item output delivered");
        restored.onPostTick(restoredTile, 101);
        for (FluidStack output : recipe.mFluidOutputs) require(
            amount(restored.getOutputTanks(), output) == output.amount,
            "completed process does not duplicate fluids");
        verifyAutomaticOutput(restoredTile);
    }

    private void verifyAutomaticOutput(BaseMetaTileEntity tile) {
        ModernFluidMachine machine = machine(tile);
        BaseMetaTileEntity tank = placeAt(ItemList.Hatch_Input_UV.get(1), tile.xCoord, tile.yCoord, tile.zCoord + 1);
        tank.setFrontFacing(ForgeDirection.NORTH);
        require(
            tank.fill(ForgeDirection.NORTH, Materials.Oxygen.getGas(1), true) == 1,
            "neighbor tank prefilled with second product");
        int oxygenBefore = amount(machine.getOutputTanks(), Materials.Oxygen.getGas(1));
        int waterBefore = amount(machine.getOutputTanks(), Materials.Water.getFluid(1));
        machine.mFluidTransfer = true;
        tile.setFrontFacing(ForgeDirection.SOUTH);
        machine.onPostTick(tile, 120);
        require(
            amount(machine.getOutputTanks(), Materials.Oxygen.getGas(1)) < oxygenBefore,
            "automatic output reaches second product when first is rejected");
        require(
            amount(machine.getOutputTanks(), Materials.Water.getFluid(1)) == waterBefore,
            "rejected first product stays buffered");
        require(
            tank.getMetaTileEntity()
                .getFluid().amount > 1,
            "neighbor receives selected second product");
    }

    private void verifyOverclock() {
        GTRecipe recipe = fixtureRecipe(
            new FluidStack[] { Materials.Water.getFluid(10), Materials.Hydrogen.getGas(10) },
            new FluidStack[] { Materials.Nitrogen.getGas(10) },
            16,
            80);
        BaseMetaTileEntity lowTile = place(MachineLoader.MODERN_MIXERS[1]);
        ModernFluidMachine low = machine(lowTile);
        low.getRecipeMap()
            .add(recipe);
        supply(low, recipe);
        require(low.checkRecipe() == 2, "LV mixer starts test recipe");
        BaseMetaTileEntity highTile = place(MachineLoader.MODERN_MIXERS[2]);
        ModernFluidMachine high = machine(highTile);
        supply(high, recipe);
        require(high.checkRecipe() == 2, "MV mixer starts same recipe");
        require(high.mEUt == low.mEUt * 4, "ordinary overclock consumes four times EU per tick");
        require(high.mMaxProgresstime * 2 == low.mMaxProgresstime, "ordinary overclock halves duration");
    }

    private static GTRecipe fixtureRecipe(FluidStack[] inputs, FluidStack[] outputs, int eut, int duration) {
        ItemStack catalyst = new ItemStack(Items.stick, 0);
        GTRecipe recipe = new GTRecipe(
            false,
            new ItemStack[] { new ItemStack(Blocks.command_block), catalyst },
            new ItemStack[] { new ItemStack(Items.gold_nugget) },
            null,
            null,
            null,
            null,
            null,
            inputs,
            outputs,
            duration,
            eut,
            0);
        recipe.mHidden = true;
        return recipe;
    }

    private static void supply(ModernFluidMachine machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.mInputs.length; i++) {
            ItemStack ingredient = recipe.mInputs[i].copy();
            if (ingredient.stackSize == 0) ingredient.stackSize = 1;
            machine.setInventorySlotContents(machine.getInputSlot() + i, ingredient);
        }
        for (FluidStack input : recipe.mFluidInputs) {
            int accepted = machine.fill(input.copy(), true);
            if (accepted != input.amount)
                throw new AssertionError("recipe input does not fit tank: " + input.getLocalizedName());
        }
    }

    private void complete(BaseMetaTileEntity tile, String label) {
        ModernFluidMachine machine = machine(tile);
        int eut = machine.mEUt;
        tile.setStoredEU(tile.getEUCapacity());
        tile.disableWorking();
        machine.mProgresstime = machine.mMaxProgresstime - 1;
        long before = tile.getStoredEU();
        machine.onPostTick(tile, 100);
        require(machine.mMaxProgresstime == 0, label + ": actual processing tick completes");
        require(tile.getStoredEU() == before - eut, label + ": processing tick drains exact EU");
    }

    private BaseMetaTileEntity place(ItemStack stack) {
        int x = nextX;
        nextX += 3;
        return placeAt(stack, x, 80, 0);
    }

    private BaseMetaTileEntity placeAt(ItemStack stack, int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.enableWorking();
        for (int i = 0; i < 6; i++) tile.updateEntity();
        if (tile.getMetaTileEntity() instanceof ModernFluidMachine machine) {
            machine.mMainFacing = ForgeDirection.NORTH;
            tile.setFrontFacing(ForgeDirection.SOUTH);
        }
        return tile;
    }

    private static ModernFluidMachine machine(BaseMetaTileEntity tile) {
        return (ModernFluidMachine) tile.getMetaTileEntity();
    }

    private static int amount(FluidStackTank[] tanks, FluidStack fluid) {
        return Arrays.stream(tanks)
            .map(FluidStackTank::getFluid)
            .filter(stack -> stack != null && stack.isFluidEqual(fluid))
            .mapToInt(stack -> stack.amount)
            .sum();
    }

    private static void setTank(FluidStackTank tank, FluidStack fluid) {
        tank.drain(Integer.MAX_VALUE, true);
        if (fluid != null) tank.fill(fluid, true);
    }

    private void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
