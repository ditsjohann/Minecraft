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

	/**
	 * A tornado funnel standing on the emitter: particles are held to a wall whose radius grows from {@code r0} at the
	 * ground to {@code r1} at {@code height} (shaped by {@code power}), and driven round it at {@code spin} blocks a
	 * tick, faster toward the top. They carry most of the funnel's own motion, so the upper parts lag and it leans
	 * into its track.
	 */
	static Module funnel(float height, float r0, float r1, float power, float spin, float pull) {
		return (em, p) -> {
			float s = em.scale, top = height * s, cvx = em.fx.cvx * 0.85F, cvz = em.fx.cvz * 0.85F;
			for (int i = 0; i < p.count; i++) {
				float dx = p.x[i] - em.ox, dz = p.z[i] - em.oz;
				float d = (float) Math.sqrt(dx * dx + dz * dz);
				if (d < 1.0E-3F) {
					dx = 1.0E-3F;
					d = 1.0E-3F;
				}
				float h = Math.max(0, Math.min(1, (p.y[i] - em.oy) / top));
				float wall = (r0 + (r1 - r0) * (float) Math.pow(h, power)) * s;
				float ux = dx / d, uz = dz / d;
				float tan = spin * em.scaleSpeed * (0.6F + 0.4F * h), rad = (wall - d) * pull;
				float tx = -uz * tan + ux * rad + cvx, tz = ux * tan + uz * rad + cvz;
				p.vx[i] += (tx - p.vx[i]) * 0.2F;
				p.vz[i] += (tz - p.vz[i]) * 0.2F;
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

	/**
	 * A twisted flux rope: every particle rides an arch from one foot to the other over its life (half go each way),
	 * winding round the arch's centre line as it goes. The arch stands along the effect's direction, {@code span}
	 * blocks to each side of the emitter and {@code height} high (growing by {@code rise} a tick); its strands sit up to
	 * {@code tube} from the centre line and wind {@code twist} times from foot to foot.
	 */
	static Module arch(float span, float height, float rise, float tube, float twist) {
		return (em, p) -> {
			float[] d = em.fx.dir;
			float hx = d[0], hz = d[2], hl = (float) Math.sqrt(hx * hx + hz * hz);
			if (hl < 1.0E-3F) {
				hx = 1;
				hz = 0;
			} else {
				hx /= hl;
				hz /= hl;
			}
			float sx = -hz, sz = hx;
			float S = span * em.scale, H = (height + rise * em.fx.age) * em.scale, T = tube * em.scale;
			float spin = em.fx.age * 0.06F;
			for (int i = 0; i < p.count; i++) {
				float u = Math.min(1, (p.age[i] + 1) / p.life[i]);
				float sd = p.seed[i];
				if (sd < 0.5F) {
					u = 1 - u;
				}
				float th = (float) Math.PI * u;
				float c = (float) Math.cos(th), s = (float) Math.sin(th);
				float cx = em.ox - hx * S * c, cy = em.oy + H * s, cz = em.oz - hz * S * c;
				// Outward from the arch's middle, in its plane.
				float nx = -c * hx, ny = s, nz = -c * hz;
				float frac = (sd * 37.31F) % 1, ph = (sd * 91.7F) % 1 * 6.2832F + twist * th * 2 + spin;
				float r = T * (0.25F + 0.75F * frac) * (0.6F + 0.4F * s);
				float co = (float) Math.cos(ph) * r, si = (float) Math.sin(ph) * r;
				float tx = cx + nx * co + sx * si, ty = cy + ny * co, tz = cz + nz * co + sz * si;
				p.vx[i] = tx - p.x[i];
				p.vy[i] = ty - p.y[i];
				p.vz[i] = tz - p.z[i];
			}
		};
	}

	/**
	 * A magnetic bottle: particles stream along the field lines of an upright dipole at the emitter, looping out of one
	 * pole and back into the other, at {@code speed} blocks a tick (half each way). Anything straying past
	 * {@code radius} is reined back in, so the lines stay a cage and never run off to the sky.
	 */
	static Module dipole(float radius, float speed) {
		return (em, p) -> {
			float R = radius * em.scale, v = speed * em.scaleSpeed;
			for (int i = 0; i < p.count; i++) {
				float rx = p.x[i] - em.ox, ry = p.y[i] - em.oy, rz = p.z[i] - em.oz;
				float r = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
				if (r < 0.3F) {
					continue;
				}
				float m = ry / r;
				// B ~ 3 (m.r^) r^ - m, with m straight up.
				float bx = 3 * m * rx / r, by = 3 * m * ry / r - 1, bz = 3 * m * rz / r;
				float bl = (float) Math.sqrt(bx * bx + by * by + bz * bz);
				if (bl < 1.0E-4F) {
					continue;
				}
				float sgn = p.seed[i] < 0.5F ? v : -v;
				float tx = bx / bl * sgn, ty = by / bl * sgn, tz = bz / bl * sgn;
				if (r > R) {
					float pull = (r - R) / r * 0.3F;
					tx -= rx * pull;
					ty -= ry * pull;
					tz -= rz * pull;
				}
				p.vx[i] += (tx - p.vx[i]) * 0.35F;
				p.vy[i] += (ty - p.vy[i]) * 0.35F;
				p.vz[i] += (tz - p.vz[i]) * 0.35F;
			}
		};
	}

	/**
	 * A Z-pinch: particles are held to a column standing on the emitter whose radius is crushed from {@code radius}
	 * toward a third of it over {@code squeeze} ticks, and as it narrows it breaks into beads (the sausage
	 * instability) every {@code wavelength} blocks, while the plasma spirals round it at {@code spin}.
	 */
	static Module pinch(float radius, float wavelength, int squeeze, float spin) {
		return (em, p) -> {
			float k = Math.min(1, em.fx.age / (float) Math.max(1, squeeze));
			float R = radius * em.scale * (1 - 0.66F * k);
			float w = (float) (Math.PI * 2 / (wavelength * em.scale));
			float sp = spin * em.scaleSpeed, ph = em.fx.age * 0.12F;
			for (int i = 0; i < p.count; i++) {
				float dx = p.x[i] - em.ox, dz = p.z[i] - em.oz, y = p.y[i] - em.oy;
				float h = (float) Math.sqrt(dx * dx + dz * dz);
				if (h < 1.0E-3F) {
					dx = 1.0E-3F;
					h = 1.0E-3F;
				}
				float target = R * (1 + 0.75F * k * (float) Math.sin(y * w + ph));
				float ux = dx / h, uz = dz / h, vr = (target - h) * 0.18F;
				float tx = ux * vr - uz * sp, tz = uz * vr + ux * sp;
				p.vx[i] += (tx - p.vx[i]) * 0.3F;
				p.vz[i] += (tz - p.vz[i]) * 0.3F;
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
