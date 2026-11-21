package com.xyp.gtneverreset.common.projector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;

import goodgenerator.blocks.tileEntity.AntimatterForge;
import goodgenerator.blocks.tileEntity.AntimatterOutputHatch;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchInputBusDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInputDebug;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchMuffler;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchPatternProvider;
import gregtech.common.tileentities.machines.multi.MTEIndustrialChisel;
import gregtech.common.tileentities.machines.multi.MTEIndustrialExtruder;
import gregtech.common.tileentities.machines.multi.MTELargeMolecularAssembler;
import gregtech.common.tileentities.machines.multi.purification.MTEHatchLensHousing;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitUVTreatment;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchChiselBus;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchExtrusion;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchSteamBusInput;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchSteamBusOutput;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchTurbineProvider;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTEHatchCustomFluidBaseDebug;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.MTESteamMultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.MTEIndustrialChiselLegacy;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.turbines.MTELargerTurbineBaseLegacy;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import kubatech.tileentity.gregtech.hatch.MTEHatchElectrode;
import kubatech.tileentity.gregtech.multiblock.MTEIndustrialArcFurnace;
import tectech.thing.metaTileEntity.hatch.MTEHatchCreativeData;
import tectech.thing.metaTileEntity.hatch.MTEHatchCreativeUncertainty;
import tectech.thing.metaTileEntity.hatch.MTEHatchDebugDataAccess;

public class ProjectorMachines {

    private static final Class<?>[] PROBE_TYPES = { MTEHatchEnergy.class, MTEHatchDynamo.class, MTEHatchInputBus.class,
        MTEHatchOutputBus.class, MTEHatchInput.class, MTEHatchOutput.class, MTEHatchMaintenance.class,
        MTEHatchMuffler.class, MTEHatchDataAccess.class };
    private static final Class<?>[] TIERED_TYPES = { MTEHatchEnergy.class, MTEHatchDynamo.class, MTEHatchInputBus.class,
        MTEHatchOutputBus.class, MTEHatchInput.class, MTEHatchOutput.class };
    // Buses before hatches: ME crafting inputs are listed under both and belong with the buses.
    private static final HatchElement[] KINDS = { HatchElement.InputBus, HatchElement.OutputBus,
        HatchElement.SolidifierHatch, HatchElement.CryotheumHatch, HatchElement.PyrotheumHatch, HatchElement.InputHatch,
        HatchElement.OutputHatch, HatchElement.Energy, HatchElement.ExoticEnergy, HatchElement.Dynamo,
        HatchElement.LaserSource, HatchElement.Maintenance, HatchElement.Muffler };

    @Desugar
    private record Owner(Class<? extends IMetaTileEntity> hatch, List<Class<? extends IMetaTileEntity>> machines) {}

    // GT keeps hatches made for one machine out of its normal kinds only by exact class, loses that for elements made
    // with withAdder or withCount, and does not blacklist the chisel, turbine and antimatter hatches at all.
    private static final Owner[] OWNERS = { owner(MTEHatchSteamBusInput.class, MTESteamMultiBlockBase.class),
        owner(MTEHatchSteamBusOutput.class, MTESteamMultiBlockBase.class),
        owner(MTEHatchExtrusion.class, MTEIndustrialExtruder.class),
        owner(MTEHatchPatternProvider.class, MTELargeMolecularAssembler.class),
        owner(MTEHatchChiselBus.class, MTEIndustrialChisel.class, MTEIndustrialChiselLegacy.class),
        owner(MTEHatchTurbineProvider.class, MTELargerTurbineBaseLegacy.class),
        owner(AntimatterOutputHatch.class, AntimatterForge.class),
        owner(MTEHatchElectrode.class, MTEIndustrialArcFurnace.class),
        owner(MTEHatchLensHousing.class, MTEPurificationUnitUVTreatment.class) };
    private static final Class<?>[] CREATIVE = { MTEHatchInputDebug.class, MTEHatchInputBusDebug.class,
        MTEHatchEnergyDebug.class, MTEHatchCustomFluidBaseDebug.class, MTEHatchCreativeData.class,
        MTEHatchCreativeUncertainty.class, MTEHatchDebugDataAccess.class };
    private static final HatchElement[] SERIES_KINDS = { HatchElement.InputBus, HatchElement.OutputBus,
        HatchElement.InputHatch, HatchElement.OutputHatch };
    private static int[] supported, hatches;
    private static byte[] kinds;
    private static Int2ObjectOpenHashMap<String> series;
    private static Map<String, HatchElement> kindsByKey;

    private ProjectorMachines() {}

    @SafeVarargs
    private static Owner owner(Class<? extends IMetaTileEntity> hatch, Class<? extends IMetaTileEntity>... machines) {
        return new Owner(hatch, Arrays.asList(machines));
    }

    public static boolean allowedAt(int id, List<String> spotKinds, IMetaTileEntity machine) {
        IMetaTileEntity prototype = prototype(id);
        if (prototype == null || isAny(prototype, CREATIVE)) return false;
        if (!isSpecial(prototype.getClass()) || isOwnedBy(prototype, machine)) return true;
        boolean matched = false;
        for (String key : spotKinds) {
            HatchElement kind = kindsByKey().get(key);
            if (kind == null || !kind.matchesHatch(prototype)) continue;
            if (namesSpecial(kind, prototype)) return true;
            matched = true;
        }
        return !matched && !spotKinds.isEmpty();
    }

    public static boolean isCreative(int id) {
        IMetaTileEntity prototype = prototype(id);
        return prototype != null && isAny(prototype, CREATIVE);
    }

    private static boolean isSpecial(Class<?> type) {
        for (Owner owner : OWNERS) {
            if (owner.hatch()
                .isAssignableFrom(type)) return true;
        }
        for (HatchElement kind : HatchElement.values()) {
            for (Class<?> blacklisted : kind.mteBlacklist()) {
                if (blacklisted.isAssignableFrom(type)) return true;
            }
        }
        return false;
    }

    private static boolean isBlacklisted(HatchElement kind, IMetaTileEntity prototype) {
        for (Class<?> type : kind.mteBlacklist()) {
            if (type.isInstance(prototype)) return true;
        }
        return false;
    }

    private static boolean isOwnedBy(IMetaTileEntity prototype, IMetaTileEntity machine) {
        for (Owner owner : OWNERS) {
            if (!owner.hatch()
                .isInstance(prototype)) continue;
            for (Class<?> type : owner.machines()) {
                if (type.isInstance(machine)) return true;
            }
        }
        return false;
    }

    private static boolean namesSpecial(HatchElement kind, IMetaTileEntity prototype) {
        for (Class<?> type : kind.mteClasses()) {
            if (type.isInstance(prototype) && isSpecial(type)) return true;
        }
        return false;
    }

    private static boolean isAny(IMetaTileEntity prototype, Class<?>[] types) {
        for (Class<?> type : types) {
            if (type.isInstance(prototype)) return true;
        }
        return false;
    }

    private static synchronized Map<String, HatchElement> kindsByKey() {
        if (kindsByKey == null) {
            Map<String, HatchElement> keys = new HashMap<>();
            for (HatchElement kind : HatchElement.values()) {
                for (String key : kind.getDescriptionLangKeys()) keys.put(key, kind);
            }
            kindsByKey = keys;
        }
        return kindsByKey;
    }

    public static IMetaTileEntity prototype(int id) {
        return id > 0 && id < GregTechAPI.METATILEENTITIES.length ? GregTechAPI.METATILEENTITIES[id] : null;
    }

    public static boolean isSupported(int id) {
        return prototype(id) instanceof ISurvivalConstructable;
    }

    public static synchronized int[] supported() {
        if (supported == null) {
            IntArrayList ids = new IntArrayList();
            for (int id = 1; id < GregTechAPI.METATILEENTITIES.length; id++) {
                if (isSupported(id)) ids.add(id);
            }
            supported = ids.toIntArray();
        }
        return supported;
    }

    public static synchronized int[] hatches() {
        if (hatches == null) {
            IntArrayList ids = new IntArrayList();
            for (int id = 1; id < GregTechAPI.METATILEENTITIES.length; id++) {
                if (GregTechAPI.METATILEENTITIES[id] instanceof MTEHatch) ids.add(id);
            }
            hatches = ids.toIntArray();
        }
        return hatches;
    }

    // GT names the steps of one series differently, such as compressed and quantum buses or basic and advanced
    // stocking buses; two hatches of one class at the same tier are different products instead.
    public static String seriesOf(int id) {
        return series().get(id);
    }

    private static synchronized Int2ObjectOpenHashMap<String> series() {
        if (series == null) {
            Map<String, IntArrayList> byKey = new HashMap<>();
            for (int id : hatches()) {
                if (!Arrays.asList(SERIES_KINDS)
                    .contains(kindOf(id)) || isCreative(id)) continue;
                byKey.computeIfAbsent(seriesKey(id), key -> new IntArrayList())
                    .add(id);
            }
            Int2ObjectOpenHashMap<String> found = new Int2ObjectOpenHashMap<>();
            for (Map.Entry<String, IntArrayList> entry : byKey.entrySet()) {
                IntOpenHashSet tiers = new IntOpenHashSet();
                boolean repeats = false;
                for (int id : entry.getValue()) repeats |= !tiers.add(tier(id));
                if (repeats) continue;
                for (int id : entry.getValue()) found.put(id, entry.getKey());
            }
            series = found;
        }
        return series;
    }

    private static String seriesKey(int id) {
        IMetaTileEntity prototype = prototype(id);
        String key = prototype.getClass()
            .getName();
        if (!(prototype instanceof IDualInputHatch dual)) return key;
        return key + (dual.supportsFluids() ? "|fluid" : "|item");
    }

    public static HatchElement kindOf(int id) {
        byte[] all = kinds();
        return id > 0 && id < all.length && all[id] >= 0 ? KINDS[all[id]] : null;
    }

    private static synchronized byte[] kinds() {
        if (kinds == null) {
            byte[] all = new byte[GregTechAPI.METATILEENTITIES.length];
            Arrays.fill(all, (byte) -1);
            for (int id : hatches()) {
                IMetaTileEntity prototype = prototype(id);
                for (int kind = 0; kind < KINDS.length; kind++) {
                    if (!KINDS[kind].matchesHatch(prototype) || isBlacklisted(KINDS[kind], prototype)) continue;
                    all[id] = (byte) kind;
                    break;
                }
            }
            kinds = all;
        }
        return kinds;
    }

    public static int[] probeHatches(int preferredTier) {
        int[] probes = new int[PROBE_TYPES.length];
        for (int type = 0; type < PROBE_TYPES.length; type++) {
            int best = 0;
            for (int id : hatches()) {
                if (!PROBE_TYPES[type].isInstance(prototype(id))) continue;
                if (best == 0 || compareHatches(id, best, preferredTier) < 0) best = id;
            }
            probes[type] = best;
        }
        return probes;
    }

    // Plain hatches before their subclasses (drone downlink, ME and wireless variants), then a fixed order of kinds so
    // spots shared by several kinds get the same kind at every tier; tier only where it matters.
    public static int compareHatches(int first, int second, int preferredTier) {
        int depth = Integer.compare(depth(first), depth(second));
        if (depth != 0) return depth;
        int kind = Integer.compare(kindIndex(first), kindIndex(second));
        if (kind != 0) return kind;
        int tier = Integer.compare(tierDistance(first, preferredTier), tierDistance(second, preferredTier));
        return tier != 0 ? tier : Integer.compare(first, second);
    }

    private static int kindIndex(int id) {
        byte[] all = kinds();
        return id > 0 && id < all.length && all[id] >= 0 ? all[id] : KINDS.length;
    }

    private static int tierDistance(int id, int preferredTier) {
        IMetaTileEntity prototype = prototype(id);
        for (Class<?> type : TIERED_TYPES) {
            if (type.isInstance(prototype)) return Math.abs(tier(id) - preferredTier);
        }
        return 0;
    }

    private static int depth(int id) {
        int depth = 0;
        IMetaTileEntity prototype = prototype(id);
        for (Class<?> type = prototype == null ? null : prototype.getClass(); type != null; type = type.getSuperclass())
            depth++;
        return depth;
    }

    public static boolean isHatch(int id) {
        return prototype(id) instanceof MTEHatch;
    }

    public static int tier(int id) {
        return prototype(id) instanceof MTETieredMachineBlock tiered ? tiered.mTier : -1;
    }

    public static ItemStack stackOf(int id) {
        return prototype(id) == null ? null : new ItemStack(GregTechAPI.sBlockMachines, 1, id);
    }

    public static int idOf(ItemStack stack) {
        if (stack == null || stack.getItem() != Item.getItemFromBlock(GregTechAPI.sBlockMachines)) return 0;
        return prototype(stack.getItemDamage()) == null ? 0 : stack.getItemDamage();
    }

    public static int supportedIdOf(ItemStack stack) {
        int id = idOf(stack);
        return isSupported(id) ? id : 0;
    }

    public static Predicate<ExtendedFacing> validFacings(int id) {
        IMetaTileEntity prototype = prototype(id);
        if (prototype instanceof IAlignment alignment) return alignment::isNewExtendedFacingValid;
        return facing -> facing.getRotation() == Rotation.NORMAL && facing.getFlip() == Flip.NONE
            && prototype != null
            && prototype.isFacingValid(facing.getDirection());
    }
}
