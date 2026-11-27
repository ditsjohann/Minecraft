package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the fossil bombs (the Mammoth, the Leviathan, the Pterosaur, the Amber, the Sabre-Tooth
 * and the Ammonite), shared by the server events and the client effects.
 */
public final class FossilShapes {
	private FossilShapes() {
	}

	static double smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return u * u * (3 - 2 * u);
	}

	public static double hash(int i, int k) {
		double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	// ====================================================================== MAMMOTH

	/**
	 * A woolly mammoth's skeleton rises out of a frozen fissure, its eyes catch a cold blue light, it trumpets, and an
	 * ice age rolls out over the land. Then it stomps, twice.
	 */
	public static final class Mammoth {
		public static final int RISE = 30, RISEN = 190, EYES = 200, TRUMPET = 230, FREEZE_END = 430, STOMP = 450, STOMP2 = 510, FINALE = 570,
				SPIKES = 590, SPIKES_END = 690, ENTOMB = 710, END = 900;
		/** Lines of ice spikes racing out in the finale, and how far they run. */
		public static final int SPIKE_LINES = 12;
		public static final double SPIKE_REACH = 72;

		/** How far the finale's spike lines have raced out at tick {@code t}. */
		public static double spikes(double t) {
			return SPIKE_REACH * MathHelper.clamp((t - SPIKES) / (double) (SPIKES_END - SPIKES), 0.0, 1.0);
		}
		public static final double R = 72;

		private Mammoth() {
		}

		/** How far the ice age has reached at tick {@code t}. */
		public static double front(double t) {
			return R * smooth((t - TRUMPET) / (FREEZE_END - TRUMPET));
		}

		/** The skull's centre, relative to the bomb's ground, along {@code heading}. */
		public static Vec3d skull(Vec3d heading) {
			return heading.multiply(15).add(0, 20, 0);
		}
	}

	// ====================================================================== LEVIATHAN

	/**
	 * An ancient sea floods back over the land as a ghost, and the fossil of the serpent that swam it breaches out of
	 * the ground in great arches. Its skull rears up out of the sea and the sea rolls out in front of it as one great wave.
	 */
	public static final class Leviathan {
		public static final int BREACH = 20, BREACHED = 200, SEA_IN = 30, SEA_FULL = 150, EYES = 210, WAVE = 250, WAVE_END = 430, DRAIN = 250,
				END = 520;
		/** The ghost sea's radius and the height of its surface over the ground. */
		public static final double SEA_R = 64, SEA_H = 11;
		/** The serpent runs from u = TAIL to u = HEAD along its heading. */
		public static final double TAIL = -52, HEAD = 34;
		/** How far the tidal wave rolls from the skull, the half-angle of its arc, and its crest's height. */
		public static final double WAVE_REACH = 100, WAVE_ARC = 1.15, CREST = 15;

		private Leviathan() {
		}

		/** Height of the spine over the ground at {@code u}: arches out of the earth, rearing up for the neck. */
		public static double spine(double u) {
			if (u > 18) {
				double k = (u - 18) / (HEAD - 18);
				return 8 + 20 * Math.sin(k * Math.PI * 0.5);
			}
			double wave = Math.sin((u + 52) / 70.0 * Math.PI * 3.5 - 0.6);
			return 11 * wave + 2;
		}

		/** How far (0..1) the sea has come in. */
		public static double sea(double t) {
			return smooth((t - SEA_IN) / (SEA_FULL - SEA_IN)) * (1 - smooth((t - DRAIN) / 70.0));
		}

		/** How far the tidal wave has rolled out from under the skull at tick {@code t}. */
		public static double wave(double t) {
			double u = MathHelper.clamp((t - WAVE) / (WAVE_END - WAVE), 0.0, 1.0);
			return 6 + (WAVE_REACH - 6) * (1 - (1 - u) * (1 - u));
		}
	}

	// ====================================================================== PTEROSAUR

	/**
	 * A pterosaur's bones burst out of the ground and assemble in the air. It takes off, circles high, and dives three
	 * times: each dive ends in a screech and a blast where it pulls up. Then it lands on a spire of bone and crumbles.
	 */
	public static final class Ptero {
		public static final int ASSEMBLE = 70, TAKEOFF = 110, DIVE = 60, PERCH = 560, CRUMBLE = 620, END = 700;
		public static final int[] DIVES = {220, 330, 440};
		public static final double ORBIT = 38, HIGH = 44, SPIRE = 16, SPAN = 15;

		private Ptero() {
		}

		/** Where dive {@code k} strikes the ground, relative to the bomb. */
		public static Vec3d target(int k) {
			double a = k * 2.3 + 0.7;
			double r = 22 + 10 * hash(k, 7);
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		/** The orbit point at tick {@code t}. */
		static Vec3d orbit(double t) {
			double a = t * 0.028;
			return new Vec3d(Math.cos(a) * ORBIT, HIGH + 5 * Math.sin(t * 0.05), Math.sin(a) * ORBIT);
		}

		/** The body's position at tick {@code t}, relative to the bomb's ground. */
		public static Vec3d pos(double t) {
			if (t < TAKEOFF) {
				// Assembling just over the fissure, then a heavy flap upward.
				double lift = smooth((t - ASSEMBLE) / (TAKEOFF - ASSEMBLE));
				return new Vec3d(0, 9 + 6 * lift, 0);
			}
			Vec3d start = new Vec3d(0, 15, 0);
			if (t < TAKEOFF + 60) {
				// Spiralling up to the orbit.
				double u = smooth((t - TAKEOFF) / 60.0);
				return start.lerp(orbit(t), u);
			}
			for (int k = 0; k < DIVES.length; k++) {
				double d = t - DIVES[k];
				if (d >= 0 && d < DIVE) {
					// Fold in, drop on the target along a curve, skim it, then climb back to the orbit.
					Vec3d from = orbit(DIVES[k]), hit = target(k).add(0, 3, 0), to = orbit(DIVES[k] + DIVE);
					if (d < 24) {
						double u = d / 24.0;
						u = u * u;
						Vec3d mid = from.lerp(hit, 0.5).add(0, 10, 0);
						return bezier(from, mid, hit, u);
					}
					double u = smooth((d - 24) / (DIVE - 24));
					Vec3d mid = hit.lerp(to, 0.3).add(0, 2, 0);
					return bezier(hit, mid, to, u);
				}
			}
			if (t >= PERCH - 40) {
				double u = smooth((t - (PERCH - 40)) / 40.0);
				return orbit(Math.min(t, PERCH - 40)).lerp(new Vec3d(0, SPIRE + 3.5, 0), u);
			}
			return orbit(t);
		}

		private static Vec3d bezier(Vec3d a, Vec3d b, Vec3d c, double u) {
			return a.multiply((1 - u) * (1 - u)).add(b.multiply(2 * u * (1 - u))).add(c.multiply(u * u));
		}

		/** True while the pterosaur is plunging on its target with its wings folded. */
		public static boolean diving(double t) {
			for (int d : DIVES) {
				if (t >= d && t < d + 24) {
					return true;
				}
			}
			return false;
		}
	}

	/** A point given in a heading's frame (x forward, y up, z right) turned into world offsets. */
	public static Vec3d frame(Vec3d heading, double x, double y, double z) {
		return new Vec3d(heading.x * x - heading.z * z, y, heading.z * x + heading.x * z);
	}

	public static Vec3d frame(Vec3d heading, Vec3d local) {
		return frame(heading, local.x, local.y, local.z);
	}

	// ====================================================================== AMBER

	/**
	 * A drop of amber the size of a house wells up out of the ground with a giant dragonfly caught inside it. Resin
	 * floods out over the land; the amber cracks and shatters, the dragonfly comes alive and strafes the land with a
	 * swarm, then resin wells up round it again and it is left sealed in amber.
	 */
	public static final class Amber {
		public static final int RISE = 20, RISEN = 140, CRACK = 200, OOZE = 230, OOZE_END = 420, SHATTER = 450, FREE = 480, FLY_END = 650,
				ENTOMB = 680, END = 800;
		/** Strafing runs: each dives low across the land for {@link #ZIP} ticks. */
		public static final int[] ZIPS = {505, 560, 615};
		public static final int ZIP = 30;
		/** How far the resin floods. */
		public static final double R = 40;
		/** The drop: centre height and radii across (right), up and along (forward). */
		public static final double CY = 10.5, RX = 12, RY = 10.5, RZ = 9.5;
		/** Where the dragonfly is sealed at the end, over the bomb. */
		public static final double SEAL_Y = 12;

		private Amber() {
		}

		/** Normalised distance from the drop's centre (1 on its skin) of a point in the heading's frame relative to the centre. */
		public static double drop(double x, double y, double z) {
			double yy = y / RY;
			double pinch = yy > 0 ? 1 - 0.42 * yy * yy : 1 - 0.1 * yy * yy;
			return Math.sqrt(Math.pow(x / (RZ * pinch), 2) + yy * yy + Math.pow(z / (RX * pinch), 2));
		}

		/** How far the resin has flooded at tick {@code t}. */
		public static double front(double t) {
			return R * smooth((t - OOZE) / (double) (OOZE_END - OOZE));
		}

		/** The free flight's figure of eight, in the heading's frame (x forward, z right). */
		static Vec3d eight(double t) {
			double a = (t - FREE) * 0.036;
			return new Vec3d(26 * Math.sin(a), 18 + 3 * Math.sin(a * 1.7), 15 * Math.sin(2 * a));
		}

		/** Where strafing run {@code k} sweeps lowest, in the heading's frame. */
		public static Vec3d strafe(int k) {
			double a = k * 2.2 + 0.4;
			double r = 10 + 8 * hash(k, 31);
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		/** The dragonfly's body in the heading's frame, relative to the bomb's ground. */
		public static Vec3d pos(double t) {
			Vec3d inside = new Vec3d(1.5, CY, 0);
			if (t < SHATTER) {
				return inside;
			}
			if (t < FREE) {
				double u = smooth((t - SHATTER) / (double) (FREE - SHATTER));
				return inside.lerp(eight(FREE), u).add(0, Math.sin(Math.PI * u) * 3, 0);
			}
			for (int k = 0; k < ZIPS.length; k++) {
				double d = t - ZIPS[k];
				if (d >= 0 && d < ZIP) {
					double u = d / ZIP;
					// A curve from the figure of eight down to skim the ground at the strafe point (about 3 up) and back.
					Vec3d s = eight(ZIPS[k]), e = eight(ZIPS[k] + ZIP), p = strafe(k);
					Vec3d m = new Vec3d(2 * p.x - 0.5 * (s.x + e.x), 6 - 0.5 * (s.y + e.y), 2 * p.z - 0.5 * (s.z + e.z));
					return s.multiply((1 - u) * (1 - u)).add(m.multiply(2 * u * (1 - u))).add(e.multiply(u * u));
				}
			}
			if (t < FLY_END) {
				return eight(t);
			}
			double u = smooth((t - FLY_END) / (double) (ENTOMB - FLY_END));
			return eight(FLY_END).lerp(new Vec3d(1.5, SEAL_Y, 0), u);
		}

		/** True while a strafing run is skimming the ground. */
		public static boolean low(double t) {
			for (int z : ZIPS) {
				if (t >= z + ZIP * 0.3 && t < z + ZIP * 0.7) {
					return true;
				}
			}
			return false;
		}
	}

	// ====================================================================== SABRE-TOOTH

	/**
	 * The land turns to tar pits. A sabre-toothed cat's skeleton climbs out of the biggest one, roars, prowls round the
	 * pits and pounces three times; then it leaps into the middle, roars a last time and sinks back into the tar,
	 * leaving its skull and sabres sticking out of it.
	 */
	public static final class Sabre {
		public static final int TAR_END = 140, EMERGE = 150, EMERGED = 260, ROAR = 270, STALK = 285, PROWL = 345, FINAL = 580, LAND = 616, ROAR2 = 625,
				SINK = 650, SUNK = 730, END = 790;
		public static final int[] POUNCES = {360, 435, 510};
		/** A pounce: crouch, leap, then land and recover. */
		public static final int CROUCH = 14, LEAP = 22, RECOVER = 14;
		public static final double R = 60, CIRCLE = 26, SPEED = 0.026, JUMP = 2.3, LEAP_H = 13;
		public static final int POOLS = 10;

		private Sabre() {
		}

		/** How far the tar has spread. */
		public static double front(double t) {
			return R * smooth(t / (double) TAR_END);
		}

		/** Pool {@code i} in the heading's frame: x, z and radius. The first is the big one in the middle. */
		public static double[] pool(int i) {
			if (i == 0) {
				return new double[]{0, 0, 9.5};
			}
			double a = i * 2.39996 + 0.5;
			double r = 17 + 31 * Math.sqrt(hash(i, 41));
			return new double[]{Math.cos(a) * r, Math.sin(a) * r, 3.5 + 3 * hash(i, 42)};
		}

		/** Ticks spent prowling by {@code t} (prowling stops while it pounces). */
		private static double walked(double t) {
			double w = MathHelper.clamp(t, PROWL, FINAL) - PROWL;
			for (int p : POUNCES) {
				w -= MathHelper.clamp(t - p, 0, CROUCH + LEAP + RECOVER);
			}
			return w;
		}

		private static int jumps(double t) {
			int n = 0;
			for (int p : POUNCES) {
				if (t >= p + CROUCH + LEAP) {
					n++;
				}
			}
			return n;
		}

		/** Its angle round the pits. */
		public static double angle(double t) {
			return 0.6 + SPEED * walked(t) + JUMP * jumps(t);
		}

		static Vec3d circle(double a) {
			return new Vec3d(Math.cos(a) * CIRCLE, 0, Math.sin(a) * CIRCLE);
		}

		/** Where pounce {@code k} lands, in the heading's frame. */
		public static Vec3d target(int k) {
			return circle(angle(POUNCES[k]) + JUMP);
		}

		private static Vec3d arc(Vec3d a, Vec3d b, double u, double h) {
			return a.lerp(b, u).add(0, Math.sin(Math.PI * u) * h, 0);
		}

		/**
		 * The body in the heading's frame: x and z on the ground, y how far it is above (leaping) or below (climbing
		 * out, sinking) the ground under it.
		 */
		public static Vec3d pos(double t) {
			if (t < EMERGE) {
				return new Vec3d(-6, -12, 0);
			}
			if (t < EMERGED) {
				double u = smooth((t - EMERGE) / (double) (EMERGED - EMERGE));
				return new Vec3d(-6 + 6 * u, -12 + 12 * u, 0);
			}
			if (t < STALK) {
				return Vec3d.ZERO;
			}
			if (t < PROWL) {
				double u = smooth((t - STALK) / (double) (PROWL - STALK));
				return Vec3d.ZERO.lerp(circle(angle(PROWL)), u);
			}
			for (int p : POUNCES) {
				double d = t - p;
				if (d >= 0 && d < CROUCH + LEAP + RECOVER) {
					Vec3d from = circle(angle(p)), to = circle(angle(p) + JUMP);
					if (d < CROUCH) {
						return from.add(0, -1.4 * smooth(d / 6.0), 0);
					}
					if (d < CROUCH + LEAP) {
						return arc(from, to, (d - CROUCH) / LEAP, LEAP_H);
					}
					return to.add(0, -1.6 * (1 - smooth((d - CROUCH - LEAP) / RECOVER)), 0);
				}
			}
			if (t < FINAL) {
				return circle(angle(t));
			}
			Vec3d from = circle(angle(FINAL));
			double d = t - FINAL;
			if (d < CROUCH) {
				return from.add(0, -1.4 * smooth(d / 6.0), 0);
			}
			if (t < LAND) {
				return arc(from, Vec3d.ZERO, (d - CROUCH) / (LAND - FINAL - CROUCH), LEAP_H + 4);
			}
			if (t < SINK) {
				return new Vec3d(0, -1.6 * (1 - smooth((t - LAND) / 12.0)), 0);
			}
			double u = smooth((t - SINK) / (double) (SUNK - SINK));
			return new Vec3d(1.5 * u, -11.5 * u, 0);
		}

		/** True while it is in the air. */
		public static boolean leaping(double t) {
			for (int p : POUNCES) {
				if (t >= p + CROUCH && t < p + CROUCH + LEAP) {
					return true;
				}
			}
			return t >= FINAL + CROUCH && t < LAND;
		}

		/** True while it is crouched to spring. */
		public static boolean crouching(double t) {
			for (int p : POUNCES) {
				if (t >= p && t < p + CROUCH) {
					return true;
				}
			}
			return t >= FINAL && t < FINAL + CROUCH;
		}

		/** Which way it faces (heading's frame, horizontal). */
		public static Vec3d facing(double t) {
			if (t < STALK) {
				return new Vec3d(1, 0, 0);
			}
			for (int p : POUNCES) {
				if (t >= p && t < p + CROUCH + LEAP) {
					return flat(circle(angle(p) + JUMP).subtract(circle(angle(p))), t);
				}
			}
			if (t >= FINAL) {
				// Facing the middle as it springs for it, and still facing that way as it goes under.
				return flat(circle(angle(FINAL)).multiply(-1), t);
			}
			Vec3d v = pos(t + 1).subtract(pos(t - 1));
			if (v.horizontalLengthSquared() > 0.01) {
				return flat(v, t);
			}
			// Prowling round anticlockwise: the tangent of its circle.
			double a = angle(t);
			return new Vec3d(-Math.sin(a), 0, Math.cos(a));
		}

		private static Vec3d flat(Vec3d v, double t) {
			Vec3d f = new Vec3d(v.x, 0, v.z);
			return f.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : f.normalize();
		}

		/** How wide its jaws are open (0 shut, 1 gaping): it roars as it climbs out, on each pounce and at the end. */
		public static double gape(double t) {
			double g = bump(t, ROAR - 8, ROAR + 28) + bump(t, ROAR2 - 6, ROAR2 + 34) + bump(t, SINK + 40, SUNK);
			for (int p : POUNCES) {
				g += 0.8 * bump(t, p + 6, p + CROUCH + LEAP + 4);
			}
			return Math.min(1, g);
		}

		/** 0 outside (a, b), rising to 1 in the middle and back. */
		public static double bump(double t, double a, double b) {
			if (t <= a || t >= b) {
				return 0;
			}
			return Math.sin(Math.PI * (t - a) / (b - a));
		}
	}

	// ====================================================================== AMMONITE

	/**
	 * An ammonite's shell, coiled like a wheel and taller than a house, grows up out of the ground chamber by chamber.
	 * The ghost of the animal reaches out of it; then a spiral of light unwinds from it across the land, pressing the
	 * whole countryside into the imprint of an ammonite, the land around turned to stone.
	 */
	public static final class Ammonite {
		public static final int COIL = 40, COILED = 240, AWAKEN = 255, SPIRAL = 300, SPIRAL_END = 480, PULSE = 505, FADE = 560, END = 700;
		/** The shell: a log spiral rho = A e^(B theta) out to THETA_MAX, its tube K times rho, squashed to WIDTH sideways. */
		public static final double A = 0.9, B = 0.139, THETA_MAX = 17.9, K = 0.36, WIDTH = 0.62, CY = 13;
		/** The imprint on the ground: a log spiral of the same shape, from AG out to R over three turns. */
		public static final double AG = 5, R = 70, PHI_MAX = 6 * Math.PI, BG = Math.log(R / AG) / PHI_MAX;
		/** The chamber walls (septa), one every this much of theta; the last part is the living chamber. */
		public static final double SEPTUM = 0.42, LIVING = 1.3;

		private Ammonite() {
		}

		public static double rho(double theta) {
			return A * Math.exp(B * theta);
		}

		/** Rotation so the shell's mouth faces forward and down. */
		static final double PHASE = -Math.PI / 2 + 0.6 - THETA_MAX;

		/** A point on the coil's centre line, in the heading's frame (x forward, y up, z right) relative to the bomb's ground. */
		public static Vec3d coil(double theta) {
			double r = rho(theta), a = theta + PHASE;
			return new Vec3d(Math.cos(a) * r, CY + Math.sin(a) * r, 0);
		}

		/** Outward (away from the coil's axis) at {@code theta}, in the heading's frame. */
		public static Vec3d out(double theta) {
			double a = theta + PHASE;
			return new Vec3d(Math.cos(a), Math.sin(a), 0);
		}

		/** How far the shell has grown (theta) by tick {@code t}. */
		public static double grown(double t) {
			return 0.6 + (THETA_MAX - 0.6) * MathHelper.clamp((t - COIL) / (double) (COILED - COIL), 0.0, 1.0);
		}

		/** How far the spiral of light has unwound (phi) by tick {@code t}. */
		public static double unwound(double t) {
			double u = MathHelper.clamp((t - SPIRAL) / (double) (SPIRAL_END - SPIRAL), 0.0, 1.0);
			// Quick at first, slower as the turns widen, so the head of the spiral sweeps the land at a steadier pace.
			return PHI_MAX * (1 - 0.35 * (1 - u) * (1 - u) - 0.65 * (1 - u));
		}

		/** The imprint's spiral on the ground at {@code phi}, in the heading's frame (y 0). */
		public static Vec3d ground(double phi) {
			double r = AG * Math.exp(BG * phi);
			return new Vec3d(Math.cos(phi) * r, 0, Math.sin(phi) * r);
		}

		/**
		 * For a column at {@code (x, z)} in the heading's frame: the phi of the nearest turn of the imprint's spiral, and
		 * the radial distance from it (positive outside it). Returns null outside the imprint.
		 */
		public static double[] nearest(double x, double z) {
			double r = Math.hypot(x, z);
			if (r > R + 2) {
				return null;
			}
			double phi0 = Math.atan2(z, x);
			if (phi0 < 0) {
				phi0 += Math.PI * 2;
			}
			double best = Double.MAX_VALUE, bestPhi = 0, bestR = AG;
			for (int k = -1; k <= 3; k++) {
				double phi = phi0 + Math.PI * 2 * k;
				if (phi < -0.5 || phi > PHI_MAX + Math.PI) {
					continue;
				}
				double rk = AG * Math.exp(BG * phi);
				double d = Math.abs(Math.log(Math.max(r, 0.5) / rk));
				if (d < best) {
					best = d;
					bestPhi = phi;
					bestR = rk;
				}
			}
			return new double[]{Math.max(0, bestPhi), r - bestR, bestR};
		}
	}
}
