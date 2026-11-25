package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Timings and geometry for the three fossil bombs (the Mammoth, the Leviathan and the Pterosaur), shared by the
 * server events and the client effects.
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
}
