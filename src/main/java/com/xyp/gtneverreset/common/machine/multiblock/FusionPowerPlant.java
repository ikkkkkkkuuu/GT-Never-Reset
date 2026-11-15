package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.FusionPowerPlantGui;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.WirelessNetworkManager;

@IMetaTileEntity.SkipGenerateDescription
public final class FusionPowerPlant extends MTEEnhancedMultiBlockBase<FusionPowerPlant>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    public static final int RATED_EUT = 32_768;
    public static final long IGNITION_EU = 1_000_000;
    public static final long OUTPUT_CAPACITY = RATED_EUT * 200L;
    public static final int AMBIENT_K = 300;
    public static final int WARNING_K = 650;
    public static final int SCRAM_K = 800;
    public static final int EXPLOSION_K = 1_000;
    public static final int MAX_COOLANT_PER_TICK = 20;
    private static final int CASING_TEXTURE = 16;
    private static final int FUEL_CREDIT_PER_BATCH = 2_000;
    private static final double COOLING_PER_LITRE = 0.1;
    private static final double REACTION_HEAT_PER_TICK = 0.8;
    private static final double MAX_DECAY_HEAT_PER_TICK = 0.6;
    private static final double DECAY_HEAT_RESPONSE = 0.002;
    private static final double PASSIVE_COOLING_PER_TICK = 0.005;
    private static final int COOLANT_CONTROL_K_PER_LITRE = 5;
    private static final int WARNING_POWER_LIMIT = 25;
    private static final String PIECE = "main";

    private double temperature = AMBIENT_K;
    private double decayHeat;
    private long ignitionEnergy;
    private long outputEnergy;
    private int fuelCredit;
    private int targetPower = 100;
    private boolean ignited;
    private boolean thermalTrip;
    private UUID ownerId;

    private IStructureDefinition<FusionPowerPlant> structure;
    private int generatedEUt;
    private int exportedEUt;
    private int coolantUsed;
    private int coolantDemand;
    private boolean coolantBlocked;
    private boolean fuelBlocked;
    private boolean fuelMissing;

    public FusionPowerPlant(int id, String name, String regionalName) {
        super(id, name, regionalName);
        fixAllIssues();
    }

    public FusionPowerPlant(String name) {
        super(name);
        fixAllIssues();
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new FusionPowerPlant(mName);
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        bindOwner(tile);
        generatedEUt = 0;
        exportedEUt = 0;
        coolantUsed = 0;
        coolantBlocked = false;
        fuelBlocked = false;
        fuelMissing = false;
        if (temperature >= EXPLOSION_K) {
            explodeMultiblock();
            return;
        }
        if (temperature >= SCRAM_K) {
            thermalTrip = true;
            tile.disableWorking();
        }
        boolean operating = mMachine && tile.isAllowedToWork() && !thermalTrip && ownerId != null;
        if (!operating) ignited = false;
        if (mMachine) {
            if (operating) runFusion();
            exportedEUt = exportEnergy();
        }
        // Heat continues after manual shutdown, loss of structure and emergency shutdown.
        double power = (double) generatedEUt / RATED_EUT;
        decayHeat += (power * MAX_DECAY_HEAT_PER_TICK - decayHeat) * DECAY_HEAT_RESPONSE;
        temperature += power * REACTION_HEAT_PER_TICK + decayHeat;
        coolantDemand = Math.min(
            MAX_COOLANT_PER_TICK,
            Math.max(0, (int) Math.ceil((temperature - AMBIENT_K) / COOLANT_CONTROL_K_PER_LITRE)));
        if (mMachine && coolantDemand > 0) coolReactor();
        temperature = Math.max(AMBIENT_K, temperature - PASSIVE_COOLING_PER_TICK - coolantUsed * COOLING_PER_LITRE);
        if (temperature >= EXPLOSION_K) {
            explodeMultiblock();
            return;
        }
        if (temperature >= SCRAM_K) {
            thermalTrip = true;
            ignited = false;
            tile.disableWorking();
        }
        mEUt = generatedEUt;
        mEfficiency = generatedEUt > 0 ? 10_000 : 0;
        mMaxProgresstime = generatedEUt > 0 ? 20 : 0;
        tile.setActive(generatedEUt > 0);
        tile.markDirty();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        // Continuous generation and cooling are advanced together in onPostTick, including while stopped.
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        super.stopMachine(reason);
        ignited = false;
        generatedEUt = 0;
    }

    @Override
    public void explodeMultiblock() {
        // One controller blast also destroys adjacent blocks without iterating hatch lists modified by removal.
        getBaseMetaTileEntity().doExplosion(GTValues.V[8]);
    }

    @Override
    public IStructureDefinition<FusionPowerPlant> getStructureDefinition() {
        if (structure == null) {
            structure = StructureDefinition.<FusionPowerPlant>builder()
                .addShape(PIECE, transpose(MultiblockStructureLoader.load("fusion_power_plant")))
                .addElement(
                    'C',
                    buildHatchAdder(FusionPowerPlant.class).atLeast(InputHatch, OutputHatch, Energy)
                        .casingIndex(CASING_TEXTURE)
                        .hint(1)
                        .buildAndChain(ofBlock(GregTechAPI.sBlockCasings2, 0)))
                .build();
        }
        return structure;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(PIECE, stack, 1, 1, 0, budget, environment, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(PIECE, 1, 1, 0, errors);
        collectTooltipStructureRequirements(errors);
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        checkHatchMin(errors, InputHatch, 3);
        checkHatchMin(errors, OutputHatch, 2);
        checkHatchExact(errors, Energy, 1);
    }

    private void runFusion() {
        if (targetPower <= 0) {
            ignited = false;
            return;
        }
        int power = temperature >= WARNING_K ? Math.min(targetPower, WARNING_POWER_LIMIT) : targetPower;
        int generation = RATED_EUT * power / 100;
        if (outputEnergy > OUTPUT_CAPACITY - generation) return;
        if (!ignited) {
            chargeIgnition();
            if (ignitionEnergy < IGNITION_EU) return;
        }
        if (fuelCredit < power && !loadFuel()) return;
        if (!ignited) {
            ignitionEnergy -= IGNITION_EU;
            ignited = true;
        }
        fuelCredit -= power;
        generatedEUt = generation;
        outputEnergy += generation;
    }

    private void chargeIgnition() {
        for (var hatch : mEnergyHatches) {
            if (!hatch.isValid()) continue;
            var tile = hatch.getBaseMetaTileEntity();
            long amount = Math.min(
                IGNITION_EU - ignitionEnergy,
                Math.min(tile.getStoredEU(), hatch.maxEUInput() * hatch.maxAmperesIn()));
            if (amount > 0 && tile.decreaseStoredEnergyUnits(amount, false)) ignitionEnergy += amount;
        }
    }

    private boolean loadFuel() {
        FluidStack deuterium = Materials.Deuterium.getGas(1);
        FluidStack tritium = Materials.Tritium.getGas(1);
        if (!depleteInput(deuterium, true) || !depleteInput(tritium, true)) {
            fuelMissing = true;
            return false;
        }
        FluidStack helium = Materials.Helium.getGas(1);
        if (fillOutputs(helium, false) < 1) {
            fuelBlocked = true;
            return false;
        }
        depleteInput(deuterium);
        depleteInput(tritium);
        fillOutputs(helium, true);
        fuelCredit += FUEL_CREDIT_PER_BATCH;
        return true;
    }

    private void coolReactor() {
        FluidStack coolant = FluidRegistry.getFluidStack("ic2coolant", coolantDemand);
        FluidStack hot = FluidRegistry.getFluidStack("ic2hotcoolant", coolantDemand);
        if (coolant == null || hot == null) return;
        int available = depleteInputQuantity(coolant, true);
        hot.amount = Math.min(coolantDemand, available);
        if (hot.amount <= 0) return;
        int room = fillOutputs(hot, false);
        coolantBlocked = room < hot.amount;
        coolant.amount = room;
        if (room <= 0) return;
        int drained = depleteInputQuantity(coolant, false);
        hot.amount = drained;
        coolantUsed = fillOutputs(hot, true);
    }

    private int fillOutputs(FluidStack fluid, boolean commit) {
        int remaining = fluid.amount;
        for (var hatch : mOutputHatches) {
            if (!hatch.isValid() || !hatch.canStoreFluid(fluid)) continue;
            FluidStack portion = fluid.copy();
            portion.amount = remaining;
            remaining -= hatch.fill(portion, commit);
            if (remaining == 0) break;
        }
        return fluid.amount - remaining;
    }

    private void bindOwner(IGregTechTileEntity tile) {
        UUID placedBy = tile.getOwnerUuid();
        if (ownerId != null || placedBy == null || placedBy.equals(GTUtility.defaultUuid)) return;
        ownerId = placedBy;
        WirelessNetworkManager.strongCheckOrAddUser(ownerId);
    }

    private int exportEnergy() {
        if (ownerId == null || outputEnergy <= 0) return 0;
        if (!WirelessNetworkManager.addEUToGlobalEnergyMap(ownerId, outputEnergy)) return 0;
        int transferred = (int) outputEnergy;
        outputEnergy = 0;
        return transferred;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER)
            .extFacing()
            .build() };
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.fusion_power_plant.name";
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipPath() {
        return "fusion-power-plant";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of(
            "power",
            RATED_EUT,
            "ignition",
            IGNITION_EU,
            "warning",
            WARNING_K,
            "scram",
            SCRAM_K,
            "explosion",
            EXPLOSION_K,
            "coolant",
            MAX_COOLANT_PER_TICK * 20);
    }

    @Override
    public String[] getInfoData() {
        return new String[] { StatCollector.translateToLocal(getStatusKey()),
            Math.round(temperature) + " / " + EXPLOSION_K + " K", generatedEUt + " EU/t",
            coolantUsed * 20 + " / " + coolantDemand * 20 + " L/s" };
    }

    public String getStatusKey() {
        // #tr gtnr.fusion.status.owner
        // # No owner bound to wireless network
        // # zh_CN 尚未绑定无线电网所有者
        if (ownerId == null && mMachine) return "gtnr.fusion.status.owner";
        // #tr gtnr.fusion.status.structure
        // # Incomplete structure - residual heat
        // # zh_CN 结构未成型，余热仍在累积
        if (!mMachine) return "gtnr.fusion.status.structure";
        // #tr gtnr.fusion.status.trip
        // # Emergency shutdown - keep cooling!
        // # zh_CN 紧急停堆，请保持冷却！
        if (thermalTrip) return "gtnr.fusion.status.trip";
        // #tr gtnr.fusion.status.hot_output
        // # Hot coolant output blocked
        // # zh_CN 热冷却液输出堵塞
        if (coolantBlocked) return "gtnr.fusion.status.hot_output";
        // #tr gtnr.fusion.status.coolant
        // # Insufficient coolant flow
        // # zh_CN 冷却液流量不足
        if (coolantUsed < coolantDemand) return "gtnr.fusion.status.coolant";
        // #tr gtnr.fusion.status.warning
        // # High temperature - power limited to 25%
        // # zh_CN 高温警告，功率限制为 25%
        if (temperature >= WARNING_K) return "gtnr.fusion.status.warning";
        // #tr gtnr.fusion.status.stopped
        // # Stopped / cooling
        // # zh_CN 已停堆 / 冷却中
        if (!getBaseMetaTileEntity().isAllowedToWork() || targetPower == 0) return "gtnr.fusion.status.stopped";
        // #tr gtnr.fusion.status.output
        // # Energy buffer full - reaction paused
        // # zh_CN 电力缓冲已满，反应暂停
        if (outputEnergy > OUTPUT_CAPACITY - RATED_EUT * targetPower / 100) return "gtnr.fusion.status.output";
        // #tr gtnr.fusion.status.fuel_output
        // # Helium output blocked
        // # zh_CN 氦气输出堵塞
        if (fuelBlocked) return "gtnr.fusion.status.fuel_output";
        // #tr gtnr.fusion.status.fuel
        // # Waiting for deuterium and tritium
        // # zh_CN 等待氘和氚
        if (fuelMissing) return "gtnr.fusion.status.fuel";
        // #tr gtnr.fusion.status.charging
        // # Charging ignition buffer
        // # zh_CN 正在积蓄点火能量
        if (!ignited) return "gtnr.fusion.status.charging";
        // #tr gtnr.fusion.status.running
        // # Fusion online
        // # zh_CN 聚变发电中
        return "gtnr.fusion.status.running";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new FusionPowerPlantGui(this);
    }

    public void requestStart() {
        var tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide() || !mMachine || temperature >= WARNING_K) return;
        thermalTrip = false;
        tile.enableWorking();
    }

    public void requestStop() {
        var tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()) return;
        tile.disableWorking();
        ignited = false;
    }

    public void setTargetPower(int value) {
        var tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) targetPower = Math.max(0, Math.min(100, value));
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setDouble("fusionTemperature", temperature);
        tag.setDouble("fusionDecayHeat", decayHeat);
        tag.setLong("fusionIgnitionEnergy", ignitionEnergy);
        tag.setLong("fusionOutputEnergy", outputEnergy);
        tag.setInteger("fusionFuelCredit", fuelCredit);
        tag.setInteger("fusionTargetPower", targetPower);
        tag.setBoolean("fusionIgnited", ignited);
        tag.setBoolean("fusionThermalTrip", thermalTrip);
        if (ownerId != null) tag.setString("fusionOwner", ownerId.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        temperature = finiteRange(tag.getDouble("fusionTemperature"), AMBIENT_K, EXPLOSION_K);
        decayHeat = finiteRange(tag.getDouble("fusionDecayHeat"), 0, MAX_DECAY_HEAT_PER_TICK);
        ignitionEnergy = Math.max(0, Math.min(IGNITION_EU, tag.getLong("fusionIgnitionEnergy")));
        outputEnergy = Math.max(0, Math.min(OUTPUT_CAPACITY, tag.getLong("fusionOutputEnergy")));
        fuelCredit = Math.max(0, Math.min(FUEL_CREDIT_PER_BATCH, tag.getInteger("fusionFuelCredit")));
        targetPower = tag.hasKey("fusionTargetPower") ? Math.max(0, Math.min(100, tag.getInteger("fusionTargetPower")))
            : 100;
        ignited = tag.getBoolean("fusionIgnited");
        thermalTrip = tag.getBoolean("fusionThermalTrip");
        ownerId = null;
        if (tag.hasKey("fusionOwner", 8)) {
            try {
                UUID savedOwner = UUID.fromString(tag.getString("fusionOwner"));
                if (!savedOwner.equals(GTUtility.defaultUuid)) ownerId = savedOwner;
            } catch (IllegalArgumentException ignored) {}
        }
        fixAllIssues();
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getDecayHeat() {
        return decayHeat;
    }

    public long getIgnitionEnergy() {
        return ignitionEnergy;
    }

    public long getOutputEnergy() {
        return outputEnergy;
    }

    public int getTargetPower() {
        return targetPower;
    }

    public int getGeneratedEUt() {
        return generatedEUt;
    }

    public int getExportedEUt() {
        return exportedEUt;
    }

    public int getCoolantUsed() {
        return coolantUsed * 20;
    }

    public int getCoolantDemand() {
        return coolantDemand * 20;
    }

    public int getDeuteriumStored() {
        return depleteInputQuantity(Materials.Deuterium.getGas(Integer.MAX_VALUE), true);
    }

    public int getTritiumStored() {
        return depleteInputQuantity(Materials.Tritium.getGas(Integer.MAX_VALUE), true);
    }

    private static double finiteRange(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : max;
    }
}
