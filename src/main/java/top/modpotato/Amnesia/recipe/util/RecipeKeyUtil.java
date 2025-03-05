package top.modpotato.Amnesia.recipe.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

/**
 * Utility class for handling recipe keys
 */
public class RecipeKeyUtil {
    
    /**
     * Checks if a recipe has a namespaced key
     * @param recipe the recipe to check
     * @return true if the recipe has a key, false otherwise
     */
    public static boolean hasNamespacedKey(Recipe recipe) {
        return recipe instanceof ShapedRecipe ||
                recipe instanceof ShapelessRecipe ||
                recipe instanceof FurnaceRecipe ||
                recipe instanceof BlastingRecipe ||
                recipe instanceof SmokingRecipe ||
                recipe instanceof CampfireRecipe ||
                recipe instanceof StonecuttingRecipe;
    }
    
    /**
     * Gets the namespaced key of a recipe
     * @param recipe the recipe
     * @return the namespaced key, or null if the recipe doesn't have a key
     */
    public static NamespacedKey getNamespacedKey(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return ((ShapedRecipe) recipe).getKey();
        } else if (recipe instanceof ShapelessRecipe) {
            return ((ShapelessRecipe) recipe).getKey();
        } else if (recipe instanceof FurnaceRecipe) {
            return ((FurnaceRecipe) recipe).getKey();
        } else if (recipe instanceof BlastingRecipe) {
            return ((BlastingRecipe) recipe).getKey();
        } else if (recipe instanceof SmokingRecipe) {
            return ((SmokingRecipe) recipe).getKey();
        } else if (recipe instanceof CampfireRecipe) {
            return ((CampfireRecipe) recipe).getKey();
        } else if (recipe instanceof StonecuttingRecipe) {
            return ((StonecuttingRecipe) recipe).getKey();
        }
        
        return null;
    }
} 