package com.example.peacefulanticheat.Checks.combat;

import com.example.peacefulanticheat.ConfigManager;
import com.example.peacefulanticheat.api.Placeholders;
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
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AimB extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final Map<Integer, UUID> entityIdToUUID = new HashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final double minAtan2;
    private final double maxAtan2;
    private final double minAccuracy;
    private final double maxAccuracy;
    private final double minDirectionChange;
    private final double maxDirectionChange;
    private final int maxViolations;

    public AimB(ConfigManager configManager) {
        this.configManager = configManager;
        this.maxViolations = configManager.AimBMaxViolations();
        this.minAtan2 = configManager.AimBMinAtan2();
        this.maxAtan2 = configManager.AimBMaxAtan2();
        this.minAccuracy = configManager.AimBMinAccuracy();
        this.maxAccuracy = configManager.AimBMaxAccuracy();
        this.minDirectionChange = configManager.AimBMinDirectionChange();
        this.maxDirectionChange = configManager.AimBMaxDirectionChange();

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
                float pitch = player.getLocation().getPitch();
                pitch = normalizePitch(pitch);
                double pitchRadians = Math.toRadians(pitch);
                double atan2 = Math.atan2(Math.sin(pitchRadians), Math.cos(pitchRadians));

                String playerName = player.getName();
                int violations = playerViolationCount.getOrDefault(playerName, 0);

                if (atan2 >= minAtan2 && atan2 <= maxAtan2) {
                    int entityId = interactEntity.getEntityId();
                    UUID entityUUID = getEntityUUID(entityId);

                    // Проверка на null UUID
                    if (entityUUID == null) {
                        return;
                    }

                    Entity targetEntity = Bukkit.getEntity(entityUUID);

                    if (targetEntity instanceof Player) {
                        Player victim = (Player) targetEntity;
                        double accuracy = calculateAim(player, victim);

                        if (accuracy >= minAccuracy && accuracy <= maxAccuracy) {
                            double directionChange = calculateDirectionChange(player, victim);

                            if (directionChange >= minDirectionChange  && directionChange <= maxDirectionChange) {
                                violations++;
                                playerViolationCount.put(playerName, violations);
                                event.setCancelled(true);
                                player.sendMessage("Вы нарушили проверку AimB");
                                if (violations >= maxViolations) {
                                    executePunishment(playerName);
                                    playerViolationCount.remove(playerName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private UUID getEntityUUID(int entityId) {
        UUID uuid = entityIdToUUID.get(entityId);
        if (uuid == null) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getEntityId() == entityId) {
                        uuid = entity.getUniqueId();
                        entityIdToUUID.put(entityId, uuid);
                        break;
                    }
                }
            }
        }
        return uuid;
    }

    private float normalizePitch(float pitch) {
        pitch = pitch % 360;
        if (pitch < -90) pitch += 180;
        if (pitch > 90) pitch -= 180;
        return pitch;
    }

    private double calculateDirectionChange(Player attacker, Player victim) {
        Vector previousDirection = attacker.getLocation().getDirection();
        Vector currentDirection = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        return previousDirection.angle(currentDirection);
    }

    private double calculateAim(Player attacker, Player victim) {
        Vector attackerDirection = attacker.getLocation().getDirection();
        Vector toVictim = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        double dotProduct = attackerDirection.dot(toVictim);
        double angle = Math.toDegrees(Math.acos(dotProduct));
        double aim = angle / 90.0d;
        Vector attackerVelocity = attacker.getVelocity();
        double movementFactor = attackerVelocity.length() / 10.0d;
        Vector previousDirection = attacker.getLocation().getDirection();
        double directionChange = previousDirection.angle(attackerDirection);
        double directionChangeFactor = directionChange / 3.141592653589793d;
        return aim + movementFactor + directionChangeFactor;
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.AimBPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholders(player, punishment);

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}