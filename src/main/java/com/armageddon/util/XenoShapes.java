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
			COMPRESS = 800, LAND = 890, SHIP = 920, DEPART = 980, LEAVE = 1070, END = 1150, CINE = 1000;
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

	/** The Watchers: eight tall faceless things standing round the harvest, glitching a step closer now and then. */
	public static final int WATCHERS = 8, WATCH = HARVEST + 60, WATCH_STEP = 90;
	/** Height of the alien ship's ring over the ground, and its radius. */
	public static final double SHIP_H = 190, SHIP_R = 120;

	/** Where watcher {@code i} stands at tick {@code t}, for a harvest of radius {@code r} (relative, on the ground). */
	public static net.minecraft.util.math.Vec3d watcher(int i, double t, double r) {
		double a = i * MathHelper.TAU / WATCHERS + hash(i, 81) * 0.4;
		int steps = (int) Math.max(0, Math.min((t - WATCH) / WATCH_STEP, (LAND - WATCH) / WATCH_STEP));
		double d = r * 0.95 - steps * r * 0.07 - hash(i, 82) * 6;
		return new net.minecraft.util.math.Vec3d(Math.cos(a) * d, 0, Math.sin(a) * d);
	}

	/** How present the watchers are (0..1): flickering in, standing, dissolving up into the ship at the end. */
	public static double watching(double t) {
		return smooth((t - WATCH) / 30.0) * (1 - smooth((t - DEPART - 20) / 50.0));
	}

	/** Where the ship's ring hangs at tick {@code t}: descending out of the sky, then gone in a streak. */
	public static double shipY(double t) {
		double down = smooth((t - SHIP) / 60.0);
		double up = Math.max(0, t - LEAVE);
		return SHIP_H + (1 - down) * 160 + up * up * 0.6;
	}
}
