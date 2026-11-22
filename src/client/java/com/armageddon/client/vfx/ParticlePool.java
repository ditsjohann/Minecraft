package com.armageddon.client.vfx;

import java.util.Arrays;

/**
 * One emitter's particles as flat arrays (no object per particle, no garbage while running). Positions are relative
 * to the effect's origin, so floats stay precise far from 0,0. Removal swaps the last particle into the gap.
 */
public final class ParticlePool {
	public int count;
	private int cap;
	public float[] x, y, z, px, py, pz, vx, vy, vz;
	public float[] age, life, size, rot, prot, spin, seed;
	/** Packed world light, refreshed every few ticks for lit particles. */
	public int[] light;
	/** Times the particle has hit a block. */
	public byte[] hits;
	/** Ticks until the particle's next "every" sub-emission. */
	public short[] timer;

	ParticlePool(int initial) {
		alloc(Math.max(8, initial));
	}

	private void alloc(int n) {
		cap = n;
		x = grow(x, n);
		y = grow(y, n);
		z = grow(z, n);
		px = grow(px, n);
		py = grow(py, n);
		pz = grow(pz, n);
		vx = grow(vx, n);
		vy = grow(vy, n);
		vz = grow(vz, n);
		age = grow(age, n);
		life = grow(life, n);
		size = grow(size, n);
		rot = grow(rot, n);
		prot = grow(prot, n);
		spin = grow(spin, n);
		seed = grow(seed, n);
		light = light == null ? new int[n] : Arrays.copyOf(light, n);
		hits = hits == null ? new byte[n] : Arrays.copyOf(hits, n);
		timer = timer == null ? new short[n] : Arrays.copyOf(timer, n);
	}

	private static float[] grow(float[] a, int n) {
		return a == null ? new float[n] : Arrays.copyOf(a, n);
	}

	/** Index of a fresh particle, or -1 if the pool is at {@code max}. */
	int add(int max) {
		if (count >= max) {
			return -1;
		}
		if (count >= cap) {
			alloc(Math.min(max, cap * 2));
		}
		int i = count++;
		hits[i] = 0;
		timer[i] = 0;
		return i;
	}

	void kill(int i) {
		int j = --count;
		if (i == j) {
			return;
		}
		x[i] = x[j];
		y[i] = y[j];
		z[i] = z[j];
		px[i] = px[j];
		py[i] = py[j];
		pz[i] = pz[j];
		vx[i] = vx[j];
		vy[i] = vy[j];
		vz[i] = vz[j];
		age[i] = age[j];
		life[i] = life[j];
		size[i] = size[j];
		rot[i] = rot[j];
		prot[i] = prot[j];
		spin[i] = spin[j];
		seed[i] = seed[j];
		light[i] = light[j];
		hits[i] = hits[j];
		timer[i] = timer[j];
	}
}
