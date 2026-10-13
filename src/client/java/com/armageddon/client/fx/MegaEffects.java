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
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			int layers = 34;
			Vec3d[] prev = new Vec3d[10];
			for (int k = 0; k <= layers; k++) {
				double h = H * k / layers;
				double r = 5 + (radius - 5) * Math.pow(h / H, 1.4);
				Vec3d c = center(h, a).subtract(cam);
				int col = Effects.fade(0x3C4248, 0x9AA4AE, (float) (h / H));
				FxDraw.ring(shade, m, c, x, z, r * 0.72, r, col, 0.0F, 0.55F * env, 0.0F, 28);
				for (int s = 0; s < 10; s++) {
					double ang = s * MathHelper.TAU / 10 + a * (0.32 + h * 0.0016) + h * 0.05;
					Vec3d p = c.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
					if (prev[s] != null) {
						FxDraw.ribbon(shade, m, prev[s], p, Vec3d.ZERO, r * 0.14, Effects.fade(0x7A848E, 0xDDE4EA, (float) (h / H)), 0.5F * env, 0.5F * env);
					}
					prev[s] = p;
				}
				if (k % 6 == 0) {
					FxDraw.ring(glow, m, c, x, z, r * 0.95, r * 1.02, 0xB8C8FF, 0.0F, 0.12F * env, 0.0F, 28);
				}
			}
			Vec3d g = base.subtract(cam);
			FxDraw.disc(shade, m, g.add(0, 1, 0), x, z, radius * 1.5, 0x6A5A48, 0.5F * env, 0.0F, 32);
			FxDraw.disc(shade, m, center(H, a).subtract(cam), x, z, radius * 2.6, 0x50565C, 0.45F * env, 0.0F, 32);
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
			int lat = 14;
			Solids.ellipsoid(solid, m, p, R, R * 0.16, R, lat, 44, (i, j, n) -> {
				if (Math.abs(i - lat / 2) <= 1 && j % 2 == 0) {
					return 0x9FE0FF;
				}
				double panel = (j % 4 == 0 || i % 5 == 0) ? 0.78 : 1.0;
				return Solids.scale(0x5E6A80, panel * (0.9 + 0.2 * Solids.hash(i / 2, j / 2)));
			}, true, 1.0F);
			Solids.ellipsoid(solid, m, p.add(0, -R * 0.07, 0), R * 0.58, R * 0.13, R * 0.58, 8, 28, (i, j, n) -> Solids.scale(0x343C4C, 0.9 + 0.2 * Solids.hash(i, j)), true, 1.0F);
			Solids.ellipsoid(shade, m, p.add(0, R * 0.08, 0), R * 0.38, R * 0.3, R * 0.38, 8, 24, (i, j, n) -> 0x7AD8FF, false, 0.4F);
			Solids.ellipsoid(glow, m, p.add(0, R * 0.12, 0), R * 0.22, R * 0.14, R * 0.22, 6, 16, (i, j, n) -> 0x40FFB0, false, 0.5F);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			for (int k = 0; k < 32; k++) {
				double ang = k * MathHelper.TAU / 32 + a * 0.05;
				Vec3d lp = p.add(Math.cos(ang) * R * 0.965, -R * 0.015, Math.sin(ang) * R * 0.965);
				Solids.dot(glow, m, lp, cb, R * 0.035, (k + (int) (a * 0.2)) % 2 == 0 ? 0x40A0FF : 0xFFFFFF, 0.9F);
			}
			Vec3d emitter = p.add(0, -R * 0.2, 0);
			Solids.dot(glow, m, emitter, cb, R * 0.3, 0x40A0FF, 0.5F);
			if (beamOn(a)) {
				Vec3d target = lastBeam.subtract(cam);
				double ramp = Math.min(1.0, Math.min((a - lifetime * 0.12) / 14.0, (lifetime * 0.88 - a) / 14.0));
				Solids.tube(glow, m, emitter, target, R * 0.08, R * 0.2, 14, 0x2080FF, (float) (0.22 * ramp), false);
				Solids.tube(glow, m, emitter, target, R * 0.03, R * 0.08, 10, 0x80C8FF, (float) (0.5 * ramp), false);
				FxDraw.beam(glow, m, emitter, target, Vec3d.ZERO, R * 0.05, 0xBFE8FF, (float) ramp);
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				for (int k = 0; k < 3; k++) {
					double rr = ((a * 3 + k * 20) % 60) / 60.0 * R * 0.35 + 2;
					FxDraw.ring(glow, m, target.add(0, 0.7, 0), x, z, rr * 0.85, rr, 0x80C8FF, 0.0F, (float) (0.8 * ramp), 0.0F, 40);
				}
				FxDraw.disc(glow, m, target.add(0, 0.8, 0), x, z, R * 0.3, 0xBFE8FF, (float) (0.8 * ramp), 0.0F, 32);
			}
		}
	}

	// ======================================================================== SPEAR

	public static class Spear extends FxEffect {
		private static final int THROW = 36;
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
				return target.subtract(throwDir.multiply(380 * (1 - u * u)));
			}
			double sink = depth * ease((a - THROW - 24) / (lifetime - THROW - 40.0));
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
			Vec3d ax = axis(a);
			Vec3d tip = tip(a).subtract(cam);
			Vec3d back = tip.subtract(ax.multiply(length));
			double headLen = length * 0.17;
			Vec3d headBase = tip.subtract(ax.multiply(headLen));
			Solids.tube(solid, m, back, headBase, radius, radius * 1.05, 12, 0x6E5A3A, 1.0F, true);
			Solids.tube(solid, m, headBase, tip, radius * 1.45, 0.0, 12, 0xD4D8E4, 1.0F, true);
			Solids.tube(solid, m, headBase.subtract(ax.multiply(radius)), headBase, radius * 1.9, radius * 1.45, 12, 0x2A2A34, 1.0F, true);
			Solids.tube(glow, m, headBase, tip, radius * 1.5, 0.0, 12, 0xFFB050, 0.3F, false);
			Vec3d[] bs = ArsenalEffects.basis(ax);
			double sp = spin(a);
			for (int k = 0; k < 4; k++) {
				Vec3d prev = null;
				for (int s = 0; s <= 28; s++) {
					double u = s / 28.0;
					double ang = sp + k * MathHelper.TAU / 4 + u * 14;
					Vec3d p = back.add(ax.multiply(length * 0.82 * u)).add(bs[0].multiply(Math.cos(ang) * radius * 1.04)).add(bs[1].multiply(Math.sin(ang) * radius * 1.04));
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, radius * 0.16, k % 2 == 0 ? 0xFFB050 : 0xFFFFFF, 0.9F, 0.9F);
					}
					prev = p;
				}
			}
			for (int k = 0; k < 6; k++) {
				Vec3d p = back.add(ax.multiply(length * (0.12 + 0.14 * k)));
				FxDraw.ring(glow, m, p, bs[0], bs[1], radius * 1.05, radius * 1.25, 0xFFB050, 0.0F, 0.7F, 0.0F, 24);
			}
			for (int f = 0; f < 3; f++) {
				double ang = sp * 0.5 + f * MathHelper.TAU / 3;
				Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
				Solids.tube(solid, m, back.add(ax.multiply(length * 0.12)), back.add(out.multiply(radius * 3.2)).subtract(ax.multiply(length * 0.04)), radius * 0.5, radius * 0.08, 4, 0xB03A2A, 1.0F, true);
			}
			if (a >= THROW) {
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				Vec3d g = target.subtract(cam).add(0, 0.8, 0);
				float ramp = (float) Math.min(1.0, (a - THROW) / 20.0);
				FxDraw.disc(glow, m, g, x, z, radius * 4.5, 0xFF8A20, 0.5F * ramp, 0.0F, 32);
				for (int k = 0; k < 3; k++) {
					double rr = radius * (2.4 + ((a * 0.6 + k * 12) % 30) * 0.3);
					FxDraw.ring(glow, m, g, x, z, rr * 0.9, rr, 0xFFD090, 0.0F, 0.6F * ramp, 0.0F, 40);
				}
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
			Solids.rocket(solid, glow, m, p, dir, len, len * 0.095, a, 1.4);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Solids.dot(glow, m, p, cb, len * 1.6, 0xA070FF, 0.18F);
			// Long comet tail.
			Vec3d tail = p.subtract(dir.multiply(len * 0.5));
			FxDraw.beam(glow, m, tail, tail.subtract(dir.multiply(len * 9)), Vec3d.ZERO, len * 0.12, 0x80F0FF, 0.6F);
			FxDraw.beam(glow, m, tail, tail.subtract(dir.multiply(len * 5)), Vec3d.ZERO, len * 0.22, 0xA070FF, 0.4F);
		}
	}
}
