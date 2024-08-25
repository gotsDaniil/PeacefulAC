package com.example.peacefulanticheat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

// Выключено, требует доработки
public class ElytraFly implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();


        Material mainHandMaterial = player.getInventory().getItemInMainHand().getType();
        Material offHandMaterial = player.getInventory().getItemInOffHand().getType();

        if ((player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() != Material.ELYTRA &&
                mainHandMaterial != Material.FIREWORK_ROCKET) || player.getInventory().getChestplate() != null &&
                (player.getInventory().getChestplate().getType() != Material.ELYTRA && offHandMaterial != Material.FIREWORK_ROCKET)) {
            handleMovementCheck(event, 7.5);
        }
    }

    private void handleMovementCheck(PlayerMoveEvent event, double yThreshold) {
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        double speedY = Math.abs(deltaY) * 20; // Берем абсолютное значение изменения Y и умножаем

        // Отменяет событие, если игрок быстро движется по X, Z или Y
        if (speedY >= yThreshold) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("speedY " + speedY);
        }
    }
}