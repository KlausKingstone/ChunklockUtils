package dev.combi.chunklockutils.client.mixin;

import dev.combi.chunklockutils.client.config.ConfigManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

	@Inject(
			method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD")
	)
	private void clu$beforeRenderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode mode,
									  boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vcp, int light,
									  CallbackInfo ci) {
		if (!isFirstPerson(mode) || !ConfigManager.get().smallHand) return;
		matrices.push();

		float s = clamp(0.40f, 0.05f, 1.0f);
		matrices.scale(s, s, s);

		float side = leftHanded ? -1f : 1f;
		float dx = 0.22f / s;
		float dy = 0.18f / s;
		float dz = 0.00f / s;
		matrices.translate(side * dx, dy, dz);
	}

	@Inject(
			method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("RETURN")
	)
	private void clu$afterRenderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode mode,
									 boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vcp, int light,
									 CallbackInfo ci) {
		if (!isFirstPerson(mode) || !ConfigManager.get().smallHand) return;
		matrices.pop();
	}

	private static boolean isFirstPerson(ModelTransformationMode mode) {
		return mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
				|| mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND;
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}
}