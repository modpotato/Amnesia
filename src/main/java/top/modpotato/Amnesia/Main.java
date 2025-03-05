package top.modpotato.Amnesia;

import org.bukkit.plugin.java.JavaPlugin;
import top.modpotato.Amnesia.commands.AmnesiaCommand;
import top.modpotato.Amnesia.config.ConfigManager;
import top.modpotato.Amnesia.config.DataManager;
import top.modpotato.Amnesia.recipe.RecipeManager;
import top.modpotato.Amnesia.timer.TimerManager;

/**
 * Main plugin class for Amnesia
 * A plugin that shuffles crafting recipes in Minecraft
 */
public class Main extends JavaPlugin {
    private static boolean isFolia;
    private static Main instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private RecipeManager recipeManager;
    private TimerManager timerManager;

    @Override
    public void onEnable() {
        try {
            instance = this;
            isFolia = checkFolia();
            getLogger().info("Running on " + (isFolia ? "Folia" : "Paper") + " server");
            
            // Initialize config manager
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            // Initialize data manager
            dataManager = new DataManager(this);
            dataManager.loadData();
            
            // Initialize recipe manager
            recipeManager = new RecipeManager(this);
            
            // Initialize timer manager
            timerManager = new TimerManager(this);
            
            // Register commands
            getCommand("amnesia").setExecutor(new AmnesiaCommand(this));
            
            // Initialize recipe manager (restore shuffle state if needed)
            recipeManager.initialize();
            
            // Start timer if enabled in config
            if (configManager.isTimerEnabled()) {
                timerManager.startTimer();
                getLogger().info("Timer started with interval of " + configManager.getTimerInterval() + " seconds");
            }
            
            getLogger().info("Amnesia has been enabled!");
        } catch (Exception e) {
            getLogger().severe("Error enabling Amnesia: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Stop timer if running
            if (timerManager != null) {
                timerManager.stopTimer();
            }
            
            // Save config
            if (configManager != null) {
                configManager.saveConfig();
            }
            
            // Save data
            if (dataManager != null) {
                dataManager.saveData();
            }
            
            getLogger().info("Amnesia has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error disabling Amnesia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if the server is running on Folia
     * @return true if running on Folia, false otherwise
     */
    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets the instance of the plugin
     * @return the plugin instance
     */
    public static Main getInstance() {
        return instance;
    }
    
    /**
     * Checks if the server is running on Folia
     * @return true if running on Folia, false otherwise
     */
    public static boolean isFolia() {
        return isFolia;
    }
    
    /**
     * Gets the config manager
     * @return the config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets the data manager
     * @return the data manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Gets the recipe manager
     * @return the recipe manager
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    /**
     * Gets the timer manager
     * @return the timer manager
     */
    public TimerManager getTimerManager() {
        return timerManager;
    }
}