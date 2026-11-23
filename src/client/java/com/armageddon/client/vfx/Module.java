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

	/**
	 * Gravity that is off until the effect is {@code atTick} old and then pulls with {@code g}: things drifting
	 * weightless that are all slammed down at the same moment.
	 */
	static Module gravityAt(int atTick, float g) {
		return (em, p) -> {
			if (em.fx.age < atTick) {
				return;
			}
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

	/**
	 * A rolling vortex ring, as in a mushroom cap: every particle is eased toward the group's mean motion plus a
	 * circulation around the ring the group itself forms (its mean height and mean distance from the axis), so smoke
	 * climbs through the middle, rolls outward over the top and tucks back under the rim. The ring follows the group,
	 * so it rises and slows with it. {@code speed}: circulation in blocks per tick at scale 1; {@code core}: radius of
	 * the rolling tube; {@code grow}: how fast the ring widens.
	 */
	static Module roll(float speed, float core, float grow) {
		return (em, p) -> {
			int n = p.count;
			if (n < 4) {
				return;
			}
			float sr = 0, sy = 0, svx = 0, svy = 0, svz = 0;
			for (int i = 0; i < n; i++) {
				float dx = p.x[i] - em.ox, dz = p.z[i] - em.oz;
				sr += (float) Math.sqrt(dx * dx + dz * dz);
				sy += p.y[i];
				svx += p.vx[i];
				svy += p.vy[i];
				svz += p.vz[i];
			}
			float ringR = sr / n, cy = sy / n, mvx = svx / n, mvy = svy / n, mvz = svz / n;
			float a = core * em.scale, a2 = a * a;
			float s = speed * em.scaleSpeed, g = grow * em.scaleSpeed;
			for (int i = 0; i < n; i++) {
				float dx = p.x[i] - em.ox, dz = p.z[i] - em.oz;
				float h = (float) Math.sqrt(dx * dx + dz * dz);
				if (h < 1.0E-3F) {
					continue;
				}
				float ux = dx / h, uz = dz / h;
				float dr = h - ringR, dy = p.y[i] - cy;
				// Strongest one tube-radius from the core, gentle at the centre and far outside.
				float f = 2 * s / (a * (1 + (dr * dr + dy * dy) / a2));
				float out = dy * f + g, up = -dr * f;
				// Smoke that wanders off the tube is drawn back onto it, so the cap stays a ring and not a ball.
				float d = (float) Math.sqrt(dr * dr + dy * dy);
				if (d > a) {
					float pull = (d - a) / d * 0.04F;
					out -= dr * pull;
					up -= dy * pull;
				}
				p.vx[i] += (mvx + ux * out - p.vx[i]) * 0.08F;
				p.vy[i] += (mvy + up - p.vy[i]) * 0.08F;
				p.vz[i] += (mvz + uz * out - p.vz[i]) * 0.08F;
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
