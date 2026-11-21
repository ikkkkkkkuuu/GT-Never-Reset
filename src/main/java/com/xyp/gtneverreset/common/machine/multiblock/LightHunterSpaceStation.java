package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.client.lighthunter.LightHunterSpaceStationRenderer;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.WirelessNetworkManager;

@IMetaTileEntity.SkipGenerateName
public final class LightHunterSpaceStation extends MTEExtendedPowerMultiBlockBase<LightHunterSpaceStation>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    public static final long MAINTENANCE_EUT = GTValues.V[VoltageIndex.UXV];
    private static IStructureDefinition<LightHunterSpaceStation> definition;
    private static final IIconContainer FRONT = Textures.BlockIcons
        .custom(GTNeverReset.MODID, "lighthunter/station_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, "lighthunter/station_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(GTNeverReset.MODID, "lighthunter/station_front_active_glow");
    private static final IIconContainer FRONT_IDLE_GLOW = Textures.BlockIcons
        .customOptional(GTNeverReset.MODID, "lighthunter/station_front_glow");

    private final EnumMap<MachineModuleType, ParameterModuleHatch> parameterHatches = new EnumMap<>(
        MachineModuleType.class);
    private long lastPowerTick = Long.MIN_VALUE;
    private boolean powered;

    public LightHunterSpaceStation(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LightHunterSpaceStation(String name) {
        super(name);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtnr.machine.light_hunter.name
        // # Light Hunter Space Station
        // # zh_CN 猎光号空间站
        return "gtnr.machine.light_hunter.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LightHunterSpaceStation(mName);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<LightHunterSpaceStation> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<LightHunterSpaceStation>builder()
                .addShape("main", transpose(MultiblockStructureLoader.load("light_hunter_space_station")));
            for (char symbol : "=ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcd".toCharArray()) {
                IStructureElement<LightHunterSpaceStation> element = LightHunterStructure.element(symbol);
                if (symbol == 'd') {
                    element = ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(
                                LightHunterSpaceStation::addParameterHatch,
                                Casings.HighPowerCasing.getTextureId(),
                                1),
                            ParameterModuleHatch.NAME_KEY),
                        element);
                }
                builder.addElement(
                    symbol,
                    "MaUWZ".indexOf(symbol) >= 0 ? GTStructureUtility.activeCoils(element) : element);
            }
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 36, 36, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 36, 36, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece("main", 36, 36, 0, errors)) return;
        fixAllIssues();
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 1;
    }

    private boolean addParameterHatch(IGregTechTileEntity tile, int casingIndex) {
        if (!(tile.getMetaTileEntity() instanceof ParameterModuleHatch hatch)) return false;
        ParameterModuleHatch previous = parameterHatches.putIfAbsent(hatch.getModuleType(), hatch);
        if (previous != null && previous != hatch) return false;
        hatch.updateTexture(casingIndex);
        hatch.updateCraftingIcon(getMachineCraftingIcon(), getBaseMetaTileEntity());
        return true;
    }

    ParameterModuleHatch getSharedParameterHatch(MachineModuleType type) {
        ParameterModuleHatch hatch = parameterHatches.get(type);
        return mMachine && hatch != null && hatch.isValid() ? hatch : null;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ParameterModuleHatch hatch : parameterHatches.values()) {
            hatch.setWorking(false);
            hatch.updateCraftingIcon(null, getBaseMetaTileEntity());
        }
        parameterHatches.clear();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        boolean online = ensurePower();
        mMaxProgresstime = online ? 20 : 0;
        mProgresstime = online ? (int) (tick % 20) : 0;
        lEUt = online ? -MAINTENANCE_EUT : 0;
        mEfficiency = online ? 10000 : 0;
        for (ParameterModuleHatch hatch : parameterHatches.values()) hatch.setWorking(online);
    }

    private boolean ensurePower() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()
            || tile.isDead()
            || !mMachine
            || mStartUpCheck >= 0
            || !tile.isAllowedToWork()) return false;
        long tick = tile.getWorld()
            .getTotalWorldTime();
        if (lastPowerTick != tick) {
            lastPowerTick = tick;
            powered = withdraw(BigInteger.valueOf(MAINTENANCE_EUT));
        }
        return powered;
    }

    BigInteger availableSatellitePower() {
        var tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide() || !mMachine || mStartUpCheck >= 0 || !tile.isAllowedToWork())
            return BigInteger.ZERO;
        var owner = tile.getOwnerUuid();
        if (owner == null) return BigInteger.ZERO;
        var balance = WirelessNetworkManager.getUserEU(owner);
        if (lastPowerTick != tile.getWorld()
            .getTotalWorldTime()) balance = balance.subtract(BigInteger.valueOf(MAINTENANCE_EUT));
        else if (!powered) return BigInteger.ZERO;
        return balance.max(BigInteger.ZERO);
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    boolean supply(BigInteger eut) {
        return eut.signum() >= 0 && ensurePower() && (eut.signum() == 0 || withdraw(eut));
    }

    private boolean withdraw(BigInteger eut) {
        UUID owner = getBaseMetaTileEntity().getOwnerUuid();
        return owner != null && WirelessNetworkManager.addEUToGlobalEnergyMap(owner, eut.negate());
    }

    public static int[] moduleOffset(ForgeDirection facing, int distance, int side) {
        return switch (facing) {
            case EAST -> new int[] { -distance, 0, side };
            case SOUTH -> new int[] { -side, 0, -distance };
            case WEST -> new int[] { distance, 0, -side };
            default -> new int[] { side, 0, distance };
        };
    }

    boolean acceptsModule(NexusSatelliteFactory module) {
        var station = getBaseMetaTileEntity();
        var satellite = module.getBaseMetaTileEntity();
        if (!mMachine || station == null
            || station.isDead()
            || satellite == null
            || station.getWorld() != satellite.getWorld()
            || station.getYCoord() != satellite.getYCoord()
            || !station.getWorld()
                .blockExists(station.getXCoord(), station.getYCoord(), station.getZCoord())
            || station.getWorld()
                .getTileEntity(station.getXCoord(), station.getYCoord(), station.getZCoord()) != station)
            return false;
        int dx = satellite.getXCoord() - station.getXCoord();
        int dz = satellite.getZCoord() - station.getZCoord();
        ForgeDirection facing = station.getFrontFacing();
        int distance = -(dx * facing.offsetX + dz * facing.offsetZ);
        int side = facing.offsetX == 0 ? dx : dz;
        ForgeDirection inward = satellite.getFrontFacing();
        return distance >= 41 && distance <= 113
            && (distance - 41) % 8 == 0
            && Math.abs(side) == 9
            && (facing.offsetX == 0 ? inward.offsetX : inward.offsetZ) == -Integer.signum(side);
    }

    @Override
    public boolean needsClientTick() {
        return true;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isClientSide()) registerClientStation();
    }

    @SideOnly(Side.CLIENT)
    private void registerClientStation() {
        LightHunterSpaceStationRenderer.track(this);
    }

    @Override
    public void onRemoval() {
        stopEffect();
        super.onRemoval();
    }

    @Override
    public void onUnload() {
        stopEffect();
        super.onUnload();
    }

    private void stopEffect() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) stopClientEffect();
    }

    @SideOnly(Side.CLIENT)
    private void stopClientEffect() {
        LightHunterSpaceStationRenderer.stop(this);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casings.HighPowerCasing.getTextureId());
        return side == facing ? new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? FRONT_ACTIVE : FRONT)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(active ? FRONT_GLOW : FRONT_IDLE_GLOW)
                .extFacing()
                .glow()
                .build() }
            : new ITexture[] { casing };
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public String getTooltipPath() {
        return "light-hunter-station";
    }
}
