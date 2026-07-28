package dev.matthiesen.packwiz_ard.neoforge;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.client.PackWizardClientCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = PackWizardCommon.MOD_ID, dist = Dist.CLIENT)
public final class PackWizardClientNeoForge {
    public static final PackWizardClientCommon INSTANCE = PackWizardClientCommon.INSTANCE;

    public PackWizardClientNeoForge() {
        INSTANCE.createInfoLog("Loading for NeoForge Mod Loader (Client)");
        INSTANCE.initialize();
    }
}
