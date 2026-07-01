package dev.matthiesen.packwiz_ard.common.config;

import com.google.gson.annotations.SerializedName;

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

    public static class DiscordEmbed {
        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("color")
        public Integer color;

        @SerializedName("author")
        public DiscordAuthor author;

        @SerializedName("fields")
        public List<DiscordEmbedField> fields;

        @SerializedName("timestamp")
        public String timestamp;

        public DiscordEmbed create(
                String title,
                String description,
                Integer color,
                DiscordAuthor author,
                List<DiscordEmbedField> fields,
                String timestamp
        ) {
            DiscordEmbed embed = new DiscordEmbed();
            embed.title = title;
            embed.description = description;
            embed.color = color;
            embed.author = author;
            embed.fields = fields;
            embed.timestamp = timestamp;
            return embed;
        }
    }

    public static class DiscordAuthor {
        @SerializedName("name")
        public String name;

        @SerializedName("url")
        public String url;

        @SerializedName("icon_url")
        public String icon_url;

        public DiscordAuthor create(String name, String url, String icon_url) {
            DiscordAuthor author = new DiscordAuthor();
            author.name = name;
            author.url = url;
            author.icon_url = icon_url;
            return author;
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
