package com.xyp.gtneverreset.common.machine.hatch;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.common.gui.modularui.MaxCapacityMEOutputGui;
import com.xyp.gtneverreset.utils.machine.FilteredMEOutputTransaction;
import com.xyp.gtneverreset.utils.machine.MaxCapacityMEOutputCapacity;
import com.xyp.gtneverreset.utils.machine.MaxCapacityMEOutputFilters;

import appeng.api.storage.StorageChannel;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import io.netty.buffer.ByteBuf;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MaxCapacityMEOutputHatch extends MTEHatchOutputME {

    private final MaxCapacityMEOutputFilters filters = new MaxCapacityMEOutputFilters(this::filtersChanged);

    public MaxCapacityMEOutputHatch(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        forceMaxCapacity();
    }

    public MaxCapacityMEOutputHatch(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
        forceMaxCapacity();
    }

    public MaxCapacityMEOutputFilters getFilters() {
        return filters;
    }

    private void filtersChanged() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            markDirty();
            notifyOutputSpaceChanged();
        }
    }

    @Override
    public boolean isFiltered() {
        return filters.hasFluids() || super.isFiltered();
    }

    @Override
    public boolean isFluidLocked() {
        return isFiltered();
    }

    @Override
    public boolean isEmptyAndAcceptsAnyFluid() {
        return !filters.hasFluids() && super.isEmptyAndAcceptsAnyFluid();
    }

    @Override
    public boolean canStoreFluid(FluidStack stack) {
        return filters.accepts(stack) && super.canStoreFluid(stack);
    }

    @Override
    public boolean isFilteredToFluid(GTUtility.FluidId id) {
        return canStoreFluid(id.getFluidStack());
    }

    @Override
    public OutputHatchType getHatchType() {
        if (getProvider().getCacheMode())
            return isFiltered() ? OutputHatchType.MECacheFiltered : OutputHatchType.MECacheUnfiltered;
        return isFiltered() ? OutputHatchType.MEFiltered : OutputHatchType.MEUnfiltered;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MaxCapacityMEOutputGui.Fluids(this).build(data, sync, settings);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MaxCapacityMEOutputHatch(this.mName, this.mTier, getDescription(), this.mTextures);
    }

    @Override
    public String[] getDescription() {
        return getLocalizedDescription();
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        forceMaxCapacity();
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        forceMaxCapacity();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool);
        forceMaxCapacity();
    }

    @Override
    public IOutputHatchTransaction createTransaction() {
        forceMaxCapacity();
        return new FilteredMEOutputTransaction.Fluids(super.createTransaction(), filters::accepts);
    }

    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        if (!filters.accepts(aFluid)) return 0;
        forceMaxCapacity();
        return super.fill(aFluid, doFill);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        forceMaxCapacity();
        super.setItemNBT(aNBT);
        aNBT.removeTag("baseCapacity");
        MaxCapacityMEOutputFilters.clearItemData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        forceMaxCapacity();
        super.saveNBTData(aNBT);
        filters.save(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        filters.load(aNBT);
        forceMaxCapacity();
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        forceMaxCapacity();
        super.writeToStream(buffer);
    }

    @Override
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        forceMaxCapacity();
    }

    @Override
    public boolean hasAvailableSpace() {
        forceMaxCapacity();
        return super.hasAvailableSpace();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        forceMaxCapacity();
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> ss, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        forceMaxCapacity();
        super.getWailaBody(itemStack, ss, accessor, config);
    }

    @Override
    public List getCellArray(StorageChannel channel) {
        forceMaxCapacity();
        return super.getCellArray(channel);
    }

    private void forceMaxCapacity() {
        MaxCapacityMEOutputCapacity.forceMaxCapacity(getProvider());
    }

    private static String[] getLocalizedDescription() {
        return new String[] {
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.0
            // # Transfers multiblock fluid outputs to the ME network
            // # zh_CN 将多方块机器产出的流体存入 ME 网络
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.0"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.1
            // # Requires a powered ME network and one channel
            // # zh_CN 需要 ME 网络供电和一个频道
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.1"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.2
            // # Output cache capacity: 2^63 - 1 L of fluid
            // # zh_CN 输出缓存容量：2^63 - 1 L
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.2"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.3
            // # 9 filters; empty accepts all; configured filters accept matching outputs only
            // # zh_CN 9 格流体筛选；全空时接收全部，设置后仅接收匹配产物
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.3"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.4
            // # Screwdriver: toggle cache mode
            // # zh_CN 螺丝刀右键：切换缓存模式
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.4"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.5
            // # Shift + screwdriver: toggle check mode
            // # zh_CN 潜行螺丝刀右键：切换检查模式
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.5"),
            // #tr tooltip.gtneverreset.maxCapacityMEOutputHatch.priority
            // # Matching filters take priority over unfiltered outputs
            // # zh_CN 筛选匹配的输出仓优先于未设置筛选的输出仓
            StatCollector.translateToLocal("tooltip.gtneverreset.maxCapacityMEOutputHatch.priority") };
    }
}
