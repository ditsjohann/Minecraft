package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.registry.ModParticles;
import com.armageddon.util.CelestialIIShapes;
import com.armageddon.util.Paths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Signature effects for Celestial II. {@code p.seed()} picks the effect. */
public final class CelestialIIFx {
	public static final int MILK_CROWN = 1, TWISTER_WAKE = 2, RUNE_SIGIL = 3, BLIGHT_FOG = 4, SPORE_BURST = 5, CROP_STAMP = 6, ABDUCTION = 7;

	private CelestialIIFx() {
	}

	public static FxEffect create(FxPayload p) {
		Vec3d a = new Vec3d(p.x(), p.y(), p.z());
		Vec3d b = new Vec3d(p.x2(), p.y2(), p.z2());
		return switch (p.seed()) {
			case MILK_CROWN -> new MilkCrown(a, p.scale(), p.duration());
			case TWISTER_WAKE -> new TwisterWake(a, b, p.scale(), p.p1(), p.duration());
			case RUNE_SIGIL -> new RuneSigil(a, (int) p.p1(), p.duration());
			case BLIGHT_FOG -> new BlightFog(a, p.duration());
			case SPORE_BURST -> new SporeBurst(a, p.scale(), p.duration());
			case CROP_STAMP -> new CropStamp(a, p.color(), p.duration());
			case ABDUCTION -> new Abduction(a, p.scale(), p.p1(), p.duration());
			default -> null;
		};
	}

	static double groundY(double x, double z) {
		ClientWorld w = MinecraftClient.getInstance().world;
		return w == null ? 64 : w.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
	}

	/** A ribbon lying flat on the ground between two camera-relative points. */
	static void flat(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double width, int rgb, float alpha) {
		FxDraw.ribbon(vc, m, a, b, a.add(b).multiply(0.5).add(0, 1.0E4, 0), width, rgb, alpha, alpha);
	}

	// ======================================================================== exploding cows

	/** The milk-drop crown (Edgerton's photograph, at the size of a house): a splash rim of spikes with beads on top. */
	static final class MilkCrown extends FxEffect {
		private final Vec3d c;
		private final double s;

		MilkCrown(Vec3d c, double size, int life) {
			super(Math.max(30, life));
			this.c = c;
			this.s = size;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 0) {
				for (int i = 0; i < 40 + s * 4; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double v = 0.25 + world.random.nextDouble() * 0.5;
					world.addImportantParticle(ParticleTypes.SNOWFLAKE, true, c.x, c.y + 0.5, c.z, Math.cos(a) * v * s * 0.12, 0.3 + v * s * 0.05, Math.sin(a) * v * s * 0.12);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float t = progress(td);
			Vec3d g = c.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			// The crown rises fast, holds, and slumps back into the splat.
			double rise = Math.sin(Math.PI * Math.min(1.0, t * 1.7));
			double ring = s * (0.45 + 0.6 * Effects.smooth(t * 2.2F));
			int n = 22;
			if (rise > 0.02) {
				for (int k = 0; k < n; k++) {
					double ang = MathHelper.TAU * k / n + Effects.hash(k, 1) * 0.12;
					Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
					double h = s * (0.9 + 0.5 * Effects.hash(k, 2)) * rise;
					Vec3d base = g.add(out.multiply(ring));
					Vec3d tip = base.add(out.multiply(h * 0.28)).add(0, h, 0);
					Solids.tube(solid, m, base, tip, s * 0.11, s * 0.035, 5, 0xFFFBF2, 1.0F, true);
					// The bead about to break free from each point.
					Solids.sphere(solid, m, tip.add(out.multiply(s * 0.05)).add(0, s * 0.06, 0), s * 0.07, 0xFFFFFF, 4, 6, true);
				}
				// The rim wall joining the spikes.
				Solids.tube(solid, m, g.add(0, -0.2, 0), g.add(0, s * 0.18 * rise, 0), ring * 1.02, ring * 1.06, 24, 0xF6EFE2, 1.0F, true);
			}
			// Beads that broke free, arcing out and falling.
			for (int k = 0; k < 30; k++) {
				double ang = Effects.hash(k, 4) * MathHelper.TAU;
				double v = s * (0.05 + Effects.hash(k, 5) * 0.06);
				float a = age + td - 4;
				if (a < 0) {
					continue;
				}
				double y = s * 0.9 + v * 2.2 * a - 0.045 * a * a;
				if (y < -1) {
					continue;
				}
				Vec3d p = g.add(Math.cos(ang) * (ring + v * a), y, Math.sin(ang) * (ring + v * a));
				Solids.dot(shade, m, p, cb, s * 0.06 + 0.15, 0xFFFFFF, 0.9F);
			}
			// The splat spreading out flat, cream with a white rim.
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			double splat = s * (0.6 + 1.6 * Effects.smooth(t * 1.4F));
			FxDraw.disc(shade, m, g.add(0, 0.12, 0), x, z, splat, 0xFFF4DE, 0.55F * (1 - t), 0.0F, 28);
			FxDraw.ring(shade, m, g.add(0, 0.14, 0), x, z, splat * 0.9, splat * 1.05, 0xFFFFFF, 0.0F, 0.7F * (1 - t), 0.0F, 40);
		}
	}

	// ======================================================================== deadly twister

	/** The twister's wake: a long dust trail hanging over the scar, and ground-hugging inflow arms feeding the funnel. */
	static final class TwisterWake extends FxEffect {
		private final Vec3d start, dir;
		private final double r, speed;

		TwisterWake(Vec3d start, Vec3d dir, double r, double speed, int life) {
			super(life + 220);
			this.start = start;
			this.dir = dir;
			this.r = r;
			this.speed = speed;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			int life = lifetime - 220;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			List<Effects.Puff> puffs = new ArrayList<>(140);
			// Dust left hanging over the scar, thinning with time since the funnel passed.
			double now = Math.min(a, life);
			for (int k = 0; k < 70; k++) {
				double back = k * 4 + (a % 4);
				double ts = now - back;
				if (ts < 0) {
					break;
				}
				Vec3d p = Paths.twister(start, dir, speed, ts);
				float since = (float) (a - ts);
				float alpha = 0.32F * (1 - Effects.smooth(since / 220.0F)) * Effects.smooth(since / 10.0F);
				if (alpha < 0.01F) {
					continue;
				}
				double side = (Effects.hash(k, 3) - 0.5) * r * 1.6;
				Vec3d s = new Vec3d(-dir.z, 0, dir.x);
				double x = p.x + s.x * side, z = p.z + s.z * side;
				Vec3d q = new Vec3d(x, groundY(x, z) + 2 + since * 0.05, z).subtract(cam);
				double rad = r * 0.35 + since * 0.04;
				puffs.add(new Effects.Puff(q, rad, 0xA8987E, 0x5E5246, alpha));
			}
			// Inflow arms spiralling into the base while it is alive.
			if (a < life) {
				Vec3d c = Paths.twister(start, dir, speed, a);
				double gy = groundY(c.x, c.z);
				float ramp = Effects.smooth(a / 60.0F) * (1 - Effects.smooth((a - life + 60) / 60.0F));
				for (int arm = 0; arm < 3; arm++) {
					for (int i = 0; i < 14; i++) {
						double f = i / 14.0;
						double rr = r * (3.4 - 2.8 * f);
						double ang = arm * MathHelper.TAU / 3 + f * 2.6 + a * 0.09;
						double x = c.x + Math.cos(ang) * rr, z = c.z + Math.sin(ang) * rr;
						Vec3d q = new Vec3d(x, gy + 1.2 + f * 2, z).subtract(cam);
						puffs.add(new Effects.Puff(q, 2.2 + f * 2.5, 0xB4A48A, 0x6A5C4C, 0.38F * ramp * (float) (0.4 + 0.6 * f)));
					}
				}
			}
			Effects.drawPuffs(puffs, shade, m, cb, 8);
		}
	}

	// ======================================================================== colossal spear

	/** The rune sigil burning itself into the ground around the shaft as the spear drills down. */
	static final class RuneSigil extends FxEffect {
		private static final int DRILL_DELAY = 24;
		private static final int SEGS = 96;
		private static final double[] RADII = {CelestialIIShapes.SIGIL_R, CelestialIIShapes.SIGIL_R - 4, 18};
		private final Vec3d c;
		private final int steps;
		private final double gy;
		/** Ground height under each ring vertex, spoke end and glyph (the sigil is burned into uneven land). */
		private final double[][] ringY = new double[RADII.length][SEGS + 1];
		private final double[][] spokeY = new double[CelestialIIShapes.SIGIL_SPOKES][2];
		private final double[] glyphY = new double[CelestialIIShapes.SIGIL_GLYPHS];

		RuneSigil(Vec3d c, int steps, int life) {
			super(life);
			this.c = c;
			this.steps = Math.max(1, steps);
			this.gy = groundY(c.x, c.z);
			sample();
		}

		private void sample() {
			for (int ri = 0; ri < RADII.length; ri++) {
				for (int k = 0; k <= SEGS; k++) {
					double a = MathHelper.TAU * k / SEGS;
					ringY[ri][k] = groundY(c.x + Math.cos(a) * RADII[ri], c.z + Math.sin(a) * RADII[ri]);
				}
			}
			for (int k = 0; k < spokeY.length; k++) {
				double a = MathHelper.TAU * k / spokeY.length;
				spokeY[k][0] = groundY(c.x + Math.cos(a) * 18, c.z + Math.sin(a) * 18);
				spokeY[k][1] = groundY(c.x + Math.cos(a) * RADII[1], c.z + Math.sin(a) * RADII[1]);
			}
			for (int k = 0; k < glyphY.length; k++) {
				double a = MathHelper.TAU * (k + 0.5) / glyphY.length;
				glyphY[k] = groundY(c.x + Math.cos(a) * (RADII[0] - 2), c.z + Math.sin(a) * (RADII[0] - 2));
			}
		}

		private Vec3d at(double ang, double r, double y, Vec3d cam) {
			return new Vec3d(c.x + Math.cos(ang) * r, y + 0.08, c.z + Math.sin(ang) * r).subtract(cam);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 20 == 0) {
				sample();
			}
			if (age > DRILL_DELAY && age < DRILL_DELAY + steps) {
				for (int i = 0; i < 2; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double rr = 12 + world.random.nextDouble() * 2;
					world.addImportantParticle(ModParticles.EMBER, true, c.x + Math.cos(ang) * rr, gy + 0.5, c.z + Math.sin(ang) * rr,
							-Math.sin(ang) * 0.3, 0.4 + world.random.nextDouble() * 0.6, Math.cos(ang) * 0.3);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double lit = CelestialIIShapes.sigilLit((a - DRILL_DELAY) / steps);
			float fadeOut = 1 - Effects.smooth((a - (lifetime - 60)) / 60.0F);
			float burnIn = Effects.smooth(a / 20.0F);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d g = new Vec3d(c.x, gy + 0.08, c.z).subtract(cam);
			double R = CelestialIIShapes.SIGIL_R;
			double sweep = lit * MathHelper.TAU;
			// Rings: each arc ignites as the sweep passes it, the rest smoulders.
			for (int ri = 0; ri < RADII.length; ri++) {
				double rr = RADII[ri];
				for (int k = 0; k < SEGS; k++) {
					double a0 = MathHelper.TAU * k / SEGS, a1 = MathHelper.TAU * (k + 1) / SEGS;
					boolean on = a0 < sweep;
					int col = ri == 2 ? 0xFF6A20 : 0xFFC860;
					float al = (on ? 0.95F : 0.18F) * burnIn * fadeOut;
					flat(glow, m, at(a0, rr, ringY[ri][k], cam), at(a1, rr, ringY[ri][k + 1], cam), on ? 1.3 : 0.9, col, al);
				}
			}
			// Spokes and glyphs light in order around the circle.
			for (int k = 0; k < CelestialIIShapes.SIGIL_SPOKES; k++) {
				double ang = MathHelper.TAU * k / CelestialIIShapes.SIGIL_SPOKES;
				boolean on = ang < sweep;
				flat(glow, m, at(ang, 18, spokeY[k][0], cam), at(ang, R - 4, spokeY[k][1], cam), 1.1, 0xFFC860, (on ? 0.9F : 0.15F) * burnIn * fadeOut);
			}
			for (int k = 0; k < CelestialIIShapes.SIGIL_GLYPHS; k++) {
				double ang = MathHelper.TAU * (k + 0.5) / CelestialIIShapes.SIGIL_GLYPHS;
				boolean on = ang < sweep;
				float pulse = on ? 0.6F + 0.4F * MathHelper.sin(a * 0.3F + k) : 0.12F;
				Vec3d p = at(ang, R - 2, glyphY[k] + 0.05, cam);
				FxDraw.disc(glow, m, p, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 1.6, 0xFF7A30, pulse * burnIn * fadeOut, 0.0F, 10);
			}
			// A floating halo of runes counter-rotating above the circle once fully lit.
			float halo = (float) Effects.smooth((float) (lit - 0.95) * 20) * fadeOut;
			if (halo > 0.01F) {
				double spin = -a * 0.02;
				double hy = ringY[0][0] + 4;
				for (int k = 0; k < 36; k++) {
					double a0 = spin + MathHelper.TAU * k / 36;
					Vec3d p0 = at(a0, R + 3, hy, cam);
					Vec3d p1 = at(a0 + 0.1, R + 3, hy, cam);
					flat(glow, m, p0, p1, 0.9, 0xFFE0A0, halo * 0.8F);
				}
			}
			// Heat and sparks breathing up out of the shaft.
			if (a > DRILL_DELAY) {
				float shaft = fadeOut * (float) Math.min(1.0, (a - DRILL_DELAY) / 20.0);
				FxDraw.beam(glow, m, g, g.add(0, 60, 0), Vec3d.ZERO, 6, 0xFF8A30, 0.12F * shaft);
				Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
				for (int k = 0; k < 24; k++) {
					double h = ((a * 0.8 + k * 13) % 70);
					double ang = k * 2.4 + a * 0.12;
					Vec3d p = g.add(Math.cos(ang) * 5, h, Math.sin(ang) * 5);
					Solids.dot(glow, m, p, cb, 0.6, 0xFFB050, shaft * (1 - (float) h / 70));
				}
			}
		}
	}

	// ======================================================================== zombie apocalypse

	/** A low, sickly fog bank rolling over the blighted ground, growing with the blight, with drifting spores. */
	static final class BlightFog extends FxEffect {
		private final Vec3d c;
		private final double[] gys = new double[110];

		BlightFog(Vec3d c, int life) {
			super(life);
			this.c = c;
			java.util.Arrays.fill(gys, Double.NaN);
		}

		@Override
		public boolean tick(ClientWorld world) {
			double r = CelestialIIShapes.blightRadius(age);
			if (r > 4 && age % 2 == 0) {
				for (int i = 0; i < 3; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double d = Math.sqrt(world.random.nextDouble()) * r;
					double x = c.x + Math.cos(ang) * d, z = c.z + Math.sin(ang) * d;
					world.addImportantParticle(ParticleTypes.SPORE_BLOSSOM_AIR, true, x, groundY(x, z) + 0.5 + world.random.nextDouble() * 3, z, 0, 0.02, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double r = CelestialIIShapes.blightRadius(a);
			if (r < 3) {
				return;
			}
			float fade = 1 - Effects.smooth((a - (lifetime - 120)) / 120.0F);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			List<Effects.Puff> puffs = new ArrayList<>(gys.length);
			for (int k = 0; k < gys.length; k++) {
				double ang = Effects.hash(k, 21) * MathHelper.TAU + a * 0.0009 * (k % 2 == 0 ? 1 : -1);
				double d = Math.sqrt(Effects.hash(k, 22)) * CelestialIIShapes.BLIGHT_MAX;
				double dx = Math.cos(ang) * d, dz = Math.sin(ang) * d;
				if (!CelestialIIShapes.blighted(dx, dz, r)) {
					continue;
				}
				if (Double.isNaN(gys[k]) || age % 40 == k % 40) {
					gys[k] = groundY(c.x + dx, c.z + dz);
				}
				double rad = 6 + Effects.hash(k, 23) * 6;
				Vec3d p = new Vec3d(c.x + dx, gys[k] + rad * 0.35, c.z + dz).subtract(cam);
				float al = 0.28F * fade * (float) Math.min(1.0, (r - d * 0.7) / 12.0);
				if (al > 0.01F) {
					puffs.add(new Effects.Puff(p, rad, 0x8FA878, 0x3C4A34, al));
				}
			}
			Effects.drawPuffs(puffs, shade, m, cb, 8);
		}
	}

	/** A giant's green burst: a churning spore column with glass glints falling through it. */
	static final class SporeBurst extends FxEffect {
		private final Vec3d c;
		private final double s;

		SporeBurst(Vec3d c, double size, int life) {
			super(life);
			this.c = c;
			this.s = size;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float t = progress(td);
			Vec3d g = c.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			float body = Effects.smooth(a / 6.0F) * (1 - Effects.smooth((t - 0.5F) / 0.5F));
			List<Effects.Puff> puffs = new ArrayList<>(30);
			double top = s * 3.2 * (1 - Math.exp(-a / 18.0));
			for (int k = 0; k < 24; k++) {
				double f = (k + 0.5) / 24;
				double ang = k * 2.39 + a * 0.03;
				double rr = s * (0.25 + 0.35 * f) * (0.8 + 0.4 * Effects.hash(k, 31));
				Vec3d p = g.add(Math.cos(ang) * rr * 0.6, top * f, Math.sin(ang) * rr * 0.6);
				puffs.add(new Effects.Puff(p, rr, 0xB6F08A, 0x3E6A2A, 0.5F * body));
			}
			Effects.drawPuffs(puffs, shade, m, cb, 8);
			for (int k = 0; k < 40; k++) {
				double ang = Effects.hash(k, 32) * MathHelper.TAU;
				double rr = s * (0.4 + Effects.hash(k, 33) * 1.4);
				double y = top * Effects.hash(k, 34) - a * 0.15 * Effects.hash(k, 35);
				float tw = 0.5F + 0.5F * MathHelper.sin(a * 0.6F + k);
				Solids.dot(glow, m, g.add(Math.cos(ang) * rr, Math.max(0.3, y), Math.sin(ang) * rr), cb, 0.5, 0x9CFF6A, tw * body);
			}
		}
	}

	// ======================================================================== ufo invasion

	/** A crop circle printed in light on the ground at the instant it is cut, then glowing down to embers. */
	static final class CropStamp extends FxEffect {
		private final Vec3d c;
		private final List<CelestialIIShapes.Mark> marks;
		private final double rot;
		/** World-space outline of every mark, sampled onto the ground once (and again after the cut settles). */
		private final List<Vec3d[]> rings = new ArrayList<>();
		private final List<Vec3d> dots = new ArrayList<>();

		CropStamp(Vec3d c, int index, int life) {
			super(life);
			this.c = c;
			this.marks = CelestialIIShapes.crop(index);
			this.rot = CelestialIIShapes.cropAngle(index);
			build();
		}

		private Vec3d world(double lx, double lz) {
			double x = c.x + lx * Math.cos(rot) - lz * Math.sin(rot);
			double z = c.z + lx * Math.sin(rot) + lz * Math.cos(rot);
			return new Vec3d(x, groundY(x, z) + 0.1, z);
		}

		private void build() {
			rings.clear();
			dots.clear();
			for (CelestialIIShapes.Mark mk : marks) {
				if (mk.ring()) {
					int segs = Math.max(16, (int) (mk.r() * 5));
					Vec3d[] pts = new Vec3d[segs + 1];
					for (int k = 0; k <= segs; k++) {
						double a0 = MathHelper.TAU * k / segs;
						pts[k] = world(mk.x() + Math.cos(a0) * mk.r(), mk.z() + Math.sin(a0) * mk.r());
					}
					rings.add(pts);
					dots.add(null);
				} else {
					rings.add(null);
					dots.add(world(mk.x(), mk.z()));
				}
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 20) {
				build();
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float t = progress(td);
			// Drawn outward from the centre in a few ticks, flaring as it completes, then cooling.
			double reach = 40 * Effects.smooth(a / 8.0F);
			float bright = (a < 10 ? 1.0F : 0.35F + 0.65F * (1 - Effects.smooth((a - 10) / 50.0F))) * (1 - Effects.smooth((t - 0.75F) / 0.25F));
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			for (int i = 0; i < marks.size(); i++) {
				CelestialIIShapes.Mark mk = marks.get(i);
				double far = Math.hypot(mk.x(), mk.z());
				if (far - mk.r() > reach) {
					continue;
				}
				Vec3d[] pts = rings.get(i);
				if (pts != null) {
					for (int k = 0; k + 1 < pts.length; k++) {
						flat(glow, m, pts[k].subtract(cam), pts[k + 1].subtract(cam), mk.w() + 0.5, 0x60D8FF, bright);
					}
				} else {
					FxDraw.disc(glow, m, dots.get(i).subtract(cam), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), mk.r(), 0xB8F4FF, bright * 0.8F, bright * 0.3F, 20);
				}
			}
			if (a < 12) {
				Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
				Solids.flare(glow, m, world(0, 0).subtract(cam).add(0, 1, 0), cb, 10, 0x80E0FF, 1 - a / 12.0F);
			}
		}
	}

	/** An abduction: a perfect cylinder of ground lifting toward the ship inside a pale light column. */
	static final class Abduction extends FxEffect {
		private final Vec3d c;
		private final double r, h;

		Abduction(Vec3d c, double r, double height, int life) {
			super(life);
			this.c = c;
			this.r = r;
			this.h = height;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float t = progress(td);
			Vec3d g = c.subtract(cam);
			float on = Effects.smooth(a / 6.0F) * (1 - Effects.smooth((t - 0.7F) / 0.3F));
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Solids.tube(glow, m, g, g.add(0, h, 0), r * 1.05, r * 1.05, 20, 0x80D8FF, 0.12F * on, false);
			Solids.tube(glow, m, g, g.add(0, h, 0), r * 0.5, r * 0.5, 12, 0xC8F4FF, 0.1F * on, false);
			for (int k = 0; k < 4; k++) {
				double y = ((a * 1.5 + k * h / 4) % h);
				FxDraw.ring(glow, m, g.add(0, y, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), r * 0.95, r * 1.08, 0xA0E8FF, 0.0F, 0.5F * on, 0.0F, 32);
			}
			// The plug of earth rising, breaking into chunks as it climbs.
			for (int k = 0; k < 26; k++) {
				double start = Effects.hash(k, 41) * 30;
				double y = Math.max(0, a - start) * (0.6 + Effects.hash(k, 42) * 0.8);
				if (y <= 0 || y > h) {
					continue;
				}
				double ang = Effects.hash(k, 43) * MathHelper.TAU + a * 0.05;
				double rr = Effects.hash(k, 44) * r * 0.8;
				Vec3d p = g.add(Math.cos(ang) * rr, y, Math.sin(ang) * rr);
				int col = k % 3 == 0 ? 0x5E9E3A : k % 3 == 1 ? 0x7A5638 : 0x7E7E7E;
				Cinematics.rock(solid, m, p, 0.8 + Effects.hash(k, 45) * 1.2, a * 0.08, k, col);
			}
		}
	}
}
