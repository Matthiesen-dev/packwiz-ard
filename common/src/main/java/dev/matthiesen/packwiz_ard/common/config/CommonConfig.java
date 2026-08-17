package dev.matthiesen.packwiz_ard.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {

    public ModConfigSpec.EnumValue<UPDATER> updater;

    public ModConfigSpec.ConfigValue<String> pack_toml;
    public ModConfigSpec.ConfigValue<String> lastSeenPackTomlHash;

    public CommonConfig(ModConfigSpec.Builder builder) {
        builder.comment("Common config").push("common");

        updater = builder.comment(
                        "The updater to use for checking for updates and downloading the pack source",
                        "PACKWIZ is the default updater and uses the pack_toml config value",
                        "PACKWEAVE reads the pack source from modpack.url in the game directory root"
                )
                .defineEnum("updater", UPDATER.PACKWIZ);

        pack_toml = builder.comment(
                        "The URL to the pack.toml file for this pack (used when using the PACKWIZ updater)",
                        "PACKWEAVE ignores this value and instead reads modpack.url from the game directory root",
                        "If this is not set, PackWiz will not be able to check for updates or download the pack source"
                )
                .define("pack_toml", "");
        lastSeenPackTomlHash = builder.comment(
                        "The last seen hash of the active pack source",
                        "This is used to detect whether the server or client pack has changed"
                )
                .define("lastSeenPackTomlHash", "");

        builder.pop();
    }

    public enum UPDATER {
        PACKWIZ,
        PACKWEAVE
    }
}
