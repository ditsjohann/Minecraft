package com.armageddon.client.vfx;

/**
 * A force or behaviour applied to every particle of an emitter each tick, before the particles move. Modules run in
 * the order the emitter lists them. Velocities are in blocks per tick.
 */
@FunctionalInterface
public interface Module {
	void update(EmitterInstance em, ParticlePool p);

	/** Pulls down (positive {@code g}) or lifts (negative). Scale-free: a bigger blast falls at the same rate. */
	static Module gravity(float g) {
		return (em, p) -> {
			for (int i = 0; i < p.count; i++) {
				p.vy[i] -= g;
			}
		};
	}

	/** Air resistance: velocity loses {@code k} of itself per tick (exponential, so it never reverses). */
	static Module drag(float k) {
		float f = (float) Math.exp(-k);
		return (em, p) -> {
			for (int i = 0; i < p.count; i++) {
				p.vx[i] *= f;
				p.vy[i] *= f;
				p.vz[i] *= f;
			}
		};
	}

	/**
	 * Hot gas rises, and less as it cools: an upward push that fades over the particle's life. This is what makes
	 * fire climb and then hand over to smoke that spreads.
	 */
	static Module buoyancy(float force) {
		return (em, p) -> {
			for (int i = 0; i < p.count; i++) {
				float cool = 1 - p.age[i] / p.life[i];
				p.vy[i] += force * cool * cool * em.scaleSpeed;
			}
		};
	}

	/**
	 * Swirling, curling motion that never bunches particles up (it is divergence-free, as in Bridson's curl noise):
	 * the curl of a few travelling sine waves. {@code wavelength} in blocks at scale 1, {@code strength} in blocks per
	 * tick per tick. Each effect gets its own wave phases from its seed.
	 */
	static Module curl(float wavelength, float strength) {
		return new Curl(wavelength, strength);
	}

	/** Spin around the vertical axis through the emitter, and pull in (positive {@code pull}) or push out. */
	static Module vortex(float spin, float pull) {
		return (em, p) -> {
			float s = em.scaleSpeed;
			for (int i = 0; i < p.count; i++) {
				float dx = p.x[i] - em.ox, dz = p.z[i] - em.oz;
				float h = (float) Math.sqrt(dx * dx + dz * dz);
				if (h < 1.0E-3F) {
					continue;
				}
				dx /= h;
				dz /= h;
				p.vx[i] += (-dz * spin - dx * pull) * s;
				p.vz[i] += (dx * spin - dz * pull) * s;
			}
		};
	}

	/** Pull toward the emitter's centre; particles that arrive within {@code killRadius} (at scale 1) die. */
	static Module attract(float strength, float killRadius) {
		return (em, p) -> {
			float kr = killRadius * em.scale;
			float kr2 = kr * kr;
			for (int i = 0; i < p.count; i++) {
				float dx = em.ox - p.x[i], dy = em.oy - p.y[i], dz = em.oz - p.z[i];
				float d2 = dx * dx + dy * dy + dz * dz;
				if (d2 < kr2) {
					p.age[i] = p.life[i];
					continue;
				}
				float inv = strength * em.scaleSpeed / (float) Math.sqrt(d2);
				p.vx[i] += dx * inv;
				p.vy[i] += dy * inv;
				p.vz[i] += dz * inv;
			}
		};
	}

	/** Drift with the shared wind ({@link FxEngine#windX}, {@link FxEngine#windZ}), so all smoke leans the same way. */
	static Module wind(float strength) {
		return (em, p) -> {
			float wx = FxEngine.windX * strength, wz = FxEngine.windZ * strength;
			for (int i = 0; i < p.count; i++) {
				p.vx[i] += (wx - p.vx[i]) * 0.02F;
				p.vz[i] += (wz - p.vz[i]) * 0.02F;
			}
		};
	}

	/** Random jitter every tick: electricity, glitches, things that should look unstable. */
	static Module jitter(float amount) {
		return (em, p) -> {
			Rng r = em.rng;
			float a = amount * em.scaleSpeed;
			for (int i = 0; i < p.count; i++) {
				p.vx[i] += r.signed() * a;
				p.vy[i] += r.signed() * a;
				p.vz[i] += r.signed() * a;
			}
		};
	}

	final class Curl implements Module {
		private final float wavelength, strength;

		Curl(float wavelength, float strength) {
			this.wavelength = wavelength;
			this.strength = strength;
		}

		@Override
		public void update(EmitterInstance em, ParticlePool p) {
			float[] w = em.fx.curlWaves();
			float k = (float) (Math.PI * 2 / (wavelength * em.scale));
			float s = strength * em.scaleSpeed;
			float t = em.fx.age * 0.05F;
			for (int i = 0; i < p.count; i++) {
				float ax = 0, ay = 0, az = 0;
				for (int n = 0; n < w.length; n += 7) {
					// A plane wave along unit k̂ with amplitude vector a: curl(a sin(k·p + φ)) = cos(...) (k̂ × a).
					float ph = k * (w[n] * p.x[i] + w[n + 1] * p.y[i] + w[n + 2] * p.z[i]) + w[n + 6] + t;
					float c = (float) Math.cos(ph);
					ax += c * (w[n + 1] * w[n + 5] - w[n + 2] * w[n + 4]);
					ay += c * (w[n + 2] * w[n + 3] - w[n] * w[n + 5]);
					az += c * (w[n] * w[n + 4] - w[n + 1] * w[n + 3]);
				}
				p.vx[i] += ax * s;
				p.vy[i] += ay * s;
				p.vz[i] += az * s;
			}
		}
	}
}
