package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.client.quantum.CraftingCycleIndex;
import com.xyp.gtneverreset.client.quantum.CraftingTreeRings;
import com.xyp.gtneverreset.client.quantum.ICraftingTreeCycles;

import appeng.api.config.CraftingMode;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingRequest.SubstitutionMode;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "craftingcycleqa", name = "Crafting Cycle QA", version = "1", dependencies = "after:gtneverreset")
public final class CraftingCycleChecks {

    private boolean done;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().currentScreen == null) return;
        done = true;
        try {
            if (!com.xyp.gtneverreset.client.quantum.CraftConfirmCycles.Host.class
                .isAssignableFrom(Class.forName("appeng.client.gui.implementations.GuiCraftConfirm"))) {
                throw new AssertionError("craft confirmation host bridge was not applied");
            }
            var tree = new GuiCraftingTree(null, 0, 0, 203, 170);
            var cycles = (ICraftingTreeCycles) tree;
            var stack = AEItemStack.create(new ItemStack(Items.iron_ingot));
            var request = new CraftingRequest(stack, SubstitutionMode.values()[0], true, CraftingMode.STANDARD);
            tree.setRequest(request);
            if (CraftingTreeRings.allNodes(tree)
                .size() != 1) throw new AssertionError("request tree was not built");
            cycles.gtnr$setCycles(CraftingCycleIndex.of(stack, List.of(), List.of()));
            if (cycles.gtnr$ringView() != null || !cycles.gtnr$hoveredPatterns()
                .isEmpty()) throw new AssertionError("empty cycle state");
            tree.setRequest(request);
            tree.updateSearchGoToList("");
            cycles.gtnr$closeRingView();
            tree.setRequest(
                new CraftingRequest(stack.copy(), SubstitutionMode.values()[0], true, CraftingMode.STANDARD));
            if (CraftingTreeRings.allNodes(tree)
                .size() != 1) throw new AssertionError("request replacement accumulated nodes");
            Files.writeString(Paths.get("result.txt"), "PASS");
            System.out
                .println("CRAFTING_CYCLE_QA PASS: mixin application, request replacement, empty cycle navigation");
            FMLCommonHandler.instance()
                .exitJava(0, false);
        } catch (Throwable failure) {
            failure.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }
}
