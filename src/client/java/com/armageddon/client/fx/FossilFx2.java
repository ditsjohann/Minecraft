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
		private static final int CRACKS = 11, RINGS = 16, SEGS = 56;
		private final Vec3d c, h;
		/** Ground heights under the amber tide's surface, taken just before it floods (after, the ground is the amber). */
		private double[][] tideGround;

		AmberFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		private Vec3d tidePoint(int i, int j) {
			double r = Amber.R * i / RINGS, ang = MathHelper.TAU * j / SEGS;
			return new Vec3d(c.x + Math.cos(ang) * r, 0, c.z + Math.sin(ang) * r);
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
			if (t == Amber.TIDE - 2 && tideGround == null) {
				tideGround = new double[RINGS + 1][SEGS + 1];
				for (int i = 0; i <= RINGS; i++) {
					for (int j = 0; j <= SEGS; j++) {
						Vec3d p = tidePoint(i, j);
						tideGround[i][j] = NewFx.groundY(p.x, p.z, c.y);
					}
				}
			}
			if (t >= Amber.TIDE && t < Amber.TIDE_END && t % 3 == 0) {
				double front = Amber.tide(t);
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double x = c.x + Math.cos(a) * front, z = c.z + Math.sin(a) * front;
					FxEngine.play(VfxIds.AMBER_FRONT, x, NewFx.groundY(x, z, c.y) + 1, z, 0, 1, 0, 1.4F, 0, t * 191L + i);
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
			// Light streaming out through the cracks: god rays, longer as it gives.
			if (a >= Amber.CRACK + 20 && a < Amber.SHATTER + 6) {
				float k = smooth((a - Amber.CRACK - 20) / (double) (Amber.SHATTER - Amber.CRACK - 20)) * (1 - smooth((a - Amber.SHATTER) / 6.0));
				Vec3d core = c.add(0, Amber.CY, 0).subtract(cam);
				for (int i = 0; i < CRACKS; i++) {
					double th = Effects.hash(i, 201) * MathHelper.TAU, y = MathHelper.clamp(Effects.hash(i, 202) * 1.4 - 0.6, -0.9, 0.9), r = Math.sqrt(1 - y * y);
					Vec3d from = skin(Math.cos(th) * r, y, Math.sin(th) * r, 1.0).subtract(cam);
					Vec3d dir = from.subtract(core).normalize();
					double len = 10 + 34 * k * (0.6 + 0.4 * Effects.hash(i, 206));
					float fl = 0.8F + 0.2F * MathHelper.sin(a * 0.4F + i * 2);
					FxDraw.ribbon(glow, m, from, from.add(dir.multiply(len)), Vec3d.ZERO, 2.6 + 2 * k, 0xFFB040, 0.3F * k * fl, 0.0F);
					FxDraw.ribbon(glow, m, from, from.add(dir.multiply(len * 0.6)), Vec3d.ZERO, 0.8, 0xFFF0C0, 0.5F * k * fl, 0.0F);
				}
			}
			// The amber tide: a glossy surface rising over the land, light crawling over it, its lip racing outward.
			if (tideGround != null && a >= Amber.TIDE && a < Amber.TIDE_END + 30) {
				double front = Amber.tide(a);
				float on = 1 - smooth((a - Amber.TIDE_END) / 30.0);
				VertexConsumer tshade = consumers.getBuffer(FxRenderLayers.SHADE);
				for (int i = 0; i < RINGS; i++) {
					double r0 = Amber.R * i / RINGS, r1 = Amber.R * (i + 1) / RINGS;
					if (r0 > front) {
						break;
					}
					float e0 = (float) MathHelper.clamp((front - r0) / 4.0, 0.0, 1.0), e1 = (float) MathHelper.clamp((front - r1) / 4.0, 0.0, 1.0);
					for (int j = 0; j < SEGS; j++) {
						Vec3d[] q = new Vec3d[4];
						int[][] ix = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
						for (int v = 0; v < 4; v++) {
							Vec3d p = tidePoint(ix[v][0], ix[v][1]);
							double rise = (ix[v][0] == i ? e0 : e1) * Amber.TIDE_H;
							q[v] = new Vec3d(p.x, tideGround[ix[v][0]][ix[v][1]] + rise + 0.05 + 0.08 * Math.sin(p.x * 0.3 + a * 0.1), p.z).subtract(cam);
						}
						quad(tshade, m, q[0], q[1], q[2], q[3], 0xC06A10, 0.4F * on * e0, 0.4F * on * e1, 0.4F * on * e1, 0.4F * on * e0);
						float cs = (float) (0.5 + 0.5 * Math.sin(q[0].x * 0.35 + cam.x * 0.35 + a * 0.08) * Math.sin(q[0].z * 0.4 + cam.z * 0.4 - a * 0.07));
						quad(glow, m, q[0], q[1], q[2], q[3], 0xFFC060, 0.12F * on * cs * e0, 0.12F * on * cs * e1, 0.12F * on * cs * e1, 0.12F * on * cs * e0);
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
			// Alive: the dragonfly flies, its brood trailing it out of the amber, and they scatter as it is sealed again.
			if (a >= Amber.SHATTER && a < Amber.ENTOMB + 2) {
				FossilPoses.Fly fly = FossilPoses.fly(a, h, false);
				drawFly(consumers, m, o, cb, fly, a, 1.0F, 1.0F);
				float brood = smooth((a - Amber.SHATTER - 10) / 20.0) * (1 - smooth((a - Amber.FLY_END) / 30.0));
				if (brood > 0.02F) {
					for (int k = 0; k < Amber.BROOD; k++) {
						drawFly(consumers, m, o, cb, FossilPoses.fly(a, h, false, k), a + k * 9, 1.0F, brood);
					}
				}
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
					double sc = fly.sc;
					Solids.tube(solid, m, o.add(s.a()), o.add(s.b()), s.ra() * sc, s.rb() * sc, 7, s.fire() ? band : body, 1.0F, true);
				}
				for (Vec3d e : fly.eyes) {
					Solids.sphere(solid, m, o.add(e), 0.85 * fly.sc, Solids.scale(0x30A890, bright), 6, 10, true);
				}
			}
			for (Vec3d e : fly.eyes) {
				FxKit.halo(consumers, m, cb, o.add(e), 1.6 * fly.sc, 0x40FFD0, 0.35F * alpha * bright);
			}
			// Wings: a sheen that shifts with the angle, veins along them.
			for (int i = 0; i < fly.wings.size(); i++) {
				Vec3d[] w = fly.wings.get(i);
				Vec3d A = o.add(w[0]), B = o.add(w[1]), C = o.add(w[2]), D = o.add(w[3]);
				double hue = 0.5 + 0.5 * Math.sin(a * 0.05 + i * 1.7);
				int col = Solids.mix(0x60E0FF, 0xFFD070, hue);
				quad(glow, m, A, B, C, D, col, 0.2F * alpha, 0.1F * alpha, 0.1F * alpha, 0.2F * alpha);
				quad(glow, m, D, C, B, A, col, 0.2F * alpha, 0.1F * alpha, 0.1F * alpha, 0.2F * alpha);
				FxDraw.ribbon(glow, m, A, B, Vec3d.ZERO, 0.14 * fly.sc, 0xE0FFF8, 0.4F * alpha, 0.2F * alpha);
				FxDraw.ribbon(glow, m, D, C, Vec3d.ZERO, 0.1 * fly.sc, 0xE0FFF8, 0.25F * alpha, 0.12F * alpha);
				for (int v = 1; v <= 3; v++) {
					double u = v / 4.0;
					FxDraw.ribbon(glow, m, A.lerp(B, u), D.lerp(C, u), Vec3d.ZERO, 0.08 * fly.sc, 0xE0FFF8, 0.2F * alpha, 0.2F * alpha);
				}
				// A bright stigma spot near each wingtip.
				FxKit.halo(consumers, m, cb, A.lerp(B, 0.86).lerp(D.lerp(C, 0.86), 0.15), 0.6 * fly.sc, 0xFFE0A0, 0.4F * alpha);
			}
		}
	}

	// ====================================================================== SABRE-TOOTH

	/**
	 * The hunt: eyes burning in the dusk, claw swipes of ember light across the ground, then the cat itself - bones
	 * wrapped in a rim-lit ghost of ember flesh, eyes ablaze - and at the end the colossal ghost of its skull.
	 */
	static final class SabreFx extends FxEffect {
		private static final int BONE = 0xE6DAC0, IVORY = 0xFAF2DE, EMBER = 0xFF5A18;
		private final Vec3d c, h;
		private double gait, prevGait;

		SabreFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		private double ground(double t) {
			Vec3d l = Sabre.pos(t);
			Vec3d w = c.add(FossilShapes.frame(h, l.x, 0, l.z));
			return NewFx.groundY(w.x, w.z, c.y) - c.y;
		}

		private FossilPoses.Cat pose(double t, double g) {
			return FossilPoses.cat(t, g, ground(t), h);
		}

		/** Where the watcher's eyes burn before it shows itself. */
		private Vec3d watcher() {
			Vec3d w = c.add(FossilShapes.frame(h, -28, 0, 10));
			return new Vec3d(w.x, NewFx.groundY(w.x, w.z, c.y) + 3.2, w.z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			prevGait = gait;
			Vec3d d = Sabre.pos(t).subtract(Sabre.pos(t - 1));
			gait += Math.hypot(d.x, d.z) / FossilPoses.CAT_SC;
			boolean alive = t > Sabre.EMERGE + 20 && t < Sabre.PETRIFY;
			if (alive && t % 2 == 0) {
				FossilPoses.Cat cat = pose(t, gait);
				Seg s = cat.flesh.get(world.random.nextInt(cat.flesh.size()));
				Vec3d p = c.add(s.a().lerp(s.b(), world.random.nextDouble()));
				Vec3d v = FossilShapes.frame(h, d);
				FxEngine.playMoving(VfxIds.SABRE_EMBERS, p.x, p.y, p.z, 0, 1, 0, 1.0F, 0, t * 71L, (float) v.x, (float) v.y, (float) v.z);
			}
			if (alive && t % 8 == 0) {
				FossilPoses.Cat cat = pose(t, gait);
				ClientFx.light(c.add(cat.body), 0.55F, 0xFF6020);
			}
			if (t < Sabre.EMERGE && t % 10 == 0) {
				ClientFx.light(watcher(), 0.3F, 0xFF5010);
			}
			if (t >= Sabre.GIANT && t < Sabre.BITE + 40 && t % 4 == 0) {
				FossilPoses.Cat g = FossilPoses.giantSkull(t, h);
				ClientFx.light(c.add(g.head), (float) (1.2 * Sabre.giant(t)), 0xFF5010);
				Seg s = g.skull.get(world.random.nextInt(g.skull.size()));
				Vec3d p = c.add(s.a().lerp(s.b(), world.random.nextDouble()));
				FxEngine.play(VfxIds.SABRE_EMBERS, p.x, p.y, p.z, 0, 1, 0, 3.0F, 0, t * 13L);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d o = c.subtract(cam);
			// The watcher: two eyes in the gloom, blinking, gone when it breaks out of the ground.
			if (a < Sabre.EMERGE + 6) {
				float in = smooth((a - 8) / 20.0) * (1 - smooth((a - Sabre.EMERGE + 6) / 10.0));
				boolean blink = (a % 47) > 44;
				if (in > 0.01F && !blink) {
					Vec3d w = watcher().subtract(cam), side = FossilShapes.frame(h, 0, 0, 1);
					for (int s = -1; s <= 1; s += 2) {
						Vec3d e = w.add(side.multiply(s * 0.9));
						FxKit.halo(consumers, m, cb, e, 1.4, 0xFF4010, 0.5F * in);
						Solids.dot(glow, m, e, cb, 0.22, 0xFFE0A0, 0.95F * in);
					}
				}
			}
			// Claw swipes: three streaks of ember light raked along each cut, then cooling.
			for (int k = 0; k < Sabre.N_RAKES; k++) {
				float d = a - Sabre.rakeAt(k);
				if (d < -2 || d > 40) {
					continue;
				}
				double[] r = Sabre.rake(k);
				Vec3d dir = FossilShapes.frame(h, Math.cos(r[2]), 0, Math.sin(r[2])), side = new Vec3d(-dir.z, 0, dir.x);
				Vec3d start = c.add(FossilShapes.frame(h, r[0], 0, r[1]));
				double sweep = r[3] * smooth((d + 2) / 5.0);
				float cool = 1 - smooth(d / 40.0);
				for (int cl = -1; cl <= 1; cl++) {
					Vec3d prev = null;
					for (double u = 0; u <= sweep; u += 1.5) {
						double bend = Math.sin(u / r[3] * 2.2) * 1.2 * cl;
						Vec3d p = start.add(dir.multiply(u)).add(side.multiply(cl * 2.6 + bend));
						p = new Vec3d(p.x, NewFx.groundY(p.x, p.z, c.y) + 0.3, p.z).subtract(cam);
						if (prev != null) {
							float tip = (float) Math.min(1, (sweep - u) / 6.0 + 0.4);
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.6, 0xFFB060, 0.8F * cool * tip, 0.8F * cool * tip);
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 2.2, EMBER, 0.25F * cool, 0.25F * cool);
						}
						prev = p;
					}
				}
			}
			// The cat.
			if (a >= Sabre.EMERGE - 2 && a < Sabre.PETRIFY + 1) {
				double g = MathHelper.lerp(td, prevGait, gait);
				FossilPoses.Cat cat = pose(a, g);
				VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
				double sc = FossilPoses.CAT_SC;
				for (Seg s : cat.segs) {
					Solids.tube(solid, m, o.add(s.a()), o.add(s.b()), s.ra() * sc, s.rb() * sc, 8, s.fire() ? IVORY : BONE, 1.0F, true);
				}
				// The ghost of its flesh: soft, rim-lit, flickering like a banked fire, burning off at the end.
				float flesh = smooth((a - Sabre.EMERGED + 40) / 50.0) * (1 - smooth((a - (Sabre.PETRIFY - 25)) / 25.0));
				if (flesh > 0.01F) {
					float fl = 0.85F + 0.15F * MathHelper.sin(a * 0.37F) * MathHelper.sin(a * 0.13F + 1);
					for (Seg s : cat.flesh) {
						Vec3d A = o.add(s.a()), B = o.add(s.b());
						double len = A.distanceTo(B), ra = s.ra() * sc, rb = s.rb() * sc;
						int n = Math.max(1, (int) Math.ceil(len / (Math.min(ra, rb) * 0.9)));
						for (int i = 0; i <= n; i++) {
							double u = i / (double) n;
							FxDraw.sphere(glow, m, A.lerp(B, u), MathHelper.lerp(u, ra, rb), Vec3d.ZERO, EMBER, 0.025F * flesh * fl, 0.16F * flesh * fl, 8, 12);
						}
					}
				}
				float eye = smooth((a - Sabre.EMERGE - 20) / 30.0) * (1 - smooth((a - Sabre.PETRIFY) / 4.0));
				float hot = 1 + 0.8F * (float) Sabre.gape(a);
				for (Vec3d e : cat.eyes) {
					Vec3d q = o.add(e);
					float fl = 0.8F + 0.2F * MathHelper.sin(a * 0.7F + (float) e.z);
					FxKit.halo(consumers, m, cb, q, 2.4 * fl * hot, 0xFF6010, 0.55F * eye);
					Solids.flare(glow, m, q, cb, 1.0 * hot, 0xFFE0A0, 0.9F * eye);
				}
			}
			// The ghost skull: a colossal skull of ember light, jaws gaping, lunging down to bite.
			float gs = (float) Sabre.giant(a);
			if (gs > 0.01F) {
				FossilPoses.Cat g = FossilPoses.giantSkull(a, h);
				double sc = FossilPoses.CAT_SC;
				float fl = 0.85F + 0.15F * MathHelper.sin(a * 0.5F);
				for (Seg s : g.skull) {
					Vec3d A = o.add(s.a()), B = o.add(s.b());
					Solids.tube(glow, m, A, B, s.ra() * sc, s.rb() * sc, 10, s.fire() ? 0xFFC080 : EMBER, 0.09F * gs * fl, false);
					FxDraw.ribbon(glow, m, A, B, Vec3d.ZERO, Math.max(0.6, (s.ra() + s.rb()) * sc * 0.25), s.fire() ? 0xFFE8C0 : 0xFF9040, 0.35F * gs * fl,
							0.35F * gs * fl);
				}
				for (Vec3d e : g.eyes) {
					Vec3d q = o.add(e);
					FxKit.halo(consumers, m, cb, q, 12 * fl, 0xFF3010, 0.6F * gs);
					Solids.flare(glow, m, q, cb, 5, 0xFFE0A0, 0.9F * gs);
				}
				FxKit.halo(consumers, m, cb, o.add(g.head), 40, 0xFF4010, 0.12F * gs);
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
