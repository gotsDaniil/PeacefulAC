package work.gotsDaniil.peacefulanticheat.Checks.combat;

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

    private final PeacefulAntiCheat plugin;
    private final DiscordWebhook DiscordWebhook;
    private final ConfigManager configManager;
    private final ConcurrentMap<UUID, Location> playerLocations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Vector> playerDirections = new ConcurrentHashMap<>();
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
    private final boolean debug;
    private final String WEBHOOK_URL;

    public MLAura(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
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
        this.debug = configManager.MLAuraDebug();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);

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
            double atan2 = PlayerRotationCalculations.calculateAtan2(player);

            int entityId = interactEntity.getEntityId();

            // Перенос вызова getEntityUUID в основной поток
            Bukkit.getScheduler().runTask(plugin, () -> {

                getEntityUUID(entityId, entityUUID -> {

                    if (entityUUID == null) return;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Entity targetEntity = Bukkit.getEntity(entityUUID);

                        if (targetEntity == null) return;

                        if (targetEntity instanceof Player) {
                            Player victim = (Player) targetEntity;

                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                                // Запись данных о передвижении игрока
                                Location previousLocation = playerLocations.get(playerUUID);

                                if (previousLocation != null) {

                                    double distance = playerLocation.distance(previousLocation);
                                    double timeDifference = (System.currentTimeMillis() - previousLocation.getWorld().getFullTime()) / 1000.0; // Время в секундах
                                    double speed = distance / timeDifference;

                                    int violations = playerViolationCount.getOrDefault(player.getName(), 0);

                                    if (atan2 >= minAtan2 && atan2 <= maxAtan2 && speed >= SpeedPlayer) {

                                        double accuracy = PlayerRotationCalculations.calculateAim(player, victim);
                                        Vector attackerDirection = playerLocation.getDirection();

                                        // Вычисление ротации на синусах и косинусах
                                        double rotationSinCos = PlayerRotationCalculations.calculateRotationSinCos(playerLocation, pitchRadians);

                                        // Вычисление скорости ротации
                                        double directionChange = PlayerRotationCalculations.calculateDirectionChange(player, victim);

                                        // Изменение направления
                                        Vector previousDirection = playerDirections.get(playerUUID);

                                        double directionChangeSpeed = 0;

                                        if (previousDirection != null) {
                                            directionChangeSpeed = previousDirection.angle(attackerDirection);
                                        }

                                        playerDirections.put(playerUUID, attackerDirection);

                                        // Скорость ротации с вычислением Yaw и Pitch
                                        double rotationSpeed = PlayerRotationCalculations.calculateRotationSpeed(player, playerUUID);

                                        if (accuracy >= minAccuracy && accuracy <= maxAccuracy
                                            && rotationSinCos >= minRotationSinCos && rotationSinCos <= maxRotationSinCos
                                            && directionChange >= minDirectionChange && directionChange <= maxDirectionChange
                                            && directionChangeSpeed >= minDirectionChangeSpeed && directionChangeSpeed <= maxDirectionChangeSpeed
                                            && rotationSpeed >= minRotationSpeed && rotationSpeed <= maxRotationSpeed) {

                                            if (!AdditionalChecks) {

                                                event.setCancelled(true);

                                                violations++;
                                                playerViolationCount.put(player.getName(), violations);

                                                AlertManager.sendAlert(configManager, player, "AimA", violations, maxViolations);
                                                DiscordWebhook.sendAlert(WEBHOOK_URL, player.getName(), "AimA", violations, maxViolations);

                                                if (violations >= maxViolations) {
                                                    executePunishment(player.getName());
                                                    playerViolationCount.remove(player.getName());
                                                }

                                                if (debug) {

                                                    // Проверка на резкие изменения углов (Снапы)
                                                    double snapAim = PlayerRotationCalculations.calculateSnapAim(player, playerUUID);

                                                    // Проверка ротации на соответствие с движением (Silent коррекция)
                                                    double SilentCorrection = PlayerRotationCalculations.calculateMovementAimConsistency(player, playerUUID);

                                                    // Проверка на изменение угла между Yaw и Pitch
                                                    double angleDelta = PlayerRotationCalculations.calculateAngleDelta(player, playerUUID);

                                                    String message = "§6§lДанные о последней атаке:\n"
                                                            + "§e§l===============================\n"
                                                            + "§aАрктангенс: §f" + atan2 + "\n"
                                                            + "§aСкорость передвижения: §f" + speed + "\n"
                                                            + "§aТочность (от 0 градусов): §f" + accuracy + "\n"
                                                            + "§aРотация sin() и cos(): §f" + rotationSinCos + "\n"
                                                            + "§aСкорость ротации: §f" + directionChange + "\n"
                                                            + "§aСкорость изменения направления: §f" + directionChangeSpeed + "\n"
                                                            + "§aСкорость ротации с Yaw и Pitch: §f" + rotationSpeed + "\n"
                                                            + "§e§l===============================\n"
                                                            + "§c§lДополнительные параметры:\n"
                                                            + "§aРезкие изменения углов (snap): §f" + snapAim + "\n"
                                                            + "§aСоотношение ротации с Silent Correction: §f" + SilentCorrection + "\n"
                                                            + "§aИзменение угла между Yaw и Pitch: §f" + angleDelta + "\n"
                                                            + "§e§l===============================";

                                                    Bukkit.getServer().broadcastMessage(message);
                                                }

                                            } else {

                                                double snapAim = PlayerRotationCalculations.calculateSnapAim(player, playerUUID);

                                                double SilentCorrection = PlayerRotationCalculations.calculateMovementAimConsistency(player, playerUUID);

                                                double angleDelta = PlayerRotationCalculations.calculateAngleDelta(player, playerUUID);

                                                if (snapAim >= minSnapAim && snapAim <= maxSnapAim
                                                        && SilentCorrection >= minSilentDeviation && SilentCorrection <= maxSilentDeviation
                                                        && angleDelta >= minAngleDelta && angleDelta <= maxAngleDelta) {

                                                    event.setCancelled(true);

                                                    violations++;
                                                    playerViolationCount.put(player.getName(), violations);

                                                    AlertManager.sendAlert(configManager, player, "AimA", violations, maxViolations);
                                                    DiscordWebhook.sendAlert(WEBHOOK_URL, player.getName(), "AimA", violations, maxViolations);

                                                    if (violations >= maxViolations) {
                                                        executePunishment(player.getName());
                                                        playerViolationCount.remove(player.getName());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                playerLocations.put(playerUUID, playerLocation);
                            });
                        }
                    });
                });
            });
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

    private float normalizePitch(float pitch) {
        pitch = pitch % 360;
        if (pitch < -90) pitch += 180;
        if (pitch > 90) pitch -= 180;
        return pitch;
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.MLAuraPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "AimA");
            DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AimA");

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}