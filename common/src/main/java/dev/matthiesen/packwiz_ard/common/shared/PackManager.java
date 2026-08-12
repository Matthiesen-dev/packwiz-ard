package dev.matthiesen.packwiz_ard.common.shared;

import com.moandjiezana.toml.Toml;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.Environment;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.client.PackTomlStatus;
import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.FailedHashMatchException;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.PackTomlUrlException;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.ProcessExitCodeException;
import dev.matthiesen.packwiz_ard.common.server.PackWizardServerCommon;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.AsyncCommandTask;
import dev.matthiesen.packwiz_ard.common.shared.util.HashedFileDownloader;
import net.minecraft.commands.CommandSource;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Consumer;

public final class PackManager {
    public static final String BOOTSTRAP_URL = "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar";
    public static final String BOOTSTRAP_HASH = "a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c";
    public static final String BOOTSTRAP_TASK_NAME = "downloadBootstrap";
    public static final String UPDATE_PACKWIZ_TASK_NAME = "updatePackwiz";

    private static final Component UPDATE_FINISHED = Component.literal("Packwiz has finished updating. Restart for changes to take effect.");
    private static final Component BOOTSTRAP_DOWNLOAD_FINISHED = Component.literal("Bootstrap downloaded successfully.");

    private static final List<String> PACKWIZ_COMMAND_PREFIX = List.of("java", "-jar", "packwiz-installer-bootstrap.jar");
    private static final Set<String> PACK_TOML_REQUIRED_KEYS = Set.of("name", "version", "index");

    public static final LinkedList<AsyncCommandTask> TASKS = new LinkedList<>();
    private static final Predicate<String> HAS_TASK = name -> TASKS.stream().anyMatch((task) -> task.hasName(name));

    public PackManager() {}

    private void sendWebhook(PWConfig.DiscordEmbed embed) {
        if (embed != null && PackWizardServerCommon.getWebhookService() != null) {
            PackWizardServerCommon.getWebhookService().sendMessage(embed);
        }
    }

    public PackTomlStatus getPackTomlStatus(String packTomlLink) {
        String normalizedLink = packTomlLink == null ? "" : packTomlLink.trim();
        long checkedAt = System.currentTimeMillis();

        if (normalizedLink.isBlank()) {
            return new PackTomlStatus(
                    PackTomlStatus.State.UNCONFIGURED,
                    null,
                    "Set a pack.toml link to check for client updates.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }

        try {
            URL packTomlUrl = testPackTomlLink(normalizedLink);
            String currentHash = getLatestPackTomlHash(normalizedLink);
            String lastSeenHash = PWConfig.COMMON_CONFIG.lastSeenPackTomlHash.get();
            boolean updateAvailable = !currentHash.equals(lastSeenHash);

            return new PackTomlStatus(
                    updateAvailable ? PackTomlStatus.State.UPDATE_AVAILABLE : PackTomlStatus.State.UP_TO_DATE,
                    packTomlUrl.toExternalForm(),
                    updateAvailable ? "A client update is available." : "The client pack.toml is valid and up to date.",
                    true,
                    updateAvailable,
                    false,
                    checkedAt
            );
        } catch (PackTomlUrlException e) {
            String message = e.getMessage() == null ? "The pack.toml link could not be validated." : e.getMessage();
            PackTomlStatus.State state = message.contains("valid URL")
                    ? PackTomlStatus.State.INVALID_URL
                    : message.contains("valid TOML") || message.contains("invalid data")
                    ? PackTomlStatus.State.INVALID_TOML
                    : PackTomlStatus.State.ERROR;

            return new PackTomlStatus(
                    state,
                    normalizedLink,
                    message,
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IOException e) {
            return new PackTomlStatus(
                    PackTomlStatus.State.UNREACHABLE,
                    normalizedLink,
                    "The pack.toml link could not be reached or read.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IllegalStateException e) {
            return new PackTomlStatus(
                    PackTomlStatus.State.INVALID_TOML,
                    normalizedLink,
                    "The pack.toml file contains invalid data.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }
    }

    public String getLatestPackTomlHash(String packTomlLink) throws PackTomlUrlException, IOException {
        URL packTomlUrl = testPackTomlLink(packTomlLink);
        var connection = packTomlUrl.openConnection();
        var toml = new Toml().read(connection.getInputStream());
        return toml.getString("index.hash");
    }

    public void pollTasks() {
        var tasksIterator = PackManager.TASKS.listIterator();

        while (tasksIterator.hasNext()) {
            var task = tasksIterator.next();
            task.tick();

            if (task.pollFinished()) {
                Exception exception = null;
                Component message = null;

                try {
                    task.getFuture().join();

                    if (task.hasName(PackManager.UPDATE_PACKWIZ_TASK_NAME))
                        message = UPDATE_FINISHED;
                    else if (task.hasName(PackManager.BOOTSTRAP_TASK_NAME))
                        message = BOOTSTRAP_DOWNLOAD_FINISHED;
                } catch (CompletionException e) {
                    exception = e;
                    Throwable cause = e.getCause() == null ? e : e.getCause();

                    if (cause instanceof RuntimeException runtimeException && runtimeException.getCause() != null) {
                        cause = runtimeException.getCause();
                    }

                    if (cause instanceof InterruptedException)
                        message = Component.literal("Process was interrupted. Check the console for details.");
                    else if (cause instanceof IOException)
                        message = Component.literal("Read/write process failed. Check the console for details.");

                    if (task.hasName(PackManager.UPDATE_PACKWIZ_TASK_NAME)) {
                        if (cause instanceof PackTomlUrlException ptfe)
                            message = Component.literal(ptfe.getMessage());
                        else if (cause instanceof ProcessExitCodeException pece)
                            message = Component.literal(pece.getMessage());
                        else if (cause instanceof FailedHashMatchException fhme)
                            message = Component.literal(fhme.getMessage());
                    }
                    if (message == null) message = Component.literal("Command failed. Check the console for errors.");
                }
                task.sendMessage(message);
                if (exception != null)
                    PackWizardCommon.INSTANCE.createErrorLog("Unexpected exception occurred whilst polling Packwiz command status", exception);
                tasksIterator.remove();
            }
        }
    }

    public boolean update(String packTomlLink, boolean hasBootstrap, CommandSource output) {
        return update(packTomlLink, hasBootstrap, output::sendSystemMessage);
    }

    public boolean update(String packTomlLink, boolean hasBootstrap, Consumer<Component> messageSink) {
        return update(packTomlLink, hasBootstrap, messageSink, () -> {}, throwable -> {});
    }

    public boolean update(String packTomlLink, boolean hasBootstrap, Consumer<Component> messageSink, Runnable onSuccess, Consumer<Throwable> onFailure) {
        List<String> command = new ArrayList<>(PACKWIZ_COMMAND_PREFIX);
        boolean isDedicatedServer = PackWizardCommon.INSTANCE.getCommonUtils().getEnvironment() == Environment.SERVER;

        if (isDedicatedServer)
            command.addAll(List.of("-g", "-s", "server"));
        command.add(packTomlLink);

        if (!HAS_TASK.test(UPDATE_PACKWIZ_TASK_NAME)) {
            TASKS.add(new AsyncCommandTask(CompletableFuture.runAsync(() -> {
                try {
                    sendWebhook(PWConfig.getPackUpdateTriggeredEmbed());

                    if (!hasBootstrap) {
                        sendWebhook(PWConfig.getBootstrapDownloadTriggeredEmbed());

                        var bootstrapPath = Path.of(PackWizardCommon.INSTANCE.getGameDir() + "/packwiz-installer-bootstrap.jar");
                        var downloader = new HashedFileDownloader(BOOTSTRAP_URL, BOOTSTRAP_HASH, bootstrapPath);

                        try {
                            downloader.download();
                            if (!downloader.hashesMatch()) {
                                var bootstrapFile = bootstrapPath.toFile();

                                if (bootstrapFile.exists()) {
                                    if (!bootstrapFile.delete()) {
                                        throw new IOException("Cannot verify the integrity of downloaded file 'packwiz-installer-bootstrap.jar'" +
                                                "Please delete this file manually from your main server directory and replace with the correct file" +
                                                "from https://github.com/packwiz/packwiz-installer-bootstrap/releases.");
                                    }
                                }
                                throw new FailedHashMatchException();
                            }
                            sendWebhook(PWConfig.getBootstrapDownloadFinishedEmbed());
                        } catch (Exception bootstrapException) {
                            sendWebhook(PWConfig.getBootstrapDownloadFailedEmbed());
                            throw bootstrapException;
                        }
                    }

                    testPackTomlLink(packTomlLink);
                    String currentHash = getLatestPackTomlHash(packTomlLink);

                    var process = new ProcessBuilder(command).inheritIO().start();
                    try (var bufferedReader = process.inputReader()) {
                        bufferedReader.lines().forEach(PackWizardCommon.INSTANCE::createInfoLog);
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0)
                        throw new ProcessExitCodeException("Process failed with exit code: " + exitCode);

                    PWConfig.setPackTomlHash(currentHash);
                    onSuccess.run();
                    sendWebhook(PWConfig.getPackUpdateFinishedEmbed());
                } catch (Exception e) {
                    onFailure.accept(e);
                    sendWebhook(PWConfig.getPackUpdateFailedEmbed());
                    throw new RuntimeException(e);
                }
            }), UPDATE_PACKWIZ_TASK_NAME, 10, messageSink));
            return true;
        }

        return false;
    }

    public @NotNull URL testPackTomlLink(@NotNull final String packTomlLink) throws PackTomlUrlException {
        try {
            var url = URI.create(packTomlLink).toURL();
            var connection = url.openConnection();
            var toml = new Toml().read(connection.getInputStream());

            if (!PACK_TOML_REQUIRED_KEYS.stream().allMatch(toml::contains)) {
                String requiredKeys = String.join(", ", PACK_TOML_REQUIRED_KEYS);
                throw new PackTomlUrlException("The file does not contain all the required keys: " + requiredKeys);
            }
            return url;
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new PackTomlUrlException("The link submitted is not a valid URL.");
        } catch (IOException e) {
            throw new PackTomlUrlException("Check this file exists and is a valid TOML file.");
        } catch (IllegalStateException e) {
            throw new PackTomlUrlException("The file contains invalid data.");
        }
    }

    public boolean hasBootstrap() {
        return new File("packwiz-installer-bootstrap.jar").exists();
    }

    public boolean isAsyncTaskRunning(String name) {
        return HAS_TASK.test(name);
    }
}
