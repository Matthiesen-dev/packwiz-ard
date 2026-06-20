package dev.matthiesen.neoforge.template_cobblemon_sidemod;

import dev.matthiesen.common.template_cobblemon_sidemod.ExampleModCommon;
import dev.matthiesen.common.template_cobblemon_sidemod.Constants;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(Constants.MOD_ID)
public class ExampleModNeoForge {
    public ExampleModNeoForge() {
        Constants.createInfoLog("Loading for NeoForge Mod Loader");
        ExampleModCommon.initialize();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ExampleModCommon.onStartup();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStopping(ServerStoppingEvent event) {
        ExampleModCommon.onShutdown();
    }
}
