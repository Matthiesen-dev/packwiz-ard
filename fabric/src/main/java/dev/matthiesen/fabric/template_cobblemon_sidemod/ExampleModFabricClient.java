package dev.matthiesen.fabric.template_cobblemon_sidemod;

import dev.matthiesen.common.template_cobblemon_sidemod.ExampleModCommonClient;
import net.fabricmc.api.ClientModInitializer;

public class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExampleModCommonClient.initialize();
    }
}
