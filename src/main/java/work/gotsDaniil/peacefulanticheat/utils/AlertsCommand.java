package work.gotsDaniil.peacefulanticheat.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AlertsCommand implements CommandExecutor {

    private final AlertManager alertManager;

    public AlertsCommand(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }

        Player player = (Player) sender;
        alertManager.toggleAlerts(player);
        return true;
    }
}