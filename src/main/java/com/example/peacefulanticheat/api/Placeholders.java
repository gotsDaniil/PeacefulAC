package com.example.peacefulanticheat.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class Placeholders implements Listener {

    public static String replacePlaceholders(Player player, String player_name) {
        if (player == null) {
            return player_name;
        }

        player_name = player_name.replace("%player%", player.getName());

        return player_name;
    }
}
