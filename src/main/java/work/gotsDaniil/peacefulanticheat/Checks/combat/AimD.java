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
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

public class AimD extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ConcurrentMap<UUID, Integer> hitCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Vector> previousDirections = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> speedSet = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, BukkitRunnable> speedResetTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final PeacefulAntiCheat plugin;
    private final int InitialHitsCounts;
    private final int BlockedHitInterval;
    private final int maxViolations;
    private final String WEBHOOK_URL;

    public AimD(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.maxViolations = configManager.AimDMaxViolations();
        this.InitialHitsCounts = configManager.AimDInitialHitsCounts();
        this.BlockedHitInterval = configManager.AimDBlockedHitInterval();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);

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

                                if (hitCounter.get(attackerUUID) >= InitialHitsCounts
                                    && !speedSet.getOrDefault(attackerUUID, false)
                                    && attacker.isSprinting()
                                    && attacker.isSwimming()
                                    && victim.isSprinting()
                                    && victim.isSwimming()) {

                                    int violations = playerViolationCount.getOrDefault(attacker.getName(), 0);

                                    violations++;
                                    playerViolationCount.put(attacker.getName(), violations);
                                    resetCounters(attackerUUID);

                                    AlertManager.sendAlert(configManager, attacker, "AimD", violations, maxViolations);
                                    DiscordWebhook.sendAlert(WEBHOOK_URL, attacker.getName(), "AimD", violations, maxViolations);

                                    if (violations >= maxViolations) {
                                        executePunishment(attacker.getName());
                                    } else {
                                        playerViolationCount.put(attacker.getName(), 0);
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

    private void resetCounters(UUID attackerUUID) {

        hitCounter.put(attackerUUID, 0);

        BukkitRunnable resetTask = speedResetTasks.remove(attackerUUID);
        if (resetTask != null) {
            resetTask.cancel();
        }
    }

    private void incrementCounters(UUID attackerUUID) {
        hitCounter.put(attackerUUID, hitCounter.getOrDefault(attackerUUID, 0) + 1);
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String punishment = configManager.AimDPunishment();
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AimD");
                DiscordWebhook.sendAlertPunish(WEBHOOK_URL, playerName, "AimD");

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }
}
