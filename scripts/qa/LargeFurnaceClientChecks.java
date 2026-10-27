package com.xyp.gtneverreset.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.StructureTooltipReader;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.ITexture;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.render.IIconTexture;

@Mod(
    modid = "largefurnaceclientqa",
    name = "Large Furnace Client QA",
    version = "1",
    dependencies = "after:largefurnaceqa")
public final class LargeFurnaceClientChecks {

    private volatile int stage;
    private volatile boolean finished;
    private boolean started;
    private int clientTicks;
    private int serverTicks;
    private int frames;
    private LargeFurnace machine;
    private BaseMetaTileEntity tile;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.furnace.client.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (finished) {
            FMLCommonHandler.instance()
                .exitJava(0, false);
            return;
        }
        try {
            require(++clientTicks < 3000, "client visual verification timeout, stage=" + stage);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            if (!started && mc.currentScreen != null && mc.theWorld == null) {
                started = true;
                mc.launchIntegratedServer(
                    "large-furnace-" + System.currentTimeMillis(),
                    "Large Furnace QA",
                    new WorldSettings(774L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
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
            serverTicks++;
            if (serverTicks == 20) {
                var checks = new LargeFurnaceChecks();
                checks.verify(player.worldObj);
                machine = checks.getMachine();
                tile = checks.getTile();
                tile.setOwnerUuid(player.getUniqueID());
                tile.setOwnerName(player.getCommandSenderName());
                tile.disableWorking();
                player.worldObj.getGameRules()
                    .setOrCreateGameRule("doDaylightCycle", "false");
                player.worldObj.getGameRules()
                    .setOrCreateGameRule("doFireTick", "false");
                player.worldObj.getGameRules()
                    .setOrCreateGameRule("doMobSpawning", "false");
                player.worldObj.setWorldTime(6000);
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(0.5, 80, -3, 0, 0);
            }
            if (serverTicks == 60) {
                require(machine.mMachine, "full structure remains formed in integrated server");
                machine.onRightclick(tile, player);
                stage = 1;
            }
            if (stage == 2) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(17, 89, -18, 38, 20);
                stage = 3;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0 || stage == 2) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 1 && !(mc.currentScreen instanceof GuiContainerWrapper)) return;
            if (stage == 3 && mc.currentScreen != null) return;
            if (stage >= 4 && !(mc.currentScreen instanceof Preview)) return;
            if (++frames < (stage == 3 ? 100 : 30)) return;
            frames = 0;
            if (stage == 1) {
                capture("large-furnace-controller-gui.png");
                stage = 2;
            } else if (stage == 3) {
                capture("large-furnace-structure-world.png");
                checkTextures();
                require(
                    RecipeCatalysts.containsCatalyst(new FurnaceRecipeHandler(), GTNRItemList.LargeFurnace.get(1)),
                    "native NEI smelting catalyst is registered");
                var tooltip = tooltip();
                List<String> lines = Arrays.asList(tooltip.getInformation());
                String text = String.join("\n", lines);
                require(
                    text.contains("200") && text.contains("80") && text.contains("64") && text.contains("6"),
                    "Chinese tooltip displays requested base performance");
                require(text.contains("熔炉") && text.contains("翻倍"), "Chinese furnace purpose and voltage scaling");
                mc.displayGuiScreen(new Preview("大型熔炉 · 自动性能说明", lines, false));
                stage = 4;
            } else if (stage == 4) {
                capture("large-furnace-tooltip-zh.png");
                List<String> lines = Arrays.asList(tooltip().getStructureInformation());
                String text = String.join("\n", lines);
                var size = StructureTooltipReader.readStructure(new LargeFurnace("large_furnace_bounds_preview"))
                    .size();
                require(
                    size.width() == 11 && size.height() == 13 && size.depth() == 11,
                    "automatic structure reader reports occupied 11x13x11 bounds");
                require(
                    text.contains("11") && text.contains("13") && text.contains("56"),
                    "Shift structure tooltip occupied 11x13x11 dimensions and lava requirement");
                mc.displayGuiScreen(new Preview("大型熔炉 · Shift 结构说明", lines, false));
                stage = 5;
            } else if (stage == 5) {
                capture("large-furnace-structure-tooltip-zh.png");
                mc.displayGuiScreen(
                    new Preview("大型熔炉 · 原生贴图预览", List.of("待机 / 运行正面，原始 16 × 16 资源", "两种正面均已注册到方块图集；透明边框显示底层机壳"), true));
                stage = 6;
            } else {
                capture("large-furnace-front-textures.png");
                Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS");
                System.out.println(
                    "LARGE_FURNACE_CLIENT_QA PASS: full integrated checks, native GUI, Chinese tooltips, "
                        + "original structure, idle/active atlas sprites and NEI catalyst");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static AutoMultiblockTooltipBuilder tooltip() {
        var tooltip = new AutoMultiblockTooltipBuilder();
        tooltip.addAutoTooltip(new LargeFurnace("large_furnace_tooltip_preview"))
            .toolTipFinisher();
        return tooltip;
    }

    private void checkTextures() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (boolean active : new boolean[] { false, true }) {
            String state = active ? "active" : "front";
            String name = ModList.GTNeverReset.getResourceLocation() + ":parallel/furnace_" + state;
            TextureAtlasSprite sprite = mc.getTextureMapBlocks()
                .getAtlasSprite(name);
            require(
                sprite.getIconName()
                    .equals(name) && sprite.getIconWidth() == 16
                    && sprite.getIconHeight() == 16,
                "registered 16x16 furnace atlas sprite " + state);
            ITexture[] layers = machine.getTexture(tile, ForgeDirection.NORTH, ForgeDirection.NORTH, -1, active, false);
            require(layers.length == 2, "separate native casing and front texture");
            require(
                ((IIconTexture) layers[1]).getIcon(2, null)
                    .getIconName()
                    .equals(name),
                "controller chooses correct " + state + " sprite");
            BufferedImage image;
            try (InputStream stream = mc.getResourceManager()
                .getResource(texture(state))
                .getInputStream()) {
                image = ImageIO.read(stream);
            }
            int transparent = 0;
            for (int pixel : image.getRGB(0, 0, 16, 16, null, 0, 16)) if ((pixel >>> 24) == 0) transparent++;
            require(transparent == 122, "original transparent panel frame " + state);
        }
    }

    private static ResourceLocation texture(String state) {
        return new ResourceLocation(
            ModList.GTNeverReset.getResourceLocation(),
            "textures/blocks/parallel/furnace_" + state + ".png");
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.furnace.client.qa.output"));
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

        private final String title;
        private final List<String> lines;
        private final boolean textures;
        private final RenderItem items = new RenderItem();

        private Preview(String title, List<String> lines, boolean textures) {
            this.title = title;
            this.lines = lines;
            this.textures = textures;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawString(fontRendererObj, title, 16, 14, 0xffffff);
            RenderHelper.enableGUIStandardItemLighting();
            items.renderItemAndEffectIntoGUI(
                fontRendererObj,
                mc.getTextureManager(),
                GTNRItemList.LargeFurnace.get(1),
                16,
                32);
            RenderHelper.disableStandardItemLighting();
            int y = 58;
            for (String line : lines) {
                for (String wrapped : fontRendererObj.listFormattedStringToWidth(line, width - 32)) {
                    drawString(fontRendererObj, wrapped, 16, y, 0xffffff);
                    y += 10;
                }
            }
            if (textures) {
                panel("front", 24, y + 20);
                panel("active", 144, y + 20);
            }
        }

        private void panel(String state, int x, int y) {
            drawRect(x, y, x + 96, y + 96, 0xff707070);
            mc.getTextureManager()
                .bindTexture(texture(state));
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(x, y + 96, 0, 0, 1);
            tessellator.addVertexWithUV(x + 96, y + 96, 0, 1, 1);
            tessellator.addVertexWithUV(x + 96, y, 0, 1, 0);
            tessellator.addVertexWithUV(x, y, 0, 0, 0);
            tessellator.draw();
            GL11.glDisable(GL11.GL_BLEND);
            drawString(fontRendererObj, state.equals("front") ? "待机" : "运行", x, y + 102, 0xffffff);
        }
    }
}
