package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class FTState implements Listener {

    // Проверка выключена, очень плохая логика проверки Killaura FTState
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && player.getAttackCooldown() > 0) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player) && target.getLocation().distance(player.getLocation()) < 0.5) {
                    player.kickPlayer("AC Вы были кикнуты за использование читов");
// Создаём NPC-бота за спиной игрока
                    createNCPBehindPlayer(player);

                    break;
                }
            }
        }
    }

    private void createNCPBehindPlayer(Player player) {
// Spawn NCP 0.5 seconds behind the player
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("YourPluginName"), () -> {
// Use ZNPCsPlus API to kick the player
        }, 10L); // 10 ticks = 0.5 seconds
    }
}