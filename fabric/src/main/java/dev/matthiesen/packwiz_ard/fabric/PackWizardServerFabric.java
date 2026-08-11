package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.server.PackWizardServerCommon;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class PackWizardServerFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        PackWizardCommon.INSTANCE.createInfoLog("Loading for Fabric Mod Loader (Server)");
        PackWizardServerCommon.initialize();
    }
}
