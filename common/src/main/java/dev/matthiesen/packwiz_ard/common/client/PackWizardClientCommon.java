package dev.matthiesen.packwiz_ard.common.client;

import dev.matthiesen.matthiesen_core.common.AbstractCommonClientMod;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;

public final class PackWizardClientCommon extends AbstractCommonClientMod {
    public static final PackWizardClientCommon INSTANCE = new PackWizardClientCommon();

    private PackWizardClientCommon() {
        super(PackWizardCommon.INSTANCE);
    }

    @Override
    public void initialize() {
        createInfoLog("Loading for Client");
    }
}
