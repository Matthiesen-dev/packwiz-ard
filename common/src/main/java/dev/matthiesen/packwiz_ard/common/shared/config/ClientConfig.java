package dev.matthiesen.packwiz_ard.common.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {

    public ModConfigSpec.BooleanValue autoRefreshEnabled;
    public ModConfigSpec.IntValue autoRefreshIntervalSeconds;

    public ClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("Client Configuration").push("client");

        autoRefreshEnabled = builder.comment(
                        "Whether the client should periodically re-check the modpack source status while the client update screen is open",
                        "Set this to false if you prefer manual refresh only"
                )
                .define("auto_refresh_enabled", true);

        autoRefreshIntervalSeconds = builder.comment(
                        "How often the client should re-check the modpack source status while the update screen is open",
                        "This value is only used when auto_refresh_enabled is true"
                )
                .defineInRange("auto_refresh_interval_seconds", 60, 5, 3600);

        builder.pop();
    }
}
