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

	// celestial bombs
	public static final String COW_SHADOW = "cow_shadow";
	public static final String COW_SHADOW_BIG = "cow_shadow_big";
	public static final String COW_STREAK = "cow_streak";
	public static final String MILK_SPLASH = "milk_splash";
	public static final String COW_DOOM = "cow_doom";
	public static final String MILK_RAINBOW = "milk_rainbow";
	public static final String TWISTER_BASE = "twister_base";
	public static final String TWISTER_RAIN = "twister_rain";
	public static final String TWISTER_CALM = "twister_calm";
	public static final String UFO_BEAM = "ufo_beam";
	public static final String UFO_DRIVE = "ufo_drive";
	public static final String CROP_PULSE = "crop_pulse";
	public static final String ABDUCT_LIFT = "abduct_lift";
	public static final String FORK_HUM = "fork_hum";
	public static final String DUST_DANCE = "dust_dance";
	public static final String CASCADE_FRONT = "cascade_front";
	public static final String FORK_SHATTER = "fork_shatter";
	public static final String TESLA_HIT = "tesla_hit";
	public static final String TESLA_CORONA = "tesla_corona";
	public static final String BALL_FEED = "ball_feed";
	public static final String TESLA_NOVA = "tesla_nova";
	public static final String TOWER_DUST = "tower_dust";
	public static final String GRAVITY_HUM = "gravity_hum";
	public static final String GRAVITY_FLOAT = "gravity_float";
	public static final String GRAVITY_SLAM = "gravity_slam";

	// city destruction bombs
	public static final String MOON_TIDE = "moon_tide";
	public static final String MOON_POCK = "moon_pock";
	public static final String MOON_LANDING = "moon_landing";
	public static final String MOON_SETTLE = "moon_settle";
	public static final String MOON_GEODE = "moon_geode";
	public static final String EATER_PULSE = "eater_pulse";
	public static final String WORM_HEAVE = "worm_heave";
	public static final String WORM_SKIN = "worm_skin";
	// set pieces
	public static final String TWISTER_FUNNEL = "twister_funnel";
	public static final String WALL_CLOUD = "wall_cloud";
	public static final String BALL_LIGHTNING = "ball_lightning";
	public static final String SWORD_FLAME = "sword_flame";
	public static final String TERMITE_SAWDUST = "termite_sawdust";
	public static final String TERMITE_SPOIL = "termite_spoil";
	public static final String CANOPY = "canopy";
	public static final String RESONANCE_RING = "resonance_ring";
	public static final String GRAVITY_CORE = "gravity_core";
	public static final String WORM_MOUTH = "worm_mouth";
	public static final String WORM_BREACH = "worm_breach";
	public static final String MAW_VORTEX = "maw_vortex";
	public static final String MAW_SNAP = "maw_snap";
	public static final String RAGNAROK_ASH = "ragnarok_ash";
	public static final String QUAKE_GEYSER = "quake_geyser";
	public static final String VENT_BURST = "vent_burst";
	public static final String VENT_SPIT = "vent_spit";
	public static final String SURTR_STRIKE = "surtr_strike";
	public static final String FIRE_GEYSER = "fire_geyser";
	public static final String FIRE_SIDE = "fire_side";
	public static final String FROST_SIDE = "frost_side";
	public static final String STAR_IMPACT = "star_impact";
	public static final String PRISM_SHATTER = "prism_shatter";
	public static final String INVERT_RAIN = "invert_rain";
	public static final String ACCRETION = "accretion";
	public static final String VOID_COLLAPSE = "void_collapse";
	public static final String COSMIC_NOVA = "cosmic_nova";
	public static final String ABYSS_MOTES = "abyss_motes";
	public static final String DIRT_SPURT = "dirt_spurt";
	public static final String ANT_SKITTER = "ant_skitter";
	public static final String TERMITE_SWARM = "termite_swarm";
	public static final String MUD_FLING = "mud_fling";
	public static final String TERMITE_CAVE = "termite_cave";
	public static final String SAPLING_SPROUT = "sapling_sprout";
	public static final String TREE_STAGE = "tree_stage";
	public static final String TREE_FIREFLIES = "tree_fireflies";
	public static final String ROOT_SLAM = "root_slam";
	public static final String TREE_BLOOM = "tree_bloom";
	public static final String SEED_SPROUT = "seed_sprout";
	public static final String LAMP_CHUNK = "lamp_chunk";
	public static final String LAMP_SHATTER = "lamp_shatter";
	public static final String STAGE_HAZE = "stage_haze";
	public static final String SPOT_SCORCH = "spot_scorch";
	public static final String SHOWTIME = "showtime";
	public static final String FINALE_GLITTER = "finale_glitter";
	public static final String SPECTRAL_MOTES = "spectral_motes";
	public static final String SPECTRAL_WISPS = "spectral_wisps";
	public static final String SPECTRAL_NOVA = "spectral_nova";
	public static final String CHUNK_STATIC = "chunk_static";
	public static final String BLIGHT_AMBIENT = "blight_ambient";
	public static final String BLIGHT_EDGE = "blight_edge";
	public static final String BLIGHT_PULSE = "blight_pulse";
	public static final String MAMMOTH_HEAVE = "mammoth_heave";
	public static final String MAMMOTH_BREATH = "mammoth_breath";
	public static final String MAMMOTH_TRUMPET = "mammoth_trumpet";
	public static final String MAMMOTH_FRONT = "mammoth_front";
	public static final String MAMMOTH_SNOW = "mammoth_snow";
	public static final String MAMMOTH_STOMP = "mammoth_stomp";
	public static final String MAMMOTH_SPIKE = "mammoth_spike";
	public static final String LEVI_BREACH = "levi_breach";
	public static final String LEVI_BUBBLES = "levi_bubbles";
	public static final String LEVI_CREST = "levi_crest";
	public static final String LEVI_ROAR = "levi_roar";
	public static final String PTERO_BURST = "ptero_burst";
	public static final String PTERO_TRAIL = "ptero_trail";
	public static final String PTERO_SCREECH = "ptero_screech";
	public static final String PTERO_IMPACT = "ptero_impact";
	public static final String PTERO_CRUMBLE = "ptero_crumble";
	public static final String XENO_UNFOLD = "xeno_unfold";
	public static final String XENO_TAG = "xeno_tag";
	public static final String XENO_DEREZ = "xeno_derez";
	public static final String XENO_FRONT = "xeno_front";
	public static final String XENO_LAND = "xeno_land";
	public static final String XENO_DEPART = "xeno_depart";
	public static final String MANIAC_ASH = "maniac_ash";
	public static final String MANIAC_RIFT = "maniac_rift";
	public static final String MANIAC_CURSE = "maniac_curse";
	public static final String MANIAC_SNAP = "maniac_snap";
	public static final String MANIAC_SHATTER = "maniac_shatter";
	public static final String MANIAC_GEYSER = "maniac_geyser";
	public static final String MANIAC_BLEED = "maniac_bleed";
	public static final String MANIAC_GORE = "maniac_gore";
	public static final String MANIAC_BLOOD_RAIN = "maniac_blood_rain";
	public static final String HORSE_APPEAR = "horse_appear";
	public static final String HORSE_VANISH = "horse_vanish";
	public static final String HORSE_HOOF = "horse_hoof";
	public static final String HORSE_PUMPKIN = "horse_pumpkin";
	public static final String HORSE_SLASH = "horse_slash";
	public static final String HORSE_TRAIL = "horse_trail";
	public static final String HORSE_MIST = "horse_mist";
	public static final String AI_BOOT = "ai_boot";
	public static final String AI_RISE = "ai_rise";
	public static final String AI_ZAP = "ai_zap";
	public static final String AI_EMP = "ai_emp";
	public static final String AI_UPLOAD = "ai_upload";
	public static final String HELLGATE_HEAT = "hellgate_heat";
	public static final String HELLGATE_EMERGE = "hellgate_emerge";
	public static final String HELLGATE_RISE = "hellgate_rise";
	public static final String HELLGATE_OPEN = "hellgate_open";
	public static final String HELLGATE_SOULS = "hellgate_souls";
	public static final String HELLGATE_FIRE_TRAIL = "hellgate_fire_trail";
	public static final String HELLGATE_IMPACT = "hellgate_impact";
	public static final String HELLGATE_SLAM = "hellgate_slam";
	public static final String HELLGATE_IMPLODE = "hellgate_implode";
	public static final String HELLGATE_LAVA_SPLASH = "hellgate_lava_splash";
	public static final String PRISM_INBEAM = "prism_inbeam";
	public static final String BANG_INHALE = "bang_inhale";
	public static final String BANG_CMB = "bang_cmb";
	public static final String GALAXY_SWIRL = "galaxy_swirl";
	public static final String NEBULA_CLOUD = "nebula_cloud";
	public static final String SUN_IGNITE = "sun_ignite";
	public static final String AURORA_STARS = "aurora_stars";
	public static final String AURORA_SPARK = "aurora_spark";
	public static final String AURORA_STORM = "aurora_storm";
	public static final String DIM_RIFT = "dim_rift";
	public static final String DIM_FRONT = "dim_front";
	public static final String DIM_ASH = "dim_ash";
	public static final String DIM_CLOSE = "dim_close";
	public static final String HAVOC_LAUNCH = "havoc_launch";
	public static final String HAVOC_TRAIL = "havoc_trail";
	public static final String HAVOC_BURST = "havoc_burst";
	public static final String HAVOC_FOUNTAIN = "havoc_fountain";
	public static final String HAVOC_WILLOW = "havoc_willow";
	public static final String LANTERN_RISE = "lantern_rise";
	public static final String LANTERN_SPARKLE = "lantern_sparkle";
	public static final String LANTERN_BURST = "lantern_burst";
	public static final String KOI_POND = "koi_pond";
	public static final String KOI_SHIMMER = "koi_shimmer";
	public static final String KOI_RIPPLE = "koi_ripple";
	public static final String KOI_TRAIL = "koi_trail";
	public static final String KOI_BREACH = "koi_breach";
	public static final String KOI_SPLASH = "koi_splash";
	public static final String KOI_GEYSER = "koi_geyser";
	public static final String KOI_RAIN = "koi_rain";
	public static final String SEASON_SPRING = "season_spring";
	public static final String SEASON_SUMMER = "season_summer";
	public static final String SEASON_AUTUMN = "season_autumn";
	public static final String SEASON_WINTER = "season_winter";
	public static final String CENTURY_WAVE = "century_wave";
	public static final String PRISM_SCORCH = "prism_scorch";
	public static final String PRISM_NOVA = "prism_nova";
	public static final String GEODE_HUM = "geode_hum";
	public static final String GEODE_SHARD = "geode_shard";
	public static final String GEODE_SHATTER = "geode_shatter";
	public static final String FOSSIL_HEAVE = "fossil_heave";
	public static final String FOSSIL_ROAR = "fossil_roar";
	public static final String LAMP_SIMMER = "lamp_simmer";
	public static final String LAMP_CRACK = "lamp_crack";
	public static final String LAMP_BURST = "lamp_burst";
	public static final String LAMP_SPLAT = "lamp_splat";
	public static final String ION_CARVE = "ion_carve";
	public static final String ION_CHARGE = "ion_charge";
	public static final String ION_IGNITE = "ion_ignite";
	public static final String ACID_STORM = "acid_storm";
	public static final String SUPERBLOOM = "superbloom";
	public static final String FORGE_SPARKFALL = "forge_sparkfall";
	public static final String CHUNK_REROLL = "chunk_reroll";
	public static final String GRID_FANFARE = "grid_fanfare";
	public static final String CHUNK_SWAP = "chunk_swap";
	public static final String CORNER_GEYSER = "corner_geyser";
	public static final String BEAM_SPARKLE = "beam_sparkle";
	public static final String DISCO_DROP = "disco_drop";
	public static final String FLOOR_PULSE = "floor_pulse";
	public static final String LIDAR_HIT = "lidar_hit";
	public static final String LIDAR_OVERLOAD = "lidar_overload";
	public static final String VOID_SWALLOW = "void_swallow";
	public static final String VOID_EXHALE = "void_exhale";
	public static final String ICE_ERUPT = "ice_erupt";
	public static final String SHARD_LAND = "shard_land";
	public static final String SONAR_SHATTER = "sonar_shatter";
	public static final String SONAR_BOOM = "sonar_boom";
	public static final String PIXEL_BURST = "pixel_burst";
	public static final String MAG_LIFT = "mag_lift";
	public static final String MAG_GRIND = "mag_grind";
	public static final String MAG_JET_HIT = "mag_jet_hit";
	public static final String MAG_COLLAPSE = "mag_collapse";
	public static final String COMIC_BLAST = "comic_blast";
	public static final String TSUNAMI_CREST = "tsunami_crest";
	public static final String VOLCANO_RISE = "volcano_rise";
	public static final String VOLCANO_OPEN = "volcano_open";
	public static final String VOLCANO_PLUME = "volcano_plume";
	public static final String VOLCANO_BURST = "volcano_burst";
	public static final String ICE_FRONT = "ice_front";
	public static final String ICE_BLIZZARD = "ice_blizzard";
	public static final String SPIRE_CRACK = "spire_crack";
	public static final String HELL_CRACK = "hell_crack";
	public static final String HELL_OPEN = "hell_open";
	public static final String HELL_GEYSER = "hell_geyser";
	public static final String HELL_THROAT = "hell_throat";
	public static final String HELL_ERUPT = "hell_erupt";
	public static final String CYCLONE_WEDGE = "cyclone_wedge";
	public static final String HOUR_MARK = "hour_mark";
	public static final String CLOCK_HAND = "clock_hand";
	public static final String HOUR_STRIKE = "hour_strike";
	public static final String MIDNIGHT = "midnight";
	public static final String BANG_BIRTH = "bang_birth";
	public static final String BANG_STARS = "bang_stars";
	public static final String PLANET_FORM = "planet_form";
	public static final String CHRONO_SHELL = "chrono_shell";
	public static final String CHRONO_SAND = "chrono_sand";
	public static final String CHRONO_SNAP = "chrono_snap";
	public static final String SPEAR_HERALD = "spear_herald";
	public static final String SPEAR_IMPACT = "spear_impact";
	public static final String SPEAR_DRILL = "spear_drill";
	public static final String CORE_GEYSER = "core_geyser";
	public static final String ZOMBIE_CLAW = "zombie_claw";
	public static final String GIANT_RISE = "giant_rise";
	public static final String GIANT_STOMP = "giant_stomp";
	public static final String GIANT_BURST = "giant_burst";
	public static final String ZOMBIE_DUST = "zombie_dust";
	public static final String SPEAR_STRIKE = "spear_strike";
	public static final String GODFALL_IMPACT = "godfall_impact";
	public static final String DISCO_BEAT = "disco_beat";
	public static final String GLITTER_FLAKES = "glitter_flakes";
	public static final String GLITTER_POP = "glitter_pop";
	public static final String PUMPKIN_SPROUT = "pumpkin_sprout";
	public static final String PUMPKIN_FOG = "pumpkin_fog";
	public static final String WEB_BURST = "web_burst";
	public static final String BALLOON_LIFT = "balloon_lift";
	public static final String BALLOON_POP = "balloon_pop";
	public static final String SPRINKLER_RAIN = "sprinkler_rain";
	public static final String THERMITE_BURN = "thermite_burn";
	public static final String TESLA_LID = "tesla_lid";
	public static final String FULGURITE = "fulgurite";
	public static final String SINK_POUR = "sink_pour";
	public static final String SINK_RIM = "sink_rim";
	public static final String SURGE_DUST = "surge_dust";
	public static final String SURGE_SPRAY = "surge_spray";
	public static final String SURGE_LEAVES = "surge_leaves";
	public static final String SURGE_WALL = "surge_wall";
	public static final String CLUSTER_POP = "cluster_pop";
	public static final String NOVA_PROMINENCE = "nova_prominence";
	public static final String NOVA_SCORCH = "nova_scorch";
	public static final String OMEGA_DRAW = "omega_draw";
	public static final String OMEGA_CRACK = "omega_crack";
	public static final String CROWN_RING = "crown_ring";
	public static final String CROWN_BURST = "crown_burst";
	public static final String CONTINENT_HEAVE = "continent_heave";
	public static final String DAWN_GATHER = "dawn_gather";
	public static final String DAWN_RISE = "dawn_rise";
	public static final String BLOOD_RAIN = "blood_rain";
	public static final String BLOOD_STRIKE = "blood_strike";
	public static final String BLOOD_NOVA = "blood_nova";
	public static final String LL_BEAT = "ll_beat";
	public static final String LL_ASH = "ll_ash";
	public static final String LL_DEATH = "ll_death";
	public static final String ZP_HUSH = "zp_hush";
	public static final String ZP_SNAP = "zp_snap";
	public static final String UM_PLANE = "um_plane";
	public static final String DEMO_BLAST = "demo_blast";
	public static final String THERMO_BLAST = "thermo_blast";
	public static final String CRYO_NOVA = "cryo_nova";
	public static final String HOLE_INFALL = "hole_infall";
	public static final String HOLE_POP = "hole_pop";
	public static final String NEUTRON_PULSE = "neutron_pulse";
	public static final String NUKE_SURGE = "nuke_surge";
	public static final String HYDROGEN_STAGE = "hydrogen_stage";
	public static final String ANTI_ANNIHILATE = "anti_annihilate";
	public static final String TECTONIC_FRONT = "tectonic_front";
	public static final String BASS_THUMP = "bass_thump";
	public static final String DROP_FINALE = "drop_finale";
	public static final String SMASH_IMPACT = "smash_impact";
	public static final String SMASH_BELL = "smash_bell";
	public static final String STAR_LAND = "star_land";
	public static final String HEART_NOVA = "heart_nova";
	public static final String SMASH_DUST = "smash_dust";
	public static final String TICK_BEAT = "tick_beat";
	public static final String TICK_BOOM = "tick_boom";
	public static final String FIELD_BLAST = "field_blast";
	public static final String TICK_FINAL = "tick_final";
	public static final String TICK_CHIME = "tick_chime";
	public static final String TIME_STILL = "time_still";
	public static final String TIME_SNAP = "time_snap";
	public static final String ALIEN_EMP = "alien_emp";
	public static final String EMP_CRAWL = "emp_crawl";
	public static final String RAY_COLUMN = "ray_column";
	public static final String RAY_IMPACT = "ray_impact";
	public static final String TERRA_PULSE = "terra_pulse";
	public static final String HARVEST_LIFT = "harvest_lift";
	public static final String HARVEST_CUT = "harvest_cut";
	public static final String BLOOM_SPORES = "bloom_spores";
	public static final String SHAFT_MOTES = "shaft_motes";
	public static final String SUN_BEAM = "sun_beam";
	public static final String SUN_FROST = "sun_frost";
	public static final String CORPSE_SHED = "corpse_shed";
	public static final String SUN_IMPACT = "sun_impact";
	public static final String DAWN_MOTES = "dawn_motes";
	public static final String MIRROR_GLINT = "mirror_glint";
	public static final String LANCE_CUT = "lance_cut";
	public static final String LANCE_DRILL = "lance_drill";
	public static final String HAMMER_BLOW = "hammer_blow";
	public static final String CREST_SPRAY = "crest_spray";

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
			RIFT_MAW, RIFT_HIT, RIFT_COLLAPSE, RIFT_BLAST, SPINDLE_SHAFT, SPINDLE_SWEEP, SPINDLE_PULSE, EXTINCTION_ASH, IMPACTOR_STREAK, EXTINCTION_WALL,
			COW_SHADOW, COW_SHADOW_BIG, COW_STREAK, MILK_SPLASH, COW_DOOM, MILK_RAINBOW, TWISTER_BASE, TWISTER_RAIN, TWISTER_CALM,
			UFO_BEAM, UFO_DRIVE, CROP_PULSE, ABDUCT_LIFT, FORK_HUM, DUST_DANCE, CASCADE_FRONT, FORK_SHATTER,
			TESLA_HIT, TESLA_CORONA, BALL_FEED, TESLA_NOVA, TOWER_DUST, GRAVITY_HUM, GRAVITY_FLOAT, GRAVITY_SLAM,
			MOON_TIDE, MOON_POCK, MOON_LANDING, MOON_SETTLE, MOON_GEODE, EATER_PULSE, WORM_HEAVE, WORM_SKIN, WORM_MOUTH, TWISTER_FUNNEL, WALL_CLOUD, BALL_LIGHTNING, SWORD_FLAME, TERMITE_SAWDUST, TERMITE_SPOIL, CANOPY, RESONANCE_RING, GRAVITY_CORE, WORM_BREACH,
			MAW_VORTEX, MAW_SNAP, RAGNAROK_ASH, QUAKE_GEYSER, VENT_BURST, VENT_SPIT, SURTR_STRIKE, FIRE_GEYSER, FIRE_SIDE,
			FROST_SIDE, STAR_IMPACT, PRISM_SHATTER, INVERT_RAIN, ACCRETION, VOID_COLLAPSE, COSMIC_NOVA, ABYSS_MOTES, DIRT_SPURT,
			ANT_SKITTER, TERMITE_SWARM, MUD_FLING, TERMITE_CAVE, SAPLING_SPROUT, TREE_STAGE, TREE_FIREFLIES, ROOT_SLAM, TREE_BLOOM,
			SEED_SPROUT, LAMP_CHUNK, LAMP_SHATTER, STAGE_HAZE, SPOT_SCORCH, SHOWTIME, FINALE_GLITTER,
			SMASH_IMPACT, SMASH_BELL, STAR_LAND, HEART_NOVA, SMASH_DUST, TICK_BEAT, TICK_BOOM, FIELD_BLAST, TICK_FINAL, TICK_CHIME, TIME_STILL, TIME_SNAP, ALIEN_EMP, EMP_CRAWL, RAY_COLUMN, RAY_IMPACT, TERRA_PULSE, HARVEST_LIFT, HARVEST_CUT, BLOOM_SPORES, SHAFT_MOTES, SUN_BEAM, SUN_FROST, CORPSE_SHED, SUN_IMPACT, DAWN_MOTES, MIRROR_GLINT, LANCE_CUT, LANCE_DRILL, HAMMER_BLOW, CREST_SPRAY,
			DEMO_BLAST, THERMO_BLAST, CRYO_NOVA, HOLE_INFALL, HOLE_POP, NEUTRON_PULSE, NUKE_SURGE, HYDROGEN_STAGE, ANTI_ANNIHILATE, TECTONIC_FRONT, BASS_THUMP, DROP_FINALE,
			CLUSTER_POP, NOVA_PROMINENCE, NOVA_SCORCH, OMEGA_DRAW, OMEGA_CRACK, CROWN_RING, CROWN_BURST, CONTINENT_HEAVE, DAWN_GATHER, DAWN_RISE, BLOOD_RAIN, BLOOD_STRIKE, BLOOD_NOVA, LL_BEAT, LL_ASH, LL_DEATH, ZP_HUSH, ZP_SNAP, UM_PLANE,
			SURGE_DUST, SURGE_SPRAY, SURGE_LEAVES, SURGE_WALL,
			SPEAR_STRIKE, GODFALL_IMPACT, DISCO_BEAT, GLITTER_FLAKES, GLITTER_POP, PUMPKIN_SPROUT, PUMPKIN_FOG, WEB_BURST, BALLOON_LIFT, BALLOON_POP, SPRINKLER_RAIN, THERMITE_BURN, TESLA_LID, FULGURITE, SINK_POUR, SINK_RIM,
			TSUNAMI_CREST, VOLCANO_RISE, VOLCANO_OPEN, VOLCANO_PLUME, VOLCANO_BURST, ICE_FRONT, ICE_BLIZZARD, SPIRE_CRACK, HELL_CRACK, HELL_OPEN, HELL_GEYSER, HELL_THROAT, HELL_ERUPT, CYCLONE_WEDGE, HOUR_MARK, CLOCK_HAND, HOUR_STRIKE, MIDNIGHT, BANG_BIRTH, BANG_STARS, PLANET_FORM, CHRONO_SHELL, CHRONO_SAND, CHRONO_SNAP, SPEAR_HERALD, SPEAR_IMPACT, SPEAR_DRILL, CORE_GEYSER, ZOMBIE_CLAW, GIANT_RISE, GIANT_STOMP, GIANT_BURST, ZOMBIE_DUST,
			CHUNK_REROLL, GRID_FANFARE, CHUNK_SWAP, CORNER_GEYSER, BEAM_SPARKLE, DISCO_DROP, FLOOR_PULSE, LIDAR_HIT, LIDAR_OVERLOAD, VOID_SWALLOW, VOID_EXHALE, ICE_ERUPT, SHARD_LAND, SONAR_SHATTER, SONAR_BOOM, PIXEL_BURST, MAG_LIFT, MAG_GRIND, MAG_JET_HIT, MAG_COLLAPSE, COMIC_BLAST,
			SEASON_SPRING, SEASON_SUMMER, SEASON_AUTUMN, SEASON_WINTER, CENTURY_WAVE, PRISM_SCORCH, PRISM_NOVA, GEODE_HUM, GEODE_SHARD, GEODE_SHATTER, FOSSIL_HEAVE, FOSSIL_ROAR, LAMP_SIMMER, LAMP_CRACK, LAMP_BURST, LAMP_SPLAT, ION_CARVE, ION_CHARGE, ION_IGNITE, ACID_STORM, SUPERBLOOM, FORGE_SPARKFALL,
			KOI_POND, KOI_SHIMMER, KOI_RIPPLE, KOI_TRAIL, KOI_BREACH, KOI_SPLASH, KOI_GEYSER, KOI_RAIN,
			DIM_RIFT, DIM_FRONT, DIM_ASH, DIM_CLOSE, HAVOC_LAUNCH, HAVOC_TRAIL, HAVOC_BURST, HAVOC_FOUNTAIN, HAVOC_WILLOW, LANTERN_RISE, LANTERN_SPARKLE, LANTERN_BURST,
			AURORA_STARS, AURORA_SPARK, AURORA_STORM,
			BANG_INHALE, BANG_CMB, GALAXY_SWIRL, NEBULA_CLOUD, SUN_IGNITE,
			PRISM_INBEAM,
			SPECTRAL_MOTES, SPECTRAL_WISPS, SPECTRAL_NOVA, CHUNK_STATIC, BLIGHT_AMBIENT, BLIGHT_EDGE, BLIGHT_PULSE, MAMMOTH_HEAVE, MAMMOTH_BREATH,
			MAMMOTH_TRUMPET, MAMMOTH_FRONT, MAMMOTH_SNOW, MAMMOTH_STOMP, MAMMOTH_SPIKE, LEVI_BREACH, LEVI_BUBBLES, LEVI_CREST, LEVI_ROAR, PTERO_BURST, PTERO_TRAIL,
			PTERO_SCREECH, PTERO_IMPACT, PTERO_CRUMBLE,
			XENO_UNFOLD, XENO_TAG, XENO_DEREZ, XENO_FRONT, XENO_LAND, XENO_DEPART,
			MANIAC_ASH, MANIAC_RIFT, MANIAC_CURSE, MANIAC_SNAP, MANIAC_SHATTER, MANIAC_GEYSER, MANIAC_BLEED, MANIAC_GORE, MANIAC_BLOOD_RAIN,
			HORSE_APPEAR, HORSE_VANISH, HORSE_HOOF, HORSE_PUMPKIN, HORSE_SLASH, HORSE_TRAIL, HORSE_MIST,
			HELLGATE_HEAT, HELLGATE_EMERGE, HELLGATE_RISE, HELLGATE_OPEN, HELLGATE_SOULS, HELLGATE_FIRE_TRAIL, HELLGATE_IMPACT, HELLGATE_SLAM, HELLGATE_IMPLODE, HELLGATE_LAVA_SPLASH,
			AI_BOOT, AI_RISE, AI_ZAP, AI_EMP, AI_UPLOAD);

	private VfxIds() {
	}
}
