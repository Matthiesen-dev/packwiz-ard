package dev.matthiesen.packwiz_ard.common.interfaces;

import dev.matthiesen.packwiz_ard.common.util.TickCounter;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class AsyncCommandTask {
    private final String name;
    private final CompletableFuture<Void> future;
    private final CommandSource co;
    private final TickCounter tc;

    public AsyncCommandTask(CompletableFuture<Void> future, String name, int pollTicks, CommandSource commandSource) {
        this.future = future;
        this.name = name;
        this.co = commandSource;
        this.tc = new TickCounter(pollTicks);
    }

    public void tick() { tc.increment(); }

    public boolean pollFinished() {
        return (tc.test() && future.isDone());
    }

    public void sendMessage(Component message) {
        if (future.isDone())
            co.sendSystemMessage(message);
    }

    public CompletableFuture<Void> getFuture() { return future; }
    public boolean hasName(String name) { return this.name.equals(name); }
}
