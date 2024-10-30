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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class MLAura extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final ConcurrentMap<Integer, UUID> entityIdToUUID = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Location> playerLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Vector> playerDirections = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastRotationTime = new ConcurrentHashMap<>(); // Время последнего изменения ротации
    private final ConcurrentMap<UUID, Double> lastYaw = new ConcurrentHashMap<>(); // Последний Yaw игрока
    private final ConcurrentMap<UUID, Double> lastPitch = new ConcurrentHashMap<>(); // Последний Pitch игрока
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final boolean AdditionalChecks;
    private final double minAtan2;
    private final double maxAtan2;
    private final double SpeedPlayer;
    private final double minAccuracy;
    private final double maxAccuracy;
    private final double minRotationSinCos;
    private final double maxRotationSinCos;
    private final double minDirectionChange;
    private final double maxDirectionChange;
    private final double minDirectionChangeSpeed;
    private final double maxDirectionChangeSpeed;
    private final double minRotationSpeed;
    private final double maxRotationSpeed;
    private final double minSnapAim;
    private final double maxSnapAim;
    private final double minSilentDeviation;
    private final double maxSilentDeviation;
    private final double minAngleDelta;
    private final double maxAngleDelta;
    private final int maxViolations;
    private int violations;

    public MLAura(ConfigManager configManager) {
        this.configManager = configManager;
        this.AdditionalChecks = configManager.MLAuraAdditionalChecks();
        this.minAtan2 = configManager.MLAuraMinAtan2();
        this.maxAtan2 = configManager.MLAuraMaxAtan2();
        this.SpeedPlayer = configManager.MLAuraSpeedPlayer();
        this.minAccuracy = configManager.MLAuraMinAccuracy();
        this.maxAccuracy = configManager.MLAuraMaxAccuracy();
        this.minRotationSinCos = configManager.MLAuraMinRotationSinCos();
        this.maxRotationSinCos = configManager.MLAuraMaxRotationSinCos();
        this.minDirectionChange = configManager.MLAuraMinDirectionChange();
        this.maxDirectionChange = configManager.MLAuraMaxDirectionChange();
        this.minDirectionChangeSpeed = configManager.MLAuraMinDirectionChangeSpeed();
        this.maxDirectionChangeSpeed = configManager.MLAuraMaxDirectionChangeSpeed();
        this.minRotationSpeed = configManager.MLAuraMinRotationSpeed();
        this.maxRotationSpeed = configManager.MLAuraMaxRotationSpeed();
        this.minSnapAim = configManager.MLAuraMinSnapAim();
        this.maxSnapAim = configManager.MLAuraMaxSnapAim();
        this.minSilentDeviation = configManager.MLAuraMinSilentDeviation();
        this.maxSilentDeviation = configManager.MLAuraMaxSilentDeviation();
        this.minAngleDelta = configManager.MLAuraMinAngleDelta();
        this.maxAngleDelta = configManager.MLAuraMaxAngleDelta();
        this.maxViolations = configManager.MLAuraMaxViolations();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);
            User user = event.getUser();
            UUID playerUUID = user.getUUID();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null) return;

            Location playerLocation = player.getLocation();
            float pitch = playerLocation.getPitch();
            pitch = normalizePitch(pitch);
            double pitchRadians = Math.toRadians(pitch);
            // Вычисление арктангенса игрока
            double atan2 = Math.atan2(Math.sin(pitchRadians), Math.cos(pitchRadians));

            int entityId = interactEntity.getEntityId();

            getEntityUUID(entityId, entityUUID -> {
                if (entityUUID == null) return;

                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
                    Entity targetEntity = Bukkit.getEntity(entityUUID);

                    if (targetEntity == null) return;

                    if (targetEntity instanceof Player) {
                        Player victim = (Player) targetEntity;

                        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
                            double accuracy = calculateAim(player, victim);
                            Vector attackerDirection = playerLocation.getDirection();

                            // Вычисление ротации на синусах и косинусах
                            double rotationSinCos = calculateRotationSinCos(playerLocation, pitchRadians);

                            // Вычисление скорости ротации
                            double directionChange = calculateDirectionChange(player, victim);

                            // Изменение направления
                            Vector previousDirection = playerDirections.get(playerUUID);
                            double directionChangeSpeed = 0;
                            if (previousDirection != null) {
                                directionChangeSpeed = previousDirection.angle(attackerDirection);
                            }
                            playerDirections.put(playerUUID, attackerDirection);

                            // Скорость ротации с вычислением Yaw и Pitch
                            double rotationSpeed = calculateRotationSpeed(player, playerUUID);

                            // Проверка на резкие изменения углов (Снапы)
                            double snapAim = calculateSnapAim(player, playerUUID);

                            // Проверка ротации на соответствие с движением (Silent коррекция)
                            double SilentCorrection = calculateMovementAimConsistency(player, playerUUID);

                            // Проверка на изменение угла между Yaw и Pitch
                            double angleDelta = calculateAngleDelta(player, playerUUID);

                            // Запись данных о передвижении игрока
                            Location previousLocation = playerLocations.get(playerUUID);
                            if (previousLocation != null) {
                                double distance = playerLocation.distance(previousLocation);
                                double timeDifference = (System.currentTimeMillis() - previousLocation.getWorld().getFullTime()) / 1000.0; // Время в секундах
                                double speed = distance / timeDifference;
                                String playerName = player.getName();
                                int violations = playerViolationCount.getOrDefault(playerName, 0);

                                if (atan2 >= minAtan2 && atan2 <= maxAtan2 && speed >= SpeedPlayer) {
                                    if (accuracy >= minAccuracy && accuracy <= maxAccuracy
                                            && rotationSinCos >= minRotationSinCos && rotationSinCos <= maxRotationSinCos
                                            && directionChange >= minDirectionChange && directionChange <= maxDirectionChange
                                            && directionChangeSpeed >= minDirectionChangeSpeed && directionChangeSpeed <= maxDirectionChangeSpeed
                                            && rotationSpeed >= minRotationSpeed && rotationSpeed <= maxRotationSpeed) {
                                        if (!AdditionalChecks) {
                                            violations++;
                                            playerViolationCount.put(playerName, violations);
                                            event.setCancelled(true);
                                            AlertManager.sendAlert(configManager, player, "AimA", violations, maxViolations);
                                            if (violations >= maxViolations) {
                                                executePunishment(playerName);
                                                playerViolationCount.remove(playerName);
                                            }
                                        } else if (snapAim >= minSnapAim && snapAim <= maxSnapAim
                                                && SilentCorrection >= minSilentDeviation && SilentCorrection <= maxSilentDeviation
                                                && angleDelta >= minAngleDelta && angleDelta <= maxAngleDelta) {
                                            violations++;
                                            playerViolationCount.put(playerName, violations);
                                            event.setCancelled(true);
                                            AlertManager.sendAlert(configManager, player, "AimA", violations, maxViolations);
                                            if (violations >= maxViolations) {
                                                executePunishment(playerName);
                                                playerViolationCount.remove(playerName);
                                            }
                                        }
                                    }
                                }
                            }

                            // Обновляем позицию игрока
                            playerLocations.put(playerUUID, playerLocation);
                        });
                    }
                });
            });
        }
    }

    private void getEntityUUID(int entityId, Consumer<UUID> callback) {
        UUID uuid = entityIdToUUID.get(entityId);
        if (uuid == null) {
            class UUIDWrapper {
                UUID uuid;
            }
            UUIDWrapper wrapper = new UUIDWrapper();

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity.getEntityId() == entityId) {
                            wrapper.uuid = entity.getUniqueId();
                            entityIdToUUID.put(entityId, wrapper.uuid);
                            break;
                        }
                    }
                }
                callback.accept(wrapper.uuid);
            });
        } else {
            callback.accept(uuid);
        }
    }

    private float normalizePitch(float pitch) {
        pitch = pitch % 360;
        if (pitch < -90) pitch += 180;
        if (pitch > 90) pitch -= 180;
        return pitch;
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

    private double calculateDirectionChange(Player attacker, Player victim) {
        Vector previousDirection = attacker.getLocation().getDirection();
        Vector currentDirection = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        return previousDirection.angle(currentDirection);
    }

    private double calculateRotationSinCos(Location playerLocation, double pitchRadians) {
        double yawRadians = Math.toRadians(playerLocation.getYaw());
        double sinYaw = Math.sin(yawRadians);
        double cosYaw = Math.cos(yawRadians);
        double sinPitch = Math.sin(pitchRadians);
        double cosPitch = Math.cos(pitchRadians);
        return Math.abs(sinYaw * cosPitch - cosYaw * sinPitch);
    }

    private double calculateRotationSpeed(Player player, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRotationTime.get(playerUUID);
        if (lastTime == null) {
            lastRotationTime.put(playerUUID, currentTime);
            lastYaw.put(playerUUID, (double) player.getLocation().getYaw());
            lastPitch.put(playerUUID, (double) player.getLocation().getPitch());
            return 0;
        }

        long timeDifference = currentTime - lastTime;
        double currentYaw = player.getLocation().getYaw();
        double currentPitch = player.getLocation().getPitch();
        double lastYawValue = lastYaw.get(playerUUID);
        double lastPitchValue = lastPitch.get(playerUUID);

        double yawChange = Math.abs(currentYaw - lastYawValue);
        double pitchChange = Math.abs(currentPitch - lastPitchValue);

        // Нормализация изменений углов
        yawChange = Math.toRadians(yawChange);
        pitchChange = Math.toRadians(pitchChange);

        // Вычисление скорости ротации с учетом изменений Yaw и Pitch
        double rotationSpeed = Math.sqrt(Math.pow(yawChange, 2) + Math.pow(pitchChange, 2)) / (timeDifference / 1000.0);

        lastRotationTime.put(playerUUID, currentTime);
        lastYaw.put(playerUUID, currentYaw);
        lastPitch.put(playerUUID, currentPitch);

        return rotationSpeed;
    }

    private double calculateSnapAim(Player player, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRotationTime.get(playerUUID);
        if (lastTime == null) {
            lastRotationTime.put(playerUUID, currentTime);
            lastYaw.put(playerUUID, (double) player.getLocation().getYaw());
            lastPitch.put(playerUUID, (double) player.getLocation().getPitch());
            return 0;
        }

        long timeDifference = currentTime - lastTime;
        double currentYaw = player.getLocation().getYaw();
        double currentPitch = player.getLocation().getPitch();
        double lastYawValue = lastYaw.get(playerUUID);
        double lastPitchValue = lastPitch.get(playerUUID);

        double yawChange = Math.abs(currentYaw - lastYawValue);
        double pitchChange = Math.abs(currentPitch - lastPitchValue);

        // Нормализация изменений углов
        yawChange = Math.toRadians(yawChange);
        pitchChange = Math.toRadians(pitchChange);

        // Вычисление резкости изменения углов
        double snapAim = Math.sqrt(Math.pow(yawChange, 2) + Math.pow(pitchChange, 2)) / (timeDifference / 1000.0);

        lastRotationTime.put(playerUUID, currentTime);
        lastYaw.put(playerUUID, currentYaw);
        lastPitch.put(playerUUID, currentPitch);

        return snapAim;
    }

    private double calculateMovementAimConsistency(Player player, UUID playerUUID) {
        Location previousLocation = playerLocations.get(playerUUID);
        if (previousLocation == null) {
            return 1;
        }

        Vector currentDirection = player.getLocation().getDirection();
        Vector previousDirection = previousLocation.getDirection();

        double directionChange = currentDirection.angle(previousDirection);

        // Вычисление соответствия ротации c движением
        double movementAimConsistency = 1 - (directionChange / Math.PI);

        return movementAimConsistency;
    }

    private double calculateAngleDelta(Player player, UUID playerUUID) {
        double currentYaw = player.getLocation().getYaw();
        double currentPitch = player.getLocation().getPitch();
        double lastYawValue = lastYaw.get(playerUUID);
        double lastPitchValue = lastPitch.get(playerUUID);

        double yawChange = Math.abs(currentYaw - lastYawValue);
        double pitchChange = Math.abs(currentPitch - lastPitchValue);

        // Нормализация изменений углов
        yawChange = Math.toRadians(yawChange);
        pitchChange = Math.toRadians(pitchChange);

        // Вычисление изменения угла между Yaw и Pitch
        double angleDelta = Math.abs(yawChange - pitchChange);

        return angleDelta;
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.MLAuraPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "AimA");
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}