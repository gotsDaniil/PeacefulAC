package work.gotsDaniil.peacefulanticheat.utils.Discord;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DiscordWebhook extends PacketListenerAbstract {

    private ConfigManager configManager;
    private ConcurrentMap<String, String> clientInfo = new ConcurrentHashMap<>();
    private final String DiscordWebhookAlertMessage;
    private final String DiscordWebhookPunishmentMessage;
    private final String DiscordWebhookTitleMessage;
    private final int DiscordWebhookAlertColor;
    private final int DiscordWebhookPunishmentColor;
    private final int DiscordWebhookCriticalAlertColor;
    private final boolean DiscordWebhookState;

    public DiscordWebhook(ConfigManager configManager) {
        this.configManager = configManager;
        this.DiscordWebhookAlertMessage = configManager.DiscordWebhookAlertMessage();
        this.DiscordWebhookPunishmentMessage = configManager.DiscordWebhookPunishmentMessage();
        this.DiscordWebhookTitleMessage = configManager.DiscordWebhookTitleMessage();
        this.DiscordWebhookAlertColor = convertRGBtoInt(configManager.DiscordWebhookAlertColor());
        this.DiscordWebhookPunishmentColor = convertRGBtoInt(configManager.DiscordWebhookPunishmentColor());
        this.DiscordWebhookCriticalAlertColor = convertRGBtoInt(configManager.DiscordWebhookCriticalAlertColor());
        this.DiscordWebhookState = configManager.DiscordWebhookState();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (!DiscordWebhookState) return;

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

            if (!channelName.equalsIgnoreCase("minecraft:brand")
                && !channelName.equals("MC|Brand")) return;

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

    public void sendAlert(String webhookUrl, String playerName, String checkType, int violations, int maxViolations) {

        try {

            if (!DiscordWebhookState) return;

            Player player = Bukkit.getPlayer(playerName);
            if (player == null) return;

            if (violations >= maxViolations) return;

            String message = Placeholders.replacePlaceholdersAlerts(player, DiscordWebhookAlertMessage, checkType, violations, maxViolations);
            message = Placeholders.replacePingAndClientInfo(player, message);

            HttpURLConnection connection = createConnection(webhookUrl);
            if (connection == null) return;

            String jsonInputString;

            if (violations > maxViolations / 2) {

                jsonInputString = "{"
                        + "\"content\": \" \","
                        + "\"embeds\": ["
                        + "{"
                        + "\"title\": \"" + DiscordWebhookTitleMessage + "\","
                        + "\"description\": \"" + message + "\","
                        + "\"color\": " + DiscordWebhookCriticalAlertColor
                        + "}"
                        + "]"
                        + "}";
            } else {

                jsonInputString = "{"
                        + "\"content\": \" \","
                        + "\"embeds\": ["
                        + "{"
                        + "\"title\": \"" + DiscordWebhookTitleMessage + "\","
                        + "\"description\": \"" + message + "\","
                        + "\"color\": " + DiscordWebhookAlertColor
                        + "}"
                        + "]"
                        + "}";
            }

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = getResponseCode(connection);

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                Bukkit.getLogger().info("Не удалось отправить оповещение в Discord. Код ответа: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAlertPunish(String webhookUrl, String playerName, String checkType) {

        try {

            if (!DiscordWebhookState) return;

            Player player = Bukkit.getPlayer(playerName);
            if (player == null) return;

            String message = Placeholders.replacePlaceholdersAlertsPunish(player, DiscordWebhookPunishmentMessage, checkType);
            message = Placeholders.replacePingAndClientInfo(player, message);

            HttpURLConnection connection = createConnection(webhookUrl);
            if (connection == null) return;

            String jsonInputString = "{"
                    + "\"content\": \" \","
                    + "\"embeds\": ["
                    + "{"
                    + "\"title\": \"" + DiscordWebhookTitleMessage + "\","
                    + "\"description\": \"" + message + "\","
                    + "\"color\": " + DiscordWebhookPunishmentColor
                    + "}"
                    + "]"
                    + "}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = getResponseCode(connection);

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                Bukkit.getLogger().info("Не удалось отправить оповещение в Discord. Код ответа: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private HttpURLConnection createConnection(String webhookUrl) {

        try {

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            return connection;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int convertRGBtoInt(String rgb) {

        String[] parts = rgb.split(",");

        int red = Integer.parseInt(parts[0].trim());
        int green = Integer.parseInt(parts[1].trim());
        int blue = Integer.parseInt(parts[2].trim());

        return (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }
}