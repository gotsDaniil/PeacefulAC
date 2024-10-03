package com.example.peacefulanticheat.Checks.utils;

import com.example.peacefulanticheat.ConfigManager;
import com.example.peacefulanticheat.api.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NicknameBans implements Listener {
    // Баним игроков за ники, если в них содержится одно из словосочетаний
    private final ConfigManager configManager;
    private final Set<String> badNicknames = new HashSet<>(Arrays.asList(
            "nurik", "nursultan", "arbuz_client", "wild", "expensive", "delta", "celka", "celestial",
            "ExpRandom", "bebraproxy", "Meteor", "babraware", "AntiBotBypass", "ServerCrasher", "neoware",
            "ddos", "dead code", "ThunderHack", "Fluger", "yestr", "FlugerClient"
    ));

    public NicknameBans(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName().toLowerCase();

        for (String badNickname : badNicknames) {
            if (playerName.contains(badNickname)) {
                executePunishment(playerName);
                break;
            }
        }
    }
    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.NicknameBansPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholders(player, punishment);

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}