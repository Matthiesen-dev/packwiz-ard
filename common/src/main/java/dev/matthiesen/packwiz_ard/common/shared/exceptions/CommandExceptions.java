package dev.matthiesen.packwiz_ard.common.shared.exceptions;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import net.minecraft.network.chat.Component;

public final class CommandExceptions {
     public static final SimpleCommandExceptionType FILE_UPDATE_FAILED = new SimpleCommandExceptionType(Component.literal("Failed to update the " + PackWizardCommon.MOD_ID + "/config.json configuration file"));
     public static final SimpleCommandExceptionType UPDATE_IN_PROGRESS_ERROR = new SimpleCommandExceptionType(Component.literal("An update is already in progress"));
     public static final SimpleCommandExceptionType NO_PACK_TOML = new SimpleCommandExceptionType(Component.literal("There is no configured modpack source to update from. Add this using /packwizard link [url]."));
     public static final SimpleCommandExceptionType DIRECTORY_SECURITY_ERROR = new SimpleCommandExceptionType(Component.literal(PackWizardCommon.MOD_NAME + " does not have permission to access the game directory and modify files"));
}
