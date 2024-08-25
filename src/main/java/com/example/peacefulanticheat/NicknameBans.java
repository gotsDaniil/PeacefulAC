package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Arrays;
import java.util.List;

public class NicknameBans implements Listener {
    // Баним игроков за ники, если в них содержится одно из словосочетаний
    private ConfigManager configManager;

    public NicknameBans(ConfigManager configManager) {
        this.configManager = configManager;
    }
    List<String> badNicknames = Arrays.asList("nurik", "nursultan", "arbuz_client", "wild", "expensive", "delta", "celka", "celestial", "ExpRandom", "bebraproxy", "Meteor", "babraware", "AntiBotBypass", "ServerCrasher");


    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName().toLowerCase();

        for (String badNickname : badNicknames) {
            if (playerName.contains(badNickname)) {
                String punishment6 = configManager.getPunishment6();
                String message6 = configManager.getMessage6();
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment6 + " " + playerName + " " + message6);
                break;
            }
        }
    }
}