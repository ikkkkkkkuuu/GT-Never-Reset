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
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoltageIndex;

public final class SunnariumFlowerCrop extends CropCard {

    public static final String ID = GTNeverReset.MODID + ":sunnarium_flower";
    private static final int GROWTH_STAGES = 4;
    private final SubSoilRequirement subSoil;
    @SideOnly(Side.CLIENT)
    private IIcon[] sprites;

    public SunnariumFlowerCrop() {
        super(GTNeverReset.MODID, "sunnarium_flower", 0xD99419, 0xFFF273);
        addDrop(Materials.Sunnarium.getDust(1), 10_000);
        addDuplicationCatalyst(Materials.Sunnarium.getDust(1));
        addLikedBiomes(BiomeDictionary.Type.HOT, BiomeDictionary.Type.PLAINS);
        ItemStack substrate = Materials.Sunnarium.getBlocks(1);
        // #tr cropsnh_growthReq.subSoil.gtneverreset.sunnarium
        // # Sunnarium Block
        // # zh_CN 阳光化合物块
        subSoil = SubSoilRequirement.get("gtneverreset.sunnarium")
            .addBlock(new BlockWithMeta(Block.getBlockFromItem(substrate.getItem()), substrate.getItemDamage()));
        addGrowthRequirement(subSoil);
    }

    @Override
    public String getUnlocalizedName() {
        // #tr gtnr.crop.sunnarium_flower
        // # Sunnarium Flower
        // # zh_CN 阳光花
        return "gtnr.crop.sunnarium_flower";
    }

    @Override
    public String getCreator() {
        return GTNeverReset.NAME;
    }

    @Override
    public int getTier() {
        return 16;
    }

    @Override
    public int getMachineBreedingRecipeTier() {
        return VoltageIndex.IV;
    }

    @Override
    public int getGrowthDuration() {
        return 9_600;
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
            sprites[stage] = register.registerIcon(GTNeverReset.MODID + ":crops/sunnarium_flower/" + (stage + 1));
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
