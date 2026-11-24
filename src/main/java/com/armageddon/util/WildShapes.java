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
		public static final double[][] ARMS = {{0.4, 13, 7.5, 11}, {2.75, 18, 6.5, 9}};
		public static final int TRUNK_SPIKES = 64, ARM_SPIKES = 18;
		public static final int SPIKES = TRUNK_SPIKES + ARM_SPIKES * 2;
		public static final double GRAVITY = 0.07;
		/** How far the red sandstorm turns the land to desert once it has stopped. */
		public static final double DESERT_R = 45;
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

		/**
		 * The turn to draw (and launch spikes) at, measured so that it comes to rest at zero: the spinning model stops
		 * exactly lined up with the blocks the server then sets in its place.
		 */
		public static double spin(double t) {
			return angle(t) - angle(STOP);
		}

		private static java.util.List<int[]> voxels;

		/**
		 * The cactus as blocks, relative to the block it grows from: {x, y, z, rib} with rib 1 for the darker ribs.
		 * The server places exactly these; the client spins exactly these, about the centre of the base block.
		 */
		public static synchronized java.util.List<int[]> voxels() {
			if (voxels == null) {
				java.util.LinkedHashMap<Long, int[]> m = new java.util.LinkedHashMap<>();
				column(m, 0, 0, TRUNK_R, 0, TRUNK_H);
				for (double[] arm : ARMS) {
					double ox = Math.cos(arm[0]), oz = Math.sin(arm[0]);
					for (double s = 0; s <= arm[2]; s += 0.5) {
						for (double dy = -ARM_R; dy <= ARM_R; dy += 1) {
							disc(m, ox * s, arm[1] + dy, oz * s, Math.sqrt(Math.max(0, ARM_R * ARM_R - dy * dy)));
						}
					}
					column(m, ox * arm[2], oz * arm[2], ARM_R, arm[1], arm[1] + arm[3]);
				}
				voxels = java.util.List.copyOf(m.values());
			}
			return voxels;
		}

		private static void column(java.util.Map<Long, int[]> m, double x, double z, double r, double y0, double y1) {
			for (double y = y0; y <= y1; y += 1) {
				// Rounded off at the top.
				double rr = y > y1 - r ? Math.sqrt(Math.max(0, r * r - (y - (y1 - r)) * (y - (y1 - r)))) : r;
				disc(m, x, y, z, rr);
			}
		}

		private static void disc(java.util.Map<Long, int[]> m, double x, double y, double z, double r) {
			int ri = MathHelper.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz > r * r + 0.5) {
						continue;
					}
					// Ribs: bands of darker green running up the stem, round its own axis.
					boolean rib = Math.floorMod((int) Math.floor(Math.atan2(dz, dx) / (Math.PI * 2) * 14), 2) == 0;
					boolean edge = dx * dx + dz * dz > (r - 1) * (r - 1);
					int bx = MathHelper.floor(x) + dx, by = MathHelper.floor(y), bz = MathHelper.floor(z) + dz;
					m.putIfAbsent(net.minecraft.util.math.BlockPos.asLong(bx, by, bz), new int[]{bx, by, bz, edge && rib ? 1 : 0});
				}
			}
		}

		/** Launch speed of spike {@code i} (blocks per tick, before the spin's own throw is added). */
		public static double speed(int i) {
			return 0.7 + 0.4 * SciShapes.hash(i, 11);
		}
	}

	// ====================================================================== BLACKFORGE

	/**
	 * The Blackforge: anvils start falling out of the sky, a few, then more and faster until it pours anvils. Then a
	 * hammer the size of a tower slams the ground: every anvil turns to iron, and the land round it breaks into
	 * tectonic plates that heave and sink.
	 */
	public static final class Forge {
		public static final int RAIN_END = 200, HAMMER_IN = 185, STRIKE = 250, LIFT = 380, END = 440;
		/** Passes of the plates shifting after the blow. */
		public static final int[] SHIFTS = {262, 292, 322, 352};
		/** How far out the anvils fall (it widens as the rain goes on), the iron, and the plates. */
		public static final double RAIN_R = 70, IRON_R = 80, PLATE_IN = 12, PLATE_R = 85;
		/** The plates: ring boundaries and how many plates each ring is split into. */
		public static final double[] RINGS = {12, 28, 48, 68, 85};
		public static final int SECTORS = 7;
		public static final double HAMMER_UP = 70;
		/** The hammer head's half-extents: width (x), height (y) and length (z), and the handle's length. */
		public static final double HEAD_W = 9, HEAD_H = 8, HEAD_L = 16, HANDLE_L = 90;

		private Forge() {
		}

		/** Anvils per tick at tick {@code t}: a trickle that becomes a downpour. */
		public static double rate(double t) {
			double u = MathHelper.clamp(t / RAIN_END, 0.0, 1.0);
			return t >= RAIN_END ? 0 : 0.12 + 5.5 * Math.pow(u, 2.2);
		}

		/** How far out the anvils are falling at tick {@code t}. */
		public static double rainRadius(double t) {
			return 12 + (RAIN_R - 12) * MathHelper.clamp(t / RAIN_END, 0.0, 1.0);
		}

		/** Gap between the hammer head and the ground at tick {@code t} (0 = struck; huge = out of sight). */
		public static double lift(double t) {
			if (t < HAMMER_IN) {
				return 400;
			}
			if (t < STRIKE - 14) {
				return HAMMER_UP + 330 * (1 - smooth((t - HAMMER_IN) / (STRIKE - 14 - HAMMER_IN)));
			}
			if (t < STRIKE) {
				double u = (t - (STRIKE - 14)) / 14.0;
				return HAMMER_UP * (1 - u * u);
			}
			if (t < LIFT) {
				return 0;
			}
			return 400 * smooth((t - LIFT) / 50.0);
		}

		/** Which plate a column lies on: ring index * SECTORS + sector, or -1 on a crack (or outside). */
		public static int plate(double dx, double dz) {
			double d = Math.sqrt(dx * dx + dz * dz);
			for (int r = 0; r < RINGS.length - 1; r++) {
				if (d >= RINGS[r] && d < RINGS[r + 1]) {
					if (d - RINGS[r] < 1.2) {
						return -1;
					}
					// Each ring's sectors are turned a little, so the cracks don't line up into spokes.
					double a = Math.atan2(dz, dx) + r * 0.7;
					double f = (a / (Math.PI * 2) + 1) % 1.0 * SECTORS;
					if (f - Math.floor(f) < 1.2 * SECTORS / (Math.PI * 2 * d)) {
						return -1;
					}
					return r * SECTORS + (int) f;
				}
			}
			return -1;
		}

		/**
		 * How far (blocks, up or down) plate {@code id} has moved at a column {@code d} out from the centre after
		 * {@code pass} passes: each plate rises or sinks, and tilts.
		 */
		public static int offset(int id, double d, int pass) {
			if (id < 0 || pass <= 0) {
				return 0;
			}
			int r = id / SECTORS;
			double mid = (RINGS[r] + RINGS[r + 1]) * 0.5, w = RINGS[r + 1] - RINGS[r];
			double lift = (SciShapes.hash(id, 41) < 0.5 ? -1 : 1) * (1.5 + 2.5 * SciShapes.hash(id, 42));
			double tilt = (SciShapes.hash(id, 43) - 0.5) * 6;
			double full = lift + tilt * (d - mid) / w;
			return (int) Math.round(full * Math.min(pass, SHIFTS.length) / SHIFTS.length);
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

	// ====================================================================== PLASMA KOI

	/**
	 * A pond of liquid plasma opens; koi made of light circle under its surface, then leap out one at a time on wider
	 * and wilder arcs, each landing in a scorching splash. At last they all leap together, braid into a dragon that
	 * climbs and circles over the pond, and dive back in: the pond erupts. Positions are relative to the centre of
	 * the pond's surface; the leaps end at surface height, and the client and server both add the landing's ground.
	 */
	public static final class Koi {
		public static final int KOI = 7, LEAPS = 16;
		public static final int OPEN = 30, LEAP0 = 120, GATHER = 420, RISE_END = 490, GEYSER = 525, RAIN_END = 610, END = 690;
		public static final double POND_R = 14, DEPTH = 3;
		/** Each koi's colour: cyan, magenta, gold, white, violet, mint, coral. */
		public static final int[] COLORS = {0x20C8FF, 0xFF30B0, 0xFFA010, 0x90C8FF, 0x7040FF, 0x30FF80, 0xFF5030};

		private Koi() {
		}

		public static double hash(int i, int k) {
			double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
			return v - Math.floor(v);
		}

		/** Where koi {@code k} swims at tick {@code t}: slow loops just under the surface. */
		public static Vec3d swim(int k, double t) {
			double dir = k % 2 == 0 ? 1 : -1;
			double a = hash(k, 1) * MathHelper.TAU + t * (0.035 + 0.02 * hash(k, 2)) * dir;
			double r = 4 + 7 * hash(k, 3) + 1.5 * Math.sin(t * 0.05 + k);
			return new Vec3d(Math.cos(a) * r, -0.9 + 0.25 * Math.sin(t * 0.13 + k), Math.sin(a) * r);
		}

		/** Tick leap {@code j} starts: the gaps shrink as they get wilder. */
		public static int leapStart(int j) {
			double t = LEAP0;
			for (int i = 0; i < j; i++) {
				t += Math.max(8, 26 - 1.4 * i);
			}
			return (int) t;
		}

		public static int flight(int j) {
			return 22 + j;
		}

		public static int koiOf(int j) {
			return j % KOI;
		}

		/** Where leap {@code j} comes down, relative to the pond's centre, at surface height. */
		public static Vec3d landing(int j) {
			Vec3d from = swim(koiOf(j), leapStart(j));
			double a = Math.atan2(from.z, from.x) + (hash(j, 4) - 0.5) * 0.8;
			double d = 22 + j * 2.4 + hash(j, 5) * 8;
			return new Vec3d(Math.cos(a) * d, 0, Math.sin(a) * d);
		}

		public static double apex(int j) {
			return 10 + j * 1.2 + hash(j, 6) * 6;
		}

		/** Leap {@code j} at tick {@code t}; {@code landDy} is the landing ground's height relative to the surface. */
		public static Vec3d leap(int j, double t, double landDy) {
			Vec3d from = swim(koiOf(j), leapStart(j));
			Vec3d to = landing(j);
			double u = MathHelper.clamp((t - leapStart(j)) / flight(j), 0.0, 1.0);
			double y = from.y + (landDy - from.y) * u + 4 * apex(j) * u * (1 - u);
			return new Vec3d(from.x + (to.x - from.x) * u, y, from.z + (to.z - from.z) * u);
		}

		/** The leap koi {@code k} is in at tick {@code t}, or -1. */
		public static int leapOf(int k, double t) {
			for (int j = k; j < LEAPS; j += KOI) {
				int s = leapStart(j);
				if (t >= s && t <= s + flight(j)) {
					return j;
				}
			}
			return -1;
		}

		/** True while koi {@code k} is gone from the pond after a landing, before it swims back in. */
		public static boolean resting(int k, double t) {
			for (int j = k; j < LEAPS; j += KOI) {
				double e = leapStart(j) + flight(j);
				if (t > e && t < e + 14) {
					return true;
				}
			}
			return false;
		}

		/** The dragon's spine: climbing round the pond, then circling, then diving straight back in. */
		public static Vec3d spine(double t) {
			double a = (t - GATHER) * 0.09;
			if (t < RISE_END) {
				double u = smooth((t - GATHER) / (RISE_END - GATHER));
				return new Vec3d(Math.cos(a) * 8, 3 + 50 * u, Math.sin(a) * 8);
			}
			double u = MathHelper.clamp((t - RISE_END) / (GEYSER - RISE_END), 0.0, 1.0);
			double r = 8 * (1 - u);
			return new Vec3d(Math.cos(a) * r, 53 - 55 * u * u, Math.sin(a) * r);
		}

		/** Koi {@code k} in the dragon: braided round the spine, blending in from where it was swimming. */
		public static Vec3d dragon(int k, double t) {
			double b = k * MathHelper.TAU / KOI + t * 0.5;
			Vec3d p = spine(t).add(Math.cos(b) * 3.0, Math.sin(b * 1.3) * 1.6, Math.sin(b) * 3.0);
			double blend = smooth((t - GATHER) / 15.0);
			return swim(k, GATHER).multiply(1 - blend).add(p.multiply(blend));
		}

		/** Where koi {@code k} is at tick {@code t} (null when it is not there to draw). */
		public static Vec3d at(int k, double t, double landDy) {
			if (t < OPEN || t >= GEYSER) {
				return null;
			}
			if (t >= GATHER) {
				return dragon(k, t);
			}
			int j = leapOf(k, t);
			if (j >= 0) {
				return leap(j, t, landDy);
			}
			return resting(k, t) ? null : swim(k, t);
		}
	}
}
