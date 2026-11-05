package com.xyp.gtneverreset.common.me;

import java.util.HashSet;
import java.util.Set;

import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.SingleRecipeCheck;

/** Uses the controller's own mode API so mode-dependent validation, parallel and overclock rules still apply. */
public final class PatternRecipeRouting {

    private static final int MAX_MODE_PROBES = 256;

    private PatternRecipeRouting() {}

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
                int next = controller.nextMachineMode();
                if (!visited.add(next)) break;
                selection.changed = true;
                controller.setMachineMode(next);
                if (controller.getRecipeMap() == requested) {
                    selected = true;
                    return selection;
                }
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
