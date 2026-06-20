package dev.matthiesen.packwiz_ard.neoforge.platform;

import dev.matthiesen.packwiz_ard.common.platform.PackWizPlatformService;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;

public final class PackWizPlatformServiceNeoForge implements PackWizPlatformService {

    @Override
    public File getRootDir() {
        return FMLPaths.GAMEDIR.get().toFile();
    }
}
