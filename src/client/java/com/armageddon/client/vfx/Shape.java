package com.armageddon.client.vfx;

/**
 * Where an emitter puts new particles, relative to the emitter. Writes the position into {@code out[0..2]} and an
 * "outward" unit direction into {@code out[3..5]}, which radial velocities push along. Sizes are in blocks at
 * scale 1 and grow with the effect's scale.
 */
@FunctionalInterface
public interface Shape {
	void sample(Rng r, float scale, float[] out);

	Shape POINT = (r, s, o) -> {
		o[0] = o[1] = o[2] = 0;
		r.unit(o, 3);
	};

	/** Inside a ball (uniform in volume). */
	static Shape sphere(float radius) {
		return (r, s, o) -> {
			r.unit(o, 3);
			float d = radius * s * (float) Math.cbrt(r.next());
			o[0] = o[3] * d;
			o[1] = o[4] * d;
			o[2] = o[5] * d;
		};
	}

	/** On a sphere's surface. */
	static Shape shell(float radius) {
		return (r, s, o) -> {
			r.unit(o, 3);
			o[0] = o[3] * radius * s;
			o[1] = o[4] * radius * s;
			o[2] = o[5] * radius * s;
		};
	}

	/** The upper half of a ball: blasts sitting on the ground. */
	static Shape dome(float radius) {
		return (r, s, o) -> {
			r.unit(o, 3);
			o[4] = Math.abs(o[4]);
			float d = radius * s * (float) Math.cbrt(r.next());
			o[0] = o[3] * d;
			o[1] = o[4] * d;
			o[2] = o[5] * d;
		};
	}

	/** A flat ring on the ground plane, {@code thickness} wide; outward is horizontal. */
	static Shape ring(float radius, float thickness) {
		return (r, s, o) -> {
			double a = r.next() * Math.PI * 2;
			float d = (radius + (r.next() - 0.5F) * thickness) * s;
			o[3] = (float) Math.cos(a);
			o[4] = 0;
			o[5] = (float) Math.sin(a);
			o[0] = o[3] * d;
			o[1] = 0;
			o[2] = o[5] * d;
		};
	}

	/**
	 * A doughnut lying flat: points in a tube of radius {@code tube} around a horizontal ring of radius {@code radius}.
	 * Outward is away from the ring's axis, so radial velocities widen it. Mushroom caps are born like this.
	 */
	static Shape torus(float radius, float tube) {
		return (r, s, o) -> {
			double a = r.next() * Math.PI * 2, b = r.next() * Math.PI * 2;
			float t = tube * (float) Math.sqrt(r.next());
			float ring = (radius + t * (float) Math.cos(b)) * s;
			o[3] = (float) Math.cos(a);
			o[4] = 0;
			o[5] = (float) Math.sin(a);
			o[0] = o[3] * ring;
			o[1] = t * (float) Math.sin(b) * s;
			o[2] = o[5] * ring;
		};
	}

	/** A filled disc on the ground plane (uniform in area). */
	static Shape disc(float radius) {
		return (r, s, o) -> {
			double a = r.next() * Math.PI * 2;
			float d = radius * s * (float) Math.sqrt(r.next());
			o[3] = (float) Math.cos(a);
			o[4] = 0;
			o[5] = (float) Math.sin(a);
			o[0] = o[3] * d;
			o[1] = 0;
			o[2] = o[5] * d;
		};
	}

	/** A vertical cylinder from the emitter up to {@code height}: smoke stems, light pillars. */
	static Shape column(float radius, float height) {
		return (r, s, o) -> {
			double a = r.next() * Math.PI * 2;
			float d = radius * s * (float) Math.sqrt(r.next());
			o[3] = (float) Math.cos(a);
			o[4] = 0;
			o[5] = (float) Math.sin(a);
			o[0] = o[3] * d;
			o[1] = r.next() * height * s;
			o[2] = o[5] * d;
		};
	}

	/** A box of half-extents {@code hx, hy, hz}. */
	static Shape box(float hx, float hy, float hz) {
		return (r, s, o) -> {
			o[0] = r.signed() * hx * s;
			o[1] = r.signed() * hy * s;
			o[2] = r.signed() * hz * s;
			r.unit(o, 3);
		};
	}
}
