package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.CelestialTimeline;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The Unmaker: the doomed volume is first outlined as a flickering wireframe, then a render pass (a gridded
 * plane with a scanline) sweeps through it in a straight line. Behind the pass, deleted voxels float up as
 * magenta and black "missing texture" pixels and dissolve. Matches the server's sweep tick for tick.
 */
public class CelestialUnmakerFx extends FxEffect {
	private static final Vec3d UP = new Vec3d(0, 1, 0);
	private final Vec3d g, dir, side;
	private final double radius, y0, y1;

	public CelestialUnmakerFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.g = new Vec3d(p.x(), p.y(), p.z());
		Vec3d d = new Vec3d(p.x2(), 0, p.z2());
		this.dir = d.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : d.normalize();
		this.side = new Vec3d(-dir.z, 0, dir.x);
		this.radius = Math.max(4, p.scale());
		this.y0 = p.p1();
		this.y1 = p.p2() + 1;
	}

	private boolean scanning(double a) {
		return a >= CelestialTimeline.UM_START && a <= CelestialTimeline.UM_START + CelestialTimeline.UM_SCAN;
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (scanning(age)) {
			double s = CelestialTimeline.umPlane(age, radius);
			ClientFx.light(new Vec3d(g.x + dir.x * s, (y0 + y1) * 0.5, g.z + dir.z * s), 0.7F, 0xFF60FF);
		}
		return super.tick(world);
	}

	/** Camera-facing square "pixel". */
	private static void pixel(VertexConsumer vc, Matrix4f m, Vec3d p, Vec3d[] cb, double h, int rgb, float alpha) {
		Vec3d u = cb[0].multiply(h), v = cb[1].multiply(h);
		Solids.quad(vc, m, p.subtract(u).subtract(v), p.add(u).subtract(v), p.add(u).add(v), p.subtract(u).add(v), rgb, alpha);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d gc = new Vec3d(g.x - cam.x, 0, g.z - cam.z);
		double yb = y0 - cam.y, yt = y1 - cam.y;
		int start = CelestialTimeline.UM_START, end = start + CelestialTimeline.UM_SCAN;
		float fade = (float) Cinematics.clamp01((lifetime - a) / 40.0);

		// The wireframe of everything about to be unmade, flickering in before the pass and out after it.
		float frame = a < start ? Effects.smooth(a / 20.0F) : a > end ? (float) Math.max(0, 1 - (a - end) / 60.0) : 0.35F;
		if (frame > 0.01F) {
			int n = 24;
			int flick = (int) (a * 0.5F);
			for (int i = 0; i < n; i++) {
				double ang = i * MathHelper.TAU / n;
				Vec3d b = gc.add(Math.cos(ang) * radius, yb, Math.sin(ang) * radius);
				float f = frame * (Solids.hash(flick, i) > 0.3 ? 1.0F : 0.25F);
				FxDraw.ribbon(glow, m, b, b.add(0, yt - yb, 0), Vec3d.ZERO, 0.3, i % 2 == 0 ? 0xFF40FF : 0xFFFFFF, 0.6F * f, 0.6F * f);
			}
			int segs = Effects.lod(gc.add(0, yb, 0), radius, 24, 64);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			FxDraw.ring(glow, m, gc.add(0, yb, 0), x, z, radius - 0.3, radius + 0.3, 0xFF40FF, 0.6F * frame, 0.6F * frame, 0.6F * frame, segs);
			FxDraw.ring(glow, m, gc.add(0, yt, 0), x, z, radius - 0.3, radius + 0.3, 0xFFFFFF, 0.5F * frame, 0.5F * frame, 0.5F * frame, segs);
		}
		if (a > end) {
			// What's left at the rim: a band of missing-texture checker.
			int segs = 64;
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			for (int i = 0; i < segs; i++) {
				double a0 = i * MathHelper.TAU / segs, a1 = (i + 1) * MathHelper.TAU / segs;
				Vec3d p0 = gc.add(Math.cos(a0) * radius, g.y - cam.y, Math.sin(a0) * radius);
				Vec3d p1 = gc.add(Math.cos(a1) * radius, g.y - cam.y, Math.sin(a1) * radius);
				if (i % 2 == 0) {
					FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 1.2, 0xFF40FF, 0.4F * fade, 0.4F * fade);
				} else {
					FxDraw.ribbon(shade, m, p0, p1, Vec3d.ZERO, 1.2, 0x000000, 0.5F * fade, 0.5F * fade);
				}
			}
			return;
		}
		if (a < start) {
			return;
		}

		// --- the pass itself.
		double s = CelestialTimeline.umPlane(a, radius);
		double half = Math.sqrt(Math.max(0, radius * radius - s * s));
		Vec3d pc = gc.add(dir.multiply(s));
		Vec3d l0 = pc.subtract(side.multiply(half)), l1 = pc.add(side.multiply(half));
		Vec3d a0 = l0.add(0, yb, 0), a1 = l1.add(0, yb, 0), b0 = l0.add(0, yt, 0), b1 = l1.add(0, yt, 0);
		Solids.quad(glow, m, a0, a1, b1, b0, 0xFF40FF, 0.05F);
		FxKit.halo(consumers, m, cb, pc.add(0, (yb + yt) * 0.5, 0), Math.max(half, (yt - yb) * 0.5) * 1.3, 0xFF40FF, 0.18F);
		FxKit.halo(consumers, m, cb, l0.add(0, sl0(a, yb, yt), 0), 4, 0xFFFFFF, 0.7F);
		FxKit.halo(consumers, m, cb, l1.add(0, sl0(a, yb, yt), 0), 4, 0xFFFFFF, 0.7F);
		// Grid on the plane, one line per lattice cell.
		int cell = CelestialTimeline.UM_CELL;
		for (double v = -half + ((half) % cell); v <= half; v += cell) {
			Vec3d p = pc.add(side.multiply(v));
			FxDraw.ribbon(glow, m, p.add(0, yb, 0), p.add(0, yt, 0), Vec3d.ZERO, 0.15, 0xFFFFFF, 0.3F, 0.3F);
		}
		double gy = g.y - 1 - cam.y;
		for (double y = gy + Math.ceil((yb - gy) / cell) * cell; y <= yt; y += cell) {
			FxDraw.ribbon(glow, m, l0.add(0, y, 0), l1.add(0, y, 0), Vec3d.ZERO, 0.15, 0xFFFFFF, 0.3F, 0.3F);
		}
		// Bright frame and a fast scanline running up the plane.
		FxDraw.ribbon(glow, m, a0, a1, Vec3d.ZERO, 0.5, 0xFF40FF, 0.9F, 0.9F);
		FxDraw.ribbon(glow, m, b0, b1, Vec3d.ZERO, 0.5, 0xFF40FF, 0.9F, 0.9F);
		FxDraw.ribbon(glow, m, a0, b0, Vec3d.ZERO, 0.5, 0xFF40FF, 0.9F, 0.9F);
		FxDraw.ribbon(glow, m, a1, b1, Vec3d.ZERO, 0.5, 0xFF40FF, 0.9F, 0.9F);
		double sl = yb + (yt - yb) * ((a * 0.08) % 1.0);
		FxDraw.ribbon(glow, m, l0.add(0, sl, 0), l1.add(0, sl, 0), Vec3d.ZERO, 0.8, 0xFFFFFF, 0.8F, 0.8F);

		// --- deleted voxels drifting up behind the pass as missing-texture pixels.
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		int count = Effects.lod(pc.add(0, (yb + yt) * 0.5, 0), radius, 30, 110);
		for (int i = 0; i < count; i++) {
			double back = Solids.hash(i, 41) * 14;
			double u = s - back;
			if (u < -radius) {
				continue;
			}
			double w = Math.sqrt(Math.max(0, radius * radius - u * u));
			double v = (Solids.hash(i, 42) * 2 - 1) * w;
			double y = yb + Solids.hash(i, 43) * (yt - yb) + back * 0.9;
			Vec3d p = gc.add(dir.multiply(u)).add(side.multiply(v)).add(UP.multiply(y));
			float al = (float) (1 - back / 14.0);
			double h = 0.25 + 0.3 * Solids.hash(i, 44);
			if (i % 2 == 0) {
				pixel(glow, m, p, cb, h, 0xFF40FF, 0.8F * al);
			} else {
				pixel(shade, m, p, cb, h, 0x000000, 0.75F * al);
			}
		}
	}

	private static double sl0(float a, double yb, double yt) {
		return yb + (yt - yb) * ((a * 0.08) % 1.0);
	}
}
