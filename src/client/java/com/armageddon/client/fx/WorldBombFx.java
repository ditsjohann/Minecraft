package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.PteroPose.Seg;
import com.armageddon.util.WorldBombShapes.Creeper;
import com.armageddon.util.WorldBombShapes.Wyrm;
import com.armageddon.util.WyrmPose;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** The two world bombs' giants: the Mega Creeper and the Elder Wyrm. */
final class WorldBombFx {
	private WorldBombFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 70 -> new CreeperFx(a, b, p.duration());
			case 71 -> new WyrmFx(a, b, p.duration());
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

	private static Vec3d camera(Vec3d fallback) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc == null || mc.gameRenderer == null ? fallback : mc.gameRenderer.getCamera().getPos();
	}

	// ====================================================================== MEGA CREEPER

	/**
	 * The creeper: a box model the height of a mountain, every pixel of its mottled green skin a quad, its face black.
	 * It rises out of the ground with its legs paddling, turns its head to stare at whoever is watching, and in its
	 * fuse swells and flashes white as a real one does, faster and faster.
	 */
	static final class CreeperFx extends FxEffect {
		private static final int[] GREENS = {0x0F7A12, 0x1D9A1F, 0x2DB52E, 0x4CCB4C, 0x77DD72, 0x157F14, 0x3FAF3A, 0x0B5E0D};
		private final Vec3d c, h, r;

		CreeperFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
			this.r = new Vec3d(-h.z, 0, h.x);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t >= Creeper.RISE && t < Creeper.BOOM && t % 8 == 0) {
				ClientFx.light(c.add(0, 70, 0), 0.3F + 0.8F * (float) Creeper.fuse(t), Creeper.flash(t) ? 0xF0FFF0 : 0x60E060);
			}
			return super.tick(world);
		}

		/** One box of the model: {@code x0..}, sizes in pixels, in the creeper's own axes (x right, y up, z forward). */
		private void box(VertexConsumer vc, Matrix4f m, Vec3d o, Vec3d ax, Vec3d ay, Vec3d az, int x0, int y0, int z0, int w, int hgt, int d, int part,
				boolean head, float white) {
			// Each face: its corner, the two directions across it, how many pixels each way, and how bright it is lit.
			Vec3d p000 = o.add(ax.multiply(x0)).add(ay.multiply(y0)).add(az.multiply(z0));
			Object[][] faces = {
					{p000.add(az.multiply(d)).add(ay.multiply(hgt)), ax, ay.multiply(-1), w, hgt, 0.95, 0},
					{p000.add(ax.multiply(w)).add(ay.multiply(hgt)), ax.multiply(-1), ay.multiply(-1), w, hgt, 0.62, 1},
					{p000.add(ax.multiply(w)).add(az.multiply(d)).add(ay.multiply(hgt)), az.multiply(-1), ay.multiply(-1), d, hgt, 0.78, 2},
					{p000.add(ay.multiply(hgt)), az, ay.multiply(-1), d, hgt, 0.72, 3},
					{p000.add(ay.multiply(hgt)), ax, az, w, d, 1.0, 4},
					{p000, ax, az, w, d, 0.45, 5}};
			for (Object[] f : faces) {
				Vec3d corner = (Vec3d) f[0], da = (Vec3d) f[1], db = (Vec3d) f[2];
				int na = (int) f[3], nb = (int) f[4], face = (int) f[6];
				double light = (double) f[5];
				for (int i = 0; i < na; i++) {
					for (int j = 0; j < nb; j++) {
						int col;
						if (head && face == 0 && Creeper.face(i, j)) {
							col = Effects.hash(i * 3 + j, 41) < 0.5 ? 0x111611 : 0x1E2A1E;
						} else {
							double hsh = Effects.hash(part * 131 + face * 37 + i * 7, j * 11 + part);
							col = GREENS[(int) (hsh * GREENS.length)];
						}
						col = Solids.scale(col, light);
						if (white > 0) {
							col = Solids.mix(col, 0xFFFFFF, white);
						}
						Vec3d a = corner.add(da.multiply(i)).add(db.multiply(j));
						Solids.quad(vc, m, a, a.add(da), a.add(da).add(db), a.add(db), col, 1.0F);
					}
				}
			}
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a < Creeper.RISE || a >= Creeper.BOOM) {
				return;
			}
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			double f = Creeper.fuse(a);
			// Swelling as a creeper does before it goes, a shiver in it.
			double wob = 1 + Math.sin(f * 100) * f * 0.012;
			double sxz = (1 + f * 0.35) * wob, sy = (1 + f * 0.1) / wob;
			double px = Creeper.PX;
			Vec3d ax = r.multiply(px * sxz), ay = new Vec3d(0, px * sy, 0), az = h.multiply(px * sxz);
			double sink = (1 - Creeper.risen(a)) * 27 * px;
			Vec3d o = c.add(0, -sink, 0).subtract(cam);
			float white = Creeper.flash(a) ? (float) (0.55 + 0.35 * f) : 0;
			// Legs: paddling as it climbs out, still once it stands.
			double swing = a < Creeper.RISEN ? Math.sin(a * 0.25) * 2.0 : 0;
			int[][] legs = {{-4, 2, 1}, {0, 2, -1}, {-4, -6, -1}, {0, -6, 1}};
			for (int i = 0; i < legs.length; i++) {
				int zoff = (int) Math.round(swing * legs[i][2]);
				box(solid, m, o, ax, ay, az, legs[i][0], 0, legs[i][1] + zoff, 4, 6, 4, i, false, white);
			}
			// Body.
			box(solid, m, o, ax, ay, az, -4, 6, -2, 8, 12, 4, 7, false, white);
			// Head: turned to stare at whoever is watching.
			Vec3d eye = camera(c);
			Vec3d to = new Vec3d(eye.x - c.x, 0, eye.z - c.z);
			double turn = 0;
			if (to.lengthSquared() > 1) {
				to = to.normalize();
				turn = MathHelper.clamp(Math.atan2(to.dotProduct(r), to.dotProduct(h)), -1.3, 1.3);
			}
			if (a < Creeper.RISEN) {
				turn *= smooth((a - Creeper.RISEN + 40) / 40.0);
			}
			double ct = Math.cos(turn), st = Math.sin(turn);
			Vec3d hf = h.multiply(ct).add(r.multiply(st)), hr = r.multiply(ct).subtract(h.multiply(st));
			Vec3d hax = hr.multiply(px * sxz), haz = hf.multiply(px * sxz);
			// Turn about the middle of the neck.
			Vec3d neck = o.add(ay.multiply(18));
			box(solid, m, neck, hax, ay, haz, -4, 0, -4, 8, 8, 8, 9, true, white);
		}
	}

	// ====================================================================== ELDER WYRM

	/**
	 * The wyrm: a dragon of black scale with magma glowing in the seams of it, plates of fire down its back, wings of
	 * dark red membrane lit from behind by the burning sky, eyes like furnaces; the fire in its throat when it breathes,
	 * its chest glowing white-hot as it gathers the nova.
	 */
	static final class WyrmFx extends FxEffect {
		private static final int SCALE = 0x241A1E, MAGMA = 0xFF5010;
		private final Vec3d c, h;

		WyrmFx(Vec3d c, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.c = c;
			this.h = unit(heading);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			if (t < Wyrm.EMERGE - 5 || t > Wyrm.END) {
				return super.tick(world);
			}
			WyrmPose.Pose p = WyrmPose.pose(t, h);
			Vec3d body = c.add(p.body);
			Vec3d vel = FossilShapes.frame(h, Wyrm.pos(t).subtract(Wyrm.pos(t - 1)));
			int k = Wyrm.breathing(t);
			boolean nova = t >= Wyrm.NOVA - 4 && t < Wyrm.NOVA + 30;
			if (k >= 0 || nova) {
				Vec3d mouth = c.add(p.mouth);
				Vec3d dir = k >= 0 ? c.add(FossilShapes.frame(h, Wyrm.breathHit(k, t))).subtract(mouth).normalize() : p.mouthDir;
				FxEngine.playMoving(VfxIds.WYRM_BREATH, mouth.x, mouth.y, mouth.z, (float) dir.x, (float) dir.y, (float) dir.z, nova ? 2.0F : 1.4F, 0, t * 17L,
						(float) vel.x, (float) vel.y, (float) vel.z);
				ClientFx.light(mouth, 1.2F, 0xFF7020);
			}
			if (t % 6 == 0) {
				ClientFx.light(body, 0.6F + 0.8F * (float) Wyrm.charge(t), 0xFF4010);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a < Wyrm.EMERGE - 5 || a > Wyrm.END) {
				return;
			}
			WyrmPose.Pose p = WyrmPose.pose(a, h);
			Vec3d o = c.subtract(cam);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			double sc = Wyrm.SC;
			float pulse = 0.75F + 0.25F * MathHelper.sin(a * 0.18F);
			for (Seg s : p.segs) {
				Vec3d A = o.add(s.a()), B = o.add(s.b());
				if (s.fire()) {
					Solids.tube(solid, m, A, B, s.ra() * sc, s.rb() * sc, 6, 0x3A1408, 1.0F, true);
					FxDraw.ribbon(glow, m, A, B, Vec3d.ZERO, Math.max(0.8, (s.ra() + s.rb()) * sc * 0.9), MAGMA, 0.45F * pulse, 0.25F * pulse);
				} else {
					Solids.tube(solid, m, A, B, s.ra() * sc, s.rb() * sc, 9, SCALE, 1.0F, true);
					if (s.ra() > 0.8) {
						// Magma in the seams between its scales.
						FxDraw.ribbon(glow, m, A, B, Vec3d.ZERO, s.ra() * sc * 0.35, 0xFF3A08, 0.12F * pulse, 0.12F * pulse);
					}
				}
			}
			// The wings: dark red membrane, lit from behind, its veins faintly glowing.
			for (Vec3d[] tri : p.membrane) {
				Vec3d A = o.add(tri[0]), B = o.add(tri[1]), C = o.add(tri[2]);
				Solids.quad(shade, m, A, B, C, C, 0x3E0C08, 0.82F);
				FxDraw.vertex(glow, m, A.x, A.y, A.z, 0xFF3010, 0.08F);
				FxDraw.vertex(glow, m, B.x, B.y, B.z, 0xFF5020, 0.02F);
				FxDraw.vertex(glow, m, C.x, C.y, C.z, 0xFF5020, 0.02F);
				FxDraw.vertex(glow, m, C.x, C.y, C.z, 0xFF5020, 0.02F);
				FxDraw.ribbon(glow, m, B, C, Vec3d.ZERO, 0.6, 0xFF6020, 0.12F, 0.12F);
			}
			// Eyes like furnaces.
			for (Vec3d e : p.eyes) {
				Vec3d q = o.add(e);
				FxKit.halo(consumers, m, cb, q, 5 * pulse, 0xFF5010, 0.7F);
				Solids.flare(glow, m, q, cb, 3.0, 0xFFE070, 0.95F);
			}
			// The chest gathering the nova.
			float ch = (float) Wyrm.charge(a);
			if (ch > 0.01F) {
				Vec3d q = o.add(p.chest);
				FxKit.halo(consumers, m, cb, q, 10 + 40 * ch, 0xFF8030, 0.6F * ch);
				FxKit.halo(consumers, m, cb, q, 4 + 14 * ch, 0xFFFFF0, 0.9F * ch);
			}
		}
	}
}
