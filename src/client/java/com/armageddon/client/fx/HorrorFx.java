package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.HorrorShapes;
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
			lightning(glow, m, cam, a);
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
				Solids.dot(shade, m, p, cb, 0.7 + v[3] * 0.55, 0x2A0405, 0.85F * fade);
				Solids.dot(glow, m, p, cb, 0.5 + v[3] * 0.4, 0xD01008, pulse * fade);
			}
		}

		/** The rift: a jagged red-lit crack in the ground under it, wider once it has climbed out. */
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
				// Black tears running down from the eyes.
				FxDraw.ribbon(shade, m, e.add(upH.multiply(-1.0)), e.add(upH.multiply(-3.2)).add(r.multiply(s * 0.2)), Vec3d.ZERO, 0.16, 0x140606, 0.95F, 0.6F);
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
			Vec3d mouthTop = front.add(upH.multiply(-1.9)), mouthBot = front.add(upH.multiply(-2.0 - shriek * 2.4));
			FxDraw.ribbon(shade, m, mouthTop, mouthBot, Vec3d.ZERO, 0.1 + shriek * 1.8, 0x050202, 0.98F, 0.98F);
			if (shriek > 0.1) {
				FxDraw.ribbon(glow, m, mouthTop.add(upH.multiply(-0.4)), mouthBot.add(upH.multiply(0.4)), Vec3d.ZERO, 0.3 + shriek * 0.5, 0x900808,
						(float) shriek * 0.3F, (float) shriek * 0.3F);
			}
		}
	}
}
