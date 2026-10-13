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

	/** Alien stacked-hull rocket along {@code dir} (unit), centred on {@code c}. */
	static void rocket(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d dir, double len, double r, float time, double flame) {
		Vec3d tail = c.subtract(dir.multiply(len / 2)), nose = c.add(dir.multiply(len / 2));
		Vec3d[] bs = ArsenalEffects.basis(dir);
		Vec3d bodyEnd = tail.add(dir.multiply(len * 0.72));
		tube(solid, m, tail.add(dir.multiply(len * 0.06)), bodyEnd, r, r * 0.96, 12, 0x2A1646, 1.0F, true);
		tube(solid, m, bodyEnd, nose, r * 0.96, 0.0, 12, 0x3B1F66, 1.0F, true);
		tube(solid, m, tail, tail.add(dir.multiply(len * 0.08)), r * 0.7, r * 1.15, 12, 0x14101C, 1.0F, true);
		// Glowing armour bands.
		for (int k = 1; k <= 4; k++) {
			Vec3d p = tail.add(dir.multiply(len * (0.12 + 0.15 * k)));
			tube(glow, m, p, p.add(dir.multiply(len * 0.018)), r * 1.04, r * 1.04, 12, 0x9040FF, 0.85F, false);
		}
		// Curved fins.
		for (int f = 0; f < 4; f++) {
			double ang = f * Math.PI / 2 + Math.PI / 4;
			Vec3d out = bs[0].multiply(Math.cos(ang)).add(bs[1].multiply(Math.sin(ang)));
			Vec3d root = tail.add(dir.multiply(len * 0.30)).add(out.multiply(r * 0.9));
			Vec3d mid = tail.add(dir.multiply(len * 0.14)).add(out.multiply(r * 2.4));
			Vec3d tip = tail.add(dir.multiply(-len * 0.02)).add(out.multiply(r * 3.0));
			tube(solid, m, root, mid, r * 0.28, r * 0.2, 4, 0x1E1038, 1.0F, true);
			tube(solid, m, mid, tip, r * 0.2, r * 0.05, 4, 0x1E1038, 1.0F, true);
			tube(glow, m, mid, tip, r * 0.07, r * 0.03, 4, 0x40FFB0, 0.9F, false);
		}
		// Spiral runes.
		for (int k = 0; k < 3; k++) {
			Vec3d prev = null;
			for (int s = 0; s <= 18; s++) {
				double u = s / 18.0;
				double ang = k * MathHelper.TAU / 3 + u * 9 + time * 0.03;
				Vec3d p = tail.add(dir.multiply(len * (0.1 + 0.6 * u))).add(bs[0].multiply(Math.cos(ang) * r * 1.03)).add(bs[1].multiply(Math.sin(ang) * r * 1.03));
				if (prev != null) {
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, r * 0.1, (s + k) % 2 == 0 ? 0x40FFB0 : 0xC070FF, 0.9F, 0.9F);
				}
				prev = p;
			}
		}
		// Exhaust.
		if (flame > 0.01) {
			Vec3d end = tail.subtract(dir.multiply(len * 0.9 * flame));
			tube(glow, m, tail, end, r * 0.9, 0.0, 10, 0x60FFE0, 0.55F, false);
			tube(glow, m, tail, tail.subtract(dir.multiply(len * 0.55 * flame)), r * 0.5, 0.0, 10, 0xFFFFFF, 0.85F, false);
			tube(glow, m, tail, tail.subtract(dir.multiply(len * 1.6 * flame)), r * 1.5, 0.0, 10, 0xA060FF, 0.18F, false);
		}
	}
}
