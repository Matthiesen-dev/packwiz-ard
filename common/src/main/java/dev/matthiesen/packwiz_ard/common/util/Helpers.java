package dev.matthiesen.packwiz_ard.common.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class Helpers {
    public static CommandSource getCommandOutput(CommandContext<CommandSourceStack> ctx) {
        return (ctx.getSource().getEntity() instanceof ServerPlayer player)
                ? player : ctx.getSource().getServer();
    }
}
