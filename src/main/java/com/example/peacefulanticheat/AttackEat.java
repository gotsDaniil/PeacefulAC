package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AttackEat implements Listener {

    private final ConfigManager configManager;

    public AttackEat(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if(player.isBlocking()) {
            event.setCancelled(true);
            String punishment3 = configManager.getPunishment3();
            String message3 = configManager.getMessage3();
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment3 + " " + player.getName() + " " + message3);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if(player.isHandRaised()) {
                event.setCancelled(true);
                String punishment3 = configManager.getPunishment3();
                String message3 = configManager.getMessage3();
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment3 + " " + player.getName() + " " + message3);
            }
        }
    }
}