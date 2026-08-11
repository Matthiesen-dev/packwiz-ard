package dev.matthiesen.packwiz_ard.neoforge;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.neoforged.fml.common.Mod;

@Mod(PackWizardCommon.MOD_ID)
public final class PackWizardNeoForge {
    public static final PackWizardCommon INSTANCE = PackWizardCommon.INSTANCE;

    public PackWizardNeoForge() {
        INSTANCE.createInfoLog("Loading for NeoForge Mod Loader");
        INSTANCE.initialize();
    }
}
