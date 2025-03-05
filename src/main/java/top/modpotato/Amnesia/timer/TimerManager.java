package top.modpotato.Amnesia.timer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import top.modpotato.Amnesia.Main;
import top.modpotato.Amnesia.util.MessageUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the timer for automatic recipe reshuffling
 */
public class TimerManager {
    private final Main plugin;
    private Object timerTask; // Can be BukkitTask or Folia's ScheduledTask
    private Object countdownTask; // Can be BukkitTask or Folia's ScheduledTask
    private boolean isRunning = false;
    
    /**
     * Creates a new TimerManager
     * @param plugin the plugin instance
     */
    public TimerManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the timer
     */
    public void startTimer() {
        if (isRunning) {
            return;
        }
        
        int interval = plugin.getConfigManager().getTimerInterval();
        
        // Schedule the timer task
        if (Main.isFolia()) {
            // Use Folia's region scheduler for global tasks
            timerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                shuffleRecipes();
            }, 0, interval * 20L); // Convert seconds to ticks
        } else {
            // Use Bukkit scheduler
            timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::shuffleRecipes, 0, interval * 20L);
        }
        
        isRunning = true;
        plugin.getLogger().info("Timer started with interval of " + interval + " seconds");
    }
    
    /**
     * Stops the timer
     */
    public void stopTimer() {
        if (!isRunning) {
            return;
        }
        
        // Cancel the timer task
        if (timerTask != null) {
            if (timerTask instanceof BukkitTask) {
                ((BukkitTask) timerTask).cancel();
            } else {
                // For Folia, we need to call cancel() on the ScheduledTask
                try {
                    timerTask.getClass().getMethod("cancel").invoke(timerTask);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to cancel timer task: " + e.getMessage());
                }
            }
            timerTask = null;
        }
        
        // Cancel the countdown task if running
        if (countdownTask != null) {
            if (countdownTask instanceof BukkitTask) {
                ((BukkitTask) countdownTask).cancel();
            } else {
                // For Folia, we need to call cancel() on the ScheduledTask
                try {
                    countdownTask.getClass().getMethod("cancel").invoke(countdownTask);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to cancel countdown task: " + e.getMessage());
                }
            }
            countdownTask = null;
        }
        
        isRunning = false;
        plugin.getLogger().info("Timer stopped");
    }
    
    /**
     * Restarts the timer with a new interval
     * @param interval the new interval in seconds
     */
    public void restartTimer(int interval) {
        stopTimer();
        plugin.getConfigManager().setTimerInterval(interval);
        startTimer();
    }
    
    /**
     * Shuffles recipes and starts a new timer
     */
    private void shuffleRecipes() {
        // Start countdown
        startCountdown();
    }
    
    /**
     * Starts the countdown before shuffling recipes
     */
    private void startCountdown() {
        List<Integer> intervals = plugin.getConfigManager().getNotificationIntervals();
        AtomicInteger secondsLeft = new AtomicInteger(intervals.get(0));
        
        if (Main.isFolia()) {
            // Use Folia's region scheduler for global tasks
            countdownTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                int seconds = secondsLeft.getAndDecrement();
                
                if (intervals.contains(seconds)) {
                    sendCountdownMessage(seconds);
                }
                
                if (seconds <= 0) {
                    task.cancel();
                    countdownTask = null;
                    
                    // Shuffle recipes - RecipeManager will handle thread safety
                    plugin.getRecipeManager().shuffleRecipes();
                }
            }, 0, 20L); // Run every second
        } else {
            // Use Bukkit scheduler
            countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                int seconds = secondsLeft.getAndDecrement();
                
                if (intervals.contains(seconds)) {
                    sendCountdownMessage(seconds);
                }
                
                if (seconds <= 0) {
                    ((BukkitTask) countdownTask).cancel();
                    countdownTask = null;
                    
                    // Shuffle recipes - RecipeManager will handle thread safety
                    plugin.getRecipeManager().shuffleRecipes();
                }
            }, 0, 20L); // Run every second
        }
    }
    
    /**
     * Sends a countdown message based on the remaining time
     * @param seconds the remaining time in seconds
     */
    private void sendCountdownMessage(int seconds) {
        String message;
        
        if (seconds == 300) { // 5 minutes
            message = plugin.getConfigManager().getNotificationMessages().countdownFiveMinutes;
        } else if (seconds == 60) { // 1 minute
            message = plugin.getConfigManager().getNotificationMessages().countdownOneMinute;
        } else if (seconds == 30) { // 30 seconds
            message = plugin.getConfigManager().getNotificationMessages().countdownThirtySeconds;
        } else if (seconds <= 10) { // 10 seconds or less
            message = MessageUtil.replacePlaceholders(
                    plugin.getConfigManager().getNotificationMessages().countdownTenSeconds,
                    "seconds", String.valueOf(seconds)
            );
        } else {
            return; // No message for this interval
        }
        
        MessageUtil.broadcastMessage(message);
    }
    
    /**
     * Checks if the timer is running
     * @return true if the timer is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
} 