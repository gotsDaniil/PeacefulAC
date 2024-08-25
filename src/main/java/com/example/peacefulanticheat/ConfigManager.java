package com.example.peacefulanticheat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import static org.bukkit.Bukkit.getLogger;

public class ConfigManager {
    private final PeacefulAntiCheat plugin;
    private FileConfiguration config;

    public ConfigManager(PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        createConfig();
        loadConfig();
        setDefaults();
    }

    private File getDataFolder() {
        return plugin.getDataFolder();
    }

    private void createConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        // Проверяем, существует ли файл
        if (!configFile.exists()) {
            // Копируем файл из ресурсов
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) {
                    return;
                }
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Config.yml был успешно загружен из ресурсов!");
            } catch (IOException e) {
                getLogger().severe("Не удалось создать config.yml: " + e.getMessage());
            }
        }
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void setDefaults() {
        config.options().header("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\n" +
                "  // Спасибо, что скачали мой античит\n" +
                "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");

        config.addDefault("#"," # punishment - команда наказания\n" +
                " # message - сообщение о наказании, можно в писать и время, если требуется\n" +
                " # Оставьте empty, если хотите отключить punishment или ' ' для message\n" +
                " # AutoFish - проверки на авто рыбалку\n" +
                " # AttackEat - проверка на атаку во время еды (поедания)\n" +
                " # BedrockPearlFix - блокировка прохода перкой через бедрок, опциально в аду\n" +
                " # ChatBan - наказывает игроков за сообщения содержащие название софта и его хвалу, пример: нурик топ\n" +
                " # NicknameBans - блокирует игроков за ники софтов, ботов и крашеров, пример: nurik, BebraProxy, ServerCrasher\n" +
                " # DragonFly - проверка на DragonFly, не дает быстро летать учитывая /speed игрока\n" +
                " # FastExp - блокирует выкидывание пузырьков опыта больше чем 10 за 0.55 секунды");
        // Устанавливаем значения по умолчанию
        config.addDefault("Checks.AutoFish.a.punishment", "kick");
        config.addDefault("Checks.AutoFish.a.message", "&cAC Вы были кикнуты за подозрение в читах (AutoFish)");
        config.addDefault("Checks.AutoFish.b.punishment", "kick");
        config.addDefault("Checks.AutoFish.b.message", "&cAC Вы были кикнуты за подозрение в читах (AutoFish)");
        config.addDefault("Checks.AutoFish.c.punishment", "kick");
        config.addDefault("Checks.AutoFish.c.message", "&cAC Вы были кикнуты за подозрение в читах (AutoFish)");
        config.addDefault("Checks.AttackEat.a.punishment", "empty");
        config.addDefault("Checks.AttackEat.a.message", " ");
        config.addDefault("Checks.BedrockPearlFix.a.punishment", "empty");
        config.addDefault("Checks.BedrockPearlFix.a.message", " ");
        config.addDefault("Checks.ChatBan.a.punishment", "tempipban");
        config.addDefault("Checks.ChatBan.a.message", "14d &cAC Вы были временно забанены подозрение в читах");
        config.addDefault("Checks.NicknameBans.a.punishment", "banip");
        config.addDefault("Checks.NicknameBans.a.message", "&cAC Вы были временно забанены подозрение в читах");
        config.addDefault("Checks.DragonFly.a.punishment", "empty");
        config.addDefault("Checks.DragonFly.a.message", " ");
        config.addDefault("Checks.FastExp.a.punishment", "empty");
        config.addDefault("Checks.FastExp.a.message", " ");
        config.options().copyDefaults(true);
        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage() {
        return config.getString("Checks.AutoFish.a.message");
    }

    public String getPunishment1() {
        return config.getString("Checks.AutoFish.b.punishment");
    }

    public String getMessage1() {
        return config.getString("Checks.AutoFish.b.message");
    }

    public String getPunishment2() {
        return config.getString("Checks.AutoFish.c.punishment");
    }

    public String getMessage2() {
        return config.getString("Checks.AutoFish.c.message");
    }

    public String getPunishment3() {
        return config.getString("Checks.AttackEat.a.punishment");
    }

    public String getMessage3() {
        return config.getString("Checks.AttackEat.a.message");
    }

    public String getPunishment4() {
        return config.getString("Checks.BedrockPearlFix.a.punishment");
    }

    public String getMessage4() {
        return config.getString("Checks.BedrockPearlFix.a.message");
    }

    public String getPunishment5() {
        return config.getString("Checks.ChatBan.a.punishment");
    }

    public String getMessage5() {
        return config.getString("Checks.ChatBan.a.message");
    }

    public String getPunishment6() {
        return config.getString("Checks.NicknameBans.a.punishment");
    }

    public String getMessage6() {
        return config.getString("Checks.NicknameBans.a.message");
    }

    public String getPunishment7() {
        return config.getString("Checks.DragonFly.a.punishment");
    }

    public String getMessage7() {
        return config.getString("Checks.DragonFly.a.message");
    }

    public String getPunishment8() {
        return config.getString("Checks.FastExp.a.punishment");
    }

    public String getMessage8() {
        return config.getString("Checks.FastExp.a.message");
    }

    public String getPunishment() {
        return config.getString("Checks.AutoFish.a.punishment");
    }
}