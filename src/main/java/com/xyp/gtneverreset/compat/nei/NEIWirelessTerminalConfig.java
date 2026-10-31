package com.xyp.gtneverreset.compat.nei;

import java.io.File;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.ConfigTag;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.api.INEIGuiAdapter;
import gregtech.api.recipe.RecipeCategory;

public final class NEIWirelessTerminalConfig implements IConfigureNEI {

    private static final String FILL_SEARCH_NAMES = "gtneverreset.dual_interface_terminal_fill_search_names";
    private static final String APPEND_ITEM_NUMBERS = "gtneverreset.dual_interface_terminal_append_circuit_damage";
    private static final String BUTTON_THEME = "gtneverreset.quick_terminal_button_theme";
    private static boolean legacySettingsMigrated;

    @Override
    public void loadConfig() {
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
            ConfigLoader.saveClient();
            return;
        }
        ConfigFile legacyConfig = new ConfigFile(legacyFile);
        ConfigTag search = legacyConfig.getTag(FILL_SEARCH_NAMES, false);
        ConfigTag suffix = legacyConfig.getTag(APPEND_ITEM_NUMBERS, false);
        ConfigTag theme = legacyConfig.getTag(BUTTON_THEME, false);
        if (search != null && search.value != null) {
            ClientConfig.wirelessTerminal.fillSearchFromNeiRecipes = search.getBooleanValue(false);
        }
        if (suffix != null && suffix.value != null) {
            ClientConfig.wirelessTerminal.appendNonConsumableItemNumbers = suffix.getBooleanValue(true);
        }
        if (theme != null && theme.value != null) {
            String value = theme.getValue("auto");
            if (value.matches("(?i)auto|light|dark")) ClientConfig.wirelessTerminal.buttonTheme = value;
        }
        ConfigLoader.saveClient();
    }

    private static void register(String identifier) {
        if (!API.hasGuiOverlayHandler(GuiQuickEncodingTerminal.class, identifier)) {
            API.registerGuiOverlayHandler(
                GuiQuickEncodingTerminal.class,
                QuickTerminalRecipeTransferHandler.INSTANCE,
                identifier);
        }
    }
}
