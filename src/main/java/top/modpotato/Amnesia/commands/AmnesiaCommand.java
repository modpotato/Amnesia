package top.modpotato.Amnesia.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import top.modpotato.Amnesia.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /amnesia command
 */
public class AmnesiaCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    
    /**
     * Creates a new AmnesiaCommand
     * @param plugin the plugin instance
     */
    public AmnesiaCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "shuffle":
                return handleShuffleCommand(sender, args);
            case "timer":
                return handleTimerCommand(sender, args);
            case "seed":
                return handleSeedCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handles the /amnesia shuffle command
     * @param sender the command sender
     * @param args the command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleShuffleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amnesia.command.shuffle")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        String mode = null;
        Long seedValue = null;
        
        // Parse arguments
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            
            if (arg.equals("random_item") || arg.equals("recipe_result")) {
                mode = arg;
            } else if (arg.equals("seed") && i + 1 < args.length) {
                try {
                    seedValue = Long.parseLong(args[i + 1]);
                    i++; // Skip the next argument as we've processed it
                } catch (NumberFormatException e) {
                    if (args[i + 1].equalsIgnoreCase("random")) {
                        // Generate a random seed
                        seedValue = plugin.getDataManager().generateRandomSeed();
                        plugin.getDataManager().saveData();
                        sender.sendMessage("§aGenerated random seed: §e" + seedValue);
                    } else {
                        sender.sendMessage("§cInvalid seed value. Use a number or 'random'.");
                        return true;
                    }
                }
            }
        }
        
        // Set mode if specified
        if (mode != null) {
            plugin.getConfigManager().setShuffleMode(mode);
            plugin.getConfigManager().saveConfig();
            sender.sendMessage("§aShuffling recipes with mode: §e" + mode);
        }
        
        // Set seed if specified
        if (seedValue != null) {
            plugin.getDataManager().setSeed(seedValue, true);
            plugin.getDataManager().saveData();
            sender.sendMessage("§aUsing seed: §e" + seedValue);
        }
        
        // Shuffle recipes - RecipeManager will handle thread safety
        sender.sendMessage("§aStarting recipe shuffle...");
        plugin.getRecipeManager().shuffleRecipes();
        
        return true;
    }
    
    /**
     * Handles the /amnesia timer command
     * @param sender the command sender
     * @param args the command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleTimerCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amnesia.command.timer")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 1) {
            // Show current timer status
            boolean isRunning = plugin.getTimerManager().isRunning();
            int interval = plugin.getConfigManager().getTimerInterval();
            sender.sendMessage("§aTimer status: " + (isRunning ? "§eEnabled" : "§cDisabled"));
            sender.sendMessage("§aTimer interval: §e" + interval + " seconds");
            return true;
        }
        
        if (args.length >= 2) {
            String subCommand = args[1].toLowerCase();
            
            if (subCommand.equals("enable")) {
                plugin.getConfigManager().setTimerEnabled(true);
                plugin.getConfigManager().saveConfig();
                plugin.getTimerManager().startTimer();
                sender.sendMessage("§aTimer enabled.");
                return true;
            } else if (subCommand.equals("disable")) {
                plugin.getConfigManager().setTimerEnabled(false);
                plugin.getConfigManager().saveConfig();
                plugin.getTimerManager().stopTimer();
                sender.sendMessage("§aTimer disabled.");
                return true;
            } else if (subCommand.equals("interval") && args.length >= 3) {
                try {
                    int interval = Integer.parseInt(args[2]);
                    if (interval <= 0) {
                        sender.sendMessage("§cInterval must be greater than 0.");
                        return true;
                    }
                    
                    plugin.getConfigManager().setTimerInterval(interval);
                    plugin.getConfigManager().saveConfig();
                    
                    if (plugin.getTimerManager().isRunning()) {
                        plugin.getTimerManager().restartTimer(interval);
                        sender.sendMessage("§aTimer interval set to §e" + interval + " seconds §aand timer restarted.");
                    } else {
                        sender.sendMessage("§aTimer interval set to §e" + interval + " seconds§a.");
                    }
                    
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid interval. Please enter a number.");
                    return true;
                }
            }
        }
        
        sender.sendMessage("§cUsage: /amnesia timer [enable|disable|interval <seconds>]");
        return true;
    }
    
    /**
     * Handles the /amnesia seed command
     * @param sender the command sender
     * @param args the command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleSeedCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amnesia.command.seed")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 1 || args[1].equalsIgnoreCase("view")) {
            // Show current seed
            long seed = plugin.getDataManager().getSeed();
            boolean isUserSet = plugin.getDataManager().isUserSetSeed();
            sender.sendMessage("§aCurrent seed: §e" + seed + " §7(" + (isUserSet ? "user-set" : "random") + ")");
            return true;
        }
        
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
                try {
                    long seed = Long.parseLong(args[2]);
                    plugin.getDataManager().setSeed(seed, true); // Mark as user-set
                    plugin.getDataManager().saveData();
                    sender.sendMessage("§aSeed set to §e" + seed + " §7(user-set)");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid seed. Please enter a number.");
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("random")) {
                long seed = plugin.getDataManager().generateRandomSeed();
                plugin.getDataManager().saveData();
                sender.sendMessage("§aGenerated random seed: §e" + seed + " §7(random)");
                return true;
            }
        }
        
        sender.sendMessage("§cUsage: /amnesia seed [view|set <seed>|random]");
        return true;
    }
    
    /**
     * Handles the /amnesia reload command
     * @param sender the command sender
     * @return true if the command was handled, false otherwise
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("amnesia.command.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        // Reload config
        plugin.getConfigManager().reloadConfig();
        
        // Restart timer if enabled
        if (plugin.getConfigManager().isTimerEnabled()) {
            plugin.getTimerManager().restartTimer(plugin.getConfigManager().getTimerInterval());
        } else {
            plugin.getTimerManager().stopTimer();
        }
        
        sender.sendMessage("§aAmnesia configuration reloaded.");
        return true;
    }
    
    /**
     * Sends the help message to a command sender
     * @param sender the command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== Amnesia Commands ===");
        sender.sendMessage("§e/amnesia shuffle [mode] [seed <value>|random] §7- Shuffle recipes");
        sender.sendMessage("§e/amnesia timer [enable|disable|interval <seconds>] §7- Manage timer");
        sender.sendMessage("§e/amnesia seed [view|set <seed>|random] §7- Manage seed");
        sender.sendMessage("§e/amnesia reload §7- Reload configuration");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = new ArrayList<>();
            
            if (sender.hasPermission("amnesia.command.shuffle")) {
                subCommands.add("shuffle");
            }
            
            if (sender.hasPermission("amnesia.command.timer")) {
                subCommands.add("timer");
            }
            
            if (sender.hasPermission("amnesia.command.seed")) {
                subCommands.add("seed");
            }
            
            if (sender.hasPermission("amnesia.command.reload")) {
                subCommands.add("reload");
            }
            
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            // Second argument - subcommand options
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("shuffle") && sender.hasPermission("amnesia.command.shuffle")) {
                return filterCompletions(Arrays.asList("random_item", "recipe_result", "seed"), args[1]);
            } else if (subCommand.equals("timer") && sender.hasPermission("amnesia.command.timer")) {
                return filterCompletions(Arrays.asList("enable", "disable", "interval"), args[1]);
            } else if (subCommand.equals("seed") && sender.hasPermission("amnesia.command.seed")) {
                return filterCompletions(Arrays.asList("view", "set", "random"), args[1]);
            }
        } else if (args.length == 3) {
            // Third argument - specific options
            String subCommand = args[0].toLowerCase();
            String option = args[1].toLowerCase();
            
            if (subCommand.equals("shuffle") && option.equals("seed")) {
                return filterCompletions(Arrays.asList("random"), args[2]);
            }
        }
        
        return completions;
    }
    
    /**
     * Filters completions based on the current input
     * @param options the available options
     * @param input the current input
     * @return the filtered completions
     */
    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
} 