package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.crafting.chaos.*;
import com.xyp.gtneverreset.common.item.items.*;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;
import com.xyp.gtneverreset.common.me.ChaosPatternCompiler;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.AEApi;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

@Mod(modid = "bottleinchaosqa", name = "Bottle in Chaos QA", version = "1", dependencies = "after:gtneverreset")
public final class BottleInChaosChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, opened, finished;
    private volatile boolean ready;
    private volatile Throwable failure;
    private int ticks, frames, stage;
    private BaseMetaTileEntity tile;
    private BottleInChaos machine;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (!checked) {
                checked = true;
                verify(player);
                ready = true;
            }
            if (ready && ++ticks > 60 && !opened) {
                require(machine.onRightclick(tile, player), "controller GUI opens");
                opened = true;
            }
            if (stage == 1) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(15, 88, -18, 35, 12);
                stage = 2;
            }
            if (stage == 3 && tile.isAllowedToWork()) {
                require(true, "GUI button synchronizes power toggle to server");
                tile.disableWorking();
                stage = 1;
            }
            if (stage == 4) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
                var hatchTile = (BaseMetaTileEntity) player.worldObj.getTileEntity(-3, 80, 0);
                require(
                    ((ChaosCredentialHatch) hatchTile.getMetaTileEntity()).onRightclick(hatchTile, player),
                    "credential hatch GUI opens");
                stage = 5;
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "chaos-" + System.currentTimeMillis(),
                    "Chaos QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server verification", failure);
            if (ticks > 1800) throw new AssertionError("GUI timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 0 && mc.currentScreen instanceof GuiContainerWrapper gui && ++frames > 60) {
                capture("chaos-gui.png");
                var tooltip = GTNRItemList.BottleInChaos.get(1)
                    .getTooltip(mc.thePlayer, false);
                Files.write(Path.of("tooltip.txt"), tooltip);
                require(
                    !String.join("\n", tooltip)
                        .contains("{var:"),
                    "tooltip variables resolved");
                firstButton(
                    gui.getScreen()
                        .getMainPanel()).onMousePressed(0);
                stage = 3;
                frames = 0;
            } else if (stage == 2 && ++frames > 80) {
                capture("chaos-structure.png");
                stage = 4;
                frames = 0;
            } else if (stage == 5 && mc.currentScreen instanceof GuiContainerWrapper && ++frames > 60) {
                capture("chaos-hatch.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verify(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        require(
            ChaosPresets.all()
                .size() == 6,
            "six real ore presets registered");
        for (ChaosChain preset : ChaosPresets.all()) {
            require(
                ChaosChain.load(preset.save())
                    .steps()
                    .size() == 4,
                "preset resolves: " + preset.name());
        }
        ChaosBuffer longBuffer = new ChaosBuffer();
        longBuffer.add(new ItemStack(Items.diamond), (long) Integer.MAX_VALUE + 19);
        ChaosBuffer restored = ChaosBuffer.load(longBuffer.save());
        require(restored.amount() == (long) Integer.MAX_VALUE + 19, "long cache quantities survive NBT");
        restored.removeSlice(restored.view());
        require(
            restored.amount() == 19 && longBuffer.amount() > (long) Integer.MAX_VALUE,
            "sliced output is detached and lossless");

        ItemStack raw = marked(Items.brick, "raw"), intermediate = marked(Items.coal, "intermediate"),
            product = marked(Items.diamond, "product");
        GTRecipe first = GTValues.RA.stdBuilder()
            .itemInputs(raw.copy())
            .itemOutputs(intermediate.copy())
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.maceratorRecipes)
            .iterator()
            .next();
        GTRecipe second = GTValues.RA.stdBuilder()
            .itemInputs(intermediate.copy())
            .itemOutputs(product.copy())
            .duration(20)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes)
            .iterator()
            .next();
        ChaosChain chain = new ChaosChain(
            "QA",
            List.of(
                new ChaosChain.Step(RecipeMaps.maceratorRecipes, first, 1),
                new ChaosChain.Step(RecipeMaps.compressorRecipes, second, 1)));
        ItemStack a = pattern(raw, intermediate, RecipeMaps.maceratorRecipes);
        ItemStack b = pattern(intermediate, product, RecipeMaps.compressorRecipes);
        require(
            ChaosPatternCompiler.compile(List.of(a, b), world)
                .steps()
                .size() == 2,
            "two real AE processing patterns compile");
        boolean rejected = false;
        try {
            ChaosPatternCompiler
                .compile(List.of(pattern(raw, new ItemStack(Items.emerald), RecipeMaps.maceratorRecipes)), world);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "forged AE output rejected");
        GTValues.RA.stdBuilder()
            .itemInputs(raw.copy())
            .itemOutputs(intermediate.copy())
            .duration(40)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes);
        ItemStack ambiguous = a.copy();
        ambiguous.getTagCompound()
            .removeTag("gtnrPatternRecipeMap");
        rejected = false;
        try {
            ChaosPatternCompiler.compile(List.of(ambiguous), world);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "cross-pool ambiguity rejected without map identifier");
        require(
            ChaosPatternCompiler.compile(List.of(a), world)
                .steps()
                .get(0)
                .map() == RecipeMaps.maceratorRecipes,
            "map identifier disambiguates real recipes");
        var water = new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 100);
        first.mFluidOutputs = new net.minecraftforge.fluids.FluidStack[] { water };
        first.mFluidOutputChances = new int[] { 5000 };
        ItemStack randomFluid = a.copy();
        randomFluid.getTagCompound()
            .getTagList("out", 10)
            .appendTag(
                appeng.util.item.AEFluidStack.create(water)
                    .toNBTGeneric());
        rejected = false;
        try {
            ChaosPatternCompiler.compile(List.of(randomFluid), world);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        first.mFluidOutputs = new net.minecraftforge.fluids.FluidStack[0];
        first.mFluidOutputChances = null;
        require(rejected, "probabilistic fluid output cannot authorize an exact AE promise");
        NBTTagCompound tampered = chain.save();
        tampered.getTagList("steps", 10)
            .getCompoundTagAt(0)
            .setString("map", "unknown");
        rejected = false;
        try {
            ChaosChain.load(tampered);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "unknown recipe pool rejected");

        tile = place(world, 0, 80, 0, GTNRMachineID.BottleInChaos.id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        machine = (BottleInChaos) tile.getMetaTileEntity();
        machine.fixAllIssues();
        machine.construct(new ItemStack(Items.stick), false);
        String[][] shape = MultiblockStructureLoader.load("bottle_in_chaos");
        require(
            shape.length == 13 && shape[0].length == 11 && shape[8][0].charAt(5) == '~',
            "11 x 13 x 11 binary bottle structure");
        require(
            machine.getStructureDefinition()
                .check(machine, "main", world, machine.getExtendedFacing(), 0, 80, 0, 5, 8, 0, false),
            "constructed structure matches binary shape");
        var credential = ChaosCredentialItem.bind(GTNRItemList.ChaosCredential.get(1), chain);
        var h0 = (ChaosCredentialHatch) place(world, -3, 80, 0, GTNRMachineID.ChaosCredentialHatch.id)
            .getMetaTileEntity();
        var h1 = (ChaosCredentialHatch) place(world, 3, 80, 0, GTNRMachineID.ChaosCredentialHatch.id)
            .getMetaTileEntity();
        h0.setInventorySlotContents(0, credential.copy());
        h1.setInventorySlotContents(0, credential.copy());
        var input = (MTEHatchInputBus) place(
            world,
            -4,
            78,
            0,
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage()).getMetaTileEntity();
        var output = (MTEHatchOutputBus) place(
            world,
            -2,
            78,
            0,
            ItemList.Hatch_Output_Bus_LV.get(1)
                .getItemDamage()).getMetaTileEntity();
        var energy = place(
            world,
            -1,
            78,
            0,
            ItemList.Hatch_Energy_LV.get(1)
                .getItemDamage());
        place(
            world,
            1,
            78,
            0,
            ItemList.Hatch_Maintenance.get(1)
                .getItemDamage());
        place(
            world,
            2,
            78,
            0,
            ItemList.Hatch_Output_LV.get(1)
                .getItemDamage());
        require(machine.checkStructure(true, tile), "controller forms with native hatches and two credential groups");
        machine.workbench()
            .setStackInSlot(0, module(RecipeMaps.maceratorRecipes));
        machine.workbench()
            .setStackInSlot(1, module(RecipeMaps.compressorRecipes));
        machine.workbench()
            .setStackInSlot(BottleInChaos.MODULE_INPUT, GTNRItemList.ChaosStructureModule.get(1));
        machine.workbench()
            .setStackInSlot(
                BottleInChaos.MACHINE_INPUT,
                ChaosStructureModuleItem.unpack(module(RecipeMaps.maceratorRecipes))
                    .getStackForm(1));
        machine.workbench()
            .setStackInSlot(BottleInChaos.CASING_INPUT, ItemList.Casing_SolidSteel.get(8));
        machine.packageModule();
        require(
            ChaosStructureModuleItem.unpack(
                machine.workbench()
                    .getStackInSlot(BottleInChaos.MODULE_OUTPUT))
                != null
                && machine.workbench()
                    .getStackInSlot(BottleInChaos.MACHINE_INPUT) == null
                && machine.workbench()
                    .getStackInSlot(BottleInChaos.CASING_INPUT) == null,
            "module packaging consumes the machine and eight casings");
        machine.workbench()
            .setStackInSlot(BottleInChaos.CREDENTIAL_INPUT, GTNRItemList.ChaosCredential.get(1));
        machine.workbench()
            .setStackInSlot(BottleInChaos.PATTERN_START, a.copy());
        machine.workbench()
            .setStackInSlot(BottleInChaos.PATTERN_START + 1, b.copy());
        machine.bindCredential(true);
        require(
            ChaosCredentialItem.isBound(
                machine.workbench()
                    .getStackInSlot(BottleInChaos.CREDENTIAL_OUTPUT))
                && machine.workbench()
                    .getStackInSlot(BottleInChaos.CREDENTIAL_INPUT) == null
                && machine.workbench()
                    .getStackInSlot(BottleInChaos.PATTERN_START) != null,
            "AE injection consumes only the blank credential");
        Method run = BottleInChaos.class.getDeclaredMethod("runMachine", IGregTechTileEntity.class, long.class);
        run.setAccessible(true);
        input.setInventorySlotContents(0, raw.copy());
        tile.enableWorking();
        energy.setStoredEU(1000000);
        run.invoke(machine, tile, 5L);
        int started = 0;
        for (int i = 0; i < 2; i++) if (machine.group(i)
            .remaining() > 0) started++;
        require(started == 1, "two groups cannot spend the same shared input");
        int active = machine.group(0)
            .remaining() > 0 ? 0 : 1;
        int remaining = machine.group(active)
            .remaining();
        energy.setStoredEU(0);
        run.invoke(machine, tile, 6L);
        require(
            machine.group(active)
                .remaining() == remaining,
            "power loss pauses paid work");
        world.setBlockToAir(1, 78, 0);
        require(!machine.checkStructure(true, tile), "missing maintenance hatch invalidates structure");
        var startup = MTEMultiBlockBase.class.getDeclaredField("mStartUpCheck");
        startup.setAccessible(true);
        startup.setInt(machine, -1);
        machine.onPostTick(tile, 7L);
        require(
            machine.group(active)
                .remaining() == remaining && !tile.isAllowedToWork(),
            "native tick stops incomplete structure without losing work");
        place(
            world,
            1,
            78,
            0,
            ItemList.Hatch_Maintenance.get(1)
                .getItemDamage());
        require(machine.checkStructure(true, tile), "structure reforms with retained work");
        tile.enableWorking();
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        require(
            machine.group(active)
                .remaining() == remaining,
            "in-flight progress restored");
        require(
            machine.workbench()
                .extractItem(0, 1, false) == null,
            "installed module cannot be extracted during work");
        for (int i = 0; i < output.getSizeInventory(); i++)
            output.setInventorySlotContents(i, new ItemStack(net.minecraft.init.Blocks.cobblestone, 64));
        for (long t = 7; t < 180; t++) {
            energy.setStoredEU(1000000);
            run.invoke(machine, tile, t);
        }
        require(
            machine.group(active)
                .busy()
                && machine.group(active)
                    .remaining() == 0,
            "blocked output retains completed chain");
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        for (int i = 0; i < output.getSizeInventory(); i++) output.setInventorySlotContents(i, null);
        tile.disableWorking();
        for (long t = 180; t < 190; t++) run.invoke(machine, tile, t);
        int produced = 0;
        for (int i = 0; i < output.getSizeInventory(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(product) && ItemStack.areItemStackTagsEqual(stack, product))
                produced += stack.stackSize;
        }
        require(
            produced == 1,
            "two-step chain consumes cached intermediate and delivers once after blocked-output reload");
        require(
            machine.group(active)
                .cache()
                .isEmpty(),
            "internal intermediate fully consumed");
        machine.exportGroup(1 - active);
        for (long t = 190; t < 200; t++) run.invoke(machine, tile, t);
        require(!machine.hasWork(), "idle bound group can be exported and unlocked");
        var dedicated = (MTEHatchInputBus) place(
            world,
            -3,
            79,
            0,
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage()).getMetaTileEntity();
        require(machine.checkStructure(true, tile), "dedicated input position forms");
        var groupField = ChaosCredentialHatch.class.getDeclaredField("group");
        groupField.setAccessible(true);
        int owner = groupField.getInt(h0), other = 1 - owner;
        dedicated.setInventorySlotContents(0, raw.copy());
        machine.group(owner)
            .setEnabled(false);
        machine.group(other)
            .setEnabled(true);
        tile.enableWorking();
        energy.setStoredEU(1000000);
        run.invoke(machine, tile, 200L);
        require(
            machine.group(other)
                .remaining() == 0 && dedicated.getStackInSlot(0) != null,
            "group cannot consume another group's dedicated input");
        machine.group(owner)
            .setEnabled(true);
        run.invoke(machine, tile, 205L);
        require(
            machine.group(owner)
                .remaining() > 0 && dedicated.getStackInSlot(0) == null,
            "owner group consumes dedicated input");
        machine.exportGroup(owner);
        machine.group(other)
            .setEnabled(false);
        for (int i = 0; i < output.getSizeInventory(); i++) output.setInventorySlotContents(i, null);
        for (long t = 206; t < 260; t++) {
            energy.setStoredEU(1000000);
            run.invoke(machine, tile, t);
        }
        int intermediates = 0;
        for (int i = 0; i < output.getSizeInventory(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (stack != null && stack.isItemEqual(intermediate)) intermediates += stack.stackSize;
        }
        require(
            intermediates == 1 && !machine.group(owner)
                .busy()
                && !machine.group(owner)
                    .enabled(),
            "export finishes paid step and skips remaining steps without discarding output");
        tile.disableWorking();
        machine.exportGroup(other);
        run.invoke(machine, tile, 260L);
        NBTTagCompound beforeCapacity = new NBTTagCompound();
        machine.saveNBTData(beforeCapacity);
        machine.group(owner)
            .bind(chain.save());
        machine.group(owner)
            .setEnabled(true);
        machine.group(owner)
            .cache()
            .add(new ItemStack(Items.apple), machine.capacityPerGroup());
        input.setInventorySlotContents(0, raw.copy());
        tile.enableWorking();
        energy.setStoredEU(1000000);
        run.invoke(machine, tile, 300L);
        require(
            machine.group(owner)
                .remaining() == 0 && machine.group(owner)
                    .status()
                    .equals("gtnr.chaos.status.capacity")
                && input.getStackInSlot(0).stackSize == 1,
            "capacity failure occurs before consuming external input");
        machine.loadNBTData(beforeCapacity);
        input.setInventorySlotContents(0, null);
        tile.disableWorking();
        machine.exportGroup(owner);
        machine.toggleGroup(owner);
        run.invoke(machine, tile, 305L);
        require(
            !machine.group(owner)
                .exporting(),
            "exporting an idle group does not prevent later reuse");
        NBTTagCompound itemState = new NBTTagCompound();
        machine.setItemNBT(itemState);
        var replaced = place(world, 20, 80, 0, GTNRMachineID.BottleInChaos.id);
        replaced.setInitialValuesAsNBT(itemState, (short) GTNRMachineID.BottleInChaos.id);
        require(
            ((BottleInChaos) replaced.getMetaTileEntity()).workbench()
                .getStackInSlot(0) != null,
            "breaking and placing preserves installed modules");
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
    }

    private ItemStack module(RecipeMap<?> map) {
        for (var entry : GregTechAPI.METATILEENTITIES)
            if (entry instanceof MTEBasicMachine basic && basic.mTier == 1 && basic.getRecipeMap() == map)
                return ChaosStructureModuleItem.pack(GTNRItemList.ChaosStructureModule.get(1), basic.getStackForm(1));
        throw new AssertionError("missing module " + map.unlocalizedName);
    }

    private static com.cleanroommc.modularui.widgets.ButtonWidget<?> firstButton(
        com.cleanroommc.modularui.api.widget.IWidget widget) {
        if (widget instanceof com.cleanroommc.modularui.widgets.ButtonWidget<?>button) return button;
        for (var child : widget.getChildren()) {
            var found = firstButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private static ItemStack marked(net.minecraft.item.Item item, String name) {
        ItemStack result = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("chaosQa", name);
        result.setTagCompound(tag);
        return result;
    }

    private static ItemStack pattern(ItemStack input, ItemStack output, RecipeMap<?> map) {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        in.appendTag(
            AEItemStack.create(input)
                .toNBTGeneric());
        out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setString("gtnrPatternRecipeMap", map.unlocalizedName);
        result.setTagCompound(tag);
        return result;
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var result = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        result.setInitialValuesAsNBT(null, (short) id);
        return result;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[chaos-qa] PASS " + message);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "chaos-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
