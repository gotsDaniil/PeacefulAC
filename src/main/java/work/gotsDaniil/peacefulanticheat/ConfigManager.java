package work.gotsDaniil.peacefulanticheat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
                    plugin.getLogger().severe("Файл config.yml не найден в ресурсах плагина!");
                    return;
                }
                Path configFilePath = configFile.toPath();
                if (configFilePath == null) {
                    plugin.getLogger().severe("Не удалось получить путь к файлу config.yml!");
                    return;
                }
                Files.copy(inputStream, configFilePath);
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось скопировать файл config.yml из ресурсов!");
                e.printStackTrace();
                return;
            }
        }

        // Загружаем конфигурацию
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public boolean isCheckEnabled(String checkName) {
        return checkCache.computeIfAbsent(checkName, name -> config.getBoolean("Checks." + name + ".enable"));
    }

    public String EnableAlerts() {
        return config.getString("enableAlerts");
    }

    public String DisableAlerts() {
        return config.getString("disableAlerts");
    }

    public String AlertMessage() {
        return config.getString("alertMessage");
    }

    public String AlertPunishMessage() {
        return config.getString("alertPunishMessage");
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

    public int ElytraFlyViolations() {
        return config.getInt("Checks.ElytraFly.a.violations");
    }

    public String ElytraFlyPunishment() {
        return config.getString("Checks.ElytraFly.a.punishment");
    }

    public double ElytraFlyHeight() {
        return config.getDouble("Checks.ElytraFly.a.height");
    }

    public String ChatListenerPunishment() {
        return config.getString("Checks.ChatListener.a.punishment");
    }

    public String NicknamePatternsAPunishment() {
        return config.getString("Checks.NicknamePatterns.a.punishment");
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

    public String AimCPunishment() {
        return config.getString("Checks.Aim.c.punishment");
    }

    public int AimCMaxViolations() {
        return config.getInt("Checks.Aim.c.violations");
    }

    public int AimCInitialHitsCounts() {
        return config.getInt("Checks.Aim.c.Settings.InitialHitsCounts");
    }

    public int AimCAddHitsDefCounts() {
        return config.getInt("Checks.Aim.c.Settings.AddHitsDefCounts");
    }

    public int AimCBlockedHitInterval() {
        return config.getInt("Checks.Aim.c.Settings.BlockedHitInterval");
    }

    public long AimCSpeedResetTime() {
        return config.getLong("Checks.Aim.c.Settings.SpeedResetTime");
    }

    public int AimCSpeedGiveAmount() {
        return config.getInt("Checks.Aim.c.Settings.SpeedGiveAmount");
    }

    public int AimCSpeedGiveReset() {
        return config.getInt("Checks.Aim.c.Settings.SpeedGiveReset");
    }
}