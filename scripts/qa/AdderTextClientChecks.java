package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;

import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.client.text.EffectTextRenderer;
import com.xyp.gtneverreset.client.text.compat.AngelicaTextAdapter;
import com.xyp.gtneverreset.utils.text.AnimatedText;
import com.xyp.gtneverreset.utils.text.effect.TextEffects;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "addertextqa", name = "Adder Text QA", version = "1", dependencies = "after:gtneverreset")
public final class AdderTextClientChecks {

    private Preview preview;
    private int frames;
    private int page;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
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
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.limitFramerate = 60;
        if (preview == null && mc.currentScreen != null && mc.theWorld == null) {
            try {
                if (!AngelicaTextAdapter.supportsEffects(mc.fontRenderer))
                    throw new AssertionError("font batching bridge is unavailable");
                show();
            } catch (Throwable failure) {
                finish(failure);
            }
        }
    }

    private void show() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.setUnicodeFlag(page == 1);
        if (page == 2) EffectTextRenderer.INSTANCE.onResourceManagerReload(mc.getResourceManager());
        preview = new Preview();
        mc.displayGuiScreen(preview);
        frames = 0;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || preview == null || finished) return;
        if (Minecraft.getMinecraft().currentScreen != preview) return;
        try {
            frames++;
            if (preview.failure != null) throw preview.failure;
            if (frames != 10 && frames != 35) return;
            Minecraft mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(
                output(),
                "adder-" + page + "-" + frames + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            var field = EffectTextRenderer.class.getDeclaredField("failed");
            field.setAccessible(true);
            if (!((Set<?>) field.get(EffectTextRenderer.INSTANCE)).isEmpty())
                throw new AssertionError("shader fell back after an error");
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error after text draw");
            if (frames == 35) {
                if (++page == 3) finish(null);
                else show();
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private static File output() {
        File directory = new File(System.getProperty("gtnr.adderText.qa.output"));
        directory.mkdirs();
        return directory;
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        try {
            Files.writeString(
                new File(output(), "client-result.txt").toPath(),
                failure == null ? "PASS" : "FAIL: " + failure);
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        finished = true;
    }

    private final class Preview extends GuiScreen {

        private Throwable failure;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            try {
                drawDefaultBackground();
                String line = AnimatedText.GT_NEVER_RESET.get();
                String plain = TextEffects.plainText(line);
                if (fontRendererObj.getStringWidth(line) != fontRendererObj.getStringWidth(plain))
                    throw new AssertionError("effect markers changed tooltip width");
                if (EffectTextRenderer.handles("&{exotic_rainbow}Other mod text"))
                    throw new AssertionError("foreign effect syntax was intercepted");
                if (EffectTextRenderer.handles("&{gtneverreset:exotic_rainbow;speed=2}User text"))
                    throw new AssertionError("configurable inline styles must not be supported");
                for (String wrapped : fontRendererObj.listFormattedStringToWidth(line, 75)) {
                    if (fontRendererObj.getStringWidth(wrapped) > 75)
                        throw new AssertionError("wrapped tooltip exceeds requested width");
                    if (fontRendererObj.getStringWidth(wrapped)
                        != fontRendererObj.getStringWidth(TextEffects.plainText(wrapped)))
                        throw new AssertionError("wrapped effect marker leaked into font measurement");
                }
                drawString(fontRendererObj, "GTNG Exotic Rainbow / GT Never Reset", 20, 20, 0xFFFFFF);
                drawString(fontRendererObj, "Mode: " + page + " (normal, unicode, reloaded)", 20, 36, 0xAAAAAA);
                fontRendererObj.drawString(line, 20, 65, 0xFFFFFF, false);
                fontRendererObj.drawStringWithShadow(line, 20, 85, 0xFFFFFF);
                fontRendererObj.drawString("添加者： " + TextEffects.apply("GT Never Reset"), 20, 105, 0xFFFFFF);
                drawHoveringText(List.of("Large Ore Processor", "Normal tooltip text", line), 40, 150, fontRendererObj);
                drawString(fontRendererObj, "Normal rendering after shader: WHITE RED GREEN", 20, 225, 0xFFFFFF);
            } catch (Throwable exception) {
                failure = exception;
            }
        }
    }
}
