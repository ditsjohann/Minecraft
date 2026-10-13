package com.armageddon.client.mixin;

import com.armageddon.client.fx.SkyTint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tints the fog / horizon colour to match the sky tint. */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
	@Shadow
	private static float red;
	@Shadow
	private static float green;
	@Shadow
	private static float blue;

	@Inject(method = "render", at = @At("TAIL"))
	private static void armageddon$tintFog(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
		Vec3d tinted = SkyTint.apply(new Vec3d(red, green, blue), 0.7F);
		if (tinted.x == red && tinted.y == green && tinted.z == blue) {
			return;
		}
		red = (float) tinted.x;
		green = (float) tinted.y;
		blue = (float) tinted.z;
		RenderSystem.clearColor(red, green, blue, 0.0F);
	}

	/** Space has no fog: stars and planets must stay crisp at hundreds of blocks. */
	@Inject(method = "applyFog", at = @At("TAIL"))
	private static void armageddon$spaceFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
		com.armageddon.client.fx.Cinematics.Cine cine = com.armageddon.client.fx.ClientFx.activeCine();
		if (cine != null && cine.frame(tickDelta).space) {
			RenderSystem.setShaderFogStart(1.0E6F);
			RenderSystem.setShaderFogEnd(2.0E6F);
		}
	}
}
