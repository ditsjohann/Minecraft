package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Lit, double-sided solid geometry (tubes, ellipsoids, rockets) for cinematic scenery. */
final class Solids {
	static final Vec3d LIGHT = new Vec3d(0.4, 0.8, 0.45).normalize();

	interface Cell {
		int color(int lat, int lon, Vec3d normal);
	}

	private Solids() {
	}

	static int scale(int rgb, double k) {
		int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * k);
		int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * k);
		int b = (int) Math.min(255, (rgb & 0xFF) * k);
		return (r << 16) | (g << 8) | b;
	}

	static double light(Vec3d n) {
		return 0.34 + 0.66 * Math.max(0.0, n.dotProduct(LIGHT));
	}

	static void quad(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb, float alpha) {
		FxDraw.vertex(vc, m, a.x, a.y, a.z, rgb, alpha);
		FxDraw.vertex(vc, m, b.x, b.y, b.z, rgb, alpha);
		FxDraw.vertex(vc, m, c.x, c.y, c.z, rgb, alpha);
		FxDraw.vertex(vc, m, d.x, d.y, d.z, rgb, alpha);
	}

	/** Tapered prism / cylinder / cone from {@code a} to {@code b}. */
	static void tube(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double ra, double rb, int n, int rgb, float alpha, boolean lit) {
		Vec3d axis = b.subtract(a);
		if (axis.lengthSquared() < 1.0E-8) {
			return;
		}
		Vec3d[] bs = ArsenalEffects.basis(axis);
		for (int i = 0; i < n; i++) {
			double a0 = MathHelper.TAU * i / n, a1 = MathHelper.TAU * (i + 1) / n;
			Vec3d n0 = bs[0].multiply(Math.cos(a0)).add(bs[1].multiply(Math.sin(a0)));
			Vec3d n1 = bs[0].multiply(Math.cos(a1)).add(bs[1].multiply(Math.sin(a1)));
			double k = lit ? light(n0.add(n1).normalize()) : 1.0;
			quad(vc, m, a.add(n0.multiply(ra)), a.add(n1.multiply(ra)), b.add(n1.multiply(rb)), b.add(n0.multiply(rb)), scale(rgb, k), alpha);
			if (rb > 0.01) {
				quad(vc, m, b, b, b.add(n0.multiply(rb)), b.add(n1.multiply(rb)), scale(rgb, lit ? 0.85 : 1.0), alpha);
			}
		}
	}

	static void ellipsoid(VertexConsumer vc, Matrix4f m, Vec3d c, double rx, double ry, double rz, int lat, int lon, Cell cell, boolean lit, float alpha) {
		for (int i = 0; i < lat; i++) {
			double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
			for (int j = 0; j < lon; j++) {
				double p0 = MathHelper.TAU * j / lon, p1 = MathHelper.TAU * (j + 1) / lon;
				Vec3d mid = unit((t0 + t1) / 2, (p0 + p1) / 2);
				int col = cell.color(i, j, mid);
				if (lit) {
					col = scale(col, light(mid));
				}
				Vec3d[] q = {unit(t0, p0), unit(t0, p1), unit(t1, p1), unit(t1, p0)};
				for (Vec3d n : q) {
					FxDraw.vertex(vc, m, c.x + n.x * rx, c.y + n.y * ry, c.z + n.z * rz, col, alpha);
				}
			}
		}
	}

	static void sphere(VertexConsumer vc, Matrix4f m, Vec3d c, double r, int rgb, int lat, int lon, boolean lit) {
		ellipsoid(vc, m, c, r, r, r, lat, lon, (i, j, n) -> rgb, lit, 1.0F);
	}

	static Vec3d unit(double theta, double phi) {
		double ct = Math.cos(theta);
		return new Vec3d(ct * Math.cos(phi), Math.sin(theta), ct * Math.sin(phi));
	}

	static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	/** Camera-facing glowing dot. */
	static void dot(VertexConsumer glow, Matrix4f m, Vec3d p, Vec3d[] cb, double r, int rgb, float alpha) {
		FxDraw.disc(glow, m, p, cb[0], cb[1], r, rgb, alpha, 0.0F, 8);
	}

	/** Star field painted on a shell around the camera (positions are camera-relative already). */
	static void stars(VertexConsumer glow, Matrix4f m, Vec3d[] cb, float time, float brightness) {
		for (int i = 0; i < 420; i++) {
			double a = hash(i, 1) * MathHelper.TAU;
			double y = hash(i, 2) * 2 - 1;
			double r = Math.sqrt(1 - y * y);
			Vec3d p = new Vec3d(Math.cos(a) * r, y, Math.sin(a) * r).multiply(300);
			double tw = 0.65 + 0.35 * Math.sin(time * 0.1 + i);
			double size = 0.35 + hash(i, 3) * 0.9;
			int col = hash(i, 4) > 0.8 ? 0xFFD8B0 : hash(i, 4) < 0.15 ? 0xB0D0FF : 0xFFFFFF;
			dot(glow, m, p, cb, size, col, (float) (tw * brightness));
		}
	}

	/** A four-point star flare: a long horizontal streak, a shorter vertical one and a hot core (camera-relative). */
	static void flare(VertexConsumer glow, Matrix4f m, Vec3d p, Vec3d[] cb, double size, int rgb, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}
		FxDraw.ribbon(glow, m, p.subtract(cb[0].multiply(size * 3.2)), p, Vec3d.ZERO, size * 0.07, rgb, 0.0F, alpha * 0.7F);
		FxDraw.ribbon(glow, m, p, p.add(cb[0].multiply(size * 3.2)), Vec3d.ZERO, size * 0.07, rgb, alpha * 0.7F, 0.0F);
		FxDraw.ribbon(glow, m, p.subtract(cb[1].multiply(size * 1.4)), p, Vec3d.ZERO, size * 0.06, rgb, 0.0F, alpha * 0.55F);
		FxDraw.ribbon(glow, m, p, p.add(cb[1].multiply(size * 1.4)), Vec3d.ZERO, size * 0.06, rgb, alpha * 0.55F, 0.0F);
		dot(glow, m, p, cb, size * 0.35, 0xFFFFFF, alpha);
		dot(glow, m, p, cb, size, rgb, alpha * 0.35F);
	}

	/** Soft coloured nebula clouds painted on the star shell, for space shots. */
	static void nebula(VertexConsumer glow, Matrix4f m, Vec3d[] cb, int salt, int tintA, int tintB, float brightness) {
		for (int i = 0; i < 18; i++) {
			double a = hash(i, salt) * 1.6 + salt * 0.7;
			double y = hash(i, salt + 1) * 0.9 - 0.2;
			double r = Math.sqrt(1 - y * y);
			Vec3d p = new Vec3d(Math.cos(a) * r, y, Math.sin(a) * r).multiply(290);
			double size = 40 + hash(i, salt + 2) * 70;
			int col = hash(i, salt + 3) < 0.5 ? tintA : tintB;
			FxDraw.disc(glow, m, p, cb[0], cb[1], size, col, 0.10F * brightness, 0.0F, 14);
			FxDraw.disc(glow, m, p, cb[0], cb[1], size * 0.4, scale(col, 1.3), 0.08F * brightness, 0.0F, 10);
		}
	}

	/** A distant sun in direction {@code dir}: corona, rays and a flare (camera-relative). */
	static void sun(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d dir, double size, int rgb, float time) {
		Vec3d p = dir.normalize().multiply(285);
		dot(glow, m, p, cb, size * 3.0, rgb, 0.18F);
		dot(glow, m, p, cb, size * 1.4, rgb, 0.5F);
		dot(glow, m, p, cb, size * 0.6, 0xFFFFFF, 1.0F);
		for (int k = 0; k < 12; k++) {
			double ang = k * MathHelper.TAU / 12 + time * 0.002;
			double len = size * (2.2 + 1.4 * hash(k, 9));
			Vec3d d = cb[0].multiply(Math.cos(ang)).add(cb[1].multiply(Math.sin(ang)));
			FxDraw.ribbon(glow, m, p, p.add(d.multiply(len)), Vec3d.ZERO, size * 0.12, rgb, 0.35F, 0.0F);
		}
		flare(glow, m, p, cb, size * 2.0, rgb, 0.6F);
	}

	/** Banded, swirled gas-giant colouring (alien worlds). */
	static Cell bands(int a, int b, int c, long seed) {
		return (i, j, n) -> {
			double v = Math.sin(n.y * 9 + Math.sin(n.x * 3 + n.z * 2 + seed) * 1.2) * 0.5 + 0.5;
			double w = hash(i / 2, j / 2 + (int) seed);
			int base = v < 0.33 ? a : v < 0.66 ? b : c;
			return scale(base, 0.82 + 0.3 * w);
		};
	}

	/** Earth-ish colouring: oceans, continents, ice caps. */
	static Cell earth() {
		return (i, j, n) -> {
			double land = Math.sin(n.x * 4.1 + 1.2) * Math.cos(n.y * 3.3 + n.z * 2.2) + Math.sin(n.z * 5.7 - n.x * 2.0) * 0.55;
			if (Math.abs(n.y) > 0.88) {
				return 0xF2F6FF;
			}
			return land > 0.35 ? scale(0x3E9A44, 0.85 + 0.3 * hash(i, j)) : scale(0x1E55B8, 0.9 + 0.2 * hash(j, i));
		};
	}

	/** True where {@link #earth()} paints land (not ocean or ice), for body-frame normal {@code n}. */
	static boolean isLand(Vec3d n) {
		double land = Math.sin(n.x * 4.1 + 1.2) * Math.cos(n.y * 3.3 + n.z * 2.2) + Math.sin(n.z * 5.7 - n.x * 2.0) * 0.55;
		return land > 0.35 && Math.abs(n.y) <= 0.88;
	}

	/** Linear blend of two RGB colours. */
	static int mix(int a, int b, double t) {
		t = MathHelper.clamp(t, 0.0, 1.0);
		int r = (int) MathHelper.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	/** Rodrigues rotation of {@code v} about unit {@code axis}. */
	static Vec3d rotate(Vec3d v, Vec3d axis, double ang) {
		double c = Math.cos(ang), s = Math.sin(ang);
		return v.multiply(c).add(axis.crossProduct(v).multiply(s)).add(axis.multiply(axis.dotProduct(v) * (1 - c)));
	}

	/**
	 * A moving night sky for cosmic skin: deep blue-black with violet, teal and magenta nebula drifting through it
	 * and a scatter of twinkling star cells.
	 */
	static Cell nightSky(float time, int salt) {
		return (i, j, n) -> {
			double drift = time * 0.012;
			double a = Math.sin(n.x * 3.1 + n.y * 2.3 + drift + salt) * Math.cos(n.z * 2.7 - n.y * 1.9 - drift * 0.7);
			double b = Math.sin(n.y * 4.3 - n.z * 3.3 + drift * 1.3 + salt * 2) * Math.cos(n.x * 2.2 + drift * 0.5);
			int col = mix(0x0A0926, 0x4A2496, a * 0.9);
			col = mix(col, 0x147A90, b * 0.7);
			col = mix(col, 0x9A2E86, (a * b - 0.25) * 1.4);
			double star = hash(i * 7 + salt, j * 13 + salt);
			if (star > 0.93) {
				col = mix(col, star > 0.985 ? 0xFFE6B0 : 0xDCE4FF, 0.55 + 0.45 * Math.sin(time * 0.21 + i * 3.1 + j * 1.7));
			}
			return col;
		};
	}

	/** {@link #tube} split into {@code rows} x {@code n} cells, each coloured by {@code cell(row, column, normal)}. */
	static void tube(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double ra, double rb, int n, int rows, Cell cell, boolean lit, float alpha) {
		Vec3d axis = b.subtract(a);
		if (axis.lengthSquared() < 1.0E-8) {
			return;
		}
		Vec3d[] bs = ArsenalEffects.basis(axis);
		for (int r = 0; r < rows; r++) {
			double u0 = (double) r / rows, u1 = (double) (r + 1) / rows;
			Vec3d c0 = a.add(axis.multiply(u0)), c1 = a.add(axis.multiply(u1));
			double r0 = MathHelper.lerp(u0, ra, rb), r1 = MathHelper.lerp(u1, ra, rb);
			for (int i = 0; i < n; i++) {
				double a0 = MathHelper.TAU * i / n, a1 = MathHelper.TAU * (i + 1) / n;
				Vec3d n0 = bs[0].multiply(Math.cos(a0)).add(bs[1].multiply(Math.sin(a0)));
				Vec3d n1 = bs[0].multiply(Math.cos(a1)).add(bs[1].multiply(Math.sin(a1)));
				Vec3d mid = n0.add(n1).normalize();
				int col = cell.color(r, i, mid);
				if (lit) {
					col = scale(col, light(mid));
				}
				quad(vc, m, c0.add(n0.multiply(r0)), c0.add(n1.multiply(r0)), c1.add(n1.multiply(r1)), c1.add(n0.multiply(r1)), col, alpha);
			}
		}
	}

	/**
	 * A rune glyph of three to four strokes on a 3x3 grid in the plane {@code u}/{@code v} around {@code c}. Each salt
	 * gives a different, stable glyph.
	 */
	static void glyph(VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d u, Vec3d v, double size, int salt, int rgb, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}
		int strokes = 3 + (int) (hash(salt, 5) * 2);
		for (int k = 0; k < strokes; k++) {
			int p0 = (int) (hash(salt, 11 + k) * 9), p1 = (int) (hash(salt, 23 + k) * 9);
			if (p0 == p1) {
				p1 = (p0 + 4) % 9;
			}
			Vec3d a = c.add(u.multiply((p0 % 3 - 1) * size * 0.5)).add(v.multiply((p0 / 3 - 1) * size * 0.5));
			Vec3d b = c.add(u.multiply((p1 % 3 - 1) * size * 0.5)).add(v.multiply((p1 / 3 - 1) * size * 0.5));
			FxDraw.ribbon(glow, m, a, b, Vec3d.ZERO, size * 0.14, rgb, alpha, alpha);
		}
	}

	static void rocket(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d dir, double len, double r, float time, double flame) {
		rocket(solid, glow, m, c, dir, len, r, time, flame, 0.0);
	}

	/**
	 * Alien planet-killer along {@code dir} (unit), centred on {@code c}: plated hull, hazard-striped collars, rune
	 * columns, an energy nose tip and a hot nozzle bell. {@code hatch} 0..1 swings the nose open in four petals.
	 */
	static void rocket(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d dir, double len, double r, float time, double flame, double hatch) {
		Vec3d tail = c.subtract(dir.multiply(len / 2)), nose = c.add(dir.multiply(len / 2));
		Vec3d[] bs = ArsenalEffects.basis(dir);
		double bodyU = 0.72;
		Vec3d bodyEnd = tail.add(dir.multiply(len * bodyU));
		// Plated hull: stacked sections of offset panels in two tones, dark seams between the sections.
		int sections = 7, panels = 10;
		for (int s = 0; s < sections; s++) {
			double ua = MathHelper.lerp((double) s / sections, 0.08, bodyU), ub = MathHelper.lerp((double) (s + 1) / sections, 0.08, bodyU);
			Vec3d pa = tail.add(dir.multiply(len * ua)), pb = tail.add(dir.multiply(len * (ub - 0.006)));
			int salt = s;
			tube(solid, m, pa, pb, r, r * 0.995, panels, 1, (i, j, n) -> {
				int tone = (j + salt) % 2 == 0 ? 0x30204F : 0x261841;
				return scale(tone, 0.9 + 0.2 * hash(j, salt + 40));
			}, true, 1.0F);
			Vec3d sa = pb, sb = tail.add(dir.multiply(len * ub));
			tube(solid, m, sa, sb, r * 1.025, r * 1.025, 16, 0x100A1C, 1.0F, true);
		}
		// Hazard-striped collars above the engine and below the nose.
		for (double[] band : new double[][]{{0.085, 0.15}, {0.655, 0.705}}) {
			Vec3d pa = tail.add(dir.multiply(len * band[0])), pb = tail.add(dir.multiply(len * band[1]));
			tube(solid, m, pa, pb, r * 1.035, r * 1.035, 24, 3, (i, j, n) -> (j + i) % 4 < 2 ? 0xE8B020 : 0x161218, true, 1.0F);
		}
		// Nose cone, or four petals swinging open around the payload bay.
		Vec3d tipGlow = nose;
		if (hatch <= 0.001) {
			tube(solid, m, bodyEnd, nose, r * 0.96, 0.0, 16, 4, (i, j, n) -> scale(0x3B2266, 0.9 + 0.12 * ((i + j) % 2)), true, 1.0F);
		} else {
			Solids.tube(solid, m, bodyEnd.subtract(dir.multiply(r * 0.6)), bodyEnd, r * 0.9, r * 0.9, 12, 0x08040E, 1.0F, false);
			FxDraw.disc(glow, m, bodyEnd.subtract(dir.multiply(r * 0.5)), bs[0], bs[1], r * 0.85, 0x60FFE0, (float) (0.6 * hatch), 0.0F, 16);
			double coneLen = len * (1 - bodyU);
			double open = hatch * 1.75;
			for (int q = 0; q < 4; q++) {
				double qa0 = q * Math.PI / 2, qa1 = (q + 1) * Math.PI / 2, qm = (qa0 + qa1) / 2;
				Vec3d out = bs[0].multiply(Math.cos(qm)).add(bs[1].multiply(Math.sin(qm)));
				Vec3d tangent = dir.crossProduct(out).normalize();
				Vec3d hinge = bodyEnd.add(out.multiply(r * 0.96));
				Vec3d tip = hinge.add(rotate(dir.multiply(coneLen).subtract(out.multiply(r * 0.96)), tangent, open));
				int steps = 4;
				for (int k = 0; k < steps; k++) {
					double b0 = MathHelper.lerp((double) k / steps, qa0, qa1), b1 = MathHelper.lerp((double) (k + 1) / steps, qa0, qa1);
					Vec3d e0 = bodyEnd.add(bs[0].multiply(Math.cos(b0) * r * 0.96)).add(bs[1].multiply(Math.sin(b0) * r * 0.96));
					Vec3d e1 = bodyEnd.add(bs[0].multiply(Math.cos(b1) * r * 0.96)).add(bs[1].multiply(Math.sin(b1) * r * 0.96));
					e0 = hinge.add(rotate(e0.subtract(hinge), tangent, open));
					e1 = hinge.add(rotate(e1.subtract(hinge), tangent, open));
					Vec3d nn = rotate(out, tangent, open);
					quad(solid, m, e0, e1, tip, tip, scale(0x3B2266, light(nn)), 1.0F);
				}
				FxDraw.ribbon(glow, m, hinge, tip, Vec3d.ZERO, r * 0.06, 0x60FFE0, 0.7F, 0.3F);
			}
			tipGlow = null;
		}
		// Energy nose tip: a pulsing core with a ring orbiting it.
		float pulse = 0.75F + 0.25F * MathHelper.sin(time * 0.3F);
		if (tipGlow != null) {
			FxDraw.sphere(glow, m, tipGlow, r * 0.32, Vec3d.ZERO, 0xFFFFFF, 0.9F * pulse, 0.5F, 6, 10);
			FxDraw.sphere(glow, m, tipGlow, r * 0.8, Vec3d.ZERO, 0x60FFE0, 0.35F * pulse, 0.0F, 6, 10);
			double ra = time * 0.12;
			Vec3d ru = bs[0].multiply(Math.cos(ra)).add(bs[1].multiply(Math.sin(ra)));
			FxDraw.ring(glow, m, tipGlow.subtract(dir.multiply(r * 0.5)), ru, dir.crossProduct(ru), r * 0.7, r * 0.85, 0xC070FF, 0.0F, 0.8F * pulse, 0.0F, 24);
		}
		// Rune columns up the hull, lit by a wave that climbs toward the nose.
		for (int col = 0; col < 4; col++) {
			double ang = col * Math.PI / 2 + Math.PI / 4;
			Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
			Vec3d side = dir.crossProduct(out).normalize();
			for (int k = 0; k < 6; k++) {
				double u = 0.2 + k * 0.075;
				float wave = 0.35F + 0.65F * (float) Math.max(0.0, Math.sin(time * 0.15 - k * 0.9 + col));
				Vec3d p = tail.add(dir.multiply(len * u)).add(out.multiply(r * 1.03));
				glyph(glow, m, p, side, dir, r * 0.55, col * 17 + k, (col + k) % 2 == 0 ? 0x40FFB0 : 0xC070FF, 0.9F * wave);
			}
		}
		// Curved fins with glowing edges.
		for (int f = 0; f < 4; f++) {
			double ang = f * Math.PI / 2;
			Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
			Vec3d root = tail.add(dir.multiply(len * 0.30)).add(out.multiply(r * 0.9));
			Vec3d mid = tail.add(dir.multiply(len * 0.14)).add(out.multiply(r * 2.4));
			Vec3d tip = tail.add(dir.multiply(-len * 0.02)).add(out.multiply(r * 3.0));
			tube(solid, m, root, mid, r * 0.28, r * 0.2, 4, 0x1E1038, 1.0F, true);
			tube(solid, m, mid, tip, r * 0.2, r * 0.05, 4, 0x1E1038, 1.0F, true);
			tube(solid, m, tip, tip.add(out.multiply(r * 0.1)).subtract(dir.multiply(len * 0.03)), r * 0.08, r * 0.02, 4, 0xE8B020, 1.0F, true);
			tube(glow, m, mid, tip, r * 0.07, r * 0.03, 4, 0x40FFB0, 0.9F, false);
		}
		// Hot nozzle bell: dark outside, glowing from orange to white inside as the engine runs.
		Vec3d bellTop = tail.add(dir.multiply(len * 0.08)), bellEnd = tail.subtract(dir.multiply(len * 0.02));
		tube(solid, m, bellTop, bellEnd, r * 0.62, r * 1.12, 16, 2, (i, j, n) -> mix(0x1A141E, 0x6A2A14, i * 0.5 + flame * 0.5), true, 1.0F);
		float heat = (float) Math.min(1.0, 0.25 + flame);
		int hot = mix(0xFF6020, 0xFFF0D0, flame);
		tube(glow, m, bellTop, bellEnd, r * 0.55, r * 1.04, 16, hot, 0.45F * heat, false);
		FxDraw.ring(glow, m, bellEnd, bs[0], bs[1], r * 0.95, r * 1.14, hot, 0.0F, 0.9F * heat, 0.0F, 32);
		// Exhaust.
		if (flame > 0.01) {
			float flick = 0.9F + 0.1F * MathHelper.sin(time * 2.3F);
			Vec3d end = bellEnd.subtract(dir.multiply(len * 0.9 * flame));
			tube(glow, m, bellEnd, end, r * 0.95, 0.0, 12, 0x60FFE0, 0.55F * flick, false);
			tube(glow, m, bellEnd, bellEnd.subtract(dir.multiply(len * 0.55 * flame)), r * 0.55, 0.0, 12, 0xFFFFFF, 0.85F, false);
			tube(glow, m, bellEnd, bellEnd.subtract(dir.multiply(len * 1.6 * flame)), r * 1.5, 0.0, 12, 0xA060FF, 0.18F * flick, false);
		}
	}
}
