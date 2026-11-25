package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.util.AiShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * AI TAKEOVER: the core's red eye, data pulsing out along the circuit traces, scanner beams sweeping the city, the
 * drone swarm, the laser walls of the purge, and the upload into the sky.
 */
final class AiFx extends FxEffect {
	private static final Vec3d UP = new Vec3d(0, 1, 0);
	private final Vec3d c;
	private final int seed;
	private final List<double[]> traces;
	private final double[] ty;

	AiFx(Vec3d c, int seed, int life) {
		super(Math.max(40, life));
		this.c = c;
		this.seed = seed;
		this.traces = AiShapes.traces(seed);
		this.ty = new double[traces.size()];
		java.util.Arrays.fill(ty, Double.NaN);
	}

	private static float smooth(double u) {
		return (float) AiShapes.smooth(u);
	}

	/** How red (0) or white (1) the eye burns: white for the purge. */
	private static float white(double a) {
		return smooth((a - AiShapes.WARN) / 30.0) * (1 - smooth((a - AiShapes.UPLOAD) / 40.0));
	}

	private static int mix(int a, int b, double t) {
		return Solids.mix(a, b, t);
	}

	@Override
	public boolean tick(ClientWorld world) {
		int t = age;
		double front = AiShapes.front(t);
		for (int i = 0; i < traces.size(); i++) {
			if (Double.isNaN(ty[i]) && traces.get(i)[2] <= front + 3) {
				ty[i] = NewFx.groundY(c.x + traces.get(i)[0], c.z + traces.get(i)[1], c.y) + 0.06;
			}
		}
		if (t >= AiShapes.BOOTED && t < AiShapes.EMP && t % 8 == 0) {
			ClientFx.light(c.add(0, AiShapes.EYE_Y, 0), 1.0F, white(t) > 0.5 ? 0xF0F0FF : 0xFF2010);
		}
		for (int k = 0; k < AiShapes.MISSILES; k++) {
			int at = AiShapes.missileAt(k);
			if (t >= at && t < at + AiShapes.MISSILE_FLIGHT) {
				Vec3d p = c.add(AiShapes.missile(k, t, seed, missileDy(k))), q = c.add(AiShapes.missile(k, t + 1, seed, missileDy(k)));
				FxEngine.playMoving(VfxIds.AI_MISSILE_TRAIL, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 131L + k, (float) (q.x - p.x), (float) (q.y - p.y),
						(float) (q.z - p.z));
			}
		}
		if (t >= AiShapes.UPLOAD && t < AiShapes.EMP && t % 2 == 0) {
			Vec3d e = c.add(0, AiShapes.EYE_Y + 2, 0);
			FxEngine.play(VfxIds.AI_UPLOAD, e.x, e.y, e.z, 0, 1, 0, 1.0F, 0, t * 37L);
		}
		return super.tick(world);
	}

	private final double[] mdy = new double[AiShapes.MISSILES];
	private final boolean[] mknown = new boolean[AiShapes.MISSILES];

	private double missileDy(int k) {
		if (!mknown[k]) {
			Vec3d tg = AiShapes.missileTarget(k, seed);
			mdy[k] = NewFx.groundY(c.x + tg.x, c.z + tg.z, c.y) - c.y;
			mknown[k] = true;
		}
		return mdy[k];
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		Vec3d o = c.subtract(cam);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		boolean alive = a >= AiShapes.BOOTED - 10 && a < AiShapes.EMP + 4;
		if (alive) {
			eye(consumers, glow, shade, solid, m, cb, o, a);
			seams(glow, m, o, a);
		}
		traces(glow, m, cb, cam, a);
		if (a >= AiShapes.NETWORK && a < AiShapes.WARN) {
			scanners(glow, m, cam, o, a);
		}
		if (a >= AiShapes.NETWORK && a < AiShapes.NETWORK_END + 20) {
			frontWall(glow, m, o, a);
		}
		if (a >= AiShapes.DRONES && a < AiShapes.EMP) {
			drones(solid, glow, m, cb, o, a);
		}
		if (a >= AiShapes.PURGE && a < AiShapes.PURGE_END + 20) {
			purge(glow, m, cb, o, a);
		}
		missiles(solid, glow, m, cb, o, a);
		if (a >= AiShapes.WALK - 40 && a < AiShapes.WALK_END + 30) {
			walkers(solid, glow, m, cb, o, cam, a);
		}
		if (a >= AiShapes.NETWORK && a < AiShapes.EMP) {
			hologram(glow, m, cb, o, a);
			glitches(glow, m, cb, o, a);
			arcs(glow, m, o, a);
		}
		if (a >= AiShapes.UPLOAD && a < AiShapes.EMP + 10) {
			float u = smooth((a - AiShapes.UPLOAD) / 20.0) * (1 - smooth((a - AiShapes.EMP) / 6.0));
			Vec3d e = o.add(0, AiShapes.EYE_Y, 0);
			FxDraw.beam(glow, m, e, e.add(0, 400, 0), Vec3d.ZERO, 2.5 * u + 0.2, 0xFF3020, 0.7F * u);
			FxDraw.beam(glow, m, e, e.add(0, 400, 0), Vec3d.ZERO, 8 * u, 0xA00808, 0.2F * u);
		}
	}

	/** The eye: a black orb, a silver-rimmed lens, a red light at its heart that tracks you and goes white for the purge. */
	private void eye(VertexConsumerProvider consumers, VertexConsumer glow, VertexConsumer shade, VertexConsumer solid, Matrix4f m, Vec3d[] cb, Vec3d o,
			float a) {
		float on = smooth((a - AiShapes.BOOTED + 10) / 15.0) * (1 - smooth((a - AiShapes.EMP) / 4.0));
		Vec3d e = o.add(0, AiShapes.EYE_Y, 0);
		Solids.sphere(solid, m, e, 4.0, 0x101014, 10, 14, true);
		Vec3d toCam = e.multiply(-1).normalize();
		// The lens floats proud of the orb, each layer well clear of the last so nothing flickers.
		Vec3d lens = e.add(toCam.multiply(4.6));
		float w = white(a);
		int hot = mix(0xFF1A10, 0xF0F4FF, w), core = mix(0xFFE0C0, 0xFFFFFF, w);
		FxDraw.disc(shade, m, lens, cb[0], cb[1], 3.6, 0x6A6E78, 0.98F, 0.98F, 32);
		FxDraw.disc(shade, m, lens.add(toCam.multiply(0.4)), cb[0], cb[1], 3.0, 0x080808, 0.98F, 0.98F, 32);
		float pulse = 0.85F + 0.15F * MathHelper.sin(a * 0.15F);
		FxDraw.disc(glow, m, lens.add(toCam.multiply(0.8)), cb[0], cb[1], 2.7, hot, 1.0F * on * pulse, 0.0F, 32);
		FxDraw.disc(glow, m, lens.add(toCam.multiply(1.0)), cb[0], cb[1], 0.9, core, 1.0F * on, 0.2F * on, 20);
		Solids.flare(glow, m, lens.add(toCam.multiply(1.2)).add(cb[0].multiply(-1.0)).add(cb[1].multiply(1.0)), cb, 0.5, 0xFFFFFF, 0.6F * on);
		FxKit.halo(consumers, m, cb, lens, 9 + 3 * w, hot, 0.35F * on * pulse);
	}

	/** Data streaming up the core's red seams. */
	private void seams(VertexConsumer glow, Matrix4f m, Vec3d o, float a) {
		float on = smooth((a - AiShapes.BOOTED) / 20.0) * (1 - smooth((a - AiShapes.EMP) / 4.0));
		for (int k = 0; k < 4; k++) {
			double ang = k * Math.PI / 2;
			Vec3d face = new Vec3d(Math.cos(ang) * 3.55, 0, Math.sin(ang) * 3.55);
			for (int j = 0; j < 6; j++) {
				double y = ((a * 0.9 + j * 9.3 + k * 4) % AiShapes.CORE_H);
				Vec3d p = o.add(face).add(0, y + 2, 0);
				FxDraw.ribbon(glow, m, p, p.add(0, 3.5, 0), Vec3d.ZERO, 0.5, 0xFF3020, 0.0F, 0.9F * on);
			}
		}
	}

	/** The circuit traces: faint red lines on the ground with bright pulses running out along them. */
	private void traces(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
		double front = AiShapes.front(a);
		float fade = 1 - smooth((a - AiShapes.EMP) / 10.0);
		for (int i = 1; i < traces.size(); i++) {
			double[] p = traces.get(i - 1), q = traces.get(i);
			if (q[2] > front || Double.isNaN(ty[i]) || Double.isNaN(ty[i - 1]) || Math.abs(p[0] - q[0]) > 1.5 || Math.abs(p[1] - q[1]) > 1.5) {
				continue;
			}
			Vec3d pa = new Vec3d(c.x + p[0] + 0.5, ty[i - 1], c.z + p[1] + 0.5).subtract(cam);
			Vec3d pb = new Vec3d(c.x + q[0] + 0.5, ty[i], c.z + q[1] + 0.5).subtract(cam);
			double ph = ((q[2] - a * 0.9) % 26 + 26) % 26;
			float pulse = (float) Math.exp(-ph * 0.35);
			FxDraw.ribbon(glow, m, pa, pb, Vec3d.ZERO, 0.35 + pulse * 0.3, 0xFF2010, (0.25F + pulse * 0.75F) * fade, (0.25F + pulse * 0.75F) * fade);
		}
	}

	/** Four scanner beams from the eye, fanning over the city as they turn. */
	private void scanners(VertexConsumer glow, Matrix4f m, Vec3d cam, Vec3d o, float a) {
		float on = smooth((a - AiShapes.NETWORK) / 20.0) * (1 - smooth((a - AiShapes.WARN + 10) / 10.0));
		Vec3d e = o.add(0, AiShapes.EYE_Y, 0);
		for (int k = 0; k < 4; k++) {
			double ang = a * 0.018 + k * Math.PI / 2;
			for (int f = -3; f <= 3; f++) {
				double aa = ang + f * 0.04;
				double r = 40 + 60 * (0.5 + 0.5 * Math.sin(a * 0.02 + k));
				double x = c.x + Math.cos(aa) * r, z = c.z + Math.sin(aa) * r;
				Vec3d g = new Vec3d(x, NewFx.groundY(x, z, c.y) + 0.2, z).subtract(cam);
				FxDraw.ribbon(glow, m, e, g, Vec3d.ZERO, 0.12, 0xFF2010, 0.1F * on, 0.5F * on);
			}
		}
	}

	/** A wall of red hologram racing outward with the assimilation, ruled with scan lines. */
	private void frontWall(VertexConsumer glow, Matrix4f m, Vec3d o, float a) {
		double r = AiShapes.front(a);
		float on = smooth((a - AiShapes.NETWORK) / 10.0) * (1 - smooth((a - AiShapes.NETWORK_END) / 20.0));
		if (r < 3) {
			return;
		}
		int n = 96;
		for (int k = 0; k < n; k++) {
			double a0 = k * MathHelper.TAU / n, a1 = (k + 1) * MathHelper.TAU / n;
			Vec3d b0 = o.add(Math.cos(a0) * r, -2, Math.sin(a0) * r), b1 = o.add(Math.cos(a1) * r, -2, Math.sin(a1) * r);
			FxDraw.vertex(glow, m, b0.x, b0.y, b0.z, 0xFF2010, 0.35F * on);
			FxDraw.vertex(glow, m, b1.x, b1.y, b1.z, 0xFF2010, 0.35F * on);
			FxDraw.vertex(glow, m, b1.x, b1.y + 14, b1.z, 0xFF2010, 0.0F);
			FxDraw.vertex(glow, m, b0.x, b0.y + 14, b0.z, 0xFF2010, 0.0F);
			for (int j = 1; j < 4; j++) {
				double y = ((a * 0.4 + j * 4) % 14) - 2;
				FxDraw.ribbon(glow, m, b0.add(0, y + 2, 0), b1.add(0, y + 2, 0), Vec3d.ZERO, 0.12, 0xFF4030, 0.4F * on * (float) (1 - y / 14), 0.4F * on * (float) (1 - y / 14));
			}
		}
	}

	/** The drones: little black quad-rotors with a red eye each, weaving round the city. */
	private void drones(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		for (int i = 0; i < AiShapes.DRONE_COUNT; i++) {
			if (a < AiShapes.launch(i)) {
				continue;
			}
			Vec3d p = o.add(AiShapes.drone(i, a, seed));
			Vec3d v = AiShapes.drone(i, a + 1, seed).subtract(AiShapes.drone(i, a, seed));
			Vec3d f = new Vec3d(v.x, 0, v.z);
			f = f.lengthSquared() < 1.0E-5 ? new Vec3d(1, 0, 0) : f.normalize();
			Vec3d s = new Vec3d(-f.z, 0, f.x);
			Solids.tube(solid, m, p.subtract(f.multiply(0.7)), p.add(f.multiply(0.7)), 0.45, 0.35, 6, 0x18181C, 1.0F, true);
			for (int k = 0; k < 4; k++) {
				Vec3d arm = f.multiply(k < 2 ? 0.9 : -0.9).add(s.multiply(k % 2 == 0 ? 0.9 : -0.9));
				Solids.tube(solid, m, p, p.add(arm).add(0, 0.15, 0), 0.08, 0.08, 4, 0x18181C, 1.0F, true);
				FxDraw.ring(glow, m, p.add(arm).add(0, 0.25, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 0.35, 0.6, 0xC0C0D0, 0.0F, 0.25F, 0.0F, 10);
			}
			float blink = ((int) (a * 0.5 + i)) % 8 == 0 ? 1.0F : 0.75F;
			Solids.flare(glow, m, p.add(f.multiply(0.75)), cb, 0.45, 0xFF2010, blink);
			FxDraw.ribbon(glow, m, p, p.subtract(v.multiply(5)), Vec3d.ZERO, 0.12, 0xFF3020, 0.3F, 0.0F);
		}
	}

	/** The purge: walls of red laser light along the grid, cutting outward from the core. */
	private void purge(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		double r = AiShapes.purge(a);
		float on = 1 - smooth((a - AiShapes.PURGE_END) / 20.0);
		if (r < 2) {
			return;
		}
		double g = AiShapes.GRID;
		for (int axis = 0; axis < 2; axis++) {
			for (double line = -Math.floor(AiShapes.R / g) * g - g / 2 + 1; line <= AiShapes.R; line += g) {
				if (Math.abs(line) > r) {
					continue;
				}
				double half = Math.sqrt(r * r - line * line);
				Vec3d along = axis == 0 ? new Vec3d(0, 0, 1) : new Vec3d(1, 0, 0);
				Vec3d at = axis == 0 ? new Vec3d(line, 0, 0) : new Vec3d(0, 0, line);
				Vec3d p0 = o.add(at).subtract(along.multiply(half)).add(0, -3, 0), p1 = o.add(at).add(along.multiply(half)).add(0, -3, 0);
				float fl = 0.8F + 0.2F * MathHelper.sin(a * 1.3F + (float) line);
				FxDraw.vertex(glow, m, p0.x, p0.y, p0.z, 0xFF2010, 0.45F * on * fl);
				FxDraw.vertex(glow, m, p1.x, p1.y, p1.z, 0xFF2010, 0.45F * on * fl);
				FxDraw.vertex(glow, m, p1.x, p1.y + 95, p1.z, 0xFF2010, 0.0F);
				FxDraw.vertex(glow, m, p0.x, p0.y + 95, p0.z, 0xFF2010, 0.0F);
				FxDraw.ribbon(glow, m, p0.add(0, 3.5, 0), p1.add(0, 3.5, 0), Vec3d.ZERO, 0.4, 0xFFE0D0, 0.9F * on, 0.9F * on);
				// The emitters at the leading ends.
				Solids.flare(glow, m, p0.add(0, 3.5, 0), cb, 2.4, 0xFFFFFF, on);
				Solids.flare(glow, m, p1.add(0, 3.5, 0), cb, 2.4, 0xFFFFFF, on);
			}
		}
	}

	/** Missiles in flight: slim black bodies with a red-hot nose and a burning tail. */
	private void missiles(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		for (int k = 0; k < AiShapes.MISSILES; k++) {
			int at = AiShapes.missileAt(k);
			if (a < at || a >= at + AiShapes.MISSILE_FLIGHT) {
				continue;
			}
			Vec3d p = o.add(AiShapes.missile(k, a, seed, missileDy(k)));
			Vec3d d = AiShapes.missile(k, a + 0.5, seed, missileDy(k)).subtract(AiShapes.missile(k, a - 0.5, seed, missileDy(k)));
			d = d.lengthSquared() < 1.0E-6 ? UP : d.normalize();
			Solids.tube(solid, m, p.subtract(d.multiply(2.5)), p.add(d.multiply(1.5)), 0.45, 0.45, 6, 0x18181C, 1.0F, true);
			Solids.tube(solid, m, p.add(d.multiply(1.5)), p.add(d.multiply(2.6)), 0.45, 0.02, 6, 0xC01010, 1.0F, true);
			Solids.flare(glow, m, p.subtract(d.multiply(2.7)), cb, 1.8, 0xFFB060, 1.0F);
		}
	}

	/**
	 * The tripods: war machines rising out of the streets on three long jointed legs, a black hull with a red eye,
	 * striding across the city sweeping a beam over everything in front of them.
	 */
	private void walkers(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, Vec3d cam, float a) {
		double up = AiShapes.walkerUp(a);
		if (up <= 0.01) {
			return;
		}
		for (int i = 0; i < AiShapes.WALKERS; i++) {
			Vec3d w = AiShapes.walker(i, Math.max(a, AiShapes.WALK), seed);
			double gy = NewFx.groundY(c.x + w.x, c.z + w.z, c.y) - c.y;
			Vec3d foot = o.add(w.x, gy, w.z);
			double bob = Math.sin(a * 0.2 + i) * 1.2;
			Vec3d hull = foot.add(0, (AiShapes.WALKER_H + bob) * up - (1 - up) * 10, 0);
			Vec3d d = AiShapes.walker(i, a + 1, seed).subtract(AiShapes.walker(i, a, seed));
			d = new Vec3d(d.x, 0, d.z);
			d = d.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : d.normalize();
			// Three legs, each stepping in turn.
			for (int k = 0; k < 3; k++) {
				double ang = k * MathHelper.TAU / 3 + Math.atan2(d.z, d.x);
				double step = Math.sin(a * 0.12 + k * 2.1);
				Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
				Vec3d hip = hull.add(out.multiply(3.5));
				Vec3d toe = foot.add(out.multiply(16)).add(d.multiply(step * 5)).add(0, Math.max(0, step) * 3, 0);
				Vec3d knee = hip.lerp(toe, 0.5).add(out.multiply(4)).add(0, 10 * up, 0);
				Solids.tube(solid, m, hip, knee, 0.9, 0.7, 6, 0x202024, 1.0F, true);
				Solids.tube(solid, m, knee, toe, 0.7, 0.3, 6, 0x202024, 1.0F, true);
				Solids.dot(glow, m, knee, cb, 0.6, 0xFF2010, 0.8F);
			}
			// The hull: a squat black dome with a ring of red lights and a red eye under it.
			Solids.sphere(solid, m, hull, 5.0, 0x141418, 8, 12, true);
			Solids.tube(solid, m, hull.add(0, -1.2, 0), hull.add(0, -2.6, 0), 5.6, 4.0, 14, 0x2A2A30, 1.0F, true);
			for (int k = 0; k < 10; k++) {
				double ang = k * MathHelper.TAU / 10 + a * 0.05;
				Solids.dot(glow, m, hull.add(Math.cos(ang) * 5.4, -1.6, Math.sin(ang) * 5.4), cb, 0.5, 0xFF2010, ((int) (a * 0.5) + k) % 3 == 0 ? 1.0F : 0.4F);
			}
			Vec3d eye = hull.add(d.multiply(3.2)).add(0, -2.4, 0);
			Solids.flare(glow, m, eye, cb, 2.2, 0xFF2010, (float) up);
			// Its beam sweeping the ground ahead.
			if (a >= AiShapes.WALK && a < AiShapes.WALK_END) {
				Vec3d b = AiShapes.beam(i, a, seed);
				Vec3d hit = new Vec3d(c.x + b.x, NewFx.groundY(c.x + b.x, c.z + b.z, c.y), c.z + b.z).subtract(cam);
				float fl = 0.8F + 0.2F * MathHelper.sin(a * 2.3F + i);
				FxDraw.beam(glow, m, eye, hit, Vec3d.ZERO, 0.6, 0xFFE0D0, fl);
				FxDraw.beam(glow, m, eye, hit, Vec3d.ZERO, 2.4, 0xFF1A10, 0.45F * fl);
				Solids.flare(glow, m, hit, cb, 3.5, 0xFFA060, fl);
			}
		}
	}

	/** A giant hologram of the eye hanging over the city, flickering, ringed with turning glyphs and warning bars. */
	private void hologram(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		float on = smooth((a - AiShapes.NETWORK - 40) / 40.0) * (1 - smooth((a - AiShapes.UPLOAD) / 20.0));
		if (on <= 0.01F) {
			return;
		}
		float flick = ((int) (a * 1.3)) % 17 == 0 ? 0.2F : 1.0F;
		float w = white(a);
		int col = mix(0xFF2010, 0xE0F0FF, w);
		Vec3d p = o.add(0, AiShapes.EYE_Y + 42, 0);
		// The face: concentric rings with a pupil that tracks the camera, scan lines through it.
		FxDraw.ring(glow, m, p, cb[0], cb[1], 15, 17, col, 0.0F, 0.6F * on * flick, 0.0F, 48);
		FxDraw.ring(glow, m, p, cb[0], cb[1], 9, 10.5, col, 0.0F, 0.5F * on * flick, 0.0F, 40);
		Vec3d toCam = p.multiply(-1).normalize();
		Vec3d pupil = p.add(cb[0].multiply(toCam.dotProduct(cb[0]) * 3)).add(cb[1].multiply(toCam.dotProduct(cb[1]) * 3));
		FxDraw.disc(glow, m, pupil, cb[0], cb[1], 4.5, col, 0.7F * on * flick, 0.1F * on, 24);
		for (int k = -6; k <= 6; k++) {
			double y = k * 2.6 + ((a * 0.4) % 2.6);
			double half = Math.sqrt(Math.max(0, 17 * 17 - y * y));
			FxDraw.ribbon(glow, m, p.add(cb[1].multiply(y)).add(cb[0].multiply(-half)), p.add(cb[1].multiply(y)).add(cb[0].multiply(half)), Vec3d.ZERO, 0.15, col,
					0.15F * on * flick, 0.15F * on * flick);
		}
		// Rings of glyphs turning round it.
		for (int r = 0; r < 2; r++) {
			int n = 34;
			for (int k = 0; k < n; k++) {
				double ang = k * MathHelper.TAU / n + a * 0.01 * (r == 0 ? 1 : -1);
				double rr = 22 + r * 5;
				Vec3d q = p.add(cb[0].multiply(Math.cos(ang) * rr)).add(cb[1].multiply(Math.sin(ang) * rr));
				Solids.glyph(glow, m, q, cb[0], cb[1], 1.8, k * 13 + r * 71 + (int) (a / 8), col, 0.7F * on * flick);
			}
		}
		// Warning bars stacked beneath it.
		for (int k = 0; k < 3; k++) {
			Vec3d b = p.add(cb[1].multiply(-30 - k * 4));
			double len = 16 - k * 3;
			boolean blink = ((int) (a * 0.25) + k) % 2 == 0;
			FxDraw.ribbon(glow, m, b.add(cb[0].multiply(-len)), b.add(cb[0].multiply(len)), Vec3d.ZERO, 2.2, col, (blink ? 0.6F : 0.2F) * on, (blink ? 0.6F : 0.2F) * on);
		}
	}

	/** Glitches in the air over the city: cubes of wrong colour jittering, split into red and cyan. */
	private void glitches(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		float on = smooth((a - AiShapes.NETWORK_END + 100) / 60.0) * (1 - smooth((a - AiShapes.EMP) / 10.0));
		if (on <= 0.01F) {
			return;
		}
		for (int k = 0; k < 50; k++) {
			int epoch = (int) (a / 6) + k * 7;
			double ang = AiShapes.hash(k, epoch) * MathHelper.TAU, r = 15 + AiShapes.hash(epoch, k) * (AiShapes.R - 20);
			double y = 4 + AiShapes.hash(k + 3, epoch) * 60;
			Vec3d p = o.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
			double sz = 0.8 + AiShapes.hash(epoch, k + 5) * 2.2;
			float al = 0.5F * on * (AiShapes.hash(epoch, k + 9) < 0.7 ? 1 : 0);
			Vec3d jit = cb[0].multiply(Math.sin(a * 3 + k) * 0.4);
			FxDraw.disc(glow, m, p.add(jit), cb[0], cb[1], sz, 0xFF2020, al, al, 4);
			FxDraw.disc(glow, m, p.subtract(jit).add(cb[0].multiply(0.5)), cb[0], cb[1], sz, 0x20E0FF, al * 0.8F, al * 0.8F, 4);
		}
	}

	/** Red lightning crackling between the rooftops and the core. */
	private void arcs(VertexConsumer glow, Matrix4f m, Vec3d o, float a) {
		float on = smooth((a - AiShapes.NETWORK - 60) / 40.0) * (1 - smooth((a - AiShapes.EMP) / 6.0));
		if (on <= 0.01F) {
			return;
		}
		for (int k = 0; k < 5; k++) {
			int epoch = (int) (a / 5) + k * 31;
			if (AiShapes.hash(epoch, k) < 0.45) {
				continue;
			}
			java.util.Random rnd = new java.util.Random(epoch * 7919L + k);
			double ang = rnd.nextDouble() * MathHelper.TAU, r = 20 + rnd.nextDouble() * (AiShapes.R - 30);
			Vec3d from = o.add(0, AiShapes.CORE_H * (0.4 + rnd.nextDouble() * 0.5), 0);
			Vec3d to = o.add(Math.cos(ang) * r, 15 + rnd.nextDouble() * 35, Math.sin(ang) * r);
			Vec3d prev = from;
			for (int j = 1; j <= 10; j++) {
				Vec3d q = from.lerp(to, j / 10.0).add(rnd.nextGaussian() * 2.5, rnd.nextGaussian() * 2.5, rnd.nextGaussian() * 2.5);
				if (j == 10) {
					q = to;
				}
				FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.35, 0xFF3020, 0.9F * on, 0.9F * on);
				FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 1.6, 0xA00808, 0.25F * on, 0.25F * on);
				prev = q;
			}
		}
	}
}
