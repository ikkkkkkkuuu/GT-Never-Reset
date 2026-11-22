package com.xyp.gtneverreset.client.projector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorStructures {

    public static final long STEP_NANOS = 8_000_000L;
    private static final int CACHED = 4, QUEUED = 3, UNWANTED_TICKS = 10;
    private static final Map<ProjectorPlan.Settings, ProjectorStructure> CACHE = new LinkedHashMap<>(8, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<ProjectorPlan.Settings, ProjectorStructure> eldest) {
            return size() > CACHED;
        }
    };
    private static final Deque<ProjectorPlan.Settings> QUEUE = new ArrayDeque<>();
    private static ProjectorSimulation running;
    private static int unwantedTicks;
    private static ProjectorPlan.Settings lastAsked;
    private static ProjectorStructure lastFound;

    private ProjectorStructures() {}

    public static ProjectorStructure get(ProjectorPlan.Settings settings) {
        if (settings.machine() <= 0) return null;
        // The GUI asks many times a frame, and hashing a plan with many changes is not free.
        if (settings == lastAsked && lastFound != null) return lastFound;
        ProjectorStructure structure = CACHE.get(settings);
        if (structure == null) request(settings);
        lastAsked = settings;
        lastFound = structure;
        return structure;
    }

    public static ProjectorSimulation running(ProjectorPlan.Settings settings) {
        return running != null && running.settings.equals(settings) ? running : null;
    }

    private static void request(ProjectorPlan.Settings settings) {
        if (running != null && running.settings.equals(settings)) {
            unwantedTicks = 0;
            return;
        }
        if (QUEUE.contains(settings)) return;
        QUEUE.addLast(settings);
        while (QUEUE.size() > QUEUED) QUEUE.removeFirst();
    }

    public static void tick() {
        if (running != null && ++unwantedTicks > UNWANTED_TICKS && !QUEUE.isEmpty()) running = null;
        if (running == null) {
            unwantedTicks = 0;
            ProjectorPlan.Settings next = QUEUE.pollLast();
            if (next == null) return;
            running = new ProjectorSimulation(next, ProjectorSimulation.CLIENT_BUDGET);
        }
        if (running.step(STEP_NANOS)) {
            CACHE.put(running.settings, running.result());
            lastAsked = null;
            running = null;
        }
    }

    public static void clear() {
        CACHE.clear();
        lastAsked = null;
        lastFound = null;
        QUEUE.clear();
        running = null;
    }
}
