package dev.matthiesen.packwiz_ard.common;

import com.moandjiezana.toml.Toml;
import dev.matthiesen.common.matthiesen_lib_api.MatthiesenLibApi;
import dev.matthiesen.common.matthiesen_lib_api.core.platform.MatthiesenLibPlatform;
import dev.matthiesen.packwiz_ard.common.exceptions.FailedHashMatchException;
import dev.matthiesen.packwiz_ard.common.exceptions.PackTomlUrlException;
import dev.matthiesen.packwiz_ard.common.exceptions.ProcessExitCodeException;
import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.interfaces.AsyncCommandTask;
import dev.matthiesen.packwiz_ard.common.util.HashedFileDownloader;
import net.minecraft.commands.CommandSource;
import org.jetbrains.annotations.NotNull;

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
import java.util.function.Predicate;

public final class PackManager {
    public static final String BOOTSTRAP_URL = "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar";
    public static final String BOOTSTRAP_HASH = "a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c";
    public static final String BOOTSTRAP_TASK_NAME = "downloadBootstrap";
    public static final String UPDATE_PACKWIZ_TASK_NAME = "updatePackwiz";

    private static final List<String> PACKWIZ_COMMAND_PREFIX = List.of("java", "-jar", "packwiz-installer-bootstrap.jar");
    private static final Set<String> PACK_TOML_REQUIRED_KEYS = Set.of("name", "version", "index");

    public static final LinkedList<AsyncCommandTask> TASKS = new LinkedList<>();
    private static final Predicate<String> HAS_TASK = name -> TASKS.stream().anyMatch((task) -> task.hasName(name));

    public PackManager() {}

    private void sendWebhook(WebhooksConfig.DiscordEmbed embed) {
        if (embed != null) {
            PackWizardCommon.INSTANCE.getWebhookService().sendMessage(embed);
        }
    }

    private WebhooksConfig.WebhookMessages getWebhookMessages() {
        var config = PackWizardCommon.INSTANCE.getWebhooksConfig();
        return config != null ? config.webhooks : null;
    }

    public boolean update(String packTomlLink, boolean hasBootstrap, CommandSource output) {
        List<String> command = new ArrayList<>(PACKWIZ_COMMAND_PREFIX);
        boolean isDedicatedServer = MatthiesenLibApi.getEnvironmentType() == MatthiesenLibPlatform.ENVIRONMENT.SERVER;

        if (isDedicatedServer)
            command.addAll(List.of("-g", "-s", "server"));
        command.add(packTomlLink);

        if (!HAS_TASK.test(UPDATE_PACKWIZ_TASK_NAME)) {
            TASKS.add(new AsyncCommandTask(CompletableFuture.runAsync(() -> {
                var webhooks = getWebhookMessages();

                try {
                    sendWebhook(webhooks != null ? webhooks.packUpdateTriggered : null);

                    if (!hasBootstrap) {
                        sendWebhook(webhooks != null ? webhooks.bootstrapDownloadTriggered : null);

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
                            sendWebhook(webhooks != null ? webhooks.bootstrapDownloadFinished : null);
                        } catch (Exception bootstrapException) {
                            sendWebhook(webhooks != null ? webhooks.bootstrapDownloadFailed : null);
                            throw bootstrapException;
                        }
                    }

                    testPackTomlLink(packTomlLink);

                    var process = new ProcessBuilder(command).inheritIO().start();
                    try (var bufferedReader = process.inputReader()) {
                        bufferedReader.lines().forEach(PackWizardCommon.INSTANCE::createInfoLog);
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0)
                        throw new ProcessExitCodeException("Process failed with exit code: " + exitCode);

                    sendWebhook(webhooks != null ? webhooks.packUpdateFinished : null);
                } catch (Exception e) {
                    sendWebhook(webhooks != null ? webhooks.packUpdateFailed : null);
                    throw new RuntimeException(e);
                }
            }), UPDATE_PACKWIZ_TASK_NAME, 10, output));
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
