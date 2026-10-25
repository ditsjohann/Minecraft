package com.armageddon.client.fx;

import com.armageddon.client.particle.ModParticleFactories;
import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** The over-the-top effects: rainbow bass, death rays, shock domes, light pillars, jets, supernovae. */
public final class InsaneEffects {
	private InsaneEffects() {
	}

	public static int rainbow(float hue) {
		return MathHelper.hsvToRgb(((hue % 1.0F) + 1.0F) % 1.0F, 0.85F, 1.0F);
	}

	static Vec3d perpendicular(Vec3d dir) {
		Vec3d a = Math.abs(dir.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
		return dir.crossProduct(a).normalize();
	}

	// ===================================================================== dubstep beam

	/** Rainbow sonic beam with travelling bass rings and a pulsing speaker cone at the muzzle. */
	public static class DubstepBeam extends FxEffect {
		private final Vec3d from, to;
		private final double width;
		private final float hue;

		public DubstepBeam(Vec3d from, Vec3d to, double width, int beat) {
			super(7);
			this.from = from;
			this.to = to;
			this.width = width;
			this.hue = (beat * 0.0625F) % 1.0F;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 0) {
				Vec3d d = to.subtract(from);
				double len = d.length();
				for (double s = 0; s < len; s += 2.0) {
					Vec3d p = from.add(d.multiply(s / len));
					world.addParticle(ModParticles.ARC_SPARK, p.x, p.y, p.z, world.random.nextGaussian() * 0.15, world.random.nextGaussian() * 0.15, world.random.nextGaussian() * 0.15);
				}
				for (int i = 0; i < 12; i++) {
					world.addParticle(ParticleTypes.NOTE, to.x + world.random.nextGaussian(), to.y + world.random.nextGaussian(), to.z + world.random.nextGaussian(),
							world.random.nextDouble(), 0, 0);
				}
				ClientFx.hueKick(0.5F);
				ClientFx.shake = Math.max(ClientFx.shake, 0.35F);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float alpha = (1 - t);
			Vec3d a = from.subtract(cam), b = to.subtract(cam);
			Vec3d dir = b.subtract(a);
			double len = dir.length();
			if (len < 0.01) {
				return;
			}
			Vec3d n = dir.multiply(1.0 / len);
			Vec3d u = perpendicular(n);
			Vec3d v = n.crossProduct(u).normalize();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			int segs = Math.max(4, (int) (len / 1.5));
			double w = width * (1.0 + 0.6 * Math.sin(t * Math.PI));
			for (int i = 0; i < segs; i++) {
				double s0 = (double) i / segs, s1 = (double) (i + 1) / segs;
				int col = rainbow(hue + (float) s0 * 0.8F - (age + tickDelta) * 0.05F);
				double wob = Math.sin((s0 * 20 - (age + tickDelta) * 2.5)) * 0.25 * width;
				Vec3d p0 = a.add(dir.multiply(s0)).add(u.multiply(wob));
				Vec3d p1 = a.add(dir.multiply(s1)).add(u.multiply(Math.sin((s1 * 20 - (age + tickDelta) * 2.5)) * 0.25 * width));
				FxDraw.beam(vc, m, p0, p1, Vec3d.ZERO, w, col, alpha);
			}
			// Bass rings rushing down the beam.
			for (int k = 0; k < 4; k++) {
				double s = ((age + tickDelta) * 0.25 + k * 0.25) % 1.0;
				Vec3d c = a.add(dir.multiply(s));
				double r = width * (1.5 + s * 3);
				FxDraw.ring(vc, m, c, u, v, r * 0.7, r, rainbow(hue + k * 0.25F), 0.0F, alpha * 0.9F, 0.0F, 32);
			}
			// Speaker cone pulse at the muzzle.
			double pulse = width * (2.2 + 1.5 * Math.sin((age + tickDelta) * 1.4));
			FxDraw.ring(vc, m, a, u, v, pulse * 0.4, pulse, rainbow(hue + 0.5F), 0.0F, alpha, 0.0F, 32);
			// Soft rainbow halo around the whole beam and a white-hot thread down its middle.
			FxDraw.ribbon(vc, m, a, b, Vec3d.ZERO, w * 4, rainbow(hue + (age + tickDelta) * 0.03F), alpha * 0.12F, alpha * 0.12F);
			FxDraw.ribbon(vc, m, a, b, Vec3d.ZERO, w * 0.25, 0xFFFFFF, alpha, alpha * 0.6F);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, b, cb[0], cb[1], width * 6, rainbow(hue + 0.3F), alpha * 0.7F, 0.0F, 20);
			Effects.streak(vc, m, b, cb, width * 14, width * 0.6, rainbow(hue + 0.6F), alpha * 0.4F);
		}
	}

	// ===================================================================== death ray

	/** A beam that stays alive while it keeps being refreshed — used for continuous lasers. */
	public static class ContinuousBeam extends FxEffect {
		private final Vec3d from, to;
		private final double width;
		private final int color;

		public ContinuousBeam(Vec3d from, Vec3d to, double width, int color) {
			super(2);
			this.from = from;
			this.to = to;
			this.width = width;
			this.color = color;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 0) {
				for (int i = 0; i < 6; i++) {
					world.addParticle(ModParticles.EMBER, to.x, to.y, to.z, world.random.nextGaussian() * 0.25, world.random.nextDouble() * 0.3, world.random.nextGaussian() * 0.25);
				}
				world.addParticle(ParticleTypes.LAVA, to.x, to.y, to.z, 0, 0, 0);
				world.addParticle(ParticleTypes.LARGE_SMOKE, to.x, to.y, to.z, 0, 0.05, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			double time = (System.nanoTime() / 5.0E7) % 10000.0;
			float flicker = 0.85F + 0.15F * (float) Math.sin(time * 2.3);
			Vec3d a = from.subtract(cam), b = to.subtract(cam);
			FxDraw.ribbon(vc, m, a, b, Vec3d.ZERO, width * 5, color, 0.1F, 0.1F);
			FxDraw.beam(vc, m, a, b, Vec3d.ZERO, width * flicker, color, 1.0F);
			FxDraw.beam(vc, m, a, b, Vec3d.ZERO, width * 0.4, 0xFFFFFF, 1.0F);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			// Energy rings racing down the beam.
			Vec3d d = b.subtract(a);
			double len = d.length();
			if (len > 0.5) {
				Vec3d n = d.multiply(1.0 / len);
				Vec3d u = perpendicular(n), v = n.crossProduct(u).normalize();
				for (int k = 0; k < 3; k++) {
					double s = (time * 0.35 + k / 3.0) % 1.0;
					double rr = width * (1.6 + Math.sin(s * Math.PI) * 1.2);
					FxDraw.ring(vc, m, a.add(d.multiply(s)), u, v, rr * 0.7, rr, Effects.fade(color, 0xFFFFFF, 0.3F), 0.0F, 0.6F, 0.0F, 20);
				}
			}
			FxDraw.disc(vc, m, b, basis[0], basis[1], width * 6 * flicker, color, 0.9F, 0.0F, 20);
			FxDraw.disc(vc, m, b, basis[0], basis[1], width * 2.5, 0xFFFFFF, 1.0F, 0.0F, 16);
			FxDraw.disc(vc, m, a, basis[0], basis[1], width * 3, color, 0.8F, 0.0F, 16);
			Effects.streak(vc, m, b, basis, width * 16, width * 0.5, color, 0.5F * flicker);
			// Molten splash ring where it bites.
			double sr = width * (3 + 1.5 * Math.sin(time * 3.1));
			FxDraw.ring(vc, m, b.add(0, 0.05, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), sr * 0.5, sr, Effects.fade(color, 0xFFC060, 0.5F), 0.0F, 0.7F, 0.0F, 24);
		}
	}

	// ===================================================================== shock dome

	/** Expanding translucent hemisphere: the visible air-pressure front of a big blast. */
	public static class Dome extends FxEffect {
		private final Vec3d center;
		private final double maxRadius;
		private final int color;

		public Dome(Vec3d center, double maxRadius, int lifetime, int color) {
			super(lifetime);
			this.center = center;
			this.maxRadius = maxRadius;
			this.color = color;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			double r = maxRadius * (1 - Math.pow(1 - t, 2.5));
			if (r < 0.05) {
				return;
			}
			float alpha = (1 - t) * 0.55F * Effects.smooth((age + tickDelta) / 3.0F);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d c = center.subtract(cam);
			int lon = Math.min(48, Effects.lod(c, r, 24, 48)), lat = lon / 3;
			int hot = Effects.fade(color, 0xFFFFFF, 0.5F);
			// Outer pressure shell (bright fresnel rim), then a fainter echo just inside it.
			shell(vc, m, c, r, lat, lon, hot, alpha);
			shell(vc, m, c, r * 0.9, lat, lon, color, alpha * 0.45F);
			// The shell's foot is where it shears along the ground: a hot band and a dust skirt.
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			FxDraw.ring(vc, m, c.add(0, 0.2, 0), x, z, r * 0.94, r * 1.02, hot, 0.0F, alpha * 0.9F, 0.0F, lon * 2);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			FxDraw.ring(shade, m, c.add(0, 0.1, 0), x, z, r * 0.8, r * 1.05, 0x8A7A68, 0.0F, 0.3F * (1 - t), 0.0F, lon * 2);
		}

		private static void shell(VertexConsumer vc, Matrix4f m, Vec3d c, double r, int lat, int lon, int color, float alpha) {
			for (int i = 0; i < lat; i++) {
				double t0 = (Math.PI / 2) * i / lat, t1 = (Math.PI / 2) * (i + 1) / lat;
				for (int j = 0; j < lon; j++) {
					double p0 = MathHelper.TAU * j / lon, p1 = MathHelper.TAU * (j + 1) / lon;
					domeVertex(vc, m, c, r, t0, p0, color, alpha);
					domeVertex(vc, m, c, r, t0, p1, color, alpha);
					domeVertex(vc, m, c, r, t1, p1, color, alpha);
					domeVertex(vc, m, c, r, t1, p0, color, alpha);
				}
			}
		}

		private static void domeVertex(VertexConsumer vc, Matrix4f m, Vec3d c, double r, double theta, double phi, int color, float alpha) {
			Vec3d n = new Vec3d(Math.cos(theta) * Math.cos(phi), Math.sin(theta), Math.cos(theta) * Math.sin(phi));
			Vec3d p = c.add(n.multiply(r));
			double len = p.length();
			float facing = len < 1.0E-4 ? 1 : (float) Math.abs(n.dotProduct(p.multiply(-1.0 / len)));
			float rim = (1 - facing) * (1 - facing);
			// Brighter low on the dome, thinning towards the crown.
			float foot = 0.65F + 0.35F * (float) Math.cos(theta);
			FxDraw.vertex(vc, m, p.x, p.y, p.z, color, alpha * rim * foot);
		}
	}

	// ===================================================================== light pillar

	/** A colossal column of light into the sky (omega charge, bass drop). */
	public static class Pillar extends FxEffect {
		private final Vec3d base;
		private final double radius;
		private final boolean rainbow;
		private final int color;

		public Pillar(Vec3d base, double radius, int lifetime, int color, boolean rainbow) {
			super(lifetime);
			this.base = base;
			this.radius = radius;
			this.color = color;
			this.rainbow = rainbow;
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int i = 0; i < 10; i++) {
				double a = world.random.nextDouble() * MathHelper.TAU;
				double r = radius * (3 + world.random.nextDouble() * 6);
				Vec3d p = base.add(Math.cos(a) * r, world.random.nextDouble() * 30, Math.sin(a) * r);
				Vec3d v = base.add(0, p.y - base.y, 0).subtract(p).multiply(0.06);
				world.addImportantParticle(rainbow ? ModParticles.PLASMA_SPARK : ModParticles.VOID_SPARK, true, p.x, p.y, p.z, v.x, v.y + 0.3, v.z);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float a = age + tickDelta;
			float grow = Math.min(1.0F, a / 20.0F);
			float alpha = grow * (t > 0.85F ? (1 - t) / 0.15F : 1.0F);
			double w = radius * (0.6 + 0.4 * grow) * (1 + 0.1 * Math.sin(a * 0.9));
			int col = rainbow ? rainbow(a * 0.02F) : color;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d b = base.subtract(cam);
			FxDraw.beam(vc, m, b, b.add(0, 500, 0), Vec3d.ZERO, w, col, alpha);
			FxDraw.beam(vc, m, b, b.add(0, 500, 0), Vec3d.ZERO, w * 0.3, 0xFFFFFF, alpha);
			for (int k = 0; k < 5; k++) {
				double h = ((a * 1.5 + k * 30) % 150);
				double r = w * (1.5 + h / 40.0);
				int rc = rainbow ? rainbow(a * 0.02F + k * 0.2F) : col;
				FxDraw.ring(vc, m, b.add(0, h, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), r * 0.8, r, rc, 0.0F, alpha * (1 - (float) h / 150), 0.0F, 48);
			}
			FxDraw.disc(vc, m, b.add(0, 0.2, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), w * 8, col, alpha * 0.6F, 0.0F, 48);
			// Three energy strands spiralling up the column.
			for (int s = 0; s < 3; s++) {
				Vec3d prev = null;
				for (int i = 0; i <= 36; i++) {
					double h = i * 6.0;
					double ang = s * MathHelper.TAU / 3 + h * 0.045 + a * 0.12;
					double rr = w * (1.1 + 0.25 * Math.sin(h * 0.05 + a * 0.2));
					Vec3d p = b.add(Math.cos(ang) * rr, h, Math.sin(ang) * rr);
					if (prev != null) {
						float k = alpha * (1 - i / 36.0F);
						int sc = rainbow ? rainbow(a * 0.02F + s * 0.33F + i * 0.01F) : Effects.fade(col, 0xFFFFFF, 0.35F);
						FxDraw.ribbon(vc, m, prev, p, Vec3d.ZERO, Math.max(0.12, w * 0.12), sc, k, k);
					}
					prev = p;
				}
			}
			// Pulsing corona at the base and a bloom where it pierces the clouds.
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			double cor = w * (3.0 + 0.6 * Math.sin(a * 0.5));
			FxDraw.disc(vc, m, b.add(0, 1.0, 0), cb[0], cb[1], cor, Effects.fade(col, 0xFFFFFF, 0.4F), alpha * 0.8F, 0.0F, 24);
			Effects.streak(vc, m, b.add(0, 1.0, 0), cb, w * 18, w * 0.4, col, alpha * 0.45F);
			FxDraw.disc(vc, m, b.add(0, 200, 0), cb[0], cb[1], w * 6, col, alpha * 0.35F, 0.0F, 20);
		}
	}

	// ===================================================================== bass pulse

	public static class BassPulse extends FxEffect {
		private final Vec3d center;
		private final double radius;
		private final float hue;

		public BassPulse(Vec3d center, double radius, int beat) {
			super(16);
			this.center = center;
			this.radius = radius;
			this.hue = beat * 0.13F;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age == 0) {
				for (int i = 0; i < 40; i++) {
					double a = MathHelper.TAU * i / 40;
					world.addImportantParticle(ParticleTypes.NOTE, true, center.x + Math.cos(a) * 2, center.y + 1, center.z + Math.sin(a) * 2,
							(hue + i / 40.0) % 1.0, 0, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			double r = radius * (1 - Math.pow(1 - t, 2));
			float alpha = 1 - t;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d c = center.subtract(cam).add(0, 0.4, 0);
			for (int k = 0; k < 3; k++) {
				double rr = r * (1 - k * 0.18);
				FxDraw.ring(vc, m, c.add(0, k * 0.6, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Math.max(0, rr - 1.2), rr, rainbow(hue + k * 0.15F + t * 0.3F), 0.0F, alpha, 0.0F, 64);
			}
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			FxDraw.disc(vc, m, c.add(0, 1, 0), cb[0], cb[1], radius * 0.25 * (1 - t), rainbow(hue + t), alpha * 0.6F, 0.0F, 20);
			// Equaliser bars around the ring.
			int bars = 48;
			for (int i = 0; i < bars; i++) {
				double a = MathHelper.TAU * i / bars;
				double h = (1 + Math.sin(i * 1.7 + hue * 30) * 0.5 + Math.sin(i * 0.43) * 0.5) * 3 * (1 - t);
				Vec3d p = c.add(Math.cos(a) * r * 0.9, 0, Math.sin(a) * r * 0.9);
				FxDraw.ribbon(vc, m, p, p.add(0, h, 0), Vec3d.ZERO.subtract(p).add(0, 2, 0), 0.5, rainbow(hue + i / (float) bars), alpha, 0.0F);
			}
		}
	}

	// ===================================================================== jet flyover

	/** A bomber streaking across the sky with engine glow and contrails, playing a fly-by roar. */
	public static class JetFlyover extends FxEffect {
		private final Vec3d from, to;

		public JetFlyover(Vec3d from, Vec3d to) {
			super(44);
			this.from = from;
			this.to = to;
		}

		Vec3d pos(float t) {
			return from.lerp(to, t);
		}

		@Override
		public boolean tick(ClientWorld world) {
			Vec3d p = pos(age / (float) lifetime);
			if (ModParticleFactories.smokeSprites != null) {
				Vec3d dir = to.subtract(from).normalize();
				Vec3d side = new Vec3d(-dir.z, 0, dir.x);
				for (int s = -1; s <= 1; s += 2) {
					for (int k = 0; k < 4; k++) {
						Vec3d q = p.subtract(dir.multiply(k * 2.5)).add(side.multiply(s * 3));
						Effects.spawn(ModParticleFactories.smokeSprites, ModParticleFactories.CRYO_MIST, q.x, q.y, q.z, 0, 0, 0, 3.0F);
					}
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			Vec3d p = pos(t).subtract(cam);
			Vec3d dir = to.subtract(from).normalize();
			Vec3d side = new Vec3d(-dir.z, 0, dir.x);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer dark = consumers.getBuffer(FxRenderLayers.SHADE);
			// Fuselage + delta wings as dark silhouette.
			FxDraw.ribbon(dark, m, p.subtract(dir.multiply(6)), p.add(dir.multiply(6)), new Vec3d(0, -100, 0).subtract(p), 1.6, 0x202428, 1.0F, 1.0F);
			FxDraw.ribbon(dark, m, p.add(side.multiply(-7)).subtract(dir.multiply(3)), p.add(side.multiply(7)).subtract(dir.multiply(3)),
					new Vec3d(0, -100, 0).subtract(p), 4.0, 0x26292E, 1.0F, 1.0F);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d engine = p.subtract(dir.multiply(6)).add(side.multiply(s * 1.2));
				FxDraw.disc(vc, m, engine, basis[0], basis[1], 3.0, 0xFF9A40, 0.9F, 0.0F, 16);
				FxDraw.ribbon(vc, m, engine, engine.subtract(dir.multiply(14)), Vec3d.ZERO, 1.2, 0xFF7A20, 0.8F, 0.0F);
				FxDraw.ribbon(vc, m, engine, engine.subtract(dir.multiply(6)), Vec3d.ZERO, 0.5, 0xFFF0C0, 0.9F, 0.0F);
				// Navigation strobes on the wing tips.
				boolean on = ((age + (s > 0 ? 5 : 0)) / 5) % 2 == 0;
				Vec3d tip = p.add(side.multiply(s * 7)).subtract(dir.multiply(3));
				FxDraw.disc(vc, m, tip, basis[0], basis[1], on ? 1.6 : 0.6, s > 0 ? 0x40FF60 : 0xFF3030, on ? 1.0F : 0.4F, 0.0F, 10);
			}
			Effects.streak(vc, m, p.subtract(dir.multiply(6)), basis, 18, 0.5, 0xFFB060, 0.35F);
		}
	}

	// ===================================================================== supernova

	public static class Supernova extends FxEffect {
		private final Vec3d center;
		private final double radius;

		public Supernova(Vec3d center, double radius) {
			super(70);
			this.center = center;
			this.radius = radius;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			Vec3d c = center.subtract(cam);
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW_NO_DEPTH);
			double r = radius * 3 * (1 - Math.pow(1 - t, 3));
			float alpha = 1 - t;
			FxDraw.sphere(vc, m, c, r, Vec3d.ZERO, Effects.fade(0xFFFFFF, 0x6A8CFF, t * 1.5F), alpha * 0.15F, alpha * 0.8F, 16, 28);
			Vec3d[] basis = FxDraw.cameraBasis(yaw, pitch);
			// Diffraction spikes.
			for (int k = 0; k < 4; k++) {
				double a = Math.PI / 4 * k + t;
				Vec3d dir = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
				FxDraw.ribbon(vc, m, c.subtract(dir.multiply(r * 1.6)), c.add(dir.multiply(r * 1.6)), Vec3d.ZERO, radius * 0.25 * alpha, 0xFFFFFF, 0.0F, 0.0F);
				FxDraw.ribbon(vc, m, c, c.add(dir.multiply(r * 1.6)), Vec3d.ZERO, radius * 0.25 * alpha, 0xFFF4E0, alpha, 0.0F);
				FxDraw.ribbon(vc, m, c, c.subtract(dir.multiply(r * 1.6)), Vec3d.ZERO, radius * 0.25 * alpha, 0xFFF4E0, alpha, 0.0F);
			}
			FxDraw.disc(vc, m, c, basis[0], basis[1], r * 0.6, 0xFFFFFF, alpha, 0.0F, 32);
			// Ejected shells in nebula colours and a shock ring racing outward in the view plane.
			FxDraw.sphere(vc, m, c, r * 1.35, Vec3d.ZERO, Effects.fade(0xFF7AC8, 0x7A4AFF, t), 0.0F, alpha * 0.45F, 14, 24);
			FxDraw.sphere(vc, m, c, r * 1.7, Vec3d.ZERO, 0x40C8FF, 0.0F, alpha * 0.25F, 12, 20);
			double sr = radius * 5 * (1 - Math.pow(1 - t, 2));
			FxDraw.ring(vc, m, c, basis[0], basis[1], sr * 0.9, sr, 0xE8F0FF, 0.0F, alpha * 0.8F, 0.0F, 72);
			for (int k = 0; k < 8; k++) {
				double ang = Math.PI / 8 + Math.PI / 4 * k - t * 0.5;
				Vec3d d = basis[0].multiply(Math.cos(ang)).add(basis[1].multiply(Math.sin(ang)));
				FxDraw.ribbon(vc, m, c, c.add(d.multiply(r * 0.9)), Vec3d.ZERO, radius * 0.12 * alpha, 0xCFE0FF, alpha * 0.6F, 0.0F);
			}
		}
	}

	// ===================================================================== kinetic impact

	/** Tall pillar of pulverised rock thrown up by a kinetic rod. */
	public static class DustColumn extends FxEffect {
		private final Vec3d base;
		private final double radius;

		public DustColumn(Vec3d base, double radius) {
			super(60);
			this.base = base;
			this.radius = radius;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (ModParticleFactories.smokeSprites != null && age < 30) {
				for (int i = 0; i < 8; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double r = world.random.nextDouble() * radius * 0.4;
					Effects.spawn(ModParticleFactories.smokeSprites, ModParticleFactories.SHOCK_DUST, base.x + Math.cos(a) * r, base.y + 1, base.z + Math.sin(a) * r,
							Math.cos(a) * 0.2, 1.2 + world.random.nextDouble() * 1.4, Math.sin(a) * 0.2, (float) radius * 0.35F);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
			float t = progress(tickDelta);
			float a = age + tickDelta;
			Vec3d b = base.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			// A column of pulverised rock punching upward, billowing as it slows.
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			float dustA = Effects.smooth(a / 4.0F) * (1 - Effects.smooth((t - 0.4F) / 0.6F));
			double top = 140 * (1 - Math.exp(-a / 12.0));
			int segs = Effects.lod(b, radius * 3, 6, 10);
			java.util.List<Effects.Puff> puffs = new java.util.ArrayList<>(16);
			for (int i = 0; i < 16; i++) {
				double f = (i + 0.5) / 16.0;
				double h = top * f;
				double rr = radius * (0.5 + f * 1.4) * (0.8 + 0.4 * Effects.hash(i, 3));
				double ang = Effects.hash(i, 4) * MathHelper.TAU;
				Vec3d p = b.add(Math.cos(ang) * rr * 0.3, h, Math.sin(ang) * rr * 0.3);
				puffs.add(new Effects.Puff(p, rr, 0xC8B49C, 0x6A5C50, 0.55F * dustA));
			}
			Effects.drawPuffs(puffs, shade, m, cb, segs);
			if (t > 0.3F) {
				return;
			}
			float alpha = 1 - t / 0.3F;
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			FxDraw.beam(vc, m, b, b.add(0, 220, 0), Vec3d.ZERO, 2.5 * alpha, 0xFFE8B0, alpha);
			FxDraw.disc(vc, m, b.add(0, 1, 0), cb[0], cb[1], radius * 3 * alpha, 0xFFD8A0, alpha * 0.7F, 0.0F, 20);
		}
	}
}
