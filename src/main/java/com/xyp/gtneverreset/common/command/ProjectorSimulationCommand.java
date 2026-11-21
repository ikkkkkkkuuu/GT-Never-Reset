package com.xyp.gtneverreset.common.command;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ProjectorSimulationCommand extends CommandBase {

    public static final String PROBE_ENVIRONMENT = "GTNR_PROJECTOR_PROBE", REPORT = "projector-probe";

    @Desugar
    private record Row(int id, String name, ProjectorStructure structure) {}

    @Override
    public String getCommandName() {
        return "gtneverreset_projector_sim";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtneverreset_projector_sim <machine id|all> [hatch tier]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) throw new WrongUsageException(getCommandUsage(sender));
        int tier = args.length > 1 ? parseIntBounded(sender, args[1], 0, ProjectorPlan.MAX_HATCH_TIER)
            : ProjectorPlan.DEFAULT_HATCH_TIER;
        if ("all".equals(args[0])) {
            sender.addChatMessage(new ChatComponentText(runAll(tier)));
            return;
        }
        int id = parseInt(sender, args[0]);
        if (!ProjectorMachines.isSupported(id)) {
            sender.addChatMessage(new ChatComponentText("machine " + id + " has no survival build"));
            return;
        }
        String line = describe(simulate(id, tier));
        GTNeverReset.LOG.info("[projector-probe] {}", line);
        sender.addChatMessage(new ChatComponentText(line));
    }

    public static void scheduleProbe() {
        if (System.getenv(PROBE_ENVIRONMENT) == null) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new Probe());
    }

    public static class Probe {

        private boolean done;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (done || event.phase != TickEvent.Phase.END) return;
            done = true;
            GTNeverReset.LOG.info("[projector-probe] {}", runAll(ProjectorPlan.DEFAULT_HATCH_TIER));
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    private static Row simulate(int id, int tier) {
        ProjectorPlan plan = new ProjectorPlan();
        plan.selectMachine(id);
        plan.hatchTier = tier;
        return new Row(
            id,
            ProjectorMachines.prototype(id)
                .getLocalName(),
            ProjectorSimulation.run(plan.settings()));
    }

    private static String runAll(int tier) {
        List<String> rows = new ArrayList<>();
        long start = System.nanoTime();
        int failures = 0, slots = 0, empty = 0;
        for (int id : ProjectorMachines.supported()) {
            Row row = simulate(id, tier);
            rows.add(describe(row));
            if (!row.structure().failure.isEmpty()) failures++;
            if (!row.structure().hatchSlots.isEmpty()) slots++;
            if (row.structure().emptyCells.length > 0) empty++;
        }
        long total = System.nanoTime() - start;
        String summary = String.format(
            Locale.ROOT,
            "simulated %d machines in %.1f s: failures %d, with hatch slots %d, with empty cells %d",
            rows.size(),
            total / 1e9,
            failures,
            slots,
            empty);
        File file = MinecraftServer.getServer()
            .getFile(REPORT + ".tsv");
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            writer.println(summary);
            for (String row : rows) writer.println(row);
        } catch (IOException e) {
            GTNeverReset.LOG.error("[projector-probe] cannot write {}", file, e);
        }
        return summary + " -> " + file.getAbsolutePath();
    }

    private static String describe(Row row) {
        ProjectorStructure structure = row.structure();
        return String.format(
            Locale.ROOT,
            "%d\t%s\t%d blocks\t%dx%dx%d\t%d rounds\t%.1f ms\tlimit=%s\tslots=%d\tempty=%d\tchannels=%s\tmaterials=%d\tshown=%d\taccepted=%d\tchoices=%d\tsig=%08x\t%s",
            row.id(),
            row.name(),
            structure.size,
            structure.maxA - structure.minA + 1,
            structure.maxB - structure.minB + 1,
            structure.maxC - structure.minC + 1,
            structure.rounds,
            structure.nanos / 1e6,
            structure.limit,
            structure.hatchSlots.size(),
            structure.emptyCells.length,
            structure.channels,
            structure.materials.size(),
            count(structure, true),
            count(structure, false),
            structure.choices.size(),
            signature(structure),
            structure.failure);
    }

    private static int count(ProjectorStructure structure, boolean shown) {
        int total = 0;
        for (HatchSlot slot : structure.hatchSlots) total += (shown ? slot.candidates() : slot.accepted()).length;
        return total;
    }

    private static int signature(ProjectorStructure structure) {
        int sig = 1;
        for (int[] values : new int[][] { structure.a, structure.b, structure.c, structure.metas, structure.machines,
            structure.slots, structure.choiceOf }) sig = 31 * sig + Arrays.hashCode(values);
        for (Block block : structure.blocks) sig = 31 * sig + Block.getIdFromBlock(block);
        sig = 31 * sig + Arrays.hashCode(structure.emptyCells);
        for (HatchSlot slot : structure.hatchSlots)
            sig = 31 * (31 * sig + Arrays.hashCode(slot.candidates())) + Arrays.hashCode(slot.accepted());
        for (Choice choice : structure.choices) sig = 31 * (31 * sig + choice.key()
            .hashCode()) + Arrays.hashCode(choice.tiers());
        return sig;
    }
}
