package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Paths;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/** City-scale set pieces: the twister, the UFO, the colossal spear and the incoming alien rocket. */
public final class MegaEffects {
	private MegaEffects() {
	}

	static double ground(ClientWorld world, double x, double z) {
		return world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
	}

	// ======================================================================== TWISTER

	public static class Twister extends FxEffect {
		private static final double H = 130;
		private final Vec3d start, dir;
		private final double radius, speed;
		private Vec3d base;

		public Twister(Vec3d start, Vec3d dir, double radius, double speed, int life) {
			super(Math.max(20, life));
			this.start = start;
			this.dir = new Vec3d(dir.x, 0, dir.z).normalize();
			this.radius = radius;
			this.speed = speed;
			this.base = start;
		}

		private Vec3d at(ClientWorld world, double t) {
			Vec3d p = Paths.twister(start, dir, speed, t);
			return new Vec3d(p.x, world == null ? start.y : ground(world, p.x, p.z), p.z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			base = at(world, age);
			// The funnel is a particle vortex riding the twister: smoke, streaks and debris held to its wall, and the wall
			// cloud turning over it. Both carry the twister's motion, so the top lags and it leans into its track.
			float env = ArsenalEffects.envelope(age, lifetime, 30, 40);
			if (env > 0.05F) {
				Vec3d v = at(world, age + 1).subtract(base);
				com.armageddon.client.vfx.FxEngine.playMoving(com.armageddon.vfx.VfxIds.TWISTER_FUNNEL, base.x, base.y, base.z, 0, 1, 0,
						(float) (radius / 24.0) * env, 0, age * 7919L, (float) v.x, 0, (float) v.z);
				if (age % 3 == 0) {
					Vec3d top = center(H, age);
					com.armageddon.client.vfx.FxEngine.playMoving(com.armageddon.vfx.VfxIds.WALL_CLOUD, top.x, top.y, top.z, 0, 1, 0,
							(float) (radius / 24.0), 0, age * 104729L, (float) v.x, 0, (float) v.z);
				}
			}
			Vec3d cam = ClientFx.cameraPos();
			double d = Math.hypot(cam.x - base.x, cam.z - base.z);
			double reach = radius * 7;
			if (d < reach) {
				float k = (float) (1 - d / reach);
				ClientFx.shake = Math.max(ClientFx.shake, k * 7.0F);
				ClientFx.distortion = Math.max(ClientFx.distortion, k * 0.5F);
			}
			if (age % 20 == 0) {
				ClientFx.playGlobal(ModSounds.NUKE_RUMBLE, (float) MathHelper.clamp(1.0 - d / 400.0, 0.05, 1.0), 0.45F);
			}
			ClientFx.tintSky(0x2A323A, Math.min(0.85F, age / 60.0F) * (age > lifetime - 40 ? (lifetime - age) / 40.0F : 1.0F));
			if (age % 2 == 0) {
				double h = world.random.nextDouble() * H * 0.9;
				Vec3d a = center(h, age), b = center(Math.min(H, h + 40), age).add(world.random.nextGaussian() * radius * 0.5, 0, world.random.nextGaussian() * radius * 0.5);
				if (world.random.nextInt(3) == 0) {
					ClientFx.add(new Effects.Arc(a, b, 0.5, 0xE0F0FF, world.random.nextInt()));
				}
			}
			return super.tick(world);
		}

		private Vec3d center(double h, double a) {
			return base.add(0, h, 0);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = ArsenalEffects.envelope(a, lifetime, 30, 40);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			boolean far = cam.distanceTo(base) > 420;
			// Debris bands: chunks of the world orbiting at three heights.
			if (!far) {
				for (int band = 0; band < 3; band++) {
					double h = H * (0.12 + band * 0.22);
					double r = (5 + (radius - 5) * Math.pow(h / H, 1.4)) * 1.15;
					Vec3d c = center(h, a).subtract(cam);
					for (int i = 0; i < 14; i++) {
						double ang = i * MathHelper.TAU / 14 + a * (0.4 - band * 0.08) + Solids.hash(i, band) * 0.5;
						double hh = (Solids.hash(i, band + 7) - 0.5) * 14;
						Vec3d p = c.add(Math.cos(ang) * r, hh, Math.sin(ang) * r);
						double sz = 0.8 + Solids.hash(i, band + 3) * 2.2;
						Vec3d axis = new Vec3d(Math.cos(a * 0.2 + i), Math.sin(a * 0.15 + i * 2), Math.sin(a * 0.25 + i * 3)).normalize().multiply(sz * 0.5);
						int rock = i % 4 == 0 ? 0x3E6A2E : i % 3 == 0 ? 0x6A5038 : 0x707070;
						Solids.tube(solid, m, p.subtract(axis), p.add(axis), sz * 0.55, sz * 0.45, 5, rock, 1.0F, true);
					}
				}
			}
			Vec3d top = center(H, a).subtract(cam);
			// Lightning flickering inside the wall cloud.
			if (((int) (a * 0.7F) * 31 + (int) a) % 9 == 0) {
				double ang = Solids.hash((int) a, 5) * MathHelper.TAU;
				Vec3d p = top.add(Math.cos(ang) * radius * 1.5, -2, Math.sin(ang) * radius * 1.5);
				Solids.dot(glow, m, p, cb, radius * 1.2, 0xC8D8FF, 0.35F * env);
			}
		}
	}

	// ======================================================================== UFO

	public static class Ufo extends FxEffect {
		private final Vec3d from, to, dir;
		private final double radius;
		private Vec3d lastBeam = Vec3d.ZERO;

		public Ufo(Vec3d from, Vec3d to, double radius, int life) {
			super(Math.max(40, life));
			this.from = from;
			this.to = to;
			this.dir = new Vec3d(to.x - from.x, 0, to.z - from.z).normalize();
			this.radius = radius;
		}

		private boolean beamOn(double a) {
			return a > lifetime * 0.12 && a < lifetime * 0.88;
		}

		private Vec3d beamPoint(ClientWorld world, double a) {
			Vec3d ship = Paths.ufo(from, to, a, lifetime);
			Vec3d b = Paths.ufoBeam(new Vec3d(ship.x, 0, ship.z), dir, a);
			return new Vec3d(b.x, ground(world, b.x, b.z), b.z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d ship = Paths.ufo(from, to, age, lifetime);
			Vec3d cam = ClientFx.cameraPos();
			double d = Math.hypot(cam.x - ship.x, cam.z - ship.z);
			if (d < radius * 3) {
				ClientFx.shake = Math.max(ClientFx.shake, (float) (1 - d / (radius * 3)) * 3.0F);
				ClientFx.tintSky(0x0A1624, (float) Math.min(0.6, 1 - d / (radius * 3)));
			}
			if (age % 20 == 0) {
				ClientFx.playGlobal(ModSounds.DEATH_RAY, (float) MathHelper.clamp(1.0 - d / (radius * 8), 0.05, 0.9), 0.35F);
			}
			if (beamOn(age)) {
				lastBeam = beamPoint(world, age);
				if (age % 3 == 0) {
					ClientFx.light(lastBeam.add(0, 6, 0), 1.0F, 0x50B0FF);
				}
				double bd = Math.hypot(cam.x - lastBeam.x, cam.z - lastBeam.z);
				if (bd < 160) {
					ClientFx.shake = Math.max(ClientFx.shake, (float) (1 - bd / 160) * 4.0F);
				}
				// The beam's life is particles laid from the ground up to the hull: shimmer, rising motes, climbing rings, lifted dust.
				Vec3d up = ship.add(0, -radius * 0.2, 0).subtract(lastBeam);
				com.armageddon.client.vfx.FxEngine.play(com.armageddon.vfx.VfxIds.UFO_BEAM, lastBeam.x, lastBeam.y, lastBeam.z, (float) up.x,
						(float) up.y, (float) up.z, (float) (radius / 95.0), 0, age * 3343L);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d p = Paths.ufo(from, to, a, lifetime).subtract(cam);
			double R = radius;
			boolean far = p.length() > R * 12;
			int lat = far ? 10 : 16, lon = far ? 32 : 56;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			// Hull: banded armour panels with seams, a lit equator band, and darker vents.
			Solids.ellipsoid(solid, m, p, R, R * 0.16, R, lat, lon, (i, j, n) -> {
				if (Math.abs(i - lat / 2) <= 1) {
					return j % 2 == 0 ? 0xB8ECFF : 0x5A90C0;
				}
				boolean seam = j % 4 == 0 || i % 3 == 0;
				double tone = 0.9 + 0.22 * Solids.hash(i / 2, j / 3) + 0.12 * n.y;
				int base = n.y > 0 ? 0x6E7A92 : 0x4A5466;
				if (!seam && Solids.hash(i, j * 7) > 0.92) {
					return 0x2A2E38;
				}
				return Solids.scale(base, (seam ? 0.72 : 1.0) * tone);
			}, true, 1.0F);
			// Upper deck ring and lower drive housing.
			Solids.ellipsoid(solid, m, p.add(0, R * 0.09, 0), R * 0.62, R * 0.07, R * 0.62, 6, far ? 20 : 36, (i, j, n) -> Solids.scale(0x7C88A0, 0.9 + 0.2 * Solids.hash(i, j)), true, 1.0F);
			Solids.ellipsoid(solid, m, p.add(0, -R * 0.07, 0), R * 0.58, R * 0.13, R * 0.58, 8, 28, (i, j, n) -> Solids.scale(0x343C4C, 0.9 + 0.2 * Solids.hash(i, j)), true, 1.0F);
			// Glass dome with a pilot glow and a moving highlight.
			Solids.ellipsoid(shade, m, p.add(0, R * 0.1, 0), R * 0.36, R * 0.28, R * 0.36, 8, 24, (i, j, n) -> n.y > 0.6 ? 0xC8F4FF : 0x6AC8F0, false, 0.45F);
			Solids.ellipsoid(glow, m, p.add(0, R * 0.13, 0), R * 0.2, R * 0.13, R * 0.2, 6, 16, (i, j, n) -> 0x40FFB0, false, 0.5F);
			double hl = a * 0.02;
			Solids.dot(glow, m, p.add(Math.cos(hl) * R * 0.2, R * 0.33, Math.sin(hl) * R * 0.2), cb, R * 0.06, 0xFFFFFF, 0.7F);
			// Rim lights: a chasing wave of white over blue.
			int lights = far ? 24 : 48;
			for (int k = 0; k < lights; k++) {
				double ang = k * MathHelper.TAU / lights;
				double chase = (a * 0.6 - k) % lights;
				chase = chase < 0 ? chase + lights : chase;
				float wave = (float) Math.exp(-chase * 0.35);
				Vec3d lp = p.add(Math.cos(ang) * R * 0.97, -R * 0.015, Math.sin(ang) * R * 0.97);
				Solids.dot(glow, m, lp, cb, R * (0.025 + 0.03 * wave), Effects.fade(0x3080FF, 0xFFFFFF, wave), 0.55F + 0.45F * wave);
			}
			// Fresnel glow along the hull edge, and running lights on the upper deck.
			FxDraw.ring(glow, m, p, x, z, R * 0.96, R * 1.06, 0x60B8FF, 0.0F, 0.35F, 0.0F, 64);
			for (int k = 0; k < 6; k++) {
				double ang = k * MathHelper.TAU / 6 + a * 0.01;
				float on = MathHelper.sin(a * 0.3F + k * 1.2F) > 0.4F ? 1.0F : 0.15F;
				Solids.dot(glow, m, p.add(Math.cos(ang) * R * 0.6, R * 0.13, Math.sin(ang) * R * 0.6), cb, R * 0.04, k % 2 == 0 ? 0xFF4040 : 0x40FF80, on);
			}
			// Drive: counter-rotating rings of light under the hull and a soft lift glow.
			Vec3d under = p.add(0, -R * 0.2, 0);
			for (int k = 0; k < 3; k++) {
				double spin = a * (k % 2 == 0 ? 0.08 : -0.12);
				int dashes = 10;
				for (int i = 0; i < dashes; i++) {
					double a0 = spin + i * MathHelper.TAU / dashes, a1 = a0 + MathHelper.TAU / dashes * 0.6;
					arc(glow, m, under.add(0, k * 0.4, 0), a0, a1, R * (0.18 + k * 0.12), R * (0.22 + k * 0.12), k == 1 ? 0xFFFFFF : 0x40A0FF, 0.8F);
				}
			}
			Solids.dot(glow, m, under, cb, R * 0.35, 0x40A0FF, 0.45F);
			FxKit.halo(consumers, m, cb, under, R * 1.2, 0x2070E0, 0.25F);
			FxDraw.disc(glow, m, under.add(0, -1, 0), x, z, R * 0.7, 0x2060C0, 0.25F, 0.0F, 32);
			if (beamOn(a) && lastBeam != Vec3d.ZERO) {
				Vec3d target = beamPoint(com.armageddon.client.render.FxDraw.world(), a).subtract(cam);
				double ramp = Math.min(1.0, Math.min((a - lifetime * 0.12) / 14.0, (lifetime * 0.88 - a) / 14.0));
				float rmp = (float) ramp;
				// Volumetric tractor beam: nested cones from wide and faint to a white-hot core.
				Solids.tube(glow, m, under, target, R * 0.12, R * 0.34, 18, 0x1A60D0, 0.12F * rmp, false);
				Solids.tube(glow, m, under, target, R * 0.08, R * 0.2, 14, 0x2080FF, 0.22F * rmp, false);
				Solids.tube(glow, m, under, target, R * 0.03, R * 0.08, 10, 0x80C8FF, 0.5F * rmp, false);
				FxDraw.beam(glow, m, under, target, Vec3d.ZERO, R * 0.05, 0xDFF4FF, rmp);
				// The rings, motes and lifted ground in the beam are particles (UFO_BEAM, played in tick).
				FxDraw.disc(glow, m, target.add(0, 0.8, 0), x, z, R * 0.3, 0xBFE8FF, 0.8F * rmp, 0.0F, 32);
				FxDraw.disc(glow, m, target.add(0, 0.6, 0), x, z, R * 0.55, 0xFF7A30, 0.3F * rmp, 0.0F, 32);
				Solids.dot(glow, m, target.add(0, 3, 0), cb, R * 0.25, 0xE0F4FF, 0.6F * rmp);
				FxKit.fire(consumers, m, cb, target.add(0, 2, 0), R * 0.18, 0xFFB070, (float) ((a * 0.05) % 1.0) * 0.6F, 0.8F * rmp, a, 81);
				FxKit.groundShock(consumers, m, target, R * 0.2 + ((a * 2) % 30), 0x80C8FF, 0.6F * rmp);
				java.util.List<Effects.Puff> burn = FxKit.puffs();
				FxKit.column(target.add(0, 2, 0), R * 0.9, R * 0.16, a, 0x6A6460, 0x24201E, 0.5F * rmp, 82, burn);
				FxKit.flush(burn, consumers, m, cb);
			}
		}
	}

	/** Flat ring segment in the horizontal plane. */
	static void arc(VertexConsumer vc, Matrix4f m, Vec3d c, double a0, double a1, double r0, double r1, int rgb, float alpha) {
		int n = 3;
		for (int i = 0; i < n; i++) {
			double t0 = a0 + (a1 - a0) * i / n, t1 = a0 + (a1 - a0) * (i + 1) / n;
			FxDraw.vertex(vc, m, c.x + Math.cos(t0) * r0, c.y, c.z + Math.sin(t0) * r0, rgb, alpha);
			FxDraw.vertex(vc, m, c.x + Math.cos(t1) * r0, c.y, c.z + Math.sin(t1) * r0, rgb, alpha);
			FxDraw.vertex(vc, m, c.x + Math.cos(t1) * r1, c.y, c.z + Math.sin(t1) * r1, rgb, alpha);
			FxDraw.vertex(vc, m, c.x + Math.cos(t0) * r1, c.y, c.z + Math.sin(t0) * r1, rgb, alpha);
		}
	}

	// ======================================================================== SPEAR

	public static class Spear extends FxEffect {
		/** Matches CelestialIIEvents.ColossalSpear.THROW: about five seconds of approach, visible across the sky. */
		private static final int THROW = 120;
		private final Vec3d target;
		private final double length, radius, depth;
		private final Vec3d throwDir = new Vec3d(0.32, -1, 0.2).normalize();

		public Spear(Vec3d target, double length, double depth, int life) {
			super(Math.max(120, life));
			this.target = target;
			this.length = length;
			this.radius = length * 0.034;
			this.depth = depth;
		}

		private double spin(double a) {
			double t = Math.max(0, a - THROW);
			return Math.pow(t, 1.4) * 0.045;
		}

		private Vec3d axis(double a) {
			double k = ease((a - THROW) / 24.0);
			return throwDir.lerp(new Vec3d(0, -1, 0), k).normalize();
		}

		private static double ease(double t) {
			t = MathHelper.clamp(t, 0.0, 1.0);
			return t * t * (3 - 2 * t);
		}

		private Vec3d tip(double a) {
			if (a < THROW) {
				double u = a / THROW;
				return target.subtract(throwDir.multiply(720 * (1 - Math.pow(u, 1.7))));
			}
			// Matches the server drill: 0.6 blocks per tick from 24 ticks after the throw lands.
			double sink = Math.min(depth, Math.max(0, a - THROW - 24) * 0.6);
			return target.add(0, -sink, 0);
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d cam = ClientFx.cameraPos();
			double d = cam.distanceTo(target);
			if (age == THROW) {
				ClientFx.flash(target, 500, 0.8F, 0xFFE0B0);
				ClientFx.shake = 9.0F;
				ClientFx.add(new Effects.Ring(target, 160, 0xFFD090, 40, 6, true));
				ClientFx.add(new InsaneEffects.Pillar(target, 10, 50, 0xFFB060, false));
			}
			if (age >= THROW) {
				float k = (float) MathHelper.clamp(1 - d / 380, 0, 1);
				ClientFx.shake = Math.max(ClientFx.shake, k * (2.0F + (float) Math.min(4, (age - THROW) * 0.03)));
				double sp = spin(age);
				for (int i = 0; i < 16; i++) {
					double ang = sp * 3 + i * MathHelper.TAU / 16;
					double r = radius * (2.4 + world.random.nextDouble() * 2);
					world.addImportantParticle(ModParticles.SHOCK_DUST, true, target.x + Math.cos(ang) * r, target.y + 1, target.z + Math.sin(ang) * r,
							-Math.sin(ang) * 2.5, 1.2 + world.random.nextDouble(), Math.cos(ang) * 2.5);
					if (i % 4 == 0) {
						world.addImportantParticle(ModParticles.EMBER, true, target.x + Math.cos(ang) * r, target.y + 1, target.z + Math.sin(ang) * r,
								Math.cos(ang) * 0.5, 1.5, Math.sin(ang) * 0.5);
					}
				}
				if (age % 20 == 0) {
					ClientFx.playGlobal(ModSounds.NUKE_RUMBLE, (float) MathHelper.clamp(1 - d / 500, 0.05, 1.0), 0.4F);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d ax = axis(a);
			Vec3d tip = tip(a).subtract(cam);
			Vec3d back = tip.subtract(ax.multiply(length));
			double headLen = length * 0.24;
			Vec3d headBase = tip.subtract(ax.multiply(headLen));
			Vec3d[] bs = ArsenalEffects.basis(ax);
			double sp = spin(a);
			// Shaft: slender polished ivory, ringed with gold bands, a leather-wrapped grip near the back.
			int ivory = 0xE6DCC4, gold = 0xD8A838, darkGold = 0x8A6418;
			Solids.tube(solid, m, back, headBase, radius * 0.82, radius * 0.9, 16, ivory, 1.0F, true);
			for (int k = 0; k < 7; k++) {
				Vec3d p0 = back.add(ax.multiply(length * (0.1 + 0.11 * k)));
				double w = k % 3 == 0 ? 0.03 : 0.012;
				Solids.tube(solid, m, p0, p0.add(ax.multiply(length * w)), radius * 0.98, radius * 0.98, 16, k % 3 == 0 ? gold : darkGold, 1.0F, true);
			}
			Vec3d gripA = back.add(ax.multiply(length * 0.06)), gripB = back.add(ax.multiply(length * 0.3));
			Solids.tube(solid, m, gripA, gripB, radius * 0.88, radius * 0.88, 16, 0x2A2440, 1.0F, true);
			Vec3d wrapPrev = null;
			for (int s2 = 0; s2 <= 60; s2++) {
				double u = s2 / 60.0, ang = u * 30 + sp;
				Vec3d p = gripA.lerp(gripB, u).add(bs[0].multiply(Math.cos(ang) * radius * 0.92)).add(bs[1].multiply(Math.sin(ang) * radius * 0.92));
				if (wrapPrev != null) {
					Solids.tube(solid, m, wrapPrev, p, radius * 0.12, radius * 0.12, 5, 0x6A4A90, 1.0F, true);
				}
				wrapPrev = p;
			}
			// Butt: a gold orb in a ring collar with a short spike.
			Vec3d orb = back.subtract(ax.multiply(radius * 1.1));
			Solids.tube(solid, m, back.subtract(ax.multiply(radius * 0.3)), back.add(ax.multiply(radius * 0.4)), radius * 1.25, radius * 1.25, 16, gold, 1.0F, true);
			Solids.ellipsoid(solid, m, orb, radius * 1.15, radius * 1.15, radius * 1.15, 12, 20, (i, j, n) -> Solids.mix(gold, 0xFFE8A0, Math.max(0, n.y) * 0.5), true, 1.0F);
			Solids.tube(solid, m, orb.subtract(ax.multiply(radius * 0.9)), orb.subtract(ax.multiply(radius * 3.2)), radius * 0.55, 0.0, 12, darkGold, 1.0F, true);
			// Socket and crossguard: a fluted gold socket, a glowing gem, and two crescent guards sweeping back.
			Vec3d socketA = headBase.subtract(ax.multiply(length * 0.05));
			Solids.tube(solid, m, socketA, headBase, radius * 1.0, radius * 1.45, 16, gold, 1.0F, true);
			Vec3d wd = bs[0].multiply(Math.cos(sp * 0.0)).add(bs[1].multiply(Math.sin(sp * 0.0)));
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Vec3d prev = headBase;
				for (int k = 1; k <= 10; k++) {
					double u = k / 10.0;
					Vec3d p = headBase.add(wd.multiply(sgn * radius * (1.2 + 3.4 * Math.sin(u * Math.PI * 0.6)))).subtract(ax.multiply(length * 0.07 * u * u));
					double rr = radius * 0.5 * (1 - u * 0.85) + 0.15;
					Solids.tube(solid, m, prev, p, rr + radius * 0.06, rr, 8, k % 4 == 0 ? 0xFFE08A : gold, 1.0F, true);
					prev = p;
				}
				Solids.flare(glow, m, prev, cb, radius * 0.9, 0xFFE8A0, 0.8F);
			}
			Vec3d gem = headBase.add(bs[1].multiply(radius * 1.35));
			Solids.ellipsoid(solid, m, gem, radius * 0.45, radius * 0.45, radius * 0.45, 8, 12, (i, j, n) -> Solids.mix(0x2060FF, 0xA0E0FF, Math.max(0, n.y)), false, 1.0F);
			Solids.flare(glow, m, gem, cb, radius * 1.6, 0x80C8FF, 0.9F);
			// The blade: a leaf-shaped, double-edged blade lofted as a flat diamond, with a glowing fuller and edges.
			int rows = 18;
			Vec3d wdir = wd, tdir = ax.crossProduct(wdir).normalize();
			Vec3d[] prevQ = null;
			for (int k = 0; k <= rows; k++) {
				double u = k / (double) rows;
				double half = radius * (1.4 + 1.9 * Math.sin(Math.min(1.0, u / 0.72) * Math.PI * 0.62)) * (u > 0.72 ? (1 - u) / 0.28 : 1.0);
				double thick = radius * 0.42 * (1 - u * 0.8) + 0.05;
				Vec3d c0 = headBase.add(ax.multiply(headLen * u));
				Vec3d[] q = {c0.add(wdir.multiply(half)), c0.add(tdir.multiply(thick)), c0.subtract(wdir.multiply(half)), c0.subtract(tdir.multiply(thick))};
				if (prevQ != null) {
					for (int f = 0; f < 4; f++) {
						int g = (f + 1) % 4;
						boolean lit = f % 2 == 0;
						int col = Solids.mix(0xC8D0DC, 0xF4F8FF, lit ? 0.8 : 0.25);
						Solids.quad(solid, m, prevQ[f], prevQ[g], q[g], q[f], col, 1.0F);
					}
					FxDraw.ribbon(glow, m, prevQ[0], q[0], Vec3d.ZERO, radius * 0.12, 0xFFFFFF, 0.8F, 0.8F);
					FxDraw.ribbon(glow, m, prevQ[2], q[2], Vec3d.ZERO, radius * 0.12, 0xFFFFFF, 0.8F, 0.8F);
				}
				prevQ = q;
			}
			FxDraw.ribbon(glow, m, headBase.add(tdir.multiply(radius * 0.44)), tip, Vec3d.ZERO, radius * 0.35, 0xFFC870, 0.75F, 0.2F);
			FxDraw.ribbon(glow, m, headBase.subtract(tdir.multiply(radius * 0.44)), tip, Vec3d.ZERO, radius * 0.35, 0xFFC870, 0.75F, 0.2F);
			Solids.dot(glow, m, tip, cb, radius * 1.6, 0xFFE8C0, 0.6F);
			// Rune light racing along the shaft in two thin spirals.
			for (int k = 0; k < 2; k++) {
				Vec3d prev = null;
				for (int s2 = 0; s2 <= 40; s2++) {
					double u = s2 / 40.0;
					double ang = sp + k * Math.PI + u * 10;
					Vec3d p = back.add(ax.multiply(length * (0.32 + 0.48 * u))).add(bs[0].multiply(Math.cos(ang) * radius * 0.95)).add(bs[1].multiply(Math.sin(ang) * radius * 0.95));
					if (prev != null) {
						float pulse = 0.5F + 0.5F * MathHelper.sin((float) (u * 20 - a * 0.4));
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, radius * 0.08, 0xFFD890, 0.8F * pulse, 0.8F * pulse);
					}
					prev = p;
				}
			}
			// Angelic wings: two fans of glowing feathers spread from the back of the shaft, beating slowly.
			Vec3d wingSide = ax.crossProduct(new Vec3d(0, 1, 0));
			wingSide = wingSide.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : wingSide.normalize();
			Vec3d wingRoot = back.add(ax.multiply(length * 0.22));
			double beat = Math.sin(a * 0.12) * 0.18;
			for (int side = -1; side <= 1; side += 2) {
				for (int f = 0; f < 8; f++) {
					double th = 0.15 + f * 0.17 + beat;
					Vec3d dir = wingSide.multiply(side * Math.cos(th)).add(ax.multiply(-Math.sin(th))).normalize();
					double fl = length * (0.42 - f * 0.025);
					Vec3d tipF = wingRoot.add(dir.multiply(fl));
					// Soft feathers of light rather than hard strokes.
					com.armageddon.client.render.FxSprites.streak(FxKit.glow(consumers), m, wingRoot.add(dir.multiply(fl * 0.1)), tipF, radius * (2.6 - f * 0.15),
							com.armageddon.client.render.FxSprites.STREAK, f % 2 == 0 ? 0xFFF6E0 : 0xFFE0A0, 0.45F, com.armageddon.client.render.FxSprites.FULL_BRIGHT);
				}
			}
			Vec3d haloC = back.subtract(ax.multiply(radius * 3));
			FxDraw.ring(glow, m, haloC, bs[0], bs[1], radius * 3.0, radius * 3.6, 0xFFE8A0, 0.0F, 0.85F, 0.0F, 32);
			if (a < THROW) {
				// Heaven opens over the target: a column of light marks where it will land.
				float open = (float) Math.min(1.0, a / 30.0);
				Vec3d gT = target.subtract(cam);
				FxDraw.beam(glow, m, gT, gT.add(0, 500, 0), Vec3d.ZERO, 10, 0xFFF0C0, 0.35F * open);
				Vec3d x0 = new Vec3d(1, 0, 0), z0 = new Vec3d(0, 0, 1);
				FxDraw.ring(glow, m, gT.add(0, 220, 0), x0, z0, 50 + a * 0.3, 62 + a * 0.3, 0xFFF4D0, 0.0F, 0.5F * open, 0.0F, 64);
				FxDraw.disc(glow, m, gT.add(0, 0.5, 0), x0, z0, 30 * (0.4 + 0.6 * a / THROW), 0xFFF0C0, 0.4F * open, 0.0F, 32);
				for (int k = 0; k < 10; k++) {
					FxKit.halo(consumers, m, cb, gT.add(0, 20 + k * 45, 0), 26 + k * 3, 0xFFF0C0, 0.14F * open);
				}
				FxKit.flare(consumers, m, cb, gT.add(0, 220, 0), 70, 0xFFF4D0, 0.6F * open, a * 0.003);
				// In flight: a long shock trail and a glowing bow wave at the tip.
				Vec3d tail = back.subtract(throwDir.multiply(length * 2.5));
				FxDraw.beam(glow, m, back, tail, Vec3d.ZERO, radius * 1.4, 0xFFC070, 0.5F);
				FxDraw.ribbon(glow, m, tip, back, Vec3d.ZERO, radius * 4, 0xFFE0B0, 0.3F, 0.0F);
				for (int k = 1; k <= 3; k++) {
					Vec3d c = tip.subtract(throwDir.multiply(length * 0.25 * k));
					FxDraw.ring(glow, m, c, bs[0], bs[1], radius * (2 + k), radius * (2.4 + k), 0xFFFFFF, 0.0F, 0.4F / k, 0.0F, 24);
				}
			}
			if (a >= THROW) {
				double fxR = length * 0.06;
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				Vec3d g = target.subtract(cam).add(0, 0.8, 0);
				float ramp = (float) Math.min(1.0, (a - THROW) / 20.0);
				// Molten collar where the spear bores in, sparks thrown off in a spiral, and a dust ring.
				FxDraw.disc(glow, m, g, x, z, fxR * 4.5, 0xFF8A20, 0.5F * ramp, 0.0F, 32);
				FxDraw.disc(glow, m, g.add(0, 0.1, 0), x, z, fxR * 2.2, 0xFFE0A0, 0.7F * ramp, 0.0F, 24);
				for (int k = 0; k < 3; k++) {
					double rr = fxR * (2.4 + ((a * 0.6 + k * 12) % 30) * 0.3);
					FxDraw.ring(glow, m, g, x, z, rr * 0.9, rr, 0xFFD090, 0.0F, 0.6F * ramp, 0.0F, 40);
				}
				for (int i = 0; i < 18; i++) {
					double t = ((a * 0.05 + i / 18.0) % 1.0);
					double ang = sp * 2 + i * 2.4;
					double rr = fxR * (1.6 + t * 6);
					Vec3d p0 = g.add(Math.cos(ang) * rr, t * fxR * 5 - t * t * fxR * 4, Math.sin(ang) * rr);
					Vec3d p1 = g.add(Math.cos(ang - 0.25) * rr * 0.92, (t - 0.04) * fxR * 5, Math.sin(ang - 0.25) * rr * 0.92);
					FxDraw.ribbon(glow, m, p1, p0, Vec3d.ZERO, fxR * 0.12, i % 3 == 0 ? 0xFFFFFF : 0xFFA040, 0.0F, 0.9F * (float) (1 - t) * ramp);
				}
				double e = a - THROW;
				// The landing: a fireball of gold, a spark shower and two textured shock fronts racing out.
				if (e < 40) {
					float k = (float) (e / 40.0);
					FxKit.fire(consumers, m, cb, g.add(0, fxR * 3, 0), fxR * 9, 0xFFE0A0, k, 1.0F, a, 91);
					FxKit.sparks(consumers, m, g.add(0, 2, 0), fxR * 40, 90, 0xFFC070, k, 1.0F, 92);
					FxKit.groundShock(consumers, m, g, fxR * 4 + e * fxR * 3.2, 0xFFE0B0, 1 - k);
					FxKit.groundShock(consumers, m, g, fxR * 2 + e * fxR * 1.8, 0xFFFFFF, 0.8F * (1 - k));
				}
				// Dust rolling out from the bore, and smoke climbing off the molten collar while it drills.
				java.util.List<Effects.Puff> dust = FxKit.puffs();
				FxKit.dustRing(m, cb, g, fxR * 6 + Math.min(e, 60) * fxR * 0.25, fxR * 2.2, 0xA89478, 0x4E4232, 0.55F * ramp, 93, dust);
				FxKit.column(g.add(0, fxR, 0), fxR * 22, fxR * 4, a, 0x8A7E70, 0x2C2420, 0.45F * ramp, 94, dust);
				FxKit.flush(dust, consumers, m, cb);
			}
		}
	}

	// ======================================================================== ALIEN ROCKET

	public static class AlienRocket extends FxEffect {
		private final Vec3d to, from, dir;
		private final double len;

		public AlienRocket(Vec3d to, Vec3d from, double len, int life) {
			super(Math.max(40, life));
			this.to = to;
			this.from = from;
			this.dir = to.subtract(from).normalize();
			this.len = len;
		}

		private Vec3d pos(double a) {
			double u = Math.pow(MathHelper.clamp(a / lifetime, 0.0, 1.0), 2.4);
			return from.lerp(to, u);
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d p = pos(age);
			Vec3d cam = ClientFx.cameraPos();
			double d = cam.distanceTo(p);
			ClientFx.tintSky(0x20083A, Math.min(0.8F, age / (float) lifetime + 0.2F));
			if (d < 700) {
				ClientFx.shake = Math.max(ClientFx.shake, (float) (1 - d / 700) * 5.0F);
			}
			if (age % 4 == 0) {
				ClientFx.light(p, 1.0F, 0xA070FF);
			}
			if (age % 10 == 0) {
				ClientFx.playGlobal(ModSounds.ANTIMATTER, 0.8F, 0.3F);
			}
			for (int i = 0; i < 6; i++) {
				Vec3d tp = p.subtract(dir.multiply(len * (0.5 + world.random.nextDouble() * 2)));
				world.addImportantParticle(ModParticles.PLASMA_SPARK, true, tp.x, tp.y, tp.z, world.random.nextGaussian() * 0.5, world.random.nextGaussian() * 0.5, world.random.nextGaussian() * 0.5);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d p = pos(a).subtract(cam);
			float t = Math.min(1.0F, a / lifetime);
			Solids.rocket(solid, glow, m, p, dir, len, len * 0.095, a, 1.4);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d[] bs = ArsenalEffects.basis(dir);
			Solids.dot(glow, m, p, cb, len * 1.6, 0xA070FF, 0.18F);
			Solids.dot(glow, m, p, cb, len * 0.7, 0xD8B0FF, 0.25F);
			// Plasma bow shock wrapping the nose; it gets hotter as the rocket speeds up.
			Vec3d nose = p.add(dir.multiply(len * 0.5));
			for (int k = 0; k < 3; k++) {
				Vec3d c = nose.subtract(dir.multiply(len * 0.1 * k));
				double rr = len * (0.14 + 0.09 * k) * (1 + t);
				FxDraw.ring(glow, m, c, bs[0], bs[1], rr * 0.7, rr, k == 0 ? 0xFFFFFF : 0xC090FF, 0.0F, (0.6F - k * 0.15F) * (0.5F + t), 0.0F, 32);
			}
			Solids.tube(glow, m, nose.add(dir.multiply(len * 0.05)), nose.subtract(dir.multiply(len * 0.6)), 0.0, len * 0.3 * (1 + t), 14, 0xE0C8FF, 0.18F + 0.2F * t, false);
			// Braided tail: three twisting strands around a long comet core.
			Vec3d tail = p.subtract(dir.multiply(len * 0.5));
			FxDraw.beam(glow, m, tail, tail.subtract(dir.multiply(len * 9)), Vec3d.ZERO, len * 0.12, 0x80F0FF, 0.6F);
			FxDraw.beam(glow, m, tail, tail.subtract(dir.multiply(len * 5)), Vec3d.ZERO, len * 0.22, 0xA070FF, 0.4F);
			for (int k = 0; k < 3; k++) {
				Vec3d prev = null;
				for (int s = 0; s <= 24; s++) {
					double u = s / 24.0;
					double ang = k * MathHelper.TAU / 3 + u * 10 - a * 0.3;
					double rr = len * 0.12 * (0.4 + u);
					Vec3d q = tail.subtract(dir.multiply(len * 6 * u)).add(bs[0].multiply(Math.cos(ang) * rr)).add(bs[1].multiply(Math.sin(ang) * rr));
					if (prev != null) {
						float al = 0.7F * (float) (1 - u);
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, len * 0.04 * (1.2 - u), k == 0 ? 0xFFFFFF : k == 1 ? 0x60FFC0 : 0xC070FF, al, al);
					}
					prev = q;
				}
			}
			// Heat-shimmer rings shed behind it.
			for (int k = 0; k < 4; k++) {
				double u = ((a * 0.04 + k / 4.0) % 1.0);
				Vec3d c = tail.subtract(dir.multiply(len * 4 * u));
				double rr = len * (0.2 + u * 0.8);
				FxDraw.ring(glow, m, c, bs[0], bs[1], rr * 0.9, rr, 0xB080FF, 0.0F, 0.35F * (float) (1 - u), 0.0F, 32);
			}
		}
	}
}
