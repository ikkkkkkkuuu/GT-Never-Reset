package com.xyp.gtneverreset.common.machine.tooltip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

public final class StructureTooltipHooks {

    public static final int UNLIMITED = Integer.MAX_VALUE;
    private static final ThreadLocal<Recording> RECORDING = new ThreadLocal<>();
    private static final Set<String> HATCH_KEYS = ConcurrentHashMap.newKeySet();

    public static <T> IStructureElement<T> describeAsHatch(IStructureElement<T> element, String... nameKeys) {
        HATCH_KEYS.addAll(Arrays.asList(nameKeys));
        return StructureUtility.withDescription(Arrays.asList(nameKeys), element);
    }

    public static boolean isHatchKey(String key) {
        return HATCH_KEYS.contains(key);
    }

    public static boolean isRecording(Object controller) {
        return activeFor(controller) != null;
    }

    public static boolean onBuildPiece(Object controller, String piece, int horizontalOffset, int verticalOffset,
        int depthOffset) {
        Recording recording = activeFor(controller);
        if (recording == null) return false;
        recording.pieces.add(new PieceCall(piece, horizontalOffset, verticalOffset, depthOffset));
        return true;
    }

    public static boolean onCheckCasingMin(Object controller, int required) {
        Recording recording = activeFor(controller);
        if (recording == null) return false;
        recording.casingMinimums.add(required);
        return true;
    }

    public static boolean onCheckHatches(Object controller, int min, int max, IHatchElement<?>... elements) {
        Recording recording = activeFor(controller);
        if (recording == null) return false;
        List<String> keys = new ArrayList<>();
        for (IHatchElement<?> element : elements) keys.addAll(element.getDescriptionLangKeys());
        return onCheckHatchKeys(controller, min, max, keys.toArray(String[]::new));
    }

    public static boolean onCheckHatchKeys(Object controller, int min, int max, String... nameKeys) {
        return recordHatchRule(controller, min, max, 0, nameKeys);
    }

    /** Records a shared formation rule requiring the given number of hatches on every repeated layer. */
    public static boolean onCheckLayerHatches(Object controller, int minLayers, int perLayer,
        IHatchElement<?>... elements) {
        if (activeFor(controller) == null) return false;
        long minimum = (long) minLayers * perLayer;
        if (minLayers < 0 || perLayer < 1 || minimum > UNLIMITED) {
            throw new IllegalArgumentException("Invalid tooltip layer hatch requirement");
        }
        List<String> keys = new ArrayList<>();
        for (IHatchElement<?> element : elements) keys.addAll(element.getDescriptionLangKeys());
        return recordHatchRule(controller, (int) minimum, UNLIMITED, perLayer, keys.toArray(String[]::new));
    }

    private static boolean recordHatchRule(Object controller, int min, int max, int perLayer, String... nameKeys) {
        Recording recording = activeFor(controller);
        if (recording == null) return false;
        if (min < 0 || max < min || nameKeys.length == 0) {
            throw new IllegalArgumentException("Invalid tooltip hatch requirement");
        }
        HATCH_KEYS.addAll(Arrays.asList(nameKeys));
        HatchRule rule = new HatchRule(List.of(nameKeys), min, max, perLayer);
        if (!recording.hatchRules.contains(rule)) recording.hatchRules.add(rule);
        return true;
    }

    static <R> R record(MTEEnhancedMultiBlockBase<?> controller, Function<Recording, R> read) {
        if (controller.getBaseMetaTileEntity() != null || RECORDING.get() != null) {
            throw new IllegalStateException("Tooltip recording requires a separate, detached machine");
        }
        Recording recording = new Recording(controller);
        RECORDING.set(recording);
        try {
            return read.apply(recording);
        } finally {
            RECORDING.remove();
        }
    }

    private static Recording activeFor(Object controller) {
        Recording recording = RECORDING.get();
        return recording != null && recording.controller == controller ? recording : null;
    }

    record PieceCall(String piece, int horizontalOffset, int verticalOffset, int depthOffset) {}

    record HatchRule(List<String> keys, int min, int max, int perLayer) {

        HatchRule(List<String> keys, int min, int max) {
            this(keys, min, max, 0);
        }
    }

    static final class Recording {

        final Object controller;
        final List<PieceCall> pieces = new ArrayList<>();
        final List<Integer> casingMinimums = new ArrayList<>();
        final List<HatchRule> hatchRules = new ArrayList<>();

        private Recording(Object controller) {
            this.controller = controller;
        }
    }
}
