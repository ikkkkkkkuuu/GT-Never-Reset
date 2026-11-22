package com.xyp.gtneverreset.client.projector;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.OpenProjectorPacket;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorInput {

    private static final String CATEGORY = "key.categories.gtneverreset";
    // #tr key.gtneverreset.projector_rotate
    // # Projector: rotate (Shift mirror, Alt pose panel)
    // # zh_CN 投影仪：旋转（Shift 镜像，Alt 姿态面板）
    private static final KeyBinding ROTATE = new KeyBinding(
        "key.gtneverreset.projector_rotate",
        Keyboard.KEY_R,
        CATEGORY);
    // #tr key.gtneverreset.projector_display
    // # Projector: switch display
    // # zh_CN 投影仪：切换显示
    private static final KeyBinding DISPLAY = new KeyBinding(
        "key.gtneverreset.projector_display",
        Keyboard.KEY_N,
        CATEGORY);
    // #tr key.gtneverreset.projector_forward
    // # Projector: move forward
    // # zh_CN 投影仪：向前移一格
    private static final KeyBinding FORWARD = new KeyBinding(
        "key.gtneverreset.projector_forward",
        Keyboard.KEY_UP,
        CATEGORY);
    // #tr key.gtneverreset.projector_back
    // # Projector: move back
    // # zh_CN 投影仪：向后移一格
    private static final KeyBinding BACK = new KeyBinding(
        "key.gtneverreset.projector_back",
        Keyboard.KEY_DOWN,
        CATEGORY);
    // #tr key.gtneverreset.projector_left
    // # Projector: move left
    // # zh_CN 投影仪：向左移一格
    private static final KeyBinding LEFT = new KeyBinding(
        "key.gtneverreset.projector_left",
        Keyboard.KEY_LEFT,
        CATEGORY);
    // #tr key.gtneverreset.projector_right
    // # Projector: move right
    // # zh_CN 投影仪：向右移一格
    private static final KeyBinding RIGHT = new KeyBinding(
        "key.gtneverreset.projector_right",
        Keyboard.KEY_RIGHT,
        CATEGORY);
    // #tr key.gtneverreset.projector_open
    // # Open projector
    // # zh_CN 打开投影仪
    private static final KeyBinding OPEN = new KeyBinding(
        "key.gtneverreset.projector_open",
        Keyboard.KEY_NONE,
        CATEGORY);
    // #tr key.gtneverreset.projector_lock
    // # Projector: lock / follow (for a worn projector)
    // # zh_CN 投影仪：锁定 / 跟随（饰品栏里的投影仪用）
    private static final KeyBinding LOCK = new KeyBinding(
        "key.gtneverreset.projector_lock",
        Keyboard.KEY_NONE,
        CATEGORY);
    private static final int[][] HORIZONTAL = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 } };

    ProjectorInput() {}

    public static void register() {
        var input = new ProjectorInput();
        var client = new ProjectorClient();
        var forge = MinecraftForge.EVENT_BUS;
        var fml = FMLCommonHandler.instance()
            .bus();
        forge.register(input);
        fml.register(input);
        forge.register(client);
        fml.register(client);
        forge.register(new ProjectorRenderer());
        ClientRegistry.registerKeyBinding(ROTATE);
        ClientRegistry.registerKeyBinding(DISPLAY);
        ClientRegistry.registerKeyBinding(FORWARD);
        ClientRegistry.registerKeyBinding(BACK);
        ClientRegistry.registerKeyBinding(LEFT);
        ClientRegistry.registerKeyBinding(RIGHT);
        ClientRegistry.registerKeyBinding(OPEN);
        ClientRegistry.registerKeyBinding(LOCK);
        ProjectorClient.registerListener();
    }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean rotate = ROTATE.isPressed(), display = DISPLAY.isPressed(), open = OPEN.isPressed(),
            lock = LOCK.isPressed();
        int forward = presses(FORWARD) - presses(BACK), right = presses(RIGHT) - presses(LEFT);
        if (mc.currentScreen != null || mc.thePlayer == null) return;
        KeyBinding pickBlock = mc.gameSettings.keyBindPickBlock;
        if (pickBlock.getKeyCode() > 0 && Keyboard.getEventKeyState()
            && Keyboard.getEventKey() == pickBlock.getKeyCode()
            && pick(mc)) presses(pickBlock);
        if (open) openAnywhere(mc.thePlayer);
        if (lock) ProjectorClient.lockKey(mc.thePlayer.isSneaking());
        ProjectorPlan plan = ProjectorClient.plan;
        if (plan == null || !plan.hasMachine() || plan.state == ProjectorPlan.State.IDLE) return;
        if (rotate) {
            if (isAltDown()) ProjectorPoseScreen.open();
            else if (isShiftDown()) turn(facing -> ProjectorOrientation.mirror(facing, ProjectorClient.validFacings()));
            else turn(facing -> ProjectorOrientation.rotateY(facing, 1, ProjectorClient.validFacings()));
        }
        if (display) cycleDisplay(plan);
        if (forward != 0 || right != 0) nudge(mc.thePlayer, plan, forward, right);
    }

    @SubscribeEvent
    public void wheel(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        ProjectorPlan plan = ProjectorClient.plan;
        if (event.dwheel == 0 || mc.currentScreen != null || !isAltDown() || plan == null || !plan.hasMachine()) return;
        if (plan.state != ProjectorPlan.State.FOLLOW && plan.state != ProjectorPlan.State.LOCKED) return;
        event.setCanceled(true);
        raise(plan, event.dwheel > 0 ? 1 : -1);
    }

    @SubscribeEvent
    public void pickBlock(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!event.buttonstate || event.button < 0
            || event.button - 100 != mc.gameSettings.keyBindPickBlock.getKeyCode()) return;
        if (mc.currentScreen == null && mc.thePlayer != null && pick(mc)) event.setCanceled(true);
    }

    private static boolean pick(Minecraft mc) {
        ProjectorPlan plan = ProjectorClient.plan;
        if (plan == null || !plan.hasMachine() || plan.state == ProjectorPlan.State.IDLE || !ProjectorClient.visible)
            return false;
        EntityPlayer player = mc.thePlayer;
        MovingObjectPosition hit = ProjectorClient.trace(mc.theWorld, player);
        boolean onBlock = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        ProjectorCheck check = ProjectorClient.CHECK;
        int hitCell = onBlock ? check.cellAt(hit.blockX, hit.blockY, hit.blockZ) : -1;
        Vec3 eye = ProjectorClient.eye(player);
        int ghost = check
            .firstCellAlong(eye, player.getLook(1.0f), onBlock ? eye.distanceTo(hit.hitVec) : ProjectorClient.REACH);
        if (ghost >= 0 && ghost != hitCell) return openSpot(check.structure, ghost, null, true);
        // A worn projector leaves real blocks to vanilla pick block, which builders use to take blocks.
        if (!onBlock || ProjectorClient.location != 0 && !player.isSneaking()) return false;
        ItemStack clicked = pickedStack(mc.theWorld, player, hit);
        if (hitCell >= 0) return openSpot(check.structure, hitCell, clicked, false);
        ProjectorStructure structure = ProjectorClient.structure;
        String channel = structure == null ? null : ProjectorChannels.channelOf(clicked, structure.channels);
        if (channel == null) return false;
        ProjectorSpotScreen.openChannel(structure, -1, channel, clicked);
        return true;
    }

    private static boolean openSpot(ProjectorStructure shown, int cell, ItemStack clicked, boolean ghost) {
        ProjectorClient.focusInArray = shown instanceof ProjectorBatchPreview && shown.slotAt(cell) != null;
        if (shown instanceof ProjectorBatchPreview array && !ProjectorClient.focusInArray) {
            int original = array.originals[cell];
            array.copyOrigin(cell, ProjectorClient.focusOffset);
            ProjectorClient.focusPose = array.copyPoses[cell];
            shown = array.template;
            cell = original;
        } else ProjectorClient.focusPose = null;
        Choice choice = shown.choiceAt(cell);
        if (shown.slotAt(cell) != null) ProjectorSpotScreen.openHatch(shown, cell);
        else if (choice != null && choice.channel() != null)
            ProjectorSpotScreen.openChannel(shown, shown.choiceOf[cell], choice.channel(), clicked);
        else if (choice != null) ProjectorSpotScreen.openChoice(shown, cell);
        else if (!ghost) return false;
        else say(
            StatCollector.translateToLocalFormatted(
                ProjectorSpotScreen.LANG
                    // #tr gtneverreset.projector.spot.any_variant
                    // # Any variant works here: %s
                    // # zh_CN 这一格任意变种都可以：%s
                    // #tr gtneverreset.projector.spot.only_block
                    // # Only this fits here: %s
                    // # zh_CN 这一格只能放：%s
                    + (shown.metas[cell] == ProjectorStructure.ANY_META ? "any_variant" : "only_block"),
                nameOf(shown, cell)));
        return true;
    }

    private static String nameOf(ProjectorStructure shown, int cell) {
        ItemStack item = shown.items[cell];
        return item != null ? ProjectorChannels.name(item) : shown.blocks[cell].getLocalizedName();
    }

    private static ItemStack pickedStack(World world, EntityPlayer player, MovingObjectPosition hit) {
        Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        try {
            return block.getPickBlock(hit, world, hit.blockX, hit.blockY, hit.blockZ, player);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String wornFollowHint() {
        int code = LOCK.getKeyCode();
        if (code == Keyboard.KEY_NONE)
            // #tr gtneverreset.projector.follow_worn_unbound
            // # Worn projector: bind "Projector: lock / follow" in Controls to lock it
            // # zh_CN 饰品栏里的投影仪：在按键设置里绑定“投影仪：锁定 / 跟随”来锁定
            return StatCollector.translateToLocal(ProjectorClient.LANG + "follow_worn_unbound");
        String key = GameSettings.getKeyDisplayString(code);
        // #tr gtneverreset.projector.follow_worn
        // # Press %s to lock · sneak+%s to cancel · R rotates · N display
        // # zh_CN 按 %s 锁定 · 潜行+%s 取消 · R 旋转 · N 切换显示
        return StatCollector.translateToLocalFormatted(ProjectorClient.LANG + "follow_worn", key, key);
    }

    private static int presses(KeyBinding key) {
        int count = 0;
        while (key.isPressed()) count++;
        return count;
    }

    public static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private static boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private static void openAnywhere(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof MultiblockProjector) {
            NetWorkHandler.sendToServer(new OpenProjectorPacket(0));
            return;
        }
        IInventory worn = BaublesCompat.getBaubles(player);
        if (worn == null) return;
        for (int slot = 0; slot < worn.getSizeInventory(); slot++) {
            ItemStack stack = worn.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof MultiblockProjector) {
                NetWorkHandler.sendToServer(new OpenProjectorPacket(-slot - 1));
                return;
            }
        }
    }

    public static void turn(UnaryOperator<ExtendedFacing> change) {
        ProjectorPlan plan = ProjectorClient.plan;
        if (plan.state == ProjectorPlan.State.ANCHORED) {
            hint(ProjectorClient.LANG + "anchored");
            return;
        }
        ExtendedFacing next = change.apply(plan.facing);
        if (next == plan.facing) {
            hint(ProjectorClient.LANG + "invalid_facing");
            return;
        }
        edit(copy -> ProjectorClient.reorient(copy, next, ProjectorClient.structure));
    }

    public static void edit(Consumer<ProjectorPlan> change) {
        ProjectorPlan next = ProjectorClient.plan.copy();
        change.accept(next);
        ProjectorClient.send(next);
    }

    private static void cycleDisplay(ProjectorPlan plan) {
        ProjectorPlan.Display[] values = ProjectorPlan.Display.values();
        ProjectorPlan.Display next = values[(plan.display.ordinal() + 1) % values.length];
        edit(copy -> copy.display = next);
        hint(ProjectorClient.LANG + "display." + next.name());
    }

    private static void nudge(EntityPlayer player, ProjectorPlan plan, int forward, int right) {
        if (plan.state != ProjectorPlan.State.LOCKED) return;
        if (isAltDown()) {
            raise(plan, forward);
            return;
        }
        int quadrant = ProjectorClient.quadrant(player);
        int[] ahead = HORIZONTAL[quadrant], side = HORIZONTAL[(quadrant + 1) % 4];
        int dx = ahead[0] * forward + side[0] * right, dz = ahead[1] * forward + side[1] * right;
        edit(copy -> {
            copy.x += dx;
            copy.z += dz;
        });
    }

    public static void raise(ProjectorPlan plan, int step) {
        if (step == 0) return;
        if (plan.state == ProjectorPlan.State.LOCKED) {
            edit(copy -> copy.y = Math.max(0, Math.min(255, copy.y + step)));
        } else {
            int min = plan.groundSnap ? 0 : -ProjectorPlan.MAX_OFFSET;
            edit(
                copy -> copy.verticalOffset = Math
                    .max(min, Math.min(ProjectorPlan.MAX_OFFSET, copy.verticalOffset + step)));
        }
    }

    private static void hint(String key) {
        say(StatCollector.translateToLocal(key));
    }

    private static void say(String text) {
        AboveHotbarHUD.renderTextAboveHotbar(EnumChatFormatting.WHITE + text, 40, true, true);
    }
}
