package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;

public final class ModuleHatchLoader {

    public static final GTNRItemList[] STATIC_PARALLEL = { GTNRItemList.StaticParallelControllerT1,
        GTNRItemList.StaticParallelControllerT2, GTNRItemList.StaticParallelControllerT3,
        GTNRItemList.StaticParallelControllerT4, GTNRItemList.StaticParallelControllerT5,
        GTNRItemList.StaticParallelControllerT6, GTNRItemList.StaticParallelControllerT7,
        GTNRItemList.StaticParallelControllerT8 };

    public static final GTNRItemList[] DYNAMIC_PARALLEL = { GTNRItemList.DynamicParallelControllerT1,
        GTNRItemList.DynamicParallelControllerT2, GTNRItemList.DynamicParallelControllerT3,
        GTNRItemList.DynamicParallelControllerT4, GTNRItemList.DynamicParallelControllerT5,
        GTNRItemList.DynamicParallelControllerT6, GTNRItemList.DynamicParallelControllerT7,
        GTNRItemList.DynamicParallelControllerT8 };

    public static final GTNRItemList[] STATIC_SPEED = { GTNRItemList.StaticSpeedControllerT1,
        GTNRItemList.StaticSpeedControllerT2, GTNRItemList.StaticSpeedControllerT3,
        GTNRItemList.StaticSpeedControllerT4, GTNRItemList.StaticSpeedControllerT5,
        GTNRItemList.StaticSpeedControllerT6, GTNRItemList.StaticSpeedControllerT7,
        GTNRItemList.StaticSpeedControllerT8 };

    public static final GTNRItemList[] DYNAMIC_SPEED = { GTNRItemList.DynamicSpeedControllerT1,
        GTNRItemList.DynamicSpeedControllerT2, GTNRItemList.DynamicSpeedControllerT3,
        GTNRItemList.DynamicSpeedControllerT4, GTNRItemList.DynamicSpeedControllerT5,
        GTNRItemList.DynamicSpeedControllerT6, GTNRItemList.DynamicSpeedControllerT7,
        GTNRItemList.DynamicSpeedControllerT8 };

    public static final GTNRItemList[] POWER = { GTNRItemList.PowerConsumptionControllerT1,
        GTNRItemList.PowerConsumptionControllerT2, GTNRItemList.PowerConsumptionControllerT3,
        GTNRItemList.PowerConsumptionControllerT4, GTNRItemList.PowerConsumptionControllerT5,
        GTNRItemList.PowerConsumptionControllerT6, GTNRItemList.PowerConsumptionControllerT7,
        GTNRItemList.PowerConsumptionControllerT8 };

    public static final GTNRItemList[] OVERCLOCK = { GTNRItemList.LowSpeedPerfectOverclockController,
        GTNRItemList.PerfectOverclockController, GTNRItemList.SingularityPerfectOverclockController };

    private ModuleHatchLoader() {}

    public static void register() {
        registerParameters(STATIC_PARALLEL, MachineModuleType.Parallel, false);
        registerParameters(DYNAMIC_PARALLEL, MachineModuleType.Parallel, true);
        registerParameters(STATIC_SPEED, MachineModuleType.Speed, false);
        registerParameters(DYNAMIC_SPEED, MachineModuleType.Speed, true);
        registerParameters(POWER, MachineModuleType.PowerConsumption, false);
        registerParameters(OVERCLOCK, MachineModuleType.Overclock, false);
    }

    private static void registerParameters(GTNRItemList[] entries, MachineModuleType type, boolean dynamic) {
        for (int i = 0; i < entries.length; i++) {
            GTNRItemList entry = entries[i];
            entry.set(new ParameterModuleHatch(id(entry), entry.name(), type, i + 1, dynamic));
        }
    }

    private static int id(GTNRItemList entry) {
        return GTNRMachineID.valueOf(entry.name()).id;
    }
}
