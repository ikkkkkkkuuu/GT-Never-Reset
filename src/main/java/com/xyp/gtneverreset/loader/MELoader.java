package com.xyp.gtneverreset.loader;

import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.me.ChatComponentInterfaceNameSuffix;

import appeng.api.AEApi;

public final class MELoader implements IContentLoader {

    @Override
    public String name() {
        return "me";
    }

    @Override
    public void postInit() {
        var interfaceTerminal = AEApi.instance()
            .registries()
            .interfaceTerminal();
        interfaceTerminal.register(AssemblerMatrix.class);
        interfaceTerminal.register(SuperMTEHatchCraftingInputME.class);
        interfaceTerminal.register(CircuitMEPatternBuffer.class);
        interfaceTerminal.register(TileEntityOverloadedProvider.class);
        interfaceTerminal.register(TileEntityPackagedProvider.class);
        ChatComponentCustomRegistry.register(ChatComponentInterfaceNameSuffix::new);
    }
}
