package top.modpotato.Amnesia.recipe.builder;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Functional interface for building recipes
 */
@FunctionalInterface
public interface RecipeBuilder {
    /**
     * Creates a new recipe with a different result
     * @param key the recipe key
     * @param originalRecipe the original recipe
     * @param newResult the new result
     * @return the new recipe, or null if the recipe type is not supported
     */
    Recipe createRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult);
} 