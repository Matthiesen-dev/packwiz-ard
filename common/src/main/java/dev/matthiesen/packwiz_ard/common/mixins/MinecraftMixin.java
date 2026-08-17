package dev.matthiesen.packwiz_ard.common.mixins;

import dev.matthiesen.packwiz_ard.common.PackWizardClientCommon;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void packwiz_ard$tick(CallbackInfo ci) {
        PackWizardClientCommon.INSTANCE.tick((Minecraft) (Object) this);
    }
}

