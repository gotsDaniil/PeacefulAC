package com.example.peacefulanticheat.Checks.utils;

import com.example.peacefulanticheat.ConfigManager;
import com.example.peacefulanticheat.api.Placeholders;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ChatBan extends PacketListenerAbstract {

    private final ConfigManager configManager;
    private final Pattern bannedMessagePattern;

    public ChatBan(ConfigManager configManager) {
        this.configManager = configManager;

        // Список возможных сообщений от читеров
        String bannedMessages = String.join("|",
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
        this.bannedMessagePattern = Pattern.compile(bannedMessages, Pattern.CASE_INSENSITIVE);

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage chatMessage = new WrapperPlayClientChatMessage(event);
            String message = chatMessage.getMessage();
            Player player = event.getPlayer();
            String playerName = player.getName();

            // Проверяем сообщения в чате и баним игрока, если обнаружены эти сообщения
            if (bannedMessagePattern.matcher(message).find()) {
                executePunishment(playerName);
            }
        }
    }
    private void executePunishment(String playerName) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PeacefulAntiCheat"), () -> {
            String punishment = configManager.ChatBanPunishment();
            Player player = Bukkit.getPlayer(playerName);
            punishment = Placeholders.replacePlaceholders(player, punishment);

            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(consoleSender, punishment);
        });
    }
}