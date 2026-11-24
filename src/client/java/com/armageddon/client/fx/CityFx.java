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
			case 5 -> new ChronoDial(a, b, p.scale(), (int) p.p1(), (int) p.p2(), p.duration());
			case 8 -> new Worm(a, p.scale(), p.p1(), p.duration());
			case 9 -> new Sword(a, b, p.scale(), (int) p.p1(), p.duration());
			default -> null;
		};
	}

	static double clamp01(double t) {
		return MathHelper.clamp(t, 0.0, 1.0);
	}

	static double groundY(double x, double z, double fallback) {
		ClientWorld world = com.armageddon.client.render.FxDraw.world();
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
				// The landing: a cold blue-white flash, a spray of glowing rock, and the shock front.
				float k = (float) clamp01(e / 40.0);
				FxKit.fire(consumers, m, cb, g.add(0, r * 0.4, 0), r * 0.9, 0xD8E0FF, k, 0.9F * (1 - k), a, 111);
				FxKit.sparks(consumers, m, g.add(0, r * 0.3, 0), r * 3.5, 80, 0xFFE0C0, k, 1.0F, 112);
				FxKit.groundShock(consumers, m, g, r * (1.2 + e * 0.12), 0xD0D8FF, 1 - k);
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
				Solids.dot(glow, m, c, cb, 0.25 + 0.2 * Math.sin(a * 0.6 * (1 + k * 3)) + k * 0.4, 0xFFFFFF, 0.6F + 0.4F * k);
				// Light bending round it: lensing rings tightening and pulsing faster as the moment comes.
				for (int r = 0; r < 3; r++) {
					double rr = (2 + r * 2.5) * (1.2 - k * 0.6) * (1 + 0.15 * Math.sin(a * (0.2 + k * 0.6) + r));
					FxDraw.ring(glow, m, c, cb[0], cb[1], rr, rr + 0.35, 0xC8D0FF, 0.0F, 0.35F * k, 0.0F, 48);
				}
				for (int i = 0; i < 10; i++) {
					double ang = i * MathHelper.TAU / 10 + a * 0.08;
					double d = (1 - (a * 0.02 + i * 0.1) % 1) * 12;
					Vec3d p = c.add(cb[0].multiply(Math.cos(ang) * d)).add(cb[1].multiply(Math.sin(ang) * d));
					Solids.dot(glow, m, p, cb, 0.15, 0xC0C8FF, 0.6F * k);
				}
				return;
			}
			float e = a - bang;
			renderVoid(consumers, m, c, cb, e);
			if (e < 50) {
				FxKit.flare(consumers, m, cb, c, scale * (0.8 + e * 0.05), 0xF4F0FF, (float) (1 - e / 50.0), e * 0.03);
				FxKit.shock(consumers, m, c, cb[0], cb[1], scale * 3.0 * Cinematics.ease(e / 50.0), 0xE8E0FF, (float) (1 - e / 50.0));
			}
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
			// The newborn sun at the heart of it, its corona turning, and the orbits of its planets drawn round it.
			if (e > 60) {
				float sun = (float) clamp01((e - 60) / 20.0);
				double sr = Math.max(1.5, scale * 0.06);
				FxDraw.sphere(glow, m, c, sr, Vec3d.ZERO, 0xFFF0C0, 0.9F * sun, 0.6F * sun, 14, 24);
				FxDraw.sphere(glow, m, c, sr * 1.8, Vec3d.ZERO, 0xFFA040, 0.0F, 0.35F * sun, 14, 24);
				for (int i = 0; i < 18; i++) {
					double ang = i * MathHelper.TAU / 18 + a * 0.01 * (i % 2 == 0 ? 1 : -1);
					double len = sr * (1.6 + 0.8 * Math.sin(a * 0.07 + i * 1.7));
					Vec3d d = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
					FxDraw.ribbon(glow, m, c.add(d.multiply(sr * 0.9)), c.add(d.multiply(len)), Vec3d.ZERO, sr * 0.35, 0xFFC060, 0.45F * sun, 0.0F);
				}
				for (int i = 0; i < CityShapes.PLANETS; i++) {
					if (e < CityShapes.planetFormAt(i) - bang) {
						continue;
					}
					Vec3d off = CityShapes.planetOffset(seed, i, scale);
					double orb = Math.hypot(off.x, off.z);
					FxDraw.ring(glow, m, c.add(0, off.y, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), orb - 0.4, orb + 0.4,
							CityShapes.planetColor(CityShapes.planetType(seed, i)), 0.0F, 0.45F * sun, 0.0F, 96);
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

		/**
		 * Outside creation there is nothing: nested dark shells close in around the new system so the world beyond
		 * fades into a deep void, with faint galaxies and dust hanging at different depths between the shells.
		 */
		private void renderVoid(VertexConsumerProvider consumers, Matrix4f m, Vec3d c, Vec3d[] cb, float e) {
			double v = Math.min(80.0, scale * 1.4);
			float in = (float) clamp01(e / 30.0), out = (float) (1 - clamp01((e - (lifetime - bang - 80)) / 80.0));
			float k = in * out;
			if (k <= 0.01F) {
				return;
			}
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			double[] shells = {1.0, 1.12, 1.26, 1.45, 1.7};
			for (int i = 0; i < shells.length; i++) {
				FxDraw.sphere(shade, m, c, v * shells[i], Vec3d.ZERO, 0x020108, 0.35F * k, 0.5F * k, 18, 36);
				// Between each pair of shells, a sparse layer of very distant things: the deeper, the dimmer.
				double depth = v * (shells[i] - 0.05);
				float dim = (float) (1.0 - i * 0.17) * k;
				for (int j = 0; j < 26; j++) {
					Vec3d d = Cinematics.sphereDir(j + i * 40, 71);
					Vec3d p = c.add(d.multiply(depth));
					double sz = 0.3 + Solids.hash(j, i + 72) * 0.6;
					if (j % 9 == 0) {
						// A tiny spiral galaxy, tilted.
						Vec3d[] gb = ArsenalEffects.basis(Cinematics.sphereDir(j, 73));
						FxDraw.disc(glow, m, p, gb[0], gb[1], v * 0.05, j % 2 == 0 ? 0xB0A0FF : 0xFFC8A0, 0.25F * dim, 0.0F, 16);
						Solids.dot(glow, m, p, cb, v * 0.008, 0xFFFFFF, 0.7F * dim);
					} else {
						Solids.dot(glow, m, p, cb, sz, j % 4 == 0 ? 0xC8D0FF : 0xFFFFFF, 0.5F * dim);
					}
				}
			}
			// The edge of creation: a faint violet rim of dust where the new universe stops.
			FxDraw.sphere(glow, m, c, v * 0.97, Vec3d.ZERO, 0x6040C0, 0.0F, 0.3F * k, 18, 36);
			for (int j = 0; j < 40; j++) {
				Vec3d d = Cinematics.sphereDir(j, 77);
				Solids.dot(glow, m, c.add(d.multiply(v * 0.96)), cb, v * 0.04, 0x8060FF, 0.06F * k);
			}
		}
	}

	// ======================================================================== WORLD EATER

	/**
	 * The world worm: a gigantic segmented body whose skin is a drifting night sky, ringed with violet bands and
	 * strung with constellations, behind a round maw of bone teeth around a swirling cosmic throat. Its head follows
	 * {@link CityShapes#worm} exactly, so the tunnels the server carves are where the body runs.
	 */
	static final class Worm extends FxEffect {
		private final Vec3d base;
		private final double reach, phase;

		Worm(Vec3d base, float reach, float phase, int life) {
			super(Math.max(40, life));
			this.base = base;
			this.reach = reach;
			this.phase = phase;
		}

		private Vec3d at(double t) {
			Vec3d o = CityShapes.worm(t, reach, phase);
			return base.add(o);
		}

		private static double radius(int i) {
			double u = i / (double) CityShapes.WORM_SEGMENTS;
			return CityShapes.WORM_R * (i < 4 ? 0.86 + 0.035 * i : 1.0 - 0.6 * Math.pow(u, 1.6));
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < CityShapes.WORM_START - 5) {
				return super.tick(world);
			}
			Vec3d h = at(age);
			skin(h);
			Vec3d cam = ClientFx.cameraPos();
			double d = cam.distanceTo(h);
			ClientFx.shake = Math.max(ClientFx.shake, (float) MathHelper.clamp(1 - d / 160, 0, 1) * 3.0F);
			if (age % 3 == 0) {
				ClientFx.light(h, 0.6F, 0xA060FF);
			}
			return super.tick(world);
		}

		/**
		 * The body is a living night sky. Every tick a few stretches of it (a different few each tick, so the whole body
		 * is covered every {@code STEP} ticks) are dressed in particles that ride along with them: nebula, stars,
		 * aurora, and stardust peeling off into a wake. Only what is above ground is dressed; the maw pulls light in.
		 */
		private void skin(Vec3d h) {
			int n = CityShapes.WORM_SEGMENTS, step = 7;
			double lag = CityShapes.WORM_LAG;
			for (int i = age % step; i < n; i += step) {
				Vec3d p = at(age - i * lag);
				double r = radius(i);
				if (p.y < groundY(p.x, p.z, base.y) - r) {
					continue;
				}
				Vec3d span = at(age - (i + step) * lag).subtract(p);
				Vec3d v = p.subtract(at(age - 1 - i * lag));
				com.armageddon.client.vfx.FxEngine.playMoving(com.armageddon.vfx.VfxIds.WORM_SKIN, p.x, p.y, p.z, (float) span.x, (float) span.y,
						(float) span.z, (float) (r / CityShapes.WORM_R), 0, age * 131L + i, (float) v.x, (float) v.y, (float) v.z);
			}
			if (age % 2 == 0 && h.y > groundY(h.x, h.z, base.y) - CityShapes.WORM_R) {
				Vec3d dir = h.subtract(at(age - 1));
				Vec3d mouth = h.add(dir.normalize().multiply(CityShapes.WORM_R * 0.35));
				com.armageddon.client.vfx.FxEngine.playMoving(com.armageddon.vfx.VfxIds.WORM_MOUTH, mouth.x, mouth.y, mouth.z, (float) dir.x,
						(float) dir.y, (float) dir.z, 1.0F, 0, age * 977L, (float) dir.x, (float) dir.y, (float) dir.z);
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a < CityShapes.WORM_START - 40) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Solids.Cell sky = Solids.nightSky(a, 29);
			int n = CityShapes.WORM_SEGMENTS;
			Vec3d prev = at(a).subtract(cam);
			for (int i = 0; i < n; i++) {
				Vec3d next = at(a - (i + 1) * CityShapes.WORM_LAG).subtract(cam);
				double r0 = radius(i), r1 = radius(i + 1);
				// A slimmer solid core for its mass and silhouette; the nebula skin around it is particles (see skin()).
				Solids.tube(solid, m, prev, next, r0 * 0.78, r1 * 0.78, 14, 2, sky, true, 1.0F);
				Vec3d axis = prev.subtract(next);
				if (axis.lengthSquared() > 1.0E-4) {
					Vec3d[] sb = ArsenalEffects.basis(axis);
					if (i % 3 == 0) {
						// Glowing violet segment rings, and a ridge of bony spines along the back.
						float pulse = 0.55F + 0.45F * MathHelper.sin(a * 0.2F - i * 0.5F);
						FxDraw.ring(glow, m, prev, sb[0], sb[1], r0 * 1.0, r0 * 1.1, i % 6 == 0 ? 0xC070FF : 0x50D0FF, 0.0F, 0.8F * pulse, 0.0F, 28);
						Vec3d upish = sb[1].y < 0 ? sb[1].multiply(-1) : sb[1];
						Solids.tube(solid, m, prev.add(upish.multiply(r0 * 0.9)), prev.add(upish.multiply(r0 * 1.7)).subtract(axis.normalize().multiply(r0 * 0.4)),
								r0 * 0.22, 0.2, 6, 0xE0D8C4, 1.0F, true);
					}
				}
				prev = next;
			}
			// The head: a round maw ringed with bone teeth, a swirling throat, and a crown of eyes.
			Vec3d h = at(a).subtract(cam);
			Vec3d dir = at(a).subtract(at(a - 1));
			dir = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : dir.normalize();
			Vec3d[] hb = ArsenalEffects.basis(dir);
			double R = CityShapes.WORM_R;
			Vec3d mouth = h.add(dir.multiply(R * 0.35));
			double gape = 0.75 + 0.25 * Math.sin(a * 0.25);
			for (int k = 0; k < 14; k++) {
				double ang = k * MathHelper.TAU / 14;
				Vec3d out = hb[0].multiply(Math.cos(ang)).add(hb[1].multiply(Math.sin(ang)));
				Vec3d root = mouth.add(out.multiply(R * 0.88));
				Vec3d tip = root.add(dir.multiply(R * 0.9)).subtract(out.multiply(R * 0.35 * gape));
				Solids.tube(solid, m, root, tip, R * 0.13, 0.1, 5, 0xEEE6D0, 1.0F, true);
			}
			FxDraw.disc(glow, m, mouth, hb[0], hb[1], R * 0.8, 0x2A0A50, 0.9F, 0.6F, 28);
			for (int k = 0; k < 3; k++) {
				Vec3d prevS = null;
				for (int s2 = 0; s2 <= 20; s2++) {
					double u = s2 / 20.0;
					double ang = k * MathHelper.TAU / 3 + u * 7 - a * 0.15;
					Vec3d p = mouth.add(hb[0].multiply(Math.cos(ang) * R * 0.75 * u)).add(hb[1].multiply(Math.sin(ang) * R * 0.75 * u)).subtract(dir.multiply(R * 0.3 * (1 - u)));
					if (prevS != null) {
						FxDraw.ribbon(glow, m, prevS, p, Vec3d.ZERO, R * 0.08, k == 0 ? 0xFFFFFF : 0xC070FF, 0.9F, 0.9F);
					}
					prevS = p;
				}
			}
			Solids.dot(glow, m, mouth.subtract(dir.multiply(R * 0.3)), cb, R * 0.35, 0xFFFFFF, 0.9F);
			Solids.dot(glow, m, mouth, cb, R * 2.2, 0x8040FF, 0.25F);
			FxKit.halo(consumers, m, cb, h, R * 4.0, 0x6020C0, 0.2F);
			// Where it breaks the surface, a violet shock rolls out over the ground (the debris is the server's breach).
			Vec3d hw = at(a);
			double gy = groundY(hw.x, hw.z, base.y);
			if (Math.abs(hw.y - gy) < R * 2.5) {
				float near = (float) (1 - Math.abs(hw.y - gy) / (R * 2.5));
				FxKit.groundShock(consumers, m, new Vec3d(hw.x, gy, hw.z).subtract(cam), R * (1.5 + (a % 20) * 0.15), 0xA060FF, 0.5F * near);
			}
			for (int k = 0; k < 6; k++) {
				double ang = k * MathHelper.TAU / 6 + 0.3;
				Vec3d out = hb[0].multiply(Math.cos(ang)).add(hb[1].multiply(Math.sin(ang)));
				Vec3d eye = h.subtract(dir.multiply(R * 0.6)).add(out.multiply(R * 0.98));
				Solids.dot(glow, m, eye, cb, R * 0.12, 0xFFFFFF, 1.0F);
				Solids.dot(glow, m, eye, cb, R * 0.35, 0x60F0FF, 0.45F);
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

	/**
	 * Surtr's sword: a blade of fire the size of a tower falls point-first out of a burning sky, strikes the centre,
	 * stands there burning while the world splits, then crumbles to embers. Laid along the event heading.
	 */
	static final class Sword extends FxEffect {
		private final Vec3d c, fwd, side;
		private final double reach;
		private final int strike;

		Sword(Vec3d c, Vec3d heading, float reach, int strike, int life) {
			super(Math.max(40, life));
			this.c = c;
			Vec3d h = new Vec3d(heading.x, 0, heading.z);
			this.fwd = h.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : h.normalize();
			this.side = new Vec3d(-fwd.z, 0, fwd.x);
			this.reach = reach;
			this.strike = strike;
		}

		/** Height of the blade's point above the ground: falling, then buried a little, then sinking away. */
		private double point(float a) {
			if (a < strike) {
				double u = a / strike;
				return 700 * (1 - u * u) - 8 * u * u;
			}
			return -8 - Math.max(0, a - strike - 140) * 0.6;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == strike) {
				ClientFx.flash(c, 100000, 1.0F, 0xFFB060);
				ClientFx.shake = 9.0F;
				ClientFx.light(c.add(0, 40, 0), 1.6F, 0xFF8030);
			}
			// The blade burns: fire, embers and heat smoke shed all along it (left behind as it falls: a wake).
			float alive = 1 - (float) clamp01((age - strike - 140) / 60.0);
			if (alive > 0.02F) {
				Vec3d tip = c.add(0, point(age), 0);
				com.armageddon.client.vfx.FxEngine.play(com.armageddon.vfx.VfxIds.SWORD_FLAME, tip.x, tip.y, tip.z, 0, 230, 0, alive, 0, age * 2269L);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			float crumble = (float) clamp01((a - strike - 140) / 60.0);
			float alive = 1 - crumble;
			if (alive <= 0.01F) {
				return;
			}
			Vec3d g = c.subtract(cam);
			Vec3d tip = g.add(0, point(a), 0);
			double blade = 230, width = 16, thick = 3.2;
			Vec3d up = new Vec3d(0, 1, 0);
			Vec3d guard = tip.add(up.multiply(blade));
			// The blade: a dark core blazing along both edges, a fuller of fire down the middle, flames licking off it.
			int steps = 12;
			for (int i = 0; i < steps; i++) {
				double u0 = i / (double) steps, u1 = (i + 1) / (double) steps;
				double w0 = width * Math.min(1, u0 * 5) * (1 - 0.15 * u0), w1 = width * Math.min(1, u1 * 5) * (1 - 0.15 * u1);
				Vec3d p0 = tip.add(up.multiply(blade * u0)), p1 = tip.add(up.multiply(blade * u1));
				int core = Solids.mix(0x2A0A04, 0x5A1A08, u0);
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d e0 = p0.add(side.multiply(sgn * w0 * 0.5)), e1 = p1.add(side.multiply(sgn * w1 * 0.5));
					Vec3d f0 = p0.add(fwd.multiply(sgn * thick * Math.min(1, u0 * 5))), f1 = p1.add(fwd.multiply(sgn * thick * Math.min(1, u1 * 5)));
					Solids.quad(solid, m, e0, e1, f1, f0, Solids.scale(core, sgn > 0 ? 1.0 : 0.7), 1.0F);
					Solids.quad(solid, m, p0.add(side.multiply(-sgn * w0 * 0.5)), p1.add(side.multiply(-sgn * w1 * 0.5)), f1, f0, Solids.scale(core, sgn > 0 ? 0.8 : 0.6), 1.0F);
					FxDraw.ribbon(glow, m, e0, e1, Vec3d.ZERO, 2.2, 0xFFD060, 0.95F * alive, 0.95F * alive);
					FxDraw.ribbon(glow, m, e0, e1, Vec3d.ZERO, 7.0, 0xFF6020, 0.35F * alive, 0.35F * alive);
				}
				float flick = 0.7F + 0.3F * MathHelper.sin(a * 0.9F + i * 1.7F);
				FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, width * 0.22, 0xFFF0B0, 0.85F * flick * alive, 0.85F * flick * alive);
			}
			// Crossguard, grip and pommel.
			Solids.tube(solid, m, guard.subtract(side.multiply(38)), guard.add(side.multiply(38)), 4.5, 4.5, 8, 0x3A1A0A, 1.0F, true);
			FxDraw.ribbon(glow, m, guard.subtract(side.multiply(40)), guard.add(side.multiply(40)), Vec3d.ZERO, 3.0, 0xFF8030, 0.8F * alive, 0.8F * alive);
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Vec3d end = guard.add(side.multiply(sgn * 38));
				Solids.tube(solid, m, end, end.add(side.multiply(sgn * 6)).add(0, 12, 0), 4.5, 1.0, 6, 0x3A1A0A, 1.0F, true);
				Solids.dot(glow, m, end.add(0, 12, 0), cb, 6, 0xFFB040, 0.9F * alive);
			}
			Vec3d grip = guard.add(0, 50, 0);
			Solids.tube(solid, m, guard, grip, 3.6, 3.6, 8, 0x1A0A06, 1.0F, true);
			for (int k = 0; k < 6; k++) {
				Vec3d wrap = guard.add(0, 6 + k * 7.5, 0);
				Solids.tube(solid, m, wrap, wrap.add(0, 2.4, 0), 4.0, 4.0, 8, 0x8A4A1A, 1.0F, true);
			}
			Solids.sphere(solid, m, grip.add(0, 5, 0), 7, 0x3A1A0A, 8, 12, true);
			Solids.dot(glow, m, grip.add(0, 5, 0), cb, 12, 0xFF6020, 0.8F * alive);
			if (a < strike) {
				// Falling: a burning wake streaming up from the blade.
				float k = (float) clamp01(a / strike);
				Solids.dot(glow, m, tip, cb, width * 1.6, 0xFFC060, 0.6F * k);
				// The target burns ahead of it: a ring of fire marking the strike.
				FxDraw.ring(glow, m, g.add(0, 0.6, 0), side, fwd, 24 * (1 - k * 0.6), 30 * (1 - k * 0.6), 0xFF6020, 0.0F, 0.8F * k, 0.0F, 48);
			} else {
				// Struck: the land splits away from it in a line of fire both ways, and a glow climbs the blade.
				float e = a - strike;
				float open = (float) clamp01(e / 30.0) * alive;
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d end = g.add(fwd.multiply(sgn * reach * Math.min(1, e / 20.0)));
					FxDraw.ribbon(glow, m, g.add(0, 0.8, 0), end.add(0, 0.8, 0), new Vec3d(0, 100, 0), 10, 0xFF5010, 0.8F * open, 0.2F * open);
				}
				FxDraw.disc(glow, m, g.add(0, 0.6, 0), side, fwd, 40 + e * 0.5, 0xFF8030, 0.6F * open, 0.0F, 40);
				Solids.dot(glow, m, g.add(0, 20, 0), cb, 60, 0xFF6020, 0.3F * open);
				// The blow itself: a wall of fire, sparks raining out, and black smoke rolling up along the split.
				if (e < 60) {
					float k = e / 60.0F;
					FxKit.fire(consumers, m, cb, g.add(0, 12, 0), 34, 0xFFC060, k, alive, a, 131);
					FxKit.sparks(consumers, m, g.add(0, 4, 0), 160, 120, 0xFFA040, k, alive, 132);
					FxKit.groundShock(consumers, m, g, 40 + e * 5, 0xFF8030, (1 - k) * alive);
				}
				java.util.List<Effects.Puff> smoke = FxKit.puffs();
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					for (int i = 1; i <= 6; i++) {
						Vec3d along = g.add(fwd.multiply(sgn * reach * Math.min(1, e / 20.0) * i / 6.0));
						FxKit.column(along, 30 + Math.min(e, 120) * 0.6, 12, a, 0x5A4A44, 0x1E1614, 0.45F * open, 133 + i * 7 + sgn, smoke);
					}
				}
				FxKit.flush(smoke, consumers, m, cb);
			}
		}
	}
}
