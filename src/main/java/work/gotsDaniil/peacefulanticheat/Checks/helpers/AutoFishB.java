package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.AlertManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AutoFishB extends PacketListenerAbstract {

    private ConfigManager configManager;
    static ConcurrentMap<Player, Long> idleTimes = new ConcurrentHashMap<>();
    private long idleThreshold;
    private final Plugin plugin;

    public AutoFishB(ConfigManager configManager, Plugin plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.idleThreshold = configManager.AutoFishBTime();
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    public void updatePlayerActivity(Player player) {
        idleTimes.put(player, System.currentTimeMillis());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE
           || event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
           || event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
           || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
           || event.getPacketType() == PacketType.Play.Client.SLOT_STATE_CHANGE
           || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            updatePlayerActivity(player);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    idleTimes.putIfAbsent(player, currentTime);

                    long lastActivity = idleTimes.get(player);
                    if ((currentTime - lastActivity) > idleThreshold &&
                            (player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD)) {
                        executePunishment(player);
                        idleTimes.remove(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 600);
    }

    private void executePunishment(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
                String punishment = configManager.AutoFishPunishmentB();
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AutoFishB");
                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}