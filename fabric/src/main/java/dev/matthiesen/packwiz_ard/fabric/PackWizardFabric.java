package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.fabricmc.api.ModInitializer;

public final class PackWizardFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        var INSTANCE = PackWizardCommon.INSTANCE;

        INSTANCE.createInfoLog("Loading for Fabric Mod Loader");
        INSTANCE.initialize();
    }
}
