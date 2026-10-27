package com.xyp.gtneverreset.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.loader.MachineLoader;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mod(
    modid = "modernfluidmachineclientqa",
    name = "Modern Fluid Machine Client QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class ModernFluidMachineClientChecks {

    private static final String[] NAMES = { "mixer", "electrolyzer", "chemical_reactor", "centrifuge" };
    private static final int[] ITEM_SLOTS = { 7, 8, 4, 8 };
    private static final int[] FLUID_SLOTS = { 3, 7, 5, 7 };
    private static final int ATLAS_PANELS_PER_PAGE = 10;
    private static final String[][] PANELS = { { "mixer", "front", "Mixer front idle" },
        { "mixer", "front_active", "Mixer front active" }, { "mixer", "top", "Mixer top idle" },
        { "mixer", "top_active", "Mixer top active" }, { "electrolyzer", "front", "Electrolyzer idle" },
        { "electrolyzer", "front_active", "Electrolyzer active" }, { "electrolyzer", "side", "Electrolyzer side" },
        { "electrolyzer", "side_active", "Electrolyzer active side" },
        { "chemical_reactor", "front", "Chemical reactor idle" },
        { "chemical_reactor", "front_active", "Chemical reactor active" },
        { "centrifuge", "front", "Centrifuge front idle" }, { "centrifuge", "front_active", "Centrifuge front active" },
        { "centrifuge", "side", "Centrifuge side idle" }, { "centrifuge", "side_active", "Centrifuge side active" },
        { "centrifuge", "top", "Centrifuge top idle" }, { "centrifuge", "top_active", "Centrifuge top active" } };

    private final BaseMetaTileEntity[] tiles = new BaseMetaTileEntity[NAMES.length];
    private final StringBuilder report = new StringBuilder();
    private volatile int stage;
    private volatile int requested = 0;
    private volatile boolean ready;
    private volatile boolean finished;
    private boolean launched;
    private int clientTicks;
    private int serverTicks;
    private int frames;
    private int controlStage;
    private int atlasPage;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.modernMachines.client.qa")) FMLCommonHandler.instance()
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
            require(++clientTicks < 6000, "client visual QA timeout, stage=" + stage);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            ready = mc.theWorld != null;
            if (ready) {
                for (int i = 0; i < NAMES.length; i++)
                    ready &= mc.theWorld.getTileEntity(i * 3, 10, 0) instanceof BaseMetaTileEntity tile
                        && tile.getMetaTileEntity() instanceof ModernFluidMachine;
            }
            if (!launched && mc.theWorld == null && mc.currentScreen != null) {
                launched = true;
                mc.launchIntegratedServer(
                    "modern-machines-" + System.currentTimeMillis(),
                    "Modern Machines QA",
                    new WorldSettings(747L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            if (++serverTicks == 1) setup(player);
            if (requested < 0 || serverTicks < 40 || !ready) return;
            int target = requested;
            requested = -1;
            player.closeScreen();
            if (target < NAMES.length) {
                player.playerNetServerHandler.setPlayerLocation(target * 3 + 0.5, 11, -3, 0, 10);
                ModernFluidMachine machine = (ModernFluidMachine) tiles[target].getMetaTileEntity();
                require(machine.onRightclick(tiles[target], player), "native machine GUI opens: " + NAMES[target]);
                stage = target + 1;
            } else {
                player.playerNetServerHandler.setPlayerLocation((NAMES.length - 1) * 1.5, 13, -9, 0, 18);
                stage = NAMES.length + 1;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        player.worldObj.setWorldTime(6000);
        player.worldObj.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        player.worldObj.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        FluidStack[] fluids = { Materials.Water.getFluid(1000), Materials.Lava.getFluid(1000),
            Materials.Oxygen.getGas(1000), Materials.Hydrogen.getGas(1000), Materials.Nitrogen.getGas(1000),
            Materials.Chlorine.getGas(1000) };
        ItemStack[] items = { new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot),
            new ItemStack(Items.redstone), new ItemStack(Items.diamond), new ItemStack(Items.coal),
            new ItemStack(Items.quartz) };
        ItemStack[][] families = families();
        for (int family = 0; family < NAMES.length; family++) {
            int x = family * 3;
            player.worldObj.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, 10, 0);
            tile.setInitialValuesAsNBT(null, (short) families[family][1].getItemDamage());
            tile.setFrontFacing(ForgeDirection.NORTH);
            tile.setOwnerName(player.getCommandSenderName());
            tile.setOwnerUuid(player.getUniqueID());
            tile.disableWorking();
            ModernFluidMachine machine = (ModernFluidMachine) tile.getMetaTileEntity();
            machine.initDefaultModes(null);
            machine.mMainFacing = ForgeDirection.NORTH;
            tile.setFrontFacing(ForgeDirection.SOUTH);
            machine.mItemTransfer = false;
            machine.mFluidTransfer = false;
            int inputs = machine.mInputSlotCount;
            int outputs = machine.mOutputItems.length;
            for (int i = 0; i < inputs; i++)
                machine.setInventorySlotContents(machine.getInputSlot() + i, items[i].copy());
            for (int i = 0; i < outputs; i++)
                machine.setInventorySlotContents(machine.getOutputSlot() + i, items[i].copy());
            for (int i = 0; i < machine.getInputTanks().length; i++)
                machine.getInputTanks()[i].fill(fluids[i].copy(), true);
            for (int i = 0; i < machine.getOutputTanks().length; i++)
                machine.getOutputTanks()[i].fill(fluids[i].copy(), true);
            tiles[family] = tile;
        }
        player.playerNetServerHandler.setPlayerLocation(0.5, 11, -3, 0, 10);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage <= NAMES.length) {
                if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper) || ++frames < 40) return;
                int family = stage - 1;
                if (family == 0 && controlStage < 7) {
                    verifyControls(wrapper);
                    controlStage++;
                    frames = 0;
                    return;
                }
                verifyGui(wrapper, family);
                capture(NAMES[family] + "-gui.png");
                frames = 0;
                int next = stage;
                stage = 0;
                requested = next;
            } else if (stage == NAMES.length + 1) {
                if (mc.currentScreen != null || ++frames < 60) return;
                capture("modern-machines-world.png");
                checkAtlas();
                mc.displayGuiScreen(new Preview(false));
                stage = NAMES.length + 2;
                frames = 0;
            } else if (stage == NAMES.length + 2) {
                if (!(mc.currentScreen instanceof Preview) || ++frames < 30) return;
                capture("modern-machines-lv-through-uv.png");
                mc.displayGuiScreen(new Preview(true));
                stage = NAMES.length + 3;
                frames = 0;
            } else if (stage == NAMES.length + 3 && mc.currentScreen instanceof Preview) {
                String imageName = atlasPage == 0 ? "modern-machines-idle-active-atlas"
                    : "centrifuge-idle-active-atlas";
                if (++frames == 30) capture(imageName + ".png");
                if (frames < 65) return;
                capture(imageName + "-next.png");
                if (++atlasPage * ATLAS_PANELS_PER_PAGE < PANELS.length) {
                    mc.displayGuiScreen(new Preview(true, atlasPage));
                    frames = 0;
                    return;
                }
                Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS\n" + report);
                System.out.println(
                    "MODERN_FLUID_MACHINE_CLIENT_QA PASS: four native GUIs, synchronized slots, all tier casings and animated atlas textures");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyControls(GuiContainerWrapper wrapper) {
        var screen = wrapper.getScreen();
        var panel = screen.getMainPanel();
        ModernFluidMachine machine = (ModernFluidMachine) tiles[0].getMetaTileEntity();
        List<ToggleButton> toggles = collect(panel, ToggleButton.class).stream()
            .filter(
                button -> button.getArea()
                    .x()
                    < panel.getArea()
                        .x() + 45)
            .sorted(
                Comparator.comparingInt(
                    button -> button.getArea()
                        .x()))
            .toList();
        require(toggles.size() == 2, "native fluid/item auto-output buttons remain available");
        if (controlStage == 0 || controlStage == 3) {
            for (ToggleButton button : toggles) button.onMousePressed(0);
        } else if (controlStage == 1 || controlStage == 4) {
            boolean expected = controlStage == 1;
            require(
                machine.mFluidTransfer == expected && machine.mItemTransfer == expected,
                "ordinary auto-output button clicks synchronize to server: " + expected);
            String key = "modernOutputSide_" + (expected ? "fluidAutoOutput" : "itemAutoOutput");
            var handler = screen.getSyncManager()
                .getMainPSM()
                .findPanelHandlerNullable(key);
            require(handler != null, "Shift side-selector handler is registered: " + key);
            handler.openPanel();
        } else if (controlStage == 2 || controlStage == 5) {
            String name = "modernOutputSide_" + (controlStage == 2 ? "fluidAutoOutput" : "itemAutoOutput");
            var popup = screen.getPanelManager()
                .getOpenPanels()
                .stream()
                .filter(open -> name.equals(open.getName()))
                .findFirst()
                .orElseThrow();
            List<ButtonWidget<?>> buttons = new ArrayList<>();
            collectButtons(popup, buttons);
            require(buttons.size() == 5, "side selector presents all five valid output directions");
            capture(controlStage == 2 ? "fluid-output-side-selector.png" : "item-output-side-selector.png");
            int selected = controlStage == 2 ? 0 : 4;
            popup.doSafe(
                () -> buttons.get(selected)
                    .onMousePressed(0));
        }
        if (controlStage == 3 || controlStage == 6) {
            ForgeDirection expected = controlStage == 3 ? ForgeDirection.UP : ForgeDirection.SOUTH;
            BaseMetaTileEntity clientTile = (BaseMetaTileEntity) Minecraft.getMinecraft().theWorld
                .getTileEntity(0, 10, 0);
            require(
                tiles[0].getFrontFacing() == expected && clientTile.getFrontFacing() == expected,
                "side selection synchronizes direction to server and client: " + expected);
            require(
                screen.getPanelManager()
                    .getOpenPanels()
                    .stream()
                    .noneMatch(
                        open -> open.getName()
                            .startsWith("modernOutputSide_")),
                "side selector closes after selection");
            if (controlStage == 6) report.append(
                "Both native auto-output toggles synchronize on/off; fluid/item Shift selector handlers open five-direction popups and actual button clicks synchronize UP/SOUTH output faces to server and client.\n");
        }
    }

    private static void collectButtons(IWidget widget, List<ButtonWidget<?>> buttons) {
        if (widget instanceof ButtonWidget<?>button) buttons.add(button);
        for (IWidget child : widget.getChildren()) collectButtons(child, buttons);
    }

    private void verifyGui(GuiContainerWrapper wrapper, int family) {
        var panel = wrapper.getScreen()
            .getMainPanel();
        List<ItemSlot> items = collect(panel, ItemSlot.class).stream()
            .filter(
                slot -> "item_inv".equals(
                    slot.getSlot()
                        .getSlotGroupName()))
            .toList();
        List<FluidSlot> fluids = collect(panel, FluidSlot.class);
        require(items.size() == ITEM_SLOTS[family], NAMES[family] + " recipe item slot count: " + items.size());
        require(fluids.size() == FLUID_SLOTS[family], NAMES[family] + " recipe fluid slot count: " + fluids.size());
        for (ItemSlot item : items) require(
            item.getSlot()
                .getStack() != null,
            "item fixture synchronized");
        for (FluidSlot fluid : fluids) require(
            fluid.getFluidStack() != null && fluid.getFluidStack().amount == 1000,
            "fluid fixture and amount synchronized");
        List<IWidget> slots = new ArrayList<>(items);
        slots.addAll(fluids);
        var bounds = panel.getArea();
        for (int i = 0; i < slots.size(); i++) {
            var area = slots.get(i)
                .getArea();
            require(
                area.x() >= bounds.x() && area.y() >= bounds.y()
                    && area.ex() <= bounds.ex()
                    && area.ey() <= bounds.ey(),
                "recipe slot remains inside native GUI");
            for (int j = i + 1; j < slots.size(); j++) {
                var other = slots.get(j)
                    .getArea();
                require(
                    area.ex() <= other.x() || area.x() >= other.ex()
                        || area.ey() <= other.y()
                        || area.y() >= other.ey(),
                    "item and fluid slots do not overlap");
            }
        }
        report.append(NAMES[family])
            .append(": ")
            .append(items.size())
            .append(" item and ")
            .append(fluids.size())
            .append(" fluid slots rendered, synchronized and visible without overlap.\n");
    }

    private void checkAtlas() {
        for (String tier : new String[] { "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv" }) {
            for (String face : new String[] { "bottom", "side", "top" }) sprite("modern_casings/" + tier + "/" + face);
        }
        for (String[] panel : PANELS) {
            TextureAtlasSprite icon = sprite("modern_" + panel[0] + "/overlay_" + panel[1]);
            if (panel[1].endsWith("active")) require(icon.getFrameCount() > 1, "active animation frames: " + panel[2]);
        }
        for (String machine : new String[] { "electrolyzer", "chemical_reactor" }) {
            sprite("modern_" + machine + "/overlay_front_emissive");
            require(
                sprite("modern_" + machine + "/overlay_front_active_emissive").getFrameCount() > 1,
                "active emissive animation: " + machine);
        }
        for (String[] panel : PANELS) {
            int family = familyIndex(panel[0]);
            ModernFluidMachine machine = (ModernFluidMachine) tiles[family].getMetaTileEntity();
            boolean active = panel[1].endsWith("active");
            ForgeDirection face = panel[1].startsWith("top") ? ForgeDirection.UP
                : panel[1].startsWith("side") ? ForgeDirection.EAST : ForgeDirection.NORTH;
            require(
                machine.getTexture(tiles[family], face, ForgeDirection.SOUTH, -1, active, false).length >= 2,
                "machine selects casing and " + panel[2] + " overlay");
        }
        report.append(
            "All 24 LV-UV casing sprites; idle/active overlays of all four machines including centrifuge front, side and top; and animated emissive sprites are present in the real block atlas.\n");
    }

    private static TextureAtlasSprite sprite(String suffix) {
        String name = "gtneverreset:basicmachines/" + suffix;
        TextureAtlasSprite sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks()
            .getAtlasSprite(name);
        require(
            name.equals(sprite.getIconName()) && sprite.getIconWidth() == 16 && sprite.getIconHeight() == 16,
            "registered 16x16 atlas sprite: " + name);
        return sprite;
    }

    private static <T> List<T> collect(IWidget widget, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(widget)) result.add(type.cast(widget));
        for (IWidget child : widget.getChildren()) result.addAll(collect(child, type));
        return result;
    }

    private static ItemStack[][] families() {
        return new ItemStack[][] { MachineLoader.MODERN_MIXERS, MachineLoader.MODERN_ELECTROLYZERS,
            MachineLoader.MODERN_CHEMICAL_REACTORS, MachineLoader.MODERN_CENTRIFUGES };
    }

    private static int familyIndex(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        throw new AssertionError("unknown machine family: " + name);
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.modernMachines.client.qa.output"));
        output.mkdirs();
        return output;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fail(Throwable failure) {
        if (finished) return;
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

    private static final class Preview extends GuiScreen {

        private final boolean textures;
        private final int texturePage;
        private final RenderItem items = new RenderItem();

        private Preview(boolean textures) {
            this(textures, 0);
        }

        private Preview(boolean textures, int texturePage) {
            this.textures = textures;
            this.texturePage = texturePage;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawString(
                fontRendererObj,
                textures ? "GTM · animated block-atlas textures" : "GTM · LV through UV machine items",
                18,
                16,
                0xffffff);
            if (textures) {
                int start = texturePage * ATLAS_PANELS_PER_PAGE;
                int end = Math.min(start + ATLAS_PANELS_PER_PAGE, PANELS.length);
                for (int i = start; i < end; i++) {
                    int local = i - start;
                    drawPanel(i, 18 + local % 5 * 104, 52 + local / 5 * 135);
                }
                return;
            }
            ItemStack[][] families = families();
            for (int family = 0; family < NAMES.length; family++) {
                int y = 55 + family * 94;
                drawString(fontRendererObj, NAMES[family], 18, y - 16, 0xffffff);
                for (int tier = 1; tier <= 8; tier++) {
                    int x = 26 + (tier - 1) * 62;
                    RenderHelper.enableGUIStandardItemLighting();
                    GL11.glPushMatrix();
                    GL11.glTranslatef(x, y, 0);
                    GL11.glScalef(2, 2, 2);
                    items.renderItemAndEffectIntoGUI(
                        fontRendererObj,
                        mc.getTextureManager(),
                        families[family][tier],
                        0,
                        0);
                    GL11.glPopMatrix();
                    RenderHelper.disableStandardItemLighting();
                    drawString(fontRendererObj, GTValues.VN[tier], x, y + 38, 0xffffff);
                }
            }
        }

        private void drawPanel(int index, int x, int y) {
            String[] panel = PANELS[index];
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            quad(sprite("modern_casings/lv/" + (panel[1].startsWith("top") ? "top" : "side")), x, y);
            quad(sprite("modern_" + panel[0] + "/overlay_" + panel[1]), x, y);
            if (panel[0].equals("electrolyzer") || panel[0].equals("chemical_reactor"))
                quad(sprite("modern_" + panel[0] + "/overlay_" + panel[1] + "_emissive"), x, y);
            GL11.glDisable(GL11.GL_BLEND);
            int textY = y + 70;
            for (String line : fontRendererObj.listFormattedStringToWidth(panel[2], 100)) {
                drawString(fontRendererObj, line, x, textY, 0xffffff);
                textY += 10;
            }
        }

        private static void quad(TextureAtlasSprite icon, int x, int y) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(x, y + 64, 0, icon.getMinU(), icon.getMaxV());
            tessellator.addVertexWithUV(x + 64, y + 64, 0, icon.getMaxU(), icon.getMaxV());
            tessellator.addVertexWithUV(x + 64, y, 0, icon.getMaxU(), icon.getMinV());
            tessellator.addVertexWithUV(x, y, 0, icon.getMinU(), icon.getMinV());
            tessellator.draw();
        }
    }
}
