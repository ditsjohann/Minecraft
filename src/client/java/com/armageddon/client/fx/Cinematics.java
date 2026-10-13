package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.registry.ModParticles;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Scripted camera cinematics for the WORLD DESTRUCTION tier. Each scene owns its scenery (a sky giant,
 * Earth from orbit, an alien launch complex) and a camera track; the server fires the matching real
 * destruction at the same tick, so the world is already breaking when control is handed back.
 */
public final class Cinematics {
	private Cinematics() {
	}

	/** Camera pose requested for the current frame. fov &lt;= 0 means "leave the player's FOV alone". */
	public static final class State {
		public Vec3d pos = Vec3d.ZERO;
		public float yaw, pitch, fov;
		public boolean space;
	}

	public static final int GALACTIC_SMASH = 1, BOOM_SURVEY = 2, ALIEN_LAUNCH = 3;

	public static Cine create(int scene, Vec3d anchor, Vec3d heading, int life) {
		return switch (scene) {
			case GALACTIC_SMASH -> new GalacticSmash(anchor, heading, life);
			case BOOM_SURVEY -> new BoomSurvey(anchor, heading, life);
			default -> new AlienLaunch(anchor, heading, life);
		};
	}

	// ======================================================================== base

	public abstract static class Cine extends FxEffect {
		protected final Vec3d anchor, fwd, right;
		protected final State st = new State();
		public boolean done;

		protected Cine(Vec3d anchor, Vec3d heading, int life) {
			super(Math.max(40, life));
			this.anchor = anchor;
			Vec3d h = new Vec3d(heading.x, 0, heading.z);
			this.fwd = h.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : h.normalize();
			this.right = new Vec3d(-fwd.z, 0, fwd.x);
		}

		protected abstract void direct(float a, State s);

		public State frame(float tickDelta) {
			direct(age + tickDelta, st);
			return st;
		}

		@Override
		public boolean tick(ClientWorld world) {
			boolean alive = super.tick(world);
			if (!alive) {
				done = true;
			}
			return alive;
		}
	}

	static double ease(double t) {
		t = MathHelper.clamp(t, 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	static Vec3d lerp(Vec3d a, Vec3d b, double t) {
		return a.lerp(b, t);
	}

	static double clamp01(double t) {
		return MathHelper.clamp(t, 0.0, 1.0);
	}

	static void aim(State s, Vec3d pos, Vec3d target, float fov, boolean space) {
		s.pos = pos;
		Vec3d d = target.subtract(pos);
		s.yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		s.pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
		s.fov = fov;
		s.space = space;
	}

	/** Two-bone IK: elbow position for a limb with bone lengths l1, l2 reaching {@code h} from {@code s}. */
	static Vec3d elbow(Vec3d s, Vec3d h, double l1, double l2, Vec3d pole) {
		Vec3d d = h.subtract(s);
		double dist = Math.max(1.0E-3, Math.min(d.length(), l1 + l2 - 0.5));
		Vec3d dir = d.normalize();
		double a = (l1 * l1 - l2 * l2 + dist * dist) / (2 * dist);
		double hh = Math.sqrt(Math.max(0, l1 * l1 - a * a));
		Vec3d perp = pole.subtract(dir.multiply(pole.dotProduct(dir)));
		if (perp.lengthSquared() < 1.0E-6) {
			perp = new Vec3d(0, 1, 0).crossProduct(dir);
		}
		return s.add(dir.multiply(a)).add(perp.normalize().multiply(hh));
	}

	// ======================================================================== GALACTIC SMASH

	/** A cosmic giant tears into the sky, scans the city, winds up, and punches the ground. */
	public static class GalacticSmash extends Cine {
		private static final int PUNCH = 440;
		private static final double BONE = 190;
		private final Vec3d shoulderMid;

		GalacticSmash(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.shoulderMid = anchor.add(fwd.multiply(230)).add(0, 300, 0);
		}

		private Vec3d rs() {
			return shoulderMid.add(right.multiply(75));
		}

		private Vec3d ls() {
			return shoulderMid.subtract(right.multiply(75));
		}

		private Vec3d head() {
			return shoulderMid.add(0, 118, 0);
		}

		private Vec3d hand(float a) {
			Vec3d hang = rs().add(right.multiply(28)).add(0, -360, 0).add(Math.sin(a * 0.05) * 6, 0, Math.cos(a * 0.04) * 6);
			Vec3d wind = rs().add(fwd.multiply(165)).add(0, 180, 0).add(right.multiply(45));
			Vec3d tgt = anchor.add(0, 2, 0);
			if (a < 330) {
				return hang;
			}
			if (a < 420) {
				double w = ease((a - 330) / 90.0);
				double tremble = w * 5;
				return lerp(hang, wind, w).add(Math.sin(a * 1.9) * tremble, Math.cos(a * 2.3) * tremble, Math.sin(a * 1.3) * tremble);
			}
			if (a < PUNCH) {
				double p = (a - 420) / (double) (PUNCH - 420);
				return lerp(wind, tgt, p * p * p);
			}
			return tgt;
		}

		private Vec3d headDir(float a) {
			Vec3d face = fwd.multiply(-1);
			double yaw = 0, pitch = 0;
			if (a > 120 && a < 280) {
				yaw = Math.sin((a - 120) / 160.0 * Math.PI * 2.2) * 1.15 * ease((a - 120) / 20.0) * (1 - ease((a - 250) / 30.0));
			}
			if (a >= 215) {
				pitch = -0.45 * ease((a - 215) / 40.0);
			}
			Vec3d dir = face.multiply(Math.cos(yaw)).add(right.multiply(Math.sin(yaw)));
			return new Vec3d(dir.x * Math.cos(pitch), Math.sin(pitch), dir.z * Math.cos(pitch)).normalize();
		}

		@Override
		protected void direct(float a, State s) {
			Vec3d head = head();
			if (a < 150) {
				Vec3d p = anchor.add(fwd.multiply(-(30 + a * 0.12))).add(0, 3, 0);
				aim(s, p, lerp(anchor.add(fwd.multiply(230)).add(0, 150, 0), head, ease(a / 150.0)), 75, false);
			} else if (a < 260) {
				Vec3d p = anchor.add(right.multiply(420)).add(fwd.multiply(60 + (a - 150) * 0.5)).add(0, 180, 0);
				aim(s, p, head.add(0, -60, 0), 55, false);
			} else if (a < 330) {
				double d = lerp(135, 92, ease((a - 260) / 70.0));
				aim(s, head.add(headDir(a).multiply(d)).add(right.multiply(28)).add(0, 4, 0), head, 45, false);
			} else if (a < 420) {
				Vec3d h = hand(a);
				double ang = (a - 330) * 0.012;
				Vec3d p = h.add(right.multiply(Math.cos(ang) * 150)).add(fwd.multiply(-Math.sin(ang) * 160 - 40)).add(0, -70, 0);
				aim(s, p, h, 70, false);
			} else if (a < PUNCH) {
				Vec3d h = hand(a);
				aim(s, h.add(right.multiply(75)).add(fwd.multiply(-50)).add(0, 25, 0), h, (float) lerp(85, 105, (a - 420) / 20.0), false);
			} else if (a < 540) {
				double u = (a - PUNCH) / 100.0;
				aim(s, anchor.add(fwd.multiply(-190 + u * 30)).add(right.multiply(50)).add(0, 14 + u * 8, 0), anchor.add(0, 22, 0), 75, false);
			} else {
				double u = ease((a - 540) / 100.0);
				aim(s, anchor.add(fwd.multiply(-220 - u * 40)).add(0, 20 + 120 * u, 0), lerp(anchor.add(0, 40, 0), head, u), 70, false);
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x0C0A24, Math.min(0.9F, age / 60.0F));
			if (age >= 330 && age < PUNCH) {
				ClientFx.shake = Math.max(ClientFx.shake, 0.4F + (age - 330) * 0.012F);
			}
			if (age >= PUNCH && age < PUNCH + 160) {
				ClientFx.shake = Math.max(ClientFx.shake, 8.0F * (1.0F - (age - PUNCH) / 160.0F) + 0.6F);
			}
			if (age >= 520 && age < 580) {
				for (int i = 0; i < 18; i++) {
					Vec3d p = shoulderMid.add(world.random.nextGaussian() * 60, -world.random.nextDouble() * 200 + 60, world.random.nextGaussian() * 60);
					world.addImportantParticle(ParticleTypes.END_ROD, true, p.x, p.y, p.z, world.random.nextGaussian() * 0.6, world.random.nextGaussian() * 0.6, world.random.nextGaussian() * 0.6);
				}
			}
			if (age >= PUNCH && age < PUNCH + 40) {
				for (int i = 0; i < 40; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double r = (age - PUNCH) * 7 + world.random.nextDouble() * 20;
					world.addImportantParticle(ModParticles.SHOCK_DUST, true, anchor.x + Math.cos(ang) * r, anchor.y + 1, anchor.z + Math.sin(ang) * r,
							Math.cos(ang) * 1.5, 0.8, Math.sin(ang) * 1.5);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			float env = ArsenalEffects.envelope(a, lifetime, 40, 70);
			boolean body = a > 25 && a < 585;
			Vec3d off = cam.multiply(-1);
			Vec3d S = shoulderMid.add(off), RS = rs().add(off), LS = ls().add(off);
			Vec3d P = S.add(0, -235, 0), N = S.add(0, 38, 0), H = head().add(off);
			Vec3d handR = hand(a).add(off);
			Vec3d elbowR = elbow(RS, handR, BONE, BONE, fwd.multiply(0.7).add(right.multiply(0.9)).add(0, 0.2, 0)).add(0, 0, 0);
			Vec3d handL = LS.add(right.multiply(-30)).add(0, -355 + Math.sin(a * 0.04) * 8, 0);
			Vec3d elbowL = elbow(LS, handL, BONE, BONE, right.multiply(-1).add(fwd.multiply(0.3)));
			Vec3d hd = headDir(a);
			int skin = 0x241C55, skinArm = 0x2E2266;

			if (body) {
				Solids.tube(solid, m, N, P, 70, 52, 14, skin, 1.0F, true);
				Solids.tube(solid, m, N, N.add(0, 30, 0), 26, 22, 10, skin, 1.0F, true);
				Solids.sphere(solid, m, H, 42, skinArm, 12, 20, true);
				Solids.sphere(solid, m, RS, 38, skin, 8, 12, true);
				Solids.sphere(solid, m, LS, 38, skin, 8, 12, true);
				Solids.tube(solid, m, RS, elbowR, 31, 27, 10, skinArm, 1.0F, true);
				Solids.tube(solid, m, elbowR, handR, 27, 24, 10, skinArm, 1.0F, true);
				Solids.sphere(solid, m, handR, 52, 0x3A2A80, 10, 16, true);
				Solids.tube(solid, m, LS, elbowL, 31, 27, 10, skinArm, 1.0F, true);
				Solids.tube(solid, m, elbowL, handL, 27, 24, 10, skinArm, 1.0F, true);
				Solids.sphere(solid, m, handL, 46, 0x3A2A80, 10, 16, true);
			}
			// Rim light: the same shapes again as additive shells.
			float rim = 0.25F * env;
			Solids.tube(glow, m, N, P, 74, 56, 12, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, RS, elbowR, 34, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowR, handR, 30, 27, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, LS, elbowL, 34, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowL, handL, 30, 27, 8, 0x7A5CFF, rim, false);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Solids.dot(glow, m, H, cb, 80, 0x7A5CFF, 0.22F * env);
			Solids.dot(glow, m, handR, cb, 110, a > 380 ? 0xFFE0A0 : 0x9A7CFF, (0.25F + 0.5F * (float) clamp01((a - 330) / 90.0)) * env);
			// Eyes.
			Vec3d sideDir = new Vec3d(-hd.z, 0, hd.x).normalize();
			int eyeCol = a > 215 ? 0xFFFFFF : 0x40F8FF;
			for (int e = -1; e <= 1; e += 2) {
				Vec3d eye = H.add(hd.multiply(38)).add(sideDir.multiply(e * 17)).add(0, 6, 0);
				Solids.dot(glow, m, eye, cb, 9, eyeCol, env);
				Solids.dot(glow, m, eye, cb, 30, eyeCol, 0.35F * env);
				if (a > 255 && a < 440) {
					float f = (float) clamp01((a - 255) / 40.0);
					FxDraw.ribbon(glow, m, eye, anchor.add(off).add(0, 5, 0), Vec3d.ZERO, 1.2, 0xFF4060, 0.0F, 0.5F * f);
				}
			}
			// Halo ring.
			float spin = a * 0.03F;
			Vec3d u = new Vec3d(Math.cos(spin), 0, Math.sin(spin)), v = new Vec3d(-Math.sin(spin) * 0.3, 0.95, Math.cos(spin) * 0.3).normalize();
			FxDraw.ring(glow, m, H.add(0, 20, 0), u, v, 92, 100, ArsenalEffects.rainbow(a * 0.01F), 0.0F, 0.8F * env, 0.0F, 64);
			// Mist below the waist.
			FxDraw.disc(shade, m, P.add(0, -10, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 150, 0xB0A8FF, 0.45F * env, 0.0F, 32);
			// Wind-up charge and punch trail.
			if (a > 330 && a < 470) {
				float w = (float) clamp01((a - 330) / 90.0);
				for (int k = 0; k < 3; k++) {
					double rr = 140 - k * 30 - w * 40;
					Vec3d ax = fwd.multiply(Math.cos(a * 0.15 + k)).add(right.multiply(Math.sin(a * 0.15 + k)));
					FxDraw.ring(glow, m, handR, ax, new Vec3d(0, 1, 0), rr * 0.9, rr, k == 1 ? 0xFFFFFF : 0xFFB070, 0.0F, 0.5F * w, 0.0F, 40);
				}
			}
			if (a > 420 && a < 480) {
				Vec3d prev = null;
				for (int k = 0; k < 10; k++) {
					Vec3d p = hand(a - k * 1.5F).add(off);
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 55 * (1 - k / 10.0), k % 2 == 0 ? 0xFFFFFF : 0xFFB070, 0.7F * (1 - k / 10F), 0.7F * (1 - (k + 1) / 10F));
					}
					prev = p;
				}
			}
			// Ground shock rings and dust pillar after the strike.
			if (a >= PUNCH) {
				float t = a - PUNCH;
				Vec3d g = anchor.add(off).add(0, 0.6, 0);
				for (int k = 0; k < 4; k++) {
					double rr = t * (9 - k) - k * 14;
					if (rr > 2) {
						FxDraw.ring(glow, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), rr, rr + 7 + k * 3, k % 2 == 0 ? 0xFFFFFF : 0xFFB070, 0.0F, 0.8F * (1 - Math.min(1, t / 120)), 0.0F, 96);
					}
				}
				FxDraw.beam(glow, m, g, g.add(0, 400, 0), Vec3d.ZERO, 14 * (1 - Math.min(1, t / 90)), 0xFFE0B0, 0.6F);
				FxDraw.disc(shade, m, g, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 60 + t * 3, 0x6A5A50, 0.5F * (1 - Math.min(1, t / 140)), 0.0F, 40);
			}
		}
	}

	// ======================================================================== BOOMING TICK

	/** Camera rises over the carpet-bombed site, cuts to orbit, and Earth cracks open and explodes. */
	public static class BoomSurvey extends Cine {
		private static final int SPACE_AT = 200;
		private final Vec3d sa;
		private final List<Vec3d[]> cracks = new ArrayList<>();
		private final Vec3d[] shardDir = new Vec3d[70];
		private final double[] shardSpeed = new double[70];

		BoomSurvey(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.sa = anchor.add(0, 3300, 0);
			for (int k = 0; k < 18; k++) {
				Vec3d d = rnd(k, 0);
				Vec3d[] pts = new Vec3d[16];
				for (int i = 0; i < pts.length; i++) {
					pts[i] = d;
					d = d.add(rnd(k * 31 + i, 7).multiply(0.22)).normalize();
				}
				cracks.add(pts);
			}
			for (int i = 0; i < shardDir.length; i++) {
				shardDir[i] = rnd(i, 99);
				shardSpeed[i] = 2.0 + Solids.hash(i, 5) * 5.0;
			}
		}

		private static Vec3d rnd(int i, int salt) {
			double a = Solids.hash(i, salt) * MathHelper.TAU;
			double y = Solids.hash(i, salt + 1) * 2 - 1;
			double r = Math.sqrt(1 - y * y);
			return new Vec3d(Math.cos(a) * r, y, Math.sin(a) * r);
		}

		@Override
		protected void direct(float a, State s) {
			if (a < SPACE_AT) {
				double t = ease(a / SPACE_AT);
				Vec3d p = anchor.add(fwd.multiply(-40 * (1 - t))).add(0, 3 + 300 * t, 0);
				aim(s, p, anchor.add(fwd.multiply(30 * t)).add(0, 0, 0), (float) lerp(80, 95, t), false);
			} else {
				double u = a - SPACE_AT;
				double ang = 0.5 + u * 0.004;
				double dist = lerp(250, 205, clamp01(u / 140.0));
				Vec3d p = sa.add(Math.cos(ang) * dist, 45 - u * 0.1, Math.sin(ang) * dist);
				aim(s, p, sa, 62, true);
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age < SPACE_AT) {
				ClientFx.shake = Math.max(ClientFx.shake, 1.2F);
			} else {
				ClientFx.tintSky(0x000006, 1.0F);
				float u = age - SPACE_AT;
				if (u > 100 && u < 140) {
					ClientFx.shake = Math.max(ClientFx.shake, (u - 100) * 0.05F);
				}
				if (u >= 140) {
					ClientFx.shake = Math.max(ClientFx.shake, 9.0F);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			if (a < SPACE_AT - 2) {
				return;
			}
			float u = a - SPACE_AT;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Solids.stars(glow, m, cb, a, 1.0F);
			Vec3d c = sa.subtract(cam);
			double R = 90;
			if (u < 140) {
				Solids.ellipsoid(solid, m, c, R, R, R, 26, 52, rotated(Solids.earth(), a * 0.004), true, 1.0F);
				FxDraw.sphere(shade, m, c, R * 1.015, Vec3d.ZERO, 0xFFFFFF, 0.05F, 0.22F, 12, 24);
				FxDraw.sphere(glow, m, c, R * 1.07, Vec3d.ZERO, 0x6AA8FF, 0.0F, 0.5F, 14, 28);
				float prog = (float) clamp01((u - 70) / 55.0);
				if (prog > 0) {
					for (Vec3d[] pts : cracks) {
						int n = (int) (pts.length * prog);
						for (int i = 0; i + 1 < n; i++) {
							float k = 0.6F + 0.4F * (float) Math.sin(a * 0.5 + i);
							FxDraw.ribbon(glow, m, c.add(pts[i].multiply(R * 1.004)), c.add(pts[i + 1].multiply(R * 1.004)), Vec3d.ZERO,
									2.0 + prog * 4, i % 3 == 0 ? 0xFFFFFF : 0xFF9030, k, k);
						}
					}
					FxDraw.sphere(glow, m, c, R * 0.98, Vec3d.ZERO, 0xFFA040, 0.0F, 0.5F * prog, 10, 20);
				}
			} else {
				float e = u - 140;
				for (int i = 0; i < shardDir.length; i++) {
					Vec3d pc = c.add(shardDir[i].multiply(R * 0.9 + shardSpeed[i] * e * 1.6));
					double sz = 4 + Solids.hash(i, 8) * 9;
					Vec3d ax = shardDir[i].crossProduct(new Vec3d(0.3, 1, 0.2)).normalize().multiply(sz);
					int col = i % 3 == 0 ? 0xFF8A20 : i % 3 == 1 ? 0x3E9A44 : 0x1E55B8;
					Solids.tube(solid, m, pc.subtract(ax), pc.add(ax), sz * 0.5, sz * 0.2, 5, col, 1.0F, true);
					FxDraw.beam(glow, m, pc, pc.subtract(shardDir[i].multiply(shardSpeed[i] * 6)), Vec3d.ZERO, 1.4, 0xFFC070, 0.6F);
				}
				double fr = 20 + e * 9;
				FxDraw.sphere(glow, m, c, fr, Vec3d.ZERO, 0xFFFFFF, 0.6F * (float) Math.max(0, 1 - e / 50), 0.9F * (float) Math.max(0, 1 - e / 60), 16, 32);
				FxDraw.sphere(glow, m, c, fr * 1.4, Vec3d.ZERO, 0xFF8030, 0.0F, 0.5F * (float) Math.max(0, 1 - e / 55), 14, 28);
				FxDraw.ring(glow, m, c, cb[0], cb[1], e * 14, e * 14 + 14, 0xFFFFFF, 0.0F, 0.8F * (float) Math.max(0, 1 - e / 50), 0.0F, 96);
			}
		}

		static Solids.Cell rotated(Solids.Cell inner, double ang) {
			return (i, j, n) -> inner.color(i, j, new Vec3d(n.x * Math.cos(ang) - n.z * Math.sin(ang), n.y, n.x * Math.sin(ang) + n.z * Math.cos(ang)));
		}
	}

	// ======================================================================== ALIEN LAUNCH

	/** A visit to the alien world: the rocket is fuelled, armed, and fired at Earth. */
	public static class AlienLaunch extends Cine {
		private static final double PR = 120, LEN = 70, RR = 6.5;
		private final Vec3d c, base, earth;

		AlienLaunch(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.c = anchor.add(0, 3700, 0);
			this.base = c.add(0, PR, 0);
			this.earth = c.add(300, 560, 0);
		}

		private Vec3d rocketCentre(float a, int[] tangentOut) {
			Vec3d c0 = base.add(0, LEN / 2, 0);
			if (a < 280) {
				return c0;
			}
			double u = clamp01((a - 280) / 200.0);
			u = u * u;
			Vec3d ctrl = c0.add(0, 240, 0);
			return bez(c0, ctrl, earth, u);
		}

		private static Vec3d bez(Vec3d p0, Vec3d p1, Vec3d p2, double u) {
			return p0.multiply((1 - u) * (1 - u)).add(p1.multiply(2 * (1 - u) * u)).add(p2.multiply(u * u));
		}

		private Vec3d rocketDir(float a) {
			if (a < 285) {
				return new Vec3d(0, 1, 0);
			}
			Vec3d p0 = rocketCentre(a - 1.5F, null), p1 = rocketCentre(a + 1.5F, null);
			Vec3d d = p1.subtract(p0);
			return d.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : d.normalize();
		}

		@Override
		protected void direct(float a, State s) {
			Vec3d rc = rocketCentre(a, null);
			Vec3d rd = rocketDir(a);
			if (a < 110) {
				double ang = 0.3 + a * 0.006;
				double r = 430 - a * 1.1;
				aim(s, c.add(Math.cos(ang) * r, 130 - a * 0.5, Math.sin(ang) * r), c.add(0, PR + 20, 0), 70, true);
			} else if (a < 230) {
				double ang = a * 0.012;
				Vec3d focus = base.add(0, LEN * 0.45, 0);
				aim(s, focus.add(Math.cos(ang) * 78, -6 + (a - 110) * 0.28, Math.sin(ang) * 78), focus, 60, true);
			} else if (a < 300) {
				aim(s, base.add(40, -4, 30), rc.add(0, LEN * 0.2, 0), 92, true);
			} else if (a < 400) {
				Vec3d side = rd.crossProduct(new Vec3d(0, 0, 1)).normalize();
				aim(s, rc.add(side.multiply(95)).add(0, 14, 0), rc, 65, true);
			} else {
				aim(s, rc.subtract(rd.multiply(105)).add(0, 28, 0), lerp(earth, rc, 0.15), 78, true);
			}
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x000006, 1.0F);
			if (age >= 240 && age < 330) {
				ClientFx.shake = Math.max(ClientFx.shake, 1.0F + (age - 240) * 0.03F);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Solids.stars(glow, m, cb, a, 1.0F);
			Vec3d pc = c.subtract(cam);
			// Alien world, rings and moons.
			double spin = a * 0.002;
			Solids.ellipsoid(solid, m, pc, PR, PR, PR, 28, 56, BoomSurvey.rotated(Solids.bands(0x5A2A9A, 0x1E8C8C, 0xB03A8A, 7), spin), true, 1.0F);
			FxDraw.sphere(glow, m, pc, PR * 1.06, Vec3d.ZERO, 0xA070FF, 0.0F, 0.5F, 14, 28);
			Vec3d ringN = new Vec3d(0.25, 1, 0.1).normalize();
			Vec3d[] rb = ArsenalEffects.basis(ringN);
			FxDraw.ring(shade, m, pc, rb[0], rb[1], 165, 215, 0xC8A0FF, 0.0F, 0.35F, 0.0F, 96);
			FxDraw.ring(glow, m, pc, rb[0], rb[1], 190, 200, 0xFFFFFF, 0.0F, 0.3F, 0.0F, 96);
			for (int k = 0; k < 3; k++) {
				double ang = a * (0.008 + k * 0.004) + k * 2.1;
				Vec3d mp = pc.add(Math.cos(ang) * (270 + k * 50), 30 + Math.sin(k * 3) * 50, Math.sin(ang) * (270 + k * 50));
				Solids.sphere(solid, m, mp, 15 - k * 4, k == 1 ? 0x8A8A98 : 0xB8A8A0, 10, 16, true);
			}
			// Launch complex.
			Vec3d bs = base.subtract(cam);
			Vec3d rcw = rocketCentre(a, null);
			Vec3d rdir = rocketDir(a);
			float flame = a < 240 ? 0 : (float) clamp01((a - 240) / 50.0);
			Solids.rocket(solid, glow, m, rcw.subtract(cam), rdir, LEN, RR, a, flame);
			if (a < 285) {
				// Pad, towers, retracting gantry arms and rising energy rings.
				Solids.tube(solid, m, bs.add(0, -1, 0), bs.add(0, 1.5, 0), 28, 26, 16, 0x1A1424, 1.0F, true);
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d tb = bs.add(sgn * 28, 0, 0);
					Solids.tube(solid, m, tb, tb.add(0, 74, 0), 2.4, 2.0, 6, 0x3A3A48, 1.0F, true);
					for (int k = 0; k < 4; k++) {
						double h = 14 + k * 15;
						Vec3d anchorPt = tb.add(0, h, 0);
						Vec3d hull = bs.add(sgn * RR * 0.9, h, 0);
						double ret = ease((a - 170 - k * 10) / 40.0);
						Vec3d end = lerp(hull, anchorPt.add(-sgn * 4, 0, 0), ret);
						Solids.tube(solid, m, anchorPt, end, 1.0, 0.7, 5, 0x6A6A7A, 1.0F, true);
						Solids.tube(glow, m, end, end.add(0, 0.8, 0), 1.5, 1.5, 6, ret < 1 ? 0xFF8A20 : 0x404058, 0.9F, false);
					}
				}
				for (int k = 0; k < 3; k++) {
					double h = ((a * 0.9 + k * 24) % 72);
					if (a > 120) {
						FxDraw.ring(glow, m, bs.add(0, h, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), RR * 1.2, RR * 1.5, 0xC070FF, 0.0F, 0.8F, 0.0F, 32);
					}
				}
				float blink = ((int) (a * 0.15) & 1) == 0 ? 1.0F : 0.2F;
				if (a > 150) {
					Solids.dot(glow, m, bs.add(0, LEN + 6, 0), cb, 4, 0xFF3030, blink);
				}
			} else {
				// Launch plume lighting the planet.
				Solids.dot(glow, m, base.subtract(cam).add(0, 2, 0), cb, 90 * Math.max(0.0, 1 - (a - 280) / 80.0), 0xC0FFE0, 0.7F);
			}
			// Earth, far away and tiny.
			if (a > 300) {
				Vec3d ec = earth.subtract(cam);
				float grow = 1.0F + (float) clamp01((a - 360) / 120.0) * 0.35F;
				Solids.ellipsoid(solid, m, ec, 70 * grow, 70 * grow, 70 * grow, 22, 44, BoomSurvey.rotated(Solids.earth(), a * 0.004), true, 1.0F);
				FxDraw.sphere(glow, m, ec, 76 * grow, Vec3d.ZERO, 0x6AA8FF, 0.0F, 0.5F, 12, 24);
			}
		}
	}
}
