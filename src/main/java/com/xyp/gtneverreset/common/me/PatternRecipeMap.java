package com.xyp.gtneverreset.common.me;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

/** Stable recipe-map identifiers shared by encoded patterns, NEI imports and input assemblies. */
public final class PatternRecipeMap {

    public static final String KEY = "gtnrPatternRecipeMap";

    private PatternRecipeMap() {}

    /** Null means unannotated; an empty or unknown identifier remains an invalid explicit request. */
    public static String name(ItemStack pattern) {
        NBTTagCompound tag = pattern == null ? null : pattern.getTagCompound();
        if (tag == null || !tag.hasKey(KEY)) return null;
        return tag.hasKey(KEY, Constants.NBT.TAG_STRING) ? tag.getString(KEY) : "";
    }

    public static RecipeMap<?> resolve(String name) {
        return name == null ? null : RecipeMap.ALL_RECIPE_MAPS.get(name);
    }

    public static String nameForImport(RecipeMap<?> map) {
        // NEI combines coolant variants in a display-only map; new patterns need the executable recipes.
        return (map == RecipeMaps.cutterFakeRecipes ? RecipeMaps.cutterRecipes : map).unlocalizedName;
    }

    public static void write(ItemStack pattern, String name) {
        if (pattern == null) return;
        if (name == null) {
            if (pattern.hasTagCompound()) pattern.getTagCompound()
                .removeTag(KEY);
            return;
        }
        if (!pattern.hasTagCompound()) pattern.setTagCompound(new NBTTagCompound());
        pattern.getTagCompound()
            .setString(KEY, name);
    }
}
