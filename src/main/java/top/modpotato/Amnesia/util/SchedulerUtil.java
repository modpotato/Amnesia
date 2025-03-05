package top.modpotato.Amnesia.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import top.modpotato.Amnesia.Main;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for handling scheduler operations across different server implementations
 */
public class SchedulerUtil {
    
    /**
     * Runs a task on the main thread
     * @param plugin the plugin instance
     * @param task the task to run
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            if (Main.isFolia()) {
                Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }
    
    /**
     * Runs a task on the main thread and returns a CompletableFuture
     * @param plugin the plugin instance
     * @param task the task to run
     * @return a CompletableFuture that completes when the task is done
     */
    public static CompletableFuture<Void> runTaskAsync(Plugin plugin, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        if (Bukkit.isPrimaryThread()) {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            if (Main.isFolia()) {
                Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        task.run();
                        future.complete(null);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            }
        }
        
        return future;
    }
    
    /**
     * Runs a task asynchronously
     * @param plugin the plugin instance
     * @param task the task to run
     * @return a CompletableFuture that completes when the task is done
     */
    public static CompletableFuture<Void> runAsync(Plugin plugin, Runnable task) {
        return CompletableFuture.runAsync(task)
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Error in async task: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }
} 