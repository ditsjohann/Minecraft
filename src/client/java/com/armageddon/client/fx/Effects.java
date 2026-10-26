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

	// ======================================================================= shared look

	static float smooth(float t) {
		t = MathHelper.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** Black-body style ramp: white-hot, yellow, orange, deep red, then cooling soot. t in 0..1. */
	static int heat(float t) {
		t = MathHelper.clamp(t, 0, 1);
		if (t < 0.15F) {
			return fade(0xFFFFF4, 0xFFE79A, t / 0.15F);
		}
		if (t < 0.35F) {
			return fade(0xFFE79A, 0xFF9A3A, (t - 0.15F) / 0.2F);
		}
		if (t < 0.6F) {
			return fade(0xFF9A3A, 0xD0401A, (t - 0.35F) / 0.25F);
		}
		if (t < 0.8F) {
			return fade(0xD0401A, 0x5A2A20, (t - 0.6F) / 0.2F);
		}
		return fade(0x5A2A20, 0x3A3634, (t - 0.8F) / 0.2F);
	}

	/** Cheap deterministic hash in 0..1. */
	static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	/** Segment count for something of {@code size} at camera-relative {@code rel}: fewer when far and small on screen. */
	static int lod(Vec3d rel, double size, int min, int max) {
		double d = Math.max(1.0, rel.length());
		double k = MathHelper.clamp(size / d * 3.0, 0.0, 1.0);
		return min + (int) ((max - min) * k);
	}

	static Vec3d dir(int i, int salt) {
		double a = hash(i, salt) * MathHelper.TAU;
		double y = hash(i, salt + 1) * 2 - 1;
		double r = Math.sqrt(1 - y * y);
		return new Vec3d(Math.cos(a) * r, y, Math.sin(a) * r);
	}

	/**
	 * A soft, camera-facing smoke puff with a vertical light gradient: lit {@code top}, shadowed {@code bottom},
	 * a dense middle and feathered edge. Drawn on an alpha-blended layer it reads as a volumetric billow.
	 */
	static void puff(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d[] cb, double r, int top, int bottom, float alpha, int segs) {
		puff(vc, m, c, cb, r, top, bottom, alpha, segs, (int) (Double.doubleToLongBits(r) >>> 20));
	}

	/**
	 * The billow as two textured smoke sprites from the FX atlas (a big puff and a smaller one riding on it), lit by
	 * the world so it darkens at night, with fire-lit undersides glowing. {@code id} picks the puff shape and spin.
	 */
	static void puff(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d[] cb, double r, int top, int bottom, float alpha, int segs, int id) {
		if (alpha <= 0.004F || r <= 0.01) {
			return;
		}
		com.armageddon.client.render.FxBuffers fb = com.armageddon.client.render.FxBuffers.current();
		if (fb != null) {
			VertexConsumer smoke = fb.getBuffer(com.armageddon.client.render.FxSprites.SMOKE_LAYER);
			int br = (bottom >> 16) & 0xFF, bb = bottom & 0xFF;
			float glow = MathHelper.clamp((br - bb) / 200.0F, 0.0F, 1.0F) * (br / 255.0F);
			int light = com.armageddon.client.render.FxSprites.light(c, glow);
			int cell = com.armageddon.client.render.FxSprites.SMOKE + Math.floorMod(id, 6);
			double rot = com.armageddon.client.render.FxSprites.hash(id, 1) * MathHelper.TAU;
			com.armageddon.client.render.FxSprites.sprite(smoke, m, c, cb[0], cb[1], r * 1.3, rot, cell, top, bottom, Math.min(1.0F, alpha * 1.25F), light);
			Vec3d lift = cb[1].multiply(r * 0.3).add(cb[0].multiply((com.armageddon.client.render.FxSprites.hash(id, 2) - 0.5) * r * 0.6));
			com.armageddon.client.render.FxSprites.sprite(smoke, m, c.add(lift), cb[0], cb[1], r * 0.85, rot + 2.1,
					com.armageddon.client.render.FxSprites.SMOKE + Math.floorMod(id + 3, 6), top, fade(bottom, top, 0.5F), alpha * 0.8F, light);
			return;
		}
		int mid = fade(bottom, top, 0.55F);
		double inner = 0.55;
		for (int i = 0; i < segs; i++) {
			double a0 = MathHelper.TAU * i / segs, a1 = MathHelper.TAU * (i + 1) / segs;
			double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
			int col0 = fade(bottom, top, (float) (s0 * 0.5 + 0.5)), col1 = fade(bottom, top, (float) (s1 * 0.5 + 0.5));
			Vec3d i0 = c.add(cb[0].multiply(c0 * r * inner)).add(cb[1].multiply(s0 * r * inner));
			Vec3d i1 = c.add(cb[0].multiply(c1 * r * inner)).add(cb[1].multiply(s1 * r * inner));
			Vec3d o0 = c.add(cb[0].multiply(c0 * r)).add(cb[1].multiply(s0 * r));
			Vec3d o1 = c.add(cb[0].multiply(c1 * r)).add(cb[1].multiply(s1 * r));
			FxDraw.vertex(vc, m, c.x, c.y, c.z, mid, alpha);
			FxDraw.vertex(vc, m, c.x, c.y, c.z, mid, alpha);
			FxDraw.vertex(vc, m, i1.x, i1.y, i1.z, col1, alpha * 0.85F);
			FxDraw.vertex(vc, m, i0.x, i0.y, i0.z, col0, alpha * 0.85F);
			FxDraw.vertex(vc, m, i0.x, i0.y, i0.z, col0, alpha * 0.85F);
			FxDraw.vertex(vc, m, i1.x, i1.y, i1.z, col1, alpha * 0.85F);
			FxDraw.vertex(vc, m, o1.x, o1.y, o1.z, col1, 0.0F);
			FxDraw.vertex(vc, m, o0.x, o0.y, o0.z, col0, 0.0F);
		}
	}

	/** A queued smoke billow; translucent puffs must be drawn back to front or far ones paint over near ones. */
	record Puff(Vec3d c, double r, int top, int bottom, float alpha) {
	}

	static void drawPuffs(java.util.List<Puff> puffs, VertexConsumer vc, Matrix4f m, Vec3d[] cb, int segs) {
		// Ids from the build order (stable frame to frame), then back-to-front.
		Integer[] order = new Integer[puffs.size()];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		java.util.Arrays.sort(order, (x, y) -> Double.compare(puffs.get(y).c().lengthSquared(), puffs.get(x).c().lengthSquared()));
		for (int i : order) {
			Puff p = puffs.get(i);
			puff(vc, m, p.c(), cb, p.r(), p.top(), p.bottom(), p.alpha(), segs, i * 7 + 3);
		}
	}

	/** Horizontal anamorphic lens streak through a bright point. */
	static void streak(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d[] cb, double length, double width, int rgb, float alpha) {
		if (alpha <= 0.004F) {
			return;
		}
		FxDraw.ribbon(vc, m, c, c.add(cb[0].multiply(length)), Vec3d.ZERO, width, rgb, alpha, 0.0F);
		FxDraw.ribbon(vc, m, c, c.subtract(cb[0].multiply(length)), Vec3d.ZERO, width, rgb, alpha, 0.0F);
	}

	/** Jagged glowing bolt between two camera-relative points, re-rolled by {@code seed}. */
	static void bolt(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double width, int rgb, float alpha, int seed) {
		Vec3d d = b.subtract(a);
		double len = d.length();
		if (len < 0.01) {
			return;
		}
		int n = Math.max(4, Math.min(14, (int) (len / 3)));
		double jitter = len * 0.12;
		Vec3d prev = a;
		for (int i = 1; i <= n; i++) {
			double t = i / (double) n;
			Vec3d p = a.add(d.multiply(t));
			if (i < n) {
				double env = Math.sin(t * Math.PI);
				p = p.add((hash(seed, i * 3) - 0.5) * 2 * jitter * env, (hash(seed, i * 3 + 1) - 0.5) * 2 * jitter * env, (hash(seed, i * 3 + 2) - 0.5) * 2 * jitter * env);
			}
			FxDraw.ribbon(vc, m, prev, p, Vec3d.ZERO, width * 3, rgb, alpha * 0.25F, alpha * 0.25F);
			FxDraw.ribbon(vc, m, prev, p, Vec3d.ZERO, width, 0xFFFFFF, alpha, alpha);
			prev = p;
		}
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
			Vec3d a = from.subtract(cam), b = to.subtract(cam);
			FxDraw.beam(vc, m, a, b, Vec3d.ZERO, w, color, alpha);
			// White-hot core that thins out as the beam dies, and a bright packet racing down it.
			FxDraw.ribbon(vc, m, a, b, Vec3d.ZERO, w * 0.35 * (1 - t), 0xFFFFFF, alpha, alpha);
			double s = MathHelper.clamp((age + tickDelta) / Math.max(1.0F, lifetime * 0.5F), 0, 1);
			Vec3d pk = a.lerp(b, s);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, pk, cb[0], cb[1], w * 2.5, fade(color, 0xFFFFFF, 0.5F), alpha * 0.8F, 0.0F, 12);
			// Muzzle and impact flares.
			FxDraw.disc(vc, m, a, cb[0], cb[1], w * 3.5, color, alpha * 0.7F, 0.0F, 14);
			FxDraw.disc(vc, m, b, cb[0], cb[1], w * 5.0, color, alpha * 0.8F, 0.0F, 16);
			FxDraw.disc(vc, m, b, cb[0], cb[1], w * 1.8, 0xFFFFFF, alpha, 0.0F, 12);
			streak(vc, m, b, cb, w * 10, w * 0.5, color, alpha * 0.4F);
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
		private Vec3d[][] forks;

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
			// Two or three forks peeling off the main channel and tapering out.
			int count = points.length > 6 ? 2 + random.nextInt(2) : 1;
			forks = new Vec3d[count][];
			for (int f = 0; f < count; f++) {
				Vec3d start = points[1 + random.nextInt(Math.max(1, points.length - 2))];
				int segs = 3 + random.nextInt(3);
				Vec3d[] fork = new Vec3d[segs + 1];
				fork[0] = start;
				Vec3d heading = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().multiply(Math.max(0.3, len * 0.08));
				for (int i = 1; i <= segs; i++) {
					fork[i] = fork[i - 1].add(heading).add(random.nextGaussian() * jitter * 0.5, random.nextGaussian() * jitter * 0.5, random.nextGaussian() * jitter * 0.5);
				}
				forks[f] = fork;
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
			float flick = (age & 1) == 0 ? 1.0F : 0.75F;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			for (int i = 0; i < points.length - 1; i++) {
				Vec3d p0 = points[i].subtract(cam), p1 = points[i + 1].subtract(cam);
				FxDraw.ribbon(vc, m, p0, p1, Vec3d.ZERO, width * 6, color, alpha * 0.12F * flick, alpha * 0.12F * flick);
				FxDraw.beam(vc, m, p0, p1, Vec3d.ZERO, width, color, alpha * flick);
				FxDraw.ribbon(vc, m, p0, p1, Vec3d.ZERO, width * 0.35, 0xFFFFFF, alpha, alpha);
			}
			for (Vec3d[] fork : forks) {
				for (int i = 0; i < fork.length - 1; i++) {
					float k = 1.0F - i / (float) fork.length;
					FxDraw.beam(vc, m, fork[i].subtract(cam), fork[i + 1].subtract(cam), Vec3d.ZERO, width * 0.55 * k, color, alpha * 0.75F * k);
				}
			}
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d end = to.subtract(cam);
			FxDraw.disc(vc, m, end, cb[0], cb[1], width * 9, color, alpha * 0.6F * flick, 0.0F, 14);
			FxDraw.disc(vc, m, end, cb[0], cb[1], width * 3, 0xFFFFFF, alpha, 0.0F, 10);
			FxDraw.disc(vc, m, from.subtract(cam), cb[0], cb[1], width * 5, color, alpha * 0.5F, 0.0F, 10);
		}
	}

	// ======================================================================= ring

	/** Horizontal expanding shockwave ring: a hot leading edge, a refraction-like echo and a dust skirt. */
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
			if (r < 0.05) {
				return;
			}
			double th = thickness * (0.6 + t);
			float alpha = (1 - t) * 0.9F;
			float in = smooth((age + tickDelta) / 3.0F);
			alpha *= in;
			Vec3d c = center.subtract(cam).add(0, 0.3, 0);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			int segs = MathHelper.clamp((int) (r * 1.2), 32, 160);
			segs = Math.min(segs, lod(c, r, 32, 160));
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			int hot = fade(color, 0xFFFFFF, 0.55F);
			// Trailing band, then the bright leading edge.
			FxDraw.ring(vc, m, c, x, z, Math.max(0, r - th * 2.6), r - th * 0.2, color, 0.0F, alpha * 0.55F, alpha * 0.8F, segs);
			FxDraw.ring(vc, m, c, x, z, Math.max(0, r - th * 0.25), r + th * 0.35, hot, alpha, alpha, 0.0F, segs);
			// Refraction echo: a faint second front a little behind the first.
			double echo = r * 0.86;
			FxDraw.ring(vc, m, c, x, z, Math.max(0, echo - th * 0.6), echo + th * 0.2, color, 0.0F, alpha * 0.3F, 0.0F, segs);
			// Vertical shock front, brightest at the ground.
			double h = Math.min(th * 1.6 + r * 0.02, maxRadius * 0.12 + 1);
			for (int i = 0; i < segs; i++) {
				double a0 = MathHelper.TAU * i / segs, a1 = MathHelper.TAU * (i + 1) / segs;
				double x0 = Math.cos(a0) * r, z0 = Math.sin(a0) * r, x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
				FxDraw.vertex(vc, m, c.x + x0, c.y, c.z + z0, hot, alpha * 0.6F);
				FxDraw.vertex(vc, m, c.x + x1, c.y, c.z + z1, hot, alpha * 0.6F);
				FxDraw.vertex(vc, m, c.x + x1, c.y + h, c.z + z1, color, 0.0F);
				FxDraw.vertex(vc, m, c.x + x0, c.y + h, c.z + z0, color, 0.0F);
			}
			if (dust) {
				// Kicked-up dust skirt riding just behind the front.
				VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
				FxDraw.ring(shade, m, c.add(0, -0.1, 0), x, z, Math.max(0, r - th * 4), r + th * 0.4, 0x8A7866, 0.0F, 0.32F * (1 - t) * in, 0.0F, segs);
			}
		}
	}

	// ======================================================================= fireball

	/** A boiling fireball: white-hot core, churning lobes that cool through the heat ramp, a soot crust and a lens flare. */
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
			if (r < 0.02) {
				return;
			}
			float t = progress(tickDelta);
			float alpha = (float) Math.pow(1 - t, 1.5);
			Vec3d c = center.subtract(cam);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			// The tint is the caller's palette, pushed through the black-body ramp as it cools.
			int shell = fade(fade(hot, cool, t * 1.4F), heat(0.2F + t * 0.7F), 0.35F);
			float coreA = alpha * (1 - smooth(t * 1.6F));
			VertexConsumer fire = consumers.getBuffer(com.armageddon.client.render.FxSprites.GLOW_LAYER);
			int full = com.armageddon.client.render.FxSprites.FULL_BRIGHT;
			// Outer heat glow, then churning tongues of textured fire, each cooling at its own pace and spinning.
			com.armageddon.client.render.FxSprites.billboard(fire, m, c, basis, r * 2.8, 0, com.armageddon.client.render.FxSprites.GLOW, shell, alpha * 0.45F, full);
			int tongues = r > 3 ? 18 : 9;
			for (int k = 0; k < tongues; k++) {
				Vec3d d = dir(k, 17);
				double h1 = hash(k, 31), h2 = hash(k, 32);
				double boil = 0.18 + 0.5 * h1 + 0.1 * Math.sin(a * 0.21 + k * 1.7);
				Vec3d lc = c.add(d.multiply(r * boil)).add(0, r * t * 0.35 * h2, 0);
				int lcol = fade(hot, heat(0.25F + t * 0.65F + (float) h1 * 0.15F), 0.35F + t * 0.45F);
				double spin = h2 * MathHelper.TAU + a * (k % 2 == 0 ? 0.025 : -0.02);
				com.armageddon.client.render.FxSprites.billboard(fire, m, lc, basis, r * (0.55 + 0.35 * h2), spin,
						com.armageddon.client.render.FxSprites.FIRE + (k & 1), lcol, alpha * (0.75F - (float) h1 * 0.3F), full);
			}
			com.armageddon.client.render.FxSprites.billboard(fire, m, c, basis, r * 1.25, a * 0.01, com.armageddon.client.render.FxSprites.FIRE, 0xFFF4D8, coreA * 0.9F, full);
			com.armageddon.client.render.FxSprites.billboard(fire, m, c, basis, r * 0.9, 0, com.armageddon.client.render.FxSprites.GLOW, 0xFFFFFF, coreA, full);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			streak(vc, m, c, basis, r * 5.5, r * 0.12, fade(shell, 0xFFFFFF, 0.4F), coreA * 0.35F);
			// Soot billowing up out of it as it cools.
			if (t > 0.25F) {
				float soot = smooth((t - 0.25F) / 0.45F) * alpha;
				VertexConsumer smoke = consumers.getBuffer(com.armageddon.client.render.FxSprites.SMOKE_LAYER);
				for (int k = 0; k < 7; k++) {
					Vec3d d = dir(k, 41);
					Vec3d sc = c.add(d.multiply(r * 0.6)).add(0, r * (0.3 + t * 0.9), 0);
					com.armageddon.client.render.FxSprites.billboard(smoke, m, sc, basis, r * (0.8 + 0.3 * hash(k, 42)), hash(k, 43) * 6,
							com.armageddon.client.render.FxSprites.SMOKE + k % 6, 0x2E2624, soot * 0.55F, com.armageddon.client.render.FxSprites.light(sc, 0.4F * (1 - t)));
				}
			}
		}
	}

	// ======================================================================= mushroom cloud

	/**
	 * A mushroom cloud built from a volumetric shell of lit smoke billows (a rolling toroidal cap, a stem,
	 * a ground surge and a condensation collar) plus hundreds of big emissive smoke particles. The underside
	 * glows with the fireball while it is young, then everything cools to sunlit grey and drifts apart.
	 */
	public static class MushroomCloud extends FxEffect {
		private static final int GROW = 150;
		private final Vec3d ground;
		private final double scale;
		private final double height;
		private final Random random = Random.create();

		public MushroomCloud(Vec3d ground, double yieldRadius) {
			super(420);
			this.ground = ground;
			this.scale = yieldRadius;
			this.height = Math.min(220, yieldRadius * 4.2 + 20);
		}

		private double capHeight(float t) {
			return height * (1 - Math.exp(-t / 38.0));
		}

		private double capRadius(float a) {
			return scale * (0.5 + 0.9 * Math.min(1.0, a / 70.0) + 0.25 * Math.max(0, (a - 70) / 300.0));
		}

		private double tube(float a) {
			return scale * 0.45 * (0.75 + 0.45 * Math.min(1.0, a / 60.0));
		}

		@Override
		public boolean tick(ClientWorld world) {
			SpriteProvider fire = ModParticleFactories.fireSprites;
			SpriteProvider smoke = ModParticleFactories.smokeSprites;
			if (fire == null || smoke == null || age >= GROW) {
				return super.tick(world);
			}
			float size = (float) Math.max(0.6, scale / 9.0);
			double capY = ground.y + capHeight(age);
			double rise = height / 38.0 * Math.exp(-age / 38.0);
			double capR = capRadius(age);
			double tube = tube(age);

			// Cap: points on a torus, velocities rolling outward-over-top.
			int capCount = age < 100 ? 6 : 2;
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
				for (int i = 0; i < 2; i++) {
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
				for (int i = 0; i < 5; i++) {
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
				for (int i = 0; i < 12; i++) {
					double a = MathHelper.TAU * i / 12 + random.nextDouble() * 0.2;
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
			float a = age + tickDelta;
			float t = progress(tickDelta);
			// Fades in over the first ticks, holds, then thins away at the end of its life.
			float body = smooth(a / 10.0F) * (1 - smooth((t - 0.6F) / 0.4F));
			if (body <= 0.003F) {
				return;
			}
			double capY = ground.y + capHeight(a);
			double capR = capRadius(a);
			double tube = tube(a);
			// 0 = fresh and burning, 1 = cooled smoke.
			float cool = smooth(a / 110.0F);
			Vec3d g = ground.subtract(cam);
			Vec3d capC = new Vec3d(ground.x, capY, ground.z).subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			int segs = lod(capC, capR + tube, 6, 12);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);

			java.util.List<Puff> puffs = new java.util.ArrayList<>(160);
			int litTop = fade(0xFFC890, 0xC9BFB4, cool);
			int litBottom = fade(0xFF6A22, 0x3E3633, cool);
			// The cap: billows on a torus that roll outward over the top.
			int nu = 18, nv = 5;
			for (int iu = 0; iu < nu; iu++) {
				for (int iv = 0; iv < nv; iv++) {
					int id = iu * nv + iv;
					double u = MathHelper.TAU * iu / nu + hash(id, 3) * 0.3 + a * 0.0015;
					double v = MathHelper.TAU * iv / nv + a * 0.035 + hash(id, 4);
					double ring = capR + tube * Math.cos(v);
					Vec3d p = capC.add(Math.cos(u) * ring, tube * Math.sin(v) * 0.8, Math.sin(u) * ring);
					double pr = tube * (0.75 + 0.35 * hash(id, 5));
					// Billows on top catch the sky; the underside reflects the fireball until it cools.
					float up = (float) (Math.sin(v) * 0.5 + 0.5);
					int top = fade(litBottom, litTop, 0.55F + up * 0.45F);
					int bottom = fade(litBottom, litTop, up * 0.35F);
					puffs.add(new Puff(p, pr, top, bottom, 0.62F * body));
				}
			}
			// A crown of billows over the middle so the cap reads as one dome.
			for (int k = 0; k < 7; k++) {
				double ang = MathHelper.TAU * k / 7 + a * 0.002;
				double rr = k == 0 ? 0 : capR * 0.55;
				Vec3d p = capC.add(Math.cos(ang) * rr, tube * 0.55, Math.sin(ang) * rr);
				puffs.add(new Puff(p, tube * 1.05, litTop, fade(litBottom, litTop, 0.4F), 0.55F * body));
			}
			// The stem: a twisting column, dusty at the base, lighter as it climbs.
			double stemTop = Math.max(0, capY - ground.y - tube * 0.5);
			int levels = 10;
			for (int l = 0; l < levels; l++) {
				double f = (l + 0.5) / levels;
				double y = stemTop * f;
				double sr = scale * (0.22 + 0.22 * (1 - f)) * (0.8 + 0.4 * Math.min(1.0, a / 50.0));
				for (int k = 0; k < 3; k++) {
					double ang = MathHelper.TAU * k / 3 + f * 3 + a * 0.01;
					Vec3d p = g.add(Math.cos(ang) * sr * 0.45, y, Math.sin(ang) * sr * 0.45);
					int top = fade(fade(0xFF9A50, 0xA89C90, cool), 0xB8AEA4, (float) f * 0.4F);
					int bottom = fade(fade(0xC0501A, 0x4A403C, cool), 0x6A5E54, (float) f * 0.3F);
					puffs.add(new Puff(p, sr * 0.9, top, bottom, 0.5F * body));
				}
			}
			// Ground surge: a ring of dust billowing outward.
			double surge = scale * (0.8 + 3.2 * (1 - Math.exp(-a / 45.0)));
			float surgeA = 0.5F * body * (1 - smooth(a / 260.0F));
			for (int k = 0; k < 22; k++) {
				double ang = MathHelper.TAU * k / 22 + hash(k, 9) * 0.25;
				double rr = surge * (0.9 + 0.15 * hash(k, 10));
				double pr = scale * (0.45 + 0.25 * hash(k, 11));
				// Lifted so the billow sits on the ground instead of being sliced by it.
				Vec3d p = g.add(Math.cos(ang) * rr, pr * 0.6, Math.sin(ang) * rr);
				puffs.add(new Puff(p, pr, 0xB09A84, 0x5A4E44, surgeA));
			}
			drawPuffs(puffs, shade, m, cb, segs);
			// Condensation (Wilson) collar: a thin white skirt that flashes into being and evaporates.
			if (a > 14 && a < 70) {
				float w = smooth((a - 14) / 8.0F) * (1 - smooth((a - 38) / 32.0F));
				double wy = capHeight(a) * 0.58;
				double wr = scale * (1.3 + (a - 14) * 0.05);
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				FxDraw.ring(shade, m, g.add(0, wy, 0), x, z, wr * 0.55, wr, 0xEEF4FF, 0.0F, 0.42F * w, 0.0F, 64);
				FxDraw.ring(shade, m, g.add(0, wy + scale * 0.35, 0), x, z, wr * 0.4, wr * 0.8, 0xFFFFFF, 0.0F, 0.25F * w, 0.0F, 48);
			}

			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			// Fire still churning inside the cap, glowing through the smoke.
			float burn = body * (1 - smooth(a / 120.0F));
			if (burn > 0.01F) {
				FxDraw.sphere(glow, m, capC, capR * 0.9, Vec3d.ZERO, heat(0.3F + cool * 0.4F), burn * 0.2F, burn * 0.05F, 10, 16);
				FxDraw.ring(glow, m, capC.add(0, -tube * 0.5, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), capR * 0.3, capR + tube,
						0xFF7A2A, 0.0F, burn * 0.35F, 0.0F, 48);
				// Lightning crackling through the young cloud.
				int roll = (int) (a / 3);
				if (a < 90 && hash(roll, 31) < 0.45) {
					Vec3d s0 = capC.add((hash(roll, 32) - 0.5) * capR * 1.6, (hash(roll, 33) - 0.3) * tube, (hash(roll, 34) - 0.5) * capR * 1.6);
					Vec3d s1 = s0.add((hash(roll, 35) - 0.5) * capR, -tube * (0.5 + hash(roll, 36)), (hash(roll, 37) - 0.5) * capR);
					bolt(glow, m, s0, s1, Math.max(0.15, scale * 0.02), 0xC8B8FF, burn, roll);
				}
			}
			// Inner glow of the rising fireball inside the cap.
			if (t < 0.35F) {
				float k = (1 - t / 0.35F);
				VertexConsumer nd = consumers.getBuffer(FxRenderLayers.GLOW_NO_DEPTH);
				FxDraw.disc(nd, m, capC, cb[0], cb[1], scale * 1.6, 0xFF8A3A, k * 0.45F, 0.0F, 32);
				FxDraw.disc(nd, m, capC, cb[0], cb[1], scale * 0.7, 0xFFE8B0, k * 0.5F, 0.0F, 24);
			}
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
			float a = age + tickDelta;
			float pulse = 0.6F + 0.4F * MathHelper.sin(a * (0.4F + t * 1.2F));
			Vec3d c = pos.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, c, c.add(0, 320, 0), Vec3d.ZERO, 0.12 + t * 0.2, 0xFF2A1A, 0.8F * pulse);
			// Lock-on pips climbing the targeting beam.
			for (int k = 0; k < 3; k++) {
				double h = ((a * 2.2 + k * 40) % 120);
				FxDraw.ring(vc, m, c.add(0, h, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 0.4, 1.0, 0xFF5544, 0.0F, pulse * (1 - (float) h / 120), 0.0F, 24);
			}
			double r = 7.0 * (1 - t) + 1.2;
			Vec3d g = c.add(0, 0.08, 0);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			FxDraw.ring(vc, m, g, x, z, r - 0.25, r + 0.25, 0xFF3322, 0.0F, pulse, 0.0F, 64);
			FxDraw.ring(vc, m, g, x, z, 4.5, 4.8, 0xFF5544, 0.0F, 0.6F, 0.0F, 64);
			// Rotating dashed outer bezel.
			double spin = a * 0.03;
			for (int i = 0; i < 24; i++) {
				if ((i & 1) == 1) {
					continue;
				}
				double a0 = spin + MathHelper.TAU * i / 24, a1 = spin + MathHelper.TAU * (i + 0.8) / 24;
				FxDraw.ribbon(vc, m, g.add(Math.cos(a0) * 9, 0, Math.sin(a0) * 9), g.add(Math.cos(a1) * 9, 0, Math.sin(a1) * 9),
						new Vec3d(0, 50, 0), 0.3, 0xFF6655, 0.7F * pulse, 0.7F * pulse);
			}
			// Radar sweep.
			double sw = a * 0.22;
			for (int k = 0; k < 6; k++) {
				double aa = sw - k * 0.08;
				FxDraw.ribbon(vc, m, g, g.add(Math.cos(aa) * 8.6, 0, Math.sin(aa) * 8.6), new Vec3d(0, 50, 0), 0.5, 0xFF3322, 0.6F * (1 - k / 6.0F), 0.0F);
			}
			// Crosshair ticks.
			for (int i = 0; i < 4; i++) {
				double aa = MathHelper.HALF_PI * i + a * 0.05;
				Vec3d d = new Vec3d(Math.cos(aa), 0, Math.sin(aa));
				FxDraw.ribbon(vc, m, g.add(d.multiply(5.2)), g.add(d.multiply(7.5)), Vec3d.ZERO, 0.25, 0xFF3322, pulse, pulse);
			}
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, g.add(0, 0.6, 0), cb[0], cb[1], 1.6, 0xFF7A60, pulse * 0.8F, 0.0F, 12);
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
			float age2 = age + tickDelta;
			float in = MathHelper.clamp(age2 / 4.0F, 0, 1);
			float alpha = in * (1 - t * t);
			double w = radius * 2 * (0.4 + 0.6 * in) * (1 + 0.08 * MathHelper.sin(age2 * 2.1F));
			Vec3d base = pos.subtract(cam).add(0, -2, 0);
			Vec3d top = base.add(0, 400, 0);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, base, top, Vec3d.ZERO, w, 0x8FE6FF, alpha);
			FxDraw.beam(vc, m, base, top, Vec3d.ZERO, w * 0.35, 0xFFFFFF, alpha);
			// Twin energy helices wound round the column.
			for (int s = 0; s < 2; s++) {
				Vec3d prev = null;
				for (int i = 0; i <= 40; i++) {
					double h = i * 4.0;
					double ang = s * Math.PI + h * 0.06 - age2 * 0.5;
					double rr = w * 0.75;
					Vec3d p = base.add(Math.cos(ang) * rr, h, Math.sin(ang) * rr);
					if (prev != null) {
						float k = alpha * (1 - i / 40.0F);
						FxDraw.ribbon(vc, m, prev, p, Vec3d.ZERO, Math.max(0.15, w * 0.08), 0xC8F6FF, k, k);
					}
					prev = p;
				}
			}
			// Heat rings rising off the impact.
			for (int k = 0; k < 4; k++) {
				double h = ((age2 * 3 + k * 25) % 100);
				double rr = w * (1.3 + h / 60.0);
				FxDraw.ring(vc, m, base.add(0, h, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), rr * 0.75, rr, 0x8FE6FF, 0.0F, alpha * (1 - (float) h / 100) * 0.7F, 0.0F, 40);
			}
			Vec3d g = pos.subtract(cam).add(0, 0.2, 0);
			FxDraw.disc(vc, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius * 4, 0x8FE6FF, alpha * 0.8F, 0.0F, 48);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, g.add(0, 1, 0), basis[0], basis[1], radius * 3.5, 0xFFFFFF, alpha * 0.7F, 0.0F, 32);
			streak(vc, m, g.add(0, 1, 0), basis, radius * 14, radius * 0.25, 0x8FE6FF, alpha * 0.45F);
			// Scorched, glassed ground left behind.
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			FxDraw.disc(shade, m, g.add(0, -0.1, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius * (2 + 2 * t), 0x1A1614, 0.55F * smooth(t * 3) * (1 - t), 0.0F, 40);
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
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			if (a < IMPLODE) {
				float t = a / IMPLODE;
				double r = radius * 1.4 * (1 - t * t) + 0.5;
				FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, 0xB060FF, 0.05F + t * 0.2F, 0.6F * t, 16, 24);
				// Tilted accretion rings spinning faster and tighter as everything falls in.
				for (int k = 0; k < 3; k++) {
					Vec3d n = dir(k, 61);
					Vec3d[] b = ArsenalEffects.basis(n);
					double spin = a * (0.2 + k * 0.08) * (1 + t * 3);
					Vec3d u = b[0].multiply(Math.cos(spin)).add(b[1].multiply(Math.sin(spin)));
					Vec3d v = b[1].multiply(Math.cos(spin)).subtract(b[0].multiply(Math.sin(spin)));
					double rr = r * (1.1 + k * 0.25);
					FxDraw.ring(vc, m, c, u, v, rr * 0.82, rr, k == 1 ? 0xFFC8FF : 0xA050FF, 0.0F, 0.55F * t, 0.0F, 48);
				}
				// A hole of anti-light opening at the centre.
				VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
				FxDraw.sphere(shade, m, c, 0.4 + t * radius * 0.25, Vec3d.ZERO, 0x05000C, 0.9F * t, 0.4F * t, 12, 18);
				FxDraw.disc(vc, m, c, basis[0], basis[1], 1.5 + t * 4, 0xFFFFFF, t, 0.0F, 24);
				streak(vc, m, c, basis, 6 + t * radius * 2, 0.3 + t * 0.4, 0xD0A0FF, t * 0.6F);
			} else {
				float t = MathHelper.clamp((a - IMPLODE) / 50.0F, 0, 1);
				double r = radius * (1 - Math.pow(1 - Math.min(1, t * 2.5), 3));
				float alpha = (1 - t);
				FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, fade(0xFFFFFF, 0xC070FF, t * 2), alpha * 0.4F, alpha, 18, 28);
				FxDraw.sphere(vc, m, c, r * 1.05, Vec3d.ZERO, 0x7030FF, 0.0F, alpha * 0.6F, 18, 28);
				FxDraw.sphere(vc, m, c, r * 0.7, Vec3d.ZERO, 0xFF60C0, alpha * 0.25F, alpha * 0.4F, 12, 18);
				// Annihilation rays.
				for (int k = 0; k < 14; k++) {
					Vec3d d = dir(k, 71);
					double len = r * (1.2 + 0.8 * hash(k, 72));
					FxDraw.ribbon(vc, m, c.add(d.multiply(r * 0.3)), c.add(d.multiply(len)), Vec3d.ZERO, radius * 0.06 * alpha, 0xF0D0FF, alpha * 0.8F, 0.0F);
				}
				FxDraw.ring(vc, m, c, basis[0], basis[1], r * 1.1, r * 1.25, 0xFFFFFF, 0.0F, alpha * 0.7F, 0.0F, 64);
			}
		}
	}
}
