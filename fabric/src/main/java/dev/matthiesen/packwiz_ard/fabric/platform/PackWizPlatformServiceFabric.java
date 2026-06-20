package dev.matthiesen.packwiz_ard.fabric.platform;

import dev.matthiesen.packwiz_ard.common.platform.PackWizPlatformService;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public final class PackWizPlatformServiceFabric implements PackWizPlatformService {

    @Override
    public File getRootDir() {
        return FabricLoader.getInstance().getGameDir().toFile();
    }
}
