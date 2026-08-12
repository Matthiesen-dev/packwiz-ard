package dev.matthiesen.packwiz_ard.common.shared.config;

import com.google.gson.annotations.SerializedName;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public final class PWConfig {
    public static final CommonConfig COMMON_CONFIG;
    public static final ModConfigSpec COMMON_SPEC;

    public static final ServerConfig SERVER_CONFIG;
    public static final ModConfigSpec SERVER_SPEC;

    public static final ClientConfig CLIENT_CONFIG;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_CONFIG = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();

        Pair<ServerConfig, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_CONFIG = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();

        Pair<ClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_CONFIG = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    public static void setPackTomlHash(String hash) {
        COMMON_CONFIG.lastSeenPackTomlHash.set(hash);
        COMMON_CONFIG.lastSeenPackTomlHash.save();
    }

    public static DiscordEmbed getBootstrapDownloadTriggeredEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_bootstrap_downloadTriggered_title.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadTriggered_description.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadTriggered_color.get().getValue()
        );
    }

    public static DiscordEmbed getBootstrapDownloadFinishedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_bootstrap_downloadFinished_title.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadFinished_description.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadFinished_color.get().getValue()
        );
    }

    public static DiscordEmbed getBootstrapDownloadFailedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_bootstrap_downloadFailed_title.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadFailed_description.get(),
                SERVER_CONFIG.webhooks_bootstrap_downloadFailed_color.get().getValue()
        );
    }

    public static DiscordEmbed getPackUpdateTriggeredEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_pack_updateTriggered_title.get(),
                SERVER_CONFIG.webhooks_pack_updateTriggered_description.get(),
                SERVER_CONFIG.webhooks_pack_updateTriggered_color.get().getValue()
        );
    }

    public static DiscordEmbed getPackUpdateFinishedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_pack_updateFinished_title.get(),
                SERVER_CONFIG.webhooks_pack_updateFinished_description.get(),
                SERVER_CONFIG.webhooks_pack_updateFinished_color.get().getValue()
        );
    }

    public static DiscordEmbed getPackUpdateFailedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_pack_updateFailed_title.get(),
                SERVER_CONFIG.webhooks_pack_updateFailed_description.get(),
                SERVER_CONFIG.webhooks_pack_updateFailed_color.get().getValue()
        );
    }

    public static DiscordEmbed getPackTomlLinkUpdatedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_pack_tomlLinkUpdated_title.get(),
                SERVER_CONFIG.webhooks_pack_tomlLinkUpdated_description.get(),
                SERVER_CONFIG.webhooks_pack_tomlLinkUpdated_color.get().getValue()
        );
    }

    public static DiscordEmbed getAutoUpdateUpdatedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_autoUpdateUpdated_title.get(),
                SERVER_CONFIG.webhooks_autoUpdateUpdated_description.get(),
                SERVER_CONFIG.webhooks_autoUpdateUpdated_color.get().getValue()
        );
    }

    public static DiscordEmbed getAutoUpdateIntervalUpdatedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_autoUpdateIntervalUpdated_title.get(),
                SERVER_CONFIG.webhooks_autoUpdateIntervalUpdated_description.get(),
                SERVER_CONFIG.webhooks_autoUpdateIntervalUpdated_color.get().getValue()
        );
    }

    public static DiscordEmbed getMinimumPermissionLevelUpdatedEmbed() {
        return DiscordEmbed.create(
                SERVER_CONFIG.webhooks_minimumPermissionLevelUpdated_title.get(),
                SERVER_CONFIG.webhooks_minimumPermissionLevelUpdated_description.get(),
                SERVER_CONFIG.webhooks_minimumPermissionLevelUpdated_color.get().getValue()
        );
    }

    public static class DiscordEmbed {
        public String title;
        public String description;
        public Integer color;
        public List<DiscordEmbedField> fields = List.of();
        public String timestamp = "%timestamp%";

        public static DiscordEmbed create(
                String title,
                String description,
                Integer color
        ) {
            DiscordEmbed embed = new DiscordEmbed();
            embed.title = title;
            embed.description = description;
            embed.color = color;
            return embed;
        }

        public static DiscordEmbed create(
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

        public static DiscordEmbedField create(String name, String value, boolean inline) {
            DiscordEmbedField field = new DiscordEmbedField();
            field.name = name;
            field.value = value;
            field.inline = inline;
            return field;
        }
    }
}
