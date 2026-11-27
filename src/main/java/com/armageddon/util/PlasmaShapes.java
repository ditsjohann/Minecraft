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
	 * Ball lightning, out of control. A core spits out eighteen big plasmoids that ricochet over the land in wild hops,
	 * burning through whatever stands in their way; each bursts and splits into three smaller ones that carry on; then
	 * the core fires a last volley in every direction and detonates.
	 */
	public static final class Plasmoid {
		public static final int GEN0 = 18, CHILDREN = 3, VOLLEY = 24, CHARGE = 20, FIRST = 24, GAP = 4, FINALE = 268, BURST = 318, END = 380;
		public static final double R = 40, CORE_Y = 5;

		private Plasmoid() {
		}

		/** One plasmoid: its id, generation (0 big, 1 split, 2 volley), when it starts and bursts, where it starts (heading frame, xz), size. */
		public record Ball(int id, int gen, int start, int end, double x, double z, double size, double a0) {
		}

		public static final java.util.List<Ball> BALLS = build();

		private static java.util.List<Ball> build() {
			java.util.List<Ball> out = new java.util.ArrayList<>();
			for (int k = 0; k < GEN0; k++) {
				int s = FIRST + k * GAP, e = s + 60 + (int) (hash(k, 3) * 60);
				Ball b = new Ball(k, 0, s, e, 0, 0, 1.3 + hash(k, 4) * 0.8, k * 2.39996);
				out.add(b);
				Vec3d p = pos(b, e);
				for (int j = 0; j < CHILDREN; j++) {
					int id = 100 + k * CHILDREN + j;
					out.add(new Ball(id, 1, e, e + 30 + (int) (hash(id, 3) * 50), p.x, p.z, 0.7 + hash(id, 4) * 0.4, hash(id, 7) * Math.PI * 2));
				}
			}
			for (int v = 0; v < VOLLEY; v++) {
				int id = 1000 + v;
				out.add(new Ball(id, 2, FINALE + v / 6, FINALE + 22 + (int) (hash(id, 3) * 24), 0, 0, 0.6 + hash(id, 4) * 0.3, v * Math.PI * 2 / VOLLEY));
			}
			return java.util.List.copyOf(out);
		}

		/**
		 * Where a plasmoid is at tick {@code t}: x and z in the heading frame, y over the ground under it. It moves in
		 * hops, each in a direction wrenched round from the last, bounding up and slamming down; it is turned back from
		 * the edge of the swarm's reach.
		 */
		public static Vec3d pos(Ball b, double t) {
			double x = b.x, z = b.z, a = b.a0;
			double time = b.start;
			double lenMin = b.gen == 0 ? 4 : b.gen == 1 ? 3 : 9, lenMax = b.gen == 0 ? 11 : b.gen == 1 ? 8 : 15;
			for (int i = 0; i < 40; i++) {
				double d = (b.gen == 2 ? 5 : 6) + hash(b.id * 41 + i, 1) * (b.gen == 2 ? 6 : 12);
				double len = lenMin + hash(b.id * 41 + i, 2) * (lenMax - lenMin);
				if (i > 0) {
					a += (hash(b.id * 41 + i, 3) - 0.5) * (b.gen == 2 ? 0.8 : 3.4);
				}
				double nx = x + Math.cos(a) * len, nz = z + Math.sin(a) * len;
				if (Math.hypot(nx, nz) > R * 0.95) {
					// Ricochet off the edge of its reach, back in toward the middle.
					a = Math.atan2(-z, -x) + (hash(b.id * 41 + i, 4) - 0.5) * 1.2;
					nx = x + Math.cos(a) * len;
					nz = z + Math.sin(a) * len;
				}
				double hgt = b.gen == 1 ? 0.5 + hash(b.id * 41 + i, 5) * 3.5 : 1 + hash(b.id * 41 + i, 5) * 7;
				if (t < time + d || i == 39) {
					double u = MathHelper.clamp((t - time) / d, 0.0, 1.0);
					double y = 1.2 * b.size + 4 * hgt * u * (1 - u);
					if (b.gen != 1 && i == 0) {
						// Out of the core, which hangs over the bomb.
						y = MathHelper.lerp(u, CORE_Y, y);
					}
					return new Vec3d(MathHelper.lerp(u, x, nx), y, MathHelper.lerp(u, z, nz));
				}
				x = nx;
				z = nz;
				time += d;
			}
			return new Vec3d(x, 1.2 * b.size, z);
		}

		/** True on the tick a plasmoid slams down at the end of a hop. */
		public static boolean lands(Ball b, int t) {
			double time = b.start;
			for (int i = 0; i < 40 && time <= t; i++) {
				double d = (b.gen == 2 ? 5 : 6) + hash(b.id * 41 + i, 1) * (b.gen == 2 ? 6 : 12);
				time += d;
				if ((int) time == t) {
					return true;
				}
			}
			return false;
		}
	}

	// ====================================================================== SOLAR ARCH

	/**
	 * Two footpoints ignite either side of the bomb and a twisted rope of plasma arches between them, climbing and
	 * winding tighter, until it snaps: the arch erupts forward as a coronal mass ejection, scorching a long wedge of
	 * land in front of it.
	 */
	public static final class Arch {
		public static final int FEET = 10, ARCH = 40, SNAP = 280, LASH = 12, BLAST_END = 380, END = 440;
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

		/** How far the eruption's front has run at tick {@code t} (it sets off once the rope has lashed down). */
		public static double front(double t) {
			double u = MathHelper.clamp((t - SNAP - LASH) / (double) (BLAST_END - SNAP - LASH), 0.0, 1.0);
			return REACH * (1 - (1 - u) * (1 - u));
		}

		/** How far the rope has whipped over toward the ground in front (0 standing, 1 flat on the ground). */
		public static double lash(double t) {
			return smooth((t - SNAP) / (double) LASH);
		}

		/** A point on the arch's centre line (theta 0..pi foot to foot), tipped over by the lash, heading frame. */
		public static Vec3d rope(double theta, double t) {
			double h = height(t), tilt = lash(t) * Math.PI / 2;
			double up = h * Math.sin(theta);
			return new Vec3d(up * Math.sin(tilt), up * Math.cos(tilt), -SPAN * Math.cos(theta));
		}
	}

	// ====================================================================== FUSION GEYSER

	/**
	 * The ground glows and cracks over a pocket of fusing plasma; a geyser of it erupts eighty blocks high and spreads
	 * into a mushroom, then rains back down in burning drops over everything around.
	 */
	public static final class Geyser {
		public static final int BULGE = 0, ERUPT = 60, RAIN = 120, FLING = 140, RAIN_END = 300, LAKE = 310, END = 380;
		public static final double R = 34, HEIGHT = 80, TORN = 20;

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
		public static final int FORM = 0, SQUEEZE = 50, BEADS = 170, COLLAPSE = 260, END = 380;
		public static final double HEIGHT = 56, R = 64, RADIUS = 6, WAVELENGTH = 9, DRAG = 32, CRATER = 16;
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
		public static final int FORM = 0, HOLD = 60, FAIL = 250, LASHED = 262, JETS = 266, RING = 276, RING_END = 336, NOVA = 352, END = 460;
		public static final double TOP = 22, LOW = 3.5, SUN = 4.5, CAGE = 11, R = 60, SHAFT = 40, PULL = 26;
		/** Flares before it fails: a single field line lashing out at the ground. */
		public static final int[] FLARES = {176, 204, 228};
		/** Field lines that lash out when it fails, and how far they reach. */
		public static final int LASHES = 12;
		public static final double LASH_REACH = 40, CRATER = 15;

		private Bottle() {
		}

		/** The sun's height over the ground at tick {@code t}. */
		public static double y(double t) {
			return MathHelper.lerp(smooth((t - HOLD) / (double) (FAIL - HOLD)), TOP, LOW);
		}

		/** Field-line lash {@code k} (0-11 when it fails, 12-14 the flares): heading-frame point {@code u} (0..1) out along it. */
		public static Vec3d lash(int k, double u) {
			double a = k < LASHES ? k * Math.PI * 2 / LASHES + 0.15 : hash(k, 71) * Math.PI * 2;
			double reach = k < LASHES ? LASH_REACH : 24;
			double r = 5 + (reach - 5) * u, ang = a + 0.45 * u;
			return new Vec3d(Math.cos(ang) * r, 0, Math.sin(ang) * r);
		}

		/** When lash {@code k} strikes. */
		public static int lashAt(int k) {
			return k < LASHES ? FAIL + 4 + (k % 4) : FLARES[k - LASHES] + 6;
		}

		/** How far the slicing ring has flown at tick {@code t}. */
		public static double ring(double t) {
			double u = MathHelper.clamp((t - RING) / (double) (RING_END - RING), 0.0, 1.0);
			return 3 + (R - 3) * (1 - (1 - u) * (1 - u));
		}
	}
}
