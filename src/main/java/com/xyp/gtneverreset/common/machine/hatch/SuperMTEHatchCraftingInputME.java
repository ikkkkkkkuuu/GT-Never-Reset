package com.xyp.gtneverreset.common.machine.hatch;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;
import static gregtech.api.enums.GTValues.TIER_COLORS;
import static gregtech.api.enums.GTValues.VN;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_BUFFER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_BUS;
import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.ArrayUtils;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtneverreset.api.terminal.ITerminalVisibilityToggle;
import com.xyp.gtneverreset.common.gui.modularui.SuperMTEHatchCraftingInputMEGui;
import com.xyp.gtneverreset.utils.machine.ChatComponentInterfaceNameSuffix;
import com.xyp.gtneverreset.utils.machine.MoldDataManager;
import com.xyp.gtneverreset.utils.machine.PatternMEOutput;
import com.xyp.gtneverreset.utils.machine.WildcardPatternSlotSuper;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.interfaces.IInterfaceNameProvider;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;
import appeng.core.AppEng;
import appeng.core.sync.GuiBridge;
import appeng.helpers.ICustomNameObject;
import appeng.items.tools.quartz.ToolQuartzCuttingKnife;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.ScheduledReason;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.inv.WrapperInventoryRange;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.GTLoggers;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IMEConnectable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.modularui.IAddGregtechLogo;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.objects.GTDualInputPattern;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.extensions.ArrayExt;
import gregtech.common.config.Gregtech;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputHatchWithPattern;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;
import gregtech.crossmod.ae2.ChatComponentGhostCircuitSuffix;
import lombok.Setter;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public class SuperMTEHatchCraftingInputME extends MTEHatchInputBus
    implements IAddGregtechLogo, IPowerChannelState, ICraftingProvider, IGridProxyable, IDualInputHatchWithPattern,
    ICustomNameObject, IInterfaceViewable, IMEConnectable, ITerminalVisibilityToggle {

    // mInventory is used for storing patterns, circuit and manual slot (typically NC items)
    private static final int MAX_PATTERN_COUNT = 60 * 9;
    private static final int SLOT_MANUAL_SIZE = 9;

    private BaseActionSource requestSource = null;
    private final PatternMEOutput meOutput = new PatternMEOutput(this);
    private final List<PatternSlot<SuperMTEHatchCraftingInputME>> pendingPatternRefunds = new ArrayList<>();
    private boolean inputsSerializedIntoDrop;
    private final IInventory patternInventory = new WrapperInventoryRange(this, 0, getPatternCount(), false);
    private final List<SuperMTEHatchCraftingInputSlave> proxyHatches = new ArrayList<>();
    private long lastProxyHatchValidationTime = -1;

    private @Nullable AENetworkProxy gridProxy = null;

    // holds all internal inventories
    @SuppressWarnings("unchecked") // Java doesn't allow to create an array of a generic type.
    private final PatternSlot<SuperMTEHatchCraftingInputME>[] internalInventory = new PatternSlot[getPatternCount()];

    // a hash map for faster lookup of pattern slots, not necessarily all valid.
    private final Map<ICraftingPatternDetails, PatternSlot<SuperMTEHatchCraftingInputME>> patternDetailsPatternSlotMap = new HashMap<>(
        getPatternCount());

    private boolean needPatternSync = true;
    private boolean justHadNewItems = false;
    private int lastNonNullIndex = -1; // 用于动态计算行数

    private String customName = null;
    private final boolean supportFluids;
    private boolean additionalConnection = false;
    public boolean disablePatternOptimization = false;
    public boolean showPattern = true;

    private ScheduledReason scheduledReason = ScheduledReason.UNDEFINED;

    /**
     * Recipe map captured from the owning multiblock controller when the structure forms. GT5 sets {@code mRecipeMap}
     * on plain input buses, but this hatch is an {@link IDualInputHatch}, and MTEMultiBlockBase.addToMachineList
     * returns early on the dual-input branch before the mRecipeMap assignment — so mRecipeMap stays null for us. A
     * mixin on addToMachineList feeds the controller's recipe map here instead. See
     * MixinMTEMultiBlockBaseHatchRecipeMap.
     */
    @Setter
    private RecipeMap<?> controllerRecipeMap;

    public SuperMTEHatchCraftingInputME(int aID, String aName, String aNameRegional, boolean supportFluids) {
        this(aID, aName, aNameRegional, supportFluids, MAX_PATTERN_COUNT / 9);
    }

    public SuperMTEHatchCraftingInputME(int aID, String aName, String aNameRegional, boolean supportFluids,
        int patternRows) {
        super(aID, aName, aNameRegional, supportFluids ? 10 : 6, patternRows * 9 + SLOT_MANUAL_SIZE + 2, null);
        disableSort = true;
        this.supportFluids = supportFluids;
    }

    public SuperMTEHatchCraftingInputME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures,
        boolean supportFluids) {
        this(aName, aTier, aDescription, aTextures, supportFluids, MAX_PATTERN_COUNT / 9);
    }

    protected SuperMTEHatchCraftingInputME(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures,
        boolean supportFluids, int patternRows) {
        super(aName, aTier, patternRows * 9 + SLOT_MANUAL_SIZE + 2, aDescription, aTextures);
        this.supportFluids = supportFluids;
        disableSort = true;
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new SuperMTEHatchCraftingInputME(
            mName,
            mTier,
            mDescriptionArray,
            mTextures,
            supportFluids,
            getPatternCount() / 9);
    }

    /** Returns the independent item and fluid product caches owned by this hatch. */
    public PatternMEOutput getMEOutput() {
        return meOutput;
    }

    /** Reuses the hatch's security action source when exporting products. */
    public BaseActionSource getMEOutputActionSource() {
        return getRequest();
    }

    /** Wakes controllers attached to the hatch after output space grows. */
    public void notifyMEOutputSpaceChanged() {
        notifyWatchers();
        for (SuperMTEHatchCraftingInputSlave mirror : getProxyHatches()) {
            mirror.notifyMEOutputSpaceChanged();
        }
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return getTexturesInactive(aBaseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture,
            TextureFactory.of(supportFluids ? OVERLAY_ME_CRAFTING_INPUT_BUFFER : OVERLAY_ME_CRAFTING_INPUT_BUS) };
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        super.onPostTick(aBaseMetaTileEntity, aTimer);

        if (getBaseMetaTileEntity().isServerSide()) {
            meOutput.tick(aTimer);
            if (needPatternSync && aTimer % 10 == 0) {
                needPatternSync = !postMEPatternChange();
            }
            if (aTimer % 20 == 0) {
                getBaseMetaTileEntity().setActive(isActive());
            }
            if (justHadNewItems) {
                notifyWatchers();
                for (SuperMTEHatchCraftingInputSlave mirror : getProxyHatches()) {
                    mirror.onParentInvChange();
                }
                justHadNewItems = false;
            }
            if (usesDefaultPatternRefunds() && aTimer % 20 == 0 && !pendingPatternRefunds.isEmpty() && isActive()) {
                retryPatternRefunds(4);
            }
        }
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        getProxy().onReady();
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return getProxy().getNode();
    }

    @Override
    public void onColorChangeServer(byte aColor) {
        updateAE2ProxyColor();
    }

    public void addProxyHatch(SuperMTEHatchCraftingInputSlave proxy) {
        if (!proxyHatches.contains(proxy)) proxyHatches.add(proxy);
    }

    public void removeProxyHatch(SuperMTEHatchCraftingInputSlave proxy) {
        proxyHatches.remove(proxy);
    }

    public List<SuperMTEHatchCraftingInputSlave> getProxyHatches() {
        long currentTime = getBaseMetaTileEntity().getTimer();
        if (currentTime != lastProxyHatchValidationTime) {
            proxyHatches
                .removeIf(hatch -> hatch == null || hatch.getBaseMetaTileEntity() == null || hatch.getMaster() != this);
            lastProxyHatchValidationTime = currentTime;
        }
        return Collections.unmodifiableList(proxyHatches);
    }

    public void updateAE2ProxyColor() {
        AENetworkProxy proxy = getProxy();
        byte color = this.getColor();
        if (color == -1) {
            proxy.setColor(AEColor.Transparent);
        } else {
            proxy.setColor(AEColor.values()[Dyes.transformDyeIndex(color)]);
        }
        if (proxy.getNode() != null) {
            proxy.getNode()
                .updateState();
        }
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return isOutputFacing(forgeDirection) ? AECableType.SMART : AECableType.NONE;
    }

    private void updateValidGridProxySides() {
        if (additionalConnection) {
            getProxy().setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        } else {
            getProxy().setValidSides(EnumSet.of(getBaseMetaTileEntity().getFrontFacing()));
        }
    }

    @Override
    public void onFacingChange() {
        updateValidGridProxySides();
    }

    @Override
    public void securityBreak() {}

    @Override
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        if (!getBaseMetaTileEntity().isServerSide()) return true;
        if (aPlayer.isSneaking()) {
            IGregTechTileEntity te = getBaseMetaTileEntity();
            aPlayer.openGui(
                AppEng.instance(),
                GuiBridge.GUI_RENAMER.ordinal() << 5 | (side.ordinal()),
                te.getWorld(),
                te.getXCoord(),
                te.getYCoord(),
                te.getZCoord());
            return true;
        }

        additionalConnection = !additionalConnection;
        updateValidGridProxySides();
        aPlayer.addChatComponentMessage(
            new ChatComponentTranslation("GT5U.hatch.additionalConnection." + additionalConnection));
        return true;
    }

    @Override
    public boolean connectsToAllSides() {
        return additionalConnection;
    }

    @Override
    public void setConnectsToAllSides(boolean connects) {
        additionalConnection = connects;
        updateValidGridProxySides();
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "proxy", getStackForm(1), true);
            gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            updateValidGridProxySides();
            if (getBaseMetaTileEntity().getWorld() != null) gridProxy.setOwner(
                getBaseMetaTileEntity().getWorld()
                    .getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()));
        }

        return this.gridProxy;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(
            getBaseMetaTileEntity().getWorld(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    @Override
    public int rows() {
        if (lastNonNullIndex == -1) refreshLastNonNullIndex();
        int calculatedRows = (lastNonNullIndex + 9) / 9 + 1;
        if ((lastNonNullIndex + 1) % 9 == 0) {
            calculatedRows++;
        }
        return Math.min(calculatedRows, getPatternCount() / 9);
    }

    @Override
    public int rowSize() {
        return 9;
    }

    @Override
    public IInventory getPatterns() {
        return patternInventory;
    }

    @Override
    public String getName() {
        if (hasCustomName()) {
            return getCustomName();
        }
        StringBuilder name = new StringBuilder();
        String recipeMapKey = getRecipeMapNameKey();
        if (recipeMapKey != null) {
            // Prefer the controller's recipe-map name (e.g. "Assembler") over the machine icon name ("Large Steam
            // Assembler"), matching the NEI-overwrite auto-fill naming (GTUtil.getRecipeName -> recipe category name).
            name.append(StatCollector.translateToLocal(recipeMapKey));
        } else if (getCrafterIcon() != null) {
            name.append(getCrafterIcon().getDisplayName());
        } else {
            name.append(getLocalName());
        }

        IChatComponent suffix = this.getNameSuffix();
        if (suffix != null) {
            name.append(suffix.getUnformattedText());
        }
        return name.toString();
    }

    @Override
    public String getRawName() {
        if (hasCustomName()) {
            return getCustomName();
        }

        String recipeMapKey = getRecipeMapNameKey();
        if (recipeMapKey != null) {
            return recipeMapKey;
        }
        if (getCrafterIcon() != null) {
            return getCrafterIcon().getUnlocalizedName();
        } else {
            return getLocalName();
        }
    }

    /**
     * Reads the accepted controller's current recipe category, including mode changes that bypass
     * {@code setMachineMode}. Registered hatch watchers avoid a world scan; membership excludes controllers that
     * registered a watcher but rejected this input. Unbound hatches retain the captured-map fallback.
     *
     * @return untranslated recipe-category key for client-side localization, or null when no map is available
     */
    private String getRecipeMapNameKey() {
        RecipeMap<?> map = this.mRecipeMap != null ? this.mRecipeMap : this.controllerRecipeMap;
        for (var watcher : watchers) {
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && controller.mDualInputHatches.contains(this)) {
                map = controller.getRecipeMap();
                break;
            }
        }
        return map == null ? null : map.getDefaultRecipeCategory().unlocalizedName;
    }

    @Override
    public IChatComponent getNameSuffix() {
        IChatComponent suffix = null;

        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base instanceof IInterfaceNameProvider nameProvider) {
            suffix = nameProvider.getInterfaceNameSuffix();
        }

        IChatComponent preferred = null;
        ItemStack circuit = allowSelectCircuit() ? mInventory[getCircuitSlot()] : null;
        if (circuit != null && circuit.getItemDamage() > 0) {
            preferred = new ChatComponentGhostCircuitSuffix(Collections.singletonList(circuit.getItemDamage()));
        }

        StringBuilder metaSuffix = new StringBuilder();

        // 手动槽与虚拟模具使用 metadata 后缀，以匹配 NEI 自动搜索规则（例如 "Assembler 2 32"）。
        for (int i = getManualSlotStart(); i < getManualSlotStart() + SLOT_MANUAL_SIZE; i++) {
            ItemStack manualStack = mInventory[i];
            if (manualStack == null) continue;
            try {
                metaSuffix
                    .append(String.format(Gregtech.machines.ghostCircuitSuffixFormat, manualStack.getItemDamage()));
            } catch (IllegalFormatException ignored) {}
        }

        ItemStack mold = hasVirtualMoldSlot() ? mInventory[getMoldSlot()] : null;
        if (mold != null) {
            try {
                metaSuffix.append(String.format(Gregtech.machines.ghostCircuitSuffixFormat, mold.getItemDamage()));
            } catch (IllegalFormatException ignored) {}
        }

        if (metaSuffix.length() > 0) {
            IChatComponent metaComponent = new ChatComponentText(metaSuffix.toString());
            suffix = suffix == null ? metaComponent : suffix.appendSibling(metaComponent);
            IChatComponent preferredMeta = new ChatComponentText(metaSuffix.toString());
            preferred = preferred == null ? preferredMeta : preferred.appendSibling(preferredMeta);
        }

        if (hasCustomName() || suffix == null && preferred == null) return suffix;
        return new ChatComponentInterfaceNameSuffix(suffix, preferred);
    }

    @Override
    public TileEntity getTileEntity() {
        return (TileEntity) getBaseMetaTileEntity();
    }

    @Override
    public boolean shouldDisplay() {
        return showPattern;
    }

    @Override
    public void toggleTerminalVisibility() {
        showPattern = !showPattern;
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    @Override
    public boolean allowsPatternOptimization() {
        return !disablePatternOptimization;
    }

    @Override
    public IAEStackType<?>[] getSupportedStackTypes() {
        if (supportFluids) {
            return new IAEStackType<?>[] { ITEM_STACK_TYPE, FLUID_STACK_TYPE };
        }
        return new IAEStackType<?>[] { ITEM_STACK_TYPE };
    }

    @Override
    public ItemStack getSelfRep() {
        return this.getStackForm(1);
    }

    /** Supplies AE2's controller-name fallback; custom names must remain literal rather than use the icon's name. */
    @Override
    public ItemStack getDisplayRep() {
        return hasCustomName() ? null : getCrafterIcon();
    }

    @Override
    public void gridChanged() {
        needPatternSync = true;
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
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        meOutput.save(aNBT);
        if (usesDefaultPatternRefunds()) writePatternRefunds(aNBT, false);

        // save internalInventory
        NBTTagList internalInventoryNBT = new NBTTagList();
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i] != null) {
                NBTTagCompound internalInventorySlotNBT = new NBTTagCompound();
                internalInventorySlotNBT.setInteger("patternSlot", i);
                internalInventorySlotNBT
                    .setTag("patternSlotNBT", internalInventory[i].writeToNBT(new NBTTagCompound()));
                internalInventoryNBT.appendTag(internalInventorySlotNBT);
            }
        }
        aNBT.setTag("internalInventory", internalInventoryNBT);
        if (customName != null) aNBT.setString("customName", customName);
        aNBT.setBoolean("additionalConnection", additionalConnection);
        aNBT.setBoolean("disablePatternOptimization", disablePatternOptimization);
        aNBT.setBoolean("showPattern", showPattern);
        getProxy().writeToNBT(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        meOutput.load(aNBT);
        pendingPatternRefunds.clear();
        inputsSerializedIntoDrop = false;
        if (usesDefaultPatternRefunds()) {
            NBTTagList refunds = aNBT.getTagList("patternInputRefunds", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < refunds.tagCount(); i++) {
                NBTTagCompound saved = refunds.getCompoundTagAt(i);
                ItemStack pattern = ItemStack.loadItemStackFromNBT(saved.getCompoundTag("pattern"));
                if (pattern != null && pattern.getItem() instanceof ICraftingPatternItem) {
                    PatternSlot<SuperMTEHatchCraftingInputME> slot = createPatternSlot(pattern, saved);
                    if (!slot.isEmpty()) pendingPatternRefunds.add(slot);
                }
            }
        }
        // load internalInventory
        NBTTagList internalInventoryNBT = aNBT.getTagList("internalInventory", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < internalInventoryNBT.tagCount(); i++) {
            NBTTagCompound internalInventorySlotNBT = internalInventoryNBT.getCompoundTagAt(i);
            int patternSlot = internalInventorySlotNBT.getInteger("patternSlot");
            NBTTagCompound patternSlotNBT = internalInventorySlotNBT.getCompoundTag("patternSlotNBT");
            ItemStack pattern = ItemStack.loadItemStackFromNBT(patternSlotNBT.getCompoundTag("pattern"));
            if (pattern != null && pattern.getItem() instanceof ICraftingPatternItem) {
                if (patternSlot >= 0 && patternSlot < internalInventory.length) {
                    internalInventory[patternSlot] = createPatternSlot(pattern, patternSlotNBT);
                }
            } else {
                GTLoggers.GT_FML_LOGGER.warn(
                    "An error occurred while loading contents of ME Crafting Input Bus. This pattern has been voided: "
                        + patternSlotNBT);
            }
        }

        // reconstruct patternDetailsPatternSlotMap
        patternDetailsPatternSlotMap.clear();
        for (PatternSlot<SuperMTEHatchCraftingInputME> patternSlot : internalInventory) {
            if (patternSlot != null) registerPatternOptions(patternSlot);
        }

        if (aNBT.hasKey("customName")) customName = aNBT.getString("customName");
        additionalConnection = aNBT.getBoolean("additionalConnection");
        disablePatternOptimization = aNBT.getBoolean("disablePatternOptimization");
        if (aNBT.hasKey("showPattern")) showPattern = aNBT.getBoolean("showPattern");

        getProxy().readFromNBT(aNBT);
        updateAE2ProxyColor();

        // Sync inventories to ensure that the real inventory matches what AE2 is seeing.
        for (int i = 0; i < getPatternCount(); i++) {
            if (internalInventory[i] == null) continue;
            mInventory[i] = internalInventory[i].pattern;
        }
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    private String describePattern(ICraftingPatternDetails patternDetails) {
        return Arrays.stream(patternDetails.getCondensedOutputs())
            .map(
                aeItemStack -> aeItemStack.getItem()
                    .getItemStackDisplayName(aeItemStack.getItemStack()))
            .collect(Collectors.joining(", "));
    }

    @Override
    public String[] getInfoData() {
        List<String> ret = new ArrayList<>();
        ret.add(
            (getProxy() != null && getProxy().isActive())
                ? StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.bus.online")
                : StatCollector.translateToLocalFormatted(
                    "GT5U.infodata.hatch.crafting_input_me.bus.offline",
                    getAEDiagnostics()));
        ret.add(
            StatCollector.translateToLocal(
                "GT5U.infodata.hatch.crafting_input_me.show_pattern." + (showPattern ? "enable" : "disabled")));
        ret.add(StatCollector.translateToLocal("GT5U.infodata.hatch.internal_inventory"));
        int i = 0;
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
            if (slot == null) continue;
            if (slot.getPatternDetails() == null) continue;
            i += 1;
            ret.add(
                StatCollector.translateToLocalFormatted(
                    "GT5U.infodata.hatch.internal_inventory.slot",
                    i,
                    EnumChatFormatting.BLUE + describePattern(slot.getPatternDetails()) + EnumChatFormatting.RESET));
            Map<GTUtility.ItemId, BigInteger> itemMap = new HashMap<>();
            for (IAEItemStack stack : slot.getStoredItems()) {
                ItemStack item = stack.copy()
                    .setStackSize(1)
                    .getItemStack();
                itemMap.merge(GTUtility.ItemId.create(item), BigInteger.valueOf(stack.getStackSize()), BigInteger::add);
            }
            for (Map.Entry<GTUtility.ItemId, BigInteger> entry : itemMap.entrySet()) {
                ItemStack item = entry.getKey()
                    .getItemStack();
                BigInteger amount = entry.getValue();
                ret.add(
                    item.getItem()
                        .getItemStackDisplayName(item) + ": "
                        + EnumChatFormatting.GOLD
                        + readableAmount(amount)
                        + EnumChatFormatting.RESET);
            }
            Map<Fluid, BigInteger> fluidMap = new HashMap<>();
            for (IAEFluidStack stack : slot.getStoredFluids()) {
                fluidMap.merge(stack.getFluid(), BigInteger.valueOf(stack.getStackSize()), BigInteger::add);
            }
            for (Map.Entry<Fluid, BigInteger> entry : fluidMap.entrySet()) {
                FluidStack fluid = new FluidStack(entry.getKey(), 1);
                BigInteger amount = entry.getValue();
                ret.add(
                    fluid.getLocalizedName() + ": "
                        + EnumChatFormatting.AQUA
                        + readableAmount(amount)
                        + EnumChatFormatting.RESET);
            }
        }
        if (!pendingPatternRefunds.isEmpty()) {
            // #tr tooltip.gtneverreset.crafting_input.refunds
            // # Pending input returns: %s buffers; reconnect ME to return them
            // # zh_CN 待退还原料：%s 份缓存；接回 ME 网络后自动退还
            ret.add(
                StatCollector.translateToLocalFormatted(
                    "tooltip.gtneverreset.crafting_input.refunds",
                    pendingPatternRefunds.size()));
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    public int getPatternCount() {
        return mInventory.length - SLOT_MANUAL_SIZE - 2;
    }

    public int getManualSlotStart() {
        return getCircuitSlot() + 1;
    }

    @Override
    public int getCircuitSlot() {
        return getPatternCount();
    }

    public int getMoldSlot() {
        return getManualSlotStart() + SLOT_MANUAL_SIZE;
    }

    public boolean hasVirtualMoldSlot() {
        return true;
    }

    public void setMold(@Nullable ItemStack selected) {
        if (!hasVirtualMoldSlot()) return;
        ItemStack phantom = findMatchingMold(selected);
        if (phantom != null) phantom.stackSize = 0;
        if (inventoryHandler != null) {
            inventoryHandler.setStackInSlot(getMoldSlot(), phantom);
        }
        setInventorySlotContents(getMoldSlot(), phantom);
        getBaseMetaTileEntity().markInventoryBeenModified();
    }

    @Nullable
    public static ItemStack findMatchingMold(@Nullable ItemStack stack) {
        if (stack == null) return null;
        for (ItemStack mold : MoldDataManager.getMolds()) {
            if (GTUtility.areStacksEqual(mold, stack, true)) return mold.copy();
        }
        return null;
    }

    public int findMatchingMoldIndex(@Nullable ItemStack stack) {
        if (stack == null) return -1;
        for (int i = 0; i < MoldDataManager.getMoldCount(); i++) {
            if (GTUtility.areStacksEqual(MoldDataManager.getMolds()[i], stack, true)) return i;
        }
        return -1;
    }

    @Override
    public int getCircuitSlotX() {
        return 170;
    }

    @Override
    public int getCircuitSlotY() {
        return 64;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new SuperMTEHatchCraftingInputMEGui(this).build(data, syncManager, uiSettings);
    }

    @Override
    public void updateSlots() {
        for (int slotId = getManualSlotStart(); slotId < getManualSlotStart() + SLOT_MANUAL_SIZE; ++slotId) {
            if (mInventory[slotId] != null && mInventory[slotId].stackSize <= 0) mInventory[slotId] = null;
        }
    }

    private BaseActionSource getRequest() {
        if (requestSource == null) requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        return requestSource;
    }

    public void onPatternChange(int index, ItemStack newItem) {
        if (!getBaseMetaTileEntity().isServerSide() || index < 0 || index >= getPatternCount()) return;

        World world = getBaseMetaTileEntity().getWorld();

        // remove old if applicable
        PatternSlot<SuperMTEHatchCraftingInputME> originalPattern = internalInventory[index];
        if (originalPattern != null) {
            if (originalPattern.hasChanged(newItem, world)) {
                try {
                    originalPattern.refund(getProxy(), getRequest(), false);
                } catch (GridAccessException ignored) {}
                if (usesDefaultPatternRefunds() && !originalPattern.isEmpty()) {
                    pendingPatternRefunds.add(originalPattern);
                    markDirty();
                }
                if (patternDetailsPatternSlotMap.remove(originalPattern.getPatternDetails(), originalPattern)) {
                    for (PatternSlot<SuperMTEHatchCraftingInputME> duplicate : internalInventory) {
                        if (duplicate != null && duplicate != originalPattern
                            && Objects.equals(duplicate.getPatternDetails(), originalPattern.getPatternDetails())) {
                            patternDetailsPatternSlotMap.put(duplicate.getPatternDetails(), duplicate);
                            break;
                        }
                    }
                }
                patternDetailsPatternSlotMap.values()
                    .removeIf(slot -> slot == originalPattern);
                internalInventory[index] = null;
                needPatternSync = true;
            } else {
                return; // nothing has changed
            }
        }

        // original does not exist or has changed
        if (newItem == null || !(newItem.getItem() instanceof ICraftingPatternItem)) {
            refreshLastNonNullIndex();
            return;
        }

        PatternSlot<SuperMTEHatchCraftingInputME> patternSlot = createPatternSlot(newItem, null);
        internalInventory[index] = patternSlot;
        registerPatternOptions(patternSlot);
        needPatternSync = true;

        refreshLastNonNullIndex(); // 更新最后非空索引
    }

    /** Allows specialized hatches to decode and expose a separate recipe inventory for each pattern. */
    protected PatternSlot<SuperMTEHatchCraftingInputME> createPatternSlot(ItemStack pattern, NBTTagCompound saved) {
        return WildcardPatternGenerator.isWildcardPattern(pattern)
            && !WildcardPatternGenerator.isGeneratedPattern(pattern)
                ? new WildcardPatternSlotSuper(this, pattern, saved)
                : new PatternSlot<>(pattern, saved, this);
    }

    private void registerPatternOptions(PatternSlot<SuperMTEHatchCraftingInputME> slot) {
        if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
            for (ICraftingPatternDetails details : wildcardSlot
                .getExpandedDetails(slot.pattern, getBaseMetaTileEntity().getWorld()))
                patternDetailsPatternSlotMap.put(details, slot);
        } else if (slot.getPatternDetails() != null) {
            patternDetailsPatternSlotMap.put(slot.getPatternDetails(), slot);
        }
    }

    protected PatternSlot<SuperMTEHatchCraftingInputME> getPatternSlot(int index) {
        return internalInventory[index];
    }

    /**
     * 刷新最后一个非空配方槽位的索引，用于动态计算GUI行数
     */
    public void refreshLastNonNullIndex() {
        lastNonNullIndex = -1;
        if (internalInventory == null || internalInventory.length == 0) return;

        for (int i = internalInventory.length - 1; i >= 0; i--) {
            if (internalInventory[i] != null) {
                lastNonNullIndex = i;
                break;
            }
        }
    }

    @Override
    public ItemStack[] getSharedItems() {
        ItemStack[] sharedItems = new ItemStack[SLOT_MANUAL_SIZE + 2];
        sharedItems[0] = mInventory[getCircuitSlot()];
        sharedItems[1] = mInventory[getMoldSlot()];
        System.arraycopy(mInventory, getManualSlotStart(), sharedItems, 2, SLOT_MANUAL_SIZE);
        return ArrayExt.withoutNulls(sharedItems, ItemStack[]::new);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
        if (tag.hasKey("name"))
            currenttip.add(EnumChatFormatting.AQUA + tag.getString("name") + EnumChatFormatting.RESET);
        currenttip.add(
            StatCollector.translateToLocal(
                "GT5U.infodata.hatch.crafting_input_me.show_pattern." + (showPattern ? "enable" : "disabled")));
        if (tag.hasKey("inventory")) {
            NBTTagList inventory = tag.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < inventory.tagCount(); ++i) {
                NBTTagCompound item = inventory.getCompoundTagAt(i);
                String name = item.getString("name");
                String amount = item.hasKey("amountText") ? item.getString("amountText")
                    : ReadableNumberConverter.INSTANCE.toWideReadableForm(item.getLong("amount"));
                currenttip.add(name + ": " + EnumChatFormatting.GOLD + amount + EnumChatFormatting.RESET);
            }
        }
        super.getWailaBody(itemStack, currenttip, accessor, config);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        tag.setBoolean("showPattern", showPattern);
        NBTTagList inventory = new NBTTagList();
        HashMap<String, BigInteger> nameToAmount = new HashMap<>();
        for (Iterator<PatternSlot<SuperMTEHatchCraftingInputME>> it = inventories(); it.hasNext();) {
            PatternSlot<SuperMTEHatchCraftingInputME> i = it.next();
            for (IAEItemStack item : i.getStoredItems()) {
                String name = item.copy()
                    .setStackSize(1)
                    .getItemStack()
                    .getDisplayName();
                nameToAmount.merge(name, BigInteger.valueOf(item.getStackSize()), BigInteger::add);
            }
            for (IAEFluidStack fluid : i.getStoredFluids()) {
                String name = fluid.copy()
                    .setStackSize(1)
                    .getFluidStack()
                    .getLocalizedName();
                nameToAmount.merge(name, BigInteger.valueOf(fluid.getStackSize()), BigInteger::add);
            }
        }
        for (Map.Entry<String, BigInteger> entry : nameToAmount.entrySet()) {
            NBTTagCompound item = new NBTTagCompound();
            item.setString("name", entry.getKey());
            item.setString("amountText", readableAmount(entry.getValue()));
            inventory.appendTag(item);
        }

        tag.setTag("inventory", inventory);
        if (!Objects.equals(getName(), getLocalName())) {
            tag.setString("name", getName());
        }
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
    }

    private static String readableAmount(BigInteger amount) {
        return amount.bitLength() <= 63 ? ReadableNumberConverter.INSTANCE.toWideReadableForm(amount.longValue())
            : NumberFormat.getIntegerInstance(Locale.ROOT)
                .format(amount);
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (!isActive()) return;

        patternDetailsPatternSlotMap.clear();
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
            if (slot == null) continue;
            if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
                for (ICraftingPatternDetails details : wildcardSlot
                    .getExpandedDetails(slot.pattern, getBaseMetaTileEntity().getWorld())) {
                    patternDetailsPatternSlotMap.put(details, slot);
                    craftingTracker.addCraftingOption(this, details);
                }
                continue;
            }
            ICraftingPatternDetails details = slot.getPatternDetails();
            if (details == null) {
                GTLoggers.GT_FML_LOGGER.warn(
                    "Found an invalid pattern at " + getBaseMetaTileEntity().getCoords()
                        + " in dim "
                        + getBaseMetaTileEntity().getWorld().provider.dimensionId);
                continue;
            }
            patternDetailsPatternSlotMap.put(details, slot);
            craftingTracker.addCraftingOption(this, details);
        }
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        if (!isActive()) return false;
        if (!getBaseMetaTileEntity().isAllowedToWork()) return false;
        if (!(table instanceof MEInventoryCrafting meic)) return false;

        for (int i = 0; i < table.getSizeInventory(); ++i) {
            IAEStack<?> stackInSlot = meic.getAEStackInSlot(i);
            if (stackInSlot == null || stackInSlot instanceof IAEItemStack
                || (supportFluids && stackInSlot instanceof IAEFluidStack)) {
                continue;
            }

            scheduledReason = ScheduledReason.UNSUPPORTED_STACK;
            return false;
        }

        PatternSlot<SuperMTEHatchCraftingInputME> slot = patternDetailsPatternSlotMap.get(patternDetails);
        if (slot == null && patternDetails != null) {
            String requestedId = WildcardPatternGenerator.getGeneratedPatternId(patternDetails.getPattern());
            if (!requestedId.isEmpty()) {
                for (Map.Entry<ICraftingPatternDetails, PatternSlot<SuperMTEHatchCraftingInputME>> entry : patternDetailsPatternSlotMap
                    .entrySet()) {
                    if (requestedId.equals(
                        WildcardPatternGenerator.getGeneratedPatternId(
                            entry.getKey()
                                .getPattern()))) {
                        slot = entry.getValue();
                        break;
                    }
                }
            }
        }
        if (slot instanceof WildcardPatternSlotSuper wildcardSlot) {
            if (!wildcardSlot.canAcceptPattern(patternDetails)) return false;
            wildcardSlot.setActivePatternDetails(patternDetails);
        }
        if (slot == null || !slot.insertItemsAndFluids(meic)) {
            scheduledReason = ScheduledReason.SOMETHING_STUCK;
            return false;
        }
        justHadNewItems = true;
        markDirty();
        return true;
    }

    @Override
    public ScheduledReason getScheduledReason() {
        return scheduledReason;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public Iterator<PatternSlot<SuperMTEHatchCraftingInputME>> inventories() {
        return Arrays.stream(internalInventory)
            .filter(Objects::nonNull)
            .iterator();
    }

    public Iterator<PatternSlot<SuperMTEHatchCraftingInputME>> inventoriesReversed() {
        return IntStream.range(0, internalInventory.length)
            .map(i -> internalInventory.length - 1 - i)
            .mapToObj(i -> internalInventory[i])
            .filter(Objects::nonNull)
            .iterator();
    }

    @Override
    public void onBlockDestroyed() {
        // 清除幽灵模具槽位，防止掉落
        if (inventoryHandler != null) {
            inventoryHandler.setStackInSlot(getMoldSlot(), null);
        }
        mInventory[getMoldSlot()] = null;

        refundAll(true);
        super.onBlockDestroyed();
    }

    public void refundAll(boolean shouldDrop) {
        // Destruction may already have serialized products into the dropped machine item.
        if (!shouldDrop) meOutput.flush();
        if (usesDefaultPatternRefunds() && shouldDrop && inputsSerializedIntoDrop) {
            for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
                if (slot == null) continue;
                slot.itemInventory.clear();
                slot.fluidInventory.clear();
            }
            pendingPatternRefunds.clear();
            return;
        }
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
            if (slot == null) continue;
            try {
                slot.refund(getProxy(), getRequest(), shouldDrop);
            } catch (GridAccessException ignored) {}
        }
        if (usesDefaultPatternRefunds()) retryPatternRefunds(pendingPatternRefunds.size());
        markDirty();
    }

    /** Preserves buffered products on the dropped machine stack when picked up with a wrench. */
    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        meOutput.save(tag);
        if (usesDefaultPatternRefunds()) {
            writePatternRefunds(tag, true);
            inputsSerializedIntoDrop = true;
        }
    }

    @Override
    public void onLeftclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        ItemStack dataStick = player.inventory.getCurrentItem();
        if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) saveToDataStick(player, dataStick);
    }

    public void saveToDataStick(EntityPlayer player, ItemStack dataStick) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", "CraftingInputBuffer");
        tag.setInteger("x", tile.getXCoord());
        tag.setInteger("y", tile.getYCoord());
        tag.setInteger("z", tile.getZCoord());
        tag.setInteger("dimension", tile.getWorld().provider.dimensionId);
        dataStick.setTagCompound(tag);
        // #tr item.gtnr.pattern_link.name
        // # Pattern Input Link (%s, %s, %s)
        // # zh_CN 样板输入连接数据（%s，%s，%s）
        dataStick.setStackDisplayName(
            StatCollector.translateToLocalFormatted(
                "item.gtnr.pattern_link.name",
                tile.getXCoord(),
                tile.getYCoord(),
                tile.getZCoord()));
        // #tr chat.gtnr.pattern_link.saved
        // # Saved pattern input link to Data Stick
        // # zh_CN 已将样板输入连接信息保存到数据棒
        GTUtility.sendChatTrans(player, "chat.gtnr.pattern_link.saved");
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        final ItemStack is = aPlayer.inventory.getCurrentItem();
        if (is != null && is.getItem() instanceof ToolQuartzCuttingKnife) {
            if (ForgeEventFactory.onItemUseStart(aPlayer, is, 1) <= 0) return false;
            IGregTechTileEntity te = getBaseMetaTileEntity();
            aPlayer.openGui(
                AppEng.instance(),
                GuiBridge.GUI_RENAMER.ordinal() << 5 | (side.ordinal()),
                te.getWorld(),
                te.getXCoord(),
                te.getYCoord(),
                te.getZCoord());
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer, side, aX, aY, aZ);
    }

    @Override
    public ItemStack getCrafterIcon() {
        return getMachineCraftingIcon();
    }

    private boolean postMEPatternChange() {
        // don't post until it's active
        if (!getProxy().isActive()) return false;
        try {
            getProxy().getGrid()
                .postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
        } catch (GridAccessException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public void setInventorySlotContents(int aIndex, ItemStack aStack) {
        super.setInventorySlotContents(aIndex, aStack);
        if (aIndex >= getPatternCount()) return;
        onPatternChange(aIndex, aStack);
        needPatternSync = true;
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Override
    public void setCustomName(String name) {
        customName = name;
    }

    @Override
    public Optional<IDualInputInventory> getFirstNonEmptyInventory() {
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
            if (slot != null && !slot.isEmpty()) return Optional.of(slot);
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsFluids() {
        return this.supportFluids;
    }

    @Override
    public List<ItemStack> getItemsForHoloGlasses() {
        List<ItemStack> list = new ArrayList<>();
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
            if (slot == null) continue;
            if (slot.getPatternDetails() == null) continue;

            IAEItemStack[] outputs = slot.getPatternDetails()
                .getCondensedOutputs();
            list.add(outputs[0].getItemStack());
        }
        return list;
    }

    public void doublePatterns(int val) {
        boolean fast = (val & 1) != 0;
        boolean backwards = (val & 2) != 0;
        CraftingGridCache.pauseRebuilds();
        try {
            IInventory patterns = this.getPatterns();
            TileEntity te = this.getTileEntity();
            for (int i = 0; i < getPatternCount(); i++) {
                ItemStack stack = patterns.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ICraftingPatternItem cpi) {
                    ICraftingPatternDetails details = cpi.getPatternForItem(stack, te.getWorldObj());
                    if (details != null && !details.isCraftable()) {
                        int max = backwards ? PatternMultiplierHelper.getMaxBitDivider(details)
                            : PatternMultiplierHelper.getMaxBitMultiplier(details);
                        if (max > 0) {
                            ItemStack copy = stack.copy();
                            PatternMultiplierHelper
                                .applyModification(copy, (fast ? Math.min(3, max) : 1) * (backwards ? -1 : 1));
                            patterns.setInventorySlotContents(i, copy);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        CraftingGridCache.unpauseRebuilds();
    }

    @Override
    public String[] getDescription() {
        // #tr tooltip.gtneverreset.crafting_input.type
        // # Machine Type: {\\YELLOW}CRIB{\\RESET}
        // # zh_CN 机器类型：{\\YELLOW}CRIB{\\RESET}
        String type = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.type");
        // #tr tooltip.gtneverreset.crafting_input.item_input
        // # Supplies multiblocks with ingredients from ME crafting jobs
        // # zh_CN 为多方块机器接收 ME 合成任务的配方原料
        String itemInput = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.item_input");
        // #tr tooltip.gtneverreset.crafting_input.tier
        // # Hatch tier: %s
        // # zh_CN 仓室等级：%s
        String tier = StatCollector.translateToLocalFormatted(
            "tooltip.gtneverreset.crafting_input.tier",
            TIER_COLORS[supportFluids ? 10 : 6] + VN[supportFluids ? 10 : 6]);
        // #tr tooltip.gtneverreset.crafting_input.processes
        // # Stores patterns and buffers their delivered ingredients separately
        // # zh_CN 存放样板，并分别缓存各样板收到的原料
        String processes = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.processes");
        // #tr tooltip.gtneverreset.crafting_input.supports_fluids
        // # Supports fluid ingredients; use Ultimate Encoded Patterns for fluid recipes
        // # zh_CN 支持流体原料；流体配方使用终极编码样板
        String supportsFluids = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.supports_fluids");
        // #tr tooltip.gtneverreset.crafting_input.no_fluids
        // # Item ingredients only; fluid patterns are not supported
        // # zh_CN 仅接收物品原料，不支持流体样板
        String noFluids = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.no_fluids");
        // #tr tooltip.gtneverreset.crafting_input.connection
        // # Wire cutters: switch between front-only and all-side ME connections
        // # zh_CN 剪线钳右键：切换仅正面或所有面的 ME 连接
        String connection = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.connection");
        // #tr tooltip.gtneverreset.crafting_input.ignores_hatches
        // # Pattern recipes do not draw ingredients from other input buses or hatches
        // # zh_CN 样板配方不会使用其他输入总线或输入仓的原料
        String ignoresHatches = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.ignores_hatches");
        // #tr tooltip.gtneverreset.crafting_input.ignores_patterns
        // # Patterns keep separate ingredients; manual input slots are shared
        // # zh_CN 不同样板的原料互不混用；手动物品槽由所有样板共用
        String ignoresPatterns = StatCollector.translateToLocal("tooltip.gtneverreset.crafting_input.ignores_patterns");
        // #tr tooltip.gtneverreset.crafting_input.capacity
        // # Pattern slots: %s; requires ME power and one channel
        // # zh_CN 样板槽：%s 个；需要 ME 供电和一个频道
        String capacity = StatCollector
            .translateToLocalFormatted("tooltip.gtneverreset.crafting_input.capacity", getPatternCount());
        return PatternMEOutput.describe(
            new String[] { type, itemInput, tier, processes, capacity, supportFluids ? supportsFluids : noFluids,
                ignoresHatches, ignoresPatterns, connection });
    }

    /** Specialized inventories can supply their own persistent refund mechanism. */
    protected boolean usesDefaultPatternRefunds() {
        return true;
    }

    private void retryPatternRefunds(int limit) {
        int attempts = Math.min(limit, pendingPatternRefunds.size());
        while (attempts-- > 0) {
            PatternSlot<SuperMTEHatchCraftingInputME> slot = pendingPatternRefunds.remove(0);
            try {
                slot.refund(getProxy(), getRequest(), false);
            } catch (GridAccessException ignored) {}
            if (!slot.isEmpty()) pendingPatternRefunds.add(slot);
            markDirty();
        }
    }

    private void writePatternRefunds(NBTTagCompound tag, boolean includeActive) {
        NBTTagList list = new NBTTagList();
        List<PatternSlot<SuperMTEHatchCraftingInputME>> slots = new ArrayList<>(pendingPatternRefunds);
        if (includeActive) {
            for (PatternSlot<SuperMTEHatchCraftingInputME> slot : internalInventory) {
                if (slot != null) slots.add(slot);
            }
        }
        for (PatternSlot<SuperMTEHatchCraftingInputME> slot : slots) {
            if (!slot.isEmpty()) list.appendTag(slot.writeToNBT(new NBTTagCompound()));
        }
        if (list.tagCount() == 0) tag.removeTag("patternInputRefunds");
        else tag.setTag("patternInputRefunds", list);
    }

    // Each pattern slot in the crafting input hatch has its own internal inventory
    public static class PatternSlot<P extends IMetaTileEntity & IDualInputHatch>
        implements IDualInputInventoryWithPattern {

        protected final P parentMTE;
        protected final ItemStack pattern;
        @Nullable
        protected final ICraftingPatternDetails patternDetails;
        protected final GTUtility.ItemId patternItemId;

        protected final List<ItemStack> itemInventory;
        protected final List<FluidStack> fluidInventory;

        public PatternSlot(ItemStack pattern, P parent) {
            this(pattern, null, parent);
        }

        public PatternSlot(ItemStack pattern, NBTTagCompound nbt, P parent) {
            this(
                pattern,
                nbt,
                parent,
                ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(
                    pattern,
                    parent.getBaseMetaTileEntity()
                        .getWorld()));
        }

        protected PatternSlot(ItemStack pattern, NBTTagCompound nbt, P parent,
            @Nullable ICraftingPatternDetails details) {
            this.pattern = pattern;
            this.parentMTE = parent;
            this.patternDetails = details;
            this.itemInventory = new ArrayList<>();
            this.fluidInventory = new ArrayList<>();
            this.patternItemId = GTUtility.ItemId.create(pattern);

            if (nbt == null) return;
            NBTTagList inv = nbt.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < inv.tagCount(); i++) {
                NBTTagCompound tagItemStack = inv.getCompoundTagAt(i);
                ItemStack item = GTUtility.loadItem(tagItemStack);
                if (item != null) {
                    if (item.stackSize > 0) {
                        itemInventory.add(item);
                    }
                } else {
                    GTLoggers.GT_FML_LOGGER.warn(
                        "An error occurred while loading contents of ME Crafting Input Bus. This item has been voided: "
                            + tagItemStack);
                }
            }
            NBTTagList fluidInv = nbt.getTagList("fluidInventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < fluidInv.tagCount(); i++) {
                NBTTagCompound tagFluidStack = fluidInv.getCompoundTagAt(i);
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(tagFluidStack);
                if (fluid != null) {
                    if (fluid.amount > 0) {
                        fluidInventory.add(fluid);
                    }
                } else {
                    GTLoggers.GT_FML_LOGGER.warn(
                        "An error occurred while loading contents of ME Crafting Input Bus. This fluid has been voided: "
                            + tagFluidStack);
                }
            }
        }

        public boolean hasChanged(ItemStack newPattern, World world) {
            return newPattern == null || patternDetails == null
                || !(newPattern.getItem() instanceof ICraftingPatternItem)
                || (!ItemStack.areItemStacksEqual(pattern, newPattern) && !this.patternDetails
                    .equals(((ICraftingPatternItem) newPattern.getItem()).getPatternForItem(newPattern, world)));
        }

        public void updateSlotItems() {
            for (int i = itemInventory.size() - 1; i >= 0; i--) {
                ItemStack itemStack = itemInventory.get(i);
                if (itemStack == null || itemStack.stackSize <= 0) {
                    itemInventory.remove(i);
                }
            }
        }

        public void updateSlotFluids() {
            for (int i = fluidInventory.size() - 1; i >= 0; i--) {
                FluidStack fluidStack = fluidInventory.get(i);
                if (fluidStack == null || fluidStack.amount <= 0) {
                    fluidInventory.remove(i);
                }
            }
        }

        public boolean isItemEmpty() {
            updateSlotItems();
            return itemInventory.isEmpty();
        }

        public boolean isFluidEmpty() {
            updateSlotFluids();
            return fluidInventory.isEmpty();
        }

        @Override
        public boolean isEmpty() {
            return isItemEmpty() && isFluidEmpty();
        }

        @Override
        public ItemStack[] getItemInputs() {
            if (isItemEmpty()) return GTValues.emptyItemStackArray;
            return itemInventory.toArray(new ItemStack[0]);
        }

        @Override
        public FluidStack[] getFluidInputs() {
            if (isEmpty()) return GTValues.emptyFluidStackArray;
            return fluidInventory.toArray(new FluidStack[0]);
        }

        /** Full buffered quantities for diagnostics; specialized slots can expose long reserves beyond recipe views. */
        public List<IAEItemStack> getStoredItems() {
            updateSlotItems();
            return itemInventory.stream()
                .map(AEItemStack::create)
                .collect(Collectors.toList());
        }

        public List<IAEFluidStack> getStoredFluids() {
            updateSlotFluids();
            return fluidInventory.stream()
                .map(AEFluidStack::create)
                .collect(Collectors.toList());
        }

        @Nullable
        public ICraftingPatternDetails getPatternDetails() {
            return patternDetails;
        }

        @Override
        public GTDualInputPattern getPatternInputs() {
            GTDualInputPattern dualInputs = new GTDualInputPattern();

            ItemStack[] inputItems = this.parentMTE.getSharedItems();
            FluidStack[] inputFluids = GTValues.emptyFluidStackArray;

            if (patternDetails != null) {
                for (IAEStack<?> singleInput : patternDetails.getAEInputs()) {
                    if (singleInput == null) continue;
                    if (singleInput instanceof IAEItemStack ais) {
                        inputItems = ArrayUtils.addAll(inputItems, ais.getItemStack());
                    } else if (singleInput instanceof IAEFluidStack ifs) {
                        inputFluids = ArrayUtils.addAll(inputFluids, ifs.getFluidStack());
                    }
                }
            }

            dualInputs.inputItems = inputItems;
            dualInputs.inputFluid = inputFluids;
            return dualInputs;
        }

        /**
         * Try to refund the items and fluids back.
         * <p>
         * Push all the items and fluids back to the AE network first. If shouldDrop is true, the remaining are dropped
         * to the world (the fluids are dropped as AE2FC fluid drop). Otherwise, they are still left in the inventory.
         */
        public void refund(AENetworkProxy proxy, BaseActionSource src, boolean shouldDrop) throws GridAccessException {
            IMEMonitor<IAEItemStack> sg = proxy.getStorage()
                .getItemInventory();
            for (ItemStack itemStack : itemInventory) {
                if (itemStack == null || itemStack.stackSize == 0) continue;
                IAEItemStack rest = Platform.poweredInsert(
                    proxy.getEnergy(),
                    sg,
                    AEApi.instance()
                        .storage()
                        .createItemStack(itemStack),
                    src);
                itemStack.stackSize = rest != null && rest.getStackSize() > 0 ? (int) rest.getStackSize() : 0;

                if (Gregtech.machines.allowCribDropItems && shouldDrop && itemStack.stackSize > 0) {
                    World world = parentMTE.getBaseMetaTileEntity()
                        .getWorld();
                    EntityItem entityItem = new EntityItem(
                        world,
                        parentMTE.getBaseMetaTileEntity()
                            .getXCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        parentMTE.getBaseMetaTileEntity()
                            .getYCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        parentMTE.getBaseMetaTileEntity()
                            .getZCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        GTUtility.copy(itemStack));
                    entityItem.motionX = XSTR_INSTANCE.nextGaussian() * 0.05;
                    entityItem.motionY = XSTR_INSTANCE.nextGaussian() * 0.25;
                    entityItem.motionZ = XSTR_INSTANCE.nextGaussian() * 0.05;
                    world.spawnEntityInWorld(entityItem);

                    itemStack.stackSize = 0;
                }
            }
            IMEMonitor<IAEFluidStack> fsg = proxy.getStorage()
                .getFluidInventory();
            for (FluidStack fluidStack : fluidInventory) {
                if (fluidStack == null || fluidStack.amount == 0) continue;
                IAEFluidStack rest = Platform.poweredInsert(
                    proxy.getEnergy(),
                    fsg,
                    AEApi.instance()
                        .storage()
                        .createFluidStack(fluidStack),
                    src);
                fluidStack.amount = rest != null && rest.getStackSize() > 0 ? (int) rest.getStackSize() : 0;

                if (Gregtech.machines.allowCribDropItems && shouldDrop && fluidStack.amount > 0) {
                    World world = parentMTE.getBaseMetaTileEntity()
                        .getWorld();

                    ItemStack fluidPacketItemStack = ItemFluidPacket.newStack(fluidStack);
                    if (fluidPacketItemStack == null) continue;

                    EntityItem entityItem = new EntityItem(
                        world,
                        parentMTE.getBaseMetaTileEntity()
                            .getXCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        parentMTE.getBaseMetaTileEntity()
                            .getYCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        parentMTE.getBaseMetaTileEntity()
                            .getZCoord() + XSTR_INSTANCE.nextFloat() * 0.8F
                            + 0.1F,
                        fluidPacketItemStack);
                    entityItem.motionX = XSTR_INSTANCE.nextGaussian() * 0.05;
                    entityItem.motionY = XSTR_INSTANCE.nextGaussian() * 0.25;
                    entityItem.motionZ = XSTR_INSTANCE.nextGaussian() * 0.05;
                    world.spawnEntityInWorld(entityItem);
                    fluidStack.amount = 0;
                }
            }
        }

        private void insertItem(IAEItemStack inserted) {
            long remaining = inserted.getStackSize();
            ItemStack template = inserted.getItemStack();
            for (ItemStack existing : itemInventory) {
                if (!GTUtility.areStacksEqual(template, existing)) continue;
                int added = (int) Math.min(remaining, (long) Integer.MAX_VALUE - existing.stackSize);
                existing.stackSize += added;
                remaining -= added;
                if (remaining == 0) return;
            }
            while (remaining > 0) {
                ItemStack stack = template.copy();
                stack.stackSize = (int) Math.min(remaining, Integer.MAX_VALUE);
                itemInventory.add(stack);
                remaining -= stack.stackSize;
            }
        }

        private void insertFluid(IAEFluidStack inserted) {
            long remaining = inserted.getStackSize();
            FluidStack template = inserted.getFluidStack();
            for (FluidStack existing : fluidInventory) {
                if (!GTUtility.areFluidsEqual(template, existing)) continue;
                int added = (int) Math.min(remaining, (long) Integer.MAX_VALUE - existing.amount);
                existing.amount += added;
                remaining -= added;
                if (remaining == 0) return;
            }
            while (remaining > 0) {
                FluidStack stack = template.copy();
                stack.amount = (int) Math.min(remaining, Integer.MAX_VALUE);
                fluidInventory.add(stack);
                remaining -= stack.amount;
            }
        }

        public boolean insertItemsAndFluids(MEInventoryCrafting inventoryCrafting) {
            for (int i = 0; i < inventoryCrafting.getSizeInventory(); ++i) {
                final IAEStack<?> aes = inventoryCrafting.getAEStackInSlot(i);
                if (aes == null) continue;

                if (aes instanceof IAEFluidStack ifs) { // insert fluid
                    insertFluid(ifs);
                } else if (aes instanceof IAEItemStack ais) { // insert item
                    insertItem(ais);
                }
            }
            return true;
        }

        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setTag("pattern", pattern.writeToNBT(new NBTTagCompound()));

            NBTTagList itemInventoryNbt = new NBTTagList();
            for (ItemStack itemStack : this.itemInventory) {
                itemInventoryNbt.appendTag(GTUtility.saveItem(itemStack));
            }
            nbt.setTag("inventory", itemInventoryNbt);

            NBTTagList fluidInventoryNbt = new NBTTagList();
            for (FluidStack fluidStack : fluidInventory) {
                fluidInventoryNbt.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
            }
            nbt.setTag("fluidInventory", fluidInventoryNbt);

            return nbt;
        }
    }
}
