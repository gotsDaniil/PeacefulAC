package work.gotsDaniil.peacefulanticheat.checks.movement;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InvMove extends PacketListenerAbstract {

    private final ConcurrentMap<Player, Location> lastLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Player, Long> lastTimes = new ConcurrentHashMap<>();
    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final String WEBHOOK_URL;
    private final int maxViolations;
    private final double threshold;
    private final long time;

    public InvMove(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.maxViolations = configManager.InvMoveMaxViolations();
        this.threshold = configManager.InvMoveThreshold();
        this.time = configManager.InvMoveTime();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {

            ItemStack chestplate = player.getInventory().getChestplate();
            boolean hasElytra = chestplate != null && chestplate.getType() == Material.ELYTRA;

            boolean isJumpingOrFlying = player.isJumping() || player.isFlying();

            if (hasElytra || isJumpingOrFlying) return;

            Location currentLocation = player.getLocation();
            long currentTime = System.currentTimeMillis();

            if (lastLocations.containsKey(player)) {
                Location lastLocation = lastLocations.get(player);
                long lastTime = lastTimes.get(player);

                double distance = calculateXZDistance(lastLocation, currentLocation);
                long timeDifference = currentTime - lastTime;

                String playerName = player.getName();
                UUID playerId = player.getUniqueId();

                if (distance >= threshold && timeDifference <= time) {

                    event.setCancelled(true);
                    Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().close());

                    violationsReset.addViolation(playerId);
                    int violations = violationsReset.getViolations(playerId);

                    AlertManager.sendAlert(configManager, player, "InventoryA", violations, maxViolations);
                    DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "InventoryA", violations, maxViolations);

                    if (violations >= maxViolations) {
                        executePunishment(playerName);
                        violationsReset.deleteViolations(playerId);
                    }
                }
            }

            lastLocations.put(player, currentLocation);
            lastTimes.put(player, currentTime);
        }
    }

    private double calculateXZDistance(Location loc1, Location loc2) {
        double deltaX = loc1.getX() - loc2.getX();
        double deltaZ = loc1.getZ() - loc2.getZ();
        return Math.hypot(deltaX, deltaZ);
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.InvMovePunishment();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "InventoryA");
            DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "InventoryA");

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}