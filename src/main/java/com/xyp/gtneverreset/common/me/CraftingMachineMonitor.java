package com.xyp.gtneverreset.common.me;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.mixins.late.gregtech.CraftingMonitorHatchAccessor;
import com.xyp.gtneverreset.mixins.late.gregtech.MultiblockProcessingAccessor;

import appeng.api.networking.crafting.CraftingItemList;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.DualityInterface;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.parts.AEBasePart;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/** Tracks real crafting handoffs on the server thread. */
public final class CraftingMachineMonitor {

    private static final int MAX_OUTPUTS = 256;
    private static final int MAX_ROUTES = 16;
    private static final Map<CraftingCPUCluster, Job> JOBS = new WeakHashMap<>();
    private static final ThreadLocal<Dispatch> DISPATCH = new ThreadLocal<>();

    private CraftingMachineMonitor() {}

    public static Dispatch begin(ICraftingMedium medium) {
        Dispatch dispatch = new Dispatch(medium, DISPATCH.get());
        DISPATCH.set(dispatch);
        return dispatch;
    }

    public static void end(Dispatch dispatch) {
        if (dispatch.previous == null) DISPATCH.remove();
        else DISPATCH.set(dispatch.previous);
    }

    public static void capture(ICraftingMedium medium, TileEntity provider, ForgeDirection providerSide,
        TileEntity machine, ForgeDirection machineSide) {
        Dispatch dispatch = DISPATCH.get();
        if (dispatch == null || dispatch.medium != medium || provider == null) return;
        if (dispatch.routes == null) dispatch.routes = new LinkedHashSet<>();
        if (dispatch.routes.size() < MAX_ROUTES) {
            dispatch.routes.add(new Route(provider, providerSide, machine, machineSide));
        }
    }

    public static void record(CraftingCPUCluster cpu, Dispatch dispatch, IAEStack<?>[] outputs) {
        String id = jobId(cpu);
        if (id == null) return;
        Set<Route> routes = dispatch.routes;
        if (routes == null) {
            Route direct = directRoute(dispatch.medium);
            if (direct == null) return;
            routes = Set.of(direct);
        }

        Job job = JOBS.get(cpu);
        if (job == null || !Objects.equals(job.id, id)) {
            job = new Job(id, new LinkedHashMap<>());
            JOBS.put(cpu, job);
        }
        for (IAEStack<?> output : outputs) {
            if (output == null || output.getStackSize() <= 0) continue;
            List<RecordedRoute> recorded = job.outputs.get(output);
            if (recorded == null) {
                if (job.outputs.size() == MAX_OUTPUTS) {
                    job.outputs.remove(
                        job.outputs.keySet()
                            .iterator()
                            .next());
                }
                recorded = new ArrayList<>();
                job.outputs.put(
                    output.copy()
                        .setStackSize(1),
                    recorded);
            }
            recorded.removeIf(route -> route.provider.get() == null);
            for (Route route : routes) {
                if (recorded.stream()
                    .anyMatch(existing -> existing.matches(route))) continue;
                if (recorded.size() == MAX_ROUTES) recorded.remove(0);
                recorded.add(new RecordedRoute(route));
            }
        }
    }

    public static void returned(CraftingCPUCluster cpu, IAEStack<?> stack) {
        Job job = JOBS.get(cpu);
        if (job == null || !job.outputs.containsKey(stack)) return;
        if (cpu.getStackAmount(stack, CraftingItemList.ACTIVE) <= 0) job.outputs.remove(stack);
        if (job.outputs.isEmpty()) JOBS.remove(cpu);
    }

    public static void clear(CraftingCPUCluster cpu) {
        JOBS.remove(cpu);
    }

    public static List<Target> find(CraftingCPUCluster cpu, IAEStack<?> stack) {
        Job job = JOBS.get(cpu);
        if (job == null || !Objects.equals(job.id, jobId(cpu))) return List.of();
        if (cpu.getStackAmount(stack, CraftingItemList.ACTIVE) <= 0) return List.of();
        List<RecordedRoute> routes = job.outputs.get(stack);
        if (routes == null) return List.of();

        Set<Route> loaded = new LinkedHashSet<>();
        for (RecordedRoute route : routes) {
            TileEntity provider = route.provider.get();
            if (!isLoaded(provider)) continue;
            TileEntity machine = route.machine.get();
            if (!isLoaded(machine)) machine = null;
            addMachines(loaded, new Route(provider, route.providerSide, machine, route.machineSide));
        }

        return loaded.stream()
            .limit(MAX_ROUTES)
            .map(route -> snapshot(route, stack))
            .toList();
    }

    private static String jobId(CraftingCPUCluster cpu) {
        ICraftingLink link = cpu.getLastCraftingLink();
        return link == null ? null : link.getCraftingID();
    }

    static Provider resolveProvider(ICraftingMedium medium) {
        if (medium instanceof IMetaTileEntity meta && meta.getBaseMetaTileEntity() instanceof TileEntity tile) {
            return new Provider(
                tile,
                meta.getBaseMetaTileEntity()
                    .getFrontFacing());
        }
        Object host = medium instanceof DualityInterface duality ? duality.getHost() : medium;
        if (host instanceof AEBasePart part && part.getTile() != null) {
            return new Provider(part.getTile(), part.getSide());
        }
        if (host instanceof TileEntity tile) return new Provider(tile, ForgeDirection.UNKNOWN);
        return null;
    }

    private static Route directRoute(ICraftingMedium medium) {
        Provider provider = resolveProvider(medium);
        if (provider == null) return null;
        TileEntity machine = medium instanceof IMetaTileEntity ? provider.tile : null;
        return new Route(provider.tile, provider.side, machine, provider.side);
    }

    private static void addMachines(Set<Route> routes, Route route) {
        if (!(route.machine instanceof IGregTechTileEntity gt) || !(gt.getMetaTileEntity() instanceof MTEHatch hatch)
            || !(hatch instanceof IDualInputHatch)) {
            routes.add(route);
            return;
        }
        boolean hasController = addHatchControllers(routes, route, hatch);
        if (hatch instanceof MTEHatchCraftingInputME aeHatch) {
            for (MTEHatch mirror : aeHatch.getProxyHatches())
                hasController |= addHatchControllers(routes, route, mirror);
        } else if (hatch instanceof SuperMTEHatchCraftingInputME superHatch) {
            for (MTEHatch mirror : superHatch.getProxyHatches())
                hasController |= addHatchControllers(routes, route, mirror);
        }
        if (!hasController) routes.add(new Route(route.provider, route.providerSide, null, ForgeDirection.UNKNOWN));
    }

    private static boolean addHatchControllers(Set<Route> routes, Route route, MTEHatch hatch) {
        if (!(hatch instanceof CraftingMonitorHatchAccessor accessor)) return false;
        boolean found = false;
        for (var watcher : accessor.gtnr$getWatchers()) {
            if (routes.size() >= MAX_ROUTES) return true;
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && controller.mDualInputHatches.contains(hatch)
                && controller.getBaseMetaTileEntity() instanceof TileEntity tile
                && isLoaded(tile)) {
                routes.add(
                    new Route(
                        route.provider,
                        route.providerSide,
                        tile,
                        controller.getBaseMetaTileEntity()
                            .getFrontFacing()));
                found = true;
            }
        }
        return found;
    }

    private static Target snapshot(Route route, IAEStack<?> stack) {
        if (!(route.machine instanceof IGregTechTileEntity gt)) return route.snapshot(0, 0, 0, "", false);
        IMetaTileEntity machine = gt.getMetaTileEntity();
        if (machine instanceof GTNRMultiBlockBase<?>gtnr) {
            RecipeThreadTask next = null;
            long parallel = 0;
            int usableThreads = gtnr.getTotalRecipeThreads();
            for (RecipeThreadTask task : gtnr.getRecipeThreadTasks()) {
                if (task.isFinished() || task.getThread() >= usableThreads || !hasOutput(task, stack)) continue;
                parallel += task.getParallels();
                if (next == null || task.getDuration() - task.getProgress() < next.getDuration() - next.getProgress()) {
                    next = task;
                }
            }
            if (next != null) {
                return route.snapshot(next.getProgress(), next.getDuration(), parallel, shutdown(gt), gt.isActive());
            }
        }

        int duration = gt.getMaxProgress();
        if (duration > 0 && hasOutput(machine, stack)) {
            long parallel = 0;
            if (machine instanceof MultiblockProcessingAccessor accessor) {
                var logic = accessor.gtnr$getProcessingLogic();
                if (logic != null) parallel = logic.getCurrentParallels();
            }
            return route.snapshot(
                Math.max(0, Math.min(gt.getProgress(), duration)),
                duration,
                parallel,
                shutdown(gt),
                gt.isActive());
        }

        return route.snapshot(0, 0, 0, gt.isActive() ? "" : shutdown(gt), false);
    }

    private static boolean hasOutput(RecipeThreadTask task, IAEStack<?> stack) {
        if (stack instanceof IAEItemStack item) return task.getOutputs()
            .hasItemOutput(item.getItemStack());
        if (stack instanceof IAEFluidStack fluid) return task.getOutputs()
            .hasFluidOutput(fluid.getFluidStack());
        return false;
    }

    private static boolean hasOutput(IMetaTileEntity machine, IAEStack<?> stack) {
        if (machine instanceof MTEMultiBlockBase multi) {
            return stack instanceof IAEItemStack item && contains(multi.mOutputItems, item)
                || stack instanceof IAEFluidStack fluid && contains(multi.mOutputFluids, fluid);
        }
        if (machine instanceof MTEBasicMachine basic) {
            return stack instanceof IAEItemStack item && contains(basic.mOutputItems, item)
                || stack instanceof IAEFluidStack fluid && basic.mOutputFluid != null
                    && fluid.equals(basic.mOutputFluid);
        }
        return false;
    }

    private static boolean contains(ItemStack[] outputs, IAEItemStack stack) {
        if (outputs == null) return false;
        for (ItemStack output : outputs) if (output != null && stack.equals(output)) return true;
        return false;
    }

    private static boolean contains(FluidStack[] outputs, IAEFluidStack stack) {
        if (outputs == null) return false;
        for (FluidStack output : outputs) if (output != null && stack.equals(output)) return true;
        return false;
    }

    private static String shutdown(IGregTechTileEntity gt) {
        if (gt.isActive()) return "";
        if (gt.wasShutdown()) return gt.getLastShutDownReason()
            .getDisplayString();
        if (gt.getMetaTileEntity() instanceof MTEMultiBlockBase multi && !multi.getCheckRecipeResult()
            .wasSuccessful()) {
            return multi.getCheckRecipeResult()
                .getDisplayString();
        }
        return "";
    }

    private static boolean isLoaded(TileEntity tile) {
        if (tile == null || tile.isInvalid()) return false;
        World world = tile.getWorldObj();
        return world != null && world.blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
            && world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile;
    }

    public record Target(TileEntity machine, TileEntity provider, ForgeDirection providerSide,
        ForgeDirection machineSide, int progress, int duration, long parallel, String shutdown, boolean active) {}

    public static final class Dispatch {

        private final ICraftingMedium medium;
        private final Dispatch previous;
        private Set<Route> routes;

        private Dispatch(ICraftingMedium medium, Dispatch previous) {
            this.medium = medium;
            this.previous = previous;
        }
    }

    private record Route(TileEntity provider, ForgeDirection providerSide, TileEntity machine,
        ForgeDirection machineSide) {

        private Target snapshot(int progress, int duration, long parallel, String shutdown, boolean active) {
            return new Target(
                machine,
                provider,
                providerSide,
                machineSide,
                progress,
                duration,
                parallel,
                shutdown,
                active);
        }
    }

    private record RecordedRoute(WeakReference<TileEntity> provider, ForgeDirection providerSide,
        WeakReference<TileEntity> machine, ForgeDirection machineSide) {

        private RecordedRoute(Route route) {
            this(
                new WeakReference<>(route.provider),
                route.providerSide,
                new WeakReference<>(route.machine),
                route.machineSide);
        }

        private boolean matches(Route route) {
            return provider.get() == route.provider && providerSide == route.providerSide
                && machine.get() == route.machine
                && machineSide == route.machineSide;
        }
    }

    record Provider(TileEntity tile, ForgeDirection side) {}

    private record Job(String id, Map<IAEStack<?>, List<RecordedRoute>> outputs) {}
}
