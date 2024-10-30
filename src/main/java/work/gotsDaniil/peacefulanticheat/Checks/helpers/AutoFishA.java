package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.AlertManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.FishHook;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AutoFishA extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final ConcurrentMap<String, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final int maxViolations;
    private final int deviation;
    private int violations;

    public AutoFishA(ConfigManager configManager) {
        this.configManager = configManager;
        this.maxViolations = configManager.AutoFishAViolations();
        this.deviation = configManager.AutoFishADeviation();
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactEntityPacket = new WrapperPlayClientInteractEntity(event);
            Player player = event.getPlayer();
            int entityId = interactEntityPacket.getEntityId();

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
                Entity targetEntity = null;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity.getEntityId() == entityId) {
                        targetEntity = entity;
                        break;
                    }
                }

                if (targetEntity instanceof FishHook) {
                    String playerName = player.getName();

                    // Проверка, есть ли рыба в инвентаре игрока
                    boolean hasFish = player.getInventory().contains(Material.COD)
                            || player.getInventory().contains(Material.SALMON)
                            || player.getInventory().contains(Material.TROPICAL_FISH)
                            || player.getInventory().contains(Material.PUFFERFISH);

                    if (!hasFish) {
                        violations = playerViolationCount.getOrDefault(playerName, 0);

                        // Логика проверки
                        Location fishLocation = targetEntity.getLocation();
                        Location playerLocation = player.getLocation();

                        double distanceMoved = playerLocation.distance(fishLocation);
                        if (distanceMoved <= deviation) {
                            violations++;
                            playerViolationCount.put(playerName, violations);
                            AlertManager.sendAlert(configManager, player, "AutoFishA", violations, maxViolations);

                            if (violations >= maxViolations) {
                                executePunishment(playerName);
                                playerViolationCount.remove(playerName); // Сбрасываем нарушения игрока
                            }
                        } else {
                            playerViolationCount.put(playerName, 0);
                        }
                    }
                }
            });
        }
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.AutoFishPunishmentA();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "AutoFishA");
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}