package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.HorrorShapes;
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
			case 60 -> new HorsemanFx(a, (int) p.p1(), p.duration());
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

	// ====================================================================== PUMPKIN HORSEMAN

	/**
	 * The horseman: a horse of night galloping with a burning mane and tail, the headless rider with ghost fire pouring
	 * from his neck, his tattered cloak streaming, his scythe, and the jack-o'-lantern he throws.
	 */
	static final class HorsemanFx extends FxEffect {
		private static final int HORSE = 0x15101A, RIDER = 0x1C1420, CLOAK = 0x1A0C20, PUMPKIN = 0xD86A10;
		private final Vec3d c;
		private final int seed;

		HorsemanFx(Vec3d c, int seed, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.seed = seed;
		}

		/** Maps the horse's local frame (x forward, y up, z to its side) to camera-relative space. */
		private final class Frame {
			final Vec3d o, d, s;
			final double cosR, sinR, bob, scale;

			Frame(Vec3d cam, double a) {
				Vec3d p = Horseman.pos(a);
				double gy = NewFx.groundY(c.x + p.x, c.z + p.z, c.y);
				this.o = new Vec3d(c.x + p.x, gy + p.y, c.z + p.z).subtract(cam);
				this.d = Horseman.dir(a);
				this.s = new Vec3d(-d.z, 0, d.x);
				double rear = Horseman.rear(a) * 0.8;
				this.cosR = Math.cos(rear);
				this.sinR = Math.sin(rear);
				boolean onLand = a >= Horseman.GALLOP && a < Horseman.ASCEND && Horseman.rear(a) < 0.05;
				this.bob = onLand ? 0.9 * Math.abs(Math.sin(a * 0.32)) : 0;
				this.scale = Horseman.BIG * smooth((a - Horseman.APPEAR) / 30.0) * (1 - smooth((a - Horseman.VANISH) / 4.0));
			}

			Vec3d at(double x, double y, double z) {
				// Rearing pivots the whole body up about the hind feet.
				double dx = x + 6;
				double x2 = -6 + dx * cosR - y * sinR, y2 = dx * sinR + y * cosR;
				return o.add(d.multiply(x2 * scale)).add(UP.multiply((y2 + bob) * scale)).add(s.multiply(z * scale));
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
			if (t >= Horseman.APPEAR && t < Horseman.VANISH && t % 5 == 0) {
				Vec3d p = Horseman.pos(t);
				ClientFx.light(new Vec3d(c.x + p.x, NewFx.groundY(c.x + p.x, c.z + p.z, c.y) + p.y + Horseman.HAND, c.z + p.z), 0.8F, 0xFF8020);
			}
			return super.tick(world);
		}

		/** World position of thrown lantern {@code k} at tick {@code t}: an arc from his hand to where it lands. */
		private Vec3d lantern(int k, double t) {
			int at = Horseman.throwAt(k);
			Vec3d p = Horseman.pos(at);
			Vec3d from = new Vec3d(c.x + p.x, NewFx.groundY(c.x + p.x, c.z + p.z, c.y) + Horseman.HAND, c.z + p.z);
			Vec3d tg = Horseman.target(k, seed);
			Vec3d to = new Vec3d(c.x + tg.x, NewFx.groundY(c.x + tg.x, c.z + tg.z, c.y) + 1, c.z + tg.z);
			double u = MathHelper.clamp((t - at) / Horseman.FLIGHT, 0.0, 1.0);
			return from.lerp(to, u).add(0, Math.sin(Math.PI * u) * 22, 0);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			for (int k = 0; k < Horseman.THROWS; k++) {
				int at = Horseman.throwAt(k);
				if (a >= at && a < at + Horseman.FLIGHT) {
					Vec3d p = lantern(k, a).subtract(cam);
					Vec3d spinD = new Vec3d(Math.cos(a * 0.4), 0, Math.sin(a * 0.4));
					jackOLantern(consumers, solid, glow, m, cb, p, spinD, 2.4, a);
				}
			}
			if (a < Horseman.APPEAR || a > Horseman.VANISH + 2) {
				return;
			}
			Frame f = new Frame(cam, a);
			if (f.scale <= 0.02) {
				return;
			}
			horse(f, solid, glow, m, cb, a);
			rider(f, consumers, solid, glow, shade, m, cb, a);
		}

		private void horse(Frame f, VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, float a) {
			double sc = f.scale;
			// Body: rump, barrel, chest.
			Solids.tube(solid, m, f.at(-8.5, 11.8, 0), f.at(-3, 11.6, 0), 3.6 * sc, 4.2 * sc, 10, HORSE, 1.0F, true);
			Solids.tube(solid, m, f.at(-3, 11.6, 0), f.at(3, 11.8, 0), 4.2 * sc, 4.3 * sc, 10, HORSE, 1.0F, true);
			Solids.tube(solid, m, f.at(3, 11.8, 0), f.at(7.5, 12.6, 0), 4.3 * sc, 3.4 * sc, 10, HORSE, 1.0F, true);
			// Neck and head, the head held low and forward.
			Vec3d n0 = f.at(6.5, 14.0, 0), n1 = f.at(9.5, 18.5, 0), n2 = f.at(11.5, 21.0, 0);
			Solids.tube(solid, m, n0, n1, 2.8 * sc, 2.2 * sc, 8, HORSE, 1.0F, true);
			Solids.tube(solid, m, n1, n2, 2.2 * sc, 1.9 * sc, 8, HORSE, 1.0F, true);
			Vec3d muzzle = f.at(15.8, 17.2, 0);
			Solids.tube(solid, m, n2, muzzle, 1.9 * sc, 1.1 * sc, 8, HORSE, 1.0F, true);
			for (int s = -1; s <= 1; s += 2) {
				Solids.tube(solid, m, f.at(11.2, 22.0, s * 0.8), f.at(10.6, 24.0, s * 0.9), 0.45 * sc, 0.1 * sc, 5, HORSE, 1.0F, true);
				Vec3d eye = f.at(12.6, 20.6, s * 1.35);
				Solids.flare(glow, m, eye, cb, 0.9 * sc, 0xFFA030, 0.95F);
			}
			// Breath of fire from the nostrils.
			FxDraw.ribbon(glow, m, muzzle, f.at(17.5, 16.0, 0), Vec3d.ZERO, 0.7 * sc, 0xFF7020, 0.5F, 0.0F);
			// Legs at the gallop: hind pair, then front pair.
			double ph = a * 0.32, rear = Horseman.rear(a);
			double[][] legs = {{-6.5, 2.4, 0.0}, {-6.5, -2.4, 0.13}, {5.5, 2.4, 0.5}, {5.5, -2.4, 0.63}};
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
				Vec3d knee = f.at(leg[0] + Math.sin(al) * 4.4, 9.0 - Math.cos(al) * 4.4, leg[1]);
				double kx = leg[0] + Math.sin(al) * 4.4, ky = 9.0 - Math.cos(al) * 4.4;
				Vec3d hoof = f.at(kx + Math.sin(be) * 4.4, ky - Math.cos(be) * 4.4, leg[1]);
				Solids.tube(solid, m, hip, knee, 1.3 * sc, 0.9 * sc, 7, HORSE, 1.0F, true);
				Solids.tube(solid, m, knee, hoof, 0.8 * sc, 0.7 * sc, 7, HORSE, 1.0F, true);
				Solids.dot(glow, m, hoof, cb, 1.2 * sc, 0xFF7020, 0.7F);
			}
			// The mane and tail: ghost fire, orange and green, streaming back.
			for (int k = 0; k < 9; k++) {
				double u = k / 8.0;
				Vec3d root = f.at(6.0 + u * 5.5, 15.5 + u * 6.0, 0);
				flame(glow, m, root, f.d.multiply(-1).add(UP.multiply(0.9)).normalize(), 3.5 + 1.5 * Math.sin(k), a, k, sc);
			}
			Vec3d tail = f.at(-8.8, 13.0, 0);
			for (int k = 0; k < 4; k++) {
				flame(glow, m, tail, f.d.multiply(-1).add(UP.multiply(0.25 + k * 0.12)).normalize(), 7 + k, a, 20 + k, sc);
			}
		}

		/** A licking tongue of ghost fire from {@code root} along {@code dir}. */
		private static void flame(VertexConsumer glow, Matrix4f m, Vec3d root, Vec3d dir, double len, float a, int salt, double sc) {
			Vec3d side = new Vec3d(-dir.z, 0, dir.x);
			Vec3d prev = root;
			int n = 6;
			for (int j = 1; j <= n; j++) {
				double u = j / (double) n;
				double wv = Math.sin(a * 0.6 + salt * 1.7 + j * 1.1) * 0.6 * u;
				Vec3d p = root.add(dir.multiply(len * u * sc)).add(side.multiply(wv * sc)).add(UP.multiply(Math.sin(a * 0.45 + salt + j) * 0.4 * u * sc));
				float al = (float) (1 - u) * 0.75F;
				FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, (1.3 - u) * sc, salt % 3 == 0 ? 0x70FF40 : 0xFF7A20, al, al * 0.7F);
				prev = p;
			}
		}

		private void rider(Frame f, VertexConsumerProvider consumers, VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb,
				float a) {
			double sc = f.scale;
			Vec3d hip = f.at(-1.0, 16.4, 0), chest = f.at(0.6, 22.5, 0), shoulders = f.at(0.8, 25.6, 0), stump = f.at(0.9, 27.0, 0);
			Solids.tube(solid, m, hip, chest, 2.2 * sc, 2.6 * sc, 9, RIDER, 1.0F, true);
			Solids.tube(solid, m, chest, shoulders, 2.6 * sc, 2.2 * sc, 9, RIDER, 1.0F, true);
			Solids.tube(solid, m, shoulders, stump, 1.4 * sc, 1.0 * sc, 8, RIDER, 1.0F, true);
			// Ghost fire pouring up from where his head should be.
			for (int k = 0; k < 6; k++) {
				Vec3d dir = UP.add(f.d.multiply(-0.35)).add(f.s.multiply((k - 2.5) * 0.08)).normalize();
				flame(glow, m, stump, dir, 5.5 + k % 3, a, k * 3, sc);
			}
			FxKit.halo(consumers, m, cb, stump.add(0, 2 * sc, 0), 4.5 * sc, 0x60FF40, 0.35F);
			// Legs down the horse's flanks.
			for (int s = -1; s <= 1; s += 2) {
				Vec3d knee = f.at(2.0, 13.0, s * 4.4), foot = f.at(0.8, 8.5, s * 4.6);
				Solids.tube(solid, m, f.at(-0.8, 16.2, s * 1.6), knee, 1.2 * sc, 1.0 * sc, 7, RIDER, 1.0F, true);
				Solids.tube(solid, m, knee, foot, 0.9 * sc, 0.8 * sc, 7, RIDER, 1.0F, true);
			}
			// The lantern arm: held high, wound back before a throw, empty until a new lantern kindles.
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
			Solids.tube(solid, m, sh, elbow, 1.0 * sc, 0.8 * sc, 7, RIDER, 1.0F, true);
			Solids.tube(solid, m, elbow, hand, 0.8 * sc, 0.65 * sc, 7, RIDER, 1.0F, true);
			if (regrow > 0.02) {
				jackOLantern(consumers, solid, glow, m, cb, hand.add(UP.multiply(1.6 * sc * regrow)), f.d, 2.4 * sc * regrow, a);
			}
			// The scythe arm.
			Vec3d sh2 = f.at(0.8, 25.2, -2.8), el2 = f.at(0.0, 21.5, -4.4), hand2 = f.at(2.4, 20.8, -4.8);
			Solids.tube(solid, m, sh2, el2, 1.0 * sc, 0.8 * sc, 7, RIDER, 1.0F, true);
			Solids.tube(solid, m, el2, hand2, 0.8 * sc, 0.65 * sc, 7, RIDER, 1.0F, true);
			double swing = a >= Horseman.CHARGE && a < Horseman.ASCEND ? Math.sin((a - Horseman.CHARGE) * 0.5) * 6 : 0;
			Vec3d butt = f.at(-5.0, 14.0 - swing * 0.3, -5.0), top = f.at(9.0, 33.0 + swing * 0.2, -5.2 + swing * 0.3);
			Solids.tube(solid, m, butt, top, 0.35 * sc, 0.3 * sc, 6, 0x2A1E14, 1.0F, true);
			Vec3d prev = top;
			for (int j = 1; j <= 8; j++) {
				double u = j / 8.0;
				Vec3d p = top.add(f.d.multiply((-u * 8.0 + u * u * 2.0) * sc)).add(UP.multiply((-u * u * 4.5) * sc)).add(f.s.multiply(-0.3 * sc));
				Solids.tube(solid, m, prev, p, (0.55 - u * 0.4) * sc, (0.5 - u * 0.4) * sc, 5, 0x8A8E90, 1.0F, true);
				FxDraw.ribbon(glow, m, prev.add(UP.multiply(-0.5 * sc)), p.add(UP.multiply(-0.5 * sc)), Vec3d.ZERO, 0.35 * sc, 0xB0FF90, 0.6F, 0.6F);
				prev = p;
			}
			// The cloak, tattered, streaming back from his shoulders.
			int strips = 7, rows = 7;
			Vec3d[][] cloak = new Vec3d[strips][rows];
			for (int k = 0; k < strips; k++) {
				double z = -3.0 + k * 1.0;
				for (int j = 0; j < rows; j++) {
					double flutter = Math.sin(a * 0.35 + j * 0.9 + k * 0.7) * 0.7 * j / rows;
					int len = rows - (int) (HorrorShapes.hash(k, 77) * 2);
					double jj = Math.min(j, len);
					cloak[k][j] = f.at(-0.6 - jj * 1.7, 25.6 - jj * 1.25 + flutter, z * (1 + jj * 0.08));
				}
			}
			for (int k = 0; k < strips - 1; k++) {
				for (int j = 0; j < rows - 1; j++) {
					Solids.quad(shade, m, cloak[k][j], cloak[k + 1][j], cloak[k + 1][j + 1], cloak[k][j + 1], CLOAK, 0.96F);
				}
			}
		}

		/** A blazing jack-o'-lantern: a ribbed pumpkin, its carved face lit from inside, a halo of fire. */
		private static void jackOLantern(VertexConsumerProvider consumers, VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d p, Vec3d face,
				double r, float a) {
			if (r < 0.05) {
				return;
			}
			Vec3d side = new Vec3d(-face.z, 0, face.x);
			for (int k = 0; k < 6; k++) {
				double ang = k * Math.PI / 6;
				Vec3d off = face.multiply(Math.cos(ang) * r * 0.35).add(side.multiply(Math.sin(ang) * r * 0.35));
				Solids.sphere(solid, m, p.add(off), r * 0.78, k % 2 == 0 ? PUMPKIN : 0xC05A0C, 6, 8, true);
			}
			Solids.tube(solid, m, p.add(UP.multiply(r * 0.7)), p.add(UP.multiply(r * 1.2)).add(side.multiply(r * 0.2)), r * 0.15, r * 0.1, 5, 0x3A6020, 1.0F,
					true);
			Vec3d front = p.add(face.multiply(r * 1.02));
			float fl = 0.8F + 0.2F * MathHelper.sin(a * 1.7F);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d e = front.add(side.multiply(s * r * 0.38)).add(UP.multiply(r * 0.22));
				Solids.quad(glow, m, e.add(UP.multiply(r * 0.22)), e.add(side.multiply(r * 0.2)).add(UP.multiply(-r * 0.12)),
						e.add(side.multiply(-r * 0.2)).add(UP.multiply(-r * 0.12)), e.add(UP.multiply(r * 0.22)), 0xFFE060, fl);
			}
			Vec3d prev = null;
			for (int k = -4; k <= 4; k++) {
				double u = k / 4.0;
				Vec3d q = front.add(side.multiply(u * r * 0.6)).add(UP.multiply(-r * 0.32 + u * u * r * 0.12 + (k % 2 == 0 ? 0 : -r * 0.1)));
				if (prev != null) {
					FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, r * 0.14, 0xFFD040, fl, fl);
				}
				prev = q;
			}
			FxKit.halo(consumers, m, cb, p, r * 2.6, 0xFF7020, 0.4F * fl);
		}
	}
}
