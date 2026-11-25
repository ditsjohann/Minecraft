package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.util.WonderShapes.Shift;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Dimension Shift as a portal: a gate of obsidian opens over the bomb, its purple membrane swirling, and the shift
 * runs out from it as a wall of the same portal light, edged in obsidian, sweeping the land into the Nether.
 */
final class ShiftFx extends FxEffect {
	private static final int OBSIDIAN = 0x1C1028, VIOLET = 0x9A40FF, PALE = 0xE0B8FF;
	private static final double GATE_W = 7, GATE_H = 20, GATE_Y = 3;
	private final Vec3d c;

	ShiftFx(Vec3d c, int life) {
		super(Math.max(40, life));
		this.c = c;
	}

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}

	/** The portal's purple: bands of light and dark swirling across it like a Nether portal's. */
	private static float swirl(double u, double v, float a) {
		double s = Math.sin(u * 0.9 + a * 0.13 + Math.sin(v * 0.6 - a * 0.07) * 2) * Math.sin(v * 0.7 - a * 0.1 + Math.cos(u * 0.5 + a * 0.05) * 2);
		return (float) MathHelper.clamp(0.55 + 0.45 * s, 0.0, 1.0);
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age % 6 == 0) {
			ClientFx.light(c.add(0, GATE_Y + GATE_H * 0.5, 0), 0.9F, VIOLET);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d o = c.subtract(cam);
		gate(glow, shade, solid, consumers, m, cb, o, a);
		front(glow, shade, m, o, cam, a);
	}

	/** The gate: an obsidian frame turning slowly, its membrane opening from a slit and pinching shut at the end. */
	private void gate(VertexConsumer glow, VertexConsumer shade, VertexConsumer solid, VertexConsumerProvider consumers, Matrix4f m, Vec3d[] cb, Vec3d o,
			float a) {
		float rise = smooth(a / 30.0);
		float open = smooth((a - 10) / (Shift.RIFT + 10.0)) * (1 - smooth((a - Shift.CLOSE + 10) / 14.0));
		float frame = rise * (1 - smooth((a - Shift.CLOSE - 10) / 20.0));
		if (frame <= 0.01F) {
			return;
		}
		double ang = a * 0.006;
		Vec3d across = new Vec3d(Math.cos(ang), 0, Math.sin(ang)), up = new Vec3d(0, 1, 0);
		Vec3d base = o.add(0, GATE_Y - (1 - rise) * GATE_H, 0);
		double w = GATE_W, h = GATE_H, t = 1.6 * frame;
		// The frame: four obsidian beams with crying-obsidian glints.
		Vec3d bl = base.add(across.multiply(-w)), br = base.add(across.multiply(w)), tl = bl.add(up.multiply(h)), tr = br.add(up.multiply(h));
		for (Vec3d[] e : new Vec3d[][]{{bl, br}, {tl, tr}, {bl, tl}, {br, tr}}) {
			Solids.tube(solid, m, e[0], e[1], t, t, 4, OBSIDIAN, 1.0F, true);
			for (int k = 0; k < 5; k++) {
				double u = (k + 0.5) / 5 + 0.02 * Math.sin(a * 0.1 + k);
				Solids.flare(glow, m, e[0].lerp(e[1], u), cb, 0.9, 0xC060FF, 0.5F + 0.4F * MathHelper.sin(a * 0.2F + k));
			}
		}
		if (open <= 0.01F) {
			return;
		}
		// The membrane: opening outward from the middle as a slit, swirling purple, dark between the bands.
		int nu = 14, nv = 24;
		double half = w * open;
		for (int i = 0; i < nu; i++) {
			for (int j = 0; j < nv; j++) {
				double u0 = -half + 2 * half * i / nu, u1 = -half + 2 * half * (i + 1) / nu;
				double v0 = h * j / nv, v1 = h * (j + 1) / nv;
				double[][] q = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
				for (int k = 0; k < 4; k++) {
					Vec3d p = base.add(across.multiply(q[k][0])).add(up.multiply(q[k][1]));
					FxDraw.vertex(shade, m, p.x, p.y, p.z, 0x200838, 0.85F * open);
				}
				for (int k = 0; k < 4; k++) {
					Vec3d p = base.add(across.multiply(q[k][0])).add(up.multiply(q[k][1]));
					float s = swirl(q[k][0], q[k][1], a);
					FxDraw.vertex(glow, m, p.x, p.y, p.z, s > 0.8 ? PALE : VIOLET, (0.2F + 0.6F * s) * open);
				}
			}
		}
		FxKit.halo(consumers, m, cb, base.add(up.multiply(h * 0.5)), 14 * open, VIOLET, 0.35F * open);
	}

	/**
	 * The front: a wall of portal light standing on the edge of the shift, swirling purple with obsidian-black rims
	 * crackling along its foot and its crest.
	 */
	private void front(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d o, Vec3d cam, float a) {
		if (a < Shift.RIFT || a > Shift.SWEEP_END + 30) {
			return;
		}
		double r = Shift.front(Math.min(a, Shift.SWEEP_END));
		float on = smooth((a - Shift.RIFT) / 10.0) * (1 - smooth((a - Shift.SWEEP_END) / 30.0));
		if (r < 2 || on <= 0.01F) {
			return;
		}
		int n = Math.max(32, (int) (r * 1.6));
		double wallH = 22;
		double[] gy = new double[n + 1];
		for (int k = 0; k <= n; k++) {
			double ang = k * MathHelper.TAU / n;
			gy[k] = NewFx.groundY(c.x + Math.cos(ang) * r, c.z + Math.sin(ang) * r, c.y) - cam.y;
		}
		int rows = 6;
		for (int k = 0; k < n; k++) {
			double a0 = k * MathHelper.TAU / n, a1 = (k + 1) * MathHelper.TAU / n;
			double x0 = o.x + Math.cos(a0) * r, z0 = o.z + Math.sin(a0) * r, x1 = o.x + Math.cos(a1) * r, z1 = o.z + Math.sin(a1) * r;
			for (int j = 0; j < rows; j++) {
				double v0 = wallH * j / rows, v1 = wallH * (j + 1) / rows;
				float f0 = 1 - (float) (v0 / wallH), f1 = 1 - (float) (v1 / wallH);
				float s00 = swirl(a0 * r, v0, a), s10 = swirl(a1 * r, v0, a), s11 = swirl(a1 * r, v1, a), s01 = swirl(a0 * r, v1, a);
				FxDraw.vertex(shade, m, x0, gy[k] + v0, z0, 0x2A0848, 0.4F * f0 * on);
				FxDraw.vertex(shade, m, x1, gy[k + 1] + v0, z1, 0x2A0848, 0.4F * f0 * on);
				FxDraw.vertex(shade, m, x1, gy[k + 1] + v1, z1, 0x2A0848, 0.4F * f1 * on);
				FxDraw.vertex(shade, m, x0, gy[k] + v1, z0, 0x2A0848, 0.4F * f1 * on);
				FxDraw.vertex(glow, m, x0, gy[k] + v0, z0, s00 > 0.8 ? PALE : VIOLET, (0.15F + 0.5F * s00) * f0 * on);
				FxDraw.vertex(glow, m, x1, gy[k + 1] + v0, z1, s10 > 0.8 ? PALE : VIOLET, (0.15F + 0.5F * s10) * f0 * on);
				FxDraw.vertex(glow, m, x1, gy[k + 1] + v1, z1, s11 > 0.8 ? PALE : VIOLET, (0.15F + 0.5F * s11) * f1 * on);
				FxDraw.vertex(glow, m, x0, gy[k] + v1, z0, s01 > 0.8 ? PALE : VIOLET, (0.15F + 0.5F * s01) * f1 * on);
			}
			// Obsidian rims: a black band along the foot, crackling with violet.
			Vec3d p0 = new Vec3d(x0, gy[k] + 0.2, z0), p1 = new Vec3d(x1, gy[k + 1] + 0.2, z1);
			FxDraw.ribbon(shade, m, p0, p1, Vec3d.ZERO, 2.2, OBSIDIAN, 0.9F * on, 0.9F * on);
			float crack = 0.5F + 0.5F * MathHelper.sin(a * 0.9F + k * 1.7F);
			FxDraw.ribbon(glow, m, p0.add(0, 0.3, 0), p1.add(0, 0.3, 0), Vec3d.ZERO, 0.5, 0xC070FF, 0.9F * on * crack, 0.9F * on * crack);
			FxDraw.ribbon(glow, m, p0.add(0, wallH * 0.55, 0), p1.add(0, wallH * 0.55, 0), Vec3d.ZERO, 0.25, PALE, 0.25F * on, 0.25F * on);
		}
	}
}
