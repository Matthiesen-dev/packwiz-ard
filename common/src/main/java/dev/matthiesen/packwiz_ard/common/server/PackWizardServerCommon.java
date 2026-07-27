package dev.matthiesen.packwiz_ard.common.server;

import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.config.PackWizardConfig;
import dev.matthiesen.packwiz_ard.common.server.commands.PackWizardCommand;
import dev.matthiesen.packwiz_ard.common.server.webhook.DiscordWebhookService;
import dev.matthiesen.packwiz_ard.common.server.webhook.NoOpWebhookService;
import dev.matthiesen.packwiz_ard.common.shared.PackManager;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IWebhookService;
import net.minecraft.server.MinecraftServer;

public final class PackWizardServerCommon {
    private static long autoUpdateTicks = 0L;
    private static boolean warnedInvalidAutoUpdateInterval = false;
    private static final PackManager PACK_MANAGER = PackWizardCommon.PACK_MANAGER;
    private static IWebhookService discordWebhookService;

    public static void initialize() {
        PackWizardCommon.INSTANCE.getCommandsRegistryManager().registerCommand(PackWizardCommand.CMD);

        PlatformEvents.SERVER_END_TICK.subscribe(event -> {
            PackWizardCommand.pollCommandStatus();
            tickAutoUpdate(event.server());
        });

        if (PackWizardCommon.INSTANCE.getCommonUtils().isModLoaded("matthiesen_lib_webhooks")) {
            discordWebhookService = new DiscordWebhookService();
        } else {
            discordWebhookService = new NoOpWebhookService();
        }
    }

    public static IWebhookService getWebhookService() {
        return discordWebhookService;
    }

    public static void resetAutoUpdateSchedule() {
        autoUpdateTicks = 0L;
    }

    public static long getAutoUpdateTicks() {
        return autoUpdateTicks;
    }

    public static PackWizardConfig getConfig() {
        return PackWizardCommon.INSTANCE.getConfig();
    }

    private static void tickAutoUpdate(MinecraftServer server) {
        var config = getConfig();

        if (!config.auto_update) {
            resetAutoUpdateSchedule();
            warnedInvalidAutoUpdateInterval = false;
            return;
        }

        var packToml = config.pack_toml;
        if (packToml == null || packToml.isBlank() || !packToml.contains("pack.toml")) {
            resetAutoUpdateSchedule();
            return;
        }

        int intervalMinutes = config.auto_update_interval_minutes;
        if (intervalMinutes <= 0) {
            if (!warnedInvalidAutoUpdateInterval) {
                PackWizardCommon.INSTANCE.getLogger().warn("Auto update is enabled, but auto_update_interval_minutes is not positive. Skipping automatic updates.");
                warnedInvalidAutoUpdateInterval = true;
            }
            resetAutoUpdateSchedule();
            return;
        }

        warnedInvalidAutoUpdateInterval = false;

        long intervalTicks = (long) intervalMinutes * 1_200L;
        autoUpdateTicks++;

        if (autoUpdateTicks < intervalTicks) {
            return;
        }

        if (PACK_MANAGER.isAsyncTaskRunning(PackManager.UPDATE_PACKWIZ_TASK_NAME)) {
            return;
        }

        PackWizardCommon.INSTANCE.createInfoLog("Automatic Packwiz update triggered after " + intervalMinutes + " minute(s).");

        boolean started = PACK_MANAGER.update(packToml, PACK_MANAGER.hasBootstrap(), server);
        if (started) {
            resetAutoUpdateSchedule();
        }
    }
}
