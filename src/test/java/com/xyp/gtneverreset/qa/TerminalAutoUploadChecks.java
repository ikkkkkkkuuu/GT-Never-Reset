package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.helpers.InventoryAction;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

@Mod(
    modid = "terminalautouploadqa",
    name = "Terminal Auto Upload QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class TerminalAutoUploadChecks {

    private static final int WINDOW_ID = 71;
    private static final String OLD_SAMPLE = "qaOriginalSample";
    private static final String REPORT_PREFIX = "[terminal-auto-upload-qa] ";

    private volatile Phase phase = Phase.PREPARE;
    private volatile Throwable failure;
    private TileInterface provider;
    private DualTerminalGuiObject serverTerminal;
    private QuickEncodingTerminalContainer serverContainer;
    private QuickEncodingTerminalGui clientGui;
    private ItemStack oldPattern;
    private ItemStack storedProcessingPattern;
    private ItemStack retainedDuplicatePattern;
    private long targetId;
    private int networkTicks, clientTicks;
    private boolean launched, finished;
    private boolean receiptKeepNonConsumables;
    private List<String> screenshotLines;
    private String screenshotName;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failure != null || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            switch (phase) {
                case PREPARE -> prepare(player);
                case ATTACH_SERVER -> {
                    player.openContainer = serverContainer;
                    serverContainer.addCraftingToCrafters(player);
                    serverContainer.detectAndSendChanges();
                    phase = Phase.VALID_READY;
                }
                case VALID_CLOSED -> acknowledgeClose(false, player);
                case VALID_SENT -> verifyStored(player);
                case VALID_CAPTURED -> {
                    seedOldPattern(player, "failed-control");
                    serverContainer.detectAndSendChanges();
                    phase = Phase.INVALID_READY;
                }
                case INVALID_CLOSED -> acknowledgeClose(true, player);
                case INVALID_VERIFY -> verifyFailedPlacement(player);
                case PROCESSING_SEED_SENT -> seedProcessingPattern(player);
                case DUPLICATE_VERIFY -> verifyDuplicatePlacement(player);
                case SCALED_SENT -> verifyScaledPlacement(player);
                default -> {}
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
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getName()
                    .equals("galaxyspace.core.gui.GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "terminal-auto-upload-" + System.currentTimeMillis(),
                    "Terminal Auto Upload QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server fixture failed", failure);
            if (mc.thePlayer == null) {
                require(clientTicks == 0, "client disconnected at " + phase);
                return;
            }
            require(++clientTicks < 1800, "timeout at " + phase);
            switch (phase) {
                case OPEN_CLIENT -> {
                    ClientConfig.wirelessTerminal.fillSearchFromNeiRecipes = true;
                    clientGui = new QuickEncodingTerminalGui(mc.thePlayer.inventory, terminal(mc.thePlayer));
                    mc.displayGuiScreen(clientGui);
                    clientGui.inventorySlots.windowId = WINDOW_ID;
                    mc.thePlayer.openContainer = clientGui.inventorySlots;
                    phase = Phase.ATTACH_SERVER;
                }
                case VALID_READY -> closeSubstitution(false);
                case VALID_OFF -> submitOverlay(false);
                case VALID_STORED -> collectScreenshot(mc, false);
                case INVALID_READY -> closeSubstitution(true);
                case INVALID_OFF -> submitOverlay(true);
                case INVALID_SENT -> awaitInvalidUploadAttempt();
                case INVALID_RETAINED -> collectScreenshot(mc, true);
                case PROCESSING_READY -> submitProcessingOverlay(Phase.PROCESSING_SEED_SENT, 1);
                case PROCESSING_SEEDED -> submitDuplicateWhenSynchronized();
                case DUPLICATE_SENT -> awaitDuplicateUploadAttempt();
                case DUPLICATE_RETAINED -> captureRetainedDuplicate(mc);
                case SCALED_READY -> submitProcessingOverlay(Phase.SCALED_SENT, 2);
                case DONE -> finish(null);
                default -> {}
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void screenshot(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (screenshotLines == null || event.gui != clientGui || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            Method draw = GuiScreen.class
                .getDeclaredMethod("drawHoveringText", List.class, int.class, int.class, FontRenderer.class);
            draw.setAccessible(true);
            draw.invoke(clientGui, screenshotLines, 10, 36, mc.fontRenderer);
            ScreenShotHelper
                .saveScreenshot(new File("."), screenshotName, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            require(Files.exists(Path.of("screenshots", screenshotName)), "screenshot " + screenshotName);
            screenshotLines = null;
            phase = switch (phase) {
                case VALID_STORED -> Phase.VALID_CAPTURED;
                case INVALID_RETAINED -> Phase.PROCESSING_READY;
                case DUPLICATE_RETAINED -> Phase.SCALED_READY;
                default -> throw new AssertionError("unexpected screenshot at " + phase);
            };
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void prepare(EntityPlayerMP player) throws Exception {
        if (provider == null) {
            provider = new TileInterface();
            provider.setWorldObj(player.worldObj);
            provider.xCoord = (int) player.posX;
            provider.yCoord = 100;
            provider.zCoord = (int) player.posZ;
            provider.setCustomName(assemblerName());
            provider.getProxy()
                .onReady();
            var power = new TileCreativeEnergyCell();
            power.setWorldObj(player.worldObj);
            power.xCoord = provider.xCoord + 1;
            power.yCoord = provider.yCoord;
            power.zCoord = provider.zCoord;
            power.getProxy()
                .onReady();
            AEApi.instance()
                .createGridConnection(
                    provider.getProxy()
                        .getNode(),
                    power.getProxy()
                        .getNode());
            return;
        }
        if (!provider.getProxy()
            .isActive()) {
            require(++networkTicks < 200, "ME network did not become active");
            return;
        }
        serverTerminal = terminal(player);
        var grid = provider.getProxy()
            .getNode()
            .getGrid();
        field(WirelessTerminalGuiObject.class, "targetGrid").set(serverTerminal, grid);
        field(WirelessTerminalGuiObject.class, "sg").set(serverTerminal, grid.getCache(IStorageGrid.class));
        field(WirelessTerminalGuiObject.class, "infinityRange").setBoolean(serverTerminal, true);
        var accessPoint = Proxy.newProxyInstance(
            IWirelessAccessPoint.class.getClassLoader(),
            new Class<?>[] { IWirelessAccessPoint.class },
            (proxy, method, args) -> method.getName()
                .equals("getGrid") ? grid
                    : provider.getProxy()
                        .getNode());
        field(WirelessTerminalGuiObject.class, "myWap").set(serverTerminal, accessPoint);
        seedOldPattern(player, "success-control");
        serverContainer = new QuickEncodingTerminalContainer(player.inventory, serverTerminal);
        serverContainer.windowId = WINDOW_ID;
        serverContainer.setPowerSource(
            provider.getProxy()
                .getEnergy());
        Object delegate = field(QuickEncodingTerminalContainer.class, "interfaceDelegate").get(serverContainer);
        Map<?, ?> tracked = (Map<?, ?>) field(ContainerInterfaceTerminal.class, "tracked").get(delegate);
        Object trackedProvider = tracked.get(provider);
        require(trackedProvider != null, "real interface terminal finds powered AE2 interface");
        targetId = field(trackedProvider.getClass(), "id").getLong(trackedProvider);
        report("fixture interface=" + provider.getRawName() + ", entry=" + targetId);
        phase = Phase.OPEN_CLIENT;
    }

    private void seedOldPattern(EntityPlayerMP player, String marker) throws Exception {
        require(encoded(serverTerminal) == null, "encoder output empty before seeding");
        serverTerminal.setCraftingRecipe(true);
        serverTerminal.setSubstitution(true);
        serverTerminal.setCanBeSubstitution(false);
        var inputs = serverTerminal.getAEInventoryByName(StorageName.CRAFTING_INPUT);
        for (int i = 0; i < inputs.getSizeInventory(); i++) {
            inputs.putAEStackInSlot(i, i == 0 ? AEItemStack.create(new ItemStack(Blocks.log, 1, 0)) : null);
        }
        Field size = field(inputs.getClass(), "size");
        int previousSize = size.getInt(inputs);
        try {
            size.setInt(inputs, 9);
            require(serverTerminal.encode(null, null, null, "QA old sample", player.worldObj), "old recipe encodes");
        } finally {
            size.setInt(inputs, previousSize);
        }
        ItemStack encoded = encoded(serverTerminal);
        require(tag(encoded).getBoolean("substitute"), "old sample has substitution enabled");
        tag(encoded).setString(OLD_SAMPLE, marker);
        oldPattern = encoded.copy();
        report("old sample " + marker + ": " + describe(oldPattern, player));
    }

    private void closeSubstitution(boolean invalid) throws Exception {
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        if (!container.substituteSync.get()
            || !samePattern(encoded((DualTerminalGuiObject) container.getTarget()), oldPattern)
            || clientInterfaceInventory() == null) return;
        GuiButton button = (GuiButton) field(GuiPatternTerm.class, "substitutionsEnabledBtn").get(clientGui);
        Method click = QuickEncodingTerminalGui.class.getDeclaredMethod("actionPerformed", GuiButton.class);
        click.setAccessible(true);
        click.invoke(clientGui, button);
        require(!container.substituteSync.get(), "client mineral substitution button switched off");
        phase = invalid ? Phase.INVALID_CLOSED : Phase.VALID_CLOSED;
    }

    private void acknowledgeClose(boolean invalid, EntityPlayerMP player) throws Exception {
        if (serverTerminal.isSubstitution()) return;
        require(samePattern(encoded(serverTerminal), oldPattern), "server output still holds exact enabled old sample");
        require(
            provider.getPatterns()
                .getStackInSlot(invalid ? 1 : 0) == null,
            "destination slot starts empty");
        if (invalid) {
            receiptKeepNonConsumables = !serverTerminal.shouldKeepNonConsumables();
            var matrix = new net.minecraft.inventory.InventoryCrafting(serverContainer, 3, 3);
            matrix.setInventorySlotContents(0, new ItemStack(Blocks.bedrock));
            require(
                Platform.findMatchingRecipeOutput(matrix, player.worldObj) == null,
                "failure input has no native recipe");
        }
        report(
            (invalid ? "failure" : "success") + " before NEI transfer: server substitute=false; old output retained");
        phase = invalid ? Phase.INVALID_OFF : Phase.VALID_OFF;
    }

    private void submitOverlay(boolean invalid) throws Exception {
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        require(!container.substituteSync.get(), "client still shows substitution disabled before transfer");
        ItemStack input = new ItemStack(invalid ? Blocks.bedrock : Blocks.planks);
        List<PositionedStack> inputs = invalid ? List.of(new PositionedStack(input, 25, 6, false))
            : List.of(new PositionedStack(input, 25, 6, false), new PositionedStack(input.copy(), 25, 24, false));
        var result = new PositionedStack(
            invalid ? new ItemStack(Items.diamond) : new ItemStack(Items.stick, 4),
            119,
            24);
        var recipe = (IRecipeHandler) Proxy.newProxyInstance(
            IRecipeHandler.class.getClassLoader(),
            new Class<?>[] { IRecipeHandler.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getOverlayIdentifier" -> "crafting";
            case "getRecipeName" -> invalid ? "QA invalid crafting grid" : "QA sticks";
            case "getIngredientStacks" -> inputs;
            case "getResultStack" -> result;
            case "getOtherStacks" -> List.of();
            default -> throw new UnsupportedOperationException(method.getName());
            });
        phase = invalid ? Phase.INVALID_SENT : Phase.VALID_SENT;
        QuickTerminalRecipeTransferHandler.INSTANCE.overlayRecipe(clientGui, recipe, 0, true);
        if (invalid) {
            require(
                field(QuickEncodingTerminalGui.class, "pendingAutoPlace").getBoolean(clientGui),
                "NEI invalid case still requests automatic placement");
        }
    }

    private void awaitInvalidUploadAttempt() throws Exception {
        if (field(QuickEncodingTerminalGui.class, "pendingAutoPlace").getBoolean(clientGui)) return;
        Object embedded = field(QuickEncodingTerminalGui.class, "interfaceTerminal").get(clientGui);
        var target = (InterfacePatternTarget) field(embedded.getClass(), "highlightedTarget").get(embedded);
        require(
            target != null && target.getEntryId() == targetId && target.getSlot() == 1,
            "automatic placement found the exact empty interface slot before sending");
        report("failure client highlighted matching interface entry=" + targetId + ", slot=1, and sent placement");
        phase = Phase.INVALID_VERIFY;
        // This unrelated request uses the same ordered C2S transport after the actual placement request.
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        container.requestKeepNonConsumables(receiptKeepNonConsumables);
    }

    private void verifyStored(EntityPlayerMP player) throws Exception {
        ItemStack stored = provider.getPatterns()
            .getStackInSlot(0);
        if (stored == null) return;
        require(!serverTerminal.isSubstitution(), "server substitution remains off after upload");
        require(encoded(serverTerminal) == null, "real upload consumes the encoder output slot");
        require(
            output(stored, player).isItemEqual(new ItemStack(Items.stick)),
            "successful upload contains new sticks");
        require(!tag(stored).hasKey(OLD_SAMPLE), "successful upload lacks the old sample marker");
        require(!tag(stored).getBoolean("substitute"), "new uploaded pattern has substitution disabled");
        report("success server interface result: " + describe(stored, player));
        phase = Phase.VALID_STORED;
    }

    private void verifyFailedPlacement(EntityPlayerMP player) throws Exception {
        if (serverTerminal.shouldKeepNonConsumables() != receiptKeepNonConsumables) return;
        var input = serverTerminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .getAEStackInSlot(0);
        require(
            input != null && input.getItemStackForNEI()
                .isItemEqual(new ItemStack(Blocks.bedrock)),
            "real C2S failure overlay established invalid crafting grid");
        require(!serverTerminal.isSubstitution(), "server substitution remains off after rejected placement");
        require(
            provider.getPatterns()
                .getStackInSlot(1) == null,
            "failed encoding leaves target interface slot empty");
        ItemStack retained = encoded(serverTerminal);
        require(samePattern(retained, oldPattern), "failed encoding retains the exact old sample and all NBT");
        report(
            "failure server after placement and C2S receipt: target empty; retained encoder "
                + describe(retained, player));
        phase = Phase.INVALID_RETAINED;
    }

    private void collectScreenshot(Minecraft mc, boolean invalid) throws Exception {
        if (screenshotLines != null) return;
        var inventory = clientInterfaceInventory();
        if (inventory == null) return;
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        ItemStack encoderOutput = encoded((DualTerminalGuiObject) container.getTarget());
        ItemStack received = invalid ? encoderOutput : inventory.getStackInSlot(0);
        if (received == null) return;
        require(!container.substituteSync.get(), "client substitution toggle remains off");
        if (invalid) {
            require(inventory.getStackInSlot(1) == null, "client failure target interface slot stays empty");
            require(samePattern(received, oldPattern), "client keeps the same old pattern in the encoder output");
        } else {
            require(encoderOutput == null, "client success encoder output slot is empty");
        }
        require(tag(received).getBoolean("substitute") == invalid, "client receives expected raw substitute flag");
        require(tag(received).hasKey(OLD_SAMPLE) == invalid, "client receives expected old or new sample identity");
        var lines = new ArrayList<String>();
        lines.add(invalid ? "编码失败，旧样板留在原编码槽" : "编码成功后上传了新样板");
        if (invalid) lines.add("目标接口槽为空");
        lines.add("编码前服务端矿物词典替换已关闭");
        for (Object line : received.getTooltip(mc.thePlayer, false)) lines.add((String) line);
        screenshotName = invalid ? "auto-upload-failed-keeps-old-pattern.png" : "auto-upload-success-new-pattern.png";
        screenshotLines = lines;
        report(
            (invalid ? "failure client: target empty; retained encoder " : "success client interface result: ")
                + describe(received, mc.thePlayer));
    }

    private void submitProcessingOverlay(Phase next, int multiplier) {
        ItemStack ingots = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.StainlessSteel, multiplier);
        var payload = new RecipeTransferPayload(
            false,
            true,
            4,
            false,
            new IAEStack<?>[] { AEItemStack.create(ingots) },
            new IAEStack<?>[] { AEFluidStack.create(Materials.StainlessSteel.getMolten(144L * multiplier)) },
            null,
            RecipeMaps.fluidExtractionRecipes.unlocalizedName);
        phase = next;
        clientGui.transferRecipe(payload, assemblerName());
    }

    private void seedProcessingPattern(EntityPlayerMP player) throws Exception {
        IInventory patterns = provider.getPatterns();
        ItemStack pattern = patterns.getStackInSlot(1);
        if (pattern == null) return;
        require(encoded(serverTerminal) == null, "first processing upload consumes the encoder output slot");
        requireProcessingOutput(pattern, player, 144);
        storedProcessingPattern = pattern.copy();
        receiptKeepNonConsumables = !serverTerminal.shouldKeepNonConsumables();
        report("first processing pattern uploaded with 144 mB molten stainless steel");
        phase = Phase.PROCESSING_SEEDED;
    }

    private void submitDuplicateWhenSynchronized() throws Exception {
        IInventory patterns = clientInterfaceInventory();
        if (patterns == null || !samePattern(patterns.getStackInSlot(1), storedProcessingPattern)) return;
        submitProcessingOverlay(Phase.DUPLICATE_SENT, 1);
    }

    private void awaitDuplicateUploadAttempt() throws Exception {
        if (field(QuickEncodingTerminalGui.class, "pendingAutoPlace").getBoolean(clientGui)) return;
        Object embedded = field(QuickEncodingTerminalGui.class, "interfaceTerminal").get(clientGui);
        var target = (InterfacePatternTarget) field(embedded.getClass(), "highlightedTarget").get(embedded);
        require(
            target != null && target.getEntryId() == targetId && target.getSlot() == 2,
            "duplicate automatic upload targets the next empty slot");
        phase = Phase.DUPLICATE_VERIFY;
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        container.requestKeepNonConsumables(receiptKeepNonConsumables);
    }

    private void verifyDuplicatePlacement(EntityPlayerMP player) throws Exception {
        if (serverTerminal.shouldKeepNonConsumables() != receiptKeepNonConsumables) return;
        IInventory patterns = provider.getPatterns();
        require(patterns.getStackInSlot(2) == null, "duplicate upload leaves the next interface slot empty");
        require(samePattern(patterns.getStackInSlot(1), storedProcessingPattern), "existing pattern remains unchanged");
        ItemStack duplicate = encoded(serverTerminal);
        require(duplicate != null, "duplicate upload retains the newly encoded processing pattern");
        requireProcessingOutput(duplicate, player, 144);
        require(
            tag(duplicate).getLong("encodedAt") != tag(storedProcessingPattern).getLong("encodedAt"),
            "the two patterns were encoded at different times");
        require(
            details(storedProcessingPattern, player).equals(details(duplicate, player)),
            "the two patterns describe the same recipe");
        ItemStack hand = duplicate.copy();
        tag(hand).setString("author", "QA different encoder");
        player.inventory.setItemStack(hand);
        try {
            serverContainer.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, 2, targetId);
            require(player.inventory.getItemStack() == hand, "manual insertion leaves the duplicate on the cursor");
            require(patterns.getStackInSlot(2) == null, "manual insertion also rejects the duplicate");
        } finally {
            player.inventory.setItemStack(null);
        }
        retainedDuplicatePattern = duplicate.copy();
        report("duplicate upload and manual insertion both rejected; original and newly encoded patterns retained");
        phase = Phase.DUPLICATE_RETAINED;
    }

    private void captureRetainedDuplicate(Minecraft mc) throws Exception {
        if (screenshotLines != null) return;
        IInventory patterns = clientInterfaceInventory();
        var container = (QuickEncodingTerminalContainer) clientGui.inventorySlots;
        ItemStack retained = encoded((DualTerminalGuiObject) container.getTarget());
        if (patterns == null || !samePattern(retained, retainedDuplicatePattern)) return;
        require(patterns.getStackInSlot(2) == null, "client sees the duplicate destination still empty");
        require(samePattern(patterns.getStackInSlot(1), storedProcessingPattern), "client sees the original pattern");
        screenshotName = "auto-upload-duplicate-keeps-encoded-pattern.png";
        var lines = new ArrayList<>(List.of("重复配方留在编码槽", "接口保留原样板，目标槽为空"));
        for (Object line : retained.getTooltip(mc.thePlayer, false)) lines.add((String) line);
        screenshotLines = lines;
    }

    private void verifyScaledPlacement(EntityPlayerMP player) throws Exception {
        IInventory patterns = provider.getPatterns();
        ItemStack scaled = patterns.getStackInSlot(2);
        if (scaled == null) return;
        require(encoded(serverTerminal) == null, "scaled processing pattern uploads successfully");
        requireProcessingOutput(scaled, player, 288);
        require(
            samePattern(patterns.getStackInSlot(1), storedProcessingPattern),
            "scaled upload preserves the original");
        report("scaled processing pattern with 288 mB of the same fluid uploaded to the same interface");
        phase = Phase.DONE;
    }

    private static void requireProcessingOutput(ItemStack pattern, EntityPlayer player, int amount) {
        ICraftingPatternDetails decoded = details(pattern, player);
        require(!decoded.isCraftable(), "fixture uses an actual processing pattern");
        IAEStack<?> output = decoded.getCondensedAEOutputs()[0];
        require(
            output.isSameType(AEFluidStack.create(Materials.StainlessSteel.getMolten(amount)))
                && output.getStackSize() == amount,
            "processing output is " + amount + " mB of molten stainless steel");
        require(
            RecipeMaps.fluidExtractionRecipes.unlocalizedName.equals(PatternRecipeMap.name(pattern)),
            "processing pattern carries the fluid extractor recipe map");
    }

    private IInventory clientInterfaceInventory() throws Exception {
        Object embedded = field(QuickEncodingTerminalGui.class, "interfaceTerminal").get(clientGui);
        Object master = field(GuiInterfaceTerminal.class, "masterList").get(embedded);
        Object entry = ((Map<?, ?>) field(master.getClass(), "list").get(master)).get(targetId);
        return entry == null ? null : (IInventory) field(entry.getClass(), "inv").get(entry);
    }

    private static DualTerminalGuiObject terminal(EntityPlayer player) {
        ItemStack stack = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        stack.setTagCompound(new NBTTagCompound());
        player.inventory.setInventorySlotContents(0, stack);
        var terminal = new DualTerminalGuiObject(
            (IWirelessTermHandler) stack.getItem(),
            stack,
            player,
            player.worldObj,
            0);
        terminal.setInventorySize(DualTerminalGuiObject.PATTERN_SLOT_COUNT, DualTerminalGuiObject.PATTERN_SLOT_COUNT);
        terminal.readInventory();
        terminal.setCraftingRecipe(true);
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

    private static String assemblerName() {
        return Platform.getItemDisplayName(
            AEApi.instance()
                .definitions()
                .blocks()
                .molecularAssembler()
                .maybeStack(1)
                .get());
    }

    private static ItemStack encoded(DualTerminalGuiObject terminal) {
        return terminal.getInventoryByName("pattern")
            .getStackInSlot(1);
    }

    private static NBTTagCompound tag(ItemStack pattern) {
        return pattern.getTagCompound();
    }

    private static boolean samePattern(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.isItemEqual(second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static ItemStack output(ItemStack pattern, EntityPlayer player) {
        return details(pattern, player).getCondensedAEOutputs()[0].getItemStackForNEI();
    }

    private static ICraftingPatternDetails details(ItemStack pattern, EntityPlayer player) {
        return ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, player.worldObj);
    }

    private static String describe(ItemStack pattern, EntityPlayer player) {
        ItemStack result = output(pattern, player);
        return "output=" + result.stackSize
            + "x "
            + result.getDisplayName()
            + ", substitute="
            + tag(pattern).getBoolean("substitute")
            + ", beSubstitute="
            + tag(pattern).getBoolean("beSubstitute")
            + ", originalMarker="
            + tag(pattern).getString(OLD_SAMPLE)
            + ", encodedAt="
            + tag(pattern).getLong("encodedAt");
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void report(String message) throws Exception {
        System.out.println(REPORT_PREFIX + message);
        Files.writeString(
            Path.of("report.txt"),
            message + System.lineSeparator(),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND);
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception io) {
            io.printStackTrace();
        }
        try {
            // End this isolated client's run loop without opening GTNH's interactive quit dialog.
            field(Minecraft.class, "running").setBoolean(Minecraft.getMinecraft(), false);
        } catch (ReflectiveOperationException exitError) {
            exitError.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }
    }

    private enum Phase {
        PREPARE,
        OPEN_CLIENT,
        ATTACH_SERVER,
        VALID_READY,
        VALID_CLOSED,
        VALID_OFF,
        VALID_SENT,
        VALID_STORED,
        VALID_CAPTURED,
        INVALID_READY,
        INVALID_CLOSED,
        INVALID_OFF,
        INVALID_SENT,
        INVALID_VERIFY,
        INVALID_RETAINED,
        PROCESSING_READY,
        PROCESSING_SEED_SENT,
        PROCESSING_SEEDED,
        DUPLICATE_SENT,
        DUPLICATE_VERIFY,
        DUPLICATE_RETAINED,
        SCALED_READY,
        SCALED_SENT,
        DONE
    }
}
