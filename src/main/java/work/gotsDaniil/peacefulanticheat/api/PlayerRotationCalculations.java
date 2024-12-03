package work.gotsDaniil.peacefulanticheat.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerRotationCalculations implements Listener {

    private static final ConcurrentMap<UUID, Long> lastRotationTime = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Double> lastYaw = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Double> lastPitch = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Location> playerLocations = new ConcurrentHashMap<>();

    public static double calculateAim(Player attacker, Player victim) {
        Location attackerLocation = attacker.getLocation();
        Vector attackerDirection = attackerLocation.getDirection();

        Vector attackerPosition = attackerLocation.toVector();
        Vector toVictim = victim.getLocation().toVector().subtract(attackerPosition).normalize();

        double dotProduct = attackerDirection.dot(toVictim);
        double angle = Math.toDegrees(Math.acos(dotProduct));
        double aim = angle / 90.0d;

        Vector attackerVelocity = attacker.getVelocity();
        double movementFactor = attackerVelocity.length() / 10.0d;

        Vector previousDirection = attackerLocation.getDirection();
        double directionChange = previousDirection.angle(attackerDirection);
        double directionChangeFactor = directionChange / Math.PI;

        return aim + movementFactor + directionChangeFactor;
    }

    public static double calculateRotationSinCos(Location playerLocation, double pitchRadians) {
        double yawRadians = Math.toRadians(playerLocation.getYaw());
        double sinYaw = Math.sin(yawRadians);
        double cosYaw = Math.cos(yawRadians);
        double sinPitch = Math.sin(pitchRadians);
        double cosPitch = Math.cos(pitchRadians);
        return Math.abs(sinYaw * cosPitch - cosYaw * sinPitch);
    }

    public static double calculateDirectionChange(Player attacker, Player victim) {
        Vector previousDirection = attacker.getLocation().getDirection();
        Vector currentDirection = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        return previousDirection.angle(currentDirection);
    }

    public static double calculateAtan2(Player player) {
        float pitch = player.getLocation().getPitch();
        pitch = normalizePitch(pitch);
        double pitchRadians = Math.toRadians(pitch);
        return Math.atan2(Math.sin(pitchRadians), Math.cos(pitchRadians));
    }

    public static double calculateRotationSpeed(Player player, UUID playerUUID) {
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

    public static double calculateSnapAim(Player player, UUID playerUUID) {
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

    public static double calculateMovementAimConsistency(Player player, UUID playerUUID) {
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

    public static double calculateAngleDelta(Player player, UUID playerUUID) {
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

    private static float normalizePitch(float pitch) {
        pitch = pitch % 360;
        if (pitch < -90) pitch += 180;
        if (pitch > 90) pitch -= 180;
        return pitch;
    }
}