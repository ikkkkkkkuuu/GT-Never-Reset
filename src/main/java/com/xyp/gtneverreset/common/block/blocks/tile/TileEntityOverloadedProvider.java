package com.xyp.gtneverreset.common.block.blocks.tile;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration;
import com.xyp.gtneverreset.api.mixinHelper.ITerminalVisibilityToggle;
import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.common.gui.OverloadedProviderGui;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternCache;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IInterfaceViewable;
import appeng.container.ContainerNull;
import appeng.helpers.ICustomNameObject;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;

public final class TileEntityOverloadedProvider extends TileEntityAENetworkBase implements IInventory,
    ICraftingProvider, IGuiHolder<PosGuiData>, IInterfaceViewable, ITerminalVisibilityToggle, ICustomNameObject {

    public static final int PATTERNS = 450;
    public static final int MAX_TARGETS = 1024;
    public static final int RANGE = 128;
    private static final int LEGACY_PATTERNS = 36;
    private static final int WORK_PER_TICK = 8;
    private final ItemStack[] inventory = new ItemStack[PATTERNS + 9];
    private final List<Lane> lanes = new ArrayList<>();
    private final List<PackagedTarget> clientTargets = new ArrayList<>();
    private final List<PackagedTarget> visibleTargets = Collections.unmodifiableList(clientTargets);
    private final Set<ICraftingPatternDetails> patterns = new LinkedHashSet<>();
    private final Map<Integer, WildcardPatternCache<List<ProviderPattern>>> patternCaches = new HashMap<>();
    private final List<FluidStack> returnedFluids = new ArrayList<>();
    private boolean patternsDirty = true;
    private boolean autoReturn = true;
    private boolean roundRobin = true;
    private boolean patternOptimization = true;
    private boolean terminalVisible = true;
    private String customName = "";
    private int dispatchCursor;
    private int workCursor;

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNRItemList.OverloadedWirelessPatternProvider.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        AENetworkProxy proxy = super.getProxy();
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        proxy.setIdlePowerUsage(4 + lanes.size());
        return proxy;
    }

    public boolean canConfigure(EntityPlayer player) {
        return player != null && ownerName.equals(player.getCommandSenderName());
    }

    public List<Lane> connections() {
        return Collections.unmodifiableList(lanes);
    }

    public boolean hasPendingDeliveryTo(int x, int y, int z) {
        for (Lane lane : lanes) {
            if (lane.target.x == x && lane.target.y == y && lane.target.z == z && !lane.machineBuffer.isEmpty())
                return true;
        }
        return false;
    }

    public boolean bind(PackagedTarget target) {
        if (!isServerSide() || target == null
            || target.face < 0
            || target.face > 5
            || target.dimension != worldObj.provider.dimensionId
            || lanes.size() >= MAX_TARGETS
            || target.resolve(worldObj) == this
            || !(target.resolve(worldObj) instanceof IInventory)
            || distanceSquared(target) > RANGE * RANGE) return false;
        for (Lane lane : lanes) if (lane.target.sameBlock(target)) return false;
        lanes.add(new Lane(target));
        patternsDirty = true;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        markDirty();
        return true;
    }

    private double distanceSquared(PackagedTarget target) {
        double dx = (double) target.x - xCoord, dy = (double) target.y - yCoord, dz = (double) target.z - zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean unlink(PackagedTarget target) {
        if (!isServerSide()) return false;
        for (int i = 0; i < lanes.size(); i++) {
            Lane lane = lanes.get(i);
            if (lane.target.equals(target) && lane.pending.isEmpty() && lane.machineBuffer.isEmpty()) {
                lanes.remove(i);
                patternsDirty = true;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                markDirty();
                return true;
            }
        }
        return false;
    }

    public void retry(PackagedTarget target) {
        if (!isServerSide() || !getProxy().isActive()) return;
        for (Lane lane : lanes) if (lane.target.equals(target)) {
            service(lane);
            break;
        }
    }

    public List<PackagedTarget> visibleConnections() {
        return visibleTargets;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList targets = new NBTTagList();
        for (Lane lane : lanes) targets.appendTag(lane.target.write());
        tag.setTag("WirelessConnections", targets);
        tag.setString("CustomName", customName);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        if (worldObj == null || !worldObj.isRemote) return;
        customName = packet.func_148857_g()
            .getString("CustomName");
        NBTTagList targets = packet.func_148857_g()
            .getTagList("WirelessConnections", 10);
        clientTargets.clear();
        for (int i = 0; i < Math.min(targets.tagCount(), MAX_TARGETS); i++) {
            PackagedTarget target = PackagedTarget.read(targets.getCompoundTagAt(i));
            if (target != null && target.dimension == worldObj.provider.dimensionId) clientTargets.add(target);
        }
    }

    public boolean getAutoReturn() {
        return autoReturn;
    }

    @Override
    public boolean allowsPatternOptimization() {
        return patternOptimization;
    }

    public void togglePatternOptimization() {
        if (isServerSide()) {
            patternOptimization = !patternOptimization;
            markDirty();
        }
    }

    @Override
    public boolean shouldDisplay() {
        return terminalVisible;
    }

    @Override
    public void toggleTerminalVisibility() {
        if (isServerSide()) {
            terminalVisible = !terminalVisible;
            markDirty();
        }
    }

    @Override
    public int rows() {
        int lastNonNullIndex = PATTERNS - 1;
        while (lastNonNullIndex >= 0 && inventory[lastNonNullIndex] == null) lastNonNullIndex--;
        int calculatedRows = (lastNonNullIndex + 9) / 9 + 1;
        if ((lastNonNullIndex + 1) % 9 == 0) calculatedRows++;
        return Math.min(calculatedRows, PATTERNS / 9);
    }

    @Override
    public int rowSize() {
        return 9;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return getInventoryName();
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public ItemStack getSelfRep() {
        return getVisualRepresentation();
    }

    @Override
    public IAEStackType<?>[] getSupportedStackTypes() {
        return new IAEStackType<?>[] { ITEM_STACK_TYPE, FLUID_STACK_TYPE };
    }

    @Override
    public IInventory getPatterns() {
        return new InventoryBasic(getInventoryName(), hasCustomName(), PATTERNS) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < PATTERNS ? inventory[slot] : null;
            }

            @Override
            public ItemStack decrStackSize(int slot, int amount) {
                return slot >= 0 && slot < PATTERNS ? TileEntityOverloadedProvider.this.decrStackSize(slot, amount)
                    : null;
            }

            @Override
            public void setInventorySlotContents(int slot, ItemStack stack) {
                if (slot >= 0 && slot < PATTERNS && (stack == null || isItemValidForSlot(slot, stack)))
                    TileEntityOverloadedProvider.this.setInventorySlotContents(slot, stack);
            }

            @Override
            public int getInventoryStackLimit() {
                return 1;
            }

            @Override
            public boolean isItemValidForSlot(int slot, ItemStack stack) {
                return slot >= 0 && slot < PATTERNS
                    && stack != null
                    && stack.stackSize == 1
                    && stack.getItem() instanceof ICraftingPatternItem;
            }

            @Override
            public void markDirty() {
                patternsDirty = true;
                TileEntityOverloadedProvider.this.markDirty();
            }
        };
    }

    public void multiplyPatterns(boolean fast, boolean divide) {
        if (!isServerSide()) return;
        CraftingGridCache.pauseRebuilds();
        try {
            for (int slot = 0; slot < PATTERNS; slot++) {
                ItemStack stack = inventory[slot];
                if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem item)) continue;
                ICraftingPatternDetails details = item.getPatternForItem(stack, worldObj);
                if (details == null || details.isCraftable()) continue;
                int max = divide ? PatternMultiplierHelper.getMaxBitDivider(details)
                    : PatternMultiplierHelper.getMaxBitMultiplier(details);
                if (max <= 0) continue;
                ItemStack modified = stack.copy();
                PatternMultiplierHelper.applyModification(modified, (fast ? Math.min(3, max) : 1) * (divide ? -1 : 1));
                setInventorySlotContents(slot, modified);
            }
        } finally {
            CraftingGridCache.unpauseRebuilds();
        }
    }

    public void toggleAutoReturn() {
        if (isServerSide()) {
            autoReturn = !autoReturn;
            markDirty();
        }
    }

    public boolean getRoundRobin() {
        return roundRobin;
    }

    public void toggleDistribution() {
        if (isServerSide()) {
            roundRobin = !roundRobin;
            markDirty();
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServerSide()) return;
        if (patternsDirty) {
            try {
                getProxy().getGrid()
                    .postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
                patternsDirty = false;
            } catch (GridAccessException ignored) {}
        }
        if (!getProxy().isActive() || worldObj.getTotalWorldTime() % 5 != 0) return;
        flushReturn();
        for (int i = 0; i < Math.min(WORK_PER_TICK, lanes.size()); i++) {
            service(lanes.get(Math.floorMod(workCursor++, lanes.size())));
        }
        flushReturn();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper helper) {
        patterns.clear();
        Set<RecipeMap<?>> recipeMaps = new LinkedHashSet<>();
        for (Lane lane : lanes) {
            if (lane.target.resolve(worldObj) instanceof BaseMetaTileEntity gt && OverloadedMachineInput.supports(gt))
                recipeMaps.add(((MTEBasicMachine) gt.getMetaTileEntity()).getRecipeMap());
        }
        for (int slot = 0; slot < PATTERNS; slot++) {
            ItemStack stack = inventory[slot];
            if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem)) {
                patternCaches.remove(slot);
                continue;
            }
            List<ProviderPattern> expanded = patternCaches
                .computeIfAbsent(slot, ignored -> new WildcardPatternCache<>())
                .get(stack, worldObj, () -> decodePatterns(stack));
            for (ProviderPattern pattern : expanded) {
                ICraftingPatternDetails details = pattern.original;
                if (pattern.runtime != null) {
                    for (RecipeMap<?> map : recipeMaps) {
                        if (pattern.runtimeMaps.computeIfAbsent(
                            map,
                            key -> !OverloadedMachineInput.patternConfigurations(key, pattern.runtime)
                                .isEmpty())) {
                            details = pattern.runtime;
                            break;
                        }
                    }
                }
                if (patterns.add(details)) helper.addCraftingOption(this, details);
            }
        }
    }

    private List<ProviderPattern> decodePatterns(ItemStack stack) {
        List<ICraftingPatternDetails> decoded;
        if (WildcardPatternGenerator.isWildcardPattern(stack) && !WildcardPatternGenerator.isGeneratedPattern(stack)) {
            decoded = WildcardPatternGenerator.generateAllDetails(stack, worldObj);
        } else {
            decoded = Collections.singletonList(CircuitPatternCodec.decode(stack, worldObj));
        }
        List<ProviderPattern> result = new ArrayList<>();
        for (ICraftingPatternDetails details : decoded) {
            if (details == null || details.isCraftable()) continue;
            result.add(
                new ProviderPattern(
                    details,
                    CircuitPatternCodec
                        .runtime(details.getPattern(), worldObj, details, CircuitPatternCodec.molds(details))));
        }
        return result;
    }

    public boolean supportsBatchCrafting(ICraftingPatternDetails details) {
        if (!isServerSide() || details.isCraftable()
            || details.isInputOnly()
            || details.canSubstitute()
            || !patterns.contains(details)
            || lanes.isEmpty()) return false;
        // Ordinary inventories require a whole delivery to fit; only GT targets have persistent overflow.
        boolean supported = false;
        for (Lane lane : lanes) {
            TileEntity target = lane.target.resolve(worldObj);
            if (roundRobin && target == null) continue;
            if (!(target instanceof BaseMetaTileEntity gt) || !OverloadedMachineInput.supports(gt)) return false;
            supported = true;
        }
        return supported;
    }

    @Override
    public boolean isBusy() {
        return !isServerSide() || !getProxy().isActive()
            || lanes.isEmpty()
            || lanes.stream()
                .allMatch(lane -> !lane.pending.isEmpty() || !lane.machineBuffer.isEmpty());
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting table) {
        if (isBusy() || !patterns.contains(details)) return false;
        long crafts = table instanceof LargeInventoryCrafting large ? large.getAssemblerSize() : 1;
        if (crafts <= 0 || crafts > 1 && !supportsBatchCrafting(details)) return false;
        if (roundRobin && crafts > 1) return pushRoundRobinBatch(details, table, crafts);
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < table.getSizeInventory(); i++) {
            ItemStack stack = table.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) inputs.add(stack.copy());
        }
        boolean ordinaryItems = java.util.Arrays.stream(details.getCondensedAEInputs())
            .allMatch(input -> input instanceof IAEItemStack)
            && !details.getPattern()
                .getTagCompound()
                .hasKey("gtnrPatternCircuit")
            && !details.getPattern()
                .getTagCompound()
                .hasKey("gtnrPatternMolds");
        for (int offset = 0; offset < Math.min(16, lanes.size()); offset++) {
            int index = Math.floorMod(dispatchCursor + offset, lanes.size());
            Lane lane = lanes.get(index);
            if (!lane.pending.isEmpty() || !lane.machineBuffer.isEmpty()) continue;
            if (!(lane.target.resolve(worldObj) instanceof IInventory target)) continue;
            if (target instanceof BaseMetaTileEntity gt && OverloadedMachineInput.supports(gt)) {
                if (!OverloadedMachineInput
                    .push(gt, details, table, ForgeDirection.getOrientation(lane.target.face), lane.machineBuffer))
                    continue;
                ((OverloadedMachineConfiguration) gt.getMetaTileEntity()).gtnr$setProvider(xCoord, yCoord, zCoord);
            } else {
                if (!ordinaryItems || inputs.isEmpty()
                    || !OverloadedProviderInventory.insert(target, lane.target.face, inputs, true)) continue;
                lane.pending.addAll(inputs);
            }
            recordOutputs(lane, details);
            dispatchCursor = roundRobin ? index + 1 : index;
            markDirty();
            return true;
        }
        dispatchCursor = Math.floorMod(dispatchCursor + 16, lanes.size());
        return false;
    }

    private boolean pushRoundRobinBatch(ICraftingPatternDetails details, InventoryCrafting table, long crafts) {
        if (!(table instanceof MEInventoryCrafting source)) return false;
        MEInventoryCrafting single = splitBatch(source, crafts, 1);
        if (single == null) return false;
        List<Integer> candidates = new ArrayList<>();
        for (int offset = 0; offset < lanes.size(); offset++) {
            int index = Math.floorMod(dispatchCursor + offset, lanes.size());
            Lane lane = lanes.get(index);
            if (!lane.pending.isEmpty() || !lane.machineBuffer.isEmpty()) continue;
            if (lane.target.resolve(worldObj) instanceof BaseMetaTileEntity gt && OverloadedMachineInput
                .prepare(gt, details, single, ForgeDirection.getOrientation(lane.target.face), lane.machineBuffer)
                != null) candidates.add(index);
        }
        while (!candidates.isEmpty()) {
            List<OverloadedMachineInput.PreparedInput> prepared = new ArrayList<>();
            long share = crafts / candidates.size();
            int remainder = (int) (crafts % candidates.size());
            boolean retry = false;
            for (int i = 0; i < candidates.size(); i++) {
                long allocated = share + (i < remainder ? 1 : 0);
                if (allocated == 0) break;
                Lane lane = lanes.get(candidates.get(i));
                MEInventoryCrafting portion = splitBatch(source, crafts, allocated);
                if (portion == null) return false;
                var input = lane.target.resolve(worldObj) instanceof BaseMetaTileEntity gt ? OverloadedMachineInput
                    .prepare(gt, details, portion, ForgeDirection.getOrientation(lane.target.face), lane.machineBuffer)
                    : null;
                if (input == null) {
                    candidates.remove(i);
                    retry = true;
                    break;
                }
                prepared.add(input);
            }
            if (retry) continue;
            if (!OverloadedMachineInput.commit(prepared)) return false;
            for (int i = 0; i < prepared.size(); i++) {
                Lane lane = lanes.get(candidates.get(i));
                BaseMetaTileEntity gt = (BaseMetaTileEntity) lane.target.resolve(worldObj);
                ((OverloadedMachineConfiguration) gt.getMetaTileEntity()).gtnr$setProvider(xCoord, yCoord, zCoord);
                recordOutputs(lane, details);
            }
            int next = remainder > 0 ? remainder : prepared.size();
            dispatchCursor = candidates.get(next - 1) + 1;
            markDirty();
            return true;
        }
        return false;
    }

    private static MEInventoryCrafting splitBatch(MEInventoryCrafting source, long total, long crafts) {
        MEInventoryCrafting result = new MEInventoryCrafting(new ContainerNull(), source.getSizeInventory(), 1);
        if (!(result instanceof LargeInventoryCrafting large)) return null;
        large.setAssemblerSize(crafts);
        for (int slot = 0; slot < source.getSizeInventory(); slot++) {
            var stack = source.getAEStackInSlot(slot);
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount % total != 0) return null;
            result.setInventorySlotContents(
                slot,
                stack.copy()
                    .setStackSize(amount / total * crafts));
        }
        return result;
    }

    private static void recordOutputs(Lane lane, ICraftingPatternDetails details) {
        lane.outputs.clear();
        lane.fluidOutputs.clear();
        for (var output : details.getCondensedAEOutputs()) {
            if (output instanceof IAEItemStack item) lane.outputs.add(item.getItemStack());
            else if (output instanceof IAEFluidStack fluid) lane.fluidOutputs.add(fluid.getFluidStack());
        }
    }

    private void service(Lane lane) {
        if (!(lane.target.resolve(worldObj) instanceof IInventory target)) return;
        if (!lane.machineBuffer.isEmpty() && target instanceof BaseMetaTileEntity gt
            && gt.getMetaTileEntity() instanceof MTEBasicMachine machine
            && machine instanceof OverloadedMachineConfiguration configuration
            && configuration.gtnr$hasPending()) {
            OverloadedMachineInput.refill(machine, lane.machineBuffer);
            markDirty();
        }
        if (!lane.pending.isEmpty()
            && OverloadedProviderInventory.insert(target, lane.target.face, lane.pending, false)) {
            lane.pending.clear();
            markDirty();
        }
        if (!autoReturn) return;
        if (returnedFluids.size() < 9 && target instanceof BaseMetaTileEntity gt
            && gt.getMetaTileEntity() instanceof MTEBasicMachine machine) {
            FluidStack fluid = machine.getDrainableStack();
            if (fluid != null && lane.fluidOutputs.stream()
                .anyMatch(output -> output.isFluidEqual(fluid))) {
                for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                    FluidStack extracted = gt.drain(side, fluid, true);
                    if (extracted != null && extracted.amount > 0) {
                        returnedFluids.add(extracted);
                        markDirty();
                        break;
                    }
                }
            }
        }
        int returnSlot = PATTERNS;
        while (returnSlot < inventory.length && inventory[returnSlot] != null) returnSlot++;
        if (returnSlot == inventory.length) return;
        for (int slot = 0; slot < target.getSizeInventory(); slot++) {
            ItemStack stack = target.getStackInSlot(slot);
            if (stack == null || !isOutput(lane, stack) || !OverloadedProviderInventory.canExtract(target, slot, stack))
                continue;
            inventory[returnSlot] = target.decrStackSize(slot, Math.min(stack.stackSize, stack.getMaxStackSize()));
            target.markDirty();
            markDirty();
            flushReturn();
            break;
        }
    }

    private boolean isOutput(Lane lane, ItemStack stack) {
        for (ItemStack output : lane.outputs) if (OverloadedProviderInventory.same(output, stack)) return true;
        for (ICraftingPatternDetails pattern : patterns) {
            for (var output : pattern.getCondensedAEOutputs()) {
                if (output instanceof IAEItemStack item && OverloadedProviderInventory.same(item.getItemStack(), stack))
                    return true;
            }
        }
        return false;
    }

    private void flushReturn() {
        try {
            for (var iterator = returnedFluids.listIterator(); iterator.hasNext();) {
                FluidStack fluid = iterator.next();
                var remainder = Platform.poweredInsert(
                    getProxy().getEnergy(),
                    getProxy().getStorage()
                        .getFluidInventory(),
                    AEFluidStack.create(fluid),
                    new MachineSource(this));
                if (remainder == null) iterator.remove();
                else iterator.set(remainder.getFluidStack());
                markDirty();
            }
            for (int slot = PATTERNS; slot < inventory.length; slot++) {
                if (inventory[slot] == null) continue;
                var remainder = Platform.poweredInsert(
                    getProxy().getEnergy(),
                    getProxy().getStorage()
                        .getItemInventory(),
                    AEItemStack.create(inventory[slot]),
                    new MachineSource(this));
                inventory[slot] = remainder == null ? null : remainder.getItemStack();
                markDirty();
            }
        } catch (GridAccessException ignored) {}
    }

    public List<ItemStack> takeDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) drops.add(inventory[i]);
            inventory[i] = null;
        }
        for (Lane lane : lanes) {
            drops.addAll(lane.pending);
            lane.pending.clear();
            boolean hadPending = !lane.machineBuffer.isEmpty();
            drops.addAll(lane.machineBuffer.getItems());
            if (lane.machineBuffer.getFluid() != null)
                drops.add(com.glodblock.github.common.item.ItemFluidPacket.newStack(lane.machineBuffer.getFluid()));
            lane.machineBuffer.replace(java.util.Collections.emptyList(), null, ForgeDirection.UNKNOWN);
            if (hadPending && lane.target.resolve(worldObj) instanceof BaseMetaTileEntity gt
                && gt.getMetaTileEntity() instanceof OverloadedMachineConfiguration configuration)
                configuration.gtnr$setPending(false);
        }
        for (FluidStack fluid : returnedFluids)
            drops.add(com.glodblock.github.common.item.ItemFluidPacket.newStack(fluid));
        returnedFluids.clear();
        return drops;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        settings.canInteractWith(this::isUseableByPlayer);
        return OverloadedProviderGui.build(this, sync, settings);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new ModularScreen(ModList.ModIds.GT_NEVER_RESET, panel);
    }

    @Override
    public void securityBreak() {
        if (isServerSide()) worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (hasCustomName()) tag.setString("CustomName", customName);
        else tag.removeTag("CustomName");
        NBTTagList stacks = new NBTTagList();
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) continue;
            NBTTagCompound entry = inventory[slot].writeToNBT(new NBTTagCompound());
            entry.setInteger("Slot", slot);
            stacks.appendTag(entry);
        }
        tag.setTag("Inventory", stacks);
        tag.setInteger("PatternSlots", PATTERNS);
        tag.setTag("ReturnedFluids", writeFluids(returnedFluids));
        NBTTagList targets = new NBTTagList();
        for (Lane lane : lanes) {
            NBTTagCompound entry = lane.target.write();
            entry.setTag("Pending", writeStacks(lane.pending));
            entry.setTag("Outputs", writeStacks(lane.outputs));
            lane.machineBuffer.writeToNBT(entry);
            entry.setTag("FluidOutputs", writeFluids(lane.fluidOutputs));
            targets.appendTag(entry);
        }
        tag.setTag("Targets", targets);
        tag.setBoolean("AutoReturn", autoReturn);
        tag.setBoolean("RoundRobin", roundRobin);
        tag.setBoolean("PatternOptimization", patternOptimization);
        tag.setBoolean("TerminalVisible", terminalVisible);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        patternCaches.clear();
        patterns.clear();
        customName = tag.getString("CustomName");
        readFluids(tag.getTagList("ReturnedFluids", 10), returnedFluids);
        java.util.Arrays.fill(inventory, null);
        NBTTagList stacks = tag.getTagList("Inventory", 10);
        int savedPatternSlots = tag.hasKey("PatternSlots") ? tag.getInteger("PatternSlots") : LEGACY_PATTERNS;
        for (int i = 0; i < stacks.tagCount(); i++) {
            NBTTagCompound entry = stacks.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= savedPatternSlots) slot += PATTERNS - savedPatternSlots;
            if (slot >= 0 && slot < inventory.length) inventory[slot] = ItemStack.loadItemStackFromNBT(entry);
        }
        lanes.clear();
        NBTTagList targets = tag.getTagList("Targets", 10);
        for (int i = 0; i < Math.min(MAX_TARGETS, targets.tagCount()); i++) {
            NBTTagCompound entry = targets.getCompoundTagAt(i);
            PackagedTarget target = PackagedTarget.read(entry);
            if (target == null) continue;
            Lane lane = new Lane(target);
            readStacks(entry.getTagList("Pending", 10), lane.pending);
            readStacks(entry.getTagList("Outputs", 10), lane.outputs);
            lane.machineBuffer.readFromNBT(entry);
            readFluids(entry.getTagList("FluidOutputs", 10), lane.fluidOutputs);
            lanes.add(lane);
        }
        autoReturn = !tag.hasKey("AutoReturn") || tag.getBoolean("AutoReturn");
        roundRobin = !tag.hasKey("RoundRobin") || tag.getBoolean("RoundRobin");
        patternOptimization = !tag.hasKey("PatternOptimization") || tag.getBoolean("PatternOptimization");
        terminalVisible = !tag.hasKey("TerminalVisible") || tag.getBoolean("TerminalVisible");
        patternsDirty = true;
    }

    private static NBTTagList writeStacks(List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : stacks) {
            NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
            tag.setInteger("Amount", stack.stackSize);
            list.appendTag(tag);
        }
        return list;
    }

    private static NBTTagList writeFluids(List<FluidStack> fluids) {
        NBTTagList list = new NBTTagList();
        for (FluidStack fluid : fluids) list.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        return list;
    }

    private static void readFluids(NBTTagList list, List<FluidStack> fluids) {
        fluids.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
            if (fluid != null && fluid.amount > 0) fluids.add(fluid);
        }
    }

    private static void readStacks(NBTTagList list, List<ItemStack> stacks) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
            if (stack != null && tag.getInteger("Amount") > 0) {
                stack.stackSize = tag.getInteger("Amount");
                stacks.add(stack);
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (inventory[slot] == null || count <= 0) return null;
        ItemStack taken = inventory[slot].splitStack(Math.min(count, inventory[slot].stackSize));
        if (inventory[slot].stackSize == 0) inventory[slot] = null;
        patternCaches.remove(slot);
        patternsDirty = true;
        markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = inventory[slot];
        inventory[slot] = null;
        patternCaches.remove(slot);
        patternsDirty = true;
        markDirty();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        patternCaches.remove(slot);
        patternsDirty = true;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return hasCustomName() ? customName : "tile.overloaded_wireless_pattern_provider.name";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return hasCustomName();
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public boolean hasCustomName() {
        return !customName.isEmpty();
    }

    @Override
    public void setCustomName(String name) {
        if (worldObj != null && worldObj.isRemote) return;
        customName = name == null ? "" : name;
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && (worldObj.isRemote || canConfigure(player))
            && player.getDistanceSq(xCoord + .5, yCoord + .5, zCoord + .5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < PATTERNS && stack != null && stack.getItem() instanceof ICraftingPatternItem;
    }

    private static final class ProviderPattern {

        private final ICraftingPatternDetails original;
        private final ICraftingPatternDetails runtime;
        private final Map<RecipeMap<?>, Boolean> runtimeMaps = new HashMap<>();

        private ProviderPattern(ICraftingPatternDetails original, ICraftingPatternDetails runtime) {
            this.original = original;
            this.runtime = runtime;
        }
    }

    public static final class Lane {

        public final PackagedTarget target;
        private final List<ItemStack> pending = new ArrayList<>();
        private final List<ItemStack> outputs = new ArrayList<>();
        private final OverloadedMachineBuffer machineBuffer = new OverloadedMachineBuffer();
        private final List<FluidStack> fluidOutputs = new ArrayList<>();

        private Lane(PackagedTarget target) {
            this.target = target;
        }

        public int pendingKinds() {
            return pending.size() + machineBuffer.getItems()
                .size() + (machineBuffer.getFluid() == null ? 0 : 1);
        }
    }
}
