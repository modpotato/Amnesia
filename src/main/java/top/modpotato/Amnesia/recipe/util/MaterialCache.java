package top.modpotato.Amnesia.recipe.util;

import org.bukkit.Material;
import top.modpotato.Amnesia.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for caching materials
 */
public class MaterialCache {
    private static List<Material> allMaterials;
    private static List<Material> itemMaterials;
    
    /**
     * Gets all materials
     * @return a list of all materials
     */
    public static List<Material> getAllMaterials() {
        if (allMaterials == null) {
            allMaterials = new ArrayList<>();
            for (Material material : Material.values()) {
                allMaterials.add(material);
            }
        }
        return allMaterials;
    }
    
    /**
     * Gets all item materials
     * @return a list of all item materials
     */
    public static List<Material> getItemMaterials() {
        if (itemMaterials == null) {
            itemMaterials = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem()) {
                    itemMaterials.add(material);
                }
            }
        }
        return itemMaterials;
    }
    
    /**
     * Gets all item materials that match a predicate
     * @param predicate the predicate to match
     * @return a list of all item materials that match the predicate
     */
    public static List<Material> getItemMaterials(Predicate<Material> predicate) {
        List<Material> result = new ArrayList<>();
        for (Material material : getItemMaterials()) {
            if (predicate.test(material)) {
                result.add(material);
            }
        }
        return result;
    }
    
    /**
     * Checks if a material is excluded from random item selection
     * @param plugin the plugin instance
     * @param material the material to check
     * @return true if the material is excluded, false otherwise
     */
    public static boolean isExcludedRandomItem(Main plugin, Material material) {
        List<String> excludedItems = plugin.getConfigManager().getExcludedRandomItems();
        return excludedItems.contains("minecraft:" + material.name().toLowerCase());
    }
    
    /**
     * Gets all item materials that are not excluded from random item selection
     * @param plugin the plugin instance
     * @return a list of all item materials that are not excluded
     */
    public static List<Material> getAvailableRandomItemMaterials(Main plugin) {
        return getItemMaterials(material -> !isExcludedRandomItem(plugin, material));
    }
} 