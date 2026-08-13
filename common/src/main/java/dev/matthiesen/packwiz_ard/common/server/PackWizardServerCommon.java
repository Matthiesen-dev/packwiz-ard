package dev.matthiesen.packwiz_ard.common.server;

import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
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

    private static boolean isServerRunning = false;

    public static void initialize() {
        PackWizardCommon.INSTANCE.getCommandsRegistryManager().registerCommand(PackWizardCommand.CMD);

        PlatformEvents.SERVER_STARTED.subscribe(event -> {
            var packToml = PWConfig.COMMON_CONFIG.pack_toml.get();

            if (packToml == null || packToml.isEmpty()) {
                PackWizardCommon.INSTANCE.createWarnLog("Failed to load a pack.toml file from config");
            }

            isServerRunning = true;
        });

        PlatformEvents.SERVER_END_TICK.subscribe(event -> {
            if (isServerRunning) {
                PACK_MANAGER.pollTasks();
                tickAutoUpdate(event.server());
            }
        });

        if (PackWizardCommon.INSTANCE.getCommonUtils().isModLoaded("matthiesen_core_webhooks")) {
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

    private static void tickAutoUpdate(MinecraftServer server) {
        if (!PWConfig.SERVER_CONFIG.autoUpdate.getAsBoolean()) {
            resetAutoUpdateSchedule();
            warnedInvalidAutoUpdateInterval = false;
            return;
        }

        var packToml = PWConfig.COMMON_CONFIG.pack_toml.get();
        if (packToml == null || packToml.isBlank() || !packToml.contains("pack.toml")) {
            resetAutoUpdateSchedule();
            return;
        }

        int intervalMinutes = PWConfig.SERVER_CONFIG.autoUpdateInterval.getAsInt();
        if (intervalMinutes <= 0) {
            if (!warnedInvalidAutoUpdateInterval) {
                PackWizardCommon.INSTANCE.createWarnLog("Auto update is enabled, but auto_update_interval_minutes is not positive. Skipping automatic updates.");
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
