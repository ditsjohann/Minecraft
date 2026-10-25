package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The DISCO BALL's timeline and its light beams, shared by the server (which burns where the beams
 * land) and the client (which draws them), so a scorch mark is always under a beam.
 * All times are ticks since the bomb went off.
 */
public final class DiscoBeams {
	private DiscoBeams() {
	}

	/** Ball radius, hanging height above the bomb, and the start height of its descent. */
	public static final double RADIUS = 14, HANG = 56, DROP_FROM = 210;
	/** Timeline. */
	public static final int DESCEND_END = 100, SPIN_START = 100, SPIN_FULL = 300, BRAKE_START = 380, STOP = 520,
			DROP = 560, CINE = 690, TOTAL = DROP + 520;
	/** Beams burn between these ticks (while they are still hot, i.e. not yet blue). */
	public static final int HOT_START = 150, HOT_END = 440;
	public static final int BEAMS = 24;
	public static final float MAX_SPIN = 0.55F;

	private static final float[] ANGLE = new float[TOTAL + 2];

	static {
		double a = 0;
		for (int t = 0; t < ANGLE.length; t++) {
			ANGLE[t] = (float) a;
			a += omega(t);
		}
	}

	private static double smooth(double t) {
		t = MathHelper.clamp(t, 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	/** Spin speed in radians per tick. */
	public static double omega(double t) {
		if (t < SPIN_START) {
			return 0.004;
		}
		if (t < SPIN_FULL) {
			double u = (t - SPIN_START) / (SPIN_FULL - SPIN_START);
			return 0.01 + MAX_SPIN * u * u * u;
		}
		if (t < BRAKE_START) {
			return 0.01 + MAX_SPIN;
		}
		if (t < STOP) {
			return (0.01 + MAX_SPIN) * (1 - smooth((t - BRAKE_START) / (STOP - BRAKE_START)));
		}
		if (t < DROP) {
			return 0;
		}
		return 0.035;
	}

	/** Spin angle at time {@code t} (interpolated between ticks). */
	public static double angle(double t) {
		if (t <= 0) {
			return 0;
		}
		int i = Math.min(ANGLE.length - 2, (int) t);
		double f = Math.min(1.0, t - i);
		return ANGLE[i] + (ANGLE[i + 1] - ANGLE[i]) * f;
	}

	/** Ball centre: lowered on its chain, then hanging (it sways a little with the spin). */
	public static Vec3d ball(Vec3d anchor, double t) {
		double u = smooth(t / DESCEND_END);
		double y = MathHelper.lerp(u, DROP_FROM, HANG);
		double sway = Math.min(1.0, omega(t) / MAX_SPIN) * 0.8;
		return anchor.add(Math.sin(t * 0.21) * sway, y + Math.sin(t * 0.05) * 0.6, Math.cos(t * 0.17) * sway);
	}

	/** 0 = party colours, 1 = icy blue. */
	public static double blue(double t) {
		if (t >= DROP) {
			return 0;
		}
		return smooth((t - 300) / 130.0);
	}

	/** Beam brightness 0..1 (off during the descent and the silence; soft in the afterparty). */
	public static double intensity(double t) {
		if (t < SPIN_START) {
			return 0;
		}
		if (t < STOP) {
			double on = smooth((t - SPIN_START) / 40.0);
			double off = t > BRAKE_START ? 1 - smooth((t - (STOP - 50)) / 50.0) : 1;
			return on * off;
		}
		if (t < DROP + 10) {
			return 0;
		}
		return 0.4 * smooth((t - DROP - 10) / 20.0) * (1 - smooth((t - (TOTAL - 40)) / 40.0));
	}

	public static boolean hot(double t) {
		return t >= HOT_START && t < HOT_END && blue(t) < 0.85;
	}

	private static double hash(int i, int salt) {
		long h = i * 374761393L + salt * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	/** Unit direction of beam {@code i}: it turns with the ball and dips up and down as facets pass. */
	public static Vec3d dir(int i, double t) {
		double spin = angle(t);
		double az = i * MathHelper.TAU / BEAMS + hash(i, 1) * 0.4 + spin * (i % 3 == 0 ? -0.6 : 1.0);
		double dip = Math.toRadians(14 + hash(i, 2) * 44 + 6 * Math.sin(spin * 0.37 + i * 1.7));
		return new Vec3d(Math.cos(az) * Math.cos(dip), -Math.sin(dip), Math.sin(az) * Math.cos(dip));
	}

	/** Where beam {@code i} meets the bomb's ground level (the caller snaps it to the real terrain). */
	public static Vec3d hit(Vec3d anchor, int i, double t) {
		Vec3d b = ball(anchor, t);
		Vec3d d = dir(i, t);
		double k = (b.y - anchor.y) / Math.max(0.05, -d.y);
		return b.add(d.multiply(k));
	}

	/** Beam colour as 0xRRGGBB: a rainbow that cycles, draining to icy blue as the ball cools. */
	public static int color(int i, double t) {
		float h = (float) (i / (double) BEAMS + t * 0.004);
		int c = MathHelper.hsvToRgb(((h % 1.0F) + 1.0F) % 1.0F, 0.8F, 1.0F);
		double b = blue(t);
		if (b <= 0) {
			return c;
		}
		int r = (int) MathHelper.lerp(b, (c >> 16) & 0xFF, 0x50);
		int g = (int) MathHelper.lerp(b, (c >> 8) & 0xFF, 0xB0);
		int bl = (int) MathHelper.lerp(b, c & 0xFF, 0xFF);
		return (r << 16) | (g << 8) | bl;
	}
}
