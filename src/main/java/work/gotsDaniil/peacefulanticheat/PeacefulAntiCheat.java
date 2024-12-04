package work.gotsDaniil.peacefulanticheat;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import work.gotsDaniil.peacefulanticheat.checks.helpers.AdHeightFix;
import work.gotsDaniil.peacefulanticheat.checks.helpers.ChatListener;
import work.gotsDaniil.peacefulanticheat.checks.helpers.NicknamePatterns;
import work.gotsDaniil.peacefulanticheat.checks.player.*;
import work.gotsDaniil.peacefulanticheat.checks.combat.*;
import work.gotsDaniil.peacefulanticheat.checks.movement.*;
import work.gotsDaniil.peacefulanticheat.utils.Alerts.*;
import work.gotsDaniil.peacefulanticheat.utils.Discord.*;
import work.gotsDaniil.peacefulanticheat.utils.Commands.*;
import work.gotsDaniil.peacefulanticheat.api.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class PeacefulAntiCheat extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private AlertManager alertManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        initializeManagers();
        registerCommands();
        createPluginFolder();
        registerChecks();
        registerListeners();
        saveDefaultConfig();
    }

    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.alertManager = new AlertManager(configManager);
    }

    private void registerCommands() {
        Objects.requireNonNull(this.getCommand("peaceful")).setExecutor(new PeacefulCommand(alertManager, configManager));
    }

    private void createPluginFolder() {
        File pluginFolder = new File(getDataFolder().getAbsolutePath());
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "PeacefulAntiCheat включен");
    }

    private void registerChecks() {
        Check[] checks = {
                new Check("AttackEat.a", AttackEat::new),
                new Check("Aim.a", () -> new MLAura(configManager, this)),
                new Check("Aim.b", () -> new AimB(configManager, this)),
                new Check("Aim.c", () -> new AimC(configManager, this)),
                new Check("Aim.d", () -> new AimD(configManager, this)),
                new Check("AutoClicker.a", () -> new AutoClicker(configManager, this)),
                new Check("AutoFish.a", () -> new AutoFishA(configManager, this)),
                new Check("AutoFish.b", () -> new AutoFishB(configManager, this)),
                new Check("AdHeightFix.a", () -> new AdHeightFix(configManager, this)),
                new Check("ChatListener.a", () -> new ChatListener(configManager, this)),
                new Check("ElytraFly.a", () -> new ElytraFly(configManager, this)),
                new Check("Inventory.a", () -> new InvMove(configManager, this)),
                new Check("NicknamePatterns.a", () -> new NicknamePatterns(configManager, this)),
                new Check("FastExp.a", FastExp::new),
                new Check("Speed.a", () -> new SpeedA(configManager, this))
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

    private void registerListeners() {

        Placeholders placeholders = new Placeholders();
        Bukkit.getPluginManager().registerEvents(placeholders, this);

        DiscordWebhook discordWebhook = new DiscordWebhook(configManager);
        PacketEvents.getAPI().getEventManager().registerListener(discordWebhook);

        PlayerRotationCalculations PlayerRotationCalculations = new PlayerRotationCalculations();
        Bukkit.getPluginManager().registerEvents(PlayerRotationCalculations, this);
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

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }
}