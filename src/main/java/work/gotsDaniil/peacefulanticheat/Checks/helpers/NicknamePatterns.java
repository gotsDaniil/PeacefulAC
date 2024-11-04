package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NicknamePatterns implements Listener {

    private final PeacefulAntiCheat plugin;
    private final DiscordWebhook DiscordWebhook;
    private final ConfigManager configManager;
    private final String WEBHOOK_URL;
    private final Set<String> badNicknames = new HashSet<>(Arrays.asList(
            "nurik", "nursultan", "arbuz_client", "wild", "expensive", "delta", "celka", "celestial",
            "ExpRandom", "bebraproxy", "Meteor", "babraware", "AntiBotBypass", "ServerCrasher", "neoware",
            "ddos", "dead code", "ThunderHack", "Fluger", "yestr", "FlugerClient"
    ));

    public NicknamePatterns(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.NicknamePatternsAPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "NicknamePatterns");
            DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "NicknamePatterns");

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}