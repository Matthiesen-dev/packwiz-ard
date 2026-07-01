package dev.matthiesen.packwiz_ard.common.config;

import com.google.gson.annotations.SerializedName;
import dev.matthiesen.common.matthiesen_lib_api.core.discord.DiscordColor;

import java.util.List;

public class WebhooksConfig {
    @SerializedName("enabled")
    public boolean enabled = false;

    @SerializedName("webhookUrl")
    public String webhookUrl = "DISCORD_WEBHOOK_URL_HERE";

    @SerializedName("discordAuthorName")
    public String discordAuthorName = "PackWiz-ard";

    @SerializedName("discordAuthorIconUrl")
    public String discordAuthorIconUrl = "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";

    @SerializedName("webhooks")
    public WebhookMessages webhooks = new WebhookMessages();

    public static class WebhookMessages {
        @SerializedName("bootstrapDownloadTriggered")
        public DiscordEmbed bootstrapDownloadTriggered = new DiscordEmbed().create(
                "Bootstrap Download Triggered!",
                "A new bootstrap download has been triggered, and the download process has started!",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("bootstrapDownloadFinished")
        public DiscordEmbed bootstrapDownloadFinished = new DiscordEmbed().create(
                "Bootstrap Download Finished!",
                "The bootstrap download process has finished successfully! The update process will now continue.",
                DiscordColor.GREEN.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("bootstrapDownloadFailed")
        public DiscordEmbed bootstrapDownloadFailed = new DiscordEmbed().create(
                "Bootstrap Download Failed!",
                "The bootstrap download process has failed! Please check the logs for more information.",
                DiscordColor.RED.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("packUpdateTriggered")
        public DiscordEmbed packUpdateTriggered = new DiscordEmbed().create(
                "Modpack Update Triggered!",
                "A new modpack update has been triggered, and the update process has started!",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("packUpdateFinished")
        public DiscordEmbed packUpdateFinished = new DiscordEmbed().create(
                "Modpack Update Finished!",
                "The modpack update process has finished successfully! Server restart pending to apply update",
                DiscordColor.GREEN.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("packUpdateFailed")
        public DiscordEmbed packUpdateFailed = new DiscordEmbed().create(
                "Modpack Update Failed!",
                "The modpack update process has failed! Please check the logs for more information.",
                DiscordColor.RED.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("packTomlLinkUpdated")
        public DiscordEmbed packTomlLinkUpdated = new DiscordEmbed().create(
                "Pack TOML Link Updated",
                "The configured pack.toml link was updated.",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("minimumPermissionLevelUpdated")
        public DiscordEmbed minimumPermissionLevelUpdated = new DiscordEmbed().create(
                "Minimum Permission Level Updated",
                "The minimum permission level for /packwizard was updated.",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("autoUpdateUpdated")
        public DiscordEmbed autoUpdateUpdated = new DiscordEmbed().create(
                "Auto Update Setting Updated",
                "The auto update enabled setting was updated.",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );

        @SerializedName("autoUpdateIntervalUpdated")
        public DiscordEmbed autoUpdateIntervalUpdated = new DiscordEmbed().create(
                "Auto Update Interval Updated",
                "The automatic update interval was updated.",
                DiscordColor.GOLD.getValue(),
                List.of(),
                "%timestamp%"
        );
    }

    public static class DiscordEmbed {
        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("color")
        public Integer color;

        @SerializedName("fields")
        public List<DiscordEmbedField> fields;

        @SerializedName("timestamp")
        public String timestamp;

        public DiscordEmbed create(
                String title,
                String description,
                Integer color,
                List<DiscordEmbedField> fields,
                String timestamp
        ) {
            DiscordEmbed embed = new DiscordEmbed();
            embed.title = title;
            embed.description = description;
            embed.color = color;
            embed.fields = fields;
            embed.timestamp = timestamp;
            return embed;
        }
    }

    public static class DiscordEmbedField {
        @SerializedName("name")
        public String name;

        @SerializedName("value")
        public String value;

        @SerializedName("inline")
        public boolean inline;

        public DiscordEmbedField create(String name, String value, boolean inline) {
            DiscordEmbedField field = new DiscordEmbedField();
            field.name = name;
            field.value = value;
            field.inline = inline;
            return field;
        }
    }
}
