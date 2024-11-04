package work.gotsDaniil.peacefulanticheat.utils.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertsCommand;

public class PeacefulCommand implements CommandExecutor {

    private final AlertsCommand alertsCommand;

    public PeacefulCommand(AlertManager alertManager, ConfigManager configManager) {
        this.alertsCommand = new AlertsCommand(alertManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1) {

            if (args[0].equalsIgnoreCase("alerts")) {
                return alertsCommand.onCommand(sender, command, label, args);
            }
        }

        sender.sendMessage("Использование: /peaceful <alerts>");
        return true;
    }
}
