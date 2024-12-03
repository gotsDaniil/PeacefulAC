package work.gotsDaniil.peacefulanticheat.checks.movement;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;
import work.gotsDaniil.peacefulanticheat.utils.ViolationsReset;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.bukkit.GameMode.CREATIVE;

public class SpeedA extends PacketListenerAbstract {

    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    private final ViolationsReset violationsReset;
    private final ConcurrentMap<UUID, Queue<Location>> locationHistory = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastTime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private final String WEBHOOK_URL;
    private final boolean setback;

    public SpeedA(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.setback = configManager.SpeedASetback();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);
        this.violationsReset = new ViolationsReset(configManager, plugin);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLAYER_POSITION || packetType == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {

            WrapperPlayClientPlayerPosition playerPosition = new WrapperPlayClientPlayerPosition(event);

            User user = event.getUser();
            UUID playerId = user.getUUID();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null) return;

            if (player.isFlying()) {
                resetAverageSpeed(playerId);
                return;
            }

            if (player.getGameMode() == CREATIVE) {
                resetAverageSpeed(playerId);
                return;
            }

            if (player.getWalkSpeed() > 1f) {
                resetAverageSpeed(playerId);
                return;
            }

            if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
                resetAverageSpeed(playerId);
                setCooldown(playerId);
                return;
            }

            ItemStack getChestplate = player.getInventory().getChestplate();

            if (getChestplate != null && getChestplate.getType() == Material.ELYTRA) {
                resetAverageSpeed(playerId);
                return;
            }

            ItemStack itemHand = player.getInventory().getItemInMainHand();
            ItemStack itemOffhand = player.getInventory().getItemInOffHand();

            if (itemHand.getType() == Material.ENDER_PEARL
                    || itemHand.getType() == Material.CHORUS_FRUIT
                    || itemOffhand.getType() == Material.ENDER_PEARL
                    || itemOffhand.getType() == Material.CHORUS_FRUIT) {
                resetAverageSpeed(playerId);
                setCooldown(playerId);
            }

            Location currentLocation = playerPosition.getLocation();
            long currentTime = System.currentTimeMillis();

            if (!locationHistory.containsKey(playerId)) {
                locationHistory.put(playerId, new LinkedList<>());
            }

            Queue<Location> history = locationHistory.get(playerId);
            history.add(currentLocation);

            if (history.size() > 10) {
                history.poll();
            }

            if (lastTime.containsKey(playerId)) {
                long lastTimeValue = lastTime.get(playerId);
                long timeDifference = currentTime - lastTimeValue;

                if (timeDifference >= 500) {
                    double totalDistance = 0;
                    Location previousLocation = null;

                    for (Location location : history) {
                        if (previousLocation != null) {
                            double deltaX = location.getX() - previousLocation.getX();
                            double deltaZ = location.getZ() - previousLocation.getZ();
                            totalDistance += Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                        }
                        previousLocation = location;
                    }

                    double averageSpeed = totalDistance / (timeDifference / 1000.0);

                    double speedMultiplier = getSpeedMultiplier(player);

                    String playerName = player.getName();

                    int number = 0;

                    if (!player.isJumping() && !isOnCooldown(playerId)) {
                        if (averageSpeed >= 6.8 * speedMultiplier && averageSpeed <= 20 * speedMultiplier) {

                            AlertManager.sendAlert(configManager, player, "SpeedA (Beta)", number, number);
                            DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "SpeedA (Beta)", number, number);

                            if (setback) {
                                setBack(currentLocation, player);
                            }
                            resetAverageSpeed(playerId);
                        }
                    } else if (player.isJumping() && !isOnCooldown(playerId)) {
                        if (averageSpeed >= 7.8 * speedMultiplier && averageSpeed <= 25 * speedMultiplier) {

                            AlertManager.sendAlert(configManager, player, "SpeedA (Beta)", number, number);
                            DiscordWebhook.sendAlert(WEBHOOK_URL, playerName, "SpeedA (Beta)", number, number);

                            if (setback) {
                                setBack(currentLocation, player);
                            }
                            resetAverageSpeed(playerId);
                        }
                    }
                    lastTime.put(playerId, currentTime);
                }
            } else {
                lastTime.put(playerId, currentTime);
            }

            if (hasBlockAbove(player)) {
                setCooldown(playerId);
            }
        }
    }

    private void setCooldown(UUID playerId) {
        cooldown.put(playerId, System.currentTimeMillis() + 3000);
    }

    private boolean isOnCooldown(UUID playerId) {
        if (cooldown.containsKey(playerId)) {
            long cooldownTime = cooldown.get(playerId);
            if (System.currentTimeMillis() < cooldownTime) {
                return true;
            } else {
                cooldown.remove(playerId);
            }
        }
        return false;
    }

    private double getSpeedMultiplier(Player player) {
        double speedMultiplier = 1.0;

        PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            int speedLevel = speedEffect.getAmplifier() + 1;
            if (speedLevel <= 10) {
                speedMultiplier += (speedLevel * 0.20);
            }
        }
        speedMultiplier += getItemSpeedMultiplier(player);

        return speedMultiplier;
    }

    private double getItemSpeedMultiplier(Player player) {
        double itemSpeedMultiplier = 0.0;

        for (EquipmentSlot slot : Arrays.asList(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasAttributeModifiers()) {
                    Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
                    if (modifiers != null) {
                        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                            if (entry.getKey() == Attribute.GENERIC_MOVEMENT_SPEED) {
                                AttributeModifier modifier = entry.getValue();
                                itemSpeedMultiplier += modifier.getAmount() * 3;
                            }
                        }
                    }
                }
            }
        }

        return itemSpeedMultiplier;
    }

    private void resetAverageSpeed(UUID playerId) {
        if (locationHistory.containsKey(playerId)) {
            locationHistory.get(playerId).clear();
        }
    }

    private void setBack(Location currentLocation, Player player) {
        org.bukkit.Location teleportLocation = new org.bukkit.Location(player.getWorld(), currentLocation.getX(), currentLocation.getY(), currentLocation.getZ());
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(teleportLocation));
    }

    private boolean hasBlockAbove(Player player) {
        org.bukkit.Location playerLocation = player.getLocation();
        int x = playerLocation.getBlockX();
        int y = playerLocation.getBlockY() + 2;
        int z = playerLocation.getBlockZ();

        return player.getWorld().getBlockAt(x, y, z).getType() != Material.AIR;
    }
}