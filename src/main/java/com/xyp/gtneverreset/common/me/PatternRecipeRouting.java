package com.xyp.gtneverreset.common.me;

import java.util.HashSet;
import java.util.Set;

import com.xyp.gtneverreset.api.mixinHelper.PatternRecipeProcessingAccess;

import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Uses the controller's own mode API so mode-dependent validation, parallel and overclock rules still apply. */
public final class PatternRecipeRouting {

    private static final int MAX_MODE_PROBES = 256;

    private PatternRecipeRouting() {}

    public static boolean cachePattern(ProcessingLogic logic, IDualInputInventoryWithPattern inventory) {
        return ((PatternRecipeProcessingAccess) logic).gtnr$cachePattern(inventory);
    }

    public static CheckRecipeResult processPattern(ProcessingLogic logic) {
        return ((PatternRecipeProcessingAccess) logic).gtnr$processPattern();
    }

    public static ProcessingLogic clearProcessing(ProcessingLogic logic) {
        ((PatternRecipeProcessingAccess) logic).gtnr$resetRouting();
        return logic.clear();
    }

    public static Selection select(IVoidable machine, RecipeMap<?> requested) {
        if (!(machine instanceof MTEMultiBlockBase controller) || requested == null
            || !controller.getAvailableRecipeMaps()
                .contains(requested))
            return null;
        Selection selection = new Selection(controller);
        if (controller.getRecipeMap() == requested) return selection;
        if (!controller.supportsMachineModeSwitch() || controller.isRecipeLockingEnabled()) return null;
        boolean selected = false;
        try {
            Set<Integer> visited = new HashSet<>();
            visited.add(controller.getMachineMode());
            for (int probe = 0; probe < MAX_MODE_PROBES; probe++) {
                // Some controllers advance their own mode inside nextMachineMode(). Restore even on failure.
                selection.changed = true;
                int next = controller.nextMachineMode();
                controller.setMachineMode(next);
                if (controller.getRecipeMap() == requested) {
                    selected = true;
                    return selection;
                }
                if (!visited.add(controller.getMachineMode())) break;
            }
            return null;
        } finally {
            if (!selected) selection.close();
        }
    }

    public static final class Selection implements AutoCloseable {

        private final MTEMultiBlockBase controller;
        private final int previousMode;
        private final SingleRecipeCheck previousLock;
        private boolean changed;

        private Selection(MTEMultiBlockBase controller) {
            this.controller = controller;
            previousMode = controller.getMachineMode();
            previousLock = controller.getSingleRecipeCheck();
        }

        @Override
        public void close() {
            if (!changed) return;
            changed = false;
            try {
                controller.setMachineMode(previousMode);
            } finally {
                controller.setSingleRecipeCheck(previousLock);
            }
        }

        public boolean changedMode() {
            return changed;
        }
    }
}
