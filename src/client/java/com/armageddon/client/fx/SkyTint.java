package com.armageddon.client.fx;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Blends sky, cloud and fog colours towards the current blast tint. */
public final class SkyTint {
	public static Vec3d apply(Vec3d base, float weight) {
		float s = ClientFx.skyTintStrength * weight;
		if (s <= 0.0F) {
			return base;
		}
		int c = ClientFx.skyTint;
		double r = ((c >> 16) & 0xFF) / 255.0;
		double g = ((c >> 8) & 0xFF) / 255.0;
		double b = (c & 0xFF) / 255.0;
		return new Vec3d(MathHelper.lerp(s, base.x, r), MathHelper.lerp(s, base.y, g), MathHelper.lerp(s, base.z, b));
	}

	private SkyTint() {
	}
}
