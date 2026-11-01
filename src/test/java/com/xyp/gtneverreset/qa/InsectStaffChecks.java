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
import com.xyp.gtneverreset.common.mining.InsectStaffMining;
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
    private int initialEnergy;
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
                initialEnergy = staff.getCapacity(staffStack);
                staffStack.getTagCompound()
                    .setInteger("Energy", initialEnergy);
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
                for (int x = 0; x < 5; x++) player.worldObj.setBlock(x, 10, 0, Blocks.coal_ore);
                player.worldObj.setBlock(5, 10, 0, Blocks.diamond_ore);
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.RANGE, 0);
                ProfileHelper.setInteger(staffStack, InsectStaffConfig.AMOUNT, 3);
                ProfileHelper.setBoolean(staffStack, InsectStaffConfig.PRECISE, true);
                player.setSneaking(true);
                InsectStaffMining.INSTANCE.start(player, staffStack, 0, 10, 0);
                phase = 1;
                wait = 0;
            } else if (phase == 1 && ++wait > 5) {
                require(
                    player.worldObj.getBlock(0, 10, 0) == Blocks.stone
                        && player.worldObj.getBlock(2, 10, 0) == Blocks.stone,
                    "vein backfill");
                require(player.worldObj.getBlock(3, 10, 0) == Blocks.coal_ore, "vein amount limit");
                require(
                    ((InsectStaff) staffStack.getItem()).getEnergyStored(staffStack)
                        == initialEnergy - 3 * ((InsectStaff) staffStack.getItem()).energyPerOperation,
                    "one RF charge per harvested block");
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
                InsectStaffMining.INSTANCE.start(player, staffStack, 3, 10, 0);
                phase = 2;
                wait = 0;
            } else if (phase == 2 && ++wait > 5) {
                require(player.worldObj.getBlock(4, 10, 0) == Blocks.coal_ore, "Forge protection respected");
                require(player.worldObj.getBlock(5, 10, 0) == Blocks.diamond_ore, "precise mode leaves other ores");
                protect = false;
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
            } else if (phase == 4) {
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
                finish(null);
                return;
            }
            if (!(mc.currentScreen instanceof IMuiScreen mui)) return;
            ModularScreen screen = mui.getScreen();
            if (!screen.getMainPanel()
                .getName()
                .equals("insect_staff") || ++frame < 30) return;
            if (screenStage == 1) {
                screenshot("settings-before");
                require(
                    screen.getMainPanel()
                        .getArea().height == 201,
                    "all options in one MUI panel");
                set(screen, InsectStaffConfig.RANGE, 2);
                set(screen, InsectStaffConfig.AMOUNT, 17);
                click(screen.getMainPanel(), InsectStaffConfig.PRECISE);
                click(screen.getMainPanel(), InsectStaffConfig.ENABLED);
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
                screen.getPanelManager()
                    .getOpenPanels()
                    .stream()
                    .filter(
                        p -> p.getName()
                            .equals("insect_staff_filter"))
                    .findFirst()
                    .orElseThrow()
                    .closeIfOpen();
                mc.gameSettings.guiScale = 3;
                mc.currentScreen.setWorldAndResolution(mc, 366, 246);
                phase = 58;
                frame = 0;
            } else if (phase == 58) {
                screenshot("settings-scale-3");
                require(
                    screen.getMainPanel()
                        .getArea().width <= 366
                        && screen.getMainPanel()
                            .getArea().height <= 246,
                    "single page fits GUI scale 3");
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
            staff.getEnergyStored(staffStack)
                == initialEnergy - ((int) (base + 300) + (int) base) * staff.getEnergyPerAttack(),
            "combat charges RF for both attacks");
        target.setDead();
        nearby.setDead();
        staffStack.addEnchantment(net.minecraft.enchantment.Enchantment.sharpness, 3);
        require(staff.getAttackDamage(staffStack) == base + 12, "sharpness included without mixin");
        staffStack.getTagCompound()
            .removeTag("ench");
        staffStack.getTagCompound()
            .setInteger("Energy", initialEnergy);
        ProfileHelper.setInteger(
            staffStack,
            com.brandon3055.draconicevolution.common.lib.References.ATTACK_AOE,
            staff.getBaseUpgradePoints(EnumUpgrade.ATTACK_AOE.index));
    }

    private void set(ModularScreen screen, String key, double value) {
        ((DoubleSyncValue) screen.getSyncManager()
            .getMainPSM()
            .findSyncHandlerNullable(key, 0)).setDoubleValue(value);
    }

    private void click(IWidget parent, String name) {
        for (IWidget widget : parent.getChildren()) {
            if (name.equals(widget.getName()) && widget instanceof ButtonWidget<?>button) {
                button.onMousePressed(0);
                return;
            }
        }
        throw new AssertionError("missing button " + name);
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

    private static final class ModelPreview extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xff18221d);
            drawCenteredString(fontRendererObj, "Insect Staff / Original Draconic Model", width / 2, 20, 0xb9ed9c);
            ItemStack stack = GTNRItemList.InsectStaff.get(1);
            GL11.glPushMatrix();
            GL11.glTranslatef(width / 2F, height / 2F, 100);
            GL11.glScalef(60, -60, 60);
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
