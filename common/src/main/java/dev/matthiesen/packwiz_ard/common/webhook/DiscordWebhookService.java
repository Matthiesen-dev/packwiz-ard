package dev.matthiesen.packwiz_ard.common.webhook;

import dev.matthiesen.common.matthiesen_lib_api.core.discord.model.Embed;
import dev.matthiesen.common.matthiesen_lib_api.core.discord.model.EmbedBuilder;
import dev.matthiesen.common.matthiesen_lib_webhooks.MatthiesenLibWebhooks;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.interfaces.IWebhookService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DiscordWebhookService implements IWebhookService {
    private static MatthiesenLibWebhooks.Webhooks webhooks;

    public DiscordWebhookService() {
        webhooks = getClient();
        PackWizardCommon.INSTANCE.createInfoLog("Matthiesen Lib Webhooks detected, using it for Discord Webhook integration");
    }

    public MatthiesenLibWebhooks.Webhooks getClient() {
        if (!PackWizardCommon.INSTANCE.getWebhooksConfig().enabled) return null;
        if (!PackWizardCommon.INSTANCE.getWebhooksConfig().webhookUrl.startsWith("https://")) {
            PackWizardCommon.INSTANCE.getLogger().error("Discord webhooks are enabled but an invalid Discord Webhook URL is set! Please check your configuration. (Must start with 'https://')");
            return null;
        }
        return new MatthiesenLibWebhooks.Webhooks(PackWizardCommon.INSTANCE.getWebhooksConfig().webhookUrl);
    }

    public static String getCurrentTimestamp() {
        return Instant.now().toString();
    }

    public static Embed parseEventEmbed(WebhooksConfig baseConfig, WebhooksConfig.DiscordEmbed embed) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        if (embed.title != null)
            embedBuilder.withTitle(embed.title);
        if (embed.description != null)
            embedBuilder.withDescription(embed.description);
        if (embed.color != null)
            embedBuilder.withColor(embed.color);
        if (embed.timestamp != null)
            embedBuilder.withTimestamp(embed.timestamp.replace("%timestamp%", getCurrentTimestamp()));
        List<Embed.EmbedField> fields = new ArrayList<>();
        if (embed.fields != null) {
            for (WebhooksConfig.DiscordEmbedField field : embed.fields) {
                Embed.EmbedField embedField = new Embed.EmbedField();
                if (field.name != null)
                    embedField.setName(field.name);
                if (field.value != null)
                    embedField.setValue(field.value);
                embedField.setInline(field.inline);
                fields.add(embedField);
            }
            embedBuilder.withFields(fields);
        }
        String userName = baseConfig.discordAuthorName != null
                ? baseConfig.discordAuthorName
                : "PackWiz-ard";
        String avatarUrl = baseConfig.discordAuthorIconUrl != null
                ? baseConfig.discordAuthorIconUrl
                : "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";
        Embed.Author author = new Embed.Author();
        author.setName(userName);
        author.setIconUrl(avatarUrl);
        embedBuilder.withAuthor(author);
        return embedBuilder.build();
    }

    @Override
    public void sendMessage(WebhooksConfig.DiscordEmbed embed) {
        if (webhooks == null) return;
        var baseConfig = PackWizardCommon.INSTANCE.getWebhooksConfig();
        try {
            String userName = baseConfig.discordAuthorName != null
                    ? baseConfig.discordAuthorName
                    : "PackWiz-ard";
            String avatarUrl = baseConfig.discordAuthorIconUrl != null
                    ? baseConfig.discordAuthorIconUrl
                    : "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";

            webhooks.sendMessage(message -> message
                    .withUsername(userName)
                    .withAvatarUrl(avatarUrl)
                    .withEmbeds(List.of(parseEventEmbed(baseConfig, embed)))
            );
        } catch (RuntimeException e) {
            PackWizardCommon.INSTANCE.createErrorLog("Failed to send Discord webhook message! Check your webhook URL and ensure that your server can connect to Discord's servers.", e);
        }
    }
}
