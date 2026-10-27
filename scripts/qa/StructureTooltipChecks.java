package com.xyp.gtneverreset.qa;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.StructureSummary;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.StructureTooltipReader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.structure.IStructureChannels;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.misc.GTStructureChannels;

@Mod(
    modid = "structuretooltipqa",
    name = "Structure Tooltip QA",
    version = "1",
    dependencies = "after:" + GTNeverReset.MODID)
public final class StructureTooltipChecks {

    private int checks;

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("gtnr.structureTooltip.qa") || Boolean.getBoolean("gtnr.structureTooltip.client.qa"))
            return;
        String result = "PASS";
        try {
            verify();
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        try {
            File output = new File(System.getProperty("gtnr.structureTooltip.qa.output"));
            output.mkdirs();
            Files.write(new File(output, "server-result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        MinecraftServer.getServer()
            .initiateShutdown();
    }

    public void verify() {
        verifyFixedStructures();
        verifyIndependentTiers();
        verifyAdditiveTiers();
        verifyMaterialTiers();
        verifyLayers(false);
        verifyLayers(true);
        verifySharedCasings();
        verifyFailureCleanup();
        System.out.println("STRUCTURE_TOOLTIP_QA PASS: " + checks + " assertions");
    }

    public static List<MTEEnhancedMultiBlockBase<?>> previews() {
        return List.of(
            new IndependentTiers(),
            new AdditiveTiers(),
            new MaterialTiers(),
            new Layers(false),
            new Layers(true),
            new SharedCasings());
    }

    private void verifyFixedStructures() {
        FixedStructure original = new FixedStructure();
        original.constructCalls = 17;
        StructureSummary summary = StructureTooltipReader.readStructure(original);
        require(original.constructCalls == 17, "reading a tooltip leaves the caller's mutable state alone");
        require(!StructureTooltipHooks.isRecording(original), "recording is not left active for the caller");
        require(
            summary.size()
                .width() == 3
                && summary.size()
                    .height() == 3
                && summary.size()
                    .depth() == 3
                && summary.hollow(),
            "existing 3x3x3 hollow geometry is preserved");
        require(
            summary.blocks()
                .size() == 1 && summary.blocks()
                    .get(0)
                    .count()
                    .equals("20-25"),
            "overlapping buildPiece calls do not double count casings");
        require(
            summary.hatches()
                .stream()
                .anyMatch(
                    hatch -> "1-2".equals(hatch.count()) && hatch.nameKeys()
                        .contains(Energy.getDescriptionLangKey())),
            "existing combined hatch minimum and maximum remain intact");
        for (MTEEnhancedMultiBlockBase<?> machine : List.of(
            new LargeFurnace("qa_furnace"),
            new LargeOreProcessor("qa_ore"),
            new QuantumComputer("qa_computer"),
            new AssemblerMatrix("qa_matrix"))) {
            StructureSummary fixed = StructureTooltipReader.readStructure(machine);
            require(
                !fixed.layered() && !fixed.hasIndependentTiers() && !fixed.hasMaterialTiers(),
                "existing fixed machine has no spurious tiers: " + machine.getClass()
                    .getSimpleName());
            require(
                !fixed.blocks()
                    .isEmpty()
                    && (machine instanceof QuantumComputer ? fixed.hatches()
                        .isEmpty()
                        : !fixed.hatches()
                            .isEmpty()),
                "existing fixed machine retains its blocks and declared hatch positions: " + machine.getClass()
                    .getSimpleName());
        }
    }

    private void verifyIndependentTiers() {
        StructureSummary summary = StructureTooltipReader.readStructure(new IndependentTiers());
        require(summary.hasIndependentTiers() && !summary.layered(), "unrelated shapes are independent tiers");
        require(
            summary.sections()
                .size() == 2,
            "clamped projector sizes are deduplicated into two tiers");
        var first = summary.sections()
            .get(0)
            .details();
        var second = summary.sections()
            .get(1)
            .details();
        require(first != null && second != null, "each independent tier carries its own geometry and hatch rules");
        require(
            first.size()
                .width() == 3
                && first.size()
                    .height() == 2
                && first.size()
                    .depth() == 1,
            "first independent tier dimensions");
        require(
            second.size()
                .width() == 5
                && second.size()
                    .height() == 3
                && second.size()
                    .depth() == 2,
            "second independent tier dimensions");
        require(
            first.controller()
                .frontCenter()
                && !second.controller()
                    .frontCenter()
                && first.controller()
                    .layer()
                    != second.controller()
                        .layer(),
            "independent tiers keep different controller positions");
        require(
            first.hatches()
                .stream()
                .anyMatch(
                    h -> h.nameKeys()
                        .contains(Energy.getDescriptionLangKey()) && "1".equals(h.count())),
            "first tier uses the first projector stack's energy requirement");
        require(
            second.hatches()
                .stream()
                .anyMatch(
                    h -> h.nameKeys()
                        .contains(InputBus.getDescriptionLangKey()) && "2".equals(h.count())),
            "second tier uses its actual first projector stack 4, not ordinal tier 2");
    }

    private void verifyMaterialTiers() {
        MaterialTiers caller = new MaterialTiers();
        caller.materialTier = 19;
        StructureSummary summary = StructureTooltipReader.readStructure(caller);
        require(caller.materialTier == 19, "material probing does not mutate the caller");
        require(
            summary.hasMaterialTiers() && !summary.hasIndependentTiers() && !summary.layered(),
            "material alternatives are separate from shape tiers");
        var materials = summary.sections()
            .get(0)
            .materialTiers();
        require(materials.size() == 2, "both material tiers are represented");
        require(
            materials.get(0)
                .header()
                .equals("Material One")
                && materials.get(1)
                    .header()
                    .equals("Material Two"),
            "machine supplied material tier headers");
        String cobble = new ItemStack(Blocks.cobblestone).getDisplayName();
        String stone = new ItemStack(Blocks.stone).getDisplayName();
        require(
            materials.get(0)
                .blocks()
                .stream()
                .anyMatch(
                    block -> block.names()
                        .contains(cobble)
                        && block.count()
                            .equals("2")),
            "first material tier resolves actual cobblestone placement");
        require(
            materials.get(1)
                .blocks()
                .stream()
                .anyMatch(
                    block -> block.names()
                        .contains(stone)
                        && block.count()
                            .equals("2")),
            "second material tier uses its projector subchannel");
        require(
            summary.channels()
                .contains(MaterialTiers.MaterialChannel.TEST),
            "custom material tier channel keeps its description and registration behavior");
        String glass = new ItemStack(Blocks.glass).getDisplayName();
        String obsidian = new ItemStack(Blocks.obsidian).getDisplayName();
        require(
            summary.sections()
                .get(0)
                .blocks()
                .stream()
                .anyMatch(
                    block -> block.tiered() && block.names()
                        .contains(glass)
                        && block.names()
                            .contains(obsidian)),
            "an independent projector channel remains a generic tiered block outside the material sections");
        require(
            materials.stream()
                .allMatch(
                    material -> material.blocks()
                        .size() == 1),
            "changing the material channel does not drag an unrelated glass channel into its material tiers");
    }

    private void verifyAdditiveTiers() {
        StructureSummary summary = StructureTooltipReader.readStructure(new AdditiveTiers());
        require(
            !summary.hasIndependentTiers() && !summary.layered(),
            "a fixed extension is an additive tier, not a layer");
        require(
            summary.sections()
                .size() == 2
                && summary.size()
                    .depth() == 1
                && summary.allTiers()
                    .depth() == 2,
            "additive tier has its own section and overall dimension range");
        require(
            summary.sections()
                .get(1)
                .blocks()
                .stream()
                .anyMatch(
                    block -> block.names()
                        .contains(new ItemStack(Blocks.brick_block).getDisplayName())
                        && block.count()
                            .equals("3")),
            "additive section counts only its newly added blocks");
        require(
            summary.sections()
                .get(1)
                .hatches()
                .stream()
                .anyMatch(
                    hatch -> hatch.nameKeys()
                        .contains(InputBus.getDescriptionLangKey()) && "2".equals(hatch.count())),
            "additive higher tier keeps its changed complete hatch requirement");
    }

    private void verifyLayers(boolean horizontal) {
        StructureSummary summary = StructureTooltipReader.readStructure(new Layers(horizontal));
        require(
            summary.layered() && !summary.hasIndependentTiers(),
            "moving cap and repeated middle are layered: " + horizontal);
        require(
            summary.sections()
                .size() >= 2,
            "layered structure includes its repeat section: " + horizontal);
        require(
            horizontal ? summary.size()
                .depth() == 4
                && summary.allTiers()
                    .depth() == 6
                : summary.size()
                    .height() == 4
                    && summary.allTiers()
                        .height() == 6,
            "layer growth uses the correct axis: " + horizontal);
        require(
            horizontal ? summary.size()
                .height()
                == summary.allTiers()
                    .height()
                : summary.size()
                    .depth()
                    == summary.allTiers()
                        .depth(),
            "non-growing axis remains fixed: " + horizontal);
        require(
            summary.hatches()
                .stream()
                .anyMatch(
                    h -> h.nameKeys()
                        .contains(InputHatch.getDescriptionLangKey())
                        && h.locations()
                            .stream()
                            .anyMatch(text -> text.contains("layer") || text.contains("层"))),
            "per-layer hatch requirement is attached to its hatch: " + horizontal);
        require(
            summary.sections()
                .stream()
                .anyMatch(
                    section -> !section.casingRequirements()
                        .isEmpty()),
            "growing casing minimum is preserved as an aggregate requirement: " + horizontal);
        String text = String.join(
            "\n",
            new AutoMultiblockTooltipBuilder().addAutoTooltip(new Layers(horizontal))
                .toolTipFinisher()
                .getStructureInformation());
        require(
            text.contains("4") && text.contains("6"),
            "rendered dimension range includes both limits: " + horizontal);
    }

    private void verifySharedCasings() {
        StructureSummary summary = StructureTooltipReader.readStructure(new SharedCasings());
        var requirements = summary.sections()
            .get(0)
            .casingRequirements();
        require(
            requirements.equals(List.of("At least 6 casings in total")),
            "shared minimum survives even when no single material can meet it");
        require(
            summary.blocks()
                .stream()
                .anyMatch(
                    block -> block.count()
                        .equals("0-4"))
                && summary.blocks()
                    .stream()
                    .anyMatch(
                        block -> block.count()
                            .equals("0-5")),
            "shared minimum does not become a false per-material lower limit");
    }

    private void verifyFailureCleanup() {
        for (FailingStructure failure : List.of(new FailingStructure(false), new FailingStructure(true))) {
            boolean rejected = false;
            try {
                StructureTooltipReader.readStructure(failure);
            } catch (RuntimeException expected) {
                rejected = true;
            }
            require(rejected, "failing construct/count collection is reported");
            require(!StructureTooltipHooks.isRecording(failure), "failure leaves no recording on the caller");
            require(
                !StructureTooltipReader.readStructure(new FixedStructure())
                    .blocks()
                    .isEmpty(),
                "a subsequent tooltip works after failure");
        }
    }

    private void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private abstract static class Fixture<T extends Fixture<T>> extends GTNRMultiBlockBase<T> {

        private Fixture(String name) {
            super(name);
        }

        @Override
        protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
            throw new AssertionError("Tooltip generation must not invoke the full machine check");
        }

        @Override
        public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
            boolean active, boolean redstone) {
            return new ITexture[0];
        }
    }

    public static final class FixedStructure extends Fixture<FixedStructure> {

        private static final IStructureDefinition<FixedStructure> STRUCTURE = StructureDefinition
            .<FixedStructure>builder()
            .addShape(
                "main",
                new String[][] { { "A~A", "AAA", "AAA" }, { "AAA", "A-A", "AAA" }, { "AAA", "AAA", "AAA" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(FixedStructure.class)
                    .atLeast(Energy, InputBus)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .build();
        private int constructCalls;

        public FixedStructure() {
            super("qa_fixed_structure");
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new FixedStructure();
        }

        @Override
        public IStructureDefinition<FixedStructure> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            constructCalls++;
            buildPiece("main", stack, hints, 1, 0, 0);
            buildPiece("main", stack, hints, 1, 0, 0);
        }

        @Override
        protected void checkMachineHatches(List<StructureError> errors) {
            checkCasingMin(errors, 0, 20);
            checkHatchMin(errors, Energy, 1);
            checkHatchMax(errors, Energy, 2);
            checkHatchMin(errors, InputBus, 1);
        }
    }

    public static final class IndependentTiers extends Fixture<IndependentTiers> {

        private static final IStructureDefinition<IndependentTiers> STRUCTURE = StructureDefinition
            .<IndependentTiers>builder()
            .addShape("first", new String[][] { { "A~A", "AAA" } })
            .addShape("second", new String[][] { { "B~BBB", "BBBBB", "BBBBB" }, { "BBBBB", "BBBBB", "BBBBB" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(IndependentTiers.class)
                    .atLeast(Energy)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .addElement(
                'B',
                GTStructureUtility.buildHatchAdder(IndependentTiers.class)
                    .atLeast(InputBus)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.brick_block, 0)))
            .build();

        public IndependentTiers() {
            super("qa_independent_tiers");
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new IndependentTiers();
        }

        @Override
        public IStructureDefinition<IndependentTiers> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            buildPiece(stack.stackSize < 4 ? "first" : "second", stack, hints, 1, 0, 0);
        }

        @Override
        public void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize) {
            if (projectorStackSize < 4) {
                checkCasingMin(errors, 0, 3);
                checkHatchExact(errors, Energy, 1);
            } else {
                checkCasingMin(errors, 0, 24);
                checkHatchExact(errors, InputBus, 2);
            }
        }
    }

    public static final class MaterialTiers extends Fixture<MaterialTiers> {

        private static final IStructureDefinition<MaterialTiers> STRUCTURE = StructureDefinition
            .<MaterialTiers>builder()
            .addShape("main", new String[][] { { "AB~BA" } })
            .addElement(
                'A',
                withChannel(
                    MaterialChannel.TEST.get(),
                    ofBlocksTiered(
                        (block, meta) -> block == Blocks.cobblestone ? 1 : block == Blocks.stone ? 2 : 0,
                        List.of(Pair.of(Blocks.cobblestone, 0), Pair.of(Blocks.stone, 0)),
                        0,
                        (machine, tier) -> machine.materialTier = tier,
                        machine -> machine.materialTier)))
            .addElement(
                'B',
                withChannel(
                    GTStructureChannels.BOROGLASS.get(),
                    ofBlocksTiered(
                        (block, meta) -> block == Blocks.glass ? 1 : block == Blocks.obsidian ? 2 : 0,
                        List.of(Pair.of(Blocks.glass, 0), Pair.of(Blocks.obsidian, 0)),
                        0,
                        (machine, tier) -> machine.glassTier = tier,
                        machine -> machine.glassTier)))
            .build();
        private int materialTier;
        private int glassTier;

        public MaterialTiers() {
            super("qa_material_tiers");
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new MaterialTiers();
        }

        @Override
        public IStructureDefinition<MaterialTiers> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            buildPiece("main", stack, hints, 2, 0, 0);
        }

        @Override
        public int getStructureTooltipMaterialTiers() {
            return 2;
        }

        @Override
        public String getStructureTooltipMaterialTierHeader(int tier) {
            return tier == 1 ? "Material One" : "Material Two";
        }

        @Override
        public IStructureChannels getStructureTooltipMaterialTierChannel() {
            return MaterialChannel.TEST;
        }

        private enum MaterialChannel implements IStructureChannels {

            TEST;

            @Override
            public String get() {
                return "qa_material";
            }

            @Override
            public String getDefaultTooltip() {
                return "Select material tier";
            }

            @Override
            public void registerAsIndicator(ItemStack indicator, int channelValue) {}
        }
    }

    public static final class AdditiveTiers extends Fixture<AdditiveTiers> {

        private static final IStructureDefinition<AdditiveTiers> STRUCTURE = StructureDefinition
            .<AdditiveTiers>builder()
            .addShape("base", new String[][] { { "A~A" } })
            .addShape("extension", new String[][] { { "BBB" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(AdditiveTiers.class)
                    .atLeast(InputBus)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .addElement('B', ofBlock(Blocks.brick_block, 0))
            .build();

        public AdditiveTiers() {
            super("qa_additive_tiers");
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new AdditiveTiers();
        }

        @Override
        public IStructureDefinition<AdditiveTiers> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            buildPiece("base", stack, hints, 1, 0, 0);
            if (stack.stackSize >= 4) buildPiece("extension", stack, hints, 1, 0, -1);
        }

        @Override
        public void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize) {
            checkHatchExact(errors, InputBus, projectorStackSize < 4 ? 1 : 2);
        }
    }

    public static final class Layers extends Fixture<Layers> {

        private final IStructureDefinition<Layers> structure = StructureDefinition.<Layers>builder()
            .addShape("base", new String[][] { { "A~A" } })
            .addShape("middle", new String[][] { { "BBB" } })
            .addShape("cap", new String[][] { { "CCC" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(Layers.class)
                    .atLeast(Energy)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.stone, 0)))
            .addElement(
                'B',
                GTStructureUtility.buildHatchAdder(Layers.class)
                    .atLeast(InputHatch)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .addElement('C', ofBlock(Blocks.brick_block, 0))
            .build();
        private final boolean horizontal;

        public Layers(boolean horizontal) {
            super(horizontal ? "qa_horizontal_layers" : "qa_vertical_layers");
            this.horizontal = horizontal;
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new Layers(horizontal);
        }

        @Override
        public IStructureDefinition<Layers> getStructureDefinition() {
            return structure;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            int layers = Math.max(2, Math.min(4, stack.stackSize));
            buildPiece("base", stack, hints, 1, 0, 0);
            for (int layer = 1; layer <= layers; layer++)
                buildPiece("middle", stack, hints, 1, horizontal ? 0 : -layer, horizontal ? -layer : 0);
            buildPiece("cap", stack, hints, 1, horizontal ? 0 : -layers - 1, horizontal ? -layers - 1 : 0);
        }

        @Override
        public void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize) {
            int layers = Math.max(2, Math.min(4, projectorStackSize));
            checkCasingMin(errors, 0, layers * 2 + 2);
            checkHatchExact(errors, Energy, 1);
            if (!StructureTooltipHooks.onCheckLayerHatches(this, layers, 1, InputHatch))
                checkHatchMin(errors, InputHatch, layers);
        }

        @Override
        public List<String> getStructureTooltipCasingRequirements(int minimum) {
            return List.of("At least " + minimum + " casings in total; increases with layers");
        }
    }

    public static final class SharedCasings extends Fixture<SharedCasings> {

        private static final IStructureDefinition<SharedCasings> STRUCTURE = StructureDefinition
            .<SharedCasings>builder()
            .addShape("main", new String[][] { { "A~BBB", "AAABB" } })
            .addElement(
                'A',
                GTStructureUtility.buildHatchAdder(SharedCasings.class)
                    .atLeast(Energy)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.cobblestone, 0)))
            .addElement(
                'B',
                GTStructureUtility.buildHatchAdder(SharedCasings.class)
                    .atLeast(InputBus)
                    .casingIndex(16)
                    .hint(1)
                    .buildAndChain(ofBlock(Blocks.brick_block, 0)))
            .build();

        public SharedCasings() {
            super("qa_shared_casings");
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new SharedCasings();
        }

        @Override
        public IStructureDefinition<SharedCasings> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            buildPiece("main", stack, hints, 1, 0, 0);
        }

        @Override
        protected void checkMachineHatches(List<StructureError> errors) {
            checkCasingMin(errors, 0, 6);
            checkHatchMin(errors, Energy, 1);
            checkHatchMin(errors, InputBus, 1);
        }

        @Override
        public List<String> getStructureTooltipCasingRequirements(int minimum) {
            return List.of("At least " + minimum + " casings in total");
        }
    }

    public static final class FailingStructure extends Fixture<FailingStructure> {

        private static final IStructureDefinition<FailingStructure> STRUCTURE = StructureDefinition
            .<FailingStructure>builder()
            .addShape("main", new String[][] { { "A~A" } })
            .addElement('A', ofBlock(Blocks.cobblestone, 0))
            .build();
        private final boolean failCounts;

        public FailingStructure(boolean failCounts) {
            super("qa_failing_structure");
            this.failCounts = failCounts;
        }

        @Override
        public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new FailingStructure(failCounts);
        }

        @Override
        public IStructureDefinition<FailingStructure> getStructureDefinition() {
            return STRUCTURE;
        }

        @Override
        public void construct(ItemStack stack, boolean hints) {
            if (!failCounts) throw new IllegalStateException("expected construction failure");
            buildPiece("main", stack, hints, 1, 0, 0);
        }

        @Override
        protected void checkMachineHatches(List<StructureError> errors) {
            throw new IllegalStateException("expected count collection failure");
        }
    }
}
