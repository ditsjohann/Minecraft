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

	// ---------------------------------------------------------------- missiles

	public static final int MISSILES = 26, MISSILE0 = 420, MISSILE_GAP = 22, MISSILE_FLIGHT = 40;

	public static int missileAt(int k) {
		return MISSILE0 + k * MISSILE_GAP;
	}

	/** Where missile {@code k} comes down, relative to the core's foot (y = 0). */
	public static Vec3d missileTarget(int k, int seed) {
		double a = hash(seed + k, 61) * MathHelper.TAU, r = 22 + hash(seed + k, 62) * (R - 30);
		return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
	}

	/** Missile {@code k} in flight at tick {@code t}: up out of the core's crown, over, and down; {@code dy} is its target's ground height. */
	public static Vec3d missile(int k, double t, int seed, double dy) {
		double u = MathHelper.clamp((t - missileAt(k)) / MISSILE_FLIGHT, 0.0, 1.0);
		Vec3d from = new Vec3d(0, CORE_H + 4, 0), to = missileTarget(k, seed).add(0, dy, 0);
		Vec3d mid = from.lerp(to, 0.4).add(0, 45, 0);
		return from.multiply((1 - u) * (1 - u)).add(mid.multiply(2 * u * (1 - u))).add(to.multiply(u * u));
	}

	// ---------------------------------------------------------------- tripods

	public static final int WALKERS = 3, WALK = 420, WALK_END = 990;
	public static final double WALKER_H = 30, LEG = 34;

	/** Where walker {@code i} stands at tick {@code t}: striding across the city from one edge to the other. */
	public static Vec3d walker(int i, double t, int seed) {
		double a = i * MathHelper.TAU / WALKERS + hash(seed + i, 71) * 0.8;
		Vec3d from = new Vec3d(Math.cos(a) * R * 0.85, 0, Math.sin(a) * R * 0.85);
		double b = a + Math.PI + (hash(seed + i, 72) - 0.5) * 1.2;
		Vec3d to = new Vec3d(Math.cos(b) * R * 0.85, 0, Math.sin(b) * R * 0.85);
		double u = MathHelper.clamp((t - WALK) / (WALK_END - WALK), 0.0, 1.0);
		Vec3d p = from.lerp(to, u);
		Vec3d d = to.subtract(from).normalize(), side = new Vec3d(-d.z, 0, d.x);
		return p.add(side.multiply(Math.sin(u * 9 + i) * 10));
	}

	/** How far walker {@code i} has risen out of the street (0..1), and how far it has sunk back at the end. */
	public static double walkerUp(double t) {
		return smooth((t - WALK + 40) / 40.0) * (1 - smooth((t - WALK_END) / 30.0));
	}

	/** Where walker {@code i}'s beam strikes the ground: ahead of it, sweeping from side to side. */
	public static Vec3d beam(int i, double t, int seed) {
		Vec3d p = walker(i, t, seed), q = walker(i, t + 1, seed);
		Vec3d d = q.subtract(p);
		d = d.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : d.normalize();
		Vec3d side = new Vec3d(-d.z, 0, d.x);
		return p.add(d.multiply(16)).add(side.multiply(Math.sin(t * 0.06 + i * 2) * 18));
	}
}
