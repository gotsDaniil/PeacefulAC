package work.gotsDaniil.peacefulanticheat;

import work.gotsDaniil.peacefulanticheat.Checks.exploits.GamemodeCreativeFix;
import work.gotsDaniil.peacefulanticheat.Checks.helpers.*;
import work.gotsDaniil.peacefulanticheat.utils.*;
import work.gotsDaniil.peacefulanticheat.Checks.cоmbat.*;
import work.gotsDaniil.peacefulanticheat.Checks.mоvement.*;
import work.gotsDaniil.peacefulanticheat.api.Placeholders;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
        PacketEvents.getAPI().lоad();
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.alertManager = new AlertManager(configManager);

        Objects.requireNonNull(this.getCommand("empty")).setExecutor(new NullCommandExecutor());
        Objects.requireNonNull(this.getCommand("gm1")).setExecutor(new GamemodeCreativeFix());
        Objects.requireNonNull(this.getCommand("peaceful")).setExecutor(new AlertsCommand(alertManager));

        // Создаем папку PeacefulAntiCheat, если она не существует
        File pluginFolder = new File(getDataFolder().getAbsolutePath());
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + "PeacefulAntiCheat включен");

        // Регистрация классов-проверок из метода registerChecks()
        registerChecks();

        // Все что регистрируется без возможности выключения
        Placeholders Placeholders = new Placeholders();
        Bukkit.getPluginManager().registerEvents(Placeholders, this);
        saveDefaultConfig();
    }

    private void registerChecks() {
        Check[] checks = {
                new Check("ChatListener.a", () -> new ChatListener(configManager)),
                new Check("AttackEat.a", AttackEat::new),
                new Check("NicknamePatterns.a", () -> new NicknamePatterns(configManager)),
                new Check("AdHeightFix.a", () -> new AdHeightFix(configManager)),
                new Check("FastExp.a", FastExp::new),
                new Check("AutoFish.a", () -> new AutoFishA(configManager)),
                new Check("AutoFish.b", () -> new AutoFishB(configManager, this)),
                new Check("ElytraFly.a", () -> new ElytraFly(configManager)),
                new Check("Aim.b", () -> new AimB(configManager, this)),
                new Check("Aim.a", () -> new MLAura(configManager)),
                new Check("Aim.c", () -> new AimC(configManager, this))
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
