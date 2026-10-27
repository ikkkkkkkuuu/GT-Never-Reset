package com.xyp.gtneverreset.client.nei;

import java.io.File;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.ConfigTag;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.api.INEIGuiAdapter;
import codechicken.nei.config.OptionToggleButton;
import gregtech.api.recipe.RecipeCategory;

public final class NEIWirelessTerminalConfig implements IConfigureNEI {

    private static final String FILL_SEARCH_NAMES = "gtneverreset.dual_interface_terminal_fill_search_names";
    private static final String APPEND_ITEM_NUMBERS = "gtneverreset.dual_interface_terminal_append_circuit_damage";
    private static final String PREFER_OWN_INTERFACE_NAMES = "gtneverreset.prefer_own_interface_names";
    private static final String BUTTON_THEME = "gtneverreset.quick_terminal_button_theme";
    private static boolean legacySettingsMigrated;

    @Override
    public void loadConfig() {
        // #tr nei.options.gtneverreset.dual_interface_terminal_fill_search_names
        // # Autofill interface search from NEI recipes
        // # zh_CN 用 NEI 配方自动填充接口搜索
        // #tr nei.options.gtneverreset.dual_interface_terminal_fill_search_names.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtneverreset.dual_interface_terminal_fill_search_names.false
        // # No
        // # zh_CN 否
        API.addOption(
            new MainConfigToggleButton(
                FILL_SEARCH_NAMES,
                () -> MainConfig.client.wirelessTerminal.fillSearchFromNeiRecipes,
                value -> MainConfig.client.wirelessTerminal.fillSearchFromNeiRecipes = value));
        // #tr nei.options.gtneverreset.dual_interface_terminal_append_circuit_damage
        // # Append non-consumed item numbers to interface search
        // # zh_CN 在接口搜索中追加不消耗物品编号
        // #tr nei.options.gtneverreset.dual_interface_terminal_append_circuit_damage.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtneverreset.dual_interface_terminal_append_circuit_damage.false
        // # No
        // # zh_CN 否
        API.addOption(
            new MainConfigToggleButton(
                APPEND_ITEM_NUMBERS,
                () -> MainConfig.client.wirelessTerminal.appendNonConsumableItemNumbers,
                value -> MainConfig.client.wirelessTerminal.appendNonConsumableItemNumbers = value));
        // #tr nei.options.gtneverreset.prefer_own_interface_names
        // # Prefer assembly's own circuit and non-consumed item numbers
        // # zh_CN 接口名称优先使用样板总成自身的电路和不消耗物品编号
        // #tr nei.options.gtneverreset.prefer_own_interface_names.true
        // # Yes
        // # zh_CN 是
        // #tr nei.options.gtneverreset.prefer_own_interface_names.false
        // # No
        // # zh_CN 否
        API.addOption(
            new MainConfigToggleButton(
                PREFER_OWN_INTERFACE_NAMES,
                () -> MainConfig.client.wirelessTerminal.preferOwnInterfaceNames,
                value -> MainConfig.client.wirelessTerminal.preferOwnInterfaceNames = value));
        API.registerNEIGuiHandler(new INEIGuiAdapter() {

            @Override
            public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int width, int height) {
                return gui instanceof GuiQuickEncodingTerminal terminal
                    && terminal.hideItemPanelSlot(x, y, width, height);
            }
        });
        for (String identifier : new String[] { "crafting", "crafting2x2", "smelting", "brewing",
            "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands", "thaumcraft.alchemy",
            "thaumcraft.infusion" }) register(identifier);
        for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values())
            register(category.unlocalizedName);
    }

    @Override
    public String getName() {
        return GTNeverReset.NAME;
    }

    @Override
    public String getVersion() {
        return "1";
    }

    public static void migrateLegacySettings(File configDirectory) {
        if (legacySettingsMigrated || !ConfigLoader.shouldMigrateLegacyClientConfig()) return;
        legacySettingsMigrated = true;
        File legacyFile = new File(configDirectory, "NEI/client.cfg");
        if (!legacyFile.isFile()) {
            ConfigLoader.save();
            return;
        }
        ConfigFile legacyConfig = new ConfigFile(legacyFile);
        ConfigTag search = legacyConfig.getTag(FILL_SEARCH_NAMES, false);
        ConfigTag suffix = legacyConfig.getTag(APPEND_ITEM_NUMBERS, false);
        ConfigTag theme = legacyConfig.getTag(BUTTON_THEME, false);
        if (search != null && search.value != null) {
            MainConfig.client.wirelessTerminal.fillSearchFromNeiRecipes = search.getBooleanValue(false);
        }
        if (suffix != null && suffix.value != null) {
            MainConfig.client.wirelessTerminal.appendNonConsumableItemNumbers = suffix.getBooleanValue(true);
        }
        if (theme != null && theme.value != null) {
            String value = theme.getValue("auto");
            if (value.matches("(?i)auto|light|dark")) MainConfig.client.wirelessTerminal.buttonTheme = value;
        }
        ConfigLoader.save();
    }

    private static void register(String identifier) {
        if (!API.hasGuiOverlayHandler(GuiQuickEncodingTerminal.class, identifier)) {
            API.registerGuiOverlayHandler(
                GuiQuickEncodingTerminal.class,
                QuickTerminalRecipeTransferHandler.INSTANCE,
                identifier);
        }
    }

    private static final class MainConfigToggleButton extends OptionToggleButton {

        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        private MainConfigToggleButton(String name, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(name, true);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public boolean state() {
            return getter.getAsBoolean();
        }

        @Override
        public boolean onClick(int button) {
            setter.accept(!state());
            ConfigLoader.save();
            return true;
        }

        @Override
        public boolean showWorldSelector() {
            return false;
        }
    }
}
