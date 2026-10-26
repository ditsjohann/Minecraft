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
			int plate = 0x241C46, gold = 0xD8A030, gem = 0x60FFE0;
			Solids.Cell sky = Solids.nightSky(a, 3);

			if (body) {
				renderCape(solid, glow, m, LS, RS, rt, a, env);
				// Skin is a window onto a drifting night sky.
				Solids.tube(solid, m, N, P, 72, 50, 16, 8, sky, true, 1.0F);
				Solids.tube(solid, m, N, N.add(0, 32, 0), 26, 22, 10, 2, sky, true, 1.0F);
				Solids.ellipsoid(solid, m, H, 44, 44, 44, 14, 22, sky, true, 1.0F);
				Solids.tube(solid, m, RS, elbowR, 32, 27, 12, 6, sky, true, 1.0F);
				Solids.tube(solid, m, elbowR, handR, 27, 25, 12, 6, sky, true, 1.0F);
				Solids.tube(solid, m, LS, elbowL, 32, 27, 12, 6, sky, true, 1.0F);
				Solids.tube(solid, m, elbowL, handL, 27, 25, 12, 6, sky, true, 1.0F);
				// Breastplate with gold trim at its collar and hem.
				Solids.tube(solid, m, N.add(0, -30, 0), N.add(0, -120, 0), 80, 70, 16, plate, 1.0F, true);
				Solids.tube(solid, m, N.add(0, -27, 0), N.add(0, -33, 0), 82.5, 82.5, 24, gold, 1.0F, true);
				Solids.tube(solid, m, N.add(0, -117, 0), N.add(0, -123, 0), 72.5, 72.5, 24, gold, 1.0F, true);
				// Pauldrons with gold rims.
				for (Vec3d sh : new Vec3d[]{RS, LS}) {
					Solids.ellipsoid(solid, m, sh.add(0, 8, 0), 52, 34, 52, 8, 14, (i, j, n) -> n.y < -0.25 ? gold : plate, true, 1.0F);
					Solids.ellipsoid(solid, m, sh.add(0, 30, 0), 26, 16, 26, 6, 10, (i, j, n) -> gold, true, 1.0F);
				}
				// Bracers and gold-knuckled gauntlets.
				for (Vec3d[] arm : new Vec3d[][]{{elbowR, handR}, {elbowL, handL}}) {
					Vec3d d = arm[1].subtract(arm[0]);
					Solids.tube(solid, m, arm[0].add(d.multiply(0.45)), arm[0].add(d.multiply(0.85)), 30, 29, 12, plate, 1.0F, true);
					for (double k : new double[]{0.45, 0.65, 0.85}) {
						Vec3d bc = arm[0].add(d.multiply(k));
						Solids.tube(solid, m, bc.subtract(d.normalize().multiply(2.5)), bc.add(d.normalize().multiply(2.5)), 31.5, 31.5, 16, gold, 1.0F, true);
					}
				}
				Solids.sphere(solid, m, elbowR, 30, plate, 8, 12, true);
				Solids.sphere(solid, m, elbowL, 30, plate, 8, 12, true);
				Solids.sphere(solid, m, handR, 50, plate, 12, 18, true);
				Solids.sphere(solid, m, handL, 44, plate, 10, 16, true);
				Vec3d[] kb = ArsenalEffects.basis(punchDir);
				for (int k = 0; k < 4; k++) {
					double s = (k - 1.5) * 22;
					Solids.sphere(solid, m, handR.add(punchDir.multiply(38)).add(kb[0].multiply(s)), 17, gold, 6, 10, true);
				}
			}
			float rim = 0.25F * env;
			Solids.tube(glow, m, N, P, 76, 54, 12, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, RS, elbowR, 35, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowR, handR, 31, 28, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, LS, elbowL, 35, 30, 8, 0x7A5CFF, rim, false);
			Solids.tube(glow, m, elbowL, handL, 31, 28, 8, 0x7A5CFF, rim, false);
			// Stars on the skin, a few big enough to flare, linked into constellations.
			Vec3d[] tb = ArsenalEffects.basis(P.subtract(N));
			Vec3d prevStar = null;
			for (int i = 0; i < 130; i++) {
				double u = 0.05 + 0.9 * Solids.hash(i, 21);
				double ang = Solids.hash(i, 22) * MathHelper.TAU;
				double rad = lerp(74, 52, u) * 1.03;
				Vec3d p = N.add(P.subtract(N).multiply(u)).add(tb[0].multiply(Math.cos(ang) * rad)).add(tb[1].multiply(Math.sin(ang) * rad));
				float tw = 0.5F + 0.5F * MathHelper.sin(a * 0.17F + i * 1.3F);
				if (i % 19 == 0) {
					Solids.flare(glow, m, p, cb, 9, 0xC8D8FF, tw * env * 0.8F);
				} else {
					Solids.dot(glow, m, p, cb, 1.5 + 2.5 * Solids.hash(i, 23), i % 5 == 0 ? 0xFFD8A0 : 0xFFFFFF, tw * env * 0.85F);
				}
				// The first 28 stars form four constellations of seven.
				if (i < 28) {
					if (i % 7 != 0 && prevStar != null) {
						float draw = (float) clamp01((a - 60 - (i / 7) * 25) / 30.0);
						FxDraw.ribbon(glow, m, prevStar, prevStar.lerp(p, draw), Vec3d.ZERO, 1.3, 0xB0C8FF, 0.55F * env, 0.55F * env);
					}
					prevStar = p;
				}
			}
			Vec3d[][] limbs = {{RS, elbowR}, {elbowR, handR}, {LS, elbowL}, {elbowL, handL}};
			for (int l = 0; l < limbs.length; l++) {
				Vec3d[] seg = limbs[l];
				Vec3d dirS = seg[1].subtract(seg[0]);
				Vec3d[] sb = ArsenalEffects.basis(dirS);
				Vec3d prev = null;
				for (int i = 0; i < 5; i++) {
					double ang = Solids.hash(i, l * 3 + 70) * MathHelper.TAU;
					Vec3d p = seg[0].add(dirS.multiply(0.12 + 0.18 * i)).add(sb[0].multiply(Math.cos(ang) * 29)).add(sb[1].multiply(Math.sin(ang) * 29));
					Solids.dot(glow, m, p, cb, 2.5, 0xFFFFFF, (0.6F + 0.4F * MathHelper.sin(a * 0.2F + i)) * env);
					if (prev != null) {
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.0, 0xB0C8FF, 0.4F * env, 0.4F * env);
					}
					prev = p;
				}
			}
			// Nebula body: drifting coloured mist through the torso so it reads as a galaxy, not a statue.
			for (int i = 0; i < 34; i++) {
				double u = Solids.hash(i, 31);
				double ang = Solids.hash(i, 32) * MathHelper.TAU + a * 0.004 * (i % 2 == 0 ? 1 : -1);
				double rad = lerp(85, 55, u) * (0.4 + 0.9 * Solids.hash(i, 33));
				Vec3d p = N.add(P.subtract(N).multiply(u)).add(tb[0].multiply(Math.cos(ang) * rad)).add(tb[1].multiply(Math.sin(ang) * rad));
				int nc = i % 3 == 0 ? 0xC060FF : i % 3 == 1 ? 0x30C0D0 : 0xFF60C0;
				Solids.dot(glow, m, p, cb, 30 + 40 * Solids.hash(i, 34), nc, 0.07F * env);
			}
			// Glyph seams: runes chase around the armour joints, gold on the trim, violet on the arms.
			float chase = a * 0.08F;
			for (int k = 0; k < 4; k++) {
				Vec3d bandC = N.add(0, -40 - k * 26, 0);
				double br = lerp(83, 71, k / 3.0);
				FxDraw.ring(glow, m, bandC, rt, fwd, br, br + 2.2, 0xB090FF, 0.0F, 0.5F * env, 0.0F, 48);
				for (int g = 0; g < 14; g++) {
					double ang = g * MathHelper.TAU / 14 + k * 0.2;
					Vec3d out = rt.multiply(Math.cos(ang)).add(fwd.multiply(Math.sin(ang)));
					float lit = 0.3F + 0.7F * (float) Math.max(0, Math.sin(chase - g * 0.7 - k));
					Solids.glyph(glow, m, bandC.add(out.multiply(br + 0.8)).add(0, 6, 0), out.crossProduct(new Vec3d(0, 1, 0)).normalize(), new Vec3d(0, 1, 0), 9, g * 5 + k, 0xFFD070, lit * env);
				}
			}
			for (Vec3d[] seg : limbs) {
				Vec3d dirS = seg[1].subtract(seg[0]);
				Vec3d axis = dirS.normalize();
				Vec3d[] sb = ArsenalEffects.basis(dirS);
				for (int k = 1; k <= 3; k++) {
					Vec3d bc = seg[0].add(dirS.multiply(k / 4.0));
					FxDraw.ring(glow, m, bc, sb[0], sb[1], 29, 31, 0x9A7CFF, 0.0F, 0.5F * env, 0.0F, 24);
					for (int g = 0; g < 6; g++) {
						double ang = g * MathHelper.TAU / 6;
						Vec3d out = sb[0].multiply(Math.cos(ang)).add(sb[1].multiply(Math.sin(ang)));
						float lit = 0.3F + 0.7F * (float) Math.max(0, Math.sin(chase - g - k * 1.3));
						Solids.glyph(glow, m, bc.add(out.multiply(30.5)), axis.crossProduct(out).normalize(), axis, 6, g * 11 + k * 3, 0xC8A8FF, lit * env);
					}
				}
			}
			for (Vec3d sh : new Vec3d[]{RS, LS}) {
				FxDraw.ring(glow, m, sh.add(0, 14, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 50, 54, 0xFFD070, 0.0F, 0.6F * env, 0.0F, 40);
			}
			// A gold crown: a band of seven spikes, gems at their roots and points of light at their tips.
			if (body) {
				Vec3d crownBase = H.add(0, 26, 0);
				Solids.tube(solid, m, crownBase, crownBase.add(0, 12, 0), 40, 42, 24, gold, 1.0F, true);
				FxDraw.ring(glow, m, crownBase.add(0, 6, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 42, 44.5, 0xFFE0A0, 0.0F, 0.5F * env, 0.0F, 48);
				for (int k = 0; k < 7; k++) {
					double ang = k * MathHelper.TAU / 7 + Math.atan2(-hd.z, -hd.x) + Math.PI;
					Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
					Vec3d root = crownBase.add(out.multiply(41)).add(0, 12, 0);
					double tall = k == 0 ? 62 : k == 1 || k == 6 ? 44 : 32;
					Vec3d tip = root.add(out.multiply(tall * 0.25)).add(0, tall, 0);
					Solids.tube(solid, m, root, tip, 8, 0.5, 6, gold, 1.0F, true);
					Solids.dot(glow, m, root.add(out.multiply(2)).add(0, 2, 0), cb, 5, k % 2 == 0 ? gem : 0xC070FF, 0.9F * env);
					Solids.flare(glow, m, tip, cb, k == 0 ? 14 : 8, 0xFFE0A0, (0.6F + 0.3F * MathHelper.sin(a * 0.25F + k)) * env);
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
				// Anamorphic eye flares that flare up when it locks on.
				float flareK = a > 270 && a < IMPACT ? 1.0F : 0.5F;
				Solids.flare(glow, m, eye, cb, 26 * flareK + 10, eyeCol, 0.7F * env * flareK);
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

		/**
		 * A nebula cape hung from the shoulders: a rippling sheet of night sky that billows behind the giant and
		 * streams back hard during the punch.
		 */
		private void renderCape(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d LS, Vec3d RS, Vec3d rt, float a, float env) {
			int cols = 12, rows = 14;
			double punch = clamp01((a - PUNCH_START) / (double) (IMPACT - PUNCH_START)) * (1 - clamp01((a - IMPACT - 40) / 80.0));
			Vec3d[][] g = new Vec3d[rows + 1][cols + 1];
			for (int r = 0; r <= rows; r++) {
				double v = (double) r / rows;
				for (int c = 0; c <= cols; c++) {
					double u = (double) c / cols;
					Vec3d top = LS.lerp(RS, u).add(0, 22, 0).add(fwd.multiply(34));
					double flare = 1 + v * 0.5;
					double across = (u - 0.5) * flare * 156 - (u - 0.5) * 156;
					double wave = Math.sin(a * 0.06 + u * 5.0 + v * 3.2) * 14 * v + Math.sin(a * 0.11 + u * 9.0) * 4 * v;
					double back = 20 * v * v + wave + punch * 140 * v * v;
					g[r][c] = top.add(rt.multiply(across)).add(fwd.multiply(back)).add(0, -v * 360 + punch * 70 * v * v, 0);
				}
			}
			Solids.Cell sky = Solids.nightSky(a * 0.7F, 17);
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Vec3d n = g[r + 1][c].subtract(g[r][c]).crossProduct(g[r][c + 1].subtract(g[r][c]));
					n = n.lengthSquared() < 1.0E-6 ? fwd : n.normalize();
					int col = Solids.scale(sky.color(r, c, n.multiply(0.6).add(c * 0.08, r * 0.06, 0)), Solids.light(n.y < 0 ? n.multiply(-1) : n) * 0.85);
					Solids.quad(solid, m, g[r][c], g[r][c + 1], g[r + 1][c + 1], g[r + 1][c], col, 1.0F);
				}
			}
			// A glowing hem and nebula wisps trailing off it.
			for (int c = 0; c < cols; c++) {
				FxDraw.ribbon(glow, m, g[rows][c], g[rows][c + 1], Vec3d.ZERO, 6, c % 2 == 0 ? 0xC070FF : 0x40D0E0, 0.8F * env, 0.8F * env);
				FxDraw.ribbon(glow, m, g[0][c], g[0][c + 1], Vec3d.ZERO, 4, 0xFFD070, 0.7F * env, 0.7F * env);
			}
			for (int r = 0; r < rows; r += 2) {
				FxDraw.ribbon(glow, m, g[r][0], g[r + 2 > rows ? rows : r + 2][0], Vec3d.ZERO, 3, 0xFFD070, 0.5F * env, 0.5F * env);
				FxDraw.ribbon(glow, m, g[r][cols], g[r + 2 > rows ? rows : r + 2][cols], Vec3d.ZERO, 3, 0xFFD070, 0.5F * env, 0.5F * env);
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

		// --- launch prep timeline (ignition at 240 and liftoff at 282 are fixed by the server's sounds) ----------

		/** Nose petals: open while the warhead goes in, shut when it is seated. */
		private static double hatch(float a) {
			return ease((a - 112) / 14.0) * (1 - ease((a - 146) / 8.0));
		}

		/** Gantry towers, arms, hoses and the crane: 0 docked, 1 pulled clear of the pad. */
		private static double retreat(float a) {
			return ease((a - 206) / 30.0);
		}

		/** A warning beacon flash that spikes on each of the server's beeps and decays. */
		private static float beep(float a) {
			float k = 0;
			for (int t : new int[]{160, 185, 210}) {
				if (a >= t) {
					k = Math.max(k, (float) Math.exp(-(a - t) / 6.0));
				}
			}
			return k;
		}

		private Vec3d corePos(float a) {
			Vec3d high = base.add(0, LEN + 34, 0);
			Vec3d seat = base.add(0, LEN * 0.74, 0);
			return lerp(high, seat, ease((a - 118) / 30.0));
		}

		private Vec3d drone(int i, float a) {
			double ang = a * (0.03 + 0.01 * (i % 3)) * (i % 2 == 0 ? 1 : -1) + i * 1.9;
			double h = 8 + (i * 7.3) % (LEN - 10) + Math.sin(a * 0.07 + i) * 3;
			double r = RR + 5 + 2 * Math.sin(a * 0.11 + i * 2);
			double away = ease((a - 214 - i) / 18.0) * 120;
			return base.add(Math.cos(ang) * (r + away), h + away * 0.6, Math.sin(ang) * (r + away));
		}

		@Override
		protected void direct(float a, State s) {
			Vec3d rc = rocketCentre(a);
			Vec3d rd = rocketDir(a);
			Vec3d nose = base.add(0, LEN * 0.84, 0);
			if (a < 110) {
				double ang = 0.3 + a * 0.006;
				double r = 430 - a * 1.1;
				aim(s, c.add(Math.cos(ang) * r, 130 - a * 0.5, Math.sin(ang) * r), c.add(0, PR + 20, 0), 70, true);
			} else if (a < 162) {
				// Close on the nose: the petals open, the glowing core is lowered in, the clamps bite.
				double u = ease((a - 110) / 52.0);
				aim(s, nose.add(lerp(26, 17, u), lerp(14, 6, u), lerp(20, 13, u)), lerp(corePos(a), nose, 0.5 + 0.5 * u), (float) lerp(56, 46, u), true);
			} else if (a < 206) {
				// Tracking down the hull past the fuel hoses and the drone swarm.
				double u = (a - 162) / 44.0;
				double h = lerp(LEN * 0.7, 12, ease(u));
				aim(s, base.add(lerp(-14, 8, u), h + 6, -26), base.add(0, h - 2, 0), 58, true);
			} else if (a < 236) {
				// Wide: steam, beacons, and the towers pulling away from the rocket.
				double u = ease((a - 206) / 30.0);
				aim(s, base.add(lerp(120, 100, u), lerp(16, 10, u), lerp(96, 80, u)), base.add(0, LEN * 0.42, 0), 62, true);
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
			if (a >= 148 && a < 162) {
				return 0.5F;
			}
			if (a >= 236 && a < 330) {
				return 0.6F + Math.max(0, a - 240) * 0.02F;
			}
			if (a >= 400) {
				return 1.2F;
			}
			return 0.15F;
		}

		@Override
		public float fade(float a) {
			return Math.max(super.fade(a), Math.max(dip(a, 110, 4), Math.max(dip(a, 206, 3), dip(a, 236, 3))));
		}

		@Override
		public boolean tick(ClientWorld world) {
			ClientFx.tintSky(0x000006, 1.0F);
			if (age >= 112 && age < 150) {
				ClientFx.light(corePos(age), 0.5F, 0x60FFE0);
			}
			if (age >= 150 && age < 160) {
				// The clamps snap shut on the nose in a spray of sparks.
				for (int q = 0; q < 4; q++) {
					double ang = q * Math.PI / 2 + Math.PI / 4;
					Vec3d p = base.add(Math.cos(ang) * RR, LEN * 0.72, Math.sin(ang) * RR);
					for (int i = 0; i < 4; i++) {
						world.addImportantParticle(ModParticles.PLASMA_SPARK, true, p.x, p.y, p.z,
								Math.cos(ang) * 0.4 + world.random.nextGaussian() * 0.3, world.random.nextDouble() * 0.5, Math.sin(ang) * 0.4 + world.random.nextGaussian() * 0.3);
					}
				}
			}
			if (age >= 228 && age < 240 && world.random.nextFloat() < 0.6F) {
				ClientFx.light(base.add(0, -2, 0), 0.3F, 0x6080FF);
			}
			if (age >= 240) {
				// The engine is the light source of every shot from ignition on.
				ClientFx.light(rocketCentre(age).subtract(rocketDir(age).multiply(LEN / 2)), 0.6F + 0.6F * (float) clamp01((age - 240) / 50.0), 0xC0FFE0);
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
			Solids.nebula(glow, m, cb, 11, 0x8040E0, 0x20B0B0, 1.2F);
			Solids.sun(glow, m, cb, new Vec3d(-0.6, 0.5, 0.6), 9, 0xB0D8FF, a);
			renderPlanet(solid, glow, shade, m, cb, cam, a);
			Vec3d bs = base.subtract(cam);
			Vec3d rcw = rocketCentre(a);
			Vec3d rdir = rocketDir(a);
			float flame = a < 240 ? 0 : (float) clamp01((a - 240) / 50.0);
			Solids.rocket(solid, glow, m, rcw.subtract(cam), rdir, LEN, RR, a, flame, hatch(a));
			// Searchlights sweeping the alien sky.
			for (int k = 0; k < 2; k++) {
				double ang = a * 0.03 + k * Math.PI;
				Vec3d from = bs.add((k == 0 ? -1 : 1) * 40, 0, 0);
				Vec3d dir = new Vec3d(Math.cos(ang) * 0.4, 1, Math.sin(ang) * 0.4).normalize();
				FxDraw.beam(glow, m, from, from.add(dir.multiply(300)), Vec3d.ZERO, 3, 0x60FFA0, 0.35F);
			}
			if (a < 285) {
				renderPad(solid, glow, shade, m, cb, bs, a);
			} else {
				Solids.dot(glow, m, bs.add(0, 2, 0), cb, 90 * Math.max(0.0, 1 - (a - 280) / 80.0), 0xC0FFE0, 0.7F);
			}
			if (a >= 228 && a < 240) {
				// Ignition sequence start: a blue flicker in the nozzle.
				float flick = Solids.hash((int) (a * 3), 7) < 0.5 ? 0.9F : 0.3F;
				Vec3d nozzle = bs.subtract(0, LEN * 0.02, 0);
				Solids.tube(glow, m, nozzle, nozzle.subtract(0, RR * (1 + (a - 228) * 0.3), 0), RR * 0.7, 0.0, 10, 0x6080FF, 0.6F * flick, false);
				Solids.dot(glow, m, nozzle, cb, RR * 2.5, 0x80A0FF, 0.5F * flick);
			}
			if (flame > 0) {
				// Shock diamonds in the exhaust.
				Vec3d nozzle = rcw.subtract(cam).subtract(rdir.multiply(LEN / 2 + LEN * 0.02));
				Solids.flare(glow, m, nozzle.subtract(rdir.multiply(RR)), cb, RR * 3.5, 0xC0FFE0, flame * 0.8F);
				for (int k = 1; k <= 6; k++) {
					Vec3d np = nozzle.subtract(rdir.multiply(k * RR * 1.7));
					float pulse = 0.8F + 0.2F * MathHelper.sin(a * 1.7F + k);
					Solids.dot(glow, m, np, cb, RR * (1.05 - k * 0.12), k % 2 == 0 ? 0xFFFFFF : 0x80FFD0, flame * pulse * (0.85F - k * 0.11F));
				}
			}
			if (a >= 240 && a < 360) {
				// The plume floods across the pad and swallows it, then the launch shock rings.
				float l = a - 240;
				float swallow = (float) (ease(l / 30.0) * (1 - clamp01((l - 70) / 50.0)));
				dustWall(shade, m, cb, bs, 8 + l * 1.5, 10 + l * 0.3, 0xD0C8DC, 0.8F * (1 - l / 120.0F), 7);
				dustWall(shade, m, cb, bs, 4 + l * 0.8, 14 + l * 0.4, 0xE8E4F0, 0.7F * swallow, 13);
				Solids.dot(glow, m, bs.add(0, 6, 0), cb, 20 + l * 1.4, 0x80FFE0, 0.5F * swallow);
				Solids.dot(glow, m, bs.add(0, 3, 0), cb, 10 + l * 0.6, 0xFFFFFF, 0.6F * swallow);
				for (int k = 0; k < 3; k++) {
					double rr = (a - 280 - k * 9) * 3.2;
					if (rr > 1) {
						FxDraw.ring(glow, m, bs.add(0, 1, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), rr, rr + 4, 0xC0FFE0, 0.0F, 0.7F * (float) Math.max(0, 1 - rr / 220), 0.0F, 64);
					}
				}
			}
			if (a > 300) {
				Vec3d ec = earth.subtract(cam);
				double grow = 1.0 + clamp01((a - 360) / 120.0) * 0.35;
				double er = 70 * grow;
				Cinematics.earth(solid, glow, shade, m, ec, er, a * 0.004);
				if (a > 308) {
					// Target lock: a reticle closes in on Earth and starts spinning.
					double lock = 1.0 + (1 - ease((a - 308) / 22.0)) * 1.2;
					float blinkR = a < 330 && ((int) (a * 0.4) & 1) == 1 ? 0.3F : 0.85F;
					for (int k = 0; k < 2; k++) {
						double ang = a * (k == 0 ? 0.03 : -0.05);
						Vec3d u = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
						Vec3d v = cb[1].multiply(Math.cos(ang)).subtract(cb[0].multiply(Math.sin(ang)));
						double rr = er * (k == 0 ? 1.3 : 1.15) * lock;
						FxDraw.ring(glow, m, ec, u, v, rr, rr + er * 0.03, 0xFF3050, 0.0F, blinkR, 0.0F, 72);
						if (k == 0) {
							for (int q = 0; q < 4; q++) {
								double qa = q * Math.PI / 2;
								Vec3d dir = u.multiply(Math.cos(qa)).add(v.multiply(Math.sin(qa)));
								FxDraw.ribbon(glow, m, ec.add(dir.multiply(rr * 1.05)), ec.add(dir.multiply(rr * 1.3)), Vec3d.ZERO, er * 0.03, 0xFF3050, blinkR, blinkR);
							}
						}
					}
				}
			}
		}

		/** The alien world: teal and violet storm bands, vortices with lightning, aurora, coloured rings, moons. */
		private void renderPlanet(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a) {
			Vec3d pc = c.subtract(cam);
			double spin = a * 0.002;
			Solids.ellipsoid(solid, m, pc, PR, PR, PR, 28, 56, rotated(Solids.bands(0x2C1470, 0x129C98, 0x6A3CC8, 7), spin), true, 1.0F);
			FxDraw.sphere(glow, m, pc, PR * 1.06, Vec3d.ZERO, 0x60D0E0, 0.0F, 0.5F, 14, 28);
			// City lights on the night side.
			for (int i = 0; i < 160; i++) {
				Vec3d d = sphereDir(i, 61);
				d = new Vec3d(d.x * Math.cos(spin) - d.z * Math.sin(spin), d.y, d.x * Math.sin(spin) + d.z * Math.cos(spin));
				if (d.dotProduct(Solids.LIGHT) < -0.05) {
					Solids.dot(glow, m, pc.add(d.multiply(PR * 1.004)), cb, 1.2 + Solids.hash(i, 62) * 1.6, i % 3 == 0 ? 0x60FFA0 : 0xC8A0FF, 0.9F);
				}
			}
			// Swirling storm vortices in the bands, lit from inside by lightning.
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
			// Polar aurora curtains.
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
			// Coloured rings with gaps, like Saturn's.
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

		/** Launch complex: towers and arms, crane and warhead, clamps, fuel hoses, drones, steam and beacons. */
		private void renderPad(VertexConsumer solid, VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d bs, float a) {
			double ret = retreat(a);
			float flash = beep(a);
			Solids.tube(solid, m, bs.add(0, -1, 0), bs.add(0, 1.5, 0), 28, 26, 16, 0x1A1424, 1.0F, true);
			// Hazard ring painted round the pad edge.
			Solids.tube(solid, m, bs.add(0, 1.5, 0), bs.add(0, 1.7, 0), 26, 26, 32, 1, (i, j, n) -> j % 2 == 0 ? 0xE8B020 : 0x161218, false, 1.0F);
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				// Towers slide out and lean away once fuelling ends.
				Vec3d foot = bs.add(sgn * (28 + ret * 34), 0, 0);
				Vec3d top = foot.add(sgn * ret * 22, 74, 0);
				Solids.tube(solid, m, foot, top, 2.4, 2.0, 6, 0x3A3A48, 1.0F, true);
				for (int k = 0; k < 7; k++) {
					Vec3d p = foot.lerp(top, k / 7.0);
					Solids.tube(solid, m, p, foot.lerp(top, (k + 1) / 7.0).add(0, 0, (k % 2 == 0 ? 3 : -3)), 0.5, 0.5, 4, 0x56566A, 1.0F, true);
				}
				for (int k = 0; k < 4; k++) {
					double h = 14 + k * 15;
					Vec3d anchorPt = foot.lerp(top, h / 74.0);
					Vec3d hull = bs.add(sgn * RR * 0.9, h, 0);
					double arm = ease((a - 206 - k * 4) / 16.0);
					Vec3d end = lerp(hull, anchorPt.add(-sgn * 4, 0, 0), arm);
					Solids.tube(solid, m, anchorPt, end, 1.0, 0.7, 5, 0x6A6A7A, 1.0F, true);
					Solids.tube(glow, m, end, end.add(0, 0.8, 0), 1.5, 1.5, 6, arm < 1 ? 0xFF8A20 : 0x404058, 0.9F, false);
				}
				// Rotating warning beacon on the tower top.
				double rot = a * 0.25 + (sgn > 0 ? Math.PI : 0);
				int bc = sgn > 0 ? 0xFF3030 : 0xFFA020;
				float on = a > 150 ? 0.6F + 0.4F * flash : 0.0F;
				Solids.dot(glow, m, top.add(0, 2, 0), cb, 2.5 + 4 * flash, bc, on);
				FxDraw.beam(glow, m, top.add(0, 2, 0), top.add(Math.cos(rot) * 40, 2, Math.sin(rot) * 40), Vec3d.ZERO, 1.6, bc, 0.4F * on);
				// Fuel hoses sag from the tower to the hull; pulses run along them until they drop away.
				for (int k = 0; k < 2; k++) {
					double h = 18 + k * 26;
					Vec3d from = foot.lerp(top, h / 74.0);
					Vec3d to = lerp(bs.add(sgn * RR, h - 4, 0), from.add(-sgn * 3, -12, 0), ease((a - 200 - k * 3) / 10.0));
					Vec3d prev = null;
					for (int i = 0; i <= 12; i++) {
						double t = i / 12.0;
						Vec3d q = from.lerp(to, t).add(0, -Math.sin(t * Math.PI) * 6, 0);
						if (prev != null) {
							Solids.tube(solid, m, prev, q, 0.8, 0.8, 6, 0x22222C, 1.0F, true);
							if (a > 160 && a < 204) {
								double pulse = ((a - 160) * 0.06 + k * 0.3) % 1.0;
								float g = (float) Math.max(0, 1 - Math.abs(t - pulse) * 6);
								FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 1.9, k == 0 ? 0x40FFB0 : 0xC070FF, 0.25F + 0.75F * g, 0.25F + 0.75F * g);
							}
						}
						prev = q;
					}
				}
			}
			// Crane: a mast behind the rocket whose jib carries the warhead core down into the open nose.
			Vec3d mastFoot = bs.add(0, 0, -34 - ret * 30);
			Vec3d mastTop = mastFoot.add(0, LEN + 46, -ret * 26);
			Vec3d trolley = mastTop.add(0, 0, 34 + ret * 30 + ret * 26);
			Solids.tube(solid, m, mastFoot, mastTop, 2.2, 1.8, 6, 0x3A3A48, 1.0F, true);
			Solids.tube(solid, m, mastTop.add(0, 0, -10), trolley.add(0, 0, 6), 1.4, 1.4, 4, 0xE8B020, 1.0F, true);
			Solids.tube(solid, m, trolley.add(-1.5, -1, -1.5), trolley.add(1.5, 1, 1.5), 1.8, 1.8, 4, 0x2A2A34, 1.0F, true);
			if (a < 150) {
				Vec3d core = corePos(a).subtract(base).add(bs);
				Solids.tube(solid, m, trolley, core.add(0, 3.4, 0), 0.25, 0.25, 4, 0x8A8A9A, 1.0F, true);
				float pulse = 0.8F + 0.2F * MathHelper.sin(a * 0.6F);
				Solids.tube(solid, m, core.add(0, 2.6, 0), core.add(0, 3.4, 0), 2.2, 1.0, 8, 0x2A2A34, 1.0F, true);
				for (int k = 0; k < 4; k++) {
					double ang = k * Math.PI / 2 + a * 0.05;
					Solids.tube(solid, m, core.add(Math.cos(ang) * 2.2, 2.6, Math.sin(ang) * 2.2), core.add(Math.cos(ang) * 2.2, -2.6, Math.sin(ang) * 2.2), 0.25, 0.25, 4, 0x6A6A7A, 1.0F, true);
				}
				FxDraw.sphere(glow, m, core, 2.0, Vec3d.ZERO, 0xFFFFFF, 0.95F * pulse, 0.6F, 8, 12);
				FxDraw.sphere(glow, m, core, 4.2, Vec3d.ZERO, 0x60FFE0, 0.4F * pulse, 0.0F, 8, 12);
				Solids.dot(glow, m, core, cb, 12, 0x60FFE0, 0.25F * pulse);
			}
			// Clamps: four jaws round the nose collar, swinging shut at 150.
			double shut = ease((a - 148) / 4.0);
			for (int q = 0; q < 4; q++) {
				double ang = q * Math.PI / 2 + Math.PI / 4;
				Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
				Vec3d jaw = bs.add(out.multiply(RR * (1.02 + (1 - shut) * 0.8))).add(0, LEN * 0.72, 0);
				Solids.tube(solid, m, jaw.add(0, -1.6, 0), jaw.add(0, 1.6, 0), 1.2, 1.2, 4, 0xE8B020, 1.0F, true);
				float spark = (float) Math.max(0, 1 - Math.abs(a - 151) / 5.0);
				if (spark > 0) {
					Solids.flare(glow, m, jaw, cb, 4, 0xFFE0A0, spark);
					for (int i = 0; i < 6; i++) {
						Vec3d d = out.multiply(1.5).add(sphereDir(i + q * 6, 31)).normalize();
						FxDraw.ribbon(glow, m, jaw, jaw.add(d.multiply(3 + (a - 148) * 1.2)), Vec3d.ZERO, 0.3, 0xFFC060, spark, 0.0F);
					}
				}
			}
			// Drones swarm the hull, welding as they go, then scatter before ignition.
			if (a > 150 && a < 240) {
				for (int i = 0; i < 12; i++) {
					Vec3d dp = drone(i, a).subtract(base).add(bs);
					Vec3d[] db = {new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)};
					Solids.tube(solid, m, dp.add(0, -0.4, 0), dp.add(0, 0.4, 0), 0.9, 0.7, 6, 0x2A2A34, 1.0F, true);
					for (int r = 0; r < 4; r++) {
						Vec3d rp = dp.add(db[r % 2].multiply(r < 2 ? 1.4 : -1.4));
						FxDraw.disc(glow, m, rp.add(0, 0.5, 0), db[0], db[1], 0.7, 0xA0E0FF, 0.25F, 0.0F, 8);
					}
					Solids.dot(glow, m, dp.add(0, 0.6, 0), cb, 0.5, ((int) (a * 0.3F + i) & 1) == 0 ? 0xFF3030 : 0x40FF80, 0.9F);
					if (a < 212 && Solids.hash(i, (int) (a * 0.5F)) > 0.6) {
						Vec3d hit = new Vec3d(dp.x - bs.x, 0, dp.z - bs.z).normalize().multiply(RR).add(bs.x, dp.y, bs.z);
						FxDraw.ribbon(glow, m, dp, hit, Vec3d.ZERO, 0.25, 0x80E0FF, 0.9F, 0.9F);
						Solids.flare(glow, m, hit, cb, 1.6, 0xC0F0FF, 0.9F);
					}
				}
			}
			// Steam venting from the hull and from under the pad as the tanks top off.
			if (a > 192) {
				float vent = (float) clamp01((a - 192) / 12.0);
				for (int i = 0; i < 24; i++) {
					double life = ((a * 0.9 + i * 7.3) % 40) / 40.0;
					double ang = Solids.hash(i, 71) * MathHelper.TAU;
					boolean pad = i % 3 != 0;
					Vec3d src = pad ? bs.add(Math.cos(ang) * 10, 1, Math.sin(ang) * 10) : bs.add(Math.cos(ang) * RR, 20 + (i % 4) * 9, Math.sin(ang) * RR);
					Vec3d p = src.add(Math.cos(ang) * life * (pad ? 20 : 10), life * (pad ? 10 : 4), Math.sin(ang) * life * (pad ? 20 : 10));
					Solids.dot(shade, m, p, cb, 2 + life * (pad ? 10 : 5), 0xE8E8F0, vent * 0.55F * (float) (1 - life));
				}
			}
			// Pad-corner beacons, flashing with each beep.
			if (a > 150) {
				for (int q = 0; q < 4; q++) {
					double ang = q * Math.PI / 2 + Math.PI / 4;
					Vec3d p = bs.add(Math.cos(ang) * 25, 2.5, Math.sin(ang) * 25);
					Solids.tube(solid, m, p.add(0, -1, 0), p, 0.6, 0.6, 4, 0x2A2A34, 1.0F, true);
					Solids.dot(glow, m, p.add(0, 0.6, 0), cb, 1 + 3 * flash, 0xFF3030, 0.4F + 0.6F * flash);
				}
			}
		}
	}
}
