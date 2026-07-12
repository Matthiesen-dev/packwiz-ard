package dev.matthiesen.packwiz_ard.common.server.webhook;

import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IWebhookService;

public final class NoOpWebhookService implements IWebhookService {
    public NoOpWebhookService() {
        PackWizardCommon.INSTANCE.createInfoLog("Matthiesen Lib Webhooks not detected, using no-op implementation for Discord Webhook integration");
    }

    @Override
    public void sendMessage(WebhooksConfig.DiscordEmbed embed) {
        // No operation performed, as this is a no-op implementation.
    }
}
