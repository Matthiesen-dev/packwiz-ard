package dev.matthiesen.packwiz_ard.neoforge;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.neoforged.fml.common.Mod;

@Mod(PackWizardCommon.MOD_ID)
public final class PackWizardNeoForge {
    public PackWizardNeoForge() {
        PackWizardCommon.INSTANCE.createInfoLog("Loading for NeoForge Mod Loader");
        PackWizardCommon.INSTANCE.initialize();
    }
}
