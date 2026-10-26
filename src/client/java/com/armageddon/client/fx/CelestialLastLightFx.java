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
 * The Last Light: a white star that beats like a heart. Each beat throws a shaft-lit wave of white across the
 * ground (in step with the server's bleaching), the light hangs in the air as pale solid rays, the heart
 * flatlines and swells, then collapses to an ember while the sky goes grey.
 */
public class CelestialLastLightFx extends FxEffect {
	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
	private final Vec3d star, ground;
	private final double radius;

	public CelestialLastLightFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.star = new Vec3d(p.x(), p.y(), p.z());
		this.ground = new Vec3d(p.x2(), p.y2(), p.z2());
		this.radius = Math.max(8, p.scale());
	}

	private static double sq(double v) {
		return v * v;
	}

	/** Heartbeat envelope: a strong beat and a softer echo six ticks later. */
	static double pulse(double a) {
		double p = 0;
		for (int b : CelestialTimeline.LL_BEATS) {
			p += Math.exp(-sq((a - b) / 2.5)) + 0.65 * Math.exp(-sq((a - b - 6) / 2.5));
		}
		return Math.min(1.3, p);
	}

	/** How far the bleaching has spread at {@code a}. */
	private double reach(double a) {
		int[] beats = CelestialTimeline.LL_BEATS;
		double r = 0;
		for (int k = 0; k < beats.length; k++) {
			if (a < beats[k]) {
				break;
			}
			double prev = k == 0 ? 0 : CelestialTimeline.llRadius(k - 1, radius);
			r = MathHelper.lerp(Cinematics.ease((a - beats[k]) / 14.0), prev, CelestialTimeline.llRadius(k, radius));
		}
		return r;
	}

	@Override
	public boolean tick(ClientWorld world) {
		float fade = (float) Cinematics.clamp01((lifetime - age) / 60.0);
		if (age < CelestialTimeline.LL_EXHALE) {
			ClientFx.light(star, 0.55F + 0.6F * (float) pulse(age), 0xFFF0D8);
			ClientFx.tintSky(0xF4F2FF, 0.45F);
		} else {
			// The light is gone: a grey, dead sky over the white land.
			ClientFx.tintSky(0x8A8A90, 0.55F * fade);
			if (age < CelestialTimeline.LL_EXHALE + 40) {
				ClientFx.light(star, 0.9F * (1 - (age - CelestialTimeline.LL_EXHALE) / 40.0F), 0xFFB070);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d s = star.subtract(cam), g = ground.subtract(cam);
		float fade = (float) Cinematics.clamp01((lifetime - a) / 60.0);
		float appear = Effects.smooth(a / 30.0F);
		double pulse = pulse(a);
		int exhale = CelestialTimeline.LL_EXHALE, flat = CelestialTimeline.LL_FLATLINE;

		// --- the star: beating, then swelling after the flatline, then collapsing to an ember.
		double size;
		int core = 0xFFFFFF, halo = 0xFFF0D8;
		float bright;
		if (a < flat) {
			size = 3.2 + 1.6 * pulse;
			bright = appear;
		} else if (a < exhale) {
			double k = Cinematics.ease((a - flat) / (double) (exhale - flat));
			size = 3.2 + 3.6 * k + Math.sin(a * 2.3) * 0.25 * k;
			halo = Effects.fade(0xFFF0D8, 0xE8F0FF, (float) k);
			bright = 1.0F;
		} else {
			double e = a - exhale;
			size = 6.8 * Math.exp(-e / 6.0) + 0.9;
			core = Effects.fade(0xFFE0C0, 0x804030, (float) Cinematics.clamp01(e / 200.0));
			halo = Effects.fade(0xFFB070, 0x402018, (float) Cinematics.clamp01(e / 160.0));
			bright = (float) (0.25 + 0.75 * Math.exp(-e / 20.0)) * fade;
		}
		Solids.dot(glow, m, s, cb, size * 5.0, halo, 0.16F * bright);
		Solids.dot(glow, m, s, cb, size * 2.2, halo, 0.5F * bright);
		Solids.dot(glow, m, s, cb, size, core, bright);
		Solids.flare(glow, m, s, cb, size * 3.0, halo, 0.55F * bright);
		FxKit.flare(consumers, m, cb, s, size * 7.0, halo, 0.6F * bright, a * 0.002);
		FxKit.halo(consumers, m, cb, s, size * 9.0, halo, 0.22F * bright * (float) (0.6 + 0.4 * pulse));
		int spikes = Effects.lod(s, 40, 10, 22);
		for (int i = 0; i < spikes; i++) {
			double ang = i * MathHelper.TAU / spikes + a * 0.004 + Solids.hash(i, 3) * 0.2;
			double len = size * (2.0 + 1.5 * Solids.hash(i, 4) + pulse * 2.0);
			Vec3d d = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
			FxDraw.ribbon(glow, m, s.add(d.multiply(size * 0.6)), s.add(d.multiply(len)), Vec3d.ZERO, size * 0.18, halo, 0.45F * bright, 0.0F);
		}

		// --- light hanging in the air: pale solid shafts from the star to the bleached ground.
		double reach = reach(a);
		float shaft = (float) ((0.05 + 0.12 * pulse) * appear * (a < exhale ? 1.0 : Math.max(0.0, 1 - (a - exhale) / 120.0)));
		if (shaft > 0.004F && reach > 1) {
			int rays = Effects.lod(g, radius, 10, 26);
			for (int i = 0; i < rays; i++) {
				double ang = Solids.hash(i, 11) * MathHelper.TAU + a * 0.002;
				double dist = reach * (0.25 + 0.75 * Solids.hash(i, 12)) + 2;
				Vec3d gp = g.add(Math.cos(ang) * dist, 0.3, Math.sin(ang) * dist);
				double w = 2.0 + Solids.hash(i, 13) * 3.0;
				FxDraw.ribbon(glow, m, s, gp, Vec3d.ZERO, w, 0xFFF4E0, shaft * 0.25F, shaft);
				Solids.dot(glow, m, gp, cb, w * 0.9, 0xFFFFFF, shaft * 1.6F);
			}
		}

		// --- each beat: a white wave rolling over the ground to the edge of the newly bleached ring.
		int segs = Effects.lod(g, radius, 32, 96);
		int[] beats = CelestialTimeline.LL_BEATS;
		for (int k = 0; k < beats.length; k++) {
			double w = a - beats[k];
			if (w < 0 || w >= 26) {
				continue;
			}
			double r0 = k == 0 ? 0 : CelestialTimeline.llRadius(k - 1, radius);
			double r1 = CelestialTimeline.llRadius(k, radius);
			double rad = MathHelper.lerp(Cinematics.ease(w / 14.0), r0, r1);
			float al = (float) (1 - w / 26.0);
			FxDraw.ring(glow, m, g.add(0, 0.4, 0), X, Z, Math.max(0, rad - 4), rad + 1.2, 0xFFFFFF, 0.0F, 0.7F * al, 0.0F, segs);
			FxKit.groundShock(consumers, m, g, rad, 0xFFFFFF, 0.85F * al);
			FxDraw.ring(glow, m, g.add(0, 0.3, 0), X, Z, r0, Math.max(r0 + 0.1, rad), 0xFFF8F0, 0.12F * al, 0.12F * al, 0.0F, segs);
		}

		// --- the exhale: one silent wave going all the way out, then crystal glints across the dead field.
		if (a >= exhale) {
			double e = a - exhale;
			if (e < 80) {
				double rad = radius * 1.6 * Cinematics.ease(e / 55.0);
				float al = (float) (1 - e / 80.0);
				FxDraw.ring(glow, m, g.add(0, 0.5, 0), X, Z, Math.max(0, rad - 10), rad + 2, 0xFFFFFF, 0.0F, 0.6F * al, 0.0F, segs);
				FxDraw.ring(glow, m, g.add(0, 0.5, 0), X, Z, Math.max(0, rad * 0.6 - 3), rad * 0.6 + 1, 0xE8F0FF, 0.0F, 0.35F * al, 0.0F, segs);
				FxKit.groundShock(consumers, m, g, rad, 0xF4F8FF, al);
				// White ash kicked up behind the last wave.
				java.util.List<Effects.Puff> ash = FxKit.puffs();
				FxKit.dustRing(m, cb, g, rad * 0.92, 6 + rad * 0.04, 0xF4F4F8, 0xA8A8B4, 0.55F * al, 71, ash);
				FxKit.flush(ash, consumers, m, cb);
			}
			int glints = Effects.lod(g, radius, 20, 70);
			for (int i = 0; i < glints; i++) {
				double ang = Solids.hash(i, 21) * MathHelper.TAU;
				double dist = Math.sqrt(Solids.hash(i, 22)) * radius;
				float tw = (float) Math.max(0, Math.sin(a * 0.15 + i * 1.7));
				Vec3d gp = g.add(Math.cos(ang) * dist, 0.5 + Solids.hash(i, 23) * 3, Math.sin(ang) * dist);
				Solids.dot(glow, m, gp, cb, 0.35 + 0.4 * tw, 0xF0F8FF, 0.7F * tw * fade);
			}
		}
	}
}
