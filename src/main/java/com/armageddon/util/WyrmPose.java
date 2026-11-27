package com.armageddon.util;

import com.armageddon.util.PteroPose.Seg;
import com.armageddon.util.WorldBombShapes.Wyrm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The Elder Wyrm, posed for any moment: a serpentine neck and tail, a horned head with a jaw that drops to breathe,
 * four legs, and two great wings with membranes stretched between their finger bones. Relative to the bomb's ground,
 * in world axes. Bone radii are in pose units (multiply by {@link Wyrm#SC} to draw); segments flagged {@code fire}
 * glow with the magma under its scales.
 */
public final class WyrmPose {
	private WyrmPose() {
	}

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	public static final class Pose {
		public final List<Seg> segs = new ArrayList<>();
		/** Wing membranes, as triangles. */
		public final List<Vec3d[]> membrane = new ArrayList<>();
		public final Vec3d[] eyes = new Vec3d[2];
		public final Vec3d[] tips = new Vec3d[2];
		public Vec3d body, chest, mouth, mouthDir, f, u, r;
	}

	private record Frame(Vec3d o, Vec3d f, Vec3d u, Vec3d r) {
		Vec3d at(double x, double y, double z) {
			double s = Wyrm.SC;
			return o.add(f.multiply(x * s)).add(u.multiply(y * s)).add(r.multiply(z * s));
		}
	}

	private static Vec3d rot(Vec3d v, Vec3d axis, double ang) {
		double c = Math.cos(ang), s = Math.sin(ang);
		return v.multiply(c).add(axis.crossProduct(v).multiply(s)).add(axis.multiply(axis.dotProduct(v) * (1 - c)));
	}

	/** Its pose at tick {@code t}, the bomb facing {@code h}. */
	public static Pose pose(double t, Vec3d h) {
		Pose p = new Pose();
		Vec3d pos = FossilShapes.frame(h, Wyrm.pos(t));
		Vec3d vel = FossilShapes.frame(h, Wyrm.pos(t + 1).subtract(Wyrm.pos(t - 1))).multiply(0.5);
		boolean ground = Wyrm.grounded(t);
		boolean hover = t >= Wyrm.HOVER - 10 && t < Wyrm.LAND;
		// Which way it faces: along its flight, or the bomb's heading when on the ground or hovering.
		Vec3d fh = new Vec3d(vel.x, 0, vel.z);
		fh = ground || hover || fh.lengthSquared() < 0.04 ? h : fh.normalize();
		Vec3d r = fh.crossProduct(UP).normalize();
		double pitch = ground || hover ? 0 : MathHelper.clamp(Math.atan2(vel.y, Math.max(0.2, Math.hypot(vel.x, vel.z))), -0.6, 0.6);
		if (t < Wyrm.ROAR - 15) {
			// Clawing its way up out of the ground.
			pitch = 0.5 * (1 - WorldBombShapes.smooth((t - Wyrm.EMERGE - 40) / 60.0));
		}
		Vec3d acc = FossilShapes.frame(h, Wyrm.pos(t + 2).subtract(Wyrm.pos(t).multiply(2)).add(Wyrm.pos(t - 2)));
		double roll = ground || hover ? 0 : MathHelper.clamp(acc.dotProduct(r) * 1.2, -0.7, 0.7);
		Vec3d f = rot(fh, r, pitch);
		Vec3d u = r.crossProduct(f).normalize();
		Vec3d rr = rot(r, f, roll), uu = rot(u, f, roll);
		Frame b = new Frame(pos, f, uu, rr);
		p.body = pos;
		p.f = f;
		p.u = uu;
		p.r = rr;
		// Spine: hips to chest, a slight hump.
		Vec3d hip = b.at(-6, 0, 0), mid = b.at(0, 0.8, 0), chest = b.at(6, 0.3, 0);
		p.chest = chest;
		p.segs.add(new Seg(hip, mid, 2.6, 3.1, false));
		p.segs.add(new Seg(mid, chest, 3.1, 2.9, false));
		// Plates of magma glowing along its back.
		for (int i = 0; i < 6; i++) {
			Vec3d s = b.at(-5 + i * 2.1, 3.1 + 0.6 * Math.sin(i), 0);
			p.segs.add(new Seg(s, s.add(uu.multiply(1.8 * Wyrm.SC)).subtract(f.multiply(0.8 * Wyrm.SC)), 0.6, 0.1, true));
		}
		// Neck: an S-curve out of the chest to the head, pointed where it looks.
		double jaw = Wyrm.jaw(t);
		int k = Wyrm.breathing(t);
		Vec3d look;
		if (k >= 0) {
			Vec3d hit = FossilShapes.frame(h, Wyrm.breathHit(k, t));
			look = hit.subtract(chest).normalize();
		} else if (ground && t > Wyrm.ROAR - 20 && t < Wyrm.TAKEOFF || t >= Wyrm.CHARGE && t < Wyrm.NOVA - 4) {
			look = fh.multiply(0.3).add(0, 1, 0).normalize();
		} else if (t >= Wyrm.NOVA - 4 && t < Wyrm.NOVA + 40) {
			// The nova: breathed straight down at the ground in front.
			look = fh.multiply(0.8).add(0, -0.9, 0).normalize();
		} else {
			look = ground ? fh.multiply(0.7).add(0, 0.5, 0).normalize() : f.multiply(0.9).add(uu.multiply(0.15)).normalize();
		}
		double sway = Math.sin(t * 0.07) * 0.3;
		Vec3d head = chest.add(look.multiply(13 * Wyrm.SC)).add(rr.multiply(sway * Wyrm.SC * 2));
		Vec3d ctrl = chest.add(f.multiply(5 * Wyrm.SC)).add(uu.multiply((ground ? 6 : 2) * Wyrm.SC));
		Vec3d prev = chest;
		for (int i = 1; i <= 10; i++) {
			double s = i / 10.0;
			Vec3d q = chest.multiply((1 - s) * (1 - s)).add(ctrl.multiply(2 * s * (1 - s))).add(head.multiply(s * s));
			q = q.add(rr.multiply(Math.sin(s * Math.PI * 2 + t * 0.05) * 0.4 * Wyrm.SC * s));
			p.segs.add(new Seg(prev, q, 2.0 - 0.9 * (i - 1) / 9.0, 2.0 - 0.9 * i / 9.0, false));
			if (i % 2 == 0) {
				p.segs.add(new Seg(q, q.add(uu.multiply(1.0 * Wyrm.SC)), 0.35, 0.05, true));
			}
			prev = q;
		}
		// Head: a long wedge of a skull, swept-back horns, the jaw dropping to breathe.
		Vec3d hf = look;
		Vec3d hr = hf.crossProduct(UP);
		hr = hr.lengthSquared() < 1.0E-4 ? rr : hr.normalize();
		Vec3d hu = hr.crossProduct(hf).normalize();
		Frame hd = new Frame(head, hf, hu, hr);
		p.segs.add(new Seg(hd.at(-1.2, 0.3, 0), hd.at(2.0, 0.4, 0), 1.6, 1.25, false));
		p.segs.add(new Seg(hd.at(2.0, 0.4, 0), hd.at(5.2, 0.0, 0), 1.2, 0.6, false));
		for (int s = -1; s <= 1; s += 2) {
			Vec3d root = hd.at(-0.6, 1.2, s * 0.9);
			Vec3d mid2 = root.add(hf.multiply(-2.6 * Wyrm.SC)).add(hu.multiply(1.6 * Wyrm.SC)).add(hr.multiply(s * 0.8 * Wyrm.SC));
			Vec3d tip = mid2.add(hf.multiply(-2.2 * Wyrm.SC)).add(hu.multiply(0.2 * Wyrm.SC)).add(hr.multiply(s * 0.4 * Wyrm.SC));
			p.segs.add(new Seg(root, mid2, 0.55, 0.35, false));
			p.segs.add(new Seg(mid2, tip, 0.35, 0.05, false));
			p.segs.add(new Seg(hd.at(1.0, 0.9, s * 0.6), hd.at(-0.4, 1.9, s * 1.0), 0.25, 0.05, false));
			p.eyes[(s + 1) / 2] = hd.at(1.6, 0.85, s * 0.95);
		}
		Vec3d jd = rot(hf, hr, -0.15 - 0.95 * jaw);
		Vec3d hinge = hd.at(-0.3, -0.7, 0);
		Vec3d chin = hinge.add(jd.multiply(5.0 * Wyrm.SC));
		p.segs.add(new Seg(hinge, chin, 0.9, 0.45, false));
		p.mouth = hd.at(4.6, -0.3, 0).lerp(chin, 0.35);
		p.mouthDir = hf.add(jd).normalize();
		if (jaw > 0.3) {
			// The fire in its throat showing between the jaws.
			p.segs.add(new Seg(hd.at(0.5, -0.3, 0), p.mouth, 0.5, 0.3, true));
		}
		// Tail: undulating behind it, lying along the ground when it stands.
		Vec3d prevT = hip;
		Vec3d back = f.multiply(-1);
		for (int i = 1; i <= 16; i++) {
			double s = i / 16.0;
			double wave = Math.sin(t * 0.12 - i * 0.45) * (0.4 + 2.6 * s);
			double droop = ground ? -Math.min(1, s * 2.5) * (Wyrm.STAND / Wyrm.SC - 0.8) : -0.6 * s * s;
			Vec3d q = hip.add(back.multiply(1.75 * i * Wyrm.SC)).add(rr.multiply(wave * Wyrm.SC)).add(uu.multiply(droop * Wyrm.SC));
			p.segs.add(new Seg(prevT, q, 2.4 * (1 - (i - 1) / 16.0) + 0.15, 2.4 * (1 - s) + 0.15, false));
			if (i % 2 == 1 && i < 15) {
				p.segs.add(new Seg(q, q.add(uu.multiply(0.9 * (1 - s) * Wyrm.SC + 0.3)), 0.4 * (1 - s) + 0.1, 0.05, true));
			}
			prevT = q;
		}
		// Legs: planted when it stands, drawn up under it in flight.
		for (int front = 0; front < 2; front++) {
			for (int s = -1; s <= 1; s += 2) {
				Vec3d top = front == 1 ? b.at(5, -1.2, s * 2.2) : b.at(-5, -1.2, s * 2.4);
				double l1 = front == 1 ? 3.0 : 3.6, l2 = front == 1 ? 3.0 : 3.4;
				Vec3d foot;
				if (ground) {
					Vec3d gp = top.add(fh.multiply((front == 1 ? 1.2 : -0.6) * Wyrm.SC)).add(rr.multiply(s * 0.6 * Wyrm.SC));
					foot = new Vec3d(gp.x, Math.min(top.y - 1, pos.y - Wyrm.STAND + 0.5), gp.z);
				} else {
					foot = top.add(back.multiply((front == 1 ? 2.0 : 4.0) * Wyrm.SC)).add(uu.multiply(-1.8 * Wyrm.SC));
				}
				Vec3d mid3 = top.lerp(foot, 0.5).add(f.multiply((front == 1 ? -1 : 1) * 1.4 * Wyrm.SC));
				double reach = top.distanceTo(foot) / Wyrm.SC;
				if (reach > l1 + l2) {
					mid3 = top.lerp(foot, 0.5);
				}
				p.segs.add(new Seg(top, mid3, front == 1 ? 1.0 : 1.3, 0.8, false));
				p.segs.add(new Seg(mid3, foot, 0.8, 0.55, false));
				for (int c = -1; c <= 1; c++) {
					p.segs.add(new Seg(foot, foot.add(fh.multiply(1.3 * Wyrm.SC)).add(rr.multiply(c * 0.5 * Wyrm.SC)).add(0, -0.3 * Wyrm.SC, 0), 0.3, 0.05, false));
				}
			}
		}
		// Wings: a beat that is slow and huge in flight, folded on the ground, half-open and beating hard to hover.
		double fold = ground ? 0.85 : 0;
		double amp = hover ? 0.75 : 0.5, rate = hover ? 0.32 : 0.15;
		if (k >= 0) {
			amp = 0.18;
		}
		double theta = ground ? 0.5 : amp * Math.sin(t * rate) + 0.08;
		if (ground && t > Wyrm.ROAR - 10 && t < Wyrm.ROAR + 30 || t >= Wyrm.NOVA - 6 && t < Wyrm.NOVA + 30) {
			// Spread wide to roar, and to breathe the nova.
			fold = 0.2;
			theta = 0.6;
		}
		for (int s = 0; s < 2; s++) {
			double side = s * 2 - 1;
			java.util.function.DoubleFunction<Vec3d> wd = th -> rr.multiply(side * Math.cos(th)).add(uu.multiply(Math.sin(th)));
			Vec3d sh = b.at(4.5, 1.6, side * 2.2);
			Vec3d el = sh.add(wd.apply(theta).multiply(6 * Wyrm.SC * (1 - 0.4 * fold))).add(f.multiply((-0.5 - 3 * fold) * Wyrm.SC));
			Vec3d wr = el.add(wd.apply(theta * 1.2).multiply(8 * Wyrm.SC * (1 - 0.55 * fold))).add(f.multiply((1.5 - 4 * fold) * Wyrm.SC));
			p.segs.add(new Seg(sh, el, 1.2, 0.9, false));
			p.segs.add(new Seg(el, wr, 0.9, 0.6, false));
			double[] len = {16, 14, 11, 8};
			double[] sweep = {0.5, -0.2, -0.9, -1.5};
			Vec3d[] tips = new Vec3d[4];
			for (int i = 0; i < 4; i++) {
				Vec3d dir = wd.apply(theta * 1.35).multiply(Math.cos(sweep[i])).add(f.multiply(Math.sin(sweep[i]))).normalize();
				dir = dir.multiply(1 - fold).add(back.multiply(fold)).normalize();
				Vec3d tip = wr.add(dir.multiply(len[i] * Wyrm.SC * (1 - 0.5 * fold))).add(uu.multiply(-fold * 2 * Wyrm.SC));
				p.segs.add(new Seg(wr, wr.lerp(tip, 0.5), 0.45, 0.3, false));
				p.segs.add(new Seg(wr.lerp(tip, 0.5), tip, 0.3, 0.06, false));
				tips[i] = tip;
			}
			p.segs.add(new Seg(wr, wr.add(f.multiply(1.6 * Wyrm.SC)).add(uu.multiply(0.4 * Wyrm.SC)), 0.35, 0.05, false));
			for (int i = 0; i < 3; i++) {
				p.membrane.add(new Vec3d[]{wr, tips[i], tips[i + 1]});
			}
			p.membrane.add(new Vec3d[]{wr, tips[3], hip.add(rr.multiply(side * 1.5 * Wyrm.SC))});
			p.membrane.add(new Vec3d[]{wr, hip.add(rr.multiply(side * 1.5 * Wyrm.SC)), sh});
			p.membrane.add(new Vec3d[]{sh, el, wr});
			p.tips[s] = tips[0];
		}
		return p;
	}
}
