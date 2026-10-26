package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.NewBombShapes;
import com.armageddon.util.NewBombShapes.Ice;
import com.armageddon.util.NewBombShapes.Lidar;
import com.armageddon.util.NewBombShapes.Sun;
import com.armageddon.util.NewBombShapes.Termite;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Client effects of the strong bombs (lidar mast and strike reticles, the shadow void's tendrils, the spectral ice
 * pyramid's glow and shard storm, the termite swarm, the sun's beam, eclipse and corpse). The kinds at 10 and up
 * belong to the oddities ({@link OddFx}). Geometry and timing come from {@link NewBombShapes}.
 */
public final class NewFx {
	private NewFx() {
	}

	static FxEffect create(FxPayload p) {
		Vec3d a = new Vec3d(p.x(), p.y(), p.z());
		Vec3d b = new Vec3d(p.x2(), p.y2(), p.z2());
		return switch (p.seed()) {
			case 0 -> new LidarMast(a, p.duration());
			case 1 -> new LidarReticle(a, (int) p.p1(), p.duration());
			case 2 -> new VoidTendrils(a, b, p.scale(), p.p1(), p.duration());
			case 3 -> new IcePyramid(a, p.duration());
			case 4 -> new Termites(a, p.duration());
			case 5 -> new SunDeath(a, b, p.duration());
			default -> OddFx.create(p.seed(), a, b, p);
		};
	}

	/** Ground height at a column, from the client's own world. */
	static double groundY(double x, double z, double fallback) {
		ClientWorld w = com.armageddon.client.render.FxDraw.world();
		if (w == null) {
			return fallback;
		}
		int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
		if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
			return fallback;
		}
		return w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
	}

	// ====================================================================== LIDAR

	/** The survey mast: a spinning head throwing a fan of laser lines over the land, white-hot when it overloads. */
	static final class LidarMast extends FxEffect {
		private final Vec3d g;

		LidarMast(Vec3d g, int life) {
			super(life);
			this.g = g;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			double rise = MathHelper.clamp(t / 25.0, 0.0, 1.0);
			Vec3d base = g.subtract(cam);
			Vec3d head = base.add(0, 1 + 7 * rise, 0);
			Solids.tube(solid, m, base, head, 0.35, 0.25, 8, 0x2A2E36, 1.0F, true);
			Solids.tube(solid, m, head.add(0, -0.6, 0), head.add(0, 0.6, 0), 0.9, 0.9, 10, 0x3A404C, 1.0F, true);
			boolean over = t > Lidar.OVERLOAD - 30;
			float heat = over ? MathHelper.clamp((t - (Lidar.OVERLOAD - 30)) / 30.0F, 0.0F, 1.0F) : 0.0F;
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Solids.flare(glow, m, head, cb, 1.2 + heat * 6, over ? 0xFFFFFF : 0x60FFFF, 0.8F);
			if (t < Lidar.SCAN - 5 || t > Lidar.OVERLOAD + 4) {
				return;
			}
			// The laser fan: 32 lines spinning fast, a few degrees under the horizon, plus a vertical sheet.
			double spin = t * 0.45;
			for (int i = 0; i < 32; i++) {
				double ang = spin + i * MathHelper.TAU / 32;
				double dip = -0.05 - 0.22 * ((i * 7) % 5) / 5.0;
				Vec3d dir = new Vec3d(Math.cos(ang) * Math.cos(dip), Math.sin(dip), Math.sin(ang) * Math.cos(dip));
				double len = 40 + 30 * NewBombShapes.hash(i, (int) (t / 3));
				FxDraw.ribbon(glow, m, head, head.add(dir.multiply(len)), Vec3d.ZERO, 0.06, 0x80FFFF, 0.55F, 0.0F);
			}
			for (int k = 0; k < 12; k++) {
				double ang = t * 0.11;
				double el = -0.6 + k * 0.1;
				Vec3d dir = new Vec3d(Math.cos(ang) * Math.cos(el), Math.sin(el), Math.sin(ang) * Math.cos(el));
				FxDraw.ribbon(glow, m, head, head.add(dir.multiply(60)), Vec3d.ZERO, 0.05, 0xC0FFFF, 0.4F, 0.0F);
			}
		}
	}

	/** A lock-on: rotating red brackets closing in on the mark and a thin laser from the sky, then the strike column. */
	static final class LidarReticle extends FxEffect {
		private final Vec3d p;
		private final int delay;

		LidarReticle(Vec3d p, int delay, int life) {
			super(life);
			this.p = p;
			this.delay = delay;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW_NO_DEPTH);
			Vec3d c = p.subtract(cam).add(0, 0.3, 0);
			if (t < delay) {
				float k = t / delay;
				double size = 6 - 4 * k;
				double rot = t * 0.08;
				for (int i = 0; i < 4; i++) {
					double a0 = rot + i * Math.PI / 2;
					Vec3d corner = c.add(Math.cos(a0) * size, 0, Math.sin(a0) * size);
					Vec3d e1 = new Vec3d(Math.cos(a0 + 2.6), 0, Math.sin(a0 + 2.6)).multiply(size * 0.5);
					Vec3d e2 = new Vec3d(Math.cos(a0 - 2.6), 0, Math.sin(a0 - 2.6)).multiply(size * 0.5);
					FxDraw.ribbon(glow, m, corner, corner.add(e1), Vec3d.ZERO, 0.25, 0xFF2020, 0.9F, 0.9F);
					FxDraw.ribbon(glow, m, corner, corner.add(e2), Vec3d.ZERO, 0.25, 0xFF2020, 0.9F, 0.9F);
				}
				float blink = (int) (t / 3) % 2 == 0 ? 1.0F : 0.5F;
				FxDraw.ribbon(glow, m, c, c.add(0, 240, 0), Vec3d.ZERO, 0.08 + 0.4 * k, 0xFF3020, 0.5F * blink * k, 0.0F);
			} else {
				float k = 1 - MathHelper.clamp((t - delay) / 14.0F, 0.0F, 1.0F);
				FxDraw.beam(consumers.getBuffer(FxRenderLayers.GLOW), m, c, c.add(0, 260, 0), Vec3d.ZERO, 3.0 * k + 0.2, 0xFF4020, k);
			}
		}
	}

	// ====================================================================== SHADOW VOID

	/** The rising black orb and its ink: tendrils reaching down to the ground, swaying, and drifting shadow motes. */
	static final class VoidTendrils extends FxEffect {
		private static final int RISE = 50, EXHALE = 345;
		private final Vec3d orb, g;
		private final float maxR, core;

		VoidTendrils(Vec3d orb, Vec3d g, float maxR, float core, int life) {
			super(life);
			this.orb = orb;
			this.g = g;
			this.maxR = maxR;
			this.core = core;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < EXHALE) {
				ClientFx.tintSky(0x05000A, 0.9F);
				Vec3d c = center(age);
				for (int i = 0; i < 4; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double d = 8 + world.random.nextDouble() * maxR * 1.4;
					world.addParticle(ParticleTypes.SQUID_INK, c.x + Math.cos(a) * d, c.y - world.random.nextDouble() * 20, c.z + Math.sin(a) * d,
							-Math.cos(a) * 0.4, 0.15, -Math.sin(a) * 0.4);
				}
			}
			return super.tick(world);
		}

		private Vec3d center(float t) {
			return g.add(orb.subtract(g).multiply(MathHelper.clamp(t / RISE, 0.0F, 1.0F)));
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			if (t > EXHALE + 30) {
				return;
			}
			Vec3d c = center(t).subtract(cam);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			float fade = t > EXHALE ? 1 - (t - EXHALE) / 30.0F : 1.0F;
			if (t < RISE + 10) {
				// Before the vision takes over: a small black pearl with a violet rim.
				Solids.sphere(consumers.getBuffer(FxRenderLayers.SOLID), m, c, 1.5 + t * 0.04, 0x000000, 10, 16, false);
				FxDraw.sphere(glow, m, c, 2.2 + t * 0.05, Vec3d.ZERO, 0x9040FF, 0.0F, 0.6F, 10, 16);
			}
			double r = 4 + (maxR - 4) * MathHelper.clamp((t - RISE) / 240.0F, 0.0F, 1.0F);
			int n = 18;
			for (int i = 0; i < n; i++) {
				double a = i * MathHelper.TAU / n + t * 0.01 + NewBombShapes.hash(i, 1);
				double reach = r * (0.9 + 0.7 * NewBombShapes.hash(i, 2));
				Vec3d end = new Vec3d(c.x + Math.cos(a) * reach, g.y - cam.y, c.z + Math.sin(a) * reach);
				Vec3d prev = c;
				int seg = 14;
				for (int k = 1; k <= seg; k++) {
					double u = k / (double) seg;
					Vec3d lin = c.add(end.subtract(c).multiply(u));
					double sway = Math.sin(t * 0.07 + i * 1.7 + u * 5) * 3.5 * Math.sin(u * Math.PI);
					Vec3d side = new Vec3d(-Math.sin(a), 0, Math.cos(a)).multiply(sway);
					Vec3d pt = lin.add(side).add(0, Math.sin(u * Math.PI) * r * 0.25, 0);
					double w = (1 - u) * 2.2 + 0.3;
					FxDraw.ribbon(shade, m, prev, pt, Vec3d.ZERO, w, 0x07020D, 0.85F * fade, 0.85F * fade);
					FxDraw.ribbon(glow, m, prev, pt, Vec3d.ZERO, w * 0.25, 0x6020B0, 0.25F * fade, 0.25F * fade);
					prev = pt;
				}
			}
			// Faint motes being drawn in.
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			for (int i = 0; i < 70; i++) {
				double ph = (t * 0.006 + NewBombShapes.hash(i, 3)) % 1.0;
				double a = NewBombShapes.hash(i, 4) * MathHelper.TAU + ph * 3;
				double el = (NewBombShapes.hash(i, 5) - 0.5) * 2.4;
				double d = (r * 2.2) * (1 - ph) + core;
				Vec3d p = c.add(Math.cos(a) * Math.cos(el) * d, Math.sin(el) * d, Math.sin(a) * Math.cos(el) * d);
				FxDraw.disc(shade, m, p, cb[0], cb[1], 0.5 + 0.8 * (1 - ph), 0x000000, 0.7F * fade, 0.0F, 6);
			}
		}
	}

	// ====================================================================== SPECTRAL ICE

	/** The apex light, the spikes' glow climbing to a frenzy, the white burst and the shard storm. */
	static final class IcePyramid extends FxEffect {
		private final Vec3d g;
		private final Vec3d[] bases, tips;
		private final Vec3d[] lands = new Vec3d[Ice.SHARDS];

		IcePyramid(Vec3d g, int life) {
			super(life);
			this.g = g;
			this.bases = Ice.bases(g);
			this.tips = new Vec3d[bases.length];
			for (int i = 0; i < bases.length; i++) {
				tips[i] = Ice.tip(g, bases[i], i, bases.length);
			}
			for (int i = 0; i < Ice.SHARDS; i++) {
				Vec3d l = Ice.shardTarget(g, i);
				lands[i] = new Vec3d(l.x, groundY(l.x, l.z, g.y), l.z);
			}
		}

		/** Spike glow 0..1+: nothing, a steady glow, then the frenzy before the spray, then a slow fade. */
		private float glow(float t) {
			if (t < Ice.GLOW) {
				return 0;
			}
			if (t < Ice.SPRAY) {
				float k = (t - Ice.GLOW) / (Ice.SPRAY - Ice.GLOW);
				float flick = 0.75F + 0.25F * MathHelper.sin(t * (0.5F + k * 1.6F));
				return (0.35F + k * 0.9F) * flick;
			}
			return Math.max(0, 1.2F - (t - Ice.SPRAY) / 140.0F);
		}

		@Override
		public boolean tick(ClientWorld world) {
			float t = age;
			Vec3d apex = Ice.apex(g);
			if (t >= Ice.APEX) {
				boolean white = t >= Ice.SPRAY - 8 && t < Ice.SPRAY + Ice.SPRAY_LEN;
				ClientFx.light(apex.add(0, 2, 0), white ? 1.4F : 0.6F + glow(t) * 0.5F, white ? 0xF0FCFF : 0x60A8FF);
			}
			if (t > Ice.ERUPT && t < Ice.END - 100) {
				ClientFx.tintSky(0x0C2850, 0.5F);
			}
			if (t >= Ice.GLOW && t < Ice.SPRAY && age % 2 == 0) {
				int i = world.random.nextInt(bases.length);
				double u = world.random.nextDouble();
				Vec3d p = bases[i].add(tips[i].subtract(bases[i]).multiply(u));
				world.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0.05, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Vec3d apex = Ice.apex(g).subtract(cam);
			// Frost mist rolling off the base while the spikes erupt.
			if (t > Ice.ERUPT && t < Ice.ERUPT_END + 60) {
				float k = Math.min(1, (t - Ice.ERUPT) / 20.0F) * Math.min(1, (Ice.ERUPT_END + 60 - t) / 40.0F);
				FxDraw.disc(glow, m, g.subtract(cam).add(0, 0.6, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Ice.HALF * 1.7, 0x6098D0, 0.25F * k, 0.0F, 32);
			}
			float gl = glow(t);
			if (gl > 0.01F) {
				for (int i = 0; i < bases.length; i++) {
					Vec3d b = bases[i].subtract(cam), tip = tips[i].subtract(cam);
					double len = tip.distanceTo(b);
					double r0 = Ice.baseRadius(i, bases.length) + 0.6;
					int col = Solids.mix(0x2060FF, 0xA0E8FF, Math.min(1, gl * 0.8));
					Solids.tube(glow, m, b, tip, r0, 0.5, 10, Solids.scale(col, 0.5), 0.22F * Math.min(gl, 1.2F), false);
					// Bands of light running up the spike to the apex.
					for (int k = 0; k < 3; k++) {
						double u = ((t * (0.012 + gl * 0.02) + k / 3.0 + i * 0.13) % 1.0);
						Vec3d c = b.add(tip.subtract(b).multiply(u));
						double rr = r0 * (1 - u) + 0.6;
						Vec3d dir = tip.subtract(b).multiply(1 / len);
						Solids.tube(glow, m, c, c.add(dir.multiply(2.2)), rr + 0.2, rr * 0.9 + 0.2, 10, 0xC0F4FF, 0.55F * Math.min(gl, 1.0F), false);
					}
				}
			}
			if (t >= Ice.APEX) {
				boolean white = t >= Ice.SPRAY - 8;
				float pulse = 1 + 0.15F * MathHelper.sin(t * 0.4F);
				double size = (white ? 9 : 3 + gl * 4) * pulse;
				float fade = t > Ice.SPRAY + Ice.SPRAY_LEN ? Math.max(0, 1 - (t - Ice.SPRAY - Ice.SPRAY_LEN) / 80.0F) : 1.0F;
				Solids.flare(glow, m, apex.add(0, 2, 0), cb, size, white ? 0xE8FAFF : 0x60B0FF, fade);
				FxDraw.sphere(glow, m, apex.add(0, 2, 0), size * 0.5, Vec3d.ZERO, white ? 0xFFFFFF : 0x80C8FF, 0.5F * fade, 0.0F, 8, 12);
			}
			// The shards in flight: needles of light with a fading trail.
			for (int i = 0; i < Ice.SHARDS; i++) {
				float k = (t - Ice.shardLaunch(i)) / Ice.shardFlight(i);
				if (k < 0 || k > 1.05) {
					continue;
				}
				double kk = Math.min(1, k);
				Vec3d p = Ice.shardAt(g, lands[i], kk).subtract(cam);
				Vec3d prev = Ice.shardAt(g, lands[i], Math.max(0, kk - 0.06)).subtract(cam);
				Vec3d dir = p.subtract(prev);
				if (dir.lengthSquared() < 1.0E-6) {
					continue;
				}
				dir = dir.normalize();
				Solids.tube(glow, m, p.subtract(dir.multiply(2.4)), p.add(dir.multiply(0.8)), 0.45, 0.0, 6, 0xD8F6FF, 0.95F, false);
				Vec3d tail = Ice.shardAt(g, lands[i], Math.max(0, kk - 0.25)).subtract(cam);
				FxDraw.ribbon(glow, m, tail, p, Vec3d.ZERO, 0.5, 0x60B8FF, 0.0F, 0.6F);
			}
		}
	}

	// ====================================================================== TERMITE DOMAIN

	/**
	 * Two and a half thousand ants. They boil out of the ground, march with the eating front, run home when the
	 * land is bare and climb all over the rising hill. Each is a camera-facing body with a twitching head, so they
	 * read on the ground and on the hill's walls alike.
	 */
	static final class Termites extends FxEffect {
		private static final int ANTS = 2600;
		private final Vec3d g;
		private final Map<Long, Double> heights = new HashMap<>();
		private final double[] px = new double[ANTS], py = new double[ANTS], pz = new double[ANTS];
		private final double[] ox = new double[ANTS], oy = new double[ANTS], oz = new double[ANTS];
		private final boolean[] shown = new boolean[ANTS];

		Termites(Vec3d g, int life) {
			super(life);
			this.g = g;
		}

		private double ground(double x, double z) {
			long key = ((long) MathHelper.floor(x) << 32) ^ (MathHelper.floor(z) & 0xFFFFFFFFL);
			return heights.computeIfAbsent(key, k -> groundY(x, z, g.y));
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 40 == 0) {
				heights.clear();
			}
			double t = age;
			double front = Termite.front(t);
			double hillH = Termite.built(t) * Termite.HILL_H;
			for (int i = 0; i < ANTS; i++) {
				ox[i] = px[i];
				oy[i] = py[i];
				oz[i] = pz[i];
				double h1 = NewBombShapes.hash(i, 1), h2 = NewBombShapes.hash(i, 2), h3 = NewBombShapes.hash(i, 3);
				double born = Termite.EMERGE + h1 * 50;
				shown[i] = t >= born;
				if (!shown[i]) {
					continue;
				}
				double a = h2 * MathHelper.TAU + Math.sin(t * 0.02 + i) * 0.08;
				double x, y, z;
				if (t < Termite.EAT_END + 10) {
					// With the front: most ants right at it, a tail of stragglers behind.
					double lag = Math.pow(h3, 2.2) * 26;
					double r = Math.max(0, Math.min(front, (t - born) * 0.7) - lag) + Math.sin(t * 0.3 + i * 7) * 0.4;
					x = g.x + Math.cos(a) * r;
					z = g.z + Math.sin(a) * r;
					y = ground(x, z);
				} else if (t < Termite.BUILD + 20 || h1 < 0.3) {
					// Home: a march back in, ending at the foot of the hill.
					double k = MathHelper.clamp((t - Termite.EAT_END - 10 - h3 * 20) / 60.0, 0.0, 1.0);
					double rEnd = Termite.HILL_R + 2 + h3 * 10;
					double rStart = Math.max(4, Termite.RADIUS - Math.pow(h3, 2.2) * 26);
					double r = MathHelper.lerp(k, rStart, rEnd) + Math.sin(t * 0.3 + i * 7) * 0.5;
					x = g.x + Math.cos(a) * r;
					z = g.z + Math.sin(a) * r;
					y = ground(x, z);
				} else if (t >= Termite.FLIGHT && h1 > 0.3) {
					// The nuptial flight: up off the hill in a spiralling column, then out with the swarm front.
					double k = MathHelper.clamp((t - Termite.FLIGHT - h3 * 60) / 70.0, 0.0, 1.0);
					double fly = Termite.flightFront(t) * (0.75 + 0.25 * h2) * Math.min(1.0, (t - Termite.FLIGHT) / 120.0);
					double aa = a + t * (0.03 + h3 * 0.02);
					double r = MathHelper.lerp(k, 3 + h2 * 6, Math.max(8, fly));
					double alt = MathHelper.lerp(k, Termite.HILL_H * Termite.built(t) + 4 + h3 * 30, 6 + h3 * 22) + Math.sin(t * 0.2 + i) * 1.5;
					x = g.x + Math.cos(aa) * r;
					z = g.z + Math.sin(aa) * r;
					y = (k < 1 ? g.y - 1 : ground(x, z)) + alt;
				} else {
					// Climbing the hill in spirals, up to the current top, then back down.
					double cyc = (t * (0.004 + h3 * 0.004) + h2) % 2.0;
					double u = cyc < 1 ? cyc : 2 - cyc;
					double yy = u * Math.max(1, hillH - 1);
					double aa = a + u * 2.5 * (h1 < 0.65 ? 1 : -1);
					double r = Termite.radius(yy, aa) + 0.15;
					x = g.x + Math.cos(aa) * r;
					z = g.z + Math.sin(aa) * r;
					y = g.y - 1 + yy;
				}
				px[i] = x;
				py[i] = y + 0.08;
				pz[i] = z;
				if (oy[i] == 0) {
					ox[i] = px[i];
					oy[i] = py[i];
					oz[i] = pz[i];
				}
			}
			if (t > Termite.EMERGE && t < Termite.EAT_END && age % 2 == 0) {
				double a = world.random.nextDouble() * MathHelper.TAU;
				double x = g.x + Math.cos(a) * front, z = g.z + Math.sin(a) * front;
				world.addParticle(ParticleTypes.ASH, x, ground(x, z) + 0.3, z, 0, 0.05, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			float fade = Math.min(1.0F, (lifetime - age - tickDelta) / 60.0F);
			for (int i = 0; i < ANTS; i++) {
				if (!shown[i]) {
					continue;
				}
				double x = MathHelper.lerp(tickDelta, ox[i], px[i]), y = MathHelper.lerp(tickDelta, oy[i], py[i]), z = MathHelper.lerp(tickDelta, oz[i], pz[i]);
				double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
				if (dx * dx + dy * dy + dz * dz > 180 * 180) {
					continue;
				}
				Vec3d head = new Vec3d(px[i] - ox[i], py[i] - oy[i], pz[i] - oz[i]);
				if (head.lengthSquared() < 1.0E-6) {
					head = new Vec3d(Math.cos(i), 0, Math.sin(i));
				}
				head = head.normalize();
				Vec3d p = new Vec3d(dx, dy, dz);
				int col = NewBombShapes.hash(i, 6) < 0.15 ? 0x5A1E0C : 0x1A0E08;
				double s = 0.75 + NewBombShapes.hash(i, 7) * 0.5;
				// Abdomen, thorax, head.
				FxDraw.ribbon(shade, m, p.subtract(head.multiply(0.36 * s)), p.subtract(head.multiply(0.08 * s)), Vec3d.ZERO, 0.2 * s, col, fade, fade);
				FxDraw.ribbon(shade, m, p.subtract(head.multiply(0.06 * s)), p.add(head.multiply(0.1 * s)), Vec3d.ZERO, 0.11 * s, col, fade, fade);
				double twitch = Math.sin((age + tickDelta) * 1.7 + i) * 0.03;
				FxDraw.ribbon(shade, m, p.add(head.multiply(0.12 * s)), p.add(head.multiply((0.26 + twitch) * s)), Vec3d.ZERO, 0.14 * s, col, fade, fade);
			}
			renderDust(consumers, m, cam, age + tickDelta, FxDraw.cameraBasis(camYaw, camPitch), fade);
		}

		/**
		 * What the colony throws up: sawdust and splinters where the front is chewing through the town, plumes of spoil
		 * off the mound while it is raised, and in the nuptial flight a dark cloud of wings rolling out over the land.
		 */
		private void renderDust(VertexConsumerProvider consumers, Matrix4f m, Vec3d cam, float a, Vec3d[] cb, float fade) {
			java.util.List<Effects.Puff> dust = FxKit.puffs();
			Vec3d gc = g.subtract(cam);
			if (a > Termite.EMERGE && a < Termite.EAT_END + 30) {
				double front = Termite.front(a);
				float k = fade * (a > Termite.EAT_END ? 1 - (a - Termite.EAT_END) / 30.0F : 1.0F);
				int n = 18 + (int) Math.min(30, front * 0.4);
				for (int i = 0; i < n; i++) {
					double ang = i * MathHelper.TAU / n + NewBombShapes.hash(i, 20) * 0.3;
					double life = (a * 0.025 + NewBombShapes.hash(i, 21)) % 1.0;
					double x = g.x + Math.cos(ang) * front, z = g.z + Math.sin(ang) * front;
					Vec3d p = new Vec3d(x - cam.x, ground(x, z) - cam.y + 0.8 + life * 7, z - cam.z);
					dust.add(new Effects.Puff(p, 1.8 + life * 4.5, 0xB89466, 0x3A2A1A, 0.55F * k * (float) (1 - life)));
				}
			}
			if (a > Termite.BUILD && a < Termite.BUILD_END + 40) {
				float k = fade * (a > Termite.BUILD_END ? 1 - (a - Termite.BUILD_END) / 40.0F : 1.0F);
				double top = Termite.built(a) * Termite.HILL_H;
				FxKit.column(gc.add(0, top, 0), 16 + top * 0.6, 4 + top * 0.12, a, 0xA08060, 0x302218, 0.5F * k, 251, dust);
				FxKit.dustRing(m, cb, gc.add(0, 0.5, 0), Termite.HILL_R + 6, 5, 0x9A7A58, 0x2A2016, 0.45F * k, 252, dust);
			}
			if (a > Termite.FLIGHT && a < Termite.FLIGHT_END + 40) {
				float k = fade * (a > Termite.FLIGHT_END ? 1 - (a - Termite.FLIGHT_END) / 40.0F : 1.0F) * Math.min(1.0F, (a - Termite.FLIGHT) / 40.0F);
				double fly = Math.max(10, Termite.flightFront(a) * Math.min(1.0, (a - Termite.FLIGHT) / 120.0));
				int n = 40;
				for (int i = 0; i < n; i++) {
					double ang = i * MathHelper.TAU / n + a * 0.004 + NewBombShapes.hash(i, 22) * 0.2;
					double rr = fly * (0.78 + 0.22 * NewBombShapes.hash(i, 23));
					Vec3d p = gc.add(Math.cos(ang) * rr, 8 + NewBombShapes.hash(i, 24) * 20 + Math.sin(a * 0.05 + i) * 2, Math.sin(ang) * rr);
					dust.add(new Effects.Puff(p, 5 + rr * 0.05, 0x5A4A38, 0x140C08, 0.32F * k));
				}
				if (a < Termite.FLIGHT + 220) {
					double top = Termite.built(a) * Termite.HILL_H;
					FxKit.column(gc.add(0, top, 0), 50, 7, a, 0x5A4A38, 0x140C08, 0.55F * (1 - (a - Termite.FLIGHT) / 220.0F) * fade, 253, dust);
				}
			}
			FxKit.flush(dust, consumers, m, cb);
		}
	}

	// ====================================================================== THE SUN DISAPPEARS

	/** The beam to the sun, the eclipse that eats it, and the dead sun falling out of the black sky. */
	static final class SunDeath extends FxEffect {
		private final Vec3d g, heading, corpseFrom;

		SunDeath(Vec3d g, Vec3d heading, int life) {
			super(life);
			this.g = g;
			this.heading = heading;
			this.corpseFrom = Sun.corpseStart(g, heading);
		}

		/** Where the sun hangs right now (the sky's rotation, as the vanilla sky draws it). */
		private static Vec3d sunDir(float tickDelta) {
			ClientWorld w = com.armageddon.client.render.FxDraw.world();
			float ang = w == null ? 0 : w.getSkyAngle(tickDelta) * MathHelper.TAU;
			return new Vec3d(-Math.sin(ang), Math.cos(ang), 0).normalize();
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t >= Sun.DIM && t < Sun.OUT) {
				float k = (t - Sun.DIM) / (float) (Sun.OUT - Sun.DIM);
				ClientFx.tintSky(0x000004, 0.3F + 0.7F * k);
				if (world.random.nextInt(18) == 0) {
					ClientFx.skyFlash(2);
				}
			}
			if (t >= Sun.FALL && t < Sun.IMPACT) {
				Vec3d c = Sun.corpse(g, corpseFrom, t);
				float k = (t - Sun.FALL) / (float) (Sun.IMPACT - Sun.FALL);
				ClientFx.light(c, 0.5F + k, 0xFF7030);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Vec3d dir = sunDir(tickDelta);
			Vec3d sun = dir.multiply(340);
			if (t >= Sun.BEAM && t < Sun.DIM + 40) {
				float k = MathHelper.clamp((t - Sun.BEAM) / (Sun.HIT - Sun.BEAM), 0.0F, 1.0F);
				float fade = t > Sun.HIT ? Math.max(0, 1 - (t - Sun.HIT) / 50.0F) : 1.0F;
				Vec3d base = g.subtract(cam);
				Vec3d end = base.add(sun.subtract(base).multiply(k * k));
				FxDraw.beam(glow, m, base, end, Vec3d.ZERO, 1.6 + 2.5 * k, 0xFFE0A0, 0.9F * fade);
				Solids.flare(glow, m, end, cb, 4 + 6 * k, 0xFFF0C0, fade);
			}
			if (t >= Sun.HIT && t < Sun.DIM + 30) {
				float k = 1 - (t - Sun.HIT) / (Sun.DIM + 30 - Sun.HIT);
				Solids.flare(glow, m, sun, cb, 90 * k, 0xFFF0C0, k);
			}
			if (t >= Sun.DIM && t < Sun.FALL) {
				// The eclipse: a black disc slides over the sun, the corona blazes, gutters and is gone.
				double slide = MathHelper.clamp((t - Sun.DIM) / 90.0, 0.0, 1.0);
				slide = 1 - Math.pow(1 - slide, 3);
				Vec3d[] sb = ArsenalEffects.basis(dir);
				Vec3d disc = sun.add(sb[0].multiply((1 - slide) * 140)).subtract(dir.multiply(5));
				FxDraw.disc(solid, m, disc, sb[0], sb[1], 64, 0x000000, 1.0F, 1.0F, 40);
				if (t > Sun.DIM + 80 && t < Sun.OUT + 30) {
					double c = (t - Sun.DIM - 80) / (Sun.OUT + 30 - Sun.DIM - 80);
					float corona = (float) (Math.sin(c * Math.PI) * (0.8 + 0.2 * Math.sin(t * 0.9)));
					FxDraw.ring(glow, m, sun.add(dir.multiply(-2)), sb[0], sb[1], 60, 60 + 50 * corona, 0xFFE8C0, 0.9F * corona, 0.4F * corona, 0.0F, 48);
					for (int i = 0; i < 16; i++) {
						double a = i * MathHelper.TAU / 16 + t * 0.003;
						Vec3d d = sb[0].multiply(Math.cos(a)).add(sb[1].multiply(Math.sin(a)));
						double len = 60 + (40 + 60 * NewBombShapes.hash(i, 2)) * corona;
						FxDraw.ribbon(glow, m, sun.add(d.multiply(58)), sun.add(d.multiply(len)), Vec3d.ZERO, 6, 0xFFD8A0, 0.5F * corona, 0.0F);
					}
				}
			}
			if (t >= Sun.FALL && t < Sun.IMPACT + 2) {
				Vec3d c = Sun.corpse(g, corpseFrom, t).subtract(cam);
				Vec3d prev = Sun.corpse(g, corpseFrom, t - 6).subtract(cam);
				double r = Sun.CORPSE_R;
				float spin = t * 0.004F;
				// A dead star: a black-red crust split by glowing seams, a smouldering shell and a trail of smoke.
				Solids.ellipsoid(solid, m, c, r, r, r, 16, 28, (lat, lon, n) -> {
					double crack = Math.abs(Math.sin(n.x * 9 + spin * 20) * Math.sin(n.y * 7 - spin * 10) * Math.sin(n.z * 8 + 1.3));
					return crack < 0.06 ? 0xFF6020 : crack < 0.14 ? 0x7A1A08 : Solids.mix(0x1A0805, 0x2E0E06, Solids.hash(lat, lon));
				}, true, 1.0F);
				FxDraw.sphere(glow, m, c, r * 1.15, Vec3d.ZERO, 0xFF5010, 0.05F, 0.6F, 14, 24);
				FxDraw.sphere(glow, m, c, r * 1.5, Vec3d.ZERO, 0xFF3000, 0.0F, 0.25F, 12, 20);
				Vec3d back = c.subtract(prev);
				if (back.lengthSquared() > 1.0E-4) {
					back = back.normalize();
					java.util.List<Effects.Puff> trail = FxKit.puffs();
					for (int i = 0; i < 26; i++) {
						double u = (i + 0.5) / 26;
						Vec3d off = cb[0].multiply((NewBombShapes.hash(i, 3) - 0.5) * r * (0.6 + u)).add(cb[1].multiply((NewBombShapes.hash(i, 4) - 0.5) * r * (0.6 + u)));
						trail.add(new Effects.Puff(c.add(off).subtract(back.multiply(r * (0.8 + u * 6))), r * (0.55 + u * 0.9), 0x5A3A30, 0x120806, 0.75F * (float) (1 - u)));
					}
					FxKit.flush(trail, consumers, m, cb);
					FxKit.fire(consumers, m, cb, c, r * 1.2, 0xFF7030, 0.3F, 0.6F, t, 211);
					FxDraw.ribbon(glow, m, c, c.subtract(back.multiply(r * 4)), Vec3d.ZERO, r * 1.2, 0xFF6020, 0.35F, 0.0F);
				}
			}
			if (t >= Sun.IMPACT && t < Sun.END) {
				renderImpact(consumers, solid, glow, m, cb, cam, t - Sun.IMPACT);
			}
		}

		/**
		 * The dead star lands: it ploughs half into the ground and lies there cooling from white to black, a flash and a
		 * wall of fire roll outward with the destruction front (7 blocks a tick, as the server's devastation runs), the
		 * crust and the city are thrown into the sky, and a burning plume climbs out of the crater.
		 */
		private void renderImpact(VertexConsumerProvider consumers, VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d cam, float e) {
			Vec3d gp = g.subtract(cam);
			double r = Sun.CORPSE_R;
			float life = Sun.END - Sun.IMPACT;
			float k = e / life;
			double heat = Math.max(0, 1 - e / 220.0);
			Solids.ellipsoid(solid, m, gp.add(0, -r * 0.25, 0), r, r * 0.75, r, 14, 26, (lat, lon, n) -> {
				double crack = Math.abs(Math.sin(n.x * 9) * Math.sin(n.y * 7) * Math.sin(n.z * 8 + 1.3));
				int crust = Solids.mix(0x2A0E06, 0xFFD090, heat * heat);
				return crack < 0.08 ? Solids.mix(0x8A2008, 0xFFF0C0, heat) : crust;
			}, true, 1.0F);
			FxDraw.sphere(glow, m, gp, r * 1.2, Vec3d.ZERO, 0xFF5010, 0.0F, 0.5F * (float) heat + 0.1F, 14, 24);
			if (e < 40) {
				float f = 1 - e / 40.0F;
				FxKit.flare(consumers, m, cb, gp.add(0, r, 0), 900 * f + 120, 0xFFF4E0, f, e * 0.004);
				FxDraw.sphere(glow, m, gp, r * 1.3 + e * 9, Vec3d.ZERO, 0xFFFFFF, 0.8F * f, 0.9F * f, 16, 32);
			}
			// The front: a ground-hugging shock ring and a wall of fire riding it out to the edge of the devastation.
			double front = r + e * 7;
			if (front < 420) {
				float fa = (float) (1 - front / 420);
				FxKit.groundShock(consumers, m, gp.add(0, 0.6, 0), front, 0xFFE0B0, fa);
				FxKit.groundShock(consumers, m, gp.add(0, 0.6, 0), front * 0.85, 0xFF7030, 0.7F * fa);
				int n = 40;
				for (int i = 0; i < n; i++) {
					double ang = i * MathHelper.TAU / n + NewBombShapes.hash(i, 9) * 0.1;
					Vec3d fp = gp.add(Math.cos(ang) * front, 6 + NewBombShapes.hash(i, 10) * 6, Math.sin(ang) * front);
					FxKit.fire(consumers, m, cb, fp, 10 + front * 0.03, 0xFFC070, (float) ((e * 0.03 + NewBombShapes.hash(i, 11)) % 1.0), fa, e, 230 + i);
				}
			}
			// Everything inside the front keeps burning.
			for (int i = 0; i < 30; i++) {
				double ang = NewBombShapes.hash(i, 12) * MathHelper.TAU;
				double rr = r * 1.4 + NewBombShapes.hash(i, 13) * Math.min(front, 420) * 0.9;
				Vec3d fp = gp.add(Math.cos(ang) * rr, 3, Math.sin(ang) * rr);
				Solids.dot(glow, m, fp, cb, 10 + NewBombShapes.hash(i, 14) * 10, 0xFF6A20, 0.4F * (1 - k));
			}
			if (e < 260) {
				Cinematics.debris(solid, glow, m, gp, e, 120, 3.6, 10, 0.05, 41, 0x3A2A20, 0xFF7030);
				Cinematics.debris(solid, glow, m, gp, e, 50, 5.5, 4, 0.05, 43, 0x5A4030, 0xFFD080);
			}
			java.util.List<Effects.Puff> plume = FxKit.puffs();
			FxKit.column(gp.add(0, r * 0.4, 0), 80 + Math.min(e, 220) * 1.6, r * 0.9 + Math.min(e, 220) * 0.35, e, 0x9A6A50, 0x221410, 0.75F * (1 - k), 240, plume);
			FxKit.dustRing(m, cb, gp, Math.min(front, 420) * 0.9, 30 + e * 0.1, 0x8A7060, 0x2A2018, 0.6F * (1 - k), 241, plume);
			FxKit.flush(plume, consumers, m, cb);
		}
	}
}
