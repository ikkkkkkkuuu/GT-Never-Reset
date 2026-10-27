package com.xyp.gtneverreset.common.machine.hatch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.IItemVault;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.BlockIcons;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.IMEConnectable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

public class VaultPortHatch extends MTEHatch
    implements ICellContainer, IGridProxyable, IActionHost, IPowerChannelState, IMEConnectable, IAEPowerStorage {

    private static final IIconContainer OVERLAY_FRONT = BlockIcons.OVERLAY_FRONT_ITEMVAULTPORTHATCH;

    public IItemVault controller;
    private AENetworkProxy gridProxy;
    private final BaseActionSource machineSource = new MachineSource(this);

    public final IMEInventoryHandler<IAEItemStack> itemHandler = new ItemMEInventory();
    public final IMEInventoryHandler<IAEFluidStack> fluidHandler = new FluidMEInventory();

    public VaultPortHatch(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            3,
            0,
            new String[] {
                // #tr gtnr.hatch.vault_port.tooltip.0
                // # Lets the ME network insert and extract items and fluids stored in the vault
                // # zh_CN 供 ME 网络存入和取出保险库中的物品与流体
                StatCollector.translateToLocal("gtnr.hatch.vault_port.tooltip.0"),
                // #tr gtnr.hatch.vault_port.network
                // # Connect ME on any side; requires a formed vault and one channel, and supplies ME power
                // # zh_CN 可从任意面连接 ME；需要保险库成型和一个频道，并为 ME 网络供电
                StatCollector.translateToLocal("gtnr.hatch.vault_port.network") });
    }

    public VaultPortHatch(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new VaultPortHatch(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public ITexture getCasingTexture() {
        return BlockLoader.eddingtonWallCasing.getCasingTexture();
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN),
            TextureFactory.builder()
                .addIcon(OVERLAY_FRONT)
                .extFacing()
                .build() };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN),
            TextureFactory.builder()
                .addIcon(OVERLAY_FRONT)
                .extFacing()
                .build() };
    }

    @Override
    public void onFirstTick(IGregTechTileEntity baseMetaTileEntity) {
        super.onFirstTick(baseMetaTileEntity);
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.onReady();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            if (controller != null && !controller.isValid()) {
                unbind();
            }
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);

        if (this.gridProxy != null) {
            NBTTagCompound proxyTag = new NBTTagCompound();
            this.gridProxy.writeToNBT(proxyTag);
            aNBT.setTag("gridProxy", proxyTag);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        NBTTagCompound proxyTag = aNBT.getCompoundTag("gridProxy");
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.readFromNBT(proxyTag);
    }

    public void bind(IItemVault controller) {
        if (this.controller == controller) return;
        unbind();
        this.controller = controller;
        if (controller == null) return;
        if (this.controller.hasItem()) {
            for (IAEItemStack item : controller.getStoreItems()) {
                postUpdateItem(item.getItemStack(), item.getStackSize());
            }
        }
        if (this.controller.hasFluid()) {
            for (IAEFluidStack fluid : controller.getStoreFluids()) {
                postUpdateFluid(fluid.getFluidStack(), fluid.getStackSize());
            }
        }
    }

    public void unbind() {
        if (this.controller == null) return;
        if (this.controller.hasItem()) {
            for (IAEItemStack item : controller.getStoreItems()) {
                postUpdateItem(item.getItemStack(), -item.getStackSize());
            }
        }
        if (this.controller.hasFluid()) {
            for (IAEFluidStack fluid : controller.getStoreFluids()) {
                postUpdateFluid(fluid.getFluidStack(), -fluid.getStackSize());
            }
        }
        this.controller = null;
    }

    @Override
    public void onRemoval() {
        unbind();
        super.onRemoval();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public IGridNode getActionableNode() {
        AENetworkProxy gp = getProxy();
        return gp != null ? gp.getNode() : null;
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            if (getBaseMetaTileEntity() instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy(this, "proxy", GTNRItemList.VaultPortHatch.get(1), true);
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                gridProxy.setIdlePowerUsage(0);
                var bmte = getBaseMetaTileEntity();
                updateValidGridProxySides();
                if (bmte.getWorld() != null) {
                    gridProxy.setOwner(
                        bmte.getWorld()
                            .getPlayerEntityByName(bmte.getOwnerName()));
                }
            }
        }
        return gridProxy;
    }

    public void updateValidGridProxySides() {
        AENetworkProxy proxy = getProxy();
        if (proxy != null) proxy.setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
    }

    @Override
    public void onFacingChange() {
        super.onFacingChange();
        updateValidGridProxySides();
    }

    @Override
    public boolean connectsToAllSides() {
        return true;
    }

    @Override
    public void setConnectsToAllSides(boolean connects) {
        updateValidGridProxySides();
    }

    @Override
    public IGridNode getGridNode(ForgeDirection forgeDirection) {
        AENetworkProxy gp = getProxy();
        return gp != null ? gp.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {}

    @Override
    @SuppressWarnings("rawtypes")
    public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
        if (channel == StorageChannel.ITEMS) {
            return Collections.singletonList(itemHandler);
        } else if (channel == StorageChannel.FLUIDS) {
            return Collections.singletonList(fluidHandler);
        }
        return Collections.emptyList();
    }

    @Override
    public void saveChanges(IMEInventory cellInventory) {
        // This is handled by host itself.
    }

    @Override
    public boolean isPowered() {
        return getProxy() != null && getProxy().isPowered();
    }

    @Override
    public boolean isActive() {
        return getProxy() != null && getProxy().isActive();
    }

    @Override
    public DimensionalCoord getLocation() {
        IGregTechTileEntity gtm = this.getBaseMetaTileEntity();
        return new DimensionalCoord(gtm.getWorld(), gtm.getXCoord(), gtm.getYCoord(), gtm.getZCoord());
    }

    public void postUpdateItem(ItemStack itemStack, long amt) {
        if (itemStack == null || amt == 0) return;
        AENetworkProxy proxy = getProxy();
        if (proxy == null) return;
        try {
            proxy.getStorage()
                .postAlterationOfStoredItems(
                    StorageChannel.ITEMS,
                    Collections.singletonList(
                        AEItemStack.create(itemStack)
                            .setStackSize(amt)),
                    this.machineSource);
        } catch (GridAccessException e) {}
    }

    public void postUpdateFluid(FluidStack fluid, long amt) {
        if (fluid == null || amt == 0) return;
        AENetworkProxy proxy = getProxy();
        if (proxy == null) return;
        try {
            proxy.getStorage()
                .postAlterationOfStoredItems(
                    StorageChannel.FLUIDS,
                    Collections.singletonList(
                        AEFluidStack.create(fluid)
                            .setStackSize(amt)),
                    this.machineSource);
        } catch (GridAccessException e) {}
    }

    @MENetworkEventSubscribe
    public void powerRender(final MENetworkPowerStatusChange c) {
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy != null && proxy.isActive()) {
                proxy.getGrid()
                    .postEvent(new MENetworkCellArrayUpdate());
            }
        } catch (GridAccessException e) {}
    }

    @MENetworkEventSubscribe
    public void channelRender(final MENetworkChannelsChanged c) {
        try {
            AENetworkProxy proxy = getProxy();
            if (proxy != null && proxy.isActive()) {
                proxy.getGrid()
                    .postEvent(new MENetworkCellArrayUpdate());
            }
        } catch (GridAccessException e) {}
    }

    @Override
    public double injectAEPower(double amt, Actionable mode) {
        return 0;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        return amt;
    }

    @Override
    public double getAEMaxPower() {
        return Long.MAX_VALUE / 10000d;
    }

    @Override
    public double getAECurrentPower() {
        return Long.MAX_VALUE / 10000d;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isInfinite() {
        return true;
    }

    private final class ItemMEInventory implements IMEInventoryHandler<IAEItemStack> {

        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
            if (input == null) return null;
            final ItemStack inputStack = input.getItemStack();
            if (inputStack == null) return null;
            if (controller == null || getBaseMetaTileEntity() == null || !controller.hasItem()) return input;
            if (mode != Actionable.SIMULATE) getBaseMetaTileEntity().markDirty();
            long amount = controller.injectItems(input, mode != Actionable.SIMULATE);
            if (amount == 0) return input;
            if (amount == input.getStackSize()) return null;
            IAEItemStack result = AEItemStack.create(input.getItemStack());
            return result.copy()
                .setStackSize(input.getStackSize() - amount);
        }

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
            if (request == null) return null;
            if (controller == null || getBaseMetaTileEntity() == null || !controller.hasItem()) return null;
            if (mode != Actionable.SIMULATE) getBaseMetaTileEntity().markDirty();
            long amount = controller.extractItems(request, mode != Actionable.SIMULATE);
            if (amount == 0) return null;
            if (amount == request.getStackSize()) return request.copy();
            IAEItemStack result = AEItemStack.create(request.getItemStack());
            return result.copy()
                .setStackSize(amount);
        }

        @Override
        public StorageChannel getChannel() {
            return StorageChannel.ITEMS;
        }

        @Override
        public AccessRestriction getAccess() {
            return AccessRestriction.READ_WRITE;
        }

        @Override
        public boolean isPrioritized(IAEItemStack input) {
            return true;
        }

        @Override
        public boolean canAccept(IAEItemStack input) {
            if (controller == null || input == null || !controller.hasItem()) return false;
            return controller.containsItems(input.getItemStack())
                || controller.itemsCount() < controller.maxItemCount();
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public boolean validForPass(int i) {
            return true;
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
            if (controller != null && controller.hasItem()) {
                controller.copyItemsTo(out);
            }
            return out;
        }
    }

    private final class FluidMEInventory implements IMEInventoryHandler<IAEFluidStack> {

        @Override
        public IAEFluidStack injectItems(IAEFluidStack input, Actionable mode, BaseActionSource src) {
            if (input == null) return null;
            final FluidStack inputStack = input.getFluidStack();
            if (inputStack == null) return null;
            if (controller == null || getBaseMetaTileEntity() == null || !controller.hasFluid()) return input;
            if (mode != Actionable.SIMULATE) getBaseMetaTileEntity().markDirty();
            long amount = controller.injectFluids(input, mode != Actionable.SIMULATE);
            if (amount == 0) return input;
            if (amount == input.getStackSize()) return null;
            IAEFluidStack result = AEFluidStack.create(input.getFluidStack());
            return result.copy()
                .setStackSize(input.getStackSize() - amount);
        }

        @Override
        public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, BaseActionSource src) {
            if (request == null) return null;
            if (controller == null || getBaseMetaTileEntity() == null || !controller.hasFluid()) return null;
            if (mode != Actionable.SIMULATE) getBaseMetaTileEntity().markDirty();
            long amount = controller.extractFluids(request, mode != Actionable.SIMULATE);
            if (amount == 0) return null;
            if (amount == request.getStackSize()) return request.copy();
            IAEFluidStack result = AEFluidStack.create(request.getFluidStack());
            return result.copy()
                .setStackSize(amount);
        }

        @Override
        public StorageChannel getChannel() {
            return StorageChannel.FLUIDS;
        }

        @Override
        public AccessRestriction getAccess() {
            return AccessRestriction.READ_WRITE;
        }

        @Override
        public boolean isPrioritized(IAEFluidStack input) {
            return true;
        }

        @Override
        public boolean canAccept(IAEFluidStack input) {
            if (controller == null || input == null || !controller.hasFluid()) return false;
            return controller.containsFluids(input.getFluidStack())
                || controller.fluidsCount() < controller.maxFluidCount();
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public boolean validForPass(int i) {
            return true;
        }

        @Override
        public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out, int iteration) {
            if (controller != null && controller.hasFluid()) {
                controller.copyFluidsTo(out);
            }
            return out;
        }
    }

}
