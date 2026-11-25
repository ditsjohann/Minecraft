package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.HorrorShapes;
import com.armageddon.util.HorrorShapes.HellGate;
import com.armageddon.util.HorrorShapes.Horseman;
import com.armageddon.util.HorrorShapes.Maniac;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Client effects of the horror celestials: the Terror Maniac, the Pumpkin Horseman and the Portal of Hell. */
final class HorrorFx {
	private HorrorFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 59 -> new ManiacFx(a, b, (int) p.p1(), p.duration());
			case 60 -> new HorsemanFx(a, b, (int) p.p1(), p.duration());
			case 61 -> new HellGateFx(a, b, (int) p.p1(), p.duration());
			default -> null;
		};
	}

	static final Vec3d UP = new Vec3d(0, 1, 0);

	static Vec3d camera(Vec3d fallback) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc == null || mc.gameRenderer == null ? fallback : mc.gameRenderer.getCamera().getPos();
	}

	static float smooth(double u) {
		return (float) HorrorShapes.smooth(u);
	}

	private static final int BLOOD = 0x6E0505, BLOOD_LIT = 0xA00A0A;

	/** Drops of blood falling from {@code p}, one after another, each a short dark streak. */
	static void drips(VertexConsumer shade, Matrix4f m, Vec3d p, float a, int salt, double fall, double rate) {
		for (int k = 0; k < 3; k++) {
			double ph = ((a * rate + k / 3.0 + HorrorShapes.hash(salt, k)) % 1.0);
			Vec3d d = p.add(0, -ph * ph * fall, 0);
			FxDraw.ribbon(shade, m, d, d.add(0, -0.45, 0), Vec3d.ZERO, 0.12, BLOOD, 0.95F, 0.6F);
		}
	}

	// ====================================================================== TERROR MANIAC

	/**
	 * The thing itself, the veins pulsing with its heartbeat, the rift it climbs out of, and red lightning crawling
	 * through the clouds overhead.
	 */
	static final class ManiacFx extends FxEffect {
		private static final int BODY = 0x231010, MASK = 0xE4DCCF;
		private final Vec3d c, f, r;
		private final List<double[]> veins;
		private final double[] vy;
		private final List<double[]> bolts = new ArrayList<>();

		ManiacFx(Vec3d c, Vec3d heading, int seed, int life) {
			super(Math.max(40, life));
			this.c = c;
			// It faces back toward whoever set it off.
			this.f = heading.lengthSquared() < 1.0E-4 ? new Vec3d(-1, 0, 0) : heading.normalize().negate();
			this.r = new Vec3d(-f.z, 0, f.x);
			this.veins = Maniac.veins(seed);
			this.vy = new double[veins.size()];
			java.util.Arrays.fill(vy, Double.NaN);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			java.util.Random rnd = new java.util.Random(t * 9176L + 17);
			// Red lightning crawling through the clouds.
			if (t < Maniac.END - 40 && rnd.nextFloat() < (t > Maniac.CURSE ? 0.07F : 0.035F)) {
				double a = rnd.nextDouble() * MathHelper.TAU, d = 30 + rnd.nextDouble() * 90;
				bolts.add(new double[]{c.x + Math.cos(a) * d, c.y + 85 + rnd.nextDouble() * 30, c.z + Math.sin(a) * d, rnd.nextDouble() * MathHelper.TAU, t,
						rnd.nextInt(100000)});
				ClientFx.light(new Vec3d(c.x + Math.cos(a) * d, c.y + 60, c.z + Math.sin(a) * d), 1.0F, 0xFF2010);
			}
			bolts.removeIf(b -> t - b[4] > 9);
			// Blood raining down while the curse lasts.
			if (t >= Maniac.CURSE && t < Maniac.SHATTER + 60 && t % 2 == 0) {
				Vec3d eye = camera(c);
				if (Math.hypot(eye.x - c.x, eye.z - c.z) < Maniac.ROT + 20) {
					FxEngine.play(VfxIds.MANIAC_BLOOD_RAIN, eye.x, eye.y, eye.z, 0, 1, 0, 1.0F, 0, t * 6151L);
				}
			}
			// Red ash drifting up out of the ground round the watcher.
			if (t % 3 == 0 && t < Maniac.END - 30) {
				Vec3d eye = camera(c);
				if (Math.hypot(eye.x - c.x, eye.z - c.z) < Maniac.ROT + 20) {
					FxEngine.play(VfxIds.MANIAC_ASH, eye.x, NewFx.groundY(eye.x, eye.z, eye.y - 1), eye.z, 0, 1, 0, 1.0F, 0, t * 977L);
				}
			}
			if (t >= Maniac.RISE && t < Maniac.SHATTER && t % 10 == 0) {
				ClientFx.light(c.add(0, 4, 0), 0.8F, 0xFF2010);
			}
			for (int i = 0; i < veins.size(); i++) {
				if (Double.isNaN(vy[i]) && veins.get(i)[2] <= Maniac.reach(t) + 4) {
					vy[i] = NewFx.groundY(c.x + veins.get(i)[0], c.z + veins.get(i)[1], c.y) + 1.05;
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d o = c.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			moon(consumers, glow, shade, m, cb, o, a);
			lightning(glow, m, cam, a);
			pool(glow, shade, m, o, a);
			veins(glow, shade, m, cb, cam, a);
			rift(glow, shade, m, o, a);
			if (a >= Maniac.RISE && a < Maniac.SHATTER + 3) {
				figure(consumers, solid, glow, shade, m, cb, o, a);
			}
		}

		/** Forked red bolts high in the clouds, flickering. */
		private void lightning(VertexConsumer glow, Matrix4f m, Vec3d cam, float a) {
			for (double[] b : bolts) {
				float life = (float) ((a - b[4]) / 9.0);
				if (life < 0 || life > 1) {
					continue;
				}
				float al = (1 - life) * (((int) (a * 2)) % 2 == 0 ? 1.0F : 0.55F);
				java.util.Random rnd = new java.util.Random((long) b[5]);
				Vec3d p = new Vec3d(b[0], b[1], b[2]).subtract(cam);
				double dir = b[3];
				for (int s = 0; s < 14; s++) {
					dir += (rnd.nextDouble() - 0.5) * 1.2;
					Vec3d q = p.add(Math.cos(dir) * 4, (rnd.nextDouble() - 0.6) * 3, Math.sin(dir) * 4);
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 0.5, 0xFF3020, 0.9F * al, 0.9F * al);
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 2.4, 0xA00808, 0.2F * al, 0.2F * al);
					if (rnd.nextFloat() < 0.3F) {
						double bd = dir + (rnd.nextBoolean() ? 1 : -1) * 0.9;
						Vec3d br = q.add(Math.cos(bd) * 5, -2 - rnd.nextDouble() * 3, Math.sin(bd) * 5);
						FxDraw.ribbon(glow, m, q, br, Vec3d.ZERO, 0.3, 0xFF5030, 0.7F * al, 0.1F * al);
					}
					p = q;
				}
			}
		}

		/** The veins pulse with the heartbeat: waves of red running in along them toward the rift. */
		private void veins(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			double reach = Maniac.reach(a);
			float fade = 1 - smooth((a - Maniac.SHATTER) / 60.0);
			double beat = a < Maniac.CURSE ? 34 : 18;
			for (int i = 0; i < veins.size(); i++) {
				double[] v = veins.get(i);
				if (v[2] > reach || Double.isNaN(vy[i])) {
					continue;
				}
				double phase = ((a / beat) - v[2] / 40.0) % 1.0;
				float pulse = (float) Math.exp(-phase * 6) * 0.8F + 0.15F;
				Vec3d p = new Vec3d(c.x + v[0], vy[i], c.z + v[1]).subtract(cam);
				Solids.dot(shade, m, p, cb, 0.8 + v[3] * 0.6, BLOOD, 0.9F * fade);
				Solids.dot(glow, m, p, cb, 0.5 + v[3] * 0.4, 0xD01008, pulse * fade);
			}
		}

		/** The rift: a jagged red-lit crack in the ground under it, wider once it has climbed out. */
		/** A blood moon hanging over the inside-out world, huge and mottled, haloed in red. */
		private void moon(VertexConsumerProvider consumers, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
			float vis = smooth((a - 40) / 120.0) * (1 - smooth((a - (Maniac.END - 60)) / 60.0));
			if (vis <= 0.01F) {
				return;
			}
			Vec3d p = o.add(f.multiply(-140)).add(r.multiply(50)).add(0, 130, 0);
			FxKit.halo(consumers, m, cb, p, 90, 0xA00808, 0.35F * vis);
			FxDraw.disc(shade, m, p, cb[0], cb[1], 30, 0x8A1008, 0.97F * vis, 0.97F * vis, 48);
			for (int k = 0; k < 10; k++) {
				double ang = HorrorShapes.hash(k, 51) * MathHelper.TAU, d = Math.sqrt(HorrorShapes.hash(k, 52)) * 22;
				Vec3d q = p.add(cb[0].multiply(Math.cos(ang) * d)).add(cb[1].multiply(Math.sin(ang) * d));
				FxDraw.disc(shade, m, q, cb[0], cb[1], 3 + HorrorShapes.hash(k, 53) * 6, 0x4A0604, 0.6F * vis, 0.0F, 20);
			}
			FxDraw.ring(glow, m, p, cb[0], cb[1], 29, 34, 0xFF2010, 0.0F, 0.45F * vis, 0.0F, 48);
		}

		/** A pool of blood spreading round the rift, glossy, its edge creeping outward. */
		private void pool(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d o, float a) {
			float grow = smooth((a - Maniac.RISE) / 160.0) * (1 - smooth((a - (Maniac.END - 40)) / 40.0));
			if (grow <= 0.01F) {
				return;
			}
			double rad = 16 * grow;
			Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
			FxDraw.disc(shade, m, o.add(0, 0.12, 0), X, Z, rad, BLOOD, 0.92F, 0.75F, 40);
			for (int k = 0; k < 6; k++) {
				double ang = k * 1.1 + 0.3, len = rad * (0.8 + 0.5 * HorrorShapes.hash(k, 61));
				FxDraw.ribbon(shade, m, o.add(0, 0.13, 0), o.add(Math.cos(ang) * len, 0.13, Math.sin(ang) * len), Vec3d.ZERO, 2.5, BLOOD, 0.9F, 0.5F);
			}
			// A wet sheen sliding over it.
			double sh = (a * 0.05) % 1.0;
			FxDraw.ring(glow, m, o.add(0, 0.15, 0), X, Z, rad * sh * 0.9, rad * sh, 0xFF3020, 0.0F, 0.12F * grow, 0.0F, 40);
		}

		private void rift(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d o, float a) {
			float open = smooth((a - Maniac.RISE + 30) / 40.0) * (1 - smooth((a - Maniac.SHATTER - 20) / 60.0));
			if (open <= 0.01F) {
				return;
			}
			Vec3d prev = null;
			java.util.Random rnd = new java.util.Random(7);
			for (int s = -10; s <= 10; s++) {
				Vec3d p = o.add(f.multiply(s * 1.0)).add(r.multiply((rnd.nextDouble() - 0.5) * 2.2)).add(0, 0.2, 0);
				if (prev != null) {
					float fl = 0.7F + 0.3F * MathHelper.sin(a * 0.5F + s);
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.6 * open, 0xFF2A10, 0.8F * open * fl, 0.8F * open * fl);
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 5.0 * open, 0x900808, 0.2F * open, 0.2F * open);
				}
				prev = p;
			}
			FxDraw.beam(glow, m, o, o.add(0, 30, 0), Vec3d.ZERO, 4, 0xA01008, 0.1F * open);
		}

		/**
		 * A thing of roots and sinew rising waist-up out of the rift: a ribcage, long arms with longer fingers, a crown
		 * of writhing tendrils, and a cracked white mask with red pinpoints for eyes.
		 */
		private void figure(VertexConsumerProvider consumers, VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
			double rise = Maniac.risen(a);
			double sink = -Maniac.TALL * (1 - rise);
			// Poses: arms hanging, arms raised (the curse), fists slammed down (the snap), head thrown back (the shriek).
			double raise = smooth((a - Maniac.CURSE + 20) / 40.0) * (1 - smooth((a - Maniac.SNAP) / 5.0));
			double slam = smooth((a - Maniac.SNAP) / 5.0);
			double shriek = smooth((a - Maniac.SHRIEK + 10) / 15.0);
			double gone = smooth((a - Maniac.SHATTER) / 3.0);
			double sc = 1 - gone;
			if (sc <= 0.01) {
				return;
			}
			double jitter = (a > Maniac.SHRIEK - 10 ? 0.6 : 0.15) * Math.sin(a * 3.7);
			Vec3d base = o.add(0, sink, 0).add(r.multiply(jitter));
			java.util.function.BiFunction<Double, Double, Vec3d> at = (up, fw) -> base.add(UP.multiply(up)).add(f.multiply(fw));
			Vec3d waist = at.apply(6.0, 0.0), chest = at.apply(22.0, 1.5), neck = at.apply(30.0, 3.0 - shriek * 2.0);
			float veinPulse = 0.25F + 0.2F * MathHelper.sin(a * 0.4F);
			// Roots spreading from the waist into the ground.
			for (int k = 0; k < 12; k++) {
				double ang = k * MathHelper.TAU / 12 + HorrorShapes.hash(k, 3);
				Vec3d end = o.add(Math.cos(ang) * (10 + 6 * HorrorShapes.hash(k, 4)), -1, Math.sin(ang) * (10 + 6 * HorrorShapes.hash(k, 4)));
				Vec3d mid = waist.lerp(end, 0.5).add(0, -2, 0);
				Solids.tube(solid, m, waist, mid, 1.2 * sc, 0.8 * sc, 6, BODY, 1.0F, true);
				Solids.tube(solid, m, mid, end, 0.8 * sc, 0.1, 6, BODY, 1.0F, true);
			}
			// Blood pouring out of its ribcage and running down to the pool.
			Vec3d gash = at.apply(16.0, 3.2);
			for (int k = -1; k <= 1; k++) {
				Vec3d top = gash.add(r.multiply(k * 1.2));
				double wob = Math.sin(a * 0.3 + k) * 0.2;
				FxDraw.ribbon(shade, m, top, top.add(f.multiply(1.5 + wob)).add(0, -16 * sc, 0), Vec3d.ZERO, 0.7 - Math.abs(k) * 0.2, BLOOD, 0.95F, 0.8F);
				FxDraw.ribbon(glow, m, top, top.add(f.multiply(1.5 + wob)).add(0, -16 * sc, 0), Vec3d.ZERO, 0.2, BLOOD_LIT, 0.35F, 0.2F);
			}
			// Spine and ribs.
			Solids.tube(solid, m, waist, chest, 2.0 * sc, 2.6 * sc, 8, BODY, 1.0F, true);
			Solids.tube(solid, m, chest, neck, 2.4 * sc, 1.2 * sc, 8, BODY, 1.0F, true);
			FxDraw.ribbon(glow, m, waist, neck, Vec3d.ZERO, 0.6, 0xFF2010, veinPulse, veinPulse);
			for (int i = 0; i < 6; i++) {
				double y = 11 + i * 2.2;
				for (int s = -1; s <= 1; s += 2) {
					Vec3d root = at.apply(y, 0.5), out = at.apply(y - 1.0, 3.2).add(r.multiply(s * (4.2 - Math.abs(i - 3) * 0.3)));
					Vec3d front = at.apply(y - 2.4, 4.2).add(r.multiply(s * 1.0));
					Solids.tube(solid, m, root, out, 0.45 * sc, 0.4 * sc, 5, BODY, 1.0F, true);
					Solids.tube(solid, m, out, front, 0.4 * sc, 0.2 * sc, 5, BODY, 1.0F, true);
				}
			}
			// Arms and long fingers.
			for (int s = -1; s <= 1; s += 2) {
				Vec3d sh = at.apply(26.0, 1.5).add(r.multiply(s * 7.0));
				Vec3d elHang = sh.add(r.multiply(s * 3)).add(0, -9, 0).add(f.multiply(1)), hHang = elHang.add(0, -9, 0).add(f.multiply(2.5));
				Vec3d elUp = sh.add(r.multiply(s * 7)).add(0, 5, 0).add(f.multiply(4)), hUp = elUp.add(r.multiply(s * 4)).add(0, 9, 0).add(f.multiply(2));
				Vec3d elSlam = sh.add(r.multiply(s * 4)).add(0, -6, 0).add(f.multiply(6)), hSlam = elSlam.add(0, -10, 0).add(f.multiply(4));
				Vec3d el = elHang.lerp(elUp, raise).lerp(elSlam, slam), hand = hHang.lerp(hUp, raise).lerp(hSlam, slam);
				Solids.tube(solid, m, chest.add(r.multiply(s * 2)), sh, 1.6 * sc, 1.3 * sc, 6, BODY, 1.0F, true);
				Solids.tube(solid, m, sh, el, 1.2 * sc, 0.9 * sc, 6, BODY, 1.0F, true);
				Solids.tube(solid, m, el, hand, 0.9 * sc, 0.7 * sc, 6, BODY, 1.0F, true);
				FxDraw.ribbon(glow, m, sh, el, Vec3d.ZERO, 0.35, 0xFF2010, veinPulse, veinPulse);
				FxDraw.ribbon(glow, m, el, hand, Vec3d.ZERO, 0.3, 0xFF2010, veinPulse, veinPulse);
				Vec3d along = hand.subtract(el).normalize();
				double curl = 0.25 + 0.9 * slam + 0.15 * Math.sin(a * 0.25 + s);
				for (int k = 0; k < 5; k++) {
					double spread = (k - 2) * 0.32;
					Vec3d side = r.multiply(s);
					Vec3d dir = along.multiply(Math.cos(spread)).add(side.multiply(Math.sin(spread))).normalize();
					Vec3d p = hand;
					double len = k == 0 || k == 4 ? 2.2 : 3.0;
					for (int j = 0; j < 3; j++) {
						double twitch = 0.2 * Math.sin(a * 0.9 + k * 1.7 + j);
						Vec3d bend = dir.add(UP.multiply(-curl * (j + 1) * 0.35 + twitch)).add(f.multiply(curl * 0.3)).normalize();
						Vec3d q = p.add(bend.multiply(len * (1 - j * 0.2)));
						Solids.tube(solid, m, p, q, (0.35 - j * 0.08) * sc, (0.27 - j * 0.08) * sc, 5, BODY, 1.0F, true);
						p = q;
						dir = bend;
					}
					// Its claws drip.
					FxDraw.ribbon(shade, m, p, p.add(0, -0.6, 0), Vec3d.ZERO, 0.18, BLOOD, 0.95F, 0.9F);
					if (k % 2 == 0) {
						drips(shade, m, p.add(0, -0.6, 0), a, k * 7 + s, 14, 0.04);
					}
				}
			}
			// The crown of tendrils, writhing.
			for (int k = 0; k < 14; k++) {
				double ang = k * MathHelper.TAU / 14;
				Vec3d p = at.apply(24.0 + 4 * HorrorShapes.hash(k, 9), -1.5).add(r.multiply(Math.cos(ang) * 4)).add(f.multiply(Math.sin(ang) * 1.5 - 1.5));
				Vec3d dir = UP.multiply(0.8).add(r.multiply(Math.cos(ang) * 0.6)).add(f.multiply(-0.5)).normalize();
				for (int j = 0; j < 8; j++) {
					double wv = Math.sin(a * 0.12 + k * 1.3 + j * 0.7) * 0.5;
					Vec3d d2 = dir.add(r.multiply(wv)).add(f.multiply(Math.cos(a * 0.1 + k + j) * 0.3)).normalize();
					Vec3d q = p.add(d2.multiply(2.4));
					Solids.tube(solid, m, p, q, (0.5 - j * 0.055) * sc, (0.45 - j * 0.055) * sc, 5, BODY, 1.0F, true);
					p = q;
					dir = d2;
				}
			}
			// The head: a long dark skull with the mask over its face, thrown back as it shrieks.
			Vec3d tilt = f.multiply(Math.cos(shriek * 0.7)).add(UP.multiply(Math.sin(shriek * 0.7))).normalize();
			Vec3d upH = UP.multiply(Math.cos(shriek * 0.7)).subtract(f.multiply(Math.sin(shriek * 0.7))).normalize();
			Vec3d head = neck.add(upH.multiply(3.2));
			Solids.sphere(solid, m, head.subtract(tilt.multiply(0.8)), 3.0 * sc, BODY, 8, 10, true);
			// A long skull swept back behind the mask.
			Solids.tube(solid, m, head.add(upH.multiply(0.6)), head.add(upH.multiply(2.2)).subtract(tilt.multiply(6.5)), 2.6 * sc, 0.6 * sc, 8, BODY, 1.0F, true);
			Vec3d face = head.add(tilt.multiply(2.6));
			FxDraw.disc(shade, m, face, r.multiply(1.0), upH.multiply(1.65), 2.4 * sc, MASK, 0.98F, 0.98F, 28);
			// Cracks across the mask.
			java.util.Random rnd = new java.util.Random(11);
			Vec3d front = face.add(tilt.multiply(0.05));
			for (int k = 0; k < 4; k++) {
				Vec3d p = front.add(r.multiply((rnd.nextDouble() - 0.5) * 2)).add(upH.multiply((rnd.nextDouble() - 0.2) * 2.5));
				for (int j = 0; j < 3; j++) {
					Vec3d q = p.add(r.multiply((rnd.nextDouble() - 0.5) * 1.4)).add(upH.multiply(-0.4 - rnd.nextDouble() * 0.8));
					FxDraw.ribbon(shade, m, p, q, Vec3d.ZERO, 0.12, 0x1A0A0A, 0.95F, 0.95F);
					p = q;
				}
			}
			// Empty eyes with red pinpoints, and a mouth that splits open when it shrieks.
			float eyeFl = 0.8F + 0.2F * MathHelper.sin(a * 1.3F);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d e = front.add(r.multiply(s * 0.95)).add(upH.multiply(0.7)).add(tilt.multiply(0.05));
				FxDraw.disc(shade, m, e, r.add(upH.multiply(s * -0.25)), upH.multiply(1.9), 0.62 * sc, 0x050202, 0.98F, 0.98F, 14);
				// It weeps blood: streams running down the mask and dripping off its chin.
				Vec3d tear0 = e.add(upH.multiply(-0.9)), tear1 = e.add(upH.multiply(-3.4)).add(r.multiply(s * 0.25)), tear2 = tear1.add(upH.multiply(-1.4));
				FxDraw.ribbon(shade, m, tear0, tear1, Vec3d.ZERO, 0.3, BLOOD, 0.97F, 0.95F);
				FxDraw.ribbon(shade, m, tear1, tear2, Vec3d.ZERO, 0.22, BLOOD, 0.95F, 0.7F);
				FxDraw.ribbon(glow, m, tear0, tear1, Vec3d.ZERO, 0.1, BLOOD_LIT, 0.3F, 0.3F);
				drips(shade, m, tear2, a, s * 31, 20, 0.05);
				Solids.flare(glow, m, e.add(tilt.multiply(0.1)), cb, 0.6, 0xFF2010, eyeFl);
				FxKit.halo(consumers, m, cb, e, 2.2, 0xFF1008, 0.45F * eyeFl);
			}
			// A wide stitched grin, which splits open when it shrieks.
			Vec3d prevG = null;
			for (int k = -6; k <= 6; k++) {
				double u = k / 6.0;
				Vec3d g = front.add(r.multiply(u * 1.7)).add(upH.multiply(-2.0 + u * u * 0.8)).add(tilt.multiply(0.06));
				if (prevG != null) {
					FxDraw.ribbon(shade, m, prevG, g, Vec3d.ZERO, 0.16, 0x050202, 0.98F, 0.98F);
				}
				if (k % 2 == 0 && Math.abs(k) < 6) {
					FxDraw.ribbon(shade, m, g.add(upH.multiply(0.28)), g.add(upH.multiply(-0.28)), Vec3d.ZERO, 0.08, 0x1A0A0A, 0.95F, 0.95F);
				}
				prevG = g;
			}
			// Blood drooling through the stitches.
			for (int k = -2; k <= 2; k += 2) {
				Vec3d lip = front.add(r.multiply(k * 0.55)).add(upH.multiply(-2.0 + k * k * 0.05));
				double len = 1.5 + 0.8 * Math.sin(a * 0.07 + k);
				FxDraw.ribbon(shade, m, lip, lip.add(upH.multiply(-len)), Vec3d.ZERO, 0.18, BLOOD, 0.95F, 0.85F);
				drips(shade, m, lip.add(upH.multiply(-len)), a, k + 40, 26, 0.035);
			}
			Vec3d mouthTop = front.add(upH.multiply(-1.9)), mouthBot = front.add(upH.multiply(-2.0 - shriek * 2.4));
			FxDraw.ribbon(shade, m, mouthTop, mouthBot, Vec3d.ZERO, 0.1 + shriek * 1.8, 0x050202, 0.98F, 0.98F);
			if (shriek > 0.1) {
				FxDraw.ribbon(glow, m, mouthTop.add(upH.multiply(-0.4)), mouthBot.add(upH.multiply(0.4)), Vec3d.ZERO, 0.3 + shriek * 0.5, 0x900808,
						(float) shriek * 0.3F, (float) shriek * 0.3F);
			}
		}
	}

	// ====================================================================== PUMPKIN HORSEMAN

	/**
	 * The horseman, made of particles: a horse of black smoke and ghost fire with a burning mane and tail, a rider of
	 * smoke with fire pouring from his neck, his cloak streaming off him in tatters, his scythe edged in green fire,
	 * and the jack-o'-lantern blazing in his raised hand. Every bone of them is a cloud of embers and smoke that
	 * flickers upward and streams off behind as he rides.
	 */
	static final class HorsemanFx extends FxEffect {
		private static final int SMOKE_DARK = 0x120A16, SMOKE_EDGE = 0x2A1630, FIRE = 0xFF7A20, GHOST = 0x70FF40, PUMPKIN = 0xFF8A10;
		private final Vec3d c;
		private final double rot;
		private final int seed;

		HorsemanFx(Vec3d c, Vec3d heading, int seed, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.rot = heading.lengthSquared() < 1.0E-4 ? 0 : Math.atan2(heading.z, heading.x);
			this.seed = seed;
		}

		/** Maps the horse's local frame (x forward, y up, z to its side) to camera-relative space. */
		private final class Frame {
			final Vec3d o, d, s;
			final double cosR, sinR, bob, scale;

			Frame(Vec3d cam, double a) {
				Vec3d p = Horseman.pos(a, rot);
				double gy = NewFx.groundY(c.x + p.x, c.z + p.z, c.y);
				this.o = new Vec3d(c.x + p.x, gy + p.y, c.z + p.z).subtract(cam);
				this.d = Horseman.dir(a, rot);
				this.s = new Vec3d(-d.z, 0, d.x);
				double rear = Horseman.rear(a) * 0.8;
				this.cosR = Math.cos(rear);
				this.sinR = Math.sin(rear);
				boolean onLand = a >= Horseman.GALLOP && a < Horseman.ASCEND && Horseman.rear(a) < 0.05;
				this.bob = onLand ? 0.9 * Math.abs(Math.sin(a * 0.32)) : 0;
				this.scale = Horseman.BIG * smooth((a - Horseman.APPEAR) / 25.0) * (1 - smooth((a - Horseman.VANISH) / 4.0));
			}

			Vec3d at(double x, double y, double z) {
				double dx = x + 6;
				double x2 = -6 + dx * cosR - y * sinR, y2 = dx * sinR + y * cosR;
				return o.add(d.multiply(x2 * scale)).add(UP.multiply((y2 + bob) * scale)).add(s.multiply(z * scale));
			}
		}

		/** Everything drawn this frame: glowing embers, and smoke puffs that are sorted and drawn together. */
		private static final class Cloud {
			final VertexConsumer glow;
			final Matrix4f m;
			final Vec3d[] cb;
			final float a;
			final List<Effects.Puff> puffs = new ArrayList<>();

			Cloud(VertexConsumer glow, Matrix4f m, Vec3d[] cb, float a) {
				this.glow = glow;
				this.m = m;
				this.cb = cb;
				this.a = a;
			}

			/**
			 * One limb as particles: a core of dark smoke along it, embers scattered through it flickering upward and
			 * fading, and flame licking off its edge.
			 */
			void limb(Vec3d p0, Vec3d p1, double r0, double r1, int salt, int ember, int edge, double density, Vec3d drift) {
				Vec3d axis = p1.subtract(p0);
				double len = axis.length();
				if (len < 1.0E-3) {
					return;
				}
				Vec3d dir = axis.multiply(1 / len);
				Vec3d side = dir.crossProduct(UP);
				side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
				Vec3d up = side.crossProduct(dir).normalize();
				int n = (int) Math.max(6, len * (r0 + r1) * density * 2.2);
				for (int i = 0; i < n; i++) {
					double u = HorrorShapes.hash(salt, i * 3 + 1);
					double rr = (r0 + (r1 - r0) * u) * Math.sqrt(HorrorShapes.hash(salt, i * 3 + 2));
					double ang = HorrorShapes.hash(salt, i * 3 + 3) * MathHelper.TAU + a * 0.04;
					double ph = (a * 0.07 + HorrorShapes.hash(salt, i + 999)) % 1.0;
					Vec3d p = p0.add(axis.multiply(u)).add(side.multiply(Math.cos(ang) * rr)).add(up.multiply(Math.sin(ang) * rr))
							.add(0, ph * 1.4, 0).add(drift.multiply(ph * 2.5));
					float life = (float) (1 - ph);
					if (i % 7 == 0) {
						puffs.add(new Effects.Puff(p, (r0 + r1) * 0.22 + 0.25, SMOKE_EDGE, SMOKE_DARK, 0.28F * life));
					} else {
						boolean outer = rr > (r0 + r1) * 0.32;
						double size = 0.22 + 0.25 * HorrorShapes.hash(salt, i + 77);
						Solids.dot(glow, m, p, cb, size, outer ? edge : ember, 0.95F * life);
						if (i % 4 == 1) {
							Solids.dot(glow, m, p, cb, size * 0.4, 0xFFF0C0, 0.8F * life);
						}
					}
				}
			}

			/** A licking tongue of flame from {@code root} along {@code dir}, drawn as embers streaming off it. */
			void flame(Vec3d root, Vec3d dir, double len, int salt, int col) {
				for (int k = 0; k < 14; k++) {
					double ph = (a * 0.09 + HorrorShapes.hash(salt, k)) % 1.0;
					double wv = Math.sin(a * 0.5 + salt + k) * 0.5 * ph;
					Vec3d p = root.add(dir.multiply(len * ph)).add(UP.multiply(wv)).add(dir.crossProduct(UP).multiply(Math.cos(a * 0.4 + k) * 0.6 * ph));
					Solids.dot(glow, m, p, cb, 0.55 * (1 - ph) + 0.15, k % 3 == 0 ? 0xFFE080 : col, 0.9F * (float) (1 - ph));
				}
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t % 4 == 0 && t < Horseman.END - 30) {
				Vec3d eye = camera(c);
				FxEngine.play(VfxIds.HORSE_MIST, eye.x, NewFx.groundY(eye.x, eye.z, eye.y - 1), eye.z, 0, 1, 0, 1.0F, 0, t * 613L);
			}
			for (int k = 0; k < Horseman.THROWS; k++) {
				int at = Horseman.throwAt(k);
				if (t >= at && t < at + Horseman.FLIGHT) {
					Vec3d p = lantern(k, t), q = lantern(k, t + 1);
					FxEngine.playMoving(VfxIds.HORSE_TRAIL, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 131L + k, (float) (q.x - p.x), (float) (q.y - p.y),
							(float) (q.z - p.z));
					if (t % 3 == 0) {
						ClientFx.light(p, 0.7F, 0xFF8020);
					}
				}
			}
			if (t >= Horseman.APPEAR && t < Horseman.VANISH) {
				Vec3d p = Horseman.pos(t, rot), q = Horseman.pos(t - 1, rot);
				double gy = NewFx.groundY(c.x + p.x, c.z + p.z, c.y);
				Vec3d body = new Vec3d(c.x + p.x, gy + p.y + 12 * Horseman.BIG, c.z + p.z);
				// Embers shed off his back as he rides.
				FxEngine.playMoving(VfxIds.HORSE_TRAIL, body.x, body.y, body.z, 0, 1, 0, 1.2F, 0, t * 977L, (float) (p.x - q.x) * 0.3F, 0.1F,
						(float) (p.z - q.z) * 0.3F);
				if (t % 5 == 0) {
					ClientFx.light(body.add(0, Horseman.HAND - 12 * Horseman.BIG, 0), 0.9F, 0xFF8020);
				}
			}
			return super.tick(world);
		}

		/** World position of thrown lantern {@code k} at tick {@code t}: an arc from his hand to where it lands. */
		private Vec3d lantern(int k, double t) {
			int at = Horseman.throwAt(k);
			Vec3d p = Horseman.pos(at, rot);
			Vec3d from = new Vec3d(c.x + p.x, NewFx.groundY(c.x + p.x, c.z + p.z, c.y) + Horseman.HAND, c.z + p.z);
			Vec3d tg = Horseman.target(k, seed, rot);
			Vec3d to = new Vec3d(c.x + tg.x, NewFx.groundY(c.x + tg.x, c.z + tg.z, c.y) + 1, c.z + tg.z);
			double u = MathHelper.clamp((t - at) / Horseman.FLIGHT, 0.0, 1.0);
			return from.lerp(to, u).add(0, Math.sin(Math.PI * u) * 22, 0);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Cloud cl = new Cloud(glow, m, cb, a);
			for (int k = 0; k < Horseman.THROWS; k++) {
				int at = Horseman.throwAt(k);
				if (a >= at && a < at + Horseman.FLIGHT) {
					Vec3d p = lantern(k, a).subtract(cam);
					jackOLantern(consumers, cl, p, new Vec3d(Math.cos(a * 0.4), 0, Math.sin(a * 0.4)), 2.6, a);
				}
			}
			if (a >= Horseman.APPEAR && a <= Horseman.VANISH + 2) {
				Frame f = new Frame(cam, a);
				if (f.scale > 0.02) {
					horse(f, cl, a);
					rider(f, consumers, cl, a);
				}
			}
			Effects.drawPuffs(cl.puffs, consumers.getBuffer(FxRenderLayers.SHADE), m, cb, 6);
		}

		private void horse(Frame f, Cloud cl, float a) {
			double sc = f.scale;
			Vec3d back = f.d.multiply(-1);
			// Body, neck and head.
			cl.limb(f.at(-8.5, 11.8, 0), f.at(-3, 11.6, 0), 3.6 * sc, 4.2 * sc, 1, 0xC02A0C, FIRE, 1.1, back);
			cl.limb(f.at(-3, 11.6, 0), f.at(3, 11.8, 0), 4.2 * sc, 4.3 * sc, 2, 0xC02A0C, FIRE, 1.1, back);
			cl.limb(f.at(3, 11.8, 0), f.at(7.5, 12.6, 0), 4.3 * sc, 3.4 * sc, 3, 0xC02A0C, FIRE, 1.1, back);
			Vec3d n0 = f.at(6.5, 14.0, 0), n1 = f.at(9.5, 18.5, 0), n2 = f.at(11.5, 21.0, 0), muzzle = f.at(15.8, 17.2, 0);
			cl.limb(n0, n1, 2.8 * sc, 2.2 * sc, 4, 0xC02A0C, FIRE, 1.2, back);
			cl.limb(n1, n2, 2.2 * sc, 1.9 * sc, 5, 0xC02A0C, FIRE, 1.2, back);
			cl.limb(n2, muzzle, 1.9 * sc, 1.1 * sc, 6, 0xC02A0C, FIRE, 1.4, back);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d eye = f.at(12.6, 20.6, s * 1.35);
				Solids.flare(cl.glow, cl.m, eye, cl.cb, 1.2 * sc, 0xFFA030, 1.0F);
				cl.limb(f.at(11.2, 22.0, s * 0.8), f.at(10.6, 24.5, s * 0.9), 0.5 * sc, 0.15 * sc, 7 + s, FIRE, FIRE, 3, back);
			}
			// Fire snorted from the nostrils.
			cl.flame(muzzle, f.d.add(0, -0.3, 0).normalize(), 5 * sc, 9, FIRE);
			// Legs at the gallop.
			double ph = a * 0.32, rear = Horseman.rear(a);
			double[][] legs = {{-6.5, 2.4, 0.0}, {-6.5, -2.4, 0.13}, {5.5, 2.4, 0.5}, {5.5, -2.4, 0.63}};
			int li = 0;
			for (double[] leg : legs) {
				boolean front = leg[0] > 0;
				double w = ph + leg[2] * MathHelper.TAU;
				double al = 0.6 * Math.sin(w), be = al * 0.4 + (front ? 0.5 : -0.5) * (0.5 + 0.5 * Math.sin(w + 1.2));
				if (front) {
					al = al * (1 - rear) + rear * (-1.3 + 0.3 * Math.sin(a * 0.4 + leg[1]));
					be = be * (1 - rear) + rear * 1.4;
				} else {
					al *= 1 - rear * 0.8;
				}
				if (a < Horseman.GALLOP || a >= Horseman.ASCEND) {
					al *= 0.6;
				}
				Vec3d hip = f.at(leg[0], 9.0, leg[1]);
				double kx = leg[0] + Math.sin(al) * 4.4, ky = 9.0 - Math.cos(al) * 4.4;
				Vec3d knee = f.at(kx, ky, leg[1]);
				Vec3d hoof = f.at(kx + Math.sin(be) * 4.4, ky - Math.cos(be) * 4.4, leg[1]);
				cl.limb(hip, knee, 1.4 * sc, 1.0 * sc, 20 + li, 0xC02A0C, FIRE, 1.6, back);
				cl.limb(knee, hoof, 0.9 * sc, 0.75 * sc, 30 + li, 0xC02A0C, FIRE, 1.8, back);
				Solids.dot(cl.glow, cl.m, hoof, cl.cb, 1.3 * sc, 0xFF9030, 0.8F);
				li++;
			}
			// The mane and the tail: ghost fire streaming back.
			for (int k = 0; k < 9; k++) {
				double u = k / 8.0;
				cl.flame(f.at(6.0 + u * 5.5, 15.5 + u * 6.0, 0), back.add(UP.multiply(0.9)).normalize(), (4 + 2 * Math.sin(k)) * sc, 40 + k, k % 3 == 0 ? GHOST : FIRE);
			}
			for (int k = 0; k < 4; k++) {
				cl.flame(f.at(-8.8, 13.0, 0), back.add(UP.multiply(0.2 + k * 0.12)).normalize(), (8 + k) * sc, 60 + k, k % 2 == 0 ? GHOST : FIRE);
			}
		}

		private void rider(Frame f, VertexConsumerProvider consumers, Cloud cl, float a) {
			double sc = f.scale;
			Vec3d back = f.d.multiply(-1);
			int robe = 0xB040FF;
			Vec3d hip = f.at(-1.0, 16.4, 0), chest = f.at(0.6, 22.5, 0), shoulders = f.at(0.8, 25.6, 0), stump = f.at(0.9, 27.0, 0);
			cl.limb(hip, chest, 2.2 * sc, 2.6 * sc, 70, robe, GHOST, 1.2, back);
			cl.limb(chest, shoulders, 2.6 * sc, 2.2 * sc, 71, robe, GHOST, 1.2, back);
			cl.limb(shoulders, stump, 1.4 * sc, 1.0 * sc, 72, robe, GHOST, 1.5, back);
			// Ghost fire pouring up from where his head should be.
			for (int k = 0; k < 6; k++) {
				Vec3d dir = UP.add(back.multiply(0.35)).add(f.s.multiply((k - 2.5) * 0.08)).normalize();
				cl.flame(stump, dir, (6 + k % 3) * sc, 80 + k, k % 2 == 0 ? GHOST : 0xC0FF80);
			}
			FxKit.halo(consumers, cl.m, cl.cb, stump.add(0, 2 * sc, 0), 4.5 * sc, 0x60FF40, 0.35F);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d knee = f.at(2.0, 13.0, s * 4.4), foot = f.at(0.8, 8.5, s * 4.6);
				cl.limb(f.at(-0.8, 16.2, s * 1.6), knee, 1.2 * sc, 1.0 * sc, 90 + s, robe, GHOST, 1.5, back);
				cl.limb(knee, foot, 0.9 * sc, 0.8 * sc, 95 + s, robe, GHOST, 1.5, back);
			}
			// The lantern arm.
			double wind = 0, release = 0, regrow = 1;
			for (int k = 0; k < Horseman.THROWS; k++) {
				double at = Horseman.throwAt(k);
				wind = Math.max(wind, smooth((a - at + 16) / 12.0) * (1 - smooth((a - at + 2) / 2.0)));
				release = Math.max(release, smooth((a - at + 2) / 3.0) * (1 - smooth((a - at - 10) / 12.0)));
				if (a >= at && a < at + 34) {
					regrow = smooth((a - at - 18) / 16.0);
				}
			}
			Vec3d sh = f.at(0.8, 25.2, 2.8);
			Vec3d elbow = f.at(1.2 - wind * 3 + release * 3, 29.0, 4.0);
			Vec3d hand = f.at(1.6 - wind * 5.5 + release * 6, 33.0 - wind * 1.5, 4.4);
			cl.limb(sh, elbow, 1.0 * sc, 0.8 * sc, 100, robe, GHOST, 1.8, back);
			cl.limb(elbow, hand, 0.8 * sc, 0.65 * sc, 101, robe, GHOST, 1.8, back);
			if (regrow > 0.02) {
				jackOLantern(consumers, cl, hand.add(UP.multiply(1.8 * sc * regrow)), f.d, 2.4 * sc * regrow, a);
			}
			// The scythe arm and the scythe, its blade edged in green fire.
			Vec3d sh2 = f.at(0.8, 25.2, -2.8), el2 = f.at(0.0, 21.5, -4.4), hand2 = f.at(2.4, 20.8, -4.8);
			cl.limb(sh2, el2, 1.0 * sc, 0.8 * sc, 102, robe, GHOST, 1.8, back);
			cl.limb(el2, hand2, 0.8 * sc, 0.65 * sc, 103, robe, GHOST, 1.8, back);
			double swing = a >= Horseman.CHARGE && a < Horseman.ASCEND ? Math.sin((a - Horseman.CHARGE) * 0.5) * 6 : 0;
			Vec3d butt = f.at(-5.0, 14.0 - swing * 0.3, -5.0), top = f.at(9.0, 33.0 + swing * 0.2, -5.2 + swing * 0.3);
			cl.limb(butt, top, 0.35 * sc, 0.3 * sc, 104, 0x8A6034, 0x8A6034, 4, Vec3d.ZERO);
			Vec3d prev = top;
			for (int j = 1; j <= 10; j++) {
				double u = j / 10.0;
				Vec3d p = top.add(f.d.multiply((-u * 8.0 + u * u * 2.0) * sc)).add(UP.multiply((-u * u * 4.5) * sc)).add(f.s.multiply(-0.3 * sc));
				FxDraw.ribbon(cl.glow, cl.m, prev, p, Vec3d.ZERO, (0.7 - u * 0.5) * sc, 0xB0FF90, 0.75F, 0.75F);
				Solids.dot(cl.glow, cl.m, p.add(UP.multiply(-0.6 * sc)), cl.cb, 0.5 * sc, GHOST, 0.6F + 0.3F * MathHelper.sin(a * 0.6F + j));
				prev = p;
			}
			// The cloak: rags of smoke streaming back from his shoulders and fraying away.
			for (int k = 0; k < 9; k++) {
				double z = -3.2 + k * 0.8;
				Vec3d p = f.at(-0.6, 25.6, z);
				for (int j = 0; j < 8; j++) {
					double flutter = Math.sin(a * 0.35 + j * 0.9 + k * 0.7) * 0.8 * j / 8.0;
					Vec3d q = f.at(-0.6 - j * 1.8, 25.6 - j * 1.3 + flutter, z * (1 + j * 0.09));
					float fade = 1 - j / 8.0F;
					cl.puffs.add(new Effects.Puff(q, (0.9 + j * 0.12) * sc, SMOKE_EDGE, SMOKE_DARK, 0.5F * fade));
					if ((j + k) % 3 == 0) {
						Solids.dot(cl.glow, cl.m, q, cl.cb, 0.25 * sc, robe, 0.6F * fade);
					}
					p = q;
				}
			}
		}

		/** A blazing jack-o'-lantern made of fire: a ball of flame, its carved face glowing gold, a halo round it. */
		private static void jackOLantern(VertexConsumerProvider consumers, Cloud cl, Vec3d p, Vec3d face, double r, float a) {
			if (r < 0.05) {
				return;
			}
			Vec3d side = new Vec3d(-face.z, 0, face.x);
			for (int i = 0; i < 90; i++) {
				double th = HorrorShapes.hash(i, 201) * MathHelper.TAU, ph = Math.acos(2 * HorrorShapes.hash(i, 202) - 1);
				double flick = 1 + 0.12 * Math.sin(a * 0.9 + i);
				Vec3d n = new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph) * 0.85, Math.sin(ph) * Math.sin(th));
				Vec3d q = p.add(n.multiply(r * flick));
				// Leave the face's cut-outs dark.
				double fwd = n.dotProduct(face);
				if (fwd > 0.55) {
					continue;
				}
				Solids.dot(cl.glow, cl.m, q, cl.cb, 0.45 * r / 2.4, i % 4 == 0 ? 0xFFC040 : PUMPKIN, 0.9F);
			}
			cl.puffs.add(new Effects.Puff(p, r * 0.9, 0x6A2A08, 0x3A1404, 0.7F));
			Vec3d front = p.add(face.multiply(r * 0.95));
			float fl = 0.8F + 0.2F * MathHelper.sin(a * 1.7F);
			for (int s = -1; s <= 1; s += 2) {
				Solids.flare(cl.glow, cl.m, front.add(side.multiply(s * r * 0.38)).add(UP.multiply(r * 0.22)), cl.cb, r * 0.32, 0xFFF080, fl);
			}
			Vec3d prev = null;
			for (int k = -4; k <= 4; k++) {
				double u = k / 4.0;
				Vec3d q = front.add(side.multiply(u * r * 0.6)).add(UP.multiply(-r * 0.32 + u * u * r * 0.12 + (k % 2 == 0 ? 0 : -r * 0.1)));
				if (prev != null) {
					FxDraw.ribbon(cl.glow, cl.m, prev, q, Vec3d.ZERO, r * 0.16, 0xFFE060, fl, fl);
				}
				prev = q;
			}
			cl.flame(p.add(UP.multiply(r * 0.8)), UP, r * 1.4, 300, PUMPKIN);
			FxKit.halo(consumers, cl.m, cl.cb, p, r * 2.8, 0xFF7020, 0.45F * fl);
		}
	}

	// ====================================================================== PORTAL OF HELL

	/**
	 * The portal: a vortex of hellfire filling the arch with an eye in it that follows you, fireballs flying out,
	 * lava pouring over the sill, and the arm that reaches through.
	 */
	static final class HellGateFx extends FxEffect {
		private static final int ARM = 0x7A2016;
		private static final double K = 1.7;
		private final Vec3d c, f, s;
		private final int seed;

		HellGateFx(Vec3d c, Vec3d facing, int seed, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.f = facing.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : facing.normalize();
			this.s = new Vec3d(-f.z, 0, f.x);
			this.seed = seed;
		}

		private Vec3d local(double u, double v, double w) {
			return c.add(s.multiply(u)).add(0, v, 0).add(f.multiply(w));
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t % 3 == 0 && t < HellGate.END - 30) {
				Vec3d eye = camera(c);
				FxEngine.play(VfxIds.HELLGATE_HEAT, eye.x, NewFx.groundY(eye.x, eye.z, eye.y - 1), eye.z, 0, 1, 0, 1.0F, 0, t * 433L);
			}
			if (t >= HellGate.ARM && t < HellGate.ARM_END && t % 2 == 0) {
				// Fire wreathing the arm.
				Vec3d hl = HellGate.hand(t);
				Vec3d hand = local(hl.x, hl.y, hl.z), sh = local(4, HellGate.EYE_Y + 2, -14);
				for (int k = 0; k < 3; k++) {
					Vec3d p = sh.lerp(hand, 0.35 + k * 0.3);
					FxEngine.play(VfxIds.HELLGATE_FIRE_TRAIL, p.x, p.y, p.z, 0, 1, 0, 0.8F, 0, t * 131L + k);
				}
			}
			if (t >= HellGate.LAVA && t < HellGate.IMPLODE && t % 4 == 0) {
				Vec3d foot = local(0, 0, 3.5);
				FxEngine.play(VfxIds.HELLGATE_LAVA_SPLASH, foot.x, NewFx.groundY(foot.x, foot.z, c.y), foot.z, (float) f.x, 0, (float) f.z, 1.0F, 0, t * 71L);
			}
			if (HellGate.open(t) > 0.05 && t % 6 == 0) {
				ClientFx.light(local(0, HellGate.EYE_Y, 3), 1.0F, 0xFF5010);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			double open = HellGate.open(a);
			if (open > 0.005) {
				vortex(glow, shade, m, cam, a, open);
				eye(consumers, glow, shade, m, cb, cam, a, open);
			}
			if (a >= HellGate.LAVA && a < HellGate.IMPLODE + 20) {
				lavafall(glow, shade, m, cam, a);
			}
			if (a >= HellGate.ARM && a < HellGate.ARM_END) {
				arm(consumers, solid, glow, shade, m, cb, cam, a);
			}
		}

		/** The arch filled with a whirling vortex of hellfire, black at its rim and white-hot at its heart. */
		private void vortex(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d cam, float a, double open) {
			double step = 1.6;
			Vec3d ctr = local(0, HellGate.EYE_Y, 0);
			double lid = smooth((a - HellGate.EYE) / 25.0) * (1 - smooth((a - HellGate.CLOSE) / 20.0));
			for (double u = -HellGate.W; u < HellGate.W; u += step) {
				for (double v = 0; v < HellGate.APEX; v += step) {
					double[][] q = {{u, v}, {u + step, v}, {u + step, v + step}, {u, v + step}};
					boolean any = false;
					for (double[] p : q) {
						any |= HellGate.opening(p[0], p[1]);
					}
					if (!any) {
						continue;
					}
					Vec3d[] w = new Vec3d[4];
					float[] al = new float[4];
					int[] col = new int[4];
					for (int i = 0; i < 4; i++) {
						double uu = q[i][0], vv = q[i][1];
						boolean in = HellGate.opening(uu, vv);
						double du = uu, dv = vv - HellGate.EYE_Y;
						double r = Math.hypot(du, dv), th = Math.atan2(dv, du);
						// The swirl: bands of fire turning inward.
						double band = 0.5 + 0.5 * Math.sin(th * 3 - a * 0.12 + r * 0.35) * Math.sin(r * 0.5 - a * 0.2 + th);
						double heat = Math.max(0, 1 - r / (HellGate.W * 1.4)) * 0.7 + band * 0.5;
						col[i] = heat > 0.85 ? 0xFFE8A0 : heat > 0.6 ? 0xFF9030 : heat > 0.35 ? 0xE03810 : 0x701008;
						double eyeCut = lid * MathHelper.clamp((HellGate.EYE_R * 1.25 - Math.hypot(du, dv / Math.max(0.3, lid * 0.62))) / 3.0, 0.0, 1.0);
						al[i] = in ? (float) (open * (0.25 + 0.55 * heat) * (1 - eyeCut)) : 0;
						w[i] = local(uu, vv, -0.2 + Math.sin(r * 0.4 - a * 0.15) * 0.6 * (1 - r / 40)).subtract(cam);
					}
					for (int i = 0; i < 4; i++) {
						// Dark base so the vortex hides what is behind the arch.
						float d = al[i] > 0 ? (float) open * 0.92F : 0;
						FxDraw.vertex(shade, m, w[i].x, w[i].y, w[i].z, 0x1A0402, d);
					}
					for (int i = 0; i < 4; i++) {
						FxDraw.vertex(glow, m, w[i].x, w[i].y, w[i].z, col[i], al[i]);
					}
				}
			}
			FxDraw.ring(glow, m, ctr.subtract(cam), s, UP, 2, HellGate.W * 0.9, 0xFFB050, (float) open * 0.2F, (float) open * 0.1F, 0.0F, 40);
		}

		/**
		 * The eye: a bloodshot amber eye under heavy lids of scorched flesh with spines for lashes. A burning iris and
		 * a slit pupil dart about and lock on to you, the pupil widening when it does; it blinks, and at the end the
		 * lids close for good.
		 */
		private void eye(VertexConsumerProvider consumers, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a, double open) {
			double lid = smooth((a - HellGate.EYE) / 25.0) * (1 - smooth((a - HellGate.CLOSE) / 20.0));
			double blink = Math.max(0, Math.sin(a * 0.05) - 0.97) / 0.03;
			double squint = 0.15 * smooth((a - HellGate.ARM) / 20.0) * (1 - smooth((a - HellGate.WITHDRAW) / 20.0));
			double o = Math.max(0, lid * (1 - blink) - squint);
			Vec3d ctr = local(0, HellGate.EYE_Y, 0.6).subtract(cam);
			double R = HellGate.EYE_R;
			if (lid > 0.02) {
				FxKit.halo(consumers, m, cb, ctr, R * 2.4, 0xFF3008, (float) (0.25 * lid * open));
			}
			if (o > 0.02) {
				// Sclera, yellowed, with bloodshot veins creeping in from its rim.
				FxDraw.disc(shade, m, ctr, s, UP.multiply(o * 0.62), R, 0xE8B048, 0.98F, 0.98F, 40);
				for (int k = 0; k < 14; k++) {
					double ang = k * MathHelper.TAU / 14 + HorrorShapes.hash(k, 5) * 0.3;
					Vec3d prev = ctr.add(s.multiply(Math.cos(ang) * R * 0.98)).add(UP.multiply(Math.sin(ang) * R * 0.98 * o * 0.62)).add(f.multiply(0.04));
					for (int j = 1; j <= 4; j++) {
						double rr = R * (0.98 - j * 0.12), aa = ang + Math.sin(j * 1.7 + k) * 0.12;
						Vec3d q = ctr.add(s.multiply(Math.cos(aa) * rr)).add(UP.multiply(Math.sin(aa) * rr * o * 0.62)).add(f.multiply(0.04));
						FxDraw.ribbon(shade, m, prev, q, Vec3d.ZERO, 0.22 - j * 0.04, 0xA01008, 0.9F, 0.9F);
						prev = q;
					}
				}
				// Where it looks: darting about, then fixing on the camera.
				Vec3d toCam = ctr.multiply(-1).normalize();
				double dart = Math.sin(a * 0.11) * Math.cos(a * 0.037);
				double lock = 0.5 + 0.5 * Math.sin(a * 0.02);
				double lu = MathHelper.clamp(toCam.dotProduct(s) * lock + dart * 0.5 * (1 - lock), -0.55, 0.55);
				double lv = MathHelper.clamp(toCam.y * lock + Math.cos(a * 0.09) * 0.3 * (1 - lock), -0.45, 0.45);
				Vec3d iris = ctr.add(s.multiply(lu * R * 0.5)).add(UP.multiply(lv * R * 0.3 * o)).add(f.multiply(0.08));
				float fl = 0.85F + 0.15F * MathHelper.sin(a * 0.4F);
				FxDraw.disc(shade, m, iris, s, UP.multiply(o * 0.62), R * 0.52, 0xB81808, 0.98F, 0.98F, 32);
				FxDraw.disc(glow, m, iris.add(f.multiply(0.02)), s, UP.multiply(o * 0.62), R * 0.52, 0xFF7020, 0.15F * fl, 0.75F * fl, 32);
				FxDraw.ring(glow, m, iris.add(f.multiply(0.03)), s, UP.multiply(o * 0.62), R * 0.44, R * 0.52, 0xFFD040, 0.0F, 0.8F * fl, 0.0F, 32);
				double dilate = 0.18 + 0.16 * lock;
				FxDraw.disc(shade, m, iris.add(f.multiply(0.06)), s.multiply(dilate), UP.multiply(o * 0.6), R * 0.95, 0x050101, 0.99F, 0.99F, 24);
				Solids.flare(glow, m, ctr.add(s.multiply(-R * 0.4)).add(UP.multiply(R * 0.22 * o)).add(f.multiply(0.2)), cb, 1.1, 0xFFFFFF, 0.7F);
			}
			if (lid > 0.02) {
				// The lids: scorched flesh closing over it, spines for lashes.
				for (int up = -1; up <= 1; up += 2) {
					double edge = o * 0.62 * R;
					Vec3d prev = null;
					for (int k = 0; k <= 16; k++) {
						double u = k / 16.0 * 2 - 1;
						double y = up * Math.sqrt(Math.max(0, 1 - u * u)) * edge;
						Vec3d p = ctr.add(s.multiply(u * R * 1.05)).add(UP.multiply(y)).add(f.multiply(0.25));
						Vec3d outer = ctr.add(s.multiply(u * R * 1.25)).add(UP.multiply(up * (Math.sqrt(Math.max(0, 1 - u * u)) * R * 0.95 + 1.2))).add(f.multiply(0.15));
						if (prev != null) {
							Solids.quad(shade, m, prev, p, outer, outer, 0x3A0C08, 0.97F);
						}
						FxDraw.ribbon(shade, m, p, outer, Vec3d.ZERO, 1.4, 0x3A0C08, 0.97F, 0.97F);
						if (k % 2 == 0 && k > 0 && k < 16) {
							Vec3d lash = p.add(UP.multiply(up * 2.2)).add(s.multiply(u * 0.8)).add(f.multiply(1.2));
							Solids.tube(shade, m, p, lash, 0.25, 0.02, 4, 0x140404, 0.95F, false);
						}
						prev = p;
					}
				}
			}
		}

		/** Lava pouring over the sill and spreading out in front of the gate. */
		private void lavafall(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d cam, float a) {
			float in = smooth((a - HellGate.LAVA) / 20.0) * (1 - smooth((a - HellGate.IMPLODE) / 20.0));
			for (int k = -3; k <= 3; k++) {
				Vec3d top = local(k * 1.2, 1.0, 1.5).subtract(cam), bot = local(k * 1.4, 0.2, 3.5 + Math.abs(k) * 0.3).subtract(cam);
				float fl = 0.75F + 0.25F * MathHelper.sin(a * 0.4F + k);
				FxDraw.ribbon(shade, m, top, bot, Vec3d.ZERO, 1.6, 0x601008, 0.8F * in, 0.8F * in);
				FxDraw.ribbon(glow, m, top, bot, Vec3d.ZERO, 1.2, 0xFF7020, 0.7F * in * fl, 0.7F * in * fl);
			}
		}

		/**
		 * The arm: hidden at the shoulder behind the portal, rippling the fire where it comes through. A huge,
		 * corded forearm of crimson hide split by glowing magma cracks, a ridge of bone spines down its back, a spiked
		 * elbow; a hand with knuckle spikes and long black claws that open as it comes down and close on the ground.
		 */
		private void arm(VertexConsumerProvider consumers, VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam,
				float a) {
			Vec3d hl = HellGate.hand(a);
			Vec3d hand = local(hl.x, hl.y, hl.z).subtract(cam);
			Vec3d shoulder = local(4, HellGate.EYE_Y + 2, -14).subtract(cam);
			Vec3d mid = shoulder.lerp(hand, 0.5);
			Vec3d elbow = mid.add(UP.multiply(6)).add(s.multiply(7));
			Vec3d upper = elbow.subtract(shoulder), fore = hand.subtract(elbow);
			Vec3d fd = fore.normalize(), ud = upper.normalize();
			Vec3d fside = fd.crossProduct(UP);
			fside = fside.lengthSquared() < 1.0E-4 ? s : fside.normalize();
			Vec3d fup = fside.crossProduct(fd).normalize();
			int hide = 0x6A1410, dark = 0x2A0806;
			// Upper arm: the biceps bulging.
			Solids.tube(solid, m, shoulder, shoulder.add(upper.multiply(0.5)), 6.5, 8.4, 12, hide, 1.0F, true);
			Solids.tube(solid, m, shoulder.add(upper.multiply(0.5)), elbow, 8.4, 6.2, 12, hide, 1.0F, true);
			Solids.sphere(solid, m, elbow, 6.6, hide, 10, 12, true);
			// The forearm: two corded muscle bundles twisting round each other, wide near the elbow.
			for (int k = 0; k < 2; k++) {
				Vec3d off0 = fside.multiply(k == 0 ? 2.2 : -2.2), off1 = fside.multiply(k == 0 ? -1.2 : 1.2).add(fup.multiply(k == 0 ? 0.8 : -0.8));
				Vec3d b0 = elbow.add(off0), b1 = elbow.add(fore.multiply(0.45)).add(off0.lerp(off1, 0.5).multiply(1.15)), b2 = hand.subtract(fd.multiply(3)).add(off1);
				Solids.tube(solid, m, b0, b1, 4.6, 5.0, 10, k == 0 ? hide : 0x5A100C, 1.0F, true);
				Solids.tube(solid, m, b1, b2, 5.0, 3.4, 10, k == 0 ? hide : 0x5A100C, 1.0F, true);
			}
			Solids.tube(solid, m, hand.subtract(fd.multiply(4)), hand, 4.0, 4.4, 10, hide, 1.0F, true);
			// Magma cracks glowing through the hide, pulsing.
			float pulse = 0.55F + 0.35F * MathHelper.sin(a * 0.3F);
			for (int k = 0; k < 9; k++) {
				double u0 = 0.1 + HorrorShapes.hash(k, 11) * 0.8, ang = HorrorShapes.hash(k, 12) * MathHelper.TAU;
				Vec3d p = elbow.add(fore.multiply(u0)), out = fside.multiply(Math.cos(ang)).add(fup.multiply(Math.sin(ang)));
				Vec3d c0 = p.add(out.multiply(5.0)), c1 = c0.add(fd.multiply(3 + HorrorShapes.hash(k, 13) * 3)).add(out.multiply(-0.4)).add(fside.multiply(0.8));
				FxDraw.ribbon(glow, m, c0, c1, Vec3d.ZERO, 0.45, 0xFF6010, pulse, pulse * 0.6F);
			}
			for (int k = 0; k < 5; k++) {
				double ang = k * 1.3;
				Vec3d out = UP.multiply(Math.cos(ang)).add(s.multiply(Math.sin(ang)));
				Vec3d c0 = shoulder.add(upper.multiply(0.3 + k * 0.12)).add(out.multiply(8.0));
				FxDraw.ribbon(glow, m, c0, c0.add(ud.multiply(4)), Vec3d.ZERO, 0.5, 0xFF6010, pulse, pulse * 0.5F);
			}
			// A ridge of bone spines down the back of the forearm, and a great spike off the elbow.
			for (int k = 0; k < 7; k++) {
				Vec3d p = elbow.add(fore.multiply(0.12 + k * 0.12)).add(fup.multiply(4.6));
				Vec3d tip = p.add(fup.multiply(3.5 - k * 0.3)).subtract(fd.multiply(2.2));
				Solids.tube(solid, m, p, tip, 0.9, 0.05, 6, 0xD8C8A8, 1.0F, true);
			}
			Solids.tube(solid, m, elbow.subtract(fd.multiply(2)), elbow.subtract(fd.multiply(9)).add(UP.multiply(3)), 2.2, 0.1, 8, 0xD8C8A8, 1.0F, true);
			// The hand.
			double fist = HellGate.fist(a);
			Solids.sphere(solid, m, hand.add(fd.multiply(2.5)), 5.2, hide, 10, 12, true);
			for (int k = 0; k < 5; k++) {
				boolean thumb = k == 4;
				Vec3d root = hand.add(fd.multiply(thumb ? 2.0 : 6.4)).add(fside.multiply(thumb ? 5.0 : (k - 1.5) * 2.5));
				if (!thumb) {
					Solids.tube(solid, m, root.add(fup.multiply(1.2)), root.add(fup.multiply(3.2)).add(fd.multiply(-0.6)), 0.6, 0.05, 5, 0xD8C8A8, 1.0F, true);
				}
				Vec3d d = thumb ? fside.multiply(0.6).add(fd.multiply(0.8)).normalize() : fd.multiply(1 - fist * 0.2).add(fup.multiply(-0.15 - fist)).normalize();
				Vec3d p = root;
				for (int j = 0; j < 3; j++) {
					double curl = (0.2 + fist * 1.15) * (j + 1) * 0.6;
					Vec3d bend = d.multiply(Math.cos(curl)).add(fup.multiply(-Math.sin(curl))).normalize();
					Vec3d q = p.add(bend.multiply((thumb ? 3.6 : 5.0 - j * 0.6)));
					Solids.tube(solid, m, p, q, 1.5 - j * 0.3, 1.25 - j * 0.3, 8, hide, 1.0F, true);
					p = q;
					d = bend;
				}
				// Long black claws.
				Vec3d claw = p.add(d.multiply(1.2).add(fup.multiply(-1.4)).normalize().multiply(4.2));
				Solids.tube(solid, m, p, claw, 0.8, 0.02, 6, 0x0A0606, 1.0F, true);
			}
			// Where the arm comes through the portal, the fire ripples round it.
			double along = ud.dotProduct(f);
			if (along > 0.1 && 14 / along < upper.length()) {
				Vec3d entry = shoulder.add(ud.multiply(14 / along));
				for (int k = 0; k < 3; k++) {
					double rr = 9 + k * 3 + ((a * 0.3) % 3);
					FxDraw.ring(glow, m, entry, s, UP, rr - 0.6, rr + 0.6, 0xFFA040, 0.0F, 0.5F / (k + 1), 0.0F, 32);
				}
			}
		}
	}
}
