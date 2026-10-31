package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.loader.machine.BasicMachineLoader;
import com.xyp.gtneverreset.loader.machine.HatchLoader;
import com.xyp.gtneverreset.loader.machine.MultiblockLoader;

public final class MachineLoader implements IContentLoader {

    @Override
    public String name() {
        return "machine";
    }

    @Override
    public void init() {
        registry();
    }

    public static void registry() {
        HatchLoader.registry();
        MultiblockLoader.registry();
        BasicMachineLoader.registry();
    }
}
