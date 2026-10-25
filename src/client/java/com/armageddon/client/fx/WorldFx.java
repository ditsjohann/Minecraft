package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.registry.ModParticles;
import com.armageddon.util.WorldShapes;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/**
 * Signature aftermath visuals for Galactic Smash, Booming Tick and Alien Death Ray. {@code p.seed()} picks
 * the effect: 1 smash aftermath, 2 tick clock, 3 terrace glow, 4 EMP static, 5 alien bloom. Shapes come from
 * {@link WorldShapes} so they sit exactly on the blocks the server placed.
 */
public final class WorldFx {
	private WorldFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			case 1 -> new SmashAftermath(p);
			case 2 -> new TickClock(p);
			case 3 -> new BandGlow(p);
			case 4 -> new EmpStatic(p);
			case 5 -> new AlienBloom(p);
			default -> null;
		};
	}

	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);

	static double ground(ClientWorld w, double x, double z, double fallback) {
		if (w == null) {
			return fallback;
		}
		int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
		return y > w.getBottomY() + 1 ? y : fallback;
	}

	/** Fade in over {@code in} ticks, out over the last {@code out}. */
	static float envelope(float a, int life, float in, float out) {
		return (float) (Cinematics.clamp01(a / in) * Cinematics.clamp01((life - a) / out));
	}

	static Vec3d flat(Vec3d v) {
		Vec3d h = new Vec3d(v.x, 0, v.z);
		return h.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : h.normalize();
	}

	// ====================================================================== Galactic Smash

	/** Glowing cosmic-glass cracks with pulses running outward, knuckle glows, bell rings, drifting star dust. */
	static final class SmashAftermath extends FxEffect {
		private final Vec3d g, fwd, right;
		private final double R;
		private final int base, color;
		private final double[][][] cracks;
		private final double[][] crackY;

		SmashAftermath(FxPayload p) {
			super(Math.max(40, p.duration()));
			g = new Vec3d(p.x(), p.y(), p.z());
			fwd = flat(new Vec3d(p.x2(), p.y2(), p.z2()));
			right = new Vec3d(-fwd.z, 0, fwd.x);
			R = p.scale();
			base = (int) p.p1();
			color = p.color();
			cracks = WorldShapes.smashCracks(WorldShapes.seed(g), R);
			crackY = new double[cracks.length][];
			for (int i = 0; i < cracks.length; i++) {
				crackY[i] = new double[cracks[i].length];
				java.util.Arrays.fill(crackY[i], base);
			}
		}

		private Vec3d local(double f, double r, double y) {
			return new Vec3d(g.x + fwd.x * f + right.x * r, y, g.z + fwd.z * f + right.z * r);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 40 == 1) {
				// The cracks are carved over time: re-sample where their glowing floors are.
				for (int i = 0; i < cracks.length; i++) {
					for (int k = 0; k < cracks[i].length; k++) {
						Vec3d p = local(cracks[i][k][0], cracks[i][k][1], base);
						crackY[i][k] = ground(world, p.x, p.z, base);
					}
				}
			}
			Vec3d cam = ClientFx.cameraPos();
			float fade = envelope(age, lifetime, 10, 300);
			if (age < 140) {
				ClientFx.light(g.add(0, 18, 0), 0.7F * (1 - age / 140.0F), 0xC090FF);
			}
			if (cam.squaredDistanceTo(g) < R * R * 6 && fade > 0.05F) {
				// Star dust settling over the imprint, slowly rising again like it can't decide.
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU, d = Math.sqrt(world.random.nextDouble()) * R * 0.75;
					double x = g.x + Math.cos(a) * d, z = g.z + Math.sin(a) * d;
					double y = ground(world, x, z, base) + 0.5 + world.random.nextDouble() * 6;
					world.addParticle(i == 0 ? ParticleTypes.WITCH : ParticleTypes.END_ROD, x, y, z,
							world.random.nextGaussian() * 0.01, 0.01 + world.random.nextDouble() * 0.02, world.random.nextGaussian() * 0.01);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float fade = envelope(a, lifetime, 10, 300);
			if (fade <= 0.01F || cam.squaredDistanceTo(g) > 600 * 600) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			// Cracks: violet glow lines with bright pulses running out from the knuckles.
			for (int i = 0; i < cracks.length; i++) {
				double[][] c = cracks[i];
				double pulse = (a * 0.01 + i * 0.137) % 1.0;
				Vec3d prev = null;
				for (int k = 0; k < c.length; k++) {
					Vec3d p = local(c[k][0], c[k][1], crackY[i][k] + 0.2).subtract(cam);
					if (prev != null) {
						double u = k / (double) (c.length - 1);
						float lit = (float) (0.3 + 0.7 * Math.exp(-Math.pow((u - pulse) / 0.07, 2)));
						float al = lit * fade * (float) (1 - u * 0.5);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.4 * (1 - u * 0.6), color, al, al);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.4, 0xFFE8FF, al * 0.8F, al * 0.8F);
					}
					prev = p;
				}
			}
			// The knuckle bowls glow like struck metal cooling.
			float beat = 0.6F + 0.4F * MathHelper.sin(a * 0.08F);
			for (double[] k : WorldShapes.KNUCKLES) {
				Vec3d p = local(k[0] * R, k[1] * R, base - WorldShapes.KNUCKLE_DEPTH * R + 1.5).subtract(cam);
				Solids.dot(glow, m, p, cb, R * 0.07, 0xD8A0FF, 0.45F * beat * fade);
				Solids.dot(glow, m, p, cb, R * 0.025, 0xFFFFFF, 0.6F * beat * fade);
			}
			// Bell rings: a stroke every three seconds, rolling out over the ripples and dying away.
			float ringing = (float) Math.exp(-a / 500.0) * fade;
			if (ringing > 0.02F) {
				double phase = (a % 60) / 60.0;
				double rr = R * (0.85 + 1.1 * phase);
				Vec3d c = new Vec3d(g.x, base + 0.6, g.z).subtract(cam);
				FxDraw.ring(glow, m, c, X, Z, rr, rr + R * 0.04, color, 0.0F, 0.4F * (float) (1 - phase) * ringing, 0.0F, 128);
			}
		}
	}

	// ====================================================================== Booming Tick

	/** The finished target as a clock: twelve hour marks, a ticking second hand, a heartbeat running inward. */
	static final class TickClock extends FxEffect {
		private final Vec3d g;
		private final double R, yaw0;
		private final int base;

		TickClock(FxPayload p) {
			super(Math.max(40, p.duration()));
			g = new Vec3d(p.x(), p.y(), p.z());
			Vec3d f = flat(new Vec3d(p.x2(), p.y2(), p.z2()));
			yaw0 = Math.atan2(f.z, f.x);
			R = p.scale();
			base = (int) p.p1();
		}

		private double floorTop(int band) {
			return base - WorldShapes.bandDepth(band) + 1.04;
		}

		private Vec3d at(double ang, double d, double y, Vec3d cam) {
			return new Vec3d(g.x + Math.cos(ang) * d, y, g.z + Math.sin(ang) * d).subtract(cam);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 20 == 0 && age > 20) {
				ClientFx.light(new Vec3d(g.x, base - WorldShapes.bandDepth(0) + 2, g.z), 0.35F * envelope(age, lifetime, 20, 200), 0xFF3020);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float fade = envelope(a, lifetime, 30, 200);
			if (fade <= 0.01F || cam.squaredDistanceTo(g) > 700 * 700) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			float beat = (float) Math.exp(-(a % 20) / 4.0);
			double rimY = floorTop(WorldShapes.BANDS);
			// Hour marks around the rim; XII (pointing where the bomb faced) is the longest.
			for (int k = 0; k < 12; k++) {
				double ang = yaw0 + k * Math.PI / 6;
				double inner = R * (k == 0 ? 0.84 : 0.92);
				float al = (0.5F + 0.5F * beat) * fade;
				FxDraw.ribbon(glow, m, at(ang, inner, rimY, cam), at(ang, R * 0.995, rimY, cam), Vec3d.ZERO, k == 0 ? 2.6 : 1.5, 0xFFE0A0, al, al);
			}
			// The second hand: jumps a sixtieth of a turn every second with a little overshoot, across every step.
			double sec = Math.floor(a / 20);
			double snap = Math.min(1, (a % 20) / 3.0);
			double ang = yaw0 + (sec - 1 + snap + 0.08 * Math.sin(snap * Math.PI)) * Math.PI / 30;
			for (int i = 1; i <= WorldShapes.BANDS; i++) {
				double y = floorTop(i);
				FxDraw.ribbon(glow, m, at(ang, WorldShapes.bandRadius(i - 1, R), y, cam), at(ang, WorldShapes.bandRadius(i, R), y, cam),
						Vec3d.ZERO, 0.9, 0xFF3020, 0.85F * fade, 0.85F * fade);
			}
			// Each step's lip lights in turn, a heartbeat running inward every second.
			double phase = (a % 20) / 20.0;
			double active = WorldShapes.BANDS - 1 - phase * (WorldShapes.BANDS - 1);
			for (int i = 1; i < WorldShapes.BANDS; i++) {
				double rr = WorldShapes.bandRadius(i, R);
				float al = (float) (0.15 + 0.65 * Math.exp(-Math.pow(i - active, 2) / 1.5)) * fade;
				Vec3d c = new Vec3d(g.x, floorTop(i + 1), g.z).subtract(cam);
				FxDraw.ring(glow, m, c, X, Z, rr - 0.35, rr + 0.35, WorldShapes.bandColor(i), 0.0F, al, 0.0F, Math.max(24, (int) (rr * 1.2)));
			}
		}
	}

	/** A freshly levelled terrace glowing hot in its colour, cooling, with embers. */
	static final class BandGlow extends FxEffect {
		private final Vec3d g;
		private final double inner, outer, y;
		private final int color;

		BandGlow(FxPayload p) {
			super(Math.max(20, p.duration()));
			g = new Vec3d(p.x(), p.y(), p.z());
			outer = p.scale();
			inner = p.p1();
			y = p.p2() + 1.05;
			color = p.color();
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d cam = ClientFx.cameraPos();
			if (age < lifetime - 20 && cam.squaredDistanceTo(g) < (outer + 80) * (outer + 80)) {
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU, d = MathHelper.lerp(world.random.nextDouble(), inner, outer);
					world.addParticle(ModParticles.EMBER, g.x + Math.cos(a) * d, y + 0.2, g.z + Math.sin(a) * d, 0, 0.04 + world.random.nextDouble() * 0.05, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float k = envelope(a, lifetime, 4, lifetime - 4) * (float) Math.exp(-a / (lifetime * 0.45));
			if (k <= 0.01F) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d c = new Vec3d(g.x, y, g.z).subtract(cam);
			int segs = Math.max(32, (int) (outer * 1.5));
			FxDraw.ring(glow, m, c, X, Z, inner, outer, color, 0.15F * k, 0.45F * k, 0.15F * k, segs);
			FxDraw.ring(glow, m, c, X, Z, outer - 0.7, outer + 0.7, 0xFFF0C0, 0.0F, 0.9F * k, 0.0F, segs);
		}
	}

	// ====================================================================== Alien Death Ray

	/** Blue static crawling over the ground: an expanding pulse, then arcs jumping between surfaces. */
	static final class EmpStatic extends FxEffect {
		private final Vec3d g;
		private final double R;
		private final int color;

		EmpStatic(FxPayload p) {
			super(Math.max(20, p.duration()));
			g = new Vec3d(p.x(), p.y(), p.z());
			R = p.scale();
			color = p.color();
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d cam = ClientFx.cameraPos();
			if (age < lifetime - 20 && Math.hypot(cam.x - g.x, cam.z - g.z) < R + 60) {
				for (int i = 0; i < 2; i++) {
					// Arcs near the viewer only, where they read.
					double a = world.random.nextDouble() * MathHelper.TAU, d = 4 + world.random.nextDouble() * 50;
					double x = cam.x + Math.cos(a) * d, z = cam.z + Math.sin(a) * d;
					if (Math.hypot(x - g.x, z - g.z) > R) {
						continue;
					}
					double b = world.random.nextDouble() * MathHelper.TAU, len = 3 + world.random.nextDouble() * 6;
					double x2 = x + Math.cos(b) * len, z2 = z + Math.sin(b) * len;
					Vec3d p1 = new Vec3d(x, ground(world, x, z, g.y) + 0.2, z), p2 = new Vec3d(x2, ground(world, x2, z2, g.y) + 0.2, z2);
					ClientFx.add(new Effects.Arc(p1, p2, 0.2, color, world.random.nextInt()));
					world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, p1.y + 0.2, z, 0, 0.05, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a > 50) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			double rr = R * a / 40.0;
			float al = (float) (1 - a / 50.0) * 0.5F;
			Vec3d c = g.add(0, 0.5, 0).subtract(cam);
			FxDraw.ring(glow, m, c, X, Z, Math.max(0, rr - 3), rr, color, 0.0F, al, 0.0F, 128);
			FxDraw.ring(glow, m, c, X, Z, Math.max(0, rr * 0.8 - 1), rr * 0.8, 0xFFFFFF, 0.0F, al * 0.6F, 0.0F, 96);
		}
	}

	/** The alien land alive: a beam out of the shaft, crystal spires breathing light, spores drifting up. */
	static final class AlienBloom extends FxEffect {
		private final Vec3d g;
		private final double reach, shaft;
		private final int base, color;
		private final double[][] spires;
		private final double[] spireGround;

		AlienBloom(FxPayload p) {
			super(Math.max(40, p.duration()));
			g = new Vec3d(p.x(), p.y(), p.z());
			reach = p.scale();
			base = (int) p.p1();
			color = p.color();
			shaft = WorldShapes.shaftRadius(reach);
			spires = WorldShapes.spires(WorldShapes.seed(g), reach);
			spireGround = new double[spires.length];
			java.util.Arrays.fill(spireGround, Double.NaN);
		}

		@Override
		public boolean tick(ClientWorld world) {
			float fade = envelope(age, lifetime, 20, 300);
			if (age < 400) {
				ClientFx.light(new Vec3d(g.x, base + 12, g.z), 0.55F * fade, color);
			}
			if (age % 60 == 30) {
				for (int i = 0; i < spires.length; i++) {
					// Re-sample: the spires grow in after the land turns, so their base settles late.
					double x = g.x + spires[i][0], z = g.z + spires[i][1];
					double y = ground(world, x, z, Double.NaN);
					if (!Double.isNaN(y)) {
						spireGround[i] = y;
					}
				}
			}
			Vec3d cam = ClientFx.cameraPos();
			if (fade > 0.05F && Math.hypot(cam.x - g.x, cam.z - g.z) < reach + 40) {
				for (int i = 0; i < 3; i++) {
					double x = cam.x + world.random.nextGaussian() * 18, z = cam.z + world.random.nextGaussian() * 18;
					if (Math.hypot(x - g.x, z - g.z) < reach) {
						double y = ground(world, x, z, cam.y) + world.random.nextDouble() * 4;
						world.addParticle(ParticleTypes.GLOW, x, y, z, 0, 0.02 + world.random.nextDouble() * 0.03, 0);
					}
				}
				if (age % 2 == 0) {
					world.addParticle(ParticleTypes.END_ROD, g.x + world.random.nextGaussian() * shaft * 0.5, base + 2, g.z + world.random.nextGaussian() * shaft * 0.5,
							0, 0.3 + world.random.nextDouble() * 0.3, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float fade = envelope(a, lifetime, 20, 300);
			if (fade <= 0.01F || cam.squaredDistanceTo(g) > 900 * 900) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			// The shaft breathes a green beam into the sky, with rings of light climbing it.
			float pulse = 0.75F + 0.25F * MathHelper.sin(a * 0.07F);
			Vec3d bottom = new Vec3d(g.x, base - 30, g.z).subtract(cam), top = new Vec3d(g.x, base + 260, g.z).subtract(cam);
			FxDraw.beam(glow, m, bottom, top, Vec3d.ZERO, shaft * 0.7 * pulse, color, 0.35F * fade);
			for (int k = 0; k < 4; k++) {
				double y = base + ((a * 1.6 + k * 60) % 240);
				float al = (float) (1 - ((a * 1.6 + k * 60) % 240) / 240.0) * 0.6F * fade;
				FxDraw.ring(glow, m, new Vec3d(g.x, y, g.z).subtract(cam), X, Z, shaft * 1.2, shaft * 1.8, 0xB0FFD0, 0.0F, al, 0.0F, 48);
			}
			// Spires: a glowing heart line and a tip flare, each breathing on its own beat.
			float grown = (float) Cinematics.clamp01((a - 200) / 120.0) * fade;
			if (grown <= 0.01F) {
				return;
			}
			for (int i = 0; i < spires.length; i++) {
				if (Double.isNaN(spireGround[i])) {
					continue;
				}
				double[] s = spires[i];
				double bx = g.x + s[0], bz = g.z + s[1], h = s[2];
				Vec3d b = new Vec3d(bx, spireGround[i] - 1, bz);
				if (b.squaredDistanceTo(cam) > 400 * 400) {
					continue;
				}
				Vec3d tip = b.add(s[4] * h, h, s[5] * h);
				float breathe = 0.55F + 0.45F * MathHelper.sin(a * 0.06F + i * 1.7F);
				FxDraw.beam(glow, m, b.subtract(cam), tip.subtract(cam), Vec3d.ZERO, s[3] * 0.35, color, 0.22F * breathe * grown);
				Solids.dot(glow, m, tip.subtract(cam), cb, 1.8 + s[3] * 0.4, 0xD0FFE0, 0.7F * breathe * grown);
				double ring = (a * 0.4 + i * 13) % 30;
				FxDraw.ring(glow, m, b.add(0, 1.1, 0).subtract(cam), X, Z, s[3] + ring * 0.4, s[3] + ring * 0.4 + 0.5, color, 0.0F,
						0.35F * (float) (1 - ring / 30) * grown, 0.0F, 32);
			}
		}
	}
}
