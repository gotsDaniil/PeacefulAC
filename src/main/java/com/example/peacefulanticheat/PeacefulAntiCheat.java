package com.example.peacefulanticheat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PeacefulAntiCheat extends JavaPlugin {

    private ConfigManager configManager;
    static Map<Player, Long> idleTimes = new HashMap<>();
    private long idleThreshold = 1800000; // 30 мин в миллисекундах
    private int checkInterval = 6000; // Проверка каждые 5 минут (6000 тиков)

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.getCommand("empty").setExecutor(new NullCommandExecutor());

        // Создаем папку PeacefulAntiCheat, если она не существует
        File pluginFolder = new File(getDataFolder().getAbsolutePath());
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "PeacefulAntiCheat включен");

        // Регистрация классов-проверок, некоторые проверки отключены
        ChatBan ChatBan = new ChatBan(configManager);
        getServer().getPluginManager().registerEvents(ChatBan, this);
        AttackEat AttackEat = new AttackEat(configManager);
        getServer().getPluginManager().registerEvents(AttackEat, this);
        NicknameBans NicknameBans = new NicknameBans(configManager);
        getServer().getPluginManager().registerEvents(NicknameBans, this);
        BedrockPearlFix BedrockPearlFix = new BedrockPearlFix(configManager);
        getServer().getPluginManager().registerEvents(BedrockPearlFix, this);
        FastExp FastExp = new FastExp(configManager);
        getServer().getPluginManager().registerEvents(FastExp, this);
        AutoFishA AutoFishA = new AutoFishA(configManager);
        getServer().getPluginManager().registerEvents(AutoFishA, this);
        AutoFishB AutoFishB = new AutoFishB(configManager);
        getServer().getPluginManager().registerEvents(AutoFishB, this);
        AutoFishC AutoFishC = new AutoFishC();
        getServer().getPluginManager().registerEvents(AutoFishC, this);
        DragonFlyA DragonFlyA = new DragonFlyA(configManager);
        getServer().getPluginManager().registerEvents(DragonFlyA, this);
        saveDefaultConfig();

        new BukkitRunnable() { // Кусочек кода AutoFishC
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (!idleTimes.containsKey(player)) {
                        idleTimes.put(player, System.currentTimeMillis());
                    }

                    long lastActivity = idleTimes.get(player);
                    if ((System.currentTimeMillis() - lastActivity) > idleThreshold &&
                            player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
                        String punishment2 = configManager.getPunishment2();
                        String message2 = configManager.getMessage2();

                        ConsoleCommandSender consoleSender = getServer().getConsoleSender();
                        getServer().dispatchCommand(consoleSender, punishment2 + " " + player.getName() + " " + message2);
                        idleTimes.remove(player); // Удаляем игрока из списка
                    }
                }
            }
        }.runTaskTimer(this, 0, checkInterval);
    }

    public class NullCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Команда доступна только из консоли
            if (sender == getServer().getConsoleSender()) {
                return true; // Команда выполнена успешно, но ничего не делаем
            }
            return false; // Команда не выполнена, если отправитель не консоль
        }
    }
}