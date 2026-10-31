package com.xyp.gtneverreset.qa;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.xyp.gtneverreset.api.packaged.PackagedTarget;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.OverloadedProviderInventory;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "overloadedproviderqa",
    name = "Overloaded provider QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class OverloadedProviderChecks {

    private boolean started, finished;
    private int ticks, frames;
    private volatile int stage;
    private TileEntityOverloadedProvider provider;

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
        mc.gameSettings.guiScale = 2;
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.gameSettings.language = "zh_CN";
            mc.getLanguageManager()
                .setCurrentLanguage(new net.minecraft.client.resources.Language("zh_CN", "CN", "Chinese", false));
            mc.refreshResources();
            mc.launchIntegratedServer(
                "overloaded-qa-" + System.currentTimeMillis(),
                "Overloaded QA",
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
            if (++ticks == 30) {
                verifyInventory();
                var world = player.worldObj;
                world.setBlock(
                    0,
                    10,
                    0,
                    net.minecraft.block.Block.getBlockFromItem(
                        GTNRItemList.OverloadedWirelessPatternProvider.get(1)
                            .getItem()));
                provider = (TileEntityOverloadedProvider) world.getTileEntity(0, 10, 0);
                provider.setOwnerName(player.getCommandSenderName());
                require(
                    appeng.api.AEApi.instance()
                        .registries()
                        .interfaceTerminal()
                        .getSupportedClasses()
                        .contains(TileEntityOverloadedProvider.class),
                    "overloaded provider is registered in AE interface terminal");
                new appeng.container.implementations.ContainerRenamer(player.inventory, provider)
                    .setNewName("QA Furnace Array");
                require("QA Furnace Array".equals(provider.getName()), "terminal exposes custom name");
                require(
                    provider.getPatterns()
                        .hasCustomInventoryName(),
                    "pattern inventory preserves literal name");
                verifyPatternControls(provider);
                for (int i = 0; i < 1; i++) {
                    world.setBlock(2 + i, 10, 0, Blocks.furnace);
                    require(
                        provider.bind(new PackagedTarget(world.provider.dimensionId, 2 + i, 10, 0, 1)),
                        "bind furnace " + i);
                }
                require(
                    !provider.bind(new PackagedTarget(world.provider.dimensionId, 2, 10, 0, 2)),
                    "duplicate rejected");
                NBTTagCompound tag = new NBTTagCompound();
                provider.writeToNBT(tag);
                TileEntityOverloadedProvider restored = new TileEntityOverloadedProvider();
                restored.readFromNBT(tag);
                require("QA Furnace Array".equals(restored.getName()), "custom name survives save and reload");
                restored.setCustomName("");
                require(
                    !restored.hasCustomName()
                        && "tile.overloaded_wireless_pattern_provider.name".equals(restored.getName()),
                    "clearing name restores default");
                require(
                    restored.connections()
                        .size() == 1,
                    "bindings persist");
                require(restored.getAutoReturn(), "return setting persists");
                verifyRecovery(player, tag);
                player.playerNetServerHandler.setPlayerLocation(.5, 11, -2, 0, 0);
            }
            if (ticks == 60) {
                TileEntityGuiFactory.INSTANCE.open(player, 0, 10, 0);
                stage = 1;
            }
            if (ticks > 900) throw new AssertionError("GUI timeout stage " + stage);
            if (stage == 3 && provider.connections()
                .size() == 0) {
                require(
                    provider.connections()
                        .isEmpty(),
                    "unlink acts on synchronized target identity");
                finish(null);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || stage == 0 || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        try {
            frames++;
            if (stage == 1 && frames > 60) {
                require(
                    "QA Furnace Array"
                        .equals(((TileEntityOverloadedProvider) mc.theWorld.getTileEntity(0, 10, 0)).getName()),
                    "custom name synchronizes to real client");
                screenshot("overloaded-patterns.png");
                Grid grid = (Grid) wrapper.getScreen()
                    .getMainPanel()
                    .getChildren()
                    .stream()
                    .filter(child -> child instanceof Grid)
                    .findFirst()
                    .orElseThrow();
                require(
                    grid.getChildren()
                        .size() == 450,
                    "GUI contains all 450 pattern slots");
                require(
                    grid.getScrollArea()
                        .getScrollY()
                        .isScrollBarActive(grid.getScrollArea()),
                    "pattern grid has an active vertical scrollbar");
                for (var child : wrapper.getScreen()
                    .getMainPanel()
                    .getChildren()) {
                    if (child instanceof ButtonWidget<?>button && "machines".equals(child.getName())) {
                        button.onMousePressed(0);
                        break;
                    }
                }
                stage = 2;
                frames = 0;
            } else if (stage == 2 && frames > 90) {
                require(
                    wrapper.getScreen()
                        .getPanelManager()
                        .getOpenPanels()
                        .size() > 1,
                    "machine panel opens");
                screenshot("overloaded-machines.png");
                for (var panel : wrapper.getScreen()
                    .getPanelManager()
                    .getOpenPanels()) {
                    for (var row : panel.getChildren()) {
                        if ("row_actions_1".equals(row.getName()))
                            require(!row.isEnabled(), "empty row actions stay hidden");
                        for (var child : row.getChildren()) {
                            if (child instanceof ButtonWidget<?>button && "unlink_0".equals(child.getName())) {
                                button.onMousePressed(0);
                                stage = 3;
                                return;
                            }
                        }
                    }
                }
                throw new AssertionError("unlink button missing");
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static void verifyRecovery(EntityPlayerMP player, NBTTagCompound saved) throws Exception {
        var targets = saved.getTagList("Targets", 10);
        NBTTagCompound first = targets.getCompoundTagAt(0);
        var pending = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound ore = new ItemStack(Blocks.iron_ore, 4).writeToNBT(new NBTTagCompound());
        ore.setInteger("Amount", 4);
        pending.appendTag(ore);
        first.setTag("Pending", pending);
        var outputs = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound ingot = new ItemStack(Items.iron_ingot).writeToNBT(new NBTTagCompound());
        ingot.setInteger("Amount", 1);
        outputs.appendTag(ingot);
        first.setTag("Outputs", outputs);
        TileEntityOverloadedProvider restored = new TileEntityOverloadedProvider();
        restored.readFromNBT(saved);
        restored.setWorldObj(player.worldObj);
        var target = (TileEntityFurnace) player.worldObj.getTileEntity(2, 10, 0);
        target.setInventorySlotContents(1, new ItemStack(Items.coal));
        target.setInventorySlotContents(2, new ItemStack(Items.iron_ingot, 2));
        require(
            restored.connections()
                .get(0)
                .pendingKinds() == 1,
            "pending delivery persists");
        require(
            !restored.unlink(
                restored.connections()
                    .get(0).target),
            "pending lane cannot unlink");
        var service = TileEntityOverloadedProvider.class
            .getDeclaredMethod("service", TileEntityOverloadedProvider.Lane.class);
        service.setAccessible(true);
        service.invoke(
            restored,
            restored.connections()
                .get(0));
        require(target.getStackInSlot(0).stackSize == 4, "restored delivery reaches furnace once");
        require(target.getStackInSlot(1).stackSize == 1, "return keeps fuel");
        require(target.getStackInSlot(2) == null, "return removes actual output");
        require(
            restored.getStackInSlot(TileEntityOverloadedProvider.PATTERNS).stackSize == 2,
            "offline ME keeps actual returned stack");
        service.invoke(
            restored,
            restored.connections()
                .get(0));
        require(target.getStackInSlot(0).stackSize == 4, "retry does not duplicate inputs");
        NBTTagCompound roundtrip = new NBTTagCompound();
        restored.writeToNBT(roundtrip);
        TileEntityOverloadedProvider again = new TileEntityOverloadedProvider();
        again.readFromNBT(roundtrip);
        require(
            again.connections()
                .get(0)
                .pendingKinds() == 0,
            "completed delivery stays completed after reload");
        require(
            again.getStackInSlot(TileEntityOverloadedProvider.PATTERNS).stackSize == 2,
            "return buffer survives reload");
        require(
            again.takeDrops()
                .stream()
                .mapToInt(stack -> stack.stackSize)
                .sum() == 2,
            "breaking returns exact buffered materials");
    }

    private static void verifyPatternControls(TileEntityOverloadedProvider provider) {
        ItemStack pattern = appeng.api.AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        var input = new net.minecraft.nbt.NBTTagList();
        input.appendTag(new ItemStack(Blocks.iron_ore, 2).writeToNBT(new NBTTagCompound()));
        var output = new net.minecraft.nbt.NBTTagList();
        output.appendTag(new ItemStack(Items.iron_ingot, 2).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", input);
        tag.setTag("out", output);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        provider.setInventorySlotContents(0, pattern);
        provider.multiplyPatterns(false, false);
        require(patternAmount(provider) == 4, "x2 doubles processing pattern");
        provider.multiplyPatterns(false, true);
        require(patternAmount(provider) == 2, "right-click halves processing pattern");
        provider.multiplyPatterns(true, false);
        require(patternAmount(provider) == 16, "shift x2 multiplies by eight");
        provider.multiplyPatterns(true, true);
        require(patternAmount(provider) == 2, "shift right-click divides by eight");
        provider.togglePatternOptimization();
        provider.toggleTerminalVisibility();
        NBTTagCompound saved = new NBTTagCompound();
        provider.writeToNBT(saved);
        TileEntityOverloadedProvider restored = new TileEntityOverloadedProvider();
        restored.readFromNBT(saved);
        require(
            !restored.allowsPatternOptimization() && !restored.shouldDisplay(),
            "optimization and terminal visibility persist");
        require(
            provider.getPatterns()
                .getSizeInventory() == 450
                && provider.getPatterns()
                    .getStackInSlot(450) == null,
            "AE terminal sees patterns only");
        provider.togglePatternOptimization();
        provider.toggleTerminalVisibility();
        provider.setInventorySlotContents(0, null);
        verifyPatternCapacity(provider, pattern);
    }

    private static void verifyPatternCapacity(TileEntityOverloadedProvider provider, ItemStack pattern) {
        require(provider.rows() == 2, "empty terminal starts with two rows");
        provider.getPatterns()
            .setInventorySlotContents(8, pattern.copy());
        require(provider.rows() == 3, "terminal expands after the first full row");
        provider.getPatterns()
            .setInventorySlotContents(8, null);
        provider.getPatterns()
            .setInventorySlotContents(449, pattern.copy());
        require(provider.rows() == 50, "terminal exposes the final pattern row");
        NBTTagCompound saved = new NBTTagCompound();
        provider.writeToNBT(saved);
        TileEntityOverloadedProvider restored = new TileEntityOverloadedProvider();
        restored.readFromNBT(saved);
        require(restored.getStackInSlot(449) != null && restored.rows() == 50, "last pattern slot survives reload");
        provider.getPatterns()
            .setInventorySlotContents(449, null);
        require(provider.rows() == 2, "terminal shrinks after removing the last pattern");

        NBTTagCompound legacy = new NBTTagCompound();
        provider.writeToNBT(legacy);
        legacy.removeTag("PatternSlots");
        var inventory = legacy.getTagList("Inventory", 10);
        NBTTagCompound oldPattern = pattern.writeToNBT(new NBTTagCompound());
        oldPattern.setInteger("Slot", 35);
        inventory.appendTag(oldPattern);
        for (int i = 0; i < 9; i++) {
            NBTTagCompound returned = new ItemStack(Items.iron_ingot, i + 1).writeToNBT(new NBTTagCompound());
            returned.setInteger("Slot", 36 + i);
            inventory.appendTag(returned);
        }
        restored.readFromNBT(legacy);
        require(restored.getStackInSlot(35) != null, "legacy patterns retain their slots");
        for (int i = 0; i < 9; i++) {
            require(
                restored.getStackInSlot(36 + i) == null && restored.getStackInSlot(450 + i).stackSize == i + 1,
                "legacy return slot migrates without loss: " + i);
        }
        NBTTagCompound migrated = new NBTTagCompound();
        restored.writeToNBT(migrated);
        restored.readFromNBT(migrated);
        require(restored.getStackInSlot(458).stackSize == 9, "migrated return slots remain stable after reload");
    }

    private static long patternAmount(TileEntityOverloadedProvider provider) {
        ItemStack stack = provider.getStackInSlot(0);
        return ((appeng.api.implementations.ICraftingPatternItem) stack.getItem())
            .getPatternForItem(stack, provider.getWorldObj())
            .getCondensedOutputs()[0].getStackSize();
    }

    private static void verifyInventory() {
        TileEntityFurnace furnace = new TileEntityFurnace();
        var ingredients = Arrays.asList(new ItemStack(Blocks.iron_ore, 4), new ItemStack(Items.coal));
        require(OverloadedProviderInventory.insert(furnace, 1, ingredients, true), "ore and fuel fit");
        require(furnace.getStackInSlot(0) == null && furnace.getStackInSlot(1) == null, "simulation unchanged");
        require(OverloadedProviderInventory.insert(furnace, 1, ingredients, false), "commit furnace inputs");
        require(
            furnace.getStackInSlot(0).stackSize == 4 && furnace.getStackInSlot(1)
                .getItem() == Items.coal,
            "correct furnace slots");
        require(
            !OverloadedProviderInventory.canExtract(furnace, 0, furnace.getStackInSlot(0)),
            "cannot extract raw ore");
        require(!OverloadedProviderInventory.canExtract(furnace, 1, furnace.getStackInSlot(1)), "cannot extract fuel");
        require(
            OverloadedProviderInventory.canExtract(furnace, 2, new ItemStack(Items.iron_ingot)),
            "extract furnace output");
        require(
            !OverloadedProviderInventory.insert(
                furnace,
                1,
                Arrays.asList(new ItemStack(Blocks.iron_ore, 4), new ItemStack(Items.coal, 64)),
                false),
            "full fuel rejects complete batch");
        require(furnace.getStackInSlot(0).stackSize == 4, "failed batch does not consume partial ore");
        TileEntityFurnace wood = new TileEntityFurnace();
        require(
            OverloadedProviderInventory.insert(wood, 1, Collections.singletonList(new ItemStack(Blocks.log)), false),
            "wood can smelt");
        require(wood.getStackInSlot(0) != null && wood.getStackInSlot(1) == null, "smeltable fuel goes to input");
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        finished = true;
        System.out.println("OVERLOADED_PROVIDER_QA: " + (failure == null ? "PASS" : "FAIL"));
        Minecraft.getMinecraft()
            .func_152344_a(
                () -> Minecraft.getMinecraft()
                    .shutdown());
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("OVERLOADED_PROVIDER_QA: " + message);
    }
}
