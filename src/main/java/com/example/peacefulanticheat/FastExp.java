package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

public class FastExp implements Listener {

    private final ConfigManager configManager;

    public FastExp(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private final HashMap<UUID, Integer> playerExperienceBottleCounts = new HashMap<>();
    private final HashMap<UUID, Long> playerLastUsedTime = new HashMap<>();

    @EventHandler
    public void onPlayerUseExperienceBottle(PlayerInteractEvent event) {
        if (event.getMaterial() == Material.EXPERIENCE_BOTTLE) {
            UUID playerId = event.getPlayer().getUniqueId();
            long currentTime = System.currentTimeMillis();

            // Проверяем, сколько пузырьков опыта использовал игрок за последнюю секунду
            if (playerLastUsedTime.containsKey(playerId) &&
                    (currentTime - playerLastUsedTime.get(playerId)) < 500) {
                int count = playerExperienceBottleCounts.getOrDefault(playerId, 0) + 1;

                // Если игрок использовал 10 или более пузырьков, отменяем событие
                if (count >= 10) {
                    event.setCancelled(true);
                    String punishment8 = configManager.getPunishment8();
                    String message8 = configManager.getMessage8();
                    ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                    Bukkit.dispatchCommand(consoleSender, punishment8 + " " + event.getPlayer().getName() + " " + message8);
                    return;
                }

                playerExperienceBottleCounts.put(playerId, count);
            } else {
                // Сбрасываем счетчик при новом использовании после 1 секунды
                playerExperienceBottleCounts.put(playerId, 1);
            }

            // Обновляем время последнего использования
            playerLastUsedTime.put(playerId, currentTime);
        }
    }
}