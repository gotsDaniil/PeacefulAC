package work.gotsDaniil.peacefulanticheat.checks.movement;

import org.bukkit.command.ConsoleCommandSender;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ElytraFly extends PacketListenerAbstract {

    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final ConcurrentMap<UUID, Double> playerHeights = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Double> playerXPositions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Double> playerZPositions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> fireworkUsage = new ConcurrentHashMap<>();
    private final int maxViolations;
    private final double height;
    private final String WEBHOOK_URL;

    public ElytraFly(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.maxViolations = configManager.ElytraFlyViolations();
        this.height = configManager.ElytraFlyHeight();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        Player player = event.getPlayer();

        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();

        double currentHeight = player.getLocation().getY();
        double currentX = player.getLocation().getX();
        double currentZ = player.getLocation().getZ();

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {

            ItemStack itemHand = player.getInventory().getItemInMainHand();
            ItemStack itemOffhand = player.getInventory().getItemInOffHand();

            if (itemHand.getType() == Material.FIREWORK_ROCKET || itemOffhand.getType() == Material.FIREWORK_ROCKET) {
                fireworkUsage.put(playerId, System.currentTimeMillis());
            }
        }

        if (playerHeights.containsKey(playerId) && event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING &&
                !isUsingFirework(playerId) && event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            Double previousHeight = playerHeights.get(playerId);
            Double previousX = playerXPositions.get(playerId);
            Double previousZ = playerZPositions.get(playerId);

            String playerName = player.getName();

            if (previousHeight != null && previousX != null && previousZ != null) {

                if (Math.abs(currentX - previousX) < 0.000001 && Math.abs(currentZ - previousZ) < 0.000001) {

                    if (currentHeight - previousHeight >= 0.09 && currentHeight - previousHeight < 0.9 && !isPlayerOnSolidBlock(player)) {

                        if (isWearingElytra(player) && !player.isFlying() && !isPlayerOnLadderOrVine(player)) {

                            setBack(player);

                            violationsReset.addViolation(playerId);
                            int violations = violationsReset.getViolations(playerId);

                            AlertManager.sendAlert(configManager, player, "ElytraFly", violations, maxViolations);
                            DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "ElytraFly", violations, maxViolations);

                            if (violations >= maxViolations) {
                                executePunishment(playerName);
                                violationsReset.deleteViolations(playerId);
                            }
                        }
                    }
                }
            }
        }

        playerHeights.put(playerId, currentHeight);
        playerXPositions.put(playerId, currentX);
        playerZPositions.put(playerId, currentZ);
    }

    private boolean isWearingElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }

    private boolean isPlayerOnSolidBlock(Player player) {
        Location playerLocation = player.getLocation();

        for (int i = 1; i <= height; i++) {

            Block blockUnderPlayer = playerLocation.clone().subtract(0.3, i-1, 0.3).getBlock();
            Block blockUnderPlayer1 = playerLocation.clone().subtract(-0.3, i-1, -0.3).getBlock();
            Block blockUnderPlayer2 = playerLocation.clone().subtract(0.3, i-1, -0.3).getBlock();
            Block blockUnderPlayer3 = playerLocation.clone().subtract(-0.3, i-1, 0.3).getBlock();

            Material blockType = blockUnderPlayer.getType();
            Material blockType1 = blockUnderPlayer1.getType();
            Material blockType2 = blockUnderPlayer2.getType();
            Material blockType3 = blockUnderPlayer3.getType();

            if (blockType.isSolid() || blockType1.isSolid() || blockType2.isSolid() || blockType3.isSolid()
                    || blockType == Material.WATER || blockType1 == Material.WATER || blockType2 == Material.WATER || blockType3 == Material.WATER
                    || blockType == Material.LAVA || blockType1 == Material.LAVA || blockType2 == Material.LAVA || blockType3 == Material.LAVA) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerOnLadderOrVine(Player player) {

        Location playerLocation = player.getLocation();
        Block blockUnderPlayer = playerLocation.clone().subtract(0, 1, 0).getBlock();
        Material blockType = blockUnderPlayer.getType();

        return blockType == Material.LADDER || blockType == Material.VINE;
    }

    private boolean isUsingFirework(UUID playerId) {
        Long lastUsage = fireworkUsage.get(playerId);
        return lastUsage != null && System.currentTimeMillis() - lastUsage < 3500;
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.ElytraFlyPunishment();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "ElytraFly");
            DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "ElytraFly");

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }

    private void setBack(Player player) {
        Location playerLocation = player.getLocation();

        playerLocation.setY(playerLocation.getY() - 2);

        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(playerLocation));
    }
}