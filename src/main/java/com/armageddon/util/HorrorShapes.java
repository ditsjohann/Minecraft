package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Timings and geometry for the three horror celestials (the Terror Maniac, the Pumpkin Horseman and the Portal of
 * Hell), shared by the server events and the client effects.
 */
public final class HorrorShapes {
	private HorrorShapes() {
	}

	public static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	// ====================================================================== TERROR MANIAC

	/**
	 * The world turns inside out: blood-black sky, red lightning, veins crawling over the ground. A towering thing of
	 * roots and sinew with a cracked white mask rises out of a rift, lifts everything living into the air, holds it
	 * there twisting, and then breaks it. It shrieks, comes apart into a storm of moths, and the land rots.
	 */
	public static final class Maniac {
		public static final int DREAD = 0, VEINS = 100, VEINS_END = 400, RISE = 380, RISEN = 500, CURSE = 520, SNAP = 880, SHRIEK = 940,
				SHATTER = 1000, END = 1120;
		/** How far the veins and the rot reach, and how tall the thing stands. */
		public static final double R = 80, ROT = 110, TALL = 46;
		static final int MAIN = 9;

		private Maniac() {
		}

		/** How far along their length the veins have crawled (in blocks). */
		public static double reach(double t) {
			return R * smooth((t - VEINS) / (VEINS_END - VEINS));
		}

		/** How far the thing has risen out of the rift (0..1). */
		public static double risen(double t) {
			return smooth((t - RISE) / (RISEN - RISE));
		}

		/**
		 * The veins: a few main ones crawling out from the rift, wandering, with side branches. Each point is
		 * {x, z, arc length from the rift, width}, relative to the bomb.
		 */
		public static List<double[]> veins(int seed) {
			List<double[]> out = new ArrayList<>();
			for (int k = 0; k < MAIN; k++) {
				double ang = k * MathHelper.TAU / MAIN + (hash(seed + k, 1) - 0.5) * 0.5;
				grow(out, seed * 31 + k, 4 * Math.cos(ang), 4 * Math.sin(ang), ang, 4, R, 2.2, true);
			}
			return out;
		}

		private static void grow(List<double[]> out, int salt, double x, double z, double ang, double arc, double max, double width, boolean branch) {
			int i = 0;
			while (arc < max && Math.hypot(x, z) < R) {
				ang += (hash(salt, i * 3 + 1) - 0.5) * 0.7;
				x += Math.cos(ang) * 1.5;
				z += Math.sin(ang) * 1.5;
				arc += 1.5;
				double w = Math.max(0.6, width * (1 - arc / (R * 1.3)));
				out.add(new double[]{x, z, arc, w});
				if (branch && i % 7 == 6 && hash(salt, i) < 0.8) {
					double side = hash(salt, i + 50) < 0.5 ? -1 : 1;
					grow(out, salt * 7 + i, x, z, ang + side * (0.6 + hash(salt, i + 9) * 0.5), arc, arc + 10 + hash(salt, i + 4) * 14, w * 0.6, false);
				}
				i++;
			}
		}
	}

	// ====================================================================== PUMPKIN HORSEMAN

	/**
	 * A colossal headless rider on a black horse made of night, gallops round the land, hurls his burning
	 * jack-o'-lantern six times, rears, charges straight through the middle with his scythe, and rides up into the sky.
	 */
	public static final class Horseman {
		public static final int APPEAR = 60, GALLOP = 120, REAR = 760, CHARGE = 790, ASCEND = 840, VANISH = 900, END = 1000;
		public static final int THROWS = 6, THROW0 = 200, THROW_GAP = 80, FLIGHT = 32;
		public static final double ORBIT = 46, OMEGA = 0.026, ASCEND_H = 70, HALLOW = 90;

		private Horseman() {
		}

		/** Tick at which throw {@code k} leaves his hand. */
		public static int throwAt(int k) {
			return THROW0 + k * THROW_GAP;
		}

		private static double orbitAngle(double t) {
			return (Math.max(t, GALLOP) - GALLOP) * OMEGA;
		}

		private static Vec3d orbit(double t) {
			double a = orbitAngle(t);
			return new Vec3d(Math.cos(a) * ORBIT, 0, Math.sin(a) * ORBIT);
		}

		/**
		 * The horse's position at tick {@code t}, relative to the bomb: y is height above the ground under it (0 while it
		 * gallops on the land).
		 */
		public static Vec3d pos(double t) {
			if (t < GALLOP) {
				// Coming out of the ghost fire and swinging onto the circle.
				double u = smooth((t - APPEAR) / (GALLOP - APPEAR));
				return orbit(GALLOP).multiply(u);
			}
			if (t < CHARGE) {
				return orbit(Math.min(t, REAR));
			}
			Vec3d from = orbit(REAR), to = from.multiply(-1);
			Vec3d dir = to.subtract(from).normalize();
			if (t < ASCEND) {
				return from.lerp(to, (t - CHARGE) / (ASCEND - CHARGE));
			}
			double u = (t - ASCEND) / (VANISH - ASCEND);
			return to.add(dir.multiply(u * 50)).add(0, u * u * ASCEND_H, 0);
		}

		/** Which way the horse is going at tick {@code t}. */
		public static Vec3d dir(double t) {
			if (t >= REAR - 1 && t < CHARGE) {
				// Rearing, it turns to face the centre it is about to charge through.
				Vec3d from = orbit(REAR), charge = from.multiply(-1).normalize();
				double a = orbitAngle(REAR);
				Vec3d tangent = new Vec3d(-Math.sin(a), 0, Math.cos(a));
				return tangent.lerp(charge, smooth((t - REAR) / 18.0)).normalize();
			}
			Vec3d d = pos(t + 0.5).subtract(pos(t - 0.5));
			d = new Vec3d(d.x, 0, d.z);
			if (d.lengthSquared() < 1.0E-6) {
				double a = orbitAngle(t);
				return new Vec3d(-Math.sin(a), 0, Math.cos(a));
			}
			return d.normalize();
		}

		/** How far the horse is rearing (0..1). */
		public static double rear(double t) {
			return smooth((t - REAR) / 14.0) * (1 - smooth((t - CHARGE + 6) / 10.0));
		}

		/** Where throw {@code k} lands, relative to the bomb (y = 0). */
		public static Vec3d target(int k, int seed) {
			double a = hash(seed + k, 21) * MathHelper.TAU;
			double r = 12 + hash(seed + k, 22) * 34;
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		/** How much bigger than a horse he is drawn, and the height of his raised hand over the horse's feet. */
		public static final double BIG = 1.5, HAND = 33 * BIG;
	}

	// ====================================================================== PORTAL OF HELL

	/**
	 * A gothic gate of blackstone the height of a cathedral rises out of the ground facing whoever lit it. Its arch
	 * ignites into a vortex of hellfire with an eye in it, souls and fireballs pour out, lava falls from its sill, and a
	 * clawed arm reaches through and slams the ground. Then the eye closes and the portal implodes, leaving the gate
	 * cracked and the land round it scorched.
	 */
	public static final class HellGate {
		public static final int RISE = 60, RISEN = 240, OPEN = 260, EYE = 300, FIRE0 = 360, FIRES = 14, FIRE_GAP = 22, FLIGHT = 30, LAVA = 420,
				HORDE = 440, ARM = 700, REACH = 760, RAISE = 790, SLAM = 800, WITHDRAW = 840, ARM_END = 890, CLOSE = 890, IMPLODE = 925, END = 1040;
		/** Half-width of the opening, height of its straight sides, frame thickness, frame depth, scorch radius. */
		public static final double W = 16, SIDES = 26, T = 4, DEPTH = 2, SCORCH = 72;
		/** Height of the arch's apex, of the eye, and the eye's radius. */
		public static final double APEX = SIDES + W * Math.sqrt(3), EYE_Y = SIDES * 0.8, EYE_R = 8;
		/** How far in front of the gate the fist comes down. */
		public static final double SLAM_AT = 34;

		private HellGate() {
		}

		/** Inside the arch's opening ({@code u} across, {@code v} up from the ground). */
		public static boolean opening(double u, double v) {
			return inside(u, v, 0);
		}

		/** Inside the arch grown outward by {@code grow}: a pointed arch drawn from two circles. */
		public static boolean inside(double u, double v, double grow) {
			if (v < 0 || Math.abs(u) > W + grow) {
				return false;
			}
			if (v <= SIDES) {
				return true;
			}
			double r = 2 * W + grow;
			return Math.hypot(u - W, v - SIDES) <= r && Math.hypot(u + W, v - SIDES) <= r;
		}

		/** Part of the frame: within the outer arch but not the opening. */
		public static boolean frame(double u, double v) {
			return inside(u, v, T) && !opening(u, v);
		}

		/** How far the gate has risen (0..1). */
		public static double risen(double t) {
			return smooth((t - RISE) / (RISEN - RISE));
		}

		/** How open the portal is (0..1): igniting, then imploding. */
		public static double open(double t) {
			return smooth((t - OPEN) / 30.0) * (1 - smooth((t - IMPLODE + 10) / 20.0));
		}

		/** Tick fireball {@code k} leaves the portal. */
		public static int fireAt(int k) {
			return FIRE0 + k * FIRE_GAP;
		}

		/** Where fireball {@code k} lands: in front of the gate, in local (across, 0, forward) terms. */
		public static Vec3d target(int k, int seed) {
			double a = (hash(seed + k, 31) - 0.5) * 2.6;
			double r = 18 + hash(seed + k, 32) * 46;
			return new Vec3d(Math.sin(a) * r, 0, Math.cos(a) * r);
		}

		/**
		 * The arm's hand at tick {@code t}, in local (across, up, forward) terms: reaching out of the portal, raising
		 * its fist, slamming it down, holding, then drawing back in.
		 */
		public static Vec3d hand(double t) {
			Vec3d in = new Vec3d(0, EYE_Y, -6), out = new Vec3d(-4, EYE_Y + 4, 16), up = new Vec3d(-2, EYE_Y + 22, 24), down = new Vec3d(0, 3, SLAM_AT);
			if (t < REACH) {
				return in.lerp(out, smooth((t - ARM) / (REACH - ARM)));
			}
			if (t < RAISE) {
				return out.lerp(up, smooth((t - REACH) / (RAISE - REACH)));
			}
			if (t < SLAM) {
				double u = (t - RAISE) / (SLAM - RAISE);
				return up.lerp(down, u * u);
			}
			if (t < WITHDRAW) {
				return down;
			}
			return down.lerp(in, smooth((t - WITHDRAW) / (ARM_END - WITHDRAW)));
		}

		/** How tightly the hand is clenched (0 open claws .. 1 fist). */
		public static double fist(double t) {
			return smooth((t - REACH) / 25.0) * (1 - smooth((t - WITHDRAW) / 20.0));
		}
	}
}
