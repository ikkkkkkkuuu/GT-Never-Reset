package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.animation.Animator;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewport;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Interpolation;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.HorizontalScrollData;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.api.IMachineBreedingRequirement;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.xyp.gtneverreset.common.machine.multiblock.LargeCropBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingChance;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingRecipeRules;
import com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingSubstrate;
import com.xyp.gtnrlib.gui.widget.BranchProgressWidget;

import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.util.GTUtility;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;

public final class LargeCropBreederGui extends GTNRMultiBlockBaseGui<LargeCropBreeder> {

    private static final int WIDTH = 232, BODY_HEIGHT = 230, SPECIES_PER_PAGE = 30;
    private static final int ROUTE_WIDTH = WIDTH - 4, ROUTE_HEIGHT = BODY_HEIGHT - 18;
    private static final float MIN_ROUTE_ZOOM = 0.25F, MAX_ROUTE_ZOOM = 2F;
    private static final int JUNCTION_RISE = 24;
    private static final UITexture ROUTE_ARROW_EMPTY = new RouteArrowTexture(false, 0);
    private static final UITexture ROUTE_ARROW_FULL = new RouteArrowTexture(true, 0);
    private static final UITexture ROUTE_ARROW_COMPLETE = new RouteArrowTexture(true, 0xFF55DD55);
    private static final UITexture ROUTE_LINE_EMPTY = new RouteArrowTexture(false, 0, false);
    private static final UITexture ROUTE_LINE_FULL = new RouteArrowTexture(true, 0, false);
    private static final UITexture ROUTE_LINE_COMPLETE = new RouteArrowTexture(true, 0xFF55DD55, false);
    private final PagedWidget.Controller pages = new PagedWidget.Controller();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, Integer> cropChances = new HashMap<>();
    private final Map<String, Integer> cropVoltageTiers = new HashMap<>();
    private final Map<String, Integer> cropBreedingGrades = new HashMap<>();
    private final Map<NBTTagCompound, CatalystDisplay> catalystDisplays = new IdentityHashMap<>();
    private final Map<String, CropBreedingSubstrate> soilRequirements = new LinkedHashMap<>();
    private final Map<String, ItemStack> presentSubstrates = new HashMap<>();
    private List<NBTTagCompound> substrateSnapshot = Collections.emptyList();
    private int substrateRevision = Integer.MIN_VALUE;
    private long substrateTick = Long.MIN_VALUE;
    private String substrateTarget = "";
    private List<String> archive = Collections.emptyList(), reachable = Collections.emptyList();
    private List<NBTTagCompound> route = Collections.emptyList();
    private final NBTTagCompound productionStep = new NBTTagCompound();
    private final SpeciesView archiveView = new SpeciesView(() -> archive);
    private final SpeciesView reachableView = new SpeciesView(() -> reachable);
    private String draftText;
    private StringSyncValue draft, target, active;
    private BooleanSyncValue valid;
    private BooleanSyncValue washingMode;
    private BooleanSyncValue washingOperation;
    private IntSyncValue progress, duration, chance;
    private IntSyncValue parallel;
    private List<NBTTagCompound> chanceSnapshot;
    private BreedingRouteWidget routeWidget;
    private int snapshotRevision = Integer.MIN_VALUE;
    private List<NBTTagCompound> snapshot = Collections.emptyList();
    private int archiveRevision = Integer.MIN_VALUE;
    private List<NBTTagCompound> archiveSnapshot = Collections.emptyList();

    public LargeCropBreederGui(LargeCropBreeder machine) {
        super(machine);
        draftText = machine.getTargetCrop();
    }

    @Override
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        progress = sync.findSyncHandler("progressTime", IntSyncValue.class);
        duration = sync.findSyncHandler("maxProgressTime", IntSyncValue.class);
        draft = new StringSyncValue(
            () -> draftText,
            value -> draftText = value.substring(0, Math.min(256, value.length()))).allowC2S();
        target = new StringSyncValue(multiblock::getTargetCrop);
        active = new StringSyncValue(multiblock::getActiveCrop);
        valid = new BooleanSyncValue(multiblock::isTargetValid);
        washingMode = new BooleanSyncValue(multiblock::isWashingMode);
        sync.syncValue("cropWashingMode", washingMode);
        washingOperation = new BooleanSyncValue(multiblock::showsWashingOperation);
        sync.syncValue("cropWashingOperation", washingOperation);
        sync.syncValue(
            "cropSwitchMode",
            new InteractionSyncHandler().setOnMousePressed(
                mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0) multiblock.toggleWashingMode(); }));
        chance = new IntSyncValue(multiblock::getSuccessChance);
        parallel = new IntSyncValue(multiblock::getCurrentParallel);
        sync.syncValue("cropParallel", parallel);
        sync.syncValue("cropDraft", draft);
        sync.syncValue("cropTarget", target);
        sync.syncValue("cropActive", active);
        sync.syncValue("cropValid", valid);
        sync.syncValue("cropChance", chance);
        sync.syncValue("cropChances", new GenericListSyncHandler<>(this::chanceSnapshot, value -> {
            cropChances.clear();
            cropVoltageTiers.clear();
            cropBreedingGrades.clear();
            for (NBTTagCompound tag : value) {
                cropChances.put(tag.getString("id"), tag.getInteger("chance"));
                cropVoltageTiers.put(tag.getString("id"), tag.getInteger("voltageTier"));
                cropBreedingGrades.put(tag.getString("id"), tag.getInteger("breedingGrade"));
            }
        },
            ByteBufUtils::readTag,
            ByteBufUtils::writeTag,
            NBTTagCompound::equals,
            value -> (NBTTagCompound) value.copy()));
        sync.syncValue("cropApply", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.mouseButton != 0) return;
            if (mouse.isClient()) {
                String text = draftText.trim();
                if (!text.isEmpty() && CropRegistry.instance.get(text) == null) {
                    for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
                        if (name(crop.getId()).equalsIgnoreCase(text)) {
                            draft.setStringValue(crop.getId());
                            break;
                        }
                    }
                }
            } else {
                multiblock.setTargetCrop(draftText);
            }
        }));
        sync.syncValue("cropArchive", new GenericListSyncHandler<>(this::archiveSnapshot, value -> {
            List<String> crops = new ArrayList<>();
            icons.clear();
            for (NBTTagCompound tag : value) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
                ICropCard crop = stack == null ? null : CropRegistry.instance.get(stack, false);
                if (crop == null) continue;
                crops.add(crop.getId());
                icons.put(crop.getId(), stack);
            }
            archive = crops;
        },
            ByteBufUtils::readTag,
            ByteBufUtils::writeTag,
            NBTTagCompound::equals,
            value -> (NBTTagCompound) value.copy()));
        sync.syncValue(
            "cropReachable",
            new GenericListSyncHandler<>(
                () -> multiblock.getPlan().reachable,
                value -> reachable = value,
                ByteBufUtils::readUTF8String,
                ByteBufUtils::writeUTF8String,
                String::equals,
                value -> value));
        sync.syncValue("cropRoute", new GenericListSyncHandler<>(this::routeSnapshot, value -> {
            route = value;
            catalystDisplays.clear();
        },
            ByteBufUtils::readTag,
            ByteBufUtils::writeTag,
            NBTTagCompound::equals,
            value -> (NBTTagCompound) value.copy()));
        sync.syncValue("cropSubstrates", new GenericListSyncHandler<>(this::substrateSnapshot, value -> {
            presentSubstrates.clear();
            for (NBTTagCompound tag : value) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("present"));
                if (stack != null) presentSubstrates.put(tag.getString("key"), stack);
            }
        },
            ByteBufUtils::readTag,
            ByteBufUtils::writeTag,
            NBTTagCompound::equals,
            value -> (NBTTagCompound) value.copy()));
    }

    private List<NBTTagCompound> substrateSnapshot() {
        routeSnapshot();
        long tick = multiblock.getBaseMetaTileEntity()
            .getWorld()
            .getTotalWorldTime() / 10;
        if (substrateRevision == snapshotRevision && substrateTick == tick) return substrateSnapshot;
        substrateRevision = snapshotRevision;
        substrateTick = tick;
        List<NBTTagCompound> result = new ArrayList<>();
        soilRequirements.forEach((key, requirement) -> {
            ItemStack present = multiblock.findSubstrate(requirement);
            if (present == null) return;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("key", key);
            tag.setTag("present", present.writeToNBT(new NBTTagCompound()));
            result.add(tag);
        });
        return substrateSnapshot = result;
    }

    private List<NBTTagCompound> chanceSnapshot() {
        if (chanceSnapshot != null) return chanceSnapshot;
        chanceSnapshot = new ArrayList<>();
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", crop.getId());
            tag.setInteger("chance", CropBreedingChance.percent(crop));
            tag.setInteger("voltageTier", CropBreedingRecipeRules.requiredVoltageTier(crop));
            tag.setInteger("breedingGrade", CropBreedingRecipeRules.breedingGrade(crop));
            chanceSnapshot.add(tag);
        }
        return chanceSnapshot;
    }

    private List<NBTTagCompound> archiveSnapshot() {
        if (archiveRevision == multiblock.getRevision()) return archiveSnapshot;
        archiveRevision = multiblock.getRevision();
        List<NBTTagCompound> result = new ArrayList<>();
        for (String id : multiblock.getArchivedCrops()) {
            ItemStack seed = multiblock.getArchivedSeed(id);
            if (seed != null) result.add(seed.writeToNBT(new NBTTagCompound()));
        }
        return archiveSnapshot = result;
    }

    private List<NBTTagCompound> routeSnapshot() {
        if (snapshotRevision == multiblock.getRevision()) return snapshot;
        snapshotRevision = multiblock.getRevision();
        if (!substrateTarget.equals(multiblock.getTargetCrop())) {
            substrateTarget = multiblock.getTargetCrop();
            soilRequirements.clear();
        }
        List<NBTTagCompound> result = new ArrayList<>();
        Map<String, NBTTagList> parentSubstrates = new HashMap<>();
        for (ICropMutation mutation : multiblock.getPlan().steps) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                "result",
                mutation.getOutput()
                    .getId());
            int parent = 0;
            for (ICropCard crop : mutation.getParents()) {
                tag.setString("parent" + parent, crop.getId());
                NBTTagList substrates = parentSubstrates.computeIfAbsent(crop.getId(), id -> {
                    NBTTagList list = new NBTTagList();
                    for (CropBreedingSubstrate soil : CropBreedingSubstrate.forCrop(crop))
                        addRequirement(list, id, "growth", soil.alternatives(), soil);
                    return list;
                });
                tag.setTag("parentRequirements" + parent++, substrates);
            }
            tag.setLong("eu", CropBreedingRecipeRules.requiredVoltage(mutation.getOutput()));
            tag.setInteger("ticks", CropBreedingRecipeRules.baseDuration(mutation.getOutput()));
            NBTTagList requirements = new NBTTagList();
            for (CropBreedingSubstrate soil : CropBreedingSubstrate.forMutation(mutation))
                addRequirement(requirements, tag.getString("result"), "mutation", soil.alternatives(), soil);
            for (var condition : mutation.getRequirements()) {
                if (condition instanceof SubSoilRequirement) continue;
                if (!(condition instanceof IMachineBreedingRequirement machineRequirement)) continue;
                addRequirement(
                    requirements,
                    tag.getString("result"),
                    "mutation",
                    machineRequirement.getMachineOnlyCatalystsForNEI(),
                    null);
            }
            tag.setTag("requirements", requirements);
            result.add(tag);
        }
        return snapshot = result;
    }

    private void addRequirement(NBTTagList requirements, String crop, String scope, List<ItemStack> alternatives,
        CropBreedingSubstrate soil) {
        if (soil == null && (alternatives == null || alternatives.isEmpty())) return;
        NBTTagList options = new NBTTagList();
        if (alternatives != null) for (ItemStack stack : alternatives) {
            if (stack != null) options.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        NBTTagCompound requirement = new NBTTagCompound();
        String key = crop + "#" + scope + "#" + requirements.tagCount();
        requirement.setString("key", key);
        requirement.setString("crop", crop);
        requirement.setBoolean("soil", soil != null);
        requirement.setBoolean("automatic", soil != null && soil.isAutomaticallySatisfied());
        if (soil != null) soilRequirements.put(key, soil);
        requirement.setTag("options", options);
        requirements.appendTag(requirement);
    }

    @Override
    protected int getBasePanelWidth() {
        return WIDTH + 12;
    }

    @Override
    protected int getBasePanelHeight() {
        return 264;
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager sync) {
        Flow tabs = Flow.row()
            .size(WIDTH, 18)
            .marginBottom(4);
        // #tr gtnr.crop_breeder.run
        // # Run
        // # zh_CN 运行
        tabs.child(
            tab(0, "gtnr.crop_breeder.run", "gtnr.crop_breeder.run", GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON));
        // #tr gtnr.crop_breeder.archive
        // # Archive
        // # zh_CN 档案
        tabs.child(tab(1, "gtnr.crop_breeder.archive", "gtnr.crop_breeder.archive", new ItemDrawable(Items.book)));
        // #tr gtnr.crop_breeder.reachable
        // # Breedable
        // # zh_CN 可育
        // #tr gtnr.crop_breeder.tab_reachable
        // # Crops
        // # zh_CN 可育
        tabs.child(
            tab(2, "gtnr.crop_breeder.tab_reachable", "gtnr.crop_breeder.reachable", new ItemDrawable(Items.wheat)));
        // #tr gtnr.crop_breeder.route
        // # Route
        // # zh_CN 路线
        tabs.child(tab(3, "gtnr.crop_breeder.route", "gtnr.crop_breeder.route", new ItemDrawable(Items.map)));
        Flow inventory = Flow.column()
            .pos(0, 154)
            .size(WIDTH, 76)
            .child(createInventoryRow(panel, sync));
        ButtonWidget<?> modeButton = modeButton();
        PagedWidget<?> content = new PagedWidget<>().controller(pages)
            .size(WIDTH, BODY_HEIGHT)
            .addPage(operationPage(panel, sync))
            .addPage(speciesPage(archiveView))
            .addPage(speciesPage(reachableView))
            .addPage(routePage())
            .onPageChange(index -> {
                inventory.setEnabled(index != 3);
                modeButton.setEnabled(index == 0);
            });
        return Flow.column()
            .padding(6)
            .child(tabs)
            .child(
                new ParentWidget<>().size(WIDTH, BODY_HEIGHT)
                    .child(content)
                    .child(inventory)
                    .child(modeButton));
    }

    private ButtonWidget<?> modeButton() {
        IDrawable breedingIcon = new ItemDrawable(Items.wheat_seeds);
        IDrawable washingIcon = new ItemDrawable(Items.water_bucket);
        return new ButtonWidget<>().name("crop_mode_switch")
            .pos(WIDTH - 20, 154)
            .size(18, 18)
            .background(GTGuiTextures.BUTTON_STANDARD)
            .overlay(
                (context, x, y, width, height, theme) -> (washingMode.getBoolValue() ? washingIcon : breedingIcon)
                    .draw(context, x + 2, y + 2, width - 4, height - 4, theme))
            .tooltipAutoUpdate(true)
            .tooltipBuilder(t -> {
                // #tr gtnr.crop_breeder.mode_breeding
                // # Breeding mode. Click to switch to seed improvement.
                // # zh_CN 杂交模式，点击切换为洗种
                // #tr gtnr.crop_breeder.mode_washing
                // # Seed improvement mode. Click to switch to breeding.
                // # zh_CN 洗种模式，点击切换为杂交模式
                t.addLine(
                    tr(
                        washingMode.getBoolValue() ? "gtnr.crop_breeder.mode_washing"
                            : "gtnr.crop_breeder.mode_breeding"));
            })
            .syncHandler("cropSwitchMode");
    }

    private PageButton tab(int index, String key, String tooltipKey, IDrawable icon) {
        IKey label = IKey.lang(key)
            .scale(0.75F)
            .color(0xFF202020)
            .shadow(false);
        return new PageButton(index, pages).size(55, 18)
            .marginRight(index == 3 ? 0 : 4)
            .background(false, GTGuiTextures.BUTTON_STANDARD)
            .background(true, GTGuiTextures.BUTTON_STANDARD_PRESSED)
            .overlay((context, x, y, width, height, theme) -> {
                icon.draw(context, x + 3, y + 2, 14, 14, theme);
                label.draw(context, x + 19, y + 3, 34, 12, theme);
            })
            .tooltipBuilder(t -> t.addLine(IKey.lang(tooltipKey)));
    }

    private ParentWidget<?> operationPage(ModularPanel panel, PanelSyncManager sync) {
        ParentWidget<?> page = new ParentWidget<>().size(WIDTH, 148);
        page.child(
            new CropDropTextField().value(draft)
                .setMaxLength(256)
                .autoUpdateOnChange(true)
                .pos(0, 0)
                .size(184, 18)
                .tooltipBuilder(t -> {
                    // #tr gtnr.crop_breeder.target_hint
                    // # Enter a crop name / ID, or drop a seed from NEI. Apply an empty field to clear.
                    // # zh_CN 输入作物名称 / ID，或从 NEI 拖入种子；留空应用可清除目标。
                    t.addLine(IKey.lang("gtnr.crop_breeder.target_hint"));
                }));
        page.child(
            new ButtonWidget<>().pos(188, 0)
                .size(44, 18)
                .background(GTGuiTextures.BUTTON_STANDARD)
                // #tr gtnr.crop_breeder.apply
                // # Apply
                // # zh_CN 应用
                .overlay(IKey.lang("gtnr.crop_breeder.apply"))
                .syncHandler("cropApply"));
        page.child(label(() -> {
            if (!valid.getBoolValue()) {
                // #tr gtnr.crop_breeder.invalid
                // # Unknown crop; target unchanged.
                // # zh_CN 未找到该作物，目标未改变。
                return tr("gtnr.crop_breeder.invalid");
            }
            // #tr gtnr.crop_breeder.target
            // # Target: %s
            // # zh_CN 目标：%s
            return tr("gtnr.crop_breeder.target", name(target.getStringValue()));
        }, 0, 22, WIDTH));
        ParentWidget<?> breeding = new ParentWidget<>().pos(0, 38)
            .size(WIDTH, 20)
            .setEnabledIf(widget -> !washingOperation.getBoolValue());
        addStep(breeding, this::breedingStep, 0);
        page.child(breeding);
        ParentWidget<?> washing = new ParentWidget<>().pos(0, 38)
            .size(WIDTH, 20)
            .setEnabledIf(widget -> washingOperation.getBoolValue());
        Supplier<String> washingCrop = () -> duration.getIntValue() > 0 ? active.getStringValue()
            : target.getStringValue();
        washing.child(cropSlot(washingCrop, 0, 0));
        washing.child(label(() -> {
            ItemStack seed = icons.get(washingCrop.get());
            if (seed == null || !archive.contains(washingCrop.get())) {
                // #tr gtnr.crop_breeder.wash_select
                // # Select an archived crop to improve
                // # zh_CN 请选择已入档的作物洗种
                return tr("gtnr.crop_breeder.wash_select");
            }
            SeedStats stats = SeedStats.getStatsFromStack(seed);
            // #tr gtnr.crop_breeder.wash_stats
            // # Archived stats: %s / %s / %s
            // # zh_CN 档案属性：%s / %s / %s
            return tr("gtnr.crop_breeder.wash_stats", stats.getGrowth(), stats.getGain(), stats.getResistance());
        }, 24, 5, WIDTH - 24));
        page.child(washing);
        page.child(
            label(
                () -> tr(
                    // #tr gtnr.crop_breeder.status
                    // # Chance %s%% | Archive %s | Steps %s
                    // # zh_CN 成功率 %s%% | 档案 %s | 剩余 %s 步
                    "gtnr.crop_breeder.status",
                    chance.getIntValue(),
                    archive.size(),
                    route.size()),
                0,
                60,
                WIDTH).setEnabledIf(widget -> !washingOperation.getBoolValue()));
        // #tr gtnr.crop_breeder.parallel
        // # Parallel %s
        // # zh_CN 并行 %s
        page.child(label(() -> tr("gtnr.crop_breeder.parallel", parallel.getIntValue()), 0, 69, WIDTH));
        page.child(createTerminalRow(panel, sync).pos(0, 78));
        return page;
    }

    private NBTTagCompound breedingStep() {
        String activeId = duration.getIntValue() > 0 ? activeBreedingCrop() : "";
        if (!route.isEmpty() && (activeId.isEmpty() || activeId.equals(
            route.get(0)
                .getString("result"))))
            return route.get(0);
        String id = activeId.isEmpty() ? target.getStringValue() : activeId;
        boolean archived = archive.contains(id);
        if (!archived && activeId.isEmpty()) id = "";
        String parent = archived ? id : "";
        if (!id.equals(productionStep.getString("result")) || !parent.equals(productionStep.getString("parent0"))) {
            ICropCard crop = CropRegistry.instance.get(id);
            productionStep.setString("parent0", parent);
            productionStep.setString("result", id);
            productionStep.setLong("eu", crop == null ? 0 : CropBreedingRecipeRules.requiredVoltage(crop));
        }
        return productionStep;
    }

    @Override
    protected int getTerminalRowWidth() {
        return WIDTH;
    }

    @Override
    protected int getTerminalRowHeight() {
        return 60;
    }

    @Override
    protected int getTerminalWidgetWidth() {
        return WIDTH;
    }

    @Override
    protected int getTerminalWidgetHeight() {
        return 60;
    }

    @Override
    protected Flow createTerminalRightCornerColumn(ModularPanel panel, PanelSyncManager sync) {
        return Flow.row()
            .coverChildren()
            .rightRel(0, 6, 0)
            .bottomRel(0, 6, 0)
            .childIf(multiblock.supportsShutdownReasonHoverable(), () -> createShutdownReasonHoverableTerminal(sync))
            .childIf(multiblock.supportsMaintenanceIssueHoverable(), () -> createMaintIssueHoverableTerminal(sync))
            .childIf(multiblock.supportsLogo(), () -> makeLogoWidget(sync, panel));
    }

    private ParentWidget<?> speciesPage(SpeciesView view) {
        ParentWidget<?> page = new ParentWidget<>().size(WIDTH, 114);
        page.child(
            new TextFieldWidget().value(view.search)
                .setMaxLength(128)
                .autoUpdateOnChange(true)
                .size(WIDTH, 18)
                .tooltipBuilder(t -> {
                    // #tr gtnr.crop_breeder.search
                    // # Search name / ID. Click a crop to fill the target, then Apply.
                    // # zh_CN 搜索名称 / ID；点击作物填入目标，再点击应用。
                    t.addLine(IKey.lang("gtnr.crop_breeder.search"));
                }));
        for (int i = 0; i < SPECIES_PER_PAGE; i++) {
            final int slot = i;
            page.child(
                cropSlot(() -> view.id(slot), 7 + i % 10 * 22, 24 + i / 10 * 22).setEnabledIf(
                    widget -> !view.id(slot)
                        .isEmpty())
                    .onMousePressed(button -> {
                        if (button == 0) {
                            draft.setStringValue(view.id(slot));
                            pages.setPage(0);
                        }
                        return true;
                    }));
        }
        // #tr gtnr.crop_breeder.empty
        // # No matching crops
        // # zh_CN 没有符合条件的作物
        page.child(
            label(() -> tr("gtnr.crop_breeder.empty"), 6, 47, WIDTH - 12).setEnabledIf(
                widget -> view.filtered()
                    .isEmpty()));
        page.child(button("<", 0, 96, () -> view.page = Math.max(0, view.page - 1)));
        page.child(button(">", WIDTH - 18, 96, () -> view.page = Math.min(view.pageCount() - 1, view.page + 1)));
        page.child(
            label(
                () -> tr(
                    // #tr gtnr.crop_breeder.page
                    // # %s entries | %s / %s
                    // # zh_CN 共 %s 项 | %s / %s 页
                    "gtnr.crop_breeder.page",
                    view.filtered()
                        .size(),
                    view.page + 1,
                    view.pageCount()),
                24,
                98,
                WIDTH - 48));
        return page;
    }

    private ParentWidget<?> routePage() {
        ParentWidget<?> page = new ParentWidget<>().size(WIDTH, BODY_HEIGHT);
        VerticalScrollData vertical = new VerticalScrollData();
        HorizontalScrollData horizontal = new HorizontalScrollData();
        ScrollWidget<?> viewport = new BreedingRouteViewport(vertical).showScrollShadows(false)
            .pos(0, 18)
            .size(WIDTH, ROUTE_HEIGHT)
            .background(IDrawable.EMPTY);
        viewport.getScrollArea()
            .setScrollDataX(horizontal);
        routeWidget = new BreedingRouteWidget(viewport, vertical, horizontal);
        viewport.child(routeWidget);
        page.child(
            label(
                () -> tr(
                    // #tr gtnr.crop_breeder.route_summary
                    // # Breeding lineage | %s steps remaining
                    // # zh_CN 杂交谱系 | 剩余 %s 步
                    "gtnr.crop_breeder.route_summary",
                    route.size()),
                0,
                2,
                WIDTH - 24).tooltipBuilder(t -> {
                    // #tr gtnr.crop_breeder.route_hint
                    // # Drag/scroll: pan. Ctrl+scroll: zoom. Middle-click: reset. Green: archived. Fill: progress.
                    // # zh_CN 拖动或滚动查看谱系；Ctrl + 滚轮缩放，中键重置视图。绿色箭头表示作物已入档，填充箭头表示本轮进度。
                    t.addLine(IKey.lang("gtnr.crop_breeder.route_hint"));
                }));
        page.child(
            new ButtonWidget<>().pos(WIDTH - 18, 0)
                .size(18)
                .background(GTGuiTextures.BUTTON_STANDARD)
                .overlay(new ItemDrawable(Items.compass).asIcon())
                // #tr gtnr.crop_breeder.focus_target
                // # Focus target crop
                // # zh_CN 定位目标作物
                .tooltipBuilder(t -> t.addLine(IKey.lang("gtnr.crop_breeder.focus_target")))
                .onMousePressed(button -> {
                    if (button == 0) routeWidget.focusTarget();
                    return true;
                }));
        page.child(viewport);
        page.child(
            label(
                () -> tr(
                    // #tr gtnr.crop_breeder.no_route
                    // # Select a target and supply analyzed parent seeds.
                    // # zh_CN 请选择目标并投入已分析的亲本种子。
                    "gtnr.crop_breeder.no_route"),
                0,
                30,
                WIDTH).setEnabledIf(
                    widget -> route.isEmpty() && routeWidget.history.isEmpty()
                        && !archive.contains(target.getStringValue())));
        return page;
    }

    private double routeProgress(String id) {
        if (duration.getIntValue() > 0 && id.equals(activeBreedingCrop()))
            return Math.min(1D, progress.getIntValue() / (double) duration.getIntValue());
        return archive.contains(id) ? 1D : 0D;
    }

    private BranchProgressWidget routeConnection(RouteNode node) {
        int[] sources = new int[node.parents.size()];
        for (int i = 0; i < sources.length; i++) sources[i] = node.parents.get(i).x + 9;
        return new BranchProgressWidget()
            .layout(node.x + 9, node.y + node.height() + 2, node.junctionY(), node.parents.get(0).y - 2, sources)
            .textures(ROUTE_LINE_EMPTY, ROUTE_LINE_FULL, ROUTE_ARROW_EMPTY, ROUTE_ARROW_FULL)
            .completedTextures(ROUTE_LINE_COMPLETE, ROUTE_ARROW_COMPLETE)
            .value(() -> routeProgress(node.uid))
            .completed(() -> archive.contains(node.uid) && !node.uid.equals(activeBreedingCrop()))
            .tooltipAutoUpdate(true)
            .tooltipBuilder(
                tooltip -> tooltip.addLine(name(node.uid))
                    .addLine(tr("gtnr.crop_breeder.crop_chance", effectiveChance(node.uid))));
    }

    private void addStep(ParentWidget<?> page, Supplier<NBTTagCompound> step, int y) {
        for (int i = 0; i < 4; i++) {
            final int parent = i;
            page.child(
                cropSlot(
                    () -> step.get()
                        .getString("parent" + parent),
                    i * 28,
                    y));
        }
        page.child(new ProgressWidget().value(new DoubleValue.Dynamic(() -> {
            if (duration.getIntValue() <= 0 || !step.get()
                .getString("result")
                .equals(activeBreedingCrop())) return 0D;
            return Math.min(1D, progress.getIntValue() / (double) duration.getIntValue());
        }, null))
            .texture(GTGuiTextures.PROGRESSBAR_ARROW_STANDARD, 20)
            .direction(ProgressWidget.Direction.RIGHT)
            .pos(120, y)
            .size(20, 18));
        page.child(
            cropSlot(
                () -> step.get()
                    .getString("result"),
                154,
                y,
                () -> GTUtility.getTier(
                    step.get()
                        .getLong("eu"))).tooltipBuilder(t -> {
                            NBTTagList requirements = step.get()
                                .getTagList("requirements", 10);
                            for (int i = 0; i < requirements.tagCount(); i++) {
                                t.addLine(requirementTooltipLine(requirements.getCompoundTagAt(i)));
                            }
                        }));
        page.child(label(() -> {
            String id = step.get()
                .getString("result");
            return id.isEmpty() ? "" : effectiveChance(id) + "%";
        }, 177, y + 5, WIDTH - 177));
    }

    private SlotLikeButtonWidget cropSlot(Supplier<String> id, int x, int y) {
        return cropSlot(id, x, y, () -> cropVoltageTiers.getOrDefault(id.get(), VoltageIndex.LV));
    }

    private SlotLikeButtonWidget cropSlot(Supplier<String> id, int x, int y, IntSupplier requiredTier) {
        return new SlotLikeButtonWidget(() -> icon(id.get())).pos(x, y)
            .size(18)
            .tooltipAutoUpdate(true)
            .tooltipBuilder(t -> {
                ItemStack stack = icon(id.get());
                if (stack == null) return;
                t.addFromItem(stack);
                // #tr gtnr.crop_breeder.breeding_grade
                // # Breeding grade: %s
                // # zh_CN 杂交等级：%s 级
                t.addLine(tr("gtnr.crop_breeder.breeding_grade", cropBreedingGrades.getOrDefault(id.get(), 1)));
                // #tr gtnr.crop_breeder.required_voltage
                // # Required breeding voltage: %s
                // # zh_CN 所需杂交电压：%s
                t.addLine(
                    tr(
                        "gtnr.crop_breeder.required_voltage",
                        GTValues.VN[Math
                            .max(VoltageIndex.HV, Math.min(GTValues.VN.length - 1, requiredTier.getAsInt()))]));
                if (!cropChances.containsKey(id.get())) return;
                // #tr gtnr.crop_breeder.crop_chance
                // # Breeding success chance: %s%%
                // # zh_CN 杂交成功率：%s%%
                t.addLine(tr("gtnr.crop_breeder.crop_chance", effectiveChance(id.get())));
            });
    }

    private String activeBreedingCrop() {
        return washingOperation.getBoolValue() ? "" : active.getStringValue();
    }

    private int effectiveChance(String id) {
        if (duration.getIntValue() > 0 && id.equals(activeBreedingCrop())) return chance.getIntValue();
        return cropChances.getOrDefault(id, 0);
    }

    private static String requirementLabel(NBTTagCompound requirement) {
        // #tr gtnr.crop_breeder.soil_automatic
        // # Vanilla substrate alternative: automatically satisfied; no item needed
        // # zh_CN 有原版底土可选：自动满足，无需放入
        if (requirement.getBoolean("automatic")) return tr("gtnr.crop_breeder.soil_automatic");
        // #tr gtnr.crop_breeder.soil_requirement
        // # Substrate: checked in input bus, not consumed
        // # zh_CN 底土：放入输入总线，仅检测、不消耗
        // #tr gtnr.crop_breeder.catalyst_requirement
        // # Catalyst: consumed each attempt
        // # zh_CN 催化剂：每次尝试消耗
        return tr(
            requirement.getBoolean("soil") ? "gtnr.crop_breeder.soil_requirement"
                : "gtnr.crop_breeder.catalyst_requirement");
    }

    private CatalystDisplay catalystDisplay(NBTTagCompound requirement) {
        return catalystDisplays.computeIfAbsent(requirement, tag -> new CatalystDisplay(tag.getTagList("options", 10)));
    }

    private boolean hasSubstrateCheckmark(NBTTagCompound requirement) {
        return requirement.getBoolean("soil")
            && (requirement.getBoolean("automatic") || archive.contains(requirement.getString("crop"))
                || presentSubstrates.containsKey(requirement.getString("key")));
    }

    private ItemStack requirementItem(NBTTagCompound requirement) {
        ItemStack present = requirement.getBoolean("soil") ? presentSubstrates.get(requirement.getString("key")) : null;
        return present == null ? catalystDisplay(requirement).get() : present;
    }

    private String requirementTooltipLine(NBTTagCompound requirement) {
        boolean soil = requirement.getBoolean("soil");
        ItemStack display = requirementItem(requirement);
        // #tr gtnr.crop_breeder.requirement_unavailable
        // # No registered matching item
        // # zh_CN 没有已注册的对应物品
        String name = display == null ? tr("gtnr.crop_breeder.requirement_unavailable") : display.getDisplayName();
        if (soil) {
            // #tr gtnr.crop_breeder.required_soil
            // # Required substrate: %s
            // # zh_CN 所需底土：%s
            return tr("gtnr.crop_breeder.required_soil", name)
                + (hasSubstrateCheckmark(requirement) ? " " + EnumChatFormatting.GREEN + "✔" + EnumChatFormatting.RESET
                    : "");
        }
        // #tr gtnr.crop_breeder.required_catalyst
        // # Required catalyst: %s × %s (consumed)
        // # zh_CN 所需催化剂：%s × %s（消耗）
        return tr("gtnr.crop_breeder.required_catalyst", name, display == null ? 0 : display.stackSize);
    }

    private ItemDisplayWidget catalystIcon(NBTTagCompound requirement) {
        CatalystDisplay display = catalystDisplay(requirement);
        return new SearchableItemDisplayWidget()
            .item(new ObjectValue.Dynamic<>(ItemStack.class, () -> requirementItem(requirement), value -> {}))
            .size(16)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .overlay((context, x, y, width, height, theme) -> {
                if (hasSubstrateCheckmark(requirement))
                    GuiDraw.drawText("✔", x + width - 7, y + height - 8, 0.8F, 0xFF55FF55, true);
            })
            .tooltipAutoUpdate(true)
            .tooltipBuilder(t -> {
                ItemStack stack = requirementItem(requirement);
                if (stack != null) t.addFromItem(stack);
                t.addLine(requirementLabel(requirement));
                if (requirement.getBoolean("soil")) {
                    // #tr gtnr.crop_breeder.substrate_archived
                    // # Crop archived
                    // # zh_CN 作物已入档
                    if (archive.contains(requirement.getString("crop")))
                        t.addLine(EnumChatFormatting.GREEN + tr("gtnr.crop_breeder.substrate_archived"));
                    // #tr gtnr.crop_breeder.substrate_detected
                    // # Substrate detected in input bus
                    // # zh_CN 输入总线已检测到底土
                    // #tr gtnr.crop_breeder.substrate_missing
                    // # Substrate not detected in input bus
                    // # zh_CN 输入总线未检测到底土
                    if (!requirement.getBoolean("automatic")) t.addLine(
                        tr(
                            presentSubstrates.containsKey(requirement.getString("key"))
                                ? "gtnr.crop_breeder.substrate_detected"
                                : "gtnr.crop_breeder.substrate_missing"));
                }
                // #tr gtnr.crop_breeder.alternatives
                // # Any one of %s alternatives; required count: %s
                // # zh_CN 以下 %s 种任选一种；所需数量：%s
                if (!requirement.getBoolean("automatic")) t.addLine(
                    tr(
                        "gtnr.crop_breeder.alternatives",
                        display.alternatives.length,
                        stack == null ? 0 : stack.stackSize));
            });
    }

    private static final class SearchableItemDisplayWidget extends ItemDisplayWidget {

        private final Slot neiSlot = new Slot(new InventoryBasic("", false, 1), 0, 0, 0);

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            if (!ModularUI.Mods.NEI.isLoaded()) return;
            GuiContainerManager manager = GuiContainerManager.getManager();
            var screen = getScreen().getScreenWrapper()
                .getGuiScreen();
            if (manager == null || !(screen instanceof GuiContainer container) || manager.window != container) return;
            neiSlot.putStack(getStackForRecipeViewer());
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(1, 1, 0);
                GL11.glScalef((getArea().width - 2) / 16F, (getArea().height - 2) / 16F, 1);
                manager.renderSlotOverlay(neiSlot);
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    private static final class CatalystDisplay implements Supplier<ItemStack> {

        private final ItemStack[] alternatives;

        private CatalystDisplay(NBTTagList options) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < options.tagCount(); i++) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(options.getCompoundTagAt(i));
                if (stack != null) stacks.add(stack);
            }
            alternatives = stacks.isEmpty() ? new ItemStack[0] : new PositionedStack(stacks, 0, 0).items;
        }

        @Override
        public ItemStack get() {
            return alternatives.length == 0 ? null
                : alternatives[(int) (System.currentTimeMillis() / 1000 % alternatives.length)];
        }
    }

    private ItemStack icon(String id) {
        return icons.computeIfAbsent(id, key -> {
            ICropCard crop = CropRegistry.instance.get(key);
            return crop == null ? null
                : crop.getSeedItem(com.xyp.gtneverreset.common.machine.multiblock.crop.ArchivedSeedStats.initial());
        });
    }

    private static ButtonWidget<?> button(String text, int x, int y, Runnable action) {
        return new ButtonWidget<>().pos(x, y)
            .size(18, 16)
            .background(GTGuiTextures.BUTTON_STANDARD)
            .overlay(IKey.str(text))
            .onMousePressed(mouse -> {
                if (mouse == 0) action.run();
                return true;
            });
    }

    private static Widget<?> label(Supplier<String> text, int x, int y, int width) {
        return new Widget<>().pos(x, y)
            .size(width, 12)
            .overlay((context, left, top, w, h, theme) -> {
                String full = text.get();
                String visible = context.getFontRenderer()
                    .getStringWidth(full) * 0.8F > w ? context.getFontRenderer()
                        .trimStringToWidth(full, (int) (w / 0.8F) - 6) + "…" : full;
                GuiDraw.drawText(visible, left, top + 2, 0.8F, 0xFF202020, false);
            })
            .tooltipAutoUpdate(true)
            .tooltipBuilder(t -> t.addLine(text.get()));
    }

    private static String name(String id) {
        ICropCard crop = CropRegistry.instance.get(id);
        return crop == null ? id : StatCollector.translateToLocal(crop.getUnlocalizedName());
    }

    private static String tr(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private final class BreedingRouteWidget extends ParentWidget<BreedingRouteWidget>
        implements IViewport, Interactable {

        private final ScrollWidget<?> viewport;
        private final VerticalScrollData vertical;
        private final HorizontalScrollData horizontal;
        private List<NBTTagCompound> lastSteps;
        private String lastTarget = "";
        private boolean lastTargetArchived;
        private final Map<String, NBTTagCompound> history = new LinkedHashMap<>();
        private RouteLayout layout = new RouteLayout(Collections.emptyList(), "");
        private final Animator zoomAnimation = new Animator().duration(160)
            .curve(Interpolation.QUAD_OUT)
            .onUpdate(this::applyZoom);
        private float zoom = 1F;
        private float targetZoom = 1F;
        private float zoomAnchorX;
        private float zoomAnchorY;
        private float zoomContentX;
        private float zoomContentY;
        private int dragX;
        private int dragY;
        private int startScrollX;
        private int startScrollY;

        private BreedingRouteWidget(ScrollWidget<?> viewport, VerticalScrollData vertical,
            HorizontalScrollData horizontal) {
            this.viewport = viewport;
            this.vertical = vertical;
            this.horizontal = horizontal;
            pos(0, 0).size(ROUTE_WIDTH, ROUTE_HEIGHT - 4);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            String currentTarget = target.getStringValue();
            boolean targetArchived = archive.contains(currentTarget);
            if (lastSteps == route && lastTarget.equals(currentTarget) && lastTargetArchived == targetArchived) return;
            boolean targetChanged = !lastTarget.equals(currentTarget) || lastSteps == null
                || (lastSteps.isEmpty() && !route.isEmpty());
            lastSteps = route;
            lastTarget = currentTarget;
            lastTargetArchived = targetArchived;
            if (targetChanged) {
                stopZoom();
                history.clear();
            }
            for (NBTTagCompound step : route) history.put(step.getString("result"), step);
            layout = new RouteLayout(
                new ArrayList<>(history.values()),
                history.isEmpty() && !targetArchived ? "" : currentTarget);
            removeAll();
            for (RouteNode node : layout.nodes.values()) {
                if (!node.parents.isEmpty()) child(routeConnection(node));
            }
            for (RouteNode node : layout.nodes.values()) {
                child(
                    cropSlot(
                        () -> node.uid,
                        node.x,
                        node.y,
                        () -> node.step == null ? cropVoltageTiers.getOrDefault(node.uid, VoltageIndex.LV)
                            : GTUtility.getTier(node.step.getLong("eu"))).tooltipBuilder(t -> {
                                NBTTagList requirements = node.requirements;
                                for (int i = 0; i < requirements.tagCount(); i++) {
                                    t.addLine(requirementTooltipLine(requirements.getCompoundTagAt(i)));
                                }
                            }));
                NBTTagList requirements = node.requirements;
                for (int i = 0; i < requirements.tagCount(); i++) {
                    child(catalystIcon(requirements.getCompoundTagAt(i)).pos(node.x + 1, node.y + 18 + i * 16));
                }
            }
            updateCanvasSize();
            if (targetChanged) focusTarget();
            else {
                vertical.clamp(viewport.getScrollArea());
                horizontal.clamp(viewport.getScrollArea());
            }
        }

        private void focusTarget() {
            stopZoom();
            RouteNode node = layout.nodes.get(target.getStringValue());
            vertical.scrollTo(viewport.getScrollArea(), node == null ? 0 : Math.round((node.y - 10) * zoom));
            horizontal.scrollTo(
                viewport.getScrollArea(),
                node == null ? 0
                    : Math.round(contentOffsetX() + (node.x + 9) * zoom)
                        - horizontal.getFullVisibleSize(viewport.getScrollArea()) / 2);
        }

        private void resetView() {
            stopZoom();
            zoom = targetZoom = 1F;
            updateCanvasSize();
            focusTarget();
        }

        @Override
        public void transformChildren(IViewportStack stack) {
            stack.translate(contentOffsetX(), 0);
            stack.scale(zoom, zoom);
        }

        private float contentOffsetX() {
            return Math.max(0F, (ROUTE_WIDTH - layout.width * zoom) / 2F);
        }

        private void setZoom(float value, float anchorX, float anchorY) {
            float minimumZoom = Math
                .min(MIN_ROUTE_ZOOM, Math.min((ROUTE_WIDTH - 8F) / layout.width, (ROUTE_HEIGHT - 12F) / layout.height));
            float nextZoom = Math.max(minimumZoom, Math.min(MAX_ROUTE_ZOOM, value));
            if (nextZoom == targetZoom) return;
            zoomAnimation.stop(true);
            zoomAnchorX = anchorX;
            zoomAnchorY = anchorY;
            zoomContentX = (horizontal.getScroll() + anchorX - contentOffsetX()) / zoom;
            zoomContentY = (vertical.getScroll() + anchorY) / zoom;
            targetZoom = nextZoom;
            zoomAnimation.bounds(zoom, targetZoom)
                .animate();
        }

        private void applyZoom(double value) {
            zoom = (float) value;
            updateCanvasSize();
            horizontal
                .scrollTo(viewport.getScrollArea(), Math.round(contentOffsetX() + zoomContentX * zoom - zoomAnchorX));
            vertical.scrollTo(viewport.getScrollArea(), Math.round(zoomContentY * zoom - zoomAnchorY));
        }

        private void updateCanvasSize() {
            int width = Math.max(ROUTE_WIDTH, (int) Math.ceil(layout.width * zoom));
            int height = Math.max(ROUTE_HEIGHT - 4, (int) Math.ceil(layout.height * zoom));
            size(width, height);
            getArea().setSize(width, height);
            horizontal.setScrollSize(width);
            vertical.setScrollSize(height);
            viewport.scheduleResize();
            scheduleResize();
        }

        private void stopZoom() {
            zoomAnimation.stop(true);
            targetZoom = zoom;
        }

        @Override
        public void dispose() {
            stopZoom();
            super.dispose();
        }

        @Override
        public @NotNull Result onMousePressed(int button) {
            if (button != 0) return Result.IGNORE;
            stopZoom();
            dragX = getContext().getAbsMouseX();
            dragY = getContext().getAbsMouseY();
            startScrollX = horizontal.getScroll();
            startScrollY = vertical.getScroll();
            return Result.SUCCESS;
        }

        @Override
        public void onMouseDrag(int button, long timeSinceClick) {
            if (button == 0) {
                vertical.scrollTo(viewport.getScrollArea(), startScrollY + dragY - getContext().getAbsMouseY());
                horizontal.scrollTo(viewport.getScrollArea(), startScrollX + dragX - getContext().getAbsMouseX());
            }
        }
    }

    private final class BreedingRouteViewport extends ScrollWidget<BreedingRouteViewport> {

        private final Area contentClip = new Area();

        private BreedingRouteViewport(VerticalScrollData vertical) {
            super(vertical);
        }

        @Override
        public @NotNull Result onMousePressed(int button) {
            if (button == 2) {
                routeWidget.resetView();
                return Result.SUCCESS;
            }
            if (button == 0) routeWidget.stopZoom();
            return super.onMousePressed(button);
        }

        @Override
        public boolean onMouseScroll(UpOrDown direction, int amount) {
            if (Interactable.hasControlDown()) {
                routeWidget.setZoom(
                    routeWidget.targetZoom * (float) Math.pow(1.2D, direction.modifier),
                    getContext().getMouseX(),
                    getContext().getMouseY());
                return true;
            }
            routeWidget.stopZoom();
            var area = getScrollArea();
            var scroll = area.getScrollX()
                .isScrollBarActive(area)
                && (Interactable.hasShiftDown() || !area.getScrollY()
                    .isScrollBarActive(area)) ? area.getScrollX() : area.getScrollY();
            if (!scroll.isScrollBarActive(area)) return false;
            int previous = scroll.getScroll();
            scroll.scrollBy(area, -direction.modifier * scroll.getScrollSpeed());
            return previous != scroll.getScroll() || scroll.isCancelScrollEdge();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void preDraw(ModularGuiContext context, boolean transformed) {
            if (!transformed) {
                var area = getScrollArea();
                contentClip.setSize(
                    area.getScrollX()
                        .getFullVisibleSize(area),
                    area.getScrollY()
                        .getFullVisibleSize(area));
                Stencil.applyAtZero(contentClip, context);
            }
        }
    }

    private static final class RouteLayout {

        private final Map<String, RouteNode> nodes = new LinkedHashMap<>();
        private final int width;
        private final int height;

        private RouteLayout(List<NBTTagCompound> steps, String target) {
            Map<String, NBTTagCompound> byResult = new HashMap<>();
            for (NBTTagCompound step : steps) byResult.put(step.getString("result"), step);
            RouteNode root = new RouteNode(target);
            ArrayDeque<RouteNode> pending = new ArrayDeque<>();
            if (!target.isEmpty()) {
                nodes.put(target, root);
                pending.add(root);
            }
            Set<String> expanded = new HashSet<>();
            while (!pending.isEmpty()) {
                RouteNode result = pending.remove();
                result.step = byResult.get(result.uid);
                if (result.step != null) result.requirements = result.step.getTagList("requirements", 10);
                if (result.step == null || !expanded.add(result.uid)) continue;
                for (int parentIndex = 0; parentIndex < 4; parentIndex++) {
                    String uid = result.step.getString("parent" + parentIndex);
                    if (uid.isEmpty() || uid.equals(result.uid)) continue;
                    if (result.parents.stream()
                        .anyMatch(parent -> parent.uid.equals(uid))) continue;
                    RouteNode parent = new RouteNode(uid);
                    parent.depth = result.depth + 1;
                    parent.requirements = result.step.getTagList("parentRequirements" + parentIndex, 10);
                    result.parents.add(parent);
                    nodes.put("#parent:" + nodes.size(), parent);
                    pending.add(parent);
                }
            }
            List<RouteNode> ordered = new ArrayList<>(nodes.values());
            for (int i = ordered.size() - 1; i >= 0; i--) layoutParents(ordered.get(i));
            int left = 0, right = 0;
            for (int value : root.leftContour) left = Math.min(left, value);
            for (int value : root.rightContour) right = Math.max(right, value);
            width = Math.max(ROUTE_WIDTH, right - left + 54);
            root.x = (width - (right - left)) / 2 - left - 9;
            for (RouteNode node : ordered) {
                for (int i = 0; i < node.parents.size(); i++) {
                    node.parents.get(i).x = node.x + (2 * i - node.parents.size() + 1) * node.parentSpacing / 2;
                }
            }
            Map<Integer, Integer> rowHeights = new HashMap<>();
            Set<Integer> branchingRows = new HashSet<>();
            for (RouteNode node : ordered) {
                rowHeights.merge(node.depth, node.height(), Math::max);
                if (node.parents.size() >= 2) branchingRows.add(node.depth);
            }
            Map<Integer, Integer> rowPositions = new HashMap<>();
            int rowY = 10;
            for (int depth = 0; depth < rowHeights.size(); depth++) {
                rowPositions.put(depth, rowY);
                rowY += rowHeights.get(depth) + 46 + (branchingRows.contains(depth) ? JUNCTION_RISE : 0);
            }
            int bottom = 0;
            for (int i = ordered.size() - 1; i >= 0; i--) {
                RouteNode node = ordered.get(i);
                node.y = rowPositions.get(node.depth);
                node.rowContentHeight = rowHeights.get(node.depth);
                bottom = Math.max(bottom, node.y + node.height() + 10);
            }
            height = Math.max(ROUTE_HEIGHT - 4, bottom);
        }

        private static void layoutParents(RouteNode node) {
            if (node.parents.isEmpty()) return;
            int spacing = 54, levels = 0;
            for (int i = 0; i < node.parents.size(); i++) {
                RouteNode first = node.parents.get(i);
                levels = Math.max(levels, first.leftContour.length);
                for (int j = i + 1; j < node.parents.size(); j++) {
                    RouteNode second = node.parents.get(j);
                    for (int depth = 0; depth
                        < Math.min(first.rightContour.length, second.leftContour.length); depth++) {
                        int gap = first.rightContour[depth] - second.leftContour[depth] + 54;
                        spacing = Math.max(spacing, (int) Math.ceil(gap / (double) (j - i)));
                    }
                }
            }
            node.parentSpacing = (spacing + 1) / 2 * 2;
            node.leftContour = new int[levels + 1];
            node.rightContour = new int[levels + 1];
            for (int depth = 1; depth <= levels; depth++) {
                node.leftContour[depth] = Integer.MAX_VALUE;
                node.rightContour[depth] = Integer.MIN_VALUE;
            }
            for (int i = 0; i < node.parents.size(); i++) {
                RouteNode parent = node.parents.get(i);
                int offset = (2 * i - node.parents.size() + 1) * node.parentSpacing / 2;
                for (int depth = 0; depth < parent.leftContour.length; depth++) {
                    node.leftContour[depth + 1] = Math
                        .min(node.leftContour[depth + 1], offset + parent.leftContour[depth]);
                    node.rightContour[depth + 1] = Math
                        .max(node.rightContour[depth + 1], offset + parent.rightContour[depth]);
                }
            }
        }
    }

    private static final class RouteNode {

        private final String uid;
        private final List<RouteNode> parents = new ArrayList<>(4);
        private NBTTagCompound step;
        private NBTTagList requirements = new NBTTagList();
        private int depth;
        private int x;
        private int y;
        private int rowContentHeight;
        private int parentSpacing;
        private int[] leftContour = { 0 };
        private int[] rightContour = { 0 };

        private RouteNode(String uid) {
            this.uid = uid;
        }

        private int height() {
            return 18 + requirements.tagCount() * 16;
        }

        private int junctionY() {
            return y + rowContentHeight + 2 + JUNCTION_RISE;
        }

    }

    private static final class RouteArrowTexture extends UITexture {

        private final boolean arrowHead;

        private RouteArrowTexture(boolean full, int color) {
            this(full, color, true);
        }

        private RouteArrowTexture(boolean full, int color, boolean arrowHead) {
            super(
                GTGuiTextures.PROGRESSBAR_ARROW_STANDARD.getLocation(),
                0,
                full ? 0.5F : 0,
                1,
                full ? 1 : 0.5F,
                ColorType.DEFAULT,
                false,
                color);
            this.arrowHead = arrowHead;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(float x, float y, float width, float height) {
            drawArrow(x, y, width, height, 0, width, 0, 1);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void drawSubArea(float x, float y, float width, float height, float uStart, float vStart, float uEnd,
            float vEnd, WidgetTheme theme) {
            if (uEnd <= uStart) return;
            applyColor(ColorType.DEFAULT.getColor(theme));
            float fullWidth = width / (uEnd - uStart);
            drawArrow(x, y, fullWidth, height, uStart * fullWidth, uEnd * fullWidth, vStart, vEnd);
        }

        @SideOnly(Side.CLIENT)
        private void drawArrow(float x, float y, float width, float height, float from, float to, float vStart,
            float vEnd) {
            if (!arrowHead) {
                drawSlice(x, y, height, from, to, 0, width, 0.05F, 0.5F, vStart, vEnd);
                return;
            }
            // Preserve the cap and arrowhead; only the straight shaft grows with the connection.
            drawSlice(x, y, height, from, to, 0, 1, 0, 0.05F, vStart, vEnd);
            drawSlice(x, y, height, from, to, 1, width - 10, 0.05F, 0.5F, vStart, vEnd);
            drawSlice(x, y, height, from, to, width - 10, width, 0.5F, 1, vStart, vEnd);
        }

        @SideOnly(Side.CLIENT)
        private void drawSlice(float x, float y, float height, float from, float to, float start, float end,
            float sourceStart, float sourceEnd, float vStart, float vEnd) {
            float left = Math.max(from, start), right = Math.min(to, end);
            if (right <= left || end <= start) return;
            float first = sourceStart + (sourceEnd - sourceStart) * (left - start) / (end - start);
            float last = sourceStart + (sourceEnd - sourceStart) * (right - start) / (end - start);
            GuiDraw.drawTexture(
                location,
                x + left - from,
                y,
                x + right - from,
                y + height,
                lerpU(first),
                lerpV(vStart),
                lerpU(last),
                lerpV(vEnd),
                nonOpaque);
        }
    }

    private static final class SpeciesView {

        private final Supplier<List<String>> source;
        private final StringValue search = new StringValue("");
        private List<String> lastSource;
        private String lastSearch;
        private List<String> filtered = Collections.emptyList();
        private int page;

        private SpeciesView(Supplier<List<String>> source) {
            this.source = source;
        }

        private List<String> filtered() {
            List<String> current = source.get();
            String query = search.getStringValue()
                .trim()
                .toLowerCase(Locale.ROOT);
            if (lastSource != current || !query.equals(lastSearch)) {
                lastSource = current;
                lastSearch = query;
                filtered = new ArrayList<>();
                for (String id : current) {
                    if (id.toLowerCase(Locale.ROOT)
                        .contains(query)
                        || name(id).toLowerCase(Locale.ROOT)
                            .contains(query))
                        filtered.add(id);
                }
                filtered.sort(String::compareTo);
                page = 0;
            }
            return filtered;
        }

        private int pageCount() {
            return Math.max(1, (filtered().size() + SPECIES_PER_PAGE - 1) / SPECIES_PER_PAGE);
        }

        private String id(int slot) {
            List<String> list = filtered();
            int index = page * SPECIES_PER_PAGE + slot;
            return index < list.size() ? list.get(index) : "";
        }
    }

    private static final class CropDropTextField extends TextFieldWidget
        implements RecipeViewerGhostIngredientSlot<ItemStack> {

        @Override
        public boolean handleDragAndDrop(ItemStack stack, int button) {
            ICropCard crop = stack == null ? null : CropRegistry.instance.get(stack);
            if (button != 0 || crop == null) return false;
            setText(crop.getId());
            onTextChanged();
            return true;
        }
    }
}
