package work.gotsDaniil.peacefulanticheat.checks.combat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

public class AutoClicker extends PacketListenerAbstract implements Listener {

    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final ConcurrentMap<UUID, Boolean> leftClickChecks = new ConcurrentHashMap<>();
    private final String WEBHOOK_URL;
    private final int maxViolations;

    public AutoClicker(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);
        this.maxViolations = configManager.AutoClickerMaxViolations();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @EventHandler
    public void isLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            leftClickChecks.put(playerUUID, true);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactPacket = new WrapperPlayClientInteractEntity(event);
            User user = event.getUser();
            UUID playerUUID = user.getUUID();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null) return;

            String playerName = player.getName();

            if (interactPacket.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {

                if (leftClickChecks.getOrDefault(playerUUID, false)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (leftClickChecks.getOrDefault(playerUUID, false)) {
                                event.setCancelled(true);

                                violationsReset.addViolation(playerUUID);
                                int violations = violationsReset.getViolations(playerUUID);

                                AlertManager.sendAlert(configManager, player, "AutoClicker", violations, maxViolations);
                                DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "AutoClicker", violations, maxViolations);

                                if (violations >= maxViolations) {
                                    executePunishment(playerName);
                                    violationsReset.deleteViolations(playerUUID);
                                }
                            }
                            leftClickChecks.remove(playerUUID);
                        }
                    }.runTaskLaterAsynchronously(plugin, 20L);
                } else {
                    leftClickChecks.remove(playerUUID);
                }
            }
        }
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AutoClickerPunishment();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AutoClicker");
                DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AutoClicker");

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}