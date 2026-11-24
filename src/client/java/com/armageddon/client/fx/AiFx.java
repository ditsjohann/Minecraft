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
		if (t >= AiShapes.UPLOAD && t < AiShapes.EMP && t % 2 == 0) {
			Vec3d e = c.add(0, AiShapes.EYE_Y + 2, 0);
			FxEngine.play(VfxIds.AI_UPLOAD, e.x, e.y, e.z, 0, 1, 0, 1.0F, 0, t * 37L);
		}
		return super.tick(world);
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
		Vec3d lens = e.add(toCam.multiply(4.1));
		float w = white(a);
		int hot = mix(0xFF1A10, 0xF0F4FF, w), core = mix(0xFFE0C0, 0xFFFFFF, w);
		FxDraw.disc(shade, m, lens, cb[0], cb[1], 3.4, 0x6A6E78, 0.98F, 0.98F, 32);
		FxDraw.disc(shade, m, lens.add(toCam.multiply(0.05)), cb[0], cb[1], 2.8, 0x080808, 0.98F, 0.98F, 32);
		float pulse = 0.85F + 0.15F * MathHelper.sin(a * 0.15F);
		FxDraw.disc(glow, m, lens.add(toCam.multiply(0.1)), cb[0], cb[1], 2.6, hot, 1.0F * on * pulse, 0.0F, 32);
		FxDraw.disc(glow, m, lens.add(toCam.multiply(0.12)), cb[0], cb[1], 0.8, core, 1.0F * on, 0.2F * on, 20);
		Solids.flare(glow, m, lens.add(toCam.multiply(0.15)).add(cb[0].multiply(-0.9)).add(cb[1].multiply(0.9)), cb, 0.5, 0xFFFFFF, 0.6F * on);
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
}
