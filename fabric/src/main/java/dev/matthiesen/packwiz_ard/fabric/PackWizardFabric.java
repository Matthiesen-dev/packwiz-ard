package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.Constants;
import net.fabricmc.api.ModInitializer;

public class PackWizardFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.createInfoLog("Loading for Fabric Mod Loader");
        PackWizardCommon.initialize();
    }
}
