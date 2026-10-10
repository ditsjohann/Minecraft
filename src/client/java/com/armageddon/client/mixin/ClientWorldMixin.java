package com.armageddon.client.mixin;

import com.armageddon.client.fx.SkyTint;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tints the sky and clouds after detonations (orange nuke skies, blood-red Omega, rainbow bass). */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void armageddon$tintSky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
		cir.setReturnValue(SkyTint.apply(cir.getReturnValue(), 0.85F));
	}

	@Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
	private void armageddon$tintClouds(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
		cir.setReturnValue(SkyTint.apply(cir.getReturnValue(), 0.6F));
	}
}
