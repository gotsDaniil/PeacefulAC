package work.gotsDaniil.peacefulanticheat.checks.helpers;

import com.github.retrooper.packetevents.PacketEvents;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;

public class AdHeightFix extends PacketListenerAbstract {

    private final PeacefulAntiCheat plugin;
    private final ConfigManager configManager;
    private final int height;
    private final int teleport;

    public AdHeightFix(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.height = configManager.AdHeightFixHeight();
        this.teleport = configManager.AdHeightFixTeleport();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition playerPosition = new WrapperPlayClientPlayerPosition(event);
            Player player = Bukkit.getPlayer(event.getUser().getUUID());

            if (player != null) {

                World world = player.getWorld();

                if (world.getEnvironment() == World.Environment.NETHER) {

                    double y = playerPosition.getPosition().getY();

                    if (y >= height) {
                        Teleport(playerPosition, player);
                        String message = configManager.AdHeightFixMessage();
                        player.sendMessage(message);
                    }
                }
            }
        }
    }

    private void Teleport(WrapperPlayClientPlayerPosition playerPosition, Player player) {

        Location playerLocation = playerPosition.getLocation();

        org.bukkit.Location teleportLocation = new org.bukkit.Location(player.getWorld(), playerLocation.getX(), height, playerLocation.getZ());

        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(teleportLocation));
    }
}