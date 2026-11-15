package com.xyp.gtneverreset.common.machine.hatch;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.common.gui.modularui.hatch.ChaosCredentialHatchGui;
import com.xyp.gtneverreset.common.item.items.ChaosCredentialItem;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

@IMetaTileEntity.SkipGenerateDescription
public final class ChaosCredentialHatch extends MTEHatch {

    // #tr gtnr.hatch.chaos_credential.name
    // # Chaos Credential Hatch
    // # zh_CN 混沌凭证仓
    public static final String NAME_KEY = "gtnr.hatch.chaos_credential.name";
    private BottleInChaos controller;
    private int group = -1;

    public ChaosCredentialHatch(int id, String name, String regionalName) {
        super(id, name, regionalName, 8, 1, new String[0]);
    }

    private ChaosCredentialHatch(String name, ITexture[][][] textures) {
        super(name, 8, 1, new String[0], textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ChaosCredentialHatch(mName, mTextures);
    }

    public boolean bind(BottleInChaos machine, int slot) {
        if (controller != null && controller != machine
            && controller.getBaseMetaTileEntity() != null
            && !controller.getBaseMetaTileEntity()
                .isDead()
            && controller.mMachine) return false;
        controller = machine;
        group = slot;
        return true;
    }

    public void unbind(BottleInChaos machine) {
        if (controller == machine) {
            controller = null;
            group = -1;
        }
    }

    public boolean locked() {
        return controller != null && controller.groupBusy(group);
    }

    public ItemStack credential() {
        return mInventory[0];
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new ChaosCredentialHatchGui(this).build(data, sync, settings);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    public String[] getDescription() {
        // #tr gtnr.chaos.hatch.help
        // # Insert a bound credential. The two positions below this hatch supply its processing group.
        // # zh_CN 放入已绑定凭证；仓室下方两格为该加工组的专用输入。
        return new String[] { StatCollector.translateToLocal("gtnr.chaos.hatch.help") };
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(Textures.BlockIcons.OVERLAY_DATA_ACCESS) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return getTexturesActive(base);
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isValidSlot(int index) {
        return index == 0 && !locked();
    }

    @Override
    public boolean isLiquidInput(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return index == 0 && !locked();
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return index == 0 && !locked() && ChaosCredentialItem.isBound(stack);
    }
}
