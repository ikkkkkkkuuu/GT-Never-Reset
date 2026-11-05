package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import gregtech.api.util.GTUtility;
import gregtech.common.items.ItemIntegratedCircuit;

/** Stores virtual tools separately from AE demands while accepting legacy patterns with tools in their inputs. */
public final class CircuitPatternCodec {

    public static final String CIRCUIT = "gtnrPatternCircuit";
    public static final String MOLDS = "gtnrPatternMolds";

    public static ICraftingPatternDetails decode(ItemStack pattern, World world) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem item)) return null;
        try {
            return item.getPatternForItem(pattern, world);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public static boolean isCircuit(IAEStack<?> stack) {
        return stack instanceof IAEItemStack item && item.getItem() instanceof ItemIntegratedCircuit;
    }

    public static boolean isMold(IAEStack<?> stack, ItemStack[] molds) {
        if (!(stack instanceof IAEItemStack item)) return false;
        for (ItemStack mold : molds) if (GTUtility.areStacksEqual(mold, item.getItemStack())) return true;
        return false;
    }

    /**
     * Reads saved tool metadata, or resolves legacy catalog molds from matching GT recipes.
     *
     * @param details processing pattern with stored tool metadata or legacy tool inputs
     * @return distinct zero-size mold templates; genuine consumable molds remain AE inputs
     */
    public static ItemStack[] molds(ICraftingPatternDetails details) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return new ItemStack[0];
        ItemStack pattern = details.getPattern();
        if (pattern != null && pattern.hasTagCompound()
            && pattern.getTagCompound()
                .hasKey(MOLDS)) {
            ItemStack[] stored = storedMolds(pattern);
            return stored == null ? new ItemStack[0] : stored;
        }
        return CircuitPatternMolds.resolve(details);
    }

    /** @return -1 for no circuit, -2 for conflicting or unsupported configurations. */
    public static int circuit(ICraftingPatternDetails details) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return -1;
        int stored = storedCircuit(details.getPattern());
        int input = inputCircuit(details);
        if (stored == -2 || input == -2 || stored >= 0 && input >= 0 && stored != input) return -2;
        return stored >= 0 ? stored : input;
    }

    private static int inputCircuit(ICraftingPatternDetails details) {
        int result = -1;
        if (details == null || details.isCraftable()) return result;
        for (IAEStack<?> stack : details.getAEInputs()) {
            if (!isCircuit(stack)) continue;
            int config = ((IAEItemStack) stack).getItemStack()
                .getItemDamage();
            if (config < 0 || config > 24 || result >= 0 && result != config) return -2;
            result = config;
        }
        return result;
    }

    /** Reads only saved configuration, without a world or a recipe-registry scan. */
    public static int storedCircuit(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) return -1;
        NBTTagCompound tag = pattern.getTagCompound();
        if (!tag.hasKey(CIRCUIT)) return -1;
        if (!tag.hasKey(CIRCUIT, Constants.NBT.TAG_INT)) return -2;
        int config = tag.getInteger(CIRCUIT);
        return config >= 0 && config <= 24 ? config : -2;
    }

    /** Null denotes malformed or unsupported stored tools; valid templates always have zero quantity. */
    public static ItemStack[] storedMolds(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()
            || !pattern.getTagCompound()
                .hasKey(MOLDS))
            return new ItemStack[0];
        NBTTagCompound tag = pattern.getTagCompound();
        if (!tag.hasKey(MOLDS, Constants.NBT.TAG_LIST)) return null;
        NBTTagList encoded = tag.getTagList(MOLDS, Constants.NBT.TAG_COMPOUND);
        if (encoded.tagCount() == 0 || encoded.tagCount() > MoldDataManager.getMolds().length) return null;
        List<ItemStack> molds = new ArrayList<>();
        for (int i = 0; i < encoded.tagCount(); i++) {
            ItemStack mold = ItemStack.loadItemStackFromNBT(encoded.getCompoundTagAt(i));
            if (mold == null || mold.stackSize != 0) return null;
            boolean supported = false;
            for (ItemStack candidate : MoldDataManager.getMolds()) {
                if (GTUtility.areStacksEqual(candidate, mold, false)) {
                    supported = true;
                    break;
                }
            }
            if (!supported || molds.stream()
                .anyMatch(previous -> GTUtility.areStacksEqual(previous, mold, false))) return null;
            molds.add(mold);
        }
        return molds.toArray(new ItemStack[0]);
    }

    /** Restores editable tool entries on a copy; the encoded pattern keeps only its consumable input list. */
    public static ItemStack editablePattern(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()
            || pattern.getTagCompound()
                .getBoolean("crafting"))
            return pattern;
        int config = storedCircuit(pattern);
        ItemStack[] molds = storedMolds(pattern);
        if (config == -2 || molds == null || config < 0 && molds.length == 0) return pattern;
        ItemStack editable = pattern.copy();
        NBTTagCompound tag = editable.getTagCompound();
        NBTTagList inputs = tag.getTagList("in", Constants.NBT.TAG_COMPOUND);
        if (config >= 0) appendTool(inputs, GTUtility.getIntegratedCircuit(config));
        for (ItemStack mold : molds) appendTool(inputs, mold);
        tag.removeTag(CIRCUIT);
        tag.removeTag(MOLDS);
        return editable;
    }

    private static void appendTool(NBTTagList inputs, ItemStack tool) {
        for (int i = 0; i < inputs.tagCount(); i++) {
            IAEStack<?> stack = Platform.readStackNBT(inputs.getCompoundTagAt(i), true);
            if (stack instanceof IAEItemStack item && GTUtility.areStacksEqual(tool, item.getItemStack(), false))
                return;
        }
        ItemStack copy = tool.copy();
        copy.stackSize = 1;
        NBTTagCompound encoded = new NBTTagCompound();
        AEItemStack.create(copy)
            .writeToNBTGeneric(encoded);
        inputs.appendTag(encoded);
    }

    /**
     * Removes virtual circuits and molds from AE demands and records them in the native pattern identity.
     * The native pattern item can reconstruct the same details after an AE crafting CPU save/reload.
     */
    public static ICraftingPatternDetails runtime(ItemStack pattern, World world) {
        ICraftingPatternDetails original = decode(pattern, world);
        return runtime(pattern, world, original, molds(original));
    }

    public static ICraftingPatternDetails runtime(ItemStack pattern, World world, ICraftingPatternDetails original,
        ItemStack[] molds) {
        if (original == null || original.isCraftable() || original.isInputOnly()) return null;
        int config = circuit(original);
        if (config == -2 || storedMolds(pattern) == null) return null;
        boolean generated = WildcardPatternGenerator.isGeneratedPattern(pattern)
            && WildcardPatternGenerator.isWildcardPattern(pattern);
        if (!generated && config < 0 && molds.length == 0) return original;
        ItemStack runtime = generated ? nativePattern(pattern, original, molds) : withoutVirtualItems(pattern, molds);
        if (runtime == null) return null;
        NBTTagCompound tag = runtime.getTagCompound();
        tag.removeTag("InvalidPattern");
        tag.removeTag(CIRCUIT);
        tag.removeTag(MOLDS);
        if (config >= 0) tag.setInteger(CIRCUIT, config);
        if (molds.length > 0) {
            NBTTagList tools = new NBTTagList();
            for (ItemStack mold : molds) tools.appendTag(mold.writeToNBT(new NBTTagCompound()));
            tag.setTag(MOLDS, tools);
        }
        ICraftingPatternDetails decoded = decode(runtime, world);
        if (decoded == null || decoded.getCondensedAEInputs().length == 0 || inputCircuit(decoded) != -1) return null;
        return decoded;
    }

    private static ItemStack nativePattern(ItemStack pattern, ICraftingPatternDetails original, ItemStack[] molds) {
        if (pattern == null || !pattern.hasTagCompound()) return null;
        IAEStack<?>[] inputs = original.getAEInputs(), outputs = original.getAEOutputs();
        boolean ultimate = hasFluids(inputs) || hasFluids(outputs);
        ItemStack result = (ultimate ? AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            : AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()).maybeStack(1)
                    .orNull();
        if (result == null) return null;
        NBTTagCompound tag = (NBTTagCompound) pattern.getTagCompound()
            .copy();
        tag.setTag("in", encodeStacks(inputs, molds, ultimate));
        tag.setTag("out", encodeStacks(outputs, null, ultimate));
        tag.setBoolean("crafting", false);
        tag.setBoolean("substitute", original.canSubstitute());
        tag.setBoolean("beSubstitute", original.canBeSubstitute());
        tag.removeTag("WildcardPattern");
        tag.removeTag(WildcardModelState.KEY_EXPANDED_COUNT);
        tag.removeTag("WildcardExpandedPatternCount");
        NBTTagCompound source = new NBTTagCompound();
        source.setString("id", Item.itemRegistry.getNameForObject(pattern.getItem()));
        source.setInteger("Damage", pattern.getItemDamage());
        tag.setTag("gtngWildcardSource", source);
        result.setTagCompound(tag);
        return result;
    }

    private static boolean hasFluids(IAEStack<?>[] stacks) {
        for (IAEStack<?> stack : stacks) if (stack instanceof IAEFluidStack) return true;
        return false;
    }

    private static NBTTagList encodeStacks(IAEStack<?>[] stacks, ItemStack[] molds, boolean ultimate) {
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> stack : stacks) {
            if (stack == null || molds != null && (isCircuit(stack) || isMold(stack, molds))) continue;
            NBTTagCompound entry = new NBTTagCompound();
            if (ultimate) stack.writeToNBTGeneric(entry);
            else stack.writeToNBT(entry);
            list.appendTag(entry);
        }
        return list;
    }

    private static ItemStack withoutVirtualItems(ItemStack pattern, ItemStack[] molds) {
        if (pattern == null || !pattern.hasTagCompound()) return null;
        ItemStack edited = pattern.copy();
        NBTTagCompound tag = edited.getTagCompound();
        NBTTagList original = tag.getTagList("in", Constants.NBT.TAG_COMPOUND);
        NBTTagList inputs = new NBTTagList();
        for (int i = 0; i < original.tagCount(); i++) {
            NBTTagCompound entry = original.getCompoundTagAt(i);
            IAEStack<?> stack = Platform.readStackNBT(entry, true);
            if (stack == null) continue;
            if (!isCircuit(stack) && !isMold(stack, molds)) inputs.appendTag(entry.copy());
        }
        if (inputs.tagCount() == 0) return null;
        tag.setTag("in", inputs);
        return edited;
    }
}
