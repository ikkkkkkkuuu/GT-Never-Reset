package com.xyp.gtneverreset.common.block.blocks.tile;

import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.common.gui.StockIOGui;
import com.xyp.gtneverreset.common.me.stockio.StockIOLogic;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.client.gui.PixelFontModularScreen;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiThemes;

/** Full-block host for the same virtual stock transaction used by the cable-mounted interface. */
public final class TileEntityStockIOInterface extends TileEntity
    implements IGuiHolder<PosGuiData>, IGridProxyable, IActionHost {

    private static final int CONNECT_RETRY_LIMIT = 5;

    private AENetworkProxy gridProxy;
    private boolean readyCalled;
    private boolean ownerApplied;
    private String ownerName = "";
    private int connectRetryTicks;

    private ForgeDirection targetSide = ForgeDirection.NORTH;
    private final BaseActionSource actionSource = new MachineSource(this);
    private final StockIOLogic logic = new StockIOLogic(new StockIOLogic.Host() {

        @Override
        public boolean isServerSide() {
            return TileEntityStockIOInterface.this.isServerSide();
        }

        @Override
        public AENetworkProxy getProxy() {
            return TileEntityStockIOInterface.this.getProxy();
        }

        @Override
        public BaseActionSource getActionSource() {
            return actionSource;
        }

        @Override
        public TileEntity getTarget() {
            return TileEntityStockIOInterface.this.getTarget();
        }

        @Override
        public ForgeDirection getTargetSide() {
            return targetSide;
        }

        @Override
        public void setTargetSide(ForgeDirection side) {
            targetSide = side;
        }

        @Override
        public void markDirty() {
            TileEntityStockIOInterface.this.markDirty();
        }

        @Override
        public long getTimer() {
            return worldObj == null ? 0 : worldObj.getTotalWorldTime();
        }
    });

    public StockIOLogic getLogic() {
        return logic;
    }

    public ForgeDirection getTargetSide() {
        return targetSide;
    }

    public void setTargetSide(ForgeDirection side) {
        logic.setTargetSide(side);
    }

    public TileEntity getTarget() {
        if (worldObj == null) return null;
        int x = xCoord + targetSide.offsetX;
        int y = yCoord + targetSide.offsetY;
        int z = zCoord + targetSide.offsetZ;
        return worldObj.blockExists(x, y, z) ? worldObj.getTileEntity(x, y, z) : null;
    }

    private ItemStack getVisualRepresentation() {
        return GTNRItemList.StockIOInterface.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "mebridge_proxy", getVisualRepresentation(), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            gridProxy.setIdlePowerUsage(1);
            gridProxy.setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        }
        if (!ownerApplied && isServerSide() && !ownerName.isEmpty()) {
            EntityPlayer player = worldObj.getPlayerEntityByName(ownerName);
            if (player != null) {
                gridProxy.setOwner(player);
                ownerApplied = true;
            }
        }
        return gridProxy;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServerSide()) {
            if (!readyCalled) {
                getProxy().onReady();
                readyCalled = true;
                connectRetryTicks = CONNECT_RETRY_LIMIT;
            }
            if (connectRetryTicks > 0) {
                connectRetryTicks--;
                IGridNode node = getProxy().getNode();
                if (node != null) node.updateState();
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
            }
        }
        logic.tick();
    }

    @Override
    public void securityBreak() {
        if (isServerSide() && !isInvalid()) worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    public void writeContents(NBTTagCompound tag) {
        logic.writeContents(tag);
    }

    public void readContents(NBTTagCompound tag) {
        logic.readContents(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("mebridge_owner", ownerName);
        getProxy().writeToNBT(tag);
        writeContents(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        ownerName = tag.getString("mebridge_owner");
        ownerApplied = false;
        if (tag.hasKey("mebridge_proxy")) getProxy().readFromNBT(tag);
        readContents(tag);
    }

    public boolean isServerSide() {
        return worldObj != null && !worldObj.isRemote;
    }

    public void setOwnerName(String name) {
        ownerName = name == null ? "" : name;
        ownerApplied = false;
        markDirty();
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {}

    @Override
    public IGridNode getGridNode(ForgeDirection direction) {
        return getProxy().getNode();
    }

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection direction) {
        return AECableType.SMART;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (gridProxy != null) gridProxy.invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (gridProxy != null) gridProxy.onChunkUnload();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        settings.useTheme(GTGuiThemes.STANDARD.getId());
        return StockIOGui.build(logic, sync);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new PixelFontModularScreen(ModList.ModIds.GT_NEVER_RESET, mainPanel);
    }
}
