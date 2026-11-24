package com.armageddon.util;

import net.minecraft.util.math.MathHelper;

/**
 * Timings and geometry for XENOFORM, shared by the server event and the client effect: a hypercube unfolds over the
 * land, scans it, takes it apart and sorts it by colour into rings, then folds itself into a monolith on the flat
 * alien grid it leaves behind.
 */
public final class XenoShapes {
	private XenoShapes() {
	}

	public static final int UNFOLD = 60, UNFOLDED = 200, SCAN = 200, SCAN_END = 330, HARVEST = 330, FRONT_END = 680, HARVEST_END = 740,
			COMPRESS = 800, LAND = 890, DEPART = 920, END = 1020, CINE = 1000;
	/** Height of the hypercube over the ground, its half-size, and the default radius of the harvest. */
	public static final double H = 58, SIZE = 14, R = 110;
	/** The new floor, relative to the bomb's ground, and the monolith's height. */
	public static final int FLOOR = -3, MONOLITH = 36;
	/** Colour rings round the hypercube. */
	public static final int RINGS = 6;

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	/** How far out the harvest has reached at tick {@code t}, for radius {@code r}. */
	public static double front(double t, double r) {
		double u = MathHelper.clamp((t - HARVEST) / (FRONT_END - HARVEST), 0.0, 1.0);
		return r * Math.pow(u, 0.8);
	}

	/** When the front reaches distance {@code d}: the inverse of {@link #front}. */
	public static double reach(double d, double r) {
		return HARVEST + Math.pow(MathHelper.clamp(d / r, 0.0, 1.0), 1 / 0.8) * (FRONT_END - HARVEST);
	}

	/** The scanning plane's height over the ground: down through the land and back up. */
	public static double scanY(double t) {
		double u = MathHelper.clamp((t - SCAN) / (SCAN_END - SCAN), 0.0, 1.0);
		return -8 + (H + 8) * (1 - Math.sin(Math.PI * u));
	}

	/** How unfolded the hypercube is (0..1). */
	public static double unfold(double t) {
		return smooth((t - UNFOLD) / (UNFOLDED - UNFOLD));
	}

	/** How far the rings and the hypercube have collapsed (0..1). */
	public static double compress(double t) {
		return smooth((t - COMPRESS) / (LAND - COMPRESS));
	}

	/** Height of the hypercube's centre: it sinks onto the ground as it folds into the monolith. */
	public static double coreY(double t) {
		return H + (MONOLITH * 0.5 - H) * compress(t);
	}

	public static double ringY(int k) {
		return H - 12 + k * 4.5;
	}

	public static double ringR(int k) {
		return 32 + k * 7;
	}
}
