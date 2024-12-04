package work.gotsDaniil.peacefulanticheat.checks.combat;

import work.gotsDaniil.peacefulanticheat.api.*;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;

import java.util.UUID;
import java.util.function.Consumer;

public class AimB extends PacketListenerAbstract {

    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final double minAtan2;
    private final double maxAtan2;
    private final double minAccuracy;
    private final double maxAccuracy;
    private final double minDirectionChange;
    private final double maxDirectionChange;
    private final int maxViolations;
    private final String WEBHOOK_URL;

    public AimB(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
        this.maxViolations = configManager.AimBMaxViolations();
        this.minAtan2 = configManager.AimBMinAtan2();
        this.maxAtan2 = configManager.AimBMaxAtan2();
        this.minAccuracy = configManager.AimBMinAccuracy();
        this.maxAccuracy = configManager.AimBMaxAccuracy();
        this.minDirectionChange = configManager.AimBMinDirectionChange();
        this.maxDirectionChange = configManager.AimBMaxDirectionChange();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

            WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);

            User user = event.getUser();
            UUID playerUUID = user.getUUID();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player != null) {

                double atan2 = PlayerRotationCalculations.calculateAtan2(player);

                String playerName = player.getName();

                if (atan2 >= minAtan2 && atan2 <= maxAtan2) {
                    int entityId = interactEntity.getEntityId();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        getEntityUUID(entityId, entityUUID -> {
                            if (entityUUID == null) {
                                return;
                            }

                            Entity targetEntity = Bukkit.getEntity(entityUUID);
                            if (targetEntity instanceof Player) {
                                Player victim = (Player) targetEntity;

                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    double accuracy = PlayerRotationCalculations.calculateAim(player, victim);
                                    double directionChange = PlayerRotationCalculations.calculateDirectionChange(player, victim);

                                    if (accuracy >= minAccuracy && accuracy <= maxAccuracy) {
                                        if (directionChange >= minDirectionChange && directionChange <= maxDirectionChange) {

                                            event.setCancelled(true);

                                            violationsReset.addViolation(playerUUID);
                                            int violations = violationsReset.getViolations(playerUUID);

                                            AlertManager.sendAlert(configManager, player, "AimB", violations, maxViolations);
                                            DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "AimB", violations, maxViolations);

                                            if (violations >= maxViolations) {
                                                executePunishment(playerName);
                                                violationsReset.deleteViolations(playerUUID);
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    });
                }
            }
        }
    }

    private void getEntityUUID(int entityId, Consumer<UUID> callback) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getEntityId() == entityId) {
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        UUID playerUUID = player.getUniqueId();
                        callback.accept(playerUUID);
                    } else {
                        callback.accept(null);
                    }
                    return;
                }
            }
        }
        callback.accept(null);
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AimBPunishment();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AimB");
                DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AimB");

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}