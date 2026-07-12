package dev.matthiesen.packwiz_ard.neoforge;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.server.PackWizardServerCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = PackWizardCommon.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class PackWizardServerNeoForge {
    public PackWizardServerNeoForge() {
        PackWizardCommon.INSTANCE.createInfoLog("Loading for NeoForge Mod Loader (Server)");
        PackWizardServerCommon.initialize();
    }
}
