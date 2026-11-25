package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.WonderShapes.Veil;
import com.armageddon.util.MainStageSet;
import com.armageddon.util.NewBombShapes;
import com.armageddon.util.NewBombShapes.Geode;
import com.armageddon.util.NewBombShapes.Lamp;
import com.armageddon.util.NewBombShapes.Prism;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Client effects of the ten oddity bombs (magnetar, comic words, aurora, prism, geode, fossil eyes, lava lamp, pixels). */
final class OddFx {
	private static final String[] WORDS = {"POW", "BAM", "ZAP", "WHAM", "KRAK", "BOOM", "BLAM", "THWACK"};

	private OddFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 10 -> new MagnetarFx(a, p.scale(), p.duration());
			case 11 -> new ComicWord(a, p.scale(), p.color(), WORDS[Math.floorMod((int) p.p1(), WORDS.length)], p.duration());
			case 12 -> new AuroraFx(a, p.scale(), p.duration());
			case 13 -> new PrismFx(a, p.duration());
			case 14 -> new GeodeFx(a, p.duration());
			case 15 -> new FossilEyes(a, b, p.duration());
			case 16 -> new LampFx(a, p.duration());
			case 17 -> new PixelsFx(a, p.scale(), p.duration());
			default -> null;
		};
	}

	/** Axis of the magnetar's field, matching the server's jets. */
	static Vec3d magnetAxis(double t) {
		double tilt = Math.toRadians(32), phi = t * 0.05;
		return new Vec3d(Math.sin(tilt) * Math.cos(phi), Math.cos(tilt), Math.sin(tilt) * Math.sin(phi));
	}

	// ====================================================================== MAGNETAR

	/** A tiny blinding star, dipole field lines turning with it, and two pulsar jets sweeping like a lighthouse. */
	static final class MagnetarFx extends FxEffect {
		private final Vec3d star;
		private final float reach;

		MagnetarFx(Vec3d star, float reach, int life) {
			super(life);
			this.star = star;
			this.reach = reach;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < 440) {
				ClientFx.light(star, 0.9F, 0xA0D0FF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Vec3d c = star.subtract(cam);
			boolean collapsing = t > 430;
			float grow = MathHelper.clamp(t / 20.0F, 0.0F, 1.0F) * (collapsing ? Math.max(0, 1 - (t - 430) / 12.0F) : 1.0F);
			if (grow <= 0) {
				return;
			}
			Solids.flare(glow, m, c, cb, 3.5 * grow, 0xC0E0FF, 1.0F);
			FxDraw.sphere(glow, m, c, 1.4 * grow, Vec3d.ZERO, 0xFFFFFF, 1.0F, 0.4F, 8, 12);
			Vec3d ax = magnetAxis(t);
			Vec3d[] bs = ArsenalEffects.basis(ax);
			// Dipole field lines r = L sin^2(theta), in planes around the axis.
			for (int k = 0; k < 10; k++) {
				double phi = k * MathHelper.TAU / 10;
				Vec3d side = bs[0].multiply(Math.cos(phi)).add(bs[1].multiply(Math.sin(phi)));
				for (int shell = 0; shell < 2; shell++) {
					double L = (shell == 0 ? 0.45 : 0.85) * reach * grow;
					Vec3d prev = null;
					for (int s = 1; s < 40; s++) {
						double th = Math.PI * s / 40;
						double r = L * Math.sin(th) * Math.sin(th);
						Vec3d pt = c.add(ax.multiply(Math.cos(th) * r)).add(side.multiply(Math.sin(th) * r));
						if (prev != null) {
							float a = 0.25F + 0.2F * MathHelper.sin((float) (t * 0.3 - s * 0.5));
							FxDraw.ribbon(glow, m, prev, pt, Vec3d.ZERO, 0.25, shell == 0 ? 0x80B0FF : 0xC060FF, a, a);
						}
						prev = pt;
					}
				}
			}
			// The jets, along the axis both ways, to the ground and beyond.
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Vec3d end = c.add(ax.multiply(sgn * 120));
				FxDraw.beam(glow, m, c, end, Vec3d.ZERO, 1.2 * grow, 0x9FD0FF, 0.8F);
			}
		}
	}

	// ====================================================================== COMIC WORD

	/** A comic-book burst: a jagged star of colour with a black rim, and its word in fat block letters, popping in. */
	static final class ComicWord extends FxEffect {
		private final Vec3d p;
		private final float size;
		private final int color;
		private final String word;

		ComicWord(Vec3d p, float size, int color, String word, int life) {
			super(life);
			this.p = p;
			this.size = size;
			this.color = color;
			this.word = word;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			float pop = t < 4 ? t / 4 * 1.25F : t < 8 ? 1.25F - (t - 4) / 16 : 1.0F;
			float fade = Math.min(1, (lifetime - t) / 10.0F);
			if (fade <= 0) {
				return;
			}
			Vec3d c = p.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Vec3d toCam = c.normalize().multiply(-1);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			double R = size * pop;
			// The burst: 12 spikes, black first (larger), colour on top, nudged towards the camera.
			for (int layer = 0; layer < 2; layer++) {
				int col = layer == 0 ? 0x101010 : color;
				double k = layer == 0 ? 1.12 : 1.0;
				Vec3d cc = c.add(toCam.multiply(layer * 0.3));
				for (int i = 0; i < 12; i++) {
					double a0 = i * MathHelper.TAU / 12, a1 = (i + 0.5) * MathHelper.TAU / 12, a2 = (i + 1) * MathHelper.TAU / 12;
					double spike = (i % 2 == 0 ? 1.0 : 0.8) * R * k;
					Vec3d p0 = cc.add(dir(cb, a0).multiply(R * 0.55 * k));
					Vec3d p1 = cc.add(dir(cb, a1).multiply(spike));
					Vec3d p2 = cc.add(dir(cb, a2).multiply(R * 0.55 * k));
					Solids.quad(shade, m, cc, p0, p1, p2, col, 0.95F * fade);
				}
			}
			// The word in the MAIN STAGE pixel font, black on the burst.
			double px = R * 0.11;
			int w = MainStageSet.width(word);
			Vec3d origin = c.add(toCam.multiply(0.7)).subtract(cb[0].multiply(w * px / 2)).add(cb[1].multiply(2.5 * px));
			for (int ch = 0; ch < word.length(); ch++) {
				for (int col = 0; col < 3; col++) {
					for (int row = 0; row < 5; row++) {
						if (!MainStageSet.pixel(word.charAt(ch), col, row)) {
							continue;
						}
						Vec3d q = origin.add(cb[0].multiply((ch * 4 + col) * px)).subtract(cb[1].multiply(row * px));
						Solids.quad(shade, m, q, q.add(cb[0].multiply(px)), q.add(cb[0].multiply(px)).subtract(cb[1].multiply(px)), q.subtract(cb[1].multiply(px)),
								0x080808, fade);
					}
				}
			}
		}

		private static Vec3d dir(Vec3d[] cb, double a) {
			return cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
		}
	}

	// ====================================================================== AURORA

	/** Curtains of light high over the land: rippling folds, green below and violet above, with vertical rays. */
	static final class AuroraFx extends FxEffect {
		private static final int RAYS = 120;
		private final Vec3d g;

		AuroraFx(Vec3d g, float span, int life) {
			super(life);
			this.g = g;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < Veil.STORM) {
				ClientFx.tintSky(0x020818, Math.min(0.92F, age / 60.0F));
			}
			if (age % 10 == 0 && age > Veil.NIGHT && age < Veil.STORM + 20) {
				ClientFx.light(g.add(0, Veil.hem(age) + 6, 0), 0.6F, 0x50FF90);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d o = g.subtract(cam);
			double open = Veil.unrolled(t);
			float alive = t < Veil.STORM ? 1.0F : 0.0F;
			if (open > 0.01 && alive > 0) {
				for (int k = 0; k < Veil.CURTAINS; k++) {
					Vec3d prevHem = null;
					for (int i = 0; i < RAYS; i++) {
						double sv = i / (double) (RAYS - 1);
						if (Math.abs(sv - 0.5) * 2 > open) {
							prevHem = null;
							continue;
						}
						// The ends of a curtain thin out, and it unrolls from the middle.
						double edge = Math.min(1, (open - Math.abs(sv - 0.5) * 2) * 6) * Math.min(1, Math.min(sv, 1 - sv) * 8 + 0.2);
						double b = Veil.pulse(k, sv, t) * edge;
						Vec3d hem = o.add(Veil.at(k, sv, t));
						double len = Veil.RAY * (0.65 + 0.35 * Math.sin(sv * 17 + k * 2.3 + t * 0.02));
						Vec3d mid = hem.add(0, len * 0.42, 0), top = hem.add(0, len, 0);
						FxDraw.ribbon(glow, m, hem, mid, Vec3d.ZERO, 1.3, Veil.HEM[k], (float) (0.55 * b), (float) (0.25 * b));
						FxDraw.ribbon(glow, m, mid, top, Vec3d.ZERO, 1.3, Veil.CROWN[k], (float) (0.25 * b), 0.0F);
						if (prevHem != null) {
							// The hem: a bright seam along the bottom of the curtain.
							FxDraw.ribbon(glow, m, prevHem, hem, Vec3d.ZERO, 0.7, 0xD8FFE8, (float) (0.6 * b), (float) (0.6 * b));
						}
						prevHem = hem;
					}
				}
			}
			// The column the curtains spiral into, brightening until the storm breaks out of it.
			if (t > Veil.SWEEP_END && t < Veil.STORM + 30) {
				float u = (float) MathHelper.clamp((t - Veil.SWEEP_END) / (Veil.STORM - Veil.SWEEP_END), 0.0, 1.0);
				float out = t < Veil.STORM ? 1.0F : 1.0F - (t - Veil.STORM) / 30.0F;
				double h = t < Veil.STORM ? 80 : 80 + (t - Veil.STORM) * 12;
				FxDraw.beam(glow, m, o, o.add(0, h, 0), Vec3d.ZERO, 2 + 6 * u, 0x60FFA0, 0.5F * u * out);
				FxDraw.beam(glow, m, o, o.add(0, h, 0), Vec3d.ZERO, 0.8 + 2 * u, 0xE0FFF0, 0.7F * u * out);
			}
		}
	}

	/**
	 * The Prism: a great faceted gem comes down into a shaft of sunlight, turning, ringed with a halo of rainbow.
	 * Seven soft spotlights fall from it and wander over the land in their colours; they gather white-hot under it,
	 * and it shatters, its shards falling as sparks of colour to grow into crystal where they land.
	 */
	static final class PrismFx extends FxEffect {
		private static final Vec3d UP = new Vec3d(0, 1, 0);
		private final Vec3d g;

		PrismFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		private static float smooth(double u) {
			u = MathHelper.clamp(u, 0.0, 1.0);
			return (float) (u * u * (3 - 2 * u));
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < Prism.SHATTER && age % 3 == 0) {
				Vec3d c = g.add(0, Prism.gemY(age), 0);
				com.armageddon.client.vfx.FxEngine.play(com.armageddon.vfx.VfxIds.PRISM_INBEAM, c.x, c.y + 6, c.z, 0, 1, 0, 1.0F, 0, age * 37L);
			}
			if (age < Prism.SHATTER && age % 8 == 0) {
				ClientFx.light(g.add(0, Prism.gemY(age), 0), 0.9F, 0xFFFFFF);
				if (age >= Prism.REFRACT) {
					for (int i = 0; i < Prism.SPOTS; i++) {
						Vec3d sp = g.add(Prism.spot(i, age));
						ClientFx.light(sp.add(0, 2, 0), 0.6F, Prism.COLORS[i]);
					}
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d o = g.subtract(cam);
			Vec3d gem = o.add(0, Prism.gemY(a), 0);
			float alive = 1 - smooth((a - Prism.SHATTER) / 3.0F);
			if (alive > 0.01F) {
				sunlight(glow, consumers, m, cb, gem, a);
				spotlights(glow, m, cam, gem, a);
				halo(glow, m, cb, gem, a);
				gem(glow, shade, consumers, m, cb, gem, a);
			}
			shards(glow, m, cb, o, cam, a);
		}

		/** A broad soft shaft of sunlight pouring down into the gem from high above. */
		private void sunlight(VertexConsumer glow, VertexConsumerProvider consumers, Matrix4f m, Vec3d[] cb, Vec3d gem, float a) {
			float on = smooth(a / 30.0F) * (1 - smooth((a - Prism.CONVERGE) / 50.0F) * 0.5F);
			Vec3d top = gem.add(0, 160, 0);
			for (int k = 0; k < 4; k++) {
				double w = 3 + k * 3.5;
				FxDraw.beam(glow, m, top, gem.add(0, Prism.GEM * 0.6, 0), Vec3d.ZERO, w, k == 0 ? 0xFFFFFF : 0xFFF6D8, (0.13F - k * 0.028F) * on);
			}
			FxKit.halo(consumers, m, cb, gem.add(0, Prism.GEM, 0), 10, 0xFFF8E0, 0.25F * on);
		}

		/**
		 * The spotlights: a soft cone of each colour from the gem's lower facets down to a pool of light on the
		 * ground, wandering; at the end they slide in under the gem and blend to white.
		 */
		private void spotlights(VertexConsumer glow, Matrix4f m, Vec3d cam, Vec3d gem, float a) {
			if (a < Prism.REFRACT - 5) {
				return;
			}
			float on = smooth((a - Prism.REFRACT + 5) / 20.0F);
			double merge = smooth((a - Prism.CONVERGE) / (Prism.SHATTER - Prism.CONVERGE));
			Vec3d apex = gem.add(0, -Prism.GEM * 0.8, 0);
			Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
			for (int i = 0; i < Prism.SPOTS; i++) {
				Vec3d sp = Prism.spot(i, a);
				double gy = NewFx.groundY(g.x + sp.x, g.z + sp.z, g.y) - cam.y + 0.15;
				Vec3d pool = new Vec3d(g.x + sp.x - cam.x, gy, g.z + sp.z - cam.z);
				int col = Solids.mix(Prism.COLORS[i], 0xFFFFFF, merge);
				double rad = Prism.SPOT * (1 - merge * 0.5);
				// The cone: a fan of soft quads from the facet to the pool's rim, fading toward the top.
				int n = 16;
				for (int k = 0; k < n; k++) {
					double a0 = k * MathHelper.TAU / n, a1 = (k + 1) * MathHelper.TAU / n;
					Vec3d r0 = pool.add(Math.cos(a0) * rad, 0, Math.sin(a0) * rad), r1 = pool.add(Math.cos(a1) * rad, 0, Math.sin(a1) * rad);
					FxDraw.vertex(glow, m, apex.x, apex.y, apex.z, col, 0.02F * on);
					FxDraw.vertex(glow, m, apex.x, apex.y, apex.z, col, 0.02F * on);
					FxDraw.vertex(glow, m, r1.x, r1.y, r1.z, col, 0.13F * on);
					FxDraw.vertex(glow, m, r0.x, r0.y, r0.z, col, 0.13F * on);
				}
				// The pool of light on the ground, soft at its edge, brighter at its heart.
				FxDraw.disc(glow, m, pool, X, Z, rad * 1.15, col, 0.5F * on, 0.0F, 32);
				FxDraw.disc(glow, m, pool.add(0, 0.02, 0), X, Z, rad * 0.45, Solids.mix(col, 0xFFFFFF, 0.4), 0.45F * on, 0.0F, 24);
			}
			if (merge > 0.05) {
				Vec3d under = new Vec3d(g.x - cam.x, NewFx.groundY(g.x, g.z, g.y) - cam.y + 0.2, g.z - cam.z);
				FxDraw.disc(glow, m, under, X, Z, 6 + merge * 6, 0xFFFFFF, (float) merge, 0.0F, 32);
				FxDraw.beam(glow, m, apex, under, Vec3d.ZERO, 2 + merge * 4, 0xFFFFFF, 0.6F * (float) merge);
			}
		}

		/** A glory of rainbow round the gem: seven soft rings, slowly breathing. */
		private void halo(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d gem, float a) {
			float on = smooth((a - 30) / 40.0F);
			double breathe = 1 + 0.05 * Math.sin(a * 0.05);
			for (int i = 0; i < Prism.SPOTS; i++) {
				double r0 = (14 + i * 1.6) * breathe;
				FxDraw.ring(glow, m, gem, cb[0], cb[1], r0, r0 + 1.6, Prism.COLORS[Prism.SPOTS - 1 - i], 0.0F, 0.22F * on, 0.0F, 48);
			}
		}

		/**
		 * The gem: a tall octahedron of clear crystal turning slowly, each facet catching a different colour, light
		 * burning at its heart; as the light gathers, it cracks with white.
		 */
		private void gem(VertexConsumer glow, VertexConsumer shade, VertexConsumerProvider consumers, Matrix4f m, Vec3d[] cb, Vec3d c, float a) {
			double spin = a * 0.015;
			double h = Prism.GEM, w = Prism.GEM * 0.62;
			Vec3d top = c.add(0, h, 0), bot = c.add(0, -h, 0);
			Vec3d[] ring = new Vec3d[6];
			for (int k = 0; k < 6; k++) {
				double ang = spin + k * MathHelper.TAU / 6;
				ring[k] = c.add(Math.cos(ang) * w, (k % 2 == 0 ? 0.8 : -0.8), Math.sin(ang) * w);
			}
			double heat = smooth((a - Prism.CONVERGE) / (Prism.SHATTER - Prism.CONVERGE));
			for (int k = 0; k < 6; k++) {
				Vec3d r0 = ring[k], r1 = ring[(k + 1) % 6];
				int col = Prism.COLORS[(k + (int) (a / 20)) % Prism.SPOTS];
				for (Vec3d tip : new Vec3d[]{top, bot}) {
					Solids.quad(shade, m, tip, r0, r1, r1, Solids.mix(0xDCEEFF, 0xFFFFFF, heat), 0.38F);
					Solids.quad(glow, m, tip, r0, r1, r1, col, 0.16F + 0.3F * (float) heat);
				}
				FxDraw.ribbon(glow, m, top, r0, Vec3d.ZERO, 0.18, 0xFFFFFF, 0.7F, 0.7F);
				FxDraw.ribbon(glow, m, bot, r0, Vec3d.ZERO, 0.18, 0xFFFFFF, 0.7F, 0.7F);
				FxDraw.ribbon(glow, m, r0, r1, Vec3d.ZERO, 0.18, 0xFFFFFF, 0.7F, 0.7F);
				Solids.flare(glow, m, r0, cb, 0.9 + heat * 2, 0xFFFFFF, 0.6F + 0.4F * MathHelper.sin(a * 0.3F + k));
			}
			Solids.flare(glow, m, c, cb, 4 + heat * 10, 0xFFFFFF, 0.8F);
			FxKit.halo(consumers, m, cb, c, 8 + heat * 20, 0xFFFFFF, 0.25F + 0.4F * (float) heat);
		}

		/** The shards: sparks of every colour falling from where the gem was to where they land. */
		private void shards(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, Vec3d cam, float a) {
			if (a < Prism.SHATTER) {
				return;
			}
			Vec3d from = o.add(0, Prism.GEM_Y, 0);
			for (int k = 0; k < Prism.SHARDS; k++) {
				int land = Prism.shardLand(k);
				double u = (a - Prism.SHATTER) / (land - Prism.SHATTER);
				if (u < 0 || u > 1) {
					continue;
				}
				Vec3d at = Prism.shard(k);
				Vec3d to = new Vec3d(g.x + at.x, NewFx.groundY(g.x + at.x, g.z + at.z, g.y), g.z + at.z).subtract(cam);
				Vec3d mid = from.lerp(to, 0.5).add(0, 14, 0);
				Vec3d p = from.multiply((1 - u) * (1 - u)).add(mid.multiply(2 * u * (1 - u))).add(to.multiply(u * u));
				int col = Prism.COLORS[(int) (Prism.hash(k, 14) * Prism.SPOTS) % Prism.SPOTS];
				Solids.flare(glow, m, p, cb, 1.4, col, 1.0F);
				Solids.dot(glow, m, p, cb, 0.5, 0xFFFFFF, 0.9F);
			}
		}
	}

	// ====================================================================== GEODE

	/** Purple glow from the open geode, building to the resonance, and the shards of amethyst flying out. */
	static final class GeodeFx extends FxEffect {
		private final Vec3d g;
		private final Vec3d[] lands = new Vec3d[Geode.SHARDS];

		GeodeFx(Vec3d g, int life) {
			super(life);
			this.g = g;
			for (int i = 0; i < Geode.SHARDS; i++) {
				Vec3d l = Geode.land(g, i);
				lands[i] = new Vec3d(l.x, NewFx.groundY(l.x, l.z, g.y), l.z);
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age > Geode.RISEN && age < Geode.SHATTER) {
				float k = MathHelper.clamp((age - Geode.RESONATE) / 40.0F, 0.0F, 1.0F);
				ClientFx.light(Geode.centre(g).add(0, Geode.R * 0.7, 0), 0.4F + k * 0.8F, 0xC070FF);
				if (age % 3 == 0) {
					Vec3d c = Geode.centre(g);
					world.addParticle(ParticleTypes.END_ROD, c.x + world.random.nextGaussian() * 5, c.y + world.random.nextDouble() * 10, c.z + world.random.nextGaussian() * 5, 0, 0.08, 0);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			if (t > Geode.RISEN && t < Geode.SHATTER + 20) {
				float k = MathHelper.clamp((t - Geode.RESONATE) / 40.0F, 0.15F, 1.0F) * (0.8F + 0.2F * MathHelper.sin(t * (0.3F + k(t))));
				Vec3d c = Geode.centre(g).subtract(cam);
				FxDraw.sphere(glow, m, c, Geode.R - 3.6, Vec3d.ZERO, 0xA040FF, 0.25F * k, 0.05F, 10, 16);
				Solids.flare(glow, m, c.add(0, Geode.R * 0.7, 0), FxDraw.cameraBasis(camYaw, camPitch), 4 + 8 * k, 0xD090FF, k);
			}
			for (int i = 0; i < Geode.SHARDS; i++) {
				float k = (t - Geode.launch(i)) / Geode.flight(i);
				if (k < 0 || k > 1) {
					continue;
				}
				Vec3d a = Geode.from(g, i);
				Vec3d p = NewBombShapes.arc(a, lands[i], k, 22).subtract(cam);
				Vec3d prev = NewBombShapes.arc(a, lands[i], Math.max(0, k - 0.05), 22).subtract(cam);
				Vec3d d = p.subtract(prev);
				if (d.lengthSquared() < 1.0E-6) {
					continue;
				}
				d = d.normalize();
				Solids.tube(glow, m, p.subtract(d.multiply(1.8)), p.add(d.multiply(0.6)), 0.4, 0.0, 6, 0xE0A0FF, 0.95F, false);
				FxDraw.ribbon(glow, m, NewBombShapes.arc(a, lands[i], Math.max(0, k - 0.2), 22).subtract(cam), p, Vec3d.ZERO, 0.35, 0x9040FF, 0.0F, 0.6F);
			}
		}

		private static float k(float t) {
			return MathHelper.clamp((t - Geode.RESONATE) / 40.0F, 0.0F, 1.0F) * 0.9F;
		}
	}

	// ====================================================================== FOSSIL

	/** Fire kindles in the skull's empty eyes, and a heat haze rolls out of its jaws while it roars. */
	static final class FossilEyes extends FxEffect {
		private final Vec3d eyes, heading;

		FossilEyes(Vec3d eyes, Vec3d heading, int life) {
			super(life);
			this.eyes = eyes;
			this.heading = heading;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			float k = MathHelper.clamp(t / 20.0F, 0.0F, 1.0F) * (0.85F + 0.15F * MathHelper.sin(t * 0.7F));
			for (int side = -1; side <= 1; side += 2) {
				Vec3d e = eyes.add(right.multiply(side * 2.4)).add(0.5, 0.5, 0.5).subtract(cam);
				Solids.flare(glow, m, e, cb, 2.5 * k, 0xFF3010, k);
				FxDraw.sphere(glow, m, e, 1.0, Vec3d.ZERO, 0xFF8020, k, 0.2F, 6, 10);
			}
		}
	}

	// ====================================================================== LAVA LAMP

	/** Goo blobs rising and sinking inside the lamp, then flung out across the land when it bursts. */
	static final class LampFx extends FxEffect {
		private final Vec3d g;
		private final Vec3d[] lands = new Vec3d[Lamp.FLYERS];

		LampFx(Vec3d g, int life) {
			super(life);
			this.g = g;
			for (int i = 0; i < Lamp.FLYERS; i++) {
				Vec3d l = Lamp.land(g, i);
				lands[i] = new Vec3d(l.x, NewFx.groundY(l.x, l.z, g.y), l.z);
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age > Lamp.BUILT && age < Lamp.BURST) {
				ClientFx.light(g.add(0, Lamp.BASE_H + Lamp.H * 0.5, 0), 0.5F, 0xFF7030);
			}
			return super.tick(world);
		}

		@Override
		public boolean early() {
			return true;
		}

		/** The goo inside the lamp is drawn before the glass, so the orange glass is seen over it, not in front of nothing. */
		@Override
		public void renderEarly(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			if (t > Lamp.BUILT - 10 && t < Lamp.BURST) {
				for (int i = 0; i < Lamp.BLOBS; i++) {
					Vec3d c = Lamp.blob(g, i, t).subtract(cam);
					double s = Lamp.blobSize(i);
					double wob = Math.sin(t * 0.08 + i) * 0.18;
					int col = i % 3 == 0 ? 0xFF3080 : 0xFF6A10;
					Solids.ellipsoid(solid, m, c, s * (1 + wob), s * (1.25 - wob), s * (1 + wob), 8, 12, (la, lo, n) -> Solids.mix(col, 0xFFD040, Math.max(0, n.y) * 0.6), false, 1.0F);
					FxDraw.sphere(glow, m, c, s * 1.3, Vec3d.ZERO, col, 0.15F, 0.4F, 8, 12);
				}
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			if (t >= Lamp.BURST) {
				for (int i = 0; i < Lamp.FLYERS; i++) {
					float k = (t - Lamp.BURST) / Lamp.flight(i);
					if (k > 1) {
						continue;
					}
					Vec3d p = NewBombShapes.arc(Lamp.launch(g, i), lands[i], k, 18 + (i % 5) * 4).subtract(cam);
					double s = i < Lamp.BLOBS ? Lamp.blobSize(i) : 0.8;
					int col = i % 3 == 0 ? 0xFF3080 : 0xFF6A10;
					Solids.sphere(solid, m, p, s, col, 6, 10, false);
					FxDraw.sphere(glow, m, p, s * 1.5, Vec3d.ZERO, col, 0.2F, 0.5F, 6, 10);
				}
			}
		}
	}

	// ====================================================================== 8-BIT PIXELS

	/** Glowing voxel pixels rising out of the downscaled ground, blinking between palette colours. */
	static final class PixelsFx extends FxEffect {
		private static final int[] PALETTE = {0xFF40FF, 0x40FFFF, 0xFFFF40, 0x40FF60, 0xFF4040, 0x4060FF};
		private final Vec3d g;
		private final float radius;

		PixelsFx(Vec3d g, float radius, int life) {
			super(life);
			this.g = g;
			this.radius = radius;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			float fade = Math.min(Math.min(1, t / 30.0F), Math.min(1, (lifetime - t) / 30.0F));
			for (int i = 0; i < 260; i++) {
				double ph = (t * (0.004 + NewBombShapes.hash(i, 1) * 0.006) + NewBombShapes.hash(i, 2)) % 1.0;
				double a = NewBombShapes.hash(i, 3) * MathHelper.TAU, d = Math.sqrt(NewBombShapes.hash(i, 4)) * radius;
				double x = g.x + Math.cos(a) * d, z = g.z + Math.sin(a) * d;
				double y = NewFx.groundY(x, z, g.y) + ph * 24;
				// Snap to a 0.5 grid so they move in steps, like sprites.
				Vec3d p = new Vec3d(Math.floor(x * 2) / 2, Math.floor(y * 2) / 2, Math.floor(z * 2) / 2).subtract(cam);
				int col = PALETTE[(i + (int) (t / 6)) % PALETTE.length];
				cube(glow, m, p, 0.5, col, (float) (fade * (1 - ph) * 0.8));
			}
		}

		private static void cube(VertexConsumer vc, Matrix4f m, Vec3d p, double s, int col, float a) {
			Vec3d[] c = new Vec3d[8];
			for (int i = 0; i < 8; i++) {
				c[i] = p.add((i & 1) * s, ((i >> 1) & 1) * s, ((i >> 2) & 1) * s);
			}
			int[][] f = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
			for (int[] q : f) {
				Solids.quad(vc, m, c[q[0]], c[q[1]], c[q[2]], c[q[3]], col, a);
			}
		}
	}
}
