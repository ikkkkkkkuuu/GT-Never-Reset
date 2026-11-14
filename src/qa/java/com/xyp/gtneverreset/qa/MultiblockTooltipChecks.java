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

import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "structuretooltipclientqa",
    name = "Structure Tooltip Client QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class MultiblockTooltipChecks {

    private static final List<String> NAMES = List.of(
        "LargeOreProcessor",
        "LargeFurnace",
        "QuantumComputer",
        "AssemblerMatrix",
        "SingularityDataHub",
        "LargeBeeBreeder",
        "LargeCropBreeder",
        "UniversalJointFactory");
    private boolean started;
    private boolean finished;
    private boolean advance;
    private int page;
    private int frames;
    private int ticks;
    private Preview preview;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.multiblock.tooltip.qa")) FMLCommonHandler.instance()
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
            if (++ticks > 3600) throw new AssertionError("structure tooltip visual verification timed out");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.limitFramerate = 60;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (!started && mc.currentScreen != null && mc.theWorld == null) {
                started = true;
                verifyPerformanceContracts();
                show();
            } else if (advance) {
                advance = false;
                page++;
                if (page == NAMES.size() * 4) {
                    Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                    System.out.println("STRUCTURE_TOOLTIP_CLIENT_QA PASS: 32 English/Chinese machine tooltip pages");
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
            if (preview.clipped) throw new AssertionError("structure page is clipped: " + name());
            ScreenShotHelper
                .saveScreenshot(output(), name() + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            advance = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void show() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String code = page < NAMES.size() * 2 ? "en_US" : "zh_CN";
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
        if (page % (NAMES.size() * 2) == 0) {
            for (String key : Files.readAllLines(new File("translation-keys.txt").toPath(), StandardCharsets.UTF_8)) {
                if (!net.minecraft.util.StatCollector.canTranslate(key))
                    throw new AssertionError("Missing translation " + code + ": " + key);
            }
        }
        var id = com.xyp.gtneverreset.utils.enums.GTNRMachineID.valueOf(NAMES.get(page % NAMES.size()));
        var machine = (gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase<?>) gregtech.api.GregTechAPI.METATILEENTITIES[id.id];
        if (machine == null) throw new AssertionError("Missing registered machine " + id);
        var builder = new AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
        boolean structure = (page / NAMES.size()) % 2 != 0;
        List<String> lines = Arrays.asList(structure ? builder.getStructureInformation() : builder.getInformation());
        if (lines.isEmpty()) throw new AssertionError("Empty tooltip " + name());
        if (lines.stream()
            .anyMatch(line -> line.contains("gtnr.") || line.contains("{var:") || line.contains("%s"))) {
            throw new AssertionError("Unresolved tooltip " + name() + ": " + lines);
        }
        if (id == com.xyp.gtneverreset.utils.enums.GTNRMachineID.LargeBeeBreeder && !structure
            && lines.stream()
                .noneMatch(line -> line.contains("100%") && (line.contains("每") || line.contains("per")))) {
            throw new AssertionError("Bee voltage doubling must be derived from its valid HV-and-higher range");
        }
        Files.write(new File(output(), name() + ".txt").toPath(), lines, StandardCharsets.UTF_8);
        preview = new Preview(name(), lines);
        mc.displayGuiScreen(preview);
        frames = 0;
    }

    private static void verifyPerformanceContracts() {
        var machine = new ProbeMachine();
        var detected = com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipReader.readStructure(machine);
        if (!net.minecraft.init.Blocks.wool.getLocalizedName()
            .equals(detected.casingName())
            || detected.casingTierNames()
                .stream()
                .distinct()
                .count() != 3)
            throw new AssertionError("Tiered casing metadata must be derived from actual structure elements");
        var empty = new com.xyp.gtneverreset.common.machine.tooltip.StructureSummary(
            null,
            false,
            null,
            List.of(),
            List.of(),
            List.of(),
            null,
            List.of(),
            false,
            "QA casing",
            List.of("QA tier zero", "QA tier one", "QA tier two"));
        var filtered = com.xyp.gtneverreset.common.machine.tooltip.MultiblockPerformanceReader
            .readPerformance(machine, empty);
        if (filtered.stats() == null || filtered.tierBonuses()
            .stream()
            .anyMatch(b -> b.part() != com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart.CASING))
            throw new AssertionError("Absent channels must exclude coil/glass without losing base stats");
        if (filtered.heat() != null || filtered.tierSkips()
            .isPresent() || !"QA casing".equals(filtered.casingName()))
            throw new AssertionError("Independent failure isolation and casing metadata");
        if (filtered.perfectOverclockFrom()
            .stream()
            .noneMatch(
                t -> t.tierName()
                    .contains("QA tier one")))
            throw new AssertionError("Actual casing tier name must reach overclock threshold");
        var present = new com.xyp.gtneverreset.common.machine.tooltip.StructureSummary(
            null,
            false,
            null,
            List.of(),
            List.of(),
            List.of(
                gregtech.common.misc.GTStructureChannels.HEATING_COIL,
                gregtech.common.misc.GTStructureChannels.BOROGLASS));
        var sampled = com.xyp.gtneverreset.common.machine.tooltip.MultiblockPerformanceReader
            .readPerformance(machine, present);
        for (var part : machine.getTooltipTieredParts()) {
            if (sampled.tierBonuses()
                .stream()
                .noneMatch(b -> b.part() == part)) throw new AssertionError("Missing sampled part " + part);
        }
        System.out.println("STRUCTURE_TOOLTIP_CONTRACT_QA PASS: channels, casing names, independent failure isolation");
    }

    private static final class ProbeMachine extends com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor {

        private ProbeMachine() {
            super("qa_performance_probe");
        }

        @Override
        protected void checkMachineHatches(List<gregtech.api.structure.error.StructureError> errors) {}

        @Override
        public com.gtnewhorizon.structurelib.structure.IStructureDefinition<com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor> getStructureDefinition() {
            return com.gtnewhorizon.structurelib.structure.StructureDefinition.<com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor>builder()
                .addShape("main", new String[][] { { "~C" } })
                .addElement(
                    'C',
                    com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel(
                        "qa_casing",
                        com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered(
                            (block, meta) -> meta,
                            List.of(
                                org.apache.commons.lang3.tuple.Pair.of(net.minecraft.init.Blocks.wool, 0),
                                org.apache.commons.lang3.tuple.Pair.of(net.minecraft.init.Blocks.wool, 1),
                                org.apache.commons.lang3.tuple.Pair.of(net.minecraft.init.Blocks.wool, 2)),
                            -1,
                            (machine, tier) -> {},
                            machine -> -1)))
                .build();
        }

        @Override
        public void construct(net.minecraft.item.ItemStack stack, boolean hintsOnly) {
            buildPiece("main", stack, hintsOnly, 0, 0, 0);
        }

        @Override
        public gregtech.api.interfaces.metatileentity.IMetaTileEntity newMetaEntity(
            gregtech.api.interfaces.tileentity.IGregTechTileEntity tile) {
            return new ProbeMachine();
        }

        @Override
        public TooltipStats getTooltipStats() {
            return new TooltipStats(1, 1, 1, false);
        }

        @Override
        public List<TieredPart> getTooltipTieredParts() {
            return List.of(TieredPart.COIL, TieredPart.GLASS, TieredPart.CASING);
        }

        @Override
        public TierRange getTooltipTierRange(TieredPart part) {
            return new TierRange(0, 2);
        }

        @Override
        public TooltipStats getTooltipStatsAtTier(TieredPart part, int tier) {
            return new TooltipStats(1 + tier, 1, 1, tier >= 1);
        }

        @Override
        public HeatRules getTooltipHeatRules() {
            throw new IllegalStateException("QA expected heat failure");
        }

        @Override
        public java.util.OptionalInt getTooltipTierSkips() {
            throw new IllegalStateException("QA expected tier skip failure");
        }
    }

    private String name() {
        return NAMES.get(page % NAMES.size()) + ((page / NAMES.size()) % 2 == 0 ? "-info" : "-structure")
            + (page < NAMES.size() * 2 ? "-en" : "-zh");
    }

    private static File output() {
        File output = new File(".");
        output.mkdirs();
        return output;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(
                new File(output(), "result.txt").toPath(),
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
            int maxWidth = lines.stream()
                .mapToInt(fontRendererObj::getStringWidth)
                .max()
                .orElse(0);
            clipped = maxWidth > width - 48 || lines.size() * 10 > height - 64;
            drawHoveringText(lines, 24, 45, fontRendererObj);
        }
    }
}
