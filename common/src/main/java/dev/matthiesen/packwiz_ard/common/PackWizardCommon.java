package dev.matthiesen.packwiz_ard.common;

import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import dev.matthiesen.packwiz_ard.common.shared.PackweavePackManager;
import dev.matthiesen.packwiz_ard.common.shared.config.CommonConfig;
import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.shared.PackWizPackManager;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IPackManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class PackWizardCommon extends AbstractCommonMod {
    public static final String MOD_ID = "packwiz_ard";
    public static final String MOD_NAME = "PackWiz-ard";
    private static @Token final String METRICS_TOKEN = "19918d00a0af78c1d5f2b78f1e2807e0";
    public static final PackWizardCommon INSTANCE = new PackWizardCommon();

    public static volatile IPackManager PACK_MANAGER;

    private File GAME_DIR_FILE;

    public PackWizardCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public void initialize() {
        super.initialize();

        registerModConfig(MOD_ID, ModConfigType.STARTUP, PWConfig.COMMON_SPEC, "packwiz_ard/common.toml");
        registerModConfig(MOD_ID, ModConfigType.SERVER, PWConfig.SERVER_SPEC, "packwiz_ard/server.toml");
        registerModConfig(MOD_ID, ModConfigType.CLIENT, PWConfig.CLIENT_SPEC, "packwiz_ard/client.toml");

        CommonConfig.UPDATER updater = PWConfig.COMMON_CONFIG.updater.get();
        switch (updater) {
            case PACKWIZ -> PACK_MANAGER = new PackWizPackManager();
            case PACKWEAVE -> PACK_MANAGER = new PackweavePackManager();
            default ->  throw new IllegalStateException("Unexpected value: " + updater);
        }

        createInfoLog("Initialized");
    }

    public File getGameDir() {
        if (GAME_DIR_FILE == null) {
            GAME_DIR_FILE = getCommonUtils().getGameDirectory().toFile();
        }
        return GAME_DIR_FILE;
    }

    @Override
    public @Token @NotNull String getMetricsToken() {
        return METRICS_TOKEN;
    }
}
