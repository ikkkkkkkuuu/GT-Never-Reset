package com.xyp.gtneverreset.common.machine.multiblock.crop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.farming.registries.MutationRegistry;
import com.gtnewhorizon.cropsnh.farming.requirements.breeding.WorldOnlyBreedingRequirement;

public final class CropBreedingPlanner {

    private CropBreedingPlanner() {}

    public static Plan plan(String target, Set<String> archive) {
        return plan(target, archive, MutationRegistry.instance.getDeterministicMutations());
    }

    public static Plan plan(String target, Set<String> archive, Collection<ICropMutation> recipes) {
        List<ICropMutation> mutations = new ArrayList<>(recipes);
        mutations.removeIf(
            m -> m.getParents()
                .isEmpty()
                || m.getRequirements()
                    .stream()
                    .anyMatch(WorldOnlyBreedingRequirement.class::isInstance));
        mutations.sort(Comparator.comparing(CropBreedingPlanner::signature));
        Map<String, Candidate> candidates = new HashMap<>();
        for (String id : archive) candidates.put(id, new Candidate());
        boolean changed;
        do {
            changed = false;
            for (ICropMutation mutation : mutations) {
                String result = mutation.getOutput()
                    .getId();
                if (archive.contains(result)) continue;
                Candidate next = resolve(mutation, candidates);
                if (next == null) continue;
                Candidate old = candidates.get(result);
                if (old == null || next.cost < old.cost) {
                    candidates.put(result, next);
                    changed = true;
                }
            }
        } while (changed);

        Candidate best = candidates.get(target);
        List<String> reachable = new ArrayList<>(candidates.keySet());
        reachable.removeAll(archive);
        Collections.sort(reachable);
        return new Plan(best == null ? Collections.emptyList() : new ArrayList<>(best.steps.values()), reachable);
    }

    private static Candidate resolve(ICropMutation mutation, Map<String, Candidate> candidates) {
        Candidate result = new Candidate();
        String output = mutation.getOutput()
            .getId();
        for (ICropCard parent : mutation.getParents()) {
            Candidate source = candidates.get(parent.getId());
            if (source == null || parent.getId()
                .equals(output) || source.steps.containsKey(output)) return null;
            result.steps.putAll(source.steps);
        }
        result.steps.put(output, mutation);
        Map<String, ICropMutation> pending = new LinkedHashMap<>(result.steps);
        result.steps.clear();
        while (!pending.isEmpty()) {
            boolean added = false;
            var iterator = pending.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                boolean ready = true;
                for (ICropCard parent : entry.getValue()
                    .getParents()) {
                    Candidate source = candidates.get(parent.getId());
                    if (source != null && source.steps.isEmpty()) continue;
                    if (pending.containsKey(parent.getId())) {
                        ready = false;
                        break;
                    }
                }
                if (!ready) continue;
                result.steps.put(entry.getKey(), entry.getValue());
                iterator.remove();
                added = true;
            }
            if (!added) return null;
        }
        for (ICropMutation step : result.steps.values())
            result.cost += CropBreedingRecipeRules.baseDuration(step.getOutput());
        return result;
    }

    private static String signature(ICropMutation mutation) {
        StringBuilder result = new StringBuilder(
            mutation.getOutput()
                .getId());
        for (ICropCard parent : mutation.getParents()) result.append('|')
            .append(parent.getId());
        return result.toString();
    }

    private static final class Candidate {

        private final Map<String, ICropMutation> steps = new LinkedHashMap<>();
        private long cost;
    }

    public static final class Plan {

        public final List<ICropMutation> steps;
        public final List<String> reachable;

        private Plan(List<ICropMutation> steps, List<String> reachable) {
            this.steps = Collections.unmodifiableList(steps);
            this.reachable = Collections.unmodifiableList(reachable);
        }
    }
}
