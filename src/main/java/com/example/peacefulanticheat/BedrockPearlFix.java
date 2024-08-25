package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class BedrockPearlFix implements Listener {

    private final ConfigManager configManager;

    public BedrockPearlFix(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Location playerLocation = event.getPlayer().getLocation();

        // Проверяем, что игрок находится в аду
        if (!playerLocation.getWorld().getName().equals("world_nether")) {
            return; // Если нет, ничего не делаем
        }

        // Получаем блок под игроком
        Location blockLocation = playerLocation.clone().subtract(0.3, 0.000000000000001, 0.3);

        // Проверяем, находится ли бедрок под игроком
        if (blockLocation.getBlock().getType() == Material.BEDROCK) {
            double distanceToBedrock = playerLocation.distance(blockLocation);
            // Если расстояние меньше или равно 0.6000000000000001
            if (distanceToBedrock <= 0.6000000000000001) {
                // Телепортируем игрока на 1 блок вниз
                Location newLocation = playerLocation.clone().subtract(0, 1, 0);
                event.getPlayer().teleport(newLocation);
                String punishment4 = configManager.getPunishment4();
                String message4 = configManager.getMessage4();
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment4 + " " + event.getPlayer().getName() + " " + message4);
            }
        }
    }
}