package work.gotsDaniil.peacefulanticheat.checks.combat;

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
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

public class AimC extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final ConcurrentMap<UUID, Double> lastX = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Double> lastZ = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> hitCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> hitCounterDef = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> speedSet = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, BukkitRunnable> speedResetTasks = new ConcurrentHashMap<>();
    private final PeacefulAntiCheat plugin;
    private final int InitialHitsCounts;
    private final int AddHitsDefCounts;
    private final int BlockedHitInterval;
    private final double SpeedGiveAmount;
    private final int SpeedResetAmount;
    private final long SpeedResetTime;
    private final int maxViolations;
    private final String WEBHOOK_URL;

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
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);

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

                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                            if (victim.getLocation().distanceSquared(attacker.getLocation()) > 100) {
                                resetCounters(attackerUUID);
                                return;
                            }

                            incrementCounters(attackerUUID);

                                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();


                                UUID victimUUID = victim.getUniqueId();

                                double currentX = victim.getLocation().getX();
                                double currentZ = victim.getLocation().getZ();

                                if (lastX.containsKey(victimUUID) && lastZ.containsKey(victimUUID)) {

                                    double lastXValue = lastX.get(victimUUID);
                                    double lastZValue = lastZ.get(victimUUID);

                                    double deltaX = currentX - lastXValue;
                                    double deltaZ = currentZ - lastZValue;

                                    double deltaZX = deltaX * deltaX + deltaZ * deltaZ;

                                    if (hitCounter.get(attackerUUID) >= InitialHitsCounts
                                            && !speedSet.getOrDefault(attackerUUID, false)
                                            && attacker.isSprinting() && deltaZX >= 1.5) {
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

                                    if (hitCounterDef.get(attackerUUID) >= AddHitsDefCounts
                                        && attacker.isSprinting()
                                        && victim.isSprinting()) {

                                        violationsReset.addViolation(attackerUUID);
                                        int violations = violationsReset.getViolations(attackerUUID);

                                        resetSpeed(attacker, consoleSender);
                                        resetCounters(attackerUUID);

                                        AlertManager.sendAlert(configManager, attacker, "AimC", violations, maxViolations);
                                        DiscordWebhook.sendAlert(WEBHOOK_URL, attacker.getName(), "AimC", violations, maxViolations);

                                        if (violations >= maxViolations) {
                                            executePunishment(attacker.getName());
                                            violationsReset.deleteViolations(attackerUUID);
                                        }
                                    }
                                }

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
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(sender, "speed walk " + SpeedGiveAmount + " " + attacker.getName());
        });
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

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AimCPunishment();

            if (punishment.isEmpty() || punishment.trim().isEmpty()) return;

            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AimC");
                DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AimC");

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}