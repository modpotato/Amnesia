package top.modpotato.Amnesia.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Utility class for handling messages
 */
public class MessageUtil {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    /**
     * Broadcasts a message to all online players
     * @param message the message to broadcast
     */
    public static void broadcastMessage(String message) {
        Component component = miniMessage.deserialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        
        // Also log to console
        Bukkit.getConsoleSender().sendMessage(component);
    }
    
    /**
     * Sends a message to a player
     * @param player the player to send the message to
     * @param message the message to send
     */
    public static void sendMessage(Player player, String message) {
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }
    
    /**
     * Replaces placeholders in a message
     * @param message the message
     * @param placeholders the placeholders and their values
     * @return the message with placeholders replaced
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be in pairs of placeholder and value");
        }
        
        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            result = result.replace("<" + placeholder + ">", value);
        }
        
        return result;
    }
} 