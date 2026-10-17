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
 * Earth from orbit, an alien launch complex), a shot list and on-screen captions. The server runs the
 * real destruction on the same timeline, so the world is already breaking when control returns.
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

	/** On-screen caption (vanilla titles are hidden while the HUD is off). */
	public record Caption(String title, String sub, int color, float alpha) {
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

		/** Handheld camera wobble in blocks/degrees for this moment of the scene. */
		protected float handheld(float a) {
			return 0.15F;
		}

		public Caption caption(float a) {
			return null;
		}

		public float time(float tickDelta) {
			return age + tickDelta;
		}

		public State frame(float tickDelta) {
			float a = age + tickDelta;
			direct(a, st);
			// Handheld wobble, toned down and capped so even the big moments stay watchable.
			float h = Math.min(0.5F, handheld(a) * 0.35F);
			if (h > 0) {
				double t = a * 0.05;
				st.pos = st.pos.add((Math.sin(t * 7.1) + Math.sin(t * 13.3) * 0.5) * h * 0.4,
						(Math.cos(t * 6.7) + Math.sin(t * 11.9) * 0.5) * h * 0.4,
						(Math.sin(t * 5.3 + 1) + Math.cos(t * 12.7) * 0.5) * h * 0.4);
				st.yaw += (float) ((Math.sin(t * 9.1) + Math.sin(t * 17.3) * 0.4) * h);
				st.pitch = MathHelper.clamp(st.pitch + (float) ((Math.cos(t * 8.3) + Math.sin(t * 15.1) * 0.4) * h), -90.0F, 90.0F);
			}
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

	/** Fading caption between {@code start} and {@code end}. */
	static Caption cap(float a, float start, float end, String title, String sub, int color) {
		if (a < start || a > end) {
			return null;
		}
		float alpha = Math.min(1.0F, Math.min((a - start) / 8.0F, (end - a) / 8.0F));
		return new Caption(title, sub, color, alpha);
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

	static Solids.Cell rotated(Solids.Cell inner, double ang) {
		return (i, j, n) -> inner.color(i, j, new Vec3d(n.x * Math.cos(ang) - n.z * Math.sin(ang), n.y, n.x * Math.sin(ang) + n.z * Math.cos(ang)));
	}

	static Vec3d sphereDir(int i, int salt) {
		double a = Solids.hash(i, salt) * MathHelper.TAU;
		double y = Solids.hash(i, salt + 1) * 2 - 1;
		double r = Math.sqrt(1 - y * y);
		return new Vec3d(Math.cos(a) * r, y, Math.sin(a) * r);
	}

	/** Earth: oceans, continents, cloud shell and atmosphere glow. {@code c} is camera-relative. */
	static void earth(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d c, double r, double spin) {
		Solids.ellipsoid(solid, m, c, r, r, r, 26, 52, rotated(Solids.earth(), spin), true, 1.0F);
		for (int k = 0; k < 26; k++) {
			Vec3d d = sphereDir(k, 41);
			double ang = spin * 1.3;
			d = new Vec3d(d.x * Math.cos(ang) - d.z * Math.sin(ang), d.y, d.x * Math.sin(ang) + d.z * Math.cos(ang));
			Vec3d[] b = ArsenalEffects.basis(d);
			FxDraw.disc(shade, m, c.add(d.multiply(r * 1.015)), b[0], b[1], r * (0.12 + Solids.hash(k, 3) * 0.12), 0xFFFFFF, 0.55F, 0.0F, 12);
		}
		FxDraw.sphere(glow, m, c, r * 1.07, Vec3d.ZERO, 0x6AA8FF, 0.0F, 0.55F, 14, 28);
	}

	/** Points on a sphere at angular distance theta from {@code centre}, as a closed ring. */
	static void sphereRing(VertexConsumer glow, Matrix4f m, Vec3d c, double r, Vec3d centre, double theta, double width, int color, float alpha) {
		Vec3d[] b = ArsenalEffects.basis(centre);
		Vec3d prev = null;
		for (int k = 0; k <= 96; k++) {
			double phi = k * MathHelper.TAU / 96;
			Vec3d d = centre.multiply(Math.cos(theta)).add(b[0].multiply(Math.sin(theta) * Math.cos(phi))).add(b[1].multiply(Math.sin(theta) * Math.sin(phi)));
			Vec3d p = c.add(d.multiply(r * 1.006));
			if (prev != null) {
				FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, width, color, alpha, alpha);
			}
			prev = p;
		}
	}

	// ======================================================================== GALACTIC SMASH

	/** A cosmic giant tears the sky open, hunts the city with searchlight eyes, winds up and punches the ground. */
	public static class GalacticSmash extends Cine {
		static final int PUNCH_START = 440, IMPACT = 460, SPACE = 560, AERIAL = 660;
		private static final double BONE = 190;
		private final Vec3d base, rift, spaceC, impN;
		private final List<Vec3d> riftLine = new ArrayList<>();
		private final List<Vec3d> crackDirs = new ArrayList<>();

		GalacticSmash(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.base = anchor.add(fwd.multiply(230)).add(0, 300, 0);
			this.rift = anchor.add(fwd.multiply(430)).add(0, 330, 0);
			this.spaceC = anchor.add(0, 3300, 0);
			this.impN = new Vec3d(Math.cos(0.6), 0.6, Math.sin(0.6)).normalize();
			for (int i = 0; i <= 18; i++) {
				double u = i / 18.0;
				riftLine.add(new Vec3d((Solids.hash(i, 77) - 0.5) * 36, (u - 0.5) * 520, 0));
			}
			Vec3d[] b = ArsenalEffects.basis(impN);
			for (int k = 0; k < 12; k++) {
				double ang = k * MathHelper.TAU / 12 + Solids.hash(k, 9) * 0.4;
				crackDirs.add(b[0].multiply(Math.cos(ang)).add(b[1].multiply(Math.sin(ang))));
			}
		}

		// --- body kinematics ------------------------------------------------------------

		private double emerge(float a) {
			return ease((a - 30) / 110.0);
		}

		private double twist(float a) {
			double w = ease((a - 330) / 100.0);
			double p = clamp01((a - PUNCH_START) / (double) (IMPACT - PUNCH_START));
			return -0.42 * w + 0.7 * p * p;
		}

		private Vec3d shoulderMid(float a) {
			double back = (1 - emerge(a)) * 200;
			double lean = clamp01((a - PUNCH_START) / (double) (IMPACT - PUNCH_START));
			return base.add(fwd.multiply(back - lean * lean * 45)).add(0, -lean * 25, 0);
		}

		private Vec3d rightT(float a) {
			double tw = twist(a);
			return right.multiply(Math.cos(tw)).add(fwd.multiply(Math.sin(tw)));
		}

		private Vec3d rs(float a) {
			return shoulderMid(a).add(rightT(a).multiply(78));
		}

		private Vec3d ls(float a) {
			return shoulderMid(a).subtract(rightT(a).multiply(78));
		}

		private Vec3d head(float a) {
			return shoulderMid(a).add(0, 122, 0);
		}

		private Vec3d hand(float a) {
			Vec3d sh = rs(a);
			Vec3d idle = sh.add(rightT(a).multiply(40)).add(fwd.multiply(-35)).add(0, -250 + Math.sin(a * 0.05) * 8, 0);
			Vec3d wind = sh.add(fwd.multiply(175)).add(rightT(a).multiply(60)).add(0, 165, 0);
			Vec3d tgt = anchor.add(0, 2, 0);
			if (a < 330) {
				return idle;
			}
			if (a < PUNCH_START) {
				double w = ease((a - 330) / 100.0);
				double tremble = w * 6;
				return lerp(idle, wind, w).add(Math.sin(a * 1.9) * tremble, Math.cos(a * 2.3) * tremble, Math.sin(a * 1.3) * tremble);
			}
			if (a < IMPACT) {
				double p = (a - PUNCH_START) / (double) (IMPACT - PUNCH_START);
				return lerp(wind, tgt, p * p * p);
			}
			// Fist stays buried, then slowly pulls free as the giant dissolves.
			return tgt.add(0, ease((a - 600) / 100.0) * 120, 0);
		}

		private Vec3d headDir(float a) {
			Vec3d face = fwd.multiply(-1);
			double yaw = 0, pitch = -0.12;
			if (a > 120 && a < 280) {
				double k = ease((a - 120) / 20.0) * (1 - ease((a - 250) / 30.0));
				yaw = Math.sin((a - 120) / 150.0 * Math.PI * 2.0) * 1.0 * k;
				pitch = -0.12 - 0.42 * k;
			}
			if (a >= 270) {
				pitch = -0.12 - 0.45 * ease((a - 270) / 30.0);
			}
			Vec3d dir = face.multiply(Math.cos(yaw)).add(right.multiply(Math.sin(yaw)));
			return new Vec3d(dir.x * Math.cos(pitch), Math.sin(pitch), dir.z * Math.cos(pitch)).normalize();
		}

		// --- camera -----------------------------------------------------------------------

		@Override
		protected void direct(float a, State s) {
			Vec3d head = head(a);
			if (a < 130) {
				Vec3d p = anchor.add(fwd.multiply(-(25 + a * 0.1))).add(0, 2.5, 0);
				aim(s, p, lerp(rift, head, ease((a - 50) / 80.0)), 78, false);
			} else if (a < 270) {
				Vec3d p = anchor.add(right.multiply(430)).add(fwd.multiply(40 + (a - 130) * 0.6)).add(0, 160, 0);
				aim(s, p, head.add(0, -80, 0), 52, false);
			} else if (a < 330) {
				double d = lerp(140, 88, ease((a - 270) / 60.0));
				aim(s, head.add(headDir(a).multiply(d)).add(right.multiply(26)).add(0, 4, 0), head, 42, false);
			} else if (a < PUNCH_START) {
				Vec3d h = hand(a);
				double ang = (a - 330) * 0.011;
				Vec3d p = h.add(right.multiply(Math.cos(ang) * 155)).add(fwd.multiply(-Math.sin(ang) * 165 - 40)).add(0, -75, 0);
				aim(s, p, h, 70, false);
			} else if (a < IMPACT) {
				Vec3d h = hand(a);
				aim(s, h.add(right.multiply(80)).add(fwd.multiply(-55)).add(0, 30, 0), h, (float) lerp(85, 110, (a - PUNCH_START) / 20.0), false);
			} else if (a < SPACE) {
				double u = (a - IMPACT) / (double) (SPACE - IMPACT);
				aim(s, anchor.add(fwd.multiply(-200 + u * 30)).add(right.multiply(55)).add(0, 14 + u * 10, 0), anchor.add(0, 25, 0), 75, false);
			} else if (a < AERIAL) {
				double u = (a - SPACE) / (double) (AERIAL - SPACE);
				double ang = 0.6 + u * 0.35;
				Vec3d p = spaceC.add(Math.cos(ang) * lerp(260, 215, u), lerp(110, 90, u), Math.sin(ang) * lerp(260, 215, u));
				aim(s, p, spaceC, 60, true);
			} else {
				double u = ease((a - AERIAL) / (lifetime - AERIAL));
				aim(s, anchor.add(fwd.multiply(-140)).add(right.multiply(40)).add(0, 150 + 70 * u, 0), lerp(anchor, head(a), 0.35 * u), 72, false);
			}
		}

		@Override
		protected float handheld(float a) {
			if (a < 130) {
				return 0.25F;
			}
			if (a >= 330 && a < PUNCH_START) {
				return 0.3F + (a - 330) * 0.008F;
			}
			if (a >= PUNCH_START && a < IMPACT) {
				return 1.4F;
			}
			if (a >= IMPACT && a < SPACE) {
				return 2.6F * (1.0F - (a - IMPACT) / (SPACE - IMPACT)) + 0.4F;
			}
			if (a >= SPACE && a < AERIAL) {
				return 0.9F;
			}
			return 0.2F;
		}

		@Override
		public Caption caption(float a) {
			Caption c;
			if ((c = cap(a, 12, 115, "THE SKY IS TEARING OPEN", "something is coming through", 0xC8B8FF)) != null) {
				return c;
			}
			if ((c = cap(a, 145, 255, "IT IS LOOKING FOR US", "", 0xFFFFFF)) != null) {
				return c;
			}
			if ((c = cap(a, 285, 328, "IT FOUND US", "", 0xFF4060)) != null) {
				return c;
			}
			if ((c = cap(a, 345, 432, "GALACTIC SMASH", "brace for impact", 0xFFD080)) != null) {
				return c;
			}
			return cap(a, SPACE + 8, AERIAL - 6, "THE WHOLE EARTH TREMBLES", "", 0xFFFFFF);
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x0C0A24, Math.min(0.9F, age / 60.0F));
			if (age >= 330 && age < IMPACT) {
				// Cosmic energy streams into the fist.
				Vec3d h = hand(age);
				for (int i = 0; i < 10; i++) {
					Vec3d d = sphereDir(age * 10 + i, 3).multiply(160);
					Vec3d p = h.add(d);
					world.addImportantParticle(ModParticles.PLASMA_SPARK, true, p.x, p.y, p.z, -d.x * 0.06, -d.y * 0.06, -d.z * 0.06);
				}
			}
			if (age >= IMPACT && age < IMPACT + 40) {
				for (int i = 0; i < 40; i++) {
					double ang = world.random.nextDouble() * MathHelper.TAU;
					double r = (age - IMPACT) * 7 + world.random.nextDouble() * 20;
					world.addImportantParticle(ModParticles.SHOCK_DUST, true, anchor.x + Math.cos(ang) * r, anchor.y + 1, anchor.z + Math.sin(ang) * r,
							Math.cos(ang) * 1.5, 0.8, Math.sin(ang) * 1.5);
				}
			}
			if (age >= 680 && age < 730) {
				Vec3d sm = shoulderMid(age);
				for (int i = 0; i < 24; i++) {
					Vec3d p = sm.add(world.random.nextGaussian() * 70, -world.random.nextDouble() * 220 + 120, world.random.nextGaussian() * 70);
					world.addImportantParticle(ParticleTypes.END_ROD, true, p.x, p.y, p.z, world.random.nextGaussian() * 0.8, world.random.nextGaussian() * 0.8, world.random.nextGaussian() * 0.8);
				}
			}
			return super.tick(world);
		}

		// --- scenery ----------------------------------------------------------------------

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			if (a >= SPACE && a < AERIAL) {
				renderOrbit(solid, glow, shade, m, cam, cb, a);
				return;
			}
			renderRift(solid, glow, m, cam, a);
			renderGiant(solid, glow, shade, m, cam, cb, a);
			if (a >= IMPACT) {
				float t = a - IMPACT;
				Vec3d g = anchor.subtract(cam).add(0, 0.6, 0);
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				for (int k = 0; k < 4; k++) {
					double rr = t * (9 - k) - k * 14;
					if (rr > 2) {
						FxDraw.ring(glow, m, g, x, z, rr, rr + 7 + k * 3, k % 2 == 0 ? 0xFFFFFF : 0xFFB070, 0.0F, 0.8F * (1 - Math.min(1, t / 120)), 0.0F, 96);
					}
				}
				FxDraw.beam(glow, m, g, g.add(0, 400, 0), Vec3d.ZERO, 14 * (1 - Math.min(1, t / 90)), 0xFFE0B0, 0.6F);
				FxDraw.disc(shade, m, g, x, z, 60 + t * 3, 0x6A5A50, 0.5F * (1 - Math.min(1, t / 140)), 0.0F, 40);
			}
		}

		private void renderRift(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d cam, float a) {
			double open = ease((a - 8) / 40.0) * (1 - ease((a - 200) / 80.0));
			if (open <= 0.01) {
				return;
			}
			Vec3d c = rift.subtract(cam);
			int n = riftLine.size();
			Vec3d prevL = null, prevR = null, prevC = null;
			for (int i = 0; i < n; i++) {
				Vec3d lp = riftLine.get(i);
				double lens = Math.sin(Math.PI * i / (n - 1));
				Vec3d centre = c.add(right.multiply(lp.x)).add(0, lp.y * open, 0);
				double w = 70 * lens * open;
				Vec3d L = centre.subtract(right.multiply(w)), R = centre.add(right.multiply(w));
				if (prevL != null) {
					Solids.quad(solid, m, prevL, prevR, R, L, 0x07020F, 1.0F);
					FxDraw.ribbon(glow, m, prevL, L, Vec3d.ZERO, 8, 0xC070FF, 0.9F, 0.9F);
					FxDraw.ribbon(glow, m, prevR, R, Vec3d.ZERO, 8, 0xC070FF, 0.9F, 0.9F);
					FxDraw.ribbon(glow, m, prevL, L, Vec3d.ZERO, 40, 0x8040FF, 0.25F, 0.25F);
					FxDraw.ribbon(glow, m, prevR, R, Vec3d.ZERO, 40, 0x8040FF, 0.25F, 0.25F);
				}
				prevL = L;
				prevR = R;
				prevC = centre;
			}
		}

		private void renderGiant(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d cam, Vec3d[] cb, float a) {
			if (a > 730) {
				return;
			}
			float fadeIn = (float) emerge(a);
			float fadeOut = 1.0F - (float) clamp01((a - 680) / 50.0);
			float env = fadeIn * fadeOut;
			boolean body = fadeIn > 0.25 && a < 690;
			Vec3d off = cam.multiply(-1);
			Vec3d S = shoulderMid(a).add(off), RS = rs(a).add(off), LS = ls(a).add(off), H = head(a).add(off);
			Vec3d rt = rightT(a);
			Vec3d P = S.add(0, -240, 0), N = S.add(0, 40, 0);
			Vec3d handR = hand(a).add(off);
			Vec3d elbowR = elbow(RS, handR, BONE, BONE, fwd.multiply(0.7).add(rt.multiply(0.9)).add(0, 0.2, 0));
			Vec3d handL = LS.add(rt.multiply(-40)).add(fwd.multiply(-35)).add(0, -250 + Math.sin(a * 0.04) * 8, 0);
			Vec3d elbowL = elbow(LS, handL, BONE, BONE, rt.multiply(-1).add(fwd.multiply(0.4)));
			Vec3d hd = headDir(a);
			Vec3d punchDir = handR.subtract(elbowR).normalize();
			int skin = 0x1E1848, limb = 0x2A2060, plate = 0x3A2C78;

			if (body) {
				Solids.tube(solid, m, N, P, 72, 50, 16, skin, 1.0F, true);
				Solids.tube(solid, m, N.add(0, -30, 0), N.add(0, -120, 0), 80, 70, 16, plate, 1.0F, true);
				Solids.tube(solid, m, N, N.add(0, 32, 0), 26, 22, 10, skin, 1.0F, true);
				Solids.sphere(solid, m, H, 44, limb, 14, 22, true);
				Solids.ellipsoid(solid, m, RS.add(0, 8, 0), 52, 34, 52, 8, 14, (i, j, n) -> plate, true, 1.0F);
				Solids.ellipsoid(solid, m, LS.add(0, 8, 0), 52, 34, 52, 8, 14, (i, j, n) -> plate, true, 1.0F);
				Solids.tube(solid, m, RS, elbowR, 32, 27, 12, limb, 1.0F, true);
				Solids.tube(solid, m, elbowR, handR, 27, 25, 12, limb, 1.0F, true);
				Solids.sphere(solid, m, elbowR, 30, plate, 8, 12, true);
				Solids.sphere(solid, m, handR, 50, plate, 12, 18, true);
				Vec3d[] kb = ArsenalEffects.basis(punchDir);
				for (int k = 0; k < 4; k++) {
					double s = (k - 1.5) * 22;
					Solids.sphere(solid, m, handR.add(punchDir.multiply(38)).add(kb[0].multiply(s)), 17, 0x463290, 6, 10, true);
				}
				Solids.tube(solid, m, LS, elbowL, 32, 27, 12, limb, 1.0F, true);
				Solids.tube(solid, m, elbowL, handL, 27, 25, 12, limb, 1.0F, true);
				Solids.sphere(solid, m, handL, 44, plate, 10, 16, true);
			}
			float rim = 0.25F * env;
			Solids.tube(glow, m, N, P, 76, 54, 12, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, RS, elbowR, 35, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowR, handR, 31, 28, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, LS, elbowL, 35, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowL, handL, 31, 28, 8, 0x7A5CFF, rim, false);
			// Galaxy skin: twinkling stars and a constellation drawn across the chest.
			Vec3d[] tb = ArsenalEffects.basis(P.subtract(N));
			Vec3d prevStar = null;
			for (int i = 0; i < 46; i++) {
				double u = 0.1 + 0.8 * Solids.hash(i, 21);
				double ang = Solids.hash(i, 22) * MathHelper.TAU;
				double rad = lerp(72, 50, u) * 1.02;
				Vec3d p = N.add(P.subtract(N).multiply(u)).add(tb[0].multiply(Math.cos(ang) * rad)).add(tb[1].multiply(Math.sin(ang) * rad));
				float tw = 0.5F + 0.5F * MathHelper.sin(a * 0.2F + i);
				Solids.dot(glow, m, p, cb, 2.5 + 3 * Solids.hash(i, 23), i % 5 == 0 ? 0xFFD8A0 : 0xFFFFFF, tw * env);
				if (i < 9) {
					if (prevStar != null) {
						FxDraw.ribbon(glow, m, prevStar, p, Vec3d.ZERO, 1.2, 0xB0C8FF, 0.5F * env, 0.5F * env);
					}
					prevStar = p;
				}
			}
			// Chest core.
			Vec3d core = N.add(0, -80, 0).add(fwd.multiply(-72));
			float pulse = 0.6F + 0.4F * MathHelper.sin(a * 0.15F);
			Solids.dot(glow, m, core, cb, 30, 0xFFFFFF, pulse * env);
			Solids.dot(glow, m, core, cb, 90, 0x9A70FF, 0.5F * pulse * env);
			Solids.dot(glow, m, H, cb, 90, 0x7A5CFF, 0.2F * env);
			Solids.dot(glow, m, handR, cb, 120, a > 400 ? 0xFFE0A0 : 0x9A7CFF, (0.2F + 0.6F * (float) clamp01((a - 330) / 100.0)) * env);
			// Eyes, searchlights and the lock-on laser.
			Vec3d sideDir = new Vec3d(-hd.z, 0, hd.x).normalize();
			int eyeCol = a > 270 ? 0xFFFFFF : 0x40F8FF;
			for (int e = -1; e <= 1; e += 2) {
				Vec3d eye = H.add(hd.multiply(40)).add(sideDir.multiply(e * 17)).add(0, 6, 0);
				Solids.dot(glow, m, eye, cb, 9, eyeCol, env);
				Solids.dot(glow, m, eye, cb, 34, eyeCol, 0.4F * env);
				if (a > 130 && a < 270 && hd.y < -0.05) {
					double t = (anchor.y - cam.y - eye.y) / hd.y;
					Vec3d spot = eye.add(hd.multiply(Math.max(0, t)));
					float k = (float) (ease((a - 130) / 15.0) * (1 - ease((a - 255) / 15.0)));
					Solids.tube(glow, m, eye, spot, 3, 28, 12, 0x60F0FF, 0.16F * k * env, false);
					FxDraw.disc(glow, m, spot.add(0, 0.6, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 32, 0xA0FFFF, 0.7F * k, 0.0F, 24);
				}
				if (a > 285 && a < IMPACT) {
					float f = (float) clamp01((a - 285) / 30.0);
					FxDraw.ribbon(glow, m, eye, anchor.subtract(cam).add(0, 5, 0), Vec3d.ZERO, 1.4, 0xFF4060, 0.0F, 0.6F * f);
				}
			}
			// Halo.
			float spin = a * 0.03F;
			Vec3d u = new Vec3d(Math.cos(spin), 0, Math.sin(spin)), v = new Vec3d(-Math.sin(spin) * 0.3, 0.95, Math.cos(spin) * 0.3).normalize();
			FxDraw.ring(glow, m, H.add(0, 22, 0), u, v, 94, 102, ArsenalEffects.rainbow(a * 0.01F), 0.0F, 0.8F * env, 0.0F, 64);
			// Nebula mist where the legs would be.
			FxDraw.disc(shade, m, P.add(0, -10, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 160, 0xB0A8FF, 0.45F * env, 0.0F, 32);
			FxDraw.disc(glow, m, P.add(0, -20, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 220, 0x6040C0, 0.2F * env, 0.0F, 32);
			// Wind-up energy rings and the punch's sonic-boom trail.
			if (a > 330 && a < IMPACT + 10) {
				float w = (float) clamp01((a - 330) / 100.0);
				for (int k = 0; k < 3; k++) {
					double rr = 150 - k * 32 - w * 40;
					Vec3d ax = fwd.multiply(Math.cos(a * 0.15 + k)).add(right.multiply(Math.sin(a * 0.15 + k)));
					FxDraw.ring(glow, m, handR, ax, new Vec3d(0, 1, 0), rr * 0.9, rr, k == 1 ? 0xFFFFFF : 0xFFB070, 0.0F, 0.5F * w, 0.0F, 40);
				}
			}
			if (a > PUNCH_START && a < IMPACT + 25) {
				Vec3d prev = null;
				for (int k = 0; k < 12; k++) {
					Vec3d p = hand(a - k * 1.4F).add(off);
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 60 * (1 - k / 12.0), k % 2 == 0 ? 0xFFFFFF : 0xFFB070, 0.7F * (1 - k / 12F), 0.7F * (1 - (k + 1) / 12F));
					}
					prev = p;
				}
				for (int k = 1; k <= 3; k++) {
					Vec3d p = hand(a - k * 4F).add(off);
					FxDraw.ring(glow, m, p, ArsenalEffects.basis(punchDir)[0], ArsenalEffects.basis(punchDir)[1], 50 + k * 22, 58 + k * 22, 0xFFFFFF, 0.0F, 0.5F / k, 0.0F, 40);
				}
			}
		}

		private void renderOrbit(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d cam, Vec3d[] cb, float a) {
			Solids.stars(glow, m, cb, a, 1.0F);
			double R = 90;
			Vec3d c = spaceC.subtract(cam);
			earth(solid, glow, shade, m, c, R, 0.0);
			double u = (a - SPACE) / (double) (AERIAL - SPACE);
			Vec3d hit = c.add(impN.multiply(R * 1.01));
			Solids.dot(glow, m, hit, cb, 10 + 30 * (1 - u), 0xFFFFFF, 1.0F);
			Solids.dot(glow, m, hit, cb, 40, 0xFFB070, 0.6F);
			for (int k = 0; k < 3; k++) {
				double theta = Math.max(0, u * Math.PI * 0.95 - k * 0.12);
				if (theta > 0.01) {
					sphereRing(glow, m, c, R, impN, theta, 2.5 - k * 0.6, k == 0 ? 0xFFFFFF : 0xFFB070, 0.9F - k * 0.25F);
				}
			}
			for (Vec3d d : crackDirs) {
				Vec3d prev = null;
				int n = (int) (14 * clamp01(u * 1.6));
				for (int i = 0; i <= n; i++) {
					double th = i * 0.06;
					Vec3d p = c.add(impN.multiply(Math.cos(th)).add(d.multiply(Math.sin(th))).multiply(R * 1.004));
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.8, 0xFF8030, 0.9F, 0.9F);
					}
					prev = p;
				}
			}
		}
	}

	// ======================================================================== BOOMING TICK

	/** Camera spirals up over the carpet-bombed site, cuts to orbit, and Earth cracks open and explodes. */
	public static class BoomSurvey extends Cine {
		static final int SPACE_AT = 200;
		private final Vec3d sa, moon;
		private final List<Vec3d[]> cracks = new ArrayList<>();
		private final Vec3d[] shardDir = new Vec3d[80];
		private final double[] shardSpeed = new double[80];

		BoomSurvey(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.sa = anchor.add(0, 3300, 0);
			this.moon = sa.add(-170, 60, 140);
			for (int k = 0; k < 20; k++) {
				Vec3d d = sphereDir(k, 0);
				Vec3d[] pts = new Vec3d[16];
				for (int i = 0; i < pts.length; i++) {
					pts[i] = d;
					d = d.add(sphereDir(k * 31 + i, 7).multiply(0.22)).normalize();
				}
				cracks.add(pts);
			}
			for (int i = 0; i < shardDir.length; i++) {
				shardDir[i] = sphereDir(i, 99);
				shardSpeed[i] = 2.0 + Solids.hash(i, 5) * 5.0;
			}
		}

		@Override
		protected void direct(float a, State s) {
			if (a < SPACE_AT) {
				double t = ease(a / SPACE_AT);
				double ang = Math.atan2(-fwd.z, -fwd.x) + a * 0.009;
				double dist = lerp(50, 210, t);
				Vec3d p = anchor.add(Math.cos(ang) * dist, lerp(8, 330, t), Math.sin(ang) * dist);
				aim(s, p, anchor, (float) lerp(78, 92, t), false);
			} else {
				double u = a - SPACE_AT;
				double ang = 0.5 + u * 0.004;
				double dist = lerp(250, 205, clamp01(u / 140.0));
				aim(s, sa.add(Math.cos(ang) * dist, 45 - u * 0.1, Math.sin(ang) * dist), sa, 62, true);
			}
		}

		@Override
		protected float handheld(float a) {
			if (a < SPACE_AT) {
				return 1.1F;
			}
			float u = a - SPACE_AT;
			return u < 100 ? 0.15F : u < 140 ? 0.15F + (u - 100) * 0.03F : 3.0F;
		}

		@Override
		public Caption caption(float a) {
			Caption c;
			if ((c = cap(a, 10, 110, "EVERYTHING YOU BUILT", "", 0xFFFFFF)) != null) {
				return c;
			}
			if ((c = cap(a, 115, 195, "IS GONE", "", 0xFF9050)) != null) {
				return c;
			}
			if ((c = cap(a, SPACE_AT + 20, SPACE_AT + 110, "AND THEN...", "", 0xFFFFFF)) != null) {
				return c;
			}
			return cap(a, SPACE_AT + 118, SPACE_AT + 175, "THE EARTH", "", 0xFF4030);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= SPACE_AT) {
				ClientFx.tintSky(0x000006, 1.0F);
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
			Vec3d mc = moon.subtract(cam);
			if (u < 160) {
				Solids.sphere(solid, m, mc, 24, 0xB8B8C0, 12, 20, true);
			}
			if (u < 140) {
				earth(solid, glow, shade, m, c, R, a * 0.004);
				float prog = (float) clamp01((u - 60) / 60.0);
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
					Solids.dot(glow, m, c, cb, R * 1.4 * prog, 0xFF7020, 0.25F * prog);
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
				if (e < 20) {
					Solids.sphere(solid, m, mc, 24, 0xFFE0C0, 12, 20, false);
				}
			}
		}
	}

	// ======================================================================== ALIEN LAUNCH

	/** A visit to the alien world: the planet-killer is fuelled, armed and fired at Earth. */
	public static class AlienLaunch extends Cine {
		private static final double PR = 120, LEN = 70, RR = 6.5;
		private final Vec3d c, base, earth;

		AlienLaunch(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.c = anchor.add(0, 3700, 0);
			this.base = c.add(0, PR, 0);
			this.earth = c.add(300, 560, 0);
		}

		private Vec3d rocketCentre(float a) {
			Vec3d c0 = base.add(0, LEN / 2, 0);
			if (a < 280) {
				return c0;
			}
			double u = clamp01((a - 280) / 200.0);
			u = u * u;
			Vec3d ctrl = c0.add(0, 240, 0);
			return c0.multiply((1 - u) * (1 - u)).add(ctrl.multiply(2 * (1 - u) * u)).add(earth.multiply(u * u));
		}

		private Vec3d rocketDir(float a) {
			if (a < 285) {
				return new Vec3d(0, 1, 0);
			}
			Vec3d d = rocketCentre(a + 1.5F).subtract(rocketCentre(a - 1.5F));
			return d.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : d.normalize();
		}

		@Override
		protected void direct(float a, State s) {
			Vec3d rc = rocketCentre(a);
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
		protected float handheld(float a) {
			if (a >= 240 && a < 330) {
				return 0.6F + (a - 240) * 0.02F;
			}
			if (a >= 400) {
				return 1.2F;
			}
			return 0.15F;
		}

		@Override
		public Caption caption(float a) {
			Caption c;
			if ((c = cap(a, 6, 104, "ZORAX PRIME", "meanwhile, 4.2 light-years away", 0xC090FF)) != null) {
				return c;
			}
			if ((c = cap(a, 160, 182, "3", "", 0x60FFA0)) != null) {
				return c;
			}
			if ((c = cap(a, 185, 207, "2", "", 0x60FFA0)) != null) {
				return c;
			}
			if ((c = cap(a, 210, 232, "1", "", 0x60FFA0)) != null) {
				return c;
			}
			if ((c = cap(a, 118, 158, "LOADING PLANET-KILLER", "fuel: dark matter", 0x60FFA0)) != null) {
				return c;
			}
			if ((c = cap(a, 240, 292, "IGNITION", "", 0xFFFFFF)) != null) {
				return c;
			}
			if ((c = cap(a, 308, 392, "TARGET ACQUIRED", "EARTH", 0xFF4060)) != null) {
				return c;
			}
			return cap(a, 402, lifetime - 4, "IMPACT IN 17 SECONDS", "", 0xFF4060);
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x000006, 1.0F);
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
			double spin = a * 0.002;
			Solids.ellipsoid(solid, m, pc, PR, PR, PR, 28, 56, rotated(Solids.bands(0x5A2A9A, 0x1E8C8C, 0xB03A8A, 7), spin), true, 1.0F);
			FxDraw.sphere(glow, m, pc, PR * 1.06, Vec3d.ZERO, 0xA070FF, 0.0F, 0.5F, 14, 28);
			// City lights on the night side.
			for (int i = 0; i < 160; i++) {
				Vec3d d = sphereDir(i, 61);
				d = new Vec3d(d.x * Math.cos(spin) - d.z * Math.sin(spin), d.y, d.x * Math.sin(spin) + d.z * Math.cos(spin));
				if (d.dotProduct(Solids.LIGHT) < -0.05) {
					Solids.dot(glow, m, pc.add(d.multiply(PR * 1.004)), cb, 1.2 + Solids.hash(i, 62) * 1.6, i % 3 == 0 ? 0x60FFA0 : 0xFFC860, 0.9F);
				}
			}
			Vec3d ringN = new Vec3d(0.25, 1, 0.1).normalize();
			Vec3d[] rb = ArsenalEffects.basis(ringN);
			FxDraw.ring(shade, m, pc, rb[0], rb[1], 165, 215, 0xC8A0FF, 0.0F, 0.35F, 0.0F, 96);
			FxDraw.ring(glow, m, pc, rb[0], rb[1], 190, 200, 0xFFFFFF, 0.0F, 0.3F, 0.0F, 96);
			for (int k = 0; k < 3; k++) {
				double ang = a * (0.008 + k * 0.004) + k * 2.1;
				Vec3d mp = pc.add(Math.cos(ang) * (270 + k * 50), 30 + Math.sin(k * 3) * 50, Math.sin(ang) * (270 + k * 50));
				Solids.sphere(solid, m, mp, 15 - k * 4, k == 1 ? 0x8A8A98 : 0xB8A8A0, 10, 16, true);
			}
			Vec3d bs = base.subtract(cam);
			Vec3d rcw = rocketCentre(a);
			Vec3d rdir = rocketDir(a);
			float flame = a < 240 ? 0 : (float) clamp01((a - 240) / 50.0);
			Solids.rocket(solid, glow, m, rcw.subtract(cam), rdir, LEN, RR, a, flame);
			// Searchlights sweeping the alien sky.
			for (int k = 0; k < 2; k++) {
				double ang = a * 0.03 + k * Math.PI;
				Vec3d from = bs.add((k == 0 ? -1 : 1) * 40, 0, 0);
				Vec3d dir = new Vec3d(Math.cos(ang) * 0.4, 1, Math.sin(ang) * 0.4).normalize();
				FxDraw.beam(glow, m, from, from.add(dir.multiply(300)), Vec3d.ZERO, 3, 0x60FFA0, 0.35F);
			}
			if (a < 285) {
				Solids.tube(solid, m, bs.add(0, -1, 0), bs.add(0, 1.5, 0), 28, 26, 16, 0x1A1424, 1.0F, true);
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d tbp = bs.add(sgn * 28, 0, 0);
					Solids.tube(solid, m, tbp, tbp.add(0, 74, 0), 2.4, 2.0, 6, 0x3A3A48, 1.0F, true);
					for (int k = 0; k < 4; k++) {
						double h = 14 + k * 15;
						Vec3d anchorPt = tbp.add(0, h, 0);
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
				Solids.dot(glow, m, bs.add(0, 2, 0), cb, 90 * Math.max(0.0, 1 - (a - 280) / 80.0), 0xC0FFE0, 0.7F);
			}
			if (a > 300) {
				Vec3d ec = earth.subtract(cam);
				double grow = 1.0 + clamp01((a - 360) / 120.0) * 0.35;
				Cinematics.earth(solid, glow, shade, m, ec, 70 * grow, a * 0.004);
			}
		}
	}
}
