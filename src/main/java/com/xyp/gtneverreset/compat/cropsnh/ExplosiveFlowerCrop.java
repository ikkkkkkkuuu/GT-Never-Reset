package com.xyp.gtneverreset.compat.cropsnh;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.BiomeDictionary;

import com.gtnewhorizon.cropsnh.api.BlockWithMeta;
import com.gtnewhorizon.cropsnh.api.CropCard;
import com.gtnewhorizon.cropsnh.api.CropsNHSoilTypes;
import com.gtnewhorizon.cropsnh.api.ICropStickTile;
import com.gtnewhorizon.cropsnh.api.ISeedStats;
import com.gtnewhorizon.cropsnh.api.ISoilList;
import com.gtnewhorizon.cropsnh.crops.abstracts.NHCropCard;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.init.CropsNHItems;
import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.VoltageIndex;

public final class ExplosiveFlowerCrop extends CropCard {

    public static final String ID = GTNeverReset.MODID + ":explosive_flower";
    private static final int GROWTH_STAGES = 4;
    private final SubSoilRequirement subSoil;
    @SideOnly(Side.CLIENT)
    private IIcon[] sprites;

    public ExplosiveFlowerCrop(ItemStack industrialTnt) {
        super(GTNeverReset.MODID, "explosive_flower", 0xA42E28, 0xF3C977);
        addDrop(industrialTnt.copy(), 10_000);
        addLikedBiomes(BiomeDictionary.Type.HOT, BiomeDictionary.Type.DRY);
        // #tr cropsnh_growthReq.subSoil.gtneverreset.industrial_tnt
        // # Industrial TNT
        // # zh_CN 工业 TNT
        subSoil = SubSoilRequirement.get("gtneverreset.industrial_tnt")
            .addBlock(
                new BlockWithMeta(Block.getBlockFromItem(industrialTnt.getItem()), industrialTnt.getItemDamage()));
        addGrowthRequirement(subSoil);
    }

    @Override
    public String getUnlocalizedName() {
        // #tr gtneverreset.crop.explosive_flower
        // # Explosive Flower
        // # zh_CN 炸药花
        return "gtneverreset.crop.explosive_flower";
    }

    @Override
    public String getCreator() {
        return GTNeverReset.NAME;
    }

    @Override
    public int getTier() {
        return 8;
    }

    @Override
    public int getMachineBreedingRecipeTier() {
        return VoltageIndex.MV;
    }

    @Override
    public int getGrowthDuration() {
        return 4_800;
    }

    @Override
    public ISoilList getSoilTypes() {
        return CropsNHSoilTypes.farmland;
    }

    @Override
    public ItemStack getSeedItem(ISeedStats stats) {
        ItemStack seed = new ItemStack(CropsNHItems.genericSeed);
        seed.setTagCompound(NHCropCard.writeNBT(this, stats));
        return seed;
    }

    @Override
    public List<ItemStack> getSoilsForNEI(boolean useCache) {
        return getSoilTypes().getNEIItemList()
            .collect(Collectors.toList());
    }

    @Override
    public List<ItemStack> getSubSoilsForNEI(boolean useCache) {
        return subSoil.getItemsForNEI();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerSprites(IIconRegister register) {
        sprites = new IIcon[GROWTH_STAGES];
        for (int stage = 0; stage < GROWTH_STAGES; stage++) {
            sprites[stage] = register.registerIcon(GTNeverReset.MODID + ":crops/explosive_flower/" + (stage + 1));
        }
    }

    @Override
    public int getSpriteIndex(ICropStickTile tile) {
        return Math
            .max(0, Math.min(GROWTH_STAGES - 1, tile.getGrowthProgress() * (GROWTH_STAGES - 1) / getGrowthDuration()));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getSprite(ICropStickTile tile) {
        return sprites == null ? null : sprites[getSpriteIndex(tile)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon[] getSprites() {
        return sprites;
    }
}
