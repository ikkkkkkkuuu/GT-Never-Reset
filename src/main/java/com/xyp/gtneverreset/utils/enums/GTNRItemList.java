package com.xyp.gtneverreset.utils.enums;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.IItemContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public enum GTNRItemList implements IItemContainer {

    VeinMiningPickaxe,

    // region 物品
    ItemWirelessConnector,
    BasicPackagedCore,
    BloodAltarCore,
    AssemblyLineCore,
    AdvancedAssemblyLineCore,
    CircuitAssemblyLineCore,
    ThaumcraftCrucibleCore,
    ArcaneWorkbenchCore,
    ThaumcraftInfusionCore,

    WirelessDualInterfaceTerminal,
    WirelessFrequencyCard,
    WildcardPattern,
    StableEndCrystal,

    AdvancedIOBus,
    StockIOInterfacePart,
    // endregion

    // region 方块
    EddingtonWallCasing,
    WirelessPackagedPatternProvider,
    StockIOInterface,
    FluxPlug,
    FluxPoint,
    // endregion

    // region 多方块
    LargeOreProcessor,
    LargeFurnace,
    QuantumComputer,
    AssemblerMatrix,
    SingularityDataHub,
    LargeBeeBreeder,
    // endregion

    // region 舱室
    SuperMTEHatchCraftingInputME,
    CircuitMEPatternBuffer,
    SuperMTEHatchCraftingInputSlave,
    CircuitMEPatternMirror,
    MaxCapacityMEOutputBus,
    SuperAdvancedMEInputBus,
    SuperAdvancedMEInputHatch,
    MaxCapacityMEOutputHatch,
    MEOutputAssembly,
    VaultPortHatch,
    MEDataAccessHatch,

    StaticParallelControllerT1,
    StaticParallelControllerT2,
    StaticParallelControllerT3,
    StaticParallelControllerT4,
    StaticParallelControllerT5,
    StaticParallelControllerT6,
    StaticParallelControllerT7,
    StaticParallelControllerT8,

    DynamicParallelControllerT1,
    DynamicParallelControllerT2,
    DynamicParallelControllerT3,
    DynamicParallelControllerT4,
    DynamicParallelControllerT5,
    DynamicParallelControllerT6,
    DynamicParallelControllerT7,
    DynamicParallelControllerT8,

    StaticSpeedControllerT1,
    StaticSpeedControllerT2,
    StaticSpeedControllerT3,
    StaticSpeedControllerT4,
    StaticSpeedControllerT5,
    StaticSpeedControllerT6,
    StaticSpeedControllerT7,
    StaticSpeedControllerT8,

    DynamicSpeedControllerT1,
    DynamicSpeedControllerT2,
    DynamicSpeedControllerT3,
    DynamicSpeedControllerT4,
    DynamicSpeedControllerT5,
    DynamicSpeedControllerT6,
    DynamicSpeedControllerT7,
    DynamicSpeedControllerT8,

    PowerConsumptionControllerT1,
    PowerConsumptionControllerT2,
    PowerConsumptionControllerT3,
    PowerConsumptionControllerT4,
    PowerConsumptionControllerT5,
    PowerConsumptionControllerT6,
    PowerConsumptionControllerT7,
    PowerConsumptionControllerT8,

    LowSpeedPerfectOverclockController,
    PerfectOverclockController,
    SingularityPerfectOverclockController,
    // endregion

    // region 单方块机器
    FragmentWorldCollector,

    EndCrystalResonatorLV,
    EndCrystalResonatorMV,
    EndCrystalResonatorHV,
    EndCrystalResonatorEV,
    EndCrystalResonatorIV,
    EndCrystalResonatorLuV,
    EndCrystalResonatorZPM,
    EndCrystalResonatorUV,
    EndCrystalResonatorUHV,
    EndCrystalResonatorUEV,
    EndCrystalResonatorUIV,
    EndCrystalResonatorUMV,
    EndCrystalResonatorUXV,
    EndCrystalResonatorMAX,

    ModernMixerLV,
    ModernMixerMV,
    ModernMixerHV,
    ModernMixerEV,
    ModernMixerIV,
    ModernMixerLuV,
    ModernMixerZPM,
    ModernMixerUV,

    ModernElectrolyzerLV,
    ModernElectrolyzerMV,
    ModernElectrolyzerHV,
    ModernElectrolyzerEV,
    ModernElectrolyzerIV,
    ModernElectrolyzerLuV,
    ModernElectrolyzerZPM,
    ModernElectrolyzerUV,

    ModernChemicalReactorLV,
    ModernChemicalReactorMV,
    ModernChemicalReactorHV,
    ModernChemicalReactorEV,
    ModernChemicalReactorIV,
    ModernChemicalReactorLuV,
    ModernChemicalReactorZPM,
    ModernChemicalReactorUV,

    ModernCentrifugeLV,
    ModernCentrifugeMV,
    ModernCentrifugeHV,
    ModernCentrifugeEV,
    ModernCentrifugeIV,
    ModernCentrifugeLuV,
    ModernCentrifugeZPM,
    ModernCentrifugeUV;
    // endregion

    // region 舱室分组
    public static final GTNRItemList[] STATIC_PARALLEL = { GTNRItemList.StaticParallelControllerT1,
        GTNRItemList.StaticParallelControllerT2, GTNRItemList.StaticParallelControllerT3,
        GTNRItemList.StaticParallelControllerT4, GTNRItemList.StaticParallelControllerT5,
        GTNRItemList.StaticParallelControllerT6, GTNRItemList.StaticParallelControllerT7,
        GTNRItemList.StaticParallelControllerT8 };

    public static final GTNRItemList[] DYNAMIC_PARALLEL = { GTNRItemList.DynamicParallelControllerT1,
        GTNRItemList.DynamicParallelControllerT2, GTNRItemList.DynamicParallelControllerT3,
        GTNRItemList.DynamicParallelControllerT4, GTNRItemList.DynamicParallelControllerT5,
        GTNRItemList.DynamicParallelControllerT6, GTNRItemList.DynamicParallelControllerT7,
        GTNRItemList.DynamicParallelControllerT8 };

    public static final GTNRItemList[] STATIC_SPEED = { GTNRItemList.StaticSpeedControllerT1,
        GTNRItemList.StaticSpeedControllerT2, GTNRItemList.StaticSpeedControllerT3,
        GTNRItemList.StaticSpeedControllerT4, GTNRItemList.StaticSpeedControllerT5,
        GTNRItemList.StaticSpeedControllerT6, GTNRItemList.StaticSpeedControllerT7,
        GTNRItemList.StaticSpeedControllerT8 };

    public static final GTNRItemList[] DYNAMIC_SPEED = { GTNRItemList.DynamicSpeedControllerT1,
        GTNRItemList.DynamicSpeedControllerT2, GTNRItemList.DynamicSpeedControllerT3,
        GTNRItemList.DynamicSpeedControllerT4, GTNRItemList.DynamicSpeedControllerT5,
        GTNRItemList.DynamicSpeedControllerT6, GTNRItemList.DynamicSpeedControllerT7,
        GTNRItemList.DynamicSpeedControllerT8 };

    public static final GTNRItemList[] POWER = { GTNRItemList.PowerConsumptionControllerT1,
        GTNRItemList.PowerConsumptionControllerT2, GTNRItemList.PowerConsumptionControllerT3,
        GTNRItemList.PowerConsumptionControllerT4, GTNRItemList.PowerConsumptionControllerT5,
        GTNRItemList.PowerConsumptionControllerT6, GTNRItemList.PowerConsumptionControllerT7,
        GTNRItemList.PowerConsumptionControllerT8 };

    public static final GTNRItemList[] OVERCLOCK = { GTNRItemList.LowSpeedPerfectOverclockController,
        GTNRItemList.PerfectOverclockController, GTNRItemList.SingularityPerfectOverclockController };

    // endregion

    // region 单方块机器分组
    public static final GTNRItemList[] MODERN_MIXERS = { null, GTNRItemList.ModernMixerLV, GTNRItemList.ModernMixerMV,
        GTNRItemList.ModernMixerHV, GTNRItemList.ModernMixerEV, GTNRItemList.ModernMixerIV, GTNRItemList.ModernMixerLuV,
        GTNRItemList.ModernMixerZPM, GTNRItemList.ModernMixerUV };

    public static final GTNRItemList[] MODERN_ELECTROLYZERS = { null, GTNRItemList.ModernElectrolyzerLV,
        GTNRItemList.ModernElectrolyzerMV, GTNRItemList.ModernElectrolyzerHV, GTNRItemList.ModernElectrolyzerEV,
        GTNRItemList.ModernElectrolyzerIV, GTNRItemList.ModernElectrolyzerLuV, GTNRItemList.ModernElectrolyzerZPM,
        GTNRItemList.ModernElectrolyzerUV };

    public static final GTNRItemList[] MODERN_CHEMICAL_REACTORS = { null, GTNRItemList.ModernChemicalReactorLV,
        GTNRItemList.ModernChemicalReactorMV, GTNRItemList.ModernChemicalReactorHV,
        GTNRItemList.ModernChemicalReactorEV, GTNRItemList.ModernChemicalReactorIV,
        GTNRItemList.ModernChemicalReactorLuV, GTNRItemList.ModernChemicalReactorZPM,
        GTNRItemList.ModernChemicalReactorUV };

    public static final GTNRItemList[] MODERN_CENTRIFUGES = { null, GTNRItemList.ModernCentrifugeLV,
        GTNRItemList.ModernCentrifugeMV, GTNRItemList.ModernCentrifugeHV, GTNRItemList.ModernCentrifugeEV,
        GTNRItemList.ModernCentrifugeIV, GTNRItemList.ModernCentrifugeLuV, GTNRItemList.ModernCentrifugeZPM,
        GTNRItemList.ModernCentrifugeUV };

    // endregion

    private ItemStack stack;

    @Override
    public GTNRItemList set(Item item) {
        return set(new ItemStack(item));
    }

    @Override
    public GTNRItemList set(ItemStack registeredStack) {
        if (GTUtility.isStackInvalid(registeredStack)) throw new IllegalArgumentException("invalid item for " + name());
        stack = GTUtility.copyAmountUnsafe(1, registeredStack);
        return this;
    }

    public GTNRItemList set(IMetaTileEntity machine) {
        return set(machine.getStackForm(1));
    }

    @Override
    public boolean hasBeenSet() {
        return stack != null;
    }

    @Override
    public Item getItem() {
        sanityCheck();
        return stack.getItem();
    }

    @Override
    public Block getBlock() {
        return Block.getBlockFromItem(getItem());
    }

    @Override
    public ItemStack get(long amount, Object... replacements) {
        sanityCheck();
        return GTUtility.copyAmountUnsafe(Math.toIntExact(amount), stack);
    }

    @Override
    public boolean isStackEqual(Object other) {
        return isStackEqual(other, false, false);
    }

    @Override
    public boolean isStackEqual(Object other, boolean wildcard, boolean ignoreNBT) {
        if (GTUtility.isStackInvalid(other)) return false;
        return GTUtility.areUnificationsEqual((ItemStack) other, wildcard ? getWildcard(1) : get(1), ignoreNBT);
    }

    @Override
    public ItemStack getWildcard(long amount, Object... replacements) {
        return getWithDamage(amount, GTRecipeBuilder.WILDCARD, replacements);
    }

    @Override
    public ItemStack getUndamaged(long amount, Object... replacements) {
        return getWithDamage(amount, 0, replacements);
    }

    @Override
    public ItemStack getAlmostBroken(long amount, Object... replacements) {
        sanityCheck();
        return getWithDamage(amount, stack.getMaxDamage() - 1, replacements);
    }

    @Override
    public ItemStack getWithDamage(long amount, long metadata, Object... replacements) {
        sanityCheck();
        return GTUtility.copyAmountAndMetaData(amount, metadata, stack);
    }

    @Override
    public ItemStack getWithCharge(long amount, int energy, Object... replacements) {
        ItemStack charged = get(1, replacements);
        GTModHandler.chargeElectricItem(charged, energy, Integer.MAX_VALUE, true, false);
        return GTUtility.copyAmount(amount, charged);
    }

    @Override
    public ItemStack getWithName(long amount, String displayName, Object... replacements) {
        return get(amount, replacements).setStackDisplayName(displayName);
    }

    @Override
    public GTNRItemList registerOre(Object... oreNames) {
        for (Object oreName : oreNames) GTOreDictUnificator.registerOre(oreName, get(1));
        return this;
    }

    @Override
    public GTNRItemList registerWildcardAsOre(Object... oreNames) {
        for (Object oreName : oreNames) GTOreDictUnificator.registerOre(oreName, getWildcard(1));
        return this;
    }

    private void sanityCheck() {
        if (!hasBeenSet()) throw new IllegalStateException("item has not been registered: " + name());
    }
}
