package com.xyp.gtneverreset.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.CircuitPatternCodec;
import com.xyp.gtneverreset.utils.machine.WildcardPatternSlotGT;
import com.xyp.gtneverreset.utils.machine.WildcardPatternSlotSuper;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.SimpleIOComponent;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.tile.misc.TileInterface;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class WildcardIntegrationChecks {

    private static MTEHatchCraftingInputME nativeHatch;
    private static SuperMTEHatchCraftingInputME superHatch;
    private static CircuitMEPatternBuffer circuitHatch;
    private static TileInterface aeInterface;

    private WildcardIntegrationChecks() {}

    public static void prepare(EntityPlayerMP player) {
        World world = player.worldObj;
        nativeHatch = (MTEHatchCraftingInputME) place(world, player, 10, ItemList.Hatch_CraftingInput_Bus_ME.get(1))
            .getMetaTileEntity();
        superHatch = (SuperMTEHatchCraftingInputME) place(
            world,
            player,
            12,
            GTNRItemList.SuperMTEHatchCraftingInputME.get(1)).getMetaTileEntity();
        circuitHatch = (CircuitMEPatternBuffer) place(world, player, 14, GTNRItemList.CircuitMEPatternBuffer.get(1))
            .getMetaTileEntity();
        nativeHatch.setConnectsToAllSides(true);
        superHatch.setConnectsToAllSides(true);
        circuitHatch.setConnectsToAllSides(true);
        IDefinitions definitions = AEApi.instance()
            .definitions();
        for (int x = 10; x <= 16; x++) {
            world.setBlock(
                x,
                10,
                1,
                definitions.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                definitions.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
        }
        world.setBlock(
            13,
            10,
            2,
            definitions.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(
            16,
            10,
            0,
            definitions.blocks()
                .iface()
                .maybeBlock()
                .get());
        aeInterface = (TileInterface) world.getTileEntity(16, 10, 0);
        aeInterface.onReady();
        nativeHatch.setInventorySlotContents(0, template(false));
        superHatch.setInventorySlotContents(0, template(false));
        circuitHatch.setInventorySlotContents(0, template(true));
        aeInterface.getInterfaceDuality()
            .getPatterns()
            .setInventorySlotContents(0, template(false));
    }

    public static void verify(EntityPlayerMP player) {
        require(
            nativeHatch.isActive() && superHatch.isActive() && circuitHatch.isActive(),
            "all wildcard hatches join the powered ME grid");
        List<ICraftingPatternDetails> ae = options(aeInterface);
        require(ae.size() == 2, "normal AE interface advertises both generated recipes");
        require(
            !id(ae.get(0)).isEmpty() && !id(ae.get(0)).equals(id(ae.get(1))),
            "expanded recipes have stable distinct identities");
        List<ICraftingPatternDetails> nativeOptions = options(nativeHatch);
        List<ICraftingPatternDetails> superOptions = options(superHatch);
        List<ICraftingPatternDetails> circuitOptions = options(circuitHatch);
        require(
            nativeOptions.size() == 2 && superOptions.size() == 2 && circuitOptions.size() == 2,
            "native, super and circuit hatches advertise all expansions");
        require(
            nativeHatch.inventories()
                .next() instanceof WildcardPatternSlotGT,
            "native GT pattern slots use wildcard buffering");
        require(
            superHatch.inventories()
                .next() instanceof WildcardPatternSlotSuper,
            "super pattern slots use wildcard buffering");
        checkNative(nativeOptions);
        checkSuper(superHatch, superOptions, false);
        checkSuper(circuitHatch, circuitOptions, true);
        System.out.println(
            "WILDCARD_INTEGRATION_QA PASS: AE/native/super/circuit expansion, native fluids, isolation and reload");
    }

    private static void checkNative(List<ICraftingPatternDetails> choices) {
        ICraftingPatternDetails first = choices.get(0), other = choices.get(1);
        require(
            nativeHatch.pushPattern(first, materials(first)),
            "native hatch accepts first expanded item/fluid batch");
        require(!nativeHatch.pushPattern(other, materials(other)), "native hatch prevents mixed generated recipes");
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> slot = nativeHatch.inventories()
            .next();
        require(
            id(slot.getPatternDetails()).equals(id(first)) && slot.getFluidInputs()[0].amount == 250,
            "native live inventory exposes active pattern and fluid quantity");
        MTEHatchCraftingInputME restored = (MTEHatchCraftingInputME) reload(
            (BaseMetaTileEntity) nativeHatch.getBaseMetaTileEntity()).getMetaTileEntity();
        MTEHatchCraftingInputME.PatternSlot<MTEHatchCraftingInputME> saved = restored.inventories()
            .next();
        require(
            id(saved.getPatternDetails()).equals(id(first)) && saved.getFluidInputs()[0].amount == 250,
            "native NBT reload retains active generated identity and fluid buffer");
        for (ItemStack stack : slot.getItemInputs()) stack.stackSize = 0;
        for (FluidStack fluid : slot.getFluidInputs()) fluid.amount = 0;
        require(nativeHatch.pushPattern(other, materials(other)), "native empty slot switches generated recipe");
    }

    private static void checkSuper(SuperMTEHatchCraftingInputME hatch, List<ICraftingPatternDetails> choices,
        boolean circuit) {
        ICraftingPatternDetails first = choices.get(0), other = choices.get(1);
        if (circuit) {
            for (IAEStack<?> input : first.getAEInputs())
                require(!CircuitPatternCodec.isCircuit(input), "circuit runtime excludes virtual circuit demand");
            require(
                !WildcardPatternGenerator.isWildcardPattern(first.getPattern()),
                "circuit runtime uses a native encoded pattern for CPU persistence");
            ICraftingPatternDetails decoded = CircuitPatternCodec.decode(
                first.getPattern(),
                hatch.getBaseMetaTileEntity()
                    .getWorld());
            require(
                decoded != null && decoded.equals(first),
                "native CPU reconstruction retains circuit recipe identity");
        }
        require(hatch.pushPattern(first, materials(first)), "super/circuit hatch accepts first expansion");
        require(!hatch.pushPattern(other, materials(other)), "super/circuit hatch prevents mixed generated recipes");
        SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> slot = hatch.inventories()
            .next();
        require(
            id(slot.getPatternDetails()).equals(id(first)) && slot.getFluidInputs()[0].amount == 250,
            "super/circuit active recipe exposes exact native fluid count");
        if (circuit) require(
            Arrays.stream(slot.getItemInputs())
                .anyMatch(
                    stack -> stack.getItem() == GTUtility.getIntegratedCircuit(2)
                        .getItem() && stack.getItemDamage() == 2),
            "circuit buffer supplies the selected virtual circuit");
        SuperMTEHatchCraftingInputME restored = (SuperMTEHatchCraftingInputME) reload(
            (BaseMetaTileEntity) hatch.getBaseMetaTileEntity()).getMetaTileEntity();
        SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME> saved = restored.inventories()
            .next();
        require(
            id(saved.getPatternDetails()).equals(id(first)) && saved.getFluidInputs()[0].amount == 250,
            "super/circuit NBT reload retains generated identity and item/fluid buffer");
        for (ItemStack stack : slot.getItemInputs()) stack.stackSize = 0;
        for (FluidStack fluid : slot.getFluidInputs()) fluid.amount = 0;
        require(hatch.pushPattern(other, materials(other)), "super/circuit empty slot switches generated recipe");
    }

    private static ItemStack template(boolean circuit) {
        ItemStack pattern = GTNRItemList.WildcardPattern.get(1);
        WildcardModelState.ensureInitialized(pattern);
        List<IWildcardIOComponent> inputs = new ArrayList<>();
        inputs.add(new PrefixIOComponent(OrePrefixes.ingot, 3));
        inputs.add(new SimpleIOComponent(ItemFluidDrop.newStack(new FluidStack(FluidRegistry.WATER, 250)), 250));
        if (circuit) inputs.add(new SimpleIOComponent(GTUtility.getIntegratedCircuit(2), 1));
        WildcardModelState.setInputs(pattern, inputs);
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new PrefixIOComponent(OrePrefixes.plate, 2)));
        WildcardModelState
            .setFilters(pattern, Collections.singletonList(new StringFilterComponent("^(Iron|Gold)$", true)));
        return pattern;
    }

    private static List<ICraftingPatternDetails> options(ICraftingProvider provider) {
        List<ICraftingPatternDetails> result = new ArrayList<>();
        provider.provideCrafting(new ICraftingProviderHelper() {

            @Override
            public void addCraftingOption(ICraftingMedium medium, ICraftingPatternDetails details) {
                result.add(details);
            }

            @Override
            public void setEmitable(IAEItemStack stack) {}

            @Override
            public void setEmitable(IAEStack<?> stack) {}
        });
        return result;
    }

    private static MEInventoryCrafting materials(ICraftingPatternDetails details) {
        IAEStack<?>[] inputs = details.getAEInputs();
        MEInventoryCrafting inventory = new MEInventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, inputs.length, 1);
        for (int i = 0; i < inputs.length; i++)
            if (inputs[i] != null) inventory.setInventorySlotContents(i, inputs[i].copy());
        return inventory;
    }

    private static String id(ICraftingPatternDetails details) {
        return details == null ? "" : WildcardPatternGenerator.getGeneratedPatternId(details.getPattern());
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, ItemStack stack) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static BaseMetaTileEntity reload(BaseMetaTileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        BaseMetaTileEntity restored = new BaseMetaTileEntity();
        restored.setWorldObj(tile.getWorld());
        restored.readFromNBT(tag);
        return restored;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
