package com.xyp.gtneverreset.common.machine.hatch;

import static gregtech.api.enums.GTValues.TIER_COLORS;
import static gregtech.api.enums.GTValues.VN;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_ME_CRAFTING_INPUT_SLAVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;
import com.xyp.gtneverreset.utils.machine.PatternMEOutput;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IDataCopyable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.modularui2.ProxiedMteGui;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatchWithPattern;
import gregtech.common.tileentities.machines.IDualInputInventory;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public class SuperMTEHatchCraftingInputSlave extends MTEHatchInputBus
    implements IDualInputHatchWithPattern, IDataCopyable {

    public static final String COPIED_DATA_IDENTIFIER = "craftingInputProxy";
    private SuperMTEHatchCraftingInputME master; // use getMaster() to access
    private int masterX, masterY, masterZ;
    private boolean masterSet = false; // indicate if values of masterX, masterY, masterZ are valid
    private boolean reverseRecipes = false; // if true, the slave will show the recipes in reverse order compared to the
                                            // master CRIB

    public SuperMTEHatchCraftingInputSlave(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 11, 0, null);
        disableSort = true;
    }

    public SuperMTEHatchCraftingInputSlave(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
        disableSort = true;
    }

    @Override
    protected boolean useMui2() {
        return false;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new SuperMTEHatchCraftingInputSlave(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return getTexturesInactive(aBaseTexture);
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY_ME_CRAFTING_INPUT_SLAVE) };
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        super.onPostTick(aBaseMetaTileEntity, aTimer);
        if (aBaseMetaTileEntity.isServerSide() && aTimer % 100 == 0) {
            if (getMaster() != null || !masterSet) {
                aBaseMetaTileEntity.tryDisableTicking();
            } else if (trySetMasterFromCoord(masterX, masterY, masterZ) != null) {
                aBaseMetaTileEntity.tryDisableTicking();
            }
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (master != null) master.removeProxyHatch(this);
        master = null;
        masterSet = false;
        if (aNBT.hasKey("master")) {
            NBTTagCompound masterNBT = aNBT.getCompoundTag("master");
            masterX = masterNBT.getInteger("x");
            masterY = masterNBT.getInteger("y");
            masterZ = masterNBT.getInteger("z");
            masterSet = true;
        }
        reverseRecipes = aNBT.getBoolean("reverseRecipes");
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (masterSet) {
            NBTTagCompound masterNBT = new NBTTagCompound();
            masterNBT.setInteger("x", masterX);
            masterNBT.setInteger("y", masterY);
            masterNBT.setInteger("z", masterZ);
            aNBT.setTag("master", masterNBT);
        }
        aNBT.setBoolean("reverseRecipes", reverseRecipes);
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public String[] getInfoData() {
        var ret = new ArrayList<String>();
        if (getMaster() != null) {
            ret.add(
                StatCollector.translateToLocalFormatted(
                    "GT5U.infodata.hatch.crafting_input_slave.linked_to",
                    masterX,
                    masterY,
                    masterZ));
            ret.addAll(Arrays.asList(getMaster().getInfoData()));
        } else ret.add(StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_slave.not_linked_to"));
        ret.add(
            StatCollector.translateToLocalFormatted(
                "GT5U.infodata.hatch.crafting_input_slave.reverseRecipes",
                reverseRecipes ? "on" : "off"));
        return ret.toArray(new String[0]);
    }

    public SuperMTEHatchCraftingInputME getMaster() {
        if (master == null) return null;
        if (!master.isValid() || master.getBaseMetaTileEntity()
            .getWorld() != getBaseMetaTileEntity().getWorld()) {
            master = null;
            getBaseMetaTileEntity().enableTicking();
        }
        return master;
    }

    /** Notifies this mirror's controllers when the shared ME output cache gains space. */
    public void notifyMEOutputSpaceChanged() {
        notifyWatchers();
    }

    @Override
    public byte getTierForStructure() {
        return getMaster() == null ? super.getTierForStructure() : getMaster().getTierForStructure();
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

    @Override
    public Iterator<SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME>> inventories() {
        return getMaster() != null ? (reverseRecipes ? getMaster().inventoriesReversed() : getMaster().inventories())
            : Collections.emptyIterator();
    }

    @Override
    public Optional<IDualInputInventory> getFirstNonEmptyInventory() {
        Iterator<SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME>> inputs = inventories();
        while (inputs.hasNext()) {
            IDualInputInventory input = inputs.next();
            if (!input.isEmpty()) return Optional.of(input);
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsFluids() {
        return getMaster() != null && getMaster().supportsFluids();
    }

    @Override
    public ItemStack[] getSharedItems() {
        return getMaster() != null ? getMaster().getSharedItems() : GTValues.emptyItemStackArray;
    }

    /**
     * Schedules a recipe check for every controller using this mirror after the master accepts ingredients.
     * The master calls every linked mirror before clearing its pending-input flag, so controllers cannot consume
     * another mirror's notification. This also works while this mirror's own ticking is disabled.
     */
    public void onParentInvChange() {
        notifyWatchers();
    }

    public SuperMTEHatchCraftingInputME trySetMasterFromCoord(int x, int y, int z) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!base.isServerSide() || !base.getWorld()
            .blockExists(x, y, z)) return null;
        var tileEntity = base.getWorld()
            .getTileEntity(x, y, z);
        if (tileEntity == null) return null;
        if (!(tileEntity instanceof IGregTechTileEntity GTTE)) return null;
        if (!(GTTE.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputME newMaster)) return null;
        if (!acceptsMaster(newMaster)) return null;
        if (master != newMaster) {
            if (master != null) master.removeProxyHatch(this);
            master = newMaster;
            master.addProxyHatch(this);
        }
        masterX = x;
        masterY = y;
        masterZ = z;
        masterSet = true;
        markDirty();
        notifyWatchers();
        return master;
    }

    protected boolean acceptsMaster(SuperMTEHatchCraftingInputME master) {
        return true;
    }

    private boolean tryLinkDataStick(EntityPlayer aPlayer) {
        ItemStack dataStick = aPlayer.inventory.getCurrentItem();

        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            return false;
        }
        if (!ItemStackNBT.getString(dataStick, "type")
            .equals("CraftingInputBuffer")) {
            return false;
        }

        NBTTagCompound nbt = dataStick.stackTagCompound;
        if (nbt.hasKey("dimension")
            && nbt.getInteger("dimension") != getBaseMetaTileEntity().getWorld().provider.dimensionId) {
            // #tr chat.gtnr.pattern_link.wrong_dimension
            // # The pattern input is in another dimension
            // # zh_CN 样板输入主仓位于其他维度
            GTUtility.sendChatTrans(aPlayer, "chat.gtnr.pattern_link.wrong_dimension");
            return true;
        }
        int x = nbt.getInteger("x");
        int y = nbt.getInteger("y");
        int z = nbt.getInteger("z");
        if (trySetMasterFromCoord(x, y, z) != null) {
            // #tr chat.gtnr.pattern_link.success
            // # Pattern input link established
            // # zh_CN 样板输入连接成功
            GTUtility.sendChatTrans(aPlayer, "chat.gtnr.pattern_link.success");
            return true;
        }
        // #tr chat.gtnr.pattern_link.failed
        // # Pattern input link failed; check the loaded master and mirror type
        // # zh_CN 样板输入连接失败，请检查主仓是否已加载且与镜像类型匹配
        GTUtility.sendChatTrans(aPlayer, "chat.gtnr.pattern_link.failed");
        return true;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP player)) {
            return false;
        }
        if (tryLinkDataStick(aPlayer)) {
            return true;
        }
        var master = getMaster();
        if (master != null) {
            if (aBaseMetaTileEntity.isServerSide()) {
                ProxiedMteGui.open(master, player);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onLeftclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!(aPlayer instanceof EntityPlayerMP)) return;

        ItemStack dataStick = aPlayer.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) return;
        var master = getMaster();
        if (master == null) {
            // #tr chat.gtnr.pattern_link.unlinked_copy
            // # This mirror has no pattern input link to copy
            // # zh_CN 此镜像尚未连接样板输入主仓，无法复制连接信息
            GTUtility.sendChatTrans(aPlayer, "chat.gtnr.pattern_link.unlinked_copy");
            return;
        }

        master.saveToDataStick(aPlayer, dataStick);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (master != null) master.removeProxyHatch(this);
    }

    @Override
    public String getCopiedDataIdentifier(EntityPlayer player) {
        return COPIED_DATA_IDENTIFIER;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, NBTTagCompound nbt) {
        if (!getBaseMetaTileEntity().isServerSide() || nbt == null
            || !COPIED_DATA_IDENTIFIER.equals(nbt.getString("type"))) return false;
        if (nbt.hasKey("master")) {
            NBTTagCompound masterNBT = nbt.getCompoundTag("master");
            trySetMasterFromCoord(masterNBT.getInteger("x"), masterNBT.getInteger("y"), masterNBT.getInteger("z"));
        }
        reverseRecipes = nbt.getBoolean("reverseRecipes");
        markDirty();
        notifyWatchers();
        return true;
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer player) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", COPIED_DATA_IDENTIFIER);
        if (masterSet) {
            NBTTagCompound masterNBT = new NBTTagCompound();
            masterNBT.setInteger("x", masterX);
            masterNBT.setInteger("y", masterY);
            masterNBT.setInteger("z", masterZ);
            tag.setTag("master", masterNBT);
        }
        tag.setBoolean("reverseRecipes", reverseRecipes);
        return tag;
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
        currenttip.add(
            StatCollector.translateToLocal(
                tag.getBoolean("linked") ? "GT5U.waila.hatch.crafting_input_slave.linked"
                    : "GT5U.waila.hatch.crafting_input_slave.unlinked"));

        if (tag.hasKey("masterX")) {
            currenttip.add(
                StatCollector.translateToLocalFormatted(
                    "GT5U.waila.hatch.crafting_input_slave.bound_to",
                    tag.getInteger("masterX"),
                    tag.getInteger("masterY"),
                    tag.getInteger("masterZ")));
        }

        currenttip.add(
            StatCollector.translateToLocalFormatted(
                "GT5U.waila.hatch.crafting_input_slave.reverseRecipes",
                tag.getBoolean("reverseRecipes") ? "on" : "off"));

        if (tag.hasKey("masterName")) {
            currenttip.add(EnumChatFormatting.GOLD + tag.getString("masterName") + EnumChatFormatting.RESET);
        }

        super.getWailaBody(itemStack, currenttip, accessor, config);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {

        tag.setBoolean("linked", getMaster() != null);
        tag.setBoolean("reverseRecipes", reverseRecipes);
        if (masterSet) {
            tag.setInteger("masterX", masterX);
            tag.setInteger("masterY", masterY);
            tag.setInteger("masterZ", masterZ);
        }
        if (getMaster() != null) tag.setString("masterName", getMaster().getName());

        super.getWailaNBTData(player, tile, tag, world, x, y, z);
    }

    @Override
    public List<ItemStack> getItemsForHoloGlasses() {
        return getMaster() != null ? getMaster().getItemsForHoloGlasses() : null;
    }

    private void toggleReverseRecipes() {
        reverseRecipes = !reverseRecipes;
        markDirty();
        notifyWatchers();
    }

    @Override
    public final void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (getBaseMetaTileEntity().isServerSide()) {
            toggleReverseRecipes();
            GTUtility.sendChatTrans(aPlayer, "chat.proxy.reverse." + reverseRecipes);
        }
        // not calling super.onScrewdriverRightClick, because input filter is irrelevant for a proxy hatch
    }

    @Override
    public String[] getDescription() {
        // #tr tooltip.gtnr.crafting_input_mirror.proxy
        // # Shares a linked Crafting Input Buffer/Bus with another multiblock
        // # zh_CN 让其他多方块机器共用已连接的样板输入总成或总线
        String proxy = StatCollector.translateToLocal("tooltip.gtnr.crafting_input_mirror.proxy");
        // #tr tooltip.gtnr.crafting_input_mirror.tier
        // # Hatch tier: %s
        // # zh_CN 仓室等级：%s
        String tier = StatCollector
            .translateToLocalFormatted("tooltip.gtnr.crafting_input_mirror.tier", TIER_COLORS[11] + VN[11]);
        // #tr tooltip.gtnr.crafting_input_mirror.share
        // # Uses the linked master's pattern inputs and product output caches
        // # zh_CN 共用主仓的样板原料与产物输出缓存
        String share = StatCollector.translateToLocal("tooltip.gtnr.crafting_input_mirror.share");
        // #tr tooltip.gtnr.crafting_input_mirror.link
        // # Data Stick: left-click the master, then right-click this hatch in the same dimension
        // # zh_CN 手持数据棒左键点击主仓，再右键点击同维度内的此仓完成连接
        String link = StatCollector.translateToLocal("tooltip.gtnr.crafting_input_mirror.link");
        return PatternMEOutput.describe(new String[] { proxy, tier, share, link });
    }
}
