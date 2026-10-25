package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.TrumanSet;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * What is left of THE TRUMAN SHOW set once the cutscene ends: the bare steel ribs of the sky above the
 * painted wall, an ON AIR sign over the door, and the studio lights still sweeping and burning the floor.
 * a = set centre (ground), b = heading; starts at event tick {@code CINE - 4}.
 */
public class TrumanFx extends FxEffect {
	private static final int START = TrumanSet.CINE - 4;
	private static final int RIBS = 16;
	private static final double TOP = TrumanSet.DOME_H + 10;
	private final Vec3d g, fwd, right, base;

	public TrumanFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.g = new Vec3d(p.x(), p.y(), p.z());
		Vec3d h = new Vec3d(p.x2(), 0, p.z2());
		this.fwd = h.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : h.normalize();
		this.right = new Vec3d(-fwd.z, 0, fwd.x);
		this.base = new Vec3d(g.x, Math.floor(g.y), g.z);
	}

	/** A point on the bare rig: from the top of the painted wall ({@code phi} = 0) arching up to the zenith. */
	private static Vec3d ribPt(Vec3d c, double az, double phi) {
		double r = TrumanSet.WALL_R * Math.cos(phi), h = TrumanSet.WALL_H + (TOP - TrumanSet.WALL_H) * Math.sin(phi);
		return c.add(Math.cos(az) * r, h, Math.sin(az) * r);
	}

	private float eventTime(float td) {
		return START + age + td;
	}

	@Override
	public boolean tick(ClientWorld world) {
		float t = START + age;
		if (t < TrumanSet.SCORCH_END && age % 3 == 0) {
			for (int i = 0; i < TrumanSet.LAMPS; i++) {
				Vec3d s = TrumanSet.spot(g, fwd, i, t);
				double y = TrumanShowCine.groundY(s.x, s.z);
				world.addParticle(ParticleTypes.LARGE_SMOKE, s.x, y + 0.5, s.z, 0, 0.08, 0);
				world.addParticle(ParticleTypes.FLAME, s.x + world.random.nextGaussian() * 0.6, y + 0.3, s.z + world.random.nextGaussian() * 0.6, 0, 0.05, 0);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float t = eventTime(td);
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d c = base.subtract(cam);
		// Bare ribs of the sky, above the painted wall. Some broke off and hang loose.
		for (int k = 0; k < RIBS; k++) {
			double az = TrumanSet.bearing(fwd) + (k + 0.5) * MathHelper.TAU / RIBS;
			double reach = k % 3 == 0 ? 1.0 : 0.3 + 0.5 * Solids.hash(k, 17);
			double end = Math.PI / 2 * reach;
			Vec3d prev = ribPt(c, az, 0);
			int segs = 8;
			for (int s = 1; s <= segs; s++) {
				Vec3d p = ribPt(c, az, end * s / segs);
				Solids.tube(solid, m, prev, p, 0.7, 0.7, 6, 0x3A3E46, 1.0F, true);
				prev = p;
			}
			if (reach < 1.0) {
				// A torn cable, swaying.
				Vec3d sway = new Vec3d(Math.sin(t * 0.05 + k) * 1.5, -14, Math.cos(t * 0.04 + k) * 1.5);
				FxDraw.ribbon(solid, m, prev, prev.add(sway), Vec3d.ZERO, 0.3, 0x1E1E22, 1.0F, 1.0F);
				Solids.quad(solid, m, prev.add(sway).add(-2.5, 0, 0), prev.add(sway).add(2.5, 0, 0), prev.add(sway).add(2.5, -4, 0.6),
						prev.add(sway).add(-2.5, -4, 0.6), TrumanShowCine.paint(az, 0.3 + end * 0.6), 1.0F);
			}
		}
		// Rig work lights blinking along the broken ribs.
		for (int k = 0; k < RIBS; k++) {
			double az = TrumanSet.bearing(fwd) + (k + 0.5) * MathHelper.TAU / RIBS;
			double reach = k % 3 == 0 ? 1.0 : 0.3 + 0.5 * Solids.hash(k, 17);
			Vec3d tip = ribPt(c, az, Math.PI / 2 * reach);
			float on = ((int) (t * 0.08F + k) & 3) == 0 ? 1.0F : 0.25F;
			Solids.dot(glow, m, tip, cb, 1.6, 0xFF4030, on);
		}
		// Ring girders.
		for (double phi : new double[]{0.0, 0.55}) {
			Vec3d prev = null;
			for (int s = 0; s <= 48; s++) {
				Vec3d p = ribPt(c, s * MathHelper.TAU / 48, phi);
				if (prev != null) {
					Solids.tube(solid, m, prev, p, 0.6, 0.6, 5, 0x3A3E46, 1.0F, true);
				}
				prev = p;
			}
		}
		// ON AIR, hung over the door.
		float blink = ((int) (t * 0.12F) & 1) == 0 ? 1.0F : 0.3F;
		double hs = TrumanSet.WALL_H + 8;
		String sign = "ON AIR";
		double px = 1.4;
		Vec3d sc = c.add(fwd.multiply(TrumanSet.WALL_R - 3)).add(0, hs, 0);
		Vec3d tl = sc.subtract(right.multiply(TrumanSet.width(sign) * px / 2));
		TrumanShowCine.box(solid, m, tl.add(right.multiply(-1.2)).add(0, 1.2, 0).add(fwd.multiply(0.2)), right.multiply(TrumanSet.width(sign) * px + 2.4),
				new Vec3d(0, -(5 * px + 2.4), 0), fwd.multiply(0.6), 0x16161A);
		TrumanShowCine.pixelText(glow, m, tl, right, new Vec3d(0, -1, 0), px, sign, 0xFF2A2A, blink);
		FxDraw.ribbon(solid, m, sc.add(0, 1.2, 0), ribPt(c, TrumanSet.bearing(fwd), 0.06), Vec3d.ZERO, 0.2, 0x1E1E22, 1.0F, 1.0F);
		// The studio lights keep burning the floor until they are switched off.
		float off = (float) MathHelper.clamp((TrumanSet.SCORCH_END - t) / 20.0, 0.0, 1.0);
		float life = (float) MathHelper.clamp((lifetime - age - td) / 30.0, 0.0, 1.0);
		for (int i = 0; i < TrumanSet.LAMPS; i++) {
			Vec3d lamp = TrumanSet.lamp(g, fwd, i);
			Vec3d spot = TrumanSet.spot(g, fwd, i, t);
			Vec3d hit = new Vec3d(spot.x, TrumanShowCine.groundY(spot.x, spot.z), spot.z);
			Vec3d dir = hit.subtract(lamp);
			Vec3d lc = lamp.subtract(cam);
			TrumanShowCine.studioLamp(solid, glow, m, cb, lc, dir, 2.4, off * life, null);
			FxDraw.ribbon(solid, m, lc.add(0, 6, 0), lc.add(0, 20, 0), Vec3d.ZERO, 0.25, 0x2A2A2E, 1.0F, 1.0F);
			if (off * life > 0.01F) {
				TrumanShowCine.lampBeam(glow, m, lc.add(dir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, off * life, t);
				TrumanShowCine.lampMotes(glow, m, cb, lc.add(dir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, off * life, t + i * 40);
				// Heat shimmer and embers rising off the scorch line.
				Vec3d h = hit.subtract(cam);
				for (int e = 0; e < 8; e++) {
					double u = (Solids.hash(e, i) + t * 0.02) % 1.0;
					Vec3d ep = h.add((Solids.hash(e + 9, i) - 0.5) * 6, u * 9, (Solids.hash(e + 19, i) - 0.5) * 6);
					Solids.dot(glow, m, ep, cb, 0.25, e % 2 == 0 ? 0xFF9030 : 0xFFD070, (float) (1 - u) * off * life);
				}
			}
		}
	}
}
