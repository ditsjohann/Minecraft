package com.armageddon.client.mixin;

import com.armageddon.client.fx.PostFx;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the Armageddon post-processing pass after the world and hand, before the HUD. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderWorld", at = @At("TAIL"))
	private void armageddon$postFx(RenderTickCounter tickCounter, CallbackInfo ci) {
		PostFx.render(tickCounter.getTickDelta(false));
	}
}
