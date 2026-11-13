package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;
import com.xyp.gtnrlib.client.text.TypewriterTimeline;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "typewriterqa", name = "Typewriter QA", version = "1", dependencies = "after:" + GTNRLib.MODID)
public final class TypewriterClientChecks {

    private Preview screen;
    private int frames;
    private int stage;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.typewriter.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (screen == null && mc.currentScreen instanceof GuiMainMenu) {
            screen = new Preview();
            mc.displayGuiScreen(screen);
            TypewriterTimeline.INSTANCE.clear();
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screen == null || finished) return;
        try {
            frames++;
            double elapsed = (System.nanoTime() - screen.started) * 1e-9;
            if (stage == 0 && frames >= 2 || stage == 1 && elapsed >= 0.8
                || stage == 2 && elapsed >= 2.0
                || stage == 3 && elapsed >= 6.0
                || stage >= 4 && frames >= 8) {
                Minecraft mc = Minecraft.getMinecraft();
                if (screen.failure != null) throw new AssertionError(screen.failure);
                ScreenShotHelper.saveScreenshot(
                    output(),
                    "stage-" + stage + ".png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                stage++;
                frames = 0;
                if (stage == 4) GTNRLibConfig.enableTypewriter = false;
                if (stage == 5) {
                    GTNRLibConfig.enableTypewriter = true;
                    TypewriterTimeline.INSTANCE.clear();
                }
                if (stage == 6) finish(null);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static File output() {
        return new File(System.getProperty("gtnrlib.typewriter.qa.output"));
    }

    private void finish(Throwable failure) {
        finished = true;
        try {
            Files.writeString(
                new File(output(), "result.txt").toPath(),
                failure == null ? "PASS\n" : "FAIL\n" + failure);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (failure != null) failure.printStackTrace();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class Preview extends GuiScreen {

        private static final String TEXT = "&{tw;10}Typewriter / 中文逐字显示&{p;800} ... done";
        private final long started = System.nanoTime();
        private GuiTextField input;
        private int measured = -1;
        private String failure;

        @Override
        public void initGui() {
            input = new GuiTextField(fontRendererObj, 24, 190, 440, 20);
            input.setMaxStringLength(200);
            input.setText("&{tw;2}Input always visible / 输入框始终显示全文");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xFF161D29);
            fontRendererObj.drawString("GTNRLib typewriter QA", 24, 16, 0xFFFFFF);
            int current = fontRendererObj.getStringWidth(TEXT);
            if (measured < 0) measured = current;
            if (current != measured) failure = "layout width changed during typing";
            fontRendererObj.drawString(TEXT, 24, 42, 0xFFFFFF);
            fontRendererObj.drawString("&{tw;15}Second tooltip line / 第二行", 24, 56, 0xFFFFFF);
            fontRendererObj.drawString("&{ba;t=10}Golden typing / 熠金文字", 25, 84, 0xFFFFFF);
            fontRendererObj.drawString("&{nr;t=10}Nebula / 星云&{p;800} pause", 26, 110, 0xFFFFFF);
            fontRendererObj.drawString("&{tw;10}Multiline / 多行\nNext line / 下一行", 27, 138, 0xFFFFFF);
            input.drawTextBox();
            if (!EffectTextRenderer.handles(TEXT)) failure = "text mixin renderer does not handle markers";
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
