package com.xyp.gtneverreset.utils.enums;

public enum MachineModuleType {

    Parallel,
    Speed,
    PowerConsumption,
    Overclock;

    public String getTranslationKey() {
        return switch (this) {
            // #tr gtnr.module.type.parallel
            // # Parallel Module
            // # zh_CN 并行模块
            case Parallel -> "gtnr.module.type.parallel";
            // #tr gtnr.module.type.speed
            // # Speed Module
            // # zh_CN 速度模块
            case Speed -> "gtnr.module.type.speed";
            // #tr gtnr.module.type.eu
            // # EU Module
            // # zh_CN EU 模块
            case PowerConsumption -> "gtnr.module.type.eu";
            // #tr gtnr.module.type.overclock
            // # Overclock Module
            // # zh_CN 超频模块
            case Overclock -> "gtnr.module.type.overclock";
        };
    }
}
