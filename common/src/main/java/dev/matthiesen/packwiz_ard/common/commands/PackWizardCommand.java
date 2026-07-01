package dev.matthiesen.packwiz_ard.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.matthiesen.common.matthiesen_lib_api.command.AbstractCommand;
import dev.matthiesen.common.matthiesen_lib_api.utility.ChatTableBuilder;
import dev.matthiesen.common.matthiesen_lib_api.utility.CommandBuilder;
import dev.matthiesen.packwiz_ard.common.PackManager;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.config.WebhooksConfig;
import dev.matthiesen.packwiz_ard.common.exceptions.FailedHashMatchException;
import dev.matthiesen.packwiz_ard.common.exceptions.PackTomlUrlException;
import dev.matthiesen.packwiz_ard.common.exceptions.ProcessExitCodeException;
import dev.matthiesen.packwiz_ard.common.util.Helpers;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public final class PackWizardCommand extends AbstractCommand {
    private static final Component UPDATE_START = Component.literal("Updating modpack. This may take a while...").withStyle(ChatFormatting.GRAY);
    private static final Component UPDATE_START_NO_BOOTSTRAP = Component.literal("Downloading the Packwiz Bootstrap and updating the modpack. This may take a while...").withStyle(ChatFormatting.GRAY);
    private static final Component UPDATE_FINISHED = Component.literal("Packwiz has finished updating. Restart for changes to take effect.").withStyle(ChatFormatting.GREEN);
    private static final Component BOOTSTRAP_DOWNLOAD_FINISHED = Component.literal("Bootstrap downloaded successfully.");
    private static final Component UPDATED_TOML_LINK = Component.literal("Successfully linked a Packwiz modpack. Use /packwizard update for the changes to take effect.").withStyle(ChatFormatting.GREEN);
    private static final Component COMMAND_FAILED = Component.literal("Command failed. Check the console for errors.").withStyle(ChatFormatting.RED);
    private static final Component PROCESS_INTERRUPTED = Component.literal("Process was interrupted. Check the console for details.").withStyle(ChatFormatting.RED);
    private static final Component FILE_HANDLING_ERROR = Component.literal("Read/write process failed. Check the console for details.").withStyle(ChatFormatting.RED);
    private static final Component SET_MIN_PERMISSION_LEVEL = Component.literal("Set minimum permission level required to use the /packwizard command").withStyle(ChatFormatting.GREEN);
    private static final Component SET_AUTO_UPDATE_ENABLED = Component.literal("Enabled automatic scheduled updates.").withStyle(ChatFormatting.GREEN);
    private static final Component SET_AUTO_UPDATE_DISABLED = Component.literal("Disabled automatic scheduled updates.").withStyle(ChatFormatting.GREEN);

    public static final PackWizardCommand CMD = new PackWizardCommand();

    private static WebhooksConfig.WebhookMessages getWebhookMessages() {
        var config = PackWizardCommon.INSTANCE.getWebhooksConfig();
        return config != null ? config.webhooks : null;
    }

    private static WebhooksConfig.DiscordEmbedField field(String name, String value) {
        return new WebhooksConfig.DiscordEmbedField().create(name, value, false);
    }

    private static void sendConfigWebhook(WebhooksConfig.DiscordEmbed template, List<WebhooksConfig.DiscordEmbedField> extraFields) {
        if (template == null) {
            return;
        }

        List<WebhooksConfig.DiscordEmbedField> fields = new ArrayList<>();
        if (template.fields != null) {
            fields.addAll(template.fields);
        }
        fields.addAll(extraFields);

        var embed = new WebhooksConfig.DiscordEmbed().create(
                template.title,
                template.description,
                template.color,
                fields,
                template.timestamp
        );
        PackWizardCommon.INSTANCE.getWebhookService().sendMessage(embed);
    }

    private static final ChatTableBuilder.Formatting PackWizFormatting = new ChatTableBuilder.Formatting(
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.AQUA,
            ChatFormatting.DARK_GRAY,
            ChatFormatting.YELLOW,
            ChatFormatting.GRAY,
            ChatFormatting.WHITE
    );

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry, Commands.CommandSelection context) {
        int minPermissionLevel = PackWizardCommon.INSTANCE.getConfig().minimum_permission_level;

        dispatcher.register(
                new CommandBuilder("packwizard", src -> src.hasPermission(minPermissionLevel))
                        .then("link", link -> link
                                .argument("url", StringArgumentType.greedyString(), url -> url
                                        .executes(this::setTomlLink)
                                )
                        )
                        .then("update", update -> update.executes(this::update))
                        .then("minimumPermissionLevel", minLevel -> minLevel
                                .argument("level", IntegerArgumentType.integer(0, 4), level -> level
                                        .executes(this::setMinPermissionLevel)
                                )
                        )
                        .then("autoUpdate", autoUpdate -> autoUpdate
                                .argument("enabled", BoolArgumentType.bool(), enabled -> enabled
                                        .executes(this::setAutoUpdate)
                                )
                        )
                        .then("autoUpdateInterval", interval -> interval
                                .argument("minutes", IntegerArgumentType.integer(1), minutes -> minutes
                                        .executes(this::setAutoUpdateInterval)
                                )
                        )
                        .then("autoUpdateStatus", status -> status.executes(this::autoUpdateStatus))
                        .build()
        );
    }

    @Override
    public int action(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private int setTomlLink(CommandContext<CommandSourceStack> context) {
        try {
            var url = PackWizardCommon.PACK_MANAGER.testPackTomlLink(StringArgumentType.getString(context, "url"));
            var configManager = PackWizardCommon.INSTANCE.getConfigManager();
            var config = configManager.getConfig();
            var oldPackTomlLink = config.pack_toml;
            var newPackTomlLink = url.toExternalForm();

            config.pack_toml = url.toExternalForm();
            configManager.setConfig(config);
            configManager.saveConfig();

            var webhooks = getWebhookMessages();
            sendConfigWebhook(
                    webhooks != null ? webhooks.packTomlLinkUpdated : null,
                    List.of(
                            field("Updated By", context.getSource().getTextName()),
                            field("Old Value", oldPackTomlLink == null || oldPackTomlLink.isBlank() ? "(empty)" : oldPackTomlLink),
                            field("New Value", newPackTomlLink)
                    )
            );

            Helpers.getCommandOutput(context).sendSystemMessage(UPDATED_TOML_LINK);
            return 1;
        } catch (PackTomlUrlException e) {
            var error = CommandExceptions.FILE_UPDATE_FAILED.create();
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(error.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public int update(CommandContext<CommandSourceStack> context) {
        try {
            if (!PackWizardCommon.INSTANCE.getGameDir().exists())
                throw CommandExceptions.DIRECTORY_SECURITY_ERROR.create();

            String packTomlLink = PackWizardCommon.INSTANCE.getConfig().pack_toml;
            if (!packTomlLink.contains("pack.toml"))
                throw CommandExceptions.NO_PACK_TOML.create();
            if (PackWizardCommon.PACK_MANAGER.isAsyncTaskRunning(PackManager.UPDATE_PACKWIZ_TASK_NAME))
                throw CommandExceptions.UPDATE_IN_PROGRESS_ERROR.create();

            CommandSource output = Helpers.getCommandOutput(context);
            boolean hasBootstrap = PackWizardCommon.PACK_MANAGER.hasBootstrap();
            if (hasBootstrap) {
                output.sendSystemMessage(UPDATE_START);
            } else {
                output.sendSystemMessage(UPDATE_START_NO_BOOTSTRAP);
            }

            if (PackWizardCommon.PACK_MANAGER.update(packTomlLink, hasBootstrap, output)) {
                PackWizardCommon.INSTANCE.resetAutoUpdateSchedule();
            }
            return 1;
        } catch (CommandSyntaxException e) {
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public int setMinPermissionLevel(CommandContext<CommandSourceStack> context) {
        try {
            int minPermissionLevel = IntegerArgumentType.getInteger(context, "level");
            var configManager = PackWizardCommon.INSTANCE.getConfigManager();
            var config = configManager.getConfig();
            int oldMinPermissionLevel = config.minimum_permission_level;

            config.minimum_permission_level = minPermissionLevel;
            configManager.setConfig(config);
            configManager.saveConfig();

            var webhooks = getWebhookMessages();
            sendConfigWebhook(
                    webhooks != null ? webhooks.minimumPermissionLevelUpdated : null,
                    List.of(
                            field("Updated By", context.getSource().getTextName()),
                            field("Old Value", String.valueOf(oldMinPermissionLevel)),
                            field("New Value", String.valueOf(minPermissionLevel))
                    )
            );

            Helpers.getCommandOutput(context).sendSystemMessage(SET_MIN_PERMISSION_LEVEL);
            return 1;
        } catch (RuntimeException e) {
            var error = CommandExceptions.FILE_UPDATE_FAILED.create();
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(error.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public int setAutoUpdate(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            var configManager = PackWizardCommon.INSTANCE.getConfigManager();
            var config = configManager.getConfig();
            boolean oldEnabled = config.auto_update;

            config.auto_update = enabled;
            configManager.setConfig(config);
            configManager.saveConfig();
            PackWizardCommon.INSTANCE.resetAutoUpdateSchedule();

            var webhooks = getWebhookMessages();
            sendConfigWebhook(
                    webhooks != null ? webhooks.autoUpdateUpdated : null,
                    List.of(
                            field("Updated By", context.getSource().getTextName()),
                            field("Old Value", String.valueOf(oldEnabled)),
                            field("New Value", String.valueOf(enabled))
                    )
            );

            Helpers.getCommandOutput(context).sendSystemMessage(enabled ? SET_AUTO_UPDATE_ENABLED : SET_AUTO_UPDATE_DISABLED);
            return 1;
        } catch (RuntimeException e) {
            var error = CommandExceptions.FILE_UPDATE_FAILED.create();
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(error.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public int setAutoUpdateInterval(CommandContext<CommandSourceStack> context) {
        try {
            int minutes = IntegerArgumentType.getInteger(context, "minutes");
            var configManager = PackWizardCommon.INSTANCE.getConfigManager();
            var config = configManager.getConfig();
            int oldInterval = config.auto_update_interval_minutes;

            config.auto_update_interval_minutes = minutes;
            configManager.setConfig(config);
            configManager.saveConfig();
            PackWizardCommon.INSTANCE.resetAutoUpdateSchedule();

            var webhooks = getWebhookMessages();
            sendConfigWebhook(
                    webhooks != null ? webhooks.autoUpdateIntervalUpdated : null,
                    List.of(
                            field("Updated By", context.getSource().getTextName()),
                            field("Old Value", String.valueOf(oldInterval)),
                            field("New Value", String.valueOf(minutes))
                    )
            );

            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal("Set automatic update interval to " + minutes + " minute(s).").withStyle(ChatFormatting.GREEN));
            return 1;
        } catch (RuntimeException e) {
            var error = CommandExceptions.FILE_UPDATE_FAILED.create();
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(error.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public int autoUpdateStatus(CommandContext<CommandSourceStack> context) {
        var output = Helpers.getCommandOutput(context);
        var config = PackWizardCommon.INSTANCE.getConfig();
        boolean updateRunning = PackWizardCommon.PACK_MANAGER.isAsyncTaskRunning(PackManager.UPDATE_PACKWIZ_TASK_NAME);

        var chatBuilder = new ChatTableBuilder("Auto Update Status", PackWizFormatting);

        chatBuilder = chatBuilder.addRow("Enabled", config.auto_update ? "Yes" : "No");
        chatBuilder = chatBuilder.addRow("Update Interval (minutes)", String.valueOf(config.auto_update_interval_minutes));
        chatBuilder = chatBuilder.addRow("Update Running", updateRunning ? "Yes" : "No");

        if (!config.auto_update) {
            output.sendSystemMessage(chatBuilder.build());
            return 1;
        }

        if (config.auto_update_interval_minutes <= 0) {
            output.sendSystemMessage(chatBuilder.build());
            output.sendSystemMessage(Component.literal("Automatic updates are enabled, but the update interval is set to 0 or less.").withStyle(ChatFormatting.RED));
            return 0;
        }

        long intervalTicks = (long) config.auto_update_interval_minutes * 1_200L;
        long elapsedTicks = Math.max(0L, PackWizardCommon.INSTANCE.getAutoUpdateTicks());
        long remainingTicks = Math.max(0L, intervalTicks - elapsedTicks);
        long remainingSeconds = remainingTicks / 20L;
        long remainingMinutes = (remainingSeconds + 59L) / 60L;

        chatBuilder = chatBuilder.addSection("Next Update");
        chatBuilder = chatBuilder.addRow("Interval (minutes)", String.valueOf(remainingMinutes));
        chatBuilder = chatBuilder.addRow("Interval (seconds)", String.valueOf(remainingSeconds));
        chatBuilder = chatBuilder.addRow("Interval (ticks)", String.valueOf(remainingTicks));

        output.sendSystemMessage(chatBuilder.build());

        return 1;
    }

    public static void pollCommandStatus() {
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
                    var cause = e.getCause();
                    exception = e;

                    if (cause instanceof InterruptedException)
                        message = PROCESS_INTERRUPTED;
                    else if (cause instanceof IOException)
                        message = FILE_HANDLING_ERROR;

                    if (task.hasName(PackManager.UPDATE_PACKWIZ_TASK_NAME)) {
                        if (cause instanceof PackTomlUrlException ptfe)
                            message = Component.literal(ptfe.getMessage());
                        else if (cause instanceof ProcessExitCodeException pece)
                            message = Component.literal(pece.getMessage());
                        else if (cause instanceof FailedHashMatchException fhme)
                            message = Component.literal(fhme.getMessage());
                    }
                    if (message == null) message = COMMAND_FAILED;
                }
                task.sendMessage(message);
                if (exception != null)
                    PackWizardCommon.INSTANCE.createErrorLog("Unexpected exception occurred whilst polling Packwiz command status", exception);
                tasksIterator.remove();
            }
        }
    }
}
