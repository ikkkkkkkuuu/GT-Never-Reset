package com.xyp.gtneverreset.compat.cropsnh;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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

public final class NetherStarFlowerCrop extends CropCard {

    public static final String ID = GTNeverReset.MODID + ":nether_star_flower";
    private static final int GROWTH_STAGES = 4;
    private final SubSoilRequirement subSoil;
    @SideOnly(Side.CLIENT)
    private IIcon[] sprites;

    public NetherStarFlowerCrop() {
        super(GTNeverReset.MODID, "nether_star_flower", 0x75618C, 0xFFF5CA);
        addDrop(new ItemStack(Items.nether_star), 10_000);
        addDuplicationCatalyst(new ItemStack(Items.nether_star));
        addLikedBiomes(BiomeDictionary.Type.NETHER, BiomeDictionary.Type.DEAD);
        // #tr cropsnh_growthReq.subSoil.gtneverreset.beacon
        // # Beacon
        // # zh_CN 信标
        subSoil = SubSoilRequirement.get("gtneverreset.beacon")
            .addBlock(new BlockWithMeta(Blocks.beacon, 0));
        addGrowthRequirement(subSoil);
    }

    @Override
    public String getUnlocalizedName() {
        // #tr gtnr.crop.nether_star_flower
        // # Nether Star Flower
        // # zh_CN 下界星花
        return "gtnr.crop.nether_star_flower";
    }

    @Override
    public String getCreator() {
        return GTNeverReset.NAME;
    }

    @Override
    public int getTier() {
        return 12;
    }

    @Override
    public int getMachineBreedingRecipeTier() {
        return VoltageIndex.EV;
    }

    @Override
    public int getGrowthDuration() {
        return 7_200;
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
            sprites[stage] = register.registerIcon(GTNeverReset.MODID + ":crops/nether_star_flower/" + (stage + 1));
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
