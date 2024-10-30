package work.gotsDaniil.peacefulanticheat.Checks.combat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.AlertManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

public class AimC extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final ConcurrentMap<UUID, Integer> hitCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> hitCounterDef = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Vector> previousDirections = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> speedSet = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, BukkitRunnable> speedResetTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final PeacefulAntiCheat plugin;
    private final int InitialHitsCounts;
    private final int AddHitsDefCounts;
    private final int BlockedHitInterval;
    private final int SpeedGiveAmount;
    private final int SpeedResetAmount;
    private final long SpeedResetTime;
    private final int maxViolations;

    public AimC(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.maxViolations = configManager.AimCMaxViolations();
        this.InitialHitsCounts = configManager.AimCInitialHitsCounts();
        this.AddHitsDefCounts = configManager.AimCAddHitsDefCounts();
        this.BlockedHitInterval = configManager.AimCBlockedHitInterval();
        this.SpeedResetTime = configManager.AimCSpeedResetTime();
        this.SpeedGiveAmount = configManager.AimCSpeedGiveAmount();
        this.SpeedResetAmount = configManager.AimCSpeedGiveReset();
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactPacket = new WrapperPlayClientInteractEntity(event);
            if (interactPacket.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                Player attacker = Bukkit.getPlayer(event.getUser().getUUID());
                int entityId = interactPacket.getEntityId();

                if (attacker != null) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        UUID attackerUUID = attacker.getUniqueId();
                        long currentTime = System.currentTimeMillis();
                        long lastTime = lastHitTime.getOrDefault(attackerUUID, 0L);
                        long timeDifference = currentTime - lastTime;

                        if (timeDifference <= BlockedHitInterval) {
                            resetCounters(attackerUUID);
                            return;
                        }

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Entity victimEntity = getEntityById(entityId);
                            if (!(victimEntity instanceof Player)) return;

                            Player victim = (Player) victimEntity;

                            if (victim.getLocation().distanceSquared(attacker.getLocation()) > 100) {
                                resetCounters(attackerUUID);
                                return;
                            }

                            incrementCounters(attackerUUID);

                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();

                                if (hitCounter.get(attackerUUID) >= InitialHitsCounts && !speedSet.getOrDefault(attackerUUID, false) && attacker.isSprinting() && victim.isSprinting()) {
                                    SpeedGive(attacker, consoleSender);
                                    speedSet.put(attackerUUID, true);

                                    BukkitRunnable resetTask = new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            resetSpeed(attacker, consoleSender);
                                        }
                                    };
                                    resetTask.runTaskLaterAsynchronously(plugin, 20 * SpeedResetTime);
                                    speedResetTasks.put(attackerUUID, resetTask);
                                }

                                if (speedSet.getOrDefault(attackerUUID, false)) {
                                    if (victim.getLocation().distanceSquared(attacker.getLocation()) > 100) {
                                        resetSpeed(attacker, consoleSender);
                                        resetCounters(attackerUUID);
                                        return;
                                    }

                                    if (timeDifference <= BlockedHitInterval) {
                                        resetCounters(attackerUUID);
                                        return;
                                    }

                                    int violations = playerViolationCount.getOrDefault(attacker.getName(), 0);

                                    if (hitCounterDef.get(attackerUUID) >= AddHitsDefCounts && attacker.isSprinting() && victim.isSprinting()) {
                                        violations++;
                                        playerViolationCount.put(attacker.getName(), violations);
                                        resetSpeed(attacker, consoleSender);
                                        resetCounters(attackerUUID);
                                        AlertManager.sendAlert(configManager, attacker, "AimC", violations, maxViolations);

                                        if (violations >= maxViolations) {
                                            executePunishment(attacker, attacker.getName());
                                        } else {
                                            playerViolationCount.put(attacker.getName(), 0);
                                        }
                                    }
                                }

                                previousDirections.put(attackerUUID, attacker.getLocation().getDirection());
                                lastHitTime.put(attackerUUID, currentTime);
                            });
                        });
                    });
                }
            }
        }
    }

    private Entity getEntityById(int entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getEntityId() == entityId) {
                    return entity;
                }
            }
        }
        return null;
    }

    private void SpeedGive(Player attacker, ConsoleCommandSender sender) {
        Bukkit.dispatchCommand(sender, "speed walk " + SpeedGiveAmount + " " + attacker.getName());
    }

    private void resetCounters(UUID attackerUUID) {
        hitCounter.put(attackerUUID, 0);
        hitCounterDef.put(attackerUUID, 0);
        speedSet.put(attackerUUID, false);

        BukkitRunnable resetTask = speedResetTasks.remove(attackerUUID);
        if (resetTask != null) {
            resetTask.cancel();
        }
    }

    private void resetSpeed(Player player, ConsoleCommandSender sender) {
        Bukkit.dispatchCommand(sender, "speed walk " + SpeedResetAmount + " " + player.getName());
        speedSet.put(player.getUniqueId(), false);
        speedResetTasks.remove(player.getUniqueId());
    }

    private void incrementCounters(UUID attackerUUID) {
        hitCounter.put(attackerUUID, hitCounter.getOrDefault(attackerUUID, 0) + 1);
        hitCounterDef.put(attackerUUID, hitCounterDef.getOrDefault(attackerUUID, 0) + 1);
    }

    private void executePunishment(Player attacker, String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AimCPunishment();
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);
                AlertManager.sendAlertPunishment(configManager, player, "AimC");
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}