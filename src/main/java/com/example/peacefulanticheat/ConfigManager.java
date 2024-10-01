package com.example.peacefulanticheat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.bukkit.Bukkit.*;

public class ConfigManager {
    private final PeacefulAntiCheat plugin;
    private FileConfiguration config;
    private final ConcurrentMap<String, Boolean> checkCache = new ConcurrentHashMap<>();

    public ConfigManager(PeacefulAntiCheat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private File getDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        // Проверяем, существует ли файл
        if (!configFile.exists()) {
            try {
                // Копируем файл из ресурсов в папку плагина
                InputStream inputStream = plugin.getResource("config.yml");
                if (inputStream == null) {
                    getLogger().severe("Файл config.yml не найден в ресурсах плагина!");
                    return;
                }
                Path configFilePath = configFile.toPath();
                if (configFilePath == null) {
                    getLogger().severe("Не удалось получить путь к файлу config.yml!");
                    return;
                }
                Files.copy(inputStream, configFilePath);
            } catch (IOException e) {
                getLogger().severe("Не удалось скопировать файл config.yml из ресурсов!");
                e.printStackTrace();
                return;
            }
        }

        // Загружаем конфигурацию
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        loadConfig();
        checkCache.clear(); // Очищаем кэш после перезагрузки конфигурации
    }

    public boolean isCheckEnabled(String checkName) {
        return checkCache.computeIfAbsent(checkName, name -> config.getBoolean("Checks." + name + ".enable"));
    }

    public String AutoFishPunishmentB() {
        return config.getString("Checks.AutoFish.b.punishment");
    }

    public String DragonFlyPunishment() {
        return config.getString("Checks.DragonFly.a.punishment");
    }

    public String AutoFishPunishmentA() {
        return config.getString("Checks.AutoFish.a.punishment");
    }

    public int AutoFishAViolations() {
        return config.getInt("Checks.AutoFish.a.violations");
    }

    public int AutoFishADeviation() {
        return config.getInt("Checks.AutoFish.a.deviation");
    }

    public int AutoFishBTime() {
        return config.getInt("Checks.AutoFish.b.time");
    }

    public int AdHeightFixHeight() {
        return config.getInt("Checks.AdHeightFix.a.height");
    }

    public int AdHeightFixTeleport() {
        return config.getInt("Checks.AdHeightFix.a.teleport");
    }

    public String AdHeightFixMessage() {
        return config.getString("Checks.AdHeightFix.a.message");
    }

    public int DragonFlyViolations() {
        return config.getInt("Checks.DragonFly.a.violations");
    }

    public int DragonFlySpeed_1_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_1_zx");
    }

    public int DragonFlySpeed_2_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_2_zx");
    }

    public int DragonFlySpeed_3_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_3_zx");
    }

    public int DragonFlySpeed_4_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_4_zx");
    }

    public int DragonFlySpeed_5_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_5_zx");
    }

    public int DragonFlySpeed_6_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_6_zx");
    }

    public int DragonFlySpeed_7_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_7_zx");
    }

    public int DragonFlySpeed_8_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_8_zx");
    }

    public int DragonFlySpeed_9_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_9_zx");
    }

    public int DragonFlySpeed_10_zx() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_10_zx");
    }

    public int DragonFlySpeed_1_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_1_y");
    }

    public int DragonFlySpeed_2_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_2_y");
    }

    public int DragonFlySpeed_3_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_3_y");
    }

    public int DragonFlySpeed_4_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_4_y");
    }

    public int DragonFlySpeed_5_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_5_y");
    }

    public int DragonFlySpeed_6_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_6_y");
    }

    public int DragonFlySpeed_7_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_7_y");
    }

    public int DragonFlySpeed_8_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_8_y");
    }

    public int DragonFlySpeed_9_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_9_y");
    }

    public int DragonFlySpeed_10_y() {
        return config.getInt("Checks.DragonFly.a.Settings.Speed_10_y");
    }

    public int ElytraFlyViolations() {
        return config.getInt("Checks.ElytraFly.a.violations");
    }

    public String ElytraFlyPunishment() {
        return config.getString("Checks.ElytraFly.a.punishment");
    }

    public int ElytraFlyHeight() {
        return config.getInt("Checks.ElytraFly.a.height");
    }

    public String ChatBanPunishment() {
        return config.getString("Checks.ChatBan.a.punishment");
    }

    public String NicknameBansPunishment() {
        return config.getString("Checks.NicknameBans.a.punishment");
    }

    public boolean MLAuraAdditionalChecks() {
        return config.getBoolean("Checks.Aim.a.AddChecks.enable");
    }

    public double MLAuraMinAtan2() {
        return config.getDouble("Checks.Aim.a.Settings.minAtan2");
    }

    public double MLAuraMaxAtan2() {
        return config.getDouble("Checks.Aim.a.Settings.maxAtan2");
    }

    public double MLAuraSpeedPlayer() {
        return config.getDouble("Checks.Aim.a.Settings.Speed");
    }

    public double MLAuraMinAccuracy() {
        return config.getDouble("Checks.Aim.a.Settings.minAccuracy");
    }

    public double MLAuraMaxAccuracy() {
        return config.getDouble("Checks.Aim.a.Settings.maxAccuracy");
    }

    public double MLAuraMinRotationSinCos() {
        return config.getDouble("Checks.Aim.a.Settings.minRotationSinCos");
    }

    public double MLAuraMaxRotationSinCos() {
        return config.getDouble("Checks.Aim.a.Settings.maxRotationSinCos");
    }

    public double MLAuraMinDirectionChange() {
        return config.getDouble("Checks.Aim.a.Settings.minDirectionChange");
    }

    public double MLAuraMaxDirectionChange() {
        return config.getDouble("Checks.Aim.a.Settings.maxDirectionChange");
    }

    public double MLAuraMinDirectionChangeSpeed() {
        return config.getDouble("Checks.Aim.a.Settings.minDirectionChangeSpeed");
    }

    public double MLAuraMaxDirectionChangeSpeed() {
        return config.getDouble("Checks.Aim.a.Settings.maxDirectionChangeSpeed");
    }

    public double MLAuraMinRotationSpeed() {
        return config.getDouble("Checks.Aim.a.Settings.minRotationSpeed");
    }

    public double MLAuraMaxRotationSpeed() {
        return config.getDouble("Checks.Aim.a.Settings.maxRotationSpeed");
    }

    public double MLAuraMinSnapAim() {
        return config.getDouble("Checks.Aim.a.AddChecks.minSnapAim");
    }

    public double MLAuraMaxSnapAim() {
        return config.getDouble("Checks.Aim.a.AddChecks.maxSnapAim");
    }

    public double MLAuraMinSilentDeviation() {
        return config.getDouble("Checks.Aim.a.AddChecks.minSilentDeviation");
    }

    public double MLAuraMaxSilentDeviation() {
        return config.getDouble("Checks.Aim.a.AddChecks.maxSilentDeviation");
    }

    public double MLAuraMinAngleDelta() {
        return config.getDouble("Checks.Aim.a.AddChecks.minAngleDelta");
    }

    public double MLAuraMaxAngleDelta() {
        return config.getDouble("Checks.Aim.a.AddChecks.maxAngleDelta");
    }

    public String MLAuraPunishment() {
        return config.getString("Checks.Aim.a.punishment");
    }

    public int MLAuraMaxViolations() {
        return config.getInt("Checks.Aim.a.violations");
    }

    public double AimBMinAtan2() {
        return config.getDouble("Checks.Aim.b.Settings.minAtan2");
    }

    public double AimBMaxAtan2() {
        return config.getDouble("Checks.Aim.b.Settings.maxAtan2");
    }

    public double AimBMinAccuracy() {
        return config.getDouble("Checks.Aim.b.Settings.minAccuracy");
    }

    public double AimBMaxAccuracy() {
        return config.getDouble("Checks.Aim.b.Settings.maxAccuracy");
    }

    public double AimBMinDirectionChange() {
        return config.getDouble("Checks.Aim.b.Settings.minDirectionChange");
    }

    public double AimBMaxDirectionChange() {
        return config.getDouble("Checks.Aim.b.Settings.maxDirectionChange");
    }

    public int AimBMaxViolations() {
        return config.getInt("Checks.Aim.b.Settings.maxDirectionChange");
    }

    public String AimBPunishment() {
        return config.getString("Checks.Aim.b.punishment");
    }
}