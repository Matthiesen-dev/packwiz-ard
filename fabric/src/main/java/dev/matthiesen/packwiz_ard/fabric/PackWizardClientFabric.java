package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardClientCommon;
import net.fabricmc.api.ClientModInitializer;

public final class PackWizardClientFabric implements ClientModInitializer {
    public static final PackWizardClientCommon INSTANCE = PackWizardClientCommon.INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE.createInfoLog("Loading for Fabric Mod Loader (Client)");
        INSTANCE.initialize();
    }
}
