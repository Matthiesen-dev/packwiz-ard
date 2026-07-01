package dev.matthiesen.packwiz_ard.common.webhook;

import dev.matthiesen.common.matthiesen_lib_api.core.discord.model.Embed;
import dev.matthiesen.common.matthiesen_lib_api.core.discord.model.EmbedBuilder;
import dev.matthiesen.common.matthiesen_lib_webhooks.MatthiesenLibWebhooks;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.interfaces.IWebhookService;

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

    public static Embed parseEventEmbed(WebhooksConfig.DiscordEmbed embed, Context context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
//        if (embed.title != null)
//            embedBuilder.withTitle(TextUtils.parse(embed.title, boost));
//        if (embed.description != null)
//            embedBuilder.withDescription(TextUtils.parse(embed.description, boost));
        if (embed.color != null)
            embedBuilder.withColor(embed.color);
        if (embed.timestamp != null)
            embedBuilder.withTimestamp(embed.timestamp);
        List<Embed.EmbedField> fields = new ArrayList<>();
        if (embed.fields != null) {
            for (WebhooksConfig.DiscordEmbedField field : embed.fields) {
                Embed.EmbedField embedField = new Embed.EmbedField();
//                if (field.name != null)
//                    embedField.setName(TextUtils.parse(field.name, boost));
//                if (field.value != null)
//                    embedField.setValue(TextUtils.parse(field.value, boost));
                embedField.setInline(field.inline);
                fields.add(embedField);
            }
            embedBuilder.withFields(fields);
        }
        if (embed.author != null) {
            Embed.Author author = new Embed.Author();
            if (embed.author.name != null) author.setName(embed.author.name);
            if (embed.author.icon_url != null) author.setIconUrl(embed.author.icon_url);
            if (embed.author.url != null) author.setUrl(embed.author.url);
            embedBuilder.withAuthor(author);
        }
        return embedBuilder.build();
    }

    @Override
    public void sendMessage(WebhooksConfig.DiscordEmbed embed, Context context) {
        if (webhooks == null) return;
        try {
            String userName = embed.author != null && embed.author.name != null
                    ? embed.author.name
                    : "PackWiz-ard";
            String avatarUrl = embed.author != null && embed.author.icon_url != null
                    ? embed.author.icon_url
                    : "https://raw.githubusercontent.com/Matthiesen-dev/.github/refs/heads/main/mod-logos/packwiz-ard.png";

            webhooks.sendMessage(message -> message
                    .withUsername(userName)
                    .withAvatarUrl(avatarUrl)
                    .withEmbeds(List.of(parseEventEmbed(embed, context)))
            );
        } catch (RuntimeException e) {
            PackWizardCommon.INSTANCE.createErrorLog("Failed to send Discord webhook message! Check your webhook URL and ensure that your server can connect to Discord's servers.", e);
        }
    }
}
