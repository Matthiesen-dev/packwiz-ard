package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.fabricmc.api.ModInitializer;

public class PackWizardFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PackWizardCommon.INSTANCE.createInfoLog("Loading for Fabric Mod Loader");
        PackWizardCommon.INSTANCE.initialize();
    }
}
