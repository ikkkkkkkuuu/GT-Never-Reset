package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;
import com.xyp.gtneverreset.common.item.items.StableEndCrystalItem;
import com.xyp.gtneverreset.common.machine.basicMachine.EndCrystalResonator;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(modid = "endcrystalqa", name = "End Crystal QA", version = "1", dependencies = "after:gtneverreset")
public final class EndCrystalClientChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched;
    private volatile boolean ready;
    private volatile boolean finished;
    private int ticks;
    private int frames;
    private int clientTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.crystal.client.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (finished) {
                FMLCommonHandler.instance()
                    .exitJava(0, false);
                return;
            }
            require(++clientTicks < 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            if (!launched && mc.theWorld == null && mc.currentScreen != null) {
                launched = true;
                mc.launchIntegratedServer(
                    "crystal-qa-" + System.currentTimeMillis(),
                    "Crystal QA",
                    new WorldSettings(747, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
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
            if (++ticks == 1) checks(player);
            if (ticks == 50) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(2, 11.5, -5, 0, 12);
                ready = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checks(EntityPlayerMP player) {
        var world = player.worldObj;
        world.setWorldTime(6000);
        world.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(2, 11.5, -5, 0, 12);
        StableEndCrystalItem item = (StableEndCrystalItem) GTNRItemList.StableEndCrystal.getItem();
        for (int tier = 1; tier <= 14; tier++) {
            int x = (tier - 1) * 3;
            world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
            tile.setInitialValuesAsNBT(null, (short) (GTNRMachineID.EndCrystalResonatorLV.id + tier - 1));
            tile.setFrontFacing(ForgeDirection.NORTH);
            UUID owner = UUID.randomUUID();
            tile.setOwnerUuid(owner);
            tile.setOwnerName(player.getCommandSenderName());
            EndCrystalResonator machine = (EndCrystalResonator) tile.getMetaTileEntity();
            machine.onFirstTick(tile);
            machine.onPostTick(tile, 20);
            require(!tile.isActive(), "no crystal means no generation");
            player.capabilities.isCreativeMode = false;
            ItemStack stack = GTNRItemList.StableEndCrystal.get(2);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
            require(!item.onItemUse(stack, player, world, x, 10, 0, 2, 0, 0, 0), "side placement rejected");
            world.setBlock(x, 12, 0, Blocks.stone);
            require(!item.onItemUse(stack, player, world, x, 10, 0, 1, 0, 0, 0), "blocked headroom rejected");
            world.setBlockToAir(x, 12, 0);
            require(machine.onRightclick(tile, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "top click places crystal");
            require(stack.stackSize == 1, "survival consumes exactly one item");
            require(!item.onItemUse(stack, player, world, x, 10, 0, 1, 0, 0, 0), "duplicate placement rejected");
            StableEndCrystalEntity crystal = (StableEndCrystalEntity) world
                .getEntitiesWithinAABB(
                    StableEndCrystalEntity.class,
                    AxisAlignedBB.getBoundingBox(x, 11, 0, x + 1, 13, 1))
                .get(0);
            require(!crystal.attackEntityFrom(DamageSource.inFire, 100), "fire does not destroy crystal");
            require(
                !crystal.attackEntityFrom(new DamageSource("qaExplosion").setExplosion(), 100),
                "explosion does not destroy crystal");
            NBTTagCompound savedEntity = new NBTTagCompound();
            crystal.writeToNBTOptional(savedEntity);
            Entity loaded = EntityList.createEntityFromNBT(savedEntity, world);
            require(
                loaded instanceof StableEndCrystalEntity && loaded.posY == 11,
                "entity save/load retains type and position");
            WirelessNetworkManager.setUserEU(owner, BigInteger.ZERO);
            for (int tick = 1; tick <= 7; tick++) machine.onPostTick(tile, tick);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            require(
                saved.getLong("resonatorPendingEU") == 7 * EndCrystalResonator.generationForTier(tier),
                "pending EU persisted");
            machine.loadNBTData(saved);
            machine.onFirstTick(tile);
            for (int tick = 8; tick <= 20; tick++) machine.onPostTick(tile, tick);
            long generation = GTValues.V[tier] * (tier == 1 ? 32L : 128L);
            require(
                WirelessNetworkManager.getUserEU(owner)
                    .equals(BigInteger.valueOf(generation * 20)),
                "exact wireless deposit for tier " + tier);
            tile.disableWorking();
            for (int tick = 21; tick <= 40; tick++) machine.onPostTick(tile, tick);
            require(
                !tile.isActive() && WirelessNetworkManager.getUserEU(owner)
                    .equals(BigInteger.valueOf(generation * 20)),
                "disabled machine stops");
            long recipes = RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .filter(
                    recipe -> recipe.mOutputs.length == 1 && recipe.mOutputs[0] != null
                        && recipe.mOutputs[0].isItemEqual(machine.getStackForm(1)))
                .count();
            require(recipes == 1, "assembler recipe exists for tier " + tier);
            if (tier == 1) {
                int before = world.getEntitiesWithinAABB(EntityItem.class, crystal.boundingBox.expand(1, 1, 1))
                    .size();
                require(
                    crystal.attackEntityFrom(DamageSource.causePlayerDamage(player), 1),
                    "player can recover crystal");
                require(
                    crystal.isDead && world.getBlock(x, 10, 0) == GregTechAPI.sBlockMachines,
                    "recovery leaves machine intact");
                require(
                    world.getEntitiesWithinAABB(EntityItem.class, crystal.boundingBox.expand(1, 1, 1))
                        .size() == before + 1,
                    "one crystal dropped");
                tile.enableWorking();
                machine.onPostTick(tile, 60);
                require(!tile.isActive(), "removed crystal stops generation");
                player.capabilities.isCreativeMode = true;
                require(item.onItemUse(stack, player, world, x, 10, 0, 1, 0, 0, 0), "creative placement succeeds");
                require(stack.stackSize == 1, "creative placement consumes nothing");
            }
            report.append("PASS tier ")
                .append(GTValues.VN[tier])
                .append(": placement, persistence, rate, pause, recipe\n");
        }
        player.capabilities.isCreativeMode = true;
        player.sendPlayerAbilities();
        var end = FMLCommonHandler.instance()
            .getMinecraftServerInstance()
            .worldServerForDimension(1);
        StableEndCrystalEntity endCrystal = new StableEndCrystalEntity(end);
        endCrystal.setPosition(20.5, 100, 20.5);
        end.setBlockToAir(20, 100, 20);
        for (int i = 0; i < 40; i++) endCrystal.onUpdate();
        require(end.isAirBlock(20, 100, 20), "End dimension crystal does not create fire");
        report.append("PASS recovery without explosion; no End fire; custom entity NBT round trip\n");
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen != null || mc.theWorld == null) return;
            require(
                mc.theWorld.loadedEntityList.stream()
                    .anyMatch(e -> e instanceof StableEndCrystalEntity),
                "custom entity synchronized to client");
            if (++frames == 80) ScreenShotHelper.saveScreenshot(
                output(),
                "stable-crystal-world.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (frames == 110) {
                ScreenShotHelper.saveScreenshot(
                    output(),
                    "stable-crystal-animation.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS\n" + report);
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static File output() {
        return new File(System.getProperty("gtnr.crystal.client.qa.output"));
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(new File(output(), "client-result.txt").toPath(), "FAIL\n" + trace);
        } catch (Exception ignored) {}
        finished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
