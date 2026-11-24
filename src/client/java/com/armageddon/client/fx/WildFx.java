package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.SciShapes;
import com.armageddon.util.WildShapes.Cactus;
import com.armageddon.util.WildShapes.Forge;
import com.armageddon.util.WildShapes.Infrit;
import com.armageddon.util.WildShapes.Koi;
import com.armageddon.util.WonderShapes.Lantern;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The big models of the wild bombs: the spinning saguaro, built of exactly the blocks it sets into; the Blackforge's
 * hammer; and the Blue Infrit's summoning circle and its burning hound's head. Particles come from the engine
 * (WildVfx); timing and geometry from {@code WildShapes}, shared with the server.
 */
public final class WildFx {
	private WildFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 50 -> new CactusFx(a, p.duration());
			case 51 -> new ForgeFx(a, p.duration());
			case 52 -> new InfritFx(a, (int) p.p1(), p.duration());
			case 53 -> new KoiFx(a, p.duration());
			case 54 -> new LanternFx(a, p.duration());
			default -> kind >= 55 && kind <= 57 ? FossilFx.create(kind, a, b, p) : kind == 58 ? new XenoFx(a, b, p.scale(), p.duration()) : null;
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

	/**
	 * The saguaro as blocks: the very blocks the server sets when it stops, spun about the centre of the block it
	 * grows from, with the spikes that have not flown yet standing out of it.
	 */
	static final class CactusFx extends FxEffect {
		/** Moss and green terracotta, as they look in the world. */
		private static final int BODY = 0x617A32, RIB = 0x4C532A, SPIKE = 0xE8DCA8;
		private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		/** Exposed faces: centre x, y, z (relative to the pivot), direction index, colour. */
		private static float[][] faces;
		private final Vec3d pivot;
		private final double[] dir = new double[3];

		CactusFx(Vec3d base, int life) {
			super(Math.max(20, life));
			BlockPos b = BlockPos.ofFloored(base);
			this.pivot = new Vec3d(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
		}

		private static float[][] faces() {
			if (faces == null) {
				List<int[]> vox = Cactus.voxels();
				Set<Long> filled = new HashSet<>();
				for (int[] v : vox) {
					filled.add(BlockPos.asLong(v[0], v[1], v[2]));
				}
				List<float[]> out = new ArrayList<>();
				for (int[] v : vox) {
					// A little variation from block to block, like a real texture.
					double k = 0.92 + 0.16 * SciShapes.hash(v[0] * 31 + v[2], v[1]);
					int col = Solids.scale(v[3] == 1 ? RIB : BODY, k);
					for (int d = 0; d < 6; d++) {
						int[] n = DIRS[d];
						if (!filled.contains(BlockPos.asLong(v[0] + n[0], v[1] + n[1], v[2] + n[2]))) {
							// Measured from the pivot (the base block's centre at its foot), block v's centre is at v, half a block up.
							out.add(new float[]{v[0] + 0.5F * n[0], v[1] + 0.5F + 0.5F * n[1], v[2] + 0.5F * n[2], d, col});
						}
					}
				}
				faces = out.toArray(new float[0][]);
			}
			return faces;
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
			Vec3d o = pivot.subtract(cam).add(0, -sink, 0);
			double turn = Cactus.spin(a);
			double cs = Math.cos(turn), sn = Math.sin(turn);
			for (float[] f : faces()) {
				int d = (int) f[3];
				int[] n = DIRS[d];
				// Face centres are already relative to the pivot (the centre of the base block).
				double fx = f[0], fy = f[1], fz = f[2];
				Vec3d c = new Vec3d(o.x + fx * cs - fz * sn, o.y + fy, o.z + fx * sn + fz * cs);
				Vec3d nn = new Vec3d(n[0] * cs - n[2] * sn, n[1], n[0] * sn + n[2] * cs);
				// Two half-edges in the face's plane.
				Vec3d u = n[1] != 0 ? new Vec3d(cs, 0, sn) : new Vec3d(0, 1, 0);
				Vec3d v = nn.crossProduct(u);
				u = u.multiply(0.5);
				v = v.multiply(0.5);
				// Shaded the way the world shades blocks: top full, bottom half, sides in between.
				double shade = nn.y > 0.5 ? 1.0 : nn.y < -0.5 ? 0.5 : 0.7 + 0.15 * (nn.x * 0.6 + nn.z * 0.8);
				int col = Solids.scale((int) f[4], shade);
				Solids.quad(solid, m, c.subtract(u).subtract(v), c.add(u).subtract(v), c.add(u).add(v), c.subtract(u).add(v), col, 1.0F);
			}
			for (int i = 0; i < Cactus.SPIKES; i++) {
				if (Cactus.fireTick(i) <= a) {
					continue;
				}
				Vec3d root = Cactus.turn(Cactus.spike(i, dir), turn);
				Vec3d dd = Cactus.turn(new Vec3d(dir[0], dir[1], dir[2]), turn);
				Solids.tube(solid, m, o.add(root), o.add(root).add(dd.multiply(2.4)), 0.2, 0.02, 4, SPIKE, 1.0F, true);
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
	}

	// ====================================================================== BLACKFORGE

	/** The hammer the size of a tower, out of the dark, and the hot scar it leaves where it lands. */
	static final class ForgeFx extends FxEffect {
		private static final int HEAD = 0x2A2A32, IRON = 0x1C171B, HANDLE = 0x2A1C12;
		private final Vec3d c;

		ForgeFx(Vec3d c, int life) {
			super(Math.max(20, life));
			this.c = c;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d o = c.subtract(cam);
			if (a >= Forge.STRIKE) {
				float hot = (float) Math.exp(-(a - Forge.STRIKE) / 45.0);
				FxDraw.ring(glow, m, o.add(0, 0.2, 0), X, Z, 0, 18, 0xFF8030, 0.9F * hot, 0.5F * hot, 0.0F, 32);
				FxKit.halo(consumers, m, cb, o.add(0, 3, 0), 28 * hot + 6, 0xFFA040, 0.5F * hot);
			}
			double lift = Forge.lift(a);
			if (lift >= 380) {
				return;
			}
			Vec3d head = o.add(0, lift + Forge.HEAD_H, 0);
			box(solid, m, head, X, Y, Z, Forge.HEAD_W, Forge.HEAD_H, Forge.HEAD_L, HEAD, true);
			// Bands of glowing runes round the head, and a heavy striking face.
			for (int k = -1; k <= 1; k++) {
				box(glow, m, head.add(0, 0, k * Forge.HEAD_L * 0.6), X, Y, Z, Forge.HEAD_W + 0.06, Forge.HEAD_H + 0.06, 0.5, 0xFF5010, false);
			}
			box(solid, m, head.add(0, -Forge.HEAD_H - 0.8, 0), X, Y, Z, Forge.HEAD_W * 0.9, 0.8, Forge.HEAD_L * 0.9, IRON, true);
			Vec3d grip = head.add(0, Forge.HEAD_H, 0);
			Solids.tube(solid, m, grip, grip.add(0, Forge.HANDLE_L, 0), 2.6, 2.2, 10, HANDLE, 1.0F, true);
			for (int k = 1; k < 7; k++) {
				Vec3d band = grip.add(0, k * Forge.HANDLE_L / 7, 0);
				Solids.tube(solid, m, band, band.add(0, 1.4, 0), 2.9, 2.9, 10, 0x3A3A40, 1.0F, true);
			}
			box(solid, m, grip.add(0, Forge.HANDLE_L + 2, 0), X, Y, Z, 4, 2, 4, HEAD, true);
			FxKit.halo(consumers, m, cb, head, 22, 0xFF5010, 0.18F);
			// Speed lines while it falls.
			double fall = Forge.lift(a - 1) - lift;
			if (fall > 1.5 && fall < 100) {
				float k = (float) MathHelper.clamp(fall / 10.0, 0.0, 1.0);
				for (int i = -2; i <= 2; i++) {
					Vec3d x0 = head.add(i * Forge.HEAD_W * 0.45, Forge.HEAD_H, 0);
					FxDraw.ribbon(glow, m, x0, x0.add(0, fall * 3, 0), Vec3d.ZERO, 0.5, 0xFFD0A0, 0.35F * k, 0.0F);
				}
			}
		}
	}

	// ====================================================================== BLUE INFRIT

	/**
	 * The summoning circle burning itself into the ground, and the hound's head: a skull of night-blue wreathed in
	 * blue fire, flames licking off every surface, two rings of fire wheeling round it, a mane streaming behind,
	 * sparks swirling, jets of fire from its eyes and, when its jaw drops, a torrent of it from its throat.
	 */
	static final class InfritFx extends FxEffect {
		private static final int SOUL = 0x40C8FF, HOT = 0xC0F8FF;
		private static final double S = 1.4;
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

		private static Vec3d basisUp(Vec3d f, Vec3d r) {
			return r.crossProduct(f).normalize();
		}

		private static Vec3d basisRight(Vec3d f) {
			Vec3d r = f.crossProduct(Y);
			return r.lengthSquared() < 1.0E-4 ? X : r.normalize();
		}

		@Override
		public boolean tick(ClientWorld world) {
			boolean alive = super.tick(world);
			if (age < Infrit.EMERGE || age >= Infrit.BURST) {
				return alive;
			}
			Vec3d h = c.add(headAt(age));
			Vec3d v = headAt(age + 1).subtract(headAt(age));
			Vec3d f = v.lengthSquared() > 1.0E-4 ? v.normalize() : facing;
			Vec3d r = basisRight(f), u = basisUp(f, r);
			long s = seed * 31L + age;
			float vx = (float) v.x, vy = (float) v.y, vz = (float) v.z;
			// The mane streams back off it, sparks wheel round it, and it leaves a wake of fire.
			FxEngine.playMoving(VfxIds.INFRIT_MANE, h.x, h.y, h.z, (float) -f.x, (float) -f.y, (float) -f.z, (float) S, 0, s, vx, vy, vz);
			FxEngine.playMoving(VfxIds.INFRIT_SPARKS, h.x, h.y, h.z, 0, 1, 0, (float) S, 0, s + 1, vx, vy, vz);
			if (age % 2 == 0) {
				FxEngine.play(VfxIds.SOUL_TRAIL, h.x, h.y, h.z, 0, 1, 0, (float) S, 0, s + 2);
			}
			if (Infrit.jaw(age) > 0.45) {
				Vec3d mouth = h.add(f.multiply(12 * S)).add(u.multiply(-2.2 * S));
				FxEngine.playMoving(VfxIds.INFRIT_BREATH, mouth.x, mouth.y, mouth.z, (float) f.x, (float) f.y, (float) f.z, (float) S, 0, s + 3, vx, vy, vz);
			}
			if (world != null) {
				ClientFx.light(h, 0.9F, 0x40B0FF);
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
				hound(consumers.getBuffer(FxRenderLayers.SOLID), glow, consumers.getBuffer(FxSprites.GLOW_LAYER), consumers, m, cb, cam, a);
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

		private void hound(VertexConsumer solid, VertexConsumer glow, VertexConsumer flames, VertexConsumerProvider consumers, Matrix4f m,
				Vec3d[] cb, Vec3d cam, float a) {
			Vec3d pos = c.add(headAt(a)).subtract(cam);
			Vec3d vel = headAt(a + 1).subtract(headAt(a));
			if (vel.lengthSquared() > 1.0E-4) {
				facing = facing.lerp(vel.normalize(), 0.35).normalize();
			}
			Vec3d f = facing, r = basisRight(f), u = basisUp(f, r);
			double s = S;
			int fur = 0x0E1838, dark = 0x080E24, mid = 0x16224A;
			FxKit.halo(consumers, m, cb, pos, 20 * s, 0x2A70FF, 0.4F + 0.1F * MathHelper.sin(a * 0.7F));
			FxKit.halo(consumers, m, cb, pos, 9 * s, 0x90E8FF, 0.35F);
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
			if (open > 0.15) {
				FxKit.halo(consumers, m, cb, at(pos, r, u, f, 0, -2.4, 6, s), 6 * s * open, 0x80E8FF, 0.9F * (float) open);
			}
			for (int side = -1; side <= 1; side += 2) {
				Vec3d ear = at(pos, r, u, f, side * 3.3, 6.5, -1.6, s);
				Vec3d back = u.multiply(Math.cos(0.35)).subtract(f.multiply(Math.sin(0.35))).normalize();
				Vec3d earF = r.crossProduct(back).multiply(-1).normalize();
				box(solid, m, ear, r, back, earF, 1.3 * s, 3.2 * s, 1.0 * s, dark, true);
				box(glow, m, ear.add(back.multiply(3.3 * s)), r, back, earF, 0.6 * s, 0.3 * s, 0.5 * s, SOUL, false);
				Vec3d eye = at(pos, r, u, f, side * 2.6, 1.5, 5.1, s);
				box(glow, m, eye, r, u, f, 1.0 * s, 0.55 * s, 0.1 * s, HOT, false);
				FxKit.halo(consumers, m, cb, eye.add(f.multiply(0.3)), 2.8 * s, 0x80F0FF, 1.0F);
				// Flames streaming back from the eyes, flickering.
				for (int k = 0; k < 3; k++) {
					double flick = 0.6 + 0.4 * MathHelper.sin(a * 1.3F + k * 2 + side);
					Vec3d tail = eye.add(f.multiply(-(5 + k * 2.5) * s * flick)).add(u.multiply((1.2 + k * 0.6) * s)).add(r.multiply(side * k * 0.6 * s));
					FxDraw.ribbon(glow, m, eye, tail, Vec3d.ZERO, (0.9 - k * 0.2) * s, k == 0 ? HOT : SOUL, 0.85F, 0.0F);
				}
			}
			skin(flames, m, cb, pos, r, u, f, a);
			rings(flames, glow, m, cb, pos, r, u, f, a);
		}

		/** Flames licking off every surface of the head, each rising and fading on its own clock. */
		private void skin(VertexConsumer flames, Matrix4f m, Vec3d[] cb, Vec3d pos, Vec3d r, Vec3d u, Vec3d f, float a) {
			for (int k = 0; k < 56; k++) {
				double hx = SciShapes.hash(k, 1) * 2 - 1, hy = SciShapes.hash(k, 2) * 2 - 1, hz = SciShapes.hash(k, 3) * 2 - 1;
				// Snap to the surface of the skull or the snout.
				boolean snout = k % 3 == 0;
				double ex = snout ? 3.0 : 5.2, ey = snout ? 2.3 : 4.7, ez = snout ? 4.2 : 5.2, oz = snout ? 8.5 : 0;
				double ax = Math.abs(hx), ay = Math.abs(hy), az = Math.abs(hz);
				if (ax >= ay && ax >= az) {
					hx = Math.signum(hx);
				} else if (ay >= az) {
					hy = Math.signum(hy);
				} else {
					hz = Math.signum(hz);
				}
				double phase = (a * (0.07 + 0.05 * SciShapes.hash(k, 4)) + SciShapes.hash(k, 5)) % 1.0;
				Vec3d p = at(pos, r, u, f, hx * ex, hy * ey + phase * 3.5, hz * ez + oz - phase * 2.5, S);
				double size = (1.0 + 1.4 * SciShapes.hash(k, 6)) * (1 - phase * 0.55) * S;
				float alpha = (float) Math.sin(phase * Math.PI) * 0.85F;
				int col = Solids.mix(0xD0F8FF, 0x2A70FF, phase);
				FxSprites.billboard(flames, m, p, cb, size, a * 0.05 + k, FxSprites.FIRE + (k & 1), col, alpha, FxSprites.FULL_BRIGHT);
			}
		}

		/** Two tilted rings of fire wheeling round the head in opposite directions, with burning tails. */
		private void rings(VertexConsumer flames, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d pos, Vec3d r, Vec3d u, Vec3d f, float a) {
			for (int ring = 0; ring < 2; ring++) {
				double tilt = ring == 0 ? 0.55 : -0.75;
				Vec3d e1 = ring == 0 ? r : f;
				Vec3d e2 = ring == 0 ? f.multiply(Math.cos(tilt)).add(u.multiply(Math.sin(tilt))) : u.multiply(Math.cos(tilt)).add(r.multiply(Math.sin(tilt)));
				double rad = (ring == 0 ? 13 : 11) * S, spin = (ring == 0 ? 0.22 : -0.3) * a;
				int n = ring == 0 ? 10 : 8;
				for (int k = 0; k < n; k++) {
					double th = spin + k * MathHelper.TAU / n;
					Vec3d p = pos.add(e1.multiply(Math.cos(th) * rad)).add(e2.multiply(Math.sin(th) * rad));
					FxSprites.billboard(flames, m, p, cb, 1.8 * S, a * 0.1 + k, FxSprites.FIRE + (k & 1), 0x60D0FF, 0.9F, FxSprites.FULL_BRIGHT);
					FxSprites.billboard(flames, m, p, cb, 0.8 * S, 0, FxSprites.GLOW, 0xE0FFFF, 0.9F, FxSprites.FULL_BRIGHT);
					Vec3d prev = p;
					for (int j = 1; j <= 6; j++) {
						double tj = th - (ring == 0 ? 1 : -1) * j * 0.07;
						Vec3d q = pos.add(e1.multiply(Math.cos(tj) * rad)).add(e2.multiply(Math.sin(tj) * rad));
						float fa = 0.6F * (1 - j / 7.0F);
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 1.1 * S * (1 - j / 8.0), SOUL, fa, fa * 0.8F);
						prev = q;
					}
				}
			}
		}

		/** A point in the head's own frame (x right, y up, z forward), scaled. */
		private static Vec3d at(Vec3d pos, Vec3d r, Vec3d u, Vec3d f, double x, double y, double z, double s) {
			return pos.add(r.multiply(x * s)).add(u.multiply(y * s)).add(f.multiply(z * s));
		}
	}

	// ====================================================================== PLASMA KOI

	/**
	 * The pond of liquid light and its koi: each fish a tapered ribbon of plasma with a white-hot core, fins and a tail
	 * fan, dimmer under the surface; leaping on the server's arcs; braided into a dragon at the end. The particles
	 * (ripples, drips, trails) come from the engine as they move.
	 */
	static final class KoiFx extends FxEffect {
		private final Vec3d c;
		private final double[] landDy = new double[Koi.LEAPS];
		private final boolean[] known = new boolean[Koi.LEAPS];

		KoiFx(Vec3d c, int life) {
			super(Math.max(40, life));
			this.c = c;
		}

		private static double ground(ClientWorld w, double x, double z, double fallback) {
			if (w == null) {
				return fallback;
			}
			int y = w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
			return y > w.getBottomY() + 1 ? y : fallback;
		}

		/** Koi {@code k} relative to the pond at time {@code t}, or null. */
		private Vec3d pos(int k, double t) {
			if (t < Koi.OPEN || t >= Koi.GEYSER) {
				return null;
			}
			if (t >= Koi.GATHER) {
				return Koi.dragon(k, t);
			}
			int j = Koi.leapOf(k, t);
			if (j >= 0) {
				return Koi.leap(j, t, landDy[j]);
			}
			return Koi.resting(k, t) ? null : Koi.swim(k, t);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			for (int j = 0; j < Koi.LEAPS; j++) {
				if (!known[j] && t >= Koi.leapStart(j) - 4 && world != null) {
					Vec3d l = c.add(Koi.landing(j));
					landDy[j] = ground(world, l.x, l.z, c.y) - c.y;
					known[j] = true;
				}
			}
			if (t >= Koi.OPEN && t < Koi.GEYSER && t % 6 == 0) {
				FxEngine.play(VfxIds.KOI_SHIMMER, c.x, c.y, c.z, 0, 1, 0, 1.0F, 0, t * 131L);
			}
			for (int k = 0; k < Koi.KOI; k++) {
				Vec3d p = pos(k, t), q = pos(k, t - 1);
				if (p == null) {
					continue;
				}
				Vec3d w = c.add(p);
				int col = Koi.COLORS[k];
				boolean flying = t >= Koi.GATHER || Koi.leapOf(k, t) >= 0;
				if (flying && q != null) {
					Vec3d v = p.subtract(q);
					FxEngine.playMoving(VfxIds.KOI_TRAIL, w.x, w.y, w.z, 0, 1, 0, t >= Koi.GATHER ? 1.3F : 1.0F, col, t * 977L + k,
							(float) v.x, (float) v.y, (float) v.z);
				} else if (t % 8 == k) {
					FxEngine.play(VfxIds.KOI_RIPPLE, w.x, c.y + 0.05, w.z, 0, 1, 0, 1.0F, col, t * 389L + k);
				}
			}
			if (t % 10 == 0 && t < Koi.GEYSER + 30) {
				ClientFx.light(c.add(0, 2, 0), 0.6F, 0x60F0FF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d o = c.subtract(cam);
			// The pond: a disc of liquid light with caustic ripples running out over it.
			float open = (float) MathHelper.clamp(a / Koi.OPEN, 0.0, 1.0);
			float fade = a < Koi.GEYSER ? 1.0F : (float) Math.max(0, 1 - (a - Koi.GEYSER) / 25.0);
			if (fade > 0) {
				double r = Koi.POND_R * (1 - (1 - open) * (1 - open));
				float pulse = 0.85F + 0.15F * MathHelper.sin(a * 0.11F);
				FxDraw.disc(glow, m, o.add(0, 0.02, 0), X, Z, r, 0x50E0FF, 0.55F * fade * pulse, 0.3F * fade, 48);
				FxDraw.disc(glow, m, o.add(0, 0.04, 0), X, Z, r * 0.55, 0xC0FFFF, 0.25F * fade * pulse, 0.0F, 32);
				for (int i = 0; i < 4; i++) {
					double rr = (a * 0.12 + i * r / 4) % r;
					FxDraw.ring(glow, m, o.add(0, 0.06, 0), X, Z, Math.max(0, rr - 0.25), rr + 0.25, 0xE0FFFF, 0.0F,
							0.35F * (float) (1 - rr / r) * fade, 0.0F, 48);
				}
			}
			boolean dragon = a >= Koi.GATHER;
			int n = dragon ? 34 : 12;
			// The body is sampled by distance, not time, so a fish stays fish-length however fast it is moving.
			double seg = dragon ? 0.8 : 0.45;
			Vec3d[] pts = new Vec3d[n];
			for (int k = 0; k < Koi.KOI; k++) {
				int col = Koi.COLORS[k];
				Vec3d head = pos(k, a);
				if (head == null) {
					continue;
				}
				int len = 0;
				pts[len++] = o.add(head);
				double tt = a;
				Vec3d last = head;
				for (int guard = 0; len < n && guard < 400; guard++) {
					tt -= 0.05;
					Vec3d p = pos(k, tt);
					if (p == null) {
						break;
					}
					if (p.squaredDistanceTo(last) >= seg * seg) {
						pts[len++] = o.add(p);
						last = p;
					}
				}
				if (len < 3) {
					continue;
				}
				float alpha = pts[0].y - o.y < -0.25 ? 0.6F : 1.0F;
				double thick = dragon ? 2.2 : 1.4;
				for (int s = 0; s < len - 1; s++) {
					double u = (s + 1) / (double) (len + 1);
					double w = (0.25 + 0.95 * Math.pow(Math.sin(Math.PI * Math.min(1, u * 1.4)), 0.7)) * thick;
					float al = alpha * (float) (1 - 0.4 * s / len);
					FxDraw.ribbon(glow, m, pts[s], pts[s + 1], Vec3d.ZERO, w, col, al, al * 0.9F);
					FxDraw.ribbon(glow, m, pts[s], pts[s + 1], Vec3d.ZERO, w * 0.2, 0xFFFFFF, al * 0.6F, al * 0.5F);
				}
				// Pectoral fins and the tail fan.
				Vec3d d = pts[0].subtract(pts[2]);
				if (d.lengthSquared() > 1.0E-4) {
					d = d.normalize();
					Vec3d side = d.crossProduct(new Vec3d(0, 1, 0));
					side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
					float flap = 0.8F + 0.4F * MathHelper.sin(a * 0.6F + k);
					for (int sgn = -1; sgn <= 1; sgn += 2) {
						Vec3d root = pts[1];
						FxDraw.ribbon(glow, m, root, root.add(side.multiply(sgn * 1.3 * flap * thick)).subtract(d.multiply(0.7)), Vec3d.ZERO, 0.5 * thick, col,
								0.6F * alpha, 0.1F * alpha);
					}
					Vec3d tail = pts[len - 1], back = tail.subtract(pts[len - 2]);
					if (back.lengthSquared() > 1.0E-4) {
						FxDraw.ribbon(glow, m, tail, tail.add(back.normalize().multiply(1.6 * thick)), Vec3d.ZERO, 1.5 * thick, col, 0.5F * alpha, 0.0F);
					}
				}
				FxKit.halo(consumers, m, cb, pts[0], 1.6 * thick, col, 0.45F * alpha);
			}
			if (dragon && a < Koi.GEYSER) {
				// The dragon's body: a glowing spine the braid winds round, thick at the shoulders, thin at the tail.
				Vec3d prevS = null;
				for (int s = 0; s < 40; s++) {
					double ts = a - s * 0.9;
					if (ts < Koi.GATHER) {
						break;
					}
					Vec3d sp = o.add(Koi.spine(ts));
					if (prevS != null) {
						double w = 4.5 * (1 - s / 44.0);
						FxDraw.ribbon(glow, m, prevS, sp, Vec3d.ZERO, w, 0x40C8FF, 0.32F, 0.3F);
					}
					prevS = sp;
				}
				// The dragon's head leading the braid: a white-hot glow and two whiskers trailing back.
				Vec3d head = o.add(Koi.spine(a)), prev = o.add(Koi.spine(a - 1.5));
				FxKit.halo(consumers, m, cb, head, 8, 0x80E8FF, 0.6F);
				Vec3d d = head.subtract(prev);
				if (d.lengthSquared() > 1.0E-4) {
					d = d.normalize();
					Vec3d side = d.crossProduct(new Vec3d(0, 1, 0));
					side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
					for (int sgn = -1; sgn <= 1; sgn += 2) {
						Vec3d tip = head.add(side.multiply(sgn * 3)).subtract(d.multiply(5)).add(0, MathHelper.sin(a * 0.3F + sgn) * 1.5, 0);
						FxDraw.ribbon(glow, m, head, tip, Vec3d.ZERO, 0.35, 0xE0FFFF, 0.8F, 0.0F);
					}
				}
			}
		}
	}

	// ====================================================================== CHINESE LANTERN

	/**
	 * The golden dragon over the lanterns: a long translucent serpent of gold with a pearly core, glints running along
	 * its scales, four legs with claws, horns, whiskers that wave, and a glowing head with a pearl. The lanterns and
	 * the glitter are the engine's.
	 */
	static final class LanternFx extends FxEffect {
		private static final int N = 60;
		private static final double SEG = 0.8;
		private final Vec3d c;

		LanternFx(Vec3d c, int life) {
			super(Math.max(40, life));
			this.c = c;
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t < Lantern.RISE && t % 2 == 0) {
				FxEngine.play(VfxIds.LANTERN_RISE, c.x, c.y, c.z, 0, 1, 0, 1.0F, 0, t * 7919L);
			}
			if (t < Lantern.BURST) {
				Vec3d h = c.add(Lantern.head(t)), v = h.subtract(c.add(Lantern.head(t - 1)));
				FxEngine.playMoving(VfxIds.LANTERN_SPARKLE, h.x, h.y, h.z, 0, 1, 0, 1.0F, 0, t * 131L, (float) v.x, (float) v.y, (float) v.z);
				if (t % 3 == 0) {
					Vec3d b = c.add(Lantern.head(t - 22));
					FxEngine.play(VfxIds.LANTERN_SPARKLE, b.x, b.y, b.z, 0, 1, 0, 1.4F, 0, t * 977L);
				}
				if (t % 10 == 0) {
					ClientFx.light(h, 0.5F, 0xFFC040);
				}
			}
			if (t == Lantern.BURST) {
				Vec3d h = c.add(Lantern.head(t));
				FxEngine.play(VfxIds.LANTERN_BURST, h.x, h.y, h.z, 0, 1, 0, 1.0F, 0, 4242L);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a >= Lantern.BURST) {
				return;
			}
			float in = (float) MathHelper.clamp(a / 40.0, 0.0, 1.0);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d o = c.subtract(cam);
			// The body, sampled by distance back along its flight.
			Vec3d[] pts = new Vec3d[N];
			Vec3d head = Lantern.head(a);
			pts[0] = o.add(head);
			int len = 1;
			double tt = a;
			Vec3d last = head;
			for (int guard = 0; len < N && guard < 2000; guard++) {
				tt -= 0.1;
				Vec3d p = Lantern.head(tt);
				if (p.squaredDistanceTo(last) >= SEG * SEG) {
					pts[len++] = o.add(p);
					last = p;
				}
			}
			for (int s = 0; s < len - 1; s++) {
				double u = s / (double) N;
				double w = (s < 4 ? 1.6 + s * 0.25 : 2.6 * (1 - Math.pow(u, 1.6)) + 0.35);
				float al = 0.36F * in;
				// Translucent gold glass over a glowing core.
				FxDraw.ribbon(shade, m, pts[s], pts[s + 1], Vec3d.ZERO, w, 0xE0A020, al * 1.4F, al * 1.4F);
				FxDraw.ribbon(glow, m, pts[s], pts[s + 1], Vec3d.ZERO, w * 0.3, 0xFFD870, al * 0.8F, al * 0.8F);
				if (s % 3 == 0) {
					// Scales catching the light, a glint running down the body.
					float glint = 0.5F + 0.5F * MathHelper.sin(a * 0.25F - s * 0.6F);
					FxKit.halo(consumers, m, cb, pts[s], 0.9 + glint * 0.5, 0xFFC040, 0.3F * glint * in);
				}
			}
			// Legs with claws, hanging below the shoulders and hips.
			for (int s : new int[]{8, 14, 34, 40}) {
				if (s + 1 >= len) {
					continue;
				}
				Vec3d d = pts[s].subtract(pts[s + 1]);
				if (d.lengthSquared() < 1.0E-4) {
					continue;
				}
				d = d.normalize();
				Vec3d side = d.crossProduct(new Vec3d(0, 1, 0));
				side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					float step = MathHelper.sin(a * 0.2F + s + sgn) * 0.6F;
					Vec3d knee = pts[s].add(side.multiply(sgn * 1.4)).add(0, -1.4, 0).add(d.multiply(step));
					Vec3d foot = knee.add(0, -1.3, 0).subtract(d.multiply(0.6 + step));
					FxDraw.ribbon(shade, m, pts[s], knee, Vec3d.ZERO, 0.45, 0xE0A020, 0.55F * in, 0.55F * in);
					FxDraw.ribbon(shade, m, knee, foot, Vec3d.ZERO, 0.35, 0xF0B830, 0.55F * in, 0.45F * in);
					for (int cl = -1; cl <= 1; cl++) {
						FxDraw.ribbon(glow, m, foot, foot.add(d.multiply(0.6)).add(side.multiply(cl * 0.3)).add(0, -0.2, 0), Vec3d.ZERO, 0.12, 0xFFF0C0,
								0.6F * in, 0.1F * in);
					}
				}
			}
			// The head: horns sweeping back, whiskers waving, a pearl it chases just ahead of its jaws.
			Vec3d d = pts[0].subtract(pts[Math.min(2, len - 1)]);
			if (d.lengthSquared() > 1.0E-4) {
				d = d.normalize();
				Vec3d side = d.crossProduct(new Vec3d(0, 1, 0));
				side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
				Vec3d up = side.crossProduct(d).normalize();
				FxKit.halo(consumers, m, cb, pts[0], 3.4, 0xFFA020, 0.35F * in);
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d hornTip = pts[0].add(up.multiply(2.2)).add(side.multiply(sgn * 0.9)).subtract(d.multiply(2.6));
					FxDraw.ribbon(glow, m, pts[0].add(up.multiply(0.6)), hornTip, Vec3d.ZERO, 0.35, 0xFFE8A0, 0.6F * in, 0.1F * in);
					Vec3d prev = pts[0].add(d.multiply(0.8)).add(side.multiply(sgn * 0.5));
					for (int k = 1; k <= 6; k++) {
						double wave = MathHelper.sin(a * 0.3F + k * 0.9F + sgn) * 0.5;
						Vec3d nxt = pts[0].add(d.multiply(0.8 - k * 1.0)).add(side.multiply(sgn * (0.5 + k * 0.6))).add(up.multiply(wave - k * 0.15));
						FxDraw.ribbon(glow, m, prev, nxt, Vec3d.ZERO, 0.14, 0xFFF0C0, 0.6F * in * (1 - k / 7.0F), 0.6F * in * (1 - (k + 1) / 7.0F));
						prev = nxt;
					}
				}
				float pearl = 0.8F + 0.2F * MathHelper.sin(a * 0.4F);
				FxKit.halo(consumers, m, cb, pts[0].add(d.multiply(3.2)), 1.6 * pearl, 0xFFFFF0, 0.7F * in);
			}
		}
	}
}
