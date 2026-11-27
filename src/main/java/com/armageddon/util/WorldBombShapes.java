package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Timings and geometry for two WORLD DESTRUCTION bombs, shared by the server events and the client effects: the
 * MEGA CREEPER (a creeper the height of a mountain that swells, flashes and goes off, raining creepers over the land)
 * and the ELDER WYRM (a dragon of black scale and magma that breathes rivers of fire across the world). Points are in
 * the bomb's heading frame (x forward, y up, z right; see {@link FossilShapes#frame}), relative to its ground.
 */
public final class WorldBombShapes {
	private WorldBombShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		return FossilShapes.hash(i + 503, k + 877);
	}

	// ====================================================================== MEGA CREEPER

	/**
	 * A creeper as tall as a mountain rises out of the ground and stares; it hisses, swelling and flashing white
	 * faster and faster, and goes off. The blast rains real creepers over the world, and its face is left pressed into
	 * the floor of the crater.
	 */
	public static final class Creeper {
		public static final int RISE = 20, RISEN = 150, HISS = 250, BOOM = 350, RAIN = 362, RAIN_END = 470, FACE = 470, END = 640;
		/** Blocks to a pixel of the creeper's model: 26 pixels tall makes it 104 blocks. */
		public static final double PX = 4;
		/** The crater, the blast's reach, and how many creepers come down. */
		public static final double CRATER = 70, R = 300;
		public static final int CREEPERS = 180;
		/** The face pressed into the crater floor: blocks to a pixel of it. */
		public static final int FACE_PX = 7;

		private Creeper() {
		}

		/** How far up out of the ground it has come (0 buried, 1 standing). */
		public static double risen(double t) {
			return smooth((t - RISE) / (double) (RISEN - RISE));
		}

		/** How far into its fuse it is (0 calm, 1 going off). */
		public static double fuse(double t) {
			return MathHelper.clamp((t - HISS) / (double) (BOOM - HISS), 0.0, 1.0);
		}

		/** True on the ticks it flashes white: slow blinks at first, then faster and faster. */
		public static boolean flash(double t) {
			double f = fuse(t);
			if (f <= 0) {
				return false;
			}
			// Blink phase speeds up as the fuse runs down.
			double phase = (t - HISS) * (0.06 + 0.5 * f * f);
			return (phase % 1.0) < 0.5;
		}

		/** The creeper face, 8 x 8, row 0 at the top: true where it is black. */
		public static boolean face(int x, int y) {
			boolean eye = (y == 2 || y == 3) && (x == 1 || x == 2 || x == 5 || x == 6);
			boolean nose = y == 4 && (x == 3 || x == 4);
			boolean mouth = (y == 5 || y == 6) && x >= 2 && x <= 5 || y == 7 && (x == 2 || x == 5);
			return eye || nose || mouth;
		}

		/** Creeper {@code i} of the rain: where it falls (heading frame) and when. */
		public static double[] drop(int i) {
			double a = hash(i, 1) * Math.PI * 2, r = 40 + Math.sqrt(hash(i, 2)) * (R - 50);
			return new double[]{Math.cos(a) * r, Math.sin(a) * r, RAIN + (RAIN_END - RAIN) * (i / (double) CREEPERS)};
		}
	}

	// ====================================================================== ELDER WYRM

	/**
	 * A dragon of black scale and magma breaks out from under the land, roars, takes to the sky and circles; it makes
	 * three passes breathing rivers of fire that carve canyons across the world, hovers and beats down a gale that
	 * flattens the forests, lands, and breathes a nova of fire straight down. Then it climbs into the red sky and is gone.
	 */
	public static final class Wyrm {
		public static final int SPLIT = 40, EMERGE = 60, ROAR = 185, TAKEOFF = 200, HOVER = 980, LAND = 1075, CHARGE = 1110, NOVA = 1170,
				DEPART = 1190, END = 1340;
		/** The three passes: when each starts, how long it lasts, and the breath along it. */
		public static final int[] RUNS = {340, 550, 760};
		public static final int RUN = 120;
		public static final double RUN_HALF = 220, RUN_Y = 40;
		/** The wingbeats of the gale, and how far it flattens. */
		public static final int[] BEATS = {995, 1015, 1035, 1055};
		public static final double GALE = 150;
		/** The nova: crater and firestorm. */
		public static final double CRATER = 46, FIRESTORM = 120, NOVA_AHEAD = 28;
		/** The canyon its breath carves, half its width and depth; the scorch either side. */
		public static final double CANYON_W = 5.5, CANYON_D = 9, SCORCH = 13;
		/** The fissure it breaks out of. */
		public static final double SPLIT_LEN = 140;
		/** Standing, its body's middle is this high. */
		public static final double STAND = 18;
		/** Size: blocks to a unit of the pose (body about 40 long, wings about 180 across). */
		public static final double SC = 3.0;

		private Wyrm() {
		}

		/** Pass {@code k}'s direction (heading frame, unit, horizontal). */
		public static Vec3d runDir(int k) {
			// Each pass comes back the other way, swung round a radian: the three canyons fan out like a star.
			double a = 0.35 + k * (Math.PI + 1.0);
			return new Vec3d(Math.cos(a), 0, Math.sin(a));
		}

		private record Key(double t, Vec3d p) {
		}

		private static final List<Key> KEYS = keys();

		private static List<Key> keys() {
			List<Key> k = new ArrayList<>();
			k.add(new Key(EMERGE - 40, new Vec3d(-70, -45, 0)));
			k.add(new Key(EMERGE, new Vec3d(-60, -40, 0)));
			k.add(new Key(EMERGE + 60, new Vec3d(-25, -6, 0)));
			k.add(new Key(ROAR - 15, new Vec3d(0, STAND, 0)));
			k.add(new Key(TAKEOFF, new Vec3d(2, STAND + 1, 0)));
			k.add(new Key(TAKEOFF + 45, new Vec3d(-40, 65, -15)));
			for (int r = 0; r < RUNS.length; r++) {
				Vec3d d = runDir(r);
				Vec3d start = d.multiply(-RUN_HALF).add(0, RUN_Y, 0), end = d.multiply(RUN_HALF).add(0, RUN_Y, 0);
				if (r == 0) {
					k.add(new Key(RUNS[0] - 40, d.multiply(-RUN_HALF - 60).add(0, 80, 0)));
				} else {
					// Swinging round between passes, out wide and high.
					Vec3d prevEnd = runDir(r - 1), nextStart = d.multiply(-1);
					Vec3d mid = prevEnd.add(nextStart).normalize().multiply(300);
					k.add(new Key((RUNS[r - 1] + RUN + RUNS[r]) / 2.0, mid.add(0, 90, 0)));
				}
				for (int q = 0; q <= 4; q++) {
					k.add(new Key(RUNS[r] + RUN * q / 4.0, start.lerp(end, q / 4.0)));
				}
			}
			Vec3d last = runDir(RUNS.length - 1);
			double la = Math.atan2(last.z, last.x) + 1.0;
			k.add(new Key(RUNS[RUNS.length - 1] + RUN + 50, new Vec3d(Math.cos(la) * 200, 75, Math.sin(la) * 200)));
			k.add(new Key(HOVER, new Vec3d(0, 55, 0)));
			k.add(new Key(BEATS[BEATS.length - 1] + 5, new Vec3d(0, 52, 0)));
			k.add(new Key(LAND, new Vec3d(0, STAND, 0)));
			k.add(new Key(NOVA + 10, new Vec3d(0, STAND, 0)));
			k.add(new Key(DEPART + 20, new Vec3d(-5, STAND + 4, 0)));
			k.add(new Key(DEPART + 60, new Vec3d(30, 70, 0)));
			k.add(new Key(DEPART + 110, new Vec3d(120, 170, 0)));
			k.add(new Key(END, new Vec3d(260, 330, 0)));
			k.add(new Key(END + 40, new Vec3d(320, 400, 0)));
			return List.copyOf(k);
		}

		/** Where its body is at tick {@code t}: a Catmull-Rom spline through the keyframes. */
		public static Vec3d pos(double t) {
			List<Key> k = KEYS;
			if (t <= k.get(0).t) {
				return k.get(0).p;
			}
			for (int i = 0; i < k.size() - 1; i++) {
				Key a = k.get(i), b = k.get(i + 1);
				if (t <= b.t) {
					Vec3d p0 = k.get(Math.max(0, i - 1)).p, p1 = a.p, p2 = b.p, p3 = k.get(Math.min(k.size() - 1, i + 2)).p;
					double u = (t - a.t) / Math.max(1.0E-6, b.t - a.t);
					return catmull(p0, p1, p2, p3, u);
				}
			}
			return k.get(k.size() - 1).p;
		}

		private static Vec3d catmull(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double u) {
			double u2 = u * u, u3 = u2 * u;
			return p1.multiply(2).add(p2.subtract(p0).multiply(u)).add(p0.multiply(2).subtract(p1.multiply(5)).add(p2.multiply(4)).subtract(p3).multiply(u2))
					.add(p1.multiply(3).subtract(p0).subtract(p2.multiply(3)).add(p3).multiply(u3)).multiply(0.5);
		}

		/** True on the ground (emerging, before takeoff, landed). */
		public static boolean grounded(double t) {
			return t < TAKEOFF + 8 || t >= LAND - 4 && t < DEPART + 15;
		}

		/** Which pass it is breathing on at tick {@code t}, or -1. */
		public static int breathing(double t) {
			for (int k = 0; k < RUNS.length; k++) {
				if (t >= RUNS[k] + 8 && t < RUNS[k] + RUN - 8) {
					return k;
				}
			}
			return -1;
		}

		/** Where pass {@code k}'s breath strikes the ground at tick {@code t} (heading frame, y 0): ahead of the head. */
		public static Vec3d breathHit(int k, double t) {
			Vec3d p = pos(t);
			return new Vec3d(p.x, 0, p.z).add(runDir(k).multiply(22));
		}

		/** How far the gale has flattened at tick {@code t}. */
		public static double gale(double t) {
			double f = 0;
			for (int i = 0; i < BEATS.length; i++) {
				f = Math.max(f, (i + smooth((t - BEATS[i]) / 14.0)) / BEATS.length * (t >= BEATS[i] ? 1 : 0));
			}
			return GALE * f;
		}

		/** How hot its chest has grown as it gathers the nova (0..1). */
		public static double charge(double t) {
			return smooth((t - CHARGE) / (double) (NOVA - CHARGE)) * (1 - smooth((t - NOVA - 4) / 10.0));
		}

		/** How far the firestorm has spread at tick {@code t}. */
		public static double firestorm(double t) {
			double u = MathHelper.clamp((t - NOVA) / 60.0, 0.0, 1.0);
			return FIRESTORM * (1 - (1 - u) * (1 - u));
		}

		/** How wide its jaws are open. */
		public static double jaw(double t) {
			double g = Math.max(bump(t, ROAR - 6, ROAR + 30), charge(t) * 0.6 + bump(t, NOVA - 6, NOVA + 40));
			if (breathing(t) >= 0) {
				g = Math.max(g, 0.85);
			}
			return Math.min(1, g);
		}

		static double bump(double t, double a, double b) {
			return t <= a || t >= b ? 0 : Math.sin(Math.PI * (t - a) / (b - a));
		}
	}
}
