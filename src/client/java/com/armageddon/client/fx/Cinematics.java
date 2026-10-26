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

	public static final int GALACTIC_SMASH = 1, BOOM_SURVEY = 2, ALIEN_LAUNCH = 3,
			ETERNAL_SAPLING = 4, SIXTEEN_CHUNKS = 5, TRUMAN_SHOW = 6, DISCO_BALL = 7;

	public static Cine create(int scene, Vec3d anchor, Vec3d heading, int life) {
		return switch (scene) {
			case GALACTIC_SMASH -> new GalacticSmash(anchor, heading, life);
			case BOOM_SURVEY -> new BoomSurvey(anchor, heading, life);
			case ETERNAL_SAPLING -> new EternalSaplingCine(anchor, heading, life);
			case SIXTEEN_CHUNKS -> new SixteenChunksCine(anchor, heading, life);
			case TRUMAN_SHOW -> new TrumanShowCine(anchor, heading, life);
			case DISCO_BALL -> new DiscoBallCine(anchor, heading, life);
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

		public Vec3d anchor() {
			return anchor;
		}

		/** Extra 2D overlay for this scene (broadcast graphics, counters...), drawn over the letterbox. */
		public void drawHud(net.minecraft.client.gui.DrawContext ctx, int w, int h, float a) {
		}

		/** Dip-to-black amount at this moment: fades in from black, out at the end, plus any scene cuts. */
		public float fade(float a) {
			return (float) Math.max(clamp01(1 - a / 12.0), clamp01((a - (lifetime - 10)) / 10.0));
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

	/** 1 at {@code at}, falling to 0 {@code half} ticks either side: a quick dip to black across a cut. */
	static float dip(float a, float at, float half) {
		return (float) clamp01(1 - Math.abs(a - at) / half);
	}

	/** A tumbling chunk of rock (a short square prism). */
	static void rock(VertexConsumer solid, Matrix4f m, Vec3d c, double size, double spin, int salt, int rgb) {
		Vec3d ax = new Vec3d(Math.cos(spin + salt), Math.sin(spin * 0.7 + salt * 2), Math.sin(spin * 1.3 + salt * 3)).normalize().multiply(size * 0.5);
		Solids.tube(solid, m, c.subtract(ax), c.add(ax), size * 0.62, size * 0.5, 5, rgb, 1.0F, true);
	}

	/**
	 * Ballistic debris thrown up from {@code g} (camera-relative), {@code t} ticks after launch: rocks with glowing
	 * ember trails that arc out and fall back. Deterministic, so every frame agrees.
	 */
	static void debris(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d g, float t, int count, double speed,
			double size, double gravity, int salt, int rockRgb, int ember) {
		if (t <= 0) {
			return;
		}
		for (int i = 0; i < count; i++) {
			Vec3d d = sphereDir(i, salt);
			double v = speed * (0.35 + 0.65 * Solids.hash(i, salt + 3));
			double vx = d.x * v, vz = d.z * v, vy = (Math.abs(d.y) * 0.8 + 0.35) * v * 1.4;
			double y = vy * t - 0.5 * gravity * t * t;
			if (y < -4 && t > 4) {
				continue;
			}
			Vec3d p = g.add(vx * t, y, vz * t);
			double sz = size * (0.4 + 0.9 * Solids.hash(i, salt + 5));
			rock(solid, m, p, sz, t * 0.12 * (1 + Solids.hash(i, salt + 6)), i, i % 4 == 0 ? Solids.scale(rockRgb, 0.7) : rockRgb);
			Vec3d vel = new Vec3d(vx, vy - gravity * t, vz);
			float hot = (float) Math.max(0.15, 1 - t / 90.0);
			FxDraw.beam(glow, m, p, p.subtract(vel.multiply(5)), Vec3d.ZERO, sz * 0.45, ember, 0.55F * hot);
		}
	}

	/** A rolling wall of dust: a ring of soft shade puffs at {@code radius} around {@code g} (camera-relative). */
	static void dustWall(VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d g, double radius, double height, int rgb, float alpha, int salt) {
		if (alpha <= 0.01) {
			return;
		}
		int n = 64;
		for (int k = 0; k < n; k++) {
			double ang = k * MathHelper.TAU / n + Solids.hash(k, salt) * 0.08;
			for (int j = 0; j < 3; j++) {
				double rr = radius * (0.94 + 0.1 * Solids.hash(k, salt + j));
				double h = height * (0.15 + 0.35 * j + 0.2 * Solids.hash(k, salt + 7 + j));
				Vec3d p = g.add(Math.cos(ang) * rr, h, Math.sin(ang) * rr);
				Solids.dot(shade, m, p, cb, height * (0.45 - j * 0.1), Solids.scale(rgb, 0.85 + 0.15 * j), alpha * (1 - j * 0.25F));
			}
		}
	}

	/** Branching crack lines of light radiating over the ground from {@code g} (camera-relative). */
	static void groundCracks(VertexConsumer glow, Matrix4f m, Vec3d g, double reach, int count, int salt, int rgb, float alpha) {
		for (int k = 0; k < count; k++) {
			double ang = k * MathHelper.TAU / count + Solids.hash(k, salt) * 0.3;
			Vec3d prev = g;
			int segs = 10;
			for (int i = 1; i <= segs; i++) {
				ang += (Solids.hash(k * 31 + i, salt + 1) - 0.5) * 0.5;
				double r = reach * i / segs;
				Vec3d p = g.add(Math.cos(ang) * r, 0.4, Math.sin(ang) * r);
				float f = alpha * (1 - (float) i / (segs + 2));
				FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, Math.max(0.8, reach * 0.02 * (1 - i / (double) segs)), rgb, f, f);
				prev = p;
			}
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
		// Streaky cloud bands that drift a little faster than the ground.
		for (int k = 0; k < 40; k++) {
			Vec3d d = sphereDir(k, 47);
			d = new Vec3d(d.x, d.y * 0.7, d.z).normalize();
			double ang = spin * 1.5 + k * 0.01;
			d = new Vec3d(d.x * Math.cos(ang) - d.z * Math.sin(ang), d.y, d.x * Math.sin(ang) + d.z * Math.cos(ang));
			Vec3d[] b = ArsenalEffects.basis(d);
			Vec3d along = new Vec3d(-d.z, 0, d.x);
			along = along.lengthSquared() < 1.0E-6 ? b[0] : along.normalize();
			Vec3d across = d.crossProduct(along).normalize();
			double rr = r * (0.05 + Solids.hash(k, 48) * 0.06);
			Vec3d p = c.add(d.multiply(r * 1.02));
			FxDraw.disc(shade, m, p.add(along.multiply(rr * 0.9)), along.multiply(1.8), across, rr, 0xFFFFFF, 0.4F, 0.0F, 10);
			FxDraw.disc(shade, m, p.subtract(along.multiply(rr * 0.9)), along.multiply(1.6), across, rr * 0.8, 0xFFFFFF, 0.35F, 0.0F, 10);
		}
		// City lights on the night side: sampled in the planet's own frame so they turn with it.
		double cs = Math.cos(spin), sn = Math.sin(spin);
		Vec3d toCam = c.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : c.multiply(-1).normalize();
		Vec3d[] vb = ArsenalEffects.basis(toCam);
		for (int k = 0; k < 260; k++) {
			Vec3d bd = sphereDir(k, 53);
			if (!Solids.isLand(bd)) {
				continue;
			}
			Vec3d wd = new Vec3d(bd.x * cs + bd.z * sn, bd.y, -bd.x * sn + bd.z * cs);
			double night = -wd.dotProduct(Solids.LIGHT);
			if (night < 0.05 || wd.dotProduct(toCam) < 0.05) {
				continue;
			}
			float a = (float) Math.min(1.0, (night - 0.05) * 5.0);
			Solids.dot(glow, m, c.add(wd.multiply(r * 1.004)), vb, r * (0.006 + Solids.hash(k, 54) * 0.008), Solids.hash(k, 55) < 0.25 ? 0xFFF0C0 : 0xFFB050, 0.9F * a);
		}
		// Twilight along the terminator, the sun glinting off the ocean and a layered atmosphere.
		sphereRing(glow, m, c, r, Solids.LIGHT, Math.PI / 2 + 0.06, r * 0.035, 0xFF8A40, 0.22F);
		sphereRing(glow, m, c, r, Solids.LIGHT, Math.PI / 2 - 0.02, r * 0.02, 0xFFD0A0, 0.18F);
		Vec3d glint = Solids.LIGHT.add(toCam).normalize();
		if (glint.dotProduct(toCam) > 0.1) {
			Vec3d gp = c.add(glint.multiply(r * 1.003));
			Solids.dot(glow, m, gp, vb, r * 0.05, 0xFFFFFF, 0.45F);
			Solids.dot(glow, m, gp, vb, r * 0.16, 0xFFE8C0, 0.14F);
		}
		FxDraw.sphere(glow, m, c, r * 1.07, Vec3d.ZERO, 0x6AA8FF, 0.0F, 0.55F, 14, 28);
		FxDraw.sphere(glow, m, c, r * 1.022, Vec3d.ZERO, 0xBFE4FF, 0.0F, 0.4F, 14, 28);
		Solids.dot(glow, m, c.add(Solids.LIGHT.multiply(r * 0.6)), vb, r * 1.25, 0x7AB8FF, 0.07F);
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
				// Worm's-eye from the streets: the giant towers over the skyline while its eyes sweep the city.
				// (Ground shots stay near the anchor: terrain only exists, and is only drawn, around the player.)
				double u = (a - 130) / 140.0;
				Vec3d p = anchor.add(right.multiply(lerp(120, 85, u))).add(fwd.multiply(lerp(-50, -20, u))).add(0, lerp(4, 18, u), 0);
				aim(s, p, head.add(0, -60, 0), (float) lerp(74, 64, u), false);
			} else if (a < 330) {
				double d = lerp(140, 88, ease((a - 270) / 60.0));
				aim(s, head.add(headDir(a).multiply(d)).add(right.multiply(26)).add(0, 4, 0), head, 42, false);
			} else if (a < PUNCH_START) {
				// From behind the city: the fist rises over the giant's shoulder and the camera slowly pushes in.
				double u = (a - 330) / (double) (PUNCH_START - 330);
				Vec3d p = anchor.add(fwd.multiply(lerp(-75, -42, ease(u)))).add(right.multiply(lerp(-62, -36, u))).add(0, lerp(10, 24, u), 0);
				aim(s, p, lerp(head, hand(a), 0.55 + 0.45 * ease(u)), (float) lerp(70, 56, ease(u)), false);
			} else if (a < IMPACT) {
				// Low and close to ground zero, staring up as the fist comes down at the lens.
				Vec3d p = anchor.add(right.multiply(48)).add(fwd.multiply(-38)).add(0, 6, 0);
				aim(s, p, hand(a), (float) lerp(78, 106, ease((a - PUNCH_START) / 20.0)), false);
			} else if (a < SPACE) {
				// Crane up and back out of the blast while the shockwave rolls outward.
				double u = ease((a - IMPACT) / (double) (SPACE - IMPACT));
				Vec3d p = anchor.add(fwd.multiply(lerp(-55, -115, u))).add(right.multiply(lerp(48, 72, u))).add(0, lerp(7, 75, u), 0);
				aim(s, p, anchor.add(0, lerp(35, 8, u), 0), (float) lerp(92, 72, u), false);
			} else if (a < AERIAL) {
				double u = (a - SPACE) / (double) (AERIAL - SPACE);
				double ang = 0.6 + u * 0.35;
				Vec3d p = spaceC.add(Math.cos(ang) * lerp(260, 215, u), lerp(110, 90, u), Math.sin(ang) * lerp(260, 215, u));
				aim(s, p, spaceC, 60, true);
			} else {
				double u = ease((a - AERIAL) / (lifetime - AERIAL));
				aim(s, anchor.add(fwd.multiply(-95)).add(right.multiply(40)).add(0, 130 + 70 * u, 0), lerp(anchor, head(a), 0.35 * u), 72, false);
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
		public float fade(float a) {
			return Math.max(super.fade(a), Math.max(dip(a, SPACE, 7), dip(a, AERIAL, 7)));
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x0C0A24, Math.min(0.9F, age / 60.0F));
			// God rays: the rift, then the giant's chest core, then the charging fist, then the impact.
			if (age < 40) {
				ClientFx.light(rift, 0.7F, 0xC070FF);
			} else if (age < 330) {
				ClientFx.light(shoulderMid(age).add(0, -80, 0).add(fwd.multiply(-72)), 0.55F, 0x9A70FF);
			} else if (age < IMPACT) {
				ClientFx.light(hand(age), 0.5F + 0.9F * (float) clamp01((age - 330) / 130.0), 0xFFD090);
			} else if (age == IMPACT) {
				ClientFx.light(anchor.add(0, 40, 0), 1.5F, 0xFFE0B0);
			} else if (age >= SPACE && age < AERIAL) {
				ClientFx.light(spaceC.add(impN.multiply(91)), 1.0F, 0xFFB070);
			}
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
				if (t < 320) {
					// The city is thrown into the sky, the ground splits with light, and a wall of dust rolls out.
					debris(solid, glow, m, g, t, 110, 3.4, 8, 0.05, 11, 0x5A4C40, 0xFF9040);
					debris(solid, glow, m, g, t, 60, 5.0, 3, 0.05, 23, 0x6E5E50, 0xFFD080);
					float fadeK = 1 - Math.min(1, t / 160);
					dustWall(shade, m, cb, g, 30 + t * 5.5, 26 + t * 0.3, 0x8A7866, 0.75F * fadeK, 5);
					groundCracks(glow, m, g, Math.min(240, 20 + t * 6), 18, 3, 0xFF7020, 0.9F * (1 - Math.min(1, t / 260)));
				}
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
			// Lightning crawling off the rift's edges, re-rolled a few times a second.
			int roll = (int) (a * 0.35F);
			for (int k = 0; k < 7; k++) {
				if (Solids.hash(k, roll * 13) < 0.35) {
					continue;
				}
				int i = 2 + (int) (Solids.hash(k, roll * 7 + 1) * (n - 4));
				Vec3d lp = riftLine.get(i);
				double side = Solids.hash(k, roll + 3) < 0.5 ? -1 : 1;
				double w = 70 * Math.sin(Math.PI * i / (n - 1)) * open;
				Vec3d p = c.add(right.multiply(lp.x + side * w)).add(0, lp.y * open, 0);
				for (int j = 0; j < 6; j++) {
					Vec3d q = p.add(right.multiply(side * (14 + Solids.hash(k * 9 + j, roll) * 22))).add(0, (Solids.hash(k * 5 + j, roll + 9) - 0.5) * 40, 0)
							.add(fwd.multiply((Solids.hash(j, k + roll) - 0.5) * 20));
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 3.0 - j * 0.35, j % 2 == 0 ? 0xFFFFFF : 0xD0A0FF, (float) open, (float) open * 0.8F);
					p = q;
				}
			}
		}

		/** Void skin: near-black with a faint violet sheen and the odd dying star deep inside it. */
		private static Solids.Cell voidSkin(float a) {
			return (i, j, n) -> {
				double sheen = Math.max(0, Math.sin(n.x * 2.3 + n.y * 1.7 + a * 0.01)) * 0.5;
				int col = Solids.mix(0x040308, 0x140A24, sheen);
				return Solids.hash(i * 5 + 1, j * 7 + 3) > 0.985 ? Solids.mix(col, 0x8A90C0, 0.5 + 0.5 * Math.sin(a * 0.15 + i + j)) : col;
			};
		}

		/**
		 * Jagged starlight cracks across an armour plate: a few branching lines of white-blue light on a tube's surface
		 * from {@code a0} to {@code b0}, pulsing like something inside is trying to get out.
		 */
		private static void plateCracks(VertexConsumer glow, Matrix4f m, Vec3d a0, Vec3d b0, double r, int salt, int count, float alpha, float time) {
			Vec3d axis = b0.subtract(a0);
			Vec3d[] bs = ArsenalEffects.basis(axis);
			for (int k = 0; k < count; k++) {
				double ang = Solids.hash(k, salt) * MathHelper.TAU;
				double u = 0.1 + 0.8 * Solids.hash(k, salt + 1);
				float pulse = 0.55F + 0.45F * MathHelper.sin(time * 0.18F + k * 1.7F + salt);
				Vec3d prev = null;
				for (int s2 = 0; s2 <= 6; s2++) {
					ang += (Solids.hash(k * 13 + s2, salt + 2) - 0.5) * 0.55;
					u += (Solids.hash(k * 7 + s2, salt + 3) - 0.3) * 0.07;
					Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
					Vec3d p = a0.add(axis.multiply(MathHelper.clamp(u, 0.0, 1.0))).add(out.multiply(r * 1.01));
					if (prev != null) {
						float w = 1 - s2 / 7.0F;
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, r * 0.05 * w + 0.6, 0xD8ECFF, alpha * pulse, alpha * pulse * w);
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, r * 0.16 * w + 1.5, 0x6A9CFF, alpha * pulse * 0.25F, 0.0F);
					}
					prev = p;
				}
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
			int plate = 0x1A1C28, edge = 0x2A2E40;
			Solids.Cell skin = voidSkin(a);
			Vec3d[][] limbs = {{RS, elbowR}, {elbowR, handR}, {LS, elbowL}, {elbowL, handL}};
			float rage = (float) clamp01((a - 270) / 60.0);

			if (body) {
				renderCape(solid, glow, m, LS, RS, rt, a, env);
				// A faceless body of void.
				Solids.tube(solid, m, N, P, 72, 50, 16, 8, skin, true, 1.0F);
				Solids.tube(solid, m, N, N.add(0, 32, 0), 26, 22, 10, 2, skin, true, 1.0F);
				Solids.ellipsoid(solid, m, H, 42, 48, 42, 14, 22, skin, true, 1.0F);
				for (Vec3d[] seg : limbs) {
					Solids.tube(solid, m, seg[0], seg[1], 31, 26, 12, 6, skin, true, 1.0F);
				}
				// Cracked armour: a heavy breastplate in stacked plates, spiked pauldrons, vambraces and gauntlets.
				for (int k = 0; k < 4; k++) {
					Vec3d top = N.add(0, -28 - k * 24, 0), bot = N.add(0, -50 - k * 24, 0);
					double rr = lerp(82, 72, k / 3.0);
					Solids.tube(solid, m, top, bot, rr, rr - 2, 16, k % 2 == 0 ? plate : edge, 1.0F, true);
				}
				for (Vec3d sh : new Vec3d[]{RS, LS}) {
					Solids.ellipsoid(solid, m, sh.add(0, 8, 0), 54, 36, 54, 8, 14, (i, j, n) -> n.y < -0.2 ? edge : plate, true, 1.0F);
					Vec3d outSide = sh.subtract(S);
					outSide = new Vec3d(outSide.x, 0, outSide.z).normalize();
					for (int k = 0; k < 3; k++) {
						Vec3d root = sh.add(outSide.multiply(20 + k * 12)).add(0, 30 - k * 6, 0);
						Solids.tube(solid, m, root, root.add(outSide.multiply(24)).add(0, 46 - k * 10, 0), 9 - k, 0.5, 6, 0x0C0C14, 1.0F, true);
					}
				}
				for (Vec3d[] arm : new Vec3d[][]{{elbowR, handR}, {elbowL, handL}}) {
					Vec3d d = arm[1].subtract(arm[0]);
					Solids.tube(solid, m, arm[0].add(d.multiply(0.4)), arm[0].add(d.multiply(0.88)), 30, 29, 12, plate, 1.0F, true);
				}
				Solids.sphere(solid, m, elbowR, 31, edge, 8, 12, true);
				Solids.sphere(solid, m, elbowL, 31, edge, 8, 12, true);
				Solids.sphere(solid, m, handR, 50, plate, 12, 18, true);
				Solids.sphere(solid, m, handL, 44, plate, 10, 16, true);
				Vec3d[] kb = ArsenalEffects.basis(punchDir);
				for (int k = 0; k < 4; k++) {
					double sk = (k - 1.5) * 22;
					Vec3d kn = handR.add(punchDir.multiply(38)).add(kb[0].multiply(sk));
					Solids.sphere(solid, m, kn, 17, edge, 6, 10, true);
					Solids.tube(solid, m, kn, kn.add(punchDir.multiply(22)), 7, 0.5, 5, 0x0C0C14, 1.0F, true);
				}
				// Two great horns sweeping back off the smooth head.
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					Vec3d root = H.add(rt.multiply(sgn * 30)).add(0, 26, 0);
					Vec3d mid = root.add(rt.multiply(sgn * 40)).add(0, 50, 0).add(fwd.multiply(30));
					Vec3d tip = mid.add(rt.multiply(sgn * 10)).add(0, 70, 0).add(fwd.multiply(50));
					Solids.tube(solid, m, root, mid, 16, 10, 8, 0x0A0A12, 1.0F, true);
					Solids.tube(solid, m, mid, tip, 10, 0.5, 8, 0x0A0A12, 1.0F, true);
					Solids.dot(glow, m, tip, cb, 6, 0xFF6020, 0.8F * env);
				}
			}
			// A faint violet rim so the black silhouette separates from the night.
			float rim = 0.2F * env;
			Solids.tube(glow, m, N, P, 76, 54, 12, 0x6A50D0, rim, false);
			for (Vec3d[] seg : limbs) {
				Solids.tube(glow, m, seg[0], seg[1], 34, 29, 8, 0x6A50D0, rim, false);
			}
			// Starlight leaking through the cracks in the armour; it burns brighter as it gets angry.
			float crackA = env * (0.6F + 0.4F * rage);
			for (int k = 0; k < 4; k++) {
				Vec3d top = N.add(0, -28 - k * 24, 0), bot = N.add(0, -50 - k * 24, 0);
				plateCracks(glow, m, top, bot, lerp(82, 72, k / 3.0), 10 + k, 9, crackA, a);
			}
			for (int l = 0; l < limbs.length; l++) {
				plateCracks(glow, m, limbs[l][0], limbs[l][1], 30, 30 + l, 6, crackA, a);
			}
			plateCracks(glow, m, handR.subtract(punchDir.multiply(40)), handR.add(punchDir.multiply(40)), 48, 50, 8, crackA * (1 + rage), a);
			// Something inside it shows through the widest crack: a white-hot core behind the breastplate.
			Vec3d core = N.add(0, -80, 0).add(fwd.multiply(-74));
			float pulse = 0.6F + 0.4F * MathHelper.sin(a * 0.15F);
			Solids.dot(glow, m, core, cb, 18, 0xFFFFFF, pulse * env);
			Solids.dot(glow, m, core, cb, 70, 0x80B0FF, 0.35F * pulse * env);
			Solids.dot(glow, m, handR, cb, 120, a > 400 ? 0xFF8040 : 0x6A8CFF, (0.2F + 0.6F * (float) clamp01((a - 330) / 100.0)) * env);
			// Burning eyes in a faceless head: flames streaming up off them, searchlights, then the lock-on.
			Vec3d sideDir = new Vec3d(-hd.z, 0, hd.x).normalize();
			int eyeCol = a > 270 ? 0xFFD0A0 : 0xFF7020;
			for (int e = -1; e <= 1; e += 2) {
				Vec3d eye = H.add(hd.multiply(40)).add(sideDir.multiply(e * 15)).add(0, 8, 0);
				Solids.dot(glow, m, eye, cb, 8, 0xFFFFFF, env);
				Solids.dot(glow, m, eye, cb, 24, eyeCol, 0.85F * env);
				Solids.dot(glow, m, eye, cb, 60, 0xFF3010, 0.3F * env);
				for (int f = 0; f < 7; f++) {
					double life = (a * 0.04 + f / 7.0) % 1.0;
					Vec3d base = eye.add(sideDir.multiply((Solids.hash(f, e + 3) - 0.5) * 10));
					Vec3d lick = base.add(fwd.multiply(life * 30)).add(sideDir.multiply(e * life * 12)).add(0, 24 + life * 60, 0);
					FxDraw.ribbon(glow, m, base, lick, Vec3d.ZERO, 10 * (1 - life), life < 0.4 ? 0xFFB040 : 0xFF4010, 0.8F * (float) (1 - life) * env, 0.0F);
				}
				float flareK = a > 270 && a < IMPACT ? 1.0F : 0.5F;
				Solids.flare(glow, m, eye, cb, 26 * flareK + 10, eyeCol, 0.7F * env * flareK);
				if (a > 130 && a < 270 && hd.y < -0.05) {
					double t = (anchor.y - cam.y - eye.y) / hd.y;
					Vec3d spot = eye.add(hd.multiply(Math.max(0, t)));
					float k = (float) (ease((a - 130) / 15.0) * (1 - ease((a - 255) / 15.0)));
					Solids.tube(glow, m, eye, spot, 3, 28, 12, 0xFF6030, 0.16F * k * env, false);
					FxDraw.disc(glow, m, spot.add(0, 0.6, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 32, 0xFFA060, 0.7F * k, 0.0F, 24);
				}
				if (a > 285 && a < IMPACT) {
					float f = (float) clamp01((a - 285) / 30.0);
					FxDraw.ribbon(glow, m, eye, anchor.subtract(cam).add(0, 5, 0), Vec3d.ZERO, 1.6, 0xFF3010, 0.0F, 0.7F * f);
				}
			}
			// A broken halo of embers: a ring with pieces missing, turning slowly behind the head.
			float spin = a * 0.02F;
			Vec3d hu = new Vec3d(Math.cos(spin), 0, Math.sin(spin)), hv = new Vec3d(-Math.sin(spin) * 0.3, 0.95, Math.cos(spin) * 0.3).normalize();
			Vec3d haloC = H.add(0, 20, 0).add(fwd.multiply(30));
			for (int k = 0; k < 24; k++) {
				if (Solids.hash(k, 61) < 0.3) {
					continue;
				}
				double a0 = k * MathHelper.TAU / 24, a1 = (k + 0.8) * MathHelper.TAU / 24;
				Vec3d p0 = haloC.add(hu.multiply(Math.cos(a0) * 100)).add(hv.multiply(Math.sin(a0) * 100));
				Vec3d p1 = haloC.add(hu.multiply(Math.cos(a1) * 100)).add(hv.multiply(Math.sin(a1) * 100));
				FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 6, 0xFF5020, 0.7F * env, 0.7F * env);
				FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 18, 0xFF2000, 0.15F * env, 0.15F * env);
			}
			// Black smoke where the legs would be, lit red from below.
			FxDraw.disc(shade, m, P.add(0, -10, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 170, 0x0A0608, 0.7F * env, 0.0F, 32);
			FxDraw.disc(glow, m, P.add(0, -20, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 220, 0x601808, 0.25F * env, 0.0F, 32);
			// Wind-up: the void around the fist curdles into rings of fire, and the punch's sonic-boom trail.
			if (a > 330 && a < IMPACT + 10) {
				float w = (float) clamp01((a - 330) / 100.0);
				for (int k = 0; k < 3; k++) {
					double rr = 150 - k * 32 - w * 40;
					Vec3d ax = fwd.multiply(Math.cos(a * 0.15 + k)).add(right.multiply(Math.sin(a * 0.15 + k)));
					FxDraw.ring(glow, m, handR, ax, new Vec3d(0, 1, 0), rr * 0.9, rr, k == 1 ? 0xFFFFFF : 0xFF6020, 0.0F, 0.5F * w, 0.0F, 40);
				}
			}
			if (a > PUNCH_START && a < IMPACT + 25) {
				Vec3d prev = null;
				for (int k = 0; k < 12; k++) {
					Vec3d p = hand(a - k * 1.4F).add(off);
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 60 * (1 - k / 12.0), k % 2 == 0 ? 0xFFFFFF : 0xFF7030, 0.7F * (1 - k / 12F), 0.7F * (1 - (k + 1) / 12F));
					}
					prev = p;
				}
				for (int k = 1; k <= 3; k++) {
					Vec3d p = hand(a - k * 4F).add(off);
					FxDraw.ring(glow, m, p, ArsenalEffects.basis(punchDir)[0], ArsenalEffects.basis(punchDir)[1], 50 + k * 22, 58 + k * 22, 0xFFFFFF, 0.0F, 0.5F / k, 0.0F, 40);
				}
			}
		}

		/**
		 * A tattered void cape hung from the shoulders: a rippling black sheet with a torn hem of embers that billows
		 * behind the giant and streams back hard during the punch.
		 */
		private void renderCape(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d LS, Vec3d RS, Vec3d rt, float a, float env) {
			int cols = 12, rows = 14;
			double punch = clamp01((a - PUNCH_START) / (double) (IMPACT - PUNCH_START)) * (1 - clamp01((a - IMPACT - 40) / 80.0));
			Vec3d[][] g = new Vec3d[rows + 1][cols + 1];
			for (int r = 0; r <= rows; r++) {
				double v = (double) r / rows;
				for (int c = 0; c <= cols; c++) {
					double u = (double) c / cols;
					// Torn: each column hangs to its own length.
					double len = 1 - 0.35 * Solids.hash(c, 91) * v;
					Vec3d top = LS.lerp(RS, u).add(0, 22, 0).add(fwd.multiply(34));
					double across = (u - 0.5) * 0.5 * v * 156;
					double wave = Math.sin(a * 0.06 + u * 5.0 + v * 3.2) * 14 * v + Math.sin(a * 0.11 + u * 9.0) * 4 * v;
					double back = 20 * v * v + wave + punch * 140 * v * v;
					g[r][c] = top.add(rt.multiply(across)).add(fwd.multiply(back)).add(0, -v * 360 * len + punch * 70 * v * v, 0);
				}
			}
			Solids.Cell skin = voidSkin(a);
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Vec3d n = g[r + 1][c].subtract(g[r][c]).crossProduct(g[r][c + 1].subtract(g[r][c]));
					n = n.lengthSquared() < 1.0E-6 ? fwd : n.normalize();
					int col = Solids.scale(skin.color(r, c, n), Solids.light(n.y < 0 ? n.multiply(-1) : n) * 0.8);
					Solids.quad(solid, m, g[r][c], g[r][c + 1], g[r + 1][c + 1], g[r + 1][c], col, 1.0F);
				}
			}
			// The torn hem smoulders: embers along it, drifting up off the cloth.
			for (int c = 0; c < cols; c++) {
				FxDraw.ribbon(glow, m, g[rows][c], g[rows][c + 1], Vec3d.ZERO, 5, 0xFF4010, 0.7F * env, 0.7F * env);
				double life = (a * 0.03 + Solids.hash(c, 93)) % 1.0;
				Vec3d e0 = g[rows][c].add(0, life * 80, 0).add(fwd.multiply(life * 20));
				FxDraw.ribbon(glow, m, e0, e0.add(0, 4, 0), Vec3d.ZERO, 2.5, 0xFFA040, 0.8F * (float) (1 - life) * env, 0.0F);
			}
		}

		private void renderOrbit(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d cam, Vec3d[] cb, float a) {
			Solids.stars(glow, m, cb, a, 1.0F);
			Solids.nebula(glow, m, cb, 3, 0x6040C0, 0x2060A0, 1.0F);
			Solids.sun(glow, m, cb, Solids.LIGHT, 7, 0xFFE8C0, a);
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
			// Ejecta thrown off into orbit from the impact.
			Vec3d[] ib = ArsenalEffects.basis(impN);
			for (int i = 0; i < 70; i++) {
				double ang = Solids.hash(i, 61) * MathHelper.TAU;
				double spread = Solids.hash(i, 62) * 0.7;
				Vec3d d = impN.add(ib[0].multiply(Math.cos(ang) * spread)).add(ib[1].multiply(Math.sin(ang) * spread)).normalize();
				double dist = R * 1.01 + (a - SPACE) * (0.3 + Solids.hash(i, 63) * 0.9);
				Vec3d p = c.add(d.multiply(dist));
				Solids.dot(glow, m, p, cb, 0.6 + Solids.hash(i, 64) * 1.2, i % 3 == 0 ? 0xFFFFFF : 0xFFA050, 0.9F);
				FxDraw.ribbon(glow, m, p, p.subtract(d.multiply(6)), Vec3d.ZERO, 0.5, 0xFF8030, 0.5F, 0.0F);
			}
			Solids.flare(glow, m, hit, cb, 18 * (1 - u * 0.5), 0xFFD0A0, 0.8F);
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
				double dist = lerp(50, 120, t);
				Vec3d p = anchor.add(Math.cos(ang) * dist, lerp(8, 230, t), Math.sin(ang) * dist);
				aim(s, p, anchor, (float) lerp(78, 88, t), false);
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
		public float fade(float a) {
			return Math.max(super.fade(a), dip(a, SPACE_AT, 7));
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= SPACE_AT) {
				ClientFx.tintSky(0x000006, 1.0F);
				int u = age - SPACE_AT;
				if (u >= 60 && u < 140) {
					// The planet's cracks glow brighter and brighter: god rays leak out of the core.
					ClientFx.light(sa, 0.3F + (u - 60) / 80.0F, 0xFF7020);
				} else if (u == 140) {
					ClientFx.light(sa, 1.5F, 0xFFE0B0);
				}
			} else if (age % 12 == 0) {
				// Fires still raging across the bombed site below.
				ClientFx.light(anchor.add(world.random.nextGaussian() * 30, 4, world.random.nextGaussian() * 30), 0.6F, 0xFF7A30);
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
			Solids.nebula(glow, m, cb, 7, 0x8030A0, 0x205090, 1.0F);
			Solids.sun(glow, m, cb, Solids.LIGHT, 7, 0xFFE8C0, a);
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
				// A flat rubble disc spreading through the orbital plane, plus shock rings at odd angles.
				Vec3d dn = new Vec3d(0.15, 1, 0.25).normalize();
				Vec3d[] db = ArsenalEffects.basis(dn);
				float dfade = (float) Math.max(0, 1 - e / 120);
				FxDraw.ring(shade, m, c, db[0], db[1], R * 0.8 + e * 3.5, R * 1.2 + e * 6, 0xB07040, 0.0F, 0.55F * dfade, 0.0F, 96);
				for (int i = 0; i < 140; i++) {
					double ang = Solids.hash(i, 71) * MathHelper.TAU + e * 0.004;
					double rr = R * 0.9 + e * (2.5 + Solids.hash(i, 72) * 4.5);
					Vec3d p = c.add(db[0].multiply(Math.cos(ang) * rr)).add(db[1].multiply(Math.sin(ang) * rr)).add(dn.multiply((Solids.hash(i, 73) - 0.5) * 10));
					Solids.dot(glow, m, p, cb, 1.2 + Solids.hash(i, 74) * 2.2, i % 2 == 0 ? 0xFFB060 : 0xFF6A20, 0.9F * dfade);
				}
				for (int k = 0; k < 3; k++) {
					Vec3d[] kb = ArsenalEffects.basis(sphereDir(k, 55));
					double rr = e * (10 + k * 3);
					FxDraw.ring(glow, m, c, kb[0], kb[1], rr, rr + 6, k == 0 ? 0xFFFFFF : 0xFF9040, 0.0F, 0.6F * (float) Math.max(0, 1 - e / (45 + k * 10)), 0.0F, 96);
				}
				// The shards catch the moon: it breaks apart and tumbles away.
				if (e >= 20) {
					float me = e - 20;
					Vec3d away = mc.subtract(c).normalize();
					for (int i = 0; i < 18; i++) {
						Vec3d d = sphereDir(i, 81).add(away.multiply(1.2)).normalize();
						Vec3d p = mc.add(d.multiply(6 + me * (0.25 + Solids.hash(i, 82) * 0.5)));
						rock(solid, m, p, 4 + Solids.hash(i, 83) * 7, me * 0.05, i, 0xA8A8B0);
					}
					Solids.dot(glow, m, mc, cb, 30 * Math.max(0, 1 - me / 30), 0xFFE0C0, 0.8F);
				}
				Solids.flare(glow, m, c, cb, 60 + e * 2, 0xFFD8A0, (float) Math.max(0, 1 - e / 70));
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

	// ======================================================================== ALIEN DEATH RAY

	/**
	 * A visit to the alien world: hatches open in the planet and a colossal ray cannon rises out of it, slews onto
	 * Earth and charges in a run of fast cuts (coils, target lock, arcing emitter, the growing sun at the muzzle). Then it
	 * fires: a beam crosses space, the camera races alongside it, and it hits Earth. The server's sounds run on the same
	 * ticks: {@link #REVEAL}, {@link #CHARGE}, the lock-on beeps, {@link #FIRE} and {@link #HIT}.
	 */
	public static class AlienLaunch extends Cine {
		public static final int REVEAL = 80, AIM = 130, CHARGE = 150, FIRE = 262, HIT = 340;
		private static final double PR = 120, BL = 95, BR = 6.2, ER = 70;
		private final Vec3d c, base, earth, pivot, aimDir, aimSide, muzzleAimed, hitN, hitP;
		private final double beamLen;

		AlienLaunch(Vec3d anchor, Vec3d heading, int life) {
			super(anchor, heading, life);
			this.c = anchor.add(0, 3700, 0);
			this.base = c.add(0, PR, 0);
			this.earth = c.add(300, 560, 0);
			this.pivot = base.add(0, 14, 0);
			this.aimDir = earth.subtract(pivot).normalize();
			Vec3d sd = aimDir.crossProduct(new Vec3d(0, 0, 1));
			this.aimSide = sd.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : sd.normalize();
			this.muzzleAimed = pivot.add(aimDir.multiply(BL));
			this.hitN = muzzleAimed.subtract(earth).normalize();
			this.hitP = earth.add(hitN.multiply(ER));
			this.beamLen = hitP.distanceTo(muzzleAimed);
		}

		// --- cannon kinematics ----------------------------------------------------------

		/** How far the cannon has risen out of its silo: -1 hidden below the surface, 0 fully up. */
		private static double rise(float a) {
			return ease((a - REVEAL - 10) / 40.0) - 1;
		}

		private Vec3d pivotAt(float a) {
			return pivot.add(0, rise(a) * 60, 0);
		}

		private Vec3d barrelDir(float a) {
			double k = ease((a - AIM) / 40.0);
			return new Vec3d(0, 1, 0).lerp(aimDir, k).normalize();
		}

		/** Recoil: the barrel slams back when it fires and eases forward again. */
		private double recoil(float a) {
			double e = a - FIRE;
			return e < 0 ? 0 : 5 * Math.exp(-e / 10.0) * Math.min(1, e / 1.5);
		}

		private Vec3d muzzle(float a) {
			Vec3d d = barrelDir(a);
			return pivotAt(a).add(d.multiply(BL - recoil(a)));
		}

		/** Charge 0..1 between CHARGE and FIRE. */
		private static double charge(float a) {
			return clamp01((a - CHARGE) / (double) (FIRE - CHARGE));
		}

		/** Distance the beam front has travelled from the muzzle. */
		private double front(float a) {
			return clamp01((a - FIRE) / (double) (HIT - FIRE)) * beamLen;
		}

		// --- camera: fast cuts ------------------------------------------------------------

		@Override
		protected void direct(float a, State s) {
			Vec3d up = new Vec3d(0, 1, 0);
			Vec3d mz = muzzle(a);
			Vec3d d = barrelDir(a);
			if (a < REVEAL) {
				double ang = 0.3 + a * 0.006;
				double r = 430 - a * 1.3;
				aim(s, c.add(Math.cos(ang) * r, 130 - a * 0.6, Math.sin(ang) * r), c.add(0, PR + 20, 0), 70, true);
			} else if (a < 110) {
				// Hero shot from the pad's edge: the silo doors part and the cannon rises.
				aim(s, base.add(44, 4, 30), pivotAt(a).add(d.multiply(BL * 0.4)), 72, true);
			} else if (a < AIM) {
				// Worm's-eye up the barrel as it locks upright.
				aim(s, base.add(10, 2, -12), mz, 84, true);
			} else if (a < CHARGE) {
				// Wide: the planet's curve, the turret slewing round onto Earth.
				double u = (a - AIM) / (double) (CHARGE - AIM);
				aim(s, base.add(lerp(170, 140, u), lerp(70, 55, u), lerp(130, 110, u)), pivot.add(aimDir.multiply(40)), 55, true);
			} else if (a < 168) {
				// Close on the coils lighting up one by one.
				double u = (a - CHARGE) / 18.0;
				aim(s, pivot.add(aimDir.multiply(lerp(26, 40, u))).add(aimSide.multiply(16)).add(0, 4, 0), pivot.add(aimDir.multiply(lerp(30, 46, u))), 50, true);
			} else if (a < 186) {
				// Gunsight: Earth in the crosshairs.
				aim(s, muzzleAimed.add(aimDir.multiply(6)), earth, (float) lerp(34, 26, (a - 168) / 18.0), true);
			} else if (a < 204) {
				// The emitter prongs crawling with arcs.
				aim(s, mz.add(aimSide.multiply(15)).subtract(d.multiply(7)).add(0, 4, 0), mz, 55, true);
			} else if (a < 222) {
				// Pull out: the whole planet lit by what is building on it.
				double u = (a - 204) / 18.0;
				aim(s, c.add(lerp(-420, -380, u), 170, 240), base, 58, true);
			} else if (a < 240) {
				// Dolly in on the sun growing at the muzzle.
				double u = (a - 222) / 18.0;
				aim(s, mz.add(aimSide.multiply(lerp(34, 20, u))).add(d.multiply(10)), mz, (float) lerp(60, 42, u), true);
			} else if (a < 252) {
				// Down the barrel from behind the turret, Earth waiting at the end of it.
				aim(s, pivot.subtract(aimDir.multiply(30)).add(up.multiply(16)), earth, 66, true);
			} else if (a < FIRE) {
				// Extreme close-up, shaking.
				aim(s, mz.add(aimSide.multiply(9)).add(d.multiply(4)), mz, 72, true);
			} else if (a < 280) {
				// FIRE: side-on as the beam erupts.
				aim(s, muzzleAimed.add(aimSide.multiply(62)).add(0, 10, 0), muzzleAimed.add(aimDir.multiply(40)), 78, true);
			} else if (a < 312) {
				// Racing alongside the front of the beam.
				Vec3d f = muzzleAimed.add(aimDir.multiply(front(a)));
				aim(s, f.subtract(aimDir.multiply(30)).add(aimSide.multiply(24)).add(0, 6, 0), f.add(aimDir.multiply(20)), 72, true);
			} else if (a < HIT) {
				// From Earth's side, the beam coming straight in.
				Vec3d cam = hitP.add(hitN.multiply(40)).add(aimSide.multiply(60)).add(0, 20, 0);
				aim(s, cam, muzzleAimed.add(aimDir.multiply(front(a))), 64, true);
			} else if (a < 400) {
				// Earth from orbit as it is struck.
				double u = (a - HIT) / 60.0;
				Vec3d side = hitN.crossProduct(up).normalize();
				aim(s, earth.add(side.multiply(lerp(250, 210, u))).add(hitN.multiply(90)).add(0, 50, 0), earth, 55, true);
			} else if (a < 440) {
				// Closer on the wound spreading across the planet.
				double u = (a - 400) / 40.0;
				aim(s, hitP.add(hitN.multiply(lerp(120, 95, u))).add(aimSide.multiply(70)).add(0, 40, 0), hitP, 60, true);
			} else {
				// Back on the alien world: the cannon still firing at the sky.
				double u = (a - 440) / 40.0;
				aim(s, base.add(lerp(90, 110, u), lerp(20, 30, u), lerp(-70, -90, u)), muzzleAimed.add(aimDir.multiply(80)), 62, true);
			}
		}

		@Override
		protected float handheld(float a) {
			if (a >= REVEAL && a < AIM) {
				return 0.5F;
			}
			if (a >= CHARGE && a < FIRE) {
				return 0.3F + (float) charge(a) * 1.3F;
			}
			if (a >= FIRE && a < 300) {
				return 2.2F;
			}
			if (a >= HIT && a < 400) {
				return 1.2F;
			}
			return 0.2F;
		}

		@Override
		public float fade(float a) {
			return Math.max(super.fade(a), Math.max(dip(a, REVEAL, 3), Math.max(dip(a, HIT, 3), dip(a, 440, 3))));
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x000006, 1.0F);
			float a = age;
			if (a >= CHARGE && a < FIRE) {
				ClientFx.light(muzzle(a), 0.4F + 1.0F * (float) charge(a), 0x80FFC0);
				// Energy pours in from all around the emitter.
				Vec3d mz = muzzle(a);
				for (int i = 0; i < 6 + (int) (charge(a) * 14); i++) {
					Vec3d d = sphereDir(age * 20 + i, 5).multiply(30 + world.random.nextDouble() * 40);
					Vec3d p = mz.add(d);
					world.addImportantParticle(ModParticles.PLASMA_SPARK, true, p.x, p.y, p.z, -d.x * 0.08, -d.y * 0.08, -d.z * 0.08);
				}
			}
			if (a >= FIRE) {
				ClientFx.light(muzzleAimed, 1.4F, 0xA0FFD0);
				ClientFx.light(muzzleAimed.add(aimDir.multiply(front(a))), 1.2F, 0xC0FFE0);
			}
			if (a == FIRE || a == HIT) {
				ClientFx.shake = 6.0F;
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
			Solids.stars(glow, m, cb, a, 1.0F);
			Solids.nebula(glow, m, cb, 11, 0x8040E0, 0x20B0B0, 1.2F);
			Solids.sun(glow, m, cb, new Vec3d(-0.6, 0.5, 0.6), 9, 0xB0D8FF, a);
			renderPlanet(solid, glow, shade, m, cb, cam, a);
			renderCannon(solid, glow, shade, m, cb, cam, a);
			renderBeam(glow, m, cb, cam, a);
			renderEarth(solid, glow, shade, m, cb, cam, a);
		}

		/** The alien world: teal and violet storm bands, vortices with lightning, aurora, coloured rings, moons. */
		private void renderPlanet(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			Vec3d pc = c.subtract(cam);
			double spin = a * 0.002;
			// Lit from the muzzle while it charges.
			float lit = (float) charge(a) * (a < FIRE ? 1 : 0) + (a >= FIRE ? 0.6F : 0);
			Solids.ellipsoid(solid, m, pc, PR, PR, PR, 28, 56, rotated(Solids.bands(0x2C1470, 0x129C98, 0x6A3CC8, 7), spin), true, 1.0F);
			FxDraw.sphere(glow, m, pc, PR * 1.06, Vec3d.ZERO, 0x60D0E0, 0.0F, 0.5F + 0.3F * lit, 14, 28);
			for (int i = 0; i < 160; i++) {
				Vec3d d = sphereDir(i, 61);
				d = new Vec3d(d.x * Math.cos(spin) - d.z * Math.sin(spin), d.y, d.x * Math.sin(spin) + d.z * Math.cos(spin));
				if (d.dotProduct(Solids.LIGHT) < -0.05) {
					Solids.dot(glow, m, pc.add(d.multiply(PR * 1.004)), cb, 1.2 + Solids.hash(i, 62) * 1.6, i % 3 == 0 ? 0x60FFA0 : 0xC8A0FF, 0.9F);
				}
			}
			for (int k = 0; k < 5; k++) {
				Vec3d sd = sphereDir(k, 91);
				sd = new Vec3d(sd.x * Math.cos(spin) - sd.z * Math.sin(spin), sd.y * 0.6, sd.x * Math.sin(spin) + sd.z * Math.cos(spin)).normalize();
				Vec3d[] sb = ArsenalEffects.basis(sd);
				Vec3d eye = pc.add(sd.multiply(PR * 1.004));
				for (int arm = 0; arm < 2; arm++) {
					Vec3d prev = null;
					for (int i = 0; i <= 40; i++) {
						double t = i / 40.0;
						double ang = t * 9 + arm * Math.PI + a * 0.05 * (k % 2 == 0 ? 1 : -1);
						double rr = PR * (0.02 + 0.16 * t);
						Vec3d q = eye.add(sb[0].multiply(Math.cos(ang) * rr)).add(sb[1].multiply(Math.sin(ang) * rr));
						if (prev != null) {
							FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 1.6 * (1 - t) + 0.4, k % 2 == 0 ? 0xC8A0FF : 0x80FFE0, 0.55F * (float) (1 - t), 0.55F * (float) (1 - t));
						}
						prev = q;
					}
				}
				Solids.dot(glow, m, eye, cb, PR * 0.04, 0xFFFFFF, 0.5F);
				if (Solids.hash(k, (int) (a * 0.25F)) > 0.7) {
					Solids.dot(glow, m, eye, cb, PR * 0.12, 0xE0F0FF, 0.6F);
				}
			}
			for (int k = 0; k < 2; k++) {
				Vec3d pole = new Vec3d(0, k == 0 ? 1 : -1, 0);
				Vec3d prev = null, prevHi = null;
				for (int i = 0; i <= 48; i++) {
					double ang = i * MathHelper.TAU / 48;
					double wob = 0.32 + 0.05 * Math.sin(ang * 5 + a * 0.08);
					Vec3d d = pole.multiply(Math.cos(wob)).add(new Vec3d(Math.cos(ang), 0, Math.sin(ang)).multiply(Math.sin(wob)));
					Vec3d q = pc.add(d.multiply(PR * 1.05)), hi = pc.add(d.multiply(PR * 1.11));
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, PR * 0.05, i % 2 == 0 ? 0x60FFB0 : 0xA060FF, 0.35F, 0.35F);
						FxDraw.ribbon(glow, m, prevHi, hi, Vec3d.ZERO, PR * 0.03, 0x40E0FF, 0.15F, 0.15F);
					}
					prev = q;
					prevHi = hi;
				}
			}
			FxDraw.sphere(glow, m, pc, PR * 1.02, Vec3d.ZERO, 0xB0A0FF, 0.0F, 0.35F, 14, 28);
			Vec3d ringN = new Vec3d(0.25, 1, 0.1).normalize();
			Vec3d[] rb = ArsenalEffects.basis(ringN);
			double[][] bands = {{150, 172, 0.30}, {176, 198, 0.42}, {204, 214, 0.22}, {218, 236, 0.30}, {242, 248, 0.15}};
			int[] bandCol = {0x40C8D0, 0x9A70FF, 0x60FFE0, 0xC880E8, 0xFFFFFF};
			for (int k = 0; k < bands.length; k++) {
				FxDraw.ring(shade, m, pc, rb[0], rb[1], bands[k][0], bands[k][1], bandCol[k], (float) bands[k][2] * 0.6F, (float) bands[k][2], (float) bands[k][2] * 0.6F, 128);
			}
			FxDraw.ring(glow, m, pc, rb[0], rb[1], 186, 190, 0xE0FFFF, 0.0F, 0.3F, 0.0F, 128);
			FxDraw.ring(glow, m, pc, rb[0], rb[1], 214, 218, 0xC070FF, 0.0F, 0.25F, 0.0F, 128);
			for (int i = 0; i < 90; i++) {
				double ang = Solids.hash(i, 95) * MathHelper.TAU + a * 0.003;
				double rr = 150 + Solids.hash(i, 96) * 98;
				Vec3d q = pc.add(rb[0].multiply(Math.cos(ang) * rr)).add(rb[1].multiply(Math.sin(ang) * rr));
				Solids.dot(glow, m, q, cb, 0.5 + Solids.hash(i, 97), i % 3 == 0 ? 0x80FFE0 : 0xFFFFFF, 0.6F);
			}
			for (int k = 0; k < 3; k++) {
				double ang = a * (0.008 + k * 0.004) + k * 2.1;
				Vec3d mp = pc.add(Math.cos(ang) * (270 + k * 50), 30 + Math.sin(k * 3) * 50, Math.sin(ang) * (270 + k * 50));
				Solids.sphere(solid, m, mp, 15 - k * 4, k == 1 ? 0x6A8A98 : 0x9A88B8, 10, 16, true);
			}
		}

		/** The ray cannon: silo doors, turret, a long coiled barrel, power conduits, and a three-pronged emitter. */
		private void renderCannon(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			Vec3d bs = base.subtract(cam);
			Vec3d pv = pivotAt(a).subtract(cam);
			Vec3d d = barrelDir(a);
			Vec3d[] db = ArsenalEffects.basis(d);
			double ch = charge(a);
			boolean fired = a >= FIRE;
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			// The silo: a broad pad, a dark well in the middle and two doors sliding apart.
			Solids.tube(solid, m, bs.add(0, -2, 0), bs.add(0, 1.5, 0), 46, 44, 24, 0x1A1424, 1.0F, true);
			Solids.tube(solid, m, bs.add(0, 1.5, 0), bs.add(0, 1.8, 0), 44, 44, 48, 1, (i, j, n) -> j % 2 == 0 ? 0xE8B020 : 0x161218, false, 1.0F);
			FxDraw.disc(solid, m, bs.add(0, 1.85, 0), x, z, 22, 0x050308, 1.0F, 1.0F, 32);
			double open = ease((a - REVEAL) / 14.0);
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Vec3d door = bs.add(sgn * (11 + open * 24), 2.6, 0);
				Solids.tube(solid, m, door.add(0, -0.8, -21), door.add(0, -0.8, 21), 1.4, 1.4, 4, 0x2E2840, 1.0F, true);
				FxDraw.disc(solid, m, door, x, z, 11.5, 0x2A2438, 1.0F, 1.0F, 16);
				FxDraw.ring(glow, m, door.add(0, 0.1, 0), x, z, 10.5, 11.5, 0xFF8A20, 0.0F, open < 1 ? 0.9F : 0.3F, 0.0F, 24);
			}
			// Power conduits from the pad's edge to the turret, pulses running inward faster as it charges.
			for (int k = 0; k < 6; k++) {
				double ang = k * MathHelper.TAU / 6 + 0.3;
				Vec3d from = bs.add(Math.cos(ang) * 42, 2, Math.sin(ang) * 42);
				Vec3d to = pv.add(Math.cos(ang) * 14, -6, Math.sin(ang) * 14);
				Solids.tube(solid, m, from, to, 1.6, 1.4, 6, 0x22202E, 1.0F, true);
				float base0 = 0.25F + 0.5F * (float) ch;
				double pulse = (a * (0.02 + ch * 0.08) + k * 0.17) % 1.0;
				FxDraw.ribbon(glow, m, from, to, Vec3d.ZERO, 1.6, 0x40FFB0, base0 * 0.5F, base0 * 0.5F);
				Vec3d pp = from.lerp(to, pulse);
				Solids.dot(glow, m, pp, cb, 2.5 + ch * 2, 0xC0FFE0, 0.8F * (float) Math.max(0.2, ch));
			}
			if (rise(a) <= -0.99 && a < REVEAL + 10) {
				return;
			}
			// Turret: a heavy dome on a ring, seams glowing, a crystal heart behind armoured windows.
			Solids.tube(solid, m, pv.add(0, -14, 0), pv.add(0, -6, 0), 20, 18, 20, 0x14101E, 1.0F, true);
			Solids.ellipsoid(solid, m, pv, 19, 13, 19, 12, 20, (i, j, n) -> (j % 5 == 0) ? 0x2A2440 : 0x1E1A2E, true, 1.0F);
			FxDraw.ring(glow, m, pv.add(0, -6, 0), x, z, 18.5, 19.5, 0x40FFB0, 0.0F, 0.6F + 0.4F * (float) ch, 0.0F, 40);
			for (int k = 0; k < 4; k++) {
				double ang = k * MathHelper.TAU / 4 + a * 0.01;
				Vec3d w = pv.add(Math.cos(ang) * 18.6, 2, Math.sin(ang) * 18.6);
				Solids.dot(glow, m, w, cb, 2.6, 0xC070FF, 0.6F + 0.4F * (float) ch);
			}
			Solids.dot(glow, m, pv, cb, 10 + ch * 8, 0xA060FF, 0.35F + 0.4F * (float) ch);
			// Trunnions and the barrel: plated sections, six coils that light up in turn, rings racing to the muzzle.
			Vec3d side = d.crossProduct(new Vec3d(0, 0, 1));
			side = side.lengthSquared() < 1.0E-6 ? x : side.normalize();
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Solids.tube(solid, m, pv.add(side.multiply(sgn * 12)), pv.add(side.multiply(sgn * 20)), 5, 5, 10, 0x2A2440, 1.0F, true);
			}
			double rc = recoil(a);
			Vec3d breech = pv.subtract(d.multiply(14 + rc)), mz = pv.add(d.multiply(BL - rc));
			Solids.tube(solid, m, breech, pv.add(d.multiply(16 - rc)), BR * 1.6, BR * 1.4, 16, 0x1E1A2E, 1.0F, true);
			for (int sct = 0; sct < 5; sct++) {
				double u0 = 16 + sct * (BL - 22) / 5.0, u1 = 16 + (sct + 1) * (BL - 22) / 5.0;
				Solids.tube(solid, m, pv.add(d.multiply(u0 - rc)), pv.add(d.multiply(u1 - rc - 0.6)), BR * (1.05 - sct * 0.05), BR * (1.0 - sct * 0.05), 16,
						sct % 2 == 0 ? 0x2C2448 : 0x221C38, 1.0F, true);
			}
			for (int k = 0; k < 6; k++) {
				double u = 22 + k * 11;
				Vec3d cp = pv.add(d.multiply(u - rc));
				double lit = clamp01(ch * 6.5 - k) + (fired ? 1 : 0);
				float pulse = 0.7F + 0.3F * MathHelper.sin(a * 0.8F - k);
				Solids.tube(solid, m, cp.subtract(d.multiply(1.8)), cp.add(d.multiply(1.8)), BR * 1.55, BR * 1.55, 16, 0x3A3456, 1.0F, true);
				FxDraw.ring(glow, m, cp, db[0], db[1], BR * 1.5, BR * 1.8, 0x50FFC0, 0.0F, (float) Math.min(1, 0.15 + 0.85 * lit) * pulse, 0.0F, 32);
				if (lit > 0.5) {
					FxDraw.ring(glow, m, cp, db[0], db[1], BR * 1.8, BR * 3.2, 0x80FFE0, 0.0F, 0.25F * (float) Math.min(1, lit) * pulse, 0.0F, 32);
				}
			}
			if (ch > 0 && !fired) {
				for (int k = 0; k < 4; k++) {
					double u = ((a * (0.02 + ch * 0.1) + k * 0.25) % 1.0) * (BL - 20) + 18;
					Vec3d rp = pv.add(d.multiply(u - rc));
					FxDraw.ring(glow, m, rp, db[0], db[1], BR * 1.1, BR * 1.6, 0xFFFFFF, 0.0F, 0.6F * (float) ch, 0.0F, 28);
				}
			}
			// Emitter: three prongs curling forward around the muzzle, arcing between them as the charge builds.
			Vec3d[] prongTips = new Vec3d[3];
			for (int k = 0; k < 3; k++) {
				double ang = k * MathHelper.TAU / 3 + 0.5;
				Vec3d out = db[0].multiply(Math.cos(ang)).add(db[1].multiply(Math.sin(ang)));
				Vec3d root = mz.subtract(d.multiply(6)).add(out.multiply(BR * 1.1));
				Vec3d mid = mz.add(d.multiply(4)).add(out.multiply(BR * 2.4));
				Vec3d tip = mz.add(d.multiply(12)).add(out.multiply(BR * 1.3));
				Solids.tube(solid, m, root, mid, 1.6, 1.2, 6, 0x2E2848, 1.0F, true);
				Solids.tube(solid, m, mid, tip, 1.2, 0.3, 6, 0x2E2848, 1.0F, true);
				Solids.dot(glow, m, tip, cb, 1.6 + ch * 2, 0xA0FFE0, 0.5F + 0.5F * (float) ch);
				prongTips[k] = tip;
			}
			int roll = (int) (a * 0.7F);
			if (ch > 0.3 && !fired) {
				for (int k = 0; k < 3; k++) {
					Vec3d p0 = prongTips[k], p1 = prongTips[(k + 1) % 3];
					Vec3d prev = p0;
					for (int s2 = 1; s2 <= 6; s2++) {
						Vec3d q = p0.lerp(p1, s2 / 6.0);
						if (s2 < 6) {
							q = q.add(sphereDir(roll * 7 + k * 11 + s2, 23).multiply(2.5 * ch));
						}
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.5, s2 % 2 == 0 ? 0xFFFFFF : 0x80FFE0, (float) ch, (float) ch);
						prev = q;
					}
				}
			}
			// The sun at the muzzle: it grows with the charge and is spent when it fires.
			Vec3d orb = mz.add(d.multiply(8));
			if (!fired && ch > 0) {
				double r = 1 + ch * ch * 8;
				float flick = 0.85F + 0.15F * MathHelper.sin(a * 2.1F);
				FxDraw.sphere(glow, m, orb, r, Vec3d.ZERO, 0xFFFFFF, 0.95F * flick, 0.7F, 10, 16);
				FxDraw.sphere(glow, m, orb, r * 1.8, Vec3d.ZERO, 0x60FFC0, 0.4F * flick, 0.0F, 10, 16);
				Solids.dot(glow, m, orb, cb, r * 5, 0x80FFD0, 0.25F * (float) ch);
				Solids.flare(glow, m, orb, cb, r * 3, 0xC0FFE0, 0.6F * (float) ch);
			}
			if (fired && a < FIRE + 14) {
				float f = 1 - (a - FIRE) / 14.0F;
				Solids.dot(glow, m, orb, cb, 60 * f + 10, 0xFFFFFF, f);
				for (int k = 0; k < 3; k++) {
					double rr = (a - FIRE) * (6 + k * 3);
					FxDraw.ring(glow, m, mz, db[0], db[1], rr, rr + 3, 0xC0FFE0, 0.0F, 0.8F * f, 0.0F, 48);
				}
			}
		}

		/** The death ray: a white-hot core in green and violet sheaths, wound with a double helix, racing to Earth. */
		private void renderBeam(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			if (a < FIRE) {
				return;
			}
			double len = front(a);
			Vec3d start = muzzleAimed.add(aimDir.multiply(8)).subtract(cam);
			Vec3d end = muzzleAimed.add(aimDir.multiply(len)).subtract(cam);
			Vec3d[] db = ArsenalEffects.basis(aimDir);
			float swell = 0.85F + 0.15F * MathHelper.sin(a * 1.3F);
			double w = (a < FIRE + 6 ? (a - FIRE) / 6.0 : 1) * swell;
			Solids.tube(glow, m, start, end, 2.4 * w, 2.4 * w, 12, 0xFFFFFF, 0.95F, false);
			Solids.tube(glow, m, start, end, 5.5 * w, 5.5 * w, 12, 0x60FFC0, 0.45F, false);
			Solids.tube(glow, m, start, end, 12 * w, 12 * w, 12, 0xA060FF, 0.14F, false);
			for (int k = 0; k < 2; k++) {
				Vec3d prev = null;
				int segs = Math.max(2, (int) (len / 4));
				for (int s2 = 0; s2 <= segs; s2++) {
					double u = s2 / (double) segs;
					double ang = k * Math.PI + u * len * 0.12 - a * 0.6;
					Vec3d p = start.add(aimDir.multiply((len - 8) * u)).add(db[0].multiply(Math.cos(ang) * 7 * w)).add(db[1].multiply(Math.sin(ang) * 7 * w));
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.2, k == 0 ? 0xC0FFE0 : 0xD0A0FF, 0.8F, 0.8F);
					}
					prev = p;
				}
			}
			for (int k = 0; k < (int) (len / 30); k++) {
				double u = (k * 30 + a * 9) % len;
				Vec3d rp = start.add(aimDir.multiply(u));
				FxDraw.ring(glow, m, rp, db[0], db[1], 7 * w, 9 * w, 0xFFFFFF, 0.0F, 0.5F, 0.0F, 24);
			}
			if (a < HIT + 2) {
				Solids.flare(glow, m, end, cb, 22, 0xC0FFE0, 0.9F);
				Solids.dot(glow, m, end, cb, 14, 0xFFFFFF, 0.9F);
			}
		}

		/** Earth, the target lock while it charges, and the wound the beam tears in it. */
		private void renderEarth(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			Vec3d ec = earth.subtract(cam);
			Cinematics.earth(solid, glow, shade, m, ec, ER, a * 0.004);
			if (a > 160 && a < FIRE + 10) {
				double lock = 1.0 + (1 - ease((a - 160) / 30.0)) * 1.2;
				float blinkR = ((int) (a * 0.4) & 1) == 1 ? 0.35F : 0.9F;
				for (int k = 0; k < 2; k++) {
					double ang = a * (k == 0 ? 0.03 : -0.05);
					Vec3d u = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
					Vec3d v = cb[1].multiply(Math.cos(ang)).subtract(cb[0].multiply(Math.sin(ang)));
					double rr = ER * (k == 0 ? 1.3 : 1.15) * lock;
					FxDraw.ring(glow, m, ec, u, v, rr, rr + ER * 0.03, 0xFF3050, 0.0F, blinkR, 0.0F, 72);
					if (k == 0) {
						for (int q = 0; q < 4; q++) {
							double qa = q * Math.PI / 2;
							Vec3d dir = u.multiply(Math.cos(qa)).add(v.multiply(Math.sin(qa)));
							FxDraw.ribbon(glow, m, ec.add(dir.multiply(rr * 1.05)), ec.add(dir.multiply(rr * 1.3)), Vec3d.ZERO, ER * 0.03, 0xFF3050, blinkR, blinkR);
						}
					}
				}
				Solids.dot(glow, m, hitP.subtract(cam), cb, 3, 0xFF3050, blinkR);
			}
			if (a >= HIT) {
				float e = a - HIT;
				Vec3d hp = hitP.subtract(cam);
				Solids.dot(glow, m, hp, cb, Math.min(80, 10 + e * 2), 0xFFFFFF, (float) Math.max(0.4, 1 - e / 60.0));
				Solids.flare(glow, m, hp, cb, 30 + e * 0.5, 0xC0FFE0, 0.8F);
				for (int k = 0; k < 3; k++) {
					double theta = Math.min(Math.PI * 0.95, Math.max(0, e * 0.02 - k * 0.1));
					if (theta > 0.01) {
						sphereRing(glow, m, ec, ER, hitN, theta, 2.6 - k * 0.6, k == 0 ? 0xFFFFFF : 0x60FFC0, 0.9F - k * 0.25F);
					}
				}
				// The crust cracks open, glowing, in lines racing away from the hit.
				Vec3d[] hb = ArsenalEffects.basis(hitN);
				for (int k = 0; k < 10; k++) {
					double ang = k * MathHelper.TAU / 10 + Solids.hash(k, 3) * 0.4;
					Vec3d dir = hb[0].multiply(Math.cos(ang)).add(hb[1].multiply(Math.sin(ang)));
					Vec3d prev = null;
					int n = (int) Math.min(18, e * 0.3);
					for (int i = 0; i <= n; i++) {
						double th = i * 0.07;
						Vec3d p = ec.add(hitN.multiply(Math.cos(th)).add(dir.multiply(Math.sin(th))).multiply(ER * 1.004));
						if (prev != null) {
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.6, 0x80FFA0, 0.9F, 0.9F);
						}
						prev = p;
					}
				}
				// The atmosphere catches fire and the planet is wrapped in a glowing shell.
				float burn = (float) clamp01(e / 80.0);
				FxDraw.sphere(glow, m, ec, ER * 1.1, Vec3d.ZERO, 0x60FFA0, 0.0F, 0.6F * burn, 14, 28);
				for (int i = 0; i < 60; i++) {
					Vec3d dd = hitN.add(sphereDir(i, 81).multiply(0.8)).normalize();
					double dist = ER + e * (0.3 + Solids.hash(i, 82) * 0.8);
					Vec3d p = ec.add(dd.multiply(dist));
					Solids.dot(glow, m, p, cb, 0.8 + Solids.hash(i, 83) * 1.4, i % 3 == 0 ? 0xFFFFFF : 0x80FFC0, 0.9F);
				}
			}
		}
	}
}
