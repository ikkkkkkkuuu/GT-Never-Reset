package com.xyp.gtneverreset.qa;

import java.io.File;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import org.lwjgl.input.Mouse;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.client.gui.WirelessPowerCardPanel;
import com.xyp.gtneverreset.common.gui.WirelessPowerCardGui;
import com.xyp.gtneverreset.common.gui.WirelessPowerCardGui.Session;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.world.WirelessPowerEvents;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.IWirelessPowerReceiver;
import com.xyp.gtneverreset.utils.machine.WirelessPowerTransfer;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessHatchOwner;
import com.xyp.gtneverreset.utils.world.WirelessPowerSavedData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchMuffler;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

@Mod(modid = "wirelesspowerqa", name = "Wireless power QA", version = "1", dependencies = "after:gtneverreset")
public final class WirelessPowerChecks {

    private final UUID sourceA = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID sourceB = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private boolean started, finished;
    private int ticks, frames, useDelay;
    private volatile int stage;
    private volatile String requestedSource;
    private ItemStack card, machine;
    private BaseMetaTileEntity receiver, automatic, canceled, furnace, energyA, energyB;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        Mouse.setCursorPosition(0, 0);
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "wireless-power-" + System.currentTimeMillis(),
                "Wireless power QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (stage == 5 && ++useDelay == 10) {
            require(mc.thePlayer.getHeldItem() != null, "fresh card synchronized before right-click");
            mc.thePlayer.rotationPitch = -90;
            stage = 1;
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
        }
        if (stage == 9 && mc.currentScreen == null) {
            try {
                var selection = com.xyp.gtneverreset.client.render.WirelessWorldHighlightRender.class
                    .getDeclaredField("selection");
                selection.setAccessible(true);
                require(selection.get(null) != null, "highlight action creates a world selection and closes the card");
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
            mc.thePlayer.rotationPitch = -90;
            mc.thePlayer.sendQueue.addToSendQueue(
                new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(
                    mc.thePlayer.rotationYaw,
                    -90,
                    mc.thePlayer.onGround));
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            stage = 10;
        }
        if (stage == 11 && mc.currentScreen == null) stage = 12;
        if (stage == 13 && ++useDelay == 20) {
            mc.thePlayer.rotationPitch = -90;
            mc.thePlayer.sendQueue.addToSendQueue(
                new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(
                    mc.thePlayer.rotationYaw,
                    -90,
                    mc.thePlayer.onGround));
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            stage = 14;
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            ticks++;
            if (ticks == 1) setup(player);
            if (ticks == 30)
                require(WirelessPowerTransfer.connect(receiver, player, sourceA), "connect ordinary LV receiver");
            if (ticks == 40) {
                require(receiver.getStoredEU() > 0, "mixin supplies real EU on the machine tick");
                require(
                    WirelessNetworkManager.getUserEU(sourceA)
                        .add(BigInteger.valueOf(receiver.getStoredEU()))
                        .equals(BigInteger.valueOf(100000)),
                    "debit exactly the received EU from the selected other player's account");
                require(
                    WirelessNetworkManager.getUserEU(player.getUniqueID())
                        .equals(BigInteger.valueOf(12345)),
                    "own account unchanged");
                WirelessPowerSavedData.get()
                    .toggle(id(receiver), player.getUniqueID());
                ((MetaTileEntity) receiver.getMetaTileEntity()).setEUVar(0);
            }
            if (ticks == 45) {
                require(receiver.getStoredEU() == 0, "paused device receives no EU");
                WirelessPowerSavedData.get()
                    .toggle(id(receiver), player.getUniqueID());
                WirelessNetworkManager.setUserEU(sourceA, BigInteger.ZERO);
            }
            if (ticks == 50) {
                require(receiver.getStoredEU() == 0, "empty account supplies no EU");
                WirelessNetworkManager.setUserEU(sourceA, BigInteger.valueOf(63));
            }
            if (ticks == 55) {
                require(
                    receiver.getStoredEU() == 32 && WirelessNetworkManager.getUserEU(sourceA)
                        .longValueExact() == 31,
                    "partial balance transfers one LV packet without overdraft");
                WirelessPowerCard.selectSource(card, player, sourceB);
                require(
                    WirelessPowerSavedData.get()
                        .connection(id(receiver)).source.equals(sourceA),
                    "source selection preserves existing bindings");
                automatic = placed(player, 2, false);
                canceled = placed(player, 4, true);
                setupMultiblock(player);
            }
            if (ticks == 65) {
                player.playerNetServerHandler.setPlayerLocation(20, 10, -3, 0, -90);
                require(
                    WirelessPowerTransfer.connect(energyA, player, sourceB)
                        && WirelessPowerTransfer.connect(energyB, player, sourceB),
                    "connect both industrial blast furnace inputs");
                player.playerNetServerHandler.setPlayerLocation(2, 10, -3, 0, -90);
                require(id(automatic) != null, "placement with carried card connects automatically");
                require(
                    WirelessPowerSavedData.get()
                        .connection(id(automatic)).source.equals(sourceB),
                    "auto-connect uses selected source");
                require(id(canceled) == null, "late placement cancellation prevents connection");
                NBTTagCompound nbt = new NBTTagCompound();
                automatic.writeToNBT(nbt);
                UUID connection = id(automatic);
                player.worldObj.removeTileEntity(2, 10, 0);
                automatic = new BaseMetaTileEntity();
                automatic.readFromNBT(nbt);
                player.worldObj.setTileEntity(2, 10, 0, automatic);
                require(connection.equals(id(automatic)), "tile NBT preserves connection identity");
                WirelessPowerSavedData.get()
                    .toggle(connection, player.getUniqueID());
            }
            if (ticks == 75) {
                require(
                    WirelessPowerSavedData.get()
                        .connection(id(automatic))
                        .loadedTile() == automatic,
                    "reloaded tile reconnects without scanning chunks");
                require(
                    WirelessPowerSavedData.get()
                        .connection(id(automatic))
                        .paused(),
                    "paused connection survives reload");
                ((MetaTileEntity) canceled.getMetaTileEntity()).setEUVar(0);
                WirelessPowerCard.toggleAutomatic(card, player);
                BaseMetaTileEntity disabled = placed(player, 5, false);
                require(id(disabled) == null, "disabled card does not connect");
                verifySpecialHatches(player);
                verifyRemoval(player);
                card = GTNRItemList.WirelessPowerCard.get(1);
                player.inventory.setInventorySlotContents(0, card);
                player.inventoryContainer.detectAndSendChanges();
                player.playerNetServerHandler.setPlayerLocation(2, 10, -3, 0, -90);
                stage = 5;
            }
            if (ticks == 105) {
                require(player.openContainer != player.inventoryContainer, "real air right-click keeps the GUI open");
                require(player.getHeldItem() != card, "vanilla right-click replaced the original stack with a copy");
            }
            if (stage == 4) {
                require(player.getHeldItem() != card, "source selection operates on the vanilla replacement stack");
                require(
                    requestedSource.equals(
                        WirelessPowerCard.source(player.getHeldItem(), player, false)
                            .toString()),
                    "avatar source selection reaches server");
                require(!WirelessPowerCard.automatic(player.getHeldItem()), "GUI toggle persists on the held card");
                stage = 6;
            }
            if (stage == 12) {
                require(
                    player.getDistanceSq(energyA.xCoord + 0.5, energyA.yCoord, energyA.zCoord + 0.5) < 20,
                    "teleport action lands beside the selected energy hatch");
                stage = 13;
            }
            if (stage == 15) {
                require(player.openContainer != player.inventoryContainer, "card remains open during removal test");
                player.worldObj.setBlockToAir(receiver.xCoord, receiver.yCoord, receiver.zCoord);
                player.worldObj.setBlockToAir(energyB.xCoord, energyB.yCoord, energyB.zCoord);
                stage = 16;
            }
            if (stage == 17) {
                player.worldObj.setBlockToAir(energyA.xCoord, energyA.yCoord, energyA.zCoord);
                stage = 18;
            }
            if (stage == 19) {
                finished = true;
                System.out.println("WIRELESS_POWER_QA: PASS");
                Minecraft.getMinecraft()
                    .func_152344_a(
                        () -> Minecraft.getMinecraft()
                            .shutdown());
            }
            if (ticks > 1200) throw new AssertionError("GUI QA timeout, stage " + stage);
        } catch (Throwable failure) {
            failure.printStackTrace();
            finished = true;
            System.out.println("WIRELESS_POWER_QA: FAIL");
            Minecraft.getMinecraft()
                .func_152344_a(
                    () -> Minecraft.getMinecraft()
                        .shutdown());
        }
    }

    private void setupMultiblock(EntityPlayerMP player) {
        furnace = placeAt(player, 20, 10, 0, registered(meta -> meta instanceof MTEElectricBlastFurnace));
        var controller = (MTEElectricBlastFurnace) furnace.getMetaTileEntity();
        controller.construct(new ItemStack(Blocks.stone), false);
        ItemStack energy = registered(meta -> meta instanceof MTEHatchEnergy hatch && hatch.mTier == 1);
        energyA = placeAt(player, 19, 10, 0, energy);
        energyB = placeAt(player, 21, 10, 0, energy);
        placeAt(player, 19, 10, 1, registered(meta -> meta instanceof MTEHatchMaintenance));
        placeAt(player, 21, 10, 1, registered(meta -> meta instanceof MTEHatchInputBus));
        placeAt(player, 21, 10, 2, registered(meta -> meta instanceof MTEHatchOutputBus));
        placeAt(player, 20, 13, 1, registered(meta -> meta instanceof MTEHatchMuffler));
        require(controller.checkStructure(true, furnace), "real industrial blast furnace structure forms");
        require(
            ((WirelessHatchOwner) energyA.getMetaTileEntity()).wirelessFrequency$getControllers()
                .contains(controller)
                && ((WirelessHatchOwner) energyB.getMetaTileEntity()).wirelessFrequency$getControllers()
                    .contains(controller),
            "structure check records both energy hatches under their controller");
        for (int x = 17; x <= 23; x++) for (int z = -3; z <= 3; z++) player.worldObj.setBlock(x, 9, z, Blocks.stone);
    }

    private void verifyRemoval(EntityPlayerMP player) {
        var saved = WirelessPowerSavedData.get();
        player.playerNetServerHandler.setPlayerLocation(30, 10, -3, 0, -90);
        BaseMetaTileEntity removed = place(player, 30, machine);
        saved.connect(removed, player.getUniqueID(), sourceB);
        UUID old = id(removed);
        player.worldObj.setBlockToAir(30, 10, 0);
        saved.connections(player.getUniqueID());
        require(saved.connection(old) == null, "destroyed machine disappears instead of becoming unloaded");
        BaseMetaTileEntity replaced = place(player, 30, machine);
        saved.connect(replaced, player.getUniqueID(), sourceB);
        old = id(replaced);
        player.worldObj.setBlockToAir(30, 10, 0);
        place(player, 30, machine);
        saved.connections(player.getUniqueID());
        require(saved.connection(old) == null, "same-type replacement cannot retain a removed machine's binding");
        NBTTagCompound stored = saved.connection(id(receiver))
            .write();
        stored.setInteger("x", 160000);
        stored.setInteger("z", 160000);
        net.minecraft.nbt.NBTTagList entries = new net.minecraft.nbt.NBTTagList();
        entries.appendTag(stored);
        NBTTagCompound storage = new NBTTagCompound();
        storage.setTag("connections", entries);
        WirelessPowerSavedData offline = new WirelessPowerSavedData();
        offline.readFromNBT(storage);
        require(
            !player.worldObj.getChunkProvider()
                .chunkExists(10000, 10000),
            "unloaded test chunk starts absent");
        require(
            offline.connections(player.getUniqueID())
                .size() == 1,
            "unloaded chunk preserves its connection");
        require(
            !player.worldObj.getChunkProvider()
                .chunkExists(10000, 10000),
            "listing never force-loads an absent chunk");
        player.playerNetServerHandler.setPlayerLocation(2, 10, -3, 0, -90);
    }

    private static ItemStack registered(Predicate<MetaTileEntity> predicate) {
        for (var meta : GregTechAPI.METATILEENTITIES) {
            if (meta instanceof MetaTileEntity candidate && predicate.test(candidate)) return candidate.getStackForm(1);
        }
        throw new AssertionError("Missing QA machine");
    }

    private void setup(EntityPlayerMP player) {
        player.playerNetServerHandler.setPlayerLocation(2, 10, -3, 0, 10);
        card = GTNRItemList.WirelessPowerCard.get(1);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, card);
        for (var candidate : GregTechAPI.METATILEENTITIES) {
            if (candidate instanceof MTEBasicMachine basic && basic.maxEUInput() == 32) {
                machine = basic.getStackForm(1);
                break;
            }
        }
        require(machine != null, "found registered LV machine");
        WirelessNetworkManager.strongCheckOrAddUser(player.getUniqueID());
        WirelessNetworkManager.setUserEU(player.getUniqueID(), BigInteger.valueOf(12345));
        for (int i = 0; i < 14; i++) {
            UUID id = new UUID(0, 0x101 + i);
            WirelessNetworkManager.strongCheckOrAddUser(id);
            WirelessNetworkManager.setUserEU(id, BigInteger.valueOf(100000));
            WirelessPowerSavedData.get()
                .remember(id, "Grid_" + (char) ('A' + i));
        }
        WirelessPowerCard.selectSource(card, player, sourceA);
        receiver = place(player, 0, machine);
    }

    private BaseMetaTileEntity placed(EntityPlayerMP player, int x, boolean cancel) {
        BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(player.worldObj, x, 10, 0);
        BaseMetaTileEntity tile = place(player, x, machine);
        BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(snapshot, Blocks.air, player);
        WirelessPowerEvents.INSTANCE.placed(event);
        if (cancel) event.setCanceled(true);
        return tile;
    }

    private void verifySpecialHatches(EntityPlayerMP player) {
        boolean laser = false, wireless = false;
        for (var candidate : GregTechAPI.METATILEENTITIES) {
            if (!laser && candidate instanceof MTEHatchEnergyTunnel) {
                BaseMetaTileEntity tile = place(player, 6, candidate.getStackForm(1));
                require(WirelessPowerTransfer.supports(tile), "laser energy input is supported");
                laser = true;
            }
            if (!wireless && candidate instanceof MTEHatchWirelessMulti) {
                BaseMetaTileEntity tile = place(player, 7, candidate.getStackForm(1));
                require(!WirelessPowerTransfer.supports(tile), "native wireless hatch is excluded");
                wireless = true;
            }
            if (laser && wireless) break;
        }
        require(laser && wireless, "checked registered special hatches");
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x, ItemStack stack) {
        return placeAt(player, x, 10, 0, stack);
    }

    private static BaseMetaTileEntity placeAt(EntityPlayerMP player, int x, int y, int z, ItemStack stack) {
        player.worldObj.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static UUID id(BaseMetaTileEntity tile) {
        return ((IWirelessPowerReceiver) tile).gtnr$powerConnection();
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws ReflectiveOperationException {
        if (event.phase != TickEvent.Phase.END || stage == 0 || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || !(wrapper.getScreen()
            .getMainPanel() instanceof WirelessPowerCardPanel panel)) return;
        var field = WirelessPowerCardPanel.class.getDeclaredField("session");
        field.setAccessible(true);
        Session session = (Session) field.get(panel);
        frames++;
        if (frames == 20 && stage == 1) session.request(WirelessPowerCardGui.SOURCES, "");
        if (frames == 60 && stage == 1) {
            require(
                session.display()
                    .getInteger("total") >= 15,
                "offline source list is paginated and synced");
            screenshot("wireless-power-sources.png");
            requestedSource = session.row(1)
                .getString("id");
            require(
                !requestedSource.equals(
                    session.display()
                        .getString("selected")),
                "select a different power source");
            session.request(WirelessPowerCardGui.SELECT, requestedSource);
            stage = 2;
        }
        if (frames == 100 && stage == 2) {
            require(
                session.display()
                    .getString("selected")
                    .equals(requestedSource),
                "selected avatar highlight synchronized");
            session.request(WirelessPowerCardGui.AUTO, "");
            stage = 3;
        }
        if (frames == 150 && stage == 3) {
            require(
                !session.display()
                    .getBoolean("sources")
                    && session.display()
                        .getInteger("total") == 3,
                "device list synchronized");
            screenshot("wireless-power-devices.png");
            stage = 4;
        }
        if (frames == 180 && stage == 6) {
            require(
                session.row(2)
                    .getBoolean("group")
                    && session.row(2)
                        .getInteger("members") == 2,
                "overview groups two energy hatches into one controller row");
            require(
                !session.row(0)
                    .getString("dimensionName")
                    .isEmpty(),
                "dimension name is synchronized");
            var copy = WirelessPowerCardPanel.class.getDeclaredMethod("copyPosition", int.class);
            copy.setAccessible(true);
            copy.invoke(panel, 0);
            require(
                GuiScreen.getClipboardString()
                    .equals("D0  0, 10, 0"),
                "copy button writes numeric dimension and coordinates");
            session.request(
                WirelessPowerCardGui.OPEN,
                session.row(2)
                    .getString("id"));
            stage = 7;
        }
        if (frames == 225 && stage == 7) {
            require(
                session.display()
                    .getBoolean("detail")
                    && session.display()
                        .getInteger("total") == 2,
                "controller details list its two energy inputs");
            require(
                wrapper.getScreen()
                    .getPanelManager()
                    .getOpenPanels()
                    .size() > 1,
                "controller detail opens in a popup");
            screenshot("wireless-power-controller.png");
            session.request(WirelessPowerCardGui.CLOSE, "");
            stage = 8;
        }
        if (frames == 265 && stage == 8) {
            require(
                !session.display()
                    .getBoolean("detail")
                    && session.display()
                        .getInteger("total") == 3,
                "closing details restores the overview");
            session.request(WirelessPowerCardGui.HIGHLIGHT, id(energyA).toString());
            stage = 9;
        }
        if (frames == 310 && stage == 10) {
            session.request(WirelessPowerCardGui.TELEPORT, id(energyA).toString());
            stage = 11;
        }
        if (stage == 14 && session.display()
            .getInteger("total") == 3) stage = 15;
        if (stage == 16 && session.display()
            .getInteger("total") == 2) {
            require(
                session.row(1)
                    .getBoolean("group")
                    && session.row(1)
                        .getInteger("members") == 1,
                "open GUI automatically removes the broken machine and updates the controller's hatch count");
            stage = 17;
        }
        if (stage == 18 && session.display()
            .getInteger("total") == 1) {
            require(
                !session.row(0)
                    .getBoolean("group"),
                "controller row disappears after its last connected input is removed");
            stage = 19;
        }
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("WIRELESS_POWER_QA: " + message);
    }
}
