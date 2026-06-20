package dev.matthiesen.fabric.template_cobblemon_sidemod;

import dev.matthiesen.common.template_cobblemon_sidemod.ExampleModCommon;
import dev.matthiesen.common.template_cobblemon_sidemod.Constants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ExampleModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.createInfoLog("Loading for Fabric Mod Loader");
        ExampleModCommon.initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ExampleModCommon.onStartup());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ExampleModCommon.onShutdown());
    }
}
