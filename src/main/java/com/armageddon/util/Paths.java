package com.armageddon.util;

import net.minecraft.util.math.Vec3d;

/** Deterministic motion paths shared by server logic and client visuals so both agree frame for frame. */
public final class Paths {
	private Paths() {
	}

	/** Twister centre {@code t} ticks after it forms: steady drift with a lazy sideways wander. */
	public static Vec3d twister(Vec3d start, Vec3d dir, double speed, double t) {
		Vec3d side = new Vec3d(-dir.z, 0, dir.x);
		return start.add(dir.multiply(speed * t)).add(side.multiply(Math.sin(t * 0.03) * 26.0));
	}

	/** UFO centre {@code t} ticks into a {@code life}-tick fly-over; it bobs and eases in and out of the path. */
	public static Vec3d ufo(Vec3d from, Vec3d to, double t, double life) {
		double u = Math.max(0, Math.min(1, t / life));
		double e = u * u * (3 - 2 * u) * 0.35 + u * 0.65;
		Vec3d p = from.lerp(to, e);
		return p.add(0, Math.sin(t * 0.07) * 4.0, 0);
	}

	/** Where the UFO's death beam touches down: it sweeps side to side as the ship passes. */
	public static Vec3d ufoBeam(Vec3d shipXZ, Vec3d dir, double t) {
		Vec3d side = new Vec3d(-dir.z, 0, dir.x);
		return shipXZ.add(side.multiply(Math.sin(t * 0.09) * 46.0)).add(dir.multiply(Math.cos(t * 0.05) * 30.0));
	}
}
