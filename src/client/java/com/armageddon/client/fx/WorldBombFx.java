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
	 * The wyrm, drawn the way the Pumpkin Horseman is: not a solid but a creature of living fire. Every bone is a
	 * swarm of embers round a core of dark smoke, the embers flickering and streaming back off it as it flies, flame
	 * licking along its spine and tail; its wings are sheets of dim embers and smoke stretched between bright bones;
	 * its eyes are furnaces, the fire in its throat shows between its jaws, its chest burns white as it gathers the nova.
	 */
	static final class WyrmFx extends FxEffect {
		private static final int SMOKE_DARK = 0x140808, SMOKE_EDGE = 0x3A1610, CORE = 0xB02008, FLAME = 0xFF6A18, HOT = 0xFFE090, WING = 0x7A1408;
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
			// Embers shed off it as it flies, streaming away in its wake.
			if (t % 2 == 0) {
				Seg s = p.segs.get(world.random.nextInt(p.segs.size()));
				Vec3d q = c.add(s.a().lerp(s.b(), world.random.nextDouble()));
				FxEngine.playMoving(VfxIds.SABRE_EMBERS, q.x, q.y, q.z, 0, 1, 0, 3.0F, 0, t * 29L, (float) vel.x * 0.4F, (float) vel.y * 0.4F,
						(float) vel.z * 0.4F);
			}
			if (t % 6 == 0) {
				ClientFx.light(body, 0.8F + 0.8F * (float) Wyrm.charge(t), 0xFF4010);
			}
			return super.tick(world);
		}

		/** Everything drawn this frame: embers glowing, smoke puffs sorted and drawn together after. */
		private static final class Cloud {
			final VertexConsumer glow;
			final Matrix4f m;
			final Vec3d[] cb;
			final float a;
			final java.util.List<Effects.Puff> puffs = new java.util.ArrayList<>();

			Cloud(VertexConsumer glow, Matrix4f m, Vec3d[] cb, float a) {
				this.glow = glow;
				this.m = m;
				this.cb = cb;
				this.a = a;
			}

			/**
			 * One bone as fire: embers filling it, brighter at its skin, flickering and drifting off it in its wake, every
			 * seventh a puff of dark smoke holding its shape.
			 */
			void limb(Vec3d p0, Vec3d p1, double r0, double r1, int salt, double density, Vec3d drift, int core, int edge) {
				Vec3d axis = p1.subtract(p0);
				double len = axis.length();
				if (len < 1.0E-3) {
					return;
				}
				Vec3d dir = axis.multiply(1 / len);
				Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
				side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
				Vec3d up = side.crossProduct(dir).normalize();
				int n = (int) Math.min(420, Math.max(8, len * (r0 + r1) * density));
				double dot = Math.max(0.5, (r0 + r1) * 0.12);
				for (int i = 0; i < n; i++) {
					double u = Effects.hash(salt, i * 3 + 1);
					double sk = Math.sqrt(Effects.hash(salt, i * 3 + 2));
					double rr = (r0 + (r1 - r0) * u) * (0.35 + 0.65 * sk);
					double ang = Effects.hash(salt, i * 3 + 3) * MathHelper.TAU + a * 0.03;
					double ph = (a * 0.06 + Effects.hash(salt, i + 999)) % 1.0;
					Vec3d p = p0.add(axis.multiply(u)).add(side.multiply(Math.cos(ang) * rr)).add(up.multiply(Math.sin(ang) * rr))
							.add(0, ph * dot * 1.5, 0).add(drift.multiply(ph * dot * 4));
					float life = (float) (1 - ph);
					if (i % 7 == 0) {
						puffs.add(new Effects.Puff(p, (r0 + r1) * 0.28 + 0.6, SMOKE_EDGE, SMOKE_DARK, 0.3F * life));
					} else {
						boolean outer = sk > 0.75;
						double size = dot * (0.7 + 0.8 * Effects.hash(salt, i + 77));
						Solids.dot(glow, m, p, cb, size, outer ? edge : core, 0.9F * life);
						if (i % 5 == 1) {
							Solids.dot(glow, m, p, cb, size * 0.4, HOT, 0.8F * life);
						}
					}
				}
			}

			/** A tongue of flame licking from {@code root} along {@code dir}, as embers streaming off it. */
			void flame(Vec3d root, Vec3d dir, double len, int salt, double size) {
				Vec3d sd = dir.crossProduct(new Vec3d(0, 1, 0));
				sd = sd.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : sd.normalize();
				for (int k = 0; k < 12; k++) {
					double ph = (a * 0.09 + Effects.hash(salt, k)) % 1.0;
					double wv = Math.sin(a * 0.5 + salt + k) * 0.4 * ph * len * 0.3;
					Vec3d p = root.add(dir.multiply(len * ph)).add(0, wv, 0).add(sd.multiply(Math.cos(a * 0.4 + k) * 0.3 * ph * len * 0.3));
					Solids.dot(glow, m, p, cb, size * (1 - ph) + size * 0.2, k % 3 == 0 ? HOT : FLAME, 0.9F * (float) (1 - ph));
				}
			}

			/** A wing membrane as a sheet of dim embers and smoke, its far edge brighter. */
			void sheet(Vec3d A, Vec3d B, Vec3d C, int salt, double dot) {
				double area = B.subtract(A).crossProduct(C.subtract(A)).length() * 0.5;
				int n = (int) Math.min(380, Math.max(10, area / (dot * dot * 7)));
				for (int i = 0; i < n; i++) {
					double u = Effects.hash(salt, i * 2 + 1), v = Effects.hash(salt, i * 2 + 2);
					if (u + v > 1) {
						u = 1 - u;
						v = 1 - v;
					}
					Vec3d p = A.add(B.subtract(A).multiply(u)).add(C.subtract(A).multiply(v));
					double ph = (a * 0.05 + Effects.hash(salt, i + 555)) % 1.0;
					float life = (float) (0.4 + 0.6 * (1 - ph));
					if (i % 6 == 0) {
						puffs.add(new Effects.Puff(p, dot * 2.6, SMOKE_EDGE, SMOKE_DARK, 0.22F * life));
					} else {
						boolean rim = u + v > 0.85;
						Solids.dot(glow, m, p, cb, dot * (0.8 + 0.6 * Effects.hash(salt, i + 7)), rim ? FLAME : WING, (rim ? 0.8F : 0.5F) * life);
					}
				}
			}
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
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Cloud cl = new Cloud(glow, m, cb, a);
			double sc = Wyrm.SC;
			Vec3d vel = FossilShapes.frame(h, Wyrm.pos(a + 1).subtract(Wyrm.pos(a - 1)));
			Vec3d drift = vel.lengthSquared() < 0.01 ? new Vec3d(0, 0.3, 0) : vel.normalize().multiply(-1);
			int salt = 1;
			for (Seg s : p.segs) {
				Vec3d A = o.add(s.a()), B = o.add(s.b());
				if (s.fire()) {
					// The plates down its back and tail: flame licking up off them.
					cl.flame(A, B.subtract(A).normalize().add(drift.multiply(0.4)).normalize(), A.distanceTo(B) * 1.6, salt++, Math.max(0.6, s.ra() * sc * 0.5));
				} else {
					double density = s.ra() > 1.5 ? 0.5 : s.ra() > 0.6 ? 0.9 : 2.2;
					cl.limb(A, B, s.ra() * sc, s.rb() * sc, salt++, density, drift, CORE, FLAME);
				}
			}
			int w = 900;
			for (Vec3d[] tri : p.membrane) {
				cl.sheet(o.add(tri[0]), o.add(tri[1]), o.add(tri[2]), w++, 1.1);
			}
			// Eyes like furnaces.
			float pulse = 0.8F + 0.2F * MathHelper.sin(a * 0.3F);
			for (Vec3d e : p.eyes) {
				Vec3d q = o.add(e);
				FxKit.halo(consumers, m, cb, q, 6 * pulse, 0xFF5010, 0.75F);
				Solids.flare(glow, m, q, cb, 3.4, 0xFFE070, 1.0F);
			}
			// The fire in its throat, showing between its jaws when they part.
			float jaw = (float) Wyrm.jaw(a);
			if (jaw > 0.2F) {
				FxKit.halo(consumers, m, cb, o.add(p.mouth), 6 + 6 * jaw, 0xFFA040, 0.6F * jaw);
			}
			// Its chest gathering the nova.
			float ch = (float) Wyrm.charge(a);
			if (ch > 0.01F) {
				Vec3d q = o.add(p.chest);
				FxKit.halo(consumers, m, cb, q, 10 + 40 * ch, 0xFF8030, 0.6F * ch);
				FxKit.halo(consumers, m, cb, q, 4 + 14 * ch, 0xFFFFF0, 0.9F * ch);
			}
			Effects.drawPuffs(cl.puffs, consumers.getBuffer(FxRenderLayers.SHADE), m, cb, 6);
		}
	}
}
