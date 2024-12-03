package work.gotsDaniil.peacefulanticheat.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ViolationsReset {

    private final ConfigManager configManager;
    private final PeacefulAntiCheat plugin;
    private final ConcurrentMap<UUID, Long> lastTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> playerViolations;
    private final int timeResetViolations;

    public ViolationsReset(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerViolations = new ConcurrentHashMap<>();
        this.timeResetViolations = configManager.TimeResetViolations();
        startResetTask();
    }

    private void startResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ResetViolations(player.getUniqueId());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 3600L);
    }

    public void ResetViolations(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastTimes.getOrDefault(playerId, 0L);

        long timeDifference = currentTime - lastTime;

        if (timeDifference >= timeResetViolations) {
            playerViolations.put(playerId, 0);
            lastTimes.put(playerId, currentTime);
        }
    }


    public void addViolation(UUID playerId) {
        int violations = playerViolations.getOrDefault(playerId, 0);
        violations++;
        playerViolations.put(playerId, violations);
        lastTimes.put(playerId, System.currentTimeMillis());
    }

    public int getViolations(UUID playerId) {
        return playerViolations.getOrDefault(playerId, 0);
    }

    public void deleteViolations(UUID playerId) {
        playerViolations.remove(playerId);
        lastTimes.remove(playerId);
    }
}