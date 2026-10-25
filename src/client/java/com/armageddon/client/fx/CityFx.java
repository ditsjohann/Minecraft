package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.CityShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Signature effects for City Destruction. {@code p.seed()} picks the effect (ids in CityDestructionEvents). */
public final class CityFx {
	private CityFx() {
	}

	public static FxEffect create(FxPayload p) {
		Vec3d a = new Vec3d(p.x(), p.y(), p.z()), b = new Vec3d(p.x2(), p.y2(), p.z2());
		return switch (p.seed()) {
			case 1 -> new MoonTide(a, p.scale(), (int) p.p1(), p.duration());
			case 2 -> new Inflation(a, p.scale(), (int) p.p1(), p.duration());
			case 3 -> new Jaws(a, p.scale(), p.p1(), (int) p.p2(), p.duration());
			case 4 -> new Maw(a, p.scale(), p.duration());
			case 5 -> new ChronoDial(a, b, p.scale(), (int) p.p1(), (int) p.p2(), p.duration());
			case 6 -> new Serpent(a, b, p.scale(), p.duration());
			case 7 -> new WorldTree(a, b, p.scale(), p.duration());
			default -> null;
		};
	}

	static double clamp01(double t) {
		return MathHelper.clamp(t, 0.0, 1.0);
	}

	static double groundY(double x, double z, double fallback) {
		ClientWorld world = MinecraftClient.getInstance().world;
		return world == null ? fallback : MegaEffects.ground(world, x, z);
	}

	// ======================================================================== MOONFALL

	/** The tide pulling the land up toward the moon, then the moon sinking into its crater and the regolith curtain. */
	static final class MoonTide extends FxEffect {
		private final Vec3d ground;
		private final double r;
		private final int fall;

		MoonTide(Vec3d ground, float r, int fall, int life) {
			super(Math.max(40, life));
			this.ground = ground;
			this.r = r;
			this.fall = fall;
		}

		private double pull(float a) {
			return clamp01((a - 140) / (double) (fall - 140));
		}

		@Override
		public boolean tick(ClientWorld world) {
			double pull = pull(age);
			if (age < fall && pull > 0 && age % 2 == 0) {
				for (int i = 0; i < 3; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double d = r * (0.4 + world.random.nextDouble() * 2.0);
					double x = ground.x + Math.cos(ang) * d, z = ground.z + Math.sin(ang) * d;
					world.addImportantParticle(ParticleTypes.CLOUD, true, x, groundY(x, z, ground.y) + 0.5, z,
							-Math.cos(ang) * 0.03, 0.15 + pull * 0.6, -Math.sin(ang) * 0.03);
				}
			}
			if (age == fall) {
				ClientFx.light(ground.add(0, r, 0), 0.7F, 0xC8D0FF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d g = ground.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			if (a < fall) {
				// Tidal strands: pale threads of water and dust drawn up off the land.
				double pull = pull(a);
				if (pull <= 0) {
					return;
				}
				VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
				for (int i = 0; i < 46; i++) {
					double ang = Solids.hash(i, 3) * MathHelper.TAU + a * 0.002;
					double d = r * (0.6 + 2.0 * Solids.hash(i, 4));
					Vec3d base = g.add(Math.cos(ang) * d, 0.3, Math.sin(ang) * d);
					double h = (10 + 60 * Solids.hash(i, 5)) * pull;
					Vec3d top = base.add(-Math.cos(ang) * h * 0.35, h, -Math.sin(ang) * h * 0.35);
					float al = (float) (0.22 * pull * (0.6 + 0.4 * Math.sin(a * 0.2 + i)));
					FxDraw.ribbon(glow, m, base, top, Vec3d.ZERO, 0.5 + pull, 0xC8DCFF, al, 0.0F);
				}
				return;
			}
			float e = a - fall;
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			// The regolith curtain: a slow grey wall of moon dust rolling out from the impact.
			Cinematics.dustWall(shade, m, cb, g, r * 1.05 + e * 2.0, r * 0.7 + e * 0.18, 0xA8A8B4, 0.8F * (float) (1 - clamp01(e / 160.0)), 17);
			Cinematics.dustWall(shade, m, cb, g, r * 0.95 + e * 0.9, r * 0.4 + e * 0.1, 0x8E8E98, 0.6F * (float) (1 - clamp01(e / 120.0)), 29);
			if (e < 64) {
				// The moon settles into its crater while the real blocks build up behind it.
				double sink = Cinematics.ease(e / 26.0);
				Vec3d c = g.add(0, MathHelper.lerp(sink, r * 0.92, r * 0.45 + 1), 0);
				VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
				double rr = r + 0.7;
				Solids.ellipsoid(solid, m, c, rr, rr, rr, 22, 40, (i, j, n) -> {
					double maria = Solids.hash(i / 3, j / 3);
					int base = maria > 0.66 ? 0x4C4C52 : 0x9A9AA0;
					return Solids.scale(base, 0.85 + 0.3 * Solids.hash(i, j));
				}, true, 1.0F);
			}
		}
	}

	// ======================================================================== BIG BANG

	/** The point, the inflation, red-shifting matter, and planets condensing out of spirals of light. */
	static final class Inflation extends FxEffect {
		private final Vec3d point;
		private final double scale;
		private final int bang;
		private final long seed;

		Inflation(Vec3d point, float scale, int bang, int life) {
			super(Math.max(40, life));
			this.point = point;
			this.scale = scale;
			this.bang = bang;
			this.seed = BlockPos.ofFloored(point).asLong();
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == bang) {
				ClientFx.light(point, 1.4F, 0xF0E8FF);
			}
			for (int i = 0; i < CityShapes.PLANETS; i++) {
				if (age == CityShapes.planetFormAt(i)) {
					ClientFx.light(point.add(CityShapes.planetOffset(seed, i, scale)), 1.0F, CityShapes.planetColor(CityShapes.planetType(seed, i)));
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d c = point.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			if (a < bang) {
				// The point, and the dark folding in on it.
				float k = (float) clamp01(a / bang);
				Solids.dot(glow, m, c, cb, 0.25 + 0.2 * Math.sin(a * 0.6) + k * 0.4, 0xFFFFFF, 0.6F + 0.4F * k);
				for (int i = 0; i < 10; i++) {
					double ang = i * MathHelper.TAU / 10 + a * 0.08;
					double d = (1 - (a * 0.02 + i * 0.1) % 1) * 12;
					Vec3d p = c.add(cb[0].multiply(Math.cos(ang) * d)).add(cb[1].multiply(Math.sin(ang) * d));
					Solids.dot(glow, m, p, cb, 0.15, 0xC0C8FF, 0.6F * k);
				}
				return;
			}
			float e = a - bang;
			// The inflating shell.
			if (e < 40) {
				double rad = scale * 2.6 * Cinematics.ease(e / 40.0);
				FxDraw.sphere(glow, m, c, rad, Vec3d.ZERO, ArsenalEffects.rainbow(e * 0.02F), 0.0F, 0.5F * (float) (1 - e / 40.0), 16, 32);
			}
			// Matter flung outward, red-shifting as it slows.
			double reach = scale * 1.7 * (1 - Math.exp(-e / 22.0));
			float fade = (float) (1 - clamp01((e - 120) / 60.0));
			int col = Effects.fade(0xC8E0FF, 0xFF6030, (float) clamp01(e / 90.0));
			for (int i = 0; i < 150 && fade > 0; i++) {
				Vec3d d = Cinematics.sphereDir(i, 13);
				double dist = reach * (0.35 + 0.65 * Solids.hash(i, 14));
				Vec3d p = c.add(d.multiply(dist));
				double speed = Math.exp(-e / 22.0) * 6 + 0.4;
				FxDraw.beam(glow, m, p, p.subtract(d.multiply(speed * 3)), Vec3d.ZERO, 0.25, col, 0.7F * fade);
			}
			// Planets condensing: a spiral of light winding in on each one.
			for (int i = 0; i < CityShapes.PLANETS; i++) {
				float tf = CityShapes.planetFormAt(i) - bang;
				float u = e - (tf - 26);
				if (u < 0 || u > 40) {
					continue;
				}
				Vec3d pc = c.add(CityShapes.planetOffset(seed, i, scale));
				double pr = CityShapes.planetRadius(seed, i, scale);
				int pcol = CityShapes.planetColor(CityShapes.planetType(seed, i));
				double k = clamp01(u / 26.0);
				for (int s = 0; s < 24; s++) {
					double ang = s * MathHelper.TAU / 24 * 2 + k * 9 + i;
					double rr = pr * (1 + 5 * (1 - k)) * (0.6 + 0.4 * ((s * 7) % 5) / 4.0);
					Vec3d p = pc.add(Math.cos(ang) * rr, Math.sin(s * 1.3) * pr * 0.5 * (1 - k), Math.sin(ang) * rr);
					Solids.dot(glow, m, p, cb, 0.3 + pr * 0.08, pcol, (float) (0.9 * (u < 26 ? 1 : 1 - (u - 26) / 14.0)));
				}
				if (u > 22 && u < 40) {
					double rr = pr * (1.2 + (u - 22) * 0.35);
					FxDraw.ring(glow, m, pc, cb[0], cb[1], rr, rr + 0.8, pcol, 0.0F, 0.7F * (float) (1 - (u - 22) / 18.0), 0.0F, 40);
				}
			}
			// Nebulae glowing softly where the glass clouds form.
			for (int k = 0; k < CityShapes.NEBULAE; k++) {
				float u = e - (CityShapes.nebulaAt(k) - bang);
				if (u < -20) {
					continue;
				}
				float al = (float) (clamp01((u + 20) / 30.0) * (1 - clamp01((e - 150) / 50.0)));
				Vec3d nc = c.add(CityShapes.nebulaOffset(seed, k, scale));
				int ncol = k == 0 ? 0xE060FF : k == 1 ? 0x40E0FF : 0x7060FF;
				Solids.dot(glow, m, nc, cb, scale * 0.28, ncol, 0.16F * al);
				Solids.dot(glow, m, nc, cb, scale * 0.12, 0xFFFFFF, 0.08F * al);
			}
		}
	}

	// ======================================================================== WORLD EATER

	/** Jaws of bone burst from the ground around a bite and snap shut over it, then sink away. */
	static final class Jaws extends FxEffect {
		private final Vec3d c;
		private final double r, toward;
		private final int snap;

		Jaws(Vec3d c, float r, float toward, int snap, int life) {
			super(Math.max(20, life));
			this.c = c;
			this.r = r;
			this.toward = toward;
			this.snap = Math.max(4, snap);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < 6) {
				for (int i = 0; i < 10; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					world.addImportantParticle(ParticleTypes.SCULK_SOUL, true, c.x + Math.cos(ang) * r, c.y + 0.5, c.z + Math.sin(ang) * r,
							0, 0.08, 0);
				}
			}
			if (age == snap) {
				ClientFx.light(c.add(0, r * 0.5, 0), 0.6F, 0x40E0C0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d g = c.subtract(cam);
			// Rise, rear back, snap, hold, sink.
			double rise = Cinematics.ease(a / (snap * 0.6));
			double close = a < snap - 6 ? -0.25 * Cinematics.ease(a / (snap - 6.0)) : MathHelper.lerp(clamp01((a - (snap - 6)) / 6.0), -0.25, 1.45);
			double sink = Cinematics.ease((a - snap - 6) / 18.0);
			double h = r * 0.95 * rise * (1 - sink);
			if (h < 0.3) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d up = new Vec3d(0, 1, 0);
			for (int jaw = 0; jaw < 2; jaw++) {
				double mid = toward + (jaw == 0 ? Math.PI : 0);
				int segs = 14;
				Vec3d prevB = null, prevT = null;
				for (int s = 0; s <= segs; s++) {
					double ang = mid + (s / (double) segs - 0.5) * Math.PI * 0.95;
					Vec3d radial = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
					Vec3d base = g.add(radial.multiply(r)).add(0, -sink * r * 0.3, 0);
					Vec3d dirTip = up.multiply(Math.cos(close)).add(radial.multiply(-Math.sin(close)));
					Vec3d tip = base.add(dirTip.multiply(h));
					if (prevB != null) {
						Solids.quad(solid, m, prevB, base, tip, prevT, 0x3A1A2A, 1.0F);
						// The gum line glows with sculk.
						FxDraw.ribbon(glow, m, prevB.add(prevT.subtract(prevB).multiply(0.85)), base.add(tip.subtract(base).multiply(0.85)),
								Vec3d.ZERO, 0.6, 0x30E0C0, 0.6F, 0.6F);
					}
					// A tooth on every segment, raking inward.
					if (s > 0 && s < segs) {
						double len = r * (0.22 + 0.12 * Solids.hash(s, jaw + 5));
						Vec3d toothDir = dirTip.multiply(0.6).add(radial.multiply(-0.8)).normalize();
						Solids.tube(solid, m, tip, tip.add(toothDir.multiply(len)), r * 0.07, 0.0, 4, 0xEDE6D6, 1.0F, true);
					}
					prevB = base;
					prevT = tip;
				}
			}
			Cinematics.dustWall(consumers.getBuffer(FxRenderLayers.SHADE), m, FxDraw.cameraBasis(yaw, pitch), g, r * 1.08, r * 0.35,
					0x4A3A40, 0.6F * (float) (1 - clamp01(a / 30.0)), 41);
		}
	}

	/** The throat: a lamprey ring of turning teeth around a sculk-lit vortex. */
	static final class Maw extends FxEffect {
		private final Vec3d c;
		private final double r;

		Maw(Vec3d c, float r, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.r = r;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 2 == 0) {
				for (int i = 0; i < 4; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double d = world.random.nextDouble() * r;
					world.addImportantParticle(ParticleTypes.SCULK_SOUL, true, c.x + Math.cos(ang) * d, c.y - 2, c.z + Math.sin(ang) * d, 0, 0.12, 0);
				}
			}
			if (age % 20 == 0) {
				ClientFx.light(c.add(0, -4, 0), 0.6F, 0x30E0C0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d g = c.subtract(cam);
			float in = (float) Cinematics.ease(a / 24.0), out = 1 - (float) clamp01((a - (lifetime - 40)) / 40.0);
			float k = in * out;
			if (k <= 0.01F) {
				return;
			}
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			// The vortex: dark spiral arms turning down into the throat.
			for (int arm = 0; arm < 5; arm++) {
				Vec3d prev = null;
				for (int s = 0; s <= 24; s++) {
					double u = s / 24.0;
					double ang = arm * MathHelper.TAU / 5 + u * 4 - a * 0.06;
					Vec3d p = g.add(Math.cos(ang) * r * (1 - u * 0.9), -u * 10 + 0.4, Math.sin(ang) * r * (1 - u * 0.9));
					if (prev != null) {
						FxDraw.ribbon(shade, m, prev, p, Vec3d.ZERO, 2.5 * (1 - u * 0.6), 0x08040C, 0.8F * k, 0.8F * k);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.4, 0x30E0C0, 0.5F * k * (float) (1 - u), 0.5F * k * (float) (1 - u));
					}
					prev = p;
				}
			}
			FxDraw.disc(shade, m, g.add(0, -2, 0), x, z, r * 0.9, 0x000000, 0.9F * k, 0.4F * k, 32);
			FxDraw.ring(glow, m, g.add(0, 0.5, 0), x, z, r * 1.02, r * 1.12 + Math.sin(a * 0.3) * 0.4, 0x30E0C0, 0.0F, 0.7F * k, 0.0F, 48);
			// The lamprey ring: rows of bone teeth around the rim, slowly turning.
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			double rise = r * 0.3 * k;
			for (int row = 0; row < 2; row++) {
				int n = 22 - row * 6;
				double rr = r * (1.0 - row * 0.25);
				for (int i = 0; i < n; i++) {
					double ang = i * MathHelper.TAU / n + a * (row == 0 ? 0.01 : -0.016);
					Vec3d radial = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
					Vec3d base = g.add(radial.multiply(rr)).add(0, -row * 2.0, 0);
					Vec3d tip = base.add(radial.multiply(-rise * 1.3)).add(0, rise, 0);
					Solids.tube(solid, m, base, tip, r * 0.06, 0.0, 4, 0xE8E0CC, 1.0F, true);
				}
			}
		}
	}

	// ======================================================================== CHRONO BREAK

	/** A clock face of light on the ground and clockwork gears in the sky; the hands run, stop, then rewind. */
	static final class ChronoDial extends FxEffect {
		private final Vec3d c, fwd;
		private final double r;
		private final int erase, restore;

		ChronoDial(Vec3d c, Vec3d fwd, float r, int erase, int restore, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.fwd = new Vec3d(fwd.x, 0, fwd.z).lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : new Vec3d(fwd.x, 0, fwd.z).normalize();
			this.r = r;
			this.erase = erase;
			this.restore = restore;
		}

		/** Clock time in "seconds": runs forward, freezes while erased, then spins back. */
		private double clock(float a) {
			if (a < erase) {
				return a * 0.6;
			}
			if (a < restore) {
				return erase * 0.6 + Math.sin(a * 2.3) * 0.05;
			}
			double back = (a - restore) * 3.2;
			return erase * 0.6 - back;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 30 == 0) {
				ClientFx.light(c.add(0, 2, 0), 0.5F, 0x50FFD0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float k = (float) (Cinematics.ease(a / 20.0) * (1 - clamp01((a - (lifetime - 30)) / 30.0)));
			if (k <= 0.01F) {
				return;
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d g = c.subtract(cam).add(0, 0.25, 0);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
			int teal = 0x50FFD0, brass = 0xE0B060;
			boolean rewinding = a >= restore;
			// The dial face.
			FxDraw.ring(glow, m, g, x, z, r - 0.6, r, teal, 0.0F, 0.8F * k, 0.0F, 96);
			FxDraw.ring(glow, m, g, x, z, r * 0.7, r * 0.7 + 0.3, teal, 0.0F, 0.4F * k, 0.0F, 80);
			FxDraw.ring(glow, m, g, x, z, 2.5, 3.0, brass, 0.0F, 0.8F * k, 0.0F, 32);
			for (int i = 0; i < 60; i++) {
				double ang = i * MathHelper.TAU / 60;
				Vec3d dir = fwd.multiply(Math.cos(ang)).add(side.multiply(Math.sin(ang)));
				double len = i % 5 == 0 ? r * 0.12 : r * 0.04;
				FxDraw.ribbon(glow, m, g.add(dir.multiply(r - 0.8 - len)), g.add(dir.multiply(r - 0.8)), Vec3d.ZERO,
						i % 5 == 0 ? 0.5 : 0.2, i % 15 == 0 ? brass : teal, 0.8F * k, 0.8F * k);
			}
			// Hands: hour, minute, second. The rewind leaves ghost trails behind them.
			double s = clock(a);
			int ghosts = rewinding ? 6 : 1;
			for (int gI = 0; gI < ghosts; gI++) {
				double sg = rewinding ? clock(a - gI * 1.5F) : s;
				float al = k * (gI == 0 ? 1.0F : 0.35F * (1 - gI / (float) ghosts));
				hand(glow, m, g, side, sg / 720.0, r * 0.45, 0.9, brass, al);
				hand(glow, m, g, side, sg / 60.0, r * 0.7, 0.55, teal, al);
				hand(glow, m, g, side, sg, r * 0.92, 0.2, 0xFFFFFF, al);
			}
			// Clockwork overhead: three meshing gears that turn with the hands.
			Vec3d sky = g.add(0, r * 1.25, 0);
			double turn = s * 0.1;
			gear(glow, m, sky, r * 0.55, 24, turn, brass, 0.55F * k);
			gear(glow, m, sky.add(fwd.multiply(r * 0.86)), r * 0.32, 14, -turn * 24 / 14.0 + 0.11, teal, 0.5F * k);
			gear(glow, m, sky.add(side.multiply(-r * 0.78)).add(0, r * 0.1, 0), r * 0.24, 10, -turn * 24 / 10.0, brass, 0.45F * k);
		}

		private void hand(VertexConsumer glow, Matrix4f m, Vec3d g, Vec3d side, double turns, double len, double width, int rgb, float alpha) {
			double ang = turns * MathHelper.TAU;
			Vec3d dir = fwd.multiply(Math.cos(ang)).add(side.multiply(Math.sin(ang)));
			FxDraw.beam(glow, m, g.add(0, 0.1, 0), g.add(dir.multiply(len)).add(0, 0.1, 0), Vec3d.ZERO, width, rgb, alpha);
		}

		private static void gear(VertexConsumer glow, Matrix4f m, Vec3d c, double r, int teeth, double turn, int rgb, float alpha) {
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			FxDraw.ring(glow, m, c, x, z, r * 0.82, r * 0.9, rgb, 0.0F, alpha, 0.0F, 64);
			FxDraw.ring(glow, m, c, x, z, r * 0.18, r * 0.24, rgb, 0.0F, alpha, 0.0F, 24);
			for (int i = 0; i < teeth; i++) {
				double ang = i * MathHelper.TAU / teeth + turn;
				Vec3d d = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
				FxDraw.ribbon(glow, m, c.add(d.multiply(r * 0.88)), c.add(d.multiply(r * 1.05)), Vec3d.ZERO, r * 0.09, rgb, alpha, alpha);
				if (i % (teeth / 4 == 0 ? 1 : teeth / 4) == 0) {
					FxDraw.ribbon(glow, m, c.add(d.multiply(r * 0.22)), c.add(d.multiply(r * 0.84)), Vec3d.ZERO, r * 0.05, rgb, alpha * 0.7F, alpha * 0.7F);
				}
			}
		}
	}

	// ======================================================================== RAGNAROK

	/** Jormungandr: a molten-seamed serpent breaching through the ground along its coil, diving at the centre. */
	static final class Serpent extends FxEffect {
		private final Vec3d c;
		private final double reach, phase;

		Serpent(Vec3d c, Vec3d heading, float reach, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.reach = reach;
			this.phase = CityShapes.serpentPhase(heading);
		}

		private Vec3d at(double s, float a, int k, double body) {
			Vec3d o = CityShapes.serpent(s, reach, phase);
			double x = c.x + o.x, z = c.z + o.z;
			// Breaching arcs travel down the body; the head dives as it reaches the centre.
			double arc = Math.sin(k * 0.32 - a * 0.25) * body * 1.6;
			double dive = s > 0.92 ? (s - 0.92) / 0.08 * body * 6 : 0;
			return new Vec3d(x, groundY(x, z, c.y) + arc - dive, z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			double s = CityShapes.serpentHead(age);
			if (s > 0 && s < 1) {
				Vec3d head = at(s, age, 0, 4);
				ClientFx.light(head.add(0, 4, 0), 0.8F, 0xFF6020);
				for (int i = 0; i < 3; i++) {
					world.addImportantParticle(ParticleTypes.FLAME, true, head.x + world.random.nextGaussian() * 3, head.y + 1, head.z + world.random.nextGaussian() * 3,
							0, 0.1, 0);
				}
				world.addImportantParticle(ParticleTypes.LARGE_SMOKE, true, head.x, head.y + 3, head.z, 0, 0.15, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double head = CityShapes.serpentHead(a);
			if (head <= 0) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			int n = 64;
			double ds = 0.011;
			Vec3d prev = null;
			for (int k = 0; k <= n; k++) {
				double s = head - k * ds;
				if (s < 0) {
					break;
				}
				double body = 4.2 * (1 - k / (double) n * 0.75);
				Vec3d p = at(s, a, k, body).subtract(cam);
				if (prev != null) {
					int scale = k % 2 == 0 ? 0x2A0C08 : 0x401410;
					Solids.tube(solid, m, prev, p, body * 1.04, body, 8, scale, 1.0F, true);
					// Molten seams between the scales, and a ridge of spines.
					Solids.tube(glow, m, prev, p, body * 1.12, body * 1.08, 8, 0xFF5010, 0.22F, false);
					if (k % 2 == 0) {
						Solids.tube(solid, m, p.add(0, body * 0.9, 0), p.add(0, body * 1.9, 0), body * 0.25, 0.0, 4, 0x1A0806, 1.0F, true);
					}
				}
				prev = p;
			}
			// The head: a heavy wedge, horns, and eyes like coals.
			Vec3d h0 = at(head, a, 0, 4.2).subtract(cam);
			Vec3d h1 = at(Math.max(0, head - ds), a, 1, 4.2).subtract(cam);
			Vec3d dir = h0.subtract(h1);
			dir = dir.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : dir.normalize();
			Vec3d snout = h0.add(dir.multiply(7));
			Solids.tube(solid, m, h0, snout, 5.2, 2.2, 8, 0x30100A, 1.0F, true);
			Vec3d side = new Vec3d(-dir.z, 0, dir.x);
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Vec3d hornBase = h0.add(side.multiply(sgn * 3.2)).add(0, 3.5, 0);
				Solids.tube(solid, m, hornBase, hornBase.add(dir.multiply(-6)).add(side.multiply(sgn * 2)).add(0, 4, 0), 1.1, 0.0, 5, 0x0E0604, 1.0F, true);
				Vec3d eye = h0.add(dir.multiply(3.5)).add(side.multiply(sgn * 2.8)).add(0, 2.2, 0);
				Solids.dot(glow, m, eye, cb, 0.9, 0xFFC040, 1.0F);
				Solids.dot(glow, m, eye, cb, 2.6, 0xFF4010, 0.4F);
			}
			FxDraw.beam(glow, m, snout, snout.add(dir.multiply(4)).add(0, -1, 0), Vec3d.ZERO, 1.6, 0xFF8030, 0.7F);
		}
	}

	/** Yggdrasil across the sky: a black silhouette of a tree that catches fire, burns, and falls. */
	static final class WorldTree extends FxEffect {
		private record Branch(Vec3d a, Vec3d b, double r, int depth) {
		}

		private final Vec3d base, fwd, side;
		private final List<Branch> branches = new ArrayList<>();

		WorldTree(Vec3d c, Vec3d heading, float reach, int life) {
			super(Math.max(40, life));
			Vec3d f = new Vec3d(heading.x, 0, heading.z);
			this.fwd = f.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : f.normalize();
			this.side = new Vec3d(-fwd.z, 0, fwd.x);
			this.base = c.add(fwd.multiply(520)).add(0, -60, 0);
			grow(base, new Vec3d(0, 1, 0), 300, 26, 0, 1);
		}

		private void grow(Vec3d from, Vec3d dir, double len, double r, int depth, int salt) {
			Vec3d to = from.add(dir.multiply(len));
			branches.add(new Branch(from, to, r, depth));
			if (depth >= 4) {
				return;
			}
			int kids = depth == 0 ? 5 : 3;
			for (int i = 0; i < kids; i++) {
				double spread = depth == 0 ? 1.0 : 0.7;
				double ang = (i - (kids - 1) / 2.0) * spread + (Solids.hash(salt, i) - 0.5) * 0.5;
				Vec3d out = side.multiply(Math.sin(ang)).add(fwd.multiply((Solids.hash(salt, i + 7) - 0.5) * 0.6));
				Vec3d nd = dir.multiply(Math.cos(ang) * 0.8).add(out).add(0, 0.25, 0).normalize();
				grow(to, nd, len * (0.62 + 0.1 * Solids.hash(salt, i + 3)), r * 0.55, depth + 1, salt * 7 + i + 1);
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float burn = (float) clamp01((a - 150) / 200.0);
			double fall = Cinematics.ease((a - CityShapes.TREE_FALL) / 60.0);
			double sinkDown = Cinematics.ease((a - CityShapes.TREE_FALL - 50) / 40.0) * 500;
			double tilt = fall * 0.7;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			float rise = (float) Cinematics.ease(a / 40.0);
			for (Branch br : branches) {
				Vec3d p = tilt(br.a(), tilt).subtract(cam).add(0, -sinkDown - (1 - rise) * 300, 0);
				Vec3d q = tilt(br.b(), tilt).subtract(cam).add(0, -sinkDown - (1 - rise) * 300, 0);
				Solids.tube(solid, m, p, q, br.r(), br.r() * 0.7, 6, 0x120808, 1.0F, false);
				// Fire climbs the tree from its outermost twigs inward.
				float lit = (float) clamp01(burn * 5 - (4 - br.depth()));
				if (lit > 0) {
					float flick = 0.7F + 0.3F * MathHelper.sin(a * 0.4F + br.depth() * 3 + (float) br.r());
					Solids.tube(glow, m, p, q, br.r() * 1.25, br.r() * 0.95, 6, 0xFF5010, 0.35F * lit * flick, false);
					if (br.depth() >= 3) {
						Solids.dot(glow, m, q, cb, br.r() * 6, 0xFF8030, 0.35F * lit * flick);
					}
				}
			}
		}

		private Vec3d tilt(Vec3d p, double ang) {
			if (ang <= 0) {
				return p;
			}
			Vec3d d = p.subtract(base);
			double up = d.y, along = d.dotProduct(fwd);
			Vec3d rest = d.subtract(fwd.multiply(along)).subtract(0, up, 0);
			// Falls away from the city, toward the horizon.
			double nUp = up * Math.cos(ang) - along * Math.sin(ang);
			double nAlong = along * Math.cos(ang) + up * Math.sin(ang);
			return base.add(rest).add(fwd.multiply(nAlong)).add(0, nUp, 0);
		}
	}
}
