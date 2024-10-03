package com.example.peacefulanticheat;

import com.example.peacefulanticheat.Checks.combat.*;
import com.example.peacefulanticheat.Checks.exploits.GamemodeCreativeFix;
import com.example.peacefulanticheat.Checks.movement.*;
import com.example.peacefulanticheat.Checks.utils.*;
import com.example.peacefulanticheat.api.*;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PeacefulAntiCheat extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    static ConcurrentMap<Player, Long> idleTimes = new ConcurrentHashMap<>();
    private long idleThreshold;
    private int checkInterval = 6000; // Проверка каждые 5 минут (6000 тиков)

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.idleThreshold = configManager.AutoFishBTime();

        Objects.requireNonNull(this.getCommand("empty")).setExecutor(new NullCommandExecutor());
        Objects.requireNonNull(this.getCommand("gm1")).setExecutor(new GamemodeCreativeFix());

        // Создаем папку PeacefulAntiCheat, если она не существует
        File pluginFolder = new File(getDataFolder().getAbsolutePath());
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "PeacefulAntiCheat включен");

        // Регистрация классов-проверок из метода registerChecks()
        registerChecks();

        // Регистрируем api без возможности выключения
        Placeholders placeholders = new Placeholders();
        Bukkit.getPluginManager().registerEvents(placeholders, this);
        saveDefaultConfig();

        new BukkitRunnable() { // Кусочек кода AutoFishC
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    idleTimes.putIfAbsent(player, currentTime);

                    long lastActivity = idleTimes.get(player);
                    if ((currentTime - lastActivity) > idleThreshold &&
                            (player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD)) {
                        executePunishment(player);
                        idleTimes.remove(player); // Удаляем игрока из списка
                    }
                }
            }
        }.runTaskTimer(this, 0, checkInterval);
    }

    private void registerChecks() {
        Check[] checks = {
                new Check("ChatBan.a", () -> new ChatBan(configManager)),
                new Check("AttackEat.a", AttackEat::new),
                new Check("NicknameBans.a", () -> new NicknameBans(configManager)),
                new Check("AdHeightFix.a", () -> new AdHeightFix(configManager)),
                new Check("FastExp.a", () -> new FastExp(configManager)),
                new Check("AutoFish.a", () -> new AutoFishA(configManager)),
                new Check("AutoFish.b", AutoFishB::new),
                new Check("DragonFly.a", () -> new DragonFlyA(configManager)),
                new Check("ElytraFly.a", () -> new ElytraFly(configManager)),
                new Check("Aim.b", () -> new AimB(configManager)),
                new Check("Aim.a", () -> new MLAura(configManager)),
        };

        for (Check check : checks) {
            if (configManager.isCheckEnabled(check.name)) {
                Object instance = check.supplier.get();
                if (instance instanceof Listener) {
                    Bukkit.getPluginManager().registerEvents((Listener) instance, this);
                }
                if (instance instanceof com.github.retrooper.packetevents.event.PacketListenerAbstract) {
                    PacketEvents.getAPI().getEventManager().registerListener((com.github.retrooper.packetevents.event.PacketListenerAbstract) instance);
                }
            }
        }
    }

    private void executePunishment(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
                String punishment = configManager.AutoFishPunishmentB();
                punishment = Placeholders.replacePlaceholders(player, punishment);

                ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(consoleSender, punishment);
            }
        });
    }

    public class NullCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Команда доступна только из консоли
            return sender == Bukkit.getConsoleSender();
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    private static class Check {
        String name;
        Supplier<Object> supplier;

        Check(String name, Supplier<Object> supplier) {
            this.name = name;
            this.supplier = supplier;
        }
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get();
    }
}