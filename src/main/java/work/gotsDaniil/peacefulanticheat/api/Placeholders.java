package work.gotsDaniil.peacefulanticheat.api;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Placeholders extends PacketListenerAbstract implements Listener {

    private static final ConcurrentMap<String, String> clientInfo = new ConcurrentHashMap<>();

    public Placeholders() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {

            WrapperPlayClientSettings settingsPacket = new WrapperPlayClientSettings(event);

            User user = event.getUser();

            if (user != null) {

                String playerName = user.getProfile().getName();
                String clientBrand = settingsPacket.getLocale();

                clientInfo.put(playerName, clientBrand);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE ||
                   event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {

            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

            String channelName = packet.getChannelName();

            if (!channelName.equalsIgnoreCase("minecraft:brand") && !channelName.equals("MC|Brand")) return;

            byte[] data = packet.getData();

            if (data.length > 64 || data.length == 0) return;

            byte[] minusLength = new byte[data.length - 1];

            System.arraycopy(data, 1, minusLength, 0, minusLength.length);
            String brand = new String(minusLength).replace(" (Velocity)", "");

            if (brand.isEmpty()) brand = "Неизвестный";

            User user = event.getUser();

            if (user != null) {
                String playerName = user.getProfile().getName();
                clientInfo.put(playerName, brand);
            }
        }
    }

    public static String replacePlaceholderPlayer(Player player, String message) {

        if (player == null) return message;

        message = message.replace("%player%", player.getName());

        return message;
    }

    public static String replacePlaceholdersAlerts(Player player, String message, String checkType, int violations, int maxViolations) {

        if (player == null) return message;

        message = message.replace("%player%", player.getName());
        message = message.replace("%checkType%", checkType);
        message = message.replace("%violations%", String.valueOf(violations));
        message = message.replace("%maxViolations%", String.valueOf(maxViolations));

        return message;
    }

    public static String replacePlaceholdersAlertsPunish(Player player, String message, String checkType) {

        if (player == null) return message;

        message = message.replace("%player%", player.getName());
        message = message.replace("%checkType%", checkType);

        return message;
    }

    public static String replacePingAndClientInfo(Player player, String message) {

        if (player == null) return message;

        int ping = player.getPing();

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);

        String clientVersion = user != null ? user.getClientVersion().toString() : " ";
        String clientBrand = clientInfo.getOrDefault(player.getName(), "Неизвестный");

        double tps = getTPS();
        double roundedTps = Math.round(tps * 100.0) / 100.0;

        message = message.replace("%ping%", String.valueOf(ping));
        message = message.replace("%clientVersion%", clientVersion);
        message = message.replace("%clientBrand%", clientBrand);
        message = message.replace("%tps%", String.valueOf(roundedTps));

        return message;
    }

    private static double getTPS() {
        return Bukkit.getServer().getTPS()[0];
    }
}