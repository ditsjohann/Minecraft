package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.CelestialTimeline;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Zeropoint: a sphere where time has stopped. Dust hangs motionless inside a faint ice-blue skin, a clock face
 * on the ground ticks slower and slower and stops, twitches backwards, then everything snaps into one black
 * point and a single crisp ring rings out. Frost glints on the hollow left behind.
 */
public class CelestialZeropointFx extends FxEffect {
	private static final int[] CLOCK = {0, 6, 12, 20, 30, 44, 62, 86};
	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
	private final Vec3d c;
	private final double radius;
	private final Vec3d[] motes = new Vec3d[220];

	public CelestialZeropointFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.c = new Vec3d(p.x(), p.y(), p.z());
		this.radius = Math.max(4, p.scale());
		for (int i = 0; i < motes.length; i++) {
			motes[i] = Effects.dir(i, 7).multiply(radius * Math.cbrt(Solids.hash(i, 8)) * 0.97);
		}
	}

	/** Second-hand angle: a tick at every server click, frozen after the last, then spinning backwards. */
	private static double hand(double a) {
		int steps = 0;
		double settle = 0;
		for (int k : CLOCK) {
			if (a >= k) {
				steps++;
				settle = Math.exp(-(a - k) * 0.6);
			}
		}
		double ang = steps - settle * 0.25;
		if (a >= CelestialTimeline.ZP_REWIND) {
			ang -= (a - CelestialTimeline.ZP_REWIND) * 0.9;
		}
		return -ang * MathHelper.TAU / 60.0 + Math.PI / 2;
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age < CelestialTimeline.ZP_SNAP) {
			ClientFx.tintSky(0x8FD8FF, 0.3F * Effects.smooth(age / 12.0F));
			ClientFx.light(c, 0.35F, 0x9FEFFF);
		} else if (age == CelestialTimeline.ZP_SNAP + 1) {
			ClientFx.light(c, 1.2F, 0xC8F8FF);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d cc = c.subtract(cam);
		int snap = CelestialTimeline.ZP_SNAP;
		int segs = Effects.lod(cc, radius, 32, 96);
		int count = Effects.lod(cc, radius, 60, motes.length);

		if (a < snap + 12) {
			float appear = Effects.smooth(a / 12.0F);
			double e = Math.max(0, a - snap);
			double pull = Cinematics.ease(e / 12.0);
			boolean rewind = a >= CelestialTimeline.ZP_REWIND;
			// The skin of the stopped moment.
			FxDraw.sphere(glow, m, cc, radius * (1 - pull), Vec3d.ZERO, 0x9FEFFF, 0.0F, 0.22F * appear, 12, 24);
			// Dust that stopped mid-air: perfectly still, a faint shiver when time twitches backwards.
			for (int i = 0; i < count; i++) {
				Vec3d p = motes[i];
				if (rewind && pull == 0) {
					p = p.add(Effects.dir(i, 9).multiply(Math.sin(a * 2.7 + i) * 0.25));
				}
				p = cc.add(p.multiply(1 - pull));
				float tw = 0.55F + 0.45F * MathHelper.sin(i * 1.3F + a * 0.05F);
				Solids.dot(glow, m, p, cb, 0.14 + 0.2 * Solids.hash(i, 10), i % 4 == 0 ? 0x9FEFFF : 0xFFFFFF, 0.8F * tw * appear);
			}
			if (pull < 1) {
				// The clock face on the ground: sixty ticks and a hand that slows, stops and runs back.
				float face = 0.55F * appear * (float) (1 - pull);
				Vec3d f = cc.add(0, 0.6, 0);
				FxDraw.ring(glow, m, f, X, Z, radius * 1.02, radius * 1.08, 0x9FEFFF, 0.0F, face, 0.0F, segs);
				for (int i = 0; i < 60; i++) {
					double ang = i * MathHelper.TAU / 60;
					Vec3d d = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
					double in = i % 5 == 0 ? 0.88 : 0.95;
					FxDraw.ribbon(glow, m, f.add(d.multiply(radius * in)), f.add(d.multiply(radius * 1.01)), Vec3d.ZERO, i % 5 == 0 ? 0.6 : 0.25,
							0xC8F8FF, face, face);
				}
				double h = hand(a);
				Vec3d hd = new Vec3d(Math.cos(h), 0, Math.sin(h));
				FxDraw.ribbon(glow, m, f.subtract(hd.multiply(radius * 0.12)), f.add(hd.multiply(radius * 0.9)), Vec3d.ZERO, 0.5, 0xFFFFFF, face, face);
				double mh = h * 0.08 + 1.1;
				Vec3d md = new Vec3d(Math.cos(mh), 0, Math.sin(mh));
				FxDraw.ribbon(glow, m, f, f.add(md.multiply(radius * 0.6)), Vec3d.ZERO, 1.0, 0x9FEFFF, face, face);
				Solids.dot(glow, m, f, cb, 1.2, 0xFFFFFF, face);
			}
			if (e > 0) {
				// Everything rushing into the point.
				Solids.dot(glow, m, cc, cb, 1.0 + e * 0.6, 0xC8F8FF, (float) (0.8 * pull));
			}
		}

		double e = a - snap;
		if (e >= 10 && e < 18) {
			// The zero point itself: one black pinhole with a hairline of light around it.
			float k = (float) (1 - Math.abs(e - 14) / 4.0);
			Solids.sphere(consumers.getBuffer(FxRenderLayers.SOLID), m, cc, 0.4 + 1.0 * k, 0x000000, 6, 10, false);
			FxDraw.ring(glow, m, cc, cb[0], cb[1], 1.4 + k, 1.8 + k, 0xFFFFFF, 0.0F, 0.9F * k, 0.0F, 24);
		}
		if (e >= 12 && e < 72) {
			// A single crisp ring: the sound of the moment closing.
			double rad = radius * 2.4 * Cinematics.ease((e - 12) / 45.0);
			float al = (float) (1 - (e - 12) / 60.0);
			FxDraw.ring(glow, m, cc, cb[0], cb[1], Math.max(0, rad - 1.5), rad, 0xC8F8FF, 0.0F, 0.8F * al, 0.0F, segs);
			FxDraw.ring(glow, m, cc.add(0, 0.5, 0), X, Z, Math.max(0, rad * 0.8 - 1.0), rad * 0.8, 0x9FEFFF, 0.0F, 0.5F * al, 0.0F, segs);
		}
		if (e >= 14) {
			// Frost glinting on the polished hollow.
			float life = (float) Cinematics.clamp01((lifetime - a) / 80.0) * Effects.smooth((float) (e - 14) / 10.0F);
			for (int i = 0; i < count / 2; i++) {
				Vec3d d = Effects.dir(i, 31);
				d = new Vec3d(d.x, -Math.abs(d.y), d.z);
				float tw = (float) Math.max(0, Math.sin(a * 0.2 + i * 2.1));
				Solids.dot(glow, m, cc.add(d.multiply(radius * 0.98)), cb, 0.3 + 0.4 * tw, 0xE0FFFF, 0.8F * tw * life);
			}
		}
	}
}
