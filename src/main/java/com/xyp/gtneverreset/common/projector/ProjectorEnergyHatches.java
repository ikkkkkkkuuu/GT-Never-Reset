package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.github.bsideup.jabel.Desugar;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import gregtech.api.util.ExoticEnergyInputHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import tectech.mechanics.pipe.IConnectsToEnergyTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

public class ProjectorEnergyHatches {

    public static final String WIRED = "wired", WIRELESS = "wireless";
    private static final long KILO = 1L << 10, MEGA = 1L << 20, GIGA = 1L << 30, SHORTEN_FROM = 16 * KILO;

    private static Int2ObjectOpenHashMap<String> models;

    public enum Flow {
        INPUT,
        OUTPUT
    }

    @Desugar
    private record Key(Flow flow, int tier, long amperes, boolean wireless, boolean laser) {}

    private ProjectorEnergyHatches() {}

    // Every mod's energy hatch has to extend these GT and TecTech bases, or register with the exotic input
    // helper, for GT's hatch adders to accept it.
    public static Flow flowOf(int id) {
        IMetaTileEntity prototype = ProjectorMachines.prototype(id);
        if (prototype instanceof MTEHatchEnergy || ExoticEnergyInputHelper.isExoticEnergyInput(prototype))
            return Flow.INPUT;
        return prototype instanceof MTEHatchDynamo ? Flow.OUTPUT : null;
    }

    public static long amperes(int id) {
        IMetaTileEntity prototype = ProjectorMachines.prototype(id);
        if (prototype instanceof MTEHatchEnergyMulti multi) return multi.maxAmperes;
        if (prototype instanceof MTEHatchDynamoMulti multi) return multi.maxAmperes;
        if (!(prototype instanceof MetaTileEntity tile)) return 0;
        return flowOf(id) == Flow.INPUT ? tile.maxAmperesIn() : tile.maxAmperesOut();
    }

    public static boolean isWireless(int id) {
        IMetaTileEntity prototype = ProjectorMachines.prototype(id);
        return prototype instanceof MTEWirelessEnergy || prototype instanceof MTEHatchWirelessMulti
            || prototype instanceof MTEHatchWirelessDynamoMulti;
    }

    public static boolean isLaser(int id) {
        if (isWireless(id)) return amperes(id) > 64;
        return ProjectorMachines.prototype(id) instanceof IConnectsToEnergyTunnel;
    }

    public static String modelOf(int id) {
        return models().get(id);
    }

    public static String amperesLabel(long amperes) {
        if (amperes < SHORTEN_FROM) return amperes + "A";
        long unit = amperes >= GIGA ? GIGA : amperes >= MEGA ? MEGA : KILO;
        String suffix = unit == GIGA ? "G" : unit == MEGA ? "M" : "K";
        return (amperes % unit == 0 ? "" : "~") + Math.round(amperes / (double) unit) + suffix + "A";
    }

    // The most common class at a set of parameters is the plain hatch; mana, tap and other hatches that share
    // those parameters become models of their own instead of replacing it.
    private static synchronized Int2ObjectOpenHashMap<String> models() {
        if (models == null) {
            Object2IntOpenHashMap<Class<?>> popularity = new Object2IntOpenHashMap<>();
            Map<Key, List<Integer>> byKey = new HashMap<>();
            for (int id : ProjectorMachines.hatches()) {
                Flow flow = flowOf(id);
                if (flow == null || ProjectorMachines.isCreative(id)) continue;
                popularity.addTo(
                    ProjectorMachines.prototype(id)
                        .getClass(),
                    1);
                byKey.computeIfAbsent(key(flow, id), key -> new ArrayList<>())
                    .add(id);
            }
            Int2ObjectOpenHashMap<String> found = new Int2ObjectOpenHashMap<>();
            Comparator<Integer> plainFirst = Comparator
                .comparingInt(
                    (Integer id) -> -popularity.getInt(
                        ProjectorMachines.prototype(id)
                            .getClass()))
                .thenComparingInt(ProjectorEnergyHatches::depth)
                .thenComparingInt(id -> id);
            Set<Class<?>> plain = new HashSet<>();
            for (List<Integer> ids : byKey.values()) {
                ids.sort(plainFirst);
                plain.add(
                    ProjectorMachines.prototype(ids.get(0))
                        .getClass());
            }
            // A plain series class that repeats another mod's hatch at the same parameters is a copy, not a model.
            for (Map.Entry<Key, List<Integer>> entry : byKey.entrySet()) {
                String plainModel = entry.getKey()
                    .wireless() ? WIRELESS : WIRED;
                for (int id : entry.getValue()) {
                    Class<?> type = ProjectorMachines.prototype(id)
                        .getClass();
                    found.put(id, plain.contains(type) ? plainModel : type.getName());
                }
            }
            models = found;
        }
        return models;
    }

    private static Key key(Flow flow, int id) {
        return new Key(flow, ProjectorMachines.tier(id), amperes(id), isWireless(id), isLaser(id));
    }

    private static int depth(int id) {
        int depth = 0;
        for (Class<?> type = ProjectorMachines.prototype(id)
            .getClass(); type != null; type = type.getSuperclass()) depth++;
        return depth;
    }

    public static Comparator<Integer> order() {
        return Comparator.comparingInt(ProjectorMachines::tier)
            .thenComparingLong(ProjectorEnergyHatches::amperes)
            .thenComparing(ProjectorEnergyHatches::isLaser);
    }

    public static List<Integer> tierChoices(List<Integer> ids, int current) {
        TreeSet<Integer> tiers = new TreeSet<>();
        for (int id : ids) tiers.add(ProjectorMachines.tier(id));
        List<Integer> choices = new ArrayList<>();
        for (int tier : tiers) choices.add(byTier(ids, current, tier));
        return choices;
    }

    public static List<Integer> amperesChoices(List<Integer> ids, int current) {
        List<Integer> sorted = new ArrayList<>(ids);
        sorted.sort(
            Comparator.comparingLong(ProjectorEnergyHatches::amperes)
                .thenComparing(ProjectorEnergyHatches::isLaser));
        List<Integer> choices = new ArrayList<>();
        Integer last = null;
        for (int id : sorted) {
            if (last != null && amperes(last) == amperes(id) && isLaser(last) == isLaser(id)) continue;
            last = id;
            choices.add(byAmperes(ids, current, amperes(id), isLaser(id)));
        }
        return choices;
    }

    public static int closest(List<Integer> ids, int current) {
        int best = ids.get(0);
        for (int id : ids) {
            if (compareTierFirst(id, best, current) < 0) best = id;
        }
        return best;
    }

    private static int byTier(List<Integer> ids, int current, int tier) {
        int best = 0;
        for (int id : ids) {
            if (ProjectorMachines.tier(id) != tier) continue;
            if (best == 0 || compareAmperesFirst(id, best, current) < 0) best = id;
        }
        return best;
    }

    private static int byAmperes(List<Integer> ids, int current, long amperes, boolean laser) {
        int best = 0;
        for (int id : ids) {
            if (amperes(id) != amperes || isLaser(id) != laser) continue;
            if (best == 0 || compareTierFirst(id, best, current) < 0) best = id;
        }
        return best;
    }

    private static int compareTierFirst(int first, int second, int current) {
        int tier = Integer.compare(tierDistance(first, current), tierDistance(second, current));
        if (tier != 0) return tier;
        int amperes = Double.compare(amperesDistance(first, current), amperesDistance(second, current));
        return amperes != 0 ? amperes : Integer.compare(first, second);
    }

    private static int compareAmperesFirst(int first, int second, int current) {
        int amperes = Double.compare(amperesDistance(first, current), amperesDistance(second, current));
        return amperes != 0 ? amperes : Integer.compare(first, second);
    }

    private static int tierDistance(int id, int current) {
        return Math.abs(ProjectorMachines.tier(id) - ProjectorMachines.tier(current));
    }

    private static double amperesDistance(int id, int current) {
        double distance = Math.abs(Math.log(Math.max(1, amperes(id))) - Math.log(Math.max(1, amperes(current))));
        return isLaser(id) == isLaser(current) ? distance : distance + 0.5;
    }
}
