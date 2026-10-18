package dev.matthiesen.packwiz_ard.common.mixins;

import dev.matthiesen.packwiz_ard.common.PackWizardClientCommon;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {
    @Inject(method = "getSplash", at = @At("RETURN"), cancellable = true)
    private void packwiz_ard$getSplash(CallbackInfoReturnable<SplashRenderer> cir) {
        var status = PackWizardClientCommon.INSTANCE.getStatus();
        if (status != null && status.shouldChangeSplash()) {
            String splashText = status.message();
            SplashRenderer newSplash = new SplashRenderer(splashText);
            cir.setReturnValue(newSplash);
        }
    }
}
