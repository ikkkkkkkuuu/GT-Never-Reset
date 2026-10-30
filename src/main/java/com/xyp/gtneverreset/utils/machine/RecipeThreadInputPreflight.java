package com.xyp.gtneverreset.utils.machine;

import static gregtech.api.util.GTUtility.validMTEList;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/** Native stocking hatches simulate independently; their shared inventory and AE energy need a combined check. */
public final class RecipeThreadInputPreflight {

    public static boolean canCommit(MTEMultiBlockBase machine, RecipeThreadProcessingLogic logic) {
        return check(machine, logic).wasSuccessful();
    }

    public static CheckRecipeResult check(MTEMultiBlockBase machine, RecipeThreadProcessingLogic logic) {
        Requests<IAEItemStack> items = new Requests<>();
        Requests<IAEFluidStack> fluids = new Requests<>();
        Map<IEnergyGrid, Long> energy = new IdentityHashMap<>();
        Set<ItemStack> seenItems = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<FluidStack> seenFluids = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (var input : validMTEList(machine.mInputBusses)) if (input instanceof MTEHatchInputBusME stocking) {
                AENetworkProxy proxy = stocking.getProxy();
                if (!proxy.isReady()) proxy.onReady();
                IMEMonitor<IAEItemStack> inventory = proxy.getStorage()
                    .getItemInventory();
                IEnergyGrid grid = proxy.getEnergy();
                int slots = stocking instanceof SuperAdvancedMEInputBus ? SuperAdvancedMEInputBus.SLOT_COUNT
                    : MTEHatchInputBusME.SLOT_COUNT;
                for (int slot = 0; slot < slots; slot++) {
                    ItemStack stack = stocking.getStackInSlot(slot);
                    if (stack == null || !seenItems.add(stack)) continue;
                    int amount = logic.getPlannedConsumption(stack);
                    if (amount <= 0) continue;
                    if (!(stocking.getBaseMetaTileEntity() instanceof IActionHost host))
                        return CheckRecipeResultRegistry.INTERNAL_ERROR;
                    IAEItemStack request = AEItemStack.create(stack)
                        .setStackSize(amount);
                    CheckRecipeResult result = items
                        .add(inventory, grid, request, new MachineSource(host), energy, false);
                    if (!result.wasSuccessful()) return result;
                }
            }
            for (var input : validMTEList(machine.mInputHatches)) if (input instanceof MTEHatchInputME stocking) {
                AENetworkProxy proxy = stocking.getProxy();
                if (!proxy.isReady()) proxy.onReady();
                IMEMonitor<IAEFluidStack> inventory = proxy.getStorage()
                    .getFluidInventory();
                IEnergyGrid grid = proxy.getEnergy();
                for (FluidStack stack : stocking.getStoredFluids()) {
                    if (stack == null || !seenFluids.add(stack)) continue;
                    int amount = logic.getPlannedConsumption(stack);
                    if (amount <= 0) continue;
                    if (!(stocking.getBaseMetaTileEntity() instanceof IActionHost host))
                        return CheckRecipeResultRegistry.INTERNAL_ERROR;
                    IAEFluidStack request = AEFluidStack.create(stack)
                        .setStackSize(amount);
                    CheckRecipeResult result = fluids
                        .add(inventory, grid, request, new MachineSource(host), energy, true);
                    if (!result.wasSuccessful()) return result;
                }
            }
            if (!items.available()) return stockUnavailable(false);
            if (!fluids.available()) return stockUnavailable(true);
            for (Map.Entry<IEnergyGrid, Long> entry : energy.entrySet()) {
                double available = entry.getKey()
                    .extractAEPower(entry.getValue(), Actionable.SIMULATE, PowerMultiplier.CONFIG);
                if (!hasSufficientEnergy(available, entry.getValue())) return energyUnavailable();
            }
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } catch (GridAccessException failure) {
            // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_network_unavailable
            // # Cannot access the ME input network
            // # zh_CN 无法访问 ME 输入网络
            return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_network_unavailable");
        } catch (ArithmeticException failure) {
            // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_request_overflow
            // # ME input request exceeds the supported amount
            // # zh_CN ME 输入请求数量溢出
            return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_request_overflow");
        }
    }

    static boolean hasSufficientEnergy(double available, long required) {
        // AE multiplies and divides by the configured ratio even for an infinite energy grid.
        // Allow only rounding error from that conversion, not a missing item unit of energy.
        return available >= required
            || (available > 0 && required - available <= Math.min(0.001, 2 * Math.ulp((double) required)));
    }

    private static CheckRecipeResult stockUnavailable(boolean fluid) {
        if (fluid) {
            // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_fluid_unavailable
            // # ME fluid extraction simulation returned less than the planned amount
            // # zh_CN ME 流体模拟抽取量不足本次计划用量
            return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_fluid_unavailable");
        }
        // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_item_unavailable
        // # ME item extraction simulation returned less than the planned amount
        // # zh_CN ME 物品模拟抽取量不足本次计划用量
        return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_item_unavailable");
    }

    private static CheckRecipeResult energyUnavailable() {
        // #tr GT5U.gui.text.recipe_result.gtnr_thread_me_energy_unavailable
        // # ME extraction energy simulation returned less than the planned amount
        // # zh_CN ME 抽取能量模拟返回值不足本次计划用量
        return SimpleCheckRecipeResult.ofFailure("gtnr_thread_me_energy_unavailable");
    }

    private static final class Requests<T extends IAEStack<T>> {

        private final Map<IMEMonitor<T>, Map<T, Request<T>>> inventories = new IdentityHashMap<>();

        private CheckRecipeResult add(IMEMonitor<T> inventory, IEnergyGrid grid, T stack, BaseActionSource source,
            Map<IEnergyGrid, Long> energy, boolean fluid) {
            T stock = inventory.extractItems(stack.copy(), Actionable.SIMULATE, source);
            if (stock == null || stock.getStackSize() != stack.getStackSize()) return stockUnavailable(fluid);
            long units = (stack.getStackSize() + stack.getAmountPerUnit() - 1) / stack.getAmountPerUnit();
            double power = grid.extractAEPower(units, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            // Match Platform.poweredExtraction's conversion from AE power to extractable units.
            if ((long) (power * stack.getAmountPerUnit() + 0.9) < stack.getStackSize()) return energyUnavailable();
            energy.put(grid, Math.addExact(energy.getOrDefault(grid, 0L), units));
            Map<T, Request<T>> requests = inventories.computeIfAbsent(inventory, ignored -> new HashMap<>());
            Request<T> request = requests.get(stack);
            if (request == null) requests.put(stack, new Request<>(stack.copy(), source));
            else request.stack.setStackSize(Math.addExact(request.stack.getStackSize(), stack.getStackSize()));
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        private boolean available() {
            for (Map.Entry<IMEMonitor<T>, Map<T, Request<T>>> inventory : inventories.entrySet()) {
                for (Request<T> request : inventory.getValue()
                    .values()) {
                    T simulated = inventory.getKey()
                        .extractItems(request.stack.copy(), Actionable.SIMULATE, request.source);
                    if (simulated == null || simulated.getStackSize() != request.stack.getStackSize()) return false;
                }
            }
            return true;
        }
    }

    private static final class Request<T extends IAEStack<T>> {

        private final T stack;
        private final BaseActionSource source;

        private Request(T stack, BaseActionSource source) {
            this.stack = stack;
            this.source = source;
        }
    }
}
