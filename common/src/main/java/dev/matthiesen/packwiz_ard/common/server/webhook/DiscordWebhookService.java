package dev.matthiesen.packwiz_ard.common.server.webhook;

import dev.matthiesen.matthiesen_core.common.api.discord.WebhookNotifierInstance;
import dev.matthiesen.matthiesen_core.common.api.discord.WebhookNotifierService;
import dev.matthiesen.matthiesen_core.common.api.exceptions.DiscordWebhookException;
import dev.matthiesen.matthiesen_core.common.core.discord.model.Embed;
import dev.matthiesen.matthiesen_core.common.core.discord.model.EmbedBuilder;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IWebhookService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DiscordWebhookService implements IWebhookService {
    private static WebhookNotifierInstance WEBHOOK_INSTANCE;

    public DiscordWebhookService() {
        WebhookNotifierService service = getService();
        if (service != null) {
            WEBHOOK_INSTANCE = service.makeInstance(PWConfig.SERVER_CONFIG.webhooks_url.get());
            PackWizardCommon.INSTANCE.createInfoLog("Matthiesen Lib Webhooks detected, using it for Discord Webhook integration");
        }
    }

    public WebhookNotifierService getService() {
        if (!PWConfig.SERVER_CONFIG.webhooks_enabled.getAsBoolean()) return null;
        if (!PWConfig.SERVER_CONFIG.webhooks_url.get().startsWith("https://")) {
            PackWizardCommon.INSTANCE.getLogger().error("Discord webhooks are enabled but an invalid Discord Webhook URL is set! Please check your configuration. (Must start with 'https://')");
            return null;
        }
        if (!PackWizardCommon.INSTANCE.getWebhookService().isAvailable()) return null;
        return PackWizardCommon.INSTANCE.getWebhookService();
    }

    public static String getCurrentTimestamp() {
        return Instant.now().toString();
    }

    public static Embed parseEventEmbed(PWConfig.DiscordEmbed embed) {
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
            for (PWConfig.DiscordEmbedField field : embed.fields) {
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
        String userName = PWConfig.SERVER_CONFIG.webhooks_authorName.get() != null
                ? PWConfig.SERVER_CONFIG.webhooks_authorName.get()
                : "PackWiz-ard";
        String avatarUrl = PWConfig.SERVER_CONFIG.webhooks_authorIconUrl.get() != null
                ? PWConfig.SERVER_CONFIG.webhooks_authorIconUrl.get()
                : "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";
        Embed.Author author = new Embed.Author();
        author.setName(userName);
        author.setIconUrl(avatarUrl);
        embedBuilder.withAuthor(author);
        return embedBuilder.build();
    }

    @Override
    public void sendMessage(PWConfig.DiscordEmbed embed) {
        if (WEBHOOK_INSTANCE == null) return;
        try {
            String userName = PWConfig.SERVER_CONFIG.webhooks_authorName.get() != null
                    ? PWConfig.SERVER_CONFIG.webhooks_authorName.get()
                    : "PackWiz-ard";
            String avatarUrl = PWConfig.SERVER_CONFIG.webhooks_authorIconUrl.get() != null
                    ? PWConfig.SERVER_CONFIG.webhooks_authorIconUrl.get()
                    : "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";

            WEBHOOK_INSTANCE.sendMessage(message -> message
                    .withUsername(userName)
                    .withAvatarUrl(avatarUrl)
                    .withEmbeds(List.of(parseEventEmbed(embed)))
            );
        } catch (RuntimeException | DiscordWebhookException e) {
            PackWizardCommon.INSTANCE.createErrorLog("Failed to send Discord webhook message! Check your webhook URL and ensure that your server can connect to Discord's servers.", e);
        }
    }
}
