package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.CircuitSharedSupplies;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.common.me.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.common.me.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.common.me.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.ITileDefinition;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.tileentities.machines.multi.MTELatex;

@Mod(modid = "cableorderqa", name = "Wildcard Cable Order QA", version = "1", dependencies = "after:gtneverreset")
public final class WildcardCableOrderChecks {

    private final List<Rig> rigs = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();
    private volatile Throwable failure;
    private volatile boolean complete;
    private boolean launched, finished;
    private int frames, serverTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (complete || failure != null || event.phase != TickEvent.Phase.END || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (rigs.isEmpty()) {
                int x = (int) player.posX, z = (int) player.posZ;
                rigs.add(new Rig(player.worldObj, x - 5, z, GTNRMachineID.CircuitMEPatternBuffer));
                rigs.add(new Rig(player.worldObj, x + 5, z, GTNRMachineID.CrossRecipeMEPatternBuffer));
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(x + 0.5, 87, z - 13, 0, 18);
            }
            serverTicks++;
            for (Rig rig : rigs) rig.tick(serverTicks);
            if (rigs.stream()
                .allMatch(r -> r.order == 3)) {
                Files.writeString(Path.of("orders.txt"), report);
                complete = true;
            }
        } catch (Throwable t) {
            failure = t;
        }
    }

    private final class Rig {

        final GTNRMachineID type;
        final World world;
        final CircuitMEPatternBuffer hatch;
        final MTELatex coater;
        final BaseMetaTileEntity energy;
        final TileDrive drive;
        ICraftingCPU cpu;
        Future<ICraftingJob> plan;
        ICraftingLink link;
        boolean seeded;
        int order, before, seededAt, insufficientTicks;
        boolean toppedUp, dustAdded;

        Rig(World world, int x, int z, GTNRMachineID type) {
            this.type = type;
            this.world = world;
            int id = 0;
            while (id < GregTechAPI.METATILEENTITIES.length && !(GregTechAPI.METATILEENTITIES[id] instanceof MTELatex))
                id++;
            require(id < GregTechAPI.METATILEENTITIES.length, "coater registered");
            var controller = place(world, x, 80, z, id);
            controller.setFrontFacing(ForgeDirection.NORTH);
            coater = (MTELatex) controller.getMetaTileEntity();
            coater.construct(new ItemStack(Items.stick), false);
            // Normal first-tier pipe casings give the coater its 6.25% rubber discount.
            var casing = new ArrayList<int[]>();
            for (int dy = 0; dy <= 7; dy++) for (int dz = -4; dz <= 4; dz++) for (int dx = -2; dx <= 2; dx++)
                if (world.getBlock(x + dx, 80 + dy, z + dz) == GregTechAPI.sBlockCasings8
                    && world.getBlockMetadata(x + dx, 80 + dy, z + dz) == 0)
                    casing.add(new int[] { x + dx, 80 + dy, z + dz });
            require(casing.size() >= 18, "coater casing count " + casing.size());
            int[] pos = casing.get(0);
            var base = place(world, pos[0], pos[1], pos[2], type.id);
            base.setFrontFacing(ForgeDirection.NORTH);
            hatch = (CircuitMEPatternBuffer) base.getMetaTileEntity();
            pos = casing.get(1);
            energy = place(
                world,
                pos[0],
                pos[1],
                pos[2],
                ItemList.Hatch_Energy_MV.get(1)
                    .getItemDamage());
            pos = casing.get(2);
            place(
                world,
                pos[0],
                pos[1],
                pos[2],
                ItemList.Hatch_Maintenance.get(1)
                    .getItemDamage());
            coater.fixAllIssues();
            require(
                coater.checkStructure(true, controller) && coater.mDualInputHatches.contains(hatch),
                type + " formed native cable coater");
            controller.enableWorking();
            int hx = base.xCoord, hy = base.yCoord, hz = base.zCoord;
            var blocks = AEApi.instance()
                .definitions()
                .blocks();
            placeAE(world, hx, hy, hz - 1, blocks.drive());
            drive = (TileDrive) world.getTileEntity(hx, hy, hz - 1);
            drive.setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell1k()
                    .maybeStack(1)
                    .get());
            drive.setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL1K));
            drive.setInventorySlotContents(2, new ItemStack(ItemAndBlockHolder.CELL1K));
            placeAE(world, hx, hy, hz - 2, blocks.creativeEnergyController());
            placeAE(world, hx, hy, hz - 3, blocks.craftingStorage1k());
            installPattern(1, "^(Tin|Lead)$", true);
        }

        private void installPattern(int batch, String materials, boolean withCircuit) {
            ItemStack wildcard = GTNRItemList.WildcardPattern.get(1);
            WildcardPatternGenerator.markAsWildcard(wildcard);
            WildcardModelState.setInputs(wildcard, List.of(new PrefixIOComponent(OrePrefixes.wireGt01, batch)));
            WildcardModelState.setOutputs(wildcard, List.of(new PrefixIOComponent(OrePrefixes.cableGt01, batch)));
            WildcardModelState.setFilters(wildcard, List.of(new StringFilterComponent(materials, true)));
            if (withCircuit) wildcard.getTagCompound()
                .setInteger(CircuitPatternCodec.CIRCUIT, 24);
            PatternRecipeMap.write(wildcard, RecipeMaps.cableRecipes.unlocalizedName);
            hatch.setInventorySlotContents(0, wildcard);
        }

        int rubber() {
            var fluids = hatch.getSharedSupplies()
                .fluidInputs();
            return fluids.isEmpty() ? 0 : fluids.get(0).amount;
        }

        long stored(IAEItemStack key) throws Exception {
            var stack = hatch.getProxy()
                .getStorage()
                .getItemInventory()
                .extractItems(
                    key.copy()
                        .setStackSize(64),
                    Actionable.SIMULATE,
                    hatch.getMEOutputActionSource());
            return stack == null ? 0 : stack.getStackSize();
        }

        void tick(int ticks) throws Exception {
            if (order == 3) return;
            energy.increaseStoredEnergyUnits(8192, true);
            if (ticks > 1200) throw new AssertionError(
                type + " timeout order="
                    + order
                    + " seeded="
                    + seeded
                    + " CPU="
                    + cpu
                    + " hatchActive="
                    + hatch.isActive()
                    + " stock="
                    + rubber()
                    + " plan="
                    + plan
                    + " submitted="
                    + (link != null)
                    + " formed="
                    + coater.mMachine
                    + " activeTicks="
                    + coater.mMaxProgresstime);
            if (!hatch.isActive()) return;
            var proxy = hatch.getProxy();
            var crafting = proxy.getCrafting();
            if (crafting.getCpus()
                .isEmpty()) return;
            cpu = crafting.getCpus()
                .iterator()
                .next();
            Materials material = order == 0 ? Materials.Tin : order == 1 ? Materials.Lead : Materials.Zinc;
            int count = order == 0 ? 2 : order == 1 ? 3 : 4;
            var wire = item(OrePrefixes.wireGt01, material, count);
            var cable = item(OrePrefixes.cableGt01, material, count);
            var options = crafting.getCraftingFor((IAEStack<?>) cable, null, 0, world);
            if (options.isEmpty()) return;
            var src = hatch.getMEOutputActionSource();
            var fluidInv = proxy.getStorage()
                .getFluidInventory();
            var rubberStack = AEFluidStack
                .create((order == 2 ? Materials.RubberSilicone : Materials.Rubber).getMolten(1000));
            var storageItems = proxy.getStorage()
                .getItemInventory();
            var dust = AEItemStack.create(Materials.PolyvinylChloride.getDust(5));
            if (!seeded) {
                require(storageItems.injectItems(dust.copy(), Actionable.MODULATE, src) == null, "seed PVC dust");
                hatch.getSharedSupplies()
                    .setTarget(0, dust);
                require(
                    fluidInv.injectItems(
                        rubberStack.copy()
                            .setStackSize(100),
                        Actionable.MODULATE,
                        src) == null,
                    "load rubber");
                for (Materials metal : new Materials[] { Materials.Tin, Materials.Lead }) {
                    require(
                        proxy.getStorage()
                            .getItemInventory()
                            .injectItems(
                                item(OrePrefixes.wireGt01, metal, metal == Materials.Tin ? 2 : 3),
                                Actionable.MODULATE,
                                src)
                            == null,
                        "load bare wires");
                }
                hatch.getSharedSupplies()
                    .setTarget(CircuitSharedSupplies.SLOTS_PER_CHANNEL, rubberStack);
                hatch.getSharedSupplies()
                    .setInterval(1);
                require(
                    hatch.getSharedSupplies()
                        .directFluids(),
                    "default mode draws fluid from ME");
                hatch.getSharedSupplies()
                    .setOnRecipeCheck(true);
                seededAt = ticks;
                seeded = true;
                return;
            }
            require(
                hatch.getSharedSupplies()
                    .itemInputs()
                    .isEmpty(),
                "marked items never prefill the hatch");
            if (order < 2) require(stored(dust) == 5, "unused marked items remain in ME");
            if (plan == null) {
                if (ticks - seededAt < 60) return;
                require(rubber() == 0, "idle timer never moves network fluid into the hatch");
                var available = fluidInv.extractItems(rubberStack, Actionable.SIMULATE, src);
                require(
                    available != null && available.getStackSize() == (order == 0 ? 100 : order == 1 ? 730 : 1000),
                    "idle ME balance");
                var inputs = options.iterator()
                    .next()
                    .getCondensedAEInputs();
                require(
                    inputs.length == 1 && wire.isSameType(inputs[0])
                        && inputs[0].getStackSize() == (order == 2 ? 4 : 1),
                    "AE requests only bare wires");
                require(
                    stored(cable) == 0 && stored(wire) == count,
                    "order starts with bare wire and zero finished cables");
                before = (int) available.getStackSize();
                plan = crafting.beginCraftingJob(world, proxy.getGrid(), src, (IAEStack<?>) cable, null);
                return;
            }
            if (link == null) {
                if (!plan.isDone()) return;
                var job = plan.get();
                require(!job.isSimulation(), "AE produced a craftable plan");
                link = crafting.submitJob(job, null, cpu, false, src);
                require(link != null, type + " accepted order for " + count + " " + material + " cables");
                return;
            }
            require(!link.isCanceled(), "AE job canceled");
            require(rubber() == 0, "recipe checks never prefill the hatch");
            if (!toppedUp) {
                if (hatch.getFirstNonEmptyInventory()
                    .isEmpty() || ++insufficientTicks < 100) return;
                require(
                    stored(cable) == 0 && coater.mMaxProgresstime == 0
                        && fluidInv.extractItems(rubberStack, Actionable.SIMULATE, src)
                            .getStackSize() == 100,
                    "insufficient rubber leaves the entire ME balance untouched");
                require(
                    fluidInv.injectItems(
                        rubberStack.copy()
                            .setStackSize(900),
                        Actionable.MODULATE,
                        src) == null,
                    "supply remaining rubber");
                toppedUp = true;
                before += 900;
            }
            if (order == 2 && !dustAdded) {
                if (hatch.getFirstNonEmptyInventory()
                    .isEmpty() || ++insufficientTicks < 100) return;
                require(
                    stored(cable) == 0 && stored(dust) == 0
                        && coater.mMaxProgresstime == 0
                        && fluidInv.extractItems(rubberStack, Actionable.SIMULATE, src)
                            .getStackSize() == 1000,
                    "missing marked item leaves fluid in ME");
                require(storageItems.injectItems(dust.copy(), Actionable.MODULATE, src) == null, "deliver PVC dust");
                dustAdded = true;
            }
            if (cpu.isBusy() || stored(cable) != count) return;
            int after = (int) fluidInv.extractItems(rubberStack, Actionable.SIMULATE, src)
                .getStackSize();
            require(
                before - after == (order == 2 ? 135 : 135 * count) && stored(wire) == 0 && rubber() == 0,
                "unexpected network debit for " + type + " / " + material + ": " + before + " -> " + after);
            String line = type + " "
                + material
                + " cables ordered="
                + count
                + " returned="
                + stored(cable)
                + " bare wire remaining=0; ME molten rubber "
                + before
                + " -> "
                + after
                + " mB; local rubber=0";
            if (order == 2) {
                require(
                    stored(dust) == 4 && hatch.getSharedSupplies()
                        .itemInputs()
                        .isEmpty(),
                    "exactly one dust debited from ME");
                line += "; ME PVC dust 5 -> 4; local dust=0";
            }
            report.append(line)
                .append(System.lineSeparator());
            System.out.println("[wildcard-cable-orders] " + line);
            if (order == 1) {
                require(
                    storageItems.extractItems(dust.copy(), Actionable.MODULATE, src)
                        .getStackSize() == 5,
                    "empty dust cell for missing item check");
                require(
                    storageItems.injectItems(item(OrePrefixes.wireGt01, Materials.Zinc, 4), Actionable.MODULATE, src)
                        == null,
                    "seed zinc wire");
                var silicone = AEFluidStack.create(Materials.RubberSilicone.getMolten(1000));
                require(
                    fluidInv.injectItems(silicone.copy(), Actionable.MODULATE, src) == null,
                    "seed silicone rubber");
                hatch.getSharedSupplies()
                    .setTarget(CircuitSharedSupplies.SLOTS_PER_CHANNEL, silicone);
                installPattern(4, "^Zinc$", false);
                insufficientTicks = 0;
            }
            order++;
            plan = null;
            link = null;
        }
    }

    private static IAEItemStack item(OrePrefixes prefix, Materials material, int count) {
        return AEItemStack.create(GTOreDictUnificator.get(prefix, material, count));
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private static void placeAE(World world, int x, int y, int z, ITileDefinition def) {
        ItemStack stack = def.maybeStack(1)
            .get();
        world.setBlock(x, y, z, Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3);
    }

    private static void require(boolean test, String message) {
        if (!test) throw new AssertionError(message);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen != null && GregTechAPI.sFullLoadFinished) {
                launched = true;
                mc.launchIntegratedServer(
                    "cables-" + System.currentTimeMillis(),
                    "Wildcard cable orders",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("cable order fixture", failure);
            if (!complete || ++frames < 10) return;
            mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "cable-coaters.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void finish(Throwable error) {
        if (finished) return;
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception io) {
            io.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "wildcard-cable-orders-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

}
