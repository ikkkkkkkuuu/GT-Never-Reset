package com.xyp.gtneverreset.common.recipe.gtneverreset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.Rule;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureDisplay;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureDisplay.Frame;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructurePackaging;
import com.xyp.gtneverreset.common.gui.recipe.BottledChaosStructureFrontend;
import com.xyp.gtneverreset.common.item.items.ChaosStructureModuleItem;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.common.blocks.ItemMachines;

/** NEI examples use the same cached assets and disk transaction as the GUI. They do not execute as GT recipes. */
public final class ChaosStructureModuleRecipes {

    public static void registerRecipes() {
        int registered = 0;
        boolean diskExample = false;
        Map<String, List<Rule>> machines = new LinkedHashMap<>();
        for (var rule : BottledChaosStructureCatalog.displayRules()) {
            if (rule.example() == null) {
                GTNeverReset.LOG.warn("Cannot resolve structure example {}.", rule.machineKey());
                continue;
            }
            // Tier variants of a single-block pool share one selectable input/output page.
            String group = ItemMachines.getMetaTileEntity(rule.controller()) instanceof MTEBasicMachine
                ? "single:" + rule.poolMask()
                    .toString()
                : rule.machineKey();
            machines.computeIfAbsent(group, key -> new ArrayList<>())
                .add(rule);
        }
        for (List<Rule> machine : machines.values()) {
            var display = new BottledChaosStructureDisplay(machine);
            if (!display.fits(BottledChaosStructureFrontend.MATERIAL_SLOTS)) {
                GTNeverReset.LOG.warn(
                    "Too many material types to display structure {}.",
                    machine.get(0)
                        .machineKey());
                continue;
            }
            if (add(display)) registered++;
            if (!diskExample) {
                var structure = machine.get(0)
                    .example();
                ItemStack module = ChaosStructureModuleItem.create(structure, 1);
                ItemStack emptyDisk = AEApi.instance()
                    .definitions()
                    .items()
                    .cell16384k()
                    .maybeStack(1)
                    .orNull();
                if (emptyDisk == null) continue;
                ItemStackHandler example = new ItemStackHandler(11);
                example.setStackInSlot(0, module.copy());
                example.setStackInSlot(1, emptyDisk.copy());
                if (BottledChaosStructurePackaging.execute(example)
                    .wasSuccessful()) {
                    ItemStack disk = example.getStackInSlot(10);
                    if (disk != null && add(
                        new BottledChaosStructureDisplay(
                            new Frame(
                                new ItemStack[] { disk.copy() },
                                GTNRItemList.ChaosStructureModule.get(1),
                                new ItemStack[] { module.copy(), emptyDisk.copy() },
                                0,
                                structure))))
                        registered++;
                    if (disk != null && add(
                        new BottledChaosStructureDisplay(
                            new Frame(
                                new ItemStack[] { emptyDisk.copy() },
                                module.copy(),
                                new ItemStack[] { GTNRItemList.ChaosStructureModule.get(1), disk.copy() },
                                0,
                                null))))
                        registered++;
                    diskExample = true;
                }
            }
        }
        GTNeverReset.LOG.info("Registered {} Bottle in Chaos structure packaging examples.", registered);
    }

    private static boolean add(BottledChaosStructureDisplay display) {
        var frame = display.frame();
        var recipe = GTValues.RA.stdBuilder()
            .itemInputsUnsafe(frame.inputs())
            .itemOutputs(frame.outputs())
            .metadata(BottledChaosStructureDisplay.DISPLAY, display)
            .specialValue(frame.profile())
            .duration(1)
            .eut(0)
            .fake()
            .ignoreCollision()
            .build();
        recipe.ifPresent(GTNRRecipeMaps.ChaosStructureModuleRecipes::add);
        return recipe.isPresent();
    }
}
