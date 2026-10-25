package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.util.TrumanSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/**
 * THE TRUMAN SHOW: a live broadcast of the player's world; a studio light falls out of the sky; the sky
 * is a painted dome with a door in it; outside is a studio, a control room and walls of monitors; the
 * transmission is cut, the set is struck and the lights burn the ground; a bow, and THE END.
 */
public class TrumanShowCine extends Cinematics.Cine {
	// Scene boundaries (event ticks). The server runs the real strike on the same clock (see TrumanSet).
	static final int OPEN_END = 110, CRANE = 205, GLIDE = 262, STUDIO = 335, WIDE = 392, INSIDE_AERIAL = 540, FINALE = 600;
	static final int LAT = 10, LON = 64, KEEP_ROWS = 4;
	static final int SEAM = 0x2C323A;
	private final Vec3d base, studio, crash;
	private final int holeJ;

	public TrumanShowCine(Vec3d anchor, Vec3d heading, int life) {
		super(anchor, heading, life);
		this.base = new Vec3d(anchor.x, Math.floor(anchor.y), anchor.z);
		this.studio = base.add(0, 3000, 0);
		Vec3d c = TrumanSet.crash(anchor, fwd);
		this.crash = new Vec3d(c.x, groundY(c.x, c.z), c.z);
		double az = Math.atan2(crash.z - base.z, crash.x - base.x);
		this.holeJ = Math.floorMod((int) Math.floor(az / MathHelper.TAU * LON), LON);
	}

	// ======================================================================== shared set pieces

	static double groundY(double x, double z) {
		ClientWorld w = MinecraftClient.getInstance().world;
		return w == null ? 64 : w.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
	}

	/** Point on the dome (centre {@code c} at ground level) at azimuth/elevation, scaled radially by {@code k}. */
	static Vec3d domePt(Vec3d c, double az, double el, double k) {
		double ce = Math.cos(el);
		return c.add(Math.cos(az) * ce * TrumanSet.DOME_R * k, Math.sin(el) * TrumanSet.DOME_H * k, Math.sin(az) * ce * TrumanSet.DOME_R * k);
	}

	/** Horizontal radius of the dome at height {@code h}. */
	static double domeRadiusAt(double h) {
		double q = MathHelper.clamp(h / TrumanSet.DOME_H, 0.0, 1.0);
		return TrumanSet.DOME_R * Math.sqrt(1 - q * q);
	}

	/** The painted sky: posterised gradient (brush strokes) with soft painted clouds. */
	static int paint(double az, double el) {
		double k = Math.floor(el / (Math.PI / 2) * 7) / 7.0;
		int sky = Effects.fade(0xB4DAF6, 0x3E80D6, (float) Math.pow(k, 0.8));
		// A warm, hazy band of painted morning along the horizon.
		if (el < 0.32) {
			sky = Effects.fade(0xFFE2C4, sky, (float) MathHelper.clamp(el / 0.32, 0.0, 1.0));
		}
		double n = TrumanSet.cloud(az, el);
		if (el < 1.25 && n > 0.45) {
			// Painted clouds: a cool shadow at the rim, bright cream in the middle.
			float body = (float) MathHelper.clamp((n - 0.45) / 0.15, 0.0, 1.0);
			int cloud = n < 0.52 ? 0xD6E2F2 : n > 0.62 ? 0xFFFDF6 : 0xF2F6FF;
			return Effects.fade(sky, cloud, body);
		}
		// Visible brush strokes: a slight per-stroke jitter in value.
		double stroke = Solids.hash((int) (az * 40), (int) (el * 60));
		return Solids.scale(sky, 0.97 + 0.06 * stroke);
	}

	/**
	 * Painted details on the inside of the sky: a sun with painted rays, a few flat painted birds.
	 * Drawn just inside the dome surface so they sit on the panels.
	 */
	static void paintedDetails(VertexConsumer solid, Matrix4f m, Vec3d c, Vec3d fwd, float a) {
		double sunAz = TrumanSet.bearing(fwd) + 0.55, sunEl = 0.62;
		Vec3d n = domePt(Vec3d.ZERO, sunAz, sunEl, 1).normalize();
		Vec3d sp = domePt(c, sunAz, sunEl, 0.992);
		Vec3d[] b = ArsenalEffects.basis(n);
		for (int k = 0; k < 16; k++) {
			double ang = k * MathHelper.TAU / 16;
			Vec3d dir = b[0].multiply(Math.cos(ang)).add(b[1].multiply(Math.sin(ang)));
			Vec3d side = b[0].multiply(-Math.sin(ang)).add(b[1].multiply(Math.cos(ang))).multiply(0.9);
			Vec3d r0 = sp.add(dir.multiply(11)), r1 = sp.add(dir.multiply(k % 2 == 0 ? 22 : 17));
			Solids.quad(solid, m, r0.subtract(side), r0.add(side), r1.add(side.multiply(0.3)), r1.subtract(side.multiply(0.3)), 0xFFEFB8, 1.0F);
		}
		FxDraw.disc(solid, m, sp.subtract(n.multiply(0.05)), b[0], b[1], 10.5, 0xFFF6D4, 1.0F, 1.0F, 28);
		FxDraw.disc(solid, m, sp.subtract(n.multiply(0.1)), b[0], b[1], 7.0, 0xFFFBEA, 1.0F, 1.0F, 24);
		// Birds: little painted "v"s.
		for (int k = 0; k < 7; k++) {
			double baz = sunAz - 0.9 + k * 0.23 + Solids.hash(k, 3) * 0.1, bel = 0.45 + Solids.hash(k, 4) * 0.4;
			Vec3d bn = domePt(Vec3d.ZERO, baz, bel, 1).normalize();
			Vec3d bp = domePt(c, baz, bel, 0.993);
			Vec3d[] bb = ArsenalEffects.basis(bn);
			double w = 1.6 + Solids.hash(k, 5);
			Vec3d l = bp.add(bb[0].multiply(-w)).add(bb[1].multiply(w * 0.45)), r = bp.add(bb[0].multiply(w)).add(bb[1].multiply(w * 0.45));
			Vec3d t = bb[1].multiply(0.22);
			Solids.quad(solid, m, l, bp, bp.add(t), l.add(t), 0x34404E, 1.0F);
			Solids.quad(solid, m, bp, r, r.add(t), bp.add(t), 0x34404E, 1.0F);
		}
	}

	/** When dome panel (i, j) comes loose: never for the bottom rows, the hole for SIRIUS, else during the strike. */
	static float fallAt(int i, int j, int holeJ) {
		if (i == LAT - 1 && j == holeJ) {
			return TrumanSet.LIGHT_FALL - 6;
		}
		if (i < KEEP_ROWS) {
			return Float.MAX_VALUE;
		}
		return TrumanSet.STRIKE + (LAT - 1 - i) * 10 + (float) Solids.hash(i, j * 7 + 3) * 45;
	}

	/**
	 * The dome. {@code inside}: painted sky on panels with dark seams, panels falling in after the strike.
	 * Otherwise the steel back of the set as seen from the studio.
	 */
	static void dome(VertexConsumer solid, Matrix4f m, Vec3d c, float a, int holeJ, boolean inside) {
		double dEl = Math.PI / 2 / LAT, dAz = MathHelper.TAU / LON;
		for (int i = 0; i < LAT; i++) {
			double el0 = i * dEl, el1 = (i + 1) * dEl;
			for (int j = 0; j < LON; j++) {
				double az0 = j * dAz, az1 = (j + 1) * dAz;
				float tf = inside ? fallAt(i, j, holeJ) : Float.MAX_VALUE;
				if (a >= tf) {
					float dt = a - tf;
					if (dt > 80) {
						continue;
					}
					// Loose panel: drops, swings inward at the top, and tumbles out of sight.
					Vec3d n = domePt(Vec3d.ZERO, (az0 + az1) / 2, (el0 + el1) / 2, 1).normalize();
					Vec3d drop = n.multiply(-dt * 0.35).add(0, -0.025 * dt * dt, 0);
					Vec3d tilt = n.multiply(-dt * 0.3);
					int col = paint((az0 + az1) / 2, (el0 + el1) / 2);
					Solids.quad(solid, m, domePt(c, az0, el0, 1).add(drop), domePt(c, az1, el0, 1).add(drop),
							domePt(c, az1, el1, 1).add(drop).add(tilt), domePt(c, az0, el1, 1).add(drop).add(tilt), Solids.scale(col, 0.85), 1.0F);
					continue;
				}
				// Backing shell: shows between panels as seams.
				Solids.quad(solid, m, domePt(c, az0, el0, 1.006), domePt(c, az1, el0, 1.006), domePt(c, az1, el1, 1.006), domePt(c, az0, el1, 1.006),
						inside ? SEAM : 0x22252B, 1.0F);
				double iAz = dAz * 0.035, iEl = dEl * 0.05;
				double pa0 = az0 + iAz, pa1 = az1 - iAz, pe0 = el0 + iEl, pe1 = Math.min(el1 - iEl, Math.PI / 2);
				for (int su = 0; su < 2; su++) {
					for (int sv = 0; sv < 2; sv++) {
						double a0 = lerp(pa0, pa1, su / 2.0), a1 = lerp(pa0, pa1, (su + 1) / 2.0);
						double e0 = lerp(pe0, pe1, sv / 2.0), e1 = lerp(pe0, pe1, (sv + 1) / 2.0);
						int col;
						if (inside) {
							col = paint((a0 + a1) / 2, (e0 + e1) / 2);
						} else {
							Vec3d n = domePt(Vec3d.ZERO, (a0 + a1) / 2, (e0 + e1) / 2, 1).normalize();
							col = Solids.scale((i + j) % 2 == 0 ? 0x6C727C : 0x646A73, Solids.light(n));
						}
						Solids.quad(solid, m, domePt(c, a0, e0, 1), domePt(c, a1, e0, 1), domePt(c, a1, e1, 1), domePt(c, a0, e1, 1), col, 1.0F);
					}
				}
			}
		}
	}

	private static double lerp(double x, double y, double t) {
		return x + (y - x) * t;
	}

	/** Centre of the door in the sky; {@code off} pushes it inside (-) or outside (+) the dome surface. */
	static Vec3d doorCentre(Vec3d c, Vec3d fwd, double off) {
		double h = TrumanSet.DOOR_H + 2.1;
		return c.add(fwd.multiply(domeRadiusAt(h) + off)).add(0, h, 0);
	}

	/** The door, its EXIT sign and (from inside) the white staircase up to it. */
	static void door(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d fwd, Vec3d right, boolean inside, float open) {
		double off = inside ? -0.35 : 0.35;
		double h0 = TrumanSet.DOOR_H, h1 = TrumanSet.DOOR_H + 4.2;
		Vec3d b0 = c.add(fwd.multiply(domeRadiusAt(h0) + off)).add(0, h0, 0), b1 = c.add(fwd.multiply(domeRadiusAt(h1) + off)).add(0, h1, 0);
		Vec3d sw = right.multiply(1.6);
		if (open < 1) {
			Solids.quad(solid, m, b0.subtract(sw), b0.add(sw), b1.add(sw), b1.subtract(sw), 0x14171C, 1.0F);
		}
		if (open > 0) {
			Vec3d in = fwd.multiply(inside ? -0.05 : 0.05);
			FxDraw.vertex(glow, m, b0.x - sw.x + in.x, b0.y + in.y, b0.z - sw.z + in.z, 0xFFF4E0, open);
			FxDraw.vertex(glow, m, b0.x + sw.x + in.x, b0.y + in.y, b0.z + sw.z + in.z, 0xFFF4E0, open);
			FxDraw.vertex(glow, m, b1.x + sw.x + in.x, b1.y + in.y, b1.z + sw.z + in.z, 0xFFF4E0, open);
			FxDraw.vertex(glow, m, b1.x - sw.x + in.x, b1.y + in.y, b1.z - sw.z + in.z, 0xFFF4E0, open);
			Vec3d mid = b0.add(b1).multiply(0.5);
			FxDraw.disc(glow, m, mid.add(in), right, new Vec3d(0, 1, 0), 9, 0xFFE8C8, 0.35F * open, 0.0F, 20);
		}
		// EXIT, in red, above the door.
		String sign = "EXIT";
		double px = 0.75;
		double hs = TrumanSet.DOOR_H + 9.6;
		Vec3d topLeft = c.add(fwd.multiply(domeRadiusAt(hs) + off * 1.4)).add(0, hs, 0).subtract(right.multiply(inside ? TrumanSet.width(sign) * px / 2 : -TrumanSet.width(sign) * px / 2));
		pixelText(glow, m, topLeft, right.multiply(inside ? 1 : -1), new Vec3d(0, -1, 0), px, sign, 0xFF3030, 0.95F);
		if (!inside) {
			return;
		}
		// Stairs climbing the inside of the sky from the left, like the set's own.
		for (int k = 0; k <= TrumanSet.DOOR_H + 2; k++) {
			double s = k <= 2 ? k - 1.0 : -1.5 - (k - 2);
			double top = k <= 2 ? h0 : h0 - (k - 2);
			double r = domeRadiusAt(Math.max(0, top)) - 0.6;
			Vec3d o = c.add(fwd.multiply(r - 2.4)).add(right.multiply(s - 0.5)).add(0, -3, 0);
			box(solid, m, o, right, new Vec3d(0, top + 3, 0), fwd.multiply(2.4), k <= 2 ? 0xF2F0EA : 0xE8E6DE);
		}
	}

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
					if (!TrumanSet.pixel(s.charAt(ci), col, row)) {
						continue;
					}
					Vec3d p = topLeft.add(r.multiply(ci * 4 + col)).add(d.multiply(row));
					Solids.quad(vc, m, p, p.add(rs), p.add(rs).add(ds), p.add(ds), rgb, alpha);
				}
			}
		}
	}

	/** A studio lamp: housing, fins, barn doors, yoke and lens, aimed along {@code dir}. */
	static void studioLamp(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d p, Vec3d dir, double s, float on, String label) {
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
		// Yoke.
		Vec3d arm = b[0].multiply(1.75 * s);
		Vec3d top = p.add(0, 2.6 * s, 0);
		Solids.tube(solid, m, p.add(arm), top.add(arm), 0.18 * s, 0.18 * s, 6, 0x3A3A40, 1.0F, true);
		Solids.tube(solid, m, p.subtract(arm), top.subtract(arm), 0.18 * s, 0.18 * s, 6, 0x3A3A40, 1.0F, true);
		Solids.tube(solid, m, top.add(arm), top.subtract(arm), 0.2 * s, 0.2 * s, 6, 0x3A3A40, 1.0F, true);
		// Lens: dark glass when off, blazing when on.
		Vec3d lens = front.add(d.multiply(0.02 * s));
		FxDraw.disc(solid, m, lens, b[0], b[1], 1.15 * s, on > 0.05F ? 0xFFF2DC : 0x2A2E36, 1.0F, 1.0F, 20);
		if (on > 0.01F) {
			FxDraw.disc(glow, m, lens.add(d.multiply(0.05)), b[0], b[1], 1.6 * s, 0xFFF0D0, on, 0.0F, 20);
			Solids.dot(glow, m, lens.add(d.multiply(0.3 * s)), cb, 4.5 * s, 0xFFE6B8, 0.35F * on);
		}
		if (label != null) {
			Vec3d sideN = b[1];
			Vec3d tl = p.add(sideN.multiply(1.37 * s)).subtract(d.multiply(1.2 * s)).add(b[0].multiply(0.45 * s));
			pixelText(glow, m, tl, d, b[0].multiply(-1), 0.1 * s, label, 0xFFFFFF, 0.8F);
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

	// ======================================================================== the falling light

	private Vec3d lampPos(float a) {
		double el = (LAT - 0.5) * (Math.PI / 2 / LAT);
		double az = (holeJ + 0.5) * MathHelper.TAU / LON;
		Vec3d hole = domePt(base, az, el, 0.97);
		double u = clamp((a - TrumanSet.LIGHT_FALL) / (double) (TrumanSet.LIGHT_HIT - TrumanSet.LIGHT_FALL));
		return Cinematics.lerp(hole, crash.add(0, 1.6, 0), u * u);
	}

	private static double clamp(double t) {
		return MathHelper.clamp(t, 0.0, 1.0);
	}

	// ======================================================================== camera

	@Override
	protected void direct(float a, Cinematics.State s) {
		Vec3d door = doorCentre(base, fwd, -0.35);
		if (a < OPEN_END) {
			// The broadcast: a quiet morning on the island.
			Vec3d p = anchor.subtract(fwd.multiply(16 - a * 0.05)).add(right.multiply(9)).add(0, 2.5 + a * 0.01, 0);
			Cinematics.aim(s, p, anchor.add(fwd.multiply(24)).add(0, 5 + a * 0.03, 0), 58, false);
		} else if (a < 150) {
			Cinematics.aim(s, anchor.subtract(fwd.multiply(6)).add(right.multiply(3)).add(0, 1.8, 0), lampPos(a), 64, false);
		} else if (a < TrumanSet.LIGHT_HIT) {
			Cinematics.aim(s, crash.subtract(fwd.multiply(20)).subtract(right.multiply(13)).add(0, 3.5, 0), lampPos(a), 60, false);
		} else if (a < CRANE) {
			double u = (a - TrumanSet.LIGHT_HIT) / (double) (CRANE - TrumanSet.LIGHT_HIT);
			Vec3d p = crash.subtract(fwd.multiply(lerp(13, 10, u))).subtract(right.multiply(lerp(9, 6, u))).add(0, 2.5 + u * 1.5, 0);
			Cinematics.aim(s, p, crash.add(0, 1, 0), 58, false);
		} else if (a < GLIDE) {
			// Crane up: something is wrong with the sky.
			double u = Cinematics.ease((a - CRANE) / (double) (GLIDE - CRANE));
			Vec3d p = anchor.subtract(fwd.multiply(8)).add(right.multiply(2)).add(0, lerp(2, 44, u), 0);
			Cinematics.aim(s, p, Cinematics.lerp(anchor.add(fwd.multiply(40)).add(0, 8, 0), door.add(0, 6, 0), u), 72, false);
		} else if (a < STUDIO) {
			// Glide across the island to the door in the sky.
			double u = Cinematics.ease((a - GLIDE) / (double) (STUDIO - 4 - GLIDE));
			Vec3d start = anchor.subtract(fwd.multiply(8)).add(right.multiply(2)).add(0, 44, 0);
			Vec3d end = door.subtract(fwd.multiply(7)).add(0, 0.8, 0);
			Cinematics.aim(s, Cinematics.lerp(start, end, u), door, (float) lerp(66, 52, u), false);
		} else if (a < WIDE) {
			// Through the door: the studio outside.
			double u = Cinematics.ease((a - STUDIO) / (double) (WIDE - STUDIO));
			Vec3d p0 = doorCentre(studio, fwd, 1.0).add(0, 0.4, 0);
			Cinematics.aim(s, p0.add(fwd.multiply(lerp(0, 12, u))), controlRoom().add(0, -4, 0), (float) lerp(72, 62, u), true);
		} else if (a < TrumanSet.CUT) {
			double u = (a - WIDE) / (double) (TrumanSet.CUT - WIDE);
			Vec3d p = studio.add(fwd.multiply(212)).add(right.multiply(lerp(80, 62, u))).add(0, 46 - u * 6, 0);
			Cinematics.aim(s, p, studio.add(0, 32, 0), 70, true);
		} else if (a < INSIDE_AERIAL) {
			// The strike, from the ground: lights switch on, panels come down, the island is peeled away.
			Vec3d p = anchor.subtract(fwd.multiply(44)).add(right.multiply(26)).add(0, 9, 0);
			Cinematics.aim(s, p, anchor.add(fwd.multiply(25)).add(0, 42 - Math.max(0, a - 500) * 0.25, 0), 84, false);
		} else if (a < FINALE) {
			double u = (a - INSIDE_AERIAL) / (double) (FINALE - INSIDE_AERIAL);
			double ang = u * 0.6;
			Vec3d off = fwd.multiply(-22 * Math.cos(ang)).add(right.multiply(22 * Math.sin(ang) + 10));
			Cinematics.aim(s, anchor.add(off).add(0, lerp(58, 66, u), 0), anchor.add(fwd.multiply(8)), 82, false);
		} else {
			// The door again, framed for the farewell. The camera itself takes a bow.
			Vec3d p = anchor.add(fwd.multiply(62)).add(right.multiply(7)).add(0, 9, 0);
			Cinematics.aim(s, p, door, 58, false);
			if (a > 672 && a < 700) {
				s.pitch = MathHelper.clamp(s.pitch + 24.0F * MathHelper.sin((float) Math.PI * (a - 672) / 28.0F), -90.0F, 90.0F);
			}
		}
	}

	private Vec3d controlRoom() {
		return studio.add(fwd.multiply(235)).add(0, 85, 0);
	}

	@Override
	protected float handheld(float a) {
		if (a >= TrumanSet.LIGHT_HIT && a < TrumanSet.LIGHT_HIT + 20) {
			return 0.8F;
		}
		if (a >= TrumanSet.LAMPS_ON && a < FINALE) {
			return 0.5F;
		}
		return a < OPEN_END ? 0.05F : 0.15F;
	}

	@Override
	public float fade(float a) {
		return Math.max(super.fade(a), Math.max(Cinematics.dip(a, STUDIO - 1, 6), Cinematics.dip(a, FINALE, 5)));
	}

	@Override
	public Cinematics.Caption caption(float a) {
		// The only words in the whole show are the farewell; everything else is left for the player to read.
		Cinematics.Caption c = Cinematics.cap(a, FINALE + 6, 648, "GOOD MORNING!", "and in case I don't see ya...", 0xFFFFFF);
		return c != null ? c : Cinematics.cap(a, 652, 698, "GOOD AFTERNOON,", "good evening, and good night.", 0xFFFFFF);
	}

	// ======================================================================== tick

	@Override
	public boolean tick(ClientWorld world) {
		if (age >= TrumanSet.STRIKE && age < TrumanSet.WALL) {
			// Through the holes in the sky: the dark of the studio.
			ClientFx.tintSky(0x07070C, (float) Math.min(1.0, (age - TrumanSet.STRIKE) / 60.0));
		}
		if (age == TrumanSet.LIGHT_HIT) {
			ClientFx.light(crash.add(0, 2, 0), 1.0F, 0xFFF0D0);
			for (int i = 0; i < 40; i++) {
				world.addImportantParticle(ParticleTypes.ELECTRIC_SPARK, true, crash.x, crash.y + 1, crash.z,
						world.random.nextGaussian() * 0.6, world.random.nextDouble() * 0.8, world.random.nextGaussian() * 0.6);
			}
		} else if (age >= TrumanSet.LIGHT_FALL && age < TrumanSet.LIGHT_HIT) {
			ClientFx.light(lampPos(age), 0.6F, 0xFFF0D0);
		}
		if (age >= TrumanSet.DOOR_OPEN && age < STUDIO) {
			ClientFx.light(doorCentre(base, fwd, -0.4), 1.2F, 0xFFF4E0);
		}
		if (age >= STUDIO && age < TrumanSet.CUT) {
			ClientFx.light(controlRoom(), 0.8F, 0xA8C8F0);
		}
		if (age >= FINALE && age < TrumanSet.WALL) {
			// The farewell is lit by the doorway: warm and golden, the way home.
			ClientFx.light(doorCentre(base, fwd, -0.4), 0.9F, 0xFFD8A0);
		}
		if (age >= TrumanSet.CUT - 6 && age < TrumanSet.CUT) {
			ClientFx.screen(3, 0.8F, 2);
		}
		for (int i = 0; i < TrumanSet.LAMPS; i++) {
			if (age == TrumanSet.lampOn(i)) {
				ClientFx.light(TrumanSet.lamp(anchor, fwd, i), 1.2F, 0xFFF0D0);
			}
		}
		return super.tick(world);
	}

	// ======================================================================== scenery

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		if (a >= TrumanSet.CUT && a < TrumanSet.BARS_END || a >= TrumanSet.WALL) {
			return;
		}
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		if (a >= STUDIO && a < TrumanSet.CUT) {
			renderStudio(solid, glow, m, cam, cb, a);
			return;
		}
		Vec3d c = base.subtract(cam);
		dome(solid, m, c, a, holeJ, true);
		if (a < TrumanSet.STRIKE + 30) {
			paintedDetails(solid, m, c, fwd, a);
		}
		float open = (float) clamp((a - TrumanSet.DOOR_OPEN) / 8.0);
		door(solid, glow, m, c, fwd, right, true, open);
		if (open > 0 && (a < STUDIO || a >= FINALE)) {
			// Daylight from the studio pours in through the open door (and again, golden, for the farewell).
			Vec3d d = doorCentre(c, fwd, -0.4);
			FxDraw.beam(glow, m, d, d.subtract(fwd.multiply(30)).add(0, -6, 0), Vec3d.ZERO, 3.2, 0xFFF0D8, 0.35F * open);
			Solids.tube(glow, m, d, d.subtract(fwd.multiply(40)).add(0, -12, 0), 2.2, 9, 14, 0xFFF0D8, 0.07F * open, false);
			lampMotes(glow, m, cb, d, d.subtract(fwd.multiply(40)).add(0, -12, 0), 1.6, open, a);
		}
		// SIRIUS falls.
		if (a >= TrumanSet.LIGHT_FALL && a < TrumanSet.LIGHT_HIT) {
			Vec3d dir = new Vec3d(Math.cos(a * 0.21), -0.5, Math.sin(a * 0.21));
			studioLamp(solid, glow, m, cb, lampPos(a).subtract(cam), dir, 1.15, 0.6F, "SIRIUS");
		}
		if (a >= TrumanSet.LIGHT_HIT && a < TrumanSet.LIGHT_HIT + 90) {
			float t = a - TrumanSet.LIGHT_HIT;
			Vec3d g = crash.subtract(cam);
			Cinematics.debris(solid, glow, m, g, t, 26, 0.7, 0.7, 0.06, 41, 0x26262C, 0xFFE6B0);
			Cinematics.dustWall(shade, m, cb, g, 2 + t * 0.6, 3 + t * 0.05, 0x9A948A, 0.6F * (1 - t / 90.0F), 9);
		}
		// The studio lights come down through the rig and switch on.
		if (a >= TrumanSet.LAMPS_ON - 20) {
			for (int i = 0; i < TrumanSet.LAMPS; i++) {
				float on = (float) clamp((a - TrumanSet.lampOn(i)) / 2.0);
				Vec3d lamp = TrumanSet.lamp(anchor, fwd, i);
				Vec3d spot = TrumanSet.spot(anchor, fwd, i, a);
				Vec3d hit = new Vec3d(spot.x, groundY(spot.x, spot.z), spot.z);
				Vec3d lc = lamp.subtract(cam);
				double lower = Math.max(0, TrumanSet.LAMPS_ON - a) * 0.8;
				lc = lc.add(0, lower, 0);
				Vec3d aimDir = hit.subtract(lamp);
				studioLamp(solid, glow, m, cb, lc, aimDir, 2.4, on, null);
				FxDraw.ribbon(solid, m, lc.add(0, 6, 0), lc.add(0, 40, 0), Vec3d.ZERO, 0.25, 0x2A2A2E, 1.0F, 1.0F);
				if (on > 0) {
					float flare = (float) Math.max(0, 1 - (a - TrumanSet.lampOn(i)) / 8.0);
					lampBeam(glow, m, lc.add(aimDir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, on, a);
					lampMotes(glow, m, cb, lc.add(aimDir.normalize().multiply(4.2)), hit.subtract(cam), 2.4, on, a + i * 40);
					if (flare > 0) {
						Solids.dot(glow, m, lc, cb, 26 * flare, 0xFFF4E0, 0.8F * flare);
					}
				}
			}
		}
	}

	private void renderStudio(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d cam, Vec3d[] cb, float a) {
		Vec3d S = studio.subtract(cam);
		Vec3d up = new Vec3d(0, 1, 0);
		// Floor: dark checker with yellow tape.
		for (int gx = -12; gx < 12; gx++) {
			for (int gz = -12; gz < 12; gz++) {
				Vec3d p = S.add(gx * 25, -0.05, gz * 25);
				Solids.quad(solid, m, p, p.add(25, 0, 0), p.add(25, 0, 25), p.add(0, 0, 25), ((gx + gz) & 1) == 0 ? 0x131418 : 0x17181D, 1.0F);
			}
		}
		for (int k = -6; k <= 6; k++) {
			Vec3d px = S.add(k * 50, 0.03, -300), pz = S.add(-300, 0.03, k * 50);
			Solids.quad(solid, m, px.add(-0.3, 0, 0), px.add(0.3, 0, 0), px.add(0.3, 0, 600), px.add(-0.3, 0, 600), 0xB8901C, 1.0F);
			Solids.quad(solid, m, pz.add(0, 0, -0.3), pz.add(0, 0, 0.3), pz.add(600, 0, 0.3), pz.add(600, 0, -0.3), 0xB8901C, 1.0F);
		}
		// The sound stage: walls and a ceiling of lighting truss.
		Solids.tube(solid, m, S.add(0, -1, 0), S.add(0, 230, 0), 300, 300, 40, 0x0B0B0E, 1.0F, false);
		for (int k = -5; k <= 5; k++) {
			// Box truss: two chords and a zig-zag web.
			Solids.tube(solid, m, S.add(k * 50, 224, -280), S.add(k * 50, 224, 280), 0.7, 0.7, 4, 0x2A2A30, 1.0F, true);
			Solids.tube(solid, m, S.add(k * 50, 219, -280), S.add(k * 50, 219, 280), 0.5, 0.5, 4, 0x2A2A30, 1.0F, true);
			for (int z = -280; z < 280; z += 10) {
				Solids.tube(solid, m, S.add(k * 50, 224, z), S.add(k * 50, 219, z + 5), 0.18, 0.18, 4, 0x34343A, 1.0F, true);
				Solids.tube(solid, m, S.add(k * 50, 219, z + 5), S.add(k * 50, 224, z + 10), 0.18, 0.18, 4, 0x34343A, 1.0F, true);
			}
			for (int q = -5; q <= 5; q++) {
				Vec3d lp = S.add(k * 50, 217, q * 50);
				float tw = 0.75F + 0.25F * MathHelper.sin(a * 0.07F + k * 3 + q);
				Solids.dot(glow, m, lp, cb, 2.2, 0xFFE6B8, 0.85F * tw);
				if ((k + q) % 2 == 0) {
					// Soft cones of light down to the stage floor.
					Solids.tube(glow, m, lp, lp.add(0, -216, 0), 1.0, 26, 10, 0xFFE6B8, 0.025F * tw, false);
					FxDraw.disc(glow, m, lp.add(0, -216.8, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 26, 0xFFE6B8, 0.12F * tw, 0.0F, 20);
				}
			}
		}
		// The set: the back of the sky, with the door left open.
		dome(solid, m, S, a, -1, false);
		door(solid, glow, m, S, fwd, right, false, 1.0F);
		Solids.dot(glow, m, doorCentre(S, fwd, 0.6), cb, 10, 0xFFE8C8, 0.4F);
		// Production lights ring the set, all aimed at the sky.
		for (int k = 0; k < 8; k++) {
			double ang = TrumanSet.bearing(fwd) + Math.toRadians(22.5 + k * 45);
			Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
			Vec3d foot = S.add(out.multiply(150));
			Vec3d head = foot.add(0, 28, 0);
			Solids.tube(solid, m, foot, head, 1.3, 1.0, 8, 0x2A2A30, 1.0F, true);
			for (int l = 0; l < 3; l++) {
				double la = ang + l * MathHelper.TAU / 3;
				Solids.tube(solid, m, foot.add(Math.cos(la) * 7, 0, Math.sin(la) * 7), foot.add(0, 9, 0), 0.5, 0.5, 5, 0x2A2A30, 1.0F, true);
			}
			Vec3d target = S.add(out.multiply(68)).add(0, 36, 0);
			Vec3d dir = target.subtract(head);
			studioLamp(solid, glow, m, cb, head.add(0, 5, 0), dir, 4.0, 1.0F, null);
			Solids.tube(glow, m, head.add(0, 5, 0).add(dir.normalize().multiply(7)), target, 4.5, 20, 16, 0xFFF0D0, 0.06F, false);
			lampMotes(glow, m, cb, head.add(0, 5, 0).add(dir.normalize().multiply(7)), target, 4.0, 0.8F, a + k * 50);
		}
		// The control room high above the stage, the director backlit in its window.
		Vec3d cr = controlRoom().subtract(cam);
		box(solid, m, cr.subtract(right.multiply(90)).add(0, -45, 0), right.multiply(180), up.multiply(90), fwd.multiply(30), 0x1B1C22);
		Vec3d win = cr.subtract(fwd.multiply(0.3));
		Vec3d wr = right.multiply(60), wu = up.multiply(13);
		glowQuad(glow, m, win.subtract(wr).subtract(wu).add(0, 5, 0), wr.multiply(2), wu.multiply(2), 0x9CC4F0, 0.95F);
		glowQuad(glow, m, win.subtract(wr.multiply(1.15)).subtract(wu.multiply(1.6)).add(0, 5, 0), wr.multiply(2.3), wu.multiply(3.2), 0x4878B8, 0.25F);
		for (int k = -4; k <= 4; k++) {
			Vec3d b = win.subtract(fwd.multiply(0.2)).add(right.multiply(k * 15)).add(0, -8, 0);
			Solids.quad(solid, m, b.subtract(right.multiply(0.5)), b.add(right.multiply(0.5)), b.add(right.multiply(0.5)).add(0, 26, 0), b.subtract(right.multiply(0.5)).add(0, 26, 0), 0x0E0E12, 1.0F);
		}
		silhouette(solid, m, win.subtract(fwd.multiply(1.2)).add(0, -5, 0), 1.0, true);
		for (int k = -1; k <= 1; k += 2) {
			silhouette(solid, m, win.subtract(fwd.multiply(1.2)).add(right.multiply(k * 32)).add(0, -6, 0), 0.75, false);
			silhouette(solid, m, win.subtract(fwd.multiply(1.2)).add(right.multiply(k * 48)).add(0, -6.5, 0), 0.7, false);
		}
		float blink = ((int) (a * 0.12F) & 1) == 0 ? 1.0F : 0.35F;
		Vec3d sign = cr.subtract(fwd.multiply(0.5)).add(0, 26, 0).add(right.multiply(-TrumanSet.width("ON AIR") * 1.1));
		pixelText(glow, m, sign, right, up.multiply(-1), 2.2, "ON AIR", 0xFF2A2A, blink);
		// Walls of monitors, every one of them showing the island.
		for (int side = -1; side <= 1; side += 2) {
			double ang = TrumanSet.bearing(fwd) + Math.toRadians(side * 32);
			Vec3d dir = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
			Vec3d rv = new Vec3d(-dir.z, 0, dir.x);
			Vec3d bank = S.add(dir.multiply(240)).add(0, 18, 0);
			box(solid, m, bank.subtract(rv.multiply(47)).add(0, -3, 0), rv.multiply(94), up.multiply(52), dir.multiply(4), 0x101014);
			for (int cx = 0; cx < 5; cx++) {
				for (int cy = 0; cy < 4; cy++) {
					Vec3d o = bank.subtract(dir.multiply(0.2)).subtract(rv.multiply(45)).add(rv.multiply(cx * 18 + 1)).add(0, cy * 12 + 1, 0);
					screen(glow, m, o, rv.multiply(16), up.multiply(10), a, cx * 7 + cy * 3 + side);
				}
			}
		}
	}

	private static void glowQuad(VertexConsumer glow, Matrix4f m, Vec3d o, Vec3d u, Vec3d v, int rgb, float alpha) {
		Solids.quad(glow, m, o, o.add(u), o.add(u).add(v), o.add(v), rgb, alpha);
	}

	/** One monitor: sky, sea and the little house, flickering, with a red LIVE dot. */
	private static void screen(VertexConsumer glow, Matrix4f m, Vec3d o, Vec3d u, Vec3d v, float a, int seed) {
		float k = 0.55F + 0.3F * MathHelper.sin(a * 0.31F + seed) + 0.1F * MathHelper.sin(a * 1.7F + seed * 3);
		if (Math.floorMod(seed, 5) == 0) {
			// A close-up: the face everybody is watching.
			glowQuad(glow, m, o, u, v, 0x704030, 0.6F * k);
			FxDraw.disc(glow, m, o.add(u.multiply(0.5)).add(v.multiply(0.5)), u.normalize(), v.normalize(), v.length() * 0.38, 0xE8A878, 0.8F * k, 0.5F * k, 16);
		} else {
			glowQuad(glow, m, o.add(v.multiply(0.42)), u, v.multiply(0.58), 0x6AB0E8, 0.75F * k);
			glowQuad(glow, m, o.add(v.multiply(0.22)), u, v.multiply(0.2), 0x2E74B0, 0.7F * k);
			glowQuad(glow, m, o, u, v.multiply(0.22), 0x4C9A3A, 0.7F * k);
			glowQuad(glow, m, o.add(u.multiply(0.55)).add(v.multiply(0.1)), u.multiply(0.15), v.multiply(0.22), 0xF0F0F0, 0.8F * k);
			// Moving picture: a drifting cloud, glinting waves and a tiny figure walking to work.
			double cloud = ((a * 0.004 + seed * 0.13) % 1.2) - 0.2;
			glowQuad(glow, m, o.add(u.multiply(cloud)).add(v.multiply(0.72)), u.multiply(0.22), v.multiply(0.1), 0xF4F8FF, 0.7F * k);
			for (int w = 0; w < 3; w++) {
				double wx = ((a * 0.012 + w * 0.37 + seed * 0.07) % 1.0) * 0.9;
				glowQuad(glow, m, o.add(u.multiply(wx)).add(v.multiply(0.27 + w * 0.05)), u.multiply(0.08), v.multiply(0.02), 0xC8E8FF, 0.6F * k);
			}
			double walk = ((a * 0.006 + seed * 0.21) % 1.0) * 0.94;
			glowQuad(glow, m, o.add(u.multiply(walk)).add(v.multiply(0.08)), u.multiply(0.03), v.multiply(0.12), 0x202430, 0.9F * k);
		}
		// The occasional channel-change roll.
		if (Solids.hash(seed, (int) (a / 30)) < 0.12) {
			double roll = (a % 30) / 30.0;
			glowQuad(glow, m, o.add(v.multiply(roll * 0.85)), u, v.multiply(0.15), 0xE0E0E0, 0.35F);
		}
		if (((int) (a * 0.1F) & 1) == 0) {
			glowQuad(glow, m, o.add(u.multiply(0.06)).add(v.multiply(0.82)), u.multiply(0.05), v.multiply(0.1), 0xFF2020, 0.9F);
		}
	}

	/** A black head-and-shoulders against the window; {@code beret} for the director. */
	private void silhouette(VertexConsumer solid, Matrix4f m, Vec3d p, double s, boolean beret) {
		Solids.ellipsoid(solid, m, p, 9 * s, 5 * s, 3.5 * s, 8, 12, (i, j, n) -> 0x050507, false, 1.0F);
		Vec3d head = p.add(0, 8.5 * s, 0);
		Solids.ellipsoid(solid, m, head.add(0, -3.5 * s, 0), 1.8 * s, 2.5 * s, 1.8 * s, 6, 8, (i, j, n) -> 0x050507, false, 1.0F);
		Solids.ellipsoid(solid, m, head, 3.4 * s, 4.0 * s, 3.4 * s, 8, 12, (i, j, n) -> 0x050507, false, 1.0F);
		if (beret) {
			Solids.ellipsoid(solid, m, head.add(right.multiply(0.9 * s)).add(0, 3.4 * s, 0), 4.2 * s, 1.2 * s, 4.2 * s, 6, 12, (i, j, n) -> 0x050507, false, 1.0F);
		}
	}

	// ======================================================================== broadcast graphics

	@Override
	public void drawHud(DrawContext ctx, int w, int h, float a) {
		TextRenderer f = MinecraftClient.getInstance().textRenderer;
		if (a >= TrumanSet.CUT && a < TrumanSet.BARS_END) {
			colourBars(ctx, f, w, h, a);
			return;
		}
		if (a >= TrumanSet.WALL) {
			theEnd(ctx, f, w, h, a);
			return;
		}
		if (a < CRANE) {
			broadcast(ctx, f, w, h, a, "CAM 07", 0);
		} else if (a >= FINALE) {
			broadcast(ctx, f, w, h, a, "CAM 01", FINALE);
		}
	}

	private void broadcast(DrawContext ctx, TextRenderer f, int w, int h, float a, String cam, int start) {
		int alpha = (int) (MathHelper.clamp((a - start) / 10.0F, 0.0F, 1.0F) * 255);
		if (alpha <= 4) {
			return;
		}
		int white = (alpha << 24) | 0xFFFFFF;
		// Old-TV 4:3 pillarbox and scanlines.
		int pw = Math.max(0, (int) ((w - h * 4 / 3.0F) / 2 * 0.75F));
		if (pw > 0) {
			ctx.fill(0, 0, pw, h, (alpha << 24));
			ctx.fill(w - pw, 0, w, h, (alpha << 24));
		}
		int lineA = (int) (alpha * 0.08F) << 24;
		for (int y = 0; y < h; y += 3) {
			ctx.fill(pw, y, w - pw, y + 1, lineA);
		}
		int top = (int) (h * 0.12F) + 8, bottom = h - (int) (h * 0.12F) - 16;
		int left = pw + 10, rightX = w - pw - 10;
		// Viewfinder corners.
		int cl = 14, ct = 2;
		ctx.fill(left - 4, top - 4, left - 4 + cl, top - 4 + ct, white);
		ctx.fill(left - 4, top - 4, left - 4 + ct, top - 4 + cl, white);
		ctx.fill(rightX + 4 - cl, top - 4, rightX + 4, top - 4 + ct, white);
		ctx.fill(rightX + 4 - ct, top - 4, rightX + 4, top - 4 + cl, white);
		ctx.fill(left - 4, bottom + 12 - ct, left - 4 + cl, bottom + 12, white);
		ctx.fill(left - 4, bottom + 12 - cl, left - 4 + ct, bottom + 12, white);
		ctx.fill(rightX + 4 - cl, bottom + 12 - ct, rightX + 4, bottom + 12, white);
		ctx.fill(rightX + 4 - ct, bottom + 12 - cl, rightX + 4, bottom + 12, white);
		if (((int) (a / 10) & 1) == 0) {
			ctx.fill(left, top + 1, left + 6, top + 7, (alpha << 24) | 0xE02020);
		}
		ctx.drawTextWithShadow(f, "LIVE", left + 10, top, white);
		ctx.drawTextWithShadow(f, cam, rightX - f.getWidth(cam), top, white);
		int frames = (int) a % 20, sec = (int) (a / 20);
		String tc = String.format("07:%02d:%02d:%02d", 14 + sec / 60, 32 + sec % 28, frames);
		ctx.drawTextWithShadow(f, "DAY 10,909", left, bottom - 10, white);
		ctx.drawTextWithShadow(f, tc, left, bottom, (alpha << 24) | 0xC8C8C8);
		String place = "SEAHAVEN ISLAND";
		ctx.drawTextWithShadow(f, place, rightX - f.getWidth(place), bottom, (alpha << 24) | 0xC8C8C8);
		// Network bug: a little "TS" globe in the corner.
		int bx = rightX - 30, by = top + 14;
		ctx.fill(bx, by, bx + 26, by + 14, (alpha * 3 / 5 << 24) | 0x0E2A4A);
		ctx.fill(bx, by + 13, bx + 26, by + 14, (alpha << 24) | 0x5AA8FF);
		ctx.drawTextWithShadow(f, "TS", bx + 7, by + 3, white);
		// Audio meters bouncing to the ambience.
		for (int ch = 0; ch < 2; ch++) {
			float lvl = 0.35F + 0.3F * MathHelper.sin(a * 0.9F + ch * 1.7F) + 0.2F * MathHelper.sin(a * 2.3F + ch);
			int mh = 38, mx = left + ch * 6, my = bottom - 26;
			ctx.fill(mx, my - mh, mx + 4, my, (alpha / 3 << 24));
			int filled = (int) (mh * MathHelper.clamp(lvl, 0.05F, 1.0F));
			for (int y = 0; y < filled; y += 3) {
				int col = y > mh * 0.8 ? 0xFF4040 : y > mh * 0.6 ? 0xFFD040 : 0x50E070;
				ctx.fill(mx, my - y - 2, mx + 4, my - y, (alpha << 24) | col);
			}
		}
		// Lower third sliding in: who we're watching.
		float slide = MathHelper.clamp((a - start - 20) / 12.0F, 0.0F, 1.0F) * MathHelper.clamp((start + (start == 0 ? 100 : 90) - a) / 10.0F, 0.0F, 1.0F);
		if (slide > 0) {
			int lw = 190, lx = left + 16 - (int) ((1 - slide) * (lw + 40)), ly = bottom - 44;
			int la = (int) (alpha * slide);
			ctx.fillGradient(lx, ly, lx + lw, ly + 22, (la * 4 / 5 << 24) | 0x1A4C8C, (la * 4 / 5 << 24) | 0x0C2448);
			ctx.fill(lx, ly, lx + 3, ly + 22, (la << 24) | 0xFFD040);
			ctx.drawTextWithShadow(f, start == 0 ? "TRUMAN BURBANK" : "THANK YOU FOR WATCHING", lx + 8, ly + 3, (la << 24) | 0xFFFFFF);
			ctx.drawTextWithShadow(f, start == 0 ? "insurance salesman · seahaven" : "1.7 billion viewers · 30 years live", lx + 8, ly + 12, (la << 24) | 0xA8C8F0);
		}
		// News-style ticker along the bottom.
		String tick = "  SEAHAVEN WEATHER: SUNNY, AS ALWAYS  ·  TRAFFIC: CLEAR  ·  SIRIUS SPOTTED ABOVE TOWN?  ·  STAY TUNED  ·";
		int tw = f.getWidth(tick);
		int ty = bottom + 16;
		ctx.fill(pw, ty - 2, w - pw, ty + 10, (alpha * 2 / 3 << 24) | 0x0A0A12);
		int off = (int) (a * 1.4F) % tw;
		ctx.enableScissor(pw, ty - 2, w - pw, ty + 10);
		ctx.drawTextWithShadow(f, tick + tick, pw - off, ty, (alpha << 24) | 0xE8E8E8);
		ctx.disableScissor();
	}

	private static void colourBars(DrawContext ctx, TextRenderer f, int w, int h, float a) {
		int[] bars = {0xC0C0C0, 0xC0C000, 0x00C0C0, 0x00C000, 0xC000C0, 0xC00000, 0x0000C0};
		int[] low = {0x0000C0, 0x131313, 0xC000C0, 0x131313, 0x00C0C0, 0x131313, 0xC0C0C0};
		int top = (int) (h * 0.67F), mid = (int) (h * 0.75F);
		for (int i = 0; i < 7; i++) {
			int x0 = w * i / 7, x1 = w * (i + 1) / 7;
			ctx.fill(x0, 0, x1, top, 0xFF000000 | bars[i]);
			ctx.fill(x0, top, x1, mid, 0xFF000000 | low[i]);
		}
		ctx.fill(0, mid, w, h, 0xFF101010);
		ctx.fill(0, mid, w / 5, h, 0xFF00214C);
		ctx.fill(w / 5, mid, w * 2 / 5, h, 0xFFFFFFFF);
		ctx.fill(w * 2 / 5, mid, w * 3 / 5, h, 0xFF32006A);
		int bw = 190, bh = 44;
		ctx.fill(w / 2 - bw / 2, h / 2 - bh / 2, w / 2 + bw / 2, h / 2 + bh / 2, 0xF0000000);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w / 2.0F, h / 2.0F - 13, 0);
		ctx.getMatrices().scale(2.0F, 2.0F, 1.0F);
		ctx.drawCenteredTextWithShadow(f, "PLEASE STAND BY", 0, 0, 0xFFFFFFFF);
		ctx.getMatrices().pop();
		ctx.drawCenteredTextWithShadow(f, "we are experiencing technical difficulties", w / 2, h / 2 + 9, 0xFFB0B0B0);
	}

	private static void theEnd(DrawContext ctx, TextRenderer f, int w, int h, float a) {
		ctx.fill(0, 0, w, h, 0xFF000000);
		float k = MathHelper.clamp((a - TrumanSet.WALL - 6) / 14.0F, 0.0F, 1.0F);
		int alpha = Math.max(5, (int) (k * 255)) << 24;
		// Old film: flickering exposure, vertical scratches and dust specks.
		int flick = (int) (18 * k * (0.6F + 0.4F * MathHelper.sin(a * 2.7F)));
		ctx.fill(0, 0, w, h, (flick << 24) | 0x302820);
		for (int i = 0; i < 3; i++) {
			if (Solids.hash(i, (int) a) < 0.5) {
				int x = (int) (Solids.hash(i + 7, (int) a) * w);
				ctx.fill(x, 0, x + 1, h, ((int) (60 * k) << 24) | 0xD0C8B8);
			}
		}
		for (int i = 0; i < 8; i++) {
			int x = (int) (Solids.hash(i, (int) a * 3) * w), y = (int) (Solids.hash(i + 40, (int) a * 3) * h);
			ctx.fill(x, y, x + 2, y + 2, ((int) (90 * k) << 24) | 0xE0D8C8);
		}
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w / 2.0F, h / 2.0F - 24, 0);
		ctx.getMatrices().scale(5.0F, 5.0F, 1.0F);
		ctx.drawCenteredTextWithShadow(f, "THE END", 0, 0, alpha | 0xF4ECDC);
		ctx.getMatrices().pop();
		float k2 = MathHelper.clamp((a - TrumanSet.WALL - 20) / 14.0F, 0.0F, 1.0F);
		int a2 = Math.max(5, (int) (k2 * 255)) << 24;
		int rule = (int) (120 * k2);
		ctx.fill(w / 2 - rule, h / 2 + 14, w / 2 + rule, h / 2 + 15, a2 | 0x8A8070);
		ctx.drawCenteredTextWithShadow(f, "a Christof production", w / 2, h / 2 + 22, a2 | 0xA8A090);
		float k3 = MathHelper.clamp((a - TrumanSet.WALL - 34) / 14.0F, 0.0F, 1.0F);
		ctx.drawCenteredTextWithShadow(f, "starring YOU", w / 2, h / 2 + 36, (Math.max(5, (int) (k3 * 255)) << 24) | 0xFFE0A0);
	}
}
