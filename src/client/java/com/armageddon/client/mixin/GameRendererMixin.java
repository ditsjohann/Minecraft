package com.armageddon.client.mixin;

import com.armageddon.client.fx.PostFx;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.render.Camera;

/** Runs the Armageddon post-processing pass after the world and hand, before the HUD. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderWorld", at = @At("TAIL"))
	private void armageddon$postFx(RenderTickCounter tickCounter, CallbackInfo ci) {
		PostFx.render(tickCounter.getTickDelta(false));
	}

	/** World-destruction cutscenes punch the field of view. */
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void armageddon$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
		if (com.armageddon.client.fx.ClientFx.fovPunch > 0) {
			cir.setReturnValue(cir.getReturnValue() + com.armageddon.client.fx.ClientFx.fovPunch);
		}
	}
}
