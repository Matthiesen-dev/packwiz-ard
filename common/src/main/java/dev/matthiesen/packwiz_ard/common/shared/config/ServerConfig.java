package dev.matthiesen.packwiz_ard.common.shared.config;

import dev.matthiesen.matthiesen_core.common.core.discord.DiscordColor;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {

    public ModConfigSpec.IntValue minimumPermissionLevel;
    public ModConfigSpec.BooleanValue autoUpdate;
    public ModConfigSpec.IntValue autoUpdateInterval;

    public ModConfigSpec.BooleanValue webhooks_enabled;
    public ModConfigSpec.ConfigValue<String> webhooks_url;
    public ModConfigSpec.ConfigValue<String> webhooks_authorName;
    public ModConfigSpec.ConfigValue<String> webhooks_authorIconUrl;

    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadTriggered_title;
    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadTriggered_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_bootstrap_downloadTriggered_color;
    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadFinished_title;
    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadFinished_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_bootstrap_downloadFinished_color;
    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadFailed_title;
    public ModConfigSpec.ConfigValue<String> webhooks_bootstrap_downloadFailed_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_bootstrap_downloadFailed_color;

    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateTriggered_title;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateTriggered_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_pack_updateTriggered_color;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateFinished_title;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateFinished_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_pack_updateFinished_color;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateFailed_title;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_updateFailed_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_pack_updateFailed_color;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_tomlLinkUpdated_title;
    public ModConfigSpec.ConfigValue<String> webhooks_pack_tomlLinkUpdated_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_pack_tomlLinkUpdated_color;

    public ModConfigSpec.ConfigValue<String> webhooks_minimumPermissionLevelUpdated_title;
    public ModConfigSpec.ConfigValue<String> webhooks_minimumPermissionLevelUpdated_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_minimumPermissionLevelUpdated_color;

    public ModConfigSpec.ConfigValue<String> webhooks_autoUpdateUpdated_title;
    public ModConfigSpec.ConfigValue<String> webhooks_autoUpdateUpdated_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_autoUpdateUpdated_color;

    public ModConfigSpec.ConfigValue<String> webhooks_autoUpdateIntervalUpdated_title;
    public ModConfigSpec.ConfigValue<String> webhooks_autoUpdateIntervalUpdated_description;
    public ModConfigSpec.EnumValue<DiscordColor> webhooks_autoUpdateIntervalUpdated_color;

    public ServerConfig(ModConfigSpec.Builder builder) {
        builder.comment("Server Configuration").push("server");

        minimumPermissionLevel = builder.comment("The minimum permission level required to use the mod's commands", "Permission Node: 'packwiz_ard.command.minimum_permission_level'")
                .defineInRange("minimumPermissionLevel", 4, 0, 4);
        autoUpdate = builder.comment("Enable or disable automatic updates")
                .define("autoUpdate", false);
        autoUpdateInterval = builder.comment("Interval in minutes for automatic updates")
                .defineInRange("autoUpdateInterval", 1440, 1, Integer.MAX_VALUE);

        builder.comment("Discord Webhooks Configuration").push("discordWebhooks");

        webhooks_enabled = builder.comment("Enable or disable Discord webhooks for server events")
                .define("enabled", false);
        webhooks_url = builder.comment("The Discord Webhook URL to send server event notifications to")
                .define("webhookUrl", "DISCORD_WEBHOOK_URL_HERE");
        webhooks_authorName = builder.comment("The name to display as the author of the Discord webhook messages")
                .define("discordAuthorName", "PackWiz ARD");
        webhooks_authorIconUrl = builder.comment("The URL of the icon to display as the author of the Discord webhook messages")
                .define("discordAuthorIconUrl", "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png");

        builder.comment("Webhook messages for various server events").push("webhookMessages");

        webhooks_bootstrap_downloadTriggered_title = builder.comment("Title for the bootstrap download triggered webhook message")
                .define("bootstrapDownloadTriggered_title", "Bootstrap Download Triggered!");
        webhooks_bootstrap_downloadTriggered_description = builder.comment("Description for the bootstrap download triggered webhook message")
                .define("bootstrapDownloadTriggered_description", "A new bootstrap download has been triggered, and the download process has started!");
        webhooks_bootstrap_downloadTriggered_color = builder.comment("Embed color for the bootstrap download triggered webhook message")
                .defineEnum("bootstrapDownloadTriggered_color", DiscordColor.GOLD);

        webhooks_bootstrap_downloadFinished_title = builder.comment("Title for the bootstrap download finished webhook message")
                .define("bootstrapDownloadFinished_title", "Bootstrap Download Finished!");
        webhooks_bootstrap_downloadFinished_description = builder.comment("Description for the bootstrap download finished webhook message")
                .define("bootstrapDownloadFinished_description", "The bootstrap download process has finished successfully! The update process will now continue.");
        webhooks_bootstrap_downloadFinished_color = builder.comment("Embed color for the bootstrap download finished webhook message")
                .defineEnum("bootstrapDownloadFinished_color", DiscordColor.GREEN);

        webhooks_bootstrap_downloadFailed_title = builder.comment("Title for the bootstrap download failed webhook message")
                .define("bootstrapDownloadFailed_title", "Bootstrap Download Failed!");
        webhooks_bootstrap_downloadFailed_description = builder.comment("Description for the bootstrap download failed webhook message")
                .define("bootstrapDownloadFailed_description", "The bootstrap download process has failed! Please check the logs for more information.");
        webhooks_bootstrap_downloadFailed_color = builder.comment("Embed color for the bootstrap download failed webhook message")
                .defineEnum("bootstrapDownloadFailed_color", DiscordColor.RED);

        webhooks_pack_updateTriggered_title = builder.comment("Title for the pack update triggered webhook message")
                .define("packUpdateTriggered_title", "Modpack Update Triggered!");
        webhooks_pack_updateTriggered_description = builder.comment("Description for the pack update triggered webhook message")
                .define("packUpdateTriggered_description", "A new modpack update has been triggered, and the update process has started!");
        webhooks_pack_updateTriggered_color = builder.comment("Embed color for the pack update triggered webhook message")
                .defineEnum("packUpdateTriggered_color", DiscordColor.GOLD);

        webhooks_pack_updateFinished_title = builder.comment("Title for the pack update finished webhook message")
                .define("packUpdateFinished_title", "Modpack Update Finished!");
        webhooks_pack_updateFinished_description = builder.comment("Description for the pack update finished webhook message")
                .define("packUpdateFinished_description", "The modpack update process has finished successfully! Server restart pending to apply update");
        webhooks_pack_updateFinished_color = builder.comment("Embed color for the pack update finished webhook message")
                .defineEnum("packUpdateFinished_color", DiscordColor.GREEN);

        webhooks_pack_updateFailed_title = builder.comment("Title for the pack update failed webhook message")
                .define("packUpdateFailed_title", "Modpack Update Failed!");
        webhooks_pack_updateFailed_description = builder.comment("Description for the pack update failed webhook message")
                .define("packUpdateFailed_description", "The modpack update process has failed! Please check the logs for more information.");
        webhooks_pack_updateFailed_color = builder.comment("Embed color for the pack update failed webhook message")
                .defineEnum("packUpdateFailed_color", DiscordColor.RED);

        webhooks_pack_tomlLinkUpdated_title = builder.comment("Title for the pack source updated webhook message")
                .define("packTomlLinkUpdated_title", "Pack Source Updated!");
        webhooks_pack_tomlLinkUpdated_description = builder.comment("Description for the pack source updated webhook message")
                .define("packTomlLinkUpdated_description", "The pack source has been updated! The new link is: %newLink%");
        webhooks_pack_tomlLinkUpdated_color = builder.comment("Embed color for the pack source updated webhook message")
                .defineEnum("packTomlLinkUpdated_color", DiscordColor.GOLD);

        webhooks_minimumPermissionLevelUpdated_title = builder.comment("Title for the minimum permission level updated webhook message")
                .define("minimumPermissionLevelUpdated_title", "Minimum Permission Level Updated!");
        webhooks_minimumPermissionLevelUpdated_description = builder.comment("Description for the minimum permission level updated webhook message")
                .define("minimumPermissionLevelUpdated_description", "The minimum permission level has been updated! The new level is: %newLevel%");
        webhooks_minimumPermissionLevelUpdated_color = builder.comment("Embed color for the minimum permission level updated webhook message")
                .defineEnum("minimumPermissionLevelUpdated_color", DiscordColor.GOLD);

        webhooks_autoUpdateUpdated_title = builder.comment("Title for the auto update updated webhook message")
                .define("autoUpdateUpdated_title", "Auto Update Updated!");
        webhooks_autoUpdateUpdated_description = builder.comment("Description for the auto update updated webhook message")
                .define("autoUpdateUpdated_description", "The auto update setting has been updated! The new setting is: %newSetting%");
        webhooks_autoUpdateUpdated_color = builder.comment("Embed color for the auto update updated webhook message")
                .defineEnum("autoUpdateUpdated_color", DiscordColor.GOLD);

        webhooks_autoUpdateIntervalUpdated_title = builder.comment("Title for the auto update interval updated webhook message")
                .define("autoUpdateIntervalUpdated_title", "Auto Update Interval Updated!");
        webhooks_autoUpdateIntervalUpdated_description = builder.comment("Description for the auto update interval updated webhook message")
                .define("autoUpdateIntervalUpdated_description", "The auto update interval has been updated! The new interval is: %newInterval% minutes");
        webhooks_autoUpdateIntervalUpdated_color = builder.comment("Embed color for the auto update interval updated webhook message")
                .defineEnum("autoUpdateIntervalUpdated_color", DiscordColor.GOLD);

        builder.pop(); // Closes "server.discordWebhooks.webhookMessages"

        builder.pop(); // Closes "server.discordWebhooks"

        builder.pop(); // Closes "server"
    }
}
