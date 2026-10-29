package com.xyp.gtneverreset.common.machine.basicMachine;

import static gregtech.api.enums.GTValues.V;
import static gregtech.api.enums.GTValues.VN;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;
import com.xyp.gtneverreset.common.item.items.StableEndCrystalItem;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.tooltip.TooltipHelper;
import gregtech.common.misc.WirelessNetworkManager;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class EndCrystalResonator extends MTETieredMachineBlock {

    private static final int UPDATE_INTERVAL = 20;
    private static final ITexture[] FRONTS = createFrontTextures();

    private UUID ownerId;
    private long pendingEU;
    private boolean crystalPresent;

    public EndCrystalResonator(int id, String name, int tier) {
        super(id, name, "", tier, 0, new String[0]);
    }

    private EndCrystalResonator(String name, int tier, ITexture[][][] textures) {
        super(name, tier, 0, new String[0], textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new EndCrystalResonator(mName, mTier, mTextures);
    }

    public static long generationForTier(int tier) {
        return V[tier] * (tier == 1 ? 32L : 128L);
    }

    @Override
    public String getLocalName() {
        // #tr gtnr.crystal_resonator.name
        // # End Crystal Resonator (%s)
        // # zh_CN 末晶谐振器 (%s)
        return StatCollector.translateToLocalFormatted("gtnr.crystal_resonator.name", VN[mTier]);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr tooltip.gtnr.crystal_resonator.generation
            // # Generates EU from a Stable End Crystal without pollution
            // # zh_CN 利用稳定末影水晶发电，无污染
            StatCollector.translateToLocal("tooltip.gtnr.crystal_resonator.generation"),
            // #tr tooltip.gtnr.crystal_resonator.crystal
            // # Requires a Stable End Crystal directly above; it is not consumed
            // # zh_CN 正上方一格需要稳定末影水晶；运行时不消耗水晶
            StatCollector.translateToLocal("tooltip.gtnr.crystal_resonator.crystal"),
            // #tr tooltip.gtnr.crystal_resonator.voltage
            // # Rated voltage: %s
            // # zh_CN 额定电压：%s
            StatCollector.translateToLocalFormatted(
                "tooltip.gtnr.crystal_resonator.voltage",
                TooltipHelper.voltageText(V[mTier])),
            // #tr tooltip.gtnr.crystal_resonator.amperage
            // # Equivalent current: %s A
            // # zh_CN 等效电流：%s A
            StatCollector.translateToLocalFormatted(
                "tooltip.gtnr.crystal_resonator.amperage",
                TooltipHelper.ampText(generationForTier(mTier) / V[mTier])),
            // #tr tooltip.gtnr.crystal_resonator.rate
            // # Generation: %s
            // # zh_CN 发电量：%s
            StatCollector.translateToLocalFormatted(
                "tooltip.gtnr.crystal_resonator.rate",
                TooltipHelper.euRateText(generationForTier(mTier))),
            // #tr tooltip.gtnr.crystal_resonator.wireless
            // # Deposits EU into the owner's GT wireless network every second
            // # zh_CN 每秒将 EU 存入机主所属的 GT 无线电网
            StatCollector.translateToLocal("tooltip.gtnr.crystal_resonator.wireless"),
            // #tr tooltip.gtnr.crystal_resonator.placement
            // # Right-click the top with a Stable End Crystal to place it
            // # zh_CN 手持稳定末影水晶右键顶面放置
            StatCollector.translateToLocal("tooltip.gtnr.crystal_resonator.placement") };
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        boolean generating = getBaseMetaTileEntity().isActive();
        tag.setBoolean("resonatorGenerating", generating);
        tag.setLong("resonatorGeneration", generating ? generationForTier(mTier) : 0L);
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey("resonatorGeneration")) return;
        if (tag.getBoolean("resonatorGenerating")) {
            // #tr waila.gtnr.crystal_resonator.generating
            // # Generating power
            // # zh_CN 正在发电
            lines.add(StatCollector.translateToLocal("waila.gtnr.crystal_resonator.generating"));
        } else {
            // #tr waila.gtnr.crystal_resonator.idle
            // # Not generating power
            // # zh_CN 未在发电
            lines.add(StatCollector.translateToLocal("waila.gtnr.crystal_resonator.idle"));
        }
        lines.add(
            StatCollector.translateToLocalFormatted(
                "tooltip.gtnr.crystal_resonator.rate",
                TooltipHelper.euRateText(tag.getLong("resonatorGeneration"))));
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (!tile.isServerSide()) return;
        bindOwner(tile);
        crystalPresent = hasCrystalAbove(tile);
    }

    private void bindOwner(IGregTechTileEntity tile) {
        UUID placedBy = tile.getOwnerUuid();
        if (ownerId != null || placedBy == null || placedBy.equals(GTUtility.defaultUuid)) return;
        ownerId = placedBy;
        WirelessNetworkManager.strongCheckOrAddUser(ownerId);
        markDirty();
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if (tick % UPDATE_INTERVAL == 0) {
            bindOwner(tile);
            crystalPresent = hasCrystalAbove(tile);
        }
        boolean generating = crystalPresent && tile.isAllowedToWork();
        tile.setActive(generating);
        if (generating) {
            pendingEU += Math.min(generationForTier(mTier), Long.MAX_VALUE - pendingEU);
            markDirty();
        }
        if (tick % UPDATE_INTERVAL == 0) flushEnergy();
    }

    private static boolean hasCrystalAbove(IGregTechTileEntity tile) {
        int x = tile.getXCoord(), y = tile.getYCoord() + 1, z = tile.getZCoord();
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        for (Object entity : tile.getWorld()
            .getEntitiesWithinAABB(StableEndCrystalEntity.class, box)) {
            StableEndCrystalEntity crystal = (StableEndCrystalEntity) entity;
            if (!crystal.isDead && MathHelper.floor_double(crystal.posX) == x
                && MathHelper.floor_double(crystal.posY) == y
                && MathHelper.floor_double(crystal.posZ) == z) return true;
        }
        return false;
    }

    private void flushEnergy() {
        if (ownerId != null && pendingEU > 0 && WirelessNetworkManager.addEUToGlobalEnergyMap(ownerId, pendingEU)) {
            pendingEU = 0;
            markDirty();
        }
    }

    @Override
    public void onRemoval() {
        if (getBaseMetaTileEntity().isServerSide()) flushEnergy();
        super.onRemoval();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        tag.setLong("resonatorPendingEU", pendingEU);
        if (ownerId != null) tag.setString("resonatorOwner", ownerId.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        pendingEU = Math.max(0, tag.getLong("resonatorPendingEU"));
        ownerId = null;
        if (tag.hasKey("resonatorOwner", 8)) {
            try {
                ownerId = UUID.fromString(tag.getString("resonatorOwner"));
            } catch (IllegalArgumentException ignored) {}
        }
        crystalPresent = false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player, ForgeDirection side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof StableEndCrystalItem item)) return false;
        boolean placed = item.onItemUse(
            held,
            player,
            tile.getWorld(),
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord(),
            side.ordinal(),
            hitX,
            hitY,
            hitZ);
        if (placed && tile.isServerSide()) crystalPresent = hasCrystalAbove(tile);
        return placed;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isFacingValid(ForgeDirection side) {
        return side != ForgeDirection.UP && side != ForgeDirection.DOWN && side != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public boolean isEnetOutput() {
        return false;
    }

    @Override
    public boolean isValidSlot(int slot) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return new ITexture[0][][];
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1];
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, FRONTS[mTier - 1] };
    }

    private static ITexture[] createFrontTextures() {
        String[] tiers = { "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max" };
        ITexture[] textures = new ITexture[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            textures[i] = TextureFactory.of(
                Textures.BlockIcons
                    .custom(GTNeverReset.MODID, "basicmachines/crystal_resonator/" + tiers[i] + "_front"));
        }
        return textures;
    }
}
