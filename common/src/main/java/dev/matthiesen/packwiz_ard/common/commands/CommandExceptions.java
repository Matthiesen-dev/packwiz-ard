package dev.matthiesen.packwiz_ard.common.commands;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.minecraft.network.chat.Component;

public final class CommandExceptions {
     static final SimpleCommandExceptionType FILE_UPDATE_FAILED = new SimpleCommandExceptionType(Component.literal("Failed to update the " + PackWizardCommon.MOD_ID + "/config.json configuration file"));
     static final SimpleCommandExceptionType UPDATE_IN_PROGRESS_ERROR = new SimpleCommandExceptionType(Component.literal("Packwiz update is already in progress"));
     static final SimpleCommandExceptionType NO_PACK_TOML = new SimpleCommandExceptionType(Component.literal("There is no pack.toml link to update from. Add this using /packwizard link [url]."));
     static final SimpleCommandExceptionType DIRECTORY_SECURITY_ERROR = new SimpleCommandExceptionType(Component.literal(PackWizardCommon.MOD_NAME + " does not have permission to access the game directory and modify files"));
}
