package dev.matthiesen.packwiz_ard.common.shared.interfaces;

import dev.matthiesen.packwiz_ard.common.shared.config.WebhooksConfig;

public interface IWebhookService {
    void sendMessage(WebhooksConfig.DiscordEmbed embed);
}
