package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Timings and geometry for AI TAKEOVER, shared by the server event and the client effect: the core, the circuit
 * traces, the drone swarm and the purge grid.
 */
public final class AiShapes {
	private AiShapes() {
	}

	public static final int BOOT = 20, BOOTED = 140, NETWORK = 160, NETWORK_END = 560, DRONES = 300, HUNT = 360, WARN = 1000, PURGE = 1040,
			PURGE_END = 1220, UPLOAD = 1240, EMP = 1340, END = 1460;
	/** Reach of the takeover, the core's height, the eye's height, and the spacing of the purge grid. */
	public static final double R = 130, CORE_H = 56, EYE_Y = 63, GRID = 24;
	public static final int DRONE_COUNT = 72;

	public static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	/** How far the assimilation has spread at tick {@code t}. */
	public static double front(double t) {
		return R * smooth((t - NETWORK) / (NETWORK_END - NETWORK));
	}

	/** How far the purge grid has cut out from the core at tick {@code t}. */
	public static double purge(double t) {
		return R * MathHelper.clamp((t - PURGE) / (PURGE_END - PURGE), 0.0, 1.0);
	}

	/** True if a column at this offset from the core lies on a purge cut. */
	public static boolean onCut(double dx, double dz) {
		double gx = Math.floorMod((int) Math.floor(dx + GRID / 2), (int) GRID), gz = Math.floorMod((int) Math.floor(dz + GRID / 2), (int) GRID);
		return gx < 2 || gz < 2;
	}

	/**
	 * Circuit traces running out from the core like the tracks on a circuit board: straight runs that turn by
	 * forty-five degrees. Each point is {x, z, distance from the core}.
	 */
	public static List<double[]> traces(int seed) {
		List<double[]> out = new ArrayList<>();
		for (int k = 0; k < 16; k++) {
			int dir = k % 8;
			double x = Math.cos(dir * Math.PI / 4) * 6, z = Math.sin(dir * Math.PI / 4) * 6;
			x += (k >= 8 ? 1 : -1) * Math.sin(dir * Math.PI / 4) * 2.5;
			z += (k >= 8 ? -1 : 1) * Math.cos(dir * Math.PI / 4) * 2.5;
			int run = 0, len = 6 + (int) (hash(seed + k, 0) * 14);
			for (int i = 0; i < 400; i++) {
				double a = dir * Math.PI / 4;
				x += Math.round(Math.cos(a));
				z += Math.round(Math.sin(a));
				double d = Math.hypot(x, z);
				if (d > R) {
					break;
				}
				out.add(new double[]{x, z, d});
				if (++run >= len) {
					run = 0;
					len = 6 + (int) (hash(seed + k, i) * 16);
					// Turn 45 degrees, but keep heading outward.
					int turn = hash(seed * 3 + k, i) < 0.5 ? 1 : -1;
					int nd = Math.floorMod(dir + turn, 8);
					double na = nd * Math.PI / 4;
					if (Math.cos(na) * x + Math.sin(na) * z < 0) {
						nd = Math.floorMod(dir - turn, 8);
					}
					dir = nd;
				}
			}
		}
		return out;
	}

	/** When drone {@code i} leaves the core. */
	public static int launch(int i) {
		return DRONES + i * 7;
	}

	/** Where drone {@code i} is at tick {@code t}, relative to the core's foot. */
	public static Vec3d drone(int i, double t, int seed) {
		Vec3d eye = new Vec3d(0, EYE_Y, 0);
		double l = launch(i);
		if (t < l) {
			return eye;
		}
		double r = 24 + hash(seed + i, 1) * (R - 30);
		double h = 22 + hash(seed + i, 2) * 46;
		double w = (0.008 + hash(seed + i, 3) * 0.016) * (i % 2 == 0 ? 1 : -1);
		double a0 = hash(seed + i, 4) * MathHelper.TAU;
		double a = a0 + (t - l) * w;
		Vec3d orbit = new Vec3d(Math.cos(a) * r, h + 4 * Math.sin(t * 0.05 + i), Math.sin(a) * r)
				.add(Math.sin(t * 0.03 + i * 2) * 5, 0, Math.cos(t * 0.027 + i) * 5);
		double out = smooth((t - l) / 45.0);
		double back = smooth((t - UPLOAD - (i % 12) * 3) / 40.0);
		return eye.lerp(orbit, out * (1 - back));
	}
}
