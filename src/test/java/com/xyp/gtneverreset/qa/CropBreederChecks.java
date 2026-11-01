package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.PageButton;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.mutation.CropMutation;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.MutationRegistry;
import com.gtnewhorizon.cropsnh.farming.requirements.breeding.WorldOnlyBreedingRequirement;
import com.gtnewhorizon.cropsnh.init.CropsNHFluids;
import com.xyp.gtneverreset.common.machine.multiblock.LargeCropBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingChance;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingPlanner;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingSubstrate;
import com.xyp.gtneverreset.config.CropBreederConfig;
import com.xyp.gtneverreset.config.CropsNHConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;

@Mod(modid = "cropbreederqa", name = "Crop breeder QA", version = "1", dependencies = "after:gtneverreset")
public final class CropBreederChecks {

    private static final boolean MIXED_ROUTE_PREVIEW = Boolean.getBoolean("gtnr.cropBreeder.mixedPreview");
    private static final boolean EXPANDED_ROUTE_PREVIEW = Boolean.getBoolean("gtnr.cropBreeder.expandedPreview");
    private static final boolean PROGRESS_PREVIEW = Boolean.getBoolean("gtnr.cropBreeder.progressPreview");
    private static final boolean LONG_ROUTE_PREVIEW = Boolean.getBoolean("gtnr.cropBreeder.longPreview");
    private static final boolean ROUTE_PREVIEW = MIXED_ROUTE_PREVIEW || EXPANDED_ROUTE_PREVIEW
        || PROGRESS_PREVIEW
        || LONG_ROUTE_PREVIEW
        || Boolean.getBoolean("gtnr.cropBreeder.preview");
    private int previewParentCount = 3;
    private boolean syntheticLongRoute;
    private final Set<String> displayedPlantingSoils = new HashSet<>();

    private boolean started, checksDone;
    private volatile boolean finished, ready;
    private volatile boolean removeSubstrate;
    private int soilTooltipPhase;
    private int ticks, frames, page, openAt, graphPhase;
    private LargeCropBreeder machine;
    private BaseMetaTileEntity controller, energy;
    private MTEHatchOutputBus output;
    private MTEHatchInputBus input;
    private MTEHatchInput fluid;
    private ICropMutation mutation;

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
                "crop-breeder-" + System.currentTimeMillis(),
                "Crop breeder QA",
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
            if (++ticks == 30) setup(player);
            if (machine == null) return;
            energy.setStoredEU(energy.getEUCapacity());
            if (removeSubstrate) {
                for (int i = 0; i < input.getSizeInventory(); i++) input.setInventorySlotContents(i, null);
                removeSubstrate = false;
            }
            if (ticks == 50) controller.enableWorking();
            if (ticks == 160) {
                require(
                    !machine.getArchivedCrops()
                        .contains(
                            mutation.getOutput()
                                .getId()),
                    "output filled during processing pauses completion without unlocking result");
                for (int i = 0; i < output.getSizeInventory(); i++) output.setInventorySlotContents(i, null);
            }
            if (!checksDone && ticks > 50
                && machine.getArchivedCrops()
                    .contains(
                        mutation.getOutput()
                            .getId())) {
                controller.disableWorking();
                boolean found = false;
                for (int i = 0; i < output.getSizeInventory(); i++) {
                    ItemStack stack = output.getStackInSlot(i);
                    if (CropRegistry.instance.get(stack, false) != mutation.getOutput()) continue;
                    SeedStats stats = SeedStats.getStatsFromStack(stack);
                    require(
                        stats.getGrowth() == 12 && stats.getGain() == 15 && stats.getResistance() == 18,
                        "native processing outputs inherited 12/15/18 stats");
                    found = true;
                }
                require(found, "successful mutation physically outputs a seed and unlocks archive");
                int remainingPlantingSoil = 0;
                CropBreedingSubstrate plantingSoil = CropBreedingSubstrate.forMutation(mutation)
                    .get(0);
                for (int i = 0; i < input.getSizeInventory(); i++) {
                    ItemStack stack = input.getStackInSlot(i);
                    if (plantingSoil.matches(stack)) remainingPlantingSoil += stack.stackSize;
                }
                require(remainingPlantingSoil == 1, "planting soil remains in the input bus after successful breeding");
                NBTTagCompound saved = new NBTTagCompound();
                machine.saveNBTData(saved);
                BaseMetaTileEntity restoredTile = place(
                    player.worldObj,
                    12,
                    10,
                    0,
                    GTNRItemList.LargeCropBreeder.get(1));
                restoredTile.disableWorking();
                LargeCropBreeder restored = (LargeCropBreeder) restoredTile.getMetaTileEntity();
                restored.loadNBTData(saved);
                require(
                    restored.getArchivedCrops()
                        .equals(machine.getArchivedCrops()),
                    "archive survives NBT reload");
                require(
                    restored.getTargetCrop()
                        .equals(machine.getTargetCrop()),
                    "target survives NBT reload");
                NBTTagCompound item = new NBTTagCompound();
                machine.setItemNBT(item);
                restored.loadNBTData(item);
                require(
                    restored.getArchivedCrops()
                        .equals(machine.getArchivedCrops()),
                    "wrench item retains archive");
                String before = machine.getTargetCrop();
                machine.setTargetCrop("invalid:crop");
                require(
                    !machine.isTargetValid() && machine.getTargetCrop()
                        .equals(before),
                    "invalid target is rejected");
                machine.setTargetCrop(before);
                catalystChecks();
                machine.stopMachine(gregtech.api.util.shutdown.ShutDownReasonRegistry.NONE);
                ICropMutation fourParents = MutationRegistry.instance.getDeterministicMutations()
                    .stream()
                    .filter(
                        m -> m.getParentCount() == 4 && m.getRequirements()
                            .stream()
                            .noneMatch(WorldOnlyBreedingRequirement.class::isInstance)
                            && m.getRequirements()
                                .stream()
                                .anyMatch(
                                    com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement.class::isInstance))
                    .findFirst()
                    .orElseThrow();
                for (int i = 0; i < input.getSizeInventory(); i++) input.setInventorySlotContents(i, null);
                int sampleSlot = 0;
                for (ICropCard parent : fourParents.getParents()) input.setInventorySlotContents(
                    sampleSlot++,
                    parent.getSeedItem(new SeedStats((byte) 12, (byte) 15, (byte) 18, true)));
                machine.setTargetCrop(
                    fourParents.getOutput()
                        .getId());
                if (ROUTE_PREVIEW) prepareRoutePreview();
                else {
                    var soil = (com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement) fourParents
                        .getRequirements()
                        .stream()
                        .filter(com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement.class::isInstance)
                        .findFirst()
                        .orElseThrow();
                    ItemStack substrate = soil.getMachineOnlyCatalystsForNEI()
                        .stream()
                        .filter(soil::isValidSubSoil)
                        .findFirst()
                        .orElseThrow()
                        .copy();
                    substrate.stackSize = 1;
                    input.setInventorySlotContents(8, substrate);
                }
                player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);
                checksDone = true;
                openAt = ticks + 40;
            }
            if (checksDone && ticks == openAt) {
                player.worldObj.markBlockForUpdate(0, 10, 0);
                machine.onRightclick(controller, player);
                ready = true;
            }
            if (ticks > 1600) throw new AssertionError("timeout: formed=" + machine.mMachine + " ready=" + ready);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        CropsNHConfig.enableMaxStats = false;
        chanceChecks();
        plannerChecks();
        World world = player.worldObj;
        controller = place(world, 0, 10, 0, GTNRItemList.LargeCropBreeder.get(1));
        machine = (LargeCropBreeder) controller.getMetaTileEntity();
        controller.disableWorking();
        controller.setOwnerName(player.getCommandSenderName());
        controller.setOwnerUuid(player.getUniqueID());
        machine.construct(new ItemStack(Items.stick), false);
        input = (MTEHatchInputBus) place(world, -2, 10, 0, ItemList.Hatch_Input_Bus_HV.get(1)).getMetaTileEntity();
        output = (MTEHatchOutputBus) place(world, -1, 10, 0, ItemList.Hatch_Output_Bus_HV.get(1)).getMetaTileEntity();
        fluid = (MTEHatchInput) place(world, 1, 10, 0, ItemList.Hatch_Input_IV.get(1)).getMetaTileEntity();
        energy = place(world, 2, 10, 0, ItemList.Hatch_Energy_UHV.get(1));
        place(world, -2, 10, 1, ItemList.Hatch_Maintenance.get(1));
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
        require(machine.checkStructure(true, controller), "5 x 4 x 5 MBS greenhouse forms with native hatches");
        mutation = MutationRegistry.instance.getDeterministicMutations()
            .stream()
            .filter(
                m -> m.getParentCount() == 2 && m.getRequirements()
                    .isEmpty() && m.getBreedingMachineRecipeEUt() <= 128)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no simple native crop mutation"));
        int slot = 0;
        for (ICropCard parent : mutation.getParents()) {
            ItemStack seed = parent.getSeedItem(new SeedStats((byte) 12, (byte) 15, (byte) 18, true));
            seed.stackSize = 2;
            input.setInventorySlotContents(slot++, seed);
        }
        machine.setTargetCrop(
            mutation.getOutput()
                .getId());
        CropBreedingSubstrate plantingSoil = CropBreedingSubstrate.forMutation(mutation)
            .get(0);
        ItemStack plantingItem = plantingSoil.alternatives()
            .stream()
            .filter(plantingSoil::matches)
            .findFirst()
            .orElseThrow()
            .copy();
        plantingItem.stackSize = 1;
        input.setInventorySlotContents(8, plantingItem);
        require(
            !machine.checkProcessing()
                .wasSuccessful(),
            "missing fertilizer blocks processing");
        require(
            machine.getArchivedCrops()
                .size() == 2,
            "two analyzed parent samples archived");
        for (int i = 0; i < 2; i++)
            require(input.getStackInSlot(i).stackSize == 1, "exactly one seed consumed per new archive");
        String cropId = mutation.getOutput()
            .getId();
        CropBreederConfig.cropOverrides = new String[] { cropId + "=37" };
        require(machine.getSuccessChance() == 37, "UHV preserves per-crop chance");
        energy = place(world, 2, 10, 0, ItemList.Hatch_Energy_HV.get(1));
        require(machine.checkStructure(true, controller), "HV energy hatch forms");
        require(machine.getSuccessChance() == 37, "changing voltage does not change chance");
        energy = place(world, 2, 10, 0, ItemList.Hatch_Energy_UHV.get(1));
        require(machine.checkStructure(true, controller), "UHV energy hatch restored");
        fluid.fill(new FluidStack(CropsNHFluids.enrichedFertilizer, 64000), true);
        for (int i = 0; i < output.getSizeInventory(); i++)
            output.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
        int before = fluid.getFluidAmount();
        require(
            !machine.checkProcessing()
                .wasSuccessful(),
            "full output blocks processing");
        require(fluid.getFluidAmount() == before, "blocked output does not consume fertilizer");
        for (int i = 0; i < output.getSizeInventory(); i++) output.setInventorySlotContents(i, null);
        CropBreederConfig.cropOverrides = new String[] { cropId + "=0" };
        require(
            machine.checkProcessing()
                .wasSuccessful(),
            "zero-percent attempt starts");
        NBTTagCompound failed = new NBTTagCompound();
        machine.saveNBTData(failed);
        require(
            !failed.hasKey("cropActiveSeed") && machine.mOutputItems.length == 0,
            "zero-percent attempt produces no seed");
        require(fluid.getFluidAmount() < before, "failed attempt consumes fertilizer");
        CropBreederConfig.cropOverrides = new String[] { cropId + "=100" };
        machine.loadNBTData(failed);
        require(machine.getSuccessChance() == 0, "running chance and failed roll survive reload without rerolling");
        require(
            machine.checkProcessing()
                .wasSuccessful(),
            "native mutation starts");
        require(fluid.getFluidAmount() < before, "attempt consumes enriched fertilizer");
        require(
            !machine.getArchivedCrops()
                .contains(
                    mutation.getOutput()
                        .getId()),
            "result not unlocked before work completes");
        NBTTagCompound running = new NBTTagCompound();
        machine.saveNBTData(running);
        machine.loadNBTData(running);
        for (int i = 0; i < output.getSizeInventory(); i++)
            output.setInventorySlotContents(i, new ItemStack(Items.stick, 64));
        require(
            machine.getActiveCrop()
                .equals(
                    mutation.getOutput()
                        .getId()),
            "running mutation survives NBT reload");
    }

    private void catalystChecks() {
        CropBreederConfig.cropOverrides = new String[0];
        ICropMutation recipe = MutationRegistry.instance.getDeterministicMutations()
            .stream()
            .filter(
                m -> m.getBreedingMachineRecipeEUt() > 0 && m.getBreedingMachineRecipeEUt() <= 128
                    && m.getRequirements()
                        .size() == 1
                    && m.getRequirements()
                        .get(0) instanceof com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement
                    && !m.getBreedingMachineCatalystsForNEI(true)
                        .isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("no substrate mutation"));
        for (int i = 0; i < input.getSizeInventory(); i++) input.setInventorySlotContents(i, null);
        int slot = 0;
        for (ICropCard parent : recipe.getParents())
            input.setInventorySlotContents(slot++, parent.getSeedItem(SeedStats.DEFAULT_ANALYZED));
        machine.setTargetCrop(
            recipe.getOutput()
                .getId());
        int fertilizerBefore = fluid.getFluidAmount();
        require(
            !machine.checkProcessing()
                .wasSuccessful(),
            "missing substrate blocks mutation");
        require(fluid.getFluidAmount() == fertilizerBefore, "missing catalyst does not consume fertilizer");
        ItemStack catalyst = recipe.getBreedingMachineCatalystsForNEI(true)
            .get(0)
            .get(0)
            .copy();
        catalyst.stackSize = 1;
        input.setInventorySlotContents(8, catalyst);
        CropBreederConfig.cropOverrides = new String[] { recipe.getOutput()
            .getId() + "=0" };
        require(
            machine.checkProcessing()
                .wasSuccessful(),
            "native substrate requirement accepted");
        int remaining = 0;
        for (int i = 0; i < input.getSizeInventory(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(catalyst) && ItemStack.areItemStackTagsEqual(stack, catalyst))
                remaining += stack.stackSize;
        }
        require(remaining == 1, "one substrate remains in input bus after breeding attempt");
        NBTTagCompound failed = new NBTTagCompound();
        machine.saveNBTData(failed);
        require(!failed.hasKey("cropActiveSeed"), "failed substrate mutation does not produce seed");
        require(fluid.getFluidAmount() < fertilizerBefore, "substrate attempt also consumes fertilizer");
        CropBreederConfig.cropOverrides = new String[] { recipe.getOutput()
            .getId() + "=100" };
        require(
            machine.checkProcessing()
                .wasSuccessful(),
            "same substrate can be reused on the next attempt");
        remaining = 0;
        for (int i = 0; i < input.getSizeInventory(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(catalyst) && ItemStack.areItemStackTagsEqual(stack, catalyst))
                remaining += stack.stackSize;
        }
        require(remaining == 1, "repeated attempt never consumes substrate");
        NBTTagCompound succeeded = new NBTTagCompound();
        machine.saveNBTData(succeeded);
        require(succeeded.hasKey("cropActiveSeed"), "successful substrate mutation still preserves substrate");
        CropBreederConfig.cropOverrides = new String[0];
        mixedCatalystChecks(recipe, catalyst);
        fallbackSubstrateChecks();
    }

    private void fallbackSubstrateChecks() {
        ICropMutation evilOre = MutationRegistry.instance.getDeterministicMutations()
            .stream()
            .filter(
                recipe -> recipe.getOutput()
                    .getId()
                    .equals("cropsnh:evilOre"))
            .findFirst()
            .orElseThrow();
        CropBreedingSubstrate soil = CropBreedingSubstrate.forMutation(evilOre)
            .get(0);
        ItemStack netherrack = new ItemStack(Blocks.netherrack);
        require(
            soil.matches(netherrack) && !soil.matches(new ItemStack(Blocks.stone)),
            "EvilOre accepts its planting soil and rejects unrelated blocks");
        require(
            soil.alternatives()
                .stream()
                .anyMatch(stack -> stack.getItem() == netherrack.getItem()),
            "EvilOre displays netherrack among its substrate alternatives");
        try {
            var check = LargeCropBreeder.class
                .getDeclaredMethod("breedingConsumption", ICropMutation.class, ArrayList.class, ItemStack[].class);
            check.setAccessible(true);
            ArrayList<ICropCard> parents = new ArrayList<>(evilOre.getParents());
            require(
                check.invoke(machine, evilOre, parents, new ItemStack[0]) == null,
                "missing planting soil prevents breeding");
            require(
                check.invoke(machine, evilOre, parents, new ItemStack[] { new ItemStack(Blocks.stone) }) == null,
                "wrong planting soil prevents breeding");
            for (int attempt = 0; attempt < 2; attempt++) {
                int[] consumption = (int[]) check.invoke(machine, evilOre, parents, new ItemStack[] { netherrack });
                require(
                    consumption != null && consumption[0] == 0 && netherrack.stackSize == 1,
                    "planting soil is detected and reusable without consumption");
            }
            mixedCatalystChecks(evilOre, netherrack);
            CropBreedingSubstrate spruce = CropBreedingSubstrate
                .forCrop(CropRegistry.instance.get("cropsnh:bonsaiSpruce"))
                .get(0);
            require(
                spruce.alternatives()
                    .size() > 1 && spruce.matches(new ItemStack(Blocks.dirt))
                    && spruce.matches(new ItemStack(Blocks.grass)),
                "all registered planting-soil alternatives can satisfy a fallback requirement");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("planting-soil fallback check failed", failure);
        }
    }

    private void mixedCatalystChecks(ICropMutation recipe, ItemStack substrate) {
        ArrayList<ICropCard> parents = new ArrayList<>(recipe.getParents());
        CropMutation mixed = new CropMutation(recipe.getOutput(), parents.get(0), parents.get(1));
        mixed.requirements.clear();
        mixed.requirements.addAll(recipe.getRequirements());
        ItemStack required = substrate.copy();
        required.stackSize = 2;
        mixed.addRequirement(
            new com.gtnewhorizon.cropsnh.farming.requirements.breeding.MachineBreedingCatalystRequirement()
                .addItem(required));
        try {
            var check = LargeCropBreeder.class
                .getDeclaredMethod("breedingConsumption", ICropMutation.class, ArrayList.class, ItemStack[].class);
            check.setAccessible(true);
            ItemStack inputStack = substrate.copy();
            inputStack.stackSize = 2;
            require(
                check.invoke(machine, mixed, parents, new ItemStack[] { inputStack }) == null,
                "consumable catalyst cannot consume the reserved substrate");
            inputStack.stackSize = 3;
            int[] consumption = (int[]) check.invoke(machine, mixed, parents, new ItemStack[] { inputStack });
            require(
                consumption != null && consumption[0] == 2,
                "mixed requirement consumes two catalysts while retaining one substrate");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("mixed catalyst check failed", failure);
        }
    }

    private static void chanceChecks() {
        int[] defaults = { 100, 80, 60, 40, 20, 10 };
        CropBreederConfig.tierChances = defaults.clone();
        CropBreederConfig.cropOverrides = new String[0];
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            int band = Math.min(5, Math.max(0, crop.getTier() - 1) / 3);
            require(CropBreedingChance.percent(crop) == defaults[band], "native crop tier maps to default chance band");
        }
        ICropCard crop = CropRegistry.instance.getAllInRegistrationOrder()
            .get(0);
        String id = crop.getId();
        CropBreederConfig.cropOverrides = new String[] { id + "=27", id + "=broken", id + "=101", id + "=-1" };
        require(CropBreedingChance.percent(crop) == 27, "valid per-crop override survives malformed entries");
        CropBreederConfig.cropOverrides = new String[] { id + "=27", id + "=0" };
        require(CropBreedingChance.percent(crop) == 0, "last valid override wins, including zero");
        CropBreederConfig.cropOverrides = new String[0];
        CropBreederConfig.tierChances = new int[0];
        require(
            CropBreedingChance.percent(crop) == defaults[Math.min(5, Math.max(0, crop.getTier() - 1) / 3)],
            "empty tier configuration falls back to defaults");
        CropBreederConfig.tierChances = defaults;
    }

    private static void plannerChecks() {
        List<ICropCard> crops = CropRegistry.instance.getAllInRegistrationOrder();
        ICropCard a = crops.get(0), b = crops.get(1), c = crops.get(2), d = crops.get(3), e = crops.get(4),
            f = crops.get(5), g = crops.get(6);
        CropMutation first = new CropMutation(c, a, b);
        CropMutation second = new CropMutation(g, c, d, e, f);
        first.requirements.clear();
        second.requirements.clear();
        Set<String> seeds = Set.of(a.getId(), b.getId(), d.getId(), e.getId(), f.getId());
        var route = CropBreedingPlanner.plan(g.getId(), seeds, List.of(second, first));
        require(route.steps.equals(List.of(first, second)), "two-step route preserves all four final parents");
        require(route.reachable.contains(g.getId()), "multi-step reachable list includes final crop");
        require(
            CropBreedingPlanner.plan(g.getId(), Set.of(a.getId(), b.getId()), List.of(first, second)).steps.isEmpty(),
            "missing third or fourth parent cannot be bypassed");
        Set<String> complete = new HashSet<>(seeds);
        complete.add(c.getId());
        complete.add(g.getId());
        require(
            CropBreedingPlanner.plan(g.getId(), complete, List.of(first, second)).steps.equals(List.of(second)),
            "archived target still requires final mutation");
        CropMutation cycle = new CropMutation(a, g, b);
        cycle.requirements.clear();
        require(
            CropBreedingPlanner.plan(g.getId(), Set.of(b.getId()), List.of(first, second, cycle)).steps.isEmpty(),
            "cyclic graph terminates without inventing parents");
        second.requirements.add(new WorldOnlyBreedingRequirement());
        require(
            CropBreedingPlanner.plan(g.getId(), seeds, List.of(first, second)).steps.isEmpty(),
            "world-only recipe excluded");
        int fourParentCount = 0;
        for (ICropMutation recipe : MutationRegistry.instance.getDeterministicMutations()) {
            if (recipe.getParentCount() != 4 || recipe.getRequirements()
                .stream()
                .anyMatch(WorldOnlyBreedingRequirement.class::isInstance)) continue;
            Set<String> parents = recipe.getParents()
                .stream()
                .map(ICropCard::getId)
                .collect(Collectors.toSet());
            require(
                !CropBreedingPlanner.plan(
                    recipe.getOutput()
                        .getId(),
                    parents).steps.isEmpty(),
                "native four-parent recipe reachable");
            fourParentCount++;
        }
        require(fourParentCount > 0, "tested native four-parent mutations");
    }

    private void prepareRoutePreview() throws Exception {
        if (MIXED_ROUTE_PREVIEW || EXPANDED_ROUTE_PREVIEW || PROGRESS_PREVIEW) return;
        CropBreedingPlanner.Plan selected = null;
        ICropCard target = null;
        int startingTier = 1;
        for (; startingTier <= 3 && selected == null; startingTier++) {
            Set<String> initial = new HashSet<>();
            for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
                if (crop.getTier() <= startingTier) initial.add(crop.getId());
            }
            for (String id : CropBreedingPlanner.plan("", initial).reachable) {
                CropBreedingPlanner.Plan candidate = CropBreedingPlanner.plan(id, initial);
                if (candidate.steps.size() < (LONG_ROUTE_PREVIEW ? 8 : 4)
                    || candidate.steps.size() > (LONG_ROUTE_PREVIEW ? 24 : 8)) continue;
                if (LONG_ROUTE_PREVIEW && candidate.steps.stream()
                    .noneMatch(
                        step -> step.getParents()
                            .stream()
                            .map(ICropCard::getId)
                            .distinct()
                            .count() == 3))
                    continue;
                ICropCard crop = CropRegistry.instance.get(id);
                if (selected == null || (LONG_ROUTE_PREVIEW ? candidate.steps.size() > selected.steps.size()
                    : crop.getTier() > target.getTier()
                        || crop.getTier() == target.getTier() && candidate.steps.size() > selected.steps.size())) {
                    selected = candidate;
                    target = crop;
                }
            }
        }
        if (LONG_ROUTE_PREVIEW && selected == null) {
            syntheticLongRoute = true;
            return;
        }
        require(selected != null, "found a native multi-step route from basic crop tiers");
        Set<String> results = selected.steps.stream()
            .map(
                step -> step.getOutput()
                    .getId())
            .collect(Collectors.toSet());
        Set<String> parents = new java.util.TreeSet<>();
        for (ICropMutation step : selected.steps) {
            for (ICropCard parent : step.getParents())
                if (!results.contains(parent.getId())) parents.add(parent.getId());
        }
        net.minecraft.nbt.NBTTagList samples = new net.minecraft.nbt.NBTTagList();
        for (String id : parents) {
            NBTTagCompound sample = SeedStats.DEFAULT_ANALYZED.writeToNBT(new NBTTagCompound());
            sample.setString("id", id);
            samples.appendTag(sample);
        }
        for (int i = 0; i < input.getSizeInventory(); i++) input.setInventorySlotContents(i, null);
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        saved.setTag("cropArchive", samples);
        saved.setString("cropTarget", target.getId());
        saved.setString("cropActive", "");
        saved.removeTag("cropActiveSeed");
        machine.loadNBTData(saved);
        if (LONG_ROUTE_PREVIEW) input.setInventorySlotContents(8, new ItemStack(Blocks.netherrack));
        List<String> substrateReport = new ArrayList<>();
        for (ICropMutation step : machine.getPlan().steps) {
            int substrateCount = 0;
            for (var condition : step.getRequirements()) {
                if (!(condition instanceof com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement soil))
                    continue;
                substrateCount++;
                substrateReport.add(
                    step.getOutput()
                        .getId() + " | "
                        + soil.getDescription()
                        + " | NEI alternatives: "
                        + soil.getItemsForNEI()
                            .size());
            }
            if (substrateCount == 0) substrateReport.add(
                step.getOutput()
                    .getId() + " | Planting-soil fallback: "
                    + step.getOutput()
                        .getSoilTypes()
                        .getId()
                    + " | alternatives: "
                    + CropBreedingSubstrate.forMutation(step)
                        .get(0)
                        .alternatives()
                        .size());
        }
        Files.write(new File("crop-route-substrates.txt").toPath(), substrateReport, StandardCharsets.UTF_8);
        List<String> description = new ArrayList<>();
        description.add("Target: " + target.getId() + " | crop tier " + target.getTier());
        description.add("Starting parents: " + String.join(", ", parents));
        description.add("Steps: " + machine.getPlan().steps.size());
        for (ICropMutation step : machine.getPlan().steps) description.add(
            step.getParents()
                .stream()
                .map(ICropCard::getId)
                .collect(Collectors.joining(" + ")) + " -> "
                + step.getOutput()
                    .getId()
                + " | "
                + CropBreedingChance.percent(step.getOutput())
                + "%");
        Files.write(new File("crop-route-preview.txt").toPath(), description, StandardCharsets.UTF_8);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, ItemStack stack) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished || ++frames < 50) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
            if (page == 3) {
                IWidget graph = findGraph(
                    wrapper.getScreen()
                        .getMainPanel());
                require(graph != null, "route tab exposes a draggable lineage graph");
                if (PROGRESS_PREVIEW && previewProgressGraph(graph, mc)) return;
                if (LONG_ROUTE_PREVIEW && previewLongGraph(graph, mc)) return;
                if (ROUTE_PREVIEW && !PROGRESS_PREVIEW && !LONG_ROUTE_PREVIEW && previewGraph(graph, mc)) return;
                if (!ROUTE_PREVIEW && graphPhase == 0) {
                    graphChecks(graph);
                    invoke(graph, "setZoom", new Class<?>[] { float.class, float.class, float.class }, 0.5F, 100F, 70F);
                    graphPhase = 1;
                    frames = 0;
                    return;
                }
                if (!ROUTE_PREVIEW && graphPhase == 1) {
                    require(
                        Math.abs((float) field(graph, "zoom") - 0.5F) < 0.01F,
                        "animated zoom reaches requested scale");
                    ScreenShotHelper.saveScreenshot(
                        new File("."),
                        "crop-breeder-route-zoom.png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer());
                    invoke(graph, "resetView", new Class<?>[0]);
                    require((float) field(graph, "zoom") == 1F, "reset restores 100 percent and focuses target");
                    graphPhase = 2;
                    frames = 0;
                    return;
                }
            }
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "crop-breeder-" + page + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (page == 3) {
                IWidget graph = findGraph(
                    wrapper.getScreen()
                        .getMainPanel());
                var present = (java.util.Map<?, ?>) field(field(graph, "this$0"), "presentSubstrates");
                if (!ROUTE_PREVIEW && soilTooltipPhase == 1 && !present.isEmpty()) {
                    frames = 0;
                    return;
                }
                if (!ROUTE_PREVIEW) require(
                    soilTooltipPhase == 0 ? !present.isEmpty() : present.isEmpty(),
                    "substrate presence is synchronized while the GUI remains open");
                voltageTooltipChecks(graph, mc);
                if (!ROUTE_PREVIEW && soilTooltipPhase == 0) {
                    soilTooltipPhase = 1;
                    removeSubstrate = true;
                    frames = 0;
                    return;
                }
            }
            List<PageButton> tabs = new ArrayList<>();
            collectTabs(
                wrapper.getScreen()
                    .getMainPanel(),
                tabs);
            require(tabs.size() == 4, "GUI exposes four tabs");
            if (++page == 4) {
                finish(null);
                return;
            }
            tabs.get(page)
                .onMousePressed(0);
            frames = 0;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void collectTabs(IWidget widget, List<PageButton> tabs) {
        if (widget instanceof PageButton button) tabs.add(button);
        for (IWidget child : widget.getChildren()) collectTabs(child, tabs);
    }

    private void voltageTooltipChecks(IWidget graph, Minecraft mc) throws Exception {
        Object gui = field(graph, "this$0");
        List<?> nodes = new ArrayList<>(((java.util.Map<?, ?>) field(field(graph, "layout"), "nodes")).values());
        List<gregtech.common.modularui2.widget.SlotLikeButtonWidget> slots = graph.getChildren()
            .stream()
            .filter(gregtech.common.modularui2.widget.SlotLikeButtonWidget.class::isInstance)
            .map(gregtech.common.modularui2.widget.SlotLikeButtonWidget.class::cast)
            .collect(Collectors.toList());
        require(slots.size() == nodes.size(), "every route node has a crop tooltip");
        for (int i = 0; i < slots.size(); i++) {
            String id = (String) field(nodes.get(i), "uid");
            ItemStack stack = (ItemStack) invoke(gui, "icon", new Class<?>[] { String.class }, id);
            var original = new com.cleanroommc.modularui.screen.RichTooltip().addFromItem(stack);
            List<String> nativeLines = new ArrayList<>(
                ((com.cleanroommc.modularui.drawable.text.RichText) original.getRichText()).getAsStrings());
            while (!nativeLines.isEmpty() && nativeLines.get(nativeLines.size() - 1)
                .isEmpty()) nativeLines.remove(nativeLines.size() - 1);
            var tooltip = slots.get(i)
                .getTooltip();
            tooltip.buildTooltip();
            List<String> actual = ((com.cleanroommc.modularui.drawable.text.RichText) tooltip.getRichText())
                .getAsStrings();
            require(
                actual.size() >= nativeLines.size() && actual.subList(0, nativeLines.size())
                    .equals(nativeLines),
                "complete original seed tooltip precedes machine-specific information");
            NBTTagCompound step = (NBTTagCompound) field(nodes.get(i), "step");
            int tier = step == null ? CropRegistry.instance.get(id)
                .getMachineBreedingRecipeTier() : gregtech.api.util.GTUtility.getTier(step.getInteger("eu"));
            String expected = net.minecraft.util.StatCollector.translateToLocalFormatted(
                "gtnr.crop_breeder.required_voltage",
                gregtech.api.enums.GTValues.VN[Math.max(1, tier)]);
            require(
                actual.indexOf(expected) >= nativeLines.size(),
                "breeding voltage tier is appended after original tooltip");
            List<String> extra = actual.subList(nativeLines.size(), actual.size());
            require(
                extra.stream()
                    .noneMatch(
                        line -> line.contains("等待杂交") || line.startsWith("基础：")
                            || line.contains("Pending breeding")
                            || line.startsWith("Base:")),
                "redundant status and base recipe lines are removed");
            {
                var requirements = (net.minecraft.nbt.NBTTagList) field(nodes.get(i), "requirements");
                if (step == null && !MIXED_ROUTE_PREVIEW && !EXPANDED_ROUTE_PREVIEW && !syntheticLongRoute) {
                    long expectedSubstrates = CropBreedingSubstrate.forCrop(CropRegistry.instance.get(id))
                        .size();
                    require(
                        requirements.tagCount() == expectedSubstrates,
                        "starting parents display substrates including planting-soil fallback");
                }
                if (LONG_ROUTE_PREVIEW && !syntheticLongRoute)
                    require(requirements.tagCount() > 0, "every crop in the native long route displays a substrate");
                if (id.equals("cropsnh:evilOre") && LONG_ROUTE_PREVIEW && !syntheticLongRoute) {
                    NBTTagCompound fallback = requirements.getCompoundTagAt(0);
                    ItemStack shown = (ItemStack) invoke(
                        gui,
                        "requirementItem",
                        new Class<?>[] { NBTTagCompound.class },
                        fallback);
                    require(
                        shown != null && shown.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.netherrack),
                        "EvilOre route icon shows the detected netherrack planting soil");
                    require(
                        (boolean) invoke(
                            gui,
                            "hasSubstrateCheckmark",
                            new Class<?>[] { NBTTagCompound.class },
                            fallback),
                        "detected planting soil receives the same green check as extra substrate");
                }
                for (int j = 0; j < requirements.tagCount(); j++) {
                    NBTTagCompound requirement = requirements.getCompoundTagAt(j);
                    if (!requirement.getBoolean("soil")) continue;
                    String prefix = net.minecraft.util.StatCollector
                        .translateToLocalFormatted("gtnr.crop_breeder.required_soil", "");
                    require(
                        extra.stream()
                            .anyMatch(line -> line.startsWith(prefix) && line.length() > prefix.length()),
                        "substrate tooltip includes a concrete item name");
                    var present = (java.util.Map<?, ?>) field(gui, "presentSubstrates");
                    ItemStack supplied = (ItemStack) present.get(requirement.getString("key"));
                    boolean archived = ((List<?>) field(gui, "archive")).contains(id);
                    require(
                        (boolean) invoke(
                            gui,
                            "hasSubstrateCheckmark",
                            new Class<?>[] { NBTTagCompound.class },
                            requirement) == (archived || supplied != null),
                        "substrate icon checkmark follows archive or actual input detection");
                    if (supplied != null) require(
                        extra.stream()
                            .anyMatch(line -> line.contains(supplied.getDisplayName()) && line.contains("✔")),
                        "supplied substrate displays its actual name with a green check");
                    else if (archived) require(
                        extra.stream()
                            .anyMatch(line -> line.startsWith(prefix) && line.contains("✔")),
                        "archived parents keep their substrate checkmark after input removal");
                    else require(
                        extra.stream()
                            .noneMatch(line -> line.startsWith(prefix) && line.contains("✔")),
                        "removing substrate clears the satisfied marker");
                }
            }
            if (i == 0) {
                if (!ROUTE_PREVIEW) ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "crop-substrate-icons-" + (soilTooltipPhase == 0 ? "present" : "missing") + ".png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                Files.write(new File("crop-voltage-tooltip.txt").toPath(), actual, StandardCharsets.UTF_8);
                tooltip.pos(275, 90)
                    .draw(graph.getContext());
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "crop-breeder-voltage-tooltip.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "crop-substrate-" + (soilTooltipPhase == 0 ? "present" : "missing") + ".png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
            }
        }
    }

    private boolean previewProgressGraph(IWidget graph, Minecraft mc) throws ReflectiveOperationException {
        Object gui = field(graph, "this$0");
        String target = ((com.cleanroommc.modularui.value.sync.StringSyncValue) field(gui, "target")).getStringValue();
        if (graphPhase > 0) {
            float expected = graphPhase / 4F;
            List<IWidget> connections = graph.getChildren()
                .stream()
                .filter(
                    widget -> widget.getClass()
                        .getSimpleName()
                        .equals("BranchProgressWidget"))
                .collect(Collectors.toList());
            require(connections.size() == 1, "one complete connection owns every parent branch, bar and arrow");
            IWidget connection = connections.get(0);
            require(
                connection.getChildren()
                    .isEmpty(),
                "the connection contains no independent segment widgets");
            require(
                Math.abs((double) invoke(connection, "getCurrentProgress", new Class<?>[0]) - expected) < 0.001,
                "one progress value controls the complete connection");
            int height = connection.getArea().height;
            require(
                (int) invoke(connection, "getFillTop", new Class<?>[0]) == height - Math.round(height * expected),
                "one continuous bottom-to-top fill boundary reveals every part of the shape");
            require(
                (boolean) invoke(connection, "isComplete", new Class<?>[0]) == (graphPhase == 4),
                "the complete connection turns green only when the result is archived");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "crop-progress-" + graphPhase * 25 + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (graphPhase == 4) return false;
        }
        graphPhase++;
        ((com.cleanroommc.modularui.value.sync.IntSyncValue) field(gui, "duration")).setIntValue(100, false, false);
        ((com.cleanroommc.modularui.value.sync.IntSyncValue) field(gui, "progress"))
            .setIntValue(graphPhase * 25, false, false);
        ((com.cleanroommc.modularui.value.sync.StringSyncValue) field(gui, "active"))
            .setStringValue(graphPhase == 4 ? "" : target, false, false);
        if (graphPhase == 4) {
            List<String> archived = new ArrayList<>();
            for (Object id : (List<?>) field(gui, "archive")) archived.add((String) id);
            archived.add(target);
            var archiveField = gui.getClass()
                .getDeclaredField("archive");
            archiveField.setAccessible(true);
            archiveField.set(gui, archived);
        }
        frames = 0;
        return true;
    }

    private boolean previewLongGraph(IWidget graph, Minecraft mc) throws Exception {
        if (!syntheticLongRoute) {
            Object gui = field(graph, "this$0");
            for (Object node : ((java.util.Map<?, ?>) field(field(graph, "layout"), "nodes")).values()) {
                if (!field(node, "uid").equals("cropsnh:bonsaiSpruce")) continue;
                var requirements = (net.minecraft.nbt.NBTTagList) field(node, "requirements");
                ItemStack shown = (ItemStack) invoke(
                    gui,
                    "requirementItem",
                    new Class<?>[] { NBTTagCompound.class },
                    requirements.getCompoundTagAt(0));
                require(shown != null, "planting-soil alternatives have a visible item icon");
                displayedPlantingSoils
                    .add(net.minecraft.item.Item.getIdFromItem(shown.getItem()) + ":" + shown.getItemDamage());
                break;
            }
        }
        if (graphPhase == 0) {
            if (syntheticLongRoute) prepareSyntheticLongRoute(graph);
            Object layout = field(graph, "layout");
            List<?> nodes = new ArrayList<>(((java.util.Map<?, ?>) field(layout, "nodes")).values());
            int steps = 0, threeParentSteps = 0, depth = 0;
            Set<String> distinctSteps = new HashSet<>();
            for (Object node : nodes) {
                if (field(node, "step") != null) {
                    steps++;
                    distinctSteps.add((String) field(node, "uid"));
                }
                if (((List<?>) field(node, "parents")).size() == 3) threeParentSteps++;
                depth = Math.max(depth, (int) field(node, "depth"));
            }
            require(
                distinctSteps.size() >= 8 && threeParentSteps >= 1,
                "long route contains at least eight steps and a three-parent mutation");
            parentArrowSpacing(layout);
            Files.write(
                new File("crop-long-route-preview.txt").toPath(),
                List.of(
                    "Synthetic layout: " + syntheticLongRoute,
                    "Steps: " + distinctSteps.size(),
                    "Displayed step nodes including repeated references: " + steps,
                    "Three-parent steps: " + threeParentSteps,
                    "Maximum depth: " + depth),
                StandardCharsets.UTF_8);
            float fit = Math
                .min(1F, Math.min(220F / (int) field(layout, "width"), 200F / (int) field(layout, "height")));
            invoke(graph, "setZoom", new Class<?>[] { float.class, float.class, float.class }, fit, 110F, 0F);
        } else if (graphPhase == 1) {
            invoke(graph, "focusTarget", new Class<?>[0]);
        } else if (graphPhase == 2 || graphPhase == 3 || graphPhase == 5 || graphPhase == 7) {
            if (graphPhase == 2) {
                Object layout = field(graph, "layout");
                float zoom = (float) field(graph, "zoom");
                require(
                    (int) field(layout, "width") * zoom <= 220.1F && (int) field(layout, "height") * zoom <= 200.1F,
                    "the complete seventeen-step route fits inside one screenshot");
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "crop-long-route-planting-soil-full.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
            }
            if (graphPhase == 7 && !syntheticLongRoute) require(
                displayedPlantingSoils.size() > 1,
                "planting-soil icon actually cycles between alternatives during the preview");
            String view = graphPhase == 2 ? "top"
                : graphPhase == 3 ? "bottom" : graphPhase == 5 ? "three-parent" : "aligned-bars";
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "crop-long-route-" + view + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (graphPhase == 7 || graphPhase == 5 && syntheticLongRoute) return false;
            if (graphPhase == 5) {
                invoke(graph, "setZoom", new Class<?>[] { float.class, float.class, float.class }, 0.8F, 110F, 0F);
            } else if (graphPhase == 2) {
                var viewport = (com.cleanroommc.modularui.widget.ScrollWidget<?>) field(graph, "viewport");
                var vertical = (com.cleanroommc.modularui.widget.scroll.VerticalScrollData) field(graph, "vertical");
                vertical.scrollTo(viewport.getScrollArea(), Integer.MAX_VALUE);
            } else {
                float detailZoom = 0.8F;
                for (Object node : ((java.util.Map<?, ?>) field(field(graph, "layout"), "nodes")).values()) {
                    List<?> parents = (List<?>) field(node, "parents");
                    if (parents.size() != 3) continue;
                    int span = (int) field(parents.get(2), "x") - (int) field(parents.get(0), "x") + 42;
                    detailZoom = Math.max(0.25F, Math.min(detailZoom, 210F / span));
                    break;
                }
                invoke(
                    graph,
                    "setZoom",
                    new Class<?>[] { float.class, float.class, float.class },
                    detailZoom,
                    110F,
                    0F);
            }
        } else if (graphPhase == 4 || graphPhase == 6) {
            for (Object node : ((java.util.Map<?, ?>) field(field(graph, "layout"), "nodes")).values()) {
                List<?> parents = (List<?>) field(node, "parents");
                if (graphPhase == 4) {
                    if (parents.size() != 3) continue;
                } else {
                    if (!field(node, "uid").equals("cropsnh:diareed")) continue;
                }
                var viewport = (com.cleanroommc.modularui.widget.ScrollWidget<?>) field(graph, "viewport");
                var vertical = (com.cleanroommc.modularui.widget.scroll.VerticalScrollData) field(graph, "vertical");
                var horizontal = (com.cleanroommc.modularui.widget.scroll.HorizontalScrollData) field(
                    graph,
                    "horizontal");
                float zoom = (float) field(graph, "zoom");
                vertical.scrollTo(viewport.getScrollArea(), Math.round(((int) field(node, "y") - 10) * zoom));
                horizontal.scrollTo(
                    viewport.getScrollArea(),
                    Math.round(
                        (float) invoke(graph, "contentOffsetX", new Class<?>[0]) + ((int) field(node, "x") + 9) * zoom)
                        - horizontal.getFullVisibleSize(viewport.getScrollArea()) / 2);
                break;
            }
        }
        graphPhase++;
        frames = 0;
        return true;
    }

    private void prepareSyntheticLongRoute(IWidget graph) throws ReflectiveOperationException {
        Object gui = field(graph, "this$0");
        String target = ((com.cleanroommc.modularui.value.sync.StringSyncValue) field(gui, "target")).getStringValue();
        List<String> crops = CropRegistry.instance.getAllInRegistrationOrder()
            .stream()
            .map(ICropCard::getId)
            .filter(id -> !id.equals(target))
            .limit(40)
            .collect(Collectors.toList());
        List<NBTTagCompound> steps = new ArrayList<>();
        int cursor = 0;
        String previous = crops.get(cursor++);
        for (int i = 0; i < 14; i++) {
            List<String> parents = new ArrayList<>(List.of(previous, crops.get(cursor++)));
            if (i == 4 || i == 9) parents.add(crops.get(cursor++));
            String result = i == 13 ? target : crops.get(cursor++);
            NBTTagCompound step = graphStep(result, parents.toArray(new String[0]));
            step.setInteger("eu", 120);
            steps.add(step);
            previous = result;
        }
        var routeField = gui.getClass()
            .getDeclaredField("route");
        routeField.setAccessible(true);
        routeField.set(gui, steps);
        ((java.util.Map<?, ?>) field(graph, "history")).clear();
        graph.onUpdate();
    }

    private boolean previewGraph(IWidget graph, Minecraft mc) throws ReflectiveOperationException {
        String previewName = EXPANDED_ROUTE_PREVIEW ? "crop-expanded-" + previewParentCount : "crop-route-preview";
        if (graphPhase == 0) {
            if (EXPANDED_ROUTE_PREVIEW) prepareExpandedRouteGraph(graph);
            if (MIXED_ROUTE_PREVIEW) {
                graphChecks(graph);
                prepareMixedRouteGraph(graph);
            }
            Object layout = field(graph, "layout");
            require(((java.util.Map<?, ?>) field(layout, "nodes")).size() >= 9, "preview renders a multi-step lineage");
            float fit = Math.max(
                0.25F,
                Math.min(1F, Math.min(220F / (int) field(layout, "width"), 200F / (int) field(layout, "height"))));
            invoke(graph, "setZoom", new Class<?>[] { float.class, float.class, float.class }, fit, 110F, 0F);
            graphPhase = 1;
        } else if (graphPhase == 1) {
            invoke(graph, "focusTarget", new Class<?>[0]);
            graphPhase = 2;
        } else if (graphPhase == 2) {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                previewName + "-overview.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            invoke(graph, "resetView", new Class<?>[0]);
            graphPhase = 3;
        } else {
            ScreenShotHelper.saveScreenshot(
                new File("."),
                previewName + "-detail.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (EXPANDED_ROUTE_PREVIEW && previewParentCount == 3) {
                previewParentCount = 4;
                graphPhase = 0;
                frames = 0;
                return true;
            }
            return false;
        }
        frames = 0;
        return true;
    }

    private void prepareMixedRouteGraph(IWidget graph) throws ReflectiveOperationException {
        Object gui = field(graph, "this$0");
        var routeField = gui.getClass()
            .getDeclaredField("route");
        routeField.setAccessible(true);
        List<?> currentRoute = (List<?>) routeField.get(gui);
        NBTTagCompound last = (NBTTagCompound) ((NBTTagCompound) currentRoute.get(currentRoute.size() - 1)).copy();
        String target = last.getString("result");
        List<String> crops = CropRegistry.instance.getAllInRegistrationOrder()
            .stream()
            .map(ICropCard::getId)
            .filter(id -> !id.equals(target))
            .limit(9)
            .collect(Collectors.toList());
        // Synthetic topology for visual inspection only; no mutation registry or machine recipes are changed.
        NBTTagCompound first = graphStep(crops.get(2), crops.get(0), crops.get(1));
        NBTTagCompound second = graphStep(crops.get(5), crops.get(2), crops.get(3), crops.get(4));
        first.setInteger("eu", 30);
        second.setInteger("eu", 120);
        for (int i = 0; i < 4; i++) last.removeTag("parentRequirements" + i);
        last.setString("parent0", crops.get(5));
        for (int i = 1; i < 4; i++) last.setString("parent" + i, crops.get(i + 5));
        routeField.set(gui, List.of(first, second, last));
        ((java.util.Map<?, ?>) field(graph, "history")).clear();
        graph.onUpdate();
        parentArrowSpacing(field(graph, "layout"));
    }

    private void prepareExpandedRouteGraph(IWidget graph) throws ReflectiveOperationException {
        Object gui = field(graph, "this$0");
        var routeField = gui.getClass()
            .getDeclaredField("route");
        routeField.setAccessible(true);
        List<?> currentRoute = (List<?>) routeField.get(gui);
        NBTTagCompound last = (NBTTagCompound) ((NBTTagCompound) currentRoute.get(currentRoute.size() - 1)).copy();
        String target = last.getString("result");
        int count = previewParentCount;
        List<String> crops = CropRegistry.instance.getAllInRegistrationOrder()
            .stream()
            .map(ICropCard::getId)
            .filter(id -> !id.equals(target))
            .limit(count * (count + 1))
            .collect(Collectors.toList());
        // A fully expanded synthetic tree shows the layout without modifying any real mutation recipes.
        List<NBTTagCompound> steps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            last.removeTag("parent" + i);
            last.removeTag("parentRequirements" + i);
        }
        for (int i = 0; i < count; i++) {
            String result = crops.get(count * count + i);
            NBTTagCompound step = graphStep(
                result,
                crops.subList(i * count, (i + 1) * count)
                    .toArray(new String[0]));
            step.setInteger("eu", 120);
            steps.add(step);
            last.setString("parent" + i, result);
        }
        steps.add(last);
        routeField.set(gui, steps);
        ((java.util.Map<?, ?>) field(graph, "history")).clear();
        graph.onUpdate();
        parentArrowSpacing(field(graph, "layout"));
    }

    private static IWidget findGraph(IWidget widget) {
        if (widget.getClass()
            .getSimpleName()
            .equals("BreedingRouteWidget")) return widget;
        for (IWidget child : widget.getChildren()) {
            IWidget found = findGraph(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void graphChecks(IWidget graph) throws ReflectiveOperationException {
        Object layout = field(graph, "layout");
        require(
            ((java.util.Map<?, ?>) field(layout, "nodes")).size() == 5,
            "native four-parent route renders five crop nodes");
        parentArrowSpacing(layout);
        var constructor = layout.getClass()
            .getDeclaredConstructor(List.class, String.class);
        constructor.setAccessible(true);
        NBTTagCompound first = graphStep("e", "a", "b");
        net.minecraft.nbt.NBTTagList requirements = new net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < 3; i++) requirements.appendTag(new NBTTagCompound());
        first.setTag("requirements", requirements);
        Object deep = constructor.newInstance(List.of(first, graphStep("g", "e", "c", "d", "f")), "g");
        parentArrowSpacing(deep);
        Object threeParents = constructor.newInstance(List.of(graphStep("d", "a", "b", "c")), "d");
        parentArrowSpacing(threeParents);
        Object uneven = constructor.newInstance(
            List.of(
                graphStep("p", "a", "b"),
                graphStep("q", "c", "d", "e"),
                graphStep("r", "f", "g", "h", "i"),
                graphStep("target", "p", "q", "r", "j")),
            "target");
        parentArrowSpacing(uneven);
        NBTTagCompound tall = graphStep("p", "a", "b");
        tall.setTag("requirements", requirements);
        Object mixedHeights = constructor
            .newInstance(List.of(tall, graphStep("q", "c", "d", "e"), graphStep("target", "p", "q")), "target");
        parentArrowSpacing(mixedHeights);
        List<?> nodes = new ArrayList<>(((java.util.Map<?, ?>) field(deep, "nodes")).values());
        require(nodes.size() == 7, "multi-step route includes intermediate crop and every ancestor");
        for (int i = 0; i < nodes.size(); i++) {
            Object a = nodes.get(i);
            int ax = (int) field(a, "x"), ay = (int) field(a, "y");
            int ah = (int) invoke(a, "height", new Class<?>[0]);
            for (int j = i + 1; j < nodes.size(); j++) {
                Object b = nodes.get(j);
                int bx = (int) field(b, "x"), by = (int) field(b, "y");
                int bh = (int) invoke(b, "height", new Class<?>[0]);
                require(
                    Math.abs(ax - bx) >= 42 || ay + ah <= by || by + bh <= ay,
                    "crop nodes, chance labels and substrate icons do not overlap");
            }
        }
        Object cycle = constructor.newInstance(List.of(graphStep("a", "b"), graphStep("b", "a")), "a");
        require(
            ((java.util.Map<?, ?>) field(cycle, "nodes")).size() <= 3,
            "cyclic snapshot cannot expand indefinitely");
        Object empty = constructor.newInstance(List.of(), "");
        require(((java.util.Map<?, ?>) field(empty, "nodes")).isEmpty(), "empty target clears route graph");
    }

    private static NBTTagCompound graphStep(String result, String... parents) {
        NBTTagCompound step = new NBTTagCompound();
        step.setString("result", result);
        for (int i = 0; i < parents.length; i++) step.setString("parent" + i, parents[i]);
        return step;
    }

    private static void parentArrowSpacing(Object layout) throws ReflectiveOperationException {
        List<?> nodes = new ArrayList<>(((java.util.Map<?, ?>) field(layout, "nodes")).values());
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Object a = nodes.get(i), b = nodes.get(j);
                if ((int) field(a, "depth") != (int) field(b, "depth")) continue;
                require(
                    Math.abs((int) field(a, "x") - (int) field(b, "x")) >= 54,
                    "neighboring subtrees leave room for crop icons and labels on each row");
                if (((List<?>) field(a, "parents")).size() >= 2 && ((List<?>) field(b, "parents")).size() >= 2) require(
                    invoke(a, "junctionY", new Class<?>[0]).equals(invoke(b, "junctionY", new Class<?>[0])),
                    "shared parent bars align across each row despite different substrate counts");
            }
        }
        for (Object result : nodes) {
            List<?> parents = (List<?>) field(result, "parents");
            if (parents.size() < 2) continue;
            int left = (int) field(parents.get(0), "x");
            int right = (int) field(parents.get(parents.size() - 1), "x");
            require(2 * (int) field(result, "x") == left + right, "result is centered over the shared parent bar");
            for (int parent = 0; parent < parents.size(); parent++) {
                require(
                    ((int) field(parents.get(parent), "x") - left) * (parents.size() - 1) == parent * (right - left),
                    "parents remain evenly spaced despite different descendant widths");
            }
            int junctionY = (int) invoke(result, "junctionY", new Class<?>[0]);
            for (Object parent : parents) require(
                (int) field(parent, "y") - 2 > junctionY,
                "parent seed icons leave room for the shared connector above them");
        }
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        var field = owner.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Object invoke(Object owner, String name, Class<?>[] parameters, Object... values)
        throws ReflectiveOperationException {
        var method = owner.getClass()
            .getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(owner, values);
    }

    private void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        if (failure != null) failure.printStackTrace();
        try {
            Files.write(
                new File("result.txt").toPath(),
                (failure == null ? "PASS" : "FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("CROP_BREEDER_QA: " + (failure == null ? "PASS" : "FAIL"));
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(failure == null ? 0 : 1, true);
        }, "crop-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
        System.out.println("CROP_BREEDER_QA: " + message);
    }
}
