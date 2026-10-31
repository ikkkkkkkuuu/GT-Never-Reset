package com.xyp.gtneverreset.common.machine.hatch;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.gui.modularui.hatch.ModuleParameterGui;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class ParameterModuleHatch extends MTEHatch {

    // #tr gtnr.module.parameter.name
    // # Parameter Module
    // # zh_CN 参数模块
    public static final String NAME_KEY = "gtnr.module.parameter.name";

    private static final int[] PARALLEL = { 8, 128, 2048, 32768, 524288, 8388608, 134217728, Integer.MAX_VALUE };
    private static final double[] POWER = { 0.95, 0.9, 0.85, 0.8, 0.75, 0.7, 0.5, 0.25 };
    private static final Map<String, IIconContainer> OVERLAYS = new HashMap<>();
    private final MachineModuleType moduleType;
    private final int moduleTier;
    private final boolean dynamic;
    private int parameter = 1;

    public ParameterModuleHatch(int id, String name, MachineModuleType type, int moduleTier, boolean dynamic) {
        super(id, name, "", moduleTier, 0, new String[0]);
        validateModule(type, moduleTier, dynamic);
        this.moduleType = type;
        this.moduleTier = moduleTier;
        this.dynamic = dynamic;
    }

    private ParameterModuleHatch(String name, MachineModuleType type, int moduleTier, boolean dynamic,
        ITexture[][][] textures) {
        super(name, moduleTier, 0, new String[0], textures);
        validateModule(type, moduleTier, dynamic);
        this.moduleType = type;
        this.moduleTier = moduleTier;
        this.dynamic = dynamic;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new ParameterModuleHatch(mName, getModuleType(), moduleTier, dynamic, mTextures);
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public int getParameter() {
        return dynamic ? parameter : getMaximumParameter();
    }

    public int getMaximumParameter() {
        if (getModuleType() == MachineModuleType.Overclock) return 1;
        return getModuleType() == MachineModuleType.Parallel ? PARALLEL[moduleTier - 1] : 1 << moduleTier;
    }

    public void setParameter(int value) {
        if (!dynamic) return;
        parameter = Math.max(1, Math.min(getMaximumParameter(), value));
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) markDirty();
    }

    public double getPowerMultiplier() {
        return getModuleType() == MachineModuleType.PowerConsumption ? POWER[moduleTier - 1] : 1;
    }

    public int getTimeReduction() {
        return moduleTier == 1 ? 4 : 8;
    }

    public int getPowerIncrease() {
        return moduleTier == 3 ? 2 : 4;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        if (dynamic) tag.setInteger("moduleParameter", parameter);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (dynamic && tag.hasKey("moduleParameter")) {
            parameter = Math.max(1, Math.min(getMaximumParameter(), tag.getInteger("moduleParameter")));
        }
    }

    @Override
    public String getLocalName() {
        return switch (getModuleType()) {
            case Parallel -> parallelName();
            case Speed -> speedName();
            case PowerConsumption -> powerName();
            case Overclock -> overclockName();
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
    }

    private String parallelName() {
        if (dynamic) {
            // #tr gtnr.module.parallel.dynamic.name
            // # Dynamic Parallel Controller Module T%s
            // # zh_CN 动态并行控制器模块 T%s
            return StatCollector.translateToLocalFormatted("gtnr.module.parallel.dynamic.name", moduleTier);
        }
        // #tr gtnr.module.parallel.static.name
        // # Static Parallel Controller Module T%s
        // # zh_CN 静态并行控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtnr.module.parallel.static.name", moduleTier);
    }

    private String speedName() {
        if (dynamic) {
            // #tr gtnr.module.speed.dynamic.name
            // # Dynamic Speed Controller Module T%s
            // # zh_CN 动态速度控制器模块 T%s
            return StatCollector.translateToLocalFormatted("gtnr.module.speed.dynamic.name", moduleTier);
        }
        // #tr gtnr.module.speed.static.name
        // # Static Speed Controller Module T%s
        // # zh_CN 静态速度控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtnr.module.speed.static.name", moduleTier);
    }

    private String powerName() {
        // #tr gtnr.module.power.name
        // # Power Consumption Controller Module T%s
        // # zh_CN 能耗控制器模块 T%s
        return StatCollector.translateToLocalFormatted("gtnr.module.power.name", moduleTier);
    }

    private String overclockName() {
        if (moduleTier == 1) {
            // #tr gtnr.module.overclock.low.name
            // # Low Speed Perfect Overclock Controller Module
            // # zh_CN 低速无损超频控制器模块
            return StatCollector.translateToLocal("gtnr.module.overclock.low.name");
        }
        if (moduleTier == 2) {
            // #tr gtnr.module.overclock.perfect.name
            // # Perfect Overclock Controller Module
            // # zh_CN 无损超频控制器模块
            return StatCollector.translateToLocal("gtnr.module.overclock.perfect.name");
        }
        // #tr gtnr.module.overclock.singularity.name
        // # Singularity Perfect Overclock Controller Module
        // # zh_CN 奇点无损超频控制器模块
        return StatCollector.translateToLocal("gtnr.module.overclock.singularity.name");
    }

    @Override
    public String[] getDescription() {
        // #tr gtnr.module.parameter.description
        // # Adjusts processing parameters of compatible multiblocks
        // # zh_CN 调整兼容多方块机器的处理参数
        String purpose = StatCollector.translateToLocal("gtnr.module.parameter.description");
        // #tr gtnr.module.parameter.limit
        // # One module of each parameter type per machine
        // # zh_CN 每台机器的每类参数模块限装一个
        String limit = StatCollector.translateToLocal("gtnr.module.parameter.limit");
        String effect = switch (getModuleType()) {
            case Parallel -> parallelDescription();
            case Speed -> speedDescription();
            case PowerConsumption -> powerDescription();
            case Overclock -> overclockDescription();
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
        if (dynamic) {
            // #tr gtnr.module.parameter.controls
            // # Right-click to set the parameter; changes apply to new recipe tasks
            // # zh_CN 右键设置参数；修改对新配方任务生效
            return new String[] { purpose, effect, limit,
                StatCollector.translateToLocal("gtnr.module.parameter.controls") };
        }
        return new String[] { purpose, effect, limit };
    }

    private String parallelDescription() {
        // #tr gtnr.module.parallel.effect
        // # Parallel limit: base x (1 + parameter); parameter up to {\\AQUA}%s{\\RESET}
        // # zh_CN 并行上限：基础值 x（1 + 参数）；参数最高 {\\AQUA}%s{\\RESET}
        return StatCollector.translateToLocalFormatted(
            "gtnr.module.parallel.effect",
            NumberFormatUtil.formatNumber(getMaximumParameter()));
    }

    private String speedDescription() {
        // #tr gtnr.module.speed.effect
        // # Processing speed multiplier: up to {\\AQUA}%sx{\\RESET}
        // # zh_CN 处理速度倍率：最高 {\\AQUA}%s 倍{\\RESET}
        return StatCollector.translateToLocalFormatted("gtnr.module.speed.effect", getMaximumParameter());
    }

    private String powerDescription() {
        // #tr gtnr.module.power.effect
        // # EU/t multiplier: {\\AQUA}%s%%{\\RESET}
        // # zh_CN EU/t 倍率：{\\AQUA}%s%%{\\RESET}
        return StatCollector
            .translateToLocalFormatted("gtnr.module.power.effect", Math.round(getPowerMultiplier() * 100));
    }

    private String overclockDescription() {
        // #tr gtnr.module.overclock.effect
        // # Each overclock: processing time / {\\AQUA}%s{\\RESET}, EU/t x {\\YELLOW}%s{\\RESET}
        // # zh_CN 每次超频：耗时 / {\\AQUA}%s{\\RESET}，EU/t x {\\YELLOW}%s{\\RESET}
        return StatCollector
            .translateToLocalFormatted("gtnr.module.overclock.effect", getTimeReduction(), getPowerIncrease());
    }

    private String getOverlayName(boolean active) {
        return switch (getModuleType()) {
            case Parallel -> "OVERLAY_ParallelController";
            case Speed -> "OVERLAY_SpeedController";
            case PowerConsumption -> "OVERLAY_PowerController";
            case Overclock -> "OVERLAY_OverClockController_" + switch (moduleTier) {
                    case 1 -> "Pri";
                    case 2 -> "Adv";
                    default -> "Per";
                };
            default -> throw new IllegalStateException("Unexpected parameter module");
        };
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (dynamic && tile.isServerSide()) openGui(player);
        return true;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new ModuleParameterGui(this).build(data, sync, settings);
    }

    public final void setWorking(boolean working) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide() && tile.isActive() != working) tile.setActive(working);
    }

    public MachineModuleType getModuleType() {
        return moduleType;
    }

    private static void validateModule(MachineModuleType type, int tier, boolean dynamic) {
        int maxTier = type == MachineModuleType.Overclock ? 3 : 8;
        if (type == null || tier < 1
            || tier > maxTier
            || (dynamic && type != MachineModuleType.Parallel && type != MachineModuleType.Speed)) {
            throw new IllegalArgumentException("Invalid parameter module");
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        String prefix = GTNeverReset.MODID + ":modular/";
        registerOverlay(prefix, getOverlayName(true));
        registerOverlay(prefix, getOverlayName(false));
        super.registerIcons(register);
    }

    @SideOnly(Side.CLIENT)
    private static void registerOverlay(String prefix, String name) {
        if (!OVERLAYS.containsKey(name)) OVERLAYS.put(name, Textures.BlockIcons.custom(prefix + name));
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAYS.get(getOverlayName(true))) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAYS.get(getOverlayName(false))) };
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean isLiquidInput(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean willExplodeInRain() {
        return false;
    }
}
