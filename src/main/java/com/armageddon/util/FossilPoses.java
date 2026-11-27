package com.armageddon.util;

import com.armageddon.util.FossilShapes.Amber;
import com.armageddon.util.FossilShapes.Sabre;
import com.armageddon.util.PteroPose.Seg;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeletons posed for any moment: the Sabre-Tooth (walking, crouching, leaping, roaring, sinking) and the Amber
 * fossil's giant dragonfly (still in the amber, then flying). The client draws them bone by bone; the server uses a
 * pose to leave the fossil behind in blocks. Positions are relative to the bomb's ground.
 */
public final class FossilPoses {
	private FossilPoses() {
	}

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	/** A body frame: origin, forward, up and right, and a scale; {@code at} maps body units to the world. */
	private record Frame(Vec3d o, Vec3d f, Vec3d u, Vec3d r, double sc) {
		Vec3d at(double x, double y, double z) {
			return o.add(f.multiply(x * sc)).add(u.multiply(y * sc)).add(r.multiply(z * sc));
		}
	}

	private static Vec3d rightOf(Vec3d f) {
		Vec3d r = f.crossProduct(UP);
		return r.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : r.normalize();
	}

	/** Turns {@code v} about {@code axis} (unit) by {@code ang}. */
	static Vec3d rotate(Vec3d v, Vec3d axis, double ang) {
		double c = Math.cos(ang), s = Math.sin(ang);
		return v.multiply(c).add(axis.crossProduct(v).multiply(s)).add(axis.multiply(axis.dotProduct(v) * (1 - c)));
	}

	// ====================================================================== SABRE-TOOTH

	/** The sabre-tooth's pose: bones, eyes, the two sabre tips and the jaw. */
	public static final class Cat {
		public final List<Seg> segs = new ArrayList<>();
		public final Vec3d[] eyes = new Vec3d[2];
		public final Vec3d[] sabres = new Vec3d[2];
		public Vec3d mouth, body, f, head;
		/** Paws, for footfalls. */
		public final Vec3d[] paws = new Vec3d[4];
		/** The skull's bones alone (for the ghost skull and the fossil left behind). */
		public final List<Seg> skull = new ArrayList<>();
		/** Capsules of the ghost of its flesh: torso, neck, head, legs, tail (radii in body units). */
		public final List<Seg> flesh = new ArrayList<>();
	}

	public static final double CAT_SC = 1.55;

	/**
	 * The cat at tick {@code t}. {@code gait} is the distance it has walked (drives the legs), {@code ground} the height of
	 * the ground under it relative to the bomb, {@code h} the bomb's heading.
	 */
	public static Cat cat(double t, double gait, double ground, Vec3d h) {
		Cat p = new Cat();
		Vec3d local = Sabre.pos(t);
		Vec3d floor = FossilShapes.frame(h, local.x, 0, local.z).add(0, ground, 0);
		Vec3d base = floor.add(0, local.y, 0);
		Vec3d fh = FossilShapes.frame(h, Sabre.facing(t)).normalize();
		Vec3d r = rightOf(fh);
		// Pitch: up as it claws its way out and on the way up a leap, nose down on the way down; reared up to roar at the end.
		double pitch = 0;
		if (t < Sabre.EMERGED) {
			pitch = 0.55 * (1 - FossilShapes.smooth((t - Sabre.EMERGE) / (double) (Sabre.EMERGED - Sabre.EMERGE)));
		} else if (Sabre.leaping(t)) {
			double vy = Sabre.pos(t + 1).y - Sabre.pos(t - 1).y;
			pitch = MathHelper.clamp(vy * 0.35, -0.5, 0.5);
		} else if (t >= Sabre.PETRIFY - 20) {
			pitch = 0.22 * FossilShapes.smooth((t - (Sabre.PETRIFY - 20)) / 14.0);
		}
		Vec3d f = rotate(fh, r, pitch);
		Vec3d u = r.crossProduct(f).normalize();
		// Pitched about the middle of its body, not its feet, so it does not lurch as it tips.
		double mid0 = 6.2 * CAT_SC;
		Frame b = new Frame(base.add(0, mid0, 0).subtract(u.multiply(mid0)), f, u, r, CAT_SC);
		p.body = b.at(0, 6.2, 0);
		p.f = fh;
		boolean crouch = Sabre.crouching(t);
		boolean air = Sabre.leaping(t);
		// The head: low while prowling, up to roar.
		double g = Sabre.gape(t);
		double headUp = crouch ? -1.0 : -0.6 + 1.8 * g;
		double headPitch = -0.15 + 0.45 * g + (crouch ? -0.15 : 0);
		// Spine, slightly arched, and a short bobbed tail.
		Vec3d hip = b.at(-4.2, 6.0, 0), mid = b.at(-0.6, 6.7, 0), shoulder = b.at(3.4, 6.5, 0);
		p.segs.add(new Seg(hip, mid, 0.55, 0.6, false));
		p.segs.add(new Seg(mid, shoulder, 0.6, 0.55, false));
		double wag = Math.sin(gait * 0.3) * 0.4;
		Vec3d t1 = b.at(-5.6, 5.6, wag * 0.5), t2 = b.at(-6.6, 4.9, wag);
		p.segs.add(new Seg(hip, t1, 0.4, 0.32, false));
		p.segs.add(new Seg(t1, t2, 0.32, 0.16, false));
		// Ribs: a deep barrel.
		for (int i = 0; i < 7; i++) {
			double x = -2.4 + i * 0.85;
			double y = 6.6 - Math.abs(x + 0.4) * 0.05;
			double deep = 3.3 - Math.abs(i - 3) * 0.25;
			for (int s = -1; s <= 1; s += 2) {
				Vec3d a0 = b.at(x, y, 0), a1 = b.at(x, y - deep * 0.45, s * 1.6), a2 = b.at(x + 0.25, y - deep, s * 0.9);
				p.segs.add(new Seg(a0, a1, 0.2, 0.18, false));
				p.segs.add(new Seg(a1, a2, 0.18, 0.14, false));
			}
		}
		// Shoulder blades and pelvis.
		for (int s = -1; s <= 1; s += 2) {
			p.segs.add(new Seg(b.at(3.6, 6.8, s * 0.9), b.at(2.6, 4.9, s * 1.45), 0.45, 0.3, false));
			p.segs.add(new Seg(b.at(-3.6, 6.2, s * 0.8), b.at(-4.6, 4.8, s * 1.25), 0.45, 0.32, false));
		}
		// Legs: walking feet planted on the ground; folded under in a crouch; stretched out in a leap.
		double stride = 1.6;
		int leg = 0;
		for (int front = 1; front >= 0; front--) {
			for (int s = -1; s <= 1; s += 2) {
				boolean isFront = front == 1;
				double hx = isFront ? 3.0 : -4.0;
				Vec3d top = b.at(hx, isFront ? 5.4 : 5.6, s * 1.4);
				// One stride cycle per 4 x stride of travel, so a planted foot keeps pace with the ground.
				double ph = gait * (Math.PI * 2 / (4 * stride)) + (isFront ? 0 : Math.PI) + (s > 0 ? Math.PI * 0.5 : 0);
				Vec3d foot;
				if (air) {
					foot = isFront ? b.at(hx + 3.4, 2.4, s * 1.2) : b.at(hx - 3.6, 2.6, s * 1.3);
				} else if (t < Sabre.EMERGED) {
					// Climbing out: forepaws reaching and clawing at the rim.
					double claw = Math.sin(t * 0.18 + s) * 1.2;
					foot = isFront ? b.at(hx + 2.6 + claw, 1.2 + Math.max(0, claw), s * 1.5) : b.at(hx - 0.6, 0.2, s * 1.4);
				} else {
					// Feet on the real ground, even while the body is crouched low or still dipped from a landing.
					double swing = -Math.cos(ph) * stride, lift = Math.max(0, Math.sin(ph)) * 0.9;
					Vec3d planted = floor.add(fh.multiply((hx + swing) * CAT_SC)).add(r.multiply(s * 1.5 * CAT_SC)).add(0, lift * CAT_SC, 0);
					foot = crouch ? floor.add(fh.multiply((hx + (isFront ? 0.8 : -0.4)) * CAT_SC)).add(r.multiply(s * 1.6 * CAT_SC)) : planted;
				}
				double l1 = (isFront ? 3.1 : 3.3) * CAT_SC, l2 = (isFront ? 3.0 : 3.2) * CAT_SC;
				Vec3d knee = knee(top, foot, l1, l2, fh, isFront ? -1 : 1);
				Vec3d paw = foot.add(fh.multiply(0.9 * CAT_SC));
				p.segs.add(new Seg(top, knee, isFront ? 0.5 : 0.55, 0.4, false));
				p.segs.add(new Seg(knee, foot, 0.4, 0.32, false));
				p.flesh.add(new Seg(top, knee, 1.3, 0.8, false));
				p.flesh.add(new Seg(knee, foot, 0.75, 0.5, false));
				p.segs.add(new Seg(foot, paw, 0.36, 0.26, false));
				for (int c = -1; c <= 1; c++) {
					p.segs.add(new Seg(paw, paw.add(fh.multiply(0.5 * CAT_SC)).add(r.multiply(c * 0.25 * CAT_SC)).add(0, -0.25 * CAT_SC, 0), 0.12, 0.04, false));
				}
				p.paws[leg++] = paw;
			}
		}
		// Neck and skull.
		Vec3d neck = b.at(5.4, 6.8 + headUp * 0.6, 0), head = b.at(6.2, 7.0 + headUp, 0);
		p.segs.add(new Seg(shoulder, neck, 0.6, 0.55, false));
		p.segs.add(new Seg(neck, head, 0.55, 0.5, false));
		Vec3d hf = rotate(f, r, headPitch), hu = r.crossProduct(hf).normalize();
		skull(p, head, hf, hu, r, CAT_SC, 1.0, g);
		p.segs.addAll(p.skull);
		// The ghost of its flesh: a deep chest, a lean waist, heavy shoulders, a thick neck, the legs.
		p.flesh.add(new Seg(b.at(-3.8, 5.8, 0), b.at(-1.0, 5.4, 0), 2.0, 2.3, false));
		p.flesh.add(new Seg(b.at(-1.0, 5.4, 0), b.at(3.2, 5.6, 0), 2.3, 2.6, false));
		p.flesh.add(new Seg(shoulder, neck, 2.0, 1.6, false));
		p.flesh.add(new Seg(neck, head.add(hf.multiply(0.6 * CAT_SC)), 1.6, 1.5, false));
		p.flesh.add(new Seg(hip, t2, 0.8, 0.4, false));
		return p;
	}

	/**
	 * A sabre-tooth's skull at {@code head} facing {@code hf} (up {@code hu}, right {@code r}), {@code sc} blocks to a
	 * unit, its radii multiplied by {@code thick}, jaws open {@code gape}: a long low cranium with a high crest, flared
	 * cheekbones, deep sockets, a short heavy muzzle, the two long flat sabres and a lower jaw that drops very wide.
	 * Adds the bones to {@code p.skull} and fills in its eyes, sabres and mouth.
	 */
	static void skull(Cat p, Vec3d head, Vec3d hf, Vec3d hu, Vec3d r, double sc, double thick, double gape) {
		Frame k = new Frame(head, hf, hu, r, sc);
		List<Seg> o = p.skull;
		double w = thick;
		p.head = head;
		// Cranium: a low dome tapering back to the crest; the crest itself.
		o.add(new Seg(k.at(-1.2, 0.3, 0), k.at(0.6, 0.55, 0), 0.85 * w, 1.15 * w, false));
		o.add(new Seg(k.at(0.6, 0.55, 0), k.at(1.7, 0.35, 0), 1.15 * w, 0.95 * w, false));
		o.add(new Seg(k.at(-1.5, 1.0, 0), k.at(1.0, 1.45, 0), 0.22 * w, 0.14 * w, false));
		// Muzzle: short and deep, the nose bridge over it.
		o.add(new Seg(k.at(1.6, 0.1, 0), k.at(3.1, -0.35, 0), 0.9 * w, 0.62 * w, false));
		o.add(new Seg(k.at(1.4, 0.75, 0), k.at(3.0, 0.15, 0), 0.32 * w, 0.22 * w, false));
		for (int s = -1; s <= 1; s += 2) {
			// Cheekbones flaring wide, and the ring of each socket.
			o.add(new Seg(k.at(-0.3, -0.25, s * 1.0), k.at(1.5, -0.2, s * 1.25), 0.3 * w, 0.32 * w, false));
			o.add(new Seg(k.at(1.5, -0.2, s * 1.25), k.at(2.0, 0.35, s * 0.85), 0.3 * w, 0.22 * w, false));
			o.add(new Seg(k.at(2.0, 0.35, s * 0.85), k.at(1.5, 0.9, s * 0.55), 0.2 * w, 0.2 * w, false));
			// The back of the skull flaring to the jaw joint.
			o.add(new Seg(k.at(-1.2, 0.2, 0), k.at(-0.6, -0.6, s * 0.95), 0.35 * w, 0.3 * w, false));
		}
		// The sabres: long, flat, curving down and a little back; a row of small incisors between them.
		for (int s = 0; s < 2; s++) {
			double side = s * 2 - 1;
			Vec3d root = k.at(2.55, -0.55, side * 0.5), m1 = k.at(2.75, -2.2, side * 0.55), m2 = k.at(2.55, -3.5, side * 0.52), tip = k.at(2.1, -4.4, side * 0.48);
			o.add(new Seg(root, m1, 0.36 * w, 0.3 * w, true));
			o.add(new Seg(m1, m2, 0.3 * w, 0.2 * w, true));
			o.add(new Seg(m2, tip, 0.2 * w, 0.04 * w, true));
			p.sabres[s] = tip;
			p.eyes[s] = k.at(1.55, 0.45, side * 0.78);
		}
		for (int i = -1; i <= 1; i++) {
			o.add(new Seg(k.at(3.05, -0.55, i * 0.22), k.at(3.15, -1.0, i * 0.22), 0.1 * w, 0.05 * w, true));
		}
		// The lower jaw: hinged at the back, dropping very wide, with a tall coronoid hook and a broad chin.
		Vec3d hinge = k.at(-0.4, -0.8, 0);
		Vec3d jaw = rotate(hf, r, -0.15 - 1.35 * gape), ju = r.crossProduct(jaw).normalize();
		Frame j = new Frame(hinge, jaw, ju, r, sc);
		for (int s = -1; s <= 1; s += 2) {
			o.add(new Seg(j.at(0, 0, s * 0.85), j.at(3.0, -0.15, s * 0.42), 0.33 * w, 0.26 * w, false));
			o.add(new Seg(j.at(0.5, 0, s * 0.8), j.at(0.9, 0.9, s * 0.75), 0.18 * w, 0.1 * w, false));
		}
		o.add(new Seg(j.at(3.0, -0.15, -0.42), j.at(3.0, -0.15, 0.42), 0.3 * w, 0.3 * w, false));
		o.add(new Seg(j.at(3.0, -0.15, 0), j.at(3.2, -0.6, 0), 0.32 * w, 0.2 * w, false));
		p.mouth = hinge.lerp(k.at(2.8, -0.9, 0), 0.6);
	}

	/**
	 * The colossal ghost of its skull at tick {@code t}: rising over the cat with its jaws open wide, then lunging
	 * forward and down as they slam shut on the land. Relative to the bomb's ground.
	 */
	public static Cat giantSkull(double t, Vec3d h) {
		Cat p = new Cat();
		double lunge = FossilShapes.smooth((t - (Sabre.BITE - 8)) / 8.0);
		double rise = FossilShapes.smooth((t - Sabre.GIANT) / 40.0);
		Vec3d at = FossilShapes.frame(h, MathHelper.lerp(lunge, -Sabre.GIANT_BACK, 8), MathHelper.lerp(lunge, Sabre.GIANT_UP + 6 * rise - 6, 16), 0);
		double pitch = MathHelper.lerp(lunge, 0.3 + 0.1 * Math.sin(t * 0.08), -0.45);
		Vec3d r = rightOf(h);
		Vec3d hf = rotate(h, r, pitch), hu = r.crossProduct(hf).normalize();
		skull(p, at, hf, hu, r, CAT_SC * Sabre.GIANT_SCALE, Sabre.GIANT_SCALE, Sabre.giantGape(t));
		p.segs.addAll(p.skull);
		p.f = hf;
		return p;
	}

	/** The knee of a two-bone leg from {@code a} to {@code c}, bending forward ({@code bend} 1) or back (-1). */
	private static Vec3d knee(Vec3d a, Vec3d c, double l1, double l2, Vec3d fh, int bend) {
		Vec3d d = c.subtract(a);
		double len = d.length();
		if (len < 1.0E-4) {
			return a.add(0, -l1, 0);
		}
		Vec3d dir = d.multiply(1 / len);
		len = Math.min(len, (l1 + l2) * 0.999);
		// Distance along the line to the knee's foot, and how far it stands off the line.
		double x = (l1 * l1 - l2 * l2 + len * len) / (2 * len);
		double hgt = Math.sqrt(Math.max(0, l1 * l1 - x * x));
		Vec3d side = rightOf(fh);
		Vec3d off = dir.crossProduct(side);
		if (off.lengthSquared() < 1.0E-6) {
			off = fh;
		}
		off = off.normalize();
		if (off.dotProduct(fh) * bend < 0) {
			off = off.multiply(-1);
		}
		return a.add(dir.multiply(x)).add(off.multiply(hgt));
	}

	// ====================================================================== DRAGONFLY

	/** The dragonfly's pose: bones (abdomen segments flagged in turn for the banding), wings, eyes. */
	public static final class Fly {
		public final List<Seg> segs = new ArrayList<>();
		/** Four wings, each as {root, tip, trailing tip, trailing root}. */
		public final List<Vec3d[]> wings = new ArrayList<>();
		public final Vec3d[] eyes = new Vec3d[2];
		public Vec3d body, f, u, r, tail;
		/** Its scale: bone radii are multiplied by this when drawn. */
		public double sc = FLY_SC;
	}

	public static final double FLY_SC = 1.0;

	/** The dragonfly at tick {@code t}; {@code still} holds it in its resting pose (sealed in amber). */
	public static Fly fly(double t, Vec3d h, boolean still) {
		return fly(t, h, still, -1);
	}

	/**
	 * The dragonfly at tick {@code t}, or brood dragonfly {@code brood} (0 and up; -1 for the big one), which is
	 * smaller and follows the big one's path.
	 */
	public static Fly fly(double t, Vec3d h, boolean still, int brood) {
		Fly p = new Fly();
		final double sc = brood < 0 ? FLY_SC : 0.42;
		p.sc = sc;
		java.util.function.DoubleFunction<Vec3d> path = brood < 0 ? Amber::pos : tt -> Amber.brood(brood, tt);
		Vec3d pos = FossilShapes.frame(h, path.apply(t));
		Vec3d f = h;
		double roll = 0, pitch = 0;
		if (!still && t > Amber.SHATTER) {
			Vec3d vel = FossilShapes.frame(h, path.apply(t + 0.5).subtract(path.apply(t - 0.5)));
			if (vel.lengthSquared() > 1.0E-3) {
				f = vel.normalize();
				Vec3d flat = new Vec3d(f.x, 0, f.z);
				pitch = Math.asin(MathHelper.clamp(f.y, -0.8, 0.8));
				f = flat.lengthSquared() > 1.0E-4 ? flat.normalize() : h;
			}
			Vec3d acc = FossilShapes.frame(h, path.apply(t + 1).subtract(path.apply(t).multiply(2)).add(path.apply(t - 1)));
			roll = MathHelper.clamp(acc.dotProduct(rightOf(f)) * 4, -0.8, 0.8);
		}
		Vec3d r = rightOf(f);
		Vec3d fp = rotate(f, r, pitch);
		Vec3d u = r.crossProduct(fp).normalize();
		Vec3d r2 = rotate(r, fp, roll), u2 = rotate(u, fp, roll);
		Frame b = new Frame(pos, fp, u2, r2, sc);
		p.body = pos;
		p.f = fp;
		p.u = u2;
		p.r = r2;
		// Head and the great compound eyes.
		p.segs.add(new Seg(b.at(2.4, 0.1, 0), b.at(3.5, 0.0, 0), 0.9, 0.75, false));
		for (int s = 0; s < 2; s++) {
			p.eyes[s] = b.at(3.3, 0.35, (s * 2 - 1) * 0.75);
		}
		// Thorax.
		p.segs.add(new Seg(b.at(0.2, 0.0, 0), b.at(2.4, 0.15, 0), 1.0, 1.05, false));
		// Abdomen: ten segments tapering away, curving down a little; the tip lifts and dips as it flies.
		double curl = still ? 0.05 : 0.08 + 0.05 * Math.sin(t * 0.21);
		Vec3d prev = b.at(0.2, 0, 0);
		for (int i = 1; i <= 10; i++) {
			double x = 0.2 - i * 0.98;
			Vec3d at = b.at(x, -curl * (x * x) * 0.12, 0);
			p.segs.add(new Seg(prev, at, 0.6 - i * 0.025, 0.58 - i * 0.03, i % 2 == 0));
			prev = at;
		}
		p.tail = prev;
		// Claspers at the tip.
		for (int s = -1; s <= 1; s += 2) {
			p.segs.add(new Seg(prev, prev.add(fp.multiply(-0.9 * sc)).add(r2.multiply(s * 0.3 * sc)), 0.15, 0.05, false));
		}
		// Six legs, held basket-wise under the thorax.
		for (int i = 0; i < 3; i++) {
			for (int s = -1; s <= 1; s += 2) {
				Vec3d hip = b.at(0.8 + i * 0.6, -0.7, s * 0.4);
				Vec3d k = b.at(1.2 + i * 0.7, -1.6, s * 0.9);
				p.segs.add(new Seg(hip, k, 0.15, 0.12, false));
				p.segs.add(new Seg(k, b.at(2.0 + i * 0.5, -1.9, s * 0.5), 0.12, 0.06, false));
			}
		}
		// Wings: front and hind pairs beating out of step; held flat and slightly forward at rest.
		double beat = still ? 0 : t * 1.4;
		for (int pair = 0; pair < 2; pair++) {
			double rootX = pair == 0 ? 1.9 : 0.9, len = pair == 0 ? 9.6 : 9.0, chord = pair == 0 ? 1.7 : 2.2;
			double flap = still ? 0.06 : 0.5 * Math.sin(beat + pair * 1.3) + 0.08;
			double sweep = pair == 0 ? 0.12 : -0.1;
			for (int s = -1; s <= 1; s += 2) {
				Vec3d root = b.at(rootX, 0.9, s * 0.6);
				Vec3d span = r2.multiply(s * Math.cos(flap)).add(u2.multiply(Math.sin(flap))).add(fp.multiply(sweep)).normalize();
				Vec3d tip = root.add(span.multiply(len * sc));
				Vec3d back = fp.multiply(-chord * sc);
				p.wings.add(new Vec3d[]{root, tip, tip.add(back.multiply(0.45)).subtract(span.multiply(0.6)), root.add(back)});
				p.segs.add(new Seg(root, tip, 0.12, 0.05, false));
			}
		}
		return p;
	}
}
