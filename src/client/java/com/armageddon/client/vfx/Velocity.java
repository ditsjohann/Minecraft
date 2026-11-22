package com.armageddon.client.vfx;

/**
 * A particle's starting velocity, in blocks per tick at scale 1 (speeds grow with the effect's scale, so a bigger
 * blast throws things further in the same time). An emitter can stack several; they add up.
 */
@FunctionalInterface
public interface Velocity {
	/**
	 * @param shape the shape sample: position in [0..2], outward direction in [3..5]
	 * @param dir   the effect's direction (unit), e.g. where a gun points or a surface normal
	 * @param out   accumulates into [0..2]
	 */
	void add(Rng r, float[] shape, float[] dir, float scale, float[] out);

	/** Outward from the shape. */
	static Velocity radial(float min, float max) {
		return (r, sh, d, s, o) -> {
			float v = r.range(min, max) * s;
			o[0] += sh[3] * v;
			o[1] += sh[4] * v;
			o[2] += sh[5] * v;
		};
	}

	/** In a cone of {@code spread} radians around straight up. */
	static Velocity up(float min, float max, float spread) {
		return (r, sh, d, s, o) -> cone(r, 0, 1, 0, spread, r.range(min, max) * s, o);
	}

	/** In a cone of {@code spread} radians around the effect's direction. */
	static Velocity along(float min, float max, float spread) {
		return (r, sh, d, s, o) -> cone(r, d[0], d[1], d[2], spread, r.range(min, max) * s, o);
	}

	/** A random direction, any way. */
	static Velocity random(float min, float max) {
		float[] u = new float[3];
		return (r, sh, d, s, o) -> {
			r.unit(u, 0);
			float v = r.range(min, max) * s;
			o[0] += u[0] * v;
			o[1] += u[1] * v;
			o[2] += u[2] * v;
		};
	}

	/** Around the vertical axis (counter-clockwise seen from above): swirls, vortex rings. */
	static Velocity swirl(float min, float max) {
		return (r, sh, d, s, o) -> {
			float h = (float) Math.sqrt(sh[0] * sh[0] + sh[2] * sh[2]);
			if (h < 1.0E-4F) {
				return;
			}
			float v = r.range(min, max) * s;
			o[0] += -sh[2] / h * v;
			o[2] += sh[0] / h * v;
		};
	}

	private static void cone(Rng r, float ax, float ay, float az, float spread, float speed, float[] o) {
		// A tangent basis around the axis, then a direction within the cone (uniform over the cap).
		float tx, ty, tz;
		if (Math.abs(ay) < 0.9F) {
			tx = az;
			ty = 0;
			tz = -ax;
		} else {
			tx = 0;
			ty = -az;
			tz = ay;
		}
		float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
		tx /= tl;
		ty /= tl;
		tz /= tl;
		float bx = ay * tz - az * ty, by = az * tx - ax * tz, bz = ax * ty - ay * tx;
		float cosMax = (float) Math.cos(spread);
		float c = 1 - r.next() * (1 - cosMax);
		float sn = (float) Math.sqrt(Math.max(0, 1 - c * c));
		double a = r.next() * Math.PI * 2;
		float ca = (float) Math.cos(a) * sn, sa = (float) Math.sin(a) * sn;
		o[0] += (ax * c + tx * ca + bx * sa) * speed;
		o[1] += (ay * c + ty * ca + by * sa) * speed;
		o[2] += (az * c + tz * ca + bz * sa) * speed;
	}
}
