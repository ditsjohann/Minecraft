package com.armageddon.util;

import net.minecraft.util.math.Vec3d;

/**
 * Shared layout and timeline of the MAIN STAGE set, so the server's real block work and the client's rig,
 * lamps and scorching beams agree to the block and the tick. There is no cutscene: it all happens around you.
 */
public final class MainStageSet {
	// ---- timeline (event ticks) ----
	/** The rigging lowers into the sky over the first few seconds. */
	public static final int RIG_DOWN = 80;
	/** A loose studio light breaks out of the rig and falls. */
	public static final int LIGHT_FALL = 40;
	/** It hits the ground. */
	public static final int LIGHT_HIT = 70;
	/** The big studio lights switch on one by one from here. */
	public static final int LAMPS_ON = 90;
	/** The set is struck, ring by ring from the edge inward. */
	public static final int STRIKE = 100, STRIKE_END = 220;
	/** The lamp beams start burning the ground. */
	public static final int SCORCH = 140, SCORCH_END = 760;
	/** The main stage rises out of the studio floor, then the painted-sky wall goes up around everything. */
	public static final int STAGE = 225, WALL = 250;
	/** Showtime: the curtain call, once the set is built. */
	public static final int CURTAIN_CALL = 420;

	// ---- layout (blocks) ----
	public static final double DOME_R = 96, DOME_H = 80;
	public static final double WALL_R = 90, WALL_H = 50, DOOR_H = 14;
	public static final double STRIKE_R = 72;
	public static final int LAMPS = 4;
	private static final double[] LAMP_BEARING = {50, 140, -140, -50};
	/** The main stage: how far in front of the bomb it stands, its width, depth and height, and its arch. */
	public static final double STAGE_AT = 26, STAGE_W = 30, STAGE_D = 14, STAGE_H = 3, ARCH_H = 20;

	private MainStageSet() {
	}

	public static double bearing(Vec3d fwd) {
		return Math.atan2(fwd.z, fwd.x);
	}

	public static int lampOn(int i) {
		return LAMPS_ON + i * 6;
	}

	/** Where studio lamp {@code i} hangs, just under the dome. */
	public static Vec3d lamp(Vec3d g, Vec3d fwd, int i) {
		double az = bearing(fwd) + Math.toRadians(LAMP_BEARING[i]);
		double r = 58;
		double h = DOME_H * Math.sqrt(1 - (r / DOME_R) * (r / DOME_R)) - 5;
		return new Vec3d(g.x + Math.cos(az) * r, g.y + h, g.z + Math.sin(az) * r);
	}

	/** Where lamp {@code i}'s beam meets the ground at event tick {@code t} (y is the anchor's). */
	public static Vec3d spot(Vec3d g, Vec3d fwd, int i, double t) {
		double u = t - LAMPS_ON;
		double ang = bearing(fwd) + i * Math.PI / 2 + 0.6 + u * 0.011 * (i % 2 == 0 ? 1 : -1) + 0.35 * Math.sin(u * 0.03 + i);
		double rad = 52 + 14 * Math.sin(u * 0.021 + i * 1.7);
		return new Vec3d(g.x + Math.cos(ang) * rad, g.y, g.z + Math.sin(ang) * rad);
	}

	/** Where the loose light lands, in front of and beside the player. */
	public static Vec3d crash(Vec3d g, Vec3d fwd) {
		return g.add(fwd.multiply(14)).add(-fwd.z * 6, 0, fwd.x * 6);
	}

	/** Painted cloud density (> ~0.55 is cloud) at azimuth {@code az} and elevation {@code el} (radians). */
	public static double cloud(double az, double el) {
		return Math.sin(az * 5 + Math.sin(el * 7) * 1.3) * 0.5 + Math.sin(az * 11 - el * 9 + 1.7) * 0.35
				+ Math.sin(az * 2.3 + el * 3.1) * 0.4;
	}

	// ---- 3x5 pixel font (signs built from blocks and drawn by the client) ----
	private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final String[] FONT = {
			".#.#.#####.##.#", "##.#.###.#.###.", ".###..#..#...##", "##.#.##.##.###.", "####..##.#..###",
			"####..##.#..#..", ".###..#.##.#.##", "#.##.#####.##.#", "###.#..#..#.###", "..#..#..##.#.#.",
			"#.##.###.#.##.#", "#..#..#..#..###", "#.########.##.#", "##.#.##.##.##.#", ".#.#.##.##.#.#.",
			"##.#.###.#..#..", ".#.#.##.###..##", "##.#.###.#.##.#", ".###...#...###.", "###.#..#..#..#.",
			"#.##.##.##.####", "#.##.##.##.#.#.", "#.##.########.#", "#.##.#.#.#.##.#", "#.##.#.#..#..#.",
			"###..#.#.#..###", "####.##.##.####", ".#.##..#..#.###", "##...#.#.#..###", "##...#.#...###.",
			"#.##.####..#..#", "####..##...###.", ".###..####.####", "###..#.#..#..#.", "####.#####.####",
			"####.####..###."};

	/** True if pixel ({@code col} 0..2 from the left, {@code row} 0..4 from the top) of {@code c} is set. */
	public static boolean pixel(char c, int col, int row) {
		int i = GLYPHS.indexOf(Character.toUpperCase(c));
		if (i < 0 || col < 0 || col > 2 || row < 0 || row > 4) {
			return false;
		}
		return FONT[i].charAt(row * 3 + col) == '#';
	}

	/** Width in pixels of {@code s} (3 per glyph, 1 between glyphs). */
	public static int width(String s) {
		return s.isEmpty() ? 0 : s.length() * 4 - 1;
	}
}
