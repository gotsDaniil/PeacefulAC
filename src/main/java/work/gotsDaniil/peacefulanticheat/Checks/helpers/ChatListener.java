package work.gotsDaniil.peacefulanticheat.Checks.helpers;

import work.gotsDaniil.peacefulanticheat.ConfigManager;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import work.gotsDaniil.peacefulanticheat.utils.AlertManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChatListener extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final List<String> bannedMessages;

    public ChatListener(ConfigManager configManager) {
        this.configManager = configManager;

        // Список возможных сообщений от читеров
        this.bannedMessages = Arrays.asList(
                "нурик топ", "nursultan топ", "какой чит выбрать", "wild топ",
                "arbuz client топ", "вилд топ", "arbuz топ", "арбуз топ",
                "нурик имба", "nursultan имба", "нурик пена", "nursultan пена",
                "вилд пена", "wild пена", "арбуз пена", "arbuz пена",
                "нурик бустит", "nursultan бустит", "arbuz бустит", "флюгер бустит",
                "кфг флюгера бустит", "флюгер клиент бустит", "Fluger Client бустит",
                "Expensive топ", "у меня xray", "у меня иксрей", "у меня храу",
                "у меня килка", "включаю килку", "включаю Killaura",
                "I have an xray", "I have a kilka", "I have a Killaura",
                "Turning on killaura"
        );

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage chatMessage = new WrapperPlayClientChatMessage(event);
            String message = chatMessage.getMessage().toLowerCase();
            Player player = event.getPlayer();
            String playerName = player.getName();

            // Проверяем сообщения в чате и баним игрока, если обнаружены эти сообщения
            for (String bannedMessage : bannedMessages) {
                if (message.contains(bannedMessage)) {
                    executePunishment(playerName);
                    break;
                }
            }
        }
    }

    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat")), () -> {
            String punishment = configManager.ChatListenerPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholderPlayer(player, punishment);

            AlertManager.sendAlertPunishment(configManager, player, "ChatListener");
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}