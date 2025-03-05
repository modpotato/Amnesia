package top.modpotato.Amnesia.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import top.modpotato.Amnesia.Main;
import top.modpotato.Amnesia.recipe.builder.RecipeBuilderFactory;
import top.modpotato.Amnesia.recipe.util.MaterialCache;
import top.modpotato.Amnesia.recipe.util.RecipeKeyUtil;
import top.modpotato.Amnesia.util.MessageUtil;
import top.modpotato.Amnesia.util.SchedulerUtil;

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
    
    /**
     * Creates a new RecipeManager
     * @param plugin the plugin instance
     */
    public RecipeManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initializes the recipe manager
     * This should be called after the plugin is enabled
     */
    public void initialize() {
        // Check if recipes were shuffled before restart
        if (plugin.getDataManager().isShuffled()) {
            plugin.getLogger().info("Recipes were shuffled before restart, restoring shuffle state...");
            shuffleRecipes(false); // Restore shuffle state without announcement
        }
    }
    
    /**
     * Shuffles recipes according to the configured mode
     */
    public void shuffleRecipes() {
        shuffleRecipes(true); // Shuffle with announcement
    }
    
    /**
     * Shuffles recipes according to the configured mode
     * @param announce whether to announce the shuffle
     */
    public void shuffleRecipes(boolean announce) {
        // Get the seed from data
        long seed = plugin.getDataManager().getSeed();
        Random random = new Random(seed);
        
        // Get the shuffle mode from config
        String shuffleMode = plugin.getConfigManager().getShuffleMode();
        
        // Announce shuffle start if needed
        if (announce) {
            MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleStarted);
        }
        
        // Prepare recipe data asynchronously when possible
        SchedulerUtil.runAsync(plugin, () -> prepareRecipeData(shuffleMode, random))
            .thenRun(() -> SchedulerUtil.runTask(plugin, () -> applyRecipeChanges(announce)));
    }
    
    /**
     * Prepares recipe data asynchronously
     * @param shuffleMode the shuffle mode
     * @param random the random number generator
     */
    private void prepareRecipeData(String shuffleMode, Random random) {
        try {
            // Store original recipes if not already stored (this needs to be done on the main thread)
            if (originalRecipes.isEmpty()) {
                SchedulerUtil.runTaskAsync(plugin, this::storeOriginalRecipes).join();
            }
            
            // Clear existing shuffled recipes
            shuffledRecipes.clear();
            
            // Prepare shuffled recipes based on mode
            if (shuffleMode.equalsIgnoreCase("random_item")) {
                prepareRandomItemRecipes(random);
            } else if (shuffleMode.equalsIgnoreCase("recipe_result")) {
                prepareRecipeResultRecipes(random);
            } else {
                plugin.getLogger().warning("Unknown shuffle mode: " + shuffleMode + ". Using random_item mode.");
                prepareRandomItemRecipes(random);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error preparing recipe data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Applies recipe changes on the main thread
     * @param announce whether to announce the completion
     */
    private void applyRecipeChanges(boolean announce) {
        // Clear server recipes
        clearServerRecipes();
        
        // Register shuffled recipes
        registerShuffledRecipes();
        
        // Sync client recipes
        syncClientRecipes();
        
        // Update shuffle state
        plugin.getDataManager().setShuffled(true);
        plugin.getDataManager().saveData();
        
        // Announce shuffle finished if needed
        if (announce) {
            MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleFinished);
        }
        
        plugin.getLogger().info("Recipes shuffled successfully with seed: " + plugin.getDataManager().getSeed() + 
                " (" + (plugin.getDataManager().isUserSetSeed() ? "user-set" : "random") + ")");
    }
    
    /**
     * Synchronizes client recipes based on the configured mode
     */
    private void syncClientRecipes() {
        String syncMode = plugin.getConfigManager().getClientSyncMode();
        
        switch (syncMode.toLowerCase()) {
            case "resync":
                // Resync all clients with the new recipes
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.updateCommands();
                    player.discoverRecipes(shuffledRecipes.keySet());
                }
                plugin.getLogger().info("Resynced all clients with new recipes");
                break;
                
            case "clear":
                // Clear all recipes from clients
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.updateCommands();
                    player.undiscoverRecipes(originalRecipes.keySet());
                }
                plugin.getLogger().info("Cleared all recipes from clients");
                break;
                
            case "vanilla":
                // Let Minecraft handle it
                plugin.getLogger().info("Using vanilla recipe handling for clients");
                break;
                
            default:
                plugin.getLogger().warning("Unknown client sync mode: " + syncMode + ". Using vanilla mode.");
                plugin.getLogger().info("Using vanilla recipe handling for clients");
                break;
        }
    }
    
    /**
     * Stores the original recipes from the server
     * Must be called on the main thread
     */
    private void storeOriginalRecipes() {
        originalRecipes.clear();
        
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        List<String> excludedRecipes = plugin.getConfigManager().getExcludedRecipes();
        
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            
            // Skip recipes that don't have a key
            if (!RecipeKeyUtil.hasNamespacedKey(recipe)) {
                continue;
            }
            
            NamespacedKey key = RecipeKeyUtil.getNamespacedKey(recipe);
            
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
     * Must be called on the main thread
     */
    private void clearServerRecipes() {
        for (NamespacedKey key : originalRecipes.keySet()) {
            Bukkit.removeRecipe(key);
        }
    }
    
    /**
     * Prepares random item recipes
     * Can be called asynchronously
     * @param random the random number generator
     */
    private void prepareRandomItemRecipes(Random random) {
        // Get all available materials
        List<Material> availableMaterials = MaterialCache.getAvailableRandomItemMaterials(plugin);
        
        // Prepare shuffled recipes
        for (Map.Entry<NamespacedKey, Recipe> entry : originalRecipes.entrySet()) {
            NamespacedKey key = entry.getKey();
            Recipe recipe = entry.getValue();
            
            // Get a random material
            Material randomMaterial = availableMaterials.get(random.nextInt(availableMaterials.size()));
            ItemStack randomItem = new ItemStack(randomMaterial);
            
            // Create a new recipe with the random item as the result
            Recipe shuffledRecipe = RecipeBuilderFactory.createRecipe(key, recipe, randomItem);
            
            // Store the shuffled recipe
            if (shuffledRecipe != null) {
                shuffledRecipes.put(key, shuffledRecipe);
            }
        }
        
        plugin.getLogger().info("Prepared " + shuffledRecipes.size() + " recipes with random items");
    }
    
    /**
     * Prepares recipe result recipes
     * Can be called asynchronously
     * @param random the random number generator
     */
    private void prepareRecipeResultRecipes(Random random) {
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
            Recipe shuffledRecipe = RecipeBuilderFactory.createRecipe(key, recipe, shuffledResult);
            
            // Store the shuffled recipe
            if (shuffledRecipe != null) {
                shuffledRecipes.put(key, shuffledRecipe);
            }
            
            i++;
        }
        
        plugin.getLogger().info("Prepared " + shuffledRecipes.size() + " recipes by swapping results");
    }
    
    /**
     * Registers the shuffled recipes on the server
     * Must be called on the main thread
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
        SchedulerUtil.runTask(plugin, () -> {
            // Clear server recipes
            clearServerRecipes();
            
            // Register original recipes
            for (Recipe recipe : originalRecipes.values()) {
                Bukkit.addRecipe(recipe);
            }
            
            // Sync client recipes
            syncClientRecipes();
            
            // Update shuffle state
            plugin.getDataManager().setShuffled(false);
            plugin.getDataManager().saveData();
            
            plugin.getLogger().info("Original recipes restored.");
        });
    }
} 