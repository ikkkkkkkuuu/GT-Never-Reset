package com.xyp.gtneverreset.common.item.items;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.BlockEvent;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.google.common.collect.Multimap;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.entity.EntityUselessWandItem;
import com.xyp.gtneverreset.common.gui.UniversalWandGui;
import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandActions;
import com.xyp.gtneverreset.common.wand.UniversalWandFeature;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandStorage;
import com.xyp.gtneverreset.compat.backhand.UniversalWandBackhandIntegration;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtneverreset.config.UniversalWandConfig;

import appeng.api.implementations.tiles.IWirelessAccessPoint;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;

/** Phase-one universal wand core. Later capabilities are added behind this state boundary. */
@Optional.Interface(iface = "crazypants.enderio.api.teleport.IItemOfTravel", modid = "EnderIO")
public final class ItemUniversalWand extends Item
    implements IGuiHolder<GuiData>, crazypants.enderio.api.teleport.IItemOfTravel {

    public enum EnchantMode {
        SILK,
        FORTUNE
    }

    private static final String NBT_SETTINGS = "gtneverresetUniversalWand";
    private static final String NBT_SCHEMA = "schemaVersion";
    private static final String NBT_MODE = "enchantMode";
    private static final String NBT_BREAK_COOLDOWN = "breakCooldown";
    private static final String NBT_PROTECTION = "protectionMode";
    private static final String NBT_FORCE_BEDROCK = "forceBedrock";
    private static final String NBT_ENHANCED_CHAIN = "enhancedChain";
    private static final String NBT_GT_SMALL_CHAIN = "gtSmallChain";
    private static final String NBT_ENHANCED_CHAIN_RADIUS = "enhancedChainRadius";
    private static final String NBT_MAX_MINING_COUNT = "maxMiningCount";
    private static final String NBT_AE_BINDING = "aeBinding";
    private static final String NBT_AE_PRIORITY = "aeStoragePriority";
    private static final String NBT_INVENTORY_COLLECTION = "collectToInventory";
    private static final String NBT_ORE_BACKFILL = "oreBackfill";
    private static final String NBT_FORCE_KILL = "forceKill";
    private static final String NBT_AOE_SIZE = "aoeSize";
    private static final String NBT_BEHEADING = "beheading";
    private static final String NBT_SPAWN_EGG = "spawnEgg";
    private static final String NBT_TELEPORT = "teleportEnabled";
    private static final String NBT_PLAYER_PROTECTION = "playerProtection";
    private static final String NBT_NO_AGGRO = "noAggro";
    private static final String NBT_FLIGHT_SPEED_OVERRIDE = "flightSpeedOverride";
    private static final String NBT_TIME_ACCELERATION = "timeAcceleration";
    private static final String NBT_ACCELERATION_RATE = "accelerationRate";
    private static final String NBT_ACCELERATION_MODE = "accelerationMode";
    private static final String NBT_ACCELERATION_GT_MODE = "accelerationGtMode";
    public static final int MAX_AOE_SIZE = 27;
    private static final int DEFAULT_ENHANCED_CHAIN_RADIUS = 16;
    private static final int DEFAULT_MAX_MINING_COUNT = 256;
    public static final int MAX_ENHANCED_CHAIN_RADIUS = 128;
    private static final int SETTINGS_SCHEMA = 1;
    private static final int HARVEST_LEVEL = Integer.MAX_VALUE;
    private static final double ATTACK_DAMAGE = 114514D;
    private static final UUID ATTACK_MODIFIER_ID = UUID.fromString("a84cf497-f821-4f11-85e8-baa799190f31");
    private static final String ATTACK_MODIFIER_NAME = "TenkeiTech Universal Wand damage";
    public static final double REACH_BONUS = 8D;

    public static boolean isHeldBy(EntityPlayer player) {
        return player != null && player.getHeldItem() != null
            && player.getHeldItem()
                .getItem() instanceof ItemUniversalWand;
    }

    public static boolean hasReachBonus(EntityPlayer player) {
        if (player == null) return false;
        ItemStack mainhand = Loader.isModLoaded("backhand") ? UniversalWandBackhandIntegration.getMainhandItem(player)
            : player.getHeldItem();
        return mainhand != null && mainhand.getItem() instanceof ItemUniversalWand;
    }

    public ItemUniversalWand() {
        // @formatter:off
        // #tr item.gtneverreset.universal_wand.name
        // # &{mt;t=12}Useless Wand  &{gl;t=12;r=decode}&l&o&m · Not So Useless&r
        // # zh_CN &{mt;t=12}无 用 之 杖  &{gl;t=12;r=decode}&l&o&m · 并 非 无 用&r
        // @formatter:on
        setUnlocalizedName(GTNeverReset.MODID + ".universal_wand");
        setTextureName(GTNeverReset.MODID + ":universal_wand");
        setCreativeTab(CreativeTabs.tabTools);
        setMaxStackSize(1);
        setMaxDamage(0);
        setHarvestLevel("pickaxe", HARVEST_LEVEL);
        setHarvestLevel("axe", HARVEST_LEVEL);
        setHarvestLevel("shovel", HARVEST_LEVEL);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, World world) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity original, ItemStack stack) {
        EntityUselessWandItem dropped = new EntityUselessWandItem(
            world,
            original.posX,
            original.posY,
            original.posZ,
            stack);
        dropped.motionX = original.motionX;
        dropped.motionY = original.motionY;
        dropped.motionZ = original.motionZ;
        if (original instanceof EntityItem item) {
            dropped.delayBeforeCanPickup = item.delayBeforeCanPickup;
            dropped.age = item.age;
            dropped.hoverStart = item.hoverStart;
            dropped.func_145797_a(item.func_145798_i());
            dropped.func_145799_b(item.func_145800_j());
        }
        return dropped;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) GTNeverReset.proxy.useWandTravelIfImmediate(stack, player);
        return stack;
    }

    /** Opens the settings screen after the dedicated key packet has been validated server-side. */
    public static void openSettingsGui(EntityPlayerMP player) {
        if (player == null) return;
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)) return;
        ensureEnchantMode(stack);
        player.sendContainerToPlayer(player.inventoryContainer);
        GuiFactories.playerInventory()
            .openFromMainHand(player);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (player.isSneaking() && isTimeAccelerationEnabled(stack)) {
            if (world.isRemote) return false;
            if (player instanceof EntityPlayerMP mp) UniversalWandAccelerationHandler.use(mp, x, y, z);
            return true;
        }
        if (player.isSneaking() && world.getTileEntity(x, y, z) instanceof IWirelessAccessPoint) {
            // A true client result prevents 1.7.10 from sending the block-use packet.
            if (world.isRemote) return false;
            if (player instanceof EntityPlayerMP mp) UniversalWandStorage.toggleBinding(mp, x, y, z);
            return true;
        }
        return false;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (!world.isRemote && sanitizeDisabledFeatures(stack) && entity instanceof EntityPlayer player)
            player.inventory.markDirty();
        if (!world.isRemote && held && entity instanceof EntityPlayerMP player)
            UniversalWandStorage.validateHeldBinding(player, stack);
        if (!world.isRemote && (!hasEnchantment(stack, Enchantment.looting.effectId) || !hasEnchantment(
            stack,
            getEnchantMode(stack) == EnchantMode.FORTUNE ? Enchantment.fortune.effectId
                : Enchantment.silkTouch.effectId))) {
            applyEnchantMode(stack, getEnchantMode(stack));
        }
    }

    @Override
    public boolean isItemTool(ItemStack stack) {
        return true;
    }

    @Override
    public boolean func_150897_b(Block block) {
        return true;
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return isProtectionMode(stack) ? 0 : HARVEST_LEVEL;
    }

    @Override
    public float func_150893_a(ItemStack stack, Block block) {
        return isProtectionMode(stack) ? 0F : Integer.MAX_VALUE;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
        EntityLivingBase entityLiving) {
        return true;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(stack);
        modifiers.put(
            SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
            new AttributeModifier(ATTACK_MODIFIER_ID, ATTACK_MODIFIER_NAME, ATTACK_DAMAGE, 0).setSaved(false));
        return modifiers;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        // #tr gtneverreset.item.universal_wand.lore_1
        // # Across countless dimensions, its former glory has faded.
        // # zh_CN 跨越无数次元，昔日荣光已逝。
        tooltip
            .add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtneverreset.item.universal_wand.lore_1"));
        // #tr gtneverreset.item.universal_wand.lore_2
        // # A lingering light remains, with countless uses within this staff.
        // # zh_CN 余辉尚堪一用，万般巧用皆藏于此杖。
        tooltip
            .add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtneverreset.item.universal_wand.lore_2"));
        tooltip.add("");
        tooltip.add(EnumChatFormatting.DARK_GRAY + "---- Tools ----");
        addStateTooltip(tooltip, "protection", isProtectionMode(stack), EnumChatFormatting.BLUE);
        tooltip.add(EnumChatFormatting.DARK_GRAY + "---- Mining ----");
        addStateTooltip(tooltip, "enhanced_chain", isEnhancedChain(stack), EnumChatFormatting.GREEN);
        addStateTooltip(tooltip, "ore_backfill", isOreBackfillEnabled(stack), EnumChatFormatting.YELLOW);
        addStateTooltip(tooltip, "gt_small_chain", isGtSmallChainEnabled(stack), EnumChatFormatting.GREEN);
        addStateTooltip(tooltip, "crop_harvest", isCropHarvestEnabled(stack), EnumChatFormatting.GREEN);
        addStateTooltip(tooltip, "crop_sticks", isCropSticksEnabled(stack), EnumChatFormatting.GREEN);
        addStateTooltip(tooltip, "force_bedrock", isForceBedrock(stack), EnumChatFormatting.RED);
        addStateTooltip(tooltip, "collect_items", isInventoryCollection(stack), EnumChatFormatting.AQUA);
        tooltip.add(EnumChatFormatting.DARK_GRAY + "---- Combat ----");
        addStateTooltip(tooltip, "force_kill", isForceKillEnabled(stack), EnumChatFormatting.RED);
        addStateTooltip(tooltip, "beheading", isBeheadingEnabled(stack), EnumChatFormatting.RED);
        addStateTooltip(tooltip, "spawn_egg", isSpawnEggEnabled(stack), EnumChatFormatting.GREEN);
        // #tr gtneverreset.gui.universal_wand.aoe
        // # Area damage
        // # zh_CN 范围伤害
        addStateTooltip(tooltip, "aoe", isAoeDamageEnabled(stack), EnumChatFormatting.RED);
        tooltip.add(EnumChatFormatting.DARK_GRAY + "---- Auxiliary ----");
        addStateTooltip(
            tooltip,
            "player_protection",
            isPlayerProtectionEnabled(stack),
            EnumChatFormatting.LIGHT_PURPLE);
        addStateTooltip(tooltip, "no_aggro", isNoAggroEnabled(stack), EnumChatFormatting.LIGHT_PURPLE);
        addStateTooltip(tooltip, "teleport", isTeleportEnabled(stack), EnumChatFormatting.LIGHT_PURPLE);
        addStateTooltip(tooltip, "flight_speed_override", isFlightSpeedOverrideEnabled(stack), EnumChatFormatting.AQUA);
        addStateTooltip(tooltip, "time_acceleration", isTimeAccelerationEnabled(stack), EnumChatFormatting.AQUA);
    }

    private static void addStateTooltip(List<String> tooltip, String key, boolean enabled, EnumChatFormatting color) {
        tooltip.add(
            color + StatCollector.translateToLocal("gtneverreset.gui.universal_wand." + key)
                + EnumChatFormatting.GRAY
                + ": "
                + (enabled ? EnumChatFormatting.GREEN : EnumChatFormatting.GRAY)
                + StatCollector
                    // #tr gtneverreset.item.universal_wand.enabled
                    // # On
                    // # zh_CN 开启
                    // #tr gtneverreset.item.universal_wand.disabled
                    // # Off
                    // # zh_CN 关闭
                    // #tr gtneverreset.item.universal_wand.mode
                    // # Mode: %s
                    // # zh_CN 模式：%s
                    // #tr gtneverreset.item.universal_wand.silk
                    // # Silk Touch X
                    // # zh_CN 精准采集 X
                    // #tr gtneverreset.item.universal_wand.fortune
                    // # Fortune X
                    // # zh_CN 时运 X
                    // #tr gtneverreset.item.universal_wand.shift_reach
                    // # +8 Reach
                    // # zh_CN +8 触及距离
                    // #tr gtneverreset.item.universal_wand.open
                    // # Press G to configure.
                    // # zh_CN 按 G 打开设置。
                    .translateToLocal("gtneverreset.item.universal_wand." + (enabled ? "enabled" : "disabled")));
        UniversalWandFeature feature = UniversalWandFeature.forControl(key);
        if (feature != null && !feature.allowed()) tooltip.add(disabledFeatureTooltip());
    }

    public static String disabledFeatureTooltip() {
        // #tr gtneverreset.gui.universal_wand.config_disabled
        // # Disabled by config
        // # zh_CN 已禁用
        return EnumChatFormatting.RED
            + StatCollector.translateToLocal("gtneverreset.gui.universal_wand.config_disabled");
    }

    @Override
    public ModularScreen createScreen(GuiData data, ModularPanel panel) {
        return new ModularScreen(GTNeverReset.MODID, panel);
    }

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        ItemStack expected = data instanceof PlayerInventoryGuiData inventoryData ? inventoryData.getUsedItemStack()
            : data.getPlayer()
                .getHeldItem();
        if (data.isClient()) settings.useTheme(ScreenThemeConfig.universalWand.themeId);
        settings.canInteractWith(
            player -> expected != null && player.getHeldItem() == expected
                && expected.getItem() instanceof ItemUniversalWand);
        settings.getRecipeViewerSettings()
            .disable();
        return UniversalWandGui.build(data, syncManager);
    }

    public static EnchantMode getEnchantMode(ItemStack stack) {
        NBTTagCompound settings = getSettings(stack);
        if (settings.getInteger(NBT_MODE) == EnchantMode.FORTUNE.ordinal()) return EnchantMode.FORTUNE;
        return EnchantMode.SILK;
    }

    public static boolean setEnchantMode(EntityPlayer player, ItemStack expectedStack, EnchantMode mode) {
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setInteger(NBT_MODE, mode.ordinal());
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        applyEnchantMode(expectedStack, mode);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static int getBreakCooldown(ItemStack stack) {
        NBTTagCompound settings = getSettings(stack);
        return Math.max(0, Math.min(20, settings.getInteger(NBT_BREAK_COOLDOWN)));
    }

    public static boolean forceBedrock(EntityPlayerMP player, int targetX, int targetY, int targetZ) {
        if (player == null || player.isDead || player.worldObj.isRemote) return false;
        ItemStack stack = player.getHeldItem();
        if (!(stack != null && stack.getItem() instanceof ItemUniversalWand) || !isForceBedrock(stack)
            || isProtectionMode(stack)) return false;
        int x = targetX;
        int y = targetY;
        int z = targetZ;
        if (!player.worldObj.blockExists(x, y, z) || !UniversalWandActions.isTarget(player, x, y, z)) return false;
        if (player.worldObj.getBlock(x, y, z) != Blocks.bedrock || !player.canPlayerEdit(x, y, z, 1, stack))
            return false;
        if (MinecraftServer.getServer()
            .isBlockProtected((WorldServer) player.worldObj, x, y, z, player)) return false;
        BlockEvent.BreakEvent event;
        try (UniversalWandMiningHandler.ForcedBreakScope ignored = UniversalWandMiningHandler.beginForcedBreak()) {
            event = ForgeHooks
                .onBlockBreakEvent(player.worldObj, player.theItemInWorldManager.getGameType(), player, x, y, z);
        }
        if (event.isCanceled() || player.getHeldItem() != stack
            || isProtectionMode(stack)
            || !isForceBedrock(stack)
            || player.worldObj.getBlock(x, y, z) != Blocks.bedrock) return false;
        if (!player.worldObj.setBlockToAir(x, y, z) || player.worldObj.getBlock(x, y, z) == Blocks.bedrock)
            return false;
        if (!player.capabilities.isCreativeMode) {
            UniversalWandMiningHandler.distributeBedrock(player, stack, x, y, z);
        }
        applyMiningExhaustion(player);
        return true;
    }

    public static boolean isProtectionMode(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_PROTECTION);
    }

    public static boolean isForceKillEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_FORCE_KILL);
    }

    public static boolean isBeheadingEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_BEHEADING);
    }

    public static boolean isSpawnEggEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_SPAWN_EGG);
    }

    public static boolean isTeleportEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_TELEPORT);
    }

    public static boolean isTimeAccelerationEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_TIME_ACCELERATION);
    }

    public static int getAccelerationRate(ItemStack stack) {
        NBTTagCompound settings = getSettings(stack);
        int rate = settings.hasKey(NBT_ACCELERATION_RATE) ? settings.getInteger(NBT_ACCELERATION_RATE) : 2;
        if (rate == 0) return 0;
        for (int allowed : UniversalWandConfig.accelerationRates()) if (allowed == rate) return rate;
        return UniversalWandConfig.accelerationRates()[0];
    }

    public static int getAccelerationMode(ItemStack stack) {
        return Math.max(0, Math.min(2, getSettings(stack).getInteger(NBT_ACCELERATION_MODE)));
    }

    public static boolean isAccelerationGtMode(ItemStack stack) {
        NBTTagCompound settings = getSettings(stack);
        return !settings.hasKey(NBT_ACCELERATION_GT_MODE) || settings.getBoolean(NBT_ACCELERATION_GT_MODE);
    }

    public static boolean setAccelerationGtMode(EntityPlayer player, ItemStack expected, boolean gtMode) {
        return setBooleanSetting(player, expected, NBT_ACCELERATION_GT_MODE, gtMode);
    }

    public static boolean setAccelerationRate(EntityPlayer player, ItemStack expected, int rate) {
        if (rate != 0 && Arrays.stream(UniversalWandConfig.accelerationRates())
            .noneMatch(value -> value == rate)) return false;
        return setAccelerationSetting(player, expected, NBT_ACCELERATION_RATE, rate);
    }

    public static boolean setAccelerationMode(EntityPlayer player, ItemStack expected, int mode) {
        return mode >= 0 && mode <= 2 && setAccelerationSetting(player, expected, NBT_ACCELERATION_MODE, mode);
    }

    private static boolean setAccelerationSetting(EntityPlayer player, ItemStack expected, String key, int value) {
        if (!UniversalWandFeature.ACCELERATION.allowed()) return false;
        if (player == null || player.worldObj.isRemote
            || expected == null
            || player.getHeldItem() != expected
            || !(expected.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expected.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expected);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setInteger(key, value);
        root.setTag(NBT_SETTINGS, settings);
        expected.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP mp) mp.sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setTimeAccelerationEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        if (!setBooleanSetting(player, expected, NBT_TIME_ACCELERATION, enabled)) return false;
        if (!enabled && !player.worldObj.isRemote) UniversalWandAccelerationHandler.cancel(player.getUniqueID());
        return true;
    }

    public static boolean isPlayerProtectionEnabled(ItemStack stack) {
        if (!UniversalWandFeature.PLAYER_PROTECTION.allowed()) return false;
        NBTTagCompound settings = readProtectionSettings(stack);
        return !settings.hasKey(NBT_PLAYER_PROTECTION) || settings.getBoolean(NBT_PLAYER_PROTECTION);
    }

    public static boolean isNoAggroEnabled(ItemStack stack) {
        return UniversalWandFeature.NO_AGGRO.allowed() && readProtectionSettings(stack).getBoolean(NBT_NO_AGGRO);
    }

    public static boolean isFlightSpeedOverrideEnabled(ItemStack stack) {
        if (!UniversalWandFeature.FLIGHT.allowed()) return false;
        NBTTagCompound settings = readProtectionSettings(stack);
        return !settings.hasKey(NBT_FLIGHT_SPEED_OVERRIDE) || settings.getBoolean(NBT_FLIGHT_SPEED_OVERRIDE);
    }

    public static boolean setFlightSpeedOverrideEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, NBT_FLIGHT_SPEED_OVERRIDE, enabled);
    }

    private static NBTTagCompound readProtectionSettings(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null) return new NBTTagCompound();
        NBTTagCompound settings = stack.getTagCompound()
            .getCompoundTag(NBT_SETTINGS);
        return settings.getInteger(NBT_SCHEMA) == SETTINGS_SCHEMA ? settings : new NBTTagCompound();
    }

    public static boolean setPlayerProtectionEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, NBT_PLAYER_PROTECTION, enabled);
    }

    public static boolean setNoAggroEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, NBT_NO_AGGRO, enabled);
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public boolean isActive(EntityPlayer player, ItemStack stack) {
        return player != null && stack != null && stack.getItem() == this && isTeleportEnabled(stack);
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public int canExtractInternal(ItemStack stack, int power) {
        return Math.max(0, power);
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public void extractInternal(ItemStack stack, int power) {}

    @Override
    @Optional.Method(modid = "EnderIO")
    public int receiveEnergy(ItemStack stack, int amount, boolean simulate) {
        return 0;
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public int extractEnergy(ItemStack stack, int amount, boolean simulate) {
        return 0;
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public int getEnergyStored(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    @Optional.Method(modid = "EnderIO")
    public int getMaxEnergyStored(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    public static boolean setTeleportEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setCombatSetting(player, expected, NBT_TELEPORT, enabled);
    }

    public static boolean setBeheadingEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setCombatSetting(player, expected, NBT_BEHEADING, enabled);
    }

    public static boolean setSpawnEggEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setCombatSetting(player, expected, NBT_SPAWN_EGG, enabled);
    }

    public static boolean isAoeDamageEnabled(ItemStack stack) {
        return getAoeSize(stack) > 1;
    }

    public static int getAoeSize(ItemStack stack) {
        NBTTagCompound settings = getSettings(stack);
        return settings.hasKey(NBT_AOE_SIZE, Constants.NBT.TAG_INT)
            ? normalizeAoeSize(settings.getInteger(NBT_AOE_SIZE))
            : 1;
    }

    private static int normalizeAoeSize(int size) {
        int bounded = Math.max(1, Math.min(MAX_AOE_SIZE, size));
        return bounded % 2 == 0 ? bounded - 1 : bounded;
    }

    public static boolean setAoeSize(EntityPlayer player, ItemStack expected, int size) {
        if (!UniversalWandFeature.AREA_ATTACK.allowed()) return false;
        if (player == null || player.worldObj.isRemote
            || expected == null
            || player.getHeldItem() != expected
            || !(expected.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expected.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expected);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setInteger(NBT_AOE_SIZE, normalizeAoeSize(size));
        root.setTag(NBT_SETTINGS, settings);
        expected.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP mp) mp.sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setForceKillEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setCombatSetting(player, expected, NBT_FORCE_KILL, enabled);
    }

    private static boolean setCombatSetting(EntityPlayer player, ItemStack expected, String key, boolean enabled) {
        if (!UniversalWandFeature.canChange(key)) return false;
        if (player == null || player.worldObj.isRemote
            || player.getHeldItem() != expected
            || expected == null
            || !(expected.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expected.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expected);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setBoolean(key, enabled);
        root.setTag(NBT_SETTINGS, settings);
        expected.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP mp) mp.sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean isForceBedrock(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_FORCE_BEDROCK);
    }

    public static boolean isEnhancedChain(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_ENHANCED_CHAIN);
    }

    public static boolean isGtSmallChainEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_GT_SMALL_CHAIN);
    }

    public static boolean isAePriority(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_AE_PRIORITY);
    }

    public static boolean isInventoryCollection(ItemStack stack) {
        return getSettings(stack).getBoolean(NBT_INVENTORY_COLLECTION);
    }

    public static boolean isOreBackfillEnabled(ItemStack stack) {
        return UniversalWandFeature.CHAIN.allowed() && getSettings(stack).getBoolean(NBT_ORE_BACKFILL);
    }

    public static boolean setOreBackfillEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, NBT_ORE_BACKFILL, enabled);
    }

    public static boolean isCropHarvestEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean("cropHarvest");
    }

    public static boolean setCropHarvestEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, "cropHarvest", enabled);
    }

    public static boolean isCropSticksEnabled(ItemStack stack) {
        return getSettings(stack).getBoolean("cropSticks");
    }

    public static boolean setCropSticksEnabled(EntityPlayer player, ItemStack expected, boolean enabled) {
        return setBooleanSetting(player, expected, "cropSticks", enabled);
    }

    public static boolean setAePriority(EntityPlayer player, ItemStack expected, boolean enabled) {
        if (!setBooleanSetting(player, expected, NBT_AE_PRIORITY, enabled)) return false;
        if (enabled) setBooleanSetting(player, expected, NBT_INVENTORY_COLLECTION, true);
        return true;
    }

    public static boolean setInventoryCollection(EntityPlayer player, ItemStack expected, boolean enabled) {
        if (!enabled && isAePriority(expected)) setBooleanSetting(player, expected, NBT_AE_PRIORITY, false);
        return setBooleanSetting(player, expected, NBT_INVENTORY_COLLECTION, enabled);
    }

    public static NBTTagCompound getAeBinding(ItemStack stack) {
        return (NBTTagCompound) getSettings(stack).getCompoundTag(NBT_AE_BINDING)
            .copy();
    }

    public static boolean setAeBinding(EntityPlayer player, ItemStack expected, NBTTagCompound binding) {
        if (player == null || player.worldObj.isRemote
            || player.getHeldItem() != expected
            || expected == null
            || !(expected.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expected.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expected);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        if (binding == null) settings.removeTag(NBT_AE_BINDING);
        else settings.setTag(NBT_AE_BINDING, binding.copy());
        root.setTag(NBT_SETTINGS, settings);
        expected.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP mp) mp.sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setGtSmallChainEnabled(EntityPlayer player, ItemStack expectedStack, boolean enabled) {
        return setBooleanSetting(player, expectedStack, NBT_GT_SMALL_CHAIN, enabled);
    }

    public static int getEnhancedChainRadius(ItemStack stack) {
        int value = getSettings(stack).getInteger(NBT_ENHANCED_CHAIN_RADIUS);
        return value <= 0 ? DEFAULT_ENHANCED_CHAIN_RADIUS : Math.max(1, Math.min(MAX_ENHANCED_CHAIN_RADIUS, value));
    }

    public static int getMaxMiningCount(ItemStack stack) {
        int value = getSettings(stack).getInteger(NBT_MAX_MINING_COUNT);
        return value <= 0 ? DEFAULT_MAX_MINING_COUNT : value;
    }

    public static void applyMiningExhaustion(EntityPlayer player) {
        if (!player.worldObj.isRemote && !player.capabilities.isCreativeMode) {
            player.addExhaustion((float) UniversalWandConfig.universalWandMiningExhaustion);
        }
    }

    public static boolean setEnhancedChainRadius(EntityPlayer player, ItemStack expectedStack, int radius) {
        return setIntegerSetting(
            player,
            expectedStack,
            NBT_ENHANCED_CHAIN_RADIUS,
            Math.max(1, Math.min(MAX_ENHANCED_CHAIN_RADIUS, radius)));
    }

    public static boolean setMaxMiningCount(EntityPlayer player, ItemStack expectedStack, int count) {
        return setIntegerSetting(
            player,
            expectedStack,
            NBT_MAX_MINING_COUNT,
            Math.max(1, Math.min(Integer.MAX_VALUE, count)));
    }

    private static boolean setIntegerSetting(EntityPlayer player, ItemStack expectedStack, String key, int value) {
        if (!UniversalWandFeature.canChange(key)) return false;
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setInteger(key, value);
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setProtectionMode(EntityPlayer player, ItemStack expectedStack, boolean enabled) {
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setBoolean(NBT_PROTECTION, enabled);
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setForceBedrock(EntityPlayer player, ItemStack expectedStack, boolean enabled) {
        if (!UniversalWandFeature.BEDROCK.allowed()) return false;
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setBoolean(NBT_FORCE_BEDROCK, enabled);
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setEnhancedChain(EntityPlayer player, ItemStack expectedStack, boolean enabled) {
        return setBooleanSetting(player, expectedStack, NBT_ENHANCED_CHAIN, enabled);
    }

    private static boolean setBooleanSetting(EntityPlayer player, ItemStack expectedStack, String key,
        boolean enabled) {
        if (!UniversalWandFeature.canChange(key)) return false;
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setBoolean(key, enabled);
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    public static boolean setBreakCooldown(EntityPlayer player, ItemStack expectedStack, int cooldown) {
        if (player == null || expectedStack == null
            || player.getHeldItem() != expectedStack
            || !(expectedStack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound root = expectedStack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        NBTTagCompound settings = getSettings(expectedStack);
        settings.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        settings.setInteger(NBT_BREAK_COOLDOWN, Math.max(0, Math.min(20, cooldown)));
        root.setTag(NBT_SETTINGS, settings);
        expectedStack.setTagCompound(root);
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP)
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        return true;
    }

    private static void ensureEnchantMode(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)) return;
        applyEnchantMode(stack, getEnchantMode(stack));
    }

    private static void applyEnchantMode(ItemStack stack, EnchantMode mode) {
        NBTTagList enchantments = stack.getEnchantmentTagList();
        if (enchantments != null) {
            for (int index = enchantments.tagCount() - 1; index >= 0; index--) {
                int id = enchantments.getCompoundTagAt(index)
                    .getShort("id");
                if (id == Enchantment.silkTouch.effectId || id == Enchantment.fortune.effectId)
                    enchantments.removeTag(index);
            }
        }
        stack.addEnchantment(mode == EnchantMode.FORTUNE ? Enchantment.fortune : Enchantment.silkTouch, 10);
        if (!hasEnchantment(stack, Enchantment.looting.effectId)) stack.addEnchantment(Enchantment.looting, 10);
    }

    private static boolean hasEnchantment(ItemStack stack, int id) {
        NBTTagList enchantments = stack.getEnchantmentTagList();
        if (enchantments == null) return false;
        for (int index = 0; index < enchantments.tagCount(); index++) {
            if (enchantments.getCompoundTagAt(index)
                .getShort("id") == id) return true;
        }
        return false;
    }

    private static NBTTagCompound getSettings(ItemStack stack) {
        NBTTagCompound settings = readProtectionSettings(stack);
        settings = (NBTTagCompound) settings.copy();
        UniversalWandFeature.restrict(settings);
        return settings;
    }

    /** Persist current server restrictions when tools enter a player's inventory or are loaded. */
    public static boolean sanitizeDisabledFeatures(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)) return false;
        NBTTagCompound previous = readProtectionSettings(stack);
        NBTTagCompound restricted = getSettings(stack);
        if (previous.equals(restricted)) return false;
        restricted.setInteger(NBT_SCHEMA, SETTINGS_SCHEMA);
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        root.setTag(NBT_SETTINGS, restricted);
        stack.setTagCompound(root);
        return true;
    }
}
