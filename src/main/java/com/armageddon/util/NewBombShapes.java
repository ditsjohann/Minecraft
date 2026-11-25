package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Shared, deterministic geometry and timelines for the newest bombs, so the server's real blocks and the client's
 * lights, swarms and shards agree to the block and the tick without extra packets.
 */
public final class NewBombShapes {
	private NewBombShapes() {
	}

	public static double hash(int a, int b) {
		long h = a * 374761393L + b * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	// ====================================================================== LIDAR

	public static final class Lidar {
		/** Scan pulses start here, one every PERIOD ticks, PULSES of them, each reaching RANGE. */
		public static final int SCAN = 30, PERIOD = 70, PULSES = 4;
		public static final float RANGE = 150, SPEED = 2.6F;
		/** Targets lock, the strikes walk the map, and the final overload. */
		public static final int LOCK = SCAN + PERIOD * PULSES, STRIKES = LOCK + 40, OVERLOAD = STRIKES + 110, END = OVERLOAD + 120;

		private Lidar() {
		}
	}

	// ====================================================================== SPECTRAL ICE

	public static final class Ice {
		/** Spikes erupt, the apex lights, the spikes glow, the spray, the end. */
		public static final int ERUPT = 20, ERUPT_END = 150, APEX = 165, GLOW = 200, SPRAY = 270, SHARDS = 72, SPRAY_LEN = 90, END = 520;
		/** Pyramid half-width at the base and apex height. */
		public static final double HALF = 24, HEIGHT = 50;
		/** Spikes per side of the square footprint (corners included once). */
		private static final int PER_SIDE = 5;

		private Ice() {
		}

		public static Vec3d apex(Vec3d g) {
			return g.add(0, HEIGHT, 0);
		}

		/** Spike bases: around the square footprint, plus an inner ring of shorter ones. */
		public static Vec3d[] bases(Vec3d g) {
			int ring = PER_SIDE * 4;
			Vec3d[] out = new Vec3d[ring + 8];
			for (int i = 0; i < ring; i++) {
				int side = i / PER_SIDE, k = i % PER_SIDE;
				double u = -HALF + 2 * HALF * k / PER_SIDE;
				double x, z;
				switch (side) {
					case 0 -> {
						x = u;
						z = -HALF;
					}
					case 1 -> {
						x = HALF;
						z = u;
					}
					case 2 -> {
						x = -u;
						z = HALF;
					}
					default -> {
						x = -HALF;
						z = -u;
					}
				}
				out[i] = g.add(x, 0, z);
			}
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4 + Math.PI / 8;
				out[ring + i] = g.add(Math.cos(a) * HALF * 0.55, 0, Math.sin(a) * HALF * 0.55);
			}
			return out;
		}

		/** Where spike {@code i} ends: the corner and edge spikes meet at the apex, the inner ring stops short. */
		public static Vec3d tip(Vec3d g, Vec3d base, int i, int count) {
			Vec3d apex = apex(g);
			double reach = i >= count - 8 ? 0.62 : 0.985;
			return base.add(apex.subtract(base).multiply(reach));
		}

		public static double baseRadius(int i, int count) {
			boolean corner = i < count - 8 && i % PER_SIDE == 0;
			return corner ? 4.2 : i >= count - 8 ? 2.6 : 3.2;
		}

		/** How far up its length spike {@code i} has erupted at tick {@code t} (0..1). */
		public static double grown(int i, int count, double t) {
			double start = ERUPT + (i % 7) * 6 + (i >= count - 8 ? 30 : 0);
			return MathHelper.clamp((t - start) / 70.0, 0.0, 1.0);
		}

		/** Launch tick of shard {@code i}. */
		public static int shardLaunch(int i) {
			return SPRAY + i * SPRAY_LEN / SHARDS;
		}

		public static int shardFlight(int i) {
			return 26 + (int) (hash(i, 3) * 18);
		}

		/** Where shard {@code i} lands, relative to the ground centre (y is resolved against the terrain by the server). */
		public static Vec3d shardTarget(Vec3d g, int i) {
			double a = i * 2.39996 + hash(i, 1) * 0.4;
			double d = 30 + hash(i, 2) * 62;
			return g.add(Math.cos(a) * d, 0, Math.sin(a) * d);
		}

		/** A shard's position along its arc at progress {@code k} (0..1) from the apex to {@code land}. */
		public static Vec3d shardAt(Vec3d g, Vec3d land, double k) {
			Vec3d a = apex(g).add(0, 3, 0);
			Vec3d p = a.add(land.subtract(a).multiply(k));
			return p.add(0, Math.sin(k * Math.PI) * 26, 0);
		}
	}

	// ====================================================================== TERMITE DOMAIN

	public static final class Termite {
		/** The swarm emerges, eats outwards, turns home and builds. */
		public static final int EMERGE = 30, EAT_END = 330, BUILD = 340, BUILD_END = 700, FLIGHT = 720, FLIGHT_END = 1020, COLLAPSE = FLIGHT_END + 20, CAVE = COLLAPSE + 60, END = CAVE + 150;
		public static final double RADIUS = 90, FLIGHT_R = 175, HILL_R = 24, HILL_H = 72;
		public static final int SPIRES = 6;

		private Termite() {
		}

		/** The eating front's radius at tick {@code t}. */
		public static double front(double t) {
			return MathHelper.clamp((t - EMERGE) / (EAT_END - EMERGE), 0.0, 1.0) * RADIUS;
		}

		/** The flying swarm's front during the nuptial flight. */
		public static double flightFront(double t) {
			return RADIUS + MathHelper.clamp((t - FLIGHT) / (FLIGHT_END - FLIGHT), 0.0, 1.0) * (FLIGHT_R - RADIUS);
		}

		/** How much of the hill (0..1 of its height) stands at tick {@code t}. */
		public static double built(double t) {
			double k = MathHelper.clamp((t - BUILD) / (BUILD_END - BUILD), 0.0, 1.0);
			return 1 - Math.pow(1 - k, 1.6);
		}

		/** Outer radius of the main mound at height {@code y} (0..HILL_H) and bearing {@code a}: lumpy, buttressed. */
		public static double radius(double y, double a) {
			double k = MathHelper.clamp(y / HILL_H, 0.0, 1.0);
			double r = HILL_R * Math.pow(1 - k, 0.85) + 1.2;
			r *= 1 + 0.16 * Math.sin(a * 5 + y * 0.11) + 0.08 * Math.sin(a * 11 - y * 0.23) + 0.1 * Math.max(0, Math.sin(a * 3 + 1.3)) * (1 - k);
			return r;
		}

		/** Secondary spire {@code i}: horizontal offset from the centre, base radius and height. */
		public static double[] spire(int i) {
			double a = i * MathHelper.TAU / SPIRES + hash(i, 7) * 0.6;
			double d = 15 + hash(i, 8) * 6;
			return new double[]{Math.cos(a) * d, Math.sin(a) * d, 6 + hash(i, 9) * 3, 26 + hash(i, 10) * 22};
		}
	}

	// ====================================================================== THE SUN DISAPPEARS

	public static final class Sun {
		/** The beam climbs, reaches the sun, the sun gutters and goes out, the cold, the corpse falls, impact, end. */
		public static final int BEAM = 40, HIT = 140, DIM = 150, OUT = 330, COLD = 340, FALL = 1000, IMPACT = 1220, END = 1500;
		public static final double COLD_R = 220, FALL_HEIGHT = 560, CORPSE_R = 64;

		private Sun() {
		}

		/** The dying sun's corpse at tick {@code t} (falls from high in the sky, accelerating, onto the target). */
		public static Vec3d corpse(Vec3d target, Vec3d from, double t) {
			double k = MathHelper.clamp((t - FALL) / (IMPACT - FALL), 0.0, 1.0);
			k = k * k * (1.4 - 0.4 * k);
			return from.add(target.subtract(from).multiply(k));
		}

		/** Where the corpse starts: up in the sky, off to the side the sun hung on. */
		public static Vec3d corpseStart(Vec3d target, Vec3d heading) {
			return target.add(heading.multiply(-180)).add(0, FALL_HEIGHT, 0);
		}
	}

	// ====================================================================== PRISM

	/**
	 * The Prism: a great crystal gem comes down out of the sky into a shaft of sunlight and splits it. Seven soft
	 * spotlights of colour fall from its facets and wander over the land, painting it; they converge white-hot under
	 * it, and the gem shatters into rainbow shards that rain down and grow into crystal.
	 */
	public static final class Prism {
		public static final int DESCEND = 90, REFRACT = 100, CONVERGE = 400, SHATTER = 460, END = 600;
		public static final int SPOTS = 7, SHARDS = 64;
		public static final int[] COLORS = {0xFF2020, 0xFF8A10, 0xFFE020, 0x30E030, 0x20A0FF, 0x3040FF, 0xB030FF};
		/** The gem's hover height and half-height, the spotlights' reach and radius. */
		public static final double GEM_Y = 34, GEM = 12, REACH = 60, SPOT = 7;

		private Prism() {
		}

		public static double hash(int i, int k) {
			double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
			return v - Math.floor(v);
		}

		/** The gem's height over the ground: down out of the sky, then hovering, bobbing. */
		public static double gemY(double t) {
			double u = MathHelper.clamp(t / DESCEND, 0.0, 1.0);
			u = 1 - (1 - u) * (1 - u);
			return GEM_Y + (1 - u) * 90 + Math.sin(t * 0.04) * 1.2;
		}

		/** How spread out the spotlights are (0 gathered under the gem .. 1 roaming the land). */
		public static double spread(double t) {
			double out = MathHelper.clamp((t - REFRACT) / 60.0, 0.0, 1.0);
			double in = MathHelper.clamp((t - CONVERGE) / (SHATTER - CONVERGE - 10), 0.0, 1.0);
			return out * out * (3 - 2 * out) * (1 - in * in * (3 - 2 * in));
		}

		/** Where spotlight {@code i} falls at tick {@code t}, relative to the bomb (y = 0): wandering loops over the land. */
		public static Vec3d spot(int i, double t) {
			double wx = 0.011 + hash(i, 1) * 0.012, wz = 0.013 + hash(i, 2) * 0.012;
			double r = REACH * (0.55 + 0.45 * hash(i, 3)) * spread(t);
			return new Vec3d(Math.sin(t * wx + i * 1.7) * r, 0, Math.sin(t * wz + i * 2.9 + 1) * r);
		}

		/** Where shard {@code k} lands (relative, y = 0), and when. */
		public static Vec3d shard(int k) {
			double a = hash(k, 11) * MathHelper.TAU, r = 8 + Math.sqrt(hash(k, 12)) * (REACH - 6);
			return new Vec3d(Math.cos(a) * r, 0, Math.sin(a) * r);
		}

		public static int shardLand(int k) {
			return SHATTER + 18 + (int) (hash(k, 13) * 45);
		}
	}

	// ====================================================================== LAVA LAMP

	public static final class Lamp {
		public static final int BUILD = 10, BUILT = 80, HEAT = 90, CRACK = 360, BURST = 420, END = 520;
		public static final double R = 6, H = 34, BASE_H = 6;
		public static final int BLOBS = 9;

		private Lamp() {
		}

		/** Centre of goo blob {@code i} inside the lamp at tick {@code t}. */
		public static Vec3d blob(Vec3d g, int i, double t) {
			double heat = MathHelper.clamp((t - HEAT) / 200.0, 0.15, 1.6);
			double ph = hash(i, 1) * MathHelper.TAU + t * 0.012 * heat * (0.7 + hash(i, 2) * 0.6);
			double y = BASE_H + 4 + (H - 8) * (0.5 - 0.5 * Math.cos(ph));
			double a = hash(i, 3) * MathHelper.TAU + t * 0.01;
			double r = (R - 3) * hash(i, 4);
			return g.add(Math.cos(a) * r, y, Math.sin(a) * r);
		}

		public static double blobSize(int i) {
			return 1.6 + hash(i, 5) * 1.8;
		}

		/** When blob {@code i} (of {@link #FLYERS}) leaves the burst lamp, how long it flies, and where it lands. */
		public static final int FLYERS = 24;

		public static int flight(int i) {
			return 24 + (int) (hash(i, 6) * 20);
		}

		public static Vec3d land(Vec3d g, int i) {
			double a = i * 2.39996 + hash(i, 7);
			double d = 12 + hash(i, 8) * 44;
			return g.add(Math.cos(a) * d, 0, Math.sin(a) * d);
		}

		public static Vec3d launch(Vec3d g, int i) {
			return g.add(0, BASE_H + 6 + hash(i, 9) * (H - 12), 0);
		}
	}

	// ====================================================================== GEODE

	public static final class Geode {
		public static final int CRACK = 0, RISE = 30, RISEN = 150, RESONATE = 170, SPRAY = 210, SHARDS = 48, SPRAY_LEN = 60, SHATTER = 300, END = 380;
		public static final double R = 14, LIFT = 3;

		private Geode() {
		}

		public static Vec3d centre(Vec3d g) {
			return g.add(0, LIFT, 0);
		}

		public static int launch(int i) {
			return SPRAY + i * SPRAY_LEN / SHARDS;
		}

		public static int flight(int i) {
			return 22 + (int) (hash(i, 13) * 16);
		}

		public static Vec3d land(Vec3d g, int i) {
			double a = i * 2.39996 + hash(i, 11) * 0.5;
			double d = R + 8 + hash(i, 12) * 40;
			return g.add(Math.cos(a) * d, 0, Math.sin(a) * d);
		}

		public static Vec3d from(Vec3d g, int i) {
			double a = i * 2.39996;
			return centre(g).add(Math.cos(a) * 4, R * 0.6, Math.sin(a) * 4);
		}
	}

	/** A ballistic arc from {@code a} to {@code b} at progress {@code k}, peaking {@code height} above the straight line. */
	public static Vec3d arc(Vec3d a, Vec3d b, double k, double height) {
		return a.add(b.subtract(a).multiply(k)).add(0, Math.sin(k * Math.PI) * height, 0);
	}
}
