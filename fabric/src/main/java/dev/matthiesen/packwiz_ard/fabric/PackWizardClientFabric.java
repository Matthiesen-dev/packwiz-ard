package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.client.PackWizardClientCommon;
import net.fabricmc.api.ClientModInitializer;

public final class PackWizardClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var INSTANCE = PackWizardClientCommon.INSTANCE;

        INSTANCE.createInfoLog("Loading for Fabric Mod Loader (Client)");
        INSTANCE.initialize();
    }
}
