package dev.matthiesen.packwiz_ard.neoforge;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.Constants;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class PackWizardNeoForge {
    public PackWizardNeoForge() {
        Constants.createInfoLog("Loading for NeoForge Mod Loader");
        PackWizardCommon.initialize();
    }
}
