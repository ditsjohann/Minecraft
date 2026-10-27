package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the plasma / sci-fi bombs, shared by the server events (which edit the world) and the
 * client effects (which draw the machines and the light) so the two always line up.
 */
public final class SciShapes {
	private SciShapes() {
	}

	public static double hash(int a, int b) {
		long h = a * 734287L ^ b * 912931L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	// ====================================================================== ION SCYTHE

	/** Orbital ion cannon: the beam drops, carves a spiral trench, the trench charges, then ignites along its length. */
	public static final class Ion {
		public static final int DROP = 40, CARVE = 60, CARVE_END = 330, CHARGE_END = 390, IGNITE = 390, END = 470;
		/** Spiral arms gap (blocks per turn) and outer radius. */
		public static final double GAP = 13, R = 64;

		private Ion() {
		}

		/** Spiral angle reached at tick {@code t}: constant ground speed along an Archimedean spiral. */
		public static double theta(double t) {
			double u = MathHelper.clamp((t - CARVE) / (CARVE_END - CARVE), 0.0, 1.0);
			double thMax = R / (GAP / MathHelper.TAU);
			// Arc length of r = a*theta grows ~ theta^2, so theta ~ sqrt(u) keeps the cutting speed even.
			return thMax * Math.sqrt(u);
		}

		/** Offset of the spiral at angle {@code th} from the centre (y = 0), turned by {@code phase}. */
		public static Vec3d at(double th, double phase) {
			double r = GAP / MathHelper.TAU * th;
			return new Vec3d(Math.cos(th + phase) * r, 0, Math.sin(th + phase) * r);
		}
	}

	// ====================================================================== PHASE SCRUBBER

	/** A hex force-field dome drops; a scan plane descends through it erasing everything; the floor is printed. */
	public static final class Phase {
		public static final int DOME = 30, DOME_UP = 90, SCAN = 110, SCAN_END = 400, PRINT = 410, SHATTER = 440, END = 540;
		public static final double R = 72, H = 74, BELOW = 1;

		private Phase() {
		}

		/** Height of the scan plane above the anchor at tick {@code t}. */
		public static double plane(double t) {
			double u = MathHelper.clamp((t - SCAN) / (SCAN_END - SCAN), 0.0, 1.0);
			return MathHelper.lerp(u * u * (3 - 2 * u) * 0.35 + u * 0.65, H, -BELOW);
		}

		/** Radius of the dome's footprint at height {@code y} above the anchor. */
		public static double radiusAt(double y) {
			if (y <= 0) {
				return R;
			}
			double k = Math.min(1.0, y / H);
			return R * Math.sqrt(Math.max(0.0, 1 - k * k));
		}
	}

	// ====================================================================== TESLA DOMINION

	/** The tower rises, arcs storm the city, a ball of lightning grows on the crown and discharges everywhere. */
	public static final class Tesla {
		public static final int RISE = 20, RISEN = 120, STORM = 130, ORB = 430, DISCHARGE = 500, FALL = 540, END = 640;
		public static final double TOWER_H = 70, REACH = 115;

		private Tesla() {
		}

		/** How far up the tower has risen (0..1). */
		public static double risen(double t) {
			double u = MathHelper.clamp((t - RISE) / (RISEN - RISE), 0.0, 1.0);
			return u * u * (3 - 2 * u);
		}

		public static Vec3d crown(Vec3d base, double t) {
			return base.add(0, TOWER_H * risen(t) + 6, 0);
		}
	}

	// ====================================================================== EVENT HORIZON ENGINE

	/** The ring machine assembles, the singularity opens, the land is torn up into its disk, then it all goes in. */
	public static final class Horizon {
		public static final int ASSEMBLE = 30, OPEN = 150, FEED = 170, FEED_END = 520, COLLAPSE = 520, GONE = 580, END = 700, CINE = 640;
		public static final double ALT = 70, RING_R = 34;

		private Horizon() {
		}

		/** Radius the disk is being fed from at tick {@code t}. */
		public static double feedRadius(double t) {
			return MathHelper.clamp(16 + (t - FEED) * 0.32, 16, 130);
		}

		/** The engine's spin: rings and disk speed up toward the collapse. */
		public static double spin(double t) {
			double u = MathHelper.clamp((t - OPEN) / (COLLAPSE - OPEN), 0.0, 1.0);
			return 0.02 + 0.16 * u * u;
		}
	}

	// ====================================================================== NANITE BLOOM

	/** Grey goo: fingers of nanite matter reach out, fill in, and the whole region resolves into a silver hex plain. */
	public static final class Nanite {
		public static final int RELEASE = 30, GROW = 40, GROW_END = 620, FILL = 360, PLAIN = 640, PULSE = 700, END = 820, CINE = 780;
		public static final double R = 190;

		private Nanite() {
		}

		/** The fill front (behind the fingers) at tick {@code t}. */
		public static double fill(double t) {
			return MathHelper.clamp((t - FILL) * 0.75, 0, R);
		}
	}

	// ====================================================================== INVERSION ENGINE

	/** A sphere of the world is flipped upside down into the sky, hangs there, and shatters back onto the land. */
	public static final class Inversion {
		public static final int CHARGE = 20, LIFT = 90, LIFT_END = 260, HANG_END = 470, SHATTER = 470, END = 620, CINE = 560;
		public static final double R = 34, HEIGHT = 100;

		private Inversion() {
		}

		/** How much of the sphere has gone up (0..1). */
		public static double lifted(double t) {
			return MathHelper.clamp((t - LIFT) / (double) (LIFT_END - LIFT), 0.0, 1.0);
		}

		/** Where a block at height {@code y} (relative to the anchor) ends up: mirrored about the half-way plane. */
		public static int mirror(int y) {
			return (int) HEIGHT - y;
		}
	}
}
