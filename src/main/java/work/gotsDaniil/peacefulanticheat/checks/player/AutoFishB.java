package work.gotsDaniil.peacefulanticheat.checks.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.PeacefulAntiCheat;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.AlertManager;
import work.gotsDaniil.peacefulanticheat.utils.Discord.DiscordWebhook;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AutoFishB extends PacketListenerAbstract {

    private ConfigManager configManager;
    private final DiscordWebhook DiscordWebhook;
    static ConcurrentMap<Player, Long> idleTimes = new ConcurrentHashMap<>();
    private long idleThreshold;
    private final PeacefulAntiCheat plugin;
    private final String WEBHOOK_URL;

    public AutoFishB(ConfigManager configManager, PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.idleThreshold = configManager.AutoFishBTime();
        this.WEBHOOK_URL = configManager.DISCORD_WEBHOOK_URL();
        this.DiscordWebhook = new DiscordWebhook(configManager);

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

                    if ((currentTime - lastActivity) > idleThreshold
                         && (player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD)
                         || (currentTime - lastActivity) > idleThreshold &&
                         (player.getInventory().getItemInOffHand().getType() == Material.FISHING_ROD)) {

                        executePunishment(player);
                        idleTimes.remove(player);

                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 6000L);
    }

    private void executePunishment(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
                String punishment = configManager.AutoFishPunishmentB();
                punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

                AlertManager.sendAlertPunishment(configManager, player, "AutoFishB");
                DiscordWebhook.sendAlertPunish(WEBHOOK_URL, player.getName(), "AutoFishB");

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}