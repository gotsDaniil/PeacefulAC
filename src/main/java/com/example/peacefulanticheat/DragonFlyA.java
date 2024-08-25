package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class DragonFlyA implements Listener {

    private final ConfigManager configManager;

    public DragonFlyA(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Проверяем, находится ли игрок в креативе или флае
        if (player.getGameMode() == GameMode.CREATIVE || player.isFlying()) {
            float flySpeed = player.getFlySpeed();

            // Магические вычисления скорости игрока для кажого спида
            if (flySpeed > 0.1f && (double)flySpeed <= 0.2f) {
                handleMovementCheck(event, 45.5, 28);
            } else if (Float.compare(flySpeed, 0.1f) == 0) {
                handleMovementCheck(event, 22.5, 28);
            } else if (flySpeed > 0.2f && (double)flySpeed <= 0.3f) {
                handleMovementCheck(event, 65.5, 36);
            } else if (flySpeed > 0.3f && (double)flySpeed <= 0.4f) {
                handleMovementCheck(event, 88.5, 46);
            } else if (flySpeed > 0.4f && (double)flySpeed <= 0.5f) {
                handleMovementCheck(event, 110.5, 56);
            } else if (flySpeed > 0.5f && (double)flySpeed <= 0.6f) {
                handleMovementCheck(event, 132.5, 66);
            } else if (flySpeed > 0.6f && (double)flySpeed <= 0.7f) {
                handleMovementCheck(event, 146.5, 76);
            } else if (flySpeed > 0.7f && (double)flySpeed <= 0.8f) {
                handleMovementCheck(event, 173.5, 86);
            } else if (flySpeed > 0.8f && (double)flySpeed <= 0.9f) {
                handleMovementCheck(event, 178.5, 96);
            } else if (flySpeed > 0.9f && (double)flySpeed <= 1f) {
                handleMovementCheck(event, 215.5, 106);
            }
        }
    }

    private void handleMovementCheck(PlayerMoveEvent event, double xzThreshold, double yThreshold) {
        double deltaX = event.getTo().getX() - event.getFrom().getX();
        double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        double speedXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20; // Умножаем на 20 для перевода в секунды

        double deltaY = event.getTo().getY() - event.getFrom().getY();
        double speedY = Math.abs(deltaY) * 20; // Мы просто берем абсолютное значение изменения Y и умножаем

        // Отменяет ивент, если игрок быстро движется по X, Z или Y
        if (speedXZ >= xzThreshold || speedY >= yThreshold) {
            event.setCancelled(true);
            String punishment7 = configManager.getPunishment7();
            String message7 = configManager.getMessage7();
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment7 + " " + event.getPlayer().getName() + " " + message7);
        }
    }
}