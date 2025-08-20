package dev.combi.chunklockutils.client.mixin;

import dev.combi.chunklockutils.client.config.ConfigManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void clu$hideDroppedItems(ItemEntityRenderState state,
									  MatrixStack matrices,
									  VertexConsumerProvider vertices,
									  int light,
									  CallbackInfo ci) {
		if (ConfigManager.get().hideDroppedItems) {
			ci.cancel();
		}
	}
}
