package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Final Existence: the sky breaks like glass. Pink cracks spread down a dome from the zenith, then every pane
 * falls away, tumbling outward, and behind it is no sky at all: a dark, starry cosmos with drifting nebulae.
 */
public class CelestialShatterFx extends FxEffect {
	private static final int LAT = 7, LON = 20;
	private final Vec3d c;
	private final double radius;
	private final int breakAt;

	public CelestialShatterFx(FxPayload p) {
		super(Math.max(60, p.duration()));
		this.c = new Vec3d(p.x(), p.y(), p.z());
		this.radius = Math.max(40, p.scale());
		this.breakAt = Math.max(10, (int) p.p1());
	}

	private static Vec3d dome(double elev, double az) {
		return new Vec3d(Math.cos(elev) * Math.cos(az), Math.sin(elev), Math.cos(elev) * Math.sin(az));
	}

	private static double elev(int i) {
		return Math.toRadians(18 + (90 - 18) * i / (double) LAT);
	}

	/** When cell (i, j) cracks: the panes near the zenith go first. */
	private double crackAt(int i, int j) {
		return breakAt * (1 - (i + 1) / (double) LAT * 0.9) * (0.8 + 0.3 * Solids.hash(i * 31 + j, 71));
	}

	private float fadeOut(float a) {
		return (float) Cinematics.clamp01((lifetime - a) / 100.0);
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age < breakAt) {
			ClientFx.tintSky(0x2A1030, 0.4F * Effects.smooth(age / 30.0F));
		} else {
			ClientFx.tintSky(0x0A0414, Math.min(0.95F, (age - breakAt) / 40.0F) * fadeOut(age));
			if (age == breakAt) {
				ClientFx.light(c.add(0, radius * 0.8, 0), 1.0F, 0xFF80FF);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d cc = c.subtract(cam);
		double crackW = Math.max(0.6, radius * 0.004);

		if (a < breakAt) {
			// Cracks running down the dome along the pane seams, flickering.
			int flick = (int) (a * 0.7F);
			for (int i = LAT - 1; i >= 0; i--) {
				for (int j = 0; j < LON; j++) {
					double t = a - crackAt(i, j);
					if (t < 0) {
						continue;
					}
					float al = (float) Math.min(1.0, t / 10.0) * (Solids.hash(flick, i * 31 + j) > 0.15 ? 0.85F : 0.35F);
					double e0 = elev(i), e1 = elev(i + 1);
					double a0 = j * MathHelper.TAU / LON, a1 = (j + 1) * MathHelper.TAU / LON;
					Vec3d p00 = cc.add(dome(e0, a0).multiply(radius)), p01 = cc.add(dome(e0, a1).multiply(radius));
					Vec3d p10 = cc.add(dome(e1, a0).multiply(radius));
					FxDraw.ribbon(glow, m, p00, p01, Vec3d.ZERO, crackW, 0xFF60FF, al, al);
					FxDraw.ribbon(glow, m, p00, p10, Vec3d.ZERO, crackW, 0xFF60FF, al, al);
					if (Solids.hash(i * 31 + j, 72) < 0.35) {
						// A diagonal fracture across the pane.
						Vec3d p11 = cc.add(dome(e1, a1).multiply(radius));
						FxDraw.ribbon(glow, m, p00, p11, Vec3d.ZERO, crackW * 0.7, 0xFFC0FF, al * 0.7F, al * 0.7F);
					}
				}
			}
			return;
		}

		double e = a - breakAt;
		float out = fadeOut(a);
		// Behind the sky: nothing but stars and nebulae.
		float cosmos = Effects.smooth((float) e / 40.0F) * out;
		Solids.stars(glow, m, cb, a, cosmos);
		Solids.nebula(glow, m, cb, 7, 0xFF40C0, 0x6040FF, cosmos);

		// The panes falling away, tumbling, their broken edges still glowing.
		if (e < 150) {
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			float al = (float) (1 - e / 150.0);
			for (int i = 0; i < LAT; i++) {
				for (int j = 0; j < LON; j++) {
					int id = i * 31 + j;
					double e0 = elev(i), e1 = elev(i + 1);
					double a0 = j * MathHelper.TAU / LON, a1 = (j + 1) * MathHelper.TAU / LON;
					Vec3d[] q = {dome(e0, a0), dome(e0, a1), dome(e1, a1), dome(e1, a0)};
					Vec3d mid = dome((e0 + e1) * 0.5, (a0 + a1) * 0.5);
					double speed = 0.8 * (1 + Solids.hash(id, 73));
					Vec3d shift = mid.multiply(e * speed).add(0, e * 0.3 - e * e * 0.004, 0);
					Vec3d axis = Effects.dir(id, 74);
					double ang = e * 0.03 * (Solids.hash(id, 75) * 2 - 1);
					Vec3d centre = cc.add(mid.multiply(radius)).add(shift);
					Vec3d[] w = new Vec3d[4];
					for (int k = 0; k < 4; k++) {
						Vec3d v = q[k].multiply(radius).subtract(mid.multiply(radius)).multiply(0.96);
						w[k] = centre.add(rotate(v, axis, ang));
					}
					int sky = Effects.fade(0x9AB8E8, 0x4A78D0, (float) ((e0 + e1) * 0.5 / (Math.PI / 2)));
					Solids.quad(shade, m, w[0], w[1], w[2], w[3], sky, 0.9F * al);
					for (int k = 0; k < 4; k++) {
						FxDraw.ribbon(glow, m, w[k], w[(k + 1) % 4], Vec3d.ZERO, crackW, 0xFF60FF, 0.6F * al, 0.6F * al);
					}
				}
			}
		}
	}

	private static Vec3d rotate(Vec3d v, Vec3d k, double ang) {
		double cos = Math.cos(ang), sin = Math.sin(ang);
		return v.multiply(cos).add(k.crossProduct(v).multiply(sin)).add(k.multiply(k.dotProduct(v) * (1 - cos)));
	}
}
