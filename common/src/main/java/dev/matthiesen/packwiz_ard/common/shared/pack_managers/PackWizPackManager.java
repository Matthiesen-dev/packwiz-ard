package dev.matthiesen.packwiz_ard.common.shared.pack_managers;

import com.moandjiezana.toml.Toml;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.Environment;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.PackStatus;
import dev.matthiesen.packwiz_ard.common.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.FailedHashMatchException;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.PackUrlException;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.ProcessExitCodeException;
import dev.matthiesen.packwiz_ard.common.PackWizardServerCommon;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.AsyncCommandTask;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IPackManager;
import dev.matthiesen.packwiz_ard.common.shared.util.HashedFileDownloader;
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

public final class PackWizPackManager implements IPackManager {
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

    public PackWizPackManager() {}

    @Override
    public String getUpdateTaskName() {
        return UPDATE_PACKWIZ_TASK_NAME;
    }

    @Override
    public String getConfiguredLink() {
        return PWConfig.COMMON_CONFIG.pack_toml.get();
    }

    @Override
    public void setConfiguredLink(String link) {
        PWConfig.COMMON_CONFIG.pack_toml.set(link == null ? "" : link);
        PWConfig.COMMON_CONFIG.pack_toml.save();
    }

    private void sendWebhook(PWConfig.DiscordEmbed embed) {
        if (embed != null && PackWizardServerCommon.INSTANCE.getWebhookService() != null) {
            PackWizardServerCommon.INSTANCE.getWebhookService().sendMessage(embed);
        }
    }

    @Override
    public PackStatus getPackStatus(String packLink) {
        String normalizedLink = packLink == null ? "" : packLink.trim();
        long checkedAt = System.currentTimeMillis();

        if (normalizedLink.isBlank()) {
            return new PackStatus(
                    PackStatus.State.UNCONFIGURED,
                    null,
                    "Set a pack.toml link to check for client updates.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }

        try {
            URL packTomlUrl = testPackLink(normalizedLink);
            String currentHash = getLatestPackHash(normalizedLink);
            String lastSeenHash = PWConfig.COMMON_CONFIG.lastSeenPackTomlHash.get();
            boolean updateAvailable = !currentHash.equals(lastSeenHash);

            return new PackStatus(
                    updateAvailable ? PackStatus.State.UPDATE_AVAILABLE : PackStatus.State.UP_TO_DATE,
                    packTomlUrl.toExternalForm(),
                    updateAvailable ? "A client update is available." : "The client pack.toml is valid and up to date.",
                    true,
                    updateAvailable,
                    false,
                    checkedAt
            );
        } catch (PackUrlException e) {
            String message = e.getMessage() == null ? "The pack.toml link could not be validated." : e.getMessage();
            PackStatus.State state = message.contains("valid URL")
                    ? PackStatus.State.INVALID_URL
                    : message.contains("valid TOML") || message.contains("invalid data")
                    ? PackStatus.State.INVALID_TOML
                    : PackStatus.State.ERROR;

            return new PackStatus(
                    state,
                    normalizedLink,
                    message,
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IOException e) {
            return new PackStatus(
                    PackStatus.State.UNREACHABLE,
                    normalizedLink,
                    "The pack.toml link could not be reached or read.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IllegalStateException e) {
            return new PackStatus(
                    PackStatus.State.INVALID_TOML,
                    normalizedLink,
                    "The pack.toml file contains invalid data.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }
    }

    @Override
    public String getLatestPackHash(String packLink) throws PackUrlException, IOException {
        URL packTomlUrl = testPackLink(packLink);
        var connection = packTomlUrl.openConnection();
        var toml = new Toml().read(connection.getInputStream());
        return toml.getString("index.hash");
    }

    @Override
    public void pollTasks() {
        var tasksIterator = PackWizPackManager.TASKS.listIterator();

        while (tasksIterator.hasNext()) {
            var task = tasksIterator.next();
            task.tick();

            if (task.pollFinished()) {
                Exception exception = null;
                Component message = null;

                try {
                    task.getFuture().join();

                    if (task.hasName(PackWizPackManager.UPDATE_PACKWIZ_TASK_NAME))
                        message = UPDATE_FINISHED;
                    else if (task.hasName(PackWizPackManager.BOOTSTRAP_TASK_NAME))
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

                    if (task.hasName(PackWizPackManager.UPDATE_PACKWIZ_TASK_NAME)) {
                        if (cause instanceof PackUrlException ptfe)
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

    @Override
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

                    testPackLink(packTomlLink);
                    String currentHash = getLatestPackHash(packTomlLink);

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

    @Override
    public @NotNull URL testPackLink(@NotNull final String packLink) throws PackUrlException {
        try {
            var url = URI.create(packLink).toURL();
            var connection = url.openConnection();
            var toml = new Toml().read(connection.getInputStream());

            if (!PACK_TOML_REQUIRED_KEYS.stream().allMatch(toml::contains)) {
                String requiredKeys = String.join(", ", PACK_TOML_REQUIRED_KEYS);
                throw new PackUrlException("The file does not contain all the required keys: " + requiredKeys);
            }
            return url;
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new PackUrlException("The link submitted is not a valid URL.");
        } catch (IOException e) {
            throw new PackUrlException("Check this file exists and is a valid TOML file.");
        } catch (IllegalStateException e) {
            throw new PackUrlException("The file contains invalid data.");
        }
    }

    @Override
    public boolean hasBootstrap() {
        return new File("packwiz-installer-bootstrap.jar").exists();
    }

    @Override
    public boolean isAsyncTaskRunning(String name) {
        return HAS_TASK.test(name);
    }
}
