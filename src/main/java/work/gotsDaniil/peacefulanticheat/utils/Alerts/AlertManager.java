package work.gotsDaniil.peacefulanticheat.utils.Alerts;

import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AlertManager {

    private static final ConcurrentMap<UUID, Boolean> alertsEnabled = new ConcurrentHashMap<>();
    private final ConfigManager configManager;

    public AlertManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void toggleAlerts(Player player) {

        UUID playerId = player.getUniqueId();

        boolean currentState = alertsEnabled.getOrDefault(playerId, false);
        alertsEnabled.put(playerId, !currentState);

        String enableAlerts = ChatColor.translateAlternateColorCodes('&', configManager.EnableAlerts());
        String disableAlerts = ChatColor.translateAlternateColorCodes('&', configManager.DisableAlerts());

        player.sendMessage(currentState ? disableAlerts : enableAlerts);
    }

    public static void sendAlert(ConfigManager configManager, Player player, String checkType, int violations, int maxViolations) {

        String message = configManager.AlertMessage();

        message = ChatColor.translateAlternateColorCodes('&', message);
        message = Placeholders.replacePlaceholdersAlerts(player, message, checkType, violations, maxViolations);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            if (alertsEnabled.getOrDefault(onlinePlayer.getUniqueId(), false)) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    public static void sendAlertPunishment(ConfigManager configManager, Player player, String checkType) {

        String message = configManager.AlertPunishMessage();

        message = ChatColor.translateAlternateColorCodes('&', message);
        message = Placeholders.replacePlaceholdersAlertsPunish(player, message, checkType);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            if (alertsEnabled.getOrDefault(onlinePlayer.getUniqueId(), false)) {
                onlinePlayer.sendMessage(message);
            }
        }
    }
}