package com.xyp.gtneverreset.common.machine.basicMachine;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.objects.overclockdescriber.EUOverclockDescriber;
import gregtech.api.objects.overclockdescriber.OverclockDescriber;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class Disassembler extends MTEBasicMachineWithRecipe {

    private static final String[] LEGACY_TIER_COLORS = { "", "\u00a77", "\u00a76", "\u00a7e", "\u00a78", "\u00a7a",
        "\u00a7d", "\u00a7b", "\u00a72", "\u00a74", "\u00a75", "\u00a71\u00a7l", "\u00a7c\u00a7l\u00a7n" };
    private static final ITexture[] OVERLAYS = {
        overlay(BlockIcons.OVERLAY_SIDE_DISASSEMBLER_ACTIVE, BlockIcons.OVERLAY_SIDE_DISASSEMBLER_ACTIVE_GLOW),
        overlay(BlockIcons.OVERLAY_SIDE_DISASSEMBLER, BlockIcons.OVERLAY_SIDE_DISASSEMBLER_GLOW),
        overlay(BlockIcons.OVERLAY_FRONT_DISASSEMBLER_ACTIVE, BlockIcons.OVERLAY_FRONT_DISASSEMBLER_ACTIVE_GLOW),
        overlay(BlockIcons.OVERLAY_FRONT_DISASSEMBLER, BlockIcons.OVERLAY_FRONT_DISASSEMBLER_GLOW),
        overlay(BlockIcons.OVERLAY_TOP_DISASSEMBLER_ACTIVE, BlockIcons.OVERLAY_TOP_DISASSEMBLER_ACTIVE_GLOW),
        overlay(BlockIcons.OVERLAY_TOP_DISASSEMBLER, BlockIcons.OVERLAY_TOP_DISASSEMBLER_GLOW),
        overlay(BlockIcons.OVERLAY_BOTTOM_DISASSEMBLER_ACTIVE, BlockIcons.OVERLAY_BOTTOM_DISASSEMBLER_ACTIVE_GLOW),
        overlay(BlockIcons.OVERLAY_BOTTOM_DISASSEMBLER, BlockIcons.OVERLAY_BOTTOM_DISASSEMBLER_GLOW) };

    public Disassembler(int id, String name, int tier) {
        super(
            id,
            name,
            "",
            tier,
            new String[0],
            GTNRRecipeMaps.DISASSEMBLER_RECIPES,
            1,
            9,
            false,
            false,
            0,
            null,
            SpecialEffects.NONE,
            OVERLAYS,
            null);
    }

    private Disassembler(Disassembler prototype) {
        super(
            prototype.mName,
            prototype.mTier,
            prototype.mDescriptionArray,
            prototype.mRecipes,
            1,
            9,
            false,
            false,
            0,
            prototype.mAmperage,
            prototype.mTextures,
            null,
            SpecialEffects.NONE);
        progressBarTexture = prototype.progressBarTexture;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new Disassembler(this);
    }

    @Override
    public String[] getDescription() {
        // #tr gtnr.disassembler.tier.1.description
        // # Disassembles Machines up to %sLow Voltage
        // # zh_CN 最高可拆解%sLV{\\RESET}机器
        // #tr gtnr.disassembler.tier.2.description
        // # Disassembles Machines up to %sMedium Voltage
        // # zh_CN 最高可拆解%sMV{\\RESET}机器
        // #tr gtnr.disassembler.tier.3.description
        // # Disassembles Machines up to %sHigh Voltage
        // # zh_CN 最高可拆解%sHV{\\RESET}机器
        // #tr gtnr.disassembler.tier.4.description
        // # Disassembles Machines up to %sExtreme Voltage
        // # zh_CN 最高可拆解%sEV{\\RESET}机器
        // #tr gtnr.disassembler.tier.5.description
        // # Disassembles Machines up to %sInsane Voltage
        // # zh_CN 最高可拆解%sIV{\\RESET}机器
        // #tr gtnr.disassembler.tier.6.description
        // # Disassembles Machines up to %sLudicrous Voltage
        // # zh_CN 最高可拆解%sLuV{\\RESET}机器
        // #tr gtnr.disassembler.tier.7.description
        // # Disassembles Machines up to %sZPM Voltage
        // # zh_CN 最高可拆解%sZPM{\\RESET}机器
        // #tr gtnr.disassembler.tier.8.description
        // # Disassembles Machines up to %sUltimate Voltage
        // # zh_CN 最高可拆解%sUV{\\RESET}机器
        // #tr gtnr.disassembler.tier.9.description
        // # Disassembles Machines up to %sUltimate High Voltage
        // # zh_CN 最高可拆解%sUHV{\\RESET}机器
        // #tr gtnr.disassembler.tier.10.description
        // # Disassembles Machines up to %sUltimate Extreme Voltage
        // # zh_CN 最高可拆解%sUEV{\\RESET}机器
        // #tr gtnr.disassembler.tier.11.description
        // # Disassembles Machines up to %sUltimate Insane Voltage
        // # zh_CN 最高可拆解%sUIV{\\RESET}机器
        // #tr gtnr.disassembler.tier.12.description
        // # Disassembles Machines up to %sUltimate Mega Voltage
        // # zh_CN 最高可拆解%sUMV{\\RESET}机器
        String tierDescription = StatCollector.translateToLocalFormatted(mName + ".description", getLegacyTierColor());
        // #tr gtnr.disassembler.assembler_recipes
        // # Can also disassemble most assembler recipes!
        // # zh_CN 也可以拆解大部分组装机合成产物!
        return new String[] { tierDescription, StatCollector.translateToLocal("gtnr.disassembler.assembler_recipes") };
    }

    public String getLegacyTierColor() {
        return LEGACY_TIER_COLORS[mTier];
    }

    @Override
    public boolean allowSelectCircuit() {
        return false;
    }

    @Override
    protected OverclockDescriber createOverclockDescriber() {
        return new EUOverclockDescriber(mTier, mAmperage) {

            @Override
            public boolean canHandle(GTRecipe recipe) {
                return getTier() >= recipe.mSpecialValue && super.canHandle(recipe);
            }
        };
    }

    @Override
    public int checkRecipe(boolean skipOC) {
        ItemStack input = getInputAt(0);
        if (input == null) return DID_NOT_FIND_RECIPE;
        GTRecipe recipe = getRecipeMap().findRecipeQuery()
            .items(input)
            .cachedRecipe(mLastRecipe)
            .filter(
                candidate -> ItemStack.areItemStackTagsEqual(candidate.mInputs[0], input)
                    && candidate.mInputs[0].isItemEqual(input))
            .find();
        if (recipe == null) return DID_NOT_FIND_RECIPE;
        if (input.stackSize < recipe.mInputs[0].stackSize || !overclockDescriber.canHandle(recipe)
            || !canOutput(recipe)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        if (!skipOC) calculateCustomOverclock(recipe);
        input.stackSize -= recipe.mInputs[0].stackSize;
        for (int slot = 0; slot < mOutputItems.length; slot++) mOutputItems[slot] = recipe.getOutput(slot);
        mLastRecipe = recipe;
        return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
    }

    private static ITexture overlay(IIconContainer base, IIconContainer glow) {
        return TextureFactory.of(
            TextureFactory.of(base),
            TextureFactory.builder()
                .addIcon(glow)
                .glow()
                .build());
    }
}
