package top.modpotato.Amnesia.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.modpotato.Amnesia.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Manages the configuration for the Amnesia plugin
 */
public class ConfigManager {
    private final Main plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Default configuration values
    private String shuffleMode = "random_item";
    private int timerInterval = 3600;
    private boolean timerEnabled = false;
    private String clientSyncMode = "resync";
    private long seed;
    private boolean userSetSeed = false; // Track if the seed was set by the user
    private boolean isShuffled = false; // Track if recipes are currently shuffled
    private long lastShuffleTime = 0; // Track when recipes were last shuffled
    private List<String> excludedRecipes = new ArrayList<>();
    private List<String> excludedRandomItems = new ArrayList<>();
    private List<Integer> notificationIntervals = Arrays.asList(300, 60, 30, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
    private NotificationMessages notificationMessages = new NotificationMessages();
    
    /**
     * Creates a new ConfigManager
     * @param plugin the plugin instance
     */
    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.seed = new Random().nextLong();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Loads the configuration from the config.yml file
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            if (!configFile.exists()) {
                try {
                    plugin.getDataFolder().mkdirs();
                    configFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create config.yml: " + e.getMessage());
                }
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load values from config
        shuffleMode = config.getString("shuffle-mode", shuffleMode);
        timerInterval = config.getInt("timer-interval", timerInterval);
        timerEnabled = config.getBoolean("timer-enabled", timerEnabled);
        clientSyncMode = config.getString("client-sync-mode", clientSyncMode);
        
        // Load seed and seed state
        if (config.contains("seed")) {
            seed = config.getLong("seed");
            userSetSeed = config.getBoolean("user-set-seed", false);
        } else {
            seed = new Random().nextLong();
            userSetSeed = false;
        }
        
        // Load shuffle state
        isShuffled = config.getBoolean("is-shuffled", false);
        lastShuffleTime = config.getLong("last-shuffle-time", 0);
        
        excludedRecipes = config.getStringList("excluded-recipes");
        excludedRandomItems = config.getStringList("excluded-random-items");
        
        // Load notification intervals
        if (config.contains("notification-intervals")) {
            notificationIntervals = config.getIntegerList("notification-intervals");
        }
        
        // Load notification messages
        if (config.contains("notification-messages")) {
            notificationMessages.countdownFiveMinutes = config.getString("notification-messages.countdown-5-minutes", 
                    notificationMessages.countdownFiveMinutes);
            notificationMessages.countdownOneMinute = config.getString("notification-messages.countdown-1-minute", 
                    notificationMessages.countdownOneMinute);
            notificationMessages.countdownThirtySeconds = config.getString("notification-messages.countdown-30-seconds", 
                    notificationMessages.countdownThirtySeconds);
            notificationMessages.countdownTenSeconds = config.getString("notification-messages.countdown-10-seconds", 
                    notificationMessages.countdownTenSeconds);
            notificationMessages.countdownStart = config.getString("notification-messages.countdown-start", 
                    notificationMessages.countdownStart);
            notificationMessages.shuffleStarted = config.getString("notification-messages.shuffle-started", 
                    notificationMessages.shuffleStarted);
            notificationMessages.shuffleFinished = config.getString("notification-messages.shuffle-finished", 
                    notificationMessages.shuffleFinished);
        }
        
        // Save config to ensure all default values are saved
        saveConfig();
    }
    
    /**
     * Saves the configuration to the config.yml file
     */
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        
        // Set values in config
        config.set("shuffle-mode", shuffleMode);
        config.set("timer-interval", timerInterval);
        config.set("timer-enabled", timerEnabled);
        config.set("client-sync-mode", clientSyncMode);
        config.set("seed", seed);
        config.set("user-set-seed", userSetSeed);
        config.set("is-shuffled", isShuffled);
        config.set("last-shuffle-time", lastShuffleTime);
        config.set("excluded-recipes", excludedRecipes);
        config.set("excluded-random-items", excludedRandomItems);
        config.set("notification-intervals", notificationIntervals);
        
        // Set notification messages
        config.set("notification-messages.countdown-5-minutes", notificationMessages.countdownFiveMinutes);
        config.set("notification-messages.countdown-1-minute", notificationMessages.countdownOneMinute);
        config.set("notification-messages.countdown-30-seconds", notificationMessages.countdownThirtySeconds);
        config.set("notification-messages.countdown-10-seconds", notificationMessages.countdownTenSeconds);
        config.set("notification-messages.countdown-start", notificationMessages.countdownStart);
        config.set("notification-messages.shuffle-started", notificationMessages.shuffleStarted);
        config.set("notification-messages.shuffle-finished", notificationMessages.shuffleFinished);
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    /**
     * Gets the shuffle mode
     * @return the shuffle mode
     */
    public String getShuffleMode() {
        return shuffleMode;
    }
    
    /**
     * Sets the shuffle mode
     * @param shuffleMode the shuffle mode
     */
    public void setShuffleMode(String shuffleMode) {
        this.shuffleMode = shuffleMode;
    }
    
    /**
     * Gets the timer interval in seconds
     * @return the timer interval
     */
    public int getTimerInterval() {
        return timerInterval;
    }
    
    /**
     * Sets the timer interval in seconds
     * @param timerInterval the timer interval
     */
    public void setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
    }
    
    /**
     * Checks if the timer is enabled
     * @return true if the timer is enabled, false otherwise
     */
    public boolean isTimerEnabled() {
        return timerEnabled;
    }
    
    /**
     * Sets whether the timer is enabled
     * @param timerEnabled true to enable the timer, false to disable
     */
    public void setTimerEnabled(boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
    }
    
    /**
     * Gets the client sync mode
     * @return the client sync mode
     */
    public String getClientSyncMode() {
        return clientSyncMode;
    }
    
    /**
     * Sets the client sync mode
     * @param clientSyncMode the client sync mode
     */
    public void setClientSyncMode(String clientSyncMode) {
        this.clientSyncMode = clientSyncMode;
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
        if (shuffled) {
            this.lastShuffleTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Gets the time when recipes were last shuffled
     * @return the last shuffle time in milliseconds since epoch
     */
    public long getLastShuffleTime() {
        return lastShuffleTime;
    }
    
    /**
     * Gets the list of excluded recipes
     * @return the excluded recipes
     */
    public List<String> getExcludedRecipes() {
        return excludedRecipes;
    }
    
    /**
     * Gets the list of excluded random items
     * @return the excluded random items
     */
    public List<String> getExcludedRandomItems() {
        return excludedRandomItems;
    }
    
    /**
     * Gets the notification intervals
     * @return the notification intervals
     */
    public List<Integer> getNotificationIntervals() {
        return notificationIntervals;
    }
    
    /**
     * Gets the notification messages
     * @return the notification messages
     */
    public NotificationMessages getNotificationMessages() {
        return notificationMessages;
    }
    
    /**
     * Reloads the configuration from the config.yml file
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    /**
     * Container class for notification messages
     */
    public static class NotificationMessages {
        public String countdownFiveMinutes = "<gold>5 minutes until recipes are shuffled!</gold>";
        public String countdownOneMinute = "<yellow>1 minute until recipes are shuffled!</yellow>";
        public String countdownThirtySeconds = "<yellow>30 seconds until recipes are shuffled!</yellow>";
        public String countdownTenSeconds = "<red>Recipes will shuffle in <bold><seconds></bold> seconds!</red>";
        public String countdownStart = "<red><bold>Recipes will shuffle in...</bold></red>";
        public String shuffleStarted = "<green><bold>Recipes have been shuffled!</bold></green>";
        public String shuffleFinished = "<green>Recipe shuffling complete.</green>";
    }
} 