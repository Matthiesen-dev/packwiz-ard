package dev.matthiesen.packwiz_ard.common;

import dev.matthiesen.common.matthiesen_lib_api.abstracts.AbstractCommonMod;
import dev.matthiesen.common.matthiesen_lib_api.config.ConfigManager;
import dev.matthiesen.common.matthiesen_lib_api.core.interfaces.MatthiesenLibServerEventHandler;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.packwiz_ard.common.commands.PackWizardCommand;
import dev.matthiesen.packwiz_ard.common.config.PackWizardConfig;
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

    private File GAME_DIR_FILE;

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
            createInfoLog("Failed to load a pack.toml file");
        }

        registerCommand(PackWizardCommand.CMD);
        registerServerEventHandler(getServerEventHandler());
        createInfoLog("Initialized");
    }

    public PackWizardConfig getConfig() {
        return CONFIG_MANAGER.getConfig();
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
            createInfoLog("Reloading configuration");
        };
    }

    public MatthiesenLibServerEventHandler getServerEventHandler() {
        return new MatthiesenLibServerEventHandler() {
            @Override
            public void onServerTick(MinecraftServer server) {
                PackWizardCommand.pollCommandStatus();
            }
        };
    }

    public void reloadConfigs() {
        CONFIG_MANAGER.loadConfig();
    }
}
