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

	/** One giant rocket climbs, bursts, and sets off a cascade of fireworks at every angle. */
	public static final class Havoc {
		public static final int CLIMB = 50, CASCADE_END = 120, GROUND = 60, GROUND_END = 170, FINALE = 175, END = 260;
		public static final double APEX = 95;

		private Havoc() {
		}

		/** The rocket's height above the pad at tick {@code t}: slow off the pad, then screaming. */
		public static double height(double t) {
			return 2 + (APEX - 2) * Math.pow(MathHelper.clamp(t / CLIMB, 0.0, 1.0), 1.7);
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
}
