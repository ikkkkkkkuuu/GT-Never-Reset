package com.xyp.gtneverreset.common.block.blocks.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class ItemBlockChaosCasing extends ItemBlock {

    public ItemBlockChaosCasing(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String key = switch (getUnlocalizedName(stack)) {
            // #tr tile.gtneverreset.chaos_casing_01.1.name
            // # High Power Radiation Proof Casing
            // # zh_CN 高能防辐射机械方块
            case "tile.gtneverreset.chaos_casing_01.1" -> "tile.gtneverreset.chaos_casing_01.1.name";
            // #tr tile.gtneverreset.chaos_casing_01.2.name
            // # Advanced High Power Coil Block
            // # zh_CN 进阶高能线圈
            case "tile.gtneverreset.chaos_casing_01.2" -> "tile.gtneverreset.chaos_casing_01.2.name";
            // #tr tile.gtneverreset.chaos_casing_02.2.name
            // # Iridium-Reinforced Neutronium Casing
            // # zh_CN 铱强化中子机械方块
            case "tile.gtneverreset.chaos_casing_02.2" -> "tile.gtneverreset.chaos_casing_02.2.name";
            // #tr tile.gtneverreset.chaos_casing_02.4.name
            // # Neutronium Pipe Casing
            // # zh_CN 中子管道方块
            case "tile.gtneverreset.chaos_casing_02.4" -> "tile.gtneverreset.chaos_casing_02.4.name";
            // #tr tile.gtneverreset.chaos_casing_02.7.name
            // # Cryogenic Cooling Casing
            // # zh_CN 低温冷却机械方块
            case "tile.gtneverreset.chaos_casing_02.7" -> "tile.gtneverreset.chaos_casing_02.7.name";
            // #tr tile.gtneverreset.chaos_casing_02.8.name
            // # Neutronium Load-Bearing Casing
            // # zh_CN 中子承载机械方块
            case "tile.gtneverreset.chaos_casing_02.8" -> "tile.gtneverreset.chaos_casing_02.8.name";
            // #tr tile.gtneverreset.chaos_glass.0.name
            // # Energy Conduction Glass
            // # zh_CN 能量传导玻璃
            case "tile.gtneverreset.chaos_glass.0" -> "tile.gtneverreset.chaos_glass.0.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.0.name
            // # Photonic Intensifier LV Tier
            // # zh_CN 光量子增幅器LV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.0" -> "tile.gtneverreset.chaos_photon_upgrade.0.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.1.name
            // # Photonic Intensifier MV Tier
            // # zh_CN 光量子增幅器MV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.1" -> "tile.gtneverreset.chaos_photon_upgrade.1.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.2.name
            // # Photonic Intensifier HV Tier
            // # zh_CN 光量子增幅器HV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.2" -> "tile.gtneverreset.chaos_photon_upgrade.2.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.3.name
            // # Photonic Intensifier EV Tier
            // # zh_CN 光量子增幅器EV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.3" -> "tile.gtneverreset.chaos_photon_upgrade.3.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.4.name
            // # Photonic Intensifier IV Tier
            // # zh_CN 光量子增幅器IV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.4" -> "tile.gtneverreset.chaos_photon_upgrade.4.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.5.name
            // # Photonic Intensifier LuV Tier
            // # zh_CN 光量子增幅器LuV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.5" -> "tile.gtneverreset.chaos_photon_upgrade.5.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.6.name
            // # Photonic Intensifier ZPM Tier
            // # zh_CN 光量子增幅器ZPM Tier
            case "tile.gtneverreset.chaos_photon_upgrade.6" -> "tile.gtneverreset.chaos_photon_upgrade.6.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.7.name
            // # Photonic Intensifier UV Tier
            // # zh_CN 光量子增幅器UV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.7" -> "tile.gtneverreset.chaos_photon_upgrade.7.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.8.name
            // # Photonic Intensifier UHV Tier
            // # zh_CN 光量子增幅器UHV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.8" -> "tile.gtneverreset.chaos_photon_upgrade.8.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.9.name
            // # Photonic Intensifier UEV Tier
            // # zh_CN 光量子增幅器UEV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.9" -> "tile.gtneverreset.chaos_photon_upgrade.9.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.10.name
            // # Photonic Intensifier UIV Tier
            // # zh_CN 光量子增幅器UIV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.10" -> "tile.gtneverreset.chaos_photon_upgrade.10.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.11.name
            // # Photonic Intensifier UMV Tier
            // # zh_CN 光量子增幅器UMV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.11" -> "tile.gtneverreset.chaos_photon_upgrade.11.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.12.name
            // # Photonic Intensifier UXV Tier
            // # zh_CN 光量子增幅器UXV Tier
            case "tile.gtneverreset.chaos_photon_upgrade.12" -> "tile.gtneverreset.chaos_photon_upgrade.12.name";
            // #tr tile.gtneverreset.chaos_photon_upgrade.13.name
            // # Photonic Intensifier MAX Tier
            // # zh_CN 光量子增幅器MAX Tier
            case "tile.gtneverreset.chaos_photon_upgrade.13" -> "tile.gtneverreset.chaos_photon_upgrade.13.name";
            // #tr tile.gtneverreset.chaos_energy_matrix.0.name
            // # Energy Sustainment Matrix Framework
            // # zh_CN 能量维持矩阵框架
            case "tile.gtneverreset.chaos_energy_matrix.0" -> "tile.gtneverreset.chaos_energy_matrix.0.name";
            // #tr tile.gtneverreset.chaos_energy_matrix.1.name
            // # Energy Sustainment Matrix UEV Tier
            // # zh_CN 能量维持矩阵(UEV)
            case "tile.gtneverreset.chaos_energy_matrix.1" -> "tile.gtneverreset.chaos_energy_matrix.1.name";
            // #tr tile.gtneverreset.chaos_energy_matrix.2.name
            // # Energy Sustainment Matrix UIV Tier
            // # zh_CN 能量维持矩阵(UIV)
            case "tile.gtneverreset.chaos_energy_matrix.2" -> "tile.gtneverreset.chaos_energy_matrix.2.name";
            // #tr tile.gtneverreset.chaos_energy_matrix.3.name
            // # Energy Sustainment Matrix UMV Tier
            // # zh_CN 能量维持矩阵(UMV)
            case "tile.gtneverreset.chaos_energy_matrix.3" -> "tile.gtneverreset.chaos_energy_matrix.3.name";
            // #tr tile.gtneverreset.chaos_energy_matrix.4.name
            // # Energy Sustainment Matrix UXV Tier
            // # zh_CN 能量维持矩阵(UXV)
            case "tile.gtneverreset.chaos_energy_matrix.4" -> "tile.gtneverreset.chaos_energy_matrix.4.name";
            // #tr tile.gtneverreset.chaos_field_generator.0.name
            // # Stabilisation Field Generator Framework
            // # zh_CN 稳定力场发生器框架
            case "tile.gtneverreset.chaos_field_generator.0" -> "tile.gtneverreset.chaos_field_generator.0.name";
            // #tr tile.gtneverreset.chaos_field_generator.1.name
            // # Stabilisation Field Generator UEV Tier
            // # zh_CN 稳定力场发生器(UEV)
            case "tile.gtneverreset.chaos_field_generator.1" -> "tile.gtneverreset.chaos_field_generator.1.name";
            // #tr tile.gtneverreset.chaos_field_generator.2.name
            // # Stabilisation Field Generator UIV Tier
            // # zh_CN 稳定力场发生器(UIV)
            case "tile.gtneverreset.chaos_field_generator.2" -> "tile.gtneverreset.chaos_field_generator.2.name";
            // #tr tile.gtneverreset.chaos_field_generator.3.name
            // # Stabilisation Field Generator UMV Tier
            // # zh_CN 稳定力场发生器(UMV)
            case "tile.gtneverreset.chaos_field_generator.3" -> "tile.gtneverreset.chaos_field_generator.3.name";
            // #tr tile.gtneverreset.chaos_field_generator.4.name
            // # Stabilisation Field Generator UXV Tier
            // # zh_CN 稳定力场发生器(UXV)
            case "tile.gtneverreset.chaos_field_generator.4" -> "tile.gtneverreset.chaos_field_generator.4.name";
            // #tr tile.gtnr.bottleinchaos.capacity_block.0.name
            // # Bottle in Chaos Capacity Block I
            // # zh_CN 混沌中瓶容量方块 I
            case "tile.gtnr.bottleinchaos.capacity_block.0" -> "tile.gtnr.bottleinchaos.capacity_block.0.name";
            // #tr tile.gtnr.bottleinchaos.capacity_block.1.name
            // # Bottle in Chaos Capacity Block II
            // # zh_CN 混沌中瓶容量方块 II
            case "tile.gtnr.bottleinchaos.capacity_block.1" -> "tile.gtnr.bottleinchaos.capacity_block.1.name";
            // #tr tile.gtnr.bottleinchaos.capacity_block.2.name
            // # Bottle in Chaos Capacity Block III
            // # zh_CN 混沌中瓶容量方块 III
            case "tile.gtnr.bottleinchaos.capacity_block.2" -> "tile.gtnr.bottleinchaos.capacity_block.2.name";
            // #tr tile.gtnr.bottleinchaos.capacity_block.3.name
            // # Bottle in Chaos Capacity Block IV
            // # zh_CN 混沌中瓶容量方块 IV
            case "tile.gtnr.bottleinchaos.capacity_block.3" -> "tile.gtnr.bottleinchaos.capacity_block.3.name";
            // #tr tile.gtnr.bottleinchaos.capacity_block.4.name
            // # Bottle in Chaos Capacity Block V
            // # zh_CN 混沌中瓶容量方块 V
            case "tile.gtnr.bottleinchaos.capacity_block.4" -> "tile.gtnr.bottleinchaos.capacity_block.4.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.0.name
            // # Multi-Purpose Core LV
            // # zh_CN 多用途核心 LV
            case "tile.gtneverreset.chaos_multi_use_core.0" -> "tile.gtneverreset.chaos_multi_use_core.0.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.1.name
            // # Multi-Purpose Core MV
            // # zh_CN 多用途核心 MV
            case "tile.gtneverreset.chaos_multi_use_core.1" -> "tile.gtneverreset.chaos_multi_use_core.1.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.2.name
            // # Multi-Purpose Core HV
            // # zh_CN 多用途核心 HV
            case "tile.gtneverreset.chaos_multi_use_core.2" -> "tile.gtneverreset.chaos_multi_use_core.2.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.3.name
            // # Multi-Purpose Core EV
            // # zh_CN 多用途核心 EV
            case "tile.gtneverreset.chaos_multi_use_core.3" -> "tile.gtneverreset.chaos_multi_use_core.3.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.4.name
            // # Multi-Purpose Core IV
            // # zh_CN 多用途核心 IV
            case "tile.gtneverreset.chaos_multi_use_core.4" -> "tile.gtneverreset.chaos_multi_use_core.4.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.5.name
            // # Multi-Purpose Core LuV
            // # zh_CN 多用途核心 LuV
            case "tile.gtneverreset.chaos_multi_use_core.5" -> "tile.gtneverreset.chaos_multi_use_core.5.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.6.name
            // # Multi-Purpose Core ZPM
            // # zh_CN 多用途核心 ZPM
            case "tile.gtneverreset.chaos_multi_use_core.6" -> "tile.gtneverreset.chaos_multi_use_core.6.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.7.name
            // # Multi-Purpose Core UV
            // # zh_CN 多用途核心 UV
            case "tile.gtneverreset.chaos_multi_use_core.7" -> "tile.gtneverreset.chaos_multi_use_core.7.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.8.name
            // # Multi-Purpose Core UHV
            // # zh_CN 多用途核心 UHV
            case "tile.gtneverreset.chaos_multi_use_core.8" -> "tile.gtneverreset.chaos_multi_use_core.8.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.9.name
            // # Multi-Purpose Core UEV
            // # zh_CN 多用途核心 UEV
            case "tile.gtneverreset.chaos_multi_use_core.9" -> "tile.gtneverreset.chaos_multi_use_core.9.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.10.name
            // # Multi-Purpose Core UIV
            // # zh_CN 多用途核心 UIV
            case "tile.gtneverreset.chaos_multi_use_core.10" -> "tile.gtneverreset.chaos_multi_use_core.10.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.11.name
            // # Multi-Purpose Core UMV
            // # zh_CN 多用途核心 UMV
            case "tile.gtneverreset.chaos_multi_use_core.11" -> "tile.gtneverreset.chaos_multi_use_core.11.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.12.name
            // # Multi-Purpose Core UXV
            // # zh_CN 多用途核心 UXV
            case "tile.gtneverreset.chaos_multi_use_core.12" -> "tile.gtneverreset.chaos_multi_use_core.12.name";
            // #tr tile.gtneverreset.chaos_multi_use_core.13.name
            // # Multi-Purpose Core MAX
            // # zh_CN 多用途核心 MAX
            case "tile.gtneverreset.chaos_multi_use_core.13" -> "tile.gtneverreset.chaos_multi_use_core.13.name";
            default -> getUnlocalizedName(stack) + ".name";
        };
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    @Override
    public int getMetadata(int meta) {
        return meta;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return field_150939_a.getUnlocalizedName() + "." + stack.getItemDamage();
    }
}
