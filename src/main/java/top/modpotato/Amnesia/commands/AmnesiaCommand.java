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
        
        // Check if a mode was specified
        if (args.length > 1) {
            String mode = args[1].toLowerCase();
            if (mode.equals("random_item") || mode.equals("recipe_result")) {
                plugin.getConfigManager().setShuffleMode(mode);
                sender.sendMessage("§aShuffling recipes with mode: §e" + mode);
            } else {
                sender.sendMessage("§cInvalid shuffle mode. Use 'random_item' or 'recipe_result'.");
                return true;
            }
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
            long seed = plugin.getConfigManager().getSeed();
            boolean isUserSet = plugin.getConfigManager().isUserSetSeed();
            sender.sendMessage("§aCurrent seed: §e" + seed + " §7(" + (isUserSet ? "user-set" : "random") + ")");
            return true;
        }
        
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
                try {
                    long seed = Long.parseLong(args[2]);
                    plugin.getConfigManager().setSeed(seed, true); // Mark as user-set
                    plugin.getConfigManager().saveConfig();
                    sender.sendMessage("§aSeed set to §e" + seed + " §7(user-set)");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid seed. Please enter a number.");
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("random")) {
                long seed = plugin.getConfigManager().generateRandomSeed();
                plugin.getConfigManager().saveConfig();
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
        sender.sendMessage("§e/amnesia shuffle [mode] §7- Shuffle recipes");
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
            // Second argument - depends on first argument
            switch (args[0].toLowerCase()) {
                case "shuffle":
                    if (sender.hasPermission("amnesia.command.shuffle")) {
                        return filterCompletions(Arrays.asList("random_item", "recipe_result"), args[1]);
                    }
                    break;
                case "timer":
                    if (sender.hasPermission("amnesia.command.timer")) {
                        return filterCompletions(Arrays.asList("enable", "disable", "interval"), args[1]);
                    }
                    break;
                case "seed":
                    if (sender.hasPermission("amnesia.command.seed")) {
                        return filterCompletions(Arrays.asList("view", "set", "random"), args[1]);
                    }
                    break;
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