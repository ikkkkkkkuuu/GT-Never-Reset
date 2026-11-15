package com.xyp.gtneverreset.common.machine.hatch;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosLocalization;
import com.xyp.gtneverreset.common.crafting.chaos.material.BottledChaosMaterialCache;
import com.xyp.gtneverreset.common.item.items.ChaosRouteItem;
import com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public class ChaosCredentialHatch extends ChaosRouteHatchBase {

    // #tr gtnr.chaos.processing_hatch.name
    // # Chaos Processing Group Hatch
    // # zh_CN 混沌加工组仓
    public static final String NAME_KEY = "gtnr.chaos.processing_hatch.name";
    private static final String EXECUTION_ENABLED_NBT = "bottledChaosExecutionEnabled";
    private static final String INFORMATION_NBT = "bottledChaosHatchInformation";
    private static final String MATERIAL_CACHE_NBT = "bottledChaosMaterialCache";

    private final BottledChaosMaterialCache materialCache = new BottledChaosMaterialCache();

    private boolean executionEnabled = true;
    private long selectedRoutes = 1;
    private static final String SELECTED_ROUTES_NBT = "chaosSelectedRoutes";
    private boolean clearRunningStateRequested;
    private BottleInChaosMachineBase<?> controller;
    private int processingGroup = -1;
    private com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.CustomRoutes customRoutes;
    private boolean customRoutesDirty = true;

    public boolean hasCustomPatterns() {
        for (int slot = 1; slot < mInventory.length; slot++) if (mInventory[slot] != null) return true;
        return false;
    }

    public com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.CustomRoutes getCustomRoutes() {
        if (customRoutesDirty) {
            customRoutesDirty = false;
            customRoutes = null;
            if (hasCustomPatterns()) {
                try {
                    var chain = com.xyp.gtneverreset.common.me.ChaosPatternCompiler.compile(
                        java.util.Arrays.asList(mInventory)
                            .subList(1, mInventory.length),
                        getBaseMetaTileEntity().getWorld());
                    customRoutes = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor
                        .compileCustom(chain);
                } catch (IllegalArgumentException | ArithmeticException failure) {
                    com.xyp.gtneverreset.GTNeverReset.LOG.debug("invalid custom chaos route", failure);
                }
            }
        }
        return customRoutes;
    }

    private static final String[] DESCRIPTION = {
        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.1
        // # Provides one processing group; select any unlocked preset directly.
        // # zh_CN 提供一个加工组，可直接选择全部预设路线，无需凭证。
        ChaosLocalization.tr("gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.1"),
        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.2
        // # The two positions below each processing hatch are dedicated inputs for its processing group.
        // # zh_CN 加工组仓下方两格为本组专用输入位.
        ChaosLocalization.tr("gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.2"),
        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.3
        // # Intermediate products are cached here, separately from other processing groups.
        // # zh_CN 中间产物缓存在本仓, 不与其他加工组共享.
        ChaosLocalization.tr("gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.3"),
        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.4
        // # The parallel controller module sets the processing group limit.
        // # zh_CN 可安装数量由并行控制器模块决定.
        ChaosLocalization.tr("gtnr.machine.BottleInChaosRecipeTokenHatch.tooltip.info.4") };

    public ChaosCredentialHatch(int id, String name, String nameRegional, int tier) {
        super(id, name, nameRegional, tier, DESCRIPTION);
    }

    public ChaosCredentialHatch(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new ChaosCredentialHatch(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected boolean isAuthorizationItem(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getAuthorizationStack() {
        boolean custom = hasCustomPatterns();
        ItemStack state = ChaosRouteItem.createCredential(
            ChaosRouteItem.MAX_META,
            custom ? 1 : selectedRoutes,
            custom ? 1 : ChaosRouteItem.getRequiredWeight(selectedRoutes));
        if (custom) {
            net.minecraft.nbt.NBTTagList patterns = new net.minecraft.nbt.NBTTagList();
            for (int slot = 1; slot < mInventory.length; slot++) if (mInventory[slot] != null) {
                NBTTagCompound pattern = mInventory[slot].writeToNBT(new NBTTagCompound());
                pattern.setInteger("slot", slot);
                patterns.appendTag(pattern);
            }
            state.getTagCompound()
                .setTag("chaosCustomPatterns", patterns);
        }
        return state;
    }

    public long getSelectedRoutes() {
        return selectedRoutes;
    }

    public void setSelectedRoutes(long routes) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        long valid = routes
            & com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel.ALL_ROUTE_MASK;
        if (routes != valid || ChaosRouteItem.getRequiredWeight(valid) > 1024 || selectedRoutes == valid) return;
        selectedRoutes = valid;
        onContentsChanged(0);
        markDirty();
    }

    private void saveSelectedRoutes(NBTTagCompound nbt) {
        NBTTagCompound routes = new NBTTagCompound();
        com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
            .writeRouteMask(routes, selectedRoutes);
        nbt.setTag(SELECTED_ROUTES_NBT, routes);
    }

    private void loadSelectedRoutes(NBTTagCompound nbt) {
        if (nbt.hasKey(SELECTED_ROUTES_NBT, 10)) {
            selectedRoutes = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
                .readRouteMask(nbt.getCompoundTag(SELECTED_ROUTES_NBT));
        }
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        customRoutesDirty = true;
        if (controller != null && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            controller.onCredentialHatchContentsChanged(this, processingGroup);
        }
    }

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    public BottledChaosMaterialCache getMaterialCache() {
        return materialCache;
    }

    public void setExecutionEnabled(boolean enabled) {
        if (executionEnabled == enabled) return;
        setExecutionEnabledFromController(enabled);
        if (controller != null && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            controller.onCredentialHatchExecutionChanged(this, processingGroup, enabled);
        }
    }

    public void setExecutionEnabledFromController(boolean enabled) {
        if (executionEnabled == enabled) return;
        executionEnabled = enabled;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    public void clearRunningState() {
        if (!isValid() || !getBaseMetaTileEntity().isServerSide()) return;
        materialCache.clearMaterials();
        clearRunningStateRequested = true;
        markDirty();
    }

    public boolean consumeClearRunningStateRequest() {
        if (!clearRunningStateRequested) return false;
        clearRunningStateRequested = false;
        return true;
    }

    public void setCacheLimit(long limit) {
        if (!isValid()) return;
        materialCache.setAmountLimit(limit);
        if (getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    public void exportMaterialCache() {
        if (controller != null) controller.exportMaterialCache(this, processingGroup);
    }

    public void bindController(BottleInChaosMachineBase<?> controller, int slot) {
        this.controller = controller;
        processingGroup = slot;
    }

    public void unbindController(BottleInChaosMachineBase<?> controller) {
        if (this.controller != controller) return;
        this.controller = null;
        processingGroup = -1;
    }

    public NBTTagCompound getInformation(boolean details) {
        return BottleInChaosMachineBase.getCredentialHatchInformation(this, controller, processingGroup, details);
    }

    @Override
    public void onRemoval() {
        unbindController(controller);
        super.onRemoval();
    }

    @Override
    public void onUnload() {
        unbindController(controller);
        super.onUnload();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setTag(INFORMATION_NBT, getInformation(true));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        BottleInChaosMachineBase.appendCredentialHatchWaila(
            currentTip,
            accessor.getNBTData()
                .getCompoundTag(INFORMATION_NBT),
            GuiScreen.isShiftKeyDown());
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setBoolean(EXECUTION_ENABLED_NBT, executionEnabled);
        saveSelectedRoutes(nbt);
        nbt.setTag(MATERIAL_CACHE_NBT, materialCache.save());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        customRoutesDirty = true;
        executionEnabled = !nbt.hasKey(EXECUTION_ENABLED_NBT) || nbt.getBoolean(EXECUTION_ENABLED_NBT);
        loadSelectedRoutes(nbt);
        materialCache.load(nbt.getCompoundTag(MATERIAL_CACHE_NBT));
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        nbt.setBoolean(EXECUTION_ENABLED_NBT, executionEnabled);
        saveSelectedRoutes(nbt);
        nbt.setTag(MATERIAL_CACHE_NBT, materialCache.save());
    }

    @Override
    public void initDefaultModes(NBTTagCompound nbt) {
        super.initDefaultModes(nbt);
        if (nbt == null) return;
        executionEnabled = !nbt.hasKey(EXECUTION_ENABLED_NBT) || nbt.getBoolean(EXECUTION_ENABLED_NBT);
        loadSelectedRoutes(nbt);
        if (nbt.hasKey(MATERIAL_CACHE_NBT, 10)) materialCache.load(nbt.getCompoundTag(MATERIAL_CACHE_NBT));
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return com.xyp.gtneverreset.common.gui.modularui.hatch.ChaosCredentialHatchGui
            .build(this, guiData, syncManager, uiSettings);
    }
}
