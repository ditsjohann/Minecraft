package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.FossilShapes.Leviathan;
import com.armageddon.util.FossilShapes.Mammoth;
import com.armageddon.util.FossilShapes.Ptero;
import com.armageddon.util.PteroPose;
import com.armageddon.util.PteroPose.Pose;
import com.armageddon.util.PteroPose.Seg;
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

/** Client effects of the three fossil bombs: the Mammoth's ice age, the Leviathan's ghost sea and the flying Pterosaur. */
final class FossilFx {
	private FossilFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 55 -> new MammothFx(a, b, p.duration());
			case 56 -> new LeviathanFx(a, b, p.p1(), p.duration());
			case 57 -> new PteroFx(a, b, p.duration());
			default -> null;
		};
	}

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	private static Vec3d rightOf(Vec3d h) {
		return new Vec3d(-h.z, 0, h.x);
	}

	private static Vec3d camera(Vec3d fallback) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc == null || mc.gameRenderer == null ? fallback : mc.gameRenderer.getCamera().getPos();
	}

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}

	/** A quad with its own alpha at each corner. */
	private static void quad(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb, float aa, float ab, float ac, float ad) {
		FxDraw.vertex(vc, m, a.x, a.y, a.z, rgb, aa);
		FxDraw.vertex(vc, m, b.x, b.y, b.z, rgb, ab);
		FxDraw.vertex(vc, m, c.x, c.y, c.z, rgb, ac);
		FxDraw.vertex(vc, m, d.x, d.y, d.z, rgb, ad);
	}

	// ====================================================================== MAMMOTH

	/**
	 * From the moment the eyes light: cold blue fire in the sockets, frost breath, the trumpet, the ice age's front
	 * rolling out as a wall of rime, and a blizzard round anyone caught under it.
	 */
	static final class MammothFx extends FxEffect {
		private final Vec3d c, h, right, skull, mouth;
		private final Vec3d[] eyes = new Vec3d[2];

		MammothFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = heading.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : heading.normalize();
			this.right = rightOf(h);
			this.skull = c.add(Mammoth.skull(h));
			this.mouth = skull.add(h.multiply(5)).add(0, -3, 0);
			for (int s = 0; s < 2; s++) {
				Vec3d e = skull.add(h.multiply(2.9)).add(right.multiply((s * 2 - 1) * 2.5)).add(0, 0.8, 0);
				eyes[s] = new Vec3d(Math.floor(e.x) + 0.5, Math.floor(e.y) + 0.5, Math.floor(e.z) + 0.5).add(h.multiply(0.7));
			}
		}

		private float time(float a) {
			return a + Mammoth.EYES;
		}

		@Override
		public boolean tick(ClientWorld world) {
			float t = time(age);
			if (age % 25 == 0 && t < Mammoth.END - 40) {
				FxEngine.play(VfxIds.MAMMOTH_BREATH, mouth.x, mouth.y, mouth.z, (float) h.x, -0.3F, (float) h.z, 1.0F, 0, age * 31L);
			}
			if (age % 10 == 0) {
				ClientFx.light(skull, 0.6F, 0x80D8FF);
			}
			if ((int) t == Mammoth.TRUMPET) {
				FxEngine.play(VfxIds.MAMMOTH_TRUMPET, mouth.x, mouth.y, mouth.z, (float) h.x, -0.1F, (float) h.z, 1.0F, 0, 77L);
			}
			if (t >= Mammoth.TRUMPET && t <= Mammoth.FREEZE_END) {
				double front = Mammoth.front(t);
				for (int i = 0; i < 4; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double x = c.x + Math.cos(ang) * front, z = c.z + Math.sin(ang) * front;
					FxEngine.play(VfxIds.MAMMOTH_FRONT, x, NewFx.groundY(x, z, c.y) + 0.5, z, 0, 1, 0, 1.0F, 0, age * 131L + i);
				}
			}
			if (t > Mammoth.TRUMPET + 20 && t < Mammoth.END - 60 && age % 3 == 0) {
				Vec3d eye = camera(c);
				double front = Mammoth.front(t);
				if (Math.hypot(eye.x - c.x, eye.z - c.z) < front + 10) {
					FxEngine.play(VfxIds.MAMMOTH_SNOW, eye.x, NewFx.groundY(eye.x, eye.z, eye.y - 1), eye.z, 0, 1, 0, 1.0F, 0, age * 977L);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td, t = time(a);
			float fade = 1 - smooth((t - (Mammoth.END - 50)) / 50.0);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			// Cold fire in the sockets, flickering up out of them.
			float in = smooth(a / 20.0) * fade;
			for (int s = 0; s < 2; s++) {
				Vec3d e = eyes[s].subtract(cam);
				float fl = 0.8F + 0.2F * MathHelper.sin(a * 0.7F + s * 2);
				FxKit.halo(consumers, m, cb, e, 3.2 * fl, 0x60C8FF, 0.55F * in);
				Solids.flare(glow, m, e, cb, 1.6, 0xE8FAFF, 0.9F * in);
				for (int k = 0; k < 5; k++) {
					double ph = ((a * 0.06 + k * 0.2) % 1.0);
					Vec3d p = e.add(h.multiply(-ph * 1.5)).add(0, ph * 3.5, 0).add(right.multiply(Math.sin(a * 0.3 + k * 2) * 0.3));
					Solids.dot(glow, m, p, cb, 0.7 * (1 - ph), 0x90E0FF, (float) (1 - ph) * 0.7F * in);
				}
			}
			// The ice age's front: a rolling wall of rime with a cold glow at its foot.
			if (t >= Mammoth.TRUMPET && t < Mammoth.FREEZE_END + 50) {
				double front = Mammoth.front(t);
				float wall = smooth((t - Mammoth.TRUMPET) / 10.0) * (1 - smooth((t - Mammoth.FREEZE_END) / 50.0));
				List<Effects.Puff> puffs = new ArrayList<>();
				int n = 90;
				for (int k = 0; k < n; k++) {
					double ang = k * MathHelper.TAU / n + Effects.hash(k, 61) * 0.05;
					double x = c.x + Math.cos(ang) * front, z = c.z + Math.sin(ang) * front;
					double gy = NewFx.groundY(x, z, c.y);
					double rad = 4 + Effects.hash(k, 62) * 3 + front * 0.03;
					double roll = Math.sin(a * 0.15 + k) * 0.8;
					puffs.add(new Effects.Puff(new Vec3d(x, gy + rad * 0.5 + roll, z).subtract(cam), rad, 0xF4FAFF, 0xA8C4DC, 0.55F * wall));
					if (k % 2 == 0) {
						Vec3d p0 = new Vec3d(x, gy + 0.3, z).subtract(cam);
						double ang2 = (k + 2) * MathHelper.TAU / n;
						double x2 = c.x + Math.cos(ang2) * front, z2 = c.z + Math.sin(ang2) * front;
						Vec3d p1 = new Vec3d(x2, NewFx.groundY(x2, z2, c.y) + 0.3, z2).subtract(cam);
						FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 2.4, 0x90E0FF, 0.35F * wall, 0.35F * wall);
					}
				}
				Effects.drawPuffs(puffs, consumers.getBuffer(FxRenderLayers.SHADE), m, cb, 8);
			}
		}
	}

	// ====================================================================== LEVIATHAN

	/**
	 * The ghost of the ancient sea: a shimmering surface rolling in over the land with a glowing wall at its edge,
	 * kelp swaying up from the ground, ammonites and shoals of fish drifting through it, light falling in shafts. The
	 * serpent's eyes light, and a maelstrom spirals down into the ground in front of it.
	 */
	static final class LeviathanFx extends FxEffect {
		private static final int RINGS = 18, SEGS = 56, KELP = 30, AMMO = 14, SHOALS = 3, FISH = 18;
		private final Vec3d c, h, right, whirl;
		private final Vec3d[] eyes = new Vec3d[2];
		private final double[] kelpY = new double[KELP];

		LeviathanFx(Vec3d c, Vec3d heading, float whirlDy, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = heading.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : heading.normalize();
			this.right = rightOf(h);
			this.whirl = c.add(h.multiply(Leviathan.HEAD + Leviathan.MAEL_AHEAD)).add(0, whirlDy, 0);
			Vec3d s = at(Leviathan.HEAD);
			Vec3d fu = h.multiply(Math.cos(0.3)).add(UP.multiply(Math.sin(0.3)));
			Vec3d nu = UP.multiply(Math.cos(0.3)).subtract(h.multiply(Math.sin(0.3)));
			for (int k = 0; k < 2; k++) {
				Vec3d e = s.add(fu.multiply(3)).add(nu.multiply(2.0)).add(right.multiply((k * 2 - 1) * 2.4));
				eyes[k] = new Vec3d(Math.floor(e.x) + 0.5, Math.floor(e.y) + 0.5, Math.floor(e.z) + 0.5).add(right.multiply((k * 2 - 1) * 0.6));
			}
			java.util.Arrays.fill(kelpY, Double.NaN);
		}

		private Vec3d at(double u) {
			return c.add(h.multiply(u)).add(right.multiply(Math.sin(u * 0.08) * 6)).add(0, Leviathan.spine(u), 0);
		}

		private double level() {
			return c.y + Leviathan.SEA_H;
		}

		@Override
		public boolean tick(ClientWorld world) {
			double sea = Leviathan.sea(age);
			if (sea > 0.3 && age % 3 == 0) {
				Vec3d eye = camera(c);
				if (Math.hypot(eye.x - c.x, eye.z - c.z) < Leviathan.SEA_R * sea && eye.y < level() + 2) {
					FxEngine.play(VfxIds.LEVI_BUBBLES, eye.x, NewFx.groundY(eye.x, eye.z, eye.y - 1), eye.z, 0, 1, 0, 1.0F, 0, age * 53L);
				}
			}
			if (age >= Leviathan.EYES && age % 10 == 0) {
				ClientFx.light(eyes[0].lerp(eyes[1], 0.5), 0.7F, 0x60FFF0);
			}
			if (age >= Leviathan.MAEL && age < Leviathan.MAEL_END && age % 10 == 0) {
				ClientFx.light(whirl.add(0, 2, 0), 0.6F, 0x60E0FF);
			}
			for (int k = 0; k < KELP; k++) {
				if (Double.isNaN(kelpY[k])) {
					Vec3d p = kelp(k);
					kelpY[k] = NewFx.groundY(p.x, p.z, c.y);
				}
			}
			return super.tick(world);
		}

		private Vec3d kelp(int k) {
			double ang = Effects.hash(k, 71) * MathHelper.TAU, r = 8 + Math.sqrt(Effects.hash(k, 72)) * (Leviathan.SEA_R - 12);
			return new Vec3d(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double sea = Leviathan.sea(a);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d o = c.subtract(cam);
			double lv = level() - cam.y;
			if (sea > 0.01) {
				double R = Leviathan.SEA_R * Math.min(1.0, sea * 1.15);
				float body = (float) Math.min(1.0, sea * 1.5);
				// The surface: a rippling disc, translucent teal, with caustic light crawling over it.
				for (int i = 0; i < RINGS; i++) {
					double r0 = R * i / RINGS, r1 = R * (i + 1) / RINGS;
					float e0 = edge(r0, R), e1 = edge(r1, R);
					for (int j = 0; j < SEGS; j++) {
						double t0 = MathHelper.TAU * j / SEGS, t1 = MathHelper.TAU * (j + 1) / SEGS;
						Vec3d p00 = surf(o, lv, r0, t0, a), p01 = surf(o, lv, r0, t1, a), p11 = surf(o, lv, r1, t1, a), p10 = surf(o, lv, r1, t0, a);
						quad(shade, m, p00, p10, p11, p01, 0x2A9CB0, 0.22F * body * e0, 0.22F * body * e1, 0.22F * body * e1, 0.22F * body * e0);
						float c00 = caustic(p00.add(cam), a), c01 = caustic(p01.add(cam), a), c11 = caustic(p11.add(cam), a), c10 = caustic(p10.add(cam), a);
						quad(glow, m, p00, p10, p11, p01, 0x50FFE8, 0.12F * body * e0 * c00, 0.12F * body * e1 * c10, 0.12F * body * e1 * c11, 0.12F * body * e0 * c01);
					}
				}
				// The edge: a glowing wall of ghost water from the ground up to the surface.
				for (int j = 0; j < SEGS * 2; j++) {
					double t0 = MathHelper.TAU * j / (SEGS * 2), t1 = MathHelper.TAU * (j + 1) / (SEGS * 2);
					Vec3d b0 = o.add(Math.cos(t0) * R, -3, Math.sin(t0) * R), b1 = o.add(Math.cos(t1) * R, -3, Math.sin(t1) * R);
					Vec3d s0 = surf(o, lv, R, t0, a), s1 = surf(o, lv, R, t1, a);
					quad(glow, m, b0, b1, s1, s0, 0x40E0D0, 0.02F * body, 0.02F * body, 0.22F * body, 0.22F * body);
					quad(shade, m, b0, b1, s1, s0, 0x1E7890, 0.12F * body, 0.12F * body, 0.2F * body, 0.2F * body);
				}
				// Shafts of light falling through it.
				for (int k = 0; k < 10; k++) {
					double ang = Effects.hash(k, 81) * MathHelper.TAU + a * 0.002, r = Math.sqrt(Effects.hash(k, 82)) * R * 0.85;
					Vec3d top = o.add(Math.cos(ang) * r, lv - o.y, Math.sin(ang) * r);
					float sway = MathHelper.sin(a * 0.02F + k);
					FxDraw.beam(glow, m, top, top.add(sway * 2, -Leviathan.SEA_H - 2, 1.5), Vec3d.ZERO, 3.5, 0x80FFF0, 0.05F * body);
				}
				// Kelp swaying up from the bed.
				for (int k = 0; k < KELP; k++) {
					if (Double.isNaN(kelpY[k])) {
						continue;
					}
					Vec3d base = kelp(k);
					Vec3d prev = new Vec3d(base.x, kelpY[k], base.z).subtract(cam);
					double tall = (Leviathan.SEA_H - 1) * (0.5 + 0.5 * Effects.hash(k, 73)) * sea;
					int segs = 8;
					for (int s = 1; s <= segs; s++) {
						double f = s / (double) segs;
						double sway = Math.sin(a * 0.05 + k + f * 3) * 1.4 * f;
						Vec3d p = new Vec3d(base.x + sway, kelpY[k] + tall * f, base.z + Math.cos(a * 0.04 + k * 2 + f * 2) * 0.8 * f).subtract(cam);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.5 * (1 - f * 0.5), 0x50D890, 0.25F * body, 0.25F * body);
						prev = p;
					}
				}
				// Ammonites drifting on slow circles, tentacles trailing.
				for (int k = 0; k < AMMO; k++) {
					double ang = Effects.hash(k, 91) * MathHelper.TAU + a * (0.004 + 0.004 * Effects.hash(k, 92)) * (k % 2 == 0 ? 1 : -1);
					double r = 10 + Effects.hash(k, 93) * (R - 16);
					double y = 3 + Effects.hash(k, 94) * (Leviathan.SEA_H - 5) + Math.sin(a * 0.03 + k) * 0.8;
					Vec3d p = o.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
					Vec3d dir = new Vec3d(-Math.sin(ang), 0, Math.cos(ang)).multiply(k % 2 == 0 ? 1 : -1);
					ammonite(glow, shade, m, cb, p, dir, 1.4 + Effects.hash(k, 95) * 1.2, a, k, body);
				}
				// Shoals of ghost fish wheeling.
				for (int s = 0; s < SHOALS; s++) {
					double ang = s * 2.1 + a * 0.012 * (s == 1 ? -1 : 1);
					double r = 18 + s * 12;
					Vec3d centre = o.add(Math.cos(ang) * r, 4 + s * 2 + Math.sin(a * 0.02 + s) * 1.5, Math.sin(ang) * r);
					Vec3d dir = new Vec3d(-Math.sin(ang), 0, Math.cos(ang)).multiply(s == 1 ? -1 : 1);
					for (int f = 0; f < FISH; f++) {
						Vec3d off = new Vec3d(Effects.hash(f, s * 7 + 1) - 0.5, (Effects.hash(f, s * 7 + 2) - 0.5) * 0.5, Effects.hash(f, s * 7 + 3) - 0.5).multiply(6);
						off = off.add(Math.sin(a * 0.1 + f) * 0.4, Math.sin(a * 0.13 + f * 2) * 0.3, Math.cos(a * 0.1 + f) * 0.4);
						Vec3d p = centre.add(off);
						double wag = Math.sin(a * 0.6 + f) * 0.25;
						Vec3d side = new Vec3d(-dir.z, 0, dir.x);
						FxDraw.ribbon(glow, m, p.add(dir.multiply(0.5)), p.subtract(dir.multiply(0.5)).add(side.multiply(wag)), Vec3d.ZERO, 0.3, 0xA0FFF8, 0.6F * body, 0.15F * body);
					}
				}
			}
			// The serpent's eyes.
			if (a >= Leviathan.EYES) {
				float in = smooth((a - Leviathan.EYES) / 15.0) * (1 - smooth((a - (Leviathan.END - 30)) / 30.0));
				for (int k = 0; k < 2; k++) {
					Vec3d e = eyes[k].subtract(cam);
					float fl = 0.85F + 0.15F * MathHelper.sin(a * 0.5F + k);
					FxKit.halo(consumers, m, cb, e, 3.6 * fl, 0x30E8D8, 0.55F * in);
					Solids.flare(glow, m, e, cb, 1.8, 0xE0FFFA, 0.9F * in);
				}
			}
			// The maelstrom: white water spiralling down into a dark eye.
			double k = Leviathan.maelstrom(a);
			if (k > 0.01) {
				Vec3d w = whirl.subtract(cam);
				double top = sea > 0.3 ? lv - w.y : 1.0;
				int arms = 7;
				for (int arm = 0; arm < arms; arm++) {
					Vec3d prev = null;
					for (int s = 0; s <= 40; s++) {
						double f = s / 40.0;
						double r = Leviathan.MAEL_R * 1.15 * (1 - f * 0.92);
						double y = top - (top + 12) * Math.pow(f, 1.6);
						double ang = arm * MathHelper.TAU / arms + f * 10.0 - a * (0.12 + f * 0.25);
						Vec3d p = w.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
						if (prev != null) {
							float al = (float) (k * (0.35 + 0.45 * f));
							int col = Solids.mix(arm % 2 == 0 ? 0xD8F4F4 : 0x8CD0D4, 0x123E4C, Math.min(1.0, f * 1.4));
							FxDraw.ribbon(shade, m, prev, p, Vec3d.ZERO, 2.0 * (1 - f * 0.6), col, al, al);
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.4, 0x60E8E0, al * 0.25F, al * 0.25F);
						}
						prev = p;
					}
				}
				FxDraw.disc(shade, m, w.add(0, -11.5, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 2.5, 0x0A2030, (float) (0.85 * k), 0.0F, 24);
				FxDraw.ring(glow, m, w.add(0, top + 0.2, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Leviathan.MAEL_R * 0.9, Leviathan.MAEL_R * 1.3, 0x60E8E0,
						0.0F, (float) (0.12 * k), 0.0F, 48);
			}
		}

		private static float edge(double r, double R) {
			return (float) MathHelper.clamp((R - r) / (R * 0.15), 0.0, 1.0);
		}

		private static Vec3d surf(Vec3d o, double lv, double r, double t, float a) {
			double x = Math.cos(t) * r, z = Math.sin(t) * r;
			double y = lv + 0.35 * Math.sin((o.x + x) * 0.18 + a * 0.06) + 0.25 * Math.sin((o.z + z) * 0.23 - a * 0.05);
			return new Vec3d(o.x + x, y, o.z + z);
		}

		private static float caustic(Vec3d w, float a) {
			double v = Math.sin(w.x * 0.45 + a * 0.07) * Math.sin(w.z * 0.5 - a * 0.06) + Math.sin((w.x + w.z) * 0.3 + a * 0.05);
			return (float) MathHelper.clamp(v * 0.6 + 0.3, 0.0, 1.0);
		}

		/** A coiled shell drawn as a glowing logarithmic spiral, with a sheen and tentacles trailing behind. */
		private static void ammonite(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d p, Vec3d dir, double size, float a, int k,
				float body) {
			Vec3d side = new Vec3d(-dir.z, 0, dir.x);
			FxDraw.disc(shade, m, p, dir, UP, size * 1.45, 0xD8B888, 0.35F * body, 0.25F * body, 16);
			Vec3d prev = null;
			for (int s = 0; s <= 40; s++) {
				double th = s * 0.32;
				double r = size * 0.1 * Math.exp(0.2 * th);
				Vec3d q = p.add(dir.multiply(Math.cos(th + a * 0.01) * r)).add(UP.multiply(Math.sin(th + a * 0.01) * r));
				if (prev != null) {
					FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.12 + s * 0.006, 0xFFE8C0, 0.55F * body, 0.55F * body);
				}
				prev = q;
			}
			Vec3d mouthP = p.add(dir.multiply(size * 1.3));
			for (int t = 0; t < 5; t++) {
				Vec3d prevT = mouthP;
				for (int s = 1; s <= 4; s++) {
					double wave = Math.sin(a * 0.2 + s + t * 1.3 + k) * 0.25 * s;
					Vec3d q = mouthP.add(dir.multiply(s * 0.55)).add(side.multiply((t - 2) * 0.18 * s * 0.5 + wave)).add(0, -0.15 * s, 0);
					FxDraw.ribbon(glow, m, prevT, q, Vec3d.ZERO, 0.08, 0xFFD0A0, 0.4F * body * (1 - s / 5.0F), 0.4F * body * (1 - (s + 1) / 5.0F));
					prevT = q;
				}
			}
			Solids.dot(glow, m, mouthP.add(UP.multiply(size * 0.3)), cb, 0.3, 0x80FFF0, 0.7F * body);
		}
	}

	// ====================================================================== PTEROSAUR

	/**
	 * The pterosaur itself: a skeleton thirty-odd blocks across drawn bone by bone, fire glowing through it and a
	 * ghost of its wings stretched between the bones. It assembles from bones flying up out of the ground, flies,
	 * folds into its dives, lands on its spire and crumbles away.
	 */
	static final class PteroFx extends FxEffect {
		private static final double SC = PteroPose.SC;
		private static final int BONE = 0xE6D8B8;
		private final Vec3d c, h;

		PteroFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = heading.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : heading.normalize();
		}

		Pose pose(double t) {
			return PteroPose.pose(t, h);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t >= Ptero.TAKEOFF && t < Ptero.PERCH) {
				Pose p = pose(t);
				Pose q = pose(t - 1);
				for (int s = 0; s < 2; s++) {
					Vec3d tip = c.add(p.tips[s]), v = p.tips[s].subtract(q.tips[s]);
					FxEngine.playMoving(VfxIds.PTERO_TRAIL, tip.x, tip.y, tip.z, 0, 1, 0, 1.0F, 0, t * 131L + s, (float) v.x, (float) v.y, (float) v.z);
				}
			}
			for (int d : Ptero.DIVES) {
				if (t == d + 2 || t == d + 10) {
					Pose p = pose(t);
					Vec3d b = c.add(p.beak);
					FxEngine.play(VfxIds.PTERO_SCREECH, b.x, b.y, b.z, (float) p.f.x, (float) p.f.y, (float) p.f.z, 1.0F, 0, t * 7L);
				}
			}
			if (t % 8 == 0 && t < Ptero.CRUMBLE + 20) {
				ClientFx.light(c.add(Ptero.pos(t)), 0.6F, 0xFF9040);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Pose p = pose(a);
			Vec3d o = c.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			int n = p.segs.size();
			float alive = 1 - smooth((a - Ptero.CRUMBLE) / 50.0);
			for (int i = 0; i < n; i++) {
				Seg s = p.segs.get(i);
				// Assembling: each bone flies up out of the ground into place, the skull last.
				double delay = 6 + (i / (double) n) * 46;
				double u = smooth((a - delay) / 24.0);
				if (u <= 0) {
					continue;
				}
				Vec3d from = new Vec3d((Effects.hash(i, 101) - 0.5) * 18, 0, (Effects.hash(i, 102) - 0.5) * 18);
				double arc = Math.sin(Math.PI * u) * 6;
				Vec3d A = from.lerp(s.a(), u).add(0, arc, 0), B = from.add(s.b().subtract(s.a()).multiply(0.3)).lerp(s.b(), u).add(0, arc, 0);
				// Turned to stone: from here the server's bone blocks stand where it perched.
				double cr = a >= Ptero.CRUMBLE + 1 ? 1 : 0;
				double scale = 1 - cr;
				if (scale <= 0.02) {
					continue;
				}
				A = A.add(0, -cr * 3, 0);
				B = B.add(0, -cr * 3, 0);
				Solids.tube(solid, m, o.add(A), o.add(B), s.ra() * SC * scale, s.rb() * SC * scale, 7, BONE, 1.0F, true);
				if (s.fire()) {
					float pulse = 0.5F + 0.5F * MathHelper.sin(a * 0.25F - i * 0.7F);
					FxDraw.ribbon(glow, m, o.add(A), o.add(B), Vec3d.ZERO, (s.ra() + s.rb()) * SC * 1.6, 0xFF7020, (0.12F + 0.12F * pulse) * (float) u * alive,
							(0.12F + 0.12F * pulse) * (float) u * alive);
				}
			}
			// The ghost of its wings: amber membrane faint between the bones.
			float mem = smooth((a - 60) / 30.0) * alive * (a >= Ptero.PERCH ? 0.4F : 1.0F);
			if (mem > 0.01F) {
				for (Vec3d[] tri : p.membrane) {
					Vec3d A = o.add(tri[0]), B = o.add(tri[1]), C = o.add(tri[2]);
					quad(glow, m, A, B, C, C, 0xFF9040, 0.13F * mem, 0.1F * mem, 0.04F * mem, 0.04F * mem);
					FxDraw.ribbon(glow, m, B, C, Vec3d.ZERO, 0.18, 0xFFB060, 0.15F * mem, 0.15F * mem);
				}
			}
			// Fire in its eyes.
			float eye = smooth((a - 50) / 20.0) * alive;
			for (Vec3d e : p.eyes) {
				Vec3d q = o.add(e);
				float fl = 0.8F + 0.2F * MathHelper.sin(a * 0.9F + (float) e.x);
				FxKit.halo(consumers, m, cb, q, 2.4 * fl, 0xFF6010, 0.55F * eye);
				Solids.flare(glow, m, q, cb, 1.0, 0xFFF0C0, 0.9F * eye);
			}
		}
	}
}
