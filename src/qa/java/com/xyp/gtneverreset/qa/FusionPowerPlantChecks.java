package com.xyp.gtneverreset.qa;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.xyp.gtneverreset.common.machine.multiblock.FusionPowerPlant;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(modid = "fusionpowerplantqa", name = "Fusion Power Plant QA", version = "1", dependencies = "after:gtneverreset")
public final class FusionPowerPlantChecks {

    private final StringBuilder report = new StringBuilder();
    private volatile Throwable failure;
    private volatile boolean ready;
    private volatile int stage;
    private boolean launched, checked, opened, finished;
    private int ticks, frames, timeout;
    private Rig display;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.fusion.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        var player = (EntityPlayerMP) players.get(0);
        try {
            if (!checked) {
                checked = true;
                verify(player);
                ready = true;
            }
            if (ready) {
                display.supply();
                if (++ticks > 60 && !opened) {
                    require(display.machine.onRightclick(display.tile, player), "native controller GUI opens");
                    opened = true;
                }
                if (stage == 1 && display.machine.getTargetPower() == 37) {
                    require(true, "target power synchronizes from client to server");
                    stage = 2;
                }
                if (stage == 3 && !display.tile.isAllowedToWork()) {
                    require(
                        display.machine.getTemperature() >= FusionPowerPlant.AMBIENT_K,
                        "GUI shutdown retains temperature");
                    stage = 4;
                }
                if (stage == 5 && display.tile.isAllowedToWork()) {
                    require(true, "GUI ignite action reaches server");
                    stage = 6;
                }
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.limitFramerate = 60;
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "fusion-" + System.currentTimeMillis(),
                    "Fusion QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server checks", failure);
            if (++timeout > 4800) throw new AssertionError("QA timeout, stage " + stage);
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainerWrapper gui) || ++frames < 40) return;
            frames = 0;
            var panel = gui.getScreen()
                .getMainPanel();
            var sync = panel.getScreen()
                .getSyncManager()
                .getMainPSM();
            if (stage == 0) {
                capture("fusion-running-scale-2.png");
                stage = 1;
                sync.findSyncHandler("fusionTarget", IntSyncValue.class)
                    .setIntValue(37);
            } else if (stage == 2) {
                capture("fusion-power-37.png");
                stage = 3;
                click(panel, "fusionStop");
            } else if (stage == 4) {
                capture("fusion-stopped.png");
                stage = 5;
                click(panel, "fusionStart");
            } else if (stage == 6) {
                mc.gameSettings.guiScale = 3;
                mc.resize(mc.displayWidth, mc.displayHeight);
                stage = 7;
            } else if (stage == 7) {
                require(
                    panel.getArea().width <= gui.width && panel.getArea().height <= gui.height,
                    "scale 3 panel fits viewport");
                require(
                    panel.getArea().y >= 14 && panel.getArea().y + panel.getArea().height <= gui.height,
                    "scale 3 title tab and inventory stay inside viewport");
                capture("fusion-scale-3.png");
                mc.gameSettings.guiScale = 1;
                mc.resize(mc.displayWidth, mc.displayHeight);
                stage = 8;
            } else if (stage == 8) {
                capture("fusion-scale-1.png");
                var manager = mc.getLanguageManager();
                manager.setCurrentLanguage(
                    manager.getLanguages()
                        .stream()
                        .filter(
                            language -> language.getLanguageCode()
                                .equals("en_US"))
                        .findFirst()
                        .orElseThrow());
                mc.gameSettings.language = "en_US";
                mc.refreshResources();
                mc.fontRenderer.setUnicodeFlag(manager.isCurrentLocaleUnicode() || mc.gameSettings.forceUnicodeFont);
                mc.gameSettings.guiScale = 2;
                mc.resize(mc.displayWidth, mc.displayHeight);
                stage = 9;
            } else if (stage == 9) {
                capture("fusion-english.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verify(EntityPlayerMP player) throws Exception {
        var shape = MultiblockStructureLoader.load("fusion_power_plant");
        require(shape.length == 3 && shape[0].length == 3 && shape[0][0].length() == 3, "MBS1 dimensions are 3x3x3");
        display = new Rig(player, 0);
        var machine = display.machine;
        require(machine.checkStructure(true, machine.getBaseMetaTileEntity()), "prototype forms with required hatches");
        var tooltip = new AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
        String text = String.join("\n", tooltip.getInformation())
            + String.join("\n", tooltip.getStructureInformation());
        Files.writeString(Path.of("fusion-tooltip.txt"), text);
        require(
            !text.contains("{var:") && text.replace(",", "")
                .contains("1000"),
            "automatic tooltip resolves prototype limits");
        machine.requestStart();
        for (int i = 0; i < 100; i++) display.tick(true);
        require(machine.getGeneratedEUt() == FusionPowerPlant.RATED_EUT, "ignition leads to rated generation");
        require(machine.getExportedEUt() > 0, "generated EU is transferred wirelessly");
        int fuel = display.inputs.get(0)
            .getFluidAmount();
        int coolant = display.inputs.get(2)
            .getFluidAmount();
        BigInteger wirelessBefore = WirelessNetworkManager.getUserEU(player.getUniqueID());
        for (int i = 0; i < 20; i++) display.tick(true);
        require(
            WirelessNetworkManager.getUserEU(player.getUniqueID())
                .subtract(wirelessBefore)
                .equals(BigInteger.valueOf(FusionPowerPlant.RATED_EUT * 20L)),
            "wireless account receives exactly the generated EU once");
        require(
            display.inputs.get(0)
                .getFluidAmount() == fuel - 1,
            "full power consumes 1 L deuterium per second");
        require(
            display.inputs.get(2)
                .getFluidAmount() < coolant,
            "coolant is actually consumed");
        require(
            display.outputs.stream()
                .anyMatch(
                    h -> h.getFluid() != null && h.getFluid()
                        .getFluid() == FluidRegistry.getFluid("ic2hotcoolant")),
            "hot coolant is actually produced");

        NBTTagCompound stored = new NBTTagCompound();
        machine.saveNBTData(stored);
        var detached = new Rig(player, -24).machine;
        detached.loadNBTData(stored);
        require(
            detached.getTemperature() == machine.getTemperature() && detached.getDecayHeat() == machine.getDecayHeat()
                && detached.getOutputEnergy() == machine.getOutputEnergy(),
            "NBT restores temperature, residual heat and output energy");
        machine.setTargetPower(Integer.MAX_VALUE);
        require(machine.getTargetPower() == 100, "server clamps target upper bound");
        machine.setTargetPower(-10);
        require(machine.getTargetPower() == 0, "server clamps target lower bound");
        machine.setTargetPower(100);

        display.setThermalState(600, 0.55);
        display.inputs.get(2)
            .drain(Integer.MAX_VALUE, true);
        machine.requestStop();
        double before = machine.getTemperature();
        display.tick(false);
        require(
            machine.getGeneratedEUt() == 0 && machine.getTemperature() > before,
            "shutdown stops generation but residual heat raises temperature without coolant");
        display.inputs.get(2)
            .fill(FluidRegistry.getFluidStack("ic2coolant", 10000), true);
        before = machine.getTemperature();
        display.tick(false);
        require(machine.getTemperature() < before, "coolant lowers temperature while stopped");
        for (var out : display.outputs) {
            out.drain(Integer.MAX_VALUE, true);
            out.fill(FluidRegistry.getFluidStack("ic2hotcoolant", out.getCapacity()), true);
        }
        coolant = display.inputs.get(2)
            .getFluidAmount();
        before = machine.getTemperature();
        display.tick(false);
        require(
            machine.getCoolantUsed() == 0 && display.inputs.get(2)
                .getFluidAmount() == coolant && machine.getTemperature() > before,
            "blocked hot output prevents coolant consumption and cooling");

        display.setThermalState(810, 0.55);
        display.tile.enableWorking();
        display.tick(false);
        require(!display.tile.isAllowedToWork() && machine.getGeneratedEUt() == 0, "overheat forces shutdown");
        machine.requestStart();
        require(!display.tile.isAllowedToWork(), "hot reactor cannot be restarted");

        display.setThermalState(400, 0.1);
        for (var out : display.outputs) out.drain(Integer.MAX_VALUE, true);
        machine.requestStart();
        for (int i = 0; i < 400; i++) display.tick(true);
        require(
            machine.getOutputEnergy() == 0 && machine.getExportedEUt() > 0,
            "wireless export needs no dynamo or energy buffer capacity");
        NBTTagCompound ownerState = new NBTTagCompound();
        machine.saveNBTData(ownerState);
        require(
            ownerState.getString("fusionOwner")
                .equals(
                    player.getUniqueID()
                        .toString()),
            "wireless owner is persisted");
        var blast = new Rig(player, 48);
        blast.setThermalState(999.9, 0.6);
        blast.inputs.get(2)
            .drain(Integer.MAX_VALUE, true);
        blast.machine.requestStop();
        blast.tick(false);
        require(
            blast.tile.isDead() || player.worldObj.getTileEntity(48, 80, 0) != blast.tile,
            "temperature threshold triggers an actual in-world explosion");

        var invalid = new Rig(player, 24);
        var hatch = invalid.inputs.get(0)
            .getBaseMetaTileEntity();
        player.worldObj.setBlockToAir(hatch.getXCoord(), hatch.getYCoord(), hatch.getZCoord());
        require(!invalid.machine.checkStructure(true, invalid.tile), "missing casing or hatch invalidates structure");
        invalid.setThermalState(600, 0.55);
        invalid.machine.checkStructure(true, invalid.tile);
        before = invalid.machine.getTemperature();
        invalid.tick(false);
        require(invalid.machine.getTemperature() > before, "breaking structure does not erase residual heat");

        display.setThermalState(360, 0.3);
        display.inputs.get(2)
            .fill(FluidRegistry.getFluidStack("ic2coolant", 50000), true);
        display.machine.requestStart();
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0, 81, -4, 0, 0);
    }

    private static void click(com.cleanroommc.modularui.api.widget.IWidget widget, String syncName) {
        if (widget instanceof com.cleanroommc.modularui.widgets.ButtonWidget<?>button && button.isSynced()
            && button.getSyncHandler() == widget.getScreen()
                .getSyncManager()
                .getMainPSM()
                .findSyncHandler(syncName, InteractionSyncHandler.class)) {
            button.onMousePressed(0);
            return;
        }
        for (var child : widget.getChildren()) click(child, syncName);
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[fusion-qa] PASS " + message);
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
        FMLCommonHandler.instance()
            .exitJava(error == null ? 0 : 1, false);
    }

    private static final class Rig {

        final BaseMetaTileEntity tile;
        final FusionPowerPlant machine;
        final List<MTEHatchInput> inputs = new ArrayList<>();
        final List<MTEHatchOutput> outputs = new ArrayList<>();
        final BaseMetaTileEntity energy;
        int tick;

        Rig(EntityPlayerMP player, int x) {
            World world = player.worldObj;
            tile = place(world, x, 80, 0, GTNRMachineID.FusionPowerPlant.id);
            tile.setFrontFacing(ForgeDirection.NORTH);
            tile.setOwnerName(player.getCommandSenderName());
            tile.setOwnerUuid(player.getUniqueID());
            tile.disableWorking();
            machine = (FusionPowerPlant) tile.getMetaTileEntity();
            machine.construct(new ItemStack(Items.stick), false);
            List<int[]> casings = new ArrayList<>();
            for (int dx = -2; dx <= 2; dx++) for (int y = 78; y <= 82; y++) for (int z = -3; z <= 3; z++) {
                if (world.getBlock(x + dx, y, z) == GregTechAPI.sBlockCasings2) casings.add(new int[] { x + dx, y, z });
            }
            if (casings.size() != 25) throw new AssertionError("expected 25 test casings, got " + casings.size());
            int[] ids = { ItemList.Hatch_Input_IV.get(1)
                .getItemDamage(),
                ItemList.Hatch_Input_IV.get(1)
                    .getItemDamage(),
                ItemList.Hatch_Input_IV.get(1)
                    .getItemDamage(),
                ItemList.Hatch_Output_IV.get(1)
                    .getItemDamage(),
                ItemList.Hatch_Output_IV.get(1)
                    .getItemDamage(),
                ItemList.Hatch_Energy_LuV.get(1)
                    .getItemDamage() };
            List<BaseMetaTileEntity> hatches = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                int[] pos = casings.get(i);
                var placed = place(world, pos[0], pos[1], pos[2], ids[i]);
                placed.setFrontFacing(ForgeDirection.DOWN);
                hatches.add(placed);
                if (i < 3) inputs.add((MTEHatchInput) placed.getMetaTileEntity());
                if (i == 3 || i == 4) outputs.add((MTEHatchOutput) placed.getMetaTileEntity());
            }
            energy = hatches.get(5);
            inputs.get(0)
                .fill(Materials.Deuterium.getGas(10000), true);
            inputs.get(1)
                .fill(Materials.Tritium.getGas(10000), true);
            inputs.get(2)
                .fill(FluidRegistry.getFluidStack("ic2coolant", 100000), true);
            if (!machine.checkStructure(true, machine.getBaseMetaTileEntity()))
                throw new AssertionError("test structure failed to form");
        }

        void supply() {
            var hatch = (MTEHatchEnergy) energy.getMetaTileEntity();
            energy.setStoredEU(hatch.maxEUStore());
        }

        void tick(boolean drainOutput) {
            if (drainOutput) supply();
            machine.onPostTick(tile, ++tick);
        }

        void setThermalState(double temperature, double decay) {
            NBTTagCompound tag = new NBTTagCompound();
            machine.saveNBTData(tag);
            tag.setDouble("fusionTemperature", temperature);
            tag.setDouble("fusionDecayHeat", decay);
            machine.loadNBTData(tag);
            machine.checkStructure(true, machine.getBaseMetaTileEntity());
        }

        private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
            world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
            var tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
            tile.setInitialValuesAsNBT(null, (short) id);
            return tile;
        }
    }
}
