package com.example.peacefulanticheat.Checks.utils;

import com.example.peacefulanticheat.ConfigManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FastExp extends PacketListenerAbstract {

    private final ConfigManager configManager;

    public FastExp(ConfigManager configManager) {
        this.configManager = configManager;
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    private final ConcurrentMap<UUID, Integer> playerExperienceBottleCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> playerLastUsedTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem useItemPacket = new WrapperPlayClientUseItem(event);
            Player player = event.getPlayer();
            if (player == null) return;

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.EXPERIENCE_BOTTLE) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();

                // Проверяем, сколько пузырьков опыта использовал игрок за последнюю секунду
                Long lastUsedTime = playerLastUsedTime.get(playerId);
                if (lastUsedTime != null && (currentTime - lastUsedTime) < 500) {
                    int count = playerExperienceBottleCounts.getOrDefault(playerId, 0) + 1;

                    // Если игрок использовал 10 или более пузырьков, отменяем событие
                    if (count >= 10) {
                        event.setCancelled(true);
                        return;
                    }

                    playerExperienceBottleCounts.put(playerId, count);
                } else {
                    // Сбрасываем счетчик при новом использовании после 1 секунды
                    playerExperienceBottleCounts.put(playerId, 1);
                }

                // Обновляем время последнего использования
                playerLastUsedTime.put(playerId, currentTime);
            }
        }
    }
}