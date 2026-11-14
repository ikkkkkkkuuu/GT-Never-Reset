package com.xyp.gtnrlib.compat.gregtech;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.ToolboxMotion;
import com.xyp.gtnrlib.client.render.animation.ToolboxModel;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.ToolboxSlot;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxUtil;
import gregtech.common.render.items.ToolboxRenderer;

@SideOnly(Side.CLIENT)
public final class GregTechToolboxAnimation implements HmiItemBindings.TwoHandedModel {

    private final ToolboxMotion motion = new ToolboxMotion();
    private final ItemStack[] contents = new ItemStack[6];
    private EntityPlayer player;
    private int slot = -1, contentsTick = -1;

    private GregTechToolboxAnimation() {}

    public static void register() {
        var adapter = new GregTechToolboxAnimation();
        HmiItemBindings.registerTool(ItemList.ToolBox.getItem(), adapter);
        FMLCommonHandler.instance()
            .bus()
            .register(adapter);
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == ToolboxRenderer.class && matchesVanilla(stack);
    }

    @Override
    public boolean matchesVanilla(ItemStack stack) {
        return stack.getItem() instanceof ItemGTToolbox && (isOpen() || ToolboxUtil.getSelectedToolType(stack)
            .isEmpty()
            && !(stack.hasTagCompound() && stack.getTagCompound()
                .hasKey(ItemGTToolbox.BROKEN_TOOL_ANIMATION_END_KEY)));
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var current = Minecraft.getMinecraft().thePlayer;
        if (current == null || current != player
            || current.inventory.currentItem != slot
            || current.getHeldItem() == null
            || !matchesVanilla(current.getHeldItem())
            || !HmiFirstPerson.INSTANCE.enabled()) {
            motion.reset();
            Arrays.fill(contents, null);
            contentsTick = -1;
            player = current;
            slot = current == null ? -1 : current.inventory.currentItem;
        }
    }

    @Override
    public void renderGripped(ItemStack stack, IItemRenderer renderer, float partial, HmiItemBindings.GripHands hands) {
        var current = Minecraft.getMinecraft().thePlayer;
        if (current != player || current.inventory.currentItem != slot) {
            motion.reset();
            contentsTick = -1;
            player = current;
            slot = current.inventory.currentItem;
        }
        motion.sample((current.ticksExisted + (double) partial) / 20, isOpen());
        if (motion.lid() > 0 && contentsTick != current.ticksExisted) {
            contentsTick = current.ticksExisted;
            Arrays.fill(contents, null);
            var inventory = new ToolboxItemStackHandler(stack);
            int count = 0;
            for (var tool : ToolboxSlot.TOOL_SLOTS) {
                var stored = inventory.getStackInSlot(tool.getSlotID());
                if (stored != null) {
                    contents[count++] = stored;
                    if (count == contents.length) break;
                }
            }
        }
        ToolboxModel.render(motion, contents, hands);
    }

    private static boolean isOpen() {
        // Both are real GT screens. Ordinary inventory, chat and unrelated MUI screens must not open the case.
        return ModularScreen.isActive("gregtech", "Field Engineer's Toolbox")
            || ModularScreen.isActive("gregtech", "ToolboxSelectUI");
    }
}
