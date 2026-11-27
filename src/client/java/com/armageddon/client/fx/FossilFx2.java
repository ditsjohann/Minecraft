package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.FossilPoses;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.FossilShapes.Amber;
import com.armageddon.util.FossilShapes.Ammonite;
import com.armageddon.util.FossilShapes.Sabre;
import com.armageddon.util.PteroPose.Seg;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Client effects of the Amber, Sabre-Tooth and Ammonite fossils. */
final class FossilFx2 {
	private FossilFx2() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 65 -> new AmberFx(a, b, p.duration());
			case 66 -> new SabreFx(a, b, p.duration());
			case 67 -> new AmmoniteFx(a, b, p.duration());
			default -> null;
		};
	}

	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);

	private static Vec3d unit(Vec3d h) {
		return h.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : new Vec3d(h.x, 0, h.z).normalize();
	}

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}

	private static void quad(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb, float aa, float ab, float ac, float ad) {
		FxDraw.vertex(vc, m, a.x, a.y, a.z, rgb, aa);
		FxDraw.vertex(vc, m, b.x, b.y, b.z, rgb, ab);
		FxDraw.vertex(vc, m, c.x, c.y, c.z, rgb, ac);
		FxDraw.vertex(vc, m, d.x, d.y, d.z, rgb, ad);
	}

	// ====================================================================== AMBER

	/**
	 * The dragonfly, still in the amber (drawn before the glass so the amber is seen over it) and then alive; the glow
	 * inside the drop, cracks of light running over it, the resin's glossy lip rolling out, the swarm.
	 */
	static final class AmberFx extends FxEffect {
		private static final int CRACKS = 11;
		private final Vec3d c, h;

		AmberFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			Vec3d core = c.add(0, Amber.CY, 0);
			if (t >= Amber.RISEN - 30 && t < Amber.SHATTER && t % 8 == 0) {
				ClientFx.light(core, 0.5F + 0.4F * smooth((t - Amber.CRACK) / 200.0), 0xFFA030);
			}
			if (t >= Amber.CRACK - 60 && t < Amber.SHATTER && t % 3 == 0) {
				// Resin beading on the drop's lower skin and running off.
				double a = world.random.nextDouble() * MathHelper.TAU, y = -world.random.nextDouble() * 0.8;
				Vec3d p = skin(Math.cos(a) * Math.sqrt(1 - y * y), y, Math.sin(a) * Math.sqrt(1 - y * y), 1.05);
				FxEngine.play(VfxIds.AMBER_DRIP, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 37L);
			}
			if (t >= Amber.OOZE && t <= Amber.OOZE_END) {
				double front = Amber.front(t);
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double x = c.x + Math.cos(a) * front, z = c.z + Math.sin(a) * front;
					FxEngine.play(VfxIds.AMBER_FRONT, x, NewFx.groundY(x, z, c.y), z, 0, 1, 0, 1.0F, 0, t * 131L + i);
				}
			}
			if (t > Amber.SHATTER && t < Amber.ENTOMB + 20) {
				Vec3d p = c.add(FossilShapes.frame(h, Amber.pos(t)));
				Vec3d v = FossilShapes.frame(h, Amber.pos(t).subtract(Amber.pos(t - 1)));
				if (t % 5 == 0) {
					FxEngine.playMoving(VfxIds.AMBER_SWARM, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 17L, (float) v.x, (float) v.y, (float) v.z);
				}
				if (t % 8 == 0) {
					ClientFx.light(p, 0.5F, 0xFFC060);
				}
			}
			return super.tick(world);
		}

		/** A point on (or {@code k} times out from) the drop's skin in direction (x fwd, y, z right), in the world. */
		private Vec3d skin(double dx, double dy, double dz, double k) {
			double s = 1 / Math.max(0.05, Amber.drop(dx, dy, dz));
			for (int i = 0; i < 3; i++) {
				s /= Math.max(0.05, Amber.drop(dx * s, dy * s, dz * s));
			}
			s *= k;
			return c.add(0, Amber.CY, 0).add(FossilShapes.frame(h, dx * s, dy * s, dz * s));
		}

		@Override
		public boolean early() {
			return true;
		}

		/** Inside the amber: the dragonfly dark and still, a warm glow round it. */
		@Override
		public void renderEarly(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a < Amber.RISE + 30 || a >= Amber.SHATTER) {
				return;
			}
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			float in = smooth((a - Amber.RISE - 30) / 60.0);
			FossilPoses.Fly fly = FossilPoses.fly(Amber.SHATTER - 1, h, true);
			drawFly(consumers, m, c.subtract(cam), cb, fly, a, 0.55F, in);
			Vec3d core = c.add(0, Amber.CY, 0).subtract(cam);
			float throb = 0.8F + 0.2F * MathHelper.sin(a * 0.1F) + 0.4F * smooth((a - Amber.CRACK) / 200.0);
			FxKit.halo(consumers, m, cb, core, 9 * throb, 0xFF9020, 0.18F * in);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d o = c.subtract(cam);
			// Cracks of light running over the skin, longer and brighter until it bursts.
			if (a >= Amber.CRACK && a < Amber.SHATTER) {
				float k = (a - Amber.CRACK) / (float) (Amber.SHATTER - Amber.CRACK);
				for (int i = 0; i < CRACKS; i++) {
					double th = Effects.hash(i, 201) * MathHelper.TAU, y = Effects.hash(i, 202) * 1.4 - 0.6;
					int steps = (int) (2 + k * 9 * (0.5 + Effects.hash(i, 203)));
					Vec3d prev = null;
					for (int s = 0; s <= steps; s++) {
						double yy = MathHelper.clamp(y, -0.9, 0.9), r = Math.sqrt(1 - yy * yy);
						Vec3d p = skin(Math.cos(th) * r, yy, Math.sin(th) * r, 1.06).subtract(cam);
						if (prev != null) {
							float fl = 0.7F + 0.3F * MathHelper.sin(a * 0.6F + i + s);
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.35, 0xFFD060, (0.5F + 0.4F * k) * fl, (0.5F + 0.4F * k) * fl);
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.2, 0xFF8010, 0.15F * k, 0.15F * k);
						}
						prev = p;
						th += (Effects.hash(i * 13 + s, 204) - 0.5) * 0.5;
						y += (Effects.hash(i * 13 + s, 205) - 0.5) * 0.35;
					}
				}
			}
			// The resin's lip rolling out: a glossy amber band riding the ground.
			if (a >= Amber.OOZE && a < Amber.OOZE_END + 30) {
				double front = Amber.front(a);
				float on = smooth((a - Amber.OOZE) / 10.0) * (1 - smooth((a - Amber.OOZE_END) / 30.0));
				VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
				int n = 96;
				Vec3d prev = null;
				for (int i = 0; i <= n; i++) {
					double ang = MathHelper.TAU * i / n;
					double x = c.x + Math.cos(ang) * front, z = c.z + Math.sin(ang) * front;
					double wob = 0.25 * Math.sin(ang * 7 + a * 0.2);
					Vec3d p = new Vec3d(x, NewFx.groundY(x, z, c.y) + 0.35 + wob, z).subtract(cam);
					if (prev != null) {
						FxDraw.ribbon(shade, m, prev, p, Vec3d.ZERO, 1.5, 0xC06810, 0.55F * on, 0.55F * on);
						float sh = (float) (0.25 + 0.2 * Math.sin(ang * 11 - a * 0.25));
						FxDraw.ribbon(glow, m, prev.add(0, 0.25, 0), p.add(0, 0.25, 0), Vec3d.ZERO, 0.45, 0xFFC860, sh * on, sh * on);
					}
					prev = p;
				}
			}
			// Alive: the dragonfly flies.
			if (a >= Amber.SHATTER && a < Amber.ENTOMB + 2) {
				FossilPoses.Fly fly = FossilPoses.fly(a, h, false);
				drawFly(consumers, m, o, cb, fly, a, 1.0F, 1.0F);
			}
			// Sealed again: resin glowing as it closes over it.
			if (a >= Amber.ENTOMB && a < Amber.ENTOMB + 90) {
				float k = 1 - smooth((a - Amber.ENTOMB) / 90.0);
				FxKit.halo(consumers, m, cb, o.add(0, Amber.SEAL_Y, 0), 14, 0xFFA030, 0.3F * k);
			}
		}

		/** The dragonfly: a bronze body banded in teal, great green eyes, four shimmering wings. */
		private static void drawFly(VertexConsumerProvider consumers, Matrix4f m, Vec3d o, Vec3d[] cb, FossilPoses.Fly fly, float a, float bright, float alpha) {
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			int body = Solids.scale(0x6A4A28, bright), band = Solids.scale(0x2A8090, bright);
			if (alpha > 0.5F) {
				for (Seg s : fly.segs) {
					double sc = FossilPoses.FLY_SC;
					Solids.tube(solid, m, o.add(s.a()), o.add(s.b()), s.ra() * sc, s.rb() * sc, 7, s.fire() ? band : body, 1.0F, true);
				}
				for (Vec3d e : fly.eyes) {
					Solids.sphere(solid, m, o.add(e), 0.85, Solids.scale(0x30A890, bright), 6, 10, true);
				}
			}
			for (Vec3d e : fly.eyes) {
				FxKit.halo(consumers, m, cb, o.add(e), 1.6, 0x40FFD0, 0.35F * alpha * bright);
			}
			// Wings: a sheen that shifts with the angle, veins along them.
			for (int i = 0; i < fly.wings.size(); i++) {
				Vec3d[] w = fly.wings.get(i);
				Vec3d A = o.add(w[0]), B = o.add(w[1]), C = o.add(w[2]), D = o.add(w[3]);
				double hue = 0.5 + 0.5 * Math.sin(a * 0.05 + i * 1.7);
				int col = Solids.mix(0x60E0FF, 0xFFD070, hue);
				quad(glow, m, A, B, C, D, col, 0.2F * alpha, 0.1F * alpha, 0.1F * alpha, 0.2F * alpha);
				quad(glow, m, D, C, B, A, col, 0.2F * alpha, 0.1F * alpha, 0.1F * alpha, 0.2F * alpha);
				FxDraw.ribbon(glow, m, A, B, Vec3d.ZERO, 0.14, 0xE0FFF8, 0.4F * alpha, 0.2F * alpha);
				FxDraw.ribbon(glow, m, D, C, Vec3d.ZERO, 0.1, 0xE0FFF8, 0.25F * alpha, 0.12F * alpha);
				for (int v = 1; v <= 3; v++) {
					double u = v / 4.0;
					FxDraw.ribbon(glow, m, A.lerp(B, u), D.lerp(C, u), Vec3d.ZERO, 0.08, 0xE0FFF8, 0.2F * alpha, 0.2F * alpha);
				}
				// A bright stigma spot near each wingtip.
				FxKit.halo(consumers, m, cb, A.lerp(B, 0.86).lerp(D.lerp(C, 0.86), 0.15), 0.6, 0xFFE0A0, 0.4F * alpha);
			}
		}
	}

	// ====================================================================== SABRE-TOOTH

	/**
	 * The tar pits' glossy surfaces, swelling and popping bubbles, and the sabre-tooth itself, bone by bone, black with
	 * tar where it has been under, embers of amber in its eye sockets.
	 */
	static final class SabreFx extends FxEffect {
		private static final int BONE = 0xD8CCB0, TAR = 0x15100C, IVORY = 0xF4ECD8;
		private final Vec3d c, h;
		private final double[] poolY = new double[Sabre.POOLS];
		private double gait, prevGait;

		SabreFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
			java.util.Arrays.fill(poolY, Double.NaN);
		}

		private Vec3d poolCentre(int i) {
			double[] p = Sabre.pool(i);
			return c.add(FossilShapes.frame(h, p[0], 0, p[1]));
		}

		private double ground(double t) {
			Vec3d l = Sabre.pos(t);
			Vec3d w = c.add(FossilShapes.frame(h, l.x, 0, l.z));
			return NewFx.groundY(w.x, w.z, c.y) - c.y;
		}

		private FossilPoses.Cat pose(double t, double g) {
			return FossilPoses.cat(t, g, ground(t), h);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			prevGait = gait;
			Vec3d d = Sabre.pos(t).subtract(Sabre.pos(t - 1));
			gait += Math.hypot(d.x, d.z) / FossilPoses.CAT_SC;
			double front = Sabre.front(t);
			for (int i = 0; i < Sabre.POOLS; i++) {
				double[] p = Sabre.pool(i);
				if (Double.isNaN(poolY[i]) && Math.hypot(p[0], p[1]) + p[2] < front - 2 && t > 10) {
					Vec3d pc = poolCentre(i);
					poolY[i] = NewFx.groundY(pc.x, pc.z, c.y);
				}
			}
			if (t <= Sabre.TAR_END) {
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double x = c.x + Math.cos(a) * front, z = c.z + Math.sin(a) * front;
					FxEngine.play(VfxIds.TAR_FRONT, x, NewFx.groundY(x, z, c.y), z, 0, 1, 0, 1.0F, 0, t * 131L + i);
				}
			}
			if (t % 3 == 0 && t < Sabre.END - 20) {
				int i = world.random.nextInt(Sabre.POOLS);
				if (!Double.isNaN(poolY[i])) {
					double[] p = Sabre.pool(i);
					double a = world.random.nextDouble() * MathHelper.TAU, r = Math.sqrt(world.random.nextDouble()) * p[2] * 0.8;
					Vec3d q = poolCentre(i).add(Math.cos(a) * r, 0, Math.sin(a) * r);
					FxEngine.play(VfxIds.TAR_BUBBLE, q.x, poolY[i] + 0.05, q.z, 0, 1, 0, 1.0F, 0, t * 53L);
				}
			}
			boolean dripping = t > Sabre.EMERGE && t < Sabre.PROWL || t > Sabre.LAND && t < Sabre.SUNK;
			if (dripping && t % 2 == 0) {
				FossilPoses.Cat cat = pose(t, gait);
				Seg s = cat.segs.get(world.random.nextInt(cat.segs.size()));
				Vec3d p = c.add(s.a().lerp(s.b(), world.random.nextDouble()));
				FxEngine.play(VfxIds.TAR_DRIP, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 71L);
			}
			if (t > Sabre.EMERGE + 30 && t < Sabre.SUNK && t % 10 == 0) {
				FossilPoses.Cat cat = pose(t, gait);
				ClientFx.light(c.add(cat.eyes[0]), 0.45F, 0xFFA030);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			float end = 1 - smooth((a - (Sabre.END - 30)) / 30.0);
			// The pits: black, glossy, a sheen sliding over them, bubbles swelling and bursting.
			for (int i = 0; i < Sabre.POOLS; i++) {
				if (Double.isNaN(poolY[i])) {
					continue;
				}
				double r = Sabre.pool(i)[2];
				Vec3d pc = poolCentre(i);
				Vec3d p = new Vec3d(pc.x, poolY[i] + 0.03, pc.z).subtract(cam);
				FxDraw.disc(shade, m, p, X, Z, r * 0.98, 0x0C0906, 0.75F * end, 0.6F * end, 32);
				FxDraw.ring(glow, m, p.add(0, 0.02, 0), X, Z, r * 0.55, r * 0.95, 0x8A7458, 0.0F, 0.07F * end, 0.0F, 32);
				double sa = a * 0.01 + i * 1.3;
				Vec3d sheen = p.add(Math.cos(sa) * r * 0.4, 0.04, Math.sin(sa) * r * 0.4);
				FxDraw.disc(glow, m, sheen, X, Z.multiply(0.4), r * 0.35, 0xB09878, 0.08F * end, 0.0F, 16);
				for (int k = 0; k < 3; k++) {
					double period = 34 + 9 * Effects.hash(i, k + 5);
					double ph = ((a + Effects.hash(i, k) * period) % period) / period;
					double ang = Effects.hash(i * 3 + k, (int) ((a + Effects.hash(i, k) * period) / period)) * MathHelper.TAU;
					double rr = Math.sqrt(Effects.hash(k, i + 9)) * r * 0.7;
					double br = (0.4 + 0.8 * Effects.hash(i, k + 11)) * Math.sin(ph * Math.PI * 0.5) * (ph < 0.92 ? 1 : 0);
					if (br > 0.05) {
						Vec3d bp = p.add(Math.cos(ang) * rr, 0, Math.sin(ang) * rr);
						FxDraw.sphere(shade, m, bp, br, Vec3d.ZERO, 0x1A1410, 0.85F * end, 0.9F * end, 6, 10);
						Solids.dot(glow, m, bp.add(0, br * 0.7, 0).add(cb[0].multiply(-br * 0.3)), cb, br * 0.3, 0x9A8466, 0.3F * end);
					}
				}
			}
			// The cat.
			if (a >= Sabre.EMERGE - 2 && a < Sabre.SUNK) {
				double g = MathHelper.lerp(td, prevGait, gait);
				FossilPoses.Cat cat = pose(a, g);
				Vec3d o = c.subtract(cam);
				VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
				double gr = ground(a);
				// Tar clings to whatever has been under: everything while it climbs out, rising up the legs as it sinks.
				double tarLine = a < Sabre.PROWL + 40 ? 6 * (1 - smooth((a - Sabre.EMERGED) / 120.0)) + 1.2 : a >= Sabre.SINK ? 2.5 : 1.0;
				for (Seg s : cat.segs) {
					double y = (s.a().y + s.b().y) * 0.5 - gr;
					double tar = MathHelper.clamp((tarLine - y) / 2.5, 0.0, 0.92);
					int col = s.fire() ? Solids.mix(IVORY, TAR, tar * 0.5) : Solids.mix(BONE, TAR, tar);
					double sc = FossilPoses.CAT_SC;
					Solids.tube(solid, m, o.add(s.a()), o.add(s.b()), s.ra() * sc, s.rb() * sc, 7, col, 1.0F, true);
				}
				float eye = smooth((a - Sabre.EMERGE - 20) / 30.0) * (1 - smooth((a - (Sabre.SUNK - 15)) / 15.0));
				float hot = 1 + 0.6F * (float) Sabre.gape(a);
				for (Vec3d e : cat.eyes) {
					Vec3d q = o.add(e);
					float fl = 0.8F + 0.2F * MathHelper.sin(a * 0.7F + (float) e.z);
					FxKit.halo(consumers, m, cb, q, 2.2 * fl * hot, 0xFF9010, 0.5F * eye);
					Solids.flare(glow, m, q, cb, 0.9 * hot, 0xFFE0A0, 0.85F * eye);
				}
			}
		}
	}

	// ====================================================================== AMMONITE

	/**
	 * Sutures of pearl light along the shell's sides as each chamber closes, the ghost of the animal reaching out of its
	 * mouth, and the spiral of light unwinding over the land.
	 */
	static final class AmmoniteFx extends FxEffect {
		private static final int TENTACLES = 10;
		private final Vec3d c, h;

		AmmoniteFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		private Vec3d world(Vec3d local) {
			return c.add(FossilShapes.frame(h, local));
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			Vec3d heart = world(new Vec3d(0, Ammonite.CY, 0));
			if (t > Ammonite.COIL && t < Ammonite.FADE + 60 && t % 18 == 0) {
				FxEngine.play(VfxIds.AMMO_MOTES, heart.x, heart.y, heart.z, 0, 1, 0, 1.0F, 0, t * 29L);
			}
			if (t > Ammonite.COIL && t < Ammonite.FADE + 80 && t % 10 == 0) {
				float k = smooth((t - Ammonite.COIL) / 200.0) * (1 - smooth((t - Ammonite.FADE) / 80.0));
				ClientFx.light(heart, 0.3F + 0.6F * k, 0xFFC8F0);
			}
			if (t >= Ammonite.SPIRAL && t <= Ammonite.SPIRAL_END && t % 4 == 0) {
				Vec3d f = world(Ammonite.ground(Ammonite.unwound(t)));
				ClientFx.light(f.add(0, 3, 0), 0.8F, 0xFFD0F0);
			}
			return super.tick(world);
		}

		/** How brightly the sutures burn: up as they form, up again when it wakes and pulses, out from the mouth inwards at the end. */
		private float sutureOn(double theta, float a) {
			float on = smooth((Ammonite.grown(a) - theta) / 0.6);
			float wake = 1 + 0.6F * smooth((a - Ammonite.AWAKEN) / 20.0) + 1.2F * (float) Sabre.bump(a, Ammonite.PULSE - 10, Ammonite.PULSE + 30);
			double off = Ammonite.THETA_MAX - (a - Ammonite.FADE) / 100.0 * Ammonite.THETA_MAX;
			float fade = a < Ammonite.FADE ? 1 : smooth((off - theta) / 1.5);
			return on * wake * fade;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			// Sutures: frilled lines of pearl across each side of the shell, one at every chamber wall.
			int k = 0;
			for (double th = 2.0; th < Ammonite.THETA_MAX - Ammonite.LIVING; th += Ammonite.SEPTUM, k++) {
				float on = sutureOn(th, a);
				if (on <= 0.01F) {
					continue;
				}
				double tr = Ammonite.K * Ammonite.rho(th), w = tr * Ammonite.WIDTH;
				Vec3d cl = Ammonite.coil(th), out = Ammonite.out(th);
				Vec3d tan = new Vec3d(-out.y, out.x, 0);
				int col = k % 2 == 0 ? 0xFFC8EC : 0x90E8FF;
				for (int side = -1; side <= 1; side += 2) {
					Vec3d prev = null;
					for (int i = 0; i <= 10; i++) {
						double u = -0.92 + 1.84 * i / 10.0;
						double frill = 0.22 * Math.sin(u * 11 + th * 5) * tr / 3;
						double lat = side * (w * Math.sqrt(Math.max(0, 1 - u * u)) + 0.6);
						Vec3d l = cl.add(out.multiply(u * tr)).add(tan.multiply(frill)).add(0, 0, lat);
						Vec3d p = world(l).subtract(cam);
						if (prev != null) {
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.32, col, 0.55F * Math.min(1.5F, on), 0.55F * Math.min(1.5F, on));
						}
						prev = p;
					}
					FxKit.halo(consumers, m, cb, world(cl.add(0, 0, side * (w + 0.7))).subtract(cam), tr * 0.9, col, 0.12F * Math.min(1.5F, on));
				}
			}
			// The ghost of the animal: tentacles reaching out of the mouth and feeling over the ground, eyes either side.
			float ghost = smooth((a - Ammonite.AWAKEN + 10) / 30.0) * (1 - smooth((a - Ammonite.FADE) / 50.0));
			if (ghost > 0.01F) {
				double th = Ammonite.THETA_MAX;
				double tr = Ammonite.K * Ammonite.rho(th), w = tr * Ammonite.WIDTH;
				Vec3d mouth = Ammonite.coil(th), out = Ammonite.out(th);
				Vec3d perp = new Vec3d(-out.y, out.x, 0);
				Vec3d dir = out.multiply(Ammonite.B).add(perp).normalize();
				Vec3d mw = world(mouth).subtract(cam);
				FxKit.halo(consumers, m, cb, mw, tr * 2.2, 0xFFB0E0, 0.25F * ghost);
				for (int i = 0; i < TENTACLES; i++) {
					double psi = MathHelper.TAU * i / TENTACLES;
					Vec3d root = mouth.add(out.multiply(Math.cos(psi) * tr * 0.55)).add(0, 0, Math.sin(psi) * w * 0.7);
					Vec3d spread = out.multiply(Math.cos(psi) * 0.35).add(0, 0, Math.sin(psi) * 0.45);
					Vec3d p = root;
					Vec3d prev = world(p).subtract(cam);
					double reach = 0.7 + 0.3 * Math.sin(a * 0.03 + i);
					for (int j = 1; j <= 12; j++) {
						double f = j / 12.0;
						double sway = Math.sin(a * 0.06 + i * 1.7 + j * 0.45);
						Vec3d d = dir.multiply(1 - f * 0.6).add(spread).add(0, -f * 0.9, 0).add(perp.multiply(sway * 0.3)).add(0, 0, Math.cos(a * 0.05 + i + j * 0.4) * 0.35);
						p = p.add(d.normalize().multiply(1.4 * reach));
						Vec3d q = world(p).subtract(cam);
						float al = 0.4F * ghost * (float) (1 - f * 0.6);
						int col = Solids.mix(0xFFB8E8, 0x90E0FF, f);
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.7 * (1 - f * 0.8), col, al, al * 0.9F);
						if (j % 3 == 0) {
							Solids.dot(glow, m, q, cb, 0.25 * (1 - f * 0.5), 0xFFF0FA, al);
						}
						prev = q;
					}
				}
				for (int side = -1; side <= 1; side += 2) {
					Vec3d eye = world(mouth.subtract(dir.multiply(1.6)).add(out.multiply(tr * 0.3)).add(0, 0, side * (w + 0.5))).subtract(cam);
					float fl = 0.85F + 0.15F * MathHelper.sin(a * 0.4F + side);
					FxKit.halo(consumers, m, cb, eye, 1.6 * fl, 0xFFE0FF, 0.5F * ghost);
					Solids.flare(glow, m, eye, cb, 0.8, 0xFFFFFF, 0.8F * ghost);
				}
			}
			// The spiral of light, unwinding over the land: a bright head, a long tail glowing down behind it.
			if (a >= Ammonite.SPIRAL && a < Ammonite.SPIRAL_END + 60) {
				double head = Ammonite.unwound(a);
				float on = smooth((a - Ammonite.SPIRAL) / 8.0) * (1 - smooth((a - Ammonite.SPIRAL_END) / 60.0));
				Vec3d prev = null;
				double phi = 0;
				while (phi <= head) {
					Vec3d l = Ammonite.ground(phi);
					Vec3d wp = world(l);
					Vec3d p = new Vec3d(wp.x, NewFx.groundY(wp.x, wp.z, c.y) + 0.4, wp.z).subtract(cam);
					if (prev != null) {
						float al = (float) (0.15 + 0.75 * Math.exp(-(head - phi) / 1.6)) * on;
						int col = Solids.mix(0xFFC8EC, 0x90E8FF, 0.5 + 0.5 * Math.sin(phi * 1.3 - a * 0.08));
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.6, col, al * 0.5F, al * 0.5F);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.5, 0xFFF4FC, al, al);
					}
					prev = p;
					double r = l.length();
					phi += 1.2 / Math.max(r, 1);
				}
				if (prev != null && a <= Ammonite.SPIRAL_END + 5) {
					FxKit.halo(consumers, m, cb, prev.add(0, 1.2, 0), 5, 0xFFD8F4, 0.6F * on);
					Solids.flare(glow, m, prev.add(0, 1.2, 0), cb, 3.0, 0xFFFFFF, 0.9F * on);
				}
			}
			// The pulse: a ring of pearl light racing out over the stone.
			if (a >= Ammonite.PULSE && a < Ammonite.PULSE + 45) {
				float u = (a - Ammonite.PULSE) / 45.0F;
				double r = Ammonite.R * (1 - (1 - u) * (1 - u));
				FxKit.groundShock(consumers, m, c.subtract(cam).add(0, 1.5, 0), r, 0xFFD8F4, 0.6F * (1 - u));
			}
		}
	}
}
