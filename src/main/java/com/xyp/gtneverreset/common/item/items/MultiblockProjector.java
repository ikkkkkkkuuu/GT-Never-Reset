package com.xyp.gtneverreset.common.item.items;

import static net.minecraft.util.EnumChatFormatting.AQUA;
import static net.minecraft.util.EnumChatFormatting.GRAY;
import static net.minecraft.util.EnumChatFormatting.YELLOW;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.gui.ProjectorMaterialSync;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MultiblockProjector extends Item implements IBaubleExpanded, IGuiHolder<PlayerInventoryGuiData> {

    public static final String LANG = "item.gtneverreset.multiblock_projector.";
    public static final int GUI_WIDTH = 480, GUI_HEIGHT = 300;

    public MultiblockProjector() {
        setMaxStackSize(1);
        // #tr item.gtneverreset.multiblock_projector.name
        // # Multiblock Projector
        // # zh_CN 多方块投影仪
        setUnlocalizedName("gtneverreset.multiblock_projector");
        setTextureName("gtneverreset:multiblock_projector");
        setCreativeTab(GTNRCreativeTabs.ITEMS);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        var materials = new ProjectorMaterialSync(data).allowC2S();
        syncManager.syncValue("materials", materials);
        return syncManager.isClient() ? createPanel(data, materials)
            : new ModularPanel("multiblock_projector").size(GUI_WIDTH, GUI_HEIGHT);
    }

    @SideOnly(Side.CLIENT)
    private ModularPanel createPanel(PlayerInventoryGuiData data, ProjectorMaterialSync materials) {
        return new ProjectorGui(data, materials);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return ((ProjectorGui) panel).screen();
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        return new String[] { BaubleExpandedSlots.universalType };
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {}

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase player) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase player) {}

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        // Returning true on the client keeps the click from reaching the block and the server.
        if (!world.isRemote) return false;
        clientClick(player);
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) clientClick(player);
        return stack;
    }

    @SideOnly(Side.CLIENT)
    private static void clientClick(EntityPlayer player) {
        ProjectorClient.rightClick(player.isSneaking());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        ProjectorPlan plan = ProjectorPlan.read(stack);
        if (plan.hasMachine()) {
            ItemStack machine = ProjectorMachines.stackOf(plan.machine);
            tooltip.add(
                AQUA + StatCollector.translateToLocalFormatted(
                    // #tr item.gtneverreset.multiblock_projector.machine
                    // # Plan: %s (%s)
                    // # zh_CN 当前方案：%s（%s）
                    LANG + "machine",
                    machine == null ? "?" : machine.getDisplayName(),
                    // #tr item.gtneverreset.multiblock_projector.state.IDLE
                    // # not projected
                    // # zh_CN 未投影
                    // #tr item.gtneverreset.multiblock_projector.state.FOLLOW
                    // # following the crosshair
                    // # zh_CN 跟随准星
                    // #tr item.gtneverreset.multiblock_projector.state.LOCKED
                    // # locked
                    // # zh_CN 已锁定
                    // #tr item.gtneverreset.multiblock_projector.state.ANCHORED
                    // # anchored
                    // # zh_CN 已锚定
                    StatCollector.translateToLocal(LANG + "state." + plan.state.name())));
        } else {
            // #tr item.gtneverreset.multiblock_projector.no_machine
            // # No machine selected
            // # zh_CN 还没有选择机器
            tooltip.add(GRAY + StatCollector.translateToLocal(LANG + "no_machine"));
        }
        // #tr item.gtneverreset.multiblock_projector.bauble_hint
        // # Fits the universal bauble slot; when worn, lock it with the "Projector: lock / follow" key
        // # zh_CN 可以放进饰品栏的万能槽；戴着时用“投影仪：锁定 / 跟随”键锁定
        tooltip.add(GRAY + StatCollector.translateToLocal(LANG + "bauble_hint"));
        if (GuiScreen.isShiftKeyDown()) {
            for (int line = 0; line < 7; line++)
                // #tr item.gtneverreset.multiblock_projector.tooltip.0
                // # Right-click: open the projector and pick a machine
                // # zh_CN 右键：打开投影仪界面，选择机器
                // #tr item.gtneverreset.multiblock_projector.tooltip.1
                // # Project follows the crosshair. Right-click to lock, sneak-right-click to cancel.
                // # zh_CN 点"投影到世界"后投影跟随准星；右键锁定，潜行右键取消
                // #tr item.gtneverreset.multiblock_projector.tooltip.2
                // # Locked: right-click opens the projector, sneak-right-click unlocks, arrow keys move it by one block
                // # zh_CN 锁定后：右键打开界面，潜行右键解锁，方向键平移一格
                // #tr item.gtneverreset.multiblock_projector.tooltip.3
                // # Middle-click a locked cell to change its hatch, block or coil tier.
                // # zh_CN 中键：锁定后点投影格子换仓或换方块；点线圈这类分级方块选等级
                // #tr item.gtneverreset.multiblock_projector.tooltip.4
                // # R rotates, Shift+R mirrors, Alt+R opens the pose panel, N switches what is shown
                // # zh_CN R 旋转，Shift+R 镜像，Alt+R 姿态面板，N 切换显示
                // #tr item.gtneverreset.multiblock_projector.tooltip.5
                // # Alt+mouse wheel or Alt+Up/Down moves it up or down
                // # zh_CN Alt+滚轮或 Alt+↑↓：上下移动
                // #tr item.gtneverreset.multiblock_projector.tooltip.6
                // # The projection closes by itself once every block is in place
                // # zh_CN 方块全部搭齐后投影自动收起
                tooltip.add(GRAY + StatCollector.translateToLocal(LANG + "tooltip." + line));
        } else {
            // #tr item.gtneverreset.multiblock_projector.more_hint
            // # Hold Shift for controls
            // # zh_CN 按住 Shift 查看操作
            tooltip.add(YELLOW + StatCollector.translateToLocal(LANG + "more_hint"));
        }
    }
}
