package work.gotsDaniil.peacefulanticheat.checks.player;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FastExp extends PacketListenerAbstract {

    private final ConcurrentMap<UUID, Long> lastUseTime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> useCount = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastWarningTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem useItem = new WrapperPlayClientUseItem(event);
            User user = event.getUser();
            UUID playerId = user.getUUID();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null) return;

            if (player.getInventory().getItemInMainHand().getType() == Material.EXPERIENCE_BOTTLE ||
                    player.getInventory().getItemInOffHand().getType() == Material.EXPERIENCE_BOTTLE) {

                long currentTime = System.currentTimeMillis();
                long lastTime = lastUseTime.getOrDefault(playerId, 0L);
                int count = useCount.getOrDefault(playerId, 0);

                if (currentTime - lastTime < 1000) {
                    count++;
                    if (count > 10 && count <= 13) {
                        long lastWarning = lastWarningTime.getOrDefault(playerId, 0L);
                        if (currentTime - lastWarning > 1000) {
                            event.setCancelled(true);
                            lastWarningTime.put(playerId, currentTime);
                        }
                    }
                } else {
                    count = 1;
                }

                lastUseTime.put(playerId, currentTime);
                useCount.put(playerId, count);
            }
        }
    }
}