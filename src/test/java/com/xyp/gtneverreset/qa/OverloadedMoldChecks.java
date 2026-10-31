package com.xyp.gtneverreset.qa;

import java.io.File;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mod(modid = "overloadedmoldqa", name = "Overloaded GT crafting QA", version = "1", dependencies = "after:gtneverreset")
public final class OverloadedMoldChecks {

    private boolean started, finished, sawProcessing;
    private int ticks, stage, guiFrames;
    private volatile int screenshot;
    private TileEntityOverloadedProvider provider;
    private BaseMetaTileEntity furnace;
    private Future<ICraftingJob> job;
    private ICraftingLink link;

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
        mc.gameSettings.pauseOnLostFocus = false;
        mc.ingameGUI.getChatGUI()
            .clearChatMessages();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "overloaded-gt-" + System.currentTimeMillis(),
                "Overloaded GT crafting QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            World world = player.worldObj;
            if (++ticks == 30) setup(world, player);
            if (furnace == null) return;
            // Test power supply only: machine recipe execution and inventories remain untouched.
            furnace.setStoredEU(furnace.getEUCapacity());
            if (furnace.isActive() && !sawProcessing) {
                sawProcessing = true;
                log("GT Mold is processing inputs delivered by the provider");
                screenshot = 1;
            }
            if (ticks < 120) return;
            if (ticks > 1800) throw new AssertionError(
                "timeout stage=" + stage
                    + " active="
                    + provider.getProxy()
                        .isActive()
                    + " products="
                    + stored(result(1))
                    + " cobble="
                    + stored(new ItemStack(net.minecraft.init.Items.iron_ingot)));
            if (stage == 0) {
                verifyOversized(world);
                require(
                    provider.getProxy()
                        .isActive(),
                    "provider has powered AE channel");
                require(
                    !provider.getProxy()
                        .getCrafting()
                        .getCpus()
                        .isEmpty(),
                    "real crafting CPU is online");
                require(
                    provider.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .injectItems(
                            AEItemStack.create(new ItemStack(net.minecraft.init.Items.iron_ingot, 4)),
                            Actionable.MODULATE,
                            new MachineSource(provider))
                        == null,
                    "4 iron ingots stored in ME disk");
                require(stored(result(1)) == 0, "no products exist before order");
                job = provider.getProxy()
                    .getCrafting()
                    .beginCraftingJob(
                        world,
                        provider.getProxy()
                            .getGrid(),
                        new MachineSource(provider),
                        AEItemStack.create(result(8)),
                        null);
                stage = 1;
            } else if (stage == 1 && job.isDone()) {
                require(
                    !job.get()
                        .isSimulation(),
                    "AE calculated processing job with available ingredients");
                link = provider.getProxy()
                    .getCrafting()
                    .submitJob(job.get(), null, null, false, new MachineSource(provider));
                require(link != null, "AE CPU accepted order for 8 products");
                stage = 2;
                require(
                    provider.getStackInSlot(0)
                        .getTagCompound()
                        .getTagList("in", 10)
                        .tagCount() == 2,
                    "encoded catalyst remains in original pattern");
            } else if (stage == 2 && stored(result(1)) == 8
                && provider.getProxy()
                    .getCrafting()
                    .getCpus()
                    .stream()
                    .noneMatch(cpu -> cpu.isBusy())) {
                        require(!link.isCanceled(), "AE CPU finished the standalone order without cancellation");
                        require(sawProcessing, "GT machine actually ran");
                        require(
                            stored(result(1)) == 8 && stored(new ItemStack(net.minecraft.init.Items.iron_ingot)) == 0,
                            "all products returned to ME and all inputs consumed");
                        require(
                            ((com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) furnace
                                .getMetaTileEntity()).gtnr$getVirtualMold() == null,
                            "temporary virtual mold released after processing");
                        for (int slot = 0; slot < furnace.getSizeInventory(); slot++) require(
                            furnace.getStackInSlot(slot) == null,
                            "machine slot " + slot + " empty after recovery");
                        for (int slot = TileEntityOverloadedProvider.PATTERNS; slot
                            < provider.getSizeInventory(); slot++)
                            require(provider.getStackInSlot(slot) == null, "return buffer " + slot + " empty");
                        stage = 3;
                        screenshot = 2;
                    } else
                if (stage == 3 && screenshot == 0) {
                    player.playerNetServerHandler.setPlayerLocation(6, 11, -2, 0, 0);
                    furnace.getMetaTileEntity()
                        .onRightclick(furnace, player, ForgeDirection.NORTH, .5f, .5f, .5f);
                    stage = 4;
                } else if (stage == 5) {
                    var configuration = (com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration) furnace
                        .getMetaTileEntity();
                    if (configuration.gtnr$getVirtualMold() != null) {
                        require(configuration.gtnr$getVirtualMold().stackSize == 0, "GUI selected mold is virtual");
                        for (int slot = 0; slot < furnace.getSizeInventory(); slot++)
                            require(furnace.getStackInSlot(slot) == null, "GUI selection creates no extractable item");
                        NBTTagCompound saved = new NBTTagCompound();
                        furnace.getMetaTileEntity()
                            .saveNBTData(saved);
                        configuration.gtnr$setVirtualMold(null);
                        furnace.getMetaTileEntity()
                            .loadNBTData(saved);
                        require(
                            configuration.gtnr$getVirtualMold() != null,
                            "manual mold selection survives save and reload");
                        stage = 6;
                    }
                }
            if (ticks % 100 == 0)
                log("stage=" + stage + " active=" + furnace.isActive() + " products=" + stored(result(1)));
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void setup(World world, EntityPlayerMP player) {
        var defs = AEApi.instance()
            .definitions();
        world.setWorldTime(6000);
        for (int x = 0; x < 4; x++) {
            world.setBlock(
                x,
                10,
                1,
                defs.blocks()
                    .multiPart()
                    .maybeBlock()
                    .get());
            ((IPartHost) world.getTileEntity(x, 10, 1)).addPart(
                defs.parts()
                    .cableGlass()
                    .stack(AEColor.Transparent, 1),
                ForgeDirection.UNKNOWN,
                player);
        }
        world.setBlock(
            0,
            10,
            0,
            net.minecraft.block.Block.getBlockFromItem(
                GTNRItemList.OverloadedWirelessPatternProvider.get(1)
                    .getItem()));
        provider = (TileEntityOverloadedProvider) world.getTileEntity(0, 10, 0);
        provider.setOwnerName(player.getCommandSenderName());
        world.setBlock(
            1,
            10,
            0,
            defs.blocks()
                .drive()
                .maybeBlock()
                .get());
        ((TileDrive) world.getTileEntity(1, 10, 0)).getInternalInventory()
            .setInventorySlotContents(
                0,
                defs.items()
                    .cell1k()
                    .maybeStack(1)
                    .get());
        world.setBlock(
            2,
            10,
            0,
            defs.blocks()
                .craftingStorage1k()
                .maybeBlock()
                .get());
        world.setBlock(
            3,
            10,
            0,
            defs.blocks()
                .energyCellCreative()
                .maybeBlock()
                .get());
        world.setBlock(6, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        furnace = (BaseMetaTileEntity) world.getTileEntity(6, 10, 0);
        furnace.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_HV_Extruder.get(1)
                .getItemDamage());
        furnace.setFrontFacing(ForgeDirection.NORTH);
        furnace.setOwnerName(player.getCommandSenderName());
        furnace.enableWorking();
        require(
            !((Object) furnace instanceof appeng.api.implementations.tiles.ICraftingMachine),
            "ordinary ME interface target has no global special-input adapter");
        require(
            provider.bind(new PackagedTarget(world.provider.dimensionId, 6, 10, 0, ForgeDirection.UP.ordinal())),
            "wireless target is GT HV Mold top face");
        ItemStack pattern = defs.items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList input = new NBTTagList(), output = new NBTTagList();
        input.appendTag(new ItemStack(net.minecraft.init.Items.iron_ingot, 4).writeToNBT(new NBTTagCompound()));
        input.appendTag(
            ItemList.Shape_Extruder_Rod.get(1)
                .writeToNBT(new NBTTagCompound()));
        output.appendTag(result(8).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", input);
        tag.setTag("out", output);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        provider.setInventorySlotContents(0, pattern);
        player.capabilities.isFlying = true;
        player.inventory.setInventorySlotContents(0, null);
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(3, 13, -6, 0, 25);
    }

    private static ItemStack result(int amount) {
        return gregtech.api.util.GTOreDictUnificator
            .get(gregtech.api.enums.OrePrefixes.stick, gregtech.api.enums.Materials.Iron, amount);
    }

    private void verifyOversized(World world) {
        world.setBlock(9, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity bender = (BaseMetaTileEntity) world.getTileEntity(9, 10, 0);
        bender.setInitialValuesAsNBT(
            null,
            (short) ItemList.Machine_HV_Bender.get(1)
                .getItemDamage());
        bender.setFrontFacing(ForgeDirection.NORTH);
        bender.enableWorking();
        var machine = (gregtech.api.metatileentity.implementations.MTEBasicMachine) bender.getMetaTileEntity();
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        in.appendTag(new ItemStack(net.minecraft.init.Items.iron_ingot, 65).writeToNBT(new NBTTagCompound()));
        in.appendTag(
            gregtech.api.util.GTUtility.getIntegratedCircuit(1)
                .writeToNBT(new NBTTagCompound()));
        ItemStack plates = gregtech.api.util.GTOreDictUnificator
            .get(gregtech.api.enums.OrePrefixes.plate, gregtech.api.enums.Materials.Iron, 1);
        plates.stackSize = 65;
        out.appendTag(plates.writeToNBT(new NBTTagCompound()));
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        var details = com.xyp.gtneverreset.common.me.CircuitPatternCodec.runtime(encoded, world);
        var table = new appeng.util.inv.MEInventoryCrafting(new appeng.container.ContainerNull(), 1, 1);
        var buffer = new com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineBuffer();
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(net.minecraft.init.Items.iron_ingot, 64)));
        require(
            !com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineInput
                .push(bender, details, table, ForgeDirection.UP, buffer),
            "wrong delivered quantity rejected atomically");
        require(
            buffer.isEmpty() && java.util.Arrays.stream(machine.mInventory)
                .allMatch(java.util.Objects::isNull),
            "rejected delivery changes neither inventory nor configuration");
        table.setInventorySlotContents(0, AEItemStack.create(new ItemStack(net.minecraft.init.Items.iron_ingot, 65)));
        require(
            com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineInput
                .push(bender, details, table, ForgeDirection.UP, buffer),
            "oversized batch accepted outside AE planning limit");
        require(
            machine.getStackInSlot(machine.getInputSlot()).stackSize == 64 && buffer.getItems()
                .get(0).stackSize == 1,
            "64 inputs inserted and one retained in overflow");
        NBTTagCompound saved = new NBTTagCompound();
        buffer.writeToNBT(saved);
        var restored = new com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineBuffer();
        restored.readFromNBT(saved);
        require(
            restored.getItems()
                .get(0).stackSize == 1,
            "overflow survives serialization");
        machine.getStackInSlot(machine.getInputSlot()).stackSize--;
        com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineInput.refill(machine, restored);
        require(
            restored.isEmpty() && machine.getStackInSlot(machine.getInputSlot()).stackSize == 64,
            "overflow refills exactly once when capacity is free");
        com.xyp.gtneverreset.common.block.blocks.tile.OverloadedMachineInput.refill(machine, restored);
        require(
            machine.getStackInSlot(machine.getInputSlot()).stackSize == 64,
            "repeated refill does not duplicate items");
    }

    private long stored(ItemStack block) throws Exception {
        var stack = provider.getProxy()
            .getStorage()
            .getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(block));
        return stack == null ? 0 : stack.getStackSize();
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (stage >= 4 && Minecraft
            .getMinecraft().currentScreen instanceof com.cleanroommc.modularui.screen.GuiContainerWrapper wrapper) {
            if (++guiFrames < 60) return;
            if (stage == 4) {
                var widget = findMold(
                    wrapper.getScreen()
                        .getMainPanel());
                require(widget != null, "native GT machine GUI contains virtual mold slot");
                ((gregtech.common.modularui2.widget.SlotLikeButtonWidget) widget).onMousePressed(0);
                stage = 5;
                guiFrames = 0;
            } else if (stage == 6) {
                Minecraft mc = Minecraft.getMinecraft();
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "singleblock-virtual-mold.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                finish(null);
            }
        }
        if (screenshot == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            new File("."),
            "overloaded-gt-" + screenshot + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        screenshot = 0;
    }

    private static com.cleanroommc.modularui.api.widget.IWidget findMold(
        com.cleanroommc.modularui.api.widget.IWidget widget) {
        if ("virtual_mold".equals(widget.getName())) return widget;
        for (var child : widget.getChildren()) {
            var found = findMold(child);
            if (found != null) return found;
        }
        return null;
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        finished = true;
        log(failure == null ? "PASS" : "FAIL");
        Minecraft.getMinecraft()
            .func_152344_a(
                () -> Minecraft.getMinecraft()
                    .shutdown());
    }

    private static void log(String message) {
        System.out.println("OVERLOADED_MOLD_QA: " + message);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        log(message);
    }
}
