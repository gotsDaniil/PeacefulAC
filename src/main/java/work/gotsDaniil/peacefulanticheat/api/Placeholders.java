package work.gotsDaniil.peacefulanticheat.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class Placeholders implements Listener {

    public static String replacePlaceholderPlayer(Player player, String message) {
        if (player == null) {
            return message;
        }

        message = message.replace("%player%", player.getName());

        return message;
    }

    public static String replacePlaceholdersAlerts(Player player, String message, String checkType, int violations, int maxViolations) {
        if (player == null) {
            return message;
        }

        message = message.replace("%player%", player.getName());
        message = message.replace("%checkType%", checkType);
        message = message.replace("%violations%", String.valueOf(violations));
        message = message.replace("%maxViolations%", String.valueOf(maxViolations));

        return message;
    }

    public static String replacePlaceholdersAlertsPunish(Player player, String message, String checkType) {
        if (player == null) {
            return message;
        }

        message = message.replace("%player%", player.getName());
        message = message.replace("%checkType%", checkType);

        return message;
    }
}