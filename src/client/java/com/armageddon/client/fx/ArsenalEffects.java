package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Visual vocabulary for the arsenal expansion (FxType 26+). */
public final class ArsenalEffects {
	private ArsenalEffects() {
	}

	static int rainbow(float t) {
		return MathHelper.hsvToRgb(((t % 1.0F) + 1.0F) % 1.0F, 0.85F, 1.0F);
	}

	static Vec3d[] basis(Vec3d normal) {
		Vec3d n = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : normal.normalize();
		Vec3d a = Math.abs(n.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
		Vec3d u = n.crossProduct(a).normalize();
		Vec3d v = n.crossProduct(u).normalize();
		return new Vec3d[]{u, v, n};
	}

	/** Fade in over the first {@code in} ticks, out over the last {@code out}. */
	static float envelope(float age, int life, float in, float out) {
		float a = in > 0 ? Math.min(1.0F, age / in) : 1.0F;
		float b = out > 0 ? Math.min(1.0F, (life - age) / out) : 1.0F;
		return MathHelper.clamp(Math.min(a, b), 0.0F, 1.0F);
	}

	// ============================================================== orb (stars, voids, pinpricks)

	public static class Orb extends FxEffect {
		private final Vec3d c;
		private final float r0, r1;
		private final int color, mode;

		public Orb(Vec3d c, float r0, float r1, int color, int mode, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r0 = r0;
			this.r1 = r1;
			this.color = color;
			this.mode = mode;
		}

		private double radius(float a) {
			float t = mode == 2 ? a / lifetime : Math.min(1.0F, a / (lifetime * 0.25F));
			float e = 1 - (1 - t) * (1 - t);
			return MathHelper.lerp(e, r0, r1);
		}

		@Override
		public boolean tick(ClientWorld world) {
			double r = radius(age);
			if (mode == 0 && age % 2 == 0) {
				for (int i = 0; i < 4; i++) {
					Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize();
					Vec3d p = c.add(d.multiply(r * 1.05));
					world.addImportantParticle(ModParticles.EMBER, true, p.x, p.y, p.z, d.x * 0.2, d.y * 0.2, d.z * 0.2);
				}
			}
			if (mode == 1 && age % 2 == 0) {
				for (int i = 0; i < 4; i++) {
					Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian() * 0.3, world.random.nextGaussian()).normalize();
					Vec3d p = c.add(d.multiply(r * 4));
					Vec3d v = c.subtract(p).multiply(0.05);
					world.addImportantParticle(ModParticles.VOID_SPARK, true, p.x, p.y, p.z, v.x, v.y, v.z);
				}
			}
			if (age == 0 || (mode == 0 && age % 20 == 0)) {
				ClientFx.light(c, mode == 1 ? 0.3F : 1.0F, color == 0 ? 0xFFFFFF : color);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double r = radius(a);
			float env = envelope(a, lifetime, 4, mode == 2 ? 2 : 10);
			Vec3d p = c.subtract(cam);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			double pulse = 1 + 0.04 * Math.sin(a * 0.5);
			if (mode == 1) {
				VertexConsumer v = consumers.getBuffer(FxRenderLayers.VOID);
				FxDraw.sphere(v, m, p, r * env, Vec3d.ZERO, 0x000000, 1, 1, 12, 20);
				FxDraw.ring(glow, m, p, cb[0], cb[1], r * 0.98, r * 1.6 * pulse, color, 0.9F * env, 0.5F * env, 0.0F, 64);
				FxDraw.ring(glow, m, p, new Vec3d(Math.cos(a * 0.05), 0, Math.sin(a * 0.05)), new Vec3d(-Math.sin(a * 0.05) * 0.3, 0.95, Math.cos(a * 0.05) * 0.3).normalize(),
						r * 1.4, r * 2.6, color, 0.0F, 0.6F * env, 0.0F, 64);
				return;
			}
			int core = mode == 2 ? 0xFFFFFF : Effects.fade(color, 0xFFFFFF, 0.6F);
			FxDraw.sphere(glow, m, p, r * pulse, Vec3d.ZERO, core, 0.9F * env, 0.3F * env, 10, 18);
			FxDraw.disc(glow, m, p, cb[0], cb[1], r * 2.4, color, 0.7F * env, 0.0F, 48);
			FxDraw.disc(glow, m, p, cb[0], cb[1], r * 5.0, color, 0.25F * env, 0.0F, 48);
			// Corona: slowly rotating flares.
			int n = 12;
			for (int i = 0; i < n; i++) {
				double ang = i * MathHelper.TAU / n + a * 0.01 * (i % 2 == 0 ? 1 : -1);
				double len = r * (2.2 + 0.8 * Math.sin(a * 0.13 + i * 1.7));
				Vec3d dir = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
				FxDraw.ribbon(glow, m, p.add(dir.multiply(r * 0.8)), p.add(dir.multiply(len)), Vec3d.ZERO, r * 0.25, color, 0.5F * env, 0.0F);
			}
		}
	}

	// ============================================================== bubble

	public static class Bubble extends FxEffect {
		private final Vec3d c;
		private final float r;
		private final int color;

		public Bubble(Vec3d c, float r, int color, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			this.color = color;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == lifetime - 1) {
				for (int i = 0; i < 80; i++) {
					Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize();
					Vec3d p = c.add(d.multiply(r));
					world.addParticle(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, d.x * 0.1, d.y * 0.1, d.z * 0.1);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float grow = Math.min(1.0F, a / 8.0F);
			double rr = r * (1 - Math.pow(1 - grow, 3)) * (1 + 0.03 * Math.sin(a * 0.4));
			float env = envelope(a, lifetime, 2, 3);
			Vec3d p = c.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.sphere(vc, m, p, rr, Vec3d.ZERO, color, 0.04F * env, 0.5F * env, 14, 24);
			FxDraw.sphere(vc, m, p, rr * 0.995, Vec3d.ZERO, rainbow(a * 0.01F), 0.0F, 0.25F * env, 14, 24);
		}
	}

	// ============================================================== cage (hex lattice)

	public static class Cage extends FxEffect {
		private final Vec3d c;
		private final float r;
		private final int color;

		public Cage(Vec3d c, float r, int color, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 6, 6);
			double rr = r * (0.9 + 0.1 * Math.min(1, a / 6));
			Vec3d p = c.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.sphere(vc, m, p, rr, Vec3d.ZERO, color, 0.03F * env, 0.25F * env, 10, 18);
			int lat = 6, lon = 12;
			double w = Math.max(0.05, r * 0.012);
			for (int i = 1; i < lat; i++) {
				double t = Math.PI * i / lat - Math.PI / 2;
				for (int j = 0; j < lon; j++) {
					double off = (i % 2) * Math.PI / lon;
					Vec3d q0 = unit(t, MathHelper.TAU * j / lon + off).multiply(rr);
					Vec3d q1 = unit(t, MathHelper.TAU * (j + 1) / lon + off).multiply(rr);
					Vec3d q2 = unit(t + Math.PI / lat, MathHelper.TAU * j / lon + off + Math.PI / lon).multiply(rr);
					float flick = 0.6F + 0.4F * MathHelper.sin(a * 0.3F + i * 1.3F + j);
					FxDraw.ribbon(vc, m, p.add(q0), p.add(q1), Vec3d.ZERO, w, color, env * flick, env * flick);
					if (i < lat - 1) {
						FxDraw.ribbon(vc, m, p.add(q0), p.add(q2), Vec3d.ZERO, w, color, env * flick * 0.7F, env * flick * 0.7F);
					}
				}
			}
		}

		static Vec3d unit(double theta, double phi) {
			double ct = Math.cos(theta);
			return new Vec3d(ct * Math.cos(phi), Math.sin(theta), ct * Math.sin(phi));
		}
	}

	// ============================================================== grid (targeting / sky panels / wireframe floor)

	public static class Grid extends FxEffect {
		private final Vec3d c, normal;
		private final float r;
		private final int color, mode;

		public Grid(Vec3d c, Vec3d normal, float r, int color, int mode, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.normal = normal;
			this.r = r;
			this.color = color;
			this.mode = mode;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 5, 8);
			Vec3d p = c.subtract(cam);
			Vec3d[] b = basis(normal);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			int lines = MathHelper.clamp((int) (r / 1.5F), 4, 40);
			double step = r * 2.0 / lines;
			double w = Math.max(0.04, r * 0.01);
			float scan = (a * 0.04F) % 1.0F;
			for (int i = 0; i <= lines; i++) {
				double o = -r + i * step;
				double half = Math.sqrt(Math.max(0, r * r - o * o));
				float k = mode == 1 ? 0.5F + 0.5F * MathHelper.sin(a * 0.3F + i) : 1.0F;
				float hot = mode == 2 ? (float) Math.max(0, 1 - Math.abs((i / (double) lines) - scan) * 8) : 0;
				int col = hot > 0 ? Effects.fade(color, 0xFFFFFF, hot) : color;
				FxDraw.ribbon(vc, m, p.add(b[0].multiply(o)).add(b[1].multiply(-half)), p.add(b[0].multiply(o)).add(b[1].multiply(half)), Vec3d.ZERO, w, col, 0.6F * env * k, 0.6F * env * k);
				FxDraw.ribbon(vc, m, p.add(b[1].multiply(o)).add(b[0].multiply(-half)), p.add(b[1].multiply(o)).add(b[0].multiply(half)), Vec3d.ZERO, w, col, 0.6F * env * k, 0.6F * env * k);
			}
			FxDraw.ring(vc, m, p, b[0], b[1], r * 0.97, r * 1.03, color, 0, env, 0, 64);
			FxDraw.disc(vc, m, p, b[0], b[1], r, color, 0.12F * env, 0.02F * env, 48);
			if (mode == 0) {
				double pr = r * (1 - ((a * 0.03) % 1.0));
				FxDraw.ring(vc, m, p, b[0], b[1], pr * 0.95, pr, 0xFFFFFF, 0, 0.8F * env, 0, 48);
			}
		}
	}

	// ============================================================== portal / rift

	public static class Portal extends FxEffect {
		private final Vec3d c, normal;
		private final float r;
		private final int color;

		public Portal(Vec3d c, Vec3d normal, float r, int color, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.normal = normal;
			this.r = r;
			this.color = color;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 2 == 0) {
				Vec3d[] b = basis(normal);
				for (int i = 0; i < 6; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					Vec3d p = c.add(b[0].multiply(Math.cos(ang) * r * 1.2)).add(b[1].multiply(Math.sin(ang) * r * 1.2));
					Vec3d v = c.subtract(p).multiply(0.05);
					world.addImportantParticle(ParticleTypes.REVERSE_PORTAL, true, p.x, p.y, p.z, v.x, v.y, v.z);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 20, 20);
			double rr = r * env;
			Vec3d p = c.subtract(cam);
			Vec3d[] b = basis(normal);
			VertexConsumer v = consumers.getBuffer(FxRenderLayers.VOID);
			FxDraw.disc(v, m, p, b[0], b[1], rr * 0.85, 0x050008, 1, 1, 32);
			VertexConsumer g = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.ring(g, m, p, b[0], b[1], rr * 0.8, rr * 1.25, color, 0.2F * env, 1.0F * env, 0.0F, 64);
			FxDraw.ring(g, m, p, b[0], b[1], rr * 1.2, rr * 2.2, color, 0.0F, 0.3F * env, 0.0F, 64);
			int arms = 5;
			for (int k = 0; k < arms; k++) {
				Vec3d prev = null;
				for (int s = 0; s <= 16; s++) {
					double t = s / 16.0;
					double ang = k * MathHelper.TAU / arms + t * 3.0 + a * 0.08;
					double rad = rr * (0.85 + t * 0.9);
					Vec3d q = p.add(b[0].multiply(Math.cos(ang) * rad)).add(b[1].multiply(Math.sin(ang) * rad));
					if (prev != null) {
						FxDraw.ribbon(g, m, prev, q, Vec3d.ZERO, rr * 0.08 * (1 - t), Effects.fade(color, 0xFFFFFF, (float) (1 - t) * 0.5F), (float) (1 - t) * env, (float) (1 - t) * env * 0.8F);
					}
					prev = q;
				}
			}
		}
	}

	// ============================================================== sky crack

	public static class SkyCrack extends FxEffect {
		private final List<Vec3d[]> segments = new ArrayList<>();
		private final int color;
		private final float intensity;

		public SkyCrack(Vec3d c, float intensity, int color, int style, int seed, int life) {
			super(Math.max(2, life));
			this.color = color;
			this.intensity = intensity;
			Random rnd = Random.create(seed);
			int roots = 4 + style * 3;
			for (int k = 0; k < roots; k++) {
				double ang = rnd.nextDouble() * MathHelper.TAU;
				Vec3d p = c.add(Math.cos(ang) * 30, 160 + rnd.nextDouble() * 40, Math.sin(ang) * 30);
				Vec3d dir = new Vec3d(Math.cos(ang), rnd.nextGaussian() * 0.1, Math.sin(ang));
				grow(rnd, p, dir, 40 + style * 15, 0);
			}
		}

		private void grow(Random rnd, Vec3d p, Vec3d dir, int steps, int depth) {
			for (int i = 0; i < steps; i++) {
				dir = dir.add(rnd.nextGaussian() * 0.35, rnd.nextGaussian() * 0.1, rnd.nextGaussian() * 0.35).normalize();
				Vec3d q = p.add(dir.multiply(4 + rnd.nextDouble() * 4));
				segments.add(new Vec3d[]{p, q, new Vec3d(depth, segments.size(), 0)});
				p = q;
				if (depth < 2 && rnd.nextFloat() < 0.12F) {
					grow(rnd, p, dir.add(rnd.nextGaussian(), 0, rnd.nextGaussian()).normalize(), steps / 3, depth + 1);
				}
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 0, 40);
			int reveal = (int) Math.min(segments.size(), a * 12);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW_NO_DEPTH);
			for (int i = 0; i < reveal; i++) {
				Vec3d[] s = segments.get(i);
				double w = (3.0 - s[2].x) * 0.9 * intensity;
				float flick = 0.75F + 0.25F * MathHelper.sin(a * 0.7F + i);
				FxDraw.beam(vc, m, s[0].subtract(cam), s[1].subtract(cam), Vec3d.ZERO, w, color, env * flick * intensity);
			}
		}
	}

	// ============================================================== crystal shard

	public static class Crystal extends FxEffect {
		private final Vec3d base, tip;
		private final float width;
		private final int color;

		public Crystal(Vec3d base, Vec3d tip, float width, int color, int life) {
			super(Math.max(2, life));
			this.base = base;
			this.tip = tip;
			this.width = width;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float grow = Math.min(1.0F, a / 6.0F);
			float env = envelope(a, lifetime, 0, 10);
			Vec3d b = base.subtract(cam);
			Vec3d t = b.add(tip.subtract(base).multiply(grow));
			Vec3d axis = t.subtract(b);
			Vec3d[] bs = basis(axis);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d mid = b.add(axis.multiply(0.78));
			for (int i = 0; i < 6; i++) {
				double a0 = i * MathHelper.TAU / 6, a1 = (i + 1) * MathHelper.TAU / 6;
				Vec3d o0 = bs[0].multiply(Math.cos(a0) * width).add(bs[1].multiply(Math.sin(a0) * width));
				Vec3d o1 = bs[0].multiply(Math.cos(a1) * width).add(bs[1].multiply(Math.sin(a1) * width));
				float shade = 0.35F + 0.25F * (float) Math.abs(Math.cos(a0 + a * 0.05));
				quad(vc, m, b.add(o0.multiply(0.6)), b.add(o1.multiply(0.6)), mid.add(o1), mid.add(o0), color, shade * env);
				quad(vc, m, mid.add(o0), mid.add(o1), t, t, Effects.fade(color, 0xFFFFFF, 0.4F), shade * env);
			}
			FxDraw.ribbon(vc, m, b, t, Vec3d.ZERO, width * 0.4, 0xFFFFFF, 0.5F * env, 0.9F * env);
		}

		static void quad(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb, float alpha) {
			FxDraw.vertex(vc, m, a.x, a.y, a.z, rgb, alpha);
			FxDraw.vertex(vc, m, b.x, b.y, b.z, rgb, alpha);
			FxDraw.vertex(vc, m, c.x, c.y, c.z, rgb, alpha);
			FxDraw.vertex(vc, m, d.x, d.y, d.z, rgb, alpha);
		}
	}

	// ============================================================== paint splats

	public static class Paint extends FxEffect {
		final Vec3d c;
		final float r;
		private final List<double[]> splats = new ArrayList<>();
		boolean washed;

		public Paint(ClientWorld world, Vec3d c, float r, int color, int seed, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			Random rnd = Random.create(seed);
			for (int i = 0; i < 90; i++) {
				double ang = rnd.nextDouble() * MathHelper.TAU;
				double d = Math.sqrt(rnd.nextDouble()) * r;
				double x = c.x + Math.cos(ang) * d, z = c.z + Math.sin(ang) * d;
				int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
				if (Math.abs(y - c.y) > r) {
					continue;
				}
				int col = color == 0 ? rainbow(rnd.nextFloat()) : color;
				splats.add(new double[]{x, y + 0.02 + i * 0.0004, z, 0.5 + rnd.nextDouble() * 1.6, col, rnd.nextDouble() * MathHelper.TAU});
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			return !washed && super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 3, 60);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.SHADE);
			for (double[] s : splats) {
				Vec3d p = new Vec3d(s[0], s[1], s[2]).subtract(cam);
				double rad = s[3] * Math.min(1.0, a / 4.0);
				int col = (int) s[4];
				int seg = 9;
				for (int i = 0; i < seg; i++) {
					double a0 = s[5] + i * MathHelper.TAU / seg, a1 = s[5] + (i + 1) * MathHelper.TAU / seg;
					double r0 = rad * (0.75 + 0.25 * Math.sin(i * 2.3 + s[5])), r1 = rad * (0.75 + 0.25 * Math.sin((i + 1) * 2.3 + s[5]));
					FxDraw.vertex(vc, m, p.x, p.y, p.z, col, 0.85F * env);
					FxDraw.vertex(vc, m, p.x, p.y, p.z, col, 0.85F * env);
					FxDraw.vertex(vc, m, p.x + Math.cos(a1) * r1, p.y, p.z + Math.sin(a1) * r1, col, 0.85F * env);
					FxDraw.vertex(vc, m, p.x + Math.cos(a0) * r0, p.y, p.z + Math.sin(a0) * r0, col, 0.85F * env);
				}
			}
		}
	}

	// ============================================================== fireworks

	public static void firework(ClientWorld world, Vec3d c, float size, int shape, int color) {
		if (shape <= 2) {
			FireworkExplosionComponent.Type type = switch (shape) {
				case 0 -> FireworkExplosionComponent.Type.STAR;
				case 1 -> FireworkExplosionComponent.Type.BURST;
				default -> size > 1.5F ? FireworkExplosionComponent.Type.LARGE_BALL : FireworkExplosionComponent.Type.SMALL_BALL;
			};
			FireworkExplosionComponent ex = new FireworkExplosionComponent(type, IntList.of(color, rainbow(world.random.nextFloat())),
					IntList.of(0xFFFFFF), true, true);
			world.addFireworkParticle(c.x, c.y, c.z, 0, 0, 0, List.of(ex));
			return;
		}
		// Custom shapes drawn in coloured dust, facing the camera.
		Vec3d[] cb = FxDraw.cameraBasis(ClientFx.cameraYaw(), 0);
		Vector3f col = new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
		DustParticleEffect dust = new DustParticleEffect(col, 2.0F);
		int n = 70;
		for (int i = 0; i < n; i++) {
			double t = i * MathHelper.TAU / n;
			double x, y;
			if (shape == 3) {
				x = 16 * Math.pow(Math.sin(t), 3) / 16.0;
				y = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16.0;
			} else {
				x = Math.cos(t);
				y = Math.sin(t);
			}
			addShapePoint(world, dust, c, cb, x * size * 2, y * size * 2);
		}
		if (shape == 4) {
			for (int i = 0; i < 24; i++) {
				double t = Math.PI * (1.15 + 0.7 * i / 23.0);
				addShapePoint(world, dust, c, cb, Math.cos(t) * size * 1.2, Math.sin(t) * size * 1.2);
			}
			addShapePoint(world, dust, c, cb, -size * 0.7, size * 0.7);
			addShapePoint(world, dust, c, cb, size * 0.7, size * 0.7);
		}
		world.playSound(c.x, c.y, c.z, net.minecraft.sound.SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, net.minecraft.sound.SoundCategory.AMBIENT, 4.0F, 1.0F, false);
	}

	private static void addShapePoint(ClientWorld world, DustParticleEffect dust, Vec3d c, Vec3d[] cb, double x, double y) {
		Vec3d p = c.add(cb[0].multiply(x)).add(cb[1].multiply(y));
		Vec3d v = p.subtract(c).multiply(0.02);
		world.addImportantParticle(dust, true, p.x, p.y, p.z, v.x, v.y, v.z);
		world.addImportantParticle(ParticleTypes.FIREWORK, true, p.x, p.y, p.z, v.x * 0.5, v.y * 0.5 - 0.02, v.z * 0.5);
	}

	// ============================================================== hologram decoys

	public static class Hologram extends FxEffect {
		private final List<double[]> figures = new ArrayList<>();
		private final int color;

		public Hologram(Vec3d c, float r, int color, int seed, int life) {
			super(Math.max(2, life));
			this.color = color;
			Random rnd = Random.create(seed);
			ClientWorld world = MinecraftClient.getInstance().world;
			for (int i = 0; i < 7; i++) {
				double ang = rnd.nextDouble() * MathHelper.TAU;
				double d = 1.5 + rnd.nextDouble() * r;
				double x = c.x + Math.cos(ang) * d, z = c.z + Math.sin(ang) * d;
				double y = world != null ? world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z)) : c.y;
				figures.add(new double[]{x, y, z, rnd.nextDouble() * 360, rnd.nextInt(3)});
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 6, 10);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			int i = 0;
			for (double[] f : figures) {
				i++;
				float flick = ((int) (a * 3 + i * 7)) % 13 == 0 ? 0.15F : 1.0F;
				double bob = Math.sin(a * 0.15 + i) * 0.05;
				double turn = Math.toRadians(f[3] + a * 2);
				Vec3d base = new Vec3d(f[0], f[1] + bob, f[2]).subtract(cam);
				int kind = (int) f[4];
				if (kind == 0) {
					box(vc, m, base, turn, 0.25, 0, 0.75, color, env * flick);
					box(vc, m, base, turn, 0.3, 0.75, 1.45, color, env * flick);
					box(vc, m, base, turn, 0.25, 1.45, 1.95, color, env * flick);
				} else if (kind == 1) {
					box(vc, m, base, turn, 0.45, 0, 1.6, Effects.fade(color, 0x40FF40, 0.5F), env * flick);
				} else {
					box(vc, m, base, turn, 0.6, 0, 1.0, Effects.fade(color, 0xFF60A0, 0.5F), env * flick);
				}
				double scan = (a * 0.08 + i * 0.3) % 2.0;
				FxDraw.ring(vc, m, base.add(0, scan, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 0.45, 0.55, 0xFFFFFF, 0, 0.6F * env, 0, 16);
			}
		}

		static void box(VertexConsumer vc, Matrix4f m, Vec3d base, double turn, double half, double y0, double y1, int color, float alpha) {
			Vec3d u = new Vec3d(Math.cos(turn), 0, Math.sin(turn)).multiply(half);
			Vec3d v = new Vec3d(-Math.sin(turn), 0, Math.cos(turn)).multiply(half);
			Vec3d[] corners = {u.add(v), u.subtract(v), u.multiply(-1).subtract(v), v.subtract(u)};
			for (int k = 0; k < 4; k++) {
				Vec3d c0 = base.add(corners[k]), c1 = base.add(corners[(k + 1) % 4]);
				Crystal.quad(vc, m, c0.add(0, y0, 0), c1.add(0, y0, 0), c1.add(0, y1, 0), c0.add(0, y1, 0), color, 0.18F * alpha);
				FxDraw.ribbon(vc, m, c0.add(0, y0, 0), c0.add(0, y1, 0), Vec3d.ZERO, 0.03, color, alpha, alpha);
				FxDraw.ribbon(vc, m, c0.add(0, y1, 0), c1.add(0, y1, 0), Vec3d.ZERO, 0.03, color, alpha, alpha);
			}
		}
	}

	// ============================================================== mirror boundary

	public static class Mirror extends FxEffect {
		private final Vec3d c;
		private final float r;
		private final int color;
		private final Random rnd;

		public Mirror(Vec3d c, float r, int color, int seed, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			this.color = color;
			this.rnd = Random.create(seed);
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int i = 0; i < 6; i++) {
				Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize().multiply(r);
				world.addParticle(ParticleTypes.END_ROD, c.x + d.x, c.y + d.y, c.z + d.z, 0, 0, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 6, 4);
			Vec3d p = c.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.sphere(vc, m, p, r, Vec3d.ZERO, 0xE8E8FF, 0.05F * env, 0.45F * env, 12, 20);
			// Mirror plane sweeping around the vertical axis.
			double ang = a * 0.06;
			Vec3d u = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
			for (int k = 0; k < 8; k++) {
				double y0 = -r + k * r / 4, y1 = y0 + r / 4 - 0.1;
				double w0 = Math.sqrt(Math.max(0, r * r - Math.min(y0 * y0, y1 * y1)));
				double jitter = (rnd.nextFloat() < 0.1F ? rnd.nextGaussian() * 0.6 : 0);
				Crystal.quad(vc, m, p.add(u.multiply(-w0 + jitter)).add(0, y0, 0), p.add(u.multiply(w0 + jitter)).add(0, y0, 0),
						p.add(u.multiply(w0 + jitter)).add(0, y1, 0), p.add(u.multiply(-w0 + jitter)).add(0, y1, 0), color, 0.22F * env);
			}
		}
	}

	// ============================================================== soundwave

	public static class Soundwave extends FxEffect {
		private final Vec3d from, to;
		private final float width, pitch;
		private final int color;

		public Soundwave(Vec3d from, Vec3d to, float width, int color, float pitch) {
			super(14);
			this.from = from;
			this.to = to;
			this.width = width;
			this.color = color;
			this.pitch = pitch;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pt) {
			float a = age + td;
			Vec3d d = to.subtract(from);
			Vec3d[] b = basis(d);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			int rings = (int) (6 + 6 * pitch);
			for (int k = 0; k < rings; k++) {
				double t = (a / lifetime) * 1.4 - k * (0.6 / rings);
				if (t < 0 || t > 1) {
					continue;
				}
				Vec3d p = from.add(d.multiply(t)).subtract(cam);
				double rr = width * (0.4 + t * 1.6);
				float alpha = (float) (1 - t) * 0.9F;
				FxDraw.ring(vc, m, p, b[0], b[1], rr * 0.8, rr, k % 2 == 0 ? color : 0xFFFFFF, 0, alpha, 0, 32);
			}
		}
	}

	// ============================================================== layered blast

	public static void layeredBlast(ClientWorld world, Vec3d c, float size, int color) {
		ClientFx.add(new Effects.Fireball(c, size * 1.2, 6, 26 + (int) size * 2, 0xFFFFFF, color));
		ClientFx.add(new Effects.Ring(c, size * 5, color, 16 + (int) size, Math.max(0.5, size * 0.25), size > 3));
		ClientFx.add(new Effects.Ring(c, size * 2.5, 0xFFFFFF, 10, Math.max(0.3, size * 0.12), false));
		ClientFx.add(new InsaneEffects.Dome(c, size * 3.5, 14 + (int) size, color));
		ClientFx.flash(c, size * 30, Math.min(0.6F, 0.08F * size), Effects.fade(color, 0xFFFFFF, 0.5F));
		ClientFx.shake(c, size * 12, Math.min(2.0F, 0.2F * size));
		ClientFx.light(c, Math.min(1.2F, 0.2F * size), color);
		for (int i = 0; i < 20 + size * 6; i++) {
			Vec3d v = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian() * 0.7 + 0.3, world.random.nextGaussian()).normalize().multiply(0.3 + world.random.nextDouble() * size * 0.15);
			world.addImportantParticle(ModParticles.EMBER, true, c.x, c.y, c.z, v.x, v.y, v.z);
			if (i % 2 == 0) {
				world.addImportantParticle(ModParticles.NUKE_SMOKE, true, c.x, c.y, c.z, v.x * 0.4, v.y * 0.4, v.z * 0.4);
			}
		}
		world.addImportantParticle(ParticleTypes.EXPLOSION_EMITTER, true, c.x, c.y, c.z, 0, 0, 0);
	}

	// ============================================================== wireframe box

	public static class Wirebox extends FxEffect {
		private final Vec3d min, max;
		private final int color;

		public Wirebox(Vec3d min, Vec3d max, int color, int life) {
			super(Math.max(2, life));
			this.min = min;
			this.max = max;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 8, 10);
			Vec3d lo = min.subtract(cam), hi = max.subtract(cam);
			double w = Math.max(0.06, hi.subtract(lo).length() * 0.002);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			double[] xs = {lo.x, hi.x}, ys = {lo.y, hi.y}, zs = {lo.z, hi.z};
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					FxDraw.ribbon(vc, m, new Vec3d(lo.x, ys[i], zs[j]), new Vec3d(hi.x, ys[i], zs[j]), Vec3d.ZERO, w, color, env, env);
					FxDraw.ribbon(vc, m, new Vec3d(xs[i], lo.y, zs[j]), new Vec3d(xs[i], hi.y, zs[j]), Vec3d.ZERO, w, color, env, env);
					FxDraw.ribbon(vc, m, new Vec3d(xs[i], ys[j], lo.z), new Vec3d(xs[i], ys[j], hi.z), Vec3d.ZERO, w, color, env, env);
				}
			}
			// Scanning plane.
			double y = lo.y + (hi.y - lo.y) * ((a * 0.02) % 1.0);
			Crystal.quad(vc, m, new Vec3d(lo.x, y, lo.z), new Vec3d(hi.x, y, lo.z), new Vec3d(hi.x, y, hi.z), new Vec3d(lo.x, y, hi.z), color, 0.12F * env);
		}
	}

	// ============================================================== streak (incoming payload)

	public static class Streak extends FxEffect {
		private final Vec3d from, to;
		private final float width;
		private final int color;

		public Streak(Vec3d from, Vec3d to, float width, int color, int life) {
			super(Math.max(2, life));
			this.from = from;
			this.to = to;
			this.width = width;
			this.color = color;
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d head = from.lerp(to, Math.min(1.0, age / (double) lifetime));
			for (int i = 0; i < 3; i++) {
				world.addImportantParticle(ModParticles.NUKE_SMOKE, true, head.x, head.y, head.z, world.random.nextGaussian() * 0.05, 0.02, world.random.nextGaussian() * 0.05);
			}
			world.addImportantParticle(ModParticles.EMBER, true, head.x, head.y, head.z, 0, 0, 0);
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float t = progress(td);
			Vec3d head = from.lerp(to, t).subtract(cam);
			Vec3d tail = from.lerp(to, Math.max(0, t - 0.25)).subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.ribbon(vc, m, tail, head, Vec3d.ZERO, width * 1.5, color, 0.0F, 0.8F);
			FxDraw.ribbon(vc, m, tail.lerp(head, 0.6), head, Vec3d.ZERO, width * 0.5, 0xFFFFFF, 0.0F, 1.0F);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, head, cb[0], cb[1], width * 2.5, color, 0.9F, 0.0F, 24);
		}
	}

	// ============================================================== spiral

	public static class Spiral extends FxEffect {
		private final Vec3d c;
		private final float r, height;
		private final int color;

		public Spiral(Vec3d c, float r, float height, int color, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			this.height = height;
			this.color = color;
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int i = 0; i < 3; i++) {
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double h = world.random.nextDouble() * height;
				double rr = r * (1 - h / height * 0.5);
				world.addParticle(ModParticles.VOID_SPARK, c.x + Math.cos(ang) * rr, c.y + h, c.z + Math.sin(ang) * rr, -Math.sin(ang) * 0.2, 0.05, Math.cos(ang) * 0.2);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 10, 15);
			Vec3d p = c.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			for (int arm = 0; arm < 3; arm++) {
				Vec3d prev = null;
				for (int s = 0; s <= 40; s++) {
					double t = s / 40.0;
					double ang = arm * MathHelper.TAU / 3 + t * 9 + a * 0.15;
					double rr = r * (1 - t * 0.6);
					Vec3d q = p.add(Math.cos(ang) * rr, t * height, Math.sin(ang) * rr);
					if (prev != null) {
						FxDraw.ribbon(vc, m, prev, q, Vec3d.ZERO, 0.25 + r * 0.03, Effects.fade(color, 0xFFFFFF, (float) t * 0.5F), env * 0.7F, env * 0.7F);
					}
					prev = q;
				}
			}
		}
	}

	// ============================================================== charge-up

	public static class Charge extends FxEffect {
		private final int entityId;
		private Vec3d at;
		private final float size;
		private final int color;

		public Charge(int entityId, Vec3d near, float size, int color, int life) {
			super(Math.max(2, life));
			this.entityId = entityId;
			this.at = near;
			this.size = size;
			this.color = color;
		}

		private Vec3d focus(ClientWorld world) {
			Entity e = world.getEntityById(entityId);
			if (e instanceof net.minecraft.entity.player.PlayerEntity p) {
				return p.getEyePos().add(p.getRotationVec(1.0F).multiply(1.0)).add(0, -0.2, 0);
			}
			return e != null && e.getPos().squaredDistanceTo(at) < 400 ? e.getPos().add(at.subtract(e.getPos())) : at;
		}

		@Override
		public boolean tick(ClientWorld world) {
			at = focus(world);
			float t = age / (float) lifetime;
			int n = 2 + (int) (t * 8);
			for (int i = 0; i < n; i++) {
				Vec3d d = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).normalize().multiply(size * (1.5 + world.random.nextDouble()));
				Vec3d p = at.add(d);
				Vec3d v = d.multiply(-0.09);
				world.addImportantParticle(ModParticles.PLASMA_SPARK, true, p.x, p.y, p.z, v.x, v.y, v.z);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float t = progress(td);
			Vec3d p = at.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			double core = size * 0.15 * (0.3 + t);
			FxDraw.disc(vc, m, p, cb[0], cb[1], core * 4, color, 0.8F * t, 0.0F, 24);
			FxDraw.disc(vc, m, p, cb[0], cb[1], core, 0xFFFFFF, 1.0F, 0.2F, 16);
			double ringR = size * (1 - t);
			FxDraw.ring(vc, m, p, cb[0], cb[1], ringR * 0.9, ringR, color, 0, 0.7F * t, 0, 32);
		}
	}

	// ============================================================== slice plane

	public static class Slice extends FxEffect {
		private final Vec3d c, normal;
		private final float half, height;
		private final int color;

		public Slice(Vec3d c, Vec3d normal, float half, float height, int color, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.normal = new Vec3d(normal.x, 0, normal.z).normalize();
			this.half = half;
			this.height = height;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 10, 6);
			Vec3d along = new Vec3d(-normal.z, 0, normal.x);
			Vec3d p = c.subtract(cam);
			double reveal = Math.min(1.0, a / 20.0);
			Vec3d l = p.add(along.multiply(-half * reveal)), r = p.add(along.multiply(half * reveal));
			double lo = -height * 0.4, hi = height;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float shimmer = 0.15F + 0.08F * MathHelper.sin(a * 0.9F);
			Crystal.quad(vc, m, l.add(0, lo, 0), r.add(0, lo, 0), r.add(0, hi, 0), l.add(0, hi, 0), color, shimmer * env);
			double w = 0.15 + 0.1 * Math.sin(a * 0.5);
			FxDraw.ribbon(vc, m, l.add(0, lo, 0), l.add(0, hi, 0), Vec3d.ZERO, w, 0xFFFFFF, env, env);
			FxDraw.ribbon(vc, m, r.add(0, lo, 0), r.add(0, hi, 0), Vec3d.ZERO, w, 0xFFFFFF, env, env);
			for (int k = 0; k < 6; k++) {
				double y = lo + (hi - lo) * (((a * 0.01) + k / 6.0) % 1.0);
				FxDraw.ribbon(vc, m, l.add(0, y, 0), r.add(0, y, 0), Vec3d.ZERO, 0.08, color, 0.8F * env, 0.8F * env);
			}
		}
	}

	// ============================================================== warning (Legendary+ announcements)

	public static class Warning extends FxEffect {
		private final Vec3d c;
		private final int tier;

		public Warning(Vec3d c, int tier) {
			super(100);
			this.c = c;
			this.tier = tier;
		}

		private int color() {
			return switch (tier) {
				case 0 -> 0xFFB000;
				case 1 -> 0xFF2020;
				default -> rainbow(age * 0.01F);
			};
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 20 == 0) {
				ClientFx.playGlobal(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, tier == 2 ? 0.5F : 0.7F);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float env = envelope(a, lifetime, 5, 20);
			float pulse = 0.5F + 0.5F * MathHelper.sin(a * 0.31F);
			int col = color();
			Vec3d p = c.subtract(cam);
			double scale = tier == 2 ? 3 : tier == 1 ? 2 : 1.3;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			for (int k = 0; k < 3; k++) {
				double rr = (8 + k * 8) * scale;
				double spin = a * 0.02 * (k % 2 == 0 ? 1 : -1);
				Vec3d u = new Vec3d(Math.cos(spin), 0, Math.sin(spin)), v = new Vec3d(-Math.sin(spin), 0, Math.cos(spin));
				FxDraw.ring(vc, m, p.add(0, 0.3, 0), u, v, rr * 0.95, rr, col, 0, (0.4F + 0.6F * pulse) * env, 0, 64);
				// Hazard ticks.
				for (int i = 0; i < 12; i++) {
					double ang = i * MathHelper.TAU / 12;
					Vec3d d = u.multiply(Math.cos(ang)).add(v.multiply(Math.sin(ang)));
					FxDraw.ribbon(vc, m, p.add(d.multiply(rr * 1.02)).add(0, 0.3, 0), p.add(d.multiply(rr * 1.12)).add(0, 0.3, 0), Vec3d.ZERO, 0.6 * scale, col, env * pulse, env * pulse);
				}
			}
			FxDraw.beam(vc, m, p, p.add(0, 300, 0), Vec3d.ZERO, 0.6 * scale, col, env * (0.4F + 0.4F * pulse));
			FxDraw.disc(vc, m, p.add(0, 0.25, 0), x, z, 30 * scale, col, 0.12F * env * pulse, 0.0F, 48);
		}
	}

	// ============================================================== gravity reversal

	public static class GravityFlip extends FxEffect {
		private final Vec3d c;
		private final float r;

		public GravityFlip(Vec3d c, float r, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d cam = ClientFx.cameraPos();
			for (int i = 0; i < 30; i++) {
				double x = cam.x + world.random.nextGaussian() * 24, z = cam.z + world.random.nextGaussian() * 24;
				if ((x - c.x) * (x - c.x) + (z - c.z) * (z - c.z) > r * r) {
					continue;
				}
				double y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
				world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y + world.random.nextDouble() * 4, z, 0, 0.3 + world.random.nextDouble() * 0.4, 0);
				if (i % 5 == 0) {
					world.addParticle(ModParticles.SHOCK_DUST, x, y + 0.2, z, 0, 0.25, 0);
				}
			}
			ClientFx.distortion = Math.max(ClientFx.distortion, 0.15F);
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		}
	}

	// ============================================================== glitch

	public static class Glitch extends FxEffect {
		private final Vec3d c;
		private final float r;
		private final int color;
		private final Random rnd;

		public Glitch(Vec3d c, float r, int color, int seed, int life) {
			super(Math.max(2, life));
			this.c = c;
			this.r = r;
			this.color = color;
			this.rnd = Random.create(seed);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (ClientFx.cameraPos().squaredDistanceTo(c) < (r * 2) * (r * 2)) {
				ClientFx.screen(3, 0.25F, 2);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Random frame = Random.create(rnd.nextLong() ^ age);
			int n = (int) Math.min(40, 6 + r);
			for (int i = 0; i < n; i++) {
				Vec3d p = c.add(frame.nextGaussian() * r * 0.6, frame.nextGaussian() * r * 0.4, frame.nextGaussian() * r * 0.6).subtract(cam);
				double s = 0.3 + frame.nextDouble() * 1.5;
				int col = frame.nextBoolean() ? color : (frame.nextBoolean() ? 0xFF00FF : 0x00FFFF);
				float alpha = 0.3F + frame.nextFloat() * 0.5F;
				Crystal.quad(vc, m, p, p.add(s * 3, 0, 0), p.add(s * 3, s * 0.4, 0), p.add(0, s * 0.4, 0), col, alpha);
				Crystal.quad(vc, m, p, p.add(0, 0, s * 3), p.add(0, s * 0.4, s * 3), p.add(0, s * 0.4, 0), col, alpha * 0.7F);
			}
		}
	}


	// ============================================================== the moon

	/** A cratered moon that falls out of the sky: slow at first, then faster than anything should fall. */
	public static class Moon extends FxEffect {
		private final Vec3d ground;
		private final float radius, startHeight;

		public Moon(Vec3d ground, float radius, float startHeight, int color, int life) {
			super(Math.max(20, life));
			this.ground = ground;
			this.radius = radius;
			this.startHeight = Math.max(startHeight, radius * 3);
		}

		private Vec3d centre(float a) {
			float t = Math.min(1.0F, a / lifetime);
			double e = Math.pow(t, 2.4);
			double y = ground.y + radius * 0.92 + (startHeight - radius * 0.92) * (1 - e);
			return new Vec3d(ground.x, y, ground.z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			float t = age / (float) lifetime;
			if (t > 0.55F) {
				ClientFx.shake = Math.max(ClientFx.shake, (t - 0.55F) * 4.0F);
			}
			if (age % 6 == 0) {
				ClientFx.tintSky(0x0A0A18, 0.4F + 0.6F * t);
			}
			if (t > 0.7F) {
				Vec3d c = centre(age);
				for (int i = 0; i < 8; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					Vec3d p = ground.add(Math.cos(ang) * radius * 1.5, 1 + world.random.nextDouble() * 8, Math.sin(ang) * radius * 1.5);
					world.addImportantParticle(ModParticles.SHOCK_DUST, true, p.x, p.y, p.z, -Math.cos(ang) * 0.4, 0.1, -Math.sin(ang) * 0.4);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			float t = Math.min(1.0F, a / lifetime);
			Vec3d c = centre(a).subtract(cam);
			Vec3d sun = new Vec3d(0.45, 0.8, 0.35).normalize();
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			int lat = 28, lon = 56;
			for (int i = 0; i < lat; i++) {
				double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
				for (int j = 0; j < lon; j++) {
					double p0 = MathHelper.TAU * j / lon, p1 = MathHelper.TAU * (j + 1) / lon;
					Vec3d mid = Cage.unit((t0 + t1) / 2, (p0 + p1) / 2);
					double h = hash(i, j);
					double mare = 0.5 + 0.5 * Math.sin(i * 0.55) * Math.cos(j * 0.31 + 1.7);
					double grey = 0.62 + 0.16 * h - 0.2 * mare * mare;
					if (hash(i * 3 + 1, j * 5 + 2) > 0.93) {
						grey *= 0.55;
					}
					double light = 0.28 + 0.72 * Math.max(0.0, mid.dotProduct(sun));
					// Re-entry scorching on the side facing the ground.
					double heat = Math.max(0.0, -mid.y) * t * t;
					int r = (int) MathHelper.clamp(255 * grey * light + 255 * heat * 0.9, 0, 255);
					int g = (int) MathHelper.clamp(255 * grey * light * 0.97 + 255 * heat * 0.35, 0, 255);
					int b = (int) MathHelper.clamp(255 * grey * light * 1.03, 0, 255);
					int col = (r << 16) | (g << 8) | b;
					Vec3d[] q = {Cage.unit(t0, p0), Cage.unit(t0, p1), Cage.unit(t1, p1), Cage.unit(t1, p0)};
					for (Vec3d n : q) {
						Vec3d pt = c.add(n.multiply(radius));
						FxDraw.vertex(solid, m, pt.x, pt.y, pt.z, col, 1.0F);
					}
				}
			}
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.sphere(glow, m, c, radius * 1.04, Vec3d.ZERO, 0xFFE0B0, 0.0F, 0.25F + 0.2F * t, 14, 28);
			FxDraw.sphere(glow, m, c, radius * (1.08 + 0.12 * t), Vec3d.ZERO, 0xFF7020, 0.0F, 0.5F * t * t, 14, 28);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(glow, m, c, cb[0], cb[1], radius * 1.9, 0xFF8030, 0.12F * t, 0.0F, 40);
		}

		private static double hash(int x, int y) {
			long h = x * 374761393L + y * 668265263L;
			h = (h ^ (h >>> 13)) * 1274126177L;
			return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
		}
	}

	// ============================================================== steam

	public static void steam(ClientWorld world, Vec3d c, float r) {
		for (int i = 0; i < 30 + r * 10; i++) {
			double x = c.x + world.random.nextGaussian() * r * 0.5, z = c.z + world.random.nextGaussian() * r * 0.5;
			world.addImportantParticle(ParticleTypes.CLOUD, true, x, c.y + world.random.nextDouble(), z, world.random.nextGaussian() * 0.05, 0.15 + world.random.nextDouble() * 0.2, world.random.nextGaussian() * 0.05);
		}
		world.playSound(c.x, c.y, c.z, net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH, net.minecraft.sound.SoundCategory.BLOCKS, 1.5F, 0.6F, false);
	}
}
