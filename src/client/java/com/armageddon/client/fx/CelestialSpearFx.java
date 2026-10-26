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
 * Godfall: one golden spear thrown from a gate in the sky. It accelerates down with a burning trail, strikes
 * exactly when the server plants its real blocks, and leaves a ring of gold dust and a fading glow along the
 * shaft. The great final spear is wider, ringed with halos, and lands with a much larger ring.
 */
public class CelestialSpearFx extends FxEffect {
	private final Vec3d start, target, dir;
	private final double len;
	private final boolean big;
	private final int fall;

	public CelestialSpearFx(FxPayload p) {
		super(Math.max(4, p.duration()) + (p.p1() > 0.5F ? 90 : 40));
		this.start = new Vec3d(p.x(), p.y(), p.z());
		this.target = new Vec3d(p.x2(), p.y2(), p.z2());
		Vec3d d = target.subtract(start);
		this.dir = d.lengthSquared() < 1.0E-6 ? new Vec3d(0, -1, 0) : d.normalize();
		this.len = Math.max(4, p.scale());
		this.big = p.p1() > 0.5F;
		this.fall = Math.max(4, p.duration());
	}

	private Vec3d tip(double a) {
		double t = Cinematics.clamp01(a / fall);
		return start.lerp(target, Math.pow(t, 1.7));
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age < fall) {
			ClientFx.light(tip(age), big ? 1.2F : 0.6F, 0xFFD070);
		} else if (age == fall) {
			ClientFx.shake(target, big ? 200 : 60, big ? 1.2F : 0.35F);
			ClientFx.light(target.add(0, 3, 0), big ? 1.4F : 0.7F, 0xFFE0A0);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		double rr = big ? 1.4 : 0.45;
		Vec3d[] basis = ArsenalEffects.basis(dir);

		if (a < 8) {
			// The gate flares as the spear leaves it.
			float k = 1 - a / 8.0F;
			Solids.flare(glow, m, start.subtract(cam), cb, (big ? 18 : 6) * k + 1, 0xFFE8A0, k);
		}
		if (a < fall) {
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d tip = tip(a).subtract(cam);
			Vec3d neck = tip.subtract(dir.multiply(len * 0.14));
			Vec3d tail = tip.subtract(dir.multiply(len));
			int sides = Effects.lod(tip, len, 5, 10);
			Solids.tube(solid, m, tail, neck, rr, rr, sides, 0xC8902A, 1.0F, true);
			Solids.tube(solid, m, neck, tip, rr * 1.9, 0.0, sides, 0xFFE070, 1.0F, true);
			Solids.tube(solid, m, tail, tail.add(dir.multiply(len * 0.06)), rr * 1.7, rr * 1.2, sides, 0xFFD050, 1.0F, true);
			Solids.tube(glow, m, tail, tip, rr * 2.4, rr * 1.3, sides, 0xFFD060, 0.25F, false);
			FxDraw.ribbon(glow, m, tail, tail.subtract(dir.multiply(len * (big ? 4.0 : 2.5))), Vec3d.ZERO, rr * 3.0, 0xFFE8A0, 0.45F, 0.0F);
			Solids.flare(glow, m, tip, cb, rr * 4 + 1, 0xFFF0C0, 0.8F);
			if (big) {
				for (int k = 0; k < 3; k++) {
					Vec3d c = tail.add(dir.multiply(len * (0.25 + 0.25 * k)));
					double spin = a * 0.2 + k;
					Vec3d u = basis[0].multiply(Math.cos(spin)).add(basis[1].multiply(Math.sin(spin)));
					Vec3d v = basis[2].crossProduct(u);
					FxDraw.ring(glow, m, c, u, v, rr * 3.0, rr * 3.6, 0xFFE8A0, 0.0F, 0.6F, 0.0F, 32);
				}
			}
			return;
		}

		// --- after the strike: a ring of gold dust, a glow down the standing shaft, dust thrown up.
		double e = a - fall;
		double span = big ? 34.0 : 16.0;
		if (e < span + 10) {
			Vec3d g = target.subtract(cam);
			double rad = (big ? 70 : 11) * Cinematics.ease(e / span);
			float al = (float) Math.max(0, 1 - e / (span + 10));
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			int segs = Effects.lod(g, rad + 4, 24, 72);
			FxDraw.ring(glow, m, g.add(0, 0.4, 0), x, z, Math.max(0, rad - (big ? 6 : 1.5)), rad + 0.6, 0xFFD070, 0.0F, 0.8F * al, 0.0F, segs);
			Solids.dot(glow, m, g.add(0, 1, 0), cb, (big ? 14 : 4) * al + 0.5, 0xFFF4D0, al);
			FxKit.groundShock(consumers, m, g, rad, 0xFFD070, 0.9F * al);
			float fe = (float) Math.min(1.0, e / (big ? 26.0 : 14.0));
			if (fe < 1) {
				FxKit.fire(consumers, m, cb, g.add(0, big ? 5 : 1.5, 0), big ? 14 : 3.5, 0xFFE8A0, fe, 1.0F, a, 61);
				FxKit.sparks(consumers, m, g.add(0, 1, 0), big ? 60 : 14, big ? 70 : 24, 0xFFD070, fe, 1.0F, 62);
			}
			if (e < 40) {
				java.util.List<Effects.Puff> puffs = FxKit.puffs();
				FxKit.dustRing(m, cb, g, rad * 0.85, (big ? 9 : 3) + e * 0.15, 0xE8D0A0, 0x7A6248, 0.6F * (float) (1 - e / 40.0), 51, puffs);
				if (big) {
					FxKit.column(g, 20 + e * 1.2, 10 + e * 0.2, a, 0xD8C8A8, 0x6A5844, 0.45F * (float) (1 - e / 40.0), 52, puffs);
				}
				FxKit.flush(puffs, consumers, m, cb);
			}
		}
		float shaft = (float) Math.max(0, 1 - e / (big ? 80.0 : 30.0));
		if (shaft > 0.01F) {
			Vec3d g = target.subtract(cam);
			// The great spear ends up upright in the altar, which sits ten blocks below where it struck.
			Vec3d top = big ? g.add(0, len - 10, 0) : g.subtract(dir.multiply(len));
			FxDraw.beam(glow, m, g, top, Vec3d.ZERO, rr * 1.6, 0xFFE8A0, 0.5F * shaft);
			Solids.flare(glow, m, top, cb, rr * 3 + 1, 0xFFE8A0, 0.6F * shaft);
		}
	}
}
