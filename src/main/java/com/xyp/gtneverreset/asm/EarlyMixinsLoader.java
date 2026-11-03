package com.xyp.gtneverreset.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.xyp.gtneverreset.asm.EarlyMixinsLoader")
public final class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtneverreset.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>(
            List.of(
                "wireless.MixinWirelessTileData",
                "gtnhlib.ServerThreadUtilMixin",
                "minecraft.VanillaFurnaceMixin"));
        mixins.add("minecraft.ItemInWorldManagerMiningMixin");
        mixins.add("minecraft.UniversalWandBlockInteractionMixin");
        mixins.add("minecraft.UniversalWandCombatMixin");
        mixins.add("minecraft.UniversalWandEntityInteractionMixin");
        mixins.add("minecraft.UniversalWandEntityProtectionMixin");
        mixins.add("minecraft.UniversalWandFlightMixin");
        mixins.add("minecraft.UniversalWandFlightSpeedAccessor");
        mixins.add("minecraft.UniversalWandFlightSpeedMixin");
        mixins.add("minecraft.UniversalWandLegacyNoAggroMixin");
        mixins.add("minecraft.UniversalWandLivingAccess");
        mixins.add("minecraft.UniversalWandLivingDropsMixin");
        mixins.add("minecraft.UniversalWandLivingProtectionMixin");
        mixins.add("minecraft.UniversalWandNaturalSpawnMixin");
        mixins.add("minecraft.UniversalWandNoAggroMixin");
        mixins.add("minecraft.UniversalWandPlayerProtectionMixin");
        mixins.add("minecraft.UniversalWandPotionAccessor");
        mixins.add("minecraft.UniversalWandRemovalMixin");
        mixins.add("minecraft.UniversalWandServerPlayerProtectionMixin");
        mixins.add("minecraft.UniversalWandSpawnerActivationMixin");
        mixins.add("minecraft.FoodStatsMiningAccessor");
        if (cpw.mods.fml.relauncher.FMLLaunchHandler.side()
            .isClient())
            mixins.addAll(
                List.of(
                    "client.PlayerControllerMPAccessor",
                    "client.UniversalWandBlockReachMixin",
                    "client.UniversalWandDeathStatusMixin",
                    "client.UniversalWandEntityReachMixin",
                    "client.UniversalWandFlightMovementMixin",
                    "client.UniversalWandRightClickMixin"));
        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
