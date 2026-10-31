package com.xyp.gtneverreset.client.radialmenu;

import java.util.List;

public final class ActionSteps {

    public static boolean move(List<ActionSpec> steps, int index, int direction) {
        if (steps == null || direction == 0 || index < 0 || index >= steps.size()) {
            return false;
        }
        int target = index + (direction < 0 ? -1 : 1);
        if (target < 0 || target >= steps.size()) {
            return false;
        }
        steps.set(index, steps.set(target, steps.get(index)));
        return true;
    }
}
