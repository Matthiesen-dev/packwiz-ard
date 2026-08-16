package dev.matthiesen.packwiz_ard.common.client;

import dev.matthiesen.matthiesen_core.common.AbstractCommonClientMod;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class PackWizardClientCommon extends AbstractCommonClientMod {
    public static final PackWizardClientCommon INSTANCE = new PackWizardClientCommon();

    private final AtomicReference<PackStatus> currentStatus = new AtomicReference<>(new PackStatus(
            PackStatus.State.UNCONFIGURED,
            null,
            "Set a modpack source to check for client updates.",
            false,
            false,
            false,
            System.currentTimeMillis()
    ));
    private final AtomicBoolean validationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);

    private PackWizardClientCommon() {
        super(PackWizardCommon.INSTANCE);
    }

    @Override
    public void initialize() {
        createInfoLog("Loading for Client");
        refreshStatus();
    }

    public void tick(Minecraft minecraft) {
        PackWizardCommon.PACK_MANAGER.pollTasks();

        if (minecraft == null || minecraft.screen == null || hasRestartRequired()) {
            return;
        }

        if (!(minecraft.screen instanceof TitleScreen) && !(minecraft.screen instanceof PackWizardClientScreen)) {
            return;
        }

        if (!PWConfig.CLIENT_CONFIG.autoRefreshEnabled.get() || isValidationInProgress() || isUpdateInProgress()) {
            return;
        }

        int intervalSeconds = PWConfig.CLIENT_CONFIG.autoRefreshIntervalSeconds.get();
        long intervalMillis = Math.max(1, intervalSeconds) * 1000L;
        long lastChecked = currentStatus.get().checkedAtMillis();

        if (System.currentTimeMillis() - lastChecked >= intervalMillis) {
            refreshStatus();
        }
    }

    public PackStatus getStatus() {
        return currentStatus.get();
    }

    public boolean hasRestartRequired() {
        return currentStatus.get().restartRequired();
    }

    public boolean isValidationInProgress() {
        return validationInProgress.get();
    }

    public boolean isUpdateInProgress() {
        return updateInProgress.get();
    }

    public void refreshStatus() {
        if (hasRestartRequired() || isUpdateInProgress()) {
            return;
        }

        String packTomlLink = getPackTomlLink();
        if (packTomlLink == null || packTomlLink.isBlank()) {
            currentStatus.set(new PackStatus(
                    PackStatus.State.UNCONFIGURED,
                    null,
                    "Set a modpack source to check for client updates.",
                    false,
                    false,
                    false,
                    System.currentTimeMillis()
            ));
            return;
        }

        if (!validationInProgress.compareAndSet(false, true)) {
            return;
        }

        currentStatus.set(new PackStatus(
                PackStatus.State.CHECKING,
                packTomlLink,
                "Checking modpack source status...",
                false,
                false,
                false,
                System.currentTimeMillis()
        ));

        CompletableFuture.runAsync(() -> {
            try {
                PackStatus status = PackWizardCommon.PACK_MANAGER.getPackStatus(packTomlLink);
                currentStatus.set(status);
            } catch (Exception e) {
                PackWizardCommon.INSTANCE.createErrorLog("Failed to validate the client modpack source", e);
                currentStatus.set(new PackStatus(
                        PackStatus.State.ERROR,
                        packTomlLink,
                        e.getMessage() == null ? "The modpack source could not be validated." : e.getMessage(),
                        false,
                        false,
                        false,
                        System.currentTimeMillis()
                ));
            } finally {
                validationInProgress.set(false);
            }
        });
    }

    public void startUpdate() {
        if (hasRestartRequired() || isUpdateInProgress()) {
            return;
        }

        PackStatus status = currentStatus.get();
        if (!status.canUpdate()) {
            return;
        }

        String packTomlLink = getPackTomlLink();
        if (packTomlLink == null || packTomlLink.isBlank()) {
            currentStatus.set(new PackStatus(
                    PackStatus.State.UNCONFIGURED,
                    null,
                    "Set a modpack source to check for client updates.",
                    false,
                    false,
                    false,
                    System.currentTimeMillis()
            ));
            return;
        }

        if (!updateInProgress.compareAndSet(false, true)) {
            return;
        }

        currentStatus.set(new PackStatus(
                PackStatus.State.UPDATING,
                packTomlLink,
                "Updating the client pack source... Restart will be required after the update completes.",
                true,
                true,
                false,
                System.currentTimeMillis()
        ));

        boolean started = PackWizardCommon.PACK_MANAGER.update(
                packTomlLink,
                PackWizardCommon.PACK_MANAGER.hasBootstrap(),
                component -> { },
                this::markUpdateFinished,
                throwable -> markUpdateFailed(throwable.getMessage() == null ? "The client update failed." : throwable.getMessage())
        );

        if (!started) {
            updateInProgress.set(false);
            currentStatus.set(new PackStatus(
                    status.state(),
                    packTomlLink,
                        "A client update is already in progress.",
                    status.valid(),
                    status.updateAvailable(),
                    status.restartRequired(),
                    System.currentTimeMillis()
            ));
        }
    }


    public String getPackTomlLink() {
        return PackWizardCommon.PACK_MANAGER.getConfiguredLink();
    }

    public void markUpdateFailed(String message) {
        updateInProgress.set(false);
        String packTomlLink = getPackTomlLink();
        PackWizardCommon.INSTANCE.createErrorLog("The client update failed: " + message);
        currentStatus.set(new PackStatus(
                PackStatus.State.ERROR,
                packTomlLink == null || packTomlLink.isBlank() ? null : packTomlLink,
                message == null ? "The client update failed." : message,
                false,
                false,
                false,
                System.currentTimeMillis()
        ));
    }

    public void markUpdateFinished() {
        updateInProgress.set(false);
        String packTomlLink = getPackTomlLink();
        currentStatus.set(new PackStatus(
                PackStatus.State.RESTART_REQUIRED,
                packTomlLink == null || packTomlLink.isBlank() ? null : packTomlLink,
                "The client update completed successfully. Restart the game to load the new files.",
                true,
                false,
                true,
                System.currentTimeMillis()
        ));
    }
}
