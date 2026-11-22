package com.armageddon.client.vfx;

import java.util.function.DoubleUnaryOperator;

/**
 * A value over 0..1 (a particle's life, an emitter's run), baked to a small table on creation so a lookup is two
 * reads and a lerp. Use the presets for the common shapes; {@link #keys} for anything else.
 */
public final class Curve {
	private static final int N = 64;
	private final float[] table = new float[N + 1];

	private Curve() {
	}

	public float at(float x) {
		if (x <= 0.0F) {
			return table[0];
		}
		if (x >= 1.0F) {
			return table[N];
		}
		float f = x * N;
		int i = (int) f;
		return table[i] + (table[i + 1] - table[i]) * (f - i);
	}

	public static Curve of(DoubleUnaryOperator f) {
		Curve c = new Curve();
		for (int i = 0; i <= N; i++) {
			c.table[i] = (float) f.applyAsDouble(i / (double) N);
		}
		return c;
	}

	/** Piecewise-linear through {@code t0, v0, t1, v1, ...} (t ascending), flat before the first and after the last. */
	public static Curve keys(float... k) {
		if (k.length < 2 || k.length % 2 != 0) {
			throw new IllegalArgumentException("curve keys come in (t, value) pairs");
		}
		return of(x -> {
			if (x <= k[0]) {
				return k[1];
			}
			for (int i = 2; i < k.length; i += 2) {
				if (x <= k[i]) {
					double u = (x - k[i - 2]) / Math.max(1.0E-6, k[i] - k[i - 2]);
					return k[i - 1] + (k[i + 1] - k[i - 1]) * u;
				}
			}
			return k[k.length - 1];
		});
	}

	public static Curve constant(float v) {
		return of(x -> v);
	}

	/** 1 all the way. */
	public static final Curve ONE = constant(1.0F);
	/** 0 → 1. */
	public static final Curve RISE = of(x -> x);
	/** 1 → 0. */
	public static final Curve FALL = of(x -> 1 - x);
	/** 0 → 1, fast then settling: growth of smoke, rings, shock fronts. */
	public static final Curve EASE_OUT = of(x -> 1 - (1 - x) * (1 - x));
	/** 0 → 1, slow then fast. */
	public static final Curve EASE_IN = of(x -> x * x);
	/** Size "pop": overshoots to 1.2 in the first tenth of life, settles at 1, shrinks a little at the end. */
	public static final Curve POP = keys(0, 0.15F, 0.1F, 1.2F, 0.3F, 1.0F, 1, 0.85F);
	/** Fade in quickly, hold, fade out: the default alpha for anything that should not pop in or out. */
	public static final Curve FADE = keys(0, 0, 0.08F, 1, 0.6F, 1, 1, 0);
	/** Full, then a long fade: sparks, flashes. */
	public static final Curve FADE_OUT = keys(0, 1, 0.35F, 1, 1, 0);
	/** Fades in slowly, out slowly: drifting gas, lingering smoke. */
	public static final Curve BELL = keys(0, 0, 0.25F, 1, 0.7F, 1, 1, 0);
	/** Hot early, cooling: emission that hands smoke back to world light. */
	public static final Curve COOL = of(x -> Math.max(0, 1 - x * 2.5) * Math.max(0, 1 - x * 2.5));
}
