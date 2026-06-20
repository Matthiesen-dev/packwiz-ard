package dev.matthiesen.common.template_cobblemon_sidemod;

import dev.matthiesen.common.matthiesen_lib_api.MatthiesenLibApi;

public class ExampleModCommon {

    public static void initialize() {
        Constants.createInfoLog("Initialized");

       if (MatthiesenLibApi.isModLoaded("cobblemon")) {
            Constants.createInfoLog("Cobblemon is loaded, Hello there Cobblemon!");
       }
    }

    public static void onStartup() {
        Constants.createInfoLog("Server starting, Setting up");
    }

    public static void onShutdown() {
        Constants.createInfoLog("Server stopping, shutting down");
    }
}
