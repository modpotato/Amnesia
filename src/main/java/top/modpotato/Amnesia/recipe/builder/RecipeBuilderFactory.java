package top.modpotato.Amnesia.recipe.builder;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating recipe builders
 */
public class RecipeBuilderFactory {
    private static final Map<Class<? extends Recipe>, RecipeBuilder> BUILDERS = new HashMap<>();
    
    static {
        // Register builders for each recipe type
        registerBuilder(ShapedRecipe.class, RecipeBuilderFactory::createShapedRecipe);
        registerBuilder(ShapelessRecipe.class, RecipeBuilderFactory::createShapelessRecipe);
        registerBuilder(FurnaceRecipe.class, RecipeBuilderFactory::createFurnaceRecipe);
        registerBuilder(BlastingRecipe.class, RecipeBuilderFactory::createBlastingRecipe);
        registerBuilder(SmokingRecipe.class, RecipeBuilderFactory::createSmokingRecipe);
        registerBuilder(CampfireRecipe.class, RecipeBuilderFactory::createCampfireRecipe);
        registerBuilder(StonecuttingRecipe.class, RecipeBuilderFactory::createStonecuttingRecipe);
    }
    
    /**
     * Registers a builder for a recipe type
     * @param recipeClass the recipe class
     * @param builder the builder
     */
    public static <T extends Recipe> void registerBuilder(Class<T> recipeClass, RecipeBuilder builder) {
        BUILDERS.put(recipeClass, builder);
    }
    
    /**
     * Gets a builder for a recipe
     * @param recipe the recipe
     * @return the builder, or null if no builder is registered for the recipe type
     */
    public static RecipeBuilder getBuilder(Recipe recipe) {
        return BUILDERS.get(recipe.getClass());
    }
    
    /**
     * Creates a new recipe with a different result
     * @param key the recipe key
     * @param originalRecipe the original recipe
     * @param newResult the new result
     * @return the new recipe, or null if no builder is registered for the recipe type
     */
    public static Recipe createRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        RecipeBuilder builder = getBuilder(originalRecipe);
        if (builder != null) {
            return builder.createRecipe(key, originalRecipe, newResult);
        }
        return null;
    }
    
    /**
     * Creates a shaped recipe
     */
    private static Recipe createShapedRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        ShapedRecipe original = (ShapedRecipe) originalRecipe;
        ShapedRecipe shuffled = new ShapedRecipe(key, newResult);
        
        // Copy shape
        shuffled.shape(original.getShape());
        
        // Copy ingredients with null check
        for (Map.Entry<Character, RecipeChoice> entry : original.getChoiceMap().entrySet()) {
            if (entry.getValue() != null) {
                shuffled.setIngredient(entry.getKey(), entry.getValue());
            }
        }
        
        return shuffled;
    }
    
    /**
     * Creates a shapeless recipe
     */
    private static Recipe createShapelessRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        ShapelessRecipe original = (ShapelessRecipe) originalRecipe;
        ShapelessRecipe shuffled = new ShapelessRecipe(key, newResult);
        
        // Copy ingredients with null check
        for (RecipeChoice ingredient : original.getChoiceList()) {
            if (ingredient != null) {
                shuffled.addIngredient(ingredient);
            }
        }
        
        return shuffled;
    }
    
    /**
     * Creates a furnace recipe
     */
    private static Recipe createFurnaceRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        FurnaceRecipe original = (FurnaceRecipe) originalRecipe;
        return new FurnaceRecipe(
                key,
                newResult,
                original.getInputChoice(),
                original.getExperience(),
                original.getCookingTime()
        );
    }
    
    /**
     * Creates a blasting recipe
     */
    private static Recipe createBlastingRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        BlastingRecipe original = (BlastingRecipe) originalRecipe;
        return new BlastingRecipe(
                key,
                newResult,
                original.getInputChoice(),
                original.getExperience(),
                original.getCookingTime()
        );
    }
    
    /**
     * Creates a smoking recipe
     */
    private static Recipe createSmokingRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        SmokingRecipe original = (SmokingRecipe) originalRecipe;
        return new SmokingRecipe(
                key,
                newResult,
                original.getInputChoice(),
                original.getExperience(),
                original.getCookingTime()
        );
    }
    
    /**
     * Creates a campfire recipe
     */
    private static Recipe createCampfireRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        CampfireRecipe original = (CampfireRecipe) originalRecipe;
        return new CampfireRecipe(
                key,
                newResult,
                original.getInputChoice(),
                original.getExperience(),
                original.getCookingTime()
        );
    }
    
    /**
     * Creates a stonecutting recipe
     */
    private static Recipe createStonecuttingRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        StonecuttingRecipe original = (StonecuttingRecipe) originalRecipe;
        return new StonecuttingRecipe(
                key,
                newResult,
                original.getInputChoice()
        );
    }
} 