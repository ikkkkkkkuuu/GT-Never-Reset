package com.xyp.gtneverreset.client.projector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.event.BlockChangeNotifier;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.packet.GenerateProjectorBatchPacket;
import com.xyp.gtneverreset.common.packet.GenerateProjectorPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.OpenProjectorPacket;
import com.xyp.gtneverreset.common.packet.ProjectorPlanPacket;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorServer;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.config.ProjectorConfig;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

@SideOnly(Side.CLIENT)
public class ProjectorClient {

    public static final String LANG = "gtneverreset.projector.";
    public static final int REACH = ProjectorPlan.REACH, SEGMENT = 64, FLOAT_WARNING = 3, COMPLETE_TICKS = 40,
        HUD_TICKS = 10, BLOCK_PREVIEW_LIMIT = 20_000;
    private static final int CLICK_GAP = 6, MAX_IN_FLIGHT = 16, SPOT_TICKS = 40;
    private static final ArrayDeque<NBTTagCompound> inFlight = new ArrayDeque<>();

    public static int location = Integer.MIN_VALUE;
    public static ProjectorPlan plan;
    public static ProjectorStructure structure;
    static ProjectorStructure projection;
    static ExtendedFacing focusPose;
    static boolean focusInArray;
    static final int[] focusOffset = new int[3];
    private static final ProjectorBatchPreview.Cache arrayCache = new ProjectorBatchPreview.Cache();
    public static ExtendedFacing facing = ExtendedFacing.DEFAULT;
    public static int x, y, z;
    public static boolean visible, placed, followed, unloadedGround, tooHigh, tooLow;
    public static int floating;
    public static int completeTicks;
    public static int[] focus = new int[0];
    public static int focusColor;
    public static final ProjectorCheck CHECK = new ProjectorCheck();
    public static final ProjectorGround GROUND = new ProjectorGround();
    private static NBTTagCompound planTag;
    private static boolean listening;
    private static long lastClick, ticks, spotTick;
    private static int[] spot;
    private static int spotMachine;
    private static int pinnedSlot = Integer.MIN_VALUE, pinnedHotbar;

    ProjectorClient() {}

    public static void registerListener() {
        if (listening) return;
        listening = true;
        BlockChangeNotifier
            .addListener((world, chunk, blockX, blockY, blockZ, oldBlock, newBlock, oldMeta, newMeta) -> {
                if (!world.isRemote || world != Minecraft.getMinecraft().theWorld) return;
                CHECK.blockChanged(world, blockX, blockY, blockZ);
                GROUND.invalidate(blockX, blockZ);
            });
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            reset();
            return;
        }
        ticks++;
        ProjectorStructures.tick();
        ProjectorDeploymentRenderer.tick(mc.theWorld);
        findActive(mc.thePlayer);
        visible = false;
        if (plan == null || !plan.hasMachine()
            || plan.state == ProjectorPlan.State.IDLE
            || plan.isPlaced() && plan.dimension != mc.thePlayer.dimension) {
            projection = null;
            arrayCache.clear();
            CHECK.clear();
            ProjectorRenderer.release();
            completeTicks = 0;
            followed = false;
            if (location == 0 && plan != null && plan.state == ProjectorPlan.State.IDLE && ticks % HUD_TICKS == 0)
                AboveHotbarHUD.renderTextAboveHotbar(
                    // #tr gtneverreset.projector.idle
                    // # Right-click: open the projector
                    // # zh_CN 右键：打开投影仪界面
                    EnumChatFormatting.WHITE + StatCollector.translateToLocal(LANG + "idle"),
                    HUD_TICKS + 5,
                    true,
                    false);
            return;
        }
        ModularScreen screen = ModularScreen.getCurrent();
        ProjectorPlan draft = screen != null && screen.getMainPanel() instanceof ProjectorGui gui
            ? gui.draftFor(location)
            : null;
        ProjectorPlan displayed = draft != null && draft.machine == plan.machine ? draft : plan;
        structure = ProjectorStructures.get(displayed.settings());
        updatePlacement(mc, displayed);
        projection = arrayCache.get(structure, displayed, facing);
        if (projection != null && visible) {
            int[] bounds = projection.worldBounds(facing);
            tooLow = y + bounds[1] < 0;
            tooHigh = y + bounds[4] > 255;
        }
        if (projection != null && projection.size > 0 && placed && visible) {
            CHECK.update(mc.theWorld, projection, facing, x, y, z);
            if (draft == null) updateCompletion();
            else completeTicks = 0;
        } else {
            CHECK.clear();
        }
        if (ticks % HUD_TICKS == 0) showStatus();
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world.isRemote) reset();
    }

    private static void reset() {
        unpinControls();
        location = Integer.MIN_VALUE;
        plan = null;
        planTag = null;
        inFlight.clear();
        spot = null;
        focus = new int[0];
        structure = null;
        projection = null;
        arrayCache.clear();
        visible = false;
        CHECK.clear();
        GROUND.clear();
        ProjectorStructures.clear();
        ProjectorDeploymentRenderer.clear();
        ProjectorRenderer.release();
    }

    private static void findActive(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof MultiblockProjector) {
            use(0, held);
            return;
        }
        IInventory worn = BaublesCompat.getBaubles(player);
        if (worn != null) {
            for (int slot = 0; slot < worn.getSizeInventory(); slot++) {
                ItemStack stack = worn.getStackInSlot(slot);
                if (stack == null || !(stack.getItem() instanceof MultiblockProjector)) continue;
                use(-slot - 1, stack);
                if (plan.state != ProjectorPlan.State.IDLE) return;
            }
        }
        location = Integer.MIN_VALUE;
        plan = null;
        planTag = null;
    }

    private static void use(int slot, ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound() == null ? null
            : stack.getTagCompound()
                .getCompoundTag(ProjectorPlan.TAG);
        if (slot != location || plan == null || tag != planTag) {
            boolean stale = slot == location && plan != null && isStaleEcho(tag);
            planTag = tag;
            if (!stale) plan = ProjectorPlan.read(stack);
        }
        location = slot;
    }

    // The server echoes every edit; an echo of an older edit arriving after newer ones would undo them.
    private static boolean isStaleEcho(NBTTagCompound tag) {
        if (tag == null || inFlight.isEmpty()) return false;
        if (tag.equals(inFlight.peekLast())) {
            inFlight.clear();
            return false;
        }
        while (!inFlight.isEmpty()) {
            if (inFlight.removeFirst()
                .equals(tag)) return true;
        }
        return false;
    }

    public static void send(ProjectorPlan next) {
        send(location, next);
    }

    public static void send(int slot, ProjectorPlan next) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack stack = player == null || slot == Integer.MIN_VALUE ? null
            : ProjectorServer.projectorAt(player, slot);
        if (stack == null) return;
        next.write(stack);
        if (slot == location) {
            plan = next;
            planTag = stack.getTagCompound()
                .getCompoundTag(ProjectorPlan.TAG);
            inFlight.addLast((NBTTagCompound) planTag.copy());
            if (inFlight.size() > MAX_IN_FLIGHT) inFlight.removeFirst();
        }
        NetWorkHandler.sendToServer(new ProjectorPlanPacket(slot, next.writeToNBT()));
    }

    static boolean canPinControls(int slot) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player != null && slot <= 0
            && slot != Integer.MIN_VALUE
            && (slot == 0 || location == Integer.MIN_VALUE || location == slot)
            && ProjectorServer.projectorAt(player, slot) != null;
    }

    static void pinControls(int slot) {
        pinnedSlot = slot;
        pinnedHotbar = Minecraft.getMinecraft().thePlayer.inventory.currentItem;
    }

    static void unpinControls() {
        pinnedSlot = Integer.MIN_VALUE;
    }

    static boolean controlsPinned(int slot) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player != null && pinnedSlot != Integer.MIN_VALUE
            && pinnedSlot == slot
            && (slot != 0 || player.inventory.currentItem == pinnedHotbar);
    }

    static ProjectorPlan pinnedPlan() {
        if (!controlsPinned(location) || plan == null
            || !plan.hasMachine()
            || plan.state == ProjectorPlan.State.IDLE
            || plan.isPlaced() && plan.dimension != Minecraft.getMinecraft().thePlayer.dimension) return null;
        return plan;
    }

    static void editPinned(Consumer<ProjectorPlan> edit) {
        ProjectorPlan current = pinnedPlan();
        if (current == null) return;
        ProjectorPlan next = current.copy();
        edit.accept(next);
        if (next.batch.arrangementKey() == current.batch.arrangementKey()) return;
        next.batchProjection = true;
        send(next);
    }

    static void openPinnedGui() {
        if (pinnedPlan() != null) open();
    }

    static int pinnedCoordinate(ForgeDirection axis) {
        ProjectorPlan current = pinnedPlan();
        boolean locked = current != null && current.isPlaced();
        return switch (axis) {
            case EAST -> locked ? current.x : x;
            case UP -> locked ? current.y : y;
            case SOUTH -> locked ? current.z : z;
            default -> throw new IllegalArgumentException(axis.name());
        };
    }

    static boolean canMovePinned() {
        ProjectorPlan current = pinnedPlan();
        return current != null && current.state == ProjectorPlan.State.LOCKED;
    }

    static void setPinnedCoordinate(ForgeDirection axis, int coordinate) {
        if (!canMovePinned() || pinnedCoordinate(axis) == coordinate) return;
        ProjectorInput.edit(next -> {
            switch (axis) {
                case EAST -> next.x = coordinate;
                case UP -> next.y = Math.max(0, Math.min(255, coordinate));
                case SOUTH -> next.z = coordinate;
                default -> throw new IllegalArgumentException(axis.name());
            }
        });
    }

    static void togglePinnedLock() {
        ProjectorPlan current = pinnedPlan();
        if (current != null && current.state != ProjectorPlan.State.ANCHORED)
            act(Minecraft.getMinecraft().thePlayer, current.state == ProjectorPlan.State.LOCKED);
    }

    static void cyclePinnedBuildStyle() {
        if (pinnedPlan() != null) ProjectorInput.edit(next -> next.buildStyle = nextBuildStyle(next.buildStyle));
    }

    static StructureDeploymentStyle nextBuildStyle(StructureDeploymentStyle current) {
        var styles = StructureDeploymentStyle.values();
        return styles[(current.ordinal() + 1) % styles.length];
    }

    static boolean canBuildPinned() {
        ProjectorPlan current = pinnedPlan();
        if (current == null || !current.batch.withinLimit()
            || !ProjectorConfig.buildEnabled
            || !Minecraft.getMinecraft().thePlayer.capabilities.allowEdit) return false;
        ProjectorStructure template = ProjectorStructures.get(current.batchSettings());
        return template != null && template.failure.isEmpty() && template.limit.isEmpty() && !template.isEmpty();
    }

    static void buildPinned() {
        if (!canBuildPinned()) return;
        ProjectorPlan next = plan.copy();
        next.batchProjection = true;
        if (!locateBuild(next, ProjectorStructures.get(next.batchSettings()), true)) {
            AboveHotbarHUD.renderTextAboveHotbar(
                StatCollector.translateToLocal(ProjectorGui.LANG + "generate_target"),
                40,
                true,
                true);
            return;
        }
        requestBuild(location, next, next.batch.machines() > 1);
        ProjectorPinnedHud.closeEditor();
    }

    public static String frontLabel(ForgeDirection front) {
        String side = front == ForgeDirection.UP ? "up" : front == ForgeDirection.DOWN ? "down" : "horizontal";
        return StatCollector.translateToLocalFormatted(
            // #tr gtneverreset.projector.pose.front
            // # Front: %s
            // # zh_CN 正面：%s
            LANG + "pose.front",
            // #tr gtneverreset.projector.pose.front.up
            // # up
            // # zh_CN 朝上
            // #tr gtneverreset.projector.pose.front.down
            // # down
            // # zh_CN 朝下
            // #tr gtneverreset.projector.pose.front.horizontal
            // # horizontal
            // # zh_CN 水平
            StatCollector.translateToLocal(LANG + "pose.front." + side));
    }

    public static Predicate<ExtendedFacing> validFacings() {
        return ProjectorMachines.validFacings(plan.machine);
    }

    public static int quadrant(EntityPlayer player) {
        return MathHelper.floor_double(player.rotationYaw * 4.0 / 360.0 + 0.5) & 3;
    }

    private static ExtendedFacing rotateWithView(ProjectorPlan plan, int turns) {
        if (plan.batchProjection && plan.batch.machines() > 1) return plan.facing;
        return ProjectorOrientation.rotateY(plan.facing, turns, ProjectorMachines.validFacings(plan.machine));
    }

    public static void reorient(ProjectorPlan next, ExtendedFacing target, ProjectorStructure shape) {
        if (next.state == ProjectorPlan.State.LOCKED && next.centerPivot && shape != null) {
            int[] before = shape.worldBounds(next.facing), after = shape.worldBounds(target);
            next.x += Math.floorDiv(before[0] + before[3] - after[0] - after[3], 2);
            next.y = Math.max(0, Math.min(255, next.y + Math.floorDiv(before[1] + before[4] - after[1] - after[4], 2)));
            next.z += Math.floorDiv(before[2] + before[5] - after[2] - after[5], 2);
        }
        next.facing = target;
    }

    public static ExtendedFacing initialFacing(int machine) {
        Predicate<ExtendedFacing> valid = ProjectorMachines.validFacings(machine);
        return ProjectorOrientation.withFront(ProjectorOrientation.canonical(valid), ForgeDirection.NORTH, valid);
    }

    public static void rightClick(boolean sneaking) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        long now = mc.theWorld.getTotalWorldTime();
        boolean repeated = now - lastClick < CLICK_GAP;
        lastClick = now;
        if (repeated) return;
        findActive(player);
        if (location != 0 || plan == null) return;
        act(player, sneaking);
    }

    public static void lockKey(boolean sneaking) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        findActive(player);
        if (location == Integer.MIN_VALUE || plan == null) return;
        act(player, sneaking);
    }

    private static void act(EntityPlayer player, boolean sneaking) {
        ProjectorPlan next = plan.copy();
        switch (plan.state) {
            case IDLE -> {
                if (!sneaking) open();
            }
            case FOLLOW -> {
                if (sneaking) {
                    next.state = ProjectorPlan.State.IDLE;
                    send(next);
                } else if (visible && structure != null && !tooHigh && !tooLow) {
                    next.facing = facing;
                    next.dimension = player.dimension;
                    next.x = x;
                    next.y = y;
                    next.z = z;
                    next.state = ProjectorPlan.State.LOCKED;
                    send(next);
                }
            }
            case LOCKED -> {
                if (sneaking) {
                    next.facing = rotateWithView(plan, -quadrant(player));
                    next.state = ProjectorPlan.State.FOLLOW;
                    send(next);
                } else open();
            }
            case ANCHORED -> {
                if (sneaking) {
                    next.state = ProjectorPlan.State.IDLE;
                    send(next);
                } else open();
            }
        }
    }

    public static void open() {
        open(location);
    }

    public static void open(int slot) {
        var minecraft = Minecraft.getMinecraft();
        var player = minecraft.thePlayer;
        if (slot == Integer.MIN_VALUE || player == null) return;
        if (ProjectorServer.projectorAt(player, slot) == null) return;
        NetWorkHandler.sendToServer(new OpenProjectorPacket(slot));
    }

    public static void openAt(ProjectorStructure shown, int cell) {
        spot = new int[] { shown.a[cell], shown.b[cell], shown.c[cell] };
        spotMachine = plan.machine;
        spotTick = ticks;
        open();
    }

    // The interface only opens once the server answers, so the chosen spot waits here until then.
    public static int[] takeSpot(int machine) {
        int[] taken = ticks - spotTick <= SPOT_TICKS && machine == spotMachine ? spot : null;
        spot = null;
        return taken;
    }

    private static void updatePlacement(Minecraft mc, ProjectorPlan plan) {
        EntityPlayer player = mc.thePlayer;
        tooHigh = tooLow = unloadedGround = false;
        floating = 0;
        placed = plan.isPlaced();
        if (placed) {
            followed = false;
            facing = plan.facing;
            x = plan.x;
            y = plan.y;
            z = plan.z;
            visible = true;
            return;
        }
        facing = rotateWithView(plan, quadrant(player));
        Target target = targetAtCrosshair(mc, plan, structure, facing);
        if (target == null) {
            visible = followed;
            return;
        }
        x = target.x;
        y = target.y;
        z = target.z;
        tooHigh = target.tooHigh;
        tooLow = target.tooLow;
        unloadedGround = target.unloaded;
        floating = target.floating;
        followed = visible = true;
    }

    static boolean locateBuild(ProjectorPlan draft, ProjectorStructure shape, boolean useAnchor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (useAnchor && draft.isPlaced()) return draft.dimension == mc.thePlayer.dimension;
        ExtendedFacing targetFacing = rotateWithView(draft, quadrant(mc.thePlayer));
        Target target = targetAtCrosshair(mc, draft, shape, targetFacing);
        if (target == null || target.tooHigh || target.tooLow || target.unloaded) return false;
        draft.facing = targetFacing;
        draft.dimension = mc.thePlayer.dimension;
        draft.x = target.x;
        draft.y = target.y;
        draft.z = target.z;
        draft.state = ProjectorPlan.State.LOCKED;
        return true;
    }

    static void requestBuild(int slot, ProjectorPlan target, boolean batch) {
        send(slot, target);
        NetWorkHandler.sendToServer(batch ? new GenerateProjectorBatchPacket(slot) : new GenerateProjectorPacket(slot));
    }

    private static Target targetAtCrosshair(Minecraft mc, ProjectorPlan plan, ProjectorStructure structure,
        ExtendedFacing facing) {
        MovingObjectPosition hit = trace(mc.theWorld, mc.thePlayer);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        int[] bounds = structure == null ? new int[6] : structure.worldBounds(facing);
        int pivotX = plan.centerPivot ? Math.floorDiv(bounds[0] + bounds[3], 2) : 0;
        int pivotZ = plan.centerPivot ? Math.floorDiv(bounds[2] + bounds[5], 2) : 0;
        if (plan.groundSnap)
            return snap(mc.theWorld, hit.blockX - pivotX, hit.blockZ - pivotZ, hit.blockY, bounds, plan.verticalOffset);
        ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit);
        int x = hit.blockX + side.offsetX - pivotX;
        int z = hit.blockZ + side.offsetZ - pivotZ;
        int y = hit.blockY + side.offsetY + plan.verticalOffset;
        return new Target(x, y, z, y + bounds[4] > 255, y + bounds[1] < 0, 0, false);
    }

    private static Target snap(World world, int column, int row, int hitY, int[] bounds, int verticalOffset) {
        boolean unloaded = false;
        GROUND.fit((bounds[3] - bounds[0] + 1) * (bounds[5] - bounds[2] + 1));
        int highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        for (int gx = column + bounds[0]; gx <= column + bounds[3]; gx++) {
            for (int gz = row + bounds[2]; gz <= row + bounds[5]; gz++) {
                int height = GROUND.height(world, gx, gz);
                if (height == ProjectorGround.UNLOADED) {
                    unloaded = true;
                    continue;
                }
                highest = Math.max(highest, height);
                lowest = Math.min(lowest, height);
            }
        }
        if (highest == Integer.MIN_VALUE) highest = lowest = hitY;
        int y = highest + 1 - bounds[1] + Math.max(0, verticalOffset);
        return new Target(
            column,
            y,
            row,
            y + bounds[4] > 255,
            y + bounds[1] < 0,
            y + bounds[1] - (lowest + 1),
            unloaded);
    }

    public static void chunkReceived(int chunkX, int chunkZ) {
        CHECK.chunkReceived(chunkX, chunkZ);
        GROUND.invalidateChunk(chunkX, chunkZ);
    }

    public static Vec3 eye(EntityPlayer player) {
        return Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
    }

    public static MovingObjectPosition trace(World world, EntityPlayer player) {
        Vec3 start = eye(player);
        Vec3 look = player.getLook(1.0f);
        for (int from = 0; from < REACH; from += SEGMENT) {
            Vec3 segmentStart = start.addVector(look.xCoord * from, look.yCoord * from, look.zCoord * from);
            int to = Math.min(REACH, from + SEGMENT);
            Vec3 segmentEnd = start.addVector(look.xCoord * to, look.yCoord * to, look.zCoord * to);
            MovingObjectPosition hit = world.func_147447_a(segmentStart, segmentEnd, false, true, false);
            if (hit != null) return hit;
        }
        return null;
    }

    private static void updateCompletion() {
        if (!CHECK.isComplete()) {
            completeTicks = 0;
            return;
        }
        if (completeTicks >= COMPLETE_TICKS || ++completeTicks < COMPLETE_TICKS) return;
        // Undo and dismantle use the locked projection's site after construction.
        AboveHotbarHUD.renderTextAboveHotbar(
            // #tr gtneverreset.projector.complete
            // # Every block is in place.
            // # zh_CN 方块已全部搭齐。
            EnumChatFormatting.GREEN + StatCollector.translateToLocal(LANG + "complete"),
            60,
            true,
            true);
    }

    private static void showStatus() {
        String text;
        if (structure == null || projection == null) {
            ProjectorSimulation running = ProjectorStructures
                .running(plan.batchProjection ? plan.batchSettings() : plan.settings());
            if (running == null) running = ProjectorStructures.running(plan.settings());
            // #tr gtneverreset.projector.simulating
            // # Working out the structure: round %s
            // # zh_CN 正在推算结构：第 %s 轮
            text = StatCollector.translateToLocalFormatted(LANG + "simulating", running == null ? 0 : running.rounds);
        } else if (!structure.failure.isEmpty()) {
            text = EnumChatFormatting.RED + StatCollector
                // #tr gtneverreset.projector.failure
                // # Simulation failed: %s
                // # zh_CN 模拟失败：%s
                .translateToLocalFormatted(LANG + "failure", StatCollector.translateToLocal(structure.failure));
        } else if (tooHigh || tooLow) {
            // #tr gtneverreset.projector.too_high
            // # Does not fit: the top is above y 255
            // # zh_CN 放不下：顶部超过 y 255
            // #tr gtneverreset.projector.too_low
            // # Does not fit: the bottom is below y 0
            // # zh_CN 放不下：底部低于 y 0
            text = EnumChatFormatting.RED + StatCollector.translateToLocal(LANG + (tooHigh ? "too_high" : "too_low"));
        } else if (!placed) {
            // #tr gtneverreset.projector.follow
            // # Right-click to lock · sneak-right-click to cancel · R rotates · N display
            // # zh_CN 右键锁定 · 潜行右键取消 · R 旋转 · N 切换显示
            text = location == 0 ? StatCollector.translateToLocal(LANG + "follow") : ProjectorInput.wornFollowHint();
            if (floating >= FLOAT_WARNING) text = StatCollector.translateToLocalFormatted(
                // #tr gtneverreset.projector.joined
                // # %s · %s
                // # zh_CN %s · %s
                LANG + "joined",
                text,
                // #tr gtneverreset.projector.floating
                // # floating %s blocks
                // # zh_CN 悬空 %s 格
                StatCollector.translateToLocalFormatted(LANG + "floating", floating));
            if (unloadedGround) text = StatCollector.translateToLocalFormatted(
                LANG + "joined",
                text,
                // #tr gtneverreset.projector.unloaded_ground
                // # some chunks are not loaded, height may be off
                // # zh_CN 有区块未加载，高度可能不准
                StatCollector.translateToLocal(LANG + "unloaded_ground"));
        } else if (projection instanceof ProjectorBatchPreview array && array.outlineOnly) {
            // #tr gtneverreset.projector.batch_outline
            // # Large array: showing its outer bounds.
            // # zh_CN 大型阵列：仅显示外框。
            text = StatCollector.translateToLocal(LANG + "batch_outline");
        } else if (!CHECK.scanned) {
            // #tr gtneverreset.projector.checking
            // # Checking blocks...
            // # zh_CN 正在校对方块……
            text = StatCollector.translateToLocal(LANG + "checking");
        } else if (CHECK.isComplete()) {
            // #tr gtneverreset.projector.all_placed
            // # Every block is in place
            // # zh_CN 方块已全部搭齐
            text = EnumChatFormatting.GREEN + StatCollector.translateToLocal(LANG + "all_placed");
        } else {
            // #tr gtneverreset.projector.locked
            // # Missing %s · wrong %s · not loaded %s
            // # zh_CN 缺 %s · 错 %s · 未加载 %s
            text = StatCollector.translateToLocalFormatted(LANG + "locked", CHECK.missing, CHECK.wrong, CHECK.unloaded);
        }
        AboveHotbarHUD.renderTextAboveHotbar(EnumChatFormatting.WHITE + text, HUD_TICKS + 5, true, false);
    }

    public static List<ItemStack> carried(EntityPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        if (player == null) return stacks;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() != null) stacks.add(stack);
        }
        IInventory worn = BaublesCompat.getBaubles(player);
        if (worn != null) {
            for (int slot = 0; slot < worn.getSizeInventory(); slot++) {
                ItemStack stack = worn.getStackInSlot(slot);
                if (stack != null && stack.getItem() != null) stacks.add(stack);
            }
        }
        return stacks;
    }

    public static Int2IntOpenHashMap inventory(EntityPlayer player) {
        Int2IntOpenHashMap have = new Int2IntOpenHashMap();
        for (ItemStack stack : carried(player))
            have.addTo(Choice.idOf(stack.getItem(), stack.getItemDamage()), stack.stackSize);
        return have;
    }

    @Desugar
    private record Target(int x, int y, int z, boolean tooHigh, boolean tooLow, int floating, boolean unloaded) {}
}
