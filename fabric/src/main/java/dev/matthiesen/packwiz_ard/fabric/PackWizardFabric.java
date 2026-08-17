package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.fabricmc.api.ModInitializer;

public final class PackWizardFabric implements ModInitializer {
    public static final PackWizardCommon INSTANCE = PackWizardCommon.INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE.createInfoLog("Loading for Fabric Mod Loader");
        INSTANCE.initialize();
    }
}
