package com.armageddon.client.fx;

import com.armageddon.client.particle.ModParticleFactories;
import com.armageddon.client.particle.WmdParticle;
import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/** All concrete visual effects. */
public final class Effects {
	private Effects() {
	}

	static int fade(int rgb, int to, float t) {
		t = MathHelper.clamp(t, 0, 1);
		int r = (int) MathHelper.lerp(t, (rgb >> 16) & 0xFF, (to >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (rgb >> 8) & 0xFF, (to >> 8) & 0xFF);
		int b = (int) MathHelper.lerp(t, rgb & 0xFF, to & 0xFF);
		return (r << 16) | (g << 8) | b;
	}

	static void spawn(SpriteProvider sprites, WmdParticle.Preset preset, double x, double y, double z, double vx, double vy, double vz, float size) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (sprites == null || mc.world == null) {
			return;
		}
		mc.particleManager.addParticle(new WmdParticle(mc.world, x, y, z, vx, vy, vz, sprites, preset, size));
	}

	// ======================================================================= beam

	public static class Beam extends FxEffect {
		private final Vec3d from, to;
		private final double width;
		private final int color;

		public Beam(Vec3d from, Vec3d to, double width, int color, int lifetime) {
			super(lifetime);
			this.from = from;
			this.to = to;
			this.width = width;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float alpha = (1.0F - t) * (1.0F - t);
			double w = width * (1.0 + t * 1.5);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, from.subtract(cam), to.subtract(cam), Vec3d.ZERO, w, color, alpha);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 0) {
				Vec3d d = to.subtract(from);
				double len = d.length();
				for (double s = 0; s < len; s += 1.5) {
					Vec3d p = from.add(d.multiply(s / len));
					world.addParticle(ModParticles.ARC_SPARK, p.x, p.y, p.z, world.random.nextGaussian() * 0.05, world.random.nextGaussian() * 0.05, world.random.nextGaussian() * 0.05);
				}
			}
			return super.tick(world);
		}
	}

	// ======================================================================= arc

	public static class Arc extends FxEffect {
		private final Vec3d from, to;
		private final double width;
		private final int color;
		private final Random random;
		private Vec3d[] points;

		public Arc(Vec3d from, Vec3d to, double width, int color, int seed) {
			super(7);
			this.from = from;
			this.to = to;
			this.width = width;
			this.color = color;
			this.random = Random.create(seed);
			regenerate();
		}

		private void regenerate() {
			double len = from.distanceTo(to);
			int n = Math.max(4, (int) (len * 1.6));
			points = new Vec3d[n + 1];
			double jitter = Math.min(1.2, 0.15 + len * 0.06);
			for (int i = 0; i <= n; i++) {
				double t = i / (double) n;
				Vec3d p = from.lerp(to, t);
				if (i > 0 && i < n) {
					double env = Math.sin(t * Math.PI);
					p = p.add(random.nextGaussian() * jitter * env, random.nextGaussian() * jitter * env, random.nextGaussian() * jitter * env);
				}
				points[i] = p;
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 2 == 1) {
				regenerate();
			}
			if (age == 0) {
				for (int i = 0; i < 8; i++) {
					world.addParticle(ModParticles.ARC_SPARK, to.x, to.y, to.z, world.random.nextGaussian() * 0.2, world.random.nextDouble() * 0.2, world.random.nextGaussian() * 0.2);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float alpha = 1.0F - progress(tickDelta) * 0.8F;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			for (int i = 0; i < points.length - 1; i++) {
				FxDraw.beam(vc, m, points[i].subtract(cam), points[i + 1].subtract(cam), Vec3d.ZERO, width, color, alpha);
			}
			// Side branch.
			if (points.length > 4) {
				Vec3d b0 = points[points.length / 3];
				Vec3d b1 = b0.add(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
				FxDraw.beam(vc, m, b0.subtract(cam), b1.subtract(cam), Vec3d.ZERO, width * 0.6, color, alpha * 0.7F);
			}
		}
	}

	// ======================================================================= ring

	/** Horizontal expanding shockwave ring. */
	public static class Ring extends FxEffect {
		private final Vec3d center;
		private final double maxRadius;
		private final int color;
		private final double thickness;
		private final boolean dust;

		public Ring(Vec3d center, double maxRadius, int color, int lifetime, double thickness, boolean dust) {
			super(lifetime);
			this.center = center;
			this.maxRadius = maxRadius;
			this.color = color;
			this.thickness = thickness;
			this.dust = dust;
		}

		double radius(float tickDelta) {
			float t = progress(tickDelta);
			return maxRadius * (1 - Math.pow(1 - t, 2.2));
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (dust && ModParticleFactories.dustSprites != null && age % 2 == 0) {
				double r = radius(0);
				int n = (int) Math.min(48, 8 + r * 0.8);
				for (int i = 0; i < n; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double x = center.x + Math.cos(a) * r;
					double z = center.z + Math.sin(a) * r;
					int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
					if (Math.abs(top - center.y) > maxRadius * 0.5) {
						continue;
					}
					spawn(ModParticleFactories.dustSprites, ModParticleFactories.SHOCK_DUST, x, top + 0.5, z,
							Math.cos(a) * 0.4, 0.05 + world.random.nextDouble() * 0.15, Math.sin(a) * 0.4, (float) Math.min(4.0, 1.0 + maxRadius / 60.0));
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			double r = radius(tickDelta);
			double th = thickness * (0.6 + t);
			float alpha = (1 - t) * 0.9F;
			Vec3d c = center.subtract(cam).add(0, 0.3, 0);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.ring(vc, m, c, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Math.max(0, r - th), r + th * 0.3, color, 0.0F, alpha, 0.0F, 96);
			// Faint vertical shock front.
			int segs = 64;
			double h = th * 0.8;
			for (int i = 0; i < segs; i++) {
				double a0 = MathHelper.TAU * i / segs, a1 = MathHelper.TAU * (i + 1) / segs;
				double x0 = Math.cos(a0) * r, z0 = Math.sin(a0) * r, x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
				FxDraw.vertex(vc, m, c.x + x0, c.y, c.z + z0, color, alpha * 0.5F);
				FxDraw.vertex(vc, m, c.x + x1, c.y, c.z + z1, color, alpha * 0.5F);
				FxDraw.vertex(vc, m, c.x + x1, c.y + h, c.z + z1, color, 0.0F);
				FxDraw.vertex(vc, m, c.x + x0, c.y + h, c.z + z0, color, 0.0F);
			}
		}
	}

	// ======================================================================= fireball

	public static class Fireball extends FxEffect {
		private final Vec3d center;
		private final double maxRadius;
		private final int growTicks;
		private final int hot, cool;

		public Fireball(Vec3d center, double maxRadius, int growTicks, int lifetime, int hot, int cool) {
			super(lifetime);
			this.center = center;
			this.maxRadius = maxRadius;
			this.growTicks = growTicks;
			this.hot = hot;
			this.cool = cool;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float a = age + tickDelta;
			float grow = MathHelper.clamp(a / growTicks, 0, 1);
			double r = maxRadius * (1 - Math.pow(1 - grow, 3));
			float t = progress(tickDelta);
			int color = fade(hot, cool, t * 1.4F);
			float alpha = (float) Math.pow(1 - t, 1.5);
			Vec3d c = center.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, color, alpha * 0.25F, alpha * 0.9F, 14, 20);
			FxDraw.sphere(vc, m, c, r * 0.7, Vec3d.ZERO, 0xFFFFFF, alpha * 0.6F, alpha * 0.1F, 10, 16);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, c, basis[0], basis[1], r * 2.4, color, alpha * 0.5F, 0.0F, 32);
		}
	}

	// ======================================================================= mushroom cloud

	/**
	 * Builds a mushroom cloud out of hundreds of big emissive smoke particles: a rising, rolling
	 * toroidal cap, a stem, a base surge and a condensation ring.
	 */
	public static class MushroomCloud extends FxEffect {
		private final Vec3d ground;
		private final double scale;
		private final double height;
		private final Random random = Random.create();

		public MushroomCloud(Vec3d ground, double yieldRadius) {
			super(150);
			this.ground = ground;
			this.scale = yieldRadius;
			this.height = Math.min(220, yieldRadius * 4.2 + 20);
		}

		private double capHeight(float t) {
			return height * (1 - Math.exp(-t / 38.0));
		}

		@Override
		public boolean tick(ClientWorld world) {
			SpriteProvider fire = ModParticleFactories.fireSprites;
			SpriteProvider smoke = ModParticleFactories.smokeSprites;
			if (fire == null || smoke == null) {
				return super.tick(world);
			}
			float size = (float) Math.max(0.6, scale / 9.0);
			double capY = ground.y + capHeight(age);
			double rise = height / 38.0 * Math.exp(-age / 38.0);
			double capR = scale * (0.5 + 0.9 * Math.min(1.0, age / 70.0));
			double tube = scale * 0.45;

			// Cap: points on a torus, velocities rolling outward-over-top.
			int capCount = age < 100 ? 7 : 3;
			for (int i = 0; i < capCount; i++) {
				double u = random.nextDouble() * MathHelper.TAU;
				double v = random.nextDouble() * MathHelper.TAU;
				double ring = capR + tube * Math.cos(v);
				double x = ground.x + Math.cos(u) * ring;
				double z = ground.z + Math.sin(u) * ring;
				double y = capY + tube * Math.sin(v) * 0.8;
				double roll = 0.04 * size;
				double vx = Math.cos(u) * (-Math.sin(v)) * roll;
				double vz = Math.sin(u) * (-Math.sin(v)) * roll;
				double vy = rise * 0.95 + Math.cos(v) * roll;
				boolean hot = age < 60 || random.nextFloat() < 0.15F;
				spawn(hot ? fire : smoke, hot ? ModParticleFactories.NUKE_FIRE : ModParticleFactories.NUKE_SMOKE, x, y, z, vx, vy, vz, size * 1.15F);
			}
			// Stem.
			if (age > 4 && age < 130) {
				for (int i = 0; i < 3; i++) {
					double h = random.nextDouble() * Math.max(1, capHeight(age) - tube);
					double sr = scale * (0.18 + 0.25 * (1 - h / height)) * random.nextDouble();
					double a = random.nextDouble() * MathHelper.TAU;
					spawn(age < 50 ? fire : smoke, age < 50 ? ModParticleFactories.NUKE_FIRE : ModParticleFactories.NUKE_SMOKE,
							ground.x + Math.cos(a) * sr, ground.y + h, ground.z + Math.sin(a) * sr,
							0, rise * 0.6 + 0.05, 0, size * 0.8F);
				}
			}
			// Base surge: dust rolling out along the ground.
			if (age < 40) {
				for (int i = 0; i < 6; i++) {
					double a = random.nextDouble() * MathHelper.TAU;
					double r = scale * (0.3 + random.nextDouble() * 0.6);
					double sp = 0.35 + random.nextDouble() * 0.5;
					spawn(smoke, ModParticleFactories.NUKE_SMOKE, ground.x + Math.cos(a) * r, ground.y + 1 + random.nextDouble() * 3,
							ground.z + Math.sin(a) * r, Math.cos(a) * sp * size * 0.5, 0.02, Math.sin(a) * sp * size * 0.5, size);
				}
			}
			// Condensation (Wilson) ring around the stem.
			if (age > 18 && age < 34 && age % 2 == 0) {
				double wy = ground.y + capHeight(age) * 0.55;
				double wr = scale * (1.4 + (age - 18) * 0.06);
				for (int i = 0; i < 16; i++) {
					double a = MathHelper.TAU * i / 16 + random.nextDouble() * 0.2;
					spawn(smoke, ModParticleFactories.CRYO_MIST, ground.x + Math.cos(a) * wr, wy, ground.z + Math.sin(a) * wr,
							Math.cos(a) * 0.05, 0.01, Math.sin(a) * 0.05, size * 6.0F);
				}
			}
			// Embers raining from the fireball.
			if (age < 80) {
				for (int i = 0; i < 4; i++) {
					world.addParticle(ModParticles.EMBER, ground.x + random.nextGaussian() * scale, capY + random.nextGaussian() * tube,
							ground.z + random.nextGaussian() * scale, random.nextGaussian() * 0.3, random.nextDouble() * 0.2, random.nextGaussian() * 0.3);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			// Inner glow of the rising fireball inside the cap.
			float t = progress(tickDelta);
			if (t > 0.6F) {
				return;
			}
			float a = (1 - t / 0.6F);
			double capY = ground.y + capHeight(age + tickDelta);
			Vec3d c = new Vec3d(ground.x, capY, ground.z).subtract(cam);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW_NO_DEPTH);
			FxDraw.disc(vc, m, c, basis[0], basis[1], scale * 1.6, 0xFF8A3A, a * 0.55F, 0.0F, 32);
			FxDraw.disc(vc, m, c, basis[0], basis[1], scale * 0.7, 0xFFE8B0, a * 0.6F, 0.0F, 24);
		}
	}

	// ======================================================================= orbital

	public static class OrbitalTarget extends FxEffect {
		private final Vec3d pos;

		public OrbitalTarget(Vec3d pos, int lifetime) {
			super(lifetime);
			this.pos = pos;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float pulse = 0.6F + 0.4F * MathHelper.sin((age + tickDelta) * (0.4F + t * 1.2F));
			Vec3d c = pos.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, c, c.add(0, 320, 0), Vec3d.ZERO, 0.12 + t * 0.2, 0xFF2A1A, 0.8F * pulse);
			double r = 7.0 * (1 - t) + 1.2;
			Vec3d g = c.add(0, 0.08, 0);
			FxDraw.ring(vc, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), r - 0.25, r + 0.25, 0xFF3322, 0.0F, pulse, 0.0F, 64);
			FxDraw.ring(vc, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 4.5, 4.8, 0xFF5544, 0.0F, 0.6F, 0.0F, 64);
			// Crosshair ticks.
			for (int i = 0; i < 4; i++) {
				double a = MathHelper.HALF_PI * i + (age + tickDelta) * 0.05;
				Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
				FxDraw.ribbon(vc, m, g.add(d.multiply(5.2)), g.add(d.multiply(7.5)), Vec3d.ZERO, 0.25, 0xFF3322, pulse, pulse);
			}
		}
	}

	public static class OrbitalBeam extends FxEffect {
		private final Vec3d pos;
		private final double radius;

		public OrbitalBeam(Vec3d pos, double radius) {
			super(46);
			this.pos = pos;
			this.radius = radius;
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int i = 0; i < 10; i++) {
				double a = world.random.nextDouble() * MathHelper.TAU;
				double r = radius * (0.5 + world.random.nextDouble());
				world.addParticle(ModParticles.PLASMA_SPARK, pos.x + Math.cos(a) * r, pos.y + world.random.nextDouble() * 4, pos.z + Math.sin(a) * r,
						Math.cos(a) * 0.4, 0.3 + world.random.nextDouble() * 0.5, Math.sin(a) * 0.4);
			}
			if (age % 3 == 0) {
				world.addImportantParticle(ParticleTypes.EXPLOSION_EMITTER, true, pos.x, pos.y, pos.z, 0, 0, 0);
			}
			if (age < 30 && ModParticleFactories.smokeSprites != null) {
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					spawn(ModParticleFactories.smokeSprites, ModParticleFactories.NUKE_SMOKE, pos.x + Math.cos(a) * radius, pos.y + 1, pos.z + Math.sin(a) * radius,
							Math.cos(a) * 0.3, 0.25, Math.sin(a) * 0.3, 0.5F);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float in = MathHelper.clamp((age + tickDelta) / 4.0F, 0, 1);
			float alpha = in * (1 - t * t);
			double w = radius * 2 * (0.4 + 0.6 * in) * (1 + 0.08 * MathHelper.sin((age + tickDelta) * 2.1F));
			Vec3d base = pos.subtract(cam).add(0, -2, 0);
			Vec3d top = base.add(0, 400, 0);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, base, top, Vec3d.ZERO, w, 0x8FE6FF, alpha);
			FxDraw.beam(vc, m, base, top, Vec3d.ZERO, w * 0.35, 0xFFFFFF, alpha);
			Vec3d g = pos.subtract(cam).add(0, 0.2, 0);
			FxDraw.disc(vc, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius * 4, 0x8FE6FF, alpha * 0.8F, 0.0F, 48);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, g.add(0, 1, 0), basis[0], basis[1], radius * 3.5, 0xFFFFFF, alpha * 0.7F, 0.0F, 32);
		}
	}

	// ======================================================================= antimatter

	public static class Antimatter extends FxEffect {
		private final Vec3d center;
		private final double radius;
		private static final int IMPLODE = 28;

		public Antimatter(Vec3d center, double radius) {
			super(IMPLODE + 50);
			this.center = center;
			this.radius = radius;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < IMPLODE) {
				for (int i = 0; i < 20; i++) {
					Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize();
					double r = radius * 1.6 * (1 - age / (double) IMPLODE) + 1;
					Vec3d p = center.add(d.multiply(r));
					Vec3d v = d.multiply(-r / 6.0);
					world.addImportantParticle(ModParticles.VOID_SPARK, true, p.x, p.y, p.z, v.x, v.y, v.z);
				}
			} else if (age == IMPLODE) {
				ClientFx.flash(center, radius * 30, 1.0F, 0xF0D8FF);
				ClientFx.shake(center, radius * 12, 2.5F);
				for (int i = 0; i < 200; i++) {
					Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize();
					Vec3d v = d.multiply(1.5 + world.random.nextDouble() * 2);
					world.addImportantParticle(ModParticles.VOID_SPARK, true, center.x, center.y, center.z, v.x, v.y, v.z);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float a = age + tickDelta;
			Vec3d c = center.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			if (a < IMPLODE) {
				float t = a / IMPLODE;
				double r = radius * 1.4 * (1 - t * t) + 0.5;
				FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, 0xB060FF, 0.05F + t * 0.2F, 0.6F * t, 16, 24);
				Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
				FxDraw.disc(vc, m, c, basis[0], basis[1], 1.5 + t * 4, 0xFFFFFF, t, 0.0F, 24);
			} else {
				float t = MathHelper.clamp((a - IMPLODE) / 50.0F, 0, 1);
				double r = radius * (1 - Math.pow(1 - Math.min(1, t * 2.5), 3));
				float alpha = (1 - t);
				FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, fade(0xFFFFFF, 0xC070FF, t * 2), alpha * 0.4F, alpha, 18, 28);
				FxDraw.sphere(vc, m, c, r * 1.05, Vec3d.ZERO, 0x7030FF, 0.0F, alpha * 0.6F, 18, 28);
			}
		}
	}
}
