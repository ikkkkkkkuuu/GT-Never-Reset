package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.api.SeedPlantingResult;
import com.gtnewhorizon.cropsnh.compatibility.NEI.NEICropsNHCropHandler;
import com.gtnewhorizon.cropsnh.compatibility.NEI.NEICropsNHCropstickBreedingHandler;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.MutationRegistry;
import com.gtnewhorizon.cropsnh.init.CropsNHBlocks;
import com.gtnewhorizon.cropsnh.recipes.CropsNHGTRecipeMaps;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingChance;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingPlanner;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingRecipeRules;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingSubstrate;
import com.xyp.gtneverreset.compat.cropsnh.NetherStarFlowerCrop;
import com.xyp.gtneverreset.compat.cropsnh.SunnariumFlowerCrop;
import com.xyp.gtneverreset.config.CropBreederConfig;
import com.xyp.gtneverreset.config.CropsNHConfig;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.Recipe.RecipeId;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.recipe.RecipeMap;

@Mod(modid = "sunnariumflowerqa", name = "Sunnarium Flower QA", version = "1", dependencies = "after:gtneverreset")
public final class SunnariumFlowerChecks {

    private final StringBuilder report = new StringBuilder();
    private volatile boolean ready;
    private volatile Throwable failure;
    private boolean launched, checked, finished;
    private int ticks, frames, stage;
    private ICropCard crop;

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
        checked = true;
        try {
            CropsNHConfig.enableInstantGrowth = false;
            CropsNHConfig.enableFastGrowthTickRate = false;
            crop = CropRegistry.instance.get(SunnariumFlowerCrop.ID);
            require(crop != null, "crop registered in the GTNR namespace");
            ItemStack product = Materials.Sunnarium.getDust(1);
            ItemStack substrate = Materials.Sunnarium.getBlocks(1);
            require(product != null && substrate != null, "Sunnarium dust and block exist");
            require(crop.getTier() == 16, "crop uses the highest existing crop tier");
            require(CropBreedingRecipeRules.breedingGrade(crop) == 5, "highest breeding grade is 5");
            require(
                CropBreedingRecipeRules.requiredVoltageTier(crop) == VoltageIndex.ZPM,
                "large breeder requires ZPM");
            require(CropBreedingChance.percent(crop) == 20, "highest grade defaults to 20 percent success");
            ItemStack seed = crop.getSeedItem(new SeedStats((byte) 31, (byte) 31, (byte) 31, true));
            require(CropRegistry.instance.get(seed) == crop, "seed NBT resolves to sunnarium flower");
            List<ICropMutation> mutations = MutationRegistry.instance.getDeterministicMutations()
                .stream()
                .filter(mutation -> mutation.getOutput() == crop)
                .toList();
            require(mutations.size() == 1, "one deterministic mutation registered before NEI generation");
            ICropMutation mutation = mutations.get(0);
            var parents = new ArrayList<>(
                List.of(CropRegistry.instance.get(NetherStarFlowerCrop.ID), CropsNHCrops.Glowheat));
            require(
                mutation.getParents()
                    .containsAll(parents) && mutation.getParentCount() == 2,
                "nether star flower and glowheat are the mutation parents");
            require(
                mutation.canBreed(parents, null, new ItemStack[0]) == null,
                "machine breeding rejects missing sunnarium block catalyst");
            int[] consumed = mutation.canBreed(parents, null, new ItemStack[] { Materials.Sunnarium.getBlocks(1) });
            require(consumed != null && consumed[0] == 1, "machine breeding consumes one sunnarium block catalyst");
            require(
                CropsNHGTRecipeMaps.fakeCropBreederRecipeMap.getAllRecipes()
                    .stream()
                    .anyMatch(
                        recipe -> recipe.getMetadata(CropsNHGTRecipeMaps.CROPSNH_CROP_MUTATION_METADATAKEY)
                            == mutation),
                "NEI machine breeding recipe generated");
            for (RecipeMap<?> map : List.of(
                CropsNHGTRecipeMaps.fakeSeedGeneratorRecipes,
                CropsNHGTRecipeMaps.fakeCropGeneExtractorRecipeMap,
                CropsNHGTRecipeMaps.fakeCropSynthesizerRecipeMap)) {
                require(
                    map.getAllRecipes()
                        .stream()
                        .anyMatch(recipe -> recipe.getMetadata(CropsNHGTRecipeMaps.CROPSNH_CROP_METADATAKEY) == crop),
                    "NEI recipe generated: " + map.unlocalizedName);
            }
            var plan = CropBreedingPlanner.plan(
                crop.getId(),
                Set.of(
                    CropRegistry.instance.get(NetherStarFlowerCrop.ID)
                        .getId(),
                    CropsNHCrops.Glowheat.getId()));
            require(
                plan.steps.size() == 1 && plan.steps.get(0) == mutation,
                "large crop breeder finds the direct breeding route");
            boolean requiredSubstrates = CropBreederConfig.requireSubstrates;
            try {
                CropBreederConfig.requireSubstrates = true;
                require(
                    CropBreedingSubstrate.forMutation(mutation)
                        .stream()
                        .anyMatch(candidate -> candidate.matches(Materials.Sunnarium.getBlocks(1))),
                    "large crop breeder recognizes the sunnarium block substrate when enabled");
            } finally {
                CropBreederConfig.requireSubstrates = requiredSubstrates;
            }

            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            World world = player.worldObj;
            world.getGameRules()
                .setOrCreateGameRule("doMobSpawning", "false");
            world.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules()
                .setOrCreateGameRule("randomTickSpeed", "0");
            world.setWorldTime(6000);
            for (int x = -2; x <= 9; x++) {
                for (int z = -2; z <= 8; z++) world.setBlock(x, 77, z, Blocks.stone, 0, 3);
            }
            for (int i = 0; i < 4; i++) {
                int x = i * 2;
                world.setBlock(x, 78, 0, Block.getBlockFromItem(substrate.getItem()), substrate.getItemDamage(), 3);
                world.setBlock(x, 79, 0, Blocks.farmland, 7, 3);
                world.setBlock(x, 80, 0, CropsNHBlocks.blockCropSticks, 0, 3);
                TileEntityCropSticks tile = (TileEntityCropSticks) world.getTileEntity(x, 80, 0);
                require(
                    tile.tryPlantSeed(seed.copy())
                        .getLeft() == SeedPlantingResult.SUCCESS,
                    "seed plants on farmland, stage " + i);
                if (i == 0) {
                    require(tile.areGrowthRequirementsMet(), "sunnarium block enables growth");
                    require(tile.harvest(1.0) == null, "immature crop cannot be harvested");
                    require(
                        mutation.canBreed(parents, world, tile, x, 80, 0),
                        "in-world breeding accepts sunnarium block substrate");
                    world.setBlock(x, 78, 0, Blocks.stone, 0, 3);
                    require(!tile.areGrowthRequirementsMet(), "growth stops without sunnarium block");
                    require(
                        !mutation.canBreed(parents, world, tile, x, 80, 0),
                        "in-world breeding rejects missing sunnarium block substrate");
                    world.setBlock(x, 78, 0, Block.getBlockFromItem(substrate.getItem()), substrate.getItemDamage(), 3);
                    require(tile.areGrowthRequirementsMet(), "growth resumes after restoring sunnarium block");
                    tile.setGrowthProgress(crop.getGrowthDuration());
                    List<ItemStack> drops = tile.harvest(1.0);
                    require(
                        drops != null && !drops.isEmpty()
                            && drops.stream()
                                .allMatch(product::isItemEqual),
                        "mature crop harvests sunnarium dust only");
                    require(!tile.isMature() && tile.hasCrop(), "harvest resets growth and preserves the planted crop");
                    NBTTagCompound nbt = new NBTTagCompound();
                    tile.writeToNBT(nbt);
                    var restored = new TileEntityCropSticks();
                    restored.setWorldObj(world);
                    restored.readFromNBT(nbt);
                    require(
                        restored.getSeed()
                            .getCrop() == crop
                            && restored.getSeed()
                                .getStats()
                                .getGain() == 31,
                        "tile NBT preserves crop identity and seed stats");
                }
                tile.setGrowthProgress(crop.getGrowthDuration() * i / 3);
                require(crop.getSpriteIndex(tile) == i, "growth selects texture stage " + i);
                world.markBlockForUpdate(x, 80, 0);
            }
            player.inventory.setInventorySlotContents(0, seed);
            player.inventory.setInventorySlotContents(1, product);
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.setPositionAndUpdate(3.5, 80.7, 5.5);
            ready = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "sunnarium-flower-" + System.currentTimeMillis(),
                    "Sunnarium Flower QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server integration checks failed", failure);
            if (launched && ++ticks > 1800) throw new AssertionError("QA timeout");
            if (ready && mc.thePlayer != null && stage == 0) {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.setPositionAndRotation(3.5, 80.7, 5.5, 180, 19);
                mc.gameSettings.thirdPersonView = 0;
                mc.gameSettings.hideGUI = false;
                if (mc.currentScreen != null) mc.displayGuiScreen(null);
                mc.setIngameNotInFocus();
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished || ++frames < 100) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld.getTileEntity(6, 80, 0) == null) return;
        try {
            frames = 0;
            if (stage == 0) {
                require(
                    "阳光花".equals(StatCollector.translateToLocal(crop.getUnlocalizedName())),
                    "Chinese crop name is loaded");
                IIcon[] sprites = crop.getSprites();
                require(sprites != null && sprites.length == 4, "all four crop sprites are stitched");
                for (int i = 0; i < sprites.length; i++) {
                    require(
                        sprites[i].getIconName()
                            .equals("gtneverreset:crops/sunnarium_flower/" + (i + 1)) && sprites[i].getIconWidth() == 16
                            && sprites[i].getIconHeight() == 16,
                        "native texture exists in the atlas: stage " + i);
                }
                var harvestHandler = new NEICropsNHCropHandler();
                harvestHandler.loadCraftingRecipes(Materials.Sunnarium.getDust(1));
                require(harvestHandler.numRecipes() == 1, "NEI finds sunnarium flower from sunnarium dust");
                var breedingHandler = new NEICropsNHCropstickBreedingHandler();
                breedingHandler.loadCraftingRecipes(crop.getSeedItem(SeedStats.DEFAULT_ANALYZED));
                require(breedingHandler.numRecipes() == 1, "NEI finds the seed's in-world breeding recipe");
                mc.ingameGUI.getChatGUI()
                    .clearChatMessages();
                screenshot("sunnarium-flower-stages.png");
                var gui = GuiCraftingRecipe.createRecipeGui("item", true, crop.getSeedItem(SeedStats.DEFAULT_ANALYZED));
                require(gui != null, "NEI opens the seed recipe page");
                require(
                    gui.openTargetRecipe(RecipeId.of(breedingHandler, 0)) >= 0,
                    "NEI selects the sunnarium flower breeding recipe");
                stage++;
            } else {
                screenshot("sunnarium-flower-nei.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[sunnarium-flower-qa] PASS " + message);
    }

    private void finish(Throwable error) {
        if (finished) return;
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("checks.txt"), report.toString());
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
        }, "sunnarium-flower-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
