package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.ElementShapes.Inferno;
import com.armageddon.util.ElementShapes.Maelstrom;
import com.armageddon.util.ElementShapes.Roller;
import com.armageddon.util.ElementShapes.Wrecker;
import com.armageddon.util.FossilShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Client parts of the Wrecking Ball, the Flattener, the Inferno Vortex and the Maelstrom. */
final class ElementFx {
	private ElementFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 72 -> new WreckerFx(a, b, p.duration());
			case 73 -> new RollerFx(a, b, p.duration());
			case 74 -> new InfernoFx(a, b, p.duration());
			case 75 -> new MaelstromFx(a, b, p.duration());
			default -> null;
		};
	}

	private static Vec3d unit(Vec3d h) {
		return h.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : new Vec3d(h.x, 0, h.z).normalize();
	}

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}

	// ====================================================================== WRECKING BALL

	/** The ball of steel and the chain it hangs on, running up out of sight into the sky; a smear of motion when it flies. */
	static final class WreckerFx extends FxEffect {
		private static final int STEEL = 0x5E646C, DARK = 0x33373C, CHAIN = 0x4A4E54;
		private final Vec3d c, h;

		WreckerFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		private Vec3d ball(double t) {
			return c.add(FossilShapes.frame(h, Wrecker.ball(t)));
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a > Wrecker.LAND + 2) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d b = ball(a).subtract(cam);
			double r = Wrecker.BALL;
			// The ball: a sphere of steel with a darker band round it and a heavy collar at the top where the chain hooks on.
			Solids.ellipsoid(solid, m, b, r, r, r, 14, 22, (la, lo, n) -> Math.abs(n.y) < 0.12 ? DARK : STEEL, true, 1.0F);
			Solids.tube(solid, m, b.add(0, r * 0.85, 0), b.add(0, r * 1.15, 0), r * 0.32, r * 0.26, 10, DARK, 1.0F, true);
			// The chain: from the collar up to the pivot, then straight up into the sky. After it snaps, its end whips away upward.
			Vec3d pivot = c.add(0, Wrecker.PIVOT, 0).subtract(cam);
			Vec3d hook = b.add(0, r * 1.15, 0);
			if (a >= Wrecker.SNAP) {
				double u = (a - Wrecker.SNAP) / 20.0;
				hook = pivot.add(hook.subtract(pivot).multiply(Math.max(0, 1 - u))).add(0, u * u * 60, 0);
			}
			chain(solid, m, hook, pivot);
			Solids.tube(solid, m, pivot, pivot.add(0, 520, 0), 0.7, 0.7, 6, CHAIN, 1.0F, true);
			// A smear of motion behind it when it is really moving.
			Vec3d prev = b;
			for (int i = 1; i <= 6; i++) {
				Vec3d q = ball(a - i).subtract(cam);
				double sp = prev.distanceTo(q);
				if (sp > 2) {
					FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, r * 1.6, 0x9AA0A8, 0.05F * (7 - i) / 6F, 0.04F * (7 - i) / 6F);
				}
				prev = q;
			}
		}

		/** A chain of heavy links from {@code a} to {@code b}, each link turned square to the last. */
		private static void chain(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b) {
			Vec3d d = b.subtract(a);
			double len = d.length();
			if (len < 0.1) {
				return;
			}
			Vec3d dir = d.multiply(1 / len);
			Vec3d s1 = dir.crossProduct(new Vec3d(0, 0, 1));
			s1 = s1.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : s1.normalize();
			Vec3d s2 = dir.crossProduct(s1).normalize();
			double link = 2.6;
			int n = (int) Math.min(90, len / link);
			for (int i = 0; i < n; i++) {
				Vec3d p0 = a.add(dir.multiply(i * link)), p1 = a.add(dir.multiply((i + 1) * link));
				Vec3d side = (i % 2 == 0 ? s1 : s2).multiply(0.75);
				Solids.tube(vc, m, p0.add(side), p1.add(side), 0.3, 0.3, 5, CHAIN, 1.0F, true);
				Solids.tube(vc, m, p0.subtract(side), p1.subtract(side), 0.3, 0.3, 5, CHAIN, 1.0F, true);
				Solids.tube(vc, m, p0.add(side), p0.subtract(side), 0.3, 0.3, 5, CHAIN, 1.0F, true);
			}
			if (n * link < len) {
				Solids.tube(vc, m, a.add(dir.multiply(n * link)), b, 0.7, 0.7, 6, CHAIN, 1.0F, true);
			}
		}
	}

	// ====================================================================== THE FLATTENER

	/** The roller: a cylinder of steel turning as it rolls, its ends capped with hubs, arms up from them into the sky. */
	static final class RollerFx extends FxEffect {
		private static final int STEEL = 0x8A9098, GROOVE = 0x4A4E54, HUB = 0x5A5E64;
		private final Vec3d c, h, r;

		RollerFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
			this.r = new Vec3d(-h.z, 0, h.x);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d axle = c.add(h.multiply(Roller.x(a))).add(0, Roller.y(a), 0).subtract(cam);
			double R = Roller.RADIUS, L = Roller.HALF;
			Vec3d e0 = axle.subtract(r.multiply(L)), e1 = axle.add(r.multiply(L));
			Solids.tube(solid, m, e0, e1, R, R, 40, STEEL, 1.0F, true);
			Solids.tube(solid, m, e1, e0, R, R, 40, STEEL, 1.0F, true);
			// Grooves running round it, and lines along it that show it turning.
			double spin = Roller.spin(a);
			for (int i = 0; i < 16; i++) {
				double ang = spin + i * MathHelper.TAU / 16;
				Vec3d off = h.multiply(Math.cos(ang) * (R + 0.05)).add(0, Math.sin(ang) * (R + 0.05), 0);
				Solids.tube(solid, m, e0.add(off), e1.add(off), 0.35, 0.35, 4, GROOVE, 1.0F, false);
			}
			for (int k = 1; k < 8; k++) {
				Vec3d p = e0.add(r.multiply(2 * L * k / 8.0));
				Solids.tube(solid, m, p.subtract(r.multiply(0.6)), p.add(r.multiply(0.6)), R + 0.3, R + 0.3, 40, GROOVE, 1.0F, true);
			}
			// Hubs, and the arms that hold it, reaching up out of sight.
			for (int s = -1; s <= 1; s += 2) {
				Vec3d end = s < 0 ? e0 : e1;
				Vec3d out = r.multiply(s);
				Solids.tube(solid, m, end, end.add(out.multiply(4)), R * 0.4, R * 0.35, 20, HUB, 1.0F, true);
				Vec3d hubEnd = end.add(out.multiply(4));
				Solids.tube(solid, m, hubEnd, hubEnd.add(out.multiply(10)).add(h.multiply(-30)).add(0, 400, 0), 3, 2, 8, HUB, 1.0F, true);
			}
		}
	}

	// ====================================================================== INFERNO VORTEX

	/** Drives the fire whirls: each a funnel of flame kept alive frame after frame where it is, then the one great pillar. */
	static final class InfernoFx extends FxEffect {
		private final Vec3d c, h;

		InfernoFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t >= Inferno.RISE && t < Inferno.MERGE + 6) {
				float grow = smooth((t - Inferno.RISE) / 40.0) * (1 + (float) (t - Inferno.RISE) / (Inferno.MERGE - Inferno.RISE) * 0.6F);
				for (int k = 0; k < Inferno.WHIRLS; k++) {
					Vec3d p = c.add(FossilShapes.frame(h, Inferno.whirl(k, t))), q = c.add(FossilShapes.frame(h, Inferno.whirl(k, t - 1)));
					double gy = NewFx.groundY(p.x, p.z, c.y);
					FxEngine.playMoving(VfxIds.INFERNO_WHIRL, p.x, gy, p.z, 0, 1, 0, grow, 0, t * 31L + k, (float) (p.x - q.x), 0, (float) (p.z - q.z));
					if (t % 6 == k % 6) {
						ClientFx.light(new Vec3d(p.x, gy + 10, p.z), 1.0F, 0xFF6010);
					}
				}
			}
			if (t >= Inferno.MERGE && t < Inferno.COLLAPSE) {
				float grow = 2.4F + 1.2F * smooth((t - Inferno.MERGE) / 40.0);
				FxEngine.play(VfxIds.INFERNO_WHIRL, c.x, c.y, c.z, 0, 1, 0, grow, 0, t * 17L);
				if (t % 4 == 0) {
					ClientFx.light(c.add(0, 30, 0), 2.0F, 0xFF7020);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			// The ground glowing under each whirl, and under the pillar.
			if (a >= Inferno.RISE && a < Inferno.MERGE) {
				for (int k = 0; k < Inferno.WHIRLS; k++) {
					Vec3d p = c.add(FossilShapes.frame(h, Inferno.whirl(k, a)));
					Vec3d g = new Vec3d(p.x, NewFx.groundY(p.x, p.z, c.y) + 1, p.z).subtract(cam);
					FxKit.halo(consumers, m, cb, g, 12, 0xFF5010, 0.35F);
				}
			}
			if (a >= Inferno.MERGE && a < Inferno.COLLAPSE + 20) {
				float k = smooth((a - Inferno.MERGE) / 20.0) * (1 - smooth((a - Inferno.COLLAPSE) / 20.0));
				FxKit.halo(consumers, m, cb, c.add(0, 4, 0).subtract(cam), 40, 0xFF6010, 0.45F * k);
				FxKit.halo(consumers, m, cb, c.add(0, 60, 0).subtract(cam), 60, 0xFF8020, 0.25F * k);
			}
		}
	}

	// ====================================================================== MAELSTROM

	/** The whirlpool's surface: arms of foam spiralling in, turning, and the dark throat of the vortex in the middle. */
	static final class MaelstromFx extends FxEffect {
		private static final int ARMS = 5;
		private final Vec3d c, h;
		private double turn, prevTurn;

		MaelstromFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		private double level() {
			return Math.floor(c.y) - 1 - Maelstrom.LEVEL + 1.05;
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			prevTurn = turn;
			turn += Maelstrom.spin(t) * 3;
			double open = Maelstrom.opened(t);
			if (open > 8 && t < Maelstrom.ERUPT + 20) {
				for (int i = 0; i < 3; i++) {
					double r = 6 + world.random.nextDouble() * (open - 6);
					double ang = turn + world.random.nextInt(ARMS) * MathHelper.TAU / ARMS + Math.log(r / 4) * 1.5;
					FxEngine.play(VfxIds.MAEL_SPRAY, c.x + Math.cos(ang) * r, level(), c.z + Math.sin(ang) * r, (float) -Math.sin(ang), 0.3F, (float) Math.cos(ang),
							1.0F, 0, t * 41L + i);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			double open = Maelstrom.opened(a);
			float on = smooth((open - 8) / 20.0) * (1 - smooth((a - Maelstrom.ERUPT - 20) / 60.0));
			if (on < 0.01F) {
				return;
			}
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			double turnA = MathHelper.lerp(td, prevTurn, turn);
			double lv = level() - cam.y;
			Vec3d o = c.subtract(cam);
			// Arms of foam spiralling in toward the throat.
			for (int k = 0; k < ARMS; k++) {
				Vec3d prev = null;
				for (double r = open; r > 5; r -= 1.2) {
					double ang = turnA + k * MathHelper.TAU / ARMS + Math.log(r / 4) * 1.5;
					double sink = r < 16 ? (16 - r) * 0.8 : 0;
					Vec3d p = new Vec3d(o.x + Math.cos(ang) * r, lv + 0.1 - sink, o.z + Math.sin(ang) * r);
					if (prev != null) {
						float al = on * (float) Math.min(1, (open - r) / 6 + 0.2);
						FxDraw.ribbon(shade, m, prev, p, Vec3d.ZERO, 1.2 + r * 0.04, 0xE8F4F8, 0.55F * al, 0.55F * al);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.6, 0xA0D8F0, 0.12F * al, 0.12F * al);
					}
					prev = p;
				}
			}
			// The throat: a dark funnel turning down into the deep.
			int n = 32;
			for (int ring = 0; ring < 6; ring++) {
				double r0 = 16 - ring * 2.6, r1 = 16 - (ring + 1) * 2.6;
				double y0 = lv - ring * 3.4, y1 = lv - (ring + 1) * 3.4;
				for (int j = 0; j < n; j++) {
					double a0 = turnA * 2 + MathHelper.TAU * j / n, a1 = turnA * 2 + MathHelper.TAU * (j + 1) / n;
					Vec3d p00 = new Vec3d(o.x + Math.cos(a0) * r0, y0, o.z + Math.sin(a0) * r0), p01 = new Vec3d(o.x + Math.cos(a1) * r0, y0, o.z + Math.sin(a1) * r0);
					Vec3d p10 = new Vec3d(o.x + Math.cos(a0) * r1, y1, o.z + Math.sin(a0) * r1), p11 = new Vec3d(o.x + Math.cos(a1) * r1, y1, o.z + Math.sin(a1) * r1);
					Solids.quad(shade, m, p00, p01, p11, p10, j % 4 == 0 ? 0x1E3E50 : 0x0A1A28, 0.75F * on);
				}
			}
		}
	}
}
