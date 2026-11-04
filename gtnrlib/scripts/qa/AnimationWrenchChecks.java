package com.xyp.gtnrlib.qa;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3f;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.compat.gregtech.GregTechWrenchAnimation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;

/** Host-only integrated-server checks. The normal GT interaction manager delivers the acknowledgement. */
public final class AnimationWrenchChecks implements Runnable {

    private final AnimationClientChecks qa;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final UUID actor = mc.thePlayer.getUniqueID();
    private final AtomicReference<Runnable> serverTask = new AtomicReference<>();
    private final int slot = mc.thePlayer.inventory.currentItem, dimension = mc.thePlayer.dimension;
    private final int x = (int) mc.thePlayer.posX - 2, y = (int) mc.thePlayer.posY + 1, z = (int) mc.thePlayer.posZ + 2;
    private volatile boolean serverDone;
    private volatile Throwable error;
    private int step, waitTick, startTick, firstTurnTick;

    public AnimationWrenchChecks(AnimationClientChecks qa) {
        this.qa = qa;
        startTick = mc.thePlayer.ticksExisted;
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    private static ItemStack wrench(Materials material) {
        return MetaGeneratedTool01.INSTANCE.getToolWithStats(IDMetaTool01.WRENCH.ID, 1, material, Materials.Wood, null);
    }

    @Override
    public void run() {
        try {
            if (error != null) throw new AssertionError("server operation failed", error);
            int tick = mc.thePlayer.ticksExisted;
            if (tick - startTick > 180) throw new AssertionError("wrench fixture timed out at " + step);
            var hmi = HmiFirstPerson.INSTANCE;
            switch (step) {
                case 0 -> {
                    qa.equip(mc, wrench(Materials.Steel));
                    waitTick = tick + 25;
                    step++;
                }
                case 1 -> {
                    if (tick < waitTick) return;
                    qa.equip(mc, wrench(Materials.Steel));
                    var stack = mc.thePlayer.getHeldItem();
                    var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
                    qa.require(
                        renderer != null && GregTechWrenchAnimation.INSTANCE.matches(stack, renderer),
                        "GT manual wrench renderer: " + renderer + ", stack " + stack);
                    qa.require(
                        hmi.sample(stack, .5f, 0) && hmi.handheldTool() != null,
                        "HMI accepts GT's own manual wrench renderer");
                    int[] steel = qa.draw(mc, "wrench-steel");
                    qa.equip(mc, wrench(Materials.Gold));
                    int[] gold = qa.draw(mc, "wrench-gold");
                    int changed = 0, gripContact = 0;
                    for (int i = 0; i < gold.length - mc.displayWidth; i++) {
                        if (gold[i] == steel[i]) continue;
                        changed++;
                        int below = i + mc.displayWidth;
                        if (gold[below] == steel[below] && gold[below] != gold[0]) gripContact++;
                    }
                    qa.require(changed > 40, "GT material changes visible wrench pixels: " + changed);
                    qa.require(gripContact > 8, "wrench handle meets the fist along " + gripContact + " pixels");
                    checkLooseningStroke();
                    var electric = new ItemStack(MetaGeneratedTool01.INSTANCE, 1, IDMetaTool01.WRENCH_LV.ID);
                    qa.require(
                        hmi.sample(electric, .5f, 0) && hmi.handheldTool() == null,
                        "electric wrench uses a generic grip without the manual turn");
                    request(0);
                    waitTick = tick + 5;
                    step++;
                }
                case 2 -> {
                    if (turning()) throw new AssertionError("rejected block use started wrist motion");
                    if (!serverDone || tick < waitTick) return;
                    qa.require(true, "rejected block use has no wrist turn");
                    request(1);
                    step++;
                }
                case 3 -> {
                    if (!serverDone || !turning()) return;
                    qa.require(true, "real GT machine rotation reached the local wrist");
                    firstTurnTick = tick;
                    mc.thePlayer.jump();
                    step++;
                }
                case 4 -> {
                    if (tick < firstTurnTick + 3) return;
                    qa.require(
                        !mc.thePlayer.onGround && mc.thePlayer.motionY > 0 && turning(),
                        "wrench turn during jump ascent");
                    qa.draw(mc, "wrench-machine-turn");
                    request(2);
                    step++;
                }
                case 5 -> {
                    // The original nine-tick stroke must have finished by this point.
                    if (!serverDone || tick < firstTurnTick + 10) return;
                    qa.require(
                        turning() && mc.thePlayer.motionY < 0,
                        "second acknowledgement extends the turn into descent");
                    qa.draw(mc, "wrench-repeat");
                    waitTick = tick + 12;
                    step++;
                }
                case 6 -> {
                    if (tick < waitTick) return;
                    qa.require(!turning(), "wrist recovers to upstream idle");
                    request(3);
                    step++;
                }
                case 7 -> {
                    if (!serverDone || !turning()) return;
                    qa.draw(mc, "wrench-block-turn");
                    qa.require(true, "successful GT tool behaviour delivered the turn");
                    mc.thePlayer.inventory.currentItem = (slot + 1) % 9;
                    waitTick = tick + 2;
                    step++;
                }
                case 8 -> {
                    if (tick < waitTick) return;
                    mc.thePlayer.inventory.currentItem = slot;
                    qa.equip(mc, wrench(Materials.Steel));
                    qa.require(!turning(), "switching away cancels the old wrist motion");
                    qa.equip(mc, AnimationToolChecks.cutter());
                    hmi.sample(mc.thePlayer.getHeldItem(), .5f, 0);
                    request(4);
                    waitTick = tick + 6;
                    step++;
                }
                case 9 -> {
                    if (!serverDone || tick < waitTick) return;
                    request(5);
                    step++;
                }
                case 10 -> {
                    hmi.sample(mc.thePlayer.getHeldItem(), .5f, 0);
                    if (!serverDone || hmi.strokeSeconds() == 0) return;
                    qa.draw(mc, "cutter-cable-use");
                    qa.require(true, "server cut an actual copper cable connection and acknowledged the local snip");
                    FMLCommonHandler.instance()
                        .bus()
                        .unregister(this);
                    qa.finish(mc, null);
                }
                default -> throw new AssertionError(step);
            }
        } catch (Throwable e) {
            qa.finish(mc, e);
        }
    }

    private void checkLooseningStroke() throws Exception {
        var player = mc.thePlayer;
        int tick = player.ticksExisted;
        var hmi = HmiFirstPerson.INSTANCE;
        // Centers of the jaw opening and lower handle on the mounted 16px GT wrench, in HMI item space.
        var jaw = new Vector3f(0, .419f, -.114f);
        var grip = new Vector3f(0, -.046f, .083f);
        var rest = hmi.item.transformPosition(jaw, new Vector3f());
        var contact = new Vector3f();
        var startGrip = new Vector3f();
        try {
            qa.require(FirstPersonItemActions.INSTANCE.trigger("gtneverreset:wrench_turn"), "manual wrench clip bound");
            for (int frame = 0; frame <= 20; frame++) {
                player.ticksExisted = tick + frame / 2;
                int[] pixels = qa.draw(mc, "wrench-stroke-" + frame, (frame % 2) * .5f);
                if (Arrays.stream(pixels, pixels.length - mc.displayWidth, pixels.length)
                    .allMatch(pixel -> pixel == pixels[0]))
                    throw new AssertionError("floating forearm at frame " + frame);
                var tip = hmi.item.transformPosition(jaw, new Vector3f());
                if (frame == 4) {
                    contact.set(tip);
                    hmi.item.transformPosition(grip, startGrip);
                    qa.require(tip.x < rest.x - .3f && tip.z < rest.z - .12f, "jaw reaches inward and forward");
                }
                if (frame > 4 && frame <= 10 && tip.distance(contact) > .055f) throw new AssertionError(
                    "jaw slipped during torque at frame " + frame + ": " + tip.distance(contact));
                if (frame == 10) qa.require(
                    hmi.item.transformPosition(grip, new Vector3f())
                        .distance(startGrip) > .26f,
                    "handle turns around the planted jaws");
                if (frame == 13) qa.require(tip.z > contact.z + .08f, "open end clears the fastener on the return");
                if (frame == 18) qa.require(tip.distance(rest) < .06f, "wrench settles back into its carrying pose");
            }
            qa.require(!turning(), "single wrench stroke has ended");
        } finally {
            player.ticksExisted = tick;
            hmi.detach();
        }
    }

    private boolean turning() {
        return FirstPersonItemActions.INSTANCE.playing("gtneverreset:wrench_turn");
    }

    private void request(int mode) {
        serverDone = false;
        serverTask.set(() -> {
            var server = MinecraftServer.getServer();
            EntityPlayerMP p = (EntityPlayerMP) server.worldServerForDimension(dimension)
                .func_152378_a(actor);
            var world = p.worldObj;
            p.inventory.currentItem = slot;
            if (mode == 4) {
                var cutter = AnimationToolChecks.cutter();
                p.inventory.mainInventory[slot] = cutter;
                var cable = GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 1);
                for (int dx = 0; dx < 2; dx++) {
                    world.setBlockToAir(x + dx, y, z);
                    if (!((ItemBlock) cable.getItem()).placeBlockAt(cable, p, world, x + dx, y, z, 1, .5f, .5f, .5f, 0))
                        throw new AssertionError("could not place copper cable");
                }
                var pipe = (MTECable) ((IGregTechTileEntity) world.getTileEntity(x, y, z)).getMetaTileEntity();
                pipe.connect(ForgeDirection.EAST);
                serverDone = true;
                return;
            }
            if (mode == 5) {
                var cutter = p.getHeldItem();
                var pipe = (MTECable) ((IGregTechTileEntity) world.getTileEntity(x, y, z)).getMetaTileEntity();
                if (!pipe.isConnectedAtSide(ForgeDirection.EAST))
                    throw new AssertionError("cable setup is disconnected");
                p.theItemInWorldManager.activateBlockOrUseItem(p, world, cutter, x, y, z, 5, 1f, .5f, .5f);
                if (pipe.isConnectedAtSide(ForgeDirection.EAST))
                    throw new AssertionError("wire cutter did not disconnect copper cable");
                serverDone = true;
                return;
            }
            var tool = wrench(Materials.Gold);
            p.inventory.mainInventory[slot] = tool;
            // All three operations target the same fixed position, including while the player is airborne.
            if (mode == 0) world.setBlock(x, y, z, Blocks.stone);
            if (mode == 0) {
                p.theItemInWorldManager.activateBlockOrUseItem(p, world, tool, x, y, z, 5, .5f, .5f, .5f);
                var machine = ItemList.Machine_LV_Macerator.get(1);
                if (!((ItemBlock) machine.getItem()).placeBlockAt(machine, p, world, x, y, z, 1, .5f, .5f, .5f, 0))
                    throw new AssertionError("could not place GT machine");
            }
            if (mode == 3) world.setBlock(x, y, z, Blocks.piston, 2, 3);
            if (mode == 0) {
                serverDone = true;
                return;
            }
            var machine = world.getTileEntity(x, y, z) instanceof IGregTechTileEntity gt ? gt : null;
            if (mode == 1 && machine != null && machine.getTimer() < 2) {
                // GT consumes clicks before the newly placed machine is usable.
                request(mode);
                return;
            }
            ForgeDirection before = machine == null ? null : machine.getFrontFacing();
            int side = machine == null ? ForgeDirection.EAST.ordinal()
                : Arrays.stream(ForgeDirection.VALID_DIRECTIONS)
                    .filter(facing -> facing.offsetY == 0 && facing != before && machine.isValidFacing(facing))
                    .findFirst()
                    .orElseThrow()
                    .ordinal();
            boolean handled = p.theItemInWorldManager
                .activateBlockOrUseItem(p, world, tool, x, y, z, side, .5f, .5f, .5f);
            if (mode != 0 && !handled) throw new AssertionError("GT rejected use " + mode);
            if ((mode == 1 || mode == 2) && before == machine.getFrontFacing())
                throw new AssertionError("GT machine facing did not change: " + before);
            if (mode == 3 && world.getBlockMetadata(x, y, z) != side) throw new AssertionError("piston did not rotate");
            serverDone = true;
        });
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var task = serverTask.getAndSet(null);
        if (task != null) try {
            task.run();
        } catch (Throwable t) {
            error = t;
        }
    }
}
