package dev.matthiesen.packwiz_ard.common;

import dev.matthiesen.common.matthiesen_lib_api.abstracts.AbstractCommonMod;
import dev.matthiesen.libs.faststats.Token;
import org.jetbrains.annotations.Nullable;

public class PackWizardCommon extends AbstractCommonMod {
    public static final String MOD_ID = "packwiz_ard";
    private static final String MOD_NAME = "PackWiz-ard";
    private static @Token final String METRICS_TOKEN = "19918d00a0af78c1d5f2b78f1e2807e0";
    public static final PackWizardCommon INSTANCE = new PackWizardCommon();

    public PackWizardCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public @Token @Nullable String getMetricsToken() {
        return METRICS_TOKEN;
    }

    @Override
    public Runnable reload() {
        return () -> {
            reloadConfigs();
            createInfoLog("Reloading configuration");
        };
    }

    public void reloadConfigs() {}

    @Override
    public void initialize() {
        super.initialize();
        reload().run();

        createInfoLog("Initialized");
    }
}
