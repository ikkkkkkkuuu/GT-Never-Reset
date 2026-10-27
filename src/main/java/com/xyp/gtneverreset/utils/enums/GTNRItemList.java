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

    WirelessDualInterfaceTerminal,
    WirelessFrequencyCard,
    WildcardPattern,
    SuperMTEHatchCraftingInputME,
    CircuitMEPatternBuffer,
    SuperMTEHatchCraftingInputSlave,
    CircuitMEPatternMirror,
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
    LargeOreProcessor,
    LowSpeedPerfectOverclockController,
    PerfectOverclockController,
    SingularityPerfectOverclockController,
    LargeFurnace,
    QuantumComputer,
    AssemblerMatrix,
    SingularityDataHub,
    VaultPortHatch,
    FragmentWorldCollector,
    LargeBeeBreeder,
    StableEndCrystal,
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
    EddingtonWallCasing,
    AdvancedIOBus,
    StockIOInterface,
    StockIOInterfacePart,
    FluxPlug,
    FluxPoint;

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
