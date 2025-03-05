package top.modpotato.Amnesia.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.modpotato.Amnesia.Main;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Manages the persistent data for the Amnesia plugin
 */
public class DataManager {
    private final Main plugin;
    private FileConfiguration data;
    private File dataFile;
    
    // Persistent data values
    private long seed;
    private boolean userSetSeed = false;
    private boolean isShuffled = false;
    private long lastShuffleTime = 0;
    
    /**
     * Creates a new DataManager
     * @param plugin the plugin instance
     */
    public DataManager(Main plugin) {
        this.plugin = plugin;
        this.seed = new Random().nextLong();
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }
    
    /**
     * Loads the data from the data.yml file
     */
    public void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load values from data file
        if (data.contains("seed")) {
            seed = data.getLong("seed");
            userSetSeed = data.getBoolean("user-set-seed", false);
        } else {
            seed = new Random().nextLong();
            userSetSeed = false;
        }
        
        // Load shuffle state
        isShuffled = data.getBoolean("is-shuffled", false);
        lastShuffleTime = data.getLong("last-shuffle-time", 0);
        
        // Save data to ensure all default values are saved
        saveData();
    }
    
    /**
     * Saves the data to the data.yml file
     */
    public void saveData() {
        if (data == null || dataFile == null) {
            return;
        }
        
        // Set values in data
        data.set("seed", seed);
        data.set("user-set-seed", userSetSeed);
        data.set("is-shuffled", isShuffled);
        data.set("last-shuffle-time", lastShuffleTime);
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
    
    /**
     * Gets the seed for recipe shuffling
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Sets the seed for recipe shuffling
     * @param seed the seed
     * @param userSet whether the seed was set by the user
     */
    public void setSeed(long seed, boolean userSet) {
        this.seed = seed;
        this.userSetSeed = userSet;
    }
    
    /**
     * Checks if the seed was set by the user
     * @return true if the seed was set by the user, false if it was generated randomly
     */
    public boolean isUserSetSeed() {
        return userSetSeed;
    }
    
    /**
     * Generates a new random seed
     * @return the new seed
     */
    public long generateRandomSeed() {
        this.seed = new Random().nextLong();
        this.userSetSeed = false;
        return this.seed;
    }
    
    /**
     * Checks if recipes are currently shuffled
     * @return true if recipes are shuffled, false otherwise
     */
    public boolean isShuffled() {
        return isShuffled;
    }
    
    /**
     * Sets whether recipes are currently shuffled
     * @param shuffled true if recipes are shuffled, false otherwise
     */
    public void setShuffled(boolean shuffled) {
        this.isShuffled = shuffled;
        this.lastShuffleTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the time when recipes were last shuffled
     * @return the last shuffle time in milliseconds since epoch
     */
    public long getLastShuffleTime() {
        return lastShuffleTime;
    }
} 