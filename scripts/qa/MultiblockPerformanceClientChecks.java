package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.Language;
import net.minecraft.util.ScreenShotHelper;

import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "multiblockperformanceclientqa",
    name = "Multiblock Performance Client QA",
    version = "1",
    dependencies = "after:multiblockperformanceqa")
public final class MultiblockPerformanceClientChecks {

    private static final List<String> NAMES = List.of(
        "tiered-bonuses",
        "nonlinear-suffix",
        "nonlinear-island",
        "recipe-heat",
        "fixed-heat",
        "always-perfect",
        "no-tier-skip",
        "unlimited-tier-skip");
    private boolean started;
    private boolean finished;
    private boolean advance;
    private int page;
    private int frames;
    private int ticks;
    private Preview preview;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.performanceTooltip.client.qa")) FMLCommonHandler.instance()
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
            if (++ticks > 3600) throw new AssertionError("multiblock performance visual verification timed out");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (!started && mc.currentScreen != null && mc.theWorld == null) {
                started = true;
                show();
            } else if (advance) {
                advance = false;
                page++;
                if (page == NAMES.size() * 2) {
                    Files.write(
                        new File(output(), "client-result.txt").toPath(),
                        "PASS".getBytes(StandardCharsets.UTF_8));
                    System.out
                        .println("MULTIBLOCK_PERFORMANCE_CLIENT_QA PASS: sixteen English/Chinese performance pages");
                    finished = true;
                } else show();
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !started || finished || advance) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen != preview || ++frames < 25) return;
            if (preview.clipped) throw new AssertionError("performance page is clipped: " + name());
            ScreenShotHelper
                .saveScreenshot(output(), name() + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            advance = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void show() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String code = page < NAMES.size() ? "en_US" : "zh_CN";
        var manager = mc.getLanguageManager();
        if (!manager.getCurrentLanguage()
            .getLanguageCode()
            .equals(code)) {
            Language language = manager.getLanguages()
                .stream()
                .filter(
                    candidate -> candidate.getLanguageCode()
                        .equals(code))
                .findFirst()
                .orElseThrow();
            manager.setCurrentLanguage(language);
            mc.gameSettings.language = code;
            mc.refreshResources();
            mc.fontRenderer.setUnicodeFlag(manager.isCurrentLocaleUnicode() || mc.gameSettings.forceUnicodeFont);
            mc.fontRenderer.setBidiFlag(manager.isCurrentLanguageBidirectional());
        }
        if (page % NAMES.size() == 0) new MultiblockPerformanceChecks().verify();
        var machine = MultiblockPerformanceChecks.previews()
            .get(page % NAMES.size());
        List<String> lines = Arrays.asList(
            new AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
                .toolTipFinisher()
                .getInformation());
        if (lines.stream()
            .anyMatch(line -> line.contains("gtnr.tooltip."))) {
            throw new AssertionError("performance page has an untranslated language key: " + name());
        }
        Files.write(new File(output(), name() + ".txt").toPath(), lines, StandardCharsets.UTF_8);
        preview = new Preview(name(), lines);
        mc.displayGuiScreen(preview);
        frames = 0;
    }

    private String name() {
        return NAMES.get(page % NAMES.size()) + (page < NAMES.size() ? "-en" : "-zh");
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.performanceTooltip.qa.output"));
        output.mkdirs();
        return output;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(
                new File(output(), "client-result.txt").toPath(),
                ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
        } catch (Exception writeFailure) {
            failure.addSuppressed(writeFailure);
        }
        finished = true;
    }

    private static final class Preview extends GuiScreen {

        private final String title;
        private final List<String> lines;
        private boolean clipped;

        private Preview(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawString(fontRendererObj, title, 16, 14, 0xffffff);
            int y = 36;
            for (String line : lines) {
                for (String wrapped : fontRendererObj.listFormattedStringToWidth(line, width - 32)) {
                    drawString(fontRendererObj, wrapped, 16, y, 0xffffff);
                    y += 10;
                }
            }
            clipped = y > height - 12;
        }
    }
}
