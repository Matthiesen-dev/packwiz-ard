package dev.matthiesen.packwiz_ard.common.shared.interfaces;

import dev.matthiesen.packwiz_ard.common.shared.exceptions.PackUrlException;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public interface IPackManager {
    PackStatus getPackStatus(String packLink);
    String getLatestPackHash(String packLink) throws PackUrlException, IOException;
    void pollTasks();
    String getUpdateTaskName();
    String getConfiguredLink();
    void setConfiguredLink(String link) throws IOException;
    URL testPackLink(@NotNull final String packLink) throws PackUrlException;
    boolean hasBootstrap();
    boolean isAsyncTaskRunning(String name);

    default boolean update(String packLink, boolean hasBootStrap, CommandSource output) {
        return update(packLink, hasBootStrap, output::sendSystemMessage);
    }

    default boolean update(String packLink, boolean hasBootStrap, Consumer<Component> messageSink) {
        return update(packLink, hasBootStrap, messageSink, () -> {}, throwable -> {});
    }

    boolean update(String packLink, boolean hasBootstrap, Consumer<Component> messageSink, Runnable onSuccess, Consumer<Throwable> onFailure);
}
