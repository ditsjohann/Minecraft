package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Paths;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
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
			return new Vec3d(p.x, ground(world, p.x, p.z), p.z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			base = at(world, age);
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
			for (int i = 0; i < 14; i++) {
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double h = world.random.nextDouble() * H;
				double r = radius * (0.2 + 0.8 * Math.pow(h / H, 1.2)) * 1.1;
				Vec3d c = center(h, age);
				world.addImportantParticle(i % 3 == 0 ? ModParticles.SHOCK_DUST : ParticleTypes.CLOUD, true,
						c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r, -Math.sin(ang) * 1.6, 0.5, Math.cos(ang) * 1.6);
			}
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
			return base.add(Math.sin(h * 0.045 + a * 0.07) * h * 0.10, h, Math.cos(h * 0.04 + a * 0.05) * h * 0.10);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = ArsenalEffects.envelope(a, lifetime, 30, 40);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			boolean far = cam.distanceTo(base) > 420;
			int layers = far ? 18 : 40;
			int strands = far ? 6 : 12;
			Vec3d[] prev = new Vec3d[strands];
			Vec3d[] prevIn = new Vec3d[strands];
			// The funnel wall: billows of dust and cloud spiralling up it, drawn back to front as real smoke.
			java.util.List<Effects.Puff> funnel = FxKit.puffs();
			for (int k = 0; k <= layers; k++) {
				double h = H * k / layers;
				float hf = (float) (h / H);
				double r = 5 + (radius - 5) * Math.pow(hf, 1.4);
				Vec3d c = center(h, a).subtract(cam);
				int lit = hf < 0.25F ? Effects.fade(0x8A7660, 0x6E747C, hf / 0.25F) : Effects.fade(0x6E747C, 0xC4CCD4, (hf - 0.25F) / 0.75F);
				int per = far ? 5 : 8;
				for (int s = 0; s < per; s++) {
					double ang = s * MathHelper.TAU / per + a * (0.3 + h * 0.0015) + h * 0.05 + Solids.hash(k, s) * 0.5;
					Vec3d p = c.add(Math.cos(ang) * r * 0.82, (Solids.hash(k, s + 9) - 0.5) * H / layers, Math.sin(ang) * r * 0.82);
					funnel.add(new Effects.Puff(p, r * 0.55 + 2, lit, Solids.scale(lit, 0.55), 0.55F * env));
				}
			}
			FxKit.flush(funnel, consumers, m, cb);
			for (int k = 0; k <= layers; k++) {
				double h = H * k / layers;
				float hf = (float) (h / H);
				double r = 5 + (radius - 5) * Math.pow(hf, 1.4);
				Vec3d c = center(h, a).subtract(cam);
				// Dusty brown at the ground, slate in the middle, pale where it meets the storm.
				int col = hf < 0.25F ? Effects.fade(0x5A4A3A, 0x464C54, hf / 0.25F) : Effects.fade(0x464C54, 0xA8B2BC, (hf - 0.25F) / 0.75F);
				FxDraw.ring(shade, m, c, x, z, r * 0.2, r * 0.6, Solids.scale(col, 0.6), 0.0F, 0.2F * env, 0.0F, far ? 12 : 20);
				for (int s = 0; s < strands; s++) {
					double spin = a * (0.32 + h * 0.0016) + h * 0.05;
					double ang = s * MathHelper.TAU / strands + spin;
					Vec3d p = c.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
					Vec3d q = c.add(Math.cos(ang + 0.9) * r * 0.62, 0, Math.sin(ang + 0.9) * r * 0.62);
					if (prev[s] != null) {
						int sc = Effects.fade(0x7A848E, 0xE4EAF0, hf);
						FxDraw.ribbon(shade, m, prev[s], p, Vec3d.ZERO, r * 0.12, sc, 0.3F * env, 0.3F * env);
						FxDraw.ribbon(shade, m, prevIn[s], q, Vec3d.ZERO, r * 0.08, Solids.scale(sc, 0.7), 0.22F * env, 0.22F * env);
					}
					prev[s] = p;
					prevIn[s] = q;
				}
				if (k % 5 == 0) {
					FxDraw.ring(glow, m, c, x, z, r * 0.95, r * 1.03, 0xB8C8FF, 0.0F, 0.1F * env, 0.0F, 28);
				}
			}
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
			// Dust skirt rolling out around the base.
			Vec3d g = base.subtract(cam);
			java.util.List<Effects.Puff> skirt = FxKit.puffs();
			FxKit.dustRing(m, cb, g, radius * 1.05 + Math.sin(a * 0.1) * 2, radius * 0.5, 0x9A8668, 0x4E4232, 0.6F * env, 3, skirt);
			FxKit.dustRing(m, cb, g, radius * 1.6, radius * 0.35, 0x8A7860, 0x4A4030, 0.35F * env, 4, skirt);
			// Wall cloud: a dark rotating shelf of billows with spiral bands feeding the funnel.
			Vec3d top = center(H, a).subtract(cam);
			for (int i = 0; i < (far ? 14 : 26); i++) {
				double ang = i * 2.39996 + a * 0.02;
				double rr = radius * 3.0 * Math.sqrt((i + 0.5) / (far ? 14 : 26));
				skirt.add(new Effects.Puff(top.add(Math.cos(ang) * rr, (Solids.hash(i, 71) - 0.5) * 6, Math.sin(ang) * rr), radius * 0.9,
						0x6A7078, 0x26292E, 0.6F * env));
			}
			FxKit.flush(skirt, consumers, m, cb);
			for (int arm = 0; arm < 4; arm++) {
				Vec3d p0 = null;
				for (int s2 = 0; s2 <= 16; s2++) {
					double t = s2 / 16.0;
					double ang = arm * MathHelper.TAU / 4 + t * 2.4 + a * 0.05;
					double rr = radius * (0.4 + 2.6 * t);
					Vec3d p = top.add(Math.cos(ang) * rr, 2 - t * 4, Math.sin(ang) * rr);
					if (p0 != null) {
						FxDraw.ribbon(shade, m, p0, p, new Vec3d(0, 400, 0), radius * 0.5 * (1 - t * 0.5), 0x5A6068, 0.4F * env * (float) (1 - t), 0.4F * env * (float) (1 - t));
					}
					p0 = p;
				}
			}
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
				for (int i = 0; i < 12; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double r = world.random.nextDouble() * radius * 0.3;
					world.addImportantParticle(ModParticles.PLASMA_SPARK, true, lastBeam.x + Math.cos(ang) * r, lastBeam.y + 0.5, lastBeam.z + Math.sin(ang) * r,
							Math.cos(ang) * 0.6, 0.8 + world.random.nextDouble(), Math.sin(ang) * 0.6);
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
				// Rings sliding down the beam, and motes rising up it.
				Vec3d axis = target.subtract(under);
				Vec3d[] bb = ArsenalEffects.basis(axis);
				for (int k = 0; k < 6; k++) {
					double t = ((a * 0.025 + k / 6.0) % 1.0);
					Vec3d c = under.add(axis.multiply(t));
					double rr = MathHelper.lerp(t, R * 0.12, R * 0.34);
					FxDraw.ring(glow, m, c, bb[0], bb[1], rr * 0.8, rr, 0xA0D8FF, 0.0F, 0.5F * rmp * (float) Math.sin(t * Math.PI), 0.0F, 32);
				}
				for (int k = 0; k < 14; k++) {
					double t = 1 - ((a * 0.015 + k / 14.0) % 1.0);
					double ang = k * 2.4 + a * 0.1;
					double rr = MathHelper.lerp(t, R * 0.1, R * 0.3) * (0.3 + 0.7 * Solids.hash(k, 3));
					Vec3d c = under.add(axis.multiply(t)).add(bb[0].multiply(Math.cos(ang) * rr)).add(bb[1].multiply(Math.sin(ang) * rr));
					Solids.dot(glow, m, c, cb, R * 0.02, 0xFFFFFF, 0.8F * rmp);
				}
				Vec3d groundC = target.add(0, 0.7, 0);
				for (int k = 0; k < 3; k++) {
					double rr = ((a * 3 + k * 20) % 60) / 60.0 * R * 0.35 + 2;
					FxDraw.ring(glow, m, groundC, x, z, rr * 0.85, rr, 0x80C8FF, 0.0F, 0.8F * rmp, 0.0F, 40);
				}
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
			this.radius = length * 0.06;
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
			double headLen = length * 0.17;
			Vec3d headBase = tip.subtract(ax.multiply(headLen));
			Vec3d[] bs = ArsenalEffects.basis(ax);
			double sp = spin(a);
			// Shaft: dark wood-like core wrapped in alternating metal bands.
			Solids.tube(solid, m, back, headBase, radius, radius * 1.05, 12, 0x6E5A3A, 1.0F, true);
			for (int k = 0; k < 9; k++) {
				Vec3d p0 = back.add(ax.multiply(length * (0.06 + 0.085 * k)));
				Solids.tube(solid, m, p0, p0.add(ax.multiply(length * 0.025)), radius * 1.12, radius * 1.12, 12, k % 3 == 0 ? 0xC8A050 : 0x3A3A44, 1.0F, true);
			}
			// Head: faceted blade with a bright cutting edge, collar and pommel.
			Solids.tube(solid, m, headBase, tip, radius * 1.45, 0.0, 6, 0xD4D8E4, 1.0F, true);
			Solids.tube(solid, m, headBase.subtract(ax.multiply(radius)), headBase, radius * 1.9, radius * 1.45, 12, 0x2A2A34, 1.0F, true);
			Solids.tube(solid, m, back.subtract(ax.multiply(radius * 1.5)), back, radius * 0.4, radius * 1.3, 8, 0x8A6A2A, 1.0F, true);
			for (int k = 0; k < 6; k++) {
				double ang = sp + k * MathHelper.TAU / 6;
				Vec3d edge = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
				FxDraw.ribbon(glow, m, headBase.add(edge.multiply(radius * 1.45)), tip, Vec3d.ZERO, radius * 0.1, 0xFFFFFF, 0.55F, 0.9F);
			}
			Solids.tube(glow, m, headBase, tip, radius * 1.6, 0.0, 12, 0xFFB050, 0.25F, false);
			Solids.dot(glow, m, tip, cb, radius * 1.6, 0xFFE8C0, 0.6F);
			// Rune spirals racing along the shaft.
			for (int k = 0; k < 4; k++) {
				Vec3d prev = null;
				for (int s = 0; s <= 28; s++) {
					double u = s / 28.0;
					double ang = sp + k * MathHelper.TAU / 4 + u * 14;
					Vec3d p = back.add(ax.multiply(length * 0.82 * u)).add(bs[0].multiply(Math.cos(ang) * radius * 1.14)).add(bs[1].multiply(Math.sin(ang) * radius * 1.14));
					if (prev != null) {
						float pulse = 0.6F + 0.4F * MathHelper.sin((float) (u * 20 - a * 0.4));
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, radius * 0.16, k % 2 == 0 ? 0xFFB050 : 0xFFFFFF, 0.9F * pulse, 0.9F * pulse);
					}
					prev = p;
				}
			}
			for (int k = 0; k < 6; k++) {
				Vec3d p0 = back.add(ax.multiply(length * (0.12 + 0.14 * k)));
				FxDraw.ring(glow, m, p0, bs[0], bs[1], radius * 1.15, radius * 1.4, 0xFFB050, 0.0F, 0.7F, 0.0F, 24);
			}
			for (int f = 0; f < 3; f++) {
				double ang = sp * 0.5 + f * MathHelper.TAU / 3;
				Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
				Vec3d root = back.add(ax.multiply(length * 0.12));
				Vec3d end = back.add(out.multiply(radius * 3.2)).subtract(ax.multiply(length * 0.04));
				Solids.tube(solid, m, root, end, radius * 0.5, radius * 0.08, 4, 0xB03A2A, 1.0F, true);
				FxDraw.ribbon(glow, m, root, end, Vec3d.ZERO, radius * 0.08, 0xFF6040, 0.6F, 0.0F);
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
					FxDraw.ribbon(glow, m, wingRoot, tipF, Vec3d.ZERO, radius * (1.3 - f * 0.08), f % 2 == 0 ? 0xFFF6E0 : 0xFFE0A0, 0.55F, 0.15F);
					FxDraw.ribbon(glow, m, wingRoot, tipF, Vec3d.ZERO, radius * 0.25, 0xFFFFFF, 0.8F, 0.3F);
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
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				Vec3d g = target.subtract(cam).add(0, 0.8, 0);
				float ramp = (float) Math.min(1.0, (a - THROW) / 20.0);
				// Molten collar where the spear bores in, sparks thrown off in a spiral, and a dust ring.
				FxDraw.disc(glow, m, g, x, z, radius * 4.5, 0xFF8A20, 0.5F * ramp, 0.0F, 32);
				FxDraw.disc(glow, m, g.add(0, 0.1, 0), x, z, radius * 2.2, 0xFFE0A0, 0.7F * ramp, 0.0F, 24);
				for (int k = 0; k < 3; k++) {
					double rr = radius * (2.4 + ((a * 0.6 + k * 12) % 30) * 0.3);
					FxDraw.ring(glow, m, g, x, z, rr * 0.9, rr, 0xFFD090, 0.0F, 0.6F * ramp, 0.0F, 40);
				}
				for (int i = 0; i < 18; i++) {
					double t = ((a * 0.05 + i / 18.0) % 1.0);
					double ang = sp * 2 + i * 2.4;
					double rr = radius * (1.6 + t * 6);
					Vec3d p0 = g.add(Math.cos(ang) * rr, t * radius * 5 - t * t * radius * 4, Math.sin(ang) * rr);
					Vec3d p1 = g.add(Math.cos(ang - 0.25) * rr * 0.92, (t - 0.04) * radius * 5, Math.sin(ang - 0.25) * rr * 0.92);
					FxDraw.ribbon(glow, m, p1, p0, Vec3d.ZERO, radius * 0.12, i % 3 == 0 ? 0xFFFFFF : 0xFFA040, 0.0F, 0.9F * (float) (1 - t) * ramp);
				}
				double e = a - THROW;
				// The landing: a fireball of gold, a spark shower and two textured shock fronts racing out.
				if (e < 40) {
					float k = (float) (e / 40.0);
					FxKit.fire(consumers, m, cb, g.add(0, radius * 3, 0), radius * 9, 0xFFE0A0, k, 1.0F, a, 91);
					FxKit.sparks(consumers, m, g.add(0, 2, 0), radius * 40, 90, 0xFFC070, k, 1.0F, 92);
					FxKit.groundShock(consumers, m, g, radius * 4 + e * radius * 3.2, 0xFFE0B0, 1 - k);
					FxKit.groundShock(consumers, m, g, radius * 2 + e * radius * 1.8, 0xFFFFFF, 0.8F * (1 - k));
				}
				// Dust rolling out from the bore, and smoke climbing off the molten collar while it drills.
				java.util.List<Effects.Puff> dust = FxKit.puffs();
				FxKit.dustRing(m, cb, g, radius * 6 + Math.min(e, 60) * radius * 0.25, radius * 2.2, 0xA89478, 0x4E4232, 0.55F * ramp, 93, dust);
				FxKit.column(g.add(0, radius, 0), radius * 22, radius * 4, a, 0x8A7E70, 0x2C2420, 0.45F * ramp, 94, dust);
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
