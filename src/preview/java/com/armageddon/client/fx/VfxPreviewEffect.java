package com.armageddon.client.fx;

import com.armageddon.client.vfx.EffectDef;
import com.armageddon.client.vfx.EffectInstance;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.client.vfx.VfxLibrary;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Lets the offline preview show particle-engine effects: scene {@code vfx:<effect>[:<scale>]}. The effect runs with
 * no world, so there is no collision, ground snapping or world light, and no cues.
 */
final class VfxPreviewEffect extends FxEffect {
	private static boolean booted;
	private final EffectInstance fx;
	private boolean reported;

	VfxPreviewEffect(String spec, Vec3d at) {
		super(1200);
		if (!booted) {
			VfxLibrary.bootstrap();
			booted = true;
		}
		String[] p = spec.split(":");
		EffectDef def = FxEngine.get(p[0]);
		if (def == null) {
			throw new IllegalArgumentException("unknown particle effect " + p[0]);
		}
		float scale = p.length > 1 ? Float.parseFloat(p[1]) : 1.0F;
		fx = FxEngine.create(def, at.x, at.y, at.z, 1, 0, 0.3F, scale, 0, 12345L, 0);
	}

	@Override
	public boolean tick(ClientWorld world) {
		age++;
		try {
			return FxEngine.tickAlone(fx, world);
		} catch (RuntimeException e) {
			if (!reported) {
				reported = true;
				PreviewMain.error("vfx tick " + age, e);
			}
			throw e;
		}
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
		FxEngine.renderAlone(fx, m, consumers, cam, tickDelta, camYaw, camPitch);
	}
}
