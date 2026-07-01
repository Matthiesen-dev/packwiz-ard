package dev.matthiesen.packwiz_ard.common.interfaces;

import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;

public interface IWebhookService {
    void sendMessage(WebhooksConfig.DiscordEmbed embed, Context context);

    record Context() {}
}
