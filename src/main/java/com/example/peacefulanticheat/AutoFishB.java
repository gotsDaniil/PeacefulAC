package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoFishB implements Listener {
    private final ConfigManager configManager;
    private final Map<Player, List<Long>> playerReactionTimes = new HashMap<>();
    private final int MAX_REPEATS = 3;
    private final int SEQUENCE_LENGTH = 5;

    public AutoFishB(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Получаем время реакции игрока (в миллисекундах)
        long reactionTime = System.currentTimeMillis();

        List<Long> reactionTimes = playerReactionTimes.getOrDefault(player, new ArrayList<>());
        reactionTimes.add(reactionTime);

        // Если длина списка больше SEQUENCE_LENGTH, удаляем старые значения
        if (reactionTimes.size() > SEQUENCE_LENGTH) {
            reactionTimes.remove(0);
        }

        // Проверяем условия для кика игрока
        if (checkForRepeatedReaction(reactionTimes) || checkForRepeatedSequence(reactionTimes)) {
            ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
            String punishment1 = configManager.getPunishment1();
            String message1 = configManager.getMessage1();
            Bukkit.dispatchCommand(consoleSender, punishment1 + " " + player.getName() + " " + message1);
            playerReactionTimes.remove(player); // Удаляем игрока из отслеживания
        } else {
            playerReactionTimes.put(player, reactionTimes);
        }
    }

    private boolean checkForRepeatedReaction(List<Long> reactions) {
        if (reactions.size() < MAX_REPEATS) return false;

        long lastReaction = reactions.get(reactions.size() - 1);
        int count = 0;

        for (int i = reactions.size() - 1; i >= 0; i--) {
            if (reactions.get(i) == lastReaction) {
                count++;
                if (count >= MAX_REPEATS) {
                    return true;
                }
            } else {
                break;
            }
        }

        return false;
    }

    private boolean checkForRepeatedSequence(List<Long> reactions) {
        if (reactions.size() < 2 * SEQUENCE_LENGTH) return false;

        // Сравниваем последние две последовательности
        List<Long> lastSequence = reactions.subList(reactions.size() - SEQUENCE_LENGTH, reactions.size());
        List<Long> secondLastSequence = reactions.subList(reactions.size() - 2 * SEQUENCE_LENGTH, reactions.size() - SEQUENCE_LENGTH);

        return lastSequence.equals(secondLastSequence);
    }
}