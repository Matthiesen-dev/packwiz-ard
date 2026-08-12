package dev.matthiesen.packwiz_ard.common.shared.interfaces;

import dev.matthiesen.packwiz_ard.common.shared.util.TickCounter;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AsyncCommandTask {
    private final String name;
    private final CompletableFuture<Void> future;
    private final Consumer<Component> messageSink;
    private final TickCounter tc;

    public AsyncCommandTask(CompletableFuture<Void> future, String name, int pollTicks, Consumer<Component> messageSink) {
        this.future = future;
        this.name = name;
        this.messageSink = messageSink;
        this.tc = new TickCounter(pollTicks);
    }

    public void tick() { tc.increment(); }

    public boolean pollFinished() {
        return (tc.test() && future.isDone());
    }

    public void sendMessage(Component message) {
        if (future.isDone())
            messageSink.accept(message);
    }

    public CompletableFuture<Void> getFuture() { return future; }
    public boolean hasName(String name) { return this.name.equals(name); }
}
