package dev.combi.chunklockutils.client.mixin;

import dev.combi.chunklockutils.client.config.CluConfig;
import dev.combi.chunklockutils.client.features.EvolvingProgressBar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Inject(
			method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
			at = @At("TAIL")
	)
	private void chunklockutils$afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!CluConfig.get().showEvolvedProgressBar) return;
		EvolvingProgressBar.renderGui((HandledScreen<?>)(Object)this, context);
	}
}
