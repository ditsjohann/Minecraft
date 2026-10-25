package com.armageddon.client.fx;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Blends sky, cloud and fog colours towards the current blast tint. */
public final class SkyTint {
	public static Vec3d apply(Vec3d base, float weight) {
		float s = MathHelper.clamp(ClientFx.skyTintStrength * weight, 0.0F, 1.0F);
		if (s <= 0.0F) {
			return base;
		}
		// Ease the blend so tints roll in and out smoothly instead of snapping.
		s = s * s * (3 - 2 * s);
		int c = ClientFx.skyTint;
		double r = ((c >> 16) & 0xFF) / 255.0;
		double g = ((c >> 8) & 0xFF) / 255.0;
		double b = (c & 0xFF) / 255.0;
		// Keep a little of the original brightness so tinted skies still read day from night.
		double lum = base.x * 0.299 + base.y * 0.587 + base.z * 0.114;
		double keep = 0.75 + 0.25 * Math.min(1.0, lum * 1.6);
		return new Vec3d(MathHelper.lerp(s, base.x, r * keep), MathHelper.lerp(s, base.y, g * keep), MathHelper.lerp(s, base.z, b * keep));
	}

	private SkyTint() {
	}
}
