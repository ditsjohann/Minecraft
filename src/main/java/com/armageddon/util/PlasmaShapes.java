package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the plasma bombs (the Plasmoid Swarm, the Solar Arch, the Fusion Geyser, the Z-Pinch and
 * the Magnetic Bottle), shared by the server events and the client effects. Points are relative to the bomb's ground,
 * in the bomb's heading frame where it matters (x forward, z right; see {@link FossilShapes#frame}).
 */
public final class PlasmaShapes {
	private PlasmaShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		return FossilShapes.hash(i + 101, k + 211);
	}

	// ====================================================================== PLASMOID SWARM

	/**
	 * A crackling core spits out ball lightning, one after another: each plasmoid hops and wanders out over the land on
	 * its own path and bursts where it ends up. Then the core goes too.
	 */
	public static final class Plasmoid {
		public static final int N = 16, CHARGE = 20, FIRST = 40, GAP = 6, BURST = 330, END = 380;
		public static final double R = 24, CORE_Y = 4;

		private Plasmoid() {
		}

		public static int launch(int k) {
			return FIRST + k * GAP;
		}

		/** When plasmoid {@code k} bursts. */
		public static int pop(int k) {
			return launch(k) + 90 + (int) (hash(k, 3) * 120);
		}

		/** How big it is (radius, blocks). */
		public static double size(int k) {
			return 1.0 + hash(k, 4) * 0.9;
		}

		/** Where plasmoid {@code k} is at tick {@code t} (null before it is launched or after it bursts). */
		public static Vec3d pos(int k, double t) {
			int l = launch(k), p = pop(k);
			if (t < l || t > p) {
				return null;
			}
			double u = (t - l) / (double) (p - l);
			double reach = (8 + (R - 8) * hash(k, 5)) * Math.pow(u, 0.75);
			double a = k * 2.39996 + 0.7 * Math.sin(t * 0.045 + k * 1.3) * u;
			double hop = Math.abs(Math.sin(t * 0.21 + k * 2.1)) * (2.5 + 1.5 * hash(k, 6)) * (1 - 0.6 * u);
			double y = MathHelper.lerp(smooth(u * 6), CORE_Y, 1.4 + hop);
			return new Vec3d(Math.cos(a) * reach, y, Math.sin(a) * reach);
		}
	}

	// ====================================================================== SOLAR ARCH

	/**
	 * Two footpoints ignite either side of the bomb and a twisted rope of plasma arches between them, climbing and
	 * winding tighter, until it snaps: the arch erupts forward as a coronal mass ejection, scorching a long wedge of
	 * land in front of it.
	 */
	public static final class Arch {
		public static final int FEET = 10, ARCH = 40, SNAP = 280, BLAST_END = 360, END = 420;
		/** Footpoints this far either side of the bomb (along the right axis); the arch rises to HEIGHT. */
		public static final double SPAN = 20, HEIGHT = 10, RISE = 0.07;
		/** The eruption's wedge: how far it reaches in front and its half-angle. */
		public static final double REACH = 72, WEDGE = 0.5;

		private Arch() {
		}

		/** The arch's height at tick {@code t}. */
		public static double height(double t) {
			return HEIGHT + RISE * MathHelper.clamp(t - ARCH, 0, SNAP - ARCH);
		}

		/** How far the eruption's front has run at tick {@code t}. */
		public static double front(double t) {
			double u = MathHelper.clamp((t - SNAP) / (double) (BLAST_END - SNAP), 0.0, 1.0);
			return REACH * (1 - (1 - u) * (1 - u));
		}
	}

	// ====================================================================== FUSION GEYSER

	/**
	 * The ground glows and cracks over a pocket of fusing plasma; a geyser of it erupts eighty blocks high and spreads
	 * into a mushroom, then rains back down in burning drops over everything around.
	 */
	public static final class Geyser {
		public static final int BULGE = 0, ERUPT = 60, RAIN = 120, RAIN_END = 300, END = 360;
		public static final double R = 34, HEIGHT = 80;

		private Geyser() {
		}

		/** Where drop {@code i} of the rain lands, relative to the bomb. */
		public static Vec3d drop(int i) {
			double a = hash(i, 11) * Math.PI * 2, r = 4 + Math.sqrt(hash(i, 12)) * (R - 4);
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}
	}

	// ====================================================================== Z-PINCH

	/**
	 * A column of plasma stands up out of the bomb and magnetic rings crush it; it narrows and breaks into beads, and the
	 * beads are squeezed out sideways and fly off to burst on the land. Then the column collapses into a fusion flash
	 * and a shockwave strips the land flat.
	 */
	public static final class Pinch {
		public static final int FORM = 0, SQUEEZE = 50, BEADS = 170, COLLAPSE = 260, END = 360;
		public static final double HEIGHT = 56, R = 58, RADIUS = 6, WAVELENGTH = 9;
		public static final int N_BEADS = 8, BEAD_FLIGHT = 26;

		private Pinch() {
		}

		/** When bead {@code k} is squeezed out. */
		public static int bead(int k) {
			return BEADS + k * 10;
		}

		/** Where bead {@code k} leaves the column (relative to the bomb). */
		public static Vec3d beadFrom(int k) {
			double y = 8 + (k * 0.618 % 1) * (HEIGHT - 16);
			y = Math.round(y / WAVELENGTH) * WAVELENGTH + WAVELENGTH * 0.25;
			return new Vec3d(0, Math.min(HEIGHT - 4, Math.max(5, y)), 0);
		}

		/** Which way bead {@code k} flies (unit, horizontal). */
		public static Vec3d beadDir(int k) {
			double a = k * 2.39996 + 0.3;
			return new Vec3d(Math.cos(a), 0, Math.sin(a));
		}

		/** Where bead {@code k} comes down. */
		public static Vec3d beadTo(int k) {
			return beadDir(k).multiply(30 + 18 * hash(k, 21));
		}

		/** How far the collapse's shockwave has run. */
		public static double front(double t) {
			double u = MathHelper.clamp((t - COLLAPSE) / 40.0, 0.0, 1.0);
			return R * (1 - (1 - u) * (1 - u));
		}
	}

	// ====================================================================== MAGNETIC BOTTLE

	/**
	 * A small sun of plasma held in a cage of magnetic field lines sinks slowly toward the ground, spinning faster and
	 * brighter. The bottle fails: the lines snap, jets burst from its poles (one drilling a shaft into the earth), and
	 * a ring of plasma flies out of its equator, slicing through everything standing at waist height.
	 */
	public static final class Bottle {
		public static final int FORM = 0, HOLD = 60, FAIL = 250, JETS = 262, RING = 270, RING_END = 330, END = 420;
		public static final double TOP = 22, LOW = 3.5, SUN = 4.5, CAGE = 11, R = 58, SHAFT = 30;

		private Bottle() {
		}

		/** The sun's height over the ground at tick {@code t}. */
		public static double y(double t) {
			return MathHelper.lerp(smooth((t - HOLD) / (double) (FAIL - HOLD)), TOP, LOW);
		}

		/** How far the slicing ring has flown at tick {@code t}. */
		public static double ring(double t) {
			double u = MathHelper.clamp((t - RING) / (double) (RING_END - RING), 0.0, 1.0);
			return 3 + (R - 3) * (1 - (1 - u) * (1 - u));
		}
	}
}
