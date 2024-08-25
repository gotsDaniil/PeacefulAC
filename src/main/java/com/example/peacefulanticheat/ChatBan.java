package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

public class ChatBan implements Listener {

    private final ConfigManager configManager;

    public ChatBan(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        // Проверяем сообщения в чате и баним игрока, если обнаружены эти сообщения
        if (message.contains("нурик топ") || message.contains("nursultan топ") ||
                message.contains("какой чит выбрать") || message.contains("wild топ") ||
                message.contains("arbuz client топ") || message.contains("вилд топ") ||
                message.contains("arbuz топ") || message.contains("арбуз топ") ||
                message.contains("нурик имба") || message.contains("nursultan имба") ||
                message.contains("нурик пена") || message.contains("nursultan пена") ||
                message.contains("вилд пена") || message.contains("wild пена") ||
                message.contains("арбуз пена") || message.contains("arbuz пена") ||
                message.contains("нурик бустит") || message.contains("nursultan бустит") ||
                message.contains("arbuz бустит") || message.contains("флюгер бустит") ||
                message.contains("кфг флюгера бустит") || message.contains("флюгер клиент бустит") ||
                message.contains("Fluger Client бустит") || message.contains("Expensive топ")) {

            String punishment5 = configManager.getPunishment5();
            String message5 = configManager.getMessage5();
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment5 + " " + player.getName() + " " + message5);
        }
    }
}