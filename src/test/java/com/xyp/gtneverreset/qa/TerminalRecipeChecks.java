package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;

@Mod(
    modid = "terminalrecipeqa",
    name = "Terminal Recipe Regression QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class TerminalRecipeChecks {

    private static CrossRecipeMEPatternBuffer provider;
    private boolean launched, checked, finished;
    private int ticks, frames, networkTicks;
    private volatile PacketInterfaceTerminalUpdate preview;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || checked || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            if (provider == null) {
                provider = createProvider(player);
                return;
            }
            if (!provider.getProxy()
                .isActive()) {
                if (++networkTicks > 200) throw new AssertionError("ME network did not become active");
                return;
            }
            checked = true;
            verifyEncoding(player);
            preview = verifyInterfaceDiscovery(player);
        } catch (Throwable error) {
            checked = true;
            failure = error;
        }
    }

    private static DualTerminalGuiObject terminal(EntityPlayer player) throws Exception {
        ItemStack stack = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        stack.setTagCompound(new NBTTagCompound());
        player.inventory.setInventorySlotContents(0, stack);
        var terminal = new DualTerminalGuiObject(
            (IWirelessTermHandler) stack.getItem(),
            stack,
            player,
            player.worldObj,
            0);
        terminal.setInventorySize(256, 256);
        terminal.readInventory();
        terminal.setCraftingRecipe(false);
        if (!player.worldObj.isRemote) {
            // Supply the real ME node without requiring wireless security pairing in this isolated fixture.
            var accessPoint = Proxy.newProxyInstance(
                IWirelessAccessPoint.class.getClassLoader(),
                new Class<?>[] { IWirelessAccessPoint.class },
                (proxy, method, args) -> provider.getProxy()
                    .getNode());
            field(WirelessTerminalGuiObject.class, "myWap").set(terminal, accessPoint);
        }
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(64)
                    .get());
        return terminal;
    }

    private static RecipeTransferPayload recipe(String map, boolean encode) {
        return new RecipeTransferPayload(
            false,
            encode,
            4,
            false,
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.iron_ingot)) },
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.stick)),
                AEItemStack.create(new ItemStack(Items.redstone)),
                AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 100)) },
            null,
            map);
    }

    private static void transfer(QuickEncodingTerminalContainer container, RecipeTransferPayload recipe)
        throws Exception {
        Method method = QuickEncodingTerminalContainer.class
            .getDeclaredMethod("applyRecipeTransfer", RecipeTransferPayload.class);
        method.setAccessible(true);
        method.invoke(container, recipe);
    }

    private static ItemStack encoded(DualTerminalGuiObject terminal) {
        return terminal.getInventoryByName("pattern")
            .getStackInSlot(1);
    }

    private static void encode(DualTerminalGuiObject terminal, EntityPlayer player, String map, String check) {
        require(terminal.encode(null, null, null, "QA", player.worldObj), check + " encodes");
        require(java.util.Objects.equals(map, PatternRecipeMap.name(encoded(terminal))), check + " recipe type");
    }

    private static void verifyEncoding(EntityPlayerMP player) throws Exception {
        var terminal = terminal(player);
        var container = new QuickEncodingTerminalContainer(player.inventory, terminal);
        String lathe = RecipeMaps.latheRecipes.unlocalizedName;
        transfer(container, recipe(lathe, true));
        require(lathe.equals(PatternRecipeMap.name(encoded(terminal))), "NEI automatic encoding writes recipe type");

        var outputs = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        outputs.putAEStackInSlot(1, null);
        encode(terminal, player, lathe, "remove item byproduct and overwrite");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, null);
        require(lathe.equals(terminal.getRecipeMapName()), "taking encoded pattern preserves draft");
        for (int slot = 0; slot < outputs.getSizeInventory(); slot++) {
            IAEStack<?> output = outputs.getAEStackInSlot(slot);
            if (output != null && output.isFluid()) outputs.putAEStackInSlot(slot, null);
        }
        encode(terminal, player, lathe, "remove fluid byproduct and encode again");
        require(
            CircuitPatternCodec.decode(encoded(terminal), player.worldObj)
                .getCondensedAEOutputs().length == 1,
            "removed byproducts stay absent from encoded pattern");
        ItemStack reducedPattern = encoded(terminal).copy();

        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, null);
        outputs.putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.stick, 4)));
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.iron_ingot, 4)));
        terminal.writeInventory();
        terminal.readInventory();
        container = new QuickEncodingTerminalContainer(player.inventory, terminal);
        outputs = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        encode(terminal, player, lathe, "quantity edits and terminal reload");

        outputs.putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.diamond)));
        encode(terminal, player, null, "unrelated output rejects stale recipe type");
        outputs.putAEStackInSlot(1, null);
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.gold_ingot)));
        encode(terminal, player, null, "changed input rejects stale recipe type");

        transfer(container, recipe(lathe, false));
        outputs.putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.stick)));
        require(terminal.getRecipeMapName() == null, "extra duplicate output is not a removed byproduct");
        transfer(container, recipe(lathe, false));
        outputs.putAEStackInSlot(0, null);
        outputs.putAEStackInSlot(1, null);
        outputs.putAEStackInSlot(2, null);
        require(terminal.getRecipeMapName() == null, "empty output has no recipe type");

        transfer(container, recipe(lathe, false));
        container.clear();
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.iron_ingot)));
        outputs.putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.stick)));
        require(terminal.getRecipeMapName() == null, "clear removes metadata even after rebuilding the same recipe");

        transfer(container, recipe(RecipeMaps.assemblerRecipes.unlocalizedName, true));
        require(
            RecipeMaps.assemblerRecipes.unlocalizedName.equals(PatternRecipeMap.name(encoded(terminal))),
            "next NEI transfer replaces recipe type");
        transfer(container, recipe(null, true));
        require(PatternRecipeMap.name(encoded(terminal)) == null, "untyped transfer clears recipe type");

        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, reducedPattern.copy());
        encode(terminal, player, lathe, "reopening annotated pattern restores recipe type");
        ItemStack untyped = reducedPattern.copy();
        PatternRecipeMap.write(untyped, null);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, untyped);
        encode(terminal, player, null, "reopening untyped pattern clears recipe type");
        container.onContainerClosed(player);
    }

    private static CrossRecipeMEPatternBuffer createProvider(EntityPlayerMP player) throws Exception {
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.xCoord = (int) player.posX;
        tile.yCoord = 100;
        tile.zCoord = (int) player.posZ;
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CrossRecipeMEPatternBuffer.id);
        var hatch = (CrossRecipeMEPatternBuffer) tile.getMetaTileEntity();
        require(!hatch.hasCustomName(), "cross-recipe hatch uses its default name");
        if (!hatch.shouldDisplay()) hatch.toggleTerminalVisibility();
        hatch.getProxy()
            .onReady();
        var power = new TileCreativeEnergyCell();
        power.setWorldObj(player.worldObj);
        power.xCoord = tile.xCoord + 1;
        power.yCoord = tile.yCoord;
        power.zCoord = tile.zCoord;
        power.getProxy()
            .onReady();
        AEApi.instance()
            .createGridConnection(
                hatch.getProxy()
                    .getNode(),
                power.getProxy()
                    .getNode());
        return hatch;
    }

    private static PacketInterfaceTerminalUpdate verifyInterfaceDiscovery(EntityPlayerMP player) throws Exception {
        var hatch = provider;
        var host = terminal(player);
        var container = new ContainerInterfaceTerminal(player.inventory, host);
        Method update = ContainerInterfaceTerminal.class.getDeclaredMethod("updateList");
        update.setAccessible(true);
        var packet = (PacketInterfaceTerminalUpdate) field(ContainerInterfaceTerminal.class, "dirty").get(container);
        Map<?, ?> tracked = (Map<?, ?>) field(ContainerInterfaceTerminal.class, "tracked").get(container);
        require(tracked.containsKey(hatch), "native interface terminal discovers cross-recipe hatch");
        Object entry = tracked.get(hatch);
        require(field(entry.getClass(), "online").getBoolean(entry), "discovered hatch is online");
        require(field(entry.getClass(), "shouldDisplay").getBoolean(entry), "display toggle is honored");
        hatch.toggleTerminalVisibility();
        update.invoke(container);
        require(!field(entry.getClass(), "shouldDisplay").getBoolean(entry), "hide toggle is honored");
        hatch.toggleTerminalVisibility();
        return packet;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("[terminal-recipe-qa] PASS " + message);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "terminal-" + System.currentTimeMillis(),
                    "Terminal Recipe QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("QA timeout");
            if (preview == null) return;
            if (!(mc.currentScreen instanceof GuiInterfaceTerminal)) {
                var gui = new GuiInterfaceTerminal(mc.thePlayer.inventory, terminal(mc.thePlayer));
                mc.displayGuiScreen(gui);
                @SuppressWarnings("unchecked")
                var entries = (List<PacketInterfaceTerminalUpdate.PacketEntry>) field(
                    PacketInterfaceTerminalUpdate.class,
                    "commands").get(preview);
                gui.postUpdate(entries, 0);
            }
            if (++frames < 16) return;
            Object masterList = field(GuiInterfaceTerminal.class, "masterList").get(mc.currentScreen);
            Map<?, ?> displayed = (Map<?, ?>) field(masterList.getClass(), "list").get(masterList);
            require(displayed.size() == 1, "client receives the cross-recipe hatch entry");
            Object entry = displayed.values()
                .iterator()
                .next();
            String name = (String) field(entry.getClass(), "dispName").get(entry);
            require(name.contains("跨配方类型"), "client displays default Chinese cross-recipe name: " + name);
            List<?> sections = (List<?>) field(masterList.getClass(), "visibleSections").get(masterList);
            require(!sections.isEmpty(), "cross-recipe interface name is visible in the terminal");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "terminal-cross-recipe-default-name.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void finish(Throwable error) {
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
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "terminal-recipe-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
