package com.xyp.gtneverreset.common.me.wildcard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.PatternHelper;
import appeng.util.item.AEFluidStack;
import gregtech.api.util.GTUtility;

public class WildcardPatternDetails implements ICraftingPatternDetails {

    private final PatternHelper delegate;

    // Inputs and outputs stay fixed while AE2 repeatedly registers this slot's details.
    private IAEStack<?>[] cachedAeInputs;
    private IAEStack<?>[] cachedCondensedAeInputs;
    private IAEStack<?>[] cachedAeOutputs;
    private IAEStack<?>[] cachedCondensedAeOutputs;
    private int cachedHashCode;
    private boolean hashCodeComputed;

    public WildcardPatternDetails(ItemStack stack, World world) {
        this.delegate = new PatternHelper(stack, world);
    }

    @Override
    public ItemStack getPattern() {
        return this.delegate.getPattern();
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        return this.delegate.isValidItemForSlot(slotIndex, itemStack, world);
    }

    @Override
    public boolean isCraftable() {
        return this.delegate.isCraftable();
    }

    @Override
    public IAEItemStack[] getInputs() {
        return this.delegate.getInputs();
    }

    /**
     * AE2FC方法:返回混合的IAEItemStack和IAEFluidStack数组
     * 新版GTNH的合成CPU和PatternSlot都通过此方法识别流体请求
     */
    @Override
    public IAEStack<?>[] getAEInputs() {
        if (this.cachedAeInputs == null) {
            this.cachedAeInputs = convertToAeStacks(this.delegate.getInputs());
        }
        return this.cachedAeInputs;
    }

    @Override
    public IAEStack<?>[] getCondensedAEInputs() {
        if (this.cachedCondensedAeInputs == null) {
            this.cachedCondensedAeInputs = convertToAeStacks(this.delegate.getCondensedInputs());
        }
        return this.cachedCondensedAeInputs;
    }

    @Override
    public IAEStack<?>[] getAEOutputs() {
        if (this.cachedAeOutputs == null) {
            this.cachedAeOutputs = convertToAeStacks(this.delegate.getOutputs());
        }
        return this.cachedAeOutputs;
    }

    @Override
    public IAEStack<?>[] getCondensedAEOutputs() {
        if (this.cachedCondensedAeOutputs == null) {
            this.cachedCondensedAeOutputs = convertToAeStacks(this.delegate.getCondensedOutputs());
        }
        return this.cachedCondensedAeOutputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.delegate.getCondensedInputs();
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return this.delegate.getCondensedOutputs();
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return this.delegate.getOutputs();
    }

    @Override
    public boolean canSubstitute() {
        return this.delegate.canSubstitute();
    }

    @Override
    public boolean canBeSubstitute() {
        return this.delegate.canBeSubstitute();
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        return this.delegate.getOutput(craftingInv, world);
    }

    @Override
    public int getPriority() {
        return this.delegate.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        this.delegate.setPriority(priority);
    }

    @Override
    public int hashCode() {
        if (!this.hashCodeComputed) {
            ItemStack pattern = this.getPattern();
            int result = pattern.getItem() != null ? System.identityHashCode(pattern.getItem()) : 0;
            result = 31 * result + pattern.getItemDamage();
            result = 31 * result + WildcardPatternGenerator.getPatternIdentity(pattern)
                .hashCode();
            this.cachedHashCode = result;
            this.hashCodeComputed = true;
        }
        return this.cachedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WildcardPatternDetails other)) {
            return false;
        }
        return arePatternStacksEqual(this.getPattern(), other.getPattern());
    }

    private IAEStack<?>[] convertToAeStacks(IAEItemStack[] items) {
        if (items == null || items.length == 0) {
            return new IAEStack<?>[0];
        }
        // 注意：绝不能在结果里保留 null 元素。AE2 的 CraftingGridCache.setPatternsFromCraftingMethods
        // 遍历 getAEOutputs() 时直接 out.copy() 不判空，若数组含 null 会每 tick NPE 刷屏、样板注册失败。
        // 输入的消费方也一律跳过 null，故这里统一跳过 null 并压缩数组。
        List<IAEStack<?>> result = new ArrayList<>(items.length);
        for (IAEItemStack ais : items) {
            if (ais == null) {
                continue;
            }
            ItemStack stack = ais.getItemStack();
            // 检测是否为流体输入(GT5 ItemFluidDisplay NEI拖入 或 AE2FC ItemFluidDrop 样板编码)
            FluidStack fluidStack = null;
            if (stack != null) {
                fluidStack = GTUtility.getFluidFromDisplayStack(stack);
                if (fluidStack == null) {
                    fluidStack = ItemFluidDrop.getFluidStack(stack);
                }
            }
            if (fluidStack != null) {
                result.add(AEFluidStack.create(fluidStack));
            } else {
                result.add(ais);
            }
        }
        return result.toArray(new IAEStack<?>[0]);
    }

    private static boolean arePatternStacksEqual(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getItem() != right.getItem() || left.getItemDamage() != right.getItemDamage()) {
            return false;
        }
        String leftId = WildcardPatternGenerator.getGeneratedPatternId(left);
        String rightId = WildcardPatternGenerator.getGeneratedPatternId(right);
        if (!leftId.isEmpty() || !rightId.isEmpty()) {
            return leftId.equals(rightId);
        }
        NBTTagCompound leftTag = left.getTagCompound();
        NBTTagCompound rightTag = right.getTagCompound();
        if (leftTag == rightTag) {
            return true;
        }
        if (leftTag == null || rightTag == null) {
            return false;
        }
        return leftTag.equals(rightTag);
    }
}
