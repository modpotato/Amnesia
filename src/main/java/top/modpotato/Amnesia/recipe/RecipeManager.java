package top.modpotato.Amnesia.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import top.modpotato.Amnesia.Main;
import top.modpotato.Amnesia.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Manages recipe shuffling
 */
public class RecipeManager {
    private final Main plugin;
    private final Map<NamespacedKey, Recipe> originalRecipes = new HashMap<>();
    private final Map<NamespacedKey, Recipe> shuffledRecipes = new HashMap<>();
    private boolean isShuffled = false;
    
    /**
     * Creates a new RecipeManager
     * @param plugin the plugin instance
     */
    public RecipeManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Shuffles recipes according to the configured mode
     */
    public void shuffleRecipes() {
        // Get the seed from config
        long seed = plugin.getConfigManager().getSeed();
        Random random = new Random(seed);
        
        // Get the shuffle mode from config
        String shuffleMode = plugin.getConfigManager().getShuffleMode();
        
        // Announce shuffle start
        MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleStarted);
        
        // Store original recipes if not already stored
        if (originalRecipes.isEmpty()) {
            storeOriginalRecipes();
        }
        
        // Clear existing shuffled recipes
        shuffledRecipes.clear();
        
        // Remove all recipes from the server
        clearServerRecipes();
        
        // Shuffle recipes based on mode
        if (shuffleMode.equalsIgnoreCase("random_item")) {
            shuffleRandomItem(random);
        } else if (shuffleMode.equalsIgnoreCase("recipe_result")) {
            shuffleRecipeResult(random);
        } else {
            plugin.getLogger().warning("Unknown shuffle mode: " + shuffleMode + ". Using random_item mode.");
            shuffleRandomItem(random);
        }
        
        // Register shuffled recipes
        registerShuffledRecipes();
        
        // Set shuffled flag
        isShuffled = true;
        
        // Announce shuffle completion
        MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleFinished);
    }
    
    /**
     * Stores the original recipes from the server
     */
    private void storeOriginalRecipes() {
        originalRecipes.clear();
        
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        List<String> excludedRecipes = plugin.getConfigManager().getExcludedRecipes();
        
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            
            // Skip recipes that don't have a key
            if (!hasNamespacedKey(recipe)) {
                continue;
            }
            
            NamespacedKey key = getNamespacedKey(recipe);
            
            // Skip excluded recipes
            if (excludedRecipes.contains(key.toString())) {
                continue;
            }
            
            // Store the recipe
            originalRecipes.put(key, recipe);
        }
        
        plugin.getLogger().info("Stored " + originalRecipes.size() + " original recipes");
    }
    
    /**
     * Clears all recipes from the server
     */
    private void clearServerRecipes() {
        for (NamespacedKey key : originalRecipes.keySet()) {
            Bukkit.removeRecipe(key);
        }
    }
    
    /**
     * Shuffles recipes by replacing their results with random items
     * @param random the random number generator
     */
    private void shuffleRandomItem(Random random) {
        // Get all available materials
        List<Material> availableMaterials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !isExcludedRandomItem(material)) {
                availableMaterials.add(material);
            }
        }
        
        // Shuffle recipes
        for (Map.Entry<NamespacedKey, Recipe> entry : originalRecipes.entrySet()) {
            NamespacedKey key = entry.getKey();
            Recipe recipe = entry.getValue();
            
            // Get a random material
            Material randomMaterial = availableMaterials.get(random.nextInt(availableMaterials.size()));
            ItemStack randomItem = new ItemStack(randomMaterial);
            
            // Create a new recipe with the random item as the result
            Recipe shuffledRecipe = createShuffledRecipe(key, recipe, randomItem);
            
            // Store the shuffled recipe
            if (shuffledRecipe != null) {
                shuffledRecipes.put(key, shuffledRecipe);
            }
        }
        
        plugin.getLogger().info("Shuffled " + shuffledRecipes.size() + " recipes with random items");
    }
    
    /**
     * Shuffles recipes by swapping their results
     * @param random the random number generator
     */
    private void shuffleRecipeResult(Random random) {
        // Get all recipe results
        List<ItemStack> recipeResults = originalRecipes.values().stream()
                .map(Recipe::getResult)
                .collect(Collectors.toList());
        
        // Shuffle the results
        Collections.shuffle(recipeResults, random);
        
        // Create new recipes with shuffled results
        int i = 0;
        for (Map.Entry<NamespacedKey, Recipe> entry : originalRecipes.entrySet()) {
            NamespacedKey key = entry.getKey();
            Recipe recipe = entry.getValue();
            
            // Get the shuffled result
            ItemStack shuffledResult = recipeResults.get(i);
            
            // Create a new recipe with the shuffled result
            Recipe shuffledRecipe = createShuffledRecipe(key, recipe, shuffledResult);
            
            // Store the shuffled recipe
            if (shuffledRecipe != null) {
                shuffledRecipes.put(key, shuffledRecipe);
            }
            
            i++;
        }
        
        plugin.getLogger().info("Shuffled " + shuffledRecipes.size() + " recipes by swapping results");
    }
    
    /**
     * Creates a shuffled recipe with a new result
     * @param key the recipe key
     * @param originalRecipe the original recipe
     * @param newResult the new result
     * @return the shuffled recipe
     */
    private Recipe createShuffledRecipe(NamespacedKey key, Recipe originalRecipe, ItemStack newResult) {
        if (originalRecipe instanceof ShapedRecipe) {
            ShapedRecipe original = (ShapedRecipe) originalRecipe;
            ShapedRecipe shuffled = new ShapedRecipe(key, newResult);
            
            // Copy shape
            shuffled.shape(original.getShape());
            
            // Copy ingredients
            for (Map.Entry<Character, RecipeChoice> entry : original.getChoiceMap().entrySet()) {
                shuffled.setIngredient(entry.getKey(), entry.getValue());
            }
            
            return shuffled;
        } else if (originalRecipe instanceof ShapelessRecipe) {
            ShapelessRecipe original = (ShapelessRecipe) originalRecipe;
            ShapelessRecipe shuffled = new ShapelessRecipe(key, newResult);
            
            // Copy ingredients
            for (RecipeChoice ingredient : original.getChoiceList()) {
                shuffled.addIngredient(ingredient);
            }
            
            return shuffled;
        } else if (originalRecipe instanceof FurnaceRecipe) {
            FurnaceRecipe original = (FurnaceRecipe) originalRecipe;
            return new FurnaceRecipe(
                    key,
                    newResult,
                    original.getInputChoice(),
                    original.getExperience(),
                    original.getCookingTime()
            );
        } else if (originalRecipe instanceof BlastingRecipe) {
            BlastingRecipe original = (BlastingRecipe) originalRecipe;
            return new BlastingRecipe(
                    key,
                    newResult,
                    original.getInputChoice(),
                    original.getExperience(),
                    original.getCookingTime()
            );
        } else if (originalRecipe instanceof SmokingRecipe) {
            SmokingRecipe original = (SmokingRecipe) originalRecipe;
            return new SmokingRecipe(
                    key,
                    newResult,
                    original.getInputChoice(),
                    original.getExperience(),
                    original.getCookingTime()
            );
        } else if (originalRecipe instanceof CampfireRecipe) {
            CampfireRecipe original = (CampfireRecipe) originalRecipe;
            return new CampfireRecipe(
                    key,
                    newResult,
                    original.getInputChoice(),
                    original.getExperience(),
                    original.getCookingTime()
            );
        } else if (originalRecipe instanceof StonecuttingRecipe) {
            StonecuttingRecipe original = (StonecuttingRecipe) originalRecipe;
            return new StonecuttingRecipe(
                    key,
                    newResult,
                    original.getInputChoice()
            );
        }
        
        return null;
    }
    
    /**
     * Registers the shuffled recipes on the server
     */
    private void registerShuffledRecipes() {
        for (Recipe recipe : shuffledRecipes.values()) {
            Bukkit.addRecipe(recipe);
        }
    }
    
    /**
     * Restores the original recipes
     */
    public void restoreOriginalRecipes() {
        if (!isShuffled) {
            return;
        }
        
        // Clear all recipes
        clearServerRecipes();
        
        // Register original recipes
        for (Recipe recipe : originalRecipes.values()) {
            Bukkit.addRecipe(recipe);
        }
        
        // Clear shuffled recipes
        shuffledRecipes.clear();
        
        // Reset shuffled flag
        isShuffled = false;
        
        plugin.getLogger().info("Restored " + originalRecipes.size() + " original recipes");
    }
    
    /**
     * Checks if a material is excluded from random item selection
     * @param material the material to check
     * @return true if the material is excluded, false otherwise
     */
    private boolean isExcludedRandomItem(Material material) {
        List<String> excludedItems = plugin.getConfigManager().getExcludedRandomItems();
        return excludedItems.contains("minecraft:" + material.name().toLowerCase());
    }
    
    /**
     * Checks if a recipe has a namespaced key
     * @param recipe the recipe to check
     * @return true if the recipe has a key, false otherwise
     */
    private boolean hasNamespacedKey(Recipe recipe) {
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
     * @return the namespaced key
     */
    private NamespacedKey getNamespacedKey(Recipe recipe) {
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
    
    /**
     * Checks if recipes are currently shuffled
     * @return true if recipes are shuffled, false otherwise
     */
    public boolean isShuffled() {
        return isShuffled;
    }
} 