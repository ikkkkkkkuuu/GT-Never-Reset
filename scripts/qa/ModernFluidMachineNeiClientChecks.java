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
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.xyp.gtneverreset.loader.GTNRRecipeMaps;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.api.util.GTRecipe;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "modernfluidmachineneiclientqa",
    name = "Modern Fluid Machine NEI Client QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class ModernFluidMachineNeiClientChecks {

    private static final RecipeMap<?>[] MAPS = { GTNRRecipeMaps.MODERN_MIXER_RECIPES,
        GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES, GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
        GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES };
    private static final String[] NAMES = { "mixer", "electrolyzer", "chemical_reactor", "centrifuge" };
    private static final int[] FULL_SLOT_FAMILIES = { 1, 3 };

    private final StringBuilder report = new StringBuilder();
    private boolean launched;
    private boolean audited;
    private boolean finished;
    private int ticks;
    private int frames;
    private int page;
    private GTNEIDefaultHandler.CachedDefaultRecipe cached;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnr.modernMachines.nei.client.qa")) FMLCommonHandler.instance()
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
            require(++ticks < 6000, "NEI visual QA timeout at page " + page);
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0);
            mc.gameSettings.renderDistanceChunks = 3;
            mc.gameSettings.limitFramerate = 60;
            if (mc.thePlayer != null) mc.ingameGUI.getChatGUI()
                .clearChatMessages();
            if (!launched && mc.theWorld == null && mc.currentScreen != null) {
                launched = true;
                mc.launchIntegratedServer(
                    "modern-machines-nei-" + System.currentTimeMillis(),
                    "Modern NEI QA",
                    new WorldSettings(764L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        try {
            if (!audited) {
                if (++frames < 60) return;
                for (int family = 0; family < MAPS.length; family++) checkMaximumLayout(MAPS[family], NAMES[family]);
                audited = true;
                frames = 0;
                openPage();
                return;
            }
            if (!(mc.currentScreen instanceof GuiCraftingRecipe) || ++frames < 50) return;
            int family = familyForPage(page);
            RecipeMap<?> map = MAPS[family];
            boolean fixture = page >= MAPS.length;
            checkCachedRecipe(map.getFrontend(), cached, NAMES[family] + (fixture ? " full-slot fixture" : ""));
            if (!fixture && map.getFrontend()
                .getUIProperties().maxFluidOutputs == 6)
                require(
                    cached.mRecipe.mFluidOutputs.length >= 2,
                    "real " + NAMES[family] + " recipe contains at least two direct-fluid products");
            if (fixture) require(
                cached.mOutputs.size() == 12 && cached.mRecipe.mFluidOutputs.length == 6,
                NAMES[family] + ": six item and six direct-fluid products all enter the real NEI renderer");
            capture(NAMES[family] + (fixture ? "-nei-six-fluid-layout-fixture.png" : "-nei-real-recipe.png"));
            frames = 0;
            if (++page < MAPS.length + FULL_SLOT_FAMILIES.length) {
                openPage();
            } else {
                Files.writeString(new File(output(), "client-result.txt").toPath(), "PASS\n" + report);
                System.out.println(
                    "MODERN_FLUID_MACHINE_NEI_CLIENT_QA PASS: all maximum slots, logo/progress clearance, cached stack positions, four real recipe pages and electrolyzer/centrifuge full six-fluid output layouts");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checkMaximumLayout(RecipeMap<?> map, String label) {
        RecipeMapFrontend frontend = map.getFrontend();
        var ui = frontend.getUIProperties();
        var nei = frontend.getNEIProperties();
        List<Rect> slots = new ArrayList<>();
        addSlots(slots, frontend.getItemInputPositions(ui.maxItemInputs), ui.maxItemInputs, "item input");
        addSlots(slots, frontend.getItemOutputPositions(ui.maxItemOutputs), ui.maxItemOutputs, "item output");
        addSlots(slots, frontend.getFluidInputPositions(ui.maxFluidInputs), ui.maxFluidInputs, "fluid input");
        addSlots(slots, frontend.getFluidOutputPositions(ui.maxFluidOutputs), ui.maxFluidOutputs, "fluid output");
        Rect background = new Rect(
            nei.recipeBackgroundOffset.x,
            nei.recipeBackgroundOffset.y,
            nei.recipeBackgroundSize.width,
            nei.recipeBackgroundSize.height,
            "NEI background");
        require(
            background.width == 170 && background.height == 82,
            label + " retains the standard 170x82 NEI recipe background");
        Rect logo = new Rect(ui.logoPos.x, ui.logoPos.y, ui.logoSize.width, ui.logoSize.height, "GregTech logo");
        Rect progress = new Rect(
            ui.progressBarPos.x,
            ui.progressBarPos.y,
            ui.progressBarSize.width,
            ui.progressBarSize.height,
            "progress bar");
        for (int index = 0; index < slots.size(); index++) {
            Rect slot = slots.get(index);
            require(background.contains(slot), label + " max slot lies inside background: " + slot);
            require(!slot.intersects(logo), label + " slot does not overlap logo: " + slot);
            require(!slot.intersects(progress), label + " slot does not overlap progress bar: " + slot);
            for (int other = index + 1; other < slots.size(); other++)
                require(!slot.intersects(slots.get(other)), label + " item/fluid slots do not overlap: " + slot);
        }
        if (ui.maxFluidOutputs == 6) {
            List<Pos2d> fluids = frontend.getFluidOutputPositions(6);
            require(
                fluids.stream()
                    .map(position -> position.x)
                    .distinct()
                    .count() == 3
                    && fluids.stream()
                        .map(position -> position.y)
                        .distinct()
                        .count() == 2,
                label + " has two rows of three direct-fluid output slots");
        }
        report.append(label)
            .append(": all ")
            .append(slots.size())
            .append(" max item/fluid slots fit ")
            .append(background.width)
            .append('x')
            .append(background.height)
            .append(" NEI background, with no slot/logo/progress overlap.\n");
    }

    private static void addSlots(List<Rect> slots, List<Pos2d> positions, int expected, String label) {
        require(positions.size() == expected, label + " position count");
        for (int index = 0; index < positions.size(); index++) {
            Pos2d position = positions.get(index);
            slots.add(new Rect(position.x, position.y, 18, 18, label + " " + index));
        }
    }

    private void openPage() throws Exception {
        int family = familyForPage(page);
        RecipeMap<?> map = MAPS[family];
        GTNEIDefaultHandler handler = new GTNEIDefaultHandler(map.getDefaultRecipeCategory());
        if (page < MAPS.length) {
            handler.loadCraftingRecipes(map.unlocalizedName);
            cached = handler.arecipes.stream()
                .map(recipe -> (GTNEIDefaultHandler.CachedDefaultRecipe) recipe)
                .filter(recipe -> recipe.mRecipe.mSpecialItems == null)
                .filter(
                    recipe -> map.getFrontend()
                        .getUIProperties().maxFluidOutputs != 6 || recipe.mRecipe.mFluidOutputs.length >= 2)
                .max(
                    Comparator.comparingInt(
                        recipe -> recipe.mRecipe.mFluidOutputs.length * 100 + recipe.mRecipe.mFluidInputs.length * 10
                            + recipe.mRecipe.mInputs.length
                            + recipe.mRecipe.mOutputs.length))
                .orElseThrow(() -> new AssertionError("representative real NEI recipe missing: " + NAMES[family]));
            handler.arecipes.removeIf(recipe -> recipe != cached);
            report.append(NAMES[family])
                .append(" actual recipe: ")
                .append(cached.mRecipe.mInputs.length)
                .append(" item inputs, ")
                .append(cached.mRecipe.mFluidInputs.length)
                .append(" fluid inputs, ")
                .append(cached.mRecipe.mOutputs.length)
                .append(" item products, ")
                .append(cached.mRecipe.mFluidOutputs.length)
                .append(" fluid products.\n");
        } else {
            int productionCount = map.getAllRecipes()
                .size();
            GTRecipe fixture = fullOutputFixture();
            fixture.setRecipeCategory(map.getDefaultRecipeCategory());
            cached = handler.new CachedDefaultRecipe(fixture);
            handler.arecipes.add(cached);
            require(
                map.getAllRecipes()
                    .size() == productionCount,
                "full-slot fixture does not enter production recipe map");
            report.append("Full-slot ")
                .append(NAMES[family])
                .append(" fixture is local to the QA handler; production recipe maps are unchanged.\n");
        }
        require(handler.numRecipes() == 1, "isolated actual NEI recipe page");
        ArrayList<ICraftingHandler> handlers = new ArrayList<>();
        handlers.add(handler);
        var constructor = GuiCraftingRecipe.class.getDeclaredConstructor(ArrayList.class);
        constructor.setAccessible(true);
        Minecraft.getMinecraft()
            .displayGuiScreen(constructor.newInstance(handlers));
    }

    private static int familyForPage(int page) {
        return page < MAPS.length ? page : FULL_SLOT_FAMILIES[page - MAPS.length];
    }

    private void checkCachedRecipe(RecipeMapFrontend frontend, GTNEIDefaultHandler.CachedDefaultRecipe recipe,
        String label) {
        GTRecipe source = recipe.mRecipe;
        List<String> expectedInputs = new ArrayList<>();
        List<String> expectedOutputs = new ArrayList<>();
        addExpected(expectedInputs, frontend.getItemInputPositions(source.mInputs.length), source.mInputs);
        addExpected(expectedInputs, frontend.getFluidInputPositions(source.mFluidInputs.length), source.mFluidInputs);
        addExpected(expectedOutputs, frontend.getItemOutputPositions(source.mOutputs.length), source.mOutputs);
        addExpected(
            expectedOutputs,
            frontend.getFluidOutputPositions(source.mFluidOutputs.length),
            source.mFluidOutputs);
        expectedInputs.sort(String::compareTo);
        expectedOutputs.sort(String::compareTo);
        require(
            expectedInputs.equals(coordinates(recipe.mInputs)),
            label + " CachedDefaultRecipe input positions match frontend");
        require(
            expectedOutputs.equals(coordinates(recipe.mOutputs)),
            label + " CachedDefaultRecipe output positions match frontend");
        var ui = frontend.getUIProperties();
        Rect logo = new Rect(
            ui.logoPos.x - 5,
            ui.logoPos.y - 11,
            ui.logoSize.width,
            ui.logoSize.height,
            "cached recipe logo");
        for (PositionedStack stack : recipe.mOutputs) require(
            !new Rect(stack.relx, stack.rely, 16, 16, "cached output").intersects(logo),
            label + " actually rendered product clears the GregTech logo");
        report.append(label)
            .append(": actual CachedDefaultRecipe stack positions match the frontend and clear the logo.\n");
    }

    private static void addExpected(List<String> result, List<Pos2d> positions, Object[] stacks) {
        for (int index = 0; index < stacks.length; index++) if (stacks[index] != null) {
            Pos2d position = positions.get(index);
            // GTNEIDefaultHandler's window offset is (-5,-11), with a one-pixel item inset.
            result.add((position.x - 4) + "," + (position.y - 10));
        }
    }

    private static List<String> coordinates(List<PositionedStack> stacks) {
        return stacks.stream()
            .map(stack -> stack.relx + "," + stack.rely)
            .sorted()
            .toList();
    }

    private static GTRecipe fullOutputFixture() {
        ItemStack[] products = { new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot),
            new ItemStack(Items.redstone), new ItemStack(Items.diamond), new ItemStack(Items.coal),
            new ItemStack(Items.quartz) };
        FluidStack[] fluids = { Materials.Water.getFluid(1000), Materials.Lava.getFluid(1000),
            Materials.Oxygen.getGas(1000), Materials.Hydrogen.getGas(1000), Materials.Nitrogen.getGas(1000),
            Materials.Chlorine.getGas(1000) };
        return GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.clay_ball), new ItemStack(Items.flint))
            .itemOutputs(products)
            .fluidInputs(Materials.Water.getFluid(1000))
            .fluidOutputs(fluids)
            .duration(200)
            .eut(30)
            .build()
            .orElseThrow();
    }

    private static File output() {
        File output = new File(System.getProperty("gtnr.modernMachines.nei.client.qa.output"));
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

    private record Rect(int x, int y, int width, int height, String label) {

        private boolean contains(Rect other) {
            return other.x >= x && other.y >= y
                && other.x + other.width <= x + width
                && other.y + other.height <= y + height;
        }

        private boolean intersects(Rect other) {
            return x < other.x + other.width && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y;
        }
    }
}
