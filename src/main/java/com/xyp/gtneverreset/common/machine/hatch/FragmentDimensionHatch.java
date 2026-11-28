package com.xyp.gtneverreset.common.machine.hatch;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.common.gui.modularui.hatch.FragmentDimensionHatchGui;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.ISmartInputHatch;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class FragmentDimensionHatch extends MTEHatch implements ISmartInputHatch {

    // #tr gtnr.hatch.fragment_dimension.name
    // # Fragment Dimension Hatch
    // # zh_CN 碎采维度仓
    public static final String NAME_KEY = "gtnr.hatch.fragment_dimension.name";

    public FragmentDimensionHatch(int id, String name, String regionalName) {
        super(id, name, regionalName, VoltageIndex.EV, 1, new String[0]);
    }

    private FragmentDimensionHatch(String name, ITexture[][][] textures) {
        super(name, VoltageIndex.EV, 1, new String[0], textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new FragmentDimensionHatch(mName, mTextures);
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) notifyWatchers();
    }

    @Nullable
    public ItemStack getDimensionMarker() {
        ItemStack marker = mInventory[0];
        return isItemValidForSlot(0, marker) ? GTUtility.copyAmount(1, marker) : null;
    }

    @Override
    public boolean isValidSlot(int index) {
        return index == 0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return isValidSlot(index) && stack != null
            && stack.stackSize > 0
            && !GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend()
                .getDimensionRecipes(stack)
                .isEmpty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return getTexturesInactive(base);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(Textures.BlockIcons.OVERLAY_DATA_ACCESS) };
    }

    @Override
    public String getLocalNameKey() {
        return NAME_KEY;
    }

    @Override
    public String[] getDescription() {
        // #tr gtnr.hatch.fragment_dimension.marker
        // # Insert one dimension marker to select its collection recipes.
        // # zh_CN 放入一个维度标记，选择该维度的采集配方。
        String marker = StatCollector.translateToLocal("gtnr.hatch.fragment_dimension.marker");
        // #tr gtnr.hatch.fragment_dimension.drill
        // # A compatible drill head in a collector input bus selects fluid collection for every dimension hatch.
        // # zh_CN 采集器的输入总线有适用钻头时，所有维度仓切换至流体采集。
        String drill = StatCollector.translateToLocal("gtnr.hatch.fragment_dimension.drill");
        return new String[] { marker, drill };
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        openGui(player);
        return true;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return FragmentDimensionHatchGui.build(this, data, sync, settings);
    }
}
