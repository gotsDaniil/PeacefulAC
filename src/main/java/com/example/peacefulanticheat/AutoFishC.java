package com.example.peacefulanticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import static com.example.peacefulanticheat.PeacefulAntiCheat.idleTimes;


public class AutoFishC implements Listener {

    public void updatePlayerActivity(Player player) {
        idleTimes.put(player, System.currentTimeMillis());
    }

    public static class IdleListener implements org.bukkit.event.Listener {
        private final AutoFishC plugin;

        public IdleListener(AutoFishC plugin) {
            this.plugin = plugin;
        }

        @org.bukkit.event.EventHandler
        public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
            plugin.updatePlayerActivity(event.getPlayer());
        }

        @org.bukkit.event.EventHandler
        public void onPlayerChat(org.bukkit.event.player.PlayerChatEvent event) {
            plugin.updatePlayerActivity(event.getPlayer());
        }
    }
}