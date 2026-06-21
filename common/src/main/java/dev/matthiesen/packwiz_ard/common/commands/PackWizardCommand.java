package dev.matthiesen.packwiz_ard.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.matthiesen.common.matthiesen_lib_api.command.AbstractCommand;
import dev.matthiesen.common.matthiesen_lib_api.utility.CommandBuilder;
import dev.matthiesen.packwiz_ard.common.PackManager;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.exceptions.FailedHashMatchException;
import dev.matthiesen.packwiz_ard.common.exceptions.PackTomlUrlException;
import dev.matthiesen.packwiz_ard.common.exceptions.ProcessExitCodeException;
import dev.matthiesen.packwiz_ard.common.util.Helpers;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
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

    public static final PackWizardCommand CMD = new PackWizardCommand();

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
            config.pack_toml = url.toExternalForm();
            configManager.setConfig(config);
            configManager.saveConfig();
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

            boolean hasBootstrap = PackWizardCommon.PACK_MANAGER.hasBootstrap();
            if (hasBootstrap) {
                Helpers.getCommandOutput(context).sendSystemMessage(UPDATE_START);
            } else {
                Helpers.getCommandOutput(context).sendSystemMessage(UPDATE_START_NO_BOOTSTRAP);
            }

            PackWizardCommon.PACK_MANAGER.update(packTomlLink, hasBootstrap, context);
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
            config.minimum_permission_level = minPermissionLevel;
            configManager.setConfig(config);
            configManager.saveConfig();
            Helpers.getCommandOutput(context).sendSystemMessage(SET_MIN_PERMISSION_LEVEL);
            return 1;
        } catch (RuntimeException e) {
            var error = CommandExceptions.FILE_UPDATE_FAILED.create();
            PackWizardCommon.INSTANCE.createErrorLog(e.getMessage(), e);
            Helpers.getCommandOutput(context).sendSystemMessage(Component.literal(error.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public static void pollCommandStatus() {
        var tasksIterator = PackManager.TASKS.listIterator();
        Exception exception = null;
        Component message = null;

        while (tasksIterator.hasNext()) {
            var task = tasksIterator.next();
            task.tick();

            if (task.pollFinished()) {
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
