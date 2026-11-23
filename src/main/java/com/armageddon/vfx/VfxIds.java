package com.armageddon.vfx;

import java.util.List;

/**
 * Names of the particle-engine effects. Shared so the server (commands, weapons) can name an effect the client
 * defines; the effects themselves live in {@code client/vfx/VfxLibrary}.
 */
public final class VfxIds {
	public static final String SPARKS = "sparks";
	public static final String GRENADE_BLAST = "grenade_blast";
	public static final String PLASMA_IMPACT = "plasma_impact";
	public static final String NAPALM = "napalm";
	public static final String SMOKE_COLUMN = "smoke_column";
	public static final String CRYO_BURST = "cryo_burst";
	public static final String VOID_IMPLOSION = "void_implosion";
	public static final String TOXIC_CLOUD = "toxic_cloud";
	public static final String ARC_BURST = "arc_burst";
	public static final String RADIATION = "radiation";
	public static final String EMBER_RAIN = "ember_rain";
	public static final String FIREWORK = "firework";
	public static final String MUZZLE_FLASH = "muzzle_flash";
	public static final String NUKE = "nuke";
	/** The mushroom cloud alone (it has several looks; each blast picks one). */
	public static final String MUSHROOM = "mushroom";

	// goo bomb
	public static final String GOO_DRIP = "goo_drip";
	public static final String GOO_PULSE = "goo_pulse";
	public static final String GOO_BURST = "goo_burst";
	public static final String GOO_GLOB = "goo_glob";
	public static final String GOO_SPLAT = "goo_splat";
	// hyperbeam raygun
	public static final String RAY_CHARGE = "ray_charge";
	public static final String RAY_MUZZLE = "ray_muzzle";
	public static final String RAY_HEAD = "ray_head";
	// cactus
	public static final String CACTUS_DUST = "cactus_dust";
	public static final String CACTUS_SPIKE = "cactus_spike";
	public static final String SPIKE_IMPACT = "spike_impact";
	public static final String CACTUS_BLOOM = "cactus_bloom";
	public static final String SANDSTORM = "sandstorm";
	// blackforge
	public static final String FORGE_STRIKE = "forge_strike";
	public static final String FORGE_SHOCK = "forge_shock";
	public static final String ANVIL_IMPACT = "anvil_impact";
	public static final String IRON_WAVE = "iron_wave";
	public static final String PLATE_DUST = "plate_dust";
	// blue infrit
	public static final String SOUL_FLARE = "soul_flare";
	public static final String SOUL_PILLAR = "soul_pillar";
	public static final String SOUL_VORTEX = "soul_vortex";
	public static final String SOUL_TRAIL = "soul_trail";
	public static final String SOUL_SWEEP = "soul_sweep";
	public static final String INFRIT_IMPACT = "infrit_impact";
	public static final String INFRIT_BURST = "infrit_burst";
	public static final String INFRIT_MANE = "infrit_mane";
	public static final String INFRIT_SPARKS = "infrit_sparks";
	public static final String INFRIT_BREATH = "infrit_breath";

	// weapons
	public static final String PLASMA_MUZZLE = "plasma_muzzle";
	public static final String PLASMA_TRAIL = "plasma_trail";
	public static final String RAIL_CHARGE = "rail_charge";
	public static final String RAIL_MUZZLE = "rail_muzzle";
	public static final String RAIL_ION = "rail_ion";
	public static final String RAIL_IMPACT = "rail_impact";
	public static final String MINIGUN_MUZZLE = "minigun_muzzle";
	public static final String SHELL_CASING = "shell_casing";
	public static final String BULLET_IMPACT = "bullet_impact";
	public static final String FLAME_JET = "flame_jet";
	public static final String FLAME_SPLASH = "flame_splash";
	public static final String ARC_MUZZLE = "arc_muzzle";
	public static final String CRYO_MUZZLE = "cryo_muzzle";
	public static final String CRYO_TRAIL = "cryo_trail";
	public static final String ROCKET_BACKBLAST = "rocket_backblast";
	public static final String ROCKET_TRAIL = "rocket_trail";
	public static final String BFG_MUZZLE = "bfg_muzzle";
	public static final String BFG_TRAIL = "bfg_trail";
	public static final String BFG_BLAST = "bfg_blast";
	public static final String HAMMER_SLAM = "hammer_slam";
	public static final String HAMMER_HIT = "hammer_hit";
	public static final String DEATHRAY_BURN = "deathray_burn";
	public static final String DEATHRAY_MUZZLE = "deathray_muzzle";

	// throwables
	public static final String GRENADE_BLINK = "grenade_blink";
	public static final String THROW_WHOOSH = "throw_whoosh";
	public static final String FUSE_SPARKS = "fuse_sparks";
	public static final String PLASMA_PULSE = "plasma_pulse";
	public static final String FROST_LEAK = "frost_leak";
	public static final String FIRE_LEAK = "fire_leak";
	public static final String GRAVITY_WARP = "gravity_warp";
	public static final String GAS_LEAK = "gas_leak";
	public static final String NUKE_TICK = "nuke_tick";
	public static final String ARC_CRACKLE = "arc_crackle";
	public static final String EMP_PING = "emp_ping";
	public static final String BOOMERANG_TRAIL = "boomerang_trail";
	public static final String FRAG_SHRAPNEL = "frag_shrapnel";
	public static final String PLASMA_NOVA = "plasma_nova";
	public static final String INCENDIARY_BURST = "incendiary_burst";
	public static final String GRAVITY_WELL = "gravity_well";
	public static final String GAS_BURST = "gas_burst";
	public static final String SHATTER_BURST = "shatter_burst";
	public static final String EMP_PULSE = "emp_pulse";

	// throwables II
	public static final String SEISMIC_HUM = "seismic_hum";
	public static final String ION_GLOW = "ion_glow";
	public static final String STASIS_HEX = "stasis_hex";
	public static final String QUANTUM_GLITCH = "quantum_glitch";
	public static final String CRYSTAL_GLINT = "crystal_glint";
	public static final String VOID_CRACKLE = "void_crackle";
	public static final String HORIZON_PULL = "horizon_pull";
	public static final String MIRROR_SHIMMER = "mirror_shimmer";
	public static final String SNOW_SWIRL = "snow_swirl";
	public static final String SLIME_DRIP = "slime_drip";
	public static final String SEISMIC_BURST = "seismic_burst";
	public static final String ION_BLOOM = "ion_bloom";
	public static final String STASIS_FIELD = "stasis_field";
	public static final String QUANTUM_BURST = "quantum_burst";
	public static final String CRYSTAL_BURST = "crystal_burst";
	public static final String VOIDSTORM_BURST = "voidstorm_burst";
	public static final String HORIZON_COLLAPSE = "horizon_collapse";
	public static final String MIRROR_FLASH = "mirror_flash";
	public static final String SNOW_GLOBE_BURST = "snow_globe_burst";
	public static final String SLIME_BURST = "slime_burst";

	// fun throwables and more weapons
	public static final String BOUNCE_TRAIL = "bounce_trail";
	public static final String LOOT_GLINT = "loot_glint";
	public static final String MOON_DUST = "moon_dust";
	public static final String HOLO_FLICKER = "holo_flicker";
	public static final String MAGNET_SPARKS = "magnet_sparks";
	public static final String PAINT_DRIP = "paint_drip";
	public static final String NUKE_ROUND_TRAIL = "nuke_round_trail";
	public static final String BOUNCE_BURST = "bounce_burst";
	public static final String LOOT_BURST = "loot_burst";
	public static final String MOON_BURST = "moon_burst";
	public static final String HOLO_BURST = "holo_burst";
	public static final String MAGNET_PULSE = "magnet_pulse";
	public static final String PAINT_SPLASH = "paint_splash";
	public static final String LANCE_CHARGE = "lance_charge";
	public static final String LANCE_MUZZLE = "lance_muzzle";
	public static final String LANCE_TRAIL = "lance_trail";
	public static final String ANTIMATTER_FLASH = "antimatter_flash";
	public static final String BLADE_HIT = "blade_hit";
	public static final String BLADE_DASH = "blade_dash";
	public static final String GATLING_MUZZLE = "gatling_muzzle";
	public static final String WUB_RING = "wub_ring";
	public static final String WUB_HIT = "wub_hit";
	public static final String BASS_DROP = "bass_drop";

	// launchers and strikes
	public static final String BOMBLET_TRAIL = "bomblet_trail";
	public static final String SINGULARITY_TRAIL = "singularity_trail";
	public static final String MORTAR_TRAIL = "mortar_trail";
	public static final String METEOR_TRAIL = "meteor_trail";
	public static final String METEOR_IMPACT = "meteor_impact";
	public static final String ROD_TRAIL = "rod_trail";
	public static final String ROD_IMPACT = "rod_impact";
	public static final String BOMB_WHISTLE = "bomb_whistle";
	public static final String TARGET_LOCK = "target_lock";
	public static final String ORBITAL_IMPACT = "orbital_impact";
	public static final String STORM_CLOUDS = "storm_clouds";
	public static final String STORM_STRIKE = "storm_strike";

	// support and mid-tier weapons
	public static final String WARDEN_SEAL = "warden_seal";
	public static final String PARTY_TRAIL = "party_trail";
	public static final String HONK_BLAST = "honk_blast";
	public static final String MICRO_IMPACT = "micro_impact";
	public static final String TETHER_PULL = "tether_pull";
	public static final String SIEGE_TRAIL = "siege_trail";
	public static final String SIEGE_BLAST = "siege_blast";
	public static final String RESONANCE_WAVE = "resonance_wave";
	public static final String SUNLANCE_CHARGE = "sunlance_charge";
	public static final String SUNLANCE_LINE = "sunlance_line";
	public static final String SUNLANCE_IMPACT = "sunlance_impact";
	public static final String SPLIT_SLAM = "split_slam";
	public static final String SPLIT_FISSURE = "split_fissure";
	public static final String SPLIT_FINALE = "split_finale";
	public static final String MIDAS_TRAIL = "midas_trail";
	public static final String MIDAS_BURST = "midas_burst";

	// legendary and mythic callers
	public static final String VERDICT_COUNT = "verdict_count";
	public static final String VERDICT_STRIKE = "verdict_strike";
	public static final String TITAN_TRAIL = "titan_trail";
	public static final String TITAN_BUILDUP = "titan_buildup";
	public static final String SHEAR_PLANE = "shear_plane";
	public static final String SHEAR_CUT = "shear_cut";
	public static final String JUDGMENT_TRAIL = "judgment_trail";
	public static final String JUDGMENT_HIT = "judgment_hit";
	public static final String JUDGMENT_VENT = "judgment_vent";
	public static final String RIFT_MAW = "rift_maw";
	public static final String RIFT_HIT = "rift_hit";
	public static final String RIFT_COLLAPSE = "rift_collapse";
	public static final String RIFT_BLAST = "rift_blast";
	public static final String SPINDLE_SHAFT = "spindle_shaft";
	public static final String SPINDLE_SWEEP = "spindle_sweep";
	public static final String SPINDLE_PULSE = "spindle_pulse";
	public static final String EXTINCTION_ASH = "extinction_ash";
	public static final String IMPACTOR_STREAK = "impactor_streak";
	public static final String EXTINCTION_WALL = "extinction_wall";

	public static final List<String> ALL = List.of(SPARKS, GRENADE_BLAST, PLASMA_IMPACT, NAPALM, SMOKE_COLUMN, CRYO_BURST,
			VOID_IMPLOSION, TOXIC_CLOUD, ARC_BURST, RADIATION, EMBER_RAIN, FIREWORK, MUZZLE_FLASH, NUKE, MUSHROOM,
			GOO_DRIP, GOO_PULSE, GOO_BURST, GOO_GLOB, GOO_SPLAT, RAY_CHARGE, RAY_MUZZLE, RAY_HEAD,
			CACTUS_DUST, CACTUS_SPIKE, SPIKE_IMPACT, FORGE_STRIKE, FORGE_SHOCK,
			SOUL_FLARE, SOUL_PILLAR, SOUL_VORTEX, SOUL_TRAIL, SOUL_SWEEP, INFRIT_IMPACT, INFRIT_BURST,
			CACTUS_BLOOM, SANDSTORM, ANVIL_IMPACT, IRON_WAVE, PLATE_DUST, INFRIT_MANE, INFRIT_SPARKS, INFRIT_BREATH,
			PLASMA_MUZZLE, PLASMA_TRAIL, RAIL_CHARGE, RAIL_MUZZLE, RAIL_ION, RAIL_IMPACT, MINIGUN_MUZZLE, SHELL_CASING,
			BULLET_IMPACT, FLAME_JET, FLAME_SPLASH, ARC_MUZZLE, CRYO_MUZZLE, CRYO_TRAIL, ROCKET_BACKBLAST, ROCKET_TRAIL,
			BFG_MUZZLE, BFG_TRAIL, BFG_BLAST, HAMMER_SLAM, HAMMER_HIT, DEATHRAY_BURN, DEATHRAY_MUZZLE,
			GRENADE_BLINK, THROW_WHOOSH, FUSE_SPARKS, PLASMA_PULSE, FROST_LEAK, FIRE_LEAK, GRAVITY_WARP, GAS_LEAK, NUKE_TICK,
			ARC_CRACKLE, EMP_PING, BOOMERANG_TRAIL, FRAG_SHRAPNEL, PLASMA_NOVA, INCENDIARY_BURST, GRAVITY_WELL, GAS_BURST,
			SHATTER_BURST, EMP_PULSE,
			SEISMIC_HUM, ION_GLOW, STASIS_HEX, QUANTUM_GLITCH, CRYSTAL_GLINT, VOID_CRACKLE, HORIZON_PULL, MIRROR_SHIMMER,
			SNOW_SWIRL, SLIME_DRIP, SEISMIC_BURST, ION_BLOOM, STASIS_FIELD, QUANTUM_BURST, CRYSTAL_BURST, VOIDSTORM_BURST,
			HORIZON_COLLAPSE, MIRROR_FLASH, SNOW_GLOBE_BURST, SLIME_BURST,
			BOUNCE_TRAIL, LOOT_GLINT, MOON_DUST, HOLO_FLICKER, MAGNET_SPARKS, PAINT_DRIP, NUKE_ROUND_TRAIL, BOUNCE_BURST,
			LOOT_BURST, MOON_BURST, HOLO_BURST, MAGNET_PULSE, PAINT_SPLASH, LANCE_CHARGE, LANCE_MUZZLE, LANCE_TRAIL,
			ANTIMATTER_FLASH, BLADE_HIT, BLADE_DASH, GATLING_MUZZLE, WUB_RING, WUB_HIT, BASS_DROP,
			BOMBLET_TRAIL, SINGULARITY_TRAIL, MORTAR_TRAIL, METEOR_TRAIL, METEOR_IMPACT, ROD_TRAIL, ROD_IMPACT, BOMB_WHISTLE,
			TARGET_LOCK, ORBITAL_IMPACT, STORM_CLOUDS, STORM_STRIKE,
			WARDEN_SEAL, PARTY_TRAIL, HONK_BLAST, MICRO_IMPACT, TETHER_PULL, SIEGE_TRAIL, SIEGE_BLAST, RESONANCE_WAVE,
			SUNLANCE_CHARGE, SUNLANCE_LINE, SUNLANCE_IMPACT, SPLIT_SLAM, SPLIT_FISSURE, SPLIT_FINALE, MIDAS_TRAIL, MIDAS_BURST,
			VERDICT_COUNT, VERDICT_STRIKE, TITAN_TRAIL, TITAN_BUILDUP, SHEAR_PLANE, SHEAR_CUT, JUDGMENT_TRAIL, JUDGMENT_HIT, JUDGMENT_VENT,
			RIFT_MAW, RIFT_HIT, RIFT_COLLAPSE, RIFT_BLAST, SPINDLE_SHAFT, SPINDLE_SWEEP, SPINDLE_PULSE, EXTINCTION_ASH, IMPACTOR_STREAK, EXTINCTION_WALL);

	private VfxIds() {
	}
}
