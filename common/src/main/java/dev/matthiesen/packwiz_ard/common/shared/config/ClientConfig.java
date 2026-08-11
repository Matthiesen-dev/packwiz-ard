package dev.matthiesen.packwiz_ard.common.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {

    public ClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("Client Configuration", "THIS CONFIGURATION FILE IS NOT YET IMPLEMENTED").push("client");
        builder.pop();
    }
}
