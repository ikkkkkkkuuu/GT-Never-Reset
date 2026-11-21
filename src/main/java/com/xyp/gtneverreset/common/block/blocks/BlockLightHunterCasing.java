package com.xyp.gtneverreset.common.block.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IBlockWithActiveOffset;

public final class BlockLightHunterCasing extends Block implements IBlockWithActiveOffset {

    public enum Variant {

        // #tr tile.gtneverreset.light_hunter.neutronium_pipe_casing.name
        // # Neutronium Pipe Casing
        // # zh_CN 中子素管道方块
        NEUTRONIUM_PIPE_CASING("neutronium_pipe_casing"),
        // #tr tile.gtneverreset.light_hunter.hyper_mechanical_casing.name
        // # Hyper Mechanical Casing
        // # zh_CN 超能机械方块
        HYPER_MECHANICAL("hyper_mechanical_casing", true),
        // #tr tile.gtneverreset.light_hunter.fusion_glass.name
        // # Fusion Glass
        // # zh_CN 聚变玻璃
        FUSION_GLASS("fusion_glass", true),
        // #tr tile.gtneverreset.light_hunter.atomic_casing.name
        // # Atomic Casing
        // # zh_CN 原子机械方块
        ATOMIC_CASING("atomic_casing", true),
        // #tr tile.gtneverreset.light_hunter.extreme_density_casing.name
        // # Extreme Density Casing
        // # zh_CN 极密机械方块
        EXTREME_DENSITY_CASING("extreme_density_casing"),
        // #tr tile.gtneverreset.light_hunter.casings_echo_casing.name
        // # Echo Reinforced Machine Casing
        // # zh_CN 回响强化机械方块
        ECHO_CASING("casings_echo_casing", true),
        // #tr tile.gtneverreset.light_hunter.casings_extreme_strength_tritanium_casing.name
        // # Dragon Strength Tritanium Machine Casing
        // # zh_CN 龙之力量三钛合金机械方块
        DRAGON_STRENGTH_TRITANIUM_CASING("casings_extreme_strength_tritanium_casing", true),
        // #tr tile.gtneverreset.light_hunter.hyper_core.name
        // # Hyper Core
        // # zh_CN 超能核心
        HYPER_CORE("hyper_core"),
        // #tr tile.gtneverreset.light_hunter.restraint_device.name
        // # Restraint Device
        // # zh_CN 力场约束装置
        RESTRAINT_DEVICE("restraint_device"),
        // #tr tile.gtneverreset.light_hunter.spacetime_assembly_line_unit.name
        // # Spacetime Assembly Line Unit
        // # zh_CN 超时空装配单元
        SPACETIME_ASSEMBLY_LINE_UNIT("spacetime_assembly_line_unit"),
        // #tr tile.gtneverreset.light_hunter.casings_fusion_fusion_casing_mk5.name
        // # Fusion Machine Casing MK-V
        // # zh_CN 聚变机械方块 MK-V
        FUSION_CASING_MK_V("casings_fusion_fusion_casing_mk5", true),
        // #tr tile.gtneverreset.light_hunter.enhanced_hyper_casing.name
        // # Enhanced Hyper Mechanical Casing
        // # zh_CN 强化超能机械方块
        ENHANCED_HYPER_CASING("enhanced_hyper_casing", true),
        // #tr tile.gtneverreset.light_hunter.improved_superconductor_coil.name
        // # Improved Superconductor Coil
        // # zh_CN 改良型超导线圈方块
        IMPROVED_SUPERCONDUCTOR("improved_superconductor_coil", true),
        // #tr tile.gtneverreset.light_hunter.gravity_stabilization_casing.name
        // # Gravity Stabilization Casing
        // # zh_CN 重力稳定机械方块
        GRAVITY_STABILIZATION_CASING("gravity_stabilization_casing"),
        // #tr tile.gtneverreset.light_hunter.module_connector.name
        // # Module Connector
        // # zh_CN 太空电梯模块连接器
        MODULE_CONNECTOR("module_connector"),
        // #tr tile.gtneverreset.light_hunter.casings_hpca_damaged_advanced_hpca_component_side.name
        // # Super Computation Component
        // # zh_CN 超级计算机组件
        SUPER_COMPUTATION_COMPONENT("casings_hpca_damaged_advanced_hpca_component_side"),
        // #tr tile.gtneverreset.light_hunter.casings_battery_uhv_ultimate_side.name
        // # UHV Ultimate Capacitor
        // # zh_CN UHV终极电池
        UHV_ULTIMATE_BATTERY("casings_battery_uhv_ultimate_side");

        private final String texture;
        private BlockLightHunterCasing block;
        private final boolean connected;

        Variant(String texture) {
            this(texture, false);
        }

        Variant(String texture, boolean connected) {
            this.texture = texture;
            this.connected = connected;
        }

        public ItemStack stack(int amount) {
            return new ItemStack(block, amount);
        }
    }

    private final Variant variant;
    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private IIcon[] connectedIcons;
    @SideOnly(Side.CLIENT)
    private IIcon batteryEnd;
    @SideOnly(Side.CLIENT)
    private IIcon activeIcon;
    @SideOnly(Side.CLIENT)
    private IIcon[] activeConnectedIcons;

    private BlockLightHunterCasing(Variant variant) {
        super(variant == Variant.FUSION_GLASS ? Material.glass : Material.iron);
        this.variant = variant;
        glass = variant == Variant.FUSION_GLASS;
        setHardness(5);
        setResistance(10);
        setBlockName("gtneverreset.light_hunter." + variant.texture);
        setBlockTextureName(GTNeverReset.MODID + ":lighthunter/" + variant.texture);
        setCreativeTab(GTNRCreativeTabs.BLOCKS);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        if (glass) setLightOpacity(0);
        setHarvestLevel("wrench", 2);
        GregTechAPI.registerMachineBlock(this, 1);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        if (variant == Variant.UHV_ULTIMATE_BATTERY)
            batteryEnd = register.registerIcon(GTNeverReset.MODID + ":lighthunter/casings_battery_uhv_ultimate_top");
        if (variant == Variant.SUPER_COMPUTATION_COMPONENT) activeIcon = register
            .registerIcon(GTNeverReset.MODID + ":lighthunter/damaged_advanced_hpca_component_side");
        if (!variant.connected) return;
        connectedIcons = registerConnected(register, variant.texture);
        if (variant == Variant.IMPROVED_SUPERCONDUCTOR)
            activeConnectedIcons = registerConnected(register, "improved_superconductor_coil_active");
    }

    @SideOnly(Side.CLIENT)
    private static IIcon[] registerConnected(IIconRegister register, String texture) {
        IIcon[] icons = new IIcon[256];
        for (int mask = 0; mask < 256; mask++) {
            if (ConnectedCasingFaces.normalizeMask(mask) == mask)
                icons[mask] = register.registerIcon(GTNeverReset.MODID + ":lighthunter/ctm/" + texture + "/" + mask);
        }
        return icons;
    }

    @Override
    public int getActiveOffset() {
        return 8;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (meta == 8 && activeIcon != null) return activeIcon;
        return batteryEnd != null && side < 2 ? batteryEnd : blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (connectedIcons == null) return getIcon(side, world.getBlockMetadata(x, y, z));
        int mask = ConnectedCasingFaces.getMask(world, x, y, z, side, this::matchesFace);
        return activeConnectedIcons != null && world.getBlockMetadata(x, y, z) == 8 ? activeConnectedIcons[mask]
            : connectedIcons[mask];
    }

    private boolean matchesFace(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) == this;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return !glass;
    }

    @Override
    public boolean isOpaqueCube() {
        return !glass;
    }

    @Override
    public int getRenderBlockPass() {
        return glass ? 1 : 0;
    }

    public static void register() {
        for (Variant variant : Variant.values()) {
            variant.block = new BlockLightHunterCasing(variant);
            GameRegistry.registerBlock(variant.block, "light_hunter_" + variant.texture);
        }
    }
}
