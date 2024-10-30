package work.gotsDaniil.peacefulanticheat.Checks.combat;

import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.AlertManager;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AimB extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final ConcurrentMap<Integer, UUID> entityIdToUUID = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final double minAtan2;
    private final double maxAtan2;
    private final double minAccuracy;
    private final double maxAccuracy;
    private final double minDirectionChange;
    private final double maxDirectionChange;
    private final int maxViolations;
    private final Plugin plugin;
    private int newViolations;

    public AimB(ConfigManager configManager, Plugin plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
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

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        getEntityUUID(entityId, entityUUID -> {
                            if (entityUUID == null) {
                                return;
                            }

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Entity targetEntity = Bukkit.getEntity(entityUUID);

                                if (targetEntity instanceof Player) {
                                    Player victim = (Player) targetEntity;

                                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                        double accuracy = calculateAim(player, victim);
                                        double directionChange = calculateDirectionChange(player, victim);

                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (accuracy >= minAccuracy && accuracy <= maxAccuracy) {
                                                if (directionChange >= minDirectionChange && directionChange <= maxDirectionChange) {
                                                    newViolations = violations + 1;
                                                    playerViolationCount.put(playerName, newViolations);
                                                    event.setCancelled(true);
                                                    AlertManager.sendAlert(configManager, player, "AimB", newViolations, maxViolations);
                                                    if (newViolations >= maxViolations) {
                                                        executePunishment(playerName);
                                                        playerViolationCount.remove(playerName);
                                                    }
                                                }
                                            }
                                        });
                                    });
                                }
                            });
                        });
                    });
                }
            }
        }
    }

    private void getEntityUUID(int entityId, Consumer<UUID> callback) {
        AtomicReference<UUID> uuidRef = new AtomicReference<>(entityIdToUUID.get(entityId));
        if (uuidRef.get() == null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity.getEntityId() == entityId) {
                            uuidRef.set(entity.getUniqueId());
                            entityIdToUUID.put(entityId, uuidRef.get());
                            break;
                        }
                    }
                }
                callback.accept(uuidRef.get());
            });
        } else {
            callback.accept(uuidRef.get());
        }
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AimBPunishment();
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AimB");
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}