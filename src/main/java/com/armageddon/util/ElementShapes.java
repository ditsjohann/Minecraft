package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for three CITY DESTRUCTION bombs (the Wrecking Ball, the Flattener, Titan's Stride) and five
 * elemental CELESTIALS (the Inferno Vortex, the Maelstrom, the Basalt Uprising, the Downburst, the Thunderhead), shared
 * by the server events and the client effects. Points are in the bomb's heading frame (x forward, y up, z right; see
 * {@link FossilShapes#frame}) relative to its ground, unless said otherwise.
 */
public final class ElementShapes {
	private ElementShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		return FossilShapes.hash(i + 1301, k + 2203);
	}

	// ====================================================================== WRECKING BALL

	/**
	 * A ball of steel as big as a house on a chain hanging from orbit swings through the city in six arcs, each in a
	 * new direction, smashing a swath through everything; then the chain snaps and it drops into the middle.
	 */
	public static final class Wrecker {
		public static final int DESCEND = 0, FIRST = 80, SWING = 64, GAP = 16, SWINGS = 6, END = 760;
		/** The pivot high over the bomb, the chain's length, the ball's radius, how far it swings out each way. */
		public static final double PIVOT = 172, CHAIN = 160, BALL = 11, MAX = 1.05;
		public static final int SNAP = FIRST + SWINGS * (SWING + GAP), LAND = SNAP + 34;

		private Wrecker() {
		}

		/** Swing {@code k}'s plane: the horizontal direction it swings along (heading frame). */
		public static Vec3d plane(int k) {
			double a = k * 0.53 + 0.2;
			return new Vec3d(Math.cos(a), 0, Math.sin(a));
		}

		/** Which way swing {@code k} goes: from -1 to +1 along its plane or back. */
		static int sense(int k) {
			return k % 2 == 0 ? 1 : -1;
		}

		/** The chain's angle from straight down on swing {@code k} at {@code u} (0..1): released high, through the bottom, up the far side. */
		static double angle(int k, double u) {
			return -sense(k) * MAX * Math.cos(Math.PI * u);
		}

		/** The ball's centre at tick {@code t}, heading frame, relative to the bomb's ground. */
		public static Vec3d ball(double t) {
			Vec3d pivot = new Vec3d(0, PIVOT, 0);
			if (t < FIRST) {
				// Lowered from the sky to the top of the first swing.
				Vec3d start = at(0, -MAX * sense(0));
				double u = smooth(t / (double) FIRST);
				return start.add(0, (1 - u) * 260, 0);
			}
			for (int k = 0; k < SWINGS; k++) {
				double s0 = FIRST + k * (SWING + GAP);
				if (t < s0 + SWING) {
					return at(k, angle(k, Math.max(0, (t - s0) / SWING)));
				}
				if (t < s0 + SWING + GAP && k + 1 < SWINGS) {
					// Turning at the top of the swing onto the next one's plane.
					double u = smooth((t - s0 - SWING) / GAP);
					return at(k, angle(k, 1)).lerp(at(k + 1, angle(k + 1, 0)), u);
				}
			}
			Vec3d top = at(SWINGS - 1, angle(SWINGS - 1, 1));
			if (t < SNAP) {
				return top;
			}
			// The chain snaps: it falls from the top of its last swing onto the middle.
			double u = MathHelper.clamp((t - SNAP) / (double) (LAND - SNAP), 0.0, 1.0);
			Vec3d land = new Vec3d(0, BALL - 2, 0);
			return top.lerp(land, u * u).add(0, Math.sin(Math.PI * u) * 14, 0);
		}

		private static Vec3d at(int k, double ang) {
			Vec3d d = plane(k);
			return new Vec3d(0, PIVOT, 0).add(d.multiply(CHAIN * Math.sin(ang))).add(0, -CHAIN * Math.cos(ang), 0);
		}

		/** Swing {@code k}'s progress at {@code t} (0..1), or -1 if it is not swinging. */
		public static double swinging(int k, double t) {
			double s0 = FIRST + k * (SWING + GAP);
			return t < s0 || t > s0 + SWING ? -1 : (t - s0) / SWING;
		}

		/** For a point at distance {@code x} along swing {@code k}'s plane: when the ball passes it (0..1) and its height there. */
		public static double[] passes(int k, double x) {
			double s = MathHelper.clamp(x / CHAIN, -Math.sin(MAX), Math.sin(MAX));
			double ang = Math.asin(s);
			// angle = -sense*MAX*cos(pi u)  =>  u = acos(-ang/(sense*MAX))/pi
			double c = MathHelper.clamp(-ang / (sense(k) * MAX), -1, 1);
			double u = Math.acos(c) / Math.PI;
			double y = PIVOT - CHAIN * Math.cos(ang);
			return new double[]{u, y};
		}
	}

	// ====================================================================== THE FLATTENER

	/**
	 * A roller of steel as long as the city comes down out of the sky and rolls right across it, pressing everything
	 * it passes flat into a single layer of whatever stood there; then it lifts away.
	 */
	public static final class Roller {
		public static final int DESCEND = 0, LANDED = 70, ROLL_END = 470, LIFT = 500, END = 600;
		/** The roller: radius, half its length; where it starts and stops rolling along the heading. */
		public static final double RADIUS = 18, HALF = 75, FROM = -165, TO = 165;

		private Roller() {
		}

		/** How far along the heading its axle is at tick {@code t}. */
		public static double x(double t) {
			return MathHelper.lerp(MathHelper.clamp((t - LANDED) / (double) (ROLL_END - LANDED), 0.0, 1.0), FROM, TO);
		}

		/** Its axle's height over the ground at tick {@code t}: dropped from the sky, rolling, lifted away. */
		public static double y(double t) {
			if (t < LANDED) {
				double u = smooth(t / (double) LANDED);
				return RADIUS + (1 - u) * 240;
			}
			if (t > LIFT) {
				double u = (t - LIFT) / 80.0;
				return RADIUS + u * u * 300;
			}
			return RADIUS;
		}

		/** How far it has turned (radians) at tick {@code t}. */
		public static double spin(double t) {
			return (x(t) - FROM) / RADIUS;
		}
	}

	// ====================================================================== TITAN'S STRIDE

	/**
	 * An unseen giant walks across the city. Its shadow darkens the ground, then its foot comes down: a footprint ten
	 * blocks deep, buildings and all. It ends with a jump, both feet together.
	 */
	public static final class Stride {
		public static final int FIRST = 50, STEP = 34, STEPS = 9, SHADOW = 22;
		/** The footprint: length, width, depth; how far each step goes, how far apart the feet are. */
		public static final double FOOT_L = 34, FOOT_W = 14, DEPTH = 10, PACE = 38, GAIT = 12;
		public static final int JUMP = FIRST + STEPS * STEP + 30, END = JUMP + 160;

		private Stride() {
		}

		/** When step {@code k} lands (the last two, together, are the jump). */
		public static int land(int k) {
			return k < STEPS ? FIRST + k * STEP + SHADOW : JUMP + SHADOW + 10;
		}

		/** Where step {@code k}'s heel-to-toe centre is (heading frame), and which foot (-1 left, 1 right). */
		public static Vec3d foot(int k) {
			if (k < STEPS) {
				double x = -150 + k * PACE;
				return new Vec3d(x, 0, (k % 2 == 0 ? -1 : 1) * GAIT);
			}
			// The jump: both feet together, past the last step.
			double x = -150 + STEPS * PACE + 30;
			return new Vec3d(x, 0, (k == STEPS ? -1 : 1) * GAIT * 1.3);
		}

		/** True if a point (heading frame, relative to the foot's centre) is inside a footprint: a heel, a sole, five toes. */
		public static boolean inside(double x, double z, double scale) {
			double l = FOOT_L * scale, w = FOOT_W * scale;
			// The sole: an ellipse, a little narrower at the heel.
			double ex = x / (l * 0.42), ez = z / (w * (x < 0 ? 0.42 : 0.5));
			if (ex * ex + ez * ez <= 1) {
				return true;
			}
			// Five toes in an arc at the front.
			for (int i = 0; i < 5; i++) {
				double tz = (i - 2) * w * 0.2, tx = l * 0.46 - Math.abs(i - 1.6) * l * 0.025;
				double tr = w * (i == 0 ? 0.13 : 0.1);
				double dx = x - tx, dz = z - tz;
				if (dx * dx + dz * dz <= tr * tr) {
					return true;
				}
			}
			return false;
		}
	}

	// ====================================================================== INFERNO VORTEX

	/**
	 * Six whirls of fire rise round the bomb and spiral in, burning spiral scars into the land, until they merge into
	 * one colossal pillar of fire that collapses into a sea of flame.
	 */
	public static final class Inferno {
		public static final int RISE = 20, MERGE = 320, COLLAPSE = 400, END = 520;
		public static final int WHIRLS = 6;
		public static final double START_R = 92, R = 140;

		private Inferno() {
		}

		/** Whirl {@code k} at tick {@code t} (heading frame, on the ground): spiralling in, faster as it closes. */
		public static Vec3d whirl(int k, double t) {
			double u = MathHelper.clamp((t - RISE) / (double) (MERGE - RISE), 0.0, 1.0);
			double r = START_R * Math.pow(1 - u, 1.15);
			double a = k * Math.PI * 2 / WHIRLS + 2.6 * u + 1.4 * u * u;
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		/** How far the sea of flame has spread at tick {@code t}. */
		public static double sea(double t) {
			double u = MathHelper.clamp((t - COLLAPSE) / 60.0, 0.0, 1.0);
			return R * (1 - (1 - u) * (1 - u));
		}
	}

	// ====================================================================== MAELSTROM

	/**
	 * The ground falls away into a vast whirlpool, everything round it dragged spiralling in and under; then the
	 * vortex erupts as a waterspout a hundred blocks high, and leaves a deep round lake.
	 */
	public static final class Maelstrom {
		public static final int OPEN = 30, OPENED = 170, ERUPT = 330, RAIN = 345, END = 520;
		public static final double R = 72, DEEP = 30, LEVEL = 3;

		private Maelstrom() {
		}

		/** How deep the bowl is at distance {@code r} from the middle. */
		public static double depth(double r) {
			double k = MathHelper.clamp(1 - (r / R) * (r / R), 0.0, 1.0);
			return 2 + (DEEP - 2) * Math.pow(k, 0.8);
		}

		/** How far the bowl has opened (from the middle out) at tick {@code t}. */
		public static double opened(double t) {
			return R * smooth((t - OPEN) / (double) (OPENED - OPEN));
		}

		/** How fast it spins (radians a tick at the rim), gathering then dying away after it erupts. */
		public static double spin(double t) {
			return 0.04 * smooth((t - OPEN) / 120.0) * (1 - smooth((t - ERUPT) / 60.0));
		}
	}

	// ====================================================================== BASALT UPRISING

	/**
	 * The land cracks into hexagonal columns of basalt and they thrust up out of the ground in a wave rolling out from
	 * the bomb, launching whatever stands on them; then the middle caves in.
	 */
	public static final class Basalt {
		public static final int CRACK = 20, RISE = 60, RISEN = 260, CAVE = 300, END = 420;
		public static final double R = 100, CELL = 6.5, PIT = 22, PIT_DEPTH = 26;

		private Basalt() {
		}

		/** How far the wave of columns has rolled out at tick {@code t}. */
		public static double front(double t) {
			return R * smooth((t - RISE) / (double) (RISEN - RISE));
		}

		/**
		 * The hexagonal cell a point (heading frame) falls in: its centre (x, z) and how far the point is from that
		 * centre as a fraction of the cell's radius (1 at the edge).
		 */
		public static double[] cell(double x, double z) {
			double s = CELL;
			double q = (2.0 / 3 * x) / s, r = (-1.0 / 3 * x + Math.sqrt(3) / 3 * z) / s;
			double cx = q, cz = -q - r, cy = r;
			double rx = Math.round(cx), ry = Math.round(cy), rz = Math.round(cz);
			double dx = Math.abs(rx - cx), dy = Math.abs(ry - cy), dz = Math.abs(rz - cz);
			if (dx > dy && dx > dz) {
				rx = -ry - rz;
			} else if (dy > dz) {
				ry = -rx - rz;
			}
			double hx = s * 1.5 * rx, hz = s * Math.sqrt(3) * (ry + rx / 2.0);
			double d = Math.hypot(x - hx, z - hz) / s;
			return new double[]{hx, hz, d, rx, ry};
		}

		/** How high the column of a cell rises: taller toward the middle, every column its own height. */
		public static int height(double hx, double hz, int qx, int qy) {
			double r = Math.hypot(hx, hz);
			double k = 1 - MathHelper.clamp(r / R, 0, 1);
			return (int) Math.round(3 + k * 16 + hash(qx * 31 + 7, qy * 17 + 3) * (6 + k * 12));
		}
	}

	// ====================================================================== DOWNBURST

	/**
	 * The air is sucked up out of the land: a vortex opens in the sky and the ground and everything on it lifts and
	 * floats, turning; then the air comes crashing back down and slams it all flat.
	 */
	public static final class Downburst {
		public static final int DRAW = 20, LIFT = 40, HOVER = 260, SLAM = 280, END = 440;
		public static final double R = 96, SKY = 90, CEILING = 62, SLAM_R = 130;

		private Downburst() {
		}

		/** How far the downburst's flattening has rolled out at tick {@code t}. */
		public static double front(double t) {
			double u = MathHelper.clamp((t - SLAM) / 40.0, 0.0, 1.0);
			return SLAM_R * (1 - (1 - u) * (1 - u));
		}
	}

	// ====================================================================== THUNDERHEAD

	/**
	 * A supercell builds over the land and lightning hammers it, every strike burning a branching scar of glass and
	 * scorch into the ground; then one last bolt, a superbolt, strikes the middle and burns a giant one.
	 */
	public static final class Thunder {
		public static final int BUILD = 0, STORM = 80, STORM_END = 330, SUPER = 360, END = 500;
		public static final int STRIKES = 56;
		public static final double R = 120, CLOUD = 95, SUPER_R = 72;

		private Thunder() {
		}

		/** Strike {@code i}: where (heading frame) and when. */
		public static double[] strike(int i) {
			double a = hash(i, 51) * Math.PI * 2, r = 12 + Math.sqrt(hash(i, 52)) * (R - 12);
			double t = STORM + (STORM_END - STORM) * Math.pow(i / (double) STRIKES, 0.8) + hash(i, 53) * 4;
			return new double[]{Math.cos(a) * r, Math.sin(a) * r, t};
		}
	}
}
