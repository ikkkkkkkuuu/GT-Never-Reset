package com.xyp.gtneverreset.common.item.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosChain;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.ItemMachines;

public final class ChaosStructureModuleItem extends Item {

    public static final String REGISTRY_NAME = "chaos_structure_module";

    public ChaosStructureModuleItem() {
        // #tr item.chaos_structure_module.name
        // # Chaos Structure Module
        // # zh_CN 混沌结构模组
        setUnlocalizedName(REGISTRY_NAME);
        setTextureName(GTNeverReset.MODID + ":chaos_structure_module");
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        setMaxStackSize(1);
    }

    public static MTEBasicMachine source(ItemStack controller) {
        if (controller == null || !(controller.getItem() instanceof ItemMachines)) return null;
        int id = controller.getItemDamage();
        if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.length) return null;
        if (!(GregTechAPI.METATILEENTITIES[id] instanceof MTEBasicMachine machine)) return null;
        RecipeMap<?> map = machine.getRecipeMap();
        return map != null && ChaosChain.SUPPORTED_MAPS.contains(map) ? machine : null;
    }

    public static MTEBasicMachine unpack(ItemStack module) {
        if (module == null || !(module.getItem() instanceof ChaosStructureModuleItem) || !module.hasTagCompound())
            return null;
        return source(GTUtility.loadItem(module.getTagCompound(), "machine"));
    }

    public static ItemStack pack(ItemStack blank, ItemStack controller) {
        if (source(controller) == null || blank == null
            || !(blank.getItem() instanceof ChaosStructureModuleItem)
            || unpack(blank) != null) throw new IllegalArgumentException("gtnr.chaos.error.module");
        ItemStack result = blank.copy();
        result.stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();
        ItemStack machine = controller.copy();
        machine.stackSize = 1;
        GTUtility.saveItem(tag, "machine", machine);
        result.setTagCompound(tag);
        return result;
    }

    public static long voltage(ItemStack module, RecipeMap<?> map) {
        MTEBasicMachine machine = unpack(module);
        return machine != null && machine.getRecipeMap() == map ? GTValues.V[machine.mTier] : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        MTEBasicMachine machine = unpack(stack);
        if (machine != null) {
            lines.add(machine.getLocalName());
            lines.add(
                StatCollector.translateToLocal(machine.getRecipeMap().unlocalizedName) + " / "
                    + GTValues.VN[machine.mTier]);
        }
        // #tr gtnr.chaos.module.help
        // # Package a supported GT machine with a blank module and 8 solid steel casings in the workbench.
        // # zh_CN 在工作区用空模组、支持的 GT 机器和 8 个坚固钢机器外壳封装。
        lines.add(StatCollector.translateToLocal("gtnr.chaos.module.help"));
    }
}
