package com.armageddon.util;

import com.armageddon.util.FossilShapes.Ptero;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pterosaur fossil's skeleton, bone by bone, posed for any moment of its flight. The client draws it; the server
 * uses the perched pose to leave the fossil standing on its spire.
 */
public final class PteroPose {
	private PteroPose() {
	}

	/** Drawing scale of the skeleton. */
	public static final double SC = 1.1;
	private static final Vec3d UP = new Vec3d(0, 1, 0);

	/** One bone: from a to b, radii ra and rb, relative to the bomb. */
	public record Seg(Vec3d a, Vec3d b, double ra, double rb, boolean fire) {
	}

	/** The skeleton's pose at time {@code t}: its bones, wingtips, beak, eyes and frame. */
	public static final class Pose {
		public final List<Seg> segs = new ArrayList<>();
		public final List<Vec3d[]> membrane = new ArrayList<>();
		public Vec3d body, f, u, r, beak;
		public final Vec3d[] tips = new Vec3d[2];
		public final Vec3d[] eyes = new Vec3d[2];
	}

	private static Vec3d frameAt(double t) {
		return Ptero.pos(t);
	}

	/** The skeleton's pose at tick {@code t}, facing {@code h} while on the ground or perched. */
	public static Pose pose(double t, Vec3d h) {
		Pose p = new Pose();
		Vec3d pos = frameAt(t);
		Vec3d vel = frameAt(t + 0.5).subtract(frameAt(t - 0.5));
		Vec3d f = t < Ptero.TAKEOFF || vel.lengthSquared() < 1.0E-3 ? h : vel.normalize();
		if (t >= Ptero.PERCH) {
			f = h;
		}
		Vec3d r = f.crossProduct(UP);
		r = r.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : r.normalize();
		Vec3d u = r.crossProduct(f).normalize();
		// Banking into its turns.
		Vec3d acc = frameAt(t + 1).subtract(frameAt(t).multiply(2)).add(frameAt(t - 1));
		double roll = t > Ptero.TAKEOFF && t < Ptero.PERCH - 20 ? MathHelper.clamp(acc.dotProduct(r) * 6, -0.7, 0.7) : 0;
		Vec3d r2 = r.multiply(Math.cos(roll)).add(u.multiply(Math.sin(roll)));
		Vec3d u2 = u.multiply(Math.cos(roll)).subtract(r.multiply(Math.sin(roll)));
		r = r2;
		u = u2;
		p.body = pos;
		p.f = f;
		p.u = u;
		p.r = r;
		// Wing stroke and fold.
		boolean flying = t >= Ptero.ASSEMBLE && t < Ptero.PERCH;
		double fold;
		if (Ptero.diving(t)) {
			double into = 1;
			for (int d : Ptero.DIVES) {
				if (t >= d && t < d + 24) {
					into = Math.min(1, (t - d) / 6.0) * Math.min(1, (d + 24 - t) / 4.0);
				}
			}
			fold = 0.85 * into;
		} else if (t >= Ptero.PERCH) {
			fold = 0.6 + 0.25 * Math.min(1, (t - Ptero.PERCH) / 20.0);
		} else {
			fold = t < Ptero.ASSEMBLE ? 0.1 : 0;
		}
		double theta = flying ? 0.55 * Math.sin(t * 0.16) + 0.1 : t < Ptero.ASSEMBLE ? 0.15 : 0.35 + 0.05 * Math.sin(t * 0.05);
		if (t >= Ptero.PERCH - 10 && t < Ptero.PERCH + 10) {
			// Back-winging as it lands.
			theta = 0.7 * Math.sin(t * 0.3) + 0.2;
		}
		double stand = t < Ptero.TAKEOFF ? 1 : t >= Ptero.PERCH - 10 ? MathHelper.clamp((t - (Ptero.PERCH - 10)) / 10.0, 0, 1) : 0;
		// Screech: the beak gapes on each dive.
		double gape = 0;
		for (int d : Ptero.DIVES) {
			if (t >= d - 4 && t < d + 22) {
				gape = Math.sin(Math.PI * (t - d + 4) / 26.0);
			}
		}
		final Vec3d ff = f, uu = u, rr = r, po = pos;
		java.util.function.DoubleFunction<java.util.function.DoubleFunction<java.util.function.DoubleFunction<Vec3d>>> L =
				x -> y -> z -> po.add(ff.multiply(x * SC)).add(uu.multiply(y * SC)).add(rr.multiply(z * SC));
		// Torso, ribs, keel, tail.
		Vec3d tail0 = L.apply(-3.2).apply(0).apply(0), chest = L.apply(2.4).apply(0.2).apply(0);
		p.segs.add(new Seg(tail0, chest, 0.75, 0.6, true));
		p.segs.add(new Seg(tail0, L.apply(-5.6).apply(-0.3).apply(0), 0.35, 0.06, true));
		for (int i = 0; i < 4; i++) {
			double x = -1.8 + i * 1.1;
			for (int s = -1; s <= 1; s += 2) {
				Vec3d a0 = L.apply(x).apply(0).apply(0), a1 = L.apply(x).apply(-0.9).apply(s * 1.0), a2 = L.apply(x + 0.3).apply(-1.8).apply(s * 0.4);
				p.segs.add(new Seg(a0, a1, 0.22, 0.2, false));
				p.segs.add(new Seg(a1, a2, 0.2, 0.16, false));
			}
		}
		p.segs.add(new Seg(L.apply(-0.5).apply(-1.9).apply(0), L.apply(1.6).apply(-1.6).apply(0), 0.3, 0.25, false));
		// Neck and skull: long beak, swept-back crest.
		Vec3d n1 = L.apply(4.0).apply(1.3).apply(0), b = L.apply(5.4).apply(1.6).apply(0);
		p.segs.add(new Seg(chest, n1, 0.5, 0.42, true));
		p.segs.add(new Seg(n1, b, 0.42, 0.38, true));
		p.segs.add(new Seg(b.subtract(f.multiply(0.8 * SC)), b.add(f.multiply(1.2 * SC)), 1.0, 0.75, false));
		Vec3d beak = b.add(f.multiply(7.5 * SC)).add(u.multiply(-0.5 * SC));
		Vec3d jaw = b.add(f.multiply(7.0 * SC)).add(u.multiply((-1.0 - gape * 1.8) * SC));
		p.segs.add(new Seg(b.add(f.multiply(1.0 * SC)), beak, 0.6, 0.08, false));
		p.segs.add(new Seg(b.add(f.multiply(0.8 * SC)).add(u.multiply(-0.5 * SC)), jaw, 0.42, 0.06, false));
		p.segs.add(new Seg(b.add(u.multiply(0.5 * SC)), b.subtract(f.multiply(4.4 * SC)).add(u.multiply(1.9 * SC)), 0.55, 0.1, false));
		p.beak = beak;
		for (int s = 0; s < 2; s++) {
			p.eyes[s] = b.add(f.multiply(0.5 * SC)).add(u.multiply(0.4 * SC)).add(r.multiply((s * 2 - 1) * 0.75 * SC));
		}
		// Wings: humerus, forearm, the very long wing finger, little clawed fingers; the ghost membrane between.
		for (int s = 0; s < 2; s++) {
			double side = s * 2 - 1;
			java.util.function.DoubleFunction<Vec3d> wd = th -> rr.multiply(side * Math.cos(th)).add(uu.multiply(Math.sin(th)));
			Vec3d sh = L.apply(1.2).apply(0.3).apply(side * 0.9);
			Vec3d el = sh.add(wd.apply(theta).multiply(3.2 * SC * (1 - 0.5 * fold))).add(f.multiply((-0.6 - 1.5 * fold) * SC));
			Vec3d wr = el.add(wd.apply(theta * 1.3).multiply(4.2 * SC * (1 - 0.6 * fold))).add(f.multiply((0.8 - 2.5 * fold) * SC));
			Vec3d tip = wr.add(wd.apply(theta * 1.7).multiply(10 * SC * (1 - 0.75 * fold))).add(f.multiply((-3.5 - 4 * fold) * SC)).add(u.multiply(fold * SC));
			p.segs.add(new Seg(sh, el, 0.55, 0.45, true));
			p.segs.add(new Seg(el, wr, 0.45, 0.35, true));
			Vec3d mid = wr.lerp(tip, 0.5).add(f.multiply(0.6 * SC));
			p.segs.add(new Seg(wr, mid, 0.35, 0.22, true));
			p.segs.add(new Seg(mid, tip, 0.22, 0.05, true));
			for (int cl = 0; cl < 3; cl++) {
				p.segs.add(new Seg(wr, wr.add(f.multiply(0.9 * SC)).add(wd.apply(theta).multiply((cl - 1) * 0.3 * SC)).add(u.multiply(-0.3 * SC)), 0.12, 0.03, false));
			}
			Vec3d hip = L.apply(-2.8).apply(-0.2).apply(side * 0.6);
			p.membrane.add(new Vec3d[]{sh, wr, mid});
			p.membrane.add(new Vec3d[]{sh, mid, tip});
			p.membrane.add(new Vec3d[]{sh, tip, hip});
			p.tips[s] = tip;
			// Legs: trailing back in flight, standing to perch.
			Vec3d kneeFly = hip.add(f.multiply(-1.8 * SC)).add(u.multiply(-0.6 * SC)).add(r.multiply(side * 0.3 * SC));
			Vec3d footFly = kneeFly.add(f.multiply(-1.8 * SC)).add(u.multiply(-0.2 * SC));
			Vec3d kneeSit = hip.add(f.multiply(0.6 * SC)).add(UP.multiply(-1.6 * SC)).add(r.multiply(side * 0.2 * SC));
			Vec3d footSit = kneeSit.add(f.multiply(-0.4 * SC)).add(UP.multiply(-1.7 * SC));
			Vec3d knee = kneeFly.lerp(kneeSit, stand), foot = footFly.lerp(footSit, stand);
			p.segs.add(new Seg(hip, knee, 0.4, 0.32, false));
			p.segs.add(new Seg(knee, foot, 0.32, 0.2, false));
			for (int cl = -1; cl <= 1; cl++) {
				p.segs.add(new Seg(foot, foot.add(f.multiply(0.8 * SC)).add(r.multiply(cl * 0.35 * SC)).add(UP.multiply(-0.2 * SC)), 0.1, 0.03, false));
			}
		}
		return p;
	}
}
