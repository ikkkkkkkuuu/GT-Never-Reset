package com.xyp.gtneverreset.common.projector;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory.CardSlot;
import com.xyp.gtneverreset.config.ToolsConfig;
import com.xyp.gtnrlib.animation.ProjectorArmSessions;

public class ProjectorServer {

    private ProjectorServer() {}

    public static ItemStack projectorAt(EntityPlayer player, int location) {
        CardSlot slot = WirelessCardInventory.resolve(player, location);
        if (slot == null) return null;
        ItemStack stack = slot.inventory()
            .getStackInSlot(slot.index());
        return stack != null && stack.getItem() instanceof MultiblockProjector ? stack : null;
    }

    public static void applyPlan(EntityPlayerMP player, int location, NBTTagCompound tag) {
        ItemStack stack = projectorAt(player, location);
        if (stack == null) return;
        ProjectorPlan plan = readPlan(tag);
        plan.write(stack);
        WirelessCardInventory.sync(player, stack);
    }

    public static void craftMaterials(EntityPlayerMP player, NBTTagCompound draft) {
        if (!ToolsConfig.enableMultiblockProjector) return;
        ProjectorPlan plan = readPlan(draft);
        if (!plan.hasMachine() || !ProjectorMachines.validFacings(plan.machine)
            .test(plan.facing)) return;
        ProjectorMaterialCrafting.request(player, plan);
    }

    private static ProjectorPlan readPlan(NBTTagCompound tag) {
        ProjectorPlan plan = new ProjectorPlan();
        plan.readFromNBT(tag);
        if (plan.hasMachine() && !ProjectorMachines.isSupported(plan.machine)) plan.clear();
        plan.overrides.values()
            .removeIf(id -> id != ProjectorPlan.CASING && !ProjectorMachines.isHatch(id));
        plan.batchOverrides.values()
            .removeIf(id -> id != ProjectorPlan.CASING && !ProjectorMachines.isHatch(id));
        return plan;
    }

    public static void openGui(EntityPlayerMP player, int location) {
        if (projectorAt(player, location) == null) return;
        var previous = player.openContainer;
        var factory = GuiFactories.playerInventory();
        if (location < 0) factory.openFromBaubles(player, -location - 1);
        else if (location == 0) factory.openFromMainHand(player);
        else factory.openFromPlayerInventory(player, location - 1);
        ProjectorArmSessions.opened(player, previous);
    }
}
