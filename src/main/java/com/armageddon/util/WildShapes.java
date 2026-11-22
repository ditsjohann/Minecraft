package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the goo bomb, the cactus, the Blackforge and the Blue Infrit, shared by the server events
 * (which edit the world) and the client effects (which draw the models), so the two always agree on where things are.
 */
public final class WildShapes {
	private WildShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	// ====================================================================== GOO BOMB

	/** An alien goo grows, throbbing, then bursts into acid globs that eat what they land on. */
	public static final class Goo {
		public static final int BURST = 140, END = 320;
		public static final double R_MAX = 5.5;
		/** Height of the blob's centre above the bomb. */
		public static final double LIFT = 3;
		public static final int GLOBS = 70;
		/** Glob fall, blocks per tick per tick (the client's particle uses the same number). */
		public static final double GRAVITY = 0.06;

		private Goo() {
		}

		/** Blob radius at tick {@code t}: slow at first, then swelling faster and faster, with a wobble. */
		public static double radius(double t) {
			double u = MathHelper.clamp(t / BURST, 0.0, 1.0);
			return 1.2 + (R_MAX - 1.2) * Math.pow(u, 1.6) + 0.25 * Math.sin(t * 0.45) * u;
		}
	}

	// ====================================================================== CACTUS

	/**
	 * A colossal saguaro grows out of the ground, starts to turn, spins faster and faster, and flings its spikes off
	 * in every direction. When it stops it sets into real blocks.
	 */
	public static final class Cactus {
		public static final int RISE = 10, RISEN = 100, SPIN = 110, FIRE = 160, FIRE_END = 340, STOP = 390, SOLID = 400, END = 420;
		public static final double TRUNK_H = 34, TRUNK_R = 3.2, ARM_R = 2.2;
		/** Arms: side angle (radians), height where it joins, how far out it reaches, how far up it then grows. */
		public static final double[][] ARMS = {{0.4, 13, 7.5, 11}, {0.4 + Math.PI, 18, 6.5, 9}};
		public static final int TRUNK_SPIKES = 64, ARM_SPIKES = 18;
		public static final int SPIKES = TRUNK_SPIKES + ARM_SPIKES * 2;
		public static final double GRAVITY = 0.07;
		public static final double MAX_SPIN = 0.22;

		private Cactus() {
		}

		/** How far it has grown out of the ground (0..1). */
		public static double risen(double t) {
			return smooth((t - RISE) / (RISEN - RISE));
		}

		/** Spin speed in radians per tick: still, winding up, flat out while it fires, then slowing to a stop. */
		public static double omega(double t) {
			if (t < SPIN || t >= STOP) {
				return 0;
			}
			if (t < FIRE) {
				return 0.06 * smooth((t - SPIN) / (FIRE - SPIN));
			}
			if (t < FIRE + 90) {
				return 0.06 + (MAX_SPIN - 0.06) * smooth((t - FIRE) / 90.0);
			}
			if (t < FIRE_END) {
				return MAX_SPIN;
			}
			return MAX_SPIN * (1 - smooth((t - FIRE_END) / (STOP - FIRE_END)));
		}

		/** Total turn (radians) by tick {@code t}, summed tick by tick so server and client agree exactly. */
		public static double angle(double t) {
			double a = 0;
			int whole = (int) Math.floor(t);
			for (int k = SPIN; k < Math.min(whole, STOP); k++) {
				a += omega(k);
			}
			return a + omega(whole) * (t - whole);
		}

		/** Tick on which spike {@code i} flies off. */
		public static int fireTick(int i) {
			return FIRE + (int) (SciShapes.hash(i, 77) * (FIRE_END - FIRE));
		}

		/** Spike {@code i}'s root on the unturned cactus (relative to its base), and the way it points ({@code out}). */
		public static Vec3d spike(int i, double[] out) {
			double golden = 2.399963;
			double px, py, pz, dx, dz;
			if (i < TRUNK_SPIKES) {
				double a = i * golden;
				py = 3 + (i + 0.5) / TRUNK_SPIKES * (TRUNK_H - 6);
				dx = Math.cos(a);
				dz = Math.sin(a);
				px = dx * TRUNK_R;
				pz = dz * TRUNK_R;
			} else {
				int k = (i - TRUNK_SPIKES) / ARM_SPIKES, j = (i - TRUNK_SPIKES) % ARM_SPIKES;
				double[] arm = ARMS[k];
				double ex = Math.cos(arm[0]) * arm[2], ez = Math.sin(arm[0]) * arm[2];
				double a = j * golden + k;
				py = arm[1] + 1.5 + (j + 0.5) / ARM_SPIKES * (arm[3] - 1.5);
				dx = Math.cos(a);
				dz = Math.sin(a);
				px = ex + dx * ARM_R;
				pz = ez + dz * ARM_R;
			}
			double dy = (SciShapes.hash(i, 3) - 0.5) * 0.4;
			double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
			out[0] = dx / l;
			out[1] = dy / l;
			out[2] = dz / l;
			return new Vec3d(px, py, pz);
		}

		/** Turns a point about the cactus' vertical axis. */
		public static Vec3d turn(Vec3d v, double a) {
			double c = Math.cos(a), s = Math.sin(a);
			return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
		}

		/** Launch speed of spike {@code i} (blocks per tick, before the spin's own throw is added). */
		public static double speed(int i) {
			return 0.7 + 0.4 * SciShapes.hash(i, 11);
		}
	}

	// ====================================================================== BLACKFORGE

	/**
	 * Under the world there is a forge. The ground cracks; a black anvil rises; a hammer the size of a tower comes
	 * down out of the sky and strikes, four times. Each blow hammers a wider ring of the land flat into slag.
	 */
	public static final class Forge {
		public static final int CRACK = 0, RISE = 30, RISEN = 100, HAMMER_IN = 100;
		public static final int[] STRIKES = {170, 230, 290, 370};
		/** Outer radius of the land each blow hammers flat (each ring starts where the last ended). */
		public static final double[] BANDS = {22, 42, 62, 85};
		public static final int RAIN = 380, RAIN_END = 470, LIFT = 470, END = 540;
		public static final double ANVIL_H = 22, HAMMER_UP = 55;
		/** The hammer head's half-extents: width (x), height (y) and length (z, along the anvil). */
		public static final double HEAD_W = 7, HEAD_H = 7, HEAD_L = 12, HANDLE_L = 70;

		private Forge() {
		}

		/** How far the anvil has risen out of the ground (0..1); it sinks again at the end. */
		public static double risen(double t) {
			return smooth((t - RISE) / (RISEN - RISE)) * (1 - smooth((t - LIFT - 20) / 50.0));
		}

		/** Gap between the hammer head and the anvil's face at tick {@code t} (0 = struck; huge = out of sight). */
		public static double lift(double t) {
			if (t < HAMMER_IN) {
				return 400;
			}
			int first = STRIKES[0];
			if (t < first - 14) {
				return HAMMER_UP + 300 * (1 - smooth((t - HAMMER_IN) / (first - 14 - HAMMER_IN)));
			}
			for (int k = 0; k < STRIKES.length; k++) {
				int s = STRIKES[k];
				if (t < s - 14) {
					// Lifting back up after the last blow, then held high.
					int prev = STRIKES[k - 1];
					return HAMMER_UP * smooth((t - prev) / 30.0);
				}
				if (t < s) {
					double u = (t - (s - 14)) / 14.0;
					return HAMMER_UP * (1 - u * u);
				}
			}
			if (t < LIFT) {
				return 0;
			}
			return 400 * smooth((t - LIFT) / 60.0);
		}

		/** Which blow is landing on tick {@code t}, or -1. */
		public static int strikeAt(int t) {
			for (int k = 0; k < STRIKES.length; k++) {
				if (STRIKES[k] == t) {
					return k;
				}
			}
			return -1;
		}
	}

	// ====================================================================== BLUE INFRIT

	/**
	 * A summoning circle burns itself into the ground in blue fire; the summoning rises; a vast hound's head of blue
	 * flame bursts out, circles, and dives on the land again and again, each time leaving a burning pit.
	 */
	public static final class Infrit {
		public static final int DRAWN = 170, SUMMON = 170, EMERGE = 270, DIVE0 = 330, DIVES = 6, DIVE_GAP = 70;
		public static final int BURST = DIVE0 + DIVES * DIVE_GAP + 20, END = BURST + 90;
		/** Circle radii: the outer ring, the inner ring (which the star is drawn in). */
		public static final double R = 42, R_IN = 31;
		public static final double HOVER = 45, SWEEP = 145;
		/** Where along the path each part starts: outer ring, inner ring, then the five strokes of the star. */
		static final double OUTER_END = 0.34, INNER_END = 0.56;

		private Infrit() {
		}

		/** How much of the circle is drawn by tick {@code t} (0..1). */
		public static double drawn(double t) {
			return MathHelper.clamp(t / DRAWN, 0.0, 1.0);
		}

		/** A point of the summoning circle's path at {@code u} (0..1), as a ground offset (y = 0). */
		public static Vec3d pen(double u) {
			if (u < OUTER_END) {
				double a = u / OUTER_END * Math.PI * 2 - Math.PI / 2;
				return new Vec3d(Math.cos(a) * R, 0, Math.sin(a) * R);
			}
			if (u < INNER_END) {
				double a = -(u - OUTER_END) / (INNER_END - OUTER_END) * Math.PI * 2 - Math.PI / 2;
				return new Vec3d(Math.cos(a) * R_IN, 0, Math.sin(a) * R_IN);
			}
			// The star: five strokes joining every second point of the inner ring.
			double s = (u - INNER_END) / (1 - INNER_END) * 5;
			int k = Math.min(4, (int) s);
			double f = s - k;
			Vec3d a = star(k * 2), b = star(k * 2 + 2);
			return a.lerp(b, f);
		}

		/** Point {@code k} (mod 5) of the star on the inner ring. */
		public static Vec3d star(int k) {
			double a = -Math.PI / 2 + (k % 5) * Math.PI * 2 / 5;
			return new Vec3d(Math.cos(a) * R_IN, 0, Math.sin(a) * R_IN);
		}

		/** Where dive {@code i} lands, as a ground offset from the circle's centre (y = 0). */
		public static Vec3d target(int i, int seed) {
			double a = SciShapes.hash(i * 31 + 7, seed) * Math.PI * 2;
			double d = 45 + SciShapes.hash(i * 17 + 3, seed + 1) * 95;
			return new Vec3d(Math.cos(a) * d, 0, Math.sin(a) * d);
		}

		/** Tick on which dive {@code i} hits the ground. */
		public static int impact(int i) {
			return DIVE0 + i * DIVE_GAP + 45;
		}

		/**
		 * The head's position (relative to the circle's centre) at tick {@code t}. {@code ground} gives the height of
		 * the ground at an offset (relative to the centre), so the dives land on the terrain.
		 */
		public static Vec3d head(double t, int seed, java.util.function.ToDoubleBiFunction<Double, Double> ground) {
			Vec3d hover = new Vec3d(0, HOVER, 0);
			if (t < EMERGE) {
				return new Vec3d(0, -10, 0);
			}
			if (t < DIVE0) {
				double u = smooth((t - EMERGE) / (DIVE0 - EMERGE));
				// Rises out of the circle in a slow spiral.
				double a = u * Math.PI * 2;
				return new Vec3d(Math.cos(a) * 8 * (1 - u), -10 + (HOVER + 10) * u, Math.sin(a) * 8 * (1 - u));
			}
			int i = (int) ((t - DIVE0) / DIVE_GAP);
			if (i >= DIVES) {
				Vec3d last = rebound(DIVES - 1, seed, ground);
				double u = smooth((t - (DIVE0 + DIVES * DIVE_GAP)) / 20.0);
				return last.lerp(hover.add(0, 30, 0), u);
			}
			double local = t - (DIVE0 + i * DIVE_GAP);
			Vec3d from = i == 0 ? hover : rebound(i - 1, seed, ground);
			Vec3d tg = target(i, seed);
			Vec3d hit = tg.add(0, ground.applyAsDouble(tg.x, tg.z), 0);
			Vec3d out = new Vec3d(tg.x, 0, tg.z).normalize();
			// Swings round to come in from the far side, high, then drops on it.
			Vec3d high = hit.add(out.multiply(-25)).add(0, 40, 0);
			if (local < 35) {
				double u = smooth(local / 35.0);
				Vec3d mid = from.lerp(high, 0.5).add(out.crossProduct(new Vec3d(0, 1, 0)).multiply(30));
				return bezier(from, mid, high, u);
			}
			if (local < 45) {
				double u = (local - 35) / 10.0;
				return high.lerp(hit, u * u);
			}
			double u = smooth((local - 45) / 25.0);
			return hit.lerp(rebound(i, seed, ground), u);
		}

		/** Where the head pulls up to after dive {@code i}. */
		static Vec3d rebound(int i, int seed, java.util.function.ToDoubleBiFunction<Double, Double> ground) {
			Vec3d tg = target(i, seed);
			return tg.multiply(0.6).add(0, ground.applyAsDouble(tg.x, tg.z) + 38, 0);
		}

		static Vec3d bezier(Vec3d a, Vec3d b, Vec3d c, double u) {
			return a.multiply((1 - u) * (1 - u)).add(b.multiply(2 * u * (1 - u))).add(c.multiply(u * u));
		}

		/** How wide the hound's jaw is open (0..1): it opens as it dives and snaps shut on the ground. */
		public static double jaw(double t) {
			if (t < DIVE0) {
				return t < EMERGE ? 0 : 0.6 * smooth((t - EMERGE) / 30.0);
			}
			double local = (t - DIVE0) % DIVE_GAP;
			if (local < 30) {
				return 0.2;
			}
			if (local < 45) {
				return smooth((local - 30) / 10.0);
			}
			return 0.2 + 0.8 * (1 - smooth((local - 45) / 6.0));
		}
	}
}
