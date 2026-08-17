package dev.matthiesen.packwiz_ard.common;

import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.packwiz_ard.common.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.server.commands.PackWizardCommand;
import dev.matthiesen.packwiz_ard.common.server.webhook.DiscordWebhookService;
import dev.matthiesen.packwiz_ard.common.server.webhook.NoOpWebhookService;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IWebhookService;
import net.minecraft.server.MinecraftServer;

public final class PackWizardServerCommon {
    public static final PackWizardServerCommon INSTANCE = new PackWizardServerCommon();

    private PackWizardServerCommon() {}

    private long autoUpdateTicks = 0L;
    private boolean warnedInvalidAutoUpdateInterval = false;
    private volatile IWebhookService discordWebhookService;
    private boolean isServerRunning = false;

    public void initialize() {
        PackWizardCommon.INSTANCE.getCommandsRegistryManager().registerCommand(PackWizardCommand.CMD);

        PlatformEvents.SERVER_STARTED.subscribe(event -> {
            var packToml = PackWizardCommon.PACK_MANAGER.getConfiguredLink();

            if (packToml == null || packToml.isEmpty()) {
                PackWizardCommon.INSTANCE.createWarnLog("Failed to load a modpack source from the configured updater");
            }

            isServerRunning = true;
        });

        PlatformEvents.SERVER_END_TICK.subscribe(event -> {
            if (isServerRunning) {
                PackWizardCommon.PACK_MANAGER.pollTasks();
                INSTANCE.tickAutoUpdate(event.server());
            }
        });

        if (PackWizardCommon.INSTANCE.getCommonUtils().isModLoaded("matthiesen_core_webhooks")) {
            discordWebhookService = new DiscordWebhookService();
        } else {
            discordWebhookService = new NoOpWebhookService();
        }
    }

    public IWebhookService getWebhookService() {
        return discordWebhookService;
    }

    public void resetAutoUpdateSchedule() {
        autoUpdateTicks = 0L;
    }

    public long getAutoUpdateTicks() {
        return autoUpdateTicks;
    }

    private void tickAutoUpdate(MinecraftServer server) {
        if (!PWConfig.SERVER_CONFIG.autoUpdate.getAsBoolean()) {
            resetAutoUpdateSchedule();
            warnedInvalidAutoUpdateInterval = false;
            return;
        }

        var packToml = PackWizardCommon.PACK_MANAGER.getConfiguredLink();
        if (packToml == null || packToml.isBlank()) {
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

        if (PackWizardCommon.PACK_MANAGER.isAsyncTaskRunning(PackWizardCommon.PACK_MANAGER.getUpdateTaskName())) {
            return;
        }

        PackWizardCommon.INSTANCE.createInfoLog("Automatic modpack update triggered after " + intervalMinutes + " minute(s).");

        boolean started = PackWizardCommon.PACK_MANAGER.update(packToml, PackWizardCommon.PACK_MANAGER.hasBootstrap(), server);
        if (started) {
            resetAutoUpdateSchedule();
        }
    }
}
