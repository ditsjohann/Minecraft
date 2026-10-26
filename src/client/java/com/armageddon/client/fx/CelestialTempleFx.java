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
 * Godfall's temple: rings of gold light run around the top of each terrace, one after another from the rim
 * inward, a column of light stands on the altar, and a slowly turning halo hangs over the great spear's tip.
 */
public class CelestialTempleFx extends FxEffect {
	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
	private final Vec3d c;
	private final double radius;
	private final int gy;

	public CelestialTempleFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.c = new Vec3d(p.x(), p.y(), p.z());
		this.radius = Math.max(6, p.scale());
		this.gy = (int) p.p1();
	}

	private float fade(float a) {
		return Effects.smooth(a / 10.0F) * (float) Cinematics.clamp01((lifetime - a) / 60.0);
	}

	@Override
	public boolean tick(ClientWorld world) {
		ClientFx.light(new Vec3d(c.x, gy + 30, c.z), 0.6F * fade(age), 0xFFD890);
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		float f = fade(a);
		if (f <= 0.01F) {
			return;
		}
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d g = new Vec3d(c.x - cam.x, gy - cam.y, c.z - cam.z);
		int steps = CelestialTimeline.GF_STEPS;
		double w = radius / (steps + 1.0);
		int segs = Effects.lod(g, radius, 32, 96);

		// A ring of gold along the lip of every terrace, lighting from the rim inward.
		for (int k = 1; k <= steps; k++) {
			float on = Effects.smooth((a - 8 - k * 7) / 10.0F);
			if (on <= 0) {
				continue;
			}
			double rk = radius - k * w;
			double y = -2 * k + 3;
			float al = f * on * (0.42F + 0.13F * MathHelper.sin(a * 0.1F - k));
			FxDraw.ring(glow, m, g.add(0, y, 0), X, Z, rk - 0.6, rk + 0.6, 0xFFD060, 0.0F, al, 0.0F, segs);
			FxDraw.ring(glow, m, g.add(0, y + 0.2, 0), X, Z, rk - 2.5, rk, 0xFFE8A0, 0.0F, al * 0.25F, 0.0F, segs);
		}

		// The column on the altar and the halo over the spear.
		Vec3d altar = g.add(0, -2 * steps + 1, 0);
		FxDraw.beam(glow, m, altar, altar.add(0, 220, 0), Vec3d.ZERO, 3.0, 0xFFE8A0, 0.35F * f);
		Vec3d tip = altar.add(0, 38, 0);
		double spin = a * 0.02;
		Vec3d u = new Vec3d(Math.cos(spin), 0, Math.sin(spin)), v = new Vec3d(0, 1, 0);
		Vec3d face = u.crossProduct(v);
		FxDraw.ring(glow, m, tip, u, v, 5.5, 6.5, 0xFFE070, 0.0F, 0.8F * f, 0.0F, 48);
		FxDraw.ring(glow, m, tip, face, v, 8.0, 8.4, 0xFFF0C0, 0.0F, 0.5F * f, 0.0F, 48);
		for (int i = 0; i < 12; i++) {
			double ang = i * MathHelper.TAU / 12 + spin * 2;
			Vec3d d = u.multiply(Math.cos(ang)).add(v.multiply(Math.sin(ang)));
			FxDraw.ribbon(glow, m, tip.add(d.multiply(6.8)), tip.add(d.multiply(9.5 + 1.5 * Solids.hash(i, 61))), Vec3d.ZERO, 0.35, 0xFFE8A0, 0.6F * f, 0.0F);
		}
		Solids.flare(glow, m, tip, cb, 6, 0xFFE8A0, 0.6F * f);
		FxKit.flare(consumers, m, cb, tip, 22, 0xFFE8A0, 0.7F * f, a * 0.004);
		// Heavenly haze filling the light column, brightest at the altar.
		for (int k = 0; k < 8; k++) {
			Vec3d p = altar.add(0, 6 + k * 24, 0);
			FxKit.halo(consumers, m, cb, p, 9 + k * 2.5, 0xFFE8B0, 0.16F * f * (1 - k / 9.0F));
		}
		FxKit.groundShock(consumers, m, g.add(0, -2 * steps + 1.2, 0), radius * (0.5 + 0.5 * ((a * 0.01) % 1.0)), 0xFFE070, 0.35F * f);

		// Motes spiralling up the column.
		int n = Effects.lod(altar, 20, 12, 40);
		for (int i = 0; i < n; i++) {
			double h = ((a * 0.6 + Solids.hash(i, 62) * 120) % 120);
			double ang = i * 2.4 + h * 0.08;
			double rr = 2.0 + 2.0 * Solids.hash(i, 63);
			Vec3d p = altar.add(Math.cos(ang) * rr, h, Math.sin(ang) * rr);
			Solids.dot(glow, m, p, cb, 0.3, 0xFFF0C0, 0.7F * f * (float) (1 - h / 120.0));
		}
	}
}
