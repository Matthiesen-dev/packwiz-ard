package dev.matthiesen.packwiz_ard.common.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {

    public ModConfigSpec.ConfigValue<String> pack_toml;

    public CommonConfig(ModConfigSpec.Builder builder) {
        builder.comment("Common config").push("common");

        pack_toml = builder.comment(
                        "The URL to the pack.toml file for this pack",
                        "This is used to check for updates and download the pack.toml file",
                        "If this is not set, the mod will not be able to check for updates or download the pack.toml file"
                )
                .define("pack_toml", "");

        builder.pop();
    }
}
