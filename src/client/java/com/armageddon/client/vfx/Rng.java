package com.armageddon.client.vfx;

/** SplitMix64: tiny, fast and seedable, so every client plays the same effect from the same seed. */
public final class Rng {
	private long state;

	public Rng(long seed) {
		this.state = seed;
	}

	public long nextLong() {
		long z = (state += 0x9E3779B97F4A7C15L);
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** 0 (inclusive) .. 1 (exclusive). */
	public float next() {
		return (nextLong() >>> 40) * 0x1.0p-24F;
	}

	public float range(float min, float max) {
		return min + (max - min) * next();
	}

	public int range(int min, int max) {
		return max <= min ? min : min + (int) (next() * (max - min + 1));
	}

	/** -1 .. 1. */
	public float signed() {
		return next() * 2.0F - 1.0F;
	}

	/** A uniformly random unit vector into {@code out[o..o+2]}. */
	public void unit(float[] out, int o) {
		float z = signed();
		float a = next() * (float) (Math.PI * 2);
		float r = (float) Math.sqrt(Math.max(0, 1 - z * z));
		out[o] = r * (float) Math.cos(a);
		out[o + 1] = z;
		out[o + 2] = r * (float) Math.sin(a);
	}
}
