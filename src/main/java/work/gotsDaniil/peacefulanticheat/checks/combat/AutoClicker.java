package work.gotsDaniil.peacefulanticheat.checks.combat;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

public class AutoClicker extends PacketListenerAbstract implements Listener {

    private final PeacefulAntiCheat plugin;
    private final ConcurrentMap<UUID, Boolean> leftClickChecks = new ConcurrentHashMap<>();

    public AutoClicker(PeacefulAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void isLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            leftClickChecks.put(playerUUID, true);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactPacket = new WrapperPlayClientInteractEntity(event);
            User user = event.getUser();
            UUID playerUUID = user.getUUID();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null) return;

            if (interactPacket.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {

                if (leftClickChecks.getOrDefault(playerUUID, false)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (leftClickChecks.getOrDefault(playerUUID, false)) {
                                Bukkit.broadcastMessage(ChatColor.RED + "Игрок " + player.getName() + " атаковал без нажатия ЛКМ!");
                            }
                            leftClickChecks.remove(playerUUID);
                        }
                    }.runTaskLaterAsynchronously(plugin, 20L);
                } else {
                    leftClickChecks.remove(playerUUID);
                }
            }
        }
    }
}