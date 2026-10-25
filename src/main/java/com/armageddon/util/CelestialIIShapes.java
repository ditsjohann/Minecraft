package com.armageddon.util;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic shapes shared by the Celestial II events (real blocks) and their client visuals, so a crop circle
 * glows exactly where it is cut and the blight fog rolls exactly where the land has turned.
 */
public final class CelestialIIShapes {
	private CelestialIIShapes() {
	}

	public static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	/** Smooth value noise in 0..1 (bilinear over a hashed lattice). */
	public static double noise(double x, double z, int salt) {
		int ix = MathHelper.floor(x), iz = MathHelper.floor(z);
		double fx = x - ix, fz = z - iz;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		double a = hash(ix * 31 + salt, iz), b = hash((ix + 1) * 31 + salt, iz);
		double c = hash(ix * 31 + salt, iz + 1), d = hash((ix + 1) * 31 + salt, iz + 1);
		return MathHelper.lerp(fz, MathHelper.lerp(fx, a, b), MathHelper.lerp(fx, c, d));
	}

	// ------------------------------------------------------------------ exploding cows

	/** Black patch of the giant cow-print stamped over the land (else white). */
	public static boolean cowPatch(int x, int z, int salt) {
		double n = noise(x / 11.0, z / 11.0, salt) * 0.7 + noise(x / 4.5, z / 4.5, salt + 7) * 0.3;
		return n > 0.56;
	}

	/**
	 * Top-down silhouette of a cow lying on its side, in units of {@code s} (body length ~ 2.4 s),
	 * local frame: u along the body (head at +u), v across. True if (u, v) is inside.
	 */
	public static boolean cowSilhouette(double u, double v, double s) {
		u /= s;
		v /= s;
		// Body.
		if (sq(u / 1.05) + sq(v / 0.55) <= 1) {
			return true;
		}
		// Head and muzzle.
		if (sq((u - 1.35) / 0.42) + sq((v - 0.08) / 0.3) <= 1) {
			return true;
		}
		// Ears / horns.
		if (sq((u - 1.45) / 0.12) + sq((v - 0.42) / 0.16) <= 1 || sq((u - 1.45) / 0.12) + sq((v + 0.26) / 0.16) <= 1) {
			return true;
		}
		// Four legs sticking out.
		for (int k = 0; k < 4; k++) {
			double lu = k < 2 ? 0.55 + k * 0.18 : -0.62 + (k - 2) * 0.2;
			if (Math.abs(u - lu) < 0.09 && v > 0.4 && v < 1.15) {
				return true;
			}
		}
		// Tail.
		return Math.abs(v - (u + 1.05) * 0.4) < 0.06 && u < -0.95 && u > -1.45;
	}

	private static double sq(double x) {
		return x * x;
	}

	// ------------------------------------------------------------------ colossal spear

	public static final int SIGIL_R = 30, SIGIL_SPOKES = 12, SIGIL_GLYPHS = 24;

	/** How much of the rune sigil has ignited, 0..1, from drill progress. */
	public static double sigilLit(double drillProgress) {
		return MathHelper.clamp(drillProgress * 1.15, 0, 1);
	}

	/** Polar membership of the sigil's carved lines: ring radii and spokes. 0 none, 1 gold line, 2 molten line. */
	public static int sigil(double dx, double dz) {
		double r = Math.sqrt(dx * dx + dz * dz);
		if (r > SIGIL_R + 1 || r < 14) {
			return 0;
		}
		if (Math.abs(r - SIGIL_R) < 0.75 || Math.abs(r - (SIGIL_R - 4)) < 0.6) {
			return 1;
		}
		if (Math.abs(r - 18) < 0.6) {
			return 2;
		}
		double ang = Math.atan2(dz, dx);
		double sector = MathHelper.TAU / SIGIL_SPOKES;
		double k = ang / sector;
		double off = (k - Math.floor(k + 0.5)) * sector * r;
		if (Math.abs(off) < 0.6 && r > 18 && r < SIGIL_R - 4) {
			return 1;
		}
		// Glyph ticks between the two outer rings.
		if (r > SIGIL_R - 4 && r < SIGIL_R) {
			double g = ang / (MathHelper.TAU / SIGIL_GLYPHS);
			double gi = Math.floor(g);
			double f = g - gi;
			int shape = (int) (hash((int) gi, 5) * 4);
			double rr = (r - (SIGIL_R - 4)) / 4.0;
			boolean on = switch (shape) {
				case 0 -> Math.abs(f - 0.5) < 0.08;
				case 1 -> Math.abs(f - rr) < 0.1;
				case 2 -> Math.abs(f - 0.3) < 0.07 || Math.abs(f - 0.7) < 0.07;
				default -> Math.abs(rr - 0.5) < 0.18 && Math.abs(f - 0.5) < 0.3;
			};
			return on ? 2 : 0;
		}
		return 0;
	}

	// ------------------------------------------------------------------ zombie apocalypse

	public static final double BLIGHT_MAX = 92;

	/** Radius the blight has crept to {@code t} ticks into the event. */
	public static double blightRadius(double t) {
		return BLIGHT_MAX * (1 - Math.exp(-Math.max(0, t - 40) / 520.0));
	}

	/** Blight reaches (dx, dz) at radius r: a ragged front with fingers racing ahead. */
	public static boolean blighted(double dx, double dz, double r) {
		double d = Math.sqrt(dx * dx + dz * dz);
		double ang = Math.atan2(dz, dx);
		double fingers = noise(ang * 4.0 + 10, 0.5, 91) * 0.55 + noise(d / 9.0, ang * 2.5, 92) * 0.25;
		return d < r * (0.62 + fingers);
	}

	// ------------------------------------------------------------------ ufo crop circles

	/** One element of a crop circle: a ring (r, width), or a dot (x, z, r) in the pattern's local frame. */
	public record Mark(boolean ring, double x, double z, double r, double w) {
	}

	public static final int CROP_EVERY = 22;

	/** The crop circle stamped by the {@code index}-th pulse: concentric rings, satellite circles, a path. */
	public static List<Mark> crop(int index) {
		List<Mark> out = new ArrayList<>();
		int style = (int) (hash(index, 11) * 4);
		double R = 11 + hash(index, 12) * 7;
		out.add(new Mark(true, 0, 0, R, 1.2));
		out.add(new Mark(false, 0, 0, 2.5 + hash(index, 13) * 2, 0));
		switch (style) {
			case 0 -> {
				// Rings within rings.
				out.add(new Mark(true, 0, 0, R * 0.62, 1.0));
				out.add(new Mark(true, 0, 0, R * 0.32, 0.9));
			}
			case 1 -> {
				// A ring of satellite discs.
				int n = 5 + (int) (hash(index, 14) * 4);
				for (int k = 0; k < n; k++) {
					double a = MathHelper.TAU * k / n;
					out.add(new Mark(false, Math.cos(a) * R * 1.32, Math.sin(a) * R * 1.32, 2.2, 0));
				}
				out.add(new Mark(true, 0, 0, R * 0.55, 1.0));
			}
			case 2 -> {
				// Triangle of rings (a "triskelion" read).
				for (int k = 0; k < 3; k++) {
					double a = MathHelper.TAU * k / 3 + 0.5;
					out.add(new Mark(true, Math.cos(a) * R * 0.5, Math.sin(a) * R * 0.5, R * 0.38, 0.9));
				}
			}
			default -> {
				// Moon phases in a line.
				for (int k = -3; k <= 3; k++) {
					if (k != 0) {
						out.add(new Mark(Math.abs(k) % 2 == 0, k * R * 0.42, 0, 1.4 + (3 - Math.abs(k)) * 0.8, 0.7));
					}
				}
				out.add(new Mark(true, 0, 0, R * 0.7, 1.0));
			}
		}
		return out;
	}

	/** True if pattern-local point (x, z) lies on a mark of {@code marks}. */
	public static boolean onCrop(List<Mark> marks, double x, double z) {
		for (Mark m : marks) {
			double d = Math.sqrt((x - m.x()) * (x - m.x()) + (z - m.z()) * (z - m.z()));
			if (m.ring() ? Math.abs(d - m.r()) <= m.w() * 0.5 + 0.3 : d <= m.r()) {
				return true;
			}
		}
		return false;
	}

	/** Pattern rotation, so circles are not all aligned. */
	public static double cropAngle(int index) {
		return hash(index, 15) * MathHelper.TAU;
	}
}
