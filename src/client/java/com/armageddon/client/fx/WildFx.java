package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.WildShapes.Cactus;
import com.armageddon.util.WildShapes.Forge;
import com.armageddon.util.WildShapes.Infrit;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The big models of the wild bombs: the spinning saguaro that sheds its spikes, the Blackforge's anvil and hammer,
 * and the Blue Infrit's summoning circle and flaming hound's head. Particles come from the engine (WildVfx); timing
 * and geometry from {@code WildShapes}, shared with the server.
 */
public final class WildFx {
	private WildFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 50 -> new CactusFx(a, p.duration());
			case 51 -> new ForgeFx(a, p.duration());
			case 52 -> new InfritFx(a, (int) p.p1(), p.duration());
			default -> null;
		};
	}

	private static final Vec3d X = new Vec3d(1, 0, 0), Y = new Vec3d(0, 1, 0), Z = new Vec3d(0, 0, 1);

	/** A lit box on any three axes (unit, at right angles), {@code h*} half-extents along each. */
	static void box(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d ax, Vec3d ay, Vec3d az, double hx, double hy, double hz, int rgb, boolean lit) {
		Vec3d x = ax.multiply(hx), y = ay.multiply(hy), z = az.multiply(hz);
		face(vc, m, c.add(x), y, z, ax, rgb, lit);
		face(vc, m, c.subtract(x), z, y, ax.multiply(-1), rgb, lit);
		face(vc, m, c.add(y), z, x, ay, rgb, lit);
		face(vc, m, c.subtract(y), x, z, ay.multiply(-1), rgb, lit);
		face(vc, m, c.add(z), x, y, az, rgb, lit);
		face(vc, m, c.subtract(z), y, x, az.multiply(-1), rgb, lit);
	}

	private static void face(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d u, Vec3d v, Vec3d n, int rgb, boolean lit) {
		int col = lit ? Solids.scale(rgb, Solids.light(n)) : rgb;
		Solids.quad(vc, m, c.subtract(u).subtract(v), c.add(u).subtract(v), c.add(u).add(v), c.subtract(u).add(v), col, 1.0F);
	}

	/** A flat glowing strip on the ground from {@code a} to {@code b}. */
	static void groundLine(VertexConsumer glow, Matrix4f m, Vec3d a, Vec3d b, double width, int rgb, float alpha) {
		Vec3d d = b.subtract(a);
		Vec3d side = new Vec3d(-d.z, 0, d.x);
		if (side.lengthSquared() < 1.0E-8) {
			return;
		}
		side = side.normalize().multiply(width * 0.5);
		FxDraw.vertex(glow, m, a.x - side.x, a.y, a.z - side.z, rgb, alpha);
		FxDraw.vertex(glow, m, b.x - side.x, b.y, b.z - side.z, rgb, alpha);
		FxDraw.vertex(glow, m, b.x + side.x, b.y, b.z + side.z, rgb, alpha);
		FxDraw.vertex(glow, m, a.x + side.x, a.y, a.z + side.z, rgb, alpha);
	}

	// ====================================================================== CACTUS

	/** The saguaro: ribbed trunk and arms, domed tops, flowers, and the spikes that have not flown yet. */
	static final class CactusFx extends FxEffect {
		private static final int DARK = 0x467E2C, LIGHT = 0x5C9C38, SPIKE = 0xE8DCA8;
		private final Vec3d base;
		private final double[] dir = new double[3];

		CactusFx(Vec3d base, int life) {
			super(Math.max(20, life));
			this.base = base;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a >= Cactus.SOLID + 1) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			double sink = (1 - Cactus.risen(a)) * (Cactus.TRUNK_H + 3);
			Vec3d o = base.subtract(cam).add(0, -sink, 0);
			double turn = Cactus.angle(a);
			ribbed(solid, m, o, Vec3d.ZERO, Cactus.TRUNK_R, 0, Cactus.TRUNK_H - Cactus.TRUNK_R, turn);
			dome(solid, m, o.add(0, Cactus.TRUNK_H - Cactus.TRUNK_R, 0), Cactus.TRUNK_R);
			for (double[] arm : Cactus.ARMS) {
				Vec3d out = Cactus.turn(new Vec3d(Math.cos(arm[0]), 0, Math.sin(arm[0])), turn);
				Vec3d join = o.add(0, arm[1], 0), elbow = join.add(out.multiply(arm[2]));
				Solids.tube(solid, m, join, elbow, Cactus.ARM_R, Cactus.ARM_R, 14, LIGHT, 1.0F, true);
				dome(solid, m, elbow, Cactus.ARM_R);
				ribbed(solid, m, o, elbow.subtract(o).multiply(1, 0, 1), Cactus.ARM_R, arm[1], arm[1] + arm[3] - Cactus.ARM_R, turn);
				dome(solid, m, elbow.add(0, arm[3] - Cactus.ARM_R, 0), Cactus.ARM_R);
			}
			// Pale blossoms crowning the trunk.
			for (int k = 0; k < 5; k++) {
				Vec3d f = Cactus.turn(new Vec3d(Math.cos(k * 1.26) * 1.6, Cactus.TRUNK_H - 0.4, Math.sin(k * 1.26) * 1.6), turn);
				box(solid, m, o.add(f), X, Y, Z, 0.45, 0.35, 0.45, 0xF4E6B0, true);
			}
			for (int i = 0; i < Cactus.SPIKES; i++) {
				if (Cactus.fireTick(i) <= a) {
					continue;
				}
				Vec3d root = Cactus.turn(Cactus.spike(i, dir), turn);
				Vec3d d = Cactus.turn(new Vec3d(dir[0], dir[1], dir[2]), turn);
				Solids.tube(solid, m, o.add(root), o.add(root).add(d.multiply(2.4)), 0.2, 0.02, 4, SPIKE, 1.0F, true);
			}
			// The faster it turns, the more dust it whips up round its foot.
			float w = (float) (Cactus.omega(a) / Cactus.MAX_SPIN);
			if (w > 0.02F) {
				for (int k = 0; k < 3; k++) {
					FxDraw.ring(glow, m, o.add(0, 1 + k * 1.5, 0), X, Z, Cactus.TRUNK_R + 2 + k, Cactus.TRUNK_R + 6 + k * 2, 0xC8B488,
							0.0F, 0.12F * w, 0.0F, 32);
				}
			}
		}

		/** A ribbed column around {@code axis} (an offset from {@code o}), from {@code y0} to {@code y1}, turned by {@code turn}. */
		private static void ribbed(VertexConsumer vc, Matrix4f m, Vec3d o, Vec3d axis, double r, double y0, double y1, double turn) {
			int n = 16;
			Vec3d c0 = o.add(axis).add(0, y0, 0), c1 = o.add(axis).add(0, y1, 0);
			for (int i = 0; i < n; i++) {
				double a0 = turn + MathHelper.TAU * i / n, a1 = turn + MathHelper.TAU * (i + 1) / n;
				// Alternate faces stand proud: the ribs.
				double rr = i % 2 == 0 ? r * 1.07 : r;
				Vec3d n0 = new Vec3d(Math.cos(a0), 0, Math.sin(a0)), n1 = new Vec3d(Math.cos(a1), 0, Math.sin(a1));
				int col = Solids.scale(i % 2 == 0 ? LIGHT : DARK, Solids.light(n0.add(n1).normalize()));
				Solids.quad(vc, m, c0.add(n0.multiply(rr)), c0.add(n1.multiply(rr)), c1.add(n1.multiply(rr)), c1.add(n0.multiply(rr)), col, 1.0F);
			}
		}

		private static void dome(VertexConsumer vc, Matrix4f m, Vec3d c, double r) {
			Solids.ellipsoid(vc, m, c, r, r, r, 6, 14, (lat, lon, n) -> lon % 2 == 0 ? LIGHT : DARK, true, 1.0F);
		}
	}

	// ====================================================================== BLACKFORGE

	/** The black anvil, glowing at its seams with every blow, and the hammer that comes down out of the dark. */
	static final class ForgeFx extends FxEffect {
		private static final int IRON = 0x1C171B, IRON_LIGHT = 0x2A2328, HEAD = 0x2A2A32, HANDLE = 0x2A1C12;
		private final Vec3d c;

		ForgeFx(Vec3d c, int life) {
			super(Math.max(20, life));
			this.c = c;
		}

		/** How hot the anvil glows: a flare after each blow that cools off. */
		private static float heat(float a) {
			float h = 0.15F * (float) Forge.risen(a);
			for (int s : Forge.STRIKES) {
				if (a >= s) {
					h = Math.max(h, (float) Math.exp(-(a - s) / 30.0));
				}
			}
			return h;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			double rise = Forge.risen(a);
			Vec3d o = c.subtract(cam).add(0, -(1 - rise) * (Forge.ANVIL_H + 4), 0);
			float hot = heat(a);
			if (rise > 0.001) {
				// Foot, waist, face, horn and heel.
				box(solid, m, o.add(0, 2.5, 0), X, Y, Z, 13, 2.5, 8, IRON, true);
				box(solid, m, o.add(0, 9, 0), X, Y, Z, 7, 4, 5, IRON, true);
				box(solid, m, o.add(0, 17.5, 0), X, Y, Z, 17, 4.5, 7, IRON_LIGHT, true);
				Solids.tube(solid, m, o.add(17, 18.5, 0), o.add(31, 20, 0), 4.5, 0.3, 10, IRON_LIGHT, 1.0F, true);
				box(solid, m, o.add(-19.5, 17.5, 0), X, Y, Z, 2.5, 3.5, 5.5, IRON, true);
				// Molten seams: along the waist, round the face, and the face itself when it is struck.
				int seam = 0xFF6018;
				float s = 0.35F + 0.65F * hot;
				for (int side = -1; side <= 1; side += 2) {
					box(glow, m, o.add(0, 9, side * 5.05), X, Y, Z, 7, 0.25, 0.05, seam, false);
					box(glow, m, o.add(side * 7.05, 9, 0), X, Y, Z, 0.05, 0.25, 5, seam, false);
				}
				FxDraw.ring(glow, m, o.add(0, 22.06, 0), X, Z, 0, 16, 0xFF8030, 0.85F * hot, 0.45F * hot, 0.0F, 24);
				FxKit.halo(consumers, m, cb, o.add(0, 2, 0), 26, 0xFF4010, 0.25F * s * (float) rise);
				FxKit.halo(consumers, m, cb, o.add(0, 23, 0), 20 * hot + 4, 0xFFA040, 0.5F * hot);
			}
			double lift = Forge.lift(a);
			if (lift < 380) {
				Vec3d head = o.add(0, Forge.ANVIL_H + lift + Forge.HEAD_H, 0);
				box(solid, m, head, X, Y, Z, Forge.HEAD_W, Forge.HEAD_H, Forge.HEAD_L, HEAD, true);
				// Bands of glowing runes round the head, and the striking face's rim.
				for (int k = -1; k <= 1; k += 2) {
					box(glow, m, head.add(0, 0, k * Forge.HEAD_L * 0.55), X, Y, Z, Forge.HEAD_W + 0.06, Forge.HEAD_H + 0.06, 0.4, 0xFF5010, false);
				}
				box(solid, m, head.add(0, -Forge.HEAD_H - 0.6, 0), X, Y, Z, Forge.HEAD_W * 0.9, 0.6, Forge.HEAD_L * 0.9, IRON, true);
				Vec3d grip = head.add(0, Forge.HEAD_H, 0);
				Solids.tube(solid, m, grip, grip.add(0, Forge.HANDLE_L, 0), 2.0, 1.7, 10, HANDLE, 1.0F, true);
				for (int k = 1; k < 6; k++) {
					Vec3d band = grip.add(0, k * Forge.HANDLE_L / 6, 0);
					Solids.tube(solid, m, band, band.add(0, 1.2, 0), 2.2, 2.2, 10, 0x3A3A40, 1.0F, true);
				}
				box(solid, m, grip.add(0, Forge.HANDLE_L + 1.5, 0), X, Y, Z, 3, 1.5, 3, HEAD, true);
				// Speed lines while it falls.
				double fall = Forge.lift(a - 1) - lift;
				if (fall > 1.5 && fall < 100) {
					float k = (float) MathHelper.clamp(fall / 10.0, 0.0, 1.0);
					for (int i = -2; i <= 2; i++) {
						Vec3d x0 = head.add(i * Forge.HEAD_W * 0.45, Forge.HEAD_H, 0);
						FxDraw.ribbon(glow, m, x0, x0.add(0, fall * 3, 0), Vec3d.ZERO, 0.4, 0xFFD0A0, 0.35F * k, 0.0F);
					}
				}
			}
		}
	}

	// ====================================================================== BLUE INFRIT

	/** The summoning circle burning itself into the ground, and the hound's head of blue flame. */
	static final class InfritFx extends FxEffect {
		private static final int SOUL = 0x40C8FF, HOT = 0xC0F8FF;
		private final Vec3d c;
		private final int seed;
		private Vec3d facing = new Vec3d(0, 0, 1);

		InfritFx(Vec3d c, int seed, int life) {
			super(Math.max(20, life));
			this.c = c;
			this.seed = seed;
		}

		private double ground(double dx, double dz) {
			return NewFx.groundY(c.x + dx, c.z + dz, c.y) - c.y;
		}

		private Vec3d headAt(double t) {
			return Infrit.head(t, seed, (x, z) -> ground(x, z));
		}

		@Override
		public boolean tick(ClientWorld world) {
			boolean alive = super.tick(world);
			if (world != null && age >= Infrit.EMERGE && age < Infrit.BURST && age % 2 == 0) {
				Vec3d h = c.add(headAt(age));
				FxEngine.play(VfxIds.SOUL_TRAIL, h.x, h.y, h.z, 0, 1, 0, 1.3F, 0, seed * 31L + age);
			}
			return alive;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			circle(glow, consumers, m, cb, cam, a);
			if (a >= Infrit.EMERGE && a < Infrit.BURST) {
				hound(consumers.getBuffer(FxRenderLayers.SOLID), glow, consumers, m, cb, cam, a);
			}
		}

		private Vec3d onGround(Vec3d offset, Vec3d cam) {
			return new Vec3d(c.x + offset.x, c.y + ground(offset.x, offset.z) + 0.12, c.z + offset.z).subtract(cam);
		}

		private void circle(VertexConsumer glow, VertexConsumerProvider consumers, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			double drawn = Infrit.drawn(a);
			if (drawn <= 0) {
				return;
			}
			float fade = 1 - (float) MathHelper.clamp((a - Infrit.BURST) / 60.0, 0.0, 1.0);
			if (fade <= 0) {
				return;
			}
			float pulse = (a < Infrit.DRAWN ? 1.0F : 0.75F + 0.25F * MathHelper.sin(a * 0.2F)) * fade;
			int steps = 420;
			Vec3d prev = null;
			for (int i = 0; i <= steps; i++) {
				double u = drawn * i / steps;
				Vec3d p = onGround(Infrit.pen(u), cam);
				// Each stroke of the star starts fresh: don't join the end of one to the start of the next.
				if (prev != null && prev.distanceTo(p) < 8) {
					groundLine(glow, m, prev, p, 3.0, SOUL, 0.22F * pulse);
					groundLine(glow, m, prev, p, 1.0, HOT, 0.8F * pulse);
				}
				prev = p;
			}
			if (a < Infrit.DRAWN) {
				FxKit.halo(consumers, m, cb, onGround(Infrit.pen(drawn), cam).add(0, 1, 0), 4, SOUL, 0.8F);
			}
			// Runes between the rings once both are drawn.
			if (drawn > 0.56) {
				float k = (float) MathHelper.clamp((drawn - 0.56) * 6, 0.0, 1.0) * pulse;
				double mid = (Infrit.R + Infrit.R_IN) * 0.5;
				for (int i = 0; i < 14; i++) {
					double ang = i * MathHelper.TAU / 14;
					Vec3d radial = new Vec3d(Math.cos(ang), 0, Math.sin(ang)), tangent = new Vec3d(-Math.sin(ang), 0, Math.cos(ang));
					Solids.glyph(glow, m, onGround(radial.multiply(mid), cam), tangent, radial, 5, seed + i, HOT, 0.8F * k);
				}
			}
		}

		/** The head: skull, brow, snout, a jaw that drops open as it dives, tall ears, burning eyes, all wreathed in fire. */
		private void hound(VertexConsumer solid, VertexConsumer glow, VertexConsumerProvider consumers, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			Vec3d pos = c.add(headAt(a)).subtract(cam);
			Vec3d vel = headAt(a + 1).subtract(headAt(a));
			if (vel.lengthSquared() > 1.0E-4) {
				facing = facing.lerp(vel.normalize(), 0.35).normalize();
			}
			Vec3d f = facing;
			Vec3d r = f.crossProduct(Y);
			if (r.lengthSquared() < 1.0E-4) {
				r = X;
			}
			r = r.normalize();
			Vec3d u = r.crossProduct(f).normalize();
			double s = 1.4;
			int fur = 0x0E1838, dark = 0x080E24, mid = 0x16224A;
			FxKit.halo(consumers, m, cb, pos, 16 * s, 0x2A70FF, 0.35F);
			box(solid, m, at(pos, r, u, f, 0, 0, 0, s), r, u, f, 5 * s, 4.5 * s, 5 * s, fur, true);
			box(solid, m, at(pos, r, u, f, 0, 2.6, 4.6, s), r, u, f, 5.2 * s, 1.2 * s, 1.2 * s, mid, true);
			box(solid, m, at(pos, r, u, f, 0, -0.8, 8.5, s), r, u, f, 2.9 * s, 2.2 * s, 4 * s, fur, true);
			box(solid, m, at(pos, r, u, f, 0, 0.4, 12.6, s), r, u, f, 1.4 * s, 1.0 * s, 0.6 * s, 0x04060C, true);
			// The jaw swings down from a hinge under the skull.
			double open = Infrit.jaw(a) * 0.65;
			Vec3d jf = f.multiply(Math.cos(open)).subtract(u.multiply(Math.sin(open))).normalize();
			Vec3d ju = jf.crossProduct(r).multiply(-1).normalize();
			Vec3d hinge = at(pos, r, u, f, 0, -3.2, 2.5, s);
			box(solid, m, hinge.add(jf.multiply(5.5 * s)).add(ju.multiply(-0.9 * s)), r, ju, jf, 2.6 * s, 0.9 * s, 5 * s, dark, true);
			for (int k = 0; k < 4; k++) {
				double z = 4.5 + k * 1.8;
				for (int side = -1; side <= 1; side += 2) {
					box(glow, m, at(pos, r, u, f, side * 2.3, -3.2, z, s), r, u, f, 0.3 * s, 0.6 * s, 0.3 * s, HOT, false);
					box(glow, m, hinge.add(jf.multiply((z - 2.5) * s)).add(r.multiply(side * 2.2 * s)).add(ju.multiply(0.4 * s)),
							r, ju, jf, 0.3 * s, 0.5 * s, 0.3 * s, HOT, false);
				}
			}
			// The fire in its throat shows when it opens.
			if (open > 0.15) {
				FxKit.halo(consumers, m, cb, at(pos, r, u, f, 0, -2.4, 6, s), 5 * s * open, 0x80E8FF, 0.8F * (float) open);
			}
			for (int side = -1; side <= 1; side += 2) {
				Vec3d ear = at(pos, r, u, f, side * 3.3, 6.5, -1.6, s);
				Vec3d back = u.multiply(Math.cos(0.35)).subtract(f.multiply(Math.sin(0.35))).normalize();
				Vec3d earF = r.crossProduct(back).multiply(-1).normalize();
				box(solid, m, ear, r, back, earF, 1.3 * s, 3.2 * s, 1.0 * s, dark, true);
				box(glow, m, ear.add(back.multiply(3.3 * s)), r, back, earF, 0.6 * s, 0.3 * s, 0.5 * s, SOUL, false);
				Vec3d eye = at(pos, r, u, f, side * 2.6, 1.5, 5.1, s);
				box(glow, m, eye, r, u, f, 1.0 * s, 0.55 * s, 0.1 * s, HOT, false);
				FxKit.halo(consumers, m, cb, eye.add(f.multiply(0.3)), 2.4 * s, 0x80F0FF, 0.9F);
			}
		}

		/** A point in the head's own frame (x right, y up, z forward), scaled. */
		private static Vec3d at(Vec3d pos, Vec3d r, Vec3d u, Vec3d f, double x, double y, double z, double s) {
			return pos.add(r.multiply(x * s)).add(u.multiply(y * s)).add(f.multiply(z * s));
		}
	}
}
