package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the Dimension Shift, the Firework Havoc and the Chinese Lantern, shared by the server
 * events and the client effects.
 */
public final class WonderShapes {
	private WonderShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	// ====================================================================== DIMENSION SHIFT

	/** A rift opens and the place it stands in is shifted into the Nether, outward from the centre. */
	public static final class Shift {
		public static final int RIFT = 40, SWEEP_END = 280, CLOSE = 300, END = 1500;
		public static final double R = 56, DEPTH = 14, HEIGHT = 40;

		private Shift() {
		}

		/** How far out the shift has reached at tick {@code t}. */
		public static double front(double t) {
			return R * smooth((t - RIFT) / (SWEEP_END - RIFT));
		}
	}

	// ====================================================================== FIREWORK HAVOC

	/** One giant rocket climbs slowly, bursts, and sets off a cascade of fireworks at every angle. */
	public static final class Havoc {
		public static final int CLIMB = 130, CASCADE_END = 200, GROUND = 140, GROUND_END = 250, FINALE = 255, END = 340;
		public static final double APEX = 95;
		/** The giant rocket's body length, radius and the length of its guide stick. */
		public static final double BODY = 12, RADIUS = 1.6, STICK = 16;

		private Havoc() {
		}

		/** The rocket's height above the pad at tick {@code t}: a slow, stately climb that gathers pace. */
		public static double height(double t) {
			double u = MathHelper.clamp(t / CLIMB, 0.0, 1.0);
			return 2 + (APEX - 2) * (0.35 * u + 0.65 * u * u);
		}
	}

	// ====================================================================== CHINESE LANTERN

	/** Lanterns fill the sky while a golden dragon flies over them, and at the end it climbs and bursts into gold. */
	public static final class Lantern {
		public static final int RISE = 600, CLIMB = 520, BURST = 560, END = 720;
		public static final double ORBIT = 34, HEIGHT = 30;

		private Lantern() {
		}

		/** The dragon's head at tick {@code t}, relative to the bomb: a slow sinuous orbit, then a climb. */
		public static Vec3d head(double t) {
			double a = t * 0.018;
			Vec3d p = new Vec3d(Math.cos(a) * ORBIT + Math.cos(3 * a) * 7, HEIGHT + 7 * Math.sin(2 * a) + 3 * Math.sin(5 * a),
					Math.sin(a) * ORBIT + Math.sin(2 * a) * 9);
			if (t > CLIMB) {
				double u = smooth((t - CLIMB) / (BURST - CLIMB));
				double r = 1 - u * 0.8;
				p = new Vec3d(p.x * r, p.y + 70 * u * u, p.z * r);
			}
			return p;
		}
	}

	// ====================================================================== AURORA

	/**
	 * Aurora curtains over the bomb. Each curtain is a long arc of vertical rays standing on a bright hem; it unrolls
	 * high overhead, dances, comes down until its hem touches the land, sweeps round painting it, then spirals into a
	 * single column. The hem's path is shared, so the server burns exactly where the client draws it touching.
	 */
	public static final class Veil {
		public static final int CURTAINS = 5, NIGHT = 60, UNROLL = 200, DESCEND = 360, TOUCH = 440, SWEEP_END = 540, CONVERGE = 600,
				STORM = 600, END = 740;
		public static final double HIGH = 78, RAY = 52, R0 = 42;
		/** Each curtain's hem and crown colours. */
		public static final int[] HEM = {0x50FF90, 0x40F0C0, 0xFF60C0, 0x60FF70, 0x40D0FF};
		public static final int[] CROWN = {0xA040FF, 0x8050FF, 0xFF4080, 0xB040FF, 0x7040FF};

		private Veil() {
		}

		/** How far along its length curtain {@code k} has unrolled (0..1). */
		public static double unrolled(double t) {
			return smooth((t - NIGHT) / (UNROLL - NIGHT));
		}

		/** Height of the hem above the bomb at tick {@code t}. */
		public static double hem(double t) {
			if (t < DESCEND) {
				return HIGH;
			}
			return HIGH * (1 - smooth((t - DESCEND) / (TOUCH - DESCEND)));
		}

		/** True once the hems are on the ground and burning it. */
		public static boolean touching(double t) {
			return t >= TOUCH - 4 && t < CONVERGE;
		}

		/** Curtain {@code k}'s hem at {@code s} (0..1 along its length), relative to the bomb, at hem height. */
		public static Vec3d at(int k, double s, double t) {
			double sweep = t > TOUCH ? (t - TOUCH) * 0.012 : 0;
			double conv = smooth((t - SWEEP_END) / (CONVERGE - SWEEP_END));
			double theta = k * MathHelper.TAU / CURTAINS + 0.4 * Math.sin(k * 1.7) + (s - 0.5) * 2.1 * (1 - conv * 0.6) + sweep + conv * 2.5 * s;
			double fold = Math.sin(s * 9 + t * 0.03 + k) * 3 + Math.sin(s * 23 - t * 0.05 + k * 2) * 1.2;
			double r = (R0 + 30 * hash(k, 3) + 12 * Math.sin(s * 3.1 + k) + fold) * (1 - conv * 0.9) + 3 * conv;
			return new Vec3d(Math.cos(theta) * r, hem(t), Math.sin(theta) * r);
		}

		/** Brightness of the ray at {@code s} on curtain {@code k}: travelling pulses, the aurora's dance. */
		public static double pulse(int k, double s, double t) {
			double a = 0.55 + 0.25 * Math.sin(s * 14 - t * 0.12 + k) + 0.2 * Math.sin(s * 31 + t * 0.21 + k * 3);
			return MathHelper.clamp(a, 0.1, 1.0);
		}
	}
}
