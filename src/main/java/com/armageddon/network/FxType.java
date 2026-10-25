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

	// ---- Expansion vocabulary (p1/p2/duration carry parameters) ----
	/** Steam / vapour burst. */
	public static final int STEAM = 26;
	/** Antimatter interference glitch. */
	public static final int GLITCH = 27;
	/** Sphere: scale=start radius, p1=end radius, p2=mode (0 glow,1 black sun,2 star corona,3 pinprick), duration. */
	public static final int ORB = 28;
	/** Wobbling translucent bubble. scale=radius, duration. */
	public static final int BUBBLE = 29;
	/** Hexagonal wireframe cage. scale=radius, duration. */
	public static final int CAGE = 30;
	/** Wireframe grid: p2=mode (0 sphere, 1 plane through from with normal to, 2 inward ground grid, 3 terrain wireframe). */
	public static final int GRID = 31;
	/** Dimensional portal / rift. to=normal, scale=radius, duration. */
	public static final int PORTAL = 32;
	/** Sky fractures around the viewer. scale=intensity, duration, p2=style (0 jagged, 1 geometric panels). */
	public static final int SKY_CRACK = 33;
	/** Energy crystal from base to tip. scale=width, duration. */
	public static final int CRYSTAL = 34;
	/** Paint splatter decals. scale=radius, color (0 = rainbow, -1 = clear), duration. */
	public static final int PAINT = 35;
	/** Firework burst. scale=size, seed=shape (0 star,1 burst,2 ball,3 heart,4 smiley). */
	public static final int FIREWORK = 36;
	/** Holographic fakes. scale=radius, duration. */
	public static final int HOLOGRAM = 37;
	/** Mirrored ghost overlay. scale=radius, duration. */
	public static final int MIRROR = 38;
	/** Sound wave rings travelling from -> to. scale=width, p1=pitch pattern. */
	public static final int SOUNDWAVE = 39;
	/** Full-screen effect: seed=kind (0 desaturate,1 whiteout,2 blackout,3 static,4 invert), scale=strength, duration. */
	public static final int SCREEN = 40;
	/** Layered explosion: flash → fireball → pressure ring → dome → smoke → secondary pulse. scale=size, color palette. */
	public static final int LAYERED_BLAST = 41;
	/** Wireframe/afterimage box from min (from) to max (to). duration. */
	public static final int WIREBOX = 42;
	/** Energy streak travelling from -> to over duration. scale=width. */
	public static final int STREAK = 43;
	/** Light pillar. scale=radius, duration, p2=1 for rainbow. */
	public static final int PILLAR = 44;
	/** Shock dome. scale=radius, duration. */
	public static final int DOME = 45;
	/** Ring with explicit params. scale=radius, p1=thickness, duration, p2=1 to throw dust. */
	public static final int RING = 46;
	/** Swirling vortex. scale=radius, p1=height, duration. */
	public static final int SPIRAL = 47;
	/** Charge-up orb that follows entity id (seed). scale=size, duration. */
	public static final int CHARGE = 48;
	/** Sky tint: color, scale=strength. */
	public static final int SKY_TINT = 49;
	/** God-ray light source at position: scale=strength, color. */
	public static final int LIGHT = 50;
	/** Flash only: scale=strength, p1=range, color. */
	public static final int FLASH = 51;
	/** Spinning blade (boomerang) is entity-rendered; this is its slice ring burst. */
	public static final int SLICE = 52;
	/** World-event warning banner for every player in range. seed=tier. */
	public static final int WARNING = 53;
	/** Gravity reversal overlay: rising particles + inverted shake. scale=radius, duration. */
	public static final int GRAVITY_FLIP = 54;

	private FxType() {
	}

	// ---- World Destruction cutscene vocabulary ----
	/** Letterbox bars. duration = ticks. */
	public static final int CUTSCENE = 55;
	/** Anime impact frame. seed = style (0 ink, 1 inverted ink, 2 red ink, 3 white), duration = ticks. */
	public static final int IMPACT = 56;
	/** FOV punch. scale = degrees, duration = ticks of decay. */
	public static final int FOV = 57;
	/** A falling moon. scale = radius, p1 = start height, duration = ticks until impact. */
	public static final int MOON = 58;

	// ---- Megastructure effects and scripted cinematics ----
	/** Scripted camera cinematic. seed = scene id (1 Galactic Smash, 2 Booming Tick, 3 Alien Launch), b = horizontal heading. */
	public static final int CINEMATIC = 60;
	/** A mile-high twister. a = start, b = heading, scale = radius, p1 = speed (blocks/tick), duration. */
	public static final int TWISTER = 61;
	/** A city-sized UFO flying a -> b. scale = radius. */
	public static final int UFO = 62;
	/** A building-sized spear thrown at a. scale = length, p1 = sink depth. */
	public static final int SPEAR = 63;
	/** The alien rocket arriving. a = impact point, b = start, scale = length. */
	public static final int ALIEN_ROCKET = 64;

	// ---- World destruction II: each built from the raw payload by its own client effect class ----
	/** The giant disco ball (client: DiscoBallFx). */
	public static final int DISCO_BALL = 65;
	/** The Truman Show studio set (client: TrumanFx). */
	public static final int TRUMAN_SET = 66;
	/** The eternal tree's aura (client: EternalTreeFx). */
	public static final int ETERNAL_TREE = 67;
	/** Sixteen-chunks glitch overlay (client: ChunkGlitchFx). */
	public static final int CHUNK_GLITCH = 68;

	// ---- Signature effects per tier group; seed picks the effect, the rest is free-form payload ----
	/** Celestial I (client: CelestialFx.create). */
	public static final int CELESTIAL_FX = 70;
	/** Celestial II (client: CelestialIIFx.create). */
	public static final int CELESTIAL2_FX = 71;
	/** City destruction (client: CityFx.create). */
	public static final int CITY_FX = 72;
	/** Galactic Smash, Booming Tick, Alien Death Ray (client: WorldFx.create). */
	public static final int WORLD_FX = 73;
}
