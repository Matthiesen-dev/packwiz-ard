package dev.matthiesen.packwiz_ard.common.shared.interfaces;

import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;

public interface IWebhookService {
    void sendMessage(PWConfig.DiscordEmbed embed);
}
