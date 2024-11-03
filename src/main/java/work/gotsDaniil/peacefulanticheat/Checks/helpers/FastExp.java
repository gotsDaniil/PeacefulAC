package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FastExp extends PacketListenerAbstract {

    private final ConcurrentMap<UUID, Integer> playerExperienceBottleCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> playerLastUsedTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            handleUseItemPacket(event);
        }
    }

    private void handleUseItemPacket(PacketReceiveEvent event) {
        WrapperPlayClientUseItem useItemPacket = new WrapperPlayClientUseItem(event);
        Player player = event.getPlayer();

        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.EXPERIENCE_BOTTLE) {

            UUID playerId = player.getUniqueId();

            long currentTime = System.currentTimeMillis();
            Long lastUsedTime = playerLastUsedTime.get(playerId);

            if (lastUsedTime != null && (currentTime - lastUsedTime) < 500) {

                handleFastUse(event, playerId);

            } else {

                resetUsageCount(playerId);
            }

            updateLastUsedTime(playerId, currentTime);
        }
    }

    private void handleFastUse(PacketReceiveEvent event, UUID playerId) {

        int count = playerExperienceBottleCounts.getOrDefault(playerId, 0) + 1;

        if (count >= 10) {
            event.setCancelled(true);
            return;
        }

        playerExperienceBottleCounts.put(playerId, count);
    }

    private void resetUsageCount(UUID playerId) {
        playerExperienceBottleCounts.put(playerId, 1);
    }

    private void updateLastUsedTime(UUID playerId, long currentTime) {
        playerLastUsedTime.put(playerId, currentTime);
    }
}