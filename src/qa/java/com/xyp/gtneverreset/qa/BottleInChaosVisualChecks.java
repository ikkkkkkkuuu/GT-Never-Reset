package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.Nxer.TwistSpaceTechnology.common.GTCMItemList;
import com.Nxer.TwistSpaceTechnology.common.init.TstBlocks;
import com.Nxer.TwistSpaceTechnology.common.modularizedMachine.MM_BottledChaos;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.structure.error.StructureError;

@Mod(
    modid = "bottleinchaosvisualqa",
    name = "Bottle in Chaos visual comparison",
    version = "1",
    dependencies = "after:gtneverreset;after:TwistSpaceTechnology")
public final class BottleInChaosVisualChecks {

    private final BaseMetaTileEntity[] tiles = new BaseMetaTileEntity[2];
    private final StringBuilder report = new StringBuilder();
    private boolean launched, prepared, finished;
    private volatile Throwable failure;
    private volatile boolean clearGroup;
    private volatile int requested = -1;
    private int menuTicks, machineIndex, state, frames, openDelay;
    private static final String[] STATES = { "runtime", "runtime-scrolled", "groups", "pools", "pools-installed",
        "modules", "modules-scrolled", "cache-limit", "empty-group", "empty-group-routes" };

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (failure != null) {
            finish(failure);
            return;
        }
        if (!launched && mc.theWorld == null && mc.currentScreen != null && ++menuTicks > 100) {
            launched = true;
            mc.launchIntegratedServer(
                "chaos-visual-" + System.currentTimeMillis(),
                "Chaos comparison",
                new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (!prepared) {
                setup(player, 0);
                setup(player, 1);
                prepared = true;
                requested = 0;
            }
            if (clearGroup) {
                ((BottleInChaos) tiles[1].getMetaTileEntity()).getManagedCredentialHatch(0)
                    .setSelectedRoutes(0);
                clearGroup = false;
            }
            if (requested >= 0) {
                player.playerNetServerHandler.setPlayerLocation(requested * 96 + 0.5, 80, -4, 0, 0);
                if (++openDelay < 60) return;
                openDelay = 0;
                int index = requested;
                requested = -1;
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(index * 96 + 0.5, 80, -4, 0, 0);
                require(
                    index == 0 ? ((MM_BottledChaos) tiles[index].getMetaTileEntity()).onRightclick(tiles[index], player)
                        : ((BottleInChaos) tiles[index].getMetaTileEntity()).onRightclick(tiles[index], player),
                    "open " + index);
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void setup(EntityPlayerMP player, int index) {
        boolean original = index == 0;
        int offset = index * 96;
        var world = player.worldObj;
        BaseMetaTileEntity tile = place(
            player,
            offset,
            80,
            0,
            original ? GTCMItemList.BottledChaos.get(1)
                .getItemDamage() : GTNRMachineID.BottleInChaos.id);
        tiles[index] = tile;
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        if (original) ((MM_BottledChaos) tile.getMetaTileEntity()).construct(new ItemStack(Items.feather, 64), false);
        else((BottleInChaos) tile.getMetaTileEntity()).construct(new ItemStack(Items.feather, 64), false);
        String[][] shape = MultiblockStructureLoader.load("bottle_in_chaos");
        List<int[]> central = new ArrayList<>();
        for (int layer = 0; layer < shape.length; layer++) {
            for (int row = 0; row < shape[layer].length; row++) {
                for (int column = 0; column < shape[layer][row].length(); column++) {
                    char symbol = shape[layer][row].charAt(column);
                    int x = offset + column - 14, y = 115 - layer, z = row - 12;
                    if (symbol == 'U') place(
                        player,
                        x,
                        y,
                        z,
                        original ? GTCMItemList.BottledChaosRecipeTokenHatch.get(1)
                            .getItemDamage() : GTNRMachineID.ChaosCredentialHatch.id);
                    if (symbol == 'V' || symbol == 'W' || symbol == 'X' || symbol == 'Y') world
                        .setBlock(x, y, z, original ? TstBlocks.MetaBlockCasing02 : BlockLoader.chaosCasing02, 2, 3);
                    if (symbol == 'Y') central.add(new int[] { x, y, z });
                }
            }
        }
        place(
            player,
            offset - 2,
            80,
            0,
            original ? GTCMItemList.StaticParallelControllerT7.get(1)
                .getItemDamage() : GTNRMachineID.StaticParallelControllerT7.id);
        var hatches = new gregtech.api.enums.ItemList[] { gregtech.api.enums.ItemList.Hatch_Input_Bus_LV,
            gregtech.api.enums.ItemList.Hatch_Output_Bus_LV, gregtech.api.enums.ItemList.Hatch_Input_LV,
            gregtech.api.enums.ItemList.Hatch_Output_LV, gregtech.api.enums.ItemList.Hatch_Energy_LV };
        for (int i = 0; i < hatches.length; i++) {
            int[] position = central.get(i);
            place(
                player,
                position[0],
                position[1],
                position[2],
                hatches[i].get(1)
                    .getItemDamage());
        }
        var errors = new ArrayList<StructureError>();
        if (original) {
            var machine = (MM_BottledChaos) tile.getMetaTileEntity();
            machine.checkMachine(tile, null, errors);
            machine.mMachine = true;
            for (int i = 0; i < 15; i++) {
                var hatch = machine.getManagedCredentialHatch(i);
                hatch.setExecutionEnabled(false);
                hatch.setInventorySlotContents(
                    0,
                    com.Nxer.TwistSpaceTechnology.common.item.ItemBottledChaosCredential.createCredential(
                        5,
                        1,
                        com.Nxer.TwistSpaceTechnology.common.item.ItemBottledChaosCredential.getRequiredWeight(1)));
            }
            require(
                machine.getMachineConfiguration()
                    .getModuleSlotLimit() == 48,
                "original 48 module slots");
        } else {
            var machine = (BottleInChaos) tile.getMetaTileEntity();
            machine.checkMachine(tile, null, errors);
            machine.mMachine = true;
            for (int i = 0; i < 15; i++) {
                machine.getManagedCredentialHatch(i)
                    .setExecutionEnabled(false);
                machine.getManagedCredentialHatch(i)
                    .setSelectedRoutes(1);
            }
            require(
                machine.getMachineConfiguration()
                    .getModuleSlotLimit() == 48,
                "port 48 module slots");
        }
        require(errors.isEmpty(), "formed " + index + ": " + errors);
        var controllerIds = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
            .displayRules()
            .stream()
            .map(
                r -> r.controller()
                    .getItemDamage())
            .distinct()
            .filter(
                id -> GregTechAPI.METATILEENTITIES[id] instanceof gregtech.api.metatileentity.implementations.MTEBasicMachine)
            .sorted()
            .limit(12)
            .toList();
        for (int slot = 0; slot < 48; slot++) {
            int id = controllerIds.get(slot % controllerIds.size());
            if (original) {
                var rule = com.Nxer.TwistSpaceTechnology.common.modularizedMachine.bottledChaos.machine.BottledChaosStructureCatalog
                    .displayRules()
                    .stream()
                    .filter(
                        r -> r.controller()
                            .getItemDamage() == id)
                    .findFirst()
                    .orElseThrow();
                ((MM_BottledChaos) tile.getMetaTileEntity()).getMachineConfiguration()
                    .machines()
                    .setStackInSlot(
                        slot,
                        com.Nxer.TwistSpaceTechnology.common.item.ItemBottledChaosMachineModule
                            .create(rule.example(), 1));
            } else {
                var rule = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
                    .displayRules()
                    .stream()
                    .filter(
                        r -> r.controller()
                            .getItemDamage() == id)
                    .findFirst()
                    .orElseThrow();
                ((BottleInChaos) tile.getMetaTileEntity()).getMachineConfiguration()
                    .machines()
                    .setStackInSlot(
                        slot,
                        com.xyp.gtneverreset.common.item.items.ChaosStructureModuleItem.create(rule.example(), 1));
            }
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !prepared || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return;
        frames++;
        try {
            if ((state == 3 || state == 4) && frames == 40) selectPool(gui, state == 3 ? 0 : 1, state == 3 ? 0 : 2);
            if (frames < 160) return;
            String prefix = machineIndex == 0 ? "original-" : "port-";
            capture(prefix + STATES[state], gui);
            frames = 0;
            state++;
            if (state == 1 || state == 6) {
                var lists = all(
                    gui.getScreen()
                        .getMainPanel()).stream()
                            .filter(w -> w instanceof ListWidget && w.getArea().width == 420)
                            .toList();
                require(!lists.isEmpty(), "scrolling list found");
                var list = (ListWidget<?, ?>) lists.get(state == 1 ? 0 : lists.size() - 1);
                list.getScrollData()
                    .scrollTo(list.getScrollArea(), 10000);
                require(list.getScrollY() > 0, "scroll offset advances");
            } else if (state == 2 || state == 3) clickTab(gui, state - 1);
            else if (state == 4) {} else if (state == 5) clickTab(gui, 3);
            else if (state == 7) {
                clickTab(gui, 1);
                gui.getScreen()
                    .getSyncManager()
                    .getMainPSM()
                    .findPanelHandlerNullable("bottledChaosManagedHatchLimit")
                    .openPanel();
            } else if (state == 8 && machineIndex == 1) {
                gui.getScreen()
                    .getPanelManager()
                    .closeTopPanel();
                clearGroup = true;
            } else if (state == 9 && machineIndex == 1) {
                var routes = all(
                    gui.getScreen()
                        .getMainPanel()).stream()
                            .filter(w -> "bottledChaosManagedHatchROUTES".equals(w.getName()))
                            .findFirst()
                            .orElseThrow();
                require(routes.isEnabled(), "empty processing group retains enabled route selector");
                var area = routes.getArea();
                gui.getScreen()
                    .getContext()
                    .updateState(area.x + area.width / 2, area.y + area.height / 2, 0);
                gui.getScreen()
                    .onFrameUpdate();
                require(routes.isHovering(), "route selector passes pointer hit testing");
                require(
                    gui.getScreen()
                        .onMousePressed(0),
                    "route selector accepts press");
                require(
                    gui.getScreen()
                        .onMouseRelease(0),
                    "route selector accepts release");
            } else if (machineIndex++ == 0) {
                state = 0;
                requested = 1;
            } else finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void selectPool(GuiContainerWrapper gui, int page, int row) {
        var root = gui.getScreen()
            .getMainPanel();
        var widgets = all(root);
        var previous = widgets.stream()
            .filter(
                w -> w instanceof ButtonWidget && w.getArea().width == 18
                    && w.getArea().height == 18
                    && w.getArea().x == root.getArea().x + 14
                    && w.getArea().y == root.getArea().y + 186)
            .findFirst()
            .orElseThrow();
        for (int i = 0; i < 20; i++) ((ButtonWidget<?>) previous).onMousePressed(0);
        if (page > 0) {
            var next = widgets.stream()
                .filter(
                    w -> w instanceof ButtonWidget && w.getArea().width == 18
                        && w.getArea().height == 18
                        && w.getArea().x == root.getArea().x + 152
                        && w.getArea().y == root.getArea().y + 186)
                .findFirst()
                .orElseThrow();
            for (int i = 0; i < page; i++) ((ButtonWidget<?>) next).onMousePressed(0);
        }
        var selection = widgets.stream()
            .filter(
                w -> w instanceof ButtonWidget && w.getArea().height == 23
                    && w.getArea().x == root.getArea().x + 14
                    && w.getArea().y == root.getArea().y + 35 + row * 25)
            .findFirst()
            .orElseThrow();
        ((ButtonWidget<?>) selection).onMousePressed(0);
        require(true, "aligned pool page " + page + " row " + row);
    }

    private void clickTab(GuiContainerWrapper gui, int page) {
        var root = gui.getScreen()
            .getMainPanel();
        var button = all(root).stream()
            .filter(
                w -> w instanceof ButtonWidget && w.getArea().width == 80
                    && w.getArea().height == 16
                    && w.getArea().x - root.getArea().x == 8 + page * 80)
            .findFirst()
            .orElseThrow();
        ((ButtonWidget<?>) button).onMousePressed(0);
    }

    private void capture(String name, GuiContainerWrapper gui) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper
            .saveScreenshot(new File("."), name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        List<String> bounds = new ArrayList<>();
        for (var panel : gui.getScreen()
            .getPanelManager()
            .getOpenPanels()) {
            for (IWidget widget : all(panel)) {
                var a = widget.getArea();
                bounds.add(
                    widget.getClass()
                        .getName() + "|"
                        + a.x
                        + ","
                        + a.y
                        + ","
                        + a.width
                        + ","
                        + a.height
                        + "|"
                        + widget.isEnabled());
            }
        }
        Files.write(Path.of(name + "-bounds.txt"), bounds);
        require(true, "captured " + name);
    }

    private static List<IWidget> all(IWidget root) {
        List<IWidget> list = new ArrayList<>();
        list.add(root);
        for (IWidget child : root.getChildren()) list.addAll(all(child));
        return list;
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x, int y, int z, int id) {
        player.worldObj.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[chaos-visual] " + message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception ignored) {}
        FMLCommonHandler.instance()
            .exitJava(error == null ? 0 : 1, true);
    }
}
