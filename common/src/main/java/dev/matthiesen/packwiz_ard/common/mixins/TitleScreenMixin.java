package dev.matthiesen.packwiz_ard.common.mixins;

import dev.matthiesen.packwiz_ard.common.PackWizardClientCommon;
import dev.matthiesen.packwiz_ard.common.client.PackWizardClientScreen;
import dev.matthiesen.packwiz_ard.common.client.PackWizardStatusButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void packwiz_ard$init(CallbackInfo ci) {
        int baseX = this.width / 2;
        int baseY = this.height / 4 + 48;

        this.addRenderableWidget(new PackWizardStatusButton(
                baseX - 148,
                baseY + 84,
                20,
                20,
                button -> Minecraft.getInstance().setScreen(new PackWizardClientScreen(this)),
                PackWizardClientCommon.INSTANCE::getStatus
        ));

    }
}


