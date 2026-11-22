package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.error.StructureError;

@Mod(modid = "threadhatchqa", name = "Thread Hatch QA", version = "1", dependencies = "after:gtneverreset")
public final class ThreadHatchChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, finished;
    private volatile boolean ready;
    private volatile Throwable failure;
    private int ticks, frames, stage;

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
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            World world = player.worldObj;
            BaseMetaTileEntity[] hatches = new BaseMetaTileEntity[8];
            var factory = (UniversalJointFactory) place(world, 12, GTNRMachineID.UniversalJointFactory.id)
                .getMetaTileEntity();
            int originalThreads = factory.getTotalRecipeThreads();
            for (int i = 0; i < hatches.length; i++) {
                ItemStack item = GTNRItemList.THREAD[i].get(1);
                hatches[i] = place(world, i, item.getItemDamage());
                var hatch = (ParameterModuleHatch) hatches[i].getMetaTileEntity();
                int bonus = 1 << i;
                require(
                    hatch.getModuleType() == MachineModuleType.Thread && hatch.getParameter() == bonus,
                    "T" + (i + 1) + " registration and bonus " + bonus);
                hatch.setParameter(9999);
                require(
                    hatch.getParameter() == bonus && !hatch.isDynamic(),
                    "tier value cannot be changed by GUI setter");
                require(
                    RecipeMaps.assemblerRecipes.getAllRecipes()
                        .stream()
                        .anyMatch(
                            recipe -> Arrays.stream(recipe.mOutputs)
                                .anyMatch(output -> output != null && output.isItemEqual(item))),
                    "T" + (i + 1) + " assembler recipe exists");
                factory.clearHatches();
                require(factory.addParameterModuleToMachineList(hatches[i], 16), "factory accepts thread hatch");
                require(
                    factory.getTotalRecipeThreads() == originalThreads + bonus,
                    "factory retains base threads and adds tier bonus");
                require(
                    factory.addParameterModuleToMachineList(hatches[i], 16)
                        && factory.getTotalRecipeThreads() == originalThreads + bonus,
                    "repeated structure visit does not duplicate hatch");
            }
            factory.clearHatches();
            require(factory.getTotalRecipeThreads() == originalThreads, "removing hatch restores machine base threads");
            verifyMachinePolicies(world, hatches);
            require(
                new LargeBeeBreeder("qa.bees").getParameterModuleLimit(MachineModuleType.Thread) == 0,
                "bee breeder without recipe-thread engine rejects thread hatches");
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.playerNetServerHandler.setPlayerLocation(4, 80, -7, 0, 6);
            ready = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void verifyMachinePolicies(World world, BaseMetaTileEntity[] hatches) {
        PolicyMachine machine = new PolicyMachine();
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setMetaTileEntity(machine);
        machine.setBaseMetaTileEntity(tile);
        tile.enableWorking();
        machine.fixAllIssues();
        require(machine.addParameterModuleToMachineList(hatches[0], 16), "default machine accepts first thread hatch");
        require(machine.getTotalRecipeThreads() == 2, "default single hatch adds one thread");
        machine.addParameterModuleToMachineList(hatches[1], 16);
        List<StructureError> errors = new ArrayList<>();
        machine.collectTooltipStructureRequirements(errors);
        require(errors.size() == 1, "default machine rejects duplicate hatches during formation");
        machine.limit = 2;
        errors.clear();
        machine.collectTooltipStructureRequirements(errors);
        require(
            errors.isEmpty() && machine.getTotalRecipeThreads() == 4,
            "machine can allow two hatches with additive bonuses");
        machine.strongestOnly = true;
        require(
            machine.getTotalRecipeThreads() == 3,
            "machine can choose strongest-only aggregation independently of count");
        machine.baseThreads = Integer.MAX_VALUE;
        require(machine.getTotalRecipeThreads() == Integer.MAX_VALUE, "thread total saturates without overflow");
        machine.baseThreads = 1;
        machine.limit = 0;
        require(
            machine.getTotalRecipeThreads() == 1 && !machine.addParameterModuleToMachineList(hatches[2], 16),
            "machine can disable thread hatches");
        machine.clearHatches();
        machine.limit = 1;
        machine.strongestOnly = false;
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList tasks = new NBTTagList();
        tasks.appendTag(
            new RecipeThreadTask(
                2,
                8,
                3,
                1,
                new RecipeThreadOutputs(new ItemStack[] { new ItemStack(Items.diamond) }, new FluidStack[0])).save());
        tag.setTag("gtnrRecipeThreads", tasks);
        machine.loadNBTData(tag);
        machine.fixAllIssues();
        machine.tick();
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .getProgress() == 0,
            "out-of-capacity saved thread remains suspended");
        machine.addParameterModuleToMachineList(hatches[1], 16);
        machine.tick();
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .getProgress() == 1,
            "reinstalled T2 hatch resumes suspended thread");
        machine.clearHatches();
        machine.tick();
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .getProgress() == 1,
            "removing hatch suspends extra thread without discarding it");
        NBTTagCompound saved = new NBTTagCompound();
        machine.saveNBTData(saved);
        machine.loadNBTData(saved);
        require(
            machine.getRecipeThreadTasks()
                .size() == 1
                && machine.getRecipeThreadTasks()
                    .get(0)
                    .getProgress() == 1,
            "suspended task and progress survive machine NBT round trip");
        machine.addParameterModuleToMachineList(hatches[1], 16);
        machine.tick();
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .getProgress() == 2,
            "restored task resumes after hatch returns");
        machine.tick();
        require(
            machine.getRecipeThreadTasks()
                .get(0)
                .isFinished() && machine.getLastWorkingTick() == 0,
            "blocked output keeps the finished task without recording delivery");
        BaseMetaTileEntity output = place(
            world,
            13,
            ItemList.Hatch_Output_Bus_LV.get(1)
                .getItemDamage());
        require(machine.addOutputBusToMachineList(output, 16), "attach output bus for delivery retry");
        machine.tick();
        long deliveredAt = machine.getTotalRuntimeInTicks();
        require(
            machine.getRecipeThreadTasks()
                .isEmpty()
                && output.getStackInSlot(0)
                    .getItem() == Items.diamond
                && machine.getLastWorkingTick() == deliveredAt,
            "successful delivery records the controller tick");
        machine.tick();
        require(machine.getLastWorkingTick() == deliveredAt, "idle tick preserves the last delivery time");
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++ticks > 80) {
                launched = true;
                ticks = 0;
                mc.launchIntegratedServer(
                    "thread-hatch-" + System.currentTimeMillis(),
                    "Thread Hatch QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server checks", failure);
            if (launched && ++ticks > 1800) throw new AssertionError("QA timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished || ++frames < 100) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            frames = 0;
            if (stage == 0) {
                if (mc.currentScreen != null) return;
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "thread-hatches-world.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                TextureAtlasSprite sprite = mc.getTextureMapBlocks()
                    .getAtlasSprite("gtneverreset:modular/OVERLAY_ThreadController");
                require(
                    sprite.getIconWidth() == 32 && sprite.getIconHeight() == 32 && sprite.getFrameCount() == 8,
                    "game atlas loaded native 32x32 texture with eight animation frames");
                mc.displayGuiScreen(new PreviewScreen());
                stage++;
            } else {
                ScreenShotHelper.saveScreenshot(
                    new File("."),
                    "thread-hatch-atlas.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static BaseMetaTileEntity place(World world, int x, int id) {
        world.setBlock(x, 80, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 80, 0);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "thread-hatch-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class PolicyMachine extends GTNRMultiBlockBase<PolicyMachine> {

        private int limit = 1;
        private int baseThreads = 1;
        private boolean strongestOnly;

        private PolicyMachine() {
            super("qa.thread.policy");
        }

        @Override
        public int getParameterModuleLimit(MachineModuleType type) {
            return type == MachineModuleType.Thread ? limit : 0;
        }

        @Override
        protected int getThreadModuleBonus() {
            if (!strongestOnly || limit <= 0) return super.getThreadModuleBonus();
            return getParameterModules().stream()
                .filter(hatch -> hatch.getModuleType() == MachineModuleType.Thread)
                .mapToInt(ParameterModuleHatch::getParameter)
                .max()
                .orElse(0);
        }

        @Override
        public int getMaxRecipeThreads() {
            return baseThreads;
        }

        @Override
        public long getMaxInputEu() {
            return 8192;
        }

        @Override
        public boolean onRunningTick(ItemStack stack) {
            return true;
        }

        private void tick() {
            mMachine = true;
            runMachine(getBaseMetaTileEntity(), ++mTotalRunTime);
        }

        @Override
        protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {}

        @Override
        public IStructureDefinition<PolicyMachine> getStructureDefinition() {
            return null;
        }

        @Override
        public void construct(ItemStack stack, boolean hintsOnly) {}

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new PolicyMachine();
        }

        @Override
        public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
            boolean active, boolean redstone) {
            return new ITexture[0];
        }
    }

    private static final class PreviewScreen extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "线程仓 / Thread Hatch — 32 × 32 / 8 frames", width / 2, 25, 0x82E9FF);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            drawTexturedModelRectFromIcon(
                width / 2 - 64,
                65,
                mc.getTextureMapBlocks()
                    .getAtlasSprite("gtneverreset:modular/OVERLAY_ThreadController"),
                128,
                128);
            drawCenteredString(fontRendererObj, "T1–T8: +1 / 2 / 4 / 8 / 16 / 32 / 64 / 128", width / 2, 215, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "安装数量与叠加方式由机器决定", width / 2, 240, 0xFFFFFF);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
