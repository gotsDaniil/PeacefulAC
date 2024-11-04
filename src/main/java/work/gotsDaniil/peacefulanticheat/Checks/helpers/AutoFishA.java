package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import com.github.retrooper.packetevents.protocol.player.User;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.FishHook;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AutoFishA extends PacketListenerAbstract {

    private final PeacefulAntiCheat plugin;
    private final DiscordWebhook DiscordWebhook;
    private final ConfigManager configManager;
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final int maxViolations;
    private final int deviation;
    private final String WEBHOOK_URL;

    public AutoFishA(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.maxViolations = configManager.AutoFishAViolations();
        this.deviation = configManager.AutoFishADeviation();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactEntityPacket = new WrapperPlayClientInteractEntity(event);
            User user = event.getUser();
            UUID playerUUID = user.getUUID();

            int entityId = interactEntityPacket.getEntityId();

            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null) return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                Entity targetEntity = null;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity.getEntityId() == entityId) {
                        targetEntity = entity;
                        break;
                    }
                }

                if (targetEntity instanceof FishHook) {
                    String playerName = player.getName();

                    // Проверка, есть ли рыба в инвентаре игрока
                    boolean hasFish = player.getInventory().contains(Material.COD)
                            || player.getInventory().contains(Material.SALMON)
                            || player.getInventory().contains(Material.TROPICAL_FISH)
                            || player.getInventory().contains(Material.PUFFERFISH);

                    if (!hasFish) {
                        int violations = playerViolationCount.getOrDefault(playerName, 0);

                        Location fishLocation = targetEntity.getLocation();
                        Location playerLocation = player.getLocation();

                        double distanceMoved = playerLocation.distance(fishLocation);
                        if (distanceMoved <= deviation) {

                            violations++;
                            playerViolationCount.put(playerName, violations);

                            AlertManager.sendAlert(configManager, player, "AutoFishA", violations, maxViolations);
                            DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "AutoFishA", violations, maxViolations);

                            if (violations >= maxViolations) {
                                executePunishment(playerName);
                                playerViolationCount.remove(playerName);
                            }
                        } else {
                            playerViolationCount.put(playerName, 0);
                        }
                    }
                }
            });
        }
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AutoFishPunishmentA();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "AutoFishA");
            DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AutoFishA");

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}