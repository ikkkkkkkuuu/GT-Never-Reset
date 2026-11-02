package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import org.lwjgl.opengl.GL11;

import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem.ProfileHelper;
import com.brandon3055.draconicevolution.common.utils.IUpgradableItem.EnumUpgrade;
import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtneverreset.common.gui.InsectStaffConfig;
import com.xyp.gtneverreset.common.item.items.InsectStaff;
import com.xyp.gtneverreset.common.item.items.VeinMiningPickaxe;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.teleport.TravelController;
import crazypants.enderio.teleport.packet.PacketTravelEvent;

@Mod(modid = "insectstaffqa", name = "Insect staff QA", version = "1", dependencies = "after:gtneverreset")
public final class InsectStaffChecks {

    private final List<String> report = new ArrayList<>();
    private volatile int phase;
    private volatile boolean finished;
    private boolean launched;
    private int clientTicks;
    private int wait;
    private int frame;
    private int screenStage;
    private ItemStack staffStack;
    private boolean protect;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void protect(BlockEvent.BreakEvent event) {
        if (protect && event.x == 4 && event.y == 10 && event.z == 0) event.setCanceled(true);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.START) return;
        var server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (phase < 3) player.setSneaking(true);
            if (phase == 0) {
                staffStack = GTNRItemList.InsectStaff.get(1);
                InsectStaff staff = (InsectStaff) staffStack.getItem();
                staffStack.setTagCompound(new NBTTagCompound());
                staffStack.getTagCompound()
                    .setInteger("ConfigProfile", 3);
                staffStack.getTagCompound()
                    .setString("ProfileName3", "old profile");
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.AMOUNT, 29);
                staff.initializeSettings(staffStack);
                require(
                    !(((Object) staff) instanceof com.brandon3055.draconicevolution.common.utils.IConfigurableItem),
                    "not a DE configurable item");
                require(InsectStaffConfig.amount(staffStack) == 29, "active settings retained during migration");
                require(
                    staffStack.getTagCompound()
                        .getTagList("ConfigProfiles", 10)
                        .tagCount() == 1
                        && staffStack.getTagCompound()
                            .getInteger("ConfigProfile") == 0
                        && !staffStack.getTagCompound()
                            .hasKey("ProfileName3"),
                    "one stored configuration without profile names");
                staffStack.getTagCompound()
                    .setInteger("Energy", 0);
                for (EnumUpgrade upgrade : staff.getUpgrades(staffStack)) require(
                    upgrade.getUpgradePoints(staffStack) == staff.getMaxUpgradePoints(upgrade.index),
                    "max upgrade " + upgrade);
                require(
                    staff.getAttackDamage(staffStack) == ModItems.CHAOTIC.getDamageVsEntity() + 320,
                    "full staff attack damage");
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, staffStack);
                player.setGameType(WorldSettings.GameType.SURVIVAL);
                player.setPositionAndUpdate(0.5, 11, 3.5);
                for (int x = -2; x < 10; x++)
                    for (int z = -2; z < 6; z++) player.worldObj.setBlock(x, 9, z, Blocks.stone);
                verifyCombat(player, staff);
                verifyAreaMining(player, staff);
                for (int x = 0; x < 5; x++) player.worldObj.setBlock(x, 10, 0, Blocks.coal_ore);
                player.worldObj.setBlock(5, 10, 0, Blocks.diamond_ore);
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.RANGE, 0);
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.AMOUNT, 3);
                ProfileHelper.setBoolean(staffStack, InsectStaffConfig.PRECISE, true);
                player.setSneaking(true);
                player.theItemInWorldManager.tryHarvestBlock(0, 10, 0);
                phase = 1;
                wait = 0;
            } else if (phase == 1 && ++wait > 5) {
                require(
                    player.worldObj.getBlock(0, 10, 0) == Blocks.stone
                        && player.worldObj.getBlock(2, 10, 0) == Blocks.stone,
                    "vein backfill");
                require(player.worldObj.getBlock(3, 10, 0) == Blocks.coal_ore, "vein amount limit");
                require(
                    staffStack.getTagCompound()
                        .getInteger("Energy") == 0,
                    "vein mining works without RF");
                int coal = 0;
                for (Object entity : player.worldObj
                    .getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(-5, 5, -5, 10, 15, 10))) {
                    ItemStack drop = ((EntityItem) entity).getEntityItem();
                    if (drop.getItem() == Items.coal) coal += drop.stackSize;
                }
                for (ItemStack drop : player.inventory.mainInventory)
                    if (drop != null && drop.getItem() == Items.coal) coal += drop.stackSize;
                require(coal == 3, "exactly one vanilla coal drop per ore");
                protect = true;
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.AMOUNT, 8);
                player.theItemInWorldManager.tryHarvestBlock(3, 10, 0);
                phase = 2;
                wait = 0;
            } else if (phase == 2 && ++wait > 5) {
                require(player.worldObj.getBlock(4, 10, 0) == Blocks.coal_ore, "Forge protection respected");
                require(player.worldObj.getBlock(5, 10, 0) == Blocks.diamond_ore, "precise mode leaves other ores");
                protect = false;
                verifySharedVeinMining(player);
                verifyHeldSpeed(player);
                require(
                    TravelController.instance.getTravelItemTravelSource(player, false) == TravelSource.TELEPORT_STAFF,
                    "advanced staff travel source");
                require(
                    TravelController.instance
                        .validatePacketTravelEvent(player, 8, 10, 3, 0, false, TravelSource.TELEPORT_STAFF_BLINK)
                        == null,
                    "advanced teleport accepted");
                require(
                    TravelController.instance
                        .validatePacketTravelEvent(player, 1000000, 10, 3, 0, false, TravelSource.TELEPORT_STAFF_BLINK)
                        != null,
                    "teleport distance validation retained");
                ProfileHelper.setBoolean(staffStack, InsectStaffConfig.TELEPORT, false);
                require(
                    TravelController.instance
                        .validatePacketTravelEvent(player, 8, 10, 3, 0, false, TravelSource.TELEPORT_STAFF_BLINK)
                        != null,
                    "disabled teleport rejected");
                ProfileHelper.setBoolean(staffStack, InsectStaffConfig.TELEPORT, true);
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
                phase = 3;
            } else if (phase == 1030 && ++wait > 5) {
                require(
                    InsectStaffConfig.amount(staffStack) == 123456,
                    "typed six-digit amount synchronized: " + InsectStaffConfig.amount(staffStack));
                phase = 1031;
            } else if (phase == 1032 && ++wait > 5) {
                require(
                    InsectStaffConfig.amount(staffStack)
                        == com.xyp.gtneverreset.config.VeinMinerPickaxeConfig.maxAmount,
                    "typed amount clamped to configured maximum");
                phase = 1033;
            } else if (phase == 1034 && ++wait > 5) {
                require(
                    InsectStaffConfig.amount(staffStack)
                        == com.xyp.gtneverreset.config.VeinMinerPickaxeConfig.maxAmount,
                    "one-click maximum synchronized");
                phase = 1035;
            } else if (phase == 1036 && ++wait > 5) {
                require(InsectStaffConfig.amount(staffStack) == 0, "negative input clamped to zero");
                phase = 1037;
            } else if (phase == 4) {
                require(
                    Math.abs(InsectStaffConfig.speedMultiplier(staffStack, InsectStaffConfig.SPRINT_SPEED) - 3.5F)
                        < 0.001F,
                    "sprint multiplier input synchronized");
                require(
                    InsectStaffConfig.speedMultiplier(staffStack, InsectStaffConfig.FLIGHT_SPEED) == 10F,
                    "flight multiplier maximum synchronized");
                require(InsectStaffConfig.range(staffStack) == 2, "MUI range synchronized");
                require(InsectStaffConfig.amount(staffStack) == 17, "MUI amount synchronized");
                require(
                    !ProfileHelper.getBoolean(staffStack, InsectStaffConfig.PRECISE, true),
                    "MUI precise toggle synchronized");
                require(!InsectStaffConfig.veinEnabled(staffStack), "MUI vein off synchronized");
                ItemStack restored = ItemStack.loadItemStackFromNBT(staffStack.writeToNBT(new NBTTagCompound()));
                require(
                    InsectStaffConfig.amount(restored) == 17 && !InsectStaffConfig.veinEnabled(restored),
                    "settings persist in NBT");
                phase = 5;
            } else if (phase == 53) {
                require(InsectStaffConfig.amount(staffStack) == 17, "single configuration retained");
                player.inventory.setItemStack(new ItemStack(Blocks.dirt));
                player.updateHeldItem();
                phase = 54;
            } else if (phase == 56) {
                require(
                    staffStack.getTagCompound()
                        .getCompoundTag("Item0")
                        .hasKey("id"),
                    "obliteration filter synchronized");
                require(player.inventory.getItemStack() == null, "filter consumes a real item, no ghost duplication");
                ItemStack book = new ItemStack(Items.enchanted_book);
                NBTTagCompound enchantment = new NBTTagCompound();
                enchantment.setShort("id", (short) net.minecraft.enchantment.Enchantment.fortune.effectId);
                enchantment.setShort("lvl", (short) 3);
                var list = new net.minecraft.nbt.NBTTagList();
                list.appendTag(enchantment);
                book.setTagCompound(new NBTTagCompound());
                book.getTagCompound()
                    .setTag("StoredEnchantments", list);
                player.inventory.setItemStack(book);
                player.updateHeldItem();
                phase = 561;
            } else if (phase == 563) {
                require(
                    net.minecraft.enchantment.EnchantmentHelper
                        .getEnchantmentLevel(net.minecraft.enchantment.Enchantment.fortune.effectId, staffStack) == 3,
                    "enchantment book applied through real slot");
                require(player.inventory.getItemStack() == null, "enchantment book consumed once");
                phase = 564;
            } else if (phase == 566) {
                require(
                    net.minecraft.enchantment.EnchantmentHelper
                        .getEnchantmentLevel(net.minecraft.enchantment.Enchantment.fortune.effectId, staffStack) == 0,
                    "enchantment removed through slot");
                require(
                    player.inventory.getItemStack() != null && player.inventory.getItemStack()
                        .getItem() == Items.enchanted_book,
                    "enchantment book returned once");
                phase = 57;
            } else if (phase == 8) {
                require(
                    Math.abs(player.posX - 8.5) < 1 && Math.abs(player.posZ - 3.5) < 1,
                    "EIO packet teleports the player");
                phase = 9;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen != null) {
                launched = true;
                mc.launchIntegratedServer(
                    "staff-qa-" + System.currentTimeMillis(),
                    "Staff QA",
                    new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (++clientTicks > 2400) throw new AssertionError("timeout phase=" + phase);
            if (phase < 3) mc.thePlayer.setSneaking(true);
            if (phase == 3 && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem()
                    .getItem() instanceof InsectStaff
                && !(mc.currentScreen instanceof IMuiScreen)) {
                var button = new com.brandon3055.draconicevolution.client.gui.guicomponents.ComponentConfigItemButton(
                    0,
                    0,
                    0,
                    mc.thePlayer);
                require(!button.hasValidItem, "DE configuration cannot select insect staff");
                net.minecraft.client.settings.KeyBinding
                    .onTick(com.xyp.gtneverreset.client.InsectStaffInputHandler.CONFIGURE.getKeyCode());
                cpw.mods.fml.common.FMLCommonHandler.instance()
                    .bus()
                    .post(new cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
                screenStage = 1;
                frame = 0;
            }
            if (phase == 7 && ++wait > 20) phase = 8;
            if (phase == 9) {
                mc.displayGuiScreen(new ModelPreview());
                phase = 10;
                frame = 0;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (phase == 10 && ++frame == 35) {
                screenshot("model");
                mc.displayGuiScreen(new TooltipPreview());
                phase = 11;
                frame = 0;
                return;
            }
            if (phase == 11 && ++frame == 35) {
                screenshot("tooltip");
                var lines = mc.thePlayer.getHeldItem()
                    .getTooltip(mc.thePlayer, false);
                String text = String.join("\n", lines);
                require(
                    text.contains("攻击伤害：380") && text.contains("按 G 打开配置"),
                    "Chinese tooltip and configured key displayed");
                require(
                    !text.contains(" RF") && !text.contains("充能:") && !text.contains("Bind Configure"),
                    "no charge line or English fallback");
                mc.thePlayer.inventory.setInventorySlotContents(8, new ItemStack(ModItems.draconicDestructionStaff));
                NativeToolConfigPreview reference = new NativeToolConfigPreview(mc.thePlayer);
                mc.displayGuiScreen(reference);
                reference.selectStaff();
                phase = 12;
                frame = 0;
                return;
            }
            if (phase == 12 && ++frame == 35) {
                screenshot("de-original-list");
                ((NativeToolConfigPreview) mc.currentScreen).selectField();
                phase = 13;
                frame = 0;
                return;
            }
            if (phase == 13 && ++frame == 35) {
                screenshot("de-original-adjuster");
                ((NativeToolConfigPreview) mc.currentScreen).buttonClicked(1, 0);
                phase = 14;
                frame = 0;
                return;
            }
            if (phase == 14 && ++frame == 35) {
                screenshot("de-original-inventory");
                finish(null);
                return;
            }
            if (!(mc.currentScreen instanceof IMuiScreen mui)) return;
            ModularScreen screen = mui.getScreen();
            if (!screen.getMainPanel()
                .getName()
                .equals("insect_staff") || ++frame < 30) return;
            if (screenStage == 1) {
                verifyHeldSpeed(mc.thePlayer);
                screenshot("settings-before");
                require(
                    !screen.getContext()
                        .getRecipeViewerSettings()
                        .isEnabled(screen),
                    "NEI hidden on DE-style configuration screens");
                require(
                    findWidget(screen.getMainPanel(), "configuration_list").getArea().width == 198
                        && screen.getMainPanel()
                            .getArea().height == 178,
                    "DE-sized option list includes all staff settings");
                IWidget row = findWidget(screen.getMainPanel(), InsectStaffConfig.RANGE);
                require(row.getArea().width == 150 && row.getArea().height == 12, "DE field button dimensions");
                click(
                    screen.getMainPanel(),
                    com.brandon3055.draconicevolution.common.lib.References.DIG_SPEED_MULTIPLIER);
                screenStage = 100;
                frame = 0;
            } else if (screenStage == 100) {
                var pages = (com.cleanroommc.modularui.widgets.PagedWidget<?>) findWidget(
                    screen.getMainPanel(),
                    "configuration_pages");
                require(
                    pages.getCurrentPageIndex() > 0 && pages.getCurrentPage()
                        .getArea().width == 198
                        && pages.getCurrentPage()
                            .getArea().height == 89,
                    "numeric option opens original-size adjustment page");
                screenshot("settings-adjuster");
                String speed = com.brandon3055.draconicevolution.common.lib.References.DIG_SPEED_MULTIPLIER;
                click(screen.getMainPanel(), speed + "_-1");
                click(screen.getMainPanel(), speed + "_1");
                click(screen.getMainPanel(), speed + "_100");
                require(
                    ((DoubleSyncValue) screen.getSyncManager()
                        .getMainPSM()
                        .findSyncHandlerNullable(speed, 0)).getDoubleValue() == 1,
                    "float steps round and clamp like DE");
                click(screen.getMainPanel(), speed + "_back");
                require(pages.getCurrentPageIndex() == 0, "back returns to option list");
                click(screen.getMainPanel(), InsectStaffConfig.SPRINT_SPEED);
                screenStage = 110;
                frame = 0;
            } else if (screenStage == 110) {
                String speed = InsectStaffConfig.SPRINT_SPEED;
                inputNumber(screen, speed, "3.5");
                screenshot("settings-sprint-speed");
                click(screen.getMainPanel(), speed + "_1");
                require(
                    Math.abs(
                        ((DoubleSyncValue) screen.getSyncManager()
                            .getMainPSM()
                            .findSyncHandlerNullable(speed, 0)).getDoubleValue() - 3.6D)
                        < 0.001D,
                    "speed multiplier increment is 0.1");
                click(screen.getMainPanel(), speed + "_-1");
                click(screen.getMainPanel(), speed + "_back");
                click(screen.getMainPanel(), InsectStaffConfig.FLIGHT_SPEED);
                screenStage = 111;
                frame = 0;
            } else if (screenStage == 111) {
                String speed = InsectStaffConfig.FLIGHT_SPEED;
                inputNumber(screen, speed, "99");
                require(
                    ((DoubleSyncValue) screen.getSyncManager()
                        .getMainPSM()
                        .findSyncHandlerNullable(speed, 0)).getDoubleValue() == 10D,
                    "flight input clamps to 10x");
                screenshot("settings-flight-speed");
                click(screen.getMainPanel(), speed + "_back");
                click(screen.getMainPanel(), InsectStaffConfig.RANGE);
                screenStage = 101;
                frame = 0;
            } else if (screenStage == 101) {
                screenshot("settings-vein-range");
                click(screen.getMainPanel(), InsectStaffConfig.RANGE + "_-100");
                click(screen.getMainPanel(), InsectStaffConfig.RANGE + "_1");
                click(screen.getMainPanel(), InsectStaffConfig.RANGE + "_1");
                click(screen.getMainPanel(), InsectStaffConfig.RANGE + "_back");
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT);
                screenStage = 102;
                frame = 0;
            } else if (screenStage == 102) {
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_-100");
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_10");
                for (int i = 0; i < 7; i++) click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_1");
                screenStage = 103;
                frame = 0;
            } else if (screenStage == 103) {
                screenshot("settings-vein-amount");
                inputNumber(screen, InsectStaffConfig.AMOUNT, "123456");
                screenStage = 104;
                phase = 1030;
                wait = 0;
                frame = 0;
            } else if (screenStage == 104 && phase == 1031) {
                screenshot("settings-vein-input");
                inputNumber(screen, InsectStaffConfig.AMOUNT, "999999999");
                phase = 1032;
                wait = 0;
                frame = 0;
            } else if (screenStage == 104 && phase == 1033) {
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_-100");
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_maximum");
                phase = 1034;
                wait = 0;
                frame = 0;
            } else if (screenStage == 104 && phase == 1035) {
                screenshot("settings-vein-maximum");
                inputNumber(screen, InsectStaffConfig.AMOUNT, "-1");
                phase = 1036;
                wait = 0;
                frame = 0;
            } else if (screenStage == 104 && phase == 1037) {
                inputNumber(screen, InsectStaffConfig.AMOUNT, "17");
                phase = 3;
                click(screen.getMainPanel(), InsectStaffConfig.AMOUNT + "_back");
                click(screen.getMainPanel(), InsectStaffConfig.PRECISE);
                click(screen.getMainPanel(), InsectStaffConfig.ENABLED);
                var pages = (com.cleanroommc.modularui.widgets.PagedWidget<?>) findWidget(
                    screen.getMainPanel(),
                    "configuration_pages");
                require(pages.getCurrentPageIndex() == 0, "boolean options toggle directly without leaving list");
                screenStage = 2;
                frame = 0;
            } else if (screenStage == 2 && phase == 3) {
                phase = 4;
                frame = 0;
            } else if (phase == 5) {
                screenshot("settings-after");
                require(
                    screen.getMainPanel()
                        .getChildren()
                        .stream()
                        .noneMatch(
                            w -> String.valueOf(w.getName())
                                .startsWith("profile_")),
                    "no profile controls in GUI");
                require(
                    screen.getSyncManager()
                        .getMainPSM()
                        .findSyncHandlerNullable("profile", 0) == null,
                    "no profile switch sync handler");
                phase = 53;
                frame = 0;
            } else if (phase == 54) {
                click(screen.getMainPanel(), "obliteration_filter");
                phase = 55;
                frame = 0;
            } else if (phase == 55) {
                var filter = screen.getPanelManager()
                    .getOpenPanels()
                    .stream()
                    .filter(
                        p -> p.getName()
                            .equals("insect_staff_filter"))
                    .findFirst()
                    .orElseThrow();
                screenshot("filter");
                require(
                    screen.getContext()
                        .getRecipeViewerSettings()
                        .isEnabled(screen),
                    "NEI available in tool inventory like DE");
                clickSlot(screen, "filter_0");
                phase = 551;
                frame = 0;
            } else if (phase == 551) {
                phase = 56;
                frame = 0;
            } else if (phase == 561) {
                clickSlot(screen, "enchantment_0");
                phase = 562;
                frame = 0;
            } else if (phase == 562) {
                phase = 563;
                frame = 0;
            } else if (phase == 564) {
                screenshot("filter-enchanted");
                clickSlot(screen, "enchantment_0");
                phase = 565;
                frame = 0;
            } else if (phase == 565) {
                phase = 566;
                frame = 0;
            } else if (phase == 57) {
                var filter = screen.getPanelManager()
                    .getOpenPanels()
                    .stream()
                    .filter(
                        p -> p.getName()
                            .equals("insect_staff_filter"))
                    .findFirst()
                    .orElseThrow();
                require(
                    filter.getArea().width == 198 && filter.getArea().height == 130,
                    "original tool inventory size");
                click(filter, "filter_back");
                mc.gameSettings.guiScale = 3;
                mc.currentScreen.setWorldAndResolution(mc, 366, 246);
                phase = 58;
                frame = 0;
            } else if (phase == 58) {
                screenshot("settings-scale-3");
                require(
                    !screen.getContext()
                        .getRecipeViewerSettings()
                        .isEnabled(screen),
                    "return from inventory restores hidden NEI");
                require(
                    screen.getMainPanel()
                        .getArea().width <= 366
                        && screen.getMainPanel()
                            .getArea().height <= 246,
                    "complete option list fits GUI scale 3");
                mc.gameSettings.guiScale = 2;
                mc.thePlayer.closeScreen();
                PacketHandler.INSTANCE.sendToServer(
                    new PacketTravelEvent(mc.thePlayer, 8, 10, 3, 0, false, TravelSource.TELEPORT_STAFF_BLINK));
                phase = 7;
                wait = 0;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static final class NativeToolConfigPreview
        extends com.brandon3055.draconicevolution.client.gui.componentguis.GUIToolConfig {

        private NativeToolConfigPreview(net.minecraft.entity.player.EntityPlayer player) {
            super(
                player,
                new com.brandon3055.draconicevolution.common.container.ContainerAdvTool(
                    player.inventory,
                    new com.brandon3055.draconicevolution.common.inventory.InventoryTool(player, null)) {

                    @Override
                    public Object sendObjectToServer(
                        com.brandon3055.draconicevolution.common.tileentities.TileObjectSync tile, int index,
                        Object object) {
                        return object;
                    }
                });
        }

        private void selectStaff() {
            collection.removeScheduled();
            mouseClicked(guiLeft + 29 + 18 * 8 + 4, guiTop + 68, 0);
        }

        private void selectField() {
            var field = ((com.brandon3055.draconicevolution.common.utils.IConfigurableItem) ModItems.draconicDestructionStaff)
                .getFields(player.inventory.getStackInSlot(8), 8)
                .get(0);
            setFieldBeingEdited(field);
        }
    }

    private void verifyHeldSpeed(net.minecraft.entity.player.EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        Runnable tick = () -> FMLCommonHandler.instance()
            .bus()
            .post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        var profile = ProfileHelper.getProfileCompound(held);
        boolean sprinting = player.isSprinting();
        boolean flying = player.capabilities.isFlying;
        boolean allowFlight = player.capabilities.allowFlying;
        boolean creative = player.capabilities.isCreativeMode;
        float originalFlight = player.capabilities.getFlySpeed();
        int slot = player.inventory.currentItem;
        var attribute = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed);
        var otherSpeed = new net.minecraft.entity.ai.attributes.AttributeModifier(
            java.util.UUID.fromString("16b95f55-6bb2-400f-b778-afc33b6ef6dc"),
            "QA external speed",
            0.25D,
            1);
        try {
            profile.removeTag(InsectStaffConfig.SPRINT_SPEED);
            profile.removeTag(InsectStaffConfig.FLIGHT_SPEED);
            require(
                InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.SPRINT_SPEED) == 2F
                    && InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.FLIGHT_SPEED) == 2F,
                "held movement defaults to 2x on " + (player.worldObj.isRemote ? "client" : "server"));
            player.capabilities.isFlying = false;
            player.setSprinting(false);
            tick.run();
            attribute.applyModifier(otherSpeed);
            double walking = attribute.getAttributeValue();
            player.setSprinting(true);
            double running = attribute.getAttributeValue();
            tick.run();
            require(
                Math.abs(attribute.getAttributeValue() - running * 2D) < 0.00001D,
                "held sprint doubles existing running speed including external modifier");
            for (int i = 0; i < 20; i++) tick.run();
            require(
                Math.abs(attribute.getAttributeValue() - running * 2D) < 0.00001D,
                "sprint boost does not compound across ticks");
            profile.setFloat(InsectStaffConfig.SPRINT_SPEED, 10F);
            tick.run();
            require(Math.abs(attribute.getAttributeValue() - running * 10D) < 0.00001D, "10x sprint works");
            player.setSprinting(false);
            tick.run();
            require(Math.abs(attribute.getAttributeValue() - walking) < 0.00001D, "walking restores original speed");
            player.setSprinting(true);
            tick.run();
            player.inventory.setInventorySlotContents(slot, null);
            tick.run();
            require(Math.abs(attribute.getAttributeValue() - running) < 0.00001D, "unholding restores sprint speed");
            player.inventory.setInventorySlotContents(slot, held);
            if (!player.worldObj.isRemote) return;
            player.capabilities.isCreativeMode = true;
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.inventory.setInventorySlotContents(slot, null);
            tick.run();
            float flightBase = player.capabilities.getFlySpeed();
            player.inventory.setInventorySlotContents(slot, held);
            for (int i = 0; i < 20; i++) tick.run();
            require(
                Math.abs(player.capabilities.getFlySpeed() - flightBase * 2F) < 0.00001F,
                "flight doubles existing speed without compounding: base=" + flightBase
                    + " speed="
                    + player.capabilities.getFlySpeed()
                    + " flying="
                    + player.capabilities.isFlying);
            profile.setFloat(InsectStaffConfig.FLIGHT_SPEED, 10F);
            tick.run();
            require(Math.abs(player.capabilities.getFlySpeed() - flightBase * 10F) < 0.00001F, "10x flight works");
            InsectStaff staff = (InsectStaff) held.getItem();
            Runnable staffTick = () -> {
                var event = new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player);
                staff.restoreFlightSpeed(event);
                staff.applyHeldSpeed(event);
            };
            player.capabilities.setFlySpeed(0.08F);
            staffTick.run();
            require(
                Math.abs(player.capabilities.getFlySpeed() - 0.8F) < 0.00001F,
                "external flight speed change becomes new base");
            profile.setFloat(InsectStaffConfig.FLIGHT_SPEED, 1F);
            staffTick.run();
            require(Math.abs(player.capabilities.getFlySpeed() - 0.08F) < 0.00001F, "1x disables flight boost");
            profile.setFloat(InsectStaffConfig.FLIGHT_SPEED, 2F);
            staffTick.run();
            player.capabilities.isFlying = false;
            staffTick.run();
            require(
                Math.abs(player.capabilities.getFlySpeed() - 0.08F) < 0.00001F,
                "landing restores base flight speed");
            player.capabilities.isFlying = true;
            staffTick.run();
            player.inventory.setInventorySlotContents(slot, null);
            staffTick.run();
            require(
                Math.abs(player.capabilities.getFlySpeed() - 0.08F) < 0.00001F,
                "unholding restores flight speed without resetting external speed");
            profile.setFloat(InsectStaffConfig.SPRINT_SPEED, Float.NaN);
            profile.setFloat(InsectStaffConfig.FLIGHT_SPEED, 100F);
            require(
                InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.SPRINT_SPEED) == 2F
                    && InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.FLIGHT_SPEED) == 10F,
                "malformed movement settings are bounded");
        } finally {
            player.inventory.setInventorySlotContents(slot, null);
            tick.run();
            attribute.removeModifier(otherSpeed);
            profile.removeTag(InsectStaffConfig.SPRINT_SPEED);
            profile.removeTag(InsectStaffConfig.FLIGHT_SPEED);
            player.setSprinting(sprinting);
            player.capabilities.isFlying = flying;
            player.capabilities.isCreativeMode = creative;
            player.capabilities.allowFlying = allowFlight;
            player.capabilities.setFlySpeed(originalFlight);
            player.inventory.setInventorySlotContents(slot, held);
        }
    }

    private void verifySharedVeinMining(EntityPlayerMP player) {
        var pickaxe = (VeinMiningPickaxe) GTNRItemList.VeinMiningPickaxe.getItem();
        for (int gap : new int[] { 0, 1, 2 }) {
            for (int amount : new int[] { 1, 3, 12 }) {
                for (boolean precise : new boolean[] { false, true }) {
                    int expected = mineComparisonPattern(
                        player,
                        GTNRItemList.VeinMiningPickaxe.get(1),
                        gap,
                        amount,
                        precise);
                    int actual = mineComparisonPattern(player, staffStack, gap, amount, precise);
                    require(
                        actual == expected,
                        "pickaxe/staff footprint gap=" + gap + " amount=" + amount + " precise=" + precise);
                }
            }
        }
        player.inventory.setInventorySlotContents(0, staffStack);
        ProfileHelper.setInteger(staffStack, InsectStaffConfig.RANGE, 0);
        ProfileHelper.setInteger(staffStack, InsectStaffConfig.AMOUNT, 80);
        ProfileHelper.setBoolean(staffStack, InsectStaffConfig.PRECISE, true);
        ProfileHelper.setBoolean(staffStack, InsectStaffConfig.BACKFILL, false);
        for (int x = 0; x < 4; x++)
            for (int y = 14; y < 18; y++) for (int z = 0; z < 5; z++) player.worldObj.setBlock(x, y, z, Blocks.glass);
        player.theItemInWorldManager.tryHarvestBlock(0, 14, 0);
        int mined = 0;
        for (int x = 0; x < 4; x++)
            for (int y = 14; y < 18; y++) for (int z = 0; z < 5; z++) if (player.worldObj.isAirBlock(x, y, z)) mined++;
        require(mined == 80, "all 80 blocks harvested synchronously without the former 64-block cap");
        require(
            staffStack.getTagCompound()
                .getInteger("Energy") == 0,
            "shared mining never consumes RF");
        require(
            !staffStack.getTagCompound()
                .hasKey("GT.ToolStats") && staffStack.stackSize == 1,
            "shared mining does not add pickaxe durability to the staff");

        player.worldObj.setBlock(0, 22, 0, Blocks.coal_ore);
        player.theItemInWorldManager.tryHarvestBlock(0, 22, 0);
        pickaxe.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        require(player.worldObj.isAirBlock(0, 22, 0), "backfill toggle off respected");
        ProfileHelper.setBoolean(staffStack, InsectStaffConfig.BACKFILL, true);
        ProfileHelper.setInteger(staffStack, com.brandon3055.draconicevolution.common.lib.References.DIG_AOE, 0);
        ProfileHelper.setInteger(staffStack, com.brandon3055.draconicevolution.common.lib.References.DIG_DEPTH, 1);
        player.setSneaking(false);
        player.worldObj.setBlock(0, 22, 0, Blocks.coal_ore);
        player.theItemInWorldManager.tryHarvestBlock(0, 22, 0);
        pickaxe.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        require(
            player.worldObj.getBlock(0, 22, 0) == Blocks.stone,
            "ordinary ore mining uses pickaxe deferred backfill");
        player.setSneaking(true);
        ProfileHelper.setBoolean(staffStack, InsectStaffConfig.ENABLED, false);
        player.worldObj.setBlock(0, 22, 0, Blocks.coal_ore);
        player.worldObj.setBlock(1, 22, 0, Blocks.coal_ore);
        player.theItemInWorldManager.tryHarvestBlock(0, 22, 0);
        pickaxe.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        require(
            player.worldObj.isAirBlock(0, 22, 0) && player.worldObj.getBlock(1, 22, 0) == Blocks.coal_ore,
            "disabled vein mining leaves neighbors and does not backfill");
        ProfileHelper.setBoolean(staffStack, InsectStaffConfig.ENABLED, true);
        var staff = (InsectStaff) staffStack.getItem();
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.DIG_AOE,
            staff.getBaseUpgradePoints(EnumUpgrade.DIG_AOE.index));
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.DIG_DEPTH,
            staff.getBaseUpgradePoints(EnumUpgrade.DIG_DEPTH.index));
    }

    private int mineComparisonPattern(EntityPlayerMP player, ItemStack tool, int gap, int amount, boolean precise) {
        player.inventory.setInventorySlotContents(0, tool);
        player.getFoodStats()
            .addStats(20, 1);
        player.setSneaking(true);
        if (tool.getItem() instanceof InsectStaff) {
            ProfileHelper.setInteger(tool, InsectStaffConfig.RANGE, gap);
            ProfileHelper.setInteger(tool, InsectStaffConfig.AMOUNT, amount);
            ProfileHelper.setBoolean(tool, InsectStaffConfig.PRECISE, precise);
        } else {
            tool.setTagCompound(new NBTTagCompound());
            tool.getTagCompound()
                .setInteger("range", gap);
            tool.getTagCompound()
                .setInteger("amount", amount);
            tool.getTagCompound()
                .setBoolean("preciseMode", precise);
        }
        for (int x = 0; x < 8; x++) player.worldObj.setBlockToAir(x, 20, 0);
        player.worldObj.setBlock(0, 20, 0, Blocks.stained_hardened_clay, 0, 3);
        player.worldObj.setBlock(1, 20, 0, Blocks.stained_hardened_clay, 1, 3);
        player.worldObj.setBlock(3, 20, 0, Blocks.stained_hardened_clay, 0, 3);
        player.worldObj.setBlock(6, 20, 0, Blocks.stained_hardened_clay, 0, 3);
        player.worldObj.setBlock(7, 20, 0, Blocks.stained_hardened_clay, 1, 3);
        player.theItemInWorldManager.tryHarvestBlock(0, 20, 0);
        int footprint = 0;
        for (int x : new int[] { 0, 1, 3, 6, 7 }) if (player.worldObj.isAirBlock(x, 20, 0)) footprint |= 1 << x;
        return footprint;
    }

    private void verifyAreaMining(EntityPlayerMP player, InsectStaff staff) {
        for (int x = 6; x <= 8; x++) for (int z = 2; z <= 4; z++) player.worldObj.setBlock(x, 10, z, Blocks.glass);
        player.setPositionAndUpdate(7.5, 12, 3.5);
        player.rotationPitch = 90;
        player.setSneaking(false);
        ProfileHelper.setInteger(staffStack, com.brandon3055.draconicevolution.common.lib.References.DIG_AOE, 1);
        ProfileHelper.setInteger(staffStack, com.brandon3055.draconicevolution.common.lib.References.DIG_DEPTH, 1);
        player.theItemInWorldManager.tryHarvestBlock(7, 10, 3);
        for (int x = 6; x <= 8; x++) for (int z = 2; z <= 4; z++)
            require(player.worldObj.isAirBlock(x, 10, z), "area mining without RF at " + x + "," + z);
        require(
            staffStack.getTagCompound()
                .getInteger("Energy") == 0,
            "area mining does not write RF");
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.DIG_AOE,
            staff.getBaseUpgradePoints(EnumUpgrade.DIG_AOE.index));
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.DIG_DEPTH,
            staff.getBaseUpgradePoints(EnumUpgrade.DIG_DEPTH.index));
        player.setPositionAndUpdate(0.5, 11, 3.5);
        player.rotationPitch = 0;
    }

    private void verifyCombat(EntityPlayerMP player, InsectStaff staff) {
        var target = new net.minecraft.entity.passive.EntityCow(player.worldObj);
        var nearby = new net.minecraft.entity.passive.EntityCow(player.worldObj);
        for (var cow : new net.minecraft.entity.passive.EntityCow[] { target, nearby }) {
            cow.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(1000);
            cow.setHealth(1000);
            cow.setPosition(cow == target ? 1 : 2, 11, 3);
            require(player.worldObj.spawnEntityInWorld(cow), "combat test entity spawned");
        }
        ProfileHelper.setInteger(staffStack, com.brandon3055.draconicevolution.common.lib.References.ATTACK_AOE, 1);
        float base = staff.getAttackDamage(staffStack);
        staff.onLeftClickEntity(staffStack, player, target);
        require(
            Math.abs(target.getHealth() - (1000 - base - 300)) < 0.01,
            "direct attack includes full damage and health bonus");
        require(
            Math.abs(nearby.getHealth() - (1000 - base)) < 0.01,
            "area attack uses own damage calculation: actual=" + nearby.getHealth() + ", base=" + base);
        require(
            staffStack.getTagCompound()
                .getInteger("Energy") == 0,
            "full combat damage without RF");
        require(
            staff.getMaxEnergyStored(staffStack) == 0 && staff.receiveEnergy(staffStack, 1000, false) == 0
                && staff.extractEnergy(staffStack, 1000, false) == 0,
            "staff cannot store or transfer RF");
        require(!staff.showDurabilityBar(staffStack), "no charging bar");
        require(staff.getDigSpeed(staffStack, Blocks.stone, 0) > 1, "full mining speed without RF");
        target.setDead();
        nearby.setDead();
        staffStack.addEnchantment(net.minecraft.enchantment.Enchantment.sharpness, 3);
        require(staff.getAttackDamage(staffStack) == base + 12, "sharpness included without mixin");
        staffStack.getTagCompound()
            .removeTag("ench");
        staffStack.getTagCompound()
            .setInteger("Energy", 0);
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.ATTACK_AOE,
            staff.getBaseUpgradePoints(EnumUpgrade.ATTACK_AOE.index));
    }

    private void inputNumber(ModularScreen screen, String key, String text) {
        var input = (com.cleanroommc.modularui.widgets.textfield.TextFieldWidget) findWidget(
            screen.getMainPanel(),
            key + "_input");
        screen.getContext()
            .focus(input);
        input.setText(text);
        input.onKeyPressed('\r', org.lwjgl.input.Keyboard.KEY_RETURN);
        require(!input.isFocused(), "Enter commits numeric input");
    }

    private IWidget findWidget(IWidget parent, String name) {
        if (name.equals(parent.getName())) return parent;
        for (IWidget child : parent.getChildren()) {
            IWidget found = findWidget(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void click(IWidget parent, String name) {
        IWidget widget = findWidget(parent, name);
        require(widget instanceof ButtonWidget<?>, "button exists: " + name);
        ((ButtonWidget<?>) widget).onMousePressed(0);
    }

    private void clickSlot(ModularScreen screen, String name) {
        var slot = screen.getPanelManager()
            .getOpenPanels()
            .stream()
            .flatMap(
                p -> p.getChildren()
                    .stream())
            .filter(w -> name.equals(w.getName()))
            .map(w -> (com.cleanroommc.modularui.widgets.slot.ItemSlot) w)
            .findFirst()
            .orElseThrow();
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController
            .windowClick(mc.thePlayer.openContainer.windowId, slot.getSlot().slotNumber, 0, 0, mc.thePlayer);
    }

    private void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper
            .saveScreenshot(new File("."), name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        synchronized (report) {
            report.add("PASS " + message);
        }
        System.out.println("INSECT_QA " + message);
    }

    private void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        if (failure != null) failure.printStackTrace();
        try {
            report.add(failure == null ? "PASS" : "FAIL " + failure);
            Files.write(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), failure == null ? "PASS" : "FAIL");
        } catch (Exception error) {
            error.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(failure == null ? 0 : 1, true);
        }, "insect-staff-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class TooltipPreview extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawHoveringText(
                mc.thePlayer.getHeldItem()
                    .getTooltip(mc.thePlayer, false),
                80,
                80,
                fontRendererObj);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    private static final class ModelPreview extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xff18221d);
            drawCenteredString(fontRendererObj, "Insect Staff / Obsidian & Amethyst", width / 2, 20, 0xb9ed9c);
            ItemStack stack = GTNRItemList.InsectStaff.get(1);
            GL11.glPushMatrix();
            GL11.glTranslatef(width / 2F, height / 2F, 100);
            GL11.glScalef(85, -85, 85);
            MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.ENTITY)
                .renderItem(IItemRenderer.ItemRenderType.ENTITY, stack);
            GL11.glPopMatrix();
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
