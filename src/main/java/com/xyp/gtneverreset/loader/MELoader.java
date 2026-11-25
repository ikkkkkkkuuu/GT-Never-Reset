package com.xyp.gtneverreset.loader;

import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;
import com.xyp.gtneverreset.common.crafting.CraftingCompletionService;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.me.ChatComponentInterfaceNameSuffix;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;

public final class MELoader implements IContentLoader {

    @Override
    public void init() {
        if (!ModList.AE2ConvenienceFeatures.isModLoaded()) {
            FMLCommonHandler.instance()
                .bus()
                .register(CraftingCompletionService.INSTANCE);
        }
    }

    @Override
    public void postInit() {
        var interfaceTerminal = AEApi.instance()
            .registries()
            .interfaceTerminal();
        interfaceTerminal.register(AssemblerMatrix.class);
        interfaceTerminal.register(SuperMTEHatchCraftingInputME.class);
        interfaceTerminal.register(CircuitMEPatternBuffer.class);
        interfaceTerminal.register(CrossRecipeMEPatternBuffer.class);
        interfaceTerminal.register(TileEntityOverloadedProvider.class);
        interfaceTerminal.register(TileEntityPackagedProvider.class);
        ChatComponentCustomRegistry.register(ChatComponentInterfaceNameSuffix::new);
    }

    @Override
    public void serverStopped() {
        CraftingCompletionService.clear();
    }
}
