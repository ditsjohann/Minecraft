package com.armageddon.network;

/** Effect identifiers carried by {@link FxPayload#type()}. */
public final class FxType {
	/** Full nuclear detonation: flash, fireball, mushroom cloud, shockwave, delayed boom. scale = yield radius. */
	public static final int NUKE = 0;
	/** Antimatter annihilation: implosion then white-violet sphere. */
	public static final int ANTIMATTER = 1;
	/** Neutron pulse: green flash + expanding ring, no cloud. */
	public static final int NEUTRON = 2;
	/** Cryo wave: icy ring + frost burst. */
	public static final int CRYO_WAVE = 3;
	/** Generic high-explosive fireball. */
	public static final int FIREBALL = 4;
	/** Straight beam (railgun, dash trail). color/scale = width. */
	public static final int BEAM = 5;
	/** Jagged electric arc between from/to. */
	public static final int ARC = 6;
	/** Thin fast tracer round. */
	public static final int TRACER = 7;
	/** Orbital targeting laser + countdown ring at target. scale = seconds. */
	public static final int ORBITAL_TARGET = 8;
	/** Orbital strike column from the sky. */
	public static final int ORBITAL_BEAM = 9;
	/** Expanding ground shockwave ring. */
	public static final int SHOCKWAVE = 10;
	/** Black hole collapse flash. */
	public static final int COLLAPSE = 11;
	/** Plasma impact burst. */
	public static final int PLASMA_BURST = 12;
	/** Screen shake only. scale = intensity. */
	public static final int SHAKE = 13;

	/** Planet-cracker detonation: red sky, mega shock dome, double flash. scale = radius. */
	public static final int OMEGA = 14;
	/** Omega charge-up: light pillar + converging energy. scale = ticks. */
	public static final int OMEGA_CHARGE = 15;
	/** Kinetic rod impact: tall dust column + shock dome. */
	public static final int KINETIC_IMPACT = 16;
	/** BFG detonation: green sphere + arcs. */
	public static final int BFG_BURST = 17;
	/** Dubstep beam pulse from -> to. seed = beat index. scale = width. */
	public static final int DUBSTEP_BEAM = 18;
	/** Rhythmic rainbow bass pulse. scale = radius, seed = beat. */
	public static final int BASS_PULSE = 19;
	/** The drop. scale = radius. */
	public static final int BASS_DROP = 20;
	/** Bomber jet flying from -> to. */
	public static final int JET_FLYOVER = 21;
	/** Continuous death-ray segment (short-lived, refreshed every tick). */
	public static final int DEATH_RAY = 22;
	/** Star heat-ray from star to ground. */
	public static final int HEAT_RAY = 23;
	/** Supernova: star collapse + blast. scale = radius. */
	public static final int SUPERNOVA = 24;
	/** Earthquake rumble: shake + dust ring. scale = radius. */
	public static final int QUAKE = 25;

	private FxType() {
	}
}
