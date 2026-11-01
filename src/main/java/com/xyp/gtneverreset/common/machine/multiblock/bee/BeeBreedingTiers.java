package com.xyp.gtneverreset.common.machine.multiblock.bee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.config.BeeBreederConfig;

import forestry.api.apiculture.IAlleleBeeSpecies;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.common.items.ItemComb;

public final class BeeBreedingTiers {

    private static final int GENERATIONS_PER_TIER = 3;
    private static final int MAX_DIFFICULTY = (VoltageIndex.UHV - VoltageIndex.HV) * GENERATIONS_PER_TIER;
    private static volatile Map<String, SpeciesDifficulty> speciesDifficulties = Collections.emptyMap();
    private static volatile TierTable cachedTable;

    private BeeBreedingTiers() {}

    public static synchronized void initialize() {
        Map<String, SpeciesDifficulty> species = new HashMap<>();
        for (IAlleleBeeSpecies bee : BeeBreedingHelper.getRegisteredSpecies()) {
            int tier = VoltageIndex.HV;
            for (ItemStack product : bee.getProductChances()
                .keySet()) tier = Math.max(tier, getItemTier(product));
            for (ItemStack specialty : bee.getSpecialtyChances()
                .keySet()) tier = Math.max(tier, getItemTier(specialty));
            species.put(bee.getUID(), new SpeciesDifficulty(toDifficulty(tier)));
        }
        for (BeeBreedingHelper.MutationData mutation : BeeBreedingHelper.getAllMutations()) {
            if (!species.containsKey(mutation.parent1) || !species.containsKey(mutation.parent2)
                || !species.containsKey(mutation.result)
                || !Double.isFinite(mutation.chance)
                || mutation.chance <= 0) {
                continue;
            }
            int tier = VoltageIndex.HV;
            for (BeeBreedingHelper.FoundationRequirement foundation : mutation.foundations) {
                int alternativeTier = VoltageIndex.UHV;
                for (ItemStack alternative : foundation.getAlternatives()) {
                    alternativeTier = Math.min(alternativeTier, getItemTier(alternative));
                }
                tier = Math.max(tier, alternativeTier);
            }
            species.get(mutation.result).routes.add(new Route(mutation.parent1, mutation.parent2, toDifficulty(tier)));
        }
        speciesDifficulties = Collections.unmodifiableMap(species);
        cachedTable = new TierTable(speciesDifficulties, BeeBreederConfig.speciesVoltageOverrides);
    }

    public static int getVoltageTier(String species) {
        String[] entries = BeeBreederConfig.speciesVoltageOverrides;
        TierTable table = cachedTable;
        if (table == null || table.entries != entries) {
            synchronized (BeeBreedingTiers.class) {
                table = cachedTable;
                if (table == null || table.entries != entries) {
                    table = new TierTable(speciesDifficulties, entries);
                    cachedTable = table;
                }
            }
        }
        return table.tiers.getOrDefault(species, VoltageIndex.UHV);
    }

    private static int getItemTier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return VoltageIndex.HV;
        int tier = VoltageIndex.HV;
        if (stack.getItem() instanceof ItemComb) {
            CombType comb = CombType.valueOf(stack.getItemDamage());
            if (comb.voltage != null) tier = Math.max(tier, comb.voltage.ordinal());
        }
        ItemData data = GTOreDictUnificator.getItemData(stack);
        if (data != null && data.mMaterial != null && data.mMaterial.mMaterial != null) {
            tier = Math.max(tier, GTUtility.getTier(data.mMaterial.mMaterial.getProcessingMaterialTierEU()));
        }
        return Math.min(tier, VoltageIndex.UHV);
    }

    private static int toDifficulty(int tier) {
        return (Math.max(VoltageIndex.HV, Math.min(VoltageIndex.UHV, tier)) - VoltageIndex.HV) * GENERATIONS_PER_TIER;
    }

    private static Map<String, Integer> calculateTiers(Map<String, SpeciesDifficulty> species,
        Map<String, Integer> overrides) {
        Map<String, Integer> difficulties = new HashMap<>();
        species.forEach(
            (uid, data) -> difficulties.put(
                uid,
                overrides.containsKey(uid) ? toDifficulty(overrides.get(uid))
                    : data.routes.isEmpty() ? data.floor : Integer.MAX_VALUE));
        // Relax from obtainable parents; cycles without an entry route remain unresolved.
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, SpeciesDifficulty> entry : species.entrySet()) {
                String uid = entry.getKey();
                if (overrides.containsKey(uid)) continue;
                SpeciesDifficulty data = entry.getValue();
                int best = difficulties.get(uid);
                for (Route route : data.routes) {
                    int parents = Math.max(difficulties.get(route.parent1), difficulties.get(route.parent2));
                    if (parents == Integer.MAX_VALUE) continue;
                    int candidate = Math.max(Math.max(data.floor, route.floor), Math.min(MAX_DIFFICULTY, parents + 1));
                    best = Math.min(best, candidate);
                }
                if (best < difficulties.get(uid)) {
                    difficulties.put(uid, best);
                    changed = true;
                }
            }
        } while (changed);
        Map<String, Integer> tiers = new HashMap<>();
        difficulties.forEach(
            (uid, difficulty) -> tiers.put(
                uid,
                difficulty == Integer.MAX_VALUE ? VoltageIndex.UHV
                    : VoltageIndex.HV + difficulty / GENERATIONS_PER_TIER));
        tiers.putAll(overrides);
        return Collections.unmodifiableMap(tiers);
    }

    private static final class SpeciesDifficulty {

        private final int floor;
        private final List<Route> routes = new ArrayList<>();

        private SpeciesDifficulty(int floor) {
            this.floor = floor;
        }
    }

    private record Route(String parent1, String parent2, int floor) {}

    private static final class TierTable {

        private final String[] entries;
        private final Map<String, Integer> tiers;

        private TierTable(Map<String, SpeciesDifficulty> species, String[] entries) {
            this.entries = entries;
            Map<String, Integer> overrides = new HashMap<>();
            if (entries != null) for (String entry : entries) {
                if (entry == null || entry.trim()
                    .isEmpty()) continue;
                int separator = entry.lastIndexOf('=');
                String uid = separator < 0 ? ""
                    : entry.substring(0, separator)
                        .trim();
                int tier = separator < 0 ? -1
                    : switch (entry.substring(separator + 1)
                        .trim()
                        .toUpperCase(Locale.ROOT)) {
                    case "HV" -> VoltageIndex.HV;
                    case "EV" -> VoltageIndex.EV;
                    case "IV" -> VoltageIndex.IV;
                    case "LUV" -> VoltageIndex.LuV;
                    case "ZPM" -> VoltageIndex.ZPM;
                    case "UV" -> VoltageIndex.UV;
                    case "UHV" -> VoltageIndex.UHV;
                    default -> -1;
                };
                if (uid.isEmpty() || tier < 0) {
                    GTNeverReset.LOG.warn("ignoring invalid bee breeder voltage override: {}", entry);
                    continue;
                }
                overrides.put(uid, tier);
            }
            tiers = calculateTiers(species, overrides);
        }
    }
}
