package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.util.XenoShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * XENOFORM: the hypercube turning through the fourth dimension over the land, its rings of glyphs, the scanning
 * plane, the filaments that take the land apart, and the land itself rising block by block in straight lines and
 * sorting itself by colour into spectrum rings. The rings fold into the hypercube, the hypercube folds into the
 * monolith, and a column of light leaves for the sky.
 */
final class XenoFx extends FxEffect {
	private static final double SAMPLE = 0.13;
	private static final int MAX_CUBES = 9000, LAYERS = 3;
	private static final int[][] EDGES = edges();

	private final Vec3d c, fwd, side;
	private final double r;
	private final int[] cols;
	private final float[] dist;
	private int cursor;
	private final List<Cube> cubes = new ArrayList<>();

	/** One block of the land on its way to the rings. Its slot round its ring is re-ranked as the ring fills. */
	private static final class Cube {
		final double x, y, z, key, dr, dy;
		final int rgb, ring;
		final float start;
		double slot;

		Cube(double x, double y, double z, int rgb, float start, int ring, double key, double dr, double dy) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.rgb = rgb;
			this.start = start;
			this.ring = ring;
			this.key = key;
			this.dr = dr;
			this.dy = dy;
		}
	}

	@SuppressWarnings("unchecked")
	private final List<Cube>[] rings = new List[XenoShapes.RINGS];
	private boolean reRank;

	XenoFx(Vec3d c, Vec3d heading, double radius, int life) {
		super(Math.max(40, life));
		this.c = c;
		this.fwd = heading.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : heading.normalize();
		this.side = new Vec3d(-fwd.z, 0, fwd.x);
		this.r = Math.max(16, radius);
		for (int k = 0; k < XenoShapes.RINGS; k++) {
			rings[k] = new ArrayList<>();
		}
		// The columns that will be shown rising, nearest first.
		int R = MathHelper.ceil(r);
		List<int[]> list = new ArrayList<>();
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d <= r && XenoShapes.hash(dx * 31 + 7, dz * 17 + 3) < SAMPLE) {
					list.add(new int[]{dx, dz});
				}
			}
		}
		list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		cols = new int[list.size() * 2];
		dist = new float[list.size()];
		for (int i = 0; i < list.size(); i++) {
			cols[i * 2] = list.get(i)[0];
			cols[i * 2 + 1] = list.get(i)[1];
			dist[i] = (float) Math.sqrt(list.get(i)[0] * list.get(i)[0] + list.get(i)[1] * list.get(i)[1]);
		}
	}

	private static int[][] edges() {
		List<int[]> e = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			for (int b = 0; b < 4; b++) {
				int j = i ^ (1 << b);
				if (j > i) {
					e.add(new int[]{i, j});
				}
			}
		}
		return e.toArray(new int[0][]);
	}

	// ---------------------------------------------------------------- tick: sampling the land before it goes

	@Override
	public boolean tick(ClientWorld world) {
		float t = age;
		// Sample the columns the front is about to reach, while their blocks are still there.
		double ahead = XenoShapes.front(t + 14, r) + 2;
		while (cursor < dist.length && dist[cursor] <= ahead && t >= XenoShapes.HARVEST - 20 && cubes.size() < MAX_CUBES) {
			sample(world, cols[cursor * 2], cols[cursor * 2 + 1], dist[cursor]);
			cursor++;
		}
		if (reRank) {
			// Each ring sorted round by its key: an even spread that reads as a smooth spectrum.
			for (List<Cube> ring : rings) {
				ring.sort((p, q) -> Double.compare(p.key, q.key));
				for (int i = 0; i < ring.size(); i++) {
					ring.get(i).slot = (i + 0.5) / ring.size() * MathHelper.TAU;
				}
			}
			reRank = false;
		}
		if (t >= XenoShapes.HARVEST && t < XenoShapes.HARVEST_END) {
			double front = XenoShapes.front(t, r);
			for (int i = 0; i < 3; i++) {
				double a = (world == null ? Math.random() : world.random.nextDouble()) * MathHelper.TAU;
				double x = c.x + Math.cos(a) * front, z = c.z + Math.sin(a) * front;
				FxEngine.play(VfxIds.XENO_FRONT, x, NewFx.groundY(x, z, c.y), z, 0, 1, 0, 1.0F, 0, age * 131L + i);
			}
		}
		if (age % 10 == 0) {
			ClientFx.light(c.add(0, XenoShapes.coreY(t), 0), 0.8F, 0xC0B0FF);
		}
		return super.tick(world);
	}

	private void sample(ClientWorld world, int dx, int dz, float d) {
		int x = MathHelper.floor(c.x) + dx, z = MathHelper.floor(c.z) + dz;
		int floor = MathHelper.floor(c.y) + XenoShapes.FLOOR;
		float start = (float) XenoShapes.reach(d, r);
		if (world == null) {
			// Offline preview: a made-up meadow with trees.
			double h = XenoShapes.hash(dx, dz);
			int rgb = h < 0.55 ? 0x5C9A3A : h < 0.75 ? 0x7A5A3A : h < 0.9 ? 0x8A8A8A : 0x3E6EC8;
			add(x, c.y + h * 4, z, rgb, start, dx, dz);
			return;
		}
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		BlockPos.Mutable p = new BlockPos.Mutable();
		int step = Math.max(2, (top - floor) / LAYERS);
		int n = 0;
		for (int y = top; y > floor && n < LAYERS; y -= step) {
			BlockState s = world.getBlockState(p.set(x, y, z));
			if (s.isAir()) {
				continue;
			}
			int rgb = s.getMapColor(world, p).color;
			if (s.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
				rgb = 0x3E6EC8;
			}
			if (rgb == 0) {
				continue;
			}
			add(x, y, z, rgb, start + n * 5, dx, dz);
			n++;
		}
	}

	private void add(double x, double y, double z, int rgb, float start, int dx, int dz) {
		float[] hsv = hsv(rgb);
		int ring;
		if (hsv[2] < 0.35F) {
			ring = 0;
		} else if (hsv[1] < 0.18F) {
			ring = 5;
		} else if (hsv[0] < 0.1F || hsv[0] > 0.9F) {
			ring = 1;
		} else if (hsv[0] < 0.2F) {
			ring = 2;
		} else if (hsv[0] < 0.45F) {
			ring = 3;
		} else {
			ring = 4;
		}
		// Within a ring, sorted round by hue (greys by brightness), so each ring becomes a smooth spectrum.
		double key = (ring == 0 || ring == 5 ? hsv[2] : hsv[0]) + XenoShapes.hash(dx, dz + 5) * 1.0E-4;
		double dr = (hsv[2] - 0.5) * 4 + (XenoShapes.hash(dz, dx) - 0.5) * 1.5;
		double dy = (XenoShapes.hash(dx + 3, dz) - 0.5) * 2.4;
		Cube cube = new Cube(x + 0.5, y + 0.5, z + 0.5, rgb, start, ring, key, dr, dy);
		cubes.add(cube);
		rings[ring].add(cube);
		reRank = true;
	}

	private static float[] hsv(int rgb) {
		float rr = (rgb >> 16 & 255) / 255F, gg = (rgb >> 8 & 255) / 255F, bb = (rgb & 255) / 255F;
		float max = Math.max(rr, Math.max(gg, bb)), min = Math.min(rr, Math.min(gg, bb)), dd = max - min;
		float h = 0;
		if (dd > 1.0E-4F) {
			if (max == rr) {
				h = ((gg - bb) / dd) % 6;
			} else if (max == gg) {
				h = (bb - rr) / dd + 2;
			} else {
				h = (rr - gg) / dd + 4;
			}
			h /= 6;
			if (h < 0) {
				h += 1;
			}
		}
		return new float[]{h, max < 1.0E-4F ? 0 : dd / max, max};
	}

	// ---------------------------------------------------------------- render

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		Vec3d o = c.subtract(cam);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		double cmp = XenoShapes.compress(a);
		Vec3d core = o.add(0, XenoShapes.coreY(a), 0);
		float gone = 1 - smooth((a - XenoShapes.LAND) / 6.0F);

		seed(glow, shade, m, cb, core, a);
		if (gone > 0.01F) {
			hypercube(glow, shade, m, cb, core, a, cmp, gone);
			glyphs(glow, m, core, a, cmp);
		}
		scan(glow, m, o, a);
		filaments(glow, m, cb, o, core, a);
		harvest(solid, glow, m, cb, o, a, cmp);
		depart(glow, m, cb, o, a);
	}

	/** Before the unfolding: a point of nothing with a ring of bent light round it. */
	private void seed(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d core, float a) {
		float in = smooth(a / 30.0F), out = 1 - smooth((a - XenoShapes.UNFOLDED) / 30.0F);
		float s = in * out;
		if (s <= 0.01F) {
			return;
		}
		double rad = 0.6 + 1.6 * smooth((a - 20) / 40.0F);
		FxDraw.disc(shade, m, core, cb[0], cb[1], rad, 0x000000, 0.97F * s, 0.97F * s, 24);
		FxDraw.ring(glow, m, core, cb[0], cb[1], rad, rad * 1.25, 0xF4F0FF, 0.0F, 0.9F * s, 0.0F, 32);
		FxDraw.ring(glow, m, core, cb[0], cb[1], rad * 1.6, rad * 4.5, 0x8070FF, 0.0F, 0.18F * s, 0.0F, 40);
	}

	/**
	 * A tesseract: sixteen corners turning through four dimensions, projected down to three. At the end its corners
	 * are drawn onto the monolith's box as it sinks.
	 */
	private void hypercube(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d core, float a, double cmp, float gone) {
		double un = XenoShapes.unfold(a);
		if (un <= 0.001) {
			return;
		}
		for (int layer = 0; layer < 2; layer++) {
			double size = XenoShapes.SIZE * un * (layer == 0 ? 1 : 0.42);
			double dir = layer == 0 ? 1 : -1.7;
			double spin = a * (1 - cmp * 0.9);
			Vec3d[] v = new Vec3d[16];
			for (int i = 0; i < 16; i++) {
				double[] p = {(i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1, (i & 8) == 0 ? -1 : 1};
				rot(p, 0, 3, spin * 0.011 * dir);
				rot(p, 1, 3, spin * 0.007 * dir);
				rot(p, 2, 3, spin * 0.009 * dir);
				rot(p, 0, 2, spin * 0.004);
				double k = 1.5 / (2.6 - p[3]);
				Vec3d q = new Vec3d(p[0] * k, p[1] * k, p[2] * k).multiply(size);
				if (layer == 0 && cmp > 0) {
					// Folding into the monolith: each corner onto a corner of a 16 x 36 x 4 box.
					Vec3d box = side.multiply(((i & 1) == 0 ? -1 : 1) * 8).add(0, ((i & 2) == 0 ? -1 : 1) * XenoShapes.MONOLITH * 0.5, 0)
							.add(fwd.multiply(((i & 4) == 0 ? -1 : 1) * 2));
					q = q.lerp(box, cmp);
				}
				v[i] = core.add(q);
			}
			int edge = layer == 0 ? 0xF0ECFF : 0x80F0FF;
			float al = (float) (Math.min(1.0, un * 1.5) * gone * (layer == 1 ? 1 - cmp : 1));
			// Dark faces: an impossible solid.
			if (layer == 0) {
				for (int ax = 0; ax < 4; ax++) {
					for (int ay = ax + 1; ay < 4; ay++) {
						int rest = 15 & ~(1 << ax) & ~(1 << ay);
						for (int fixed = 0; fixed < 16; fixed++) {
							if ((fixed & ~rest) != 0) {
								continue;
							}
							int i0 = fixed, i1 = fixed | 1 << ax, i2 = fixed | 1 << ax | 1 << ay, i3 = fixed | 1 << ay;
							Solids.quad(shade, m, v[i0], v[i1], v[i2], v[i3], 0x07030E, (float) (0.22 + 0.5 * cmp) * al);
						}
					}
				}
			}
			for (int[] e : EDGES) {
				FxDraw.ribbon(glow, m, v[e[0]], v[e[1]], Vec3d.ZERO, layer == 0 ? 0.32 : 0.2, edge, 0.85F * al, 0.85F * al);
			}
			for (Vec3d p : v) {
				Solids.dot(glow, m, p, cb, layer == 0 ? 0.9 : 0.5, 0xFFFFFF, 0.8F * al);
			}
		}
		// The singularity at its heart.
		float s = (float) (un * (1 - cmp)) * gone;
		FxDraw.disc(shade, m, core, cb[0], cb[1], 1.6, 0x000000, 0.97F * s, 0.97F * s, 20);
		FxDraw.ring(glow, m, core, cb[0], cb[1], 1.6, 2.0, 0xFFFFFF, 0.0F, 0.9F * s, 0.0F, 24);
	}

	private static void rot(double[] p, int i, int j, double ang) {
		double cs = Math.cos(ang), sn = Math.sin(ang);
		double a = p[i] * cs - p[j] * sn, b = p[i] * sn + p[j] * cs;
		p[i] = a;
		p[j] = b;
	}

	/** Three tilted rings of alien glyphs turning round it. */
	private void glyphs(VertexConsumer glow, Matrix4f m, Vec3d core, float a, double cmp) {
		float vis = smooth((a - XenoShapes.UNFOLDED + 40) / 40.0F) * (float) (1 - cmp);
		if (vis <= 0.01F) {
			return;
		}
		double[] tilt = {0.35, -0.5, 1.25};
		for (int k = 0; k < 3; k++) {
			double rad = 21 + k * 4;
			int n = 40;
			for (int i = 0; i < n; i++) {
				double ang = i * MathHelper.TAU / n + a * 0.004 * (k == 1 ? -1 : 1) * (k + 1);
				Vec3d p = new Vec3d(Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
				Vec3d tan = new Vec3d(-Math.sin(ang), 0, Math.cos(ang));
				p = tiltX(p, tilt[k]);
				tan = tiltX(tan, tilt[k]);
				Vec3d up = tiltX(new Vec3d(0, 1, 0), tilt[k]);
				float flick = 0.6F + 0.4F * MathHelper.sin(a * 0.2F + i * 1.7F + k);
				Solids.glyph(glow, m, core.add(p), tan, up, 1.6, i * 7 + k * 101 + (int) (a / 40), k == 2 ? 0x80F0FF : 0xC0A8FF, 0.7F * vis * flick);
			}
		}
	}

	private static Vec3d tiltX(Vec3d p, double t) {
		return new Vec3d(p.x, p.y * Math.cos(t) - p.z * Math.sin(t), p.y * Math.sin(t) + p.z * Math.cos(t));
	}

	/** The scanning plane: a disc of ruled light passing down through the land and back up. */
	private void scan(VertexConsumer glow, Matrix4f m, Vec3d o, float a) {
		if (a < XenoShapes.SCAN || a > XenoShapes.SCAN_END) {
			return;
		}
		float vis = smooth((a - XenoShapes.SCAN) / 10.0F) * (1 - smooth((a - XenoShapes.SCAN_END + 10) / 10.0F));
		Vec3d p = o.add(0, XenoShapes.scanY(a), 0);
		double scroll = (a * 0.3) % 6;
		for (double k = -r + scroll; k <= r; k += 6) {
			double half = Math.sqrt(Math.max(0, r * r - k * k));
			FxDraw.ribbon(glow, m, p.add(-half, 0, k), p.add(half, 0, k), Vec3d.ZERO, 0.18, 0x70F0FF, 0.3F * vis, 0.3F * vis);
			FxDraw.ribbon(glow, m, p.add(k, 0, -half), p.add(k, 0, half), Vec3d.ZERO, 0.18, 0x70F0FF, 0.3F * vis, 0.3F * vis);
		}
		Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
		FxDraw.ring(glow, m, p, X, Z, r - 1.5, r + 1.5, 0xE0FFFF, 0.0F, 0.8F * vis, 0.0F, 96);
		FxDraw.ring(glow, m, p, X, Z, 0, r, 0x4080FF, 0.04F * vis, 0.06F * vis, 0.0F, 64);
	}

	/** Filaments of light from the hypercube down to the edge where the land is being taken. */
	private void filaments(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, Vec3d core, float a) {
		if (a < XenoShapes.HARVEST || a > XenoShapes.HARVEST_END + 10) {
			return;
		}
		float vis = smooth((a - XenoShapes.HARVEST) / 15.0F) * (1 - smooth((a - XenoShapes.HARVEST_END) / 10.0F));
		double front = XenoShapes.front(a, r);
		int n = 48;
		for (int k = 0; k < n; k++) {
			double ang = k * MathHelper.TAU / n + a * 0.012;
			double x = c.x + Math.cos(ang) * front, z = c.z + Math.sin(ang) * front;
			Vec3d foot = new Vec3d(x, NewFx.groundY(x, z, c.y) + 0.5, z).subtract(c).add(o);
			float fl = 0.6F + 0.4F * MathHelper.sin(a * 0.5F + k * 2.3F);
			FxDraw.ribbon(glow, m, core, foot, Vec3d.ZERO, 0.22, 0xE8F4FF, 0.12F * vis * fl, 0.45F * vis * fl);
			Solids.dot(glow, m, foot, cb, 1.2, 0xB0F0FF, 0.8F * vis * fl);
		}
	}

	/**
	 * The land rising: each sampled block lifts off straight up when the front reaches it, then spirals into its
	 * place in the colour rings; at the end the rings fold into the hypercube.
	 */
	private void harvest(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a, double cmp) {
		if (cubes.isEmpty() || cmp >= 0.999) {
			return;
		}
		double coreY = XenoShapes.coreY(a);
		// Faint orbit guides: the catalogue's shelves.
		float guide = smooth((a - XenoShapes.HARVEST - 40) / 40.0F) * (float) (1 - cmp);
		if (guide > 0.01F) {
			Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
			for (int k = 0; k < XenoShapes.RINGS; k++) {
				double rr = XenoShapes.ringR(k) * (1 - cmp);
				Vec3d ctr = o.add(0, XenoShapes.ringY(k), 0);
				FxDraw.ring(glow, m, ctr, X, Z, rr - 2.6, rr + 2.6, 0xC8C0FF, 0.0F, 0.07F * guide, 0.0F, 96);
				FxDraw.ring(glow, m, ctr, X, Z, rr - 0.12, rr + 0.12, 0xE8E4FF, 0.35F * guide, 0.35F * guide, 0.35F * guide, 128);
			}
		}
		for (Cube q : cubes) {
			double e = a - q.start;
			if (e < 0) {
				continue;
			}
			int k = q.ring;
			double w = 0.006 * (k % 2 == 0 ? 1 : -1) * (1 + k * 0.15);
			double ringR = XenoShapes.ringR(k) + q.dr, ringY = XenoShapes.ringY(k) + q.dy;
			double th = q.slot + a * w;
			double x, y, z, grow = 0;
			double rx = q.x - c.x, rz = q.z - c.z;
			if (e < 25) {
				double u = e / 25.0;
				u = u * u;
				x = rx;
				z = rz;
				y = (q.y - c.y) + (ringY - (q.y - c.y)) * u;
			} else if (e < 55) {
				double u = smooth((e - 25) / 30.0);
				double r0 = Math.hypot(rx, rz), t0 = Math.atan2(rz, rx);
				double dt = MathHelper.wrapDegrees(Math.toDegrees(th - t0)) * Math.PI / 180;
				double rr = r0 + (ringR - r0) * u, tt = t0 + dt * u;
				x = Math.cos(tt) * rr;
				z = Math.sin(tt) * rr;
				y = ringY;
				grow = u;
			} else {
				x = Math.cos(th) * ringR;
				z = Math.sin(th) * ringR;
				y = ringY;
				grow = 1;
			}
			if (cmp > 0) {
				x *= 1 - cmp;
				z *= 1 - cmp;
				y += (coreY - y) * cmp;
			}
			Vec3d p = o.add(x, y, z);
			double h = (0.45 + 0.5 * grow) * (1 - cmp * 0.9);
			cube(solid, m, p, h, q.rgb);
			if (e < 25) {
				// A thread of light under each block as it lifts.
				float tr = (float) (1 - e / 25.0);
				FxDraw.ribbon(glow, m, p, p.add(0, -3 - e * 0.15, 0), Vec3d.ZERO, 0.15, 0xC8F0FF, 0.5F * tr, 0.0F);
			}
			if (e < 6) {
				Solids.dot(glow, m, p, cb, 1.0, 0xFFFFFF, (float) (1 - e / 6.0));
			}
		}
	}

	/** An axis-aligned cube, only the three faces turned toward the camera, shaded by face. */
	private static void cube(VertexConsumer vc, Matrix4f m, Vec3d p, double h, int rgb) {
		// p is relative to the camera, so the faces toward it are the ones on the camera's side.
		double sx = p.x > 0 ? -1 : 1, sy = p.y > 0 ? -1 : 1, sz = p.z > 0 ? -1 : 1;
		double fx = p.x + sx * h, fy = p.y + sy * h, fz = p.z + sz * h;
		face(vc, m, new Vec3d(fx, p.y - h, p.z - h), new Vec3d(fx, p.y + h, p.z - h), new Vec3d(fx, p.y + h, p.z + h), new Vec3d(fx, p.y - h, p.z + h),
				Solids.scale(rgb, 0.8));
		face(vc, m, new Vec3d(p.x - h, fy, p.z - h), new Vec3d(p.x + h, fy, p.z - h), new Vec3d(p.x + h, fy, p.z + h), new Vec3d(p.x - h, fy, p.z + h),
				Solids.scale(rgb, sy > 0 ? 1.0 : 0.55));
		face(vc, m, new Vec3d(p.x - h, p.y - h, fz), new Vec3d(p.x + h, p.y - h, fz), new Vec3d(p.x + h, p.y + h, fz), new Vec3d(p.x - h, p.y + h, fz),
				Solids.scale(rgb, 0.68));
	}

	/** A quad wound to face the camera (at the origin), whichever way round its corners were given. */
	private static void face(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb) {
		Vec3d n = b.subtract(a).crossProduct(c.subtract(a));
		if (n.dotProduct(a) > 0) {
			Solids.quad(vc, m, d, c, b, a, rgb, 1.0F);
		} else {
			Solids.quad(vc, m, a, b, c, d, rgb, 1.0F);
		}
	}

	/** The column of light leaving for the sky. */
	private void depart(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d o, float a) {
		if (a < XenoShapes.DEPART) {
			return;
		}
		float u = (a - XenoShapes.DEPART) / (XenoShapes.END - XenoShapes.DEPART);
		float w = smooth(u * 6) * (1 - smooth((u - 0.4F) / 0.6F));
		Vec3d top = o.add(0, XenoShapes.FLOOR + XenoShapes.MONOLITH + 1, 0);
		FxDraw.beam(glow, m, top, top.add(0, 420, 0), Vec3d.ZERO, 5 * w + 0.5, 0xF4F0FF, 0.8F * w);
		FxDraw.beam(glow, m, top, top.add(0, 420, 0), Vec3d.ZERO, 14 * w, 0x8070FF, 0.2F * w);
		Solids.flare(glow, m, top, cb, 10 * w, 0xFFFFFF, w);
	}

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}
}
