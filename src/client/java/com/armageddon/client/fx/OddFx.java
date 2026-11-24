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

	static final class PrismFx extends FxEffect {
		private static final double R = 7, H = 7;
		private final Vec3d g;

		PrismFx(Vec3d g, int life) {
			super(life);
			this.g = g;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= Prism.CHARGE && age < Prism.CONVERGE && age % 3 == 0) {
				Vec3d c = g.add(0, Prism.HEIGHT, 0);
				com.armageddon.client.vfx.FxEngine.play(com.armageddon.vfx.VfxIds.PRISM_INBEAM, c.x, c.y + 8, c.z, 0, 1, 0, 1.0F, 0, age * 131L);
			}
			if (age >= Prism.SPLIT && age < Prism.CONVERGE && age % 10 == 0) {
				ClientFx.light(g.add(0, Prism.HEIGHT, 0), 0.8F, 0xFFFFFF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
			float t = age + tickDelta;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(camYaw, camPitch);
			float rise = MathHelper.clamp(t / 40.0F, 0.0F, 1.0F);
			Vec3d c = g.add(0, Prism.HEIGHT * rise, 0).subtract(cam);
			double spin = t * 0.015;
			float charge = MathHelper.clamp((t - Prism.CHARGE) / 40.0F, 0.0F, 1.0F);
			boolean split = t >= Prism.SPLIT && t < Prism.CONVERGE + 8;
			// The prism: a faceted glass column, its white-hot heart showing through, rainbow glints on its faces.
			Vec3d[] top = new Vec3d[3], bot = new Vec3d[3];
			for (int i = 0; i < 3; i++) {
				double a = spin + i * MathHelper.TAU / 3;
				top[i] = c.add(Math.cos(a) * R, H, Math.sin(a) * R);
				bot[i] = c.add(Math.cos(a) * R, -H, Math.sin(a) * R);
			}
			FxDraw.beam(glow, m, c.add(0, -H * 0.8, 0), c.add(0, H * 0.8, 0), Vec3d.ZERO, 1.2 + 2.5 * charge, 0xFFFFFF, 0.3F + 0.6F * charge);
			for (int i = 0; i < 3; i++) {
				int j = (i + 1) % 3;
				Solids.quad(shade, m, bot[i], bot[j], top[j], top[i], 0xD0ECFF, 0.22F);
				Solids.quad(shade, m, top[0], top[1], top[2], top[2], 0xE0F4FF, 0.18F);
				FxDraw.ribbon(glow, m, top[i], top[j], Vec3d.ZERO, 0.3, 0xFFFFFF, 0.9F, 0.9F);
				FxDraw.ribbon(glow, m, bot[i], bot[j], Vec3d.ZERO, 0.3, 0xFFFFFF, 0.9F, 0.9F);
				FxDraw.ribbon(glow, m, bot[i], top[i], Vec3d.ZERO, 0.3, 0xFFFFFF, 0.9F, 0.9F);
				// A rainbow glint sliding down each face.
				double u = ((t * 0.02 + i * 0.33) % 1.0);
				Vec3d ga = bot[i].lerp(top[i], 1 - u), gb = bot[j].lerp(top[j], 1 - u);
				int gcol = Prism.COLORS[(int) (t / 6 + i * 2) % Prism.BEAMS];
				FxDraw.ribbon(glow, m, ga, gb, Vec3d.ZERO, 0.8, gcol, 0.35F, 0.35F);
			}
			// Crystal shards orbiting it, catching the light.
			for (int k = 0; k < 9; k++) {
				double a = k * MathHelper.TAU / 9 + t * 0.03 * (k % 2 == 0 ? 1 : -1);
				double r = R + 4 + 2 * Math.sin(k * 2.1 + t * 0.05);
				Vec3d p = c.add(Math.cos(a) * r, Math.sin(t * 0.04 + k) * H * 0.8, Math.sin(a) * r);
				Vec3d up = new Vec3d(0, 1.4, 0), side = new Vec3d(-Math.sin(a), 0, Math.cos(a)).multiply(0.8);
				Solids.quad(shade, m, p.subtract(up), p.add(side), p.add(up), p.subtract(side), 0xE0F4FF, 0.35F);
				Solids.flare(glow, m, p, cb, 0.8, Prism.COLORS[k % Prism.BEAMS], 0.5F);
			}
			// The sunbeam coming down into it.
			if (t >= Prism.CHARGE && t < Prism.CONVERGE + 20) {
				FxDraw.beam(glow, m, c.add(0, 220, 0), c.add(0, H, 0), Vec3d.ZERO, 0.8 + 3.2 * charge, 0xFFFFFF, charge);
				FxDraw.beam(glow, m, c.add(0, 220, 0), c.add(0, H, 0), Vec3d.ZERO, 3 + 6 * charge, 0xFFF8E0, 0.2F * charge);
			}
			if (!split) {
				return;
			}
			// The split: seven beams fanning out of it with the spectrum stretched between them like a sheet.
			Vec3d[] ends = new Vec3d[Prism.BEAMS];
			for (int i = 0; i < Prism.BEAMS; i++) {
				Vec3d h = Prism.hit(g, i, t);
				ends[i] = new Vec3d(h.x, NewFx.groundY(h.x, h.z, g.y), h.z).subtract(cam);
			}
			Vec3d src = c.add(0, -H * 0.3, 0);
			for (int i = 0; i < Prism.BEAMS - 1; i++) {
				Solids.quad(glow, m, src, ends[i], ends[i + 1], src, Prism.COLORS[i], 0.12F);
			}
			for (int i = 0; i < Prism.BEAMS; i++) {
				float flick = 0.85F + 0.15F * MathHelper.sin(t * 0.9F + i);
				FxDraw.beam(glow, m, src, ends[i], Vec3d.ZERO, 2.2 * flick, Prism.COLORS[i], 0.45F);
				FxDraw.beam(glow, m, src, ends[i], Vec3d.ZERO, 0.6, 0xFFFFFF, 0.8F);
				FxDraw.disc(glow, m, ends[i].add(0, 0.15, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 3.4, Prism.COLORS[i], 0.6F, 0.0F, 24);
				Solids.flare(glow, m, ends[i].add(0, 0.6, 0), cb, 3.0, Prism.COLORS[i], 0.8F);
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
