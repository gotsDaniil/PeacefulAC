package com.example.peacefulanticheat.Checks.movement;

import com.example.peacefulanticheat.ConfigManager;
import com.example.peacefulanticheat.api.Placeholders;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DragonFlyA extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final ConcurrentMap<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final int maxViolations;
    private final int Speed_1_zx;
    private final int Speed_2_zx;
    private final int Speed_3_zx;
    private final int Speed_4_zx;
    private final int Speed_5_zx;
    private final int Speed_6_zx;
    private final int Speed_7_zx;
    private final int Speed_8_zx;
    private final int Speed_9_zx;
    private final int Speed_10_zx;
    private final int Speed_1_y;
    private final int Speed_2_y;
    private final int Speed_3_y;
    private final int Speed_4_y;
    private final int Speed_5_y;
    private final int Speed_6_y;
    private final int Speed_7_y;
    private final int Speed_8_y;
    private final int Speed_9_y;
    private final int Speed_10_y;

    public DragonFlyA(ConfigManager configManager) {
        this.configManager = configManager;
        this.maxViolations = configManager.DragonFlyViolations();
        this.Speed_1_zx = configManager.DragonFlySpeed_1_zx();
        this.Speed_2_zx = configManager.DragonFlySpeed_2_zx();
        this.Speed_3_zx = configManager.DragonFlySpeed_3_zx();
        this.Speed_4_zx = configManager.DragonFlySpeed_4_zx();
        this.Speed_5_zx = configManager.DragonFlySpeed_5_zx();
        this.Speed_6_zx = configManager.DragonFlySpeed_6_zx();
        this.Speed_7_zx = configManager.DragonFlySpeed_7_zx();
        this.Speed_8_zx = configManager.DragonFlySpeed_8_zx();
        this.Speed_9_zx = configManager.DragonFlySpeed_9_zx();
        this.Speed_10_zx = configManager.DragonFlySpeed_10_zx();
        this.Speed_1_y = configManager.DragonFlySpeed_1_y();
        this.Speed_2_y = configManager.DragonFlySpeed_2_y();
        this.Speed_3_y = configManager.DragonFlySpeed_3_y();
        this.Speed_4_y = configManager.DragonFlySpeed_4_y();
        this.Speed_5_y = configManager.DragonFlySpeed_5_y();
        this.Speed_6_y = configManager.DragonFlySpeed_6_y();
        this.Speed_7_y = configManager.DragonFlySpeed_7_y();
        this.Speed_8_y = configManager.DragonFlySpeed_8_y();
        this.Speed_9_y = configManager.DragonFlySpeed_9_y();
        this.Speed_10_y = configManager.DragonFlySpeed_10_y();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
            Player player = event.getPlayer();
            if (player == null) return;

            float flySpeed = player.getFlySpeed();

                // Магические вычисления скорости игрока для каждого спида
                switch ((int)(flySpeed * 10)) {
                    case 1:
                        handleMovementCheck(event, player, Speed_1_zx, Speed_1_y);
                        break;
                    case 2:
                        handleMovementCheck(event, player, Speed_2_zx, Speed_2_y);
                        break;
                    case 3:
                        handleMovementCheck(event, player, Speed_3_zx, Speed_3_y);
                        break;
                    case 4:
                        handleMovementCheck(event, player, Speed_4_zx, Speed_4_y);
                        break;
                    case 5:
                        handleMovementCheck(event, player, Speed_5_zx, Speed_5_y);
                        break;
                    case 6:
                        handleMovementCheck(event, player, Speed_6_zx, Speed_6_y);
                        break;
                    case 7:
                        handleMovementCheck(event, player, Speed_7_zx, Speed_7_y);
                        break;
                    case 8:
                        handleMovementCheck(event, player, Speed_8_zx, Speed_8_y);
                        break;
                    case 9:
                        handleMovementCheck(event, player, Speed_9_zx, Speed_9_y);
                        break;
                    case 10:
                        handleMovementCheck(event, player, Speed_10_zx, Speed_10_y);
                        break;
                }
        }
    }

    private void handleMovementCheck(PacketReceiveEvent event, Player player, double xzThreshold, double yThreshold) {
        WrapperPlayClientPlayerFlying flyingPacket = new WrapperPlayClientPlayerFlying(event);
        Location currentLocation = flyingPacket.getLocation();
        Location lastLocation = lastLocations.get(player.getUniqueId());

        if (lastLocation != null) {
            double deltaX = currentLocation.getX() - lastLocation.getX();
            double deltaZ = currentLocation.getZ() - lastLocation.getZ();
            double deltaXZ = deltaX * deltaX + deltaZ * deltaZ;
            double speedXZ = Math.sqrt(deltaXZ) * 20; // Умножаем на 20 для перевода в секунды

            double deltaY = currentLocation.getY() - lastLocation.getY();
            double speedY = Math.abs(deltaY) * 20; // Мы просто берем абсолютное значение изменения Y и умножаем

            String playerName = player.getName();
            int violations = playerViolationCount.getOrDefault(playerName, 0);


            // Отменяет ивент, если игрок быстро движется по X, Z или Y
            if (speedXZ >= xzThreshold || speedY >= yThreshold) {
                violations++;
                playerViolationCount.put(playerName, violations);
                event.setCancelled(true);
                if (violations >= maxViolations) {
                    executePunishment(playerName);
                    playerViolationCount.remove(playerName);
                }
            }
        }

        lastLocations.put(player.getUniqueId(), currentLocation);
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.DragonFlyPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholders(player, punishment);

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}