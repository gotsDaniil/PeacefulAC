package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AutoFishA implements Listener {

    private final ConfigManager configManager;
    private final Map<String, Integer> playerViolationCount = new HashMap<>();
    private final int maxViolations = 1;

    public AutoFishA(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            String playerName = player.getName();

            // Проверка, есть ли рыба в инвентаре игрока
            if (!player.getInventory().contains(Material.COD)
                    || !player.getInventory().contains(Material.SALMON)
                    || !player.getInventory().contains(Material.TROPICAL_FISH)
                    || !player.getInventory().contains(Material.PUFFERFISH)) {

                int violations = playerViolationCount.getOrDefault(playerName, 0);

                // Логика проверки
                Location fishLocation = event.getHook().getLocation();
                Location playerLocation = player.getLocation();

                double distanceMoved = playerLocation.distance(fishLocation);
                if (distanceMoved <= 1.5) {
                    violations++;
                    playerViolationCount.put(playerName, violations);

                    if (violations >= maxViolations) {
                        event.setCancelled(true);
                        String punishment = configManager.getPunishment();
                        String message = configManager.getMessage();

                        ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                        Bukkit.dispatchCommand(consoleSender, punishment + " " + playerName + " " + message);
                        playerViolationCount.remove(playerName); // Сбрасываем нарушения игрока
                    }
                } else {
                    playerViolationCount.put(playerName, 0);
                }
            }
        }
    }
}