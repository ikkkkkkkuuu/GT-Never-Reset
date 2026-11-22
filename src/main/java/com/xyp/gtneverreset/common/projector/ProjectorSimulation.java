package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;
import com.xyp.gtneverreset.common.wireless.WirelessControllerContext;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.threads.RunnableMachineUpdate;
import gregtech.common.misc.GTStructureChannels;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class ProjectorSimulation {

    public static final int CONTROLLER_X = 0, CONTROLLER_Y = 128, CONTROLLER_Z = 0, MAX_ROUNDS = 2000,
        STABLE_ROUNDS = 3, MAX_BLOCKS = 250_000, CLIENT_BUDGET = 65_536;
    public static final String CHANNEL_TAG = "channels";
    private static final GameProfile PROFILE = new GameProfile(
        UUID.nameUUIDFromBytes("gtneverreset_projector_simulation".getBytes()),
        "[GTNR Projector]");

    public enum Stage {
        SETUP,
        BUILD,
        FINISH,
        DONE
    }

    public final ProjectorPlan.Settings settings;
    public ExtendedFacing canonical = ExtendedFacing.DEFAULT;
    public ProjectorSimulationWorld world;
    public final List<HatchSlot> hatchSlots = new ArrayList<>();
    public final List<Choice> choices = new ArrayList<>();
    public final LongArrayList emptyCells = new LongArrayList();
    public final List<String> errors = new ArrayList<>();
    public boolean formed, checked;
    public List<Material> materials = new ArrayList<>();
    public final List<String> channels = new ArrayList<>();
    public String limit = "";
    public String failure = "";
    public int rounds;
    public long nanos;
    public final int[] bounds = new int[6];
    public Actor actor;
    public IMetaTileEntity machine;
    public ItemStack trigger;
    public ProjectorItemSource source;
    public final Long2ObjectOpenHashMap<IStructureElement<Object>> elements = new Long2ObjectOpenHashMap<>();
    public final Map<IStructureElement<Object>, Integer> slotOfElement = new IdentityHashMap<>();
    public final Map<IStructureElement<Object>, Integer> choiceOfElement = new IdentityHashMap<>();
    private final int elementBudget;
    private final int[] offset = new int[3];
    private final Set<String> queried = new LinkedHashSet<>();
    private int stableRounds;
    private Stage stage = Stage.SETUP;
    private IGregTechTileEntity base;
    private ISurvivalBuildEnvironment environment;
    private ProjectorStructure result;

    public ProjectorSimulation(ProjectorPlan.Settings settings, int elementBudget) {
        this.settings = settings;
        this.elementBudget = elementBudget;
    }

    public static ProjectorStructure run(ProjectorPlan.Settings settings) {
        ProjectorSimulation simulation = new ProjectorSimulation(settings, Integer.MAX_VALUE);
        while (!simulation.step(Long.MAX_VALUE / 4)) {}
        return simulation.result();
    }

    public ProjectorStructure result() {
        return result;
    }

    public boolean step(long budgetNanos) {
        long start = System.nanoTime();
        long deadline = start + budgetNanos;
        boolean updates = RunnableMachineUpdate.isCurrentThreadEnabled();
        if (updates) RunnableMachineUpdate.setCurrentThreadEnabled(false);
        try {
            do {
                switch (stage) {
                    case SETUP -> setup();
                    case BUILD -> buildRound();
                    case FINISH -> finish();
                    default -> {}
                }
            } while (stage != Stage.DONE && System.nanoTime() < deadline);
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        } finally {
            if (updates) RunnableMachineUpdate.setCurrentThreadEnabled(true);
            nanos += System.nanoTime() - start;
            if (result != null) result.nanos = nanos;
        }
        return stage == Stage.DONE;
    }

    private void fail(Throwable error) {
        failure = error.getClass()
            .getSimpleName() + ": "
            + error.getMessage();
        GTNeverReset.LOG.warn("Projector simulation of machine {} failed", settings.machine(), error);
        try {
            result = new ProjectorPlanApplier(this).collect();
        } catch (RuntimeException | LinkageError again) {
            result = new ProjectorStructure(this, 0);
        }
        stage = Stage.DONE;
    }

    private void setup() {
        world = new ProjectorSimulationWorld();
        actor = new Actor(world);
        canonical = ProjectorOrientation.canonical(ProjectorMachines.validFacings(settings.machine()));
        ItemStack controller = ProjectorMachines.stackOf(settings.machine());
        if (controller != null) place(controller, CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        TileEntity tile = world.getTileEntity(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        if (!(tile instanceof IGregTechTileEntity controllerTile)
            || !(controllerTile.getMetaTileEntity() instanceof ISurvivalConstructable)) {
            // #tr gtneverreset.projector.failure.no_controller
            // # the controller could not be placed
            // # zh_CN 控制器放不下去
            failure = "gtneverreset.projector.failure.no_controller";
            stage = Stage.FINISH;
            return;
        }
        base = controllerTile;
        machine = controllerTile.getMetaTileEntity();
        base.setFrontFacing(canonical.getDirection());
        if (machine instanceof IAlignment alignment) alignment.setExtendedFacing(canonical);
        trigger = new ItemStack(StructureLibAPI.getDefaultHologramItem(), settings.master());
        trigger.setTagCompound(new NBTTagCompound());
        trigger.getTagCompound()
            .setTag(CHANNEL_TAG, new ProjectorChannelTrace(queried));
        for (Map.Entry<String, Integer> channel : settings.channels()
            .entrySet()) {
            if (channel.getValue() >= 1)
                ChannelDataAccessor.setChannelData(trigger, channel.getKey(), channel.getValue());
        }
        ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.HATCH.get(), 1);
        queried.clear();
        source = new ProjectorItemSource();
        environment = ISurvivalBuildEnvironment.create(source, actor);
        world.consumeChanged();
        stage = Stage.BUILD;
    }

    private void buildRound() {
        int placedBefore = world.placed;
        boolean instrument = !StructureLibAPI.isInstrumentEnabled();
        if (instrument) StructureLibAPI.enableInstrument(this);
        try {
            ((ISurvivalConstructable) machine).survivalConstruct(trigger, elementBudget, environment);
        } finally {
            if (instrument) StructureLibAPI.disableInstrument();
        }
        rounds++;
        // Blocks that break themselves after placement (portals without a frame) would otherwise loop forever.
        stableRounds = world.placed == placedBefore ? stableRounds + 1 : 0;
        boolean building = world.consumeChanged() && stableRounds < STABLE_ROUNDS;
        if (world.outOfHeight) limit = "height";
        else if (world.placed >= MAX_BLOCKS) limit = "size";
        else if (building && rounds >= MAX_ROUNDS) limit = "rounds";
        if (!building || !limit.isEmpty()) stage = Stage.FINISH;
    }

    private void finish() {
        ProjectorPlanApplier applier = new ProjectorPlanApplier(this);
        if (machine != null) {
            for (String channel : queried) {
                if (!channel.equals(GTStructureChannels.HATCH.get())
                    && !channel.equals(StructureLibAPI.CHANNEL_SHOW_ERROR)) channels.add(channel);
            }
            ProjectorHatchProbe hatches = new ProjectorHatchProbe(this);
            hatches.findSlots();
            hatches.fillHatchOnlySpots();
            hatches.replaceHatchesWithCasings();
            new ProjectorChoiceProbe(this).findChoices();
            applier.applyChoices();
            applier.applyOverrides();
            applier.applyCellChoices();
            checkStructure();
        }
        result = applier.collect();
        stage = Stage.DONE;
    }

    private void checkStructure() {
        if (!(machine instanceof MTEMultiBlockBase multiblock)) return;
        List<StructureError> found = new ArrayList<>();
        multiblock.clearHatches();
        multiblock.checkMachine(base, multiblock.mInventory[1], found);
        checked = true;
        formed = found.isEmpty();
        for (StructureError error : found) {
            String line;
            try {
                line = error.getDisplayString();
            } catch (RuntimeException | LinkageError e) {
                line = error.getId()
                    .name();
            }
            if (!errors.contains(line)) errors.add(line);
        }
        if (settings.overrides()
            .isEmpty()) return;
        // Tiered machines give their hatches the casing texture of the chosen tier only in their full check.
        try {
            multiblock.checkStructure(true, base);
        } catch (RuntimeException e) {
            GTNeverReset.LOG.debug("Projector structure check of {} failed to finish", multiblock, e);
        }
    }

    public Choice choiceFor(IStructureElement<Object> element) {
        Integer index = choiceOfElement.get(element);
        Choice choice = index == null ? null : choices.get(index);
        return choice == null || choice.channel() != null ? null : choice;
    }

    public void relative(int x, int y, int z, int[] out) {
        offset[0] = x - CONTROLLER_X;
        offset[1] = y - CONTROLLER_Y;
        offset[2] = z - CONTROLLER_Z;
        canonical.getOffsetABC(offset, out);
    }

    public ItemStack itemOf(Block block, int meta, int x, int y, int z) {
        // Block.getPickBlock calls the client-only getItem, so both sides use the block item instead.
        Item item = Item.getItemFromBlock(block);
        if (item == null) return null;
        return new ItemStack(item, 1, ProjectorStructure.damageOf(world, block, meta, x, y, z));
    }

    public void placeMachine(int id, int x, int y, int z) {
        ItemStack stack = ProjectorMachines.stackOf(id);
        if (stack == null || !place(stack, x, y, z)) return;
        if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile) tile.setFrontFacing(outward(x, y, z));
    }

    public boolean place(ItemStack stack, int x, int y, int z) {
        if (!(stack.getItem() instanceof ItemBlock itemBlock)) return false;
        return itemBlock.placeBlockAt(
            stack.copy(),
            actor,
            world,
            x,
            y,
            z,
            ForgeDirection.UP.ordinal(),
            0.5f,
            0.5f,
            0.5f,
            itemBlock.getMetadata(stack.getItemDamage()));
    }

    ForgeDirection outward(int x, int y, int z) {
        ForgeDirection vertical = ForgeDirection.UNKNOWN;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int nx = x + direction.offsetX, ny = y + direction.offsetY, nz = z + direction.offsetZ;
            if (!world.isAirBlock(nx, ny, nz) || elements.containsKey(ProjectorPlan.pack(nx, ny, nz))) continue;
            if (direction.offsetY == 0) return direction;
            if (vertical == ForgeDirection.UNKNOWN) vertical = direction;
        }
        return vertical == ForgeDirection.UNKNOWN ? canonical.getDirection() : vertical;
    }

    public boolean checks(IStructureElement<Object> element, int x, int y, int z) {
        try {
            return element.check(machine, world, x, y, z);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static class Actor extends EntityPlayer {

        public Actor(World world) {
            super(world, PROFILE);
        }

        @Override
        public void addChatMessage(IChatComponent message) {}

        @Override
        public void addChatComponentMessage(IChatComponent message) {}

        @Override
        public boolean canCommandSenderUseCommand(int level, String command) {
            return false;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }
    }

    public static final class Visits {

        @SubscribeEvent
        public void visited(StructureElementVisitedEvent event) {
            if (!(event.getInstrumentIdentifier() instanceof ProjectorSimulation simulation)
                || event.getWorld() != simulation.world
                || WirelessControllerContext.current() == simulation.machine) return;
            @SuppressWarnings("unchecked")
            IStructureElement<Object> element = (IStructureElement<Object>) event.getElement();
            simulation.elements.put(ProjectorPlan.pack(event.getX(), event.getY(), event.getZ()), element);
        }

    }
}
