package com.xyp.gtneverreset.common.item.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.event.world.BlockEvent;

import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.entity.EntityDragonProjectile;
import com.brandon3055.draconicevolution.common.items.tools.DraconicDistructionStaff;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.MiningTool;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem.ProfileHelper;
import com.brandon3055.draconicevolution.common.utils.IHudDisplayItem;
import com.brandon3055.draconicevolution.common.utils.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.client.InsectStaffInputHandler;
import com.xyp.gtneverreset.common.gui.InsectStaffConfig;
import com.xyp.gtneverreset.common.gui.InsectStaffGui;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.api.teleport.IItemOfTravel;

public final class InsectStaff extends Item
    implements IItemOfTravel, IGuiHolder<PlayerInventoryGuiData>, IUpgradableItem, IHudDisplayItem {

    private static final UUID SPRINT_SPEED_ID = UUID.fromString("58f0b874-b18f-409c-a4a2-f7ed8b58ba75");
    private final MiningTool areaMining = new FreeMiningTool();
    private final Map<EntityPlayer, FlightSpeed> flightSpeeds = new WeakHashMap<>();

    public InsectStaff() {
        setUnlocalizedName("gtneverreset.insect_staff");
        setMaxStackSize(1);
        setHarvestLevel("pickaxe", 10);
        setHarvestLevel("shovel", 10);
        setHarvestLevel("axe", 10);
        GameRegistry.registerItem(this, "insect_staff");
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        GTNRItemList.InsectStaff.set(new ItemStack(this));
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        return InsectStaffGui.build(data, sync, settings);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return new ModularScreen("gtneverreset", panel);
    }

    @Override
    public String getUnlocalizedName() {
        // #tr item.gtneverreset.insect_staff.name
        // # Insect Staff
        // # zh_CN 虫之权杖
        return "item.gtneverreset.insect_staff";
    }

    @Override
    public int getBaseUpgradePoints(int index) {
        return original().getMaxUpgradePoints(index);
    }

    @Override
    public int getUpgradeCap(ItemStack stack) {
        return 0;
    }

    public List<ItemConfigField> getFields(ItemStack stack, int slot) {
        initializeSettings(stack);
        return InsectStaffConfig.fields(original().getFields(stack, slot), stack, slot);
    }

    public void initializeSettings(ItemStack stack) {
        NBTTagCompound tags = ItemNBTHelper.getCompound(stack);
        NBTTagList profiles = tags.getTagList("ConfigProfiles", 10);
        int selected = tags.getInteger("ConfigProfile");
        if (profiles.tagCount() != 1 || selected != 0) {
            int index = selected >= 0 && selected < profiles.tagCount() ? selected : 0;
            NBTTagCompound active = (NBTTagCompound) profiles.getCompoundTagAt(index)
                .copy();
            NBTTagList settings = new NBTTagList();
            settings.appendTag(active);
            tags.setTag("ConfigProfiles", settings);
            tags.setInteger("ConfigProfile", 0);
            for (int i = 0; i < 5; i++) tags.removeTag("ProfileName" + i);
        }
        NBTTagCompound profile = ProfileHelper.getProfileCompound(stack);
        if (!profile.hasKey(References.DIG_AOE))
            profile.setInteger(References.DIG_AOE, getBaseUpgradePoints(EnumUpgrade.DIG_AOE.index));
        if (!profile.hasKey(References.DIG_DEPTH))
            profile.setInteger(References.DIG_DEPTH, getBaseUpgradePoints(EnumUpgrade.DIG_DEPTH.index));
        if (!profile.hasKey(References.ATTACK_AOE))
            profile.setInteger(References.ATTACK_AOE, getBaseUpgradePoints(EnumUpgrade.ATTACK_AOE.index));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        super.onUpdate(stack, world, entity, slot, held);
        if (!world.isRemote) initializeSettings(stack);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void restoreFlightSpeed(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !event.player.worldObj.isRemote) return;
        FlightSpeed previous = flightSpeeds.remove(event.player);
        // Restore before armor recalculates its speed, preserving any intervening external change.
        if (previous != null && Float.compare(event.player.capabilities.getFlySpeed(), previous.applied) == 0) {
            event.player.capabilities.setFlySpeed(previous.base);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void applyHeldSpeed(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        ItemStack held = player.getHeldItem();
        boolean active = player.isEntityAlive() && held != null && held.getItem() == this;
        double sprint = active && player.isSprinting() && !player.capabilities.isFlying
            ? InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.SPRINT_SPEED) - 1D
            : 0D;
        var attribute = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        AttributeModifier modifier = attribute.getModifier(SPRINT_SPEED_ID);
        if (modifier != null && modifier.getAmount() != sprint) {
            attribute.removeModifier(modifier);
            modifier = null;
        }
        if (sprint > 0 && modifier == null) {
            attribute.applyModifier(
                new AttributeModifier(SPRINT_SPEED_ID, "Insect staff sprint", sprint, 2).setSaved(false));
        }
        // Like DE armor, apply flight speed on the movement client; keep ability packets at the base speed.
        if (player.worldObj.isRemote && active && player.capabilities.isFlying) {
            float multiplier = InsectStaffConfig.speedMultiplier(held, InsectStaffConfig.FLIGHT_SPEED);
            if (multiplier > 1F) {
                float base = player.capabilities.getFlySpeed();
                float applied = base * multiplier;
                flightSpeeds.put(player, new FlightSpeed(base, applied));
                player.capabilities.setFlySpeed(applied);
            }
        }
    }

    public float getAttackDamage(ItemStack stack) {
        return ModItems.CHAOTIC.getDamageVsEntity() + getBaseUpgradePoints(EnumUpgrade.ATTACK_DAMAGE.index) * 5F
            + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 4F;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity target) {
        if (player.worldObj.isRemote) return true;
        initializeSettings(stack);
        target.hurtResistantTime = 0;
        attack(stack, player, target, true);
        int range = ProfileHelper.getInteger(stack, References.ATTACK_AOE, 0);
        if (range > 0) {
            AxisAlignedBB area = AxisAlignedBB
                .getBoundingBox(
                    target.posX - range,
                    target.posY - range,
                    target.posZ - range,
                    target.posX + range,
                    target.posY + range,
                    target.posZ + range)
                .expand(1, 1, 1);
            for (Object candidate : player.worldObj.getEntitiesWithinAABBExcludingEntity(player, area)) {
                if (candidate != target
                    && (candidate instanceof EntityLivingBase || candidate instanceof EntityDragonProjectile)) {
                    attack(stack, player, (Entity) candidate, false);
                }
            }
        }
        return true;
    }

    private void attack(ItemStack stack, EntityPlayer player, Entity target, boolean healthBonus) {
        float damage = getAttackDamage(stack);
        if (target instanceof EntityLivingBase living) {
            if (living.isEntityUndead())
                damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.smite.effectId, stack) * 6F;
            if (target instanceof EntitySpider)
                damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.baneOfArthropods.effectId, stack) * 6F;
            if (healthBonus) damage += living.getHealth() * 0.3F;
        } else if (healthBonus && target instanceof EntityDragonPart part
            && part.entityDragonObj instanceof EntityLivingBase dragon) {
                damage += dragon.getHealth() * 0.3F;
            }
        target.attackEntityFrom(DamageSource.causePlayerDamage(player), healthBonus ? Math.max(1, damage) : damage);
        int fire = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
        if (fire > 0) target.setFire(fire * 15);
        if (target instanceof EntityLivingBase living) {
            double dx = player.posX - living.posX;
            double dz = player.posZ - living.posZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.0001) {
                dx = 0.01;
                dz = 0;
                length = 0.01;
            }
            if (living.getRNG()
                .nextDouble()
                >= living.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                    .getAttributeValue()) {
                float force = 0.1F + EnchantmentHelper.getKnockbackModifier(player, living) * 0.4F;
                living.isAirBorne = true;
                living.motionX = living.motionX / 2 - dx / length * force;
                living.motionY = Math.min(0.4, living.motionY / 2 + force);
                living.motionZ = living.motionZ / 2 - dz / length * force;
            }
            if (!healthBonus) living.attackTime = 0;
        }
    }

    private static DraconicDistructionStaff original() {
        return (DraconicDistructionStaff) ModItems.draconicDestructionStaff;
    }

    @Override
    public int getMaxUpgradePoints(int index) {
        return original().getMaxUpgradePoints(index);
    }

    @Override
    public int getMaxUpgradePoints(int index, ItemStack stack) {
        return getMaxUpgradePoints(index);
    }

    @Override
    public int getMaxTier(ItemStack stack) {
        return original().getMaxTier(stack);
    }

    @Override
    public List<EnumUpgrade> getUpgrades(ItemStack stack) {
        List<EnumUpgrade> upgrades = original().getUpgrades(stack);
        upgrades.remove(EnumUpgrade.RF_CAPACITY);
        return upgrades;
    }

    @Override
    public List<String> getUpgradeStats(ItemStack stack) {
        return getDisplayData(stack);
    }

    // Ender IO's travel interface also requires RF methods; this item has no RF storage or transfer.
    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return 0;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        return 0;
    }

    @Override
    public int receiveEnergy(ItemStack stack, int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(ItemStack stack, int amount, boolean simulate) {
        return 0;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        return original().createEntity(world, location, stack);
    }

    @Override
    public boolean func_150897_b(Block block) {
        return original().func_150897_b(block);
    }

    @Override
    public float func_150893_a(ItemStack stack, Block block) {
        return original().func_150893_a(stack, block);
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        return areaMining.getDigSpeed(stack, block, meta);
    }

    @Override
    public int getItemEnchantability() {
        return original().getItemEnchantability();
    }

    public boolean isEnchantValid(Enchantment enchantment) {
        return original().isEnchantValid(enchantment);
    }

    public Map<Block, Integer> getObliterationList(ItemStack stack) {
        return original().getObliterationList(stack);
    }

    @SideOnly(Side.CLIENT)
    public void tweakRender(IItemRenderer.ItemRenderType type) {
        original().tweakRender(type);
    }

    @Override
    public List<String> getDisplayData(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        // #tr tooltip.gtneverreset.insect_staff.damage
        // # Attack damage: %s
        // # zh_CN 攻击伤害：%s
        lines.add(
            EnumChatFormatting.BLUE + StatCollector.translateToLocalFormatted(
                "tooltip.gtneverreset.insect_staff.damage",
                NumberFormatUtil.formatNumber(getAttackDamage(stack))));
        // #tr tooltip.gtneverreset.insect_staff.health_damage
        // # Adds 30% of the target's current health as damage
        // # zh_CN 附加目标当前生命值 30% 的伤害
        lines.add(
            EnumChatFormatting.BLUE
                + StatCollector.translateToLocal("tooltip.gtneverreset.insect_staff.health_damage"));
        return lines;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
        initializeSettings(stack);
        VeinMiningPickaxe pickaxe = veinMiningPickaxe();
        if (pickaxe.isHarvesting(player)) return false;
        if (player.isSneaking() && InsectStaffConfig.veinEnabled(stack)) {
            if (player instanceof EntityPlayerMP serverPlayer) {
                World world = player.worldObj;
                pickaxe.clearConnectedBlocks(
                    serverPlayer,
                    stack,
                    x,
                    y,
                    z,
                    world.getBlock(x, y, z),
                    world.getBlockMetadata(x, y, z),
                    InsectStaffConfig.range(stack),
                    InsectStaffConfig.amount(stack),
                    ProfileHelper.getBoolean(stack, InsectStaffConfig.PRECISE, false),
                    ProfileHelper.getBoolean(stack, References.OBLITERATE, false) ? getObliterationList(stack)
                        : Collections.emptyMap());
            }
            return true;
        }
        return areaMining.onBlockStartBreak(stack, x, y, z, player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.world.isRemote) return;
        ItemStack stack = event.getPlayer()
            .getHeldItem();
        if (stack != null && stack.getItem() == this
            && InsectStaffConfig.veinEnabled(stack)
            && ProfileHelper.getBoolean(stack, InsectStaffConfig.BACKFILL, true)) {
            veinMiningPickaxe().queueOreBackfill(event.world, event.x, event.y, event.z);
        }
    }

    private static VeinMiningPickaxe veinMiningPickaxe() {
        return (VeinMiningPickaxe) GTNRItemList.VeinMiningPickaxe.getItem();
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
        EntityLivingBase entity) {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (isActive(player, stack)) return EnderIO.itemTeleportStaff.onItemRightClick(stack, world, player);
        return super.onItemRightClick(stack, world, player);
    }

    @Override
    public boolean isActive(EntityPlayer player, ItemStack stack) {
        return player != null && stack != null
            && stack.getItem() == this
            && ProfileHelper.getBoolean(stack, InsectStaffConfig.TELEPORT, true);
    }

    @Override
    public void extractInternal(ItemStack stack, int power) {}

    @Override
    public int canExtractInternal(ItemStack stack, int power) {
        return Math.max(0, power);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon("draconicevolution:draconicDistructionStaff");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(this);
        initializeSettings(stack);
        list.add(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.addAll(getDisplayData(stack));
        int key = InsectStaffInputHandler.CONFIGURE.getKeyCode();
        if (key == 0) {
            // #tr tooltip.gtneverreset.insect_staff.unbound
            // # Bind Configure Insect Staff in Controls
            // # zh_CN 请在控制设置中绑定「配置虫之权杖」
            list.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.gtneverreset.insect_staff.unbound"));
        } else {
            // #tr tooltip.gtneverreset.insect_staff.configure
            // # Press %s to configure
            // # zh_CN 按 %s 打开配置
            list.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                    "tooltip.gtneverreset.insect_staff.configure",
                    GameSettings.getKeyDisplayString(key)));
        }
        // #tr tooltip.gtneverreset.insect_staff.mining
        // # Sneak while mining to mine a vein
        // # zh_CN 按住潜行键挖掘，可连锁开采矿脉
        list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.gtneverreset.insect_staff.mining"));
        // #tr tooltip.gtneverreset.insect_staff.teleport
        // # Right-click to use advanced teleport
        // # zh_CN 右键使用高级传送
        list.add(
            EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.gtneverreset.insect_staff.teleport"));
    }

    private static final class FlightSpeed {

        private final float base;
        private final float applied;

        private FlightSpeed(float base, float applied) {
            this.base = base;
            this.applied = applied;
        }
    }

    private static final class FreeMiningTool extends MiningTool {

        private FreeMiningTool() {
            super(ModItems.CHAOTIC);
            setHarvestLevel("pickaxe", 10);
            setHarvestLevel("shovel", 10);
            setHarvestLevel("axe", 10);
            energyPerOperation = 0;
        }

        @Override
        public int getEnergyStored(ItemStack stack) {
            return 0;
        }

        @Override
        public int extractEnergy(ItemStack stack, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public float getEfficiency(ItemStack stack) {
            return original().getEfficiency(stack);
        }

        @Override
        public float func_150893_a(ItemStack stack, Block block) {
            return getEfficiency(stack);
        }

        @Override
        public int getUpgradeCap(ItemStack stack) {
            return 0;
        }

        @Override
        public int getMaxTier(ItemStack stack) {
            return original().getMaxTier(stack);
        }

        @Override
        public int getMaxUpgradePoints(int index) {
            return original().getMaxUpgradePoints(index);
        }

        @Override
        public int getBaseUpgradePoints(int index) {
            return original().getMaxUpgradePoints(index);
        }
    }

}
