package com.example.peacefulanticheat.Checks.movement;

import com.example.peacefulanticheat.ConfigManager;
import com.example.peacefulanticheat.api.Placeholders;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.bukkit.GameMode.CREATIVE;

public class ElytraFly extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final Map<UUID, Double> playerHeights = new HashMap<>();
    private final Map<UUID, Double> playerXPositions = new HashMap<>();
    private final Map<UUID, Double> playerZPositions = new HashMap<>();
    private final Map<UUID, Long> fireworkUsage = new HashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final int maxViolations;
    private final int height;

    public ElytraFly(ConfigManager configManager) {
        this.configManager = configManager;
        this.maxViolations = configManager.ElytraFlyViolations();
        this.height = configManager.ElytraFlyHeight();
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
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.FIREWORK_ROCKET) {
                fireworkUsage.put(playerId, System.currentTimeMillis());
            }
        }

        if (playerHeights.containsKey(playerId) && event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING &&
                !isUsingFirework(playerId) && player.getGameMode() != CREATIVE && event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            Double previousHeight = playerHeights.get(playerId);
            Double previousX = playerXPositions.get(playerId);
            Double previousZ = playerZPositions.get(playerId);

            String playerName = player.getName();
            int violations = playerViolationCount.getOrDefault(playerName, 0);

            if (previousHeight != null && previousX != null && previousZ != null) {
                if (Math.abs(currentX - previousX) < 0.000001 && Math.abs(currentZ - previousZ) < 0.000001) {
                    if (currentHeight - previousHeight >= 0.09 && currentHeight - previousHeight < 0.9 && !isPlayerOnSolidBlock(player)) {
                        if (isWearingElytra(player) && !player.isFlying()) {
                            violations++;
                            playerViolationCount.put(playerName, violations);
                            setBack(player);
                            if (violations >= maxViolations) {
                                executePunishment(playerName);
                                playerViolationCount.remove(playerName);
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
        for (int i = 1; i <= 3; i++) {
            Block blockUnderPlayer = playerLocation.clone().subtract(0, i, 0).getBlock();
            Material blockType = blockUnderPlayer.getType();
            if (blockType.isSolid() || blockType == Material.WATER || blockType == Material.LAVA) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsingFirework(UUID playerId) {
        Long lastUsage = fireworkUsage.get(playerId);
        return lastUsage != null && System.currentTimeMillis() - lastUsage < 3500;
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.ElytraFlyPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholders(player, punishment);

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }

    private void setBack(Player player) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            Location playerLocation = player.getLocation();
            playerLocation.setY(playerLocation.getY() - 2 );
            player.teleport(playerLocation);
        });
    }
}