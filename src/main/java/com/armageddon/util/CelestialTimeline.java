package com.armageddon.util;

/**
 * Shared clocks for the Celestial I events, so the server's block work and the client's signature
 * effects ({@code FxType.CELESTIAL_FX}) land on the same ticks. Effect ids go in the payload seed.
 */
public final class CelestialTimeline {
	private CelestialTimeline() {
	}

	public static final int FX_LAST_LIGHT = 1, FX_ZEROPOINT = 2, FX_UNMAKER = 3, FX_SPEAR = 4, FX_TEMPLE = 5, FX_SHATTER = 6;

	// ---- The Last Light: an accelerating heartbeat, a flatline, then one long exhale.
	public static final int[] LL_BEATS = {40, 78, 112, 142, 168, 190, 208, 222};
	public static final int LL_FLATLINE = 236, LL_EXHALE = 264, LL_END = LL_EXHALE + 420;

	/** Outer radius bleached by beat {@code k} (0-based) for a field of radius {@code r}. */
	public static double llRadius(int k, double r) {
		return r * Math.pow((k + 1) / (double) LL_BEATS.length, 0.8);
	}

	// ---- Zeropoint: the clock slows and stops, twitches backwards, then the moment snaps shut.
	public static final int ZP_REWIND = 112, ZP_SNAP = 150, ZP_END = ZP_SNAP + 300;

	// ---- The Unmaker: a render pass sweeps the land and leaves only its wireframe.
	public static final int UM_START = 60, UM_SCAN = 170, UM_END = UM_START + UM_SCAN + 90;
	public static final int UM_BELOW = 20, UM_ABOVE = 40, UM_CELL = 4;

	/** Position of the scan plane along the sweep axis, from -r to +r. */
	public static double umPlane(double t, double r) {
		double u = Math.max(0.0, Math.min(1.0, (t - UM_START) / UM_SCAN));
		return -r + 2 * r * u;
	}

	/** True if a block at lattice-local coordinates belongs to the wireframe (on at least two grid planes). */
	public static boolean umLattice(int lx, int ly, int lz) {
		int n = (Math.floorMod(lx, UM_CELL) == 0 ? 1 : 0) + (Math.floorMod(ly, UM_CELL) == 0 ? 1 : 0) + (Math.floorMod(lz, UM_CELL) == 0 ? 1 : 0);
		return n >= 2;
	}

	// ---- Godfall: volleys of golden spears, then the last spear presses a temple into the ground.
	public static final int GF_VOLLEY_START = 50, GF_SPEAR_FALL = 18, GF_FINAL = 262, GF_FINAL_FALL = 28;
	public static final int GF_TEMPLE = GF_FINAL + GF_FINAL_FALL, GF_END = GF_TEMPLE + 380, GF_STEPS = 5;

	/** Terrace index (0 = rim .. GF_STEPS = altar) at horizontal distance {@code d} in a temple of radius {@code r}. */
	public static int gfTier(double d, double r) {
		if (d > r) {
			return -1;
		}
		int k = (int) ((r - d) / (r / (GF_STEPS + 1.0)));
		return Math.min(GF_STEPS, Math.max(0, k));
	}

	// ---- Final Existence: the sky cracks, shatters, and the floor of the world falls away.
	public static final int FE_CRACK = 200, FE_BREAK = 360, FE_FALL = 420, FE_END = FE_FALL + 700;
}
