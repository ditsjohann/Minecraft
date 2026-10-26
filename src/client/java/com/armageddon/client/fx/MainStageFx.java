package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.MainStageSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/**
 * The MAIN STAGE rig, live from the moment the bomb is lit: steel ribs and ring girders lowering into the sky over
 * the set, work lights blinking along them, an ON AIR sign over the EXIT door, and four big studio lamps sweeping
 * the floor and burning it. a = set centre (ground), b = heading; its age runs with the server event.
 */
public class MainStageFx extends FxEffect {
	private static final int RIBS = 16;
	private static final double TOP = MainStageSet.DOME_H + 10;
	private final Vec3d g, fwd, right, base;

	public MainStageFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.g = new Vec3d(p.x(), p.y(), p.z());
		Vec3d h = new Vec3d(p.x2(), 0, p.z2());
		this.fwd = h.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : h.normalize();
		this.right = new Vec3d(-fwd.z, 0, fwd.x);
		this.base = new Vec3d(g.x, Math.floor(g.y), g.z);
	}

	static double groundY(double x, double z) {
		ClientWorld w = com.armageddon.client.render.FxDraw.world();
		return w == null ? 64 : w.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
	}

	/** How far the rig still has to come down: it lowers out of the sky over the first few seconds. */
	private static double drop(float t) {
		double u = MathHelper.clamp(t / MainStageSet.RIG_DOWN, 0.0, 1.0);
		double w = Math.max(0, t - MainStageSet.WRAP - 10);
		// After the wrap the whole rig falls in on the set.
		return 140 * (1 - u) * (1 - u) - w * w * 0.06;
	}

	/** A point on the rig: from the top of the painted wall ({@code phi} = 0) arching up to the zenith. */
	private static Vec3d ribPt(Vec3d c, double az, double phi, double drop) {
		double r = MainStageSet.WALL_R * Math.cos(phi), h = MainStageSet.WALL_H + (TOP - MainStageSet.WALL_H) * Math.sin(phi) + drop;
		return c.add(Math.cos(az) * r, h, Math.sin(az) * r);
	}

	@Override
	public boolean tick(ClientWorld world) {
		float t = age;
		if (t >= MainStageSet.SCORCH && t < MainStageSet.SCORCH_END && age % 3 == 0) {
			for (int i = 0; i < MainStageSet.LAMPS; i++) {
				Vec3d s = MainStageSet.spot(g, fwd, i, t);
				double y = groundY(s.x, s.z);
				world.addParticle(ParticleTypes.LARGE_SMOKE, s.x, y + 0.5, s.z, 0, 0.08, 0);
				world.addParticle(ParticleTypes.FLAME, s.x + world.random.nextGaussian() * 0.6, y + 0.3, s.z + world.random.nextGaussian() * 0.6, 0, 0.05, 0);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float t = age + td;
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d c = base.subtract(cam);
		double drop = drop(t);
		// Steel ribs arching over the set, lowering into place. A few are short, with cables swaying off their ends.
		for (int k = 0; k < RIBS; k++) {
			double az = MainStageSet.bearing(fwd) + (k + 0.5) * MathHelper.TAU / RIBS;
			double reach = k % 3 == 0 ? 1.0 : 0.3 + 0.5 * Solids.hash(k, 17);
			double end = Math.PI / 2 * reach;
			Vec3d prev = ribPt(c, az, 0, drop);
			int segs = 8;
			for (int s = 1; s <= segs; s++) {
				Vec3d p = ribPt(c, az, end * s / segs, drop);
				Solids.tube(solid, m, prev, p, 0.7, 0.7, 6, 0x3A3E46, 1.0F, true);
				prev = p;
			}
			if (reach < 1.0) {
				Vec3d sway = new Vec3d(Math.sin(t * 0.05 + k) * 1.5, -14, Math.cos(t * 0.04 + k) * 1.5);
				FxDraw.ribbon(solid, m, prev, prev.add(sway), Vec3d.ZERO, 0.3, 0x1E1E22, 1.0F, 1.0F);
				box(solid, m, prev.add(sway).add(-1.2, -2.4, -1.2), new Vec3d(2.4, 0, 0), new Vec3d(0, 2.4, 0), new Vec3d(0, 0, 2.4), 0x2A2A30);
			}
			float on = ((int) (t * 0.08F + k) & 3) == 0 ? 1.0F : 0.25F;
			Solids.dot(glow, m, prev, cb, 1.6, 0xFF4030, on);
		}
		for (double phi : new double[]{0.0, 0.55}) {
			Vec3d prev = null;
			for (int s = 0; s <= 48; s++) {
				Vec3d p = ribPt(c, s * MathHelper.TAU / 48, phi, drop);
				if (prev != null) {
					Solids.tube(solid, m, prev, p, 0.6, 0.6, 5, 0x3A3E46, 1.0F, true);
				}
				prev = p;
			}
		}
		// ON AIR, hung over the EXIT door.
		float blink = ((int) (t * 0.12F) & 1) == 0 ? 1.0F : 0.3F;
		double hs = MainStageSet.WALL_H + 8 + drop;
		String sign = "ON AIR";
		double px = 1.4;
		Vec3d sc = c.add(fwd.multiply(MainStageSet.WALL_R - 3)).add(0, hs, 0);
		Vec3d tl = sc.subtract(right.multiply(MainStageSet.width(sign) * px / 2));
		box(solid, m, tl.add(right.multiply(-1.2)).add(0, 1.2, 0).add(fwd.multiply(0.2)), right.multiply(MainStageSet.width(sign) * px + 2.4),
				new Vec3d(0, -(5 * px + 2.4), 0), fwd.multiply(0.6), 0x16161A);
		pixelText(glow, m, tl, right, new Vec3d(0, -1, 0), px, sign, 0xFF2A2A, blink);
		// The studio lamps, sweeping and burning the floor until they are switched off.
		float off = (float) MathHelper.clamp((MainStageSet.SCORCH_END - t) / 20.0, 0.0, 1.0);
		float life = (float) MathHelper.clamp((lifetime - age - td) / 30.0, 0.0, 1.0);
		for (int i = 0; i < MainStageSet.LAMPS; i++) {
			float lit = t >= MainStageSet.lampOn(i) ? 1.0F : 0.0F;
			Vec3d lamp = MainStageSet.lamp(g, fwd, i).add(0, drop, 0);
			if (t >= MainStageSet.WRAP) {
				// Cut loose: the lamp drops straight down and is gone in its own explosion.
				double f = (t - MainStageSet.WRAP) / MainStageSet.LAMP_FALL;
				if (f >= 1) {
					continue;
				}
				Vec3d top = MainStageSet.lamp(g, fwd, i);
				lamp = top.add(0, -(top.y - groundY(top.x, top.z)) * f * f, 0);
			}
			Vec3d spot = MainStageSet.spot(g, fwd, i, Math.max(t, MainStageSet.LAMPS_ON));
			Vec3d hit = new Vec3d(spot.x, groundY(spot.x, spot.z), spot.z);
			Vec3d dir = hit.subtract(lamp);
			Vec3d lc = lamp.subtract(cam);
			studioLamp(solid, glow, m, cb, lc, dir, 2.4, off * life * lit);
			FxDraw.ribbon(solid, m, lc.add(0, 6, 0), lc.add(0, 20, 0), Vec3d.ZERO, 0.25, 0x2A2A2E, 1.0F, 1.0F);
			if (off * life * lit > 0.01F) {
				float k = off * life * lit;
				lampBeam(glow, m, lc.add(dir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, k, t);
				lampMotes(glow, m, cb, lc.add(dir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, k, t + i * 40);
				Vec3d h = hit.subtract(cam);
				for (int e = 0; e < 8; e++) {
					double u = (Solids.hash(e, i) + t * 0.02) % 1.0;
					Vec3d ep = h.add((Solids.hash(e + 9, i) - 0.5) * 6, u * 9, (Solids.hash(e + 19, i) - 0.5) * 6);
					Solids.dot(glow, m, ep, cb, 0.25, e % 2 == 0 ? 0xFF9030 : 0xFFD070, (float) (1 - u) * k);
				}
			}
		}
	}

	// ======================================================================== set pieces

	/** Lit parallelepiped from {@code o} spanning u, v, w. */
	static void box(VertexConsumer vc, Matrix4f m, Vec3d o, Vec3d u, Vec3d v, Vec3d w, int rgb) {
		Vec3d ou = o.add(u), ov = o.add(v), ow = o.add(w), ouv = ou.add(v), ouw = ou.add(w), ovw = ov.add(w), ouvw = ouv.add(w);
		face(vc, m, o, ou, ouv, ov, w.multiply(-1), rgb);
		face(vc, m, ow, ouw, ouvw, ovw, w, rgb);
		face(vc, m, o, ow, ovw, ov, u.multiply(-1), rgb);
		face(vc, m, ou, ouw, ouvw, ouv, u, rgb);
		face(vc, m, o, ou, ouw, ow, v.multiply(-1), rgb);
		face(vc, m, ov, ouv, ouvw, ovw, v, rgb);
	}

	private static void face(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d c, Vec3d d, Vec3d n, int rgb) {
		Solids.quad(vc, m, a, b, c, d, Solids.scale(rgb, Solids.light(n.lengthSquared() < 1.0E-9 ? new Vec3d(0, 1, 0) : n.normalize())), 1.0F);
	}

	/** Block-pixel text (the shared 3x5 font) laid out from {@code topLeft} along {@code right}/{@code down}. */
	static void pixelText(VertexConsumer vc, Matrix4f m, Vec3d topLeft, Vec3d right, Vec3d down, double px, String s, int rgb, float alpha) {
		Vec3d r = right.normalize().multiply(px), d = down.normalize().multiply(px);
		Vec3d rs = r.multiply(0.9), ds = d.multiply(0.9);
		for (int ci = 0; ci < s.length(); ci++) {
			for (int col = 0; col < 3; col++) {
				for (int row = 0; row < 5; row++) {
					if (!MainStageSet.pixel(s.charAt(ci), col, row)) {
						continue;
					}
					Vec3d p = topLeft.add(r.multiply(ci * 4 + col)).add(d.multiply(row));
					Solids.quad(vc, m, p, p.add(rs), p.add(rs).add(ds), p.add(ds), rgb, alpha);
				}
			}
		}
	}

	/** A studio lamp: housing, fins, barn doors, yoke and lens, aimed along {@code dir}. */
	static void studioLamp(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d p, Vec3d dir, double s, float on) {
		Vec3d d = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0, -1, 0) : dir.normalize();
		Vec3d[] b = ArsenalEffects.basis(d);
		Vec3d back = p.subtract(d.multiply(1.6 * s)), front = p.add(d.multiply(1.6 * s));
		Solids.tube(solid, m, back, front, 1.25 * s, 1.35 * s, 16, 0x1C1C20, 1.0F, true);
		Solids.tube(solid, m, back.subtract(d.multiply(0.6 * s)), back, 0.8 * s, 1.25 * s, 16, 0x111114, 1.0F, true);
		for (int k = 0; k < 4; k++) {
			Vec3d q = back.add(d.multiply((0.3 + 0.55 * k) * s));
			Solids.tube(solid, m, q, q.add(d.multiply(0.14 * s)), 1.48 * s, 1.48 * s, 16, 0x2A2A30, 1.0F, true);
		}
		for (int f = 0; f < 4; f++) {
			double ang = f * Math.PI / 2;
			Vec3d e = b[0].multiply(Math.cos(ang)).add(b[1].multiply(Math.sin(ang)));
			Vec3d side = b[0].multiply(-Math.sin(ang)).add(b[1].multiply(Math.cos(ang))).multiply(1.25 * s);
			Vec3d root = front.add(e.multiply(1.35 * s));
			Vec3d tip = root.add(d.multiply(1.0 * s)).add(e.multiply(0.55 * s));
			Solids.quad(solid, m, root.subtract(side), root.add(side), tip.add(side), tip.subtract(side), 0x18181C, 1.0F);
		}
		Vec3d arm = b[0].multiply(1.75 * s);
		Vec3d top = p.add(0, 2.6 * s, 0);
		Solids.tube(solid, m, p.add(arm), top.add(arm), 0.18 * s, 0.18 * s, 6, 0x3A3A40, 1.0F, true);
		Solids.tube(solid, m, p.subtract(arm), top.subtract(arm), 0.18 * s, 0.18 * s, 6, 0x3A3A40, 1.0F, true);
		Solids.tube(solid, m, top.add(arm), top.subtract(arm), 0.2 * s, 0.2 * s, 6, 0x3A3A40, 1.0F, true);
		Vec3d lens = front.add(d.multiply(0.02 * s));
		FxDraw.disc(solid, m, lens, b[0], b[1], 1.15 * s, on > 0.05F ? 0xFFF2DC : 0x2A2E36, 1.0F, 1.0F, 20);
		if (on > 0.01F) {
			FxDraw.disc(glow, m, lens.add(d.multiply(0.05)), b[0], b[1], 1.6 * s, 0xFFF0D0, on, 0.0F, 20);
			Solids.dot(glow, m, lens.add(d.multiply(0.3 * s)), cb, 4.5 * s, 0xFFE6B8, 0.35F * on);
		}
	}

	/** A volumetric lamp beam from {@code lens} to {@code hit}, with a hot scorching spot. */
	static void lampBeam(VertexConsumer glow, Matrix4f m, Vec3d lens, Vec3d hit, double s, float alpha, float time) {
		Solids.tube(glow, m, lens, hit, 1.1 * s, 5.0 * s, 16, 0xFFF0D0, 0.09F * alpha, false);
		Solids.tube(glow, m, lens, hit, 0.5 * s, 1.8 * s, 12, 0xFFFFFF, 0.12F * alpha, false);
		Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
		FxDraw.disc(glow, m, hit.add(0, 0.3, 0), x, z, 5.5 * s, 0xFFF0C8, 0.6F * alpha, 0.0F, 24);
		float flick = 0.7F + 0.3F * MathHelper.sin(time * 1.3F);
		FxDraw.ring(glow, m, hit.add(0, 0.35, 0), x, z, 1.5 * s, 3.2 * s, 0xFF7020, 0.0F, 0.8F * alpha * flick, 0.0F, 24);
	}

	/** Dust hanging in a lamp's beam, drifting slowly through the light. */
	static void lampMotes(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d lens, Vec3d hit, double s, float alpha, float time) {
		if (alpha <= 0.02F || lens.add(hit).multiply(0.5).lengthSquared() > 200 * 200) {
			return;
		}
		Vec3d axis = hit.subtract(lens);
		Vec3d[] b = ArsenalEffects.basis(axis.normalize());
		for (int i = 0; i < 40; i++) {
			double u = (Solids.hash(i, 91) + time * 0.002 * (1 + Solids.hash(i, 92))) % 1.0;
			double r = (1.1 + 3.6 * u) * s * Math.sqrt(Solids.hash(i, 93));
			double ang = Solids.hash(i, 94) * MathHelper.TAU + time * 0.01;
			Vec3d p = lens.add(axis.multiply(u)).add(b[0].multiply(Math.cos(ang) * r)).add(b[1].multiply(Math.sin(ang) * r));
			float tw = 0.5F + 0.5F * MathHelper.sin(time * 0.2F + i);
			Solids.dot(glow, m, p, cb, 0.12 * s + 0.05, 0xFFF6E0, 0.8F * alpha * tw);
		}
	}
}
