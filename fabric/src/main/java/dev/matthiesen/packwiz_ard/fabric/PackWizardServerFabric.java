package dev.matthiesen.packwiz_ard.fabric;

import dev.matthiesen.packwiz_ard.common.PackWizardServerCommon;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class PackWizardServerFabric implements DedicatedServerModInitializer {
    public static final PackWizardServerCommon INSTANCE = PackWizardServerCommon.INSTANCE;

    @Override
    public void onInitializeServer() {
        PackWizardFabric.INSTANCE.createInfoLog("Loading for Fabric Mod Loader (Server)");
        INSTANCE.initialize();
    }
}
