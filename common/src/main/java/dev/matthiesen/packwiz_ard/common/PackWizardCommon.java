package dev.matthiesen.packwiz_ard.common;

import dev.matthiesen.common.matthiesen_lib_api.abstracts.AbstractCommonMod;
import dev.matthiesen.common.matthiesen_lib_api.config.ConfigManager;
import dev.matthiesen.common.matthiesen_lib_api.core.interfaces.MatthiesenLibServerEventHandler;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.packwiz_ard.common.commands.PackWizardCommand;
import dev.matthiesen.packwiz_ard.common.config.PackWizardConfig;
import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.platform.PackWizPlatformService;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ServiceLoader;

public final class PackWizardCommon extends AbstractCommonMod {
    public static final String MOD_ID = "packwiz_ard";
    public static final String MOD_NAME = "PackWiz-ard";
    private static @Token final String METRICS_TOKEN = "19918d00a0af78c1d5f2b78f1e2807e0";
    public static final PackWizardCommon INSTANCE = new PackWizardCommon();
    public static final PackManager PACK_MANAGER = new PackManager();

    private static final ConfigManager<PackWizardConfig> CONFIG_MANAGER =
            INSTANCE.createConfigManager(PackWizardConfig.class, "config");
    private static final ConfigManager<WebhooksConfig> WEBHOOKS_CONFIG =
            INSTANCE.createConfigManager(WebhooksConfig.class, "webhooks");

    private File GAME_DIR_FILE;
    private long autoUpdateTicks = 0L;
    private boolean warnedInvalidAutoUpdateInterval = false;

    private static final PackWizPlatformService PLATFORM_SERVICE =
            ServiceLoader.load(PackWizPlatformService.class).findFirst().orElseThrow();

    public PackWizardCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public void initialize() {
        super.initialize();
        reload().run();

        var packToml = getConfig().pack_toml;

        if (packToml == null || packToml.isEmpty()) {
            getLogger().warn("Failed to load a pack.toml file from config");
        }

        registerCommand(PackWizardCommand.CMD);
        registerServerEventHandler(getServerEventHandler());
        createInfoLog("Initialized");
    }

    public PackWizardConfig getConfig() {
        return CONFIG_MANAGER.getConfig();
    }

    public WebhooksConfig getWebhooksConfig() {
        return WEBHOOKS_CONFIG.getConfig();
    }

    public ConfigManager<PackWizardConfig> getConfigManager() {
        return CONFIG_MANAGER;
    }

    public File getGameDir() {
        if (GAME_DIR_FILE == null) {
            GAME_DIR_FILE = PLATFORM_SERVICE.getRootDir();
        }
        return GAME_DIR_FILE;
    }

    @Override
    public @Token @NotNull String getMetricsToken() {
        return METRICS_TOKEN;
    }

    @Override
    public Runnable reload() {
        return () -> {
            reloadConfigs();
            resetAutoUpdateSchedule();
            createInfoLog("Reloading configuration");
        };
    }

    public MatthiesenLibServerEventHandler getServerEventHandler() {
        return new MatthiesenLibServerEventHandler() {
            @Override
            public void onServerTick(MinecraftServer server) {
                PackWizardCommand.pollCommandStatus();
                tickAutoUpdate(server);
            }
        };
    }

    public void resetAutoUpdateSchedule() {
        autoUpdateTicks = 0L;
    }

    public long getAutoUpdateTicks() {
        return autoUpdateTicks;
    }

    private void tickAutoUpdate(MinecraftServer server) {
        var config = getConfig();

        if (!config.auto_update) {
            resetAutoUpdateSchedule();
            warnedInvalidAutoUpdateInterval = false;
            return;
        }

        var packToml = config.pack_toml;
        if (packToml == null || packToml.isBlank() || !packToml.contains("pack.toml")) {
            resetAutoUpdateSchedule();
            return;
        }

        int intervalMinutes = config.auto_update_interval_minutes;
        if (intervalMinutes <= 0) {
            if (!warnedInvalidAutoUpdateInterval) {
                getLogger().warn("Auto update is enabled, but auto_update_interval_minutes is not positive. Skipping automatic updates.");
                warnedInvalidAutoUpdateInterval = true;
            }
            resetAutoUpdateSchedule();
            return;
        }

        warnedInvalidAutoUpdateInterval = false;

        long intervalTicks = (long) intervalMinutes * 1_200L;
        autoUpdateTicks++;

        if (autoUpdateTicks < intervalTicks) {
            return;
        }

        if (PACK_MANAGER.isAsyncTaskRunning(PackManager.UPDATE_PACKWIZ_TASK_NAME)) {
            return;
        }

        createInfoLog("Automatic Packwiz update triggered after " + intervalMinutes + " minute(s).");

        boolean started = PACK_MANAGER.update(packToml, PACK_MANAGER.hasBootstrap(), server);
        if (started) {
            resetAutoUpdateSchedule();
        }
    }

    public void reloadConfigs() {
        CONFIG_MANAGER.loadConfig();
        WEBHOOKS_CONFIG.loadConfig();
    }
}
