package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Shared shapes for the aftermath of the three original WORLD DESTRUCTION bombs, so the server's
 * real blocks and the client's glow effects agree. Everything is deterministic from the anchor.
 *
 * <ul>
 * <li>Galactic Smash: a fist imprint (four knuckle bowls, finger plateau with ridges, a thumb groove)
 * in a local frame where {@code f} runs along the punch heading and {@code r} across it.</li>
 * <li>Booming Tick: twelve concentric terraces, one per boom, stepping down towards the centre.</li>
 * <li>Alien Death Ray: crystal spires scattered over the terraformed land.</li>
 * </ul>
 */
public final class WorldShapes {
	private WorldShapes() {
	}

	public static long seed(Vec3d anchor) {
		return MathHelper.floor(anchor.x) * 341873128712L ^ MathHelper.floor(anchor.z) * 132897987541L;
	}

	static double hash(long seed, int i, int salt) {
		long h = seed + i * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
		h ^= h >>> 32;
		return (h & 0xFFFFFFL) / (double) 0xFFFFFF;
	}

	// ------------------------------------------------------------------ Galactic Smash

	/** Knuckle bowl centres in (f, r) units of R. */
	public static final double[][] KNUCKLES = {{0.02, -0.33}, {0.06, -0.11}, {0.06, 0.11}, {0.02, 0.33}};
	public static final double KNUCKLE_R = 0.125, KNUCKLE_DEPTH = 0.34;

	private static double bowl(double f, double r, double cf, double cr, double radF, double radR, double depth) {
		double q = ((f - cf) / radF) * ((f - cf) / radF) + ((r - cr) / radR) * ((r - cr) / radR);
		return q >= 1 ? 0 : depth * Math.sqrt(1 - q);
	}

	/**
	 * Depth of the fist imprint below the original ground at local (f, r) (both in blocks), for a fist of
	 * size {@code R}. 0 outside. The knuckles hit hardest; the folded fingers leave a ridged plateau behind
	 * them; the thumb cuts a groove along one side.
	 */
	public static double smashDepth(double f, double r, double R) {
		double uf = f / R, ur = r / R;
		double d = 0;
		for (double[] k : KNUCKLES) {
			d = Math.max(d, bowl(uf, ur, k[0], k[1], KNUCKLE_R * 1.15, KNUCKLE_R, KNUCKLE_DEPTH));
		}
		// Folded fingers: a plateau behind the knuckles with ridges between the fingers.
		if (uf < 0.02 && uf > -0.40 && Math.abs(ur) < 0.45) {
			double edge = Math.min(Math.min(0.02 - uf, uf + 0.40), 0.45 - Math.abs(ur));
			double plateau = 0.19 * Math.min(1, edge / 0.05);
			double ridge = 0;
			for (double gap : new double[] {-0.22, 0, 0.22}) {
				ridge = Math.max(ridge, Math.max(0, 1 - Math.abs(ur - gap) / 0.035));
			}
			d = Math.max(d, plateau - 0.07 * ridge);
		}
		// Thumb groove along the left side, curling in towards the knuckles.
		double tr = -0.50 + 0.12 * MathHelper.clamp((uf + 0.05) / 0.3, 0, 1);
		if (uf > -0.38 && uf < 0.0) {
			d = Math.max(d, bowl(0, ur, 0, tr, 1, 0.085, 0.24) * Math.min(1, (0.0 - uf) / 0.06) * Math.min(1, (uf + 0.38) / 0.06));
		}
		return d * R;
	}

	/** Height of the displaced-earth lip just outside the imprint (0 elsewhere). */
	public static double smashRim(double f, double r, double R, double depth) {
		if (depth > 0) {
			return 0;
		}
		// Distance outside the imprint, measured roughly by sampling the field around the point.
		// Cheap reject: the whole fist sits within ~0.6R of the centre.
		if (Math.hypot(f, r) > R * 0.72) {
			return 0;
		}
		double best = Double.MAX_VALUE;
		for (double s = 0.03; s <= 0.12 && best == Double.MAX_VALUE; s += 0.045) {
			for (int k = 0; k < 8; k++) {
				double a = k * Math.PI / 4;
				if (smashDepth(f + Math.cos(a) * s * R, r + Math.sin(a) * s * R, R) > 0) {
					best = s;
					break;
				}
			}
		}
		return best == Double.MAX_VALUE ? 0 : R * 0.05 * (1 - best / 0.13);
	}

	/** Radial cracks of cosmic glass running out from the knuckles: polylines in (f, r) blocks. */
	public static double[][][] smashCracks(long seed, double R) {
		int n = 11;
		double[][][] out = new double[n][][];
		for (int i = 0; i < n; i++) {
			double ang = i * Math.PI * 2 / n + (hash(seed, i, 1) - 0.5) * 0.35;
			int segs = 14;
			double[][] pts = new double[segs + 1][];
			double len = R * (1.15 + 0.6 * hash(seed, i, 2));
			double a = ang, f = Math.cos(ang) * R * 0.22, r = Math.sin(ang) * R * 0.22;
			for (int s = 0; s <= segs; s++) {
				pts[s] = new double[] {f, r};
				a += (hash(seed, i * 31 + s, 3) - 0.5) * 0.5;
				f += Math.cos(a) * len / segs;
				r += Math.sin(a) * len / segs;
			}
			out[i] = pts;
		}
		return out;
	}

	/** Bell ripples outside the imprint: terrain offset (blocks) at distance {@code d}, already decayed. */
	public static double ripple(double d, double R) {
		double start = R * 1.05;
		if (d < start) {
			return 0;
		}
		double x = d - start;
		double wave = Math.sin(x / (R * 0.17) * Math.PI * 2);
		return R * 0.065 * wave * Math.exp(-x / (R * 0.55));
	}

	public static double rippleReach(double R) {
		return R * 1.9;
	}

	// ------------------------------------------------------------------ Booming Tick

	public static final int BANDS = 12;

	/** Outer radius of terrace {@code i} (1..12) for a final reach of {@code R}; terrace 0 is the hub. */
	public static double bandRadius(int i, double R) {
		return R * 1.5 * Math.pow(1.45, i) / (1.5 * Math.pow(1.45, BANDS));
	}

	/** How far terrace {@code i} sits below the original ground. The centre is the deepest step. */
	public static int bandDepth(int i) {
		return 1 + Math.round((BANDS - i) * 1.5F);
	}

	/** The terrace a distance falls in (0 = hub). */
	public static int bandOf(double d, double R) {
		for (int i = 0; i <= BANDS; i++) {
			if (d < bandRadius(i, R)) {
				return i;
			}
		}
		return BANDS + 1;
	}

	/**
	 * Terrace palette index per band (0 hub .. 12 rim): alternating dark and light so it reads as a target,
	 * warming from blood red at the centre to pale at the rim. 0 redstone, 1 red, 2 terracotta, 3 orange,
	 * 4 yellow, 5 white terracotta.
	 */
	public static final int[] BAND_PALETTE = {0, 1, 2, 1, 3, 2, 3, 4, 3, 4, 5, 4, 5};
	private static final int[] PALETTE_RGB = {0xAA1A08, 0x8E3C2E, 0x985E43, 0xA15325, 0xBA8523, 0xD1B1A1};

	/** Colour of terrace {@code i} (as RGB, for the client's glow). */
	public static int bandColor(int i) {
		return PALETTE_RGB[BAND_PALETTE[MathHelper.clamp(i, 0, BANDS)]];
	}

	// ------------------------------------------------------------------ Alien Death Ray

	/** Crystal spires: {x offset, z offset, height, base radius, lean x, lean z}. */
	public static double[][] spires(long seed, double reach) {
		int n = (int) MathHelper.clamp(reach * 0.3, 10, 40);
		double[][] out = new double[n][];
		for (int i = 0; i < n; i++) {
			double a = hash(seed, i, 11) * Math.PI * 2;
			double d = reach * (0.22 + 0.72 * Math.sqrt(hash(seed, i, 12)));
			double h = 10 + 26 * hash(seed, i, 13) * (1.2 - d / reach);
			double r0 = 1.4 + 2.2 * hash(seed, i, 14);
			double la = hash(seed, i, 15) * Math.PI * 2, lean = 0.08 + 0.22 * hash(seed, i, 16);
			out[i] = new double[] {Math.cos(a) * d, Math.sin(a) * d, h, r0, Math.cos(la) * lean, Math.sin(la) * lean};
		}
		return out;
	}

	/** Radius of the glass-walled shaft the rocket punches through the world. */
	public static double shaftRadius(double reach) {
		return MathHelper.clamp(reach * 0.05, 4, 8);
	}
}
