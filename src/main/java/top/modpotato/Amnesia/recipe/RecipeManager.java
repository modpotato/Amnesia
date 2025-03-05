package top.modpotato.Amnesia.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
import java.util.concurrent.CompletableFuture;
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
        if (plugin.getConfigManager().isShuffled()) {
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
        // Get the seed from config
        long seed = plugin.getConfigManager().getSeed();
        Random random = new Random(seed);
        
        // Get the shuffle mode from config
        String shuffleMode = plugin.getConfigManager().getShuffleMode();
        
        // Announce shuffle start if needed
        if (announce) {
            MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleStarted);
        }
        
        // Prepare recipe data asynchronously when possible
        CompletableFuture<Void> preparationFuture = CompletableFuture.runAsync(() -> {
            prepareRecipeData(shuffleMode, random);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error preparing recipe data: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
        
        // Apply changes on the main thread after preparation is complete
        preparationFuture.thenRun(() -> {
            if (Main.isFolia()) {
                // For Folia, we need to use the global region scheduler
                Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                    applyRecipeChanges(announce);
                });
            } else {
                // For Paper, we use the Bukkit scheduler
                Bukkit.getScheduler().runTask(plugin, () -> applyRecipeChanges(announce));
            }
        });
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
                if (Bukkit.isPrimaryThread()) {
                    storeOriginalRecipes();
                } else {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    if (Main.isFolia()) {
                        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                            storeOriginalRecipes();
                            future.complete(null);
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            storeOriginalRecipes();
                            future.complete(null);
                        });
                    }
                    future.join(); // Wait for completion
                }
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
        try {
            // Remove all recipes from the server
            clearServerRecipes();
            
            // Register shuffled recipes
            registerShuffledRecipes();
            
            // Update shuffle state in config
            plugin.getConfigManager().setShuffled(true);
            plugin.getConfigManager().saveConfig();
            
            // Handle client recipe synchronization based on config
            syncClientRecipes();
            
            // Announce shuffle completion if needed
            if (announce) {
                MessageUtil.broadcastMessage(plugin.getConfigManager().getNotificationMessages().shuffleFinished);
            }
            
            plugin.getLogger().info("Recipes shuffled successfully with seed: " + plugin.getConfigManager().getSeed() + 
                    " (" + (plugin.getConfigManager().isUserSetSeed() ? "user-set" : "random") + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Error applying recipe changes: " + e.getMessage());
            e.printStackTrace();
            
            // Try to restore original recipes if possible
            try {
                restoreOriginalRecipes();
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to restore original recipes: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
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
                plugin.getLogger().warning("Unknown client sync mode: " + syncMode + ". Using resync mode.");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.updateCommands();
                    player.discoverRecipes(shuffledRecipes.keySet());
                }
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
        List<Material> availableMaterials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !isExcludedRandomItem(material)) {
                availableMaterials.add(material);
            }
        }
        
        // Prepare shuffled recipes
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
            Recipe shuffledRecipe = createShuffledRecipe(key, recipe, shuffledResult);
            
            // Store the shuffled recipe
            if (shuffledRecipe != null) {
                shuffledRecipes.put(key, shuffledRecipe);
            }
            
            i++;
        }
        
        plugin.getLogger().info("Prepared " + shuffledRecipes.size() + " recipes by swapping results");
    }
    
    /**
     * Creates a shuffled recipe with a new result
     * Can be called asynchronously
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
            
            // Copy ingredients with null check
            for (Map.Entry<Character, RecipeChoice> entry : original.getChoiceMap().entrySet()) {
                if (entry.getValue() != null) {
                    shuffled.setIngredient(entry.getKey(), entry.getValue());
                }
            }
            
            return shuffled;
        } else if (originalRecipe instanceof ShapelessRecipe) {
            ShapelessRecipe original = (ShapelessRecipe) originalRecipe;
            ShapelessRecipe shuffled = new ShapelessRecipe(key, newResult);
            
            // Copy ingredients with null check
            for (RecipeChoice ingredient : original.getChoiceList()) {
                if (ingredient != null) {
                    shuffled.addIngredient(ingredient);
                }
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
        if (!plugin.getConfigManager().isShuffled()) {
            return;
        }
        
        // We need to run recipe manipulation on the main thread
        Runnable restoreTask = () -> {
            try {
                // Clear all recipes
                clearServerRecipes();
                
                // Register original recipes
                for (Recipe recipe : originalRecipes.values()) {
                    Bukkit.addRecipe(recipe);
                }
                
                // Handle client recipe synchronization based on config
                String syncMode = plugin.getConfigManager().getClientSyncMode();
                if (syncMode.equalsIgnoreCase("resync") || syncMode.equalsIgnoreCase("clear")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.updateCommands();
                        player.discoverRecipes(originalRecipes.keySet());
                    }
                }
                
                // Clear shuffled recipes
                shuffledRecipes.clear();
                
                // Update shuffle state in config
                plugin.getConfigManager().setShuffled(false);
                plugin.getConfigManager().saveConfig();
                
                plugin.getLogger().info("Restored " + originalRecipes.size() + " original recipes");
            } catch (Exception e) {
                plugin.getLogger().severe("Error restoring original recipes: " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        // Run on the main thread
        if (Bukkit.isPrimaryThread()) {
            restoreTask.run();
        } else {
            if (Main.isFolia()) {
                Bukkit.getGlobalRegionScheduler().run(plugin, task -> restoreTask.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, restoreTask);
            }
        }
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
} 