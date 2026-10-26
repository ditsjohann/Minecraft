package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * One timed {@link Vision} mode. Payload: a = source, scale = max radius, seed = mode, p1 = radius speed
 * (blocks/tick, negative to shrink), p2 = mode parameter, b.x = pulse period in ticks (0 = one sweep),
 * b.y = starting radius, color = mode tint. A newer vision of the same mode replaces this one.
 */
public class VisionFx extends FxEffect {
	final Vec3d origin;
	final float maxR, speed, period, start, param;
	final int mode, color;
	boolean replaced;
	/** Replacing a live vision of the same mode: carry on at full strength instead of fading in again. */
	private boolean seamless;

	public VisionFx(FxPayload p) {
		super(Math.max(20, p.duration()));
		this.origin = new Vec3d(p.x(), p.y(), p.z());
		this.maxR = p.scale();
		this.mode = p.seed();
		this.speed = p.p1();
		this.param = p.p2();
		this.period = (float) p.x2();
		this.start = (float) p.y2();
		this.color = p.color();
		for (FxEffect e : ClientFx.EFFECTS) {
			if (e instanceof VisionFx v && v.mode == mode && !v.replaced) {
				v.replaced = true;
				seamless = true;
			}
		}
	}

	float strength(float tickDelta) {
		float t = age + tickDelta;
		float in = seamless ? 1.0F : MathHelper.clamp(t / 12.0F, 0.0F, 1.0F);
		float out = MathHelper.clamp((lifetime - t) / 20.0F, 0.0F, 1.0F);
		return in * out;
	}

	/** The current pulse front (or the region's edge). */
	float radius(float t) {
		if (period > 0) {
			return (t % period) * speed;
		}
		return MathHelper.clamp(start + speed * t, 0.0F, maxR);
	}

	/** How far the first sweep has uncovered the world. */
	float reveal(float t) {
		return Math.min(maxR, Math.max(0, start) + Math.abs(speed) * t);
	}

	float param(float t) {
		return switch (mode) {
			case Vision.LIDAR -> maxR;
			// The black core grows with the void.
			case Vision.VOID -> param * radius(t) / Math.max(1.0F, maxR);
			// The pixels get chunkier as the effect takes hold.
			case Vision.PIXEL -> Math.max(1.0F, param * strength(t - age));
			default -> param;
		};
	}

	@Override
	public boolean tick(ClientWorld world) {
		return !replaced && super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
		// Drawn as a screen pass by Vision.
	}
}
