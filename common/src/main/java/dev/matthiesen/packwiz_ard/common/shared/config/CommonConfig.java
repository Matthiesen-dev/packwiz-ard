package dev.matthiesen.packwiz_ard.common.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {

    public ModConfigSpec.ConfigValue<String> pack_toml;
    public ModConfigSpec.ConfigValue<String> lastSeenPackTomlHash;

    public CommonConfig(ModConfigSpec.Builder builder) {
        builder.comment("Common config").push("common");

        pack_toml = builder.comment(
                        "The URL to the pack.toml file for this pack",
                        "This is used to check for updates and download the pack.toml file on both the server and client",
                        "If this is not set, the mod will not be able to check for updates or download the pack.toml file"
                )
                .define("pack_toml", "");
        lastSeenPackTomlHash = builder.comment(
                        "The last seen hash of the pack.toml file",
                        "This is used to detect whether the server or client pack has changed"
                )
                .define("lastSeenPackTomlHash", "");

        builder.pop();
    }
}
