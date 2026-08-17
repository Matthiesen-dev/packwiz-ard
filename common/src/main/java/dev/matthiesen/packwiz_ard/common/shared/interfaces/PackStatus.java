package dev.matthiesen.packwiz_ard.common.shared.interfaces;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public record PackStatus(
        @NotNull State state,
        @Nullable String packLink,
        @NotNull String message,
        boolean valid,
        boolean updateAvailable,
        boolean restartRequired,
        long checkedAtMillis
) {
    public enum State {
        UNCONFIGURED,
        CHECKING,
        INVALID_URL,
        INVALID_TOML,
        UNREACHABLE,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        UPDATING,
        RESTART_REQUIRED,
        ERROR
    }

    public boolean canUpdate() {
        return valid && updateAvailable && !restartRequired && state != State.UPDATING;
    }

    public boolean isBusy() {
        return state == State.CHECKING || state == State.UPDATING;
    }
}


