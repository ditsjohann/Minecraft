package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Deterministic layouts for the CITY DESTRUCTION tier, shared by the server (which places the
 * real blocks) and the client (which draws the matching set pieces), so both agree exactly.
 */
public final class CityShapes {
	private CityShapes() {
	}

	public static double hash(long seed, int i) {
		long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return ((h >>> 11) & 0xFFFFF) / (double) 0xFFFFF;
	}

	/** Smooth value noise in 3D, about one feature per {@code cell} blocks. */
	public static double noise(long seed, double x, double y, double z, double cell) {
		x /= cell;
		y /= cell;
		z /= cell;
		int ix = MathHelper.floor(x), iy = MathHelper.floor(y), iz = MathHelper.floor(z);
		double fx = x - ix, fy = y - iy, fz = z - iz;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		fz = fz * fz * (3 - 2 * fz);
		double v = 0;
		for (int c = 0; c < 8; c++) {
			int dx = c & 1, dy = (c >> 1) & 1, dz = (c >> 2) & 1;
			double w = (dx == 1 ? fx : 1 - fx) * (dy == 1 ? fy : 1 - fy) * (dz == 1 ? fz : 1 - fz);
			long k = ((long) (ix + dx) * 73856093L) ^ ((long) (iy + dy) * 19349663L) ^ ((long) (iz + dz) * 83492791L);
			v += w * hash(seed, (int) (k ^ (k >>> 32)));
		}
		return v;
	}

	// ------------------------------------------------------------------ MOONFALL

	public static final int MOON_FALL = 300;

	/** Radius of the moon that stays, for a max-radius cap. */
	public static float moonRadius(float cap) {
		return Math.max(12.0F, Math.min(34.0F, cap * 0.5F));
	}

	// ------------------------------------------------------------------ BIG BANG

	public static final int BANG = 90, PLANETS = 6, PLANET_TYPES = 7;

	public static int planetFormAt(int i) {
		return BANG + 26 + i * 22;
	}

	/** Planet centre relative to the singularity point. */
	public static Vec3d planetOffset(long seed, int i, double scale) {
		double a = i * 2.39996 + hash(seed, i) * 0.7;
		double d = scale * (0.38 + 0.55 * hash(seed, i + 11));
		double y = scale * (0.32 + 0.45 * hash(seed, i + 23));
		return new Vec3d(Math.cos(a) * d, y, Math.sin(a) * d);
	}

	public static double planetRadius(long seed, int i, double scale) {
		return Math.max(2.5, scale * (0.055 + 0.06 * hash(seed, i + 31)));
	}

	/** Every planet in one Big Bang is a different kind. */
	public static int planetType(long seed, int i) {
		return (i + (int) (hash(seed, 99) * PLANET_TYPES)) % PLANET_TYPES;
	}

	public static boolean planetRinged(long seed, int i) {
		return hash(seed, i + 51) < 0.4;
	}

	public static final int NEBULAE = 3;

	public static int nebulaAt(int k) {
		return BANG + 30 + k * 40;
	}

	public static Vec3d nebulaOffset(long seed, int k, double scale) {
		double a = hash(seed, k + 60) * Math.PI * 2;
		double d = scale * (0.45 + 0.4 * hash(seed, k + 61));
		double y = scale * (0.55 + 0.3 * hash(seed, k + 62));
		return new Vec3d(Math.cos(a) * d, y, Math.sin(a) * d);
	}

	/** Planet palette as a colour, for the client's accretion glow. */
	public static int planetColor(int type) {
		return switch (type) {
			case 0 -> 0xB070FF; // geode
			case 1 -> 0xFF5020; // magma
			case 2 -> 0x40E0D0; // ocean
			case 3 -> 0x70E060; // jungle
			case 4 -> 0xC8F0FF; // ice
			case 5 -> 0x40C8A0; // copper patina
			default -> 0xE8E0A0; // end
		};
	}

	// ------------------------------------------------------------------ WORLD EATER

	/** The world worm: when it surfaces, how long it runs, how many body segments and how far apart (in ticks of path). */
	public static final int WORM_START = 70, WORM_RUN = 560, WORM_SEGMENTS = 56;
	public static final double WORM_R = 8.5, WORM_LAG = 1.2, WORM_SWING = 34;

	/**
	 * Head of the world worm at event tick {@code t}, as an offset from the centre's ground (x, height, z). It weaves a
	 * figure across the whole field so it passes over everything, and dives in and out of the ground as it goes.
	 */
	public static Vec3d worm(double t, double reach, double phase) {
		double s = Math.max(0, Math.min(WORM_RUN + 40, t - WORM_START)) * 0.021;
		double x = reach * 0.9 * Math.sin(s + phase);
		double z = reach * 0.9 * Math.sin(s * 1.618 + phase * 2);
		double y = -WORM_SWING * Math.cos(s * 2.7) - 4;
		return new Vec3d(x, y, z);
	}
}
