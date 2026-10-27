package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardVisualisation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Exercises the lazy class loads that failed after rebuilding a running client's development JAR. */
@Mod(modid = "runtimejarqa", name = "Runtime JAR QA", version = "1", dependencies = "after:gtneverreset")
public final class RuntimeJarClientChecks {

    private boolean started;
    private volatile boolean given;
    private boolean finished;
    private int ticks;
    private int frames;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "runtime-jar-qa-" + System.currentTimeMillis(),
                "Runtime JAR QA",
                new WorldSettings(39L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!given || mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return;
        if (mc.thePlayer.getHeldItem()
            .getItem() != GTNRItemList.WirelessFrequencyCard.getItem()) return;
        try {
            // Also runs via AE's RenderWorldLastEvent while the card is held for 120 client ticks.
            WirelessCardVisualisation.activeCard(mc.thePlayer);
            if (++ticks == 120) mc.displayGuiScreen(new ModelProbeGui());
        } catch (Throwable failure) {
            finish(failure.toString());
            throw failure;
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || given) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, GTNRItemList.WirelessFrequencyCard.get(1));
        player.inventory.setInventorySlotContents(1, GTNRItemList.AdvancedIOBus.get(1));
        player.inventory.setInventorySlotContents(2, GTNRItemList.StockIOInterfacePart.get(1));
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        given = true;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || finished || !(mc.currentScreen instanceof ModelProbeGui)) return;
        if (++frames != 30) return;
        ScreenShotHelper
            .saveScreenshot(output(), "runtime-items.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        finish("PASS");
    }

    private void finish(String result) throws Exception {
        finished = true;
        Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        System.out.println("RUNTIME_JAR_QA: " + result);
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static File output() {
        return new File(System.getProperty("gtnr.runtimeJar.output"));
    }

    private static final class ModelProbeGui extends GuiScreen {

        private final RenderItem renderer = new RenderItem();

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawCenteredString(fontRendererObj, "运行时 JAR 验证", width / 2, height / 2 - 90, 0xFFFFFF);
            GTNRItemList[] items = { GTNRItemList.AdvancedIOBus, GTNRItemList.StockIOInterfacePart,
                GTNRItemList.WirelessFrequencyCard };
            String[] labels = { "ME 高级 IO 总线", "库存 IO 接口（ME）", "无线频率卡" };
            for (int i = 0; i < items.length; i++) {
                int x = width / 2 - 170 + 138 * i;
                int y = height / 2 - 35;
                drawRect(x, y, x + 64, y + 64, 0xFF888888);
                GL11.glPushMatrix();
                GL11.glTranslatef(x, y, 0);
                GL11.glScalef(4, 4, 4);
                RenderHelper.enableGUIStandardItemLighting();
                ItemStack stack = items[i].get(1);
                // Direct rendering: exceptions must fail QA instead of being replaced with NEI's fire icon.
                renderer.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
                drawCenteredString(fontRendererObj, labels[i], x + 32, y + 82, 0xFFFFFF);
            }
        }
    }
}
