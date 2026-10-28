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

	// ====================================================================== RESONANCE CASCADE

	/** A tuning-fork emitter hums; the resonance spreads; every structure behind its front shakes apart and falls. */
	public static final class Resonance {
		public static final int EMIT = 20, HUM = 40, CASCADE = 160, CASCADE_END = 400, SHATTER = 410, END = 480;
		public static final double R = 95, FORK_H = 24;

		private Resonance() {
		}

		/** Radius the collapse has reached at tick {@code t}. */
		public static double front(double t) {
			return MathHelper.clamp((t - CASCADE) / (CASCADE_END - CASCADE) * R, 0, R);
		}

		/** How hard everything is shaking (0..1). */
		public static double hum(double t) {
			return MathHelper.clamp((t - HUM) / (CASCADE - HUM), 0.0, 1.0);
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

	// ====================================================================== DYSON LANCE

	/** A swarm of orbital mirrors focuses the sun into one beam that cuts three canyons across the map, then drills. */
	public static final class Dyson {
		public static final int SWARM = 20, FOCUS = 170, CUT = 200, CUT_LEN = 130, CUT_GAP = 20, DRILL = 200 + 3 * 150, END = DRILL + 140, CINE = DRILL + 100;
		public static final double L = 300, HALF_W = 6, DEPTH = 34, MIRROR_ALT = 320;

		private Dyson() {
		}

		/** Start tick of cut {@code k} (0..2). */
		public static int cutStart(int k) {
			return CUT + k * (CUT_LEN + CUT_GAP);
		}

		/** Direction of cut {@code k}: three lines through the centre, sixty degrees apart. */
		public static Vec3d axis(int k, double yaw) {
			double a = yaw + k * Math.PI / 3;
			return new Vec3d(Math.cos(a), 0, Math.sin(a));
		}

		/** Where along cut {@code k} the beam is at tick {@code t} (-L..L), or NaN when it is not cutting. */
		public static double along(int k, double t) {
			double u = (t - cutStart(k)) / CUT_LEN;
			return u < 0 || u > 1 ? Double.NaN : -L + 2 * L * u;
		}
	}

	// ====================================================================== GRAVITY KILLSWITCH

	/** Gravity switches off: the town floats up and tumbles; then it comes back many times over and slams it all down. */
	public static final class Gravity {
		public static final int HUM = 20, OFF = 90, RISE_END = 330, SLAM = 340, END = 460, CINE = 420;
		public static final double R = 130, CEIL = 70;

		private Gravity() {
		}

		/** How far things have floated up (0..1) at tick {@code t}. */
		public static double rise(double t) {
			double u = MathHelper.clamp((t - OFF) / (RISE_END - OFF), 0.0, 1.0);
			return u * (2 - u);
		}

		/** The fall after the slam (0..1): fast, accelerating. */
		public static double fall(double t) {
			double u = MathHelper.clamp((t - SLAM) / 14.0, 0.0, 1.0);
			return u * u;
		}
	}

	// ====================================================================== SEISMIC TSUNAMI

	/** A piston hammer drives a wave of rock out through the land, scraping it down to bare stone. */
	public static final class Wave {
		public static final int HAMMER = 20, SLAMS = 4, WAVE = 140, END = 560, CINE = 520;
		public static final double R = 320, CREST = 22, SCRAPE = 6;

		private Wave() {
		}

		/** Tick of hammer blow {@code i}. */
		public static int slam(int i) {
			return HAMMER + 20 + i * 26;
		}

		/** Radius of the crest at tick {@code t}. */
		public static double front(double t) {
			return MathHelper.clamp((t - WAVE) * 0.85 + 8, 0, R);
		}

		/** Height of the crest at radius {@code r}: it builds up, then dies away near the end of its run. */
		public static double height(double r) {
			return CREST * MathHelper.clamp(r / 40.0, 0.2, 1.0) * MathHelper.clamp((R - r) / 50.0, 0.0, 1.0);
		}
	}
}
