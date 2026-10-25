package com.armageddon.util;

import net.minecraft.util.math.Vec3d;

/**
 * Timeline and geometry of the Eternal Sapling, shared by the server (which builds the tree out of real
 * blocks) and the client (cutscene and aura), so the camera is always looking at the right place.
 */
public final class EternalTree {
	private EternalTree() {
	}

	public static final int TREE1_START = 60, TREE1_END = 120;
	public static final int TREE2_START = 130, TREE2_END = 250;
	public static final int WORLD_START = 265, WORLD_END = 520;
	public static final int ROOTS = 12, ROOTS_START = 530, ROOT_GAP = 12, ROOT_GROW = 16;
	public static final int CINE = 760, LIFE = 820;

	/** World tree height and base trunk radius at full scale. */
	public static double height(double s) {
		return 115 * s;
	}

	public static double trunkRadius(double s) {
		return 9 * s;
	}

	/** Trunk radius at {@code y} blocks above the base: a root flare at the bottom, tapering to the crown. */
	public static double trunkRadiusAt(double s, double y) {
		double h = height(s);
		double r = trunkRadius(s) + (4.5 * s - trunkRadius(s)) * Math.max(0, Math.min(1, y / h));
		if (y < 8 * s) {
			r += (8 * s - y) * 0.9;
		}
		return r;
	}

	/** Height of the growth front of the world tree at event tick {@code t} (blocks above the base). */
	public static double front(double s, double t) {
		double u = Math.max(0, Math.min(1, (t - WORLD_START) / (double) (WORLD_END - WORLD_START)));
		return u * (height(s) + 24 * s);
	}

	public static double rootAngle(int i) {
		return i * Math.PI * 2 / ROOTS + (hash(i, 1) - 0.5) * 0.35;
	}

	public static double rootReach(int i, double s) {
		return (70 + 70 * hash(i, 2)) * Math.max(0.35, s);
	}

	/** Roots launch in an order that keeps hopping round the trunk, so they come from every side. */
	public static int rootLaunch(int i) {
		return ROOTS_START + ((i * 5) % ROOTS) * ROOT_GAP;
	}

	public static int rootSlam(int i) {
		return rootLaunch(i) + ROOT_GROW;
	}

	/** Point {@code u} (0 trunk .. 1 buried tip) along root {@code i}; {@code groundY} is the ground height at its tip. */
	public static Vec3d rootPoint(Vec3d base, int i, double s, double u, double groundY) {
		double a = rootAngle(i);
		Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
		double reach = rootReach(i, s);
		Vec3d p0 = base.add(d.multiply(trunkRadius(s) * 0.6)).add(0, 26 * s, 0);
		Vec3d p1 = base.add(d.multiply(reach * 0.45)).add(0, 70 * s + reach * 0.15, 0);
		Vec3d p2 = new Vec3d(base.x + d.x * reach, groundY - 5, base.z + d.z * reach);
		double v = 1 - u;
		return p0.multiply(v * v).add(p1.multiply(2 * v * u)).add(p2.multiply(u * u));
	}

	/** Where root {@code i} hits the ground (x/z only; y is whatever the caller's ground is). */
	public static Vec3d rootTip(Vec3d base, int i, double s) {
		double a = rootAngle(i), reach = rootReach(i, s);
		return new Vec3d(base.x + Math.cos(a) * reach, base.y, base.z + Math.sin(a) * reach);
	}

	public static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}
}
