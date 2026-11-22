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
 * Lets the offline preview show particle-engine effects: scene {@code vfx:<effect>[:<scale>[:<length>]]}, where a
 * length stretches the direction (for effects spread along a line); or {@code vfx:trail:<effect>:<speed>}, an emitter
 * flying along +x at that speed, playing the effect every tick as a projectile does; or {@code vfx:repeat:<effect>},
 * played every tick from one spot along +x, as a held weapon does. The effect runs with no world, so
 * there is no collision, ground snapping or world light, and no cues.
 */
final class VfxPreviewEffect extends FxEffect {
	private static boolean booted;
	private final EffectInstance fx;
	private boolean reported;
	private final String trail;
	private final double speed;
	private final Vec3d from;

	static void boot() {
		if (!booted) {
			VfxLibrary.bootstrap();
			booted = true;
		}
	}

	VfxPreviewEffect(String spec, Vec3d at) {
		super(1200);
		boot();
		String[] p = spec.split(":");
		from = at;
		if (p[0].equals("trail") || p[0].equals("repeat")) {
			trail = p[1];
			speed = p[0].equals("repeat") ? 0 : Double.parseDouble(p[2]);
			fx = null;
			return;
		}
		trail = null;
		speed = 0;
		EffectDef def = FxEngine.get(p[0]);
		if (def == null) {
			throw new IllegalArgumentException("unknown particle effect " + p[0]);
		}
		float scale = p.length > 1 ? Float.parseFloat(p[1]) : 1.0F;
		float len = p.length > 2 ? Float.parseFloat(p[2]) : 1.0F;
		fx = FxEngine.create(def, at.x, at.y, at.z, len, 0, 0.3F * len, scale, 0, 12345L, 0);
	}

	@Override
	public boolean tick(ClientWorld world) {
		age++;
		if (trail != null) {
			// The emitter flies along +x, rising gently; the trail it leaves is drawn by the engine.
			if (speed == 0) {
				// Repeat: played every tick from one place, aimed along +x (a held weapon).
				FxEngine.play(trail, from.x, from.y + 1.5, from.z, 1, 0, 0.3F, 1.0F, 0, 777L + age);
				return true;
			}
			Vec3d at = from.add(age * speed, 4 + age * 0.02 * speed, 0);
			FxEngine.playMoving(trail, at.x, at.y, at.z, (float) -speed, 0, 0, 1.0F, 777L + age, (float) speed, (float) (0.02 * speed), 0);
			return true;
		}
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
		if (fx != null) {
			FxEngine.renderAlone(fx, m, consumers, cam, tickDelta, camYaw, camPitch);
		}
	}
}
