package com.armageddon.client.vfx;

/**
 * An ARGB colour ramp over 0..1 with any number of stops, baked to a table. Alpha in the stops multiplies the
 * particle's alpha curve, so a ramp can also fade.
 */
public final class Gradient {
	private static final int N = 128;
	private final int[] table = new int[N + 1];

	private Gradient() {
	}

	public int at(float x) {
		int i = (int) (Math.max(0.0F, Math.min(1.0F, x)) * N + 0.5F);
		return table[i];
	}

	/**
	 * Stops as {@code t, argb, t, argb, ...}. A stop given as plain RGB ({@code 0xRRGGBB}, alpha byte 0) is opaque;
	 * write {@code 0x01RRGGBB}-style values only through {@link #argb} when you want a real alpha.
	 */
	public static Gradient of(Object... stops) {
		if (stops.length < 2 || stops.length % 2 != 0) {
			throw new IllegalArgumentException("gradient stops come in (t, colour) pairs");
		}
		int n = stops.length / 2;
		float[] t = new float[n];
		int[] c = new int[n];
		for (int i = 0; i < n; i++) {
			t[i] = ((Number) stops[i * 2]).floatValue();
			int v = ((Number) stops[i * 2 + 1]).intValue();
			c[i] = (v >>> 24) == 0 ? v | 0xFF000000 : v;
		}
		Gradient g = new Gradient();
		for (int k = 0; k <= N; k++) {
			float x = k / (float) N;
			int col = c[n - 1];
			if (x <= t[0]) {
				col = c[0];
			} else {
				for (int i = 1; i < n; i++) {
					if (x <= t[i]) {
						col = lerp(c[i - 1], c[i], (x - t[i - 1]) / Math.max(1.0E-6F, t[i] - t[i - 1]));
						break;
					}
				}
			}
			g.table[k] = col;
		}
		return g;
	}

	public static Gradient solid(int rgb) {
		return of(0, rgb, 1, rgb);
	}

	/** Packs a colour with an explicit alpha (0..1) for a gradient stop. */
	public static int argb(int rgb, float alpha) {
		int a = Math.max(1, Math.min(255, (int) (alpha * 255)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	static int lerp(int a, int b, float t) {
		int r = 0;
		for (int s = 0; s <= 24; s += 8) {
			int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
			r |= ((int) (ca + (cb - ca) * t) & 0xFF) << s;
		}
		return r;
	}
}
